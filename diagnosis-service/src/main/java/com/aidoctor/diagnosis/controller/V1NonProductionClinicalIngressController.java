package com.aidoctor.diagnosis.controller;

import com.aidoctor.diagnosis.runtime.u01.U01NaturalLanguageStartService;
import com.aidoctor.diagnosis.runtime.u01.U01RawStartCommand;
import com.aidoctor.diagnosis.runtime.u01.U01Result;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Explicitly gated real U01 entry on a separate NONPRODUCTION URL.
 *
 * It invokes the existing governed U01 natural language consultation service,
 * not the legacy five-step diagnosis orchestrator. This is an ingress-only
 * integration point: U01's nextUnit is a routing REQUEST, not evidence that
 * U02/U03/U04 or final DDx actually executed.
 *
 * No synthetic Thread IDs, no automatically allowed safety decision,
 * no delivery to a real patient and no production cutover.
 */
@RestController
@Profile("!prod & !production")
@ConditionalOnProperty(prefix = "aidoctor.v1.clinical-e2e",
        name = "nonproduction-entry-enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnBean(U01NaturalLanguageStartService.class)
@RequestMapping("/internal/nonproduction/v1/clinical")
public final class V1NonProductionClinicalIngressController {
    private final U01NaturalLanguageStartService u01;

    public V1NonProductionClinicalIngressController(U01NaturalLanguageStartService u01) {
        if (u01 == null) throw new IllegalArgumentException("V1_U01_HOST_REQUIRED");
        this.u01 = u01;
    }

    public static final class StartRequest {
        @NotBlank public String eventId;
        @NotBlank public String idempotencyKey;
        @NotBlank public String userId;
        @NotBlank public String rawText;
        public String knownSubjectReferenceId;
    }

    public static final class StartResponse {
        public final String consultationId;
        public final String cdpId;
        public final String runId;
        public final String nextUnit;
        public final String lifecycleStatus;
        public final String entryStatus;
        public final boolean completeClinicalE2e;
        public final boolean patientDeliveryAllowed;

        StartResponse(U01Result result) {
            this.consultationId = result.getConsultationId();
            this.cdpId = result.getCdpId();
            this.runId = result.getRunId();
            this.nextUnit = result.getNextUnit();
            this.lifecycleStatus = result.getLifecycleStatus();
            this.entryStatus = "U01_COMMITTED_DOWNSTREAM_NOT_EXECUTED";
            this.completeClinicalE2e = false;
            this.patientDeliveryAllowed = false;
        }
    }

    @PostMapping("/start")
    public ResponseEntity<StartResponse> start(@Valid @RequestBody StartRequest request) {
        U01Result result = u01.start(new U01RawStartCommand(
                request.eventId, request.idempotencyKey, request.userId,
                request.rawText, request.knownSubjectReferenceId));
        return ResponseEntity.ok(new StartResponse(result));
    }
}
