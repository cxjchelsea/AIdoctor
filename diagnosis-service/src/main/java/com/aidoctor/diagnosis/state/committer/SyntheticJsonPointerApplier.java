package com.aidoctor.diagnosis.state.committer;

import com.aidoctor.contracts.v1.StateTypes;

import java.util.List;
import java.util.Map;

/**
 * PBNC-02 internal synthetic JSON-pointer-compatible applier.
 *
 * <p>These engineering-only semantics are not Shared Contracts clinical
 * application semantics and never infer domain-specific structure.
 */
final class SyntheticJsonPointerApplier {

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
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return;
        }
        if (value instanceof List<?>) {
            List<?> values = (List<?>) value;
            for (Object item : values) {
                if (!(item == null || item instanceof String || item instanceof Number || item instanceof Boolean)) {
                    throw new SyntheticApplicationException("array value contains unsupported item");
                }
            }
            return;
        }
        throw new SyntheticApplicationException("value application is unsupported");
    }

    static final class SyntheticApplicationException extends RuntimeException {
        SyntheticApplicationException(String message) {
            super(message);
        }
    }
}
