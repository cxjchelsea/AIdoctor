package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.u04.U04AdmissionService;
import com.aidoctor.diagnosis.runtime.u04.U04ExecutionResult;
import com.aidoctor.diagnosis.runtime.u04.U04NonProductionApplicationService;
import com.aidoctor.diagnosis.runtime.u04.U04SafetyGateDecision;
import com.aidoctor.diagnosis.runtime.u04.U04SafetyGatePolicy;
import com.aidoctor.diagnosis.runtime.u04.U04ScopeContext;
import com.aidoctor.diagnosis.runtime.u04.U04StateProposalFactory;
import com.aidoctor.diagnosis.runtime.u04.U04CommitService;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executes governed U04 cases and emits structured per-case durable evidence.
 *
 * <p>The evidence is produced from observed runtime objects, not inferred later
 * from test names.</p>
 */
class U04EvidenceHarnessTest {
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-09-18T00:00:00Z"), ZoneOffset.UTC);
    private static final String ENV = "ci-nonprod-u04";

    @Test
    void emitStructuredEvidenceForGovernedU04Cases() throws Exception {
        List<CaseEvidence> evidence = new ArrayList<CaseEvidence>();

        evidence.add(executeGateCase(
                "U04-EV-001",
                "VALID_NO_HIGH_RISK_SIGNAL",
                validHandoff("NO_HIGH_RISK_SIGNAL", "event-ev-001"),
                8,
                U04ScopeContext.current(true, true),
                "GATE_COMMITTED",
                U04SafetyGateDecision.ALLOW,
                true,
                false,
                false,
                false));

        evidence.add(executeGateCase(
                "U04-EV-002",
                "VALID_CAUTION",
                validHandoff("CAUTION", "event-ev-002"),
                8,
                U04ScopeContext.current(true, true),
                "GATE_COMMITTED",
                U04SafetyGateDecision.RESTRICTED,
                true,
                true,
                false,
                false));

        evidence.add(executeGateCase(
                "U04-EV-003",
                "VALID_HIGH_RISK",
                validHandoff("HIGH_RISK", "event-ev-003"),
                8,
                U04ScopeContext.current(true, true),
                "GATE_COMMITTED",
                U04SafetyGateDecision.BLOCKED,
                false,
                false,
                true,
                false));

        evidence.add(executeGateCase(
                "U04-EV-004",
                "U03_FAILED",
                failedHandoff("event-ev-004"),
                7,
                U04ScopeContext.current(true, true),
                "GATE_COMMITTED",
                U04SafetyGateDecision.UNAVAILABLE,
                false,
                false,
                false,
                true));

        evidence.add(executeGateCase(
                "U04-EV-005",
                "SCOPE_UNAVAILABLE",
                validHandoff("NO_HIGH_RISK_SIGNAL", "event-ev-005"),
                8,
                U04ScopeContext.current(false, false),
                "GATE_COMMITTED",
                U04SafetyGateDecision.UNAVAILABLE,
                false,
                false,
                false,
                true));

        evidence.add(executeAdmissionFailureCase(
                "U04-EV-006",
                "STALE_U03_HANDOFF",
                validHandoff("NO_HIGH_RISK_SIGNAL", "event-ev-006"),
                9,
                U04AdmissionService.STALE_U03_HANDOFF));

        List<String> wrongRefs = governedRefs();
        wrongRefs.set(2, "RR-U03-RISK-001@0.2.0-candidate");
        evidence.add(executeAdmissionFailureCase(
                "U04-EV-007",
                "WRONG_RELEASE_SET",
                validHandoff("NO_HIGH_RISK_SIGNAL", "event-ev-007", wrongRefs),
                8,
                U04AdmissionService.UNTRUSTED_U03_RELEASE_SET));

        evidence.add(executeAdmissionFailureCase(
                "U04-EV-008",
                "EXECUTION_DECISION_CONFLICT",
                conflictingStatusHandoff("event-ev-008"),
                8,
                U04AdmissionService.MALFORMED_U03_HANDOFF));

        evidence.add(executeAdmissionFailureCase(
                "U04-EV-009",
                "CAPABILITY_BINDING_SUBSTITUTION",
                bindingMismatchHandoff("event-ev-009"),
                8,
                U04AdmissionService.UNTRUSTED_U03_RELEASE_SET));

        evidence.add(executeAdmissionFailureCase(
                "U04-EV-010",
                "MISSING_PROVENANCE",
                missingProvenanceHandoff("event-ev-010"),
                8,
                U04AdmissionService.MALFORMED_U03_HANDOFF));

        evidence.add(executeAdmissionFailureCase(
                "U04-EV-011",
                "FAILED_REASON_MISMATCH",
                failedReasonMismatchHandoff("event-ev-011"),
                7,
                U04AdmissionService.MALFORMED_U03_HANDOFF));

        evidence.add(executeGateCase(
                "U04-EV-012",
                "HIGH_RISK_WITH_SCOPE_UNAVAILABLE",
                validHandoff("HIGH_RISK", "event-ev-012"),
                8,
                U04ScopeContext.current(false, false),
                "GATE_COMMITTED",
                U04SafetyGateDecision.BLOCKED,
                false,
                false,
                true,
                false));

        Path output = Paths.get("target", "u04-case-evidence.json");
        Files.createDirectories(output.getParent());
        Files.write(output, toJson(evidence).getBytes(StandardCharsets.UTF_8));

        assertEquals(12, evidence.size());
        for (CaseEvidence item : evidence) {
            assertTrue(item.pass);
        }
    }

