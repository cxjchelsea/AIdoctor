package com.aidoctor.diagnosis.runtime.u05;

import java.util.List;

/** Frozen RDP-01 consumer admission implementation for the authorized non-production slice. */
public final class U05AdmissionService {
    public static final String CONTRACT_VERSION = "U05-RDP01-V1";

    public static final String MALFORMED_REQUEST = "U05_ADMISSION_MALFORMED_REQUEST";
    public static final String CONSULTATION_ID_MISMATCH = "U05_ADMISSION_CONSULTATION_ID_MISMATCH";
    public static final String CDP_ID_MISMATCH = "U05_ADMISSION_CDP_ID_MISMATCH";
    public static final String STATE_VERSION_MISMATCH = "U05_ADMISSION_STATE_VERSION_MISMATCH";
    public static final String CONTEXT_MISMATCH = "U05_ADMISSION_CONTEXT_MISMATCH";
    public static final String GATE_NOT_COMMITTED = "U05_ADMISSION_GATE_NOT_COMMITTED";
    public static final String GATE_NOT_CURRENT = "U05_ADMISSION_GATE_NOT_CURRENT";
    public static final String GATE_NOT_ELIGIBLE = "U05_ADMISSION_GATE_NOT_ELIGIBLE";
    public static final String ROUTING_AUTH_MISSING = "U05_ADMISSION_ROUTING_AUTH_MISSING";
    public static final String ROUTING_AUTH_STALE = "U05_ADMISSION_ROUTING_AUTH_STALE";
    public static final String ROUTE_SOURCE_MISMATCH = "U05_ADMISSION_ROUTE_SOURCE_MISMATCH";
    public static final String ROUTE_CONSEQUENCE_NOT_U05 = "U05_ADMISSION_ROUTE_CONSEQUENCE_NOT_U05";
    public static final String RESTRICTED_CONTEXT_MISSING = "U05_ADMISSION_RESTRICTED_CONTEXT_MISSING";
    public static final String RESTRICTED_CONTEXT_MISMATCH = "U05_ADMISSION_RESTRICTED_CONTEXT_MISMATCH";
    public static final String RESTRICTED_PERMISSION_MISSING = "U05_ADMISSION_RESTRICTED_PERMISSION_MISSING";
    public static final String RESTRICTED_ACTION_NOT_PERMITTED = "U05_ADMISSION_RESTRICTED_ACTION_NOT_PERMITTED";
    public static final String INPUT_MANIFEST_MISSING = "U05_ADMISSION_INPUT_MANIFEST_MISSING";
    public static final String INPUT_SET_IDENTITY_MISMATCH = "U05_ADMISSION_INPUT_SET_IDENTITY_MISMATCH";
    public static final String INPUT_REF_IDENTITY_MISMATCH = "U05_ADMISSION_INPUT_REF_IDENTITY_MISMATCH";
    public static final String PENDING_OWNER_RECOMPUTATION = "U05_ADMISSION_PENDING_OWNER_RECOMPUTATION";
    public static final String REPLAY_CONFLICT = "U05_ADMISSION_REPLAY_CONFLICT";
    public static final String CONTRACT_VERSION_MISMATCH = "U05_ADMISSION_CONTRACT_VERSION_MISMATCH";
    public static final String ENVIRONMENT_NOT_AUTHORIZED = "U05_ADMISSION_ENVIRONMENT_NOT_AUTHORIZED";

    private final U05AdmissionLedger ledger;

    public U05AdmissionService(U05AdmissionLedger ledger) {
        if (ledger == null) throw new IllegalArgumentException("ledger is required");
        this.ledger = ledger;
    }

