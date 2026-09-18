package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;
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
