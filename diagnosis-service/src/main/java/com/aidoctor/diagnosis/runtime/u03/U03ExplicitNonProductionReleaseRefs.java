package com.aidoctor.diagnosis.runtime.u03;

/**
 * Exact governed release identities supplied by the controlled non-production
 * CD-07 wiring. These refs do not publish or activate any clinical release.
 */
public final class U03ExplicitNonProductionReleaseRefs {
    private final String knowledgeReleaseRef;
    private final String ruleReleaseRef;
    private final String coverageContractRef;
    private final String policyReleaseRef;
    private final String policyPairRef;

    public U03ExplicitNonProductionReleaseRefs(
            String knowledgeReleaseRef,
            String ruleReleaseRef,
            String coverageContractRef,
            String policyReleaseRef,
            String policyPairRef) {
        this.knowledgeReleaseRef = exact(knowledgeReleaseRef, "knowledgeReleaseRef");
        this.ruleReleaseRef = exact(ruleReleaseRef, "ruleReleaseRef");
        this.coverageContractRef = exact(coverageContractRef, "coverageContractRef");
        this.policyReleaseRef = exact(policyReleaseRef, "policyReleaseRef");
        this.policyPairRef = exact(policyPairRef, "policyPairRef");
    }

    public String getKnowledgeReleaseRef() { return knowledgeReleaseRef; }
    public String getRuleReleaseRef() { return ruleReleaseRef; }
    public String getCoverageContractRef() { return coverageContractRef; }
    public String getPolicyReleaseRef() { return policyReleaseRef; }
    public String getPolicyPairRef() { return policyPairRef; }

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
            throw new IllegalArgumentException(name + " must be an exact governed ref, not an alias");
        }
        return value;
    }
}
