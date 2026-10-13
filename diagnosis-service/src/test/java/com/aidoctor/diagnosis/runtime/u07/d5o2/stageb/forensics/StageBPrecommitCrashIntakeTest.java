package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.forensics;

import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06PrecommitRecoveryGate;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06RecoverySchedulerGate;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class StageBPrecommitCrashIntakeTest {
    private U06DeliveryStore.Command command() {
        String id=UUID.randomUUID().toString();
        return new U06DeliveryStore.Command("s-"+id,"e-"+id,"d-"+id,
            "q-"+id,"content-"+id,"synthetic-endpoint","idem-"+id,
            "eval-"+id,"confirm-"+id,"2026-10-13T08:00:00Z");
    }
    private U06PrecommitRecoveryGate gate(StageBFTestConfig env) {
        return new U06PrecommitRecoveryGate(new JdbcTemplate(env.writer),env.tx);
    }
    private int rows(StageBFTestConfig env,String table,String effect,String field) {
        return new JdbcTemplate(env.writer).queryForObject(
            "SELECT COUNT(*) FROM "+table+" WHERE "+field+"=?",
            new Object[]{effect},Integer.class);
    }
    @Test public void persistedBeforeCrashReclaimedByNewServiceInstanceWithoutSending() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command command=command();
            // A simulated crash after pre-commit durable preparation: the
            // original gate is discarded without performing a six-table write.
            gate(env).prepare(command);
            Assertions.assertEquals(1,rows(env,"u07_d5o2_recovery_claim",
                command.deliveryEffectId,"effect_id"));
            Assertions.assertEquals(1,rows(env,"u07_d5o2_outbox_fence",
                command.deliveryEffectId,"effect_id"));
            Assertions.assertEquals(0,rows(env,"u06_delivery_ledger",
                command.deliveryEffectId,"delivery_effect_id"));
            U06RecoverySchedulerGate scheduler=new U06RecoverySchedulerGate(
                new JdbcTemplate(env.writer),env.tx);
            U06RecoverySchedulerGate.Result result=scheduler.tick(
                Collections.singletonMap(command.deliveryEffectId,command));
            Assertions.assertEquals(1,result.observed);
            Assertions.assertFalse(result.externalDispatchAllowed);
            Assertions.assertEquals("OBSERVED_ABSENT_NO_DISPATCH",
                new JdbcTemplate(env.writer).queryForObject(
                    "SELECT claim_status FROM u07_d5o2_recovery_claim WHERE effect_id=?",
                    new Object[]{command.deliveryEffectId},String.class));
            Assertions.assertFalse(gate(env).externalDispatchAllowed());
        }
    }
    @Test public void preparedCommitKeepsClaimAndDenyFenceUntilReconciliation() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command command=command();
            gate(env).prepare(command);
            Assertions.assertFalse(gate(env).executePrepared(command).replay);
            Assertions.assertEquals(1,rows(env,"u06_delivery_ledger",
                command.deliveryEffectId,"delivery_effect_id"));
            Assertions.assertEquals(1,rows(env,"u07_d5o2_outbox_fence",
                command.deliveryEffectId,"effect_id"));
            U06RecoverySchedulerGate.Result result=new U06RecoverySchedulerGate(
                new JdbcTemplate(env.writer),env.tx).tick(
                    Collections.singletonMap(command.deliveryEffectId,command));
            Assertions.assertEquals(1,result.observed);
            Assertions.assertFalse(result.externalDispatchAllowed);
        }
    }
    @Test public void missingPrecommitFenceCannotStartStoreWrite() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command command=command();
            Assertions.assertThrows(IllegalStateException.class,
                ()->gate(env).executePrepared(command));
            Assertions.assertEquals(0,rows(env,"u06_delivery_ledger",
                command.deliveryEffectId,"delivery_effect_id"));
        }
    }
}
