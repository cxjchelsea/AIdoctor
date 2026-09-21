package com.aidoctor.diagnosis.runtime.u05;

/**
 * Deterministic RDP-04 projection.
 *
 * <p>Stops at typed eligibility/failure handoff. It never invokes Scheduler or
 * any U06/U08/U10/U11/U14 owner.</p>
 */
public final class U05RoutingService {
    public static final String ROUTING_POLICY_VERSION = "U05-RDP04-V1-FROZEN";
    public static final String ROUTING_DECISION_CONTRACT_VERSION = "U05-RDP04-DECISION-V1";
    public static final String ROUTE_EFFECT_CONTRACT_VERSION = "U05-RDP04-EFFECT-V1";
    public static final String ELIGIBILITY_CONTRACT_VERSION = "U05-RDP04-ELIGIBILITY-V1";

    private final U05RouteLedger ledger;
    private final U05DownstreamPermissionPort permissionPort;

    public U05RoutingService(U05RouteLedger ledger, U05DownstreamPermissionPort permissionPort) {
        if (ledger == null) throw new IllegalArgumentException("ledger is required");
        if (permissionPort == null) throw new IllegalArgumentException("permissionPort is required");
        this.ledger = ledger;
        this.permissionPort = permissionPort;
    }

    public U05DownstreamRoutingDecision route(
            U05AdmittedInput input,
            U05ClinicalReadinessDecision readinessDecision,
            U05ClinicalReadinessCommitEvidence commitEvidence,
            U05RoutingCurrentness currentness) {
        if (input == null || readinessDecision == null || commitEvidence == null || currentness == null) {
            throw new IllegalArgumentException("routing inputs are required");
        }
        if (!readinessDecision.isDecided()) {
            throw new IllegalStateException("ordinary routing requires DECIDED readiness");
        }
        if (!"COMMITTED".equals(commitEvidence.getCommitStatus())) {
            throw new IllegalStateException("ordinary routing requires authoritative committed readiness");
        }

        RouteTarget target = map(readinessDecision.getClinicalReadiness());

        if (!currentness.ordinaryRouteCurrentFor(commitEvidence)) {
            return finalizeDecision(
                    input, readinessDecision, commitEvidence, currentness, target,
                    U05DownstreamRoutingDecision.REJECTED_STALE,
                    null, null,
                    "U05_ROUTE_READINESS_OR_DEPENDENCY_STALE");
        }

        String gate = currentness.getGateValue();
        if (U05ConsumerInboundRequest.GATE_BLOCKED.equals(gate)) {
            return finalizeDecision(
                    input, readinessDecision, commitEvidence, currentness, target,
                    U05DownstreamRoutingDecision.PREEMPTED,
                    null, null,
                    "U05_ROUTE_SAFETY_PREEMPTED");
        }
        if (U05ConsumerInboundRequest.GATE_UNAVAILABLE.equals(gate)) {
            return finalizeDecision(
                    input, readinessDecision, commitEvidence, currentness, target,
                    U05DownstreamRoutingDecision.FAILURE_REQUIRED,
                    null, null,
                    "U05_ROUTE_SAFETY_UNAVAILABLE");
        }
        if (U05ConsumerInboundRequest.GATE_ALLOW.equals(gate)) {
            return finalizeDecision(
                    input, readinessDecision, commitEvidence, currentness, target,
                    U05DownstreamRoutingDecision.ELIGIBLE,
                    null, null,
                    "U05_ROUTE_ELIGIBLE");
        }
        if (!U05ConsumerInboundRequest.GATE_RESTRICTED.equals(gate)) {
            return finalizeDecision(
                    input, readinessDecision, commitEvidence, currentness, target,
                    U05DownstreamRoutingDecision.FAILURE_REQUIRED,
                    null, null,
                    "U05_ROUTE_UNSUPPORTED_SAFETY_GATE");
        }

        if (!equal(currentness.getRestrictedContextRef(), input.getAcceptedRestrictedContextRef())) {
            return finalizeDecision(
                    input, readinessDecision, commitEvidence, currentness, target,
                    U05DownstreamRoutingDecision.REJECTED_STALE,
                    null, null,
                    "U05_ROUTE_RESTRICTED_CONTEXT_STALE");
        }

        U05DownstreamPermissionDecision permission = permissionPort.evaluate(
                input,
                commitEvidence,
                currentness,
                target.consequence,
                target.unitId,
                target.action);

        if (!validPermissionBinding(input, currentness, target, permission)) {
            return finalizeDecision(
                    input, readinessDecision, commitEvidence, currentness, target,
                    U05DownstreamRoutingDecision.FAILURE_REQUIRED,
                    permission,
                    null,
                    "U05_ROUTE_PERMISSION_BINDING_INVALID");
        }

        if (U05DownstreamPermissionDecision.DENIED.equals(permission.getPermissionStatus())) {
            return finalizeDecision(
                    input, readinessDecision, commitEvidence, currentness, target,
                    U05DownstreamRoutingDecision.PREEMPTED,
                    permission,
                    null,
                    "U05_ROUTE_PERMISSION_DENIED");
        }
        if (U05DownstreamPermissionDecision.UNAVAILABLE.equals(permission.getPermissionStatus())) {
            return finalizeDecision(
                    input, readinessDecision, commitEvidence, currentness, target,
                    U05DownstreamRoutingDecision.FAILURE_REQUIRED,
                    permission,
                    null,
                    "U05_ROUTE_PERMISSION_UNAVAILABLE");
        }

        return finalizeDecision(
                input, readinessDecision, commitEvidence, currentness, target,
                U05DownstreamRoutingDecision.ELIGIBLE,
                permission,
                permission.getPermissionRef(),
                "U05_ROUTE_ELIGIBLE_RESTRICTED");
    }

