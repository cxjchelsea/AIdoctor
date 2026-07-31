package com.aidoctor.diagnosis.agent;

import com.aidoctor.diagnosis.dto.tool.ToolResult;
import com.aidoctor.diagnosis.entity.AgentState;
import com.aidoctor.diagnosis.entity.CDP;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceFusionTest {

    @Test
    void conflictValuesHaveExpectedFieldsAndRemainImmutableOnJava8() {
        ToolResult result = diagnosisResult(0.9, 0.1);
        AgentState state = stateWithEmptyFusionData();

        EvidenceFusion.FusionResult fusion = new EvidenceFusion().fuseEvidence(
            Collections.singletonList(result), new CDP(), state);

        assertEquals(1, fusion.getConflicts().size());
        List<?> values = (List<?>) fusion.getConflicts().get(0).get("conflicting_values");
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) values.get(0);
        assertEquals("tool_3", first.get("tool_id"));
        assertEquals(0.9, first.get("value"));
        assertThrows(UnsupportedOperationException.class, () -> first.put("extra", true));
    }

    @Test
    void missingPayloadDoesNotInventAConflict() {
        ToolResult result = ToolResult.builder()
            .toolId("tool_3")
            .status("success")
            .payload(null)
            .build();

        EvidenceFusion.FusionResult fusion = new EvidenceFusion().fuseEvidence(
            Collections.singletonList(result), new CDP(), stateWithEmptyFusionData());

        assertTrue(fusion.getConflicts().isEmpty());
    }

    @Test
    void missingProbabilityKeepsExistingZeroDefaultBehavior() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ddx_rank_list", Arrays.asList(new HashMap<>(), new HashMap<>()));
        ToolResult result = ToolResult.builder()
            .toolId("tool_3")
            .status("success")
            .payload(payload)
            .build();

        EvidenceFusion.FusionResult fusion = new EvidenceFusion().fuseEvidence(
            Collections.singletonList(result), new CDP(), stateWithEmptyFusionData());

        assertTrue(fusion.getConflicts().isEmpty());
    }

    private ToolResult diagnosisResult(double first, double second) {
        Map<String, Object> firstItem = new HashMap<>();
        firstItem.put("probability", first);
        Map<String, Object> secondItem = new HashMap<>();
        secondItem.put("probability", second);
        Map<String, Object> payload = new HashMap<>();
        payload.put("ddx_rank_list", Arrays.asList(firstItem, secondItem));
        return ToolResult.builder()
            .toolId("tool_3")
            .status("success")
            .payload(payload)
            .build();
    }

    private AgentState stateWithEmptyFusionData() {
        AgentState state = new AgentState();
        state.setEvidenceFusionStateMap(new HashMap<>());
        return state;
    }
}
