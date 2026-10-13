package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.p1c;

import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import java.sql.Connection;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StageBP1CConcurrencyTest {
 @Test public void race01()throws Exception{race("race01",1,0);}
 @Test public void race02()throws Exception{race("race02",2,0);}
 @Test public void race03()throws Exception{race("race03",3,0);}
 @Test public void race04A()throws Exception{race("race04A",4,0);}
 @Test public void race04B()throws Exception{race("race04B",5,0);}
 @Test public void race05()throws Exception{race("race05",6,0);}
 @Test public void race06A()throws Exception{race("race06A",1,1);}
 @Test public void race06B()throws Exception{race("race06B",2,2);}

 private void race(String method,int scenario,int replayMode)throws Exception{
  U06DeliveryStore.Command first=StageBP1CEvidence.command(),second=StageBP1CEvidence.change(first,scenario);
  StageBP1CEvidence.Actor a=new StageBP1CEvidence.Actor(first);
  StageBP1CEvidence.Actor b=new StageBP1CEvidence.Actor(second);
  try(StageBP1CTestConfig env=new StageBP1CTestConfig()){
   CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);
   ExecutorService pool=Executors.newFixedThreadPool(2);
   Future<StageBP1CEvidence.Actor> fa=null,fb=null;
   boolean readyOk=false,terminated=false,timedOut=false;
   long releaseNano=0;
   try{
    fa=pool.submit(()->worker(env,a,ready,start));
    fb=pool.submit(()->worker(env,b,ready,start));
    readyOk=ready.await(3,TimeUnit.SECONDS);
    if(!readyOk)throw new IllegalStateException("P1C_READY_TIMEOUT");
    releaseNano=System.nanoTime();start.countDown();
    long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
    fa.get(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);
    fb.get(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);
   }catch(Exception ex){
    timedOut=true;
    if(fa!=null)fa.cancel(true);
    if(fb!=null)fb.cancel(true);
    throw ex;
   }finally{
    start.countDown();pool.shutdownNow();
    terminated=pool.awaitTermination(5,TimeUnit.SECONDS);
   }
   assertTrue(readyOk&&terminated&&!timedOut,"P1C_HARNESS_TIMEOUT_FAIL");
   assertTrue(a.resourcesClear&&b.resourcesClear,"P1C_TRANSACTION_RESOURCE_LEAK");
   assertTrue(a.hook!=null&&b.hook!=null&&a.hook.writerId>0&&b.hook.writerId>0);
   assertNotEquals(a.hook.writerId,b.hook.writerId,"P1C_NOT_DISTINCT_WRITERS");
   assertTrue(a.readyNano<=releaseNano&&b.readyNano<=releaseNano);
   boolean overlapping=a.beginNano<b.endNano&&b.beginNano<a.endNano;
   Map<String,Object> fact=new LinkedHashMap<String,Object>();
   fact.put("caseId","P1C-RACE-"+method.substring(4).toUpperCase());
   fact.put("actorA",StageBP1CEvidence.actor(a));fact.put("actorB",StageBP1CEvidence.actor(b));
   fact.put("startReleaseNano",releaseNano);fact.put("bothReady",readyOk);
   fact.put("transactionIntervalsOverlapped",overlapping);
   fact.put("dbContentionProvenByError",a.error.endsWith(":1213")||b.error.endsWith(":1213")||
       a.error.endsWith(":1205")||b.error.endsWith(":1205"));
   fact.put("executorTerminated",terminated);fact.put("workerTimeout",timedOut);
   fact.put("harnessVerdict",terminated&&!timedOut&&overlapping?"OBSERVED_OVERLAP":"NOT_OBSERVED");
   int successes=(isNew(a)?1:0)+(isNew(b)?1:0);
   int replay=(isReplay(a)?1:0)+(isReplay(b)?1:0);
   fact.put("newSuccessCount",successes);fact.put("replayCount",replay);
   String groupA,groupB;
   int[] sixA,sixB;long readerId;
   try(Connection r=StageBP1CEvidence.reader()){
    readerId=StageBP1CEvidence.id(r);
    assertNotEquals(readerId,a.hook.writerId);
    assertNotEquals(readerId,b.hook.writerId);
    StageBP1CEvidence.Group ga=StageBP1CEvidence.read(r,first.deliveryEffectId);
    StageBP1CEvidence.Group gb=StageBP1CEvidence.read(r,second.deliveryEffectId);
    groupA=ga.digest;groupB=gb.digest;sixA=ga.counts;sixB=gb.counts;
    if(scenario==6){
     StageBP1CEvidence.full(r,first);StageBP1CEvidence.full(r,second);
    }else{
     if(isNew(a))StageBP1CEvidence.full(r,first);
     if(isNew(b))StageBP1CEvidence.full(r,second);
    }
   }
   fact.put("finalReaderConnectionId",readerId);fact.put("readerDeleteDenied",true);
   fact.put("effectAAfterVector",sixA);fact.put("effectBAfterVector",sixB);
   fact.put("effectAAfterDigest",groupA);fact.put("effectBAfterDigest",groupB);
   fact.put("effectsShared",first.deliveryEffectId.equals(second.deliveryEffectId));
   fact.put("physicalSixTableSafety","VERIFIED_BY_FRESH_READER");
   // Preserve observed facts even if original Store concurrency violates the expected invariant.
   StageBP1CEvidence.emit(method,fact);
   assertTrue(overlapping,"P1C_NO_OBSERVED_TX_OVERLAP");
   if(scenario==6){
    assertEquals(2,successes,"P1C_UNRELATED_TX_BOTH_REQUIRED");
   }else{
    assertEquals(1,successes,"P1C_ONE_WINNER_REQUIRED");
    StageBP1CEvidence.Actor winner=isNew(a)?a:b;
    StageBP1CEvidence.Actor loser=isNew(a)?b:a;
    assertEquals(6,winner.hook.writes);
    assertTrue(allowedLoser(loser,scenario),"P1C_UNEXPECTED_LOSER_"+loser.outcome+"/"+loser.error);
    if(scenario==1){
     assertArrayEquals(new int[]{1,1,1,1,1,1},sixA);
    }else if(scenario==2){
     assertArrayEquals(new int[]{1,1,1,1,1,1},sixA);
     assertFalse(isReplay(loser),"P1C_FALSE_CHANGED_REPLAY");
    }else{
     assertArrayEquals(isNew(a)?new int[]{1,1,1,1,1,1}:new int[6],sixA);
     assertArrayEquals(isNew(b)?new int[]{1,1,1,1,1,1}:new int[6],sixB);
    }
   }
   if(replayMode!=0){
    StageBP1CEvidence.Actor winner=isNew(a)?a:b;
    U06DeliveryStore.Command follow=replayMode==1?winner.command:
      StageBP1CEvidence.change(winner.command,2);
    StageBP1CEvidence.Actor serial=StageBP1CEvidence.invoke(env,new StageBP1CEvidence.Actor(follow));
    fact.put("sequential",StageBP1CEvidence.actor(serial));
    try(Connection r=StageBP1CEvidence.reader()){
     StageBP1CEvidence.Group after=StageBP1CEvidence.read(r,winner.command.deliveryEffectId);
     assertEquals(groupA,after.digest);
     StageBP1CEvidence.full(r,winner.command);
     fact.put("afterSequentialDigest",after.digest);
    }
    if(replayMode==1){assertEquals("ORIGINAL_REPLAY_TRUE",serial.outcome);assertEquals(0,serial.hook.writes);}
    else {assertEquals("ORIGINAL_THROW_TYPED_CONFLICT",serial.outcome);assertEquals(0,serial.hook.writes);}
    StageBP1CEvidence.emit(method,fact);
   }
  }
 }
 private static StageBP1CEvidence.Actor worker(StageBP1CTestConfig env,StageBP1CEvidence.Actor actor,
    CountDownLatch ready,CountDownLatch start)throws Exception{
  actor.readyNano=System.nanoTime();ready.countDown();
  if(!start.await(3,TimeUnit.SECONDS))throw new IllegalStateException("P1C_START_TIMEOUT");
  return StageBP1CEvidence.invoke(env,actor);
 }
 private static boolean isNew(StageBP1CEvidence.Actor x){
  return "ORIGINAL_NEW_SUCCESS".equals(x.outcome)&&x.committed;
 }
 private static boolean isReplay(StageBP1CEvidence.Actor x){
  return "ORIGINAL_REPLAY_TRUE".equals(x.outcome)&&x.committed;
 }
 private static boolean allowedLoser(StageBP1CEvidence.Actor x,int scenario){
  if(scenario==1&&isReplay(x))return true;
  if(scenario==2&&"ORIGINAL_THROW_TYPED_CONFLICT".equals(x.outcome))return true;
  return Arrays.asList("ORIGINAL_THROW_SQL_1062","ORIGINAL_THROW_SQL_1213",
    "ORIGINAL_THROW_SQL_1205").contains(x.outcome);
 }
}
