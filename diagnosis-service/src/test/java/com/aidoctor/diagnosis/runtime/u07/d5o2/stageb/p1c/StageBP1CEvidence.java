package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.p1c;

import com.aidoctor.diagnosis.runtime.u06.delivery.JdbcU06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

final class StageBP1CEvidence {
 static final String[] TABLES={"u06_delivery_authority","u06_delivery_intent","u06_delivery_attempt",
   "u06_delivery_receipt","u06_delivery_confirmation","u06_delivery_ledger"};
 static final String[] INSERTS={
  "insert into u06_delivery_authority (selection_effect_id,delivery_effect_id,delivery_id,authority_status,created_at) values (?,?,?,?,?)",
  "insert into u06_delivery_intent (delivery_effect_id,delivery_id,question_id,content_fingerprint,endpoint_ref,idempotency_key,intent_status,created_at) values (?,?,?,?,?,?,?,?)",
  "insert into u06_delivery_attempt (attempt_id,delivery_effect_id,delivery_id,attempt_status,created_at) values (?,?,?,?,?)",
  "insert into u06_delivery_receipt (receipt_id,delivery_effect_id,delivery_id,receipt_status,evidence_type,created_at) values (?,?,?,?,?,?)",
  "insert into u06_delivery_confirmation (confirmation_evaluation_id,delivery_effect_id,delivery_id,confirmation_status,confirmation_fingerprint,created_at) values (?,?,?,?,?,?)",
  "insert into u06_delivery_ledger (delivery_effect_id,delivery_id,confirmation_status,canonical_fingerprint,created_at) values (?,?,?,?,?)"
 };
 private StageBP1CEvidence(){}
 static U06DeliveryStore.Command command(){
  String k=UUID.randomUUID().toString();
  return new U06DeliveryStore.Command("sel-"+k,"effect-"+k,"delivery-"+k,"question-"+k,
    "content-"+k,"synthetic-endpoint","idem-"+k,"confirm-"+k,"fingerprint-"+k,"2026-10-13T08:00:00Z");
 }
 static U06DeliveryStore.Command change(U06DeliveryStore.Command a,int scenario){
  U06DeliveryStore.Command b=command();
  if(scenario==1)return a;
  if(scenario==2)return new U06DeliveryStore.Command(a.selectionEffectId,a.deliveryEffectId,a.deliveryId,
    a.questionId,a.contentFingerprint+"-other",a.endpointRef,a.idempotencyKey,
    a.confirmationEvaluationId,a.confirmationFingerprint,a.createdAt);
  if(scenario==3)return new U06DeliveryStore.Command(b.selectionEffectId,b.deliveryEffectId,b.deliveryId,
    b.questionId,b.contentFingerprint,b.endpointRef,a.idempotencyKey,b.confirmationEvaluationId,b.confirmationFingerprint,b.createdAt);
  if(scenario==4)return new U06DeliveryStore.Command(a.selectionEffectId,b.deliveryEffectId,b.deliveryId,
    b.questionId,b.contentFingerprint,b.endpointRef,b.idempotencyKey,b.confirmationEvaluationId,b.confirmationFingerprint,b.createdAt);
  if(scenario==5)return new U06DeliveryStore.Command(b.selectionEffectId,b.deliveryEffectId,a.deliveryId,
    b.questionId,b.contentFingerprint,b.endpointRef,b.idempotencyKey,b.confirmationEvaluationId,b.confirmationFingerprint,b.createdAt);
  if(scenario==6)return b;
  throw new IllegalArgumentException("P1C_SCENARIO");
 }
 static long id(Connection c)throws SQLException{
  try(PreparedStatement p=c.prepareStatement("SELECT CONNECTION_ID()");ResultSet r=p.executeQuery()){
   Assertions.assertTrue(r.next());return r.getLong(1);
  }
 }
 static String role(Connection c)throws SQLException{
  try(PreparedStatement p=c.prepareStatement("SELECT CURRENT_USER()");ResultSet r=p.executeQuery()){
   Assertions.assertTrue(r.next());return r.getString(1);
  }
 }
 static boolean denied(Connection c)throws SQLException{
  try(PreparedStatement p=c.prepareStatement("DELETE FROM u06_delivery_ledger WHERE delivery_effect_id=?")){
   p.setString(1,"synthetic-p1c-no-delete");
   SQLException e=Assertions.assertThrows(SQLException.class,()->p.executeUpdate());
   Assertions.assertEquals("42000",e.getSQLState());return true;
  }
 }
 static Connection reader()throws SQLException{
  Connection c=DriverManager.getConnection(StageBP1CTestConfig.URL,"stagebp1c_reader",
    StageBP1CTestConfig.require("P1C_READER_PWD"));
  Assertions.assertEquals(StageBP1CTestConfig.SCHEMA,c.getCatalog());
  Assertions.assertTrue(role(c).startsWith("stagebp1c_reader@"));
  denied(c);
  return c;
 }
 static void preflight(JdbcTemplate j){
  Assertions.assertEquals(StageBP1CTestConfig.SCHEMA,j.queryForObject("SELECT DATABASE()",String.class));
  Assertions.assertTrue(j.queryForObject("SELECT VERSION()",String.class).startsWith("8."));
  Assertions.assertTrue(j.queryForObject("SELECT CURRENT_USER()",String.class).startsWith("stagebp1c_writer@"));
  for(String table:TABLES)
   Assertions.assertEquals("InnoDB",j.queryForObject(
    "SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?",
    new Object[]{StageBP1CTestConfig.SCHEMA,table},String.class));
 }
 static String sha(String in){
  try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(in.getBytes(StandardCharsets.UTF_8));
   StringBuilder b=new StringBuilder();
   for(byte x:bytes)b.append(String.format(Locale.ROOT,"%02x",x&255));
   return b.toString();
  }catch(Exception e){throw new IllegalStateException(e);}
 }
 static void add(StringBuilder b,String v){if(v==null)b.append("-1:");else b.append(v.length()).append(':').append(v);}
 static final class Group {
  final int[] counts;final String digest;
  Group(int[] counts,String digest){this.counts=counts;this.digest=digest;}
 }
 static Group read(Connection c,String effect)throws SQLException{
  int[] counts=new int[6];StringBuilder all=new StringBuilder("P1C-SIX-ROW-CANONICAL-V1");
  add(all,effect);
  for(int i=0;i<6;i++){
   add(all,TABLES[i]);List<String> rows=new ArrayList<String>();
   try(PreparedStatement p=c.prepareStatement("SELECT * FROM "+TABLES[i]+" WHERE delivery_effect_id=?")){
    p.setString(1,effect);
    try(ResultSet rs=p.executeQuery()){
     ResultSetMetaData md=rs.getMetaData();
     while(rs.next()){
      Map<String,String> columns=new TreeMap<String,String>();
      for(int k=1;k<=md.getColumnCount();k++)columns.put(md.getColumnLabel(k),rs.getString(k));
      StringBuilder one=new StringBuilder();
      for(Map.Entry<String,String> e:columns.entrySet()){add(one,e.getKey());add(one,e.getValue());}
      rows.add(one.toString());
     }
    }
   }
   Collections.sort(rows);counts[i]=rows.size();add(all,Integer.toString(rows.size()));
   for(String row:rows)add(all,row);
  }
  return new Group(counts,sha(all.toString()));
 }
 static void full(Connection c,U06DeliveryStore.Command cmd)throws SQLException{
  Assertions.assertArrayEquals(new int[]{1,1,1,1,1,1},read(c,cmd.deliveryEffectId).counts);
  String[][] fields={
   {"selection_effect_id",cmd.selectionEffectId,"delivery_id",cmd.deliveryId,"authority_status","CONFIRMED_TERMINAL"},
   {"delivery_id",cmd.deliveryId,"question_id",cmd.questionId,"content_fingerprint",cmd.contentFingerprint,
     "endpoint_ref",cmd.endpointRef,"idempotency_key",cmd.idempotencyKey,"intent_status","READY"},
   {"attempt_id",cmd.deliveryEffectId+":attempt:1","delivery_id",cmd.deliveryId,"attempt_status","SYNTHETIC_ACCEPTED"},
   {"receipt_id",cmd.deliveryEffectId+":receipt:1","delivery_id",cmd.deliveryId,"receipt_status","SYNTHETIC_CONFIRMED","evidence_type","SYNTHETIC"},
   {"confirmation_evaluation_id",cmd.confirmationEvaluationId,"delivery_id",cmd.deliveryId,"confirmation_status","CONFIRMED",
      "confirmation_fingerprint",cmd.confirmationFingerprint},
   {"delivery_id",cmd.deliveryId,"confirmation_status","CONFIRMED","canonical_fingerprint",cmd.confirmationFingerprint}
  };
  for(int i=0;i<6;i++)
   try(PreparedStatement p=c.prepareStatement("SELECT * FROM "+TABLES[i]+" WHERE delivery_effect_id=?")){
    p.setString(1,cmd.deliveryEffectId);
    try(ResultSet r=p.executeQuery()){
     Assertions.assertTrue(r.next());Assertions.assertEquals(cmd.deliveryEffectId,r.getString("delivery_effect_id"));
     for(int k=0;k<fields[i].length;k+=2)Assertions.assertEquals(fields[i][k+1],r.getString(fields[i][k]));
     Assertions.assertFalse(r.next());
    }
   }
 }
 static String sqlError(Throwable e){
  for(Throwable x=e;x!=null;x=x.getCause())
   if(x instanceof SQLException){
    SQLException q=(SQLException)x;return q.getSQLState()+":"+q.getErrorCode();
   }
  return "NONE";
 }
 static final class Hook extends JdbcTemplate{
  int writes;long writerId;String writerRole;int isolation;int lockWait;
  boolean deleteDenied;long firstInsertNano;
  final List<String> sqlTrace=new ArrayList<String>();
  Hook(DataSource ds){super(ds);}
  void bind(Connection c)throws SQLException{
   Assertions.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
   Assertions.assertTrue(DataSourceUtils.isConnectionTransactional(c,getDataSource()));
   Assertions.assertTrue(role(c).startsWith("stagebp1c_writer@"));
   writerRole=role(c);writerId=id(c);isolation=c.getTransactionIsolation();
   Assertions.assertEquals(Connection.TRANSACTION_SERIALIZABLE,isolation);
   try(Statement q=c.createStatement()){q.execute("SET SESSION innodb_lock_wait_timeout=5");}
   try(PreparedStatement q=c.prepareStatement("SELECT @@SESSION.innodb_lock_wait_timeout");
       ResultSet r=q.executeQuery()){Assertions.assertTrue(r.next());lockWait=r.getInt(1);}
   Assertions.assertEquals(5,lockWait);
   deleteDenied=denied(c);
  }
  @Override public int update(String sql,Object... args){
   Assertions.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
   Assertions.assertTrue(writerId>0&&deleteDenied);
   Assertions.assertTrue(writes<6);
   String normalized=sql.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
   Assertions.assertEquals(INSERTS[writes],normalized,"P1C_UNEXPECTED_OR_UNORDERED_SQL");
   if(firstInsertNano==0)firstInsertNano=System.nanoTime();
   int affected=super.update(sql,args);
   Assertions.assertEquals(1,affected);sqlTrace.add(TABLES[writes]);writes++;return affected;
  }
 }
 static final class Actor {
  final U06DeliveryStore.Command command;
  long readyNano,beginNano,endNano;
  Hook hook;String outcome="NOT_OBSERVED",error="NONE",exceptionClass="NONE";
  boolean committed,resourcesClear;
  Actor(U06DeliveryStore.Command c){command=c;}
 }
 static Actor invoke(StageBP1CTestConfig env,Actor a){
  if(TransactionSynchronizationManager.isActualTransactionActive()||
     !TransactionSynchronizationManager.getResourceMap().isEmpty())
   throw new IllegalStateException("P1C_AMBIENT_TX");
  a.hook=new Hook(env.source);
  TransactionTemplate t=new TransactionTemplate(env.manager);
  t.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
  t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);t.setTimeout(10);
  a.beginNano=System.nanoTime();
  try{
   U06DeliveryStore.Snapshot result=t.execute(status->{
    Connection c=DataSourceUtils.getConnection(env.source);
    try{a.hook.bind(c);}catch(SQLException ex){throw new IllegalStateException("P1C_PHYSICAL_ROLE",ex);}
    return new JdbcU06DeliveryStore(a.hook).reconcileConfirmed(a.command);
   });
   a.outcome=result.replay?"ORIGINAL_REPLAY_TRUE":"ORIGINAL_NEW_SUCCESS";
   a.committed=true;
  }catch(Throwable e){
   a.error=sqlError(e);a.exceptionClass=e.getClass().getName();
   if(e instanceof IllegalStateException&&"U06_DELIVERY_REPLAY_CONFLICT".equals(e.getMessage()))
    a.outcome="ORIGINAL_THROW_TYPED_CONFLICT";
   else if(a.error.endsWith(":1062"))a.outcome="ORIGINAL_THROW_SQL_1062";
   else if(a.error.endsWith(":1213"))a.outcome="ORIGINAL_THROW_SQL_1213";
   else if(a.error.endsWith(":1205"))a.outcome="ORIGINAL_THROW_SQL_1205";
   else a.outcome="OTHER_EXCEPTION";
  }finally{
   a.endNano=System.nanoTime();
   a.resourcesClear=!TransactionSynchronizationManager.isActualTransactionActive()&&
    TransactionSynchronizationManager.getResourceMap().isEmpty();
  }
  return a;
 }
 static Map<String,Object> actor(Actor a){
  Map<String,Object> x=new LinkedHashMap<String,Object>();
  x.put("effectDigest",sha(a.command.deliveryEffectId));x.put("payloadDigest",sha(a.command.contentFingerprint));
  x.put("selectionDigest",sha(a.command.selectionEffectId));x.put("deliveryDigest",sha(a.command.deliveryId));
  x.put("idempotencyDigest",sha(a.command.idempotencyKey));
  x.put("readyNano",a.readyNano);x.put("beginNano",a.beginNano);x.put("endNano",a.endNano);
  x.put("sourceOutcome",a.outcome);x.put("sqlError",a.error);x.put("exceptionClass",a.exceptionClass);
  x.put("transactionCallReturned",a.committed);x.put("resourceClear",a.resourcesClear);
  if(a.hook!=null){
   x.put("writerConnectionId",a.hook.writerId);x.put("role",a.hook.writerRole);
   x.put("isolation",a.hook.isolation);x.put("lockWait",a.hook.lockWait);
   x.put("deleteDenied",a.hook.deleteDenied);x.put("firstInsertNano",a.hook.firstInsertNano);
   x.put("writes",a.hook.writes);x.put("sqlTrace",new ArrayList<String>(a.hook.sqlTrace));
  }
  return x;
 }
 static void emit(String method,Map<String,Object> fact){
  try{
   fact.put("testMethod",method);fact.put("testClass","StageBP1CConcurrencyTest");
   fact.put("sourceMainSha","20a54b4a7478641d8b8d8c12c6213b8e2ea73acc");
   fact.put("prHeadSha",System.getenv("U07_P1C_PR_HEAD_SHA"));
   fact.put("mergeCheckoutSha",System.getenv("GITHUB_SHA"));fact.put("runId",System.getenv("GITHUB_RUN_ID"));
   fact.put("schema",StageBP1CTestConfig.SCHEMA);fact.put("profile",StageBP1CTestConfig.PROFILE);
   fact.put("originalFourTableReplayGapStillOpen",true);
   fact.put("businessReplaySafetyVerdict","NOT_PROVEN_PRODUCTION");
   fact.put("downstreamAuthority",false);
   java.nio.file.Path dir=Paths.get("target","stage-b-p1c-observations");Files.createDirectories(dir);
   new ObjectMapper().writeValue(dir.resolve(method+".json").toFile(),fact);
  }catch(java.io.IOException e){throw new IllegalStateException(e);}
 }
}
