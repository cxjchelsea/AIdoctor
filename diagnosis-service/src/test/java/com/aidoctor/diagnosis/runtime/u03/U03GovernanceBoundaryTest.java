package com.aidoctor.diagnosis.runtime.u03;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

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
    void validCandidateCarriesVersionReleaseAndProvenanceMetadataWithoutBecomingCommittedState() {
        U03RiskAssessmentCandidate candidate = validCandidate(7, "evidence-1");
        assertFalse(candidate.isFailed());
        assertEquals("VALID", candidate.getStatus());
        assertEquals(Integer.valueOf(7), candidate.getClinicalStateVersion());
        assertEquals(Arrays.asList("evidence-1"), candidate.getEvidenceRefs());
        assertEquals("c02-u03-v1-active", candidate.getCapabilityBindingId());
        assertEquals("1.0.0", candidate.getCapabilityVersion());
        assertEquals("rules-v1", candidate.getRuleReleaseId());
        assertEquals("knowledge-v1", candidate.getKnowledgeReleaseId());
        assertEquals(Arrays.asList("source-1"), candidate.getSourceRefs());
        assertEquals(Arrays.asList("synthetic-eval"), candidate.getProvenance());
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

    private static U03RiskAssessmentCandidate validCandidate(int version, String evidenceRef) {
        return U03RiskAssessmentCandidate.valid(
                version,
                Arrays.asList(evidenceRef),
                0.8d,
                "SYNTHETIC_UNCERTAINTY",
                Collections.<String>emptyList(),
                Arrays.asList("source-1"),
                Arrays.asList("synthetic-eval"),
                "c02-u03-v1-active",
                "1.0.0",
                "rules-v1",
                "knowledge-v1");
    }
}
