package com.aidoctor.diagnosis.agent;

import com.aidoctor.diagnosis.entity.AgentState;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.service.agent_state.AgentStateManager;
import com.aidoctor.diagnosis.service.audit.AuditTrailManager;
import com.aidoctor.diagnosis.service.cdp.CDPManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClinicalAgentBrainTest {

    private CDPManager cdpManager;
    private AgentStateManager agentStateManager;
    private AgentLoop agentLoop;
    private ClinicalAgentBrain brain;

    @BeforeEach
    void setUp() {
        cdpManager = mock(CDPManager.class);
        agentStateManager = mock(AgentStateManager.class);
        agentLoop = mock(AgentLoop.class);
        brain = new ClinicalAgentBrain();
        ReflectionTestUtils.setField(brain, "cdpManager", cdpManager);
        ReflectionTestUtils.setField(brain, "agentStateManager", agentStateManager);
        ReflectionTestUtils.setField(brain, "auditTrailManager", mock(AuditTrailManager.class));
        ReflectionTestUtils.setField(brain, "agentLoop", agentLoop);
    }

    @Test
    void loadsCdpThroughCanonicalManagerMethod() {
        CDP cdp = CDP.builder().id("cdp-1").sessionId("session-1").version(2).build();
        AgentState state = AgentState.builder().sessionId("session-1").cdpId("cdp-1").build();
        AgentLoop.LoopResult loopResult = AgentLoop.LoopResult.builder()
            .decisionType("blocked")
            .result(Collections.<String, Object>singletonMap("reason", "test"))
            .loopCount(1)
            .build();
        when(cdpManager.getCDPById("cdp-1")).thenReturn(Optional.of(cdp));
        when(agentStateManager.getAgentStateByCdpId("cdp-1")).thenReturn(Optional.of(state));
        when(agentLoop.runLoop(cdp, state)).thenReturn(loopResult);

        Map<String, Object> result = brain.runAgent("cdp-1");

        assertEquals("blocked", result.get("decision_type"));
        verify(cdpManager).getCDPById("cdp-1");
    }

    @Test
    void missingCdpRetainsExplicitFailureSemantics() {
        when(cdpManager.getCDPById("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> brain.runAgent("missing"));
        verify(cdpManager).getCDPById("missing");
        verify(agentStateManager, never()).getAgentStateByCdpId("missing");
    }
}
