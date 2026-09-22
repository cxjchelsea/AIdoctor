package com.aidoctor.diagnosis.runtime.u02.capability;

/** Fail-closed C01/U02 invocation or contract failure; never a clinical negative result. */
public class C01U02CapabilityException extends RuntimeException {
    private final String reasonCode;
    private final boolean retryable;

    public C01U02CapabilityException(String reasonCode, boolean retryable, String message) {
        super(message);
        this.reasonCode = reasonCode;
        this.retryable = retryable;
    }

    public C01U02CapabilityException(String reasonCode, boolean retryable, String message, Throwable cause) {
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
