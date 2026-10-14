package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.repository.CDPVersionRepository;
import com.aidoctor.diagnosis.runtime.u06.*;
import com.aidoctor.diagnosis.service.cdp.CDPVersionService;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
import com.aidoctor.diagnosis.state.committer.fakes.*;
import com.aidoctor.diagnosis.state.committer.ports.*;
import com.aidoctor.diagnosis.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Real isolated MySQL8 physical U06 Mode-2 acceptance. Explicitly uses
 * synthetic clinical/route authority; this is NOT full F3/A1/U05-to-WAIT E2E.
 * A SELECTED question is not delivery and cannot itself create durable WAIT.
 */
public class U07U06SelectionPhysicalMysqlTest {
    private static void disposableOnly() {
        if (!"true".equals(System.getenv("GITHUB_ACTIONS"))
                || !"DISPOSABLE_CDP_ONLY".equals(System.getenv("U07_WORKPACK_A_MYSQL_MODE")))
            throw new IllegalStateException("U07_U06_DISPOSABLE_MYSQL_ONLY");
    }

    @Test
    public void realMode2SelectionCommitsExactlyOnceAndCannotCreateWaitWithoutDelivery() {
        disposableOnly();
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                U07WorkpackACdpPhysicalMysqlTest.IsolatedConfig.class)) {
            CDPRepository cdps = ctx.getBean(CDPRepository.class);
            CDPVersionRepository history = ctx.getBean(CDPVersionRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);
            String id = tx.execute(status -> {
                CDP cdp = CDP.builder().id("u06-physical-" + UUID.randomUUID())
                        .patientId("synthetic-person").sessionId("synthetic-session")
                        .version(8).build();
                Map<String,Object> state = new LinkedHashMap<String,Object>();
                Map<String,Object> f3 = new LinkedHashMap<String,Object>();
                f3.put("f3_canonical_effect_id", "synthetic-f3-effect");
                state.put("f3_gap_assessment", f3);
                state.put("questions", new LinkedHashMap<String,Object>());
                cdp.setPatientState(state);
                cdps.saveAndFlush(cdp);
                return cdp.getId();
            });
            CDPVersionService versions = new CDPVersionService();
            ReflectionTestUtils.setField(versions, "cdpVersionRepository", history);
            ReflectionTestUtils.setField(versions, "jsonUtil", new JsonUtil());
            Clock fixed = Clock.fixed(Instant.parse("2026-10-14T00:00:00Z"), ZoneOffset.UTC);
            StateCommitter committer = new StateCommitter(
                    new ClinicalCdpStateRepositoryAdapter(cdps, versions, new ObjectMapper()),
                    (capability,version) -> CapabilityPolicyPort.CapabilityDecision.authorized(),
                    (field,capability) -> FieldPermissionPort.FieldPermissionDecision.authorized(),
                    (cdp,capability) -> ConsentPolicyPort.ConsentDecision.authorized(),
                    source -> SourceValidationPort.SourceDecision.authorized(),
                    new InMemoryIdempotencyFake(), new SyntheticAuditPortFake(fixed),
                    new RecordingCommitEventEvidenceFake(), fixed);
            U06AdmissionService admission = new U06AdmissionService();
            U06NonProductionPhysicalMode23Service physical = new U06NonProductionPhysicalMode23Service(
                    (request,decision,safety) -> {
                        throw new IllegalStateException("TEST_MODE1_NOT_AUTHORIZED");
                    }, cdps, committer, admission);
            U06ProfileBRequest request = new U06ProfileBRequest(
                    "physical-u06-select", "synthetic-consult", id,
                    U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,
                    U06ProfileBRequest.U05_QUESTION_ROUTING, "synthetic-u05-route",
                    8, 8, U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                    U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING,
                    "synthetic-dependency", "synthetic-u05-readiness",
                    "synthetic-question-policy", "synthetic-d04-policy",
                    "synthetic-event", "synthetic-business-event",
                    "synthetic-thread", "synthetic-run", 0L,
                    "synthetic-correlation", "synthetic-trace",
                    "2026-10-14T00:00:00Z", U06AdmissionEvidence.syntheticCurrentAllow());
            U06SyntheticDecisionBundle decision = new U06SyntheticDecisionBundle(
                    U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED,
                    "synthetic-f3-effect", "synthetic-gap", "DECISION_MATERIAL",
                    true, U06SyntheticDecisionBundle.CONTINUE,
                    U06SyntheticDecisionBundle.SELECTED, "synthetic-selection-effect",
                    "synthetic-question-1", "TEST:FOLLOWUP", "synthetic-question-content",
                    "sha256:synthetic-question", null, null);

            U06ExecutionResult selected = tx.execute(status ->
                    physical.execute(request, decision, null));
            assertEquals(U06ExecutionResult.MODE2_SELECTION_PERSISTED, selected.getStatus());
            assertEquals("synthetic-selection-effect", selected.getEffectRef());
            CDP persisted = cdps.findById(id).orElseThrow(IllegalStateException::new);
            assertEquals(9, persisted.getVersion().intValue());
            Map<?,?> questions = (Map<?,?>) persisted.getPatientState().get("questions");
            Map<?,?> question = (Map<?,?>) questions.get("synthetic-question-1");
            assertEquals("SELECTED", question.get("status"));
            assertEquals("synthetic-selection-effect", question.get("selection_effect_id"));
            assertFalse(persisted.getPatientState().containsKey("pending_question"));
            assertEquals(1, history.findByCdpIdOrderByVersionNumberDesc(id).size());

            // Replaying Mode-2 cannot silently select a second question or
            // make a delivery claim with the old clinical-state version.
            assertThrows(RuntimeException.class, () -> tx.execute(status ->
                    physical.execute(request, decision, null)));
            assertEquals(9, cdps.findById(id).get().getVersion().intValue());
            assertEquals(1, history.findByCdpIdOrderByVersionNumberDesc(id).size());

            AtomicInteger calledWaitOwner = new AtomicInteger();
            U07NonProductionDeliveryWaitRecovery recovery =
                    new U07NonProductionDeliveryWaitRecovery(cdps, jdbc, tx, committer,
                            receipt -> false,
                            (receipt,version) -> {
                                calledWaitOwner.incrementAndGet();
                                throw new AssertionError("Unverified delivery must never establish WAIT");
                            });
            U07NonProductionDeliveryWaitRecovery.Receipt unverified =
                    new U07NonProductionDeliveryWaitRecovery.Receipt(
                            request, decision, "synthetic-delivery", "synthetic-delivery-effect",
                            "unverified-confirmation");
            assertThrows(IllegalStateException.class, () -> recovery.accept(unverified));
            assertThrows(IllegalStateException.class, () -> recovery.recover(unverified));
            assertEquals(0, calledWaitOwner.get());
            assertEquals(0, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_question_delivery_evidence WHERE cdp_id=?",
                    Integer.class, id).intValue());
            assertEquals(0, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_question_wait_evidence WHERE cdp_id=?",
                    Integer.class, id).intValue());
            assertEquals(9, cdps.findById(id).get().getVersion().intValue());
        }
    }
}
