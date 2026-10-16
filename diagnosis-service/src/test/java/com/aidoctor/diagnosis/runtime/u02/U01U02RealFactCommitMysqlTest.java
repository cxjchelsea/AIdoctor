package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.diagnosis.client.C01U02CapabilityClient;
import com.aidoctor.diagnosis.dto.capability.c01.*;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.*;
import com.aidoctor.diagnosis.runtime.foundation.*;
import com.aidoctor.diagnosis.runtime.foundation.adapters.ClinicalCdpStateRepositoryAdapter;
import com.aidoctor.diagnosis.runtime.governance.*;
import com.aidoctor.diagnosis.runtime.u01.*;
import com.aidoctor.diagnosis.runtime.u01.capability.C01U01CapabilityGateway;
import com.aidoctor.diagnosis.runtime.u02.capability.C01U02CapabilityGateway;
import com.aidoctor.diagnosis.runtime.u02.d05.U02DependencyInvalidationHook;
import com.aidoctor.diagnosis.runtime.u03.*;
import com.aidoctor.diagnosis.runtime.u04.*;
import com.aidoctor.diagnosis.runtime.u05.U04A1U05AuthorityBridge;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionF3A1U05Flow;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionAnswerResumeAdmission;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionDurableF8Consume;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionTrustedAnswerU02Bridge;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionGovernedRiskSafetyContinuation;
import org.springframework.jdbc.core.JdbcTemplate;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalFollowupCoordinator;
import com.aidoctor.diagnosis.service.cdp.*;
import com.aidoctor.diagnosis.state.committer.*;
import com.aidoctor.diagnosis.state.committer.fakes.*;
import com.aidoctor.diagnosis.state.committer.ports.*;
import com.aidoctor.diagnosis.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Real U01 services, U02 application, P01 mechanical committer and disposable
 * MySQL/JPA CDP history. C01 outputs, authorization and trace identities are
 * controlled SYNTHETIC TEST FIXTURES, NOT real clinical approvals.
 * No public HTTP, diagnosis, risk approval or patient delivery is implied.
 */
public class U01U02RealFactCommitMysqlTest {
    @Test
    public void cautionProducesRestrictedGate() {
        runScenario("caution", U03GateCClinicalInput.builder()
                .suspectedSepsis("TRUE")
                .measurement("respiratory_rate_bpm", U03GateCClinicalInput.Measurement.present(21))
                .measurement("systolic_bp_mmHg", U03GateCClinicalInput.Measurement.present(120))
                .measurement("usual_systolic_bp_mmHg", U03GateCClinicalInput.Measurement.present(120))
                .measurement("heart_rate_bpm", U03GateCClinicalInput.Measurement.present(80))
                .build(), "CAUTION", U04SafetyGateDecision.RESTRICTED);
    }

    @Test
    public void highRiskMustBlock() {
        runScenario("high", U03GateCClinicalInput.builder()
                .evidence("EV-RF-RESP-001", "PRESENT")
                .build(), "HIGH_RISK", U04SafetyGateDecision.BLOCKED);
    }

    @Test
    public void completeNoHighRiskSignalCanAllow() {
        runScenario("none", U03GateCClinicalInput.builder().build(),
                "NO_HIGH_RISK_SIGNAL", U04SafetyGateDecision.ALLOW);
    }

    @Test
    public void insufficientRiskEvidenceCannotBecomeSafeGate() {
        runScenario("insufficient", U03GateCClinicalInput.builder()
                .evidence("EV-RF-RESP-001", "UNKNOWN").build(),
                "INSUFFICIENT_INFORMATION", null);
    }

