package com.aidoctor.diagnosis.runtime.foundation;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

public final class RuntimeWaitCheckpointService {
    private final RuntimeThreadStateRepository threads;
    private final RuntimeWaitCheckpointRepository checkpoints;

    public RuntimeWaitCheckpointService(RuntimeThreadStateRepository t,RuntimeWaitCheckpointRepository c){
        if(t==null||c==null)throw new IllegalArgumentException("repositories required");
        threads=t;checkpoints=c;
    }

    public RuntimeThreadStateRecord initialize(String threadId,String consultationId,String createdAt){
        Optional<RuntimeThreadStateRecord> existing=threads.findById(threadId);
        if(existing.isPresent()){
            requireSameConsultation(existing.get(),consultationId);
            return existing.get();
        }

        try {
            return threads.saveAndFlush(new RuntimeThreadStateRecord(threadId,consultationId,time(createdAt)));
        } catch (DataIntegrityViolationException race) {
            RuntimeThreadStateRecord winner=threads.findById(threadId)
                    .orElseThrow(()->new IllegalStateException("U06_RUNTIME_THREAD_INITIALIZATION_RACE_UNRESOLVED",race));
            requireSameConsultation(winner,consultationId);
            return winner;
        }
    }

    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Reservation reserve(Command c){
        RuntimeThreadStateRecord t=threads.findByThreadIdForUpdate(c.threadId)
                .orElseThrow(()->new IllegalStateException("U06_RUNTIME_THREAD_NOT_FOUND"));
        Optional<RuntimeWaitCheckpointRecord>e=checkpoints.findById(c.checkpointId);

        if(e.isPresent()){
            RuntimeWaitCheckpointRecord cp=e.get();
            if(!cp.same(c.payloadFingerprint,c.runId,c.parentWaitEffectId))
                throw new IllegalStateException("U06_WAIT_CHECKPOINT_REPLAY_CONFLICT");
            if((RuntimeThreadStateRecord.WAIT_CHECKPOINTED.equals(t.getRuntimeStatus())
                    ||RuntimeThreadStateRecord.AWAITING_USER.equals(t.getRuntimeStatus()))
                    &&t.sameWait(c.runId,c.checkpointId,c.parentWaitEffectId))
                return new Reservation(cp,true);
            throw new IllegalStateException("RUNTIME_WAIT_STATE_INCONSISTENT");
        }

        if(!RuntimeThreadStateRecord.ACTIVE.equals(t.getRuntimeStatus()))
            throw new IllegalStateException("U06_RUNTIME_WAIT_CONFLICT");

        RuntimeWaitCheckpointRecord cp=new RuntimeWaitCheckpointRecord(c.checkpointId,c.consultationId,
                c.threadId,c.runId,c.questionId,c.pendingQuestionRef,c.selectionEffectId,c.deliveryEffectId,
                c.parentWaitEffectId,c.deliveryId,c.confirmationRef,c.clinicalStateVersion,
                c.consultationWaitEffectId,c.dependencyBindingRef,c.questionPolicyRef,c.payloadFingerprint,
                time(c.createdAt));

        checkpoints.saveAndFlush(cp);
        t.reserve(c.runId,c.checkpointId,c.parentWaitEffectId,time(c.createdAt));
        threads.saveAndFlush(t);
        return new Reservation(cp,false);
    }

    private static void requireSameConsultation(RuntimeThreadStateRecord record,String consultationId){
        if(!record.getConsultationId().equals(consultationId))
            throw new IllegalStateException("U06_RUNTIME_BINDING_CONFLICT");
    }

    public static final class Command{
        public final String checkpointId,consultationId,threadId,runId,questionId,pendingQuestionRef,
                selectionEffectId,deliveryEffectId,parentWaitEffectId,deliveryId,confirmationRef,
                consultationWaitEffectId,dependencyBindingRef,questionPolicyRef,payloadFingerprint,createdAt;
        public final int clinicalStateVersion;

        public Command(String cp,String c,String t,String r,String q,String pending,String sel,String del,
                String parent,String delivery,String confirm,int version,String consultWait,String dep,
                String policy,String fp,String at){
            checkpointId=req(cp);consultationId=req(c);threadId=req(t);runId=req(r);questionId=req(q);
            pendingQuestionRef=req(pending);selectionEffectId=req(sel);deliveryEffectId=req(del);
            parentWaitEffectId=req(parent);deliveryId=req(delivery);confirmationRef=req(confirm);
            clinicalStateVersion=version;consultationWaitEffectId=req(consultWait);dependencyBindingRef=dep;
            questionPolicyRef=policy;payloadFingerprint=req(fp);createdAt=req(at);
        }
    }

    public static final class Reservation{
        public final RuntimeWaitCheckpointRecord checkpoint;
        public final boolean replay;
        Reservation(RuntimeWaitCheckpointRecord c,boolean r){checkpoint=c;replay=r;}
    }

    private static LocalDateTime time(String v){return OffsetDateTime.parse(v).toLocalDateTime();}
    private static String req(String v){
        if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("value required");
        return v.trim();
    }
}
