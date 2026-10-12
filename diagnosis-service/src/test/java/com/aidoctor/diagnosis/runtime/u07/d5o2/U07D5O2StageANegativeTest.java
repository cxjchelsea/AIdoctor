package com.aidoctor.diagnosis.runtime.u07.d5o2;

import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitTransitionService;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRepository;
import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitEffectRepository;
import javax.sql.DataSource;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Negative controls must fail closed, never elevate test-only advice to production evidence. */
public class U07D5O2StageANegativeTest {
    @Test
    public void originalFinalConcreteClassIsUnadvisedAndCannotBeCglibSubclassed() {
        U07D5O2StageAFixtureConfig.requireEnvironment();
        try (AnnotationConfigApplicationContext ctx =
                 new AnnotationConfigApplicationContext(U07D5O2StageAFixtureConfig.class)) {
            ConsultationWaitTransitionService original = new ConsultationWaitTransitionService(
                ctx.getBean(ConsultationRepository.class),
                ctx.getBean(ConsultationWaitEffectRepository.class));
            assertFalse(org.springframework.aop.support.AopUtils.isAopProxy(original));
            ProxyFactory forced = new ProxyFactory(original);
            forced.setProxyTargetClass(true);
            forced.addAdvice((MethodInterceptor) invocation -> invocation.proceed());
            assertThrows(org.springframework.aop.framework.AopConfigException.class, forced::getProxy);
        }
    }

    @Test
    public void wrongTransactionManagerDoesNotBindExpectedDatasource() {
        U07D5O2StageAFixtureConfig.requireEnvironment();
        try (AnnotationConfigApplicationContext ctx =
                 new AnnotationConfigApplicationContext(U07D5O2StageAFixtureConfig.class)) {
            DataSource expected = ctx.getBean(DataSource.class);
            DriverManagerDataSource mismatched = new DriverManagerDataSource(
                U07D5O2StageAFixtureConfig.FIXED_URL,
                "stagea_writer", "synthetic-stagea-writer");
            TransactionTemplate wrong = new TransactionTemplate(
                new DataSourceTransactionManager(mismatched));
            assertThrows(IllegalStateException.class, () -> wrong.execute(status -> {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                if (!TransactionSynchronizationManager.hasResource(expected))
                    throw new IllegalStateException("STAGE_A_EXPECTED_DATASOURCE_UNBOUND");
                return null;
            }));
        }
    }
}
