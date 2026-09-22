package com.aidoctor.diagnosis.runtime.u03;

/** Operational-only P05 write outcome. It is not a clinical result. */
public final class U03RuntimeTraceWriteOutcome {
    public static final String RECORDED = "RECORDED";
    public static final String FAILED = "FAILED";
    public static final String TRACE_PERSISTENCE_FAILED = "U03_TRACE_PERSISTENCE_FAILED";

    private final String status;
    private final String failureCode;

    private U03RuntimeTraceWriteOutcome(String status, String failureCode) {
        this.status = status;
        this.failureCode = failureCode;
    }

    public static U03RuntimeTraceWriteOutcome recorded() {
        return new U03RuntimeTraceWriteOutcome(RECORDED, null);
    }

    public static U03RuntimeTraceWriteOutcome failed() {
        return new U03RuntimeTraceWriteOutcome(FAILED, TRACE_PERSISTENCE_FAILED);
    }

    public String getStatus() { return status; }
    public String getFailureCode() { return failureCode; }
    public boolean isRecorded() { return RECORDED.equals(status); }
}
