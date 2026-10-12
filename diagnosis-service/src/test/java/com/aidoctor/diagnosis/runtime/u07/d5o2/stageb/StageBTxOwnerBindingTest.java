package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb;

import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRepository;
import java.sql.Connection;
import java.util.UUID;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
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
 static TransactionTemplate template(org.springframework.transaction.PlatformTransactionManager manager){
  TransactionTemplate t=new TransactionTemplate(manager);
  t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
  t.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
  t.setTimeout(10);return t;
 }
 static void stage(TransactionTemplate t,Runnable operation){
  clean();
  try{
   t.execute(status->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
    operation.run();return null;});
  }finally{clean();}
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
   TransactionTemplate jt=template(jm),pt=template(pm);
   for(int direction=0;direction<2;direction++){
    final String row="p0-"+UUID.randomUUID();
    Runnable jdbcWork=()->{
     assertTrue(TransactionSynchronizationManager.hasResource(ds));
     Connection c=DataSourceUtils.getConnection(ds);
     assertTrue(DataSourceUtils.isConnectionTransactional(c,ds));
     assertEquals(Connection.TRANSACTION_SERIALIZABLE,readIsolation(c));
     assertNotNull(jdbc.queryForObject("SELECT CONNECTION_ID()",Long.class));
    };
    Runnable jpaWork=()->{
     assertTrue(TransactionSynchronizationManager.hasResource(ds));
     assertTrue(TransactionSynchronizationManager.hasResource(emf));
     Connection c=DataSourceUtils.getConnection(ds);
     assertTrue(DataSourceUtils.isConnectionTransactional(c,ds));
     assertEquals(Connection.TRANSACTION_SERIALIZABLE,readIsolation(c));
     ctx.getBean(RuntimeThreadStateRepository.class)
       .saveAndFlush(new RuntimeThreadStateRecord(row,row,java.time.LocalDateTime.now()));
    };
    if(direction==0){stage(jt,jdbcWork);stage(pt,jpaWork);}
    else{stage(pt,jpaWork);stage(jt,jdbcWork);}
    stage(pt,()->ctx.getBean(RuntimeThreadStateRepository.class).deleteById(row));
   }
  }
 }
 private static int readIsolation(Connection c){
  try{return c.getTransactionIsolation();}
  catch(java.sql.SQLException e){throw new IllegalStateException("STAGE_B_ISOLATION_UNREADABLE",e);}
 }
 @Test public void p0NestedManagerAndResidualResourcesFailClosed(){
  StageBTestConfig.requireEnvironment();
  try(AnnotationConfigApplicationContext ctx=new AnnotationConfigApplicationContext(StageBTestConfig.class)){
   TransactionTemplate jt=template(ctx.getBean("stageBJdbcTransactionManager",DataSourceTransactionManager.class));
   TransactionTemplate pt=template(ctx.getBean("stageBJpaTransactionManager",JpaTransactionManager.class));
   stage(jt,()->assertThrows(IllegalStateException.class,()->stage(pt,()->{})));
   stage(pt,()->assertThrows(IllegalStateException.class,()->stage(jt,()->{})));
   clean();
  }
 }
}