    private U05DownstreamRoutingDecision finalizeDecision(
            U05AdmittedInput input,
            U05ClinicalReadinessDecision readinessDecision,
            U05ClinicalReadinessCommitEvidence commitEvidence,
            U05RoutingCurrentness currentness,
            RouteTarget target,
            String status,
            U05DownstreamPermissionDecision permission,
            String permissionRef,
            String reasonCode) {
        String permissionDecisionRef = permission == null ? null : permission.getPermissionDecisionId();
        String routingDecisionId = U05Ids.hash(
                "u05-routing-decision",
                input.getConsultationId(),
                input.getCdpId(),
                commitEvidence.getEffectId(),
                currentness.getCurrentU04GateRef(),
                String.valueOf(currentness.getCurrentClinicalStateVersion()),
                target.consequence,
                target.unitId,
                permissionDecisionRef,
                permission == null ? null : permission.getPermissionStatus(),
                status,
                ROUTING_POLICY_VERSION,
                ROUTING_DECISION_CONTRACT_VERSION);

        String failureHandoffRef = U05DownstreamRoutingDecision.FAILURE_REQUIRED.equals(status)
                ? U05Ids.hash("u05-failure-handoff", routingDecisionId, reasonCode)
                : null;

        String fingerprint = U05Ids.hash(
                "u05-routing-fingerprint",
                commitEvidence.getAuthoritativeReadinessRecordRef(),
                commitEvidence.getEffectId(),
                readinessDecision.getClinicalReadiness(),
                currentness.getCurrentU04GateRef(),
                currentness.getGateValue(),
                String.valueOf(currentness.getCurrentClinicalStateVersion()),
                target.consequence,
                target.unitId,
                permissionDecisionRef,
                permission == null ? null : permission.getPermissionStatus(),
                permissionRef,
                status,
                failureHandoffRef,
                reasonCode,
                currentness.getRoutingContextRef(),
                ROUTING_POLICY_VERSION,
                ROUTING_DECISION_CONTRACT_VERSION);

        U05RouteLedger.Entry existing = ledger.find(routingDecisionId);
        if (existing != null) {
            if (!fingerprint.equals(existing.getFingerprint())) {
                throw new IllegalStateException("U05_ROUTE_REPLAY_CONFLICT");
            }
            return existing.getDecision().reattached();
        }

        String routeEffectId = null;
        String routeAuthorizationId = null;
        U05DownstreamEligibility eligibility = null;
        if (U05DownstreamRoutingDecision.ELIGIBLE.equals(status)) {
            routeEffectId = U05Ids.hash(
                    "u05-route-effect",
                    input.getConsultationId(),
                    input.getCdpId(),
                    commitEvidence.getEffectId(),
                    commitEvidence.getAuthoritativeReadinessRecordRef(),
                    readinessDecision.getClinicalReadiness(),
                    currentness.getRoutingContextRef(),
                    currentness.getCurrentU04GateRef(),
                    String.valueOf(currentness.getCurrentClinicalStateVersion()),
                    target.consequence,
                    target.unitId,
                    currentness.getRestrictedContextRef(),
                    permissionRef,
                    ROUTING_POLICY_VERSION,
                    ROUTE_EFFECT_CONTRACT_VERSION);
            routeAuthorizationId = U05Ids.hash(
                    "u05-downstream-route-auth",
                    routeEffectId,
                    routingDecisionId,
                    commitEvidence.getEffectId(),
                    currentness.getCurrentU04GateRef(),
                    target.consequence,
                    target.unitId,
                    permissionRef,
                    ROUTING_POLICY_VERSION);
            String eligibilityId = U05Ids.hash(
                    "u05-downstream-eligibility",
                    routeEffectId,
                    ELIGIBILITY_CONTRACT_VERSION);
            eligibility = new U05DownstreamEligibility(
                    eligibilityId,
                    routingDecisionId,
                    routeEffectId,
                    input,
                    commitEvidence,
                    readinessDecision.getClinicalReadiness(),
                    currentness,
                    target.consequence,
                    target.unitId,
                    permissionRef,
                    ROUTING_POLICY_VERSION);
        }

        U05DownstreamRoutingDecision decision = new U05DownstreamRoutingDecision(
                routingDecisionId,
                fingerprint,
                input,
                readinessDecision,
                commitEvidence,
                currentness,
                ROUTING_POLICY_VERSION,
                status,
                U05DownstreamRoutingDecision.ORIGINAL,
                target.consequence,
                target.unitId,
                U05DownstreamRoutingDecision.ELIGIBLE.equals(status) ? target.consequence : null,
                U05DownstreamRoutingDecision.ELIGIBLE.equals(status) ? target.unitId : null,
                permissionDecisionRef,
                permissionRef,
                routeEffectId,
                routeAuthorizationId,
                failureHandoffRef,
                reasonCode,
                eligibility);
        ledger.store(routingDecisionId, fingerprint, decision);
        return decision;
    }

