package com.aidoctor.verification.sourcebinding;

import com.aidoctor.diagnosis.runtime.foundation.*;
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
import org.springframework.transaction.support.TransactionTemplate;
import javax.persistence.*;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static com.aidoctor.verification.sourcebinding.SourceBindingAdapter.*;

/** Actual MySQL + real Foundation JPA; authority is explicitly synthetic test-only. */
class SourceBindingTest {
    static final String URL="jdbc:mysql://127.0.0.1:33323/u07_source_binding?useSSL=false&allowPublicKeyRetrieval=true";
    static AnnotationConfigApplicationContext context;
    static DataSource ds;static JdbcTemplate admin,consumer,issuerJdbc;
    static PlatformTransactionManager manager;static EntityManager em;
    static CanonicalBusinessEventLedger ledger;static CanonicalBusinessEventRepository events;
    static TestAuthority issuer;static TestAuthority.CipherBox box;
    static final AtomicInteger ids=new AtomicInteger();
    static final Instant WHEN=Instant.parse("2026-01-01T00:00:00Z");
    @Configuration @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses=CanonicalBusinessEventRepository.class)
    static class Config {
        @Bean DataSource dataSource(){
            if(!URL.equals(System.getenv("U07_SOURCE_BINDING_URL")))throw new IllegalArgumentException("fixed disposable target only");
            return new DriverManagerDataSource(URL,"synthetic_adapter","synthetic-only-adapter");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds){
            LocalContainerEntityManagerFactoryBean f=new LocalContainerEntityManagerFactoryBean();f.setDataSource(ds);
            f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());f.setPackagesToScan("com.aidoctor.diagnosis.runtime.foundation");
            Properties p=new Properties();p.setProperty("hibernate.hbm2ddl.auto","none");p.setProperty("hibernate.dialect","org.hibernate.dialect.MySQL8Dialect");f.setJpaProperties(p);return f;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory f,DataSource ds){JpaTransactionManager m=new JpaTransactionManager(f);m.setDataSource(ds);return m;}
        @Bean CanonicalBusinessEventLedger ledger(CanonicalBusinessEventRepository r){return new CanonicalBusinessEventLedger(r);}
    }
    @BeforeAll static void setup(){
        context=new AnnotationConfigApplicationContext(Config.class);ds=context.getBean(DataSource.class);consumer=new JdbcTemplate(ds);
        admin=new JdbcTemplate(new DriverManagerDataSource(URL,"root",System.getenv("U07_SOURCE_BINDING_ADMIN_PASSWORD")));
        DataSource issuerDs=new DriverManagerDataSource(URL,"synthetic_issuer","synthetic-only-issuer");issuerJdbc=new JdbcTemplate(issuerDs);
        box=new TestAuthority.CipherBox("0123456789abcdef".getBytes(StandardCharsets.US_ASCII));
        issuer=new TestAuthority(issuerJdbc,new TransactionTemplate(new DataSourceTransactionManager(issuerDs)),box,BindingCodec.PROFILE);
        manager=context.getBean(PlatformTransactionManager.class);ledger=context.getBean(CanonicalBusinessEventLedger.class);events=context.getBean(CanonicalBusinessEventRepository.class);
        em=SharedEntityManagerCreator.createSharedEntityManager(context.getBean(EntityManagerFactory.class));
    }
    @AfterAll static void close(){if(context!=null)context.close();}
    static String id(){return "synthetic-sb-"+ids.incrementAndGet();}
    static SourceBindingAdapter adapter(){return adapter(p->{});}
    static SourceBindingAdapter adapter(Probe p){return new SourceBindingAdapter(ds,manager,ledger,events,em,box,BindingCodec.PROFILE,p);}
    static class Fixture {
        String tenant=id(),c=id(),actor=id(),token=id(),event=id(),ref=id();
        byte[] answer="synthetic-answer:测试e\u0301".getBytes(StandardCharsets.UTF_8);
        String[] fields;
        Fixture(){
            fields=new String[]{BindingCodec.CONTRACT,"USER_ANSWER",BindingCodec.ENV,BindingCodec.PROFILE,tenant,c,actor,
                id(),id(),id(),id(),id(),id(),null,"0",null,BindingCodec.hash(answer),null,id(),id(),"placeholder"};
            refresh();issuer.epoch(tenant,true);issue();
        }
        void refresh(){fields[15]="USER_ANSWER".equals(fields[1])?"u07db:v1:"+BindingCodec.key(fields,token)+":answer":null;fields[20]=BindingCodec.payload(fields);}
        void issue(){issuer.issue(ref,fields,token,"USER_ANSWER".equals(fields[1])?answer:null,WHEN);}
        Request req(){return new Request(event,token,ref,WHEN);}
        Request alias(){return new Request(id(),token,ref,WHEN.plusSeconds(7));}
        Scope scope(){return new Scope(tenant,c,actor);}
        String key(){return BindingCodec.key(fields,token);}
        void nextProof(){ref=id();refresh();issue();}
    }
    static int count(String table,String event){return admin.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE "+("canonical_business_event".equals(table)?"event_id":"canonical_event_id")+"=?",Integer.class,event);}
    static void absent(String event){assertEquals(0,count("canonical_business_event",event));assertEquals(0,count("u07_canonical_event_binding",event));}
    static void pair(String event){assertEquals(1,count("canonical_business_event",event));assertEquals(1,count("u07_canonical_event_binding",event));}
    @Test void realProxyAndConnectionOwnership(){
        assertTrue(AopUtils.isAopProxy(ledger));
        new TransactionTemplate(manager).execute(s->{
            Long jpa=em.unwrap(Session.class).doReturningWork(c->{try(Statement st=c.createStatement();ResultSet r=st.executeQuery("SELECT CONNECTION_ID()")){r.next();return r.getLong(1);}});
            assertEquals(jpa,consumer.queryForObject("SELECT CONNECTION_ID()",Long.class));return null;
        });
    }
    @Test void createReplayAndAliasKeepOriginalHistoryAndEncryptedBytes(){
        Fixture f=new Fixture();Result first=adapter().admit(f.req(),f.scope());assertEquals(Status.STORED,first.status);assertEquals(Durability.COMMITTED,first.durability);pair(f.event);
        assertEquals(Status.REATTACHED,adapter().admit(f.req(),f.scope()).status);
        Request alias=f.alias();Result retry=adapter().admit(alias,f.scope());assertEquals(f.event,retry.canonicalId);absent(alias.eventId);
        assertEquals(WHEN.toString(),admin.queryForObject("SELECT occurred_at FROM u07_canonical_event_binding WHERE canonical_event_id=?",String.class,f.event));
        byte[] cipher=admin.queryForObject("SELECT synthetic_answer_bytes FROM u07_canonical_event_binding WHERE canonical_event_id=?",byte[].class,f.event);
        assertFalse(Arrays.equals(f.answer,cipher));assertArrayEquals(f.answer,box.decrypt(cipher,TestAuthority.KEY_REF));
        assertEquals(f.fields[20],admin.queryForObject("SELECT payload_digest FROM canonical_business_event WHERE event_id=?",String.class,f.event));
        assertNotEquals(BindingCodec.hash(BindingCodec.encode(f.fields)),f.fields[20]);
    }
    @Test void originalOccurredAtCannotChangeButAliasMay(){
        Fixture f=new Fixture();adapter().admit(f.req(),f.scope());
        assertEquals(Status.INTEGRITY_CONFLICT,adapter().admit(new Request(f.event,f.token,f.ref,WHEN.plusSeconds(1)),f.scope()).status);pair(f.event);
    }
    @Test void changedQuestionAndAnswerWithSameKeyConflictWithoutOverwrite(){
        Fixture f=new Fixture();adapter().admit(f.req(),f.scope());byte[] before=BindingCodec.encode(f.fields);
        f.fields[7]=id();f.nextProof();assertEquals(Status.INTEGRITY_CONFLICT,adapter().admit(f.alias(),f.scope()).status);
        f.answer="synthetic-answer:changed".getBytes(StandardCharsets.UTF_8);f.fields[16]=BindingCodec.hash(f.answer);f.nextProof();
        assertEquals(Status.INTEGRITY_CONFLICT,adapter().admit(f.alias(),f.scope()).status);
        assertArrayEquals(before,admin.queryForObject("SELECT canonical_binding_bytes FROM u07_canonical_event_binding WHERE canonical_event_id=?",byte[].class,f.event));pair(f.event);
    }
    @Test void newKeySameAnswerIsSeparateCandidateNotBusinessDuplicate(){
        Fixture f=new Fixture();adapter().admit(f.req(),f.scope());String first=f.event;
        f.event=id();f.token=id();f.nextProof();assertEquals(Status.STORED,adapter().admit(f.req(),f.scope()).status);pair(first);pair(f.event);
    }
    @Test void crossScopeReadsAndEventIdCollisionFailClosed(){
        Fixture f=new Fixture();adapter().admit(f.req(),f.scope());
        assertEquals(Status.DENIED,adapter().read(f.req(),new Scope(id(),f.c,f.actor)).status);
        Fixture g=new Fixture();g.event=f.event;assertEquals(Status.INTEGRITY_CONFLICT,adapter().admit(g.req(),g.scope()).status);pair(f.event);
        assertNotEquals(f.key(),g.key());
    }
    @Test void syntheticIngressRejectsInvalidInputsBeforeWrites(){
        Fixture f=new Fixture();assertEquals(Status.INVALID_INPUT,adapter().admit(new Request("real-event",f.token,f.ref,WHEN),f.scope()).status);
        assertEquals(Status.INVALID_INPUT,adapter().admit(new Request(f.event,f.token,f.ref,null),f.scope()).status);absent(f.event);
        assertThrows(IllegalArgumentException.class,()->new TestAuthority(issuerJdbc,new TransactionTemplate(manager),box,"PRODUCTION"));
        assertThrows(IllegalArgumentException.class,()->new SourceBindingAdapter(ds,manager,ledger,events,em,box,"PROFILE-A"));
        new TransactionTemplate(manager).execute(s->{assertThrows(IllegalStateException.class,()->adapter().admit(f.req(),f.scope()));return null;});
        SourceBindingAdapter wrong=new SourceBindingAdapter(new DriverManagerDataSource(),manager,ledger,events,em,box,BindingCodec.PROFILE);
        assertEquals(Status.UNAVAILABLE,wrong.admit(f.req(),f.scope()).status);absent(f.event);
    }
    @Test void forgedRefWrongIssuerPolicyTypeAndManifestCannotBecomeVerified(){
        Fixture f=new Fixture();assertEquals(Status.DENIED,adapter().admit(new Request(f.event,f.token,id(),WHEN),f.scope()).status);
        for(String column:new String[]{"issuer_id","policy_id","record_type","manifest_digest"}){
            Object old=admin.queryForObject("SELECT "+column+" FROM u07_test_authority_record WHERE record_id=?",String.class,f.ref);
            admin.update("UPDATE u07_test_authority_record SET "+column+"='forged' WHERE record_id=?",f.ref);
            assertEquals(Status.DENIED,adapter().admit(f.req(),f.scope()).status);
            admin.update("UPDATE u07_test_authority_record SET "+column+"=? WHERE record_id=?",old,f.ref);
        }absent(f.event);
    }
    @Test void wrongProfileAndCorruptFrameOrCipherAreRejected(){
        Fixture f=new Fixture();byte[] original=BindingCodec.encode(f.fields);String[] fake=f.fields.clone();fake[3]="PROFILE-A";
        byte[] bad=BindingCodec.frame("u07-event-binding-v1",BindingCodec.NAMES,fake);
        admin.update("UPDATE u07_test_authority_record SET binding_bytes=?,binding_fingerprint=? WHERE record_id=?",bad,BindingCodec.hash(bad),f.ref);
        assertEquals(Status.INTEGRITY_CONFLICT,adapter().admit(f.req(),f.scope()).status);
        admin.update("UPDATE u07_test_authority_record SET binding_bytes=?,binding_fingerprint=?,answer_cipher=? WHERE record_id=?",original,BindingCodec.hash(original),new byte[30],f.ref);
        assertEquals(Status.INTEGRITY_CONFLICT,adapter().admit(f.req(),f.scope()).status);absent(f.event);
    }
    @Test void sourceUnavailableIsDistinctFromDenied(){
        Fixture f=new Fixture();admin.execute("RENAME TABLE u07_test_authority_record TO u07_test_authority_record_offline");
        try{Result r=adapter().admit(f.req(),f.scope());assertEquals(Status.UNAVAILABLE,r.status);assertNull(r.canonicalId);assertEquals(Attempt.NOT_ATTEMPTED,r.attempt);}
        finally{admin.execute("RENAME TABLE u07_test_authority_record_offline TO u07_test_authority_record");}absent(f.event);
    }
    @Test void issuerAndConsumerDatabasePrivilegesEnforceAppendOnlyBoundary(){
        Fixture f=new Fixture();
        assertThrows(RuntimeException.class,()->consumer.update("DELETE FROM u07_test_authority_record WHERE record_id=?",f.ref));
        assertThrows(RuntimeException.class,()->consumer.update("UPDATE u07_test_authority_record SET issuer_id='forged' WHERE record_id=?",f.ref));
        assertThrows(RuntimeException.class,()->issuerJdbc.update("UPDATE u07_test_authority_record SET issuer_id='forged' WHERE record_id=?",f.ref));
        adapter().admit(f.req(),f.scope());
        assertThrows(RuntimeException.class,()->consumer.update("UPDATE u07_canonical_event_binding SET occurred_at='changed' WHERE canonical_event_id=?",f.event));
        assertThrows(RuntimeException.class,()->consumer.update("DELETE FROM canonical_business_event WHERE event_id=?",f.event));pair(f.event);
    }
    @Test void twoPreCommitFailureWindowsRollBackBothTables(){
        for(String phase:new String[]{"after_foundation_flush","after_binding_insert"}){
            Fixture f=new Fixture();Result r=adapter(p->{if(p.equals(phase))throw new Block(Status.UNAVAILABLE);}).admit(f.req(),f.scope());
            assertEquals(Status.UNAVAILABLE,r.status);assertEquals(Durability.NOT_COMMITTED,r.durability);assertNull(r.canonicalId);absent(f.event);
            assertEquals(Status.STORED,adapter().admit(f.req(),f.scope()).status);pair(f.event);
        }
    }
    @Test void actualBindingConstraintFailureRollsBackFoundationFlush(){
        Fixture f=new Fixture();admin.execute("CREATE TRIGGER synthetic_binding_fail BEFORE INSERT ON u07_canonical_event_binding FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='synthetic injected constraint'");
        try{Result r=adapter().admit(f.req(),f.scope());assertNull(r.canonicalId);assertNotEquals(Durability.COMMITTED,r.durability);absent(f.event);}
        finally{admin.execute("DROP TRIGGER synthetic_binding_fail");}
    }
    @Test void lostResponseAndUnknownOutcomeNeverCreateSecondIdentity(){
        Fixture f=new Fixture();assertThrows(IllegalStateException.class,()->{Result r=adapter().admit(f.req(),f.scope());assertEquals(Status.STORED,r.status);throw new IllegalStateException("lost response");});
        assertEquals(f.event,adapter().read(f.req(),f.scope()).canonicalId);assertEquals(Status.REATTACHED,adapter().admit(f.alias(),f.scope()).status);pair(f.event);
        Fixture g=new Fixture();Result unknown=adapter(p->{if(p.equals("before_commit"))throw new IllegalStateException("ambiguous fault");}).admit(g.req(),g.scope());
        assertEquals(Status.UNKNOWN,unknown.status);assertNull(unknown.canonicalId);assertEquals(g.key(),unknown.queryKey);absent(g.event);
        // ABSENT/unresolved read does not mutate the original UNKNOWN observation.
        assertEquals(Status.ABSENT,adapter().read(g.req(),g.scope()).status);assertEquals(Durability.UNKNOWN,unknown.durability);
    }
    @Test void legacyOrphanAndCorruptCommittedBindingNeverRepairFromRetry(){
        Fixture f=new Fixture();new TransactionTemplate(manager).execute(s->{ledger.resolveOrCreate(f.event,f.c,"USER_ANSWER",f.key(),f.fields[20]);em.flush();return null;});
        assertEquals(Status.INCONSISTENT,adapter().admit(f.req(),f.scope()).status);assertEquals(0,count("u07_canonical_event_binding",f.event));
        Fixture g=new Fixture();adapter().admit(g.req(),g.scope());admin.update("UPDATE u07_canonical_event_binding SET binding_fingerprint=? WHERE canonical_event_id=?",String.join("",Collections.nCopies(64,"0")),g.event);
        assertEquals(Status.INTEGRITY_CONFLICT,adapter().read(g.req(),g.scope()).status);pair(g.event);
    }
    @Test void resumeReattachesExactTargetAndNeverAddsAnswerBytes(){
        Fixture f=new Fixture();adapter().admit(f.req(),f.scope());String target=f.event;
        f.fields[1]="RESUME_REQUEST";f.fields[15]=null;f.fields[16]=null;f.fields[17]=target;f.event=id();f.token=id();f.nextProof();
        Result r=adapter().admit(f.req(),f.scope());assertEquals(Status.TARGET_REATTACHED,r.status);assertEquals(target,r.targetId);pair(f.event);
        assertNull(admin.queryForObject("SELECT synthetic_answer_bytes FROM u07_canonical_event_binding WHERE canonical_event_id=?",byte[].class,f.event));
        assertEquals(Status.TARGET_REATTACHED,adapter().admit(f.alias(),f.scope()).status);
        f.fields[17]=id();f.event=id();f.token=id();f.nextProof();assertEquals(Status.UNRESOLVED_TARGET,adapter().admit(f.req(),f.scope()).status);absent(f.event);
        assertThrows(IllegalArgumentException.class,()->issuer.issue(id(),f.fields,f.token,new byte[]{1},WHEN));
    }
    @Test void pendingUniqueIndexRaceHasOneWinnerAndFreshTransactionRead() throws Exception {
        Fixture f=new Fixture();String e1=f.event,e2=id();CyclicBarrier bothPending=new CyclicBarrier(2);AtomicInteger fresh=new AtomicInteger();
        SourceBindingAdapter a=adapter(p->{if(p.equals("fresh_winner_read"))fresh.incrementAndGet();if(p.equals("pending_insert"))try{bothPending.await(8,TimeUnit.SECONDS);}catch(Exception e){throw new IllegalStateException(e);}});
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            Future<Result> one=pool.submit(()->a.admit(new Request(e1,f.token,f.ref,WHEN),f.scope()));
            Future<Result> two=pool.submit(()->a.admit(new Request(e2,f.token,f.ref,WHEN),f.scope()));
            Result r1=one.get(20,TimeUnit.SECONDS),r2=two.get(20,TimeUnit.SECONDS);
            assertEquals(r1.canonicalId,r2.canonicalId);assertNotNull(r1.canonicalId);
            assertEquals(1,Arrays.asList(r1.status,r2.status).stream().filter(s->s==Status.STORED).count());
            assertEquals(1,fresh.get());pair(r1.canonicalId);absent(r1.canonicalId.equals(e1)?e2:e1);
        }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void revocationFirstBlocksNewWriteButHistoricalReadSurvives(){
        Fixture f=new Fixture();adapter().admit(f.req(),f.scope());issuer.epoch(f.tenant,false);
        assertEquals(Status.DENIED,adapter().admit(f.alias(),f.scope()).status);assertEquals(f.event,adapter().read(f.req(),f.scope()).canonicalId);
        Fixture g=new Fixture();issuer.epoch(g.tenant,false);assertEquals(Status.DENIED,adapter().admit(g.req(),g.scope()).status);absent(g.event);
    }
    @Test void effectFirstHoldsSharedEpochUntilCommitThenRevocationWins() throws Exception {
        Fixture f=new Fixture();CountDownLatch ready=new CountDownLatch(1),release=new CountDownLatch(1),contender=new CountDownLatch(1);
        SourceBindingAdapter a=adapter(p->{if(p.equals("before_commit")){ready.countDown();try{if(!release.await(8,TimeUnit.SECONDS))throw new IllegalStateException("timeout");}catch(InterruptedException e){throw new IllegalStateException(e);}}});
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            Future<Result> write=pool.submit(()->a.admit(f.req(),f.scope()));assertTrue(ready.await(8,TimeUnit.SECONDS));
            Future<?> revoke=pool.submit(()->{contender.countDown();issuer.epoch(f.tenant,false);});assertTrue(contender.await(8,TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,()->revoke.get(200,TimeUnit.MILLISECONDS));release.countDown();
            assertEquals(Status.STORED,write.get(20,TimeUnit.SECONDS).status);revoke.get(20,TimeUnit.SECONDS);pair(f.event);
            assertEquals(Status.DENIED,adapter().admit(f.alias(),f.scope()).status);
        }finally{release.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void codecMatchesIndependentGoldenAndRejectsMalformedInputs() throws Exception {
        Properties golden=new Properties();golden.load(getClass().getResourceAsStream("/golden.properties"));
        String[] f=new String[21];for(int i=0;i<21;i++)f[i]="NULL".equals(golden.getProperty("f"+i))?null:golden.getProperty("f"+i);
        assertEquals(golden.getProperty("binding"),BindingCodec.hash(BindingCodec.encode(f)));
        assertEquals(golden.getProperty("payload"),BindingCodec.payload(f));assertEquals(golden.getProperty("key"),BindingCodec.key(f,"synthetic-token"));
        assertArrayEquals(f,BindingCodec.decode(BindingCodec.encode(f)));
        for(int index:new int[]{0,1,2,3,14,15,16,17,20}){
            String[] bad=f.clone();bad[index]="bad";assertThrows(IllegalArgumentException.class,()->BindingCodec.encode(bad));
        }
        byte[] bytes=BindingCodec.encode(f);assertThrows(IllegalArgumentException.class,()->BindingCodec.decode(Arrays.copyOf(bytes,bytes.length+1)));
        assertThrows(IllegalArgumentException.class,()->BindingCodec.text("\ud800"));
        assertThrows(IllegalArgumentException.class,()->BindingCodec.text(""));
    }
    @Test void illegalResultCombinationsCannotPublishSuccess(){
        assertThrows(IllegalArgumentException.class,()->Result.committed(Status.DENIED,"synthetic-x","key",null,Attempt.ATTEMPTED));
        assertThrows(IllegalArgumentException.class,()->Result.committed(Status.STORED,null,"key",null,Attempt.ATTEMPTED));
        assertThrows(IllegalArgumentException.class,()->Result.blocked(Status.STORED,Attempt.NOT_ATTEMPTED));
        assertThrows(IllegalArgumentException.class,()->Result.unknown(null));
        assertThrows(IllegalArgumentException.class,()->Result.blocked(Status.UNKNOWN,Attempt.ATTEMPTED));
        assertThrows(IllegalArgumentException.class,()->Result.committed(Status.STORED,"synthetic-x","key",null,Attempt.NOT_ATTEMPTED));
        assertThrows(IllegalArgumentException.class,()->Result.committed(Status.TARGET_REATTACHED,"synthetic-x","key",null,Attempt.ATTEMPTED));
        assertThrows(IllegalArgumentException.class,()->Result.committed(Status.REATTACHED,"synthetic-x","key","synthetic-target",Attempt.NOT_ATTEMPTED));
        assertNull(Result.blocked(Status.DENIED,Attempt.NOT_ATTEMPTED).canonicalId);
    }
}
