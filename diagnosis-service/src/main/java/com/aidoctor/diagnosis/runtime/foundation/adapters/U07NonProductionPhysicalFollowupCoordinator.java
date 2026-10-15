package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u03.U03OutboundHandoff;
import com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRecord;
import com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingService;
import com.aidoctor.diagnosis.runtime.u04.U04ExecutionResult;
import com.aidoctor.diagnosis.runtime.u05.U05DownstreamRoutingDecision;
import com.aidoctor.diagnosis.runtime.u05.U05ExecutionResult;
import com.aidoctor.diagnosis.runtime.u06.*;
import java.util.Objects;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final U06NonProductionPhysicalMode23Service u06;
    private final U07NonProductionDeliveryWaitRecovery recovery;
    private final CDPRepository cdps;
    private final AuthorizedQuestionHost host;
    private final TransactionTemplate selectionTransactions;
    private final RuntimeBindingService bindings;
    private final ClinicalRunRepository runs;

    public U07NonProductionPhysicalFollowupCoordinator(
            U07NonProductionF3A1U05Flow f3A1U05,
            U05QuestionRouteAdmissionAdapter u05U06,
            U06NonProductionPhysicalMode23Service u06,
            U07NonProductionDeliveryWaitRecovery recovery,
            CDPRepository cdps, RuntimeBindingService bindings,
            ClinicalRunRepository runs, TransactionTemplate selectionTransactions,
            AuthorizedQuestionHost host) {
        if (f3A1U05 == null || u05U06 == null || u06 == null
                || recovery == null || cdps == null || bindings == null
                || runs == null || selectionTransactions == null || host == null)
            throw new IllegalArgumentException("U07_PHYSICAL_FOLLOWUP_DEPENDENCIES_REQUIRED");
        this.f3A1U05 = f3A1U05;
        this.u05U06 = u05U06;
        this.u06 = u06;
        this.recovery = recovery;
        this.cdps = cdps;
        this.host = host;
        this.bindings = bindings;
        this.runs = runs;
        this.selectionTransactions = selectionTransactions;
    }

    /**
     * First-round entry for a newly committed U02 clinical observation.
     * A fresh P05 trace, independent accepted-evidence admission, frozen Gate-C
     * evaluation and the actual U03/P01/S14/U04 commits are mandatory BEFORE
     * F3/A1 or a question selection may be attempted. No authority is minted.
     *
     * Failed/insufficient or blocked U04 assessments fail closed in select(),
     * before the F3 owner can mutate clinical state.
     */
    public Selected selectFromCommittedU02(
            com.aidoctor.diagnosis.runtime.u02.U02ExecutionResult fact,
            com.aidoctor.diagnosis.runtime.u03.U03ExecutionCommand command,
            com.aidoctor.diagnosis.runtime.u03.U03ExplicitNonProductionReleaseRefs releases,
            String environmentId,
            com.aidoctor.diagnosis.runtime.u04.U04ScopeContext scope,
            String authoritativeStateRef,
            U07NonProductionGovernedRiskSafetyContinuation continuation) {
        if (continuation == null || command == null)
            throw blocked("GOVERNED_RISK_CONTINUATION_REQUIRED");
        // Validate existing durable clinical authority BEFORE U03/P01 can
        // commit risk or U04 can mutate the safety gate. Checking only after
        // these commits would allow an invalid Run/Thread to change the CDP.
        requireDurableRuntimeForCommandPreRisk(command, bindings, runs,
                host.threadId(), host.runId());
        U07NonProductionGovernedRiskSafetyContinuation.Result firstRound =
                continuation.executeFromCommittedU02(
                        fact, command, releases, environmentId, scope);
        if (firstRound == null || firstRound.handoff == null || firstRound.gate == null
                || command == null)
            throw blocked("GOVERNED_RISK_SAFETY_COMMIT_REQUIRED");
        // The U03 handoff must belong to this very U02-backed clinical run.
        // An otherwise valid gate for a different consultation or older run
        // may never drive F3, question selection, delivery or durable WAIT.
        if (!Objects.equals(command.consultationId, firstRound.handoff.getConsultationId())
                || !Objects.equals(command.cdpId, firstRound.handoff.getCdpId())
                || !Objects.equals(command.threadId, firstRound.handoff.getThreadId())
                || !Objects.equals(command.runId, firstRound.handoff.getRunId())
                || !Objects.equals(command.eventId, firstRound.handoff.getEventId()))
            throw blocked("GOVERNED_FIRST_ROUND_IDENTITY_MISMATCH");
        U07NonProductionF3A1U05Flow.requireEligiblePreF3(
                firstRound.handoff, firstRound.gate);
        return select(firstRound.handoff, firstRound.gate,
                environmentId, authoritativeStateRef);
    }

    /**
     * Pre-risk equivalent of the pre-F3 durable authority fence. The command
     * must be bound to a persisted consultation, thread and OPEN clinical Run.
     */
    static void requireDurableRuntimeForCommandPreRisk(
            com.aidoctor.diagnosis.runtime.u03.U03ExecutionCommand command,
            RuntimeBindingService bindings, ClinicalRunRepository runs,
            String hostThreadId, String hostRunId) {
        if (command == null || bindings == null || runs == null)
            throw blocked("PRE_RISK_DURABLE_AUTHORITY_MISSING");
        RuntimeBindingRecord binding = bindings.requireBinding(command.consultationId);
        ClinicalRunRecord run = runs.findById(command.runId)
                .orElseThrow(() -> blocked("PRE_RISK_RUN_NOT_FOUND"));
        if (!RuntimeBindingRecord.CLINICAL_RUNTIME_V1.equals(binding.getRuntimeAuthority())
                || !Objects.equals(command.cdpId, binding.getCdpId())
                || !Objects.equals(command.threadId, binding.getThreadId())
                || !Objects.equals(command.threadId, run.getThreadId())
                || !Objects.equals(command.consultationId, run.getConsultationId())
                || !Objects.equals(command.eventId, run.getEventId())
                || !Objects.equals(command.threadId, hostThreadId)
                || !Objects.equals(command.runId, hostRunId)
                || !ClinicalRunRecord.OPEN.equals(run.getStatus()))
            throw blocked("PRE_RISK_DURABLE_AUTHORITY_MISMATCH");
    }

    /**
     * Persists exactly one selected question through the existing governed U06
     * P01 owner. No delivery, checkpoint or external side effect occurs here.
     */
    public Selected select(U03OutboundHandoff handoff, U04ExecutionResult u04,
            String environmentId, String authoritativeStateRef) {
        U07NonProductionF3A1U05Flow.requireEligiblePreF3(handoff, u04);
        // A once-valid U04 result is not a durable permit. Re-read the actual
        // CDP version before entering the F3 side-effect owner, otherwise a
        // second clinical update could silently reuse the previous gate.
        requireCurrentU04Version(handoff.getCdpId(), u04, cdps);
        // Durable identity must be admitted BEFORE the F3 owner writes state.
        requireDurableRuntimePreF3(handoff, bindings, runs, host.threadId(), host.runId());
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
        U06ExecutionResult selected = selectionTransactions.execute(
                status -> u06.execute(request, decision, null));
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
                || !Objects.equals(selected.request.getConsultationId(),
                        receipt.request.getConsultationId())
                || !Objects.equals(selected.request.getCdpId(), receipt.request.getCdpId())
                || !Objects.equals(selected.request.getSourceAuthorityRef(),
                        receipt.request.getSourceAuthorityRef())
                || selected.request.getAuthoritativeClinicalStateVersion()
                        != receipt.request.getAuthoritativeClinicalStateVersion()
                || !Objects.equals(selected.decision.getQuestionId(),
                        receipt.decision.getQuestionId())
                || !Objects.equals(selected.decision.getQuestionSelectionEffectId(),
                        receipt.decision.getQuestionSelectionEffectId())
                || !Objects.equals(selected.decision.getQuestionContentFingerprint(),
                        receipt.decision.getQuestionContentFingerprint())
                || !Objects.equals(selected.decision.getQuestionSelectionEffectId(),
                        selected.selection.getEffectRef()))
            throw blocked("CONFIRMED_RECEIPT_SELECTION_MISMATCH");
        recovery.accept(receipt);
        return recovery.recover(receipt);
    }

    /**
     * Independently check persisted consultation/Thread/Run before mutating F3.
     * Exposed for physical acceptance checks; it issues no authority.
     */
    public static void requireDurableRuntimePreF3(U03OutboundHandoff handoff,
            RuntimeBindingService bindings, ClinicalRunRepository runs,
            String threadId, String runId) {
        if (handoff == null || bindings == null || runs == null)
            throw blocked("DURABLE_RUNTIME_AUTHORITY_MISSING");
        RuntimeBindingRecord binding = bindings.requireBinding(handoff.getConsultationId());
        ClinicalRunRecord activeRun = runs.findById(handoff.getRunId())
                .orElseThrow(() -> blocked("RUN_NOT_FOUND"));
        if (!RuntimeBindingRecord.CLINICAL_RUNTIME_V1.equals(binding.getRuntimeAuthority())
                || !Objects.equals(handoff.getCdpId(), binding.getCdpId())
                || !Objects.equals(handoff.getConsultationId(), activeRun.getConsultationId())
                || !Objects.equals(handoff.getThreadId(), binding.getThreadId())
                || !Objects.equals(binding.getThreadId(), activeRun.getThreadId())
                || !Objects.equals(handoff.getEventId(), activeRun.getEventId())
                || !Objects.equals(threadId, binding.getThreadId())
                || !Objects.equals(runId, activeRun.getRunId())
                || !ClinicalRunRecord.OPEN.equals(activeRun.getStatus()))
            throw blocked("DURABLE_THREAD_RUN_AUTHORITY_MISMATCH");

    }

    /** Independent persisted-currentness guard, exercised by disposable MySQL acceptance. */
    static void requireCurrentU04Version(String cdpId, U04ExecutionResult gate,
            CDPRepository cdps) {
        if (cdpId == null || gate == null || cdps == null
                || gate.getCommitResult() == null
                || !"COMMITTED".equals(gate.getCommitResult().status)
                || gate.getCommitResult().committedVersion == null
                || !cdpId.equals(gate.getCommitResult().cdpId))
            throw blocked("U04_COMMIT_AUTHORITY_MISSING");
        CDP current = cdps.findById(cdpId)
                .orElseThrow(() -> blocked("CDP_NOT_FOUND"));
        if (current.getVersion() == null
                || !current.getVersion().equals(gate.getCommitResult().committedVersion))
            throw blocked("U04_SAFETY_GATE_STALE_BEFORE_F3");
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
