package com.aidoctor.diagnosis.runtime.u06.delivery;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Explicitly invoked, DB-only transaction retry boundary.
 * No network/tool/notification operation is executed by this adapter.
 */
public final class BoundedU06WholeTxRetry implements U06DeliveryStore {
    private static final int MAX_ATTEMPTS = 2;
    private static final long TOTAL_BUDGET_NANOS = 12_000_000_000L;
    private final JdbcTemplate jdbc;
    private final DataSourceTransactionManager txManager;

    public BoundedU06WholeTxRetry(JdbcTemplate jdbc, DataSourceTransactionManager txManager) {
        if (jdbc == null || txManager == null || jdbc.getDataSource() == null)
            throw new IllegalArgumentException("U06_RETRY_DATASOURCE_REQUIRED");
        DataSource storeSource = jdbc.getDataSource();
        if (txManager.getDataSource() != storeSource)
            throw new IllegalArgumentException("U06_RETRY_DATASOURCE_MISMATCH");
        this.jdbc = jdbc;
        this.txManager = txManager;
    }

    @Override
    public U06DeliveryStore.Snapshot reconcileConfirmed(U06DeliveryStore.Command command) {
        if (command == null) throw new IllegalArgumentException("U06_RETRY_COMMAND_REQUIRED");
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("U06_RETRY_AMBIENT_TRANSACTION_FORBIDDEN");
        // A bounded operation-wide budget, measured on a monotonic clock.
        // It controls whether another DB-only attempt may START; it does not
        // establish crash-recovery authority for an ambiguous commit.
        final long startedAt = System.nanoTime();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            ensureNotCancelled();
            long remaining = TOTAL_BUDGET_NANOS - (System.nanoTime() - startedAt);
            if (remaining <= 0) throw new IllegalStateException("U06_RETRY_DEADLINE_EXCEEDED");
            TransactionTemplate tx = new TransactionTemplate(txManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
            long seconds = (remaining + 999_999_999L) / 1_000_000_000L;
            tx.setTimeout((int) Math.min(10L, Math.max(1L, seconds)));
            try {
                return tx.execute(status -> {
                    ensureNotCancelled();
                    try {
                        return new JdbcU06DeliveryStore(jdbc).reconcileConfirmed(command);
                    } catch (RuntimeException failure) {
                        // Only a 1213 error raised INSIDE the database transaction body
                        // may become a retry candidate. A commit-stage failure is never
                        // wrapped, so an ambiguous commit cannot be retried here.
                        if (isKnownMySqlDeadlock(failure))
                            throw new RolledBackDeadlockCandidate(failure);
                        throw failure;
                    }
                });
            } catch (RolledBackDeadlockCandidate deadlock) {
                // TransactionTemplate has completed its rollback before propagating
                // this signal. Rollback failure does not become this candidate.
                ensureNotCancelled();
                if (attempt == MAX_ATTEMPTS)
                    throw new IllegalStateException("U06_RETRY_EXHAUSTED");
            }
        }
        throw new IllegalStateException("U06_RETRY_UNREACHABLE");
    }

    private static void ensureNotCancelled() {
        if (Thread.currentThread().isInterrupted())
            throw new IllegalStateException("U06_RETRY_CANCELLED");
    }

    private static boolean isKnownMySqlDeadlock(Throwable failure) {
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 16; depth++, current = current.getCause()) {
            if (current instanceof SQLException) {
                SQLException sql = (SQLException) current;
                // Never treat SQLSTATE 40001 alone, timeout 1205, or connection
                // failures as proof of an uncommitted 1213 deadlock.
                return sql.getErrorCode() == 1213 && "40001".equals(sql.getSQLState());
            }
        }
        return false;
    }

    private static final class RolledBackDeadlockCandidate extends RuntimeException {
        private static final long serialVersionUID = 1L;
        RolledBackDeadlockCandidate(Throwable cause) {
            super("U06_RETRY_DEADLOCK_CANDIDATE", cause);
        }
    }
}
