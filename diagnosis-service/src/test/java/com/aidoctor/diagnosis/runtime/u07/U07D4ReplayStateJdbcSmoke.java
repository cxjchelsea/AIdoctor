package com.aidoctor.diagnosis.runtime.u07;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/** Disposable MySQL replay-state and conflict-classification regressions only. */
public final class U07D4ReplayStateJdbcSmoke {
    private static final Timestamp NOW = Timestamp.valueOf("2026-01-01 00:00:00");
    private static int assertions;
    private static void check(boolean value, String reason) {
        assertions++;
        if (!value) throw new AssertionError(reason);
    }
    private static Connection connect(String[] args) throws SQLException {
        Connection c = DriverManager.getConnection(args[0], args[1], args[2]);
        c.setAutoCommit(false);
        return c;
    }
    private static U07SyntheticInput cmd(String suffix) {
        String id = "synthetic-d3-replay-state-" + suffix;
        return new U07SyntheticInput(id, "synthetic-consult-d3",
                "synthetic-question-d3", "synthetic-wait-d3", id + "-key", "digest-" + suffix, 1);
    }
    private static void corrupt(String[] args, String event, String phase,
                                String decision, long revision, String effect) throws SQLException {
        try (Connection c = connect(args);
             PreparedStatement p = c.prepareStatement("UPDATE u07_event_application "
                     + "SET phase=?,decision=?,row_version=?,effect_id=? WHERE event_id=?")) {
            p.setString(1, phase);
            p.setString(2, decision);
            p.setLong(3, revision);
            p.setString(4, effect);
            p.setString(5, event);
            check(p.executeUpdate() == 1, "one deliberately corrupted synthetic fixture");
            c.commit();
        }
    }
    private static void invalidReplay(String[] args, String suffix, String phase,
                                      String decision, long revision, String effect,
                                      boolean delete) throws Exception {
        U07SyntheticInput in = cmd(suffix);
        U07D4SyntheticOwnedTransactionRunner runner =
                U07SyntheticSmokeSupport.runner(args);
        check(U07SyntheticSmokeSupport.run(args, in, NOW)
                == U07SyntheticResults.Business.ACCEPTED, "seed accepted");
        if (delete) {
            try (Connection c = connect(args);
                 PreparedStatement p = c.prepareStatement("DELETE FROM u07_event_application WHERE event_id=?")) {
                p.setString(1, in.eventId);
                check(p.executeUpdate() == 1, "missing application fixture");
                c.commit();
            }
        } else {
            corrupt(args, in.eventId, phase, decision, revision, effect);
        }
        U07SyntheticResults.Evidence state = U07SyntheticSmokeSupport.inspect(args, in);
        check(state != U07SyntheticResults.Evidence.HISTORICAL_ACCEPTED,
                "inspector never upgrades incomplete or inconsistent state");
        U07SyntheticResults.WriteResult bad = runner.execute(in, NOW, U07D4SyntheticOwnedTransactionRunner.Fault.NONE);
        String reason = delete ? "U07_D3_RECONCILIATION_REQUIRED" : "U07_D3_REPLAY_STATE_REQUIRES_REVIEW";
        check(bad.primaryFailure != null && reason.equals(bad.primaryFailure.getMessage()), "explicit reconciliation reason");
        check(bad.operationStatus == U07SyntheticResults.Operation.FAILED
                && bad.transactionStatus == U07SyntheticResults.Transaction.ROLLED_BACK
                && bad.businessOutcome == null && bad.cleanupStatus == U07SyntheticResults.Cleanup.COMPLETE,
                "invalid historical record is not a successful replay");
        check(U07SyntheticSmokeSupport.inspect(args, in) == state, "rejected replay does not repair or mutate evidence");
        try (Connection c = connect(args);
             PreparedStatement p = c.prepareStatement("SELECT phase,decision,row_version,effect_id "
                     + "FROM u07_event_application WHERE event_id=?")) {
            p.setString(1, in.eventId);
            try (ResultSet r = p.executeQuery()) {
                if (delete) {
                    check(!r.next(), "missing application remains missing");
                } else {
                    check(r.next(), "application still present");
                    check(phase.equals(r.getString(1))
                            && java.util.Objects.equals(decision, r.getString(2))
                            && revision == r.getLong(3)
                            && java.util.Objects.equals(effect, r.getString(4)), "state unchanged on rejection");
                }
            }
            c.rollback();
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("args");
        U07D4SyntheticOwnedTransactionRunner runner =
                U07SyntheticSmokeSupport.runner(args);
        Class.forName("com.mysql.cj.jdbc.Driver");
        // The same shared classifier drives BOTH concurrency harnesses.
        check(U07D4MysqlConflictClassifier.isExpected(new SQLException("duplicate", "23000", 1062)),
                "duplicate-key allowed");
        check(U07D4MysqlConflictClassifier.isExpected(new SQLException("deadlock", "40001", 1213)),
                "deadlock allowed");
        int[][] wrong = {{1045, 0}, {1452, 1}, {1048, 1}, {1062, 2}, {1213, 1}, {0, 3}};
        String[] states = {"28000", "23000", "HY000", "08006"};
        for (int[] item : wrong) {
            SQLException fault = new SQLException("injected", states[item[1]], item[0]);
            boolean propagated = false;
            try {
                U07D4MysqlConflictClassifier.requireExpected(fault);
            } catch (SQLException expected) {
                check(expected == fault, "original error identity preserved");
                propagated = true;
            }
            check(propagated, "non-conflict error propagates to harness failure");
        }
        try (Connection c = connect(args)) {
            boolean failed = false;
            try (PreparedStatement p = c.prepareStatement("SELECT * FROM u07_d4_deliberately_missing_table")) {
                p.executeQuery();
            } catch (SQLException actual) {
                check(!U07D4MysqlConflictClassifier.isExpected(actual), "real SQL failure is not a conflict");
                failed = true;
            }
            check(failed, "missing-table database fault exercised");
            c.rollback();
        }
        invalidReplay(args, "received", "RECEIVED", null, 0, null, false);
        invalidReplay(args, "decision", "ACCEPTED", "REJECTED", 1, null, false);
        invalidReplay(args, "null-decision", "ACCEPTED", null, 1, null, false);
        invalidReplay(args, "revision", "ACCEPTED", "ACCEPTED", 2, null, false);
        invalidReplay(args, "phase", "APPLIED", "ACCEPTED", 1, null, false);
        invalidReplay(args, "effect", "ACCEPTED", "ACCEPTED", 1, "synthetic-effect-invalid", false);
        invalidReplay(args, "missing", null, null, 0, null, true);
        U07SyntheticInput valid = cmd("valid");
        check(U07SyntheticSmokeSupport.run(args, valid, NOW)
                == U07SyntheticResults.Business.ACCEPTED, "valid first acceptance");
        try (Connection c = connect(args);
             PreparedStatement p = c.prepareStatement("UPDATE clinical_consultation "
                     + "SET lifecycle_status='ACTIVE',row_version=2,current_wait_effect_id=NULL "
                     + "WHERE consultation_id='synthetic-consult-d3'")) {
            check(p.executeUpdate() == 1, "advance synthetic wait after commit");
            c.commit();
        }
        check(U07SyntheticSmokeSupport.run(args, valid, NOW)
                == U07SyntheticResults.Business.SAME_EVENT_REPLAY, "valid historical replay after advance");
        check(U07SyntheticSmokeSupport.inspect(args, valid) == U07SyntheticResults.Evidence.HISTORICAL_ACCEPTED,
                "inspector and coordinator agree on valid historical acceptance");
        System.out.println("U07_D4_REPLAY_STATE_SMOKE=PASS assertions=" + assertions
                + " invalid_states=7 scope=SYNTHETIC_HISTORICAL_ACCEPTED_ONLY");
    }
}
