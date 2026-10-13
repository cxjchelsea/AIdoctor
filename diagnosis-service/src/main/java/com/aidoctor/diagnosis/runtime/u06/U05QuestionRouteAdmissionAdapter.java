package com.aidoctor.diagnosis.runtime.u06;

import com.aidoctor.diagnosis.runtime.u05.U05DownstreamRoutingDecision;
import com.aidoctor.diagnosis.runtime.u05.U05ExecutionResult;

/**
 * Non-production, fail-closed U05 to U06 question admission boundary.
 * U05 routing eligibility is never itself U06 admission authority.
 * The host must independently supply current U06AdmissionEvidence.
 * This adapter does not send a question or mutate consultation state.
 */
public final class U05QuestionRouteAdmissionAdapter {
    private final U06AdmissionService admission;

    public U05QuestionRouteAdmissionAdapter(U06AdmissionService admission) {
        if (admission == null) throw new IllegalArgumentException("admission required");
        this.admission = admission;
    }

    public U06AdmissionService.Admission admit(
            U05ExecutionResult upstream,
            U06AdmissionEvidence hostEvidence,
            String questionPolicyRef,
            String d04PolicyRef,
            String dependencyRef,
            String threadId,
            String runId,
            long expectedConsultationRowVersion) {
        if (upstream == null
                || !U05ExecutionResult.ROUTING_COMPLETE.equals(upstream.getStatus())
                || upstream.getCommitResult() == null
                || !"COMMITTED".equals(upstream.getCommitResult().status)
                || upstream.getRoutingDecision() == null) {
            throw new IllegalStateException("U05_COMMITTED_ROUTING_REQUIRED");
        }
        U05DownstreamRoutingDecision route = upstream.getRoutingDecision();
        if (!U05DownstreamRoutingDecision.ELIGIBLE.equals(route.getRoutingStatus())
                || !U05DownstreamRoutingDecision.TO_U06_QUESTION_PATH.equals(route.getDownstreamConsequence())
                || !"U06".equals(route.getTargetUnitId())
                || route.getEligibility() == null
                || !"CURRENT".equals(route.getValidity())
                || route.getSourceReadinessCommittedVersion() != route.getCurrentClinicalStateVersionAtRouting()
                || !required(route.getRouteEffectId())
                || !required(route.getDownstreamRouteAuthorizationId())) {
            throw new IllegalStateException("U05_U06_QUESTION_ROUTE_NOT_AUTHORIZED");
        }
        if (hostEvidence == null) {
            throw new IllegalStateException("U06_INDEPENDENT_HOST_EVIDENCE_REQUIRED");
        }
        if (!required(questionPolicyRef) || !required(d04PolicyRef)
                || !required(dependencyRef) || !required(threadId) || !required(runId)) {
            throw new IllegalArgumentException("U06 question execution bindings required");
        }

        int version = route.getSourceReadinessCommittedVersion();
        U06ProfileBRequest request = new U06ProfileBRequest(
                "u05-u06-" + route.getRouteEffectId(),
                route.getConsultationId(), route.getCdpId(),
                U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,
                U06ProfileBRequest.U05_QUESTION_ROUTING,
                route.getDownstreamRouteAuthorizationId(),
                version, version,
                U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING, dependencyRef,
                "u05-readiness-" + route.getSourceReadinessEffectId(),
                questionPolicyRef, d04PolicyRef,
                route.getCanonicalEventRef(), route.getBusinessEventIdentity(),
                threadId, runId, expectedConsultationRowVersion,
                route.getCorrelationId(), route.getTraceId(), route.getCreatedAt(),
                hostEvidence);
        return admission.admit(request, version);
    }

    private static boolean required(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
