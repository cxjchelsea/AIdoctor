package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;

/**
 * Business-owner input boundary. It checks that a VALID candidate was produced for
 * the exact state version and governed release tuple before D09 may consume it.
 */
public final class U03EvidenceAcceptanceService {
    /**
     * Historical component-level acceptance path. It intentionally keeps the
     * existing active-release requirement and must not be used to infer CD-07
     * non-production release activation.
     */
    public U03RiskAssessmentCandidate accept(
            U03ExecutionCommand command,
            U03RiskAssessmentCandidate candidate,
            CapabilityBindingRecord capabilityBinding,
            U03ReleaseBinding releaseBinding) {
        if (command == null || candidate == null) {
            throw new IllegalArgumentException("U03 acceptance inputs are required");
        }
        if (candidate.isFailed()) return candidate;
        if (capabilityBinding == null || releaseBinding == null || !releaseBinding.isActive()) {
            return U03RiskAssessmentCandidate.failed("U03_RELEASE_CONTEXT_UNAVAILABLE");
        }
        return validateCandidate(
                command,
                candidate,
                capabilityBinding,
                releaseBinding.getRuleReleaseId(),
                releaseBinding.getKnowledgeReleaseId());
    }

    /**
     * Authorized CD-07 acceptance path for EXPLICIT_NON_PRODUCTION_BINDING_ONLY.
     *
     * <p>The resolved release set remains candidate/frozen/evaluated and is not
     * required (or allowed) to masquerade as production-active metadata.</p>
     */
    public U03RiskAssessmentCandidate accept(
            U03NonProductionExecutionContext context,
            U03GovernedCandidateGateway.GovernedResult governedResult) {
        if (context == null || governedResult == null) {
            throw new IllegalArgumentException("CD-07 non-production acceptance inputs are required");
        }

        U03RiskAssessmentCandidate candidate = governedResult.getCandidate();
        if (candidate == null) {
            throw new IllegalArgumentException("governed candidate is required");
        }
        if (candidate.isFailed()) return candidate;

        U03AcceptedEvidenceBinding acceptedEvidence = context.requireAcceptedEvidenceBinding();
        U03ResolvedNonProductionReleaseSet resolved =
                governedResult.getResolvedNonProductionReleaseSet();
        CapabilityBindingRecord capabilityBinding = governedResult.getCapabilityBinding();

        if (resolved == null || capabilityBinding == null) {
            return U03RiskAssessmentCandidate.failed("U03_RELEASE_CONTEXT_UNAVAILABLE");
        }
        if (!U03NonProductionExecutionContext.BINDING_MODE.equals(context.getBindingMode())) {
            return U03RiskAssessmentCandidate.failed("U03_RELEASE_CONTEXT_UNAVAILABLE");
        }
        if (!sameFrozenRefs(context.getReleaseRefs(), resolved.getRefs())
                || !resolved.getCapabilityBindingId().equals(capabilityBinding.getBindingId())) {
            return U03RiskAssessmentCandidate.failed("U03_RELEASE_BINDING_MISMATCH");
        }

        U03RiskAssessmentCandidate validated = validateCandidate(
                context.getCommand(),
                candidate,
                capabilityBinding,
                resolved.getRefs().getRuleReleaseRef(),
                resolved.getRefs().getKnowledgeReleaseRef());
        if (validated.isFailed()) return validated;

        if (!acceptedEvidence.getEvidenceRefs().equals(candidate.getEvidenceRefs())
                || !candidate.getSourceRefs().containsAll(acceptedEvidence.getSourceRefs())
                || !candidate.getProvenance().containsAll(acceptedEvidence.getProvenanceRefs())) {
            return U03RiskAssessmentCandidate.failed("U03_ACCEPTED_EVIDENCE_BINDING_MISMATCH");
        }
        return candidate;
    }

    private static U03RiskAssessmentCandidate validateCandidate(
            U03ExecutionCommand command,
            U03RiskAssessmentCandidate candidate,
            CapabilityBindingRecord capabilityBinding,
            String expectedRuleReleaseRef,
            String expectedKnowledgeReleaseRef) {
        if (!Integer.valueOf(command.clinicalStateVersion).equals(candidate.getClinicalStateVersion())) {
            return U03RiskAssessmentCandidate.failed("U03_CLINICAL_STATE_VERSION_MISMATCH");
        }
        if (!capabilityBinding.getBindingId().equals(candidate.getCapabilityBindingId())
                || !capabilityBinding.getCapabilityVersion().equals(candidate.getCapabilityVersion())
                || !expectedRuleReleaseRef.equals(candidate.getRuleReleaseId())
                || !expectedKnowledgeReleaseRef.equals(candidate.getKnowledgeReleaseId())) {
            return U03RiskAssessmentCandidate.failed("U03_RELEASE_BINDING_MISMATCH");
        }
        return candidate;
    }

    private static boolean sameFrozenRefs(
            U03ExplicitNonProductionReleaseRefs left,
            U03ExplicitNonProductionReleaseRefs right) {
        return left != null
                && right != null
                && left.getKnowledgeReleaseRef().equals(right.getKnowledgeReleaseRef())
                && left.getRuleReleaseRef().equals(right.getRuleReleaseRef())
                && left.getCoverageContractRef().equals(right.getCoverageContractRef())
                && left.getPolicyReleaseRef().equals(right.getPolicyReleaseRef())
                && left.getPolicyPairRef().equals(right.getPolicyPairRef());
    }
}
