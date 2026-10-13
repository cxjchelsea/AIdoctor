package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.forensics;

import com.aidoctor.diagnosis.runtime.u06.delivery.JdbcU06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Disposable MySQL8 only. No patient data, external delivery or retry. */
public class StageBSixTableReplayTest {
    private static final String[] TABLES = {
        "u06_delivery_authority", "u06_delivery_intent", "u06_delivery_attempt",
        "u06_delivery_receipt", "u06_delivery_confirmation", "u06_delivery_ledger"
    };

    private U06DeliveryStore.Command command() {
        String id = UUID.randomUUID().toString();
        return new U06DeliveryStore.Command("s-"+id, "e-"+id, "d-"+id, "q-"+id,
                "f-"+id, "synthetic-only", "idem-"+id, "eval-"+id,
                "confirm-"+id, "2026-10-13T08:00:00Z");
    }

    private U06DeliveryStore.Snapshot write(StageBFTestConfig env, U06DeliveryStore.Command c) {
        TransactionTemplate tx = new TransactionTemplate(env.tx);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx.execute(status ->
                new JdbcU06DeliveryStore(new JdbcTemplate(env.writer)).reconcileConfirmed(c));
    }

    private void counts(StageBFTestConfig env, U06DeliveryStore.Command c, int... expected) {
        JdbcTemplate j = new JdbcTemplate(env.writer);
        for (int i = 0; i < TABLES.length; i++) {
            Integer n = j.queryForObject(
                    "SELECT COUNT(*) FROM " + TABLES[i] + " WHERE delivery_effect_id=?",
                    new Object[]{c.deliveryEffectId}, Integer.class);
            Assertions.assertEquals(expected[i], n.intValue(), TABLES[i]);
        }
    }

    private void admin(U06DeliveryStore.Command c, String sql) throws Exception {
        String secret = System.getenv("U07_REPLAY_SYNTHETIC_ADMIN_PWD");
        Assertions.assertNotNull(secret, "ADMIN_FIXTURE_SECRET_MISSING");
        Assertions.assertEquals("true", System.getenv("GITHUB_ACTIONS"));
        Assertions.assertEquals("true", System.getenv("U07_F_ISOLATED"));
        try (Connection db = DriverManager.getConnection(StageBFTestConfig.URL, "root", secret);
             PreparedStatement stmt = db.prepareStatement(sql)) {
            stmt.setString(1, c.deliveryEffectId);
            Assertions.assertEquals(1, stmt.executeUpdate(), "FIXTURE_MUTATION_ROW_COUNT");
        }
    }

    private void incomplete(StageBFTestConfig env, U06DeliveryStore.Command c) {
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class,
                () -> write(env, c));
        Assertions.assertEquals("U06_DELIVERY_REPLAY_INCOMPLETE", ex.getMessage());
    }

    private void conflict(StageBFTestConfig env, U06DeliveryStore.Command c) {
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class,
                () -> write(env, c));
        Assertions.assertEquals("U06_DELIVERY_REPLAY_CONFLICT", ex.getMessage());
    }

    @Test
    public void completeSixRowsReplayExactlyOnce() throws Exception {
        try (StageBFTestConfig env = new StageBFTestConfig()) {
            U06DeliveryStore.Command c = command();
            Assertions.assertFalse(write(env, c).replay);
            counts(env, c, 1,1,1,1,1,1);
            Assertions.assertTrue(write(env, c).replay);
            counts(env, c, 1,1,1,1,1,1);
        }
    }

    @Test
    public void missingAttemptFailsClosedRatherThanReplay() throws Exception {
        try (StageBFTestConfig env = new StageBFTestConfig()) {
            U06DeliveryStore.Command c = command();
            Assertions.assertFalse(write(env, c).replay);
            admin(c, "DELETE FROM u06_delivery_attempt WHERE delivery_effect_id=?");
            incomplete(env, c);
            counts(env, c, 1,1,0,1,1,1);
        }
    }

    @Test
    public void missingReceiptFailsClosedRatherThanReplay() throws Exception {
        try (StageBFTestConfig env = new StageBFTestConfig()) {
            U06DeliveryStore.Command c = command();
            write(env, c);
            admin(c, "DELETE FROM u06_delivery_receipt WHERE delivery_effect_id=?");
            incomplete(env, c);
            counts(env, c, 1,1,1,0,1,1);
        }
    }

    @Test
    public void missingLedgerFailsClosedRatherThanNewInsert() throws Exception {
        try (StageBFTestConfig env = new StageBFTestConfig()) {
            U06DeliveryStore.Command c = command();
            write(env, c);
            admin(c, "DELETE FROM u06_delivery_ledger WHERE delivery_effect_id=?");
            incomplete(env, c);
            counts(env, c, 1,1,1,1,1,0);
        }
    }

    @Test
    public void changedAttemptStatusFailsClosed() throws Exception {
        try (StageBFTestConfig env = new StageBFTestConfig()) {
            U06DeliveryStore.Command c = command();
            write(env, c);
            admin(c, "UPDATE u06_delivery_attempt SET attempt_status='CORRUPTED' WHERE delivery_effect_id=?");
            conflict(env, c);
            counts(env, c, 1,1,1,1,1,1);
        }
    }

    @Test
    public void changedReceiptStatusFailsClosed() throws Exception {
        try (StageBFTestConfig env = new StageBFTestConfig()) {
            U06DeliveryStore.Command c = command();
            write(env, c);
            admin(c, "UPDATE u06_delivery_receipt SET receipt_status='CORRUPTED' WHERE delivery_effect_id=?");
            conflict(env, c);
            counts(env, c, 1,1,1,1,1,1);
        }
    }
}
