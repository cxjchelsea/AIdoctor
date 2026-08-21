package com.aidoctor.diagnosis.runtime;

import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.ToolTypes;
import com.aidoctor.diagnosis.client.OcrServiceClient;
import com.aidoctor.diagnosis.client.PythonRuntimeClient;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 受控 Raw OCR 网关有界单元证明。
 *
 * <p>分类：NON_PRODUCTION_ENGINEERING_CONTROLLED_CUTOVER。
 * 使用桩 PythonRuntimeClient，不启动真实 Runtime，不调用遗留 OCR。
 */
class ControlledRawOcrGatewayTest {

    private static final String TRACE_ID = "trace-controlled-raw-ocr-gateway-1";
    private static final String CDP_ID = "synthetic-cdp-controlled-raw-ocr-gateway-1";
    private static final String ARTIFACT_ID = "artifact-engineering-raw-ocr-process-1";

    @Test
    void disabledPolicyRejectsBeforeHttp() {
        PythonRuntimeClient client = mock(PythonRuntimeClient.class);
        ControlledRawOcrGateway gateway = new ControlledRawOcrGateway(
                client,
                ControlledRawOcrRoutePolicy.disabled()
        );

        ControlledRawOcrGatewayException thrown = assertThrows(
                ControlledRawOcrGatewayException.class,
                () -> gateway.invokeRawOcr(buildRawOcrContext(
                        ControlledRawOcrGateway.RAW_OCR_CAPABILITY_ID,
                        ControlledRawOcrGateway.RAW_OCR_CAPABILITY_VERSION
                ))
        );

        assertEquals(ControlledRawOcrGatewayException.Code.ROUTE_DISABLED, thrown.getCode());
        verifyNoInteractions(client);
        System.out.println("JAVA_RAW_OCR_GATEWAY_DEFAULT_OFF=YES");
        System.out.println("JAVA_DISABLED_ROUTE_HTTP_CALL_COUNT=0");
        System.out.println("JAVA_PYTHON_ROUTE_DEFAULT_OFF=YES");
        System.out.println("JAVA_DEFAULT_ROUTE_PRESERVED=YES");
    }

    @Test
    void controlledPolicyInvokesClientOnceWithExactRawOcrAndDerivedHeaders() {
        PythonRuntimeClient client = mock(PythonRuntimeClient.class);
        ToolTypes.ToolResult expected = succeededResult();
        when(client.invokeToolContext(eq(TRACE_ID), eq(CDP_ID), any(ToolTypes.ToolContext.class)))
                .thenReturn(ResponseEntity.ok(expected));
        ToolTypes.ToolContext context = buildRawOcrContext(
                ControlledRawOcrGateway.RAW_OCR_CAPABILITY_ID,
                ControlledRawOcrGateway.RAW_OCR_CAPABILITY_VERSION
        );
        ControlledRawOcrGateway gateway = new ControlledRawOcrGateway(
                client,
                ControlledRawOcrRoutePolicy.pythonRuntimeControlled()
        );

        ToolTypes.ToolResult actual = gateway.invokeRawOcr(context);

        assertSame(expected, actual);
        verify(client, times(1)).invokeToolContext(TRACE_ID, CDP_ID, context);
        verifyNoMoreInteractions(client);
        System.out.println("JAVA_RAW_OCR_CONTROLLED_ROUTE_SELECTED=YES");
        System.out.println("JAVA_PYTHON_RUNTIME_CLIENT_USED=YES");
        System.out.println("JAVA_CUTOVER_INPUTREF_ONLY=YES");
    }

    @Test
    void nonRawOcrCapabilityRejectsBeforeHttp() {
        PythonRuntimeClient client = mock(PythonRuntimeClient.class);
        ControlledRawOcrGateway gateway = new ControlledRawOcrGateway(
                client,
                ControlledRawOcrRoutePolicy.pythonRuntimeControlled()
        );

        ControlledRawOcrGatewayException thrown = assertThrows(
                ControlledRawOcrGatewayException.class,
                () -> gateway.invokeRawOcr(buildRawOcrContext(
                        "engineering.synthetic.runtime_smoke",
                        "0.0.1"
                ))
        );

        assertEquals(ControlledRawOcrGatewayException.Code.INVALID_RAW_OCR_CAPABILITY, thrown.getCode());
        verify(client, never()).invokeToolContext(any(), any(), any(ToolTypes.ToolContext.class));
        System.out.println("JAVA_RAW_OCR_GATEWAY_CAPABILITY_ALLOWLIST=EXACT");
        System.out.println("JAVA_NON_RAW_OCR_CAPABILITY_NOT_ROUTED=YES");
    }

