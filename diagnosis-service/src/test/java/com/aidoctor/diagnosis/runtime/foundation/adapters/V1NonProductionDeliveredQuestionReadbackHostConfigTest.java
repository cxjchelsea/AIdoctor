package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class V1NonProductionDeliveredQuestionReadbackHostConfigTest {
    private AnnotationConfigApplicationContext context(String profile, boolean enabled,
            boolean repositoriesPresent) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles(profile);
        if (enabled) ctx.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("first-round", Collections.<String,Object>singletonMap(
                    "aidoctor.v1.clinical-e2e.first-round-wait-http-enabled", "true")));
        if (repositoriesPresent) {
            ctx.registerBean(CDPRepository.class, () -> mock(CDPRepository.class));
            ctx.registerBean(RuntimeWaitCheckpointRepository.class,
                    () -> mock(RuntimeWaitCheckpointRepository.class));
        }
        ctx.register(V1NonProductionDeliveredQuestionReadbackHostConfig.class);
        ctx.refresh();
        return ctx;
    }

    @Test void activeOnlyWithOptInAndRealRepositories() {
        try (AnnotationConfigApplicationContext ctx =
                context("clinical-e2e-test", true, true)) {
            assertEquals(1,ctx.getBeansOfType(V1NonProductionDeliveredQuestionReadback.class).size());
        }
        try (AnnotationConfigApplicationContext ctx =
                context("clinical-e2e-test", true, false)) {
            assertTrue(ctx.getBeansOfType(V1NonProductionDeliveredQuestionReadback.class).isEmpty());
        }
        try (AnnotationConfigApplicationContext ctx =
                context("clinical-e2e-test", false, true)) {
            assertTrue(ctx.getBeansOfType(V1NonProductionDeliveredQuestionReadback.class).isEmpty());
        }
        try (AnnotationConfigApplicationContext ctx =
                context("production", true, true)) {
            assertTrue(ctx.getBeansOfType(V1NonProductionDeliveredQuestionReadback.class).isEmpty());
        }
    }
}
