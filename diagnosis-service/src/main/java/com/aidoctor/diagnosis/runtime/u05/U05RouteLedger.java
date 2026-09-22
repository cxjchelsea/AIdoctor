package com.aidoctor.diagnosis.runtime.u05;

/** U05-owned route replay ledger. It never invokes downstream Units. */
public interface U05RouteLedger {

    Entry reconcile(
            String routingDecisionId,
            String fingerprint,
            U05DownstreamRoutingDecision candidate);

    final class Entry {
        private final String fingerprint;
        private final U05DownstreamRoutingDecision decision;
        private final boolean reattached;

        public Entry(
                String fingerprint,
                U05DownstreamRoutingDecision decision,
                boolean reattached) {
            if (fingerprint == null || fingerprint.trim().isEmpty()) {
                throw new IllegalArgumentException("fingerprint is required");
            }
            if (decision == null) {
                throw new IllegalArgumentException("decision is required");
            }
            this.fingerprint = fingerprint;
            this.decision = decision;
            this.reattached = reattached;
        }

        public String getFingerprint() { return fingerprint; }
        public U05DownstreamRoutingDecision getDecision() { return decision; }
        public boolean isReattached() { return reattached; }
    }
}
