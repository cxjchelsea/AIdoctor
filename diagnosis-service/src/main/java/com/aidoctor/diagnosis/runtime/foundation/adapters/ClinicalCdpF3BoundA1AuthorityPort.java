package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u05.U04A1U05AuthorityBridge;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks independent A1 attestations against the CURRENT, physically persisted
 * F3 effect. This is a consumer/verifier, never an A1 authority issuer.
 *
 * <p>The independent host must provide an authenticated and provenance-checked
 * source. An in-memory/synthetic source is NOT valid in production. No bean is
 * auto-installed, so the production authority remains unavailable by default.
 */
public final class ClinicalCdpF3BoundA1AuthorityPort
        implements U04A1U05AuthorityBridge.A1BarrierAuthorityPort {
    public interface IndependentlyAttestedA1Source {
        U04A1U05AuthorityBridge.A1BarrierAuthority loadAttested(
                String consultationId, String cdpId, int expectedVersion);
    }

    private final CDPRepository repository;
    private final IndependentlyAttestedA1Source attested;

    public ClinicalCdpF3BoundA1AuthorityPort(
            CDPRepository repository, IndependentlyAttestedA1Source attested) {
        if (repository == null || attested == null)
            throw new IllegalArgumentException("Independent A1 attestation and CDP required");
        this.repository = repository;
        this.attested = attested;
    }

    @Override
    @Transactional(readOnly = true)
    public U04A1U05AuthorityBridge.A1BarrierAuthority loadCurrent(
            String consultationId, String cdpId, int expectedVersion) {
        if (blank(consultationId) || blank(cdpId) || expectedVersion < 0)
            throw new IllegalArgumentException("U07_A1_IDENTITY_VERSION_INVALID");
        CDP cdp = repository.findById(cdpId)
                .orElseThrow(() -> new IllegalStateException("U07_A1_CDP_NOT_FOUND"));
        if (cdp.getVersion() == null || cdp.getVersion().intValue() != expectedVersion)
            throw new IllegalStateException("U07_A1_CDP_VERSION_STALE");
        Map<String, Object> state = cdp.getPatientState();
        if (state == null || !(state.get("f3_gap_assessment") instanceof Map<?, ?>))
            throw new IllegalStateException("U07_A1_F3_NOT_PERSISTED");
        Object effect = ((Map<?, ?>) state.get("f3_gap_assessment")).get("f3_canonical_effect_id");
        if (!(effect instanceof String) || blank((String) effect))
            throw new IllegalStateException("U07_A1_F3_EFFECT_MISSING");

        // The CDP never issues authorization; only a separately trusted host
        // may attest post-F3 barrier, revalidation and route permission.
        U04A1U05AuthorityBridge.A1BarrierAuthority a1 =
                attested.loadAttested(consultationId, cdpId, expectedVersion);
        if (a1 == null || !consultationId.equals(a1.consultationId)
                || !cdpId.equals(a1.cdpId)
                || expectedVersion != a1.clinicalStateVersion
                || !effect.equals(a1.f3CommitRef)
                || blank(a1.barrierRef) || blank(a1.revalidationRef)
                || blank(a1.routeSourceRef) || blank(a1.routeAuthorizationRef)
                || blank(a1.routingPolicyVersion) || !a1.routeCurrent) {
            throw new IllegalStateException("U07_A1_INDEPENDENT_ATTESTATION_MISMATCH");
        }
        return a1;
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
