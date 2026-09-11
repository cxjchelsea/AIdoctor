package com.aidoctor.diagnosis.runtime.u01;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.runtime.foundation.CanonicalBusinessEventLedger;
import com.aidoctor.diagnosis.runtime.foundation.ClinicalRunCoordinator;
import com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingService;
import com.aidoctor.diagnosis.service.cdp.CDPManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** Implements U01 only: establish consultation, subject/problem framing and deterministic scope routing. */
@Service
public class U01ConsultationService {
    static final String SCOPE_VERSION = "aidoctor-v1-scope";
    static final String CAPABILITY_SET_VERSION = "aidoctor-v1-u01";
    static final String CONTRACT_VERSION = "contracts-v1";

    private final ConsultationRepository consultationRepository;
    private final U01SemanticPolicy semanticPolicy;
    private final CanonicalBusinessEventLedger eventLedger;
    private final RuntimeBindingService runtimeBindingService;
    private final ClinicalRunCoordinator runCoordinator;
    private final CDPManager cdpManager;

    public U01ConsultationService(ConsultationRepository consultationRepository,
                                  U01SemanticPolicy semanticPolicy,
                                  CanonicalBusinessEventLedger eventLedger,
                                  RuntimeBindingService runtimeBindingService,
                                  ClinicalRunCoordinator runCoordinator,
                                  CDPManager cdpManager) {
        this.consultationRepository = consultationRepository;
        this.semanticPolicy = semanticPolicy;
        this.eventLedger = eventLedger;
        this.runtimeBindingService = runtimeBindingService;
        this.runCoordinator = runCoordinator;
        this.cdpManager = cdpManager;
    }

    @Transactional
    public U01Result start(U01StartCommand command) {
        String consultationId = consultationIdFor(command.getEventId());
        String payloadDigest = digest(command);

        eventLedger.resolveOrCreate(
                command.getEventId(), consultationId, "START_CONSULTATION",
                command.getIdempotencyKey(), payloadDigest);

        Optional<ConsultationRecord> existing = consultationRepository.findById(consultationId);
        if (existing.isPresent()) {
            requireSameStart(existing.get(), command);
            return new U01Result(existing.get(), null);
        }

        U01SemanticDecision decision = semanticPolicy.decide(command);

        // U01 may reuse CDP persistence creation, but it does not use legacy direct update/orchestration paths.
        CDP cdp = cdpManager.createCDP(command.getUserId(), sessionIdFor(consultationId));

        runtimeBindingService.bind(
                consultationId,
                cdp.getId(),
                RuntimeBindingRecord.CLINICAL_RUNTIME_V1,
                SCOPE_VERSION,
                CAPABILITY_SET_VERSION,
                CONTRACT_VERSION);

        ConsultationRecord record = new ConsultationRecord(
                consultationId,
                cdp.getId(),
                command.getUserId(),
                command,
                decision,
                LocalDateTime.now());
        record = consultationRepository.save(record);

        ClinicalRunRecord run = runCoordinator.openRun(consultationId, command.getEventId(), cdp.getVersion());
        return new U01Result(record, run.getRunId());
    }

    private void requireSameStart(ConsultationRecord record, U01StartCommand command) {
        if (!record.getStartEventId().equals(command.getEventId())
                || !record.getUserId().equals(command.getUserId())) {
            throw new IllegalStateException("Existing consultation does not match canonical START_CONSULTATION semantics.");
        }
    }

    static String consultationIdFor(String eventId) {
        UUID stable = UUID.nameUUIDFromBytes(("START_CONSULTATION:" + eventId).getBytes(StandardCharsets.UTF_8));
        return "consult_" + stable.toString().replace("-", "");
    }

    private String sessionIdFor(String consultationId) {
        return "session_" + consultationId.substring("consult_".length());
    }

    private String digest(U01StartCommand command) {
        String canonical = safe(command.getUserId()) + "|"
                + safe(command.getSubjectType()) + "|"
                + safe(command.getSubjectReferenceId()) + "|"
                + safe(command.getProblemText()) + "|"
                + safe(command.getScopeIntentCandidate()) + "|"
                + command.isEarlySafetySignalPresent();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot compute canonical payload digest", e);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
