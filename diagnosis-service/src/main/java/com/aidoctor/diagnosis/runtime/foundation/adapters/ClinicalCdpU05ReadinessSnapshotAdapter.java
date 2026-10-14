package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u05.U05ClinicalReadinessSnapshot;
import com.aidoctor.diagnosis.runtime.u05.U05ClinicalReadinessSnapshotPort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exact physical U05 readiness readback. This adapter neither issues U05
 * admission/route authority nor trusts a synthetic snapshot or commit claim.
 */
@Component
@Profile("!prod & !production")
@ConditionalOnProperty(prefix="aidoctor.v1.clinical-e2e",
        name="physical-followup-enabled", havingValue="true", matchIfMissing=false)
public class ClinicalCdpU05ReadinessSnapshotAdapter
        implements U05ClinicalReadinessSnapshotPort {
    private final CDPRepository repository;

    public ClinicalCdpU05ReadinessSnapshotAdapter(CDPRepository repository) {
        if (repository == null) throw new IllegalArgumentException("CDP repository required");
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly=true)
    public U05ClinicalReadinessSnapshot read(String cdpId, int expectedCommittedVersion) {
        if (cdpId == null || cdpId.trim().isEmpty() || expectedCommittedVersion < 0)
            throw new IllegalArgumentException("U05_READBACK_IDENTITY_INVALID");
        CDP cdp = repository.findById(cdpId)
                .orElseThrow(() -> blocked("CDP_MISSING"));
        if (cdp.getVersion() == null
                || cdp.getVersion().intValue() != expectedCommittedVersion)
            throw blocked("COMMITTED_VERSION_STALE");
        Map<String, Object> state = cdp.getPatientState();
        if (state == null || !(state.get("clinical_readiness") instanceof Map<?, ?>))
            throw blocked("READINESS_PAYLOAD_MISSING");
        Map<?, ?> stored = (Map<?, ?>) state.get("clinical_readiness");
        Map<String, Object> structured = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : stored.entrySet()) {
            if (!(entry.getKey() instanceof String))
                throw blocked("READINESS_FIELD_INVALID");
            structured.put((String) entry.getKey(), entry.getValue());
        }
        // The existing U05 contract validates the full expected payload,
        // provenance, audit and version against this physical snapshot.
        return new U05ClinicalReadinessSnapshot(expectedCommittedVersion, structured);
    }

    private static IllegalStateException blocked(String reason) {
        return new IllegalStateException("U05_PHYSICAL_READBACK_BLOCKED:" + reason);
    }
}
