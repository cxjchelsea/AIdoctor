package com.aidoctor.diagnosis.runtime.u06;

import com.aidoctor.contracts.v1.ContractVersion;
import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u06.state.U06StateValues;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
import java.util.Collections;
import java.util.Map;

/**
 * Explicit NON-PRODUCTION U06 physical execution for Mode 1/2/3.
 *
 * Mode 2 commits only question SELECTION. A delivery confirmation, consultation
 * WAITING_USER transition and runtime checkpoint must originate from real,
 * independently authorized host services. None is synthesized here.
 *
 * Mode 3 checks current CDP readback; its result is diagnostic only and NOT an
 * immutable post-F3 revalidation issuer or A1 routing authorization.
 */
public final class U06NonProductionPhysicalMode23Service
        implements U06ProfileBApplicationService.PhysicalExecutionPort {
    private final U06ProfileBApplicationService.PhysicalMode1Port mode1;
    private final CDPRepository repository;
    private final StateCommitter committer;
    private final U06AdmissionService admission;

    public U06NonProductionPhysicalMode23Service(
            U06ProfileBApplicationService.PhysicalMode1Port mode1,
            CDPRepository repository, StateCommitter committer, U06AdmissionService admission) {
        if (mode1 == null || repository == null || committer == null || admission == null)
            throw new IllegalArgumentException("U06_PHYSICAL_MODE23_DEPENDENCIES_REQUIRED");
        this.mode1 = mode1;
        this.repository = repository;
        this.committer = committer;
        this.admission = admission;
    }

    @Override
    public U06ExecutionResult execute(U06ProfileBRequest r, U06SyntheticDecisionBundle d,
            U06SyntheticPostF3SafetyBarrier.Evidence safety) {
        if (r == null || d == null
                || !U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD.equals(r.getExecutionProfile()))
            throw new IllegalStateException("U06_PHYSICAL_NONPRODUCTION_ONLY");
        if (U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT.equals(r.getMode()))
            return mode1.execute(r, d, safety);

        CDP cdp = repository.findById(r.getCdpId())
                .orElseThrow(() -> new IllegalStateException("U06_PHYSICAL_CDP_NOT_FOUND"));
        Integer actualVersion = cdp.getVersion();
        if (actualVersion == null || actualVersion.intValue() < 0)
            throw new IllegalStateException("U06_PHYSICAL_VERSION_INVALID");
        U06AdmissionService.Admission a = admission.admit(r, actualVersion.intValue(), false);
        if (!a.isAdmitted())
            return new U06ExecutionResult(U06ExecutionResult.ADMISSION_REJECTED,
                    a.getAdmissionId(), null, null, null, null, null, null, a.getReasonCode());
        if (U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(r.getMode()))
            return select(r, d, a, cdp);
        if (U06ProfileBRequest.F3_CURRENT_VERSION_REVALIDATION.equals(r.getMode()))
            return revalidate(r, d, a, cdp);
        throw new IllegalStateException("U06_PHYSICAL_MODE_UNSUPPORTED");
    }

    private U06ExecutionResult select(U06ProfileBRequest r, U06SyntheticDecisionBundle d,
            U06AdmissionService.Admission a, CDP cdp) {
        if (!U06SyntheticDecisionBundle.SELECTED.equals(d.getQuestionSelectionStatus()))
            return new U06ExecutionResult(U06ExecutionResult.MODE2_NO_SELECTION,
                    a.getAdmissionId(), null, null, null, null, null, null,
                    d.getQuestionSelectionStatus());
        if (!U06SyntheticDecisionBundle.CONTINUE.equals(d.getD04Status()))
            throw new IllegalStateException("U06_PHYSICAL_QUESTION_D04_STOP");
        if (empty(r.getThreadId()) || empty(r.getRunId()))
            return new U06ExecutionResult(U06ExecutionResult.ADMISSION_REJECTED,
                    a.getAdmissionId(), null, null, null, null, null, null,
                    "U06_RUNTIME_WAIT_INPUT_REQUIRED");

        Map<String, Object> patient = cdp.getPatientState();
        if (patient == null || !(patient.get("questions") instanceof Map<?, ?>))
            throw new IllegalStateException("U06_PHYSICAL_QUESTIONS_PARENT_REQUIRED");
        if (patient.containsKey("pending_question"))
            throw new IllegalStateException("U06_PHYSICAL_PENDING_QUESTION_CONFLICT");
        String questionId = d.getQuestionId();
        // Avoid untrusted JSON pointer segments and forbidden path splicing.
        if (empty(questionId) || !questionId.matches("[A-Za-z0-9][A-Za-z0-9._:-]*"))
            throw new IllegalStateException("U06_PHYSICAL_QUESTION_ID_INVALID");
        if (((Map<?, ?>) patient.get("questions")).containsKey(questionId))
            throw new IllegalStateException("U06_PHYSICAL_QUESTION_ALREADY_SELECTED");

        StateTypes.StatePatchOperation op = new StateTypes.StatePatchOperation();
        op.op = "ADD";
        op.path = "/patient_state/questions/" + questionId;
        op.value = U06StateValues.questionSelected(r, d);
        op.source = "RULE_DERIVED";
        op.sensitivity = "PHI";
        StateTypes.StatePatch patch = patch(r, "u06selection",
                d.getQuestionSelectionEffectId(), Collections.singletonList(op));
        StateTypes.CommitResult committed = committer.commit(patch);
        if (committed == null || !"COMMITTED".equals(committed.status)
                || committed.committedVersion == null
                || committed.committedVersion.intValue() != cdp.getVersion().intValue() + 1)
            throw new IllegalStateException("U06_PHYSICAL_SELECTION_COMMIT_REJECTED");

        CDP readback = repository.findById(r.getCdpId())
                .orElseThrow(() -> new IllegalStateException("U06_PHYSICAL_SELECTION_READBACK_MISSING"));
        Map<String, Object> saved = readback.getPatientState();
        Object raw = saved == null ? null : saved.get("questions");
        Object question = raw instanceof Map<?, ?>
                ? ((Map<?, ?>) raw).get(questionId) : null;
        if (readback.getVersion() == null
                || readback.getVersion().intValue() != committed.committedVersion.intValue()
                || !(question instanceof Map<?, ?>)
                || !d.getQuestionSelectionEffectId().equals(
                        ((Map<?, ?>) question).get("selection_effect_id"))
                || !"SELECTED".equals(((Map<?, ?>) question).get("status"))
                || saved.containsKey("pending_question"))
            throw new IllegalStateException("U06_PHYSICAL_SELECTION_READBACK_MISMATCH");

        // No delivery, no confirmation receipt and NO fabricated WAIT state.
        return new U06ExecutionResult(U06ExecutionResult.MODE2_SELECTION_PERSISTED,
                a.getAdmissionId(), d.getQuestionSelectionEffectId(), "COMMITTED",
                null, null, null, null, "DELIVERY_AUTHORITY_NOT_CONNECTED");
    }

    private U06ExecutionResult revalidate(U06ProfileBRequest r, U06SyntheticDecisionBundle d,
            U06AdmissionService.Admission a, CDP cdp) {
        Object record = cdp.getPatientState() == null ? null
                : cdp.getPatientState().get("f3_gap_assessment");
        String currentEffect = record instanceof Map<?, ?>
                ? string(((Map<?, ?>) record).get("f3_canonical_effect_id")) : null;
        String expected = d.getF3CanonicalEffectId();
        if (!U06SyntheticDecisionBundle.REVALIDATED_CURRENT.equals(d.getRevalidationStatus())
                && !U06SyntheticDecisionBundle.REASSESSMENT_REQUIRED.equals(
                        d.getRevalidationStatus()))
            return new U06ExecutionResult(U06ExecutionResult.FAILURE_REQUIRED,
                    a.getAdmissionId(), d.getRevalidationRef(), null,
                    null, null, null, null, "U06_REVALIDATION_DECISION_REQUIRED");
        if (U06SyntheticDecisionBundle.REASSESSMENT_REQUIRED.equals(d.getRevalidationStatus())
                || expected == null || !expected.equals(currentEffect))
            return new U06ExecutionResult(U06ExecutionResult.REASSESSMENT_REQUIRED,
                    a.getAdmissionId(), d.getRevalidationRef(), null,
                    null, null, null, null, null);

        // Currentness is checked against physical CDP and causes no mutation.
        // This is NOT an issued/persisted revalidation record or A1 authority.
        return new U06ExecutionResult(U06ExecutionResult.REVALIDATED_CURRENT,
                a.getAdmissionId(), d.getRevalidationRef(), null,
                null, null, null, null, null);
    }

    private static StateTypes.StatePatch patch(U06ProfileBRequest r, String purpose,
            String effectId, java.util.List<StateTypes.StatePatchOperation> operations) {
        String id = U06Ids.hash(purpose, r.getConsultationId(), r.getCdpId(), effectId);
        FoundationTypes.ContractEnvelope e = new FoundationTypes.ContractEnvelope();
        e.contractName = "StatePatch";
        e.contractVersion = ContractVersion.CONTRACT_VERSION;
        e.messageId = id;
        e.correlationId = r.getCorrelationId();
        e.traceId = r.getTraceId();
        e.createdAt = r.getCreatedAt();
        e.producer = "diagnosis-service";
        e.capabilityId = "P01";
        e.capabilityVersion = "1.0.0";
        StateTypes.StatePatch patch = new StateTypes.StatePatch();
        patch.contractVersion = ContractVersion.CONTRACT_VERSION;
        patch.envelope = e;
        patch.cdpId = r.getCdpId();
        patch.baseVersion = Integer.valueOf(r.getAuthoritativeClinicalStateVersion());
        patch.patchId = id;
        patch.idempotencyKey = U06Ids.hash("u06selectionidem", id, r.getBusinessEventIdentity());
        patch.operations = operations;
        patch.reasonCode = "U06_MODE2_QUESTION_SELECTION";
        patch.evidenceRefs = Collections.singletonList(r.getSourceAuthorityRef());
        patch.producer = "diagnosis-service";
        patch.createdAt = r.getCreatedAt();
        return patch;
    }

    private static String string(Object value) {
        return value instanceof String ? (String) value : null;
    }
    private static boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
