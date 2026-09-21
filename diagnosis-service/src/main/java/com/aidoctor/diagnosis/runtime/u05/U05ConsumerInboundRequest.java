package com.aidoctor.diagnosis.runtime.u05;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Canonical RDP-01 consumer inbound request. Caller claims are not authoritative truth. */
public final class U05ConsumerInboundRequest {
    public static final String POST_SAFETY_INITIAL = "POST_SAFETY_INITIAL";
    public static final String A1_POST_BARRIER_CURRENT = "A1_POST_BARRIER_CURRENT";
    public static final String POST_USER_FACT_UPDATE = "POST_USER_FACT_UPDATE";
    public static final String POST_DDX_REEVALUATION = "POST_DDX_REEVALUATION";
    public static final String POST_OFFLINE_ASSESSMENT = "POST_OFFLINE_ASSESSMENT";

    public static final String U04_ORDINARY_ROUTING = "U04_ORDINARY_ROUTING";
    public static final String U04_A1_POST_BARRIER_ROUTING = "U04_A1_POST_BARRIER_ROUTING";
    public static final String CLINICAL_CONTINUATION_ROUTING = "CLINICAL_CONTINUATION_ROUTING";
    public static final String TO_U05_CLINICAL_READINESS = "TO_U05_CLINICAL_READINESS";
    public static final String U05_ELIGIBLE = "U05_ELIGIBLE";

    public static final String GATE_ALLOW = "ALLOW";
    public static final String GATE_RESTRICTED = "RESTRICTED";
    public static final String GATE_BLOCKED = "BLOCKED";
    public static final String GATE_UNAVAILABLE = "UNAVAILABLE";

    private final String requestId;
    private final String consultationId;
    private final String cdpId;
    private final int claimedClinicalStateVersion;
    private final String authoritativeStateRef;
    private final String evaluationContext;
    private final String routeSourceType;
    private final String routeSourceRef;
    private final String routeConsequence;
    private final String u04GateRef;
    private final String u04GateCommitRef;
    private final String gateValue;
    private final String routeAuthorizationType;
    private final String routeAuthorizationRef;
    private final String routingPolicyVersion;
    private final String bootstrapArchitectureBindingRef;
    private final String restrictedContextRef;
    private final String restrictedPermissionRef;
    private final String readinessInputManifestRef;
    private final String readinessInputSetIdentity;
    private final List<String> readinessInputRefs;
    private final String canonicalEventRef;
    private final String businessEventIdentity;
    private final String correlationId;
    private final String traceId;
    private final String admissionContractVersion;
    private final String environmentId;
    private final String createdAt;

    public U05ConsumerInboundRequest(
            String requestId,
            String consultationId,
            String cdpId,
            int claimedClinicalStateVersion,
            String authoritativeStateRef,
            String evaluationContext,
            String routeSourceType,
            String routeSourceRef,
            String routeConsequence,
            String u04GateRef,
            String u04GateCommitRef,
            String gateValue,
            String routeAuthorizationType,
            String routeAuthorizationRef,
            String routingPolicyVersion,
            String bootstrapArchitectureBindingRef,
            String restrictedContextRef,
            String restrictedPermissionRef,
            String readinessInputManifestRef,
            String readinessInputSetIdentity,
            List<String> readinessInputRefs,
            String canonicalEventRef,
            String businessEventIdentity,
            String correlationId,
            String traceId,
            String admissionContractVersion,
            String environmentId,
            String createdAt) {
        this.requestId = required(requestId, "requestId");
        this.consultationId = required(consultationId, "consultationId");
        this.cdpId = required(cdpId, "cdpId");
        if (claimedClinicalStateVersion < 0) throw new IllegalArgumentException("claimedClinicalStateVersion must be non-negative");
        this.claimedClinicalStateVersion = claimedClinicalStateVersion;
        this.authoritativeStateRef = required(authoritativeStateRef, "authoritativeStateRef");
        this.evaluationContext = required(evaluationContext, "evaluationContext");
        this.routeSourceType = required(routeSourceType, "routeSourceType");
        this.routeSourceRef = required(routeSourceRef, "routeSourceRef");
        this.routeConsequence = routeConsequence;
        this.u04GateRef = required(u04GateRef, "u04GateRef");
        this.u04GateCommitRef = required(u04GateCommitRef, "u04GateCommitRef");
        this.gateValue = required(gateValue, "gateValue");
        this.routeAuthorizationType = required(routeAuthorizationType, "routeAuthorizationType");
        this.routeAuthorizationRef = required(routeAuthorizationRef, "routeAuthorizationRef");
        this.routingPolicyVersion = required(routingPolicyVersion, "routingPolicyVersion");
        this.bootstrapArchitectureBindingRef = bootstrapArchitectureBindingRef;
        this.restrictedContextRef = restrictedContextRef;
        this.restrictedPermissionRef = restrictedPermissionRef;
        this.readinessInputManifestRef = required(readinessInputManifestRef, "readinessInputManifestRef");
        this.readinessInputSetIdentity = required(readinessInputSetIdentity, "readinessInputSetIdentity");
        if (readinessInputRefs == null) throw new IllegalArgumentException("readinessInputRefs is required");
        this.readinessInputRefs = Collections.unmodifiableList(new ArrayList<String>(readinessInputRefs));
        this.canonicalEventRef = required(canonicalEventRef, "canonicalEventRef");
        this.businessEventIdentity = required(businessEventIdentity, "businessEventIdentity");
        this.correlationId = required(correlationId, "correlationId");
        this.traceId = required(traceId, "traceId");
        this.admissionContractVersion = required(admissionContractVersion, "admissionContractVersion");
        this.environmentId = required(environmentId, "environmentId");
        this.createdAt = required(createdAt, "createdAt");
    }

    public String getRequestId() { return requestId; }
    public String getConsultationId() { return consultationId; }
    public String getCdpId() { return cdpId; }
    public int getClaimedClinicalStateVersion() { return claimedClinicalStateVersion; }
    public String getAuthoritativeStateRef() { return authoritativeStateRef; }
    public String getEvaluationContext() { return evaluationContext; }
    public String getRouteSourceType() { return routeSourceType; }
    public String getRouteSourceRef() { return routeSourceRef; }
    public String getRouteConsequence() { return routeConsequence; }
    public String getU04GateRef() { return u04GateRef; }
    public String getU04GateCommitRef() { return u04GateCommitRef; }
    public String getGateValue() { return gateValue; }
    public String getRouteAuthorizationType() { return routeAuthorizationType; }
    public String getRouteAuthorizationRef() { return routeAuthorizationRef; }
    public String getRoutingPolicyVersion() { return routingPolicyVersion; }
    public String getBootstrapArchitectureBindingRef() { return bootstrapArchitectureBindingRef; }
    public String getRestrictedContextRef() { return restrictedContextRef; }
    public String getRestrictedPermissionRef() { return restrictedPermissionRef; }
    public String getReadinessInputManifestRef() { return readinessInputManifestRef; }
    public String getReadinessInputSetIdentity() { return readinessInputSetIdentity; }
    public List<String> getReadinessInputRefs() { return readinessInputRefs; }
    public String getCanonicalEventRef() { return canonicalEventRef; }
    public String getBusinessEventIdentity() { return businessEventIdentity; }
    public String getCorrelationId() { return correlationId; }
    public String getTraceId() { return traceId; }
    public String getAdmissionContractVersion() { return admissionContractVersion; }
    public String getEnvironmentId() { return environmentId; }
    public String getCreatedAt() { return createdAt; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