    public U05AdmissionResult admit(
            U05ConsumerInboundRequest request,
            U05ReadinessInputManifest manifest,
            U05AdmissionAuthoritySnapshot authority) {
        if (request == null || manifest == null || authority == null) {
            return U05AdmissionResult.rejected(MALFORMED_REQUEST);
        }

        // A1 — environment / explicit authorization scope.
        if (!authority.isEnvironmentAuthorized()
                || !request.getEnvironmentId().equals(authority.getEnvironmentId())
                || isProduction(request.getEnvironmentId())) {
            return U05AdmissionResult.rejected(ENVIRONMENT_NOT_AUTHORIZED);
        }

        // A2-A4 — authoritative identity and current version.
        if (!request.getConsultationId().equals(authority.getConsultationId())) {
            return U05AdmissionResult.rejected(CONSULTATION_ID_MISMATCH);
        }
        if (!request.getCdpId().equals(authority.getCdpId())) {
            return U05AdmissionResult.rejected(CDP_ID_MISMATCH);
        }
        if (request.getClaimedClinicalStateVersion() != authority.getCurrentClinicalStateVersion()) {
            return U05AdmissionResult.rejected(STATE_VERSION_MISMATCH);
        }
        if (!CONTRACT_VERSION.equals(request.getAdmissionContractVersion())) {
            return U05AdmissionResult.rejected(CONTRACT_VERSION_MISMATCH);
        }

        // A5 — context vocabulary and route-source compatibility.
        String contextFailure = validateContext(request);
        if (contextFailure != null) return U05AdmissionResult.rejected(contextFailure);

        // A6 — committed/current Gate.
        if (!authority.isGateCommitted()
                || !request.getU04GateCommitRef().equals(authority.getGateCommitRef())) {
            return U05AdmissionResult.rejected(GATE_NOT_COMMITTED);
        }
        if (!authority.isGateCurrent()
                || !request.getU04GateRef().equals(authority.getGateRef())
                || !request.getGateValue().equals(authority.getGateValue())) {
            return U05AdmissionResult.rejected(GATE_NOT_CURRENT);
        }
        if (!U05ConsumerInboundRequest.GATE_ALLOW.equals(request.getGateValue())
                && !U05ConsumerInboundRequest.GATE_RESTRICTED.equals(request.getGateValue())) {
            return U05AdmissionResult.rejected(GATE_NOT_ELIGIBLE);
        }

        // A7 — RESTRICTED exact U05-evaluation permission provenance.
        if (U05ConsumerInboundRequest.GATE_RESTRICTED.equals(request.getGateValue())) {
            if (blank(request.getRestrictedContextRef())) {
                return U05AdmissionResult.rejected(RESTRICTED_CONTEXT_MISSING);
            }
            if (blank(request.getRestrictedPermissionRef())) {
                return U05AdmissionResult.rejected(RESTRICTED_PERMISSION_MISSING);
            }
            if (!request.getRestrictedContextRef().equals(authority.getRestrictedContextRef())) {
                return U05AdmissionResult.rejected(RESTRICTED_CONTEXT_MISMATCH);
            }
            if (!request.getRestrictedPermissionRef().equals(authority.getRestrictedPermissionRef())
                    || !authority.isRestrictedActionPermitted()) {
                return U05AdmissionResult.rejected(RESTRICTED_ACTION_NOT_PERMITTED);
            }
        }

        // A8-A9 — route source/consequence and source-neutral authorization.
        if (blank(request.getRouteAuthorizationType()) || blank(request.getRouteAuthorizationRef())) {
            return U05AdmissionResult.rejected(ROUTING_AUTH_MISSING);
        }
        if (!authority.isRouteAuthorizationCurrent()) {
            return U05AdmissionResult.rejected(ROUTING_AUTH_STALE);
        }
        if (!request.getRouteSourceRef().equals(authority.getRouteSourceRef())
                || !request.getRouteAuthorizationType().equals(authority.getRouteAuthorizationType())
                || !request.getRouteAuthorizationRef().equals(authority.getRouteAuthorizationRef())
                || !request.getRoutingPolicyVersion().equals(authority.getRoutingPolicyVersion())) {
            return U05AdmissionResult.rejected(ROUTE_SOURCE_MISMATCH);
        }
        if (U05ConsumerInboundRequest.CLINICAL_CONTINUATION_ROUTING.equals(request.getRouteSourceType())) {
            if (!U05ConsumerInboundRequest.TO_U05_CLINICAL_READINESS.equals(request.getRouteConsequence())
                    || !request.getRouteConsequence().equals(authority.getRouteConsequence())) {
                return U05AdmissionResult.rejected(ROUTE_CONSEQUENCE_NOT_U05);
            }
        }

        // A10 — A1 post-barrier prerequisites.
        if (U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(request.getEvaluationContext())) {
            if (!"A1".equals(request.getBootstrapArchitectureBindingRef())
                    || !"A1".equals(authority.getBootstrapArchitectureBindingRef())
                    || !authority.isA1CanonicalF3Complete()
                    || !authority.isPostF3SafetyBarrierComplete()
                    || !authority.isF3RevalidatedCurrent()
                    || !manifest.hasCurrentPresent(U05ReadinessInput.F3)) {
                return U05AdmissionResult.rejected(CONTEXT_MISMATCH);
            }
        }

        // A11 — immutable manifest identity/binding.
        String manifestFailure = validateManifest(request, manifest);
        if (manifestFailure != null) return U05AdmissionResult.rejected(manifestFailure);

        // A12 — known owner recomputation remains a pre-D03 non-entry.
        if (authority.isOwnerRecomputationPending()
                || hasMutationStaleOwnerInput(request.getEvaluationContext(), manifest)) {
            return U05AdmissionResult.rejected(PENDING_OWNER_RECOMPUTATION);
        }

        // A13-A14 — stable semantic admission identity and replay reconciliation.
        String admissionId = admissionId(request);
        String fingerprint = admissionFingerprint(request, manifest, authority);
        U05AdmissionLedger.Entry existing = ledger.find(admissionId);
        if (existing != null) {
            if (!fingerprint.equals(existing.getFingerprint())) {
                return U05AdmissionResult.rejected(REPLAY_CONFLICT);
            }
            return U05AdmissionResult.admitted(
                    existing.getAdmittedInput(),
                    U05AdmissionResult.REATTACHED);
        }

        U05AdmittedInput input = new U05AdmittedInput(admissionId, request, manifest, authority);
        try {
            ledger.store(admissionId, fingerprint, input);
        } catch (IllegalStateException conflict) {
            return U05AdmissionResult.rejected(REPLAY_CONFLICT);
        }
        return U05AdmissionResult.admitted(input, U05AdmissionResult.ORIGINAL);
    }

