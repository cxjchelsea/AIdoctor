package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.p1b;

import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import java.sql.Connection;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import static org.junit.jupiter.api.Assertions.*;

public class StageBP1BPartialLedgerTest {
 @Test public void partial01()throws Exception{partial(1);}
 @Test public void partial02()throws Exception{partial(2);}
 @Test public void partial03()throws Exception{partial(3);}
 @Test public void partial04()throws Exception{partial(4);}
 @Test public void partial05()throws Exception{partial(5);}
 @Test public void partial06()throws Exception{partial(6);}
 private void partial(int n)throws Exception{
  String name=String.format("partial%02d",n),caseId=String.format("P1-PARTIAL-%02d",n);
  U06DeliveryStore.Command target=StageBP1BEvidence.command(),healthy=StageBP1BEvidence.command();
  try(StageBP1BTestConfig env=new StageBP1BTestConfig()){
   StageBP1BEvidence.Hook positive=StageBP1BEvidence.hook(env);
   U06DeliveryStore.Snapshot initial=StageBP1BEvidence.call(env,healthy,positive);
   assertFalse(initial.replay);assertEquals(6,positive.writes);
   String healthyBefore;
   try(Connection reader=StageBP1BEvidence.reader()){
    StageBP1BEvidence.assertReaderDenied(reader);
    StageBP1BEvidence.complete(reader,healthy);
    healthyBefore=StageBP1BEvidence.snapshot(reader,healthy.deliveryEffectId).digest;
    assertArrayEquals(new int[6],StageBP1BEvidence.snapshot(reader,target.deliveryEffectId).vector);
   }
   long seederId;
   try(Connection seeder=StageBP1BEvidence.seeder()){
    seederId=StageBP1BEvidence.connectionId(seeder);
    try{
     if(n==1)StageBP1BEvidence.insertSeed(seeder,5,target);
     if(n==2)StageBP1BEvidence.insertSeed(seeder,0,target);
     if(n==3)StageBP1BEvidence.insertSeed(seeder,1,target);
     if(n==4)StageBP1BEvidence.insertSeed(seeder,4,target);
     if(n>=5){
      for(int index:new int[]{0,1,4,5})StageBP1BEvidence.insertSeed(seeder,index,target);
      StageBP1BEvidence.insertSeed(seeder,n==5?3:2,target);
     }
     seeder.commit();
    }catch(Throwable err){seeder.rollback();throw err;}
   }
   StageBP1BEvidence.Snapshot seeded=null,after=null;
   long readerId=0;
   StageBP1BEvidence.Hook hook=StageBP1BEvidence.hook(env);
   boolean gap=n>=5;
   String outcome=null,error="NO_SQL_CODE",tx=null;
   try{
    try(Connection reader=StageBP1BEvidence.reader()){
     readerId=StageBP1BEvidence.connectionId(reader);
     seeded=StageBP1BEvidence.snapshot(reader,target.deliveryEffectId);
    }
    int[][] expected={{0,0,0,0,0,1},{1,0,0,0,0,0},{0,1,0,0,0,0},
                      {0,0,0,0,1,0},{1,1,0,1,1,1},{1,1,1,0,1,1}};
    assertArrayEquals(expected[n-1],seeded.vector);
    if(gap){
     U06DeliveryStore.Snapshot result=StageBP1BEvidence.call(env,target,hook);
     assertTrue(result.replay,"P1B_FOUR_TABLE_JOIN_SOURCE_GAP_EXPECTED");
     outcome="ORIGINAL_REPLAY_TRUE";tx="NO_NEW_TX_WRITES_OBSERVED";
    }else{
     DataAccessException thrown=assertThrows(DataAccessException.class,
       ()->StageBP1BEvidence.call(env,target,hook));
     outcome="ORIGINAL_THROW_SQL_CONSTRAINT";tx="ROLLED_BACK_OBSERVED";
     error=StageBP1BEvidence.sqlInfo(thrown);assertNotEquals("NO_SQL_CODE",error);
    }
    try(Connection reader=StageBP1BEvidence.reader()){
     after=StageBP1BEvidence.snapshot(reader,target.deliveryEffectId);
     assertArrayEquals(seeded.vector,after.vector);
     assertEquals(seeded.digest,after.digest,"P1B_SEEDED_PARTIAL_MUTATED");
     StageBP1BEvidence.complete(reader,healthy);
     assertEquals(healthyBefore,StageBP1BEvidence.snapshot(reader,healthy.deliveryEffectId).digest);
    }
    int[] writeOrdinals={5,0,1,4,0,0};
    assertEquals(writeOrdinals[n-1],hook.writes);
   }finally{
    try(Connection seeder=StageBP1BEvidence.seeder()){
     StageBP1BEvidence.cleanup(seeder,target);
    }
   }
   Map<String,Object> fact=StageBP1BEvidence.fact(name,caseId,outcome,tx,seeded,after,
       target.deliveryEffectId,gap,false,true,hook.writes,readerId,seederId,error);
   fact.put("fullSixTableReplaySafety","FAIL_NOT_ESTABLISHED");
   fact.put("protectedGraphBefore",healthyBefore);fact.put("protectedGraphAfter",healthyBefore);
   fact.put("fixtureCleanupAfter",new int[6]);
   StageBP1BEvidence.emit(fact);
  }
 }
}
