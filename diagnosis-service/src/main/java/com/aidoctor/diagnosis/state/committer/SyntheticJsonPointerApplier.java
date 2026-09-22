package com.aidoctor.diagnosis.state.committer;

import com.aidoctor.contracts.v1.StateTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * PBNC-02 internal synthetic JSON-pointer-compatible applier.
 *
 * <p>These engineering-only semantics are not Shared Contracts clinical
 * application semantics and never infer domain-specific structure.
 *
 * <p>PBNC-02A keeps the executable synthetic value boundary aligned with the
 * already-authorized StatePatch controlled-value boundary. It intentionally
 * supports exactly one structured object level and rejects nested structures.
 */
final class SyntheticJsonPointerApplier {
    private static final Pattern OPAQUE_ID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]*$");
    private static final BigDecimal MAX_SAFE_INTEGER = new BigDecimal("9007199254740991");
    private static final int TOP_LEVEL_STRING_LIMIT = 4000;
    private static final int STRUCTURED_STRING_LIMIT = 1000;
    private static final int MAX_LIST_SIZE = 64;
    private static final int MAX_MAP_SIZE = 32;
    private static final int MAX_MAP_KEY_LENGTH = 64;

    void apply(StateTypes.StatePatchOperation operation, Map<String, Object> working) {
        if (operation == null || operation.path == null || operation.op == null) {
            throw new SyntheticApplicationException("operation is malformed");
        }
        if (operation.expectedCurrentValue != null) {
            throw new SyntheticApplicationException("expected_current_value application is unsupported");
        }
        if (("ADD".equals(operation.op) || "REPLACE".equals(operation.op)) && operation.value == null) {
            throw new SyntheticApplicationException("null value application is unsupported");
        }
        if ("TEST".equals(operation.op)) {
            throw new SyntheticApplicationException("TEST application is unsupported");
        }
        if (!"ADD".equals(operation.op) && !"REPLACE".equals(operation.op) && !"REMOVE".equals(operation.op)) {
            throw new SyntheticApplicationException("operation semantics are unsupported");
        }
        assertSupportedValue(operation.value);

        String[] tokens = decode(operation.path);
        if (tokens.length < 2) {
            throw new SyntheticApplicationException("path must include root and leaf");
        }
        Map<String, Object> parent = parent(tokens, working);
        String leaf = tokens[tokens.length - 1];
        boolean exists = parent.containsKey(leaf);

        if ("ADD".equals(operation.op)) {
            if (exists) {
                throw new SyntheticApplicationException("ADD target already exists");
            }
            parent.put(leaf, SyntheticStateSnapshot.copyValue(operation.value));
            return;
        }
        if ("REPLACE".equals(operation.op)) {
            if (!exists) {
                throw new SyntheticApplicationException("REPLACE target is missing");
            }
            parent.put(leaf, SyntheticStateSnapshot.copyValue(operation.value));
            return;
        }
        if (!exists) {
            throw new SyntheticApplicationException("REMOVE target is missing");
        }
        parent.remove(leaf);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parent(String[] tokens, Map<String, Object> working) {
        Object current = working;
        for (int index = 0; index < tokens.length - 1; index++) {
            if (!(current instanceof Map<?, ?>)) {
                throw new SyntheticApplicationException("array element paths are unsupported");
            }
            Map<String, Object> map = (Map<String, Object>) current;
            String token = tokens[index];
            if (!map.containsKey(token)) {
                throw new SyntheticApplicationException("parent path is missing");
            }
            current = map.get(token);
        }
        if (!(current instanceof Map<?, ?>)) {
            throw new SyntheticApplicationException("parent path is not an object");
        }
        return (Map<String, Object>) current;
    }

    private static String[] decode(String pointer) {
        if (!pointer.startsWith("/")) {
            throw new SyntheticApplicationException("path is not a JSON pointer");
        }
        String[] raw = pointer.substring(1).split("/", -1);
        for (int index = 0; index < raw.length; index++) {
            raw[index] = raw[index].replace("~1", "/").replace("~0", "~");
        }
        return raw;
    }

    private static void assertSupportedValue(Object value) {
        if (!controlledValue(value, TOP_LEVEL_STRING_LIMIT, 0)) {
            throw new SyntheticApplicationException(
                    "value application is outside the StatePatch controlled-value boundary");
        }
    }

    private static boolean controlledValue(Object value, int stringLimit, int depth) {
        if (value == null || value instanceof Boolean) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).length() <= stringLimit;
        }
        if (value instanceof Number) {
            try {
                BigDecimal number = new BigDecimal(value.toString());
                return number.abs().compareTo(MAX_SAFE_INTEGER) <= 0;
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        if (value instanceof List<?>) {
            List<?> values = (List<?>) value;
            if (values.size() > MAX_LIST_SIZE) {
                return false;
            }
            for (Object item : values) {
                if (item instanceof List<?>
                        || item instanceof Map<?, ?>
                        || !controlledValue(item, STRUCTURED_STRING_LIMIT, depth + 1)) {
                    return false;
                }
            }
            return true;
        }
        if (value instanceof Map<?, ?>) {
            if (depth > 0) {
                return false;
            }
            Map<?, ?> values = (Map<?, ?>) value;
            if (values.isEmpty() || values.size() > MAX_MAP_SIZE) {
                return false;
            }
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    return false;
                }
                String key = (String) entry.getKey();
                if (key.length() < 1
                        || key.length() > MAX_MAP_KEY_LENGTH
                        || !OPAQUE_ID.matcher(key).matches()) {
                    return false;
                }
                Object child = entry.getValue();
                if (child instanceof Map<?, ?>
                        || !controlledValue(child, STRUCTURED_STRING_LIMIT, depth + 1)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    static final class SyntheticApplicationException extends RuntimeException {
        SyntheticApplicationException(String message) {
            super(message);
        }
    }
}
