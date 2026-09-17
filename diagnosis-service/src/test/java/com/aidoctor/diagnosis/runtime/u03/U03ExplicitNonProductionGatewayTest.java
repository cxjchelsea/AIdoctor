package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityExecutionContext;
import com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class U03ExplicitNonProductionGatewayTest {
    @Test
    void explicitContextInvokesC02WithAcceptedEvidenceWithoutActiveReleaseDiscovery() {
        CapabilityBindingRecord binding = capabilityBinding();
        CapabilityInvocationGuard guard = fixedGuard(binding);
        U03ReleaseRegistry emptyHistoricalRegistry = new U03ReleaseRegistry();

        U03AcceptedEvidenceAwareCandidateProvider provider =
                (command, capabilityBinding, releaseBinding, acceptedEvidence) -> {
                    assertEquals(7, command.clinicalStateVersion);
                    assertEquals(U03GovernedCandidateGateway.BINDING_ID, capabilityBinding.getBindingId());
                    assertEquals("acceptance-1", acceptedEvidence.getAcceptanceRef());
                    assertEquals(Collections.singletonList("accepted-evidence-1"), acceptedEvidence.getEvidenceRefs());
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
                            acceptedEvidence.getEvidenceRefs(),
                            0.8d,
                            "SYNTHETIC_UNCERTAINTY",
                            Collections.<String>emptyList(),
                            acceptedEvidence.getSourceRefs(),
                            Arrays.asList("evidence-provenance-1", "runtime-c02"),
                            capabilityBinding.getBindingId(),
                            capabilityBinding.getCapabilityVersion(),
                            releaseBinding.getRuleReleaseId(),
                            releaseBinding.getKnowledgeReleaseId());
                };

        U03GovernedCandidateGateway gateway = new U03GovernedCandidateGateway(
                guard, emptyHistoricalRegistry, provider, true);

        U03GovernedCandidateGateway.GovernedResult result = gateway.assess(contextWithEvidence());

        assertNotNull(result.getResolvedNonProductionReleaseSet());
        assertEquals(
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF,
                result.getResolvedNonProductionReleaseSet().getRefs().getPolicyPairRef());
        assertEquals(
                U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                result.getCandidate().getRuleReleaseId());
        assertEquals(Collections.singletonList("accepted-evidence-1"), result.getCandidate().getEvidenceRefs());
    }

    @Test
    void explicitContextRejectsMissingAcceptedEvidenceBeforeC02() {
        U03AcceptedEvidenceAwareCandidateProvider provider =
                (command, capabilityBinding, releaseBinding, acceptedEvidence) -> {
                    throw new AssertionError("provider must not run without accepted evidence");
                };
        U03GovernedCandidateGateway gateway = new U03GovernedCandidateGateway(
                fixedGuard(capabilityBinding()), new U03ReleaseRegistry(), provider, true);

        assertThrows(IllegalStateException.class, () -> gateway.assess(contextWithoutEvidence()));
    }

    @Test
    void contextRejectsAcceptedEvidenceFromDifferentClinicalStateVersion() {
        assertThrows(IllegalStateException.class, () ->
                new U03NonProductionExecutionContext(
                        command(),
                        U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                        new U03AcceptedEvidenceBinding(
                                "acceptance-stale",
                                6,
                                Collections.singletonList("accepted-evidence-old"),
                                Collections.singletonList("source-old"),
                                Collections.singletonList("provenance-old")),
                        "ci-nonprod"));
    }

    @Test
    void explicitContextRejectsProviderThatCannotConsumeAcceptedEvidence() {
        U03CandidateProvider legacyProvider = (command, capabilityBinding, releaseBinding) ->
                U03RiskAssessmentCandidate.failed("SHOULD_NOT_RUN");
        U03GovernedCandidateGateway gateway = new U03GovernedCandidateGateway(
                fixedGuard(capabilityBinding()), new U03ReleaseRegistry(), legacyProvider, true);

        assertThrows(IllegalStateException.class, () -> gateway.assess(contextWithEvidence()));
    }

    @Test
    void explicitContextRejectsCandidateThatDropsAcceptedEvidenceProvenance() {
        U03AcceptedEvidenceAwareCandidateProvider provider =
                (command, capabilityBinding, releaseBinding, acceptedEvidence) ->
                        U03RiskAssessmentCandidate.valid(
                                command.clinicalStateVersion,
                                acceptedEvidence.getEvidenceRefs(),
                                0.8d,
                                "SYNTHETIC_UNCERTAINTY",
                                Collections.<String>emptyList(),
                                acceptedEvidence.getSourceRefs(),
                                Collections.singletonList("different-provenance"),
                                capabilityBinding.getBindingId(),
                                capabilityBinding.getCapabilityVersion(),
                                releaseBinding.getRuleReleaseId(),
                                releaseBinding.getKnowledgeReleaseId());
        U03GovernedCandidateGateway gateway = new U03GovernedCandidateGateway(
                fixedGuard(capabilityBinding()), new U03ReleaseRegistry(), provider, true);

        assertThrows(IllegalStateException.class, () -> gateway.assess(contextWithEvidence()));
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
        U03AcceptedEvidenceAwareCandidateProvider provider =
                (command, capabilityBinding, releaseBinding, acceptedEvidence) -> {
                    throw new AssertionError("provider must not run when capability authorization fails");
                };

        U03GovernedCandidateGateway gateway = new U03GovernedCandidateGateway(
                denied, new U03ReleaseRegistry(), provider, true);

        assertThrows(IllegalStateException.class, () -> gateway.assess(contextWithEvidence()));
    }

    @Test
    void disabledGatewayFailsBeforeC02Invocation() {
        U03AcceptedEvidenceAwareCandidateProvider provider =
                (command, capabilityBinding, releaseBinding, acceptedEvidence) -> {
                    throw new AssertionError("provider must not run when gateway is disabled");
                };
        U03GovernedCandidateGateway gateway = new U03GovernedCandidateGateway(
                fixedGuard(capabilityBinding()), new U03ReleaseRegistry(), provider, false);

        assertThrows(IllegalStateException.class, () -> gateway.assess(contextWithEvidence()));
    }

    private static U03NonProductionExecutionContext contextWithEvidence() {
        return new U03NonProductionExecutionContext(
                command(),
                U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                acceptedEvidence(),
                "ci-nonprod");
    }

    private static U03NonProductionExecutionContext contextWithoutEvidence() {
        return new U03NonProductionExecutionContext(
                command(),
                U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                "ci-nonprod");
    }

    private static U03AcceptedEvidenceBinding acceptedEvidence() {
        return new U03AcceptedEvidenceBinding(
                "acceptance-1",
                7,
                Collections.singletonList("accepted-evidence-1"),
                Collections.singletonList("source-1"),
                Collections.singletonList("evidence-provenance-1"));
    }

    private static U03ExecutionCommand command() {
        return new U03ExecutionCommand(
                "consult-1", "thread-1", "run-1", "event-1",
                "cdp-1", 7, "corr-1", "trace-1");
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
