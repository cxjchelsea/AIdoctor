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
  void persist(){f[15]="USER_ANSWER".equals(f[1])?"u07db:v1:"+BindingCodec.key(f,token)+":answer":null;f[20]=BindingCodec.payload(f);sourceIssuer.issue(ref,f,token,"USER_ANSWER".equals(f[1])?answer:null,WHEN);assertEquals("RESUME_REQUEST".equals(f[1])?SourceBindingAdapter.Status.TARGET_REATTACHED:SourceBindingAdapter.Status.STORED,admission.admit(req(),scope()).status);}
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
 @Test void resumeResolvesTargetWithoutNewDecision(){Fixture f=new Fixture();F8Result original=adapter().finalizeAnswer(f.req(),f.scope());String target=f.event;
  f.event=id();f.token=id();f.ref=id();f.f[1]="RESUME_REQUEST";f.f[16]=null;f.f[17]=target;f.persist();
  F8Result result=adapter().finalizeAnswer(f.req(),f.scope());assertEquals(Operational.HISTORICAL_FOUND,result.operational);assertEquals(original.confirmed.decisionId,result.confirmed.decisionId);assertEquals(target,result.confirmed.canonicalId);
  assertEquals(0,admin.queryForObject("SELECT COUNT(*) FROM f8_decision WHERE canonical_id=?",Integer.class,f.event));
 }
 @Test void aliasKeepsCanonicalDecisionIdentity(){Fixture f=new Fixture();F8Result first=adapter().finalizeAnswer(f.req(),f.scope());SourceBindingAdapter.Request alias=new SourceBindingAdapter.Request(id(),f.token,f.ref,WHEN.plusSeconds(9));assertEquals(first.confirmed.decisionId,adapter().finalizeAnswer(alias,f.scope()).confirmed.decisionId);}
 @Test void crossScopeDoesNotCreateDecision(){Fixture f=new Fixture();F8Identity i=f.identity();assertEquals(Operational.DENIED,adapter().finalizeAnswer(f.req(),new SourceBindingAdapter.Scope(id(),f.f[5],f.f[6])).operational);assertEquals(0,decisions(i));}
 @ParameterizedTest @CsvSource({"cipher","binding","canonical","timestamp"})
 void immutableProofDamageBlocks(String part){Fixture f=new Fixture();F8Identity i=f.identity();
  if(part.equals("cipher"))admin.update("UPDATE u07_test_authority_record SET answer_cipher=? WHERE record_id=?",new byte[30],f.ref);
  if(part.equals("binding"))admin.update("UPDATE u07_canonical_event_binding SET binding_fingerprint='forged' WHERE canonical_event_id=?",f.event);
  if(part.equals("canonical"))admin.update("UPDATE canonical_business_event SET payload_digest='forged' WHERE event_id=?",f.event);
  if(part.equals("timestamp"))admin.update("UPDATE u07_canonical_event_binding SET occurred_at='forged' WHERE canonical_event_id=?",f.event);
  assertNull(adapter().finalizeAnswer(f.req(),f.scope()).confirmed);assertEquals(0,decisions(i));}
 @Test void p8FinalizerZeroRowsIsAttemptedRollback(){Fixture f=new Fixture();F8Identity i=f.identity();admin.update("UPDATE f8_owner SET state='PENDING' WHERE ref=?",f.f[10]);F8Result r=adapter().finalizeAnswer(f.req(),f.scope());assertEquals(Operational.RETRYABLE_FAILURE,r.operational);assertEquals(Attempt.ATTEMPTED,r.attempt);assertEquals(Durability.NOT_COMMITTED,r.durability);assertEquals(0,decisions(i));assertEquals(0,claims(i));}
 @Test void damagedAppliedOriginalProofIsNotDuplicate(){Fixture f=new Fixture();adapter().finalizeAnswer(f.req(),f.scope());owner.applied(f.identity());String original=f.event;f.next("f8");admin.update("UPDATE u07_canonical_event_binding SET binding_fingerprint='forged' WHERE canonical_event_id=?",original);assertEquals(Operational.INTEGRITY_CONFLICT,adapter().finalizeAnswer(f.req(),f.scope()).operational);assertEquals(0,decisions(f.identity()));}
 @Test void wrongAppliedGenerationIsInconsistent(){Fixture f=new Fixture();adapter().finalizeAnswer(f.req(),f.scope());F8Identity i=f.identity();owner.applied(i);admin.update("UPDATE f8_applied SET generation=2 WHERE decision_id=?",i.decisionId);f.next("f8");assertEquals(Operational.INTEGRITY_CONFLICT,adapter().finalizeAnswer(f.req(),f.scope()).operational);}
 @Test void duplicateCannotBecomeClaimWinner(){Fixture f=new Fixture();adapter().finalizeAnswer(f.req(),f.scope());owner.applied(f.identity());f.next("f8");F8Result r=adapter().finalizeAnswer(f.req(),f.scope());admin.update("UPDATE f8_claim SET canonical_id=?,decision_id=? WHERE wait_key=?",f.event,r.confirmed.decisionId,f.identity().waitKey);assertEquals(F8HistoricalReader.State.INCONSISTENT,adapter().read(f.req(),f.scope()).state);}
 @Test void legacySentinelIsAbsentAndCanBeCompleted(){Fixture f=new Fixture();F8Identity i=f.identity();admin.update("INSERT INTO f8_claim VALUES(?,?,?,?,?,?,?,NULL,NULL,0)",i.waitKey,f.f[2],f.f[3],f.f[4],f.f[5],f.f[7],f.f[9]);assertEquals(F8HistoricalReader.State.ABSENT,adapter().read(f.req(),f.scope()).state);assertEquals(Verdict.ACCEPTED,adapter().finalizeAnswer(f.req(),f.scope()).confirmed.verdict);}
 @Test void twoDifferentAnswersProduceOneAcceptedWinner() throws Exception {Fixture f=new Fixture();SourceBindingAdapter.Request original=f.req();f.next("second");SourceBindingAdapter.Request second=f.req();ExecutorService pool=Executors.newFixedThreadPool(2);try{
  Future<F8Result>a=pool.submit(()->adapter().finalizeAnswer(original,f.scope()));Future<F8Result>b=pool.submit(()->adapter().finalizeAnswer(second,f.scope()));List<F8Result> results=Arrays.asList(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
  assertEquals(1,results.stream().filter(r->r.confirmed!=null&&r.confirmed.verdict==Verdict.ACCEPTED).count());assertEquals(1,results.stream().filter(r->r.operational==Operational.DEFER).count());assertEquals(1,claims(f.identity()));
 }finally{pool.shutdownNow();}}
 @ParameterizedTest @CsvSource({"true","false"})
 void ownerAndAdapterSerializeInBothOrders(boolean ownerFirst)throws Exception{Fixture f=new Fixture();F8Identity i=f.identity();ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch locked=new CountDownLatch(1),release=new CountDownLatch(1),started=new CountDownLatch(1),acquired=new CountDownLatch(1);
  try{Future<?> first;
   if(ownerFirst)first=pool.submit(()->owner.tx.execute(s->{owner.lock(i.scopeKey);locked.countDown();await(release);owner.db.update("UPDATE f8_owner SET state='CANCELLED',current_wait=NULL,version=version+1 WHERE ref=?",f.f[5]);return null;}));
   else first=pool.submit(()->adapter(p->{if(p.equals("after_sentinel")){locked.countDown();await(release);}}).finalizeAnswer(f.req(),f.scope()));
   assertTrue(locked.await(5,TimeUnit.SECONDS));Future<?> second;
   if(ownerFirst)second=pool.submit(()->{started.countDown();F8Result r=adapter().finalizeAnswer(f.req(),f.scope());acquired.countDown();assertEquals(Verdict.REJECTED,r.confirmed.verdict);});
   else second=pool.submit(()->owner.tx.execute(s->{started.countDown();owner.lock(i.scopeKey);acquired.countDown();owner.db.update("UPDATE f8_owner SET state='CANCELLED',version=version+1 WHERE ref=?",f.f[5]);return null;}));
   assertTrue(started.await(5,TimeUnit.SECONDS));assertFalse(acquired.await(150,TimeUnit.MILLISECONDS));release.countDown();first.get(8,TimeUnit.SECONDS);second.get(8,TimeUnit.SECONDS);assertEquals(ownerFirst?Verdict.REJECTED:Verdict.ACCEPTED,adapter().read(f.req(),f.scope()).receipt.verdict);
  }finally{release.countDown();pool.shutdownNow();}}
 static void await(CountDownLatch latch){try{if(!latch.await(5,TimeUnit.SECONDS))throw new IllegalStateException("timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
 @Test void actualStatementClockIndependentOfSessionOffset(){Fixture f=new Fixture();F8Identity i=f.identity();admin.update("UPDATE f8_owner SET deadline=UTC_TIMESTAMP(6)-INTERVAL 1 SECOND WHERE ref=?",f.f[5]);F8Adapter live=new F8Adapter(ds,manager,em,box,BindingCodec.PROFILE,null,p->{if(p.equals("source_verified"))db.execute("SET time_zone='+08:00'");});F8Result r=live.finalizeAnswer(f.req(),f.scope());assertEquals(Verdict.EXPIRED,r.confirmed.verdict);assertEquals(F8HistoricalReader.State.FOUND_MATCH,adapter().read(f.req(),f.scope()).state);}
 static class AmbiguousManager extends JpaTransactionManager {
  final boolean failCommit,failRollback;
  AmbiguousManager(boolean commit,boolean rollback){super(context.getBean(EntityManagerFactory.class));setDataSource(ds);failCommit=commit;failRollback=rollback;}
  @Override protected void doCommit(DefaultTransactionStatus status){super.doCommit(status);if(failCommit)throw new IllegalStateException("injected lost commit acknowledgement");}
  @Override protected void doRollback(DefaultTransactionStatus status){super.doRollback(status);if(failRollback)throw new IllegalStateException("injected lost rollback acknowledgement");}
 }
 @Test void injectedCommitUnknownRecoversViaFreshRead(){Fixture f=new Fixture();F8Adapter uncertain=new F8Adapter(ds,new AmbiguousManager(true,false),em,box,BindingCodec.PROFILE,T,p->{});F8Result r=uncertain.finalizeAnswer(f.req(),f.scope());assertEquals(Durability.UNKNOWN,r.durability);assertNull(r.confirmed);assertEquals(F8HistoricalReader.State.FOUND_MATCH,adapter().read(f.req(),f.scope()).state);}
 @Test void injectedRollbackUnknownDoesNotClaimNotCommitted(){Fixture f=new Fixture();F8Adapter uncertain=new F8Adapter(ds,new AmbiguousManager(false,true),em,box,BindingCodec.PROFILE,T,p->{if(p.equals("after_sentinel"))throw new IllegalStateException("trigger");});F8Result r=uncertain.finalizeAnswer(f.req(),f.scope());assertEquals(Durability.UNKNOWN,r.durability);assertNull(r.confirmed);assertNotNull(r.queryKey);}
 @Test void invalidInputAndReadPermissionDenialDoNotWrite(){Fixture f=new Fixture();F8Identity i=f.identity();assertEquals(Operational.INVALID_INPUT,adapter().finalizeAnswer(new SourceBindingAdapter.Request("real-event",f.token,f.ref,WHEN),f.scope()).operational);owner.permission(i,false,true);assertEquals(F8HistoricalReader.State.MISMATCH,adapter().read(f.req(),f.scope()).state);assertEquals(Operational.DENIED,adapter().finalizeAnswer(f.req(),f.scope()).operational);assertEquals(0,decisions(i));}
 @Test void unavailableFreshReadDoesNotResolveUnknown(){Fixture f=new Fixture();F8Result unknown=adapter(p->{if(p.equals("after_commit"))throw new IllegalStateException();}).finalizeAnswer(f.req(),f.scope());assertEquals(Durability.UNKNOWN,unknown.durability);F8Identity i=f.identity();admin.update("DELETE FROM f8_guard WHERE scope_key=?",i.scopeKey);assertEquals(F8HistoricalReader.State.UNAVAILABLE,adapter().read(f.req(),f.scope()).state);assertEquals(Durability.UNKNOWN,unknown.durability);}
 @Test void absentFreshReadDoesNotResolveUnknown(){Fixture f=new Fixture();F8Identity i=f.identity();F8Result unknown=F8Result.unknown(i.decisionId);assertEquals(F8HistoricalReader.State.ABSENT,adapter().read(f.req(),f.scope()).state);assertEquals(Durability.UNKNOWN,unknown.durability);assertNull(unknown.confirmed);}
 @Test void resultValidatorEnumeratesOperationalAttemptDurabilityMatrix(){Receipt r=new Receipt("d","c","f","w",Verdict.ACCEPTED,null);
  for(Operational o:Operational.values())for(Attempt a:Attempt.values())for(Durability d:Durability.values()){
   boolean success=o==Operational.FINALIZED||o==Operational.HISTORICAL_FOUND;
   boolean valid=success?d==Durability.COMMITTED&&a==(o==Operational.FINALIZED?Attempt.ATTEMPTED:Attempt.NOT_ATTEMPTED)
    :o==Operational.RECONCILIATION_REQUIRED?d==Durability.UNKNOWN&&a==Attempt.ATTEMPTED
    :o==Operational.RETRYABLE_FAILURE?d==Durability.NOT_COMMITTED&&a==Attempt.ATTEMPTED
    :o==Operational.DEFER||o==Operational.INVALID_INPUT?d==Durability.NOT_ATTEMPTED&&a==Attempt.NOT_ATTEMPTED
    :(a==Attempt.ATTEMPTED&&d==Durability.NOT_COMMITTED||a==Attempt.NOT_ATTEMPTED&&d==Durability.NOT_ATTEMPTED);
   Reason reason=success?Reason.NONE:o==Operational.RECONCILIATION_REQUIRED?Reason.COMMIT_UNKNOWN:Reason.EVIDENCE;
   if(valid)assertNotNull(new F8Result(o,a,d,reason,"d",success?r:null));else assertThrows(IllegalArgumentException.class,()->new F8Result(o,a,d,reason,"d",success?r:null));
  }
 }
 @Test void adapterDoesNotMutateCanonicalOrRuntimeState(){Fixture f=new Fixture();Map<String,Integer> before=new HashMap<>();for(String table:new String[]{"canonical_business_event","u07_canonical_event_binding","clinical_runtime_binding","clinical_runtime_run","f8_applied"})before.put(table,admin.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class));byte[] frame=admin.queryForObject("SELECT canonical_binding_bytes FROM u07_canonical_event_binding WHERE canonical_event_id=?",byte[].class,f.event);assertEquals(Verdict.ACCEPTED,adapter().finalizeAnswer(f.req(),f.scope()).confirmed.verdict);for(Map.Entry<String,Integer> e:before.entrySet())assertEquals(e.getValue(),admin.queryForObject("SELECT COUNT(*) FROM "+e.getKey(),Integer.class));assertArrayEquals(frame,admin.queryForObject("SELECT canonical_binding_bytes FROM u07_canonical_event_binding WHERE canonical_event_id=?",byte[].class,f.event));assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("DELETE FROM clinical_runtime_binding"));}
 @Test void resultMatrixRejectsInvalidCombinations(){
  Receipt receipt=new Receipt("d","c","f","w",Verdict.ACCEPTED,null);assertThrows(IllegalArgumentException.class,()->new F8Result(Operational.FINALIZED,Attempt.NOT_ATTEMPTED,Durability.COMMITTED,Reason.NONE,"d",receipt));
  assertThrows(IllegalArgumentException.class,()->new F8Result(Operational.RECONCILIATION_REQUIRED,Attempt.ATTEMPTED,Durability.UNKNOWN,Reason.COMMIT_UNKNOWN,"d",receipt));
  assertThrows(IllegalArgumentException.class,()->new F8Result(Operational.DEFER,Attempt.ATTEMPTED,Durability.NOT_COMMITTED,Reason.EVIDENCE,null,null));
  assertThrows(IllegalArgumentException.class,()->new F8Result(Operational.RETRYABLE_FAILURE,Attempt.NOT_ATTEMPTED,Durability.NOT_COMMITTED,Reason.DB_FAILURE,null,null));
 }
 @Test void unjoinedEntityManagerIsRejectedBeforeDml(){Fixture f=new Fixture();F8Identity i=f.identity();EntityManager separate=context.getBean(EntityManagerFactory.class).createEntityManager();try{F8Result r=new F8Adapter(ds,manager,separate,box,BindingCodec.PROFILE).finalizeAnswer(f.req(),f.scope());assertEquals(Reason.RESOURCE_MISMATCH,r.reason);assertEquals(Attempt.NOT_ATTEMPTED,r.attempt);assertEquals(0,decisions(i));}finally{separate.close();}}
 static class AutoCommitManager extends JpaTransactionManager {
  AutoCommitManager(){super(context.getBean(EntityManagerFactory.class));setDataSource(ds);}
  @Override protected void doBegin(Object transaction,TransactionDefinition definition){super.doBegin(transaction,definition);try{DataSourceUtils.getConnection(ds).setAutoCommit(true);}catch(SQLException e){throw new IllegalStateException(e);}}
  @Override protected void doRollback(DefaultTransactionStatus status){try{DataSourceUtils.getConnection(ds).setAutoCommit(false);}catch(SQLException e){throw new IllegalStateException(e);}super.doRollback(status);}
 }
 @Test void actualAutoCommitConnectionIsRejectedBeforeDml(){Fixture f=new Fixture();F8Identity i=f.identity();F8Result r=new F8Adapter(ds,new AutoCommitManager(),em,box,BindingCodec.PROFILE).finalizeAnswer(f.req(),f.scope());assertEquals(Reason.RESOURCE_MISMATCH,r.reason);assertEquals(Attempt.NOT_ATTEMPTED,r.attempt);assertEquals(0,decisions(i));assertEquals(0,claims(i));}
 @Test void differentPhysicalJpaConnectionIsRejectedBeforeDml(){Fixture f=new Fixture();F8Identity i=f.identity();EntityManager separate=context.getBean(EntityManagerFactory.class).createEntityManager();EntityManager reportedJoined=(EntityManager)java.lang.reflect.Proxy.newProxyInstance(EntityManager.class.getClassLoader(),new Class[]{EntityManager.class},(proxy,method,args)->{if(method.getName().equals("isJoinedToTransaction"))return true;try{return method.invoke(separate,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}});try{F8Result r=new F8Adapter(ds,manager,reportedJoined,box,BindingCodec.PROFILE).finalizeAnswer(f.req(),f.scope());assertEquals(Reason.RESOURCE_MISMATCH,r.reason);assertEquals(Attempt.NOT_ATTEMPTED,r.attempt);assertEquals(0,decisions(i));}finally{separate.close();}}
 @Test void actionEpochRevocationBeforeGuardIsObserved() throws Exception {Fixture f=new Fixture();F8Identity i=f.identity();ExecutorService pool=Executors.newSingleThreadExecutor();try{F8Result r=adapter(phase->{if(phase.equals("source_verified"))try{pool.submit(()->owner.permission(i,true,false)).get(5,TimeUnit.SECONDS);}catch(Exception e){throw new IllegalStateException(e);}}).finalizeAnswer(f.req(),f.scope());assertEquals(Operational.DENIED,r.operational);assertEquals(Attempt.NOT_ATTEMPTED,r.attempt);assertEquals(0,decisions(i));}finally{pool.shutdownNow();}}
 // Targeted review oracles. Production implementation is unchanged from PR #401.
 static class ReviewRollbackOnlyManager extends JpaTransactionManager {
  DefaultTransactionStatus current;
  ReviewRollbackOnlyManager(){super(context.getBean(EntityManagerFactory.class));setDataSource(ds);}
  @Override protected void prepareSynchronization(DefaultTransactionStatus status,TransactionDefinition definition){super.prepareSynchronization(status,definition);current=status;}
 }
 @Test void reviewLocalRollbackOnlyMustNotPublishCommittedReceipt(){Fixture f=new Fixture();F8Identity i=f.identity();ReviewRollbackOnlyManager m=new ReviewRollbackOnlyManager();F8Result r=new F8Adapter(ds,m,em,box,BindingCodec.PROFILE,T,phase->{if(phase.equals("before_commit"))m.current.setRollbackOnly();}).finalizeAnswer(f.req(),f.scope());assertEquals(0,decisions(i));assertEquals(0,claims(i));assertEquals(Durability.NOT_COMMITTED,r.durability);assertNull(r.confirmed);}
 @Test void reviewAcceptedMustRejectForgedWinnerReference(){Fixture f=new Fixture();adapter().finalizeAnswer(f.req(),f.scope());F8Identity i=f.identity();admin.update("UPDATE f8_decision SET winner_id=? WHERE id=?",F8Identity.digest("review-forged-winner","missing"),i.decisionId);assertEquals(F8HistoricalReader.State.INCONSISTENT,adapter().read(f.req(),f.scope()).state);}
 @Test void reviewNegativeMustRejectUnrelatedWinnerReference(){Fixture f=new Fixture();owner.current(f.identity(),null,"WAITING",T);assertEquals(Verdict.EXPIRED,adapter().finalizeAnswer(f.req(),f.scope()).confirmed.verdict);F8Identity i=f.identity();admin.update("UPDATE f8_decision SET winner_id=? WHERE id=?",F8Identity.digest("review-forged-winner","missing"),i.decisionId);assertEquals(F8HistoricalReader.State.INCONSISTENT,adapter().read(f.req(),f.scope()).state);}

 @Test void globalRollbackOnlyNeverPublishesReceipt(){Fixture f=new Fixture();F8Identity i=f.identity();F8Result r=adapter(phase->{if(phase.equals("before_commit"))((EntityManagerHolder)TransactionSynchronizationManager.getResource(context.getBean(EntityManagerFactory.class))).getEntityManager().getTransaction().setRollbackOnly();}).finalizeAnswer(f.req(),f.scope());assertEquals(Durability.NOT_COMMITTED,r.durability);assertNull(r.confirmed);assertEquals(0,decisions(i));assertEquals(0,claims(i));}
 @Test void flushFailureRollsBackBeforeReceiptPublication(){Fixture f=new Fixture();F8Identity i=f.identity();F8Result r=adapter(phase->{if(phase.equals("before_commit")){em.persist(new CanonicalBusinessEventRecord(id(),f.f[5],"USER_ANSWER",id(),"synthetic-flush-fault",T));em.flush();}}).finalizeAnswer(f.req(),f.scope());assertEquals(Durability.NOT_COMMITTED,r.durability);assertNull(r.confirmed);assertEquals(0,decisions(i));assertEquals(0,claims(i));}
 @ParameterizedTest @CsvSource({"EXPIRED","REJECTED"})
 void negativeHistoricalWinnerReferenceIsValidated(String verdict){Fixture f=new Fixture();adapter().finalizeAnswer(f.req(),f.scope());String winner=f.identity().decisionId;owner.current(f.identity(),null,"WAITING",verdict.equals("EXPIRED")?T:T.plusSeconds(10));f.next("changed");F8Result r=adapter().finalizeAnswer(f.req(),f.scope());assertEquals(Verdict.valueOf(verdict),r.confirmed.verdict);assertEquals(winner,r.confirmed.winnerId);assertEquals(F8HistoricalReader.State.FOUND_MATCH,adapter().read(f.req(),f.scope()).state);admin.update("UPDATE f8_decision SET winner_id=? WHERE id=?",F8Identity.digest("forged","missing"),r.confirmed.decisionId);assertEquals(F8HistoricalReader.State.INCONSISTENT,adapter().read(f.req(),f.scope()).state);assertEquals(Operational.INTEGRITY_CONFLICT,adapter().finalizeAnswer(f.req(),f.scope()).operational);}
 @Test void negativeNullWinnerRemainsValidAfterLaterWinner(){Fixture f=new Fixture();F8Identity i=f.identity();SourceBindingAdapter.Request negative=f.req();owner.current(i,null,"WAITING",T);assertEquals(Verdict.EXPIRED,adapter().finalizeAnswer(negative,f.scope()).confirmed.verdict);owner.current(i,f.f[9],"WAITING",T.plusSeconds(10));f.next("later");assertEquals(Verdict.ACCEPTED,adapter().finalizeAnswer(f.req(),f.scope()).confirmed.verdict);assertEquals(F8HistoricalReader.State.FOUND_MATCH,adapter().read(negative,f.scope()).state);assertNull(adapter().read(negative,f.scope()).receipt.winnerId);}
 @Test void receiptValidatorRejectsInvalidWinnerShapes(){assertThrows(IllegalArgumentException.class,()->new Receipt("d","c","f","w",Verdict.ACCEPTED,"other"));assertThrows(IllegalArgumentException.class,()->new Receipt("d","c","f","w",Verdict.DUPLICATE,"d"));assertThrows(IllegalArgumentException.class,()->new Receipt("d","c","f","w",Verdict.EXPIRED,""));assertThrows(IllegalArgumentException.class,()->new Receipt("d","c","f","w",Verdict.REJECTED,"d"));assertNotNull(new Receipt("d","c","f","w",Verdict.EXPIRED,null));}

 @Test void duplicateCannotHideCorruptOriginalAcceptedWinnerReference(){Fixture f=new Fixture();adapter().finalizeAnswer(f.req(),f.scope());F8Identity original=f.identity();owner.applied(original);f.next("f8");F8Result duplicate=adapter().finalizeAnswer(f.req(),f.scope());assertEquals(Verdict.DUPLICATE,duplicate.confirmed.verdict);admin.update("UPDATE f8_decision SET winner_id=? WHERE id=?",F8Identity.digest("forged","missing"),original.decisionId);assertEquals(F8HistoricalReader.State.INCONSISTENT,adapter().read(f.req(),f.scope()).state);assertEquals(Operational.INTEGRITY_CONFLICT,adapter().finalizeAnswer(f.req(),f.scope()).operational);}

 // EV-F8-02: execute actual database faults inside the adapter-owned transaction.
 @Test void actualClaimCasZeroRollsBackDecisionAndSentinel(){
  Fixture f=new Fixture();F8Identity i=f.identity();AtomicInteger changed=new AtomicInteger(),claimed=new AtomicInteger();
  F8Result r=adapter(phase->{if(phase.equals("after_decision")){
   changed.set(db.update("UPDATE f8_claim SET generation=2 WHERE wait_key=? AND generation=0",i.waitKey));
   assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM f8_claim WHERE wait_key=? AND canonical_id IS NULL AND decision_id IS NULL AND generation=0",Integer.class,i.waitKey));
  }if(phase.equals("after_claim"))claimed.incrementAndGet();}).finalizeAnswer(f.req(),f.scope());
  assertEquals(1,changed.get());assertEquals(0,claimed.get());assertEquals(Operational.INTEGRITY_CONFLICT,r.operational);
  assertEquals(Attempt.ATTEMPTED,r.attempt);assertEquals(Durability.NOT_COMMITTED,r.durability);assertEquals(Reason.INTEGRITY,r.reason);assertNull(r.confirmed);
  assertEquals(0,decisions(i));assertEquals(0,claims(i));assertEquals(F8HistoricalReader.State.ABSENT,adapter().read(f.req(),f.scope()).state);
  assertEquals(Verdict.ACCEPTED,adapter().finalizeAnswer(f.req(),f.scope()).confirmed.verdict);
 }
 @Test void actualMysql1062OnClaimRollsBackOwnedAttempt(){
  Fixture f=new Fixture();F8Identity i=f.identity();AtomicInteger vendor=new AtomicInteger();String[] state={null};
  F8Result r=adapter(phase->{if(phase.equals("after_sentinel")){
   try{db.update("INSERT INTO f8_claim SELECT * FROM f8_claim WHERE wait_key=?",i.waitKey);fail("duplicate constraint must reject the second claim");}
   catch(org.springframework.dao.DataAccessException fault){
    for(Throwable cause=fault;cause!=null;cause=cause.getCause())if(cause instanceof SQLException){vendor.set(((SQLException)cause).getErrorCode());state[0]=((SQLException)cause).getSQLState();}
    throw fault;
   }
  }}).finalizeAnswer(f.req(),f.scope());
  assertEquals(1062,vendor.get());assertEquals("23000",state[0]);assertEquals(Operational.RETRYABLE_FAILURE,r.operational);
  assertEquals(Attempt.ATTEMPTED,r.attempt);assertEquals(Durability.NOT_COMMITTED,r.durability);assertEquals(Reason.DB_FAILURE,r.reason);assertNull(r.confirmed);
  assertEquals(0,decisions(i));assertEquals(0,claims(i));assertEquals(F8HistoricalReader.State.ABSENT,adapter().read(f.req(),f.scope()).state);
  assertEquals(Verdict.ACCEPTED,adapter().finalizeAnswer(f.req(),f.scope()).confirmed.verdict);
 }
 // EV-F8-03: RESUME is admitted while intact; only its independently verified target is then damaged.
 @ParameterizedTest @CsvSource({"missing-canonical,DEFER,INDETERMINATE","missing-binding,INTEGRITY_CONFLICT,INCONSISTENT","fingerprint,INTEGRITY_CONFLICT,INCONSISTENT","cipher,INTEGRITY_CONFLICT,INCONSISTENT","scope,DENIED,MISMATCH"})
 void resumeTargetDamageHasTypedNoWriteExit(String damage,Operational expected,F8HistoricalReader.State readState){
  Fixture f=new Fixture();F8Identity original=f.identity();String target=f.event;
  f.event=id();f.token=id();f.ref=id();f.f[1]="RESUME_REQUEST";f.f[16]=null;f.f[17]=target;f.persist();
  if(damage.equals("missing-canonical"))admin.execute((org.springframework.jdbc.core.ConnectionCallback<Void>)connection->{
   try(Statement statement=connection.createStatement()){
    statement.execute("SET FOREIGN_KEY_CHECKS=0");
    try(PreparedStatement delete=connection.prepareStatement("DELETE FROM canonical_business_event WHERE event_id=?")){delete.setString(1,target);assertEquals(1,delete.executeUpdate());}
    finally{statement.execute("SET FOREIGN_KEY_CHECKS=1");}
   }return null;
  });
  if(damage.equals("missing-binding"))assertEquals(1,admin.update("DELETE FROM u07_canonical_event_binding WHERE canonical_event_id=?",target));
  if(damage.equals("fingerprint"))assertEquals(1,admin.update("UPDATE u07_canonical_event_binding SET binding_fingerprint='forged' WHERE canonical_event_id=?",target));
  if(damage.equals("cipher"))assertEquals(1,admin.update("UPDATE u07_canonical_event_binding SET synthetic_answer_bytes=? WHERE canonical_event_id=?",new byte[30],target));
  if(damage.equals("scope"))assertEquals(1,admin.update("UPDATE canonical_business_event SET consultation_id=? WHERE event_id=?",id(),target));
  F8Result r=adapter().finalizeAnswer(f.req(),f.scope());assertEquals(expected,r.operational);assertEquals(Reason.EVIDENCE,r.reason);
  assertEquals(Attempt.NOT_ATTEMPTED,r.attempt);assertEquals(Durability.NOT_ATTEMPTED,r.durability);assertNull(r.confirmed);
  assertEquals(readState,adapter().read(f.req(),f.scope()).state);assertEquals(0,claims(original));assertEquals(0,decisions(original));
  assertEquals(0,admin.queryForObject("SELECT COUNT(*) FROM f8_decision WHERE canonical_id=?",Integer.class,f.event));
 }

}
