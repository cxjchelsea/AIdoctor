package com.aidoctor.diagnosis.state.committer;

import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.ports.AuditPort;
import com.aidoctor.diagnosis.state.committer.ports.CapabilityPolicyPort;
import com.aidoctor.diagnosis.state.committer.ports.CommitEventEvidencePort;
import com.aidoctor.diagnosis.state.committer.ports.ConsentPolicyPort;
import com.aidoctor.diagnosis.state.committer.ports.FieldPermissionPort;
import com.aidoctor.diagnosis.state.committer.ports.IdempotencyPort;
import com.aidoctor.diagnosis.state.committer.ports.SourceValidationPort;
import com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure mechanical State Committer core.
 *
 * <p>{@code COMMITTED} means the request passed the PBNC-01 admission gates
 * and the authoritative mechanical repository boundary advanced its version.
 * It does not mean operations were applied to a clinical record. No Spring,
 * HTTP, persistence adapter, clinical runtime, or production wiring is here.
 */
public final class StateCommitter {
    private static final Set<String> MECHANICALLY_SUPPORTED_OPERATIONS =
            new HashSet<String>(Arrays.asList("ADD", "REPLACE", "REMOVE"));

    private final StateRepositoryPort stateRepository;
    private final CapabilityPolicyPort capabilityPolicy;
    private final FieldPermissionPort fieldPermission;
    private final ConsentPolicyPort consentPolicy;
    private final SourceValidationPort sourceValidation;
    private final IdempotencyPort idempotency;
    private final AuditPort audit;
    private final CommitEventEvidencePort eventEvidence;
    private final Clock clock;
    private final CommitResultAssembler assembler = new CommitResultAssembler();
    private final StatePatchBoundaryValidator boundaryValidator = new StatePatchBoundaryValidator();

    public StateCommitter(
            StateRepositoryPort stateRepository,
            CapabilityPolicyPort capabilityPolicy,
            FieldPermissionPort fieldPermission,
            ConsentPolicyPort consentPolicy,
            SourceValidationPort sourceValidation,
            IdempotencyPort idempotency,
            AuditPort audit,
            CommitEventEvidencePort eventEvidence
    ) {
        this(stateRepository, capabilityPolicy, fieldPermission, consentPolicy, sourceValidation,
                idempotency, audit, eventEvidence, Clock.systemUTC());
    }

    public StateCommitter(
            StateRepositoryPort stateRepository,
            CapabilityPolicyPort capabilityPolicy,
            FieldPermissionPort fieldPermission,
            ConsentPolicyPort consentPolicy,
            SourceValidationPort sourceValidation,
            IdempotencyPort idempotency,
            AuditPort audit,
            CommitEventEvidencePort eventEvidence,
            Clock clock
    ) {
        this.stateRepository = requirePort(stateRepository, "stateRepository");
        this.capabilityPolicy = requirePort(capabilityPolicy, "capabilityPolicy");
        this.fieldPermission = requirePort(fieldPermission, "fieldPermission");
        this.consentPolicy = requirePort(consentPolicy, "consentPolicy");
        this.sourceValidation = requirePort(sourceValidation, "sourceValidation");
        this.idempotency = requirePort(idempotency, "idempotency");
        this.audit = requirePort(audit, "audit");
        this.eventEvidence = requirePort(eventEvidence, "eventEvidence");
        this.clock = requirePort(clock, "clock");
    }

    public StateTypes.CommitResult commit(StateTypes.StatePatch patch) {
        String now = clock.instant().toString();
        try {
            return doCommit(patch, now);
        } catch (RuntimeException exception) {
            return finishFailed(patch, previousVersionOrZero(patch), now,
                    CommitReasonCodes.INVARIANT_FAILURE, "Unexpected mechanical failure.", false);
        }
    }