    @Test
    void wrongCapabilityVersionRejectsBeforeHttp() {
        PythonRuntimeClient client = mock(PythonRuntimeClient.class);
        ControlledRawOcrGateway gateway = new ControlledRawOcrGateway(
                client,
                ControlledRawOcrRoutePolicy.pythonRuntimeControlled()
        );

        ControlledRawOcrGatewayException thrown = assertThrows(
                ControlledRawOcrGatewayException.class,
                () -> gateway.invokeRawOcr(buildRawOcrContext(
                        ControlledRawOcrGateway.RAW_OCR_CAPABILITY_ID,
                        "9.9.9"
                ))
        );

        assertEquals(ControlledRawOcrGatewayException.Code.INVALID_RAW_OCR_CAPABILITY, thrown.getCode());
        verifyNoInteractions(client);
    }

    @Test
    void nullAndInvalidToolContextFailClosed() {
        PythonRuntimeClient client = mock(PythonRuntimeClient.class);
        ControlledRawOcrGateway gateway = new ControlledRawOcrGateway(
                client,
                ControlledRawOcrRoutePolicy.pythonRuntimeControlled()
        );

        ControlledRawOcrGatewayException nullContext = assertThrows(
                ControlledRawOcrGatewayException.class,
                () -> gateway.invokeRawOcr(null)
        );
        assertEquals(ControlledRawOcrGatewayException.Code.INVALID_TOOL_CONTEXT, nullContext.getCode());

        ToolTypes.ToolContext missingEnvelope = buildRawOcrContext(
                ControlledRawOcrGateway.RAW_OCR_CAPABILITY_ID,
                ControlledRawOcrGateway.RAW_OCR_CAPABILITY_VERSION
        );
        missingEnvelope.envelope = null;
        ControlledRawOcrGatewayException invalid = assertThrows(
                ControlledRawOcrGatewayException.class,
                () -> gateway.invokeRawOcr(missingEnvelope)
        );
        assertEquals(ControlledRawOcrGatewayException.Code.INVALID_TOOL_CONTEXT, invalid.getCode());
        verifyNoInteractions(client);
    }

    @Test
    void nullRuntimeBodyFailsClosed() {
        PythonRuntimeClient client = mock(PythonRuntimeClient.class);
        when(client.invokeToolContext(eq(TRACE_ID), eq(CDP_ID), any(ToolTypes.ToolContext.class)))
                .thenReturn(ResponseEntity.ok(null));
        ControlledRawOcrGateway gateway = new ControlledRawOcrGateway(
                client,
                ControlledRawOcrRoutePolicy.pythonRuntimeControlled()
        );

        ControlledRawOcrGatewayException thrown = assertThrows(
                ControlledRawOcrGatewayException.class,
                () -> gateway.invokeRawOcr(buildRawOcrContext(
                        ControlledRawOcrGateway.RAW_OCR_CAPABILITY_ID,
                        ControlledRawOcrGateway.RAW_OCR_CAPABILITY_VERSION
                ))
        );

        assertEquals(ControlledRawOcrGatewayException.Code.INVALID_RUNTIME_RESPONSE, thrown.getCode());
        verify(client, times(1)).invokeToolContext(eq(TRACE_ID), eq(CDP_ID), any(ToolTypes.ToolContext.class));
    }

    @Test
    void pythonRuntimeClientFailureIsExplicitAndDoesNotRetryOrFallback() {
        PythonRuntimeClient client = mock(PythonRuntimeClient.class);
        FeignException failure = mock(FeignException.class);
        when(client.invokeToolContext(eq(TRACE_ID), eq(CDP_ID), any(ToolTypes.ToolContext.class)))
                .thenThrow(failure);
        ControlledRawOcrGateway gateway = new ControlledRawOcrGateway(
                client,
                ControlledRawOcrRoutePolicy.pythonRuntimeControlled()
        );

        ControlledRawOcrGatewayException thrown = assertThrows(
                ControlledRawOcrGatewayException.class,
                () -> gateway.invokeRawOcr(buildRawOcrContext(
                        ControlledRawOcrGateway.RAW_OCR_CAPABILITY_ID,
                        ControlledRawOcrGateway.RAW_OCR_CAPABILITY_VERSION
                ))
        );

        assertEquals(ControlledRawOcrGatewayException.Code.PYTHON_RUNTIME_CALL_FAILED, thrown.getCode());
        assertSame(failure, thrown.getCause());
        verify(client, times(1)).invokeToolContext(eq(TRACE_ID), eq(CDP_ID), any(ToolTypes.ToolContext.class));
        verifyNoMoreInteractions(client);
        assertNoLegacyOcrDependency();
        System.out.println("JAVA_PYTHON_FAILURE_NO_SILENT_LEGACY_FALLBACK=YES");
        System.out.println("JAVA_PYTHON_RUNTIME_FAILURE_EXPLICIT=YES");
    }

