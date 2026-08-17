package com.aidoctor.diagnosis.ncclose04;

import com.aidoctor.diagnosis.annotation.TraceExecution;
import com.aidoctor.diagnosis.aspect.ExecutionTraceAspect;
import com.aidoctor.diagnosis.client.TraceServiceClient;
import com.aidoctor.diagnosis.config.FeignTraceInterceptor;
import com.aidoctor.diagnosis.dto.trace.ExecutionTraceEvent;
import com.aidoctor.diagnosis.entity.AuditTrail;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.service.audit.AuditTrailManager;
import com.aidoctor.diagnosis.service.orchestration.DiagnosisWorkflowOrchestrator;
import com.aidoctor.diagnosis.service.wellness.WellnessScreeningOrchestrator;
import com.aidoctor.diagnosis.util.TraceContext;
import feign.RequestTemplate;
import feign.Target;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NC-CLOSE-TRACE-01 追踪边界表征化验证。
 * 测试通过只表示当前行为被复现，不等于目标隔离/OTel 要求通过。
 */
class NcClose04TraceVerificationTest {

    private static final String SYNTHETIC_CDP_ID = "cdp-synthetic-trace-04";
    private static final String SYNTHETIC_PATIENT_CANARY = "SYNTHETIC_PATIENT_CANARY_04";
    private static final String SYNTHETIC_PROMPT_CANARY = "SYNTHETIC_PROMPT_CANARY_04";
    private static final String SYNTHETIC_RESPONSE_CANARY = "SYNTHETIC_RESPONSE_CANARY_04";

    @AfterEach
    void clearTraceContext() {
        TraceContext.clear();
    }

    @Test
    void authoritativePathAnnotationCoverage_characterization() throws Exception {
        assertNotNull(DiagnosisWorkflowOrchestrator.class
            .getMethod("step1IdentifyProblem", CDP.class)
            .getAnnotation(TraceExecution.class));
        assertNotNull(DiagnosisWorkflowOrchestrator.class
            .getMethod("executeDiagnosisWorkflow", CDP.class)
            .getAnnotation(TraceExecution.class));
        assertNull(DiagnosisWorkflowOrchestrator.class
            .getMethod("executeRemainingSteps", CDP.class)
            .getAnnotation(TraceExecution.class),
            "当前权威 Steps 2-5 入口 executeRemainingSteps 未标注 @TraceExecution");
        assertNotNull(WellnessScreeningOrchestrator.class
            .getMethod("executeWellnessScreening", CDP.class)
            .getAnnotation(TraceExecution.class));
    }

    @Test
    void executionTraceEnabledDefaultsOffWhenMissing_characterization() {
        ConditionalOnProperty aspectGate = ExecutionTraceAspect.class.getAnnotation(ConditionalOnProperty.class);
        ConditionalOnProperty clientGate = TraceServiceClient.class.getAnnotation(ConditionalOnProperty.class);
        ConditionalOnProperty feignGate = FeignTraceInterceptor.class.getAnnotation(ConditionalOnProperty.class);
        assertEquals("execution.trace.enabled", aspectGate.name()[0]);
        assertEquals("true", aspectGate.havingValue());
        assertFalse(aspectGate.matchIfMissing());
        assertEquals("execution.trace.enabled", clientGate.name()[0]);
        assertFalse(clientGate.matchIfMissing());
        assertEquals("execution.trace.enabled", feignGate.name()[0]);
        assertFalse(feignGate.matchIfMissing());
    }

