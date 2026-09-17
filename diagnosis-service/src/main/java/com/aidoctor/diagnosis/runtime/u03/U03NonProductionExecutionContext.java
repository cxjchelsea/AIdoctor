package com.aidoctor.diagnosis.runtime.u03;

/**
 * Immutable CD-07 execution binding for controlled non-production runtime use.
 *
 * <p>The context binds one U03 execution identity (consultation / Thread / Run /
 * Event / Clinical State Version) to the exact Gate-C-frozen governed release
 * set. It is not a production activation mechanism and carries no clinical
 * mutation authority.</p>
 */
public final class U03NonProductionExecutionContext {
    public static final String BINDING_MODE = "EXPLICIT_NON_PRODUCTION_BINDING_ONLY";

    private final U03ExecutionCommand command;
    private final U03ExplicitNonProductionReleaseRefs releaseRefs;
    private final String environmentId;

    public U03NonProductionExecutionContext(
            U03ExecutionCommand command,
            U03ExplicitNonProductionReleaseRefs releaseRefs,
            String environmentId) {
        if (command == null) throw new IllegalArgumentException("command is required");
        if (releaseRefs == null) throw new IllegalArgumentException("releaseRefs are required");
        this.environmentId = requireNonProductionEnvironment(environmentId);
        releaseRefs.requireGateCFrozenSet();
        this.command = command;
        this.releaseRefs = releaseRefs;
    }

    public U03ExecutionCommand getCommand() { return command; }
    public U03ExplicitNonProductionReleaseRefs getReleaseRefs() { return releaseRefs; }
    public String getEnvironmentId() { return environmentId; }
    public String getBindingMode() { return BINDING_MODE; }

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
