package com.aidoctor.diagnosis.runtime.u06;
public final class U06ProfileBRequest {
    public static final String PRE_READINESS_GAP_ASSESSMENT="PRE_READINESS_GAP_ASSESSMENT";
    public static final String QUESTION_SELECTION_DELIVERY="QUESTION_SELECTION_DELIVERY";
    public static final String F3_CURRENT_VERSION_REVALIDATION="F3_CURRENT_VERSION_REVALIDATION";
    public static final String A1_PRE_READINESS_ROUTING="A1_PRE_READINESS_ROUTING";
    public static final String F1_CLARIFICATION_ROUTING="F1_CLARIFICATION_ROUTING";
    public static final String U05_QUESTION_ROUTING="U05_QUESTION_ROUTING";
    public static final String POST_F3_SAFETY_BARRIER_ROUTING="POST_F3_SAFETY_BARRIER_ROUTING";
    public static final String CLINICAL_CONTINUATION_ROUTING="CLINICAL_CONTINUATION_ROUTING";
    public static final String F3_REASSESSMENT_ROUTING="F3_REASSESSMENT_ROUTING";
    public static final String SYNTHETIC_STRUCTURAL_NONPROD="SYNTHETIC_STRUCTURAL_NONPROD";
    public static final String SYNTHETIC_VERIFICATION_BINDING="SYNTHETIC_VERIFICATION_BINDING";
    private final String requestId,consultationId,cdpId,mode,sourceAuthorityType,sourceAuthorityRef,executionProfile,
            dependencyBindingType,dependencyBindingRef,f3OwnerPolicyRef,questionPolicyRef,d04PolicyRef,canonicalEventRef,
            businessEventIdentity,threadId,runId,correlationId,traceId,createdAt;
    private final int claimedClinicalStateVersion,authoritativeClinicalStateVersion;
    private final long expectedConsultationRowVersion;
    private final U06AdmissionEvidence admissionEvidence;
    public U06ProfileBRequest(String requestId,String consultationId,String cdpId,String mode,String sourceAuthorityType,
        String sourceAuthorityRef,int claimedClinicalStateVersion,int authoritativeClinicalStateVersion,String executionProfile,
        String dependencyBindingType,String dependencyBindingRef,String f3OwnerPolicyRef,String questionPolicyRef,String d04PolicyRef,
        String canonicalEventRef,String businessEventIdentity,String threadId,String runId,long expectedConsultationRowVersion,
        String correlationId,String traceId,String createdAt) {
        this.requestId=req(requestId,"requestId");this.consultationId=req(consultationId,"consultationId");this.cdpId=req(cdpId,"cdpId");
        this.mode=req(mode,"mode");this.sourceAuthorityType=req(sourceAuthorityType,"sourceAuthorityType");this.sourceAuthorityRef=req(sourceAuthorityRef,"sourceAuthorityRef");
        if(claimedClinicalStateVersion<0||authoritativeClinicalStateVersion<0)throw new IllegalArgumentException("state versions must be non-negative");
        this.claimedClinicalStateVersion=claimedClinicalStateVersion;this.authoritativeClinicalStateVersion=authoritativeClinicalStateVersion;
        this.executionProfile=req(executionProfile,"executionProfile");this.dependencyBindingType=req(dependencyBindingType,"dependencyBindingType");
        this.dependencyBindingRef=req(dependencyBindingRef,"dependencyBindingRef");this.f3OwnerPolicyRef=req(f3OwnerPolicyRef,"f3OwnerPolicyRef");
        this.questionPolicyRef=questionPolicyRef;this.d04PolicyRef=d04PolicyRef;this.canonicalEventRef=req(canonicalEventRef,"canonicalEventRef");
        this.businessEventIdentity=req(businessEventIdentity,"businessEventIdentity");this.threadId=threadId;this.runId=runId;
        if(expectedConsultationRowVersion<0)throw new IllegalArgumentException("expectedConsultationRowVersion must be non-negative");
        this.expectedConsultationRowVersion=expectedConsultationRowVersion;this.correlationId=req(correlationId,"correlationId");
        this.traceId=req(traceId,"traceId");this.createdAt=req(createdAt,"createdAt");
        this.admissionEvidence=U06AdmissionEvidence.syntheticCurrentAllow();
    }

