package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.diagnosis.runtime.u02.d05.U02DependencyInvalidationHook;
import java.util.Collections;

/**
 * Host-gated START handoff. Caller supplies only the original raw patient text;
 * clinical identity must be read from durable authoritative U01 records.
 * Trace authority is a required independent host port, never synthesized here.
 */
public final class U01U02NonProductionHandoffService {
    public interface TrustedTraceAuthority {
        TraceIdentity requireForStart(String consultationId, String runId, String eventId);
    }
    public static final class TraceIdentity {
        public final String correlationId;
        public final String traceId;
        public TraceIdentity(String correlationId, String traceId) {
            if (blank(correlationId) || blank(traceId))
                throw new IllegalArgumentException("Trusted trace identity required");
            this.correlationId = correlationId;
            this.traceId = traceId;
        }
    }

    private final U01U02TrustedStartContext resolver;
    private final TrustedTraceAuthority traceAuthority;
    private final U02ClinicalFactApplicationService application;

    public U01U02NonProductionHandoffService(U01U02TrustedStartContext resolver,
            TrustedTraceAuthority traceAuthority,
            U02ClinicalFactApplicationService application) {
        if (resolver == null || traceAuthority == null || application == null)
            throw new IllegalArgumentException("Trusted U02 host dependencies required");
        this.resolver = resolver;
        this.traceAuthority = traceAuthority;
        this.application = application;
    }

    public U02ExecutionResult executeCommittedStart(
            String consultationId, String runId, String rawClinicalText) {
        if (blank(rawClinicalText)) throw new IllegalArgumentException("Patient source text required");
        U01U02TrustedStartContext.VerifiedStart verified = resolver.require(consultationId, runId);
        TraceIdentity trace = traceAuthority.requireForStart(
                verified.consultationId, verified.runId, verified.eventId);
        if (trace == null) throw new IllegalStateException("U01_U02_HANDOFF_BLOCKED:TRACE_NOT_AUTHORIZED");
        return application.execute(new U02ExecutionCommand(
                verified.userId, verified.consultationId, verified.threadId, verified.runId,
                verified.eventId, verified.cdpId, rawClinicalText, "PATIENT_REPORTED",
                verified.clinicalStateVersion, trace.correlationId, trace.traceId,
                Collections.<U02DependencyInvalidationHook.DependentArtifact>emptyList()));
    }

    private static boolean blank(String v) { return v == null || v.trim().isEmpty(); }
}
