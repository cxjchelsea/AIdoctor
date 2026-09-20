package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.state.committer.StatePatchBoundaryValidator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class U03NonProductionStateProposalTest {
    @Test
    void createsTypedK09ProposalWithFullFrozenReleaseSetWithoutCommittingState() {
        U03NonProductionExecutionContext context = context();
        U03GovernedCandidateGateway.GovernedResult governed = governed(context);
        U03DecisionOutcome decision = new U03DecisionOutcome(
                "decision-1",
                U03RiskAssessmentCandidate.VALID,
                "CAUTION",
                "SYNTHETIC_RULE_DECISION",
                governed.getCandidate().getEvidenceRefs());

        U03StateProposal proposal = new U03StateProposalFactory()
                .createNonProductionValid(context, decision, governed);

        assertTrue(proposal.isFullReleaseEvidenceRequired());
        assertEquals(6, proposal.getReleaseRefs().size());
        assertEquals(U03GovernedCandidateGateway.BINDING_ID, proposal.getReleaseRefs().get(0));
        assertEquals(U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                proposal.getReleaseRefs().get(1));
        assertEquals(U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                proposal.getReleaseRefs().get(2));
        assertEquals(U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                proposal.getReleaseRefs().get(3));
        assertEquals(U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                proposal.getReleaseRefs().get(4));
        assertEquals(U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF,
                proposal.getReleaseRefs().get(5));

        StateTypes.StatePatch patch = proposal.getStatePatch();
        assertEquals(Integer.valueOf(7), patch.baseVersion);
        assertEquals("u03-event-event-1", patch.idempotencyKey);
        assertTrue(new StatePatchBoundaryValidator().validate(patch).valid);
        assertEquals("/patient_state/current_risk_assessment", patch.operations.get(0).path);
        assertEquals("RULE_DERIVED", patch.operations.get(0).source);

        Map<?, ?> value = (Map<?, ?>) patch.operations.get(0).value;
        assertEquals(U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                value.get("knowledge_release_ref"));
        assertEquals(U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                value.get("rule_release_ref"));
        assertEquals(U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                value.get("coverage_contract_ref"));
        assertEquals(U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                value.get("policy_release_ref"));
        assertEquals(U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF,
                value.get("policy_pair_ref"));
        assertEquals(governed.getCandidate().getEvidenceRefs(), value.get("evidence_refs"));
    }

    @Test
    void rejectsHistoricalGovernedResultWithoutResolvedNonProductionReleaseSet() {
        U03NonProductionExecutionContext context = context();
        U03RiskAssessmentCandidate candidate = candidate(context);
        U03GovernedCandidateGateway.GovernedResult historical =
                new U03GovernedCandidateGateway.GovernedResult(
                        candidate,
                        capabilityBinding(),
                        new U03ReleaseBinding(U03GovernedCandidateGateway.BINDING_ID,
                                "rules-v1", "knowledge-v1", true));
        U03DecisionOutcome decision = new U03DecisionOutcome(
                "decision-legacy",
                U03RiskAssessmentCandidate.VALID,
                "CAUTION",
                "SYNTHETIC_RULE_DECISION",
                candidate.getEvidenceRefs());

        assertThrows(IllegalStateException.class, () ->
                new U03StateProposalFactory().createNonProductionValid(context, decision, historical));
    }

    @Test
    void rejectsDecisionThatDoesNotPreserveAcceptedC02Evidence() {
        U03NonProductionExecutionContext context = context();
        U03GovernedCandidateGateway.GovernedResult governed = governed(context);
        U03DecisionOutcome decision = new U03DecisionOutcome(
                "decision-bad-evidence",
                U03RiskAssessmentCandidate.VALID,
                "CAUTION",
                "SYNTHETIC_RULE_DECISION",
                Collections.singletonList("different-evidence"));

        assertThrows(IllegalStateException.class, () ->
                new U03StateProposalFactory().createNonProductionValid(context, decision, governed));
    }

    @Test
    void failedProposalDoesNotInventFrozenClinicalOutcomeOrFullReleaseEvidence() {
        U03ExecutionCommand command = context().getCommand();
        U03DecisionOutcome failed = new U03DecisionOutcome(
                "decision-failed",
                U03RiskAssessmentCandidate.FAILED,
                null,
                "U03_RELEASE_CONTEXT_UNAVAILABLE",
                Collections.<String>emptyList());

        U03StateProposal proposal = new U03StateProposalFactory().createFailed(
                command, failed, U03GovernedCandidateGateway.BINDING_ID);

        assertFalse(proposal.isFullReleaseEvidenceRequired());
        Map<?, ?> value = (Map<?, ?>) proposal.getStatePatch().operations.get(0).value;
        assertEquals("FAILED", value.get("assessment_status"));
        assertNull(value.get("outcome_code"));
        assertNull(value.get("coverage_contract_ref"));
        assertNull(value.get("policy_release_ref"));
        assertNull(value.get("policy_pair_ref"));
    }

    private static U03NonProductionExecutionContext context() {
        U03ExecutionCommand command = new U03ExecutionCommand(
                "consult-1", "thread-1", "run-1", "event-1",
                "cdp-1", 7, "corr-1", "trace-1");
        U03AcceptedEvidenceBinding evidence = new U03AcceptedEvidenceBinding(
                "acceptance-1",
                7,
                Collections.singletonList("evidence-1"),
                Collections.singletonList("source-1"),
                Collections.singletonList("accepted-provenance-1"));
        return new U03NonProductionExecutionContext(
                command,
                U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                evidence,
                "ci-nonprod");
    }

    private static U03GovernedCandidateGateway.GovernedResult governed(
            U03NonProductionExecutionContext context) {
        CapabilityBindingRecord binding = capabilityBinding();
        U03ResolvedNonProductionReleaseSet resolved = new U03ResolvedNonProductionReleaseSet(
                context.getReleaseRefs(), binding.getBindingId());
        return new U03GovernedCandidateGateway.GovernedResult(
                candidate(context), binding, resolved.asCandidateReleaseBinding(), resolved);
    }

    private static U03RiskAssessmentCandidate candidate(U03NonProductionExecutionContext context) {
        return U03RiskAssessmentCandidate.valid(
                context.getCommand().clinicalStateVersion,
                context.getAcceptedEvidenceBinding().getEvidenceRefs(),
                0.8d,
                "SYNTHETIC_UNCERTAINTY",
                Collections.<String>emptyList(),
                context.getAcceptedEvidenceBinding().getSourceRefs(),
                Arrays.asList("accepted-provenance-1",
                        U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF),
                U03GovernedCandidateGateway.BINDING_ID,
                "1.0.0",
                U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF);
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
}
