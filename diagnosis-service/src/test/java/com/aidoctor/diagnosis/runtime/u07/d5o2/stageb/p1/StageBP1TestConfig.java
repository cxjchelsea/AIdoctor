package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.p1;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class StageBP1TestConfig {
 public static final String URL="jdbc:mysql://127.0.0.1:33328/u07_d5o2_stage_b_p1?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
 static void requireEnvironment(){
  if(!"true".equals(System.getenv("GITHUB_ACTIONS"))
   ||!"U07_D5O2_STAGE_B_P1_SYNTHETIC".equals(System.getenv("U07_D5O2_PROFILE"))
   ||!"u07_d5o2_stage_b_p1".equals(System.getenv("U07_D5O2_SCHEMA"))
   ||!URL.equals(System.getenv("U07_D5O2_URL")))
   throw new IllegalStateException("P1A_NONDISPOSABLE_ENV_DENIED");
 }
 @Bean(name="p1DataSource",destroyMethod="close")
 public HikariDataSource p1DataSource(){
  requireEnvironment();
  HikariConfig cfg=new HikariConfig();
  cfg.setJdbcUrl(URL);cfg.setUsername("stagebp1_writer");
  cfg.setPassword("synthetic-stagebp1-writer");
  cfg.setMaximumPoolSize(6);cfg.setMinimumIdle(0);cfg.setPoolName("stagebp1-isolated");
  return new HikariDataSource(cfg);
 }
 @Bean(name="p1JdbcTemplate")
 public JdbcTemplate p1JdbcTemplate(@org.springframework.beans.factory.annotation.Qualifier("p1DataSource") DataSource ds){
  return new JdbcTemplate(ds);
 }
 @Bean(name="p1JdbcTransactionManager")
 public DataSourceTransactionManager p1JdbcTransactionManager(@org.springframework.beans.factory.annotation.Qualifier("p1DataSource") DataSource ds){
  return new DataSourceTransactionManager(ds);
 }
}
