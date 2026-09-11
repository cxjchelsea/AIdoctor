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

    public U01StartCommand(String eventId, String idempotencyKey, String userId,
                           String subjectType, String subjectReferenceId,
                           String problemText, String scopeIntentCandidate,
                           boolean earlySafetySignalPresent) {
        this.eventId = required(eventId, "eventId");
        this.idempotencyKey = required(idempotencyKey, "idempotencyKey");
        this.userId = required(userId, "userId");
        this.subjectType = subjectType;
        this.subjectReferenceId = subjectReferenceId;
        this.problemText = problemText;
        this.scopeIntentCandidate = scopeIntentCandidate;
        this.earlySafetySignalPresent = earlySafetySignalPresent;
    }

    public String getEventId() { return eventId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getUserId() { return userId; }
    public String getSubjectType() { return subjectType; }
    public String getSubjectReferenceId() { return subjectReferenceId; }
    public String getProblemText() { return problemText; }
    public String getScopeIntentCandidate() { return scopeIntentCandidate; }
    public boolean isEarlySafetySignalPresent() { return earlySafetySignalPresent; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
