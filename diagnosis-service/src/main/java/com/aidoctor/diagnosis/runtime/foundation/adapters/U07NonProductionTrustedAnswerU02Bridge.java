package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.runtime.foundation.CanonicalBusinessEventRecord;
import com.aidoctor.diagnosis.runtime.foundation.CanonicalBusinessEventRepository;
import com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRecord;
import com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRepository;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionCommand;
import java.util.Objects;

/**
 * Fail-closed new-Run/Event identity gate for an answer after F8 admission.
 * No F8 consumption, permission issuance, clinical interpretation or mutation
 * occurs here. Merely persisting an event/run does NOT prove F8 was consumed:
 * the caller must provide independent durable F8 consumption authority.
 */
public final class U07NonProductionTrustedAnswerU02Bridge {
    public interface F8ConsumptionAuthority {
        boolean consumedFor(String consultationId, String waitingRunId,
                String checkpointId, String newRunId, String answerEventId);
    }

    private final ClinicalRunRepository runs;
    private final CanonicalBusinessEventRepository events;
    private final F8ConsumptionAuthority consumed;

    public U07NonProductionTrustedAnswerU02Bridge(ClinicalRunRepository runs,
            CanonicalBusinessEventRepository events, F8ConsumptionAuthority consumed) {
        this.runs = Objects.requireNonNull(runs);
        this.events = Objects.requireNonNull(events);
        this.consumed = Objects.requireNonNull(consumed);
    }

    public U02ExecutionCommand requireAuthorizedNewRun(
            U02ExecutionCommand waitingCandidate, String checkpointId,
            String newRunId) {
        if (waitingCandidate == null || blank(checkpointId) || blank(newRunId))
            throw blocked("INPUT_MISSING");
        if (newRunId.equals(waitingCandidate.runId))
            throw blocked("WAITING_RUN_REUSE");
        ClinicalRunRecord run = runs.findById(newRunId)
                .orElseThrow(() -> blocked("NEW_RUN_MISSING"));
        CanonicalBusinessEventRecord event = events.findById(waitingCandidate.eventId)
                .orElseThrow(() -> blocked("ANSWER_EVENT_MISSING"));
        if (!ClinicalRunRecord.OPEN.equals(run.getStatus())
                || !Objects.equals(run.getConsultationId(), waitingCandidate.consultationId)
                || !Objects.equals(run.getThreadId(), waitingCandidate.threadId)
                || !Objects.equals(run.getEventId(), waitingCandidate.eventId)
                || !Objects.equals(run.getBasedOnClinicalStateVersion(),
                        Integer.valueOf(waitingCandidate.baseClinicalStateVersion))
                || !Objects.equals(event.getConsultationId(), waitingCandidate.consultationId)
                || !"PATIENT_ANSWER".equals(event.getEventType()))
            throw blocked("NEW_RUN_OR_EVENT_MISMATCH");
        if (!consumed.consumedFor(waitingCandidate.consultationId,
                waitingCandidate.runId, checkpointId, newRunId, waitingCandidate.eventId))
            throw blocked("F8_DURABLE_CONSUMPTION_NOT_CONFIRMED");
        return new U02ExecutionCommand(waitingCandidate.userId,
                waitingCandidate.consultationId, waitingCandidate.threadId,
                newRunId, waitingCandidate.eventId, waitingCandidate.cdpId,
                waitingCandidate.rawClinicalText, waitingCandidate.sourceType,
                waitingCandidate.baseClinicalStateVersion,
                waitingCandidate.correlationId, waitingCandidate.traceId,
                waitingCandidate.registeredDependencies);
    }

    private static boolean blank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private static IllegalStateException blocked(String why) {
        return new IllegalStateException("U07_F8_U02_BLOCKED:" + why);
    }
}
