package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.contracts.v1.StateTypes;

/**
 * Authorized U05 non-production vertical slice.
 *
 * <p>Stops at committed Clinical Readiness plus typed downstream eligibility.
 * It does not invoke U06/U08/U10/U11/U14 or production routing.</p>
 */
public final class U05NonProductionApplicationService {
    private final U05AdmissionService admissionService;
    private final U05ClinicalReadinessPolicy readinessPolicy;
    private final U05ReadinessStateProposalFactory proposalFactory;
    private final U05CommitService commitService;
    private final U05RoutingCurrentnessPort currentnessPort;
    private final U05RoutingService routingService;

    public U05NonProductionApplicationService(
            U05AdmissionService admissionService,
            U05ClinicalReadinessPolicy readinessPolicy,
            U05ReadinessStateProposalFactory proposalFactory,
            U05CommitService commitService,
            U05RoutingCurrentnessPort currentnessPort,
            U05RoutingService routingService) {
        if (admissionService == null || readinessPolicy == null || proposalFactory == null
                || commitService == null || currentnessPort == null || routingService == null) {
            throw new IllegalArgumentException("all U05 services are required");
        }
        this.admissionService = admissionService;
        this.readinessPolicy = readinessPolicy;
        this.proposalFactory = proposalFactory;
        this.commitService = commitService;
        this.currentnessPort = currentnessPort;
        this.routingService = routingService;
    }

    public U05ExecutionResult execute(
            U05ConsumerInboundRequest request,
            U05ReadinessInputManifest manifest,
            U05AdmissionAuthoritySnapshot authority) {
        U05AdmissionResult admission = admissionService.admit(request, manifest, authority);
        if (!admission.isAdmitted()) {
            return new U05ExecutionResult(
                    U05ExecutionResult.ADMISSION_REJECTED,
                    admission, null, null, null, null, null, null);
        }

        U05AdmittedInput input = admission.getAdmittedInput();
        U05ClinicalReadinessDecision decision;
        try {
            decision = readinessPolicy.decide(input);
        } catch (U05PolicyExpectationGapException gap) {
            return new U05ExecutionResult(
                    U05ExecutionResult.POLICY_EXPECTATION_GAP,
                    admission, null, null, null, null, null, gap.getSentinelCode());
        }

        if (U05ClinicalReadinessDecision.INPUT_FAILURE.equals(decision.getDecisionStatus())) {
            return new U05ExecutionResult(
                    U05ExecutionResult.D03_INPUT_FAILURE,
                    admission, decision, null, null, null, null, null);
        }
        if (U05ClinicalReadinessDecision.INPUT_CONFLICT.equals(decision.getDecisionStatus())) {
            return new U05ExecutionResult(
                    U05ExecutionResult.D03_INPUT_CONFLICT,
                    admission, decision, null, null, null, null, null);
        }

        U05ReadinessStateProposal proposal = proposalFactory.create(input, decision);
        StateTypes.CommitResult commit = commitService.commitNonProduction(input, decision, proposal);
        if (!"COMMITTED".equals(commit.status)) {
            return new U05ExecutionResult(
                    U05ExecutionResult.COMMIT_NOT_COMPLETE,
                    admission, decision, proposal, commit, null, null, null);
        }

        U05ClinicalReadinessCommitEvidence commitEvidence =
                commitService.verifyCommittedReadBack(proposal, commit);
        U05RoutingCurrentness currentness =
                currentnessPort.inspect(input, decision, commitEvidence);
        U05DownstreamRoutingDecision routing =
                routingService.route(input, decision, commitEvidence, currentness);

        return new U05ExecutionResult(
                U05ExecutionResult.ROUTING_COMPLETE,
                admission, decision, proposal, commit, commitEvidence, routing, null);
    }
}