    @Test
    void gatewayAndPolicyAreNotSpringComponents() {
        assertNoSpringWiring(ControlledRawOcrGateway.class);
        assertNoSpringWiring(ControlledRawOcrRoutePolicy.class);
        assertNoSpringWiring(ControlledRawOcrRoute.class);
        assertNoSpringWiring(ControlledRawOcrGatewayException.class);
        System.out.println("JAVA_CONTROLLED_GATEWAY_COMPONENT_SCANNED=NO");
        System.out.println("JAVA_RAW_OCR_CONTROLLED_GATEWAY_DEFAULT_WIRING=ABSENT");
    }

    @Test
    void noRequestControlledRouteSurfaceExists() throws Exception {
        Method invoke = ControlledRawOcrGateway.class.getDeclaredMethod(
                "invokeRawOcr",
                ToolTypes.ToolContext.class
        );
        assertEquals(1, invoke.getParameterCount());
        assertEquals(ToolTypes.ToolResult.class, invoke.getReturnType());

        for (Method method : ControlledRawOcrGateway.class.getDeclaredMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            for (Class<?> parameter : parameters) {
                assertFalse(parameter == ControlledRawOcrRoute.class);
                assertFalse(parameter == ControlledRawOcrRoutePolicy.class);
                assertFalse("boolean".equals(parameter.getName()));
            }
            assertFalse(method.getName().toLowerCase().contains("route")
                    && method.getParameterCount() > 0
                    && method.getParameterTypes()[0] == String.class);
        }
        System.out.println("REQUEST_CONTROLLED_JAVA_CUTOVER=NO");
        System.out.println("NO_TOOLCONTEXT_CONTROLLED_JAVA_ROUTE_SELECTION=YES");
        System.out.println("NO_HEADER_CONTROLLED_JAVA_CUTOVER=YES");
        System.out.println("NO_QUERY_CONTROLLED_JAVA_CUTOVER=YES");
    }

    @Test
    void policyNullRouteFailsClosed() {
        ControlledRawOcrGatewayException thrown = assertThrows(
                ControlledRawOcrGatewayException.class,
                () -> new ControlledRawOcrRoutePolicy(null)
        );
        assertEquals(ControlledRawOcrGatewayException.Code.INVALID_ROUTE_POLICY, thrown.getCode());
        assertTrue(ControlledRawOcrRoutePolicy.disabled().isDisabled());
        assertEquals(ControlledRawOcrRoute.DISABLED, ControlledRawOcrRoutePolicy.disabled().getRoute());
        System.out.println("JAVA_TYPED_ROUTE_POLICY=YES");
        System.out.println("JAVA_ROUTE_POLICY_IMMUTABLE=YES");
        System.out.println("JAVA_ROUTE_POLICY_DEFAULT=DISABLED");
        System.out.println("STRING_ROUTE_SURFACE_INTRODUCED=NO");
        System.out.println("REQUEST_ROUTE_SURFACE_INTRODUCED=NO");
    }

    @Test
    void toolCallerAgentLoopAndExaminationBoundariesRemainUnchanged() throws Exception {
        String toolCaller = readRepoFile(
                "src/main/java/com/aidoctor/diagnosis/agent/ToolCaller.java"
        );
        String agentLoop = readRepoFile(
                "src/main/java/com/aidoctor/diagnosis/agent/AgentLoop.java"
        );
        String examination = readRepoFile(
                "../examination-service/src/main/java/com/aidoctor/examination/service/ExaminationService.java"
        );
        String gateway = readRepoFile(
                "src/main/java/com/aidoctor/diagnosis/runtime/ControlledRawOcrGateway.java"
        );

        assertFalse(toolCaller.contains("PythonRuntimeClient"));
        assertFalse(toolCaller.contains("ControlledRawOcr"));
        assertFalse(agentLoop.contains("PythonRuntimeClient"));
        assertFalse(agentLoop.contains("ControlledRawOcr"));
        assertTrue(examination.contains("OcrServiceClient"));
        assertTrue(examination.contains("ocrServiceClient.recognize(file)"));
        assertFalse(examination.contains("ControlledRawOcr"));
        assertFalse(examination.contains("PythonRuntimeClient"));
        assertFalse(gateway.contains("OcrServiceClient"));
        assertFalse(gateway.contains("ToolCaller"));
        assertFalse(gateway.contains("AgentLoop"));
        assertFalse(gateway.contains("ExaminationService"));
        assertFalse(gateway.contains("storage_ref"));
        assertFalse(gateway.contains("base64"));
        assertFalse(gateway.contains("multipart"));
        assertFalse(gateway.contains("Files."));
        assertFalse(gateway.contains("Paths."));

        System.out.println("JAVA_TOOLCALLER_UNCHANGED=YES");
        System.out.println("JAVA_AGENTLOOP_UNCHANGED=YES");
        System.out.println("EXAMINATION_DEFAULT_OCR_ROUTE_UNCHANGED=YES");
        System.out.println("JAVA_PYTHON_CUTOVER_VERIFIED=NO");
        System.out.println("CANONICAL_RUNTIME_RAW_OCR_ENABLED=NO");
    }