    private StateTypes.CommitResult doCommit(StateTypes.StatePatch patch, String now) {
        StatePatchBoundaryValidator.Validation validation = boundaryValidator.validate(patch);
        if (!validation.valid) {
            return boundaryFailure(patch, now, validation.message);
        }

        String fingerprint = CanonicalPatchFingerprint.fingerprint(patch);
        IdempotencyPort.Decision existing;
        try {
            existing = idempotency.inspect(patch.idempotencyKey, fingerprint);
        } catch (RuntimeException exception) {
            return finishFailed(patch, patch.baseVersion.intValue(), now,
                    CommitReasonCodes.IDEMPOTENCY_INFRASTRUCTURE_FAILURE,
                    "Idempotency inspection failed.", true);
        }
        StateTypes.CommitResult existingResult = resolveExisting(patch, existing, now);
        if (existingResult != null) {
            return existingResult;
        }

        String capabilityId = patch.envelope.capabilityId;
        CapabilityPolicyPort.CapabilityDecision capabilityDecision =
                capabilityPolicy.evaluate(capabilityId, patch.envelope.capabilityVersion);
        if (capabilityDecision == null || !capabilityDecision.isAuthorized()) {
            return finishRejected(patch, patch.baseVersion.intValue(), now,
                    CommitReasonCodes.CAPABILITY_NOT_AUTHORIZED,
                    assembler.rejectAll(patch, CommitReasonCodes.CAPABILITY_NOT_AUTHORIZED));
        }
        ConsentPolicyPort.ConsentDecision consentDecision = consentPolicy.evaluate(patch.cdpId, capabilityId);
        if (consentDecision == null || !consentDecision.isAuthorized()) {
            return finishRejected(patch, patch.baseVersion.intValue(), now,
                    CommitReasonCodes.CONSENT_NOT_AUTHORIZED,
                    assembler.rejectAll(patch, CommitReasonCodes.CONSENT_NOT_AUTHORIZED));
        }

        List<StateTypes.RejectedOperation> rejected = new ArrayList<StateTypes.RejectedOperation>();
        String rejectionReason = null;
        for (int index = 0; index < patch.operations.size(); index++) {
            StateTypes.StatePatchOperation operation = patch.operations.get(index);
            if (!MECHANICALLY_SUPPORTED_OPERATIONS.contains(operation.op)) {
                rejected.add(assembler.rejectedOperation(index, CommitReasonCodes.OPERATION_NOT_AUTHORIZED));
                rejectionReason = CommitReasonCodes.OPERATION_NOT_AUTHORIZED;
                continue;
            }
            if (operation.expectedCurrentValue != null
                    || (("ADD".equals(operation.op) || "REPLACE".equals(operation.op))
                    && operation.value == null)) {
                rejected.add(assembler.rejectedOperation(index, CommitReasonCodes.STATE_OPERATION_SEMANTICS_UNSUPPORTED));
                if (rejectionReason == null) {
                    rejectionReason = CommitReasonCodes.STATE_OPERATION_SEMANTICS_UNSUPPORTED;
                }
                continue;
            }
            FieldPermissionPort.FieldPermissionDecision fieldDecision =
                    fieldPermission.evaluate(operation.path, capabilityId);
            if (fieldDecision == null || !fieldDecision.authorized) {
                rejected.add(assembler.rejectedOperation(index, CommitReasonCodes.FIELD_NOT_AUTHORIZED));
                if (rejectionReason == null) {
                    rejectionReason = CommitReasonCodes.FIELD_NOT_AUTHORIZED;
                }
                continue;
            }
            SourceValidationPort.SourceDecision sourceDecision = sourceValidation.evaluate(operation.source);
            if (sourceDecision == null || !sourceDecision.authorized) {
                rejected.add(assembler.rejectedOperation(index, CommitReasonCodes.SOURCE_NOT_AUTHORIZED));
                if (rejectionReason == null) {
                    rejectionReason = CommitReasonCodes.SOURCE_NOT_AUTHORIZED;
                }
            }
        }
        if (!rejected.isEmpty()) {
            return finishRejected(patch, patch.baseVersion.intValue(), now, rejectionReason, rejected);
        }

        int currentVersion;
        try {
            currentVersion = stateRepository.readCurrentVersion(patch.cdpId);
        } catch (RuntimeException exception) {
            return finishFailed(patch, patch.baseVersion.intValue(), now,
                    CommitReasonCodes.REPOSITORY_INTERNAL_FAILURE,
                    "Repository version read failed.", true);
        }
        if (currentVersion != patch.baseVersion.intValue()) {
            return finishConflict(patch, currentVersion, now, CommitReasonCodes.VERSION_MISMATCH,
                    assembler.versionMismatch(
                            assembler.conflictId(patch, CommitReasonCodes.VERSION_MISMATCH),
                            patch.baseVersion.intValue(), currentVersion, firstOperationPath(patch)));
        }

        IdempotencyPort.Decision reservation;
        try {
            reservation = idempotency.reserve(patch.idempotencyKey, fingerprint);
        } catch (RuntimeException exception) {
            return finishFailed(patch, currentVersion, now,
                    CommitReasonCodes.IDEMPOTENCY_INFRASTRUCTURE_FAILURE,
                    "Idempotency reservation failed.", true);
        }
        StateTypes.CommitResult lostReservation = resolveReservation(patch, reservation, now);
        if (lostReservation != null) {
            return lostReservation;
        }

        FoundationTypes.AuditRef auditRef = recordAudit(
                "STATE_PATCH_REQUESTED", patch, "REQUESTED", CommitReasonCodes.PATCH_COMMITTED);
        if (auditRef == null) {
            releaseReservation(patch, fingerprint);
            return failedAfterAudit(patch, currentVersion, now);
        }

        StateRepositoryPort.AtomicCommitOutcome outcome;
        try {
            outcome = stateRepository.attemptAtomicCommit(new StateRepositoryPort.AtomicCommitCommand(
                    patch.cdpId, patch.baseVersion.intValue(), patch.patchId, patch.idempotencyKey, patch));
        } catch (RuntimeException exception) {
            releaseReservation(patch, fingerprint);
            return finishFailedWithAudit(patch, currentVersion, now,
                    CommitReasonCodes.REPOSITORY_INTERNAL_FAILURE,
                    "Repository commit attempt failed.", auditRef, true);
        }
        if (outcome == null || outcome.status == null
                || outcome.status == StateRepositoryPort.AtomicCommitOutcome.Status.FAILED) {
            releaseReservation(patch, fingerprint);
            return finishFailedWithAudit(patch, currentVersion, now,
                    outcome == null || isBlank(outcome.failureCode)
                            ? CommitReasonCodes.REPOSITORY_INTERNAL_FAILURE : outcome.failureCode,
                    outcome == null || isBlank(outcome.failureMessage)
                            ? "Repository atomic commit failed." : outcome.failureMessage,
                    auditRef, outcome == null || outcome.retryable);
        }
        if (outcome.status == StateRepositoryPort.AtomicCommitOutcome.Status.CONFLICT) {
            releaseReservation(patch, fingerprint);
            int actual = outcome.actualVersion;
            return finishConflictWithAudit(patch, actual, now, CommitReasonCodes.VERSION_MISMATCH,
                    assembler.versionMismatch(
                            assembler.conflictId(patch, CommitReasonCodes.VERSION_MISMATCH),
                            patch.baseVersion.intValue(), actual, firstOperationPath(patch)), auditRef);
        }
        StateTypes.CommitResult result = assembler.committed(
                patch, outcome.previousVersion, outcome.committedVersion, now, auditRef);
        try {
            idempotency.complete(patch.idempotencyKey, fingerprint, assembler.copyOf(result));
        } catch (RuntimeException ignored) {
            // The reservation remains and prevents any retry from committing twice.
        }
        emitSafely(InternalCommitEventEvidence.KIND_COMMITTED, result, now);
        return result;
    }

