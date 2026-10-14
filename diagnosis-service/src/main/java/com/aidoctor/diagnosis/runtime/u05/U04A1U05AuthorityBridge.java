package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.diagnosis.runtime.u03.U03OutboundHandoff;
import com.aidoctor.diagnosis.runtime.u04.U04ExecutionResult;
import com.aidoctor.diagnosis.runtime.u04.U04SafetyGateDecision;
import com.aidoctor.diagnosis.runtime.u04.U04ScopeContext;

/**
 * Explicit non-production U04 -> A1 -> U05 authority assembly boundary.
 *
 * U04 eligibility cannot issue an A1 route. Both current gate readback and
 * post-F3 A1 authorization MUST come from separate authoritative host ports.
 * The U05 request is never an authority source. No owner execution occurs here.
 */
public final class U04A1U05AuthorityBridge {
    public interface GateReadbackPort {
        GateReadback loadCurrent(String cdpId);
    }

    public interface A1BarrierAuthorityPort {
        A1BarrierAuthority loadCurrent(String consultationId, String cdpId, int stateVersion);
    }

    public static final class GateReadback {
        public final int stateVersion;
        public final String gateValue;
        public final String sourceU03DecisionRef;
        public final String policyRef;

        public GateReadback(int stateVersion, String gateValue,
                            String sourceU03DecisionRef, String policyRef) {
            this.stateVersion = stateVersion;
            this.gateValue = gateValue;
            this.sourceU03DecisionRef = sourceU03DecisionRef;
            this.policyRef = policyRef;
        }
    }

    /** Read-only host evidence. A synthetic fixture is not a production issuer. */
    public static final class A1BarrierAuthority {
        public final String consultationId, cdpId, f3CommitRef, barrierRef, revalidationRef;
        public final String routeSourceRef, routeAuthorizationRef, routingPolicyVersion;
        public final String restrictedContextRef, restrictedPermissionRef;
        public final int clinicalStateVersion;
        public final boolean routeCurrent, restrictedActionPermitted;

        public A1BarrierAuthority(
                String consultationId, String cdpId, int clinicalStateVersion,
                String f3CommitRef, String barrierRef, String revalidationRef,
                String routeSourceRef, String routeAuthorizationRef,
                String routingPolicyVersion, String restrictedContextRef,
                String restrictedPermissionRef, boolean routeCurrent,
                boolean restrictedActionPermitted) {
            this.consultationId = consultationId;
            this.cdpId = cdpId;
            this.clinicalStateVersion = clinicalStateVersion;
            this.f3CommitRef = f3CommitRef;
            this.barrierRef = barrierRef;
            this.revalidationRef = revalidationRef;
            this.routeSourceRef = routeSourceRef;
            this.routeAuthorizationRef = routeAuthorizationRef;
            this.routingPolicyVersion = routingPolicyVersion;
            this.restrictedContextRef = restrictedContextRef;
            this.restrictedPermissionRef = restrictedPermissionRef;
            this.routeCurrent = routeCurrent;
            this.restrictedActionPermitted = restrictedActionPermitted;
        }
    }

    private final GateReadbackPort gatePort;
    private final A1BarrierAuthorityPort barrierPort;

    public U04A1U05AuthorityBridge(GateReadbackPort gatePort,
                                   A1BarrierAuthorityPort barrierPort) {
        if (gatePort == null || barrierPort == null) {
            throw new IllegalArgumentException("independent gate and A1 ports required");
        }
        this.gatePort = gatePort;
        this.barrierPort = barrierPort;
    }

    public U05AdmissionAuthoritySnapshot load(
            U03OutboundHandoff handoff, U04ExecutionResult executed,
            String environmentId, String authoritativeStateRef) {
        if (handoff == null || executed == null
                || !U04ExecutionResult.COMMIT_COMPLETE.equals(executed.getStatus())
                || executed.getCommitResult() == null
                || !"COMMITTED".equals(executed.getCommitResult().status)
                || executed.getCommitResult().committedVersion == null
                || executed.getCommitResult().auditRef == null
                || blank(executed.getCommitResult().auditRef.auditId)
                || !handoff.getCdpId().equals(executed.getCommitResult().cdpId)
                || executed.getDecision() == null
                || executed.getRoutingEligibility() == null
                || !executed.getRoutingEligibility().isU05Eligible()) {
            throw new IllegalStateException("U04_COMMITTED_ELIGIBLE_GATE_REQUIRED");
        }
        if (blank(environmentId) || isProduction(environmentId)
                || !environmentId.equals(handoff.getEnvironmentId())
                || blank(authoritativeStateRef)) {
            throw new IllegalStateException("U05_NONPRODUCTION_ENVIRONMENT_OR_STATE_REQUIRED");
        }
        int current = executed.getCommitResult().committedVersion.intValue();
        if (current != executed.getDecision().getClinicalStateVersion() + 1
                || !handoff.getDecisionId().equals(executed.getDecision().getSourceU03DecisionRef())) {
            throw new IllegalStateException("U04_COMMIT_PROVENANCE_MISMATCH");
        }

        GateReadback readback = gatePort.loadCurrent(handoff.getCdpId());
        if (readback == null || readback.stateVersion != current
                || !executed.getDecision().getGate().equals(readback.gateValue)
                || !handoff.getDecisionId().equals(readback.sourceU03DecisionRef)
                || !U04ScopeContext.FROZEN_POLICY_REF.equals(readback.policyRef)) {
            throw new IllegalStateException("U04_GATE_READBACK_NOT_CURRENT");
        }

        A1BarrierAuthority a1 = barrierPort.loadCurrent(
                handoff.getConsultationId(), handoff.getCdpId(), current);
        if (a1 == null
                || !handoff.getConsultationId().equals(a1.consultationId)
                || !handoff.getCdpId().equals(a1.cdpId)
                || a1.clinicalStateVersion != current
                || blank(a1.f3CommitRef) || blank(a1.barrierRef)
                || blank(a1.revalidationRef) || !a1.routeCurrent
                || blank(a1.routeSourceRef) || blank(a1.routeAuthorizationRef)
                || blank(a1.routingPolicyVersion)) {
            throw new IllegalStateException("A1_F3_BARRIER_AUTHORITY_NOT_CURRENT");
        }
        boolean restricted = U04SafetyGateDecision.RESTRICTED.equals(readback.gateValue);
        if (restricted && (blank(a1.restrictedContextRef)
                || blank(a1.restrictedPermissionRef)
                || !a1.restrictedActionPermitted)) {
            throw new IllegalStateException("A1_RESTRICTED_PERMISSION_REQUIRED");
        }
        return new U05AdmissionAuthoritySnapshot(
                handoff.getConsultationId(), handoff.getCdpId(), current,
                authoritativeStateRef, environmentId, true,
                true, true, executed.getDecision().getDecisionId(),
                executed.getCommitResult().auditRef.auditId, readback.gateValue,
                true, a1.routeSourceRef, "U04_A1_ROUTING_AUTHORIZATION",
                a1.routeAuthorizationRef, U05ConsumerInboundRequest.U05_ELIGIBLE,
                a1.routingPolicyVersion,
                restricted ? a1.restrictedContextRef : null,
                restricted ? a1.restrictedPermissionRef : null,
                restricted && a1.restrictedActionPermitted,
                "A1", true, true, true, false, null);
    }

    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }

    private static boolean isProduction(String s) {
        String value = s.trim().toLowerCase(java.util.Locale.ROOT);
        return "prod".equals(value) || "production".equals(value)
                || value.startsWith("prod-") || value.startsWith("production-");
    }
}
