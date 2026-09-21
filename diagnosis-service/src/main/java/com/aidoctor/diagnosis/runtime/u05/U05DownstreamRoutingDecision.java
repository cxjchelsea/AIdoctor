package com.aidoctor.diagnosis.runtime.u05;

/** Stable RDP-04 post-readiness routing projection. */
public final class U05DownstreamRoutingDecision {
    public static final String ELIGIBLE = "ELIGIBLE";
    public static final String PREEMPTED = "PREEMPTED";
    public static final String FAILURE_REQUIRED = "FAILURE_REQUIRED";
    public static final String REJECTED_STALE = "REJECTED_STALE";

    public static final String ORIGINAL = "ORIGINAL";
    public static final String REATTACHED = "REATTACHED";

    public static final String TO_U06_QUESTION_PATH = "TO_U06_QUESTION_PATH";
    public static final String TO_U08_CLINICAL_ANALYSIS = "TO_U08_CLINICAL_ANALYSIS";
    public static final String TO_U10_OFFLINE_EVIDENCE = "TO_U10_OFFLINE_EVIDENCE";
    public static final String TO_U11_SAFE_EXIT = "TO_U11_SAFE_EXIT";

    private final String routingDecisionId;
    private final String routingDecisionFingerprint;

    private final String consultationId;
    private final String cdpId;

    private final String sourceReadinessRecordRef;
    private final String sourceReadinessEffectId;
    private final String sourceReadinessCommitEvidenceRef;
    private final String sourceReadinessValue;
    private final int sourceReadinessDerivedFromVersion;
    private final int sourceReadinessCommittedVersion;
    private final int currentClinicalStateVersionAtRouting;

    private final String sourceU04GateRef;
    private final String gateValue;
    private final String sourceInboundRouteRef;
    private final String restrictedContextRef;

    private final String routingStatus;
    private final String replayDisposition;
    private final String candidateDownstreamConsequence;
    private final String candidateTargetUnitId;
    private final String downstreamConsequence;
    private final String targetUnitId;
    private final String downstreamPermissionDecisionRef;
    private final String downstreamPermissionRef;
    private final String routeEffectId;
    private final String downstreamRouteAuthorizationId;
    private final String failureHandoffRef;
    private final String reasonCode;

    private final String canonicalEventRef;
    private final String businessEventIdentity;
    private final String correlationId;
    private final String traceId;
    private final String routingPolicyVersion;
    private final String createdAt;
    private final String validity;

    private final U05DownstreamEligibility eligibility;

