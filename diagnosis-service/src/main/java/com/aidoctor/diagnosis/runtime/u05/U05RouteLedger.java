package com.aidoctor.diagnosis.runtime.u05;

/** U05-owned route replay ledger. It never invokes downstream Units. */
public interface U05RouteLedger {
    Entry find(String routingDecisionId);
    void store(String routingDecisionId, String fingerprint, U05DownstreamRoutingDecision decision);

    final class Entry {
        private final String fingerprint;
        private final U05DownstreamRoutingDecision decision;

        public Entry(String fingerprint, U05DownstreamRoutingDecision decision) {
            this.fingerprint = fingerprint;
            this.decision = decision;
        }

        public String getFingerprint() { return fingerprint; }
        public U05DownstreamRoutingDecision getDecision() { return decision; }
    }
}
