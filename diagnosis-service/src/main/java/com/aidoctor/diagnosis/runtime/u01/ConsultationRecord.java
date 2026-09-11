package com.aidoctor.diagnosis.runtime.u01;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.LocalDateTime;

/** Authoritative U01 consultation lifecycle/framing state. This is not Runtime state. */
@Entity
@Table(name = "clinical_consultation")
@Getter
@NoArgsConstructor
public class ConsultationRecord {
    public static final String ACTIVE = "ACTIVE";

    @Id
    @Column(name = "consultation_id", length = 128, nullable = false)
    private String consultationId;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    @Column(name = "cdp_id", length = 128, nullable = false, unique = true)
    private String cdpId;

    @Column(name = "user_id", length = 128, nullable = false)
    private String userId;

    @Column(name = "lifecycle_status", length = 32, nullable = false)
    private String lifecycleStatus;

    @Column(name = "subject_status", length = 32, nullable = false)
    private String subjectStatus;

    @Column(name = "subject_type", length = 32)
    private String subjectType;

    @Column(name = "subject_reference_id", length = 128)
    private String subjectReferenceId;

    @Column(name = "problem_status", length = 32, nullable = false)
    private String problemStatus;

    @Column(name = "problem_text", length = 2000)
    private String problemText;

    @Column(name = "scope_decision", length = 32, nullable = false)
    private String scopeDecision;

    @Column(name = "clarification_reason", length = 128)
    private String clarificationReason;

    @Column(name = "early_safety_signal", nullable = false)
    private boolean earlySafetySignal;

    @Column(name = "start_event_id", length = 128, nullable = false, unique = true)
    private String startEventId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ConsultationRecord(String consultationId, String cdpId, String userId,
                              U01StartCommand command, U01SemanticDecision decision,
                              LocalDateTime createdAt) {
        this.consultationId = required(consultationId, "consultationId");
        this.rowVersion = 0L;
        this.cdpId = required(cdpId, "cdpId");
        this.userId = required(userId, "userId");
        this.lifecycleStatus = ACTIVE;
        this.subjectStatus = required(decision.getSubjectStatus(), "subjectStatus");
        this.subjectType = decision.getNormalizedSubjectType();
        this.subjectReferenceId = trimToNull(command.getSubjectReferenceId());
        this.problemStatus = required(decision.getProblemStatus(), "problemStatus");
        this.problemText = trimToNull(command.getProblemText());
        this.scopeDecision = required(decision.getScopeDecision(), "scopeDecision");
        this.clarificationReason = decision.getClarificationReason();
        this.earlySafetySignal = decision.isEarlySafetySignalPresent();
        this.startEventId = required(command.getEventId(), "startEventId");
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    private static String required(String value, String name) {
        String normalized = trimToNull(value);
        if (normalized == null) throw new IllegalArgumentException(name + " is required");
        return normalized;
    }
}
