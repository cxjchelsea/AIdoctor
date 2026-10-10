package com.aidoctor.diagnosis.runtime.u07;

/** Immutable synthetic input. Validation occurs BEFORE opening a connection. */
public final class U07SyntheticInput {
    public final String eventId, consultationId, questionId, waitEffectId, idempotencyKey, digest;
    public final long expectedConsultationVersion;
    public U07SyntheticInput(String eventId, String consultationId, String questionId,
                             String waitEffectId, String idempotencyKey, String digest, long version) {
        this.eventId = eventId;
        this.consultationId = consultationId;
        this.questionId = questionId;
        this.waitEffectId = waitEffectId;
        this.idempotencyKey = idempotencyKey;
        this.digest = digest;
        this.expectedConsultationVersion = version;
    }
}
