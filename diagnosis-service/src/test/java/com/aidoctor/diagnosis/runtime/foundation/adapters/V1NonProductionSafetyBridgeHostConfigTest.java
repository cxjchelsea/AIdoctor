package com.aidoctor.diagnosis.runtime.foundation.adapters;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import static org.junit.jupiter.api.Assertions.*;

class V1NonProductionSafetyBridgeHostConfigTest {
    private AnnotationConfigApplicationContext host(boolean enabled, String profile) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles(profile);
        if (enabled) {
            ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "v1-bridge-flag", Collections.<String, Object>singletonMap(
                    "aidoctor.v1.clinical-e2e.u01-u04-http-enabled", "true")));
        }
        ctx.register(V1NonProductionSafetyBridgeHostConfig.class);
        ctx.refresh();
        return ctx;
    }

    @Test void disabledByDefault() {
        try (AnnotationConfigApplicationContext ctx = host(false, "clinical-e2e-test")) {
            assertTrue(ctx.getBeansOfType(
                    com.aidoctor.diagnosis.runtime.u03.U02U03GovernedSafetyBridge.class)
                    .isEmpty());
        }
    }

    @Test void missingAcceptedEvidenceAndSafetyOwnersCannotActivate() {
        try (AnnotationConfigApplicationContext ctx = host(true, "clinical-e2e-test")) {
            assertTrue(ctx.getBeansOfType(
                    com.aidoctor.diagnosis.runtime.u03.U02U03GovernedSafetyBridge.class)
                    .isEmpty());
        }
    }

    @Test void productionProfileNeverActivatesBridge() {
        try (AnnotationConfigApplicationContext ctx = host(true, "production")) {
            assertTrue(ctx.getBeansOfType(
                    com.aidoctor.diagnosis.runtime.u03.U02U03GovernedSafetyBridge.class)
                    .isEmpty());
        }
    }
}
