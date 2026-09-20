package com.aidoctor.diagnosis.runtime.u03;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Concrete non-production D09 implementation mechanically ported from the frozen
 * Gate-C evaluator policy ordering. It consumes C02 rule results and does not
 * re-evaluate clinical thresholds.
 */
public final class U03GateCFrozenDecisionPort implements U03NonProductionDecisionPort {
    private static final Set<String> HIGH_SIGNALS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            U03GateCFrozenRuleEvaluator.RULE_SIGNAL_CRITICAL_RED_FLAG,
            U03GateCFrozenRuleEvaluator.RULE_SIGNAL_MUST_NOT_MISS,
            U03GateCFrozenRuleEvaluator.RULE_SIGNAL_SEPSIS_HIGH)));

    @Override
    public U03DecisionOutcome decide(
            U03NonProductionExecutionContext context,
            U03RiskAssessmentCandidate acceptedCandidate,
            U03ResolvedNonProductionReleaseSet resolvedReleaseSet) {
        if (context == null || acceptedCandidate == null || resolvedReleaseSet == null) {
            throw new IllegalArgumentException("Gate-C D09 inputs are required");
        }
        resolvedReleaseSet.getRefs().requireGateCFrozenSet();

        U03GateCRuleEvaluation evaluation = acceptedCandidate.getGateCEvaluation();
        if (evaluation == null) {
            throw new IllegalStateException("Gate-C D09 requires frozen C02 rule execution evidence");
        }

        String preFailure = evaluation.getPreDecisionFailureReason();
        if (preFailure != null) return failed(context, acceptedCandidate, preFailure);

        List<U03GateCRuleResult> results = evaluation.getRuleResults();
        if (hasSharedScopeConflict(results)) {
            return failed(context, acceptedCandidate, "UNRESOLVABLE_CONFLICT");
        }

        boolean matchedHigh = false;
        boolean matchedModerateHigh = false;
        boolean insufficient = false;
        for (U03GateCRuleResult result : results) {
            if ("MATCHED".equals(result.getExecutionState()) && HIGH_SIGNALS.contains(result.getSignal())) {
                matchedHigh = true;
            }
            if ("INPUT_INSUFFICIENT".equals(result.getExecutionState())) insufficient = true;
            if ("MATCHED".equals(result.getExecutionState())
                    && U03GateCFrozenRuleEvaluator.RULE_SIGNAL_SEPSIS_MODHIGH.equals(result.getSignal())) {
                matchedModerateHigh = true;
            }
        }

        // Frozen policy ordering: P1 > P2 > P3 > P4.
        if (matchedHigh) {
            return valid(context, acceptedCandidate, "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT");
        }
        if (insufficient) {
            return failed(context, acceptedCandidate, "INSUFFICIENT_INFORMATION");
        }
        if (matchedModerateHigh) {
            return valid(context, acceptedCandidate, "CAUTION", "MODERATE_HIGH_RULE_SIGNAL_PRESENT");
        }
        if (coverageComplete(results)) {
            return valid(
                    context,
                    acceptedCandidate,
                    "NO_HIGH_RISK_SIGNAL",
                    "COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL");
        }
        return failed(context, acceptedCandidate, "UNRESOLVABLE_CONFLICT");
    }

    @Override
    public U03DecisionOutcome decide(
            U03ExecutionCommand command,
            U03RiskAssessmentCandidate candidate,
            U03ReleaseBinding releaseBinding) {
        throw new IllegalStateException("Gate-C frozen D09 is authorized only on the explicit CD-07 non-production path");
    }

    private static U03DecisionOutcome valid(
            U03NonProductionExecutionContext context,
            U03RiskAssessmentCandidate candidate,
            String outcome,
            String reason) {
        return new U03DecisionOutcome(
                "u03-gatec-d09-" + context.getCommand().eventId,
                U03RiskAssessmentCandidate.VALID,
                outcome,
                reason,
                candidate.getEvidenceRefs());
    }

    private static U03DecisionOutcome failed(
            U03NonProductionExecutionContext context,
            U03RiskAssessmentCandidate candidate,
            String reason) {
        return new U03DecisionOutcome(
                "u03-gatec-d09-" + context.getCommand().eventId,
                U03RiskAssessmentCandidate.FAILED,
                null,
                reason,
                candidate.getEvidenceRefs());
    }

    private static boolean hasSharedScopeConflict(List<U03GateCRuleResult> results) {
        return familyStateConflict(results, U03GateCFrozenRuleEvaluator.DYSPNOEA_RULES)
                || familyStateConflict(results, U03GateCFrozenRuleEvaluator.SEPSIS_RULES);
    }

    private static boolean familyStateConflict(List<U03GateCRuleResult> results, List<String> ruleIds) {
        Map<String, U03GateCRuleResult> byId = byId(results);
        boolean sawMismatch = false;
        boolean sawOther = false;
        for (String ruleId : ruleIds) {
            U03GateCRuleResult result = byId.get(ruleId);
            if (result == null) continue;
            if ("SCOPE_MISMATCH".equals(result.getExecutionState())) sawMismatch = true;
            else sawOther = true;
        }
        return sawMismatch && sawOther;
    }

    private static boolean coverageComplete(List<U03GateCRuleResult> results) {
        Map<String, U03GateCRuleResult> byId = byId(results);
        for (String rule : U03GateCFrozenRuleEvaluator.BASELINE_RULES) {
            U03GateCRuleResult result = byId.get(rule);
            if (result == null || !"NO_MATCH".equals(result.getExecutionState())) return false;
        }
        return familyCompleteOrNotApplicable(byId, U03GateCFrozenRuleEvaluator.DYSPNOEA_RULES)
                && familyCompleteOrNotApplicable(byId, U03GateCFrozenRuleEvaluator.SEPSIS_RULES);
    }

    private static boolean familyCompleteOrNotApplicable(
            Map<String, U03GateCRuleResult> byId,
            List<String> ruleIds) {
        boolean allMismatch = true;
        boolean anyMismatch = false;
        for (String ruleId : ruleIds) {
            U03GateCRuleResult result = byId.get(ruleId);
            if (result == null) return false;
            String state = result.getExecutionState();
            if (!"SCOPE_MISMATCH".equals(state)) allMismatch = false;
            if ("SCOPE_MISMATCH".equals(state)) anyMismatch = true;
        }
        if (allMismatch) return true;
        if (anyMismatch) return false;
        for (String ruleId : ruleIds) {
            if (!"NO_MATCH".equals(byId.get(ruleId).getExecutionState())) return false;
        }
        return true;
    }

    private static Map<String, U03GateCRuleResult> byId(List<U03GateCRuleResult> results) {
        Map<String, U03GateCRuleResult> byId = new HashMap<String, U03GateCRuleResult>();
        for (U03GateCRuleResult result : results) byId.put(result.getRuleId(), result);
        return byId;
    }
}
