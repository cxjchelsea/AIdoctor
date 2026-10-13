package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.forensics;

import com.aidoctor.diagnosis.runtime.u06.delivery.BoundedU06WholeTxRetry;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DurableRecoveryAuthority;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06RecoverySchedulerGate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class StageBRecoverySchedulerGateTest {
    private U06DeliveryStore.Command command() {
        String k=UUID.randomUUID().toString();
        return new U06DeliveryStore.Command("s-"+k,"e-"+k,"d-"+k,"q-"+k,
            "f-"+k,"synthetic-endpoint","i-"+k,"eval-"+k,"fp-"+k,
            "2026-10-13T08:00:00Z");
    }
    private U06RecoverySchedulerGate gate(StageBFTestConfig env) {
        return new U06RecoverySchedulerGate(new JdbcTemplate(env.writer),env.tx);
    }
    private U06DurableRecoveryAuthority authority(StageBFTestConfig env) {
        return new U06DurableRecoveryAuthority(new JdbcTemplate(env.writer),env.tx);
    }
    private void expire(String effect)throws Exception {
        Assertions.assertEquals("true",System.getenv("GITHUB_ACTIONS"));
        Assertions.assertEquals("true",System.getenv("U07_F_ISOLATED"));
        try(Connection c=DriverManager.getConnection(StageBFTestConfig.URL,"root",
                System.getenv("U07_REPLAY_SYNTHETIC_ADMIN_PWD"));
            PreparedStatement p=c.prepareStatement(
                "UPDATE u07_d5o2_recovery_claim SET lease_until=DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 1 SECOND) WHERE effect_id=?")) {
            p.setString(1,effect);
            Assertions.assertEquals(1,p.executeUpdate());
        }
    }
    @Test public void pendingRecoveryObservedOnceNeverDispatches() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            authority(env).register(c.deliveryEffectId,c.confirmationFingerprint);
            Map<String,U06DeliveryStore.Command> m=new HashMap<>();
            m.put(c.deliveryEffectId,c);
            U06RecoverySchedulerGate.Result result=gate(env).tick(m);
            Assertions.assertEquals(1,result.observed);
            Assertions.assertFalse(result.externalDispatchAllowed);
            Assertions.assertEquals(0,gate(env).tick(m).observed);
        }
    }
    @Test public void completedDeliveryOnlyBecomesObservedEvidence() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            new BoundedU06WholeTxRetry(new JdbcTemplate(env.writer),env.tx)
                .reconcileConfirmed(c);
            authority(env).register(c.deliveryEffectId,c.confirmationFingerprint);
            Map<String,U06DeliveryStore.Command> m=new HashMap<>();
            m.put(c.deliveryEffectId,c);
            Assertions.assertEquals(1,gate(env).tick(m).observed);
            String status=new JdbcTemplate(env.writer).queryForObject(
                "SELECT claim_status FROM u07_d5o2_recovery_claim WHERE effect_id=?",
                new Object[]{c.deliveryEffectId},String.class);
            Assertions.assertEquals("OBSERVED_COMPLETE_NO_DISPATCH",status);
        }
    }
    @Test public void mismatchedFactsNeverClaimOrDispatch() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            authority(env).register(c.deliveryEffectId,c.confirmationFingerprint);
            Map<String,U06DeliveryStore.Command> m=new HashMap<>();
            m.put(c.deliveryEffectId,new U06DeliveryStore.Command(c.selectionEffectId,
                c.deliveryEffectId,c.deliveryId,c.questionId,c.contentFingerprint,
                c.endpointRef,c.idempotencyKey,c.confirmationEvaluationId,
                "WRONG-FINGERPRINT",c.createdAt));
            U06RecoverySchedulerGate.Result r=gate(env).tick(m);
            Assertions.assertEquals(0,r.observed);
            Assertions.assertFalse(r.externalDispatchAllowed);
            Assertions.assertEquals("PENDING",new JdbcTemplate(env.writer).queryForObject(
                "SELECT claim_status FROM u07_d5o2_recovery_claim WHERE effect_id=?",
                new Object[]{c.deliveryEffectId},String.class));
        }
    }
    @Test public void staleOwnerIsSupersededAndCannotFinalize()throws Exception {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            U06DurableRecoveryAuthority a=authority(env);
            a.register(c.deliveryEffectId,c.confirmationFingerprint);
            U06DurableRecoveryAuthority.Claim stale=a.acquire(
                c.deliveryEffectId,c.confirmationFingerprint,20);
            expire(c.deliveryEffectId);
            Map<String,U06DeliveryStore.Command> m=new HashMap<>();
            m.put(c.deliveryEffectId,c);
            Assertions.assertEquals(1,gate(env).tick(m).observed);
            Assertions.assertFalse(a.renew(stale,10));
            Assertions.assertThrows(IllegalStateException.class,
                ()->a.observeAndFinalize(stale,c));
        }
    }
}
