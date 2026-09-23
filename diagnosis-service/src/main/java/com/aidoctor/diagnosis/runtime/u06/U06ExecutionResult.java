package com.aidoctor.diagnosis.runtime.u06;
public final class U06ExecutionResult {
    public static final String ADMISSION_REJECTED="ADMISSION_REJECTED",MODE1_COMMITTED="MODE1_COMMITTED",MODE1_NO_MUTATION="MODE1_NO_MUTATION",
        MODE2_NO_SELECTION="MODE2_NO_SELECTION",WAIT_ESTABLISHED="WAIT_ESTABLISHED",RECONCILIATION_REQUIRED="RECONCILIATION_REQUIRED",
        FAILURE_REQUIRED="FAILURE_REQUIRED",REVALIDATED_CURRENT="REVALIDATED_CURRENT",REASSESSMENT_REQUIRED="REASSESSMENT_REQUIRED";
    private final String status,admissionId,effectRef,commitStatus,deliveryId,waitEffectId,checkpointId,u07ResumeEligibilityId,reasonCode;
    public U06ExecutionResult(String status,String admissionId,String effectRef,String commitStatus,String deliveryId,String waitEffectId,String checkpointId,String eligibility,String reasonCode){
        this.status=status;this.admissionId=admissionId;this.effectRef=effectRef;this.commitStatus=commitStatus;this.deliveryId=deliveryId;this.waitEffectId=waitEffectId;this.checkpointId=checkpointId;this.u07ResumeEligibilityId=eligibility;this.reasonCode=reasonCode;}
    public String getStatus(){return status;}public String getAdmissionId(){return admissionId;}public String getEffectRef(){return effectRef;}public String getCommitStatus(){return commitStatus;}
    public String getDeliveryId(){return deliveryId;}public String getWaitEffectId(){return waitEffectId;}public String getCheckpointId(){return checkpointId;}public String getU07ResumeEligibilityId(){return u07ResumeEligibilityId;}public String getReasonCode(){return reasonCode;}
}
