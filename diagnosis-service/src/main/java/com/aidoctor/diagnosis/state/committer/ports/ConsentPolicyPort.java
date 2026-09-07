package com.aidoctor.diagnosis.state.committer.ports;

/**
 * Synthetic consent lookup. Missing / denied / invalid are policy rejections.
 */
public interface ConsentPolicyPort {

    ConsentDecision evaluate(String cdpId, String capabilityId);

    final class ConsentDecision {
        public enum Status {
            AUTHORIZED,
            MISSING,
            DENIED,
            INVALID
        }

        public final Status status;

        private ConsentDecision(Status status) {
            this.status = status;
        }

        public static ConsentDecision authorized() {
            return new ConsentDecision(Status.AUTHORIZED);
        }

        public static ConsentDecision missing() {
            return new ConsentDecision(Status.MISSING);
        }

        public static ConsentDecision denied() {
            return new ConsentDecision(Status.DENIED);
        }

        public static ConsentDecision invalid() {
            return new ConsentDecision(Status.INVALID);
        }

        public boolean isAuthorized() {
            return status == Status.AUTHORIZED;
        }
    }
}
