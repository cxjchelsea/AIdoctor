package com.aidoctor.diagnosis.runtime.u07;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import static com.aidoctor.diagnosis.runtime.u07.U07SyntheticResults.*;

/** Synthetic ONLY. No Spring wiring, caller connection, datasource or callback. */
public final class U07D4SyntheticOwnedTransactionRunner {
    public enum Fault { NONE, BEFORE_ADMISSION, AFTER_COMMIT_ACK_LOSS }
    private final U07SyntheticTestTarget target;
    private final String user, password;
    public U07D4SyntheticOwnedTransactionRunner(U07SyntheticTestTarget target, String user, String password) {
        this.target = target; this.user = user; this.password = password;
    }
    private void validate(U07SyntheticInput input) {
        U07SyntheticInputValidator.validate(input);
        if (target == null || user == null || password == null) {
            throw new IllegalArgumentException("INVALID_SYNTHETIC_TARGET_OR_CREDENTIALS");
        }
    }
    private static final class Session {
        Connection connection;
        boolean started, commitEntered, committed, rollbackAttempted;
        Transaction tx = Transaction.NOT_STARTED;
        Cleanup cleanup = Cleanup.NOT_REQUIRED;
        Throwable primary;
        final List<Throwable> errors = new ArrayList<>();
        void cleanupError(Throwable error) {
            errors.add(error);
            cleanup = Cleanup.FAILED;
            if (primary == null) primary = error;
            else if (primary != error) primary.addSuppressed(error);
        }
        void rollback() {
            rollbackAttempted = true;
            try {
                connection.rollback();
                if (!commitEntered) tx = Transaction.ROLLED_BACK;
            } catch (SQLException | RuntimeException error) {
                if (!commitEntered) tx = Transaction.ABORT_UNCONFIRMED;
                cleanupError(error);
            }
        }
        void finish(Error originalFatal) {
            if (connection == null) return;
            if (cleanup != Cleanup.FAILED) cleanup = Cleanup.COMPLETE;
            Error fatal = originalFatal;
            try {
                if (started && !committed && !rollbackAttempted) rollback();
            } catch (Error error) {
                cleanupError(error);
                if (fatal == null) fatal = error;
            } finally {
                try { connection.close(); }
                catch (SQLException | RuntimeException error) { cleanupError(error); }
                catch (Error error) {
                    cleanupError(error);
                    if (fatal == null) fatal = error;
                }
            }
            // cleanupError already appends to the original primary. If the first
            // fatal came from cleanup after an ordinary failure, attach subsequent
            // cleanup errors to that fatal too, without self-suppression.
            if (fatal != null && primary != fatal) {
                for (Throwable error : errors) {
                    if (error != fatal) fatal.addSuppressed(error);
                }
            }
            if (originalFatal == null && fatal != null) throw fatal;
        }
    }

    public WriteResult execute(U07SyntheticInput input, Timestamp now, Fault fault) {
        Session s = new Session();
        Operation op = Operation.FAILED;
        Response response = Response.AVAILABLE;
        Business business = null;
        Error fatal = null;
        try {
            validate(input);
            if (now == null || fault == null) throw new IllegalArgumentException("INVALID_SYNTHETIC_INPUT");
            // Defensive copy: timestamp is caller-mutable, unlike the input.
            Timestamp frozenNow = new Timestamp(now.getTime());
            frozenNow.setNanos(now.getNanos());
            s.connection = DriverManager.getConnection(target.jdbcUrl(), user, password);
            s.connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            s.connection.setAutoCommit(false);
            s.started = true;
            s.tx = Transaction.ABORT_UNCONFIRMED;
            if (fault == Fault.BEFORE_ADMISSION) throw new IllegalStateException("U07_SYNTHETIC_BEFORE_ADMISSION");
            U07D3SyntheticTransactionCoordinator.Outcome outcome =
                    new U07D3SyntheticTransactionCoordinator().admitAndDecideSynthetic(s.connection, input, frozenNow);
            if (outcome == U07D3SyntheticTransactionCoordinator.Outcome.REJECTED_CURRENTNESS) {
                s.rollback();
                if (s.tx == Transaction.ROLLED_BACK) {
                    op = Operation.REJECTED;
                    business = Business.REJECTED_CURRENTNESS;
                }
            } else {
                s.commitEntered = true;
                s.tx = Transaction.COMMIT_OUTCOME_UNKNOWN;
                s.connection.commit();
                s.committed = true;
                s.tx = Transaction.COMMITTED;
                op = Operation.SUCCEEDED;
                if (fault == Fault.AFTER_COMMIT_ACK_LOSS) {
                    response = Response.SUPPRESSED_AFTER_COMMIT;
                } else {
                    business = outcome == U07D3SyntheticTransactionCoordinator.Outcome.ACCEPTED
                            ? Business.ACCEPTED : Business.SAME_EVENT_REPLAY;
                }
            }
        } catch (SQLException | RuntimeException failure) {
            s.primary = failure;
        } catch (Error failure) {
            fatal = failure;
            s.primary = failure;
            throw failure;
        } finally {
            s.finish(fatal);
        }
        if (s.cleanup == Cleanup.FAILED) op = Operation.FAILED;
        return new WriteResult(op, s.tx, response, s.cleanup, business, s.primary, s.errors);
    }
    public RecoveryResult inspectFresh(U07SyntheticInput input) {
        Session s = new Session();
        Read read = Read.NOT_ATTEMPTED;
        Observation observation = null;
        Error fatal = null;
        try {
            validate(input);
            read = Read.UNAVAILABLE;
            s.connection = DriverManager.getConnection(target.jdbcUrl(), user, password);
            s.connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            s.connection.setAutoCommit(false);
            s.started = true;
            observation = new Observation(new U07D4SyntheticRecoveryInspector().inspect(s.connection, input));
            read = Read.COMPLETE;
        } catch (SQLException | RuntimeException failure) {
            s.primary = failure;
        } catch (Error failure) {
            fatal = failure; s.primary = failure; throw failure;
        } finally {
            s.finish(fatal);
        }
        Operation op = read == Read.COMPLETE && s.cleanup == Cleanup.COMPLETE
                ? Operation.SUCCEEDED : Operation.FAILED;
        return new RecoveryResult(op, read, observation, s.cleanup, s.primary, s.errors);
    }
}
