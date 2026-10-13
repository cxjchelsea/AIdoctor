package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.forensics;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Assertions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

final class StageBFTestConfig implements AutoCloseable {
 static final String DB="u07_d5o2_f_deadlock";
 static final String PROFILE="U07_D5_O2_RACE05_F_ISOLATED_SYNTHETIC";
 static final String URL="jdbc:mysql://127.0.0.1:33331/"+DB+"?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
 final HikariDataSource writer;
 final DataSourceTransactionManager tx;
 StageBFTestConfig(){
  if(!"true".equals(System.getenv("GITHUB_ACTIONS"))||
     !"true".equals(System.getenv("U07_F_ISOLATED"))||
     !PROFILE.equals(System.getenv("U07_F_PROFILE"))||
     !URL.equals(System.getenv("U07_F_URL")))
   throw new IllegalStateException("F_ISOLATED_RUNNER_GUARD");
  HikariConfig c=new HikariConfig();c.setJdbcUrl(URL);
  c.setUsername("stagebf_writer");c.setPassword(secret("F_WRITER_PWD"));
  c.setMaximumPoolSize(4);c.setMinimumIdle(0);c.setPoolName("f-isolated-writer");
  c.setConnectionTimeout(3000);c.setValidationTimeout(1500);
  writer=new HikariDataSource(c);tx=new DataSourceTransactionManager(writer);
  JdbcTemplate j=new JdbcTemplate(writer);
  Assertions.assertEquals(DB,j.queryForObject("SELECT DATABASE()",String.class));
  Assertions.assertTrue(j.queryForObject("SELECT VERSION()",String.class).startsWith("8."));
  for(String table:StageBFObserverEvidence.TABLES){
   Assertions.assertEquals("InnoDB",j.queryForObject(
    "SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?",
    new Object[]{DB,table},String.class));
  }
 }
 static String secret(String key){
  String v=System.getenv(key);if(v==null||v.length()<24)throw new IllegalStateException("F_MISSING_SECRET_"+key);
  return v;
 }
 static Connection connection(String user,String key)throws SQLException{
  Connection c=DriverManager.getConnection(URL,user,secret(key));
  c.setNetworkTimeout(java.util.concurrent.Executors.newSingleThreadExecutor(r->{
   Thread t=new Thread(r,"f-jdbc-timeout");t.setDaemon(true);return t;
  }),3000);
  return c;
 }
 @Override public void close(){writer.close();}
}
