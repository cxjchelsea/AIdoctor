package com.aidoctor.verification.sourcebinding;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static com.aidoctor.verification.sourcebinding.SourceBindingAdapter.*;
/** Shared read-only validation. No transaction creation, no durability receipt. Tool modules only. */
final class SourceBindingVerifier {
 private final JdbcTemplate jdbc; private final EntityManager em; private final TestAuthority.CipherBox box;
 SourceBindingVerifier(JdbcTemplate jdbc,EntityManager em,TestAuthority.CipherBox box){this.jdbc=jdbc;this.em=em;this.box=box;}
 static final class VerifiedCanonical {
  final String canonicalId, fingerprint; private final String[] fields;
  private VerifiedCanonical(String id,String[] fields,byte[] frame){this.canonicalId=id;this.fields=fields.clone();fingerprint=BindingCodec.hash(frame);}
  String field(int i){return fields[i];}
 }
 VerifiedCanonical verify(Request r,Scope scope){
  if(!TransactionSynchronizationManager.isActualTransactionActive()||!em.isJoinedToTransaction())throw new Block(Status.UNAVAILABLE);
  if(r==null||scope==null||r.occurred==null)throw new IllegalArgumentException("input");
  BindingCodec.id(r.eventId);BindingCodec.id(r.token);BindingCodec.id(r.sourceRef);
  Source proof=source(r,scope);
  List<Map<String,Object>> rows=rows("e.event_id=? OR e.idempotency_key=?",r.eventId,proof.key);
  if(rows.isEmpty())throw new Block(Status.ABSENT);if(rows.size()!=1)throw new Block(Status.INTEGRITY_CONFLICT);
  Map<String,Object> row=rows.get(0);checkBinding(row,proof);
  if(r.eventId.equals(row.get("event_id"))&&!r.occurred.toString().equals(row.get("occurred_at")))throw new Block(Status.INTEGRITY_CONFLICT);
  if("RESUME_REQUEST".equals(proof.f[1])){
   target(proof); row=rows("e.event_id=?",proof.f[17]).get(0);
  }
  String[] f=BindingCodec.decode((byte[])row.get("canonical_binding_bytes"));
  if(!"USER_ANSWER".equals(f[1]))throw new Block(Status.INTEGRITY_CONFLICT);
  return new VerifiedCanonical((String)row.get("event_id"),f,(byte[])row.get("canonical_binding_bytes"));
 }
 private List<Map<String,Object>> rows(String where,Object...args){return jdbc.queryForList("SELECT e.event_id,e.consultation_id AS owner_consultation,e.event_type AS owner_type,e.idempotency_key AS owner_key,e.payload_digest AS owner_digest,b.* FROM canonical_business_event e LEFT JOIN u07_canonical_event_binding b ON b.canonical_event_id=e.event_id WHERE "+where,args);}
    Source source(Request r,Scope scope) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM u07_test_authority_record WHERE record_id=?",r.sourceRef);
        if(rows.size()!=1)throw new Block(Status.DENIED);
        Map<String,Object> row=rows.get(0);
        if(!"SOURCE".equals(row.get("record_type"))||!"SYNTHETIC_ADMISSION".equals(row.get("action_id"))||!TestAuthority.ISSUER.equals(row.get("issuer_id"))
            || !TestAuthority.POLICY.equals(row.get("policy_id"))||!TestAuthority.MANIFEST.equals(row.get("manifest_digest")))throw new Block(Status.DENIED);
        try {
            byte[] frame=(byte[])row.get("binding_bytes");String[] f=BindingCodec.decode(frame);
            if(!scope.tenant.equals(f[4])||!scope.consultation.equals(f[5])||!scope.actor.equals(f[6])
                ||!scope.tenant.equals(row.get("tenant_id")))throw new Block(Status.DENIED);
            String key=BindingCodec.key(f,r.token);
            if(!r.token.equals(row.get("source_token"))||!key.equals(row.get("storage_key"))
                ||!BindingCodec.hash(frame).equals(row.get("binding_fingerprint"))||!BindingCodec.payload(f).equals(f[20]))throw new Block(Status.INTEGRITY_CONFLICT);
            byte[] cipher=(byte[])row.get("answer_cipher");
            if("USER_ANSWER".equals(f[1])) {
                byte[] answer=box.decrypt(cipher,(String)row.get("key_ref"));
                if(!BindingCodec.hash(answer).equals(f[16])||!new String(answer,StandardCharsets.UTF_8).startsWith("synthetic-answer:")
                    ||!("u07db:v1:"+key+":answer").equals(f[15]))throw new Block(Status.INTEGRITY_CONFLICT);
            }else if(cipher!=null||row.get("key_ref")!=null)throw new Block(Status.INTEGRITY_CONFLICT);
            return new Source(f,frame,cipher,key,(String)row.get("occurred_at"),((Number)row.get("permission_epoch")).longValue());
        }catch(IllegalArgumentException e){throw new Block(Status.INTEGRITY_CONFLICT);}
    }
    void checkBinding(Map<String,Object> row,Source source) {
        if(row.get("canonical_event_id")==null)throw new Block(Status.INCONSISTENT);
        try {
            byte[] b=(byte[])row.get("canonical_binding_bytes");String[] f=BindingCodec.decode(b);
            if(!f[1].equals(row.get("owner_type"))||!f[5].equals(row.get("owner_consultation"))
                ||!source.key.equals(row.get("owner_key"))||!f[20].equals(row.get("owner_digest"))
                ||!Objects.equals(row.get("event_id"),row.get("canonical_event_id"))
                ||!BindingCodec.payload(f).equals(f[20])
                ||!Arrays.equals(b,source.frame)||!BindingCodec.hash(b).equals(row.get("binding_fingerprint"))
                ||!source.key.equals(row.get("storage_idempotency_key"))||!source.f[20].equals(row.get("payload_digest"))
                ||!f[1].equals(row.get("event_type"))||!f[5].equals(row.get("consultation_id"))||!f[4].equals(row.get("tenant_id"))
                ||!Objects.equals(f[17],row.get("target_answer_event_id")))throw new Block(Status.INTEGRITY_CONFLICT);
            if("USER_ANSWER".equals(f[1])) {
                byte[] plain=box.decrypt((byte[])row.get("synthetic_answer_bytes"),(String)row.get("key_ref"));
                if(!BindingCodec.hash(plain).equals(f[16])||!f[16].equals(row.get("answer_payload_digest")))throw new Block(Status.INTEGRITY_CONFLICT);
            }else if(row.get("synthetic_answer_bytes")!=null||row.get("answer_payload_digest")!=null||row.get("key_ref")!=null)throw new Block(Status.INTEGRITY_CONFLICT);
            if(row.get("source_record_ref")==null)throw new Block(Status.INCONSISTENT);
            List<Map<String,Object>> original=jdbc.queryForList("SELECT * FROM u07_test_authority_record WHERE record_id=?",row.get("source_record_ref"));
            if(original.size()!=1)throw new Block(Status.INCONSISTENT);
            Map<String,Object> proof=original.get(0);
            if(!"SOURCE".equals(proof.get("record_type"))||!"SYNTHETIC_ADMISSION".equals(proof.get("action_id"))||!TestAuthority.ISSUER.equals(proof.get("issuer_id"))
                ||!TestAuthority.POLICY.equals(proof.get("policy_id"))||!TestAuthority.MANIFEST.equals(proof.get("manifest_digest"))
                ||!f[4].equals(proof.get("tenant_id"))||!Arrays.equals(b,(byte[])proof.get("binding_bytes"))
                ||!BindingCodec.hash(b).equals(proof.get("binding_fingerprint"))||!source.key.equals(proof.get("storage_key"))
                ||!Objects.equals(row.get("occurred_at"),proof.get("occurred_at")))throw new Block(Status.INTEGRITY_CONFLICT);
            String originalKey=BindingCodec.key(f,(String)proof.get("source_token"));
            if(!originalKey.equals(source.key))throw new Block(Status.INTEGRITY_CONFLICT);
            if("USER_ANSWER".equals(f[1])) {
                if(!("u07db:v1:"+originalKey+":answer").equals(f[15])
                    ||!BindingCodec.hash(box.decrypt((byte[])proof.get("answer_cipher"),(String)proof.get("key_ref"))).equals(f[16]))throw new Block(Status.INTEGRITY_CONFLICT);
            }else if(proof.get("answer_cipher")!=null||proof.get("key_ref")!=null)throw new Block(Status.INTEGRITY_CONFLICT);
        }catch(IllegalArgumentException e){throw new Block(Status.INTEGRITY_CONFLICT);}
    }
    void target(Source source) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT e.event_id,e.consultation_id AS owner_consultation,e.event_type AS owner_type,e.idempotency_key AS owner_key,e.payload_digest AS owner_digest,b.* FROM canonical_business_event e LEFT JOIN u07_canonical_event_binding b ON b.canonical_event_id=e.event_id WHERE e.event_id=?",source.f[17]);
        if(rows.size()!=1)throw new Block(Status.UNRESOLVED_TARGET);
        Map<String,Object> row=rows.get(0);
        if(!"USER_ANSWER".equals(row.get("owner_type"))||!source.f[5].equals(row.get("owner_consultation")))throw new Block(Status.DENIED);
        if(row.get("canonical_binding_bytes")==null)throw new Block(Status.INCONSISTENT);
        try {
            String[] f=BindingCodec.decode((byte[])row.get("canonical_binding_bytes"));
            if(!"USER_ANSWER".equals(f[1])||f[17]!=null||row.get("synthetic_answer_bytes")==null)
                throw new Block(Status.INTEGRITY_CONFLICT);
            for(int i:new int[]{2,3,4,5,6,7,8,9,10,11,12,13,14,18,19})if(!Objects.equals(source.f[i],f[i]))throw new Block(Status.INTEGRITY_CONFLICT);
            Source target=new Source(f,(byte[])row.get("canonical_binding_bytes"),(byte[])row.get("synthetic_answer_bytes"),(String)row.get("owner_key"),(String)row.get("occurred_at"),source.epoch);
            if(!BindingCodec.payload(f).equals(row.get("owner_digest")))throw new Block(Status.INTEGRITY_CONFLICT);
            checkBinding(row,target);
        }catch(IllegalArgumentException e){throw new Block(Status.INTEGRITY_CONFLICT);}
    }
}
