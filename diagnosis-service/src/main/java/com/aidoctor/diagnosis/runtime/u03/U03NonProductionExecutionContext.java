package com.aidoctor.diagnosis.runtime.u03;

/**
 * Immutable CD-07 execution binding for controlled non-production runtime use.
 *
 * <p>The context binds one U03 execution identity (consultation / Thread / Run /
 * Event / Clinical State Version), accepted evidence, and the exact Gate-C-frozen
 * governed release set. It is not a production activation mechanism and carries
 * no clinical mutation authority.</p>
 */
public final class U03NonProductionExecutionContext {
    public static final String BINDING_MODE = "EXPLICIT_NON_PRODUCTION_BINDING_ONLY";

    private final U03ExecutionCommand command;
    private final U03ExplicitNonProductionReleaseRefs releaseRefs;
    private final U03AcceptedEvidenceBinding acceptedEvidenceBinding;
    private final String environmentId;

    /**
     * Compatibility constructor for pre-S5 tests/setup only. The explicit CD-07
     * gateway fails closed until an accepted-evidence binding is supplied.
     */
    public U03NonProductionExecutionContext(
            U03ExecutionCommand command,
            U03ExplicitNonProductionReleaseRefs releaseRefs,
            String environmentId) {
        this(command, releaseRefs, null, environmentId);
    }

    public U03NonProductionExecutionContext(
            U03ExecutionCommand command,
            U03ExplicitNonProductionReleaseRefs releaseRefs,
            U03AcceptedEvidenceBinding acceptedEvidenceBinding,
            String environmentId) {
        if (command == null) throw new IllegalArgumentException("command is required");
        if (releaseRefs == null) throw new IllegalArgumentException("releaseRefs are required");
        this.environmentId = requireNonProductionEnvironment(environmentId);
        releaseRefs.requireGateCFrozenSet();
        this.command = command;
        this.releaseRefs = releaseRefs;
        this.acceptedEvidenceBinding = acceptedEvidenceBinding;
        if (acceptedEvidenceBinding != null
                && acceptedEvidenceBinding.getClinicalStateVersion() != command.clinicalStateVersion) {
            throw new IllegalStateException(
                    "accepted evidence is not bound to the execution Clinical State Version");
        }
    }

    public U03ExecutionCommand getCommand() { return command; }
    public U03ExplicitNonProductionReleaseRefs getReleaseRefs() { return releaseRefs; }
    public U03AcceptedEvidenceBinding getAcceptedEvidenceBinding() { return acceptedEvidenceBinding; }
    public String getEnvironmentId() { return environmentId; }
    public String getBindingMode() { return BINDING_MODE; }

    public U03AcceptedEvidenceBinding requireAcceptedEvidenceBinding() {
        if (acceptedEvidenceBinding == null) {
            throw new IllegalStateException("accepted evidence binding is required for CD-07 runtime");
        }
        if (acceptedEvidenceBinding.getClinicalStateVersion() != command.clinicalStateVersion) {
            throw new IllegalStateException(
                    "accepted evidence is stale for the execution Clinical State Version");
        }
        return acceptedEvidenceBinding;
    }

    private static String requireNonProductionEnvironment(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("environmentId is required");
        }
        String normalized = value.trim().toLowerCase();
        if ("prod".equals(normalized)
                || "production".equals(normalized)
                || normalized.startsWith("prod-")
                || normalized.startsWith("production-")) {
            throw new IllegalStateException("production environment is not authorized for CD-07 runtime");
        }
        return value;
    }
}
