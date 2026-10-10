package com.aidoctor.verification.sourcebinding;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static com.aidoctor.verification.sourcebinding.F8Result.*;
final class F8HistoricalReader {
 enum State { FOUND_MATCH,ABSENT,MISMATCH,INCONSISTENT,UNAVAILABLE,INDETERMINATE }
 static final class Read {final State state;final Receipt receipt;Read(State s,Receipt r){state=s;receipt=r;}}
 final JdbcTemplate db;final SourceBindingVerifier verifier;
 F8HistoricalReader(JdbcTemplate db,SourceBindingVerifier verifier){this.db=db;this.verifier=verifier;}
 Map<String,Object> claim(F8Identity id){
  List<Map<String,Object>> rows=db.queryForList("SELECT * FROM f8_claim WHERE wait_key=? OR (env=? AND profile=? AND tenant=? AND consultation=? AND question=? AND parent_wait=?)",id.waitKey,id.source.field(2),id.source.field(3),id.source.field(4),id.source.field(5),id.source.field(7),id.source.field(9));
  if(rows.isEmpty())return null;if(rows.size()!=1||!id.scope(rows.get(0)))throw corrupt();
  Map<String,Object> c=rows.get(0);long gen=((Number)c.get("generation")).longValue();
  if(c.get("canonical_id")==null&&c.get("decision_id")==null){if(gen!=0)throw corrupt();return c;}
  if(c.get("canonical_id")==null||c.get("decision_id")==null||gen!=1)throw corrupt();
  winner(id,c);return c;
 }
 Map<String,Object> winner(F8Identity id,Map<String,Object> c){
  List<Map<String,Object>> rows=db.queryForList("SELECT * FROM f8_decision WHERE id=?",c.get("decision_id"));
  if(rows.size()!=1)throw corrupt();Map<String,Object> w=rows.get(0);
  if(!id.scope(w)||!"ACCEPTED".equals(w.get("verdict"))||!Objects.equals(c.get("canonical_id"),w.get("canonical_id"))
   ||!F8Identity.CONTRACT.equals(w.get("contract"))||!F8Identity.digest("f8-decision",(String)w.get("canonical_id"),F8Identity.CONTRACT).equals(w.get("id")))throw corrupt();
  List<Map<String,Object>> b=db.queryForList("SELECT b.source_record_ref,b.occurred_at,p.source_token FROM u07_canonical_event_binding b JOIN u07_test_authority_record p ON p.record_id=b.source_record_ref WHERE b.canonical_event_id=?",w.get("canonical_id"));
  if(b.size()!=1)throw corrupt();Map<String,Object> proof=b.get(0);
  SourceBindingVerifier.VerifiedCanonical original=verifier.verify(new SourceBindingAdapter.Request((String)w.get("canonical_id"),(String)proof.get("source_token"),(String)proof.get("source_record_ref"),java.time.Instant.parse((String)proof.get("occurred_at"))),new SourceBindingAdapter.Scope(id.source.field(4),id.source.field(5),id.source.field(6)));
  if(!original.fingerprint.equals(w.get("fingerprint"))||!new F8Identity(original).waitKey.equals(id.waitKey)||!Objects.equals(original.field(16),w.get("digest")))throw corrupt();
  return w;
 }
 Map<String,Object> applied(F8Identity id,Map<String,Object> c){
  if(c==null||c.get("decision_id")==null)return null;Map<String,Object> w=winner(id,c);
  List<Map<String,Object>> rows=db.queryForList("SELECT * FROM f8_applied WHERE decision_id=?",c.get("decision_id"));
  if(rows.isEmpty())return null;if(rows.size()!=1)throw corrupt();Map<String,Object> a=rows.get(0);
  if(!F8OwnerReader.issuer(a)||!Objects.equals(a.get("wait_key"),id.waitKey)||!Objects.equals(a.get("canonical_id"),c.get("canonical_id"))||!Objects.equals(a.get("digest"),w.get("digest"))||((Number)a.get("generation")).longValue()!=1)throw corrupt();return a;
 }
 Read read(F8Identity id){
  try{
   Map<String,Object> c=claim(id);
   List<Map<String,Object>> rows=db.queryForList("SELECT * FROM f8_decision WHERE id=? OR (canonical_id=? AND contract=?)",id.decisionId,id.source.canonicalId,F8Identity.CONTRACT);
   if(rows.isEmpty())return new Read(State.ABSENT,null);
   if(rows.size()!=1)throw corrupt();Map<String,Object> r=rows.get(0);
   if(!id.scope(r)||!id.decisionId.equals(r.get("id"))||!id.source.canonicalId.equals(r.get("canonical_id"))||!id.source.fingerprint.equals(r.get("fingerprint"))||!Objects.equals(id.source.field(16),r.get("digest"))||!F8Identity.CONTRACT.equals(r.get("contract"))||!Objects.equals(r.get("sampled_at"),r.get("predicate_at")))throw corrupt();
   Verdict v=Verdict.valueOf((String)r.get("verdict"));boolean own=c!=null&&id.decisionId.equals(c.get("decision_id"));
   if(v==Verdict.ACCEPTED&&!own||v!=Verdict.ACCEPTED&&own)throw corrupt();
   if(v==Verdict.DUPLICATE){Map<String,Object> a=applied(id,c);if(a==null||!Objects.equals(r.get("winner_id"),c.get("decision_id"))||!Objects.equals(a.get("digest"),id.source.field(16)))throw corrupt();}
   return new Read(State.FOUND_MATCH,new Receipt(id.decisionId,id.source.canonicalId,id.source.fingerprint,id.waitKey,v,(String)r.get("winner_id")));
  }catch(F8OwnerReader.Halt|SourceBindingAdapter.Block|IllegalArgumentException e){return new Read(State.INCONSISTENT,null);}
 }
 static F8OwnerReader.Halt corrupt(){return new F8OwnerReader.Halt(Operational.INTEGRITY_CONFLICT,Reason.INTEGRITY);}
}
