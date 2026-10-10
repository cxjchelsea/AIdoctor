package com.aidoctor.diagnosis.runtime.u07;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static com.aidoctor.diagnosis.runtime.u07.U07SyntheticResults.*;
import static com.aidoctor.diagnosis.runtime.u07.U07D5PTestDriver.Fault;

/** Fixed disposable MySQL only. Explicit JDBC fault simulation, not network partition. */
public final class U07D5PBoundaryJdbcSmoke {
    private static final Timestamp NOW = Timestamp.valueOf("2026-01-01 00:00:00");
    private static int assertions;
    private static int serial;
    private static void check(boolean yes, String label) {
        assertions++;
        if (!yes) throw new AssertionError(label);
    }
    private static U07SyntheticInput input(String suffix) {
        String id = "synthetic-d3-d5p-" + suffix;
        return new U07SyntheticInput(id, "synthetic-consult-d3", "synthetic-question-d3",
                "synthetic-wait-d3", id + "-key", "digest-" + suffix, 1);
    }
    private static void ready(String[] args) throws SQLException {
        try (Connection c = DriverManager.getConnection(args[0], args[1], args[2]);
             PreparedStatement p = c.prepareStatement("UPDATE clinical_consultation "
                     + "SET lifecycle_status='WAITING_USER',row_version=1,current_wait_effect_id='synthetic-wait-d3' "
                     + "WHERE consultation_id='synthetic-consult-d3'")) {
            check(p.executeUpdate() == 1, "reset synthetic wait fixture");
        }
    }
    private static void architecture() throws Exception {
        check(!Modifier.isPublic(U07D3SyntheticTransactionCoordinator.class.getModifiers()), "internal coordinator");
        check(!Modifier.isPublic(U07D4SyntheticRecoveryInspector.class.getModifiers()), "internal inspector");
        for (Class<?> type : new Class<?>[]{U07D3SyntheticTransactionCoordinator.class,
                U07D4SyntheticRecoveryInspector.class, U07D4SyntheticOwnedTransactionRunner.class}) {
            for (Method m : type.getDeclaredMethods()) {
                if (!Modifier.isPublic(m.getModifiers())) continue;
                for (Class<?> p : m.getParameterTypes()) {
                    check(p != Connection.class && p != javax.sql.DataSource.class, "no public connection execution");
                }
            }
        }
        for (Constructor<?> c : U07D4SyntheticOwnedTransactionRunner.class.getConstructors()) {
            check(c.getParameterTypes().length == 3
                    && c.getParameterTypes()[0] == U07SyntheticTestTarget.class, "fixed target constructor only");
        }
    }
    private static void invalids(U07D5PTestDriver driver, String[] args) {
        U07D4SyntheticOwnedTransactionRunner runner = U07SyntheticSmokeSupport.runner(args);
        U07SyntheticInput valid = input("invalid-control");
        String[] values = {"real-id", "synthetic- space", "synthetic-\ncontrol", "synthetic-用户",
                "synthetic-" + new String(new char[130]).replace('\0', 'x'), null};
        for (int field = 0; field < 5; field++) {
            for (String bad : values) {
                String[] ids = {valid.eventId, valid.consultationId, valid.questionId, valid.waitEffectId, valid.idempotencyKey};
                ids[field] = bad;
                U07SyntheticInput in = new U07SyntheticInput(ids[0], ids[1], ids[2], ids[3], ids[4], valid.digest, 1);
                U07D5PTestDriver.Trace t = driver.mode(Fault.NONE);
                WriteResult w = runner.execute(in, NOW, U07D4SyntheticOwnedTransactionRunner.Fault.NONE);
                RecoveryResult r = runner.inspectFresh(in);
                check(w.transactionStatus == Transaction.NOT_STARTED && w.operationStatus == Operation.FAILED,
                        "invalid identity write rejected");
                check(r.readStatus == Read.NOT_ATTEMPTED && r.observation == null, "invalid identity read rejected");
                check(t.connections == 0 && t.statements == 0, "invalid identity opens no connection/SQL");
            }
        }
        U07SyntheticInput[] badInputs = {null, new U07SyntheticInput(valid.eventId, valid.consultationId,
                valid.questionId, valid.waitEffectId, valid.idempotencyKey, " ", 1),
                new U07SyntheticInput(valid.eventId, valid.consultationId, valid.questionId,
                        valid.waitEffectId, valid.idempotencyKey, valid.digest, -1)};
        for (U07SyntheticInput bad : badInputs) {
            U07D5PTestDriver.Trace t = driver.mode(Fault.NONE);
            check(runner.execute(bad, NOW, U07D4SyntheticOwnedTransactionRunner.Fault.NONE).operationStatus == Operation.FAILED,
                    "invalid input typed failure");
            check(runner.inspectFresh(bad).readStatus == Read.NOT_ATTEMPTED && t.connections == 0, "invalid no connection");
        }
        U07D5PTestDriver.Trace t = driver.mode(Fault.NONE);
        check(runner.execute(valid, null, U07D4SyntheticOwnedTransactionRunner.Fault.NONE).operationStatus == Operation.FAILED,
                "null time rejected");
        check(runner.execute(valid, NOW, null).operationStatus == Operation.FAILED && t.connections == 0,
                "null fault rejected before connect");
        for (String bad : new String[]{"jdbc:mysql://example.com/production", args[0] + "&socketFactory=x",
                args[0].replace("127.0.0.1", "localhost"), args[0].replace("u07_d4_synthetic", "other")}) {
            boolean denied = false;
            try { U07SyntheticTestTarget.fromExactUrl(bad); }
            catch (IllegalArgumentException expected) { denied = true; }
            check(denied && t.connections == 0, "arbitrary target denied");
        }
        check(new U07D4SyntheticOwnedTransactionRunner(null, args[1], args[2])
                .execute(valid, NOW, U07D4SyntheticOwnedTransactionRunner.Fault.NONE).transactionStatus == Transaction.NOT_STARTED,
                "invalid constructor target rejected at execution");
        check(t.connections == 0, "target opens no connection");
    }
    private static void writes(U07D5PTestDriver driver, String[] args) throws Exception {
        U07D4SyntheticOwnedTransactionRunner runner = U07SyntheticSmokeSupport.runner(args);
        for (Fault fault : new Fault[]{Fault.CONNECT, Fault.CONFIG, Fault.BEGIN, Fault.BODY,
                Fault.BODY_ROLLBACK, Fault.BODY_ROLLBACK_CLOSE, Fault.COMMIT_BEFORE, Fault.COMMIT_AFTER, Fault.CLOSE}) {
            U07SyntheticInput in = input("fault-" + serial++);
            U07D5PTestDriver.Trace t = driver.mode(fault);
            WriteResult w = runner.execute(in, NOW, U07D4SyntheticOwnedTransactionRunner.Fault.NONE);
            check(w.operationStatus == Operation.FAILED && !w.isNormalSuccess(), "failure never normal success");
            if (fault == Fault.CONNECT || fault == Fault.CONFIG || fault == Fault.BEGIN) {
                check(w.transactionStatus == Transaction.NOT_STARTED && t.statements == 0, "config/connect fail no business SQL");
            } else if (fault == Fault.BODY) {
                check(w.transactionStatus == Transaction.ROLLED_BACK && w.cleanupStatus == Cleanup.COMPLETE,
                        "body fail confirmed rollback");
            } else if (fault == Fault.BODY_ROLLBACK || fault == Fault.BODY_ROLLBACK_CLOSE) {
                check(w.transactionStatus == Transaction.ABORT_UNCONFIRMED && w.cleanupStatus == Cleanup.FAILED,
                        "rollback failure remains unconfirmed");
                check(w.primaryFailure == t.primary && w.cleanupErrors.get(0) == t.rollbackError,
                        "primary error preserved and rollback secondary");
                if (fault == Fault.BODY_ROLLBACK_CLOSE) {
                    check(w.cleanupErrors.size() == 2 && w.cleanupErrors.get(1) == t.closeError,
                            "rollback/close secondary order");
                }
            } else if (fault == Fault.COMMIT_BEFORE || fault == Fault.COMMIT_AFTER) {
                check(w.transactionStatus == Transaction.COMMIT_OUTCOME_UNKNOWN && w.businessOutcome == null,
                        "both commit fault windows unknown");
                check(t.commits == 1 && t.rollbacks == 1, "commit then best effort rollback exactly once");
            } else {
                check(w.transactionStatus == Transaction.COMMITTED && w.businessOutcome == Business.ACCEPTED
                        && w.cleanupStatus == Cleanup.FAILED, "close failure preserves known commit only as metadata");
            }
            driver.mode(Fault.NONE);
            Evidence recovered = U07SyntheticSmokeSupport.inspect(args, in);
            boolean committed = fault == Fault.COMMIT_AFTER || fault == Fault.CLOSE;
            check(recovered == (committed ? Evidence.HISTORICAL_ACCEPTED : Evidence.NO_DURABLE_EVENT),
                    "fresh durable evidence for fault window");
            if (fault == Fault.COMMIT_BEFORE || fault == Fault.COMMIT_AFTER) {
                check(w.transactionStatus == Transaction.COMMIT_OUTCOME_UNKNOWN, "readback never rewrites old UNKNOWN");
            }
        }
        U07SyntheticInput in = input("suppression");
        U07D5PTestDriver.Trace t = driver.mode(Fault.CLOSE);
        WriteResult w = runner.execute(in, NOW, U07D4SyntheticOwnedTransactionRunner.Fault.AFTER_COMMIT_ACK_LOSS);
        check(w.transactionStatus == Transaction.COMMITTED && w.responseAvailability == Response.SUPPRESSED_AFTER_COMMIT
                && w.cleanupStatus == Cleanup.FAILED && w.businessOutcome == null && !w.isNormalSuccess(),
                "suppression and cleanup failure coexist");
        check(t.commits == 1 && t.rollbacks == 0, "known commit not rolled back");
        t = driver.mode(Fault.REJECT_ROLLBACK);
        U07SyntheticInput stale = input("stale");
        stale = new U07SyntheticInput(stale.eventId, stale.consultationId, stale.questionId,
                stale.waitEffectId, stale.idempotencyKey, stale.digest, 0);
        w = runner.execute(stale, NOW, U07D4SyntheticOwnedTransactionRunner.Fault.NONE);
        check(w.transactionStatus == Transaction.ABORT_UNCONFIRMED && w.businessOutcome == null
                && w.primaryFailure == t.rollbackError && t.rollbacks == 1, "rejection rollback failure not normal reject");
        t = driver.mode(Fault.FATAL);
        boolean fatal = false;
        try { runner.execute(input("fatal"), NOW, U07D4SyntheticOwnedTransactionRunner.Fault.NONE); }
        catch (AssertionError expected) { fatal = expected == t.fatal; }
        check(fatal && t.rollbacks == 1 && t.closes == 1, "fatal Error propagates with finally cleanup");
        driver.mode(Fault.NONE);
        check(U07SyntheticSmokeSupport.inspect(args, input("fatal")) == Evidence.NO_DURABLE_EVENT, "fatal partial writes rolled back");
    }
    private static void fatalCleanup(U07D5PTestDriver driver, String[] args) throws Exception {
        U07D4SyntheticOwnedTransactionRunner runner = U07SyntheticSmokeSupport.runner(args);
        for (Fault fault : new Fault[]{Fault.BODY_ROLLBACK_FATAL, Fault.READ_ROLLBACK_FATAL,
                Fault.FATAL_ROLLBACK_CLOSE, Fault.FATAL_ROLLBACK_CLOSE_FATAL, Fault.FATAL_SELF_ROLLBACK}) {
            String suffix = "fatal-cleanup-" + serial++;
            U07SyntheticInput in = input(suffix);
            U07D5PTestDriver.Trace t = driver.mode(fault);
            AssertionError caught = null;
            try {
                if (fault == Fault.READ_ROLLBACK_FATAL) runner.inspectFresh(in);
                else runner.execute(in, NOW, U07D4SyntheticOwnedTransactionRunner.Fault.NONE);
            } catch (AssertionError error) { caught = error; }
            boolean bodyFatal = fault == Fault.FATAL_ROLLBACK_CLOSE
                    || fault == Fault.FATAL_ROLLBACK_CLOSE_FATAL || fault == Fault.FATAL_SELF_ROLLBACK;
            check(caught == (bodyFatal ? t.fatal : t.rollbackFatal), "first fatal identity preserved");
            check(t.rollbacks == 1 && t.closes == 1 && t.commits == 0, "fatal rollback still attempts close once");
            if (fault == Fault.FATAL_ROLLBACK_CLOSE || fault == Fault.FATAL_ROLLBACK_CLOSE_FATAL) {
                Throwable[] errors = caught.getSuppressed();
                check(errors.length == 2 && errors[0] == t.rollbackFatal
                        && errors[1] == (fault == Fault.FATAL_ROLLBACK_CLOSE ? t.closeError : t.closeFatal),
                        "fatal secondary errors retain rollback then close order");
            } else {
                check(caught.getSuppressed().length == 0, "no self suppression or fabricated secondary");
            }
            if (fault == Fault.READ_ROLLBACK_FATAL) check(t.queries == 1, "fatal read cleanup still single query");
            driver.mode(Fault.NONE);
            check(U07SyntheticSmokeSupport.inspect(args, in) == Evidence.NO_DURABLE_EVENT,
                    "fresh evidence after fatal cleanup; close removes uncommitted writes");
        }
        System.out.println("U07_D5P_FATAL_CLEANUP=PASS cases=5 close_attempts=5 first_fatal_preserved=1");
    }

