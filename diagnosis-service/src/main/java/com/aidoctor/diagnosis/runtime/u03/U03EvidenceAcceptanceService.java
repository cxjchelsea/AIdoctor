package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;

/**
 * Business-owner input boundary. It checks that a VALID candidate was produced for
 * the exact state version and governed release tuple before D09 may consume it.
 */
public final class U03EvidenceAcceptanceService {
    public U03RiskAssessmentCandidate accept(
            U03ExecutionCommand command,
            U03RiskAssessmentCandidate candidate,
            CapabilityBindingRecord capabilityBinding,
            U03ReleaseBinding releaseBinding) {
        if (command == null || candidate == null) throw new IllegalArgumentException("U03 acceptance inputs are required");
        if (candidate.isFailed()) return candidate;
        if (capabilityBinding == null || releaseBinding == null || !releaseBinding.isActive()) {
            return U03RiskAssessmentCandidate.failed("U03_RELEASE_CONTEXT_UNAVAILABLE");
        }
        if (!Integer.valueOf(command.clinicalStateVersion).equals(candidate.getClinicalStateVersion())) {
            return U03RiskAssessmentCandidate.failed("U03_CLINICAL_STATE_VERSION_MISMATCH");
        }
        if (!capabilityBinding.getBindingId().equals(candidate.getCapabilityBindingId())
                || !capabilityBinding.getCapabilityVersion().equals(candidate.getCapabilityVersion())
                || !releaseBinding.getRuleReleaseId().equals(candidate.getRuleReleaseId())
                || !releaseBinding.getKnowledgeReleaseId().equals(candidate.getKnowledgeReleaseId())) {
            return U03RiskAssessmentCandidate.failed("U03_RELEASE_BINDING_MISMATCH");
        }
        return candidate;
    }
}
