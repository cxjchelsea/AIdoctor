package com.aidoctor.diagnosis.evaluation.u03;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Evaluation-only executable semantics for AUTH-U03-GATEC-EVAL-IMPL-001. */
public final class U03EvaluationClinicalEvaluator {
    public static final String RULE_RELEASE = "RR-U03-RISK-001@0.2.1-candidate";
    public static final String KNOWLEDGE_RELEASE = "KR-U03-SOURCE-001@0.1.0-candidate";
    public static final String COVERAGE_RELEASE = "U03_D09_COVERAGE_V0_2_1_CANDIDATE";
    public static final String POLICY_RELEASE = "PR-U03-D09-001@0.2.1-candidate";

    public enum FactState { PRESENT, ABSENT, UNKNOWN, UNMEASURED, NOT_ASKED, AMBIGUOUS, CONFLICTING, REMOTE_NOT_OBSERVED }
    public enum ScopeState { TRUE, FALSE, UNKNOWN, NOT_ASKED, NOT_ESTABLISHED }
    public enum RuleOutcome { MATCHED, NO_MATCH, INPUT_INSUFFICIENT, SCOPE_MISMATCH }

    public static final class Fixture {
        public String id;
        public String ruleRelease = RULE_RELEASE;
        public String knowledgeRelease = KNOWLEDGE_RELEASE;
        public String coverageRelease = COVERAGE_RELEASE;
        public String policyRelease = POLICY_RELEASE;
        public ScopeState pregnancy = ScopeState.FALSE;
        public boolean pediatric;
        public boolean inRegion = true;
        public boolean inChannel = true;
        public boolean currentVersion = true;
        public boolean conflict;
        public ScopeState dyspnoeaContext = ScopeState.FALSE;
        public ScopeState sepsisContext = ScopeState.FALSE;
        public boolean sepsisSettingSupported = true;
        public Integer age = 18;
        public Integer rr;
        public Integer sbp;
        public Integer usualSbp;
        public Integer hr;
        public FactState rrState = FactState.ABSENT;
        public FactState sbpState = FactState.ABSENT;
        public FactState hrState = FactState.ABSENT;
        public final Map<String, FactState> evidence = new LinkedHashMap<String, FactState>();
        public String idempotencyKey;

        public Fixture(String id) { this.id = id; }
        public Fixture evidence(String ref, FactState state) { evidence.put(ref, state); return this; }
        public FactState evidenceState(String ref) {
            FactState state = evidence.get(ref);
            return state == null ? FactState.ABSENT : state;
        }
    }

    public static final class RuleResult {
        public final String ruleId;
        public final RuleOutcome outcome;
        public final String signal;
        RuleResult(String ruleId, RuleOutcome outcome, String signal) {
            this.ruleId = ruleId; this.outcome = outcome; this.signal = signal;
        }
    }

    public static final class Decision {
        public final String branch;
        public final String status;
        public final String disposition;
        public final String reasonCode;
        public final List<RuleResult> ruleResults;
        Decision(String branch, String status, String disposition, String reasonCode, List<RuleResult> ruleResults) {
            this.branch = branch; this.status = status; this.disposition = disposition; this.reasonCode = reasonCode;
            this.ruleResults = Collections.unmodifiableList(new ArrayList<RuleResult>(ruleResults));
        }
    }

