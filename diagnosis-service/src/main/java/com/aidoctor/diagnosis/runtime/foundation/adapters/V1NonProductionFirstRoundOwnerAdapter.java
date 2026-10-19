package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.controller.V1NonProductionFirstRoundWaitController;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionResult;
import com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery;
import java.util.Objects;

/**
 * Composes independent host authorities and persisted WAIT readback.
 * Neither status authority nor delivered-question confirmation is synthesized.
 */
public final class V1NonProductionFirstRoundOwnerAdapter
        implements V1NonProductionFirstRoundWaitController.FirstRoundOwner {
    public interface AuthorizedStateOwner {
        String requireCurrentStateRef(String consultationId, String runId,
                U02ExecutionResult committedFact);
        String requireCurrentCdpId(String consultationId, String runId);
    }
    public interface AuthorizedQuestionContent {
        String requireText(String consultationId, String runId,
                String questionId, String contentRef);
    }

    public interface DeliveryOwnerAuthority {
        U07NonProductionPhysicalFollowupCoordinator.ConfirmedDeliveryOwner
                requireConfirmedDeliveryOwner(String consultationId, String runId);
    }

    private final AuthorizedStateOwner state;
    private final DeliveryOwnerAuthority delivery;
    private final AuthorizedQuestionContent content;
    private final V1NonProductionDeliveredQuestionReadback readback;

    public V1NonProductionFirstRoundOwnerAdapter(AuthorizedStateOwner state,
            DeliveryOwnerAuthority delivery, V1NonProductionDeliveredQuestionReadback readback,
            AuthorizedQuestionContent content) {
        this.state=Objects.requireNonNull(state);
        this.delivery=Objects.requireNonNull(delivery);
        this.readback=Objects.requireNonNull(readback);
        this.content=Objects.requireNonNull(content);
    }

    @Override
    public String requireAuthorizedQuestionText(String consultationId, String runId,
            V1NonProductionFirstRoundWaitController.DeliveredQuestion question) {
        if (question == null)
            throw new IllegalStateException("V1_FIRST_ROUND_QUESTION_REQUIRED");
        String text = content.requireText(consultationId, runId,
                question.questionId, question.contentRef);
        if (text == null || text.trim().isEmpty())
            throw new IllegalStateException("V1_FIRST_ROUND_QUESTION_CONTENT_UNAVAILABLE");
        return text;
    }

    @Override
    public String requireCurrentAuthoritativeStateRef(String consultationId,
            String runId, U02ExecutionResult fact) {
        String ref=state.requireCurrentStateRef(consultationId,runId,fact);
        if(ref==null || ref.trim().isEmpty())
            throw new IllegalStateException("V1_FIRST_ROUND_STATE_AUTHORITY_MISSING");
        return ref;
    }

    @Override
    public U07NonProductionPhysicalFollowupCoordinator.ConfirmedDeliveryOwner
            requireIndependentConfirmedDeliveryOwner(String consultationId,String runId) {
        U07NonProductionPhysicalFollowupCoordinator.ConfirmedDeliveryOwner owner=
                delivery.requireConfirmedDeliveryOwner(consultationId,runId);
        if(owner==null) throw new IllegalStateException("V1_FIRST_ROUND_DELIVERY_AUTHORITY_MISSING");
        return owner;
    }

    @Override
    public V1NonProductionFirstRoundWaitController.DeliveredQuestion requireDeliveredQuestion(
            String consultationId, String runId, String checkpointId,
            U07NonProductionDeliveryWaitRecovery.WaitReceipt wait) {
        if(wait==null || !Objects.equals(checkpointId,wait.checkpointId))
            throw new IllegalStateException("V1_FIRST_ROUND_WAIT_BINDING_MISMATCH");
        String cdpId=state.requireCurrentCdpId(consultationId,runId);
        if(cdpId==null || cdpId.trim().isEmpty())
            throw new IllegalStateException("V1_FIRST_ROUND_CDP_AUTHORITY_MISSING");
        return readback.require(consultationId,runId,cdpId,wait);
    }
}
