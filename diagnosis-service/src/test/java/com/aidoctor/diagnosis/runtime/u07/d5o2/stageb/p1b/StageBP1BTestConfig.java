package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.p1b;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

final class StageBP1BTestConfig implements AutoCloseable {
 static final String SCHEMA="u07_d5o2_stage_b_p1b";
 static final String PROFILE="U07_D5O2_STAGE_B_P1B_SYNTHETIC";
 static final String URL="jdbc:mysql://127.0.0.1:33329/"+SCHEMA+"?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
 final HikariDataSource source;
 final DataSourceTransactionManager manager;
 final JdbcTemplate jdbc;
 StageBP1BTestConfig(){
  if(!"true".equals(System.getenv("GITHUB_ACTIONS"))
    ||!PROFILE.equals(System.getenv("U07_D5O2_P1B_PROFILE"))
    ||!URL.equals(System.getenv("U07_D5O2_P1B_URL"))
    ||!"true".equals(System.getenv("U07_D5O2_P1B_ISOLATED")))
   throw new IllegalStateException("P1B_NOT_DISPOSABLE_RUNNER");
  HikariConfig cfg=new HikariConfig();
  cfg.setJdbcUrl(URL);
  cfg.setUsername("stagebp1b_writer");
  cfg.setPassword(require("P1B_WRITER_PWD"));
  cfg.setMaximumPoolSize(4);cfg.setMinimumIdle(0);cfg.setPoolName("p1b-disposable-writer");
  source=new HikariDataSource(cfg);
  manager=new DataSourceTransactionManager(source);
  jdbc=new JdbcTemplate(source);
  StageBP1BEvidence.schemaPreflight(jdbc);
 }
 static String require(String key){
  String value=System.getenv(key);
  if(value==null||value.length()<24)throw new IllegalStateException("P1B_MISSING_ENV_"+key);
  return value;
 }
 @Override public void close(){source.close();}
}
