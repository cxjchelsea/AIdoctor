package com.aidoctor.diagnosis.controller;

import com.aidoctor.diagnosis.runtime.u01.U01NaturalLanguageStartService;
import com.aidoctor.diagnosis.runtime.u01.U01RawStartCommand;
import com.aidoctor.diagnosis.runtime.u01.U01Result;
import com.aidoctor.diagnosis.runtime.u02.U01U02NonProductionHandoffService;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionResult;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Limited, nonproduction HTTP handoff: real U01 commit then governed U02/P01.
 * This is NOT a full clinical answer, a safety approval, or patient delivery.
 * The installed host must supply independent C01 and trusted trace authority.
 */
@RestController
@Profile("!prod & !production")
@ConditionalOnProperty(prefix = "aidoctor.v1.clinical-e2e",
        name = "u01-u02-http-enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnBean({U01NaturalLanguageStartService.class,
        U01U02NonProductionHandoffService.class})
@RequestMapping("/internal/nonproduction/v1/clinical")
public final class V1NonProductionClinicalStartHandoffController {
    private final U01NaturalLanguageStartService u01;
    interface TrustedFactCommit {
        U02ExecutionResult commit(String consultationId, String runId, String rawText);
    }

    private final TrustedFactCommit u02;

    @Autowired
    public V1NonProductionClinicalStartHandoffController(
            U01NaturalLanguageStartService u01,
            U01U02NonProductionHandoffService u02) {
        this(u01, u02 == null ? null : u02::executeCommittedStart);
    }

    V1NonProductionClinicalStartHandoffController(
            U01NaturalLanguageStartService u01, TrustedFactCommit u02) {
        if (u01 == null || u02 == null)
            throw new IllegalArgumentException("V1_U01_U02_TRUSTED_HOST_REQUIRED");
        this.u01 = u01;
        this.u02 = u02;
    }

    public static final class StartRequest {
        @NotBlank public String eventId;
        @NotBlank public String idempotencyKey;
        @NotBlank public String userId;
        @NotBlank public String rawText;
        public String knownSubjectReferenceId;
    }

    public static final class StartResult {
        public final String consultationId;
        public final String cdpId;
        public final String runId;
        public final String entryStatus = "U02_COMMITTED_DOWNSTREAM_NOT_EXECUTED";
        public final boolean completeClinicalE2e = false;
        public final boolean patientDeliveryAllowed = false;

        StartResult(U01Result u01) {
            consultationId = u01.getConsultationId();
            cdpId = u01.getCdpId();
            runId = u01.getRunId();
        }
    }

    @PostMapping("/start-through-u02")
    public ResponseEntity<StartResult> start(@Valid @RequestBody StartRequest request) {
        U01Result result = u01.start(new U01RawStartCommand(
                request.eventId, request.idempotencyKey, request.userId,
                request.rawText, request.knownSubjectReferenceId));
        if (result == null || !"U02".equals(result.getNextUnit()))
            throw new IllegalStateException("V1_U01_U02_ROUTE_NOT_AUTHORIZED");
        U02ExecutionResult fact = u02.commit(
                result.getConsultationId(), result.getRunId(), request.rawText);
        if (fact == null || !"COMMITTED".equals(fact.status)
                || fact.commitResult == null
                || !"COMMITTED".equals(fact.commitResult.status))
            throw new IllegalStateException("V1_U02_CLINICAL_FACT_NOT_COMMITTED");
        return ResponseEntity.ok(new StartResult(result));
    }
}
