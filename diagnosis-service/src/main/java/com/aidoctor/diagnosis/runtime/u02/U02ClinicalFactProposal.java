package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.contracts.v1.StateTypes;

import java.util.Collections;
import java.util.List;

/**
 * U02 typed K09 proposal wrapper. Business ownership and capability binding
 * evidence remain explicit; the embedded StatePatch is only the P01 mechanical payload.
 */
public final class U02ClinicalFactProposal {
    private final String proposalId;
    private final String consultationId;
    private final String businessOwner;
    private final String sourceDecisionRef;
    private final List<String> capabilityBindingRefs;
    private final StateTypes.StatePatch statePatch;

    public U02ClinicalFactProposal(
            String proposalId,
            String consultationId,
            String businessOwner,
            String sourceDecisionRef,
            List<String> capabilityBindingRefs,
            StateTypes.StatePatch statePatch) {
        this.proposalId = proposalId;
        this.consultationId = consultationId;
        this.businessOwner = businessOwner;
        this.sourceDecisionRef = sourceDecisionRef;
        this.capabilityBindingRefs = capabilityBindingRefs == null
                ? Collections.<String>emptyList() : Collections.unmodifiableList(capabilityBindingRefs);
        this.statePatch = statePatch;
    }

    public String getProposalId() { return proposalId; }
    public String getConsultationId() { return consultationId; }
    public String getBusinessOwner() { return businessOwner; }
    public String getSourceDecisionRef() { return sourceDecisionRef; }
    public List<String> getCapabilityBindingRefs() { return capabilityBindingRefs; }
    public StateTypes.StatePatch getStatePatch() { return statePatch; }
}
