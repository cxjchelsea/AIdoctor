package com.aidoctor.diagnosis.runtime.u06.delivery;

import com.aidoctor.diagnosis.runtime.u06.U06Ids;
import java.util.*;

public final class U06SyntheticDeliveryRuntime {
    public static final String ACTIVE_PENDING="ACTIVE_PENDING";
    public static final String RETRYABLE_NOT_CONFIRMED="RETRYABLE_NOT_CONFIRMED";
    public static final String RECONCILIATION_BLOCKED="RECONCILIATION_BLOCKED";
    public static final String CONFIRMED_TERMINAL="CONFIRMED_TERMINAL";
    public static final String NOT_CONFIRMED_TERMINAL="NOT_CONFIRMED_TERMINAL";
    public static final String CANCELLED_TERMINAL="CANCELLED_TERMINAL";

    public static final String READY="READY";
    public static final String CANCELLED_BEFORE_SEND="CANCELLED_BEFORE_SEND";

    public static final String ACCEPTED="ACCEPTED";
    public static final String DELIVERED="DELIVERED";
    public static final String TRANSIENT_NOT_DELIVERED="TRANSIENT_NOT_DELIVERED";
    public static final String AMBIGUOUS="AMBIGUOUS";
    public static final String FAILED="FAILED";

    public static final String CONFIRMED="CONFIRMED";
    public static final String NOT_CONFIRMED="NOT_CONFIRMED";
    public static final String INDETERMINATE="INDETERMINATE";

    private final Map<String,DeliveryState> bySelection=new LinkedHashMap<String,DeliveryState>();
    private final Map<String,DeliveryState> byEffect=new LinkedHashMap<String,DeliveryState>();

    public synchronized Snapshot createIntent(String consultationId,String selectionEffectId,String questionId,
                                              String contentFingerprint,String endpointRef,String policyRef,String createdAt) {
        String effect=U06Ids.hash("u06de",req(consultationId),req(questionId),req(selectionEffectId),
                req(contentFingerprint),req(endpointRef),req(policyRef),"1");
        String deliveryId=U06Ids.hash("u06delivery",effect);
        String idem=U06Ids.hash("u06deliveryidem",effect,"1");
        String fp=U06Ids.hash("u06intentfp",consultationId,selectionEffectId,questionId,contentFingerprint,
                endpointRef,policyRef,deliveryId,idem,"1");

        DeliveryState active=bySelection.get(selectionEffectId);
        if(active!=null&&!terminal(active.authorityStatus)) {
            if(active.deliveryEffectId.equals(effect)) {
                if(!active.intentFingerprint.equals(fp))throw new IllegalStateException("U06_DELIVERY_REPLAY_CONFLICT");
                return active.snapshot(true);
            }
            throw new IllegalStateException("U06_SECOND_ACTIVE_DELIVERY_EFFECT_PROHIBITED");
        }
        DeliveryState existing=byEffect.get(effect);
        if(existing!=null) {
            if(!existing.intentFingerprint.equals(fp))throw new IllegalStateException("U06_DELIVERY_REPLAY_CONFLICT");
            return existing.snapshot(true);
        }
        DeliveryState s=new DeliveryState();
        s.consultationId=consultationId;s.selectionEffectId=selectionEffectId;s.questionId=questionId;
        s.contentFingerprint=contentFingerprint;s.endpointRef=endpointRef;s.policyRef=policyRef;
        s.deliveryEffectId=effect;s.deliveryId=deliveryId;s.idempotencyKey=idem;s.intentFingerprint=fp;
        s.intentStatus=READY;s.authorityStatus=ACTIVE_PENDING;s.createdAt=createdAt;
        bySelection.put(selectionEffectId,s);byEffect.put(effect,s);
        return s.snapshot(false);
    }

    public synchronized Snapshot rebindBeforeSend(String oldEffect,String newEndpointRef,boolean allowed,String createdAt) {
        DeliveryState old=state(oldEffect);
        if(!allowed)throw new IllegalStateException("U06_DELIVERY_REBIND_DENIED");
        if(!old.attempts.isEmpty()||CONFIRMED_TERMINAL.equals(old.authorityStatus)
                ||RECONCILIATION_BLOCKED.equals(old.authorityStatus))
            throw new IllegalStateException("U06_DELIVERY_REBIND_DENIED");
        old.intentStatus=CANCELLED_BEFORE_SEND;old.authorityStatus=CANCELLED_TERMINAL;
        bySelection.remove(old.selectionEffectId);
        return createIntent(old.consultationId,old.selectionEffectId,old.questionId,old.contentFingerprint,
                newEndpointRef,old.policyRef,createdAt);
    }

