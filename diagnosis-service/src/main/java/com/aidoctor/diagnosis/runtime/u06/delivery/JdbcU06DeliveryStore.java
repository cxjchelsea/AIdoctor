package com.aidoctor.diagnosis.runtime.u06.delivery;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public final class JdbcU06DeliveryStore implements U06DeliveryStore {
    private final JdbcTemplate jdbc;

    public JdbcU06DeliveryStore(JdbcTemplate jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc required");
        this.jdbc = jdbc;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Snapshot reconcileConfirmed(Command c) {
        Existing existing = find(c.deliveryEffectId);
        if (existing != null) {
            if (!existing.matches(c)) throw new IllegalStateException("U06_DELIVERY_REPLAY_CONFLICT");
            if (!"CONFIRMED".equals(existing.confirmationStatus))
                throw new IllegalStateException("U06_DELIVERY_REPLAY_CONFLICT");
            return new Snapshot(existing.deliveryEffectId, existing.deliveryId, existing.confirmationStatus, true);
        }

        jdbc.update("INSERT INTO u06_delivery_authority (selection_effect_id,delivery_effect_id,delivery_id,authority_status,created_at) VALUES (?,?,?,?,?)",
                c.selectionEffectId,c.deliveryEffectId,c.deliveryId,"CONFIRMED_TERMINAL",c.createdAt);
        jdbc.update("INSERT INTO u06_delivery_intent (delivery_effect_id,delivery_id,question_id,content_fingerprint,endpoint_ref,idempotency_key,intent_status,created_at) VALUES (?,?,?,?,?,?,?,?)",
                c.deliveryEffectId,c.deliveryId,c.questionId,c.contentFingerprint,c.endpointRef,c.idempotencyKey,"READY",c.createdAt);
        jdbc.update("INSERT INTO u06_delivery_attempt (attempt_id,delivery_effect_id,delivery_id,attempt_status,created_at) VALUES (?,?,?,?,?)",
                c.deliveryEffectId+":attempt:1",c.deliveryEffectId,c.deliveryId,"SYNTHETIC_ACCEPTED",c.createdAt);
        jdbc.update("INSERT INTO u06_delivery_receipt (receipt_id,delivery_effect_id,delivery_id,receipt_status,evidence_type,created_at) VALUES (?,?,?,?,?,?)",
                c.deliveryEffectId+":receipt:1",c.deliveryEffectId,c.deliveryId,"SYNTHETIC_CONFIRMED","SYNTHETIC",c.createdAt);
        jdbc.update("INSERT INTO u06_delivery_confirmation (confirmation_evaluation_id,delivery_effect_id,delivery_id,confirmation_status,confirmation_fingerprint,created_at) VALUES (?,?,?,?,?,?)",
                c.confirmationEvaluationId,c.deliveryEffectId,c.deliveryId,"CONFIRMED",c.confirmationFingerprint,c.createdAt);
        jdbc.update("INSERT INTO u06_delivery_ledger (delivery_effect_id,delivery_id,confirmation_status,canonical_fingerprint,created_at) VALUES (?,?,?,?,?)",
                c.deliveryEffectId,c.deliveryId,"CONFIRMED",c.confirmationFingerprint,c.createdAt);
        return new Snapshot(c.deliveryEffectId,c.deliveryId,"CONFIRMED",false);
    }

    private Existing find(String effectId) {
        // A partial or duplicated durable write must never be interpreted as
        // "new" or as a complete replay, even when the ledger is absent.
        String[] tables = {
                "u06_delivery_authority", "u06_delivery_intent", "u06_delivery_attempt",
                "u06_delivery_receipt", "u06_delivery_confirmation", "u06_delivery_ledger"
        };
        int present = 0;
        for (String table : tables) {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM " + table + " WHERE delivery_effect_id=?",
                    new Object[]{effectId}, Integer.class);
            if (count == null || count < 0 || count > 1)
                throw new IllegalStateException("U06_DELIVERY_REPLAY_INCOMPLETE");
            present += count;
        }
        if (present == 0) return null;
        if (present != tables.length)
            throw new IllegalStateException("U06_DELIVERY_REPLAY_INCOMPLETE");
        try {
            return jdbc.queryForObject(
                    "SELECT a.selection_effect_id,l.delivery_effect_id,l.delivery_id,l.confirmation_status,l.canonical_fingerprint," +
                    "i.question_id,i.content_fingerprint,i.endpoint_ref,i.idempotency_key,c.confirmation_evaluation_id,c.confirmation_fingerprint," +
                    "a.delivery_id,a.authority_status,i.delivery_id,i.intent_status," +
                    "t.attempt_id,t.delivery_id,t.attempt_status," +
                    "r.receipt_id,r.delivery_id,r.receipt_status,r.evidence_type," +
                    "c.delivery_id,c.confirmation_status " +
                    "FROM u06_delivery_ledger l " +
                    "JOIN u06_delivery_authority a ON a.delivery_effect_id=l.delivery_effect_id " +
                    "JOIN u06_delivery_intent i ON i.delivery_effect_id=l.delivery_effect_id " +
                    "JOIN u06_delivery_attempt t ON t.delivery_effect_id=l.delivery_effect_id " +
                    "JOIN u06_delivery_receipt r ON r.delivery_effect_id=l.delivery_effect_id " +
                    "JOIN u06_delivery_confirmation c ON c.delivery_effect_id=l.delivery_effect_id " +
                    "WHERE l.delivery_effect_id=?",
                    new Object[]{effectId},
                    (rs,n)->new Existing(
                            rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),
                            rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9),rs.getString(10),rs.getString(11),
                            rs.getString(12),rs.getString(13),rs.getString(14),rs.getString(15),
                            rs.getString(16),rs.getString(17),rs.getString(18),rs.getString(19),
                            rs.getString(20),rs.getString(21),rs.getString(22),rs.getString(23),rs.getString(24)));
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalStateException("U06_DELIVERY_REPLAY_INCOMPLETE", e);
        } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
            throw new IllegalStateException("U06_DELIVERY_REPLAY_INCOMPLETE", e);
        }
    }

    private static final class Existing {
        final String selectionEffectId,deliveryEffectId,deliveryId,confirmationStatus,canonicalFingerprint;
        final String questionId,contentFingerprint,endpointRef,idempotencyKey,confirmationEvaluationId,confirmationFingerprint;
        final String authorityDeliveryId,authorityStatus,intentDeliveryId,intentStatus;
        final String attemptId,attemptDeliveryId,attemptStatus,receiptId,receiptDeliveryId,receiptStatus,evidenceType;
        final String confirmationDeliveryId, confirmationRecordStatus;

        Existing(String s,String e,String d,String status,String canonical,String q,String content,String endpoint,
                 String idem,String eval,String confirmFp,String authorityD,String authorityS,
                 String intentD,String intentS,String attempt,String attemptD,String attemptS,
                 String receipt,String receiptD,String receiptS,String evidence,String confirmationD,String confirmationS) {
            selectionEffectId=s;deliveryEffectId=e;deliveryId=d;confirmationStatus=status;
            canonicalFingerprint=canonical;questionId=q;contentFingerprint=content;endpointRef=endpoint;
            idempotencyKey=idem;confirmationEvaluationId=eval;confirmationFingerprint=confirmFp;
            authorityDeliveryId=authorityD;authorityStatus=authorityS;
            intentDeliveryId=intentD;intentStatus=intentS;
            attemptId=attempt;attemptDeliveryId=attemptD;attemptStatus=attemptS;
            receiptId=receipt;receiptDeliveryId=receiptD;receiptStatus=receiptS;
            evidenceType=evidence;confirmationDeliveryId=confirmationD;confirmationRecordStatus=confirmationS;
        }

        boolean matches(Command c) {
            return eq(selectionEffectId,c.selectionEffectId)
                    &&eq(deliveryEffectId,c.deliveryEffectId)
                    &&eq(deliveryId,c.deliveryId)
                    &&eq(questionId,c.questionId)
                    &&eq(contentFingerprint,c.contentFingerprint)
                    &&eq(endpointRef,c.endpointRef)
                    &&eq(idempotencyKey,c.idempotencyKey)
                    &&eq(confirmationEvaluationId,c.confirmationEvaluationId)
                    &&eq(confirmationFingerprint,c.confirmationFingerprint)
                    &&eq(canonicalFingerprint,c.confirmationFingerprint)
                    &&eq(authorityDeliveryId,c.deliveryId)
                    &&eq(authorityStatus,"CONFIRMED_TERMINAL")
                    &&eq(intentDeliveryId,c.deliveryId)
                    &&eq(intentStatus,"READY")
                    &&eq(attemptId,c.deliveryEffectId+":attempt:1")
                    &&eq(attemptDeliveryId,c.deliveryId)
                    &&eq(attemptStatus,"SYNTHETIC_ACCEPTED")
                    &&eq(receiptId,c.deliveryEffectId+":receipt:1")
                    &&eq(receiptDeliveryId,c.deliveryId)
                    &&eq(receiptStatus,"SYNTHETIC_CONFIRMED")
                    &&eq(evidenceType,"SYNTHETIC")
                    &&eq(confirmationDeliveryId,c.deliveryId)
                    &&eq(confirmationRecordStatus,"CONFIRMED");
        }

        private static boolean eq(String a,String b){return a==null?b==null:a.equals(b);}
    }
}