    private static CaseEvidence executeGateCase(
            String caseId,
            String scenario,
            U03OutboundHandoff handoff,
            int currentVersion,
            U04ScopeContext scope,
            String expectedBoundary,
            String expectedGate,
            boolean expectedU05,
            boolean expectedRestricted,
            boolean expectedU11,
            boolean expectedU14) {
        Fixture fixture = new Fixture(currentVersion);
        U04ExecutionResult result =
                fixture.application.execute(handoff, currentVersion, scope, ENV);

        assertEquals(U04ExecutionResult.COMMIT_COMPLETE, result.getStatus());
        assertEquals(expectedGate, result.getDecision().getGate());
        assertEquals("COMMITTED", result.getCommitResult().status);
        assertEquals(expectedU05, result.getRoutingEligibility().isU05Eligible());
        assertEquals(
                expectedRestricted,
                result.getRoutingEligibility().isRestrictedContextRequired());
        assertEquals(expectedU11, result.getRoutingEligibility().isU11Eligible());
        assertEquals(expectedU14, result.getRoutingEligibility().isU14Eligible());

        CaseEvidence item = base(caseId, scenario, handoff, currentVersion);
        item.expectedBoundary = expectedBoundary;
        item.observedBoundary = "GATE_COMMITTED";
        item.expectedResult = expectedGate;
        item.observedResult = result.getDecision().getGate();
        item.observedGate = result.getDecision().getGate();
        item.typedFailureReason = null;
        item.proposalId = result.getProposal().getProposalId();
        item.commitStatus = result.getCommitResult().status;
        item.committedVersion = result.getCommitResult().committedVersion;
        item.auditId =
                result.getCommitResult().auditRef == null
                        ? null
                        : result.getCommitResult().auditRef.auditId;
        item.u05Eligible = Boolean.valueOf(result.getRoutingEligibility().isU05Eligible());
        item.restrictedContextRequired =
                Boolean.valueOf(result.getRoutingEligibility().isRestrictedContextRequired());
        item.u11Eligible = Boolean.valueOf(result.getRoutingEligibility().isU11Eligible());
        item.u14Eligible = Boolean.valueOf(result.getRoutingEligibility().isU14Eligible());
        item.pass = expectedBoundary.equals(item.observedBoundary)
                && expectedGate.equals(item.observedResult)
                && expectedU05 == item.u05Eligible.booleanValue()
                && expectedRestricted == item.restrictedContextRequired.booleanValue()
                && expectedU11 == item.u11Eligible.booleanValue()
                && expectedU14 == item.u14Eligible.booleanValue()
                && "COMMITTED".equals(item.commitStatus)
                && item.auditId != null;
        return item;
    }