    private static void reads(U07D5PTestDriver driver, String[] args) throws Exception {
        U07D4SyntheticOwnedTransactionRunner runner = U07SyntheticSmokeSupport.runner(args);
        U07SyntheticInput valid = input("read-valid");
        driver.mode(Fault.NONE);
        check(U07SyntheticSmokeSupport.run(args, valid, NOW) == Business.ACCEPTED, "read fixture committed");
        for (Fault fault : new Fault[]{Fault.NONE, Fault.READ, Fault.READ_CLOSE, Fault.READ_ROLLBACK,
                Fault.READ_ROLLBACK_CLOSE, Fault.READ_FAIL_CLOSE}) {
            U07D5PTestDriver.Trace t = driver.mode(fault);
            RecoveryResult r = runner.inspectFresh(valid);
            check(t.connections == 1 && t.queries == 1 && t.commits == 0 && t.rollbacks == 1 && t.closes == 1,
                    "one evidence SELECT and read-only terminalization");
            if (fault == Fault.READ || fault == Fault.READ_FAIL_CLOSE) {
                check(r.readStatus == Read.UNAVAILABLE && r.observation == null && r.primaryFailure == t.primary,
                        "read failure never absence");
            } else {
                check(r.readStatus == Read.COMPLETE && r.observation.state == Evidence.HISTORICAL_ACCEPTED,
                        "cleanup failure preserves complete observation");
            }
            check(r.operationStatus == (fault == Fault.NONE ? Operation.SUCCEEDED : Operation.FAILED),
                    "read failure/cleanup failure explicit operation failure");
        }
        driver.mode(Fault.NONE);
        U07SyntheticInput orphan = input("orphan");
        try (Connection c = DriverManager.getConnection(args[0], args[1], args[2])) {
            c.setAutoCommit(false);
            new U07EventApplicationRepository().insertReceived(c, orphan.eventId, orphan.consultationId,
                    orphan.questionId, orphan.waitEffectId, orphan.digest, "synthetic-source", "synthetic-version", 1, NOW);
            c.commit();
        }
        check(U07SyntheticSmokeSupport.inspect(args, orphan) == Evidence.PARTIAL_OR_INCONSISTENT,
                "anchor preserves application-only orphan");
        U07SyntheticInput a = input("split-a"), b = input("split-b");
        check(U07SyntheticSmokeSupport.run(args, a, NOW) == Business.ACCEPTED, "split a seed");
        check(U07SyntheticSmokeSupport.run(args, b, NOW) == Business.ACCEPTED, "split b seed");
        U07SyntheticInput split = new U07SyntheticInput(a.eventId, a.consultationId, a.questionId,
                a.waitEffectId, b.idempotencyKey, a.digest, 1);
        check(U07SyntheticSmokeSupport.inspect(args, split) == Evidence.IDENTITY_CONFLICT, "both canonical candidates rejected");
        U07SyntheticInput received = input("received-only");
        U07SyntheticSmokeSupport.run(args, received, NOW);
        try (Connection c = DriverManager.getConnection(args[0], args[1], args[2]);
             PreparedStatement p = c.prepareStatement("UPDATE u07_event_application SET phase='RECEIVED',decision=NULL,row_version=0 WHERE event_id=?")) {
            p.setString(1, received.eventId); check(p.executeUpdate() == 1, "received fixture downgrade");
        }
        check(U07SyntheticSmokeSupport.inspect(args, received) == Evidence.RECEIVED_ONLY_REQUIRES_REVIEW,
                "RECEIVED never upgraded");
    }
    private static void interleaving(U07D5PTestDriver driver, String[] args) throws Exception {
        driver.mode(Fault.NONE);
        U07SyntheticInput old = input("old-snapshot");
        CountDownLatch firstRead = new CountDownLatch(1), committed = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread writer = new Thread(() -> {
            try {
                if (!firstRead.await(15, TimeUnit.SECONDS)) throw new AssertionError("writer latch timeout");
                check(U07SyntheticSmokeSupport.run(args, old, NOW) == Business.ACCEPTED, "controlled writer commit");
            } catch (Throwable failure) { error.set(failure); }
            finally { committed.countDown(); }
        });
        writer.start();
        try (Connection c = DriverManager.getConnection(args[0], args[1], args[2])) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED); c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM canonical_business_event WHERE event_id=?")) {
                p.setString(1, old.eventId);
                try (ResultSet r = p.executeQuery()) { r.next(); check(r.getInt(1) == 0, "old first SELECT before commit"); }
            }
            firstRead.countDown();
            check(committed.await(15, TimeUnit.SECONDS), "wait controlled commit");
            if (error.get() != null) throw new AssertionError("writer failed", error.get());
            try (PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM u07_event_application WHERE event_id=?")) {
                p.setString(1, old.eventId);
                try (ResultSet r = p.executeQuery()) { r.next(); check(r.getInt(1) == 1, "old second SELECT after commit produces mixed reads"); }
            }
            c.rollback();
        } finally { firstRead.countDown(); writer.join(15000); }
        check(!writer.isAlive(), "controlled writer completed");
        U07SyntheticInput fresh = input("single-snapshot");
        U07D5PTestDriver.Trace t = driver.mode(Fault.NONE);
        check(U07SyntheticSmokeSupport.inspect(args, fresh) == Evidence.NO_DURABLE_EVENT && t.queries == 1,
                "single SELECT complete precommit snapshot");
        U07SyntheticSmokeSupport.run(args, fresh, NOW);
        t = driver.mode(Fault.NONE);
        check(U07SyntheticSmokeSupport.inspect(args, fresh) == Evidence.HISTORICAL_ACCEPTED && t.queries == 1,
                "single SELECT complete postcommit snapshot");
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 3 && args.length != 4) throw new IllegalArgumentException("fixed args");
        U07SyntheticTestTarget.fromExactUrl(args[0]);
        try (U07D5PTestDriver driver = new U07D5PTestDriver()) {
            if (args.length == 4 && "worker-failure".equals(args[3])) {
                AtomicReference<Throwable> failed = new AtomicReference<>();
                Thread worker = new Thread(() -> {
                    driver.mode(Fault.BODY);
                    try { U07SyntheticSmokeSupport.run(args, input("worker-failure"), NOW); }
                    catch (SQLException error) {
                        try { U07D4MysqlConflictClassifier.requireExpected(error); }
                        catch (SQLException nonConflict) { failed.set(nonConflict); }
                    } catch (Throwable other) { failed.set(other); }
                });
                worker.start(); worker.join(15000);
                if (worker.isAlive()) throw new AssertionError("worker timeout");
                if (failed.get() != null) {
                    System.err.println("U07_D5P_EXPECTED_NON_CONFLICT_WORKER_FAILURE");
                    throw new AssertionError("unexpected worker SQL error must fail process", failed.get());
                }
                throw new AssertionError("fault injection failed to reach worker");
            }
            architecture(); ready(args); invalids(driver, args); writes(driver, args); reads(driver, args); fatalCleanup(driver, args);
            interleaving(driver, args);
            System.out.println("U07_D5P_BOUNDARY_SMOKE=PASS assertions=" + assertions
                    + " scope=SYNTHETIC_MYSQL_DRIVER_FAULTS_AND_SINGLE_QUERY");
        }
    }
}
