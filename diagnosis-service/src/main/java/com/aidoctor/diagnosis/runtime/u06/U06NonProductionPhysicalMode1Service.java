package com.aidoctor.diagnosis.runtime.u06;

import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07GovernedNonProductionF3CommitPort;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionF3A1U05Flow;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalF3Owner;
import com.aidoctor.diagnosis.state.committer.StateCommitter;

/**
 * U06 Mode-1 physical implementation behind the existing ProfileB execute
 * entry. Explicit NON-PRODUCTION host construction only; the legacy synthetic
 * Mode-2/3 path remains separately gated and cannot run through this host.
 *
 * Uses the same U06 Admission, canonical business-state builder, and post-F3
 * safety semantics, but commits using the governed StateCommitter and verifies
 * physical CDP readback. Safety evidence remains externally supplied and is
 * NOT authenticated production clinical approval.
 */
public final class U06NonProductionPhysicalMode1Service
        implements U06ProfileBApplicationService.PhysicalMode1Port {
    private final CDPRepository repository;
    private final StateCommitter committer;
    private final U06AdmissionService admission;
    private final U06SyntheticPostF3SafetyBarrier safety;

    public U06NonProductionPhysicalMode1Service(CDPRepository repository,
            StateCommitter committer, U06AdmissionService admission,
            U06SyntheticPostF3SafetyBarrier safety) {
        if (repository == null || committer == null || admission == null || safety == null)
            throw new IllegalArgumentException("U06_PHYSICAL_MODE1_DEPENDENCIES_REQUIRED");
        this.repository = repository;
        this.committer = committer;
        this.admission = admission;
        this.safety = safety;
    }

    @Override
    public U06ExecutionResult execute(U06ProfileBRequest request,
            U06SyntheticDecisionBundle decision,
            U06SyntheticPostF3SafetyBarrier.Evidence evidence) {
        if (request == null || decision == null
                || !U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT.equals(request.getMode())
                || !U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD.equals(request.getExecutionProfile()))
            throw new IllegalStateException("U06_PHYSICAL_MODE1_NONPRODUCTION_ONLY");

        U07GovernedNonProductionF3CommitPort commits =
                new U07GovernedNonProductionF3CommitPort(repository, committer);
        int version = commits.currentVersion(request.getCdpId());
        U06AdmissionService.Admission result = admission.admit(request, version, false);
        if (!result.isAdmitted())
            return new U06ExecutionResult(U06ExecutionResult.ADMISSION_REJECTED,
                    result.getAdmissionId(), null, null, null, null, null, null,
                    result.getReasonCode());
        if (U06SyntheticDecisionBundle.FAILED.equals(decision.getF3OwnerStatus()))
            return new U06ExecutionResult(U06ExecutionResult.FAILURE_REQUIRED,
                    result.getAdmissionId(), null, null, null, null, null, null,
                    decision.getGapDecisionImpact());
        if (!U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED.equals(decision.getF3OwnerStatus())
                && !U06SyntheticDecisionBundle.NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED.equals(
                        decision.getF3OwnerStatus()))
            return new U06ExecutionResult(U06ExecutionResult.MODE1_NO_MUTATION,
                    result.getAdmissionId(), null, null, null, null, null, null,
                    decision.getF3OwnerStatus());

        U07NonProductionPhysicalF3Owner owner = new U07NonProductionPhysicalF3Owner(
                commits, repository,
                new U06NonProductionPhysicalF3DecisionProvider(request, decision, admission));
        U07NonProductionF3A1U05Flow.F3Committed committed =
                owner.executeAndReadback(request.getConsultationId(), request.getCdpId());
        if (!committed.committedAndReadback
                || !decision.getF3CanonicalEffectId().equals(committed.effectId))
            throw new IllegalStateException("U06_PHYSICAL_F3_EFFECT_NOT_CURRENT");
        U06SyntheticPostF3SafetyBarrier.Evaluation evaluated = safety.evaluate(
                request.getConsultationId(), committed.effectId, "COMMITTED",
                committed.persistedVersion, evidence);
        if (U06SyntheticPostF3SafetyBarrier.BLOCKED.equals(evaluated.getStatus()))
            return new U06ExecutionResult(U06ExecutionResult.MODE1_SAFETY_BLOCKED,
                    result.getAdmissionId(), committed.effectId, "COMMITTED",
                    null, null, null, null, "POST_F3_SAFETY_BLOCKED",
                    evaluated.getEvaluationId());
        if (!U06SyntheticPostF3SafetyBarrier.ALLOWED.equals(evaluated.getStatus()))
            return new U06ExecutionResult(U06ExecutionResult.FAILURE_REQUIRED,
                    result.getAdmissionId(), committed.effectId, "COMMITTED",
                    null, null, null, null, "POST_F3_SAFETY_UNAVAILABLE",
                    evaluated.getEvaluationId());
        return new U06ExecutionResult(U06ExecutionResult.MODE1_COMMITTED,
                result.getAdmissionId(), committed.effectId, "COMMITTED",
                null, null, null, null, null, evaluated.getEvaluationId());
    }
}
