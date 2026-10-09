package com.aidoctor.diagnosis.runtime.u07;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Synthetic-only, READ-ONLY durable evidence inspector for ambiguous commit.
 * Does not confer F8, clinical, execution or recovery mutation authority.
 * Caller supplies a NEW connection/transaction after failed or uncertain commit.
 */
public final class U07D4SyntheticRecoveryInspector {
    public enum State {
        NO_DURABLE_EVENT,
        RECEIVED_ONLY_REQUIRES_REVIEW,
        HISTORICAL_ACCEPTED,
        PARTIAL_OR_INCONSISTENT,
        IDENTITY_CONFLICT
    }

    public State inspect(Connection tx, String eventId, String consultationId,
                         String questionId, String waitEffectId,
                         String idempotencyKey, String digest) throws SQLException {
        if (tx == null || tx.isClosed() || tx.getAutoCommit()) {
            throw new IllegalStateException("U07_D4_EXPLICIT_READ_TRANSACTION_REQUIRED");
        }
        if (!safe(eventId, "synthetic-d3-") || !safe(idempotencyKey, "synthetic-d3-")
                || !safe(consultationId, "synthetic-")
                || !safe(questionId, "synthetic-") || !safe(waitEffectId, "synthetic-")
                || digest == null || digest.trim().isEmpty() || digest.length() > 128) {
            throw new IllegalArgumentException("U07_D4_SYNTHETIC_IDENTITY_REQUIRED");
        }
        String canonicalId = null;
        boolean canonicalPresent = false;
        // Query by BOTH identities to surface split/colliding identities.
        try (PreparedStatement p = tx.prepareStatement(
                "SELECT event_id,consultation_id,idempotency_key,payload_digest,event_type "
                + "FROM canonical_business_event "
                + "WHERE event_id=? OR idempotency_key=?")) {
            p.setString(1, eventId);
            p.setString(2, idempotencyKey);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    if (canonicalPresent) return State.IDENTITY_CONFLICT;
                    canonicalPresent = true;
                    canonicalId = r.getString(1);
                    if (!eventId.equals(canonicalId)
                            || !consultationId.equals(r.getString(2))
                            || !idempotencyKey.equals(r.getString(3))
                            || !digest.equals(r.getString(4))
                            || !"SYNTHETIC_TEST_ONLY".equals(r.getString(5))) {
                        return State.IDENTITY_CONFLICT;
                    }
                }
            }
        }

        boolean applicationPresent = false;
        String phase = null;
        try (PreparedStatement p = tx.prepareStatement(
                "SELECT consultation_id,question_id,parent_wait_effect_id,"
                + "payload_digest,phase,decision,effect_id,row_version "
                + "FROM u07_event_application WHERE event_id=?")) {
            p.setString(1, eventId);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    applicationPresent = true;
                    if (!consultationId.equals(r.getString(1))
                            || !questionId.equals(r.getString(2))
                            || !waitEffectId.equals(r.getString(3))
                            || !digest.equals(r.getString(4))
                            || r.getString(7) != null) {
                        return State.IDENTITY_CONFLICT;
                    }
                    phase = r.getString(5);
                    String decision = r.getString(6);
                    long revision = r.getLong(8);
                    if (("ACCEPTED".equals(phase)
                            && (!"ACCEPTED".equals(decision) || revision != 1))
                            || ("RECEIVED".equals(phase)
                            && (decision != null || revision != 0))) {
                        return State.PARTIAL_OR_INCONSISTENT;
                    }
                }
            }
        }
        if (!canonicalPresent && !applicationPresent) return State.NO_DURABLE_EVENT;
        if (!canonicalPresent || !applicationPresent) {
            return State.PARTIAL_OR_INCONSISTENT;
        }
        if ("ACCEPTED".equals(phase)) return State.HISTORICAL_ACCEPTED;
        if ("RECEIVED".equals(phase)) return State.RECEIVED_ONLY_REQUIRES_REVIEW;
        return State.PARTIAL_OR_INCONSISTENT;
    }

    private static boolean safe(String v, String prefix) {
        return v != null && v.startsWith(prefix) && v.length() <= 128;
    }
}
