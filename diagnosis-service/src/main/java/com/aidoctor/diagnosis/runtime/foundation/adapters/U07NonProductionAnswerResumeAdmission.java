package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRecord;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRepository;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionCommand;
import java.util.Collections;
import java.util.Objects;

/**
 * Non-production read-only F8 answer admission. This is NOT an F8 consume,
 * checkpoint release, new Run/Event issuance, or U02 mutation authority.
 * The caller must supply a separately issued trusted answer event and a
 * durable F8 consumer before executing the returned U02 command.
 */
public final class U07NonProductionAnswerResumeAdmission {
    private final ConsultationRepository consultations;
    private final RuntimeThreadStateRepository threads;
    private final RuntimeWaitCheckpointRepository checkpoints;
    private final CDPRepository cdps;

    public U07NonProductionAnswerResumeAdmission(
            ConsultationRepository consultations, RuntimeThreadStateRepository threads,
            RuntimeWaitCheckpointRepository checkpoints, CDPRepository cdps) {
        this.consultations = Objects.requireNonNull(consultations);
        this.threads = Objects.requireNonNull(threads);
        this.checkpoints = Objects.requireNonNull(checkpoints);
        this.cdps = Objects.requireNonNull(cdps);
    }

    public U02ExecutionCommand requireWaitingAnswer(
            String consultationId, String cdpId, String threadId, String waitingRunId,
            String checkpointId, String questionId, String deliveryId,
            String userId, String newEventId, String answerText,
            String correlationId, String traceId) {
        if (blank(consultationId) || blank(cdpId) || blank(threadId)
                || blank(waitingRunId) || blank(checkpointId) || blank(questionId)
                || blank(deliveryId) || blank(userId) || blank(newEventId)
                || blank(answerText) || blank(correlationId) || blank(traceId))
            throw blocked("ANSWER_INPUT_INCOMPLETE");

        ConsultationRecord consultation = consultations.findById(consultationId)
                .orElseThrow(() -> blocked("CONSULTATION_MISSING"));
        RuntimeThreadStateRecord thread = threads.findById(threadId)
                .orElseThrow(() -> blocked("THREAD_MISSING"));
        RuntimeWaitCheckpointRecord checkpoint = checkpoints.findById(checkpointId)
                .orElseThrow(() -> blocked("CHECKPOINT_MISSING"));
        CDP cdp = cdps.findById(cdpId)
                .orElseThrow(() -> blocked("CDP_MISSING"));

        if (!ConsultationRecord.WAITING_USER.equals(consultation.getLifecycleStatus())
                || !RuntimeThreadStateRecord.AWAITING_USER.equals(thread.getRuntimeStatus())
                || !Objects.equals(consultationId, thread.getConsultationId())
                || !Objects.equals(cdpId, consultation.getCdpId())
                || !Objects.equals(userId, consultation.getUserId())
                || !Objects.equals(consultationId, checkpoint.getConsultationId())
                || !Objects.equals(threadId, checkpoint.getThreadId())
                || !Objects.equals(waitingRunId, checkpoint.getRunId())
                || !Objects.equals(waitingRunId, thread.getCurrentRunId())
                || !Objects.equals(checkpointId, thread.getCurrentWaitCheckpointId())
                || !Objects.equals(questionId, checkpoint.getQuestionId())
                || !Objects.equals(deliveryId, checkpoint.getDeliveryId())
                || !Objects.equals(consultation.getCurrentWaitEffectId(),
                        checkpoint.getConsultationWaitEffectId())
                || !Objects.equals(thread.getCurrentWaitEffectId(),
                        checkpoint.getQuestionDeliveredWaitEffectId())
                || cdp.getVersion() == null
                || checkpoint.getClinicalStateVersion() == null
                || !cdp.getVersion().equals(checkpoint.getClinicalStateVersion()))
            throw blocked("WAIT_BINDING_OR_VERSION_MISMATCH");

        // Diagnostic candidate only. No authority to consume F8 or to submit U02.
        return new U02ExecutionCommand(userId, consultationId, threadId,
                waitingRunId, newEventId, cdpId, answerText, "PATIENT_ANSWER",
                cdp.getVersion().intValue(), correlationId, traceId,
                Collections.emptyList());
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static IllegalStateException blocked(String why) {
        return new IllegalStateException("U07_F8_ANSWER_BLOCKED:" + why);
    }
}
