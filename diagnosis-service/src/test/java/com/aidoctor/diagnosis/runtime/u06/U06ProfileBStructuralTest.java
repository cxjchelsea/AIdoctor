package com.aidoctor.diagnosis.runtime.u06;

import com.aidoctor.diagnosis.runtime.foundation.*;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRecord;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRepository;
import com.aidoctor.diagnosis.runtime.u06.delivery.*;
import com.aidoctor.diagnosis.runtime.u06.state.U06StateValues;
import com.aidoctor.diagnosis.runtime.u06.state.U06SyntheticP01Runtime;
import com.aidoctor.diagnosis.runtime.u06.trace.U06GovernedExecutionTraceStore;
import com.aidoctor.diagnosis.runtime.u06.wait.*;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class U06ProfileBStructuralTest {
    private static final String AT="2026-09-23T00:00:00Z";
    private static final Clock CLOCK=Clock.fixed(Instant.parse(AT), ZoneOffset.UTC);

    @Test
    void p01UsesSameSyntheticStoreAndExactReplayDoesNotMutateTwice(){
        U06SyntheticP01Runtime state=U06SyntheticP01Runtime.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        Map<String,Object> value=new LinkedHashMap<String,Object>();value.put("f3_state_record_id","effect-1");value.put("status","CURRENT");
        List<U06SyntheticP01Runtime.OperationIntent> ops=Collections.singletonList(state.upsert("/patient_state/f3_gap_assessment",value));
        U06SyntheticP01Runtime.CommitEvidence first=state.commit("effect-1","proposal-1",ops,Collections.singletonList("evidence-1"),"corr-1","trace-1",AT);
        U06SyntheticP01Runtime.CommitEvidence replay=state.commit("effect-1","proposal-1",ops,Collections.singletonList("evidence-1"),"corr-1","trace-1",AT);
        assertEquals("COMMITTED",first.getResult().status);assertEquals("COMMITTED",replay.getResult().status);
        assertEquals(1,state.getMutationCount());assertEquals(state.getStoreRef(),state.getReadStoreRef());assertEquals(state.getReadStoreRef(),state.getCommitStoreRef());
        assertEquals(1,replay.getReadBack().getVersion());
    }

    @Test
    void directF1ActivationIsRejected(){
        U06AdmissionService service=new U06AdmissionService();
        U06ProfileBRequest request=request(U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,U06ProfileBRequest.F1_CLARIFICATION_ROUTING,0,"thread-1","run-1");
        assertEquals(U06AdmissionService.REJECTED_F1_DISABLED,service.admit(request).getStatus());
    }

    @Test
    void mode1ThenMode2EstablishesSyntheticWaitWithoutExternalIo(){
        U06SyntheticP01Runtime state=U06SyntheticP01Runtime.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        InMemoryDeliveryStore deliveryStore=new InMemoryDeliveryStore();
        U06SyntheticDeliveryService delivery=new U06SyntheticDeliveryService(deliveryStore);

        ConsultationRepository consultations=mock(ConsultationRepository.class);
        ConsultationWaitEffectRepository waitEffects=mock(ConsultationWaitEffectRepository.class);
        ConsultationRecord consultation=mock(ConsultationRecord.class);
        when(consultations.findByIdForUpdate("consult-1")).thenReturn(Optional.of(consultation));
        when(consultation.getLifecycleStatus()).thenReturn(ConsultationRecord.ACTIVE);
        when(consultation.getCurrentWaitEffectId()).thenReturn(null);
        when(consultation.getRowVersion()).thenReturn(Long.valueOf(0),Long.valueOf(1));
        when(consultations.saveAndFlush(consultation)).thenReturn(consultation);
        when(waitEffects.findById(anyString())).thenReturn(Optional.empty());
        when(waitEffects.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(waitEffects.saveAndFlush(any(ConsultationWaitEffectRecord.class))).thenAnswer(i->i.getArgument(0));
        ConsultationWaitTransitionService consultationWait=new ConsultationWaitTransitionService(consultations,waitEffects);

        RuntimeThreadStateRepository threadRepo=mock(RuntimeThreadStateRepository.class);
        RuntimeWaitCheckpointRepository checkpointRepo=mock(RuntimeWaitCheckpointRepository.class);
        AtomicReference<RuntimeThreadStateRecord> thread=new AtomicReference<RuntimeThreadStateRecord>();
        AtomicReference<RuntimeWaitCheckpointRecord> checkpoint=new AtomicReference<RuntimeWaitCheckpointRecord>();
        when(threadRepo.findById(anyString())).thenAnswer(i->Optional.ofNullable(thread.get()));
        when(threadRepo.findByThreadIdForUpdate(anyString())).thenAnswer(i->Optional.ofNullable(thread.get()));
        when(threadRepo.saveAndFlush(any(RuntimeThreadStateRecord.class))).thenAnswer(i->{RuntimeThreadStateRecord x=i.getArgument(0);thread.set(x);return x;});
        when(checkpointRepo.findById(anyString())).thenAnswer(i->Optional.ofNullable(checkpoint.get()));
        when(checkpointRepo.saveAndFlush(any(RuntimeWaitCheckpointRecord.class))).thenAnswer(i->{RuntimeWaitCheckpointRecord x=i.getArgument(0);checkpoint.set(x);return x;});
        RuntimeWaitCheckpointService cps=new RuntimeWaitCheckpointService(threadRepo,checkpointRepo);
        RuntimeThreadWaitTransitionService transitions=new RuntimeThreadWaitTransitionService(threadRepo,checkpointRepo);
        U06WaitCoordinator waitCoordinator=new U06WaitCoordinator(cps,transitions);

        U06GovernedExecutionTraceStore traces=new U06GovernedExecutionTraceStore(){
            public void start(String a,String b,String c,String d,String e,String f,String g){}
            public void complete(String a,String b,String c,String d){}
        };
        U06ProfileBApplicationService app=new U06ProfileBApplicationService(new U06AdmissionService(),state,delivery,consultationWait,waitCoordinator,traces);

        U06SyntheticDecisionBundle gap=new U06SyntheticDecisionBundle(
                U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED,"f3-effect-1","gap-1","DECISION_MATERIAL",true,
                null,null,null,null,null,null,null,null,null);
        U06ExecutionResult m1=app.execute(
                request(U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,null,null),
                gap,
                null,
                new U06SyntheticPostF3SafetyBarrier.Evidence(U06SyntheticPostF3SafetyBarrier.ALLOWED,"synthetic-safety-allowed-1"));
        assertEquals(U06ExecutionResult.MODE1_COMMITTED,m1.getStatus());assertEquals(1,state.readCurrent().getVersion());

        U06SyntheticDecisionBundle selected=new U06SyntheticDecisionBundle(
                U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED,"f3-effect-1","gap-1","DECISION_MATERIAL",true,
                U06SyntheticDecisionBundle.CONTINUE,U06SyntheticDecisionBundle.SELECTED,"select-effect-1",
                "question-1","semantic-1","synthetic-content-ref-1","content-fingerprint-1",null,null);
        U06SyntheticDeliveryService.ScopeAuthorization scope=new U06SyntheticDeliveryService.ScopeAuthorization(
                "consult-1",U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,"fixture-scope-1","synthetic-store-1","ci-nonprod-u06","synthetic-endpoint-1",false,false,false);
        U06ExecutionResult m2=app.execute(request(U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,U06ProfileBRequest.U05_QUESTION_ROUTING,1,"thread-1","run-1"),selected,scope);

        assertEquals(U06ExecutionResult.WAIT_ESTABLISHED,m2.getStatus());
        assertEquals(3,state.readCurrent().getVersion());
        assertTrue(state.readCurrent().exists("/patient_state/pending_question"));
        assertTrue(state.readCurrent().exists("/patient_state/questions/question-1"));
        assertEquals(RuntimeThreadStateRecord.AWAITING_USER,thread.get().getRuntimeStatus());
        assertEquals(1,deliveryStore.physicalSends);
        verify(consultation).enterWaitingUser(anyString());
    }


    @Test
    void mode1BlockedSafetyStopsAfterCanonicalCommitWithoutQuestionOrWait(){
        U06SyntheticP01Runtime state=U06SyntheticP01Runtime.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        U06ProfileBApplicationService app=minimalApp(state);
        U06SyntheticDecisionBundle gap=new U06SyntheticDecisionBundle(
                U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED,"f3-effect-blocked","gap-blocked","DECISION_MATERIAL",true,
                null,null,null,null,null,null,null,null,null);
        U06ExecutionResult result=app.execute(
                request(U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,null,null),
                gap,
                null,
                new U06SyntheticPostF3SafetyBarrier.Evidence(U06SyntheticPostF3SafetyBarrier.BLOCKED,"synthetic-safety-blocked-1"));
        assertEquals(U06ExecutionResult.MODE1_SAFETY_BLOCKED,result.getStatus());
        assertNotNull(result.getSafetyEvaluationId());
        assertEquals(1,state.getMutationCount());
        assertFalse(state.readCurrent().exists("/patient_state/pending_question"));
    }

    @Test
    void selectedMode2MissingRuntimeInputsFailsBeforeAnyMutationOrDelivery(){
        U06SyntheticP01Runtime state=U06SyntheticP01Runtime.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        InMemoryDeliveryStore deliveryStore=new InMemoryDeliveryStore();
        U06ProfileBApplicationService app=minimalApp(state,deliveryStore);
        U06SyntheticDecisionBundle selected=new U06SyntheticDecisionBundle(
                U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED,"f3-effect-1","gap-1","DECISION_MATERIAL",true,
                U06SyntheticDecisionBundle.CONTINUE,U06SyntheticDecisionBundle.SELECTED,"select-effect-missing-runtime",
                "question-1","semantic-1","synthetic-content-ref-1","content-fingerprint-1",null,null);
        U06SyntheticDeliveryService.ScopeAuthorization scope=new U06SyntheticDeliveryService.ScopeAuthorization(
                "consult-1",U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,"fixture-scope-1","synthetic-store-1",
                "ci-nonprod-u06","synthetic-endpoint-1",false,false,false);

        U06ExecutionResult result=app.execute(
                request(U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,U06ProfileBRequest.U05_QUESTION_ROUTING,0,null,null),
                selected,scope);

        assertEquals(U06ExecutionResult.ADMISSION_REJECTED,result.getStatus());
        assertEquals("U06_RUNTIME_WAIT_INPUT_REQUIRED",result.getReasonCode());
        assertEquals(0,state.getMutationCount());
        assertEquals(0,deliveryStore.physicalSends);
    }

    @Test
    void mode3SameIdentityChangedEvidenceConflictsAndNeverMutates(){
        U06SyntheticP01Runtime state=U06SyntheticP01Runtime.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        U06ProfileBApplicationService app=minimalApp(state);
        U06SyntheticDecisionBundle first=new U06SyntheticDecisionBundle(
                U06SyntheticDecisionBundle.NOT_DECIDABLE,null,null,null,false,null,null,null,null,null,null,null,
                U06SyntheticDecisionBundle.REVALIDATED_CURRENT,"revalidation-1");
        U06ExecutionResult r1=app.execute(
                request(U06ProfileBRequest.F3_CURRENT_VERSION_REVALIDATION,U06ProfileBRequest.POST_F3_SAFETY_BARRIER_ROUTING,0,null,null),
                first,null);
        assertEquals(U06ExecutionResult.REVALIDATED_CURRENT,r1.getStatus());

        U06ProfileBRequest changed=new U06ProfileBRequest(
                "req-mode3-changed","consult-1","cdp-1",U06ProfileBRequest.F3_CURRENT_VERSION_REVALIDATION,
                U06ProfileBRequest.POST_F3_SAFETY_BARRIER_ROUTING,"synthetic-source-CHANGED",0,0,
                U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING,
                "synthetic-binding-1","f3-policy-1",null,null,"event-ref-1","business-event-1",null,null,0L,
                "corr-1","trace-1",AT);
        U06ExecutionResult r2=app.execute(changed,first,null);
        assertEquals(U06ExecutionResult.FAILURE_REQUIRED,r2.getStatus());
        assertEquals(U06SyntheticRevalidationAuthority.REPLAY_CONFLICT,r2.getReasonCode());
        assertEquals(0,state.getMutationCount());
    }

    @Test
    void mode3NeverMutatesClinicalState(){
        U06SyntheticP01Runtime state=U06SyntheticP01Runtime.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        U06ProfileBApplicationService app=minimalApp(state);
        U06SyntheticDecisionBundle d=new U06SyntheticDecisionBundle(U06SyntheticDecisionBundle.NOT_DECIDABLE,null,null,null,false,null,null,null,null,null,null,null,U06SyntheticDecisionBundle.REVALIDATED_CURRENT,"revalidation-1");
        U06ExecutionResult result=app.execute(request(U06ProfileBRequest.F3_CURRENT_VERSION_REVALIDATION,U06ProfileBRequest.POST_F3_SAFETY_BARRIER_ROUTING,0,null,null),d,null);
        assertEquals(U06ExecutionResult.REVALIDATED_CURRENT,result.getStatus());assertEquals(0,state.getMutationCount());assertEquals(0,state.readCurrent().getVersion());
    }

    private U06ProfileBApplicationService minimalApp(U06SyntheticP01Runtime state){
        return minimalApp(state,new InMemoryDeliveryStore());
    }

    private U06ProfileBApplicationService minimalApp(U06SyntheticP01Runtime state,InMemoryDeliveryStore deliveryStore){
        ConsultationRepository c=mock(ConsultationRepository.class);ConsultationWaitEffectRepository e=mock(ConsultationWaitEffectRepository.class);
        RuntimeThreadStateRepository tr=mock(RuntimeThreadStateRepository.class);RuntimeWaitCheckpointRepository cp=mock(RuntimeWaitCheckpointRepository.class);
        return new U06ProfileBApplicationService(new U06AdmissionService(),state,new U06SyntheticDeliveryService(deliveryStore),
                new ConsultationWaitTransitionService(c,e),new U06WaitCoordinator(new RuntimeWaitCheckpointService(tr,cp),new RuntimeThreadWaitTransitionService(tr,cp)),
                new U06GovernedExecutionTraceStore(){public void start(String a,String b,String c,String d,String e,String f,String g){}public void complete(String a,String b,String c,String d){}});
    }

    private U06ProfileBRequest request(String mode,String source,int version,String thread,String run){
        return new U06ProfileBRequest("req-"+mode+"-"+version,"consult-1","cdp-1",mode,source,"synthetic-source-1",version,version,
                U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING,"synthetic-binding-1",
                "f3-policy-1",U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(mode)?"question-policy-1":null,
                U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(mode)?"d04-policy-1":null,
                "event-ref-1","business-event-1",thread,run,0L,"corr-1","trace-1",AT);
    }

    private static final class InMemoryDeliveryStore implements U06DeliveryStore{
        private final Map<String,Snapshot> snapshots=new LinkedHashMap<String,Snapshot>();int physicalSends;
        public synchronized Snapshot reconcileConfirmed(Command c){Snapshot e=snapshots.get(c.deliveryEffectId);if(e!=null)return new Snapshot(e.deliveryEffectId,e.deliveryId,e.confirmationStatus,true);
            physicalSends++;Snapshot s=new Snapshot(c.deliveryEffectId,c.deliveryId,"CONFIRMED",false);snapshots.put(c.deliveryEffectId,s);return s;}
    }
}
