package com.aidoctor.diagnosis.runtime.u01;

/**
 * Internal U01 command. This is intentionally not the legacy DiagnosisRequest.
 * Subject and scope semantics must be explicit; U01 never guesses them.
 */
public class U01StartCommand {
    private final String eventId;
    private final String idempotencyKey;
    private final String userId;
    private final String subjectType;
    private final String subjectReferenceId;
    private final String problemText;
    private final String scopeIntentCandidate;
    private final boolean earlySafetySignalPresent;
    /**
     * Optional fingerprint of the original source payload. When present, canonical
     * event identity is based on this source fingerprint instead of derived C01
     * candidates so a replay stays stable across capability implementation changes.
     */
    private final String sourceInputFingerprint;

    public U01StartCommand(String eventId, String idempotencyKey, String userId,
                           String subjectType, String subjectReferenceId,
                           String problemText, String scopeIntentCandidate,
                           boolean earlySafetySignalPresent) {
        this(eventId, idempotencyKey, userId, subjectType, subjectReferenceId,
                problemText, scopeIntentCandidate, earlySafetySignalPresent, null);
    }

    public U01StartCommand(String eventId, String idempotencyKey, String userId,
                           String subjectType, String subjectReferenceId,
                           String problemText, String scopeIntentCandidate,
                           boolean earlySafetySignalPresent,
                           String sourceInputFingerprint) {
        this.eventId = required(eventId, "eventId");
        this.idempotencyKey = required(idempotencyKey, "idempotencyKey");
        this.userId = required(userId, "userId");
        this.subjectType = subjectType;
        this.subjectReferenceId = subjectReferenceId;
        this.problemText = problemText;
        this.scopeIntentCandidate = scopeIntentCandidate;
        this.earlySafetySignalPresent = earlySafetySignalPresent;
        this.sourceInputFingerprint = trimToNull(sourceInputFingerprint);
    }

    public String getEventId() { return eventId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getUserId() { return userId; }
    public String getSubjectType() { return subjectType; }
    public String getSubjectReferenceId() { return subjectReferenceId; }
    public String getProblemText() { return problemText; }
    public String getScopeIntentCandidate() { return scopeIntentCandidate; }
    public boolean isEarlySafetySignalPresent() { return earlySafetySignalPresent; }
    public String getSourceInputFingerprint() { return sourceInputFingerprint; }

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
