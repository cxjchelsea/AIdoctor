package com.aidoctor.diagnosis.state.committer;

import com.aidoctor.contracts.v1.ContractVersion;
import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.SharedContractsMapper;
import com.aidoctor.contracts.v1.StateTypes;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Builds frozen {@link StateTypes.CommitResult} shapes. Does not invent
 * new contract fields.
 */
final class CommitResultAssembler {
    private static final ObjectMapper MAPPER = SharedContractsMapper.create();
    private static final String PRODUCER = "state-committer";

    StateTypes.CommitResult copyOf(StateTypes.CommitResult original) {
        return MAPPER.convertValue(MAPPER.valueToTree(original), StateTypes.CommitResult.class);
    }

    StateTypes.CommitResult committed(
            StateTypes.StatePatch patch,
            int previousVersion,
            int committedVersion,
            String committedAt,
            FoundationTypes.AuditRef auditRef
    ) {
        StateTypes.CommitResult result = base(patch, "COMMITTED", previousVersion, committedAt, auditRef);
        result.committedVersion = Integer.valueOf(committedVersion);
        result.committedAt = committedAt;
        result.reasonCode = CommitReasonCodes.PATCH_COMMITTED;
        result.conflicts = emptyConflicts();
        result.rejectedOperations = emptyRejected();
        result.errors = emptyErrors();
        result.retryable = Boolean.FALSE;
        return result;
    }

    StateTypes.CommitResult rejected(
            StateTypes.StatePatch patch,
            int previousVersion,
            String createdAt,
            String reasonCode,
            List<StateTypes.RejectedOperation> rejectedOperations,
            FoundationTypes.AuditRef auditRef
    ) {
        StateTypes.CommitResult result = base(patch, "REJECTED", previousVersion, createdAt, auditRef);
        result.reasonCode = reasonCode;
        result.conflicts = emptyConflicts();
        result.rejectedOperations = rejectedOperations;
        result.errors = emptyErrors();
        result.retryable = Boolean.FALSE;
        return result;
    }

    StateTypes.CommitResult conflict(
            StateTypes.StatePatch patch,
            int previousVersion,
            String createdAt,
            String reasonCode,
            FoundationTypes.ContractConflict conflict,
            FoundationTypes.AuditRef auditRef
    ) {
        StateTypes.CommitResult result = base(patch, "CONFLICT", previousVersion, createdAt, auditRef);
        result.reasonCode = reasonCode;
        List<FoundationTypes.ContractConflict> conflicts = new ArrayList<FoundationTypes.ContractConflict>();
        conflicts.add(conflict);
        result.conflicts = conflicts;
        result.rejectedOperations = emptyRejected();
        result.errors = emptyErrors();
        result.retryable = Boolean.TRUE;
        return result;
    }

    StateTypes.CommitResult failed(
            StateTypes.StatePatch patch,
            int previousVersion,
            String createdAt,
            String errorCode,
            String errorMessage,
            FoundationTypes.AuditRef auditRef,
            boolean retryable
    ) {
        StateTypes.CommitResult result = base(patch, "FAILED", previousVersion, createdAt, auditRef);
        result.reasonCode = errorCode;
        result.conflicts = emptyConflicts();
        result.rejectedOperations = emptyRejected();
        List<StateTypes.CommitError> errors = new ArrayList<StateTypes.CommitError>();
        StateTypes.CommitError error = new StateTypes.CommitError();
        error.code = errorCode;
        error.message = errorMessage;
        errors.add(error);
        result.errors = errors;
        result.retryable = Boolean.valueOf(retryable);
        return result;
    }

    FoundationTypes.ContractConflict versionMismatch(
            String conflictId,
            int expectedVersion,
            int actualVersion,
            String path
    ) {
        FoundationTypes.ContractConflict conflict = new FoundationTypes.ContractConflict();
        conflict.contractVersion = ContractVersion.CONTRACT_VERSION;
        conflict.conflictId = conflictId;
        conflict.type = "VERSION_MISMATCH";
        conflict.path = path;
        conflict.expectedVersion = Integer.valueOf(expectedVersion);
        conflict.actualVersion = Integer.valueOf(actualVersion);
        conflict.expectedValue = null;
        conflict.actualValue = null;
        conflict.resolution = "RETRY_WITH_CURRENT_VERSION";
        conflict.retryable = Boolean.TRUE;
        conflict.details = "Caller base_version does not match internal current_version.";
        return conflict;
    }

    FoundationTypes.ContractConflict idempotencyMismatch(String conflictId, Integer baseVersion) {
        FoundationTypes.ContractConflict conflict = new FoundationTypes.ContractConflict();
        conflict.contractVersion = ContractVersion.CONTRACT_VERSION;
        conflict.conflictId = conflictId;
        conflict.type = "IDEMPOTENCY_MISMATCH";
        conflict.path = "/idempotency_key";
        conflict.expectedVersion = baseVersion;
        conflict.actualVersion = null;
        conflict.expectedValue = null;
        conflict.actualValue = null;
        conflict.resolution = "REJECT_PATCH";
        conflict.retryable = Boolean.FALSE;
        conflict.details = "Same idempotency_key refers to a different logical StatePatch.";
        return conflict;
    }

