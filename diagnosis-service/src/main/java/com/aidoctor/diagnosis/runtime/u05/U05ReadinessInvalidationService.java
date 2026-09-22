package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.StateCommitter;

import java.util.Map;

/**
 * K09/P01 adapter for the frozen readiness invalidation proposal.
 *
 * <p>Mechanical COMMITTED is never promoted by itself. Authoritative
 * non-production invalidation evidence exists only after exact synthetic-state
 * read-back verifies the stale readiness record.</p>
 */
public final class U05ReadinessInvalidationService {
    private final StateCommitter stateCommitter;
    private final U05ClinicalReadinessSnapshotPort snapshotPort;

    public U05ReadinessInvalidationService(
            StateCommitter stateCommitter,
            U05ClinicalReadinessSnapshotPort snapshotPort) {
        if (stateCommitter == null) throw new IllegalArgumentException("stateCommitter is required");
        if (snapshotPort == null) throw new IllegalArgumentException("snapshotPort is required");
        this.stateCommitter = stateCommitter;
        this.snapshotPort = snapshotPort;
    }

    public StateTypes.CommitResult commitMechanicalNonProduction(
            U05ReadinessInvalidationRequest request,
            U05ReadinessInvalidationProposal proposal) {
        validate(request, proposal);
        return stateCommitter.commit(proposal.getStatePatch());
    }

    public U05ReadinessInvalidationEvidence commitAndVerifyNonProduction(
            U05ReadinessInvalidationRequest request,
            U05ReadinessInvalidationProposal proposal) {
        StateTypes.CommitResult result = commitMechanicalNonProduction(request, proposal);
        if (!"COMMITTED".equals(result.status) || result.committedVersion == null) {
            throw new IllegalStateException("readiness invalidation did not commit");
        }
        U05ClinicalReadinessSnapshot snapshot =
                snapshotPort.read(result.cdpId, result.committedVersion.intValue());
        return U05ReadinessInvalidationEvidence.fromVerifiedSnapshot(
                request, proposal, result, snapshot);
    }

    private static void validate(
            U05ReadinessInvalidationRequest request,
            U05ReadinessInvalidationProposal proposal) {
        if (request == null || proposal == null) {
            throw new IllegalArgumentException("invalidation inputs are required");
        }
        requireNonProduction(request.getEnvironmentId());
        StateTypes.StatePatch patch = proposal.getStatePatch();
        if (!request.getCdpId().equals(patch.cdpId)
                || patch.baseVersion == null
                || patch.baseVersion.intValue() != request.getBaseClinicalStateVersion()
                || patch.operations == null
                || patch.operations.size() != 1
                || !"REPLACE".equals(patch.operations.get(0).op)
                || !U05ReadinessStateProposalFactory.READINESS_PATH.equals(patch.operations.get(0).path)) {
            throw new IllegalStateException("invalid readiness invalidation proposal");
        }
        Object raw = patch.operations.get(0).value;
        if (!(raw instanceof Map<?, ?>)) {
            throw new IllegalStateException("invalidation payload must preserve readiness record structure");
        }
        Map<?, ?> value = (Map<?, ?>) raw;
        requireValue(value, "readiness_record_id", request.getPriorReadinessRecordRef());
        requireValue(value, "effect_id", request.getPriorReadinessEffectId());
        requireValue(value, "state_validity", "STALE");
        requireValue(value, "invalidation_effect_ref", proposal.getInvalidationEffectId());
    }

    private static void requireValue(Map<?, ?> value, String key, Object expected) {
        if (!expected.equals(value.get(key))) {
            throw new IllegalStateException("invalidation payload " + key + " mismatch");
        }
    }

    private static void requireNonProduction(String environment) {
        String normalized = environment == null ? "" : environment.trim().toLowerCase();
        if (normalized.isEmpty()
                || "prod".equals(normalized)
                || "production".equals(normalized)
                || normalized.startsWith("prod-")
                || normalized.startsWith("production-")) {
            throw new IllegalStateException("production invalidation is not authorized for U05");
        }
    }
}
