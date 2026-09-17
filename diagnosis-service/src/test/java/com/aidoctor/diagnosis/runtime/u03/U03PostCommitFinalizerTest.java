package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class U03PostCommitFinalizerTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-17T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void committedStateRemainsCommittedWhenTracePersistenceFailsAndNoNormalOutboundIsProduced() {
        Fixture fixture = fixture();
        StateTypes.CommitResult commit = fixture.commitService.commitNonProduction(
                fixture.context, fixture.proposal);
        assertEquals("COMMITTED", commit.status);
        assertEquals(8, fixture.repository.currentVersion("cdp-finalizer-1"));

        U03RuntimeTracePort failingTracePort = record -> {
            throw new IllegalStateException("synthetic trace persistence failure");
        };
        U03PostCommitFinalizer finalizer = new U03PostCommitFinalizer(
                new U03RuntimeTraceService(failingTracePort),
                new U03OutboundProducer());

        U03PostCommitFinalizationOutcome outcome = finalizer.finalizeCommittedExecution(
                fixture.context,
                fixture.governed,
                fixture.decision,
                fixture.proposal,
                commit);

        assertEquals(U03PostCommitFinalizationOutcome.RECONCILIATION_REQUIRED, outcome.getStatus());
        assertTrue(outcome.requiresReconciliation());
        assertEquals(U03RuntimeTraceWriteOutcome.FAILED, outcome.getTraceOutcome().getStatus());
        assertEquals(U03RuntimeTraceWriteOutcome.TRACE_PERSISTENCE_FAILED,
                outcome.getTraceOutcome().getFailureCode());
        assertNull(outcome.getOutboundHandoff());
        assertEquals("COMMITTED", commit.status);
        assertEquals(Integer.valueOf(8), commit.committedVersion);
        assertEquals(1, fixture.repository.commitCalls());
        assertEquals(8, fixture.repository.currentVersion("cdp-finalizer-1"));
    }

    @Test
    void recordedTracePermitsNormalProducerOnlyS14Handoff() {
        Fixture fixture = fixture();
        StateTypes.CommitResult commit = fixture.commitService.commitNonProduction(
                fixture.context, fixture.proposal);
        List<U03RuntimeTraceRecord> records = new ArrayList<U03RuntimeTraceRecord>();
        U03RuntimeTracePort recordingTracePort = records::add;
        U03PostCommitFinalizer finalizer = new U03PostCommitFinalizer(
                new U03RuntimeTraceService(recordingTracePort),
                new U03OutboundProducer());

        U03PostCommitFinalizationOutcome outcome = finalizer.finalizeCommittedExecution(
                fixture.context,
                fixture.governed,
                fixture.decision,
                fixture.proposal,
                commit);

        assertEquals(U03PostCommitFinalizationOutcome.DELIVERABLE, outcome.getStatus());
        assertEquals(U03RuntimeTraceWriteOutcome.RECORDED, outcome.getTraceOutcome().getStatus());
        assertEquals(1, records.size());
        assertEquals("COMMITTED", outcome.getOutboundHandoff().getCommitStatus());
        assertEquals(Integer.valueOf(8), outcome.getOutboundHandoff().getCommittedClinicalStateVersion());
        assertEquals(1, fixture.repository.commitCalls());
    }

    private static Fixture fixture() {
        U03ExecutionCommand command = new U03ExecutionCommand(
                "consult-finalizer-1", "thread-finalizer-1", "run-finalizer-1", "event-finalizer-1",
                "cdp-finalizer-1", 7, "corr-finalizer-1", "trace-finalizer-1");
        U03AcceptedEvidenceBinding evidence = new U03AcceptedEvidenceBinding(
                "acceptance-finalizer-1", 7,
                Collections.singletonList("evidence-finalizer-1"),
                Collections.singletonList("source-finalizer-1"),
                Collections.singletonList("provenance-finalizer-1"));
        U03NonProductionExecutionContext context = new U03NonProductionExecutionContext(
                command,
                U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                evidence,
                "ci-nonprod-e2e");

        LocalDateTime now = LocalDateTime.now();
        CapabilityBindingRecord capability = new CapabilityBindingRecord(
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
                now.minusMinutes(1), null, now.minusMinutes(1));
        U03ResolvedNonProductionReleaseSet resolved = new U03ResolvedNonProductionReleaseSet(
                context.getReleaseRefs(), capability.getBindingId());
        U03ReleaseBinding releaseBinding = resolved.asCandidateReleaseBinding();
        U03RiskAssessmentCandidate candidate = U03RiskAssessmentCandidate.valid(
                7,
                evidence.getEvidenceRefs(),
                0.8d,
                "SYNTHETIC_UNCERTAINTY",
                Collections.<String>emptyList(),
                evidence.getSourceRefs(),
                evidence.getProvenanceRefs(),
                capability.getBindingId(),
                capability.getCapabilityVersion(),
                releaseBinding.getRuleReleaseId(),
                releaseBinding.getKnowledgeReleaseId());
        U03GovernedCandidateGateway.GovernedResult governed =
                new U03GovernedCandidateGateway.GovernedResult(
                        candidate, capability, releaseBinding, resolved);
        U03DecisionOutcome decision = new U03DecisionOutcome(
                "decision-finalizer-1",
                U03RiskAssessmentCandidate.VALID,
                "CAUTION",
                "SYNTHETIC_FROZEN_D09_DECISION",
                evidence.getEvidenceRefs());
        U03StateProposal proposal = new U03StateProposalFactory()
                .createNonProductionValid(context, decision, governed);

        List<String> repositoryOrder = new ArrayList<String>();
        MechanicalVersionRepositoryFake repository = new MechanicalVersionRepositoryFake(repositoryOrder);
        repository.seed("cdp-finalizer-1", 7);
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
        return new Fixture(
                context,
                governed,
                decision,
                proposal,
                repository,
                new U03CommitService(committer));
    }

    private static final class Fixture {
        private final U03NonProductionExecutionContext context;
        private final U03GovernedCandidateGateway.GovernedResult governed;
        private final U03DecisionOutcome decision;
        private final U03StateProposal proposal;
        private final MechanicalVersionRepositoryFake repository;
        private final U03CommitService commitService;

        private Fixture(
                U03NonProductionExecutionContext context,
                U03GovernedCandidateGateway.GovernedResult governed,
                U03DecisionOutcome decision,
                U03StateProposal proposal,
                MechanicalVersionRepositoryFake repository,
                U03CommitService commitService) {
            this.context = context;
            this.governed = governed;
            this.decision = decision;
            this.proposal = proposal;
            this.repository = repository;
            this.commitService = commitService;
        }
    }
}