    private static CaseEvidence executeAdmissionFailureCase(
            String caseId,
            String scenario,
            U03OutboundHandoff handoff,
            int currentVersion,
            String expectedReason) {
        Fixture fixture = new Fixture(currentVersion);
        U04ExecutionResult result =
                fixture.application.execute(
                        handoff,
                        currentVersion,
                        U04ScopeContext.current(true, true),
                        ENV);

        assertEquals(U04ExecutionResult.ADMISSION_FAILED, result.getStatus());
        assertEquals(expectedReason, result.getAdmission().getReasonCode());
        assertNull(result.getDecision());
        assertNull(result.getProposal());
        assertNull(result.getCommitResult());
        assertNull(result.getRoutingEligibility());
        assertEquals(0, fixture.repository.commitCalls());

        CaseEvidence item = base(caseId, scenario, handoff, currentVersion);
        item.expectedBoundary = "U04_CONSUMER_ADMISSION";
        item.observedBoundary = result.getAdmission().getBoundary();
        item.expectedResult = expectedReason;
        item.observedResult = result.getAdmission().getReasonCode();
        item.observedGate = null;
        item.typedFailureReason = result.getAdmission().getReasonCode();
        item.proposalId = null;
        item.commitStatus = null;
        item.committedVersion = null;
        item.auditId = null;
        item.u05Eligible = null;
        item.restrictedContextRequired = null;
        item.u11Eligible = null;
        item.u14Eligible = null;
        item.pass = item.expectedBoundary.equals(item.observedBoundary)
                && expectedReason.equals(item.observedResult);
        return item;
    }

    private static CaseEvidence base(
            String caseId,
            String scenario,
            U03OutboundHandoff handoff,
            int currentVersion) {
        CaseEvidence item = new CaseEvidence();
        item.caseId = caseId;
        item.scenario = scenario;
        item.sourceClinicalStateVersion = handoff.getSourceClinicalStateVersion();
        item.currentClinicalStateVersion = currentVersion;
        item.inputIdentity =
                handoff.getConsultationId()
                        + "|"
                        + handoff.getCdpId()
                        + "|"
                        + handoff.getEventId()
                        + "|"
                        + handoff.getDecisionId();
        item.u03ExecutionStatus = handoff.getExecutionStatus();
        item.u03DecisionStatus = handoff.getDecisionStatus();
        item.u03Disposition = handoff.getDispositionCode();
        item.policyRef = U04ScopeContext.FROZEN_POLICY_REF;
        item.correlationId = handoff.getCorrelationId();
        item.traceId = handoff.getTraceId();
        return item;
    }

    private static U03OutboundHandoff validHandoff(String disposition, String eventId) {
        return validHandoff(disposition, eventId, governedRefs());
    }

    private static U03OutboundHandoff validHandoff(
            String disposition,
            String eventId,
            List<String> refs) {
        return new U03OutboundHandoff(
                "consult-ev",
                "cdp-ev",
                7,
                Integer.valueOf(8),
                "thread-ev",
                "run-ev",
                eventId,
                "corr-" + eventId,
                "trace-" + eventId,
                ENV,
                U03NonProductionExecutionContext.BINDING_MODE,
                U03RiskAssessmentCandidate.VALID,
                null,
                Collections.<String>emptyList(),
                "DETERMINISTIC_FROZEN_RULE_EXECUTION",
                "u03-decision-" + eventId,
                U03RiskAssessmentCandidate.VALID,
                disposition,
                "U03_RULE_DECISION",
                U03GovernedCandidateGateway.BINDING_ID,
                refs,
                "acceptance-" + eventId,
                Collections.singletonList("evidence-" + eventId),
                Collections.singletonList("source-" + eventId),
                Collections.singletonList("provenance-" + eventId),
                "u03-proposal-" + eventId,
                "COMMITTED",
                null,
                "audit-u03-" + eventId);
    }

    private static U03OutboundHandoff failedHandoff(String eventId) {
        return new U03OutboundHandoff(
                "consult-ev",
                "cdp-ev",
                7,
                null,
                "thread-ev",
                "run-ev",
                eventId,
                "corr-" + eventId,
                "trace-" + eventId,
                ENV,
                U03NonProductionExecutionContext.BINDING_MODE,
                U03RiskAssessmentCandidate.FAILED,
                "INSUFFICIENT_INFORMATION",
                Collections.singletonList("RISK_ASSESSMENT_FAILED"),
                "RISK_ASSESSMENT_UNAVAILABLE",
                "u03-decision-" + eventId,
                U03RiskAssessmentCandidate.FAILED,
                null,
                "INSUFFICIENT_INFORMATION",
                U03GovernedCandidateGateway.BINDING_ID,
                governedRefs(),
                "acceptance-" + eventId,
                Collections.singletonList("evidence-" + eventId),
                Collections.singletonList("source-" + eventId),
                Collections.singletonList("provenance-" + eventId),
                null,
                null,
                null,
                null);
    }

