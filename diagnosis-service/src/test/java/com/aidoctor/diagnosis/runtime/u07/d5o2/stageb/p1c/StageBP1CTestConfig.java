package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.p1c;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

final class StageBP1CTestConfig implements AutoCloseable {
 static final String SCHEMA="u07_d5o2_stage_b_p1c";
 static final String PROFILE="U07_D5O2_STAGE_B_P1C_SYNTHETIC";
 static final String URL="jdbc:mysql://127.0.0.1:33330/"+SCHEMA+
   "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
 final HikariDataSource source;
 final DataSourceTransactionManager manager;
 final JdbcTemplate jdbc;
 StageBP1CTestConfig(){
  if(!"true".equals(System.getenv("GITHUB_ACTIONS")) ||
    !PROFILE.equals(System.getenv("U07_D5O2_P1C_PROFILE")) ||
    !URL.equals(System.getenv("U07_D5O2_P1C_URL")) ||
    !"true".equals(System.getenv("U07_D5O2_P1C_ISOLATED")))
   throw new IllegalStateException("P1C_NOT_DISPOSABLE_RUNNER");
  HikariConfig cfg=new HikariConfig();cfg.setJdbcUrl(URL);
  cfg.setUsername("stagebp1c_writer");cfg.setPassword(require("P1C_WRITER_PWD"));
  cfg.setMaximumPoolSize(4);cfg.setMinimumIdle(0);cfg.setPoolName("p1c-disposable-writer");
  cfg.setConnectionTimeout(5000);cfg.setValidationTimeout(2000);
  source=new HikariDataSource(cfg);
  manager=new DataSourceTransactionManager(source);
  jdbc=new JdbcTemplate(source);
  StageBP1CEvidence.preflight(jdbc);
 }
 static String require(String k){
  String v=System.getenv(k);
  if(v==null||v.length()<24)throw new IllegalStateException("P1C_MISSING_ENV_"+k);
  return v;
 }
 @Override public void close(){source.close();}
}
