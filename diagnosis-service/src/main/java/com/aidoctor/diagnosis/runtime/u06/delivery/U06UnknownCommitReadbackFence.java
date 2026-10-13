package com.aidoctor.diagnosis.runtime.u06.delivery;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Read-only, durable six-table reconciliation after an uncertain commit.
 * This is NOT a delivery/send permission and intentionally has NO tool or
 * notification interface. A separate, reviewed authority must decide any
 * future external side effect.
 */
public final class U06UnknownCommitReadbackFence {
    public enum Status {
        COMPLETE_NO_DISPATCH, ABSENT_NO_DISPATCH, INDETERMINATE_NO_DISPATCH
    }

    public static final class Evidence {
        public final Status status;
        public final boolean externalDispatchAllowed;
        public final int[] rowCounts;
        private Evidence(Status status, int[] counts) {
            this.status = status;
            this.rowCounts = counts.clone();
            this.externalDispatchAllowed = false;
        }
    }

    private static final String[] TABLES = {
        "u06_delivery_authority", "u06_delivery_intent", "u06_delivery_attempt",
        "u06_delivery_receipt", "u06_delivery_confirmation", "u06_delivery_ledger"
    };
    private final JdbcTemplate jdbc;
    private final DataSourceTransactionManager manager;

    public U06UnknownCommitReadbackFence(JdbcTemplate jdbc,
                                         DataSourceTransactionManager manager) {
        if (jdbc == null || manager == null || jdbc.getDataSource() == null)
            throw new IllegalArgumentException("U06_RECONCILE_DATASOURCE_REQUIRED");
        DataSource ds = jdbc.getDataSource();
        if (ds != manager.getDataSource())
            throw new IllegalArgumentException("U06_RECONCILE_DATASOURCE_MISMATCH");
        this.jdbc = jdbc;
        this.manager = manager;
    }

    public Evidence inspect(U06DeliveryStore.Command command) {
        if (command == null) throw new IllegalArgumentException("U06_RECONCILE_COMMAND_REQUIRED");
        if (Thread.currentThread().isInterrupted())
            throw new IllegalStateException("U06_RECONCILE_CANCELLED");
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("U06_RECONCILE_AMBIENT_TX_FORBIDDEN");
        TransactionTemplate tx = new TransactionTemplate(manager);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setReadOnly(true);
        tx.setTimeout(8);
        return tx.execute(status -> inspectWithinSnapshot(command));
    }

    private Evidence inspectWithinSnapshot(U06DeliveryStore.Command c) {
        int[] counts = new int[TABLES.length];
        int sum = 0;
        for (int i = 0; i < TABLES.length; i++) {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + TABLES[i] + " WHERE delivery_effect_id=?",
                new Object[]{c.deliveryEffectId}, Integer.class);
            if (count == null || count < 0 || count > 1)
                return new Evidence(Status.INDETERMINATE_NO_DISPATCH, counts);
            counts[i] = count;
            sum += count;
        }
        if (sum == 0) return new Evidence(Status.ABSENT_NO_DISPATCH, counts);
        if (sum != TABLES.length)
            return new Evidence(Status.INDETERMINATE_NO_DISPATCH, counts);
        // No write or resend is possible from this class. Field-level validation
        // is independent of the original Store's mutate-or-replay entry point.
        if (!matches("u06_delivery_authority", c, "selection_effect_id",
                c.selectionEffectId, "delivery_id", c.deliveryId,
                "authority_status", "CONFIRMED_TERMINAL"))
            return new Evidence(Status.INDETERMINATE_NO_DISPATCH, counts);
        if (!matches("u06_delivery_intent", c, "delivery_id", c.deliveryId,
                "question_id", c.questionId, "content_fingerprint", c.contentFingerprint,
                "endpoint_ref", c.endpointRef, "idempotency_key", c.idempotencyKey,
                "intent_status", "READY"))
            return new Evidence(Status.INDETERMINATE_NO_DISPATCH, counts);
        if (!matches("u06_delivery_attempt", c, "attempt_id",
                c.deliveryEffectId + ":attempt:1", "delivery_id", c.deliveryId,
                "attempt_status", "SYNTHETIC_ACCEPTED"))
            return new Evidence(Status.INDETERMINATE_NO_DISPATCH, counts);
        if (!matches("u06_delivery_receipt", c, "receipt_id",
                c.deliveryEffectId + ":receipt:1", "delivery_id", c.deliveryId,
                "receipt_status", "SYNTHETIC_CONFIRMED", "evidence_type", "SYNTHETIC"))
            return new Evidence(Status.INDETERMINATE_NO_DISPATCH, counts);
        if (!matches("u06_delivery_confirmation", c, "confirmation_evaluation_id",
                c.confirmationEvaluationId, "delivery_id", c.deliveryId,
                "confirmation_status", "CONFIRMED",
                "confirmation_fingerprint", c.confirmationFingerprint))
            return new Evidence(Status.INDETERMINATE_NO_DISPATCH, counts);
        if (!matches("u06_delivery_ledger", c, "delivery_id", c.deliveryId,
                "confirmation_status", "CONFIRMED",
                "canonical_fingerprint", c.confirmationFingerprint))
            return new Evidence(Status.INDETERMINATE_NO_DISPATCH, counts);
        return new Evidence(Status.COMPLETE_NO_DISPATCH, counts);
    }

    private boolean matches(String table, U06DeliveryStore.Command c, String... pairs) {
        // Only fixed, audited column identifiers from the caller are interpolated.
        StringBuilder selection = new StringBuilder("SELECT ");
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) selection.append(',');
            selection.append(pairs[i]);
        }
        selection.append(" FROM ").append(table).append(" WHERE delivery_effect_id=?");
        List<Map<String,Object>> rows = jdbc.queryForList(selection.toString(), c.deliveryEffectId);
        if (rows.size() != 1) return false;
        Map<String,Object> found = rows.get(0);
        for (int i = 0; i < pairs.length; i += 2) {
            Object value = found.get(pairs[i]);
            if (value == null || !pairs[i + 1].equals(value.toString())) return false;
        }
        return true;
    }
}
