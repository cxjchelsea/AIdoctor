package com.aidoctor.verification.sourcebinding;

/** Tool-only result contract. A validated fact is not a committed receipt. */
final class F8Result {
 enum Operational { FINALIZED,HISTORICAL_FOUND,DENIED,INVALID_INPUT,INTEGRITY_CONFLICT,DEFER,RETRYABLE_FAILURE,RECONCILIATION_REQUIRED }
 enum Attempt { NOT_ATTEMPTED,ATTEMPTED }
 enum Durability { NOT_ATTEMPTED,NOT_COMMITTED,COMMITTED,UNKNOWN }
 enum Verdict { ACCEPTED,DUPLICATE,EXPIRED,REJECTED }
 enum Reason { NONE,INPUT,PERMISSION,EVIDENCE,RESOURCE_MISMATCH,INTEGRITY,PENDING_WINNER,PREDICATE_CHANGED,SENTINEL_ROLLBACK,DB_FAILURE,COMMIT_UNKNOWN }
 static final class Receipt {
  final String decisionId,canonicalId,fingerprint,waitKey,winnerId; final Verdict verdict;
  Receipt(String decisionId,String canonicalId,String fingerprint,String waitKey,Verdict verdict,String winnerId){
   if(decisionId==null||canonicalId==null||fingerprint==null||waitKey==null||verdict==null
    ||(verdict==Verdict.DUPLICATE&&winnerId==null)
    ||(verdict==Verdict.ACCEPTED&&winnerId!=null)
    ||(winnerId!=null&&(winnerId.isEmpty()||winnerId.equals(decisionId))))throw new IllegalArgumentException("INVALID_RECEIPT");
   this.decisionId=decisionId;this.canonicalId=canonicalId;this.fingerprint=fingerprint;this.waitKey=waitKey;this.verdict=verdict;this.winnerId=winnerId;
  }
 }
 final Operational operational;final Attempt attempt;final Durability durability;final Reason reason;
 final String queryKey;final Receipt confirmed;
 F8Result(Operational o,Attempt a,Durability d,Reason reason,String query,Receipt receipt){
  if(o==null||a==null||d==null||reason==null)throw new IllegalArgumentException("INVALID_RESULT");
  boolean success=o==Operational.FINALIZED||o==Operational.HISTORICAL_FOUND;
  if(success!=(d==Durability.COMMITTED)||success!=(receipt!=null)
   ||(success&&(query==null||!query.equals(receipt.decisionId)||reason!=Reason.NONE))
   ||(o==Operational.FINALIZED&&a!=Attempt.ATTEMPTED)
   ||(o==Operational.HISTORICAL_FOUND&&a!=Attempt.NOT_ATTEMPTED)
   ||(d==Durability.NOT_ATTEMPTED&&a!=Attempt.NOT_ATTEMPTED)
   ||(d==Durability.NOT_COMMITTED&&a!=Attempt.ATTEMPTED)
   ||((o==Operational.RECONCILIATION_REQUIRED)!=(d==Durability.UNKNOWN))
   ||(d==Durability.UNKNOWN&&(a!=Attempt.ATTEMPTED||query==null||reason!=Reason.COMMIT_UNKNOWN))
   ||(o==Operational.RETRYABLE_FAILURE&&(a!=Attempt.ATTEMPTED||d!=Durability.NOT_COMMITTED))
   ||((o==Operational.DEFER||o==Operational.INVALID_INPUT)&&(a!=Attempt.NOT_ATTEMPTED||d!=Durability.NOT_ATTEMPTED))
   ||(!success&&reason==Reason.NONE))throw new IllegalArgumentException("INVALID_RESULT");
  operational=o;attempt=a;durability=d;this.reason=reason;queryKey=query;confirmed=receipt;
 }
 static F8Result blocked(Operational o,Attempt a,Reason r,String key){return new F8Result(o,a,a==Attempt.ATTEMPTED?Durability.NOT_COMMITTED:Durability.NOT_ATTEMPTED,r,key,null);}
 static F8Result unknown(String key){return new F8Result(Operational.RECONCILIATION_REQUIRED,Attempt.ATTEMPTED,Durability.UNKNOWN,Reason.COMMIT_UNKNOWN,key,null);}
 static F8Result success(Receipt r,boolean historical){return new F8Result(historical?Operational.HISTORICAL_FOUND:Operational.FINALIZED,historical?Attempt.NOT_ATTEMPTED:Attempt.ATTEMPTED,Durability.COMMITTED,Reason.NONE,r.decisionId,r);}
}
