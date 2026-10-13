package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.forensics;

import com.aidoctor.diagnosis.runtime.u06.delivery.BoundedU06WholeTxRetry;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06UnknownCommitReadbackFence;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06UnknownCommitReadbackFence.Evidence;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06UnknownCommitReadbackFence.Status;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class StageBUnknownCommitReadbackFenceTest {
    private static final String[] TABLES = {
        "u06_delivery_authority","u06_delivery_intent","u06_delivery_attempt",
        "u06_delivery_receipt","u06_delivery_confirmation","u06_delivery_ledger"
    };
    private U06DeliveryStore.Command command() {
        String k=UUID.randomUUID().toString();
        return new U06DeliveryStore.Command("s-"+k,"e-"+k,"d-"+k,"q-"+k,
            "f-"+k,"synthetic-endpoint","i-"+k,"eval-"+k,"fp-"+k,
            "2026-10-13T08:00:00Z");
    }
    private U06UnknownCommitReadbackFence fence(StageBFTestConfig env) {
        return new U06UnknownCommitReadbackFence(new JdbcTemplate(env.writer),env.tx);
    }
    private void counts(StageBFTestConfig env,U06DeliveryStore.Command c,int expected) {
        JdbcTemplate j=new JdbcTemplate(env.writer);
        for(String table:TABLES) {
            Integer n=j.queryForObject("SELECT COUNT(*) FROM "+table+
                " WHERE delivery_effect_id=?",new Object[]{c.deliveryEffectId},Integer.class);
            Assertions.assertEquals(expected,n.intValue(),table);
        }
    }
    private void corrupt(U06DeliveryStore.Command c,String sql)throws Exception {
        Assertions.assertEquals("true",System.getenv("GITHUB_ACTIONS"));
        Assertions.assertEquals("true",System.getenv("U07_F_ISOLATED"));
        String secret=System.getenv("U07_REPLAY_SYNTHETIC_ADMIN_PWD");
        Assertions.assertNotNull(secret);
        try(Connection db=DriverManager.getConnection(StageBFTestConfig.URL,"root",secret);
            PreparedStatement p=db.prepareStatement(sql)) {
            p.setString(1,c.deliveryEffectId);
            Assertions.assertEquals(1,p.executeUpdate());
        }
    }
    @Test public void absentNeverAuthorizesDispatchOrInserts() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            Evidence e=fence(env).inspect(c);
            Assertions.assertEquals(Status.ABSENT_NO_DISPATCH,e.status);
            Assertions.assertFalse(e.externalDispatchAllowed);
            counts(env,c,0);
        }
    }
    @Test public void completeOriginalSixTableReadbackStillDeniesDispatch() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            new BoundedU06WholeTxRetry(new JdbcTemplate(env.writer),env.tx).reconcileConfirmed(c);
            Evidence e=fence(env).inspect(c);
            Assertions.assertEquals(Status.COMPLETE_NO_DISPATCH,e.status);
            Assertions.assertFalse(e.externalDispatchAllowed);
            counts(env,c,1);
        }
    }
    @Test public void missingReceiptIsIndeterminateAndCannotResend()throws Exception {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            new BoundedU06WholeTxRetry(new JdbcTemplate(env.writer),env.tx).reconcileConfirmed(c);
            corrupt(c,"DELETE FROM u06_delivery_receipt WHERE delivery_effect_id=?");
            Evidence e=fence(env).inspect(c);
            Assertions.assertEquals(Status.INDETERMINATE_NO_DISPATCH,e.status);
            Assertions.assertFalse(e.externalDispatchAllowed);
        }
    }
    @Test public void changedConfirmationIsIndeterminateAndCannotResend()throws Exception {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            U06DeliveryStore.Command c=command();
            new BoundedU06WholeTxRetry(new JdbcTemplate(env.writer),env.tx).reconcileConfirmed(c);
            corrupt(c,"UPDATE u06_delivery_confirmation SET confirmation_status='CORRUPTED' WHERE delivery_effect_id=?");
            Evidence e=fence(env).inspect(c);
            Assertions.assertEquals(Status.INDETERMINATE_NO_DISPATCH,e.status);
            Assertions.assertFalse(e.externalDispatchAllowed);
            counts(env,c,1);
        }
    }
    @Test public void abortedThreadCannotInspect() {
        try(StageBFTestConfig env=new StageBFTestConfig()) {
            Thread.currentThread().interrupt();
            try {
                Assertions.assertThrows(IllegalStateException.class,()->fence(env).inspect(command()));
            } finally {
                Thread.interrupted();
            }
        }
    }
}