    private void runScenario(String scenario, U03GateCClinicalInput scenarioInput,
            String expectedRisk, String expectedGate) {
        Assumptions.assumeTrue("DISPOSABLE_U01_U02_ONLY".equals(
                System.getenv("V1_U01_U02_MYSQL_MODE")));
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                U01U02PhysicalMysqlHandoffTest.DatabaseConfig.class)) {
            CDPRepository cdps = ctx.getBean(CDPRepository.class);
            CDPVersionRepository versionsRepo = ctx.getBean(CDPVersionRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            CDPVersionService versions = new CDPVersionService();
            ReflectionTestUtils.setField(versions, "cdpVersionRepository", versionsRepo);
            ReflectionTestUtils.setField(versions, "jsonUtil", new JsonUtil());
            CDPManager manager = new CDPManager();
            ReflectionTestUtils.setField(manager, "cdpRepository", cdps);
            ReflectionTestUtils.setField(manager, "cdpVersionService", versions);

            RuntimeBindingService bindingService = ctx.getBean(RuntimeBindingService.class);
            CanonicalBusinessEventRepository events =
                    ctx.getBean(CanonicalBusinessEventRepository.class);
            U01ConsultationService business = new U01ConsultationService(
                    ctx.getBean(ConsultationRepository.class), new U01SemanticPolicy(),
                    new CanonicalBusinessEventLedger(events), bindingService,
                    new ClinicalRunCoordinator(bindingService, events,
                            ctx.getBean(ClinicalRunRepository.class)), manager);
            C01U01CapabilityGateway u01Gateway = mock(C01U01CapabilityGateway.class);
            when(u01Gateway.interpret(anyString(), anyString(), anyString(), isNull()))
                    .thenReturn(u01Candidate());
            U01NaturalLanguageStartService start = new U01NaturalLanguageStartService(
                    ctx.getBean(ConsultationRepository.class), u01Gateway, business);
            final String raw = "synthetic symptom: I do not know if headache is present";
            U01RawStartCommand rawStart = new U01RawStartCommand(
                    "real-u01-u02-event-" + scenario, "real-u01-u02-idem-" + scenario,
                    "synthetic-person", raw, null);
            U01Result created = tx.execute(status -> start.start(rawStart));
            assertEquals("U02", created.getNextUnit());
            assertEquals(1, cdps.findById(created.getCdpId()).get().getVersion().intValue());
            assertEquals(1, versionsRepo.findByCdpIdOrderByVersionNumberDesc(created.getCdpId()).size());

            CapabilityBindingRecord authorizedFixture = new CapabilityBindingRecord(
                    C01U02CapabilityGateway.BINDING_ID, "C01", "fixture-c01-u02-1",
                    "aidoctor-v1-u02", "aidoctor-v1-scope", "contracts-v1",
                    "*", "*", "*", "*", CapabilityBindingRecord.ACTIVE,
                    LocalDateTime.of(2026,1,1,0,0), null, LocalDateTime.of(2026,1,1,0,0));
            CapabilityInvocationGuard guard = mock(CapabilityInvocationGuard.class);
            when(guard.authorize(eq(C01U02CapabilityGateway.BINDING_ID), eq("C01"), any()))
                    .thenReturn(authorizedFixture);
            C01U02CapabilityClient client = mock(C01U02CapabilityClient.class);
            when(client.interpret(any())).thenReturn(u02Candidate());
            C01U02CapabilityGateway u02Gateway = new C01U02CapabilityGateway(client, guard, true);
            Clock clock = Clock.fixed(Instant.parse("2026-10-14T00:00:00Z"), ZoneOffset.UTC);
            StateCommitter committer = new StateCommitter(
                    new ClinicalCdpStateRepositoryAdapter(cdps, versions, new ObjectMapper()),
                    (id,v) -> CapabilityPolicyPort.CapabilityDecision.authorized(),
                    (path,id) -> FieldPermissionPort.FieldPermissionDecision.authorized(),
                    (id,cap) -> ConsentPolicyPort.ConsentDecision.authorized(),
                    source -> SourceValidationPort.SourceDecision.authorized(),
                    new InMemoryIdempotencyFake(), new SyntheticAuditPortFake(clock),
                    new RecordingCommitEventEvidenceFake(), clock);
            CapabilityTraceService traces = new CapabilityTraceService(
                    ctx.getBean(CapabilityCallTraceRepository.class));
            U02ClinicalFactApplicationService u02 = new U02ClinicalFactApplicationService(
                    u02Gateway, new U02ClinicalFactBusinessOwner(),
                    new U02ClinicalFactProposalFactory(), new U02ClinicalFactCommitService(committer),
                    new U02DependencyInvalidationHook(), traces);
            U01U02NonProductionHandoffService handoff = new U01U02NonProductionHandoffService(
                    ctx.getBean(U01U02TrustedStartContext.class),
                    (consultation, run, event) -> new U01U02NonProductionHandoffService.TraceIdentity(
                            "synthetic-correlation", "synthetic-trace"), u02);
            U02ExecutionResult outcome = tx.execute(status -> handoff.executeCommittedStart(
                    created.getConsultationId(), created.getRunId(), raw));
            assertEquals("COMMITTED", outcome.status);
            assertEquals("COMMITTED", outcome.commitResult.status);
            CDP stored = cdps.findById(created.getCdpId()).get();
            assertEquals(2, stored.getVersion().intValue());
            assertTrue(stored.getPatientState().get("clinical_fact_fixture-obs") instanceof Map);
            Map<?,?> fact = (Map<?,?>)stored.getPatientState().get("clinical_fact_fixture-obs");
            assertEquals("UNKNOWN", fact.get("value_semantics"));
            // A real committed U02 headache observation is NOT automatically
            // accepted as any Gate-C red-flag evidence.
            U03CdpAcceptedEvidenceAuthority.ClinicalEvidenceAttestation noSyntheticApproval =
                    (consultation, cdp, version, call, evidenceRefs, sourceRefs, provenanceRefs) -> {
                        throw new AssertionError("Unmapped U02 fact must never request acceptance");
                    };
            U03CdpAcceptedEvidenceAuthority cdpEvidence =
                    new U03CdpAcceptedEvidenceAuthority(cdps, noSyntheticApproval);
            assertTrue(assertThrows(IllegalStateException.class, () ->
                    cdpEvidence.requireAccepted(created.getConsultationId(),
                            created.getCdpId(), 2, outcome.capabilityCallId))
                    .getMessage().contains("NO_ACCEPTABLE_GATE_C_EVIDENCE"));

            assertNotEquals("NO", fact.get("value_semantics"));
            assertEquals("PATIENT_REPORTED", fact.get("source_type"));
            assertEquals(2, versionsRepo.findByCdpIdOrderByVersionNumberDesc(created.getCdpId()).size());
            CapabilityCallTraceRecord trace = traces.require(outcome.capabilityCallId);
            assertEquals(CapabilityCallTraceRecord.SUCCEEDED, trace.getCallStatus());
            assertEquals(Integer.valueOf(2), trace.getClinicalStateVersionAfter());
            assertEquals(created.getRunId(), trace.getRunId());
            // Exact persisted U02 commit and P05 trace are the only entry
            // to governed U03 evidence admission; no C02 risk result is invented.
            U04NonProductionApplicationService u04 = new U04NonProductionApplicationService(
                    new U04AdmissionService(), new U04SafetyGatePolicy(),
                    new U04StateProposalFactory(), new U04CommitService(committer));
            U02U03GovernedSafetyBridge safety = new U02U03GovernedSafetyBridge(
                    cdps, ctx.getBean(CapabilityCallTraceRepository.class),
                    (consultation, cdp, version, call) ->
                            new U03AcceptedEvidenceBinding("synthetic-acceptance", version,
                                    java.util.Arrays.asList(
                                            "EV-RF-RESP-001", "EV-MNM-NEURO-001",
                                            "EV-MNM-NEURO-002", "EV-MNM-CARD-001",
                                            "EV-RF-ALLERGY-001", "EV-RF-APPEAR-001",
                                            "EV-RF-NEURO-001", "EV-VS-SEPSIS-001",
                                            "EV-VS-SEPSIS-002", "EV-VS-SEPSIS-003",
                                            "EV-RF-SEPSIS-001"),
                                    Collections.singletonList("synthetic-source"),
                                    Collections.singletonList("synthetic-provenance")),
                    new U03NonProductionAdmissionService(), u04);
            U03ExecutionCommand riskCommand = new U03ExecutionCommand(
                    created.getConsultationId(), trace.getThreadId(),
                    created.getRunId(), trace.getEventId(), created.getCdpId(),
                    2, "synthetic-correlation", "synthetic-trace");
            U03NonProductionExecutionContext admitted = safety.requireU03Admission(
                    outcome, riskCommand, U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                    "ci-nonprod-safety");
            assertEquals(2, admitted.requireAcceptedEvidenceBinding().getClinicalStateVersion());
            CapabilityBindingRecord riskBinding = new CapabilityBindingRecord(
                    U03GovernedCandidateGateway.BINDING_ID, "C02", "1.0.0",
                    "synthetic-risk-capset", "aidoctor-v1-scope", "contracts-v1",
                    "*", "*", "*", "*", CapabilityBindingRecord.ACTIVE,
                    LocalDateTime.of(2026,1,1,0,0), null, LocalDateTime.of(2026,1,1,0,0));
            CapabilityInvocationGuard riskGuard = mock(CapabilityInvocationGuard.class);
            when(riskGuard.authorize(eq(U03GovernedCandidateGateway.BINDING_ID),
                    eq("C02"), any())).thenReturn(riskBinding);
            U03GateCClinicalInputPort input = (cmd, accepted) -> scenarioInput;
            U03GovernedCandidateGateway governedGateway = new U03GovernedCandidateGateway(
                    riskGuard, new U03ReleaseRegistry(),
                    new U03GateCFrozenRuleEvaluator(input), true);
            U03GovernedCandidateGateway.GovernedResult governed = governedGateway.assess(admitted);
            U03DecisionOutcome riskDecision = new U03DecisionService(
                    new U03GateCFrozenDecisionPort()).decide(admitted, governed);
            if ("INSUFFICIENT_INFORMATION".equals(expectedRisk)) {
                assertEquals(U03RiskAssessmentCandidate.FAILED, riskDecision.getStatus());
                assertNull(riskDecision.getOutcomeCode());
                assertEquals("INSUFFICIENT_INFORMATION", riskDecision.getReasonCode());
                assertThrows(IllegalArgumentException.class, () ->
                        new U03StateProposalFactory()
                                .createNonProductionValid(admitted, riskDecision, governed));
                assertEquals(2, cdps.findById(created.getCdpId()).get()
                        .getVersion().intValue());
                U03OutboundHandoff failedHandoff = new U03OutboundProducer().produce(
                        admitted, governed, riskDecision, null, null);
                assertEquals("FAILED", failedHandoff.getDecisionStatus());
                assertNull(failedHandoff.getDispositionCode());
                assertNull(failedHandoff.getCommittedClinicalStateVersion());
                U04ExecutionResult unavailable = tx.execute(status -> safety.applyU04(
                        failedHandoff, U04ScopeContext.current(true, true), "ci-nonprod-safety"));
                assertEquals(U04ExecutionResult.COMMIT_COMPLETE, unavailable.getStatus());
                assertEquals(U04SafetyGateDecision.UNAVAILABLE, unavailable.getDecision().getGate());
                assertTrue(assertThrows(IllegalStateException.class, () ->
                        U07NonProductionF3A1U05Flow.requireEligiblePreF3(
                                failedHandoff, unavailable))
                        .getMessage().contains("U07_U03_RISK_COMMIT_EVIDENCE_REQUIRED"));

                assertEquals(Integer.valueOf(3), unavailable.getCommitResult().committedVersion);
                assertEquals("UNAVAILABLE", ((Map<?,?>) cdps.findById(created.getCdpId())
                        .get().getPatientState().get("safety_gate")).get("safety_gate"));
                assertEquals(3, versionsRepo.findByCdpIdOrderByVersionNumberDesc(
                        created.getCdpId()).size());
                return;
            }
            assertEquals(expectedRisk, riskDecision.getOutcomeCode());
            U03StateProposal riskProposal = new U03StateProposalFactory()
                    .createNonProductionValid(admitted, riskDecision, governed);
            com.aidoctor.contracts.v1.StateTypes.CommitResult riskCommit =
                    tx.execute(status -> new U03CommitService(committer).commitNonProduction(admitted, riskProposal));
            assertEquals("COMMITTED", riskCommit.status, "P01 reason=" + riskCommit.reasonCode);
            assertEquals(Integer.valueOf(3), riskCommit.committedVersion);
            assertEquals(expectedRisk, ((Map<?,?>) cdps.findById(created.getCdpId())
                    .get().getPatientState().get("current_risk_assessment")).get("outcome_code"));
            java.util.List<U03RuntimeTraceRecord> recorded = new java.util.ArrayList<>();
            U03PostCommitFinalizationOutcome finalized = new U03PostCommitFinalizer(
                    new U03RuntimeTraceService(recorded::add),
                    new U03OutboundProducer()).finalizeCommittedExecution(
                            admitted, governed, riskDecision, riskProposal, riskCommit);
            assertEquals(U03PostCommitFinalizationOutcome.DELIVERABLE, finalized.getStatus());
            assertEquals(1, recorded.size());
            U03OutboundHandoff riskHandoff = finalized.getOutboundHandoff();
            assertEquals(Integer.valueOf(3), riskHandoff.getCommittedClinicalStateVersion());
            RuntimeBindingService durableBinding = ctx.getBean(RuntimeBindingService.class);
            ClinicalRunRepository durableRuns = ctx.getBean(ClinicalRunRepository.class);
            String persistedThread = durableBinding.requireBinding(
                    created.getConsultationId()).getThreadId();
            assertDoesNotThrow(() ->
                    U07NonProductionPhysicalFollowupCoordinator.requireDurableRuntimePreF3(
                            riskHandoff, durableBinding, durableRuns,
                            persistedThread, created.getRunId()));
            int beforeForgedRun = cdps.findById(created.getCdpId()).get()
                    .getVersion().intValue();
            assertTrue(assertThrows(IllegalStateException.class, () ->
                    U07NonProductionPhysicalFollowupCoordinator.requireDurableRuntimePreF3(
                            riskHandoff, durableBinding, durableRuns,
                            persistedThread, "forged-run"))
                    .getMessage().contains("DURABLE_THREAD_RUN_AUTHORITY_MISMATCH"));
            assertEquals(beforeForgedRun, cdps.findById(created.getCdpId()).get()
                    .getVersion().intValue());

            U04ExecutionResult gated = tx.execute(status -> safety.applyU04(riskHandoff,
                    U04ScopeContext.current(true, true), "ci-nonprod-safety"));
            assertEquals(U04ExecutionResult.COMMIT_COMPLETE, gated.getStatus());
            assertEquals(expectedGate, gated.getDecision().getGate());
            int safetyCommittedVersion = cdps.findById(created.getCdpId()).get()
                    .getVersion().intValue();
            if (U04SafetyGateDecision.BLOCKED.equals(expectedGate)
                    || U04SafetyGateDecision.UNAVAILABLE.equals(expectedGate)) {
                assertTrue(assertThrows(IllegalStateException.class, () ->
                        U07NonProductionF3A1U05Flow.requireEligiblePreF3(riskHandoff, gated))
                        .getMessage().contains("U07_U04_SAFETY_PRE_F3_NOT_ELIGIBLE"));
            } else {
                assertDoesNotThrow(() ->
                        U07NonProductionF3A1U05Flow.requireEligiblePreF3(riskHandoff, gated));
            }
            assertEquals(safetyCommittedVersion,
                    cdps.findById(created.getCdpId()).get().getVersion().intValue());

            assertEquals("COMMITTED", gated.getCommitResult().status);
            assertEquals(Integer.valueOf(4), gated.getCommitResult().committedVersion);
            assertEquals(expectedGate, ((Map<?,?>) cdps.findById(created.getCdpId())
                    .get().getPatientState().get("safety_gate")).get("safety_gate"));
            // Real U04 MySQL readback alone never mints the required A1/F3
            // post-barrier authority for U05 (or downstream U06 question delivery).
            U04A1U05AuthorityBridge continuation = new U04A1U05AuthorityBridge(
                    cdpId -> {
                        CDP currentCdp = cdps.findById(cdpId).orElseThrow(
                                IllegalStateException::new);
                        Map<?,?> gate = (Map<?,?>)currentCdp.getPatientState().get("safety_gate");
                        return new U04A1U05AuthorityBridge.GateReadback(
                                currentCdp.getVersion().intValue(),
                                (String)gate.get("safety_gate"),
                                (String)gate.get("source_u03_decision_ref"),
                                (String)gate.get("u04_policy_ref"));
                    },
                    (consultation, cdpId, version) -> null);
            if (U04SafetyGateDecision.ALLOW.equals(expectedGate)
                    || U04SafetyGateDecision.RESTRICTED.equals(expectedGate)) {
                assertTrue(assertThrows(IllegalStateException.class, () ->
                        continuation.load(riskHandoff, gated, "ci-nonprod-safety",
                                "cdp-version-4"))
                        .getMessage().contains("A1_F3_BARRIER_AUTHORITY_NOT_CURRENT"));
            } else {
                assertTrue(assertThrows(IllegalStateException.class, () ->
                        continuation.load(riskHandoff, gated, "ci-nonprod-safety",
                                "cdp-version-4"))
                        .getMessage().contains("U04_COMMITTED_ELIGIBLE_GATE_REQUIRED"));
            }
            assertEquals(4, versionsRepo.findByCdpIdOrderByVersionNumberDesc(
                    created.getCdpId()).size());
            U04ExecutionResult staleGate = safety.applyU04(riskHandoff,
                    U04ScopeContext.current(true, true), "ci-nonprod-safety");
            assertEquals(U04ExecutionResult.ADMISSION_FAILED, staleGate.getStatus());
            assertEquals(4, cdps.findById(created.getCdpId()).get().getVersion().intValue());
            assertThrows(IllegalStateException.class, () ->
                    safety.applyU04(null, U04ScopeContext.current(true, true), "ci-nonprod-safety"));

            assertThrows(IllegalStateException.class, () -> handoff.executeCommittedStart(
                    created.getConsultationId(), created.getRunId(), raw));
            assertEquals(4, cdps.findById(created.getCdpId()).get().getVersion().intValue());

            // POSITIVE physical handoff: the real committed U02/P05 -> U03/P01
            // -> S14/U04 is the input to an actual F3 CDP CAS commit/readback.
            // The F3 decision remains a clearly labeled TEST-ONLY candidate;
            // this verifies wiring, not independent clinical F3 adjudication.
            if (U04SafetyGateDecision.ALLOW.equals(expectedGate)
                    || U04SafetyGateDecision.RESTRICTED.equals(expectedGate)) {
                U07NonProductionPhysicalFollowupCoordinator.requireCurrentU04Version(
                        created.getCdpId(), gated, cdps);
                StateRepositoryPort physicalState =
                        new ClinicalCdpStateRepositoryAdapter(cdps, versions, new ObjectMapper());
                String f3Effect = "synthetic-first-round-f3-" + scenario;
                com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalF3Owner
                        physicalF3 = new com.aidoctor.diagnosis.runtime.foundation.adapters
                        .U07NonProductionPhysicalF3Owner(
                        new com.aidoctor.diagnosis.runtime.foundation.adapters
                                .U07NonProductionPhysicalF3Owner.F3CommitPort() {
                            @Override public int currentVersion(String cdpId) {
                                return physicalState.readCurrentVersion(cdpId);
                            }
                            @Override public int commitAuthorized(String cdpId, int version,
                                    com.aidoctor.diagnosis.runtime.foundation.adapters
                                            .U07NonProductionPhysicalF3Owner.PreparedF3 decision) {
                                StateRepositoryPort.AtomicCommitOutcome committed =
                                        physicalState.attemptAtomicCommit(
                                                new StateRepositoryPort.AtomicCommitCommand(
                                                        cdpId, version, decision.patchId,
                                                        decision.idempotencyKey, decision.patch));
                                if (committed.status
                                        != StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED)
                                    throw new IllegalStateException("TEST_F3_CAS_FAILED");
                                return committed.committedVersion;
                            }
                        }, cdps, (consultation, cdpId, version) -> {
                            assertEquals(created.getConsultationId(), consultation);
                            assertEquals(created.getCdpId(), cdpId);
                            assertEquals(4, version);
                            com.aidoctor.contracts.v1.StateTypes.StatePatchOperation op =
                                    new com.aidoctor.contracts.v1.StateTypes.StatePatchOperation();
                            op.op = "ADD";
                            op.path = "/patient_state/f3_gap_assessment";
                            Map<String,Object> payload = new LinkedHashMap<String,Object>();
                            payload.put("f3_canonical_effect_id", f3Effect);
                            payload.put("gap_id", "synthetic-first-round-gap-" + scenario);
                            op.value = payload;
                            op.source = "PATIENT_FACT";
                            op.sensitivity = "PHI";
                            com.aidoctor.contracts.v1.StateTypes.StatePatch patch =
                                    new com.aidoctor.contracts.v1.StateTypes.StatePatch();
                            patch.operations = Collections.singletonList(op);
                            return new com.aidoctor.diagnosis.runtime.foundation.adapters
                                    .U07NonProductionPhysicalF3Owner.PreparedF3(
                                    f3Effect, "test-first-round-f3-patch-" + scenario,
                                    "test-first-round-f3-idem-" + scenario, patch);
                        });
                com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionF3A1U05Flow
                        .F3Committed physical = tx.execute(status ->
                                physicalF3.executeAndReadback(
                                        riskHandoff.getConsultationId(),
                                        riskHandoff.getCdpId()));
                assertTrue(physical.committedAndReadback);
                assertEquals(5, physical.persistedVersion);
                assertEquals(f3Effect, physical.effectId);
                assertEquals(f3Effect, ((Map<?, ?>)cdps.findById(created.getCdpId())
                        .get().getPatientState().get("f3_gap_assessment"))
                        .get("f3_canonical_effect_id"));
                assertEquals(5, versionsRepo.findByCdpIdOrderByVersionNumberDesc(
                        created.getCdpId()).size());
                // F3 physical readback alone cannot mint post-F3 medical safety
                // or A1 route authority. The independently supplied evidence is
                // intentionally absent; issuance must fail before any A1 insert.
                com.aidoctor.diagnosis.runtime.foundation.adapters.U07DurableNonProductionA1Issuer
                        unapprovedA1 = new com.aidoctor.diagnosis.runtime.foundation.adapters
                        .U07DurableNonProductionA1Issuer(
                                cdps, new org.springframework.jdbc.core.JdbcTemplate(
                                        ctx.getBean(javax.sql.DataSource.class)),
                                tx, (consultation, cdpId, version, effect) -> null);
                assertTrue(assertThrows(IllegalStateException.class, () ->
                        unapprovedA1.issue(created.getConsultationId(),
                                created.getCdpId(), physical.persistedVersion))
                        .getMessage().contains("U07_A1_INDEPENDENT_SAFETY_NOT_AUTHORIZED"));
                assertEquals(5, cdps.findById(created.getCdpId()).get().getVersion().intValue());
                assertEquals(5, versionsRepo.findByCdpIdOrderByVersionNumberDesc(
                        created.getCdpId()).size());

                // NONPRODUCTION TEST-ONLY A1 positive integration: independently
                // inspect the actual prior U04 gate and current persisted F3.
                // This is NOT a real medical safety adjudicator or production authority.
                org.springframework.jdbc.core.JdbcTemplate a1Jdbc =
                        new org.springframework.jdbc.core.JdbcTemplate(
                                ctx.getBean(javax.sql.DataSource.class));
                a1Jdbc.execute("CREATE TABLE IF NOT EXISTS u07_a1_attestation ("
                        + "consultation_id VARCHAR(64) NOT NULL, cdp_id VARCHAR(64) NOT NULL, "
                        + "cdp_version INT NOT NULL, f3_effect_id VARCHAR(128) NOT NULL, "
                        + "barrier_ref VARCHAR(128) NOT NULL, revalidation_ref VARCHAR(128) NOT NULL, "
                        + "route_source_ref VARCHAR(128) NOT NULL, "
                        + "route_authorization_ref VARCHAR(128) NOT NULL, "
                        + "routing_policy_version VARCHAR(128) NOT NULL, "
                        + "PRIMARY KEY (consultation_id, cdp_id, cdp_version)) ENGINE=InnoDB");
                // Model independent post-F3 authority being withdrawn after
                // durable issuance. A historical row must not grant current U05
                // routing permission once its issuing authority is revoked.
                java.util.concurrent.atomic.AtomicBoolean testSafetyCurrent =
                        new java.util.concurrent.atomic.AtomicBoolean(true);
                com.aidoctor.diagnosis.runtime.foundation.adapters.U07DurableNonProductionA1Issuer
                        testOnlyA1 = new com.aidoctor.diagnosis.runtime.foundation.adapters
                        .U07DurableNonProductionA1Issuer(cdps, a1Jdbc, tx,
                                (consultation, cdpId, version, effect) -> {
                                    if (!created.getConsultationId().equals(consultation)
                                            || !created.getCdpId().equals(cdpId)
                                            || version != 5 || !f3Effect.equals(effect))
                                        throw new IllegalStateException("TEST_A1_IDENTITY_MISMATCH");
                                    if (!testSafetyCurrent.get())
                                        return null;
                                    CDP inspected = cdps.findById(cdpId)
                                            .orElseThrow(IllegalStateException::new);
                                    if (inspected.getVersion() != 5
                                            || !f3Effect.equals(((Map<?, ?>)inspected
                                            .getPatientState().get("f3_gap_assessment"))
                                            .get("f3_canonical_effect_id"))
                                            || !expectedGate.equals(((Map<?, ?>)inspected
                                            .getPatientState().get("safety_gate"))
                                            .get("safety_gate")))
                                        throw new IllegalStateException("TEST_A1_READBACK_MISMATCH");
                                    return new com.aidoctor.diagnosis.runtime.foundation.adapters
                                            .U07DurableNonProductionA1Issuer.VerifiedEvidence(
                                            effect, "test-only-post-f3-barrier-" + scenario,
                                            "test-only-current-revalidation-" + scenario,
                                            "test-only-u05-route-source-" + scenario,
                                            "test-only-u05-route-auth-" + scenario,
                                            "test-only-routing-policy-v1",
                                            true, true, true);
                                });
                U04A1U05AuthorityBridge.A1BarrierAuthority attested =
                        testOnlyA1.issue(created.getConsultationId(),
                                created.getCdpId(), physical.persistedVersion);
                assertEquals(f3Effect, attested.f3CommitRef);
                assertEquals(f3Effect, testOnlyA1.loadAttested(created.getConsultationId(),
                        created.getCdpId(), physical.persistedVersion).f3CommitRef);
                assertEquals(1, a1Jdbc.queryForObject(
                        "SELECT COUNT(*) FROM u07_a1_attestation WHERE consultation_id=? AND cdp_id=? AND cdp_version=?",
                        Integer.class, created.getConsultationId(), created.getCdpId(), 5).intValue());
                // Replaying the same attestation must not insert another row.
                testOnlyA1.issue(created.getConsultationId(), created.getCdpId(), 5);
                assertEquals(1, a1Jdbc.queryForObject(
                        "SELECT COUNT(*) FROM u07_a1_attestation WHERE consultation_id=? AND cdp_id=? AND cdp_version=?",
                        Integer.class, created.getConsultationId(), created.getCdpId(), 5).intValue());
                assertEquals(5, cdps.findById(created.getCdpId()).get().getVersion().intValue());
                // Consume the durable A1 row through the real U04 -> A1 -> U05
                // authority bridge rather than constructing a U05 authority claim.
                U04A1U05AuthorityBridge physicalBridge = new U04A1U05AuthorityBridge(
                        new com.aidoctor.diagnosis.runtime.foundation.adapters
                                .ClinicalCdpGateReadbackAdapter(cdps),
                        new com.aidoctor.diagnosis.runtime.foundation.adapters
                                .ClinicalCdpF3BoundA1AuthorityPort(cdps, testOnlyA1));
                if (U04SafetyGateDecision.ALLOW.equals(expectedGate)) {
                    com.aidoctor.diagnosis.runtime.u05.U05AdmissionAuthoritySnapshot
                            admittedA1 = physicalBridge.load(
                                    riskHandoff, gated, "ci-nonprod-safety", "cdp-physical-v5");
                    assertEquals(5, admittedA1.getCurrentClinicalStateVersion());
                    assertEquals(f3Effect, attested.f3CommitRef);
                    assertTrue(admittedA1.isA1CanonicalF3Complete());
                    assertTrue(admittedA1.isPostF3SafetyBarrierComplete());
                    assertTrue(admittedA1.isF3RevalidatedCurrent());
                    assertTrue(admittedA1.isRouteAuthorizationCurrent());

                    // Real U05 application, P01 commit and JDBC-backed CDP
                    // readback in the SAME consultation after physical F3/A1.
                    // RDP-05 readiness facts and routing ports remain explicitly
                    // synthetic NONPRODUCTION host decisions.
                    java.util.List<com.aidoctor.diagnosis.runtime.u05.U05ReadinessInput>
                            readinessInputs = new java.util.ArrayList<>();
                    for (String domain : Arrays.asList("F1", "F3", "F5", "F6")) {
                        boolean present = "F1".equals(domain) || "F3".equals(domain);
                        String signal = "F1".equals(domain) ? "FRAMED_IN_SCOPE"
                                : "F3".equals(domain) ? "CAN_ASK_MORE" : null;
                        String kind = "F1".equals(domain) ? "SCOPE_OR_FRAMING"
                                : "F3".equals(domain) ? "ONLINE_INFORMATION_GAP"
                                : "F5".equals(domain) ? "DDX_OR_MUST_EXCLUDE" : "OFFLINE_EVIDENCE";
                        readinessInputs.add(new com.aidoctor.diagnosis.runtime.u05.U05ReadinessInput(
                                "test-u05-input-" + domain, domain, "test-owner-" + domain,
                                kind, present ? "PRESENT" : "NOT_YET_APPLICABLE", signal,
                                created.getConsultationId(), created.getCdpId(), 5,
                                "test-u05-decision-" + domain, "test-u05-state-" + domain,
                                "test-u05-applicability-" + domain,
                                Collections.singletonList("test-evidence-" + domain),
                                Collections.singletonList("test-policy-" + domain),
                                "2026-10-16T00:00:00Z", "CURRENT", null));
                    }
                    String u05Context = com.aidoctor.diagnosis.runtime.u05
                            .U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT;
                    String inputSet = com.aidoctor.diagnosis.runtime.u05
                            .U05ReadinessInputManifest.semanticSetIdentity(
                                    created.getConsultationId(), created.getCdpId(), 5,
                                    u05Context, com.aidoctor.diagnosis.runtime.u05
                                            .U05ReadinessInputManifest.RDP05_CONTRACT_VERSION,
                                    readinessInputs);
                    com.aidoctor.diagnosis.runtime.u05.U05ReadinessInputManifest manifest =
                            new com.aidoctor.diagnosis.runtime.u05.U05ReadinessInputManifest(
                                    "test-u05-manifest-" + scenario, inputSet,
                                    created.getConsultationId(), created.getCdpId(), 5, u05Context,
                                    com.aidoctor.diagnosis.runtime.u05.U05ReadinessInputManifest
                                            .RDP05_CONTRACT_VERSION, readinessInputs);
                    com.aidoctor.diagnosis.runtime.u05.U05ConsumerInboundRequest u05Request =
                            new com.aidoctor.diagnosis.runtime.u05.U05ConsumerInboundRequest(
                                    "test-u05-request-" + scenario,
                                    created.getConsultationId(), created.getCdpId(), 5,
                                    admittedA1.getAuthoritativeStateRef(), u05Context,
                                    com.aidoctor.diagnosis.runtime.u05.U05ConsumerInboundRequest
                                            .U04_A1_POST_BARRIER_ROUTING,
                                    admittedA1.getRouteSourceRef(),
                                    com.aidoctor.diagnosis.runtime.u05.U05ConsumerInboundRequest
                                            .U05_ELIGIBLE,
                                    admittedA1.getGateRef(), admittedA1.getGateCommitRef(),
                                    admittedA1.getGateValue(), admittedA1.getRouteAuthorizationType(),
                                    admittedA1.getRouteAuthorizationRef(),
                                    admittedA1.getRoutingPolicyVersion(), "A1", null, null,
                                    manifest.getManifestRef(), manifest.getSetIdentity(),
                                    manifest.authoritativeRecordRefs(), "test-u05-event",
                                    "test-u05-business-event", "synthetic-correlation",
                                    "synthetic-trace",
                                    com.aidoctor.diagnosis.runtime.u05.U05AdmissionService
                                            .CONTRACT_VERSION,
                                    "ci-nonprod-safety", "2026-10-16T00:00:00Z");
                    com.aidoctor.diagnosis.runtime.u05.U05NonProductionApplicationService realU05 =
                            new com.aidoctor.diagnosis.runtime.u05.U05NonProductionApplicationService(
                                    new com.aidoctor.diagnosis.runtime.u05.U05AdmissionService(
                                            new com.aidoctor.diagnosis.runtime.u05
                                                    .U05InMemoryAdmissionLedger()),
                                    new com.aidoctor.diagnosis.runtime.u05.U05ClinicalReadinessPolicy(),
                                    new com.aidoctor.diagnosis.runtime.u05
                                            .U05ReadinessStateProposalFactory(),
                                    new com.aidoctor.diagnosis.runtime.u05.U05CommitService(
                                            committer,
                                            new com.aidoctor.diagnosis.runtime.foundation.adapters
                                                    .ClinicalCdpReadbackAdapter(cdps)),
                                    (readinessInput, decision, evidence) ->
                                            new com.aidoctor.diagnosis.runtime.u05
                                                    .U05RoutingCurrentness(
                                                    evidence.getCommittedClinicalStateVersion(),
                                                    "test-u05-current-route", true, true, true,
                                                    true, true, readinessInput.getAcceptedU04GateRef(),
                                                    readinessInput.getGateValue(), null),
                                    new com.aidoctor.diagnosis.runtime.u05.U05RoutingService(
                                            new com.aidoctor.diagnosis.runtime.u05
                                                    .U05InMemoryRouteLedger(),
                                            (readinessInput, evidence, currentness, consequence, target, action) ->
                                                    new com.aidoctor.diagnosis.runtime.u05
                                                            .U05DownstreamPermissionDecision(
                                                            "test-u05-permission", readinessInput.getConsultationId(),
                                                            readinessInput.getCdpId(),
                                                            currentness.getCurrentU04GateRef(), null,
                                                            consequence, action, target,
                                                            "PERMITTED", "test-route-permission",
                                                            "test-policy", "v1", "CURRENT")));
                    com.aidoctor.diagnosis.runtime.u05.U05ExecutionResult u05Result =
                            tx.execute(status -> realU05.execute(
                                    u05Request, manifest, admittedA1));
                    assertEquals(com.aidoctor.diagnosis.runtime.u05.U05ExecutionResult.ROUTING_COMPLETE,
                            u05Result.getStatus());
                    assertEquals("COMMITTED", u05Result.getCommitResult().status);
                    assertEquals(6, cdps.findById(created.getCdpId()).get().getVersion().intValue());
                    assertTrue(cdps.findById(created.getCdpId()).get().getPatientState()
                            .get("clinical_readiness") instanceof Map<?, ?>);
                    assertEquals(6, versionsRepo.findByCdpIdOrderByVersionNumberDesc(
                            created.getCdpId()).size());

                    // SAME Consultation/CDP: use the actual U05 committed route
                    // as the prerequisite for the governed U06 Mode-2 owner.
                    assertEquals(com.aidoctor.diagnosis.runtime.u05
                                    .U05DownstreamRoutingDecision.TO_U06_QUESTION_PATH,
                            u05Result.getRoutingDecision().getDownstreamConsequence());
                    assertEquals(com.aidoctor.diagnosis.runtime.u05
                                    .U05DownstreamRoutingDecision.ELIGIBLE,
                            u05Result.getRoutingDecision().getRoutingStatus());
                    com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest u06Request =
                            new com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest(
                                    "first-round-u06-select-" + scenario,
                                    created.getConsultationId(), created.getCdpId(),
                                    com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest
                                            .QUESTION_SELECTION_DELIVERY,
                                    com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest
                                            .U05_QUESTION_ROUTING,
                                    u05Result.getRoutingDecision().getRoutingDecisionId(),
                                    6, 6,
                                    com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest
                                            .SYNTHETIC_STRUCTURAL_NONPROD,
                                    com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest
                                            .SYNTHETIC_VERIFICATION_BINDING,
                                    "test-u06-dependency", "test-u05-readiness",
                                    "test-question-policy", "test-d04-policy",
                                    "test-u06-event", "test-u06-business-event",
                                    trace.getThreadId(), created.getRunId(), 0L,
                                    "synthetic-correlation", "synthetic-trace",
                                    "2026-10-16T00:00:00Z",
                                    com.aidoctor.diagnosis.runtime.u06
                                            .U06AdmissionEvidence.syntheticCurrentAllow());
                    com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle u06Decision =
                            new com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle(
                                    com.aidoctor.diagnosis.runtime.u06
                                            .U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED,
                                    f3Effect, "test-u06-gap-" + scenario,
                                    "DECISION_MATERIAL", true,
                                    com.aidoctor.diagnosis.runtime.u06
                                            .U06SyntheticDecisionBundle.CONTINUE,
                                    com.aidoctor.diagnosis.runtime.u06
                                            .U06SyntheticDecisionBundle.SELECTED,
                                    "test-u06-selection-effect-" + scenario,
                                    "test-u06-question-" + scenario, "TEST:FOLLOWUP",
                                    "synthetic follow-up question", "sha256:synthetic-question",
                                    null, null);
                    com.aidoctor.diagnosis.runtime.u06.U06NonProductionPhysicalMode23Service
                            u06Physical = new com.aidoctor.diagnosis.runtime.u06
                                    .U06NonProductionPhysicalMode23Service(
                                    (request, decision, mode1Safety) -> {
                                        throw new IllegalStateException("TEST_U06_MODE1_NOT_AUTHORIZED");
                                    }, cdps, committer,
                                    new com.aidoctor.diagnosis.runtime.u06.U06AdmissionService());
                    com.aidoctor.diagnosis.runtime.u06.U06ExecutionResult selected =
                            tx.execute(status -> u06Physical.execute(u06Request, u06Decision, null));
                    assertEquals(com.aidoctor.diagnosis.runtime.u06.U06ExecutionResult
                            .MODE2_SELECTION_PERSISTED, selected.getStatus());
                    assertEquals(7, cdps.findById(created.getCdpId()).get()
                            .getVersion().intValue());
                    assertEquals(7, versionsRepo.findByCdpIdOrderByVersionNumberDesc(
                            created.getCdpId()).size());
                    assertFalse(cdps.findById(created.getCdpId()).get()
                            .getPatientState().containsKey("pending_question"));
                    // An actual U05-backed U06 selection is NOT delivery.
                    // A fabricated confirmation must fail before any WAIT owner
                    // or database delivery effect can be invoked.
                    java.util.concurrent.atomic.AtomicInteger waitInvocations =
                            new java.util.concurrent.atomic.AtomicInteger();
                    com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery
                            noDelivery = new com.aidoctor.diagnosis.runtime.u06
                                    .U07NonProductionDeliveryWaitRecovery(
                                    cdps, new org.springframework.jdbc.core.JdbcTemplate(ctx.getBean(javax.sql.DataSource.class)),
                                    tx, committer, receipt -> false,
                                    (receipt, version) -> {
                                        waitInvocations.incrementAndGet();
                                        throw new AssertionError("UNVERIFIED_DELIVERY_REACHED_WAIT");
                                    });
                    com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery.Receipt
                            unverified = new com.aidoctor.diagnosis.runtime.u06
                                    .U07NonProductionDeliveryWaitRecovery.Receipt(
                                    u06Request, u06Decision, "unverified-delivery-id",
                                    "unverified-delivery-effect", "not-confirmed");
                    assertThrows(IllegalStateException.class, () -> noDelivery.accept(unverified));
                    assertEquals(0, waitInvocations.get());
                    assertEquals(7, cdps.findById(created.getCdpId()).get()
                            .getVersion().intValue());

                    // Test-only externally confirmed receipt, scoped to this
                    // U01 consultation and its real U05-backed U06 selection.
                    com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery.Receipt
                            confirmed = new com.aidoctor.diagnosis.runtime.u06
                                    .U07NonProductionDeliveryWaitRecovery.Receipt(
                                    u06Request, u06Decision, "fixture-delivery-" + scenario,
                                    "fixture-delivery-effect-" + scenario, "fixture-confirmed");
                    com.aidoctor.diagnosis.runtime.u06.U07ExistingU06WaitAuthority hostWait =
                            new com.aidoctor.diagnosis.runtime.u06.U07ExistingU06WaitAuthority(
                                    new com.aidoctor.diagnosis.runtime.u06.wait
                                            .ConsultationWaitTransitionService(
                                            ctx.getBean(ConsultationRepository.class),
                                            ctx.getBean(com.aidoctor.diagnosis.runtime.u06.wait
                                                    .ConsultationWaitEffectRepository.class)),
                                    new com.aidoctor.diagnosis.runtime.u06.wait.U06WaitCoordinator(
                                            new com.aidoctor.diagnosis.runtime.foundation
                                                    .RuntimeWaitCheckpointService(
                                                    ctx.getBean(com.aidoctor.diagnosis.runtime.foundation
                                                            .RuntimeThreadStateRepository.class),
                                                    ctx.getBean(com.aidoctor.diagnosis.runtime.foundation
                                                            .RuntimeWaitCheckpointRepository.class)),
                                            new com.aidoctor.diagnosis.runtime.foundation
                                                    .RuntimeThreadWaitTransitionService(
                                                    ctx.getBean(com.aidoctor.diagnosis.runtime.foundation
                                                            .RuntimeThreadStateRepository.class),
                                                    ctx.getBean(com.aidoctor.diagnosis.runtime.foundation
                                                            .RuntimeWaitCheckpointRepository.class))));
                    com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery
                            confirmedDelivery = new com.aidoctor.diagnosis.runtime.u06
                                    .U07NonProductionDeliveryWaitRecovery(
                                    cdps, new org.springframework.jdbc.core.JdbcTemplate(ctx.getBean(javax.sql.DataSource.class)),
                                    tx, committer, receipt ->
                                            "fixture-confirmed".equals(receipt.confirmationRef)
                                            && confirmed.deliveryId.equals(receipt.deliveryId),
                                    (receipt, version) ->
                                            tx.execute(status -> hostWait.establish(receipt, version)));
                    confirmedDelivery.accept(confirmed);
                    com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery.WaitReceipt
                            durableWait = confirmedDelivery.recover(confirmed);
                    assertNotNull(durableWait);
                    assertEquals(8, cdps.findById(created.getCdpId()).get()
                            .getVersion().intValue());
                    assertEquals("WAITING_USER", ctx.getBean(ConsultationRepository.class)
                            .findById(created.getConsultationId()).get().getLifecycleStatus());
                    assertEquals("AWAITING_USER", ctx.getBean(com.aidoctor.diagnosis.runtime.foundation
                            .RuntimeThreadStateRepository.class)
                            .findById(persistedThread).get().getRuntimeStatus());
                    assertTrue(ctx.getBean(com.aidoctor.diagnosis.runtime.foundation
                            .RuntimeWaitCheckpointRepository.class)
                            .findById(durableWait.checkpointId).isPresent());
                    assertEquals(durableWait.checkpointId,
                            confirmedDelivery.recover(confirmed).checkpointId);
                    assertEquals(8, versionsRepo.findByCdpIdOrderByVersionNumberDesc(
                            created.getCdpId()).size());

                    // Full two-turn physical continuation. The durable WAIT belongs
                    // to the U01-created Consultation/CDP/Thread/first Run above.
                    U07NonProductionAnswerResumeAdmission answerAdmission =
                            new U07NonProductionAnswerResumeAdmission(
                                    ctx.getBean(ConsultationRepository.class),
                                    ctx.getBean(RuntimeThreadStateRepository.class),
                                    ctx.getBean(RuntimeWaitCheckpointRepository.class), cdps);
                    String answerEventId = "real-second-answer-event-" + scenario;
                    U02ExecutionCommand answerCandidate = answerAdmission.requireWaitingAnswer(
                            created.getConsultationId(), created.getCdpId(), persistedThread,
                            created.getRunId(), durableWait.checkpointId,
                            "test-u06-question-" + scenario, confirmed.deliveryId,
                            "synthetic-person", answerEventId,
                            "I have severe difficulty breathing", "synthetic-answer-correlation",
                            "synthetic-answer-trace");
                    assertEquals(8, answerCandidate.baseClinicalStateVersion);
                    assertThrows(IllegalStateException.class, () ->
                            answerAdmission.requireWaitingAnswer(
                                    created.getConsultationId(), created.getCdpId(), "wrong-thread",
                                    created.getRunId(), durableWait.checkpointId,
                                    "test-u06-question-" + scenario, confirmed.deliveryId,
                                    "synthetic-person", "forged-answer-event", "forged answer",
                                    "synthetic-answer-correlation", "synthetic-answer-trace"));

                    JdbcTemplate f8Jdbc = new JdbcTemplate(ctx.getBean(javax.sql.DataSource.class));
                    U07NonProductionDurableF8Consume f8 = new U07NonProductionDurableF8Consume(
                            f8Jdbc, tx, new CanonicalBusinessEventLedger(
                                    ctx.getBean(CanonicalBusinessEventRepository.class)),
                            new ClinicalRunCoordinator(durableBinding,
                                    ctx.getBean(CanonicalBusinessEventRepository.class),
                                    durableRuns),
                            ctx.getBean(ConsultationRepository.class),
                            ctx.getBean(RuntimeThreadStateRepository.class),
                            ctx.getBean(RuntimeWaitCheckpointRepository.class));
                    long runCountBefore = durableRuns.count();
                    long eventCountBefore = ctx.getBean(CanonicalBusinessEventRepository.class).count();
                    String resumedRun = f8.consume(answerCandidate, durableWait.checkpointId,
                            "real-second-answer-idem-" + scenario,
                            "sha256:real-second-answer-" + scenario);
                    assertNotEquals(created.getRunId(), resumedRun);
                    assertEquals(runCountBefore + 1, durableRuns.count());
                    assertEquals(eventCountBefore + 1,
                            ctx.getBean(CanonicalBusinessEventRepository.class).count());
                    assertEquals("CONSUMED", ctx.getBean(RuntimeWaitCheckpointRepository.class)
                            .findById(durableWait.checkpointId).get().getCheckpointStatus());
                    assertEquals("ACTIVE", ctx.getBean(ConsultationRepository.class)
                            .findById(created.getConsultationId()).get().getLifecycleStatus());
                    assertEquals("ACTIVE", ctx.getBean(RuntimeThreadStateRepository.class)
                            .findById(persistedThread).get().getRuntimeStatus());
                    assertThrows(IllegalStateException.class, () ->
                            answerAdmission.requireWaitingAnswer(
                                    created.getConsultationId(), created.getCdpId(), persistedThread,
                                    created.getRunId(), durableWait.checkpointId,
                                    "test-u06-question-" + scenario, confirmed.deliveryId,
                                    "synthetic-person", "duplicate-answer-event", "duplicate",
                                    "synthetic-answer-correlation", "synthetic-answer-trace"));
                    U07NonProductionTrustedAnswerU02Bridge authorizedAnswer =
                            new U07NonProductionTrustedAnswerU02Bridge(
                                    durableRuns, ctx.getBean(CanonicalBusinessEventRepository.class), f8);
                    U02ExecutionCommand secondRound =
                            authorizedAnswer.requireAuthorizedNewRun(
                                    answerCandidate, durableWait.checkpointId, resumedRun);
                    assertEquals(created.getConsultationId(), secondRound.consultationId);
                    assertEquals(created.getCdpId(), secondRound.cdpId);
                    assertEquals(persistedThread, secondRound.threadId);
                    assertEquals("PATIENT_REPORTED", secondRound.sourceType);

                    // Nonproduction C01 fixture is deliberately changed only for
                    // the resumed answer; the real U02/P01/P05 services still run.
                    when(client.interpret(any())).thenReturn(resumedRespiratoryCandidate());
                    U02ExecutionResult updatedAnswer =
                            tx.execute(status -> u02.execute(secondRound));
                    assertEquals("COMMITTED", updatedAnswer.status);
                    assertEquals(Integer.valueOf(9),
                            updatedAnswer.commitResult.committedVersion);
                    assertEquals(9, cdps.findById(created.getCdpId()).get()
                            .getVersion().intValue());
                    assertEquals(9, versionsRepo.findByCdpIdOrderByVersionNumberDesc(
                            created.getCdpId()).size());
                    CapabilityCallTraceRecord secondTrace =
                            traces.require(updatedAnswer.capabilityCallId);
                    assertEquals(resumedRun, secondTrace.getRunId());
                    assertEquals(answerEventId, secondTrace.getEventId());
                    assertEquals(Integer.valueOf(8), secondTrace.getClinicalStateVersionBefore());
                    assertEquals(Integer.valueOf(9), secondTrace.getClinicalStateVersionAfter());

                    U03CdpAcceptedEvidenceAuthority secondEvidence =
                            new U03CdpAcceptedEvidenceAuthority(cdps,
                                    (consultation, cdp, version, call, evidence, sources, provenance) -> {
                                        assertEquals(created.getConsultationId(), consultation);
                                        assertEquals(created.getCdpId(), cdp);
                                        assertEquals(9, version);
                                        assertEquals(updatedAnswer.capabilityCallId, call);
                                        assertEquals(Collections.singletonList("EV-RF-RESP-001"), evidence);
                                        return "test-only-second-round-respiratory-acceptance";
                                    });
                    U02U03GovernedSafetyBridge secondSafety =
                            new U02U03GovernedSafetyBridge(cdps,
                                    ctx.getBean(CapabilityCallTraceRepository.class),
                                    secondEvidence, new U03NonProductionAdmissionService(), u04);
                    U03ExecutionCommand secondRiskCommand = new U03ExecutionCommand(
                            created.getConsultationId(), persistedThread, resumedRun,
                            answerEventId, created.getCdpId(), 9,
                            secondRound.correlationId, secondRound.traceId);
                    U03NonProductionExecutionContext secondAdmission =
                            secondSafety.requireU03Admission(updatedAnswer, secondRiskCommand,
                                    U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                                    "ci-nonprod-safety");
                    U03CdpGateCClinicalInputPort secondInput = new U03CdpGateCClinicalInputPort(
                            cdps, (cmd, accepted) -> U03GateCClinicalInput.builder());
                    assertEquals("PRESENT", secondInput.load(secondRiskCommand,
                            secondAdmission.requireAcceptedEvidenceBinding())
                            .evidenceState("EV-RF-RESP-001"));
                    U03GovernedCandidateGateway secondGateway =
                            new U03GovernedCandidateGateway(riskGuard, new U03ReleaseRegistry(),
                                    new U03GateCFrozenRuleEvaluator(secondInput), true);
                    U07NonProductionGovernedRiskSafetyContinuation secondContinuation =
                            new U07NonProductionGovernedRiskSafetyContinuation(
                                    secondGateway, new U03DecisionService(
                                            new U03GateCFrozenDecisionPort()),
                                    new U03StateProposalFactory(),
                                    new U03CommitService(committer),
                                    new U03PostCommitFinalizer(
                                            new U03RuntimeTraceService(recorded::add),
                                            new U03OutboundProducer()),
                                    new U03OutboundProducer(), secondSafety, tx);
                    U07NonProductionGovernedRiskSafetyContinuation.Result secondResult =
                            secondContinuation.executeFromCommittedU02ReplacingExistingRisk(updatedAnswer,
                                    secondRiskCommand,
                                    U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                                    "ci-nonprod-safety", U04ScopeContext.current(true, true));
                    assertEquals("HIGH_RISK", secondResult.risk.getOutcomeCode());
                    assertEquals("COMMITTED", secondResult.riskCommit.status);
                    assertEquals(Integer.valueOf(10),
                            secondResult.riskCommit.committedVersion);
                    assertEquals(U04SafetyGateDecision.BLOCKED,
                            secondResult.gate.getDecision().getGate());
                    assertEquals(Integer.valueOf(11),
                            secondResult.gate.getCommitResult().committedVersion);
                    assertEquals(11, cdps.findById(created.getCdpId()).get()
                            .getVersion().intValue());
                    assertEquals(11, versionsRepo.findByCdpIdOrderByVersionNumberDesc(
                            created.getCdpId()).size());
                    assertThrows(IllegalStateException.class, () ->
                            U07NonProductionF3A1U05Flow.requireEligiblePreF3(
                                    secondResult.handoff, secondResult.gate));
                    assertEquals(resumedRun, secondResult.handoff.getRunId());
                    assertEquals(resumedRun, f8.consume(answerCandidate, durableWait.checkpointId,
                            "real-second-answer-idem-" + scenario,
                            "sha256:real-second-answer-" + scenario));
                    assertThrows(IllegalStateException.class, () ->
                            f8.consume(answerCandidate, durableWait.checkpointId,
                                    "real-second-answer-idem-" + scenario,
                                    "sha256:CONFLICT"));
                    assertEquals(runCountBefore + 1, durableRuns.count());
                    assertEquals(eventCountBefore + 1,
                            ctx.getBean(CanonicalBusinessEventRepository.class).count());
                    assertEquals(1, f8Jdbc.queryForObject(
                            "SELECT COUNT(*) FROM u07_f8_answer_consumption WHERE checkpoint_id=?",
                            Integer.class, durableWait.checkpointId).intValue());
                    assertEquals(11, cdps.findById(created.getCdpId()).get()
                            .getVersion().intValue());

                } else {
                    // RESTRICTED cannot acquire an unrestricted U05 route merely
                    // from a durable A1 row lacking restricted-action permission.
                    assertTrue(assertThrows(IllegalStateException.class, () ->
                            physicalBridge.load(riskHandoff, gated,
                                    "ci-nonprod-safety", "cdp-physical-v5"))
                            .getMessage().contains("A1_RESTRICTED_PERMISSION_REQUIRED"));
                }
                // Once U05 commits v6, the v5 attestation is historical only.
                // Without U05 (RESTRICTED), live revocation is independently
                // enforced at v5. Both cases deny reusable A1 authority.
                testSafetyCurrent.set(false);
                String denied = U04SafetyGateDecision.ALLOW.equals(expectedGate)
                        ? "U07_A1_CDP_VERSION_STALE" : "U07_A1_CURRENT_AUTHORITY_REVOKED";
                assertTrue(assertThrows(IllegalStateException.class, () ->
                        testOnlyA1.loadAttested(created.getConsultationId(),
                                created.getCdpId(), 5)).getMessage().contains(denied));
                String bridgeDenied = U04SafetyGateDecision.ALLOW.equals(expectedGate)
                        ? "U07_A1_INDEPENDENT_ATTESTATION_MISMATCH"
                        : "U07_A1_CURRENT_AUTHORITY_REVOKED";
                assertTrue(assertThrows(IllegalStateException.class, () ->
                        physicalBridge.load(riskHandoff, gated,
                                "ci-nonprod-safety", "cdp-physical-v5"))
                        .getMessage().contains(bridgeDenied));
                assertEquals(1, a1Jdbc.queryForObject(
                        "SELECT COUNT(*) FROM u07_a1_attestation WHERE consultation_id=? AND cdp_id=? AND cdp_version=?",
                        Integer.class, created.getConsultationId(), created.getCdpId(), 5).intValue());
                assertEquals(U04SafetyGateDecision.ALLOW.equals(expectedGate) ? 8 : 5,
                        cdps.findById(created.getCdpId()).get().getVersion().intValue());
                assertTrue(assertThrows(IllegalStateException.class, () ->
                        U07NonProductionPhysicalFollowupCoordinator
                                .requireCurrentU04Version(created.getCdpId(), gated, cdps))
                        .getMessage().contains("U04_SAFETY_GATE_STALE_BEFORE_F3"));
            }
        }
    }


    private static C01U02CapabilityResponse resumedRespiratoryCandidate() {
        C01U02CapabilityResponse response = u02Candidate();
        C01U02CapabilityResponse.ObservationCandidate fact =
                new C01U02CapabilityResponse.ObservationCandidate();
        fact.setObservationId("second-round-respiratory");
        fact.setConceptId("EV-RF-RESP-001");
        fact.setConceptDisplay("synthetic severe respiratory difficulty");
        fact.setRawTextRef("sha256:real-second-round-respiratory");
        fact.setValueSemantics("YES");
        fact.setSourceType("PATIENT_REPORTED");
        fact.setLifecycle("CONFIRMED");
        fact.setConfidenceOrUncertainty(0.95d);
        fact.setProvenance(Collections.singletonList("nonproduction"));
        fact.setAmbiguityFlags(Collections.emptyList());
        fact.setContradictionRefs(Collections.emptyList());
        response.setObservationCandidates(Collections.singletonList(fact));
        return response;
    }

    private static C01U01CapabilityResponse u01Candidate() {
        C01U01CapabilityResponse response = new C01U01CapabilityResponse();
        response.setBusinessStatus("SUCCESS");
        C01U01CapabilityResponse.SubjectCandidate subject =
                new C01U01CapabilityResponse.SubjectCandidate();
        subject.setSubjectType("SELF");
        response.setSubjectCandidate(subject);
        C01U01CapabilityResponse.ProblemCandidate problem =
                new C01U01CapabilityResponse.ProblemCandidate();
        problem.setText("synthetic headache uncertainty");
        response.setProblemCandidate(problem);
        C01U01CapabilityResponse.ScopeCandidate scope =
                new C01U01CapabilityResponse.ScopeCandidate();
        scope.setScope("SYMPTOM");
        response.setScopeCandidate(scope);
        C01U01CapabilityResponse.EarlySafetySignalCandidate signal =
                new C01U01CapabilityResponse.EarlySafetySignalCandidate();
        signal.setDetected(false);
        response.setEarlySafetySignalCandidate(signal);
        return response;
    }

    private static C01U02CapabilityResponse u02Candidate() {
        C01U02CapabilityResponse response = new C01U02CapabilityResponse();
        response.setBusinessStatus("SUCCESS");
        response.setSourceAttribution("USER_TEXT");
        C01U02CapabilityResponse.BindingRef binding = new C01U02CapabilityResponse.BindingRef();
        binding.setBindingId(C01U02CapabilityGateway.BINDING_ID);
        binding.setBindingStatus(CapabilityBindingRecord.ACTIVE);
        binding.setCapabilityId("C01");
        binding.setCapabilityVersion("fixture-c01-u02-1");
        binding.setCapabilitySetVersion("aidoctor-v1-u02");
        binding.setScopeVersion("aidoctor-v1-scope");
        binding.setContractVersion("contracts-v1");
        response.setBindingRef(binding);
        C01U02CapabilityResponse.ObservationCandidate fact =
                new C01U02CapabilityResponse.ObservationCandidate();
        fact.setObservationId("fixture-obs");
        fact.setConceptId("TEST:HEADACHE");
        fact.setConceptDisplay("synthetic headache");
        fact.setRawTextRef("sha256:synthetic-evidence-fixture");
        fact.setValueSemantics("UNKNOWN");
        fact.setSourceType("PATIENT_REPORTED");
        fact.setLifecycle("UNCERTAIN");
        fact.setConfidenceOrUncertainty(0.7d);
        fact.setProvenance(Collections.singletonList("synthetic-only"));
        fact.setAmbiguityFlags(Collections.emptyList());
        fact.setContradictionRefs(Collections.emptyList());
        response.setObservationCandidates(Collections.singletonList(fact));
        return response;
    }
}
