package com.aidoctor.diagnosis.runtime.u04;

/** Typed pre-decision U04 consumer admission result. */
public final class U04AdmissionResult {
    public static final String ACCEPTED = "ACCEPTED";
    public static final String FAILED = "FAILED";
    public static final String BOUNDARY = "U04_CONSUMER_ADMISSION";

    private final String status;
    private final String reasonCode;
    private final U04AdmittedInput admittedInput;

    private U04AdmissionResult(String status, String reasonCode, U04AdmittedInput admittedInput) {
        this.status = required(status, "status");
        this.reasonCode = reasonCode;
        this.admittedInput = admittedInput;
        if (ACCEPTED.equals(status)) {
            if (admittedInput == null || reasonCode != null) {
                throw new IllegalArgumentException("accepted admission must carry input and no failure reason");
            }
        } else if (FAILED.equals(status)) {
            if (admittedInput != null || reasonCode == null) {
                throw new IllegalArgumentException("failed admission must carry reason and no admitted input");
            }
        } else {
            throw new IllegalArgumentException("unsupported admission status");
        }
    }

    public static U04AdmissionResult accepted(U04AdmittedInput input) {
        return new U04AdmissionResult(ACCEPTED, null, input);
    }

    public static U04AdmissionResult failed(String reasonCode) {
        return new U04AdmissionResult(FAILED, required(reasonCode, "reasonCode"), null);
    }

    public String getStatus() { return status; }
    public String getReasonCode() { return reasonCode; }
    public U04AdmittedInput getAdmittedInput() { return admittedInput; }
    public String getBoundary() { return BOUNDARY; }
    public boolean isAccepted() { return ACCEPTED.equals(status); }
    public boolean isFailed() { return FAILED.equals(status); }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
