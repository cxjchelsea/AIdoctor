package com.aidoctor.diagnosis.runtime.u03;

/** D09 boundary. Implementations must be deterministic and version-bound. */
public interface U03DecisionPort {
    U03DecisionOutcome decide(U03ExecutionCommand command, U03RiskAssessmentCandidate candidate,
            U03ReleaseBinding releaseBinding);
}
