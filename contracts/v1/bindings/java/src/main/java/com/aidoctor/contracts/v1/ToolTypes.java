package com.aidoctor.contracts.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * REVIEWED_BINDING：v1 ToolContext / ToolResult，不是遗留 call_params / suggested_writes。
 */
public final class ToolTypes {
    private ToolTypes() {
    }

    public static class ToolActor {
        @JsonProperty("actor_id")
        public String actorId;
        @JsonProperty("actor_type")
        public String actorType;
    }

    public static class ToolCapability {
        @JsonProperty("capability_id")
        public String capabilityId;
        @JsonProperty("capability_version")
        public String capabilityVersion;
    }

    public static class CurrentStateRef {
        @JsonProperty("cdp_id")
        public String cdpId;
        @JsonProperty("version")
        public Integer version;
        @JsonProperty("read_fields")
        public List<String> readFields;
    }

    public static class AuthorizationScope {
        @JsonProperty("granted")
        public List<String> granted;
        @JsonProperty("requested")
        public List<String> requested;
    }

    public static class InputRef {
        @JsonProperty("ref_type")
        public String refType;
        @JsonProperty("ref_id")
        public String refId;
        @JsonProperty("ref_version")
        public Integer refVersion;
    }

    public static class ToolContext {
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("envelope")
        public FoundationTypes.ContractEnvelope envelope;
        @JsonProperty("actor")
        public ToolActor actor;
        @JsonProperty("identifiers")
        public FoundationTypes.IdentifierSet identifiers;
        @JsonProperty("capability")
        public ToolCapability capability;
        @JsonProperty("current_state_ref")
        public CurrentStateRef currentStateRef;
        @JsonProperty("authorization_scope")
        public AuthorizationScope authorizationScope;
        @JsonProperty("deadline")
        public String deadline;
        @JsonProperty("locale")
        public String locale;
        @JsonProperty("requested_operation")
        public String requestedOperation;
        @JsonProperty("input_refs")
        public List<InputRef> inputRefs;
    }

    public static class NamedValue {
        @JsonProperty("name")
        public String name;
        @JsonProperty("value")
        public Object value;
    }

    public static class ToolError {
        @JsonProperty("code")
        public String code;
        @JsonProperty("category")
        public String category;
        @JsonProperty("message")
        public String message;
        @JsonProperty("retryable")
        public Boolean retryable;
    }

    public static class ToolResult {
        @JsonProperty("contract_version")
        public String contractVersion;
        @JsonProperty("envelope")
        public FoundationTypes.ContractEnvelope envelope;
        @JsonProperty("tool_name")
        public String toolName;
        @JsonProperty("tool_version")
        public String toolVersion;
        @JsonProperty("invocation_id")
        public String invocationId;
        @JsonProperty("status")
        public String status;
        @JsonProperty("reason_code")
        public String reasonCode;
        @JsonProperty("retryable")
        public Boolean retryable;
        @JsonProperty("output")
        public List<NamedValue> output;
        @JsonProperty("suggested_patches")
        public List<StateTypes.StatePatch> suggestedPatches;
        @JsonProperty("evidence_refs")
        public List<String> evidenceRefs;
        @JsonProperty("errors")
        public List<ToolError> errors;
        @JsonProperty("started_at")
        public String startedAt;
        @JsonProperty("completed_at")
        public String completedAt;
    }
}
