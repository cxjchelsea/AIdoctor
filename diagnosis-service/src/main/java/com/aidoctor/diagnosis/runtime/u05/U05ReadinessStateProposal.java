package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.contracts.v1.StateTypes;

/** K09-shaped U05 readiness proposal. Proposal != committed readiness truth. */
public final class U05ReadinessStateProposal {
    private final String effectId;
    private final String readinessRecordId;
    private final String proposalId;
    private final String canonicalPayloadFingerprint;
    private final String sourceDecisionRef;
    private final StateTypes.StatePatch statePatch;

    public U05ReadinessStateProposal(
            String effectId,
            String readinessRecordId,
            String proposalId,
            String canonicalPayloadFingerprint,
            String sourceDecisionRef,
            StateTypes.StatePatch statePatch) {
        this.effectId = required(effectId, "effectId");
        this.readinessRecordId = required(readinessRecordId, "readinessRecordId");
        this.proposalId = required(proposalId, "proposalId");
        this.canonicalPayloadFingerprint = required(canonicalPayloadFingerprint, "canonicalPayloadFingerprint");
        this.sourceDecisionRef = required(sourceDecisionRef, "sourceDecisionRef");
        if (statePatch == null) throw new IllegalArgumentException("statePatch is required");
        this.statePatch = statePatch;
    }

    public String getEffectId() { return effectId; }
    public String getReadinessRecordId() { return readinessRecordId; }
    public String getProposalId() { return proposalId; }
    public String getCanonicalPayloadFingerprint() { return canonicalPayloadFingerprint; }
    public String getSourceDecisionRef() { return sourceDecisionRef; }
    public StateTypes.StatePatch getStatePatch() { return statePatch; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
