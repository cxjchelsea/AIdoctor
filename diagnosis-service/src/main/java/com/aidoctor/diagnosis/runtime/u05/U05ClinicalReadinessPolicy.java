package com.aidoctor.diagnosis.runtime.u05;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic frozen D03 policy implementation. No model/tool/external call is permitted. */
public final class U05ClinicalReadinessPolicy {
    public static final String POLICY_ID = "D03";
    public static final String POLICY_VERSION = "U05-D03-POLICY-V1-REFROZEN";

    public U05ClinicalReadinessDecision decide(U05AdmittedInput input) {
        if (input == null) throw new IllegalArgumentException("admitted input is required");
        validateAdmittedBinding(input);

        String inputFailure = inputFailureReason(input);
        if (inputFailure != null) {
            return decision(input, U05ClinicalReadinessDecision.INPUT_FAILURE, null,
                    inputFailure, "D03-P0");
        }

        if (hasConflict(input.getManifest())) {
            return decision(input, U05ClinicalReadinessDecision.INPUT_CONFLICT, null,
                    "D03_MUTUALLY_EXCLUSIVE_INPUT_CONFLICT", "D03-P1");
        }

        // P2 — F1 owner says the consultation/question is out of scope.
        if (hasSignal(input, U05ReadinessInput.F1, U05ReadinessInput.OUT_OF_SCOPE)) {
            return decision(input, U05ClinicalReadinessDecision.DECIDED,
                    U05ClinicalReadinessDecision.OUT_OF_SCOPE,
                    "D03_OUT_OF_SCOPE", "D03-POL-001");
        }

        // P3 — qualified blocking offline path outranks online questioning.
        if (hasSignal(input, U05ReadinessInput.F3, U05ReadinessInput.NEEDS_OFFLINE_EVIDENCE)
                || hasSignal(input, U05ReadinessInput.F5, U05ReadinessInput.NEEDS_OFFLINE_EVIDENCE)
                || hasSignal(input, U05ReadinessInput.F6, U05ReadinessInput.NEEDS_OFFLINE_EVIDENCE)) {
            return decision(input, U05ClinicalReadinessDecision.DECIDED,
                    U05ClinicalReadinessDecision.NEEDS_OFFLINE_EVIDENCE,
                    "D03_BLOCKING_OFFLINE_EVIDENCE_REQUIRED", "D03-POL-002");
        }

        // P4 — lawful F1/F2 clarification.
        if (hasSignal(input, U05ReadinessInput.F1, U05ReadinessInput.NEEDS_CLARIFICATION)
                || hasSignal(input, U05ReadinessInput.F2_CLARIFICATION, U05ReadinessInput.NEEDS_CLARIFICATION)) {
            return decision(input, U05ClinicalReadinessDecision.DECIDED,
                    U05ClinicalReadinessDecision.NEEDS_CLARIFICATION,
                    "D03_CLARIFICATION_REQUIRED", "D03-POL-003");
        }

        // P5 — current F3 Owner has an online question with actual decision value.
        if (hasSignal(input, U05ReadinessInput.F3, U05ReadinessInput.CAN_ASK_MORE)) {
            return decision(input, U05ClinicalReadinessDecision.DECIDED,
                    U05ClinicalReadinessDecision.CAN_ASK_MORE,
                    "D03_HIGH_VALUE_ONLINE_GAP_AVAILABLE", "D03-POL-004");
        }

        // P6-A — owner-approved first clinical-analysis entry.
        if (matchesPol005(input)) {
            return decision(input, U05ClinicalReadinessDecision.DECIDED,
                    U05ClinicalReadinessDecision.READY_FOR_CLINICAL_ANALYSIS,
                    "D03_MINIMUM_ANALYSIS_CONDITIONS_SATISFIED", "D03-POL-005");
        }

        // P6-B — first analysis after current F6 proves no blocking offline need.
        if (matchesPol011(input)) {
            return decision(input, U05ClinicalReadinessDecision.DECIDED,
                    U05ClinicalReadinessDecision.READY_FOR_CLINICAL_ANALYSIS,
                    "D03_MINIMUM_ANALYSIS_CONDITIONS_SATISFIED_AFTER_F6", "D03-POL-011");
        }

        // P7 — lawful business negative, never a fallback for missing/failed inputs.
        if (hasSignal(input, U05ReadinessInput.F5, U05ReadinessInput.NO_RELIABLE_DIRECTION)
                && hasSignal(input, U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP)
                && !hasAnyOfflineBlockingSignal(input)) {
            return decision(input, U05ClinicalReadinessDecision.DECIDED,
                    U05ClinicalReadinessDecision.NO_RELIABLE_DIRECTION,
                    "D03_NO_RELIABLE_DIRECTION", "D03-POL-006");
        }

        throw new U05PolicyExpectationGapException(
                "D03_INITIAL_READINESS_BOOTSTRAP_UNDERDETERMINED",
                "D03_POLICY_EXPECTATION_GAP: admitted profile has no unique frozen D03 result");
    }

