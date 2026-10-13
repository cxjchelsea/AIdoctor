package com.aidoctor.diagnosis.runtime.u06.delivery;

import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Durable, database-CAS recovery claim and observation authority.
 * No method grants network/tool/notification dispatch. Requires a separately
 * provisioned u07_d5o2_recovery_claim table; not enabled by default.
 */
public final class U06DurableRecoveryAuthority {
    private static final int MAX_LEASE_SECONDS = 30;
    private final JdbcTemplate jdbc;
    private final DataSourceTransactionManager manager;
    private final U06UnknownCommitReadbackFence readback;

    public U06DurableRecoveryAuthority(JdbcTemplate jdbc,
                                        DataSourceTransactionManager manager) {
        if (jdbc == null || manager == null || jdbc.getDataSource() == null)
            throw new IllegalArgumentException("U07_RECOVERY_DATASOURCE_REQUIRED");
        DataSource source = jdbc.getDataSource();
        if (source != manager.getDataSource())
            throw new IllegalArgumentException("U07_RECOVERY_DATASOURCE_MISMATCH");
        this.jdbc = jdbc;
        this.manager = manager;
        this.readback = new U06UnknownCommitReadbackFence(jdbc, manager);
    }

    public static final class Claim {
        public final String effectId;
        public final String ownerToken;
        public final long generation;
        public final boolean externalDispatchAllowed = false;
        private Claim(String effect, String token, long number) {
            effectId = effect; ownerToken = token; generation = number;
        }
    }

    private TransactionTemplate transaction() {
        TransactionTemplate tx = new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.setTimeout(8);
        return tx;
    }

    private static void ensureOutermost() {
        if (Thread.currentThread().isInterrupted())
            throw new IllegalStateException("U07_RECOVERY_CANCELLED");
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("U07_RECOVERY_AMBIENT_TX_FORBIDDEN");
    }

    public void register(String effectId, String commandFingerprint) {
        ensureOutermost();
        required(effectId); required(commandFingerprint);
        transaction().execute(status -> {
            // New effects only. Duplicate identity is deliberately not ignored:
            // a caller cannot silently replace or reset an existing claim.
            jdbc.update("INSERT INTO u07_d5o2_recovery_claim " +
                "(effect_id,command_fingerprint,claim_status,generation,owner_token,lease_until,updated_at) " +
                "VALUES (?,?,'PENDING',0,NULL,NULL,CURRENT_TIMESTAMP(6))",
                effectId, commandFingerprint);
            return null;
        });
    }

    public Claim acquire(String effectId, String commandFingerprint, int leaseSeconds) {
        ensureOutermost();
        required(effectId); required(commandFingerprint);
        if (leaseSeconds < 1 || leaseSeconds > MAX_LEASE_SECONDS)
            throw new IllegalArgumentException("U07_RECOVERY_LEASE_BOUNDS");
        final String token = UUID.randomUUID().toString();
        return transaction().execute(status -> {
            int changed = jdbc.update(
                "UPDATE u07_d5o2_recovery_claim SET " +
                "claim_status='CLAIMED',generation=generation+1,owner_token=?," +
                "lease_until=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL ? SECOND)," +
                "updated_at=CURRENT_TIMESTAMP(6) " +
                "WHERE effect_id=? AND command_fingerprint=? AND " +
                "(claim_status='PENDING' OR " +
                "(claim_status='CLAIMED' AND lease_until < CURRENT_TIMESTAMP(6)))",
                token, leaseSeconds, effectId, commandFingerprint);
            if (changed != 1) return null;
            Long generation = jdbc.queryForObject(
                "SELECT generation FROM u07_d5o2_recovery_claim " +
                "WHERE effect_id=? AND owner_token=? AND claim_status='CLAIMED'",
                new Object[]{effectId, token}, Long.class);
            if (generation == null || generation < 1)
                throw new IllegalStateException("U07_RECOVERY_CLAIM_INCONSISTENT");
            return new Claim(effectId, token, generation);
        });
    }

    public boolean renew(Claim claim, int leaseSeconds) {
        ensureOutermost();
        if (claim == null) throw new IllegalArgumentException("U07_RECOVERY_CLAIM_REQUIRED");
        if (leaseSeconds < 1 || leaseSeconds > MAX_LEASE_SECONDS)
            throw new IllegalArgumentException("U07_RECOVERY_LEASE_BOUNDS");
        return transaction().execute(status -> jdbc.update(
            "UPDATE u07_d5o2_recovery_claim SET " +
            "lease_until=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL ? SECOND)," +
            "updated_at=CURRENT_TIMESTAMP(6) " +
            "WHERE effect_id=? AND generation=? AND owner_token=? " +
            "AND claim_status='CLAIMED' AND lease_until>CURRENT_TIMESTAMP(6)",
            leaseSeconds,claim.effectId,claim.generation,claim.ownerToken) == 1);
    }

    public String observeAndFinalize(Claim claim, U06DeliveryStore.Command command) {
        ensureOutermost();
        if (claim == null || command == null ||
            !claim.effectId.equals(command.deliveryEffectId))
            throw new IllegalArgumentException("U07_RECOVERY_CLAIM_COMMAND_MISMATCH");

        // First verify this owner still holds a live fenced claim.
        // Readback occurs in a separate read-only database transaction. Its
        // result is evidence-only, NEVER authority to dispatch.
        if (!isCurrentOwner(claim))
            throw new IllegalStateException("U07_RECOVERY_OWNER_FENCED");
        U06UnknownCommitReadbackFence.Evidence evidence = readback.inspect(command);
        if (evidence.externalDispatchAllowed)
            throw new IllegalStateException("U07_RECOVERY_DISPATCH_AUTHORITY_FORBIDDEN");
        final String outcome = "OBSERVED_" + evidence.status.name();
        boolean committed = transaction().execute(status -> jdbc.update(
            "UPDATE u07_d5o2_recovery_claim SET claim_status=?," +
            "owner_token=NULL,lease_until=NULL,updated_at=CURRENT_TIMESTAMP(6) " +
            "WHERE effect_id=? AND generation=? AND owner_token=? " +
            "AND claim_status='CLAIMED' AND lease_until>CURRENT_TIMESTAMP(6)",
            outcome,claim.effectId,claim.generation,claim.ownerToken) == 1);
        if (!committed) throw new IllegalStateException("U07_RECOVERY_OWNER_FENCED");
        return outcome;
    }

    private boolean isCurrentOwner(Claim claim) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM u07_d5o2_recovery_claim " +
            "WHERE effect_id=? AND generation=? AND owner_token=? " +
            "AND claim_status='CLAIMED' AND lease_until>CURRENT_TIMESTAMP(6)",
            new Object[]{claim.effectId,claim.generation,claim.ownerToken},Integer.class);
        return count != null && count == 1;
    }

    private static void required(String value) {
        if (value == null || value.trim().isEmpty() || value.length() > 128)
            throw new IllegalArgumentException("U07_RECOVERY_IDENTITY_REQUIRED");
    }
}
