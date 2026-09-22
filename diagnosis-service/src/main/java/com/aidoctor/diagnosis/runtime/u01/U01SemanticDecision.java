package com.aidoctor.diagnosis.runtime.u01;

public class U01SemanticDecision {
    public static final String SUBJECT_RESOLVED = "RESOLVED";
    public static final String SUBJECT_CLARIFICATION_REQUIRED = "CLARIFICATION_REQUIRED";
    public static final String PROBLEM_FRAMED = "FRAMED";
    public static final String PROBLEM_CLARIFICATION_REQUIRED = "CLARIFICATION_REQUIRED";
    public static final String IN_SCOPE = "IN_SCOPE";
    public static final String OUT_OF_SCOPE = "OUT_OF_SCOPE";
    public static final String NEEDS_CLARIFICATION = "NEEDS_CLARIFICATION";

    private final String subjectStatus;
    private final String normalizedSubjectType;
    private final String problemStatus;
    private final String scopeDecision;
    private final String clarificationReason;
    private final String nextUnit;
    private final boolean earlySafetySignalPresent;

    public U01SemanticDecision(String subjectStatus, String normalizedSubjectType,
                               String problemStatus, String scopeDecision,
                               String clarificationReason, String nextUnit,
                               boolean earlySafetySignalPresent) {
        this.subjectStatus = subjectStatus;
        this.normalizedSubjectType = normalizedSubjectType;
        this.problemStatus = problemStatus;
        this.scopeDecision = scopeDecision;
        this.clarificationReason = clarificationReason;
        this.nextUnit = nextUnit;
        this.earlySafetySignalPresent = earlySafetySignalPresent;
    }

    public String getSubjectStatus() { return subjectStatus; }
    public String getNormalizedSubjectType() { return normalizedSubjectType; }
    public String getProblemStatus() { return problemStatus; }
    public String getScopeDecision() { return scopeDecision; }
    public String getClarificationReason() { return clarificationReason; }
    public String getNextUnit() { return nextUnit; }
    public boolean isEarlySafetySignalPresent() { return earlySafetySignalPresent; }
}
