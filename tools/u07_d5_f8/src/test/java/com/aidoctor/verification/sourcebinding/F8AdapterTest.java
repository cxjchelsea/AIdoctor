package com.aidoctor.verification.sourcebinding;
import com.aidoctor.diagnosis.runtime.foundation.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.aidoctor.verification.sourcebinding.F8Result.*;
class F8AdapterTest {
 static final String URL="jdbc:mysql://127.0.0.1:33324/u07_f8_adapter?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
 static final LocalDateTime T=LocalDateTime.of(2030,1,1,0,0);
 static final Instant WHEN=Instant.parse("2026-01-01T00:00:00Z");
 static AnnotationConfigApplicationContext context,sourceContext;
 static DataSource ds;static EntityManager em;static PlatformTransactionManager manager;
 static JdbcTemplate db,admin;static TestAuthority sourceIssuer;static F8TestAuthority owner;
 static TestAuthority.CipherBox box;static SourceBindingAdapter admission;
 static final AtomicInteger ids=new AtomicInteger();
 @Configuration @EnableTransactionManagement @EnableJpaRepositories(basePackageClasses=CanonicalBusinessEventRepository.class)
 static class Config {
  @Bean DataSource dataSource(){if(!URL.equals(System.getenv("U07_F8_ADAPTER_URL")))throw new IllegalArgumentException("fixed disposable database required");return new DriverManagerDataSource(URL,"f8_adapter","synthetic-f8-only");}
  @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds){LocalContainerEntityManagerFactoryBean f=new LocalContainerEntityManagerFactoryBean();f.setDataSource(ds);f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());f.setPackagesToScan("com.aidoctor.diagnosis.runtime.foundation");Properties p=new Properties();p.setProperty("hibernate.hbm2ddl.auto","none");p.setProperty("hibernate.dialect","org.hibernate.dialect.MySQL8Dialect");f.setJpaProperties(p);return f;}
  @Bean PlatformTransactionManager transactionManager(EntityManagerFactory emf,DataSource ds){JpaTransactionManager m=new JpaTransactionManager(emf);m.setDataSource(ds);return m;}
  @Bean CanonicalBusinessEventLedger ledger(CanonicalBusinessEventRepository r){return new CanonicalBusinessEventLedger(r);}
 }
 @Configuration @EnableTransactionManagement @EnableJpaRepositories(basePackageClasses=CanonicalBusinessEventRepository.class)
 static class SourceConfig extends Config {@Override @Bean DataSource dataSource(){return new DriverManagerDataSource(URL,"synthetic_adapter","synthetic-only-adapter");}}
 @BeforeAll static void setup(){
  context=new AnnotationConfigApplicationContext(Config.class);sourceContext=new AnnotationConfigApplicationContext(SourceConfig.class);
  ds=context.getBean(DataSource.class);db=new JdbcTemplate(ds);em=SharedEntityManagerCreator.createSharedEntityManager(context.getBean(EntityManagerFactory.class));manager=context.getBean(PlatformTransactionManager.class);
  admin=new JdbcTemplate(new DriverManagerDataSource(URL,"root","synthetic-ci-only"));box=new TestAuthority.CipherBox("0123456789abcdef".getBytes(StandardCharsets.US_ASCII));
  DataSource issuerDs=new DriverManagerDataSource(URL,"synthetic_issuer","synthetic-only-issuer");sourceIssuer=new TestAuthority(new JdbcTemplate(issuerDs),new TransactionTemplate(new DataSourceTransactionManager(issuerDs)),box,BindingCodec.PROFILE);
  DataSource od=new DriverManagerDataSource(URL,"f8_owner_issuer","synthetic-f8-owner-only");owner=new F8TestAuthority(new JdbcTemplate(od),new TransactionTemplate(new DataSourceTransactionManager(od)));
  admission=new SourceBindingAdapter(sourceContext.getBean(DataSource.class),sourceContext.getBean(PlatformTransactionManager.class),sourceContext.getBean(CanonicalBusinessEventLedger.class),sourceContext.getBean(CanonicalBusinessEventRepository.class),SharedEntityManagerCreator.createSharedEntityManager(sourceContext.getBean(EntityManagerFactory.class)),box,BindingCodec.PROFILE);
 }
 @AfterAll static void close(){if(context!=null)context.close();if(sourceContext!=null)sourceContext.close();}
 static String id(){return "synthetic-f8-"+ids.incrementAndGet();}
 static F8Adapter adapter(){return adapter(p->{});}
 static F8Adapter adapter(F8Adapter.Probe p){return new F8Adapter(ds,manager,em,box,BindingCodec.PROFILE,T,p);}
 static class Fixture {
  String[] f;String event=id(),token=id(),ref=id();byte[] answer="synthetic-answer:f8".getBytes(StandardCharsets.UTF_8);
  Fixture(){f=new String[]{BindingCodec.CONTRACT,"USER_ANSWER",BindingCodec.ENV,BindingCodec.PROFILE,id(),id(),id(),id(),id(),id(),id(),id(),id(),null,"0",null,BindingCodec.hash(answer),null,id(),id(),"placeholder"};sourceIssuer.epoch(f[4],true);persist();owner.issue(f,T.plusSeconds(10));}
  void persist(){f[15]="USER_ANSWER".equals(f[1])?"u07db:v1:"+BindingCodec.key(f,token)+":answer":null;f[20]=BindingCodec.payload(f);sourceIssuer.issue(ref,f,token,"USER_ANSWER".equals(f[1])?answer:null,WHEN);assertEquals(SourceBindingAdapter.Status.STORED,admission.admit(req(),scope()).status);}
  SourceBindingAdapter.Request req(){return new SourceBindingAdapter.Request(event,token,ref,WHEN);}
  SourceBindingAdapter.Scope scope(){return new SourceBindingAdapter.Scope(f[4],f[5],f[6]);}
  F8Identity identity(){return new TransactionTemplate(manager).execute(s->new F8Identity(new SourceBindingVerifier(db,em,box).verify(req(),scope())));}
  void next(String text){event=id();token=id();ref=id();answer=("synthetic-answer:"+text).getBytes(StandardCharsets.UTF_8);f[16]=BindingCodec.hash(answer);persist();}
 }
 int decisions(F8Identity i){return admin.queryForObject("SELECT COUNT(*) FROM f8_decision WHERE id=?",Integer.class,i.decisionId);}
 int claims(F8Identity i){return admin.queryForObject("SELECT COUNT(*) FROM f8_claim WHERE wait_key=?",Integer.class,i.waitKey);}
 @Test void acceptedAndHistoricalReplayShareIdentity(){Fixture f=new Fixture();F8Result r=adapter().finalizeAnswer(f.req(),f.scope());assertEquals(Operational.FINALIZED,r.operational);assertEquals(Verdict.ACCEPTED,r.confirmed.verdict);assertEquals(Durability.COMMITTED,r.durability);
  F8Identity i=f.identity();owner.current(i,null,"CANCELLED",T.minusSeconds(1));owner.permission(i,true,false);
  assertEquals(Operational.HISTORICAL_FOUND,adapter().finalizeAnswer(f.req(),f.scope()).operational);assertEquals(F8HistoricalReader.State.FOUND_MATCH,adapter().read(f.req(),f.scope()).state);}
 @Test void distinctAnswerPendingDoesNotDuplicate(){Fixture f=new Fixture();adapter().finalizeAnswer(f.req(),f.scope());f.next("changed");F8Result r=adapter().finalizeAnswer(f.req(),f.scope());assertEquals(Operational.DEFER,r.operational);assertEquals(Attempt.NOT_ATTEMPTED,r.attempt);assertEquals(0,decisions(f.identity()));}
 @ParameterizedTest @CsvSource({"same,DUPLICATE","changed,REJECTED"})
 void independentAppliedEvidenceControlsEquivalence(String text,Verdict expected){Fixture f=new Fixture();adapter().finalizeAnswer(f.req(),f.scope());owner.applied(f.identity());f.next(text.equals("same")?"f8":"changed");F8Result r=adapter().finalizeAnswer(f.req(),f.scope());assertEquals(expected,r.confirmed.verdict);assertEquals(F8HistoricalReader.State.FOUND_MATCH,adapter().read(f.req(),f.scope()).state);}
 @Test void appliedEquivalentSurvivesMovedAndExpiredOwner(){Fixture f=new Fixture();adapter().finalizeAnswer(f.req(),f.scope());F8Identity i=f.identity();owner.applied(i);owner.current(i,null,"CANCELLED",T.minusSeconds(1));f.next("f8");assertEquals(Verdict.DUPLICATE,adapter().finalizeAnswer(f.req(),f.scope()).confirmed.verdict);}
 @ParameterizedTest @CsvSource({"CLEARED,REJECTED","MOVED,REJECTED","EXPIRED,EXPIRED","CANCELLED,REJECTED"})
 void higherPriorityNegativeWithPendingWinner(String mode,Verdict expected){Fixture f=new Fixture();adapter().finalizeAnswer(f.req(),f.scope());owner.current(f.identity(),mode.equals("MOVED")?id():null,mode.equals("CANCELLED")?"CANCELLED":"WAITING",mode.equals("EXPIRED")?T:T.plusSeconds(10));f.next("changed");assertEquals(expected,adapter().finalizeAnswer(f.req(),f.scope()).confirmed.verdict);assertEquals(F8HistoricalReader.State.FOUND_MATCH,adapter().read(f.req(),f.scope()).state);}
 @Test void noOwnerEvidenceDoesNotBecomeNegative(){Fixture f=new Fixture();F8Identity i=f.identity();admin.update("DELETE FROM f8_owner WHERE ref=?",f.f[8]);F8Result r=adapter().finalizeAnswer(f.req(),f.scope());assertEquals(Operational.DEFER,r.operational);assertEquals(Attempt.NOT_ATTEMPTED,r.attempt);assertEquals(0,decisions(i));}
 @Test void permissionReadAndFinalizeAreSeparate(){Fixture f=new Fixture();F8Identity i=f.identity();owner.permission(i,true,false);assertEquals(Operational.DENIED,adapter().finalizeAnswer(f.req(),f.scope()).operational);assertEquals(F8HistoricalReader.State.ABSENT,adapter().read(f.req(),f.scope()).state);assertEquals(0,decisions(i));}
 @Test void sourceProofAndCipherCorruptionAreRejected(){Fixture f=new Fixture();F8Identity i=f.identity();admin.update("UPDATE u07_test_authority_record SET manifest_digest='forged' WHERE record_id=?",f.ref);assertNull(adapter().finalizeAnswer(f.req(),f.scope()).confirmed);assertEquals(0,decisions(i));}
 @Test void outerAndWrongResourceAreBlocked(){Fixture f=new Fixture();new TransactionTemplate(manager).execute(s->{assertThrows(IllegalStateException.class,()->adapter().finalizeAnswer(f.req(),f.scope()));return null;});
  F8Adapter wrong=new F8Adapter(new DriverManagerDataSource(),manager,em,box,BindingCodec.PROFILE);assertEquals(Reason.RESOURCE_MISMATCH,wrong.finalizeAnswer(f.req(),f.scope()).reason);
  F8Adapter jdbcOnly=new F8Adapter(ds,new DataSourceTransactionManager(ds),em,box,BindingCodec.PROFILE);assertEquals(Reason.RESOURCE_MISMATCH,jdbcOnly.finalizeAnswer(f.req(),f.scope()).reason);}
 @ParameterizedTest @CsvSource({"after_sentinel","after_decision","after_claim","before_commit"})
 void confirmedRollbackAtEveryWriteBoundary(String phase){Fixture f=new Fixture();F8Identity i=f.identity();F8Result r=adapter(p->{if(p.equals(phase))throw new IllegalStateException("fault");}).finalizeAnswer(f.req(),f.scope());assertEquals(Operational.RETRYABLE_FAILURE,r.operational);assertEquals(Attempt.ATTEMPTED,r.attempt);assertEquals(Durability.NOT_COMMITTED,r.durability);assertNull(r.confirmed);assertEquals(0,decisions(i));assertEquals(0,claims(i));}
 @Test void lostPostCommitResponseRequiresSeparateRead(){Fixture f=new Fixture();F8Result r=adapter(p->{if(p.equals("after_commit"))throw new IllegalStateException("lost response");}).finalizeAnswer(f.req(),f.scope());assertEquals(Durability.UNKNOWN,r.durability);assertNull(r.confirmed);assertEquals(F8HistoricalReader.State.FOUND_MATCH,adapter().read(f.req(),f.scope()).state);}
 @Test void claimScopeCorruptionCannotDisappear(){Fixture f=new Fixture();F8Identity i=f.identity();admin.update("INSERT INTO f8_claim VALUES(?,?,?,?,?,?,?,NULL,NULL,2)",i.waitKey,f.f[2],f.f[3],id(),f.f[5],f.f[7],f.f[9]);assertEquals(Operational.INTEGRITY_CONFLICT,adapter().finalizeAnswer(f.req(),f.scope()).operational);assertEquals(0,decisions(i));}
 @Test void acceptedMissingClaimIsInconsistent(){Fixture f=new Fixture();adapter().finalizeAnswer(f.req(),f.scope());admin.update("DELETE FROM f8_claim WHERE wait_key=?",f.identity().waitKey);assertEquals(F8HistoricalReader.State.INCONSISTENT,adapter().read(f.req(),f.scope()).state);assertEquals(Operational.INTEGRITY_CONFLICT,adapter().finalizeAnswer(f.req(),f.scope()).operational);}
 @Test void expiredWithoutOwnClaimIsNormal(){Fixture f=new Fixture();owner.current(f.identity(),null,"WAITING",T);assertEquals(Verdict.EXPIRED,adapter().finalizeAnswer(f.req(),f.scope()).confirmed.verdict);assertEquals(0,claims(f.identity()));assertEquals(F8HistoricalReader.State.FOUND_MATCH,adapter().read(f.req(),f.scope()).state);}
 @Test void twoSameAnswerCallsProduceOneDecision() throws Exception {Fixture f=new Fixture();ExecutorService pool=Executors.newFixedThreadPool(2);try{Future<F8Result>a=pool.submit(()->adapter().finalizeAnswer(f.req(),f.scope()));Future<F8Result>b=pool.submit(()->adapter().finalizeAnswer(f.req(),f.scope()));assertNotNull(a.get(10,TimeUnit.SECONDS).confirmed);assertNotNull(b.get(10,TimeUnit.SECONDS).confirmed);assertEquals(1,decisions(f.identity()));assertEquals(1,claims(f.identity()));}finally{pool.shutdownNow();}}
 @Test void actualAdapterClockCrossingRollsBackSentinel(){Fixture f=new Fixture();F8Identity i=f.identity();admin.update("UPDATE f8_owner SET deadline=UTC_TIMESTAMP(6)+INTERVAL 600000 MICROSECOND WHERE ref=?",f.f[5]);F8Adapter live=new F8Adapter(ds,manager,em,box,BindingCodec.PROFILE,null,p->{if(p.equals("after_sentinel"))db.queryForObject("SELECT SLEEP(0.8)",Integer.class);});F8Result r=live.finalizeAnswer(f.req(),f.scope());assertEquals(Reason.SENTINEL_ROLLBACK,r.reason);assertEquals(Durability.NOT_COMMITTED,r.durability);assertEquals(0,claims(i));assertEquals(0,decisions(i));assertEquals(Verdict.EXPIRED,new F8Adapter(ds,manager,em,box,BindingCodec.PROFILE).finalizeAnswer(f.req(),f.scope()).confirmed.verdict);}
 @Test void consumerCannotMutateOwnersAppliedOrCanonical(){assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE f8_owner SET version=version+1"));assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("DELETE FROM canonical_business_event"));assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("DELETE FROM f8_applied"));}
 @Test void resultMatrixRejectsInvalidCombinations(){
  Receipt receipt=new Receipt("d","c","f","w",Verdict.ACCEPTED,null);assertThrows(IllegalArgumentException.class,()->new F8Result(Operational.FINALIZED,Attempt.NOT_ATTEMPTED,Durability.COMMITTED,Reason.NONE,"d",receipt));
  assertThrows(IllegalArgumentException.class,()->new F8Result(Operational.RECONCILIATION_REQUIRED,Attempt.ATTEMPTED,Durability.UNKNOWN,Reason.COMMIT_UNKNOWN,"d",receipt));
  assertThrows(IllegalArgumentException.class,()->new F8Result(Operational.DEFER,Attempt.ATTEMPTED,Durability.NOT_COMMITTED,Reason.EVIDENCE,null,null));
  assertThrows(IllegalArgumentException.class,()->new F8Result(Operational.RETRYABLE_FAILURE,Attempt.NOT_ATTEMPTED,Durability.NOT_COMMITTED,Reason.DB_FAILURE,null,null));
 }
}
