package com.aidoctor.diagnosis.runtime.u07;

/**
 * Read-only, synthetic-only reconciliation of an adapter barrier with the
 * in-memory application-state fixture. This class NEVER grants a retry,
 * clinical mutation, runtime resume, U02 handoff, or real-world idempotency.
 */
public final class U07SyntheticReconciliationIntegration {
    public enum Resolution {
        NOT_A_RECONCILIATION_CASE,
        INVALID_EVIDENCE,
        APPLICATION_STATE_MISSING,
        APPLICATION_STATE_PENDING,
        APPLICATION_STATE_FAILED_REVIEW,
        SYNTHETIC_APPLIED_RECEIPT_SEEN,
        PROTECTED_IDENTITY_CONFLICT
    }
    public static final class Result {
        public final Resolution resolution;
        public final U07SyntheticEventApplicationState.Recovery applicationRecovery;
        public final long observedRevision;
        public final boolean mayResumeRuntime;
        public final boolean mayHandoffU02;
        private Result(Resolution resolution,
                       U07SyntheticEventApplicationState.Recovery recovery,
                       long observedRevision) {
            this.resolution = resolution;
            this.applicationRecovery = recovery;
            this.observedRevision = observedRevision;
            this.mayResumeRuntime = false;
            this.mayHandoffU02 = false;
        }
    }

    private final U07SyntheticEventApplicationState applications;

    public U07SyntheticReconciliationIntegration(U07SyntheticEventApplicationState applications) {
        if (applications == null) throw new IllegalArgumentException("applications required");
        this.applications = applications;
    }

    public Result inspect(U07SyntheticLedgerToF8Adapter.Result adapterResult,
                          U07CanonicalEventSource.Snapshot eventSource,
                          U07SyntheticEventApplicationState.EventKey protectedKey) {
        if (adapterResult == null
                || adapterResult.stage != U07SyntheticLedgerToF8Adapter.Stage.RECONCILIATION_REQUIRED) {
            return result(Resolution.NOT_A_RECONCILIATION_CASE, null);
        }
        // A source version/marker is only synthetic metadata, never authentication.
        if (eventSource == null || eventSource.event == null
                || eventSource.provenance != U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY
                || blank(eventSource.sourceVersion) || protectedKey == null
                || blank(protectedKey.eventId) || blank(protectedKey.payloadDigest)
                || !protectedKey.eventId.equals(eventSource.event.eventId)
                || !protectedKey.payloadDigest.equals(eventSource.event.payloadDigest)
                || !protectedKey.eventId.equals(adapterResult.protectedEventId)
                || !protectedKey.payloadDigest.equals(adapterResult.protectedPayloadDigest)
                || adapterResult.registrationStatus == null
                || (adapterResult.registrationStatus
                    != U07SyntheticCanonicalEventLedger.Status.SAME_EVENT_REPLAY
                    && adapterResult.registrationStatus
                    != U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN)) {
            return result(Resolution.INVALID_EVIDENCE, null);
        }
        U07SyntheticEventApplicationState.Review review = applications.reconcile(protectedKey);
        switch (review.recovery) {
            case NOT_FOUND:
                return result(Resolution.APPLICATION_STATE_MISSING, review);
            case NEEDS_RECONCILIATION:
                return result(Resolution.APPLICATION_STATE_PENDING, review);
            case FAILED_REQUIRES_REVIEW:
                return result(Resolution.APPLICATION_STATE_FAILED_REVIEW, review);
            case APPLIED_SYNTHETIC_RECEIPT_PRESENT:
                // Informational only: this in-memory fixture is NOT proof of a real effect.
                return result(Resolution.SYNTHETIC_APPLIED_RECEIPT_SEEN, review);
            case CONFLICT:
            default:
                return result(Resolution.PROTECTED_IDENTITY_CONFLICT, review);
        }
    }

    private static Result result(Resolution resolution, U07SyntheticEventApplicationState.Review review) {
        return new Result(resolution, review == null ? null : review.recovery,
                review == null || review.snapshot == null ? -1L : review.snapshot.revision);
    }
    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
