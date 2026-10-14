package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.u05.*;
import com.aidoctor.diagnosis.runtime.u06.delivery.*;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRecord;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.aidoctor.diagnosis.runtime.u04.U04AdmissionResult;
import com.aidoctor.diagnosis.runtime.u04.U04AdmissionService;
import com.aidoctor.diagnosis.runtime.u04.U04AdmittedInput;
import com.aidoctor.diagnosis.runtime.u04.U04CommitService;
import com.aidoctor.diagnosis.runtime.u04.U04ExecutionResult;
import com.aidoctor.diagnosis.runtime.u04.U04NonProductionApplicationService;
import com.aidoctor.diagnosis.runtime.u04.U04RoutingEligibility;
import com.aidoctor.diagnosis.runtime.u04.U04SafetyGateDecision;
import com.aidoctor.diagnosis.runtime.u04.U04SafetyGatePolicy;
import com.aidoctor.diagnosis.runtime.u04.U04ScopeContext;
import com.aidoctor.diagnosis.runtime.u04.U04StateProposal;
import com.aidoctor.diagnosis.runtime.u04.U04StateProposalFactory;
import com.aidoctor.diagnosis.runtime.u05.U04A1U05AuthorityBridge;
import com.aidoctor.diagnosis.runtime.u05.U05AdmissionAuthoritySnapshot;
import com.aidoctor.diagnosis.runtime.u06.*;
import com.aidoctor.diagnosis.runtime.u06.state.U06SyntheticP01Runtime;
import com.aidoctor.diagnosis.runtime.u06.state.U06SyntheticP01TestFactory;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06SyntheticDeliveryService;
import com.aidoctor.diagnosis.runtime.u06.trace.U06GovernedExecutionTraceStore;
import com.aidoctor.diagnosis.runtime.u06.wait.*;
import com.aidoctor.diagnosis.runtime.foundation.*;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRepository;
import static org.mockito.Mockito.mock;
import com.aidoctor.diagnosis.state.committer.SyntheticStateSnapshot;
import com.aidoctor.diagnosis.state.committer.SyntheticVersionedStateRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
import com.aidoctor.diagnosis.state.committer.fakes.InMemoryIdempotencyFake;
import com.aidoctor.diagnosis.state.committer.fakes.MechanicalVersionRepositoryFake;
import com.aidoctor.diagnosis.state.committer.fakes.RecordingCommitEventEvidenceFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticAuditPortFake;
import com.aidoctor.diagnosis.state.committer.ports.CapabilityPolicyPort;
import com.aidoctor.diagnosis.state.committer.ports.ConsentPolicyPort;
import com.aidoctor.diagnosis.state.committer.ports.FieldPermissionPort;
import com.aidoctor.diagnosis.state.committer.ports.SourceValidationPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class U04NonProductionSafetyGateTest {
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-09-18T00:00:00Z"), ZoneOffset.UTC);
    private static final String ENV = "ci-nonprod-u04";

    @Test
    void actualU04CommitReadbackRequiresIndependentA1BeforeU05Authority() {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        Map<String, Object> patientState = new LinkedHashMap<String, Object>();
        patientState.put("questions", new LinkedHashMap<String, Object>());
        patientState.put("information_gaps", new LinkedHashMap<String, Object>());
        root.put("patient_state", patientState);
        Map<String, SyntheticStateSnapshot> initial =
                new LinkedHashMap<String, SyntheticStateSnapshot>();
        initial.put("cdp-1", new SyntheticStateSnapshot(8, root));
        SyntheticVersionedStateRepository repository =
                new SyntheticVersionedStateRepository(initial);
        List<String> order = new ArrayList<String>();
        StateCommitter committer = new StateCommitter(
                repository,
                (id, version) -> CapabilityPolicyPort.CapabilityDecision.authorized(),
                (path, id) -> FieldPermissionPort.FieldPermissionDecision.authorized(),
                (cdp, id) -> ConsentPolicyPort.ConsentDecision.authorized(),
                source -> SourceValidationPort.SourceDecision.authorized(),
                new InMemoryIdempotencyFake(order),
                new SyntheticAuditPortFake(CLOCK, order),
                new RecordingCommitEventEvidenceFake(order), CLOCK);
        U04NonProductionApplicationService application =
                new U04NonProductionApplicationService(
                        new U04AdmissionService(), new U04SafetyGatePolicy(),
                        new U04StateProposalFactory(), new U04CommitService(committer));
        U03OutboundHandoff handoff = validHandoff("NO_HIGH_RISK_SIGNAL");
        U04ExecutionResult committed =
                application.execute(handoff, 8, U04ScopeContext.current(true, true), ENV);
        assertEquals(U04ExecutionResult.COMMIT_COMPLETE, committed.getStatus());
        assertEquals(9, repository.snapshot("cdp-1").version());

        U04A1U05AuthorityBridge.GateReadbackPort readback = cdp -> {
            SyntheticStateSnapshot snapshot = repository.snapshot(cdp);
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) snapshot.state().get("patient_state");
            @SuppressWarnings("unchecked")
            Map<String, Object> gate = (Map<String, Object>) patient.get("safety_gate");
            return new U04A1U05AuthorityBridge.GateReadback(
                    snapshot.version(), (String) gate.get("safety_gate"),
                    (String) gate.get("source_u03_decision_ref"),
                    (String) gate.get("u04_policy_ref"));
        };
        U04A1U05AuthorityBridge noA1 =
                new U04A1U05AuthorityBridge(readback, (consultation, cdp, version) -> null);
        assertThrows(IllegalStateException.class,
                () -> noA1.load(handoff, committed, ENV, "clinical-state-cdp-1-v9"));
        assertEquals(1, repository.mutationCount());

        U04A1U05AuthorityBridge staleReadback = new U04A1U05AuthorityBridge(
                cdp -> new U04A1U05AuthorityBridge.GateReadback(
                        8, "ALLOW", handoff.getDecisionId(), U04ScopeContext.FROZEN_POLICY_REF),
                (consultation, cdp, version) -> null);
        assertThrows(IllegalStateException.class,
                () -> staleReadback.load(handoff, committed, ENV, "clinical-state-cdp-1-v9"));

        // Explicitly synthetic A1 host attestation, not a production F3 issuer.
        U04A1U05AuthorityBridge.A1BarrierAuthorityPort syntheticA1 =
                (consultation, cdp, version) -> new U04A1U05AuthorityBridge.A1BarrierAuthority(
                        consultation, cdp, version, "synthetic-f3-commit",
                        "synthetic-post-f3-barrier", "synthetic-f3-revalidated",
                        "synthetic-a1-route-source", "synthetic-a1-route-auth",
                        "synthetic-a1-routing-policy", null, null, true, false);
        U04A1U05AuthorityBridge bridge = new U04A1U05AuthorityBridge(readback, syntheticA1);
        U05AdmissionAuthoritySnapshot authority =
                bridge.load(handoff, committed, ENV, "clinical-state-cdp-1-v9");
        assertEquals(9, authority.getCurrentClinicalStateVersion());
        assertEquals("ALLOW", authority.getGateValue());
        assertEquals(committed.getDecision().getDecisionId(), authority.getGateRef());
        assertEquals(committed.getCommitResult().auditRef.auditId, authority.getGateCommitRef());
        assertTrue(authority.isA1CanonicalF3Complete());
        assertTrue(authority.isPostF3SafetyBarrierComplete());
        assertEquals(1, repository.mutationCount());

        // Actual U06 Mode-1 F3 commit advances the SAME CDP v9 -> v10.
        U06SyntheticP01Runtime u06State = U06SyntheticP01TestFactory.fromSharedSyntheticRepository(
                "u04-u06-shared-store", "consult-1", "cdp-1", CLOCK, repository);
        U06ProfileBApplicationService u06 = new U06ProfileBApplicationService(
                new U06AdmissionService(), u06State,
                new U06SyntheticDeliveryService(mock(U06DeliveryStore.class)),
                new ConsultationWaitTransitionService(
                        mock(ConsultationRepository.class), mock(ConsultationWaitEffectRepository.class)),
                new U06WaitCoordinator(
                        new RuntimeWaitCheckpointService(
                                mock(RuntimeThreadStateRepository.class),
                                mock(RuntimeWaitCheckpointRepository.class)),
                        new RuntimeThreadWaitTransitionService(
                                mock(RuntimeThreadStateRepository.class),
                                mock(RuntimeWaitCheckpointRepository.class))),
                new U06GovernedExecutionTraceStore() {
                    public void start(String a,String b,String c,String d,String e,String f,String g) {}
                    public void complete(String a,String b,String c,String d) {}
                });
        U06ProfileBRequest f3Request = new U06ProfileBRequest(
                "a1-f3-req", "consult-1", "cdp-1",
                U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT,
                U06ProfileBRequest.A1_PRE_READINESS_ROUTING,
                "a1-source-u04-" + committed.getDecision().getDecisionId(),
                9, 9, U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING, "synthetic-dependency",
                "f3-policy", null, null,
                "canonical-event", "business-event", null, null, 0,
                "corr-1", "trace-1", "2026-09-18T00:00:00Z",
                U06AdmissionEvidence.syntheticCurrentAllow());
        U06SyntheticDecisionBundle f3Decision = new U06SyntheticDecisionEngine().decide(
                f3Request, new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.SUCCESS, true, false,
                        "gap-1", "DECISION_MATERIAL", true, null,
                        Collections.<U06SyntheticDecisionInput.Candidate>emptyList()),
                u06State.readCurrent());
        U06SyntheticPostF3SafetyBarrier.Evidence safetyEvidence =
                new U06SyntheticPostF3SafetyBarrier.Evidence(
                        U06SyntheticPostF3SafetyBarrier.ALLOWED, "synthetic-a1-safety-evidence");
        U06ExecutionResult f3Result = u06.execute(f3Request, f3Decision, null, safetyEvidence);
        assertEquals(U06ExecutionResult.MODE1_COMMITTED, f3Result.getStatus());
        assertEquals(10, repository.snapshot("cdp-1").version());
        U06SyntheticPostF3SafetyBarrier.Evaluation evaluated =
                new U06SyntheticPostF3SafetyBarrier().evaluate(
                        "consult-1", f3Result.getEffectRef(),
                        f3Result.getCommitStatus(), 10, safetyEvidence);
        U06ProfileBRequest rvRequest = new U06ProfileBRequest(
                "a1-f3-rv", "consult-1", "cdp-1",
                U06ProfileBRequest.F3_CURRENT_VERSION_REVALIDATION,
                U06ProfileBRequest.POST_F3_SAFETY_BARRIER_ROUTING,
                "a1-f3-post-barrier", 10, 10,
                U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING, "synthetic-dependency",
                "f3-policy", null, null,
                "canonical-event", "business-event", null, null, 0,
                "corr-1", "trace-1", "2026-09-18T00:00:00Z",
                U06AdmissionEvidence.syntheticCurrentAllow());
        U06SyntheticDecisionBundle rvDecision = new U06SyntheticDecisionBundle(
                U06SyntheticDecisionBundle.NOT_DECIDABLE, f3Result.getEffectRef(),
                null, null, false, null, null, null, null, null, null, null,
                U06SyntheticDecisionBundle.REVALIDATED_CURRENT, "a1-revalidation-10");
        U06SyntheticRevalidationAuthority.Result rv =
                new U06SyntheticRevalidationAuthority().evaluate(
                        rvRequest, rvDecision, u06State.readCurrent().getVersion(),
                        u06State.readCurrent().mapString(
                                "/patient_state/f3_gap_assessment", "f3_canonical_effect_id"));
        assertEquals(U06SyntheticDecisionBundle.REVALIDATED_CURRENT, rv.getStatus());

        U04A1U05AuthorityBridge dynamicBridge = new U04A1U05AuthorityBridge(
                readback, new U06F3SyntheticA1HostPort(
                        "consult-1", "cdp-1", u06State, f3Result, evaluated, rv));
        U05AdmissionAuthoritySnapshot currentA1 =
                dynamicBridge.load(handoff, committed, ENV, "clinical-state-cdp-1-v10");
        assertEquals(10, currentA1.getCurrentClinicalStateVersion());
        assertEquals(f3Result.getEffectRef(),
                u06State.readCurrent().mapString(
                        "/patient_state/f3_gap_assessment", "f3_canonical_effect_id"));
        assertEquals(2, repository.mutationCount());
        assertThrows(IllegalStateException.class, () -> new U04A1U05AuthorityBridge(
                readback, new U06F3SyntheticA1HostPort(
                        "consult-1", "cdp-1", u06State, f3Result,
                        new U06SyntheticPostF3SafetyBarrier().evaluate(
                                "consult-1", f3Result.getEffectRef(), "COMMITTED", 10,
                                new U06SyntheticPostF3SafetyBarrier.Evidence(
                                        U06SyntheticPostF3SafetyBarrier.BLOCKED,
                                        "synthetic-blocked")), rv))
                .load(handoff, committed, ENV, "clinical-state-cdp-1-v10"));
        // Full E2E continuation: U05 MUST consume the actual dynamic A1
        // authority produced above; all CDP writes share 'repository'.
        String context = U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT;
        int readinessVersion = repository.snapshot("cdp-1").version();
        assertEquals(10, readinessVersion);
        assertTrue(u06State.readCurrent().exists("/patient_state/information_gaps/gap-1"));
        List<U05ReadinessInput> inputs = Arrays.asList(
                present(U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, readinessVersion),
                present(U05ReadinessInput.F3, U05ReadinessInput.CAN_ASK_MORE, readinessVersion),
                applicability(U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, readinessVersion),
                applicability(U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE, readinessVersion));
        U05ReadinessInputManifest manifest = manifest(context, readinessVersion, inputs);
        U05ConsumerInboundRequest inbound = new U05ConsumerInboundRequest(
                "full-e2e-u05-request", handoff.getConsultationId(), handoff.getCdpId(),
                readinessVersion, currentA1.getAuthoritativeStateRef(),
                context, U05ConsumerInboundRequest.U04_A1_POST_BARRIER_ROUTING,
                currentA1.getRouteSourceRef(), currentA1.getRouteConsequence(),
                currentA1.getGateRef(), currentA1.getGateCommitRef(), currentA1.getGateValue(),
                currentA1.getRouteAuthorizationType(), currentA1.getRouteAuthorizationRef(),
                currentA1.getRoutingPolicyVersion(), currentA1.getBootstrapArchitectureBindingRef(),
                null, null, manifest.getManifestRef(), manifest.getSetIdentity(),
                manifest.authoritativeRecordRefs(), "canonical-event",
                "business-event", handoff.getCorrelationId(), handoff.getTraceId(),
                U05AdmissionService.CONTRACT_VERSION, ENV, "2026-09-18T00:00:00Z");
        U05RoutingCurrentnessPort routingCurrentness =
                (input, decision, evidence) -> new U05RoutingCurrentness(
                        evidence.getCommittedClinicalStateVersion(),
                        "full-e2e-route-" + evidence.getEffectId(),
                        true, true, true, true, true,
                        input.getAcceptedU04GateRef(), input.getGateValue(),
                        input.getAcceptedRestrictedContextRef());
        U05DownstreamPermissionPort downstreamPermission =
                (input, evidence, currentness, consequence, targetUnit, action) ->
                        new U05DownstreamPermissionDecision(
                                "synthetic-question-permission", input.getConsultationId(),
                                input.getCdpId(), currentness.getCurrentU04GateRef(),
                                input.getAcceptedRestrictedContextRef(), consequence, action,
                                targetUnit, U05DownstreamPermissionDecision.PERMITTED,
                                "synthetic-permission", "synthetic-policy", "1", "CURRENT");
        U05NonProductionApplicationService u05 = new U05NonProductionApplicationService(
                new U05AdmissionService(new U05InMemoryAdmissionLedger()),
                new U05ClinicalReadinessPolicy(), new U05ReadinessStateProposalFactory(),
                new U05CommitService(committer,
                        new U05SyntheticClinicalReadinessSnapshotAdapter(repository)),
                routingCurrentness,
                new U05RoutingService(new U05InMemoryRouteLedger(), downstreamPermission));
        U05ExecutionResult readiness = u05.execute(inbound, manifest, currentA1);
        assertEquals(U05ExecutionResult.ROUTING_COMPLETE, readiness.getStatus());
        assertEquals(U05DownstreamRoutingDecision.ELIGIBLE,
                readiness.getRoutingDecision().getRoutingStatus());
        assertEquals(U05DownstreamRoutingDecision.TO_U06_QUESTION_PATH,
                readiness.getRoutingDecision().getDownstreamConsequence());
        assertEquals(11, repository.snapshot("cdp-1").version());
        U06AdmissionService questionAdmission = new U06AdmissionService();
        U06AdmissionService.Admission admitted =
                new U05QuestionRouteAdmissionAdapter(questionAdmission).admit(
                        readiness, U06AdmissionEvidence.syntheticCurrentAllow(),
                        u06State.readCurrent().getVersion(),
                        "question-policy", "d04-policy", "synthetic-dependency",
                        "thread-1", "run-1", 0);
        assertTrue(admitted.isAdmitted());
        final AtomicInteger syntheticSends = new AtomicInteger();
        U06DeliveryStore deliveryStore = new U06DeliveryStore() {
            private final Map<String, Snapshot> confirmations = new LinkedHashMap<String, Snapshot>();
            public synchronized Snapshot reconcileConfirmed(Command command) {
                Snapshot existing = confirmations.get(command.deliveryEffectId);
                if (existing != null) return new Snapshot(
                        existing.deliveryEffectId, existing.deliveryId, existing.confirmationStatus, true);
                syntheticSends.incrementAndGet();
                Snapshot confirmed = new Snapshot(command.deliveryEffectId, command.deliveryId, "CONFIRMED", false);
                confirmations.put(command.deliveryEffectId, confirmed);
                return confirmed;
            }
        };
        U06SyntheticDeliveryService delivery=new U06SyntheticDeliveryService(deliveryStore);
        ConsultationRepository consultations=mock(ConsultationRepository.class);
        ConsultationWaitEffectRepository waitEffects=mock(ConsultationWaitEffectRepository.class);
        ConsultationRecord consultation=mock(ConsultationRecord.class);
        AtomicReference<String> consultationLifecycle=new AtomicReference<String>(ConsultationRecord.ACTIVE);
        AtomicReference<String> consultationWaitEffect=new AtomicReference<String>();
        AtomicReference<Long> consultationVersion=new AtomicReference<Long>(Long.valueOf(0));
        AtomicReference<ConsultationWaitEffectRecord> waitEffectRecord=new AtomicReference<ConsultationWaitEffectRecord>();
        when(consultations.findByIdForUpdate("consult-1")).thenReturn(Optional.of(consultation));
        when(consultation.getLifecycleStatus()).thenAnswer(i->consultationLifecycle.get());
        when(consultation.getCurrentWaitEffectId()).thenAnswer(i->consultationWaitEffect.get());
        when(consultation.getRowVersion()).thenAnswer(i->consultationVersion.get());
        doAnswer(i->{consultationLifecycle.set(ConsultationRecord.WAITING_USER);consultationWaitEffect.set(i.getArgument(0));consultationVersion.set(Long.valueOf(1));return null;})
                .when(consultation).enterWaitingUser(anyString());
        when(consultations.saveAndFlush(consultation)).thenReturn(consultation);
        when(waitEffects.findById(anyString())).thenAnswer(i->{
            ConsultationWaitEffectRecord x=waitEffectRecord.get();
            return x!=null&&x.getWaitEffectId().equals(i.getArgument(0))?Optional.of(x):Optional.empty();
        });
        when(waitEffects.findByIdempotencyKey(anyString())).thenAnswer(i->{
            ConsultationWaitEffectRecord x=waitEffectRecord.get();
            return x!=null&&x.getIdempotencyKey().equals(i.getArgument(0))?Optional.of(x):Optional.empty();
        });
        when(waitEffects.saveAndFlush(any(ConsultationWaitEffectRecord.class))).thenAnswer(i->{ConsultationWaitEffectRecord x=i.getArgument(0);waitEffectRecord.set(x);return x;});
        ConsultationWaitTransitionService consultationWait=new ConsultationWaitTransitionService(consultations,waitEffects);

        RuntimeThreadStateRepository threadRepo=mock(RuntimeThreadStateRepository.class);
        RuntimeWaitCheckpointRepository checkpointRepo=mock(RuntimeWaitCheckpointRepository.class);
        AtomicReference<RuntimeThreadStateRecord> thread=new AtomicReference<RuntimeThreadStateRecord>();
        AtomicReference<RuntimeWaitCheckpointRecord> checkpoint=new AtomicReference<RuntimeWaitCheckpointRecord>();
        when(threadRepo.findById(anyString())).thenAnswer(i->Optional.ofNullable(thread.get()));
        when(threadRepo.findByThreadIdForUpdate(anyString())).thenAnswer(i->Optional.ofNullable(thread.get()));
        when(threadRepo.saveAndFlush(any(RuntimeThreadStateRecord.class))).thenAnswer(i->{RuntimeThreadStateRecord x=i.getArgument(0);thread.set(x);return x;});
        when(checkpointRepo.findById(anyString())).thenAnswer(i->Optional.ofNullable(checkpoint.get()));
        when(checkpointRepo.saveAndFlush(any(RuntimeWaitCheckpointRecord.class))).thenAnswer(i->{RuntimeWaitCheckpointRecord x=i.getArgument(0);checkpoint.set(x);return x;});
        RuntimeWaitCheckpointService cps=new RuntimeWaitCheckpointService(threadRepo,checkpointRepo);
        RuntimeThreadWaitTransitionService transitions=new RuntimeThreadWaitTransitionService(threadRepo,checkpointRepo);
        U06WaitCoordinator waitCoordinator=new U06WaitCoordinator(cps,transitions);

        U06GovernedExecutionTraceStore traces=new U06GovernedExecutionTraceStore(){
            public void start(String a,String b,String c,String d,String e,String f,String g){}
            public void complete(String a,String b,String c,String d){}
        };
        U06ProfileBApplicationService questionApp=new U06ProfileBApplicationService(questionAdmission,u06State,delivery,consultationWait,waitCoordinator,traces);


        U06SyntheticDecisionBundle selected = new U06SyntheticDecisionEngine().decide(
                admitted.getRequest(),
                new U06SyntheticDecisionInput(
                        U06SyntheticDecisionInput.SUCCESS, false, false,
                        "gap-1", "DECISION_MATERIAL", true,
                        U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,
                        Collections.singletonList(new U06SyntheticDecisionInput.Candidate(
                                "candidate-1", "question-1", "semantic-1",
                                "synthetic-content-ref-1", "content-fingerprint-1", 10, true))),
                u06State.readCurrent());
        U06SyntheticDeliveryService.ScopeAuthorization scope =
                new U06SyntheticDeliveryService.ScopeAuthorization(
                        "scope-auth-1", "fixture-authorization-1",
                        U06SyntheticDeliveryService.ScopeAuthorization.CURRENT,
                        "consult-1", U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                        "fixture-scope-1", "fixture-review-1", "u04-u06-shared-store",
                        "ci-nonprod-u06", "synthetic-endpoint-1", null,
                        false, false, false);
        U06ExecutionResult first = questionApp.execute(admitted.getRequest(), selected, scope);
        assertEquals(U06ExecutionResult.WAIT_ESTABLISHED, first.getStatus());
        assertEquals(13, repository.snapshot("cdp-1").version());
        assertEquals(5, repository.mutationCount());
        assertEquals(1, syntheticSends.get());
        assertEquals(RuntimeThreadStateRecord.AWAITING_USER, thread.get().getRuntimeStatus());
        assertTrue(u06State.readCurrent().exists("/patient_state/pending_question"));
        U06ExecutionResult replay = questionApp.execute(admitted.getRequest(), selected, scope);
        assertEquals(U06ExecutionResult.WAIT_ESTABLISHED, replay.getStatus());
        assertEquals(first.getDeliveryId(), replay.getDeliveryId());
        assertEquals(first.getWaitEffectId(), replay.getWaitEffectId());
        assertEquals(first.getCheckpointId(), replay.getCheckpointId());
        assertEquals(first.getU07ResumeEligibilityId(), replay.getU07ResumeEligibilityId());
        assertEquals(5, repository.mutationCount());
        assertEquals(1, syntheticSends.get());
        verify(consultation, times(1)).enterWaitingUser(anyString());
    }

    @Test
    void noHighRiskSignalCommitsAllowAndExposesOnlyU05Eligibility() {
        Fixture fixture = new Fixture(8);
        U04ExecutionResult result = fixture.application.execute(
                validHandoff("NO_HIGH_RISK_SIGNAL"),
                8,
                U04ScopeContext.current(true, true),
                ENV);

        assertEquals(U04ExecutionResult.COMMIT_COMPLETE, result.getStatus());
        assertEquals(U04SafetyGateDecision.ALLOW, result.getDecision().getGate());
        assertEquals("COMMITTED", result.getCommitResult().status);
        assertEquals(Integer.valueOf(9), result.getCommitResult().committedVersion);
        assertTrue(result.getRoutingEligibility().isU05Eligible());
        assertFalse(result.getRoutingEligibility().isRestrictedContextRequired());
        assertFalse(result.getRoutingEligibility().isU11Eligible());
        assertFalse(result.getRoutingEligibility().isU14Eligible());
        assertEquals(1, fixture.repository.commitCalls());
    }

    @Test
    void cautionCommitsRestrictedAndPreservesRestrictedContext() {
        Fixture fixture = new Fixture(8);
        U04ExecutionResult result = fixture.application.execute(
                validHandoff("CAUTION"),
                8,
                U04ScopeContext.current(true, true),
                ENV);

        assertEquals(U04SafetyGateDecision.RESTRICTED, result.getDecision().getGate());
        assertTrue(result.getRoutingEligibility().isU05Eligible());
        assertTrue(result.getRoutingEligibility().isRestrictedContextRequired());
        assertFalse(result.getRoutingEligibility().isU11Eligible());
        assertFalse(result.getRoutingEligibility().isU14Eligible());
    }

    @Test
    void highRiskCommitsBlockedAndNeverExposesOrdinaryU05() {
        Fixture fixture = new Fixture(8);
        U04ExecutionResult result = fixture.application.execute(
                validHandoff("HIGH_RISK"),
                8,
                U04ScopeContext.current(true, true),
                ENV);

        assertEquals(U04SafetyGateDecision.BLOCKED, result.getDecision().getGate());
        assertFalse(result.getRoutingEligibility().isU05Eligible());
        assertTrue(result.getRoutingEligibility().isU11Eligible());
        assertFalse(result.getRoutingEligibility().isU14Eligible());
    }

    @Test
    void failedU03CommitsUnavailableAndExposesOnlyU14Eligibility() {
        Fixture fixture = new Fixture(7);
        U04ExecutionResult result = fixture.application.execute(
                failedHandoff(),
                7,
                U04ScopeContext.current(true, true),
                ENV);

        assertEquals(U04SafetyGateDecision.UNAVAILABLE, result.getDecision().getGate());
        assertFalse(result.getRoutingEligibility().isU05Eligible());
        assertFalse(result.getRoutingEligibility().isU11Eligible());
        assertTrue(result.getRoutingEligibility().isU14Eligible());
    }

    @Test
    void unavailableScopeProducesUnavailableRatherThanAllow() {
        Fixture fixture = new Fixture(8);
        U04ExecutionResult result = fixture.application.execute(
                validHandoff("NO_HIGH_RISK_SIGNAL"),
                8,
                U04ScopeContext.current(false, false),
                ENV);

        assertEquals(U04SafetyGateDecision.UNAVAILABLE, result.getDecision().getGate());
        assertFalse(result.getRoutingEligibility().isU05Eligible());
        assertTrue(result.getRoutingEligibility().isU14Eligible());
    }

    @Test
    void staleU03HandoffFailsBeforeGateDecisionAndStateCommitter() {
        Fixture fixture = new Fixture(9);
        U04ExecutionResult result = fixture.application.execute(
                validHandoff("NO_HIGH_RISK_SIGNAL"),
                9,
                U04ScopeContext.current(true, true),
                ENV);

        assertEquals(U04ExecutionResult.ADMISSION_FAILED, result.getStatus());
        assertEquals(U04AdmissionService.STALE_U03_HANDOFF, result.getAdmission().getReasonCode());
        assertNull(result.getDecision());
        assertNull(result.getCommitResult());
        assertNull(result.getRoutingEligibility());
        assertEquals(0, fixture.repository.readCalls());
        assertEquals(0, fixture.repository.commitCalls());
    }

    @Test
    void wrongReleaseSetFailsBeforeGateDecisionAndCommit() {
        Fixture fixture = new Fixture(8);
        List<String> wrong = governedRefs();
        wrong.set(2, "RR-U03-RISK-001@0.2.0-candidate");

        U04ExecutionResult result = fixture.application.execute(
                validHandoff("NO_HIGH_RISK_SIGNAL", wrong),
                8,
                U04ScopeContext.current(true, true),
                ENV);

        assertEquals(U04ExecutionResult.ADMISSION_FAILED, result.getStatus());
        assertEquals(
                U04AdmissionService.UNTRUSTED_U03_RELEASE_SET,
                result.getAdmission().getReasonCode());
        assertEquals(0, fixture.repository.commitCalls());
    }

    @Test
    void productionEnvironmentIsRejectedBeforeAnyClinicalEffect() {
        Fixture fixture = new Fixture(8);
        assertThrows(
                IllegalStateException.class,
                () -> fixture.application.execute(
                        validHandoff("NO_HIGH_RISK_SIGNAL"),
                        8,
                        U04ScopeContext.current(true, true),
                        "production"));
        assertEquals(0, fixture.repository.commitCalls());
    }

    @Test
    void replayUsesStateCommitterIdempotencyAndDoesNotCommitTwice() {
        Fixture fixture = new Fixture(8);
        U04AdmissionResult admission = new U04AdmissionService().admit(
                validHandoff("CAUTION"),
                8,
                U04ScopeContext.current(true, true),
                ENV);
        assertTrue(admission.isAccepted());

        U04AdmittedInput input = admission.getAdmittedInput();
        U04SafetyGateDecision decision = new U04SafetyGatePolicy().decide(input);
        U04StateProposal proposal = new U04StateProposalFactory().create(input, decision);

        StateTypes.CommitResult first =
                fixture.commitService.commitNonProduction(input, decision, proposal);
        StateTypes.CommitResult replay =
                fixture.commitService.commitNonProduction(input, decision, proposal);

        assertEquals("COMMITTED", first.status);
        assertEquals(first.status, replay.status);
        assertEquals(first.committedVersion, replay.committedVersion);
        assertEquals(1, fixture.repository.commitCalls());
    }

    @Test
    void unsupportedStatePathFailsBeforeStateCommitter() {
        Fixture fixture = new Fixture(8);
        U04AdmissionResult admission = new U04AdmissionService().admit(
                validHandoff("HIGH_RISK"),
                8,
                U04ScopeContext.current(true, true),
                ENV);
        U04AdmittedInput input = admission.getAdmittedInput();
        U04SafetyGateDecision decision = new U04SafetyGatePolicy().decide(input);
        U04StateProposal proposal = new U04StateProposalFactory().create(input, decision);
        proposal.getStatePatch().operations.get(0).path = "/patient_state/other";

        assertThrows(
                IllegalStateException.class,
                () -> fixture.commitService.commitNonProduction(input, decision, proposal));
        assertEquals(0, fixture.repository.readCalls());
        assertEquals(0, fixture.repository.commitCalls());
    }


    @Test
    void executionAndDecisionStatusConflictFailsClosedBeforeGateDecision() {
        Fixture fixture = new Fixture(8);
        U03OutboundHandoff conflicting = new U03OutboundHandoff(
                "consult-1",
                "cdp-1",
                7,
                Integer.valueOf(8),
                "thread-1",
                "run-1",
                "event-1",
                "corr-1",
                "trace-1",
                ENV,
                U03NonProductionExecutionContext.BINDING_MODE,
                U03RiskAssessmentCandidate.FAILED,
                "INSUFFICIENT_INFORMATION",
                Collections.singletonList("RISK_ASSESSMENT_FAILED"),
                "RISK_ASSESSMENT_UNAVAILABLE",
                "u03-decision-1",
                U03RiskAssessmentCandidate.VALID,
                "NO_HIGH_RISK_SIGNAL",
                "U03_RULE_DECISION",
                U03GovernedCandidateGateway.BINDING_ID,
                governedRefs(),
                "acceptance-1",
                Collections.singletonList("evidence-1"),
                Collections.singletonList("source-1"),
                Collections.singletonList("provenance-1"),
                "u03-proposal-1",
                "COMMITTED",
                null,
                "audit-u03-1");

        U04ExecutionResult result = fixture.application.execute(
                conflicting,
                8,
                U04ScopeContext.current(true, true),
                ENV);

        assertEquals(U04ExecutionResult.ADMISSION_FAILED, result.getStatus());
        assertEquals(U04AdmissionService.MALFORMED_U03_HANDOFF, result.getAdmission().getReasonCode());
        assertNull(result.getDecision());
        assertEquals(0, fixture.repository.commitCalls());
    }

    @Test
    void failedReasonMismatchFailsClosedBeforeGateDecision() {
        Fixture fixture = new Fixture(7);
        U03OutboundHandoff conflicting = new U03OutboundHandoff(
                "consult-1",
                "cdp-1",
                7,
                null,
                "thread-1",
                "run-1",
                "event-1",
                "corr-1",
                "trace-1",
                ENV,
                U03NonProductionExecutionContext.BINDING_MODE,
                U03RiskAssessmentCandidate.FAILED,
                "INSUFFICIENT_INFORMATION",
                Collections.singletonList("RISK_ASSESSMENT_FAILED"),
                "RISK_ASSESSMENT_UNAVAILABLE",
                "u03-decision-1",
                U03RiskAssessmentCandidate.FAILED,
                null,
                "OTHER_FAILURE",
                U03GovernedCandidateGateway.BINDING_ID,
                governedRefs(),
                "acceptance-1",
                Collections.singletonList("evidence-1"),
                Collections.singletonList("source-1"),
                Collections.singletonList("provenance-1"),
                null,
                null,
                null,
                null);

        U04ExecutionResult result = fixture.application.execute(
                conflicting,
                7,
                U04ScopeContext.current(true, true),
                ENV);

        assertEquals(U04ExecutionResult.ADMISSION_FAILED, result.getStatus());
        assertEquals(U04AdmissionService.MALFORMED_U03_HANDOFF, result.getAdmission().getReasonCode());
        assertNull(result.getDecision());
        assertEquals(0, fixture.repository.commitCalls());
    }

    @Test
    void capabilityBindingMismatchFailsClosedBeforeGateDecision() {
        Fixture fixture = new Fixture(8);
        U03OutboundHandoff conflicting = new U03OutboundHandoff(
                "consult-1",
                "cdp-1",
                7,
                Integer.valueOf(8),
                "thread-1",
                "run-1",
                "event-1",
                "corr-1",
                "trace-1",
                ENV,
                U03NonProductionExecutionContext.BINDING_MODE,
                U03RiskAssessmentCandidate.VALID,
                null,
                Collections.<String>emptyList(),
                "DETERMINISTIC_FROZEN_RULE_EXECUTION",
                "u03-decision-1",
                U03RiskAssessmentCandidate.VALID,
                "NO_HIGH_RISK_SIGNAL",
                "U03_RULE_DECISION",
                "binding-substituted",
                governedRefs(),
                "acceptance-1",
                Collections.singletonList("evidence-1"),
                Collections.singletonList("source-1"),
                Collections.singletonList("provenance-1"),
                "u03-proposal-1",
                "COMMITTED",
                null,
                "audit-u03-1");

        U04ExecutionResult result = fixture.application.execute(
                conflicting,
                8,
                U04ScopeContext.current(true, true),
                ENV);

        assertEquals(U04ExecutionResult.ADMISSION_FAILED, result.getStatus());
        assertEquals(U04AdmissionService.UNTRUSTED_U03_RELEASE_SET, result.getAdmission().getReasonCode());
        assertNull(result.getDecision());
        assertEquals(0, fixture.repository.commitCalls());
    }

    @Test
    void missingProvenanceFailsClosedBeforeGateDecision() {
        Fixture fixture = new Fixture(8);
        U03OutboundHandoff missingProvenance = new U03OutboundHandoff(
                "consult-1",
                "cdp-1",
                7,
                Integer.valueOf(8),
                "thread-1",
                "run-1",
                "event-1",
                "corr-1",
                "trace-1",
                ENV,
                U03NonProductionExecutionContext.BINDING_MODE,
                U03RiskAssessmentCandidate.VALID,
                null,
                Collections.<String>emptyList(),
                "DETERMINISTIC_FROZEN_RULE_EXECUTION",
                "u03-decision-1",
                U03RiskAssessmentCandidate.VALID,
                "NO_HIGH_RISK_SIGNAL",
                "U03_RULE_DECISION",
                U03GovernedCandidateGateway.BINDING_ID,
                governedRefs(),
                "acceptance-1",
                Collections.singletonList("evidence-1"),
                Collections.singletonList("source-1"),
                Collections.<String>emptyList(),
                "u03-proposal-1",
                "COMMITTED",
                null,
                "audit-u03-1");

        U04ExecutionResult result = fixture.application.execute(
                missingProvenance,
                8,
                U04ScopeContext.current(true, true),
                ENV);

        assertEquals(U04ExecutionResult.ADMISSION_FAILED, result.getStatus());
        assertEquals(U04AdmissionService.MALFORMED_U03_HANDOFF, result.getAdmission().getReasonCode());
        assertNull(result.getDecision());
        assertEquals(0, fixture.repository.commitCalls());
    }

    @Test
    void gateOverrideAttemptFailsBeforeStateCommitter() {
        Fixture fixture = new Fixture(8);
        U04AdmissionResult admission = new U04AdmissionService().admit(
                validHandoff("HIGH_RISK"),
                8,
                U04ScopeContext.current(true, true),
                ENV);
        U04AdmittedInput input = admission.getAdmittedInput();
        U04SafetyGateDecision decision = new U04SafetyGatePolicy().decide(input);
        U04StateProposal proposal = new U04StateProposalFactory().create(input, decision);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> value =
                (java.util.Map<String, Object>) proposal.getStatePatch().operations.get(0).value;
        value.put("safety_gate", U04SafetyGateDecision.ALLOW);

        assertThrows(
                IllegalStateException.class,
                () -> fixture.commitService.commitNonProduction(input, decision, proposal));
        assertEquals(0, fixture.repository.readCalls());
        assertEquals(0, fixture.repository.commitCalls());
    }

    @Test
    void mutablePolicyAliasIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new U04ScopeContext(true, true, "latest"));
    }

    private static U03OutboundHandoff validHandoff(String disposition) {
        return validHandoff(disposition, governedRefs());
    }

    private static U03OutboundHandoff validHandoff(
            String disposition,
            List<String> governedReleaseRefs) {
        return new U03OutboundHandoff(
                "consult-1",
                "cdp-1",
                7,
                Integer.valueOf(8),
                "thread-1",
                "run-1",
                "event-1",
                "corr-1",
                "trace-1",
                ENV,
                U03NonProductionExecutionContext.BINDING_MODE,
                U03RiskAssessmentCandidate.VALID,
                null,
                Collections.<String>emptyList(),
                "DETERMINISTIC_FROZEN_RULE_EXECUTION",
                "u03-decision-1",
                U03RiskAssessmentCandidate.VALID,
                disposition,
                "U03_RULE_DECISION",
                U03GovernedCandidateGateway.BINDING_ID,
                governedReleaseRefs,
                "acceptance-1",
                Collections.singletonList("evidence-1"),
                Collections.singletonList("source-1"),
                Collections.singletonList("provenance-1"),
                "u03-proposal-1",
                "COMMITTED",
                null,
                "audit-u03-1");
    }

    private static U03OutboundHandoff failedHandoff() {
        return new U03OutboundHandoff(
                "consult-1",
                "cdp-1",
                7,
                null,
                "thread-1",
                "run-1",
                "event-1",
                "corr-1",
                "trace-1",
                ENV,
                U03NonProductionExecutionContext.BINDING_MODE,
                U03RiskAssessmentCandidate.FAILED,
                "INSUFFICIENT_INFORMATION",
                Collections.singletonList("RISK_ASSESSMENT_FAILED"),
                "RISK_ASSESSMENT_UNAVAILABLE",
                "u03-decision-1",
                U03RiskAssessmentCandidate.FAILED,
                null,
                "INSUFFICIENT_INFORMATION",
                U03GovernedCandidateGateway.BINDING_ID,
                governedRefs(),
                "acceptance-1",
                Collections.singletonList("evidence-1"),
                Collections.singletonList("source-1"),
                Collections.singletonList("provenance-1"),
                null,
                null,
                null,
                null);
    }

    private static List<String> governedRefs() {
        return new ArrayList<String>(Arrays.asList(
                U03GovernedCandidateGateway.BINDING_ID,
                U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF));
    }

    private static U05ReadinessInputManifest manifest(
            String context,
            int version,
            List<U05ReadinessInput> inputs) {
        String setIdentity = U05ReadinessInputManifest.semanticSetIdentity(
                "consult-1",
                "cdp-1",
                version,
                context,
                U05ReadinessInputManifest.RDP05_CONTRACT_VERSION,
                inputs);
        return new U05ReadinessInputManifest(
                "manifest-" + context,
                setIdentity,
                "consult-1",
                "cdp-1",
                version,
                context,
                U05ReadinessInputManifest.RDP05_CONTRACT_VERSION,
                inputs);
    }


    private static U05ReadinessInput present(String domain, String signal, int version) {
        return new U05ReadinessInput(
                "input-" + domain + "-" + signal,
                domain,
                "owner-" + domain,
                inputKind(domain),
                U05ReadinessInput.PRESENT,
                signal,
                "consult-1",
                "cdp-1",
                version,
                "decision-" + domain + "-" + signal,
                "state-" + domain,
                "app-evidence-" + domain,
                Collections.singletonList("evidence-" + domain),
                Collections.singletonList("rule-" + domain),
                "2026-09-21T08:00:00Z",
                U05ReadinessInput.CURRENT,
                null);
    }

    private static U05ReadinessInput applicability(String domain, String status, int version) {
        return new U05ReadinessInput(
                "app-" + domain + "-" + status,
                domain,
                "owner-" + domain,
                inputKind(domain),
                status,
                null,
                "consult-1",
                "cdp-1",
                version,
                "app-decision-" + domain + "-" + status,
                "state-" + domain,
                "app-evidence-" + domain + "-" + status,
                Collections.singletonList("evidence-" + domain),
                Collections.singletonList("rule-" + domain),
                "2026-09-21T08:00:00Z",
                U05ReadinessInput.CURRENT,
                U05ReadinessInput.STALE.equals(status) ? "invalidation-" + domain : null);
    }

    private static String inputKind(String domain) {
        if (U05ReadinessInput.F1.equals(domain)) return "SCOPE_OR_FRAMING";
        if (U05ReadinessInput.F2_CLARIFICATION.equals(domain)) return "CLARIFICATION_REQUIREMENT";
        if (U05ReadinessInput.F3.equals(domain)) return "ONLINE_INFORMATION_GAP";
        if (U05ReadinessInput.F5.equals(domain)) return "DDX_OR_MUST_EXCLUDE";
        return "OFFLINE_EVIDENCE";
    }


    private static final class Fixture {
        final List<String> order = new ArrayList<String>();
        final MechanicalVersionRepositoryFake repository = new MechanicalVersionRepositoryFake(order);
        final U04CommitService commitService;
        final U04NonProductionApplicationService application;

        Fixture(int currentVersion) {
            repository.seed("cdp-1", currentVersion);
            StateCommitter committer = new StateCommitter(
                    repository,
                    (capabilityId, capabilityVersion) ->
                            CapabilityPolicyPort.CapabilityDecision.authorized(),
                    (path, capabilityId) ->
                            FieldPermissionPort.FieldPermissionDecision.authorized(),
                    (cdpId, capabilityId) ->
                            ConsentPolicyPort.ConsentDecision.authorized(),
                    source -> SourceValidationPort.SourceDecision.authorized(),
                    new InMemoryIdempotencyFake(order),
                    new SyntheticAuditPortFake(CLOCK, order),
                    new RecordingCommitEventEvidenceFake(order),
                    CLOCK);
            commitService = new U04CommitService(committer);
            application = new U04NonProductionApplicationService(
                    new U04AdmissionService(),
                    new U04SafetyGatePolicy(),
                    new U04StateProposalFactory(),
                    commitService);
        }
    }
}
