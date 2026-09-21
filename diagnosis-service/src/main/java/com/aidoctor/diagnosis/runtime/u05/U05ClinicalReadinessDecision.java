package com.aidoctor.diagnosis.runtime.u05;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Frozen D03 Clinical Readiness decision envelope. */
public final class U05ClinicalReadinessDecision {
    public static final String DECIDED = "DECIDED";
    public static final String INPUT_FAILURE = "INPUT_FAILURE";
    public static final String INPUT_CONFLICT = "INPUT_CONFLICT";

    public static final String OUT_OF_SCOPE = "OUT_OF_SCOPE";
    public static final String NEEDS_OFFLINE_EVIDENCE = "NEEDS_OFFLINE_EVIDENCE";
    public static final String NEEDS_CLARIFICATION = "NEEDS_CLARIFICATION";
    public static final String CAN_ASK_MORE = "CAN_ASK_MORE";
    public static final String READY_FOR_CLINICAL_ANALYSIS = "READY_FOR_CLINICAL_ANALYSIS";
    public static final String NO_RELIABLE_DIRECTION = "NO_RELIABLE_DIRECTION";

    private final String decisionId;
    private final String consultationId;
    private final String cdpId;
    private final int inputClinicalStateVersion;
    private final String sourceAdmissionRef;
    private final String sourceReadinessInputSetIdentity;
    private final String evaluationContext;
    private final String decisionStatus;
    private final String clinicalReadiness;
    private final List<String> reasonCodes;
    private final List<String> basisRefs;
    private final List<String> inputRefs;
    private final String policyVersion;
    private final String policyRuleRef;
    private final List<String> ruleReleaseRefs;
    private final List<String> knowledgeReleaseRefs;
    private final String safetyGateRef;
    private final String restrictedContextRef;
    private final String restrictedPermissionRef;
    private final String createdAt;
    private final String validity;

    public U05ClinicalReadinessDecision(
            String decisionId,
            U05AdmittedInput input,
            String decisionStatus,
            String clinicalReadiness,
            List<String> reasonCodes,
            List<String> basisRefs,
            String policyVersion,
            String policyRuleRef,
            List<String> ruleReleaseRefs,
            List<String> knowledgeReleaseRefs,
            String createdAt) {
        this.decisionId = required(decisionId, "decisionId");
        if (input == null) throw new IllegalArgumentException("input is required");
        this.consultationId = input.getConsultationId();
        this.cdpId = input.getCdpId();
        this.inputClinicalStateVersion = input.getClinicalStateVersion();
        this.sourceAdmissionRef = input.getAdmissionId();
        this.sourceReadinessInputSetIdentity = input.getAcceptedReadinessInputSetIdentity();
        this.evaluationContext = input.getEvaluationContext();
        this.decisionStatus = status(decisionStatus);
        this.clinicalReadiness = clinicalReadiness;
        this.reasonCodes = immutable(reasonCodes);
        this.basisRefs = immutable(basisRefs);
        this.inputRefs = Collections.unmodifiableList(new ArrayList<String>(input.getAcceptedReadinessInputRefs()));
        this.policyVersion = required(policyVersion, "policyVersion");
        this.policyRuleRef = required(policyRuleRef, "policyRuleRef");
        this.ruleReleaseRefs = immutable(ruleReleaseRefs);
        this.knowledgeReleaseRefs = immutable(knowledgeReleaseRefs);
        this.safetyGateRef = input.getAcceptedU04GateRef();
        this.restrictedContextRef = input.getAcceptedRestrictedContextRef();
        this.restrictedPermissionRef = input.getAcceptedRestrictedPermissionRef();
        this.createdAt = required(createdAt, "createdAt");
        this.validity = "CURRENT";

        if (DECIDED.equals(this.decisionStatus)) {
            readiness(this.clinicalReadiness);
        } else if (this.clinicalReadiness != null) {
            throw new IllegalArgumentException("non-DECIDED D03 result must not carry Clinical Readiness");
        }
    }

    public String getDecisionId() { return decisionId; }
    public String getConsultationId() { return consultationId; }
    public String getCdpId() { return cdpId; }
    public int getInputClinicalStateVersion() { return inputClinicalStateVersion; }
    public String getSourceAdmissionRef() { return sourceAdmissionRef; }
    public String getSourceReadinessInputSetIdentity() { return sourceReadinessInputSetIdentity; }
    public String getEvaluationContext() { return evaluationContext; }
    public String getDecisionStatus() { return decisionStatus; }
    public String getClinicalReadiness() { return clinicalReadiness; }
    public List<String> getReasonCodes() { return reasonCodes; }
    public List<String> getBasisRefs() { return basisRefs; }
    public List<String> getInputRefs() { return inputRefs; }
    public String getPolicyVersion() { return policyVersion; }
    public String getPolicyRuleRef() { return policyRuleRef; }
    public List<String> getRuleReleaseRefs() { return ruleReleaseRefs; }
    public List<String> getKnowledgeReleaseRefs() { return knowledgeReleaseRefs; }
    public String getSafetyGateRef() { return safetyGateRef; }
    public String getRestrictedContextRef() { return restrictedContextRef; }
    public String getRestrictedPermissionRef() { return restrictedPermissionRef; }
    public String getCreatedAt() { return createdAt; }
    public String getValidity() { return validity; }
    public boolean isDecided() { return DECIDED.equals(decisionStatus); }

    private static String status(String value) {
        String v = required(value, "decisionStatus");
        if (!DECIDED.equals(v) && !INPUT_FAILURE.equals(v) && !INPUT_CONFLICT.equals(v)) {
            throw new IllegalArgumentException("unsupported D03 decisionStatus");
        }
        return v;
    }

    private static void readiness(String value) {
        if (!OUT_OF_SCOPE.equals(value)
                && !NEEDS_OFFLINE_EVIDENCE.equals(value)
                && !NEEDS_CLARIFICATION.equals(value)
                && !CAN_ASK_MORE.equals(value)
                && !READY_FOR_CLINICAL_ANALYSIS.equals(value)
                && !NO_RELIABLE_DIRECTION.equals(value)) {
            throw new IllegalArgumentException("unsupported Clinical Readiness");
        }
    }

    private static List<String> immutable(List<String> values) {
        if (values == null) throw new IllegalArgumentException("list field is required");
        return Collections.unmodifiableList(new ArrayList<String>(values));
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
