package com.aidoctor.diagnosis.runtime;

import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.SharedContractsMapper;
import com.aidoctor.contracts.v1.ToolTypes;
import com.aidoctor.diagnosis.client.PythonRuntimeClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Java Feign → 真实 HTTP → 03B-G engineering Raw OCR Runtime process。
 *
 * <p>分类：NON_PRODUCTION_ENGINEERING_JAVA_REAL_TOOL_PROTOCOL_PROOF。
 * 不启动完整 diagnosis 生产应用，不连接数据库 / Redis / Nacos。
 * 不使用 MockWebServer 或伪造 Feign 目标替代真实套接字。
 * 成功路径必须走 {@code PythonRuntimeClient.invokeToolContext}。
 * 进程不可用时必须失败，不得 skip。
 */
@SpringBootTest(
        classes = PythonRuntimeRawOcrRealToolIT.MinimalFeignContext.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@TestPropertySource(properties = {
        "python-runtime.service-url=http://127.0.0.1:8099",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class PythonRuntimeRawOcrRealToolIT {

    private static final String HEALTH_URL = "http://127.0.0.1:8099/api/v1/runtime/health";
    private static final String TRACE_ID = "trace-engineering-raw-ocr-java-1";
    private static final String ARTIFACT_ID = "artifact-engineering-raw-ocr-process-1";
    private static final ObjectMapper OBJECT_MAPPER = SharedContractsMapper.create();

    @Autowired
    private PythonRuntimeClient pythonRuntimeClient;

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            RedisAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @EnableFeignClients(clients = PythonRuntimeClient.class)
    static class MinimalFeignContext {
    }

    @BeforeAll
    static void waitForEngineeringProcessHealth() throws Exception {
        URL healthUrl = new URL(HEALTH_URL);
        long deadlineMillis = System.currentTimeMillis() + 15_000L;
        Exception lastError = null;
        while (System.currentTimeMillis() < deadlineMillis) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) healthUrl.openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.setRequestMethod("GET");
                if (connection.getResponseCode() == 200) {
                    return;
                }
            } catch (Exception exc) {
                lastError = exc;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            Thread.sleep(500L);
        }
        throw new IllegalStateException(
                "Engineering Raw OCR Runtime process is not ready at 127.0.0.1:8099",
                lastError
        );
    }

    @Test
    void invokeToolContextOverRealHttpReturnsSucceededRawOcrToolResult() throws Exception {
        ToolTypes.ToolContext context = buildEngineeringRawOcrToolContext(
                "engineering.ocr.raw",
                "0.0.1",
                TRACE_ID
        );
        assertInputRefOnlyPayload(context);
        System.out.println("JAVA_RAW_OCR_REAL_TOOL_SHARED_TOOLCONTEXT=YES");
        System.out.println("JAVA_RAW_OCR_REAL_TOOL_INPUTREF_ONLY=YES");
        System.out.println("JAVA_RAW_OCR_REAL_TOOL_ENGINEERING_PROCESS=YES");

        ResponseEntity<ToolTypes.ToolResult> response = pythonRuntimeClient.invokeToolContext(
                TRACE_ID,
                "synthetic-cdp-engineering-raw-ocr-java-1",
                context
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ToolTypes.ToolResult result = response.getBody();
        assertNotNull(result);
        assertEquals("1.0.0", result.contractVersion);
        assertEquals("SUCCEEDED", result.status);
        assertEquals("RAW_OCR_OK", result.reasonCode);
        assertEquals("raw-ocr", result.toolName);
        assertEquals(Boolean.FALSE, result.retryable);
        assertTrue(result.errors == null || result.errors.isEmpty());
        assertTrue(result.suggestedPatches == null || result.suggestedPatches.isEmpty());
        assertEquals(TRACE_ID, response.getHeaders().getFirst("X-Trace-Id"));

        String rawText = extractRawText(result);
        String normalized = rawText.toUpperCase().replaceAll("\\s+", " ");
        assertTrue(normalized.contains("HELLO"), "OCR text missing HELLO: " + rawText);
        assertTrue(normalized.contains("OCR"), "OCR text missing OCR: " + rawText);
        assertFalse(OBJECT_MAPPER.writeValueAsString(result).contains("patient_id"));

        System.out.println("JAVA_RAW_OCR_REAL_TOOL_REAL_HTTP=YES");
        System.out.println("JAVA_RAW_OCR_REAL_TOOL_HTTP_STATUS=200");
        System.out.println("JAVA_RAW_OCR_REAL_TOOL_STATUS=SUCCEEDED");
        System.out.println("JAVA_RAW_OCR_REAL_TOOL_REASON_CODE=RAW_OCR_OK");
        System.out.println("JAVA_RAW_OCR_REAL_TOOL_TOOL_NAME=raw-ocr");
        System.out.println("JAVA_RAW_OCR_REAL_TOOL_TOKEN_HELLO=YES");
        System.out.println("JAVA_RAW_OCR_REAL_TOOL_TOKEN_OCR=YES");
        System.out.println("JAVA_RAW_OCR_REAL_TOOL_RESULT_DESERIALIZED=YES");
        System.out.println("JAVA_RAW_OCR_REAL_TOOL_PROTOCOL=PASS");
        System.out.println("JAVA_PYTHON_CUTOVER_VERIFIED=NO");
    }

    @Test
    void unauthorizedCapabilityFailsClosedOverRealHttp() throws Exception {
        ToolTypes.ToolContext context = buildEngineeringRawOcrToolContext(
                "engineering.synthetic.runtime_smoke",
                "0.0.1",
                "trace-engineering-raw-ocr-java-unauthorized-1"
        );

        FeignException thrown = assertThrows(FeignException.class, () ->
                pythonRuntimeClient.invokeToolContext(
                        context.envelope.traceId,
                        null,
                        context
                )
        );

        assertEquals(403, thrown.status());
        assertTrue(thrown.contentUTF8().contains("OPERATION_NOT_AUTHORIZED"));
        assertFalse(thrown.contentUTF8().contains("SUCCEEDED"));
        assertFalse(thrown.contentUTF8().contains("RAW_OCR_OK"));
    }

    private static ToolTypes.ToolContext buildEngineeringRawOcrToolContext(
            String capabilityId,
            String capabilityVersion,
            String traceId
    ) {
        FoundationTypes.ContractEnvelope envelope = new FoundationTypes.ContractEnvelope();
        envelope.contractName = "ToolContext";
        envelope.contractVersion = "1.0.0";
        envelope.messageId = "msg-engineering-raw-ocr-java-1";
        envelope.correlationId = "corr-engineering-raw-ocr-java-1";
        envelope.traceId = traceId;
        envelope.createdAt = "2026-08-20T06:50:00Z";
        envelope.producer = "java-diagnosis-real-tool";
        envelope.capabilityId = capabilityId;
        envelope.capabilityVersion = capabilityVersion;

        FoundationTypes.IdentifierSet identifiers = new FoundationTypes.IdentifierSet();
        identifiers.contractVersion = "1.0.0";
        identifiers.cdpId = "synthetic-cdp-engineering-raw-ocr-java-1";

        ToolTypes.ToolActor actor = new ToolTypes.ToolActor();
        actor.actorId = "java-diagnosis-real-tool";
        actor.actorType = "SERVICE";

        ToolTypes.ToolCapability capability = new ToolTypes.ToolCapability();
        capability.capabilityId = capabilityId;
        capability.capabilityVersion = capabilityVersion;

        ToolTypes.CurrentStateRef currentStateRef = new ToolTypes.CurrentStateRef();
        currentStateRef.cdpId = "synthetic-cdp-engineering-raw-ocr-java-1";
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
        context.deadline = "2026-08-20T06:55:00Z";
        context.locale = "und";
        context.requestedOperation = "RAW_OCR_RECOGNIZE";
        context.inputRefs = Collections.singletonList(inputRef);
        return context;
    }

    private static void assertInputRefOnlyPayload(ToolTypes.ToolContext context) throws Exception {
        assertEquals(1, context.inputRefs.size());
        ToolTypes.InputRef inputRef = context.inputRefs.get(0);
        assertEquals("ARTIFACT", inputRef.refType);
        assertEquals(ARTIFACT_ID, inputRef.refId);
        assertEquals(Integer.valueOf(1), inputRef.refVersion);

        String serialized = OBJECT_MAPPER.writeValueAsString(context);
        assertFalse(serialized.contains("artifact_base64"));
        assertFalse(serialized.contains("storage_ref"));
        assertFalse(serialized.contains("raw_bytes"));
        assertFalse(serialized.contains("base64"));
        assertFalse(serialized.contains(".png"));
        assertFalse(serialized.contains("multipart"));
    }

    private static String extractRawText(ToolTypes.ToolResult result) {
        assertNotNull(result.output);
        for (ToolTypes.NamedValue item : result.output) {
            if ("raw_text".equals(item.name) && item.value != null) {
                return String.valueOf(item.value);
            }
        }
        throw new AssertionError("ToolResult.output missing raw_text");
    }
}
