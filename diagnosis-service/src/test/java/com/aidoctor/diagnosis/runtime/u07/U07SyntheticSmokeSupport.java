package com.aidoctor.diagnosis.runtime.u07;

import java.sql.SQLException;
import java.sql.Timestamp;
import static com.aidoctor.diagnosis.runtime.u07.U07SyntheticResults.*;

/** Test adapter only; no caller transaction crosses into the owned public boundary. */
final class U07SyntheticSmokeSupport {
    private U07SyntheticSmokeSupport() { }
    static U07D4SyntheticOwnedTransactionRunner runner(String[] args) {
        return new U07D4SyntheticOwnedTransactionRunner(U07SyntheticTestTarget.fromExactUrl(args[0]), args[1], args[2]);
    }
    static Business run(String[] args, U07SyntheticInput input, Timestamp now) throws SQLException {
        WriteResult result = runner(args).execute(input, now, U07D4SyntheticOwnedTransactionRunner.Fault.NONE);
        if (result.isNormalSuccess() || (result.operationStatus == Operation.REJECTED
                && result.transactionStatus == Transaction.ROLLED_BACK && result.cleanupStatus == Cleanup.COMPLETE)) {
            return result.businessOutcome;
        }
        // Unknown commit or failed cleanup MUST NOT count as an ordinary conflict.
        if (result.transactionStatus == Transaction.COMMIT_OUTCOME_UNKNOWN
                || result.transactionStatus == Transaction.ABORT_UNCONFIRMED || result.cleanupStatus == Cleanup.FAILED) {
            throw new AssertionError("uncertain transaction or failed cleanup", result.primaryFailure);
        }
        if (result.primaryFailure instanceof SQLException) throw (SQLException) result.primaryFailure;
        if (result.primaryFailure instanceof RuntimeException) throw (RuntimeException) result.primaryFailure;
        throw new AssertionError("unexpected synthetic result", result.primaryFailure);
    }
    static Evidence inspect(String[] args, U07SyntheticInput input) throws SQLException {
        RecoveryResult result = runner(args).inspectFresh(input);
        if (result.operationStatus == Operation.SUCCEEDED && result.readStatus == Read.COMPLETE) {
            return result.observation.state;
        }
        throw new AssertionError("recovery unavailable/cleanup failed", result.primaryFailure);
    }
}
