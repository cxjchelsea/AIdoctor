package com.aidoctor.contracts.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.List;

/**
 * Structural Java binding for StatePatch and CommitResult. Cross-field semantics remain owned by the release validator.
 */
public final class StateTypes {
    private StateTypes() {
    }

    public abstract static class StatePatchOperation {
        @JsonProperty("op")
        public String op;
        @JsonProperty("path")
        public String path;
        @JsonProperty("source")
        public String source;
        @JsonProperty("sensitivity")
        public String sensitivity;
    }

    public static class LegacyStatePatchOperation extends StatePatchOperation {
        @JsonProperty("value")
        public JsonNode value;
        @JsonProperty("expected_current_value")
        public JsonNode expectedCurrentValue;
    }

    public abstract static class CanonicalObservationOperation extends StatePatchOperation {
    }

    public static class CanonicalObservationAddOperation extends CanonicalObservationOperation {
        @JsonProperty("value")
        public ClinicalTypes.ClinicalObservation value;
    }

    public static class CanonicalObservationReplaceOperation extends CanonicalObservationOperation {
        @JsonProperty("value")
        public ClinicalTypes.ClinicalObservation value;
    }

    public static class CanonicalObservationRemoveOperation extends CanonicalObservationOperation {
    }

    public static class StatePatchOperationDeserializer extends JsonDeserializer<StatePatchOperation> {
        @Override
        public StatePatchOperation deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            String path = node.has("path") && node.get("path").isTextual() ? node.get("path").asText() : "";
            String op = node.has("op") && node.get("op").isTextual() ? node.get("op").asText() : "";
            Class<? extends StatePatchOperation> target;
            if (path.startsWith("/observations/")) {
                if ("ADD".equals(op)) {
                    target = CanonicalObservationAddOperation.class;
                } else if ("REPLACE".equals(op)) {
                    target = CanonicalObservationReplaceOperation.class;
                } else if ("REMOVE".equals(op)) {
                    target = CanonicalObservationRemoveOperation.class;
                } else {
                    throw JsonMappingException.from(parser, "unsupported canonical observation operation: " + op);
                }
            } else {
                target = LegacyStatePatchOperation.class;
            }
            return parser.getCodec().treeToValue(node, target);
        }
    }

    public static class StatePatch {
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("envelope")
        public FoundationTypes.ContractEnvelope envelope;
        @JsonProperty("cdp_id")
        public String cdpId;
        @JsonProperty("encounter_id")
        public String encounterId;
        @JsonProperty("base_version")
        public Long baseVersion;
        @JsonProperty("patch_id")
        public String patchId;
        @JsonProperty("idempotency_key")
        public String idempotencyKey;
        @JsonProperty("operations")
        @JsonDeserialize(contentUsing = StatePatchOperationDeserializer.class)
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
        public Long operationIndex;
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
        @JsonProperty("encounter_id")
        public String encounterId;
        @JsonProperty("status")
        public String status;
        @JsonProperty("previous_version")
        public Long previousVersion;
        @JsonProperty("committed_version")
        public Long committedVersion;
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
