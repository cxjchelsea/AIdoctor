package com.aidoctor.diagnosis.runtime.u05;

/** Verification/design sentinel. It is intentionally not a D03 runtime status. */
public final class U05PolicyExpectationGapException extends IllegalStateException {
    private final String sentinelCode;

    public U05PolicyExpectationGapException(String sentinelCode, String message) {
        super(message);
        this.sentinelCode = sentinelCode;
    }

    public String getSentinelCode() { return sentinelCode; }
}
