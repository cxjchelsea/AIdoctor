package com.aidoctor.diagnosis.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.*;

/** Actual TCP HTTP must not expose nonproduction diagnostics in production. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = V1NonProductionRuntimeTcpProductionIsolationTest.TestHost.class)
@ActiveProfiles("production")
@TestPropertySource(properties = {
        "aidoctor.v1.clinical-e2e.readiness-diagnostics-enabled=true",
        "springfox.documentation.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false"
})
class V1NonProductionRuntimeTcpProductionIsolationTest {
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
    void productionProfileRejectsDiagnosticEndpointOverTcp() {
        ResponseEntity<String> response = http.getForEntity(
                "/internal/nonproduction/v1/clinical/runtime-readiness", String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
