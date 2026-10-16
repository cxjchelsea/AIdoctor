package com.aidoctor.diagnosis.controller;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class V1NonProductionFirstRoundWaitTest {
    @Test
    void cannotConstructFirstRoundWithoutIndependentClinicalOwners() {
        assertThrows(IllegalArgumentException.class, () ->
                new V1NonProductionFirstRoundWaitController(
                        null, null, null, null, null, null));
    }
    @Test
    void rejectedU01NeverCallsU02OrClinicalFollowup() {
        com.aidoctor.diagnosis.runtime.u01.U01NaturalLanguageStartService u01 =
                mock(com.aidoctor.diagnosis.runtime.u01.U01NaturalLanguageStartService.class);
        com.aidoctor.diagnosis.runtime.u02.U01U02NonProductionHandoffService u02 =
                new com.aidoctor.diagnosis.runtime.u02.U01U02NonProductionHandoffService(
                        mock(com.aidoctor.diagnosis.runtime.u02.U01U02TrustedStartContext.class),
                        mock(com.aidoctor.diagnosis.runtime.u02.U01U02NonProductionHandoffService.TrustedTraceAuthority.class),
                        new com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactApplicationService(
                                mock(com.aidoctor.diagnosis.runtime.u02.capability.C01U02CapabilityGateway.class),
                                mock(com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactBusinessOwner.class),
                                mock(com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactProposalFactory.class),
                                mock(com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactCommitService.class),
                                new com.aidoctor.diagnosis.runtime.u02.d05.U02DependencyInvalidationHook(),
                                mock(com.aidoctor.diagnosis.runtime.governance.CapabilityTraceService.class)));
        com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionGovernedRiskSafetyContinuation risk =
                mock(com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionGovernedRiskSafetyContinuation.class);
        com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalFollowupCoordinator followup =
                mock(com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalFollowupCoordinator.class);
        V1NonProductionClinicalSafetyHandoffController.AuthorizedClinicalHost authority =
                mock(V1NonProductionClinicalSafetyHandoffController.AuthorizedClinicalHost.class);
        V1NonProductionFirstRoundWaitController.FirstRoundOwner owner =
                mock(V1NonProductionFirstRoundWaitController.FirstRoundOwner.class);
        V1NonProductionFirstRoundWaitController controller =
                new V1NonProductionFirstRoundWaitController(
                        u01, u02, risk, followup, authority, owner);
        V1NonProductionFirstRoundWaitController.StartRequest request =
                new V1NonProductionFirstRoundWaitController.StartRequest();
        request.eventId = "synthetic-event";
        request.idempotencyKey = "synthetic-idem";
        request.userId = "synthetic-user";
        request.rawText = "synthetic headache";
        assertThrows(IllegalStateException.class, () -> controller.start(request));
        verifyNoInteractions(risk, followup, authority, owner);
    }

}
