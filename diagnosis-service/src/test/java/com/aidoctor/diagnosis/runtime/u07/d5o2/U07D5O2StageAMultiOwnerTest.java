package com.aidoctor.diagnosis.runtime.u07.d5o2;

import com.aidoctor.diagnosis.runtime.u01.*;
import com.aidoctor.diagnosis.runtime.u06.wait.*;
import com.aidoctor.diagnosis.runtime.foundation.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * True two-owner, real MySQL8 rollback cases under an explicit TEST-ONLY
 * outer TransactionTemplate. Does not claim original final method AOP advice.
 */
public class U07D5O2StageAMultiOwnerTest {
    private static String id() { return UUID.randomUUID().toString(); }
    private static final String NOW = "2026-10-12T10:00:00Z";

    private static int count(JdbcTemplate jdbc, String sql, String id) {
        return jdbc.queryForObject(sql, new Object[] {id}, Integer.class);
    }

    private static ConsultationRecord consultation(String consultationId) {
        U01StartCommand cmd = new U01StartCommand(id(), id(), "synthetic-user", "SELF",
            null, "synthetic-only", "IN_SCOPE", false);
        U01SemanticDecision semantic = new U01SemanticDecision(
            U01SemanticDecision.SUBJECT_RESOLVED, "SELF",
            U01SemanticDecision.PROBLEM_FRAMED, U01SemanticDecision.IN_SCOPE,
            null, "U02", false);
        return new ConsultationRecord(consultationId, id(), "synthetic-user",
            cmd, semantic, LocalDateTime.now());
    }

    @Test
    public void consultationAndWaitEffectRollBackTogether() {
        U07D5O2StageAFixtureConfig.requireEnvironment();
        try (AnnotationConfigApplicationContext ctx =
                 new AnnotationConfigApplicationContext(U07D5O2StageAFixtureConfig.class)) {
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);
            ConsultationRepository consultations = ctx.getBean(ConsultationRepository.class);
            ConsultationWaitEffectRepository effects = ctx.getBean(ConsultationWaitEffectRepository.class);
            String cid = id(), eid = id();
            tx.execute(s -> { consultations.saveAndFlush(consultation(cid)); return null; });
            ConsultationWaitTransitionService original = new ConsultationWaitTransitionService(consultations, effects);
            RuntimeException injected = assertThrows(RuntimeException.class, () -> tx.execute(s -> {
                original.establish(new ConsultationWaitTransitionService.Command(
                    eid, id(), cid, id(), id(), id(), id(), 0, LocalDateTime.now()));
                assertEquals(1, count(jdbc,
                    "SELECT COUNT(*) FROM clinical_consultation_wait_effect WHERE wait_effect_id=?", eid));
                throw new IllegalStateException("STAGE_A_TWO_OWNER_ROLLBACK");
            }));
            assertEquals("STAGE_A_TWO_OWNER_ROLLBACK", injected.getMessage());
            assertEquals(0, count(jdbc,
                "SELECT COUNT(*) FROM clinical_consultation_wait_effect WHERE wait_effect_id=?", eid));
            assertEquals("ACTIVE", jdbc.queryForObject(
                "SELECT lifecycle_status FROM clinical_consultation WHERE consultation_id=?",
                new Object[]{cid}, String.class));
            tx.execute(s -> { consultations.deleteById(cid); return null; });
        }
    }

    @Test
    public void checkpointAndThreadRollBackTogetherAndStageIsVisibleWhenCommitted() {
        U07D5O2StageAFixtureConfig.requireEnvironment();
        try (AnnotationConfigApplicationContext ctx =
                 new AnnotationConfigApplicationContext(U07D5O2StageAFixtureConfig.class)) {
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);
            RuntimeThreadStateRepository threads = ctx.getBean(RuntimeThreadStateRepository.class);
            RuntimeWaitCheckpointRepository cps = ctx.getBean(RuntimeWaitCheckpointRepository.class);
            RuntimeWaitCheckpointService original = new RuntimeWaitCheckpointService(threads, cps);
            String tid = id(), cid = id(), cp = id(), run = id(), parent = id();
            tx.execute(s -> {
                threads.saveAndFlush(new RuntimeThreadStateRecord(tid, cid, LocalDateTime.now()));
                return null;
            });
            RuntimeWaitCheckpointService.Command command = new RuntimeWaitCheckpointService.Command(
                cp,cid,tid,run,id(),"synthetic-pending",id(),id(),parent,id(),id(),1,
                id(),null,null,id(),NOW);
            RuntimeException injected = assertThrows(RuntimeException.class, () -> tx.execute(s -> {
                original.reserve(command);
                assertEquals(1, count(jdbc,
                    "SELECT COUNT(*) FROM clinical_runtime_wait_checkpoint WHERE checkpoint_id=?", cp));
                throw new IllegalStateException("STAGE_A_CHECKPOINT_ROLLBACK");
            }));
            assertEquals("STAGE_A_CHECKPOINT_ROLLBACK", injected.getMessage());
            assertEquals(0, count(jdbc,
                "SELECT COUNT(*) FROM clinical_runtime_wait_checkpoint WHERE checkpoint_id=?", cp));
            assertEquals("ACTIVE", jdbc.queryForObject(
                "SELECT runtime_status FROM clinical_runtime_thread_state WHERE thread_id=?",
                new Object[]{tid}, String.class));

            // This second phase is intentionally committed: not a complete WAIT_ESTABLISHED result.
            tx.execute(s -> { original.reserve(command); return null; });
            assertEquals("WAIT_CHECKPOINTED", jdbc.queryForObject(
                "SELECT runtime_status FROM clinical_runtime_thread_state WHERE thread_id=?",
                new Object[]{tid}, String.class));
            assertEquals(1, count(jdbc,
                "SELECT COUNT(*) FROM clinical_runtime_wait_checkpoint WHERE checkpoint_id=?", cp));
            tx.execute(s -> { cps.deleteById(cp); threads.deleteById(tid); return null; });
        }
    }
}