    public U06ProfileBRequest(String requestId,String consultationId,String cdpId,String mode,String sourceAuthorityType,
        String sourceAuthorityRef,int claimedClinicalStateVersion,int authoritativeClinicalStateVersion,String executionProfile,
        String dependencyBindingType,String dependencyBindingRef,String f3OwnerPolicyRef,String questionPolicyRef,String d04PolicyRef,
        String canonicalEventRef,String businessEventIdentity,String threadId,String runId,long expectedConsultationRowVersion,
        String correlationId,String traceId,String createdAt,U06AdmissionEvidence admissionEvidence) {
        this.requestId=req(requestId,"requestId");this.consultationId=req(consultationId,"consultationId");this.cdpId=req(cdpId,"cdpId");
        this.mode=req(mode,"mode");this.sourceAuthorityType=req(sourceAuthorityType,"sourceAuthorityType");this.sourceAuthorityRef=req(sourceAuthorityRef,"sourceAuthorityRef");
        if(claimedClinicalStateVersion<0||authoritativeClinicalStateVersion<0)throw new IllegalArgumentException("state versions must be non-negative");
        this.claimedClinicalStateVersion=claimedClinicalStateVersion;this.authoritativeClinicalStateVersion=authoritativeClinicalStateVersion;
        this.executionProfile=req(executionProfile,"executionProfile");this.dependencyBindingType=req(dependencyBindingType,"dependencyBindingType");
        this.dependencyBindingRef=req(dependencyBindingRef,"dependencyBindingRef");this.f3OwnerPolicyRef=req(f3OwnerPolicyRef,"f3OwnerPolicyRef");
        this.questionPolicyRef=questionPolicyRef;this.d04PolicyRef=d04PolicyRef;this.canonicalEventRef=req(canonicalEventRef,"canonicalEventRef");
        this.businessEventIdentity=req(businessEventIdentity,"businessEventIdentity");this.threadId=threadId;this.runId=runId;
        if(expectedConsultationRowVersion<0)throw new IllegalArgumentException("expectedConsultationRowVersion must be non-negative");
        this.expectedConsultationRowVersion=expectedConsultationRowVersion;this.correlationId=req(correlationId,"correlationId");
        this.traceId=req(traceId,"traceId");this.createdAt=req(createdAt,"createdAt");
        if(admissionEvidence==null)throw new IllegalArgumentException("admissionEvidence is required");
        this.admissionEvidence=admissionEvidence;
    }
    public String getRequestId(){return requestId;} public String getConsultationId(){return consultationId;} public String getCdpId(){return cdpId;}
    public String getMode(){return mode;} public String getSourceAuthorityType(){return sourceAuthorityType;} public String getSourceAuthorityRef(){return sourceAuthorityRef;}
    public int getClaimedClinicalStateVersion(){return claimedClinicalStateVersion;} public int getAuthoritativeClinicalStateVersion(){return authoritativeClinicalStateVersion;}
    public String getExecutionProfile(){return executionProfile;} public String getDependencyBindingType(){return dependencyBindingType;}
    public String getDependencyBindingRef(){return dependencyBindingRef;} public String getF3OwnerPolicyRef(){return f3OwnerPolicyRef;}
    public String getQuestionPolicyRef(){return questionPolicyRef;} public String getD04PolicyRef(){return d04PolicyRef;}
    public String getCanonicalEventRef(){return canonicalEventRef;} public String getBusinessEventIdentity(){return businessEventIdentity;}
    public String getThreadId(){return threadId;} public String getRunId(){return runId;} public long getExpectedConsultationRowVersion(){return expectedConsultationRowVersion;}
    public String getCorrelationId(){return correlationId;} public String getTraceId(){return traceId;} public String getCreatedAt(){return createdAt;}
    public U06AdmissionEvidence getAdmissionEvidence(){return admissionEvidence;}
    private static String req(String v,String n){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException(n+" is required");return v.trim();}
}
