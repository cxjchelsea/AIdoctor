package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityExecutionContext;
import com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard;
import com.aidoctor.diagnosis.state.committer.CommitReasonCodes;
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

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * S12 closure evidence for R5 N1-N13 mandatory fail-closed cases.
 *
 * <p>These tests deliberately use synthetic, non-clinical values. Each case proves
 * either that execution is rejected before the next owner boundary or that the
 * mechanical commit layer returns a non-committed result, and additionally asserts
 * that no prohibited clinical commit / downstream safety output is produced.</p>
 */
class U03FailClosedNegativeCoverageTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-17T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void n1StaleClinicalStateVersionFailsBeforeC02AndCommit() {
        AtomicInteger c02Calls = new AtomicInteger();
        U03AcceptedEvidenceAwareCandidateProvider provider =
                (command, capabilityBinding, releaseBinding, acceptedEvidence) -> {
                    c02Calls.incrementAndGet();
                    return U03RiskAssessmentCandidate.failed("SHOULD_NOT_RUN");
                };
        U03GovernedCandidateGateway gateway = gateway(provider);

        assertThrows(IllegalStateException.class, () -> {
            U03NonProductionExecutionContext stale = new U03NonProductionExecutionContext(
                    command(),
                    U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                    new U03AcceptedEvidenceBinding(
                            "acceptance-stale", 6,
                            Collections.singletonList("evidence-old"),
                            Collections.singletonList("source-old"),
                            Collections.singletonList("provenance-old")),
                    "ci-nonprod");
            gateway.assess(stale);
        });
        assertEquals(0, c02Calls.get());
    }

    @Test
    void n2MissingReleaseRefFailsBeforeRuntimeOutput() {
        assertThrows(IllegalArgumentException.class, () ->
                new U03ExplicitNonProductionReleaseRefs(
                        null,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF));
    }

    @Test
    void n3WrongReleaseRefFailsBeforeC02OrCommit() {
        U03ExplicitNonProductionReleaseRefs wrong = new U03ExplicitNonProductionReleaseRefs(
                "KR-U03-SOURCE-001@0.1.1-candidate",
                U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF);

        assertThrows(IllegalStateException.class, () ->
                new U03NonProductionExecutionContext(command(), wrong, evidence(), "ci-nonprod"));
    }

    @Test
    void n4CrossReleaseIncompatibleSetFailsBeforeRuntimeOutput() {
        U03ExplicitNonProductionReleaseRefs mixed = new U03ExplicitNonProductionReleaseRefs(
                U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                "PF-U03-C-POLICY-999");

        assertThrows(IllegalStateException.class, () ->
                new U03ResolvedNonProductionReleaseSet(
                        mixed, U03GovernedCandidateGateway.BINDING_ID));
    }

    @Test
    void n5DependencyFailureDoesNotInvokeD09CommitOrInventSafetyOutput() {
        AtomicInteger d09Calls = new AtomicInteger();
        U03AcceptedEvidenceAwareCandidateProvider dependencyFailure =
                (command, capabilityBinding, releaseBinding, acceptedEvidence) ->
                        U03RiskAssessmentCandidate.failed("SYNTHETIC_DEPENDENCY_FAILURE");
        U03NonProductionExecutionContext context = context();
        U03GovernedCandidateGateway.GovernedResult governed = gateway(dependencyFailure).assess(context);

        U03NonProductionDecisionPort d09 = new U03NonProductionDecisionPort() {
            @Override
            public U03DecisionOutcome decide(
                    U03NonProductionExecutionContext executionContext,
                    U03RiskAssessmentCandidate acceptedCandidate,
                    U03ResolvedNonProductionReleaseSet resolvedReleaseSet) {
                d09Calls.incrementAndGet();
                throw new AssertionError("D09 must not run after C02 dependency failure");
            }

            @Override
            public U03DecisionOutcome decide(
                    U03ExecutionCommand executionCommand,
                    U03RiskAssessmentCandidate candidate,
                    U03ReleaseBinding releaseBinding) {
                throw new AssertionError("historical D09 must not run");
            }
        };

        U03DecisionOutcome decision = new U03DecisionService(d09).decide(context, governed);
        U03OutboundHandoff failureHandoff = new U03OutboundProducer().produce(
                context, governed, decision, null, null);

        assertEquals(0, d09Calls.get());
        assertEquals(U03RiskAssessmentCandidate.FAILED, decision.getStatus());
        assertNull(decision.getOutcomeCode());
        assertEquals("FAILED", failureHandoff.getExecutionStatus());
        assertNull(failureHandoff.getDispositionCode());
        assertNull(failureHandoff.getCommittedClinicalStateVersion());
        assertNull(failureHandoff.getCommitStatus());
    }

    @Test
    void n6MalformedC02ResultFailsBeforeD09ProposalOrCommit() {
        AtomicInteger providerCalls = new AtomicInteger();
        U03AcceptedEvidenceAwareCandidateProvider malformed =
                (command, capabilityBinding, releaseBinding, acceptedEvidence) -> {
                    providerCalls.incrementAndGet();
                    return null;
                };

        assertThrows(IllegalStateException.class, () -> gateway(malformed).assess(context()));
        assertEquals(1, providerCalls.get());
    }

    @Test
    void n7UnsupportedProposalTypeCannotCommit() {
        CommitFixture fixture = new CommitFixture(7);
        U03NonProductionExecutionContext context = context();
        U03StateProposal proposal = validProposal(context);
        proposal.getStatePatch().envelope.contractName = "UnsupportedClinicalMutation";

        StateTypes.CommitResult result = fixture.service.commitNonProduction(context, proposal);

        assertFalse("COMMITTED".equals(result.status));
        assertEquals(0, fixture.repository.commitCalls());
        assertEquals(7, fixture.repository.currentVersion("cdp-1"));
    }

    @Test
    void n8DuplicateEventWithDifferentFingerprintConflictsWithoutSecondCommit() {
        CommitFixture fixture = new CommitFixture(7);
        U03NonProductionExecutionContext context = context();
        U03StateProposal proposal = validProposal(context);

        StateTypes.CommitResult first = fixture.service.commitNonProduction(context, proposal);
        proposal.getStatePatch().reasonCode = "SYNTHETIC_REPLAY_FINGERPRINT_CHANGE";
        StateTypes.CommitResult duplicate = fixture.service.commitNonProduction(context, proposal);

        assertEquals("COMMITTED", first.status);
        assertEquals("CONFLICT", duplicate.status);
        assertEquals(CommitReasonCodes.IDEMPOTENCY_MISMATCH, duplicate.reasonCode);
        assertEquals(1, fixture.repository.commitCalls());
        assertEquals(8, fixture.repository.currentVersion("cdp-1"));
    }

    @Test
    void n9StateCommitterVersionConflictProducesNoMutationOrOutbound() {
        CommitFixture fixture = new CommitFixture(8);
        U03NonProductionExecutionContext context = context();
        U03StateProposal proposal = validProposal(context);

        StateTypes.CommitResult conflict = fixture.service.commitNonProduction(context, proposal);

        assertEquals("CONFLICT", conflict.status);
        assertEquals(CommitReasonCodes.VERSION_MISMATCH, conflict.reasonCode);
        assertEquals(0, fixture.repository.commitCalls());
        assertEquals(8, fixture.repository.currentVersion("cdp-1"));
        assertThrows(IllegalStateException.class, () -> new U03OutboundProducer().produce(
                context,
                governed(context),
                decision(),
                proposal,
                conflict));
    }

    @Test
    void n10AttemptedDirectMutationBypassIsRejectedBeforeStateRepository() {
        CommitFixture fixture = new CommitFixture(7);
        U03NonProductionExecutionContext context = context();
        U03StateProposal proposal = validProposal(context);
        proposal.getStatePatch().envelope.capabilityId = "DIRECT_CDP_MUTATION";

        assertThrows(IllegalStateException.class, () ->
                fixture.service.commitNonProduction(context, proposal));
        assertEquals(0, fixture.repository.readCalls());
        assertEquals(0, fixture.repository.commitCalls());
        assertEquals(7, fixture.repository.currentVersion("cdp-1"));
    }

    @Test
    void n11ImplicitLatestReleaseSelectionIsRejectedBeforeRuntimeOutput() {
        assertThrows(IllegalArgumentException.class, () ->
                new U03ExplicitNonProductionReleaseRefs(
                        "KR-U03-SOURCE-001@latest",
                        U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF));
    }

    @Test
    void n12ProductionEnvironmentUseIsRejectedBeforeC02OrCommit() {
        AtomicInteger c02Calls = new AtomicInteger();
        U03AcceptedEvidenceAwareCandidateProvider provider =
                (command, capabilityBinding, releaseBinding, acceptedEvidence) -> {
                    c02Calls.incrementAndGet();
                    return validCandidate(command.clinicalStateVersion, capabilityBinding, releaseBinding, acceptedEvidence);
                };

        assertThrows(IllegalStateException.class, () -> {
            U03NonProductionExecutionContext production = new U03NonProductionExecutionContext(
                    command(),
                    U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                    evidence(),
                    "production-patient-traffic");
            gateway(provider).assess(production);
        });
        assertEquals(0, c02Calls.get());
    }

    @Test
    void n13OutboundProducerExposesNoUnauthorizedU04OwnerExecutionOrRoutingSurface() {
        boolean foundUnauthorizedSurface = false;
        for (Method method : U03OutboundProducer.class.getDeclaredMethods()) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            if (name.contains("u04") || name.contains("route") || name.contains("execute")) {
                foundUnauthorizedSurface = true;
            }
            if (method.getReturnType().getName().contains(".u04.")) {
                foundUnauthorizedSurface = true;
            }
            for (Class<?> parameterType : method.getParameterTypes()) {
                if (parameterType.getName().contains(".u04.")) {
                    foundUnauthorizedSurface = true;
                }
            }
        }
        assertFalse(foundUnauthorizedSurface);

        U03OutboundHandoff handoff = committedHandoff();
        assertEquals("COMMITTED", handoff.getCommitStatus());
        assertEquals("CAUTION", handoff.getDispositionCode());
        assertThrows(NoSuchMethodException.class, () ->
                U03OutboundHandoff.class.getMethod("getU04Decision"));
        assertThrows(NoSuchMethodException.class, () ->
                U03OutboundHandoff.class.getMethod("isSafe"));
        assertThrows(NoSuchMethodException.class, () ->
                U03OutboundHandoff.class.getMethod("isU04Passed"));
        assertThrows(NoSuchMethodException.class, () ->
                U03OutboundHandoff.class.getMethod("continueWithoutSafetyGate"));
    }

    private static U03GovernedCandidateGateway gateway(U03AcceptedEvidenceAwareCandidateProvider provider) {
        return new U03GovernedCandidateGateway(
                fixedGuard(capabilityBinding()),
                new U03ReleaseRegistry(),
                provider,
                true);
    }

    private static CapabilityInvocationGuard fixedGuard(CapabilityBindingRecord binding) {
        return new CapabilityInvocationGuard(null) {
            @Override
            public CapabilityBindingRecord authorize(
                    String bindingId,
                    String expectedCapabilityId,
                    CapabilityExecutionContext executionContext) {
                return binding;
            }
        };
    }

    private static U03NonProductionExecutionContext context() {
        return new U03NonProductionExecutionContext(
                command(),
                U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                evidence(),
                "ci-nonprod");
    }

    private static U03ExecutionCommand command() {
        return new U03ExecutionCommand(
                "consult-1", "thread-1", "run-1", "event-1",
                "cdp-1", 7, "corr-1", "trace-1");
    }

    private static U03AcceptedEvidenceBinding evidence() {
        return new U03AcceptedEvidenceBinding(
                "acceptance-1", 7,
                Collections.singletonList("evidence-1"),
                Collections.singletonList("source-1"),
                Collections.singletonList("accepted-provenance-1"));
    }

    private static CapabilityBindingRecord capabilityBinding() {
        LocalDateTime now = LocalDateTime.now();
        return new CapabilityBindingRecord(
                U03GovernedCandidateGateway.BINDING_ID,
                U03GovernedCandidateGateway.CAPABILITY_ID,
                "1.0.0",
                "capset-v1",
                U03GovernedCandidateGateway.SCOPE_VERSION,
                U03GovernedCandidateGateway.CONTRACT_VERSION,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ACTIVE,
                now.minusMinutes(1), null, now.minusMinutes(1));
    }

    private static U03RiskAssessmentCandidate validCandidate(
            int stateVersion,
            CapabilityBindingRecord capability,
            U03ReleaseBinding releaseBinding,
            U03AcceptedEvidenceBinding accepted) {
        return U03RiskAssessmentCandidate.valid(
                stateVersion,
                accepted.getEvidenceRefs(),
                0.8d,
                "SYNTHETIC_UNCERTAINTY",
                Collections.<String>emptyList(),
                accepted.getSourceRefs(),
                accepted.getProvenanceRefs(),
                capability.getBindingId(),
                capability.getCapabilityVersion(),
                releaseBinding.getRuleReleaseId(),
                releaseBinding.getKnowledgeReleaseId());
    }

    private static U03GovernedCandidateGateway.GovernedResult governed(
            U03NonProductionExecutionContext context) {
        CapabilityBindingRecord capability = capabilityBinding();
        U03ResolvedNonProductionReleaseSet resolved = new U03ResolvedNonProductionReleaseSet(
                context.getReleaseRefs(), capability.getBindingId());
        U03ReleaseBinding releaseBinding = resolved.asCandidateReleaseBinding();
        U03RiskAssessmentCandidate candidate = validCandidate(
                context.getCommand().clinicalStateVersion,
                capability,
                releaseBinding,
                context.requireAcceptedEvidenceBinding());
        return new U03GovernedCandidateGateway.GovernedResult(
                candidate, capability, releaseBinding, resolved);
    }

    private static U03DecisionOutcome decision() {
        return new U03DecisionOutcome(
                "decision-1",
                U03RiskAssessmentCandidate.VALID,
                "CAUTION",
                "SYNTHETIC_RULE_DECISION",
                Collections.singletonList("evidence-1"));
    }

    private static U03StateProposal validProposal(U03NonProductionExecutionContext context) {
        return new U03StateProposalFactory().createNonProductionValid(
                context, decision(), governed(context));
    }

    private static U03OutboundHandoff committedHandoff() {
        U03NonProductionExecutionContext context = context();
        U03GovernedCandidateGateway.GovernedResult governed = governed(context);
        U03DecisionOutcome decision = decision();
        U03StateProposal proposal = new U03StateProposalFactory().createNonProductionValid(
                context, decision, governed);
        CommitFixture fixture = new CommitFixture(7);
        StateTypes.CommitResult commit = fixture.service.commitNonProduction(context, proposal);
        assertEquals("COMMITTED", commit.status);
        return new U03OutboundProducer().produce(context, governed, decision, proposal, commit);
    }

    private static final class CommitFixture {
        final MechanicalVersionRepositoryFake repository =
                new MechanicalVersionRepositoryFake(new ArrayList<String>());
        final U03CommitService service;

        CommitFixture(int currentVersion) {
            repository.seed("cdp-1", currentVersion);
            StateCommitter committer = new StateCommitter(
                    repository,
                    (capabilityId, capabilityVersion) -> CapabilityPolicyPort.CapabilityDecision.authorized(),
                    (path, capabilityId) -> FieldPermissionPort.FieldPermissionDecision.authorized(),
                    (cdpId, capabilityId) -> ConsentPolicyPort.ConsentDecision.authorized(),
                    source -> SourceValidationPort.SourceDecision.authorized(),
                    new InMemoryIdempotencyFake(),
                    new SyntheticAuditPortFake(CLOCK),
                    new RecordingCommitEventEvidenceFake(),
                    CLOCK);
            service = new U03CommitService(committer);
        }
    }
}
