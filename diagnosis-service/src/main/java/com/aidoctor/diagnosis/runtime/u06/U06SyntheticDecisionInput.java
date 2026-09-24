package com.aidoctor.diagnosis.runtime.u06;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class U06SyntheticDecisionInput {
    public static final String SUCCESS="SUCCESS";
    public static final String NO_RESULT="NO_RESULT";
    public static final String INSUFFICIENT_INFORMATION="INSUFFICIENT_INFORMATION";
    public static final String DEPENDENCY_FAILURE="DEPENDENCY_FAILURE";
    public static final String TIMEOUT="TIMEOUT";
    public static final String INVALID_OUTPUT="INVALID_OUTPUT";

    public static final String POLICY_ALLOW_CONTINUE="ALLOW_CONTINUE";
    public static final String POLICY_STOP="STOP";
    public static final String POLICY_FAIL="FAIL";

    private final String c03BusinessStatus;
    private final boolean explicitGapBasis;
    private final boolean explicitNoCurrentOnlineGapBasis;
    private final String gapId;
    private final String gapDecisionImpact;
    private final boolean askableOnline;
    private final String d04PolicyToken;
    private final List<Candidate> candidates;

    public U06SyntheticDecisionInput(String c03BusinessStatus,boolean explicitGapBasis,
            boolean explicitNoCurrentOnlineGapBasis,String gapId,String gapDecisionImpact,
            boolean askableOnline,String d04PolicyToken,List<Candidate> candidates) {
        this.c03BusinessStatus=req(c03BusinessStatus,"c03BusinessStatus");
        if(explicitGapBasis&&explicitNoCurrentOnlineGapBasis)
            throw new IllegalArgumentException("synthetic gap basis is mutually exclusive");
        this.explicitGapBasis=explicitGapBasis;
        this.explicitNoCurrentOnlineGapBasis=explicitNoCurrentOnlineGapBasis;
        this.gapId=gapId;
        this.gapDecisionImpact=gapDecisionImpact;
        this.askableOnline=askableOnline;
        this.d04PolicyToken=d04PolicyToken;
        this.candidates=candidates==null?Collections.<Candidate>emptyList():
                Collections.unmodifiableList(new ArrayList<Candidate>(candidates));
    }

    public String getC03BusinessStatus(){return c03BusinessStatus;}
    public boolean hasExplicitGapBasis(){return explicitGapBasis;}
    public boolean hasExplicitNoCurrentOnlineGapBasis(){return explicitNoCurrentOnlineGapBasis;}
    public String getGapId(){return gapId;}
    public String getGapDecisionImpact(){return gapDecisionImpact;}
    public boolean isAskableOnline(){return askableOnline;}
    public String getD04PolicyToken(){return d04PolicyToken;}
    public List<Candidate> getCandidates(){return candidates;}

    public static final class Candidate {
        private final String candidateId,questionId,semanticKey,contentRef,contentFingerprint;
        private final int deterministicRank;
        private final boolean lawful;

        public Candidate(String candidateId,String questionId,String semanticKey,String contentRef,
                String contentFingerprint,int deterministicRank,boolean lawful) {
            this.candidateId=req(candidateId,"candidateId");
            this.questionId=req(questionId,"questionId");
            this.semanticKey=req(semanticKey,"semanticKey");
            this.contentRef=req(contentRef,"contentRef");
            this.contentFingerprint=req(contentFingerprint,"contentFingerprint");
            if(deterministicRank<0)throw new IllegalArgumentException("deterministicRank must be non-negative");
            this.deterministicRank=deterministicRank;
            this.lawful=lawful;
        }
        public String getCandidateId(){return candidateId;}
        public String getQuestionId(){return questionId;}
        public String getSemanticKey(){return semanticKey;}
        public String getContentRef(){return contentRef;}
        public String getContentFingerprint(){return contentFingerprint;}
        public int getDeterministicRank(){return deterministicRank;}
        public boolean isLawful(){return lawful;}
    }

    private static String req(String v,String n){
        if(v==null||v.trim().isEmpty())throw new IllegalArgumentException(n+" is required");
        return v.trim();
    }
}
