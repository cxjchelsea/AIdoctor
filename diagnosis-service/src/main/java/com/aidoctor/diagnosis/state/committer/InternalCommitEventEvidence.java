package com.aidoctor.diagnosis.state.committer;

/**
 * Non-authoritative internal evidence for tests and later observability.
 * Not a Shared Contract, not SoR, and not clinical truth.
 */
public final class InternalCommitEventEvidence {
    public static final String KIND_COMMITTED = "STATE_COMMIT_COMMITTED";
    public static final String KIND_REJECTED = "STATE_COMMIT_REJECTED";
    public static final String KIND_CONFLICT = "STATE_COMMIT_CONFLICT";
    public static final String KIND_FAILED = "STATE_COMMIT_FAILED";
    public static final String KIND_REPLAYED = "STATE_COMMIT_REPLAYED";

    public final String eventKind;
    public final String patchId;
    public final String cdpId;
    public final String status;
    public final String reasonCode;
    public final boolean authoritative;
    public final String createdAt;

    public InternalCommitEventEvidence(
            String eventKind,
            String patchId,
            String cdpId,
            String status,
            String reasonCode,
            String createdAt
    ) {
        this.eventKind = eventKind;
        this.patchId = patchId;
        this.cdpId = cdpId;
        this.status = status;
        this.reasonCode = reasonCode;
        this.authoritative = false;
        this.createdAt = createdAt;
    }
}
