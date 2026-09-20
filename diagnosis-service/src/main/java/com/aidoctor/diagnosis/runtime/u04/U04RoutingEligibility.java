package com.aidoctor.diagnosis.runtime.u04;

/**
 * Typed downstream eligibility only.
 *
 * <p>No U05/U11/U14 owner execution or live routing occurs in this type.</p>
 */
public final class U04RoutingEligibility {
    private final boolean u05Eligible;
    private final boolean restrictedContextRequired;
    private final boolean u11Eligible;
    private final boolean u14Eligible;

    private U04RoutingEligibility(
            boolean u05Eligible,
            boolean restrictedContextRequired,
            boolean u11Eligible,
            boolean u14Eligible) {
        this.u05Eligible = u05Eligible;
        this.restrictedContextRequired = restrictedContextRequired;
        this.u11Eligible = u11Eligible;
        this.u14Eligible = u14Eligible;
    }

    public static U04RoutingEligibility from(U04SafetyGateDecision decision) {
        if (decision == null) throw new IllegalArgumentException("decision is required");
        if (U04SafetyGateDecision.ALLOW.equals(decision.getGate())) {
            return new U04RoutingEligibility(true, false, false, false);
        }
        if (U04SafetyGateDecision.RESTRICTED.equals(decision.getGate())) {
            return new U04RoutingEligibility(true, true, false, false);
        }
        if (U04SafetyGateDecision.BLOCKED.equals(decision.getGate())) {
            return new U04RoutingEligibility(false, false, true, false);
        }
        if (U04SafetyGateDecision.UNAVAILABLE.equals(decision.getGate())) {
            return new U04RoutingEligibility(false, false, false, true);
        }
        throw new IllegalStateException("SAFETY_POLICY_EXPECTATION_GAP: unsupported gate");
    }

    public boolean isU05Eligible() { return u05Eligible; }
    public boolean isRestrictedContextRequired() { return restrictedContextRequired; }
    public boolean isU11Eligible() { return u11Eligible; }
    public boolean isU14Eligible() { return u14Eligible; }
}
