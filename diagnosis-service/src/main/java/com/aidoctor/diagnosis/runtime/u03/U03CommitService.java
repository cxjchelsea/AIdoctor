package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.StateCommitter;

/** Narrow P01 adapter for U03 typed proposals. */
public final class U03CommitService {
    private final StateCommitter stateCommitter;

    public U03CommitService(StateCommitter stateCommitter) {
        if (stateCommitter == null) throw new IllegalArgumentException("stateCommitter is required");
        this.stateCommitter = stateCommitter;
    }

    public StateTypes.CommitResult commit(U03StateProposal proposal) {
        if (proposal == null || proposal.getStatePatch() == null) throw new IllegalArgumentException("U03 proposal is required");
        if (proposal.getSourceDecisionRef() == null || proposal.getSourceDecisionRef().trim().isEmpty()) {
            throw new IllegalArgumentException("U03 proposal must reference its source decision");
        }
        if (proposal.getReleaseRefs().isEmpty()) {
            throw new IllegalArgumentException("U03 proposal must retain at least the attempted capability binding ref");
        }
        if (proposal.isFullReleaseEvidenceRequired() && proposal.getReleaseRefs().size() < 3) {
            throw new IllegalArgumentException("VALID U03 proposal must bind capability, rule and knowledge releases");
        }
        return stateCommitter.commit(proposal.getStatePatch());
    }
}
