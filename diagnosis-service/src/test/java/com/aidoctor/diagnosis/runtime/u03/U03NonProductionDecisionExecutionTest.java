package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class U03NonProductionDecisionExecutionTest {
    @Test
    void acceptedC02ResultReachesD09WithExactFrozenCoverageAndPolicyRefs() {
        U03NonProductionDecisionPort d09 = new U03NonProductionDecisionPort() {
            @Override
            public U03DecisionOutcome decide(
                    U03NonProductionExecutionContext context,
                    U03RiskAssessmentCandidate acceptedCandidate,
                    U03ResolvedNonProductionReleaseSet resolvedReleaseSet) {
                assertEquals(7, context.getCommand().clinicalStateVersion);
                assertEquals(
                        U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                        resolvedReleaseSet.getRefs().getCoverageContractRef());
                assertEquals(
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                        resolvedReleaseSet.getRefs().getPolicyReleaseRef());
                assertEquals(
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF,
                        resolvedReleaseSet.getRefs().getPolicyPairRef());
                assertFalse(resolvedReleaseSet.asCandidateReleaseBinding().isActive());
                return new U03DecisionOutcome(
                        "decision-1",
                        U03RiskAssessmentCandidate.VALID,
                        "CAUTION",
                        "FROZEN_D09_TEST_DECISION",
                        acceptedCandidate.getEvidenceRefs());
            }

            @Override
            public U03DecisionOutcome decide(
                    U03ExecutionCommand command,
                    U03RiskAssessmentCandidate candidate,
                    U03ReleaseBinding releaseBinding) {
                throw new AssertionError("historical D09 path must not run");
            }
        };

        U03DecisionOutcome outcome = new U03DecisionService(d09).decide(context(), governedResult(validCandidate()));

        assertEquals(U03RiskAssessmentCandidate.VALID, outcome.getStatus());
        assertEquals("CAUTION", outcome.getOutcomeCode());
        assertEquals(Collections.singletonList("evidence-1"), outcome.getEvidenceRefs());
    }

    @Test
    void legacyD09PortCannotSilentlyIgnoreCoverageAndPolicyBindings() {
        U03DecisionPort legacyOnly = (command, candidate, releaseBinding) ->
                new U03DecisionOutcome(
                        "legacy-decision",
                        U03RiskAssessmentCandidate.VALID,
                        "CAUTION",
                        "LEGACY",
                        candidate.getEvidenceRefs());

        assertThrows(IllegalStateException.class, () ->
                new U03DecisionService(legacyOnly).decide(context(), governedResult(validCandidate())));
    }

    @Test
    void failedAcceptedCandidateDoesNotInvokeD09OrBecomeNegativeClinicalConclusion() {
        U03NonProductionDecisionPort mustNotRun = new U03NonProductionDecisionPort() {
            @Override
            public U03DecisionOutcome decide(
                    U03NonProductionExecutionContext context,
                    U03RiskAssessmentCandidate acceptedCandidate,
                    U03ResolvedNonProductionReleaseSet resolvedReleaseSet) {
                throw new AssertionError("D09 must not run for failed accepted candidate");
            }

            @Override
            public U03DecisionOutcome decide(
                    U03ExecutionCommand command,
                    U03RiskAssessmentCandidate candidate,
                    U03ReleaseBinding releaseBinding) {
                throw new AssertionError("historical D09 path must not run");
            }
        };

        U03GovernedCandidateGateway.GovernedResult failedResult = governedResult(
                U03RiskAssessmentCandidate.failed("U03_GOVERNED_INVOCATION_FAILED"));
        U03DecisionOutcome outcome = new U03DecisionService(mustNotRun).decide(context(), failedResult);

        assertEquals(U03RiskAssessmentCandidate.FAILED, outcome.getStatus());
        assertNull(outcome.getOutcomeCode());
        assertEquals("U03_GOVERNED_INVOCATION_FAILED", outcome.getReasonCode());
    }

    @Test
    void unsupportedNewDispositionVocabularyIsRejected() {
        U03NonProductionDecisionPort unsafeVocabulary = new U03NonProductionDecisionPort() {
            @Override
            public U03DecisionOutcome decide(
                    U03NonProductionExecutionContext context,
                    U03RiskAssessmentCandidate acceptedCandidate,
                    U03ResolvedNonProductionReleaseSet resolvedReleaseSet) {
                return new U03DecisionOutcome(
                        "decision-safe",
                        U03RiskAssessmentCandidate.VALID,
                        "SAFE",
                        "UNAUTHORIZED_NEW_SEMANTIC",
                        acceptedCandidate.getEvidenceRefs());
            }

            @Override
            public U03DecisionOutcome decide(
                    U03ExecutionCommand command,
                    U03RiskAssessmentCandidate candidate,
                    U03ReleaseBinding releaseBinding) {
                throw new AssertionError("historical D09 path must not run");
            }
        };

        assertThrows(IllegalStateException.class, () ->
                new U03DecisionService(unsafeVocabulary).decide(context(), governedResult(validCandidate())));
    }

    @Test
    void D09CannotDropAcceptedC02EvidenceRefs() {
        U03NonProductionDecisionPort dropsEvidence = new U03NonProductionDecisionPort() {
            @Override
            public U03DecisionOutcome decide(
                    U03NonProductionExecutionContext context,
                    U03RiskAssessmentCandidate acceptedCandidate,
                    U03ResolvedNonProductionReleaseSet resolvedReleaseSet) {
                return new U03DecisionOutcome(
                        "decision-2",
                        U03RiskAssessmentCandidate.VALID,
                        "HIGH_RISK",
                        "FROZEN_D09_TEST_DECISION",
                        Collections.<String>emptyList());
            }

            @Override
            public U03DecisionOutcome decide(
                    U03ExecutionCommand command,
                    U03RiskAssessmentCandidate candidate,
                    U03ReleaseBinding releaseBinding) {
                throw new AssertionError("historical D09 path must not run");
            }
        };

        assertThrows(IllegalStateException.class, () ->
                new U03DecisionService(dropsEvidence).decide(context(), governedResult(validCandidate())));
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
                Collections.singletonList("provenance-1"));
        return new U03NonProductionExecutionContext(
                command,
                U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                evidence,
                "ci-nonprod");
    }

    private static U03GovernedCandidateGateway.GovernedResult governedResult(
            U03RiskAssessmentCandidate candidate) {
        CapabilityBindingRecord binding = capabilityBinding();
        U03ResolvedNonProductionReleaseSet resolved = new U03ResolvedNonProductionReleaseSet(
                U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                U03GovernedCandidateGateway.BINDING_ID);
        return new U03GovernedCandidateGateway.GovernedResult(
                candidate,
                binding,
                resolved.asCandidateReleaseBinding(),
                resolved);
    }

    private static U03RiskAssessmentCandidate validCandidate() {
        return U03RiskAssessmentCandidate.valid(
                7,
                Collections.singletonList("evidence-1"),
                0.8d,
                "SYNTHETIC_UNCERTAINTY",
                Collections.<String>emptyList(),
                Collections.singletonList("source-1"),
                Arrays.asList("provenance-1", "coverage-provenance"),
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
