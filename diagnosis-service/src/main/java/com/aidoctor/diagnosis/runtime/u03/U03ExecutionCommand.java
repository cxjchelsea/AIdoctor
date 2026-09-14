package com.aidoctor.diagnosis.runtime.u03;

/** Runtime command for assessing exactly one current Clinical State Version. */
public final class U03ExecutionCommand {
    public final String consultationId;
    public final String threadId;
    public final String runId;
    public final String eventId;
    public final String cdpId;
    public final int clinicalStateVersion;
    public final String correlationId;
    public final String traceId;

    public U03ExecutionCommand(String consultationId, String threadId, String runId, String eventId,
            String cdpId, int clinicalStateVersion, String correlationId, String traceId) {
        this.consultationId = required(consultationId, "consultationId");
        this.threadId = required(threadId, "threadId");
        this.runId = required(runId, "runId");
        this.eventId = required(eventId, "eventId");
        this.cdpId = required(cdpId, "cdpId");
        if (clinicalStateVersion < 0) throw new IllegalArgumentException("clinicalStateVersion must be non-negative");
        this.clinicalStateVersion = clinicalStateVersion;
        this.correlationId = required(correlationId, "correlationId");
        this.traceId = required(traceId, "traceId");
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
