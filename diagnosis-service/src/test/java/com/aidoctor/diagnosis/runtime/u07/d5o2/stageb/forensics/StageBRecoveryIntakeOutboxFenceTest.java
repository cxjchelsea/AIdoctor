package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.forensics;

import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06RecoveryIntakeOutboxFence;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

public class StageBRecoveryIntakeOutboxFenceTest {
    private U06DeliveryStore.Command command() {
        String id=UUID.randomUUID().toString();
        return new U06DeliveryStore.Command("s-"+id,"e-"+id,"d-"+id,"q-"+id,
            "f-"+id,"synthetic-endpoint","i-"+id,"eval-"+id,"fp-"+id,
            "2026-10-13T08:00:00Z");
    }
    private U06RecoveryIntakeOutboxFence intake(StageBFTestConfig env) {
        return new U06RecoveryIntakeOutboxFence(new JdbcTemplate(env.writer),env.tx);
    }
    private int count(StageBFTestConfig env,String table,String effect) {
        return new JdbcTemplate(env.writer).queryForObject(
            "SELECT COUNT(*) FROM "+table+" WHERE effect_id=?",
            new Object[]{effect},Integer.class);
    }
    private void admin(String effect,String sql)throws Exception {
        Assertions.assertEquals("true",System.getenv("GITHUB_ACTIONS"));
        Assertions.assertEquals("true",System.getenv("U07_F_ISOLATED"));
        try(Connection connection=DriverManager.getConnection(StageBFTestConfig.URL,"root",
                System.getenv("U07_REPLAY_SYNTHETIC_ADMIN_PWD"));
            PreparedStatement p=connection.prepareStatement(sql)) {
            p.setString(1,effect);
            Assertions.assertEquals(1,p.executeUpdate());
        }
    }
    @Test public void atomicIntakeCreatesClaimAndDeniedOutbox() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            U06RecoveryIntakeOutboxFence f=intake(env);
            Assertions.assertEquals("PENDING_WITH_OUTBOX_DENIED",
                f.registerUncertainSynthetic(c).outcome);
            Assertions.assertEquals(1,count(env,"u07_d5o2_recovery_claim",c.deliveryEffectId));
            Assertions.assertEquals(1,count(env,"u07_d5o2_outbox_fence",c.deliveryEffectId));
            Assertions.assertEquals("DENIED_NO_DISPATCH",
                f.verifyDenied(c.deliveryEffectId,c.confirmationFingerprint));
            Assertions.assertFalse(f.canDispatch(c.deliveryEffectId));
        }
    }
    @Test public void duplicateCannotResetClaimOrFence() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            U06RecoveryIntakeOutboxFence f=intake(env);
            f.registerUncertainSynthetic(c);
            Assertions.assertThrows(DuplicateKeyException.class,
                ()->f.registerUncertainSynthetic(c));
            Assertions.assertEquals(1,count(env,"u07_d5o2_recovery_claim",c.deliveryEffectId));
            Assertions.assertEquals(1,count(env,"u07_d5o2_outbox_fence",c.deliveryEffectId));
        }
    }
    @Test public void failedSecondInsertRollsBackFirst() throws Exception {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            // Force an outbox duplicate while recovery claim is absent.
            try(Connection db=DriverManager.getConnection(StageBFTestConfig.URL,"root",
                    System.getenv("U07_REPLAY_SYNTHETIC_ADMIN_PWD"));
                PreparedStatement p=db.prepareStatement(
                    "INSERT INTO u07_d5o2_outbox_fence " +
                    "(effect_id,command_fingerprint,fence_status,fence_generation,created_at) " +
                    "VALUES (?,?,'DENIED_NO_DISPATCH',0,CURRENT_TIMESTAMP(6))")) {
                p.setString(1,c.deliveryEffectId);
                p.setString(2,c.confirmationFingerprint);
                Assertions.assertEquals(1,p.executeUpdate());
            }
            Assertions.assertThrows(DuplicateKeyException.class,
                ()->intake(env).registerUncertainSynthetic(c));
            Assertions.assertEquals(0,count(env,"u07_d5o2_recovery_claim",c.deliveryEffectId));
            Assertions.assertEquals(1,count(env,"u07_d5o2_outbox_fence",c.deliveryEffectId));
        }
    }
    @Test public void corruptedFenceDoesNotAuthorizeDispatch() throws Exception {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            U06RecoveryIntakeOutboxFence f=intake(env);
            f.registerUncertainSynthetic(c);
            admin(c.deliveryEffectId,
                "UPDATE u07_d5o2_outbox_fence SET fence_status='CORRUPTED' WHERE effect_id=?");
            Assertions.assertThrows(IllegalStateException.class,
                ()->f.verifyDenied(c.deliveryEffectId,c.confirmationFingerprint));
            Assertions.assertFalse(f.canDispatch(c.deliveryEffectId));
        }
    }
    @Test public void preCancelledIntakeWritesNothing() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            Thread.currentThread().interrupt();
            try {
                Assertions.assertThrows(IllegalStateException.class,
                    ()->intake(env).registerUncertainSynthetic(c));
            } finally {
                Thread.interrupted();
            }
            Assertions.assertEquals(0,count(env,"u07_d5o2_recovery_claim",c.deliveryEffectId));
            Assertions.assertEquals(0,count(env,"u07_d5o2_outbox_fence",c.deliveryEffectId));
        }
    }
}
