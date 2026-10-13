package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.forensics;

import com.aidoctor.diagnosis.runtime.u06.delivery.BoundedU06WholeTxRetry;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Real disposable MySQL8 persistence, synthetic failure only inside the transaction. */
public class StageBBoundedWholeTxRetryTest {
    private static final String[] TABLES = {
        "u06_delivery_authority", "u06_delivery_intent", "u06_delivery_attempt",
        "u06_delivery_receipt", "u06_delivery_confirmation", "u06_delivery_ledger"
    };
    private U06DeliveryStore.Command command() {
        String k = UUID.randomUUID().toString();
        return new U06DeliveryStore.Command("s-"+k,"e-"+k,"d-"+k,"q-"+k,
            "fingerprint-"+k,"synthetic-only","idem-"+k,"eval-"+k,
            "confirmation-"+k,"2026-10-13T08:00:00Z");
    }
    private void assertSix(StageBFTestConfig env, U06DeliveryStore.Command c, int count) {
        JdbcTemplate j = new JdbcTemplate(env.writer);
        for (String table : TABLES) {
            Integer found = j.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE delivery_effect_id=?",
                new Object[]{c.deliveryEffectId}, Integer.class);
            Assertions.assertEquals(count,found.intValue(),table);
        }
    }
    private JdbcTemplate faultJdbc(StageBFTestConfig env, AtomicInteger injected,
                                   int allowedFailures, int vendorCode, String state) {
        return new JdbcTemplate(env.writer) {
            @Override
            public int update(String sql, Object... args) {
                // The authority and intent inserts have already run when failure
                // is injected on the third write; they must rollback together.
                if (sql.startsWith("INSERT INTO u06_delivery_attempt") &&
                        injected.getAndIncrement() < allowedFailures)
                    throw new UncategorizedSQLException("SYNTHETIC_TX_ONLY", sql,
                        new SQLException("SYNTHETIC_DEADLOCK",state,vendorCode));
                return super.update(sql,args);
            }
        };
    }
    @Test
    public void deadlock1213RollsBackFullAttemptThenCommitsOnce() {
        try (StageBFTestConfig env = new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            AtomicInteger attempts=new AtomicInteger();
            BoundedU06WholeTxRetry retry=new BoundedU06WholeTxRetry(
                faultJdbc(env,attempts,1,1213,"40001"),env.tx);
            Assertions.assertFalse(retry.reconcileConfirmed(c).replay);
            Assertions.assertEquals(2,attempts.get());
            assertSix(env,c,1);
            Assertions.assertTrue(new BoundedU06WholeTxRetry(new JdbcTemplate(env.writer),env.tx)
                .reconcileConfirmed(c).replay);
            assertSix(env,c,1);
        }
    }
    @Test
    public void repeatedDeadlockExhaustsWithSixTableRollback() {
        try (StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            AtomicInteger attempts=new AtomicInteger();
            BoundedU06WholeTxRetry retry=new BoundedU06WholeTxRetry(
                faultJdbc(env,attempts,2,1213,"40001"),env.tx);
            IllegalStateException e=Assertions.assertThrows(IllegalStateException.class,
                ()->retry.reconcileConfirmed(c));
            Assertions.assertEquals("U06_RETRY_EXHAUSTED",e.getMessage());
            Assertions.assertEquals(2,attempts.get());
            assertSix(env,c,0);
        }
    }
    @Test
    public void timeout1205NeverRetriesAndRollsBack() {
        try (StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            AtomicInteger attempts=new AtomicInteger();
            BoundedU06WholeTxRetry retry=new BoundedU06WholeTxRetry(
                faultJdbc(env,attempts,2,1205,"HY000"),env.tx);
            Assertions.assertThrows(UncategorizedSQLException.class,()->retry.reconcileConfirmed(c));
            Assertions.assertEquals(1,attempts.get());
            assertSix(env,c,0);
        }
    }
    @Test
    public void state40001WithoutVendor1213NeverRetries() {
        try (StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            AtomicInteger attempts=new AtomicInteger();
            BoundedU06WholeTxRetry retry=new BoundedU06WholeTxRetry(
                faultJdbc(env,attempts,2,9999,"40001"),env.tx);
            Assertions.assertThrows(UncategorizedSQLException.class,()->retry.reconcileConfirmed(c));
            Assertions.assertEquals(1,attempts.get());
            assertSix(env,c,0);
        }
    }
    @Test
    public void interruptedThreadFailsBeforeTransaction() {
        try (StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            Thread.currentThread().interrupt();
            try {
                IllegalStateException e=Assertions.assertThrows(IllegalStateException.class,
                    ()->new BoundedU06WholeTxRetry(new JdbcTemplate(env.writer),env.tx)
                        .reconcileConfirmed(c));
                Assertions.assertEquals("U06_RETRY_CANCELLED",e.getMessage());
            } finally {
                Thread.interrupted();
            }
            assertSix(env,c,0);
        }
    }
    @Test
    public void ambientTransactionIsRejectedBeforeAnyWrite() {
        try (StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            TransactionTemplate outer=new TransactionTemplate(env.tx);
            outer.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            outer.execute(status->{
                IllegalStateException e=Assertions.assertThrows(IllegalStateException.class,
                    ()->new BoundedU06WholeTxRetry(new JdbcTemplate(env.writer),env.tx)
                        .reconcileConfirmed(c));
                Assertions.assertEquals("U06_RETRY_AMBIENT_TRANSACTION_FORBIDDEN",e.getMessage());
                return null;
            });
            assertSix(env,c,0);
        }
    }
    @Test
    public void normalFirstWriteAndReplayDoNotDuplicate() {
        try (StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            BoundedU06WholeTxRetry retry=new BoundedU06WholeTxRetry(
                new JdbcTemplate(env.writer),env.tx);
            Assertions.assertFalse(retry.reconcileConfirmed(c).replay);
            Assertions.assertTrue(retry.reconcileConfirmed(c).replay);
            assertSix(env,c,1);
        }
    }
}
