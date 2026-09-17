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
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class U03OutboundProducerTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-17T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void producesCommittedProvenanceBoundOutboundWithoutU04DecisionSemantics() {
        Chain chain = validChain(7);

        U03OutboundHandoff handoff = new U03OutboundProducer().produce(
                chain.context, chain.governed, chain.decision, chain.proposal, chain.commitResult);

        assertEquals(7, handoff.getSourceClinicalStateVersion());
        assertEquals(Integer.valueOf(8), handoff.getCommittedClinicalStateVersion());
        assertEquals("thread-1", handoff.getThreadId());
        assertEquals("run-1", handoff.getRunId());
        assertEquals("event-1", handoff.getEventId());
        assertEquals("trace-1", handoff.getTraceId());
        assertEquals("VALID", handoff.getExecutionStatus());
        assertEquals("VALID", handoff.getDecisionStatus());
        assertEquals("CAUTION", handoff.getDispositionCode());
        assertEquals(chain.proposal.getProposalId(), handoff.getProposalId());
        assertEquals("COMMITTED", handoff.getCommitStatus());
        assertEquals(6, handoff.getGovernedReleaseRefs().size());
        assertTrue(handoff.getGovernedReleaseRefs().contains(
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF));
    }

    @Test
    void failedExecutionPreservesFailureWithoutInventingDispositionOrCommittedState() {
        U03NonProductionExecutionContext context = context();
        CapabilityBindingRecord capability = capabilityBinding();
        U03ResolvedNonProductionReleaseSet resolved = new U03ResolvedNonProductionReleaseSet(
                context.getReleaseRefs(), capability.getBindingId());
        U03RiskAssessmentCandidate failed = U03RiskAssessmentCandidate.failed("SYNTHETIC_DEPENDENCY_FAILURE");
        U03GovernedCandidateGateway.GovernedResult governed =
                new U03GovernedCandidateGateway.GovernedResult(
                        failed, capability, resolved.asCandidateReleaseBinding(), resolved);
        U03DecisionOutcome decision = new U03DecisionOutcome(
                "u03-decision-event-1",
                U03RiskAssessmentCandidate.FAILED,
                null,
                "SYNTHETIC_DEPENDENCY_FAILURE",
                Collections.<String>emptyList());

        U03OutboundHandoff handoff = new U03OutboundProducer().produce(
                context, governed, decision, null, null);

        assertEquals("FAILED", handoff.getExecutionStatus());
        assertEquals("SYNTHETIC_DEPENDENCY_FAILURE", handoff.getExecutionFailureReasonCode());
        assertEquals("FAILED", handoff.getDecisionStatus());
        assertNull(handoff.getDispositionCode());
        assertNull(handoff.getCommittedClinicalStateVersion());
        assertNull(handoff.getProposalId());
        assertNull(handoff.getCommitStatus());
    }

    @Test
    void commitConflictFailsClosedInsteadOfProducingStaleOutbound() {
        Chain chain = validChain(8);

        assertEquals("CONFLICT", chain.commitResult.status);
        assertThrows(IllegalStateException.class, () -> new U03OutboundProducer().produce(
                chain.context, chain.governed, chain.decision, chain.proposal, chain.commitResult));
    }

    @Test
    void validExecutionWithoutCommitFailsClosed() {
        Chain chain = validChain(7);

        assertThrows(IllegalStateException.class, () -> new U03OutboundProducer().produce(
                chain.context, chain.governed, chain.decision, chain.proposal, null));
    }

    @Test
    void proposalReleaseSubstitutionFailsClosed() {
        Chain chain = validChain(7);
        U03StateProposal substituted = new U03StateProposal(
                chain.proposal.getProposalId(),
                chain.proposal.getSourceDecisionRef(),
                Arrays.asList(
                        U03GovernedCandidateGateway.BINDING_ID,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF),
                true,
                chain.proposal.getStatePatch());

        assertThrows(IllegalStateException.class, () -> new U03OutboundProducer().produce(
                chain.context, chain.governed, chain.decision, substituted, chain.commitResult));
    }

    @Test
    void proposalStateVersionMismatchFailsClosed() {
        Chain chain = validChain(7);
        chain.proposal.getStatePatch().baseVersion = Integer.valueOf(6);

        assertThrows(IllegalStateException.class, () -> new U03OutboundProducer().produce(
                chain.context, chain.governed, chain.decision, chain.proposal, chain.commitResult));
    }

    private static Chain validChain(int currentVersion) {
        U03NonProductionExecutionContext context = context();
        CapabilityBindingRecord capability = capabilityBinding();
        U03ResolvedNonProductionReleaseSet resolved = new U03ResolvedNonProductionReleaseSet(
                context.getReleaseRefs(), capability.getBindingId());
        U03ReleaseBinding releaseBinding = resolved.asCandidateReleaseBinding();
        U03RiskAssessmentCandidate candidate = U03RiskAssessmentCandidate.valid(
                7,
                Collections.singletonList("evidence-1"),
                0.8d,
                "SYNTHETIC_UNCERTAINTY",
                Collections.singletonList("SYNTHETIC_LIMITATION"),
                Collections.singletonList("source-1"),
                Collections.singletonList("accepted-provenance-1"),
                capability.getBindingId(),
                capability.getCapabilityVersion(),
                releaseBinding.getRuleReleaseId(),
                releaseBinding.getKnowledgeReleaseId());
        U03GovernedCandidateGateway.GovernedResult governed =
                new U03GovernedCandidateGateway.GovernedResult(candidate, capability, releaseBinding, resolved);
        U03DecisionOutcome decision = new U03DecisionOutcome(
                "decision-1", "VALID", "CAUTION", "SYNTHETIC_RULE_DECISION", candidate.getEvidenceRefs());
        U03StateProposal proposal = new U03StateProposalFactory().createNonProductionValid(context, decision, governed);

        MechanicalVersionRepositoryFake repository = new MechanicalVersionRepositoryFake(new ArrayList<String>());
        repository.seed("cdp-1", currentVersion);
        StateCommitter committer = new StateCommitter(
                repository,
                (capabilityId, capabilityVersion) -> CapabilityPolicyPort.CapabilityDecision.authorized(),
                (path, capabilityId) -> FieldPermissionPort.FieldPermissionDecision.authorized(),
                (cdpId, capabilityId) -> ConsentPolicyPort.ConsentDecision.authorized(),
                source -> SourceValidationPort.SourceDecision.authorized(),
                new InMemoryIdempotencyFake(),
                new SyntheticAuditPortFake(CLOCK),
                new RecordingCommitEventEvidenceFake(),
                CLOCK);
        StateTypes.CommitResult commitResult = new U03CommitService(committer).commitNonProduction(context, proposal);
        return new Chain(context, governed, decision, proposal, commitResult);
    }

    private static U03NonProductionExecutionContext context() {
        U03ExecutionCommand command = new U03ExecutionCommand(
                "consult-1", "thread-1", "run-1", "event-1",
                "cdp-1", 7, "corr-1", "trace-1");
        U03AcceptedEvidenceBinding evidence = new U03AcceptedEvidenceBinding(
                "acceptance-1", 7,
                Collections.singletonList("evidence-1"),
                Collections.singletonList("source-1"),
                Collections.singletonList("accepted-provenance-1"));
        return new U03NonProductionExecutionContext(
                command,
                U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                evidence,
                "ci-nonprod");
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
                now.minusMinutes(1), null, now.minusMinutes(1));
    }

    private static final class Chain {
        final U03NonProductionExecutionContext context;
        final U03GovernedCandidateGateway.GovernedResult governed;
        final U03DecisionOutcome decision;
        final U03StateProposal proposal;
        final StateTypes.CommitResult commitResult;

        Chain(
                U03NonProductionExecutionContext context,
                U03GovernedCandidateGateway.GovernedResult governed,
                U03DecisionOutcome decision,
                U03StateProposal proposal,
                StateTypes.CommitResult commitResult) {
            this.context = context;
            this.governed = governed;
            this.decision = decision;
            this.proposal = proposal;
            this.commitResult = commitResult;
        }
    }
}
