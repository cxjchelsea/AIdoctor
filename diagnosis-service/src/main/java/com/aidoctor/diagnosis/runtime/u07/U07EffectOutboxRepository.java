package com.aidoctor.diagnosis.runtime.u07;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * D2 storage-only outbox. Insert does not dispatch; consumer binding forbidden.
 * Use the SAME caller-owned JDBC transaction as the corresponding application row.
 */
public final class U07EffectOutboxRepository {
    public static final class Record {
        public final String effectId;
        public final String eventId;
        public final String targetType;
        public final String status;
        public final long attempts;
        private Record(ResultSet r) throws SQLException {
            effectId = r.getString("effect_id");
            eventId = r.getString("event_id");
            targetType = r.getString("target_type");
            status = r.getString("effect_status");
            attempts = r.getLong("attempt");
        }
    }

    private static void transaction(Connection tx) throws SQLException {
        if (tx == null || tx.isClosed() || tx.getAutoCommit()) {
            throw new IllegalStateException("U07_D2_TRANSACTION_REQUIRED");
        }
    }

    private static String required(String v, int max) {
        if (v == null || v.trim().isEmpty() || v.length() > max) {
            throw new IllegalArgumentException("U07_D2_INVALID_OUTBOX_FIELD");
        }
        return v;
    }

    /**
     * Only SYNTHETIC_TEST_ONLY is supported by D2. No real business delivery
     * target can be inserted by this primitive.
     */
    public int insertSyntheticPending(Connection tx, String effectId, String eventId,
                                      String payloadDigest, Timestamp now) throws SQLException {
        transaction(tx);
        if (now == null) throw new IllegalArgumentException("now required");
        try (PreparedStatement p = tx.prepareStatement(
                "INSERT INTO u07_effect_outbox "
                        + "(effect_id,event_id,target_type,payload_ref_or_digest,"
                        + "effect_status,attempt,created_at,updated_at) "
                        + "VALUES (?,?,'SYNTHETIC_TEST_ONLY',?,'PENDING',0,?,?)")) {
            p.setString(1, required(effectId, 128));
            p.setString(2, required(eventId, 128));
            p.setString(3, required(payloadDigest, 256));
            p.setTimestamp(4, now);
            p.setTimestamp(5, now);
            return p.executeUpdate();
        }
    }

    public Record find(Connection tx, String effectId) throws SQLException {
        transaction(tx);
        try (PreparedStatement p = tx.prepareStatement(
                "SELECT effect_id,event_id,target_type,effect_status,attempt "
                        + "FROM u07_effect_outbox WHERE effect_id=?")) {
            p.setString(1, required(effectId, 128));
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? new Record(r) : null;
            }
        }
    }
}
