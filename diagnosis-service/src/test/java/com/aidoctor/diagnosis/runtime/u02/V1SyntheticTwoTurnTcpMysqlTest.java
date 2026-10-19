package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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

/** Disposable MySQL and actual random-port HTTP. Synthetic fixtures are not clinical authority. */
@EnabledIfEnvironmentVariable(named="V1_U01_U02_MYSQL_MODE",matches="DISPOSABLE_U01_U02_ONLY")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes=V1SyntheticTwoTurnTcpMysqlTest.Host.class)
@ActiveProfiles("clinical-e2e-test")
@TestPropertySource(properties={
        "aidoctor.v1.clinical-e2e.first-round-wait-http-enabled=true",
        "aidoctor.v1.clinical-e2e.synthetic-two-turn-fixture=true",
        "aidoctor.v1.clinical-e2e.answer-resume-http-enabled=true",
        "aidoctor.v1.clinical-e2e.answer-risk-http-enabled=true",
        "aidoctor.v1.clinical-e2e.synthetic-question-content-enabled=true",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "springfox.documentation.enabled=false",
        "spring.flyway.enabled=false"})
class V1SyntheticTwoTurnTcpMysqlTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,RedisAutoConfiguration.class})
    @Import({V1NonProductionRealTcpMysqlStartTest.Host.class,
            V1SyntheticTwoTurnMysqlHost.class,V1SyntheticManualReadinessController.class})
    static class Host {}

    @Autowired TestRestTemplate http;
    @Autowired CDPRepository cdps;
    @Autowired RuntimeWaitCheckpointRepository checkpoints;
    @Autowired JdbcTemplate jdbc;

    @Test void actualTcpHttpWaitAndResumedRiskPersistToMySql() {
        ResponseEntity<Map> ready=http.getForEntity(
                "/internal/nonproduction/v1/clinical/runtime-readiness",Map.class);
        assertEquals(HttpStatus.OK,ready.getStatusCode());
        assertEquals("COMPONENTS_PRESENT_NOT_E2E_VERIFIED",ready.getBody().get("status"));
        String id=UUID.randomUUID().toString();
        Map<String,String> start=new LinkedHashMap<>();
        start.put("eventId","test-two-turn-event-"+id);
        start.put("idempotencyKey","test-two-turn-idem-"+id);
        start.put("userId","synthetic-two-turn");
        start.put("rawText","synthetic headache uncertainty");
        ResponseEntity<Map> first=http.postForEntity(
                "/internal/nonproduction/v1/clinical/start-through-wait",start,Map.class);
        assertEquals(HttpStatus.OK,first.getStatusCode(),String.valueOf(first.getBody()));
        Map<?,?> body=first.getBody();
        assertNotNull(body);
        String cdpId=(String)body.get("cdpId");
        String checkpointId=(String)body.get("checkpointId");
        assertNotNull(body.get("questionText"));
        assertNotNull(checkpointId);
        assertTrue(checkpoints.findById(checkpointId).isPresent());
        CDP waiting=cdps.findById(cdpId).orElseThrow(IllegalStateException::new);
        assertTrue(waiting.getPatientState().containsKey("pending_question"));
        int before=waiting.getVersion();
        String consultationId=(String)body.get("consultationId");
        int runsBefore=jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinical_runtime_run WHERE consultation_id=?",
                Integer.class,consultationId);
        int eventsBefore=jdbc.queryForObject(
                "SELECT COUNT(*) FROM canonical_business_event WHERE consultation_id=?",
                Integer.class,consultationId);
        Map<String,String> wrongConsultation=new LinkedHashMap<>();
        wrongConsultation.put("consultationId","missing-consultation-"+id);
        wrongConsultation.put("checkpointId",checkpointId);
        wrongConsultation.put("answerText","synthetic invalid answer");
        assertNotEquals(HttpStatus.OK,http.postForEntity(
                "/internal/nonproduction/v1/clinical/answer-through-u04",
                wrongConsultation,Map.class).getStatusCode());
        Map<String,String> wrongCheckpoint=new LinkedHashMap<>();
        wrongCheckpoint.put("consultationId",consultationId);
        wrongCheckpoint.put("checkpointId","missing-checkpoint-"+id);
        wrongCheckpoint.put("answerText","synthetic invalid answer");
        assertNotEquals(HttpStatus.OK,http.postForEntity(
                "/internal/nonproduction/v1/clinical/answer-through-u04",
                wrongCheckpoint,Map.class).getStatusCode());
        assertEquals("ACTIVE",checkpoints.findById(checkpointId)
                .orElseThrow(IllegalStateException::new).getCheckpointStatus());
        assertEquals(before,cdps.findById(cdpId)
                .orElseThrow(IllegalStateException::new).getVersion().intValue());
        assertEquals(runsBefore,jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinical_runtime_run WHERE consultation_id=?",
                Integer.class,consultationId).intValue());
        assertEquals(eventsBefore,jdbc.queryForObject(
                "SELECT COUNT(*) FROM canonical_business_event WHERE consultation_id=?",
                Integer.class,consultationId).intValue());

        Map<String,String> answer=new LinkedHashMap<>();
        answer.put("consultationId",(String)body.get("consultationId"));
        answer.put("checkpointId",checkpointId);
        answer.put("answerText","synthetic severe difficulty breathing");
        ResponseEntity<Map> next=http.postForEntity(
                "/internal/nonproduction/v1/clinical/answer-through-u04",answer,Map.class);
        assertEquals(HttpStatus.OK,next.getStatusCode(),String.valueOf(next.getBody()));
        assertEquals("F8_U02_U03_U04_COMMITTED_NEXT_QUESTION_NOT_DELIVERED",
                next.getBody().get("status"));
        CDP resumed=cdps.findById(cdpId).orElseThrow(IllegalStateException::new);
        assertTrue(resumed.getVersion()>before);
        assertTrue(resumed.getPatientState().containsKey("current_risk_assessment"));
        assertEquals("CONSUMED",checkpoints.findById(checkpointId)
                .orElseThrow(IllegalStateException::new).getCheckpointStatus());
        int committed=resumed.getVersion();
        ResponseEntity<Map> replay=http.postForEntity(
                "/internal/nonproduction/v1/clinical/answer-through-u04",answer,Map.class);
        assertNotEquals(HttpStatus.OK,replay.getStatusCode(),
                "Consumed checkpoint must reject replay");
        assertEquals(committed,cdps.findById(cdpId)
                .orElseThrow(IllegalStateException::new).getVersion().intValue());
    }
}
