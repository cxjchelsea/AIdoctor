package com.aidoctor.diagnosis.controller;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class V1NonProductionClinicalRuntimeReadinessTest {
    @Test
    void missingIndependentOwnersDoNotReportOperationalReadiness() {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBeanNamesForType(any(Class.class), eq(false), eq(false)))
                .thenReturn(new String[0]);
        Map<String, Object> response =
                new V1NonProductionClinicalRuntimeReadinessController(context).readiness();
        assertEquals("MISSING_REQUIRED_COMPONENTS", response.get("status"));
        assertEquals(Boolean.FALSE, response.get("fullClinicalE2eVerified"));
        assertEquals(Boolean.FALSE, response.get("patientDeliveryAllowed"));
        @SuppressWarnings("unchecked")
        Map<String, Boolean> components = (Map<String, Boolean>) response.get("components");
        assertEquals(Boolean.FALSE, components.get("independentClinicalEvidenceAttestation"));
        assertEquals(Boolean.FALSE, components.get("verifiedGateCScopeAuthority"));
        assertEquals(Boolean.FALSE, components.get("trustedTraceAuthority"));
        assertEquals(Boolean.FALSE, components.get("independentClinicalHost"));
    }

    @Test
    void beanPresenceNeverOverstatesEndToEndClinicalEvidence() {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBeanNamesForType(any(Class.class), eq(false), eq(false)))
                .thenReturn(new String[] {"synthetic-test-bean"});
        Map<String, Object> response =
                new V1NonProductionClinicalRuntimeReadinessController(context).readiness();
        assertEquals("COMPONENTS_PRESENT_NOT_E2E_VERIFIED", response.get("status"));
        assertEquals(Boolean.FALSE, response.get("fullClinicalE2eVerified"));
        assertEquals(Boolean.FALSE, response.get("patientDeliveryAllowed"));
    }
}