    public U05DownstreamRoutingDecision(
            String routingDecisionId,
            String routingDecisionFingerprint,
            U05AdmittedInput input,
            U05ClinicalReadinessDecision readinessDecision,
            U05ClinicalReadinessCommitEvidence commitEvidence,
            U05RoutingCurrentness currentness,
            String routingPolicyVersion,
            String routingStatus,
            String replayDisposition,
            String candidateDownstreamConsequence,
            String candidateTargetUnitId,
            String downstreamConsequence,
            String targetUnitId,
            String downstreamPermissionDecisionRef,
            String downstreamPermissionRef,
            String routeEffectId,
            String downstreamRouteAuthorizationId,
            String failureHandoffRef,
            String reasonCode,
            U05DownstreamEligibility eligibility) {
        this.routingDecisionId = required(routingDecisionId, "routingDecisionId");
        this.routingDecisionFingerprint = required(routingDecisionFingerprint, "routingDecisionFingerprint");
        if (input == null || readinessDecision == null || commitEvidence == null || currentness == null) {
            throw new IllegalArgumentException("routing provenance is required");
        }

        this.consultationId = input.getConsultationId();
        this.cdpId = input.getCdpId();

        this.sourceReadinessRecordRef = commitEvidence.getAuthoritativeReadinessRecordRef();
        this.sourceReadinessEffectId = commitEvidence.getEffectId();
        this.sourceReadinessCommitEvidenceRef = commitEvidence.getCommitResultRef();
        this.sourceReadinessValue = readinessDecision.getClinicalReadiness();
        this.sourceReadinessDerivedFromVersion = readinessDecision.getInputClinicalStateVersion();
        this.sourceReadinessCommittedVersion = commitEvidence.getCommittedClinicalStateVersion();
        this.currentClinicalStateVersionAtRouting = currentness.getCurrentClinicalStateVersion();

        this.sourceU04GateRef = currentness.getCurrentU04GateRef();
        this.gateValue = currentness.getGateValue();
        this.sourceInboundRouteRef = input.getAcceptedRouteAuthorizationRef();
        this.restrictedContextRef = currentness.getRestrictedContextRef();

        this.routingStatus = status(routingStatus);
        this.replayDisposition = replay(replayDisposition);
        this.candidateDownstreamConsequence = candidateDownstreamConsequence;
        this.candidateTargetUnitId = candidateTargetUnitId;
        this.downstreamConsequence = downstreamConsequence;
        this.targetUnitId = targetUnitId;
        this.downstreamPermissionDecisionRef = downstreamPermissionDecisionRef;
        this.downstreamPermissionRef = downstreamPermissionRef;
        this.routeEffectId = routeEffectId;
        this.downstreamRouteAuthorizationId = downstreamRouteAuthorizationId;
        this.failureHandoffRef = failureHandoffRef;
        this.reasonCode = required(reasonCode, "reasonCode");

        this.canonicalEventRef = input.getCanonicalEventRef();
        this.businessEventIdentity = input.getBusinessEventIdentity();
        this.correlationId = input.getCorrelationId();
        this.traceId = input.getTraceId();
        this.routingPolicyVersion = required(routingPolicyVersion, "routingPolicyVersion");
        this.createdAt = commitEvidence.getCommittedAt();
        this.validity = "CURRENT";
        this.eligibility = eligibility;

        if (ELIGIBLE.equals(this.routingStatus)) {
            required(this.downstreamConsequence, "downstreamConsequence");
            required(this.targetUnitId, "targetUnitId");
            required(this.routeEffectId, "routeEffectId");
            required(this.downstreamRouteAuthorizationId, "downstreamRouteAuthorizationId");
            if (eligibility == null) throw new IllegalArgumentException("ELIGIBLE route requires eligibility");
        } else if (eligibility != null || downstreamConsequence != null || targetUnitId != null) {
            throw new IllegalArgumentException("non-ELIGIBLE route must not carry ordinary eligibility");
        }
    }

    private U05DownstreamRoutingDecision(U05DownstreamRoutingDecision source, String replayDisposition) {
        this.routingDecisionId = source.routingDecisionId;
        this.routingDecisionFingerprint = source.routingDecisionFingerprint;
        this.consultationId = source.consultationId;
        this.cdpId = source.cdpId;
        this.sourceReadinessRecordRef = source.sourceReadinessRecordRef;
        this.sourceReadinessEffectId = source.sourceReadinessEffectId;
        this.sourceReadinessCommitEvidenceRef = source.sourceReadinessCommitEvidenceRef;
        this.sourceReadinessValue = source.sourceReadinessValue;
        this.sourceReadinessDerivedFromVersion = source.sourceReadinessDerivedFromVersion;
        this.sourceReadinessCommittedVersion = source.sourceReadinessCommittedVersion;
        this.currentClinicalStateVersionAtRouting = source.currentClinicalStateVersionAtRouting;
        this.sourceU04GateRef = source.sourceU04GateRef;
        this.gateValue = source.gateValue;
        this.sourceInboundRouteRef = source.sourceInboundRouteRef;
        this.restrictedContextRef = source.restrictedContextRef;
        this.routingStatus = source.routingStatus;
        this.replayDisposition = replay(replayDisposition);
        this.candidateDownstreamConsequence = source.candidateDownstreamConsequence;
        this.candidateTargetUnitId = source.candidateTargetUnitId;
        this.downstreamConsequence = source.downstreamConsequence;
        this.targetUnitId = source.targetUnitId;
        this.downstreamPermissionDecisionRef = source.downstreamPermissionDecisionRef;
        this.downstreamPermissionRef = source.downstreamPermissionRef;
        this.routeEffectId = source.routeEffectId;
        this.downstreamRouteAuthorizationId = source.downstreamRouteAuthorizationId;
        this.failureHandoffRef = source.failureHandoffRef;
        this.reasonCode = source.reasonCode;
        this.canonicalEventRef = source.canonicalEventRef;
        this.businessEventIdentity = source.businessEventIdentity;
        this.correlationId = source.correlationId;
        this.traceId = source.traceId;
        this.routingPolicyVersion = source.routingPolicyVersion;
        this.createdAt = source.createdAt;
        this.validity = source.validity;
        this.eligibility = source.eligibility;
    }