    List<StateTypes.RejectedOperation> rejectAll(StateTypes.StatePatch patch, String reasonCode) {
        List<StateTypes.RejectedOperation> rejected = new ArrayList<StateTypes.RejectedOperation>();
        int size = patch == null || patch.operations == null ? 0 : patch.operations.size();
        if (size == 0) {
            rejected.add(rejectedOperation(0, reasonCode));
            return rejected;
        }
        for (int index = 0; index < size; index++) {
            rejected.add(rejectedOperation(index, reasonCode));
        }
        return rejected;
    }

    StateTypes.RejectedOperation rejectedOperation(int index, String reasonCode) {
        StateTypes.RejectedOperation operation = new StateTypes.RejectedOperation();
        operation.operationIndex = Integer.valueOf(index);
        operation.reasonCode = reasonCode;
        return operation;
    }

    FoundationTypes.AuditRef fallbackAuditRef(String createdAt) {
        return fallbackAuditRef(createdAt, "STATE_PATCH_REQUESTED");
    }

    private FoundationTypes.AuditRef fallbackAuditRef(String createdAt, String auditType) {
        FoundationTypes.AuditRef auditRef = new FoundationTypes.AuditRef();
        auditRef.contractVersion = ContractVersion.CONTRACT_VERSION;
        auditRef.auditId = "synthetic-audit-fallback";
        auditRef.auditType = auditType;
        auditRef.auditVersion = Integer.valueOf(1);
        auditRef.createdAt = createdAt;
        auditRef.accessLevel = "INTERNAL";
        auditRef.phiCapable = Boolean.FALSE;
        return auditRef;
    }

    private StateTypes.CommitResult base(
            StateTypes.StatePatch patch,
            String status,
            int previousVersion,
            String createdAt,
            FoundationTypes.AuditRef auditRef
    ) {
        StateTypes.CommitResult result = new StateTypes.CommitResult();
        result.contractVersion = ContractVersion.CONTRACT_VERSION;
        result.envelope = envelope(patch, status, createdAt);
        result.patchId = patch == null || isBlank(patch.patchId) ? "synthetic-missing-patch" : patch.patchId;
        result.cdpId = patch == null || isBlank(patch.cdpId) ? "synthetic-missing-cdp" : patch.cdpId;
        result.status = status;
        result.previousVersion = Integer.valueOf(previousVersion);
        result.auditRef = auditRef;
        return result;
    }

    private FoundationTypes.ContractEnvelope envelope(
            StateTypes.StatePatch patch,
            String status,
            String createdAt
    ) {
        FoundationTypes.ContractEnvelope envelope = new FoundationTypes.ContractEnvelope();
        envelope.contractName = "CommitResult";
        envelope.contractVersion = ContractVersion.CONTRACT_VERSION;
        envelope.messageId = stableId(
                "synthetic-commit-",
                status + "|" + safePatchValue(patch == null ? null : patch.patchId)
                        + "|" + safePatchValue(patch == null ? null : patch.idempotencyKey)
        );
        envelope.createdAt = createdAt;
        envelope.producer = PRODUCER;
        if (patch != null && patch.envelope != null) {
            envelope.correlationId = blankToDefault(patch.envelope.correlationId, "synthetic-correlation");
            envelope.traceId = blankToDefault(patch.envelope.traceId, "synthetic-trace");
            envelope.capabilityId = blankToDefault(patch.envelope.capabilityId, "synthetic-capability-v1");
            envelope.capabilityVersion = blankToDefault(patch.envelope.capabilityVersion, "1.0.0");
        } else {
            envelope.correlationId = "synthetic-correlation";
            envelope.traceId = "synthetic-trace";
            envelope.capabilityId = "synthetic-capability-v1";
            envelope.capabilityVersion = "1.0.0";
        }
        return envelope;
    }

    private static List<FoundationTypes.ContractConflict> emptyConflicts() {
        return new ArrayList<FoundationTypes.ContractConflict>();
    }

    private static List<StateTypes.RejectedOperation> emptyRejected() {
        return new ArrayList<StateTypes.RejectedOperation>();
    }

    private static List<StateTypes.CommitError> emptyErrors() {
        return new ArrayList<StateTypes.CommitError>();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static String blankToDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    String conflictId(StateTypes.StatePatch patch, String type) {
        return stableId(
                "synthetic-conflict-",
                type + "|" + safePatchValue(patch == null ? null : patch.patchId)
                        + "|" + safePatchValue(patch == null ? null : patch.idempotencyKey)
        );
    }

    private static String stableId(String prefix, String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(prefix);
            for (byte item : bytes) {
                value.append(String.format("%02x", Integer.valueOf(item & 0xff)));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    private static String safePatchValue(String value) {
        return value == null ? "missing" : value;
    }
}
