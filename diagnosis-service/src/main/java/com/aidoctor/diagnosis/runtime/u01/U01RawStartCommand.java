package com.aidoctor.diagnosis.runtime.u01;

/**
 * Raw natural-language start input for the governed U01+C01 internal chain.
 * This is not the external HTTP contract and does not imply Phase 10 wiring.
 */
public class U01RawStartCommand {
    private final String eventId;
    private final String idempotencyKey;
    private final String userId;
    private final String rawText;
    private final String knownSubjectReferenceId;

    public U01RawStartCommand(
            String eventId,
            String idempotencyKey,
            String userId,
            String rawText,
            String knownSubjectReferenceId) {
        this.eventId = required(eventId, "eventId");
        this.idempotencyKey = required(idempotencyKey, "idempotencyKey");
        this.userId = required(userId, "userId");
        this.rawText = rawText == null ? "" : rawText.trim();
        this.knownSubjectReferenceId = trimToNull(knownSubjectReferenceId);
    }

    public String getEventId() { return eventId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getUserId() { return userId; }
    public String getRawText() { return rawText; }
    public String getKnownSubjectReferenceId() { return knownSubjectReferenceId; }

    private static String required(String value, String name) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}
