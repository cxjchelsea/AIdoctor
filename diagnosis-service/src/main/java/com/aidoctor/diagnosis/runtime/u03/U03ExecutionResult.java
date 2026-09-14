package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;

/** Component result; routing to U04 remains outside this class. */
public final class U03ExecutionResult {
    private final String status;
    private final String capabilityCallId;
    private final U03DecisionOutcome decision;
    private final U03StateProposal proposal;
    private final StateTypes.CommitResult commitResult;

    public U03ExecutionResult(String status, String capabilityCallId, U03DecisionOutcome decision,
            U03StateProposal proposal, StateTypes.CommitResult commitResult) {
        this.status = status;
        this.capabilityCallId = capabilityCallId;
        this.decision = decision;
        this.proposal = proposal;
        this.commitResult = commitResult;
    }

    public String getStatus() { return status; }
    public String getCapabilityCallId() { return capabilityCallId; }
    public U03DecisionOutcome getDecision() { return decision; }
    public U03StateProposal getProposal() { return proposal; }
    public StateTypes.CommitResult getCommitResult() { return commitResult; }
}
