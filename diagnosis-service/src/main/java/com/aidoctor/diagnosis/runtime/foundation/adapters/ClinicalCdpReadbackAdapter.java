package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u05.U05ClinicalReadinessSnapshot;
import com.aidoctor.diagnosis.runtime.u05.U05ClinicalReadinessSnapshotPort;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Optional read-only, current-version CDP readback in the real Spring host.
 *
 * <p>Disabled unless explicitly selected. Does NOT install a U05 orchestration
 * service, authorize A1/F3, dispatch messages, or modify schema. It is never
 * an authority issuer and does not substitute for deployment attestation.</p>
 */
@Component
@ConditionalOnProperty(prefix = "aidoctor.u07.persistence",
        name = "readback-enabled", havingValue = "true", matchIfMissing = false)
public final class ClinicalCdpReadbackAdapter implements U05ClinicalReadinessSnapshotPort {
    private final CDPRepository repository;

    public ClinicalCdpReadbackAdapter(CDPRepository repository) {
        if (repository == null) throw new IllegalArgumentException("CDP repository required");
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public U05ClinicalReadinessSnapshot read(String cdpId, int expectedCommittedVersion) {
        if (cdpId == null || cdpId.trim().isEmpty() || expectedCommittedVersion < 0)
            throw new IllegalArgumentException("U07_READBACK_IDENTITY_OR_VERSION_INVALID");
        CDP record = repository.findById(cdpId)
                .orElseThrow(() -> new IllegalStateException("U07_READBACK_CDP_NOT_FOUND"));
        Integer current = record.getVersion();
        if (current == null || current.intValue() != expectedCommittedVersion)
            throw new IllegalStateException("U07_READBACK_VERSION_MISMATCH");
        Map<String, Object> patientState = record.getPatientState();
        if (patientState == null || !(patientState.get("clinical_readiness") instanceof Map<?, ?>))
            throw new IllegalStateException("U07_READBACK_READINESS_MISSING");
        Map<?, ?> raw = (Map<?, ?>) patientState.get("clinical_readiness");
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String))
                throw new IllegalStateException("U07_READBACK_READINESS_KEY_INVALID");
        }
        return new U05ClinicalReadinessSnapshot(current.intValue(),
                (Map<String, Object>) raw);
    }
}
