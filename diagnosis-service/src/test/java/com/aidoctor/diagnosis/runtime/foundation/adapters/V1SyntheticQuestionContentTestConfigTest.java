package com.aidoctor.diagnosis.runtime.foundation.adapters;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import static org.junit.jupiter.api.Assertions.*;

class V1SyntheticQuestionContentTestConfigTest {
    private AnnotationConfigApplicationContext context(String profile,boolean enabled) {
        AnnotationConfigApplicationContext ctx=new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles(profile);
        if(enabled) ctx.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("fixture",Collections.<String,Object>singletonMap(
                    "aidoctor.v1.clinical-e2e.synthetic-question-content-enabled","true")));
        ctx.register(V1SyntheticQuestionContentTestConfig.class);
        ctx.refresh();
        return ctx;
    }
    @Test void explicitlyOptedInFixtureIsNarrowlyAllowlisted() {
        try(AnnotationConfigApplicationContext ctx=context("clinical-e2e-test",true)) {
            V1NonProductionFirstRoundOwnerAdapter.AuthorizedQuestionContent owner=ctx.getBean(
                    V1NonProductionFirstRoundOwnerAdapter.AuthorizedQuestionContent.class);
            assertTrue(owner.requireText("consult","run","test-u06-question-none",
                    "synthetic follow-up question").contains("合成测试"));
            assertThrows(IllegalStateException.class,()->owner.requireText(
                    "consult","run","real-clinical-question","synthetic follow-up question"));
            assertThrows(IllegalStateException.class,()->owner.requireText(
                    "consult","run","test-u06-question-none","unknown content"));
        }
    }
    @Test void productionAndDefaultProfilesNeverInstallFixture() {
        for(String profile:new String[]{"production","prod","default","clinical-e2e-test"}) {
            boolean enabled=!"clinical-e2e-test".equals(profile);
            try(AnnotationConfigApplicationContext ctx=context(profile,enabled)) {
                assertTrue(ctx.getBeansOfType(
                        V1NonProductionFirstRoundOwnerAdapter.AuthorizedQuestionContent.class).isEmpty());
            }
        }
    }
}
