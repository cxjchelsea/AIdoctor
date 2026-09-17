package com.aidoctor.diagnosis.runtime.u03;

/**
 * CD-07 non-production D09 owner boundary.
 *
 * <p>The implementation consumes an already accepted C02 candidate plus the exact
 * Gate-C-frozen release set. It must not recalculate C-layer clinical thresholds,
 * invent new disposition vocabulary, or mutate canonical Clinical State.</p>
 */
public interface U03NonProductionDecisionPort extends U03DecisionPort {
    U03DecisionOutcome decide(
            U03NonProductionExecutionContext context,
            U03RiskAssessmentCandidate acceptedCandidate,
            U03ResolvedNonProductionReleaseSet resolvedReleaseSet);
}
