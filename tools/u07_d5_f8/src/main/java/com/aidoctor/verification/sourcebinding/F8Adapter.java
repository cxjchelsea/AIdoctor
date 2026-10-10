package com.aidoctor.verification.sourcebinding;
import com.aidoctor.diagnosis.runtime.foundation.*;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import org.hibernate.Session;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import static com.aidoctor.verification.sourcebinding.F8Result.*;
/** Synthetic isolated adapter, not a production bean. Always owns the outer transaction. */
final class F8Adapter {
 interface Probe {void at(String phase);}
 static final class Context {F8Identity id;boolean attempted,commitStarted;int completion=TransactionSynchronization.STATUS_UNKNOWN;}
 private final DataSource ds;private final EntityManager em;private final TransactionTemplate tx;
 private final JdbcTemplate db;private final NamedParameterJdbcTemplate named;
 private final SourceBindingVerifier verifier;private final F8OwnerReader owners;private final F8HistoricalReader history;
 private final LocalDateTime fixedClock;private final Probe probe;private final String sql;private final boolean managerMatches;
 F8Adapter(DataSource ds,PlatformTransactionManager manager,EntityManager em,TestAuthority.CipherBox box,String profile){this(ds,manager,em,box,profile,null,p->{});}
 F8Adapter(DataSource ds,PlatformTransactionManager manager,EntityManager em,TestAuthority.CipherBox box,String profile,LocalDateTime clock,Probe probe){
  if(!BindingCodec.PROFILE.equals(profile))throw new IllegalArgumentException("isolated profile only");
  this.ds=ds;this.em=em;db=new JdbcTemplate(ds);named=new NamedParameterJdbcTemplate(ds);this.probe=probe;fixedClock=clock;
  managerMatches=manager instanceof JpaTransactionManager&&((JpaTransactionManager)manager).getDataSource()==ds;
  tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);tx.setTimeout(15);
  verifier=new SourceBindingVerifier(db,em,box);owners=new F8OwnerReader(db);history=new F8HistoricalReader(db,verifier);
  try{sql=org.springframework.util.StreamUtils.copyToString(getClass().getResourceAsStream("/adapter-finalize.sql"),java.nio.charset.StandardCharsets.UTF_8);}catch(java.io.IOException e){throw new IllegalStateException(e);}
 }
 private void entry(){if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("outer transaction already active");}
 private void guard(){
  if(!managerMatches||!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(ds)||!em.isJoinedToTransaction())throw new F8OwnerReader.Halt(Operational.DEFER,Reason.RESOURCE_MISMATCH);
  Connection c=DataSourceUtils.getConnection(ds);
  try{if(c.getAutoCommit()||!DataSourceUtils.isConnectionTransactional(c,ds))throw new F8OwnerReader.Halt(Operational.DEFER,Reason.RESOURCE_MISMATCH);
   long jpa=em.unwrap(Session.class).doReturningWork(conn->{try(Statement st=conn.createStatement();ResultSet r=st.executeQuery("SELECT CONNECTION_ID()")){r.next();return r.getLong(1);}});
   if(jpa!=db.queryForObject("SELECT CONNECTION_ID()",Long.class))throw new F8OwnerReader.Halt(Operational.DEFER,Reason.RESOURCE_MISMATCH);
  }catch(SQLException e){throw new F8OwnerReader.Halt(Operational.DEFER,Reason.RESOURCE_MISMATCH);}finally{DataSourceUtils.releaseConnection(c,ds);}
 }
 F8Result finalizeAnswer(SourceBindingAdapter.Request request,SourceBindingAdapter.Scope scope){
  entry();Context ctx=new Context();
  try{F8Result result=tx.execute(s->{guard();TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void beforeCommit(boolean readOnly){ctx.commitStarted=true;}@Override public void afterCompletion(int status){ctx.completion=status;}});ctx.id=new F8Identity(verifier.verify(request,scope));probe.at("source_verified");owners.lock(ctx.id);
   owners.permission(ctx.id,false);F8HistoricalReader.Read old=history.read(ctx.id);
   if(old.state==F8HistoricalReader.State.INCONSISTENT)throw F8HistoricalReader.corrupt();
   if(old.state==F8HistoricalReader.State.FOUND_MATCH)return F8Result.success(old.receipt,true);
   long epoch=owners.permission(ctx.id,true);F8OwnerReader.Snapshot snapshot=owners.read(ctx.id,epoch);
   Map<String,Object> claim=history.claim(ctx.id);Map<String,Object> applied=history.applied(ctx.id,claim);
   // Read-only P5 exits only if no higher-priority fact. Clock is advisory here; final SQL is authoritative.
   Timestamp now=db.queryForObject("SELECT UTC_TIMESTAMP(6)",Timestamp.class);if(fixedClock!=null)now=Timestamp.valueOf(fixedClock);
   Map<String,Object> o=snapshot.consultation;boolean legal="WAITING".equals(o.get("state"))&&Objects.equals(o.get("current_wait"),ctx.id.source.field(9))&&((Number)o.get("clinical_version")).longValue()==Long.parseLong(ctx.id.source.field(14))&&"ASKED".equals(snapshot.rows.get("QUESTION").get("state"))&&"WAITING".equals(snapshot.rows.get("PENDING").get("state"));
   if(claim!=null&&claim.get("decision_id")!=null&&applied==null&&legal&&now.before(db.queryForObject("SELECT deadline FROM f8_owner WHERE ref=?",Timestamp.class,ctx.id.source.field(5))))throw new F8OwnerReader.Halt(Operational.DEFER,Reason.PENDING_WINNER);
   boolean sentinel=claim==null&&legal&&"DELIVERED".equals(snapshot.rows.get("ISSUANCE").get("state"))&&now.before(db.queryForObject("SELECT deadline FROM f8_owner WHERE ref=?",Timestamp.class,ctx.id.source.field(5)));
   if(sentinel){ctx.attempted=true;db.update("INSERT INTO f8_claim(wait_key,env,profile,tenant,consultation,question,parent_wait,canonical_id,decision_id,generation) VALUES (?,?,?,?,?,?,?,NULL,NULL,0)",ctx.id.waitKey,ctx.id.source.field(2),ctx.id.source.field(3),ctx.id.source.field(4),ctx.id.source.field(5),ctx.id.source.field(7),ctx.id.source.field(9));probe.at("after_sentinel");}
   Map<String,Object> params=parameters(ctx.id,snapshot);ctx.attempted=true;
   if(named.update(sql.replace("CLOCK_EXPR",fixedClock==null?"UTC_TIMESTAMP(6)":"CAST(:clock AS DATETIME(6))"),params)!=1)throw new F8OwnerReader.Halt(Operational.RETRYABLE_FAILURE,Reason.PREDICATE_CHANGED);
   probe.at("after_decision");String verdict=db.queryForObject("SELECT verdict FROM f8_decision WHERE id=?",String.class,ctx.id.decisionId);
   if(sentinel&&!"ACCEPTED".equals(verdict))throw new F8OwnerReader.Halt(Operational.RETRYABLE_FAILURE,Reason.SENTINEL_ROLLBACK);
   if("ACCEPTED".equals(verdict)){if(db.update("UPDATE f8_claim SET canonical_id=?,decision_id=?,generation=1 WHERE wait_key=? AND canonical_id IS NULL AND decision_id IS NULL AND generation=0",ctx.id.source.canonicalId,ctx.id.decisionId,ctx.id.waitKey)!=1)throw F8HistoricalReader.corrupt();probe.at("after_claim");}
   F8HistoricalReader.Read finalRead=history.read(ctx.id);if(finalRead.state!=F8HistoricalReader.State.FOUND_MATCH)throw F8HistoricalReader.corrupt();
   probe.at("before_commit");return F8Result.success(finalRead.receipt,false);
  });
   if(ctx.completion!=TransactionSynchronization.STATUS_COMMITTED){
    if(!ctx.attempted)return F8Result.blocked(Operational.DEFER,Attempt.NOT_ATTEMPTED,Reason.DB_FAILURE,key(ctx));
    if(ctx.completion==TransactionSynchronization.STATUS_ROLLED_BACK)return F8Result.blocked(Operational.RETRYABLE_FAILURE,Attempt.ATTEMPTED,Reason.DB_FAILURE,key(ctx));
    return F8Result.unknown(key(ctx));
   }
   probe.at("after_commit");return result;}catch(F8OwnerReader.Halt h){if(ctx.attempted&&(ctx.commitStarted||ctx.completion!=TransactionSynchronization.STATUS_ROLLED_BACK))return F8Result.unknown(key(ctx));return F8Result.blocked(ctx.attempted&&h.operational==Operational.DEFER?Operational.RETRYABLE_FAILURE:h.operational,ctx.attempted?Attempt.ATTEMPTED:Attempt.NOT_ATTEMPTED,h.reason,key(ctx));}
  catch(SourceBindingAdapter.Block b){if(ctx.attempted&&(ctx.commitStarted||ctx.completion!=TransactionSynchronization.STATUS_ROLLED_BACK))return F8Result.unknown(key(ctx));return F8Result.blocked(b.status==SourceBindingAdapter.Status.DENIED?Operational.DENIED:(b.status==SourceBindingAdapter.Status.ABSENT||b.status==SourceBindingAdapter.Status.UNRESOLVED_TARGET||b.status==SourceBindingAdapter.Status.UNAVAILABLE)?(ctx.attempted?Operational.RETRYABLE_FAILURE:Operational.DEFER):Operational.INTEGRITY_CONFLICT,ctx.attempted?Attempt.ATTEMPTED:Attempt.NOT_ATTEMPTED,Reason.EVIDENCE,key(ctx));}
  catch(IllegalArgumentException e){if(ctx.attempted&&(ctx.commitStarted||ctx.completion!=TransactionSynchronization.STATUS_ROLLED_BACK))return F8Result.unknown(key(ctx));return F8Result.blocked(ctx.attempted?Operational.INTEGRITY_CONFLICT:Operational.INVALID_INPUT,ctx.attempted?Attempt.ATTEMPTED:Attempt.NOT_ATTEMPTED,Reason.INPUT,key(ctx));}
  catch(RuntimeException e){return ctx.attempted?(!ctx.commitStarted&&ctx.completion==TransactionSynchronization.STATUS_ROLLED_BACK?F8Result.blocked(Operational.RETRYABLE_FAILURE,Attempt.ATTEMPTED,Reason.DB_FAILURE,key(ctx)):F8Result.unknown(key(ctx))):F8Result.blocked(Operational.DEFER,Attempt.NOT_ATTEMPTED,Reason.DB_FAILURE,key(ctx));}
 }
 F8HistoricalReader.Read read(SourceBindingAdapter.Request request,SourceBindingAdapter.Scope scope){
  entry();try{return tx.execute(s->{guard();F8Identity id=new F8Identity(verifier.verify(request,scope));owners.lock(id);owners.permission(id,false);return history.read(id);});}
  catch(F8OwnerReader.Halt h){return new F8HistoricalReader.Read(h.operational==Operational.DENIED?F8HistoricalReader.State.MISMATCH:h.operational==Operational.DEFER?F8HistoricalReader.State.UNAVAILABLE:F8HistoricalReader.State.INCONSISTENT,null);}
  catch(SourceBindingAdapter.Block b){return new F8HistoricalReader.Read(b.status==SourceBindingAdapter.Status.DENIED?F8HistoricalReader.State.MISMATCH:b.status==SourceBindingAdapter.Status.UNAVAILABLE?F8HistoricalReader.State.UNAVAILABLE:b.status==SourceBindingAdapter.Status.ABSENT||b.status==SourceBindingAdapter.Status.UNRESOLVED_TARGET?F8HistoricalReader.State.INDETERMINATE:F8HistoricalReader.State.INCONSISTENT,null);}
  catch(IllegalArgumentException e){return new F8HistoricalReader.Read(F8HistoricalReader.State.MISMATCH,null);}
  catch(RuntimeException e){return new F8HistoricalReader.Read(F8HistoricalReader.State.UNAVAILABLE,null);}
 }
 private String key(Context c){return c.id==null?null:c.id.decisionId;}
 private Map<String,Object> parameters(F8Identity id,F8OwnerReader.Snapshot s){
  Map<String,Object> p=new HashMap<>();p.put("id",id.decisionId);p.put("canonical",id.source.canonicalId);p.put("contract",F8Identity.CONTRACT);p.put("fingerprint",id.source.fingerprint);
  p.put("env",id.source.field(2));p.put("profile",id.source.field(3));p.put("tenant",id.source.field(4));p.put("consultation",id.source.field(5));p.put("question",id.source.field(7));p.put("wait",id.source.field(9));p.put("digest",id.source.field(16));p.put("wait_key",id.waitKey);p.put("clinical_version",Long.parseLong(id.source.field(14)));p.put("scope_key",id.scopeKey);p.put("epoch",s.epoch);p.put("issuer",F8Identity.ISSUER);p.put("policy",F8Identity.POLICY);p.put("manifest",F8Identity.MANIFEST);p.put("clock",fixedClock==null?null:Timestamp.valueOf(fixedClock));
  StringBuilder versions=new StringBuilder();for(String kind:new String[]{"CONSULTATION","QUESTION","PENDING","ISSUANCE"}){String k=kind.toLowerCase(Locale.ROOT);Map<String,Object> r=s.rows.get(kind);p.put(k+"_ref",r.get("ref"));p.put(k+"_version",r.get("version"));versions.append(r.get("version")).append(':');}p.put("owner_versions",versions.toString());return p;
 }
}
