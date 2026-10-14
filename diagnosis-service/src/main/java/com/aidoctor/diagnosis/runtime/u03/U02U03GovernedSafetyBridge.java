package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.runtime.governance.CapabilityCallTraceRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityCallTraceRepository;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionResult;
import com.aidoctor.diagnosis.runtime.u04.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * Non-production, fail-closed U02 -> U03 admission and U03 -> U04 execution.
 * Does not infer accepted clinical evidence, issue a risk disposition, mint a
 * C02 authority, or translate failures into a permissive Safety Gate.
 */
public class U02U03GovernedSafetyBridge {
    /** Host-owned evidence acceptance; never derived from C01 candidate output. */
    public interface AcceptedEvidenceAuthority {
        U03AcceptedEvidenceBinding requireAccepted(String consultationId, String cdpId,
                int committedClinicalStateVersion, String u02CapabilityCallId);
    }

    private final CDPRepository cdps;
    private final CapabilityCallTraceRepository traces;
    private final AcceptedEvidenceAuthority acceptedEvidence;
    private final U03NonProductionAdmissionService admission;
    private final U04NonProductionApplicationService safetyGate;

    public U02U03GovernedSafetyBridge(CDPRepository cdps,
            CapabilityCallTraceRepository traces, AcceptedEvidenceAuthority acceptedEvidence,
            U03NonProductionAdmissionService admission,
            U04NonProductionApplicationService safetyGate) {
        if (cdps == null || traces == null || acceptedEvidence == null || admission == null || safetyGate == null)
            throw new IllegalArgumentException("Governed safety dependencies required");
        this.cdps = cdps;
        this.traces = traces;
        this.acceptedEvidence = acceptedEvidence;
        this.admission = admission;
        this.safetyGate = safetyGate;
    }

    /**
     * Caller must retain the exact trusted U01 identities from the runtime.
     * No U03 invocation is made here: return an admitted context for its
     * independently authorized CD-07 execution owner.
     */
    @Transactional(readOnly = true)
    public U03NonProductionExecutionContext requireU03Admission(
            U02ExecutionResult u02, U03ExecutionCommand command,
            U03ExplicitNonProductionReleaseRefs releases, String environmentId) {
        requireNonProduction(environmentId);
        if (u02 == null || command == null
                || !"COMMITTED".equals(u02.status)
                || u02.commitResult == null
                || !"COMMITTED".equals(u02.commitResult.status)
                || u02.commitResult.committedVersion == null
                || u02.commitResult.previousVersion == null
                || u02.decision == null || u02.proposal == null
                || u02.capabilityCallId == null) {
            throw blocked("U02_NOT_COMMITTED");
        }
        if (!command.consultationId.equals(u02.decision.getConsultationId())
                || !command.eventId.equals(u02.decision.getEventId())
                || u02.proposal.getStatePatch() == null
                || !command.cdpId.equals(u02.proposal.getStatePatch().cdpId)
                || u02.proposal.getStatePatch().baseVersion == null
                || !u02.commitResult.previousVersion.equals(u02.proposal.getStatePatch().baseVersion)
                || command.clinicalStateVersion != u02.commitResult.committedVersion.intValue()) {
            throw blocked("U02_U03_IDENTITY_OR_VERSION_MISMATCH");
        }
        CapabilityCallTraceRecord trace = traces.findById(u02.capabilityCallId)
                .orElseThrow(() -> blocked("U02_TRACE_NOT_FOUND"));
        if (!CapabilityCallTraceRecord.SUCCEEDED.equals(trace.getCallStatus())
                || !"U02".equals(trace.getUnitId())
                || !command.consultationId.equals(trace.getConsultationId())
                || !command.threadId.equals(trace.getThreadId())
                || !command.runId.equals(trace.getRunId())
                || !command.eventId.equals(trace.getEventId())
                || !u02.commitResult.previousVersion.equals(trace.getClinicalStateVersionBefore())
                || !u02.commitResult.committedVersion.equals(trace.getClinicalStateVersionAfter())
                || !u02.commitResult.patchId.equals(trace.getCommitRef())) {
            throw blocked("U02_TRACE_IDENTITY_OR_COMMIT_MISMATCH");
        }
        CDP cdp = cdps.findById(command.cdpId)
                .orElseThrow(() -> blocked("CDP_NOT_FOUND"));
        if (cdp.getVersion() == null
                || cdp.getVersion().intValue() != command.clinicalStateVersion) {
            throw blocked("U03_STALE_CDP_STATE");
        }
        U03AcceptedEvidenceBinding accepted = acceptedEvidence.requireAccepted(
                command.consultationId, command.cdpId,
                command.clinicalStateVersion, u02.capabilityCallId);
        if (accepted == null) throw blocked("ACCEPTED_EVIDENCE_MISSING");
        U03NonProductionAdmissionResult result = admission.admit(
                command, releases, accepted, environmentId);
        if (!result.isAccepted()) throw blocked("U03_ADMISSION_REJECTED");
        return result.getContext();
    }

    /**
     * Only the U03-authorized S14 producer may supply the handoff. U04 performs
     * its own release, red-flag, policy, freshness and routing gate checks.
     */
    public U04ExecutionResult applyU04(
            U03OutboundHandoff handoff, U04ScopeContext policy, String environmentId) {
        requireNonProduction(environmentId);
        if (handoff == null || policy == null) throw blocked("U03_HANDOFF_REQUIRED");
        CDP cdp = cdps.findById(handoff.getCdpId())
                .orElseThrow(() -> blocked("CDP_NOT_FOUND"));
        if (cdp.getVersion() == null) throw blocked("CDP_VERSION_MISSING");
        return safetyGate.execute(handoff, cdp.getVersion(), policy, environmentId);
    }

    private static void requireNonProduction(String environment) {
        if (environment == null || environment.trim().isEmpty())
            throw blocked("ENVIRONMENT_REQUIRED");
        String e = environment.trim().toLowerCase(java.util.Locale.ROOT);
        if ("prod".equals(e) || "production".equals(e)
                || e.startsWith("prod-") || e.startsWith("production-"))
            throw blocked("PRODUCTION_FORBIDDEN");
    }

    private static IllegalStateException blocked(String reason) {
        return new IllegalStateException("U02_U03_U04_SAFETY_BLOCKED:" + reason);
    }
}
