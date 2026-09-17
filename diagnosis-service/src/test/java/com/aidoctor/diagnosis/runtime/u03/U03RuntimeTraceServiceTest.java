package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.state.committer.CommitReasonCodes;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class U03RuntimeTraceServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-17T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void recordsExecutionGovernanceC02D09K09AndCommitFacts() {
        Chain chain = chain(7);
        RecordingTracePort tracePort = new RecordingTracePort();
        U03RuntimeTraceWriteOutcome outcome = new U03RuntimeTraceService(tracePort).record(
                chain.context, chain.governed, chain.decision, chain.proposal, chain.commitResult);

        assertEquals(U03RuntimeTraceWriteOutcome.RECORDED, outcome.getStatus());
        assertNull(outcome.getFailureCode());
        U03RuntimeTraceRecord record = tracePort.records.get(0);
        assertEquals("thread-1", record.getThreadId());
        assertEquals("run-1", record.getRunId());
        assertEquals("event-1", record.getEventId());
        assertEquals(7, record.getClinicalStateVersion());
        assertEquals("acceptance-1", record.getAcceptanceRef());
        assertEquals(6, record.getGovernedReleaseRefs().size());
        assertEquals("VALID", record.getC02Status());
        assertEquals("decision-1", record.getD09DecisionId());
        assertEquals("CAUTION", record.getD09OutcomeCode());
        assertEquals(chain.proposal.getProposalId(), record.getK09ProposalId());
        assertEquals(chain.proposal.getStatePatch().idempotencyKey, record.getK09IdempotencyKey());
        assertEquals("COMMITTED", record.getCommitStatus());
        assertEquals(Integer.valueOf(7), record.getCommitPreviousVersion());
        assertEquals(Integer.valueOf(8), record.getCommitCommittedVersion());
    }

    @Test
    void tracePersistenceFailureDoesNotRewriteCommittedRuntimeOutcome() {
        Chain chain = chain(7);
        U03RuntimeTracePort failing = record -> { throw new IllegalStateException("trace sink unavailable"); };

        U03RuntimeTraceWriteOutcome outcome = new U03RuntimeTraceService(failing).record(
                chain.context, chain.governed, chain.decision, chain.proposal, chain.commitResult);

        assertEquals(U03RuntimeTraceWriteOutcome.FAILED, outcome.getStatus());
        assertEquals(U03RuntimeTraceWriteOutcome.TRACE_PERSISTENCE_FAILED, outcome.getFailureCode());
        assertEquals("COMMITTED", chain.commitResult.status);
        assertEquals(Integer.valueOf(8), chain.commitResult.committedVersion);
        assertEquals("VALID", chain.decision.getStatus());
        assertEquals("CAUTION", chain.decision.getOutcomeCode());
    }

    @Test
    void correlationMismatchFailsBeforeTraceSink() {
        Chain chain = chain(7);
        RecordingTracePort tracePort = new RecordingTracePort();
        chain.proposal.getStatePatch().baseVersion = Integer.valueOf(6);

        assertThrows(IllegalStateException.class, () -> new U03RuntimeTraceService(tracePort).record(
                chain.context, chain.governed, chain.decision, chain.proposal, chain.commitResult));
        assertEquals(0, tracePort.records.size());
    }

    @Test
    void commitConflictIsTracedAsOperationalCommitFactNotClinicalNegative() {
        Chain chain = chain(8);
        RecordingTracePort tracePort = new RecordingTracePort();

        U03RuntimeTraceWriteOutcome outcome = new U03RuntimeTraceService(tracePort).record(
                chain.context, chain.governed, chain.decision, chain.proposal, chain.commitResult);

        assertEquals(U03RuntimeTraceWriteOutcome.RECORDED, outcome.getStatus());
        U03RuntimeTraceRecord record = tracePort.records.get(0);
        assertEquals("CONFLICT", record.getCommitStatus());
        assertEquals(CommitReasonCodes.VERSION_MISMATCH, record.getCommitReasonCode());
        assertEquals("VALID", record.getD09Status());
        assertEquals("CAUTION", record.getD09OutcomeCode());
        assertFalse(U03RiskAssessmentCandidate.FAILED.equals(record.getD09Status()));
    }

    private static Chain chain(int currentVersion) {
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
                Collections.<String>emptyList(),
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

    private static final class RecordingTracePort implements U03RuntimeTracePort {
        private final List<U03RuntimeTraceRecord> records = new ArrayList<U03RuntimeTraceRecord>();
        @Override
        public void record(U03RuntimeTraceRecord record) {
            records.add(record);
        }
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
