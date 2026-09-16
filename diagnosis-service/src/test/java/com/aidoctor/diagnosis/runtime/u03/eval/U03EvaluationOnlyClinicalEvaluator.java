package com.aidoctor.diagnosis.runtime.u03.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gate-C evaluation-only executable semantics.
 *
 * <p>This class intentionally lives in the test source set. It mechanically implements the frozen
 * U03 C/D/Coverage candidates for offline evaluation only. It has no production wiring, state
 * committer, network, database, release activation, U04 or U14 dependency.</p>
 */
final class U03EvaluationOnlyClinicalEvaluator {
    static final String RULE_RELEASE = "RR-U03-RISK-001@0.2.1-candidate";
    static final String KNOWLEDGE_RELEASE = "KR-U03-SOURCE-001@0.1.0-candidate";
    static final String COVERAGE_RELEASE = "U03_D09_COVERAGE_V0_2_1_CANDIDATE";
    static final String POLICY_RELEASE = "PR-U03-D09-001@0.2.1-candidate";
    static final String POLICY_PAIR = "PF-U03-C-POLICY-001";
    static final String EVALSET_RELEASE = "ER-U03-RISK-001@0.1.0-candidate";

    enum FactState {
        PRESENT, ABSENT, UNKNOWN, UNMEASURED, NOT_ASKED, AMBIGUOUS,
        CONFLICTING, REMOTE_NOT_OBSERVED, INVALID
    }

    enum TriState { TRUE, FALSE, UNKNOWN, NOT_ASKED, NOT_ESTABLISHED }
    enum RuleState { MATCHED, NO_MATCH, INPUT_INSUFFICIENT, SCOPE_MISMATCH }

    static final class Fixture {
        final Map<String, FactState> evidence = new LinkedHashMap<String, FactState>();
        TriState pregnancyOrPuerperium = TriState.FALSE;
        boolean adultPopulation = true;
        boolean regionInScope = true;
        boolean channelInScope = true;
        TriState dyspnoeaContext = TriState.FALSE;
        TriState suspectedSepsis = TriState.FALSE;
        Integer age = 40;
        String setting = "SOURCE_SUPPORTED_COMMUNITY";
        Integer respiratoryRate;
        Integer systolicBp;
        Integer usualSystolicBp;
        boolean usualSystolicBpTraceable;
        Integer heartRate;
        FactState rrState = FactState.ABSENT;
        FactState sbpState = FactState.ABSENT;
        FactState hrState = FactState.ABSENT;
        boolean staleInput;
        boolean releaseMismatch;
        boolean invalidInput;
        boolean dependencyFailure;
        boolean unresolvableConflict;

        Fixture evidence(String ref, FactState state) { evidence.put(ref, state); return this; }
        Fixture pregnancy(TriState v) { pregnancyOrPuerperium = v; return this; }
        Fixture dyspnoea(TriState v) { dyspnoeaContext = v; return this; }
        Fixture sepsis(TriState v) { suspectedSepsis = v; return this; }
        Fixture rr(Integer v) { respiratoryRate = v; rrState = v == null ? FactState.UNMEASURED : FactState.PRESENT; return this; }
        Fixture rrState(FactState v) { rrState = v; if (v != FactState.PRESENT) respiratoryRate = null; return this; }
        Fixture sbp(Integer v) { systolicBp = v; sbpState = v == null ? FactState.UNMEASURED : FactState.PRESENT; return this; }
        Fixture sbpState(FactState v) { sbpState = v; if (v != FactState.PRESENT) systolicBp = null; return this; }
        Fixture usualSbp(Integer v, boolean traceable) { usualSystolicBp = v; usualSystolicBpTraceable = traceable; return this; }
        Fixture hr(Integer v) { heartRate = v; hrState = v == null ? FactState.UNMEASURED : FactState.PRESENT; return this; }
        Fixture hrState(FactState v) { hrState = v; if (v != FactState.PRESENT) heartRate = null; return this; }
    }

    static final class RuleResult {
        final String ruleId;
        final RuleState state;
        final String signal;
        RuleResult(String ruleId, RuleState state, String signal) {
            this.ruleId = ruleId; this.state = state; this.signal = signal;
        }
    }

    static final class Outcome {
        final String status;
        final String disposition;
        final String reasonCode;
        final String policyId;
        final Map<String, RuleResult> rules;
        final List<String> matchedRuleRefs;
        final List<String> insufficientRuleRefs;
        final List<String> notApplicableFamilyRefs;
        Outcome(String status, String disposition, String reasonCode, String policyId,
                Map<String, RuleResult> rules, List<String> matched, List<String> insufficient,
                List<String> notApplicableFamilies) {
            this.status = status; this.disposition = disposition; this.reasonCode = reasonCode;
            this.policyId = policyId; this.rules = rules; this.matchedRuleRefs = matched;
            this.insufficientRuleRefs = insufficient; this.notApplicableFamilyRefs = notApplicableFamilies;
        }
    }

