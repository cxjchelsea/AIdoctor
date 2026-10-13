package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.forensics;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;

final class StageBFObserverEvidence {
 static final String[] TABLES={"u06_delivery_authority","u06_delivery_intent","u06_delivery_attempt",
 "u06_delivery_receipt","u06_delivery_confirmation","u06_delivery_ledger"};
 static final Set<String> TABLE_ALLOW=new HashSet<String>(Arrays.asList(TABLES));
 static final String[] P_S={"data_locks","data_lock_waits","threads"};
 static final Set<String> ROOT_KEYS=new HashSet<String>(Arrays.asList(
  "schema","sourceMainSha","candidateHead","checkoutSha","runId","runAttempt","profile",
  "database","L1Verdict","sourceReplayGap","downstreamAuthority","businessCompletion",
  "forensicVerdict","caseId","caseAcceptance","observerRole","rolesVerified",
  "roleNegativePassed","roleNegativeMatrix","actorA","actorB","bothReady",
  "executorTerminated","harnessTimeout","observerSampleCount","observerSamples",
  "readerId","effectAVector","effectBVector","sourceVerdict","liveLockGraphVerdict",
  "readerRole","sampleEpochs","observerConnectionId","writerAThreadId","writerBThreadId",
  "selectedEdges","completeSixIdentityA","completeSixIdentityB","harnessFailure",
  "physicalReadStatus","physicalReadSqlState"));
 static final Set<String> ACTOR_KEYS=new HashSet<String>(Arrays.asList(
  "effectDigest","connectionId","outcome","sqlError","txReturned","resourcesClear",
  "beginNano","endNano","successfulInsertCount"));
 static final Set<String> EDGE_KEYS=new HashSet<String>(Arrays.asList(
  "epoch","requester","blocker","table","index","engine","requestMode","requestStatus",
  "blockMode","blockStatus","requestType","blockType","requestThread","blockThread"));
 static final Set<String> INDEX_ALLOW=new HashSet<String>(Arrays.asList(
  "PRIMARY","uk_u06_delivery_authority_effect","uk_u06_delivery_intent_id",
  "uk_u06_delivery_intent_idem"));
 private StageBFObserverEvidence(){}
 static long id(Connection c)throws SQLException{
  try(Statement p=c.createStatement()){p.setQueryTimeout(2);
   try(ResultSet r=p.executeQuery("SELECT CONNECTION_ID()")){Assertions.assertTrue(r.next());return r.getLong(1);}
  }
 }
 static String user(Connection c)throws SQLException{
  try(Statement p=c.createStatement()){p.setQueryTimeout(2);
   try(ResultSet r=p.executeQuery("SELECT CURRENT_USER()")){Assertions.assertTrue(r.next());return r.getString(1);}
  }
 }
 static void deny(Connection c,String sql){
  SQLException e=Assertions.assertThrows(SQLException.class,()->{
   try(Statement p=c.createStatement()){p.setQueryTimeout(2);p.execute(sql);}
  });
  Assertions.assertEquals("42000",e.getSQLState());
 }
 static void permissions(Connection w,Connection r,Connection o)throws SQLException{
  Assertions.assertTrue(user(w).startsWith("stagebf_writer@"));
  Assertions.assertTrue(user(r).startsWith("stagebf_reader@"));
  Assertions.assertTrue(user(o).startsWith("stagebf_observer_ps@"));
  deny(w,"DELETE FROM u06_delivery_ledger WHERE delivery_effect_id='f-denied'");
  deny(w,"UPDATE u06_delivery_ledger SET confirmation_status='X' WHERE delivery_effect_id='f-denied'");
  deny(r,"INSERT INTO u06_delivery_ledger(delivery_effect_id) VALUES ('f-denied')");
  deny(r,"DELETE FROM u06_delivery_ledger WHERE delivery_effect_id='f-denied'");
  deny(o,"SELECT delivery_effect_id FROM u07_d5o2_f_deadlock.u06_delivery_ledger LIMIT 1");
  deny(o,"DELETE FROM u07_d5o2_f_deadlock.u06_delivery_ledger WHERE delivery_effect_id='f-denied'");
  deny(o,"UPDATE u07_d5o2_f_deadlock.u06_delivery_ledger SET confirmation_status='X' WHERE delivery_effect_id='f-denied'");
  deny(o,"INSERT INTO u07_d5o2_f_deadlock.u06_delivery_ledger(delivery_effect_id) VALUES ('f-denied')");
  deny(o,"CREATE TABLE u07_d5o2_f_deadlock.stagebf_illegal(a int)");
  deny(o,"SHOW ENGINE INNODB STATUS");
  try(Statement p=o.createStatement()){p.setQueryTimeout(2);
   try(ResultSet rs=p.executeQuery("SHOW GRANTS")){
    while(rs.next()){
     String g=rs.getString(1).toUpperCase(java.util.Locale.ROOT);
     Assertions.assertFalse(g.contains("PROCESS")||g.contains("SUPER")||g.contains("FILE")||
      g.contains("GRANT OPTION")||g.contains("ALL PRIVILEGES")||g.contains("RELOAD"));
    }
   }
  }
  for(String table:P_S)try(Statement p=o.createStatement()){p.setQueryTimeout(2);
   try(ResultSet q=p.executeQuery("SELECT COUNT(*) FROM performance_schema."+table)){Assertions.assertTrue(q.next());}
  }
 }
 static String digest(String s){
  try{
   byte[] b=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
   StringBuilder x=new StringBuilder();
   for(byte y:b)x.append(String.format(java.util.Locale.ROOT,"%02x",y&255));
   return x.toString();
  }catch(Exception e){throw new IllegalStateException("DIGEST_UNAVAILABLE");}
 }
 static void validateSafe(Map<String,Object> m){
  for(Map.Entry<String,Object> e:m.entrySet()){
   if(!ROOT_KEYS.contains(e.getKey()))throw new IllegalStateException("FORENSIC_UNKNOWN_FIELD");
   Object v=e.getValue();
   if("actorA".equals(e.getKey())||"actorB".equals(e.getKey())){
    if(!(v instanceof Map))throw new IllegalStateException("FORENSIC_ACTOR_TYPE");
    for(Object k:((Map<?,?>)v).keySet())if(!ACTOR_KEYS.contains(k))
     throw new IllegalStateException("FORENSIC_ACTOR_EXTRA_FIELD");
   }else if("selectedEdges".equals(e.getKey())){
    if(!(v instanceof List)||((List<?>)v).size()>24)throw new IllegalStateException("FORENSIC_EDGES_SIZE");
    for(Object item:(List<?>)v){
     if(!(item instanceof Map))throw new IllegalStateException("FORENSIC_EDGE_TYPE");
     Map<?,?> edge=(Map<?,?>)item;
     if(!EDGE_KEYS.containsAll(edge.keySet()))throw new IllegalStateException("FORENSIC_EDGE_EXTRA_FIELD");
     if(!TABLE_ALLOW.contains(edge.get("table"))||!INDEX_ALLOW.contains(edge.get("index")))
      throw new IllegalStateException("FORENSIC_EDGE_DDL");
     if(!"A".equals(edge.get("requester"))&&!"B".equals(edge.get("requester")))
      throw new IllegalStateException("FORENSIC_REQUESTER");
     if(!"A".equals(edge.get("blocker"))&&!"B".equals(edge.get("blocker")))
      throw new IllegalStateException("FORENSIC_BLOCKER");
    }
   }else if(v instanceof Map && !"roleNegativeMatrix".equals(e.getKey()))
    throw new IllegalStateException("FORENSIC_NESTED_FIELD");
  }
  String x;
  try{x=new ObjectMapper().writeValueAsString(m);}catch(Exception e){throw new IllegalStateException("JSON");}
  if(x.getBytes(StandardCharsets.UTF_8).length>65536)throw new IllegalStateException("EVIDENCE_SIZE");
  for(String bad:new String[]{"F_FAKE_SECRET_9271","FAKE_PATIENT_6271","13900009999",
    "LOCK_DATA","SELECT *","INSERT INTO","SHOW ENGINE INNODB STATUS","rawSql","password","PROCESSLIST_INFO",
    "jdbc:mysql://","CREATE USER","IDENTIFIED BY","FAKE_UNKNOWN_SECRET_4192"})
   if(x.contains(bad))throw new IllegalStateException("REDACTION_REJECTED");
 }
 static Map<String,Object> evidence(String verdict,Map<String,Object> more){
  Map<String,Object> m=new LinkedHashMap<String,Object>();
  m.put("schema","U07_F_ISOLATED_L0_EVIDENCE_V1");
  m.put("sourceMainSha","20a54b4a7478641d8b8d8c12c6213b8e2ea73acc");
  m.put("candidateHead",System.getenv("U07_F_PR_HEAD_SHA"));
  m.put("checkoutSha",System.getenv("GITHUB_SHA"));
  m.put("runId",System.getenv("GITHUB_RUN_ID"));
  m.put("runAttempt",System.getenv("GITHUB_RUN_ATTEMPT"));
  m.put("profile",StageBFTestConfig.PROFILE);
  m.put("database",StageBFTestConfig.DB);
  m.put("L1Verdict","L1_NOT_ENABLED");m.put("sourceReplayGap","CONFIRMED_NOT_SAFE");
  m.put("downstreamAuthority",false);m.put("businessCompletion","NOT_AUTHORIZED_TO_ATTEST");
  for(String key:more.keySet())if(m.containsKey(key)||!ROOT_KEYS.contains(key))
   throw new IllegalStateException("FORENSIC_RESERVED_OR_UNKNOWN_FIELD");
  m.put("forensicVerdict",verdict);m.putAll(more);validateSafe(m);return m;
 }
 static void emit(String file,Map<String,Object> m){
  try{
   Path p=Paths.get("target","stage-b-f-observations");Files.createDirectories(p);
   new ObjectMapper().writeValue(p.resolve(file+".json").toFile(),m);
  }catch(Exception e){throw new IllegalStateException("EVIDENCE_EMIT_FAILED");}
 }
 static final class Sample{
  long aThread,bThread,observerId;int count,edges,opposing;
  boolean observedAtoB,observedBtoA;
  String verdict="NO_EDGE_CAPTURED";
  List<Map<String,Object>> selected=new ArrayList<Map<String,Object>>();
 }
 static Sample observe(Connection o,long a,long b,AtomicBoolean stop)throws SQLException,InterruptedException{
  Sample out=new Sample();out.observerId=id(o);
  final long start=System.nanoTime(),deadline=start+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
  long previous=0;
  while(!stop.get()&&out.count<100&&System.nanoTime()<deadline){
   long now=System.nanoTime();
   if(previous!=0&&now-previous<java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(50)){
    long wait=java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(50)-(now-previous);
    java.util.concurrent.TimeUnit.NANOSECONDS.sleep(wait);
   }
   previous=System.nanoTime();out.count++;
   Map<Long,Long> map=new HashMap<Long,Long>();
   try(PreparedStatement p=o.prepareStatement("SELECT PROCESSLIST_ID,THREAD_ID FROM performance_schema.threads WHERE PROCESSLIST_ID IN (?,?)")){
    p.setQueryTimeout(2);p.setLong(1,a);p.setLong(2,b);
    try(ResultSet r=p.executeQuery()){while(r.next())map.put(r.getLong(1),r.getLong(2));}
   }
   Long at=map.get(a),bt=map.get(b);
   if(at!=null&&bt!=null){
    out.aThread=at;out.bThread=bt;
    String sql="SELECT w.REQUESTING_THREAD_ID,w.BLOCKING_THREAD_ID,"+
      "req.OBJECT_SCHEMA,req.OBJECT_NAME,req.INDEX_NAME,req.LOCK_TYPE,req.LOCK_MODE,req.LOCK_STATUS,"+
      "blk.OBJECT_SCHEMA,blk.OBJECT_NAME,blk.INDEX_NAME,blk.LOCK_TYPE,blk.LOCK_MODE,blk.LOCK_STATUS,req.ENGINE "+
      "FROM performance_schema.data_lock_waits w "+
      "JOIN performance_schema.data_locks req ON req.ENGINE=w.ENGINE AND req.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID "+
      "JOIN performance_schema.data_locks blk ON blk.ENGINE=w.ENGINE AND blk.ENGINE_LOCK_ID=w.BLOCKING_ENGINE_LOCK_ID "+
      "WHERE w.REQUESTING_THREAD_ID IN (?,?) AND w.BLOCKING_THREAD_ID IN (?,?)";
    try(PreparedStatement p=o.prepareStatement(sql)){
     p.setQueryTimeout(2);p.setLong(1,at);p.setLong(2,bt);p.setLong(3,at);p.setLong(4,bt);
     try(ResultSet r=p.executeQuery()){
      boolean ab=false,ba=false;
      while(r.next()){
       long request=r.getLong(1),block=r.getLong(2);
       String schema=r.getString(3),table=r.getString(4),idx=r.getString(5);
       if(!StageBFTestConfig.DB.equals(schema)||!TABLE_ALLOW.contains(table))
        throw new IllegalStateException("FORENSIC_UNAUTHORIZED_SCHEMA");
       if(!INDEX_ALLOW.contains(idx)||!StageBFTestConfig.DB.equals(r.getString(9))||
         !table.equals(r.getString(10))||!INDEX_ALLOW.contains(r.getString(11)))
        throw new IllegalStateException("FORENSIC_DDL_INDEX_OR_BLOCK_MISMATCH");
       if(!(request==at&&block==bt)&&!(request==bt&&block==at))
        throw new IllegalStateException("FORENSIC_UNBOUND_EDGE");
       Map<String,Object> edge=new LinkedHashMap<String,Object>();
       edge.put("epoch",out.count);
       edge.put("requester",request==at?"A":"B");edge.put("blocker",block==at?"A":"B");
       edge.put("requestThread",request);edge.put("blockThread",block);
       edge.put("table",table);edge.put("index",idx);
       edge.put("engine",r.getString(15));
       int[] columns={6,7,8,12,13,14};
       String[] keys={"requestType","requestMode","requestStatus","blockType","blockMode","blockStatus"};
       for(int j=0;j<columns.length;j++){
        String v=r.getString(columns[j]);
        if(v==null||!v.matches("[A-Za-z0-9_, ]{1,64}"))
         throw new IllegalStateException("FORENSIC_LOCK_ENUM_REJECTED");
        edge.put(keys[j],v);
       }
       if(!"INNODB".equals(edge.get("engine")))
        throw new IllegalStateException("FORENSIC_ENGINE");
       if(!"WAITING".equals(edge.get("requestStatus"))||!"GRANTED".equals(edge.get("blockStatus")))
        throw new IllegalStateException("FORENSIC_LOCK_STATE");
       if(out.selected.size()<24)out.selected.add(edge);
       out.edges++;
       if(request==at)ab=true;else ba=true;
      }
      if(ab)out.observedAtoB=true;
      if(ba)out.observedBtoA=true;
      if(ab&&ba)out.opposing++;
     }
    }
   }
  }
  out.verdict=out.opposing>0?"LIVE_TWO_WRITER_CYCLE_BOUND":
   out.edges>0?"PARTIAL_EDGE_ONLY":"NO_EDGE_CAPTURED";
  return out;
 }
 static void negativeRedactionTests(){
  Map<String,Object> unknown=new HashMap<String,Object>();unknown.put("invented","FAKE_UNKNOWN_SECRET_4192");
  Assertions.assertThrows(IllegalStateException.class,()->validateSafe(unknown));
  Map<String,Object> fake=new HashMap<String,Object>();fake.put("caseId","FAKE_UNKNOWN_SECRET_4192");
  Assertions.assertThrows(IllegalStateException.class,()->validateSafe(fake));
  for(String bad:new String[]{"F_FAKE_SECRET_9271","FAKE_PATIENT_6271","13900009999",
      "LOCK_DATA","SELECT *","INSERT INTO","password"}){
   Map<String,Object> m=new HashMap<String,Object>();m.put("unauthorized",bad);
   Assertions.assertThrows(IllegalStateException.class,()->validateSafe(m));
  }
 }
 static int[] rows(Connection c,String effect)throws SQLException{
  int[] result=new int[6];
  for(int i=0;i<6;i++)try(PreparedStatement p=c.prepareStatement(
    "SELECT COUNT(*) FROM "+TABLES[i]+" WHERE delivery_effect_id=?")){
   p.setQueryTimeout(2);p.setString(1,effect);
   try(ResultSet r=p.executeQuery()){Assertions.assertTrue(r.next());result[i]=r.getInt(1);}
  }
  return result;
 }
}
