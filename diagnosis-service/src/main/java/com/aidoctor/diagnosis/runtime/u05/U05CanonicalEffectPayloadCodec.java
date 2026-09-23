package com.aidoctor.diagnosis.runtime.u05;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Deterministic U05-owned encoder for opaque Canonical Effect Ledger payloads.
 *
 * <p>The shared ledger never parses these bytes. Replay reconciliation compares
 * the exact U05 candidate fingerprint/schema/payload against the immutable
 * record already stored in the generic ledger.</p>
 */
final class U05CanonicalEffectPayloadCodec {
    static final String ADMISSION_SCHEMA = "U05-ADMISSION-LEDGER-V1";
    static final String ROUTING_SCHEMA = "U05-ROUTING-LEDGER-V1";
    static final String ELIGIBILITY_SCHEMA = "U05-ELIGIBILITY-LEDGER-V1";

    private U05CanonicalEffectPayloadCodec() {}

    static byte[] admission(U05AdmittedInput input) {
        if (input == null) throw new IllegalArgumentException("input is required");
        Writer w = new Writer(ADMISSION_SCHEMA);
        w.string("admissionId", input.getAdmissionId());
        w.string("consultationId", input.getConsultationId());
        w.string("cdpId", input.getCdpId());
        w.integer("clinicalStateVersion", input.getClinicalStateVersion());
        w.string("evaluationContext", input.getEvaluationContext());
        w.string("acceptedU04GateRef", input.getAcceptedU04GateRef());
        w.string("acceptedRouteAuthorizationType", input.getAcceptedRouteAuthorizationType());
        w.string("acceptedRouteAuthorizationRef", input.getAcceptedRouteAuthorizationRef());
        w.string("acceptedRouteSourceRef", input.getAcceptedRouteSourceRef());
        w.string("acceptedReadinessInputManifestRef", input.getAcceptedReadinessInputManifestRef());
        w.string("acceptedReadinessInputSetIdentity", input.getAcceptedReadinessInputSetIdentity());
        w.strings("acceptedReadinessInputRefs", input.getAcceptedReadinessInputRefs());
        w.string("acceptedRestrictedContextRef", input.getAcceptedRestrictedContextRef());
        w.string("acceptedRestrictedPermissionRef", input.getAcceptedRestrictedPermissionRef());
        w.string("canonicalEventRef", input.getCanonicalEventRef());
        w.string("businessEventIdentity", input.getBusinessEventIdentity());
        w.string("correlationId", input.getCorrelationId());
        w.string("traceId", input.getTraceId());
        w.string("admissionContractVersion", input.getAdmissionContractVersion());
        w.string("environmentId", input.getEnvironmentId());
        w.string("gateValue", input.getGateValue());
        w.string("admittedCreatedAt", input.getAdmittedCreatedAt());
        w.string("sourceCurrentReadinessRecordRef", input.getSourceCurrentReadinessRecordRef());
        U05ReadinessInputManifest manifest = input.getManifest();
        w.string("manifestRdp05ContractVersion", manifest.getRdp05ContractVersion());
        for (U05ReadinessInput readinessInput : manifest.getInputs()) {
            w.string("manifestInputSemanticFingerprint", readinessInput.semanticFingerprint());
        }
        return w.bytes();
    }

    static byte[] routing(U05DownstreamRoutingDecision decision) {
        if (decision == null) throw new IllegalArgumentException("decision is required");
        Writer w = new Writer(ROUTING_SCHEMA);
        w.string("routingDecisionId", decision.getRoutingDecisionId());
        w.string("routingDecisionFingerprint", decision.getRoutingDecisionFingerprint());
        w.string("consultationId", decision.getConsultationId());
        w.string("cdpId", decision.getCdpId());
        w.string("sourceReadinessRecordRef", decision.getSourceReadinessRecordRef());
        w.string("sourceReadinessEffectId", decision.getSourceReadinessEffectId());
        w.string("sourceReadinessCommitEvidenceRef", decision.getSourceReadinessCommitEvidenceRef());
        w.string("sourceReadinessValue", decision.getSourceReadinessValue());
        w.integer("sourceReadinessDerivedFromVersion", decision.getSourceReadinessDerivedFromVersion());
        w.integer("sourceReadinessCommittedVersion", decision.getSourceReadinessCommittedVersion());
        // currentClinicalStateVersionAtRouting is attempt-local audit/currentness
        // evidence. Unrelated version advancement must not change durable route
        // identity or replay equality.
        w.string("sourceU04GateRef", decision.getSourceU04GateRef());
        w.string("gateValue", decision.getGateValue());
        w.string("sourceInboundRouteRef", decision.getSourceInboundRouteRef());
        w.string("restrictedContextRef", decision.getRestrictedContextRef());
        w.string("routingStatus", decision.getRoutingStatus());
        // Replay disposition is attempt-local, not immutable ledger material.
        w.string("candidateDownstreamConsequence", decision.getCandidateDownstreamConsequence());
        w.string("candidateTargetUnitId", decision.getCandidateTargetUnitId());
        w.string("downstreamConsequence", decision.getDownstreamConsequence());
        w.string("targetUnitId", decision.getTargetUnitId());
        w.string("downstreamPermissionDecisionRef", decision.getDownstreamPermissionDecisionRef());
        w.string("downstreamPermissionRef", decision.getDownstreamPermissionRef());
        w.string("routeEffectId", decision.getRouteEffectId());
        w.string("downstreamRouteAuthorizationId", decision.getDownstreamRouteAuthorizationId());
        w.string("failureHandoffRef", decision.getFailureHandoffRef());
        w.string("reasonCode", decision.getReasonCode());
        w.string("canonicalEventRef", decision.getCanonicalEventRef());
        w.string("businessEventIdentity", decision.getBusinessEventIdentity());
        w.string("correlationId", decision.getCorrelationId());
        w.string("traceId", decision.getTraceId());
        w.string("routingPolicyVersion", decision.getRoutingPolicyVersion());
        w.string("createdAt", decision.getCreatedAt());
        w.string("validity", decision.getValidity());
        return w.bytes();
    }

