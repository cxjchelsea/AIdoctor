package com.aidoctor.diagnosis.runtime.u07;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import static com.aidoctor.diagnosis.runtime.u07.U07SyntheticResults.Evidence;

/** Internal single-statement nonlocking evidence read; no writes or terminalization. */
final class U07D4SyntheticRecoveryInspector {
    Evidence inspect(Connection tx, U07SyntheticInput in) throws SQLException {
        U07SyntheticInputValidator.validate(in);
        if (tx == null || tx.isClosed() || tx.getAutoCommit()) {
            throw new IllegalStateException("U07_D4_EXPLICIT_READ_TRANSACTION_REQUIRED");
        }
        String sql = "SELECT c.event_id c_id,c.consultation_id c_consult,c.idempotency_key c_key,"
                + "c.payload_digest c_digest,c.event_type c_type,a.event_id a_id,"
                + "a.consultation_id a_consult,a.question_id a_question,a.parent_wait_effect_id a_wait,"
                + "a.payload_digest a_digest,a.phase a_phase,a.decision a_decision,"
                + "a.row_version a_revision,a.effect_id a_effect "
                + "FROM (SELECT CAST(? AS CHAR(128)) request_event_id) q "
                + "LEFT JOIN canonical_business_event c ON (c.event_id=q.request_event_id OR c.idempotency_key=?) "
                + "LEFT JOIN u07_event_application a ON a.event_id=q.request_event_id";
        try (PreparedStatement p = tx.prepareStatement(sql)) {
            p.setString(1, in.eventId); p.setString(2, in.idempotencyKey);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new SQLException("U07_ANCHOR_MISSING");
                boolean canonical = r.getString("c_id") != null;
                boolean application = r.getString("a_id") != null;
                boolean identityConflict = canonical && (!in.eventId.equals(r.getString("c_id"))
                        || !in.consultationId.equals(r.getString("c_consult"))
                        || !in.idempotencyKey.equals(r.getString("c_key"))
                        || !in.digest.equals(r.getString("c_digest"))
                        || !"SYNTHETIC_TEST_ONLY".equals(r.getString("c_type")));
                identityConflict |= application && (!in.consultationId.equals(r.getString("a_consult"))
                        || !in.questionId.equals(r.getString("a_question"))
                        || !in.waitEffectId.equals(r.getString("a_wait"))
                        || !in.digest.equals(r.getString("a_digest")));
                String phase = r.getString("a_phase"), decision = r.getString("a_decision");
                long revision = r.getLong("a_revision");
                boolean nullRevision = r.wasNull();
                String effect = r.getString("a_effect");
                // Fully check split candidates BEFORE accepting any first row.
                if (r.next() || identityConflict) return Evidence.IDENTITY_CONFLICT;
                if (!canonical && !application) return Evidence.NO_DURABLE_EVENT;
                if (!canonical || !application || effect != null || nullRevision) {
                    return Evidence.PARTIAL_OR_INCONSISTENT;
                }
                if ("RECEIVED".equals(phase) && decision == null && revision == 0) {
                    return Evidence.RECEIVED_ONLY_REQUIRES_REVIEW;
                }
                if ("ACCEPTED".equals(phase) && "ACCEPTED".equals(decision) && revision == 1) {
                    return Evidence.HISTORICAL_ACCEPTED;
                }
                return Evidence.PARTIAL_OR_INCONSISTENT;
            }
        }
    }
}