    public synchronized AttemptResult startAttemptWithoutReceipt(String effect,String transportScenario,String createdAt) {
        DeliveryState s=state(effect);
        if(!READY.equals(s.intentStatus))throw new IllegalStateException("U06_DELIVERY_INTENT_NOT_READY");
        if(CONFIRMED_TERMINAL.equals(s.authorityStatus)||NOT_CONFIRMED_TERMINAL.equals(s.authorityStatus)
                ||CANCELLED_TERMINAL.equals(s.authorityStatus))
            throw new IllegalStateException("U06_DELIVERY_TERMINAL_NO_SEND");
        int n=s.attempts.size()+1;
        String attemptId=U06Ids.hash("u06attempt",s.deliveryEffectId,String.valueOf(n));
        Attempt a=new Attempt(attemptId,s.deliveryId,s.idempotencyKey,transportScenario,createdAt);
        a.status=transportScenario!=null&&transportScenario.startsWith("AMBIGUOUS")?AMBIGUOUS:ACCEPTED;
        s.attempts.add(a);
        if(AMBIGUOUS.equals(a.status))s.authorityStatus=RECONCILIATION_BLOCKED;
        return new AttemptResult(a.attemptId,a.deliveryId,a.idempotencyKey,a.status,s.authorityStatus,
                latestConfirmation(s),s.snapshot(false));
    }

    public synchronized Snapshot recordReceiptWithoutConfirmation(String effect,String receiptStatus,String createdAt) {
        DeliveryState s=state(effect);
        if(s.attempts.isEmpty())throw new IllegalStateException("U06_DELIVERY_ATTEMPT_REQUIRED");
        Attempt a=s.attempts.get(s.attempts.size()-1);
        addReceipt(s,a,req(receiptStatus),createdAt);
        return s.snapshot(false);
    }

    public synchronized Snapshot resolveConfirmation(String effect,String confirmationStatus,String evidenceRef,String createdAt) {
        DeliveryState s=state(effect);
        if(s.receipts.isEmpty()&&!INDETERMINATE.equals(confirmationStatus))
            throw new IllegalStateException("U06_DELIVERY_RECEIPT_REQUIRED");
        evaluateConfirmation(s,req(confirmationStatus),req(evidenceRef),createdAt);
        return s.snapshot(false);
    }

    public synchronized AttemptResult attempt(String effect,String transportScenario,String createdAt) {
        DeliveryState s=state(effect);
        if(!READY.equals(s.intentStatus))throw new IllegalStateException("U06_DELIVERY_INTENT_NOT_READY");
        if(CONFIRMED_TERMINAL.equals(s.authorityStatus)||NOT_CONFIRMED_TERMINAL.equals(s.authorityStatus)
                ||CANCELLED_TERMINAL.equals(s.authorityStatus))
            throw new IllegalStateException("U06_DELIVERY_TERMINAL_NO_SEND");

        int n=s.attempts.size()+1;
        String attemptId=U06Ids.hash("u06attempt",s.deliveryEffectId,String.valueOf(n));
        Attempt a=new Attempt(attemptId,s.deliveryId,s.idempotencyKey,transportScenario,createdAt);
        s.attempts.add(a);

        if("SYNTHETIC_DELIVERED".equals(transportScenario)||"CONFIRMED".equals(transportScenario)) {
            a.status=DELIVERED;
            addReceipt(s,a,DELIVERED,createdAt);
            evaluateConfirmation(s,CONFIRMED,"synthetic-delivered-"+n,createdAt);
        } else if("RECEIPT_ACCEPTED_ONLY".equals(transportScenario)) {
            a.status=ACCEPTED;addReceipt(s,a,ACCEPTED,createdAt);
            evaluateConfirmation(s,NOT_CONFIRMED,"accepted-only-"+n,createdAt);
        } else if("TRANSIENT_NOT_DELIVERED_RETRY_ALLOWED".equals(transportScenario)) {
            a.status=TRANSIENT_NOT_DELIVERED;
            addReceipt(s,a,TRANSIENT_NOT_DELIVERED,createdAt);
            s.authorityStatus=RETRYABLE_NOT_CONFIRMED;
            evaluateConfirmation(s,NOT_CONFIRMED,"transient-"+n,createdAt);
        } else if("RETRY_EXHAUSTED".equals(transportScenario)) {
            a.status=FAILED;addReceipt(s,a,FAILED,createdAt);
            s.authorityStatus=NOT_CONFIRMED_TERMINAL;
            evaluateConfirmation(s,NOT_CONFIRMED,"retry-exhausted-"+n,createdAt);
        } else if(transportScenario!=null&&transportScenario.startsWith("AMBIGUOUS")) {
            a.status=AMBIGUOUS;
            s.authorityStatus=RECONCILIATION_BLOCKED;
            evaluateConfirmation(s,INDETERMINATE,"ambiguous-"+n,createdAt);
        } else if("DELIVERY_FAILURE".equals(transportScenario)) {
            a.status=FAILED;addReceipt(s,a,FAILED,createdAt);
            s.authorityStatus=NOT_CONFIRMED_TERMINAL;
            evaluateConfirmation(s,NOT_CONFIRMED,"failure-"+n,createdAt);
        } else {
            a.status=ACCEPTED;addReceipt(s,a,ACCEPTED,createdAt);
            evaluateConfirmation(s,NOT_CONFIRMED,"accepted-"+n,createdAt);
        }
        return new AttemptResult(a.attemptId,a.deliveryId,a.idempotencyKey,a.status,s.authorityStatus,
                latestConfirmation(s),s.snapshot(false));
    }

