package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;

/**
 * CD-07 non-production C02 provider boundary that explicitly consumes the
 * accepted-evidence binding for the exact execution.
 */
public interface U03AcceptedEvidenceAwareCandidateProvider extends U03CandidateProvider {
    U03RiskAssessmentCandidate assess(
            U03ExecutionCommand command,
            CapabilityBindingRecord capabilityBinding,
            U03ReleaseBinding releaseBinding,
            U03AcceptedEvidenceBinding acceptedEvidenceBinding);
}
