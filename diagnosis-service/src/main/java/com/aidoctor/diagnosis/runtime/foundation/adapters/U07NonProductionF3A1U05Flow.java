package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.runtime.u03.U03OutboundHandoff;
import com.aidoctor.diagnosis.runtime.u04.U04ExecutionResult;
import com.aidoctor.diagnosis.runtime.u05.U04A1U05AuthorityBridge;
import com.aidoctor.diagnosis.runtime.u05.U05AdmissionAuthoritySnapshot;
import com.aidoctor.diagnosis.runtime.u05.U05ConsumerInboundRequest;
import com.aidoctor.diagnosis.runtime.u05.U05ExecutionResult;
import com.aidoctor.diagnosis.runtime.u05.U05NonProductionApplicationService;
import com.aidoctor.diagnosis.runtime.u05.U05ReadinessInputManifest;

/**
 * Explicit NON-PRODUCTION business sequence. Does not create clinical decisions,
 * authentication, production authority, outbound delivery or a second state store.
 *
 * The owner MUST perform an actual CDP commit and authoritative readback;
 * the existing A1 issuer independently checks the persisted F3 effect and
 * post-F3 safety/revalidation evidence. The bridge then constructs the
 * independently observed U05 authority. A claimed inbound request is never
 * allowed to replace that authority.
 */
public final class U07NonProductionF3A1U05Flow {
    public interface F3Owner {
        F3Committed executeAndReadback(String consultationId, String cdpId);
    }

    public static final class F3Committed {
        public final String consultationId, cdpId, effectId;
        public final int persistedVersion;
        public final boolean committedAndReadback;

        public F3Committed(String consultationId, String cdpId, String effectId,
                int persistedVersion, boolean committedAndReadback) {
            this.consultationId = consultationId;
            this.cdpId = cdpId;
            this.effectId = effectId;
            this.persistedVersion = persistedVersion;
            this.committedAndReadback = committedAndReadback;
        }
    }

    public interface U05InputProvider {
        U05ConsumerInboundRequest request(U05AdmissionAuthoritySnapshot currentAuthority);
        U05ReadinessInputManifest manifest(U05AdmissionAuthoritySnapshot currentAuthority);
    }

    private final F3Owner owner;
    private final U07DurableNonProductionA1Issuer issuer;
    private final U04A1U05AuthorityBridge bridge;
    private final U05InputProvider inputs;
    private final U05NonProductionApplicationService u05;

    public U07NonProductionF3A1U05Flow(F3Owner owner,
            U07DurableNonProductionA1Issuer issuer, U04A1U05AuthorityBridge bridge,
            U05InputProvider inputs, U05NonProductionApplicationService u05) {
        if (owner == null || issuer == null || bridge == null || inputs == null || u05 == null)
            throw new IllegalArgumentException("U07_F3_A1_U05_DEPENDENCY_MISSING");
        this.owner = owner;
        this.issuer = issuer;
        this.bridge = bridge;
        this.inputs = inputs;
        this.u05 = u05;
    }

    public U05ExecutionResult execute(U03OutboundHandoff handoff, U04ExecutionResult u04,
            String environmentId, String authoritativeStateRef) {
        if (handoff == null || u04 == null || blank(environmentId)
                || blank(authoritativeStateRef) || production(environmentId))
            throw new IllegalArgumentException("U07_F3_A1_U05_NONPROD_INPUT_REQUIRED");
        if (!U04ExecutionResult.COMMIT_COMPLETE.equals(u04.getStatus())
                || u04.getCommitResult() == null
                || !"COMMITTED".equals(u04.getCommitResult().status)
                || !handoff.getCdpId().equals(u04.getCommitResult().cdpId))
            throw new IllegalStateException("U07_U04_REAL_COMMIT_REQUIRED");

        F3Committed f3 = owner.executeAndReadback(handoff.getConsultationId(), handoff.getCdpId());
        if (f3 == null || !f3.committedAndReadback || blank(f3.effectId)
                || f3.persistedVersion <= u04.getCommitResult().committedVersion.intValue()
                || !handoff.getConsultationId().equals(f3.consultationId)
                || !handoff.getCdpId().equals(f3.cdpId))
            throw new IllegalStateException("U07_F3_OWNER_COMMIT_READBACK_REQUIRED");

        // Issuer checks the real persisted CDP row under lock, with independent
        // post-F3 barrier and revalidation evidence; missing proof fails closed.
        U04A1U05AuthorityBridge.A1BarrierAuthority issued =
                issuer.issue(f3.consultationId, f3.cdpId, f3.persistedVersion);
        if (issued == null || !f3.effectId.equals(issued.f3CommitRef))
            throw new IllegalStateException("U07_F3_A1_EFFECT_MISMATCH");

        U05AdmissionAuthoritySnapshot authority =
                bridge.load(handoff, u04, environmentId, authoritativeStateRef);
        if (authority.getCurrentClinicalStateVersion() != f3.persistedVersion
                || !authority.isA1CanonicalF3Complete()
                || !authority.isPostF3SafetyBarrierComplete()
                || !authority.isF3RevalidatedCurrent()
                || !authority.isRouteAuthorizationCurrent())
            throw new IllegalStateException("U07_A1_U05_AUTHORITY_NOT_CURRENT");

        U05ConsumerInboundRequest request = inputs.request(authority);
        U05ReadinessInputManifest manifest = inputs.manifest(authority);
        if (request == null || manifest == null
                || !handoff.getConsultationId().equals(request.getConsultationId())
                || !handoff.getCdpId().equals(request.getCdpId())
                || request.getClaimedClinicalStateVersion() != f3.persistedVersion
                || !environmentId.equals(request.getEnvironmentId())
                || !U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(
                        request.getEvaluationContext())
                || !authority.getRouteAuthorizationRef().equals(
                        request.getRouteAuthorizationRef())
                || !authority.getRouteSourceRef().equals(request.getRouteSourceRef())
                || !authority.getRoutingPolicyVersion().equals(
                        request.getRoutingPolicyVersion())
                || !manifest.getConsultationId().equals(handoff.getConsultationId())
                || !manifest.getCdpId().equals(handoff.getCdpId())
                || manifest.getClinicalStateVersion() != f3.persistedVersion)
            throw new IllegalStateException("U07_U05_INPUT_AUTHORITY_MISMATCH");
        // U05 retains its own admission, commit and currentness guards.
        return u05.execute(request, manifest, authority);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean production(String value) {
        String v = value.trim().toLowerCase(java.util.Locale.ROOT);
        return v.equals("prod") || v.equals("production")
                || v.startsWith("prod-") || v.startsWith("production-");
    }
}
