package com.aidoctor.diagnosis.state.committer;

import com.aidoctor.contracts.v1.ContractVersion;
import com.aidoctor.contracts.v1.ExactVersion;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pure mechanical State Committer core.
 *
 * <p>Receives an already parsed Shared Contracts v1 {@link StateTypes.StatePatch}
 * and returns a frozen {@link StateTypes.CommitResult}. No Spring wiring.
 */
public final class StateCommitter {
    private static final Set<String> ALLOWED_OPERATIONS = new HashSet<String>(
            Arrays.asList("ADD", "REPLACE", "REMOVE", "TEST")
    );

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
    private final AtomicInteger conflictSequence = new AtomicInteger(1);

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
        this(
                stateRepository,
                capabilityPolicy,
                fieldPermission,
                consentPolicy,
                sourceValidation,
                idempotency,
                audit,
                eventEvidence,
                Clock.systemUTC()
        );
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
        if (clock == null) {
            throw new IllegalArgumentException("clock is required");
        }
        this.clock = clock;
    }

    public StateTypes.CommitResult commit(StateTypes.StatePatch patch) {
        String now = now();
        try {
            return doCommit(patch, now);
        } catch (RuntimeException exception) {
            return finishFailed(
                    patch,
                    previousVersionOrZero(patch),
                    now,
                    CommitReasonCodes.INVARIANT_FAILURE,
                    "Unexpected mechanical failure."
            );
        }
    }

    private StateTypes.CommitResult doCommit(StateTypes.StatePatch patch, String now) {
        IdentityCheck identity = checkIdentity(patch);
        if (!identity.valid) {
            int previous = previousVersionOrZero(patch);
            if (identity.failed) {
                return finishFailed(
                        patch,
                        previous,
                        now,
                        CommitReasonCodes.INVARIANT_FAILURE,
                        identity.message
                );
            }
            return finishRejected(
                    patch,
                    previous,
                    now,
                    CommitReasonCodes.CONTRACT_IDENTITY_INVALID,
                    assembler.rejectAll(patch, CommitReasonCodes.CONTRACT_IDENTITY_INVALID)
            );
        }

        String fingerprint = CanonicalPatchFingerprint.fingerprint(patch);
        IdempotencyPort.IdempotencyRecord existing = idempotency.lookup(patch.idempotencyKey).orElse(null);
        if (existing != null) {
            if (fingerprint.equals(existing.canonicalFingerprint)) {
                emitAfterAuthoritativeCommit(
                        InternalCommitEventEvidence.KIND_REPLAYED,
                        existing.originalResult,
                        now
                );
                return assembler.copyOf(existing.originalResult);
            }
            return finishConflict(
                    patch,
                    previousVersionOrZero(patch),
                    now,
                    CommitReasonCodes.IDEMPOTENCY_MISMATCH,
                    assembler.idempotencyMismatch(nextConflictId(), patch.baseVersion)
            );
        }

        String capabilityId = patch.envelope.capabilityId;
        String capabilityVersion = patch.envelope.capabilityVersion;
        CapabilityPolicyPort.CapabilityDecision capabilityDecision =
                capabilityPolicy.evaluate(capabilityId, capabilityVersion);
        if (!capabilityDecision.isAuthorized()) {
            return finishRejected(
                    patch,
                    previousVersionOrZero(patch),
                    now,
                    CommitReasonCodes.CAPABILITY_NOT_AUTHORIZED,
                    assembler.rejectAll(patch, CommitReasonCodes.CAPABILITY_NOT_AUTHORIZED)
            );
        }

        ConsentPolicyPort.ConsentDecision consentDecision = consentPolicy.evaluate(patch.cdpId, capabilityId);
        if (!consentDecision.isAuthorized()) {
            return finishRejected(
                    patch,
                    previousVersionOrZero(patch),
                    now,
                    CommitReasonCodes.CONSENT_NOT_AUTHORIZED,
                    assembler.rejectAll(patch, CommitReasonCodes.CONSENT_NOT_AUTHORIZED)
            );
        }

        List<StateTypes.RejectedOperation> rejectedOperations = new ArrayList<StateTypes.RejectedOperation>();
        String rejectionReason = null;
        for (int index = 0; index < patch.operations.size(); index++) {
            StateTypes.StatePatchOperation operation = patch.operations.get(index);
            if (operation == null || operation.op == null || !ALLOWED_OPERATIONS.contains(operation.op)) {
                rejectedOperations.add(assembler.rejectedOperation(index, CommitReasonCodes.OPERATION_NOT_AUTHORIZED));
                rejectionReason = CommitReasonCodes.OPERATION_NOT_AUTHORIZED;
                continue;
            }
            FieldPermissionPort.FieldPermissionDecision fieldDecision =
                    fieldPermission.evaluate(operation.path, capabilityId);
            if (!fieldDecision.authorized) {
                rejectedOperations.add(assembler.rejectedOperation(index, CommitReasonCodes.FIELD_NOT_AUTHORIZED));
                if (rejectionReason == null) {
                    rejectionReason = CommitReasonCodes.FIELD_NOT_AUTHORIZED;
                }
                continue;
            }
            SourceValidationPort.SourceDecision sourceDecision = sourceValidation.evaluate(operation.source);
            if (!sourceDecision.authorized) {
                rejectedOperations.add(assembler.rejectedOperation(index, CommitReasonCodes.SOURCE_NOT_AUTHORIZED));
                if (rejectionReason == null) {
                    rejectionReason = CommitReasonCodes.SOURCE_NOT_AUTHORIZED;
                }
            }
        }
        if (!rejectedOperations.isEmpty()) {
            return finishRejected(
                    patch,
                    previousVersionOrZero(patch),
                    now,
                    rejectionReason,
                    rejectedOperations
            );
        }

        int currentVersion;
        try {
            currentVersion = stateRepository.readCurrentVersion(patch.cdpId);
        } catch (RuntimeException exception) {
            return finishFailed(
                    patch,
                    previousVersionOrZero(patch),
                    now,
                    CommitReasonCodes.REPOSITORY_INTERNAL_FAILURE,
                    "Repository version read failed."
            );
        }
        if (currentVersion != patch.baseVersion.intValue()) {
            return finishConflict(
                    patch,
                    currentVersion,
                    now,
                    CommitReasonCodes.VERSION_MISMATCH,
                    assembler.versionMismatch(
                            nextConflictId(),
                            patch.baseVersion.intValue(),
                            currentVersion,
                            firstOperationPath(patch)
                    )
            );
        }

        StateRepositoryPort.AtomicCommitOutcome outcome;
        try {
            outcome = stateRepository.attemptAtomicCommit(new StateRepositoryPort.AtomicCommitCommand(
                    patch.cdpId,
                    patch.baseVersion.intValue(),
                    patch.patchId,
                    patch.idempotencyKey
            ));
        } catch (RuntimeException exception) {
            return finishFailed(
                    patch,
                    currentVersion,
                    now,
                    CommitReasonCodes.REPOSITORY_INTERNAL_FAILURE,
                    "Repository commit attempt failed."
            );
        }
        if (outcome == null || outcome.status == StateRepositoryPort.AtomicCommitOutcome.Status.FAILED) {
            return finishFailed(
                    patch,
                    currentVersion,
                    now,
                    outcome == null || isBlank(outcome.failureCode)
                            ? CommitReasonCodes.REPOSITORY_INTERNAL_FAILURE
                            : outcome.failureCode,
                    outcome == null || isBlank(outcome.failureMessage)
                            ? "Repository atomic commit failed."
                            : outcome.failureMessage
            );
        }
        if (outcome.status == StateRepositoryPort.AtomicCommitOutcome.Status.CONFLICT) {
            int actual = outcome.actualVersion == null ? currentVersion : outcome.actualVersion.intValue();
            return finishConflict(
                    patch,
                    actual,
                    now,
                    CommitReasonCodes.VERSION_MISMATCH,
                    assembler.versionMismatch(
                            nextConflictId(),
                            patch.baseVersion.intValue(),
                            actual,
                            firstOperationPath(patch)
                    )
            );
        }
        if (outcome.committedVersion == null
                || outcome.previousVersion == null
                || outcome.committedVersion.intValue() != outcome.previousVersion.intValue() + 1) {
            return finishFailed(
                    patch,
                    currentVersion,
                    now,
                    CommitReasonCodes.INVARIANT_FAILURE,
                    "Committed version was not previous_version + 1."
            );
        }

        return finishCommitted(
                patch,
                fingerprint,
                outcome.previousVersion.intValue(),
                outcome.committedVersion.intValue(),
                now
        );
    }

    private StateTypes.CommitResult finishCommitted(
            StateTypes.StatePatch patch,
            String fingerprint,
            int previousVersion,
            int committedVersion,
            String now
    ) {
        FoundationTypes.AuditRef auditRef = recordAudit("STATE_COMMITTED", patch, "COMMITTED", CommitReasonCodes.PATCH_COMMITTED, now);
        if (auditRef == null) {
            auditRef = assembler.fallbackCommittedAuditRef(now);
        }
        StateTypes.CommitResult result = assembler.committed(
                patch,
                previousVersion,
                committedVersion,
                now,
                auditRef
        );
        rememberAfterAuthoritativeCommit(patch, fingerprint, result);
        emitAfterAuthoritativeCommit(InternalCommitEventEvidence.KIND_COMMITTED, result, now);
        return result;
    }

    /**
     * The repository has already committed authoritative state before this
     * method is reached. Idempotency persistence is auxiliary PBNC-01
     * evidence and must never downgrade that authoritative result.
     */
    private void rememberAfterAuthoritativeCommit(
            StateTypes.StatePatch patch,
            String fingerprint,
            StateTypes.CommitResult result
    ) {
        try {
            remember(patch, fingerprint, result);
        } catch (RuntimeException ignored) {
            // Authoritative state already advanced. Preserve COMMITTED.
        }
    }

    /**
     * Internal commit event evidence is explicitly non-authoritative. A sink
     * failure after the repository commit cannot invert the commit result.
     */
    private void emitAfterAuthoritativeCommit(
            String kind,
            StateTypes.CommitResult result,
            String now
    ) {
        try {
            emit(kind, result, now);
        } catch (RuntimeException ignored) {
            // Authoritative state already advanced. Preserve COMMITTED.
        }
    }

    private StateTypes.CommitResult finishRejected(
            StateTypes.StatePatch patch,
            int previousVersion,
            String now,
            String reasonCode,
            List<StateTypes.RejectedOperation> rejectedOperations
    ) {
        FoundationTypes.AuditRef auditRef = recordAudit("STATE_PATCH_REQUESTED", patch, "REJECTED", reasonCode, now);
        if (auditRef == null) {
            return failedAfterAudit(patch, previousVersion, now);
        }
        StateTypes.CommitResult result = assembler.rejected(
                patch,
                previousVersion,
                now,
                reasonCode,
                rejectedOperations,
                auditRef
        );
        remember(patch, CanonicalPatchFingerprint.fingerprint(patch), result);
        emit(InternalCommitEventEvidence.KIND_REJECTED, result, now);
        return result;
    }

    private StateTypes.CommitResult finishConflict(
            StateTypes.StatePatch patch,
            int previousVersion,
            String now,
            String reasonCode,
            FoundationTypes.ContractConflict conflict
    ) {
        FoundationTypes.AuditRef auditRef = recordAudit("STATE_PATCH_REQUESTED", patch, "CONFLICT", reasonCode, now);
        if (auditRef == null) {
            return failedAfterAudit(patch, previousVersion, now);
        }
        StateTypes.CommitResult result = assembler.conflict(
                patch,
                previousVersion,
                now,
                reasonCode,
                conflict,
                auditRef
        );
        if (!CommitReasonCodes.IDEMPOTENCY_MISMATCH.equals(reasonCode)) {
            remember(patch, CanonicalPatchFingerprint.fingerprint(patch), result);
        }
        emit(InternalCommitEventEvidence.KIND_CONFLICT, result, now);
        return result;
    }

    private StateTypes.CommitResult finishFailed(
            StateTypes.StatePatch patch,
            int previousVersion,
            String now,
            String errorCode,
            String errorMessage
    ) {
        FoundationTypes.AuditRef auditRef = recordAudit("STATE_PATCH_REQUESTED", patch, "FAILED", errorCode, now);
        if (auditRef == null) {
            auditRef = assembler.fallbackAuditRef(now);
        }
        StateTypes.CommitResult result = assembler.failed(
                patch,
                previousVersion,
                now,
                errorCode,
                errorMessage,
                auditRef
        );
        emit(InternalCommitEventEvidence.KIND_FAILED, result, now);
        return result;
    }

    private StateTypes.CommitResult failedAfterAudit(StateTypes.StatePatch patch, int previousVersion, String now) {
        StateTypes.CommitResult result = assembler.failed(
                patch,
                previousVersion,
                now,
                CommitReasonCodes.AUDIT_INFRASTRUCTURE_FAILURE,
                "Audit infrastructure failed.",
                assembler.fallbackAuditRef(now)
        );
        emit(InternalCommitEventEvidence.KIND_FAILED, result, now);
        return result;
    }

    private FoundationTypes.AuditRef recordAudit(
            String auditType,
            StateTypes.StatePatch patch,
            String status,
            String reasonCode,
            String now
    ) {
        try {
            FoundationTypes.AuditRef auditRef = audit.record(new AuditPort.AuditCommand(
                    auditType,
                    patch == null ? "synthetic-missing-patch" : patch.patchId,
                    patch == null ? "synthetic-missing-cdp" : patch.cdpId,
                    status,
                    reasonCode
            ));
            if (auditRef == null || isBlank(auditRef.auditId) || isBlank(auditRef.auditType)) {
                return null;
            }
            if (auditRef.phiCapable == null) {
                auditRef.phiCapable = Boolean.FALSE;
            }
            if (isBlank(auditRef.createdAt)) {
                auditRef.createdAt = now;
            }
            if (isBlank(auditRef.contractVersion)) {
                auditRef.contractVersion = ContractVersion.CONTRACT_VERSION;
            }
            if (auditRef.auditVersion == null) {
                auditRef.auditVersion = Integer.valueOf(1);
            }
            if (isBlank(auditRef.accessLevel)) {
                auditRef.accessLevel = "INTERNAL";
            }
            return auditRef;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void remember(StateTypes.StatePatch patch, String fingerprint, StateTypes.CommitResult result) {
        if (patch == null || isBlank(patch.idempotencyKey) || fingerprint == null) {
            return;
        }
        idempotency.remember(patch.idempotencyKey, fingerprint, assembler.copyOf(result));
    }

    private void emit(String kind, StateTypes.CommitResult result, String now) {
        eventEvidence.record(new InternalCommitEventEvidence(
                kind,
                result.patchId,
                result.cdpId,
                result.status,
                result.reasonCode,
                now
        ));
    }

    private IdentityCheck checkIdentity(StateTypes.StatePatch patch) {
        if (patch == null) {
            return IdentityCheck.failed("StatePatch is required.");
        }
        try {
            ExactVersion.requireExact(patch.contractVersion);
        } catch (IllegalArgumentException exception) {
            return IdentityCheck.rejected();
        }
        if (patch.envelope == null) {
            return IdentityCheck.rejected();
        }
        if (!"StatePatch".equals(patch.envelope.contractName)) {
            return IdentityCheck.rejected();
        }
        try {
            ExactVersion.requireExact(patch.envelope.contractVersion);
        } catch (IllegalArgumentException exception) {
            return IdentityCheck.rejected();
        }
        if (isBlank(patch.cdpId) || isBlank(patch.patchId) || isBlank(patch.idempotencyKey)) {
            return IdentityCheck.rejected();
        }
        if (patch.baseVersion == null || patch.operations == null || patch.operations.isEmpty()) {
            return IdentityCheck.rejected();
        }
        if (isBlank(patch.envelope.capabilityId) || isBlank(patch.envelope.capabilityVersion)) {
            return IdentityCheck.rejected();
        }
        return IdentityCheck.ok();
    }

    private String nextConflictId() {
        return "synthetic-conflict-" + conflictSequence.getAndIncrement();
    }

    private String now() {
        return clock.instant().toString();
    }

    private static int previousVersionOrZero(StateTypes.StatePatch patch) {
        if (patch == null || patch.baseVersion == null) {
            return 0;
        }
        return patch.baseVersion.intValue();
    }

    private static String firstOperationPath(StateTypes.StatePatch patch) {
        if (patch == null || patch.operations == null || patch.operations.isEmpty()
                || patch.operations.get(0) == null || isBlank(patch.operations.get(0).path)) {
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

    private static final class IdentityCheck {
        final boolean valid;
        final boolean failed;
        final String message;

        private IdentityCheck(boolean valid, boolean failed, String message) {
            this.valid = valid;
            this.failed = failed;
            this.message = message;
        }

        static IdentityCheck ok() {
            return new IdentityCheck(true, false, null);
        }

        static IdentityCheck rejected() {
            return new IdentityCheck(false, false, null);
        }

        static IdentityCheck failed(String message) {
            return new IdentityCheck(false, true, message);
        }
    }
}
