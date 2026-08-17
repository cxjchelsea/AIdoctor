package com.aidoctor.contracts.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

/**
 * Java 绑定结构往返。语义 oracle 仍是 Python Shared Contract validator。
 */
public class SharedContractsConformanceTest {
    private final ObjectMapper mapper = SharedContractsMapper.create();

    @Test
    public void catalogHasThirteenSchemas() {
        Assert.assertEquals(13, SchemaCatalog.SCHEMA_NAMES.length);
        Assert.assertEquals("1.0.0", ContractVersion.CONTRACT_VERSION);
        Assert.assertEquals("EXACT", ContractVersion.VERSION_NEGOTIATION);
        for (int index = 0; index < SchemaCatalog.SCHEMA_NAMES.length; index++) {
            Assert.assertNotNull(SchemaCatalog.typeFor(SchemaCatalog.SCHEMA_NAMES[index]));
        }
    }

    @Test
    public void validFixturesRoundTrip() throws Exception {
        ObjectNode fixtures = loadValidFixtures();
        for (int index = 0; index < SchemaCatalog.SCHEMA_NAMES.length; index++) {
            String schemaName = SchemaCatalog.SCHEMA_NAMES[index];
            JsonNode original = fixtures.get(schemaName);
            Assert.assertNotNull("missing fixture " + schemaName, original);
            ExactVersion.requireExact(original.get("contract_version").asText());
            Object parsed = mapper.treeToValue(original, SchemaCatalog.typeFor(schemaName));
            JsonNode dumped = mapper.valueToTree(parsed);
            ExactVersion.requireExact(dumped.get("contract_version").asText());
            Assert.assertEquals(schemaName + " contract_version", "1.0.0", dumped.get("contract_version").asText());
        }
    }

    @Test
    public void unknownVersionFailsClosed() throws Exception {
        ObjectNode fixtures = loadValidFixtures();
        ObjectNode trace = fixtures.get("TraceRef").deepCopy();
        trace.put("contract_version", "9.9.9");
        try {
            ExactVersion.requireExact(trace.get("contract_version").asText());
            Assert.fail("unknown version must fail closed");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("9.9.9"));
        }
    }

    @Test
    public void statePatchIsNotCommitResult() throws Exception {
        ObjectNode fixtures = loadValidFixtures();
        StateTypes.StatePatch patch = mapper.treeToValue(fixtures.get("StatePatch"), StateTypes.StatePatch.class);
        StateTypes.CommitResult commit = mapper.treeToValue(fixtures.get("CommitResult"), StateTypes.CommitResult.class);
        Assert.assertEquals("StatePatch", patch.envelope.contractName);
        Assert.assertEquals("CommitResult", commit.envelope.contractName);
        Assert.assertNotNull(patch.operations);
        Assert.assertNull(mapper.valueToTree(patch).get("status"));
        Assert.assertEquals("COMMITTED", commit.status);
    }

    @Test
    public void requiredNullsSurviveRoundTrip() throws Exception {
        ObjectNode fixtures = loadValidFixtures();
        FoundationTypes.ContractConflict conflict = mapper.treeToValue(
            fixtures.get("ContractConflict"),
            FoundationTypes.ContractConflict.class
        );
        JsonNode dumped = mapper.valueToTree(conflict);
        Assert.assertTrue(dumped.has("expected_value"));
        Assert.assertTrue(dumped.get("expected_value").isNull());
        Assert.assertTrue(dumped.has("actual_value"));
        Assert.assertTrue(dumped.get("actual_value").isNull());
    }

    @Test
    public void unknownPropertyFailsClosed() throws Exception {
        ObjectNode fixtures = loadValidFixtures();
        ObjectNode trace = fixtures.get("TraceRef").deepCopy();
        trace.put("raw_input", "forbidden");
        try {
            mapper.treeToValue(trace, FoundationTypes.TraceRef.class);
            Assert.fail("unknown property must fail closed");
        } catch (Exception expected) {
            Assert.assertTrue(expected.getMessage().contains("raw_input") || expected.getMessage().contains("Unrecognized"));
        }
    }

    @Test
    public void toolResultUsesSuggestedPatchesNotLegacyWrites() throws Exception {
        ObjectNode fixtures = loadValidFixtures();
        ToolTypes.ToolResult result = mapper.treeToValue(fixtures.get("ToolResult"), ToolTypes.ToolResult.class);
        JsonNode dumped = mapper.valueToTree(result);
        Assert.assertEquals("SUCCEEDED", result.status);
        Assert.assertFalse(dumped.has("suggested_writes"));
        Assert.assertTrue(dumped.has("suggested_patches"));
        Assert.assertEquals("StatePatch", dumped.get("suggested_patches").get(0).get("envelope").get("contract_name").asText());
    }

    private ObjectNode loadValidFixtures() throws Exception {
        File validDir = new File("../..", "fixtures/valid").getCanonicalFile();
        Assert.assertTrue("fixtures dir " + validDir, validDir.isDirectory());
        ObjectNode merged = mapper.createObjectNode();
        File[] files = validDir.listFiles();
        Assert.assertNotNull(files);
        for (int index = 0; index < files.length; index++) {
            File file = files[index];
            if (!file.getName().endsWith(".json")) {
                continue;
            }
            JsonNode node = mapper.readTree(file);
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                merged.set(field.getKey(), field.getValue());
            }
        }
        return merged;
    }
}
