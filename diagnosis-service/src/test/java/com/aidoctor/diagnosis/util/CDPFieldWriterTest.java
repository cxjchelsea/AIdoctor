package com.aidoctor.diagnosis.util;

import com.aidoctor.diagnosis.entity.CDP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDPFieldWriterTest {

    private CDP cdp;
    private CDPFieldWriter writer;

    @BeforeEach
    void setUp() {
        cdp = new CDP();
        cdp.setPatientState(new HashMap<>());
        cdp.setDdx(new ArrayList<>());
        cdp.setEvidenceGraph(new ArrayList<>());
        cdp.setWorkupPlan(new ArrayList<>());
        cdp.setManagementPlan(new ArrayList<>());
        cdp.setTriage(new HashMap<>());
        cdp.setUncertainty(new HashMap<>());
        cdp.setHealthStateAssessment(new HashMap<>());
        cdp.setWellnessPlan(new HashMap<>());
        writer = new CDPFieldWriter();
    }

    @Test
    void writesCanonicalMapAndListFieldsWithoutDataLoss() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("unit", "C");
        Map<String, Object> mapValue = new HashMap<>();
        mapValue.put("temperature", nested);
        List<Map<String, Object>> listValue = Collections.singletonList(
            Collections.<String, Object>singletonMap("disease_name", "candidate"));

        assertTrue(writer.writeField(cdp, "cdp.patient_state", mapValue));
        assertTrue(writer.writeField(cdp, "cdp.ddx", listValue));
        assertEquals(mapValue, cdp.getPatientState());
        assertEquals(listValue, cdp.getDdx());
    }

    @Test
    void writesEveryCanonicalListField() {
        List<Map<String, Object>> value = Collections.singletonList(
            Collections.<String, Object>singletonMap("id", "one"));

        assertTrue(writer.writeField(cdp, "cdp.evidence_graph", value));
        assertTrue(writer.writeField(cdp, "cdp.workup_plan", value));
        assertTrue(writer.writeField(cdp, "cdp.management_plan", value));
        assertEquals(value, cdp.getEvidenceGraph());
        assertEquals(value, cdp.getWorkupPlan());
        assertEquals(value, cdp.getManagementPlan());
    }

    @Test
    void rejectsMapForListFieldAndListForMapField() {
        assertFalse(writer.writeField(cdp, "cdp.ddx", Collections.singletonMap("bad", true)));
        assertFalse(writer.writeField(cdp, "cdp.triage", Collections.singletonList("bad")));
        assertTrue(cdp.getDdx().isEmpty());
        assertTrue(cdp.getTriage().isEmpty());
    }

    @Test
    void rejectsListItemsThatAreNotMaps() {
        assertFalse(writer.writeField(cdp, "cdp.workup_plan", Arrays.asList("bad")));
        assertTrue(cdp.getWorkupPlan().isEmpty());
    }

    @Test
    void acceptsNullForWholeFieldAndRetainsCanonicalEmptyReadBehavior() {
        assertTrue(writer.writeField(cdp, "cdp.patient_state", null));
        assertTrue(writer.writeField(cdp, "cdp.ddx", null));
        assertTrue(cdp.getPatientState().isEmpty());
        assertTrue(cdp.getDdx().isEmpty());
    }

    @Test
    void updatesNestedListDataUsingCanonicalListPath() {
        Map<String, Object> first = new HashMap<>();
        first.put("disease_name", "candidate");
        first.put("probability", 0.2);
        cdp.setDdx(Collections.singletonList(first));

        assertTrue(writer.writeField(cdp, "cdp.ddx[0].probability", 0.8));
        assertEquals("candidate", cdp.getDdx().get(0).get("disease_name"));
        assertEquals(0.8, cdp.getDdx().get(0).get("probability"));
    }

    @Test
    void finalConclusionUsesExistingPatientStateJsonField() {
        Map<String, Object> conclusion = Collections.<String, Object>singletonMap("summary", "reviewed");

        assertTrue(writer.writeField(cdp, "cdp.final_conclusion", conclusion));
        assertEquals(conclusion, cdp.getPatientState().get("conclusion_package"));
    }
}
