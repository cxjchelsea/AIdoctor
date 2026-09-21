package com.aidoctor.diagnosis.runtime.u05;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit non-production in-memory admission ledger. No Spring/production activation. */
public final class U05InMemoryAdmissionLedger implements U05AdmissionLedger {
    private final Map<String, Entry> entries = new LinkedHashMap<String, Entry>();

    @Override
    public synchronized Entry find(String admissionId) {
        return entries.get(admissionId);
    }

    @Override
    public synchronized void store(String admissionId, String fingerprint, U05AdmittedInput input) {
        Entry existing = entries.get(admissionId);
        if (existing != null && !existing.getFingerprint().equals(fingerprint)) {
            throw new IllegalStateException("U05 admission replay conflict");
        }
        if (existing == null) entries.put(admissionId, new Entry(fingerprint, input));
    }
}
