package com.aidoctor.diagnosis.runtime.u07;

/**
 * Synthetic-only composition: volatile event registration -> inbound gate -> F8.
 * FIRST_SEEN is NOT business ACCEPTED; SAME_EVENT_REPLAY is NOT APPLIED.
 *
 * No trustworthy source resolver, production ledger, mutation, P02 resume,
 * delivery, runtime side effects or clinical authority is implemented here.
 */
public final class U07SyntheticLedgerToF8Adapter {
    public enum Stage {
        INVALID_CROSS_INPUT,
        LEDGER_BLOCKED,
        RECONCILIATION_REQUIRED,
        ADMISSION_BLOCKED,
        F8_EVALUATED
    }

    public static final class Result {
        public final Stage stage;
        public final U07SyntheticCanonicalEventLedger.Status registrationStatus;
        public final U07InboundAdmission.Status admissionStatus;
        public final U07SyntheticF8Decision.Outcome f8Outcome;
        public final String reason;
        private Result(Stage stage, U07SyntheticCanonicalEventLedger.Status registrationStatus,
                       U07InboundAdmission.Status admissionStatus,
                       U07SyntheticF8Decision.Outcome f8Outcome, String reason) {
            this.stage = stage;
            this.registrationStatus = registrationStatus;
            this.admissionStatus = admissionStatus;
            this.f8Outcome = f8Outcome;
            this.reason = reason;
        }
    }

    private final U07SyntheticCanonicalEventLedger ledger;
    private final U07SyntheticResumePipeline pipeline;

    public U07SyntheticLedgerToF8Adapter(U07SyntheticCanonicalEventLedger ledger) {
        if (ledger == null) throw new IllegalArgumentException("ledger required");
        this.ledger = ledger;
        this.pipeline = new U07SyntheticResumePipeline();
    }

    public Result evaluate(U07CanonicalEventSource.Snapshot source,
                           U07InboundAdmission.Input inbound,
                           U07SyntheticF8Decision.Input business) {
        if (source == null || source.event == null || inbound == null || business == null
                || source.provenance != U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY
                || business.evidenceScope != U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED
                || !same(source.event.eventId, inbound.eventId)
                || !same(source.event.eventId, business.eventId)
                || !same(source.event.consultationId, inbound.consultationId)
                || !same(source.event.consultationId, business.consultationId)
                || !same(source.event.waitEffectId, inbound.sourceWaitEffectId)
                || !same(source.event.waitEffectId, business.waitEffectId)
                || !same(source.event.questionId, business.questionId)
                || source.event.eventType == null || inbound.eventType == null || business.eventType == null
                || !source.event.eventType.name().equals(inbound.eventType.name())
                || !source.event.eventType.name().equals(business.eventType.name())) {
            return result(Stage.INVALID_CROSS_INPUT, null, null, null, "CANONICAL_EVENT_INPUT_MISMATCH");
        }
        // Register only a synthetic event identity, not clinical/business application.
        U07SyntheticCanonicalEventLedger.Result registration = ledger.register(source);
        if (registration.status == U07SyntheticCanonicalEventLedger.Status.SAME_EVENT_REPLAY) {
            // No APPLIED evidence exists in this in-memory ledger. Never invent DUPLICATE.
            return result(Stage.RECONCILIATION_REQUIRED, registration.status, null, null,
                    "REGISTERED_BEFORE_APPLICATION_STATUS_UNKNOWN");
        }
        if (registration.status != U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN) {
            return result(Stage.LEDGER_BLOCKED, registration.status, null, null,
                    "LEDGER_REGISTRATION_NOT_NEW");
        }
        U07SyntheticResumePipeline.Result flow = pipeline.evaluate(inbound, business);
        if (flow.stage == U07SyntheticResumePipeline.Stage.INVALID_COMPOSITION) {
            return result(Stage.INVALID_CROSS_INPUT, registration.status, flow.admissionStatus,
                    null, "PIPELINE_COMPOSITION_CONFLICT");
        }
        if (flow.stage == U07SyntheticResumePipeline.Stage.BLOCKED_BY_ADMISSION) {
            return result(Stage.ADMISSION_BLOCKED, registration.status, flow.admissionStatus,
                    null, "INBOUND_ADMISSION_NOT_ELIGIBLE");
        }
        return result(Stage.F8_EVALUATED, registration.status, flow.admissionStatus,
                flow.f8Outcome, "SYNTHETIC_F8_ONLY_NO_APPLY");
    }

    private static boolean same(String a, String b) {
        return a != null && !a.trim().isEmpty() && a.equals(b);
    }

    private static Result result(Stage stage, U07SyntheticCanonicalEventLedger.Status registration,
                                 U07InboundAdmission.Status admission,
                                 U07SyntheticF8Decision.Outcome outcome, String reason) {
        return new Result(stage, registration, admission, outcome, reason);
    }
}
