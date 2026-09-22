package com.aidoctor.diagnosis.runtime.u05;

/** Route-time RESTRICTED downstream-action permission decision. */
public final class U05DownstreamPermissionDecision {
    public static final String PERMITTED = "PERMITTED";
    public static final String DENIED = "DENIED";
    public static final String UNAVAILABLE = "UNAVAILABLE";

    private final String permissionDecisionId;
    private final String consultationId;
    private final String cdpId;
    private final String currentU04GateRef;
    private final String restrictedContextRef;
    private final String candidateDownstreamConsequence;
    private final String targetAction;
    private final String targetUnitId;
    private final String permissionStatus;
    private final String permissionRef;
    private final String permissionPolicyId;
    private final String permissionPolicyVersion;
    private final String validity;

    public U05DownstreamPermissionDecision(
            String permissionDecisionId,
            String consultationId,
            String cdpId,
            String currentU04GateRef,
            String restrictedContextRef,
            String candidateDownstreamConsequence,
            String targetAction,
            String targetUnitId,
            String permissionStatus,
            String permissionRef,
            String permissionPolicyId,
            String permissionPolicyVersion,
            String validity) {
        this.permissionDecisionId = required(permissionDecisionId, "permissionDecisionId");
        this.consultationId = required(consultationId, "consultationId");
        this.cdpId = required(cdpId, "cdpId");
        this.currentU04GateRef = required(currentU04GateRef, "currentU04GateRef");
        this.restrictedContextRef = required(restrictedContextRef, "restrictedContextRef");
        this.candidateDownstreamConsequence = required(candidateDownstreamConsequence, "candidateDownstreamConsequence");
        this.targetAction = required(targetAction, "targetAction");
        this.targetUnitId = required(targetUnitId, "targetUnitId");
        this.permissionStatus = status(permissionStatus);
        this.permissionRef = permissionRef;
        this.permissionPolicyId = required(permissionPolicyId, "permissionPolicyId");
        this.permissionPolicyVersion = required(permissionPolicyVersion, "permissionPolicyVersion");
        this.validity = required(validity, "validity");
        if (PERMITTED.equals(this.permissionStatus) && (permissionRef == null || permissionRef.trim().isEmpty())) {
            throw new IllegalArgumentException("PERMITTED decision requires permissionRef");
        }
    }

    public String getPermissionDecisionId() { return permissionDecisionId; }
    public String getConsultationId() { return consultationId; }
    public String getCdpId() { return cdpId; }
    public String getCurrentU04GateRef() { return currentU04GateRef; }
    public String getRestrictedContextRef() { return restrictedContextRef; }
    public String getCandidateDownstreamConsequence() { return candidateDownstreamConsequence; }
    public String getTargetAction() { return targetAction; }
    public String getTargetUnitId() { return targetUnitId; }
    public String getPermissionStatus() { return permissionStatus; }
    public String getPermissionRef() { return permissionRef; }
    public String getPermissionPolicyId() { return permissionPolicyId; }
    public String getPermissionPolicyVersion() { return permissionPolicyVersion; }
    public String getValidity() { return validity; }

    private static String status(String value) {
        String v = required(value, "permissionStatus");
        if (!PERMITTED.equals(v) && !DENIED.equals(v) && !UNAVAILABLE.equals(v)) {
            throw new IllegalArgumentException("unsupported permissionStatus");
        }
        return v;
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
