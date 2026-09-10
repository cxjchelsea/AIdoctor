package com.aidoctor.contracts.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import java.io.File;
import java.util.Iterator;
import java.util.Map;

public class SharedContractsConformanceTest {
    private final ObjectMapper mapper = SharedContractsMapper.create();
    @Test public void catalogHasSixteenSchemas() {
        Assert.assertEquals(16, SchemaCatalog.SCHEMA_NAMES.length);
        Assert.assertEquals("1.1.0", ContractVersion.CONTRACT_VERSION);
        for (String name : SchemaCatalog.SCHEMA_NAMES) Assert.assertNotNull(SchemaCatalog.typeFor(name));
    }
    @Test public void validFixturesRoundTrip() throws Exception {
        ObjectNode fixtures = loadValidFixtures();
        for (String schemaName : SchemaCatalog.SCHEMA_NAMES) {
            JsonNode original = fixtures.get(schemaName);
            Assert.assertNotNull("missing fixture " + schemaName, original);
            ExactVersion.requireExact(original.get("contract_version").asText());
            Object parsed = mapper.treeToValue(original, SchemaCatalog.typeFor(schemaName));
            JsonNode dumped = mapper.valueToTree(parsed);
            Assert.assertEquals("1.1.0", dumped.get("contract_version").asText());
        }
    }
    @Test public void clinicalTypesArePresent() throws Exception {
        ObjectNode fixtures = loadValidFixtures();
        ClinicalTypes.Encounter encounter = mapper.treeToValue(fixtures.get("Encounter"), ClinicalTypes.Encounter.class);
        ClinicalTypes.ClinicalObservation observation = mapper.treeToValue(fixtures.get("ClinicalObservation"), ClinicalTypes.ClinicalObservation.class);
        ClinicalTypes.ClinicalStateSnapshot snapshot = mapper.treeToValue(fixtures.get("ClinicalStateSnapshot"), ClinicalTypes.ClinicalStateSnapshot.class);
        Assert.assertEquals("Encounter", encounter.envelope.contractName);
        Assert.assertEquals("COMMITTED", observation.status);
        Assert.assertTrue(observation.value instanceof ClinicalTypes.TextObservationValue);
        Assert.assertTrue(snapshot.observations.containsKey("test.observation.001"));
    }
    @Test public void statePatchAndCommitResultExposeEncounterTarget() throws Exception {
        ObjectNode fixtures = loadValidFixtures();
        StateTypes.StatePatch patch = mapper.treeToValue(fixtures.get("StatePatch"), StateTypes.StatePatch.class);
        StateTypes.CommitResult result = mapper.treeToValue(fixtures.get("CommitResult"), StateTypes.CommitResult.class);
        Assert.assertEquals("test.cdp.001", patch.cdpId);
        Assert.assertNull(patch.encounterId);
        Assert.assertEquals("test.cdp.001", result.cdpId);
        Assert.assertNull(result.encounterId);
    }
    @Test public void observationValueVariantsDeserializeThroughClinicalObservationField() throws Exception {
        ObjectNode base = loadValidFixtures().get("ClinicalObservation").deepCopy();
        assertObservationValueSubtype(base, textValue("alpha"), ClinicalTypes.TextObservationValue.class);
        assertObservationValueSubtype(base, numberValue(42.5), ClinicalTypes.NumberObservationValue.class);
        assertObservationValueSubtype(base, booleanValue(true), ClinicalTypes.BooleanObservationValue.class);
        assertObservationValueSubtype(base, codedValue(), ClinicalTypes.CodedObservationValue.class);
        assertObservationValueSubtype(base, quantityValue(), ClinicalTypes.QuantityObservationValue.class);
        assertObservationValueSubtype(base, referenceValue(), ClinicalTypes.ReferenceObservationValue.class);
    }
    @Test public void statePatchOperationsDeserializeToTypedBranches() throws Exception {
        ObjectNode fixtures = loadValidFixtures();
        StateTypes.StatePatch legacy = mapper.treeToValue(fixtures.get("StatePatch"), StateTypes.StatePatch.class);
        Assert.assertTrue(legacy.operations.get(0) instanceof StateTypes.LegacyStatePatchOperation);
        StateTypes.LegacyStatePatchOperation legacyOperation = (StateTypes.LegacyStatePatchOperation) legacy.operations.get(0);
        Assert.assertNotNull(legacyOperation.value);

        ObjectNode canonicalAdd = canonicalPatch("ADD");
        StateTypes.StatePatch addPatch = mapper.treeToValue(canonicalAdd, StateTypes.StatePatch.class);
        Assert.assertTrue(addPatch.operations.get(0) instanceof StateTypes.CanonicalObservationAddOperation);
        Assert.assertTrue(((StateTypes.CanonicalObservationAddOperation) addPatch.operations.get(0)).value.value instanceof ClinicalTypes.TextObservationValue);

        ObjectNode canonicalReplace = canonicalPatch("REPLACE");
        StateTypes.StatePatch replacePatch = mapper.treeToValue(canonicalReplace, StateTypes.StatePatch.class);
        Assert.assertTrue(replacePatch.operations.get(0) instanceof StateTypes.CanonicalObservationReplaceOperation);
        Assert.assertTrue(((StateTypes.CanonicalObservationReplaceOperation) replacePatch.operations.get(0)).value.value instanceof ClinicalTypes.TextObservationValue);

        ObjectNode canonicalRemove = canonicalPatch("REMOVE");
        StateTypes.StatePatch removePatch = mapper.treeToValue(canonicalRemove, StateTypes.StatePatch.class);
        Assert.assertTrue(removePatch.operations.get(0) instanceof StateTypes.CanonicalObservationRemoveOperation);
        JsonNode dumpedRemove = mapper.valueToTree(removePatch.operations.get(0));
        Assert.assertFalse(dumpedRemove.has("value"));
        Assert.assertFalse(dumpedRemove.has("expected_current_value"));
    }
    @Test public void legacyTestExpectedCurrentValueRemainsLegacyOnly() throws Exception {
        ObjectNode legacyTest = loadValidFixtures().get("StatePatch").deepCopy();
        ObjectNode operation = (ObjectNode) legacyTest.withArray("operations").get(0);
        operation.put("op", "TEST");
        operation.put("expected_current_value", "alpha");
        StateTypes.StatePatch patch = mapper.treeToValue(legacyTest, StateTypes.StatePatch.class);
        Assert.assertTrue(patch.operations.get(0) instanceof StateTypes.LegacyStatePatchOperation);
        Assert.assertNotNull(((StateTypes.LegacyStatePatchOperation) patch.operations.get(0)).expectedCurrentValue);
    }
    @Test public void safeIntegerFieldsRoundTripAtSchemaMaximum() throws Exception {
        long maxSafe = 9007199254740991L;
        ObjectNode fixtures = loadValidFixtures();

        ObjectNode patchNode = fixtures.get("StatePatch").deepCopy();
        patchNode.put("base_version", maxSafe);
        StateTypes.StatePatch patch = mapper.treeToValue(patchNode, StateTypes.StatePatch.class);
        Assert.assertEquals(Long.valueOf(maxSafe), patch.baseVersion);
        Assert.assertEquals(maxSafe, mapper.valueToTree(patch).get("base_version").asLong());

        ObjectNode resultNode = fixtures.get("CommitResult").deepCopy();
        resultNode.put("previous_version", maxSafe - 1);
        resultNode.put("committed_version", maxSafe);
        StateTypes.CommitResult result = mapper.treeToValue(resultNode, StateTypes.CommitResult.class);
        Assert.assertEquals(Long.valueOf(maxSafe - 1), result.previousVersion);
        Assert.assertEquals(Long.valueOf(maxSafe), result.committedVersion);

        ObjectNode encounterNode = fixtures.get("Encounter").deepCopy();
        encounterNode.put("current_state_version", maxSafe);
        ClinicalTypes.Encounter encounter = mapper.treeToValue(encounterNode, ClinicalTypes.Encounter.class);
        Assert.assertEquals(Long.valueOf(maxSafe), encounter.currentStateVersion);

        ObjectNode snapshotNode = fixtures.get("ClinicalStateSnapshot").deepCopy();
        snapshotNode.put("state_version", maxSafe);
        ClinicalTypes.ClinicalStateSnapshot snapshot = mapper.treeToValue(snapshotNode, ClinicalTypes.ClinicalStateSnapshot.class);
        Assert.assertEquals(Long.valueOf(maxSafe), snapshot.stateVersion);
    }
    @Test public void unknownPropertyFailsClosed() throws Exception {
        ObjectNode trace = loadValidFixtures().get("TraceRef").deepCopy(); trace.put("raw_input", "forbidden");
        try { mapper.treeToValue(trace, FoundationTypes.TraceRef.class); Assert.fail("unknown property must fail closed"); }
        catch (Exception expected) { Assert.assertTrue(expected.getMessage().contains("raw_input") || expected.getMessage().contains("Unrecognized")); }
    }
    private void assertObservationValueSubtype(ObjectNode base, ObjectNode value, Class<?> expectedType) throws Exception {
        ObjectNode item = base.deepCopy();
        item.set("value", value);
        ClinicalTypes.ClinicalObservation parsed = mapper.treeToValue(item, ClinicalTypes.ClinicalObservation.class);
        Assert.assertTrue(expectedType.isInstance(parsed.value));
        JsonNode dumped = mapper.valueToTree(parsed);
        Assert.assertEquals(value.get("kind"), dumped.get("value").get("kind"));
    }
    private ObjectNode textValue(String value) {
        ObjectNode node = mapper.createObjectNode();
        node.put("kind", "TEXT"); node.put("value", value); return node;
    }
    private ObjectNode numberValue(double value) {
        ObjectNode node = mapper.createObjectNode();
        node.put("kind", "NUMBER"); node.put("value", value); return node;
    }
    private ObjectNode booleanValue(boolean value) {
        ObjectNode node = mapper.createObjectNode();
        node.put("kind", "BOOLEAN"); node.put("value", value); return node;
    }
    private ObjectNode codedValue() {
        ObjectNode node = mapper.createObjectNode();
        node.put("kind", "CODED"); node.put("code", "example-code"); node.put("system", "example-system"); node.put("display", "Example"); return node;
    }
    private ObjectNode quantityValue() {
        ObjectNode node = mapper.createObjectNode();
        node.put("kind", "QUANTITY"); node.put("value", 42.5); node.put("unit", "unit-x"); return node;
    }
    private ObjectNode referenceValue() {
        ObjectNode node = mapper.createObjectNode();
        node.put("kind", "REFERENCE"); node.put("reference_type", "example"); node.put("reference_id", "test.reference.001"); return node;
    }
    private ObjectNode canonicalPatch(String op) throws Exception {
        ObjectNode patch = loadValidFixtures().get("StatePatch").deepCopy();
        ObjectNode observation = loadValidFixtures().get("ClinicalObservation").deepCopy();
        patch.remove("cdp_id");
        patch.put("encounter_id", observation.get("encounter_id").asText());
        patch.withArray("operations").removeAll();
        ObjectNode operation = mapper.createObjectNode();
        operation.put("op", op);
        operation.put("path", "/observations/" + observation.get("observation_id").asText());
        operation.put("source", "PATIENT_FACT");
        operation.put("sensitivity", observation.get("sensitivity").asText());
        if (!"REMOVE".equals(op)) {
            operation.set("value", observation);
        }
        patch.withArray("operations").add(operation);
        return patch;
    }
    private ObjectNode loadValidFixtures() throws Exception {
        File validDir = new File("../..", "fixtures/valid").getCanonicalFile(); Assert.assertTrue(validDir.isDirectory());
        ObjectNode merged = mapper.createObjectNode(); File[] files = validDir.listFiles(); Assert.assertNotNull(files);
        for (File file : files) if (file.getName().endsWith(".json")) {
            JsonNode node = mapper.readTree(file); Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) { Map.Entry<String, JsonNode> field = fields.next(); merged.set(field.getKey(), field.getValue()); }
        }
        return merged;
    }
}
