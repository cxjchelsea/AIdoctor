package com.aidoctor.diagnosis.runtime.u05;

/** Typed U05 RDP-01 admission result. */
public final class U05AdmissionResult {
    public static final String ADMITTED = "ADMITTED";
    public static final String REJECTED = "REJECTED";
    public static final String ORIGINAL = "ORIGINAL";
    public static final String REATTACHED = "REATTACHED";

    private final String admissionStatus;
    private final String reasonCode;
    private final String replayDisposition;
    private final String restrictedPermissionRef;
    private final U05AdmittedInput admittedInput;

    private U05AdmissionResult(
            String admissionStatus,
            String reasonCode,
            String replayDisposition,
            String restrictedPermissionRef,
            U05AdmittedInput admittedInput) {
        this.admissionStatus = admissionStatus;
        this.reasonCode = reasonCode;
        this.replayDisposition = replayDisposition;
        this.restrictedPermissionRef = restrictedPermissionRef;
        this.admittedInput = admittedInput;
    }

    public static U05AdmissionResult admitted(U05AdmittedInput input, String replayDisposition) {
        if (input == null) throw new IllegalArgumentException("admitted input is required");
        if (!ORIGINAL.equals(replayDisposition) && !REATTACHED.equals(replayDisposition)) {
            throw new IllegalArgumentException("unsupported replayDisposition");
        }
        return new U05AdmissionResult(
                ADMITTED,
                null,
                replayDisposition,
                input.getAcceptedRestrictedPermissionRef(),
                input);
    }

    public static U05AdmissionResult rejected(String reasonCode) {
        if (reasonCode == null || reasonCode.trim().isEmpty()) throw new IllegalArgumentException("reasonCode is required");
        return new U05AdmissionResult(REJECTED, reasonCode, null, null, null);
    }

    public String getAdmissionStatus() { return admissionStatus; }
    public String getReasonCode() { return reasonCode; }
    public String getReplayDisposition() { return replayDisposition; }
    public String getRestrictedPermissionRef() { return restrictedPermissionRef; }
    public U05AdmittedInput getAdmittedInput() { return admittedInput; }
    public boolean isAdmitted() { return ADMITTED.equals(admissionStatus); }
}
