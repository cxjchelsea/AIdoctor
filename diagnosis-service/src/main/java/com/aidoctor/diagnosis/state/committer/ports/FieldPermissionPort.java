package com.aidoctor.diagnosis.state.committer.ports;

/**
 * Runtime field-permission check. Not JSON Schema path validation.
 */
public interface FieldPermissionPort {

    FieldPermissionDecision evaluate(String path, String capabilityId);

    final class FieldPermissionDecision {
        public final boolean authorized;

        private FieldPermissionDecision(boolean authorized) {
            this.authorized = authorized;
        }

        public static FieldPermissionDecision authorized() {
            return new FieldPermissionDecision(true);
        }

        public static FieldPermissionDecision denied() {
            return new FieldPermissionDecision(false);
        }
    }
}
