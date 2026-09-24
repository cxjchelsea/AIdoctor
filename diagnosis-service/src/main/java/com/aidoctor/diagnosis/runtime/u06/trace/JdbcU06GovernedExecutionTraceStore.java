package com.aidoctor.diagnosis.runtime.u06.trace;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcU06GovernedExecutionTraceStore implements U06GovernedExecutionTraceStore {
    private final JdbcTemplate jdbc;

    public JdbcU06GovernedExecutionTraceStore(JdbcTemplate jdbc){
        if(jdbc==null)throw new IllegalArgumentException("jdbc required");
        this.jdbc=jdbc;
    }

    public void start(String t,String c,String a,String m,String p,String fp,String at){
        Existing existing=find(t);
        if(existing!=null){
            if(!existing.matches(c,a,m,p,fp))
                throw new IllegalStateException("U06_TRACE_REPLAY_CONFLICT");
            return;
        }
        jdbc.update("INSERT INTO u06_governed_execution_trace (trace_id,consultation_id,admission_id,mode,execution_profile,lifecycle_status,outcome_status,payload_fingerprint,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                t,c,a,m,p,"STARTED",null,fp,at,at);
    }

    public void complete(String t,String life,String outcome,String at){
        Existing existing=find(t);
        if(existing==null)throw new IllegalStateException("U06_TRACE_NOT_FOUND");
        if(!"STARTED".equals(existing.lifecycleStatus)){
            if(eq(existing.lifecycleStatus,life)&&eq(existing.outcomeStatus,outcome))return;
            throw new IllegalStateException("U06_TRACE_TERMINAL_CONFLICT");
        }
        if(jdbc.update("UPDATE u06_governed_execution_trace SET lifecycle_status=?,outcome_status=?,updated_at=? WHERE trace_id=? AND lifecycle_status=?",
                life,outcome,at,t,"STARTED")!=1)
            throw new IllegalStateException("U06_TRACE_TERMINALIZATION_RACE");
    }

    private Existing find(String traceId){
        try{
            return jdbc.queryForObject(
                    "SELECT consultation_id,admission_id,mode,execution_profile,lifecycle_status,outcome_status,payload_fingerprint FROM u06_governed_execution_trace WHERE trace_id=?",
                    new Object[]{traceId},
                    (rs,n)->new Existing(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7)));
        }catch(EmptyResultDataAccessException absent){
            return null;
        }
    }

    private static final class Existing{
        final String consultationId,admissionId,mode,profile,lifecycleStatus,outcomeStatus,fingerprint;
        Existing(String c,String a,String m,String p,String life,String outcome,String fp){
            consultationId=c;admissionId=a;mode=m;profile=p;lifecycleStatus=life;outcomeStatus=outcome;fingerprint=fp;
        }
        boolean matches(String c,String a,String m,String p,String fp){
            return eq(consultationId,c)&&eq(admissionId,a)&&eq(mode,m)&&eq(profile,p)&&eq(fingerprint,fp);
        }
    }

    private static boolean eq(String a,String b){return a==null?b==null:a.equals(b);}
}