    private StateTypes.CommitResult resolveExisting(
            StateTypes.StatePatch patch, IdempotencyPort.Decision decision, String now) {
        if (decision == null) {
            return finishFailed(patch, patch.baseVersion.intValue(), now,
                    CommitReasonCodes.IDEMPOTENCY_INFRASTRUCTURE_FAILURE,
                    "Idempotency inspection returned no decision.", true);
        }
        if (decision.status == IdempotencyPort.Decision.Status.ABSENT) {
            return null;
        }
        if (decision.status == IdempotencyPort.Decision.Status.COMPLETED_SAME_FINGERPRINT
                && decision.originalResult != null) {
            StateTypes.CommitResult replay = assembler.copyOf(decision.originalResult);
            emitSafely(InternalCommitEventEvidence.KIND_REPLAYED, replay, now);
            return replay;
        }
        if (decision.status == IdempotencyPort.Decision.Status.MISMATCH) {
            return idempotencyMismatch(patch, now);
        }
        return unavailableIdempotencyResult(patch, now);
    }

    private StateTypes.CommitResult resolveReservation(
            StateTypes.StatePatch patch, IdempotencyPort.Decision decision, String now) {
        if (decision != null && decision.status == IdempotencyPort.Decision.Status.ACQUIRED) {
            return null;
        }
        if (decision != null
                && decision.status == IdempotencyPort.Decision.Status.COMPLETED_SAME_FINGERPRINT
                && decision.originalResult != null) {
            StateTypes.CommitResult replay = assembler.copyOf(decision.originalResult);
            emitSafely(InternalCommitEventEvidence.KIND_REPLAYED, replay, now);
            return replay;
        }
        if (decision != null && decision.status == IdempotencyPort.Decision.Status.MISMATCH) {
            return idempotencyMismatch(patch, now);
        }
        if (decision == null || decision.status == IdempotencyPort.Decision.Status.ABSENT) {
            return finishFailed(patch, patch.baseVersion.intValue(), now,
                    CommitReasonCodes.IDEMPOTENCY_INFRASTRUCTURE_FAILURE,
                    "Idempotency reservation returned an invalid decision.", true);
        }
        return unavailableIdempotencyResult(patch, now);
    }

