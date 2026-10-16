package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.diagnosis.runtime.foundation.adapters.U01U02NonProductionHostConfig;
import com.aidoctor.diagnosis.runtime.foundation.*;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRepository;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u02.capability.C01U02CapabilityGateway;
import com.aidoctor.diagnosis.runtime.u02.d05.U02DependencyInvalidationHook;
import com.aidoctor.diagnosis.runtime.governance.CapabilityTraceService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class U01U02NonProductionHostConfigTest {
    private AnnotationConfigApplicationContext context(boolean enabled, boolean trusted) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        if (enabled) {
            ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "handoff-flag", Collections.<String, Object>singletonMap(
                    "aidoctor.v1.clinical-e2e.u01-u02-handoff-enabled", "true")));
        }
        ctx.registerBean(U01U02TrustedStartContext.class,
                () -> new U01U02TrustedStartContext(
                        mock(ConsultationRepository.class), mock(RuntimeBindingService.class),
                        mock(ClinicalRunRepository.class), mock(CanonicalBusinessEventRepository.class),
                        mock(CDPRepository.class)));
        ctx.registerBean(U02ClinicalFactApplicationService.class,
                () -> new U02ClinicalFactApplicationService(
                        mock(C01U02CapabilityGateway.class), mock(U02ClinicalFactBusinessOwner.class),
                        mock(U02ClinicalFactProposalFactory.class), mock(U02ClinicalFactCommitService.class),
                        new U02DependencyInvalidationHook(), mock(CapabilityTraceService.class)));
        if (trusted) {
            ctx.registerBean(U01U02NonProductionHandoffService.TrustedTraceAuthority.class,
                    () -> mock(U01U02NonProductionHandoffService.TrustedTraceAuthority.class));
        }
        ctx.register(U01U02NonProductionHostConfig.class);
        return ctx;
    }

    @Test void disabledByDefault() {
        try (AnnotationConfigApplicationContext ctx = context(false, true)) {
            ctx.refresh();
            assertTrue(ctx.getBeansOfType(U01U02NonProductionHandoffService.class).isEmpty());
        }
    }

    @Test void missingIndependentTraceAuthorityFailsClosed() {
        try (AnnotationConfigApplicationContext ctx = context(true, false)) {
            ctx.refresh();
            assertTrue(ctx.getBeansOfType(U01U02NonProductionHandoffService.class).isEmpty());
        }
    }

    @Test void attestedHostOnlyCanInstallHandoff() {
        try (AnnotationConfigApplicationContext ctx = context(true, true)) {
            ctx.refresh();
            assertEquals(1, ctx.getBeansOfType(U01U02NonProductionHandoffService.class).size());
        }
    }

    @Test void productionProfileNeverInstallsHandoff() {
        try (AnnotationConfigApplicationContext ctx = context(true, true)) {
            ctx.getEnvironment().setActiveProfiles("production");
            ctx.refresh();
            assertTrue(ctx.getBeansOfType(U01U02NonProductionHandoffService.class).isEmpty());
        }
    }
}