    static final String CRITICAL = "RULE_SIGNAL_CRITICAL_RED_FLAG";
    static final String MUST_NOT_MISS = "RULE_SIGNAL_MUST_NOT_MISS";
    static final String SEPSIS_HIGH = "RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION";
    static final String SEPSIS_MODHIGH = "RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION";
    static final String INSUFFICIENT = "RULE_SIGNAL_INPUT_INSUFFICIENT";
    static final String SCOPE_MISMATCH = "RULE_SIGNAL_SCOPE_MISMATCH";

    static final List<String> BASELINE = Collections.unmodifiableList(Arrays.asList(
            "C-RULE-RESP-001", "C-RULE-NEURO-001", "C-RULE-NEURO-002",
            "C-RULE-CARD-001", "C-RULE-ALLERGY-001"));
    static final List<String> DYSPNOEA = Collections.unmodifiableList(Arrays.asList(
            "C-RULE-DYSPNOEA-APPEAR-001", "C-RULE-DYSPNOEA-CONFUSION-001"));
    static final List<String> SEPSIS = Collections.unmodifiableList(Arrays.asList(
            "C-RULE-SEPSIS-RR-HIGH-001", "C-RULE-SEPSIS-RR-MODHIGH-001",
            "C-RULE-SEPSIS-SBP-HIGH-001", "C-RULE-SEPSIS-SBP-MODHIGH-001",
            "C-RULE-SEPSIS-HR-HIGH-001", "C-RULE-SEPSIS-HR-MODHIGH-001",
            "C-RULE-SEPSIS-APPEAR-HIGH-001", "C-RULE-SEPSIS-RASH-HIGH-001"));

    Outcome evaluate(Fixture f) {
        String p0 = p0Failure(f);
        if (p0 != null) return outcome("FAILED", null, p0, "D09-P-001",
                Collections.<String, RuleResult>emptyMap(), new ArrayList<String>(),
                new ArrayList<String>(), new ArrayList<String>());

        Map<String, RuleResult> rules = evaluateRules(f);
        List<String> matched = new ArrayList<String>();
        List<String> insufficient = new ArrayList<String>();
        boolean high = false;
        boolean caution = false;
        for (RuleResult r : rules.values()) {
            if (r.state == RuleState.MATCHED) {
                matched.add(r.ruleId);
                if (CRITICAL.equals(r.signal) || MUST_NOT_MISS.equals(r.signal) || SEPSIS_HIGH.equals(r.signal)) high = true;
                if (SEPSIS_MODHIGH.equals(r.signal)) caution = true;
            } else if (r.state == RuleState.INPUT_INSUFFICIENT) {
                insufficient.add(r.ruleId);
            }
        }
        List<String> notApplicableFamilies = new ArrayList<String>();
        boolean dyspnoeaNotApplicable = allState(rules, DYSPNOEA, RuleState.SCOPE_MISMATCH);
        boolean sepsisNotApplicable = allState(rules, SEPSIS, RuleState.SCOPE_MISMATCH);
        if (dyspnoeaNotApplicable) notApplicableFamilies.add("NHS_DYSPNOEA_FAMILY");
        if (sepsisNotApplicable) notApplicableFamilies.add("NG253_SEPSIS_FAMILY");

        if (high) return outcome("VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", rules, matched, insufficient, notApplicableFamilies);
        if (!insufficient.isEmpty()) return outcome("FAILED", null, "INSUFFICIENT_INFORMATION", "D09-P-020", rules, matched, insufficient, notApplicableFamilies);
        if (caution) return outcome("VALID", "CAUTION", "MODERATE_HIGH_RULE_SIGNAL_PRESENT", "D09-P-030", rules, matched, insufficient, notApplicableFamilies);
        if (f.unresolvableConflict) return outcome("FAILED", null, "UNRESOLVABLE_CONFLICT", "D09-P-090", rules, matched, insufficient, notApplicableFamilies);
        if (coverageCompleteNoMatch(rules, dyspnoeaNotApplicable, sepsisNotApplicable)) {
            return outcome("VALID", "NO_HIGH_RISK_SIGNAL", "COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL", "D09-P-040", rules, matched, insufficient, notApplicableFamilies);
        }
        return outcome("FAILED", null, "UNRESOLVABLE_CONFLICT", "D09-P-090", rules, matched, insufficient, notApplicableFamilies);
    }

