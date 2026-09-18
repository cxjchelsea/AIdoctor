package com.aidoctor.diagnosis.runtime.u03;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Exact non-production result of one Gate-C-frozen C rule execution. */
public final class U03GateCRuleResult {
    private final String ruleId;
    private final String executionState;
    private final String signal;
    private final List<String> evidenceRefs;

    public U03GateCRuleResult(
            String ruleId,
            String executionState,
            String signal,
            List<String> evidenceRefs) {
        this.ruleId = required(ruleId, "ruleId");
        this.executionState = required(executionState, "executionState");
        this.signal = signal;
        this.evidenceRefs = evidenceRefs == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(evidenceRefs));
    }

    public String getRuleId() { return ruleId; }
    public String getExecutionState() { return executionState; }
    public String getSignal() { return signal; }
    public List<String> getEvidenceRefs() { return evidenceRefs; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
