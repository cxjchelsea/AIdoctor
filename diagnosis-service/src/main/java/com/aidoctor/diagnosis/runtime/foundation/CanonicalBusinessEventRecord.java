package com.aidoctor.diagnosis.runtime.foundation;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** Durable canonical event identity. It records identity/correlation only, not business validity. */
@Entity
@Table(name = "canonical_business_event", uniqueConstraints = {
        @UniqueConstraint(name = "uk_canonical_event_idempotency", columnNames = "idempotency_key")
})
@Getter
@NoArgsConstructor
public class CanonicalBusinessEventRecord {
    @Id
    @Column(name = "event_id", length = 128, nullable = false)
    private String eventId;

    @Column(name = "consultation_id", length = 128, nullable = false)
    private String consultationId;

    @Column(name = "event_type", length = 64, nullable = false)
    private String eventType;

    @Column(name = "idempotency_key", length = 128, nullable = false)
    private String idempotencyKey;

    @Column(name = "payload_digest", length = 128, nullable = false)
    private String payloadDigest;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    public CanonicalBusinessEventRecord(String eventId, String consultationId, String eventType,
                                        String idempotencyKey, String payloadDigest, LocalDateTime receivedAt) {
        this.eventId = required(eventId, "eventId");
        this.consultationId = required(consultationId, "consultationId");
        this.eventType = required(eventType, "eventType");
        this.idempotencyKey = required(idempotencyKey, "idempotencyKey");
        this.payloadDigest = required(payloadDigest, "payloadDigest");
        this.receivedAt = receivedAt == null ? LocalDateTime.now() : receivedAt;
    }

    public boolean sameCanonicalInput(String consultationId, String eventType, String idempotencyKey, String payloadDigest) {
        return this.consultationId.equals(consultationId)
                && this.eventType.equals(eventType)
                && this.idempotencyKey.equals(idempotencyKey)
                && this.payloadDigest.equals(payloadDigest);
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