    private static U03OutboundHandoff conflictingStatusHandoff(String eventId) {
        return new U03OutboundHandoff(
                "consult-ev", "cdp-ev", 7, Integer.valueOf(8),
                "thread-ev", "run-ev", eventId, "corr-" + eventId, "trace-" + eventId,
                ENV, U03NonProductionExecutionContext.BINDING_MODE,
                U03RiskAssessmentCandidate.FAILED,
                "INSUFFICIENT_INFORMATION",
                Collections.singletonList("RISK_ASSESSMENT_FAILED"),
                "RISK_ASSESSMENT_UNAVAILABLE",
                "u03-decision-" + eventId,
                U03RiskAssessmentCandidate.VALID,
                "NO_HIGH_RISK_SIGNAL",
                "U03_RULE_DECISION",
                U03GovernedCandidateGateway.BINDING_ID,
                governedRefs(),
                "acceptance-" + eventId,
                Collections.singletonList("evidence-" + eventId),
                Collections.singletonList("source-" + eventId),
                Collections.singletonList("provenance-" + eventId),
                "u03-proposal-" + eventId, "COMMITTED", null, "audit-u03-" + eventId);
    }

    private static U03OutboundHandoff bindingMismatchHandoff(String eventId) {
        U03OutboundHandoff valid = validHandoff("NO_HIGH_RISK_SIGNAL", eventId);
        return new U03OutboundHandoff(
                valid.getConsultationId(), valid.getCdpId(),
                valid.getSourceClinicalStateVersion(), valid.getCommittedClinicalStateVersion(),
                valid.getThreadId(), valid.getRunId(), valid.getEventId(),
                valid.getCorrelationId(), valid.getTraceId(), valid.getEnvironmentId(),
                valid.getBindingMode(), valid.getExecutionStatus(),
                valid.getExecutionFailureReasonCode(), valid.getExecutionLimitations(),
                valid.getExecutionUncertainty(), valid.getDecisionId(),
                valid.getDecisionStatus(), valid.getDispositionCode(),
                valid.getDecisionReasonCode(), "binding-substituted",
                valid.getGovernedReleaseRefs(), valid.getAcceptanceRef(),
                valid.getAcceptedEvidenceRefs(), valid.getAcceptedSourceRefs(),
                valid.getAcceptedProvenanceRefs(), valid.getProposalId(),
                valid.getCommitStatus(), valid.getCommitReasonCode(),
                valid.getCommitAuditId());
    }

    private static U03OutboundHandoff missingProvenanceHandoff(String eventId) {
        U03OutboundHandoff valid = validHandoff("NO_HIGH_RISK_SIGNAL", eventId);
        return new U03OutboundHandoff(
                valid.getConsultationId(), valid.getCdpId(),
                valid.getSourceClinicalStateVersion(), valid.getCommittedClinicalStateVersion(),
                valid.getThreadId(), valid.getRunId(), valid.getEventId(),
                valid.getCorrelationId(), valid.getTraceId(), valid.getEnvironmentId(),
                valid.getBindingMode(), valid.getExecutionStatus(),
                valid.getExecutionFailureReasonCode(), valid.getExecutionLimitations(),
                valid.getExecutionUncertainty(), valid.getDecisionId(),
                valid.getDecisionStatus(), valid.getDispositionCode(),
                valid.getDecisionReasonCode(), valid.getCapabilityBindingId(),
                valid.getGovernedReleaseRefs(), valid.getAcceptanceRef(),
                valid.getAcceptedEvidenceRefs(), valid.getAcceptedSourceRefs(),
                Collections.<String>emptyList(), valid.getProposalId(),
                valid.getCommitStatus(), valid.getCommitReasonCode(),
                valid.getCommitAuditId());
    }

