package com.aidoctor.diagnosis.runtime.u06.delivery;

import javax.sql.DataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Explicit synthetic-only recovery intake. Atomically records a pending
 * recovery claim and a DENIED outbox fence; does NOT execute deliveries.
 * This must not be treated as lossless unknown-commit capture if the process
 * crashes before intake. No production caller or sender is wired.
 */
public final class U06RecoveryIntakeOutboxFence {
    private final JdbcTemplate jdbc;
    private final DataSourceTransactionManager manager;

    public U06RecoveryIntakeOutboxFence(JdbcTemplate jdbc, DataSourceTransactionManager manager) {
        if (!"true".equals(System.getenv("GITHUB_ACTIONS"))
                || !"true".equals(System.getenv("U07_F_ISOLATED"))
                || !"SYNTHETIC_ONLY".equals(System.getenv("U07_RECOVERY_SCHEDULER_MODE")))
            throw new IllegalStateException("U07_INTAKE_PRODUCTION_FORBIDDEN");
        if (jdbc == null || manager == null || jdbc.getDataSource() == null)
            throw new IllegalArgumentException("U07_INTAKE_DATASOURCE_REQUIRED");
        DataSource source = jdbc.getDataSource();
        if (manager.getDataSource() != source)
            throw new IllegalArgumentException("U07_INTAKE_DATASOURCE_MISMATCH");
        this.jdbc = jdbc;
        this.manager = manager;
    }

    public static final class Intake {
        public final String effectId;
        public final String outcome;
        public final boolean externalDispatchAllowed = false;
        private Intake(String effectId) {
            this.effectId = effectId;
            this.outcome = "PENDING_WITH_OUTBOX_DENIED";
        }
    }

    public Intake registerUncertainSynthetic(U06DeliveryStore.Command command) {
        if (command == null) throw new IllegalArgumentException("U07_INTAKE_COMMAND_REQUIRED");
        if (Thread.currentThread().isInterrupted())
            throw new IllegalStateException("U07_INTAKE_CANCELLED");
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("U07_INTAKE_AMBIENT_TX_FORBIDDEN");
        if (command.deliveryEffectId.length() > 128 || command.confirmationFingerprint.length() > 128)
            throw new IllegalArgumentException("U07_INTAKE_IDENTITY_TOO_LONG");
        TransactionTemplate tx = new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        tx.setTimeout(8);
        return tx.execute(status -> {
            // Do not swallow duplicates: malicious or accidental re-intake
            // must never reset generation, claim, or fence state.
            jdbc.update("INSERT INTO u07_d5o2_recovery_claim " +
                    "(effect_id,command_fingerprint,claim_status,generation,owner_token,lease_until,updated_at) " +
                    "VALUES (?,?,'PENDING',0,NULL,NULL,CURRENT_TIMESTAMP(6))",
                    command.deliveryEffectId, command.confirmationFingerprint);
            jdbc.update("INSERT INTO u07_d5o2_outbox_fence " +
                    "(effect_id,command_fingerprint,fence_status,fence_generation,created_at) " +
                    "VALUES (?,?,'DENIED_NO_DISPATCH',0,CURRENT_TIMESTAMP(6))",
                    command.deliveryEffectId, command.confirmationFingerprint);
            return new Intake(command.deliveryEffectId);
        });
    }

    /** Evidence only. Must never be converted into a send permission. */
    public boolean canDispatch(String effectId) {
        if (effectId == null || effectId.trim().isEmpty())
            throw new IllegalArgumentException("U07_OUTBOX_EFFECT_REQUIRED");
        return false;
    }

    /** Strict verification after intake: any missing or altered fence blocks. */
    public String verifyDenied(String effectId, String fingerprint) {
        if (effectId == null || fingerprint == null)
            throw new IllegalArgumentException("U07_OUTBOX_IDENTITY_REQUIRED");
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM u07_d5o2_outbox_fence " +
            "WHERE effect_id=? AND command_fingerprint=? AND " +
            "fence_status='DENIED_NO_DISPATCH' AND fence_generation=0",
            new Object[]{effectId,fingerprint},Integer.class);
        if (count == null || count != 1)
            throw new IllegalStateException("U07_OUTBOX_FENCE_NOT_PROVEN");
        return "DENIED_NO_DISPATCH";
    }
}
