package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.service.cdp.CDPVersionService;
import com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClinicalCdpStateRepositoryAdapterTest {
    private CDPRepository repository;
    private CDPVersionService versionService;
    private ClinicalCdpStateRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(CDPRepository.class);
        versionService = mock(CDPVersionService.class);
        adapter = new ClinicalCdpStateRepositoryAdapter(repository, versionService, new ObjectMapper());
    }

    @Test
    void commitsIntoAuthoritativeCdpAndAdvancesVersionOnce() {
        CDP cdp = cdp("cdp-1", 3);
        cdp.setPatientState(new HashMap<String, Object>());
        when(repository.findByIdWithLock("cdp-1")).thenReturn(Optional.of(cdp));
        when(repository.save(any(CDP.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StateTypes.StatePatch patch = patch(add("/patient_state/subject_id", "patient-7"));
        StateRepositoryPort.AtomicCommitOutcome result = adapter.attemptAtomicCommit(
                new StateRepositoryPort.AtomicCommitCommand("cdp-1", 3, "patch-1", "idem-1", patch));

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED, result.status);
        assertEquals(3, result.previousVersion);
        assertEquals(4, result.committedVersion);
        assertEquals(4, cdp.getVersion().intValue());
        assertEquals("patient-7", cdp.getPatientState().get("subject_id"));
        verify(versionService).createVersion(cdp);
        verify(repository).save(cdp);
    }

    @Test
    void staleBaseVersionConflictsWithoutMutation() {
        CDP cdp = cdp("cdp-1", 4);
        cdp.setPatientState(new HashMap<String, Object>());
        when(repository.findByIdWithLock("cdp-1")).thenReturn(Optional.of(cdp));

        StateRepositoryPort.AtomicCommitOutcome result = adapter.attemptAtomicCommit(
                new StateRepositoryPort.AtomicCommitCommand("cdp-1", 3, "patch-1", "idem-1",
                        patch(add("/patient_state/subject_id", "patient-7"))));

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.CONFLICT, result.status);
        assertEquals(4, result.actualVersion);
        assertEquals(4, cdp.getVersion().intValue());
        assertFalse(cdp.getPatientState().containsKey("subject_id"));
        verify(versionService, never()).createVersion(any(CDP.class));
        verify(repository, never()).save(any(CDP.class));
    }

    @Test
    void unsupportedListRootFailsClosedBeforeMutation() {
        CDP cdp = cdp("cdp-1", 2);
        cdp.setDdx(new java.util.ArrayList<java.util.Map<String, Object>>());
        when(repository.findByIdWithLock("cdp-1")).thenReturn(Optional.of(cdp));

        StateRepositoryPort.AtomicCommitOutcome result = adapter.attemptAtomicCommit(
                new StateRepositoryPort.AtomicCommitCommand("cdp-1", 2, "patch-1", "idem-1",
                        patch(add("/ddx/0", "not-a-typed-ddx"))));

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.FAILED, result.status);
        assertEquals(ClinicalCdpStateRepositoryAdapter.UNSUPPORTED_CLINICAL_PATH, result.failureCode);
        assertFalse(result.retryable);
        assertEquals(2, cdp.getVersion().intValue());
        assertTrue(cdp.getDdx().isEmpty());
        verify(versionService, never()).createVersion(any(CDP.class));
        verify(repository, never()).save(any(CDP.class));
    }

    @Test
    void compositePatchWithMissingNestedParentFailsAtomicallyBeforeVersionHistory() {
        CDP cdp = cdp("cdp-1", 8);
        Map<String, Object> original = new LinkedHashMap<String, Object>();
        original.put("original", "retained");
        cdp.setPatientState(original);
        when(repository.findByIdWithLock("cdp-1")).thenReturn(Optional.of(cdp));

        StateTypes.StatePatch combined = patch(
                add("/patient_state/safety_gate", "ALLOW"));
        combined.operations = Arrays.asList(
                add("/patient_state/safety_gate", "ALLOW"),
                add("/patient_state/missing_parent/f3_gap", "should-not-persist"));
        StateRepositoryPort.AtomicCommitOutcome result = adapter.attemptAtomicCommit(
                new StateRepositoryPort.AtomicCommitCommand(
                        "cdp-1", 8, "patch-atomic", "idem-atomic", combined));

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.FAILED, result.status);
        assertEquals(ClinicalCdpStateRepositoryAdapter.UNSUPPORTED_CLINICAL_PATH, result.failureCode);
        assertEquals(8, cdp.getVersion().intValue());
        assertEquals("retained", cdp.getPatientState().get("original"));
        assertFalse(cdp.getPatientState().containsKey("safety_gate"));
        verify(versionService, never()).createVersion(any(CDP.class));
        verify(repository, never()).save(any(CDP.class));
    }

    @Test
    void versionConflictPreventsU04GateAndF3Writes() {
        CDP cdp = cdp("cdp-1", 10);
        cdp.setPatientState(new LinkedHashMap<String, Object>());
        when(repository.findByIdWithLock("cdp-1")).thenReturn(Optional.of(cdp));
        StateTypes.StatePatch p = patch(add("/patient_state/f3_gap_assessment", "stale"));

        StateRepositoryPort.AtomicCommitOutcome result = adapter.attemptAtomicCommit(
                new StateRepositoryPort.AtomicCommitCommand(
                        "cdp-1", 9, "patch-stale", "idem-stale", p));
        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.CONFLICT, result.status);
        assertEquals(10, result.actualVersion);
        assertFalse(cdp.getPatientState().containsKey("f3_gap_assessment"));
        verify(versionService, never()).createVersion(any(CDP.class));
        verify(repository, never()).save(any(CDP.class));
    }

    @Test
    void versionHistoryFailureDoesNotWriteCdpOrAdvanceVersion() {
        CDP cdp = cdp("cdp-1", 10);
        cdp.setPatientState(new LinkedHashMap<String, Object>());
        when(repository.findByIdWithLock("cdp-1")).thenReturn(Optional.of(cdp));
        org.mockito.Mockito.when(versionService.createVersion(cdp))
                .thenThrow(new IllegalStateException("synthetic-version-history-failure"));
        boolean rejected = false;
        try {
            adapter.attemptAtomicCommit(
                    new StateRepositoryPort.AtomicCommitCommand(
                            "cdp-1", 10, "patch-history", "idem-history",
                            patch(add("/patient_state/clinical_readiness", "synthetic"))));
        } catch (IllegalStateException expected) {
            rejected = true;
            assertEquals("synthetic-version-history-failure", expected.getMessage());
        }
        assertTrue(rejected);
        assertEquals(10, cdp.getVersion().intValue());
        assertFalse(cdp.getPatientState().containsKey("clinical_readiness"));
        verify(repository, never()).save(any(CDP.class));
    }

    @Test
    void missingCdpFailsWithoutCreatingHistory() {
        when(repository.findByIdWithLock("unknown")).thenReturn(Optional.empty());
        StateRepositoryPort.AtomicCommitOutcome result = adapter.attemptAtomicCommit(
                new StateRepositoryPort.AtomicCommitCommand(
                        "unknown", 0, "patch-missing", "idem-missing",
                        patch(add("/patient_state/safety_gate", "ALLOW"))));
        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.FAILED, result.status);
        assertEquals(ClinicalCdpStateRepositoryAdapter.CDP_NOT_FOUND, result.failureCode);
        verify(versionService, never()).createVersion(any(CDP.class));
        verify(repository, never()).save(any(CDP.class));
    }

    private CDP cdp(String id, int version) {
        return CDP.builder().id(id).patientId("patient").sessionId("session").version(version).build();
    }

    private StateTypes.StatePatch patch(StateTypes.StatePatchOperation operation) {
        StateTypes.StatePatch patch = new StateTypes.StatePatch();
        patch.operations = Arrays.asList(operation);
        return patch;
    }

    private StateTypes.StatePatchOperation add(String path, Object value) {
        StateTypes.StatePatchOperation operation = new StateTypes.StatePatchOperation();
        operation.op = "ADD";
        operation.path = path;
        operation.source = "PATIENT_FACT";
        operation.sensitivity = "PHI";
        operation.value = value;
        return operation;
    }
}
