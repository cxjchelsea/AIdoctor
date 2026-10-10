package com.aidoctor.diagnosis.runtime.u07;

import java.sql.SQLException;

/** Test-only MySQL conflict allowlist. Never a production retry policy. */
final class U07D4MysqlConflictClassifier {
    private U07D4MysqlConflictClassifier() { }

    static void requireExpected(SQLException failure) throws SQLException {
        if (!isExpected(failure)) throw failure;
    }

    static boolean isExpected(SQLException failure) {
        return ("23000".equals(failure.getSQLState()) && failure.getErrorCode() == 1062)
                || ("40001".equals(failure.getSQLState()) && failure.getErrorCode() == 1213);
    }
}
