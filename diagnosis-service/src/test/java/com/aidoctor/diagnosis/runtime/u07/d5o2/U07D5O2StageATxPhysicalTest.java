package com.aidoctor.diagnosis.runtime.u07.d5o2;

import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Real disposable MySQL8 only. Assertions are candidate transaction facts,
 * never production authority, APPLIED, delivery, or resume eligibility.
 */
public class U07D5O2StageATxPhysicalTest {
    private static final String SQL =
        "SELECT COUNT(*) FROM clinical_runtime_thread_state WHERE thread_id = ?";

    private int separatelyCommittedCount(DataSource dataSource, String key) throws Exception {
        // Observer must never inherit the writer's transaction-bound ConnectionHolder.
        final java.util.concurrent.atomic.AtomicReference<Integer> out =
            new java.util.concurrent.atomic.AtomicReference<Integer>();
        final java.util.concurrent.atomic.AtomicReference<Throwable> failure =
            new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread observer = new Thread(() -> {
            try {
                out.set(new JdbcTemplate(dataSource).queryForObject(SQL,
                    new Object[] {key}, Integer.class));
            } catch (Throwable error) {
                failure.set(error);
            }
        }, "d5o2-isolated-readback");
        observer.start();
        observer.join(10000);
        assertFalse(observer.isAlive(), "Observer timeout: no partial verdict");
        if (failure.get() != null)
            throw new AssertionError("Observer failed", failure.get());
        assertNotNull(out.get());
        return out.get().intValue();
    }

    @Test
    public void actualJpaAndJdbcShareACommitAndRollbackBoundary() throws Exception {
        U07D5O2StageAFixtureConfig.requireEnvironment();
        try (AnnotationConfigApplicationContext ctx =
                 new AnnotationConfigApplicationContext(U07D5O2StageAFixtureConfig.class)) {
            final DataSource ds = ctx.getBean(DataSource.class);
            final RuntimeThreadStateRepository threads = ctx.getBean(RuntimeThreadStateRepository.class);
            final TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            final JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);
            final String rollbackId = "stagea-rollback-" + UUID.randomUUID().toString();
            final String commitId = "stagea-commit-" + UUID.randomUUID().toString();
            RuntimeException injected = assertThrows(RuntimeException.class, () ->
                tx.execute(status -> {
                    assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                    assertTrue(TransactionSynchronizationManager.hasResource(ds));
                    assertTrue(DataSourceUtils.isConnectionTransactional(
                        DataSourceUtils.getConnection(ds), ds));
                    threads.saveAndFlush(new RuntimeThreadStateRecord(
                        rollbackId, rollbackId, LocalDateTime.now()));
                    assertEquals(Integer.valueOf(1), jdbc.queryForObject(SQL,
                        new Object[]{rollbackId}, Integer.class));
                    try {
                        assertEquals(0, separatelyCommittedCount(ds, rollbackId),
                            "Uncommitted row must be invisible to independent observer");
                    } catch (Exception ex) {
                        throw new IllegalStateException("READBACK_UNAVAILABLE", ex);
                    }
                    throw new IllegalStateException("STAGE_A_ROLLBACK_INJECTED");
                }));
            assertEquals("STAGE_A_ROLLBACK_INJECTED", injected.getMessage());
            assertEquals(0, separatelyCommittedCount(ds, rollbackId),
                "saveAndFlush must not survive outer rollback");

            tx.execute(status -> {
                threads.saveAndFlush(new RuntimeThreadStateRecord(
                    commitId, commitId, LocalDateTime.now()));
                assertEquals(Integer.valueOf(1), jdbc.queryForObject(SQL,
                    new Object[]{commitId}, Integer.class));
                return null;
            });
            assertEquals(1, separatelyCommittedCount(ds, commitId),
                "Committed row must survive independent readback");

            // Keep test fixtures isolated and idempotent even on repeated CI runs.
            tx.execute(status -> { threads.deleteById(commitId); return null; });
            assertEquals(0, separatelyCommittedCount(ds, commitId));
        }
    }
}
