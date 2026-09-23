package com.aidoctor.diagnosis.runtime.u06.trace;
import org.springframework.jdbc.core.JdbcTemplate;
public final class JdbcU06GovernedExecutionTraceStore implements U06GovernedExecutionTraceStore{
 private final JdbcTemplate jdbc;public JdbcU06GovernedExecutionTraceStore(JdbcTemplate jdbc){if(jdbc==null)throw new IllegalArgumentException("jdbc required");this.jdbc=jdbc;}
 public void start(String t,String c,String a,String m,String p,String fp,String at){int n=jdbc.update("UPDATE u06_governed_execution_trace SET lifecycle_status=?,updated_at=? WHERE trace_id=?","STARTED",at,t);if(n==0)jdbc.update("INSERT INTO u06_governed_execution_trace (trace_id,consultation_id,admission_id,mode,execution_profile,lifecycle_status,outcome_status,payload_fingerprint,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?)",t,c,a,m,p,"STARTED",null,fp,at,at);}
 public void complete(String t,String life,String outcome,String at){if(jdbc.update("UPDATE u06_governed_execution_trace SET lifecycle_status=?,outcome_status=?,updated_at=? WHERE trace_id=?",life,outcome,at,t)!=1)throw new IllegalStateException("U06_TRACE_NOT_FOUND");}
}
