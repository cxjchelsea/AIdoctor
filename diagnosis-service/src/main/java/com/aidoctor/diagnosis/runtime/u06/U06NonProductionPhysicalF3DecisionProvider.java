package com.aidoctor.diagnosis.runtime.u06;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalF3Owner;
import com.aidoctor.diagnosis.runtime.u06.state.U06StateValues;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bridges existing U06 Mode-1 admission and F3 state semantics to the
 * physical CDP owner. NON-PRODUCTION: U06 admission currently requires a
 * synthetic verification profile and does not assert production authority.
 * No clinical decision is inferred from caller claims.
 */
public final class U06NonProductionPhysicalF3DecisionProvider
        implements U07NonProductionPhysicalF3Owner.F3DecisionProvider {
    private final U06ProfileBRequest request;
    private final U06SyntheticDecisionBundle decision;
    private final U06AdmissionService admission;

    public U06NonProductionPhysicalF3DecisionProvider(U06ProfileBRequest request,
            U06SyntheticDecisionBundle decision, U06AdmissionService admission) {
        if (request == null || decision == null || admission == null)
            throw new IllegalArgumentException("U07_U06_F3_DEPENDENCY_REQUIRED");
        this.request = request;
        this.decision = decision;
        this.admission = admission;
    }

    @Override
    public U07NonProductionPhysicalF3Owner.PreparedF3 prepare(
            String consultationId, String cdpId, int currentVersion) {
        if (!request.getConsultationId().equals(consultationId)
                || !request.getCdpId().equals(cdpId)
                || !U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT.equals(request.getMode()))
            throw new IllegalStateException("U07_U06_F3_MODE_OR_IDENTITY_INVALID");
        U06AdmissionService.Admission admitted = admission.admit(
                request, currentVersion, false);
        if (!admitted.isAdmitted())
            throw new IllegalStateException("U07_U06_F3_ADMISSION_" + admitted.getReasonCode());
        if (!U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED.equals(decision.getF3OwnerStatus())
                && !U06SyntheticDecisionBundle.NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED.equals(
                        decision.getF3OwnerStatus()))
            throw new IllegalStateException("U07_U06_F3_NOT_MUTATING_DECISION");
        String effect = decision.getF3CanonicalEffectId();
        if (blank(effect))
            throw new IllegalStateException("U07_U06_F3_EFFECT_REQUIRED");
        if (U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED.equals(decision.getF3OwnerStatus())
                && (blank(decision.getGapId()) || blank(decision.getGapDecisionImpact())))
            throw new IllegalStateException("U07_U06_F3_GAP_BASIS_INCOMPLETE");

        String fingerprint = U06Ids.hash("u06f3payload", effect,
                decision.getF3OwnerStatus(), decision.getGapId(),
                decision.getGapDecisionImpact(), String.valueOf(decision.isAskableOnline()),
                request.getDependencyBindingType(), request.getDependencyBindingRef(),
                request.getF3OwnerPolicyRef(), request.getSourceAuthorityRef(), "1");
        Map<String, Object> assessment =
                U06StateValues.f3Assessment(request, decision, effect, fingerprint);
        List<StateTypes.StatePatchOperation> operations =
                new ArrayList<StateTypes.StatePatchOperation>();
        operations.add(add("/patient_state/f3_gap_assessment", assessment));
        if (U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED.equals(decision.getF3OwnerStatus()))
            operations.add(add("/patient_state/information_gaps/" + decision.getGapId(),
                    U06StateValues.gap(request, decision, "QUESTIONABLE_ONLINE")));

        StateTypes.StatePatch patch = new StateTypes.StatePatch();
        patch.operations = operations;
        return new U07NonProductionPhysicalF3Owner.PreparedF3(
                effect, U06Ids.hash("u06proposal", effect),
                U06Ids.hash("u06f3physicalidem", consultationId, cdpId, effect, fingerprint),
                patch);
    }

    private static StateTypes.StatePatchOperation add(String path, Object value) {
        StateTypes.StatePatchOperation operation = new StateTypes.StatePatchOperation();
        operation.op = "ADD";
        operation.path = path;
        operation.value = value;
        operation.source = "PATIENT_FACT";
        operation.sensitivity = "PHI";
        return operation;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