    private StateTypes.CommitResult idempotencyMismatch(StateTypes.StatePatch patch, String now) {
        return finishConflict(patch, patch.baseVersion.intValue(), now,
                CommitReasonCodes.IDEMPOTENCY_MISMATCH,
                assembler.idempotencyMismatch(
                        assembler.conflictId(patch, CommitReasonCodes.IDEMPOTENCY_MISMATCH),
                        patch.baseVersion));
    }

    private StateTypes.CommitResult unavailableIdempotencyResult(StateTypes.StatePatch patch, String now) {
        return finishFailed(patch, patch.baseVersion.intValue(), now,
                CommitReasonCodes.IDEMPOTENCY_RESULT_UNAVAILABLE,
                "The matching idempotency reservation has no completed result.", true);
    }

    private StateTypes.CommitResult boundaryFailure(
            StateTypes.StatePatch patch, String now, String message) {
        StateTypes.CommitResult result = assembler.failed(
                patch, previousVersionOrZero(patch), now,
                CommitReasonCodes.CONTRACT_IDENTITY_INVALID, message,
                assembler.fallbackAuditRef(now), false);
        emitSafely(InternalCommitEventEvidence.KIND_FAILED, result, now);
        return result;
    }

    private StateTypes.CommitResult finishRejected(
            StateTypes.StatePatch patch, int previousVersion, String now, String reasonCode,
            List<StateTypes.RejectedOperation> rejectedOperations) {
        FoundationTypes.AuditRef auditRef = recordAudit(
                "STATE_PATCH_REQUESTED", patch, "REJECTED", reasonCode);
        if (auditRef == null) {
            return failedAfterAudit(patch, previousVersion, now);
        }
        StateTypes.CommitResult result = assembler.rejected(
                patch, previousVersion, now, reasonCode, rejectedOperations, auditRef);
        emitSafely(InternalCommitEventEvidence.KIND_REJECTED, result, now);
        return result;
    }

