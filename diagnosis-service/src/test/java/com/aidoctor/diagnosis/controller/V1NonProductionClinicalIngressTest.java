package com.aidoctor.diagnosis.controller;

import com.aidoctor.diagnosis.runtime.u01.U01NaturalLanguageStartService;
import com.aidoctor.diagnosis.runtime.u01.U01RawStartCommand;
import com.aidoctor.diagnosis.runtime.u01.U01Result;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Safe contract tests for genuine U01 service delegation. This does not claim
 * a real CDP database, C01 provider or subsequent medical units executed.
 */
class V1NonProductionClinicalIngressTest {

    @Test
    void actualU01IngressReturnsOnlyCommittedU01FactsNotFabricatedClinicalConclusion() throws Exception {
        U01NaturalLanguageStartService u01 = mock(U01NaturalLanguageStartService.class);
        U01Result u01Result = mock(U01Result.class);
        when(u01Result.getConsultationId()).thenReturn("consult_synthetic");
        when(u01Result.getCdpId()).thenReturn("cdp_synthetic");
        when(u01Result.getRunId()).thenReturn("run_synthetic");
        when(u01Result.getNextUnit()).thenReturn("U02");
        when(u01Result.getLifecycleStatus()).thenReturn("ACTIVE");
        when(u01.start(any(U01RawStartCommand.class))).thenReturn(u01Result);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new V1NonProductionClinicalIngressController(u01)).build();

        mvc.perform(post("/internal/nonproduction/v1/clinical/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventId\":\"synthetic-start-1\","
                        + "\"idempotencyKey\":\"synthetic-idem-1\","
                        + "\"userId\":\"synthetic-user\","
                        + "\"rawText\":\"synthetic patient complaint\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultationId").value("consult_synthetic"))
                .andExpect(jsonPath("$.cdpId").value("cdp_synthetic"))
                .andExpect(jsonPath("$.runId").value("run_synthetic"))
                .andExpect(jsonPath("$.nextUnit").value("U02"))
                .andExpect(jsonPath("$.entryStatus").value("U01_COMMITTED_DOWNSTREAM_NOT_EXECUTED"))
                .andExpect(jsonPath("$.completeClinicalE2e").value(false))
                .andExpect(jsonPath("$.patientDeliveryAllowed").value(false));
        verify(u01).start(argThat(command ->
                "synthetic-start-1".equals(command.getEventId())
                && "synthetic-idem-1".equals(command.getIdempotencyKey())
                && "synthetic-user".equals(command.getUserId())
                && "synthetic patient complaint".equals(command.getRawText())));
        verifyNoMoreInteractions(u01);
    }

    @Test
    void malformedStartIsRejectedBeforeU01() throws Exception {
        U01NaturalLanguageStartService u01 = mock(U01NaturalLanguageStartService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new V1NonProductionClinicalIngressController(u01)).build();
        mvc.perform(post("/internal/nonproduction/v1/clinical/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"synthetic-user\",\"rawText\":\"hello\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(u01);
    }

    @Test
    void springProductionOrUnconfiguredHostNeverRegistersEndpoint() {
        try (AnnotationConfigApplicationContext disabled = new AnnotationConfigApplicationContext()) {
            disabled.register(V1NonProductionClinicalIngressController.class);
            disabled.refresh();
            assertTrue(disabled.getBeansOfType(
                    V1NonProductionClinicalIngressController.class).isEmpty());
        }
        try (AnnotationConfigApplicationContext unbound = new AnnotationConfigApplicationContext()) {
            unbound.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("u01-flag",
                            Collections.<String,Object>singletonMap(
                                    "aidoctor.v1.clinical-e2e.nonproduction-entry-enabled","true")));
            unbound.register(V1NonProductionClinicalIngressController.class);
            unbound.refresh();
            assertTrue(unbound.getBeansOfType(
                    V1NonProductionClinicalIngressController.class).isEmpty());
        }
        try (AnnotationConfigApplicationContext production = new AnnotationConfigApplicationContext()) {
            production.getEnvironment().setActiveProfiles("prod");
            production.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("u01-prod-flag",
                            Collections.<String,Object>singletonMap(
                                    "aidoctor.v1.clinical-e2e.nonproduction-entry-enabled","true")));
            production.registerBean(U01NaturalLanguageStartService.class,
                    () -> mock(U01NaturalLanguageStartService.class));
            production.register(V1NonProductionClinicalIngressController.class);
            production.refresh();
            assertTrue(production.getBeansOfType(
                    V1NonProductionClinicalIngressController.class).isEmpty());
        }
    }
}
