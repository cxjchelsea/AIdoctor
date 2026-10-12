package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb;

import com.aidoctor.diagnosis.runtime.u06.delivery.JdbcU06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

public class StageBJdbcFaultHookTest {
 private static final String[] TABLES={"u06_delivery_authority","u06_delivery_intent",
  "u06_delivery_attempt","u06_delivery_receipt","u06_delivery_confirmation","u06_delivery_ledger"};
 private static int readCount(String table,String effect) throws Exception{
  boolean allowed=false;for(String t:TABLES)if(t.equals(table))allowed=true;
  if(!allowed)throw new IllegalArgumentException("STAGE_B_READ_TABLE_DENIED");
  String col=table.equals("u06_delivery_authority")?"delivery_effect_id":"delivery_effect_id";
  try(Connection c=DriverManager.getConnection(StageBTestConfig.URL,"stageb_reader","synthetic-stageb-reader");
      PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM "+table+" WHERE "+col+"=?")){
   p.setString(1,effect);
   try(ResultSet r=p.executeQuery()){assertTrue(r.next());return r.getInt(1);}
  }
 }
 @Test public void p0PostWriteHookIsRealAndRollsBack() throws Exception {
  StageBTestConfig.requireEnvironment();
  try(AnnotationConfigApplicationContext ctx=new AnnotationConfigApplicationContext(StageBTestConfig.class)){
   DataSource ds=ctx.getBean("stageBDataSource",DataSource.class);
   DataSourceTransactionManager tm=ctx.getBean("stageBJdbcTransactionManager",DataSourceTransactionManager.class);
   String effect="effect-"+UUID.randomUUID().toString(),selection="selection-"+UUID.randomUUID().toString();
   StageBPostWriteFaultJdbcTemplate intercepted=new StageBPostWriteFaultJdbcTemplate(ds,1);
   JdbcU06DeliveryStore store=new JdbcU06DeliveryStore(intercepted);
   U06DeliveryStore.Command cmd=new U06DeliveryStore.Command(selection,effect,"delivery-"+UUID.randomUUID(),
     "question-"+UUID.randomUUID(),"synthetic-fp","synthetic-endpoint","idem-"+UUID.randomUUID(),
     "confirmation-"+UUID.randomUUID(),"confirm-fp","2026-10-12T10:00:00Z");
   TransactionTemplate tx=StageBTxOwnerBindingTest.template(tm);
   assertThrows(StageBPostWriteFaultJdbcTemplate.StageBInjectedAfterWriteFailure.class,
     ()->StageBTxOwnerBindingTest.stage(tx,()->store.reconcileConfirmed(cmd)));
   assertEquals(1,intercepted.actualWrites());
   for(String table:TABLES)assertEquals(0,readCount(table,effect),table+" must rollback");
   JdbcTemplate jdbc=ctx.getBean("stageBJdbcTemplate",JdbcTemplate.class);
   assertEquals(Integer.valueOf(0),jdbc.queryForObject(
     "SELECT COUNT(*) FROM u06_delivery_authority WHERE delivery_effect_id=?",new Object[]{effect},Integer.class));
   StageBTxOwnerBindingTest.clean();
  }
 }
}
