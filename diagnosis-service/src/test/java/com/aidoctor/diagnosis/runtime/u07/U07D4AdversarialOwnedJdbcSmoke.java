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
 * Disposable MySQL synthetic-only ownership and adversarial two-worker tests.
 * ACK loss is injected after DB commit, not an actual network partition/kill -9.
 */
public final class U07D4AdversarialOwnedJdbcSmoke {
    private static final Timestamp NOW = Timestamp.valueOf("2026-01-01 00:00:00");
    private static int assertions;

    private static void check(boolean yes, String reason) {
        assertions++;
        if (!yes) throw new AssertionError(reason);
    }

    private static U07D3SyntheticTransactionCoordinator.Input cmd(
            String event, String idem, String digest) {
        return new U07D3SyntheticTransactionCoordinator.Input(
                event, "synthetic-consult-d3", "synthetic-question-d3",
                "synthetic-wait-d3", idem, digest, 1);
    }

    private static int count(String[] args, String table, String id) throws SQLException {
        if (!"canonical_business_event".equals(table) && !"u07_event_application".equals(table)) {
            throw new IllegalArgumentException("table");
        }
        try (Connection c = DriverManager.getConnection(args[0], args[1], args[2]);
             PreparedStatement s = c.prepareStatement(
                     "SELECT COUNT(*) FROM " + table + " WHERE event_id=?")) {
            s.setString(1, id);
            try (ResultSet r = s.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }

    private static void runRace(U07D4SyntheticOwnedTransactionRunner runner,
                                U07D3SyntheticTransactionCoordinator.Input a,
                                U07D3SyntheticTransactionCoordinator.Input b,
                                String[] args,
                                boolean sameEvent) throws Exception {
        AtomicInteger accept = new AtomicInteger();
        AtomicInteger replay = new AtomicInteger();
        AtomicInteger explicitlyRejected = new AtomicInteger();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();
        CountDownLatch gate = new CountDownLatch(1);
        Thread[] workers = new Thread[2];
        U07D3SyntheticTransactionCoordinator.Input[] inputs = {a, b};
        for (int i = 0; i < 2; i++) {
            final int position = i;
            workers[i] = new Thread(() -> {
                try {
                    if (!gate.await(15, TimeUnit.SECONDS)) throw new AssertionError("start timed out");
                    U07D4SyntheticOwnedTransactionRunner.Result result = runner.execute(
                            inputs[position], NOW, U07D4SyntheticOwnedTransactionRunner.Fault.NONE);
                    if (!result.acknowledgementKnown) throw new AssertionError("unknown ack");
                    if (result.outcome == U07D3SyntheticTransactionCoordinator.Outcome.ACCEPTED) {
                        accept.incrementAndGet();
                    } else if (result.outcome ==
                            U07D3SyntheticTransactionCoordinator.Outcome.SAME_EVENT_REPLAY) {
                        replay.incrementAndGet();
                    } else throw new AssertionError("unexpected currentness");
                } catch (SQLException sql) {
                    explicitlyRejected.incrementAndGet();
                } catch (IllegalStateException conflict) {
                    if (conflict.getMessage() != null &&
                            conflict.getMessage().startsWith("U07_D3_CANONICAL_")) {
                        explicitlyRejected.incrementAndGet();
                    } else {
                        unexpected.compareAndSet(null, conflict);
                    }
                } catch (Throwable unexpectedFailure) {
                    unexpected.compareAndSet(null, unexpectedFailure);
                }
            }, "u07-d4-adversarial-" + i);
            workers[i].start();
        }
        gate.countDown();
        for (Thread worker : workers) {
            worker.join(25000);
            check(!worker.isAlive(), "race has bounded completion");
        }
        if (unexpected.get() != null) throw new AssertionError("unexpected race exception", unexpected.get());
        check(accept.get() == 1, "exactly one newly accepted event per race");
        check(accept.get() + replay.get() + explicitlyRejected.get() == 2,
                "both invocations have explicit outcomes");
        if (sameEvent) {
            check(count(args, "canonical_business_event", a.eventId) == 1,
                    "one canonical row for duplicate event");
            check(count(args, "u07_event_application", a.eventId) == 1,
                    "one application row for duplicate event");
            check(runner.inspectFresh(a) ==
                    U07D4SyntheticRecoveryInspector.State.HISTORICAL_ACCEPTED,
                    "duplicate event durable evidence accepted");
        } else {
            int canonicals = count(args, "canonical_business_event", a.eventId)
                    + count(args, "canonical_business_event", b.eventId);
            int applications = count(args, "u07_event_application", a.eventId)
                    + count(args, "u07_event_application", b.eventId);
            check(canonicals == 1, "aliased idempotency key yields one canonical row");
            check(applications == 1, "aliased key yields one application row");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !args[0].startsWith(
                "jdbc:mysql://127.0.0.1:33321/u07_d4_synthetic")) {
            throw new IllegalArgumentException("disposable localhost only");
        }
        Class.forName("com.mysql.cj.jdbc.Driver");
        U07D4SyntheticOwnedTransactionRunner runner =
                new U07D4SyntheticOwnedTransactionRunner(args[0], args[1], args[2]);
        boolean externalDenied = false;
        try {
            new U07D4SyntheticOwnedTransactionRunner(
                    "jdbc:mysql://example.com/production", args[1], args[2]);
        } catch (IllegalArgumentException expected) {
            externalDenied = true;
        }
        check(externalDenied, "remote DB URL denied");

        U07D3SyntheticTransactionCoordinator.Input before = cmd(
                "synthetic-d3-owner-pre", "synthetic-d3-owner-pre-key", "digest-pre");
        U07D4SyntheticOwnedTransactionRunner.Result noCommit = runner.execute(
                before, NOW, U07D4SyntheticOwnedTransactionRunner.Fault.BEFORE_ADMISSION);
        check(!noCommit.acknowledgementKnown && noCommit.outcome == null, "injected prewrite crash");
        check(runner.inspectFresh(before) ==
                U07D4SyntheticRecoveryInspector.State.NO_DURABLE_EVENT,
                "no durable evidence after prewrite crash");

        U07D3SyntheticTransactionCoordinator.Input after = cmd(
                "synthetic-d3-owner-post", "synthetic-d3-owner-post-key", "digest-post");
        U07D4SyntheticOwnedTransactionRunner.Result unknown = runner.execute(
                after, NOW, U07D4SyntheticOwnedTransactionRunner.Fault.AFTER_COMMIT_ACK_LOSS);
        check(!unknown.acknowledgementKnown && unknown.outcome == null,
                "postcommit acknowledgement intentionally hidden");
        check(runner.inspectFresh(after) ==
                U07D4SyntheticRecoveryInspector.State.HISTORICAL_ACCEPTED,
                "new connection recovers durable acceptance after lost acknowledgement");
        check(runner.execute(after, NOW, U07D4SyntheticOwnedTransactionRunner.Fault.NONE).outcome ==
                U07D3SyntheticTransactionCoordinator.Outcome.SAME_EVENT_REPLAY,
                "retry after ack loss is stable replay");

        for (int i = 0; i < 5; i++) {
            String id = "synthetic-d3-owner-same-" + i;
            U07D3SyntheticTransactionCoordinator.Input identical = cmd(
                    id, "synthetic-d3-owner-idem-" + i, "digest-same-" + i);
            runRace(runner, identical, identical, args, true);
        }
        for (int i = 0; i < 5; i++) {
            String prefix = "synthetic-d3-owner-alias-" + i;
            String key = "synthetic-d3-owner-alias-key-" + i;
            U07D3SyntheticTransactionCoordinator.Input a = cmd(
                    prefix + "-a", key, "digest-a");
            U07D3SyntheticTransactionCoordinator.Input b = cmd(
                    prefix + "-b", key, "digest-b");
            runRace(runner, a, b, args, false);
        }
        System.out.println("U07_D4_ADVERSARIAL_OWNER_SMOKE=PASS assertions=" + assertions
                + " races=10 (5 duplicate identity + 5 same-key conflict)");
    }
}
