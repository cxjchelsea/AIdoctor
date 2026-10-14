package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.entity.CDPVersion;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.repository.CDPVersionRepository;
import com.aidoctor.diagnosis.service.cdp.CDPVersionService;
import com.aidoctor.diagnosis.util.JsonUtil;
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
            ClinicalCdpStateRepositoryAdapter adapter =
                    ctx.getBean(ClinicalCdpStateRepositoryAdapter.class);
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

    @Test
    public void noPartialCdpWriteOnInvalidSecondOperation() {
        authorize();
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(IsolatedConfig.class)) {
            CDPRepository repo = ctx.getBean(CDPRepository.class);
            CDPVersionRepository histories = ctx.getBean(CDPVersionRepository.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            ClinicalCdpStateRepositoryAdapter adapter = ctx.getBean(ClinicalCdpStateRepositoryAdapter.class);
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
