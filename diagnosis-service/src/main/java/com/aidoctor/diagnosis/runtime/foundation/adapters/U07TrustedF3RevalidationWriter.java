package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Non-production, immutable F3 revalidation evidence writer. The caller must
 * provide a separately authorized, independently verified owner/barrier/
 * revalidation decision: a U06 Mode-3 readback is never an authorization.
 *
 * DDL and trusted issuer deployment are external prerequisites. No production
 * migration or authority provider is installed by this component.
 */
public final class U07TrustedF3RevalidationWriter {
    public interface IndependentAuthority {
        boolean verify(Command command);
    }

    public static final class Command {
        public final String consultationId, cdpId, effectId, ownerCommitRef;
        public final String barrierRef, revalidationRef, routeSourceRef;
        public final String routeAuthorizationRef, routingPolicyVersion;
        public final int version;

        public Command(String consultationId, String cdpId, int version,
                String effectId, String ownerCommitRef, String barrierRef,
                String revalidationRef, String routeSourceRef,
                String routeAuthorizationRef, String routingPolicyVersion) {
            this.consultationId = require(consultationId);
            this.cdpId = require(cdpId);
            this.version = version;
            this.effectId = require(effectId);
            this.ownerCommitRef = require(ownerCommitRef);
            this.barrierRef = require(barrierRef);
            this.revalidationRef = require(revalidationRef);
            this.routeSourceRef = require(routeSourceRef);
            this.routeAuthorizationRef = require(routeAuthorizationRef);
            this.routingPolicyVersion = require(routingPolicyVersion);
            if (version < 0) throw new IllegalArgumentException("U07_REVALIDATION_VERSION_INVALID");
        }
    }

    private final CDPRepository repository;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final IndependentAuthority authority;

    public U07TrustedF3RevalidationWriter(CDPRepository repository,
            JdbcTemplate jdbc, TransactionTemplate tx, IndependentAuthority authority) {
        if (repository == null || jdbc == null || tx == null || authority == null)
            throw new IllegalArgumentException("U07_REVALIDATION_WRITER_DEPENDENCIES_REQUIRED");
        this.repository = repository;
        this.jdbc = jdbc;
        this.tx = tx;
        this.authority = authority;
    }

    /** Exact replay succeeds without rewriting; conflicting replay fails closed. */
    public String persist(Command command) {
        if (command == null) throw new IllegalArgumentException("U07_REVALIDATION_COMMAND_REQUIRED");
        return tx.execute(status -> {
            CDP cdp = repository.findByIdWithLock(command.cdpId)
                    .orElseThrow(() -> new IllegalStateException("U07_REVALIDATION_CDP_MISSING"));
            if (cdp.getVersion() == null || cdp.getVersion().intValue() != command.version)
                throw new IllegalStateException("U07_REVALIDATION_STALE_VERSION");
            Map<String, Object> state = cdp.getPatientState();
            Object f3 = state == null ? null : state.get("f3_gap_assessment");
            if (!(f3 instanceof Map<?, ?>) || !command.effectId.equals(
                    ((Map<?, ?>) f3).get("f3_canonical_effect_id")))
                throw new IllegalStateException("U07_REVALIDATION_F3_EFFECT_MISMATCH");

            List<String[]> owners = jdbc.query(
                    "SELECT owner_commit_ref, state FROM u07_f3_owner_evidence "
                    + "WHERE consultation_id=? AND cdp_id=? AND cdp_version=? AND f3_effect_id=?",
                    (rs, n) -> new String[]{rs.getString(1), rs.getString(2)},
                    command.consultationId, command.cdpId, command.version, command.effectId);
            if (owners.size() != 1 || !command.ownerCommitRef.equals(owners.get(0)[0])
                    || !"COMMITTED".equals(owners.get(0)[1]))
                throw new IllegalStateException("U07_REVALIDATION_OWNER_NOT_VERIFIED");

            List<String[]> barriers = jdbc.query(
                    "SELECT barrier_ref, outcome FROM u07_f3_barrier_evidence "
                    + "WHERE consultation_id=? AND cdp_id=? AND cdp_version=? "
                    + "AND f3_effect_id=? AND owner_commit_ref=?",
                    (rs, n) -> new String[]{rs.getString(1), rs.getString(2)},
                    command.consultationId, command.cdpId, command.version,
                    command.effectId, command.ownerCommitRef);
            if (barriers.size() != 1 || !command.barrierRef.equals(barriers.get(0)[0])
                    || !"ALLOWED".equals(barriers.get(0)[1]))
                throw new IllegalStateException("U07_REVALIDATION_BARRIER_NOT_ALLOWED");
            if (!authority.verify(command))
                throw new IllegalStateException("U07_REVALIDATION_INDEPENDENT_AUTHORITY_DENIED");

            List<String[]> existing = jdbc.query(
                    "SELECT f3_effect_id, owner_commit_ref, barrier_ref, revalidation_ref, "
                    + "outcome, route_source_ref, route_authorization_ref, routing_policy_version "
                    + "FROM u07_f3_revalidation_evidence WHERE consultation_id=? AND cdp_id=? "
                    + "AND cdp_version=?",
                    (rs, n) -> new String[]{rs.getString(1), rs.getString(2),
                            rs.getString(3), rs.getString(4), rs.getString(5),
                            rs.getString(6), rs.getString(7), rs.getString(8)},
                    command.consultationId, command.cdpId, command.version);
            if (!existing.isEmpty()) {
                if (existing.size() != 1 || !same(existing.get(0), command))
                    throw new IllegalStateException("U07_REVALIDATION_DURABLE_REPLAY_CONFLICT");
                return command.revalidationRef;
            }
            jdbc.update("INSERT INTO u07_f3_revalidation_evidence "
                    + "(consultation_id, cdp_id, cdp_version, f3_effect_id, owner_commit_ref, "
                    + "barrier_ref, revalidation_ref, outcome, route_source_ref, "
                    + "route_authorization_ref, routing_policy_version) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    command.consultationId, command.cdpId, command.version, command.effectId,
                    command.ownerCommitRef, command.barrierRef, command.revalidationRef,
                    "REVALIDATED_CURRENT", command.routeSourceRef,
                    command.routeAuthorizationRef, command.routingPolicyVersion);
            return command.revalidationRef;
        });
    }

    private static boolean same(String[] row, Command c) {
        return c.effectId.equals(row[0]) && c.ownerCommitRef.equals(row[1])
                && c.barrierRef.equals(row[2]) && c.revalidationRef.equals(row[3])
                && "REVALIDATED_CURRENT".equals(row[4]) && c.routeSourceRef.equals(row[5])
                && c.routeAuthorizationRef.equals(row[6])
                && c.routingPolicyVersion.equals(row[7]);
    }

    private static String require(String field) {
        if (field == null || field.trim().isEmpty())
            throw new IllegalArgumentException("U07_REVALIDATION_EVIDENCE_FIELD_REQUIRED");
        return field;
    }
}
