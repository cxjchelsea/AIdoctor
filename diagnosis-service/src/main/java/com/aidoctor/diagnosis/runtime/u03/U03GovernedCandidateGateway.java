package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityExecutionContext;
import com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard;

/**
 * U03 governed invocation boundary. Authorization, accepted evidence, and release
 * binding must all succeed before the CD-07 candidate provider is called.
 */
public final class U03GovernedCandidateGateway {
    public static final String BINDING_ID = "c02-u03-v1-active";
    public static final String CAPABILITY_ID = "C02";
    public static final String SCOPE_VERSION = "aidoctor-v1-scope";
    public static final String CONTRACT_VERSION = "contracts-v1";

    private final CapabilityInvocationGuard invocationGuard;
    private final U03ReleaseRegistry releaseRegistry;
    private final U03ExactNonProductionReleaseResolver exactReleaseResolver;
    private final U03CandidateProvider provider;
    private final boolean enabled;

    public U03GovernedCandidateGateway(CapabilityInvocationGuard invocationGuard,
            U03ReleaseRegistry releaseRegistry, U03CandidateProvider provider, boolean enabled) {
        this(invocationGuard, releaseRegistry, new U03ExactNonProductionReleaseResolver(), provider, enabled);
    }

    U03GovernedCandidateGateway(
            CapabilityInvocationGuard invocationGuard,
            U03ReleaseRegistry releaseRegistry,
            U03ExactNonProductionReleaseResolver exactReleaseResolver,
            U03CandidateProvider provider,
            boolean enabled) {
        if (invocationGuard == null || releaseRegistry == null || exactReleaseResolver == null || provider == null) {
            throw new IllegalArgumentException("U03 governed gateway dependencies are required");
        }
        this.invocationGuard = invocationGuard;
        this.releaseRegistry = releaseRegistry;
        this.exactReleaseResolver = exactReleaseResolver;
        this.provider = provider;
        this.enabled = enabled;
    }

    /**
     * Authorized CD-07 path: explicit non-production execution context, accepted
     * evidence, and the Gate-C-frozen release set. No active/latest release
     * discovery occurs here.
     */
    public GovernedResult assess(U03NonProductionExecutionContext context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        if (!enabled) throw new IllegalStateException("U03 capability binding is not active");
        if (!(provider instanceof U03AcceptedEvidenceAwareCandidateProvider)) {
            throw new IllegalStateException(
                    "CD-07 explicit runtime requires an accepted-evidence-aware C02 provider");
        }

        U03AcceptedEvidenceBinding acceptedEvidence = context.requireAcceptedEvidenceBinding();
        CapabilityBindingRecord capabilityBinding = authorizeCapability();
        U03ResolvedNonProductionReleaseSet resolved =
                exactReleaseResolver.resolve(context, capabilityBinding);
        U03ReleaseBinding candidateReleaseBinding = resolved.asCandidateReleaseBinding();

        U03RiskAssessmentCandidate candidate =
                ((U03AcceptedEvidenceAwareCandidateProvider) provider).assess(
                        context.getCommand(), capabilityBinding, candidateReleaseBinding, acceptedEvidence);
        if (candidate == null) throw new IllegalStateException("U03 candidate provider returned null");
        verifyAcceptedEvidencePreserved(candidate, acceptedEvidence);
        return new GovernedResult(candidate, capabilityBinding, candidateReleaseBinding, resolved);
    }

    /**
     * Historical component-level path retained for compatibility with the existing
     * U03 engineering slice. CD-07 non-production runtime must use assess(context).
     */
    public GovernedResult assess(U03ExecutionCommand command) {
        if (!enabled) throw new IllegalStateException("U03 capability binding is not active");
        CapabilityBindingRecord capabilityBinding = authorizeCapability();
        U03ReleaseBinding releaseBinding = releaseRegistry.requireActive(capabilityBinding.getBindingId());
        if (!releaseBinding.isCompatibleWith(capabilityBinding)) {
            throw new IllegalStateException("U03 release scope is not compatible with authorized capability binding");
        }
        U03RiskAssessmentCandidate candidate = provider.assess(command, capabilityBinding, releaseBinding);
        if (candidate == null) throw new IllegalStateException("U03 candidate provider returned null");
        return new GovernedResult(candidate, capabilityBinding, releaseBinding, null);
    }

    private CapabilityBindingRecord authorizeCapability() {
        return invocationGuard.authorize(
                BINDING_ID,
                CAPABILITY_ID,
                new CapabilityExecutionContext(
                        SCOPE_VERSION,
                        CONTRACT_VERSION,
                        CapabilityBindingRecord.ANY,
                        CapabilityBindingRecord.ANY,
                        CapabilityBindingRecord.ANY,
                        CapabilityBindingRecord.ANY));
    }

    private static void verifyAcceptedEvidencePreserved(
            U03RiskAssessmentCandidate candidate,
            U03AcceptedEvidenceBinding acceptedEvidence) {
        if (candidate.isFailed()) return;
        if (!acceptedEvidence.getEvidenceRefs().equals(candidate.getEvidenceRefs())) {
            throw new IllegalStateException("C02 candidate did not preserve the accepted evidence binding");
        }
        if (!candidate.getSourceRefs().containsAll(acceptedEvidence.getSourceRefs())) {
            throw new IllegalStateException("C02 candidate did not preserve accepted evidence source refs");
        }
        if (!candidate.getProvenance().containsAll(acceptedEvidence.getProvenanceRefs())) {
            throw new IllegalStateException("C02 candidate did not preserve accepted evidence provenance");
        }
    }

    public static final class GovernedResult {
        private final U03RiskAssessmentCandidate candidate;
        private final CapabilityBindingRecord capabilityBinding;
        private final U03ReleaseBinding releaseBinding;
        private final U03ResolvedNonProductionReleaseSet resolvedNonProductionReleaseSet;

        GovernedResult(U03RiskAssessmentCandidate candidate, CapabilityBindingRecord capabilityBinding,
                U03ReleaseBinding releaseBinding) {
            this(candidate, capabilityBinding, releaseBinding, null);
        }

        GovernedResult(
                U03RiskAssessmentCandidate candidate,
                CapabilityBindingRecord capabilityBinding,
                U03ReleaseBinding releaseBinding,
                U03ResolvedNonProductionReleaseSet resolvedNonProductionReleaseSet) {
            this.candidate = candidate;
            this.capabilityBinding = capabilityBinding;
            this.releaseBinding = releaseBinding;
            this.resolvedNonProductionReleaseSet = resolvedNonProductionReleaseSet;
        }

        public U03RiskAssessmentCandidate getCandidate() { return candidate; }
        public CapabilityBindingRecord getCapabilityBinding() { return capabilityBinding; }
        public U03ReleaseBinding getReleaseBinding() { return releaseBinding; }
        public U03ResolvedNonProductionReleaseSet getResolvedNonProductionReleaseSet() {
            return resolvedNonProductionReleaseSet;
        }
    }
}
