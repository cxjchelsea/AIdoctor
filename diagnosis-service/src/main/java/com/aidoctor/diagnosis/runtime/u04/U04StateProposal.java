package com.aidoctor.diagnosis.runtime.u04;

import com.aidoctor.contracts.v1.StateTypes;

/** Typed U04 proposal. Proposal != committed Safety Gate state. */
public final class U04StateProposal {
    private final String proposalId;
    private final String sourceDecisionRef;
    private final String policyRef;
    private final StateTypes.StatePatch statePatch;

    public U04StateProposal(
            String proposalId,
            String sourceDecisionRef,
            String policyRef,
            StateTypes.StatePatch statePatch) {
        this.proposalId = required(proposalId, "proposalId");
        this.sourceDecisionRef = required(sourceDecisionRef, "sourceDecisionRef");
        this.policyRef = required(policyRef, "policyRef");
        if (statePatch == null) throw new IllegalArgumentException("statePatch is required");
        this.statePatch = statePatch;
    }

    public String getProposalId() { return proposalId; }
    public String getSourceDecisionRef() { return sourceDecisionRef; }
    public String getPolicyRef() { return policyRef; }
    public StateTypes.StatePatch getStatePatch() { return statePatch; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
