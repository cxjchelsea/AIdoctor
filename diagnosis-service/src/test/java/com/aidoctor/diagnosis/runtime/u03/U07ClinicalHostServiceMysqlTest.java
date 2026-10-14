package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07ClinicalHostServicesConfig;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07WorkpackACdpPhysicalMysqlTest;
import com.aidoctor.diagnosis.runtime.u04.*;
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
            assertFalse(U04ExecutionResult.COMMIT_COMPLETE.equals(replayStale.getStatus()));
            assertEquals(9, repo.findById(id).get().getVersion().intValue());
            assertEquals(1, ctx.getBean(
                    com.aidoctor.diagnosis.repository.CDPVersionRepository.class)
                    .findByCdpIdOrderByVersionNumberDesc(id).size());
        }
    }
}
