package com.aidoctor.diagnosis.runtime.u06.delivery;
public interface U06DeliveryStore {
    Snapshot reconcileConfirmed(Command c);
    final class Command{public final String selectionEffectId,deliveryEffectId,deliveryId,questionId,contentFingerprint,endpointRef,idempotencyKey,confirmationEvaluationId,confirmationFingerprint,createdAt;
        public Command(String s,String e,String d,String q,String c,String endpoint,String idem,String eval,String fp,String at){selectionEffectId=req(s);deliveryEffectId=req(e);deliveryId=req(d);questionId=req(q);contentFingerprint=req(c);endpointRef=req(endpoint);idempotencyKey=req(idem);confirmationEvaluationId=req(eval);confirmationFingerprint=req(fp);createdAt=req(at);}}
    final class Snapshot{public final String deliveryEffectId,deliveryId,confirmationStatus;public final boolean replay;public Snapshot(String e,String d,String s,boolean r){deliveryEffectId=e;deliveryId=d;confirmationStatus=s;replay=r;}}
    static String req(String v){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("delivery value required");return v.trim();}
}
