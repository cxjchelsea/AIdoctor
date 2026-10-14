package com.aidoctor.diagnosis.runtime.foundation.adapters;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Read-only, fail-closed intake of three separately persisted F3-owner,
 * post-F3 safety and revalidation decisions for NON-PRODUCTION A1 issuance.
 *
 * The records are never synthesized from the CDP. Their writers, authenticity,
 * authorization and deployment migrations are NOT supplied by this adapter.
 */
public final class U07PersistedF3OwnerBarrierRevalidationEvidence
        implements U07DurableNonProductionA1Issuer.VerifiedPostF3EvidencePort {
    /** Independent host attestation; DB status fields are not proof of authority. */
    public interface TrustedEvidenceVerifier {
        boolean verify(String consultationId, String cdpId, int version, String effectId,
                String ownerCommitRef, String barrierRef, String revalidationRef,
                String routeAuthorizationRef);
    }

    private final JdbcTemplate jdbc;
    private final TrustedEvidenceVerifier trustedVerifier;

    public U07PersistedF3OwnerBarrierRevalidationEvidence(JdbcTemplate jdbc,
            TrustedEvidenceVerifier trustedVerifier) {
        if (jdbc == null || trustedVerifier == null)
            throw new IllegalArgumentException("U07_OWNER_EVIDENCE_AND_TRUST_REQUIRED");
        this.jdbc = jdbc;
        this.trustedVerifier = trustedVerifier;
    }

    @Override
    public U07DurableNonProductionA1Issuer.VerifiedEvidence evaluate(
            String consultationId, String cdpId, int version, String effectId) {
        if (blank(consultationId) || blank(cdpId) || blank(effectId) || version < 0)
            throw new IllegalArgumentException("U07_OWNER_EVIDENCE_INPUT_INVALID");

        List<String[]> owners = jdbc.query(
                "SELECT owner_commit_ref, state FROM u07_f3_owner_evidence "
                + "WHERE consultation_id=? AND cdp_id=? AND cdp_version=? AND f3_effect_id=?",
                (rs, n) -> new String[]{rs.getString(1), rs.getString(2)},
                consultationId, cdpId, version, effectId);
        if (owners.size() != 1 || blank(owners.get(0)[0])
                || !"COMMITTED".equals(owners.get(0)[1]))
            throw new IllegalStateException("U07_F3_OWNER_NOT_AUTHORITATIVE");
        String ownerCommit = owners.get(0)[0];

        List<String[]> barriers = jdbc.query(
                "SELECT barrier_ref, outcome FROM u07_f3_barrier_evidence "
                + "WHERE consultation_id=? AND cdp_id=? AND cdp_version=? "
                + "AND f3_effect_id=? AND owner_commit_ref=?",
                (rs, n) -> new String[]{rs.getString(1), rs.getString(2)},
                consultationId, cdpId, version, effectId, ownerCommit);
        if (barriers.size() != 1 || blank(barriers.get(0)[0])
                || !"ALLOWED".equals(barriers.get(0)[1]))
            throw new IllegalStateException("U07_F3_SAFETY_BARRIER_NOT_ALLOWED");
        String barrier = barriers.get(0)[0];

        List<String[]> validations = jdbc.query(
                "SELECT revalidation_ref, outcome, route_source_ref, route_authorization_ref, "
                + "routing_policy_version FROM u07_f3_revalidation_evidence "
                + "WHERE consultation_id=? AND cdp_id=? AND cdp_version=? "
                + "AND f3_effect_id=? AND owner_commit_ref=? AND barrier_ref=?",
                (rs, n) -> new String[]{
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5)},
                consultationId, cdpId, version, effectId, ownerCommit, barrier);
        if (validations.size() != 1 || blank(validations.get(0)[0])
                || !"REVALIDATED_CURRENT".equals(validations.get(0)[1])
                || blank(validations.get(0)[2])
                || blank(validations.get(0)[3])
                || blank(validations.get(0)[4]))
            throw new IllegalStateException("U07_F3_REVALIDATION_NOT_CURRENT");

        String[] revalidated = validations.get(0);
        // A mutable SQL status is never sufficient independent clinical authority.
        if (!trustedVerifier.verify(consultationId, cdpId, version, effectId,
                ownerCommit, barrier, revalidated[0], revalidated[3]))
            throw new IllegalStateException("U07_F3_INDEPENDENT_TRUST_NOT_VERIFIED");
        return new U07DurableNonProductionA1Issuer.VerifiedEvidence(
                effectId, barrier, revalidated[0], revalidated[2],
                revalidated[3], revalidated[4], true, true, true);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
