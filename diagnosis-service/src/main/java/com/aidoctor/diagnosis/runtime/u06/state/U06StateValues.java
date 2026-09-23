package com.aidoctor.diagnosis.runtime.u06.state;
import com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest;
import com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle;
import java.util.ArrayList;import java.util.LinkedHashMap;import java.util.List;import java.util.Map;
public final class U06StateValues {
    private U06StateValues(){}
    public static Map<String,Object> f3Assessment(U06ProfileBRequest r,U06SyntheticDecisionBundle d,String canonicalEffectId){Map<String,Object>v=new LinkedHashMap<String,Object>();
        v.put("f3_state_record_id",canonicalEffectId);v.put("f3_canonical_effect_id",canonicalEffectId);v.put("consultation_id",r.getConsultationId());
        v.put("cdp_id",r.getCdpId());v.put("owner_decision",d.getF3OwnerStatus());v.put("dependency_binding_type",r.getDependencyBindingType());v.put("dependency_binding_ref",r.getDependencyBindingRef());
        v.put("f3_owner_policy_ref",r.getF3OwnerPolicyRef());v.put("online_gap_basis_status",d.getF3OwnerStatus());if(d.getGapId()!=null)v.put("gap_refs",list(d.getGapId()));v.put("validity","CURRENT");return v;}
    public static Map<String,Object> gap(U06ProfileBRequest r,U06SyntheticDecisionBundle d,String status){Map<String,Object>v=new LinkedHashMap<String,Object>();v.put("gap_id",d.getGapId());v.put("status",status);
        v.put("decision_impact",d.getGapDecisionImpact());v.put("askable_online",Boolean.valueOf(d.isAskableOnline()));v.put("source_basis_refs",list(r.getSourceAuthorityRef()));v.put("question_refs",new ArrayList<String>());return v;}
    public static Map<String,Object> gapAsked(U06ProfileBRequest r,U06SyntheticDecisionBundle d){Map<String,Object>v=gap(r,d,"ASKED");v.put("question_refs",list(d.getQuestionId()));return v;}
    public static Map<String,Object> questionSelected(U06ProfileBRequest r,U06SyntheticDecisionBundle d){Map<String,Object>v=new LinkedHashMap<String,Object>();
        v.put("question_id",d.getQuestionId());v.put("question_semantic_key",d.getQuestionSemanticKey());v.put("need_class","F3_INFORMATION_GAP");v.put("purpose","SYNTHETIC_STRUCTURAL_VERIFICATION");
        if(d.getGapId()!=null)v.put("source_requirement_ref",d.getGapId());v.put("content_ref",d.getQuestionContentRef());v.put("content_fingerprint",d.getQuestionContentFingerprint());
        v.put("clinical_state_version_basis",Integer.valueOf(r.getAuthoritativeClinicalStateVersion()));v.put("dependency_binding_type",r.getDependencyBindingType());v.put("dependency_binding_ref",r.getDependencyBindingRef());
        v.put("f3_owner_policy_ref",r.getF3OwnerPolicyRef());v.put("question_policy_ref",r.getQuestionPolicyRef());v.put("d04_policy_ref",r.getD04PolicyRef());
        v.put("d04_decision",d.getD04Status());v.put("selection_decision",d.getQuestionSelectionStatus());v.put("status","SELECTED");v.put("selection_effect_id",d.getQuestionSelectionEffectId());return v;}
    public static Map<String,Object> questionDelivered(U06ProfileBRequest r,U06SyntheticDecisionBundle d,String deliveryEffect,String deliveryId){Map<String,Object>v=questionSelected(r,d);v.put("status","DELIVERED_TO_USER");v.put("delivery_effect_id",deliveryEffect);v.put("delivery_id",deliveryId);return v;}
    public static Map<String,Object> pending(U06SyntheticDecisionBundle d,String parentEffect,String deliveryId){Map<String,Object>v=new LinkedHashMap<String,Object>();v.put("question_id",d.getQuestionId());v.put("question_selection_effect_id",d.getQuestionSelectionEffectId());v.put("question_delivered_wait_effect_id",parentEffect);v.put("delivery_id",deliveryId);v.put("status","DELIVERED_TO_USER");return v;}
    private static List<String> list(String x){List<String>l=new ArrayList<String>();l.add(x);return l;}
}
