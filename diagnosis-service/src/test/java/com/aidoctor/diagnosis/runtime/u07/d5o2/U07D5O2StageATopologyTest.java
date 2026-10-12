package com.aidoctor.diagnosis.runtime.u07.d5o2;

import com.aidoctor.diagnosis.runtime.u01.ConsultationRecord;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRepository;
import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitEffectRecord;
import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitEffectRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository;
import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitTransitionService;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointService;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadWaitTransitionService;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import static org.junit.jupiter.api.Assertions.*;

/** Source observations and candidate wiring are intentionally separate evidence planes. */
public class U07D5O2StageATopologyTest {
    @Test
    public void originalFinalServicesAreNotImplicitlyAdvised() {
        assertTrue(Modifier.isFinal(ConsultationWaitTransitionService.class.getModifiers()));
        assertTrue(Modifier.isFinal(RuntimeWaitCheckpointService.class.getModifiers()));
        assertTrue(Modifier.isFinal(RuntimeThreadWaitTransitionService.class.getModifiers()));
        // Source-only reflection: never claim to have opened the production context.
        assertFalse(ConsultationWaitTransitionService.class.isAnnotationPresent(
            org.springframework.stereotype.Service.class));
        assertFalse(RuntimeWaitCheckpointService.class.isAnnotationPresent(
            org.springframework.stereotype.Service.class));
        assertFalse(RuntimeThreadWaitTransitionService.class.isAnnotationPresent(
            org.springframework.stereotype.Service.class));
    }

    @Test
    public void candidateContextHasExactlyFourRepositoriesAndFourEntities() {
        U07D5O2StageAFixtureConfig.requireEnvironment();
        try (AnnotationConfigApplicationContext context =
                 new AnnotationConfigApplicationContext(U07D5O2StageAFixtureConfig.class)) {
            assertEquals(1, context.getBeanNamesForType(PlatformTransactionManager.class).length);
            assertTrue(context.getBean("transactionManager") instanceof JpaTransactionManager);
            assertEquals(1, context.getBeanNamesForType(EntityManagerFactory.class).length);
            Set<String> managed = context.getBean(EntityManagerFactory.class).getMetamodel()
                .getEntities().stream().map(x -> x.getJavaType().getName())
                .collect(Collectors.toSet());
            assertEquals(U07D5O2StageAFixtureConfig.ENTITY_NAMES, managed,
                "R1-F02 managed entity allowlist must be exact");
            Set<Class<?>> repositories = new HashSet<Class<?>>(Arrays.asList(
                ConsultationRepository.class, ConsultationWaitEffectRepository.class,
                RuntimeThreadStateRepository.class, RuntimeWaitCheckpointRepository.class));
            Set<Class<?>> actual = new HashSet<Class<?>>();
            for (String beanName : context.getBeanNamesForType(Repository.class)) {
                Object bean = context.getBean(beanName);
                for (Class<?> expected : repositories) {
                    if (expected.isInstance(bean)) actual.add(expected);
                }
                assertTrue(repositories.stream().anyMatch(c -> c.isInstance(bean)),
                    "R1-F01 unexpected repository bean " + beanName);
                assertTrue(AopUtils.isAopProxy(bean), "Expected Spring Data repository proxy");
            }
            assertEquals(repositories, actual, "R1-F01 repository allowlist mismatch");
            assertEquals(4, context.getBeanNamesForType(Repository.class).length);
            assertFalse(context.containsBean("consultationWaitTransitionService"),
                "Test wiring must not masquerade as production service registration");
        }
    }
}
