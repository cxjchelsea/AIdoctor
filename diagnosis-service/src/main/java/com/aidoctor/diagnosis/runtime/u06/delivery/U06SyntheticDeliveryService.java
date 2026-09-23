package com.aidoctor.diagnosis.runtime.u06.delivery;
import com.aidoctor.diagnosis.runtime.u06.U06Ids;
public final class U06SyntheticDeliveryService {
 public static final String CONFIRMED="CONFIRMED"; private final U06DeliveryStore store;
 public U06SyntheticDeliveryService(U06DeliveryStore store){if(store==null)throw new IllegalArgumentException("store required");this.store=store;}
 public Confirmation confirm(String consultationId,String selectionEffectId,String questionId,String contentFingerprint,ScopeAuthorization scope,String createdAt){
  if(scope==null||!scope.isValidFor(consultationId))throw new IllegalStateException("U06_SYNTHETIC_DELIVERY_SCOPE_INVALID");
  String effect=U06Ids.hash("u06de",consultationId,selectionEffectId,contentFingerprint),deliveryId=U06Ids.hash("u06delivery",effect),idem=U06Ids.hash("u06deliveryidem",effect,"1"),
    eval=U06Ids.hash("u06confirm",effect,deliveryId,contentFingerprint),
    fp=U06Ids.hash("u06confirmfp",consultationId,selectionEffectId,effect,deliveryId,questionId,contentFingerprint,scope.syntheticEndpointRef,idem,eval,"CONFIRMED");
  U06DeliveryStore.Snapshot s=store.reconcileConfirmed(new U06DeliveryStore.Command(selectionEffectId,effect,deliveryId,questionId,contentFingerprint,scope.syntheticEndpointRef,idem,eval,fp,createdAt));
  if(!CONFIRMED.equals(s.confirmationStatus))throw new IllegalStateException("U06_SYNTHETIC_CONFIRMATION_NOT_CONFIRMED");
  return new Confirmation(effect,deliveryId,eval,fp,s.replay);
 }
 public static final class ScopeAuthorization{
  private final String consultationId,executionProfile,fixtureRef,stateStoreRef,environmentRef,syntheticEndpointRef;private final boolean externalSideEffectAllowed,realRecipientAllowed,productionStoreAllowed;
  public ScopeAuthorization(String c,String p,String f,String s,String e,String endpoint,boolean ext,boolean real,boolean prod){consultationId=req(c);executionProfile=req(p);fixtureRef=req(f);stateStoreRef=req(s);environmentRef=req(e);syntheticEndpointRef=req(endpoint);externalSideEffectAllowed=ext;realRecipientAllowed=real;productionStoreAllowed=prod;}
  public boolean isValidFor(String c){return consultationId.equals(c)&&"SYNTHETIC_STRUCTURAL_NONPROD".equals(executionProfile)&&syntheticEndpointRef.startsWith("synthetic-")&&!externalSideEffectAllowed&&!realRecipientAllowed&&!productionStoreAllowed;}
  public String getStateStoreRef(){return stateStoreRef;} public String getSyntheticEndpointRef(){return syntheticEndpointRef;}
 }
 public static final class Confirmation{private final String deliveryEffectId,deliveryId,confirmationEvaluationId,confirmationFingerprint;private final boolean replay;
  Confirmation(String e,String d,String i,String f,boolean r){deliveryEffectId=e;deliveryId=d;confirmationEvaluationId=i;confirmationFingerprint=f;replay=r;}
  public String getDeliveryEffectId(){return deliveryEffectId;}public String getDeliveryId(){return deliveryId;}public String getConfirmationEvaluationId(){return confirmationEvaluationId;}public String getConfirmationFingerprint(){return confirmationFingerprint;}public boolean isReplay(){return replay;}}
 private static String req(String v){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("value required");return v.trim();}
}
