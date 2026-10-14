package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.foundation.*;
import com.aidoctor.diagnosis.runtime.u01.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * REAL disposable MySQL 8 + Spring JPA identity/readback test.
 * This is NOT a C01, P01 clinical commit, HTTP, or complete clinical E2E.
 */
public class U01U02PhysicalMysqlHandoffTest {
    private static final String URL = "jdbc:mysql://127.0.0.1:33333/v1_u01_u02?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static void authorize() {
        if (!"true".equals(System.getenv("GITHUB_ACTIONS"))
                || !"DISPOSABLE_U01_U02_ONLY".equals(System.getenv("V1_U01_U02_MYSQL_MODE"))
                || !URL.equals(System.getenv("V1_U01_U02_MYSQL_URL")))
            throw new IllegalStateException("V1_U01_U02_PHYSICAL_NONPRODUCTION_ONLY");
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(
            basePackages = {"com.aidoctor.diagnosis.runtime.u01",
                    "com.aidoctor.diagnosis.runtime.foundation",
                    "com.aidoctor.diagnosis.repository"},
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {ConsultationRepository.class, RuntimeBindingRepository.class,
                            ClinicalRunRepository.class, CanonicalBusinessEventRepository.class,
                            CDPRepository.class}))
    public static class DatabaseConfig {
        @Bean(destroyMethod = "close")
        public HikariDataSource dataSource() {
            authorize();
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(URL);
            cfg.setUsername("root");
            cfg.setPassword("synthetic-u01-u02-root");
            cfg.setMaximumPoolSize(4);
            return new HikariDataSource(cfg);
        }

        @Bean(name = "entityManagerFactory")
        public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
            emf.setDataSource(dataSource);
            emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            emf.setPackagesToScan("com.aidoctor.diagnosis.runtime.u01",
                    "com.aidoctor.diagnosis.runtime.foundation", "com.aidoctor.diagnosis.entity");
            emf.setPersistenceUnitPostProcessors(unit -> {
                unit.getManagedClassNames().clear();
                unit.getManagedClassNames().add(CDP.class.getName());
                unit.getManagedClassNames().add(ConsultationRecord.class.getName());
                unit.getManagedClassNames().add(RuntimeBindingRecord.class.getName());
                unit.getManagedClassNames().add(ClinicalRunRecord.class.getName());
                unit.getManagedClassNames().add(CanonicalBusinessEventRecord.class.getName());
            });
            java.util.Properties properties = new java.util.Properties();
            properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
            properties.put("hibernate.hbm2ddl.auto", "none");
            emf.setJpaProperties(properties);
            return emf;
        }

        @Bean(name = "transactionManager")
        public JpaTransactionManager transactionManager(EntityManagerFactory emf, DataSource source) {
            JpaTransactionManager manager = new JpaTransactionManager(emf);
            manager.setDataSource(source);
            return manager;
        }
        @Bean
        public TransactionTemplate tx(JpaTransactionManager manager) {
            return new TransactionTemplate(manager);
        }
        @Bean
        public RuntimeBindingService bindings(RuntimeBindingRepository repo) {
            return new RuntimeBindingService(repo);
        }
        @Bean
        public U01U02TrustedStartContext resolver(ConsultationRepository consultations,
                RuntimeBindingService bindings, ClinicalRunRepository runs,
                CanonicalBusinessEventRepository events, CDPRepository cdps) {
            return new U01U02TrustedStartContext(consultations, bindings, runs, events, cdps);
        }
    }

    @Test
    public void durableHostReadsCanonicalContextAndRejectsStaleOrAlteredInput() throws Exception {
        Assumptions.assumeTrue("DISPOSABLE_U01_U02_ONLY".equals(System.getenv("V1_U01_U02_MYSQL_MODE")),
                "Physical test requires isolated disposable MySQL job");
        authorize();
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(DatabaseConfig.class)) {
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
            CDPRepository cdps = ctx.getBean(CDPRepository.class);
            ConsultationRepository consultations = ctx.getBean(ConsultationRepository.class);
            ClinicalRunRepository runs = ctx.getBean(ClinicalRunRepository.class);
            CanonicalBusinessEventRepository events = ctx.getBean(CanonicalBusinessEventRepository.class);
            RuntimeBindingService bindings = ctx.getBean(RuntimeBindingService.class);
            String raw = "synthetic patient headache";
            tx.execute(s -> {
                CDP cdp = CDP.builder().id("physical-cdp").patientId("synthetic-user")
                        .sessionId("synthetic-session").version(0).build();
                cdps.saveAndFlush(cdp);
                String fp = digest(raw + "|subjectRef=");
                U01StartCommand command = new U01StartCommand("physical-event",
                        "physical-idem", "synthetic-user", "SELF", null,
                        raw, "SYMPTOM", false, fp);
                U01SemanticDecision decision = new U01SemanticDecision(
                        U01SemanticDecision.SUBJECT_RESOLVED, "SELF",
                        U01SemanticDecision.PROBLEM_FRAMED, U01SemanticDecision.IN_SCOPE,
                        null, "U02", false);
                consultations.saveAndFlush(new ConsultationRecord(
                        "physical-consult", "physical-cdp", "synthetic-user",
                        command, decision, LocalDateTime.now()));
                events.saveAndFlush(new CanonicalBusinessEventRecord(
                        "physical-event", "physical-consult", "START_CONSULTATION",
                        "physical-idem", digest("synthetic-user|raw:" + fp),
                        LocalDateTime.now()));
                RuntimeBindingRecord bound = bindings.bind("physical-consult", "physical-cdp",
                        RuntimeBindingRecord.CLINICAL_RUNTIME_V1,
                        "aidoctor-v1-scope", "aidoctor-v1-u01", "contracts-v1");
                runs.saveAndFlush(new ClinicalRunRecord("physical-run", bound.getThreadId(),
                        "physical-consult", "physical-event", 0, LocalDateTime.now()));
                return null;
            });
            U01U02TrustedStartContext resolver = ctx.getBean(U01U02TrustedStartContext.class);
            U01U02TrustedStartContext.VerifiedStart resolved =
                    resolver.require("physical-consult", "physical-run", raw);
            assertEquals("physical-run", resolved.runId);
            assertEquals("physical-event", resolved.eventId);
            assertEquals("physical-cdp", resolved.cdpId);
            assertTrue(resolved.threadId.startsWith("thread_"));
            assertTrue(assertThrows(IllegalStateException.class, () ->
                    resolver.require("physical-consult", "physical-run", "altered symptom"))
                    .getMessage().contains("SOURCE_FINGERPRINT_MISMATCH"));
            tx.execute(s -> {
                CDP cdp = cdps.findById("physical-cdp").orElseThrow(IllegalStateException::new);
                cdp.setVersion(1);
                cdps.saveAndFlush(cdp);
                return null;
            });
            assertTrue(assertThrows(IllegalStateException.class, () ->
                    resolver.require("physical-consult", "physical-run", raw))
                    .getMessage().contains("CDP_STATE_STALE"));
            assertEquals(1, cdps.findById("physical-cdp").get().getVersion().intValue());
        }
    }

    private static String digest(String s) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) hex.append(String.format("%02x", b & 0xff));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
