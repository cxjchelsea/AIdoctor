package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.contracts.v1.StateTypes;

/** K09-shaped dedicated readiness invalidation proposal. */
public final class U05ReadinessInvalidationProposal {
    private final String invalidationEffectId;
    private final String proposalId;
    private final String canonicalPayloadFingerprint;
    private final StateTypes.StatePatch statePatch;

    public U05ReadinessInvalidationProposal(
            String invalidationEffectId,
            String proposalId,
            String canonicalPayloadFingerprint,
            StateTypes.StatePatch statePatch) {
        this.invalidationEffectId = required(invalidationEffectId, "invalidationEffectId");
        this.proposalId = required(proposalId, "proposalId");
        this.canonicalPayloadFingerprint = required(canonicalPayloadFingerprint, "canonicalPayloadFingerprint");
        if (statePatch == null) throw new IllegalArgumentException("statePatch is required");
        this.statePatch = statePatch;
    }

    public String getInvalidationEffectId() { return invalidationEffectId; }
    public String getProposalId() { return proposalId; }
    public String getCanonicalPayloadFingerprint() { return canonicalPayloadFingerprint; }
    public StateTypes.StatePatch getStatePatch() { return statePatch; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
