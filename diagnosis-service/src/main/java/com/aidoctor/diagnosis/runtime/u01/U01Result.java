package com.aidoctor.diagnosis.runtime.u01;

public class U01Result {
    private final String consultationId;
    private final String cdpId;
    private final String lifecycleStatus;
    private final String subjectStatus;
    private final String problemStatus;
    private final String scopeDecision;
    private final String clarificationReason;
    private final boolean earlySafetySignalPresent;
    private final String nextUnit;
    private final String runId;

    public U01Result(ConsultationRecord record, String runId) {
        this.consultationId = record.getConsultationId();
        this.cdpId = record.getCdpId();
        this.lifecycleStatus = record.getLifecycleStatus();
        this.subjectStatus = record.getSubjectStatus();
        this.problemStatus = record.getProblemStatus();
        this.scopeDecision = record.getScopeDecision();
        this.clarificationReason = record.getClarificationReason();
        this.earlySafetySignalPresent = record.isEarlySafetySignal();
        this.nextUnit = record.getNextUnit();
        this.runId = runId;
    }

    public String getConsultationId() { return consultationId; }
    public String getCdpId() { return cdpId; }
    public String getLifecycleStatus() { return lifecycleStatus; }
    public String getSubjectStatus() { return subjectStatus; }
    public String getProblemStatus() { return problemStatus; }
    public String getScopeDecision() { return scopeDecision; }
    public String getClarificationReason() { return clarificationReason; }
    public boolean isEarlySafetySignalPresent() { return earlySafetySignalPresent; }
    public String getNextUnit() { return nextUnit; }
    public String getRunId() { return runId; }
}
