package com.aidoctor.diagnosis.runtime.u03;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable P05 execution-fact record for one controlled non-production U03 run.
 *
 * <p>This record is non-authoritative. It correlates execution/provenance facts only;
 * it cannot create, override, or mutate Clinical Truth or canonical Clinical State.</p>
 */
public final class U03RuntimeTraceRecord {
    private final String consultationId;
    private final String threadId;
    private final String runId;
    private final String eventId;
    private final String cdpId;
    private final int clinicalStateVersion;
    private final String correlationId;
    private final String traceId;
    private final String environmentId;
    private final String bindingMode;

    private final String acceptanceRef;
    private final List<String> acceptedEvidenceRefs;
    private final List<String> acceptedSourceRefs;
    private final List<String> acceptedProvenanceRefs;

    private final String capabilityBindingId;
    private final List<String> governedReleaseRefs;

    private final String c02Status;
    private final String c02FailureReasonCode;
    private final String c02RuleReleaseRef;
    private final String c02KnowledgeReleaseRef;

    private final String d09DecisionId;
    private final String d09Status;
    private final String d09OutcomeCode;
    private final String d09ReasonCode;

    private final String k09ProposalId;
    private final String k09SourceDecisionRef;
    private final String k09PatchId;
    private final String k09IdempotencyKey;
    private final int k09BaseVersion;

    private final String commitStatus;
    private final String commitReasonCode;
    private final Integer commitPreviousVersion;
    private final Integer commitCommittedVersion;
    private final String commitAuditId;

    public U03RuntimeTraceRecord(
            String consultationId,
            String threadId,
            String runId,
            String eventId,
            String cdpId,
            int clinicalStateVersion,
            String correlationId,
            String traceId,
            String environmentId,
            String bindingMode,
            String acceptanceRef,
            List<String> acceptedEvidenceRefs,
            List<String> acceptedSourceRefs,
            List<String> acceptedProvenanceRefs,
            String capabilityBindingId,
            List<String> governedReleaseRefs,
            String c02Status,
            String c02FailureReasonCode,
            String c02RuleReleaseRef,
            String c02KnowledgeReleaseRef,
            String d09DecisionId,
            String d09Status,
            String d09OutcomeCode,
            String d09ReasonCode,
            String k09ProposalId,
            String k09SourceDecisionRef,
            String k09PatchId,
            String k09IdempotencyKey,
            int k09BaseVersion,
            String commitStatus,
            String commitReasonCode,
            Integer commitPreviousVersion,
            Integer commitCommittedVersion,
            String commitAuditId) {
        this.consultationId = required(consultationId, "consultationId");
        this.threadId = required(threadId, "threadId");
        this.runId = required(runId, "runId");
        this.eventId = required(eventId, "eventId");
        this.cdpId = required(cdpId, "cdpId");
        if (clinicalStateVersion < 0) throw new IllegalArgumentException("clinicalStateVersion must be non-negative");
        this.clinicalStateVersion = clinicalStateVersion;
        this.correlationId = required(correlationId, "correlationId");
        this.traceId = required(traceId, "traceId");
        this.environmentId = required(environmentId, "environmentId");
        this.bindingMode = required(bindingMode, "bindingMode");
        this.acceptanceRef = required(acceptanceRef, "acceptanceRef");
        this.acceptedEvidenceRefs = immutableRequired(acceptedEvidenceRefs, "acceptedEvidenceRefs");
        this.acceptedSourceRefs = immutable(acceptedSourceRefs);
        this.acceptedProvenanceRefs = immutableRequired(acceptedProvenanceRefs, "acceptedProvenanceRefs");
        this.capabilityBindingId = required(capabilityBindingId, "capabilityBindingId");
        this.governedReleaseRefs = immutableRequired(governedReleaseRefs, "governedReleaseRefs");
        this.c02Status = required(c02Status, "c02Status");
        this.c02FailureReasonCode = c02FailureReasonCode;
        this.c02RuleReleaseRef = c02RuleReleaseRef;
        this.c02KnowledgeReleaseRef = c02KnowledgeReleaseRef;
        this.d09DecisionId = required(d09DecisionId, "d09DecisionId");
        this.d09Status = required(d09Status, "d09Status");
        this.d09OutcomeCode = d09OutcomeCode;
        this.d09ReasonCode = required(d09ReasonCode, "d09ReasonCode");
        this.k09ProposalId = required(k09ProposalId, "k09ProposalId");
        this.k09SourceDecisionRef = required(k09SourceDecisionRef, "k09SourceDecisionRef");
        this.k09PatchId = required(k09PatchId, "k09PatchId");
        this.k09IdempotencyKey = required(k09IdempotencyKey, "k09IdempotencyKey");
        if (k09BaseVersion < 0) throw new IllegalArgumentException("k09BaseVersion must be non-negative");
        this.k09BaseVersion = k09BaseVersion;
        this.commitStatus = required(commitStatus, "commitStatus");
        this.commitReasonCode = required(commitReasonCode, "commitReasonCode");
        this.commitPreviousVersion = commitPreviousVersion;
        this.commitCommittedVersion = commitCommittedVersion;
        this.commitAuditId = commitAuditId;
    }

    public String getConsultationId() { return consultationId; }
    public String getThreadId() { return threadId; }
    public String getRunId() { return runId; }
    public String getEventId() { return eventId; }
    public String getCdpId() { return cdpId; }
    public int getClinicalStateVersion() { return clinicalStateVersion; }
    public String getCorrelationId() { return correlationId; }
    public String getTraceId() { return traceId; }
    public String getEnvironmentId() { return environmentId; }
    public String getBindingMode() { return bindingMode; }
    public String getAcceptanceRef() { return acceptanceRef; }
    public List<String> getAcceptedEvidenceRefs() { return acceptedEvidenceRefs; }
    public List<String> getAcceptedSourceRefs() { return acceptedSourceRefs; }
    public List<String> getAcceptedProvenanceRefs() { return acceptedProvenanceRefs; }
    public String getCapabilityBindingId() { return capabilityBindingId; }
    public List<String> getGovernedReleaseRefs() { return governedReleaseRefs; }
    public String getC02Status() { return c02Status; }
    public String getC02FailureReasonCode() { return c02FailureReasonCode; }
    public String getC02RuleReleaseRef() { return c02RuleReleaseRef; }
    public String getC02KnowledgeReleaseRef() { return c02KnowledgeReleaseRef; }
    public String getD09DecisionId() { return d09DecisionId; }
    public String getD09Status() { return d09Status; }
    public String getD09OutcomeCode() { return d09OutcomeCode; }
    public String getD09ReasonCode() { return d09ReasonCode; }
    public String getK09ProposalId() { return k09ProposalId; }
    public String getK09SourceDecisionRef() { return k09SourceDecisionRef; }
    public String getK09PatchId() { return k09PatchId; }
    public String getK09IdempotencyKey() { return k09IdempotencyKey; }
    public int getK09BaseVersion() { return k09BaseVersion; }
    public String getCommitStatus() { return commitStatus; }
    public String getCommitReasonCode() { return commitReasonCode; }
    public Integer getCommitPreviousVersion() { return commitPreviousVersion; }
    public Integer getCommitCommittedVersion() { return commitCommittedVersion; }
    public String getCommitAuditId() { return commitAuditId; }

    private static List<String> immutableRequired(List<String> values, String name) {
        List<String> copy = immutable(values);
        if (copy.isEmpty()) throw new IllegalArgumentException(name + " is required");
        return copy;
    }

    private static List<String> immutable(List<String> values) {
        return values == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(values));
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
