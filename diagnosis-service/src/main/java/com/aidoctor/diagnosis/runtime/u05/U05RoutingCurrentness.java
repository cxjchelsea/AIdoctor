package com.aidoctor.diagnosis.runtime.u05;

/** Authoritative post-commit currentness snapshot consumed by RDP-04 projection. */
public final class U05RoutingCurrentness {
    private final int currentClinicalStateVersion;
    private final String routingContextRef;
    private final boolean readinessCurrent;
    private final boolean readinessDependenciesCurrent;
    private final boolean gateCurrent;
    private final boolean inboundProvenanceCurrent;
    private final boolean restrictedContextCurrent;
    private final String currentU04GateRef;
    private final String gateValue;
    private final String restrictedContextRef;

    public U05RoutingCurrentness(
            int currentClinicalStateVersion,
            String routingContextRef,
            boolean readinessCurrent,
            boolean readinessDependenciesCurrent,
            boolean gateCurrent,
            boolean inboundProvenanceCurrent,
            boolean restrictedContextCurrent,
            String currentU04GateRef,
            String gateValue,
            String restrictedContextRef) {
        if (currentClinicalStateVersion < 0) throw new IllegalArgumentException("currentClinicalStateVersion must be non-negative");
        this.currentClinicalStateVersion = currentClinicalStateVersion;
        this.routingContextRef = required(routingContextRef, "routingContextRef");
        this.readinessCurrent = readinessCurrent;
        this.readinessDependenciesCurrent = readinessDependenciesCurrent;
        this.gateCurrent = gateCurrent;
        this.inboundProvenanceCurrent = inboundProvenanceCurrent;
        this.restrictedContextCurrent = restrictedContextCurrent;
        this.currentU04GateRef = required(currentU04GateRef, "currentU04GateRef");
        this.gateValue = required(gateValue, "gateValue");
        this.restrictedContextRef = restrictedContextRef;
    }

    public int getCurrentClinicalStateVersion() { return currentClinicalStateVersion; }
    public String getRoutingContextRef() { return routingContextRef; }
    public boolean isReadinessCurrent() { return readinessCurrent; }
    public boolean isReadinessDependenciesCurrent() { return readinessDependenciesCurrent; }
    public boolean isGateCurrent() { return gateCurrent; }
    public boolean isInboundProvenanceCurrent() { return inboundProvenanceCurrent; }
    public boolean isRestrictedContextCurrent() { return restrictedContextCurrent; }
    public String getCurrentU04GateRef() { return currentU04GateRef; }
    public String getGateValue() { return gateValue; }
    public String getRestrictedContextRef() { return restrictedContextRef; }

    public boolean ordinaryRouteCurrentFor(U05ClinicalReadinessCommitEvidence evidence) {
        return evidence != null
                && currentClinicalStateVersion >= evidence.getCommittedClinicalStateVersion()
                && readinessCurrent
                && readinessDependenciesCurrent
                && gateCurrent
                && inboundProvenanceCurrent
                && (!U05ConsumerInboundRequest.GATE_RESTRICTED.equals(gateValue) || restrictedContextCurrent);
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
