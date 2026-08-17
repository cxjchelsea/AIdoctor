package com.aidoctor.diagnosis.ncclose04;

import com.aidoctor.diagnosis.agent.AgentLoop;
import com.aidoctor.diagnosis.agent.ClinicalAgentBrain;
import com.aidoctor.diagnosis.annotation.TraceExecution;
import com.aidoctor.diagnosis.client.DiagnosisEngineClient;
import com.aidoctor.diagnosis.client.DialogServiceClient;
import com.aidoctor.diagnosis.client.HealthStateAssessmentClient;
import com.aidoctor.diagnosis.controller.DiagnosisController;
import com.aidoctor.diagnosis.dto.request.DiagnosisRequest;
import com.aidoctor.diagnosis.dto.request.UserAnswer;
import com.aidoctor.diagnosis.dto.response.DiagnosisResponse;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.service.DiagnosisOrchestrationService;
import com.aidoctor.diagnosis.service.cdp.CDPManager;
import com.aidoctor.diagnosis.service.orchestration.DiagnosisWorkflowOrchestrator;
import com.aidoctor.diagnosis.service.wellness.WellnessScreeningOrchestrator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NC-CLOSE-WF-01 固定工作流表征化验证。
 * 这些测试断言当前技术行为，不等于临床正确或目标架构通过。
 */
class NcClose04WorkflowVerificationTest {

    private static final String SYNTHETIC_CDP_ID = "cdp-synthetic-nc-close-04";
    private static final String SYNTHETIC_USER_ID = "user-synthetic-nc-close-04";

    private CDPManager cdpManager;
    private HealthStateAssessmentClient healthStateAssessmentClient;
    private DiagnosisWorkflowOrchestrator diagnosisWorkflowOrchestrator;
    private WellnessScreeningOrchestrator wellnessScreeningOrchestrator;
    private DiagnosisOrchestrationService diagnosisOrchestrationService;
    private CDP storedCdp;

    @BeforeEach
    void setUpWorkflowHarness() {
        cdpManager = mock(CDPManager.class);
        healthStateAssessmentClient = mock(HealthStateAssessmentClient.class);
        diagnosisWorkflowOrchestrator = mock(DiagnosisWorkflowOrchestrator.class);
        wellnessScreeningOrchestrator = mock(WellnessScreeningOrchestrator.class);
        diagnosisOrchestrationService = new DiagnosisOrchestrationService();
        ReflectionTestUtils.setField(diagnosisOrchestrationService, "cdpManager", cdpManager);
        ReflectionTestUtils.setField(diagnosisOrchestrationService, "healthStateAssessmentClient", healthStateAssessmentClient);
        ReflectionTestUtils.setField(diagnosisOrchestrationService, "diagnosisWorkflowOrchestrator", diagnosisWorkflowOrchestrator);
        ReflectionTestUtils.setField(diagnosisOrchestrationService, "wellnessScreeningOrchestrator", wellnessScreeningOrchestrator);
        storedCdp = newCdp(SYNTHETIC_CDP_ID);
        when(cdpManager.createCDP(anyString(), anyString())).thenReturn(storedCdp);
        when(cdpManager.getCDPById(anyString())).thenAnswer(invocation -> Optional.of(storedCdp));
        when(cdpManager.updateCDP(anyString(), anyMap())).thenAnswer(invocation -> {
            applyUpdates(storedCdp, invocation.getArgument(1));
            return storedCdp;
        });
    }

    @AfterEach
    void clearTraceContext() {
        com.aidoctor.diagnosis.util.TraceContext.clear();
    }

    @Test
    void currentAuthoritativeHttpPathUsesOrchestrationService_characterization() throws Exception {
        Field orchestrationField = DiagnosisController.class.getDeclaredField("diagnosisOrchestrationService");
        assertEquals(DiagnosisOrchestrationService.class, orchestrationField.getType());
        assertFalse(hasControllerFieldOfType(ClinicalAgentBrain.class));
        assertFalse(hasControllerFieldOfType(AgentLoop.class));
        assertNotNull(DiagnosisController.class.getMethod("startDiagnosis", DiagnosisRequest.class)
            .getAnnotation(PostMapping.class));
        assertNotNull(DiagnosisController.class.getMethod("continueDiagnosis", UserAnswer.class)
            .getAnnotation(PostMapping.class));
        Method wellnessStart = DiagnosisController.class.getMethod("startWellnessScreening", String.class);
        assertNotNull(wellnessStart.getAnnotation(PostMapping.class));
        assertNull(DiagnosisWorkflowOrchestrator.class.getMethod("executeRemainingSteps", CDP.class)
            .getAnnotation(TraceExecution.class));
    }

