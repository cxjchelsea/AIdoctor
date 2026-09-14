package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;

import java.util.Collections;
import java.util.List;

/** Typed K09 wrapper for U03. */
public final class U03StateProposal {
    private final String proposalId;
    private final String sourceDecisionRef;
    private final List<String> releaseRefs;
    private final StateTypes.StatePatch statePatch;

    public U03StateProposal(String proposalId, String sourceDecisionRef, List<String> releaseRefs,
            StateTypes.StatePatch statePatch) {
        this.proposalId = required(proposalId, "proposalId");
        this.sourceDecisionRef = required(sourceDecisionRef, "sourceDecisionRef");
        this.releaseRefs = releaseRefs == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(releaseRefs);
        if (statePatch == null) throw new IllegalArgumentException("statePatch is required");
        this.statePatch = statePatch;
    }

    public String getProposalId() { return proposalId; }
    public String getSourceDecisionRef() { return sourceDecisionRef; }
    public List<String> getReleaseRefs() { return releaseRefs; }
    public StateTypes.StatePatch getStatePatch() { return statePatch; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