    public synchronized AttemptResult retrySameEffect(String effect,String transportScenario,String createdAt) {
        DeliveryState s=state(effect);
        if(!(RETRYABLE_NOT_CONFIRMED.equals(s.authorityStatus)||RECONCILIATION_BLOCKED.equals(s.authorityStatus)))
            throw new IllegalStateException("U06_DELIVERY_RETRY_NOT_ALLOWED");
        if(RECONCILIATION_BLOCKED.equals(s.authorityStatus)
                &&"AMBIGUOUS_NO_QUERY_NO_IDEMPOTENCY".equals(transportScenario))
            throw new IllegalStateException("U06_BLIND_RESEND_PROHIBITED");
        s.authorityStatus=ACTIVE_PENDING;
        return attempt(effect,transportScenario,createdAt);
    }

    public synchronized Snapshot reconcileStatusQuery(String effect,String resolvedStatus,String createdAt) {
        DeliveryState s=state(effect);
        if(!RECONCILIATION_BLOCKED.equals(s.authorityStatus))
            throw new IllegalStateException("U06_STATUS_QUERY_NOT_REQUIRED");
        if(CONFIRMED.equals(resolvedStatus)) {
            evaluateConfirmation(s,CONFIRMED,"status-query-confirmed",createdAt);
        } else if(NOT_CONFIRMED.equals(resolvedStatus)) {
            s.authorityStatus=RETRYABLE_NOT_CONFIRMED;
            evaluateConfirmation(s,NOT_CONFIRMED,"status-query-not-confirmed",createdAt);
        } else {
            evaluateConfirmation(s,INDETERMINATE,"status-query-indeterminate",createdAt);
        }
        return s.snapshot(false);
    }

    public synchronized Snapshot applyLaterEvidence(String effect,String confirmationStatus,String evidenceRef,String createdAt) {
        DeliveryState s=state(effect);
        String prior=latestConfirmation(s);
        if(CONFIRMED.equals(prior)&&!CONFIRMED.equals(confirmationStatus))
            throw new IllegalStateException("U06_DELIVERY_CONFIRMATION_EVIDENCE_CONFLICT");
        evaluateConfirmation(s,confirmationStatus,evidenceRef,createdAt);
        return s.snapshot(false);
    }

    public synchronized Snapshot expireBeforeSend(String effect) {
        DeliveryState s=state(effect);
        if(!s.attempts.isEmpty())throw new IllegalStateException("U06_EXPIRE_AFTER_SEND_REQUIRES_RECONCILIATION");
        s.intentStatus=CANCELLED_BEFORE_SEND;s.authorityStatus=CANCELLED_TERMINAL;
        return s.snapshot(false);
    }

    public synchronized Snapshot snapshot(String effect){return state(effect).snapshot(true);}
    public synchronized int physicalAttemptCount(String effect){return state(effect).attempts.size();}
    public synchronized int confirmationEvaluationCount(String effect){return state(effect).confirmations.size();}
    public synchronized int receiptCount(String effect){return state(effect).receipts.size();}

