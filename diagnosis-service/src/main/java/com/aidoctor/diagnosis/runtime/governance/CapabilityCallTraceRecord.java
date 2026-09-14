package com.aidoctor.diagnosis.runtime.governance;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Minimal P05 correlation record for a governed capability call.
 * This is audit/trace metadata and must never be used as Clinical State.
 */
@Entity
@Table(name = "clinical_capability_call_trace")
@Getter
@NoArgsConstructor
public class CapabilityCallTraceRecord {
    public static final String STARTED = "STARTED";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";

    @Id
    @Column(name = "capability_call_id", length = 128, nullable = false)
    private String capabilityCallId;

    @Column(name = "consultation_id", length = 128, nullable = false)
    private String consultationId;

    @Column(name = "thread_id", length = 128, nullable = false)
    private String threadId;

    @Column(name = "run_id", length = 128, nullable = false)
    private String runId;

    @Column(name = "event_id", length = 128, nullable = false)
    private String eventId;

    @Column(name = "unit_id", length = 32, nullable = false)
    private String unitId;

    @Column(name = "capability_id", length = 64, nullable = false)
    private String capabilityId;

    @Column(name = "binding_id", length = 128, nullable = false)
    private String bindingId;

    @Column(name = "capability_result_ref", length = 128)
    private String capabilityResultRef;

    @Column(name = "decision_ref", length = 128)
    private String decisionRef;

    @Column(name = "proposal_ref", length = 128)
    private String proposalRef;

    @Column(name = "commit_ref", length = 128)
    private String commitRef;

    @Column(name = "clinical_state_version_before")
    private Integer clinicalStateVersionBefore;

    @Column(name = "clinical_state_version_after")
    private Integer clinicalStateVersionAfter;

    @Column(name = "call_status", length = 32, nullable = false)
    private String callStatus;

    @Column(name = "reason_code", length = 128)
    private String reasonCode;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public CapabilityCallTraceRecord(
            String capabilityCallId,
            String consultationId,
            String threadId,
            String runId,
            String eventId,
            String unitId,
            String capabilityId,
            String bindingId,
            Integer clinicalStateVersionBefore,
            LocalDateTime startedAt
    ) {
        this.capabilityCallId = required(capabilityCallId, "capabilityCallId");
        this.consultationId = required(consultationId, "consultationId");
        this.threadId = required(threadId, "threadId");
        this.runId = required(runId, "runId");
        this.eventId = required(eventId, "eventId");
        this.unitId = required(unitId, "unitId");
        this.capabilityId = required(capabilityId, "capabilityId");
        this.bindingId = required(bindingId, "bindingId");
        this.clinicalStateVersionBefore = clinicalStateVersionBefore;
        this.callStatus = STARTED;
        this.startedAt = startedAt == null ? LocalDateTime.now() : startedAt;
    }

    public void succeed(
            String capabilityResultRef,
            String decisionRef,
            String proposalRef,
            String commitRef,
            Integer clinicalStateVersionAfter,
            LocalDateTime finishedAt
    ) {
        requireStarted();
        this.capabilityResultRef = required(capabilityResultRef, "capabilityResultRef");
        this.decisionRef = decisionRef;
        this.proposalRef = proposalRef;
        this.commitRef = commitRef;
        this.clinicalStateVersionAfter = clinicalStateVersionAfter;
        this.callStatus = SUCCEEDED;
        this.finishedAt = finishedAt == null ? LocalDateTime.now() : finishedAt;
    }

    public void fail(String reasonCode, LocalDateTime finishedAt) {
        requireStarted();
        this.reasonCode = required(reasonCode, "reasonCode");
        this.callStatus = FAILED;
        this.finishedAt = finishedAt == null ? LocalDateTime.now() : finishedAt;
    }

    private void requireStarted() {
        if (!STARTED.equals(callStatus)) {
            throw new IllegalStateException("Capability call trace is already terminal: " + capabilityCallId);
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
