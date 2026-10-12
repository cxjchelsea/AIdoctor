package com.aidoctor.diagnosis.runtime.u07.d5o2;

import com.aidoctor.diagnosis.runtime.u01.ConsultationRecord;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRepository;
import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitEffectRecord;
import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitEffectRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Isolated test-only candidate, never imported into production application.
 * This configuration alone makes no claim about original U06 wiring/proxy.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = {
        "com.aidoctor.diagnosis.runtime.u01",
        "com.aidoctor.diagnosis.runtime.u06.wait",
        "com.aidoctor.diagnosis.runtime.foundation"
    },
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {ConsultationRepository.class, ConsultationWaitEffectRepository.class,
            RuntimeThreadStateRepository.class, RuntimeWaitCheckpointRepository.class}),
    repositoryImplementationPostfix = "StageAImplementationOnly",
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager")
public class U07D5O2StageAFixtureConfig {
    public static final String FIXED_URL =
        "jdbc:mysql://127.0.0.1:33326/u07_d5o2_stage_a?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    public static final Set<String> ENTITY_NAMES = new HashSet<String>(Arrays.asList(
        ConsultationRecord.class.getName(), ConsultationWaitEffectRecord.class.getName(),
        RuntimeThreadStateRecord.class.getName(), RuntimeWaitCheckpointRecord.class.getName()));

    static void requireEnvironment() {
        if (!"U07_D5O2_STAGE_A_SYNTHETIC".equals(System.getenv("U07_D5O2_PROFILE")))
            throw new IllegalStateException("STAGE_A_PROFILE_DENIED");
        if (!FIXED_URL.equals(System.getenv("U07_D5O2_URL")))
            throw new IllegalStateException("STAGE_A_URL_DENIED");
        if (!"u07_d5o2_stage_a".equals(System.getenv("U07_D5O2_SCHEMA")))
            throw new IllegalStateException("STAGE_A_SCHEMA_DENIED");
    }

    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource() {
        requireEnvironment(); // Reject before connection creation.
        HikariConfig c = new HikariConfig();
        c.setJdbcUrl(FIXED_URL);
        c.setUsername("stagea_writer");
        c.setPassword("synthetic-stagea-writer");
        c.setMaximumPoolSize(4);
        c.setMinimumIdle(0);
        c.setPoolName("u07-d5o2-stagea-disposable");
        return new HikariDataSource(c);
    }

    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setPackagesToScan("com.aidoctor.diagnosis.runtime.u01",
            "com.aidoctor.diagnosis.runtime.u06.wait",
            "com.aidoctor.diagnosis.runtime.foundation");
        emf.setPersistenceUnitPostProcessors((PersistenceUnitPostProcessor) pui -> {
            pui.getManagedClassNames().clear();
            pui.getManagedClassNames().addAll(ENTITY_NAMES);
        });
        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "validate");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "false");
        emf.setJpaProperties(properties);
        return emf;
    }

    @Bean(name = "transactionManager")
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory, DataSource dataSource) {
        JpaTransactionManager manager = new JpaTransactionManager(entityManagerFactory);
        manager.setDataSource(dataSource);
        return manager;
    }

    @Bean
    public TransactionTemplate stageATransactionTemplate(JpaTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    public JdbcTemplate stageAJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
