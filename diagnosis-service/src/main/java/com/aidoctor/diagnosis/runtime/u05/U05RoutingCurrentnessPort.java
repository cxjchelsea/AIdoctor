package com.aidoctor.diagnosis.runtime.u05;

/** Read-only authoritative currentness boundary used after readiness commit. */
public interface U05RoutingCurrentnessPort {
    U05RoutingCurrentness inspect(
            U05AdmittedInput input,
            U05ClinicalReadinessDecision decision,
            U05ClinicalReadinessCommitEvidence commitEvidence);
}
