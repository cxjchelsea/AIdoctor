package com.aidoctor.diagnosis.runtime.u06;

import com.aidoctor.diagnosis.runtime.foundation.*;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRecord;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRepository;
import com.aidoctor.diagnosis.runtime.u06.delivery.*;
import com.aidoctor.diagnosis.runtime.u06.state.U06StateValues;
import com.aidoctor.diagnosis.runtime.u06.state.U06SyntheticP01Runtime;
import com.aidoctor.diagnosis.runtime.u06.state.U06SyntheticP01TestFactory;
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
        U06SyntheticP01Runtime state=U06SyntheticP01TestFactory.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
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
        U06SyntheticP01Runtime state=U06SyntheticP01TestFactory.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        InMemoryDeliveryStore deliveryStore=new InMemoryDeliveryStore();
        U06SyntheticDeliveryService delivery=new U06SyntheticDeliveryService(deliveryStore);

        ConsultationRepository consultations=mock(ConsultationRepository.class);
        ConsultationWaitEffectRepository waitEffects=mock(ConsultationWaitEffectRepository.class);
        ConsultationRecord consultation=mock(ConsultationRecord.class);
        AtomicReference<String> consultationLifecycle=new AtomicReference<String>(ConsultationRecord.ACTIVE);
        AtomicReference<String> consultationWaitEffect=new AtomicReference<String>();
        AtomicReference<Long> consultationVersion=new AtomicReference<Long>(Long.valueOf(0));
        AtomicReference<ConsultationWaitEffectRecord> waitEffectRecord=new AtomicReference<ConsultationWaitEffectRecord>();
        when(consultations.findByIdForUpdate("consult-1")).thenReturn(Optional.of(consultation));
        when(consultation.getLifecycleStatus()).thenAnswer(i->consultationLifecycle.get());
        when(consultation.getCurrentWaitEffectId()).thenAnswer(i->consultationWaitEffect.get());
        when(consultation.getRowVersion()).thenAnswer(i->consultationVersion.get());
        doAnswer(i->{consultationLifecycle.set(ConsultationRecord.WAITING_USER);consultationWaitEffect.set(i.getArgument(0));consultationVersion.set(Long.valueOf(1));return null;})
                .when(consultation).enterWaitingUser(anyString());
        when(consultations.saveAndFlush(consultation)).thenReturn(consultation);
        when(waitEffects.findById(anyString())).thenAnswer(i->{
            ConsultationWaitEffectRecord x=waitEffectRecord.get();
            return x!=null&&x.getWaitEffectId().equals(i.getArgument(0))?Optional.of(x):Optional.empty();
        });
        when(waitEffects.findByIdempotencyKey(anyString())).thenAnswer(i->{
            ConsultationWaitEffectRecord x=waitEffectRecord.get();
            return x!=null&&x.getIdempotencyKey().equals(i.getArgument(0))?Optional.of(x):Optional.empty();
        });
        when(waitEffects.saveAndFlush(any(ConsultationWaitEffectRecord.class))).thenAnswer(i->{ConsultationWaitEffectRecord x=i.getArgument(0);waitEffectRecord.set(x);return x;});
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

        U06SyntheticDecisionEngine engine=new U06SyntheticDecisionEngine();
        U06ProfileBRequest mode1Request=request(
                U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,null,null);
        U06SyntheticDecisionBundle gap=engine.decide(
                mode1Request,
                new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.SUCCESS,true,false,"gap-1","DECISION_MATERIAL",true,
                        null,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),
                state.readCurrent());
        U06ExecutionResult m1=app.execute(
                mode1Request,
                gap,
                null,
                new U06SyntheticPostF3SafetyBarrier.Evidence(U06SyntheticPostF3SafetyBarrier.ALLOWED,"synthetic-safety-allowed-1"));
        assertEquals(U06ExecutionResult.MODE1_COMMITTED,m1.getStatus());assertEquals(1,state.readCurrent().getVersion());

        U06ProfileBRequest mode2Request=request(
                U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,U06ProfileBRequest.U05_QUESTION_ROUTING,1,"thread-1","run-1");
        U06SyntheticDecisionBundle selected=engine.decide(
                mode2Request,
                new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.SUCCESS,false,false,"gap-1","DECISION_MATERIAL",true,
                        U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,
                        Collections.singletonList(new U06SyntheticDecisionInput.Candidate(
                                "candidate-1","question-1","semantic-1","synthetic-content-ref-1",
                                "content-fingerprint-1",10,true))),
                state.readCurrent());
        U06SyntheticDeliveryService.ScopeAuthorization scope=scope();
        U06ExecutionResult m2=app.execute(mode2Request,selected,scope);

        assertEquals(U06ExecutionResult.WAIT_ESTABLISHED,m2.getStatus());
        assertEquals(3,state.readCurrent().getVersion());
        assertTrue(state.readCurrent().exists("/patient_state/pending_question"));
        assertTrue(state.readCurrent().exists("/patient_state/questions/question-1"));
        assertEquals(RuntimeThreadStateRecord.AWAITING_USER,thread.get().getRuntimeStatus());
        assertEquals(1,deliveryStore.physicalSends);
        verify(consultation).enterWaitingUser(anyString());

        U06ExecutionResult replay=app.execute(mode2Request,selected,scope);
        assertEquals(U06ExecutionResult.WAIT_ESTABLISHED,replay.getStatus());
        assertEquals(m2.getDeliveryId(),replay.getDeliveryId());
        assertEquals(m2.getWaitEffectId(),replay.getWaitEffectId());
        assertEquals(m2.getCheckpointId(),replay.getCheckpointId());
        assertEquals(m2.getU07ResumeEligibilityId(),replay.getU07ResumeEligibilityId());
        assertEquals(3,state.readCurrent().getVersion());
        assertEquals(3,state.getMutationCount());
        assertEquals(1,deliveryStore.physicalSends);
        verify(consultation,times(1)).enterWaitingUser(anyString());
    }

    @Test
    void mode1ExactReplayReattachesAndChangedPayloadFailsClosed(){
        U06SyntheticP01Runtime state=U06SyntheticP01TestFactory.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        U06ProfileBApplicationService app=minimalApp(state);
        U06ProfileBRequest req=request(
                U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,null,null);
        U06SyntheticDecisionEngine engine=new U06SyntheticDecisionEngine();
        U06SyntheticDecisionBundle first=engine.decide(req,
                new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.SUCCESS,true,false,"gap-replay","DECISION_MATERIAL",true,
                        null,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),
                state.readCurrent());
        U06SyntheticPostF3SafetyBarrier.Evidence safety=
                new U06SyntheticPostF3SafetyBarrier.Evidence(U06SyntheticPostF3SafetyBarrier.ALLOWED,"synthetic-safety-replay");

        U06ExecutionResult one=app.execute(req,first,null,safety);
        U06ExecutionResult two=app.execute(req,first,null,safety);
        assertEquals(U06ExecutionResult.MODE1_COMMITTED,one.getStatus());
        assertEquals(U06ExecutionResult.MODE1_COMMITTED,two.getStatus());
        assertEquals(one.getEffectRef(),two.getEffectRef());
        assertEquals(1,state.getMutationCount());
        assertEquals(1,state.readCurrent().getVersion());

        U06SyntheticDecisionBundle changed=engine.decide(req,
                new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.SUCCESS,true,false,"gap-changed","DECISION_MATERIAL",true,
                        null,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),
                state.readCurrent());
        assertEquals(first.getF3CanonicalEffectId(),changed.getF3CanonicalEffectId());

        U06ExecutionResult conflict=app.execute(req,changed,null,safety);
        assertEquals(U06ExecutionResult.FAILURE_REQUIRED,conflict.getStatus());
        assertEquals("U06_F3_CANONICAL_REPLAY_CONFLICT",conflict.getReasonCode());
        assertEquals(1,state.getMutationCount());
    }

    @Test
    void invalidSyntheticDeliveryEnvironmentFailsClosedBeforeMutation(){
        U06SyntheticP01Runtime state=U06SyntheticP01TestFactory.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        InMemoryDeliveryStore deliveryStore=new InMemoryDeliveryStore();
        U06ProfileBApplicationService app=minimalApp(state,deliveryStore);
        U06ProfileBRequest req=request(
                U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,U06ProfileBRequest.U05_QUESTION_ROUTING,0,"thread-1","run-1");
        U06SyntheticDecisionBundle selected=new U06SyntheticDecisionEngine().decide(req,
                new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.SUCCESS,false,false,"gap-1","DECISION_MATERIAL",true,
                        U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,
                        Collections.singletonList(new U06SyntheticDecisionInput.Candidate(
                                "candidate-prod-env","question-prod-env","semantic-prod-env","synthetic-content-ref-prod",
                                "content-fingerprint-prod",1,true))),
                state.readCurrent());
        U06SyntheticDeliveryService.ScopeAuthorization invalid=new U06SyntheticDeliveryService.ScopeAuthorization(
                "scope-auth-prod","fixture-auth-prod",U06SyntheticDeliveryService.ScopeAuthorization.CURRENT,
                "consult-1",U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                "fixture-scope-prod","fixture-review-prod","synthetic-store-1",
                "production","synthetic-endpoint-prod",null,false,false,false);

        U06ExecutionResult result=app.execute(req,selected,invalid);
        assertEquals(U06ExecutionResult.ADMISSION_REJECTED,result.getStatus());
        assertEquals("U06_SYNTHETIC_DELIVERY_SCOPE_INVALID",result.getReasonCode());
        assertEquals(0,state.getMutationCount());
        assertEquals(0,deliveryStore.physicalSends);
    }

    @Test
    void runtimeExceptionTerminalizesParentTraceAsFailed(){
        U06SyntheticP01Runtime state=U06SyntheticP01TestFactory.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        U06DeliveryStore exploding=new U06DeliveryStore(){
            public Snapshot reconcileConfirmed(Command command){throw new IllegalStateException("synthetic-delivery-explosion");}
        };
        AtomicReference<String> lifecycle=new AtomicReference<String>();
        AtomicReference<String> outcome=new AtomicReference<String>();
        U06GovernedExecutionTraceStore trace=new U06GovernedExecutionTraceStore(){
            public void start(String a,String b,String c,String d,String e,String f,String g){lifecycle.set("STARTED");}
            public void complete(String a,String b,String c,String d){lifecycle.set(b);outcome.set(c);}
        };
        ConsultationRepository cr=mock(ConsultationRepository.class);
        ConsultationWaitEffectRepository er=mock(ConsultationWaitEffectRepository.class);
        RuntimeThreadStateRepository tr=mock(RuntimeThreadStateRepository.class);
        RuntimeWaitCheckpointRepository cp=mock(RuntimeWaitCheckpointRepository.class);
        U06ProfileBApplicationService app=new U06ProfileBApplicationService(
                new U06AdmissionService(),state,new U06SyntheticDeliveryService(exploding),
                new ConsultationWaitTransitionService(cr,er),
                new U06WaitCoordinator(new RuntimeWaitCheckpointService(tr,cp),new RuntimeThreadWaitTransitionService(tr,cp)),
                trace);

        U06ProfileBRequest req=request(
                U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,U06ProfileBRequest.U05_QUESTION_ROUTING,0,"thread-1","run-1");
        U06SyntheticDecisionBundle selected=new U06SyntheticDecisionEngine().decide(req,
                new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.SUCCESS,false,false,"gap-1","DECISION_MATERIAL",true,
                        U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,
                        Collections.singletonList(new U06SyntheticDecisionInput.Candidate(
                                "candidate-trace","question-trace","semantic-trace","synthetic-content-ref-trace",
                                "content-fingerprint-trace",1,true))),
                state.readCurrent());

        IllegalStateException failure=assertThrows(IllegalStateException.class,()->app.execute(req,selected,scope()));
        assertEquals("synthetic-delivery-explosion",failure.getMessage());
        assertEquals("FAILED",lifecycle.get());
        assertEquals(U06ExecutionResult.FAILURE_REQUIRED,outcome.get());
    }

    @Test
    void mode1BlockedSafetyStopsAfterCanonicalCommitWithoutQuestionOrWait(){
        U06SyntheticP01Runtime state=U06SyntheticP01TestFactory.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        U06ProfileBApplicationService app=minimalApp(state);
        U06ProfileBRequest req=request(
                U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,null,null);
        U06SyntheticDecisionBundle gap=new U06SyntheticDecisionEngine().decide(
                req,
                new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.SUCCESS,true,false,"gap-blocked","DECISION_MATERIAL",true,
                        null,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),
                state.readCurrent());
        U06ExecutionResult result=app.execute(
                req,
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
        U06SyntheticP01Runtime state=U06SyntheticP01TestFactory.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        InMemoryDeliveryStore deliveryStore=new InMemoryDeliveryStore();
        U06ProfileBApplicationService app=minimalApp(state,deliveryStore);
        U06ProfileBRequest req=request(
                U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,U06ProfileBRequest.U05_QUESTION_ROUTING,0,null,null);
        U06SyntheticDecisionBundle selected=new U06SyntheticDecisionEngine().decide(
                req,
                new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.SUCCESS,false,false,"gap-1","DECISION_MATERIAL",true,
                        U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,
                        Collections.singletonList(new U06SyntheticDecisionInput.Candidate(
                                "candidate-missing-runtime","question-1","semantic-1","synthetic-content-ref-1",
                                "content-fingerprint-1",1,true))),
                state.readCurrent());
        U06SyntheticDeliveryService.ScopeAuthorization scope=scope();

        U06ExecutionResult result=app.execute(req,selected,scope);

        assertEquals(U06ExecutionResult.ADMISSION_REJECTED,result.getStatus());
        assertEquals("U06_RUNTIME_WAIT_INPUT_REQUIRED",result.getReasonCode());
        assertEquals(0,state.getMutationCount());
        assertEquals(0,deliveryStore.physicalSends);
    }

    @Test
    void mode3SameIdentityChangedEvidenceConflictsAndNeverMutates(){
        U06SyntheticP01Runtime state=U06SyntheticP01TestFactory.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        U06ProfileBApplicationService app=minimalApp(state);
        String currentEffect=establishF3Current(app,state);

        U06SyntheticDecisionBundle first=new U06SyntheticDecisionBundle(
                U06SyntheticDecisionBundle.NOT_DECIDABLE,currentEffect,null,null,false,null,null,null,null,null,null,null,
                U06SyntheticDecisionBundle.REVALIDATED_CURRENT,"revalidation-1");
        U06ExecutionResult r1=app.execute(
                request(U06ProfileBRequest.F3_CURRENT_VERSION_REVALIDATION,U06ProfileBRequest.POST_F3_SAFETY_BARRIER_ROUTING,1,null,null),
                first,null);
        assertEquals(U06ExecutionResult.REVALIDATED_CURRENT,r1.getStatus());

        U06ProfileBRequest changed=new U06ProfileBRequest(
                "req-mode3-changed","consult-1","cdp-1",U06ProfileBRequest.F3_CURRENT_VERSION_REVALIDATION,
                U06ProfileBRequest.POST_F3_SAFETY_BARRIER_ROUTING,"synthetic-source-CHANGED",1,1,
                U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING,
                "synthetic-binding-1","f3-policy-1",null,null,"event-ref-1","business-event-1",null,null,0L,
                "corr-1","trace-1",AT);
        U06ExecutionResult r2=app.execute(changed,first,null);
        assertEquals(U06ExecutionResult.FAILURE_REQUIRED,r2.getStatus());
        assertEquals(U06SyntheticRevalidationAuthority.REPLAY_CONFLICT,r2.getReasonCode());
        assertEquals(1,state.getMutationCount());
        assertEquals(1,state.readCurrent().getVersion());
    }

    @Test
    void mode3NeverMutatesClinicalState(){
        U06SyntheticP01Runtime state=U06SyntheticP01TestFactory.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        U06ProfileBApplicationService app=minimalApp(state);
        String currentEffect=establishF3Current(app,state);
        int mutationsBefore=state.getMutationCount();
        int versionBefore=state.readCurrent().getVersion();

        U06SyntheticDecisionBundle d=new U06SyntheticDecisionBundle(
                U06SyntheticDecisionBundle.NOT_DECIDABLE,currentEffect,null,null,false,null,null,null,null,null,null,null,
                U06SyntheticDecisionBundle.REVALIDATED_CURRENT,"revalidation-current");
        U06ExecutionResult result=app.execute(
                request(U06ProfileBRequest.F3_CURRENT_VERSION_REVALIDATION,U06ProfileBRequest.POST_F3_SAFETY_BARRIER_ROUTING,versionBefore,null,null),
                d,null);

        assertEquals(U06ExecutionResult.REVALIDATED_CURRENT,result.getStatus());
        assertEquals(mutationsBefore,state.getMutationCount());
        assertEquals(versionBefore,state.readCurrent().getVersion());
    }

    @Test
    void syntheticDecisionEngineMapsNoResultAndTieFailClosed(){
        U06SyntheticP01Runtime state=U06SyntheticP01TestFactory.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        U06SyntheticDecisionEngine engine=new U06SyntheticDecisionEngine();

        U06SyntheticDecisionBundle noResult=engine.decide(
                request(U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,null,null),
                new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.NO_RESULT,false,false,null,null,false,
                        null,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),
                state.readCurrent());
        assertEquals(U06SyntheticDecisionBundle.NOT_DECIDABLE,noResult.getF3OwnerStatus());
        assertNull(noResult.getF3CanonicalEffectId());

        List<U06SyntheticDecisionInput.Candidate> tie=Arrays.asList(
                new U06SyntheticDecisionInput.Candidate("c1","q1","semantic-q1","ref-q1","fp-q1",1,true),
                new U06SyntheticDecisionInput.Candidate("c2","q2","semantic-q2","ref-q2","fp-q2",1,true));
        U06SyntheticDecisionBundle tied=engine.decide(
                request(U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,U06ProfileBRequest.U05_QUESTION_ROUTING,0,"thread-1","run-1"),
                new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.SUCCESS,false,false,"gap-1","DECISION_MATERIAL",true,
                        U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,tie),
                state.readCurrent());
        assertEquals(U06SyntheticDecisionBundle.FAILED,tied.getQuestionSelectionStatus());
        assertEquals(U06SyntheticDecisionBundle.CONTINUE,tied.getD04Status());
    }

    @Test
    void syntheticDecisionEngineDoesNotInventD04ForC03Failure(){
        U06SyntheticP01Runtime state=U06SyntheticP01TestFactory.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        U06SyntheticDecisionBundle failed=new U06SyntheticDecisionEngine().decide(
                request(U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,U06ProfileBRequest.U05_QUESTION_ROUTING,0,"thread-1","run-1"),
                new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.DEPENDENCY_FAILURE,false,false,null,null,false,
                        null,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),
                state.readCurrent());

        assertEquals(U06SyntheticDecisionBundle.FAILED,failed.getQuestionSelectionStatus());
        assertNull(failed.getD04Status());
    }

    @Test
    void syntheticDecisionEngineSuppressesCurrentSemanticDuplicate(){
        U06SyntheticP01Runtime state=U06SyntheticP01TestFactory.create("synthetic-store-1","consult-1","cdp-1",CLOCK);
        Map<String,Object> existing=new LinkedHashMap<String,Object>();
        existing.put("question_id","q-old");
        existing.put("question_semantic_key","semantic-dup");
        existing.put("status","SELECTED");
        state.commit("seed-question-effect","seed-question-proposal",
                Collections.singletonList(state.upsert("/patient_state/questions/q-old",existing)),
                Collections.singletonList("synthetic-seed"),"corr-seed","trace-seed",AT);

        U06SyntheticDecisionBundle result=new U06SyntheticDecisionEngine().decide(
                request(U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,U06ProfileBRequest.U05_QUESTION_ROUTING,1,"thread-1","run-1"),
                new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.SUCCESS,false,false,"gap-1","DECISION_MATERIAL",true,
                        U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,
                        Collections.singletonList(new U06SyntheticDecisionInput.Candidate(
                                "candidate-dup","q-new","semantic-dup","ref-new","fp-new",1,true))),
                state.readCurrent());

        assertEquals(U06SyntheticDecisionBundle.NO_SELECTION,result.getQuestionSelectionStatus());
        assertEquals(U06SyntheticDecisionBundle.STOP,result.getD04Status());
    }

    @Test
    void admissionIdentityIgnoresTransportRequestAndTraceMetadata(){
        U06AdmissionService service=new U06AdmissionService();
        U06ProfileBRequest first=new U06ProfileBRequest(
                "transport-request-a","consult-1","cdp-1",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                U06ProfileBRequest.A1_PRE_READINESS_ROUTING,"synthetic-source-1",0,0,
                U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING,
                "synthetic-binding-1","f3-policy-1",null,null,"event-ref-1","business-event-1",null,null,0L,
                "corr-a","trace-a",AT);
        U06ProfileBRequest retry=new U06ProfileBRequest(
                "transport-request-b","consult-1","cdp-1",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                U06ProfileBRequest.A1_PRE_READINESS_ROUTING,"synthetic-source-1",0,0,
                U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING,
                "synthetic-binding-1","f3-policy-1",null,null,"event-ref-1","business-event-1",null,null,0L,
                "corr-b","trace-b",AT);

        U06AdmissionService.Admission a=service.admit(first,0);
        U06AdmissionService.Admission b=service.admit(retry,0);

        assertTrue(a.isAdmitted());
        assertTrue(b.isAdmitted());
        assertEquals(a.getAdmissionId(),b.getAdmissionId());
        assertEquals(a.getFingerprint(),b.getFingerprint());
        assertTrue(b.isReplay());
    }

    private String establishF3Current(U06ProfileBApplicationService app,U06SyntheticP01Runtime state){
        U06ProfileBRequest req=request(
                U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,null,null);
        U06SyntheticDecisionBundle gap=new U06SyntheticDecisionEngine().decide(
                req,
                new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.SUCCESS,false,true,null,null,false,
                        null,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),
                state.readCurrent());
        U06ExecutionResult result=app.execute(
                req,
                gap,
                null,
                new U06SyntheticPostF3SafetyBarrier.Evidence(U06SyntheticPostF3SafetyBarrier.ALLOWED,"synthetic-safety-mode3"));
        assertEquals(U06ExecutionResult.MODE1_COMMITTED,result.getStatus());
        assertEquals(gap.getF3CanonicalEffectId(),state.readCurrent().mapString("/patient_state/f3_gap_assessment","f3_canonical_effect_id"));
        return gap.getF3CanonicalEffectId();
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

    private U06SyntheticDeliveryService.ScopeAuthorization scope(){
        return new U06SyntheticDeliveryService.ScopeAuthorization(
                "scope-auth-1","fixture-authorization-1",U06SyntheticDeliveryService.ScopeAuthorization.CURRENT,
                "consult-1",U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                "fixture-scope-1","fixture-review-1","synthetic-store-1",
                "ci-nonprod-u06","synthetic-endpoint-1",null,false,false,false);
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
