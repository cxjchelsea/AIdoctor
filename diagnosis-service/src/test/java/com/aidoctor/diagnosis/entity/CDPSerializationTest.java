package com.aidoctor.diagnosis.entity;

import com.aidoctor.diagnosis.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDPSerializationTest {

    @Test
    void mapAndListFieldsKeepTheirCanonicalTypesAndNestedData() {
        CDP cdp = new CDP();
        Map<String, Object> nested = new HashMap<>();
        nested.put("severity", "high");
        Map<String, Object> patientState = new HashMap<>();
        patientState.put("symptom", nested);
        List<Map<String, Object>> ddx = Collections.singletonList(
            Collections.<String, Object>singletonMap("disease_name", "candidate"));

        cdp.setPatientState(patientState);
        cdp.setTriage(Collections.<String, Object>singletonMap("risk_level", "L3"));
        cdp.setUncertainty(Collections.<String, Object>singletonMap("source", "missing"));
        cdp.setHealthStateAssessment(Collections.<String, Object>singletonMap("mode", "clinical"));
        cdp.setDdx(ddx);
        cdp.setEvidenceGraph(ddx);
        cdp.setWorkupPlan(ddx);
        cdp.setManagementPlan(ddx);

        assertEquals(patientState, cdp.getPatientState());
        assertEquals("L3", cdp.getTriage().get("risk_level"));
        assertEquals(ddx, cdp.getDdx());
        assertEquals(ddx, cdp.getEvidenceGraph());
        assertEquals(ddx, cdp.getWorkupPlan());
        assertEquals(ddx, cdp.getManagementPlan());
    }

    @Test
    void nullAndEmptyCollectionsRetainTheDocumentedReadBehavior() {
        CDP cdp = new CDP();
        cdp.setPatientState(null);
        cdp.setDdx(null);

        assertTrue(cdp.getPatientState().isEmpty());
        assertTrue(cdp.getDdx().isEmpty());

        cdp.setPatientState(Collections.<String, Object>emptyMap());
        cdp.setDdx(Collections.<Map<String, Object>>emptyList());
        assertEquals(Collections.emptyMap(), cdp.getPatientState());
        assertEquals(Collections.emptyList(), cdp.getDdx());
    }

    @Test
    void cdpJsonSnapshotRoundTripPreservesMapAndListFields() {
        CDP cdp = CDP.builder()
            .id("cdp-1")
            .patientId("patient-1")
            .sessionId("session-1")
            .version(3)
            .cdpStatus("clinical_mode_collecting")
            .build();
        cdp.setPatientState(Collections.<String, Object>singletonMap("age", 42));
        cdp.setDdx(Arrays.asList(
            Collections.<String, Object>singletonMap("disease_name", "first"),
            Collections.<String, Object>singletonMap("disease_name", "second")));

        JsonUtil jsonUtil = new JsonUtil();
        String json = jsonUtil.toJson(cdp);
        CDP restored = jsonUtil.fromJson(json, CDP.class);

        assertNotNull(json);
        assertNotNull(restored);
        assertEquals(cdp.getPatientState(), restored.getPatientState());
        assertEquals(cdp.getDdx(), restored.getDdx());
        assertEquals(cdp.getCdpStatus(), restored.getCdpStatus());
    }
}
