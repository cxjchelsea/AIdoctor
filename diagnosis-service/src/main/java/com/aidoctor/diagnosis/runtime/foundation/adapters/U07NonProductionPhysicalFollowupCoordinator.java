package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u03.U03OutboundHandoff;
import com.aidoctor.diagnosis.runtime.u04.U04ExecutionResult;
import com.aidoctor.diagnosis.runtime.u05.U05DownstreamRoutingDecision;
import com.aidoctor.diagnosis.runtime.u05.U05ExecutionResult;
import com.aidoctor.diagnosis.runtime.u06.*;
import java.util.Objects;

/**
 * Explicit NON-PRODUCTION physical continuation across EXISTING owners:
 * U04 -> durable F3/A1 -> U05 -> authorized U06 selection -> independent
 * confirmed-delivery receipt -> durable WAIT.
 *
 * This coordinator neither issues clinical authority nor delivers questions.
 * Selection is never mistaken for delivery or for an established WAIT.
 */
public final class U07NonProductionPhysicalFollowupCoordinator {
    public interface AuthorizedQuestionHost {
        U06AdmissionEvidence requireCurrentAdmissionEvidence(U05ExecutionResult readiness);
        String questionPolicyRef();
        String d04PolicyRef();
        String dependencyBindingRef();
        String threadId();
        String runId();
        long expectedConsultationRowVersion();
        /** Governed F3/D04/clinical candidate decision from the owning host. */
        U06SyntheticDecisionBundle requireCurrentQuestionDecision(
                U06ProfileBRequest admittedRequest, int currentCdpVersion);
    }

    public static final class Selected {
        public final U05ExecutionResult readiness;
        public final U06ProfileBRequest request;
        public final U06SyntheticDecisionBundle decision;
        public final U06ExecutionResult selection;
        private Selected(U05ExecutionResult readiness, U06ProfileBRequest request,
                U06SyntheticDecisionBundle decision, U06ExecutionResult selection) {
            this.readiness = readiness;
            this.request = request;
            this.decision = decision;
            this.selection = selection;
        }
    }

    private final U07NonProductionF3A1U05Flow f3A1U05;
    private final U05QuestionRouteAdmissionAdapter u05U06;
    private final U06ProfileBApplicationService u06;
    private final U07NonProductionDeliveryWaitRecovery recovery;
    private final CDPRepository cdps;
    private final AuthorizedQuestionHost host;

    public U07NonProductionPhysicalFollowupCoordinator(
            U07NonProductionF3A1U05Flow f3A1U05,
            U05QuestionRouteAdmissionAdapter u05U06,
            U06ProfileBApplicationService u06,
            U07NonProductionDeliveryWaitRecovery recovery,
            CDPRepository cdps, AuthorizedQuestionHost host) {
        if (f3A1U05 == null || u05U06 == null || u06 == null
                || recovery == null || cdps == null || host == null)
            throw new IllegalArgumentException("U07_PHYSICAL_FOLLOWUP_DEPENDENCIES_REQUIRED");
        this.f3A1U05 = f3A1U05;
        this.u05U06 = u05U06;
        this.u06 = u06;
        this.recovery = recovery;
        this.cdps = cdps;
        this.host = host;
    }

    /**
     * Persists exactly one selected question through the existing governed U06
     * P01 owner. No delivery, checkpoint or external side effect occurs here.
     */
    public Selected select(U03OutboundHandoff handoff, U04ExecutionResult u04,
            String environmentId, String authoritativeStateRef) {
        U07NonProductionF3A1U05Flow.requireEligiblePreF3(handoff, u04);
        U05ExecutionResult readiness = f3A1U05.execute(
                handoff, u04, environmentId, authoritativeStateRef);
        if (readiness == null
                || !U05ExecutionResult.ROUTING_COMPLETE.equals(readiness.getStatus())
                || readiness.getCommitResult() == null
                || !"COMMITTED".equals(readiness.getCommitResult().status)
                || readiness.getRoutingDecision() == null)
            throw blocked("U05_NOT_COMMITTED");
        U05DownstreamRoutingDecision route = readiness.getRoutingDecision();
        if (!U05DownstreamRoutingDecision.ELIGIBLE.equals(route.getRoutingStatus())
                || !U05DownstreamRoutingDecision.TO_U06_QUESTION_PATH.equals(
                        route.getDownstreamConsequence())
                || !"U06".equals(route.getTargetUnitId()))
            throw blocked("QUESTION_ROUTE_NOT_CURRENT");
        int version = currentVersion(route.getCdpId());
        U06AdmissionEvidence evidence = host.requireCurrentAdmissionEvidence(readiness);
        if (evidence == null) throw blocked("INDEPENDENT_U06_ADMISSION_MISSING");
        U06AdmissionService.Admission admitted = u05U06.admit(readiness, evidence,
                version, host.questionPolicyRef(), host.d04PolicyRef(),
                host.dependencyBindingRef(), host.threadId(), host.runId(),
                host.expectedConsultationRowVersion());
        if (admitted == null || !admitted.isAdmitted())
            throw blocked("U06_ADMISSION_REJECTED");
        U06ProfileBRequest request = admitted.getRequest();
        U06SyntheticDecisionBundle decision =
                host.requireCurrentQuestionDecision(request, version);
        if (decision == null
                || !U06SyntheticDecisionBundle.SELECTED.equals(
                        decision.getQuestionSelectionStatus())
                || !U06SyntheticDecisionBundle.CONTINUE.equals(decision.getD04Status())
                || decision.getQuestionId() == null)
            throw blocked("GOVERNED_QUESTION_DECISION_NOT_SELECTED");
        // The physical Mode-2 owner checks the CDP F3 effect, D04 and version
        // again, commits P01 and performs exact readback.
        U06ExecutionResult selected = u06.execute(request, decision, null);
        if (selected == null || !U06ExecutionResult.MODE2_SELECTION_PERSISTED.equals(
                selected.getStatus())
                || !Objects.equals(decision.getQuestionSelectionEffectId(),
                        selected.getEffectRef())
                || currentVersion(route.getCdpId()) != version + 1)
            throw blocked("PHYSICAL_QUESTION_SELECTION_NOT_COMMITTED");
        return new Selected(readiness, request, decision, selected);
    }

    /**
     * Called ONLY after a separate delivery owner has returned a confirmed
     * receipt. The existing recovery service independently verifies the receipt
     * and binds physical CDP delivery to durable Consultation/Runtime WAIT.
     */
    public U07NonProductionDeliveryWaitRecovery.WaitReceipt confirmedDeliveryToWait(
            Selected selected, U07NonProductionDeliveryWaitRecovery.Receipt receipt) {
        if (selected == null || receipt == null
                || !U06ExecutionResult.MODE2_SELECTION_PERSISTED.equals(
                        selected.selection.getStatus())
                || selected.request != receipt.request || selected.decision != receipt.decision
                || !Objects.equals(selected.decision.getQuestionSelectionEffectId(),
                        selected.selection.getEffectRef()))
            throw blocked("CONFIRMED_RECEIPT_SELECTION_MISMATCH");
        recovery.accept(receipt);
        return recovery.recover(receipt);
    }

    private int currentVersion(String cdpId) {
        CDP current = cdps.findById(cdpId)
                .orElseThrow(() -> blocked("CDP_NOT_FOUND"));
        Integer version = current.getVersion();
        if (version == null || version.intValue() < 0)
            throw blocked("CURRENT_CDP_VERSION_INVALID");
        return version.intValue();
    }

    private static IllegalStateException blocked(String why) {
        return new IllegalStateException("U07_FOLLOWUP_BLOCKED:" + why);
    }
}
