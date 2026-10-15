package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.u03.*;
import com.aidoctor.diagnosis.runtime.u04.*;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reusable non-production U03 -> P01 -> S14 -> U04 physical continuation.
 * The caller must first obtain a trusted U03 admission from U02/P05 evidence
 * and arrange its own durable Run/Event authority. This component never
 * issues accepted evidence or creates a clinical Run.
 */
public final class U07NonProductionGovernedRiskSafetyContinuation {
    private final U03GovernedCandidateGateway gateway;
    private final U03DecisionService decisions;
    private final U03StateProposalFactory proposals;
    private final U03CommitService riskCommitter;
    private final U03PostCommitFinalizer finalizer;
    private final U03OutboundProducer outbound;
    private final U02U03GovernedSafetyBridge safety;
    private final TransactionTemplate transactions;

    public U07NonProductionGovernedRiskSafetyContinuation(
            U03GovernedCandidateGateway gateway, U03DecisionService decisions,
            U03StateProposalFactory proposals, U03CommitService riskCommitter,
            U03PostCommitFinalizer finalizer, U03OutboundProducer outbound,
            U02U03GovernedSafetyBridge safety, TransactionTemplate transactions) {
        if (gateway == null || decisions == null || proposals == null
                || riskCommitter == null || finalizer == null || outbound == null
                || safety == null || transactions == null)
            throw new IllegalArgumentException("Governed risk continuation dependencies required");
        this.gateway = gateway;
        this.decisions = decisions;
        this.proposals = proposals;
        this.riskCommitter = riskCommitter;
        this.finalizer = finalizer;
        this.outbound = outbound;
        this.safety = safety;
        this.transactions = transactions;
    }

    public Result execute(U03NonProductionExecutionContext admitted, U04ScopeContext scope) {
        if (admitted == null || scope == null)
            throw new IllegalArgumentException("Accepted U03 admission and U04 scope required");
        admitted.requireAcceptedEvidenceBinding();
        U03GovernedCandidateGateway.GovernedResult governed = gateway.assess(admitted);
        U03DecisionOutcome decision = decisions.decide(admitted, governed);
        U03OutboundHandoff handoff;
        StateTypes.CommitResult riskCommit = null;
        if (U03RiskAssessmentCandidate.FAILED.equals(decision.getStatus())) {
            // Failed assessment is never converted into a successful risk commit.
            handoff = outbound.produce(admitted, governed, decision, null, null);
        } else if (U03RiskAssessmentCandidate.VALID.equals(decision.getStatus())) {
            U03StateProposal proposal = proposals.createNonProductionValid(admitted, decision, governed);
            riskCommit = transactions.execute(status -> riskCommitter.commitNonProduction(admitted, proposal));
            if (riskCommit == null || !"COMMITTED".equals(riskCommit.status))
                throw new IllegalStateException("V1_U03_RISK_COMMIT_NOT_COMPLETE");
            U03PostCommitFinalizationOutcome finalized =
                    finalizer.finalizeCommittedExecution(admitted, governed, decision, proposal, riskCommit);
            if (!U03PostCommitFinalizationOutcome.DELIVERABLE.equals(finalized.getStatus())
                    || finalized.getOutboundHandoff() == null)
                throw new IllegalStateException("V1_U03_TRACE_RECONCILIATION_REQUIRED");
            handoff = finalized.getOutboundHandoff();
        } else {
            throw new IllegalStateException("V1_U03_UNKNOWN_DECISION_STATUS");
        }

        U04ExecutionResult gate = transactions.execute(status ->
                safety.applyU04(handoff, scope, admitted.getEnvironmentId()));
        if (gate == null || !U04ExecutionResult.COMMIT_COMPLETE.equals(gate.getStatus())
                || gate.getCommitResult() == null
                || !"COMMITTED".equals(gate.getCommitResult().status))
            throw new IllegalStateException("V1_U04_GATE_COMMIT_NOT_COMPLETE");
        if (U03RiskAssessmentCandidate.FAILED.equals(decision.getStatus())
                && (gate.getDecision() == null
                    || !U04SafetyGateDecision.UNAVAILABLE.equals(gate.getDecision().getGate())))
            throw new IllegalStateException("V1_U03_FAILED_MUST_BE_UNAVAILABLE");
        return new Result(decision, riskCommit, handoff, gate);
    }

    public static final class Result {
        public final U03DecisionOutcome risk;
        public final StateTypes.CommitResult riskCommit;
        public final U03OutboundHandoff handoff;
        public final U04ExecutionResult gate;
        private Result(U03DecisionOutcome risk, StateTypes.CommitResult riskCommit,
                U03OutboundHandoff handoff, U04ExecutionResult gate) {
            this.risk = risk;
            this.riskCommit = riskCommit;
            this.handoff = handoff;
            this.gate = gate;
        }
    }
}
