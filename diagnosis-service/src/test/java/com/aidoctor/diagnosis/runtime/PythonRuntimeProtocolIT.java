package com.aidoctor.diagnosis.runtime;

import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.ToolTypes;
import com.aidoctor.diagnosis.client.PythonRuntimeClient;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Java Feign → 真实 HTTP → Python Runtime 门面的跨语言协议证明。
 *
 * <p>不启动完整 diagnosis 生产应用，不连接数据库 / Redis / Nacos。
 * 不使用 MockWebServer 或伪造 Feign 目标替代真实套接字。
 */
@SpringBootTest(
        classes = PythonRuntimeProtocolIT.MinimalFeignContext.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@TestPropertySource(properties = {
        "python-runtime.service-url=http://127.0.0.1:8099",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class PythonRuntimeProtocolIT {

    private static final String HEALTH_URL = "http://127.0.0.1:8099/api/v1/runtime/health";
    private static final String GOLDEN_FIXTURE_RELATIVE =
            "packages/python_runtime/tests/fixtures/runtime_protocol_invoke.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    static void waitForLocalRuntimeHealth() throws Exception {
        // 仅用于就绪探测；协议成功路径必须走 PythonRuntimeClient
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
                "Python Runtime HTTP façade is not ready at 127.0.0.1:8099",
                lastError
        );
    }

    @Test
    void validSyntheticInvokeOverRealHttpReturnsSucceededToolResult() throws Exception {
        FoundationTypes.ContractEnvelope envelope = loadGoldenEnvelope();
        assertEquals("1.0.0", envelope.contractVersion);
        assertEquals("engineering.synthetic.runtime_smoke", envelope.capabilityId);

        ResponseEntity<ToolTypes.ToolResult> firstResponse = pythonRuntimeClient.invoke(
                envelope.traceId,
                null,
                envelope
        );
        ResponseEntity<ToolTypes.ToolResult> secondResponse = pythonRuntimeClient.invoke(
                envelope.traceId,
                "synthetic-cdp-protocol-1",
                envelope
        );

        assertEquals(HttpStatus.OK, firstResponse.getStatusCode());
        assertEquals(HttpStatus.OK, secondResponse.getStatusCode());
        ToolTypes.ToolResult firstResult = firstResponse.getBody();
        ToolTypes.ToolResult secondResult = secondResponse.getBody();
        assertNotNull(firstResult);
        assertNotNull(secondResult);
        assertEquals("SUCCEEDED", firstResult.status);
        assertEquals("synthetic-echo", firstResult.toolName);
        assertEquals(Boolean.FALSE, firstResult.retryable);
        assertTrue(firstResult.suggestedPatches == null || firstResult.suggestedPatches.isEmpty());
        assertEquals(firstResult.status, secondResult.status);
        assertEquals(firstResult.toolName, secondResult.toolName);
        assertEquals(firstResult.reasonCode, secondResult.reasonCode);
        assertEquals(envelope.traceId, firstResponse.getHeaders().getFirst("X-Trace-Id"));
        assertEquals("synthetic-cdp-protocol-1", secondResponse.getHeaders().getFirst("X-CDP-Id"));
        assertFalse(OBJECT_MAPPER.writeValueAsString(firstResult).contains("patient_id"));
    }

    @Test
    void wrongContractVersionFailsClosedOverRealHttp() throws Exception {
        FoundationTypes.ContractEnvelope envelope = loadGoldenEnvelope();
        envelope.contractVersion = "2.0.0";

        FeignException thrown = assertThrows(FeignException.class, () ->
                pythonRuntimeClient.invoke(envelope.traceId, null, envelope)
        );

        assertTrue(thrown.status() >= 400 && thrown.status() < 500);
        String errorBody = thrown.contentUTF8();
        assertTrue(errorBody.contains("CONTRACT_VERSION_MISMATCH"));
        assertFalse(errorBody.contains("synthetic-echo"));
        assertFalse(errorBody.contains("SUCCEEDED"));
    }

    private static FoundationTypes.ContractEnvelope loadGoldenEnvelope() throws Exception {
        Path fixturePath = resolveGoldenFixture();
        try (InputStream inputStream = Files.newInputStream(fixturePath)) {
            JsonNode tree = OBJECT_MAPPER.readTree(inputStream);
            assertEquals("1.0.0", tree.get("contract_version").asText());
            assertEquals("engineering.synthetic.runtime_smoke", tree.get("capability_id").asText());
            return OBJECT_MAPPER.treeToValue(tree, FoundationTypes.ContractEnvelope.class);
        }
    }

    private static Path resolveGoldenFixture() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        for (int depth = 0; depth < 8; depth++) {
            Path candidate = current.resolve(GOLDEN_FIXTURE_RELATIVE);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            Path parent = current.getParent();
            if (parent == null) {
                break;
            }
            current = parent;
        }
        throw new IllegalStateException(
                "shared golden fixture not found: " + GOLDEN_FIXTURE_RELATIVE
        );
    }
}
