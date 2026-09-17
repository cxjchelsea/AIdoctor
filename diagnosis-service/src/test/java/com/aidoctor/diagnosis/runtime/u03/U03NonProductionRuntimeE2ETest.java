package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityExecutionContext;
import com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
import com.aidoctor.diagnosis.state.committer.fakes.InMemoryIdempotencyFake;
import com.aidoctor.diagnosis.state.committer.fakes.MechanicalVersionRepositoryFake;
import com.aidoctor.diagnosis.state.committer.fakes.RecordingCommitEventEvidenceFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticAuditPortFake;
import com.aidoctor.diagnosis.state.committer.ports.CapabilityPolicyPort;
import com.aidoctor.diagnosis.state.committer.ports.ConsentPolicyPort;
import com.aidoctor.diagnosis.state.committer.ports.FieldPermissionPort;
import com.aidoctor.diagnosis.state.committer.ports.SourceValidationPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V8 controlled NON_PRODUCTION_RUNTIME_E2E for the authorized CD-07 chain.
 *
 * <p>The clinical values remain synthetic, but C02 and D09 are the real concrete
 * Gate-C-frozen non-production implementations. The repository is an in-memory
 * mechanical fake and no production adapter, patient traffic, U04 consumer, or
 * external network is used.</p>
 */
class U03NonProductionRuntimeE2ETest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-17T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void nonProductionRuntimeE2EBindsConcreteGateCC02D09K09P01TraceAndOutboundWithoutU04Execution() {
        U03NonProductionExecutionContext context = context();
        CapabilityBindingRecord capability = capabilityBinding();
        CapabilityInvocationGuard guard = new CapabilityInvocationGuard(null) {
            @Override
            public CapabilityBindingRecord authorize(
                    String bindingId,
                    String expectedCapabilityId,
                    CapabilityExecutionContext executionContext) {
                assertEquals(U03GovernedCandidateGateway.BINDING_ID, bindingId);
                assertEquals(U03GovernedCandidateGateway.CAPABILITY_ID, expectedCapabilityId);
                return capability;
            }
        };

        U03GateCClinicalInputPort clinicalInputPort = (command, acceptedEvidence) ->
                U03GateCClinicalInput.builder()
                        .suspectedSepsis("TRUE")
                        .dyspnoeaContext("FALSE")
                        .measurement("respiratory_rate_bpm", U03GateCClinicalInput.Measurement.present(21))
                        .measurement("systolic_bp_mmHg", U03GateCClinicalInput.Measurement.present(120))
                        .measurement("usual_systolic_bp_mmHg", U03GateCClinicalInput.Measurement.present(120))
                        .measurement("heart_rate_bpm", U03GateCClinicalInput.Measurement.present(80))
                        .build();

        U03AcceptedEvidenceAwareCandidateProvider c02 = new U03GateCFrozenRuleEvaluator(clinicalInputPort);
        U03GovernedCandidateGateway gateway = new U03GovernedCandidateGateway(
                guard, new U03ReleaseRegistry(), c02, true);
        U03GovernedCandidateGateway.GovernedResult governed = gateway.assess(context);
        assertFalse(governed.getReleaseBinding().isActive());
        assertNotNull(governed.getCandidate().getGateCEvaluation());
        assertEquals(15, governed.getCandidate().getGateCEvaluation().getRuleResults().size());

        U03NonProductionDecisionPort d09 = new U03GateCFrozenDecisionPort();
        U03DecisionOutcome decision = new U03DecisionService(d09).decide(context, governed);
        assertEquals(U03RiskAssessmentCandidate.VALID, decision.getStatus());
        assertEquals("CAUTION", decision.getOutcomeCode());
        assertEquals("MODERATE_HIGH_RULE_SIGNAL_PRESENT", decision.getReasonCode());

        U03StateProposal proposal = new U03StateProposalFactory()
                .createNonProductionValid(context, decision, governed);
        assertEquals(6, proposal.getReleaseRefs().size());
        assertEquals(Integer.valueOf(7), proposal.getStatePatch().baseVersion);

        List<String> repositoryOrder = new ArrayList<String>();
        MechanicalVersionRepositoryFake repository = new MechanicalVersionRepositoryFake(repositoryOrder);
        repository.seed("cdp-e2e-1", 7);
        StateCommitter committer = new StateCommitter(
                repository,
                (capabilityId, capabilityVersion) -> CapabilityPolicyPort.CapabilityDecision.authorized(),
                (path, capabilityId) -> FieldPermissionPort.FieldPermissionDecision.authorized(),
                (cdpId, capabilityId) -> ConsentPolicyPort.ConsentDecision.authorized(),
                source -> SourceValidationPort.SourceDecision.authorized(),
                new InMemoryIdempotencyFake(repositoryOrder),
                new SyntheticAuditPortFake(CLOCK, repositoryOrder),
                new RecordingCommitEventEvidenceFake(repositoryOrder),
                CLOCK);
        StateTypes.CommitResult commit = new U03CommitService(committer)
                .commitNonProduction(context, proposal);
        assertEquals("COMMITTED", commit.status);
        assertEquals(Integer.valueOf(7), commit.previousVersion);
        assertEquals(Integer.valueOf(8), commit.committedVersion);
        assertEquals(1, repository.commitCalls());

