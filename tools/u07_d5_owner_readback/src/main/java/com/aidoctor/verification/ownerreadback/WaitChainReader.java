package com.aidoctor.verification.ownerreadback;
import java.sql.*;import java.util.*;
import org.springframework.jdbc.datasource.DataSourceUtils;import org.springframework.transaction.TransactionDefinition;import org.springframework.transaction.support.*;
import static com.aidoctor.verification.ownerreadback.WaitChainObservation.Status.*;
final class WaitChainReader {
 interface Probe {void afterRoot();}
 final OwnerReadbackResources resources;final ScopeAuthorizationPort scope;final Probe probe;
 WaitChainReader(OwnerReadbackResources r,ScopeAuthorizationPort s){this(r,s,()->{});}
 WaitChainReader(OwnerReadbackResources r,ScopeAuthorizationPort s,Probe p){resources=Objects.requireNonNull(r);scope=Objects.requireNonNull(s);probe=Objects.requireNonNull(p);}
 static final class Halt extends RuntimeException {final WaitChainObservation.Status status;final String reason;Halt(WaitChainObservation.Status s,String r){status=s;reason=r;}}
 static void stop(WaitChainObservation.Status s,String r){throw new Halt(s,r);}
 static void require(boolean condition,String reason){if(!condition)stop(INCONSISTENT,reason);}
 WaitChainObservation read(OwnerReadbackRequest request){
  if(request==null||!request.valid())return out(INVALID_INPUT,"INPUT_INVALID",false);
  ScopeAuthorizationPort.Decision decision;
  try{decision=scope.evaluate(request);}catch(RuntimeException e){return out(UNAVAILABLE,"SCOPE_UNAVAILABLE",false);}
  if(decision==null||decision.kind==null||decision.kind==ScopeAuthorizationPort.Kind.UNAVAILABLE)return out(UNAVAILABLE,"SCOPE_UNAVAILABLE",false);
  if(decision.kind==ScopeAuthorizationPort.Kind.DENY)return out(DENIED,"SCOPE_DENIED",false);
  if(decision.evidence==null||!decision.evidence.matches(request))return out(DENIED,"SCOPE_BINDING_INVALID",false);
  if(TransactionSynchronizationManager.isActualTransactionActive())return out(UNAVAILABLE,"OUTER_TRANSACTION_PRESENT",true);
  TransactionTemplate tx=new TransactionTemplate(resources.manager);tx.setReadOnly(true);tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);tx.setTimeout(15);
  try{return tx.execute(status->{
   Connection c=DataSourceUtils.getConnection(resources.ds);
   try{resources.preflight(c);long connection=id();return observe(request,c,connection);}catch(SQLException e){throw new IllegalStateException("DB_UNAVAILABLE",e);}finally{DataSourceUtils.releaseConnection(c,resources.ds);}
  });}catch(Halt h){return out(h.status,h.reason,true);}catch(RuntimeException e){return out(UNAVAILABLE,"RESOURCE_PRECONDITION_FAILED".equals(e.getMessage())?e.getMessage():"DB_UNAVAILABLE",true);}
 }
 WaitChainObservation out(WaitChainObservation.Status s,String reason,boolean authorized){return new WaitChainObservation(s,reason,authorized,null);}
 long id(){return resources.jdbc.queryForObject("SELECT CONNECTION_ID()",Long.class);}
 Map<String,Object> row(String table,String key,String value,Connection c,long physical){
  try{resources.physical(c);}catch(SQLException e){throw new IllegalStateException("DB_UNAVAILABLE",e);}if(id()!=physical)throw new IllegalStateException("RESOURCE_PRECONDITION_FAILED");
  List<Map<String,Object>> rows=resources.jdbc.queryForList("SELECT "+columns(table)+" FROM "+table+" WHERE "+key+"=?",value);if(rows.size()>1)stop(INCONSISTENT,"OWNER_LINK_INVALID");return rows.isEmpty()?null:rows.get(0);
 }
 // All SQL identifiers are internal constants; no request-provided identifiers.
 String columns(String table){
  switch(table){case "clinical_consultation":return "consultation_id,cdp_id,row_version,lifecycle_status,current_wait_effect_id";
   case "clinical_runtime_binding":return "consultation_id,cdp_id,thread_id,runtime_authority,scope_version,capability_set_version,contract_version";
   case "clinical_runtime_thread_state":return "thread_id,consultation_id,row_version,runtime_status,current_run_id,current_wait_checkpoint_id,current_wait_effect_id";
   case "clinical_runtime_run":return "run_id,thread_id,consultation_id,based_on_clinical_state_version,status";
   case "clinical_consultation_wait_effect":return "wait_effect_id,consultation_id,parent_delivered_wait_effect_id,question_id,delivery_id,effect_status,expected_prior_lifecycle,committed_lifecycle,expected_prior_row_version,committed_row_version";
   case "clinical_runtime_wait_checkpoint":return "checkpoint_id,consultation_id,thread_id,run_id,question_id,pending_question_ref,question_selection_effect_id,question_delivery_effect_id,question_delivered_wait_effect_id,delivery_id,delivery_confirmation_ref,clinical_state_version,consultation_wait_effect_id,dependency_binding_ref,question_policy_ref,payload_fingerprint,checkpoint_status";
   default:throw new IllegalArgumentException("fixed table required");}
 }
 static String text(Map<String,Object> m,String k){Object v=m.get(k);return v==null?null:v.toString();}
 static boolean same(Map<String,Object> m,String k,String v){return Objects.equals(text(m,k),v);}
 static long number(Map<String,Object> m,String k,String reason){Object v=m.get(k);require(v instanceof Number,reason);return ((Number)v).longValue();}
 static boolean known(String v,String...values){return Arrays.asList(values).contains(v);}
 void run(Map<String,Object> r,OwnerReadbackRequest req,String reason){require(r!=null,reason);require(same(r,"consultation_id",req.consultationId)&&same(r,"thread_id",req.threadId),reason);require(known(text(r,"status"),"OPEN","CLOSED")&&number(r,"based_on_clinical_state_version",reason)>=0,reason);}
 WaitChainObservation observe(OwnerReadbackRequest req,Connection c,long physical){
  Map<String,Object> root=row("clinical_consultation","consultation_id",req.consultationId,c,physical);probe.afterRoot();
  if(root==null)return out(ABSENT,"CONSULTATION_ABSENT",true);
  long version=number(root,"row_version","CONSULTATION_SHAPE_INVALID");String lifecycle=text(root,"lifecycle_status"),wait=text(root,"current_wait_effect_id");
  require(version>=0&&known(lifecycle,"ACTIVE","WAITING_USER"),"CONSULTATION_SHAPE_INVALID");require(!("ACTIVE".equals(lifecycle)&&wait!=null)&&!("WAITING_USER".equals(lifecycle)&&wait==null),"CONSULTATION_WAIT_SHAPE_INVALID");
  if("ACTIVE".equals(lifecycle))return out(STALE,"NO_CURRENT_WAIT",true);
  Map<String,Object> binding=row("clinical_runtime_binding","consultation_id",req.consultationId,c,physical),thread=row("clinical_runtime_thread_state","thread_id",req.threadId,c,physical),requestedRun=row("clinical_runtime_run","run_id",req.runId,c,physical);
  require(binding!=null&&thread!=null,"OWNER_LINK_INVALID");require(same(binding,"cdp_id",text(root,"cdp_id"))&&same(binding,"thread_id",req.threadId)&&same(thread,"consultation_id",req.consultationId),"OWNER_LINK_INVALID");
  require(same(binding,"runtime_authority","CLINICAL_RUNTIME_V1")&&same(binding,"scope_version","o1-scope-v1")&&same(binding,"capability_set_version","o1-cap-v1")&&same(binding,"contract_version","1.1.0"),"OWNER_ENUM_INVALID");
  long threadVersion=number(thread,"row_version","OWNER_LINK_INVALID");require(threadVersion>=0,"OWNER_LINK_INVALID");String ts=text(thread,"runtime_status");require(known(ts,"ACTIVE","WAIT_CHECKPOINTED","AWAITING_USER"),"OWNER_ENUM_INVALID");run(requestedRun,req,"OWNER_LINK_INVALID");
  Map<String,Object> effect=row("clinical_consultation_wait_effect","wait_effect_id",wait,c,physical);require(effect!=null,"WAIT_EFFECT_INVALID");
  require(same(effect,"consultation_id",req.consultationId)&&same(effect,"effect_status","COMMITTED")&&same(effect,"expected_prior_lifecycle","ACTIVE")&&same(effect,"committed_lifecycle","WAITING_USER"),"WAIT_EFFECT_INVALID");
  long prior=number(effect,"expected_prior_row_version","WAIT_EFFECT_INVALID"),committed=number(effect,"committed_row_version","WAIT_EFFECT_INVALID");require(prior>=0&&prior<Long.MAX_VALUE&&committed==prior+1,"WAIT_EFFECT_INVALID");
  for(String key:new String[]{"question_id","delivery_id","parent_delivered_wait_effect_id"})require(OwnerReadbackRequest.id(text(effect,key)),"WAIT_EFFECT_INVALID");require(version>=committed,"VERSION_REGRESSION");
  String cr=text(thread,"current_run_id"),cp=text(thread,"current_wait_checkpoint_id"),parent=text(thread,"current_wait_effect_id");
  if("ACTIVE".equals(ts)){require(cr==null&&cp==null&&parent==null,"THREAD_WAIT_SHAPE_INVALID");return out(STALE,"THREAD_WAIT_NOT_ESTABLISHED",true);}
  require(OwnerReadbackRequest.id(cr)&&OwnerReadbackRequest.id(cp)&&OwnerReadbackRequest.id(parent),"THREAD_WAIT_SHAPE_INVALID");
  Map<String,Object> currentRun=cr.equals(req.runId)?requestedRun:row("clinical_runtime_run","run_id",cr,c,physical),checkpoint=row("clinical_runtime_wait_checkpoint","checkpoint_id",cp,c,physical);
  require(currentRun!=null&&checkpoint!=null,"CURRENT_OWNER_MISSING");run(currentRun,req,"CHECKPOINT_LINK_INVALID");
  require(same(checkpoint,"checkpoint_status","ACTIVE")&&same(checkpoint,"consultation_id",req.consultationId)&&same(checkpoint,"thread_id",req.threadId)&&same(checkpoint,"run_id",cr)&&same(checkpoint,"consultation_wait_effect_id",wait),"CHECKPOINT_LINK_INVALID");
  require(same(checkpoint,"question_delivered_wait_effect_id",parent)&&Objects.equals(parent,text(effect,"parent_delivered_wait_effect_id")),"CHECKPOINT_LINK_INVALID");
  require(same(checkpoint,"question_id",text(effect,"question_id"))&&same(checkpoint,"delivery_id",text(effect,"delivery_id")),"CHECKPOINT_LINK_INVALID");
  long captured=number(checkpoint,"clinical_state_version","CHECKPOINT_LINK_INVALID");require(captured>=0&&captured==number(currentRun,"based_on_clinical_state_version","CHECKPOINT_LINK_INVALID"),"CHECKPOINT_LINK_INVALID");
  for(String key:new String[]{"pending_question_ref","question_selection_effect_id","question_delivery_effect_id","delivery_confirmation_ref","payload_fingerprint"})require(text(checkpoint,key)!=null&&!text(checkpoint,key).trim().isEmpty(),"CHECKPOINT_LINK_INVALID");
  if(!cp.equals(req.checkpointId)){Map<String,Object> old=row("clinical_runtime_wait_checkpoint","checkpoint_id",req.checkpointId,c,physical);if(old!=null)require(same(old,"consultation_id",req.consultationId)&&same(old,"thread_id",req.threadId)&&same(old,"run_id",req.runId),"REQUEST_CHECKPOINT_SCOPE_INVALID");}
  if(!cr.equals(req.runId)||!cp.equals(req.checkpointId))return out(STALE,"REQUEST_NOT_CURRENT",true);if(version>committed)return out(STALE,"CONSULTATION_ADVANCED",true);if("WAIT_CHECKPOINTED".equals(ts))return out(STALE,"THREAD_WAIT_CHECKPOINTED",true);
  require("AWAITING_USER".equals(ts),"UNSUPPORTED_SHAPE");Map<String,Object> payload=new LinkedHashMap<>();payload.put("consultationId",req.consultationId);payload.put("threadId",req.threadId);payload.put("runId",cr);payload.put("checkpointId",cp);payload.put("waitEffectId",wait);payload.put("cdpId",text(root,"cdp_id"));payload.put("consultationRowVersion",version);payload.put("threadRowVersion",threadVersion);payload.put("capturedClinicalVersion",captured);
  return new WaitChainObservation(MATCH,"WAIT_CHAIN_MATCH",true,payload);
 }
}
