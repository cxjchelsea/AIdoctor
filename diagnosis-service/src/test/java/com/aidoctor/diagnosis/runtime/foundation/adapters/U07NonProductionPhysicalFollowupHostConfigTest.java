package com.aidoctor.diagnosis.runtime.foundation.adapters;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import static org.junit.jupiter.api.Assertions.*;

class U07NonProductionPhysicalFollowupHostConfigTest {
    private AnnotationConfigApplicationContext host(boolean enable, String profile) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        if (profile != null) context.getEnvironment().setActiveProfiles(profile);
        if (enable) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "physical-followup-flag", Collections.<String,Object>singletonMap(
                    "aidoctor.v1.clinical-e2e.physical-followup-enabled", "true")));
        }
        context.register(U07NonProductionPhysicalFollowupHostConfig.class);
        context.refresh();
        return context;
    }

    @Test void defaultDisabled() {
        try (AnnotationConfigApplicationContext ctx = host(false, "clinical-e2e-test")) {
            assertTrue(ctx.getBeansOfType(U07NonProductionPhysicalFollowupCoordinator.class)
                    .isEmpty());
        }
    }

    @Test void optInWithoutIndependentOwnersFailsClosed() {
        try (AnnotationConfigApplicationContext ctx = host(true, "clinical-e2e-test")) {
            assertTrue(ctx.getBeansOfType(U07NonProductionPhysicalFollowupCoordinator.class)
                    .isEmpty());
        }
    }

    @Test void productionProfileNeverInstallsFollowup() {
        try (AnnotationConfigApplicationContext ctx = host(true, "production")) {
            assertTrue(ctx.getBeansOfType(U07NonProductionPhysicalFollowupCoordinator.class)
                    .isEmpty());
        }
    }
}
