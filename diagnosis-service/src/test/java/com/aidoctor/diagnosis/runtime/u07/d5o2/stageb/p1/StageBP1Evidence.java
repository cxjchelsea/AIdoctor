package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.p1;

import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;
import java.util.Locale;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

final class StageBP1Evidence {
 static final String[] TABLES={"u06_delivery_authority","u06_delivery_intent","u06_delivery_attempt",
  "u06_delivery_receipt","u06_delivery_confirmation","u06_delivery_ledger"};
 static final String[] IDS={"selection_effect_id","delivery_effect_id","attempt_id",
  "receipt_id","confirmation_evaluation_id","delivery_effect_id"};
 static final String READER="stagebp1_reader",READ_PWD="synthetic-stagebp1-reader";
 private StageBP1Evidence(){}
 static U06DeliveryStore.Command command(){
  String suffix=UUID.randomUUID().toString();
  return new U06DeliveryStore.Command("selection-"+suffix,"effect-"+suffix,"delivery-"+suffix,
   "question-"+suffix,"synthetic-content-"+suffix,"synthetic-endpoint","idem-"+suffix,
   "confirmation-"+suffix,"synthetic-confirm-"+suffix,"2026-10-12T14:00:00Z");
 }
 static Connection reader() throws SQLException {
  Connection c=DriverManager.getConnection(StageBP1TestConfig.URL,READER,READ_PWD);
  Assertions.assertEquals("u07_d5o2_stage_b_p1",c.getCatalog());c.setAutoCommit(true);return c;
 }
 /** Each test reader is physically SELECT-only. A zero-row DELETE must still be denied. */
 static boolean assertReaderPermission(Connection c)throws SQLException{
  try(PreparedStatement who=c.prepareStatement("SELECT CURRENT_USER()");
      ResultSet rs=who.executeQuery()){
   Assertions.assertTrue(rs.next());
   Assertions.assertTrue(rs.getString(1).startsWith(READER+"@"),"P1A_WRONG_READER_ROLE");
  }
  try(PreparedStatement write=c.prepareStatement(
    "DELETE FROM u06_delivery_authority WHERE delivery_effect_id=?")){
   write.setString(1,"synthetic-denied-permission-probe");
   SQLException denied=Assertions.assertThrows(SQLException.class,()->write.executeUpdate());
   Assertions.assertEquals("42000",denied.getSQLState(),"P1A_READER_DML_MUST_FAIL_PRIVILEGE");
  }
  return true;
 }
 /** Domain-separated, stable, complete six-table row serialization including every column. */
 static String graphDigest(Connection c,U06DeliveryStore.Command x)throws SQLException{
  complete(c,x);
  StringBuilder canonical=new StringBuilder("P1A-SIX-GRAPH-V1");
  append(canonical,x.deliveryEffectId);
  for(int n=0;n<TABLES.length;n++){
   append(canonical,TABLES[n]);
   Map<String,String> sorted=new TreeMap<String,String>(row(c,n,x.deliveryEffectId));
   for(Map.Entry<String,String> entry:sorted.entrySet()){
    append(canonical,entry.getKey());append(canonical,entry.getValue());
   }
  }
  return sha256(canonical.toString());
 }
 static String effectDigest(String effect){return sha256("P1A-EFFECT-V1:"+effect);}
 private static void append(StringBuilder b,String value){
  if(value==null){b.append("-1:");return;}
  b.append(value.length()).append(":").append(value);
 }
 private static String sha256(String value){
  try{
   byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
   StringBuilder hex=new StringBuilder();
   for(byte each:bytes)hex.append(String.format(Locale.ROOT,"%02x",each&0xff));
   return hex.toString();
  }catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 static long connectionId(Connection c)throws SQLException{
  try(PreparedStatement p=c.prepareStatement("SELECT CONNECTION_ID()");
      ResultSet r=p.executeQuery()){Assertions.assertTrue(r.next());return r.getLong(1);}
 }
 static int[] counts(Connection c,String effect)throws SQLException{
  int[] result=new int[6];
  for(int i=0;i<6;i++){
   try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM "+TABLES[i]+" WHERE delivery_effect_id=?")){
    p.setString(1,effect);
    try(ResultSet r=p.executeQuery()){Assertions.assertTrue(r.next());result[i]=r.getInt(1);}
   }
  }
  return result;
 }
 static Map<String,String> row(Connection c,int index,String effect)throws SQLException{
  try(PreparedStatement p=c.prepareStatement("SELECT * FROM "+TABLES[index]+" WHERE delivery_effect_id=?")){
   p.setString(1,effect);
   try(ResultSet r=p.executeQuery()){
    Assertions.assertTrue(r.next(),TABLES[index]+" missing");
    Map<String,String> values=new LinkedHashMap<String,String>();
    ResultSetMetaData meta=r.getMetaData();
    for(int n=1;n<=meta.getColumnCount();n++)values.put(meta.getColumnLabel(n),r.getString(n));
    Assertions.assertFalse(r.next(),TABLES[index]+" duplicate effect");
    return values;
   }
  }
 }
 static void complete(Connection c,U06DeliveryStore.Command x)throws SQLException{
  Assertions.assertArrayEquals(new int[]{1,1,1,1,1,1},counts(c,x.deliveryEffectId));
  Map<String,String> a=row(c,0,x.deliveryEffectId),i=row(c,1,x.deliveryEffectId),
   t=row(c,2,x.deliveryEffectId),r=row(c,3,x.deliveryEffectId),
   f=row(c,4,x.deliveryEffectId),l=row(c,5,x.deliveryEffectId);
  Assertions.assertEquals(x.selectionEffectId,a.get("selection_effect_id"));
  Assertions.assertEquals(x.deliveryId,a.get("delivery_id"));
  Assertions.assertEquals("CONFIRMED_TERMINAL",a.get("authority_status"));
  Assertions.assertEquals(x.deliveryId,i.get("delivery_id"));
  Assertions.assertEquals(x.questionId,i.get("question_id"));
  Assertions.assertEquals(x.contentFingerprint,i.get("content_fingerprint"));
  Assertions.assertEquals(x.endpointRef,i.get("endpoint_ref"));
  Assertions.assertEquals(x.idempotencyKey,i.get("idempotency_key"));
  Assertions.assertEquals("READY",i.get("intent_status"));
  Assertions.assertEquals(x.deliveryEffectId+":attempt:1",t.get("attempt_id"));
  Assertions.assertEquals(x.deliveryId,t.get("delivery_id"));
  Assertions.assertEquals("SYNTHETIC_ACCEPTED",t.get("attempt_status"));
  Assertions.assertEquals(x.deliveryEffectId+":receipt:1",r.get("receipt_id"));
  Assertions.assertEquals(x.deliveryId,r.get("delivery_id"));
  Assertions.assertEquals("SYNTHETIC_CONFIRMED",r.get("receipt_status"));
  Assertions.assertEquals("SYNTHETIC",r.get("evidence_type"));
  Assertions.assertEquals(x.confirmationEvaluationId,f.get("confirmation_evaluation_id"));
  Assertions.assertEquals(x.deliveryId,f.get("delivery_id"));
  Assertions.assertEquals(x.confirmationFingerprint,f.get("confirmation_fingerprint"));
  Assertions.assertEquals("CONFIRMED",f.get("confirmation_status"));
  Assertions.assertEquals(x.deliveryId,l.get("delivery_id"));
  Assertions.assertEquals(x.confirmationFingerprint,l.get("canonical_fingerprint"));
  Assertions.assertEquals("CONFIRMED",l.get("confirmation_status"));
 }
 static Map<String,Object> facts(String method,String caseId){
  Map<String,Object> m=new LinkedHashMap<String,Object>();
  m.put("testMethod",method);m.put("caseId",caseId);
  m.put("evidencePlane","TEST_ONLY_REAL_MYSQL8");m.put("productionTopologyVerified",false);
  m.put("downstreamAuthority",false);m.put("realD5Ready",false);
  m.put("sourceMainSha","3b0589347d90af5ec5b86fbdad5eecabcce41a24");
  m.put("junitClass",method.startsWith("fault")?"StageBP1AtomicityTest":"StageBP1ReplayConflictTest");
  m.put("actualPrHeadSha",System.getenv("U07_D5O2_PR_HEAD_SHA"));
  m.put("workflowRunId",System.getenv("GITHUB_RUN_ID"));
  m.put("testedSyntheticMergeCheckoutSha",System.getenv("GITHUB_SHA"));
  return m;
 }
 static void emit(Map<String,Object> m){
  try{
   StageBP1TestConfig.requireEnvironment();
   java.nio.file.Path dir=Paths.get("target","stage-b-p1a-observations");
   Files.createDirectories(dir);
   new ObjectMapper().writeValue(dir.resolve(m.get("testMethod")+".json").toFile(),m);
  }catch(java.io.IOException e){throw new IllegalStateException("P1A_EVIDENCE_IO",e);}
 }
 static void clean(){
  if(TransactionSynchronizationManager.isActualTransactionActive()
   ||TransactionSynchronizationManager.isSynchronizationActive()
   ||!TransactionSynchronizationManager.getResourceMap().isEmpty())
   throw new IllegalStateException("P1A_AMBIENT_OR_LEAKED_RESOURCES");
 }
 static <T> T execute(DataSource ds,DataSourceTransactionManager mgr,java.util.function.Supplier<T> work){
  clean();
  if(mgr.getDataSource()!=ds)throw new IllegalStateException("P1A_WRONG_MANAGER");
  TransactionTemplate t=new TransactionTemplate(mgr);
  t.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
  t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
  t.setTimeout(10);
  try{return t.execute(s->{
    Assertions.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
    Assertions.assertTrue(TransactionSynchronizationManager.hasResource(ds));
    Connection c=DataSourceUtils.getConnection(ds);
    Assertions.assertTrue(DataSourceUtils.isConnectionTransactional(c,ds));
    try{Assertions.assertEquals(Connection.TRANSACTION_SERIALIZABLE,c.getTransactionIsolation());}
    catch(SQLException ex){throw new IllegalStateException(ex);}
    return work.get();
  });}finally{clean();}
 }
 static void physicalPreflight(JdbcTemplate jdbc){
  Assertions.assertEquals("u07_d5o2_stage_b_p1",jdbc.queryForObject("SELECT DATABASE()",String.class));
  Assertions.assertTrue(jdbc.queryForObject("SELECT VERSION()",String.class).startsWith("8."));
  for(String table:TABLES){
   String engine=jdbc.queryForObject("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?",
    new Object[]{"u07_d5o2_stage_b_p1",table},String.class);
   Assertions.assertEquals("InnoDB",engine);
  }
 }
 /** Test-only precise original SQL interception, never a production guard or mock. */
 static final class Hook extends JdbcTemplate {
  private final int failAt;
  private int writes;
  private long writerId;
  private int[] uncommitted;
  private long observerId;
  Hook(DataSource ds,int failAt){super(ds);if(failAt<0||failAt>6)throw new IllegalArgumentException();this.failAt=failAt;}
  private static final String[] EXACT_INSERTS={
   "insert into u06_delivery_authority (selection_effect_id,delivery_effect_id,delivery_id,authority_status,created_at) values (?,?,?,?,?)",
   "insert into u06_delivery_intent (delivery_effect_id,delivery_id,question_id,content_fingerprint,endpoint_ref,idempotency_key,intent_status,created_at) values (?,?,?,?,?,?,?,?)",
   "insert into u06_delivery_attempt (attempt_id,delivery_effect_id,delivery_id,attempt_status,created_at) values (?,?,?,?,?)",
   "insert into u06_delivery_receipt (receipt_id,delivery_effect_id,delivery_id,receipt_status,evidence_type,created_at) values (?,?,?,?,?,?)",
   "insert into u06_delivery_confirmation (confirmation_evaluation_id,delivery_effect_id,delivery_id,confirmation_status,confirmation_fingerprint,created_at) values (?,?,?,?,?,?)",
   "insert into u06_delivery_ledger (delivery_effect_id,delivery_id,confirmation_status,canonical_fingerprint,created_at) values (?,?,?,?,?)"
  };
  @Override public int update(String sql,Object... args){
   if(sql==null||writes>=6||!sql.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT).equals(EXACT_INSERTS[writes]))
    throw new IllegalStateException("P1A_UNEXPECTED_OR_UNORDERED_SQL");
   if(!TransactionSynchronizationManager.isActualTransactionActive()
     ||!TransactionSynchronizationManager.hasResource(getDataSource())
     ||!DataSourceUtils.isConnectionTransactional(DataSourceUtils.getConnection(getDataSource()),getDataSource()))
    throw new IllegalStateException("P1A_SQL_OUTSIDE_OWNED_TX");
   int count=super.update(sql,args);
   if(count!=1)throw new IllegalStateException("P1A_WRONG_INSERT_COUNT");
   writes++;
   if(writes==failAt){
    writerId=super.queryForObject("SELECT CONNECTION_ID()",Long.class);
    try(Connection observer=reader()){
     observerId=connectionId(observer);
     Assertions.assertNotEquals(writerId,observerId);
     uncommitted=counts(observer,(String)args[sql.contains("u06_delivery_authority")?1:0]);
    }catch(SQLException e){throw new IllegalStateException("P1A_INFLIGHT_READ_FAILED",e);}
    throw new Injected(writes);
   }
   return count;
  }
  int writes(){return writes;}
  long writerId(){return writerId;}
  long observerId(){return observerId;}
  int[] uncommitted(){return uncommitted;}
 }
 static final class Injected extends RuntimeException{
  final int ordinal;
  Injected(int n){super("P1A_AFTER_REAL_SQL_"+n);ordinal=n;}
 }
}
