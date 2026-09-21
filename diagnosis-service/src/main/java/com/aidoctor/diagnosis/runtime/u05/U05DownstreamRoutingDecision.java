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
    private final U05DownstreamEligibility eligibility;

    public U05DownstreamRoutingDecision(
            String routingDecisionId,
            String routingDecisionFingerprint,
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

    public String getRoutingDecisionId() { return routingDecisionId; }
    public String getRoutingDecisionFingerprint() { return routingDecisionFingerprint; }
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
    public U05DownstreamEligibility getEligibility() { return eligibility; }

    U05DownstreamRoutingDecision reattached() {
        return new U05DownstreamRoutingDecision(
                routingDecisionId,
                routingDecisionFingerprint,
                routingStatus,
                REATTACHED,
                candidateDownstreamConsequence,
                candidateTargetUnitId,
                downstreamConsequence,
                targetUnitId,
                downstreamPermissionDecisionRef,
                downstreamPermissionRef,
                routeEffectId,
                downstreamRouteAuthorizationId,
                failureHandoffRef,
                reasonCode,
                eligibility);
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
