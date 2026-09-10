package com.aidoctor.diagnosis.state.committer;

/**
 * Deterministic reason codes. Pattern matches Shared Contracts v1
 * {@code ^[A-Z][A-Z0-9_]*$}. Not a new Shared Contract.
 */
public final class CommitReasonCodes {
    public static final String PATCH_COMMITTED = "PATCH_COMMITTED";
    public static final String CONTRACT_IDENTITY_INVALID = "CONTRACT_IDENTITY_INVALID";
    public static final String CAPABILITY_NOT_AUTHORIZED = "CAPABILITY_NOT_AUTHORIZED";
    public static final String CONSENT_NOT_AUTHORIZED = "CONSENT_NOT_AUTHORIZED";
    public static final String FIELD_NOT_AUTHORIZED = "FIELD_NOT_AUTHORIZED";
    public static final String SOURCE_NOT_AUTHORIZED = "SOURCE_NOT_AUTHORIZED";
    public static final String OPERATION_NOT_AUTHORIZED = "OPERATION_NOT_AUTHORIZED";
    public static final String VERSION_MISMATCH = "VERSION_MISMATCH";
    public static final String IDEMPOTENCY_MISMATCH = "IDEMPOTENCY_MISMATCH";
    public static final String IDEMPOTENCY_RESULT_UNAVAILABLE = "IDEMPOTENCY_RESULT_UNAVAILABLE";
    public static final String IDEMPOTENCY_INFRASTRUCTURE_FAILURE = "IDEMPOTENCY_INFRASTRUCTURE_FAILURE";
    public static final String REPOSITORY_INTERNAL_FAILURE = "REPOSITORY_INTERNAL_FAILURE";
    public static final String STATE_OPERATION_APPLICATION_FAILED = "STATE_OPERATION_APPLICATION_FAILED";
    public static final String STATE_OPERATION_SEMANTICS_UNSUPPORTED = "STATE_OPERATION_SEMANTICS_UNSUPPORTED";
    public static final String STATE_REPOSITORY_COMMAND_INVALID = "STATE_REPOSITORY_COMMAND_INVALID";
    public static final String AUDIT_INFRASTRUCTURE_FAILURE = "AUDIT_INFRASTRUCTURE_FAILURE";
    public static final String INVARIANT_FAILURE = "INVARIANT_FAILURE";

    private CommitReasonCodes() {
    }
}