    private StateTypes.CommitResult finishConflict(
            StateTypes.StatePatch patch, int previousVersion, String now, String reasonCode,
            FoundationTypes.ContractConflict conflict) {
        FoundationTypes.AuditRef auditRef = recordAudit(
                "STATE_PATCH_REQUESTED", patch, "CONFLICT", reasonCode);
        if (auditRef == null) {
            return failedAfterAudit(patch, previousVersion, now);
        }
        return finishConflictWithAudit(patch, previousVersion, now, reasonCode, conflict, auditRef);
    }

    private StateTypes.CommitResult finishConflictWithAudit(
            StateTypes.StatePatch patch, int previousVersion, String now, String reasonCode,
            FoundationTypes.ContractConflict conflict, FoundationTypes.AuditRef auditRef) {
        StateTypes.CommitResult result = assembler.conflict(
                patch, previousVersion, now, reasonCode, conflict, auditRef);
        emitSafely(InternalCommitEventEvidence.KIND_CONFLICT, result, now);
        return result;
    }

    private StateTypes.CommitResult finishFailed(
            StateTypes.StatePatch patch, int previousVersion, String now,
            String errorCode, String errorMessage, boolean retryable) {
        FoundationTypes.AuditRef auditRef = recordAudit(
                "STATE_PATCH_REQUESTED", patch, "FAILED", errorCode);
        if (auditRef == null) {
            auditRef = assembler.fallbackAuditRef(now);
        }
        return finishFailedWithAudit(
                patch, previousVersion, now, errorCode, errorMessage, auditRef, retryable);
    }

    private StateTypes.CommitResult finishFailedWithAudit(
            StateTypes.StatePatch patch, int previousVersion, String now,
            String errorCode, String errorMessage, FoundationTypes.AuditRef auditRef,
            boolean retryable) {
        StateTypes.CommitResult result = assembler.failed(
                patch, previousVersion, now, errorCode, errorMessage, auditRef, retryable);
        emitSafely(InternalCommitEventEvidence.KIND_FAILED, result, now);
        return result;
    }

    private StateTypes.CommitResult failedAfterAudit(
            StateTypes.StatePatch patch, int previousVersion, String now) {
        return finishFailedWithAudit(
                patch, previousVersion, now, CommitReasonCodes.AUDIT_INFRASTRUCTURE_FAILURE,
                "Audit infrastructure failed.", assembler.fallbackAuditRef(now), true);
    }

    private FoundationTypes.AuditRef recordAudit(
            String auditType, StateTypes.StatePatch patch, String status, String reasonCode) {
        try {
            FoundationTypes.AuditRef auditRef = audit.record(new AuditPort.AuditCommand(
                    auditType,
                    patch == null ? "synthetic-missing-patch" : patch.patchId,
                    patch == null ? "synthetic-missing-cdp" : patch.cdpId,
                    status,
                    reasonCode));
            return StatePatchBoundaryValidator.validAuditRef(auditRef) ? auditRef : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void releaseReservation(StateTypes.StatePatch patch, String fingerprint) {
        try {
            idempotency.release(patch.idempotencyKey, fingerprint);
        } catch (RuntimeException ignored) {
            // A retained reservation is fail-safe: it blocks a duplicate commit.
        }
    }

    private void emitSafely(String kind, StateTypes.CommitResult result, String now) {
        try {
            eventEvidence.record(new InternalCommitEventEvidence(
                    kind, result.patchId, result.cdpId, result.status, result.reasonCode, now));
        } catch (RuntimeException ignored) {
            // Non-authoritative evidence never changes any domain outcome.
        }
    }

    private static int previousVersionOrZero(StateTypes.StatePatch patch) {
        return patch == null || patch.baseVersion == null ? 0 : patch.baseVersion.intValue();
    }

    private static String firstOperationPath(StateTypes.StatePatch patch) {
        if (patch.operations.isEmpty() || patch.operations.get(0) == null
                || isBlank(patch.operations.get(0).path)) {
            return "/base_version";
        }
        return patch.operations.get(0).path;
    }

    private static <T> T requirePort(T port, String name) {
        if (port == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return port;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
