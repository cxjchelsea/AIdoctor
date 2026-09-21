package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.contracts.v1.StateTypes;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class U05NonProductionClinicalReadinessTest {
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-09-21T08:00:00Z"), ZoneOffset.UTC);
    private static final String ENV = "ci-nonprod-u05";
    private static final int VERSION = 12;

    @Test
    void pol005ReadyCommitsAndRoutesOnlyToU08Eligibility() {
        U05ReadinessInputManifest manifest = pol005Manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                VERSION);
        Fixture fixture = new Fixture(VERSION, U05DownstreamPermissionDecision.PERMITTED);

        U05ExecutionResult result = fixture.application.execute(
                request(
                        manifest,
                        U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW,
                        null,
                        null),
                manifest,
                authority(
                        U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW,
                        null,
                        null,
                        true));

        assertEquals(U05ExecutionResult.ROUTING_COMPLETE, result.getStatus());
        assertEquals(U05ClinicalReadinessDecision.DECIDED, result.getDecision().getDecisionStatus());
        assertEquals(
                U05ClinicalReadinessDecision.READY_FOR_CLINICAL_ANALYSIS,
                result.getDecision().getClinicalReadiness());
        assertEquals("D03-POL-005", result.getDecision().getPolicyRuleRef());
        assertEquals("COMMITTED", result.getCommitResult().status);
        assertEquals(Integer.valueOf(VERSION + 1), result.getCommitResult().committedVersion);
        assertEquals(U05DownstreamRoutingDecision.ELIGIBLE, result.getRoutingDecision().getRoutingStatus());
        assertEquals(U05DownstreamRoutingDecision.TO_U08_CLINICAL_ANALYSIS,
                result.getRoutingDecision().getDownstreamConsequence());
        assertEquals("U08", result.getRoutingDecision().getTargetUnitId());
        assertNotNull(result.getRoutingDecision().getEligibility());
        assertEquals(1, fixture.repository.commitCalls());
    }

    @Test
    void blockingOfflineEvidenceOutranksCanAskMore() {
        List<U05ReadinessInput> inputs = new ArrayList<U05ReadinessInput>();
        inputs.add(present(U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION));
        inputs.add(present(U05ReadinessInput.F3, U05ReadinessInput.CAN_ASK_MORE, VERSION));
        inputs.add(applicability(U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION));
        inputs.add(present(U05ReadinessInput.F6, U05ReadinessInput.NEEDS_OFFLINE_EVIDENCE, VERSION));
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.POST_OFFLINE_ASSESSMENT, VERSION, inputs);
        Fixture fixture = new Fixture(VERSION, U05DownstreamPermissionDecision.PERMITTED);

        U05ExecutionResult result = fixture.application.execute(
                request(
                        manifest,
                        U05ConsumerInboundRequest.POST_OFFLINE_ASSESSMENT,
                        U05ConsumerInboundRequest.GATE_ALLOW,
                        null,
                        null),
                manifest,
                authority(
                        U05ConsumerInboundRequest.POST_OFFLINE_ASSESSMENT,
                        U05ConsumerInboundRequest.GATE_ALLOW,
                        null,
                        null,
                        true));

        assertEquals(U05ClinicalReadinessDecision.NEEDS_OFFLINE_EVIDENCE,
                result.getDecision().getClinicalReadiness());
        assertEquals("D03-POL-002", result.getDecision().getPolicyRuleRef());
        assertEquals(U05DownstreamRoutingDecision.TO_U10_OFFLINE_EVIDENCE,
                result.getRoutingDecision().getDownstreamConsequence());
        assertEquals("U10", result.getRoutingDecision().getTargetUnitId());
    }

    @Test
    void restrictedPermissionProvenanceIsPreservedAndDownstreamPermissionIsSeparate() {
        U05ReadinessInputManifest manifest = pol005Manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                VERSION);
        Fixture fixture = new Fixture(VERSION, U05DownstreamPermissionDecision.PERMITTED);

        U05ExecutionResult result = fixture.application.execute(
                request(
                        manifest,
                        U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_RESTRICTED,
                        "restricted-ctx-1",
                        "permission-u05-eval-1"),
                manifest,
                authority(
                        U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_RESTRICTED,
                        "restricted-ctx-1",
                        "permission-u05-eval-1",
                        true));

        assertEquals("permission-u05-eval-1", result.getAdmission().getRestrictedPermissionRef());
        assertEquals("permission-u05-eval-1", result.getAdmission().getAdmittedInput().getAcceptedRestrictedPermissionRef());
        assertEquals("permission-u05-eval-1", result.getDecision().getRestrictedPermissionRef());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload =
                (Map<String, Object>) result.getProposal().getStatePatch().operations.get(0).value;
        assertEquals("permission-u05-eval-1", payload.get("source_restricted_permission_ref"));

        assertEquals(U05DownstreamRoutingDecision.ELIGIBLE, result.getRoutingDecision().getRoutingStatus());
        assertEquals("permission-downstream-1", result.getRoutingDecision().getDownstreamPermissionRef());
        assertEquals("permission-downstream-1",
                result.getRoutingDecision().getEligibility().getDownstreamPermissionRef());
    }

    @Test
    void restrictedDownstreamPermissionDeniedPreemptsWithoutEligibility() {
        U05ReadinessInputManifest manifest = pol005Manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                VERSION);
        Fixture fixture = new Fixture(VERSION, U05DownstreamPermissionDecision.DENIED);

        U05ExecutionResult result = fixture.application.execute(
                request(
                        manifest,
                        U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_RESTRICTED,
                        "restricted-ctx-1",
                        "permission-u05-eval-1"),
                manifest,
                authority(
                        U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_RESTRICTED,
                        "restricted-ctx-1",
                        "permission-u05-eval-1",
                        true));

        assertEquals(U05DownstreamRoutingDecision.PREEMPTED, result.getRoutingDecision().getRoutingStatus());
        assertNull(result.getRoutingDecision().getEligibility());
        assertNull(result.getRoutingDecision().getDownstreamConsequence());
        assertEquals(1, fixture.repository.commitCalls());
    }

    @Test
    void exactReplayReattachesAdmissionAndStateCommitterDoesNotCommitTwice() {
        U05ReadinessInputManifest manifest = pol005Manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                VERSION);
        Fixture fixture = new Fixture(VERSION, U05DownstreamPermissionDecision.PERMITTED);
        U05ConsumerInboundRequest request = request(
                manifest,
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW,
                null,
                null);
        U05AdmissionAuthoritySnapshot authority = authority(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW,
                null,
                null,
                true);

        U05ExecutionResult first = fixture.application.execute(request, manifest, authority);
        U05ExecutionResult replay = fixture.application.execute(request, manifest, authority);

        assertEquals("COMMITTED", first.getCommitResult().status);
        assertEquals("COMMITTED", replay.getCommitResult().status);
        assertEquals(first.getCommitResult().committedVersion, replay.getCommitResult().committedVersion);
        assertEquals(U05AdmissionResult.REATTACHED, replay.getAdmission().getReplayDisposition());
        assertEquals(U05DownstreamRoutingDecision.REATTACHED,
                replay.getRoutingDecision().getReplayDisposition());
        assertEquals(1, fixture.repository.commitCalls());
    }

    @Test
    void mutationStaleF6IsRejectedBeforeD03AndCommit() {
        List<U05ReadinessInput> inputs = new ArrayList<U05ReadinessInput>();
        inputs.add(present(U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION));
        inputs.add(present(U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP, VERSION));
        inputs.add(applicability(U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION));
        inputs.add(applicability(U05ReadinessInput.F6, U05ReadinessInput.STALE, VERSION));
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.POST_USER_FACT_UPDATE, VERSION, inputs);
        Fixture fixture = new Fixture(VERSION, U05DownstreamPermissionDecision.PERMITTED);

        U05ExecutionResult result = fixture.application.execute(
                request(
                        manifest,
                        U05ConsumerInboundRequest.POST_USER_FACT_UPDATE,
                        U05ConsumerInboundRequest.GATE_ALLOW,
                        null,
                        null),
                manifest,
                authority(
                        U05ConsumerInboundRequest.POST_USER_FACT_UPDATE,
                        U05ConsumerInboundRequest.GATE_ALLOW,
                        null,
                        null,
                        false));

        assertEquals(U05ExecutionResult.ADMISSION_REJECTED, result.getStatus());
        assertEquals(U05AdmissionService.PENDING_OWNER_RECOMPUTATION,
                result.getAdmission().getReasonCode());
        assertNull(result.getDecision());
        assertEquals(0, fixture.repository.commitCalls());
    }

    @Test
    void requiredFailedF6BecomesInputFailureAndNeverCommits() {
        List<U05ReadinessInput> inputs = new ArrayList<U05ReadinessInput>();
        inputs.add(present(U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION));
        inputs.add(present(U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP, VERSION));
        inputs.add(applicability(U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION));
        inputs.add(applicability(U05ReadinessInput.F6, U05ReadinessInput.FAILED, VERSION));
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.POST_OFFLINE_ASSESSMENT, VERSION, inputs);
        Fixture fixture = new Fixture(VERSION, U05DownstreamPermissionDecision.PERMITTED);

        U05ExecutionResult result = fixture.application.execute(
                request(
                        manifest,
                        U05ConsumerInboundRequest.POST_OFFLINE_ASSESSMENT,
                        U05ConsumerInboundRequest.GATE_ALLOW,
                        null,
                        null),
                manifest,
                authority(
                        U05ConsumerInboundRequest.POST_OFFLINE_ASSESSMENT,
                        U05ConsumerInboundRequest.GATE_ALLOW,
                        null,
                        null,
                        true));

        assertEquals(U05ExecutionResult.D03_INPUT_FAILURE, result.getStatus());
        assertEquals(U05ClinicalReadinessDecision.INPUT_FAILURE, result.getDecision().getDecisionStatus());
        assertNull(result.getDecision().getClinicalReadiness());
        assertEquals(0, fixture.repository.commitCalls());
    }

    @Test
    void pol011ReadyAfterCurrentF6NotNeededRoutesToU08() {
        List<U05ReadinessInput> inputs = new ArrayList<U05ReadinessInput>();
        inputs.add(present(U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION));
        inputs.add(present(U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP, VERSION));
        inputs.add(applicability(U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION));
        inputs.add(present(U05ReadinessInput.F6, U05ReadinessInput.NO_BLOCKING_OFFLINE_EVIDENCE_NEED, VERSION));
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.POST_OFFLINE_ASSESSMENT, VERSION, inputs);
        Fixture fixture = new Fixture(VERSION, U05DownstreamPermissionDecision.PERMITTED);

        U05ExecutionResult result = fixture.application.execute(
                request(
                        manifest,
                        U05ConsumerInboundRequest.POST_OFFLINE_ASSESSMENT,
                        U05ConsumerInboundRequest.GATE_ALLOW,
                        null,
                        null),
                manifest,
                authority(
                        U05ConsumerInboundRequest.POST_OFFLINE_ASSESSMENT,
                        U05ConsumerInboundRequest.GATE_ALLOW,
                        null,
                        null,
                        true));

        assertEquals(U05ClinicalReadinessDecision.READY_FOR_CLINICAL_ANALYSIS,
                result.getDecision().getClinicalReadiness());
        assertEquals("D03-POL-011", result.getDecision().getPolicyRuleRef());
        assertEquals("U08", result.getRoutingDecision().getTargetUnitId());
    }

    @Test
    void productionEnvironmentIsRejectedBeforeD03OrCommit() {
        U05ReadinessInputManifest manifest = pol005Manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                VERSION);
        Fixture fixture = new Fixture(VERSION, U05DownstreamPermissionDecision.PERMITTED);

        U05ConsumerInboundRequest request = request(
                manifest,
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW,
                null,
                null,
                "production");

        U05ExecutionResult result = fixture.application.execute(
                request,
                manifest,
                authority(
                        U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW,
                        null,
                        null,
                        true,
                        "production",
                        false));

        assertEquals(U05ExecutionResult.ADMISSION_REJECTED, result.getStatus());
        assertEquals(U05AdmissionService.ENVIRONMENT_NOT_AUTHORIZED, result.getAdmission().getReasonCode());
        assertEquals(0, fixture.repository.commitCalls());
    }

    private static U05ReadinessInputManifest pol005Manifest(String context, int version) {
        return manifest(
                context,
                version,
                Arrays.asList(
                        present(U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, version),
                        present(U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP, version),
                        applicability(U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, version),
                        applicability(U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE, version)));
    }

    private static U05ReadinessInputManifest manifest(
            String context,
            int version,
            List<U05ReadinessInput> inputs) {
        String setIdentity = U05ReadinessInputManifest.semanticSetIdentity(
                "consult-1", "cdp-1", version, context, inputs);
        return new U05ReadinessInputManifest(
                "manifest-" + context,
                setIdentity,
                "consult-1",
                "cdp-1",
                version,
                context,
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

    private static U05ConsumerInboundRequest request(
            U05ReadinessInputManifest manifest,
            String context,
            String gate,
            String restrictedContext,
            String restrictedPermission) {
        return request(manifest, context, gate, restrictedContext, restrictedPermission, ENV);
    }

    private static U05ConsumerInboundRequest request(
            U05ReadinessInputManifest manifest,
            String context,
            String gate,
            String restrictedContext,
            String restrictedPermission,
            String environment) {
        String routeSource = routeSource(context);
        String routeConsequence =
                U05ConsumerInboundRequest.CLINICAL_CONTINUATION_ROUTING.equals(routeSource)
                        ? U05ConsumerInboundRequest.TO_U05_CLINICAL_READINESS
                        : null;
        return new U05ConsumerInboundRequest(
                "request-1",
                "consult-1",
                "cdp-1",
                VERSION,
                "clinical-state-cdp-1-v12",
                context,
                routeSource,
                "route-source-" + context,
                routeConsequence,
                "u04-gate-1",
                "u04-gate-commit-1",
                gate,
                routeAuthType(context),
                "route-auth-" + context,
                "routing-policy-v1",
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context) ? "A1" : null,
                restrictedContext,
                restrictedPermission,
                manifest.getManifestRef(),
                manifest.getSetIdentity(),
                manifest.presentInputRefs(),
                "canonical-event-1",
                "business-event-1",
                "corr-1",
                "trace-1",
                U05AdmissionService.CONTRACT_VERSION,
                environment,
                "2026-09-21T08:00:00Z");
    }

    private static U05AdmissionAuthoritySnapshot authority(
            String context,
            String gate,
            String restrictedContext,
            String restrictedPermission,
            boolean restrictedPermitted) {
        return authority(context, gate, restrictedContext, restrictedPermission, restrictedPermitted, ENV, true);
    }

    private static U05AdmissionAuthoritySnapshot authority(
            String context,
            String gate,
            String restrictedContext,
            String restrictedPermission,
            boolean restrictedPermitted,
            String environment,
            boolean environmentAuthorized) {
        String routeSource = routeSource(context);
        String consequence =
                U05ConsumerInboundRequest.CLINICAL_CONTINUATION_ROUTING.equals(routeSource)
                        ? U05ConsumerInboundRequest.TO_U05_CLINICAL_READINESS
                        : null;
        boolean a1 = U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context);
        return new U05AdmissionAuthoritySnapshot(
                "consult-1",
                "cdp-1",
                VERSION,
                "clinical-state-cdp-1-v12",
                environment,
                environmentAuthorized,
                true,
                true,
                "u04-gate-1",
                "u04-gate-commit-1",
                gate,
                true,
                "route-source-" + context,
                routeAuthType(context),
                "route-auth-" + context,
                consequence,
                "routing-policy-v1",
                restrictedContext,
                restrictedPermission,
                restrictedPermitted,
                a1 ? "A1" : null,
                a1,
                a1,
                a1,
                false,
                null);
    }

    private static String routeSource(String context) {
        if (U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context)) {
            return U05ConsumerInboundRequest.U04_A1_POST_BARRIER_ROUTING;
        }
        if (U05ConsumerInboundRequest.POST_SAFETY_INITIAL.equals(context)) {
            return U05ConsumerInboundRequest.U04_ORDINARY_ROUTING;
        }
        return U05ConsumerInboundRequest.CLINICAL_CONTINUATION_ROUTING;
    }

    private static String routeAuthType(String context) {
        if (U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context)) {
            return "U04_A1_ROUTING_AUTHORIZATION";
        }
        if (U05ConsumerInboundRequest.POST_SAFETY_INITIAL.equals(context)) {
            return "U04_ORDINARY_ROUTING_AUTHORIZATION";
        }
        return "CLINICAL_CONTINUATION_ROUTING_DECISION";
    }

    private static final class Fixture {
        final MechanicalVersionRepositoryFake repository = new MechanicalVersionRepositoryFake(new ArrayList<String>());
        final U05NonProductionApplicationService application;

        Fixture(int currentVersion, final String downstreamPermissionStatus) {
            repository.seed("cdp-1", currentVersion);
            List<String> order = new ArrayList<String>();
            StateCommitter committer = new StateCommitter(
                    repository,
                    (capabilityId, capabilityVersion) ->
                            CapabilityPolicyPort.CapabilityDecision.authorized(),
                    (statePath, capabilityId) ->
                            FieldPermissionPort.FieldPermissionDecision.authorized(),
                    (cdpId, capabilityId) ->
                            ConsentPolicyPort.ConsentDecision.authorized(),
                    source -> SourceValidationPort.SourceDecision.authorized(),
                    new InMemoryIdempotencyFake(order),
                    new SyntheticAuditPortFake(CLOCK, order),
                    new RecordingCommitEventEvidenceFake(order),
                    CLOCK);

            U05AdmissionService admissionService =
                    new U05AdmissionService(new U05InMemoryAdmissionLedger());
            U05ClinicalReadinessPolicy policy = new U05ClinicalReadinessPolicy();
            U05ReadinessStateProposalFactory proposalFactory =
                    new U05ReadinessStateProposalFactory();
            U05CommitService commitService = new U05CommitService(committer);

            U05RoutingCurrentnessPort currentnessPort =
                    (input, decision, evidence) ->
                            new U05RoutingCurrentness(
                                    evidence.getCommittedClinicalStateVersion(),
                                    "routing-context-" + evidence.getEffectId(),
                                    true,
                                    true,
                                    true,
                                    true,
                                    true,
                                    input.getAcceptedU04GateRef(),
                                    input.getGateValue(),
                                    input.getAcceptedRestrictedContextRef());

            U05DownstreamPermissionPort permissionPort =
                    (input, evidence, currentness, consequence, targetUnitId, targetAction) ->
                            new U05DownstreamPermissionDecision(
                                    "downstream-permission-decision-" + targetUnitId,
                                    input.getConsultationId(),
                                    input.getCdpId(),
                                    currentness.getCurrentU04GateRef(),
                                    input.getAcceptedRestrictedContextRef(),
                                    consequence,
                                    targetAction,
                                    targetUnitId,
                                    downstreamPermissionStatus,
                                    U05DownstreamPermissionDecision.PERMITTED.equals(downstreamPermissionStatus)
                                            ? "permission-downstream-1" : null,
                                    "safety-permission-policy",
                                    "v1",
                                    "CURRENT");

            U05RoutingService routingService =
                    new U05RoutingService(new U05InMemoryRouteLedger(), permissionPort);

            application = new U05NonProductionApplicationService(
                    admissionService,
                    policy,
                    proposalFactory,
                    commitService,
                    currentnessPort,
                    routingService);
        }
    }
}
