package com.aidoctor.diagnosis.runtime.u05;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        this.readinessPayload = copyMap(readinessPayload);
    }

    public int getVersion() { return version; }

    public Map<String, Object> getReadinessPayload() {
        return copyMap(readinessPayload);
    }

    private static Map<String, Object> copyMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            result.put(entry.getKey(), copyValue(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?>) {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new IllegalArgumentException("readiness snapshot map key must be string");
                }
                result.put((String) entry.getKey(), copyValue(entry.getValue()));
            }
            return result;
        }
        if (value instanceof List<?>) {
            List<Object> result = new ArrayList<Object>();
            for (Object item : (List<Object>) value) result.add(copyValue(item));
            return result;
        }
        if (value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            return value;
        }
        throw new IllegalArgumentException("unsupported readiness snapshot value");
    }
}
