package com.aidoctor.diagnosis.runtime.u06;

import com.aidoctor.diagnosis.runtime.u06.state.U06SyntheticP01Runtime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class U06SyntheticDecisionEngine {
    public U06SyntheticDecisionBundle decide(U06ProfileBRequest request,U06SyntheticDecisionInput input,
                                              U06SyntheticP01Runtime.StateView current) {
        if(request==null||input==null||current==null)throw new IllegalArgumentException("decision inputs required");
        if(U06ProfileBRequest.F3_CURRENT_VERSION_REVALIDATION.equals(request.getMode()))
            throw new IllegalArgumentException("MODE-3 uses revalidation authority, not C03/D04 decision engine");

        String c03=input.getC03BusinessStatus();
        if(U06SyntheticDecisionInput.NO_RESULT.equals(c03))
            return noDecision(request,"C03_NO_RESULT");
        if(U06SyntheticDecisionInput.INSUFFICIENT_INFORMATION.equals(c03))
            return noDecision(request,"C03_INSUFFICIENT_FOR_SELECTION");
        if(U06SyntheticDecisionInput.DEPENDENCY_FAILURE.equals(c03)
                ||U06SyntheticDecisionInput.TIMEOUT.equals(c03)
                ||U06SyntheticDecisionInput.INVALID_OUTPUT.equals(c03))
            return failed(request,c03);
        if(!U06SyntheticDecisionInput.SUCCESS.equals(c03))
            return failed(request,"C03_STATUS_UNKNOWN");

        if(U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT.equals(request.getMode()))
            return decideMode1(request,input);
        if(U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(request.getMode()))
            return decideMode2(request,input,current);
        return failed(request,"MODE_UNSUPPORTED");
    }

    private U06SyntheticDecisionBundle decideMode1(U06ProfileBRequest r,U06SyntheticDecisionInput in) {
        if(in.hasExplicitGapBasis()) {
            String gap=req(in.getGapId(),"gapId");
            String impact=req(in.getGapDecisionImpact(),"gapDecisionImpact");
            String effect=U06Ids.hash("u06f3",r.getConsultationId(),r.getDependencyBindingType(),
                    r.getDependencyBindingRef(),String.valueOf(r.getAuthoritativeClinicalStateVersion()),
                    r.getF3OwnerPolicyRef(),r.getBusinessEventIdentity(),"GAP_BASIS",gap,impact,
                    String.valueOf(in.isAskableOnline()));
            return new U06SyntheticDecisionBundle(
                    U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED,effect,gap,impact,in.isAskableOnline(),
                    null,null,null,null,null,null,null,null,null);
        }
        if(in.hasExplicitNoCurrentOnlineGapBasis()) {
            String effect=U06Ids.hash("u06f3",r.getConsultationId(),r.getDependencyBindingType(),
                    r.getDependencyBindingRef(),String.valueOf(r.getAuthoritativeClinicalStateVersion()),
                    r.getF3OwnerPolicyRef(),r.getBusinessEventIdentity(),"NO_CURRENT_ONLINE_GAP_BASIS");
            return new U06SyntheticDecisionBundle(
                    U06SyntheticDecisionBundle.NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED,effect,null,null,false,
                    null,null,null,null,null,null,null,null,null);
        }
        return noDecision(r,"SUCCESS_WITHOUT_EXPLICIT_F3_BASIS");
    }

    private U06SyntheticDecisionBundle decideMode2(U06ProfileBRequest r,U06SyntheticDecisionInput in,
                                                    U06SyntheticP01Runtime.StateView current) {
        if(current.exists("/patient_state/pending_question"))
            return noSelection(r,U06SyntheticDecisionBundle.STOP,"CURRENT_PENDING_QUESTION");

        if(U06SyntheticDecisionInput.POLICY_FAIL.equals(in.getD04PolicyToken()))
            return failedSelection(r,"D04_POLICY_FAILED");
        if(U06SyntheticDecisionInput.POLICY_STOP.equals(in.getD04PolicyToken()))
            return noSelection(r,U06SyntheticDecisionBundle.STOP,"D04_POLICY_STOP");
        if(!U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE.equals(in.getD04PolicyToken()))
            return failedSelection(r,"D04_POLICY_TOKEN_INVALID");

        List<U06SyntheticDecisionInput.Candidate> eligible=new ArrayList<U06SyntheticDecisionInput.Candidate>();
        for(U06SyntheticDecisionInput.Candidate candidate:in.getCandidates()) {
            if(!candidate.isLawful())continue;
            if(duplicateSemanticKey(current,candidate.getSemanticKey()))continue;
            eligible.add(candidate);
        }

        if(eligible.isEmpty())
            return noSelection(r,U06SyntheticDecisionBundle.STOP,"NO_LAWFUL_CANDIDATE");

        int best=Integer.MAX_VALUE;
        for(U06SyntheticDecisionInput.Candidate c:eligible)best=Math.min(best,c.getDeterministicRank());

        U06SyntheticDecisionInput.Candidate selected=null;
        int bestCount=0;
        for(U06SyntheticDecisionInput.Candidate c:eligible) {
            if(c.getDeterministicRank()==best){bestCount++;selected=c;}
        }
        if(bestCount!=1)
            return selectionFailedAfterContinue(r,"DETERMINISTIC_TIE_UNRESOLVED");

        String selection=U06Ids.hash("u06selection",r.getConsultationId(),
                selected.getCandidateId(),selected.getSemanticKey(),
                String.valueOf(r.getAuthoritativeClinicalStateVersion()),
                r.getDependencyBindingType(),r.getDependencyBindingRef(),
                r.getF3OwnerPolicyRef(),r.getQuestionPolicyRef(),r.getD04PolicyRef(),"1");

        return new U06SyntheticDecisionBundle(
                U06SyntheticDecisionBundle.NOT_DECIDABLE,null,in.getGapId(),in.getGapDecisionImpact(),
                in.isAskableOnline(),U06SyntheticDecisionBundle.CONTINUE,U06SyntheticDecisionBundle.SELECTED,
                selection,selected.getQuestionId(),selected.getSemanticKey(),selected.getContentRef(),
                selected.getContentFingerprint(),null,null);
    }

    @SuppressWarnings("unchecked")
    private boolean duplicateSemanticKey(U06SyntheticP01Runtime.StateView current,String semanticKey) {
        Object questions=current.value("/patient_state/questions");
        if(!(questions instanceof Map))return false;
        for(Object raw:((Map<String,Object>)questions).values()) {
            if(!(raw instanceof Map))continue;
            Map<String,Object> q=(Map<String,Object>)raw;
            if(!semanticKey.equals(String.valueOf(q.get("question_semantic_key"))))continue;
            String status=String.valueOf(q.get("status"));
            if("SELECTED".equals(status)||"DELIVERED_TO_USER".equals(status)||"ANSWER_RECEIVED".equals(status))
                return true;
        }
        return false;
    }

    private U06SyntheticDecisionBundle noDecision(U06ProfileBRequest r,String reason) {
        if(U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT.equals(r.getMode()))
            return new U06SyntheticDecisionBundle(U06SyntheticDecisionBundle.NOT_DECIDABLE,null,null,reason,false,
                    null,null,null,null,null,null,null,null,null);
        return noSelection(r,null,reason);
    }

    private U06SyntheticDecisionBundle failed(U06ProfileBRequest r,String reason) {
        if(U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT.equals(r.getMode()))
            return new U06SyntheticDecisionBundle(U06SyntheticDecisionBundle.FAILED,null,null,reason,false,
                    null,null,null,null,null,null,null,null,null);
        return capabilityFailedSelection(r,reason);
    }

    private U06SyntheticDecisionBundle noSelection(U06ProfileBRequest r,String d04,String reason) {
        return new U06SyntheticDecisionBundle(U06SyntheticDecisionBundle.NOT_DECIDABLE,null,null,reason,false,
                d04,U06SyntheticDecisionBundle.NO_SELECTION,null,null,null,null,null,null,null);
    }

    private U06SyntheticDecisionBundle failedSelection(U06ProfileBRequest r,String reason) {
        return new U06SyntheticDecisionBundle(U06SyntheticDecisionBundle.NOT_DECIDABLE,null,null,reason,false,
                U06SyntheticDecisionBundle.FAILED,U06SyntheticDecisionBundle.FAILED,null,null,null,null,null,null,null);
    }

    private U06SyntheticDecisionBundle capabilityFailedSelection(U06ProfileBRequest r,String reason) {
        return new U06SyntheticDecisionBundle(U06SyntheticDecisionBundle.NOT_DECIDABLE,null,null,reason,false,
                null,U06SyntheticDecisionBundle.FAILED,null,null,null,null,null,null,null);
    }

    private U06SyntheticDecisionBundle selectionFailedAfterContinue(U06ProfileBRequest r,String reason) {
        return new U06SyntheticDecisionBundle(U06SyntheticDecisionBundle.NOT_DECIDABLE,null,null,reason,false,
                U06SyntheticDecisionBundle.CONTINUE,U06SyntheticDecisionBundle.FAILED,null,null,null,null,null,null,null);
    }

    private static String req(String v,String n){
        if(v==null||v.trim().isEmpty())throw new IllegalArgumentException(n+" is required");
        return v.trim();
    }
}
