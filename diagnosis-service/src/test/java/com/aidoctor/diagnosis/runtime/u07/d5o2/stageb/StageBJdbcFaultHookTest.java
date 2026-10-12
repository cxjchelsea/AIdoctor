package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb;

import com.aidoctor.diagnosis.runtime.u06.delivery.JdbcU06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import static org.junit.jupiter.api.Assertions.*;

public class StageBJdbcFaultHookTest {
 private static final String[] TABLES={"u06_delivery_authority","u06_delivery_intent",
  "u06_delivery_attempt","u06_delivery_receipt","u06_delivery_confirmation","u06_delivery_ledger"};
 private static int readCount(Connection c,String table,String effect) throws SQLException{
  boolean allowed=false;for(String t:TABLES)if(t.equals(table))allowed=true;
  if(!allowed)throw new IllegalArgumentException("STAGE_B_READ_TABLE_DENIED");
  try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM "+table+" WHERE delivery_effect_id=?")){
   p.setString(1,effect);try(ResultSet r=p.executeQuery()){assertTrue(r.next());return r.getInt(1);}
  }
 }
 private static long id(Connection c)throws SQLException{
  try(PreparedStatement p=c.prepareStatement("SELECT CONNECTION_ID()");
      ResultSet r=p.executeQuery()){assertTrue(r.next());return r.getLong(1);}
 }
 private static Connection independentReader()throws SQLException{
  return DriverManager.getConnection(StageBTestConfig.URL,"stageb_reader","synthetic-stageb-reader");
 }
 @Test public void p0PostWriteHookIsRealAndRollsBack() throws Exception{
  StageBTestConfig.requireEnvironment();
  try(AnnotationConfigApplicationContext ctx=new AnnotationConfigApplicationContext(StageBTestConfig.class)){
   DataSource ds=ctx.getBean("stageBDataSource",DataSource.class);
   DataSourceTransactionManager tm=ctx.getBean("stageBJdbcTransactionManager",DataSourceTransactionManager.class);
   JdbcTemplate physical=ctx.getBean("stageBJdbcTemplate",JdbcTemplate.class);
   assertEquals("u07_d5o2_stage_b_p0",physical.queryForObject("SELECT DATABASE()",String.class));
   String version=physical.queryForObject("SELECT VERSION()",String.class);
   assertTrue(version.startsWith("8."),"test requires real MySQL 8");
   for(String table:TABLES){
    String engine=physical.queryForObject("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?",
       new Object[]{"u07_d5o2_stage_b_p0",table},String.class);
    assertEquals("InnoDB",engine,"only transactional InnoDB tables are authorized");
   }
   String effect="effect-"+UUID.randomUUID(),selection="selection-"+UUID.randomUUID();
   try(Connection reader=independentReader()){
    assertEquals("u07_d5o2_stage_b_p0",reader.getCatalog());
    int baseline=readCount(reader,TABLES[0],effect);
    assertEquals(0,baseline);
    final int[] uncommittedReaderCount={-1};
    final long[] observerId={-1};
    StageBPostWriteFaultJdbcTemplate intercepted=new StageBPostWriteFaultJdbcTemplate(ds,1,ordinal->{
     try{
      assertEquals(1,ordinal);
      uncommittedReaderCount[0]=readCount(reader,TABLES[0],effect);
      observerId[0]=id(reader);
      assertEquals(0,uncommittedReaderCount[0],"uncommitted insert must be invisible");
     }catch(SQLException e){throw new IllegalStateException("STAGE_B_OBSERVER_FAILED",e);}
    });
    JdbcU06DeliveryStore store=new JdbcU06DeliveryStore(intercepted);
    U06DeliveryStore.Command cmd=new U06DeliveryStore.Command(selection,effect,"delivery-"+UUID.randomUUID(),
      "question-"+UUID.randomUUID(),"synthetic-fp","synthetic-endpoint","idem-"+UUID.randomUUID(),
      "confirmation-"+UUID.randomUUID(),"confirm-fp","2026-10-12T10:00:00Z");
    StageBPostWriteFaultJdbcTemplate.StageBInjectedAfterWriteFailure failure=
       assertThrows(StageBPostWriteFaultJdbcTemplate.StageBInjectedAfterWriteFailure.class,
       ()->StageBTxOwnerBindingTest.stage(tm,tm,ds,null,()->store.reconcileConfirmed(cmd)));
    assertEquals(1,failure.ordinal);
    assertEquals(1,intercepted.actualWrites());
    assertTrue(intercepted.transactionActiveAtFault());
    assertTrue(intercepted.writerConnectionId()>0);
    assertTrue(observerId[0]>0);
    assertNotEquals(intercepted.writerConnectionId(),observerId[0]);
    assertEquals(0,uncommittedReaderCount[0]);
    int after=0;
    for(String table:TABLES){int count=readCount(reader,table,effect);assertEquals(0,count,table+" must rollback");after+=count;}
    assertEquals(0,after);
    assertThrows(SQLException.class,()->{
     try(PreparedStatement p=reader.prepareStatement(
       "DELETE FROM u06_delivery_authority WHERE delivery_effect_id=?")){
      p.setString(1,effect);p.executeUpdate();
     }
    },"SELECT-only reader must be unable to delete even zero rows");
    assertEquals(0,readCount(reader,TABLES[0],effect));
    JdbcTemplate jdbc=ctx.getBean("stageBJdbcTemplate",JdbcTemplate.class);
    assertEquals(Integer.valueOf(0),jdbc.queryForObject(
       "SELECT COUNT(*) FROM u06_delivery_authority WHERE delivery_effect_id=?",new Object[]{effect},Integer.class));
    Map<String,Object> facts=StageBTxOwnerBindingTest.facts("P0-HOOK-01-02");
    facts.put("faultOrdinal",failure.ordinal);facts.put("successfulPhysicalSqlWrites",intercepted.actualWrites());
    facts.put("writerConnectionId",intercepted.writerConnectionId());
    facts.put("readerConnectionId",observerId[0]);
    facts.put("baselineCount",baseline);facts.put("uncommittedReaderCount",uncommittedReaderCount[0]);
    facts.put("postRollbackSumSixTables",after);facts.put("readOnlyDeleteDenied",true);
    facts.put("writerTxActiveAtFault",intercepted.transactionActiveAtFault());
    StageBTxOwnerBindingTest.evidence("p0PostWriteHookIsRealAndRollsBack",facts);
   }
   StageBTxOwnerBindingTest.clean();
  }
 }
 @Test public void p0UnexpectedDeliverySqlFailsBeforeMutation(){
  StageBTestConfig.requireEnvironment();
  try(AnnotationConfigApplicationContext ctx=new AnnotationConfigApplicationContext(StageBTestConfig.class)){
   DataSource ds=ctx.getBean("stageBDataSource",DataSource.class);
   StageBPostWriteFaultJdbcTemplate hook=new StageBPostWriteFaultJdbcTemplate(ds,0,null);
   assertThrows(IllegalStateException.class,()->hook.update(
    "INSERT INTO u06_delivery_ledger (delivery_effect_id) VALUES (?)","forbidden"));
   assertThrows(IllegalStateException.class,()->hook.update(
    "DELETE FROM u06_delivery_authority WHERE delivery_effect_id=?","forbidden"));
   assertEquals(0,hook.actualWrites());
   Map<String,Object> facts=StageBTxOwnerBindingTest.facts("P0-HOOK-NEG");
   facts.put("unexpectedDeliveryInsertDenied",true);
   facts.put("unexpectedDmlDenied",true);
   facts.put("successfulPhysicalSqlWrites",0);
   StageBTxOwnerBindingTest.evidence("p0UnexpectedDeliverySqlFailsBeforeMutation",facts);
  }
 }
}
