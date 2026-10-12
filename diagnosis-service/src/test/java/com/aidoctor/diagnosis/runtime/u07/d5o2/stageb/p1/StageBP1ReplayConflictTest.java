package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.p1;

import com.aidoctor.diagnosis.runtime.u06.delivery.JdbcU06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import static org.junit.jupiter.api.Assertions.*;

public class StageBP1ReplayConflictTest {
 @Test public void happy01(){
  StageBP1TestConfig.requireEnvironment();
  try(AnnotationConfigApplicationContext ctx=new AnnotationConfigApplicationContext(StageBP1TestConfig.class)){
   DataSource ds=ctx.getBean("p1DataSource",DataSource.class);
   DataSourceTransactionManager tm=ctx.getBean("p1JdbcTransactionManager",DataSourceTransactionManager.class);
   StageBP1Evidence.physicalPreflight(ctx.getBean("p1JdbcTemplate",JdbcTemplate.class));
   U06DeliveryStore.Command cmd=StageBP1Evidence.command();
   StageBP1Evidence.Hook hook=new StageBP1Evidence.Hook(ds,0);
   U06DeliveryStore.Snapshot result=StageBP1Evidence.execute(ds,tm,
     ()->new JdbcU06DeliveryStore(hook).reconcileConfirmed(cmd));
   assertFalse(result.replay);assertEquals("CONFIRMED",result.confirmationStatus);
   assertEquals(6,hook.writes());
   int[] six;
   try(Connection c=StageBP1Evidence.reader()){
    StageBP1Evidence.complete(c,cmd);
    six=StageBP1Evidence.counts(c,cmd.deliveryEffectId);
   }
   Map<String,Object> facts=StageBP1Evidence.facts("happy01","P1-HAPPY-01");
   facts.put("methodOutcome","ORIGINAL_NEW_SUCCESS");
   facts.put("physicalTransactionResult","COMMITTED_OBSERVED");
   facts.put("successfulOriginalSqlWrites",hook.writes());
   facts.put("sixAfter",six);facts.put("testVerdict","ATOMICITY_PASS");
   StageBP1Evidence.emit(facts);
  }
 }
 @Test public void replay01(){
  StageBP1TestConfig.requireEnvironment();
  try(AnnotationConfigApplicationContext ctx=new AnnotationConfigApplicationContext(StageBP1TestConfig.class)){
   DataSource ds=ctx.getBean("p1DataSource",DataSource.class);
   DataSourceTransactionManager tm=ctx.getBean("p1JdbcTransactionManager",DataSourceTransactionManager.class);
   U06DeliveryStore.Command cmd=StageBP1Evidence.command();
   StageBP1Evidence.Hook first=new StageBP1Evidence.Hook(ds,0);
   U06DeliveryStore.Snapshot original=StageBP1Evidence.execute(ds,tm,
     ()->new JdbcU06DeliveryStore(first).reconcileConfirmed(cmd));
   assertFalse(original.replay);assertEquals(6,first.writes());
   int[] before,after;
   try(Connection c=StageBP1Evidence.reader()){
    StageBP1Evidence.complete(c,cmd);before=StageBP1Evidence.counts(c,cmd.deliveryEffectId);
   }
   StageBP1Evidence.Hook second=new StageBP1Evidence.Hook(ds,0);
   U06DeliveryStore.Snapshot repeated=StageBP1Evidence.execute(ds,tm,
     ()->new JdbcU06DeliveryStore(second).reconcileConfirmed(cmd));
   assertTrue(repeated.replay);assertEquals("CONFIRMED",repeated.confirmationStatus);
   assertEquals(0,second.writes());
   try(Connection fresh=StageBP1Evidence.reader()){
    StageBP1Evidence.complete(fresh,cmd);after=StageBP1Evidence.counts(fresh,cmd.deliveryEffectId);
   }
   assertArrayEquals(before,after);
   Map<String,Object> facts=StageBP1Evidence.facts("replay01","P1-REPLAY-01");
   facts.put("methodOutcome","ORIGINAL_REPLAY_TRUE");
   facts.put("physicalTransactionResult","COMMITTED_OBSERVED");
   facts.put("successfulOriginalSqlWrites",second.writes());
   facts.put("sixBefore",before);facts.put("sixAfter",after);
   facts.put("testVerdict","SOURCE_CHARACTERIZATION_PASS");
   StageBP1Evidence.emit(facts);
  }
 }
}
