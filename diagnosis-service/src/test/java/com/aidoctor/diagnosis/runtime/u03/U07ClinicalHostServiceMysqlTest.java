package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07ClinicalHostServicesConfig;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07WorkpackACdpPhysicalMysqlTest;
import com.aidoctor.diagnosis.runtime.u04.*;
import com.aidoctor.diagnosis.runtime.u05.*;
import com.aidoctor.diagnosis.runtime.foundation.adapters.ClinicalCdpReadbackAdapter;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
import com.aidoctor.diagnosis.state.committer.fakes.*;
import com.aidoctor.diagnosis.state.committer.ports.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Host-created REAL U04 policy/proposal/commit service on a disposable MySQL8 CDP.
 * No synthetic version repository or production route; test-only policy ports.
 */
public class U07ClinicalHostServiceMysqlTest {
    private static final String ENV = "ci-nonprod-u07-host";
    private static final String U05_ENV = "ci-nonprod-u05";
    private static final int VERSION = 12;

    @Configuration
    static class ScopedTestCommitter {
        @Bean
        StateCommitter scopedNonproductionCommitter(StateRepositoryPort repository) {
            Clock clock = Clock.fixed(Instant.parse("2026-09-18T00:00:00Z"), ZoneOffset.UTC);
            List<String> order = new ArrayList<String>();
            return new StateCommitter(
                    repository,
                    (id, ver) -> CapabilityPolicyPort.CapabilityDecision.authorized(),
                    (path, capability) -> FieldPermissionPort.FieldPermissionDecision.authorized(),
                    (cdp, capability) -> ConsentPolicyPort.ConsentDecision.authorized(),
                    source -> SourceValidationPort.SourceDecision.authorized(),
                    new InMemoryIdempotencyFake(order),
                    new SyntheticAuditPortFake(clock, order),
                    new RecordingCommitEventEvidenceFake(order),
                    clock);
        }
    }

    @Configuration
    static class ScopedU05TestPorts {
        @Bean U05AdmissionLedger u05AdmissionLedger() {
            return new U05InMemoryAdmissionLedger();
        }
        @Bean U05RouteLedger u05RouteLedger() {
            return new U05InMemoryRouteLedger();
        }
        @Bean U05RoutingCurrentnessPort u05RoutingCurrentnessPort() {
            return (input, decision, evidence) -> new U05RoutingCurrentness(
                    evidence.getCommittedClinicalStateVersion(),
                    "u07-physical-route-" + evidence.getEffectId(),
                    true, true, true, true, true,
                    input.getAcceptedU04GateRef(),
                    input.getGateValue(), input.getAcceptedRestrictedContextRef());
        }
        @Bean U05DownstreamPermissionPort u05DownstreamPermissionPort() {
            return (input, evidence, currentness, consequence, targetUnit, action) ->
                    new U05DownstreamPermissionDecision(
                            "u07-physical-permission-" + targetUnit,
                            input.getConsultationId(), input.getCdpId(),
                            currentness.getCurrentU04GateRef(),
                            input.getAcceptedRestrictedContextRef(),
                            consequence, action, targetUnit,
                            U05DownstreamPermissionDecision.PERMITTED,
                            "u07-isolated-test-permission",
                            "u07-isolated-permission-policy", "v1", "CURRENT");
        }
    }

