package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u05.U04A1U05AuthorityBridge;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Non-production durable F3/A1 receipt writer and independent readback source.
 *
 * <p>Never derives post-F3 safety approval from CDP content. An independently
 * adjudicated, trusted host owner must supply barrier/revalidation/permission
 * evidence. Neither this component nor its table enables external dispatch.
 * The table DDL remains test-only: no production migration is authorized.
 */
public final class U07DurableNonProductionA1Issuer
        implements ClinicalCdpF3BoundA1AuthorityPort.IndependentlyAttestedA1Source {

    public interface VerifiedPostF3EvidencePort {
        VerifiedEvidence evaluate(String consultationId, String cdpId, int currentVersion,
                                  String persistedF3Effect);
    }

    public static final class VerifiedEvidence {
        public final String f3Effect, barrierRef, revalidationRef, routeSourceRef;
        public final String routeAuthorizationRef, routingPolicyVersion;
        public final boolean barrierAllowed, revalidatedCurrent, routeCurrent;

        public VerifiedEvidence(String f3Effect, String barrierRef, String revalidationRef,
                String routeSourceRef, String routeAuthorizationRef, String routingPolicyVersion,
                boolean barrierAllowed, boolean revalidatedCurrent, boolean routeCurrent) {
            this.f3Effect = required(f3Effect);
            this.barrierRef = required(barrierRef);
            this.revalidationRef = required(revalidationRef);
            this.routeSourceRef = required(routeSourceRef);
            this.routeAuthorizationRef = required(routeAuthorizationRef);
            this.routingPolicyVersion = required(routingPolicyVersion);
            this.barrierAllowed = barrierAllowed;
            this.revalidatedCurrent = revalidatedCurrent;
            this.routeCurrent = routeCurrent;
        }
    }

    private final CDPRepository repository;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final VerifiedPostF3EvidencePort verifier;

    public U07DurableNonProductionA1Issuer(CDPRepository repository, JdbcTemplate jdbc,
            TransactionTemplate transactions, VerifiedPostF3EvidencePort verifier) {
        if (repository == null || jdbc == null || transactions == null || verifier == null)
            throw new IllegalArgumentException("U07_DURABLE_A1_DEPENDENCY_MISSING");
        this.repository = repository;
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.verifier = verifier;
    }

    /**
     * Row lock + F3 state verification + immutable single insert share ONE
     * database transaction. Exact replay returns the same row; a conflicting
     * replay cannot supersede it or sign for another CDP version.
     */
    public U04A1U05AuthorityBridge.A1BarrierAuthority issue(
            String consultationId, String cdpId, int version) {
        requireIdentity(consultationId, cdpId, version);
        return transactions.execute(status -> {
            CDP cdp = repository.findByIdWithLock(cdpId)
                    .orElseThrow(() -> new IllegalStateException("U07_A1_CDP_NOT_FOUND"));
            if (cdp.getVersion() == null || cdp.getVersion() != version)
                throw new IllegalStateException("U07_A1_CDP_VERSION_STALE");
            String effect = f3Effect(cdp);
            VerifiedEvidence evidence = verifier.evaluate(consultationId, cdpId, version, effect);
            if (evidence == null || !effect.equals(evidence.f3Effect)
                    || !evidence.barrierAllowed || !evidence.revalidatedCurrent
                    || !evidence.routeCurrent)
                throw new IllegalStateException("U07_A1_INDEPENDENT_SAFETY_NOT_AUTHORIZED");

            U04A1U05AuthorityBridge.A1BarrierAuthority existing =
                    select(consultationId, cdpId, version);
            if (existing != null) {
                if (!same(existing, evidence))
                    throw new IllegalStateException("U07_A1_DURABLE_REPLAY_CONFLICT");
                return existing;
            }
            jdbc.update("INSERT INTO u07_a1_attestation "
                    + "(consultation_id, cdp_id, cdp_version, f3_effect_id, barrier_ref, "
                    + "revalidation_ref, route_source_ref, route_authorization_ref, routing_policy_version) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    consultationId, cdpId, version, evidence.f3Effect, evidence.barrierRef,
                    evidence.revalidationRef, evidence.routeSourceRef,
                    evidence.routeAuthorizationRef, evidence.routingPolicyVersion);
            return select(consultationId, cdpId, version);
        });
    }

    @Override
    public U04A1U05AuthorityBridge.A1BarrierAuthority loadAttested(
            String consultationId, String cdpId, int expectedVersion) {
        requireIdentity(consultationId, cdpId, expectedVersion);
        return transactions.execute(status -> {
            CDP cdp = repository.findById(cdpId)
                    .orElseThrow(() -> new IllegalStateException("U07_A1_CDP_NOT_FOUND"));
            if (cdp.getVersion() == null || cdp.getVersion() != expectedVersion)
                throw new IllegalStateException("U07_A1_CDP_VERSION_STALE");
            String effect = f3Effect(cdp);
            U04A1U05AuthorityBridge.A1BarrierAuthority row =
                    select(consultationId, cdpId, expectedVersion);
            if (row == null) return null;
            if (!effect.equals(row.f3CommitRef))
                throw new IllegalStateException("U07_A1_DURABLE_F3_CHANGED");
            return row;
        });
    }

    private U04A1U05AuthorityBridge.A1BarrierAuthority select(
            String consultationId, String cdpId, int version) {
        java.util.List<U04A1U05AuthorityBridge.A1BarrierAuthority> found = jdbc.query(
                "SELECT consultation_id, cdp_id, cdp_version, f3_effect_id, barrier_ref, "
                + "revalidation_ref, route_source_ref, route_authorization_ref, routing_policy_version "
                + "FROM u07_a1_attestation WHERE consultation_id=? AND cdp_id=? AND cdp_version=?",
                (rs, index) -> map(rs), consultationId, cdpId, version);
        if (found.size() > 1) throw new IllegalStateException("U07_A1_DUPLICATE_AUTHORITY");
        return found.isEmpty() ? null : found.get(0);
    }

    private static U04A1U05AuthorityBridge.A1BarrierAuthority map(ResultSet rs)
            throws SQLException {
        return new U04A1U05AuthorityBridge.A1BarrierAuthority(
                rs.getString("consultation_id"), rs.getString("cdp_id"),
                rs.getInt("cdp_version"), rs.getString("f3_effect_id"),
                rs.getString("barrier_ref"), rs.getString("revalidation_ref"),
                rs.getString("route_source_ref"), rs.getString("route_authorization_ref"),
                rs.getString("routing_policy_version"), null, null, true, false);
    }

    private static boolean same(U04A1U05AuthorityBridge.A1BarrierAuthority row,
                                VerifiedEvidence e) {
        return row.f3CommitRef.equals(e.f3Effect)
                && row.barrierRef.equals(e.barrierRef)
                && row.revalidationRef.equals(e.revalidationRef)
                && row.routeSourceRef.equals(e.routeSourceRef)
                && row.routeAuthorizationRef.equals(e.routeAuthorizationRef)
                && row.routingPolicyVersion.equals(e.routingPolicyVersion);
    }

    private static String f3Effect(CDP cdp) {
        Map<String,Object> state = cdp.getPatientState();
        if (state == null || !(state.get("f3_gap_assessment") instanceof Map<?, ?>))
            throw new IllegalStateException("U07_A1_F3_NOT_COMMITTED");
        Object effect = ((Map<?, ?>) state.get("f3_gap_assessment")).get("f3_canonical_effect_id");
        if (!(effect instanceof String) || ((String) effect).trim().isEmpty())
            throw new IllegalStateException("U07_A1_F3_EFFECT_MISSING");
        return (String) effect;
    }

    private static String required(String v) {
        if (v == null || v.trim().isEmpty())
            throw new IllegalArgumentException("U07_A1_EVIDENCE_FIELD_REQUIRED");
        return v;
    }

    private static void requireIdentity(String consultation, String cdp, int version) {
        required(consultation);
        required(cdp);
        if (version < 0) throw new IllegalArgumentException("U07_A1_VERSION_INVALID");
    }
}
