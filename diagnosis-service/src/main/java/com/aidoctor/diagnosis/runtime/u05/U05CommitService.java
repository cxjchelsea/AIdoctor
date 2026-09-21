package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.StateCommitter;

import java.util.Map;

/** Narrow U05 adapter over the existing governed G2/P01 StateCommitter boundary. */
public final class U05CommitService {
    private final StateCommitter stateCommitter;

    public U05CommitService(StateCommitter stateCommitter) {
        if (stateCommitter == null) throw new IllegalArgumentException("stateCommitter is required");
        this.stateCommitter = stateCommitter;
    }

    public StateTypes.CommitResult commitNonProduction(
            U05AdmittedInput input,
            U05ClinicalReadinessDecision decision,
            U05ReadinessStateProposal proposal) {
        if (input == null || decision == null || proposal == null) {
            throw new IllegalArgumentException("U05 commit inputs are required");
        }
        requireNonProduction(input.getEnvironmentId());
        validate(input, decision, proposal);
        return stateCommitter.commit(proposal.getStatePatch());
    }

    private static void validate(
            U05AdmittedInput input,
            U05ClinicalReadinessDecision decision,
            U05ReadinessStateProposal proposal) {
        if (!decision.isDecided()) throw new IllegalStateException("non-DECIDED D03 cannot commit");
        if (!decision.getDecisionId().equals(proposal.getSourceDecisionRef())) {
            throw new IllegalStateException("proposal source D03 mismatch");
        }
        if (!input.getAdmissionId().equals(decision.getSourceAdmissionRef())) {
            throw new IllegalStateException("D03 admission mismatch");
        }
        if (!input.getAcceptedReadinessInputSetIdentity().equals(decision.getSourceReadinessInputSetIdentity())) {
            throw new IllegalStateException("D03 input-set mismatch");
        }

        StateTypes.StatePatch patch = proposal.getStatePatch();
        if (patch.baseVersion == null || patch.baseVersion.intValue() != input.getClinicalStateVersion()) {
            throw new IllegalStateException("U05 proposal baseVersion mismatch");
        }
        if (!input.getCdpId().equals(patch.cdpId)) {
            throw new IllegalStateException("U05 proposal CDP mismatch");
        }
        if (patch.envelope == null
                || !input.getCorrelationId().equals(patch.envelope.correlationId)
                || !input.getTraceId().equals(patch.envelope.traceId)
                || !"P01".equals(patch.envelope.capabilityId)) {
            throw new IllegalStateException("U05 proposal correlation/P01 mismatch");
        }
        if (patch.operations == null
                || patch.operations.size() != 1
                || !U05ReadinessStateProposalFactory.READINESS_PATH.equals(patch.operations.get(0).path)) {
            throw new IllegalStateException("unsupported U05 proposal operation set");
        }
        Object raw = patch.operations.get(0).value;
        if (!(raw instanceof Map<?, ?>)) throw new IllegalStateException("U05 readiness payload malformed");
        Map<?, ?> value = (Map<?, ?>) raw;

        requireValue(value, "readiness_record_id", proposal.getReadinessRecordId());
        requireValue(value, "clinical_readiness", decision.getClinicalReadiness());
        requireValue(value, "source_admission_id", input.getAdmissionId());
        requireValue(value, "source_d03_decision_ref", decision.getDecisionId());
        requireValue(value, "source_readiness_input_set_identity", input.getAcceptedReadinessInputSetIdentity());
        requireValue(value, "source_u04_gate_ref", input.getAcceptedU04GateRef());
        requireValue(value, "source_route_authorization_type", input.getAcceptedRouteAuthorizationType());
        requireValue(value, "source_route_authorization_ref", input.getAcceptedRouteAuthorizationRef());
        requireValue(value, "effect_id", proposal.getEffectId());
        requireValue(value, "proposal_ref", proposal.getProposalId());
        requireValue(value, "canonical_payload_fingerprint", proposal.getCanonicalPayloadFingerprint());

        if (!equal(input.getAcceptedRestrictedContextRef(), value.get("source_restricted_context_ref"))) {
            throw new IllegalStateException("U05 proposal restricted context mismatch");
        }
        if (!equal(input.getAcceptedRestrictedPermissionRef(), value.get("source_restricted_permission_ref"))) {
            throw new IllegalStateException("U05 proposal restricted permission mismatch");
        }
    }

    private static void requireValue(Map<?, ?> value, String field, Object expected) {
        if (!expected.equals(value.get(field))) {
            throw new IllegalStateException("U05 proposal " + field + " mismatch");
        }
    }

    private static boolean equal(String expected, Object actual) {
        return expected == null ? actual == null : expected.equals(actual);
    }

    private static void requireNonProduction(String environment) {
        if (environment == null || environment.trim().isEmpty()) throw new IllegalArgumentException("environment is required");
        String normalized = environment.trim().toLowerCase();
        if ("prod".equals(normalized)
                || "production".equals(normalized)
                || normalized.startsWith("prod-")
                || normalized.startsWith("production-")) {
            throw new IllegalStateException("production mutation is not authorized for U05");
        }
    }
}
