package com.aidoctor.diagnosis.runtime.u07;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;

/** Runs against disposable MySQL with NO Spring, no PHI, no real dispatch. */
public final class U07D2JdbcRepositorySmoke {
    private static int assertions;
    private static final U07EventApplicationRepository APPLICATION =
            new U07EventApplicationRepository();
    private static final U07EffectOutboxRepository OUTBOX = new U07EffectOutboxRepository();
    private static final Timestamp NOW = Timestamp.valueOf("2026-01-01 00:00:00");

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static int receive(Connection c, String event, String digest) throws SQLException {
        return APPLICATION.insertReceived(c, event, "synthetic-consult", "synthetic-question",
                "synthetic-wait", digest, "synthetic-source", "synthetic-v1", 1L, NOW);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !args[0].startsWith("jdbc:mysql://127.0.0.1:")) {
            throw new IllegalArgumentException("local synthetic DB args required");
        }
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection c = DriverManager.getConnection(args[0], args[1], args[2])) {
            c.setAutoCommit(false);
            check(receive(c, "synthetic-repo-event-1", "digest-1") == 1, "receive insert");
            U07EventApplicationRepository.Record before =
                    APPLICATION.find(c, "synthetic-repo-event-1");
            check(before != null && before.revision == 0, "initial revision");
            check("RECEIVED".equals(before.phase), "initial phase");
            check(APPLICATION.acceptWithCas(c, "synthetic-repo-event-1",
                    "wrong-digest", 0, NOW) == 0, "digest mismatch fail-closed");
            check(APPLICATION.acceptWithCas(c, "synthetic-repo-event-1",
                    "digest-1", 0, NOW) == 1, "accept CAS");
            check(APPLICATION.acceptWithCas(c, "synthetic-repo-event-1",
                    "digest-1", 0, NOW) == 0, "stale CAS rejected");
            check(APPLICATION.bindEffectWithCas(c, "synthetic-repo-event-1",
                    "digest-1", 1, "synthetic-effect-1", NOW) == 1, "bind effect CAS");
            check(OUTBOX.insertSyntheticPending(c, "synthetic-effect-1",
                    "synthetic-repo-event-1", "digest-1", NOW) == 1, "outbox insert");
            c.commit();
            check(APPLICATION.find(c, "synthetic-repo-event-1").revision == 2, "durable CAS");
            U07EffectOutboxRepository.Record stored = OUTBOX.find(c, "synthetic-effect-1");
            check(stored != null && "SYNTHETIC_TEST_ONLY".equals(stored.targetType),
                    "outbox synthetic only");
            check("ACCEPTED".equals(APPLICATION.find(c, "synthetic-repo-event-1").phase),
                    "no synthetic APPLIED promotion");

            // A transaction failure after dual writes must roll back both.
            receive(c, "synthetic-repo-rollback", "digest-rollback");
            OUTBOX.insertSyntheticPending(c, "synthetic-effect-rollback",
                    "synthetic-repo-rollback", "digest-rollback", NOW);
            c.rollback();
            check(APPLICATION.find(c, "synthetic-repo-rollback") == null,
                    "application rollback");
            check(OUTBOX.find(c, "synthetic-effect-rollback") == null, "outbox rollback");

            // Unique effect ID conflicts must not commit a partial second event.
            boolean duplicateDenied = false;
            try {
                receive(c, "synthetic-repo-duplicate", "digest-duplicate");
                OUTBOX.insertSyntheticPending(c, "synthetic-effect-1",
                        "synthetic-repo-duplicate", "digest-duplicate", NOW);
            } catch (SQLException expected) {
                duplicateDenied = true;
            } finally {
                c.rollback();
            }
            check(duplicateDenied, "duplicate effect is denied");
            check(APPLICATION.find(c, "synthetic-repo-duplicate") == null,
                    "no partial application after duplicate effect");

            // Reject caller-managed connection that auto-commits.
            c.rollback();
            c.setAutoCommit(true);
            boolean denied = false;
            try {
                receive(c, "synthetic-should-not-write", "digest");
            } catch (IllegalStateException expected) {
                denied = true;
            }
            check(denied, "autocommit rejected");
        }
        System.out.println("U07_D2_MYSQL_JDBC_SMOKE=PASS assertions=" + assertions);
    }
}
