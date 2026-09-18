package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityExecutionContext;
import com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Focused evidence for BF-CD08-03 / BF-CD08-04 remediation. */
class U03NonProductionAdmissionServiceTest {

    @Test
    void staleInputReturnsTypedFailureBeforeC02OrD09() {
        AtomicInteger c02Calls = new AtomicInteger();
        AtomicInteger d09Calls = new AtomicInteger();

        U03NonProductionAdmissionResult admission = new U03NonProductionAdmissionService().admit(
                command(7),
                U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                evidence(6),
                "ci-nonprod-cd08");

        assertTrue(admission.isFailed());
        assertEquals(U03RiskAssessmentCandidate.FAILED, admission.getStatus());
        assertEquals(U03NonProductionAdmissionService.STALE_INPUT, admission.getReasonCode());
        assertNull(admission.getDispositionCode());
        assertNull(admission.getContext());
        assertEquals("PRE_C02_ADMISSION", admission.getBoundary());

        if (admission.isAccepted()) {
            U03GovernedCandidateGateway.GovernedResult governed =
                    gateway(c02Calls).assess(admission.getContext());
            new U03DecisionService(decisionPort(d09Calls)).decide(admission.getContext(), governed);
        }

        assertEquals(0, c02Calls.get());
        assertEquals(0, d09Calls.get());
    }

    @Test
    void releaseMismatchReturnsTypedFailureBeforeC02OrD09() {
        AtomicInteger c02Calls = new AtomicInteger();
        AtomicInteger d09Calls = new AtomicInteger();

        U03ExplicitNonProductionReleaseRefs wrong = new U03ExplicitNonProductionReleaseRefs(
                U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                "RR-U03-RISK-001@0.2.0-candidate",
                U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF);

        U03NonProductionAdmissionResult admission = new U03NonProductionAdmissionService().admit(
                command(7), wrong, evidence(7), "ci-nonprod-cd08");

        assertTrue(admission.isFailed());
        assertEquals(U03RiskAssessmentCandidate.FAILED, admission.getStatus());
        assertEquals(U03NonProductionAdmissionService.RELEASE_MISMATCH, admission.getReasonCode());
        assertNull(admission.getDispositionCode());
        assertNull(admission.getContext());

        if (admission.isAccepted()) {
            U03GovernedCandidateGateway.GovernedResult governed =
                    gateway(c02Calls).assess(admission.getContext());
            new U03DecisionService(decisionPort(d09Calls)).decide(admission.getContext(), governed);
        }

        assertEquals(0, c02Calls.get());
        assertEquals(0, d09Calls.get());
    }

    @Test
    void frozenP0OrderingPrefersReleaseMismatchOverStaleInput() {
        U03ExplicitNonProductionReleaseRefs wrong = new U03ExplicitNonProductionReleaseRefs(
                U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                "RR-U03-RISK-001@0.2.0-candidate",
                U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF);

        U03NonProductionAdmissionResult admission = new U03NonProductionAdmissionService().admit(
                command(7), wrong, evidence(6), "ci-nonprod-cd08");

        assertEquals(U03NonProductionAdmissionService.RELEASE_MISMATCH, admission.getReasonCode());
    }

    @Test
    void acceptedInputsProduceStrictContextAndExistingGuardsRemainDefenseInDepth() {
        U03NonProductionAdmissionResult admission = new U03NonProductionAdmissionService().admit(
                command(7),
                U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                evidence(7),
                "ci-nonprod-cd08");

        assertTrue(admission.isAccepted());
        assertFalse(admission.isFailed());
        assertNull(admission.getReasonCode());
        assertNull(admission.getDispositionCode());
        assertEquals(7, admission.getContext().getCommand().clinicalStateVersion);

        assertThrows(IllegalStateException.class, () ->
                new U03NonProductionExecutionContext(
                        command(7),
                        U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                        evidence(6),
                        "ci-nonprod-cd08"));

        U03ExplicitNonProductionReleaseRefs wrong = new U03ExplicitNonProductionReleaseRefs(
                U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                "RR-U03-RISK-001@0.2.0-candidate",
                U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF);
        assertThrows(IllegalStateException.class, () ->
                new U03NonProductionExecutionContext(command(7), wrong, evidence(7), "ci-nonprod-cd08"));
    }

    @Test
    void productionAndAliasFailuresAreNotMisclassified() {
        assertThrows(IllegalStateException.class, () ->
                new U03NonProductionAdmissionService().admit(
                        command(7),
                        U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                        evidence(6),
                        "production"));

        assertThrows(IllegalArgumentException.class, () ->
                new U03ExplicitNonProductionReleaseRefs(
                        U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                        "latest",
                        U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF));
    }

    private static U03ExecutionCommand command(int version) {
        return new U03ExecutionCommand(
                "consult-admission",
                "thread-admission",
                "run-admission",
                "event-admission",
                "cdp-admission",
                version,
                "corr-admission",
                "trace-admission");
    }

    private static U03AcceptedEvidenceBinding evidence(int version) {
        return new U03AcceptedEvidenceBinding(
                "acceptance-admission",
                version,
                Collections.singletonList("evidence-admission"),
                Collections.singletonList("source-admission"),
                Collections.singletonList("provenance-admission"));
    }

    private static U03GovernedCandidateGateway gateway(final AtomicInteger c02Calls) {
        U03AcceptedEvidenceAwareCandidateProvider provider =
                (command, capabilityBinding, releaseBinding, acceptedEvidence) -> {
                    c02Calls.incrementAndGet();
                    return U03RiskAssessmentCandidate.failed("SHOULD_NOT_RUN");
                };
        return new U03GovernedCandidateGateway(
                fixedGuard(capabilityBinding()),
                new U03ReleaseRegistry(),
                provider,
                true);
    }

    private static U03NonProductionDecisionPort decisionPort(final AtomicInteger d09Calls) {
        return new U03NonProductionDecisionPort() {
            @Override
            public U03DecisionOutcome decide(
                    U03NonProductionExecutionContext executionContext,
                    U03RiskAssessmentCandidate acceptedCandidate,
                    U03ResolvedNonProductionReleaseSet resolvedReleaseSet) {
                d09Calls.incrementAndGet();
                throw new AssertionError("D09 must not run for failed admission");
            }

            @Override
            public U03DecisionOutcome decide(
                    U03ExecutionCommand executionCommand,
                    U03RiskAssessmentCandidate candidate,
                    U03ReleaseBinding releaseBinding) {
                throw new AssertionError("historical D09 must not run");
            }
        };
    }

    private static CapabilityInvocationGuard fixedGuard(final CapabilityBindingRecord binding) {
        return new CapabilityInvocationGuard(null) {
            @Override
            public CapabilityBindingRecord authorize(
                    String bindingId,
                    String expectedCapabilityId,
                    CapabilityExecutionContext executionContext) {
                return binding;
            }
        };
    }

    private static CapabilityBindingRecord capabilityBinding() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 18, 0, 0);
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
