package com.aidoctor.diagnosis.runtime.u01;

import com.aidoctor.diagnosis.dto.capability.c01.C01U01CapabilityResponse;
import com.aidoctor.diagnosis.runtime.u01.capability.C01U01CapabilityGateway;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * Internal natural-language ingress for U01. This is not a Phase 10 HTTP adapter.
 *
 * raw input -> C01 candidates -> U01 deterministic business interpretation.
 */
@Service
public class U01NaturalLanguageStartService {
    private final ConsultationRepository consultationRepository;
    private final C01U01CapabilityGateway c01Gateway;
    private final U01ConsultationService u01ConsultationService;

    public U01NaturalLanguageStartService(
            ConsultationRepository consultationRepository,
            C01U01CapabilityGateway c01Gateway,
            U01ConsultationService u01ConsultationService) {
        this.consultationRepository = consultationRepository;
        this.c01Gateway = c01Gateway;
        this.u01ConsultationService = u01ConsultationService;
    }

    public U01Result start(U01RawStartCommand raw) {
        String consultationId = U01ConsultationService.consultationIdFor(raw.getEventId());
        String sourceFingerprint = sourceFingerprint(raw);

        // Canonical replay must return the already committed U01 outcome without
        // invoking a capability again. Capability implementation changes cannot
        // rewrite a historical START_CONSULTATION interpretation.
        Optional<ConsultationRecord> existing = consultationRepository.findById(consultationId);
        if (existing.isPresent()) {
            return u01ConsultationService.start(new U01StartCommand(
                    raw.getEventId(),
                    raw.getIdempotencyKey(),
                    raw.getUserId(),
                    null,
                    null,
                    null,
                    null,
                    false,
                    sourceFingerprint));
        }

        C01U01CapabilityResponse capability = c01Gateway.interpret(
                raw.getUserId(),
                raw.getRawText(),
                consultationId,
                raw.getKnownSubjectReferenceId());

        String subjectType = capability.getSubjectCandidate().getSubjectType();
        String subjectReferenceId = "OTHER".equals(subjectType)
                ? raw.getKnownSubjectReferenceId()
                : null;

        U01StartCommand interpreted = new U01StartCommand(
                raw.getEventId(),
                raw.getIdempotencyKey(),
                raw.getUserId(),
                subjectType,
                subjectReferenceId,
                capability.getProblemCandidate().getText(),
                capability.getScopeCandidate().getScope(),
                capability.getEarlySafetySignalCandidate().isDetected(),
                sourceFingerprint);

        return u01ConsultationService.start(interpreted);
    }

    private String sourceFingerprint(U01RawStartCommand raw) {
        String canonical = raw.getRawText() + "|subjectRef="
                + (raw.getKnownSubjectReferenceId() == null ? "" : raw.getKnownSubjectReferenceId());
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot compute raw U01 source fingerprint", e);
        }
    }
}
