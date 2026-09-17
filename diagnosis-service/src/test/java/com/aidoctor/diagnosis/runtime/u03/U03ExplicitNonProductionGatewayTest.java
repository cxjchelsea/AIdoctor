package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityExecutionContext;
import com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class U03ExplicitNonProductionGatewayTest {
    @Test
    void explicitContextInvokesC02WithoutActiveReleaseDiscovery() {
        CapabilityBindingRecord binding = capabilityBinding();
        CapabilityInvocationGuard guard = fixedGuard(binding);
        U03ReleaseRegistry emptyHistoricalRegistry = new U03ReleaseRegistry();

        U03CandidateProvider provider = (command, capabilityBinding, releaseBinding) -> {
            assertEquals(7, command.clinicalStateVersion);
            assertEquals(U03GovernedCandidateGateway.BINDING_ID, capabilityBinding.getBindingId());
            assertEquals(
                    U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                    releaseBinding.getRuleReleaseId());
            assertEquals(
                    U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                    releaseBinding.getKnowledgeReleaseId());
            assertFalse(releaseBinding.isActive());
            assertEquals(3, releaseBinding.getProvenanceRefs().size());
            return U03RiskAssessmentCandidate.valid(
                    command.clinicalStateVersion,
                    Collections.singletonList("accepted-evidence-1"),
                    0.8d,
                    "SYNTHETIC_UNCERTAINTY",
                    Collections.<String>emptyList(),
                    Collections.singletonList("source-1"),
                    releaseBinding.getProvenanceRefs(),
                    capabilityBinding.getBindingId(),
                    capabilityBinding.getCapabilityVersion(),
                    releaseBinding.getRuleReleaseId(),
                    releaseBinding.getKnowledgeReleaseId());
        };

        U03GovernedCandidateGateway gateway = new U03GovernedCandidateGateway(
                guard, emptyHistoricalRegistry, provider, true);

        U03GovernedCandidateGateway.GovernedResult result = gateway.assess(context());

        assertNotNull(result.getResolvedNonProductionReleaseSet());
        assertEquals(
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF,
                result.getResolvedNonProductionReleaseSet().getRefs().getPolicyPairRef());
        assertEquals(
                U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                result.getCandidate().getRuleReleaseId());
    }

    @Test
    void explicitContextStillRequiresCapabilityAuthorization() {
        CapabilityInvocationGuard denied = new CapabilityInvocationGuard(null) {
            @Override
            public CapabilityBindingRecord authorize(
                    String bindingId,
                    String expectedCapabilityId,
                    CapabilityExecutionContext executionContext) {
                throw new IllegalStateException("capability authorization denied");
            }
        };
        U03CandidateProvider provider = (command, capabilityBinding, releaseBinding) -> {
            throw new AssertionError("provider must not run when capability authorization fails");
        };

        U03GovernedCandidateGateway gateway = new U03GovernedCandidateGateway(
                denied, new U03ReleaseRegistry(), provider, true);

        assertThrows(IllegalStateException.class, () -> gateway.assess(context()));
    }

    @Test
    void disabledGatewayFailsBeforeC02Invocation() {
        U03CandidateProvider provider = (command, capabilityBinding, releaseBinding) -> {
            throw new AssertionError("provider must not run when gateway is disabled");
        };
        U03GovernedCandidateGateway gateway = new U03GovernedCandidateGateway(
                fixedGuard(capabilityBinding()), new U03ReleaseRegistry(), provider, false);

        assertThrows(IllegalStateException.class, () -> gateway.assess(context()));
    }

    private static U03NonProductionExecutionContext context() {
        return new U03NonProductionExecutionContext(
                new U03ExecutionCommand(
                        "consult-1", "thread-1", "run-1", "event-1",
                        "cdp-1", 7, "corr-1", "trace-1"),
                U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                "ci-nonprod");
    }

    private static CapabilityInvocationGuard fixedGuard(CapabilityBindingRecord binding) {
        return new CapabilityInvocationGuard(null) {
            @Override
            public CapabilityBindingRecord authorize(
                    String bindingId,
                    String expectedCapabilityId,
                    CapabilityExecutionContext executionContext) {
                assertEquals(U03GovernedCandidateGateway.BINDING_ID, bindingId);
                assertEquals(U03GovernedCandidateGateway.CAPABILITY_ID, expectedCapabilityId);
                return binding;
            }
        };
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
