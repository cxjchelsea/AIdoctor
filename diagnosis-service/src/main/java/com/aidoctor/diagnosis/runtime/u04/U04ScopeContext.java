package com.aidoctor.diagnosis.runtime.u04;

/** Governed U04 V1 non-production policy/scope context. */
public final class U04ScopeContext {
    public static final String FROZEN_POLICY_REF = "U04-SAFETY-GATE-POLICY-V0.1-FROZEN";

    private final boolean established;
    private final boolean applicable;
    private final String policyRef;

    public U04ScopeContext(boolean established, boolean applicable, String policyRef) {
        this.established = established;
        this.applicable = applicable;
        this.policyRef = exact(policyRef, "policyRef");
    }

    public static U04ScopeContext current(boolean established, boolean applicable) {
        return new U04ScopeContext(established, applicable, FROZEN_POLICY_REF);
    }

    public boolean isEstablished() { return established; }
    public boolean isApplicable() { return applicable; }
    public String getPolicyRef() { return policyRef; }

    public void requireFrozenPolicy() {
        if (!FROZEN_POLICY_REF.equals(policyRef)) {
            throw new IllegalStateException("U04 policy ref is not authorized for the frozen V1 slice");
        }
    }

    private static String exact(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        String normalized = value.trim().toLowerCase();
        if ("latest".equals(normalized)
                || "current".equals(normalized)
                || "newest".equals(normalized)
                || normalized.endsWith("@latest")
                || normalized.endsWith("@current")
                || normalized.endsWith("@newest")) {
            throw new IllegalArgumentException(name + " must be an exact governed ref");
        }
        return value;
    }
}