    private static String validateContext(U05ConsumerInboundRequest request) {
        String context = request.getEvaluationContext();
        if (U05ConsumerInboundRequest.POST_SAFETY_INITIAL.equals(context)) {
            return U05ConsumerInboundRequest.U04_ORDINARY_ROUTING.equals(request.getRouteSourceType())
                    ? null : ROUTE_SOURCE_MISMATCH;
        }
        if (U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context)) {
            return U05ConsumerInboundRequest.U04_A1_POST_BARRIER_ROUTING.equals(request.getRouteSourceType())
                    ? null : ROUTE_SOURCE_MISMATCH;
        }
        if (U05ConsumerInboundRequest.POST_USER_FACT_UPDATE.equals(context)
                || U05ConsumerInboundRequest.POST_DDX_REEVALUATION.equals(context)
                || U05ConsumerInboundRequest.POST_OFFLINE_ASSESSMENT.equals(context)) {
            return U05ConsumerInboundRequest.CLINICAL_CONTINUATION_ROUTING.equals(request.getRouteSourceType())
                    ? null : ROUTE_SOURCE_MISMATCH;
        }
        return CONTEXT_MISMATCH;
    }

    private static String validateManifest(
            U05ConsumerInboundRequest request,
            U05ReadinessInputManifest manifest) {
        if (blank(request.getReadinessInputManifestRef())
                || !request.getReadinessInputManifestRef().equals(manifest.getManifestRef())) {
            return INPUT_MANIFEST_MISSING;
        }
        if (!request.getReadinessInputSetIdentity().equals(manifest.getSetIdentity())
                || !request.getConsultationId().equals(manifest.getConsultationId())
                || !request.getCdpId().equals(manifest.getCdpId())
                || request.getClaimedClinicalStateVersion() != manifest.getClinicalStateVersion()
                || !request.getEvaluationContext().equals(manifest.getEvaluationContext())) {
            return INPUT_SET_IDENTITY_MISMATCH;
        }
        List<String> expectedRefs = manifest.presentInputRefs();
        if (!expectedRefs.equals(request.getReadinessInputRefs())) {
            return INPUT_REF_IDENTITY_MISMATCH;
        }
        for (U05ReadinessInput input : manifest.getInputs()) {
            if (!request.getConsultationId().equals(input.getConsultationId())
                    || !request.getCdpId().equals(input.getCdpId())) {
                return INPUT_REF_IDENTITY_MISMATCH;
            }
        }
        return null;
    }

    private static boolean hasMutationStaleOwnerInput(
            String context,
            U05ReadinessInputManifest manifest) {
        if (!U05ConsumerInboundRequest.POST_USER_FACT_UPDATE.equals(context)) return false;
        return manifest.hasStatus(U05ReadinessInput.F3, U05ReadinessInput.STALE)
                || manifest.hasStatus(U05ReadinessInput.F5, U05ReadinessInput.STALE)
                || manifest.hasStatus(U05ReadinessInput.F6, U05ReadinessInput.STALE);
    }

    private static String admissionId(U05ConsumerInboundRequest request) {
        return U05Ids.hash(
                "u05-admission",
                request.getConsultationId(),
                request.getCdpId(),
                String.valueOf(request.getClaimedClinicalStateVersion()),
                request.getEvaluationContext(),
                request.getU04GateRef(),
                request.getRouteAuthorizationType(),
                request.getRouteAuthorizationRef(),
                request.getRouteSourceRef(),
                request.getReadinessInputSetIdentity(),
                request.getRestrictedContextRef(),
                request.getRestrictedPermissionRef(),
                request.getBusinessEventIdentity(),
                request.getRoutingPolicyVersion(),
                request.getAdmissionContractVersion(),
                request.getEnvironmentId());
    }

    private static String admissionFingerprint(
            U05ConsumerInboundRequest request,
            U05ReadinessInputManifest manifest,
            U05AdmissionAuthoritySnapshot authority) {
        StringBuilder refs = new StringBuilder();
        for (String ref : manifest.presentInputRefs()) refs.append(ref).append('|');
        return U05Ids.hash(
                "u05-admission-payload",
                admissionId(request),
                request.getReadinessInputManifestRef(),
                refs.toString(),
                request.getCanonicalEventRef(),
                request.getRestrictedPermissionRef(),
                authority.getCurrentReadinessRecordRef());
    }

    private static boolean isProduction(String environment) {
        String normalized = environment == null ? "" : environment.trim().toLowerCase();
        return "prod".equals(normalized)
                || "production".equals(normalized)
                || normalized.startsWith("prod-")
                || normalized.startsWith("production-");
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
