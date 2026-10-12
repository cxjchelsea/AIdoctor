package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.p1b;

import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import java.sql.Connection;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import static org.junit.jupiter.api.Assertions.*;

public class StageBP1BReplayConflictTest {
 @Test public void conflict01()throws Exception{conflict(1);}
 @Test public void conflict02()throws Exception{conflict(2);}
 @Test public void conflict03()throws Exception{conflict(3);}
 @Test public void conflict04()throws Exception{conflict(4);}
 @Test public void conflict05()throws Exception{conflict(5);}
 @Test public void conflict06A()throws Exception{conflict(6);}
 @Test public void conflict06B()throws Exception{conflict(7);}
 @Test public void conflict06C()throws Exception{conflict(8);}
 @Test public void conflict06D()throws Exception{conflict(9);}
 private void conflict(int n)throws Exception{
  String name=n<6?String.format("conflict%02d",n):"conflict06"+(char)('A'+n-6);
  String caseId=n<6?String.format("P1-CONFLICT-%02d",n):"P1-CONFLICT-06"+(char)('A'+n-6);
  U06DeliveryStore.Command healthy=StageBP1BEvidence.command();
  U06DeliveryStore.Command candidate=StageBP1BEvidence.change(healthy,n,n>5);
  try(StageBP1BTestConfig env=new StageBP1BTestConfig()){
   StageBP1BEvidence.Hook positive=StageBP1BEvidence.hook(env);
   U06DeliveryStore.Snapshot created=StageBP1BEvidence.call(env,healthy,positive);
   assertFalse(created.replay);assertEquals(6,positive.writes);
   StageBP1BEvidence.Snapshot before,after;
   long readerId;
   try(Connection reader=StageBP1BEvidence.reader()){
    readerId=StageBP1BEvidence.connectionId(reader);
    StageBP1BEvidence.assertReaderDenied(reader);
    StageBP1BEvidence.complete(reader,healthy);
    before=StageBP1BEvidence.snapshot(reader,healthy.deliveryEffectId);
    if(n>5)assertArrayEquals(new int[6],StageBP1BEvidence.snapshot(reader,candidate.deliveryEffectId).vector,
     "P1B_DISTINCT_EFFECT_CANDIDATE_NOT_ABSENT");
    else assertEquals(before.digest,StageBP1BEvidence.snapshot(reader,candidate.deliveryEffectId).digest,
     "P1B_SAME_EFFECT_MUST_BE_EXISTING_HEALTHY_GROUP");
   }
   StageBP1BEvidence.Hook hook=StageBP1BEvidence.hook(env);
   String outcome,error="NO_SQL_CODE";
   if(n<=5){
    IllegalStateException thrown=assertThrows(IllegalStateException.class,
      ()->StageBP1BEvidence.call(env,candidate,hook));
    assertEquals("U06_DELIVERY_REPLAY_CONFLICT",thrown.getMessage());
    outcome="ORIGINAL_THROW_TYPED_CONFLICT";
    assertEquals(0,hook.writes);
   }else{
    DataAccessException thrown=assertThrows(DataAccessException.class,
      ()->StageBP1BEvidence.call(env,candidate,hook));
    error=StageBP1BEvidence.sqlInfo(thrown);assertNotEquals("NO_SQL_CODE",error);
    outcome="ORIGINAL_THROW_SQL_CONSTRAINT";
    assertEquals(n==6?0:n==9?4:1,hook.writes);
   }
   try(Connection reader=StageBP1BEvidence.reader()){
    StageBP1BEvidence.complete(reader,healthy);
    after=StageBP1BEvidence.snapshot(reader,healthy.deliveryEffectId);
    assertArrayEquals(before.vector,after.vector);
    assertEquals(before.digest,after.digest,"P1B_PROTECTED_WINNER_MUTATED");
    if(n>5)assertArrayEquals(new int[6],
      StageBP1BEvidence.snapshot(reader,candidate.deliveryEffectId).vector);
   }
   Map<String,Object> fact=StageBP1BEvidence.fact(name,caseId,outcome,"ROLLED_BACK_OBSERVED",
     before,after,candidate.deliveryEffectId,false,true,true,hook.writes,readerId,0,error);
   fact.put("protectedGraphBefore",before.digest);
   fact.put("protectedGraphAfter",after.digest);
   fact.put("candidateSixAfter",n>5?new int[6]:before.vector);
   fact.put("fullSixTableReplaySafety","NOT_ASSESSED_CONFLICT_ONLY");
   StageBP1BEvidence.emit(fact);
  }
 }
}
