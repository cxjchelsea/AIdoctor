package com.aidoctor.diagnosis.state.committer.ports;

/**
 * Synthetic capability identity / policy lookup. Not Capability activation.
 */
public interface CapabilityPolicyPort {

    CapabilityDecision evaluate(String capabilityId, String capabilityVersion);

    final class CapabilityDecision {
        public enum Status {
            AUTHORIZED,
            INVALID,
            DENIED
        }

        public final Status status;

        private CapabilityDecision(Status status) {
            this.status = status;
        }

        public static CapabilityDecision authorized() {
            return new CapabilityDecision(Status.AUTHORIZED);
        }

        public static CapabilityDecision invalid() {
            return new CapabilityDecision(Status.INVALID);
        }

        public static CapabilityDecision denied() {
            return new CapabilityDecision(Status.DENIED);
        }

        public boolean isAuthorized() {
            return status == Status.AUTHORIZED;
        }
    }
}
