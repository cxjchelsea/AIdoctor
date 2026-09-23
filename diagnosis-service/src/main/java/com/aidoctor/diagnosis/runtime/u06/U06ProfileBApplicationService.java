package com.aidoctor.diagnosis.runtime.u06;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06SyntheticDeliveryService;
import com.aidoctor.diagnosis.runtime.u06.state.U06StateValues;
import com.aidoctor.diagnosis.runtime.u06.state.U06SyntheticP01Runtime;
import com.aidoctor.diagnosis.runtime.u06.trace.U06GovernedExecutionTraceStore;
import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitTransitionService;
import com.aidoctor.diagnosis.runtime.u06.wait.U06WaitCoordinator;
import java.time.OffsetDateTime;import java.util.ArrayList;import java.util.List;
public final class U06ProfileBApplicationService{
 private final U06AdmissionService admission;private final U06SyntheticP01Runtime state;private final U06SyntheticDeliveryService delivery;private final ConsultationWaitTransitionService consultationWait;private final U06WaitCoordinator runtimeWait;private final U06GovernedExecutionTraceStore trace;
 public U06ProfileBApplicationService(U06AdmissionService a,U06SyntheticP01Runtime s,U06SyntheticDeliveryService d,ConsultationWaitTransitionService c,U06WaitCoordinator r,U06GovernedExecutionTraceStore t){if(a==null||s==null||d==null||c==null||r==null||t==null)throw new IllegalArgumentException("all services required");admission=a;state=s;delivery=d;consultationWait=c;runtimeWait=r;trace=t;}
 public U06ExecutionResult execute(U06ProfileBRequest r,U06SyntheticDecisionBundle d,U06SyntheticDeliveryService.ScopeAuthorization scope){
  U06AdmissionService.Admission a=admission.admit(r);if(!a.isAdmitted())return new U06ExecutionResult(U06ExecutionResult.ADMISSION_REJECTED,a.getAdmissionId(),null,null,null,null,null,null,a.getReasonCode());
  String tr=U06Ids.hash("u06trace",r.getConsultationId(),a.getAdmissionId(),r.getMode(),String.valueOf(r.getAuthoritativeClinicalStateVersion()),r.getExecutionProfile(),"1");
  trace.start(tr,r.getConsultationId(),a.getAdmissionId(),r.getMode(),r.getExecutionProfile(),a.getFingerprint(),r.getCreatedAt());
  U06ExecutionResult result;
  if(U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT.equals(r.getMode()))result=mode1(r,a,d);
  else if(U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(r.getMode()))result=mode2(r,a,d,scope);
  else result=mode3(a,d);
  trace.complete(tr,U06ExecutionResult.WAIT_ESTABLISHED.equals(result.getStatus())?"MUTATION_RECONCILED":"NO_MUTATION_TERMINAL",result.getStatus(),r.getCreatedAt());return result;
 }
 private U06ExecutionResult mode1(U06ProfileBRequest r,U06AdmissionService.Admission a,U06SyntheticDecisionBundle d){
  if(!U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED.equals(d.getF3OwnerStatus())&&!U06SyntheticDecisionBundle.NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED.equals(d.getF3OwnerStatus()))
   return new U06ExecutionResult(U06ExecutionResult.MODE1_NO_MUTATION,a.getAdmissionId(),null,null,null,null,null,null,d.getF3OwnerStatus());
  String effect=d.getF3CanonicalEffectId()!=null?d.getF3CanonicalEffectId():U06Ids.hash("u06f3",r.getConsultationId(),r.getDependencyBindingType(),r.getDependencyBindingRef(),String.valueOf(r.getAuthoritativeClinicalStateVersion()),r.getF3OwnerPolicyRef(),r.getBusinessEventIdentity());
  List<U06SyntheticP01Runtime.OperationIntent>ops=new ArrayList<U06SyntheticP01Runtime.OperationIntent>();ops.add(state.upsert("/patient_state/f3_gap_assessment",U06StateValues.f3Assessment(r,d)));
  if(U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED.equals(d.getF3OwnerStatus()))ops.add(state.upsert("/patient_state/information_gaps/"+d.getGapId(),U06StateValues.gap(r,d,"QUESTIONABLE_ONLINE")));
  U06SyntheticP01Runtime.CommitEvidence ce=state.commit(effect,U06Ids.hash("u06proposal",effect),ops,refs(r.getSourceAuthorityRef()),r.getCorrelationId(),r.getTraceId(),r.getCreatedAt());
  return new U06ExecutionResult(U06ExecutionResult.MODE1_COMMITTED,a.getAdmissionId(),effect,ce.getResult().status,null,null,null,null,ce.getResult().reasonCode);
 }
 private U06ExecutionResult mode2(U06ProfileBRequest r,U06AdmissionService.Admission a,U06SyntheticDecisionBundle d,U06SyntheticDeliveryService.ScopeAuthorization scope){
  if(!U06SyntheticDecisionBundle.SELECTED.equals(d.getQuestionSelectionStatus()))return new U06ExecutionResult(U06ExecutionResult.MODE2_NO_SELECTION,a.getAdmissionId(),null,null,null,null,null,null,d.getQuestionSelectionStatus());
  String selection=d.getQuestionSelectionEffectId();List<U06SyntheticP01Runtime.OperationIntent>sel=new ArrayList<U06SyntheticP01Runtime.OperationIntent>();
  sel.add(state.upsert("/patient_state/questions/"+d.getQuestionId(),U06StateValues.questionSelected(r,d)));
  U06SyntheticP01Runtime.CommitEvidence sc=state.commit(selection,U06Ids.hash("u06proposal",selection),sel,refs(r.getSourceAuthorityRef()),r.getCorrelationId(),r.getTraceId(),r.getCreatedAt());
  if(!"COMMITTED".equals(sc.getResult().status))return new U06ExecutionResult(U06ExecutionResult.FAILURE_REQUIRED,a.getAdmissionId(),selection,sc.getResult().status,null,null,null,null,sc.getResult().reasonCode);
  if(scope==null||!state.getStoreRef().equals(scope.getStateStoreRef()))throw new IllegalStateException("U06_SYNTHETIC_DELIVERY_SCOPE_STORE_MISMATCH");
  U06SyntheticDeliveryService.Confirmation conf=delivery.confirm(r.getConsultationId(),selection,d.getQuestionId(),d.getQuestionContentFingerprint(),scope,r.getCreatedAt());
  String parent=U06Ids.hash("u06wait",r.getConsultationId(),d.getQuestionId(),selection,conf.getDeliveryId(),d.getQuestionContentFingerprint(),"1"),clinical=U06Ids.hash("u06waitstate",parent,"1");
  List<U06SyntheticP01Runtime.OperationIntent>ops=new ArrayList<U06SyntheticP01Runtime.OperationIntent>();ops.add(state.upsert("/patient_state/questions/"+d.getQuestionId(),U06StateValues.questionDelivered(r,d,conf.getDeliveryEffectId(),conf.getDeliveryId())));
  if(d.getGapId()!=null&&state.readCurrent().exists("/patient_state/information_gaps/"+d.getGapId()))ops.add(state.upsert("/patient_state/information_gaps/"+d.getGapId(),U06StateValues.gapAsked(r,d)));
  if(state.readCurrent().exists("/patient_state/pending_question"))throw new IllegalStateException("U06_PENDING_QUESTION_CONFLICT");
  ops.add(state.upsert("/patient_state/pending_question",U06StateValues.pending(d,parent,conf.getDeliveryId())));
  U06SyntheticP01Runtime.CommitEvidence dc=state.commit(clinical,U06Ids.hash("u06proposal",clinical),ops,refs(conf.getConfirmationEvaluationId()),r.getCorrelationId(),r.getTraceId(),r.getCreatedAt());
  if(!"COMMITTED".equals(dc.getResult().status))return new U06ExecutionResult(U06ExecutionResult.RECONCILIATION_REQUIRED,a.getAdmissionId(),parent,dc.getResult().status,conf.getDeliveryId(),null,null,null,dc.getResult().reasonCode);
  String waitEffect=U06Ids.hash("u06consultwait",parent,"1"),waitFp=U06Ids.hash("u06consultwaitfp",r.getConsultationId(),waitEffect,parent,d.getQuestionId(),conf.getDeliveryId(),String.valueOf(r.getExpectedConsultationRowVersion()),"WAITING_USER");
  ConsultationWaitTransitionService.Result wr=consultationWait.establish(new ConsultationWaitTransitionService.Command(waitEffect,parent,r.getConsultationId(),d.getQuestionId(),conf.getDeliveryId(),waitFp,U06Ids.hash("u06consultwaitidem",waitEffect,"1"),r.getExpectedConsultationRowVersion(),OffsetDateTime.parse(r.getCreatedAt()).toLocalDateTime()));
  String cp=U06Ids.hash("u06checkpoint",parent,"1"),cpFp=U06Ids.hash("u06checkpointfp",cp,r.getThreadId(),r.getRunId(),d.getQuestionId(),conf.getDeliveryId(),String.valueOf(dc.getReadBack().getVersion()),waitEffect);
  U06WaitCoordinator.Result rr=runtimeWait.establish(new U06WaitCoordinator.Command(cp,r.getConsultationId(),required(r.getThreadId(),"threadId"),required(r.getRunId(),"runId"),d.getQuestionId(),"/patient_state/pending_question",selection,conf.getDeliveryEffectId(),parent,conf.getDeliveryId(),conf.getConfirmationEvaluationId(),dc.getReadBack().getVersion(),wr.waitEffectId,r.getDependencyBindingRef(),r.getQuestionPolicyRef(),cpFp,r.getCreatedAt()));
  return new U06ExecutionResult(U06ExecutionResult.WAIT_ESTABLISHED,a.getAdmissionId(),parent,dc.getResult().status,conf.getDeliveryId(),wr.waitEffectId,rr.checkpointId,rr.eligibilityId,null);
 }
 private U06ExecutionResult mode3(U06AdmissionService.Admission a,U06SyntheticDecisionBundle d){String s=d.getRevalidationStatus();if(U06SyntheticDecisionBundle.REVALIDATED_CURRENT.equals(s))return new U06ExecutionResult(U06ExecutionResult.REVALIDATED_CURRENT,a.getAdmissionId(),d.getRevalidationRef(),null,null,null,null,null,null);if(U06SyntheticDecisionBundle.REASSESSMENT_REQUIRED.equals(s))return new U06ExecutionResult(U06ExecutionResult.REASSESSMENT_REQUIRED,a.getAdmissionId(),d.getRevalidationRef(),null,null,null,null,null,null);return new U06ExecutionResult(U06ExecutionResult.FAILURE_REQUIRED,a.getAdmissionId(),d.getRevalidationRef(),null,null,null,null,null,s);}
 private static List<String>refs(String r){List<String>x=new ArrayList<String>();x.add(r);return x;}private static String required(String v,String n){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException(n+" required");return v.trim();}
}
