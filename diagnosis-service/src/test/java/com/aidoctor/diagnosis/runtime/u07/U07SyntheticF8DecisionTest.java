package com.aidoctor.diagnosis.runtime.u07;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class U07SyntheticF8DecisionTest {
    private final U07SyntheticF8Decision f8 = new U07SyntheticF8Decision();

    private U07SyntheticF8Decision.Input evidence(
            U07SyntheticF8Decision.Lifecycle lifecycle,
            U07SyntheticF8Decision.Question question,
            U07SyntheticF8Decision.Ledger ledger) {
        return input(lifecycle, question, ledger, U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED,
                "question-1", "question-1", "wait-1", "wait-1", true, true);
    }

    private U07SyntheticF8Decision.Input input(
            U07SyntheticF8Decision.Lifecycle lifecycle,
            U07SyntheticF8Decision.Question question,
            U07SyntheticF8Decision.Ledger ledger,
            U07SyntheticF8Decision.EvidenceScope scope,
            String questionId, String expectedQuestionId, String waitId, String expectedWait,
            boolean sourceCurrent, boolean answerBound) {
        return new U07SyntheticF8Decision.Input("synthetic-event", "synthetic-consult",
                questionId, expectedQuestionId, waitId, expectedWait,
                U07SyntheticF8Decision.EventType.USER_ANSWER, lifecycle, question, ledger,
                scope, sourceCurrent, answerBound);
    }

    @Test void acceptsOnlySyntheticCurrentDeliveredWait() {
        U07SyntheticF8Decision.Result result = f8.decide(evidence(
                U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                U07SyntheticF8Decision.Ledger.NEW_EVENT));
        assertEquals(U07SyntheticF8Decision.Outcome.ACCEPTED, result.outcome);
        assertTrue(result.eligibleForRuntimeCompatibilityCheck);
    }

    @Test void sameAppliedEventIsDuplicate() {
        U07SyntheticF8Decision.Result result = f8.decide(evidence(
                U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                U07SyntheticF8Decision.Ledger.ALREADY_APPLIED_SAME_EVENT));
        assertEquals(U07SyntheticF8Decision.Outcome.DUPLICATE, result.outcome);
        assertFalse(result.eligibleForRuntimeCompatibilityCheck);
    }

    @Test void acceptedButNotAppliedRequiresReconciliationNotReplay() {
        assertEquals(U07SyntheticF8Decision.Outcome.INSUFFICIENT_EVIDENCE, f8.decide(evidence(
                U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                U07SyntheticF8Decision.Ledger.PREVIOUSLY_ACCEPTED_NOT_APPLIED)).outcome);
    }

    @Test void protectedCanonicalEventIdentityConflictRejected() {
        assertEquals(U07SyntheticF8Decision.Outcome.REJECTED, f8.decide(evidence(
                U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                U07SyntheticF8Decision.Ledger.PROTECTED_IDENTITY_CONFLICT)).outcome);
    }

    @Test void expiredConsultationOrQuestionReturnsExpired() {
        assertEquals(U07SyntheticF8Decision.Outcome.EXPIRED, f8.decide(evidence(
                U07SyntheticF8Decision.Lifecycle.EXPIRED,
                U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                U07SyntheticF8Decision.Ledger.NEW_EVENT)).outcome);
        assertEquals(U07SyntheticF8Decision.Outcome.EXPIRED, f8.decide(evidence(
                U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                U07SyntheticF8Decision.Question.EXPIRED,
                U07SyntheticF8Decision.Ledger.NEW_EVENT)).outcome);
    }

    @Test void cancelledTerminalUndeliveredOrSupersededReject() {
        for (U07SyntheticF8Decision.Lifecycle state : new U07SyntheticF8Decision.Lifecycle[]{
                U07SyntheticF8Decision.Lifecycle.CANCELLED, U07SyntheticF8Decision.Lifecycle.TERMINAL}) {
            assertEquals(U07SyntheticF8Decision.Outcome.REJECTED, f8.decide(evidence(
                    state, U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                    U07SyntheticF8Decision.Ledger.NEW_EVENT)).outcome);
        }
        for (U07SyntheticF8Decision.Question state : new U07SyntheticF8Decision.Question[]{
                U07SyntheticF8Decision.Question.NOT_DELIVERED,
                U07SyntheticF8Decision.Question.SUPERSEDED}) {
            assertEquals(U07SyntheticF8Decision.Outcome.REJECTED, f8.decide(evidence(
                    U07SyntheticF8Decision.Lifecycle.WAITING_USER, state,
                    U07SyntheticF8Decision.Ledger.NEW_EVENT)).outcome);
        }
    }

    @Test void mismatchedQuestionWaitOrSourceRejected() {
        for (U07SyntheticF8Decision.Input candidate : new U07SyntheticF8Decision.Input[]{
                input(U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                        U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                        U07SyntheticF8Decision.Ledger.NEW_EVENT,
                        U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED,
                        "other-question", "question-1", "wait-1", "wait-1", true, true),
                input(U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                        U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                        U07SyntheticF8Decision.Ledger.NEW_EVENT,
                        U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED,
                        "question-1", "question-1", "other-wait", "wait-1", true, true),
                input(U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                        U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                        U07SyntheticF8Decision.Ledger.NEW_EVENT,
                        U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED,
                        "question-1", "question-1", "wait-1", "wait-1", false, true),
                input(U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                        U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                        U07SyntheticF8Decision.Ledger.NEW_EVENT,
                        U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED,
                        "question-1", "question-1", "wait-1", "wait-1", true, false)
        }) {
            assertEquals(U07SyntheticF8Decision.Outcome.REJECTED, f8.decide(candidate).outcome);
        }
    }

    @Test void unknownOrUnverifiedEvidenceFailsClosed() {
        assertEquals(U07SyntheticF8Decision.Outcome.INSUFFICIENT_EVIDENCE, f8.decide(null).outcome);
        assertEquals(U07SyntheticF8Decision.Outcome.INSUFFICIENT_EVIDENCE, f8.decide(evidence(
                U07SyntheticF8Decision.Lifecycle.UNKNOWN,
                U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                U07SyntheticF8Decision.Ledger.NEW_EVENT)).outcome);
        assertEquals(U07SyntheticF8Decision.Outcome.INSUFFICIENT_EVIDENCE, f8.decide(evidence(
                U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                U07SyntheticF8Decision.Question.UNKNOWN,
                U07SyntheticF8Decision.Ledger.NEW_EVENT)).outcome);
        assertEquals(U07SyntheticF8Decision.Outcome.INSUFFICIENT_EVIDENCE, f8.decide(evidence(
                U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                U07SyntheticF8Decision.Ledger.UNKNOWN)).outcome);
        assertEquals(U07SyntheticF8Decision.Outcome.INSUFFICIENT_EVIDENCE, f8.decide(input(
                U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                U07SyntheticF8Decision.Ledger.NEW_EVENT,
                U07SyntheticF8Decision.EvidenceScope.UNVERIFIED_OR_REAL,
                "question-1", "question-1", "wait-1", "wait-1", true, true)).outcome);
    }

    @Test void runtimeCheckpointDoesNotInfluencePureF8() {
        // No checkpoint field exists in F8's business input: runtime is separate.
        U07SyntheticF8Decision.Input current = evidence(
                U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                U07SyntheticF8Decision.Ledger.NEW_EVENT);
        assertEquals(U07SyntheticF8Decision.Outcome.ACCEPTED, f8.decide(current).outcome);
        assertEquals(U07SyntheticF8Decision.Outcome.ACCEPTED, f8.decide(current).outcome);
    }
}
