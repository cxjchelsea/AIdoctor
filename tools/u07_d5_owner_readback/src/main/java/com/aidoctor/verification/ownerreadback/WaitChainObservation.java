package com.aidoctor.verification.ownerreadback;
import java.util.*;
final class WaitChainObservation {
 enum Status { MATCH,ABSENT,STALE,INCONSISTENT,INVALID_INPUT,DENIED,UNAVAILABLE }
 final Status status;final String reason,authorityStatus="UNKNOWN",scopeEvidenceKind,sourceHead,schemaManifestRef;
 final Map<String,Object> payload;
 WaitChainObservation(Status s,String reason,boolean authorized,Map<String,Object> payload){
  if(s==null||reason==null||(s==Status.MATCH)!=(payload!=null))throw new IllegalArgumentException("result shape");
  status=s;this.reason=reason;scopeEvidenceKind=authorized?"TEST_SCOPE":"NONE";sourceHead=OwnerReadbackResources.SOURCE;schemaManifestRef=OwnerReadbackResources.SCHEMA;
  this.payload=payload==null?null:Collections.unmodifiableMap(new LinkedHashMap<>(payload));
 }
}
