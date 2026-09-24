package com.aidoctor.diagnosis.runtime.u06;
public final class U06SyntheticDecisionBundle {
    public static final String GAP_BASIS_ESTABLISHED="GAP_BASIS_ESTABLISHED",NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED="NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED",
        NOT_DECIDABLE="NOT_DECIDABLE",FAILED="FAILED",CONTINUE="CONTINUE",STOP="STOP",SELECTED="SELECTED",NO_SELECTION="NO_SELECTION",
        REVALIDATED_CURRENT="REVALIDATED_CURRENT",REASSESSMENT_REQUIRED="REASSESSMENT_REQUIRED";
    private final String f3OwnerStatus,f3CanonicalEffectId,gapId,gapDecisionImpact,d04Status,questionSelectionStatus,questionSelectionEffectId,
        questionId,questionSemanticKey,questionContentRef,questionContentFingerprint,revalidationStatus,revalidationRef;
    private final boolean askableOnline;
    public U06SyntheticDecisionBundle(String f3OwnerStatus,String f3CanonicalEffectId,String gapId,String gapDecisionImpact,boolean askableOnline,
        String d04Status,String questionSelectionStatus,String questionSelectionEffectId,String questionId,String questionSemanticKey,
        String questionContentRef,String questionContentFingerprint,String revalidationStatus,String revalidationRef){
        this.f3OwnerStatus=req(f3OwnerStatus,"f3OwnerStatus");this.f3CanonicalEffectId=f3CanonicalEffectId;this.gapId=gapId;
        this.gapDecisionImpact=gapDecisionImpact;this.askableOnline=askableOnline;this.d04Status=d04Status;this.questionSelectionStatus=questionSelectionStatus;
        this.questionSelectionEffectId=questionSelectionEffectId;this.questionId=questionId;this.questionSemanticKey=questionSemanticKey;
        this.questionContentRef=questionContentRef;this.questionContentFingerprint=questionContentFingerprint;this.revalidationStatus=revalidationStatus;this.revalidationRef=revalidationRef;validate();
    }
    private void validate(){if(GAP_BASIS_ESTABLISHED.equals(f3OwnerStatus)){req(f3CanonicalEffectId,"f3CanonicalEffectId");req(gapId,"gapId");req(gapDecisionImpact,"gapDecisionImpact");}
        if(SELECTED.equals(questionSelectionStatus)){if(!CONTINUE.equals(d04Status))throw new IllegalArgumentException("SELECTED requires CONTINUE");
            req(questionSelectionEffectId,"questionSelectionEffectId");req(questionId,"questionId");req(questionSemanticKey,"questionSemanticKey");
            req(questionContentRef,"questionContentRef");req(questionContentFingerprint,"questionContentFingerprint");}
        if(REVALIDATED_CURRENT.equals(revalidationStatus)){req(revalidationRef,"revalidationRef");req(f3CanonicalEffectId,"f3CanonicalEffectId");}
        if(REASSESSMENT_REQUIRED.equals(revalidationStatus)){req(revalidationRef,"revalidationRef");}}
    public String getF3OwnerStatus(){return f3OwnerStatus;}public String getF3CanonicalEffectId(){return f3CanonicalEffectId;}public String getGapId(){return gapId;}
    public String getGapDecisionImpact(){return gapDecisionImpact;}public boolean isAskableOnline(){return askableOnline;}public String getD04Status(){return d04Status;}
    public String getQuestionSelectionStatus(){return questionSelectionStatus;}public String getQuestionSelectionEffectId(){return questionSelectionEffectId;}
    public String getQuestionId(){return questionId;}public String getQuestionSemanticKey(){return questionSemanticKey;}public String getQuestionContentRef(){return questionContentRef;}
    public String getQuestionContentFingerprint(){return questionContentFingerprint;}public String getRevalidationStatus(){return revalidationStatus;}public String getRevalidationRef(){return revalidationRef;}
    private static String req(String v,String n){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException(n+" is required");return v.trim();}
}