    @Test
    void tool0UnavailableDefaultsToClinicalTechnicalBranch_characterization() {
        when(healthStateAssessmentClient.assessHealthState(anyMap()))
            .thenThrow(new RuntimeException("SYNTHETIC_TOOL0_UNAVAILABLE"));

        DiagnosisResponse firstRun = diagnosisOrchestrationService.startDiagnosis(newSyntheticRequest());
        DiagnosisResponse secondRun = diagnosisOrchestrationService.startDiagnosis(newSyntheticRequest());

        assertEquals("clinical_mode_collecting", firstRun.getStatus());
        assertEquals("clinical_mode", firstRun.getWorkMode());
        assertEquals("clinical_mode_collecting", storedCdp.getCdpStatus());
        assertEquals(Boolean.TRUE, storedCdp.getHealthStateAssessment().get("needsClinicalMode"));
        assertEquals("clinical_mode_collecting", secondRun.getStatus());
        assertEquals("clinical_mode", secondRun.getWorkMode());
        verify(wellnessScreeningOrchestrator, never()).executeWellnessScreening(any(CDP.class));
    }

    @Test
    void emptyTool0MapDefaultsNeedsClinicalModeTrue_characterization() {
        when(healthStateAssessmentClient.assessHealthState(anyMap())).thenReturn(new HashMap<String, Object>());

        DiagnosisResponse response = diagnosisOrchestrationService.startDiagnosis(newSyntheticRequest());

        assertEquals("clinical_mode_collecting", response.getStatus());
        assertEquals("clinical_mode", response.getWorkMode());
    }

    @Test
    void wellnessWorkModeDefersScreeningAndWrongModeRejects_characterization() {
        Map<String, Object> wellnessData = new HashMap<String, Object>();
        wellnessData.put("workMode", "wellness_mode");
        wellnessData.put("needsClinicalMode", Boolean.FALSE);
        Map<String, Object> wellnessAssessment = new HashMap<String, Object>();
        wellnessAssessment.put("code", Integer.valueOf(200));
        wellnessAssessment.put("data", wellnessData);
        when(healthStateAssessmentClient.assessHealthState(anyMap())).thenReturn(wellnessAssessment);

        DiagnosisResponse startResponse = diagnosisOrchestrationService.startDiagnosis(newSyntheticRequest());
        assertEquals("wellness_mode_pending", startResponse.getStatus());
        assertEquals("wellness_mode", startResponse.getWorkMode());
        verify(wellnessScreeningOrchestrator, never()).executeWellnessScreening(any(CDP.class));

        Map<String, Object> clinicalAssessment = new HashMap<String, Object>(storedCdp.getHealthStateAssessment());
        clinicalAssessment.put("workMode", "clinical_mode");
        storedCdp.setHealthStateAssessment(clinicalAssessment);
        RuntimeException rejected = assertThrows(RuntimeException.class,
            () -> diagnosisOrchestrationService.startWellnessScreening(SYNTHETIC_CDP_ID));
        assertTrue(rejected.getMessage().contains("健康筛查流程执行失败"));
        verify(wellnessScreeningOrchestrator, never()).executeWellnessScreening(any(CDP.class));
    }

    @Test
    void wellnessA1FailureSurfacesHardFailure_characterization() {
        Map<String, Object> wellnessAssessment = new HashMap<String, Object>();
        wellnessAssessment.put("workMode", "wellness_mode");
        storedCdp.setHealthStateAssessment(wellnessAssessment);
        when(wellnessScreeningOrchestrator.executeWellnessScreening(any(CDP.class)))
            .thenThrow(new RuntimeException("A1需求分类失败"));

        RuntimeException failure = assertThrows(RuntimeException.class,
            () -> diagnosisOrchestrationService.startWellnessScreening(SYNTHETIC_CDP_ID));
        assertTrue(failure.getMessage().contains("健康筛查流程执行失败"));
    }

