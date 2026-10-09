package com.aidoctor.diagnosis.runtime.u07;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * D3 SYNTHETIC-ONLY transaction harness. Never registered as a Spring bean or
 * exposed to production ingress. This is not the authoritative clinical F8.
 *
 * Uses one JDBC transaction for canonical identity, locked U06 wait basis,
 * event application admission and a synthetic positive decision. No effect
 * dispatch, APPLIED transition, U02/P02 resume, or clinical state mutation.
 */
public final class U07D3SyntheticTransactionCoordinator {
    private final U07EventApplicationRepository applications =
            new U07EventApplicationRepository();

    public enum Outcome { ACCEPTED, SAME_EVENT_REPLAY, REJECTED_CURRENTNESS }

    public static final class Input {
        public final String eventId;
        public final String consultationId;
        public final String questionId;
        public final String waitEffectId;
        public final String idempotencyKey;
        public final String digest;
        public final long expectedConsultationVersion;

        public Input(String eventId, String consultationId, String questionId,
                     String waitEffectId, String idempotencyKey, String digest,
                     long expectedConsultationVersion) {
            this.eventId = required(eventId);
            this.consultationId = required(consultationId);
            this.questionId = required(questionId);
            this.waitEffectId = required(waitEffectId);
            this.idempotencyKey = required(idempotencyKey);
            this.digest = required(digest);
            if (expectedConsultationVersion < 0) throw new IllegalArgumentException("version");
            this.expectedConsultationVersion = expectedConsultationVersion;
        }
    }

    private static String required(String value) {
        if (value == null || value.trim().isEmpty() || value.length() > 128) {
            throw new IllegalArgumentException("INVALID_SYNTHETIC_INPUT");
        }
        return value;
    }

    /** A terminal commit is owned here. Caller must provide an unused transaction. */
    public Outcome admitAndDecideSynthetic(Connection connection, Input in,
                                            Timestamp now) throws SQLException {
        if (connection == null || connection.isClosed() || connection.getAutoCommit()) {
            throw new IllegalStateException("U07_D3_TRANSACTION_REQUIRED");
        }
        if (now == null) throw new IllegalArgumentException("time");
        try {
            // A synthetic test-only event namespace is mandatory, not caller authority.
            if (!in.eventId.startsWith("synthetic-d3-")
                    || !in.idempotencyKey.startsWith("synthetic-d3-")) {
                throw new IllegalArgumentException("U07_D3_SYNTHETIC_SCOPE_ONLY");
            }
            // Lock consultation first, then current wait basis, then application.
            // Never trust caller's rowVersion without DB readback.
            String currentWait;
            long currentVersion;
            String lifecycle;
            try (PreparedStatement p = connection.prepareStatement(
                    "SELECT row_version,lifecycle_status,current_wait_effect_id "
                            + "FROM clinical_consultation WHERE consultation_id=? FOR UPDATE")) {
                p.setString(1, in.consultationId);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) {
                        connection.rollback();
                        return Outcome.REJECTED_CURRENTNESS;
                    }
                    currentVersion = r.getLong(1);
                    lifecycle = r.getString(2);
                    currentWait = r.getString(3);
                }
            }
            // An invalid currentness must never create a canonical event identity.
            if (currentVersion != in.expectedConsultationVersion
                    || !"WAITING_USER".equals(lifecycle)
                    || !Objects.equals(currentWait, in.waitEffectId)) {
                connection.rollback();
                return Outcome.REJECTED_CURRENTNESS;
            }
            try (PreparedStatement p = connection.prepareStatement(
                    "SELECT consultation_id,question_id,committed_row_version,effect_status "
                            + "FROM clinical_consultation_wait_effect WHERE wait_effect_id=? FOR UPDATE")) {
                p.setString(1, in.waitEffectId);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next() || !in.consultationId.equals(r.getString(1))
                            || !in.questionId.equals(r.getString(2))
                            || r.getLong(3) != in.expectedConsultationVersion
                            || !"COMMITTED".equals(r.getString(4))) {
                        connection.rollback();
                        return Outcome.REJECTED_CURRENTNESS;
                    }
                }
            }
            String canonicalEventId = null;
            String canonicalConsultation = null;
            String canonicalDigest = null;
            String canonicalKey = null;
            String canonicalType = null;
            try (PreparedStatement p = connection.prepareStatement(
                    "SELECT event_id,consultation_id,payload_digest,idempotency_key,event_type "
                            + "FROM canonical_business_event "
                            + "WHERE event_id=? OR idempotency_key=? FOR UPDATE")) {
                p.setString(1, in.eventId);
                p.setString(2, in.idempotencyKey);
                try (ResultSet r = p.executeQuery()) {
                    if (r.next()) {
                        canonicalEventId = r.getString(1);
                        canonicalConsultation = r.getString(2);
                        canonicalDigest = r.getString(3);
                        canonicalKey = r.getString(4);
                        canonicalType = r.getString(5);
                        if (r.next()) throw new IllegalStateException("U07_D3_CANONICAL_SPLIT");
                    }
                }
            }
            if (canonicalEventId != null) {
                if (!in.eventId.equals(canonicalEventId)
                        || !in.consultationId.equals(canonicalConsultation)
                        || !in.digest.equals(canonicalDigest)
                        || !in.idempotencyKey.equals(canonicalKey)
                        || !"SYNTHETIC_TEST_ONLY".equals(canonicalType)) {
                    throw new IllegalStateException("U07_D3_CANONICAL_CONFLICT");
                }
                U07EventApplicationRepository.Record existing =
                        applications.find(connection, in.eventId);
                if (existing == null
                        || !in.consultationId.equals(existing.consultationId)
                        || !in.questionId.equals(existing.questionId)
                        || !in.waitEffectId.equals(existing.waitEffectId)
                        || !in.digest.equals(existing.digest)) {
                    throw new IllegalStateException("U07_D3_RECONCILIATION_REQUIRED");
                }
                connection.commit();
                return Outcome.SAME_EVENT_REPLAY;
            }
            // No unsafe catch-and-read in a rollback-only transaction: SQL unique
            // races throw, rollback, and require a fresh transaction for readback.
            try (PreparedStatement p = connection.prepareStatement(
                    "INSERT INTO canonical_business_event "
                            + "(event_id,consultation_id,event_type,idempotency_key,"
                            + "payload_digest,received_at) VALUES (?,?,'SYNTHETIC_TEST_ONLY',?,?,?)")) {
                p.setString(1, in.eventId);
                p.setString(2, in.consultationId);
                p.setString(3, in.idempotencyKey);
                p.setString(4, in.digest);
                p.setTimestamp(5, now);
                p.executeUpdate();
            }
            if (applications.insertReceived(connection, in.eventId, in.consultationId,
                    in.questionId, in.waitEffectId, in.digest,
                    "synthetic-d3-authority", "synthetic-d3-v1",
                    in.expectedConsultationVersion, now) != 1) {
                throw new IllegalStateException("U07_D3_APPLICATION_INSERT_FAILED");
            }
            // SYNTHETIC positive gate only: NOT a real F8 clinical decision.
            if (applications.acceptWithCas(connection, in.eventId, in.digest, 0, now) != 1) {
                throw new IllegalStateException("U07_D3_SYNTHETIC_CAS_FAILED");
            }
            connection.commit();
            return Outcome.ACCEPTED;
        } catch (SQLException | RuntimeException failure) {
            try {
                connection.rollback();
            } catch (SQLException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }
}
