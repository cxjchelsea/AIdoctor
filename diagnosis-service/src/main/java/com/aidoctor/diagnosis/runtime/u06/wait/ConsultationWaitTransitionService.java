package com.aidoctor.diagnosis.runtime.u06.wait;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRecord;import com.aidoctor.diagnosis.runtime.u01.ConsultationRepository;import org.springframework.transaction.annotation.*;import java.time.LocalDateTime;import java.util.Optional;
public final class ConsultationWaitTransitionService{
 private final ConsultationRepository consultations;private final ConsultationWaitEffectRepository effects;
 public ConsultationWaitTransitionService(ConsultationRepository c,ConsultationWaitEffectRepository e){if(c==null||e==null)throw new IllegalArgumentException("repositories required");consultations=c;effects=e;}
 @Transactional(isolation=Isolation.SERIALIZABLE) public Result establish(Command c){
  ConsultationRecord consultation=consultations.findByIdForUpdate(c.consultationId).orElseThrow(()->new IllegalStateException("U06_CONSULTATION_NOT_FOUND"));
  Optional<ConsultationWaitEffectRecord> existing=effects.findById(c.waitEffectId);if(!existing.isPresent())existing=effects.findByIdempotencyKey(c.idempotencyKey);
  if(existing.isPresent()){ConsultationWaitEffectRecord e=existing.get();if(!e.same(c.payloadFingerprint,c.consultationId,c.questionId,c.deliveryId,c.parentEffectId))throw new IllegalStateException("U06_CONSULTATION_WAITING_REPLAY_CONFLICT");
   if(!ConsultationRecord.WAITING_USER.equals(consultation.getLifecycleStatus())||!c.waitEffectId.equals(consultation.getCurrentWaitEffectId()))throw new IllegalStateException("CONSULTATION_WAIT_STATE_INCONSISTENT");
   return new Result(c.waitEffectId,consultation.getRowVersion().longValue(),true);}
  if(!ConsultationRecord.ACTIVE.equals(consultation.getLifecycleStatus())||consultation.getCurrentWaitEffectId()!=null)throw new IllegalStateException("U06_CONSULTATION_WAIT_CONFLICT");
  if(consultation.getRowVersion()==null||consultation.getRowVersion().longValue()!=c.expectedPriorRowVersion)throw new IllegalStateException("U06_CONSULTATION_ROW_VERSION_CONFLICT");
  consultation.enterWaitingUser(c.waitEffectId);consultation=consultations.saveAndFlush(consultation);long committed=consultation.getRowVersion().longValue();
  effects.saveAndFlush(new ConsultationWaitEffectRecord(c.waitEffectId,c.parentEffectId,c.consultationId,c.questionId,c.deliveryId,c.payloadFingerprint,c.idempotencyKey,c.expectedPriorRowVersion,committed,c.now));
  return new Result(c.waitEffectId,committed,false);
 }
 public static final class Command{public final String waitEffectId,parentEffectId,consultationId,questionId,deliveryId,payloadFingerprint,idempotencyKey;public final long expectedPriorRowVersion;public final LocalDateTime now;
  public Command(String w,String p,String c,String q,String d,String fp,String i,long v,LocalDateTime now){waitEffectId=req(w);parentEffectId=req(p);consultationId=req(c);questionId=req(q);deliveryId=req(d);payloadFingerprint=req(fp);idempotencyKey=req(i);if(v<0)throw new IllegalArgumentException("row version");expectedPriorRowVersion=v;this.now=now==null?LocalDateTime.now():now;}}
 public static final class Result{public final String waitEffectId;public final long committedRowVersion;public final boolean replay;Result(String w,long v,boolean r){waitEffectId=w;committedRowVersion=v;replay=r;}}
 private static String req(String v){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("value required");return v.trim();}
}
