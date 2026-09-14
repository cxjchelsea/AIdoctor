package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.governance.CapabilityTraceService;

import java.util.UUID;

/**
 * U03 component path. Both VALID and FAILED assessment states are committed for the
 * exact input Clinical State Version. Routing to U04 is intentionally outside this class.
 */
public final class U03RiskAssessmentApplicationService {
    private final U03GovernedCandidateGateway gateway;
    private final U03DecisionService decisionService;
    private final U03StateProposalFactory proposalFactory;
    private final U03CommitService commitService;
    private final CapabilityTraceService traceService;

    public U03RiskAssessmentApplicationService(
            U03GovernedCandidateGateway gateway,
            U03DecisionService decisionService,
            U03StateProposalFactory proposalFactory,
            U03CommitService commitService,
            CapabilityTraceService traceService) {
        this.gateway = required(gateway, "gateway");
        this.decisionService = required(decisionService, "decisionService");
        this.proposalFactory = required(proposalFactory, "proposalFactory");
        this.commitService = required(commitService, "commitService");
        this.traceService = required(traceService, "traceService");
    }

    public U03ExecutionResult execute(U03ExecutionCommand command) {
        if (command == null) throw new IllegalArgumentException("command is required");
        String capabilityCallId = "c02-u03-call-" + UUID.randomUUID().toString();
        traceService.start(
                capabilityCallId,
                command.consultationId,
                command.threadId,
                command.runId,
                command.eventId,
                "U03",
                U03GovernedCandidateGateway.CAPABILITY_ID,
                U03GovernedCandidateGateway.BINDING_ID,
                Integer.valueOf(command.clinicalStateVersion));

        final U03GovernedCandidateGateway.GovernedResult governed;
        try {
            governed = gateway.assess(command);
        } catch (RuntimeException invocationFailure) {
            return commitExplicitFailure(command, capabilityCallId, "U03_GOVERNED_INVOCATION_FAILED");
        }

        traceService.bindReleases(
                capabilityCallId,
                governed.getReleaseBinding().getRuleReleaseId(),
                governed.getReleaseBinding().getKnowledgeReleaseId());

        U03RiskAssessmentCandidate candidate = governed.getCandidate();
        U03DecisionOutcome decision = decisionService.decide(command, candidate, governed.getReleaseBinding());
        U03StateProposal proposal = candidate.isFailed()
                ? proposalFactory.createFailed(command, decision, governed)
                : proposalFactory.createValid(command, decision, governed);
        StateTypes.CommitResult commitResult = commitService.commit(proposal);

        traceService.succeed(
                capabilityCallId,
                "c02-u03-result-" + command.eventId,
                decision.getDecisionId(),
                proposal.getProposalId(),
                commitResult == null ? null : commitResult.patchId,
                committedVersion(commitResult));

        String status = isCommitted(commitResult) ? decision.getStatus() : "COMMIT_NOT_APPLIED";
        return new U03ExecutionResult(status, capabilityCallId, decision, proposal, commitResult);
    }

    private U03ExecutionResult commitExplicitFailure(
            U03ExecutionCommand command,
            String capabilityCallId,
            String reasonCode) {
        U03RiskAssessmentCandidate candidate = U03RiskAssessmentCandidate.failed(reasonCode);
        U03DecisionOutcome decision = decisionService.decide(command, candidate, null);
        U03StateProposal proposal = proposalFactory.createFailed(
                command, decision, U03GovernedCandidateGateway.BINDING_ID);
        StateTypes.CommitResult commitResult = commitService.commit(proposal);
        traceService.failWithOutcome(
                capabilityCallId,
                reasonCode,
                decision.getDecisionId(),
                proposal.getProposalId(),
                commitResult == null ? null : commitResult.patchId,
                committedVersion(commitResult));
        return new U03ExecutionResult(
                isCommitted(commitResult) ? U03RiskAssessmentCandidate.FAILED : "FAILED_COMMIT_NOT_APPLIED",
                capabilityCallId,
                decision,
                proposal,
                commitResult);
    }

    private static boolean isCommitted(StateTypes.CommitResult result) {
        return result != null && "COMMITTED".equals(result.status);
    }

    private static Integer committedVersion(StateTypes.CommitResult result) {
        return result == null ? null : result.committedVersion;
    }

    private static <T> T required(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
