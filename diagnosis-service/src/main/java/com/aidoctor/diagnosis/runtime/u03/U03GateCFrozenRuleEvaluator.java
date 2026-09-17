package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Concrete non-production C02 implementation mechanically ported from
 * tools/u03_gatec_eval/evaluator.py at the Gate-C frozen release tuple.
 *
 * <p>No new clinical thresholds or dispositions are introduced here. Any semantic
 * change requires a new governed release and evaluation cycle.</p>
 */
public final class U03GateCFrozenRuleEvaluator implements U03AcceptedEvidenceAwareCandidateProvider {
    public static final String RULE_SIGNAL_CRITICAL_RED_FLAG = "RULE_SIGNAL_CRITICAL_RED_FLAG";
    public static final String RULE_SIGNAL_MUST_NOT_MISS = "RULE_SIGNAL_MUST_NOT_MISS";
    public static final String RULE_SIGNAL_SEPSIS_HIGH = "RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION";
    public static final String RULE_SIGNAL_SEPSIS_MODHIGH = "RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION";
    public static final String RULE_SIGNAL_INPUT_INSUFFICIENT = "RULE_SIGNAL_INPUT_INSUFFICIENT";
    public static final String RULE_SIGNAL_SCOPE_MISMATCH = "RULE_SIGNAL_SCOPE_MISMATCH";

    public static final List<String> BASELINE_RULES = Collections.unmodifiableList(Arrays.asList(
            "C-RULE-RESP-001",
            "C-RULE-NEURO-001",
            "C-RULE-NEURO-002",
            "C-RULE-CARD-001",
            "C-RULE-ALLERGY-001"));

    public static final List<String> DYSPNOEA_RULES = Collections.unmodifiableList(Arrays.asList(
            "C-RULE-DYSPNOEA-APPEAR-001",
            "C-RULE-DYSPNOEA-CONFUSION-001"));

    public static final List<String> SEPSIS_RULES = Collections.unmodifiableList(Arrays.asList(
            "C-RULE-SEPSIS-RR-HIGH-001",
            "C-RULE-SEPSIS-RR-MODHIGH-001",
            "C-RULE-SEPSIS-SBP-HIGH-001",
            "C-RULE-SEPSIS-SBP-MODHIGH-001",
            "C-RULE-SEPSIS-HR-HIGH-001",
            "C-RULE-SEPSIS-HR-MODHIGH-001",
            "C-RULE-SEPSIS-APPEAR-HIGH-001",
            "C-RULE-SEPSIS-RASH-HIGH-001"));

