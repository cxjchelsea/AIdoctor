package com.aidoctor.diagnosis.runtime.u05;

/** Typed downstream eligibility only. It is not a Scheduler or Unit execution result. */
public final class U05DownstreamEligibility {
    private final String eligibilityId;
    private final String routingDecisionRef;
    private final String routeEffectId;
    private final String consultationId;
    private final String cdpId;
    private final String authoritativeReadinessRecordRef;
    private final String authoritativeReadinessEffectId;
    private final String readinessCommitEvidenceRef;
    private final String readinessValue;
    private final int currentClinicalStateVersion;
    private final String downstreamConsequence;
    private final String targetUnitId;
    private final String currentU04GateRef;
    private final String restrictedContextRef;
    private final String downstreamPermissionRef;
    private final String canonicalEventRef;
    private final String businessEventIdentity;
    private final String correlationId;
    private final String traceId;
    private final String routingPolicyVersion;
    private final String validity;
    private final String createdAt;

    public U05DownstreamEligibility(
            String eligibilityId,
            String routingDecisionRef,
            String routeEffectId,
            U05AdmittedInput input,
            U05ClinicalReadinessCommitEvidence commitEvidence,
            String readinessValue,
            U05RoutingCurrentness currentness,
            String downstreamConsequence,
            String targetUnitId,
            String downstreamPermissionRef,
            String routingPolicyVersion) {
        this.eligibilityId = required(eligibilityId, "eligibilityId");
        this.routingDecisionRef = required(routingDecisionRef, "routingDecisionRef");
        this.routeEffectId = required(routeEffectId, "routeEffectId");
        this.consultationId = input.getConsultationId();
        this.cdpId = input.getCdpId();
        this.authoritativeReadinessRecordRef = commitEvidence.getAuthoritativeReadinessRecordRef();
        this.authoritativeReadinessEffectId = commitEvidence.getEffectId();
        this.readinessCommitEvidenceRef = commitEvidence.getCommitResultRef();
        this.readinessValue = required(readinessValue, "readinessValue");
        this.currentClinicalStateVersion = currentness.getCurrentClinicalStateVersion();
        this.downstreamConsequence = required(downstreamConsequence, "downstreamConsequence");
        this.targetUnitId = required(targetUnitId, "targetUnitId");
        this.currentU04GateRef = currentness.getCurrentU04GateRef();
        this.restrictedContextRef = currentness.getRestrictedContextRef();
        this.downstreamPermissionRef = downstreamPermissionRef;
        this.canonicalEventRef = input.getCanonicalEventRef();
        this.businessEventIdentity = input.getBusinessEventIdentity();
        this.correlationId = input.getCorrelationId();
        this.traceId = input.getTraceId();
        this.routingPolicyVersion = required(routingPolicyVersion, "routingPolicyVersion");
        this.validity = "CURRENT";
        this.createdAt = commitEvidence.getCommittedAt();
    }

    public String getEligibilityId() { return eligibilityId; }
    public String getRoutingDecisionRef() { return routingDecisionRef; }
    public String getRouteEffectId() { return routeEffectId; }
    public String getConsultationId() { return consultationId; }
    public String getCdpId() { return cdpId; }
    public String getAuthoritativeReadinessRecordRef() { return authoritativeReadinessRecordRef; }
    public String getAuthoritativeReadinessEffectId() { return authoritativeReadinessEffectId; }
    public String getReadinessCommitEvidenceRef() { return readinessCommitEvidenceRef; }
    public String getReadinessValue() { return readinessValue; }
    public int getCurrentClinicalStateVersion() { return currentClinicalStateVersion; }
    public String getDownstreamConsequence() { return downstreamConsequence; }
    public String getTargetUnitId() { return targetUnitId; }
    public String getCurrentU04GateRef() { return currentU04GateRef; }
    public String getRestrictedContextRef() { return restrictedContextRef; }
    public String getDownstreamPermissionRef() { return downstreamPermissionRef; }
    public String getCanonicalEventRef() { return canonicalEventRef; }
    public String getBusinessEventIdentity() { return businessEventIdentity; }
    public String getCorrelationId() { return correlationId; }
    public String getTraceId() { return traceId; }
    public String getRoutingPolicyVersion() { return routingPolicyVersion; }
    public String getValidity() { return validity; }
    public String getCreatedAt() { return createdAt; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
