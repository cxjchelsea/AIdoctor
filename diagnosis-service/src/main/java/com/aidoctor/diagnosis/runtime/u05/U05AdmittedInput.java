package com.aidoctor.diagnosis.runtime.u05;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Stable RDP-01 admitted snapshot; D03 may consume only this object. */
public final class U05AdmittedInput {
    private final String admissionId;
    private final String consultationId;
    private final String cdpId;
    private final int clinicalStateVersion;
    private final String evaluationContext;
    private final String acceptedU04GateRef;
    private final String acceptedRouteAuthorizationType;
    private final String acceptedRouteAuthorizationRef;
    private final String acceptedRouteSourceRef;
    private final String acceptedReadinessInputManifestRef;
    private final String acceptedReadinessInputSetIdentity;
    private final List<String> acceptedReadinessInputRefs;
    private final String acceptedRestrictedContextRef;
    private final String acceptedRestrictedPermissionRef;
    private final String canonicalEventRef;
    private final String businessEventIdentity;
    private final String correlationId;
    private final String traceId;
    private final String admissionContractVersion;
    private final String environmentId;
    private final String gateValue;
    private final String admittedCreatedAt;
    private final U05ReadinessInputManifest manifest;

    U05AdmittedInput(
            String admissionId,
            U05ConsumerInboundRequest request,
            U05ReadinessInputManifest manifest) {
        this.admissionId = admissionId;
        this.consultationId = request.getConsultationId();
        this.cdpId = request.getCdpId();
        this.clinicalStateVersion = request.getClaimedClinicalStateVersion();
        this.evaluationContext = request.getEvaluationContext();
        this.acceptedU04GateRef = request.getU04GateRef();
        this.acceptedRouteAuthorizationType = request.getRouteAuthorizationType();
        this.acceptedRouteAuthorizationRef = request.getRouteAuthorizationRef();
        this.acceptedRouteSourceRef = request.getRouteSourceRef();
        this.acceptedReadinessInputManifestRef = request.getReadinessInputManifestRef();
        this.acceptedReadinessInputSetIdentity = request.getReadinessInputSetIdentity();
        this.acceptedReadinessInputRefs = Collections.unmodifiableList(new ArrayList<String>(request.getReadinessInputRefs()));
        this.acceptedRestrictedContextRef = request.getRestrictedContextRef();
        this.acceptedRestrictedPermissionRef = request.getRestrictedPermissionRef();
        this.canonicalEventRef = request.getCanonicalEventRef();
        this.businessEventIdentity = request.getBusinessEventIdentity();
        this.correlationId = request.getCorrelationId();
        this.traceId = request.getTraceId();
        this.admissionContractVersion = request.getAdmissionContractVersion();
        this.environmentId = request.getEnvironmentId();
        this.gateValue = request.getGateValue();
        this.admittedCreatedAt = request.getCreatedAt();
        this.manifest = manifest;
    }

    public String getAdmissionId() { return admissionId; }
    public String getConsultationId() { return consultationId; }
    public String getCdpId() { return cdpId; }
    public int getClinicalStateVersion() { return clinicalStateVersion; }
    public String getEvaluationContext() { return evaluationContext; }
    public String getAcceptedU04GateRef() { return acceptedU04GateRef; }
    public String getAcceptedRouteAuthorizationType() { return acceptedRouteAuthorizationType; }
    public String getAcceptedRouteAuthorizationRef() { return acceptedRouteAuthorizationRef; }
    public String getAcceptedRouteSourceRef() { return acceptedRouteSourceRef; }
    public String getAcceptedReadinessInputManifestRef() { return acceptedReadinessInputManifestRef; }
    public String getAcceptedReadinessInputSetIdentity() { return acceptedReadinessInputSetIdentity; }
    public List<String> getAcceptedReadinessInputRefs() { return acceptedReadinessInputRefs; }
    public String getAcceptedRestrictedContextRef() { return acceptedRestrictedContextRef; }
    public String getAcceptedRestrictedPermissionRef() { return acceptedRestrictedPermissionRef; }
    public String getCanonicalEventRef() { return canonicalEventRef; }
    public String getBusinessEventIdentity() { return businessEventIdentity; }
    public String getCorrelationId() { return correlationId; }
    public String getTraceId() { return traceId; }
    public String getAdmissionContractVersion() { return admissionContractVersion; }
    public String getEnvironmentId() { return environmentId; }
    public String getGateValue() { return gateValue; }
    public String getAdmittedCreatedAt() { return admittedCreatedAt; }
    public U05ReadinessInputManifest getManifest() { return manifest; }
}
