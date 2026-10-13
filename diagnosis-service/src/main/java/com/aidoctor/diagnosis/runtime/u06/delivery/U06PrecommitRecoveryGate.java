package com.aidoctor.diagnosis.runtime.u06.delivery;

import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * Synthetic-only pre-commit recovery fence. Persist recovery intent + a denied
 * outbox BEFORE attempting the six-table transaction. No message dispatch.
 * This is an opt-in boundary, not a default production U06 implementation.
 */
public final class U06PrecommitRecoveryGate {
    private final U06RecoveryIntakeOutboxFence intake;
    private final BoundedU06WholeTxRetry retry;

    public U06PrecommitRecoveryGate(JdbcTemplate jdbc, DataSourceTransactionManager manager) {
        if (jdbc == null || manager == null || jdbc.getDataSource() == null)
            throw new IllegalArgumentException("U07_PRECOMMIT_DATASOURCE_REQUIRED");
        DataSource source=jdbc.getDataSource();
        if (source != manager.getDataSource())
            throw new IllegalArgumentException("U07_PRECOMMIT_DATASOURCE_MISMATCH");
        intake=new U06RecoveryIntakeOutboxFence(jdbc,manager);
        retry=new BoundedU06WholeTxRetry(jdbc,manager);
    }

    /** Durable entry barrier, committed in a standalone transaction before any store attempt. */
    public void prepare(U06DeliveryStore.Command command) {
        intake.registerUncertainSynthetic(command);
        intake.verifyDenied(command.deliveryEffectId,command.confirmationFingerprint);
    }

    /** Only a command already fenced by this entry point may be attempted. */
    public U06DeliveryStore.Snapshot executePrepared(U06DeliveryStore.Command command) {
        if (command == null) throw new IllegalArgumentException("U07_PRECOMMIT_COMMAND_REQUIRED");
        intake.verifyDenied(command.deliveryEffectId,command.confirmationFingerprint);
        return retry.reconcileConfirmed(command);
    }

    /** No preparation and store write can be mistaken for one database transaction. */
    public U06DeliveryStore.Snapshot prepareAndExecute(U06DeliveryStore.Command command) {
        prepare(command);
        return executePrepared(command);
    }

    public boolean externalDispatchAllowed() { return false; }
}
