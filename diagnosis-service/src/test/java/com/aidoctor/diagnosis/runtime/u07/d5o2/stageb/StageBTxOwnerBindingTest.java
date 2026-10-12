package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb;

import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

public class StageBTxOwnerBindingTest {
 static void clean(){
  if(TransactionSynchronizationManager.isActualTransactionActive()
   ||TransactionSynchronizationManager.isSynchronizationActive()
   ||!TransactionSynchronizationManager.getResourceMap().isEmpty())
   throw new IllegalStateException("STAGE_B_AMBIENT_RESOURCE_DENIED");
 }
 static TransactionTemplate template(PlatformTransactionManager manager){
  TransactionTemplate t=new TransactionTemplate(manager);
  t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
  t.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
  t.setTimeout(10);return t;
 }
 /** Fail before acquiring a connection when a same-URL but different resource manager is offered. */
 static void stage(PlatformTransactionManager supplied,PlatformTransactionManager expected,
                   DataSource ds,EntityManagerFactory emf,Runnable operation){
  clean();
  if(supplied!=expected || !matchesResource(supplied,ds,emf))
   throw new IllegalStateException("STAGE_B_WRONG_MANAGER_DATASOURCE_DENIED");
  TransactionTemplate t=template(supplied);
  try{
   t.execute(status->{
    assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
    assertTrue(TransactionSynchronizationManager.hasResource(ds));
    if(emf!=null)assertTrue(TransactionSynchronizationManager.hasResource(emf));
    operation.run();return null;
   });
  }finally{clean();}
 }
 private static boolean matchesResource(PlatformTransactionManager manager,DataSource ds,EntityManagerFactory emf){
  if(manager instanceof JpaTransactionManager){
   JpaTransactionManager j=(JpaTransactionManager)manager;
   return emf!=null && j.getDataSource()==ds && j.getEntityManagerFactory()==emf;
  }
  if(manager instanceof DataSourceTransactionManager)
   return emf==null && ((DataSourceTransactionManager)manager).getDataSource()==ds;
  return false;
 }
 static int readIsolation(Connection c){
  try{return c.getTransactionIsolation();}
  catch(java.sql.SQLException e){throw new IllegalStateException("STAGE_B_ISOLATION_UNREADABLE",e);}
 }
 static void evidence(String method,Map<String,Object> facts){
  try{
   if(!"true".equals(System.getenv("GITHUB_ACTIONS")))throw new IllegalStateException("P0_EVIDENCE_NOT_CI");
   facts.put("testMethod",method);
   facts.put("evidencePlane","TEST_ONLY_REAL_MYSQL8");
   facts.put("productionTopologyVerified",false);
   Path dir=Paths.get("target","stage-b-p0-observations");
   Files.createDirectories(dir);
   new ObjectMapper().writeValue(dir.resolve(method+".json").toFile(),facts);
  }catch(java.io.IOException e){throw new IllegalStateException("P0_EVIDENCE_WRITE_FAILED",e);}
 }
 static Map<String,Object> facts(String id){
  Map<String,Object> m=new LinkedHashMap<String,Object>();
  m.put("caseId",id);m.put("guardEnforced",true);return m;
 }
 @Test public void p0OwnerIdentityJdbcAndJpaEffectiveIsolation(){
  StageBTestConfig.requireEnvironment();
  try(AnnotationConfigApplicationContext ctx=new AnnotationConfigApplicationContext(StageBTestConfig.class)){
   DataSource ds=ctx.getBean("stageBDataSource",DataSource.class);
   JdbcTemplate jdbc=ctx.getBean("stageBJdbcTemplate",JdbcTemplate.class);
   DataSourceTransactionManager jm=ctx.getBean("stageBJdbcTransactionManager",DataSourceTransactionManager.class);
   JpaTransactionManager pm=ctx.getBean("stageBJpaTransactionManager",JpaTransactionManager.class);
   EntityManagerFactory emf=ctx.getBean("stageBEntityManagerFactory",EntityManagerFactory.class);
   assertSame(ds,jdbc.getDataSource());assertSame(ds,jm.getDataSource());
   assertSame(ds,pm.getDataSource());assertSame(emf,pm.getEntityManagerFactory());
   final int[] jdbcIsolation={0},jpaIsolation={0};
   final long[] jdbcConnection={0},jpaConnection={0};
   for(int direction=0;direction<2;direction++){
    final String row="p0-"+UUID.randomUUID();
    Runnable jdbcWork=()->{
     Connection c=DataSourceUtils.getConnection(ds);
     assertTrue(DataSourceUtils.isConnectionTransactional(c,ds));
     jdbcIsolation[0]=readIsolation(c);
     assertEquals(Connection.TRANSACTION_SERIALIZABLE,jdbcIsolation[0]);
     jdbcConnection[0]=jdbc.queryForObject("SELECT CONNECTION_ID()",Long.class);
    };
    Runnable jpaWork=()->{
     Connection c=DataSourceUtils.getConnection(ds);
     assertTrue(DataSourceUtils.isConnectionTransactional(c,ds));
     jpaIsolation[0]=readIsolation(c);
     assertEquals(Connection.TRANSACTION_SERIALIZABLE,jpaIsolation[0]);
     jpaConnection[0]=jdbc.queryForObject("SELECT CONNECTION_ID()",Long.class);
     ctx.getBean(RuntimeThreadStateRepository.class)
       .saveAndFlush(new RuntimeThreadStateRecord(row,row,java.time.LocalDateTime.now()));
    };
    if(direction==0){
     stage(jm,jm,ds,null,jdbcWork);stage(pm,pm,ds,emf,jpaWork);
    }else{
     stage(pm,pm,ds,emf,jpaWork);stage(jm,jm,ds,null,jdbcWork);
    }
    stage(pm,pm,ds,emf,()->ctx.getBean(RuntimeThreadStateRepository.class).deleteById(row));
   }
   Map<String,Object> result=facts("P0-OWNER-01-04");
   result.put("jdbcIsolation",jdbcIsolation[0]);result.put("jpaIsolation",jpaIsolation[0]);
   result.put("jdbcWriterConnectionId",jdbcConnection[0]);result.put("jpaWriterConnectionId",jpaConnection[0]);
   result.put("sequentialOrdersVerified",2);result.put("managerResourceIdentityVerified",true);
   evidence("p0OwnerIdentityJdbcAndJpaEffectiveIsolation",result);
  }
 }
 @Test public void p0NestedManagerAndResidualResourcesFailClosed(){
  StageBTestConfig.requireEnvironment();
  try(AnnotationConfigApplicationContext ctx=new AnnotationConfigApplicationContext(StageBTestConfig.class)){
   DataSource ds=ctx.getBean("stageBDataSource",DataSource.class);
   DataSourceTransactionManager jm=ctx.getBean("stageBJdbcTransactionManager",DataSourceTransactionManager.class);
   JpaTransactionManager pm=ctx.getBean("stageBJpaTransactionManager",JpaTransactionManager.class);
   EntityManagerFactory emf=ctx.getBean("stageBEntityManagerFactory",EntityManagerFactory.class);
   stage(jm,jm,ds,null,()->assertThrows(IllegalStateException.class,
      ()->stage(pm,pm,ds,emf,()->{})));
   stage(pm,pm,ds,emf,()->assertThrows(IllegalStateException.class,
      ()->stage(jm,jm,ds,null,()->{})));
   clean();
   Map<String,Object> result=facts("P0-OWNER-05");
   result.put("nestedRejectedDirections",2);
   evidence("p0NestedManagerAndResidualResourcesFailClosed",result);
  }
 }
 @Test public void p0WrongManagerDifferentDatasourceCannotBindExpectedResource(){
  StageBTestConfig.requireEnvironment();
  try(AnnotationConfigApplicationContext ctx=new AnnotationConfigApplicationContext(StageBTestConfig.class)){
   DataSource expected=ctx.getBean("stageBDataSource",DataSource.class);
   DataSourceTransactionManager authorized=ctx.getBean("stageBJdbcTransactionManager",DataSourceTransactionManager.class);
   org.springframework.jdbc.datasource.DriverManagerDataSource incorrect=
     new org.springframework.jdbc.datasource.DriverManagerDataSource(StageBTestConfig.URL,
       "stageb_writer","synthetic-stageb-writer");
   DataSourceTransactionManager wrong=new DataSourceTransactionManager(incorrect);
   final boolean[] entered={false};
   IllegalStateException ex=assertThrows(IllegalStateException.class,
     ()->stage(wrong,authorized,expected,null,()->entered[0]=true));
   assertEquals("STAGE_B_WRONG_MANAGER_DATASOURCE_DENIED",ex.getMessage());
   assertFalse(entered[0]);clean();
   Map<String,Object> result=facts("P0-OWNER-06");
   result.put("wrongManagerDeniedBeforeCallback",!entered[0]);
   result.put("sameJdbcUrlDifferentDataSourceInstance",true);
   evidence("p0WrongManagerDifferentDatasourceCannotBindExpectedResource",result);
  }
 }
}
