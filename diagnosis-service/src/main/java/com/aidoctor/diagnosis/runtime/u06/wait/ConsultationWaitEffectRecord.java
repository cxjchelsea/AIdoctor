package com.aidoctor.diagnosis.runtime.u06.wait;
import lombok.Getter;import lombok.NoArgsConstructor;import javax.persistence.*;import java.time.LocalDateTime;
@Entity @Table(name="clinical_consultation_wait_effect") @Getter @NoArgsConstructor
public class ConsultationWaitEffectRecord{
 @Id @Column(name="wait_effect_id",length=128,nullable=false) private String waitEffectId;
 @Column(name="parent_delivered_wait_effect_id",length=128,nullable=false,unique=true) private String parentDeliveredWaitEffectId;
 @Column(name="consultation_id",length=128,nullable=false) private String consultationId;@Column(name="question_id",length=128,nullable=false) private String questionId;@Column(name="delivery_id",length=128,nullable=false) private String deliveryId;
 @Column(name="payload_fingerprint",length=128,nullable=false) private String payloadFingerprint;@Column(name="idempotency_key",length=128,nullable=false,unique=true) private String idempotencyKey;
 @Column(name="expected_prior_lifecycle",length=32,nullable=false) private String expectedPriorLifecycle;@Column(name="expected_prior_row_version",nullable=false) private Long expectedPriorRowVersion;
 @Column(name="committed_lifecycle",length=32,nullable=false) private String committedLifecycle;@Column(name="committed_row_version",nullable=false) private Long committedRowVersion;@Column(name="effect_status",length=32,nullable=false) private String effectStatus;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;@Column(name="committed_at",nullable=false) private LocalDateTime committedAt;
 public ConsultationWaitEffectRecord(String id,String parent,String consultation,String question,String delivery,String fp,String idem,long expected,long committed,LocalDateTime now){waitEffectId=req(id);parentDeliveredWaitEffectId=req(parent);consultationId=req(consultation);questionId=req(question);deliveryId=req(delivery);payloadFingerprint=req(fp);idempotencyKey=req(idem);expectedPriorLifecycle="ACTIVE";expectedPriorRowVersion=Long.valueOf(expected);committedLifecycle="WAITING_USER";committedRowVersion=Long.valueOf(committed);effectStatus="COMMITTED";createdAt=now;committedAt=now;}
 public boolean same(String fp,String c,String q,String d,String parent){return payloadFingerprint.equals(fp)&&consultationId.equals(c)&&questionId.equals(q)&&deliveryId.equals(d)&&parentDeliveredWaitEffectId.equals(parent);}
 private static String req(String v){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("value required");return v.trim();}
}
