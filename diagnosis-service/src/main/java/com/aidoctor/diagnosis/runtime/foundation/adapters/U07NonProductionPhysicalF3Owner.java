package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort;
import java.util.Map;

/**
 * Physical NON-PRODUCTION F3 state owner, reusing the existing atomic CDP
 * repository. A separately approved clinical decision prepares the F3 patch;
 * this class cannot create its own medical assessment or safety approval.
 * It is NOT a production F3 authority.
 */
public final class U07NonProductionPhysicalF3Owner
        implements U07NonProductionF3A1U05Flow.F3Owner {
    public interface F3DecisionProvider {
        PreparedF3 prepare(String consultationId, String cdpId, int currentVersion);
    }

    public static final class PreparedF3 {
        public final String effectId, patchId, idempotencyKey;
        public final StateTypes.StatePatch patch;

        public PreparedF3(String effectId, String patchId, String idempotencyKey,
                StateTypes.StatePatch patch) {
            if (blank(effectId) || blank(patchId) || blank(idempotencyKey) || patch == null)
                throw new IllegalArgumentException("U07_F3_PREPARED_DECISION_INVALID");
            this.effectId = effectId;
            this.patchId = patchId;
            this.idempotencyKey = idempotencyKey;
            this.patch = patch;
        }
    }

    private final StateRepositoryPort state;
    private final CDPRepository cdp;
    private final F3DecisionProvider decisions;

    public U07NonProductionPhysicalF3Owner(StateRepositoryPort state, CDPRepository cdp,
            F3DecisionProvider decisions) {
        if (state == null || cdp == null || decisions == null)
            throw new IllegalArgumentException("U07_F3_PHYSICAL_DEPENDENCY_MISSING");
        this.state = state;
        this.cdp = cdp;
        this.decisions = decisions;
    }

    @Override
    public U07NonProductionF3A1U05Flow.F3Committed executeAndReadback(
            String consultationId, String cdpId) {
        if (blank(consultationId) || blank(cdpId))
            throw new IllegalArgumentException("U07_F3_IDENTITY_REQUIRED");
        int version = state.readCurrentVersion(cdpId);
        PreparedF3 decision = decisions.prepare(consultationId, cdpId, version);
        if (decision == null || decision.patch.operations == null
                || decision.patch.operations.isEmpty())
            throw new IllegalStateException("U07_F3_APPROVED_PATCH_REQUIRED");
        // The owner only accepts a dedicated F3 assessment mutation, never
        // a generic patient/permission/safety patch smuggled into this boundary.
        if (decision.patch.operations.size() != 1
                || !"/patient_state/f3_gap_assessment".equals(
                        decision.patch.operations.get(0).path))
            throw new IllegalStateException("U07_F3_PATCH_SCOPE_INVALID");
        Object payload = decision.patch.operations.get(0).value;
        if (!(payload instanceof Map<?, ?>)
                || !decision.effectId.equals(
                        ((Map<?, ?>) payload).get("f3_canonical_effect_id")))
            throw new IllegalStateException("U07_F3_PATCH_EFFECT_MISMATCH");

        StateRepositoryPort.AtomicCommitOutcome committed = state.attemptAtomicCommit(
                new StateRepositoryPort.AtomicCommitCommand(cdpId, version,
                        decision.patchId, decision.idempotencyKey, decision.patch));
        if (committed == null
                || committed.status != StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED
                || committed.committedVersion != version + 1)
            throw new IllegalStateException("U07_F3_PHYSICAL_COMMIT_NOT_COMPLETE");

        CDP current = cdp.findById(cdpId)
                .orElseThrow(() -> new IllegalStateException("U07_F3_CDP_READBACK_MISSING"));
        Map<String, Object> patient = current.getPatientState();
        Object assessment = patient == null ? null : patient.get("f3_gap_assessment");
        Object persistedEffect = assessment instanceof Map<?, ?>
                ? ((Map<?, ?>) assessment).get("f3_canonical_effect_id") : null;
        if (current.getVersion() == null
                || current.getVersion() != committed.committedVersion
                || !decision.effectId.equals(persistedEffect))
            throw new IllegalStateException("U07_F3_PHYSICAL_READBACK_MISMATCH");
        return new U07NonProductionF3A1U05Flow.F3Committed(
                consultationId, cdpId, decision.effectId, committed.committedVersion, true);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
