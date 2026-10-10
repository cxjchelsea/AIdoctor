package com.aidoctor.verification;

import com.aidoctor.diagnosis.runtime.foundation.*;
import com.aidoctor.diagnosis.runtime.u01.*;
import com.aidoctor.diagnosis.runtime.u06.wait.*;
import com.aidoctor.diagnosis.runtime.u07.U07EventApplicationRepository;
import org.hibernate.Session;
import org.junit.jupiter.api.*;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.*;
import javax.persistence.*;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Isolated test wiring only. No Boot application, real ingress, APPLY or dispatch. */
public class BridgeTest {
    static AnnotationConfigApplicationContext context;
    static DataSource ds;
    static JdbcTemplate jdbc;
    static TransactionTemplate tx;
    static EntityManager em;
    static CanonicalBusinessEventLedger ledger;
    static CanonicalBusinessEventRepository events;
    static ConsultationWaitTransitionService waits;
    static final AtomicInteger ids = new AtomicInteger();
    static final Timestamp NOW = Timestamp.valueOf("2026-01-01 00:00:00");

    @Configuration @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses={CanonicalBusinessEventRepository.class,
            ConsultationRepository.class, ConsultationWaitEffectRepository.class})
    static class Config {
        @Bean DataSource dataSource() {
            String url=System.getenv("U07_BRIDGE_URL");
            if (!"jdbc:mysql://127.0.0.1:33322/u07_d5_bridge?useSSL=false&allowPublicKeyRetrieval=true".equals(url))
                throw new IllegalArgumentException("fixed disposable target required");
            return new DriverManagerDataSource(url,System.getenv("U07_BRIDGE_USER"),System.getenv("U07_BRIDGE_PASSWORD"));
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
            LocalContainerEntityManagerFactoryBean f=new LocalContainerEntityManagerFactoryBean();
            f.setDataSource(ds); f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            f.setPackagesToScan("com.aidoctor.diagnosis.runtime.foundation", "com.aidoctor.diagnosis.runtime.u01",
                    "com.aidoctor.diagnosis.runtime.u06.wait");
            Properties p=new Properties();p.setProperty("hibernate.hbm2ddl.auto","none");
            p.setProperty("hibernate.dialect","org.hibernate.dialect.MySQL8Dialect");
            f.setJpaProperties(p);return f;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory f,DataSource ds) {
            JpaTransactionManager m=new JpaTransactionManager(f);m.setDataSource(ds);return m;
        }
        @Bean CanonicalBusinessEventLedger ledger(CanonicalBusinessEventRepository r) {return new CanonicalBusinessEventLedger(r);}
        @Bean ConsultationWaitTransitionService waits(ConsultationRepository c,ConsultationWaitEffectRepository w) {
            return new ConsultationWaitTransitionService(c,w);
        }
    }
    @BeforeAll static void start() {
        context=new AnnotationConfigApplicationContext(Config.class);
        ds=context.getBean(DataSource.class);jdbc=new JdbcTemplate(ds);
        tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));tx.setTimeout(12);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        em=SharedEntityManagerCreator.createSharedEntityManager(context.getBean(EntityManagerFactory.class));
        ledger=context.getBean(CanonicalBusinessEventLedger.class);events=context.getBean(CanonicalBusinessEventRepository.class);
        waits=context.getBean(ConsultationWaitTransitionService.class);
    }
    @AfterAll static void stop() {if(context!=null)context.close();}
    static String id() {return "synthetic-bridge-"+ids.incrementAndGet();}
    static String consultation() {
        String c=id();
        jdbc.update("INSERT INTO clinical_consultation (consultation_id,row_version,cdp_id,user_id,lifecycle_status,subject_status,problem_status,scope_decision,early_safety_signal,start_event_id,created_at) VALUES (?,0,?,'synthetic-user','ACTIVE','SYNTHETIC','SYNTHETIC','SYNTHETIC',0,?,?)",c,c,c,NOW);
        return c;
    }
    static void guard(DataSource expected) {
        if(!TransactionSynchronizationManager.isActualTransactionActive()
                ||!TransactionSynchronizationManager.hasResource(expected)||!em.isJoinedToTransaction())
            throw new IllegalStateException("UNBOUND_BRIDGE");
    }
    static void lock(String c) {jdbc.queryForObject("SELECT row_version FROM clinical_consultation WHERE consultation_id=? FOR UPDATE",Long.class,c);}
    static void app(String event,String c) {
        Connection con=DataSourceUtils.getConnection(ds);
        try {
            assertTrue(DataSourceUtils.isConnectionTransactional(con,ds));assertFalse(con.getAutoCommit());
            new U07EventApplicationRepository().insertReceived(con,event,c,"synthetic-question","synthetic-wait","digest",
                    "synthetic-source","synthetic-version",0,NOW);
        } catch(SQLException e) {throw new IllegalStateException(e);}
        finally {DataSourceUtils.releaseConnection(con,ds);}
    }
    static void admit(String event,String key,String c) {
        guard(ds);lock(c);ledger.resolveOrCreate(event,c,"SYNTHETIC",key,"digest");app(event,c);em.flush();
    }
    static int count(String table,String event) {return jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE event_id=?",Integer.class,event);}
    static void absent(String event) {assertEquals(0,count("canonical_business_event",event));assertEquals(0,count("u07_event_application",event));}

    @Test void actualProxiesAndSamePhysicalConnection() {
        assertTrue(AopUtils.isAopProxy(ledger));assertTrue(AopUtils.isAopProxy(waits));
        tx.execute(s->{guard(ds);Long jpa=em.unwrap(Session.class).doReturningWork(c->{try(Statement st=c.createStatement();ResultSet r=st.executeQuery("SELECT CONNECTION_ID()")){r.next();return r.getLong(1);}});
            assertEquals(jpa,jdbc.queryForObject("SELECT CONNECTION_ID()",Long.class));return null;});
    }
    @Test void jpaThenJdbcFailureRollsBackBoth() {
        String e=id(),c=consultation();
        assertThrows(IllegalStateException.class,()->tx.execute(s->{admit(e,e,c);throw new IllegalStateException("injected body");}));
        absent(e);
    }
    @Test void jdbcThenJpaFlushFailureRollsBackApplication() {
        String winner=id(),key=id(),c=consultation(),loser=id();
        tx.execute(s->{admit(winner,key,c);return null;});
        assertThrows(RuntimeException.class,()->tx.execute(s->{guard(ds);lock(c);app(loser,c);
            em.persist(new CanonicalBusinessEventRecord(loser,c,"SYNTHETIC",key,"digest",LocalDateTime.now()));em.flush();return null;}));
        absent(loser);assertEquals(1,count("canonical_business_event",winner));
        tx.execute(s->{assertEquals(winner,events.findByIdempotencyKey(key).get().getEventId());return null;});
    }
    @Test void commitThenLostResponseHasFreshDurablePair() {
        String e=id(),c=consultation();
        assertThrows(IllegalStateException.class,()->{tx.execute(s->{admit(e,e,c);return null;});throw new IllegalStateException("response lost after template returned");});
        assertEquals(1,count("canonical_business_event",e));assertEquals(1,count("u07_event_application",e));
        assertEquals("RECEIVED",jdbc.queryForObject("SELECT phase FROM u07_event_application WHERE event_id=?",String.class,e));
    }
    @Test void noTransactionAndWrongDatasourceFailBeforeWrites() {
        String e=id(),c=consultation();
        assertThrows(IllegalStateException.class,()->admit(e,e,c));absent(e);
        DataSource wrong=new DriverManagerDataSource();
        tx.execute(s->{assertThrows(IllegalStateException.class,()->guard(wrong));return null;});absent(e);
    }
    @Test void failedFlushMarksTransactionRollbackOnlyAndWinnerReadUsesNewTransaction() {
        String winner=id(),key=id(),c=consultation(),loser=id();tx.execute(s->{admit(winner,key,c);return null;});
        assertThrows(RuntimeException.class,()->tx.execute(s->{
            ledger.resolveOrCreate(loser,c,"SYNTHETIC",id(),"digest");
            em.persist(new CanonicalBusinessEventRecord(id(),c,"SYNTHETIC",key,"digest",LocalDateTime.now()));
            assertThrows(RuntimeException.class,()->em.flush());
            assertTrue(s.isRollbackOnly());return null;
        }));
        absent(loser);
        tx.execute(s->{assertEquals(winner,events.findByIdempotencyKey(key).get().getEventId());return null;});
    }
    @Test void crossConsultationUniqueRaceHasOneWinnerAndScopeConflict() throws Exception {
        String c1=consultation(),c2=consultation(),e1=id(),e2=id(),key=id();
        CyclicBarrier bothAbsent=new CyclicBarrier(2);ExecutorService pool=Executors.newFixedThreadPool(2);
        List<Future<Throwable>> f=new ArrayList<>();
        try {
            for(String[] pair:new String[][]{{e1,c1},{e2,c2}}) f.add(pool.submit(()->{
                try {tx.execute(s->{guard(ds);lock(pair[1]);assertFalse(events.findByIdempotencyKey(key).isPresent());
                    ledger.resolveOrCreate(pair[0],pair[1],"SYNTHETIC",key,"digest");
                    try{bothAbsent.await(8,TimeUnit.SECONDS);}catch(Exception ex){throw new IllegalStateException(ex);}
                    // Different consultation locks do not serialize this unique insert race.
                    app(pair[0],pair[1]);em.flush();return null;});return null;
                }catch(Throwable ex){return ex;}
            }));
            int successes=0,duplicates=0;
            for(Future<Throwable> result:f){Throwable failure=result.get(18,TimeUnit.SECONDS);
                if(failure==null)successes++;else {SQLException duplicate=null;
                    for(Throwable t=failure;t!=null;t=t.getCause())if(t instanceof SQLException)duplicate=(SQLException)t;
                    assertNotNull(duplicate,"unexpected failure must fail test");assertEquals("23000",duplicate.getSQLState());assertEquals(1062,duplicate.getErrorCode());duplicates++;}}
            assertEquals(1,successes);assertEquals(1,duplicates);
            CanonicalBusinessEventRecord winner=tx.execute(s->events.findByIdempotencyKey(key).get());
            String losing=winner.getEventId().equals(e1)?e2:e1, other=winner.getConsultationId().equals(c1)?c2:c1;
            absent(losing);assertEquals(1,count("u07_event_application",winner.getEventId()));
            assertThrows(IllegalStateException.class,()->tx.execute(s->ledger.resolveOrCreate(id(),other,"SYNTHETIC",key,"digest")));
        }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void actualU06WaitAndBridgeShareConsultationFirstLock() throws Exception {
        String c=consultation(),e=id(),w=id();CountDownLatch locked=new CountDownLatch(1),release=new CountDownLatch(1),contender=new CountDownLatch(1);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<?> first=pool.submit(()->tx.execute(s->{
                ConsultationWaitTransitionService.Result r=waits.establish(new ConsultationWaitTransitionService.Command(w,id(),c,"synthetic-question","synthetic-delivery","digest",id(),0,LocalDateTime.now()));
                assertEquals(1,r.committedRowVersion);locked.countDown();
                try{assertTrue(release.await(8,TimeUnit.SECONDS));}catch(InterruptedException ex){throw new IllegalStateException(ex);}return null;}));
            assertTrue(locked.await(8,TimeUnit.SECONDS));
            Future<?> second=pool.submit(()->tx.execute(s->{contender.countDown();admit(e,e,c);return null;}));
            assertTrue(contender.await(8,TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,()->second.get(200,TimeUnit.MILLISECONDS));
            release.countDown();first.get(18,TimeUnit.SECONDS);second.get(18,TimeUnit.SECONDS);
            assertEquals("WAITING_USER",jdbc.queryForObject("SELECT lifecycle_status FROM clinical_consultation WHERE consultation_id=?",String.class,c));
            assertEquals(1,count("u07_event_application",e));
        }finally{release.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
}
