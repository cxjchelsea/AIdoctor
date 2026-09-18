package com.aidoctor.diagnosis.runtime.u03;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * C02-side frozen rule execution evidence. This is not the D09 business decision
 * and carries no Clinical State mutation authority.
 */
public final class U03GateCRuleEvaluation {
    private final List<U03GateCRuleResult> ruleResults;
    private final String preDecisionFailureReason;

    public U03GateCRuleEvaluation(
            List<U03GateCRuleResult> ruleResults,
            String preDecisionFailureReason) {
        this.ruleResults = ruleResults == null
                ? Collections.<U03GateCRuleResult>emptyList()
                : Collections.unmodifiableList(new ArrayList<U03GateCRuleResult>(ruleResults));
        this.preDecisionFailureReason = preDecisionFailureReason;
    }

    public List<U03GateCRuleResult> getRuleResults() { return ruleResults; }
    public String getPreDecisionFailureReason() { return preDecisionFailureReason; }
}