    private static void validateAdmittedBinding(U05AdmittedInput input) {
        if (!input.getAdmissionId().equals(input.getAdmissionId())
                || !input.getAcceptedReadinessInputSetIdentity().equals(input.getManifest().getSetIdentity())) {
            throw new IllegalStateException("D03 admitted-snapshot binding mismatch");
        }
    }

    private static String inputFailureReason(U05AdmittedInput input) {
        U05ReadinessInputManifest manifest = input.getManifest();
        int version = input.getClinicalStateVersion();

        for (U05ReadinessInput record : manifest.getInputs()) {
            String status = record.getApplicabilityStatus();
            if (U05ReadinessInput.STALE.equals(status)) return "D03_STALE_READINESS_INPUT";
            if (U05ReadinessInput.FAILED.equals(status)) return "D03_REQUIRED_INPUT_FAILED";
            if (U05ReadinessInput.UNAVAILABLE.equals(status)) return "D03_REQUIRED_INPUT_UNAVAILABLE";
            if (record.isPresent()) {
                if (!input.getConsultationId().equals(record.getConsultationId())
                        || !input.getCdpId().equals(record.getCdpId())) {
                    return "D03_INPUT_IDENTITY_MISMATCH";
                }
                if (!record.isCurrentPresentAt(version)) {
                    return "D03_INPUT_VERSION_MISMATCH";
                }
            }
        }

        String context = input.getEvaluationContext();
        if (U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context)) {
            if (!manifest.hasCurrentPresent(U05ReadinessInput.F1)
                    || !manifest.hasCurrentPresent(U05ReadinessInput.F3)) {
                return "D03_REQUIRED_INPUT_UNAVAILABLE";
            }
        } else if (U05ConsumerInboundRequest.POST_DDX_REEVALUATION.equals(context)) {
            if (!manifest.hasCurrentPresent(U05ReadinessInput.F1)
                    || !manifest.hasCurrentPresent(U05ReadinessInput.F3)
                    || !manifest.hasCurrentPresent(U05ReadinessInput.F5)) {
                return "D03_REQUIRED_INPUT_UNAVAILABLE";
            }
        } else if (U05ConsumerInboundRequest.POST_OFFLINE_ASSESSMENT.equals(context)) {
            if (!manifest.hasCurrentPresent(U05ReadinessInput.F1)
                    || !manifest.hasCurrentPresent(U05ReadinessInput.F6)) {
                return "D03_REQUIRED_INPUT_UNAVAILABLE";
            }
        } else if (U05ConsumerInboundRequest.POST_USER_FACT_UPDATE.equals(context)
                || U05ConsumerInboundRequest.POST_SAFETY_INITIAL.equals(context)) {
            if (!manifest.hasCurrentPresent(U05ReadinessInput.F1)) {
                return "D03_REQUIRED_INPUT_UNAVAILABLE";
            }
        }
        return null;
    }

    private static boolean hasConflict(U05ReadinessInputManifest manifest) {
        Map<String, Set<String>> domainSignals = new LinkedHashMap<String, Set<String>>();
        Map<String, String> idFingerprints = new LinkedHashMap<String, String>();
        for (U05ReadinessInput input : manifest.getInputs()) {
            String prior = idFingerprints.get(input.getReadinessInputId());
            String fingerprint = input.semanticFingerprint();
            if (prior != null && !prior.equals(fingerprint)) return true;
            idFingerprints.put(input.getReadinessInputId(), fingerprint);

            if (input.isPresent()) {
                Set<String> signals = domainSignals.get(input.getSourceDomain());
                if (signals == null) {
                    signals = new LinkedHashSet<String>();
                    domainSignals.put(input.getSourceDomain(), signals);
                }
                signals.add(input.getBusinessSignal());
                if (signals.size() > 1) return true;
            }
        }
        return false;
    }

    private static boolean matchesPol005(U05AdmittedInput input) {
        String context = input.getEvaluationContext();
        if (U05ConsumerInboundRequest.POST_DDX_REEVALUATION.equals(context)
                || U05ConsumerInboundRequest.POST_OFFLINE_ASSESSMENT.equals(context)) {
            return false;
        }
        return hasSignal(input, U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE)
                && hasSignal(input, U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP)
                && onlyStatus(input, U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE)
                && onlyStatus(input, U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE);
    }

    private static boolean matchesPol011(U05AdmittedInput input) {
        String context = input.getEvaluationContext();
        boolean allowedContext = U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context)
                || U05ConsumerInboundRequest.POST_USER_FACT_UPDATE.equals(context)
                || U05ConsumerInboundRequest.POST_OFFLINE_ASSESSMENT.equals(context);
        return allowedContext
                && hasSignal(input, U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE)
                && hasSignal(input, U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP)
                && onlyStatus(input, U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE)
                && hasSignal(input, U05ReadinessInput.F6, U05ReadinessInput.NO_BLOCKING_OFFLINE_EVIDENCE_NEED);
    }

    private static boolean hasAnyOfflineBlockingSignal(U05AdmittedInput input) {
        return hasSignal(input, U05ReadinessInput.F3, U05ReadinessInput.NEEDS_OFFLINE_EVIDENCE)
                || hasSignal(input, U05ReadinessInput.F5, U05ReadinessInput.NEEDS_OFFLINE_EVIDENCE)
                || hasSignal(input, U05ReadinessInput.F6, U05ReadinessInput.NEEDS_OFFLINE_EVIDENCE);
    }

    private static boolean hasSignal(U05AdmittedInput input, String domain, String signal) {
        for (U05ReadinessInput record : input.getManifest().getInputs(domain)) {
            if (record.isPresent() && signal.equals(record.getBusinessSignal())) return true;
        }
        return false;
    }

    private static boolean onlyStatus(U05AdmittedInput input, String domain, String status) {
        List<U05ReadinessInput> records = input.getManifest().getInputs(domain);
        if (records.isEmpty()) return false;
        for (U05ReadinessInput record : records) {
            if (!status.equals(record.getApplicabilityStatus())) return false;
        }
        return true;
    }

    private static U05ClinicalReadinessDecision decision(
            U05AdmittedInput input,
            String status,
            String readiness,
            String reason,
            String rule) {
        String decisionId = U05Ids.hash(
                "u05-d03",
                input.getAdmissionId(),
                input.getAcceptedReadinessInputSetIdentity(),
                POLICY_VERSION,
                rule,
                status,
                readiness,
                reason);
        List<String> basis = new ArrayList<String>();
        basis.add(input.getAdmissionId());
        basis.add(input.getAcceptedReadinessInputManifestRef());
        basis.add(input.getAcceptedU04GateRef());
        basis.add(input.getAcceptedRouteAuthorizationRef());
        if (input.getAcceptedRestrictedContextRef() != null) basis.add(input.getAcceptedRestrictedContextRef());
        if (input.getAcceptedRestrictedPermissionRef() != null) basis.add(input.getAcceptedRestrictedPermissionRef());

        Set<String> ruleRefs = new LinkedHashSet<String>();
        for (U05ReadinessInput record : input.getManifest().getInputs()) {
            ruleRefs.addAll(record.getPolicyOrRuleRefs());
        }
        ruleRefs.add(rule);

        return new U05ClinicalReadinessDecision(
                decisionId,
                input,
                status,
                readiness,
                Arrays.asList(reason),
                basis,
                POLICY_VERSION,
                rule,
                new ArrayList<String>(ruleRefs),
                new ArrayList<String>(),
                Instant.now().toString());
    }
}
