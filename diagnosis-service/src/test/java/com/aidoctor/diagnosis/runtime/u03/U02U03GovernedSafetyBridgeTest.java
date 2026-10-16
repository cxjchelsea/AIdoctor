package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.governance.*;
import com.aidoctor.diagnosis.runtime.u02.*;
import com.aidoctor.diagnosis.runtime.u04.*;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
import com.aidoctor.diagnosis.state.committer.ports.*;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class U02U03GovernedSafetyBridgeTest {
    private CDPRepository cdps;
    private CapabilityCallTraceRepository traces;
    private U02U03GovernedSafetyBridge.AcceptedEvidenceAuthority evidence;
    private U02U03GovernedSafetyBridge bridge;
    private U02ExecutionResult u02;
    private U03ExecutionCommand command;
    private CapabilityCallTraceRecord trace;
    private CDP cdp;

    @BeforeEach
    void setup() {
        cdps = mock(CDPRepository.class);
        traces = mock(CapabilityCallTraceRepository.class);
        evidence = mock(U02U03GovernedSafetyBridge.AcceptedEvidenceAuthority.class);
        StateCommitter committer = new StateCommitter(
                mock(StateRepositoryPort.class), mock(CapabilityPolicyPort.class),
                mock(FieldPermissionPort.class), mock(ConsentPolicyPort.class),
                mock(SourceValidationPort.class), mock(IdempotencyPort.class),
                mock(AuditPort.class), mock(CommitEventEvidencePort.class));
        U04NonProductionApplicationService u04 = new U04NonProductionApplicationService(
                new U04AdmissionService(), new U04SafetyGatePolicy(),
                new U04StateProposalFactory(), new U04CommitService(committer));
        bridge = new U02U03GovernedSafetyBridge(cdps, traces, evidence,
                new U03NonProductionAdmissionService(), u04);
        StateTypes.StatePatch patch = new StateTypes.StatePatch();
        patch.cdpId = "cdp";
        patch.baseVersion = Integer.valueOf(1);
        U02ClinicalFactProposal proposal = new U02ClinicalFactProposal(
                "proposal", "consult", U02ClinicalFactBusinessOwner.OWNER,
                "decision", Collections.singletonList("c01-u02-v1-active"), patch);
        U02ClinicalFactDecision decision = new U02ClinicalFactDecision(
                "decision", "consult", "event", U02ClinicalFactBusinessOwner.OWNER,
                Collections.emptyList(), false, "FACT_ACCEPTED");
        StateTypes.CommitResult committed = new StateTypes.CommitResult();
        committed.status = "COMMITTED";
        committed.previousVersion = Integer.valueOf(1);
        committed.committedVersion = Integer.valueOf(2);
        committed.patchId = "proposal";
        u02 = new U02ExecutionResult("COMMITTED", "OK", "cap-call", decision, proposal,
                committed, Collections.emptyList());
        command = new U03ExecutionCommand("consult", "thread", "run", "event",
                "cdp", 2, "corr", "trace");
        trace = new CapabilityCallTraceRecord("cap-call", "consult", "thread", "run",
                "event", "U02", "C01", "c01-u02-v1-active", 1,
                java.time.LocalDateTime.now());
        trace.succeed("candidate", "decision", "proposal", "proposal", 2,
                java.time.LocalDateTime.now());
        cdp = CDP.builder().id("cdp").patientId("synthetic-patient")
                .sessionId("synthetic").version(2).build();
        when(traces.findById("cap-call")).thenReturn(Optional.of(trace));
        when(cdps.findById("cdp")).thenReturn(Optional.of(cdp));
        when(evidence.requireAccepted("consult", "cdp", 2, "cap-call")).thenReturn(
                new U03AcceptedEvidenceBinding("accepted-ref", 2,
                        Collections.singletonList("fact-ref"),
                        Collections.singletonList("source-ref"),
                        Collections.singletonList("provenance-ref")));
    }

    @Test void currentCommittedFactWithAuthoritativeTraceCanEnterU03Admission() {
        U03NonProductionExecutionContext ctx = bridge.requireU03Admission(
                u02, command, U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                "ci-nonprod-safety");
        assertEquals(2, ctx.requireAcceptedEvidenceBinding().getClinicalStateVersion());
        assertEquals("cdp", ctx.getCommand().cdpId);
    }

    @Test void staleCdpBlocksBeforeEvidenceAcceptance() {
        cdp.setVersion(3);
        assertTrue(assertThrows(IllegalStateException.class, () -> bridge.requireU03Admission(
                u02, command, U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                "ci-nonprod-safety")).getMessage().contains("U03_STALE_CDP_STATE"));
        verifyNoInteractions(evidence);
    }

    @Test void wrongThreadCannotBorrowValidU02Commit() {
        U03ExecutionCommand wrong = new U03ExecutionCommand(
                "consult", "wrong-thread", "run", "event", "cdp", 2, "corr", "trace");
        assertTrue(assertThrows(IllegalStateException.class, () -> bridge.requireU03Admission(
                u02, wrong, U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                "ci-nonprod-safety")).getMessage().contains("U02_TRACE_IDENTITY_OR_COMMIT_MISMATCH"));
        verifyNoInteractions(evidence);
    }

    @Test void productionIsRejectedBeforeTraceOrCdpReads() {
        assertThrows(IllegalStateException.class, () -> bridge.requireU03Admission(
                u02, command, U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                "production"));
        verifyNoInteractions(evidence, traces, cdps);
    }

    @Test void missingTrustedTraceFailsClosed() {
        when(traces.findById("cap-call")).thenReturn(Optional.empty());
        assertTrue(assertThrows(IllegalStateException.class, () -> bridge.requireU03Admission(
                u02, command, U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                "ci-nonprod-safety")).getMessage().contains("U02_TRACE_NOT_FOUND"));
        verifyNoInteractions(evidence);
    }

    @Test void u04CannotBeInvokedWithoutGenuineU03Handoff() {
        assertThrows(IllegalStateException.class, () -> bridge.applyU04(
                null, U04ScopeContext.current(true, true), "ci-nonprod-safety"));
        verifyNoInteractions(cdps);
    }
}
