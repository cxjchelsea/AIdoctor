package com.aidoctor.verification.f8;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Disposable relational prototype only. No production authority adapter or patient data. */
class SqlClockTest {
    static final String URL="jdbc:mysql://127.0.0.1:33324/u07_f8?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    static final LocalDateTime T=LocalDateTime.of(2030,1,1,0,0);
    JdbcTemplate admin, db;
    TransactionTemplate tx, ownerTx;
    String sql;
    @BeforeEach void setup() throws Exception {
        DriverManagerDataSource ownerDs=new DriverManagerDataSource(URL,"root","synthetic-ci-only");
        admin=new JdbcTemplate(ownerDs); ownerTx=new TransactionTemplate(new DataSourceTransactionManager(ownerDs));
        ownerTx.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        DriverManagerDataSource consumer=new DriverManagerDataSource(URL,"f8_consumer","synthetic-consumer-only");
        db=new JdbcTemplate(consumer);
        tx=new TransactionTemplate(new DataSourceTransactionManager(consumer));
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        sql=org.springframework.util.StreamUtils.copyToString(getClass().getResourceAsStream("/finalize.sql"),StandardCharsets.UTF_8);
        admin.update("DELETE FROM applied_evidence"); admin.update("DELETE FROM wait_claim");
        admin.update("DELETE FROM decision"); admin.update("DELETE FROM owner_fact"); admin.update("DELETE FROM guard");
        admin.update("INSERT INTO guard (id,version) VALUES ('o',1)");
        admin.update("INSERT INTO owner_fact VALUES ('o','scope',1,'wait','wait','same',?,NULL,1,1)",Timestamp.valueOf(T.plusSeconds(10)));
    }
    void winner(boolean applied, String digest) {
        admin.update("INSERT INTO decision VALUES ('original','original-answer','scope','wait',?,'ACCEPTED',?,?,1,NULL)",digest,Timestamp.valueOf(T),Timestamp.valueOf(T));
        admin.update("INSERT INTO wait_claim VALUES ('wait','scope','original-answer','original',1)");
        if(applied) admin.update("INSERT INTO applied_evidence VALUES ('original','scope','wait',?,1,'synthetic-independent-issuer')",digest);
    }
    int insert(String clock, long version) {
        return db.update(sql.replace("CLOCK_EXPR",clock),"new","new-answer","o","scope",version,1,"new-answer");
    }
    String fixed(LocalDateTime time) { return "CAST('"+Timestamp.valueOf(time)+"' AS DATETIME(6))"; }
    void lock() { db.queryForObject("SELECT version FROM guard WHERE id='o' FOR UPDATE",Long.class); }
    String verdict() { return admin.queryForObject("SELECT verdict FROM decision WHERE id='new'",String.class); }
    int count(String table) { return admin.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class); }

    @ParameterizedTest
    @CsvSource({"P1,DUPLICATE,1","P2,EXPIRED,1","P3,REJECTED,1","P4,REJECTED,1","P5,NONE,0","P6,REJECTED,1","P7,ACCEPTED,1","P8,NONE,0"})
    void independentPolicyMatrix(String policy,String expected,int rows) {
        if(policy.equals("P1")) {winner(true,"same"); admin.update("UPDATE owner_fact SET current_wait=NULL,terminal='CANCELLED'");}
        if(policy.equals("P2")) {winner(false,"different"); admin.update("UPDATE owner_fact SET deadline=?,current_wait=NULL,terminal='CANCELLED'",Timestamp.valueOf(T));}
        if(policy.equals("P3")) admin.update("UPDATE owner_fact SET terminal='CANCELLED',current_wait=NULL");
        if(policy.equals("P4")) admin.update("UPDATE owner_fact SET current_wait='moved'");
        if(policy.equals("P5")) winner(false,"same");
        if(policy.equals("P6")) winner(true,"different");
        if(policy.equals("P8")) admin.update("UPDATE owner_fact SET delivered=0");
        tx.execute(s->{ lock(); assertEquals(rows,insert(fixed(T),1)); if(rows==1)assertEquals(expected,db.queryForObject("SELECT verdict FROM decision WHERE id='new'",String.class)); if(rows==0 || policy.equals("P7"))s.setRollbackOnly(); return null; });
        if(rows==1 && !policy.equals("P7"))assertEquals(expected,verdict());
        // Raw policy probe rolls back P7 separately in atomicity tests; it is not a finalizer implementation.
        if(!policy.equals("P7"))assertEquals(policy.equals("P1")||policy.equals("P2")||policy.equals("P5")||policy.equals("P6")?1:0,count("wait_claim"));
    }
    @Test void missingAuthorityDoesNotBecomeNegative() {
        admin.update("UPDATE owner_fact SET authority_complete=0,current_wait=NULL,terminal='CANCELLED'");
        tx.execute(s->{lock();assertEquals(0,insert(fixed(T),1));s.setRollbackOnly();return null;});
        assertEquals(0,count("decision"));
    }
    @Test void versionChangeIsZeroRows() {
        tx.execute(s->{lock(); assertEquals(0,insert(fixed(T),2)); s.setRollbackOnly();return null;});
        assertEquals(0,count("decision"));
    }
    @Test void damagedAppliedEvidenceBlocks() {
        winner(true,"same");admin.update("UPDATE applied_evidence SET generation=2");
        tx.execute(s->{lock();assertEquals(0,insert(fixed(T),1));s.setRollbackOnly();return null;});
    }
    @Test void damagedSentinelBlocks() {
        admin.update("INSERT INTO wait_claim VALUES ('wait','scope',NULL,NULL,2)");
        tx.execute(s->{lock();assertEquals(0,insert(fixed(T),1));s.setRollbackOnly();return null;});
    }
    @Test void fixedEqualityAndOneMicrosecondBefore() {
        admin.update("UPDATE owner_fact SET deadline=?",Timestamp.valueOf(T));
        tx.execute(s->{lock();assertEquals(1,insert(fixed(T.minusNanos(1000)),1));assertEquals("ACCEPTED",db.queryForObject("SELECT verdict FROM decision WHERE id='new'",String.class));s.setRollbackOnly();return null;});
        tx.execute(s->{lock();assertEquals(1,insert(fixed(T),1));return null;});assertEquals("EXPIRED",verdict());
    }
    @Test void actualClockAdvancesWithinLongTransactionAndIsStatementStable() {
        admin.update("UPDATE owner_fact SET deadline=UTC_TIMESTAMP(6)+INTERVAL 200000 MICROSECOND");
        tx.execute(s->{lock(); Timestamp start=db.queryForObject("SELECT UTC_TIMESTAMP(6)",Timestamp.class);
            db.queryForObject("SELECT SLEEP(0.35)",Integer.class);
            assertEquals(1,insert("UTC_TIMESTAMP(6)",1));
            Map<String,Object> row=db.queryForMap("SELECT sampled_at,predicate_at FROM decision WHERE id='new'");
            assertEquals(row.get("sampled_at"),row.get("predicate_at"));assertTrue(((Timestamp)row.get("sampled_at")).after(start));
            return null;}); assertEquals("EXPIRED",verdict());
        System.out.println("CLOCK_ENV="+admin.queryForMap("SELECT VERSION() version,@@session.time_zone session_zone,@@global.time_zone global_zone,UTC_TIMESTAMP(6) clock"));
    }
    @Test void transactionConnectionIsSharedAndIndependentConnectionIsRejected() {
        tx.execute(s->{lock();long bound=db.queryForObject("SELECT CONNECTION_ID()",Long.class);
            assertEquals(bound,db.queryForObject("SELECT CONNECTION_ID()",Long.class).longValue());
            assertFalse(db.queryForObject("SELECT @@autocommit",Boolean.class));
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            JdbcTemplate foreign=new JdbcTemplate(new DriverManagerDataSource(URL,"f8_consumer","synthetic-consumer-only"));
            assertThrows(IllegalStateException.class,()->requireSameConnection(bound,foreign));return null;});
    }
    void requireSameConnection(long expected,JdbcTemplate candidate) {
        if(candidate.queryForObject("SELECT CONNECTION_ID()",Long.class)!=expected)throw new IllegalStateException("RESOURCE_MISMATCH");
    }
    @Test void legacyEmptySentinelCanBecomeCompleteWinner() {
        admin.update("INSERT INTO wait_claim VALUES ('wait','scope',NULL,NULL,0)");
        assertEquals("FINALIZED/ATTEMPTED/COMMITTED",finalizeAtomic(fixed(T),false,false,false));assertEquals("ACCEPTED",verdict());
    }
    @Test void pendingWinnerReadOnlyExitDoesNotAttemptDml() {
        winner(false,"same");tx.execute(s->{lock();assertEquals("ACCEPTED",db.queryForObject("SELECT verdict FROM decision WHERE id='original'",String.class));return null;});
        assertEquals(1,count("decision"));assertEquals(1,count("wait_claim"));
        // Wrapper contract: DEFER/NOT_ATTEMPTED/NOT_ATTEMPTED. Raw forced finalizer P5 is tested separately.
    }
    static class RollbackProbe extends RuntimeException { private static final long serialVersionUID=1L; }
    String finalizeAtomic(String clock,boolean sentinel,boolean faultAfterDecision,boolean faultAfterClaim) {
        AtomicBoolean attempted=new AtomicBoolean(false);
        try { tx.execute(s->{lock();
            if(sentinel) {attempted.set(true);db.update("INSERT INTO wait_claim VALUES ('wait','scope',NULL,NULL,0)");}
            attempted.set(true);if(insert(clock,1)!=1)throw new RollbackProbe();
            String v=db.queryForObject("SELECT verdict FROM decision WHERE id='new'",String.class);
            if(faultAfterDecision || sentinel&&!v.equals("ACCEPTED"))throw new RollbackProbe();
            if(v.equals("ACCEPTED")) {
                if(db.update("UPDATE wait_claim SET answer_id='new-answer',decision_id='new',generation=1 WHERE wait_id='wait' AND scope_id='scope' AND answer_id IS NULL AND decision_id IS NULL AND generation=0")!=1)throw new RollbackProbe();
            }
            if(faultAfterClaim)throw new RollbackProbe();return null;});return "FINALIZED/ATTEMPTED/COMMITTED";
        } catch(RollbackProbe e) {assertTrue(attempted.get()); return "RETRYABLE_FAILURE/ATTEMPTED/NOT_COMMITTED";}
        // Database commit/rollback exceptions deliberately propagate; never report confirmed rollback for them.
    }
    @Test void acceptedDecisionAndClaimCommitTogether() {
        assertEquals("FINALIZED/ATTEMPTED/COMMITTED",finalizeAtomic(fixed(T),true,false,false));
        assertEquals("ACCEPTED",verdict());assertEquals(1,count("wait_claim"));
        assertEquals(1L,admin.queryForObject("SELECT generation FROM wait_claim",Long.class));
        assertEquals("new",admin.queryForObject("SELECT decision_id FROM wait_claim",String.class));
    }
    @ParameterizedTest @CsvSource({"true,false","false,true"})
    void faultRollsBackWholeTransaction(boolean afterDecision,boolean afterClaim) {
        assertEquals("RETRYABLE_FAILURE/ATTEMPTED/NOT_COMMITTED",finalizeAtomic(fixed(T),true,afterDecision,afterClaim));
        assertEquals(0,count("decision"));assertEquals(0,count("wait_claim"));
    }
    @Test void zeroRowsRollBackSentinel() {
        admin.update("UPDATE owner_fact SET authority_complete=0");
        assertEquals("RETRYABLE_FAILURE/ATTEMPTED/NOT_COMMITTED",finalizeAtomic(fixed(T),true,false,false));
        assertEquals(0,count("wait_claim"));assertEquals(0,count("decision"));
    }
    @Test void deadlineCrossingRollsBackSentinelThenSameIdentityExpires() {
        admin.update("UPDATE owner_fact SET deadline=UTC_TIMESTAMP(6)+INTERVAL 200000 MICROSECOND");
        assertThrows(RollbackProbe.class,()->tx.execute(s->{lock();db.update("INSERT INTO wait_claim VALUES ('wait','scope',NULL,NULL,0)");db.queryForObject("SELECT SLEEP(0.35)",Integer.class);assertEquals(1,insert("UTC_TIMESTAMP(6)",1));assertEquals("EXPIRED",db.queryForObject("SELECT verdict FROM decision WHERE id='new'",String.class));throw new RollbackProbe();}));
        assertEquals(0,count("decision"));assertEquals(0,count("wait_claim"));
        assertEquals("FINALIZED/ATTEMPTED/COMMITTED",finalizeAtomic("UTC_TIMESTAMP(6)",false,false,false));assertEquals("EXPIRED",verdict());assertEquals(0,count("wait_claim"));
    }
    @Test void consumerCannotForgeOwnerOrAppliedEvidence() {
        assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE guard SET version=2"));
        assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE owner_fact SET terminal='CANCELLED'"));
        assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("INSERT INTO applied_evidence VALUES ('x','scope','wait','same',1,'synthetic-independent-issuer')"));
        assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("DELETE FROM decision"));
    }
    @ParameterizedTest @CsvSource({"true","false"})
    void commonGuardSerializesBothOrders(boolean ownerFirst) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch locked=new CountDownLatch(1),release=new CountDownLatch(1),started=new CountDownLatch(1),acquired=new CountDownLatch(1);
        try {
            Future<?> first=pool.submit(()->(ownerFirst?ownerTx:tx).execute(s->{
                if(ownerFirst)admin.queryForObject("SELECT version FROM guard WHERE id='o' FOR UPDATE",Long.class);else lock();
                locked.countDown();try {assertTrue(release.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}
                if(ownerFirst)adminOwnerUpdate();else {assertEquals(1,insert(fixed(T),1));s.setRollbackOnly();}return null;}));
            assertTrue(locked.await(5,TimeUnit.SECONDS));
            Future<?> second=pool.submit(()->(ownerFirst?tx:ownerTx).execute(s->{started.countDown();
                if(ownerFirst)lock();else admin.queryForObject("SELECT version FROM guard WHERE id='o' FOR UPDATE",Long.class);
                acquired.countDown();if(ownerFirst){assertEquals(1,insert(fixed(T),2));assertEquals("REJECTED",db.queryForObject("SELECT verdict FROM decision WHERE id='new'",String.class));}else adminOwnerUpdate();return null;}));
            assertTrue(started.await(5,TimeUnit.SECONDS));assertFalse(acquired.await(200,TimeUnit.MILLISECONDS));release.countDown();first.get(5,TimeUnit.SECONDS);second.get(5,TimeUnit.SECONDS);
        } finally {release.countDown();pool.shutdownNow();}
    }
    void adminOwnerUpdate() {
        admin.update("UPDATE owner_fact SET terminal='CANCELLED',current_wait=NULL,version=2 WHERE id='o'");
    }
}
