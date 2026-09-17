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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class U03NonProductionCommitIntegrationTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-17T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void validProposalCommitsOnlyThroughExistingStateCommitter() {
        Fixture fixture = new Fixture(7);
        U03NonProductionExecutionContext context = context();
        U03StateProposal proposal = proposal(context);

        StateTypes.CommitResult result = fixture.service.commitNonProduction(context, proposal);

        assertEquals("COMMITTED", result.status);
        assertEquals(Integer.valueOf(7), result.previousVersion);
        assertEquals(Integer.valueOf(8), result.committedVersion);
        assertEquals(1, fixture.repository.commitCalls());
        assertEquals(8, fixture.repository.currentVersion("cdp-1"));
    }

    @Test
    void staleClinicalStateVersionConflictsWithoutMutation() {
        Fixture fixture = new Fixture(8);
        U03NonProductionExecutionContext context = context();

        StateTypes.CommitResult result = fixture.service.commitNonProduction(context, proposal(context));

        assertEquals("CONFLICT", result.status);
        assertEquals(CommitReasonCodes.VERSION_MISMATCH, result.reasonCode);
        assertEquals(0, fixture.repository.commitCalls());
        assertEquals(8, fixture.repository.currentVersion("cdp-1"));
    }

    @Test
    void replayUsesStateCommitterIdempotencyAndDoesNotMutateTwice() {
        Fixture fixture = new Fixture(7);
        U03NonProductionExecutionContext context = context();
        U03StateProposal proposal = proposal(context);

        StateTypes.CommitResult first = fixture.service.commitNonProduction(context, proposal);
        StateTypes.CommitResult replay = fixture.service.commitNonProduction(context, proposal);

        assertEquals("COMMITTED", first.status);
        assertEquals(first.status, replay.status);
        assertEquals(first.previousVersion, replay.previousVersion);
        assertEquals(first.committedVersion, replay.committedVersion);
        assertEquals(1, fixture.repository.commitCalls());
        assertEquals(8, fixture.repository.currentVersion("cdp-1"));
    }

    @Test
    void unsupportedProposalOperationFailsBeforeStateCommitter() {
        Fixture fixture = new Fixture(7);
        U03NonProductionExecutionContext context = context();
        U03StateProposal proposal = proposal(context);
        proposal.getStatePatch().operations.get(0).path = "/patient_state/other";

        assertThrows(IllegalStateException.class, () -> fixture.service.commitNonProduction(context, proposal));
        assertEquals(0, fixture.repository.readCalls());
        assertEquals(0, fixture.repository.commitCalls());
    }

    @Test
    void releaseEvidenceSubstitutionFailsBeforeStateCommitter() {
        Fixture fixture = new Fixture(7);
        U03NonProductionExecutionContext context = context();
        U03StateProposal original = proposal(context);
        U03StateProposal substituted = new U03StateProposal(
                original.getProposalId(),
                original.getSourceDecisionRef(),
                Arrays.asList(
                        U03GovernedCandidateGateway.BINDING_ID,
                        "KR-U03-SOURCE-001@0.1.1-candidate",
                        U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF),
                true,
                original.getStatePatch());

        assertThrows(IllegalStateException.class, () -> fixture.service.commitNonProduction(context, substituted));
        assertEquals(0, fixture.repository.readCalls());
        assertEquals(0, fixture.repository.commitCalls());
    }

    private static U03StateProposal proposal(U03NonProductionExecutionContext context) {
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
                "decision-1", "VALID", "CAUTION", "SYNTHETIC_RULE_DECISION",
                candidate.getEvidenceRefs());
        return new U03StateProposalFactory().createNonProductionValid(context, decision, governed);
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

    private static final class Fixture {
        final List<String> order = new ArrayList<String>();
        final MechanicalVersionRepositoryFake repository = new MechanicalVersionRepositoryFake(order);
        final U03CommitService service;

        Fixture(int currentVersion) {
            repository.seed("cdp-1", currentVersion);
            InMemoryIdempotencyFake idempotency = new InMemoryIdempotencyFake(order);
            SyntheticAuditPortFake audit = new SyntheticAuditPortFake(CLOCK, order);
            RecordingCommitEventEvidenceFake events = new RecordingCommitEventEvidenceFake(order);
            StateCommitter committer = new StateCommitter(
                    repository,
                    (capabilityId, capabilityVersion) -> CapabilityPolicyPort.CapabilityDecision.authorized(),
                    (path, capabilityId) -> FieldPermissionPort.FieldPermissionDecision.authorized(),
                    (cdpId, capabilityId) -> ConsentPolicyPort.ConsentDecision.authorized(),
                    source -> SourceValidationPort.SourceDecision.authorized(),
                    idempotency,
                    audit,
                    events,
                    CLOCK);
            service = new U03CommitService(committer);
        }
    }
}
