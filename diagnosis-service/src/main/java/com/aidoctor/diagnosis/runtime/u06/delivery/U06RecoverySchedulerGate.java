package com.aidoctor.diagnosis.runtime.u06.delivery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Explicit, non-production-only durable recovery batch pump.
 * Resolves previously registered durable commands, never sends externally.
 * No @Scheduled annotation, startup bean, or production dispatch endpoint.
 */
public final class U06RecoverySchedulerGate {
    private static final int MAX_BATCH = 8;
    private final JdbcTemplate jdbc;
    private final U06DurableRecoveryAuthority authority;

    public U06RecoverySchedulerGate(JdbcTemplate jdbc, DataSourceTransactionManager manager) {
        if (!"true".equals(System.getenv("GITHUB_ACTIONS"))
                || !"true".equals(System.getenv("U07_F_ISOLATED"))
                || !"SYNTHETIC_ONLY".equals(System.getenv("U07_RECOVERY_SCHEDULER_MODE")))
            throw new IllegalStateException("U07_RECOVERY_SCHEDULER_NOT_AUTHORIZED");
        if (jdbc == null || manager == null || jdbc.getDataSource() == null)
            throw new IllegalArgumentException("U07_SCHEDULER_DATASOURCE_REQUIRED");
        DataSource ds = jdbc.getDataSource();
        if (ds != manager.getDataSource())
            throw new IllegalArgumentException("U07_SCHEDULER_DATASOURCE_MISMATCH");
        this.jdbc = jdbc;
        this.authority = new U06DurableRecoveryAuthority(jdbc, manager);
    }

    public static final class Result {
        public final int examined;
        public final int observed;
        public final boolean externalDispatchAllowed = false;
        private Result(int examined, int observed) {
            this.examined = examined;
            this.observed = observed;
        }
    }

    public Result tick(Map<String,U06DeliveryStore.Command> authorizedSyntheticCommands) {
        if (Thread.currentThread().isInterrupted())
            throw new IllegalStateException("U07_SCHEDULER_CANCELLED");
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("U07_SCHEDULER_AMBIENT_TX_FORBIDDEN");
        if (authorizedSyntheticCommands == null)
            throw new IllegalArgumentException("U07_SCHEDULER_COMMANDS_REQUIRED");

        // Database-clock eligibility; permanently observed states never replay.
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT effect_id,command_fingerprint FROM u07_d5o2_recovery_claim " +
            "WHERE claim_status='PENDING' OR " +
            "(claim_status='CLAIMED' AND lease_until<CURRENT_TIMESTAMP(6)) " +
            "ORDER BY updated_at,effect_id LIMIT " + MAX_BATCH);
        int observed = 0;
        for (Map<String,Object> row : rows) {
            if (Thread.currentThread().isInterrupted())
                throw new IllegalStateException("U07_SCHEDULER_CANCELLED");
            String effect = String.valueOf(row.get("effect_id"));
            String fingerprint = String.valueOf(row.get("command_fingerprint"));
            U06DeliveryStore.Command command = authorizedSyntheticCommands.get(effect);
            // Missing or changed command facts are a hard no-claim, no-send boundary.
            if (command == null
                    || !effect.equals(command.deliveryEffectId)
                    || !fingerprint.equals(command.confirmationFingerprint))
                continue;
            U06DurableRecoveryAuthority.Claim claim =
                authority.acquire(effect, fingerprint, 20);
            if (claim == null) continue;
            if (claim.externalDispatchAllowed)
                throw new IllegalStateException("U07_SCHEDULER_DISPATCH_FORBIDDEN");
            String result = authority.observeAndFinalize(claim, command);
            if (!result.endsWith("_NO_DISPATCH"))
                throw new IllegalStateException("U07_SCHEDULER_DISPATCH_FORBIDDEN");
            observed++;
        }
        return new Result(rows.size(), observed);
    }
}
