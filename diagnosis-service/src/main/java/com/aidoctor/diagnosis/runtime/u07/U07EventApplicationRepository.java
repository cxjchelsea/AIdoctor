package com.aidoctor.diagnosis.runtime.u07;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * D2 repository only: no clinical mutation, runtime resume or dispatch.
 * The caller owns a JDBC transaction (autoCommit=false); this class never commits.
 * A received row does not confer authority to accept/apply.
 */
public final class U07EventApplicationRepository {
    public static final class Record {
        public final String eventId;
        public final String consultationId;
        public final String questionId;
        public final String waitEffectId;
        public final String digest;
        public final String phase;
        public final String decision;
        public final long revision;
        public final String effectId;

        private Record(ResultSet r) throws SQLException {
            eventId = r.getString("event_id");
            consultationId = r.getString("consultation_id");
            questionId = r.getString("question_id");
            waitEffectId = r.getString("parent_wait_effect_id");
            digest = r.getString("payload_digest");
            phase = r.getString("phase");
            decision = r.getString("decision");
            revision = r.getLong("row_version");
            effectId = r.getString("effect_id");
        }
    }

    private static void transaction(Connection tx) throws SQLException {
        if (tx == null || tx.isClosed() || tx.getAutoCommit()) {
            throw new IllegalStateException("U07_D2_TRANSACTION_REQUIRED");
        }
    }

    private static String required(String v) {
        if (v == null || v.trim().isEmpty() || v.length() > 128) {
            throw new IllegalArgumentException("U07_D2_INVALID_IDENTITY");
        }
        return v;
    }

    /** Caller MUST first resolve and verify canonical event and wait authority. */
    public int insertReceived(Connection tx, String eventId, String consultationId,
                              String questionId, String waitEffectId,
                              String payloadDigest, String sourceEventRef,
                              String sourceVersionRef, long sourceStateVersion,
                              Timestamp now) throws SQLException {
        transaction(tx);
        if (sourceStateVersion < 0 || now == null) throw new IllegalArgumentException("invalid version/time");
        String sql = "INSERT INTO u07_event_application "
                + "(event_id,consultation_id,question_id,parent_wait_effect_id,payload_digest,"
                + "source_event_ref,source_version_ref,source_state_version,phase,row_version,"
                + "created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,'RECEIVED',0,?,?)";
        try (PreparedStatement p = tx.prepareStatement(sql)) {
            p.setString(1, required(eventId));
            p.setString(2, required(consultationId));
            p.setString(3, required(questionId));
            p.setString(4, required(waitEffectId));
            p.setString(5, required(payloadDigest));
            p.setString(6, required(sourceEventRef));
            p.setString(7, required(sourceVersionRef));
            p.setLong(8, sourceStateVersion);
            p.setTimestamp(9, now);
            p.setTimestamp(10, now);
            return p.executeUpdate();
        }
    }

    public Record find(Connection tx, String eventId) throws SQLException {
        transaction(tx);
        try (PreparedStatement p = tx.prepareStatement(
                "SELECT event_id,consultation_id,question_id,parent_wait_effect_id,"
                        + "payload_digest,phase,decision,row_version,effect_id "
                        + "FROM u07_event_application WHERE event_id=?")) {
            p.setString(1, required(eventId));
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? new Record(r) : null;
            }
        }
    }

    /**
     * One transition from RECEIVED to ACCEPTED; row_version CAS and immutable digest.
     * This is a storage primitive ONLY; only a future authorized business service
     * may call it after independent F8 verification.
     */
    public int acceptWithCas(Connection tx, String eventId, String digest,
                             long expectedRevision, Timestamp now) throws SQLException {
        transaction(tx);
        if (expectedRevision < 0 || now == null) throw new IllegalArgumentException("invalid revision/time");
        try (PreparedStatement p = tx.prepareStatement(
                "UPDATE u07_event_application SET phase='ACCEPTED',decision='ACCEPTED',"
                        + "row_version=row_version+1,updated_at=? "
                        + "WHERE event_id=? AND payload_digest=? AND phase='RECEIVED' AND row_version=?")) {
            p.setTimestamp(1, now);
            p.setString(2, required(eventId));
            p.setString(3, required(digest));
            p.setLong(4, expectedRevision);
            return p.executeUpdate();
        }
    }

    /** Binds one non-null synthetic/local effect ID, without marking APPLIED. */
    public int bindEffectWithCas(Connection tx, String eventId, String digest,
                                 long expectedRevision, String effectId,
                                 Timestamp now) throws SQLException {
        transaction(tx);
        if (expectedRevision < 0 || now == null) throw new IllegalArgumentException("invalid revision/time");
        try (PreparedStatement p = tx.prepareStatement(
                "UPDATE u07_event_application SET effect_id=?,row_version=row_version+1,"
                        + "updated_at=? WHERE event_id=? AND payload_digest=? "
                        + "AND phase='ACCEPTED' AND effect_id IS NULL AND row_version=?")) {
            p.setString(1, required(effectId));
            p.setTimestamp(2, now);
            p.setString(3, required(eventId));
            p.setString(4, required(digest));
            p.setLong(5, expectedRevision);
            return p.executeUpdate();
        }
    }
}
