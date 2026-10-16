package com.aidoctor.diagnosis.controller;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.*;

/** Real ephemeral TCP HTTP for the diagnostic endpoint, not clinical E2E. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = V1NonProductionRuntimeTcpHttpTest.TestHost.class)
@ActiveProfiles("clinical-e2e-test")
@TestPropertySource(properties = {
        "aidoctor.v1.clinical-e2e.readiness-diagnostics-enabled=true",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "springfox.documentation.enabled=false"
})
class V1NonProductionRuntimeTcpHttpTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            RedisAutoConfiguration.class
    })
    @Import(V1NonProductionClinicalRuntimeReadinessController.class)
    static class TestHost {}

    @Autowired TestRestTemplate http;

    @Test
    void actualTcpHttpDoesNotExposeUngovernedFirstRoundOrF8() {
        org.springframework.http.ResponseEntity<String> first = http.postForEntity(
                "/internal/nonproduction/v1/clinical/start-through-wait",
                java.util.Collections.singletonMap("rawText", "synthetic only"), String.class);
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, first.getStatusCode());
        org.springframework.http.ResponseEntity<String> answer = http.postForEntity(
                "/internal/nonproduction/v1/clinical/answer-through-u04",
                java.util.Collections.singletonMap("answerText", "synthetic only"), String.class);
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, answer.getStatusCode());
    }

    @Test
    void actualTcpHttpFailsClosedWithoutClinicalAuthorities() {
        Map<?, ?> result = http.getForObject(
                "/internal/nonproduction/v1/clinical/runtime-readiness", Map.class);
        assertNotNull(result);
        assertEquals("MISSING_REQUIRED_COMPONENTS", result.get("status"));
        assertEquals(Boolean.FALSE, result.get("fullClinicalE2eVerified"));
        assertEquals(Boolean.FALSE, result.get("patientDeliveryAllowed"));
        assertTrue(result.get("components") instanceof Map);
        Map<?, ?> components = (Map<?, ?>) result.get("components");
        assertEquals(Boolean.FALSE, components.get("independentClinicalHost"));
        assertEquals(Boolean.FALSE, components.get("u01U04HttpEntry"));
        assertEquals(Boolean.FALSE, components.get("firstRoundHttpEntry"));
        assertEquals(Boolean.FALSE, components.get("firstRoundOwner"));
        assertEquals(Boolean.FALSE, components.get("f8AnswerHttpEntry"));
        assertEquals(Boolean.FALSE, components.get("trustedAnswerHost"));
        assertEquals(Boolean.FALSE, components.get("resumedRiskHttpEntry"));
        assertEquals(Boolean.FALSE, components.get("resumedRiskOwner"));
    }
}
