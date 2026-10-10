package com.aidoctor.diagnosis.runtime.u07;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

/**
 * Isolated CI child JVM only. Runtime.halt intentionally skips graceful close;
 * the parent CI shell checks committed/uncommitted DB state on new connections.
 * Not a database-server crash or a network partition.
 */
public final class U07D4ForcedProcessExitWorker {
    private static final Timestamp NOW = Timestamp.valueOf("2026-01-01 00:00:00");
    public static void main(String[] args) throws Exception {
        if (args.length != 4 || !args[0].startsWith(
                "jdbc:mysql://127.0.0.1:33321/u07_d4_synthetic")) {
            throw new IllegalArgumentException("local test-only invocation");
        }
        Class.forName("com.mysql.cj.jdbc.Driver");
        String mode = args[3];
        if (!"pre".equals(mode) && !"post".equals(mode)) {
            throw new IllegalArgumentException("mode");
        }
        Connection connection = DriverManager.getConnection(args[0], args[1], args[2]);
        connection.setAutoCommit(false);
        String event = "synthetic-d3-forced-" + mode;
        String key = event + "-key";
        if ("pre".equals(mode)) {
            try (PreparedStatement p = connection.prepareStatement(
                    "INSERT INTO canonical_business_event "
                    + "(event_id,consultation_id,event_type,idempotency_key,payload_digest,received_at) "
                    + "VALUES (?,?,'SYNTHETIC_TEST_ONLY',?,?,?)")) {
                p.setString(1, event);
                p.setString(2, "synthetic-consult-d3");
                p.setString(3, key);
                p.setString(4, "synthetic-forced-digest");
                p.setTimestamp(5, NOW);
                p.executeUpdate();
            }
            new U07EventApplicationRepository().insertReceived(
                    connection, event, "synthetic-consult-d3", "synthetic-question-d3",
                    "synthetic-wait-d3", "synthetic-forced-digest", "synthetic-d3-authority",
                    "synthetic-d3-v1", 1, NOW);
            // Process dies with both JDBC INSERTs uncommitted.
            Runtime.getRuntime().halt(77);
        } else {
            connection.close();
            U07SyntheticResults.WriteResult result = U07SyntheticSmokeSupport.runner(args).execute(
                    new U07SyntheticInput(event, "synthetic-consult-d3", "synthetic-question-d3",
                            "synthetic-wait-d3", key, "synthetic-forced-digest", 1), NOW,
                    U07D4SyntheticOwnedTransactionRunner.Fault.NONE);
            if (!result.isNormalSuccess()) throw new AssertionError("owner did not commit", result.primaryFailure);
            // D3 committed, but calling process disappears before responding.
            Runtime.getRuntime().halt(78);
        }
    }
}
