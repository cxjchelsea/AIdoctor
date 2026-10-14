package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.entity.CDPVersion;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.repository.CDPVersionRepository;
import com.aidoctor.diagnosis.service.cdp.CDPVersionService;
import com.aidoctor.diagnosis.util.JsonUtil;
import com.aidoctor.diagnosis.runtime.u04.U04ScopeContext;
import com.aidoctor.diagnosis.runtime.u05.U04A1U05AuthorityBridge;
import org.springframework.core.env.MapPropertySource;
import java.util.Collections;
import com.aidoctor.diagnosis.runtime.u05.U05ClinicalReadinessSnapshotPort;
import com.aidoctor.diagnosis.runtime.u05.U05ClinicalReadinessSnapshot;
import java.util.Collections;
import org.springframework.core.env.MapPropertySource;
import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Spring JPA + MySQL 8 CDP transaction test, disposable CI schema ONLY.
 * Bootstrap DDL is explicitly synthetic, not a Flyway/Oracle deployment claim.
 */
public final class U07WorkpackACdpPhysicalMysqlTest {
    private static final String URL =
            "jdbc:mysql://127.0.0.1:33332/u07_workpack_a_cdp?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static void authorize() {
        if (!"true".equals(System.getenv("GITHUB_ACTIONS"))
                || !"DISPOSABLE_CDP_ONLY".equals(System.getenv("U07_WORKPACK_A_MYSQL_MODE"))
                || !URL.equals(System.getenv("U07_WORKPACK_A_MYSQL_URL")))
            throw new IllegalStateException("U07_WORKPACK_A_CDP_PRODUCTION_FORBIDDEN");
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(
            basePackages = "com.aidoctor.diagnosis.repository",
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {CDPRepository.class, CDPVersionRepository.class}),
            entityManagerFactoryRef = "entityManagerFactory",
            transactionManagerRef = "transactionManager")
    public static class IsolatedConfig {
        @Bean(destroyMethod = "close")
        public HikariDataSource dataSource() {
            authorize();
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(URL);
            cfg.setUsername("root");
            cfg.setPassword("synthetic-workpack-a-root");
            cfg.setMaximumPoolSize(4);
            cfg.setMinimumIdle(0);
            return new HikariDataSource(cfg);
        }

        @Bean(name = "entityManagerFactory")
        public LocalContainerEntityManagerFactoryBean emf(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setPackagesToScan("com.aidoctor.diagnosis.entity");
            factory.setPersistenceUnitPostProcessors((PersistenceUnitPostProcessor) unit -> {
                unit.getManagedClassNames().clear();
                unit.getManagedClassNames().add(CDP.class.getName());
                unit.getManagedClassNames().add(CDPVersion.class.getName());
            });
            java.util.Properties props = new java.util.Properties();
            props.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
            props.put("hibernate.hbm2ddl.auto", "none");
            props.put("hibernate.show_sql", "false");
            factory.setJpaProperties(props);
            return factory;
        }

        @Bean(name = "transactionManager")
        public JpaTransactionManager transactionManager(EntityManagerFactory emf, DataSource ds) {
            JpaTransactionManager tx = new JpaTransactionManager(emf);
            tx.setDataSource(ds);
            return tx;
        }
        @Bean public TransactionTemplate transactions(JpaTransactionManager tx) {
            return new TransactionTemplate(tx);
        }
        @Bean public JdbcTemplate jdbc(DataSource ds) { return new JdbcTemplate(ds); }
        @Bean public JsonUtil jsonUtil() { return new JsonUtil(); }
        @Bean public ObjectMapper mapper() { return new ObjectMapper(); }
        @Bean public CDPVersionService history() { return new CDPVersionService(); }
        @Bean public ClinicalCdpStateRepositoryAdapter adapter(
                CDPRepository cdp, CDPVersionService history, ObjectMapper mapper) {
            return new ClinicalCdpStateRepositoryAdapter(cdp,history,mapper);
        }
    }

    private static StateRepositoryPort.AtomicCommitCommand patch(
            String id, int version, String leaf) {
        StateTypes.StatePatchOperation op = new StateTypes.StatePatchOperation();
        op.op = "ADD";
        op.path = "/patient_state/" + leaf;
        op.value = "synthetic-" + leaf;
        op.source = "PATIENT_FACT";
        op.sensitivity = "PHI";
        StateTypes.StatePatch p = new StateTypes.StatePatch();
        p.operations = Arrays.asList(op);
        return new StateRepositoryPort.AtomicCommitCommand(id, version, "patch-" + leaf,
                "idem-" + leaf, p);
    }

    private static String seed(CDPRepository repo) {
        String id = "u07-a-" + UUID.randomUUID().toString();
        CDP cdp = CDP.builder().id(id).patientId("synthetic-patient")
                .sessionId("synthetic-session").version(8).build();
        cdp.setPatientState(new LinkedHashMap<String,Object>());
        repo.saveAndFlush(cdp);
        return id;
    }

    @Test
    public void actualJpaCdpCommitCasAndHistoryRollback() {
        authorize();
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(IsolatedConfig.class)) {
            CDPRepository repo = ctx.getBean(CDPRepository.class);
            CDPVersionRepository histories = ctx.getBean(CDPVersionRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            StateRepositoryPort adapter =
                    ctx.getBean(StateRepositoryPort.class);
            JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);
            assertEquals("u07_workpack_a_cdp", jdbc.queryForObject("SELECT DATABASE()", String.class));
            String id = tx.execute(status -> seed(repo));

            StateRepositoryPort.AtomicCommitOutcome first = adapter.attemptAtomicCommit(patch(id,8,"gate"));
            assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED, first.status);
            assertEquals(9, repo.findById(id).get().getVersion().intValue());
            assertEquals("synthetic-gate", repo.findById(id).get().getPatientState().get("gate"));
            assertEquals(1, histories.findByCdpIdOrderByVersionNumberDesc(id).size());

            StateRepositoryPort.AtomicCommitOutcome stale =
                    adapter.attemptAtomicCommit(patch(id,8,"stale"));
            assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.CONFLICT, stale.status);
            assertEquals(1, histories.findByCdpIdOrderByVersionNumberDesc(id).size());
            assertFalse(repo.findById(id).get().getPatientState().containsKey("stale"));

            IllegalStateException injected = assertThrows(IllegalStateException.class, () ->
                    tx.execute(status -> {
                        StateRepositoryPort.AtomicCommitOutcome second =
                                adapter.attemptAtomicCommit(patch(id,9,"rolled_back"));
                        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED, second.status);
                        throw new IllegalStateException("SYNTHETIC_AFTER_HISTORY_ROLLBACK");
                    }));
            assertEquals("SYNTHETIC_AFTER_HISTORY_ROLLBACK", injected.getMessage());
            assertEquals(9, repo.findById(id).get().getVersion().intValue());
            assertFalse(repo.findById(id).get().getPatientState().containsKey("rolled_back"));
            assertEquals(1, histories.findByCdpIdOrderByVersionNumberDesc(id).size());
        }
    }

    /**
     * Physical CDP persistence handoff for the U04, F3, U05 state fields.
     * Uses the authoritative Spring transactional adapter, not the synthetic
     * versioned repository. This is NOT the clinical authority issuing layer.
     */
    @Test
    public void physicalU04F3U05FieldTransitionsShareCdpHistoryAndRejectStaleWrites() {
        authorize();
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(IsolatedConfig.class)) {
            CDPRepository repo = ctx.getBean(CDPRepository.class);
            CDPVersionRepository histories = ctx.getBean(CDPVersionRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            StateRepositoryPort adapter = ctx.getBean(StateRepositoryPort.class);
            String id = tx.execute(status -> seed(repo));

            assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED,
                    adapter.attemptAtomicCommit(clinicalPatch(id, 8,
                            "/patient_state/safety_gate", "u04-gate",
                            "source_u03_decision_ref", "synthetic-u03")).status);
            assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED,
                    adapter.attemptAtomicCommit(clinicalPatch(id, 9,
                            "/patient_state/f3_gap_assessment", "f3-effect",
                            "f3_canonical_effect_id", "synthetic-f3")).status);
            assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED,
                    adapter.attemptAtomicCommit(clinicalPatch(id, 10,
                            "/patient_state/clinical_readiness", "u05-readiness",
                            "clinical_readiness", "CAN_ASK_MORE")).status);

            assertEquals(11, repo.findById(id).get().getVersion().intValue());
            Map<String,Object> current = repo.findById(id).get().getPatientState();
            assertEquals("synthetic-u03", ((Map<?,?>)current.get("safety_gate"))
                    .get("source_u03_decision_ref"));
            assertEquals("synthetic-f3", ((Map<?,?>)current.get("f3_gap_assessment"))
                    .get("f3_canonical_effect_id"));
            assertEquals("CAN_ASK_MORE", ((Map<?,?>)current.get("clinical_readiness"))
                    .get("clinical_readiness"));
            assertEquals(3, histories.findByCdpIdOrderByVersionNumberDesc(id).size());

            StateRepositoryPort.AtomicCommitOutcome rejected =
                    adapter.attemptAtomicCommit(clinicalPatch(id, 9,
                            "/patient_state/clinical_readiness", "stale-readiness",
                            "clinical_readiness", "READY"));
            assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.CONFLICT, rejected.status);
            assertEquals(3, histories.findByCdpIdOrderByVersionNumberDesc(id).size());
            assertEquals(11, repo.findById(id).get().getVersion().intValue());

            assertThrows(IllegalStateException.class, () ->
                    tx.execute(status -> {
                        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED,
                                adapter.attemptAtomicCommit(clinicalPatch(id, 11,
                                        "/patient_state/pending_question", "question-selection",
                                        "question_id", "synthetic-question")).status);
                        throw new IllegalStateException("SYNTHETIC_U06_ROLLBACK");
                    }));
            assertEquals(11, repo.findById(id).get().getVersion().intValue());
            assertFalse(repo.findById(id).get().getPatientState().containsKey("pending_question"));
            assertEquals(3, histories.findByCdpIdOrderByVersionNumberDesc(id).size());
        }
    }

    private static StateRepositoryPort.AtomicCommitCommand clinicalPatch(
            String id, int version, String path, String effectId,
            String field, String value) {
        StateTypes.StatePatchOperation op = new StateTypes.StatePatchOperation();
        op.op = "ADD";
        op.path = path;
        Map<String,Object> body = new LinkedHashMap<String,Object>();
        body.put("effect_id", effectId);
        body.put(field, value);
        op.value = body;
        op.source = "PATIENT_FACT";
        op.sensitivity = "PHI";
        StateTypes.StatePatch patch = new StateTypes.StatePatch();
        patch.operations = Arrays.asList(op);
        return new StateRepositoryPort.AtomicCommitCommand(id, version,
                "patch-" + effectId, "idem-" + effectId, patch);
    }

    @Test
    public void optInSpringReadbackUsesRealCommittedCdpAndRejectsStaleOrMissingAuthority() {
        authorize();
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "synthetic-u07-readback-opt-in",
                    Collections.singletonMap("aidoctor.u07.persistence.readback-enabled", "true")));
            ctx.register(IsolatedConfig.class, ClinicalCdpReadbackAdapter.class);
            ctx.refresh();
            CDPRepository repo = ctx.getBean(CDPRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            StateRepositoryPort writer = ctx.getBean(StateRepositoryPort.class);
            U05ClinicalReadinessSnapshotPort readback =
                    ctx.getBean(U05ClinicalReadinessSnapshotPort.class);
            String id = tx.execute(status -> seed(repo));
            assertThrows(IllegalStateException.class, () -> readback.read(id, 8));
            assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED,
                    writer.attemptAtomicCommit(clinicalPatch(id, 8,
                            "/patient_state/safety_gate", "u04", "gate", "ALLOW")).status);
            assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED,
                    writer.attemptAtomicCommit(clinicalPatch(id, 9,
                            "/patient_state/f3_gap_assessment", "f3",
                            "f3_canonical_effect_id", "synthetic-f3")).status);
            assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED,
                    writer.attemptAtomicCommit(clinicalPatch(id, 10,
                            "/patient_state/clinical_readiness", "u05",
                            "clinical_readiness", "CAN_ASK_MORE")).status);

            U05ClinicalReadinessSnapshot accepted = readback.read(id, 11);
            assertEquals(11, accepted.getVersion());
            assertEquals("CAN_ASK_MORE", accepted.getReadinessPayload().get("clinical_readiness"));
            assertThrows(IllegalStateException.class, () -> readback.read(id, 10));
            assertThrows(IllegalStateException.class, () -> readback.read("missing-cdp", 11));
            assertEquals(11, writer.readCurrentVersion(id));
            assertEquals("CAN_ASK_MORE", readback.read(id, 11)
                    .getReadinessPayload().get("clinical_readiness"));
        }
    }

    @Test
    public void gateAuthorityReadbackUsesActualCdpAndDoesNotMintA1() {
        authorize();
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "u07-authority-readback-opt-in",
                    Collections.<String,Object>singletonMap(
                            "aidoctor.u07.persistence.authority-readback-enabled", "true")));
            ctx.register(IsolatedConfig.class, ClinicalCdpGateReadbackAdapter.class);
            ctx.refresh();
            CDPRepository repo = ctx.getBean(CDPRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            StateRepositoryPort writer = ctx.getBean(StateRepositoryPort.class);
            U04A1U05AuthorityBridge.GateReadbackPort readback =
                    ctx.getBean(U04A1U05AuthorityBridge.GateReadbackPort.class);
            String id = tx.execute(status -> seed(repo));

            assertThrows(IllegalStateException.class, () -> readback.loadCurrent(id));
            assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED,
                    writer.attemptAtomicCommit(clinicalPatch(id, 8,
                            "/patient_state/safety_gate", "u04-gate",
                            "source_u03_decision_ref", "u03-1")).status);
            // An incomplete synthetic record is not trusted as a gate.
            assertThrows(IllegalStateException.class, () -> readback.loadCurrent(id));

            tx.execute(status -> {
                CDP cdp = repo.findByIdWithLock(id).get();
                Map<String,Object> state =
                        new LinkedHashMap<String,Object>(cdp.getPatientState());
                Map<String,Object> gate = new LinkedHashMap<String,Object>(
                        (Map<String,Object>) state.get("safety_gate"));
                gate.put("safety_gate", "ALLOW");
                gate.put("u04_policy_ref", U04ScopeContext.FROZEN_POLICY_REF);
                state.put("safety_gate", gate);
                cdp.setPatientState(state);
                repo.saveAndFlush(cdp);
                return null;
            });
            U04A1U05AuthorityBridge.GateReadback accepted = readback.loadCurrent(id);
            assertEquals(9, accepted.stateVersion);
            assertEquals("ALLOW", accepted.gateValue);
            assertEquals("u03-1", accepted.sourceU03DecisionRef);
            assertEquals(U04ScopeContext.FROZEN_POLICY_REF, accepted.policyRef);

            tx.execute(status -> {
                CDP cdp = repo.findByIdWithLock(id).get();
                Map<String,Object> state =
                        new LinkedHashMap<String,Object>(cdp.getPatientState());
                Map<String,Object> gate = new LinkedHashMap<String,Object>(
                        (Map<String,Object>) state.get("safety_gate"));
                gate.put("u04_policy_ref", "UNAPPROVED_POLICY");
                state.put("safety_gate", gate);
                cdp.setPatientState(state);
                repo.saveAndFlush(cdp);
                return null;
            });
            assertThrows(IllegalStateException.class, () -> readback.loadCurrent(id));
            assertTrue(ctx.getBeansOfType(
                    U04A1U05AuthorityBridge.A1BarrierAuthorityPort.class).isEmpty());
        }
    }

    @Test
    public void independentA1AttestationRequiresExactPhysicallyCommittedF3EffectAndVersion() {
        authorize();
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(IsolatedConfig.class)) {
            CDPRepository repo = ctx.getBean(CDPRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            StateRepositoryPort writer = ctx.getBean(StateRepositoryPort.class);
            String id = tx.execute(status -> seed(repo));
            java.util.concurrent.atomic.AtomicReference<String> attestedEffect =
                    new java.util.concurrent.atomic.AtomicReference<String>("f3-effect");
            ClinicalCdpF3BoundA1AuthorityPort port = new ClinicalCdpF3BoundA1AuthorityPort(
                    repo, (consultation, cdp, version) ->
                            new U04A1U05AuthorityBridge.A1BarrierAuthority(
                                    consultation, cdp, version, attestedEffect.get(),
                                    "independent-barrier", "independent-revalidation",
                                    "independent-route-source", "independent-route-auth",
                                    "independent-routing-policy", null, null, true, false));

            assertThrows(IllegalStateException.class,
                    () -> port.loadCurrent("consult-1", id, 8));
            assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED,
                    writer.attemptAtomicCommit(clinicalPatch(id, 8,
                            "/patient_state/f3_gap_assessment", "f3-effect",
                            "f3_canonical_effect_id", "f3-effect")).status);
            assertEquals(9, repo.findById(id).get().getVersion().intValue());
            assertThrows(IllegalStateException.class,
                    () -> port.loadCurrent("consult-1", id, 8));
            assertEquals("f3-effect",
                    port.loadCurrent("consult-1", id, 9).f3CommitRef);
            attestedEffect.set("different-effect");
            assertThrows(IllegalStateException.class,
                    () -> port.loadCurrent("consult-1", id, 9));
            attestedEffect.set("f3-effect");
            assertThrows(IllegalStateException.class,
                    () -> new ClinicalCdpF3BoundA1AuthorityPort(repo,
                            (consultation, cdp, version) -> null)
                            .loadCurrent("consult-1", id, 9));
            assertEquals(9, repo.findById(id).get().getVersion().intValue());
        }
    }

    @Test
    public void durableIndependentA1IssuerWritesOneTransactionalReceiptAndRejectsStaleReplays() {
        authorize();
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(IsolatedConfig.class)) {
            JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);
            jdbc.execute("CREATE TABLE IF NOT EXISTS u07_a1_attestation ("
                    + "consultation_id VARCHAR(64) NOT NULL, cdp_id VARCHAR(64) NOT NULL, "
                    + "cdp_version INT NOT NULL, f3_effect_id VARCHAR(128) NOT NULL, "
                    + "barrier_ref VARCHAR(128) NOT NULL, revalidation_ref VARCHAR(128) NOT NULL, "
                    + "route_source_ref VARCHAR(128) NOT NULL, "
                    + "route_authorization_ref VARCHAR(128) NOT NULL, "
                    + "routing_policy_version VARCHAR(128) NOT NULL, "
                    + "PRIMARY KEY (consultation_id, cdp_id, cdp_version)) ENGINE=InnoDB");
            CDPRepository repo = ctx.getBean(CDPRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            StateRepositoryPort writer = ctx.getBean(StateRepositoryPort.class);
            String id = tx.execute(status -> seed(repo));
            java.util.concurrent.atomic.AtomicBoolean barrier =
                    new java.util.concurrent.atomic.AtomicBoolean(false);
            U07DurableNonProductionA1Issuer.VerifiedPostF3EvidencePort verified =
                    (consultation, cdp, version, effect) ->
                            new U07DurableNonProductionA1Issuer.VerifiedEvidence(
                                    effect, "verified-barrier", "verified-revalidation",
                                    "verified-route-source", "verified-route-authorization",
                                    "U07_NONPROD_TEST_V1", barrier.get(), true, true);
            U07DurableNonProductionA1Issuer issuer = new U07DurableNonProductionA1Issuer(
                    repo, jdbc, tx, verified);

            assertThrows(IllegalStateException.class,
                    () -> issuer.issue("consult-1", id, 8));
            assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED,
                    writer.attemptAtomicCommit(clinicalPatch(id, 8,
                            "/patient_state/f3_gap_assessment", "durable-f3",
                            "f3_canonical_effect_id", "durable-effect")).status);
            assertThrows(IllegalStateException.class,
                    () -> issuer.issue("consult-1", id, 9));
            assertEquals(0, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_a1_attestation WHERE cdp_id=?",
                    Integer.class, id).intValue());

            barrier.set(true);
            U04A1U05AuthorityBridge.A1BarrierAuthority original =
                    issuer.issue("consult-1", id, 9);
            assertEquals("durable-effect", original.f3CommitRef);
            assertEquals("verified-barrier", original.barrierRef);
            assertEquals("durable-effect", issuer.issue("consult-1", id, 9).f3CommitRef);
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_a1_attestation WHERE cdp_id=?",
                    Integer.class, id).intValue());

            // Fresh issuer proves a durable DB read, not process-local memory.
            U07DurableNonProductionA1Issuer restarted = new U07DurableNonProductionA1Issuer(
                    repo, jdbc, tx, verified);
            ClinicalCdpF3BoundA1AuthorityPort consumer =
                    new ClinicalCdpF3BoundA1AuthorityPort(repo, restarted);
            assertEquals("verified-revalidation",
                    consumer.loadCurrent("consult-1", id, 9).revalidationRef);

            U07DurableNonProductionA1Issuer changed = new U07DurableNonProductionA1Issuer(
                    repo, jdbc, tx, (consultation, cdp, version, effect) ->
                            new U07DurableNonProductionA1Issuer.VerifiedEvidence(
                                    effect, "different-barrier", "verified-revalidation",
                                    "verified-route-source", "verified-route-authorization",
                                    "U07_NONPROD_TEST_V1", true, true, true));
            assertThrows(IllegalStateException.class,
                    () -> changed.issue("consult-1", id, 9));
            assertThrows(IllegalStateException.class,
                    () -> consumer.loadCurrent("consult-1", id, 8));

            assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED,
                    writer.attemptAtomicCommit(clinicalPatch(id, 9,
                            "/patient_state/another_state", "version-bump",
                            "reason", "synthetic-unrelated")).status);
            assertThrows(IllegalStateException.class,
                    () -> consumer.loadCurrent("consult-1", id, 9));
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM u07_a1_attestation WHERE cdp_id=?",
                    Integer.class, id).intValue());
        }
    }

    @Test
    public void noPartialCdpWriteOnInvalidSecondOperation() {
        authorize();
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(IsolatedConfig.class)) {
            CDPRepository repo = ctx.getBean(CDPRepository.class);
            CDPVersionRepository histories = ctx.getBean(CDPVersionRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            StateRepositoryPort adapter = ctx.getBean(StateRepositoryPort.class);
            String id = tx.execute(status -> seed(repo));
            StateTypes.StatePatchOperation valid = new StateTypes.StatePatchOperation();
            valid.op = "ADD"; valid.path = "/patient_state/first";
            valid.value = "first";valid.source = "PATIENT_FACT";valid.sensitivity = "PHI";
            StateTypes.StatePatchOperation invalid = new StateTypes.StatePatchOperation();
            invalid.op = "ADD"; invalid.path = "/patient_state/missing/deeper";
            invalid.value = "bad";invalid.source = "PATIENT_FACT";invalid.sensitivity = "PHI";
            StateTypes.StatePatch patch = new StateTypes.StatePatch();
            patch.operations = Arrays.asList(valid,invalid);
            StateRepositoryPort.AtomicCommitOutcome failed = adapter.attemptAtomicCommit(
                    new StateRepositoryPort.AtomicCommitCommand(id,8,"invalid-two-op","idem-invalid",patch));
            assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.FAILED, failed.status);
            assertEquals(8, repo.findById(id).get().getVersion().intValue());
            assertFalse(repo.findById(id).get().getPatientState().containsKey("first"));
            assertEquals(0, histories.findByCdpIdOrderByVersionNumberDesc(id).size());
        }
    }
}