    private String p0Failure(Fixture f) {
        if (f.staleInput) return "STALE_INPUT";
        if (f.releaseMismatch) return "RELEASE_MISMATCH";
        if (f.invalidInput) return "INVALID_INPUT";
        if (f.dependencyFailure) return "DEPENDENCY_FAILURE";
        if (!f.adultPopulation || !f.regionInScope || !f.channelInScope) return "OVERALL_POLICY_SCOPE_MISMATCH";
        if (f.pregnancyOrPuerperium == TriState.TRUE) return "OVERALL_POLICY_SCOPE_MISMATCH";
        if (f.pregnancyOrPuerperium == TriState.UNKNOWN || f.pregnancyOrPuerperium == TriState.NOT_ASKED
                || f.pregnancyOrPuerperium == TriState.NOT_ESTABLISHED) return "OVERALL_POLICY_SCOPE_NOT_ESTABLISHED";
        return null;
    }

    private Map<String, RuleResult> evaluateRules(Fixture f) {
        Map<String, RuleResult> out = new LinkedHashMap<String, RuleResult>();
        out.put("C-RULE-RESP-001", evidenceRule("C-RULE-RESP-001", f, "EV-RF-RESP-001", CRITICAL));
        out.put("C-RULE-NEURO-001", evidenceRule("C-RULE-NEURO-001", f, "EV-MNM-NEURO-001", MUST_NOT_MISS));
        out.put("C-RULE-NEURO-002", evidenceRule("C-RULE-NEURO-002", f, "EV-MNM-NEURO-002", MUST_NOT_MISS));
        out.put("C-RULE-CARD-001", evidenceRule("C-RULE-CARD-001", f, "EV-MNM-CARD-001", MUST_NOT_MISS));
        out.put("C-RULE-ALLERGY-001", evidenceRule("C-RULE-ALLERGY-001", f, "EV-RF-ALLERGY-001", CRITICAL));

        out.put("C-RULE-DYSPNOEA-APPEAR-001", scopedEvidenceRule("C-RULE-DYSPNOEA-APPEAR-001", f, f.dyspnoeaContext, "EV-RF-APPEAR-001", CRITICAL));
        out.put("C-RULE-DYSPNOEA-CONFUSION-001", scopedEvidenceRule("C-RULE-DYSPNOEA-CONFUSION-001", f, f.dyspnoeaContext, "EV-RF-NEURO-001", CRITICAL));

        RuleState sepsisScope = sepsisScope(f);
        out.put("C-RULE-SEPSIS-RR-HIGH-001", numericRule("C-RULE-SEPSIS-RR-HIGH-001", sepsisScope, f.rrState, f.respiratoryRate, 25, Integer.MAX_VALUE, SEPSIS_HIGH));
        out.put("C-RULE-SEPSIS-RR-MODHIGH-001", numericRule("C-RULE-SEPSIS-RR-MODHIGH-001", sepsisScope, f.rrState, f.respiratoryRate, 21, 24, SEPSIS_MODHIGH));
        out.put("C-RULE-SEPSIS-SBP-HIGH-001", sbpHigh(f, sepsisScope));
        out.put("C-RULE-SEPSIS-SBP-MODHIGH-001", numericRule("C-RULE-SEPSIS-SBP-MODHIGH-001", sepsisScope, f.sbpState, f.systolicBp, 91, 100, SEPSIS_MODHIGH));
        out.put("C-RULE-SEPSIS-HR-HIGH-001", numericRule("C-RULE-SEPSIS-HR-HIGH-001", sepsisScope, f.hrState, f.heartRate, 131, Integer.MAX_VALUE, SEPSIS_HIGH));
        out.put("C-RULE-SEPSIS-HR-MODHIGH-001", numericRule("C-RULE-SEPSIS-HR-MODHIGH-001", sepsisScope, f.hrState, f.heartRate, 91, 130, SEPSIS_MODHIGH));
        out.put("C-RULE-SEPSIS-APPEAR-HIGH-001", scopedEvidenceRule("C-RULE-SEPSIS-APPEAR-HIGH-001", f, fromRuleState(sepsisScope), "EV-RF-APPEAR-001", SEPSIS_HIGH));
        out.put("C-RULE-SEPSIS-RASH-HIGH-001", scopedEvidenceRule("C-RULE-SEPSIS-RASH-HIGH-001", f, fromRuleState(sepsisScope), "EV-RF-SEPSIS-001", SEPSIS_HIGH));
        return out;
    }

    private RuleResult evidenceRule(String id, Fixture f, String evidenceRef, String signal) {
        return resolveEvidence(id, f.evidence.get(evidenceRef), signal);
    }

    private RuleResult scopedEvidenceRule(String id, Fixture f, TriState scope, String evidenceRef, String signal) {
        RuleState s = scopeState(scope);
        if (s == RuleState.INPUT_INSUFFICIENT) return result(id, s, INSUFFICIENT);
        if (s == RuleState.SCOPE_MISMATCH) return result(id, s, SCOPE_MISMATCH);
        return resolveEvidence(id, f.evidence.get(evidenceRef), signal);
    }

