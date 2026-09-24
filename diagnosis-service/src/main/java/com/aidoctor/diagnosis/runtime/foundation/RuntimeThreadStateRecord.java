package com.aidoctor.diagnosis.runtime.foundation;
import lombok.Getter;import lombok.NoArgsConstructor;import javax.persistence.*;import java.time.LocalDateTime;
@Entity @Table(name="clinical_runtime_thread_state") @Getter @NoArgsConstructor
public class RuntimeThreadStateRecord{
 public static final String ACTIVE="ACTIVE",WAIT_CHECKPOINTED="WAIT_CHECKPOINTED",AWAITING_USER="AWAITING_USER";
 @Id @Column(name="thread_id",length=128,nullable=false) private String threadId;@Column(name="consultation_id",length=128,nullable=false,unique=true) private String consultationId;@Version @Column(name="row_version",nullable=false) private Long rowVersion;
 @Column(name="runtime_status",length=32,nullable=false) private String runtimeStatus;@Column(name="current_run_id",length=128) private String currentRunId;@Column(name="current_wait_checkpoint_id",length=128) private String currentWaitCheckpointId;@Column(name="current_wait_effect_id",length=128) private String currentWaitEffectId;
 @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;@Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 public RuntimeThreadStateRecord(String t,String c,LocalDateTime now){threadId=req(t);consultationId=req(c);rowVersion=0L;runtimeStatus=ACTIVE;createdAt=now==null?LocalDateTime.now():now;updatedAt=createdAt;}
 public void reserve(String run,String cp,String effect,LocalDateTime now){if(!ACTIVE.equals(runtimeStatus)||currentWaitCheckpointId!=null||currentWaitEffectId!=null)throw new IllegalStateException("U06_RUNTIME_WAIT_CONFLICT");runtimeStatus=WAIT_CHECKPOINTED;currentRunId=req(run);currentWaitCheckpointId=req(cp);currentWaitEffectId=req(effect);updatedAt=now==null?LocalDateTime.now():now;}
 public void enterAwaitingUser(String run,String cp,String effect,LocalDateTime now){if(!WAIT_CHECKPOINTED.equals(runtimeStatus)||!sameWait(run,cp,effect))throw new IllegalStateException("U06_RUNTIME_WAIT_CONFLICT");runtimeStatus=AWAITING_USER;updatedAt=now==null?LocalDateTime.now():now;}
 public boolean sameWait(String run,String cp,String effect){return eq(currentRunId,run)&&eq(currentWaitCheckpointId,cp)&&eq(currentWaitEffectId,effect);}private static boolean eq(String a,String b){return a==null?b==null:a.equals(b);}private static String req(String v){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("value required");return v.trim();}
}
