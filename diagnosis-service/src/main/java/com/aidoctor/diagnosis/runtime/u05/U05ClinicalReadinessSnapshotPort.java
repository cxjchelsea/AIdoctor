package com.aidoctor.diagnosis.runtime.u05;

/**
 * U05-owned exact-readback boundary.
 *
 * <p>A mechanical COMMITTED result is insufficient without this read-back.</p>
 */
public interface U05ClinicalReadinessSnapshotPort {
    U05ClinicalReadinessSnapshot read(
            String cdpId,
            int expectedCommittedVersion);
}
