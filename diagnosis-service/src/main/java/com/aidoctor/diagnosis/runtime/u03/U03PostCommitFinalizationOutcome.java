package com.aidoctor.diagnosis.runtime.u03;

/**
 * Operational-only post-commit finalization result.
 *
 * <p>This type does not represent clinical truth or disposition. A reconciliation
 * requirement means the canonical commit result must remain untouched while the
 * operational trace persistence problem is handled separately.</p>
 */
public final class U03PostCommitFinalizationOutcome {
    public static final String DELIVERABLE = "DELIVERABLE";
    public static final String RECONCILIATION_REQUIRED = "RECONCILIATION_REQUIRED";

    private final String status;
    private final U03RuntimeTraceWriteOutcome traceOutcome;
    private final U03OutboundHandoff outboundHandoff;

    private U03PostCommitFinalizationOutcome(
            String status,
            U03RuntimeTraceWriteOutcome traceOutcome,
            U03OutboundHandoff outboundHandoff) {
        this.status = status;
        this.traceOutcome = traceOutcome;
        this.outboundHandoff = outboundHandoff;
    }

    public static U03PostCommitFinalizationOutcome deliverable(
            U03RuntimeTraceWriteOutcome traceOutcome,
            U03OutboundHandoff outboundHandoff) {
        if (traceOutcome == null || !traceOutcome.isRecorded()) {
            throw new IllegalArgumentException("deliverable finalization requires recorded trace");
        }
        if (outboundHandoff == null) {
            throw new IllegalArgumentException("deliverable finalization requires outbound handoff");
        }
        return new U03PostCommitFinalizationOutcome(DELIVERABLE, traceOutcome, outboundHandoff);
    }

    public static U03PostCommitFinalizationOutcome reconciliationRequired(
            U03RuntimeTraceWriteOutcome traceOutcome) {
        if (traceOutcome == null || traceOutcome.isRecorded()) {
            throw new IllegalArgumentException("reconciliation requires failed trace persistence");
        }
        return new U03PostCommitFinalizationOutcome(RECONCILIATION_REQUIRED, traceOutcome, null);
    }

    public String getStatus() { return status; }
    public U03RuntimeTraceWriteOutcome getTraceOutcome() { return traceOutcome; }
    public U03OutboundHandoff getOutboundHandoff() { return outboundHandoff; }
    public boolean requiresReconciliation() { return RECONCILIATION_REQUIRED.equals(status); }
}
