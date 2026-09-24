package com.aidoctor.diagnosis.runtime.u06.delivery;
import com.aidoctor.diagnosis.runtime.u06.U06Ids;
import java.time.OffsetDateTime;
import java.util.Locale;

public final class U06SyntheticDeliveryService {
 public static final String CONFIRMED="CONFIRMED"; private final U06DeliveryStore store;
 public U06SyntheticDeliveryService(U06DeliveryStore store){if(store==null)throw new IllegalArgumentException("store required");this.store=store;}

 public Confirmation confirm(String consultationId,String selectionEffectId,String questionId,String contentFingerprint,ScopeAuthorization scope,String createdAt){
  if(scope==null||!scope.isValidFor(consultationId,createdAt))throw new IllegalStateException("U06_SYNTHETIC_DELIVERY_SCOPE_INVALID");
  String effect=U06Ids.hash("u06de",consultationId,selectionEffectId,contentFingerprint),deliveryId=U06Ids.hash("u06delivery",effect),idem=U06Ids.hash("u06deliveryidem",effect,"1"),
    eval=U06Ids.hash("u06confirm",effect,deliveryId,contentFingerprint),
    fp=U06Ids.hash("u06confirmfp",consultationId,selectionEffectId,effect,deliveryId,questionId,contentFingerprint,scope.syntheticEndpointRef,scope.scopeAuthorizationId,scope.authorizationRef,idem,eval,"CONFIRMED");
  U06DeliveryStore.Snapshot s=store.reconcileConfirmed(new U06DeliveryStore.Command(selectionEffectId,effect,deliveryId,questionId,contentFingerprint,scope.syntheticEndpointRef,idem,eval,fp,createdAt));
  if(!CONFIRMED.equals(s.confirmationStatus))throw new IllegalStateException("U06_SYNTHETIC_CONFIRMATION_NOT_CONFIRMED");
  return new Confirmation(effect,deliveryId,eval,fp,s.replay);
 }

 public static final class ScopeAuthorization{
  public static final String CURRENT="CURRENT";
  private final String scopeAuthorizationId,authorizationRef,validity,consultationId,executionProfile,fixtureRef,fixtureReviewRef,stateStoreRef,environmentRef,syntheticEndpointRef,expiresAt;
  private final boolean externalSideEffectAllowed,realRecipientAllowed,productionStoreAllowed;

  public ScopeAuthorization(String scopeAuthorizationId,String authorizationRef,String validity,
          String consultationId,String executionProfile,String fixtureRef,String fixtureReviewRef,
          String stateStoreRef,String environmentRef,String syntheticEndpointRef,String expiresAt,
          boolean ext,boolean real,boolean prod){
    this.scopeAuthorizationId=req(scopeAuthorizationId);this.authorizationRef=req(authorizationRef);this.validity=req(validity);
    this.consultationId=req(consultationId);this.executionProfile=req(executionProfile);this.fixtureRef=req(fixtureRef);
    this.fixtureReviewRef=req(fixtureReviewRef);this.stateStoreRef=req(stateStoreRef);this.environmentRef=req(environmentRef);
    this.syntheticEndpointRef=req(syntheticEndpointRef);this.expiresAt=expiresAt;
    externalSideEffectAllowed=ext;realRecipientAllowed=real;productionStoreAllowed=prod;
  }

  public boolean isValidFor(String c,String currentAt){
    if(!consultationId.equals(c)
            ||!"SYNTHETIC_STRUCTURAL_NONPROD".equals(executionProfile)
            ||!CURRENT.equals(validity)
            ||!syntheticEndpointRef.startsWith("synthetic-")
            ||!nonProductionEnvironment(environmentRef)
            ||externalSideEffectAllowed||realRecipientAllowed||productionStoreAllowed)return false;
    if(expiresAt!=null&&!expiresAt.trim().isEmpty()){
      try{
        OffsetDateTime now=OffsetDateTime.parse(req(currentAt));
        OffsetDateTime expiry=OffsetDateTime.parse(expiresAt);
        if(now.isAfter(expiry))return false;
      }catch(RuntimeException invalidTime){return false;}
    }
    return true;
  }

  private static boolean nonProductionEnvironment(String v){
    String x=req(v).toLowerCase(Locale.ROOT);
    if(x.equals("prod")||x.startsWith("prod-")||x.contains("production"))return false;
    return x.startsWith("ci-")||x.startsWith("test-")||x.startsWith("nonprod-")||x.startsWith("local-");
  }

  public String getScopeAuthorizationId(){return scopeAuthorizationId;}
  public String getAuthorizationRef(){return authorizationRef;}
  public String getFixtureReviewRef(){return fixtureReviewRef;}
  public String getStateStoreRef(){return stateStoreRef;}
  public String getEnvironmentRef(){return environmentRef;}
  public String getSyntheticEndpointRef(){return syntheticEndpointRef;}
 }

 public static final class Confirmation{private final String deliveryEffectId,deliveryId,confirmationEvaluationId,confirmationFingerprint;private final boolean replay;
  Confirmation(String e,String d,String i,String f,boolean r){deliveryEffectId=e;deliveryId=d;confirmationEvaluationId=i;confirmationFingerprint=f;replay=r;}
  public String getDeliveryEffectId(){return deliveryEffectId;}public String getDeliveryId(){return deliveryId;}public String getConfirmationEvaluationId(){return confirmationEvaluationId;}public String getConfirmationFingerprint(){return confirmationFingerprint;}public boolean isReplay(){return replay;}}

 private static String req(String v){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("value required");return v.trim();}
}
