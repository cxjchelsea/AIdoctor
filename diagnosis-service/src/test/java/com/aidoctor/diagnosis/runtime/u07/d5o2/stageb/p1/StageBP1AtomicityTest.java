package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.p1;

import com.aidoctor.diagnosis.runtime.u06.delivery.JdbcU06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import static org.junit.jupiter.api.Assertions.*;

public class StageBP1AtomicityTest {
 @Test public void fault01(){fault(1);}
 @Test public void fault02(){fault(2);}
 @Test public void fault03(){fault(3);}
 @Test public void fault04(){fault(4);}
 @Test public void fault05(){fault(5);}
 @Test public void fault06(){fault(6);}
 private void fault(int n){
  StageBP1TestConfig.requireEnvironment();
  String suffix=String.format("%02d",n),method="fault"+suffix,caseId="P1-FAULT-"+suffix;
  try(AnnotationConfigApplicationContext ctx=new AnnotationConfigApplicationContext(StageBP1TestConfig.class)){
   DataSource ds=ctx.getBean("p1DataSource",DataSource.class);
   JdbcTemplate jdbc=ctx.getBean("p1JdbcTemplate",JdbcTemplate.class);
   DataSourceTransactionManager tm=ctx.getBean("p1JdbcTransactionManager",DataSourceTransactionManager.class);
   StageBP1Evidence.physicalPreflight(jdbc);
   U06DeliveryStore.Command healthy=StageBP1Evidence.command(),candidate=StageBP1Evidence.command();
   StageBP1Evidence.Hook committedHook=new StageBP1Evidence.Hook(ds,0);
   U06DeliveryStore.Snapshot h=StageBP1Evidence.execute(ds,tm,
     ()->new JdbcU06DeliveryStore(committedHook).reconcileConfirmed(healthy));
   assertFalse(h.replay);assertEquals(6,committedHook.writes());
   int[] baseline,healthyBaseline;
   try(Connection observer=StageBP1Evidence.reader()){
    StageBP1Evidence.complete(observer,healthy);
    healthyBaseline=StageBP1Evidence.counts(observer,healthy.deliveryEffectId);
    baseline=StageBP1Evidence.counts(observer,candidate.deliveryEffectId);
   }
   assertArrayEquals(new int[6],baseline);
   StageBP1Evidence.Hook faultHook=new StageBP1Evidence.Hook(ds,n);
   StageBP1Evidence.Injected thrown=assertThrows(StageBP1Evidence.Injected.class,
    ()->StageBP1Evidence.execute(ds,tm,
     ()->new JdbcU06DeliveryStore(faultHook).reconcileConfirmed(candidate)));
   assertEquals(n,thrown.ordinal);assertEquals(n,faultHook.writes());
   assertTrue(faultHook.writerId()>0);assertTrue(faultHook.observerId()>0);
   assertArrayEquals(new int[6],faultHook.uncommitted());
   int[] after,healthyAfter;long afterObserverId;
   try(Connection fresh=StageBP1Evidence.reader()){
    afterObserverId=StageBP1Evidence.connectionId(fresh);
    after=StageBP1Evidence.counts(fresh,candidate.deliveryEffectId);
    StageBP1Evidence.complete(fresh,healthy);
    healthyAfter=StageBP1Evidence.counts(fresh,healthy.deliveryEffectId);
   }
   assertArrayEquals(new int[6],after);
   assertArrayEquals(healthyBaseline,healthyAfter);
   Map<String,Object> facts=StageBP1Evidence.facts(method,caseId);
   facts.put("faultOrdinal",n);facts.put("successfulOriginalSqlWrites",faultHook.writes());
   facts.put("methodOutcome","ORIGINAL_THROW");
   facts.put("exceptionClass",thrown.getClass().getName());
   facts.put("physicalTransactionResult","ROLLED_BACK_OBSERVED");
   facts.put("sixBefore",baseline);facts.put("sixInFlight",faultHook.uncommitted());
   facts.put("sixAfter",after);facts.put("healthyBefore",healthyBaseline);
   facts.put("healthyAfter",healthyAfter);
   facts.put("writerConnectionId",faultHook.writerId());
   facts.put("inFlightReaderConnectionId",faultHook.observerId());
   facts.put("postFreshReaderConnectionId",afterObserverId);
   facts.put("testVerdict","ATOMICITY_PASS");facts.put("freshObserver",true);
   StageBP1Evidence.emit(facts);
  }
 }
}
