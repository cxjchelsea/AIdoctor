package com.aidoctor.diagnosis.runtime.u06;

import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitTransitionService;
import com.aidoctor.diagnosis.runtime.u06.wait.U06WaitCoordinator;
import java.time.OffsetDateTime;

/**
 * Existing governed U06 consultation and runtime waiting services bound to the
 * durable delivery recovery interface. Both services must use persistent
 * repositories in the integrating host; no simulated WAITING_USER is issued.
 */
public final class U07ExistingU06WaitAuthority
        implements U07NonProductionDeliveryWaitRecovery.WaitAuthority {
    private final ConsultationWaitTransitionService consultation;
    private final U06WaitCoordinator runtime;

    public U07ExistingU06WaitAuthority(ConsultationWaitTransitionService consultation,
            U06WaitCoordinator runtime) {
        if (consultation == null || runtime == null)
            throw new IllegalArgumentException("U07_DURABLE_WAIT_HOST_REQUIRED");
        this.consultation = consultation;
        this.runtime = runtime;
    }

    @Override
    public U07NonProductionDeliveryWaitRecovery.WaitReceipt establish(
            U07NonProductionDeliveryWaitRecovery.Receipt receipt, int version) {
        U06ProfileBRequest r = receipt.request;
        U06SyntheticDecisionBundle d = receipt.decision;
        if (r.getThreadId() == null || r.getRunId() == null)
            throw new IllegalStateException("U07_WAIT_THREAD_ID_REQUIRED");
        String parent = U06Ids.hash("u06wait",r.getConsultationId(),d.getQuestionId(),
                d.getQuestionSelectionEffectId(),receipt.deliveryId,
                d.getQuestionContentFingerprint(),"1");
        String waitEffect = U06Ids.hash("u06consultwait",parent,"1");
        String waitFingerprint = U06Ids.hash("u06consultwaitfp",
                r.getConsultationId(),waitEffect,parent,d.getQuestionId(),
                receipt.deliveryId,String.valueOf(r.getExpectedConsultationRowVersion()),
                "WAITING_USER");
        ConsultationWaitTransitionService.Result waiting = consultation.establish(
                new ConsultationWaitTransitionService.Command(waitEffect,parent,
                        r.getConsultationId(),d.getQuestionId(),receipt.deliveryId,
                        waitFingerprint,U06Ids.hash("u06consultwaitidem",waitEffect,"1"),
                        r.getExpectedConsultationRowVersion(),
                        OffsetDateTime.parse(r.getCreatedAt()).toLocalDateTime()));
        String checkpoint = U06Ids.hash("u06checkpoint",parent,"1");
        String checkpointFingerprint = U06Ids.hash("u06checkpointfp",
                checkpoint,r.getThreadId(),r.getRunId(),d.getQuestionId(),
                receipt.deliveryId,String.valueOf(version),waitEffect);
        U06WaitCoordinator.Result durable = runtime.establish(
                new U06WaitCoordinator.Command(checkpoint,r.getConsultationId(),
                        r.getThreadId(),r.getRunId(),d.getQuestionId(),
                        "/patient_state/pending_question",d.getQuestionSelectionEffectId(),
                        receipt.deliveryEffectId,parent,receipt.deliveryId,
                        receipt.confirmationRef,version,waiting.waitEffectId,
                        r.getDependencyBindingRef(),r.getQuestionPolicyRef(),
                        checkpointFingerprint,r.getCreatedAt()));
        return new U07NonProductionDeliveryWaitRecovery.WaitReceipt(
                waiting.waitEffectId,durable.checkpointId,durable.eligibilityId);
    }
}
