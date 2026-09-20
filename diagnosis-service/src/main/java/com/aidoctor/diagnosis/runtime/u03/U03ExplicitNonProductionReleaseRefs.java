package com.aidoctor.diagnosis.runtime.u03;

/**
 * Exact governed release identities supplied by the controlled non-production
 * CD-07 wiring. These refs do not publish or activate any clinical release.
 */
public final class U03ExplicitNonProductionReleaseRefs {
    public static final String GATE_C_KNOWLEDGE_RELEASE_REF = "KR-U03-SOURCE-001@0.1.0-candidate";
    public static final String GATE_C_RULE_RELEASE_REF = "RR-U03-RISK-001@0.2.1-candidate";
    public static final String GATE_C_COVERAGE_CONTRACT_REF = "U03_D09_COVERAGE_V0_2_1_CANDIDATE";
    public static final String GATE_C_POLICY_RELEASE_REF = "PR-U03-D09-001@0.2.1-candidate";
    public static final String GATE_C_POLICY_PAIR_REF = "PF-U03-C-POLICY-001";

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

    public static U03ExplicitNonProductionReleaseRefs gateCFrozenSet() {
        return new U03ExplicitNonProductionReleaseRefs(
                GATE_C_KNOWLEDGE_RELEASE_REF,
                GATE_C_RULE_RELEASE_REF,
                GATE_C_COVERAGE_CONTRACT_REF,
                GATE_C_POLICY_RELEASE_REF,
                GATE_C_POLICY_PAIR_REF);
    }

    public void requireGateCFrozenSet() {
        requireExactMatch(knowledgeReleaseRef, GATE_C_KNOWLEDGE_RELEASE_REF, "knowledgeReleaseRef");
        requireExactMatch(ruleReleaseRef, GATE_C_RULE_RELEASE_REF, "ruleReleaseRef");
        requireExactMatch(coverageContractRef, GATE_C_COVERAGE_CONTRACT_REF, "coverageContractRef");
        requireExactMatch(policyReleaseRef, GATE_C_POLICY_RELEASE_REF, "policyReleaseRef");
        requireExactMatch(policyPairRef, GATE_C_POLICY_PAIR_REF, "policyPairRef");
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

    private static void requireExactMatch(String actual, String expected, String name) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(name + " is not authorized for CD-07 non-production runtime");
        }
    }
}
