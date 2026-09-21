package com.aidoctor.diagnosis.runtime.u05;

/**
 * Authoritative admission-host snapshot.
 *
 * <p>It represents values independently loaded by the host; request claims are
 * compared against this snapshot and are never promoted to truth.</p>
 */
public final class U05AdmissionAuthoritySnapshot {
    private final String consultationId;
    private final String cdpId;
    private final int currentClinicalStateVersion;
    private final String environmentId;
    private final boolean environmentAuthorized;
    private final boolean gateCommitted;
    private final boolean gateCurrent;
    private final String gateRef;
    private final String gateCommitRef;
    private final String gateValue;
    private final boolean routeAuthorizationCurrent;
    private final String routeSourceRef;
    private final String routeAuthorizationType;
    private final String routeAuthorizationRef;
    private final String routeConsequence;
    private final String routingPolicyVersion;
    private final String restrictedContextRef;
    private final String restrictedPermissionRef;
    private final boolean restrictedActionPermitted;
    private final String bootstrapArchitectureBindingRef;
    private final boolean a1CanonicalF3Complete;
    private final boolean postF3SafetyBarrierComplete;
    private final boolean f3RevalidatedCurrent;
    private final boolean ownerRecomputationPending;

    public U05AdmissionAuthoritySnapshot(
            String consultationId,
            String cdpId,
            int currentClinicalStateVersion,
            String environmentId,
            boolean environmentAuthorized,
            boolean gateCommitted,
            boolean gateCurrent,
            String gateRef,
            String gateCommitRef,
            String gateValue,
            boolean routeAuthorizationCurrent,
            String routeSourceRef,
            String routeAuthorizationType,
            String routeAuthorizationRef,
            String routeConsequence,
            String routingPolicyVersion,
            String restrictedContextRef,
            String restrictedPermissionRef,
            boolean restrictedActionPermitted,
            String bootstrapArchitectureBindingRef,
            boolean a1CanonicalF3Complete,
            boolean postF3SafetyBarrierComplete,
            boolean f3RevalidatedCurrent,
            boolean ownerRecomputationPending) {
        this.consultationId = required(consultationId, "consultationId");
        this.cdpId = required(cdpId, "cdpId");
        if (currentClinicalStateVersion < 0) throw new IllegalArgumentException("currentClinicalStateVersion must be non-negative");
        this.currentClinicalStateVersion = currentClinicalStateVersion;
        this.environmentId = required(environmentId, "environmentId");
        this.environmentAuthorized = environmentAuthorized;
        this.gateCommitted = gateCommitted;
        this.gateCurrent = gateCurrent;
        this.gateRef = required(gateRef, "gateRef");
        this.gateCommitRef = required(gateCommitRef, "gateCommitRef");
        this.gateValue = required(gateValue, "gateValue");
        this.routeAuthorizationCurrent = routeAuthorizationCurrent;
        this.routeSourceRef = required(routeSourceRef, "routeSourceRef");
        this.routeAuthorizationType = required(routeAuthorizationType, "routeAuthorizationType");
        this.routeAuthorizationRef = required(routeAuthorizationRef, "routeAuthorizationRef");
        this.routeConsequence = routeConsequence;
        this.routingPolicyVersion = required(routingPolicyVersion, "routingPolicyVersion");
        this.restrictedContextRef = restrictedContextRef;
        this.restrictedPermissionRef = restrictedPermissionRef;
        this.restrictedActionPermitted = restrictedActionPermitted;
        this.bootstrapArchitectureBindingRef = bootstrapArchitectureBindingRef;
        this.a1CanonicalF3Complete = a1CanonicalF3Complete;
        this.postF3SafetyBarrierComplete = postF3SafetyBarrierComplete;
        this.f3RevalidatedCurrent = f3RevalidatedCurrent;
        this.ownerRecomputationPending = ownerRecomputationPending;
    }

    public String getConsultationId() { return consultationId; }
    public String getCdpId() { return cdpId; }
    public int getCurrentClinicalStateVersion() { return currentClinicalStateVersion; }
    public String getEnvironmentId() { return environmentId; }
    public boolean isEnvironmentAuthorized() { return environmentAuthorized; }
    public boolean isGateCommitted() { return gateCommitted; }
    public boolean isGateCurrent() { return gateCurrent; }
    public String getGateRef() { return gateRef; }
    public String getGateCommitRef() { return gateCommitRef; }
    public String getGateValue() { return gateValue; }
    public boolean isRouteAuthorizationCurrent() { return routeAuthorizationCurrent; }
    public String getRouteSourceRef() { return routeSourceRef; }
    public String getRouteAuthorizationType() { return routeAuthorizationType; }
    public String getRouteAuthorizationRef() { return routeAuthorizationRef; }
    public String getRouteConsequence() { return routeConsequence; }
    public String getRoutingPolicyVersion() { return routingPolicyVersion; }
    public String getRestrictedContextRef() { return restrictedContextRef; }
    public String getRestrictedPermissionRef() { return restrictedPermissionRef; }
    public boolean isRestrictedActionPermitted() { return restrictedActionPermitted; }
    public String getBootstrapArchitectureBindingRef() { return bootstrapArchitectureBindingRef; }
    public boolean isA1CanonicalF3Complete() { return a1CanonicalF3Complete; }
    public boolean isPostF3SafetyBarrierComplete() { return postF3SafetyBarrierComplete; }
    public boolean isF3RevalidatedCurrent() { return f3RevalidatedCurrent; }
    public boolean isOwnerRecomputationPending() { return ownerRecomputationPending; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
