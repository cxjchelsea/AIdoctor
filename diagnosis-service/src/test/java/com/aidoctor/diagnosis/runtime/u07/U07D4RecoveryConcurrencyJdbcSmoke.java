package com.aidoctor.diagnosis.runtime.u07;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Disposable MySQL-only synthetic crash/rollback and multi-connection smoke.
 * Does not exercise kill -9 or actual post-commit loss of network acknowledgement.
 */
public final class U07D4RecoveryConcurrencyJdbcSmoke {
    private static final U07D4SyntheticRecoveryInspector INSPECTOR =
            new U07D4SyntheticRecoveryInspector();
    private static final U07D3SyntheticTransactionCoordinator COORDINATOR =
            new U07D3SyntheticTransactionCoordinator();
    private static final Timestamp NOW = Timestamp.valueOf("2026-01-01 00:00:00");
    private static int assertions;

    private static void check(boolean value, String description) {
        assertions++;
        if (!value) throw new AssertionError(description);
    }

    private static Connection connect(String[] args) throws SQLException {
        Connection c = DriverManager.getConnection(args[0], args[1], args[2]);
        c.setAutoCommit(false);
        return c;
    }

    private static U07D3SyntheticTransactionCoordinator.Input command(String id) {
        return new U07D3SyntheticTransactionCoordinator.Input(id, "synthetic-consult-d3",
                "synthetic-question-d3", "synthetic-wait-d3", id + "-key", "digest-" + id, 1);
    }

    private static U07D4SyntheticRecoveryInspector.State read(
            Connection c, U07D3SyntheticTransactionCoordinator.Input in) throws SQLException {
        return INSPECTOR.inspect(c, in.eventId, in.consultationId,
                in.questionId, in.waitEffectId, in.idempotencyKey, in.digest);
    }

    private static int count(Connection c, String table, String event) throws SQLException {
        if (!"canonical_business_event".equals(table)
                && !"u07_event_application".equals(table)) throw new IllegalArgumentException();
        try (PreparedStatement p = c.prepareStatement(
                "SELECT COUNT(*) FROM " + table + " WHERE event_id=?")) {
            p.setString(1, event);
            try (ResultSet r = p.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !args[0].startsWith("jdbc:mysql://127.0.0.1:")) {
            throw new IllegalArgumentException("local synthetic-only database");
        }
        Class.forName("com.mysql.cj.jdbc.Driver");
        U07D3SyntheticTransactionCoordinator.Input crashed =
                command("synthetic-d3-uncommitted");
        try (Connection c = connect(args)) {
            check(read(c, crashed) == U07D4SyntheticRecoveryInspector.State.NO_DURABLE_EVENT,
                    "no accepted evidence before insert");
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO canonical_business_event "
                    + "(event_id,consultation_id,event_type,idempotency_key,payload_digest,received_at) "
                    + "VALUES (?,?,'SYNTHETIC_TEST_ONLY',?,?,?)")) {
                p.setString(1, crashed.eventId);
                p.setString(2, crashed.consultationId);
                p.setString(3, crashed.idempotencyKey);
                p.setString(4, crashed.digest);
                p.setTimestamp(5, NOW);
                p.executeUpdate();
            }
            // Simulate connection disappearing before COMMIT.
        }
        try (Connection fresh = connect(args)) {
            check(read(fresh, crashed) == U07D4SyntheticRecoveryInspector.State.NO_DURABLE_EVENT,
                    "uncommitted canonical rolled back on connection loss");
            fresh.rollback();
        }

        U07D3SyntheticTransactionCoordinator.Input accepted = command("synthetic-d3-recover");
        try (Connection c = connect(args)) {
            check(COORDINATOR.admitAndDecideSynthetic(c, accepted, NOW)
                    == U07D3SyntheticTransactionCoordinator.Outcome.ACCEPTED,
                    "positive durable admission");
        }
        try (Connection fresh = connect(args)) {
            check(read(fresh, accepted) ==
                    U07D4SyntheticRecoveryInspector.State.HISTORICAL_ACCEPTED,
                    "fresh connection accepted readback after commit");
            fresh.rollback();
        }
        try (Connection mutation = connect(args)) {
            try (PreparedStatement p = mutation.prepareStatement(
                    "UPDATE clinical_consultation SET lifecycle_status='ACTIVE',"
                    + "row_version=row_version+1,current_wait_effect_id=NULL "
                    + "WHERE consultation_id='synthetic-consult-d3'")) {
                check(p.executeUpdate() == 1, "synthetic consultation advances after commit");
            }
            mutation.commit();
        }
        try (Connection fresh = connect(args)) {
            check(read(fresh, accepted) ==
                    U07D4SyntheticRecoveryInspector.State.HISTORICAL_ACCEPTED,
                    "historical evidence stable after wait advances");
            check(COORDINATOR.admitAndDecideSynthetic(fresh, accepted, NOW)
                    == U07D3SyntheticTransactionCoordinator.Outcome.SAME_EVENT_REPLAY,
                    "historical replay survives current wait state advancement");
        }

