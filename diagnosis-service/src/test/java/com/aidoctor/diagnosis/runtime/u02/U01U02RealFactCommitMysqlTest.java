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
                        .F3Committed physical = physicalF3.executeAndReadback(
                        riskHandoff.getConsultationId(), riskHandoff.getCdpId());
                assertTrue(physical.committedAndReadback);
                assertEquals(5, physical.persistedVersion);
                assertEquals(f3Effect, physical.effectId);
                assertEquals(f3Effect, ((Map<?, ?>)cdps.findById(created.getCdpId())
                        .get().getPatientState().get("f3_gap_assessment"))
                        .get("f3_canonical_effect_id"));
                assertEquals(5, versionsRepo.findByCdpIdOrderByVersionNumberDesc(
                        created.getCdpId()).size());
                assertTrue(assertThrows(IllegalStateException.class, () ->
                        U07NonProductionPhysicalFollowupCoordinator
                                .requireCurrentU04Version(created.getCdpId(), gated, cdps))
                        .getMessage().contains("U04_SAFETY_GATE_STALE_BEFORE_F3"));
            }
        }
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
