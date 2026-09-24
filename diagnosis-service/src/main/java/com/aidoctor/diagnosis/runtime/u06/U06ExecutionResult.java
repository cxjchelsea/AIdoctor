package com.aidoctor.diagnosis.runtime.u06;

public final class U06ExecutionResult {
    public static final String ADMISSION_REJECTED="ADMISSION_REJECTED";
    public static final String MODE1_COMMITTED="MODE1_COMMITTED";
    public static final String MODE1_NO_MUTATION="MODE1_NO_MUTATION";
    public static final String MODE1_SAFETY_BLOCKED="MODE1_SAFETY_BLOCKED";
    public static final String MODE2_NO_SELECTION="MODE2_NO_SELECTION";
    public static final String WAIT_ESTABLISHED="WAIT_ESTABLISHED";
    public static final String RECONCILIATION_REQUIRED="RECONCILIATION_REQUIRED";
    public static final String FAILURE_REQUIRED="FAILURE_REQUIRED";
    public static final String REVALIDATED_CURRENT="REVALIDATED_CURRENT";
    public static final String REASSESSMENT_REQUIRED="REASSESSMENT_REQUIRED";

    private final String status;
    private final String admissionId;
    private final String effectRef;
    private final String commitStatus;
    private final String deliveryId;
    private final String waitEffectId;
    private final String checkpointId;
    private final String u07ResumeEligibilityId;
    private final String reasonCode;
    private final String safetyEvaluationId;

    public U06ExecutionResult(String status,String admissionId,String effectRef,String commitStatus,
                              String deliveryId,String waitEffectId,String checkpointId,String eligibility,
                              String reasonCode) {
        this(status,admissionId,effectRef,commitStatus,deliveryId,waitEffectId,checkpointId,
                eligibility,reasonCode,null);
    }

    public U06ExecutionResult(String status,String admissionId,String effectRef,String commitStatus,
                              String deliveryId,String waitEffectId,String checkpointId,String eligibility,
                              String reasonCode,String safetyEvaluationId) {
        this.status=status;
        this.admissionId=admissionId;
        this.effectRef=effectRef;
        this.commitStatus=commitStatus;
        this.deliveryId=deliveryId;
        this.waitEffectId=waitEffectId;
        this.checkpointId=checkpointId;
        this.u07ResumeEligibilityId=eligibility;
        this.reasonCode=reasonCode;
        this.safetyEvaluationId=safetyEvaluationId;
    }

    public String getStatus(){return status;}
    public String getAdmissionId(){return admissionId;}
    public String getEffectRef(){return effectRef;}
    public String getCommitStatus(){return commitStatus;}
    public String getDeliveryId(){return deliveryId;}
    public String getWaitEffectId(){return waitEffectId;}
    public String getCheckpointId(){return checkpointId;}
    public String getU07ResumeEligibilityId(){return u07ResumeEligibilityId;}
    public String getReasonCode(){return reasonCode;}
    public String getSafetyEvaluationId(){return safetyEvaluationId;}
}
