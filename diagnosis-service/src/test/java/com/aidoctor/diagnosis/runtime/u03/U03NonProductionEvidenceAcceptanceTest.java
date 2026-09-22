package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class U03NonProductionEvidenceAcceptanceTest {
    @Test
    void acceptsCandidateBoundToExactNonProductionContextWithoutProductionActivation() {
        U03NonProductionExecutionContext context = context();
        CapabilityBindingRecord capability = capabilityBinding();
        U03ResolvedNonProductionReleaseSet resolved =
                new U03ExactNonProductionReleaseResolver().resolve(context, capability);
        U03ReleaseBinding candidateRelease = resolved.asCandidateReleaseBinding();
        U03RiskAssessmentCandidate candidate = validCandidate(context, capability, candidateRelease);
        U03GovernedCandidateGateway.GovernedResult governed =
                new U03GovernedCandidateGateway.GovernedResult(
                        candidate, capability, candidateRelease, resolved);

        U03RiskAssessmentCandidate accepted =
                new U03EvidenceAcceptanceService().accept(context, governed);

        assertFalse(accepted.isFailed());
        assertFalse(candidateRelease.isActive());
        assertEquals(
                U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                accepted.getRuleReleaseId());
    }

    @Test
    void rejectsHistoricalResultWithoutExplicitResolvedNonProductionSet() {
        U03NonProductionExecutionContext context = context();
        CapabilityBindingRecord capability = capabilityBinding();
        U03ReleaseBinding historical = new U03ReleaseBinding(
                U03GovernedCandidateGateway.BINDING_ID,
                U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                true);
        U03RiskAssessmentCandidate candidate = validCandidate(context, capability, historical);
        U03GovernedCandidateGateway.GovernedResult governed =
                new U03GovernedCandidateGateway.GovernedResult(candidate, capability, historical);

        U03RiskAssessmentCandidate rejected =
                new U03EvidenceAcceptanceService().accept(context, governed);

        assertTrue(rejected.isFailed());
        assertEquals("U03_RELEASE_CONTEXT_UNAVAILABLE", rejected.getFailureReasonCode());
    }

    @Test
    void rejectsCandidateWithDifferentClinicalStateVersion() {
        U03NonProductionExecutionContext context = context();
        CapabilityBindingRecord capability = capabilityBinding();
        U03ResolvedNonProductionReleaseSet resolved =
                new U03ExactNonProductionReleaseResolver().resolve(context, capability);
        U03ReleaseBinding release = resolved.asCandidateReleaseBinding();
        U03RiskAssessmentCandidate candidate = U03RiskAssessmentCandidate.valid(
                6,
                context.getAcceptedEvidenceBinding().getEvidenceRefs(),
                0.8d,
                "SYNTHETIC_UNCERTAINTY",
                Collections.<String>emptyList(),
                context.getAcceptedEvidenceBinding().getSourceRefs(),
                context.getAcceptedEvidenceBinding().getProvenanceRefs(),
                capability.getBindingId(),
                capability.getCapabilityVersion(),
                release.getRuleReleaseId(),
                release.getKnowledgeReleaseId());
        U03GovernedCandidateGateway.GovernedResult governed =
                new U03GovernedCandidateGateway.GovernedResult(
                        candidate, capability, release, resolved);

        U03RiskAssessmentCandidate rejected =
                new U03EvidenceAcceptanceService().accept(context, governed);

        assertTrue(rejected.isFailed());
        assertEquals("U03_CLINICAL_STATE_VERSION_MISMATCH", rejected.getFailureReasonCode());
    }

    @Test
    void rejectsCandidateThatDropsAcceptedEvidenceProvenance() {
        U03NonProductionExecutionContext context = context();
        CapabilityBindingRecord capability = capabilityBinding();
        U03ResolvedNonProductionReleaseSet resolved =
                new U03ExactNonProductionReleaseResolver().resolve(context, capability);
        U03ReleaseBinding release = resolved.asCandidateReleaseBinding();
        U03RiskAssessmentCandidate candidate = U03RiskAssessmentCandidate.valid(
                context.getCommand().clinicalStateVersion,
                context.getAcceptedEvidenceBinding().getEvidenceRefs(),
                0.8d,
                "SYNTHETIC_UNCERTAINTY",
                Collections.<String>emptyList(),
                context.getAcceptedEvidenceBinding().getSourceRefs(),
                Collections.singletonList("other-provenance"),
                capability.getBindingId(),
                capability.getCapabilityVersion(),
                release.getRuleReleaseId(),
                release.getKnowledgeReleaseId());
        U03GovernedCandidateGateway.GovernedResult governed =
                new U03GovernedCandidateGateway.GovernedResult(
                        candidate, capability, release, resolved);

        U03RiskAssessmentCandidate rejected =
                new U03EvidenceAcceptanceService().accept(context, governed);

        assertTrue(rejected.isFailed());
        assertEquals("U03_ACCEPTED_EVIDENCE_BINDING_MISMATCH", rejected.getFailureReasonCode());
    }

    private static U03RiskAssessmentCandidate validCandidate(
            U03NonProductionExecutionContext context,
            CapabilityBindingRecord capability,
            U03ReleaseBinding release) {
        U03AcceptedEvidenceBinding evidence = context.getAcceptedEvidenceBinding();
        return U03RiskAssessmentCandidate.valid(
                context.getCommand().clinicalStateVersion,
                evidence.getEvidenceRefs(),
                0.8d,
                "SYNTHETIC_UNCERTAINTY",
                Collections.<String>emptyList(),
                evidence.getSourceRefs(),
                evidence.getProvenanceRefs(),
                capability.getBindingId(),
                capability.getCapabilityVersion(),
                release.getRuleReleaseId(),
                release.getKnowledgeReleaseId());
    }

    private static U03NonProductionExecutionContext context() {
        U03ExecutionCommand command = new U03ExecutionCommand(
                "consult-1", "thread-1", "run-1", "event-1",
                "cdp-1", 7, "corr-1", "trace-1");
        U03AcceptedEvidenceBinding evidence = new U03AcceptedEvidenceBinding(
                "acceptance-1",
                7,
                Arrays.asList("evidence-1", "evidence-2"),
                Collections.singletonList("source-1"),
                Collections.singletonList("accepted-by-u02"));
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
                now.minusMinutes(1),
                null,
                now.minusMinutes(1));
    }
}
