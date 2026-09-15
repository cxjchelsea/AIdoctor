package com.aidoctor.diagnosis.runtime.u03;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** C02 output boundary. Candidate metadata is not final state. */
public final class U03RiskAssessmentCandidate {
    public static final String VALID = "VALID";
    public static final String FAILED = "FAILED";

    private final String status;
    private final Integer clinicalStateVersion;
    private final List<String> evidenceRefs;
    private final double confidence;
    private final String uncertainty;
    private final List<String> limitations;
    private final List<String> sourceRefs;
    private final List<String> provenance;
    private final String capabilityBindingId;
    private final String capabilityVersion;
    private final String ruleReleaseId;
    private final String knowledgeReleaseId;
    private final String failureReasonCode;

    private U03RiskAssessmentCandidate(String status, Integer clinicalStateVersion, List<String> evidenceRefs,
            double confidence, String uncertainty, List<String> limitations, List<String> sourceRefs,
            List<String> provenance, String capabilityBindingId, String capabilityVersion,
            String ruleReleaseId, String knowledgeReleaseId, String failureReasonCode) {
        this.status = status;
        this.clinicalStateVersion = clinicalStateVersion;
        this.evidenceRefs = immutable(evidenceRefs);
        this.confidence = confidence;
        this.uncertainty = uncertainty;
        this.limitations = immutable(limitations);
        this.sourceRefs = immutable(sourceRefs);
        this.provenance = immutable(provenance);
        this.capabilityBindingId = capabilityBindingId;
        this.capabilityVersion = capabilityVersion;
        this.ruleReleaseId = ruleReleaseId;
        this.knowledgeReleaseId = knowledgeReleaseId;
        this.failureReasonCode = failureReasonCode;
    }

    public static U03RiskAssessmentCandidate valid(int clinicalStateVersion, List<String> evidenceRefs,
            double confidence, String uncertainty, List<String> limitations, List<String> sourceRefs,
            List<String> provenance, String capabilityBindingId, String capabilityVersion,
            String ruleReleaseId, String knowledgeReleaseId) {
        if (clinicalStateVersion < 0) throw new IllegalArgumentException("clinicalStateVersion must be non-negative");
        if (confidence < 0.0d || confidence > 1.0d) throw new IllegalArgumentException("confidence must be in [0,1]");
        return new U03RiskAssessmentCandidate(
                VALID, Integer.valueOf(clinicalStateVersion), evidenceRefs, confidence, uncertainty,
                limitations, sourceRefs, provenance,
                required(capabilityBindingId, "capabilityBindingId"),
                required(capabilityVersion, "capabilityVersion"),
                required(ruleReleaseId, "ruleReleaseId"),
                required(knowledgeReleaseId, "knowledgeReleaseId"), null);
    }

    public static U03RiskAssessmentCandidate failed(String reasonCode) {
        return new U03RiskAssessmentCandidate(
                FAILED, null, Collections.<String>emptyList(), 0.0d, null,
                Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
                null, null, null, null, required(reasonCode, "reasonCode"));
    }

    public String getStatus() { return status; }
    public Integer getClinicalStateVersion() { return clinicalStateVersion; }
    public List<String> getEvidenceRefs() { return evidenceRefs; }
    public double getConfidence() { return confidence; }
    public String getUncertainty() { return uncertainty; }
    public List<String> getLimitations() { return limitations; }
    public List<String> getSourceRefs() { return sourceRefs; }
    public List<String> getProvenance() { return provenance; }
    public String getCapabilityBindingId() { return capabilityBindingId; }
    public String getCapabilityVersion() { return capabilityVersion; }
    public String getRuleReleaseId() { return ruleReleaseId; }
    public String getKnowledgeReleaseId() { return knowledgeReleaseId; }
    public String getFailureReasonCode() { return failureReasonCode; }
    public boolean isFailed() { return FAILED.equals(status); }

    private static List<String> immutable(List<String> values) {
        return values == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(values));
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
