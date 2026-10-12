package com.aidoctor.diagnosis.runtime.u07.d5o2;

import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
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

    private int separatelyCommittedCount(DataSource ignoredWriter, String key) throws Exception {
        final java.util.concurrent.atomic.AtomicReference<Integer> out =
            new java.util.concurrent.atomic.AtomicReference<Integer>();
        final java.util.concurrent.atomic.AtomicReference<Throwable> failure =
            new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread observer = new Thread(() -> {
            try (Connection c = DriverManager.getConnection(
                    U07D5O2StageAFixtureConfig.FIXED_URL,
                    "stagea_reader", "synthetic-stagea-reader")) {
                c.setReadOnly(true);
                try (PreparedStatement p = c.prepareStatement(SQL)) {
                    p.setString(1, key);
                    try (ResultSet rs = p.executeQuery()) {
                        if (!rs.next()) throw new IllegalStateException("NO_READER_ROW");
                        out.set(rs.getInt(1));
                    }
                }
            } catch (Throwable error) {
                failure.set(error);
            }
        }, "d5o2-select-only-readback");
        observer.start();
        observer.join(10000);
        assertFalse(observer.isAlive(), "Observer timeout: no verdict");
        if (failure.get() != null) throw new AssertionError("SELECT_ONLY_READER_FAILED", failure.get());
        assertNotNull(out.get());
        return out.get().intValue();
    }

    static void assertDisposableMySql(DataSource ds) throws SQLException {
        try (Connection c = ds.getConnection();
             Statement st = c.createStatement()) {
            assertEquals("u07_d5o2_stage_a", c.getCatalog(), "schema fence");
            assertTrue(c.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql"));
            try (ResultSet rs = st.executeQuery("SELECT VERSION(), CURRENT_USER()")) {
                assertTrue(rs.next());
                assertTrue(rs.getString(1).startsWith("8."), "MySQL 8 required");
                assertTrue(rs.getString(2).startsWith("stagea_writer@"), "writer identity");
            }
            try (ResultSet rs = st.executeQuery(
                    "SELECT ENGINE FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA='u07_d5o2_stage_a' " +
                    "AND TABLE_NAME='clinical_runtime_thread_state'")) {
                assertTrue(rs.next());
                assertEquals("InnoDB", rs.getString(1));
            }
        }
    }

    private long independentReaderConnectionId() throws SQLException {
        try (Connection c = DriverManager.getConnection(
                U07D5O2StageAFixtureConfig.FIXED_URL,
                "stagea_reader", "synthetic-stagea-reader");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT CONNECTION_ID()")) {
            assertTrue(rs.next());
            return rs.getLong(1);
        }
    }

    @Test
    public void readerIsSelectOnlyAndWriterIdentityIsIsolated() throws Exception {
        U07D5O2StageAFixtureConfig.requireEnvironment();
        try (Connection c = DriverManager.getConnection(
                U07D5O2StageAFixtureConfig.FIXED_URL,
                "stagea_reader", "synthetic-stagea-reader");
             Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT CURRENT_USER()")) {
                assertTrue(rs.next());
                assertTrue(rs.getString(1).startsWith("stagea_reader@"));
            }
            assertThrows(SQLException.class, () -> st.executeUpdate(
                "DELETE FROM clinical_runtime_thread_state WHERE thread_id='not-an-actual-row'"));
        }
    }

    @Test
    public void actualJpaAndJdbcShareACommitAndRollbackBoundary() throws Exception {
        U07D5O2StageAFixtureConfig.requireEnvironment();
        try (AnnotationConfigApplicationContext ctx =
                 new AnnotationConfigApplicationContext(U07D5O2StageAFixtureConfig.class)) {
            final DataSource ds = ctx.getBean(DataSource.class);
            assertDisposableMySql(ds);
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
                    assertNotEquals(jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class),
                        independentReaderConnectionId(), "reader and writer must differ");
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