    private RuleResult resolveEvidence(String id, FactState state, String signal) {
        if (state == null) state = FactState.ABSENT;
        if (state == FactState.PRESENT) return result(id, RuleState.MATCHED, signal);
        if (state == FactState.ABSENT) return result(id, RuleState.NO_MATCH, null);
        return result(id, RuleState.INPUT_INSUFFICIENT, INSUFFICIENT);
    }

    private RuleState sepsisScope(Fixture f) {
        if (f.age == null || f.suspectedSepsis == TriState.UNKNOWN || f.suspectedSepsis == TriState.NOT_ASKED
                || f.suspectedSepsis == TriState.NOT_ESTABLISHED || f.setting == null) return RuleState.INPUT_INSUFFICIENT;
        if (f.age.intValue() < 16 || f.suspectedSepsis == TriState.FALSE
                || !("SOURCE_SUPPORTED_COMMUNITY".equals(f.setting) || "SOURCE_SUPPORTED_CUSTODIAL".equals(f.setting))) return RuleState.SCOPE_MISMATCH;
        return RuleState.NO_MATCH;
    }

    private RuleResult numericRule(String id, RuleState sepsisScope, FactState measurementState,
            Integer value, int min, int max, String signal) {
        if (sepsisScope == RuleState.INPUT_INSUFFICIENT) return result(id, RuleState.INPUT_INSUFFICIENT, INSUFFICIENT);
        if (sepsisScope == RuleState.SCOPE_MISMATCH) return result(id, RuleState.SCOPE_MISMATCH, SCOPE_MISMATCH);
        if (measurementState != FactState.PRESENT || value == null) return result(id, RuleState.INPUT_INSUFFICIENT, INSUFFICIENT);
        return value.intValue() >= min && value.intValue() <= max ? result(id, RuleState.MATCHED, signal) : result(id, RuleState.NO_MATCH, null);
    }

    private RuleResult sbpHigh(Fixture f, RuleState sepsisScope) {
        String id = "C-RULE-SEPSIS-SBP-HIGH-001";
        if (sepsisScope == RuleState.INPUT_INSUFFICIENT) return result(id, RuleState.INPUT_INSUFFICIENT, INSUFFICIENT);
        if (sepsisScope == RuleState.SCOPE_MISMATCH) return result(id, RuleState.SCOPE_MISMATCH, SCOPE_MISMATCH);
        if (f.sbpState != FactState.PRESENT || f.systolicBp == null) return result(id, RuleState.INPUT_INSUFFICIENT, INSUFFICIENT);
        if (f.systolicBp.intValue() <= 90) return result(id, RuleState.MATCHED, SEPSIS_HIGH);
        if (f.usualSystolicBp != null && f.usualSystolicBpTraceable) {
            return f.usualSystolicBp.intValue() - f.systolicBp.intValue() > 40
                    ? result(id, RuleState.MATCHED, SEPSIS_HIGH) : result(id, RuleState.NO_MATCH, null);
        }
        return result(id, RuleState.INPUT_INSUFFICIENT, INSUFFICIENT);
    }

    private RuleState scopeState(TriState s) {
        if (s == TriState.TRUE) return RuleState.NO_MATCH;
        if (s == TriState.FALSE) return RuleState.SCOPE_MISMATCH;
        return RuleState.INPUT_INSUFFICIENT;
    }

    private TriState fromRuleState(RuleState s) {
        if (s == RuleState.SCOPE_MISMATCH) return TriState.FALSE;
        if (s == RuleState.INPUT_INSUFFICIENT) return TriState.UNKNOWN;
        return TriState.TRUE;
    }

    private boolean coverageCompleteNoMatch(Map<String, RuleResult> rules, boolean dyspnoeaNotApplicable, boolean sepsisNotApplicable) {
        if (!allState(rules, BASELINE, RuleState.NO_MATCH)) return false;
        if (!dyspnoeaNotApplicable && !allState(rules, DYSPNOEA, RuleState.NO_MATCH)) return false;
        if (!sepsisNotApplicable && !allState(rules, SEPSIS, RuleState.NO_MATCH)) return false;
        return true;
    }

    private boolean allState(Map<String, RuleResult> rules, List<String> ids, RuleState state) {
        for (String id : ids) if (rules.get(id).state != state) return false;
        return true;
    }

    private RuleResult result(String id, RuleState state, String signal) { return new RuleResult(id, state, signal); }

    private Outcome outcome(String status, String disposition, String reason, String policy,
            Map<String, RuleResult> rules, List<String> matched, List<String> insufficient, List<String> na) {
        return new Outcome(status, disposition, reason, policy, rules, matched, insufficient, na);
    }
}