    @Test
    public void u05ActualSpringServiceCommitsToMysqlWithExactReadbackAndControlledRoute() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            Map<String,Object> flags = new HashMap<String,Object>();
            flags.put("aidoctor.u07.clinical-host.nonproduction-enabled", "true");
            flags.put("aidoctor.u07.persistence.readback-enabled", "true");
            ctx.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("u07-physical-u05-test-opt-in", flags));
            ctx.register(U07WorkpackACdpPhysicalMysqlTest.IsolatedConfig.class,
                    ScopedTestCommitter.class, ScopedU05TestPorts.class,
                    ClinicalCdpReadbackAdapter.class,
                    U07ClinicalHostServicesConfig.class);
            ctx.refresh();
            CDPRepository repo = ctx.getBean(CDPRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            tx.execute(status -> {
                CDP cdp = CDP.builder().id("cdp-1").patientId("synthetic-only")
                        .sessionId("synthetic-only").version(VERSION).build();
                Map<String,Object> initial = new LinkedHashMap<String,Object>();
                Map<String,Object> gate = new LinkedHashMap<String,Object>();
                gate.put("safety_gate", U05ConsumerInboundRequest.GATE_ALLOW);
                gate.put("source_u03_decision_ref", "synthetic-u03");
                initial.put("safety_gate", gate);
                cdp.setPatientState(initial);
                repo.saveAndFlush(cdp);
                return null;
            });

            U05NonProductionApplicationService application =
                    ctx.getBean(U05NonProductionApplicationService.class);
            String context = U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT;
            List<U05ReadinessInput> inputs = Arrays.asList(
                    present(U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION),
                    present(U05ReadinessInput.F3, U05ReadinessInput.CAN_ASK_MORE, VERSION),
                    applicability(U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION),
                    applicability(U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION));
            U05ReadinessInputManifest manifest = manifest(context, VERSION, inputs);
            U05ConsumerInboundRequest inbound = request(manifest, context,
                    U05ConsumerInboundRequest.GATE_ALLOW, null, null, U05_ENV);
            U05AdmissionAuthoritySnapshot authority = authority(context,
                    U05ConsumerInboundRequest.GATE_ALLOW, null, null, true, U05_ENV, true);

            U05ExecutionResult result = application.execute(inbound, manifest, authority);
            assertEquals(U05ExecutionResult.ROUTING_COMPLETE, result.getStatus());
            assertEquals("COMMITTED", result.getCommitResult().status);
            assertEquals(U05DownstreamRoutingDecision.TO_U06_QUESTION_PATH,
                    result.getRoutingDecision().getDownstreamConsequence());
            assertEquals(U05DownstreamRoutingDecision.ELIGIBLE,
                    result.getRoutingDecision().getRoutingStatus());
            assertEquals(13, repo.findById("cdp-1").get().getVersion().intValue());
            Map<String,Object> stored = repo.findById("cdp-1").get().getPatientState();
            assertTrue(stored.get("clinical_readiness") instanceof Map<?,?>);
            assertEquals("CAN_ASK_MORE",
                    ((Map<?,?>)stored.get("clinical_readiness")).get("clinical_readiness"));
            assertEquals(13, ctx.getBean(U05ClinicalReadinessSnapshotPort.class)
                    .read("cdp-1", 13).getVersion());
            assertThrows(IllegalStateException.class, () ->
                    ctx.getBean(U05ClinicalReadinessSnapshotPort.class).read("cdp-1", 12));
            assertEquals(1, ctx.getBean(
                    com.aidoctor.diagnosis.repository.CDPVersionRepository.class)
                    .findByCdpIdOrderByVersionNumberDesc("cdp-1").size());

            // Exact replay may reattach the original successful decision, but
            // MUST NOT duplicate persisted CDP or version history.
            U05ExecutionResult replay = application.execute(inbound, manifest, authority);
            assertEquals(U05ExecutionResult.ROUTING_COMPLETE, replay.getStatus());
            assertEquals(13, repo.findById("cdp-1").get().getVersion().intValue());
            assertEquals(1, ctx.getBean(
                    com.aidoctor.diagnosis.repository.CDPVersionRepository.class)
                    .findByCdpIdOrderByVersionNumberDesc("cdp-1").size());
        }
    }

    @Test
    public void continuousU04F3SafetyA1U05PhysicalMysqlNonproductionFlow() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            Map<String,Object> flags = new HashMap<String,Object>();
            flags.put("aidoctor.u07.clinical-host.nonproduction-enabled", "true");
            flags.put("aidoctor.u07.persistence.readback-enabled", "true");
            ctx.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("u07-full-chain-isolated", flags));
            ctx.register(U07WorkpackACdpPhysicalMysqlTest.IsolatedConfig.class,
                    ScopedTestCommitter.class, ScopedU05TestPorts.class,
                    ClinicalCdpReadbackAdapter.class, U07ClinicalHostServicesConfig.class);
            ctx.refresh();
            CDPRepository repo = ctx.getBean(CDPRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            org.springframework.jdbc.core.JdbcTemplate jdbc =
                    ctx.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
            String id = "cdp-1";
            tx.execute(status -> {
                CDP cdp = CDP.builder().id(id).patientId("synthetic-only")
                        .sessionId("synthetic-only").version(10).build();
                cdp.setPatientState(new LinkedHashMap<String,Object>());
                repo.saveAndFlush(cdp);
                return null;
            });
            jdbc.execute("CREATE TABLE IF NOT EXISTS u07_a1_attestation ("
                    + "consultation_id VARCHAR(64) NOT NULL, cdp_id VARCHAR(64) NOT NULL, "
                    + "cdp_version INT NOT NULL, f3_effect_id VARCHAR(128) NOT NULL, "
                    + "barrier_ref VARCHAR(128) NOT NULL, revalidation_ref VARCHAR(128) NOT NULL, "
                    + "route_source_ref VARCHAR(128) NOT NULL, "
                    + "route_authorization_ref VARCHAR(128) NOT NULL, "
                    + "routing_policy_version VARCHAR(128) NOT NULL, "
                    + "PRIMARY KEY (consultation_id, cdp_id, cdp_version)) ENGINE=InnoDB");

            U03OutboundHandoff handoff = new U03OutboundHandoff(
                    "consult-1", id, 9, 10,
                    "thread-1","run-1","event-1","corr-1","trace-1",ENV,
                    U03NonProductionExecutionContext.BINDING_MODE,
                    U03RiskAssessmentCandidate.VALID, null,
                    Collections.<String>emptyList(), "DETERMINISTIC_FROZEN_RULE_EXECUTION",
                    "u03-decision-1", U03RiskAssessmentCandidate.VALID,
                    "NO_HIGH_RISK_SIGNAL", "U03_RULE_DECISION",
                    U03GovernedCandidateGateway.BINDING_ID, Arrays.asList(
                            U03GovernedCandidateGateway.BINDING_ID,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF),
                    "acceptance-1", Collections.singletonList("evidence-1"),
                    Collections.singletonList("source-1"),
                    Collections.singletonList("provenance-1"),
                    "u03-proposal-1","COMMITTED",null,"audit-u03-1");
            U04ExecutionResult u04 = ctx.getBean(U04NonProductionApplicationService.class)
                    .execute(handoff, 10, U04ScopeContext.current(true, true), ENV);
            assertEquals(U04ExecutionResult.COMMIT_COMPLETE, u04.getStatus());
            assertEquals(11, repo.findById(id).get().getVersion().intValue());
            // A real persisted U04 gate can authorize F3 only at its exact CDP version.
            com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalFollowupCoordinator
                    .requireCurrentU04Version(id, u04, repo);

            StateRepositoryPort state = ctx.getBean(StateRepositoryPort.class);
            com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalF3Owner.F3CommitPort
                    f3Commit = new com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalF3Owner.F3CommitPort() {
                @Override public int currentVersion(String cdp) {
                    return state.readCurrentVersion(cdp);
                }
                @Override public int commitAuthorized(String cdp, int version,
                        com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalF3Owner.PreparedF3 d) {
                    StateRepositoryPort.AtomicCommitOutcome result = state.attemptAtomicCommit(
                            new StateRepositoryPort.AtomicCommitCommand(
                                    cdp, version, d.patchId, d.idempotencyKey, d.patch));
                    if (result.status != StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED)
                        throw new IllegalStateException("U07_F3_TEST_COMMIT_FAILED");
                    return result.committedVersion;
                }
            };
            com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalF3Owner owner =
                    new com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalF3Owner(
                    f3Commit, repo, (consultation, cdp, version) -> {
                        com.aidoctor.contracts.v1.StateTypes.StatePatchOperation op =
                                new com.aidoctor.contracts.v1.StateTypes.StatePatchOperation();
                        op.op = "ADD";
                        op.path = "/patient_state/f3_gap_assessment";
                        Map<String,Object> value = new LinkedHashMap<String,Object>();
                        value.put("f3_canonical_effect_id", "physical-e2e-f3");
                        value.put("gap_id", "gap-e2e");
                        op.value = value;
                        op.source = "PATIENT_FACT";
                        op.sensitivity = "PHI";
                        com.aidoctor.contracts.v1.StateTypes.StatePatch patch =
                                new com.aidoctor.contracts.v1.StateTypes.StatePatch();
                        patch.operations = Collections.singletonList(op);
                        return new com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalF3Owner.PreparedF3(
                                "physical-e2e-f3", "patch-e2e-f3", "idem-e2e-f3", patch);
                    });
            java.util.concurrent.atomic.AtomicBoolean safetyAllowed =
                    new java.util.concurrent.atomic.AtomicBoolean(false);
            com.aidoctor.diagnosis.runtime.u06.U06SyntheticPostF3SafetyBarrier barrier =
                    new com.aidoctor.diagnosis.runtime.u06.U06SyntheticPostF3SafetyBarrier();
            com.aidoctor.diagnosis.runtime.foundation.adapters.U07DurableNonProductionA1Issuer.VerifiedPostF3EvidencePort
                    verifier = (consultation, cdp, version, effect) -> {
                        com.aidoctor.diagnosis.runtime.u06.U06SyntheticPostF3SafetyBarrier.Evaluation evaluated =
                                barrier.evaluate(consultation, effect, "COMMITTED", version,
                                        new com.aidoctor.diagnosis.runtime.u06.U06SyntheticPostF3SafetyBarrier.Evidence(
                                                safetyAllowed.get() ? "ALLOWED" : "BLOCKED",
                                                "isolated-independent-safety"));
                        if (!"ALLOWED".equals(evaluated.getStatus()))
                            throw new IllegalStateException("U07_E2E_POST_F3_SAFETY_BLOCKED");
                        // The nonproduction revalidation input is derived from current JPA
                        // readback, not from an inbound request assertion.
                        CDP latest = repo.findById(cdp).get();
                        if (latest.getVersion() == null || latest.getVersion() != version
                                || !effect.equals(((Map<?, ?>) latest.getPatientState()
                                .get("f3_gap_assessment")).get("f3_canonical_effect_id")))
                            throw new IllegalStateException("U07_E2E_REVALIDATION_STALE");
                        return new com.aidoctor.diagnosis.runtime.foundation.adapters.U07DurableNonProductionA1Issuer.VerifiedEvidence(
                                effect, evaluated.getEvaluationId(), "isolated-current-revalidation",
                                "route-source-" + U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                                "route-auth-" + U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                                "routing-policy-v1", true, true, true);
                    };
            com.aidoctor.diagnosis.runtime.foundation.adapters.U07DurableNonProductionA1Issuer issuer =
                    new com.aidoctor.diagnosis.runtime.foundation.adapters.U07DurableNonProductionA1Issuer(
                            repo, jdbc, tx, verifier);
            U04A1U05AuthorityBridge bridge = new U04A1U05AuthorityBridge(
                    new com.aidoctor.diagnosis.runtime.foundation.adapters.ClinicalCdpGateReadbackAdapter(repo),
                    new com.aidoctor.diagnosis.runtime.foundation.adapters.ClinicalCdpF3BoundA1AuthorityPort(repo, issuer));
            com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionF3A1U05Flow flow =
                    new com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionF3A1U05Flow(
                    owner, issuer, bridge,
                    new com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionF3A1U05Flow.U05InputProvider() {
                        @Override public U05ReadinessInputManifest manifest(U05AdmissionAuthoritySnapshot authority) {
                            return U07ClinicalHostServiceMysqlTest.manifest(
                                    U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION,
                                    Arrays.asList(
                                            present(U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION),
                                            present(U05ReadinessInput.F3, U05ReadinessInput.CAN_ASK_MORE, VERSION),
                                            applicability(U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION),
                                            applicability(U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION)));
                        }
                        @Override public U05ConsumerInboundRequest request(U05AdmissionAuthoritySnapshot authority) {
                            U05ReadinessInputManifest m = manifest(authority);
                            return new U05ConsumerInboundRequest(
                                    "e2e-request", "consult-1", id, VERSION,
                                    authority.getAuthoritativeStateRef(),
                                    U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                                    U05ConsumerInboundRequest.U04_A1_POST_BARRIER_ROUTING,
                                    authority.getRouteSourceRef(), U05ConsumerInboundRequest.U05_ELIGIBLE,
                                    authority.getGateRef(), authority.getGateCommitRef(),
                                    authority.getGateValue(), authority.getRouteAuthorizationType(),
                                    authority.getRouteAuthorizationRef(), authority.getRoutingPolicyVersion(),
                                    "A1", null, null, m.getManifestRef(), m.getSetIdentity(),
                                    m.authoritativeRecordRefs(), "canonical-event-1", "business-event-1",
                                    "corr-1", "trace-1", U05AdmissionService.CONTRACT_VERSION,
                                    ENV, "2026-09-21T08:00:00Z");
                        }
                    }, ctx.getBean(U05NonProductionApplicationService.class));

            // Denied safety stops before U05; F3 itself remains committed.
            assertThrows(IllegalStateException.class,
                    () -> flow.execute(handoff, u04, ENV, "clinical-state-cdp-1-v12"));
            assertEquals(12, repo.findById(id).get().getVersion().intValue());
            // The formerly current U04 receipt is now stale after the real F3
            // P01 commit: recovery may use fresh independent authority, never
            // replay the old safety gate to start another F3 execution.
            assertTrue(assertThrows(IllegalStateException.class, () ->
                    com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalFollowupCoordinator
                            .requireCurrentU04Version(id, u04, repo))
                    .getMessage().contains("U04_SAFETY_GATE_STALE_BEFORE_F3"));
            assertEquals(0, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_a1_attestation WHERE cdp_id=?",
                    Integer.class, id).intValue());
            // The F3 decision may be retried only by explicit recovery orchestration.
            // Recreate the flow with a readback-only F3 owner for this test's
            // authorized continuation (no duplicate state mutation).
            safetyAllowed.set(true);
            com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionF3A1U05Flow resumed =
                    new com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionF3A1U05Flow(
                    (consultation, cdp) -> {
                        CDP persisted = repo.findById(cdp)
                                .orElseThrow(() -> new IllegalStateException("U07_E2E_F3_READBACK_MISSING"));
                        Object assessment = persisted.getPatientState().get("f3_gap_assessment");
                        if (persisted.getVersion() == null
                                || persisted.getVersion() != 12
                                || !(assessment instanceof Map<?, ?>)
                                || !"physical-e2e-f3".equals(
                                        ((Map<?, ?>) assessment).get("f3_canonical_effect_id")))
                            throw new IllegalStateException("U07_E2E_F3_READBACK_STALE");
                        return new com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionF3A1U05Flow.F3Committed(
                                consultation, cdp, "physical-e2e-f3",
                                persisted.getVersion(), true);
                    },
                    issuer, bridge, new com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionF3A1U05Flow.U05InputProvider() {
                        @Override public U05ReadinessInputManifest manifest(U05AdmissionAuthoritySnapshot a) {
                            return U07ClinicalHostServiceMysqlTest.manifest(
                                    U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION,
                                    Arrays.asList(
                                            present(U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION),
                                            present(U05ReadinessInput.F3, U05ReadinessInput.CAN_ASK_MORE, VERSION),
                                            applicability(U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION),
                                            applicability(U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION)));
                        }
                        @Override public U05ConsumerInboundRequest request(U05AdmissionAuthoritySnapshot a) {
                            U05ReadinessInputManifest m = manifest(a);
                            return new U05ConsumerInboundRequest(
                                    "e2e-request", "consult-1", id, VERSION, a.getAuthoritativeStateRef(),
                                    U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                                    U05ConsumerInboundRequest.U04_A1_POST_BARRIER_ROUTING,
                                    a.getRouteSourceRef(), U05ConsumerInboundRequest.U05_ELIGIBLE,
                                    a.getGateRef(), a.getGateCommitRef(), a.getGateValue(),
                                    a.getRouteAuthorizationType(), a.getRouteAuthorizationRef(),
                                    a.getRoutingPolicyVersion(), "A1", null, null,
                                    m.getManifestRef(), m.getSetIdentity(), m.authoritativeRecordRefs(),
                                    "canonical-event-1", "business-event-1", "corr-1", "trace-1",
                                    U05AdmissionService.CONTRACT_VERSION, ENV, "2026-09-21T08:00:00Z");
                        }
                    }, ctx.getBean(U05NonProductionApplicationService.class));
            U05ExecutionResult result = resumed.execute(handoff, u04, ENV, "clinical-state-cdp-1-v12");
            assertEquals(U05ExecutionResult.ROUTING_COMPLETE, result.getStatus());
            assertEquals("COMMITTED", result.getCommitResult().status);
            assertEquals(13, repo.findById(id).get().getVersion().intValue());
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_a1_attestation WHERE cdp_id=?",
                    Integer.class, id).intValue());
            assertEquals(3, ctx.getBean(com.aidoctor.diagnosis.repository.CDPVersionRepository.class)
                    .findByCdpIdOrderByVersionNumberDesc(id).size());
            assertThrows(IllegalStateException.class,
                    () -> issuer.loadAttested("consult-1", id, 12));
            // This legacy test suite uses a fixed CDP id in another test.
            // Remove only this disposable E2E fixture so execution order cannot
            // inflate the other test's history assertions.
            tx.execute(status -> {
                com.aidoctor.diagnosis.repository.CDPVersionRepository histories =
                        ctx.getBean(com.aidoctor.diagnosis.repository.CDPVersionRepository.class);
                histories.deleteAll(histories.findByCdpIdOrderByVersionNumberDesc(id));
                repo.deleteById(id);
                return null;
            });
            jdbc.update("DELETE FROM u07_a1_attestation WHERE cdp_id=?", id);
        }
    }

    @Test
    public void unifiedU06Mode1UsesGovernedCommitAndPhysicalMysqlReadback() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(U07WorkpackACdpPhysicalMysqlTest.IsolatedConfig.class,
                    ScopedTestCommitter.class);
            ctx.refresh();
            CDPRepository repo = ctx.getBean(CDPRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            String id = "u06-unified-" + UUID.randomUUID().toString();
            tx.execute(status -> {
                CDP cdp = CDP.builder().id(id).patientId("synthetic-only")
                        .sessionId("synthetic-only").version(8).build();
                Map<String,Object> initial = new LinkedHashMap<String,Object>();
                initial.put("information_gaps", new LinkedHashMap<String,Object>());
                cdp.setPatientState(initial);
                repo.saveAndFlush(cdp);
                return null;
            });

            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest request =
                    new com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest(
                            "u06-unified-req", "consult-unified", id,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.A1_PRE_READINESS_ROUTING,
                            "source-u04", 8, 8,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING,
                            "synthetic-dependency", "f3-policy", null, null,
                            "canonical-event", "business-event", null, null, 0L,
                            "correlation", "trace", "2026-09-18T00:00:00Z",
                            com.aidoctor.diagnosis.runtime.u06.U06AdmissionEvidence.syntheticCurrentAllow());
            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle decision =
                    new com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle(
                            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED,
                            "unified-effect", "unified-gap", "DECISION_MATERIAL", true,
                            null, null, null, null, null, null, null, null, null);
            com.aidoctor.diagnosis.runtime.u06.U06ProfileBApplicationService service =
                    new com.aidoctor.diagnosis.runtime.u06.U06ProfileBApplicationService(
                            new com.aidoctor.diagnosis.runtime.u06.U06NonProductionPhysicalMode1Service(
                                    repo, ctx.getBean(StateCommitter.class),
                                    new com.aidoctor.diagnosis.runtime.u06.U06AdmissionService(),
                                    new com.aidoctor.diagnosis.runtime.u06.U06SyntheticPostF3SafetyBarrier()));
            com.aidoctor.diagnosis.runtime.u06.U06ExecutionResult denied = service.execute(
                    request, decision, null,
                    new com.aidoctor.diagnosis.runtime.u06.U06SyntheticPostF3SafetyBarrier.Evidence(
                            "BLOCKED", "nonprod-evidence"));
            assertEquals(com.aidoctor.diagnosis.runtime.u06.U06ExecutionResult.MODE1_SAFETY_BLOCKED,
                    denied.getStatus());
            assertEquals("COMMITTED", denied.getCommitStatus());
            assertEquals(9, repo.findById(id).get().getVersion().intValue());
            Map<String,Object> stored = repo.findById(id).get().getPatientState();
            assertEquals("unified-effect",
                    ((Map<?,?>) stored.get("f3_gap_assessment")).get("f3_canonical_effect_id"));
            assertEquals("QUESTIONABLE_ONLINE",
                    ((Map<?,?>) ((Map<?,?>) stored.get("information_gaps"))
                            .get("unified-gap")).get("status"));
            assertEquals(1, ctx.getBean(com.aidoctor.diagnosis.repository.CDPVersionRepository.class)
                    .findByCdpIdOrderByVersionNumberDesc(id).size());
            assertEquals(
                    com.aidoctor.diagnosis.runtime.u06.U06ExecutionResult.ADMISSION_REJECTED,
                    service.execute(request, decision, null,
                            new com.aidoctor.diagnosis.runtime.u06.U06SyntheticPostF3SafetyBarrier.Evidence(
                                    "ALLOWED", "nonprod-evidence")).getStatus());
            assertEquals(9, repo.findById(id).get().getVersion().intValue());

            String allowedId = "u06-unified-allowed-" + UUID.randomUUID().toString();
            tx.execute(status -> {
                CDP cdp = CDP.builder().id(allowedId).patientId("synthetic-only")
                        .sessionId("synthetic-only").version(8).build();
                Map<String,Object> initial = new LinkedHashMap<String,Object>();
                initial.put("information_gaps", new LinkedHashMap<String,Object>());
                cdp.setPatientState(initial);
                repo.saveAndFlush(cdp);
                return null;
            });
            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest allowRequest =
                    new com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest(
                            "u06-unified-allowed", "consult-unified", allowedId,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.A1_PRE_READINESS_ROUTING,
                            "source-u04", 8, 8,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING,
                            "synthetic-dependency", "f3-policy", null, null,
                            "canonical-event-allow", "business-event-allow", null, null, 0L,
                            "correlation", "trace", "2026-09-18T00:00:00Z",
                            com.aidoctor.diagnosis.runtime.u06.U06AdmissionEvidence.syntheticCurrentAllow());
            com.aidoctor.diagnosis.runtime.u06.U06ExecutionResult allowed = service.execute(
                    allowRequest, decision, null,
                    new com.aidoctor.diagnosis.runtime.u06.U06SyntheticPostF3SafetyBarrier.Evidence(
                            "ALLOWED", "nonprod-allowed-evidence"));
            assertEquals(com.aidoctor.diagnosis.runtime.u06.U06ExecutionResult.MODE1_COMMITTED,
                    allowed.getStatus());
            assertEquals("COMMITTED", allowed.getCommitStatus());
            assertEquals(9, repo.findById(allowedId).get().getVersion().intValue());
            assertEquals(1, ctx.getBean(com.aidoctor.diagnosis.repository.CDPVersionRepository.class)
                    .findByCdpIdOrderByVersionNumberDesc(allowedId).size());
        }
    }

    @Test
    public void unifiedPhysicalMode2SelectsDurablyWithoutDeliveryAndMode3ReadsCurrentF3() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(U07WorkpackACdpPhysicalMysqlTest.IsolatedConfig.class,
                    ScopedTestCommitter.class);
            ctx.refresh();
            CDPRepository repo = ctx.getBean(CDPRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            String id = "u06-mode23-" + UUID.randomUUID().toString();
            tx.execute(status -> {
                CDP cdp = CDP.builder().id(id).patientId("synthetic-only")
                        .sessionId("synthetic-only").version(8).build();
                Map<String,Object> patient = new LinkedHashMap<String,Object>();
                patient.put("questions", new LinkedHashMap<String,Object>());
                Map<String,Object> f3 = new LinkedHashMap<String,Object>();
                f3.put("f3_canonical_effect_id", "f3-current-effect");
                patient.put("f3_gap_assessment", f3);
                cdp.setPatientState(patient);
                repo.saveAndFlush(cdp);
                return null;
            });
            com.aidoctor.diagnosis.runtime.u06.U06AdmissionService admission =
                    new com.aidoctor.diagnosis.runtime.u06.U06AdmissionService();
            com.aidoctor.diagnosis.runtime.u06.U06ProfileBApplicationService unified =
                    new com.aidoctor.diagnosis.runtime.u06.U06ProfileBApplicationService(
                            new com.aidoctor.diagnosis.runtime.u06.U06NonProductionPhysicalMode23Service(
                                    new com.aidoctor.diagnosis.runtime.u06.U06NonProductionPhysicalMode1Service(
                                            repo, ctx.getBean(StateCommitter.class), admission,
                                            new com.aidoctor.diagnosis.runtime.u06.U06SyntheticPostF3SafetyBarrier()),
                                    repo, ctx.getBean(StateCommitter.class), admission));
            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest mode2 =
                    new com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest(
                            "mode2-physical", "consult-mode23", id,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.U05_QUESTION_ROUTING,
                            "source-u05", 8, 8,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING,
                            "synthetic-dependency", "f3-policy", "question-policy", "d04-policy",
                            "canonical-mode2", "business-mode2", "thread-1", "run-1", 0L,
                            "correlation", "trace", "2026-09-18T00:00:00Z",
                            com.aidoctor.diagnosis.runtime.u06.U06AdmissionEvidence.syntheticCurrentAllow());
            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle selected =
                    new com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle(
                            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle.NOT_DECIDABLE,
                            "f3-current-effect", null, null, false,
                            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle.CONTINUE,
                            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle.SELECTED,
                            "question-effect-1", "question-1", "semantic-question-1",
                            "content-ref-1", "content-fingerprint-1", null, null);
            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle forgedSelection =
                    new com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle(
                            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle.NOT_DECIDABLE,
                            "forged-f3-effect", null, null, false,
                            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle.CONTINUE,
                            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle.SELECTED,
                            "question-effect-1", "question-1", "semantic-question-1",
                            "content-ref-1", "content-fingerprint-1", null, null);
            assertThrows(IllegalStateException.class,
                    () -> unified.execute(mode2, forgedSelection, null));
            assertEquals(8, repo.findById(id).get().getVersion().intValue());
            com.aidoctor.diagnosis.runtime.u06.U06ExecutionResult selection =
                    unified.execute(mode2, selected, null);
            assertEquals(com.aidoctor.diagnosis.runtime.u06.U06ExecutionResult.MODE2_SELECTION_PERSISTED,
                    selection.getStatus());
            assertEquals("COMMITTED", selection.getCommitStatus());
            assertNull(selection.getDeliveryId());
            assertNull(selection.getWaitEffectId());
            assertNull(selection.getCheckpointId());
            CDP persisted = repo.findById(id).get();
            assertEquals(9, persisted.getVersion().intValue());
            assertFalse(persisted.getPatientState().containsKey("pending_question"));
            Map<?,?> selectedQuestion = (Map<?,?>) ((Map<?,?>)
                    persisted.getPatientState().get("questions")).get("question-1");
            assertEquals("SELECTED", selectedQuestion.get("status"));
            assertEquals("question-effect-1", selectedQuestion.get("selection_effect_id"));
            assertEquals(1, ctx.getBean(com.aidoctor.diagnosis.repository.CDPVersionRepository.class)
                    .findByCdpIdOrderByVersionNumberDesc(id).size());
            assertEquals(com.aidoctor.diagnosis.runtime.u06.U06ExecutionResult.ADMISSION_REJECTED,
                    unified.execute(mode2, selected, null).getStatus());
            assertEquals(9, repo.findById(id).get().getVersion().intValue());

            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest mode3 =
                    new com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest(
                            "mode3-physical", "consult-mode23", id,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.F3_CURRENT_VERSION_REVALIDATION,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.POST_F3_SAFETY_BARRIER_ROUTING,
                            "source-post-f3", 9, 9,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING,
                            "synthetic-dependency", "f3-policy", null, null,
                            "canonical-mode3", "business-mode3", null, null, 0L,
                            "correlation", "trace", "2026-09-18T00:00:00Z",
                            com.aidoctor.diagnosis.runtime.u06.U06AdmissionEvidence.syntheticCurrentAllow());
            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle current =
                    new com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle(
                            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle.NOT_DECIDABLE,
                            "f3-current-effect", null, null, false,
                            null, null, null, null, null, null, null,
                            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle.REVALIDATED_CURRENT,
                            "mode3-current-ref");
            assertEquals(com.aidoctor.diagnosis.runtime.u06.U06ExecutionResult.REVALIDATED_CURRENT,
                    unified.execute(mode3, current, null).getStatus());
            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle forged =
                    new com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle(
                            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle.NOT_DECIDABLE,
                            "forged-f3-effect", null, null, false,
                            null, null, null, null, null, null, null,
                            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle.REVALIDATED_CURRENT,
                            "mode3-forged-ref");
            assertEquals(com.aidoctor.diagnosis.runtime.u06.U06ExecutionResult.REASSESSMENT_REQUIRED,
                    unified.execute(mode3, forged, null).getStatus());
            assertEquals(9, repo.findById(id).get().getVersion().intValue());
        }
    }

    @Test
    public void durableDeliveryReceiptCanRecoverCdpAndWaitAfterCrashWithoutResending() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(U07WorkpackACdpPhysicalMysqlTest.IsolatedConfig.class,
                    ScopedTestCommitter.class);
            ctx.refresh();
            CDPRepository repo=ctx.getBean(CDPRepository.class);
            TransactionTemplate tx=ctx.getBean(TransactionTemplate.class);
            org.springframework.jdbc.core.JdbcTemplate jdbc=
                    ctx.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
            jdbc.execute("CREATE TABLE IF NOT EXISTS u07_question_delivery_evidence ("
                    + "consultation_id VARCHAR(64) NOT NULL, cdp_id VARCHAR(64) NOT NULL, "
                    + "question_id VARCHAR(64) NOT NULL, selection_effect_id VARCHAR(128) NOT NULL, "
                    + "delivery_id VARCHAR(128) NOT NULL, delivery_effect_id VARCHAR(128) NOT NULL, "
                    + "confirmation_ref VARCHAR(128) NOT NULL, "
                    + "PRIMARY KEY (consultation_id, cdp_id, question_id)) ENGINE=InnoDB");
            jdbc.execute("CREATE TABLE IF NOT EXISTS u07_question_wait_evidence ("
                    + "consultation_id VARCHAR(64) NOT NULL, cdp_id VARCHAR(64) NOT NULL, "
                    + "question_id VARCHAR(64) NOT NULL, wait_effect_ref VARCHAR(128) NOT NULL, "
                    + "checkpoint_ref VARCHAR(128) NOT NULL, eligibility_ref VARCHAR(128) NOT NULL, "
                    + "PRIMARY KEY (consultation_id, cdp_id, question_id)) ENGINE=InnoDB");
            String id="u07-delivery-"+UUID.randomUUID().toString();
            tx.execute(status -> {
                CDP cdp=CDP.builder().id(id).patientId("synthetic-only")
                        .sessionId("synthetic-only").version(8).build();
                Map<String,Object> patient=new LinkedHashMap<String,Object>();
                Map<String,Object> question=new LinkedHashMap<String,Object>();
                question.put("question_id","question-1");
                question.put("selection_effect_id","selection-effect-1");
                question.put("content_fingerprint","fingerprint-1");
                question.put("status","SELECTED");
                Map<String,Object> questions=new LinkedHashMap<String,Object>();
                questions.put("question-1",question);
                patient.put("questions",questions);
                Map<String,Object> f3=new LinkedHashMap<String,Object>();
                f3.put("f3_canonical_effect_id","f3-current-1");
                patient.put("f3_gap_assessment",f3);
                cdp.setPatientState(patient);
                repo.saveAndFlush(cdp);
                return null;
            });
            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest request=
                    new com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest(
                            "delivery-request","consult-delivery",id,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.U05_QUESTION_ROUTING,
                            "source-u05",8,8,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                            com.aidoctor.diagnosis.runtime.u06.U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING,
                            "synthetic-dependency","f3-policy","question-policy","d04-policy",
                            "canonical-delivery","business-delivery","thread-1","run-1",0L,
                            "correlation","trace","2026-09-18T00:00:00Z",
                            com.aidoctor.diagnosis.runtime.u06.U06AdmissionEvidence.syntheticCurrentAllow());
            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle decision=
                    new com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle(
                            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle.NOT_DECIDABLE,
                            "f3-current-1",null,null,false,
                            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle.CONTINUE,
                            com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle.SELECTED,
                            "selection-effect-1","question-1","semantic-1",
                            "content-ref-1","fingerprint-1",null,null);
            com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery.Receipt receipt=
                    new com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery.Receipt(
                            request,decision,"delivery-1","delivery-effect-1","confirmed-1");
            java.util.concurrent.atomic.AtomicBoolean trusted=
                    new java.util.concurrent.atomic.AtomicBoolean(false);
            java.util.concurrent.atomic.AtomicInteger waitCalls=
                    new java.util.concurrent.atomic.AtomicInteger(0);
            com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery.WaitAuthority waits=
                    (r,version) -> {
                        assertEquals(9,version);
                        if(waitCalls.incrementAndGet()==1)
                            throw new IllegalStateException("SIMULATED_WAIT_CRASH");
                        return new com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery.WaitReceipt(
                                "consult-wait-1","checkpoint-1","eligibility-1");
                    };
            com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery recovery=
                    new com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery(
                            repo,jdbc,tx,ctx.getBean(StateCommitter.class),
                            r -> trusted.get(),waits);
            assertThrows(IllegalStateException.class,()->recovery.accept(receipt));
            assertEquals(0,jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_question_delivery_evidence WHERE cdp_id=?",
                    Integer.class,id).intValue());
            trusted.set(true);
            recovery.accept(receipt);
            assertEquals(8,repo.findById(id).get().getVersion().intValue());
            assertThrows(IllegalStateException.class,()->recovery.recover(receipt));
            assertEquals(9,repo.findById(id).get().getVersion().intValue());
            assertEquals(0,jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_question_wait_evidence WHERE cdp_id=?",
                    Integer.class,id).intValue());
            com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery restarted=
                    new com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery(
                            repo,jdbc,tx,ctx.getBean(StateCommitter.class),
                            r -> trusted.get(),waits);
            assertEquals("checkpoint-1",restarted.recover(receipt).checkpointId);
            assertEquals("checkpoint-1",restarted.recover(receipt).checkpointId);
            assertEquals(2,waitCalls.get());
            restarted.accept(receipt); // duplicate inbound confirmation after completed wait
            assertEquals(1,jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_question_delivery_evidence WHERE cdp_id=?",
                    Integer.class,id).intValue());
            assertEquals(9,repo.findById(id).get().getVersion().intValue());
            assertEquals(1,ctx.getBean(com.aidoctor.diagnosis.repository.CDPVersionRepository.class)
                    .findByCdpIdOrderByVersionNumberDesc(id).size());
            assertEquals(1,jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_question_wait_evidence WHERE cdp_id=?",
                    Integer.class,id).intValue());
            Map<String,Object> state=repo.findById(id).get().getPatientState();
            assertEquals("DELIVERED_TO_USER",
                    ((Map<?,?>)((Map<?,?>)state.get("questions")).get("question-1")).get("status"));
            assertEquals("delivery-1",
                    ((Map<?,?>)state.get("pending_question")).get("delivery_id"));
        }
    }

    @Test
    public void u07DurableHostIsDefaultOffAndProductionProfileNeverWiresAuthority() {
        try (AnnotationConfigApplicationContext disabled =
                     new AnnotationConfigApplicationContext()) {
            disabled.register(
                    com.aidoctor.diagnosis.runtime.foundation.adapters.U07DurableClinicalHostIntegrationConfig.class);
            disabled.refresh();
            assertTrue(disabled.getBeansOfType(
                    com.aidoctor.diagnosis.runtime.foundation.adapters.U07TrustedF3RevalidationWriter.class).isEmpty());
            assertTrue(disabled.getBeansOfType(
                    com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery.class).isEmpty());
        }
        try (AnnotationConfigApplicationContext missingAuthority =
                     new AnnotationConfigApplicationContext()) {
            missingAuthority.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("nonprod-u07-host", Collections.<String,Object>singletonMap(
                            "aidoctor.u07.host-integration.nonproduction-enabled", "true")));
            missingAuthority.register(
                    com.aidoctor.diagnosis.runtime.foundation.adapters.U07DurableClinicalHostIntegrationConfig.class);
            missingAuthority.refresh();
            assertTrue(missingAuthority.getBeansOfType(
                    com.aidoctor.diagnosis.runtime.foundation.adapters.U07TrustedF3RevalidationWriter.class).isEmpty());
            assertTrue(missingAuthority.getBeansOfType(
                    com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery.class).isEmpty());
        }
        try (AnnotationConfigApplicationContext production =
                     new AnnotationConfigApplicationContext()) {
            production.getEnvironment().setActiveProfiles("prod");
            production.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("prod-u07-host", Collections.<String,Object>singletonMap(
                            "aidoctor.u07.host-integration.nonproduction-enabled", "true")));
            production.register(
                    com.aidoctor.diagnosis.runtime.foundation.adapters.U07DurableClinicalHostIntegrationConfig.class);
            production.refresh();
            assertTrue(production.getBeansOfType(
                    com.aidoctor.diagnosis.runtime.foundation.adapters.U07TrustedF3RevalidationWriter.class).isEmpty());
            assertTrue(production.getBeansOfType(
                    com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery.class).isEmpty());
        }
    }

    @Test
    public void disabledByDefaultProvidesNoClinicalHostServices() {
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(U07ClinicalHostServicesConfig.class)) {
            assertTrue(ctx.getBeansOfType(U04NonProductionApplicationService.class).isEmpty());
        }
    }

    @Test
    public void actualU04ServiceRunsOnSpringHostAndPersistsGateWithRealCdp() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "u07-host-test-only", Collections.<String,Object>singletonMap(
                            "aidoctor.u07.clinical-host.nonproduction-enabled", "true")));
            ctx.register(U07WorkpackACdpPhysicalMysqlTest.IsolatedConfig.class,
                    ScopedTestCommitter.class, U07ClinicalHostServicesConfig.class);
            ctx.refresh();

            CDPRepository repo = ctx.getBean(CDPRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            String id = "u07-host-" + UUID.randomUUID().toString();
            tx.execute(status -> {
                CDP cdp = CDP.builder().id(id).patientId("synthetic-only")
                        .sessionId("synthetic-only").version(8).build();
                cdp.setPatientState(new LinkedHashMap<String,Object>());
                repo.saveAndFlush(cdp);
                return null;
            });

            U04NonProductionApplicationService u04 =
                    ctx.getBean(U04NonProductionApplicationService.class);
            U03OutboundHandoff handoff = new U03OutboundHandoff(
                    "consult-1", id, 7, 8,
                    "thread-1","run-1","event-1","corr-1","trace-1",ENV,
                    U03NonProductionExecutionContext.BINDING_MODE,
                    U03RiskAssessmentCandidate.VALID, null,
                    Collections.<String>emptyList(), "DETERMINISTIC_FROZEN_RULE_EXECUTION",
                    "u03-decision-1", U03RiskAssessmentCandidate.VALID,
                    "NO_HIGH_RISK_SIGNAL", "U03_RULE_DECISION",
                    U03GovernedCandidateGateway.BINDING_ID, Arrays.asList(
                            U03GovernedCandidateGateway.BINDING_ID,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF),
                    "acceptance-1", Collections.singletonList("evidence-1"),
                    Collections.singletonList("source-1"),
                    Collections.singletonList("provenance-1"),
                    "u03-proposal-1","COMMITTED",null,"audit-u03-1");

            U04ExecutionResult result =
                    u04.execute(handoff, 8, U04ScopeContext.current(true,true), ENV);
            assertEquals(U04ExecutionResult.COMMIT_COMPLETE, result.getStatus());
            assertEquals(9, repo.findById(id).get().getVersion().intValue());
            Map<String,Object> state = repo.findById(id).get().getPatientState();
            assertTrue(state.get("safety_gate") instanceof Map<?,?>);
            Map<?,?> gate = (Map<?,?>) state.get("safety_gate");
            assertEquals("ALLOW", gate.get("safety_gate"));
            assertEquals("u03-decision-1", gate.get("source_u03_decision_ref"));

            U04ExecutionResult replayStale =
                    u04.execute(handoff, 8, U04ScopeContext.current(true,true), ENV);
            // A replay of the identical governed effect returns the recorded
            // COMMITTED result; the physical CDP must not advance twice.
            assertEquals(U04ExecutionResult.COMMIT_COMPLETE, replayStale.getStatus());
            assertEquals(9, repo.findById(id).get().getVersion().intValue());
            assertEquals(1, ctx.getBean(
                    com.aidoctor.diagnosis.repository.CDPVersionRepository.class)
                    .findByCdpIdOrderByVersionNumberDesc(id).size());
        }
    }
    private static U05ReadinessInputManifest manifest(
            String context,
            int version,
            List<U05ReadinessInput> inputs) {
        String setIdentity = U05ReadinessInputManifest.semanticSetIdentity(
                "consult-1",
                "cdp-1",
                version,
                context,
                U05ReadinessInputManifest.RDP05_CONTRACT_VERSION,
                inputs);
        return new U05ReadinessInputManifest(
                "manifest-" + context,
                setIdentity,
                "consult-1",
                "cdp-1",
                version,
                context,
                U05ReadinessInputManifest.RDP05_CONTRACT_VERSION,
                inputs);
    }


    private static U05ReadinessInput present(String domain, String signal, int version) {
        return new U05ReadinessInput(
                "input-" + domain + "-" + signal,
                domain,
                "owner-" + domain,
                inputKind(domain),
                U05ReadinessInput.PRESENT,
                signal,
                "consult-1",
                "cdp-1",
                version,
                "decision-" + domain + "-" + signal,
                "state-" + domain,
                "app-evidence-" + domain,
                Collections.singletonList("evidence-" + domain),
                Collections.singletonList("rule-" + domain),
                "2026-09-21T08:00:00Z",
                U05ReadinessInput.CURRENT,
                null);
    }

    private static U05ReadinessInput applicability(String domain, String status, int version) {
        return new U05ReadinessInput(
                "app-" + domain + "-" + status,
                domain,
                "owner-" + domain,
                inputKind(domain),
                status,
                null,
                "consult-1",
                "cdp-1",
                version,
                "app-decision-" + domain + "-" + status,
                "state-" + domain,
                "app-evidence-" + domain + "-" + status,
                Collections.singletonList("evidence-" + domain),
                Collections.singletonList("rule-" + domain),
                "2026-09-21T08:00:00Z",
                U05ReadinessInput.CURRENT,
                U05ReadinessInput.STALE.equals(status) ? "invalidation-" + domain : null);
    }

    private static String inputKind(String domain) {
        if (U05ReadinessInput.F1.equals(domain)) return "SCOPE_OR_FRAMING";
        if (U05ReadinessInput.F2_CLARIFICATION.equals(domain)) return "CLARIFICATION_REQUIREMENT";
        if (U05ReadinessInput.F3.equals(domain)) return "ONLINE_INFORMATION_GAP";
        if (U05ReadinessInput.F5.equals(domain)) return "DDX_OR_MUST_EXCLUDE";
        return "OFFLINE_EVIDENCE";
    }

    private static U05ConsumerInboundRequest request(
            U05ReadinessInputManifest manifest,
            String context,
            String gate,
            String restrictedContext,
            String restrictedPermission) {
        return request(manifest, context, gate, restrictedContext, restrictedPermission, ENV);
    }

    private static U05ConsumerInboundRequest request(
            U05ReadinessInputManifest manifest,
            String context,
            String gate,
            String restrictedContext,
            String restrictedPermission,
            String environment) {
        String routeSource = routeSource(context);
        String routeConsequence;
        if (U05ConsumerInboundRequest.CLINICAL_CONTINUATION_ROUTING.equals(routeSource)) {
            routeConsequence = U05ConsumerInboundRequest.TO_U05_CLINICAL_READINESS;
        } else if (U05ConsumerInboundRequest.U04_A1_POST_BARRIER_ROUTING.equals(routeSource)) {
            routeConsequence = U05ConsumerInboundRequest.U05_ELIGIBLE;
        } else {
            routeConsequence = null;
        }
        return new U05ConsumerInboundRequest(
                "request-1",
                "consult-1",
                "cdp-1",
                VERSION,
                "clinical-state-cdp-1-v12",
                context,
                routeSource,
                "route-source-" + context,
                routeConsequence,
                "u04-gate-1",
                "u04-gate-commit-1",
                gate,
                routeAuthType(context),
                "route-auth-" + context,
                "routing-policy-v1",
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context) ? "A1" : null,
                restrictedContext,
                restrictedPermission,
                manifest.getManifestRef(),
                manifest.getSetIdentity(),
                manifest.authoritativeRecordRefs(),
                "canonical-event-1",
                "business-event-1",
                "corr-1",
                "trace-1",
                U05AdmissionService.CONTRACT_VERSION,
                environment,
                "2026-09-21T08:00:00Z");
    }

    private static U05AdmissionAuthoritySnapshot authority(
            String context,
            String gate,
            String restrictedContext,
            String restrictedPermission,
            boolean restrictedPermitted) {
        return authority(context, gate, restrictedContext, restrictedPermission, restrictedPermitted, ENV, true);
    }

    private static U05AdmissionAuthoritySnapshot authority(
            String context,
            String gate,
            String restrictedContext,
            String restrictedPermission,
            boolean restrictedPermitted,
            String environment,
            boolean environmentAuthorized) {
        String routeSource = routeSource(context);
        String consequence;
        if (U05ConsumerInboundRequest.CLINICAL_CONTINUATION_ROUTING.equals(routeSource)) {
            consequence = U05ConsumerInboundRequest.TO_U05_CLINICAL_READINESS;
        } else if (U05ConsumerInboundRequest.U04_A1_POST_BARRIER_ROUTING.equals(routeSource)) {
            consequence = U05ConsumerInboundRequest.U05_ELIGIBLE;
        } else {
            consequence = null;
        }
        boolean a1 = U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context);
        return new U05AdmissionAuthoritySnapshot(
                "consult-1",
                "cdp-1",
                VERSION,
                "clinical-state-cdp-1-v12",
                environment,
                environmentAuthorized,
                true,
                true,
                "u04-gate-1",
                "u04-gate-commit-1",
                gate,
                true,
                "route-source-" + context,
                routeAuthType(context),
                "route-auth-" + context,
                consequence,
                "routing-policy-v1",
                restrictedContext,
                restrictedPermission,
                restrictedPermitted,
                a1 ? "A1" : null,
                a1,
                a1,
                a1,
                false,
                null);
    }

    private static String routeSource(String context) {
        if (U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context)) {
            return U05ConsumerInboundRequest.U04_A1_POST_BARRIER_ROUTING;
        }
        if (U05ConsumerInboundRequest.POST_SAFETY_INITIAL.equals(context)) {
            return U05ConsumerInboundRequest.U04_ORDINARY_ROUTING;
        }
        return U05ConsumerInboundRequest.CLINICAL_CONTINUATION_ROUTING;
    }

    private static String routeAuthType(String context) {
        if (U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context)) {
            return "U04_A1_ROUTING_AUTHORIZATION";
        }
        if (U05ConsumerInboundRequest.POST_SAFETY_INITIAL.equals(context)) {
            return "U04_ORDINARY_ROUTING_AUTHORIZATION";
        }
        return "CLINICAL_CONTINUATION_ROUTING_DECISION";
    }


}
