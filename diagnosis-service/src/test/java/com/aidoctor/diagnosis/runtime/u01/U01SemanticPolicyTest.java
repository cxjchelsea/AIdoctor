package com.aidoctor.diagnosis.runtime.u01;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class U01SemanticPolicyTest {
    private final U01SemanticPolicy policy = new U01SemanticPolicy();

    @Test
    void selfWithFramedInScopeProblemRoutesToU02() {
        U01SemanticDecision decision = policy.decide(command("SELF", null, "持续咳嗽三天", "SYMPTOM", false));
        assertEquals(U01SemanticDecision.SUBJECT_RESOLVED, decision.getSubjectStatus());
        assertEquals(U01SemanticDecision.PROBLEM_FRAMED, decision.getProblemStatus());
        assertEquals(U01SemanticDecision.IN_SCOPE, decision.getScopeDecision());
        assertEquals("U02", decision.getNextUnit());
    }

    @Test
    void otherWithoutReferenceDoesNotGuessSubjectAndRoutesToClarification() {
        U01SemanticDecision decision = policy.decide(command("OTHER", null, "孩子发热", "SYMPTOM", false));
        assertEquals(U01SemanticDecision.SUBJECT_CLARIFICATION_REQUIRED, decision.getSubjectStatus());
        assertEquals(U01SemanticDecision.NEEDS_CLARIFICATION, decision.getScopeDecision());
        assertEquals("U06", decision.getNextUnit());
        assertNotEquals("WAITING_USER", decision.getNextUnit());
    }

    @Test
    void outsideV1IntentRoutesToSafeExitAssemblyNotDdx() {
        U01SemanticDecision decision = policy.decide(command("SELF", null, "请替我制定长期慢病治疗方案", "OUTSIDE_V1_INTENT", false));
        assertEquals(U01SemanticDecision.OUT_OF_SCOPE, decision.getScopeDecision());
        assertEquals("U11", decision.getNextUnit());
    }

    @Test
    void earlySafetySignalSurvivesIncompleteFraming() {
        U01SemanticDecision decision = policy.decide(command("SELF", null, "胸痛", "MIXED", true));
        assertEquals(U01SemanticDecision.NEEDS_CLARIFICATION, decision.getScopeDecision());
        assertTrue(decision.isEarlySafetySignalPresent());
        assertEquals("U06", decision.getNextUnit());
    }

    @Test
    void blankProblemRequiresClarification() {
        U01SemanticDecision decision = policy.decide(command("SELF", null, " ", "SYMPTOM", false));
        assertEquals(U01SemanticDecision.PROBLEM_CLARIFICATION_REQUIRED, decision.getProblemStatus());
        assertEquals("U06", decision.getNextUnit());
    }

    private U01StartCommand command(String subjectType, String subjectRef, String text, String scope, boolean safety) {
        return new U01StartCommand("evt-1", "idem-1", "user-1", subjectType, subjectRef, text, scope, safety);
    }
}