    private void addReceipt(DeliveryState s,Attempt a,String status,String at) {
        s.receipts.add(new Receipt(U06Ids.hash("u06receipt",s.deliveryEffectId,a.attemptId,String.valueOf(s.receipts.size()+1)),status,at));
    }

    private void evaluateConfirmation(DeliveryState s,String status,String evidenceRef,String at) {
        String id=U06Ids.hash("u06confirm",s.deliveryEffectId,String.valueOf(s.confirmations.size()+1),req(evidenceRef));
        s.confirmations.add(new Confirmation(id,status,evidenceRef,at));
        if(CONFIRMED.equals(status))s.authorityStatus=CONFIRMED_TERMINAL;
        else if(INDETERMINATE.equals(status))s.authorityStatus=RECONCILIATION_BLOCKED;
    }

    private String latestConfirmation(DeliveryState s) {
        return s.confirmations.isEmpty()?null:s.confirmations.get(s.confirmations.size()-1).status;
    }

    private DeliveryState state(String effect) {
        DeliveryState s=byEffect.get(effect);
        if(s==null)throw new IllegalStateException("U06_DELIVERY_EFFECT_NOT_FOUND");
        return s;
    }

    private static boolean terminal(String status) {
        return CONFIRMED_TERMINAL.equals(status)||NOT_CONFIRMED_TERMINAL.equals(status)||CANCELLED_TERMINAL.equals(status);
    }

    private static String req(String v){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("value required");return v.trim();}

    private static final class DeliveryState {
        String consultationId,selectionEffectId,questionId,contentFingerprint,endpointRef,policyRef;
        String deliveryEffectId,deliveryId,idempotencyKey,intentFingerprint,intentStatus,authorityStatus,createdAt;
        final List<Attempt>attempts=new ArrayList<Attempt>();
        final List<Receipt>receipts=new ArrayList<Receipt>();
        final List<Confirmation>confirmations=new ArrayList<Confirmation>();
        Snapshot snapshot(boolean replay) {
            String latest=confirmations.isEmpty()?null:confirmations.get(confirmations.size()-1).status;
            return new Snapshot(selectionEffectId,deliveryEffectId,deliveryId,idempotencyKey,intentStatus,authorityStatus,
                    latest,attempts.size(),receipts.size(),confirmations.size(),replay,endpointRef,contentFingerprint);
        }
    }
    private static final class Attempt {
        final String attemptId,deliveryId,idempotencyKey,scenario,createdAt;String status;
        Attempt(String a,String d,String i,String s,String at){attemptId=a;deliveryId=d;idempotencyKey=i;scenario=s;createdAt=at;}
    }
    private static final class Receipt {
        final String receiptId,status,createdAt;Receipt(String i,String s,String at){receiptId=i;status=s;createdAt=at;}
    }
    private static final class Confirmation {
        final String evaluationId,status,evidenceRef,createdAt;
        Confirmation(String i,String s,String e,String at){evaluationId=i;status=s;evidenceRef=e;createdAt=at;}
    }

    public static final class Snapshot {
        public final String selectionEffectId,deliveryEffectId,deliveryId,idempotencyKey,intentStatus,authorityStatus,confirmationStatus;
        public final int attemptCount,receiptCount,confirmationEvaluationCount;public final boolean replay;
        public final String endpointRef,contentFingerprint;
        Snapshot(String s,String e,String d,String i,String is,String as,String cs,int ac,int rc,int cc,boolean r,String ep,String fp) {
            selectionEffectId=s;deliveryEffectId=e;deliveryId=d;idempotencyKey=i;intentStatus=is;authorityStatus=as;confirmationStatus=cs;
            attemptCount=ac;receiptCount=rc;confirmationEvaluationCount=cc;replay=r;endpointRef=ep;contentFingerprint=fp;
        }
    }

    public static final class AttemptResult {
        public final String attemptId,deliveryId,idempotencyKey,attemptStatus,authorityStatus,confirmationStatus;
        public final Snapshot snapshot;
        AttemptResult(String a,String d,String i,String s,String as,String cs,Snapshot snap) {
            attemptId=a;deliveryId=d;idempotencyKey=i;attemptStatus=s;authorityStatus=as;confirmationStatus=cs;snapshot=snap;
        }
    }
}