    private static boolean validPermissionBinding(
            U05AdmittedInput input,
            U05RoutingCurrentness currentness,
            RouteTarget target,
            U05DownstreamPermissionDecision permission) {
        return permission != null
                && input.getConsultationId().equals(permission.getConsultationId())
                && input.getCdpId().equals(permission.getCdpId())
                && currentness.getCurrentU04GateRef().equals(permission.getCurrentU04GateRef())
                && equal(currentness.getRestrictedContextRef(), permission.getRestrictedContextRef())
                && target.consequence.equals(permission.getCandidateDownstreamConsequence())
                && target.unitId.equals(permission.getTargetUnitId())
                && "CURRENT".equals(permission.getValidity());
    }

    private static RouteTarget map(String readiness) {
        if (U05ClinicalReadinessDecision.NEEDS_CLARIFICATION.equals(readiness)
                || U05ClinicalReadinessDecision.CAN_ASK_MORE.equals(readiness)) {
            return new RouteTarget(
                    U05DownstreamRoutingDecision.TO_U06_QUESTION_PATH,
                    "U06",
                    "U06_QUESTION_PATH");
        }
        if (U05ClinicalReadinessDecision.READY_FOR_CLINICAL_ANALYSIS.equals(readiness)) {
            return new RouteTarget(
                    U05DownstreamRoutingDecision.TO_U08_CLINICAL_ANALYSIS,
                    "U08",
                    "U08_CLINICAL_ANALYSIS");
        }
        if (U05ClinicalReadinessDecision.NEEDS_OFFLINE_EVIDENCE.equals(readiness)) {
            return new RouteTarget(
                    U05DownstreamRoutingDecision.TO_U10_OFFLINE_EVIDENCE,
                    "U10",
                    "U10_OFFLINE_EVIDENCE");
        }
        if (U05ClinicalReadinessDecision.OUT_OF_SCOPE.equals(readiness)
                || U05ClinicalReadinessDecision.NO_RELIABLE_DIRECTION.equals(readiness)) {
            return new RouteTarget(
                    U05DownstreamRoutingDecision.TO_U11_SAFE_EXIT,
                    "U11",
                    "U11_SAFE_EXIT");
        }
        throw new IllegalStateException("unsupported frozen readiness-to-target mapping");
    }

    private static boolean equal(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static final class RouteTarget {
        final String consequence;
        final String unitId;
        final String action;

        RouteTarget(String consequence, String unitId, String action) {
            this.consequence = consequence;
            this.unitId = unitId;
            this.action = action;
        }
    }
}
