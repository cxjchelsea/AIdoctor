package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.controller.V1NonProductionFirstRoundWaitController;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository;
import com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery;
import java.util.Map;
import java.util.Objects;

/**
 * Exact persisted WAIT -> delivered U06 question readback.
 * Test-only helper: it does not issue delivery, authorization or user identity.
 * The independent FirstRoundOwner still authenticates the caller.
 */
public final class V1NonProductionDeliveredQuestionReadback {
    private final RuntimeWaitCheckpointRepository checkpoints;
    private final CDPRepository cdps;

    public V1NonProductionDeliveredQuestionReadback(
            RuntimeWaitCheckpointRepository checkpoints, CDPRepository cdps) {
        this.checkpoints = Objects.requireNonNull(checkpoints);
        this.cdps = Objects.requireNonNull(cdps);
    }

    public V1NonProductionFirstRoundWaitController.DeliveredQuestion require(
            String consultationId, String runId, String cdpId,
            U07NonProductionDeliveryWaitRecovery.WaitReceipt wait) {
        if (wait == null || wait.checkpointId == null || wait.waitEffectId == null)
            throw blocked("WAIT_PROOF_MISSING");
        RuntimeWaitCheckpointRecord cp = checkpoints.findById(wait.checkpointId)
                .orElseThrow(() -> blocked("CHECKPOINT_MISSING"));
        CDP cdp = cdps.findById(cdpId)
                .orElseThrow(() -> blocked("CDP_MISSING"));
        if (!"ACTIVE".equals(cp.getCheckpointStatus())
                || !Objects.equals(cp.getConsultationId(), consultationId)
                || !Objects.equals(cp.getRunId(), runId)
                || !Objects.equals(cp.getQuestionDeliveredWaitEffectId(), wait.waitEffectId)
                || !Objects.equals(cp.getClinicalStateVersion(), cdp.getVersion()))
            throw blocked("CHECKPOINT_AUTHORITY_MISMATCH");
        Map<String, Object> state = cdp.getPatientState();
        Object raw = state == null ? null : state.get("questions");
        Object selected = raw instanceof Map<?, ?>
                ? ((Map<?, ?>) raw).get(cp.getQuestionId()) : null;
        if (!(selected instanceof Map<?, ?>)) throw blocked("QUESTION_MISSING");
        Map<?, ?> question = (Map<?, ?>) selected;
        if (!"DELIVERED_TO_USER".equals(question.get("status"))
                || !Objects.equals(cp.getQuestionDeliveryEffectId(),
                        question.get("delivery_effect_id"))
                || !Objects.equals(cp.getDeliveryId(), question.get("delivery_id"))
                || !Objects.equals(cp.getQuestionSelectionEffectId(),
                        question.get("selection_effect_id")))
            throw blocked("QUESTION_DELIVERY_MISMATCH");
        Object content = question.get("content_ref");
        if (!(content instanceof String) || ((String) content).trim().isEmpty())
            throw blocked("QUESTION_CONTENT_REF_MISSING");
        return new V1NonProductionFirstRoundWaitController.DeliveredQuestion(
                cp.getQuestionId(), (String) content);
    }

    private static IllegalStateException blocked(String reason) {
        return new IllegalStateException("V1_QUESTION_READBACK_BLOCKED:" + reason);
    }
}
