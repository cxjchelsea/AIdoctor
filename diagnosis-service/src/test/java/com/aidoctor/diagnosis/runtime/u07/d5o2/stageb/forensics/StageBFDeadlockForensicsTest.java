package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.forensics;

import com.aidoctor.diagnosis.runtime.u06.delivery.JdbcU06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

public class StageBFDeadlockForensicsTest {
 private static final class Worker {
  final U06DeliveryStore.Command cmd;
  final AtomicLong session=new AtomicLong();
  volatile String outcome="NOT_OBSERVED",error="NONE";
  volatile boolean returned,cleared;
  volatile long begin,end;volatile int inserts;
  Worker(U06DeliveryStore.Command c){cmd=c;}
 }
 private static U06DeliveryStore.Command command(){
  String k=UUID.randomUUID().toString();
  return new U06DeliveryStore.Command("s-"+k,"e-"+k,"d-"+k,"q-"+k,"f-"+k,
   "synthetic-endpoint","i-"+k,"c-"+k,"cf-"+k,"2026-10-13T08:00:00Z");
 }
 private static void invoke(StageBFTestConfig env,Worker w){
  w.begin=System.nanoTime();
  JdbcTemplate j=new JdbcTemplate(env.writer){
   @Override public int update(String sql,Object... args){
    Assertions.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
    int rows=super.update(sql,args);w.inserts++;return rows;
   }
  };
  TransactionTemplate tx=new TransactionTemplate(env.tx);
  tx.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
  tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
  tx.setTimeout(10);
  try{
   U06DeliveryStore.Snapshot result=tx.execute(status->{
    Connection c=DataSourceUtils.getConnection(env.writer);
    try{
     w.session.set(StageBFObserverEvidence.id(c));
     Assertions.assertEquals(Connection.TRANSACTION_SERIALIZABLE,c.getTransactionIsolation());
     try(java.sql.Statement p=c.createStatement()){p.setQueryTimeout(2);p.execute("SET SESSION innodb_lock_wait_timeout=5");}
    }catch(SQLException e){throw new IllegalStateException("F_WRITER_PHYSICAL_GUARD");}
    return new JdbcU06DeliveryStore(j).reconcileConfirmed(w.cmd);
   });
   w.returned=true;w.outcome=result.replay?"ORIGINAL_REPLAY_TRUE":"ORIGINAL_NEW_SUCCESS";
  }catch(Exception e){
   Throwable t=e;
   while(t!=null&&!(t instanceof SQLException))t=t.getCause();
   if(t instanceof SQLException){
    SQLException q=(SQLException)t;w.error=q.getSQLState()+":"+q.getErrorCode();
    if("40001".equals(q.getSQLState())&&q.getErrorCode()==1213)w.outcome="ORIGINAL_SQL_1213";
    else w.outcome="OTHER_SQL_ERROR";
   }else w.outcome="OTHER_EXCEPTION";
  }finally{
   w.end=System.nanoTime();
   w.cleared=!TransactionSynchronizationManager.isActualTransactionActive()&&
      TransactionSynchronizationManager.getResourceMap().isEmpty();
  }
 }
 private static Map<String,Object> actor(Worker w){
  Map<String,Object> m=new LinkedHashMap<String,Object>();
  m.put("effectDigest",StageBFObserverEvidence.digest(w.cmd.deliveryEffectId));
  m.put("connectionId",w.session.get());m.put("outcome",w.outcome);m.put("sqlError",w.error);
  m.put("txReturned",w.returned);m.put("resourcesClear",w.cleared);
  m.put("beginNano",w.begin);m.put("endNano",w.end);m.put("successfulInsertCount",w.inserts);
  return m;
 }
 @Test public void originalIndependentEffectDeadlockObservation()throws Exception{
  StageBFObserverEvidence.negativeRedactionTests();
  Worker a=new Worker(command()),b=new Worker(command());
  Map<String,Object> facts=new LinkedHashMap<String,Object>();
  facts.put("caseId","F_ISOLATED_ORIGINAL_RACE05_L0");
  facts.put("caseAcceptance","DIAGNOSTIC_ONLY_NOT_BUSINESS_COMPLETION");
  StageBFObserverEvidence.emit("forensic-incident",
      StageBFObserverEvidence.evidence("CAPTURE_NOT_STARTED",facts));
  try(StageBFTestConfig env=new StageBFTestConfig();
      Connection reader=StageBFTestConfig.connection("stagebf_reader","F_READER_PWD");
      Connection observer=StageBFTestConfig.connection("stagebf_observer_ps","F_OBSERVER_PWD");
      Connection wcheck=StageBFTestConfig.connection("stagebf_writer","F_WRITER_PWD")){
   StageBFObserverEvidence.permissions(wcheck,reader,observer);
   facts.put("observerRole","L0_TABLE_SELECT_ONLY");
   facts.put("rolesVerified",true);
   facts.put("roleNegativePassed",true);
   StageBFObserverEvidence.emit("role-negative",StageBFObserverEvidence.evidence("ROLE_NEGATIVES_PASS",facts));
   CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);
   AtomicInteger finished=new AtomicInteger();
   AtomicBoolean stopped=new AtomicBoolean();
   ExecutorService pool=Executors.newFixedThreadPool(2);
   Future<?> fa=null,fb=null;
   boolean terminated=false,readyOk=false,timeout=false;
   StageBFObserverEvidence.Sample sample=null;
   try{
    fa=pool.submit(()->{
     ready.countDown();
     try{if(!start.await(3,TimeUnit.SECONDS))throw new IllegalStateException("START_TIMEOUT");
      invoke(env,a);
     }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("INTERRUPTED");}
     finally{if(finished.incrementAndGet()==2)stopped.set(true);}
    });
    fb=pool.submit(()->{
     ready.countDown();
     try{if(!start.await(3,TimeUnit.SECONDS))throw new IllegalStateException("START_TIMEOUT");
      invoke(env,b);
     }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("INTERRUPTED");}
     finally{if(finished.incrementAndGet()==2)stopped.set(true);}
    });
    readyOk=ready.await(3,TimeUnit.SECONDS);
    if(!readyOk)throw new IllegalStateException("READY_TIMEOUT");
    start.countDown();
    long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
    while((a.session.get()==0||b.session.get()==0)&&finished.get()<2&&System.nanoTime()<deadline)
     TimeUnit.MILLISECONDS.sleep(2);
    if(a.session.get()!=0&&b.session.get()!=0)
     sample=StageBFObserverEvidence.observe(observer,a.session.get(),b.session.get(),stopped);
    fa.get(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);
    fb.get(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);
   }catch(Exception e){
    timeout=true; if(fa!=null)fa.cancel(true);if(fb!=null)fb.cancel(true);
    facts.put("harnessFailure","WORKER_OR_OBSERVER_NOT_OBSERVED");
    throw e;
   }finally{
    stopped.set(true);start.countDown();pool.shutdownNow();
    terminated=pool.awaitTermination(5,TimeUnit.SECONDS);
    facts.put("actorA",actor(a));facts.put("actorB",actor(b));
    facts.put("bothReady",readyOk);facts.put("executorTerminated",terminated);
    facts.put("harnessTimeout",timeout);
    facts.put("observerSampleCount",sample==null?0:sample.count);
    StageBFObserverEvidence.emit("forensic-incident",
     StageBFObserverEvidence.evidence("DIAGNOSTIC_ONLY_NOT_ACCEPTED",facts));
   }
   Assertions.assertTrue(terminated&&!timeout&&a.cleared&&b.cleared);
   Assertions.assertNotEquals(a.session.get(),b.session.get());
   Assertions.assertTrue(a.session.get()>0&&b.session.get()>0);
   int[] ca=StageBFObserverEvidence.rows(reader,a.cmd.deliveryEffectId);
   int[] cb=StageBFObserverEvidence.rows(reader,b.cmd.deliveryEffectId);
   facts.put("readerId",StageBFObserverEvidence.id(reader));
   facts.put("effectAVector",ca);facts.put("effectBVector",cb);
   boolean aSuccess=a.returned&&"ORIGINAL_NEW_SUCCESS".equals(a.outcome);
   boolean bSuccess=b.returned&&"ORIGINAL_NEW_SUCCESS".equals(b.outcome);
   boolean victim="ORIGINAL_SQL_1213".equals(a.outcome)||"ORIGINAL_SQL_1213".equals(b.outcome);
   if(aSuccess)Assertions.assertArrayEquals(new int[]{1,1,1,1,1,1},ca);
   if(bSuccess)Assertions.assertArrayEquals(new int[]{1,1,1,1,1,1},cb);
   if(victim){
    if("ORIGINAL_SQL_1213".equals(a.outcome))Assertions.assertArrayEquals(new int[6],ca);
    if("ORIGINAL_SQL_1213".equals(b.outcome))Assertions.assertArrayEquals(new int[6],cb);
   }
   String verdict=sample==null?"NO_EDGE_CAPTURED":sample.verdict;
   // Never promote a mere pair of observed edges into proof unless both were captured
   // in one sampling epoch and tied to real 1213.
   if("LIVE_TWO_WRITER_CYCLE_BOUND".equals(verdict)&&!victim)verdict="PARTIAL_EDGE_ONLY";
   facts.put("sourceVerdict",victim?"ROLLBACK_CONFIRMED_1213":"NO_1213_OBSERVED");
   facts.put("liveLockGraphVerdict",verdict);
   facts.put("observerSamples",sample==null?0:sample.count);
   facts.put("readerRole","SIX_TABLE_SELECT_ONLY");
   facts.put("caseAcceptance","DIAGNOSTIC_PIPELINE_ONLY");
   StageBFObserverEvidence.emit("forensic-incident",StageBFObserverEvidence.evidence(verdict,facts));
   Assertions.assertTrue(aSuccess||bSuccess||victim,"NO_PHYSICAL_OBSERVATION");
  }
 }
}
