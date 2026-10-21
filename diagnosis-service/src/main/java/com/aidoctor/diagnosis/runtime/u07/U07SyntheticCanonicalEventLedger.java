package com.aidoctor.diagnosis.runtime.u07;

import java.util.HashMap;
import java.util.Map;

/**
 * Volatile, synthetic-only canonical event identity ledger.
 *
 * Never a durable/production idempotency service. It does not perform event
 * authentication, F8 admission, side effects, patient state or runtime resume.
 */
public final class U07SyntheticCanonicalEventLedger {
    public enum EventType { USER_ANSWER, RESUME_REQUEST }
    public enum Status { FIRST_SEEN, SAME_EVENT_REPLAY, PROTECTED_IDENTITY_CONFLICT, SOURCE_UNVERIFIED, INVALID_EVENT }

    public static final class Event {
        public final String eventId, consultationId, questionId, waitEffectId, payloadDigest;
        public final EventType eventType;
        public Event(String eventId, String consultationId, String questionId,
                     String waitEffectId, String payloadDigest, EventType eventType) {
            this.eventId = eventId;
            this.consultationId = consultationId;
            this.questionId = questionId;
            this.waitEffectId = waitEffectId;
            this.payloadDigest = payloadDigest;
            this.eventType = eventType;
        }
    }

    public static final class Result {
        public final Status status;
        public final String eventId;
        private Result(Status status, String eventId) {
            this.status = status;
            this.eventId = eventId;
        }
    }

    private final Map<String, Event> entries = new HashMap<String, Event>();

    /**
     * Atomic only inside one in-memory Java object, NOT durable or multi-host.
     * This method records identity, not an ACCEPTED/APPLIED business effect.
     */
    public synchronized Result register(U07CanonicalEventSource.Snapshot snapshot) {
        if (snapshot == null || snapshot.provenance != U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY
                || blank(snapshot.sourceVersion)) {
            return new Result(Status.SOURCE_UNVERIFIED, null);
        }
        Event event = snapshot.event;
        if (event == null || blank(event.eventId) || blank(event.consultationId)
                || blank(event.questionId) || blank(event.waitEffectId)
                || blank(event.payloadDigest) || event.eventType == null) {
            return new Result(Status.INVALID_EVENT, null);
        }
        Event existing = entries.get(event.eventId);
        if (existing == null) {
            entries.put(event.eventId, event);
            return new Result(Status.FIRST_SEEN, event.eventId);
        }
        return new Result(same(existing, event) ? Status.SAME_EVENT_REPLAY
                : Status.PROTECTED_IDENTITY_CONFLICT, event.eventId);
    }

    public synchronized int syntheticEntryCount() {
        return entries.size();
    }

    private static boolean same(Event a, Event b) {
        return a.eventType == b.eventType
                && a.consultationId.equals(b.consultationId)
                && a.questionId.equals(b.questionId)
                && a.waitEffectId.equals(b.waitEffectId)
                && a.payloadDigest.equals(b.payloadDigest);
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
