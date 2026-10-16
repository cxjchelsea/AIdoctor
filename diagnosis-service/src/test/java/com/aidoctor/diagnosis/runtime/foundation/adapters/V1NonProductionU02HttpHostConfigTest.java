package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.runtime.u02.*;
import com.aidoctor.diagnosis.runtime.u02.capability.C01U02CapabilityGateway;
import com.aidoctor.diagnosis.runtime.u02.d05.U02DependencyInvalidationHook;
import com.aidoctor.diagnosis.runtime.governance.CapabilityTraceService;
import com.aidoctor.diagnosis.runtime.foundation.*;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRepository;
import com.aidoctor.diagnosis.repository.CDPRepository;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class V1NonProductionU02HttpHostConfigTest {
    private AnnotationConfigApplicationContext context(boolean enabled, boolean trusted,
            String profile) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles(profile);
        if (enabled) ctx.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("v1-u02-http", Collections.<String, Object>singletonMap(
                        "aidoctor.v1.clinical-e2e.u01-u04-http-enabled", "true")));
        ctx.registerBean(U01U02TrustedStartContext.class, () -> new U01U02TrustedStartContext(
                mock(ConsultationRepository.class), mock(RuntimeBindingService.class),
                mock(ClinicalRunRepository.class), mock(CanonicalBusinessEventRepository.class),
                mock(CDPRepository.class)));
        ctx.registerBean(U02ClinicalFactApplicationService.class,
                () -> new U02ClinicalFactApplicationService(
                        mock(C01U02CapabilityGateway.class),
                        mock(U02ClinicalFactBusinessOwner.class),
                        mock(U02ClinicalFactProposalFactory.class),
                        mock(U02ClinicalFactCommitService.class),
                        new U02DependencyInvalidationHook(), mock(CapabilityTraceService.class)));
        if (trusted) ctx.registerBean(U01U02NonProductionHandoffService.TrustedTraceAuthority.class,
                () -> mock(U01U02NonProductionHandoffService.TrustedTraceAuthority.class));
        ctx.register(V1NonProductionU02HttpHostConfig.class);
        ctx.refresh();
        return ctx;
    }

    @Test void firstRoundFlagWiresRealU02HandoffWithoutLegacyFlag() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles("clinical-e2e-test");
        ctx.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("v1-first-round", Collections.<String,Object>singletonMap(
                        "aidoctor.v1.clinical-e2e.first-round-wait-http-enabled", "true")));
        ctx.registerBean(U01U02TrustedStartContext.class, () -> new U01U02TrustedStartContext(
                mock(ConsultationRepository.class), mock(RuntimeBindingService.class),
                mock(ClinicalRunRepository.class), mock(CanonicalBusinessEventRepository.class),
                mock(CDPRepository.class)));
        ctx.registerBean(U02ClinicalFactApplicationService.class,
                () -> new U02ClinicalFactApplicationService(
                        mock(C01U02CapabilityGateway.class),
                        mock(U02ClinicalFactBusinessOwner.class),
                        mock(U02ClinicalFactProposalFactory.class),
                        mock(U02ClinicalFactCommitService.class),
                        new U02DependencyInvalidationHook(), mock(CapabilityTraceService.class)));
        ctx.registerBean(U01U02NonProductionHandoffService.TrustedTraceAuthority.class,
                () -> mock(U01U02NonProductionHandoffService.TrustedTraceAuthority.class));
        ctx.register(V1NonProductionU02HttpHostConfig.class);
        try {
            ctx.refresh();
            assertEquals(1, ctx.getBeansOfType(U01U02NonProductionHandoffService.class).size());
        } finally {
            ctx.close();
        }
    }

    @Test void disabledFlagDoesNotActivateHttpHandoff() {
        try (AnnotationConfigApplicationContext ctx = context(false, true, "clinical-e2e-test")) {
            assertTrue(ctx.getBeansOfType(U01U02NonProductionHandoffService.class).isEmpty());
        }
    }

    @Test void missingTrustedTraceAuthorityFailsClosed() {
        try (AnnotationConfigApplicationContext ctx = context(true, false, "clinical-e2e-test")) {
            assertTrue(ctx.getBeansOfType(U01U02NonProductionHandoffService.class).isEmpty());
        }
    }

    @Test void authorizedNonproductionHostCreatesRealHandoffBean() {
        try (AnnotationConfigApplicationContext ctx = context(true, true, "clinical-e2e-test")) {
            assertEquals(1, ctx.getBeansOfType(U01U02NonProductionHandoffService.class).size());
        }
    }

    @Test void productionProfileNeverActivatesHttpHandoff() {
        try (AnnotationConfigApplicationContext ctx = context(true, true, "production")) {
            assertTrue(ctx.getBeansOfType(U01U02NonProductionHandoffService.class).isEmpty());
        }
    }
}
