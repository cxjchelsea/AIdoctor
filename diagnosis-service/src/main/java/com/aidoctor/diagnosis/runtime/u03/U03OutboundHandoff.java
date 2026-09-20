package com.aidoctor.diagnosis.runtime.u03;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Typed S14 producer-side boundary for a future U04 consumer.
 *
 * <p>This is not a U04 request, route, safety decision, or authorization. It only
 * preserves governed U03 execution/result facts and provenance. No field in this
 * type may be interpreted as a U04 PASS, SAFE, NORMAL, or continue decision.</p>
 */
public final class U03OutboundHandoff {
    private final String consultationId;
    private final String cdpId;
    private final int sourceClinicalStateVersion;
    private final Integer committedClinicalStateVersion;
    private final String threadId;
    private final String runId;
    private final String eventId;
    private final String correlationId;
    private final String traceId;
    private final String environmentId;
    private final String bindingMode;

    private final String executionStatus;
    private final String executionFailureReasonCode;
    private final List<String> executionLimitations;
    private final String executionUncertainty;

    private final String decisionId;
    private final String decisionStatus;
    private final String dispositionCode;
    private final String decisionReasonCode;

    private final String capabilityBindingId;
    private final List<String> governedReleaseRefs;

    private final String acceptanceRef;
    private final List<String> acceptedEvidenceRefs;
    private final List<String> acceptedSourceRefs;
    private final List<String> acceptedProvenanceRefs;

    private final String proposalId;
    private final String commitStatus;
    private final String commitReasonCode;
    private final String commitAuditId;

    U03OutboundHandoff(
            String consultationId,
            String cdpId,
            int sourceClinicalStateVersion,
            Integer committedClinicalStateVersion,
            String threadId,
            String runId,
            String eventId,
            String correlationId,
            String traceId,
            String environmentId,
            String bindingMode,
            String executionStatus,
            String executionFailureReasonCode,
            List<String> executionLimitations,
            String executionUncertainty,
            String decisionId,
            String decisionStatus,
            String dispositionCode,
            String decisionReasonCode,
            String capabilityBindingId,
            List<String> governedReleaseRefs,
            String acceptanceRef,
            List<String> acceptedEvidenceRefs,
            List<String> acceptedSourceRefs,
            List<String> acceptedProvenanceRefs,
            String proposalId,
            String commitStatus,
            String commitReasonCode,
            String commitAuditId) {
        this.consultationId = required(consultationId, "consultationId");
        this.cdpId = required(cdpId, "cdpId");
        if (sourceClinicalStateVersion < 0) {
            throw new IllegalArgumentException("sourceClinicalStateVersion must be non-negative");
        }
        if (committedClinicalStateVersion != null && committedClinicalStateVersion.intValue() < 0) {
            throw new IllegalArgumentException("committedClinicalStateVersion must be non-negative");
        }
        this.sourceClinicalStateVersion = sourceClinicalStateVersion;
        this.committedClinicalStateVersion = committedClinicalStateVersion;
        this.threadId = required(threadId, "threadId");
        this.runId = required(runId, "runId");
        this.eventId = required(eventId, "eventId");
        this.correlationId = required(correlationId, "correlationId");
        this.traceId = required(traceId, "traceId");
        this.environmentId = required(environmentId, "environmentId");
        this.bindingMode = required(bindingMode, "bindingMode");
        this.executionStatus = required(executionStatus, "executionStatus");
        this.executionFailureReasonCode = executionFailureReasonCode;
        this.executionLimitations = immutable(executionLimitations);
        this.executionUncertainty = executionUncertainty;
        this.decisionId = required(decisionId, "decisionId");
        this.decisionStatus = required(decisionStatus, "decisionStatus");
        this.dispositionCode = dispositionCode;
        this.decisionReasonCode = required(decisionReasonCode, "decisionReasonCode");
        this.capabilityBindingId = required(capabilityBindingId, "capabilityBindingId");
        this.governedReleaseRefs = immutableRequired(governedReleaseRefs, "governedReleaseRefs");
        this.acceptanceRef = required(acceptanceRef, "acceptanceRef");
        this.acceptedEvidenceRefs = immutable(acceptedEvidenceRefs);
        this.acceptedSourceRefs = immutable(acceptedSourceRefs);
        this.acceptedProvenanceRefs = immutable(acceptedProvenanceRefs);
        this.proposalId = proposalId;
        this.commitStatus = commitStatus;
        this.commitReasonCode = commitReasonCode;
        this.commitAuditId = commitAuditId;
    }

    public String getConsultationId() { return consultationId; }
    public String getCdpId() { return cdpId; }
    public int getSourceClinicalStateVersion() { return sourceClinicalStateVersion; }
    public Integer getCommittedClinicalStateVersion() { return committedClinicalStateVersion; }
    public String getThreadId() { return threadId; }
    public String getRunId() { return runId; }
    public String getEventId() { return eventId; }
    public String getCorrelationId() { return correlationId; }
    public String getTraceId() { return traceId; }
    public String getEnvironmentId() { return environmentId; }
    public String getBindingMode() { return bindingMode; }
    public String getExecutionStatus() { return executionStatus; }
    public String getExecutionFailureReasonCode() { return executionFailureReasonCode; }
    public List<String> getExecutionLimitations() { return executionLimitations; }
    public String getExecutionUncertainty() { return executionUncertainty; }
    public String getDecisionId() { return decisionId; }
    public String getDecisionStatus() { return decisionStatus; }
    public String getDispositionCode() { return dispositionCode; }
    public String getDecisionReasonCode() { return decisionReasonCode; }
    public String getCapabilityBindingId() { return capabilityBindingId; }
    public List<String> getGovernedReleaseRefs() { return governedReleaseRefs; }
    public String getAcceptanceRef() { return acceptanceRef; }
    public List<String> getAcceptedEvidenceRefs() { return acceptedEvidenceRefs; }
    public List<String> getAcceptedSourceRefs() { return acceptedSourceRefs; }
    public List<String> getAcceptedProvenanceRefs() { return acceptedProvenanceRefs; }
    public String getProposalId() { return proposalId; }
    public String getCommitStatus() { return commitStatus; }
    public String getCommitReasonCode() { return commitReasonCode; }
    public String getCommitAuditId() { return commitAuditId; }

    private static List<String> immutableRequired(List<String> values, String name) {
        List<String> copy = immutable(values);
        if (copy.isEmpty()) throw new IllegalArgumentException(name + " is required");
        return copy;
    }

    private static List<String> immutable(List<String> values) {
        return values == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(values));
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
