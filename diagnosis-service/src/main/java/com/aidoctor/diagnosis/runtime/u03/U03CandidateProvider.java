package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;

/** Port implemented by the governed C02 adapter; returns candidate-only output. */
public interface U03CandidateProvider {
    U03RiskAssessmentCandidate assess(U03ExecutionCommand command, CapabilityBindingRecord capabilityBinding,
            U03ReleaseBinding releaseBinding);
}
