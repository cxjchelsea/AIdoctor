package com.aidoctor.diagnosis.runtime.u01;

import org.springframework.stereotype.Component;

/** Deterministic U01 business interpretation. Final scope decision is not delegated to a model. */
@Component
public class U01SemanticPolicy {

    public U01SemanticDecision decide(U01StartCommand command) {
        String subjectType = normalize(command.getSubjectType());
        boolean subjectResolved = "SELF".equals(subjectType)
                || ("OTHER".equals(subjectType) && hasText(command.getSubjectReferenceId()));

        if (!subjectResolved) {
            return clarification(subjectType, false, "SUBJECT_NOT_RESOLVED", command.isEarlySafetySignalPresent());
        }

        if (!hasText(command.getProblemText())) {
            return clarification(subjectType, true, "PROBLEM_NOT_FRAMED", command.isEarlySafetySignalPresent());
        }

        String scope = normalize(command.getScopeIntentCandidate());
        if (!hasText(scope) || "UNKNOWN".equals(scope) || "MIXED".equals(scope)) {
            return clarification(subjectType, true, "SCOPE_AMBIGUOUS", command.isEarlySafetySignalPresent());
        }

        if (isInScope(scope)) {
            return new U01SemanticDecision(
                    U01SemanticDecision.SUBJECT_RESOLVED,
                    subjectType,
                    U01SemanticDecision.PROBLEM_FRAMED,
                    U01SemanticDecision.IN_SCOPE,
                    null,
                    "U02",
                    command.isEarlySafetySignalPresent());
        }

        if ("OUTSIDE_V1_INTENT".equals(scope)) {
            return new U01SemanticDecision(
                    U01SemanticDecision.SUBJECT_RESOLVED,
                    subjectType,
                    U01SemanticDecision.PROBLEM_FRAMED,
                    U01SemanticDecision.OUT_OF_SCOPE,
                    null,
                    "U11",
                    command.isEarlySafetySignalPresent());
        }

        return clarification(subjectType, true, "SCOPE_UNRECOGNIZED", command.isEarlySafetySignalPresent());
    }

    private U01SemanticDecision clarification(String subjectType, boolean subjectResolved,
                                               String reason, boolean earlySafetySignal) {
        return new U01SemanticDecision(
                subjectResolved
                        ? U01SemanticDecision.SUBJECT_RESOLVED
                        : U01SemanticDecision.SUBJECT_CLARIFICATION_REQUIRED,
                subjectType,
                U01SemanticDecision.PROBLEM_CLARIFICATION_REQUIRED,
                U01SemanticDecision.NEEDS_CLARIFICATION,
                reason,
                "U06",
                earlySafetySignal);
    }

    private boolean isInScope(String scope) {
        return "SYMPTOM".equals(scope)
                || "EXAMINATION".equals(scope)
                || "COMPREHENSIVE".equals(scope)
                || "CLINICAL_CONSULTATION".equals(scope);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
