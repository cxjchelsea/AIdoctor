package com.aidoctor.diagnosis.runtime.u05;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit non-production in-memory admission ledger. No Spring/production activation. */
public final class U05InMemoryAdmissionLedger implements U05AdmissionLedger {
    private final Map<String, Entry> entries = new LinkedHashMap<String, Entry>();

    @Override
    public synchronized Entry reconcile(
            String admissionId,
            String fingerprint,
            U05AdmittedInput candidate) {
        Entry existing = entries.get(admissionId);
        if (existing != null) {
            if (!existing.getFingerprint().equals(fingerprint)) {
                throw new IllegalStateException("U05_ADMISSION_REPLAY_CONFLICT");
            }
            return new Entry(
                    existing.getFingerprint(),
                    existing.getAdmittedInput(),
                    true);
        }
        Entry created = new Entry(fingerprint, candidate, false);
        entries.put(admissionId, created);
        return created;
    }
}
