package com.aidoctor.diagnosis.runtime.u03;

/**
 * Narrow pre-C02 admission boundary for the authorized CD-08 BF0304 remediation.
 *
 * <p>Only two already-frozen guard failures are normalized here:
 * RELEASE_MISMATCH and STALE_INPUT. All other failures remain exceptional and
 * are not guessed or collapsed into governed reason codes.</p>
 */
public final class U03NonProductionAdmissionService {
    public static final String STALE_INPUT = "STALE_INPUT";
    public static final String RELEASE_MISMATCH = "RELEASE_MISMATCH";

    public U03NonProductionAdmissionResult admit(
            U03ExecutionCommand command,
            U03ExplicitNonProductionReleaseRefs attemptedReleaseRefs,
            U03AcceptedEvidenceBinding acceptedEvidenceBinding,
            String environmentId) {
        if (command == null) throw new IllegalArgumentException("command is required");
        if (attemptedReleaseRefs == null) {
            throw new IllegalArgumentException("attemptedReleaseRefs are required");
        }
        if (acceptedEvidenceBinding == null) {
            throw new IllegalArgumentException("acceptedEvidenceBinding is required");
        }

        // Production remains a hard exceptional boundary and must never be
        // reclassified as STALE_INPUT or RELEASE_MISMATCH.
        U03NonProductionExecutionContext.requireNonProductionEnvironment(environmentId);

        // Match the frozen P0 ordering: release identity precedes stale-state.
        if (!isGateCFrozenSet(attemptedReleaseRefs)) {
            return U03NonProductionAdmissionResult.failed(
                    command, attemptedReleaseRefs, RELEASE_MISMATCH);
        }

        if (acceptedEvidenceBinding.getClinicalStateVersion() != command.clinicalStateVersion) {
            return U03NonProductionAdmissionResult.failed(
                    command, attemptedReleaseRefs, STALE_INPUT);
        }

        // Existing strict constructor guards remain as defense-in-depth.
        return U03NonProductionAdmissionResult.accepted(
                new U03NonProductionExecutionContext(
                        command,
                        attemptedReleaseRefs,
                        acceptedEvidenceBinding,
                        environmentId));
    }

    private static boolean isGateCFrozenSet(U03ExplicitNonProductionReleaseRefs refs) {
        return U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF
                        .equals(refs.getKnowledgeReleaseRef())
                && U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF
                        .equals(refs.getRuleReleaseRef())
                && U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF
                        .equals(refs.getCoverageContractRef())
                && U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF
                        .equals(refs.getPolicyReleaseRef())
                && U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF
                        .equals(refs.getPolicyPairRef());
    }
}
