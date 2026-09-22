package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.diagnosis.state.committer.SyntheticStateSnapshot;
import com.aidoctor.diagnosis.state.committer.SyntheticVersionedStateRepository;

import java.util.Map;

/**
 * Non-production U05 adapter over the verified PBNC-02A synthetic repository.
 *
 * <p>This adapter does not turn SyntheticStateSnapshot into production Clinical
 * State. It only proves that the exact structured readiness payload was
 * actually applied by the synthetic repository used in engineering runtime.</p>
 */
public final class U05SyntheticClinicalReadinessSnapshotAdapter
        implements U05ClinicalReadinessSnapshotPort {

    private final SyntheticVersionedStateRepository repository;

    public U05SyntheticClinicalReadinessSnapshotAdapter(
            SyntheticVersionedStateRepository repository) {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        this.repository = repository;
    }

    @Override
    @SuppressWarnings("unchecked")
    public U05ClinicalReadinessSnapshot read(
            String cdpId,
            int expectedCommittedVersion) {
        if (cdpId == null || cdpId.trim().isEmpty()) {
            throw new IllegalArgumentException("cdpId is required");
        }

        SyntheticStateSnapshot snapshot = repository.snapshot(cdpId);
        if (snapshot.version() != expectedCommittedVersion) {
            throw new IllegalStateException("U05_READBACK_VERSION_MISMATCH");
        }

        Object patientState = snapshot.state().get("patient_state");
        if (!(patientState instanceof Map<?, ?>)) {
            throw new IllegalStateException("U05_READBACK_PATIENT_STATE_MISSING");
        }
        Object readiness =
                ((Map<String, Object>) patientState).get("clinical_readiness");
        if (!(readiness instanceof Map<?, ?>)) {
            throw new IllegalStateException("U05_READBACK_READINESS_MISSING");
        }

        return new U05ClinicalReadinessSnapshot(
                snapshot.version(),
                (Map<String, Object>) readiness);
    }
}
