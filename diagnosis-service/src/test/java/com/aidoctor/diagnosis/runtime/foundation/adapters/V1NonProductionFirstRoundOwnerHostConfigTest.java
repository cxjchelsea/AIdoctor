package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.controller.V1NonProductionFirstRoundWaitController;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class V1NonProductionFirstRoundOwnerHostConfigTest {
    private AnnotationConfigApplicationContext context(boolean owners, String profile) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles(profile);
        ctx.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("test", Collections.<String,Object>singletonMap(
                    "aidoctor.v1.clinical-e2e.first-round-wait-http-enabled", "true")));
        ctx.registerBean(V1NonProductionDeliveredQuestionReadback.class,
                () -> new V1NonProductionDeliveredQuestionReadback(
                        mock(com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository.class),
                        mock(com.aidoctor.diagnosis.repository.CDPRepository.class)));
        if (owners) {
            ctx.registerBean(V1NonProductionFirstRoundOwnerAdapter.AuthorizedStateOwner.class,
                    () -> mock(V1NonProductionFirstRoundOwnerAdapter.AuthorizedStateOwner.class));
            ctx.registerBean(V1NonProductionFirstRoundOwnerAdapter.AuthorizedQuestionContent.class,
                    () -> mock(V1NonProductionFirstRoundOwnerAdapter.AuthorizedQuestionContent.class));
            ctx.registerBean(V1NonProductionFirstRoundOwnerAdapter.DeliveryOwnerAuthority.class,
                    () -> mock(V1NonProductionFirstRoundOwnerAdapter.DeliveryOwnerAuthority.class));
        }
        ctx.register(V1NonProductionFirstRoundOwnerHostConfig.class);
        ctx.refresh();
        return ctx;
    }
    @Test void independentOwnersAreRequired() {
        try(AnnotationConfigApplicationContext ctx=context(false,"clinical-e2e-test")) {
            assertTrue(ctx.getBeansOfType(V1NonProductionFirstRoundWaitController.FirstRoundOwner.class).isEmpty());
        }
        try(AnnotationConfigApplicationContext ctx=context(true,"clinical-e2e-test")) {
            assertEquals(1,ctx.getBeansOfType(V1NonProductionFirstRoundWaitController.FirstRoundOwner.class).size());
        }
        try(AnnotationConfigApplicationContext ctx=context(true,"production")) {
            assertTrue(ctx.getBeansOfType(V1NonProductionFirstRoundWaitController.FirstRoundOwner.class).isEmpty());
        }
    }
}
