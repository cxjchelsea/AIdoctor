package com.aidoctor.diagnosis.state.committer;

import com.aidoctor.contracts.v1.StateTypes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SyntheticJsonPointerApplierObjectValueParityTest {

    private final SyntheticJsonPointerApplier applier = new SyntheticJsonPointerApplier();

    @Test
    void oneLevelStructuredObjectAddIsAppliedAndDeepCopied() {
        Map<String, Object> working = workingPatientState();
        Map<String, Object> readiness = new LinkedHashMap<String, Object>();
        List<Object> refs = new ArrayList<Object>(Arrays.<Object>asList("f1-ref", "f3-ref"));
        readiness.put("readiness_record_id", "readiness-001");
        readiness.put("clinical_readiness", "READY_FOR_CLINICAL_ANALYSIS");
        readiness.put("source_refs", refs);
        readiness.put("state_validity", "CURRENT");

        applier.apply(operation("ADD", readiness), working);

        readiness.put("clinical_readiness", "MUTATED_BY_CALLER");
        refs.add("late-ref");

        @SuppressWarnings("unchecked")
        Map<String, Object> stored = (Map<String, Object>) patientState(working).get("clinical_readiness");
        assertEquals("READY_FOR_CLINICAL_ANALYSIS", stored.get("clinical_readiness"));
        assertEquals(Arrays.<Object>asList("f1-ref", "f3-ref"), stored.get("source_refs"));
    }

    @Test
    void oneLevelStructuredObjectReplaceIsApplied() {
        Map<String, Object> working = workingPatientState();
        Map<String, Object> initial = new LinkedHashMap<String, Object>();
        initial.put("readiness_record_id", "readiness-001");
        initial.put("state_validity", "CURRENT");
        patientState(working).put("clinical_readiness", initial);

        Map<String, Object> replacement = new LinkedHashMap<String, Object>();
        replacement.put("readiness_record_id", "readiness-001");
        replacement.put("state_validity", "STALE");
        replacement.put("invalidation_reason_refs", Arrays.<Object>asList("F3_INPUT_CHANGED"));

        applier.apply(operation("REPLACE", replacement), working);

        @SuppressWarnings("unchecked")
        Map<String, Object> stored = (Map<String, Object>) patientState(working).get("clinical_readiness");
        assertEquals("STALE", stored.get("state_validity"));
        assertEquals(Arrays.<Object>asList("F3_INPUT_CHANGED"), stored.get("invalidation_reason_refs"));
    }

    @Test
    void exactBoundaryValuesAreAccepted() {
        Map<String, Object> object = new LinkedHashMap<String, Object>();
        object.put(repeat("k", 64), repeat("s", 1000));
        object.put("safe_number", Long.valueOf(9007199254740991L));
        object.put("list", listOfSize(64, "v"));
        for (int index = 3; index < 32; index++) {
            object.put("k" + index, "v");
        }

        Map<String, Object> working = workingPatientState();
        applier.apply(operation("ADD", object), working);

        assertEquals(32, ((Map<?, ?>) patientState(working).get("clinical_readiness")).size());
    }

    @Test
    void topLevelStringLimitIsPreserved() {
        Map<String, Object> working = workingPatientState();
        applier.apply(operationAt("/patient_state/test_value", "ADD", repeat("s", 4000)), working);
        assertEquals(4000, ((String) patientState(working).get("test_value")).length());

        assertRejected(operationAt("/patient_state/too_long", "ADD", repeat("s", 4001)));
    }

    @Test
    void mapSizeThirtyThreeIsRejected() {
        Map<String, Object> object = new LinkedHashMap<String, Object>();
        for (int index = 0; index < 33; index++) object.put("k" + index, "v");
        assertRejected(operation("ADD", object));
    }

    @Test
    void malformedOrTooLongMapKeyIsRejected() {
        Map<String, Object> malformed = new LinkedHashMap<String, Object>();
        malformed.put("../escape", "v");
        assertRejected(operation("ADD", malformed));

        Map<String, Object> tooLong = new LinkedHashMap<String, Object>();
        tooLong.put(repeat("k", 65), "v");
        assertRejected(operation("ADD", tooLong));
    }

    @Test
    void listSizeSixtyFiveIsRejected() {
        Map<String, Object> object = new LinkedHashMap<String, Object>();
        object.put("refs", listOfSize(65, "v"));
        assertRejected(operation("ADD", object));
    }

    @Test
    void structuredChildStringOverLimitIsRejected() {
        Map<String, Object> object = new LinkedHashMap<String, Object>();
        object.put("value", repeat("s", 1001));
        assertRejected(operation("ADD", object));
    }

    @Test
    void unsafeNumberIsRejected() {
        Map<String, Object> object = new LinkedHashMap<String, Object>();
        object.put("number", Long.valueOf(9007199254740992L));
        assertRejected(operation("ADD", object));
    }

    @Test
    void nestedMapIsRejected() {
        Map<String, Object> nested = new LinkedHashMap<String, Object>();
        nested.put("inner", "value");
        Map<String, Object> object = new LinkedHashMap<String, Object>();
        object.put("nested", nested);
        assertRejected(operation("ADD", object));
    }

    @Test
    void nestedListIsRejected() {
        Map<String, Object> object = new LinkedHashMap<String, Object>();
        object.put("refs", Arrays.<Object>asList(Arrays.<Object>asList("nested")));
        assertRejected(operation("ADD", object));
    }

    @Test
    void listOfMapIsRejected() {
        Map<String, Object> child = new LinkedHashMap<String, Object>();
        child.put("id", "one");
        Map<String, Object> object = new LinkedHashMap<String, Object>();
        object.put("refs", Arrays.<Object>asList(child));
        assertRejected(operation("ADD", object));
    }

    @Test
    void addAndReplaceTopLevelNullRemainRejected() {
        assertRejected(operation("ADD", null));

        Map<String, Object> working = workingPatientState();
        patientState(working).put("clinical_readiness", "existing");
        final StateTypes.StatePatchOperation replace = operation("REPLACE", null);
        assertThrows(SyntheticJsonPointerApplier.SyntheticApplicationException.class,
                () -> applier.apply(replace, working));
    }

    private void assertRejected(final StateTypes.StatePatchOperation operation) {
        assertThrows(SyntheticJsonPointerApplier.SyntheticApplicationException.class,
                () -> applier.apply(operation, workingPatientState()));
    }

    private static StateTypes.StatePatchOperation operation(String op, Object value) {
        return operationAt("/patient_state/clinical_readiness", op, value);
    }

    private static StateTypes.StatePatchOperation operationAt(String path, String op, Object value) {
        StateTypes.StatePatchOperation operation = new StateTypes.StatePatchOperation();
        operation.op = op;
        operation.path = path;
        operation.value = value;
        operation.expectedCurrentValue = null;
        operation.source = "RULE_DERIVED";
        operation.sensitivity = "PHI";
        return operation;
    }

    private static Map<String, Object> workingPatientState() {
        Map<String, Object> working = new LinkedHashMap<String, Object>();
        working.put("patient_state", new LinkedHashMap<String, Object>());
        return working;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> patientState(Map<String, Object> working) {
        return (Map<String, Object>) working.get("patient_state");
    }

    private static List<Object> listOfSize(int size, Object value) {
        List<Object> result = new ArrayList<Object>();
        for (int index = 0; index < size; index++) result.add(value);
        return result;
    }

    private static String repeat(String value, int count) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < count; index++) result.append(value);
        return result.toString();
    }
}
