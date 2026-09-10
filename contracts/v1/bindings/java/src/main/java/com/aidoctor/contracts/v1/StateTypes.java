package com.aidoctor.contracts.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * REVIEWED_BINDING：StatePatch 为提案，CommitResult 为提交结果，二者不可互换。
 */
public final class StateTypes {
    private StateTypes() {
    }

    public static class StatePatchOperation {
        @JsonProperty("op")
        public String op;
        @JsonProperty("path")
        public String path;
        @JsonProperty("value")
        public Object value;
        @JsonProperty("expected_current_value")
        public Object expectedCurrentValue;
        @JsonProperty("source")
        public String source;
        @JsonProperty("sensitivity")
        public String sensitivity;
    }

    public static class StatePatch {
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("envelope")
        public FoundationTypes.ContractEnvelope envelope;
        @JsonProperty("cdp_id")
        public String cdpId;
        @JsonProperty("base_version")
        public Integer baseVersion;
        @JsonProperty("patch_id")
        public String patchId;
        @JsonProperty("idempotency_key")
        public String idempotencyKey;
        @JsonProperty("operations")
        public List<StatePatchOperation> operations;
        @JsonProperty("reason_code")
        public String reasonCode;
        @JsonProperty("evidence_refs")
        public List<String> evidenceRefs;
        @JsonProperty("producer")
        public String producer;
        @JsonProperty("created_at")
        public String createdAt;
    }

    public static class RejectedOperation {
        @JsonProperty("operation_index")
        public Integer operationIndex;
        @JsonProperty("reason_code")
        public String reasonCode;
    }

    public static class CommitError {
        @JsonProperty("code")
        public String code;
        @JsonProperty("message")
        public String message;
    }

    public static class CommitResult {
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("envelope")
        public FoundationTypes.ContractEnvelope envelope;
        @JsonProperty("patch_id")
        public String patchId;
        @JsonProperty("cdp_id")
        public String cdpId;
        @JsonProperty("status")
        public String status;
        @JsonProperty("previous_version")
        public Integer previousVersion;
        @JsonProperty("committed_version")
        public Integer committedVersion;
        @JsonProperty("committed_at")
        public String committedAt;
        @JsonProperty("reason_code")
        public String reasonCode;
        @JsonProperty("conflicts")
        public List<FoundationTypes.ContractConflict> conflicts;
        @JsonProperty("rejected_operations")
        public List<RejectedOperation> rejectedOperations;
        @JsonProperty("errors")
        public List<CommitError> errors;
        @JsonProperty("audit_ref")
        public FoundationTypes.AuditRef auditRef;
        @JsonProperty("retryable")
        public Boolean retryable;
    }
}
