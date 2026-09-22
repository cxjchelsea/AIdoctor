package com.aidoctor.diagnosis.state.committer;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyntheticStructuredObjectRepositoryIntegrationTest {
    private static final String CDP = "test.subject.structured";

    @Test
    void repositoryAddPersistsStructuredRecordAndSnapshotReadsItBack() {
        SyntheticVersionedStateRepository repository = repository(0, new LinkedHashMap<String, Object>());
        Map<String, Object> readiness = readiness("CURRENT");

        StateRepositoryPort.AtomicCommitOutcome outcome =
                repository.attemptAtomicCommit(command(patch("ADD", readiness, 0, "patch-add")));

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED, outcome.status);
        assertEquals(1, repository.snapshot(CDP).version());
        assertEquals(readiness, clinicalReadiness(repository.snapshot(CDP)));
    }

    @Test
    void repositoryReplacePersistsStructuredStaleRecordAndHistoricalInputIsNotAliased() {
        Map<String, Object> current = readiness("CURRENT");
        SyntheticVersionedStateRepository repository = repository(0, current);
        Map<String, Object> stale = readiness("STALE");
        stale.put("invalidation_reason_refs", Arrays.<Object>asList("F3_INPUT_CHANGED"));

        StateRepositoryPort.AtomicCommitOutcome outcome =
                repository.attemptAtomicCommit(command(patch("REPLACE", stale, 0, "patch-replace")));

        stale.put("state_validity", "MUTATED_BY_CALLER");

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED, outcome.status);
        Map<String, Object> stored = clinicalReadiness(repository.snapshot(CDP));
        assertEquals("STALE", stored.get("state_validity"));
        assertEquals(Arrays.<Object>asList("F3_INPUT_CHANGED"), stored.get("invalidation_reason_refs"));
        assertEquals("CURRENT", current.get("state_validity"));
    }

    private static SyntheticVersionedStateRepository repository(
            int version,
            Map<String, Object> readiness) {
        Map<String, Object> patient = new LinkedHashMap<String, Object>();
        if (!readiness.isEmpty()) patient.put("clinical_readiness", readiness);
        Map<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("patient_state", patient);
        Map<String, SyntheticStateSnapshot> initial =
                new LinkedHashMap<String, SyntheticStateSnapshot>();
        initial.put(CDP, new SyntheticStateSnapshot(version, state));
        return new SyntheticVersionedStateRepository(initial);
    }

    private static Map<String, Object> readiness(String validity) {
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("readiness_record_id", "readiness-001");
        value.put("clinical_readiness", "READY_FOR_CLINICAL_ANALYSIS");
        value.put("source_refs", Arrays.<Object>asList("f1-ref", "f3-ref"));
        value.put("state_validity", validity);
        value.put("effect_id", "readiness-effect-001");
        return value;
    }

    private static StateTypes.StatePatch patch(
            String op,
            Map<String, Object> value,
            int baseVersion,
            String patchId) {
        StateTypes.StatePatch patch = new StateTypes.StatePatch();
        patch.cdpId = CDP;
        patch.baseVersion = Integer.valueOf(baseVersion);
        patch.patchId = patchId;
        patch.idempotencyKey = "idem-" + patchId;
        patch.operations = Arrays.asList(operation(op, value));
        return patch;
    }

    private static StateTypes.StatePatchOperation operation(
            String op,
            Map<String, Object> value) {
        StateTypes.StatePatchOperation operation = new StateTypes.StatePatchOperation();
        operation.op = op;
        operation.path = "/patient_state/clinical_readiness";
        operation.value = value;
        operation.expectedCurrentValue = null;
        operation.source = "RULE_DERIVED";
        operation.sensitivity = "PHI";
        return operation;
    }

    private static StateRepositoryPort.AtomicCommitCommand command(StateTypes.StatePatch patch) {
        return new StateRepositoryPort.AtomicCommitCommand(
                patch.cdpId,
                patch.baseVersion.intValue(),
                patch.patchId,
                patch.idempotencyKey,
                patch);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> clinicalReadiness(SyntheticStateSnapshot snapshot) {
        Map<String, Object> patient =
                (Map<String, Object>) snapshot.state().get("patient_state");
        return (Map<String, Object>) patient.get("clinical_readiness");
    }
}