        RecordingTracePort tracePort = new RecordingTracePort();
        U03PostCommitFinalizationOutcome finalization = new U03PostCommitFinalizer(
                new U03RuntimeTraceService(tracePort),
                new U03OutboundProducer())
                .finalizeCommittedExecution(context, governed, decision, proposal, commit);
        assertEquals(U03PostCommitFinalizationOutcome.DELIVERABLE, finalization.getStatus());
        assertEquals(U03RuntimeTraceWriteOutcome.RECORDED,
                finalization.getTraceOutcome().getStatus());
        assertEquals(1, tracePort.records.size());
        U03RuntimeTraceRecord trace = tracePort.records.get(0);
        assertEquals("thread-e2e-1", trace.getThreadId());
        assertEquals("run-e2e-1", trace.getRunId());
        assertEquals("event-e2e-1", trace.getEventId());
        assertEquals(7, trace.getClinicalStateVersion());
        assertEquals("COMMITTED", trace.getCommitStatus());
        assertEquals(Integer.valueOf(8), trace.getCommitCommittedVersion());

        U03OutboundHandoff outbound = finalization.getOutboundHandoff();
        assertNotNull(outbound);
        assertEquals(7, outbound.getSourceClinicalStateVersion());
        assertEquals(Integer.valueOf(8), outbound.getCommittedClinicalStateVersion());
        assertEquals("VALID", outbound.getExecutionStatus());
        assertEquals("CAUTION", outbound.getDispositionCode());
        assertEquals(6, outbound.getGovernedReleaseRefs().size());
        assertNotNull(outbound.getCommitAuditId());
        assertNull(outbound.getExecutionFailureReasonCode());
        assertEquals("ci-nonprod-e2e", outbound.getEnvironmentId());
        assertEquals(U03NonProductionExecutionContext.BINDING_MODE, outbound.getBindingMode());

        assertTrue(repositoryOrder.size() > 0);
        assertEquals(8, repository.currentVersion("cdp-e2e-1"));
    }

    private static U03NonProductionExecutionContext context() {
        U03ExecutionCommand command = new U03ExecutionCommand(
                "consult-e2e-1",
                "thread-e2e-1",
                "run-e2e-1",
                "event-e2e-1",
                "cdp-e2e-1",
                7,
                "corr-e2e-1",
                "trace-e2e-1");
        U03AcceptedEvidenceBinding evidence = new U03AcceptedEvidenceBinding(
                "acceptance-e2e-1",
                7,
                Arrays.asList(
                        "EV-RF-RESP-001",
                        "EV-MNM-NEURO-001",
                        "EV-MNM-NEURO-002",
                        "EV-MNM-CARD-001",
                        "EV-RF-ALLERGY-001",
                        "EV-RF-APPEAR-001",
                        "EV-RF-NEURO-001",
                        "EV-VS-SEPSIS-001",
                        "EV-VS-SEPSIS-002",
                        "EV-VS-SEPSIS-003",
                        "EV-RF-SEPSIS-001"),
                Collections.singletonList("synthetic-source-e2e-1"),
                Collections.singletonList("synthetic-provenance-e2e-1"));
        return new U03NonProductionExecutionContext(
                command,
                U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                evidence,
                "ci-nonprod-e2e");
    }

    private static CapabilityBindingRecord capabilityBinding() {
        LocalDateTime now = LocalDateTime.now();
        return new CapabilityBindingRecord(
                U03GovernedCandidateGateway.BINDING_ID,
                U03GovernedCandidateGateway.CAPABILITY_ID,
                "1.0.0",
                "capset-v1",
                U03GovernedCandidateGateway.SCOPE_VERSION,
                U03GovernedCandidateGateway.CONTRACT_VERSION,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ACTIVE,
                now.minusMinutes(1),
                null,
                now.minusMinutes(1));
    }

    private static final class RecordingTracePort implements U03RuntimeTracePort {
        private final List<U03RuntimeTraceRecord> records = new ArrayList<U03RuntimeTraceRecord>();

        @Override
        public void record(U03RuntimeTraceRecord record) {
            records.add(record);
        }
    }
}
