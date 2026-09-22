package com.aidoctor.diagnosis.runtime.u05;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Canonical RDP-05 readiness input/applicability envelope consumed by U05. */
public final class U05ReadinessInput {
    public static final String F1 = "F1";
    public static final String F2_CLARIFICATION = "F2_CLARIFICATION";
    public static final String F3 = "F3";
    public static final String F5 = "F5";
    public static final String F6 = "F6";

    public static final String PRESENT = "PRESENT";
    public static final String ABSENT_BY_DESIGN = "ABSENT_BY_DESIGN";
    public static final String NOT_YET_APPLICABLE = "NOT_YET_APPLICABLE";
    public static final String STALE = "STALE";
    public static final String FAILED = "FAILED";
    public static final String UNAVAILABLE = "UNAVAILABLE";

    public static final String CURRENT = "CURRENT";

    public static final String OUT_OF_SCOPE = "OUT_OF_SCOPE";
    public static final String NEEDS_CLARIFICATION = "NEEDS_CLARIFICATION";
    public static final String FRAMED_IN_SCOPE = "FRAMED_IN_SCOPE";
    public static final String CAN_ASK_MORE = "CAN_ASK_MORE";
    public static final String NEEDS_OFFLINE_EVIDENCE = "NEEDS_OFFLINE_EVIDENCE";
    public static final String NO_ACTIVE_ONLINE_BLOCKING_GAP = "NO_ACTIVE_ONLINE_BLOCKING_GAP";
    public static final String ANALYSIS_RESULT_AVAILABLE = "ANALYSIS_RESULT_AVAILABLE";
    public static final String NO_RELIABLE_DIRECTION = "NO_RELIABLE_DIRECTION";
    public static final String REASSESSMENT_REQUIRED = "REASSESSMENT_REQUIRED";
    public static final String NO_BLOCKING_OFFLINE_EVIDENCE_NEED = "NO_BLOCKING_OFFLINE_EVIDENCE_NEED";

    private final String readinessInputId;
    private final String sourceDomain;
    private final String sourceOwner;
    private final String inputKind;
    private final String applicabilityStatus;
    private final String businessSignal;
    private final String consultationId;
    private final String cdpId;
    private final int clinicalStateVersion;
    private final String sourceDecisionRef;
    private final String sourceStateRef;
    private final String applicabilityEvidenceRef;
    private final List<String> evidenceRefs;
    private final List<String> policyOrRuleRefs;
    private final String producedAt;
    private final String validity;
    private final String invalidationRef;

    public U05ReadinessInput(
            String readinessInputId,
            String sourceDomain,
            String sourceOwner,
            String inputKind,
            String applicabilityStatus,
            String businessSignal,
            String consultationId,
            String cdpId,
            int clinicalStateVersion,
            String sourceDecisionRef,
            String sourceStateRef,
            String applicabilityEvidenceRef,
            List<String> evidenceRefs,
            List<String> policyOrRuleRefs,
            String producedAt,
            String validity,
            String invalidationRef) {
        this.readinessInputId = required(readinessInputId, "readinessInputId");
        this.sourceDomain = domain(sourceDomain);
        this.sourceOwner = required(sourceOwner, "sourceOwner");
        this.inputKind = required(inputKind, "inputKind");
        this.applicabilityStatus = applicability(applicabilityStatus);
        this.businessSignal = businessSignal;
        this.consultationId = required(consultationId, "consultationId");
        this.cdpId = required(cdpId, "cdpId");
        if (clinicalStateVersion < 0) throw new IllegalArgumentException("clinicalStateVersion must be non-negative");
        this.clinicalStateVersion = clinicalStateVersion;
        this.sourceDecisionRef = required(sourceDecisionRef, "sourceDecisionRef");
        this.sourceStateRef = sourceStateRef;
        this.applicabilityEvidenceRef = required(applicabilityEvidenceRef, "applicabilityEvidenceRef");
        this.evidenceRefs = immutable(evidenceRefs, "evidenceRefs");
        this.policyOrRuleRefs = immutable(policyOrRuleRefs, "policyOrRuleRefs");
        this.producedAt = required(producedAt, "producedAt");
        this.validity = required(validity, "validity");
        this.invalidationRef = invalidationRef;

        if (PRESENT.equals(this.applicabilityStatus)) {
            required(this.businessSignal, "businessSignal");
        } else if (this.businessSignal != null && !this.businessSignal.trim().isEmpty()) {
            throw new IllegalArgumentException("non-PRESENT input must not carry a business signal");
        }
    }

    public String getReadinessInputId() { return readinessInputId; }
    public String getSourceDomain() { return sourceDomain; }
    public String getSourceOwner() { return sourceOwner; }
    public String getInputKind() { return inputKind; }
    public String getApplicabilityStatus() { return applicabilityStatus; }
    public String getBusinessSignal() { return businessSignal; }
    public String getConsultationId() { return consultationId; }
    public String getCdpId() { return cdpId; }
    public int getClinicalStateVersion() { return clinicalStateVersion; }
    public String getSourceDecisionRef() { return sourceDecisionRef; }
    public String getSourceStateRef() { return sourceStateRef; }
    public String getApplicabilityEvidenceRef() { return applicabilityEvidenceRef; }
    public List<String> getEvidenceRefs() { return evidenceRefs; }
    public List<String> getPolicyOrRuleRefs() { return policyOrRuleRefs; }
    public String getProducedAt() { return producedAt; }
    public String getValidity() { return validity; }
    public String getInvalidationRef() { return invalidationRef; }

    public boolean isPresent() { return PRESENT.equals(applicabilityStatus); }
    public boolean isCurrentPresentAt(int version) {
        return isPresent() && CURRENT.equals(validity) && clinicalStateVersion == version;
    }

    String semanticFingerprint() {
        return U05Ids.hash(
                "u05-input",
                readinessInputId, sourceDomain, sourceOwner, inputKind, applicabilityStatus,
                businessSignal, consultationId, cdpId, String.valueOf(clinicalStateVersion),
                sourceDecisionRef, sourceStateRef, applicabilityEvidenceRef, validity, invalidationRef);
    }

    private static String domain(String value) {
        String v = required(value, "sourceDomain");
        if (!F1.equals(v) && !F2_CLARIFICATION.equals(v) && !F3.equals(v)
                && !F5.equals(v) && !F6.equals(v)) {
            throw new IllegalArgumentException("unsupported sourceDomain");
        }
        return v;
    }

    private static String applicability(String value) {
        String v = required(value, "applicabilityStatus");
        if (!PRESENT.equals(v) && !ABSENT_BY_DESIGN.equals(v) && !NOT_YET_APPLICABLE.equals(v)
                && !STALE.equals(v) && !FAILED.equals(v) && !UNAVAILABLE.equals(v)) {
            throw new IllegalArgumentException("unsupported applicabilityStatus");
        }
        return v;
    }

    private static List<String> immutable(List<String> values, String name) {
        if (values == null) throw new IllegalArgumentException(name + " is required");
        List<String> copy = new ArrayList<String>();
        for (String value : values) copy.add(required(value, name + " item"));
        return Collections.unmodifiableList(copy);
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
