package com.aidoctor.diagnosis.runtime.u07;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Synthetic-only test boundary: creates a fresh dedicated JDBC connection
 * per call. Never consumes or commits a caller-owned transaction.
 *
 * Do NOT register as a production Spring component or pass real DB credentials.
 */
public final class U07D4SyntheticOwnedTransactionRunner {
    public enum Fault {
        NONE,
        BEFORE_ADMISSION,
        AFTER_COMMIT_ACK_LOSS
    }

    public static final class Result {
        public final boolean acknowledgementKnown;
        public final U07D3SyntheticTransactionCoordinator.Outcome outcome;
        private Result(boolean known, U07D3SyntheticTransactionCoordinator.Outcome value) {
            acknowledgementKnown = known;
            outcome = value;
        }
    }

    private final String url;
    private final String user;
    private final String password;
    private final U07D3SyntheticTransactionCoordinator coordinator =
            new U07D3SyntheticTransactionCoordinator();

    public U07D4SyntheticOwnedTransactionRunner(String url, String user, String password) {
        if (url == null || !url.matches(
                "^jdbc:mysql://127[.]0[.]0[.]1:[0-9]{1,5}/u07_d4_synthetic"
                        + "(?:[?].*)?$")) {
            throw new IllegalArgumentException("U07_D4_LOCAL_SYNTHETIC_DATABASE_ONLY");
        }
        if (user == null || password == null) throw new IllegalArgumentException("credentials required");
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Result execute(U07D3SyntheticTransactionCoordinator.Input input,
                          Timestamp now, Fault fault) throws SQLException {
        if (fault == null) throw new IllegalArgumentException("fault required");
        try (Connection tx = DriverManager.getConnection(url, user, password)) {
            if (!tx.getAutoCommit()) {
                // Require a fresh connection in the driver's ordinary initial state.
                throw new IllegalStateException("U07_D4_NEW_CONNECTION_REQUIRED");
            }
            tx.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            tx.setAutoCommit(false);
            if (fault == Fault.BEFORE_ADMISSION) {
                tx.rollback();
                return new Result(false, null);
            }
            U07D3SyntheticTransactionCoordinator.Outcome outcome =
                    coordinator.admitAndDecideSynthetic(tx, input, now);
            // D3 owns commit; injected ack loss occurs AFTER DB commit.
            if (fault == Fault.AFTER_COMMIT_ACK_LOSS) {
                return new Result(false, null);
            }
            return new Result(true, outcome);
        }
    }

    /**
     * Fresh-connection readback is separate from a possibly poisoned write TX.
     * Never invent an acceptance if durable evidence is partial or missing.
     */
    public U07D4SyntheticRecoveryInspector.State inspectFresh(
            U07D3SyntheticTransactionCoordinator.Input input) throws SQLException {
        try (Connection tx = DriverManager.getConnection(url, user, password)) {
            tx.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            tx.setAutoCommit(false);
            try {
                return new U07D4SyntheticRecoveryInspector().inspect(
                        tx, input.eventId, input.consultationId, input.questionId,
                        input.waitEffectId, input.idempotencyKey, input.digest);
            } finally {
                tx.rollback();
            }
        }
    }
}