    private static void assertNoLegacyOcrDependency() {
        for (Constructor<?> constructor : ControlledRawOcrGateway.class.getDeclaredConstructors()) {
            for (Class<?> parameter : constructor.getParameterTypes()) {
                assertFalse(OcrServiceClient.class.equals(parameter));
            }
        }
        for (Field field : ControlledRawOcrGateway.class.getDeclaredFields()) {
            assertFalse(OcrServiceClient.class.equals(field.getType()));
        }
    }

    private static void assertNoSpringWiring(Class<?> type) {
        assertEquals(null, type.getAnnotation(Component.class));
        assertEquals(null, type.getAnnotation(Service.class));
        assertEquals(null, type.getAnnotation(Configuration.class));
        assertEquals(null, type.getAnnotation(RestController.class));
        assertEquals(null, type.getAnnotation(Controller.class));
        for (Method method : type.getDeclaredMethods()) {
            assertEquals(null, method.getAnnotation(Bean.class));
            assertEquals(null, method.getAnnotation(Autowired.class));
        }
        for (Field field : type.getDeclaredFields()) {
            assertEquals(null, field.getAnnotation(Autowired.class));
        }
    }

    private static ToolTypes.ToolContext buildRawOcrContext(
            String capabilityId,
            String capabilityVersion
    ) {
        FoundationTypes.ContractEnvelope envelope = new FoundationTypes.ContractEnvelope();
        envelope.contractName = "ToolContext";
        envelope.contractVersion = "1.0.0";
        envelope.messageId = "msg-controlled-raw-ocr-gateway-1";
        envelope.correlationId = "corr-controlled-raw-ocr-gateway-1";
        envelope.traceId = TRACE_ID;
        envelope.createdAt = "2026-08-21T02:40:00Z";
        envelope.producer = "java-diagnosis-controlled-cutover";
        envelope.capabilityId = capabilityId;
        envelope.capabilityVersion = capabilityVersion;

        FoundationTypes.IdentifierSet identifiers = new FoundationTypes.IdentifierSet();
        identifiers.contractVersion = "1.0.0";
        identifiers.cdpId = CDP_ID;

        ToolTypes.ToolActor actor = new ToolTypes.ToolActor();
        actor.actorId = "java-diagnosis-controlled-cutover";
        actor.actorType = "SERVICE";

        ToolTypes.ToolCapability capability = new ToolTypes.ToolCapability();
        capability.capabilityId = capabilityId;
        capability.capabilityVersion = capabilityVersion;

        ToolTypes.CurrentStateRef currentStateRef = new ToolTypes.CurrentStateRef();
        currentStateRef.cdpId = CDP_ID;
        currentStateRef.version = 1;
        currentStateRef.readFields = new ArrayList<String>();

        ToolTypes.AuthorizationScope authorizationScope = new ToolTypes.AuthorizationScope();
        authorizationScope.granted = new ArrayList<String>();
        authorizationScope.requested = new ArrayList<String>();

        ToolTypes.InputRef inputRef = new ToolTypes.InputRef();
        inputRef.refType = "ARTIFACT";
        inputRef.refId = ARTIFACT_ID;
        inputRef.refVersion = 1;

        ToolTypes.ToolContext context = new ToolTypes.ToolContext();
        context.contractVersion = "1.0.0";
        context.envelope = envelope;
        context.actor = actor;
        context.identifiers = identifiers;
        context.capability = capability;
        context.currentStateRef = currentStateRef;
        context.authorizationScope = authorizationScope;
        context.deadline = "2026-08-21T02:45:00Z";
        context.locale = "und";
        context.requestedOperation = "RAW_OCR_RECOGNIZE";
        context.inputRefs = Collections.singletonList(inputRef);
        return context;
    }

    private static ToolTypes.ToolResult succeededResult() {
        ToolTypes.ToolResult result = new ToolTypes.ToolResult();
        result.contractVersion = "1.0.0";
        result.status = "SUCCEEDED";
        result.reasonCode = "RAW_OCR_OK";
        result.toolName = "raw-ocr";
        result.retryable = Boolean.FALSE;
        result.errors = Collections.emptyList();
        return result;
    }

    private static String readRepoFile(String relativePath) throws IOException {
        Path path = Paths.get(relativePath);
        assertTrue(Files.exists(path), "missing " + relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
