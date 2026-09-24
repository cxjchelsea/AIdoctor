package com.aidoctor.diagnosis.runtime.u06;

import com.aidoctor.diagnosis.runtime.u06.delivery.U06SyntheticDeliveryRuntime;
import com.aidoctor.diagnosis.runtime.u06.state.U06SyntheticP01Runtime;
import com.aidoctor.diagnosis.runtime.u06.state.U06SyntheticP01TestFactory;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class U06AuthoritativeContractTest {
    private static final String AT="2026-09-24T00:00:00Z";
    private static final Clock CLOCK=Clock.fixed(Instant.parse(AT), ZoneOffset.UTC);

    @Test
    void admissionCases001To018() {
        U06AdmissionService s=new U06AdmissionService();

        U06AdmissionService.Admission a1=s.admit(req("a1",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,0,evidence(true,true,true,"ALLOW_CURRENT","NOT_REQUIRED",true,"ACTIVE_SYNTHETIC",false,true)),0);
        caseAssert("U06-EV-001",a1.isAdmitted());

        caseAssert("U06-EV-002",U06AdmissionService.REJECTED_SOURCE_AUTHORITY.equals(
                new U06AdmissionService().admit(req("a2",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                        U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,0,evidence(false,false,true,"ALLOW_CURRENT","NOT_REQUIRED",true,"ACTIVE_SYNTHETIC",false,true)),0).getStatus()));

        caseAssert("U06-EV-003",U06AdmissionService.REJECTED_GATE.equals(
                new U06AdmissionService().admit(req("a3",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                        U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,0,evidence(true,true,true,"STALE","NOT_REQUIRED",true,"ACTIVE_SYNTHETIC",false,true)),0).getStatus()));

        caseAssert("U06-EV-004",U06AdmissionService.REJECTED_PERMISSION.equals(
                new U06AdmissionService().admit(req("a4",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                        U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,0,evidence(true,true,true,"RESTRICTED_CURRENT","DENIED",true,"ACTIVE_SYNTHETIC",false,true)),0).getStatus()));

        caseAssert("U06-EV-005",U06AdmissionService.FAILURE_PERMISSION_UNAVAILABLE.equals(
                new U06AdmissionService().admit(req("a5",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                        U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,0,evidence(true,true,true,"RESTRICTED_CURRENT","UNAVAILABLE",true,"ACTIVE_SYNTHETIC",false,true)),0).getStatus()));

        caseAssert("U06-EV-006",new U06AdmissionService().admit(req("a6",U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,
                U06ProfileBRequest.U05_QUESTION_ROUTING,0,0,U06AdmissionEvidence.syntheticCurrentAllow()),0).isAdmitted());

        caseAssert("U06-EV-007",U06AdmissionService.REJECTED_F1_DISABLED.equals(
                new U06AdmissionService().admit(req("a7",U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,
                        U06ProfileBRequest.F1_CLARIFICATION_ROUTING,0,0,U06AdmissionEvidence.syntheticCurrentAllow()),0).getStatus()));

        caseAssert("U06-EV-008",U06AdmissionService.REJECTED_ROUTE_CONSEQUENCE.equals(
                new U06AdmissionService().admit(req("a8",U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,
                        U06ProfileBRequest.U05_QUESTION_ROUTING,0,0,evidence(true,true,false,"ALLOW_CURRENT","NOT_REQUIRED",true,"ACTIVE_SYNTHETIC",false,true)),0).getStatus()));

        caseAssert("U06-EV-009",new U06AdmissionService().admit(req("a9",U06ProfileBRequest.F3_CURRENT_VERSION_REVALIDATION,
                U06ProfileBRequest.POST_F3_SAFETY_BARRIER_ROUTING,0,0,U06AdmissionEvidence.syntheticCurrentAllow()),0).isAdmitted());

        caseAssert("U06-EV-010",U06AdmissionService.REJECTED_MODE_SOURCE_MISMATCH.equals(
                new U06AdmissionService().admit(req("a10",U06ProfileBRequest.F3_CURRENT_VERSION_REVALIDATION,
                        "ARBITRARY_DIRECT",0,0,U06AdmissionEvidence.syntheticCurrentAllow()),0).getStatus()));

        caseAssert("U06-EV-011",U06AdmissionService.REJECTED_MODE_SOURCE_MISMATCH.equals(
                new U06AdmissionService().admit(req("a11",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                        U06ProfileBRequest.U05_QUESTION_ROUTING,0,0,U06AdmissionEvidence.syntheticCurrentAllow()),0).getStatus()));

        caseAssert("U06-EV-012",U06AdmissionService.REJECTED_CONSULTATION_CDP_MISMATCH.equals(
                new U06AdmissionService().admit(req("a12",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                        U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,0,evidence(true,true,true,"ALLOW_CURRENT","NOT_REQUIRED",false,"ACTIVE_SYNTHETIC",false,true)),0).getStatus()));

        caseAssert("U06-EV-013",U06AdmissionService.REJECTED_STALE_STATE.equals(
                new U06AdmissionService().admit(req("a13",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                        U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,0,U06AdmissionEvidence.syntheticCurrentAllow()),1).getStatus()));

        caseAssert("U06-EV-014",U06AdmissionService.REJECTED_DEPENDENCY.equals(
                new U06AdmissionService().admit(req("a14",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                        U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,0,evidence(true,true,true,"ALLOW_CURRENT","NOT_REQUIRED",true,"EXPIRED",false,true)),0).getStatus()));

        U06AdmissionService replayService=new U06AdmissionService();
        U06ProfileBRequest r15=req("transport-a",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,0,U06AdmissionEvidence.syntheticCurrentAllow());
        U06AdmissionService.Admission first15=replayService.admit(r15,0);
        U06ProfileBRequest retry15=req("transport-b",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,0,U06AdmissionEvidence.syntheticCurrentAllow());
        U06AdmissionService.Admission second15=replayService.admit(retry15,1,true);
        caseAssert("U06-EV-015",first15.getAdmissionId().equals(second15.getAdmissionId())&&second15.isReplay());

        U06AdmissionService conflictService=new U06AdmissionService();
        conflictService.admit(req("a16-first",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,0,U06AdmissionEvidence.syntheticCurrentAllow()),0);
        IllegalStateException replayConflict=assertThrows(IllegalStateException.class,()->conflictService.admit(
                req("a16-second",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                        U06ProfileBRequest.A1_PRE_READINESS_ROUTING,1,0,U06AdmissionEvidence.syntheticCurrentAllow()),0));
        caseAssert("U06-EV-016","U06_ADMISSION_REPLAY_CONFLICT".equals(replayConflict.getMessage()));

        caseAssert("U06-EV-017",U06AdmissionService.REJECTED_SOURCE_SUPERSEDED.equals(
                new U06AdmissionService().admit(req("a17",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                        U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,0,evidence(true,true,true,"ALLOW_CURRENT","NOT_REQUIRED",true,"ACTIVE_SYNTHETIC",true,true)),0).getStatus()));

        caseAssert("U06-EV-018",U06AdmissionService.REJECTED_SYNTHETIC_MARKER.equals(
                new U06AdmissionService().admit(req("a18",U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                        U06ProfileBRequest.A1_PRE_READINESS_ROUTING,0,0,evidence(true,true,true,"ALLOW_CURRENT","NOT_REQUIRED",true,"ACTIVE_SYNTHETIC",false,false)),0).getStatus()));
    }

    @Test
    void decisionCases019To041() {
        U06SyntheticDecisionEngine engine=new U06SyntheticDecisionEngine();

        U06SyntheticP01Runtime s19=state();
        U06SyntheticDecisionBundle d19=engine.decide(mode1(0),new U06SyntheticDecisionInput(
                U06SyntheticDecisionInput.SUCCESS,true,false,"gap-19","DECISION_MATERIAL",true,null,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),s19.readCurrent());
        caseAssert("U06-EV-019",U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED.equals(d19.getF3OwnerStatus())&&d19.getQuestionSelectionStatus()==null);

        U06SyntheticDecisionBundle d20=engine.decide(mode1(0),new U06SyntheticDecisionInput(
                U06SyntheticDecisionInput.SUCCESS,false,true,null,null,false,null,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),state().readCurrent());
        caseAssert("U06-EV-020",U06SyntheticDecisionBundle.NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED.equals(d20.getF3OwnerStatus()));

        U06SyntheticDecisionBundle d21=engine.decide(mode1(0),new U06SyntheticDecisionInput(
                U06SyntheticDecisionInput.SUCCESS,false,false,null,null,false,null,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),state().readCurrent());
        caseAssert("U06-EV-021",U06SyntheticDecisionBundle.NOT_DECIDABLE.equals(d21.getF3OwnerStatus()));

        U06SyntheticDecisionBundle d22=engine.decide(mode1(0),input(U06SyntheticDecisionInput.NO_RESULT,null,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),state().readCurrent());
        caseAssert("U06-EV-022",U06SyntheticDecisionBundle.NOT_DECIDABLE.equals(d22.getF3OwnerStatus())&&d22.getF3CanonicalEffectId()==null);

        U06SyntheticDecisionBundle d23=engine.decide(mode1(0),input(U06SyntheticDecisionInput.INSUFFICIENT_INFORMATION,null,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),state().readCurrent());
        caseAssert("U06-EV-023",U06SyntheticDecisionBundle.NOT_DECIDABLE.equals(d23.getF3OwnerStatus()));

        for(String c03:Arrays.asList(U06SyntheticDecisionInput.DEPENDENCY_FAILURE,U06SyntheticDecisionInput.TIMEOUT,U06SyntheticDecisionInput.INVALID_OUTPUT)) {
            U06SyntheticDecisionBundle dx=engine.decide(mode1(0),input(c03,null,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),state().readCurrent());
            caseAssert("U06-EV-024",U06SyntheticDecisionBundle.FAILED.equals(dx.getF3OwnerStatus())&&dx.getF3CanonicalEffectId()==null);
        }

        U06SyntheticDecisionInput.Candidate f1Candidate=candidate("c25","q25","sem25",1,true);
        U06SyntheticDecisionBundle d25=engine.decide(mode2(0),new U06SyntheticDecisionInput(
                U06SyntheticDecisionInput.SUCCESS,false,false,"f1-requirement-25","F1_MINIMAL_CLARIFICATION",true,
                U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,Collections.singletonList(f1Candidate)),state().readCurrent());
        caseAssert("U06-EV-025","f1-requirement-25".equals(d25.getGapId())&&U06SyntheticDecisionBundle.SELECTED.equals(d25.getQuestionSelectionStatus()));

        U06SyntheticDecisionBundle d26=engine.decide(mode2(0),new U06SyntheticDecisionInput(
                U06SyntheticDecisionInput.SUCCESS,false,false,"gap-26","DECISION_MATERIAL",true,
                U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,Collections.singletonList(candidate("c26","q26","sem26",1,true))),state().readCurrent());
        caseAssert("U06-EV-026",U06SyntheticDecisionBundle.SELECTED.equals(d26.getQuestionSelectionStatus()));

        U06SyntheticDecisionBundle old=engine.decide(mode1(0),new U06SyntheticDecisionInput(
                U06SyntheticDecisionInput.SUCCESS,true,false,"gap-old","DECISION_MATERIAL",true,null,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),state().readCurrent());
        U06SyntheticDecisionBundle fresh=engine.decide(mode2(0),new U06SyntheticDecisionInput(
                U06SyntheticDecisionInput.SUCCESS,false,false,"gap-fresh","DECISION_MATERIAL",true,
                U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,Collections.singletonList(candidate("fresh","q27","sem27",1,true))),state().readCurrent());
        caseAssert("U06-EV-027",old.getF3CanonicalEffectId()!=null&&fresh.getQuestionSelectionEffectId()!=null&&!old.getF3CanonicalEffectId().equals(fresh.getQuestionSelectionEffectId()));

        for(String status:Arrays.asList("SELECTED","DELIVERED_TO_USER","ANSWER_RECEIVED")) {
            U06SyntheticP01Runtime dup=state();
            seedQuestion(dup,"old-"+status,"dup-key",status);
            U06SyntheticDecisionBundle dd=engine.decide(mode2(1),new U06SyntheticDecisionInput(
                    U06SyntheticDecisionInput.SUCCESS,false,false,"gap","DECISION_MATERIAL",true,
                    U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,Collections.singletonList(candidate("dup","q-new","dup-key",1,true))),dup.readCurrent());
            String id="SELECTED".equals(status)?"U06-EV-028":("DELIVERED_TO_USER".equals(status)?"U06-EV-029":"U06-EV-030");
            caseAssert(id,U06SyntheticDecisionBundle.NO_SELECTION.equals(dd.getQuestionSelectionStatus()));
        }

        U06SyntheticP01Runtime userUnknown=state();
        seedQuestion(userUnknown,"old-unknown","unknown-key","ANSWER_RECEIVED");
        U06SyntheticDecisionBundle d31=engine.decide(mode2(1),new U06SyntheticDecisionInput(
                U06SyntheticDecisionInput.SUCCESS,false,false,"gap-user-unknown","DECISION_MATERIAL",true,
                U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,Collections.singletonList(candidate("c31","q31","unknown-key",1,true))),userUnknown.readCurrent());
        caseAssert("U06-EV-031",U06SyntheticDecisionBundle.NO_SELECTION.equals(d31.getQuestionSelectionStatus()));

        U06SyntheticDecisionBundle d32=engine.decide(mode2(0),new U06SyntheticDecisionInput(
                U06SyntheticDecisionInput.SUCCESS,false,false,"gap-unmeasured","OFFLINE_REQUIRED",false,
                U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),state().readCurrent());
        caseAssert("U06-EV-032",U06SyntheticDecisionBundle.NO_SELECTION.equals(d32.getQuestionSelectionStatus()));

        U06SyntheticDecisionBundle d33=engine.decide(mode2(0),new U06SyntheticDecisionInput(
                U06SyntheticDecisionInput.SUCCESS,false,false,"gap33","DECISION_MATERIAL",true,
                U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,Collections.singletonList(candidate("c33","q33","sem33",1,true))),state().readCurrent());
        caseAssert("U06-EV-033",U06SyntheticDecisionBundle.CONTINUE.equals(d33.getD04Status())&&U06SyntheticDecisionBundle.SELECTED.equals(d33.getQuestionSelectionStatus()));

        List<U06SyntheticDecisionInput.Candidate> ordered=Arrays.asList(candidate("c34a","q34a","sem34a",2,true),candidate("c34b","q34b","sem34b",1,true));
        U06SyntheticDecisionBundle d34=engine.decide(mode2(0),new U06SyntheticDecisionInput(
                U06SyntheticDecisionInput.SUCCESS,false,false,"gap34","DECISION_MATERIAL",true,U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,ordered),state().readCurrent());
        caseAssert("U06-EV-034","q34b".equals(d34.getQuestionId())&&U06SyntheticDecisionBundle.SELECTED.equals(d34.getQuestionSelectionStatus()));

        List<U06SyntheticDecisionInput.Candidate> tie=Arrays.asList(candidate("c35a","q35a","sem35a",1,true),candidate("c35b","q35b","sem35b",1,true));
        U06SyntheticDecisionBundle d35=engine.decide(mode2(0),new U06SyntheticDecisionInput(
                U06SyntheticDecisionInput.SUCCESS,false,false,"gap35","DECISION_MATERIAL",true,U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,tie),state().readCurrent());
        caseAssert("U06-EV-035",U06SyntheticDecisionBundle.CONTINUE.equals(d35.getD04Status())&&U06SyntheticDecisionBundle.FAILED.equals(d35.getQuestionSelectionStatus()));

        U06SyntheticDecisionBundle d36=engine.decide(mode2(0),input(U06SyntheticDecisionInput.SUCCESS,U06SyntheticDecisionInput.POLICY_STOP,Collections.singletonList(candidate("c36","q36","sem36",1,true))),state().readCurrent());
        caseAssert("U06-EV-036",U06SyntheticDecisionBundle.STOP.equals(d36.getD04Status())&&U06SyntheticDecisionBundle.NO_SELECTION.equals(d36.getQuestionSelectionStatus()));

        U06SyntheticDecisionBundle d37=engine.decide(mode2(0),input(U06SyntheticDecisionInput.SUCCESS,U06SyntheticDecisionInput.POLICY_FAIL,Collections.singletonList(candidate("c37","q37","sem37",1,true))),state().readCurrent());
        caseAssert("U06-EV-037",U06SyntheticDecisionBundle.FAILED.equals(d37.getD04Status())&&U06SyntheticDecisionBundle.FAILED.equals(d37.getQuestionSelectionStatus()));

        U06SyntheticDecisionBundle d38=engine.decide(mode2(0),input(U06SyntheticDecisionInput.NO_RESULT,U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),state().readCurrent());
        caseAssert("U06-EV-038",d38.getD04Status()==null&&U06SyntheticDecisionBundle.NO_SELECTION.equals(d38.getQuestionSelectionStatus()));

        U06SyntheticDecisionBundle d39=engine.decide(mode2(0),input(U06SyntheticDecisionInput.INSUFFICIENT_INFORMATION,U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),state().readCurrent());
        caseAssert("U06-EV-039",U06SyntheticDecisionBundle.NO_SELECTION.equals(d39.getQuestionSelectionStatus()));

        U06NoProgressRouter router=new U06NoProgressRouter();
        caseAssert("U06-EV-040",U06NoProgressRouter.RETURN_U01_F1.equals(router.route(U06NoProgressRouter.F1)));
        caseAssert("U06-EV-041",U06NoProgressRouter.READINESS_REEVALUATION_REQUIRED_U05_D03.equals(router.route(U06NoProgressRouter.F3)));
    }

    @Test
    void deliveryCases062To080() {
        U06SyntheticDeliveryRuntime d=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot i62=d.createIntent("consult","sel62","q62","fp62","synthetic-endpoint","policy",AT);
        caseAssert("U06-EV-062",U06SyntheticDeliveryRuntime.READY.equals(i62.intentStatus)&&i62.attemptCount==0);

        U06SyntheticDeliveryRuntime stale=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot staleIntent=stale.createIntent("consult","sel63","q63","fp63","synthetic-endpoint","policy",AT);
        stale.expireBeforeSend(staleIntent.deliveryEffectId);
        caseAssert("U06-EV-063",stale.snapshot(staleIntent.deliveryEffectId).attemptCount==0&&U06SyntheticDeliveryRuntime.CANCELLED_TERMINAL.equals(stale.snapshot(staleIntent.deliveryEffectId).authorityStatus));

        U06SyntheticDeliveryRuntime.Snapshot replay=d.createIntent("consult","sel62","q62","fp62","synthetic-endpoint","policy",AT);
        caseAssert("U06-EV-064",replay.replay&&i62.deliveryId.equals(replay.deliveryId)&&i62.idempotencyKey.equals(replay.idempotencyKey));

        U06SyntheticDeliveryRuntime conflict=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot c65=conflict.createIntent("consult","sel65","q65","fp65","synthetic-endpoint","policy",AT);
        IllegalStateException e65=assertThrows(IllegalStateException.class,()->conflict.createIntent("consult","sel65","q65","CHANGED","synthetic-endpoint","policy",AT));
        caseAssert("U06-EV-065","U06_SECOND_ACTIVE_DELIVERY_EFFECT_PROHIBITED".equals(e65.getMessage())||"U06_DELIVERY_REPLAY_CONFLICT".equals(e65.getMessage()));

        U06SyntheticDeliveryRuntime one=new U06SyntheticDeliveryRuntime();
        one.createIntent("consult","sel66","q66","fp66","synthetic-endpoint","policy",AT);
        IllegalStateException e66=assertThrows(IllegalStateException.class,()->one.createIntent("consult","sel66","q66","fp66","synthetic-endpoint-2","policy",AT));
        caseAssert("U06-EV-066","U06_SECOND_ACTIVE_DELIVERY_EFFECT_PROHIBITED".equals(e66.getMessage()));

        U06SyntheticDeliveryRuntime reb=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot old67=reb.createIntent("consult","sel67","q67","fp67","synthetic-endpoint-a","policy",AT);
        U06SyntheticDeliveryRuntime.Snapshot new67=reb.rebindBeforeSend(old67.deliveryEffectId,"synthetic-endpoint-b",true,AT);
        caseAssert("U06-EV-067",!old67.deliveryEffectId.equals(new67.deliveryEffectId)&&U06SyntheticDeliveryRuntime.ACTIVE_PENDING.equals(new67.authorityStatus));

        U06SyntheticDeliveryRuntime amb68=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot i68=amb68.createIntent("consult","sel68","q68","fp68","synthetic-endpoint","policy",AT);
        amb68.attempt(i68.deliveryEffectId,"AMBIGUOUS_NO_QUERY_NO_IDEMPOTENCY",AT);
        IllegalStateException e68=assertThrows(IllegalStateException.class,()->amb68.rebindBeforeSend(i68.deliveryEffectId,"synthetic-endpoint-2",true,AT));
        caseAssert("U06-EV-068","U06_DELIVERY_REBIND_DENIED".equals(e68.getMessage()));

        U06SyntheticDeliveryRuntime order=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot i69=order.createIntent("consult","sel69","q69","fp69","synthetic-endpoint","policy",AT);
        U06SyntheticDeliveryRuntime.AttemptResult a69=order.attempt(i69.deliveryEffectId,"RECEIPT_ACCEPTED_ONLY",AT);
        caseAssert("U06-EV-069",i69.attemptCount==0&&a69.snapshot.attemptCount==1);

        U06SyntheticDeliveryRuntime retry70=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot i70=retry70.createIntent("consult","sel70","q70","fp70","synthetic-endpoint","policy",AT);
        U06SyntheticDeliveryRuntime.AttemptResult a701=retry70.attempt(i70.deliveryEffectId,"TRANSIENT_NOT_DELIVERED_RETRY_ALLOWED",AT);
        U06SyntheticDeliveryRuntime.AttemptResult a702=retry70.retrySameEffect(i70.deliveryEffectId,"SYNTHETIC_DELIVERED",AT);
        caseAssert("U06-EV-070",!a701.attemptId.equals(a702.attemptId)&&a701.deliveryId.equals(a702.deliveryId)&&a701.idempotencyKey.equals(a702.idempotencyKey));

        U06SyntheticDeliveryRuntime accepted=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot i71=accepted.createIntent("consult","sel71","q71","fp71","synthetic-endpoint","policy",AT);
        U06SyntheticDeliveryRuntime.AttemptResult a71=accepted.attempt(i71.deliveryEffectId,"RECEIPT_ACCEPTED_ONLY",AT);
        caseAssert("U06-EV-071",!U06SyntheticDeliveryRuntime.CONFIRMED.equals(a71.confirmationStatus));

        U06SyntheticDeliveryRuntime delivered=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot i72=delivered.createIntent("consult","sel72","q72","fp72","synthetic-endpoint","policy",AT);
        U06SyntheticDeliveryRuntime.AttemptResult a72=delivered.attempt(i72.deliveryEffectId,"SYNTHETIC_DELIVERED",AT);
        caseAssert("U06-EV-072",U06SyntheticDeliveryRuntime.CONFIRMED.equals(a72.confirmationStatus));

        U06SyntheticDeliveryRuntime query=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot i73=query.createIntent("consult","sel73","q73","fp73","synthetic-endpoint","policy",AT);
        query.attempt(i73.deliveryEffectId,"AMBIGUOUS_STATUS_QUERY_AVAILABLE",AT);
        U06SyntheticDeliveryRuntime.Snapshot q73=query.reconcileStatusQuery(i73.deliveryEffectId,U06SyntheticDeliveryRuntime.NOT_CONFIRMED,AT);
        caseAssert("U06-EV-073",U06SyntheticDeliveryRuntime.RETRYABLE_NOT_CONFIRMED.equals(q73.authorityStatus));

        U06SyntheticDeliveryRuntime idem=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot i74=idem.createIntent("consult","sel74","q74","fp74","synthetic-endpoint","policy",AT);
        U06SyntheticDeliveryRuntime.AttemptResult a741=idem.attempt(i74.deliveryEffectId,"AMBIGUOUS_IDEMPOTENT_RESEND",AT);
        U06SyntheticDeliveryRuntime.AttemptResult a742=idem.retrySameEffect(i74.deliveryEffectId,"SYNTHETIC_DELIVERED",AT);
        caseAssert("U06-EV-074",a741.idempotencyKey.equals(a742.idempotencyKey));

        U06SyntheticDeliveryRuntime blind=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot i75=blind.createIntent("consult","sel75","q75","fp75","synthetic-endpoint","policy",AT);
        blind.attempt(i75.deliveryEffectId,"AMBIGUOUS_NO_QUERY_NO_IDEMPOTENCY",AT);
        IllegalStateException e75=assertThrows(IllegalStateException.class,()->blind.retrySameEffect(i75.deliveryEffectId,"AMBIGUOUS_NO_QUERY_NO_IDEMPOTENCY",AT));
        caseAssert("U06-EV-075","U06_BLIND_RESEND_PROHIBITED".equals(e75.getMessage())&&blind.physicalAttemptCount(i75.deliveryEffectId)==1);

        U06SyntheticDeliveryRuntime transient76=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot i76=transient76.createIntent("consult","sel76","q76","fp76","synthetic-endpoint","policy",AT);
        U06SyntheticDeliveryRuntime.AttemptResult a76=transient76.attempt(i76.deliveryEffectId,"TRANSIENT_NOT_DELIVERED_RETRY_ALLOWED",AT);
        caseAssert("U06-EV-076",U06SyntheticDeliveryRuntime.RETRYABLE_NOT_CONFIRMED.equals(a76.authorityStatus));

        U06SyntheticDeliveryRuntime exhausted=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot i77=exhausted.createIntent("consult","sel77","q77","fp77","synthetic-endpoint","policy",AT);
        exhausted.attempt(i77.deliveryEffectId,"RETRY_EXHAUSTED",AT);
        IllegalStateException e77=assertThrows(IllegalStateException.class,()->exhausted.attempt(i77.deliveryEffectId,"SYNTHETIC_DELIVERED",AT));
        caseAssert("U06-EV-077","U06_DELIVERY_TERMINAL_NO_SEND".equals(e77.getMessage()));

        U06SyntheticDeliveryRuntime later=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot i78=later.createIntent("consult","sel78","q78","fp78","synthetic-endpoint","policy",AT);
        later.attempt(i78.deliveryEffectId,"AMBIGUOUS_STATUS_QUERY_AVAILABLE",AT);
        int before78=later.confirmationEvaluationCount(i78.deliveryEffectId);
        U06SyntheticDeliveryRuntime.Snapshot s78=later.applyLaterEvidence(i78.deliveryEffectId,U06SyntheticDeliveryRuntime.CONFIRMED,"later-receipt",AT);
        caseAssert("U06-EV-078",s78.confirmationEvaluationCount==before78+1&&U06SyntheticDeliveryRuntime.CONFIRMED.equals(s78.confirmationStatus));

        U06SyntheticDeliveryRuntime protected79=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot i79=protected79.createIntent("consult","sel79","q79","fp79","synthetic-endpoint","policy",AT);
        protected79.attempt(i79.deliveryEffectId,"SYNTHETIC_DELIVERED",AT);
        IllegalStateException e79=assertThrows(IllegalStateException.class,()->protected79.applyLaterEvidence(i79.deliveryEffectId,U06SyntheticDeliveryRuntime.NOT_CONFIRMED,"conflict",AT));
        caseAssert("U06-EV-079","U06_DELIVERY_CONFIRMATION_EVIDENCE_CONFLICT".equals(e79.getMessage())&&U06SyntheticDeliveryRuntime.CONFIRMED.equals(protected79.snapshot(i79.deliveryEffectId).confirmationStatus));

        U06SyntheticDeliveryRuntime oneEffect=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot i80=oneEffect.createIntent("consult","sel80","q80","fp80","synthetic-endpoint","policy",AT);
        oneEffect.attempt(i80.deliveryEffectId,"SYNTHETIC_DELIVERED",AT);
        U06SyntheticDeliveryRuntime.Snapshot replay80=oneEffect.createIntent("consult","sel80","q80","fp80","synthetic-endpoint","policy",AT);
        caseAssert("U06-EV-080",replay80.deliveryEffectId.equals(i80.deliveryEffectId)&&oneEffect.physicalAttemptCount(i80.deliveryEffectId)==1);
    }

    private static U06AdmissionEvidence evidence(boolean sourcePresent,boolean sourceCurrent,boolean routeValid,String gate,
                                                   String permission,boolean match,String dep,boolean superseded,boolean marker) {
        return new U06AdmissionEvidence(sourcePresent,sourceCurrent,routeValid,gate,permission,match,dep,superseded,marker);
    }

    private static U06ProfileBRequest req(String requestId,String mode,String source,int claimed,int authoritative,U06AdmissionEvidence evidence) {
        return new U06ProfileBRequest(requestId,"consult-1","cdp-1",mode,source,"synthetic-source-1",
                claimed,authoritative,U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING,"synthetic-binding-1","f3-policy-1",
                U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(mode)?"question-policy-1":null,
                U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(mode)?"d04-policy-1":null,
                "event-ref-1","business-event-1",
                U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(mode)?"thread-1":null,
                U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(mode)?"run-1":null,
                0L,"corr-1","trace-1",AT,evidence);
    }

    private static U06ProfileBRequest mode1(int version){return req("mode1-"+version,U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,U06ProfileBRequest.A1_PRE_READINESS_ROUTING,version,version,U06AdmissionEvidence.syntheticCurrentAllow());}
    private static U06ProfileBRequest mode2(int version){return req("mode2-"+version,U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,U06ProfileBRequest.U05_QUESTION_ROUTING,version,version,U06AdmissionEvidence.syntheticCurrentAllow());}
    private static U06SyntheticP01Runtime state(){return U06SyntheticP01TestFactory.create("synthetic-store-1","consult-1","cdp-1",CLOCK);}

    private static U06SyntheticDecisionInput input(String c03,String policy,List<U06SyntheticDecisionInput.Candidate> candidates) {
        return new U06SyntheticDecisionInput(c03,false,false,"gap-x","DECISION_MATERIAL",true,policy,candidates);
    }
    private static U06SyntheticDecisionInput.Candidate candidate(String id,String q,String semantic,int rank,boolean lawful) {
        return new U06SyntheticDecisionInput.Candidate(id,q,semantic,"synthetic-content-"+q,"fp-"+q,rank,lawful);
    }
    private static void seedQuestion(U06SyntheticP01Runtime state,String q,String semantic,String status) {
        Map<String,Object> value=new LinkedHashMap<String,Object>();
        value.put("question_id",q);value.put("question_semantic_key",semantic);value.put("status",status);
        state.commit("seed-"+q,"proposal-"+q,Collections.singletonList(state.upsert("/patient_state/questions/"+q,value)),
                Collections.singletonList("synthetic-seed"),"corr","trace",AT);
    }
    private static void caseAssert(String caseId,boolean condition){assertTrue(condition,caseId);}
}
