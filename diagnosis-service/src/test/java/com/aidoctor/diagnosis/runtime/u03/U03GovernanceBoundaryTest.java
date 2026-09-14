package com.aidoctor.diagnosis.runtime.u03;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class U03GovernanceBoundaryTest {
    @Test
    void failedCandidateRemainsExplicitFailure() {
        U03RiskAssessmentCandidate candidate = U03RiskAssessmentCandidate.failed("DEPENDENCY_FAILURE");
        assertTrue(candidate.isFailed());
        assertEquals("FAILED", candidate.getStatus());
        assertTrue(candidate.getEvidenceRefs().isEmpty());
        assertEquals("DEPENDENCY_FAILURE", candidate.getFailureReasonCode());
    }

    @Test
    void validCandidateCarriesEvidenceWithoutBecomingCommittedState() {
        U03RiskAssessmentCandidate candidate = U03RiskAssessmentCandidate.valid(Arrays.asList("evidence-1"));
        assertFalse(candidate.isFailed());
        assertEquals("VALID", candidate.getStatus());
        assertEquals(Arrays.asList("evidence-1"), candidate.getEvidenceRefs());
    }

    @Test
    void releaseRegistryRejectsMissingOrInactiveBinding() {
        U03ReleaseRegistry registry = new U03ReleaseRegistry();
        assertThrows(IllegalStateException.class, () -> registry.requireActive("c02-u03-v1"));

        registry.register(new U03ReleaseBinding("c02-u03-v1", "rules-v1", "knowledge-v1", false));
        assertThrows(IllegalStateException.class, () -> registry.requireActive("c02-u03-v1"));
    }

    @Test
    void releaseRegistryReturnsExplicitActiveReleaseTuple() {
        U03ReleaseRegistry registry = new U03ReleaseRegistry();
        registry.register(new U03ReleaseBinding("c02-u03-v1", "rules-v1", "knowledge-v1", true));
        U03ReleaseBinding binding = registry.requireActive("c02-u03-v1");
        assertEquals("rules-v1", binding.getRuleReleaseId());
        assertEquals("knowledge-v1", binding.getKnowledgeReleaseId());
    }
}
