package com.aidoctor.contracts.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * REVIEWED_BINDING：信封、标识、冲突、追踪与审计引用。
 */
public final class FoundationTypes {
    private FoundationTypes() {
    }

    public static class ContractEnvelope {
        @JsonProperty("contract_name")
        public String contractName;
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("message_id")
        public String messageId;
        @JsonProperty("correlation_id")
        public String correlationId;
        @JsonProperty("trace_id")
        public String traceId;
        @JsonProperty("created_at")
        public String createdAt;
        @JsonProperty("producer")
        public String producer;
        @JsonProperty("capability_id")
        public String capabilityId;
        @JsonProperty("capability_version")
        public String capabilityVersion;
    }

    public static class IdentifierSet {
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("cdp_id")
        public String cdpId;
        @JsonProperty("patient_id")
        public String patientId;
        @JsonProperty("encounter_id")
        public String encounterId;
        @JsonProperty("session_id")
        public String sessionId;
        @JsonProperty("tenant_id")
        public String tenantId;
        @JsonProperty("review_id")
        public String reviewId;
        @JsonProperty("delivery_id")
        public String deliveryId;
    }

    public static class ContractConflict {
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("conflict_id")
        public String conflictId;
        @JsonProperty("type")
        public String type;
        @JsonProperty("path")
        public String path;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("expected_version")
        public Integer expectedVersion;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("actual_version")
        public Integer actualVersion;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("expected_value")
        public Object expectedValue;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("actual_value")
        public Object actualValue;
        @JsonProperty("resolution")
        public String resolution;
        @JsonProperty("retryable")
        public Boolean retryable;
        @JsonProperty("details")
        public String details;
    }

    public static class TraceRef {
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("trace_id")
        public String traceId;
        @JsonProperty("trace_type")
        public String traceType;
        @JsonProperty("trace_version")
        public Integer traceVersion;
        @JsonProperty("created_at")
        public String createdAt;
        @JsonProperty("access_level")
        public String accessLevel;
        @JsonProperty("phi_capable")
        public Boolean phiCapable;
    }

    public static class AuditRef {
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("audit_id")
        public String auditId;
        @JsonProperty("audit_type")
        public String auditType;
        @JsonProperty("audit_version")
        public Integer auditVersion;
        @JsonProperty("created_at")
        public String createdAt;
        @JsonProperty("access_level")
        public String accessLevel;
        @JsonProperty("phi_capable")
        public Boolean phiCapable;
    }
}
