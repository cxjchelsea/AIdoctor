package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.StateCommitter;

/**
 * Narrow P01/U02 adapter. Only business-owned U02 proposals can reach the
 * mechanical StateCommitter through this boundary.
 */
public final class U02ClinicalFactCommitService {
    private final StateCommitter stateCommitter;

    public U02ClinicalFactCommitService(StateCommitter stateCommitter) {
        if (stateCommitter == null) throw new IllegalArgumentException("stateCommitter is required");
        this.stateCommitter = stateCommitter;
    }

    public StateTypes.CommitResult commit(U02ClinicalFactProposal proposal) {
        if (proposal == null || proposal.getStatePatch() == null) {
            throw new IllegalArgumentException("U02 Clinical Fact proposal is required");
        }
        if (!U02ClinicalFactBusinessOwner.OWNER.equals(proposal.getBusinessOwner())) {
            throw new IllegalArgumentException("U02 proposal is not owned by the Clinical Fact Business Owner");
        }
        if (proposal.getSourceDecisionRef() == null || proposal.getSourceDecisionRef().trim().isEmpty()) {
            throw new IllegalArgumentException("U02 proposal must reference its source business decision");
        }
        if (proposal.getCapabilityBindingRefs().isEmpty()) {
            throw new IllegalArgumentException("U02 proposal must bind the contributing capability release");
        }
        return stateCommitter.commit(proposal.getStatePatch());
    }
}