    @Test
    void traceServiceClientHttpFailureIsIsolated_characterization() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForObject(anyString(), any(), eq(Void.class)))
            .thenThrow(new RestClientException("SYNTHETIC_TRACE_HTTP_UNAVAILABLE"));
        TraceServiceClient traceServiceClient = new TraceServiceClient(restTemplate, "http://127.0.0.1:9");
        ExecutionTraceEvent event = ExecutionTraceEvent.builder()
            .cdpId(SYNTHETIC_CDP_ID)
            .type("SERVICE_CALL_START")
            .build();
        traceServiceClient.recordEvent(event);
        verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(Void.class));
    }

    @Test
    void aspectStartRecordFailureCurrentlyBlocksBusinessInvocation_characterization() throws Throwable {
        ExecutionTraceAspect aspect = new ExecutionTraceAspect();
        TraceServiceClient throwingClient = mock(TraceServiceClient.class);
        doThrow(new RuntimeException("SYNTHETIC_TRACE_START_THROW")).when(throwingClient).recordEvent(any(ExecutionTraceEvent.class));
        ReflectionTestUtils.setField(aspect, "traceServiceClient", throwingClient);
        TraceContext.setCdpId(SYNTHETIC_CDP_ID);

        ProceedingJoinPoint joinPoint = newAnnotatedJoinPoint("businessShouldNotRun");
        RuntimeException failure = assertThrows(RuntimeException.class, () -> invokeAspect(aspect, joinPoint));
        assertEquals("SYNTHETIC_TRACE_START_THROW", failure.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void aspectEndRecordFailureCurrentlyConvertsSuccessToFailure_characterization() throws Throwable {
        ExecutionTraceAspect aspect = new ExecutionTraceAspect();
        TraceServiceClient throwingOnEndClient = mock(TraceServiceClient.class);
        doThrow(new RuntimeException("SYNTHETIC_TRACE_END_THROW"))
            .when(throwingOnEndClient).recordEvent(any(ExecutionTraceEvent.class));
        ReflectionTestUtils.setField(aspect, "traceServiceClient", throwingOnEndClient);
        TraceContext.setCdpId(SYNTHETIC_CDP_ID);

        ProceedingJoinPoint joinPoint = newAnnotatedJoinPoint("businessAlreadySucceeded");
        when(joinPoint.proceed()).thenReturn("SYNTHETIC_SUCCESS");
        // 第一次 recordEvent 是 START，放行；第二次是 END，抛出
        org.mockito.Mockito.doNothing().doThrow(new RuntimeException("SYNTHETIC_TRACE_END_THROW"))
            .when(throwingOnEndClient).recordEvent(any(ExecutionTraceEvent.class));

        RuntimeException failure = assertThrows(RuntimeException.class, () -> invokeAspect(aspect, joinPoint));
        assertEquals("SYNTHETIC_TRACE_END_THROW", failure.getMessage());
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void nullTraceContextSkipsAndProceeds_characterization() throws Throwable {
        ExecutionTraceAspect aspect = new ExecutionTraceAspect();
        TraceServiceClient traceServiceClient = mock(TraceServiceClient.class);
        ReflectionTestUtils.setField(aspect, "traceServiceClient", traceServiceClient);
        TraceContext.clear();
        ProceedingJoinPoint joinPoint = newAnnotatedJoinPoint("skippedWhenNoCdp");
        when(joinPoint.proceed()).thenReturn("PROCEEDED");

        Object result = aspect.trace(joinPoint);
        assertEquals("PROCEEDED", result);
        verify(traceServiceClient, never()).recordEvent(any(ExecutionTraceEvent.class));
    }

    @Test
    void feignRecordFailureCurrentlyAbortsApply_characterization() {
        FeignTraceInterceptor interceptor = new FeignTraceInterceptor();
        TraceServiceClient throwingClient = mock(TraceServiceClient.class);
        doThrow(new RuntimeException("SYNTHETIC_FEIGN_TRACE_THROW"))
            .when(throwingClient).recordEvent(any(ExecutionTraceEvent.class));
        ReflectionTestUtils.setField(interceptor, "traceServiceClient", throwingClient);
        TraceContext.setCdpId(SYNTHETIC_CDP_ID);
        RequestTemplate template = newRequestTemplate();

        RuntimeException failure = assertThrows(RuntimeException.class, () -> interceptor.apply(template));
        assertEquals("SYNTHETIC_FEIGN_TRACE_THROW", failure.getMessage());
        assertTrue(headerValues(template, "X-CDP-Id").contains(SYNTHETIC_CDP_ID));
        assertTrue(headerValues(template, "X-Trace-Id").isEmpty());
    }

    @Test
    void feignSetsCdpAndTraceHeadersAndRegeneratesTraceId_characterization() {
        FeignTraceInterceptor interceptor = new FeignTraceInterceptor();
        TraceServiceClient traceServiceClient = mock(TraceServiceClient.class);
        ReflectionTestUtils.setField(interceptor, "traceServiceClient", traceServiceClient);
        TraceContext.setCdpId(SYNTHETIC_CDP_ID);

        RequestTemplate firstTemplate = newRequestTemplate();
        firstTemplate.header("X-Trace-Id", "caller-provided-trace");
        interceptor.apply(firstTemplate);
        assertTrue(headerValues(firstTemplate, "X-CDP-Id").contains(SYNTHETIC_CDP_ID));
        assertFalse(headerValues(firstTemplate, "X-Trace-Id").isEmpty());
        assertTrue(headerValues(firstTemplate, "X-Service-Name").contains("health-state-assessment-service"));
        assertFalse(headerValues(firstTemplate, "X-Start-Time").isEmpty());

        RequestTemplate secondTemplate = newRequestTemplate();
        interceptor.apply(secondTemplate);
        String firstTraceId = firstNonEmpty(headerValues(firstTemplate, "X-Trace-Id"));
        String secondTraceId = firstNonEmpty(headerValues(secondTemplate, "X-Trace-Id"));
        assertNotEquals(firstTraceId, secondTraceId);
    }

    @Test
    void feignNullContextSkipsHeaders_characterization() {
        FeignTraceInterceptor interceptor = new FeignTraceInterceptor();
        ReflectionTestUtils.setField(interceptor, "traceServiceClient", mock(TraceServiceClient.class));
        TraceContext.clear();
        RequestTemplate template = newRequestTemplate();
        interceptor.apply(template);
        assertTrue(headerValues(template, "X-CDP-Id").isEmpty());
        assertTrue(headerValues(template, "X-Trace-Id").isEmpty());
    }

    @Test
    void traceContextSetGetClearAndNoCrossThreadPropagation_characterization() throws Exception {
        TraceContext.setCdpId(SYNTHETIC_CDP_ID);
        assertEquals(SYNTHETIC_CDP_ID, TraceContext.getCdpId());
        TraceContext.clear();
        assertNull(TraceContext.getCdpId());

        TraceContext.setCdpId(SYNTHETIC_CDP_ID);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final AtomicReference<String> workerValue = new AtomicReference<String>("UNSET");
            final CountDownLatch done = new CountDownLatch(1);
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    workerValue.set(TraceContext.getCdpId());
                    done.countDown();
                }
            });
            assertTrue(done.await(3, TimeUnit.SECONDS));
            assertNull(workerValue.get());
        } finally {
            executor.shutdownNow();
            TraceContext.clear();
        }
    }

    @Test
    void defaultEmittedMetadataExcludesRawCanaryPayload_characterization() throws Throwable {
        ExecutionTraceAspect aspect = new ExecutionTraceAspect();
        TraceServiceClient traceServiceClient = mock(TraceServiceClient.class);
        ReflectionTestUtils.setField(aspect, "traceServiceClient", traceServiceClient);
        TraceContext.setCdpId(SYNTHETIC_CDP_ID);
        ProceedingJoinPoint joinPoint = newAnnotatedJoinPoint("sanitizeCanary");
        when(joinPoint.getArgs()).thenReturn(new Object[]{SYNTHETIC_PATIENT_CANARY, SYNTHETIC_PROMPT_CANARY});
        when(joinPoint.proceed()).thenReturn(SYNTHETIC_RESPONSE_CANARY);

        aspect.trace(joinPoint);

        ArgumentCaptor<ExecutionTraceEvent> eventCaptor = ArgumentCaptor.forClass(ExecutionTraceEvent.class);
        verify(traceServiceClient, times(2)).recordEvent(eventCaptor.capture());
        ExecutionTraceEvent startEvent = eventCaptor.getAllValues().get(0);
        ExecutionTraceEvent endEvent = eventCaptor.getAllValues().get(1);
        String startInput = String.valueOf(startEvent.getInput());
        String endOutput = String.valueOf(endEvent.getOutput());
        assertFalse(startInput.contains(SYNTHETIC_PATIENT_CANARY));
        assertFalse(startInput.contains(SYNTHETIC_PROMPT_CANARY));
        assertFalse(endOutput.contains(SYNTHETIC_RESPONSE_CANARY));
        assertTrue(startInput.contains("argCount"));
        assertTrue(endOutput.contains("returnType"));
    }

    @Test
    void errorMessageCurrentlyPersistsRawExceptionText_characterization() throws Throwable {
        ExecutionTraceAspect aspect = new ExecutionTraceAspect();
        TraceServiceClient traceServiceClient = mock(TraceServiceClient.class);
        ReflectionTestUtils.setField(aspect, "traceServiceClient", traceServiceClient);
        TraceContext.setCdpId(SYNTHETIC_CDP_ID);
        ProceedingJoinPoint joinPoint = newAnnotatedJoinPoint("errorCanary");
        when(joinPoint.proceed()).thenThrow(new RuntimeException("SYNTHETIC_PATIENT_CANARY_04 leaked in exception"));

        assertThrows(RuntimeException.class, () -> invokeAspect(aspect, joinPoint));
        ArgumentCaptor<ExecutionTraceEvent> eventCaptor = ArgumentCaptor.forClass(ExecutionTraceEvent.class);
        verify(traceServiceClient, times(2)).recordEvent(eventCaptor.capture());
        ExecutionTraceEvent errorEvent = eventCaptor.getAllValues().get(1);
        assertTrue(errorEvent.getErrorMessage().contains("SYNTHETIC_PATIENT_CANARY_04"));
    }

    @Test
    void eventDtoAndAuditTrailArePayloadCapableLegacyShapes_characterization() throws Exception {
        Field inputField = ExecutionTraceEvent.class.getDeclaredField("input");
        Field outputField = ExecutionTraceEvent.class.getDeclaredField("output");
        Field errorField = ExecutionTraceEvent.class.getDeclaredField("errorMessage");
        assertEquals(Object.class, inputField.getType());
        assertEquals(Object.class, outputField.getType());
        assertEquals(String.class, errorField.getType());

        Method recordToolCall = AuditTrailManager.class.getMethod("recordToolCall", String.class, String.class, Map.class);
        Method recordCdpUpdate = AuditTrailManager.class.getMethod("recordCDPUpdate", String.class, String.class, Map.class);
        Method recordAgentDecision = AuditTrailManager.class.getMethod("recordAgentDecision", String.class, String.class, Map.class);
        assertNotNull(recordToolCall);
        assertNotNull(recordCdpUpdate);
        assertNotNull(recordAgentDecision);
        Field eventType = AuditTrail.class.getDeclaredField("eventType");
        assertEquals(String.class, eventType.getType());
    }

    @Test
    void otelSdkAbsentOnDiagnosisClasspath_characterization() {
        assertThrows(ClassNotFoundException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                Class.forName("io.opentelemetry.api.GlobalOpenTelemetry");
            }
        });
        assertThrows(ClassNotFoundException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                Class.forName("io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter");
            }
        });
    }

    private static void invokeAspect(ExecutionTraceAspect aspect, ProceedingJoinPoint joinPoint) throws Exception {
        try {
            aspect.trace(joinPoint);
        } catch (RuntimeException runtimeException) {
            throw runtimeException;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private static ProceedingJoinPoint newAnnotatedJoinPoint(String methodName) throws Exception {
        Method annotatedMethod = WellnessScreeningOrchestrator.class.getMethod("executeWellnessScreening", CDP.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(annotatedMethod);
        when(signature.getName()).thenReturn(methodName);
        when(signature.getDeclaringType()).thenReturn(WellnessScreeningOrchestrator.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new WellnessScreeningOrchestrator());
        when(joinPoint.getArgs()).thenReturn(new Object[]{new CDP()});
        return joinPoint;
    }

    private static RequestTemplate newRequestTemplate() {
        RequestTemplate template = new RequestTemplate();
        template.feignTarget(new Target.HardCodedTarget<Object>(
            Object.class,
            "health-state-assessment-service",
            "http://127.0.0.1"));
        template.uri("/api/v1/health-state/assess");
        return template;
    }

    private static Collection<String> headerValues(RequestTemplate template, String headerName) {
        Map<String, Collection<String>> headers = template.headers();
        if (headers == null || !headers.containsKey(headerName)) {
            return java.util.Collections.emptyList();
        }
        return headers.get(headerName);
    }

    private static String firstNonEmpty(Collection<String> values) {
        for (String value : values) {
            if (value != null && !value.isEmpty() && !"caller-provided-trace".equals(value)) {
                return value;
            }
        }
        if (values.isEmpty()) {
            return "";
        }
        return values.iterator().next();
    }
}
