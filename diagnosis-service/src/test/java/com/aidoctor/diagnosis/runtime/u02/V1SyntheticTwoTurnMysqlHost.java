package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.client.C01U02CapabilityClient;
import com.aidoctor.diagnosis.controller.*;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.foundation.*;
import com.aidoctor.diagnosis.runtime.foundation.adapters.*;
import com.aidoctor.diagnosis.runtime.u01.*;
import com.aidoctor.diagnosis.runtime.u03.*;
import com.aidoctor.diagnosis.runtime.u04.*;
import com.aidoctor.diagnosis.runtime.u05.*;
import com.aidoctor.diagnosis.runtime.u06.*;
import com.aidoctor.diagnosis.runtime.u06.wait.*;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
import com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort;
import com.aidoctor.diagnosis.service.cdp.CDPVersionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * EXPLICIT disposable-MySQL engineering fixture; never import into the
 * production application. All F3/A1/C01/U05/U06 medical policy and delivery
 * material below is synthetic. The physical persistence services are real.
 */
@Configuration
@Profile("clinical-e2e-test")
@Import(V1SyntheticQuestionContentTestConfig.class)
public class V1SyntheticTwoTurnMysqlHost {
    static final class CurrentRun {
        volatile String consultation, run, thread, cdp;
        void bind(String consultation,String run,String thread,String cdp) {
            this.consultation=consultation;this.run=run;this.thread=thread;this.cdp=cdp;
        }
    }

    @Bean CurrentRun syntheticCurrentRun() { return new CurrentRun(); }

    @Bean @Primary
    V1NonProductionClinicalSafetyHandoffController.AuthorizedClinicalHost
            syntheticCurrentClinicalAuthority(RuntimeBindingService bindings,
                    ClinicalRunRepository runs, CurrentRun current) {
        return (consultationId,runId,fact)->{
            RuntimeBindingRecord binding=bindings.requireBinding(consultationId);
            ClinicalRunRecord run=runs.findById(runId)
                    .orElseThrow(IllegalStateException::new);
            if(!consultationId.equals(run.getConsultationId())
                    || !runId.equals(run.getRunId()))
                throw new IllegalStateException("SYNTHETIC_RUN_NOT_CURRENT");
            current.bind(consultationId,runId,binding.getThreadId(),binding.getCdpId());
            return new V1NonProductionClinicalSafetyHandoffController.AuthorizedExecution(
                    new U03ExecutionCommand(consultationId,binding.getThreadId(),runId,
                            run.getEventId(),binding.getCdpId(),
                            fact.commitResult.committedVersion.intValue(),
                            "synthetic-correlation","synthetic-trace"),
                    U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                    "ci-nonprod-safety",U04ScopeContext.current(true,true));
        };
    }

    @Bean @ConditionalOnMissingBean(JdbcTemplate.class)
    JdbcTemplate physicalJdbc(DataSource ds) { return new JdbcTemplate(ds); }

    @Bean V1NonProductionDeliveredQuestionReadback physicalQuestionReadback(
            RuntimeWaitCheckpointRepository checkpoints,CDPRepository cdps) {
        return new V1NonProductionDeliveredQuestionReadback(checkpoints,cdps);
    }

