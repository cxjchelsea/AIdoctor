package com.aidoctor.diagnosis.runtime.u01.capability;

/** Fail-closed C01 invocation/contract failure. Must not be mapped to a clinical negative result. */
public class C01CapabilityException extends RuntimeException {
    private final String reasonCode;
    private final boolean retryable;

    public C01CapabilityException(String reasonCode, boolean retryable, String message) {
        super(message);
        this.reasonCode = reasonCode;
        this.retryable = retryable;
    }

    public C01CapabilityException(String reasonCode, boolean retryable, String message, Throwable cause) {
        super(message, cause);
        this.reasonCode = reasonCode;
        this.retryable = retryable;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
