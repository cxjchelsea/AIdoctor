package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.diagnosis.runtime.foundation.*;
import com.aidoctor.diagnosis.runtime.u01.*;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.entity.CDP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class U01U02TrustedStartContextTest {
    private ConsultationRepository consultations;
    private RuntimeBindingService bindings;
    private ClinicalRunRepository runs;
    private CanonicalBusinessEventRepository events;
    private CDPRepository cdps;
    private U01U02TrustedStartContext resolver;
    private ConsultationRecord consultation;
    private RuntimeBindingRecord binding;
    private ClinicalRunRecord run;
    private CanonicalBusinessEventRecord event;
    private CDP cdp;

    @BeforeEach
    void setup() throws Exception {
        consultations = mock(ConsultationRepository.class);
        bindings = mock(RuntimeBindingService.class);
        runs = mock(ClinicalRunRepository.class);
        events = mock(CanonicalBusinessEventRepository.class);
        cdps = mock(CDPRepository.class);
        resolver = new U01U02TrustedStartContext(consultations, bindings, runs, events, cdps);
        U01StartCommand command = new U01StartCommand("event-1", "idem-1", "user-1",
                "SELF", null, "synthetic complaint", "SYMPTOM", false, digest("synthetic complaint|subjectRef="));
        U01SemanticDecision decision = new U01SemanticDecision(
                U01SemanticDecision.SUBJECT_RESOLVED, "SELF",
                U01SemanticDecision.PROBLEM_FRAMED, U01SemanticDecision.IN_SCOPE,
                null, "U02", false);
        consultation = new ConsultationRecord("consult-1", "cdp-1", "user-1",
                command, decision, LocalDateTime.now());
        binding = new RuntimeBindingRecord("consult-1", "cdp-1",
                RuntimeBindingRecord.CLINICAL_RUNTIME_V1, "thread-1",
                "aidoctor-v1-scope", "aidoctor-v1-u01", "contracts-v1", LocalDateTime.now());
        run = new ClinicalRunRecord("run-1", "thread-1", "consult-1",
                "event-1", 0, LocalDateTime.now());
        event = new CanonicalBusinessEventRecord("event-1", "consult-1",
                "START_CONSULTATION", "idem-1",
                digest("user-1|raw:" + digest("synthetic complaint|subjectRef=")),
                LocalDateTime.now());
        cdp = CDP.builder().id("cdp-1").patientId("user-1").version(0).build();
        when(consultations.findById("consult-1")).thenReturn(Optional.of(consultation));
        when(bindings.requireBinding("consult-1")).thenReturn(binding);
        when(runs.findById("run-1")).thenReturn(Optional.of(run));
        when(events.findById("event-1")).thenReturn(Optional.of(event));
        when(cdps.findById("cdp-1")).thenReturn(Optional.of(cdp));
    }

    @Test void verifiedDurableStartReturnsExactTrustedIdentity() {
        U01U02TrustedStartContext.VerifiedStart context =
                resolver.require("consult-1", "run-1", "synthetic complaint");
        assertEquals("thread-1", context.threadId);
        assertEquals("event-1", context.eventId);
        assertEquals("cdp-1", context.cdpId);
        assertEquals(0, context.clinicalStateVersion);
    }

    @Test void changedPatientTextFailsBeforeClinicalExecution() {
        assertTrue(assertThrows(IllegalStateException.class,
                () -> resolver.require("consult-1", "run-1", "different complaint"))
                .getMessage().contains("SOURCE_FINGERPRINT_MISMATCH"));
    }

    @Test void staleClinicalVersionCannotCrossHandoff() {
        cdp.setVersion(1);
        assertTrue(assertThrows(IllegalStateException.class,
                () -> resolver.require("consult-1", "run-1", "synthetic complaint"))
                .getMessage().contains("CDP_STATE_STALE"));
    }

    @Test void mismatchedRunCannotCrossHandoff() {
        ClinicalRunRecord another = new ClinicalRunRecord("run-1", "wrong-thread",
                "consult-1", "event-1", 0, LocalDateTime.now());
        when(runs.findById("run-1")).thenReturn(Optional.of(another));
        assertTrue(assertThrows(IllegalStateException.class,
                () -> resolver.require("consult-1", "run-1", "synthetic complaint"))
                .getMessage().contains("RUN_CONTEXT_MISMATCH"));
    }

    private static String digest(String input) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(String.format("%02x", b & 0xff));
        return result.toString();
    }
}
