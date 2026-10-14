package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.runtime.foundation.CanonicalBusinessEventLedger;
import com.aidoctor.diagnosis.runtime.foundation.CanonicalBusinessEventRecord;
import com.aidoctor.diagnosis.runtime.foundation.ClinicalRunCoordinator;
import com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRecord;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionCommand;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Nonproduction, transactionally durable F8 consume + canonical answer event
 * and fresh Run. Requires upstream read-only WAIT admission and a host-
 * authenticated answer. It does not mutate clinical facts or infer safety.
 * Deployment of this candidate table to production remains prohibited.
 */
public final class U07NonProductionDurableF8Consume
        implements U07NonProductionTrustedAnswerU02Bridge.F8ConsumptionAuthority {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final CanonicalBusinessEventLedger ledger;
    private final ClinicalRunCoordinator coordinator;

    public U07NonProductionDurableF8Consume(JdbcTemplate jdbc, TransactionTemplate tx,
            CanonicalBusinessEventLedger ledger, ClinicalRunCoordinator coordinator) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.tx = Objects.requireNonNull(tx);
        this.ledger = Objects.requireNonNull(ledger);
        this.coordinator = Objects.requireNonNull(coordinator);
    }

    /** Must only be called with an authenticated, canonicalized answer digest. */
    public String consume(U02ExecutionCommand waiting, String checkpointId,
            String idempotencyKey, String answerDigest) {
        if (waiting == null || empty(checkpointId) || empty(idempotencyKey)
                || empty(answerDigest) || !"PATIENT_ANSWER".equals(waiting.sourceType))
            throw new IllegalStateException("U07_F8_CONSUME_INPUT_INVALID");
        return tx.execute(status -> {
            List<String[]> old = jdbc.query(
                    "SELECT consultation_id,waiting_run_id,answer_event_id,"
                    + "answer_digest,new_run_id FROM u07_f8_answer_consumption "
                    + "WHERE checkpoint_id=? FOR UPDATE",
                    (rs,n) -> new String[]{rs.getString(1),rs.getString(2),
                            rs.getString(3),rs.getString(4),rs.getString(5)},checkpointId);
            if (!old.isEmpty()) {
                String[] x = old.get(0);
                if (!Objects.equals(x[0],waiting.consultationId)
                        || !Objects.equals(x[1],waiting.runId)
                        || !Objects.equals(x[2],waiting.eventId)
                        || !Objects.equals(x[3],answerDigest))
                    throw new IllegalStateException("U07_F8_REPLAY_CONFLICT");
                return x[4];
            }
            List<String[]> eligible = jdbc.query(
                    "SELECT c.consultation_id,t.current_run_id,w.run_id,w.clinical_state_version "
                    + "FROM clinical_consultation c JOIN clinical_runtime_thread_state t "
                    + "ON t.consultation_id=c.consultation_id "
                    + "JOIN clinical_runtime_wait_checkpoint w "
                    + "ON w.thread_id=t.thread_id AND w.checkpoint_id=t.current_wait_checkpoint_id "
                    + "WHERE c.consultation_id=? AND c.cdp_id=? AND t.thread_id=? "
                    + "AND w.checkpoint_id=? AND c.lifecycle_status='WAITING_USER' "
                    + "AND t.runtime_status='AWAITING_USER' FOR UPDATE",
                    (rs,n) -> new String[]{rs.getString(1),rs.getString(2),
                            rs.getString(3),rs.getString(4)},
                    waiting.consultationId,waiting.cdpId,waiting.threadId,checkpointId);
            if (eligible.size()!=1 || !Objects.equals(eligible.get(0)[1],waiting.runId)
                    || !Objects.equals(eligible.get(0)[2],waiting.runId)
                    || Integer.parseInt(eligible.get(0)[3])!=waiting.baseClinicalStateVersion)
                throw new IllegalStateException("U07_F8_WAIT_NOT_CURRENT");
            CanonicalBusinessEventRecord event = ledger.resolveOrCreate(
                    waiting.eventId,waiting.consultationId,
                    "PATIENT_ANSWER",idempotencyKey,answerDigest);
            if (!Objects.equals(event.getEventId(),waiting.eventId))
                throw new IllegalStateException("U07_F8_EVENT_REPLAY_MISMATCH");
            ClinicalRunRecord run = coordinator.openRun(
                    waiting.consultationId,waiting.eventId,waiting.baseClinicalStateVersion);
            if (Objects.equals(run.getRunId(),waiting.runId)
                    || !Objects.equals(run.getThreadId(),waiting.threadId))
                throw new IllegalStateException("U07_F8_NEW_RUN_MISMATCH");
            jdbc.update("INSERT INTO u07_f8_answer_consumption "
                    + "(checkpoint_id,consultation_id,waiting_run_id,answer_event_id,"
                    + "answer_digest,new_run_id) VALUES(?,?,?,?,?,?)",
                    checkpointId,waiting.consultationId,waiting.runId,waiting.eventId,
                    answerDigest,run.getRunId());
            return run.getRunId();
        });
    }

    @Override
    public boolean consumedFor(String consultationId, String waitingRunId,
            String checkpointId, String newRunId, String answerEventId) {
        if (empty(consultationId)||empty(waitingRunId)||empty(checkpointId)
                ||empty(newRunId)||empty(answerEventId)) return false;
        Integer count=jdbc.queryForObject(
                "SELECT COUNT(*) FROM u07_f8_answer_consumption WHERE "
                + "consultation_id=? AND waiting_run_id=? AND checkpoint_id=? "
                + "AND new_run_id=? AND answer_event_id=?",
                Integer.class,consultationId,waitingRunId,checkpointId,newRunId,answerEventId);
        return count != null && count.intValue()==1;
    }

    private static boolean empty(String s) { return s==null||s.trim().isEmpty(); }
}
