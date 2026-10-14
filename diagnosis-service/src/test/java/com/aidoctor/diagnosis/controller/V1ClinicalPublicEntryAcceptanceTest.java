package com.aidoctor.diagnosis.controller;

import com.aidoctor.diagnosis.dto.request.DiagnosisRequest;
import com.aidoctor.diagnosis.dto.request.UserAnswer;
import com.aidoctor.diagnosis.dto.response.DiagnosisResponse;
import com.aidoctor.diagnosis.service.DiagnosisOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HTTP ADAPTER BASELINE only, NOT a full clinical E2E pass.
 * Proves the public entry still delegates to the legacy orchestration service.
 * Uses MockMvc, a mocked service, and entirely synthetic requests; does not
 * start the full Spring host, access a database, or invoke a clinical model.
 */
class V1ClinicalPublicEntryAcceptanceTest {
    private DiagnosisOrchestrationService legacy;
    private MockMvc mvc;

    @BeforeEach
    void configure() {
        legacy = mock(DiagnosisOrchestrationService.class);
        DiagnosisController controller = new DiagnosisController();
        ReflectionTestUtils.setField(controller, "diagnosisOrchestrationService", legacy);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void startEndpointStillRoutesToLegacyOrchestrationNotGovernedU01Chain() throws Exception {
        when(legacy.startDiagnosis(any(DiagnosisRequest.class)))
                .thenReturn(DiagnosisResponse.builder()
                        .cdpId("synthetic-cdp-start").status("clinical_mode_collecting").build());
        mvc.perform(post("/api/v1/diagnosis/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"synthetic-user\",\"diagnosisType\":\"symptom\","
                        + "\"userInput\":\"synthetic symptom\",}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(legacy);
        mvc.perform(post("/api/v1/diagnosis/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"synthetic-user\",\"diagnosisType\":\"symptom\","
                        + "\"userInput\":\"synthetic symptom\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cdpId").value("synthetic-cdp-start"));
        verify(legacy, times(1)).startDiagnosis(any(DiagnosisRequest.class));
        verifyNoMoreInteractions(legacy);
    }

    @Test
    void continueEndpointStillRoutesToLegacyOrchestrationNotGovernedF8Resume() throws Exception {
        when(legacy.continueDiagnosis(any(UserAnswer.class)))
                .thenReturn(DiagnosisResponse.builder()
                        .cdpId("synthetic-cdp-resume").status("clinical_mode_collecting").build());
        mvc.perform(post("/api/v1/diagnosis/continue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cdpId\":\"synthetic-cdp-resume\","
                        + "\"questionId\":\"synthetic-question\","
                        + "\"answer\":\"synthetic answer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cdpId").value("synthetic-cdp-resume"));
        verify(legacy, times(1)).continueDiagnosis(any(UserAnswer.class));
        verifyNoMoreInteractions(legacy);
    }

    @Test
    void missingQuestionIdNeverReachesLegacyService() throws Exception {
        mvc.perform(post("/api/v1/diagnosis/continue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cdpId\":\"synthetic-cdp\",\"answer\":\"yes\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(legacy);
    }
}
