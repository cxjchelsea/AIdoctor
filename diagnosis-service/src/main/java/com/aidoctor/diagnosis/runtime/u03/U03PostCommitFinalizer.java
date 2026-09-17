package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;

/**
 * Operational post-commit finalizer for the authorized CD-07 non-production path.
 *
 * <p>The canonical commit has already happened before this boundary. Trace failure
 * therefore must never roll back, rewrite, or reinterpret Clinical State. Instead,
 * normal S14 delivery is withheld and an operational reconciliation result is
 * returned. This class owns no clinical semantics and performs no U04 execution.</p>
 */
public final class U03PostCommitFinalizer {
    private final U03RuntimeTraceService traceService;
    private final U03OutboundProducer outboundProducer;

    public U03PostCommitFinalizer(
            U03RuntimeTraceService traceService,
            U03OutboundProducer outboundProducer) {
        if (traceService == null) throw new IllegalArgumentException("traceService is required");
        if (outboundProducer == null) throw new IllegalArgumentException("outboundProducer is required");
        this.traceService = traceService;
        this.outboundProducer = outboundProducer;
    }

    public U03PostCommitFinalizationOutcome finalizeCommittedExecution(
            U03NonProductionExecutionContext context,
            U03GovernedCandidateGateway.GovernedResult governed,
            U03DecisionOutcome decision,
            U03StateProposal proposal,
            StateTypes.CommitResult commitResult) {
        if (commitResult == null) throw new IllegalArgumentException("commitResult is required");

        U03RuntimeTraceWriteOutcome traceOutcome = traceService.record(
                context, governed, decision, proposal, commitResult);
        if (!traceOutcome.isRecorded()) {
            return U03PostCommitFinalizationOutcome.reconciliationRequired(traceOutcome);
        }

        U03OutboundHandoff handoff = outboundProducer.produce(
                context, governed, decision, proposal, commitResult);
        return U03PostCommitFinalizationOutcome.deliverable(traceOutcome, handoff);
    }
}
