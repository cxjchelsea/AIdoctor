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
import org.junit.jupiter.api.Assumptions;
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
        Assumptions.assumeTrue("DISPOSABLE_CDP_ONLY".equals(
                System.getenv("U07_WORKPACK_A_MYSQL_MODE")),
                "requires dedicated disposable MySQL physical job");
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
            U06ExecutionResult stale = tx.execute(status ->
                    physical.execute(request, decision, null));
            assertEquals(U06ExecutionResult.ADMISSION_REJECTED, stale.getStatus());
            assertEquals(U06AdmissionService.REJECTED_STALE_STATE, stale.getReasonCode());
            assertEquals(9, cdps.findById(id).get().getVersion().intValue());
            assertEquals(1, ((Map<?,?>) cdps.findById(id).get()
                    .getPatientState().get("questions")).size());
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

            // Test-only exact receipt fixture: exercises delivery CDP persistence,
            // not a real independently confirmed external patient delivery.
            U07NonProductionDeliveryWaitRecovery.Receipt fixture =
                    new U07NonProductionDeliveryWaitRecovery.Receipt(
                            request, decision, "fixture-delivery-id",
                            "fixture-delivery-effect", "fixture-confirmation");
            AtomicInteger unavailableWaitCalls = new AtomicInteger();
            U07NonProductionDeliveryWaitRecovery.DeliveryVerifier fixtureVerifier =
                    receipt -> "fixture-confirmation".equals(receipt.confirmationRef)
                            && "fixture-delivery-id".equals(receipt.deliveryId)
                            && "fixture-delivery-effect".equals(receipt.deliveryEffectId);
            U07NonProductionDeliveryWaitRecovery.WaitAuthority unavailableWait =
                    (receipt, version) -> {
                        unavailableWaitCalls.incrementAndGet();
                        throw new IllegalStateException("TEST_DURABLE_WAIT_UNAVAILABLE");
                    };
            U07NonProductionDeliveryWaitRecovery delivery =
                    new U07NonProductionDeliveryWaitRecovery(
                            cdps, jdbc, tx, committer, fixtureVerifier, unavailableWait);
            delivery.accept(fixture);
            assertThrows(IllegalStateException.class, () -> delivery.recover(fixture));
            CDP afterDelivery = cdps.findById(id).orElseThrow(IllegalStateException::new);
            assertEquals(10, afterDelivery.getVersion().intValue());
            Map<?,?> deliveredQuestions = (Map<?,?>) afterDelivery.getPatientState()
                    .get("questions");
            assertEquals("DELIVERED_TO_USER", ((Map<?,?>) deliveredQuestions
                    .get("synthetic-question-1")).get("status"));
            assertTrue(afterDelivery.getPatientState().containsKey("pending_question"));
            assertEquals(2, history.findByCdpIdOrderByVersionNumberDesc(id).size());
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_question_delivery_evidence WHERE cdp_id=?",
                    Integer.class, id).intValue());
            assertEquals(0, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_question_wait_evidence WHERE cdp_id=?",
                    Integer.class, id).intValue());

            // Simulate a new process/owner retry: no duplicate delivery commit
            // and no manufactured WAIT when the durable host is unavailable.
            U07NonProductionDeliveryWaitRecovery restarted =
                    new U07NonProductionDeliveryWaitRecovery(
                            cdps, jdbc, tx, committer, fixtureVerifier, unavailableWait);
            restarted.accept(fixture);
            assertThrows(IllegalStateException.class, () -> restarted.recover(fixture));
            assertEquals(2, unavailableWaitCalls.get());
            assertEquals(10, cdps.findById(id).get().getVersion().intValue());
            assertEquals(2, history.findByCdpIdOrderByVersionNumberDesc(id).size());
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_question_delivery_evidence WHERE cdp_id=?",
                    Integer.class, id).intValue());
            assertEquals(0, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_question_wait_evidence WHERE cdp_id=?",
                    Integer.class, id).intValue());

            // Independent disposable MySQL consultation fixture. The WAIT
            // transitions themselves use actual governed JPA owners.
            jdbc.update("INSERT INTO clinical_consultation "
                    + "(consultation_id,row_version,cdp_id,user_id,lifecycle_status,"
                    + "subject_status,problem_status,scope_decision,"
                    + "early_safety_signal,start_event_id,created_at) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    request.getConsultationId(), 0L, id, "synthetic-person", "ACTIVE",
                    "KNOWN", "PRESENT", "IN_SCOPE", false,
                    "synthetic-start-" + id, java.sql.Timestamp.valueOf(
                            java.time.LocalDateTime.of(2026, 10, 14, 0, 0)));
            com.aidoctor.diagnosis.runtime.u01.ConsultationRepository consultations =
                    ctx.getBean(com.aidoctor.diagnosis.runtime.u01.ConsultationRepository.class);
            com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitEffectRepository waitEffects =
                    ctx.getBean(com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitEffectRepository.class);
            com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRepository threads =
                    ctx.getBean(com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRepository.class);
            com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository checkpoints =
                    ctx.getBean(com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository.class);
            com.aidoctor.diagnosis.runtime.u06.U07ExistingU06WaitAuthority actualWait =
                    new com.aidoctor.diagnosis.runtime.u06.U07ExistingU06WaitAuthority(
                            new com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitTransitionService(
                                    consultations, waitEffects),
                            new com.aidoctor.diagnosis.runtime.u06.wait.U06WaitCoordinator(
                                    new com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointService(
                                            threads, checkpoints),
                                    new com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadWaitTransitionService(
                                            threads, checkpoints)));
            // Crash-window adversary: the real WAIT host has committed,
            // but the recovery owner dies before saving WAIT evidence.
            U07NonProductionDeliveryWaitRecovery crashBeforeEvidence =
                    new U07NonProductionDeliveryWaitRecovery(cdps, jdbc, tx, committer,
                            fixtureVerifier, (receipt, version) -> {
                                tx.execute(status -> actualWait.establish(receipt, version));
                                throw new IllegalStateException("TEST_CRASH_AFTER_WAIT_HOST_COMMIT");
                            });
            assertThrows(IllegalStateException.class,
                    () -> crashBeforeEvidence.recover(fixture));
            assertEquals("WAITING_USER", consultations.findById(
                    request.getConsultationId()).get().getLifecycleStatus());
            assertEquals("AWAITING_USER", threads.findById(
                    request.getThreadId()).get().getRuntimeStatus());
            assertEquals(1L, checkpoints.count());
            assertEquals(1L, waitEffects.count());
            assertEquals(0, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_question_wait_evidence WHERE cdp_id=?",
                    Integer.class, id).intValue());

            // Reconcile existing host authority instead of starting a new WAIT.
            U07NonProductionDeliveryWaitRecovery physicalWait =
                    new U07NonProductionDeliveryWaitRecovery(cdps, jdbc, tx, committer,
                            fixtureVerifier, (receipt, version) ->
                                    tx.execute(status -> actualWait.establish(receipt, version)));
            U07NonProductionDeliveryWaitRecovery.WaitReceipt persistedWait =
                    physicalWait.recover(fixture);
            assertNotNull(persistedWait);
            assertEquals("WAITING_USER", consultations.findById(
                    request.getConsultationId()).get().getLifecycleStatus());
            assertEquals("AWAITING_USER", threads.findById(
                    request.getThreadId()).get().getRuntimeStatus());
            assertTrue(checkpoints.findById(persistedWait.checkpointId).isPresent());
            assertTrue(waitEffects.findById(persistedWait.waitEffectId).isPresent());
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_question_wait_evidence WHERE cdp_id=?",
                    Integer.class, id).intValue());

            // Recreate the recovery service and assert full durable readback.
            U07NonProductionDeliveryWaitRecovery afterRestart =
                    new U07NonProductionDeliveryWaitRecovery(cdps, jdbc, tx, committer,
                            fixtureVerifier, (receipt, version) -> {
                                throw new AssertionError("Completed WAIT must not be reissued");
                            });
            U07NonProductionDeliveryWaitRecovery.WaitReceipt replay =
                    afterRestart.recover(fixture);
            assertEquals(persistedWait.waitEffectId, replay.waitEffectId);
            assertEquals(persistedWait.checkpointId, replay.checkpointId);
            assertEquals(persistedWait.eligibilityId, replay.eligibilityId);

            // F8 boundary is read-only: the persisted WAIT admits an answer
            // candidate, but no Run/Event issuance or U02 commit is fabricated.
            U07NonProductionAnswerResumeAdmission f8Admission =
                    new U07NonProductionAnswerResumeAdmission(
                            consultations, threads, checkpoints, cdps);
            com.aidoctor.diagnosis.runtime.u02.U02ExecutionCommand answer =
                    f8Admission.requireWaitingAnswer(
                            request.getConsultationId(), id, request.getThreadId(),
                            request.getRunId(), persistedWait.checkpointId,
                            "synthetic-question-1", fixture.deliveryId, "synthetic-person",
                            "synthetic-answer-event", "I have severe difficulty breathing",
                            "synthetic-answer-correlation", "synthetic-answer-trace");
            assertEquals(10, answer.baseClinicalStateVersion);
            assertEquals("I have severe difficulty breathing", answer.rawClinicalText);
            assertThrows(IllegalStateException.class, () ->
                    f8Admission.requireWaitingAnswer(
                            request.getConsultationId(), id, "wrong-thread",
                            request.getRunId(), persistedWait.checkpointId,
                            "synthetic-question-1", fixture.deliveryId, "synthetic-person",
                            "synthetic-answer-event", "I have severe difficulty breathing",
                            "synthetic-answer-correlation", "synthetic-answer-trace"));
            assertThrows(IllegalStateException.class, () ->
                    f8Admission.requireWaitingAnswer(
                            request.getConsultationId(), id, request.getThreadId(),
                            request.getRunId(), persistedWait.checkpointId,
                            "wrong-question", fixture.deliveryId, "synthetic-person",
                            "synthetic-answer-event", "I have severe difficulty breathing",
                            "synthetic-answer-correlation", "synthetic-answer-trace"));

            assertEquals(10, cdps.findById(id).get().getVersion().intValue());
            assertEquals(2, history.findByCdpIdOrderByVersionNumberDesc(id).size());

            // Real Foundation-0 ledgers on the same disposable MySQL8 DB.
            com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingRepository bindingRepo =
                    ctx.getBean(com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingRepository.class);
            com.aidoctor.diagnosis.runtime.foundation.CanonicalBusinessEventRepository events =
                    ctx.getBean(com.aidoctor.diagnosis.runtime.foundation.CanonicalBusinessEventRepository.class);
            com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRepository runs =
                    ctx.getBean(com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRepository.class);
            tx.execute(status -> {
                bindingRepo.saveAndFlush(new com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingRecord(
                        request.getConsultationId(), id,
                        com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingRecord.CLINICAL_RUNTIME_V1,
                        request.getThreadId(), "synthetic-scope", "synthetic-capabilities",
                        "contracts-v1", java.time.LocalDateTime.of(2026,10,14,0,0)));
                return null;
            });
            com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingService bindingService =
                    new com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingService(bindingRepo);
            U07NonProductionDurableF8Consume consume = new U07NonProductionDurableF8Consume(
                    jdbc, tx,
                    new com.aidoctor.diagnosis.runtime.foundation.CanonicalBusinessEventLedger(events),
                    new com.aidoctor.diagnosis.runtime.foundation.ClinicalRunCoordinator(
                            bindingService, events, runs), consultations, threads, checkpoints);
            String freshRun = consume.consume(
                    answer, persistedWait.checkpointId, "synthetic-answer-idem",
                    "sha256:synthetic-answer-digest");
            assertNotEquals(answer.runId, freshRun);
            assertEquals("ACTIVE", consultations.findById(
                    request.getConsultationId()).get().getLifecycleStatus());
            assertEquals("ACTIVE", threads.findById(request.getThreadId()).get().getRuntimeStatus());
            assertEquals("CONSUMED", checkpoints.findById(
                    persistedWait.checkpointId).get().getCheckpointStatus());
            assertThrows(IllegalStateException.class, () ->
                    f8Admission.requireWaitingAnswer(
                            request.getConsultationId(), id, request.getThreadId(),
                            request.getRunId(), persistedWait.checkpointId,
                            "synthetic-question-1", fixture.deliveryId, "synthetic-person",
                            "new-answer-event", "second answer", "corr", "trace"));

            assertTrue(consume.consumedFor(answer.consultationId, answer.runId,
                    persistedWait.checkpointId, freshRun, answer.eventId));

            U07NonProductionTrustedAnswerU02Bridge trustedAnswer =
                    new U07NonProductionTrustedAnswerU02Bridge(runs, events, consume);
            com.aidoctor.diagnosis.runtime.u02.U02ExecutionCommand resumed =
                    trustedAnswer.requireAuthorizedNewRun(
                            answer, persistedWait.checkpointId, freshRun);
            assertEquals(freshRun, resumed.runId);
            assertNotEquals(answer.runId, resumed.runId);
            assertEquals(answer.eventId, resumed.eventId);
            assertEquals(10, resumed.baseClinicalStateVersion);
            assertEquals("PATIENT_REPORTED", resumed.sourceType);
            assertEquals("I have severe difficulty breathing", resumed.rawClinicalText);

            // Actual U02 business path, P01 commit and P05 trace on the same
            // physical MySQL DB. C01 interpretation is an explicit fixture.
            com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard guard =
                    org.mockito.Mockito.mock(com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard.class);
            com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord authorized =
                    new com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord(
                            com.aidoctor.diagnosis.runtime.u02.capability.C01U02CapabilityGateway.BINDING_ID,
                            "C01", "fixture-c01-u02-1", "aidoctor-v1-u02",
                            "aidoctor-v1-scope", "contracts-v1", "*", "*", "*", "*",
                            com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord.ACTIVE,
                            java.time.LocalDateTime.of(2026,1,1,0,0), null,
                            java.time.LocalDateTime.of(2026,1,1,0,0));
            org.mockito.Mockito.when(guard.authorize(
                    org.mockito.ArgumentMatchers.eq(
                            com.aidoctor.diagnosis.runtime.u02.capability.C01U02CapabilityGateway.BINDING_ID),
                    org.mockito.ArgumentMatchers.eq("C01"),
                    org.mockito.ArgumentMatchers.any())).thenReturn(authorized);
            com.aidoctor.diagnosis.client.C01U02CapabilityClient client =
                    org.mockito.Mockito.mock(com.aidoctor.diagnosis.client.C01U02CapabilityClient.class);
            org.mockito.Mockito.when(client.interpret(org.mockito.ArgumentMatchers.any()))
                    .thenReturn(resumedAnswerCandidate());
            com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactApplicationService realU02 =
                    new com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactApplicationService(
                            new com.aidoctor.diagnosis.runtime.u02.capability.C01U02CapabilityGateway(
                                    client, guard, true),
                            new com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactBusinessOwner(),
                            new com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactProposalFactory(),
                            new com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactCommitService(committer),
                            new com.aidoctor.diagnosis.runtime.u02.d05.U02DependencyInvalidationHook(),
                            new com.aidoctor.diagnosis.runtime.governance.CapabilityTraceService(
                                    ctx.getBean(com.aidoctor.diagnosis.runtime.governance.CapabilityCallTraceRepository.class)));
            com.aidoctor.diagnosis.runtime.u02.U02ExecutionResult updated =
                    tx.execute(status -> realU02.execute(resumed));
            assertEquals("COMMITTED", updated.status);
            assertEquals("COMMITTED", updated.commitResult.status);
            assertEquals(Integer.valueOf(11), updated.commitResult.committedVersion);
            assertEquals(11, cdps.findById(id).get().getVersion().intValue());
            assertEquals(3, history.findByCdpIdOrderByVersionNumberDesc(id).size());
            assertTrue(cdps.findById(id).get().getPatientState()
                    .get("clinical_fact_fixture-answer-obs") instanceof Map);
            com.aidoctor.diagnosis.runtime.governance.CapabilityCallTraceRecord persistedTrace =
                    ctx.getBean(com.aidoctor.diagnosis.runtime.governance.CapabilityCallTraceRepository.class)
                            .findById(updated.capabilityCallId).orElseThrow(IllegalStateException::new);
            assertEquals(freshRun, persistedTrace.getRunId());
            assertEquals(resumed.eventId, persistedTrace.getEventId());
            assertEquals(Integer.valueOf(10), persistedTrace.getClinicalStateVersionBefore());
            assertEquals(Integer.valueOf(11), persistedTrace.getClinicalStateVersionAfter());

            // The independent test attestation accepts ONLY the exact
            // persisted respiratory red-flag fact at v11; it does not fabricate
            // missing negative findings or a clinical clearance.
            com.aidoctor.diagnosis.runtime.u03.U03CdpAcceptedEvidenceAuthority clinicalEvidence =
                    new com.aidoctor.diagnosis.runtime.u03.U03CdpAcceptedEvidenceAuthority(
                            cdps, (consultation, cdp, version, call, evidence, sources, provenance) -> {
                                assertEquals(request.getConsultationId(), consultation);
                                assertEquals(id, cdp);
                                assertEquals(11, version);
                                assertEquals(updated.capabilityCallId, call);
                                assertEquals(java.util.Collections.singletonList("EV-RF-RESP-001"), evidence);
                                assertEquals(java.util.Collections.singletonList(
                                        "sha256:synthetic-answer-evidence"), sources);
                                assertEquals(1, provenance.size());
                                return "test-only-independent-v11-evidence-acceptance";
                            });
            com.aidoctor.diagnosis.runtime.u04.U04NonProductionApplicationService u04 =
                    new com.aidoctor.diagnosis.runtime.u04.U04NonProductionApplicationService(
                            new com.aidoctor.diagnosis.runtime.u04.U04AdmissionService(),
                            new com.aidoctor.diagnosis.runtime.u04.U04SafetyGatePolicy(),
                            new com.aidoctor.diagnosis.runtime.u04.U04StateProposalFactory(),
                            new com.aidoctor.diagnosis.runtime.u04.U04CommitService(committer));
            com.aidoctor.diagnosis.runtime.u03.U02U03GovernedSafetyBridge safety =
                    new com.aidoctor.diagnosis.runtime.u03.U02U03GovernedSafetyBridge(
                            cdps, ctx.getBean(
                                    com.aidoctor.diagnosis.runtime.governance.CapabilityCallTraceRepository.class),
                            clinicalEvidence,
                            new com.aidoctor.diagnosis.runtime.u03.U03NonProductionAdmissionService(), u04);
            com.aidoctor.diagnosis.runtime.u03.U03ExecutionCommand riskCommand =
                    new com.aidoctor.diagnosis.runtime.u03.U03ExecutionCommand(
                            resumed.consultationId, resumed.threadId, freshRun,
                            resumed.eventId, id, 11, resumed.correlationId, resumed.traceId);
            com.aidoctor.diagnosis.runtime.u03.U03NonProductionExecutionContext admitted =
                    safety.requireU03Admission(updated, riskCommand,
                            com.aidoctor.diagnosis.runtime.u03.U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                            "ci-nonprod-resumed-risk");
            assertEquals(11, admitted.requireAcceptedEvidenceBinding().getClinicalStateVersion());
            assertThrows(IllegalStateException.class, () ->
                    safety.requireU03Admission(updated,
                            new com.aidoctor.diagnosis.runtime.u03.U03ExecutionCommand(
                                    resumed.consultationId, resumed.threadId, freshRun,
                                    resumed.eventId, id, 10, resumed.correlationId, resumed.traceId),
                            com.aidoctor.diagnosis.runtime.u03.U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                            "ci-nonprod-resumed-risk"));
            com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord riskBinding =
                    new com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord(
                            com.aidoctor.diagnosis.runtime.u03.U03GovernedCandidateGateway.BINDING_ID,
                            "C02", "1.0.0", "synthetic-risk-capset",
                            "aidoctor-v1-scope", "contracts-v1", "*", "*", "*", "*",
                            com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord.ACTIVE,
                            java.time.LocalDateTime.of(2026,1,1,0,0), null,
                            java.time.LocalDateTime.of(2026,1,1,0,0));
            com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard riskGuard =
                    org.mockito.Mockito.mock(
                            com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard.class);
            org.mockito.Mockito.when(riskGuard.authorize(
                    org.mockito.ArgumentMatchers.eq(
                            com.aidoctor.diagnosis.runtime.u03.U03GovernedCandidateGateway.BINDING_ID),
                    org.mockito.ArgumentMatchers.eq("C02"),
                    org.mockito.ArgumentMatchers.any())).thenReturn(riskBinding);
            com.aidoctor.diagnosis.runtime.u03.U03CdpGateCClinicalInputPort input =
                    new com.aidoctor.diagnosis.runtime.u03.U03CdpGateCClinicalInputPort(
                            cdps, (cmd, accepted) -> {
                                assertEquals(11, cmd.clinicalStateVersion);
                                assertEquals("test-only-independent-v11-evidence-acceptance",
                                        accepted.getAcceptanceRef());
                                // Test fixture scope, not inferred patient demographics.
                                return com.aidoctor.diagnosis.runtime.u03.U03GateCClinicalInput.builder();
                            });
            assertEquals("PRESENT",
                    input.load(riskCommand, admitted.requireAcceptedEvidenceBinding())
                            .evidenceState("EV-RF-RESP-001"));
            assertEquals("UNKNOWN",
                    input.load(riskCommand, admitted.requireAcceptedEvidenceBinding())
                            .evidenceState("EV-MNM-CARD-001"));
            com.aidoctor.diagnosis.runtime.u03.U03GovernedCandidateGateway gateway =
                    new com.aidoctor.diagnosis.runtime.u03.U03GovernedCandidateGateway(
                            riskGuard, new com.aidoctor.diagnosis.runtime.u03.U03ReleaseRegistry(),
                            new com.aidoctor.diagnosis.runtime.u03.U03GateCFrozenRuleEvaluator(input),
                            true);
            com.aidoctor.diagnosis.runtime.u03.U03GovernedCandidateGateway.GovernedResult governed =
                    gateway.assess(admitted);
            com.aidoctor.diagnosis.runtime.u03.U03DecisionOutcome riskDecision =
                    new com.aidoctor.diagnosis.runtime.u03.U03DecisionService(
                            new com.aidoctor.diagnosis.runtime.u03.U03GateCFrozenDecisionPort())
                            .decide(admitted, governed);
            assertEquals("HIGH_RISK", riskDecision.getOutcomeCode());
            com.aidoctor.diagnosis.runtime.u03.U03StateProposal proposal =
                    new com.aidoctor.diagnosis.runtime.u03.U03StateProposalFactory()
                            .createNonProductionValid(admitted, riskDecision, governed);
            com.aidoctor.contracts.v1.StateTypes.CommitResult riskCommit =
                    tx.execute(status -> new com.aidoctor.diagnosis.runtime.u03.U03CommitService(
                            committer).commitNonProduction(admitted, proposal));
            assertEquals("COMMITTED", riskCommit.status);
            assertEquals(Integer.valueOf(12), riskCommit.committedVersion);
            assertEquals("HIGH_RISK", ((Map<?,?>) cdps.findById(id).get()
                    .getPatientState().get("current_risk_assessment")).get("outcome_code"));
            java.util.List<com.aidoctor.diagnosis.runtime.u03.U03RuntimeTraceRecord> u03Traces =
                    new java.util.ArrayList<>();
            com.aidoctor.diagnosis.runtime.u03.U03PostCommitFinalizationOutcome finalized =
                    new com.aidoctor.diagnosis.runtime.u03.U03PostCommitFinalizer(
                            new com.aidoctor.diagnosis.runtime.u03.U03RuntimeTraceService(
                                    u03Traces::add),
                            new com.aidoctor.diagnosis.runtime.u03.U03OutboundProducer())
                            .finalizeCommittedExecution(admitted, governed, riskDecision,
                                    proposal, riskCommit);
            assertEquals("DELIVERABLE", finalized.getStatus());
            com.aidoctor.diagnosis.runtime.u03.U03OutboundHandoff handoff =
                    finalized.getOutboundHandoff();
            assertEquals(Integer.valueOf(12), handoff.getCommittedClinicalStateVersion());
            assertEquals(freshRun, handoff.getRunId());
            com.aidoctor.diagnosis.runtime.u04.U04ExecutionResult gated =
                    tx.execute(status -> safety.applyU04(handoff,
                            com.aidoctor.diagnosis.runtime.u04.U04ScopeContext.current(true,true),
                            "ci-nonprod-resumed-risk"));
            assertEquals(com.aidoctor.diagnosis.runtime.u04.U04ExecutionResult.COMMIT_COMPLETE,
                    gated.getStatus());
            assertEquals(com.aidoctor.diagnosis.runtime.u04.U04SafetyGateDecision.BLOCKED,
                    gated.getDecision().getGate());
            assertEquals(Integer.valueOf(13), gated.getCommitResult().committedVersion);
            assertEquals("BLOCKED", ((Map<?,?>) cdps.findById(id).get()
                    .getPatientState().get("safety_gate")).get("safety_gate"));
            assertEquals(13, cdps.findById(id).get().getVersion().intValue());
            assertThrows(IllegalStateException.class, () ->
                    U07NonProductionF3A1U05Flow.requireEligiblePreF3(handoff, gated));
            assertEquals(com.aidoctor.diagnosis.runtime.u04.U04ExecutionResult.ADMISSION_FAILED,
                    safety.applyU04(handoff,
                            com.aidoctor.diagnosis.runtime.u04.U04ScopeContext.current(true,true),
                            "ci-nonprod-resumed-risk").getStatus());


            assertThrows(IllegalStateException.class, () ->
                    trustedAnswer.requireAuthorizedNewRun(
                            answer, persistedWait.checkpointId, answer.runId));

            assertEquals(freshRun, consume.consume(answer, persistedWait.checkpointId,
                    "synthetic-answer-idem", "sha256:synthetic-answer-digest"));
            assertEquals(1L, events.count());
            assertEquals(1L, runs.count());
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_f8_answer_consumption WHERE checkpoint_id=?",
                    Integer.class, persistedWait.checkpointId).intValue());
            assertThrows(IllegalStateException.class, () -> consume.consume(
                    answer, persistedWait.checkpointId, "synthetic-answer-idem",
                    "sha256:DIFFERENT"));
            assertEquals(1L, runs.count());
            assertEquals(13, cdps.findById(id).get().getVersion().intValue());
            assertEquals(5, history.findByCdpIdOrderByVersionNumberDesc(id).size());
        }
    }

    private static com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse resumedAnswerCandidate() {
        com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse response =
                new com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse();
        response.setBusinessStatus("SUCCESS");
        response.setSourceAttribution("USER_TEXT");
        com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse.BindingRef binding =
                new com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse.BindingRef();
        binding.setBindingId(com.aidoctor.diagnosis.runtime.u02.capability.C01U02CapabilityGateway.BINDING_ID);
        binding.setBindingStatus(com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord.ACTIVE);
        binding.setCapabilityId("C01");
        binding.setCapabilityVersion("fixture-c01-u02-1");
        binding.setCapabilitySetVersion("aidoctor-v1-u02");
        binding.setScopeVersion("aidoctor-v1-scope");
        binding.setContractVersion("contracts-v1");
        response.setBindingRef(binding);
        com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse.ObservationCandidate fact =
                new com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse.ObservationCandidate();
        fact.setObservationId("fixture-answer-obs");
        fact.setConceptId("EV-RF-RESP-001");
        fact.setConceptDisplay("synthetic severe respiratory distress red flag");
        fact.setRawTextRef("sha256:synthetic-answer-evidence");
        fact.setValueSemantics("YES");
        fact.setSourceType("PATIENT_REPORTED");
        fact.setLifecycle("CONFIRMED");
        fact.setConfidenceOrUncertainty(0.95d);
        fact.setProvenance(java.util.Collections.singletonList("synthetic-only"));
        fact.setAmbiguityFlags(java.util.Collections.emptyList());
        fact.setContradictionRefs(java.util.Collections.emptyList());
        response.setObservationCandidates(java.util.Collections.singletonList(fact));
        return response;
    }
}
