package com.aidoctor.diagnosis.runtime.u07;

import java.util.HashMap;
import java.util.Map;

/**
 * Non-durable synthetic event application state, deliberately separate from
 * canonical identity registration. Never produces clinical/business authority.
 *
 * RECEIVED != ACCEPTED != APPLIED. A synthetic APPLIED requires a matching
 * receipt and cannot be inferred from repeated registration or claimed flags.
 */
public final class U07SyntheticEventApplicationState {
    public enum Phase { RECEIVED, ACCEPTED, APPLIED, FAILED }
    public enum WriteStatus { CREATED, ADVANCED, IDEMPOTENT, CONFLICT, NOT_FOUND, INVALID }
    public enum Recovery { NOT_FOUND, NEEDS_RECONCILIATION, FAILED_REQUIRES_REVIEW,
                            APPLIED_SYNTHETIC_RECEIPT_PRESENT, CONFLICT }

    public static final class EventKey {
        public final String eventId, payloadDigest;
        public EventKey(String eventId, String payloadDigest) {
            this.eventId = eventId;
            this.payloadDigest = payloadDigest;
        }
    }
    public static final class Snapshot {
        public final Phase phase;
        public final String eventId, payloadDigest, syntheticReceiptRef;
        public final long revision;
        private Snapshot(Entry e) {
            this.phase = e.phase;
            this.eventId = e.eventId;
            this.payloadDigest = e.payloadDigest;
            this.syntheticReceiptRef = e.receipt;
            this.revision = e.revision;
        }
    }
    public static final class Write {
        public final WriteStatus status;
        public final Snapshot snapshot;
        private Write(WriteStatus status, Entry e) {
            this.status = status;
            this.snapshot = e == null ? null : new Snapshot(e);
        }
    }
    public static final class Review {
        public final Recovery recovery;
        public final Snapshot snapshot;
        private Review(Recovery recovery, Entry e) {
            this.recovery = recovery;
            this.snapshot = e == null ? null : new Snapshot(e);
        }
    }
    private static final class Entry {
        final String eventId, payloadDigest;
        Phase phase = Phase.RECEIVED;
        String receipt;
        long revision = 1;
        Entry(EventKey key) { eventId = key.eventId; payloadDigest = key.payloadDigest; }
    }

    private final Map<String, Entry> entries = new HashMap<String, Entry>();

    /** Call only after synthetic ledger reported FIRST_SEEN; not production proof. */
    public synchronized Write receive(EventKey key,
                                       U07SyntheticCanonicalEventLedger.Status registrationStatus) {
        if (!valid(key) || registrationStatus != U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN)
            return new Write(WriteStatus.INVALID, null);
        Entry old = entries.get(key.eventId);
        if (old != null) return new Write(old.payloadDigest.equals(key.payloadDigest)
                ? WriteStatus.IDEMPOTENT : WriteStatus.CONFLICT, old);
        Entry created = new Entry(key);
        entries.put(key.eventId, created);
        return new Write(WriteStatus.CREATED, created);
    }

    public synchronized Write accept(EventKey key, long expectedRevision) {
        Entry e = find(key);
        if (e == null) return new Write(WriteStatus.NOT_FOUND, null);
        if (!e.payloadDigest.equals(key.payloadDigest)) return new Write(WriteStatus.CONFLICT, e);
        if (e.revision != expectedRevision || e.phase != Phase.RECEIVED)
            return new Write(WriteStatus.CONFLICT, e);
        e.phase = Phase.ACCEPTED;
        e.revision++;
        return new Write(WriteStatus.ADVANCED, e);
    }

    /** Receipt is an explicit synthetic fixture, not a validated external effect. */
    public synchronized Write apply(EventKey key, long expectedRevision, String syntheticReceiptRef) {
        Entry e = find(key);
        if (e == null) return new Write(WriteStatus.NOT_FOUND, null);
        if (!e.payloadDigest.equals(key.payloadDigest) || blank(syntheticReceiptRef)
                || e.revision != expectedRevision || e.phase != Phase.ACCEPTED)
            return new Write(WriteStatus.CONFLICT, e);
        e.receipt = syntheticReceiptRef;
        e.phase = Phase.APPLIED;
        e.revision++;
        return new Write(WriteStatus.ADVANCED, e);
    }

    public synchronized Write fail(EventKey key, long expectedRevision) {
        Entry e = find(key);
        if (e == null) return new Write(WriteStatus.NOT_FOUND, null);
        if (!e.payloadDigest.equals(key.payloadDigest) || e.revision != expectedRevision
                || (e.phase != Phase.RECEIVED && e.phase != Phase.ACCEPTED))
            return new Write(WriteStatus.CONFLICT, e);
        e.phase = Phase.FAILED;
        e.revision++;
        return new Write(WriteStatus.ADVANCED, e);
    }

    /** Read-only: does not retry, mutate, resume or infer an effect. */
    public synchronized Review reconcile(EventKey key) {
        if (!valid(key)) return new Review(Recovery.CONFLICT, null);
        Entry e = entries.get(key.eventId);
        if (e == null) return new Review(Recovery.NOT_FOUND, null);
        if (!e.payloadDigest.equals(key.payloadDigest)) return new Review(Recovery.CONFLICT, e);
        if (e.phase == Phase.APPLIED)
            return new Review(blank(e.receipt) ? Recovery.CONFLICT
                    : Recovery.APPLIED_SYNTHETIC_RECEIPT_PRESENT, e);
        if (e.phase == Phase.FAILED) return new Review(Recovery.FAILED_REQUIRES_REVIEW, e);
        return new Review(Recovery.NEEDS_RECONCILIATION, e);
    }

    public synchronized int syntheticEntryCount() { return entries.size(); }

    private Entry find(EventKey key) {
        return valid(key) ? entries.get(key.eventId) : null;
    }
    private static boolean valid(EventKey key) {
        return key != null && !blank(key.eventId) && !blank(key.payloadDigest);
    }
    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }
}