        U07D3SyntheticTransactionCoordinator.Input partial = command("synthetic-d3-partial");
        try (Connection c = connect(args)) {
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO canonical_business_event "
                    + "(event_id,consultation_id,event_type,idempotency_key,payload_digest,received_at) "
                    + "VALUES (?,?,'SYNTHETIC_TEST_ONLY',?,?,?)")) {
                p.setString(1, partial.eventId);
                p.setString(2, partial.consultationId);
                p.setString(3, partial.idempotencyKey);
                p.setString(4, partial.digest);
                p.setTimestamp(5, NOW);
                p.executeUpdate();
            }
            c.commit();
        }
        try (Connection c = connect(args)) {
            check(read(c, partial) ==
                    U07D4SyntheticRecoveryInspector.State.PARTIAL_OR_INCONSISTENT,
                    "canonical-only partial evidence is fail-closed");
            check(INSPECTOR.inspect(c, partial.eventId, partial.consultationId,
                    partial.questionId, partial.waitEffectId, partial.idempotencyKey,
                    "conflicting-digest") ==
                    U07D4SyntheticRecoveryInspector.State.IDENTITY_CONFLICT,
                    "mismatching canonical digest rejected");
            c.rollback();
        }

        // Restore the SAME synthetic consultation to WAITING_USER for a
        // two-connection race after our historical-readback checks.
        try (Connection c = connect(args)) {
            try (PreparedStatement p = c.prepareStatement(
                    "UPDATE clinical_consultation SET lifecycle_status='WAITING_USER',"
                    + "row_version=1,current_wait_effect_id='synthetic-wait-d3' "
                    + "WHERE consultation_id='synthetic-consult-d3'")) {
                check(p.executeUpdate() == 1, "synthetic race fixture reset");
            }
            c.commit();
        }
        final U07D3SyntheticTransactionCoordinator.Input race =
                command("synthetic-d3-race");
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger acceptedCount = new AtomicInteger();
        AtomicInteger replayCount = new AtomicInteger();
        AtomicInteger dbConflictCount = new AtomicInteger();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();
        Thread[] workers = new Thread[2];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(() -> {
                try (Connection c = connect(args)) {
                    if (!start.await(12, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                    U07D3SyntheticTransactionCoordinator.Outcome o =
                            COORDINATOR.admitAndDecideSynthetic(c, race, NOW);
                    if (o == U07D3SyntheticTransactionCoordinator.Outcome.ACCEPTED) {
                        acceptedCount.incrementAndGet();
                    } else if (o == U07D3SyntheticTransactionCoordinator.Outcome.SAME_EVENT_REPLAY) {
                        replayCount.incrementAndGet();
                    } else {
                        throw new AssertionError("unexpected currentness rejection");
                    }
                } catch (SQLException sqlFailure) {
                    // A retryable deadlock/serialization failure is an explicit
                    // failure surface, never silently promoted to acceptance.
                    try {
                        U07D4MysqlConflictClassifier.requireExpected(sqlFailure);
                        dbConflictCount.incrementAndGet();
                    } catch (SQLException nonConflict) {
                        unexpected.compareAndSet(null, nonConflict);
                    }
                } catch (Throwable fail) {
                    unexpected.compareAndSet(null, fail);
                }
            }, "u07-d4-synthetic-" + i);
            workers[i].start();
        }
        start.countDown();
        for (Thread t : workers) {
            t.join(20000);
            check(!t.isAlive(), "bounded concurrent completion");
        }
        if (unexpected.get() != null) {
            throw new AssertionError("unexpected worker failure", unexpected.get());
        }
        try (Connection fresh = connect(args)) {
            check(count(fresh, "canonical_business_event", race.eventId) == 1,
                    "exactly one durable canonical identity");
            check(count(fresh, "u07_event_application", race.eventId) == 1,
                    "exactly one application");
            check(read(fresh, race) == U07D4SyntheticRecoveryInspector.State.HISTORICAL_ACCEPTED,
                    "race winner reconcile from fresh connection");
            fresh.rollback();
        }
        check(acceptedCount.get() == 1, "exactly one first acceptance");
        check(acceptedCount.get() + replayCount.get() + dbConflictCount.get() == 2,
                "second invocation either replay or explicit database conflict");
        System.out.println("U07_D4_SYNTHETIC_RECOVERY=PASS assertions=" + assertions
                + " concurrency_replay=" + replayCount.get()
                + " explicit_db_conflict=" + dbConflictCount.get());
    }
}
