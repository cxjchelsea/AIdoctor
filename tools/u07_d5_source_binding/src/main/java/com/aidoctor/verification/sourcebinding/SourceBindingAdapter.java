package com.aidoctor.verification.sourcebinding;

import com.aidoctor.diagnosis.runtime.foundation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import org.springframework.jdbc.datasource.DataSourceUtils;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Owned outer transaction. Package-private, no Boot bean or production route. */
final class SourceBindingAdapter {
    enum Status { STORED, REATTACHED, TARGET_REATTACHED, DENIED, INVALID_INPUT, INTEGRITY_CONFLICT,
        UNAVAILABLE, INCONSISTENT, UNKNOWN, ABSENT, UNRESOLVED_TARGET }
    enum Attempt { NOT_ATTEMPTED, ATTEMPTED }
    enum Durability { NOT_ATTEMPTED, NOT_COMMITTED, UNKNOWN, COMMITTED }
    static final class Result {
        final Status status; final Attempt attempt; final Durability durability;
        final String canonicalId, queryKey, targetId;
        private Result(Status status, Attempt attempt, Durability durability, String canonicalId, String queryKey, String targetId) {
            boolean success=status==Status.STORED||status==Status.REATTACHED||status==Status.TARGET_REATTACHED;
            if(success != (durability==Durability.COMMITTED) || success != (canonicalId!=null)
                || (durability==Durability.NOT_ATTEMPTED && attempt!=Attempt.NOT_ATTEMPTED)
                || (durability==Durability.UNKNOWN && (status!=Status.UNKNOWN || queryKey==null))
                || (!success && targetId!=null)) throw new IllegalArgumentException("INVALID_RESULT");
            this.status=status;this.attempt=attempt;this.durability=durability;this.canonicalId=canonicalId;this.queryKey=queryKey;this.targetId=targetId;
        }
        static Result committed(Status s,String canonical,String key,String target,Attempt attempt) {
            return new Result(s,attempt,Durability.COMMITTED,canonical,key,target);
        }
        static Result blocked(Status s,Attempt a) {
            return new Result(s,a,a==Attempt.ATTEMPTED?Durability.NOT_COMMITTED:Durability.NOT_ATTEMPTED,null,null,null);
        }
        static Result unknown(String key) {return new Result(Status.UNKNOWN,Attempt.ATTEMPTED,Durability.UNKNOWN,null,key,null);}
    }
    static final class Scope {
        final String tenant, consultation, actor;
        Scope(String tenant,String consultation,String actor) {this.tenant=BindingCodec.id(tenant);this.consultation=BindingCodec.id(consultation);this.actor=BindingCodec.id(actor);}
    }
    static final class Request {
        final String eventId, token, sourceRef; final Instant occurred;
        Request(String eventId,String token,String sourceRef,Instant occurred) {this.eventId=eventId;this.token=token;this.sourceRef=sourceRef;this.occurred=occurred;}
    }
    interface Probe { void at(String phase); }
    private static final Probe NO_PROBE = phase -> {};
    private final DataSource ds; private final JdbcTemplate jdbc; private final TransactionTemplate tx;
    private final CanonicalBusinessEventLedger ledger; private final CanonicalBusinessEventRepository events;
    private final EntityManager em; private final TestAuthority.CipherBox box; private final Probe probe;
    SourceBindingAdapter(DataSource ds,PlatformTransactionManager manager,CanonicalBusinessEventLedger ledger,
                         CanonicalBusinessEventRepository events,EntityManager em,TestAuthority.CipherBox box,String profile) {
        this(ds,manager,ledger,events,em,box,profile,NO_PROBE);
    }
    SourceBindingAdapter(DataSource ds,PlatformTransactionManager manager,CanonicalBusinessEventLedger ledger,
                         CanonicalBusinessEventRepository events,EntityManager em,TestAuthority.CipherBox box,String profile,Probe probe) {
        if(!BindingCodec.PROFILE.equals(profile))throw new IllegalArgumentException("isolated profile only");
        this.ds=ds;this.jdbc=new JdbcTemplate(ds);this.ledger=ledger;this.events=events;this.em=em;this.box=box;this.probe=probe;
        tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);tx.setTimeout(15);
    }
    private void ownedEntry() {
        if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("outer transaction already active");
    }
    private void guard() {
        if(!TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.hasResource(ds)
            || !em.isJoinedToTransaction())throw new Block(Status.UNAVAILABLE);
        Connection c=DataSourceUtils.getConnection(ds);
        try {if(c.getAutoCommit()||!DataSourceUtils.isConnectionTransactional(c,ds))throw new Block(Status.UNAVAILABLE);}
        catch(SQLException e){throw new Block(Status.UNAVAILABLE);}finally{DataSourceUtils.releaseConnection(c,ds);}
    }
    Result admit(Request r,Scope scope) {
        ownedEntry();
        try {if(r==null||scope==null||r.occurred==null)throw new IllegalArgumentException();BindingCodec.id(r.eventId);BindingCodec.id(r.token);BindingCodec.id(r.sourceRef);}
        catch(IllegalArgumentException e){return Result.blocked(Status.INVALID_INPUT,Attempt.NOT_ATTEMPTED);}
        // Validate authority/payload before any identity mutation; recheck in the write transaction.
        final Source source;
        try {source=tx.execute(s->{guard();return source(r,scope);});}
        catch(Block b){return Result.blocked(b.status,Attempt.NOT_ATTEMPTED);}
        catch(RuntimeException e){return Result.blocked(Status.UNAVAILABLE,Attempt.NOT_ATTEMPTED);}
        try {
            Result provisional=tx.execute(s->{guard();Source current=source(r,scope);lockEpoch(current,scope);
                if(!Arrays.equals(source.frame,current.frame))throw new Block(Status.INTEGRITY_CONFLICT);
                Result result=write(r,current);em.flush();if(s.isRollbackOnly())throw new Block(Status.UNAVAILABLE);
                probe.at("before_commit");return result;});
            // This result was private until TransactionTemplate finished the outer commit.
            return provisional;
        }catch(Block b){return Result.blocked(b.status,Attempt.ATTEMPTED);}
        catch(RuntimeException e){
            if(duplicate(e))return reconcile(r,scope,source.key);
            // A non-duplicate commit exception may be ambiguous; never publish the provisional success.
            return Result.unknown(source.key);
        }
    }
    Result read(Request r,Scope scope) {
        ownedEntry();
        try {if(r==null||scope==null||r.occurred==null)throw new IllegalArgumentException();BindingCodec.id(r.eventId);BindingCodec.id(r.token);BindingCodec.id(r.sourceRef);
            return tx.execute(s->{guard();Source source=source(r,scope);return existing(r,source,false);});
        }catch(Block b){return Result.blocked(b.status,Attempt.NOT_ATTEMPTED);}
        catch(IllegalArgumentException e){return Result.blocked(Status.INVALID_INPUT,Attempt.NOT_ATTEMPTED);}
        catch(RuntimeException e){return Result.blocked(Status.UNAVAILABLE,Attempt.NOT_ATTEMPTED);}
    }
    private Result reconcile(Request r,Scope scope,String key) {
        // Called only after the failed template completed/rolled back. No same-transaction retry.
        try {return tx.execute(s->{guard();probe.at("fresh_winner_read");return existing(r,source(r,scope),false);});}
        catch(Block b){return Result.blocked(b.status,Attempt.ATTEMPTED);}
        catch(RuntimeException e){return Result.unknown(key);}
    }
    private Source source(Request r,Scope scope) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM u07_test_authority_record WHERE record_id=?",r.sourceRef);
        if(rows.size()!=1)throw new Block(Status.DENIED);
        Map<String,Object> row=rows.get(0);
        if(!"SOURCE".equals(row.get("record_type"))||!TestAuthority.ISSUER.equals(row.get("issuer_id"))
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
    private void lockEpoch(Source source,Scope scope) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT epoch,allowed FROM u07_test_permission_epoch WHERE tenant_id=? LOCK IN SHARE MODE",scope.tenant);
        if(rows.size()!=1||((Number)rows.get(0).get("allowed")).intValue()!=1||((Number)rows.get(0).get("epoch")).longValue()!=source.epoch)
            throw new Block(Status.DENIED);
    }
    private Result write(Request r,Source source) {
        Optional<CanonicalBusinessEventRecord> byId=events.findById(r.eventId);
        Optional<CanonicalBusinessEventRecord> byKey=events.findByIdempotencyKey(source.key);
        if(byId.isPresent()||byKey.isPresent())return existing(r,source,true);
        if(!r.occurred.toString().equals(source.occurred))throw new Block(Status.INTEGRITY_CONFLICT);
        if("RESUME_REQUEST".equals(source.f[1]))target(source);
        CanonicalBusinessEventRecord event=ledger.resolveOrCreate(r.eventId,source.f[5],source.f[1],source.key,source.f[20]);
        probe.at("pending_insert");em.flush();probe.at("after_foundation_flush");
        if(!event.getEventId().equals(r.eventId))throw new Block(Status.INCONSISTENT);
        jdbc.update("INSERT INTO u07_canonical_event_binding (canonical_event_id,storage_idempotency_key,event_type,consultation_id,tenant_id,binding_fingerprint,payload_digest,canonical_binding_bytes,synthetic_answer_bytes,answer_payload_digest,key_ref,source_record_ref,occurred_at,target_answer_event_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            event.getEventId(),source.key,source.f[1],source.f[5],source.f[4],BindingCodec.hash(source.frame),source.f[20],source.frame,
            source.cipher,source.f[16],source.cipher==null?null:TestAuthority.KEY_REF,r.sourceRef,source.occurred,source.f[17]);
        probe.at("after_binding_insert");
        return Result.committed("RESUME_REQUEST".equals(source.f[1])?Status.TARGET_REATTACHED:Status.STORED,event.getEventId(),source.key,source.f[17],Attempt.ATTEMPTED);
    }
    private Result existing(Request r,Source source,boolean writing) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT e.event_id,e.consultation_id AS owner_consultation,e.event_type AS owner_type,e.idempotency_key AS owner_key,e.payload_digest AS owner_digest,b.* FROM canonical_business_event e LEFT JOIN u07_canonical_event_binding b ON b.canonical_event_id=e.event_id WHERE e.event_id=? OR e.idempotency_key=?",r.eventId,source.key);
        if(rows.isEmpty())throw new Block(Status.ABSENT);
        if(rows.size()!=1)throw new Block(Status.INTEGRITY_CONFLICT);
        Map<String,Object> row=rows.get(0);
        if(!source.f[5].equals(row.get("owner_consultation"))||!source.f[1].equals(row.get("owner_type"))
            ||!source.key.equals(row.get("owner_key"))||!source.f[20].equals(row.get("owner_digest")))throw new Block(Status.INTEGRITY_CONFLICT);
        checkBinding(row,source);
        String canonical=(String)row.get("event_id");
        if(canonical.equals(r.eventId)&&!r.occurred.toString().equals(row.get("occurred_at")))throw new Block(Status.INTEGRITY_CONFLICT);
        if("RESUME_REQUEST".equals(source.f[1]))target(source);
        return Result.committed("RESUME_REQUEST".equals(source.f[1])?Status.TARGET_REATTACHED:Status.REATTACHED,canonical,source.key,source.f[17],writing?Attempt.ATTEMPTED:Attempt.NOT_ATTEMPTED);
    }
    private void checkBinding(Map<String,Object> row,Source source) {
        if(row.get("canonical_event_id")==null)throw new Block(Status.INCONSISTENT);
        try {
            byte[] b=(byte[])row.get("canonical_binding_bytes");String[] f=BindingCodec.decode(b);
            if(!Arrays.equals(b,source.frame)||!BindingCodec.hash(b).equals(row.get("binding_fingerprint"))
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
            if(!"SOURCE".equals(proof.get("record_type"))||!TestAuthority.ISSUER.equals(proof.get("issuer_id"))
                ||!TestAuthority.POLICY.equals(proof.get("policy_id"))||!TestAuthority.MANIFEST.equals(proof.get("manifest_digest"))
                ||!f[4].equals(proof.get("tenant_id"))||!Arrays.equals(b,(byte[])proof.get("binding_bytes"))
                ||!BindingCodec.hash(b).equals(proof.get("binding_fingerprint"))||!source.key.equals(proof.get("storage_key"))
                ||!Objects.equals(row.get("occurred_at"),proof.get("occurred_at")))throw new Block(Status.INTEGRITY_CONFLICT);
            if("USER_ANSWER".equals(f[1])&&!BindingCodec.hash(box.decrypt((byte[])proof.get("answer_cipher"),(String)proof.get("key_ref"))).equals(f[16]))throw new Block(Status.INTEGRITY_CONFLICT);
        }catch(IllegalArgumentException e){throw new Block(Status.INTEGRITY_CONFLICT);}
    }
    private void target(Source source) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT e.event_id,e.consultation_id AS owner_consultation,e.event_type AS owner_type,e.idempotency_key AS owner_key,e.payload_digest AS owner_digest,b.* FROM canonical_business_event e LEFT JOIN u07_canonical_event_binding b ON b.canonical_event_id=e.event_id WHERE e.event_id=?",source.f[17]);
        if(rows.size()!=1)throw new Block(Status.UNRESOLVED_TARGET);
        Map<String,Object> row=rows.get(0);
        if(!"USER_ANSWER".equals(row.get("owner_type"))||!source.f[5].equals(row.get("owner_consultation")))throw new Block(Status.DENIED);
        if(row.get("canonical_binding_bytes")==null)throw new Block(Status.INCONSISTENT);
        try {
            String[] f=BindingCodec.decode((byte[])row.get("canonical_binding_bytes"));
            for(int i:new int[]{2,3,4,5,6,7,8,9,10,11,12,13,14,18,19})if(!Objects.equals(source.f[i],f[i]))throw new Block(Status.INTEGRITY_CONFLICT);
            Source target=new Source(f,(byte[])row.get("canonical_binding_bytes"),(byte[])row.get("synthetic_answer_bytes"),(String)row.get("owner_key"),(String)row.get("occurred_at"),source.epoch);
            if(!BindingCodec.payload(f).equals(row.get("owner_digest")))throw new Block(Status.INTEGRITY_CONFLICT);
            checkBinding(row,target);
        }catch(IllegalArgumentException e){throw new Block(Status.INTEGRITY_CONFLICT);}
    }
    private static boolean duplicate(Throwable e) {
        for(Throwable t=e;t!=null;t=t.getCause())if(t instanceof SQLException) {
            SQLException s=(SQLException)t;if(s.getErrorCode()==1062&&"23000".equals(s.getSQLState()))return true;
        }return false;
    }
    private static final class Source {
        final String[] f;final byte[] frame,cipher;final String key,occurred;final long epoch;
        Source(String[] f,byte[] frame,byte[] cipher,String key,String occurred,long epoch){this.f=f;this.frame=frame;this.cipher=cipher;this.key=key;this.occurred=occurred;this.epoch=epoch;}
    }
    static final class Block extends RuntimeException {
        final Status status;Block(Status status){super(status.name());this.status=status;}
    }
}
