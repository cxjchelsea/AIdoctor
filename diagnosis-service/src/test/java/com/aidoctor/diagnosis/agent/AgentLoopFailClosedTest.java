package com.aidoctor.diagnosis.agent;

import com.aidoctor.diagnosis.entity.AgentState;
import com.aidoctor.diagnosis.exception.ClinicalSemanticBlockerException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentLoopFailClosedTest {

    private final AgentLoop loop = new AgentLoop();
    private final AgentLoop.CDPState cdpState = AgentLoop.CDPState.builder()
        .cdpData(new HashMap<>())
        .missingInfo(new ArrayList<>())
        .criticalMissingInfo(new ArrayList<>())
        .conflicts(new ArrayList<>())
        .redFlags(new ArrayList<>())
        .build();
    private final AgentState agentState = new AgentState();

    @Test
    void redFlagPlanningCannotDefaultToContinue() {
        assertThrows(ClinicalSemanticBlockerException.class,
            () -> loop.checkRedFlagPriority(cdpState, agentState));
    }

    @Test
    void conflictReviewCannotBeSilentlyIgnored() {
        assertThrows(ClinicalSemanticBlockerException.class,
            () -> loop.checkConflictReview(cdpState, agentState));
    }

    @Test
    void insufficientEvidenceCannotBeTreatedAsSufficient() {
        assertThrows(ClinicalSemanticBlockerException.class,
            () -> loop.checkInsufficientEvidence(cdpState, agentState));
    }
}
