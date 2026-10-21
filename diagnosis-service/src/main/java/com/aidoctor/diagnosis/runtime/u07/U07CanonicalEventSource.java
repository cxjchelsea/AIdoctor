package com.aidoctor.diagnosis.runtime.u07;

/**
 * Contract for a future authenticated source of canonical business events.
 * No production implementation is provided by this slice. An arbitrary caller
 * must not be permitted to self-issue VERIFIED source evidence.
 */
public interface U07CanonicalEventSource {
    enum Provenance { SYNTHETIC_TEST_ONLY, UNVERIFIED, AUTHENTICATED_SOURCE }
    final class Snapshot {
        public final String sourceVersion;
        public final Provenance provenance;
        public final U07SyntheticCanonicalEventLedger.Event event;
        public Snapshot(String sourceVersion, Provenance provenance,
                        U07SyntheticCanonicalEventLedger.Event event) {
            this.sourceVersion = sourceVersion;
            this.provenance = provenance;
            this.event = event;
        }
    }
    Snapshot resolve(String eventId);
}
