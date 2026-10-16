package com.aidoctor.diagnosis.controller;

import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionGovernedRiskSafetyContinuation;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalFollowupCoordinator;
import com.aidoctor.diagnosis.runtime.u01.U01NaturalLanguageStartService;
import com.aidoctor.diagnosis.runtime.u01.U01RawStartCommand;
import com.aidoctor.diagnosis.runtime.u01.U01Result;
import com.aidoctor.diagnosis.runtime.u02.U01U02NonProductionHandoffService;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionResult;
import com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Nonproduction, owner-gated first clinical round through persisted WAIT.
 * Clinical authorization, current state reference, delivery confirmation and
 * question decision come from independently installed host owners, not HTTP.
 * Neither this adapter nor its response claims patient-facing delivery.
 */
@RestController
@Profile("!prod & !production")
@ConditionalOnProperty(prefix = "aidoctor.v1.clinical-e2e",
        name = "first-round-wait-http-enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnBean({
        U01NaturalLanguageStartService.class,
        U01U02NonProductionHandoffService.class,
        U07NonProductionGovernedRiskSafetyContinuation.class,
        U07NonProductionPhysicalFollowupCoordinator.class,
        V1NonProductionClinicalSafetyHandoffController.AuthorizedClinicalHost.class,
        V1NonProductionFirstRoundWaitController.FirstRoundOwner.class
})
@RequestMapping("/internal/nonproduction/v1/clinical")
public final class V1NonProductionFirstRoundWaitController {
    public interface FirstRoundOwner {
        String requireCurrentAuthoritativeStateRef(String consultationId, String runId,
                U02ExecutionResult committedFact);
        U07NonProductionPhysicalFollowupCoordinator.ConfirmedDeliveryOwner
                requireIndependentConfirmedDeliveryOwner(String consultationId, String runId);
    }

    public static final class StartRequest {
        @NotBlank public String eventId;
        @NotBlank public String idempotencyKey;
        @NotBlank public String userId;
        @NotBlank public String rawText;
        public String knownSubjectReferenceId;
    }

    public static final class StartResponse {
        public final String consultationId, cdpId, runId, checkpointId, waitEffectId;
        public final String entryStatus = "DURABLE_WAIT_ESTABLISHED_NONPRODUCTION";
        public final boolean completeClinicalE2e = false;
        public final boolean patientDeliveryAllowed = false;

        StartResponse(U01Result started, U07NonProductionDeliveryWaitRecovery.WaitReceipt wait) {
            this.consultationId = started.getConsultationId();
            this.cdpId = started.getCdpId();
            this.runId = started.getRunId();
            this.checkpointId = wait.checkpointId;
            this.waitEffectId = wait.waitEffectId;
        }
    }

    private final U01NaturalLanguageStartService u01;
    private final U01U02NonProductionHandoffService u02;
    private final U07NonProductionGovernedRiskSafetyContinuation continuation;
    private final U07NonProductionPhysicalFollowupCoordinator followup;
    private final V1NonProductionClinicalSafetyHandoffController.AuthorizedClinicalHost authority;
    private final FirstRoundOwner owner;

    public V1NonProductionFirstRoundWaitController(U01NaturalLanguageStartService u01,
            U01U02NonProductionHandoffService u02,
            U07NonProductionGovernedRiskSafetyContinuation continuation,
            U07NonProductionPhysicalFollowupCoordinator followup,
            V1NonProductionClinicalSafetyHandoffController.AuthorizedClinicalHost authority,
            FirstRoundOwner owner) {
        if (u01 == null || u02 == null || continuation == null || followup == null
                || authority == null || owner == null)
            throw new IllegalArgumentException("V1_FIRST_ROUND_TRUSTED_OWNER_REQUIRED");
        this.u01 = u01;
        this.u02 = u02;
        this.continuation = continuation;
        this.followup = followup;
        this.authority = authority;
        this.owner = owner;
    }

    @PostMapping("/start-through-wait")
    public ResponseEntity<StartResponse> start(@Valid @RequestBody StartRequest request) {
        U01Result started = u01.start(new U01RawStartCommand(
                request.eventId, request.idempotencyKey, request.userId,
                request.rawText, request.knownSubjectReferenceId));
        if (started == null || !"U02".equals(started.getNextUnit()))
            throw blocked("U01_NOT_ADMITTED");
        U02ExecutionResult fact = u02.executeCommittedStart(
                started.getConsultationId(), started.getRunId(), request.rawText);
        if (fact == null || !"COMMITTED".equals(fact.status)
                || fact.commitResult == null
                || !"COMMITTED".equals(fact.commitResult.status))
            throw blocked("U02_COMMIT_REQUIRED");

        V1NonProductionClinicalSafetyHandoffController.AuthorizedExecution approved =
                authority.requireCurrent(started.getConsultationId(), started.getRunId(), fact);
        V1NonProductionClinicalSafetyHandoffController.requireBoundAuthorizedExecution(
                started, fact, approved);
        V1NonProductionClinicalSafetyHandoffController.requireNonProductionEnvironment(
                approved.environmentId);
        V1NonProductionClinicalSafetyHandoffController.requireFrozenSafetyRelease(approved);

        String stateRef = owner.requireCurrentAuthoritativeStateRef(
                started.getConsultationId(), started.getRunId(), fact);
        U07NonProductionPhysicalFollowupCoordinator.ConfirmedDeliveryOwner delivery =
                owner.requireIndependentConfirmedDeliveryOwner(
                        started.getConsultationId(), started.getRunId());
        if (stateRef == null || stateRef.trim().isEmpty() || delivery == null)
            throw blocked("INDEPENDENT_DELIVERY_AND_STATE_AUTHORITY_REQUIRED");

        U07NonProductionDeliveryWaitRecovery.WaitReceipt wait =
                followup.firstRoundToDurableWait(fact, approved.command,
                        approved.releases, approved.environmentId, approved.scope,
                        stateRef, continuation, delivery);
        if (wait == null)
            throw blocked("DURABLE_WAIT_NOT_ESTABLISHED");
        return ResponseEntity.ok(new StartResponse(started, wait));
    }

    private static IllegalStateException blocked(String reason) {
        return new IllegalStateException("V1_FIRST_ROUND_BLOCKED:" + reason);
    }
}
