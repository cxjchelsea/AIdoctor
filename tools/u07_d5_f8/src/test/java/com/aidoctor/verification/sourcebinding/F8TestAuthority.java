package com.aidoctor.verification.sourcebinding;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
/** Independent synthetic owner writer. All mutations take the same consultation guard. */
final class F8TestAuthority {
 final JdbcTemplate db;final TransactionTemplate tx;
 F8TestAuthority(JdbcTemplate db,TransactionTemplate tx){this.db=db;this.tx=tx;}
 void issue(String[] f,LocalDateTime deadline){String scope=F8Identity.digest("f8-scope",f[4],f[5],f[6]);
  tx.execute(s->{db.update("INSERT IGNORE INTO f8_guard(scope_key) VALUES(?)",scope);lock(scope);
   db.update("INSERT INTO f8_action VALUES(?,?,?, ?,1,1,1,?,?,?)",scope,f[4],f[5],f[6],F8Identity.ISSUER,F8Identity.POLICY,F8Identity.MANIFEST);
   for(String[] pair:new String[][]{{f[5],"CONSULTATION","WAITING"},{f[7],"QUESTION","ASKED"},{f[8],"PENDING","WAITING"},{f[10],"ISSUANCE","DELIVERED"}})
    db.update("INSERT INTO f8_owner VALUES(?,?,?,?,?,?,?,?,?,?,?,1,0,?,?,?, ?,?,?)",pair[0],pair[1],f[2],f[3],f[4],f[5],f[6],f[7],f[9],f[8],f[10],pair[2],f[9],Timestamp.valueOf(deadline),F8Identity.ISSUER,F8Identity.POLICY,F8Identity.MANIFEST);
   return null;});
 }
 void lock(String scope){db.queryForObject("SELECT scope_key FROM f8_guard WHERE scope_key=? FOR UPDATE",String.class,scope);}
 void permission(F8Identity id,boolean read,boolean finalize){tx.execute(s->{lock(id.scopeKey);db.update("UPDATE f8_action SET epoch=epoch+1,read_allowed=?,finalize_allowed=? WHERE scope_key=?",read,finalize,id.scopeKey);return null;});}
 void current(F8Identity id,String wait,String state,LocalDateTime deadline){tx.execute(s->{lock(id.scopeKey);db.update("UPDATE f8_owner SET current_wait=?,state=?,deadline=?,version=version+1 WHERE ref=?",wait,state,Timestamp.valueOf(deadline),id.source.field(5));return null;});}
 void applied(F8Identity id){tx.execute(s->{lock(id.scopeKey);
  Map<String,Object> c=db.queryForMap("SELECT * FROM f8_claim WHERE wait_key=?",id.waitKey);
  Map<String,Object> w=db.queryForMap("SELECT * FROM f8_decision WHERE id=?",c.get("decision_id"));
  if(!"ACCEPTED".equals(w.get("verdict")))throw new IllegalStateException();
  db.update("INSERT INTO f8_applied VALUES(?,?,?,?,1,?,?,?)",w.get("id"),id.waitKey,w.get("canonical_id"),w.get("digest"),F8Identity.ISSUER,F8Identity.POLICY,F8Identity.MANIFEST);return null;});}
}
