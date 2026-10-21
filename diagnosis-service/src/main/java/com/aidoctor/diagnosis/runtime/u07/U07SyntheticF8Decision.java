package com.aidoctor.diagnosis.runtime.u07;

/**
 * Synthetic-only, pure F8 decision model. All evidence is supplied by an
 * explicitly synthetic test caller; this is not an authoritative production
 * event/state readback or permission to apply a resume.
 *
 * Business ACCEPTED is separate from P02 checkpoint compatibility, U02
 * handoff, canonical ledger mutation and any Clinical Truth.
 */
public final class U07SyntheticF8Decision {
    public enum Outcome { ACCEPTED, DUPLICATE, EXPIRED, REJECTED, INSUFFICIENT_EVIDENCE }
    public enum EventType { USER_ANSWER, RESUME_REQUEST }
    public enum Lifecycle { WAITING_USER, EXPIRED, CANCELLED, TERMINAL, UNKNOWN }
    public enum Question { CURRENT_DELIVERED, EXPIRED, SUPERSEDED, NOT_DELIVERED, UNKNOWN }
    public enum Ledger {
        NEW_EVENT, ALREADY_APPLIED_SAME_EVENT, PREVIOUSLY_ACCEPTED_NOT_APPLIED,
        PROTECTED_IDENTITY_CONFLICT, UNKNOWN
    }
    public enum EvidenceScope { SYNTHETIC_VERIFIED, UNVERIFIED_OR_REAL }

    public static final class Input {
        public final String eventId, consultationId, questionId, expectedQuestionId;
        public final String waitEffectId, expectedWaitEffectId;
        public final EventType eventType;
        public final Lifecycle consultationLifecycle;
        public final Question questionState;
        public final Ledger ledgerStatus;
        public final EvidenceScope evidenceScope;
        public final boolean sourceCurrent, answerReferenceBound;

        public Input(String eventId, String consultationId, String questionId,
                     String expectedQuestionId, String waitEffectId, String expectedWaitEffectId,
                     EventType eventType, Lifecycle consultationLifecycle, Question questionState,
                     Ledger ledgerStatus, EvidenceScope evidenceScope,
                     boolean sourceCurrent, boolean answerReferenceBound) {
            this.eventId = eventId;
            this.consultationId = consultationId;
            this.questionId = questionId;
            this.expectedQuestionId = expectedQuestionId;
            this.waitEffectId = waitEffectId;
            this.expectedWaitEffectId = expectedWaitEffectId;
            this.eventType = eventType;
            this.consultationLifecycle = consultationLifecycle;
            this.questionState = questionState;
            this.ledgerStatus = ledgerStatus;
            this.evidenceScope = evidenceScope;
            this.sourceCurrent = sourceCurrent;
            this.answerReferenceBound = answerReferenceBound;
        }
    }

    public static final class Result {
        public final Outcome outcome;
        public final String reason;
        public final boolean eligibleForRuntimeCompatibilityCheck;
        private Result(Outcome outcome, String reason) {
            this.outcome = outcome;
            this.reason = reason;
            this.eligibleForRuntimeCompatibilityCheck = outcome == Outcome.ACCEPTED;
        }
    }

    public Result decide(Input input) {
        if (input == null || blank(input.eventId) || blank(input.consultationId)
                || blank(input.questionId) || blank(input.expectedQuestionId)
                || blank(input.waitEffectId) || blank(input.expectedWaitEffectId)
                || input.eventType == null || input.consultationLifecycle == null
                || input.questionState == null || input.ledgerStatus == null
                || input.evidenceScope != EvidenceScope.SYNTHETIC_VERIFIED) {
            return result(Outcome.INSUFFICIENT_EVIDENCE, "MISSING_OR_UNTRUSTED_EVIDENCE");
        }
        if (input.consultationLifecycle == Lifecycle.UNKNOWN
                || input.questionState == Question.UNKNOWN || input.ledgerStatus == Ledger.UNKNOWN) {
            return result(Outcome.INSUFFICIENT_EVIDENCE, "UNKNOWN_AUTHORITATIVE_STATE");
        }
        // These branches are synthetic expectations only. Exact F8 precedence
        // must be frozen by RDP-02 before any real consumer can use this model.
        if (input.ledgerStatus == Ledger.PROTECTED_IDENTITY_CONFLICT) {
            return result(Outcome.REJECTED, "CANONICAL_IDENTITY_CONFLICT");
        }
        if (input.ledgerStatus == Ledger.ALREADY_APPLIED_SAME_EVENT) {
            return result(Outcome.DUPLICATE, "ALREADY_APPLIED_SAME_EVENT");
        }
        if (input.ledgerStatus == Ledger.PREVIOUSLY_ACCEPTED_NOT_APPLIED) {
            return result(Outcome.INSUFFICIENT_EVIDENCE, "RECONCILIATION_REQUIRED");
        }
        if (input.consultationLifecycle == Lifecycle.EXPIRED
                || input.questionState == Question.EXPIRED) {
            return result(Outcome.EXPIRED, "EXPIRED_BUSINESS_WINDOW");
        }
        if (input.consultationLifecycle != Lifecycle.WAITING_USER
                || input.questionState != Question.CURRENT_DELIVERED) {
            return result(Outcome.REJECTED, "NOT_CURRENT_DELIVERED_WAIT");
        }
        if (!input.sourceCurrent || !input.answerReferenceBound
                || !input.questionId.equals(input.expectedQuestionId)
                || !input.waitEffectId.equals(input.expectedWaitEffectId)) {
            return result(Outcome.REJECTED, "BUSINESS_BINDING_MISMATCH");
        }
        return result(Outcome.ACCEPTED, "CURRENT_SYNTHETIC_BUSINESS_RESUME");
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static Result result(Outcome outcome, String reason) {
        return new Result(outcome, reason);
    }
}
