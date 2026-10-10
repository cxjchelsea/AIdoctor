package com.aidoctor.verification.sourcebinding;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static com.aidoctor.verification.sourcebinding.F8Result.*;
final class F8OwnerReader {
 static final class Halt extends RuntimeException {final Operational operational;final Reason reason;Halt(Operational o,Reason r){super(r.name());operational=o;reason=r;}}
 static final class Snapshot {
  final Map<String,Object> consultation;final Map<String,Map<String,Object>> rows;final long epoch;
  Snapshot(Map<String,Map<String,Object>> r,long epoch){rows=r;consultation=r.get("CONSULTATION");this.epoch=epoch;}
 }
 final JdbcTemplate db;
 F8OwnerReader(JdbcTemplate db){this.db=db;}
 void lock(F8Identity id){
  if(db.queryForList("SELECT scope_key FROM f8_guard WHERE scope_key=? FOR UPDATE",id.scopeKey).size()!=1)throw new Halt(Operational.DEFER,Reason.EVIDENCE);
 }
 long permission(F8Identity id,boolean finalize){
  List<Map<String,Object>> rows=db.queryForList("SELECT * FROM f8_action WHERE scope_key=? LOCK IN SHARE MODE",id.scopeKey);
  if(rows.size()!=1)throw new Halt(Operational.DENIED,Reason.PERMISSION);
  Map<String,Object> r=rows.get(0);
  if(!Objects.equals(r.get("tenant"),id.source.field(4))||!Objects.equals(r.get("consultation"),id.source.field(5))||!Objects.equals(r.get("actor"),id.source.field(6))||!issuer(r))throw new Halt(Operational.DENIED,Reason.PERMISSION);
  if(!one(r.get(finalize?"finalize_allowed":"read_allowed")))throw new Halt(Operational.DENIED,Reason.PERMISSION);
  return ((Number)r.get("epoch")).longValue();
 }
 Snapshot read(F8Identity id,long epoch){
  Map<String,String> refs=new TreeMap<>();refs.put(id.source.field(10),"ISSUANCE");refs.put(id.source.field(7),"QUESTION");refs.put(id.source.field(8),"PENDING");refs.put(id.source.field(5),"CONSULTATION");
  if(refs.size()!=4)throw new Halt(Operational.INTEGRITY_CONFLICT,Reason.INTEGRITY);
  Map<String,Map<String,Object>> found=new HashMap<>();
  for(Map.Entry<String,String> e:refs.entrySet()){
   List<Map<String,Object>> rows=db.queryForList("SELECT * FROM f8_owner WHERE ref=? LOCK IN SHARE MODE",e.getKey());
   if(rows.size()!=1)throw new Halt(Operational.DEFER,Reason.EVIDENCE);Map<String,Object> r=rows.get(0);
   if(!e.getValue().equals(r.get("kind"))||!issuer(r))throw new Halt(Operational.DEFER,Reason.EVIDENCE);
   for(String[] pair:new String[][]{{"env","2"},{"profile","3"},{"tenant","4"},{"consultation","5"},{"actor","6"},{"question","7"},{"parent_wait","9"},{"pending_ref","8"},{"eligibility_ref","10"}})
    if(!Objects.equals(r.get(pair[0]),id.source.field(Integer.parseInt(pair[1]))))throw new Halt(Operational.INTEGRITY_CONFLICT,Reason.INTEGRITY);
   String state=(String)r.get("state");
   String legal=e.getValue().equals("CONSULTATION")?"WAITING,CANCELLED,EXPIRED,TERMINAL":e.getValue().equals("ISSUANCE")?"DELIVERED,PENDING":e.getValue().equals("QUESTION")?"ASKED,CLOSED":"WAITING,CLOSED";
   if(!Arrays.asList(legal.split(",")).contains(state)||((Number)r.get("version")).longValue()<1)throw new Halt(Operational.DEFER,Reason.EVIDENCE);
   found.put(e.getValue(),r);
  }
  return new Snapshot(found,epoch);
 }
 static boolean one(Object o){return o instanceof Boolean?(Boolean)o:o instanceof Number&&((Number)o).intValue()==1;}
 static boolean issuer(Map<String,Object> r){return F8Identity.ISSUER.equals(r.get("issuer"))&&F8Identity.POLICY.equals(r.get("policy"))&&F8Identity.MANIFEST.equals(r.get("manifest"));}
}