    private static final Set<String> MISSING_STATES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "UNKNOWN", "UNMEASURED", "NOT_ASKED", "AMBIGUOUS", "CONFLICTING",
            "REMOTE_NOT_OBSERVED", "NOT_ESTABLISHED", "INVALID")));

    private final U03GateCClinicalInputPort clinicalInputPort;

    public U03GateCFrozenRuleEvaluator(U03GateCClinicalInputPort clinicalInputPort) {
        if (clinicalInputPort == null) throw new IllegalArgumentException("clinicalInputPort is required");
        this.clinicalInputPort = clinicalInputPort;
    }

    @Override
    public U03RiskAssessmentCandidate assess(
            U03ExecutionCommand command,
            CapabilityBindingRecord capabilityBinding,
            U03ReleaseBinding releaseBinding,
            U03AcceptedEvidenceBinding acceptedEvidenceBinding) {
        if (command == null || capabilityBinding == null || releaseBinding == null || acceptedEvidenceBinding == null) {
            throw new IllegalArgumentException("Gate-C C02 inputs are required");
        }
        if (!U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF.equals(releaseBinding.getRuleReleaseId())
                || !U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF.equals(releaseBinding.getKnowledgeReleaseId())) {
            throw new IllegalStateException("Gate-C C02 requires the exact frozen rule and knowledge releases");
        }

        U03GateCClinicalInput input = clinicalInputPort.load(command, acceptedEvidenceBinding);
        if (input == null) throw new IllegalStateException("Gate-C clinical input port returned null");

        String preDecisionFailure = preDecisionFailure(input);
        List<U03GateCRuleResult> results = preDecisionFailure == null
                ? evaluateRules(input)
                : Collections.<U03GateCRuleResult>emptyList();
        requireAcceptedEvidenceCoverage(results, acceptedEvidenceBinding);

        U03GateCRuleEvaluation evaluation = new U03GateCRuleEvaluation(results, preDecisionFailure);
        return U03RiskAssessmentCandidate.validWithGateCEvaluation(
                command.clinicalStateVersion,
                acceptedEvidenceBinding.getEvidenceRefs(),
                acceptedEvidenceBinding.getSourceRefs(),
                acceptedEvidenceBinding.getProvenanceRefs(),
                capabilityBinding.getBindingId(),
                capabilityBinding.getCapabilityVersion(),
                releaseBinding.getRuleReleaseId(),
                releaseBinding.getKnowledgeReleaseId(),
                evaluation);
    }

    private static String preDecisionFailure(U03GateCClinicalInput input) {
        if (input.isInvalidInput()) return "INVALID_INPUT";
        if (input.isDependencyFailure()) return "DEPENDENCY_FAILURE";
        String pregnancy = input.getPregnancyOrPuerperium();
        if ("TRUE".equals(pregnancy)) return "OVERALL_POLICY_SCOPE_MISMATCH";
        if ("UNKNOWN".equals(pregnancy)
                || "NOT_ASKED".equals(pregnancy)
                || "NOT_ESTABLISHED".equals(pregnancy)) {
            return "OVERALL_POLICY_SCOPE_NOT_ESTABLISHED";
        }
        if (input.isPediatrics() || !input.isRegionScopeOk() || !input.isChannelScopeOk()) {
            return "OVERALL_POLICY_SCOPE_MISMATCH";
        }
        return null;
    }

    private static List<U03GateCRuleResult> evaluateRules(U03GateCClinicalInput input) {
        List<U03GateCRuleResult> results = new ArrayList<U03GateCRuleResult>();
        results.add(evidenceRule("C-RULE-RESP-001", "EV-RF-RESP-001", RULE_SIGNAL_CRITICAL_RED_FLAG, input));
        results.add(evidenceRule("C-RULE-NEURO-001", "EV-MNM-NEURO-001", RULE_SIGNAL_MUST_NOT_MISS, input));
        results.add(evidenceRule("C-RULE-NEURO-002", "EV-MNM-NEURO-002", RULE_SIGNAL_MUST_NOT_MISS, input));
        results.add(evidenceRule("C-RULE-CARD-001", "EV-MNM-CARD-001", RULE_SIGNAL_MUST_NOT_MISS, input));
        results.add(evidenceRule("C-RULE-ALLERGY-001", "EV-RF-ALLERGY-001", RULE_SIGNAL_CRITICAL_RED_FLAG, input));

        String dyspContext = input.getDyspnoeaContext();
        results.add(dyspnoeaRule("C-RULE-DYSPNOEA-APPEAR-001", "EV-RF-APPEAR-001", dyspContext, input));
        results.add(dyspnoeaRule("C-RULE-DYSPNOEA-CONFUSION-001", "EV-RF-NEURO-001", dyspContext, input));

        results.addAll(evaluateSepsisRules(input));
        return results;
    }

    private static U03GateCRuleResult evidenceRule(
            String ruleId,
            String evidenceRef,
            String signal,
            U03GateCClinicalInput input) {
        String scope = input.ruleScope(ruleId);
        if (MISSING_STATES.contains(scope)) {
            return result(ruleId, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, evidenceRef);
        }
        if ("FALSE".equals(scope)) {
            return result(ruleId, "SCOPE_MISMATCH", RULE_SIGNAL_SCOPE_MISMATCH, evidenceRef);
        }
        String state = input.evidenceState(evidenceRef);
        if ("PRESENT".equals(state)) return result(ruleId, "MATCHED", signal, evidenceRef);
        if ("ABSENT".equals(state)) return result(ruleId, "NO_MATCH", null, evidenceRef);
        return result(ruleId, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, evidenceRef);
    }

    private static U03GateCRuleResult dyspnoeaRule(
            String ruleId,
            String evidenceRef,
            String dyspContext,
            U03GateCClinicalInput input) {
        if (MISSING_STATES.contains(dyspContext)) {
            return result(ruleId, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, evidenceRef);
        }
        if ("FALSE".equals(dyspContext)) {
            return result(ruleId, "SCOPE_MISMATCH", RULE_SIGNAL_SCOPE_MISMATCH, evidenceRef);
        }
        String state = input.evidenceState(evidenceRef);
        if ("PRESENT".equals(state)) {
            return result(ruleId, "MATCHED", RULE_SIGNAL_CRITICAL_RED_FLAG, evidenceRef);
        }
        if ("ABSENT".equals(state)) return result(ruleId, "NO_MATCH", null, evidenceRef);
        return result(ruleId, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, evidenceRef);
    }

    private static List<U03GateCRuleResult> evaluateSepsisRules(U03GateCClinicalInput input) {
        List<U03GateCRuleResult> out = new ArrayList<U03GateCRuleResult>();

        out.add(rateRule("C-RULE-SEPSIS-RR-HIGH-001", "HIGH", input));
        out.add(rateRule("C-RULE-SEPSIS-RR-MODHIGH-001", "MODHIGH", input));
        out.add(systolicHighRule(input));
        out.add(systolicModerateHighRule(input));
        out.add(heartRateRule("C-RULE-SEPSIS-HR-HIGH-001", "HIGH", input));
        out.add(heartRateRule("C-RULE-SEPSIS-HR-MODHIGH-001", "MODHIGH", input));
        out.add(sepsisEvidenceRule("C-RULE-SEPSIS-APPEAR-HIGH-001", "EV-RF-APPEAR-001", input));
        out.add(sepsisEvidenceRule("C-RULE-SEPSIS-RASH-HIGH-001", "EV-RF-SEPSIS-001", input));
        return out;
    }

    private static U03GateCRuleResult rateRule(String ruleId, String mode, U03GateCClinicalInput input) {
        String evidenceRef = "EV-VS-SEPSIS-001";
        U03GateCRuleResult scoped = sepsisScopeResult(ruleId, evidenceRef, input);
        if (scoped != null) return scoped;
        U03GateCClinicalInput.Measurement m = input.measurement("respiratory_rate_bpm");
        if (!"PRESENT".equals(m.getState()) || m.getValue() == null) {
            return result(ruleId, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, evidenceRef);
        }
        double rr = m.getValue().doubleValue();
        if ("HIGH".equals(mode) && rr >= 25.0d) {
            return result(ruleId, "MATCHED", RULE_SIGNAL_SEPSIS_HIGH, evidenceRef);
        }
        if ("MODHIGH".equals(mode) && rr >= 21.0d && rr <= 24.0d) {
            return result(ruleId, "MATCHED", RULE_SIGNAL_SEPSIS_MODHIGH, evidenceRef);
        }
        return result(ruleId, "NO_MATCH", null, evidenceRef);
    }

    private static U03GateCRuleResult systolicHighRule(U03GateCClinicalInput input) {
        String ruleId = "C-RULE-SEPSIS-SBP-HIGH-001";
        String evidenceRef = "EV-VS-SEPSIS-002";
        U03GateCRuleResult scoped = sepsisScopeResult(ruleId, evidenceRef, input);
        if (scoped != null) return scoped;
        U03GateCClinicalInput.Measurement sbp = input.measurement("systolic_bp_mmHg");
        if (!"PRESENT".equals(sbp.getState()) || sbp.getValue() == null) {
            return result(ruleId, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, evidenceRef);
        }
        if (sbp.getValue().doubleValue() <= 90.0d) {
            return result(ruleId, "MATCHED", RULE_SIGNAL_SEPSIS_HIGH, evidenceRef);
        }
        U03GateCClinicalInput.Measurement usual = input.measurement("usual_systolic_bp_mmHg");
        if (!"PRESENT".equals(usual.getState()) || usual.getValue() == null) {
            return result(ruleId, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, evidenceRef);
        }
        if (usual.getValue().doubleValue() - sbp.getValue().doubleValue() > 40.0d) {
            return result(ruleId, "MATCHED", RULE_SIGNAL_SEPSIS_HIGH, evidenceRef);
        }
        return result(ruleId, "NO_MATCH", null, evidenceRef);
    }

    private static U03GateCRuleResult systolicModerateHighRule(U03GateCClinicalInput input) {
        String ruleId = "C-RULE-SEPSIS-SBP-MODHIGH-001";
        String evidenceRef = "EV-VS-SEPSIS-002";
        U03GateCRuleResult scoped = sepsisScopeResult(ruleId, evidenceRef, input);
        if (scoped != null) return scoped;
        U03GateCClinicalInput.Measurement sbp = input.measurement("systolic_bp_mmHg");
        if (!"PRESENT".equals(sbp.getState()) || sbp.getValue() == null) {
            return result(ruleId, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, evidenceRef);
        }
        double value = sbp.getValue().doubleValue();
        if (value >= 91.0d && value <= 100.0d) {
            return result(ruleId, "MATCHED", RULE_SIGNAL_SEPSIS_MODHIGH, evidenceRef);
        }
        return result(ruleId, "NO_MATCH", null, evidenceRef);
    }

    private static U03GateCRuleResult heartRateRule(String ruleId, String mode, U03GateCClinicalInput input) {
        String evidenceRef = "EV-VS-SEPSIS-003";
        U03GateCRuleResult scoped = sepsisScopeResult(ruleId, evidenceRef, input);
        if (scoped != null) return scoped;
        U03GateCClinicalInput.Measurement hr = input.measurement("heart_rate_bpm");
        if (!"PRESENT".equals(hr.getState()) || hr.getValue() == null) {
            return result(ruleId, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, evidenceRef);
        }
        double value = hr.getValue().doubleValue();
        if ("HIGH".equals(mode) && value > 130.0d) {
            return result(ruleId, "MATCHED", RULE_SIGNAL_SEPSIS_HIGH, evidenceRef);
        }
        if ("MODHIGH".equals(mode) && value >= 91.0d && value <= 130.0d) {
            return result(ruleId, "MATCHED", RULE_SIGNAL_SEPSIS_MODHIGH, evidenceRef);
        }
        return result(ruleId, "NO_MATCH", null, evidenceRef);
    }

    private static U03GateCRuleResult sepsisEvidenceRule(
            String ruleId,
            String evidenceRef,
            U03GateCClinicalInput input) {
        U03GateCRuleResult scoped = sepsisScopeResult(ruleId, evidenceRef, input);
        if (scoped != null) return scoped;
        String state = input.evidenceState(evidenceRef);
        if ("PRESENT".equals(state)) return result(ruleId, "MATCHED", RULE_SIGNAL_SEPSIS_HIGH, evidenceRef);
        if ("ABSENT".equals(state)) return result(ruleId, "NO_MATCH", null, evidenceRef);
        return result(ruleId, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, evidenceRef);
    }

    private static U03GateCRuleResult sepsisScopeResult(
            String ruleId,
            String evidenceRef,
            U03GateCClinicalInput input) {
        String scope = sharedSepsisScope(input);
        if ("INSUFFICIENT".equals(scope)) {
            return result(ruleId, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, evidenceRef);
        }
        if ("MISMATCH".equals(scope)) {
            return result(ruleId, "SCOPE_MISMATCH", RULE_SIGNAL_SCOPE_MISMATCH, evidenceRef);
        }
        return null;
    }

    private static String sharedSepsisScope(U03GateCClinicalInput input) {
        String ageState = input.getAgeState();
        String pregnancy = input.getPregnancyOrPuerperium();
        String suspected = input.getSuspectedSepsis();
        String setting = input.getSetting();
        if (MISSING_STATES.contains(ageState)
                || MISSING_STATES.contains(pregnancy)
                || MISSING_STATES.contains(suspected)
                || MISSING_STATES.contains(setting)) {
            return "INSUFFICIENT";
        }
        Integer age = input.getAgeYears();
        if (!"PRESENT".equals(ageState) || age == null) return "INSUFFICIENT";
        if (age.intValue() < 16
                || "TRUE".equals(pregnancy)
                || "FALSE".equals(suspected)
                || !("SOURCE_SUPPORTED_COMMUNITY".equals(setting)
                    || "SOURCE_SUPPORTED_CUSTODIAL".equals(setting))) {
            return "MISMATCH";
        }
        if (!"TRUE".equals(suspected) || !"FALSE".equals(pregnancy)) return "INSUFFICIENT";
        return "APPLICABLE";
    }

    private static U03GateCRuleResult result(
            String ruleId,
            String executionState,
            String signal,
            String evidenceRef) {
        return new U03GateCRuleResult(
                ruleId,
                executionState,
                signal,
                Collections.singletonList(evidenceRef));
    }

    private static void requireAcceptedEvidenceCoverage(
            List<U03GateCRuleResult> results,
            U03AcceptedEvidenceBinding acceptedEvidenceBinding) {
        Set<String> accepted = new HashSet<String>(acceptedEvidenceBinding.getEvidenceRefs());
        for (U03GateCRuleResult result : results) {
            for (String evidenceRef : result.getEvidenceRefs()) {
                if (!accepted.contains(evidenceRef)) {
                    throw new IllegalStateException(
                            "Gate-C rule execution referenced evidence outside the accepted binding: " + evidenceRef);
                }
            }
        }
    }
}
