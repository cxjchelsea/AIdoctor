package com.aidoctor.diagnosis.runtime.foundation;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/** Minimal execution record. It never stores or owns Clinical Truth. */
@Entity
@Table(name = "clinical_runtime_run")
@Getter
@NoArgsConstructor
public class ClinicalRunRecord {
    public static final String OPEN = "OPEN";
    public static final String CLOSED = "CLOSED";

    @Id
    @Column(name = "run_id", length = 128, nullable = false)
    private String runId;

    @Column(name = "thread_id", length = 128, nullable = false)
    private String threadId;

    @Column(name = "consultation_id", length = 128, nullable = false)
    private String consultationId;

    @Column(name = "event_id", length = 128, nullable = false)
    private String eventId;

    @Column(name = "based_on_clinical_state_version", nullable = false)
    private Integer basedOnClinicalStateVersion;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ClinicalRunRecord(String runId, String threadId, String consultationId, String eventId,
                             int basedOnClinicalStateVersion, LocalDateTime createdAt) {
        this.runId = required(runId, "runId");
        this.threadId = required(threadId, "threadId");
        this.consultationId = required(consultationId, "consultationId");
        this.eventId = required(eventId, "eventId");
        if (basedOnClinicalStateVersion < 0) throw new IllegalArgumentException("clinical state version must be non-negative");
        this.basedOnClinicalStateVersion = basedOnClinicalStateVersion;
        this.status = OPEN;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