    public Decision evaluate(Fixture f) {
        if (!exactBindings(f)) return failed("P0", "RELEASE_MISMATCH");
        if (!f.currentVersion) return failed("P0", "STALE_INPUT");
        if (f.pediatric || !f.inRegion || !f.inChannel || f.pregnancy == ScopeState.TRUE) {
            return failed("P0", "OVERALL_POLICY_SCOPE_MISMATCH");
        }
        if (f.pregnancy == ScopeState.UNKNOWN || f.pregnancy == ScopeState.NOT_ASKED || f.pregnancy == ScopeState.NOT_ESTABLISHED) {
            return failed("P0", "OVERALL_POLICY_SCOPE_NOT_ESTABLISHED");
        }
        if (f.conflict) return failed("P5", "UNRESOLVABLE_CONFLICT");

        List<RuleResult> results = new ArrayList<RuleResult>();
        results.add(evidenceRule("C-RULE-RESP-001", f.evidenceState("EV-RF-RESP-001"), "RULE_SIGNAL_CRITICAL_RED_FLAG"));
        results.add(evidenceRule("C-RULE-NEURO-001", f.evidenceState("EV-MNM-NEURO-001"), "RULE_SIGNAL_MUST_NOT_MISS"));
        results.add(evidenceRule("C-RULE-NEURO-002", f.evidenceState("EV-MNM-NEURO-002"), "RULE_SIGNAL_MUST_NOT_MISS"));
        results.add(evidenceRule("C-RULE-CARD-001", f.evidenceState("EV-MNM-CARD-001"), "RULE_SIGNAL_MUST_NOT_MISS"));
        results.add(evidenceRule("C-RULE-ALLERGY-001", f.evidenceState("EV-RF-ALLERGY-001"), "RULE_SIGNAL_CRITICAL_RED_FLAG"));
        results.add(contextEvidenceRule("C-RULE-DYSPNOEA-APPEAR-001", f.dyspnoeaContext, f.evidenceState("EV-RF-APPEAR-001"), "RULE_SIGNAL_CRITICAL_RED_FLAG"));
        results.add(contextEvidenceRule("C-RULE-DYSPNOEA-CONFUSION-001", f.dyspnoeaContext, f.evidenceState("EV-RF-NEURO-001"), "RULE_SIGNAL_CRITICAL_RED_FLAG"));

        RuleOutcome sepsisScope = sepsisScope(f);
        results.add(numericRule("C-RULE-SEPSIS-RR-HIGH-001", sepsisScope, f.rrState, f.rr, 25, Integer.MAX_VALUE, "RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION"));
        results.add(numericRule("C-RULE-SEPSIS-RR-MODHIGH-001", sepsisScope, f.rrState, f.rr, 21, 24, "RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION"));
        results.add(sbpHighRule(f, sepsisScope));
        results.add(numericRule("C-RULE-SEPSIS-SBP-MODHIGH-001", sepsisScope, f.sbpState, f.sbp, 91, 100, "RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION"));
        results.add(numericRule("C-RULE-SEPSIS-HR-HIGH-001", sepsisScope, f.hrState, f.hr, 131, Integer.MAX_VALUE, "RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION"));
        results.add(numericRule("C-RULE-SEPSIS-HR-MODHIGH-001", sepsisScope, f.hrState, f.hr, 91, 130, "RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION"));
        results.add(contextEvidenceRule("C-RULE-SEPSIS-APPEAR-HIGH-001", toScopeState(sepsisScope), f.evidenceState("EV-RF-APPEAR-001"), "RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION"));
        results.add(contextEvidenceRule("C-RULE-SEPSIS-RASH-HIGH-001", toScopeState(sepsisScope), f.evidenceState("EV-RF-SEPSIS-001"), "RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION"));

        if (hasHigh(results)) return valid("P1", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", results);
        if (hasInsufficient(results)) return failed("P2", "INSUFFICIENT_INFORMATION", results);
        if (hasModerateHigh(results)) return valid("P3", "CAUTION", "MODERATE_HIGH_RULE_SIGNAL_PRESENT", results);
        if (coverageComplete(results)) return valid("P4", "NO_HIGH_RISK_SIGNAL", "COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL", results);
        return failed("P5", "UNRESOLVABLE_CONFLICT", results);
    }

    private static boolean exactBindings(Fixture f) {
        if (containsLatest(f.ruleRelease) || containsLatest(f.knowledgeRelease) || containsLatest(f.coverageRelease) || containsLatest(f.policyRelease)) return false;
        return RULE_RELEASE.equals(f.ruleRelease) && KNOWLEDGE_RELEASE.equals(f.knowledgeRelease)
                && COVERAGE_RELEASE.equals(f.coverageRelease) && POLICY_RELEASE.equals(f.policyRelease);
    }

    private static boolean containsLatest(String value) { return value != null && value.toLowerCase().contains("latest"); }

    private static RuleResult evidenceRule(String id, FactState state, String matchedSignal) {
        if (state == FactState.PRESENT) return matched(id, matchedSignal);
        if (state == FactState.ABSENT) return noMatch(id);
        return insufficient(id);
    }

    private static RuleResult contextEvidenceRule(String id, ScopeState scope, FactState state, String matchedSignal) {
        if (scope == ScopeState.FALSE) return scopeMismatch(id);
        if (scope != ScopeState.TRUE) return insufficient(id);
        return evidenceRule(id, state, matchedSignal);
    }

    private static RuleOutcome sepsisScope(Fixture f) {
        if (f.age == null || f.sepsisContext == ScopeState.UNKNOWN || f.sepsisContext == ScopeState.NOT_ASKED || f.sepsisContext == ScopeState.NOT_ESTABLISHED) return RuleOutcome.INPUT_INSUFFICIENT;
        if (f.age.intValue() < 16 || f.sepsisContext == ScopeState.FALSE || !f.sepsisSettingSupported) return RuleOutcome.SCOPE_MISMATCH;
        return RuleOutcome.NO_MATCH;
    }

    private static ScopeState toScopeState(RuleOutcome scope) {
        if (scope == RuleOutcome.SCOPE_MISMATCH) return ScopeState.FALSE;
        if (scope == RuleOutcome.INPUT_INSUFFICIENT) return ScopeState.UNKNOWN;
        return ScopeState.TRUE;
    }

    private static RuleResult numericRule(String id, RuleOutcome scope, FactState state, Integer value, int min, int max, String signal) {
        if (scope == RuleOutcome.SCOPE_MISMATCH) return scopeMismatch(id);
        if (scope == RuleOutcome.INPUT_INSUFFICIENT) return insufficient(id);
        if (state != FactState.PRESENT || value == null) {
            if (state == FactState.ABSENT) return noMatch(id);
            return insufficient(id);
        }
        return value.intValue() >= min && value.intValue() <= max ? matched(id, signal) : noMatch(id);
    }

    private static RuleResult sbpHighRule(Fixture f, RuleOutcome scope) {
        String id = "C-RULE-SEPSIS-SBP-HIGH-001";
        if (scope == RuleOutcome.SCOPE_MISMATCH) return scopeMismatch(id);
        if (scope == RuleOutcome.INPUT_INSUFFICIENT) return insufficient(id);
        if (f.sbpState != FactState.PRESENT || f.sbp == null) {
            if (f.sbpState == FactState.ABSENT) return noMatch(id);
            return insufficient(id);
        }
        if (f.sbp.intValue() <= 90) return matched(id, "RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION");
        if (f.usualSbp == null) return insufficient(id);
        return f.usualSbp.intValue() - f.sbp.intValue() > 40 ? matched(id, "RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION") : noMatch(id);
    }

    private static boolean hasHigh(List<RuleResult> rs) {
        for (RuleResult r : rs) if (r.outcome == RuleOutcome.MATCHED && ("RULE_SIGNAL_CRITICAL_RED_FLAG".equals(r.signal)
                || "RULE_SIGNAL_MUST_NOT_MISS".equals(r.signal) || "RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION".equals(r.signal))) return true;
        return false;
    }
    private static boolean hasModerateHigh(List<RuleResult> rs) {
        for (RuleResult r : rs) if (r.outcome == RuleOutcome.MATCHED && "RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION".equals(r.signal)) return true;
        return false;
    }
    private static boolean hasInsufficient(List<RuleResult> rs) {
        for (RuleResult r : rs) if (r.outcome == RuleOutcome.INPUT_INSUFFICIENT) return true;
        return false;
    }
    private static boolean coverageComplete(List<RuleResult> rs) {
        for (RuleResult r : rs) if (r.outcome == RuleOutcome.INPUT_INSUFFICIENT || r.outcome == RuleOutcome.MATCHED) return false;
        return true;
    }

    private static RuleResult matched(String id, String signal) { return new RuleResult(id, RuleOutcome.MATCHED, signal); }
    private static RuleResult noMatch(String id) { return new RuleResult(id, RuleOutcome.NO_MATCH, null); }
    private static RuleResult insufficient(String id) { return new RuleResult(id, RuleOutcome.INPUT_INSUFFICIENT, "RULE_SIGNAL_INPUT_INSUFFICIENT"); }
    private static RuleResult scopeMismatch(String id) { return new RuleResult(id, RuleOutcome.SCOPE_MISMATCH, "RULE_SIGNAL_SCOPE_MISMATCH"); }
    private static Decision failed(String branch, String reason) { return new Decision(branch, "FAILED", null, reason, Collections.<RuleResult>emptyList()); }
    private static Decision failed(String branch, String reason, List<RuleResult> rs) { return new Decision(branch, "FAILED", null, reason, rs); }
    private static Decision valid(String branch, String disposition, String reason, List<RuleResult> rs) { return new Decision(branch, "VALID", disposition, reason, rs); }
}
