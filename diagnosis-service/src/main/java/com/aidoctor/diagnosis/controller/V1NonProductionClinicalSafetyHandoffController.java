package com.aidoctor.diagnosis.controller;

import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionGovernedRiskSafetyContinuation;
import com.aidoctor.diagnosis.runtime.u01.U01NaturalLanguageStartService;
import com.aidoctor.diagnosis.runtime.u01.U01RawStartCommand;
import com.aidoctor.diagnosis.runtime.u01.U01Result;
import com.aidoctor.diagnosis.runtime.u02.U01U02NonProductionHandoffService;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionResult;
import com.aidoctor.diagnosis.runtime.u03.U03ExecutionCommand;
import com.aidoctor.diagnosis.runtime.u03.U03ExplicitNonProductionReleaseRefs;
import com.aidoctor.diagnosis.runtime.u04.U04ExecutionResult;
import com.aidoctor.diagnosis.runtime.u04.U04ScopeContext;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Gated first-round clinical safety execution only. Neither patient delivery
 * nor downstream question selection is permitted at this ingress.
 * Authorities are resolved by a separately installed trusted host, never the caller.
 */
@RestController
@Profile("!prod & !production")
@ConditionalOnProperty(prefix = "aidoctor.v1.clinical-e2e",
        name = "u01-u04-http-enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnBean({U01NaturalLanguageStartService.class,
        U01U02NonProductionHandoffService.class,
        U07NonProductionGovernedRiskSafetyContinuation.class,
        V1NonProductionClinicalSafetyHandoffController.AuthorizedClinicalHost.class})
@RequestMapping("/internal/nonproduction/v1/clinical")
public final class V1NonProductionClinicalSafetyHandoffController {
    /**
     * Resolves already-issued, durable execution authorization. This interface
     * must not fabricate release refs, acceptance or clinical policy.
     */
    public interface AuthorizedClinicalHost {
        AuthorizedExecution requireCurrent(String consultationId, String runId,
                U02ExecutionResult fact);
    }

    public static final class AuthorizedExecution {
        public final U03ExecutionCommand command;
        public final U03ExplicitNonProductionReleaseRefs releases;
        public final String environmentId;
        public final U04ScopeContext scope;

        public AuthorizedExecution(U03ExecutionCommand command,
                U03ExplicitNonProductionReleaseRefs releases,
                String environmentId, U04ScopeContext scope) {
            if (command == null || releases == null || scope == null
                    || environmentId == null || environmentId.trim().isEmpty())
                throw new IllegalArgumentException("V1_AUTHORIZED_EXECUTION_REQUIRED");
            this.command = command;
            this.releases = releases;
            this.environmentId = environmentId;
            this.scope = scope;
        }
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
        public final String entryStatus = "U04_COMMITTED_DOWNSTREAM_NOT_EXECUTED";
        public final boolean completeClinicalE2e = false;
        public final boolean patientDeliveryAllowed = false;

        StartResponse(U01Result started) {
            this.consultationId = started.getConsultationId();
            this.cdpId = started.getCdpId();
            this.runId = started.getRunId();
        }
    }

    private final U01NaturalLanguageStartService u01;
    private final U01U02NonProductionHandoffService u02;
    private final U07NonProductionGovernedRiskSafetyContinuation riskSafety;
    private final AuthorizedClinicalHost host;

    public V1NonProductionClinicalSafetyHandoffController(
            U01NaturalLanguageStartService u01,
            U01U02NonProductionHandoffService u02,
            U07NonProductionGovernedRiskSafetyContinuation riskSafety,
            AuthorizedClinicalHost host) {
        if (u01 == null || u02 == null || riskSafety == null || host == null)
            throw new IllegalArgumentException("V1_TRUSTED_CLINICAL_HOST_REQUIRED");
        this.u01 = u01;
        this.u02 = u02;
        this.riskSafety = riskSafety;
        this.host = host;
    }

    @PostMapping("/start-through-u04")
    public ResponseEntity<StartResponse> start(@Valid @RequestBody StartRequest request) {
        U01Result started = u01.start(new U01RawStartCommand(
                request.eventId, request.idempotencyKey, request.userId,
                request.rawText, request.knownSubjectReferenceId));
        if (started == null || !"U02".equals(started.getNextUnit()))
            throw blocked("U01_ROUTE_NOT_AUTHORIZED");

        U02ExecutionResult fact = u02.executeCommittedStart(
                started.getConsultationId(), started.getRunId(), request.rawText);
        if (fact == null || !"COMMITTED".equals(fact.status)
                || fact.commitResult == null
                || !"COMMITTED".equals(fact.commitResult.status))
            throw blocked("U02_COMMIT_REQUIRED");

        AuthorizedExecution authorized = host.requireCurrent(
                started.getConsultationId(), started.getRunId(), fact);
        requireBoundAuthorizedExecution(started, fact, authorized);

        requireNonProductionEnvironment(authorized.environmentId);
        requireFrozenSafetyRelease(authorized);
        U07NonProductionGovernedRiskSafetyContinuation.Result result =
                riskSafety.executeFromCommittedU02(fact, authorized.command,
                        authorized.releases, authorized.environmentId, authorized.scope);
        U04ExecutionResult gate = result == null ? null : result.gate;
        if (gate == null
                || !U04ExecutionResult.COMMIT_COMPLETE.equals(gate.getStatus())
                || gate.getCommitResult() == null
                || !"COMMITTED".equals(gate.getCommitResult().status)
                || !started.getCdpId().equals(gate.getCommitResult().cdpId))
            throw blocked("U04_SAFETY_COMMIT_REQUIRED");
        return ResponseEntity.ok(new StartResponse(started));
    }

    static void requireBoundAuthorizedExecution(U01Result started,
            U02ExecutionResult fact, AuthorizedExecution authorized) {
        if (started == null || fact == null || fact.commitResult == null
                || authorized == null
                || !started.getConsultationId().equals(authorized.command.consultationId)
                || !started.getRunId().equals(authorized.command.runId)
                || !started.getCdpId().equals(authorized.command.cdpId)
                || fact.commitResult.committedVersion == null
                || fact.commitResult.committedVersion.intValue()
                    != authorized.command.clinicalStateVersion)
            throw blocked("U03_DURABLE_AUTHORITY_MISMATCH");
    }

    static void requireFrozenSafetyRelease(AuthorizedExecution authorized) {
        if (authorized == null || authorized.releases == null || authorized.scope == null)
            throw blocked("CLINICAL_RELEASE_SCOPE_REQUIRED");
        try {
            authorized.releases.requireGateCFrozenSet();
            authorized.scope.requireFrozenPolicy();
        } catch (IllegalStateException ex) {
            throw blocked("CLINICAL_RELEASE_SCOPE_NOT_AUTHORIZED");
        }
        if (!authorized.scope.isEstablished() || !authorized.scope.isApplicable())
            throw blocked("CLINICAL_SCOPE_NOT_ESTABLISHED");
    }

    static void requireNonProductionEnvironment(String environment) {
        if (environment == null || environment.trim().isEmpty())
            throw blocked("ENVIRONMENT_REQUIRED");
        String value = environment.trim().toLowerCase(java.util.Locale.ROOT);
        if ("prod".equals(value) || "production".equals(value)
                || value.startsWith("prod-") || value.startsWith("production-"))
            throw blocked("PRODUCTION_FORBIDDEN");
    }

    private static IllegalStateException blocked(String reason) {
        return new IllegalStateException("V1_HTTP_CLINICAL_SAFETY_BLOCKED:" + reason);
    }
}
