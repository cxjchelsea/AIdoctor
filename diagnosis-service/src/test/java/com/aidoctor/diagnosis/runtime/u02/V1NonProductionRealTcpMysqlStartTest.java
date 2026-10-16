package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.diagnosis.client.C01U02CapabilityClient;
import com.aidoctor.diagnosis.controller.V1NonProductionClinicalStartHandoffController;
import com.aidoctor.diagnosis.controller.V1NonProductionClinicalSafetyHandoffController;
import com.aidoctor.diagnosis.runtime.u03.*;
import com.aidoctor.diagnosis.runtime.u04.*;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionGovernedRiskSafetyContinuation;
import com.aidoctor.diagnosis.dto.capability.c01.C01U01CapabilityResponse;
import com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.repository.CDPVersionRepository;
import com.aidoctor.diagnosis.runtime.foundation.*;
import com.aidoctor.diagnosis.runtime.foundation.adapters.ClinicalCdpStateRepositoryAdapter;
import com.aidoctor.diagnosis.runtime.governance.*;
import com.aidoctor.diagnosis.runtime.u01.*;
import com.aidoctor.diagnosis.runtime.u01.capability.C01U01CapabilityGateway;
import com.aidoctor.diagnosis.runtime.u02.capability.C01U02CapabilityGateway;
import com.aidoctor.diagnosis.runtime.u02.d05.U02DependencyInvalidationHook;
import com.aidoctor.diagnosis.service.cdp.*;
import com.aidoctor.diagnosis.state.committer.*;
import com.aidoctor.diagnosis.state.committer.fakes.*;
import com.aidoctor.diagnosis.state.committer.ports.*;
import com.aidoctor.diagnosis.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Actual random-port TCP HTTP -> Spring application -> disposable MySQL/JPA U01
 * and U02/P01 readback. ALL clinical facts and capability approvals are explicit
 * synthetic fixtures. This proves neither U03/U04 nor question delivery/F8.
 */
@EnabledIfEnvironmentVariable(named = "V1_U01_U02_MYSQL_MODE", matches = "DISPOSABLE_U01_U02_ONLY")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes=V1NonProductionRealTcpMysqlStartTest.Host.class)
@ActiveProfiles("clinical-e2e-test")
@TestPropertySource(properties={
        "aidoctor.v1.clinical-e2e.u01-u02-http-enabled=true",
        "aidoctor.v1.clinical-e2e.u01-u04-http-enabled=true",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "springfox.documentation.enabled=false",
        "spring.flyway.enabled=false"})
public class V1NonProductionRealTcpMysqlStartTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,RedisAutoConfiguration.class})
    @Import({U01U02PhysicalMysqlHandoffTest.DatabaseConfig.class,
            V1NonProductionClinicalStartHandoffController.class,
            V1NonProductionClinicalSafetyHandoffController.class})
    static class Host {
        @Bean JsonUtil jsonUtil() { return new JsonUtil(); }
        @Bean CDPVersionService versions(CDPVersionRepository repository) {
            CDPVersionService versions=new CDPVersionService();
            ReflectionTestUtils.setField(versions,"cdpVersionRepository",repository);
            ReflectionTestUtils.setField(versions,"jsonUtil",new JsonUtil());
            return versions;
        }
        @Bean CDPManager cdpManager(CDPRepository cdps,CDPVersionService versions) {
            CDPManager manager=new CDPManager();
            ReflectionTestUtils.setField(manager,"cdpRepository",cdps);
            ReflectionTestUtils.setField(manager,"cdpVersionService",versions);
            return manager;
        }
        @Bean U01NaturalLanguageStartService startService(ConsultationRepository consultations,
                CanonicalBusinessEventRepository events,RuntimeBindingService bindings,
                ClinicalRunRepository runs,CDPManager manager) {
            C01U01CapabilityGateway gateway=mock(C01U01CapabilityGateway.class);
            when(gateway.interpret(anyString(),anyString(),anyString(),isNull()))
                    .thenReturn(startCandidate());
            U01ConsultationService business=new U01ConsultationService(
                    consultations,new U01SemanticPolicy(),new CanonicalBusinessEventLedger(events),
                    bindings,new ClinicalRunCoordinator(bindings,events,runs),manager);
            return new U01NaturalLanguageStartService(consultations,gateway,business);
        }
        @Bean StateCommitter syntheticPhysicalCommitter(
                CDPRepository cdps,CDPVersionService versions) {
            Clock clock=Clock.fixed(Instant.parse("2026-10-14T00:00:00Z"),ZoneOffset.UTC);
            return new StateCommitter(
                    new ClinicalCdpStateRepositoryAdapter(cdps,versions,new ObjectMapper()),
                    (id,v)->CapabilityPolicyPort.CapabilityDecision.authorized(),
                    (path,id)->FieldPermissionPort.FieldPermissionDecision.authorized(),
                    (id,cap)->ConsentPolicyPort.ConsentDecision.authorized(),
                    source->SourceValidationPort.SourceDecision.authorized(),
                    new InMemoryIdempotencyFake(),new SyntheticAuditPortFake(clock),
                    new RecordingCommitEventEvidenceFake(),clock);
        }
        @Bean U07NonProductionGovernedRiskSafetyContinuation syntheticRiskSafety(
                CDPRepository cdps,CapabilityCallTraceRepository traces,
                StateCommitter committer,org.springframework.transaction.support.TransactionTemplate tx) {
            U04NonProductionApplicationService u04=new U04NonProductionApplicationService(
                    new U04AdmissionService(),new U04SafetyGatePolicy(),
                    new U04StateProposalFactory(),new U04CommitService(committer));
            U02U03GovernedSafetyBridge safety=new U02U03GovernedSafetyBridge(
                    cdps,traces,(consultation,cdp,version,call)->
                            new U03AcceptedEvidenceBinding(
                                    "synthetic-acceptance",version,Arrays.asList(
                                    "EV-RF-RESP-001","EV-MNM-NEURO-001","EV-MNM-NEURO-002",
                                    "EV-MNM-CARD-001","EV-RF-ALLERGY-001","EV-RF-APPEAR-001",
                                    "EV-RF-NEURO-001","EV-VS-SEPSIS-001","EV-VS-SEPSIS-002",
                                    "EV-VS-SEPSIS-003","EV-RF-SEPSIS-001"),
                                    Collections.singletonList("synthetic-source"),
                                    Collections.singletonList("synthetic-provenance")),
                    new U03NonProductionAdmissionService(),u04);
            CapabilityBindingRecord binding=new CapabilityBindingRecord(
                    U03GovernedCandidateGateway.BINDING_ID,"C02","1.0.0",
                    "synthetic-risk-capset","aidoctor-v1-scope","contracts-v1",
                    "*","*","*","*",CapabilityBindingRecord.ACTIVE,
                    LocalDateTime.of(2026,1,1,0,0),null,LocalDateTime.of(2026,1,1,0,0));
            CapabilityInvocationGuard riskGuard=mock(CapabilityInvocationGuard.class);
            when(riskGuard.authorize(eq(U03GovernedCandidateGateway.BINDING_ID),eq("C02"),any()))
                    .thenReturn(binding);
            U03GateCClinicalInputPort input=(cmd,accepted)->U03GateCClinicalInput.builder().build();
            U03GovernedCandidateGateway gateway=new U03GovernedCandidateGateway(
                    riskGuard,new U03ReleaseRegistry(),
                    new U03GateCFrozenRuleEvaluator(input),true);
            return new U07NonProductionGovernedRiskSafetyContinuation(
                    gateway,new U03DecisionService(new U03GateCFrozenDecisionPort()),
                    new U03StateProposalFactory(),new U03CommitService(committer),
                    new U03PostCommitFinalizer(
                            new U03RuntimeTraceService(record->{}),new U03OutboundProducer()),
                    new U03OutboundProducer(),safety,tx);
        }
        @Bean V1NonProductionClinicalSafetyHandoffController.AuthorizedClinicalHost
                syntheticClinicalAuthority(RuntimeBindingService bindings,
                    ClinicalRunRepository runs) {
            return (consultationId,runId,fact)->{
                ClinicalRunRecord run=runs.findById(runId)
                        .orElseThrow(IllegalStateException::new);
                U03ExecutionCommand command=new U03ExecutionCommand(
                        consultationId,bindings.requireBinding(consultationId).getThreadId(),
                        runId,run.getEventId(),bindings.requireBinding(consultationId).getCdpId(),
                        fact.commitResult.committedVersion.intValue(),
                        "synthetic-correlation","synthetic-trace");
                return new V1NonProductionClinicalSafetyHandoffController.AuthorizedExecution(
                        command,U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                        "ci-nonprod-safety",U04ScopeContext.current(true,true));
            };
        }
        @Bean U01U02NonProductionHandoffService trustedHandoff(
                U01U02TrustedStartContext context,CapabilityCallTraceRepository traces,
                StateCommitter committer) {
            CapabilityBindingRecord authorized=new CapabilityBindingRecord(
                    C01U02CapabilityGateway.BINDING_ID,"C01","fixture-c01-u02-1",
                    "aidoctor-v1-u02","aidoctor-v1-scope","contracts-v1",
                    "*","*","*","*",CapabilityBindingRecord.ACTIVE,
                    LocalDateTime.of(2026,1,1,0,0),null,LocalDateTime.of(2026,1,1,0,0));
            CapabilityInvocationGuard guard=mock(CapabilityInvocationGuard.class);
            when(guard.authorize(eq(C01U02CapabilityGateway.BINDING_ID),eq("C01"),any()))
                    .thenReturn(authorized);
            C01U02CapabilityClient client=mock(C01U02CapabilityClient.class);
            when(client.interpret(any())).thenReturn(factCandidate());
            C01U02CapabilityGateway gateway=new C01U02CapabilityGateway(client,guard,true);
            U02ClinicalFactApplicationService facts=new U02ClinicalFactApplicationService(
                    gateway,new U02ClinicalFactBusinessOwner(),new U02ClinicalFactProposalFactory(),
                    new U02ClinicalFactCommitService(committer),
                    new U02DependencyInvalidationHook(),new CapabilityTraceService(traces));
            return new U01U02NonProductionHandoffService(context,
                    (consultation,run,event)->
                            new U01U02NonProductionHandoffService.TraceIdentity(
                                    "synthetic-correlation","synthetic-trace"),facts);
        }
    }
    @Autowired TestRestTemplate http;
    @Autowired CDPRepository cdps;
    @Autowired CDPVersionRepository versions;

    @Test void trueTcpHttpCommitsClinicalFactToMysqlAndReturnsRealRun() {
        Assumptions.assumeTrue("DISPOSABLE_U01_U02_ONLY".equals(
                System.getenv("V1_U01_U02_MYSQL_MODE")));
        String id=UUID.randomUUID().toString();
        Map<String,String> request=new LinkedHashMap<>();
        request.put("eventId","tcp-e-"+id);
        request.put("idempotencyKey","tcp-i-"+id);
        request.put("userId","synthetic-tcp-user");
        request.put("rawText","synthetic headache uncertainty");
        ResponseEntity<Map> response=http.postForEntity(
                "/internal/nonproduction/v1/clinical/start-through-u02",
                request,Map.class);
        assertEquals(HttpStatus.OK,response.getStatusCode(),
                String.valueOf(response.getBody()));
        Map<?,?> body=response.getBody();
        assertNotNull(body);
        assertEquals("U02_COMMITTED_DOWNSTREAM_NOT_EXECUTED",body.get("entryStatus"));
        assertEquals(Boolean.FALSE,body.get("patientDeliveryAllowed"));
        String cdpId=(String)body.get("cdpId");
        assertNotNull(cdpId);
        assertNotNull(body.get("runId"));
        CDP cdp=cdps.findById(cdpId).orElseThrow(IllegalStateException::new);
        assertEquals(2,cdp.getVersion().intValue());
        assertEquals(2,versions.findByCdpIdOrderByVersionNumberDesc(cdpId).size());
        assertTrue(cdp.getPatientState().containsKey("clinical_fact_fixture-obs"));
    }

    @Test void trueTcpHttpCommitsRiskAndSafetyToMysql() {
        Assumptions.assumeTrue("DISPOSABLE_U01_U02_ONLY".equals(
                System.getenv("V1_U01_U02_MYSQL_MODE")));
        String id=UUID.randomUUID().toString();
        Map<String,String> request=new LinkedHashMap<>();
        request.put("eventId","tcp-u04-event-"+id);
        request.put("idempotencyKey","tcp-u04-idem-"+id);
        request.put("userId","synthetic-risk-user");
        request.put("rawText","synthetic headache uncertainty");
        ResponseEntity<Map> response=http.postForEntity(
                "/internal/nonproduction/v1/clinical/start-through-u04",request,Map.class);
        assertEquals(HttpStatus.OK,response.getStatusCode(),
                String.valueOf(response.getBody()));
        Map<?,?> body=response.getBody();
        assertNotNull(body);
        assertEquals("U04_COMMITTED_DOWNSTREAM_NOT_EXECUTED",body.get("entryStatus"));
        String cdpId=(String)body.get("cdpId");
        CDP cdp=cdps.findById(cdpId).orElseThrow(IllegalStateException::new);
        assertEquals(4,cdp.getVersion().intValue());
        assertEquals(4,versions.findByCdpIdOrderByVersionNumberDesc(cdpId).size());
        assertTrue(cdp.getPatientState().containsKey("current_risk_assessment"));
        assertTrue(cdp.getPatientState().containsKey("safety_gate"));
        assertEquals(Boolean.FALSE,body.get("patientDeliveryAllowed"));
    }

    private static C01U01CapabilityResponse startCandidate() {
        C01U01CapabilityResponse response=new C01U01CapabilityResponse();
        response.setBusinessStatus("SUCCESS");
        C01U01CapabilityResponse.SubjectCandidate subject=
                new C01U01CapabilityResponse.SubjectCandidate();
        subject.setSubjectType("SELF");response.setSubjectCandidate(subject);
        C01U01CapabilityResponse.ProblemCandidate problem=
                new C01U01CapabilityResponse.ProblemCandidate();
        problem.setText("synthetic headache uncertainty");response.setProblemCandidate(problem);
        C01U01CapabilityResponse.ScopeCandidate scope=
                new C01U01CapabilityResponse.ScopeCandidate();
        scope.setScope("SYMPTOM");response.setScopeCandidate(scope);
        C01U01CapabilityResponse.EarlySafetySignalCandidate signal=
                new C01U01CapabilityResponse.EarlySafetySignalCandidate();
        signal.setDetected(false);response.setEarlySafetySignalCandidate(signal);
        return response;
    }
    private static C01U02CapabilityResponse factCandidate() {
        C01U02CapabilityResponse response=new C01U02CapabilityResponse();
        response.setBusinessStatus("SUCCESS");
        response.setSourceAttribution("USER_TEXT");
        C01U02CapabilityResponse.BindingRef binding=new C01U02CapabilityResponse.BindingRef();
        binding.setBindingId(C01U02CapabilityGateway.BINDING_ID);
        binding.setBindingStatus(CapabilityBindingRecord.ACTIVE);
        binding.setCapabilityId("C01");binding.setCapabilityVersion("fixture-c01-u02-1");
        binding.setCapabilitySetVersion("aidoctor-v1-u02");
        binding.setScopeVersion("aidoctor-v1-scope");
        binding.setContractVersion("contracts-v1");response.setBindingRef(binding);
        C01U02CapabilityResponse.ObservationCandidate fact=
                new C01U02CapabilityResponse.ObservationCandidate();
        fact.setObservationId("fixture-obs");fact.setConceptId("TEST:HEADACHE");
        fact.setConceptDisplay("synthetic headache");
        fact.setRawTextRef("sha256:synthetic-evidence-fixture");
        fact.setValueSemantics("UNKNOWN");fact.setSourceType("PATIENT_REPORTED");
        fact.setLifecycle("UNCERTAIN");fact.setConfidenceOrUncertainty(0.7d);
        fact.setProvenance(Collections.singletonList("synthetic-only"));
        fact.setAmbiguityFlags(Collections.emptyList());
        fact.setContradictionRefs(Collections.emptyList());
        response.setObservationCandidates(Collections.singletonList(fact));
        return response;
    }
}
