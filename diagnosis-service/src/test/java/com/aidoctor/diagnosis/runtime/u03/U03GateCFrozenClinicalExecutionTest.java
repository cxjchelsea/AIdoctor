package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityExecutionContext;
import com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class U03GateCFrozenClinicalExecutionTest {
    @Test
    void frozenHighRiskRuleProducesHighRisk() {
        U03GateCClinicalInput input = U03GateCClinicalInput.builder()
                .evidence("EV-RF-RESP-001", "PRESENT")
                .build();
        U03DecisionOutcome decision = execute(input);
        assertEquals(U03RiskAssessmentCandidate.VALID, decision.getStatus());
        assertEquals("HIGH_RISK", decision.getOutcomeCode());
        assertEquals("HIGH_RISK_RULE_SIGNAL_PRESENT", decision.getReasonCode());
    }

    @Test
    void frozenModerateHighSepsisRuleProducesCaution() {
        U03GateCClinicalInput input = U03GateCClinicalInput.builder()
                .suspectedSepsis("TRUE")
                .measurement("respiratory_rate_bpm", U03GateCClinicalInput.Measurement.present(21))
                .measurement("systolic_bp_mmHg", U03GateCClinicalInput.Measurement.present(120))
                .measurement("usual_systolic_bp_mmHg", U03GateCClinicalInput.Measurement.present(120))
                .measurement("heart_rate_bpm", U03GateCClinicalInput.Measurement.present(80))
                .build();
        U03DecisionOutcome decision = execute(input);
        assertEquals(U03RiskAssessmentCandidate.VALID, decision.getStatus());
        assertEquals("CAUTION", decision.getOutcomeCode());
        assertEquals("MODERATE_HIGH_RULE_SIGNAL_PRESENT", decision.getReasonCode());
    }

    @Test
    void completeNoMatchCoverageProducesNoHighRiskSignal() {
        U03DecisionOutcome decision = execute(U03GateCClinicalInput.builder().build());
        assertEquals(U03RiskAssessmentCandidate.VALID, decision.getStatus());
        assertEquals("NO_HIGH_RISK_SIGNAL", decision.getOutcomeCode());
        assertEquals("COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL", decision.getReasonCode());
    }

    @Test
    void insufficientEvidenceFailsClosed() {
        U03GateCClinicalInput input = U03GateCClinicalInput.builder()
                .evidence("EV-RF-RESP-001", "UNKNOWN")
                .build();
        U03DecisionOutcome decision = execute(input);
        assertEquals(U03RiskAssessmentCandidate.FAILED, decision.getStatus());
        assertEquals(null, decision.getOutcomeCode());
        assertEquals("INSUFFICIENT_INFORMATION", decision.getReasonCode());
    }

    @Test
    void outOfScopePregnancyFailsBeforeRuleExecution() {
        U03GateCClinicalInput input = U03GateCClinicalInput.builder()
                .pregnancyOrPuerperium("TRUE")
                .build();
        Execution execution = executeWithCandidate(input);
        assertNotNull(execution.governed.getCandidate().getGateCEvaluation());
        assertEquals(0, execution.governed.getCandidate().getGateCEvaluation().getRuleResults().size());
        assertEquals(U03RiskAssessmentCandidate.FAILED, execution.decision.getStatus());
        assertEquals("OVERALL_POLICY_SCOPE_MISMATCH", execution.decision.getReasonCode());
    }

    private static U03DecisionOutcome execute(U03GateCClinicalInput input) {
        return executeWithCandidate(input).decision;
    }

    private static Execution executeWithCandidate(U03GateCClinicalInput input) {
        U03NonProductionExecutionContext context = context();
        CapabilityBindingRecord capability = capabilityBinding();
        CapabilityInvocationGuard guard = new CapabilityInvocationGuard(null) {
            @Override
            public CapabilityBindingRecord authorize(
                    String bindingId,
                    String expectedCapabilityId,
                    CapabilityExecutionContext executionContext) {
                return capability;
            }
        };
        U03GateCFrozenRuleEvaluator c02 = new U03GateCFrozenRuleEvaluator((command, evidence) -> input);
        U03GovernedCandidateGateway.GovernedResult governed = new U03GovernedCandidateGateway(
                guard,
                new U03ReleaseRegistry(),
                c02,
                true).assess(context);
        U03DecisionOutcome decision = new U03DecisionService(new U03GateCFrozenDecisionPort())
                .decide(context, governed);
        return new Execution(governed, decision);
    }

    private static U03NonProductionExecutionContext context() {
        U03ExecutionCommand command = new U03ExecutionCommand(
                "consult-gatec",
                "thread-gatec",
                "run-gatec",
                "event-gatec",
                "cdp-gatec",
                7,
                "corr-gatec",
                "trace-gatec");
        U03AcceptedEvidenceBinding evidence = new U03AcceptedEvidenceBinding(
                "acceptance-gatec",
                7,
                Arrays.asList(
                        "EV-RF-RESP-001",
                        "EV-MNM-NEURO-001",
                        "EV-MNM-NEURO-002",
                        "EV-MNM-CARD-001",
                        "EV-RF-ALLERGY-001",
                        "EV-RF-APPEAR-001",
                        "EV-RF-NEURO-001",
                        "EV-VS-SEPSIS-001",
                        "EV-VS-SEPSIS-002",
                        "EV-VS-SEPSIS-003",
                        "EV-RF-SEPSIS-001"),
                Collections.singletonList("source-gatec"),
                Collections.singletonList("provenance-gatec"));
        return new U03NonProductionExecutionContext(
                command,
                U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                evidence,
                "ci-nonprod-gatec");
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

    private static final class Execution {
        private final U03GovernedCandidateGateway.GovernedResult governed;
        private final U03DecisionOutcome decision;

        private Execution(
                U03GovernedCandidateGateway.GovernedResult governed,
                U03DecisionOutcome decision) {
            this.governed = governed;
            this.decision = decision;
        }
    }
}
