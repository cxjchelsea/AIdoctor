package com.aidoctor.diagnosis.runtime.u03;

import java.util.Collections;
import java.util.List;

/** C02 output boundary. Candidate data is not a final Clinical Risk Disposition. */
public final class U03RiskAssessmentCandidate {
    public static final String VALID = "VALID";
    public static final String FAILED = "FAILED";

    private final String status;
    private final List<String> evidenceRefs;
    private final String failureReasonCode;

    private U03RiskAssessmentCandidate(String status, List<String> evidenceRefs, String failureReasonCode) {
        this.status = status;
        this.evidenceRefs = evidenceRefs == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(evidenceRefs);
        this.failureReasonCode = failureReasonCode;
    }

    public static U03RiskAssessmentCandidate valid(List<String> evidenceRefs) {
        return new U03RiskAssessmentCandidate(VALID, evidenceRefs, null);
    }

    public static U03RiskAssessmentCandidate failed(String reasonCode) {
        if (reasonCode == null || reasonCode.trim().isEmpty()) {
            throw new IllegalArgumentException("reasonCode is required for FAILED risk assessment");
        }
        return new U03RiskAssessmentCandidate(FAILED, Collections.<String>emptyList(), reasonCode);
    }

    public String getStatus() { return status; }
    public List<String> getEvidenceRefs() { return evidenceRefs; }
    public String getFailureReasonCode() { return failureReasonCode; }
    public boolean isFailed() { return FAILED.equals(status); }
}
