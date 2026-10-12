package com.aidoctor.verification.ownerreadback;
import java.util.Objects;
interface ScopeAuthorizationPort {
 Decision evaluate(OwnerReadbackRequest request);
 enum Kind { ALLOW,DENY,UNAVAILABLE }
 final class Decision {final Kind kind;final Attestation evidence;Decision(Kind k,Attestation e){kind=k;evidence=e;}}
 final class Attestation {
  final String schemaVersion,evidenceKind,profile,fixtureManifestRef,schemaManifestRef,consultationId,threadId,runId,checkpointId,testTenantId,testActorId;
  Attestation(String v,String k,String p,String f,String s,String c,String t,String r,String cp,String tenant,String actor){schemaVersion=v;evidenceKind=k;profile=p;fixtureManifestRef=f;schemaManifestRef=s;consultationId=c;threadId=t;runId=r;checkpointId=cp;testTenantId=tenant;testActorId=actor;}
  boolean matches(OwnerReadbackRequest r){return "O1_TEST_SCOPE_V1".equals(schemaVersion)&&"TEST_SCOPE".equals(evidenceKind)&&OwnerReadbackResources.PROFILE.equals(profile)&&OwnerReadbackResources.FIXTURE.equals(fixtureManifestRef)&&OwnerReadbackResources.SCHEMA.equals(schemaManifestRef)&&Objects.equals(consultationId,r.consultationId)&&Objects.equals(threadId,r.threadId)&&Objects.equals(runId,r.runId)&&Objects.equals(checkpointId,r.checkpointId)&&OwnerReadbackRequest.id(testTenantId)&&OwnerReadbackRequest.id(testActorId);}
 }
}
