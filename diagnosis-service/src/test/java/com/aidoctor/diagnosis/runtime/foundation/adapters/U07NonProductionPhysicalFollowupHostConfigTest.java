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
    @Test void firstRoundRequiresPersistedRunBeforeRiskCommit() {
        com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingService bindings =
                org.mockito.Mockito.mock(
                        com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingService.class);
        com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRepository runs =
                org.mockito.Mockito.mock(
                        com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRepository.class);
        com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingRecord binding =
                new com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingRecord(
                        "consult", "cdp",
                        com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingRecord.CLINICAL_RUNTIME_V1,
                        "thread", "scope", "capabilities", "contracts-v1", null);
        com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRecord run =
                new com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRecord(
                        "run", "thread", "consult", "event", 1, null);
        com.aidoctor.diagnosis.runtime.u03.U03ExecutionCommand command =
                new com.aidoctor.diagnosis.runtime.u03.U03ExecutionCommand(
                        "consult", "thread", "run", "event", "cdp", 2, "corr", "trace");
        org.mockito.Mockito.when(bindings.requireBinding("consult")).thenReturn(binding);
        org.mockito.Mockito.when(runs.findById("run")).thenReturn(java.util.Optional.of(run));
        assertDoesNotThrow(() ->
                U07NonProductionPhysicalFollowupCoordinator.requireDurableRuntimeForCommandPreRisk(
                        command, bindings, runs, "thread", "run"));
        assertTrue(assertThrows(IllegalStateException.class, () ->
                U07NonProductionPhysicalFollowupCoordinator.requireDurableRuntimeForCommandPreRisk(
                        command, bindings, runs, "wrong-thread", "run"))
                .getMessage().contains("PRE_RISK_DURABLE_AUTHORITY_MISMATCH"));
        assertTrue(assertThrows(IllegalStateException.class, () ->
                U07NonProductionPhysicalFollowupCoordinator.requireDurableRuntimeForCommandPreRisk(
                        command, bindings, runs, "thread", "old-run"))
                .getMessage().contains("PRE_RISK_DURABLE_AUTHORITY_MISMATCH"));
        org.mockito.Mockito.when(runs.findById("run")).thenReturn(java.util.Optional.empty());
        assertTrue(assertThrows(IllegalStateException.class, () ->
                U07NonProductionPhysicalFollowupCoordinator.requireDurableRuntimeForCommandPreRisk(
                        command, bindings, runs, "thread", "run"))
                .getMessage().contains("PRE_RISK_RUN_NOT_FOUND"));
    }

    @Test void missingIndependentDeliveryOwnerCannotStartFirstRound() {
        U07NonProductionF3A1U05Flow f3 =
                org.mockito.Mockito.mock(U07NonProductionF3A1U05Flow.class);
        com.aidoctor.diagnosis.runtime.u06.U05QuestionRouteAdmissionAdapter admission =
                org.mockito.Mockito.mock(
                        com.aidoctor.diagnosis.runtime.u06.U05QuestionRouteAdmissionAdapter.class);
        com.aidoctor.diagnosis.runtime.u06.U06NonProductionPhysicalMode23Service u06 =
                org.mockito.Mockito.mock(
                        com.aidoctor.diagnosis.runtime.u06.U06NonProductionPhysicalMode23Service.class);
        com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery recovery =
                org.mockito.Mockito.mock(
                        com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery.class);
        com.aidoctor.diagnosis.repository.CDPRepository cdps =
                org.mockito.Mockito.mock(com.aidoctor.diagnosis.repository.CDPRepository.class);
        com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingService bindings =
                org.mockito.Mockito.mock(
                        com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingService.class);
        com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRepository runs =
                org.mockito.Mockito.mock(
                        com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRepository.class);
        org.springframework.transaction.support.TransactionTemplate tx =
                org.mockito.Mockito.mock(
                        org.springframework.transaction.support.TransactionTemplate.class);
        U07NonProductionPhysicalFollowupCoordinator.AuthorizedQuestionHost host =
                org.mockito.Mockito.mock(
                        U07NonProductionPhysicalFollowupCoordinator.AuthorizedQuestionHost.class);
        U07NonProductionPhysicalFollowupCoordinator coordinator =
                new U07NonProductionPhysicalFollowupCoordinator(
                        f3, admission, u06, recovery, cdps, bindings, runs, tx, host);
        assertTrue(assertThrows(IllegalStateException.class, () ->
                coordinator.firstRoundToDurableWait(
                        null, null, null, "ci-nonprod", null, "state",
                        null, null)).getMessage()
                .contains("INDEPENDENT_DELIVERY_OWNER_REQUIRED"));
        org.mockito.Mockito.verifyNoInteractions(
                f3, admission, u06, recovery, cdps, bindings, runs, tx, host);
    }

}
