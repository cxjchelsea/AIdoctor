package com.aidoctor.diagnosis.runtime.u06;

import com.aidoctor.diagnosis.runtime.u06.delivery.U06SyntheticDeliveryService;
import com.aidoctor.diagnosis.runtime.u06.state.U06StateValues;
import com.aidoctor.diagnosis.runtime.u06.state.U06SyntheticP01Runtime;
import com.aidoctor.diagnosis.runtime.u06.trace.U06GovernedExecutionTraceStore;
import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitTransitionService;
import com.aidoctor.diagnosis.runtime.u06.wait.U06WaitCoordinator;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public final class U06ProfileBApplicationService {
    private final U06AdmissionService admission;
    private final U06SyntheticP01Runtime state;
    private final U06SyntheticDeliveryService delivery;
    private final ConsultationWaitTransitionService consultationWait;
    private final U06WaitCoordinator runtimeWait;
    private final U06GovernedExecutionTraceStore trace;
    private final U06SyntheticPostF3SafetyBarrier safetyBarrier;
    private final U06SyntheticRevalidationAuthority revalidation;

    public U06ProfileBApplicationService(U06AdmissionService a,U06SyntheticP01Runtime s,
            U06SyntheticDeliveryService d,ConsultationWaitTransitionService c,U06WaitCoordinator r,
            U06GovernedExecutionTraceStore t) {
        this(a,s,d,c,r,t,new U06SyntheticPostF3SafetyBarrier(),new U06SyntheticRevalidationAuthority());
    }

    public U06ProfileBApplicationService(U06AdmissionService a,U06SyntheticP01Runtime s,
            U06SyntheticDeliveryService d,ConsultationWaitTransitionService c,U06WaitCoordinator r,
            U06GovernedExecutionTraceStore t,U06SyntheticPostF3SafetyBarrier safety,
            U06SyntheticRevalidationAuthority revalidation) {
        if(a==null||s==null||d==null||c==null||r==null||t==null||safety==null||revalidation==null)
            throw new IllegalArgumentException("all services required");
        admission=a;state=s;delivery=d;consultationWait=c;runtimeWait=r;trace=t;
        safetyBarrier=safety;this.revalidation=revalidation;
    }

    public U06ExecutionResult execute(U06ProfileBRequest r,U06SyntheticDecisionBundle d,
                                      U06SyntheticDeliveryService.ScopeAuthorization scope) {
        return execute(r,d,scope,null);
    }

    public U06ExecutionResult execute(U06ProfileBRequest r,U06SyntheticDecisionBundle d,
                                      U06SyntheticDeliveryService.ScopeAuthorization scope,
                                      U06SyntheticPostF3SafetyBarrier.Evidence safetyEvidence) {
        U06AdmissionService.Admission a=admission.admit(r,state.readCurrent().getVersion());
        if(!a.isAdmitted())
            return new U06ExecutionResult(U06ExecutionResult.ADMISSION_REJECTED,a.getAdmissionId(),
                    null,null,null,null,null,null,a.getReasonCode());

        U06ExecutionResult preflight=preflight(r,d,scope,a);
        if(preflight!=null)return preflight;

        String tr=U06Ids.hash("u06trace",r.getConsultationId(),a.getAdmissionId(),r.getMode(),
                String.valueOf(r.getAuthoritativeClinicalStateVersion()),r.getExecutionProfile(),"1");
        trace.start(tr,r.getConsultationId(),a.getAdmissionId(),r.getMode(),r.getExecutionProfile(),
                a.getFingerprint(),r.getCreatedAt());

        U06ExecutionResult result;
        if(U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT.equals(r.getMode()))
            result=mode1(r,a,d,safetyEvidence);
        else if(U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(r.getMode()))
            result=mode2(r,a,d,scope);
        else
            result=mode3(r,a,d);

        trace.complete(tr,traceLifecycle(result),result.getStatus(),r.getCreatedAt());
        return result;
    }

    private U06ExecutionResult preflight(U06ProfileBRequest r,U06SyntheticDecisionBundle d,
                                         U06SyntheticDeliveryService.ScopeAuthorization scope,
                                         U06AdmissionService.Admission a) {
        if(d==null)
            return rejected(a,"U06_DECISION_BUNDLE_REQUIRED");

        if(U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(r.getMode())
                && U06SyntheticDecisionBundle.SELECTED.equals(d.getQuestionSelectionStatus())) {
            if(blank(r.getThreadId())||blank(r.getRunId()))
                return rejected(a,"U06_RUNTIME_WAIT_INPUT_REQUIRED");
            if(scope==null||!scope.isValidFor(r.getConsultationId())
                    ||!state.getStoreRef().equals(scope.getStateStoreRef()))
                return rejected(a,"U06_SYNTHETIC_DELIVERY_SCOPE_INVALID");
        }
        return null;
    }

    private U06ExecutionResult rejected(U06AdmissionService.Admission a,String reason) {
        return new U06ExecutionResult(U06ExecutionResult.ADMISSION_REJECTED,a.getAdmissionId(),
                null,null,null,null,null,null,reason);
    }

    private U06ExecutionResult mode1(U06ProfileBRequest r,U06AdmissionService.Admission a,
                                     U06SyntheticDecisionBundle d,
                                     U06SyntheticPostF3SafetyBarrier.Evidence safetyEvidence) {
        if(!U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED.equals(d.getF3OwnerStatus())
                &&!U06SyntheticDecisionBundle.NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED.equals(d.getF3OwnerStatus()))
            return new U06ExecutionResult(U06ExecutionResult.MODE1_NO_MUTATION,a.getAdmissionId(),
                    null,null,null,null,null,null,d.getF3OwnerStatus());

        String effect=d.getF3CanonicalEffectId()!=null?d.getF3CanonicalEffectId():
                U06Ids.hash("u06f3",r.getConsultationId(),r.getDependencyBindingType(),
                        r.getDependencyBindingRef(),String.valueOf(r.getAuthoritativeClinicalStateVersion()),
                        r.getF3OwnerPolicyRef(),r.getBusinessEventIdentity());

        List<U06SyntheticP01Runtime.OperationIntent>ops=new ArrayList<U06SyntheticP01Runtime.OperationIntent>();
        ops.add(state.upsert("/patient_state/f3_gap_assessment",U06StateValues.f3Assessment(r,d,effect)));
        if(U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED.equals(d.getF3OwnerStatus()))
            ops.add(state.upsert("/patient_state/information_gaps/"+d.getGapId(),
                    U06StateValues.gap(r,d,"QUESTIONABLE_ONLINE")));

        U06SyntheticP01Runtime.CommitEvidence ce=state.commit(effect,U06Ids.hash("u06proposal",effect),
                ops,refs(r.getSourceAuthorityRef()),r.getCorrelationId(),r.getTraceId(),r.getCreatedAt());

        if(!"COMMITTED".equals(ce.getResult().status))
            return new U06ExecutionResult(U06ExecutionResult.FAILURE_REQUIRED,a.getAdmissionId(),effect,
                    ce.getResult().status,null,null,null,null,ce.getResult().reasonCode);

        String readBackEffect=ce.getReadBack().mapString("/patient_state/f3_gap_assessment","f3_canonical_effect_id");
        if(!effect.equals(readBackEffect))
            return new U06ExecutionResult(U06ExecutionResult.FAILURE_REQUIRED,a.getAdmissionId(),effect,
                    ce.getResult().status,null,null,null,null,"U06_AUTHORITATIVE_READBACK_MISMATCH");

        U06SyntheticPostF3SafetyBarrier.Evaluation safety=safetyBarrier.evaluate(
                r.getConsultationId(),effect,ce.getResult().status,ce.getReadBack().getVersion(),safetyEvidence);

        if(U06SyntheticPostF3SafetyBarrier.BLOCKED.equals(safety.getStatus()))
            return new U06ExecutionResult(U06ExecutionResult.MODE1_SAFETY_BLOCKED,a.getAdmissionId(),effect,
                    ce.getResult().status,null,null,null,null,"POST_F3_SAFETY_BLOCKED",safety.getEvaluationId());

        if(U06SyntheticPostF3SafetyBarrier.UNAVAILABLE.equals(safety.getStatus()))
            return new U06ExecutionResult(U06ExecutionResult.FAILURE_REQUIRED,a.getAdmissionId(),effect,
                    ce.getResult().status,null,null,null,null,"POST_F3_SAFETY_UNAVAILABLE",safety.getEvaluationId());

        return new U06ExecutionResult(U06ExecutionResult.MODE1_COMMITTED,a.getAdmissionId(),effect,
                ce.getResult().status,null,null,null,null,null,safety.getEvaluationId());
    }

    private U06ExecutionResult mode2(U06ProfileBRequest r,U06AdmissionService.Admission a,
                                     U06SyntheticDecisionBundle d,
                                     U06SyntheticDeliveryService.ScopeAuthorization scope) {
        if(!U06SyntheticDecisionBundle.SELECTED.equals(d.getQuestionSelectionStatus()))
            return new U06ExecutionResult(U06ExecutionResult.MODE2_NO_SELECTION,a.getAdmissionId(),
                    null,null,null,null,null,null,d.getQuestionSelectionStatus());

        String selection=d.getQuestionSelectionEffectId();
        List<U06SyntheticP01Runtime.OperationIntent>sel=new ArrayList<U06SyntheticP01Runtime.OperationIntent>();
        sel.add(state.upsert("/patient_state/questions/"+d.getQuestionId(),U06StateValues.questionSelected(r,d)));

        U06SyntheticP01Runtime.CommitEvidence sc=state.commit(selection,U06Ids.hash("u06proposal",selection),
                sel,refs(r.getSourceAuthorityRef()),r.getCorrelationId(),r.getTraceId(),r.getCreatedAt());

        if(!"COMMITTED".equals(sc.getResult().status))
            return new U06ExecutionResult(U06ExecutionResult.FAILURE_REQUIRED,a.getAdmissionId(),selection,
                    sc.getResult().status,null,null,null,null,sc.getResult().reasonCode);

        U06SyntheticDeliveryService.Confirmation conf=delivery.confirm(r.getConsultationId(),selection,
                d.getQuestionId(),d.getQuestionContentFingerprint(),scope,r.getCreatedAt());

        String parent=U06Ids.hash("u06wait",r.getConsultationId(),d.getQuestionId(),selection,
                conf.getDeliveryId(),d.getQuestionContentFingerprint(),"1");
        String clinical=U06Ids.hash("u06waitstate",parent,"1");

        List<U06SyntheticP01Runtime.OperationIntent>ops=new ArrayList<U06SyntheticP01Runtime.OperationIntent>();
        ops.add(state.upsert("/patient_state/questions/"+d.getQuestionId(),
                U06StateValues.questionDelivered(r,d,conf.getDeliveryEffectId(),conf.getDeliveryId())));

        if(d.getGapId()!=null&&state.readCurrent().exists("/patient_state/information_gaps/"+d.getGapId()))
            ops.add(state.upsert("/patient_state/information_gaps/"+d.getGapId(),U06StateValues.gapAsked(r,d)));

        U06SyntheticP01Runtime.StateView pendingView=state.readCurrent();
        if(pendingView.exists("/patient_state/pending_question")) {
            String existingQuestion=pendingView.mapString("/patient_state/pending_question","question_id");
            String existingParent=pendingView.mapString("/patient_state/pending_question","question_delivered_wait_effect_id");
            String existingDelivery=pendingView.mapString("/patient_state/pending_question","delivery_id");
            if(!d.getQuestionId().equals(existingQuestion)
                    ||!parent.equals(existingParent)
                    ||!conf.getDeliveryId().equals(existingDelivery))
                throw new IllegalStateException("U06_PENDING_QUESTION_CONFLICT");
        }

        ops.add(state.upsert("/patient_state/pending_question",
                U06StateValues.pending(d,parent,conf.getDeliveryId())));

        U06SyntheticP01Runtime.CommitEvidence dc=state.commit(clinical,U06Ids.hash("u06proposal",clinical),
                ops,refs(conf.getConfirmationEvaluationId()),r.getCorrelationId(),r.getTraceId(),r.getCreatedAt());

        if(!"COMMITTED".equals(dc.getResult().status))
            return new U06ExecutionResult(U06ExecutionResult.RECONCILIATION_REQUIRED,a.getAdmissionId(),parent,
                    dc.getResult().status,conf.getDeliveryId(),null,null,null,dc.getResult().reasonCode);

        String waitEffect=U06Ids.hash("u06consultwait",parent,"1");
        String waitFp=U06Ids.hash("u06consultwaitfp",r.getConsultationId(),waitEffect,parent,d.getQuestionId(),
                conf.getDeliveryId(),String.valueOf(r.getExpectedConsultationRowVersion()),"WAITING_USER");

        ConsultationWaitTransitionService.Result wr=consultationWait.establish(
                new ConsultationWaitTransitionService.Command(waitEffect,parent,r.getConsultationId(),
                        d.getQuestionId(),conf.getDeliveryId(),waitFp,
                        U06Ids.hash("u06consultwaitidem",waitEffect,"1"),
                        r.getExpectedConsultationRowVersion(),
                        OffsetDateTime.parse(r.getCreatedAt()).toLocalDateTime()));

        String cp=U06Ids.hash("u06checkpoint",parent,"1");
        String cpFp=U06Ids.hash("u06checkpointfp",cp,r.getThreadId(),r.getRunId(),d.getQuestionId(),
                conf.getDeliveryId(),String.valueOf(dc.getReadBack().getVersion()),waitEffect);

        U06WaitCoordinator.Result rr=runtimeWait.establish(new U06WaitCoordinator.Command(cp,
                r.getConsultationId(),r.getThreadId(),r.getRunId(),d.getQuestionId(),
                "/patient_state/pending_question",selection,conf.getDeliveryEffectId(),parent,
                conf.getDeliveryId(),conf.getConfirmationEvaluationId(),dc.getReadBack().getVersion(),
                wr.waitEffectId,r.getDependencyBindingRef(),r.getQuestionPolicyRef(),cpFp,r.getCreatedAt()));

        return new U06ExecutionResult(U06ExecutionResult.WAIT_ESTABLISHED,a.getAdmissionId(),parent,
                dc.getResult().status,conf.getDeliveryId(),wr.waitEffectId,rr.checkpointId,rr.eligibilityId,null);
    }

    private U06ExecutionResult mode3(U06ProfileBRequest r,U06AdmissionService.Admission a,
                                     U06SyntheticDecisionBundle d) {
        U06SyntheticP01Runtime.StateView current=state.readCurrent();
        String currentF3Effect=current.mapString("/patient_state/f3_gap_assessment","f3_canonical_effect_id");
        U06SyntheticRevalidationAuthority.Result x=revalidation.evaluate(
                r,d,current.getVersion(),currentF3Effect);

        if(x.getFailureCode()!=null)
            return new U06ExecutionResult(U06ExecutionResult.FAILURE_REQUIRED,a.getAdmissionId(),
                    x.getRevalidationId(),null,null,null,null,null,x.getFailureCode());

        if(U06SyntheticDecisionBundle.REVALIDATED_CURRENT.equals(x.getStatus()))
            return new U06ExecutionResult(U06ExecutionResult.REVALIDATED_CURRENT,a.getAdmissionId(),
                    x.getRevalidationId(),null,null,null,null,null,null);

        if(U06SyntheticDecisionBundle.REASSESSMENT_REQUIRED.equals(x.getStatus()))
            return new U06ExecutionResult(U06ExecutionResult.REASSESSMENT_REQUIRED,a.getAdmissionId(),
                    x.getRevalidationId(),null,null,null,null,null,null);

        return new U06ExecutionResult(U06ExecutionResult.FAILURE_REQUIRED,a.getAdmissionId(),
                x.getRevalidationId(),null,null,null,null,null,x.getStatus());
    }

    private static String traceLifecycle(U06ExecutionResult result) {
        if(U06ExecutionResult.WAIT_ESTABLISHED.equals(result.getStatus())
                ||U06ExecutionResult.MODE1_COMMITTED.equals(result.getStatus())
                ||U06ExecutionResult.MODE1_SAFETY_BLOCKED.equals(result.getStatus())
                ||U06ExecutionResult.RECONCILIATION_REQUIRED.equals(result.getStatus()))
            return "MUTATION_RECONCILED";

        if(U06ExecutionResult.FAILURE_REQUIRED.equals(result.getStatus()))
            return "FAILED";

        return "NO_MUTATION_TERMINAL";
    }

    private static List<String>refs(String r){List<String>x=new ArrayList<String>();x.add(r);return x;}
    private static boolean blank(String v){return v==null||v.trim().isEmpty();}
}
