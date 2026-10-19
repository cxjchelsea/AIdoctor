package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository;
import com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class V1NonProductionDeliveredQuestionReadbackTest {
    private final RuntimeWaitCheckpointRepository checkpoints =
            mock(RuntimeWaitCheckpointRepository.class);
    private final CDPRepository cdps = mock(CDPRepository.class);
    private final V1NonProductionDeliveredQuestionReadback reader =
            new V1NonProductionDeliveredQuestionReadback(checkpoints, cdps);

    @Test void readsBackOnlyPersistedDeliveredQuestion() {
        fixture("DELIVERED_TO_USER", "delivery-effect", "consult-wait");
        U07NonProductionDeliveryWaitRecovery.WaitReceipt receipt =
                new U07NonProductionDeliveryWaitRecovery.WaitReceipt(
                        "consult-wait", "checkpoint", "eligibility");
        com.aidoctor.diagnosis.controller.V1NonProductionFirstRoundWaitController.DeliveredQuestion
                question = reader.require("consult", "run", "cdp", receipt);
        assertEquals("question", question.questionId);
        assertEquals("synthetic-content-ref", question.contentRef);
    }

    @Test void unconfirmedOrMismatchedDeliveryNeverReturnsQuestion() {
        fixture("SELECTED", "delivery-effect", "consult-wait");
        U07NonProductionDeliveryWaitRecovery.WaitReceipt receipt =
                new U07NonProductionDeliveryWaitRecovery.WaitReceipt(
                        "consult-wait", "checkpoint", "eligibility");
        assertThrows(IllegalStateException.class, () ->
                reader.require("consult", "run", "cdp", receipt));
        fixture("DELIVERED_TO_USER", "wrong-effect", "consult-wait");
        assertThrows(IllegalStateException.class, () ->
                reader.require("consult", "run", "cdp", receipt));
        fixture("DELIVERED_TO_USER", "delivery-effect", "wrong-wait");
        assertThrows(IllegalStateException.class, () ->
                reader.require("consult", "run", "cdp", receipt));
    }

    private void fixture(String status, String deliveryEffect, String waitEffect) {
        RuntimeWaitCheckpointRecord cp = mock(RuntimeWaitCheckpointRecord.class);
        when(cp.getCheckpointStatus()).thenReturn("ACTIVE");
        when(cp.getConsultationId()).thenReturn("consult");
        when(cp.getRunId()).thenReturn("run");
        when(cp.getQuestionId()).thenReturn("question");
        when(cp.getQuestionSelectionEffectId()).thenReturn("selection-effect");
        when(cp.getQuestionDeliveryEffectId()).thenReturn("delivery-effect");
        when(cp.getDeliveryId()).thenReturn("delivery");
        when(cp.getConsultationWaitEffectId()).thenReturn(waitEffect);
        when(cp.getClinicalStateVersion()).thenReturn(8);
        when(checkpoints.findById("checkpoint")).thenReturn(Optional.of(cp));
        CDP cdp = mock(CDP.class);
        when(cdp.getVersion()).thenReturn(8);
        Map<String,Object> q = new LinkedHashMap<>();
        q.put("status", status);
        q.put("selection_effect_id", "selection-effect");
        q.put("delivery_effect_id", deliveryEffect);
        q.put("delivery_id", "delivery");
        q.put("content_ref", "synthetic-content-ref");
        Map<String,Object> questions = new LinkedHashMap<>();
        questions.put("question", q);
        Map<String,Object> state = new LinkedHashMap<>();
        state.put("questions", questions);
        when(cdp.getPatientState()).thenReturn(state);
        when(cdps.findById("cdp")).thenReturn(Optional.of(cdp));
    }
}
