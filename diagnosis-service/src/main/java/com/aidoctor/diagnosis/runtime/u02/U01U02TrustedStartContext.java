package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.diagnosis.runtime.foundation.*;
import com.aidoctor.diagnosis.runtime.u01.*;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.entity.CDP;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Read-only, durable authority resolution for U01 -> U02 START handoff.
 * This does not mint identifiers, admit new business events, or bypass P01.
 */
@Service
public final class U01U02TrustedStartContext {
    private final ConsultationRepository consultations;
    private final RuntimeBindingService bindings;
    private final ClinicalRunRepository runs;
    private final CanonicalBusinessEventRepository events;
    private final CDPRepository cdps;

    public U01U02TrustedStartContext(ConsultationRepository consultations,
            RuntimeBindingService bindings, ClinicalRunRepository runs,
            CanonicalBusinessEventRepository events, CDPRepository cdps) {
        this.consultations = consultations;
        this.bindings = bindings;
        this.runs = runs;
        this.events = events;
        this.cdps = cdps;
    }

    @Transactional(readOnly = true)
    public VerifiedStart require(String consultationId, String runId, String rawClinicalText) {
        required(rawClinicalText);
        ConsultationRecord consultation = consultations.findById(required(consultationId))
                .orElseThrow(() -> blocked("CONSULTATION_NOT_FOUND"));
        if (!ConsultationRecord.ACTIVE.equals(consultation.getLifecycleStatus())
                || !U01SemanticDecision.SUBJECT_RESOLVED.equals(consultation.getSubjectStatus())
                || !U01SemanticDecision.PROBLEM_FRAMED.equals(consultation.getProblemStatus())
                || !U01SemanticDecision.IN_SCOPE.equals(consultation.getScopeDecision())
                || consultation.isEarlySafetySignal()
                || consultation.getCurrentWaitEffectId() != null) {
            throw blocked("U01_NOT_ELIGIBLE_FOR_U02");
        }

        RuntimeBindingRecord binding = bindings.requireBinding(consultationId);
        if (!RuntimeBindingRecord.CLINICAL_RUNTIME_V1.equals(binding.getRuntimeAuthority())
                || !consultation.getCdpId().equals(binding.getCdpId())) {
            throw blocked("RUNTIME_BINDING_MISMATCH");
        }

        ClinicalRunRecord run = runs.findById(required(runId))
                .orElseThrow(() -> blocked("RUN_NOT_FOUND"));
        if (!consultationId.equals(run.getConsultationId())
                || !binding.getThreadId().equals(run.getThreadId())
                || !consultation.getStartEventId().equals(run.getEventId())
                || !ClinicalRunRecord.OPEN.equals(run.getStatus())) {
            throw blocked("RUN_CONTEXT_MISMATCH");
        }

        CanonicalBusinessEventRecord event = events.findById(run.getEventId())
                .orElseThrow(() -> blocked("EVENT_NOT_FOUND"));
        if (!consultationId.equals(event.getConsultationId())
                || !"START_CONSULTATION".equals(event.getEventType())) {
            throw blocked("EVENT_CONTEXT_MISMATCH");
        }

        if (!sourceMatches(consultation, event, rawClinicalText)) {
            throw blocked("SOURCE_FINGERPRINT_MISMATCH");
        }

        CDP cdp = cdps.findById(binding.getCdpId())
                .orElseThrow(() -> blocked("CDP_NOT_FOUND"));
        if (!consultation.getUserId().equals(cdp.getPatientId())
                || cdp.getVersion() == null
                || cdp.getVersion().intValue() != run.getBasedOnClinicalStateVersion().intValue()) {
            throw blocked("CDP_STATE_STALE_OR_WRONG_OWNER");
        }
        return new VerifiedStart(consultation.getUserId(), consultationId,
                binding.getThreadId(), run.getRunId(), event.getEventId(),
                binding.getCdpId(), cdp.getVersion());
    }

    /** Mirror the canonical raw-source digest used by U01, not the candidate problem text. */
    private static boolean sourceMatches(ConsultationRecord consultation,
            CanonicalBusinessEventRecord event, String rawClinicalText) {
        String ref = consultation.getSubjectReferenceId();
        String canonicalSource = rawClinicalText + "|subjectRef=" + (ref == null ? "" : ref);
        String sourceFingerprint = sha256(canonicalSource);
        String canonicalPayload = consultation.getUserId() + "|raw:" + sourceFingerprint;
        return sha256(canonicalPayload).equals(event.getPayloadDigest());
    }

    private static String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b & 0xff));
            return hex.toString();
        } catch (Exception e) {
            throw blocked("SOURCE_DIGEST_UNAVAILABLE");
        }
    }

    private static String required(String value) {
        if (value == null || value.trim().isEmpty()) throw blocked("IDENTITY_REQUIRED");
        return value;
    }

    private static IllegalStateException blocked(String reason) {
        return new IllegalStateException("U01_U02_HANDOFF_BLOCKED:" + reason);
    }

    /** Identity is derived exclusively from persisted host authorities. */
    public static final class VerifiedStart {
        public final String userId;
        public final String consultationId;
        public final String threadId;
        public final String runId;
        public final String eventId;
        public final String cdpId;
        public final int clinicalStateVersion;

        private VerifiedStart(String userId, String consultationId, String threadId,
                String runId, String eventId, String cdpId, int clinicalStateVersion) {
            this.userId = userId;
            this.consultationId = consultationId;
            this.threadId = threadId;
            this.runId = runId;
            this.eventId = eventId;
            this.cdpId = cdpId;
            this.clinicalStateVersion = clinicalStateVersion;
        }
    }
}
