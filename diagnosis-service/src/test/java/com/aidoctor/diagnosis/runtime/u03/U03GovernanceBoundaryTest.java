package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
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
    void releaseRegistryRejectsMissingInactiveOrDuplicateBinding() {
        U03ReleaseRegistry registry = new U03ReleaseRegistry();
        assertThrows(IllegalStateException.class, () -> registry.requireActive("c02-u03-v1"));

        registry.register(new U03ReleaseBinding("c02-u03-v1", "rules-v1", "knowledge-v1", false));
        assertThrows(IllegalStateException.class, () -> registry.requireActive("c02-u03-v1"));
        assertThrows(IllegalStateException.class, () ->
                registry.register(new U03ReleaseBinding("c02-u03-v1", "rules-v2", "knowledge-v2", true)));
    }

    @Test
    void releaseRegistryReturnsExplicitActiveReleaseTuple() {
        U03ReleaseRegistry registry = new U03ReleaseRegistry();
        registry.register(new U03ReleaseBinding("c02-u03-v1", "rules-v1", "knowledge-v1", true));
        U03ReleaseBinding binding = registry.requireActive("c02-u03-v1");
        assertEquals("rules-v1", binding.getRuleReleaseId());
        assertEquals("1.0.0", binding.getRuleReleaseVersion());
        assertEquals("knowledge-v1", binding.getKnowledgeReleaseId());
        assertEquals("1.0.0", binding.getKnowledgeReleaseVersion());
    }

    @Test
    void releaseScopeCannotBeNarrowerThanUnresolvedCapabilityScope() {
        LocalDateTime now = LocalDateTime.now();
        CapabilityBindingRecord unresolved = new CapabilityBindingRecord(
                "c02-u03-v1-active", "C02", "1.0.0", "capset-v1",
                "aidoctor-v1-scope", "contracts-v1",
                CapabilityBindingRecord.ANY, CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY, CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ACTIVE, now.minusMinutes(1), null, now.minusMinutes(1));
        U03ReleaseBinding narrowRelease = new U03ReleaseBinding(
                "c02-u03-v1-active", "rules-v1", "1.0.0", "knowledge-v1", "1.0.0",
                "population-a", U03ReleaseBinding.ANY, U03ReleaseBinding.ANY, U03ReleaseBinding.ANY,
                now.minusMinutes(1), null, Arrays.asList("approved-release-record"), true);
        assertFalse(narrowRelease.isCompatibleWith(unresolved));

        U03ReleaseBinding universalRelease = new U03ReleaseBinding(
                "c02-u03-v1-active", "rules-v1", "knowledge-v1", true);
        assertTrue(universalRelease.isCompatibleWith(unresolved));
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
