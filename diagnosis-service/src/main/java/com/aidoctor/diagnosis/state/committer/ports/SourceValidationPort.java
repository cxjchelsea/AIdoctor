package com.aidoctor.diagnosis.state.committer.ports;

/**
 * Runtime source authorization. Schema-valid source values may still be denied.
 */
public interface SourceValidationPort {

    SourceDecision evaluate(String source);

    final class SourceDecision {
        public final boolean authorized;

        private SourceDecision(boolean authorized) {
            this.authorized = authorized;
        }

        public static SourceDecision authorized() {
            return new SourceDecision(true);
        }

        public static SourceDecision denied() {
            return new SourceDecision(false);
        }
    }
}
