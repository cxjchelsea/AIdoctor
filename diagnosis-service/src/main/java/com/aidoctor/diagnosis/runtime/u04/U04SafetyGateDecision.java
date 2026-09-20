package com.aidoctor.diagnosis.runtime.u04;

/** Frozen U04 business-owner Safety Gate decision. */
public final class U04SafetyGateDecision {
    public static final String ALLOW = "ALLOW";
    public static final String RESTRICTED = "RESTRICTED";
    public static final String BLOCKED = "BLOCKED";
    public static final String UNAVAILABLE = "UNAVAILABLE";

    private final String decisionId;
    private final String gate;
    private final String reasonCode;
    private final int clinicalStateVersion;
    private final String policyRef;
    private final String sourceU03DecisionRef;

    public U04SafetyGateDecision(
            String decisionId,
            String gate,
            String reasonCode,
            int clinicalStateVersion,
            String policyRef,
            String sourceU03DecisionRef) {
        this.decisionId = required(decisionId, "decisionId");
        this.gate = gate(gate);
        this.reasonCode = required(reasonCode, "reasonCode");
        if (clinicalStateVersion < 0) {
            throw new IllegalArgumentException("clinicalStateVersion must be non-negative");
        }
        this.clinicalStateVersion = clinicalStateVersion;
        this.policyRef = required(policyRef, "policyRef");
        this.sourceU03DecisionRef = required(sourceU03DecisionRef, "sourceU03DecisionRef");
    }

    public String getDecisionId() { return decisionId; }
    public String getGate() { return gate; }
    public String getReasonCode() { return reasonCode; }
    public int getClinicalStateVersion() { return clinicalStateVersion; }
    public String getPolicyRef() { return policyRef; }
    public String getSourceU03DecisionRef() { return sourceU03DecisionRef; }

    private static String gate(String value) {
        String required = required(value, "gate");
        if (!ALLOW.equals(required)
                && !RESTRICTED.equals(required)
                && !BLOCKED.equals(required)
                && !UNAVAILABLE.equals(required)) {
            throw new IllegalArgumentException("unsupported Safety Gate");
        }
        return required;
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
