package com.aidoctor.diagnosis.state.committer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal-only synthetic state snapshot. This is not Shared Contracts,
 * Clinical State, CDP, Encounter, checkpoint, or production DTO.
 */
public final class SyntheticStateSnapshot {
    private final int version;
    private final Map<String, Object> state;

    public SyntheticStateSnapshot(int version, Map<String, Object> state) {
        this.version = version;
        this.state = immutableMap(copyMap(state));
    }

    public int version() {
        return version;
    }

    public Map<String, Object> state() {
        return copyMap(state);
    }

    static Map<String, Object> copyMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (source == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            result.put(entry.getKey(), copyValue(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static Object copyValue(Object value) {
        if (value instanceof Map<?, ?>) {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new IllegalArgumentException("synthetic state map keys must be strings");
                }
                result.put((String) entry.getKey(), copyValue(entry.getValue()));
            }
            return result;
        }
        if (value instanceof List<?>) {
            List<Object> result = new ArrayList<Object>();
            for (Object item : (List<Object>) value) {
                result.add(copyValue(item));
            }
            return result;
        }
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        throw new IllegalArgumentException("unsupported synthetic state value");
    }

    private static Map<String, Object> immutableMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            result.put(entry.getKey(), immutableValue(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    @SuppressWarnings("unchecked")
    private static Object immutableValue(Object value) {
        if (value instanceof Map<?, ?>) {
            return immutableMap((Map<String, Object>) value);
        }
        if (value instanceof List<?>) {
            List<Object> result = new ArrayList<Object>();
            for (Object item : (List<Object>) value) {
                result.add(immutableValue(item));
            }
            return Collections.unmodifiableList(result);
        }
        return value;
    }
}
