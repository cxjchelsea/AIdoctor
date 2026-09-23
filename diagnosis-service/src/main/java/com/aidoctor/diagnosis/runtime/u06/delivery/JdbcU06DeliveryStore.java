package com.aidoctor.diagnosis.runtime.u06.delivery;
import org.springframework.dao.EmptyResultDataAccessException;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.transaction.annotation.Isolation;import org.springframework.transaction.annotation.Transactional;
public final class JdbcU06DeliveryStore implements U06DeliveryStore{
 private final JdbcTemplate jdbc;public JdbcU06DeliveryStore(JdbcTemplate jdbc){if(jdbc==null)throw new IllegalArgumentException("jdbc required");this.jdbc=jdbc;}
 @Transactional(isolation=Isolation.SERIALIZABLE) public Snapshot reconcileConfirmed(Command c){
  Snapshot e=find(c.deliveryEffectId);if(e!=null){if(!e.deliveryId.equals(c.deliveryId)||!"CONFIRMED".equals(e.confirmationStatus))throw new IllegalStateException("U06_DELIVERY_REPLAY_CONFLICT");return new Snapshot(e.deliveryEffectId,e.deliveryId,e.confirmationStatus,true);}
  jdbc.update("INSERT INTO u06_delivery_authority (selection_effect_id,delivery_effect_id,delivery_id,authority_status,created_at) VALUES (?,?,?,?,?)",c.selectionEffectId,c.deliveryEffectId,c.deliveryId,"CONFIRMED_TERMINAL",c.createdAt);
  jdbc.update("INSERT INTO u06_delivery_intent (delivery_effect_id,delivery_id,question_id,content_fingerprint,endpoint_ref,idempotency_key,intent_status,created_at) VALUES (?,?,?,?,?,?,?,?)",c.deliveryEffectId,c.deliveryId,c.questionId,c.contentFingerprint,c.endpointRef,c.idempotencyKey,"READY",c.createdAt);
  jdbc.update("INSERT INTO u06_delivery_attempt (attempt_id,delivery_effect_id,delivery_id,attempt_status,created_at) VALUES (?,?,?,?,?)",c.deliveryEffectId+":attempt:1",c.deliveryEffectId,c.deliveryId,"SYNTHETIC_ACCEPTED",c.createdAt);
  jdbc.update("INSERT INTO u06_delivery_receipt (receipt_id,delivery_effect_id,delivery_id,receipt_status,evidence_type,created_at) VALUES (?,?,?,?,?,?)",c.deliveryEffectId+":receipt:1",c.deliveryEffectId,c.deliveryId,"SYNTHETIC_CONFIRMED","SYNTHETIC",c.createdAt);
  jdbc.update("INSERT INTO u06_delivery_confirmation (confirmation_evaluation_id,delivery_effect_id,delivery_id,confirmation_status,confirmation_fingerprint,created_at) VALUES (?,?,?,?,?,?)",c.confirmationEvaluationId,c.deliveryEffectId,c.deliveryId,"CONFIRMED",c.confirmationFingerprint,c.createdAt);
  jdbc.update("INSERT INTO u06_delivery_ledger (delivery_effect_id,delivery_id,confirmation_status,canonical_fingerprint,created_at) VALUES (?,?,?,?,?)",c.deliveryEffectId,c.deliveryId,"CONFIRMED",c.confirmationFingerprint,c.createdAt);
  return new Snapshot(c.deliveryEffectId,c.deliveryId,"CONFIRMED",false);
 }
 private Snapshot find(String id){try{return jdbc.queryForObject("SELECT delivery_effect_id,delivery_id,confirmation_status FROM u06_delivery_ledger WHERE delivery_effect_id=?",new Object[]{id},(rs,n)->new Snapshot(rs.getString(1),rs.getString(2),rs.getString(3),true));}catch(EmptyResultDataAccessException e){return null;}}
}