    static byte[] eligibility(U05DownstreamEligibility eligibility) {
        if (eligibility == null) throw new IllegalArgumentException("eligibility is required");
        Writer w = new Writer(ELIGIBILITY_SCHEMA);
        w.string("eligibilityId", eligibility.getEligibilityId());
        w.string("routingDecisionRef", eligibility.getRoutingDecisionRef());
        w.string("routeEffectId", eligibility.getRouteEffectId());
        w.string("consultationId", eligibility.getConsultationId());
        w.string("cdpId", eligibility.getCdpId());
        w.string("authoritativeReadinessRecordRef", eligibility.getAuthoritativeReadinessRecordRef());
        w.string("authoritativeReadinessEffectId", eligibility.getAuthoritativeReadinessEffectId());
        w.string("readinessCommitEvidenceRef", eligibility.getReadinessCommitEvidenceRef());
        w.string("readinessValue", eligibility.getReadinessValue());
        // currentClinicalStateVersion is attempt-local eligibility audit evidence
        // and is intentionally excluded from immutable ledger equality.
        w.string("downstreamConsequence", eligibility.getDownstreamConsequence());
        w.string("targetUnitId", eligibility.getTargetUnitId());
        w.string("currentU04GateRef", eligibility.getCurrentU04GateRef());
        w.string("restrictedContextRef", eligibility.getRestrictedContextRef());
        w.string("downstreamPermissionRef", eligibility.getDownstreamPermissionRef());
        w.string("canonicalEventRef", eligibility.getCanonicalEventRef());
        w.string("businessEventIdentity", eligibility.getBusinessEventIdentity());
        w.string("correlationId", eligibility.getCorrelationId());
        w.string("traceId", eligibility.getTraceId());
        w.string("routingPolicyVersion", eligibility.getRoutingPolicyVersion());
        w.string("validity", eligibility.getValidity());
        w.string("createdAt", eligibility.getCreatedAt());
        return w.bytes();
    }

    static String eligibilityFingerprint(U05DownstreamEligibility eligibility) {
        return U05Ids.hash(
                "u05-eligibility-ledger",
                eligibility.getEligibilityId(),
                eligibility.getRoutingDecisionRef(),
                eligibility.getRouteEffectId(),
                eligibility.getAuthoritativeReadinessRecordRef(),
                eligibility.getAuthoritativeReadinessEffectId(),
                eligibility.getReadinessCommitEvidenceRef(),
                eligibility.getReadinessValue(),
                eligibility.getDownstreamConsequence(),
                eligibility.getTargetUnitId(),
                eligibility.getCurrentU04GateRef(),
                eligibility.getRestrictedContextRef(),
                eligibility.getDownstreamPermissionRef(),
                eligibility.getCanonicalEventRef(),
                eligibility.getBusinessEventIdentity(),
                eligibility.getRoutingPolicyVersion(),
                eligibility.getValidity());
    }

    private static final class Writer {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final DataOutputStream out = new DataOutputStream(buffer);

        Writer(String schema) {
            string("schema", schema);
        }

        void integer(String name, int value) {
            string(name, String.valueOf(value));
        }

        void strings(String name, List<String> values) {
            if (values == null) throw new IllegalArgumentException(name + " is required");
            integer(name + ".size", values.size());
            for (String value : values) string(name + ".item", value);
        }

        void string(String name, String value) {
            try {
                writeUtf8(name);
                out.writeBoolean(value != null);
                if (value != null) writeUtf8(value);
            } catch (IOException exception) {
                throw new IllegalStateException("U05 canonical payload encoding failed", exception);
            }
        }

        byte[] bytes() {
            try {
                out.flush();
                return buffer.toByteArray();
            } catch (IOException exception) {
                throw new IllegalStateException("U05 canonical payload finalization failed", exception);
            }
        }

        private void writeUtf8(String value) throws IOException {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            out.writeInt(bytes.length);
            out.write(bytes);
        }
    }
}
