package com.aidoctor.diagnosis.runtime.u05;

import java.util.LinkedHashMap;
import java.util.Map;

/** U05-owned exact synthetic read-back view over /patient_state/clinical_readiness. */
public final class U05ClinicalReadinessSnapshot {
    private final int version;
    private final Map<String, Object> readinessPayload;

    public U05ClinicalReadinessSnapshot(
            int version,
            Map<String, Object> readinessPayload) {
        if (version < 0) throw new IllegalArgumentException("version must be non-negative");
        if (readinessPayload == null || readinessPayload.isEmpty()) {
            throw new IllegalArgumentException("readinessPayload is required");
        }
        this.version = version;
        this.readinessPayload = new LinkedHashMap<String, Object>(readinessPayload);
    }

    public int getVersion() { return version; }

    public Map<String, Object> getReadinessPayload() {
        return new LinkedHashMap<String, Object>(readinessPayload);
    }
}
