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
    @Test public void unknownPropertyFailsClosed() throws Exception {
        ObjectNode trace = loadValidFixtures().get("TraceRef").deepCopy(); trace.put("raw_input", "forbidden");
        try { mapper.treeToValue(trace, FoundationTypes.TraceRef.class); Assert.fail("unknown property must fail closed"); }
        catch (Exception expected) { Assert.assertTrue(expected.getMessage().contains("raw_input") || expected.getMessage().contains("Unrecognized")); }
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
