package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.p1b;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

final class StageBP1BEvidence {
 static final String[] TABLES={"u06_delivery_authority","u06_delivery_intent","u06_delivery_attempt",
  "u06_delivery_receipt","u06_delivery_confirmation","u06_delivery_ledger"};
 private StageBP1BEvidence(){}
 static U06DeliveryStore.Command command(){
  String k=UUID.randomUUID().toString();
  return new U06DeliveryStore.Command("sel-"+k,"effect-"+k,"delivery-"+k,
   "question-"+k,"content-"+k,"synthetic-endpoint","idem-"+k,
   "confirm-"+k,"fingerprint-"+k,"2026-10-12T14:00:00Z");
 }
 static U06DeliveryStore.Command change(U06DeliveryStore.Command c,int n,boolean newEffect){
  String s=c.selectionEffectId,e=c.deliveryEffectId,d=c.deliveryId,q=c.questionId,
    fp=c.contentFingerprint,endpoint=c.endpointRef,idem=c.idempotencyKey,
    eval=c.confirmationEvaluationId,confirm=c.confirmationFingerprint;
  if(newEffect){String k=UUID.randomUUID().toString();s="sel-"+k;e="effect-"+k;
   d="delivery-"+k;idem="idem-"+k;eval="confirm-"+k;}
  switch(n){
   case 1:fp+="-different";break;
   case 2:endpoint+="-different";break;
   case 3:confirm+="-different";break;
   case 4:d+="-different";break;
   case 5:idem+="-different";break;
   case 6:s=c.selectionEffectId;break;
   case 7:d=c.deliveryId;break;
   case 8:idem=c.idempotencyKey;break;
   case 9:eval=c.confirmationEvaluationId;break;
   default:throw new IllegalArgumentException();
  }
  return new U06DeliveryStore.Command(s,e,d,q,fp,endpoint,idem,eval,confirm,c.createdAt);
 }
 static Connection connect(String role,String env)throws SQLException{
  Connection c=DriverManager.getConnection(StageBP1BTestConfig.URL,role,StageBP1BTestConfig.require(env));
  Assertions.assertEquals(StageBP1BTestConfig.SCHEMA,c.getCatalog());
  try(PreparedStatement p=c.prepareStatement("SELECT CURRENT_USER()");
      ResultSet r=p.executeQuery()){
   Assertions.assertTrue(r.next());
   Assertions.assertTrue(r.getString(1).startsWith(role+"@"),"P1B_WRONG_MYSQL_ROLE");
  }
  return c;
 }
 static Connection reader()throws SQLException{
  Connection c=connect("stagebp1b_reader","P1B_READER_PWD");
  c.setAutoCommit(true);
  return c;
 }
 static Connection seeder()throws SQLException{
  Connection c=connect("stagebp1b_seeder","P1B_SEEDER_PWD");
  c.setAutoCommit(false);
  return c;
 }
 static long connectionId(Connection c)throws SQLException{
  try(PreparedStatement p=c.prepareStatement("SELECT CONNECTION_ID()");
      ResultSet r=p.executeQuery()){Assertions.assertTrue(r.next());return r.getLong(1);}
 }
 static boolean assertReaderDenied(Connection c)throws SQLException{
  try(PreparedStatement p=c.prepareStatement("DELETE FROM u06_delivery_ledger WHERE delivery_effect_id=?")){
   p.setString(1,"synthetic-denied");
   SQLException error=Assertions.assertThrows(SQLException.class,()->p.executeUpdate());
   Assertions.assertEquals("42000",error.getSQLState());
  }
  return true;
 }
 static boolean assertWriterDenied(Connection c)throws SQLException{
  try(PreparedStatement p=c.prepareStatement("DELETE FROM u06_delivery_ledger WHERE delivery_effect_id=?")){
   p.setString(1,"synthetic-writer-denied");
   SQLException denied=Assertions.assertThrows(SQLException.class,()->p.executeUpdate());
   Assertions.assertEquals("42000",denied.getSQLState());
  }
  return true;
 }
 static boolean assertSeederDdlDenied(Connection c)throws SQLException{
  try(PreparedStatement p=c.prepareStatement("CREATE TABLE p1b_disallowed_ddl (id INTEGER)")){
   SQLException denied=Assertions.assertThrows(SQLException.class,()->p.executeUpdate());
   Assertions.assertEquals("42000",denied.getSQLState());
  }
  return true;
 }
 static String actualRole(Connection c)throws SQLException{
  try(PreparedStatement p=c.prepareStatement("SELECT CURRENT_USER()");ResultSet r=p.executeQuery()){
   Assertions.assertTrue(r.next());return r.getString(1);
  }
 }
 static void schemaPreflight(JdbcTemplate jdbc){
  Assertions.assertEquals(StageBP1BTestConfig.SCHEMA,jdbc.queryForObject("SELECT DATABASE()",String.class));
  Assertions.assertTrue(jdbc.queryForObject("SELECT VERSION()",String.class).startsWith("8."));
  String role=jdbc.queryForObject("SELECT CURRENT_USER()",String.class);
  Assertions.assertTrue(role.startsWith("stagebp1b_writer@"));
  for(String table:TABLES){
   String engine=jdbc.queryForObject("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?",
    new Object[]{StageBP1BTestConfig.SCHEMA,table},String.class);
   Assertions.assertEquals("InnoDB",engine);
  }
 }
 static final class Snapshot {
  final int[] vector;final String digest;
  Snapshot(int[] c,String d){vector=c;digest=d;}
 }
 /** A partial-aware all-row canonical projection: supports 0..N rows per effect. */
 static Snapshot snapshot(Connection c,String effect)throws SQLException{
  int[] counts=new int[6];
  StringBuilder b=new StringBuilder("P1B-PARTIAL-AWARE-SIX-TABLE-V1");
  append(b,effect);
  for(int i=0;i<TABLES.length;i++){
   append(b,TABLES[i]);
   List<String> rows=new ArrayList<String>();
   try(PreparedStatement p=c.prepareStatement("SELECT * FROM "+TABLES[i]+" WHERE delivery_effect_id=?")){
    p.setString(1,effect);
    try(ResultSet r=p.executeQuery()){
     ResultSetMetaData meta=r.getMetaData();
     while(r.next()){
      Map<String,String> sorted=new TreeMap<String,String>();
      for(int k=1;k<=meta.getColumnCount();k++)sorted.put(meta.getColumnLabel(k),r.getString(k));
      StringBuilder row=new StringBuilder();
      for(Map.Entry<String,String> field:sorted.entrySet()){
       append(row,field.getKey());append(row,field.getValue());
      }
      rows.add(row.toString());
     }
    }
   }
   Collections.sort(rows);counts[i]=rows.size();
   append(b,Integer.toString(rows.size()));
   for(String row:rows)append(b,row);
  }
  return new Snapshot(counts,sha(b.toString()));
 }
 static void complete(Connection c,U06DeliveryStore.Command cmd)throws SQLException{
  Snapshot snap=snapshot(c,cmd.deliveryEffectId);
  Assertions.assertArrayEquals(new int[]{1,1,1,1,1,1},snap.vector);
  String[][] expected={{"selection_effect_id",cmd.selectionEffectId,"delivery_id",cmd.deliveryId,"authority_status","CONFIRMED_TERMINAL"},
   {"delivery_id",cmd.deliveryId,"question_id",cmd.questionId,"content_fingerprint",cmd.contentFingerprint,
    "endpoint_ref",cmd.endpointRef,"idempotency_key",cmd.idempotencyKey,"intent_status","READY"},
   {"attempt_id",cmd.deliveryEffectId+":attempt:1","delivery_id",cmd.deliveryId,"attempt_status","SYNTHETIC_ACCEPTED"},
   {"receipt_id",cmd.deliveryEffectId+":receipt:1","delivery_id",cmd.deliveryId,
    "receipt_status","SYNTHETIC_CONFIRMED","evidence_type","SYNTHETIC"},
   {"confirmation_evaluation_id",cmd.confirmationEvaluationId,"delivery_id",cmd.deliveryId,
    "confirmation_status","CONFIRMED","confirmation_fingerprint",cmd.confirmationFingerprint},
   {"delivery_id",cmd.deliveryId,"confirmation_status","CONFIRMED","canonical_fingerprint",cmd.confirmationFingerprint}};
  for(int i=0;i<TABLES.length;i++){
   try(PreparedStatement p=c.prepareStatement("SELECT * FROM "+TABLES[i]+" WHERE delivery_effect_id=?")){
    p.setString(1,cmd.deliveryEffectId);
    try(ResultSet r=p.executeQuery()){
     Assertions.assertTrue(r.next());Assertions.assertEquals(cmd.deliveryEffectId,r.getString("delivery_effect_id"));
     for(int k=0;k<expected[i].length;k+=2)Assertions.assertEquals(expected[i][k+1],r.getString(expected[i][k]));
     Assertions.assertFalse(r.next());
    }
   }
  }
 }
 static String sha(String v){
  try{
   byte[] raw=MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8));
   StringBuilder b=new StringBuilder();for(byte n:raw)b.append(String.format(Locale.ROOT,"%02x",n&255));
   return b.toString();
  }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 static void append(StringBuilder b,String v){if(v==null)b.append("-1:");else b.append(v.length()).append(':').append(v);}
 static final String[] EXACT_INSERTS={
  "insert into u06_delivery_authority (selection_effect_id,delivery_effect_id,delivery_id,authority_status,created_at) values (?,?,?,?,?)",
  "insert into u06_delivery_intent (delivery_effect_id,delivery_id,question_id,content_fingerprint,endpoint_ref,idempotency_key,intent_status,created_at) values (?,?,?,?,?,?,?,?)",
  "insert into u06_delivery_attempt (attempt_id,delivery_effect_id,delivery_id,attempt_status,created_at) values (?,?,?,?,?)",
  "insert into u06_delivery_receipt (receipt_id,delivery_effect_id,delivery_id,receipt_status,evidence_type,created_at) values (?,?,?,?,?,?)",
  "insert into u06_delivery_confirmation (confirmation_evaluation_id,delivery_effect_id,delivery_id,confirmation_status,confirmation_fingerprint,created_at) values (?,?,?,?,?,?)",
  "insert into u06_delivery_ledger (delivery_effect_id,delivery_id,confirmation_status,canonical_fingerprint,created_at) values (?,?,?,?,?)"
 };
 static final class Hook extends JdbcTemplate {
  int writes;
  long writerConnectionId;
  String writerRole;
  int writerIsolation;
  boolean writerDeleteDenied;
  final List<String> sqlTrace=new ArrayList<String>();
  Hook(DataSource ds){super(ds);}
  void bindWriter(Connection c)throws SQLException{
   Assertions.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
   Assertions.assertTrue(DataSourceUtils.isConnectionTransactional(c,getDataSource()));
   writerConnectionId=connectionId(c);
   writerRole=actualRole(c);
   Assertions.assertTrue(writerRole.startsWith("stagebp1b_writer@"));
   writerIsolation=c.getTransactionIsolation();
   Assertions.assertEquals(Connection.TRANSACTION_SERIALIZABLE,writerIsolation);
   writerDeleteDenied=assertWriterDenied(c);
  }
  @Override public int update(String sql,Object... args){
   Assertions.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
   Assertions.assertTrue(TransactionSynchronizationManager.hasResource(getDataSource()));
   Assertions.assertTrue(DataSourceUtils.isConnectionTransactional(DataSourceUtils.getConnection(getDataSource()),getDataSource()));
   Assertions.assertTrue(writerConnectionId>0&&writerDeleteDenied);
   Assertions.assertTrue(writes<EXACT_INSERTS.length,"P1B_TOO_MANY_SQL_WRITES");
   String normalized=sql.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
   Assertions.assertEquals(EXACT_INSERTS[writes],normalized,"P1B_INSERT_SIGNATURE_OR_ORDER");
   int rows=super.update(sql,args);
   Assertions.assertEquals(1,rows);
   sqlTrace.add(TABLES[writes]);writes++;return rows;
  }
 }
 static U06DeliveryStore.Snapshot call(StageBP1BTestConfig env,U06DeliveryStore.Command cmd,Hook hook){
  if(TransactionSynchronizationManager.isActualTransactionActive()||
      !TransactionSynchronizationManager.getResourceMap().isEmpty())
   throw new IllegalStateException("P1B_AMBIENT_TX_DENIED");
  Assertions.assertSame(env.source,env.manager.getDataSource());
  TransactionTemplate template=new TransactionTemplate(env.manager);
  template.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
  template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);template.setTimeout(10);
  try{return template.execute(status->{
    Assertions.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
    Connection c=DataSourceUtils.getConnection(env.source);
    try{hook.bindWriter(c);}
    catch(SQLException ex){throw new IllegalStateException("P1B_WRITER_ROLE_OR_ISOLATION",ex);}
    return new JdbcU06DeliveryStore(hook).reconcileConfirmed(cmd);
  });}finally{
   Assertions.assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
   Assertions.assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
  }
 }
 static Hook hook(StageBP1BTestConfig env){return new Hook(env.source);}
 static String sqlInfo(Throwable e){
  Throwable x=e;
  while(x!=null){
   if(x instanceof SQLException){
    SQLException s=(SQLException)x;
    return s.getSQLState()+":"+s.getErrorCode();
   }
   x=x.getCause();
  }
  return "NO_SQL_CODE";
 }
 static Map<String,Object> fact(String name,String caseId,String sourceOutcome,String tx,Snapshot before,Snapshot after,
      String effect,boolean gap,boolean safe,Hook hook,long readerId,long seederId,String error){
  Map<String,Object> f=new LinkedHashMap<String,Object>();
  f.put("testMethod",name);f.put("caseId",caseId);
  f.put("testClass",name.startsWith("partial")?"StageBP1BPartialLedgerTest":"StageBP1BReplayConflictTest");
  f.put("sourceMainSha","e7562bd9b5390c85e14c3c01424c1004b1479ebc");
  f.put("prHeadSha",System.getenv("U07_P1B_PR_HEAD_SHA"));
  f.put("mergeCheckoutSha",System.getenv("GITHUB_SHA"));
  f.put("runId",System.getenv("GITHUB_RUN_ID"));
  f.put("profile",StageBP1BTestConfig.PROFILE);f.put("schema",StageBP1BTestConfig.SCHEMA);
  f.put("caseEffectDigest",sha("P1B-EFFECT:"+effect));f.put("sourceOutcome",sourceOutcome);
  f.put("transaction",tx);f.put("sixBefore",before.vector);f.put("sixAfter",after.vector);
  f.put("graphBefore",before.digest);f.put("graphAfter",after.digest);
  f.put("sourceGapDetected",gap);f.put("fullSixTableReplaySafe",safe);
  f.put("originalInsertCount",hook.writes);
  f.put("sqlTrace",new ArrayList<String>(hook.sqlTrace));
  f.put("sqlSignatureStrict",true);
  f.put("writerConnectionId",hook.writerConnectionId);
  f.put("writerRole",hook.writerRole);
  f.put("writerIsolation",hook.writerIsolation);
  f.put("writerDeleteDenied",hook.writerDeleteDenied);
  f.put("readerConnectionId",readerId);f.put("seederConnectionId",seederId);
  f.put("readerRole","stagebp1b_reader");f.put("seederRole",seederId>0?"stagebp1b_seeder":"NOT_USED");
  f.put("readerDeleteDenied",true);f.put("seederDdlDenied",seederId>0);
  f.put("roleIdentityPhysicallyChecked",true);
  f.put("sqlError",error);f.put("downstreamAuthority",false);
  f.put("testVerdict",gap?"SOURCE_GAP_CONFIRMED":"SOURCE_CHARACTERIZATION_PASS");
  return f;
 }
 static void emit(Map<String,Object> f){
  try{
   java.nio.file.Path dir=Paths.get("target","stage-b-p1b-observations");Files.createDirectories(dir);
   new ObjectMapper().writeValue(dir.resolve(f.get("testMethod")+".json").toFile(),f);
  }catch(java.io.IOException e){throw new IllegalStateException("P1B_EVIDENCE_IO",e);}
 }
 /** Strictly seeder-owned rows; no candidate-writer seeding. */
 static void insertSeed(Connection c,int index,U06DeliveryStore.Command x)throws SQLException{
  String[] sql={
   "INSERT INTO u06_delivery_authority (selection_effect_id,delivery_effect_id,delivery_id,authority_status,created_at) VALUES (?,?,?,?,?)",
   "INSERT INTO u06_delivery_intent (delivery_effect_id,delivery_id,question_id,content_fingerprint,endpoint_ref,idempotency_key,intent_status,created_at) VALUES (?,?,?,?,?,?,?,?)",
   "INSERT INTO u06_delivery_attempt (attempt_id,delivery_effect_id,delivery_id,attempt_status,created_at) VALUES (?,?,?,?,?)",
   "INSERT INTO u06_delivery_receipt (receipt_id,delivery_effect_id,delivery_id,receipt_status,evidence_type,created_at) VALUES (?,?,?,?,?,?)",
   "INSERT INTO u06_delivery_confirmation (confirmation_evaluation_id,delivery_effect_id,delivery_id,confirmation_status,confirmation_fingerprint,created_at) VALUES (?,?,?,?,?,?)",
   "INSERT INTO u06_delivery_ledger (delivery_effect_id,delivery_id,confirmation_status,canonical_fingerprint,created_at) VALUES (?,?,?,?,?)"
  };
  String[][] args={
   {x.selectionEffectId,x.deliveryEffectId,x.deliveryId,"CONFIRMED_TERMINAL",x.createdAt},
   {x.deliveryEffectId,x.deliveryId,x.questionId,x.contentFingerprint,x.endpointRef,x.idempotencyKey,"READY",x.createdAt},
   {x.deliveryEffectId+":attempt:1",x.deliveryEffectId,x.deliveryId,"SYNTHETIC_ACCEPTED",x.createdAt},
   {x.deliveryEffectId+":receipt:1",x.deliveryEffectId,x.deliveryId,"SYNTHETIC_CONFIRMED","SYNTHETIC",x.createdAt},
   {x.confirmationEvaluationId,x.deliveryEffectId,x.deliveryId,"CONFIRMED",x.confirmationFingerprint,x.createdAt},
   {x.deliveryEffectId,x.deliveryId,"CONFIRMED",x.confirmationFingerprint,x.createdAt}
  };
  try(PreparedStatement p=c.prepareStatement(sql[index])){
   for(int i=0;i<args[index].length;i++)p.setString(i+1,args[index][i]);
   Assertions.assertEquals(1,p.executeUpdate());
  }
 }
 static final class CleanupResult {
  final Snapshot state;
  final long seederId,readerId;
  final String effectDigest;
  CleanupResult(Snapshot state,long seederId,long readerId,String effectDigest){
   this.state=state;this.seederId=seederId;this.readerId=readerId;this.effectDigest=effectDigest;
  }
 }
 static CleanupResult cleanup(Connection s,U06DeliveryStore.Command x)throws SQLException{
  Assertions.assertTrue(actualRole(s).startsWith("stagebp1b_seeder@"));
  long seedId=connectionId(s);
  for(int i=TABLES.length-1;i>=0;i--){
   try(PreparedStatement p=s.prepareStatement("DELETE FROM "+TABLES[i]+" WHERE delivery_effect_id=?")){
    p.setString(1,x.deliveryEffectId);p.executeUpdate();
   }
  }
  s.commit();
  try(Connection r=reader()){
   assertReaderDenied(r);
   long readerId=connectionId(r);
   Assertions.assertNotEquals(seedId,readerId);
   Snapshot observed=snapshot(r,x.deliveryEffectId);
   Assertions.assertArrayEquals(new int[6],observed.vector);
   return new CleanupResult(observed,seedId,readerId,sha("P1B-EFFECT:"+x.deliveryEffectId));
  }
 }
 static void bindCleanup(Map<String,Object> fact,CleanupResult cleanup){
  Assertions.assertNotNull(cleanup);
  fact.put("cleanupVerified",true);
  fact.put("fixtureCleanupAfter",cleanup.state.vector);
  fact.put("fixtureCleanupGraphSha256",cleanup.state.digest);
  fact.put("cleanupSeederConnectionId",cleanup.seederId);
  fact.put("cleanupReaderConnectionId",cleanup.readerId);
  fact.put("cleanupEffectDigest",cleanup.effectDigest);
 }
 static boolean fullSixCoherent(Connection c,U06DeliveryStore.Command cmd)throws SQLException{
  if(!Arrays.equals(snapshot(c,cmd.deliveryEffectId).vector,new int[]{1,1,1,1,1,1}))return false;
  complete(c,cmd);return true;
 }
}