    @Test
    void completenessGateIsTechnicalThreshold_characterization() {
        storedCdp.setCdpStatus("clinical_mode_collecting");
        storedCdp.setPatientState(new HashMap<String, Object>());
        when(diagnosisWorkflowOrchestrator.step1IdentifyProblem(any(CDP.class))).thenAnswer(invocation -> {
            CDP currentCdp = invocation.getArgument(0);
            Map<String, Object> patientState = new HashMap<String, Object>(currentCdp.getPatientState());
            patientState.put("completeness", Double.valueOf(59.9));
            currentCdp.setPatientState(patientState);
            return currentCdp;
        });

        UserAnswer firstAnswer = new UserAnswer();
        firstAnswer.setCdpId(SYNTHETIC_CDP_ID);
        firstAnswer.setQuestionId("q-synthetic-04");
        firstAnswer.setAnswer("SYNTHETIC_ANSWER_BELOW_GATE");
        DiagnosisResponse belowGate = diagnosisOrchestrationService.continueDiagnosis(firstAnswer);
        assertEquals("clinical_mode_collecting", belowGate.getStatus());
        verify(diagnosisWorkflowOrchestrator, never()).executeRemainingSteps(any(CDP.class));

        when(diagnosisWorkflowOrchestrator.step1IdentifyProblem(any(CDP.class))).thenAnswer(invocation -> {
            CDP currentCdp = invocation.getArgument(0);
            Map<String, Object> patientState = new HashMap<String, Object>(currentCdp.getPatientState());
            patientState.put("completeness", Double.valueOf(60.0));
            currentCdp.setPatientState(patientState);
            return currentCdp;
        });
        when(diagnosisWorkflowOrchestrator.executeRemainingSteps(any(CDP.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAnswer secondAnswer = new UserAnswer();
        secondAnswer.setCdpId(SYNTHETIC_CDP_ID);
        secondAnswer.setQuestionId("q-synthetic-04-gate");
        secondAnswer.setAnswer("SYNTHETIC_ANSWER_AT_GATE");
        DiagnosisResponse atGate = diagnosisOrchestrationService.continueDiagnosis(secondAnswer);
        assertEquals("completed", atGate.getStatus());
        verify(diagnosisWorkflowOrchestrator, times(1)).executeRemainingSteps(any(CDP.class));
    }

    @Test
    void step3OrganizeFailureIsHardFail_characterization() {
        DiagnosisWorkflowOrchestrator realOrchestrator = newWiredOrchestrator();
        DiagnosisEngineClient diagnosisEngineClient = (DiagnosisEngineClient) ReflectionTestUtils.getField(realOrchestrator, "diagnosisEngineClient");
        when(diagnosisEngineClient.generateDDxCandidates(anyMap())).thenReturn(new HashMap<String, Object>());
        when(diagnosisEngineClient.organizeReasoningGroups(anyMap()))
            .thenThrow(new RuntimeException("SYNTHETIC_STEP3_ORGANIZE_UNAVAILABLE"));

        RuntimeException failure = assertThrows(RuntimeException.class,
            () -> realOrchestrator.executeRemainingSteps(storedCdp));
        assertTrue(failure.getMessage().contains("Step 3执行失败"));
        assertEquals("error", storedCdp.getCdpStatus());
    }

    @Test
    void step4EvidenceFailureIsHardFail_characterization() {
        DiagnosisWorkflowOrchestrator realOrchestrator = newWiredOrchestrator();
        DiagnosisEngineClient diagnosisEngineClient = (DiagnosisEngineClient) ReflectionTestUtils.getField(realOrchestrator, "diagnosisEngineClient");
        DialogServiceClient dialogServiceClient = (DialogServiceClient) ReflectionTestUtils.getField(realOrchestrator, "dialogServiceClient");
        when(diagnosisEngineClient.generateDDxCandidates(anyMap())).thenReturn(new HashMap<String, Object>());
        when(diagnosisEngineClient.organizeReasoningGroups(anyMap())).thenReturn(new HashMap<String, Object>());
        when(dialogServiceClient.designRoutingPath(anyMap())).thenReturn(new HashMap<String, Object>());
        when(dialogServiceClient.collectKeyEvidence(anyMap()))
            .thenThrow(new RuntimeException("SYNTHETIC_STEP4_EVIDENCE_UNAVAILABLE"));

        RuntimeException failure = assertThrows(RuntimeException.class,
            () -> realOrchestrator.executeRemainingSteps(storedCdp));
        assertTrue(failure.getMessage().contains("Step 4执行失败"));
        assertEquals("error", storedCdp.getCdpStatus());
    }

    @Test
    void step2DdxFailureSoftContinuesThenHitsStep3_characterization() {
        DiagnosisWorkflowOrchestrator realOrchestrator = newWiredOrchestrator();
        DiagnosisEngineClient diagnosisEngineClient = (DiagnosisEngineClient) ReflectionTestUtils.getField(realOrchestrator, "diagnosisEngineClient");
        when(diagnosisEngineClient.generateDDxCandidates(anyMap()))
            .thenThrow(new RuntimeException("SYNTHETIC_STEP2_DDX_UNAVAILABLE"));
        when(diagnosisEngineClient.organizeReasoningGroups(anyMap()))
            .thenThrow(new RuntimeException("SYNTHETIC_STEP3_AFTER_SOFT_STEP2"));

        RuntimeException failure = assertThrows(RuntimeException.class,
            () -> realOrchestrator.executeRemainingSteps(storedCdp));
        assertTrue(failure.getMessage().contains("Step 3执行失败"));
        verify(diagnosisEngineClient, times(1)).generateDDxCandidates(anyMap());
        verify(diagnosisEngineClient, times(1)).organizeReasoningGroups(anyMap());
    }

    @Test
    void doubleRunTool0UnavailableStableTechnicalFields_characterization() {
        when(healthStateAssessmentClient.assessHealthState(anyMap()))
            .thenThrow(new RuntimeException("SYNTHETIC_TOOL0_UNAVAILABLE"));
        AtomicInteger createCount = new AtomicInteger();
        when(cdpManager.createCDP(anyString(), anyString())).thenAnswer(invocation -> {
            createCount.incrementAndGet();
            storedCdp = newCdp(SYNTHETIC_CDP_ID + "-" + createCount.get());
            return storedCdp;
        });

        DiagnosisResponse firstRun = diagnosisOrchestrationService.startDiagnosis(newSyntheticRequest());
        DiagnosisResponse secondRun = diagnosisOrchestrationService.startDiagnosis(newSyntheticRequest());

        assertEquals(firstRun.getStatus(), secondRun.getStatus());
        assertEquals(firstRun.getWorkMode(), secondRun.getWorkMode());
        assertEquals(firstRun.getCurrentStep(), secondRun.getCurrentStep());
        assertEquals("clinical_mode_collecting", firstRun.getStatus());
    }

    private DiagnosisWorkflowOrchestrator newWiredOrchestrator() {
        DiagnosisWorkflowOrchestrator orchestrator = new DiagnosisWorkflowOrchestrator();
        ReflectionTestUtils.setField(orchestrator, "cdpManager", cdpManager);
        ReflectionTestUtils.setField(orchestrator, "clinicalParsingClient", mock(com.aidoctor.diagnosis.client.ClinicalParsingClient.class));
        ReflectionTestUtils.setField(orchestrator, "dialogServiceClient", mock(DialogServiceClient.class));
        ReflectionTestUtils.setField(orchestrator, "diagnosisEngineClient", mock(DiagnosisEngineClient.class));
        ReflectionTestUtils.setField(orchestrator, "workupPlannerClient", mock(com.aidoctor.diagnosis.client.WorkupPlannerClient.class));
        ReflectionTestUtils.setField(orchestrator, "treatmentEngineClient", mock(com.aidoctor.diagnosis.client.TreatmentEngineClient.class));
        ReflectionTestUtils.setField(orchestrator, "riskAssessmentClient", mock(com.aidoctor.diagnosis.client.RiskAssessmentClient.class));
        ReflectionTestUtils.setField(orchestrator, "explanationServiceClient", mock(com.aidoctor.diagnosis.client.ExplanationServiceClient.class));
        return orchestrator;
    }

    private static boolean hasControllerFieldOfType(Class<?> fieldType) {
        Field[] fields = DiagnosisController.class.getDeclaredFields();
        for (int index = 0; index < fields.length; index++) {
            if (fields[index].getType().equals(fieldType)) {
                return true;
            }
        }
        return false;
    }

    private static DiagnosisRequest newSyntheticRequest() {
        DiagnosisRequest request = new DiagnosisRequest();
        request.setUserId(SYNTHETIC_USER_ID);
        request.setDiagnosisType("symptom");
        request.setUserInput("SYNTHETIC_NON_PHI_INPUT_04");
        return request;
    }

    private static CDP newCdp(String cdpId) {
        CDP cdp = new CDP();
        cdp.setId(cdpId);
        cdp.setPatientId(SYNTHETIC_USER_ID);
        cdp.setSessionId("session-synthetic-nc-close-04");
        cdp.setVersion(1);
        cdp.setCdpStatus("initial");
        cdp.setPatientState(new HashMap<String, Object>());
        cdp.setHealthStateAssessment(new HashMap<String, Object>());
        cdp.setDdx(new ArrayList<Map<String, Object>>());
        return cdp;
    }

    @SuppressWarnings("unchecked")
    private static void applyUpdates(CDP targetCdp, Map<String, Object> updates) {
        if (updates.containsKey("cdpStatus")) {
            targetCdp.setCdpStatus((String) updates.get("cdpStatus"));
        }
        if (updates.containsKey("healthStateAssessment")) {
            targetCdp.setHealthStateAssessment((Map<String, Object>) updates.get("healthStateAssessment"));
        }
        if (updates.containsKey("patientState")) {
            targetCdp.setPatientState((Map<String, Object>) updates.get("patientState"));
        }
        if (updates.containsKey("ddx")) {
            targetCdp.setDdx((java.util.List<Map<String, Object>>) updates.get("ddx"));
        }
        if (updates.containsKey("evidenceGraph")) {
            targetCdp.setEvidenceGraph((java.util.List<Map<String, Object>>) updates.get("evidenceGraph"));
        }
    }
}
