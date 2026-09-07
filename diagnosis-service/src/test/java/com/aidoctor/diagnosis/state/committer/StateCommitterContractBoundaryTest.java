package com.aidoctor.diagnosis.state.committer;

import com.aidoctor.contracts.v1.SharedContractsMapper;
import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.support.CommitResultSchemaAssertions;
import com.aidoctor.diagnosis.state.committer.support.StateCommitterTestHarness;
import com.aidoctor.diagnosis.state.committer.support.SyntheticStatePatchFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural validation remains Shared Contracts v1. The mechanical core
 * consumes already parsed StatePatch objects only.
 */
class StateCommitterContractBoundaryTest {
    private static final ObjectMapper MAPPER = SharedContractsMapper.create();

    @Test
    void invalidRawPayloadCannotCrossSharedContractBinding() throws Exception {
        ObjectNode raw = MAPPER.createObjectNode();
        raw.put("contract_version", "1.0.0");
        raw.put("expected_version", 9);
        raw.put("cdp_id", "synthetic-cdp-001");

        Exception exception = assertThrows(Exception.class, () -> MAPPER.treeToValue(raw, StateTypes.StatePatch.class));
        assertTrue(
                exception.getMessage().contains("expected_version")
                        || exception.getMessage().contains("Unrecognized")
        );
    }

    @Test
    void unknownContractVersionIsRejectedByCoreIdentityCheck() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(0, "synthetic-idem-version", "synthetic-patch-version");
        patch.contractVersion = "9.9.9";

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertEquals("FAILED", result.status);
        assertEquals(CommitReasonCodes.CONTRACT_IDENTITY_INVALID, result.reasonCode);
        assertEquals(0, harness.repository.commitCalls());
        assertEquals(0, harness.audit.commands().size());
        assertEquals(0, harness.idempotency.reserveCalls());
        CommitResultSchemaAssertions.assertValid(result);
    }

    @Test
    void malformedPojoFieldsFailBeforeAnySideEffect() {
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(
                0, "synthetic-idem-malformed", "synthetic-patch-malformed");
        patch.envelope.messageId = "invalid message id";
        patch.reasonCode = "lower-case";
        patch.operations.get(0).path = "/unknown/root";
        StateCommitterTestHarness harness = new StateCommitterTestHarness();

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertEquals("FAILED", result.status);
        assertEquals(CommitReasonCodes.CONTRACT_IDENTITY_INVALID, result.reasonCode);
        assertEquals(Boolean.FALSE, result.retryable);
        assertEquals(0, harness.idempotency.inspectCalls());
        assertEquals(0, harness.audit.commands().size());
        assertEquals(0, harness.repository.commitCalls());
        CommitResultSchemaAssertions.assertValid(result);
    }

    @Test
    void malformedControlledValueFailsBeforeRepository() {
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(
                0, "synthetic-idem-value", "synthetic-patch-value");
        patch.operations.get(0).value = new java.util.HashMap<String, Object>();
        StateCommitterTestHarness harness = new StateCommitterTestHarness();

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertEquals("FAILED", result.status);
        assertEquals(0, harness.repository.commitCalls());
        assertEquals(0, harness.audit.commands().size());
    }

    @Test
    void validParsedSyntheticPatchIsConsumedWithoutSchemaReimplementation() throws Exception {
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(0, "synthetic-idem-parsed", "synthetic-patch-parsed");
        ObjectNode tree = MAPPER.valueToTree(patch);
        assertEquals("StatePatch", tree.get("envelope").get("contract_name").asText());
        assertTrue(tree.has("base_version"));
        assertTrue(!tree.has("expected_version"));

        StateTypes.StatePatch parsed = MAPPER.treeToValue(tree, StateTypes.StatePatch.class);
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        StateTypes.CommitResult result = harness.committer.commit(parsed);
        assertEquals("COMMITTED", result.status);
        CommitResultSchemaAssertions.assertValid(result);
    }
}
