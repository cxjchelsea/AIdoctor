package com.aidoctor.diagnosis.runtime.u03;

/**
 * Typed result of the controlled non-production U03 admission boundary.
 *
 * <p>This value is intentionally pre-C02 and pre-D09. FAILED here is a
 * governance/admission result, not a clinical policy decision and not a
 * Clinical State mutation instruction.</p>
 */
public final class U03NonProductionAdmissionResult {
    public static final String ACCEPTED = "ACCEPTED";
    public static final String FAILED = "FAILED";

    private final String status;
    private final String reasonCode;
    private final String dispositionCode;
    private final String boundary;
    private final U03NonProductionExecutionContext context;
    private final U03ExecutionCommand command;
    private final U03ExplicitNonProductionReleaseRefs attemptedReleaseRefs;

    private U03NonProductionAdmissionResult(
            String status,
            String reasonCode,
            String dispositionCode,
            String boundary,
            U03NonProductionExecutionContext context,
            U03ExecutionCommand command,
            U03ExplicitNonProductionReleaseRefs attemptedReleaseRefs) {
        this.status = required(status, "status");
        this.reasonCode = reasonCode;
        this.dispositionCode = dispositionCode;
        this.boundary = required(boundary, "boundary");
        this.context = context;
        this.command = command;
        this.attemptedReleaseRefs = attemptedReleaseRefs;

        if (ACCEPTED.equals(status)) {
            if (context == null || reasonCode != null || dispositionCode != null) {
                throw new IllegalArgumentException("ACCEPTED admission must carry context only");
            }
        } else if (FAILED.equals(status)) {
            if (context != null || reasonCode == null || dispositionCode != null) {
                throw new IllegalArgumentException(
                        "FAILED admission must carry reason, no context, and no normal disposition");
            }
        } else {
            throw new IllegalArgumentException("unsupported admission status: " + status);
        }
    }

    public static U03NonProductionAdmissionResult accepted(
            U03NonProductionExecutionContext context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        return new U03NonProductionAdmissionResult(
                ACCEPTED,
                null,
                null,
                "PRE_C02_ADMISSION",
                context,
                context.getCommand(),
                context.getReleaseRefs());
    }

    public static U03NonProductionAdmissionResult failed(
            U03ExecutionCommand command,
            U03ExplicitNonProductionReleaseRefs attemptedReleaseRefs,
            String reasonCode) {
        return new U03NonProductionAdmissionResult(
                FAILED,
                required(reasonCode, "reasonCode"),
                null,
                "PRE_C02_ADMISSION",
                null,
                command,
                attemptedReleaseRefs);
    }

    public String getStatus() { return status; }
    public String getReasonCode() { return reasonCode; }
    public String getDispositionCode() { return dispositionCode; }
    public String getBoundary() { return boundary; }
    public U03NonProductionExecutionContext getContext() { return context; }
    public U03ExecutionCommand getCommand() { return command; }
    public U03ExplicitNonProductionReleaseRefs getAttemptedReleaseRefs() {
        return attemptedReleaseRefs;
    }
    public boolean isAccepted() { return ACCEPTED.equals(status); }
    public boolean isFailed() { return FAILED.equals(status); }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
