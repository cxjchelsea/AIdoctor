package com.aidoctor.diagnosis.runtime.u07;

/**
 * Synthetic-only integration of inbound preflight and F8 business classification.
 * A positive output is NEVER an execution/commit/clinical authority.
 * The two models consume caller-provided synthetic facts, not authenticated state.
 */
public final class U07SyntheticResumePipeline {
    public enum Stage { INVALID_COMPOSITION, BLOCKED_BY_ADMISSION, F8_CLASSIFIED }
    public static final class Result {
        public final Stage stage;
        public final U07InboundAdmission.Status admissionStatus;
        public final U07SyntheticF8Decision.Outcome f8Outcome;
        public final String reason;
        private Result(Stage stage, U07InboundAdmission.Status admissionStatus,
                       U07SyntheticF8Decision.Outcome f8Outcome, String reason) {
            this.stage = stage;
            this.admissionStatus = admissionStatus;
            this.f8Outcome = f8Outcome;
            this.reason = reason;
        }
    }

    private final U07InboundAdmission admission = new U07InboundAdmission();
    private final U07SyntheticF8Decision f8 = new U07SyntheticF8Decision();

    public Result evaluate(U07InboundAdmission.Input inbound, U07SyntheticF8Decision.Input business) {
        // Cross-contract equality is checked *before* interpreting either layer.
        // Never mix evidence for different canonical events or wait effects.
        if (inbound == null || business == null || inbound.eventType == null
                || business.eventType == null
                || !sameNonblank(inbound.eventId, business.eventId)
                || !sameNonblank(inbound.consultationId, business.consultationId)
                || !sameNonblank(inbound.sourceWaitEffectId, business.waitEffectId)
                || business.evidenceScope != U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED
                || !inbound.eventType.name().equals(business.eventType.name())) {
            return new Result(Stage.INVALID_COMPOSITION, null, null, "CROSS_INPUT_IDENTITY_OR_SCOPE_MISMATCH");
        }
        U07InboundAdmission.Result inboundResult = admission.evaluate(inbound);
        if (!inboundResult.mayEvaluateF8) {
            return new Result(Stage.BLOCKED_BY_ADMISSION, inboundResult.status, null, "ADMISSION_NOT_ELIGIBLE");
        }
        U07SyntheticF8Decision.Result businessResult = f8.decide(business);
        // F8 INSUFFICIENT_EVIDENCE remains unresolved, never treated as ACCEPTED.
        return new Result(Stage.F8_CLASSIFIED, inboundResult.status,
                businessResult.outcome, businessResult.reason);
    }

    private static boolean sameNonblank(String left, String right) {
        return left != null && !left.trim().isEmpty() && left.equals(right);
    }
}
