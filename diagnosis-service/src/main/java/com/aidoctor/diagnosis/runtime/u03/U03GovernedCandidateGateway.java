package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityExecutionContext;
import com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard;

/**
 * U03 governed invocation boundary. Authorization and release binding must both succeed
 * before the candidate provider is called.
 */
public final class U03GovernedCandidateGateway {
    public static final String BINDING_ID = "c02-u03-v1-active";
    public static final String CAPABILITY_ID = "C02";
    public static final String SCOPE_VERSION = "aidoctor-v1-scope";
    public static final String CONTRACT_VERSION = "contracts-v1";

    private final CapabilityInvocationGuard invocationGuard;
    private final U03ReleaseRegistry releaseRegistry;
    private final U03CandidateProvider provider;
    private final boolean enabled;

    public U03GovernedCandidateGateway(CapabilityInvocationGuard invocationGuard,
            U03ReleaseRegistry releaseRegistry, U03CandidateProvider provider, boolean enabled) {
        if (invocationGuard == null || releaseRegistry == null || provider == null) {
            throw new IllegalArgumentException("U03 governed gateway dependencies are required");
        }
        this.invocationGuard = invocationGuard;
        this.releaseRegistry = releaseRegistry;
        this.provider = provider;
        this.enabled = enabled;
    }

    public GovernedResult assess(U03ExecutionCommand command) {
        if (!enabled) throw new IllegalStateException("U03 capability binding is not active");
        CapabilityBindingRecord capabilityBinding = invocationGuard.authorize(
                BINDING_ID,
                CAPABILITY_ID,
                new CapabilityExecutionContext(
                        SCOPE_VERSION,
                        CONTRACT_VERSION,
                        CapabilityBindingRecord.ANY,
                        CapabilityBindingRecord.ANY,
                        CapabilityBindingRecord.ANY,
                        CapabilityBindingRecord.ANY));
        U03ReleaseBinding releaseBinding = releaseRegistry.requireActive(capabilityBinding.getBindingId());
        if (!releaseBinding.isCompatibleWith(capabilityBinding)) {
            throw new IllegalStateException("U03 release scope is not compatible with authorized capability binding");
        }
        U03RiskAssessmentCandidate candidate = provider.assess(command, capabilityBinding, releaseBinding);
        if (candidate == null) throw new IllegalStateException("U03 candidate provider returned null");
        return new GovernedResult(candidate, capabilityBinding, releaseBinding);
    }

    public static final class GovernedResult {
        private final U03RiskAssessmentCandidate candidate;
        private final CapabilityBindingRecord capabilityBinding;
        private final U03ReleaseBinding releaseBinding;

        GovernedResult(U03RiskAssessmentCandidate candidate, CapabilityBindingRecord capabilityBinding,
                U03ReleaseBinding releaseBinding) {
            this.candidate = candidate;
            this.capabilityBinding = capabilityBinding;
            this.releaseBinding = releaseBinding;
        }

        public U03RiskAssessmentCandidate getCandidate() { return candidate; }
        public CapabilityBindingRecord getCapabilityBinding() { return capabilityBinding; }
        public U03ReleaseBinding getReleaseBinding() { return releaseBinding; }
    }
}
