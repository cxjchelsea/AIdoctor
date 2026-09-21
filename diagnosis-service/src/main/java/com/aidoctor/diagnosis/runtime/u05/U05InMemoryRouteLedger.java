package com.aidoctor.diagnosis.runtime.u05;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit non-production route ledger. No Scheduler or production wiring. */
public final class U05InMemoryRouteLedger implements U05RouteLedger {
    private final Map<String, Entry> entries = new LinkedHashMap<String, Entry>();

    @Override
    public synchronized Entry find(String routingDecisionId) {
        return entries.get(routingDecisionId);
    }

    @Override
    public synchronized void store(
            String routingDecisionId,
            String fingerprint,
            U05DownstreamRoutingDecision decision) {
        Entry existing = entries.get(routingDecisionId);
        if (existing != null && !existing.getFingerprint().equals(fingerprint)) {
            throw new IllegalStateException("U05_ROUTE_REPLAY_CONFLICT");
        }
        if (existing == null) entries.put(routingDecisionId, new Entry(fingerprint, decision));
    }
}
