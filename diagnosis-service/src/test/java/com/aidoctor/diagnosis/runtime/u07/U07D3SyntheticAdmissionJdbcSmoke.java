package com.aidoctor.diagnosis.runtime.u07;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public final class U07D3SyntheticAdmissionJdbcSmoke {
    private static int assertions;
    private static final Timestamp NOW = Timestamp.valueOf("2026-01-01 00:00:00");

    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError(label);
    }

    private static int count(Connection c, String table, String column, String value) throws SQLException {
        // Identifiers are constants in this test, not caller-provided.
        try (PreparedStatement s = c.prepareStatement(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + "=?")) {
            s.setString(1, value);
            try (ResultSet rows = s.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    private static U07SyntheticInput command(
            String event, String key, String digest, String question, long revision) {
        return new U07SyntheticInput(
                event, "synthetic-consult-d3", question, "synthetic-wait-d3",
                key, digest, revision);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !args[0].startsWith("jdbc:mysql://127.0.0.1:")) {
            throw new IllegalArgumentException("disposable local JDBC only");
        }
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection c = DriverManager.getConnection(args[0], args[1], args[2])) {
            c.setAutoCommit(false);
            U07SyntheticInput original =
                    command("synthetic-d3-event-1", "synthetic-d3-idem-1", "digest-1",
                            "synthetic-question-d3", 1);
            check(U07SyntheticSmokeSupport.run(args, original, NOW)
                    == U07SyntheticResults.Business.ACCEPTED, "first accepted");
            check(count(c, "canonical_business_event", "event_id", original.eventId) == 1,
                    "canonical identity persisted");
            check(count(c, "u07_event_application", "event_id", original.eventId) == 1,
                    "application persisted");
            U07EventApplicationRepository.Record accepted =
                    new U07EventApplicationRepository().find(c, original.eventId);
            check("ACCEPTED".equals(accepted.phase) && accepted.revision == 1,
                    "synthetic positive decision durable");
            check(accepted.effectId == null, "no effect or APPLIED inferred");
            c.rollback();

            check(U07SyntheticSmokeSupport.run(args, original, NOW)
                    == U07SyntheticResults.Business.SAME_EVENT_REPLAY,
                    "canonical replay returns historical application");
            check(count(c, "canonical_business_event", "event_id", original.eventId) == 1,
                    "no duplicate canonical row on replay");

            boolean mismatch = false;
            try {
                U07SyntheticSmokeSupport.run(args,
                        command(original.eventId, original.idempotencyKey, "other-digest",
                                "synthetic-question-d3", 1), NOW);
            } catch (IllegalStateException expected) {
                mismatch = expected.getMessage().contains("CANONICAL_CONFLICT");
            }
            check(mismatch, "same event different digest fails closed");
            check(count(c, "canonical_business_event", "event_id", original.eventId) == 1,
                    "original canonical unchanged");

            boolean aliasConflict = false;
            try {
                U07SyntheticSmokeSupport.run(args,
                        command("synthetic-d3-alias", original.idempotencyKey, "digest-1",
                                "synthetic-question-d3", 1), NOW);
            } catch (IllegalStateException expected) {
                aliasConflict = expected.getMessage().contains("CANONICAL_CONFLICT");
            }
            check(aliasConflict, "same idempotency key cannot alias another event");
            check(count(c, "canonical_business_event", "event_id", "synthetic-d3-alias") == 0,
                    "alias not inserted");

            check(U07SyntheticSmokeSupport.run(args,
                    command("synthetic-d3-stale", "synthetic-d3-idem-stale", "digest-stale",
                            "synthetic-question-d3", 0), NOW)
                    == U07SyntheticResults.Business.REJECTED_CURRENTNESS,
                    "stale wait version rejected");
            check(count(c, "canonical_business_event", "event_id", "synthetic-d3-stale") == 0,
                    "stale event not admitted");

            check(U07SyntheticSmokeSupport.run(args,
                    command("synthetic-d3-wrong-question", "synthetic-d3-idem-q", "digest-q",
                            "synthetic-different-question", 1), NOW)
                    == U07SyntheticResults.Business.REJECTED_CURRENTNESS,
                    "wrong question rejected");
            check(count(c, "canonical_business_event", "event_id", "synthetic-d3-wrong-question") == 0,
                    "invalid question no identity");

            boolean forbidden = false;
            try {
                U07SyntheticSmokeSupport.run(args,
                        command("real-event", "synthetic-d3-idem-real", "digest",
                                "synthetic-question-d3", 1), NOW);
            } catch (IllegalArgumentException expected) {
                forbidden = expected.getMessage().contains("INVALID_SYNTHETIC_INPUT");
            }
            check(forbidden, "non-synthetic event rejected");
            check(count(c, "canonical_business_event", "event_id", "real-event") == 0,
                    "no real event touched");

            check(count(c, "u07_effect_outbox", "effect_id", "unissued") == 0,
                    "no outbox dispatch intent");
            c.rollback();
        }
        System.out.println("U07_D3_SYNTHETIC_JDBC_SMOKE=PASS assertions=" + assertions);
    }
}
