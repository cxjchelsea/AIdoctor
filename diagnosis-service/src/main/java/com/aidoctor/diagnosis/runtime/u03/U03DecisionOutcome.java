package com.aidoctor.diagnosis.runtime.u03;

import java.util.Collections;
import java.util.List;

/** Business-owned U03 decision value. The concrete policy is supplied separately. */
public final class U03DecisionOutcome {
    private final String decisionId;
    private final String status;
    private final String outcomeCode;
    private final String reasonCode;
    private final List<String> evidenceRefs;

    public U03DecisionOutcome(String decisionId, String status, String outcomeCode,
            String reasonCode, List<String> evidenceRefs) {
        this.decisionId = required(decisionId, "decisionId");
        this.status = required(status, "status");
        this.outcomeCode = outcomeCode;
        this.reasonCode = required(reasonCode, "reasonCode");
        this.evidenceRefs = evidenceRefs == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(evidenceRefs);
        if (U03RiskAssessmentCandidate.FAILED.equals(status) && outcomeCode != null) {
            throw new IllegalArgumentException("FAILED status must not carry a normal outcomeCode");
        }
    }

    public String getDecisionId() { return decisionId; }
    public String getStatus() { return status; }
    public String getOutcomeCode() { return outcomeCode; }
    public String getReasonCode() { return reasonCode; }
    public List<String> getEvidenceRefs() { return evidenceRefs; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
