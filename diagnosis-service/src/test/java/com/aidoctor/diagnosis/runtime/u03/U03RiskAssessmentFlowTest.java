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

class U03RiskAssessmentFlowTest {
    @Test
    void failedCandidateCannotBecomeNormalOutcome() {
        U03DecisionService service = new U03DecisionService((command, candidate, release) -> {
            throw new AssertionError("valid decision port must not run for FAILED candidate");
        });
        U03DecisionOutcome decision = service.decide(
                command("event-failed"),
                U03RiskAssessmentCandidate.failed("DEPENDENCY_FAILURE"),
                null);

        assertEquals("FAILED", decision.getStatus());
        assertNull(decision.getOutcomeCode());
        assertEquals("DEPENDENCY_FAILURE", decision.getReasonCode());
    }

    @Test
    void validDecisionMustUseFrozenOutcomeVocabulary() {
        U03DecisionService service = new U03DecisionService((command, candidate, release) ->
                new U03DecisionOutcome("decision-1", "VALID", "UNSUPPORTED", "TEST", candidate.getEvidenceRefs()));

        assertThrows(IllegalStateException.class, () -> service.decide(
                command("event-invalid-outcome"),
                U03RiskAssessmentCandidate.valid(Arrays.asList("evidence-1")),
                release()));
    }

    @Test
    void validProposalBindsInputVersionAndAllGovernedReleases() {
        U03ExecutionCommand command = command("event-valid");
        U03RiskAssessmentCandidate candidate = U03RiskAssessmentCandidate.valid(Arrays.asList("evidence-1"));
        U03DecisionOutcome decision = new U03DecisionOutcome(
                "decision-valid", "VALID", "CAUTION", "RULE_PACK_DECISION", candidate.getEvidenceRefs());
        U03GovernedCandidateGateway.GovernedResult governed = new U03GovernedCandidateGateway.GovernedResult(
                candidate, capabilityBinding(), release());

        U03StateProposal proposal = new U03StateProposalFactory().createValid(command, decision, governed);
        StateTypes.StatePatch patch = proposal.getStatePatch();

        assertEquals(Integer.valueOf(7), patch.baseVersion);
        assertEquals(3, proposal.getReleaseRefs().size());
        assertTrue(proposal.isFullReleaseEvidenceRequired());
        assertTrue(new StatePatchBoundaryValidator().validate(patch).valid);
        assertEquals("/patient_state/current_risk_assessment", patch.operations.get(0).path);
        assertEquals("RULE_DERIVED", patch.operations.get(0).source);
        assertTrue(patch.operations.get(0).value instanceof Map<?, ?>);
        Map<?, ?> value = (Map<?, ?>) patch.operations.get(0).value;
        assertEquals(Integer.valueOf(7), value.get("clinical_state_version"));
        assertEquals("rules-v1", value.get("rule_release_ref"));
        assertEquals("knowledge-v1", value.get("knowledge_release_ref"));
    }

    @Test
    void failedProposalDoesNotInventUnknownRuleOrKnowledgeRelease() {
        U03ExecutionCommand command = command("event-provider-down");
        U03DecisionOutcome decision = new U03DecisionService((c, candidate, release) -> {
            throw new AssertionError("valid port should not be called");
        }).decide(command, U03RiskAssessmentCandidate.failed("U03_GOVERNED_INVOCATION_FAILED"), null);

        U03StateProposal proposal = new U03StateProposalFactory().createFailed(
                command, decision, U03GovernedCandidateGateway.BINDING_ID);

        assertFalse(proposal.isFullReleaseEvidenceRequired());
        assertEquals(Collections.singletonList(U03GovernedCandidateGateway.BINDING_ID), proposal.getReleaseRefs());
        assertTrue(new StatePatchBoundaryValidator().validate(proposal.getStatePatch()).valid);
        Map<?, ?> value = (Map<?, ?>) proposal.getStatePatch().operations.get(0).value;
        assertNull(value.get("rule_release_ref"));
        assertNull(value.get("knowledge_release_ref"));
        assertNull(value.get("outcome_code"));
        assertEquals("FAILED", value.get("assessment_status"));
    }

    private static U03ExecutionCommand command(String eventId) {
        return new U03ExecutionCommand(
                "consult-1", "thread-1", "run-1", eventId, "cdp-1", 7, "corr-1", "trace-1");
    }

    private static U03ReleaseBinding release() {
        return new U03ReleaseBinding("c02-u03-v1-active", "rules-v1", "knowledge-v1", true);
    }

    private static CapabilityBindingRecord capabilityBinding() {
        LocalDateTime now = LocalDateTime.now();
        return new CapabilityBindingRecord(
                "c02-u03-v1-active",
                "C02",
                "1.0.0",
                "capset-v1",
                "aidoctor-v1-scope",
                "contracts-v1",
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
