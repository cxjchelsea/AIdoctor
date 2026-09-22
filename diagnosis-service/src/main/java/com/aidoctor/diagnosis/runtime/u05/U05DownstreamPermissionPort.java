package com.aidoctor.diagnosis.runtime.u05;

/**
 * U05 route-time permission boundary.
 *
 * <p>The implementation of this port is outside U05 business ownership. U05
 * only consumes a current Safety/permission decision for the exact candidate
 * downstream action.</p>
 */
public interface U05DownstreamPermissionPort {
    U05DownstreamPermissionDecision evaluate(
            U05AdmittedInput input,
            U05ClinicalReadinessCommitEvidence commitEvidence,
            U05RoutingCurrentness currentness,
            String downstreamConsequence,
            String targetUnitId,
            String targetAction);
}
