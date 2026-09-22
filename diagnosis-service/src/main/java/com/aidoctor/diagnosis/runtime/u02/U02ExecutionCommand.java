package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.diagnosis.runtime.u02.d05.U02DependencyInvalidationHook;

import java.util.Collections;
import java.util.List;

/** U02 requires real Runtime correlation IDs; it never synthesizes Thread/Run/Event identity. */
public final class U02ExecutionCommand {
    public final String userId;
    public final String consultationId;
    public final String threadId;
    public final String runId;
    public final String eventId;
    public final String cdpId;
    public final String rawClinicalText;
    public final String sourceType;
    public final int baseClinicalStateVersion;
    public final String correlationId;
    public final String traceId;
    public final List<U02DependencyInvalidationHook.DependentArtifact> registeredDependencies;

    public U02ExecutionCommand(
            String userId,
            String consultationId,
            String threadId,
            String runId,
            String eventId,
            String cdpId,
            String rawClinicalText,
            String sourceType,
            int baseClinicalStateVersion,
            String correlationId,
            String traceId,
            List<U02DependencyInvalidationHook.DependentArtifact> registeredDependencies) {
        this.userId = required(userId, "userId");
        this.consultationId = required(consultationId, "consultationId");
        this.threadId = required(threadId, "threadId");
        this.runId = required(runId, "runId");
        this.eventId = required(eventId, "eventId");
        this.cdpId = required(cdpId, "cdpId");
        this.rawClinicalText = rawClinicalText == null ? "" : rawClinicalText;
        this.sourceType = required(sourceType, "sourceType");
        if (baseClinicalStateVersion < 0) throw new IllegalArgumentException("baseClinicalStateVersion must be non-negative");
        this.baseClinicalStateVersion = baseClinicalStateVersion;
        this.correlationId = required(correlationId, "correlationId");
        this.traceId = required(traceId, "traceId");
        this.registeredDependencies = registeredDependencies == null
                ? Collections.<U02DependencyInvalidationHook.DependentArtifact>emptyList()
                : Collections.unmodifiableList(registeredDependencies);
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
