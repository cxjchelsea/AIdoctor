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
    public void actualU01ThenU02CommitsUnknownWithTraceAndVersion() {
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
                    "real-u01-u02-event", "real-u01-u02-idem", "synthetic-person", raw, null);
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
                                    Collections.singletonList("synthetic-observation-evidence"),
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
            U03GateCClinicalInputPort input = (cmd, accepted) ->
                    U03GateCClinicalInput.builder().suspectedSepsis("TRUE")
                            .measurement("respiratory_rate_bpm",
                                    U03GateCClinicalInput.Measurement.present(21))
                            .measurement("systolic_bp_mmHg",
                                    U03GateCClinicalInput.Measurement.present(120))
                            .measurement("usual_systolic_bp_mmHg",
                                    U03GateCClinicalInput.Measurement.present(120))
                            .measurement("heart_rate_bpm",
                                    U03GateCClinicalInput.Measurement.present(80)).build();
            U03GovernedCandidateGateway governedGateway = new U03GovernedCandidateGateway(
                    riskGuard, new U03ReleaseRegistry(),
                    new U03GateCFrozenRuleEvaluator(input), true);
            U03GovernedCandidateGateway.GovernedResult governed = governedGateway.assess(admitted);
            U03DecisionOutcome riskDecision = new U03DecisionService(
                    new U03GateCFrozenDecisionPort()).decide(admitted, governed);
            assertEquals("CAUTION", riskDecision.getOutcomeCode());
            U03StateProposal riskProposal = new U03StateProposalFactory()
                    .createNonProductionValid(admitted, riskDecision, governed);
            com.aidoctor.contracts.v1.StateTypes.CommitResult riskCommit =
                    new U03CommitService(committer).commitNonProduction(admitted, riskProposal);
            assertEquals("COMMITTED", riskCommit.status);
            assertEquals(Integer.valueOf(3), riskCommit.committedVersion);
            assertThrows(IllegalStateException.class, () ->
                    safety.applyU04(null, U04ScopeContext.current(true, true), "ci-nonprod-safety"));

            assertThrows(IllegalStateException.class, () -> handoff.executeCommittedStart(
                    created.getConsultationId(), created.getRunId(), raw));
            assertEquals(2, cdps.findById(created.getCdpId()).get().getVersion().intValue());
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