    private static U03OutboundHandoff failedReasonMismatchHandoff(String eventId) {
        U03OutboundHandoff failed = failedHandoff(eventId);
        return new U03OutboundHandoff(
                failed.getConsultationId(), failed.getCdpId(),
                failed.getSourceClinicalStateVersion(), failed.getCommittedClinicalStateVersion(),
                failed.getThreadId(), failed.getRunId(), failed.getEventId(),
                failed.getCorrelationId(), failed.getTraceId(), failed.getEnvironmentId(),
                failed.getBindingMode(), failed.getExecutionStatus(),
                failed.getExecutionFailureReasonCode(), failed.getExecutionLimitations(),
                failed.getExecutionUncertainty(), failed.getDecisionId(),
                failed.getDecisionStatus(), failed.getDispositionCode(),
                "OTHER_FAILURE", failed.getCapabilityBindingId(),
                failed.getGovernedReleaseRefs(), failed.getAcceptanceRef(),
                failed.getAcceptedEvidenceRefs(), failed.getAcceptedSourceRefs(),
                failed.getAcceptedProvenanceRefs(), failed.getProposalId(),
                failed.getCommitStatus(), failed.getCommitReasonCode(),
                failed.getCommitAuditId());
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

    private static String toJson(List<CaseEvidence> items) {
        StringBuilder out = new StringBuilder();
        out.append("{\n  \"schema\": \"U04_CASE_EVIDENCE_V0_1\",\n  \"cases\": [\n");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) out.append(",\n");
            out.append(items.get(i).toJson());
        }
        out.append("\n  ]\n}\n");
        return out.toString();
    }

    private static String json(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String bool(Boolean value) {
        return value == null ? "null" : value.toString();
    }

    private static String integer(Integer value) {
        return value == null ? "null" : value.toString();
    }

    private static final class CaseEvidence {
        String caseId;
        String scenario;
        int sourceClinicalStateVersion;
        int currentClinicalStateVersion;
        String inputIdentity;
        String u03ExecutionStatus;
        String u03DecisionStatus;
        String u03Disposition;
        String policyRef;
        String correlationId;
        String traceId;
        String expectedBoundary;
        String observedBoundary;
        String expectedResult;
        String observedResult;
        String observedGate;
        String typedFailureReason;
        String proposalId;
        String commitStatus;
        Integer committedVersion;
        String auditId;
        Boolean u05Eligible;
        Boolean restrictedContextRequired;
        Boolean u11Eligible;
        Boolean u14Eligible;
        boolean pass;

        String toJson() {
            return "    {"
                    + "\n      \"case_id\": " + json(caseId)
                    + ",\n      \"scenario\": " + json(scenario)
                    + ",\n      \"source_clinical_state_version\": " + sourceClinicalStateVersion
                    + ",\n      \"current_clinical_state_version\": " + currentClinicalStateVersion
                    + ",\n      \"input_identity\": " + json(inputIdentity)
                    + ",\n      \"u03_execution_status\": " + json(u03ExecutionStatus)
                    + ",\n      \"u03_decision_status\": " + json(u03DecisionStatus)
                    + ",\n      \"u03_disposition\": " + json(u03Disposition)
                    + ",\n      \"policy_ref\": " + json(policyRef)
                    + ",\n      \"correlation_id\": " + json(correlationId)
                    + ",\n      \"trace_id\": " + json(traceId)
                    + ",\n      \"expected_boundary\": " + json(expectedBoundary)
                    + ",\n      \"observed_boundary\": " + json(observedBoundary)
                    + ",\n      \"expected_result\": " + json(expectedResult)
                    + ",\n      \"observed_result\": " + json(observedResult)
                    + ",\n      \"observed_gate\": " + json(observedGate)
                    + ",\n      \"typed_failure_reason\": " + json(typedFailureReason)
                    + ",\n      \"proposal_id\": " + json(proposalId)
                    + ",\n      \"commit_status\": " + json(commitStatus)
                    + ",\n      \"committed_version\": " + integer(committedVersion)
                    + ",\n      \"audit_id\": " + json(auditId)
                    + ",\n      \"routing_eligibility\": {"
                    + "\"u05\": " + bool(u05Eligible)
                    + ", \"restricted_context\": " + bool(restrictedContextRequired)
                    + ", \"u11\": " + bool(u11Eligible)
                    + ", \"u14\": " + bool(u14Eligible)
                    + "}"
                    + ",\n      \"pass\": " + pass
                    + "\n    }";
        }
    }

    private static final class Fixture {
        final List<String> order = new ArrayList<String>();
        final MechanicalVersionRepositoryFake repository =
                new MechanicalVersionRepositoryFake(order);
        final U04NonProductionApplicationService application;

        Fixture(int currentVersion) {
            repository.seed("cdp-ev", currentVersion);
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
            application = new U04NonProductionApplicationService(
                    new U04AdmissionService(),
                    new U04SafetyGatePolicy(),
                    new U04StateProposalFactory(),
                    new U04CommitService(committer));
        }
    }
}
