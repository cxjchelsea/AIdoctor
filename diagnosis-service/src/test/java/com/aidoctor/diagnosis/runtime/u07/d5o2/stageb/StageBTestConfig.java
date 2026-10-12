package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb;

import com.aidoctor.diagnosis.runtime.u01.ConsultationRecord;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRepository;
import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitEffectRecord;
import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitEffectRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
 basePackages={"com.aidoctor.diagnosis.runtime.u01","com.aidoctor.diagnosis.runtime.u06.wait","com.aidoctor.diagnosis.runtime.foundation"},
 includeFilters=@ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE,
  classes={ConsultationRepository.class,ConsultationWaitEffectRepository.class,
   RuntimeThreadStateRepository.class,RuntimeWaitCheckpointRepository.class}),
 repositoryImplementationPostfix="StageBP0ImplementationOnly",
 entityManagerFactoryRef="stageBEntityManagerFactory",
 transactionManagerRef="stageBJpaTransactionManager")
public class StageBTestConfig {
 public static final String URL="jdbc:mysql://127.0.0.1:33327/u07_d5o2_stage_b_p0?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
 private static final Set<String> ENTITY_NAMES=new HashSet<String>(Arrays.asList(
  ConsultationRecord.class.getName(),ConsultationWaitEffectRecord.class.getName(),
  RuntimeThreadStateRecord.class.getName(),RuntimeWaitCheckpointRecord.class.getName()));
 public static void requireEnvironment(){
  if(!"true".equals(System.getenv("GITHUB_ACTIONS"))
    ||!"U07_D5O2_STAGE_B_P0_SYNTHETIC".equals(System.getenv("U07_D5O2_PROFILE"))
    ||!URL.equals(System.getenv("U07_D5O2_URL"))
    ||!"u07_d5o2_stage_b_p0".equals(System.getenv("U07_D5O2_SCHEMA")))
   throw new IllegalStateException("STAGE_B_P0_DISPOSABLE_ENVIRONMENT_DENIED");
 }
 @Bean(name="stageBDataSource",destroyMethod="close")
 public HikariDataSource stageBDataSource(){
  requireEnvironment();
  HikariConfig c=new HikariConfig();c.setJdbcUrl(URL);
  c.setUsername("stageb_writer");c.setPassword("synthetic-stageb-writer");
  c.setMaximumPoolSize(5);c.setMinimumIdle(0);c.setPoolName("stageb-p0-disposable");
  return new HikariDataSource(c);
 }
 @Bean(name="stageBEntityManagerFactory")
 public LocalContainerEntityManagerFactoryBean stageBEntityManagerFactory(
   @org.springframework.beans.factory.annotation.Qualifier("stageBDataSource") DataSource ds){
  LocalContainerEntityManagerFactoryBean emf=new LocalContainerEntityManagerFactoryBean();
  emf.setDataSource(ds);emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
  emf.setPackagesToScan("com.aidoctor.diagnosis.runtime.u01",
   "com.aidoctor.diagnosis.runtime.u06.wait","com.aidoctor.diagnosis.runtime.foundation");
  emf.setPersistenceUnitPostProcessors((PersistenceUnitPostProcessor) pui->{
   pui.getManagedClassNames().clear();pui.getManagedClassNames().addAll(ENTITY_NAMES);
  });
  java.util.Properties p=new java.util.Properties();
  p.setProperty("hibernate.dialect","org.hibernate.dialect.MySQL8Dialect");
  p.setProperty("hibernate.hbm2ddl.auto","validate");emf.setJpaProperties(p);
  return emf;
 }
 @Bean(name="stageBJpaTransactionManager")
 public JpaTransactionManager stageBJpaTransactionManager(
   @org.springframework.beans.factory.annotation.Qualifier("stageBEntityManagerFactory") EntityManagerFactory emf,
   @org.springframework.beans.factory.annotation.Qualifier("stageBDataSource") DataSource ds){
  JpaTransactionManager m=new JpaTransactionManager(emf);m.setDataSource(ds);return m;
 }
 @Bean(name="stageBJdbcTransactionManager")
 public DataSourceTransactionManager stageBJdbcTransactionManager(
   @org.springframework.beans.factory.annotation.Qualifier("stageBDataSource") DataSource ds){
  return new DataSourceTransactionManager(ds);
 }
 @Bean(name="stageBJdbcTemplate")
 public JdbcTemplate stageBJdbcTemplate(
   @org.springframework.beans.factory.annotation.Qualifier("stageBDataSource") DataSource ds){
  return new JdbcTemplate(ds);
 }
}
