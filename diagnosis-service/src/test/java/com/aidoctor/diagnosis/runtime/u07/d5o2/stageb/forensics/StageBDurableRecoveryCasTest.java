package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.forensics;

import com.aidoctor.diagnosis.runtime.u06.delivery.BoundedU06WholeTxRetry;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DurableRecoveryAuthority;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DurableRecoveryAuthority.Claim;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

public class StageBDurableRecoveryCasTest {
    private U06DeliveryStore.Command command() {
        String k=UUID.randomUUID().toString();
        return new U06DeliveryStore.Command("s-"+k,"e-"+k,"d-"+k,"q-"+k,
            "f-"+k,"synthetic-endpoint","i-"+k,"eval-"+k,"fp-"+k,
            "2026-10-13T08:00:00Z");
    }
    private U06DurableRecoveryAuthority authority(StageBFTestConfig env) {
        return new U06DurableRecoveryAuthority(new JdbcTemplate(env.writer),env.tx);
    }
    private void fixtureUpdate(String effect, String sql)throws Exception {
        Assertions.assertEquals("true",System.getenv("GITHUB_ACTIONS"));
        Assertions.assertEquals("true",System.getenv("U07_F_ISOLATED"));
        String secret=System.getenv("U07_REPLAY_SYNTHETIC_ADMIN_PWD");
        Assertions.assertNotNull(secret);
        try(Connection c=DriverManager.getConnection(StageBFTestConfig.URL,"root",secret);
            PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,effect);
            Assertions.assertEquals(1,p.executeUpdate());
        }
    }
    @Test public void absentResultIsDurableButNoDispatch() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            U06DurableRecoveryAuthority a=authority(env);
            a.register(c.deliveryEffectId,c.confirmationFingerprint);
            Claim token=a.acquire(c.deliveryEffectId,c.confirmationFingerprint,20);
            Assertions.assertNotNull(token);
            Assertions.assertFalse(token.externalDispatchAllowed);
            Assertions.assertEquals("OBSERVED_ABSENT_NO_DISPATCH",a.observeAndFinalize(token,c));
            Assertions.assertNull(a.acquire(c.deliveryEffectId,c.confirmationFingerprint,20));
            Assertions.assertFalse(a.renew(token,10));
        }
    }
    @Test public void completedOriginalSixRowsRemainEvidenceOnly() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            new BoundedU06WholeTxRetry(new JdbcTemplate(env.writer),env.tx)
                .reconcileConfirmed(c);
            U06DurableRecoveryAuthority a=authority(env);
            a.register(c.deliveryEffectId,c.confirmationFingerprint);
            Claim token=a.acquire(c.deliveryEffectId,c.confirmationFingerprint,20);
            Assertions.assertEquals("OBSERVED_COMPLETE_NO_DISPATCH",a.observeAndFinalize(token,c));
            Assertions.assertNull(a.acquire(c.deliveryEffectId,c.confirmationFingerprint,20));
        }
    }
    @Test public void missingReceiptIsDurablyBlocked() throws Exception {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            new BoundedU06WholeTxRetry(new JdbcTemplate(env.writer),env.tx)
                .reconcileConfirmed(c);
            fixtureUpdate(c.deliveryEffectId,
                "DELETE FROM u06_delivery_receipt WHERE delivery_effect_id=?");
            U06DurableRecoveryAuthority a=authority(env);
            a.register(c.deliveryEffectId,c.confirmationFingerprint);
            Claim token=a.acquire(c.deliveryEffectId,c.confirmationFingerprint,20);
            Assertions.assertEquals("OBSERVED_INDETERMINATE_NO_DISPATCH",
                a.observeAndFinalize(token,c));
        }
    }
    @Test public void sameEffectCannotOverwriteRegistrationOrStealActiveLease() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            U06DurableRecoveryAuthority a=authority(env);
            a.register(c.deliveryEffectId,c.confirmationFingerprint);
            Assertions.assertThrows(DuplicateKeyException.class,
                () -> a.register(c.deliveryEffectId,"different-fingerprint"));
            Claim first=a.acquire(c.deliveryEffectId,c.confirmationFingerprint,20);
            Assertions.assertNull(authority(env).acquire(
                c.deliveryEffectId,c.confirmationFingerprint,20));
            Assertions.assertNull(authority(env).acquire(
                c.deliveryEffectId,"different-fingerprint",20));
            Assertions.assertTrue(a.renew(first,10));
        }
    }
    @Test public void expiredOwnerCannotRenewOrFinalizeAfterGenerationChange()throws Exception {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            U06DurableRecoveryAuthority a=authority(env);
            a.register(c.deliveryEffectId,c.confirmationFingerprint);
            Claim stale=a.acquire(c.deliveryEffectId,c.confirmationFingerprint,20);
            fixtureUpdate(c.deliveryEffectId,
                "UPDATE u07_d5o2_recovery_claim SET lease_until=DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 1 SECOND) WHERE effect_id=?");
            Claim current=authority(env).acquire(
                c.deliveryEffectId,c.confirmationFingerprint,20);
            Assertions.assertNotNull(current);
            Assertions.assertEquals(stale.generation+1,current.generation);
            Assertions.assertFalse(a.renew(stale,10));
            IllegalStateException x=Assertions.assertThrows(IllegalStateException.class,
                () -> a.observeAndFinalize(stale,c));
            Assertions.assertEquals("U07_RECOVERY_OWNER_FENCED",x.getMessage());
            Assertions.assertEquals("OBSERVED_ABSENT_NO_DISPATCH",
                authority(env).observeAndFinalize(current,c));
        }
    }
    @Test public void concurrentClaimHasExactlyOneWinner() throws Exception {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            authority(env).register(c.deliveryEffectId,c.confirmationFingerprint);
            ExecutorService workers=Executors.newFixedThreadPool(2);
            CountDownLatch start=new CountDownLatch(1);
            try {
                Future<Claim> a=workers.submit(()->{
                    start.await();
                    return authority(env).acquire(c.deliveryEffectId,c.confirmationFingerprint,20);
                });
                Future<Claim> b=workers.submit(()->{
                    start.await();
                    return authority(env).acquire(c.deliveryEffectId,c.confirmationFingerprint,20);
                });
                start.countDown();
                Claim first=a.get(),second=b.get();
                Assertions.assertTrue((first==null) != (second==null),
                    "RECOVERY_CAS_MORE_THAN_ONE_OWNER");
            } finally {
                workers.shutdownNow();
                Assertions.assertTrue(workers.awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS));
            }
        }
    }
}