    @Bean U07DurableNonProductionA1Issuer syntheticA1Issuer(
            CDPRepository cdps,JdbcTemplate jdbc,TransactionTemplate tx) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS u07_a1_attestation ("
                + "consultation_id VARCHAR(64) NOT NULL, cdp_id VARCHAR(64) NOT NULL, "
                + "cdp_version INT NOT NULL, f3_effect_id VARCHAR(128) NOT NULL, "
                + "barrier_ref VARCHAR(128) NOT NULL, revalidation_ref VARCHAR(128) NOT NULL, "
                + "route_source_ref VARCHAR(128) NOT NULL, "
                + "route_authorization_ref VARCHAR(128) NOT NULL, "
                + "routing_policy_version VARCHAR(128) NOT NULL, "
                + "PRIMARY KEY (consultation_id, cdp_id, cdp_version)) ENGINE=InnoDB");
        return new U07DurableNonProductionA1Issuer(cdps,jdbc,tx,
                (consultation,cdpId,version,effect)->{
                    CDP cdp=cdps.findById(cdpId).orElseThrow(IllegalStateException::new);
                    Object assessment=cdp.getPatientState().get("f3_gap_assessment");
                    Object gate=cdp.getPatientState().get("safety_gate");
                    if(cdp.getVersion()!=version || version!=5
                            || !(assessment instanceof Map<?,?>)
                            || !(gate instanceof Map<?,?>)
                            || !effect.equals(((Map<?,?>)assessment).get("f3_canonical_effect_id"))
                            || !"ALLOW".equals(((Map<?,?>)gate).get("safety_gate")))
                        throw new IllegalStateException("SYNTHETIC_A1_READBACK_DENIED");
                    return new U07DurableNonProductionA1Issuer.VerifiedEvidence(
                            effect,"test-only-post-f3-barrier","test-only-current-revalidation",
                            "test-only-u05-route-source","test-only-u05-route-auth",
                            "test-only-routing-policy-v1",true,true,true);
                });
    }

    @Bean U07NonProductionF3A1U05Flow syntheticF3U05(
            CDPRepository cdps,CDPVersionService versions,TransactionTemplate tx,
            U07DurableNonProductionA1Issuer issuer,StateCommitter committer) {
        StateRepositoryPort state=new ClinicalCdpStateRepositoryAdapter(
                cdps,versions,new ObjectMapper());
        U07NonProductionPhysicalF3Owner physical=new U07NonProductionPhysicalF3Owner(
                new U07NonProductionPhysicalF3Owner.F3CommitPort() {
                    @Override public int currentVersion(String cdpId) {
                        return state.readCurrentVersion(cdpId);
                    }
                    @Override public int commitAuthorized(String cdpId,int version,
                            U07NonProductionPhysicalF3Owner.PreparedF3 prepared) {
                        StateRepositoryPort.AtomicCommitOutcome committed=
                                state.attemptAtomicCommit(
                                        new StateRepositoryPort.AtomicCommitCommand(
                                                cdpId,version,prepared.patchId,
                                                prepared.idempotencyKey,prepared.patch));
                        if(committed.status!=StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED)
                            throw new IllegalStateException("SYNTHETIC_F3_CAS_FAILED");
                        return committed.committedVersion;
                    }
                },cdps,(consultation,cdpId,version)->{
                    String effect="synthetic-http-f3-"+consultation;
                    StateTypes.StatePatchOperation op=new StateTypes.StatePatchOperation();
                    op.op="ADD";op.path="/patient_state/f3_gap_assessment";
                    Map<String,Object> payload=new LinkedHashMap<>();
                    payload.put("f3_canonical_effect_id",effect);
                    payload.put("gap_id","synthetic-http-gap");
                    op.value=payload;op.source="PATIENT_FACT";op.sensitivity="PHI";
                    StateTypes.StatePatch patch=new StateTypes.StatePatch();
                    patch.operations=Collections.singletonList(op);
                    return new U07NonProductionPhysicalF3Owner.PreparedF3(effect,
                            "synthetic-http-f3-patch-"+consultation,
                            "synthetic-http-f3-idem-"+consultation,patch);
                });
        U07NonProductionF3A1U05Flow.F3Owner owner=(consultation,cdp)->
                tx.execute(status->physical.executeAndReadback(consultation,cdp));
        U04A1U05AuthorityBridge bridge=new U04A1U05AuthorityBridge(
                new ClinicalCdpGateReadbackAdapter(cdps),
                new ClinicalCdpF3BoundA1AuthorityPort(cdps,issuer));
        U07NonProductionF3A1U05Flow.U05InputProvider provider=
                new U07NonProductionF3A1U05Flow.U05InputProvider() {
                    @Override public U05ReadinessInputManifest manifest(
                            U05AdmissionAuthoritySnapshot a) {
                        return readinessManifest(a);
                    }
                    @Override public U05ConsumerInboundRequest request(
                            U05AdmissionAuthoritySnapshot a) {
                        U05ReadinessInputManifest m=readinessManifest(a);
                        return new U05ConsumerInboundRequest(
                                "test-http-u05-"+a.getConsultationId(),
                                a.getConsultationId(),a.getCdpId(),
                                a.getCurrentClinicalStateVersion(),a.getAuthoritativeStateRef(),
                                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                                U05ConsumerInboundRequest.U04_A1_POST_BARRIER_ROUTING,
                                a.getRouteSourceRef(),U05ConsumerInboundRequest.U05_ELIGIBLE,
                                a.getGateRef(),a.getGateCommitRef(),a.getGateValue(),
                                a.getRouteAuthorizationType(),a.getRouteAuthorizationRef(),
                                a.getRoutingPolicyVersion(),"A1",null,null,m.getManifestRef(),
                                m.getSetIdentity(),m.authoritativeRecordRefs(),
                                "test-http-u05-event","test-http-u05-business-event",
                                "synthetic-correlation","synthetic-trace",
                                U05AdmissionService.CONTRACT_VERSION,
                                "ci-nonprod-safety","2026-10-16T00:00:00Z");
                    }
                };
        U05NonProductionApplicationService u05=new U05NonProductionApplicationService(
                new U05AdmissionService(new U05InMemoryAdmissionLedger()),
                new U05ClinicalReadinessPolicy(),new U05ReadinessStateProposalFactory(),
                new U05CommitService(committer,new ClinicalCdpReadbackAdapter(cdps)),
                (request,decision,evidence)->new U05RoutingCurrentness(
                        evidence.getCommittedClinicalStateVersion(),"test-http-route-current",
                        true,true,true,true,true,request.getAcceptedU04GateRef(),
                        request.getGateValue(),null),
                new U05RoutingService(new U05InMemoryRouteLedger(),
                        (request,evidence,current,consequence,target,action)->
                                new U05DownstreamPermissionDecision(
                                        "test-http-u05-permission",request.getConsultationId(),
                                        request.getCdpId(),current.getCurrentU04GateRef(),null,
                                        consequence,action,target,"PERMITTED",
                                        "test-http-route-permission","test-policy","v1","CURRENT")));
        return new U07NonProductionF3A1U05Flow(
                owner,issuer,bridge,provider,u05,tx);
    }

    private static U05ReadinessInputManifest readinessManifest(U05AdmissionAuthoritySnapshot a) {
        List<U05ReadinessInput> inputs=new ArrayList<>();
        for(String domain:Arrays.asList("F1","F3","F5","F6")) {
            boolean present="F1".equals(domain)||"F3".equals(domain);
            String signal="F1".equals(domain)?"FRAMED_IN_SCOPE":
                    "F3".equals(domain)?"CAN_ASK_MORE":null;
            String kind="F1".equals(domain)?"SCOPE_OR_FRAMING":
                    "F3".equals(domain)?"ONLINE_INFORMATION_GAP":
                    "F5".equals(domain)?"DDX_OR_MUST_EXCLUDE":"OFFLINE_EVIDENCE";
            inputs.add(new U05ReadinessInput("test-http-u05-"+domain,domain,
                    "test-owner-"+domain,kind,present?"PRESENT":"NOT_YET_APPLICABLE",
                    signal,a.getConsultationId(),a.getCdpId(),a.getCurrentClinicalStateVersion(),
                    "test-http-decision-"+domain,"test-http-state-"+domain,
                    "test-http-applicability-"+domain,
                    Collections.singletonList("test-evidence-"+domain),
                    Collections.singletonList("test-policy-"+domain),
                    "2026-10-16T00:00:00Z","CURRENT",null));
        }
        String context=U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT;
        String identity=U05ReadinessInputManifest.semanticSetIdentity(
                a.getConsultationId(),a.getCdpId(),a.getCurrentClinicalStateVersion(),
                context,U05ReadinessInputManifest.RDP05_CONTRACT_VERSION,inputs);
        return new U05ReadinessInputManifest("test-http-manifest-"+a.getConsultationId(),
                identity,a.getConsultationId(),a.getCdpId(),a.getCurrentClinicalStateVersion(),
                context,U05ReadinessInputManifest.RDP05_CONTRACT_VERSION,inputs);
    }

    @Bean U05QuestionRouteAdmissionAdapter admissionFromU05() {
        return new U05QuestionRouteAdmissionAdapter(new U06AdmissionService());
    }

    @Bean U06NonProductionPhysicalMode23Service physicalU06(CDPRepository cdps,
            StateCommitter committer) {
        return new U06NonProductionPhysicalMode23Service(
                (request,decision,safety)->{
                    throw new IllegalStateException("SYNTHETIC_MODE1_NOT_AUTHORIZED");
                },cdps,committer,new U06AdmissionService());
    }

    @Bean U07NonProductionPhysicalFollowupCoordinator.AuthorizedQuestionHost
            syntheticQuestionDecisionHost(CurrentRun current,CDPRepository cdps) {
        return new U07NonProductionPhysicalFollowupCoordinator.AuthorizedQuestionHost() {
            @Override public U06AdmissionEvidence requireCurrentAdmissionEvidence(
                    U05ExecutionResult readiness) {
                if(!current.consultation.equals(readiness.getRoutingDecision().getConsultationId()))
                    throw new IllegalStateException("SYNTHETIC_CONSULTATION_MISMATCH");
                return U06AdmissionEvidence.syntheticCurrentAllow();
            }
            @Override public String questionPolicyRef() {return "test-question-policy";}
            @Override public String d04PolicyRef() {return "test-d04-policy";}
            @Override public String dependencyBindingRef() {return "test-u06-dependency";}
            @Override public String threadId() {return current.thread;}
            @Override public String runId() {return current.run;}
            @Override public long expectedConsultationRowVersion() {return 0L;}
            @Override public U06SyntheticDecisionBundle requireCurrentQuestionDecision(
                    U06ProfileBRequest request,int version) {
                CDP cdp=cdps.findById(current.cdp).orElseThrow(IllegalStateException::new);
                Map<?,?> assessment=(Map<?,?>)cdp.getPatientState().get("f3_gap_assessment");
                String effect=(String)assessment.get("f3_canonical_effect_id");
                return new U06SyntheticDecisionBundle(
                        U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED,effect,
                        "test-http-u06-gap","DECISION_MATERIAL",true,
                        U06SyntheticDecisionBundle.CONTINUE,U06SyntheticDecisionBundle.SELECTED,
                        "test-http-selection-effect-"+current.consultation,
                        "test-u06-question-ci","TEST:FOLLOWUP","synthetic follow-up question",
                        "sha256:synthetic-question",null,null);
            }
        };
    }

    @Bean U07NonProductionDeliveryWaitRecovery verifiedSyntheticDeliveryRecovery(
            CDPRepository cdps,JdbcTemplate jdbc,TransactionTemplate tx,
            StateCommitter committer,ConsultationRepository consultations,
            ConsultationWaitEffectRepository effects,
            RuntimeThreadStateRepository threads,RuntimeWaitCheckpointRepository checkpoints) {
        U07ExistingU06WaitAuthority durable=new U07ExistingU06WaitAuthority(
                new ConsultationWaitTransitionService(consultations,effects),
                new U06WaitCoordinator(
                        new RuntimeWaitCheckpointService(threads,checkpoints),
                        new RuntimeThreadWaitTransitionService(threads,checkpoints)));
        return new U07NonProductionDeliveryWaitRecovery(cdps,jdbc,tx,committer,
                receipt->"fixture-confirmed".equals(receipt.confirmationRef)
                        && receipt.deliveryId.startsWith("fixture-delivery-"),
                (receipt,version)->tx.execute(status->durable.establish(receipt,version)));
    }

    @Bean U07NonProductionPhysicalFollowupCoordinator physicalCoordinator(
            U07NonProductionF3A1U05Flow flow,U05QuestionRouteAdmissionAdapter admission,
            U06NonProductionPhysicalMode23Service u06,
            U07NonProductionDeliveryWaitRecovery wait,CDPRepository cdps,
            RuntimeBindingService bindings,ClinicalRunRepository runs,
            TransactionTemplate tx,
            U07NonProductionPhysicalFollowupCoordinator.AuthorizedQuestionHost host) {
        return new U07NonProductionPhysicalFollowupCoordinator(
                flow,admission,u06,wait,cdps,bindings,runs,tx,host);
    }

    @Bean V1NonProductionFirstRoundWaitController.FirstRoundOwner
            syntheticFirstRoundOwner(RuntimeBindingService bindings,
            V1NonProductionDeliveredQuestionReadback readback,
            V1NonProductionFirstRoundOwnerAdapter.AuthorizedQuestionContent content,
            CurrentRun current) {
        V1NonProductionFirstRoundOwnerAdapter.AuthorizedStateOwner state=
                new V1NonProductionFirstRoundOwnerAdapter.AuthorizedStateOwner() {
                    @Override public String requireCurrentStateRef(String consultation,
                            String run,U02ExecutionResult fact) {return "cdp-physical-v5";}
                    @Override public String requireCurrentCdpId(String consultation,String run) {
                        return bindings.requireBinding(consultation).getCdpId();
                    }
                };
        V1NonProductionFirstRoundOwnerAdapter.DeliveryOwnerAuthority delivery=
                (consultation,run)->selected->{
                    if(!consultation.equals(current.consultation)
                            || !run.equals(current.run))
                        throw new IllegalStateException("SYNTHETIC_DELIVERY_OWNER_MISMATCH");
                    return new U07NonProductionDeliveryWaitRecovery.Receipt(
                            selected.request,selected.decision,
                            "fixture-delivery-"+current.run,
                            "fixture-delivery-effect-"+current.run,
                            "fixture-confirmed");
                };
        return new V1NonProductionFirstRoundOwnerAdapter(state,delivery,readback,content);
    }

    @Bean V1NonProductionFirstRoundWaitController tcpStartThroughWait(
            U01NaturalLanguageStartService start,U01U02NonProductionHandoffService facts,
            U07NonProductionGovernedRiskSafetyContinuation clinical,
            U07NonProductionPhysicalFollowupCoordinator followup,
            V1NonProductionClinicalSafetyHandoffController.AuthorizedClinicalHost authority,
            V1NonProductionFirstRoundWaitController.FirstRoundOwner owner) {
        return new V1NonProductionFirstRoundWaitController(
                start,facts,clinical,followup,authority,owner);
    }

    @Bean U07NonProductionAnswerResumeAdmission answerAdmission(
            ConsultationRepository consultations,RuntimeThreadStateRepository threads,
            RuntimeWaitCheckpointRepository checkpoints,CDPRepository cdps) {
        return new U07NonProductionAnswerResumeAdmission(
                consultations,threads,checkpoints,cdps);
    }

    @Bean U07NonProductionDurableF8Consume durableF8(
            JdbcTemplate jdbc,TransactionTemplate tx,
            CanonicalBusinessEventRepository events,RuntimeBindingService bindings,
            ClinicalRunRepository runs,ConsultationRepository consultations,
            RuntimeThreadStateRepository threads,RuntimeWaitCheckpointRepository checkpoints) {
        return new U07NonProductionDurableF8Consume(
                jdbc,tx,new CanonicalBusinessEventLedger(events),
                new ClinicalRunCoordinator(bindings,events,runs),
                consultations,threads,checkpoints);
    }

    @Bean U07NonProductionTrustedAnswerU02Bridge trustedAnswerRun(
            ClinicalRunRepository runs,CanonicalBusinessEventRepository events,
            U07NonProductionDurableF8Consume f8) {
        return new U07NonProductionTrustedAnswerU02Bridge(runs,events,f8);
    }

    @Bean V1NonProductionAnswerResumeController.TrustedAnswerHost
            syntheticAnswerHost(ConsultationRepository consultations,
                    RuntimeWaitCheckpointRepository checkpoints) {
        return request->{
            RuntimeWaitCheckpointRecord cp=checkpoints.findById(request.checkpointId)
                    .orElseThrow(IllegalStateException::new);
            ConsultationRecord consultation=consultations.findById(request.consultationId)
                    .orElseThrow(IllegalStateException::new);
            if(!request.consultationId.equals(cp.getConsultationId())
                    || !"ACTIVE".equals(cp.getCheckpointStatus())
                    || consultation.getCdpId()==null
                    || consultation.getUserId()==null
                    || !consultation.getUserId().startsWith("synthetic-"))
                throw new IllegalStateException("SYNTHETIC_ANSWER_AUTHORITY_DENIED");
            String suffix=cp.getCheckpointId();
            return new V1NonProductionAnswerResumeController.AuthorizedAnswer(
                    request.consultationId,consultation.getCdpId(),cp.getThreadId(),
                    cp.getRunId(),cp.getCheckpointId(),cp.getQuestionId(),cp.getDeliveryId(),
                    consultation.getUserId(),"test-answer-event-"+suffix,
                    "synthetic-answer-correlation","synthetic-answer-trace",
                    "test-answer-idempotency-"+suffix,"sha256:synthetic-answer-"+suffix);
        };
    }

    @Bean V1NonProductionAnswerResumeController tcpF8U02(
            U07NonProductionAnswerResumeAdmission admission,
            U07NonProductionDurableF8Consume f8,
            U07NonProductionTrustedAnswerU02Bridge bridge,
            U02ClinicalFactApplicationService facts,
            V1NonProductionAnswerResumeController.TrustedAnswerHost host,
            TransactionTemplate tx) {
        return new V1NonProductionAnswerResumeController(admission,f8,bridge,facts,host,tx);
    }

    @Bean V1NonProductionAnswerRiskReevaluationController.ResumedRiskOwner
            syntheticResumedRiskOwner(RuntimeBindingService bindings,
                    ClinicalRunRepository runs) {
        return (consultation,runId,fact)->{
            ClinicalRunRecord run=runs.findById(runId).orElseThrow(IllegalStateException::new);
            RuntimeBindingRecord bound=bindings.requireBinding(consultation);
            return new V1NonProductionClinicalSafetyHandoffController.AuthorizedExecution(
                    new U03ExecutionCommand(consultation,bound.getThreadId(),runId,
                            run.getEventId(),bound.getCdpId(),
                            fact.commitResult.committedVersion.intValue(),
                            "synthetic-answer-correlation","synthetic-answer-trace"),
                    U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                    "ci-nonprod-safety",U04ScopeContext.current(true,true));
        };
    }

    @Bean V1NonProductionAnswerRiskReevaluationController tcpAnswerThroughRisk(
            V1NonProductionAnswerResumeController answer,
            U07NonProductionGovernedRiskSafetyContinuation clinical,
            V1NonProductionAnswerRiskReevaluationController.ResumedRiskOwner owner) {
        return new V1NonProductionAnswerRiskReevaluationController(answer,clinical,owner);
    }
}
