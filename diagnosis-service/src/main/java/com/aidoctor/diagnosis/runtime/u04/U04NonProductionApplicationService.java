package com.aidoctor.diagnosis.runtime.u04;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.u03.U03OutboundHandoff;

/**
 * Authorized non-production U04 vertical slice.
 *
 * <p>This service stops at committed Safety Gate + downstream eligibility. It does
 * not call U05, U11 or U14 and does not activate production routing.</p>
 */
public final class U04NonProductionApplicationService {
    private final U04AdmissionService admissionService;
    private final U04SafetyGatePolicy policy;
    private final U04StateProposalFactory proposalFactory;
    private final U04CommitService commitService;

    public U04NonProductionApplicationService(
            U04AdmissionService admissionService,
            U04SafetyGatePolicy policy,
            U04StateProposalFactory proposalFactory,
            U04CommitService commitService) {
        if (admissionService == null || policy == null || proposalFactory == null || commitService == null) {
            throw new IllegalArgumentException("U04 services are required");
        }
        this.admissionService = admissionService;
        this.policy = policy;
        this.proposalFactory = proposalFactory;
        this.commitService = commitService;
    }

    public U04ExecutionResult execute(
            U03OutboundHandoff handoff,
            int currentClinicalStateVersion,
            U04ScopeContext scopeContext,
            String environmentId) {
        U04AdmissionResult admission = admissionService.admit(
                handoff,
                currentClinicalStateVersion,
                scopeContext,
                environmentId);
        if (admission.isFailed()) {
            return new U04ExecutionResult(
                    U04ExecutionResult.ADMISSION_FAILED,
                    admission,
                    null,
                    null,
                    null,
                    null);
        }

        U04AdmittedInput input = admission.getAdmittedInput();
        U04SafetyGateDecision decision = policy.decide(input);
        U04StateProposal proposal = proposalFactory.create(input, decision);
        StateTypes.CommitResult commit = commitService.commitNonProduction(input, decision, proposal);

        if (!"COMMITTED".equals(commit.status)) {
            return new U04ExecutionResult(
                    U04ExecutionResult.COMMIT_NOT_COMPLETE,
                    admission,
                    decision,
                    proposal,
                    commit,
                    null);
        }

        return new U04ExecutionResult(
                U04ExecutionResult.COMMIT_COMPLETE,
                admission,
                decision,
                proposal,
                commit,
                U04RoutingEligibility.from(decision));
    }
}
