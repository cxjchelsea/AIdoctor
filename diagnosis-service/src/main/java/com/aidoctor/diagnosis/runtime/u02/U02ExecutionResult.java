package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.u02.d05.U02DependencyInvalidationHook;

import java.util.Collections;
import java.util.List;

/** U02 component result; not a patient-facing delivery artifact. */
public final class U02ExecutionResult {
    public final String status;
    public final String reasonCode;
    public final String capabilityCallId;
    public final U02ClinicalFactDecision decision;
    public final U02ClinicalFactProposal proposal;
    public final StateTypes.CommitResult commitResult;
    public final List<U02DependencyInvalidationHook.InvalidationRecord> invalidations;

    public U02ExecutionResult(
            String status,
            String reasonCode,
            String capabilityCallId,
            U02ClinicalFactDecision decision,
            U02ClinicalFactProposal proposal,
            StateTypes.CommitResult commitResult,
            List<U02DependencyInvalidationHook.InvalidationRecord> invalidations) {
        this.status = status;
        this.reasonCode = reasonCode;
        this.capabilityCallId = capabilityCallId;
        this.decision = decision;
        this.proposal = proposal;
        this.commitResult = commitResult;
        this.invalidations = invalidations == null
                ? Collections.<U02DependencyInvalidationHook.InvalidationRecord>emptyList()
                : Collections.unmodifiableList(invalidations);
    }
}
