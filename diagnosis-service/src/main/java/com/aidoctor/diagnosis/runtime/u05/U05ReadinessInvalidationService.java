package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.StateCommitter;

import java.util.Map;

/**
 * Mechanical K09/P01 adapter for the frozen invalidation proposal.
 *
 * <p>A COMMITTED result here is not promoted to authoritative stale readiness
 * until a governed synthetic-state read-back verifies the record.</p>
 */
public final class U05ReadinessInvalidationService {
    private final StateCommitter stateCommitter;

    public U05ReadinessInvalidationService(StateCommitter stateCommitter) {
        if (stateCommitter == null) throw new IllegalArgumentException("stateCommitter is required");
        this.stateCommitter = stateCommitter;
    }

    public StateTypes.CommitResult commitMechanicalNonProduction(
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
        return stateCommitter.commit(patch);
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