    public String getRoutingDecisionId() { return routingDecisionId; }
    public String getRoutingDecisionFingerprint() { return routingDecisionFingerprint; }
    public String getConsultationId() { return consultationId; }
    public String getCdpId() { return cdpId; }
    public String getSourceReadinessRecordRef() { return sourceReadinessRecordRef; }
    public String getSourceReadinessEffectId() { return sourceReadinessEffectId; }
    public String getSourceReadinessCommitEvidenceRef() { return sourceReadinessCommitEvidenceRef; }
    public String getSourceReadinessValue() { return sourceReadinessValue; }
    public int getSourceReadinessDerivedFromVersion() { return sourceReadinessDerivedFromVersion; }
    public int getSourceReadinessCommittedVersion() { return sourceReadinessCommittedVersion; }
    public int getCurrentClinicalStateVersionAtRouting() { return currentClinicalStateVersionAtRouting; }
    public String getSourceU04GateRef() { return sourceU04GateRef; }
    public String getGateValue() { return gateValue; }
    public String getSourceInboundRouteRef() { return sourceInboundRouteRef; }
    public String getRestrictedContextRef() { return restrictedContextRef; }
    public String getRoutingStatus() { return routingStatus; }
    public String getReplayDisposition() { return replayDisposition; }
    public String getCandidateDownstreamConsequence() { return candidateDownstreamConsequence; }
    public String getCandidateTargetUnitId() { return candidateTargetUnitId; }
    public String getDownstreamConsequence() { return downstreamConsequence; }
    public String getTargetUnitId() { return targetUnitId; }
    public String getDownstreamPermissionDecisionRef() { return downstreamPermissionDecisionRef; }
    public String getDownstreamPermissionRef() { return downstreamPermissionRef; }
    public String getRouteEffectId() { return routeEffectId; }
    public String getDownstreamRouteAuthorizationId() { return downstreamRouteAuthorizationId; }
    public String getFailureHandoffRef() { return failureHandoffRef; }
    public String getReasonCode() { return reasonCode; }
    public String getCanonicalEventRef() { return canonicalEventRef; }
    public String getBusinessEventIdentity() { return businessEventIdentity; }
    public String getCorrelationId() { return correlationId; }
    public String getTraceId() { return traceId; }
    public String getRoutingPolicyVersion() { return routingPolicyVersion; }
    public String getCreatedAt() { return createdAt; }
    public String getValidity() { return validity; }
    public U05DownstreamEligibility getEligibility() { return eligibility; }

    U05DownstreamRoutingDecision reattached() {
        return new U05DownstreamRoutingDecision(this, REATTACHED);
    }

    private static String status(String value) {
        String v = required(value, "routingStatus");
        if (!ELIGIBLE.equals(v) && !PREEMPTED.equals(v)
                && !FAILURE_REQUIRED.equals(v) && !REJECTED_STALE.equals(v)) {
            throw new IllegalArgumentException("unsupported routingStatus");
        }
        return v;
    }

    private static String replay(String value) {
        String v = required(value, "replayDisposition");
        if (!ORIGINAL.equals(v) && !REATTACHED.equals(v)) {
            throw new IllegalArgumentException("unsupported replayDisposition");
        }
        return v;
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
