package com.aidoctor.diagnosis.runtime.u06.wait;

import com.aidoctor.diagnosis.runtime.u06.U06Ids;

public final class U07ResumeEligibilityProjector {
    public String project(String consultationId,String threadId,String runId,String checkpointId,
                          String parentWaitEffectId,boolean threadAwaitingUser) {
        if(!threadAwaitingUser)throw new IllegalStateException("U06_THREAD_NOT_AWAITING_USER");
        return U06Ids.hash("u07elig",required(parentWaitEffectId),required(checkpointId),"1");
    }
    private static String required(String v){
        if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("value required");
        return v.trim();
    }
}
