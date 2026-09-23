package com.aidoctor.diagnosis.runtime.u06;

import java.util.LinkedHashMap;
import java.util.Map;

public final class U06AdmissionService {
    public static final String ADMITTED="ADMITTED";
    public static final String REJECTED_MODE_SOURCE_MISMATCH="REJECTED_MODE_SOURCE_MISMATCH";
    public static final String REJECTED_PROFILE="REJECTED_PROFILE";
    public static final String REJECTED_STALE_STATE="REJECTED_STALE_STATE";
    public static final String REJECTED_BINDING="REJECTED_BINDING";
    public static final String REJECTED_POLICY="REJECTED_POLICY";
    public static final String REJECTED_F1_DISABLED="REJECTED_F1_DISABLED";

    private final Map<String,String> admittedFingerprints=new LinkedHashMap<String,String>();

    public synchronized Admission admit(U06ProfileBRequest r) {
        return admit(r,r.getAuthoritativeClinicalStateVersion());
    }

    public synchronized Admission admit(U06ProfileBRequest r,int actualCurrentStateVersion) {
        String rejection=validate(r,actualCurrentStateVersion);

        String id=U06Ids.hash("u06adm",
                r.getConsultationId(),
                r.getCdpId(),
                r.getMode(),
                r.getSourceAuthorityType(),
                r.getSourceAuthorityRef(),
                String.valueOf(r.getAuthoritativeClinicalStateVersion()),
                r.getCanonicalEventRef(),
                r.getBusinessEventIdentity(),
                r.getExecutionProfile(),
                r.getDependencyBindingType(),
                r.getDependencyBindingRef(),
                r.getF3OwnerPolicyRef(),
                r.getQuestionPolicyRef(),
                r.getD04PolicyRef(),
                "1");

        String fp=U06Ids.hash("u06admf",
                r.getConsultationId(),
                r.getCdpId(),
                r.getMode(),
                r.getSourceAuthorityType(),
                r.getSourceAuthorityRef(),
                String.valueOf(r.getClaimedClinicalStateVersion()),
                String.valueOf(actualCurrentStateVersion),
                r.getExecutionProfile(),
                r.getDependencyBindingType(),
                r.getDependencyBindingRef(),
                r.getF3OwnerPolicyRef(),
                r.getQuestionPolicyRef(),
                r.getD04PolicyRef(),
                r.getCanonicalEventRef(),
                r.getBusinessEventIdentity(),
                "1");

        if(rejection!=null)return Admission.rejected(id,rejection,fp,r);

        String existing=admittedFingerprints.get(id);
        if(existing!=null&&!existing.equals(fp))
            throw new IllegalStateException("U06_ADMISSION_REPLAY_CONFLICT");

        boolean replay=existing!=null;
        if(!replay)admittedFingerprints.put(id,fp);
        return Admission.admitted(id,fp,r,replay);
    }

    private String validate(U06ProfileBRequest r,int actualCurrentStateVersion) {
        if(!U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD.equals(r.getExecutionProfile()))
            return REJECTED_PROFILE;
        if(!U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING.equals(r.getDependencyBindingType()))
            return REJECTED_BINDING;
        if(r.getClaimedClinicalStateVersion()!=r.getAuthoritativeClinicalStateVersion()
                || actualCurrentStateVersion!=r.getAuthoritativeClinicalStateVersion())
            return REJECTED_STALE_STATE;
        if(U06ProfileBRequest.F1_CLARIFICATION_ROUTING.equals(r.getSourceAuthorityType()))
            return REJECTED_F1_DISABLED;
        if(!legal(r.getMode(),r.getSourceAuthorityType()))
            return REJECTED_MODE_SOURCE_MISMATCH;
        if(U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(r.getMode())
                &&(blank(r.getQuestionPolicyRef())||blank(r.getD04PolicyRef())))
            return REJECTED_POLICY;
        return null;
    }

    private boolean legal(String mode,String source) {
        if(U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT.equals(mode))
            return U06ProfileBRequest.A1_PRE_READINESS_ROUTING.equals(source)
                    ||U06ProfileBRequest.F3_REASSESSMENT_ROUTING.equals(source);
        if(U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(mode))
            return U06ProfileBRequest.U05_QUESTION_ROUTING.equals(source);
        if(U06ProfileBRequest.F3_CURRENT_VERSION_REVALIDATION.equals(mode))
            return U06ProfileBRequest.POST_F3_SAFETY_BARRIER_ROUTING.equals(source)
                    ||U06ProfileBRequest.CLINICAL_CONTINUATION_ROUTING.equals(source);
        return false;
    }

    private static boolean blank(String v){return v==null||v.trim().isEmpty();}

    public static final class Admission {
        private final String status,admissionId,reasonCode,fingerprint;
        private final U06ProfileBRequest request;
        private final boolean replay;

        private Admission(String s,String id,String reason,String fp,U06ProfileBRequest r,boolean replay) {
            status=s;admissionId=id;reasonCode=reason;fingerprint=fp;request=r;this.replay=replay;
        }

        static Admission admitted(String id,String fp,U06ProfileBRequest r,boolean replay) {
            return new Admission(ADMITTED,id,null,fp,r,replay);
        }

        static Admission rejected(String id,String reason,String fp,U06ProfileBRequest r) {
            return new Admission(reason,id,reason,fp,r,false);
        }

        public boolean isAdmitted(){return ADMITTED.equals(status);}
        public String getStatus(){return status;}
        public String getAdmissionId(){return admissionId;}
        public String getReasonCode(){return reasonCode;}
        public String getFingerprint(){return fingerprint;}
        public U06ProfileBRequest getRequest(){return request;}
        public boolean isReplay(){return replay;}
    }
}
