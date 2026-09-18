package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityExecutionContext;
import com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
import com.aidoctor.diagnosis.state.committer.fakes.InMemoryIdempotencyFake;
import com.aidoctor.diagnosis.state.committer.fakes.MechanicalVersionRepositoryFake;
import com.aidoctor.diagnosis.state.committer.fakes.RecordingCommitEventEvidenceFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticAuditPortFake;
import com.aidoctor.diagnosis.state.committer.ports.CapabilityPolicyPort;
import com.aidoctor.diagnosis.state.committer.ports.ConsentPolicyPort;
import com.aidoctor.diagnosis.state.committer.ports.FieldPermissionPort;
import com.aidoctor.diagnosis.state.committer.ports.SourceValidationPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CD-08 authorized non-production post-implementation clinical validation harness.
 *
 * <p>The frozen Python Gate-C package remains the source of fixture/expected semantics.
 * This test only maps those frozen fixtures into the real Java CD-07R runtime. It does
 * not create new clinical truth, U04 execution, production mutation, or patient traffic.</p>
 */
class U03Cd08FrozenClinicalValidationTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-18T00:00:00Z"), ZoneOffset.UTC);

    private static final List<String> ACCEPTED_EVIDENCE_REFS = Collections.unmodifiableList(Arrays.asList(
            "EV-RF-RESP-001",
            "EV-MNM-NEURO-001",
            "EV-MNM-NEURO-002",
            "EV-MNM-CARD-001",
            "EV-RF-ALLERGY-001",
            "EV-RF-APPEAR-001",
            "EV-RF-NEURO-001",
            "EV-VS-SEPSIS-001",
            "EV-VS-SEPSIS-002",
            "EV-VS-SEPSIS-003",
            "EV-RF-SEPSIS-001"));

    @Test
    void executesFrozenThirtyGoldenAndNineteenCriticalSafetyScenarios() throws Exception {
        String casesProperty = System.getProperty("u03.cd08.cases");
        String reportProperty = System.getProperty("u03.cd08.report");
        Assumptions.assumeTrue(
                casesProperty != null && !casesProperty.trim().isEmpty()
                        && reportProperty != null && !reportProperty.trim().isEmpty(),
                "CD-08 harness runs only under explicit authorized validation workflow");
        ObjectMapper mapper = new ObjectMapper();
        Path casesPath = Paths.get(casesProperty);
        Path reportPath = Paths.get(reportProperty);

        Map<String, Object> root = mapper.readValue(
                casesPath.toFile(), new TypeReference<Map<String, Object>>() {});

        List<Map<String, Object>> golden = listOfMaps(root.get("golden"));
        List<Map<String, Object>> safety = listOfMaps(root.get("safety"));
        assertEquals(30, golden.size(), "frozen Golden count");
        assertEquals(19, safety.size(), "frozen Critical Safety count");

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> spec : golden) {
            results.add(runGolden(spec));
        }
        for (Map<String, Object> spec : safety) {
            results.add(runSafety(spec));
        }

        int passed = 0;
        List<String> failedIds = new ArrayList<String>();
        for (Map<String, Object> result : results) {
            if (Boolean.TRUE.equals(result.get("pass"))) {
                passed++;
            } else {
                failedIds.add(String.valueOf(result.get("case_id")));
            }
        }

        Map<String, Object> report = new LinkedHashMap<String, Object>();
        report.put("schema", "U03_CD08_RUNTIME_VALIDATION_RESULT_V1");
        report.put("authorization_id", "AUTH-U03-CD08-CLINICAL-VALIDATION-EXEC-001");
        report.put("authorization_scope", "NON_PRODUCTION_POST_IMPLEMENTATION_CLINICAL_VALIDATION_ONLY");
        report.put("frozen_source", root.get("source"));
        report.put("golden_count", Integer.valueOf(golden.size()));
        report.put("safety_count", Integer.valueOf(safety.size()));
        report.put("total_count", Integer.valueOf(results.size()));
        report.put("passed_count", Integer.valueOf(passed));
        report.put("failed_count", Integer.valueOf(results.size() - passed));
        report.put("failed_ids", failedIds);
        report.put("results", results);

        Files.createDirectories(reportPath.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), report);

        assertEquals(results.size(), passed,
                "CD-08 mismatches: " + failedIds + "; report=" + reportPath);
    }

    private static Map<String, Object> runGolden(Map<String, Object> spec) {
        String caseId = string(spec.get("case_id"));
        Map<String, Object> fixture = map(spec.get("fixture"));
        Map<String, Object> expected = map(spec.get("expected"));
        List<String> errors = new ArrayList<String>();
        Map<String, Object> observed = new LinkedHashMap<String, Object>();

        try {
            String stateVersion = string(fixture.get("clinical_state_version"));
            if ("STALE".equals(stateVersion)) {
                observed.put("boundary", "EXECUTION_CONTEXT_VERSION_GUARD");
                try {
                    staleContext(caseId);
                    errors.add("stale Clinical State Version was accepted");
                } catch (RuntimeException expectedRejection) {
                    observed.put("runtime_exception_type", expectedRejection.getClass().getSimpleName());
                    observed.put("runtime_exception_message", expectedRejection.getMessage());
                    observed.put("reason_code", "RUNTIME_VERSION_GUARD_REJECTION");
                }
                compareDecisionExpectation(expected, observed, errors);
            } else if (!hasFrozenReleaseTuple(fixture)) {
                observed.put("boundary", "EXACT_RELEASE_GUARD");
                try {
                    context(caseId, 7, 7, releaseRefsFromFixture(fixture), "ci-nonprod-cd08");
                    errors.add("non-frozen release tuple was accepted");
                } catch (RuntimeException expectedRejection) {
                    observed.put("runtime_exception_type", expectedRejection.getClass().getSimpleName());
                    observed.put("runtime_exception_message", expectedRejection.getMessage());
                    observed.put("reason_code", "RUNTIME_RELEASE_GUARD_REJECTION");
                }
                compareDecisionExpectation(expected, observed, errors);
            } else {
                ClinicalExecution execution = executeClinical(caseId, fixture, Boolean.TRUE.equals(spec.get("replay")));
                observed.putAll(execution.observed);
                compareDecisionExpectation(expected, observed, errors);
                compareRuleExpectation(expected, execution.governed, errors);
                checkMustNot(listOfStrings(spec.get("must_not_output")), observed, errors);
            }
        } catch (RuntimeException failure) {
            errors.add("unexpected runtime failure: " + failure.getClass().getSimpleName() + ": " + failure.getMessage());
            observed.put("unexpected_exception", failure.toString());
        }

        return caseResult(caseId, "GOLDEN", expected, observed, errors);
    }

    private static Map<String, Object> runSafety(Map<String, Object> spec) {
        String caseId = string(spec.get("case_id"));
        String scenario = string(spec.get("scenario"));
        List<String> errors = new ArrayList<String>();
        Map<String, Object> observed = new LinkedHashMap<String, Object>();

        try {
            if ("UNKNOWN_REQUIRED_EVIDENCE".equals(scenario)) {
                U03GateCClinicalInput input = U03GateCClinicalInput.builder()
                        .evidence("EV-RF-RESP-001", "UNKNOWN").build();
                requireDecision(caseId, input, "FAILED", null, "INSUFFICIENT_INFORMATION", observed, errors);
            } else if ("UNMEASURED_REQUIRED_MEASUREMENT".equals(scenario)) {
                U03GateCClinicalInput input = sepsisBuilder()
                        .measurement("respiratory_rate_bpm", U03GateCClinicalInput.Measurement.state("UNMEASURED"))
                        .build();
                requireDecision(caseId, input, "FAILED", null, "INSUFFICIENT_INFORMATION", observed, errors);
            } else if ("REMOTE_NOT_OBSERVED".equals(scenario)) {
                U03GateCClinicalInput input = U03GateCClinicalInput.builder()
                        .evidence("EV-RF-RESP-001", "REMOTE_NOT_OBSERVED").build();
                requireDecision(caseId, input, "FAILED", null, "INSUFFICIENT_INFORMATION", observed, errors);
            } else if ("NOT_ASKED".equals(scenario)) {
                U03GateCClinicalInput input = U03GateCClinicalInput.builder()
                        .evidence("EV-RF-RESP-001", "NOT_ASKED").build();
                requireDecision(caseId, input, "FAILED", null, "INSUFFICIENT_INFORMATION", observed, errors);
            } else if ("PREGNANCY_TRUE".equals(scenario)) {
                requireDecision(caseId,
                        U03GateCClinicalInput.builder().pregnancyOrPuerperium("TRUE").build(),
                        "FAILED", null, "OVERALL_POLICY_SCOPE_MISMATCH", observed, errors);
            } else if ("PREGNANCY_UNKNOWN".equals(scenario)
                    || "PREGNANCY_NOT_ASKED".equals(scenario)
                    || "PREGNANCY_NOT_ESTABLISHED".equals(scenario)) {
                String state = "PREGNANCY_UNKNOWN".equals(scenario) ? "UNKNOWN"
                        : ("PREGNANCY_NOT_ASKED".equals(scenario) ? "NOT_ASKED" : "NOT_ESTABLISHED");
                requireDecision(caseId,
                        U03GateCClinicalInput.builder().pregnancyOrPuerperium(state).build(),
                        "FAILED", null, "OVERALL_POLICY_SCOPE_NOT_ESTABLISHED", observed, errors);
            } else if ("STALE".equals(scenario)) {
                observed.put("boundary", "EXECUTION_CONTEXT_VERSION_GUARD");
                try {
                    staleContext(caseId);
                    errors.add("stale Clinical State Version was accepted");
                } catch (RuntimeException expectedRejection) {
                    observed.put("rejected", Boolean.TRUE);
                    observed.put("runtime_exception_message", expectedRejection.getMessage());
                    // Safety requires fail-closed. Exact frozen reason parity is separately exposed by GC-024.
                }
            } else if ("RELEASE_MISMATCH".equals(scenario)) {
                observed.put("boundary", "EXACT_RELEASE_GUARD");
                try {
                    U03ExplicitNonProductionReleaseRefs wrong = new U03ExplicitNonProductionReleaseRefs(
                            U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                            "RR-U03-RISK-001@0.2.0-candidate",
                            U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF);
                    context(caseId, 7, 7, wrong, "ci-nonprod-cd08");
                    errors.add("release mismatch was accepted");
                } catch (RuntimeException expectedRejection) {
                    observed.put("rejected", Boolean.TRUE);
                    observed.put("runtime_exception_message", expectedRejection.getMessage());
                }
            } else if ("NO_HIGH_NOT_SAFE".equals(scenario)) {
                ClinicalExecution execution = executeClinical(caseId, baseFixtureInput(), false);
                observed.putAll(execution.observed);
                if (!"NO_HIGH_RISK_SIGNAL".equals(execution.decision.getOutcomeCode())) {
                    errors.add("expected NO_HIGH_RISK_SIGNAL");
                }
                Set<String> tokens = collectTokens(observed);
                for (String forbidden : Arrays.asList("SAFE", "NORMAL", "NO_DISEASE")) {
                    if (tokens.contains(forbidden)) errors.add("forbidden expansion: " + forbidden);
                }
            } else if ("IDEMPOTENT_REPLAY".equals(scenario)) {
                U03GateCClinicalInput input = U03GateCClinicalInput.builder()
                        .evidence("EV-RF-RESP-001", "PRESENT").build();
                ClinicalExecution execution = executeClinical(caseId, input, true);
                observed.putAll(execution.observed);
                if (!Boolean.TRUE.equals(observed.get("replay_same_result"))) {
                    errors.add("idempotent replay did not preserve result");
                }
                if (!Integer.valueOf(1).equals(observed.get("repository_commit_calls"))) {
                    errors.add("idempotent replay caused duplicate repository commit");
                }
            } else if ("NO_DIRECT_COMMIT".equals(scenario)) {
                observed.put("boundary", "STRUCTURAL_NO_DIRECT_COMMIT");
                assertNoTypeSurface(U03GateCFrozenRuleEvaluator.class,
                        "StateCommitter", "commit", errors);
                assertNoTypeSurface(U03GateCFrozenDecisionPort.class,
                        "StateCommitter", "commit", errors);
            } else if ("RULE_SIGNAL_NOT_DISPOSITION".equals(scenario)) {
                U03GateCClinicalInput input = sepsisBuilder()
                        .measurement("respiratory_rate_bpm", U03GateCClinicalInput.Measurement.present(25))
                        .build();
                ClinicalExecution execution = executeClinical(caseId, input, false);
                observed.putAll(execution.observed);
                if (!"HIGH_RISK".equals(execution.decision.getOutcomeCode())) {
                    errors.add("expected HIGH_RISK disposition");
                }
                for (U03GateCRuleResult result : execution.governed.getCandidate().getGateCEvaluation().getRuleResults()) {
                    if (result.getSignal() != null && result.getSignal().equals(execution.decision.getOutcomeCode())) {
                        errors.add("rule signal conflated with D09 disposition");
                    }
                }
            } else if ("D09_NOT_U04".equals(scenario)) {
                observed.put("boundary", "STRUCTURAL_NO_U04_OWNER");
                assertNoU04Surface(U03GateCFrozenDecisionPort.class, errors);
                assertNoU04Surface(U03OutboundProducer.class, errors);
                try {
                    U03OutboundHandoff.class.getMethod("getU04Decision");
                    errors.add("outbound exposes getU04Decision");
                } catch (NoSuchMethodException expected) {
                    observed.put("u04_decision_surface_absent", Boolean.TRUE);
                }
            } else if ("MUTABLE_LATEST".equals(scenario)) {
                observed.put("boundary", "EXACT_RELEASE_GUARD");
                try {
                    new U03ExplicitNonProductionReleaseRefs(
                            U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                            "latest",
                            U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF);
                    errors.add("mutable latest alias was accepted");
                } catch (RuntimeException expectedRejection) {
                    observed.put("rejected", Boolean.TRUE);
                }
            } else if ("UNAPPROVED_REF".equals(scenario)) {
                observed.put("boundary", "EXACT_RELEASE_GUARD");
                try {
                    U03ExplicitNonProductionReleaseRefs wrong = new U03ExplicitNonProductionReleaseRefs(
                            U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                            U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                            "PR-U03-D09-001@0.2.1-draft",
                            U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF);
                    context(caseId, 7, 7, wrong, "ci-nonprod-cd08");
                    errors.add("unapproved policy ref was accepted");
                } catch (RuntimeException expectedRejection) {
                    observed.put("rejected", Boolean.TRUE);
                }
            } else if ("NO_FREEFORM_KNOWLEDGE_EXECUTION".equals(scenario)) {
                observed.put("boundary", "TYPED_INPUT_ONLY");
                for (Method method : U03GateCClinicalInput.class.getDeclaredMethods()) {
                    if (method.getName().toLowerCase(Locale.ROOT).contains("freeform")) {
                        errors.add("typed Gate-C input exposes free-form clinical execution surface");
                    }
                }
                ClinicalExecution execution = executeClinical(caseId, baseFixtureInput(), false);
                observed.putAll(execution.observed);
                if (!"NO_HIGH_RISK_SIGNAL".equals(execution.decision.getOutcomeCode())) {
                    errors.add("typed baseline decision changed unexpectedly");
                }
            } else if ("NO_SCOPE_EXPANSION".equals(scenario)) {
                U03GateCClinicalInput input = U03GateCClinicalInput.builder()
                        .ageYears(12)
                        .pediatrics(true)
                        .evidence("EV-RF-RESP-001", "PRESENT")
                        .build();
                ClinicalExecution execution = executeDecisionOnly(caseId, input);
                observed.putAll(execution.observed);
                if (!"OVERALL_POLICY_SCOPE_MISMATCH".equals(execution.decision.getReasonCode())) {
                    errors.add("broader population scope was not rejected");
                }
                U03GateCRuleEvaluation evaluation = execution.governed.getCandidate().getGateCEvaluation();
                if (evaluation == null || !evaluation.getRuleResults().isEmpty()) {
                    errors.add("out-of-scope population entered C-rule evaluation");
                }
            } else {
                errors.add("unsupported safety scenario mapping: " + scenario);
            }
        } catch (RuntimeException failure) {
            errors.add("unexpected safety runtime failure: "
                    + failure.getClass().getSimpleName() + ": " + failure.getMessage());
            observed.put("unexpected_exception", failure.toString());
        }

        Map<String, Object> expected = map(spec.get("frozen_gatec_result"));
        return caseResult(caseId, "CRITICAL_SAFETY/" + scenario, expected, observed, errors);
    }

    private static ClinicalExecution executeClinical(String caseId, Map<String, Object> fixture, boolean replay) {
        return executeClinical(caseId, inputFromFixture(fixture), replay);
    }

    private static ClinicalExecution executeClinical(String caseId, U03GateCClinicalInput input, boolean replay) {
        ClinicalExecution execution = executeDecisionOnly(caseId, input);
        if (U03RiskAssessmentCandidate.VALID.equals(execution.decision.getStatus())) {
            U03StateProposal proposal = new U03StateProposalFactory()
                    .createNonProductionValid(execution.context, execution.decision, execution.governed);
            CommitFixture commitFixture = new CommitFixture("cdp-" + safe(caseId), 7);
            StateTypes.CommitResult commit = commitFixture.service.commitNonProduction(execution.context, proposal);
            execution.proposal = proposal;
            execution.commit = commit;
            execution.observed.put("proposal_id", proposal.getProposalId());
            execution.observed.put("commit_status", commit.status);
            execution.observed.put("commit_reason", commit.reasonCode);
            execution.observed.put("committed_version", commit.committedVersion);
            execution.observed.put("repository_commit_calls", Integer.valueOf(commitFixture.repository.commitCalls()));

            if ("COMMITTED".equals(commit.status)) {
                RecordingTracePort tracePort = new RecordingTracePort();
                U03PostCommitFinalizationOutcome finalization = new U03PostCommitFinalizer(
                        new U03RuntimeTraceService(tracePort),
                        new U03OutboundProducer())
                        .finalizeCommittedExecution(
                                execution.context,
                                execution.governed,
                                execution.decision,
                                proposal,
                                commit);
                execution.observed.put("finalization_status", finalization.getStatus());
                execution.observed.put("trace_status", finalization.getTraceOutcome().getStatus());
                if (finalization.getOutboundHandoff() != null) {
                    execution.observed.put("outbound_execution_status",
                            finalization.getOutboundHandoff().getExecutionStatus());
                    execution.observed.put("outbound_disposition",
                            finalization.getOutboundHandoff().getDispositionCode());
                    execution.observed.put("outbound_commit_status",
                            finalization.getOutboundHandoff().getCommitStatus());
                }
            }

            if (replay) {
                StateTypes.CommitResult second = commitFixture.service.commitNonProduction(execution.context, proposal);
                execution.observed.put("replay_status", second.status);
                execution.observed.put("replay_reason", second.reasonCode);
                execution.observed.put("replay_same_result",
                        Boolean.valueOf(sameCommitIdentity(commit, second)));
                execution.observed.put("repository_commit_calls",
                        Integer.valueOf(commitFixture.repository.commitCalls()));
            }
        } else {
            // Frozen D09 FAILED is itself the terminal expected governance boundary.
            // Do not invent a downstream S14 handoff for a case whose governed
            // decision explicitly failed closed.
            execution.observed.put("boundary", "D09_FAILED_NO_COMMIT");
            execution.observed.put("commit_status", null);
            execution.observed.put("outbound_execution_status", null);
            execution.observed.put("outbound_disposition", null);
        }
        return execution;
    }

    private static ClinicalExecution executeDecisionOnly(String caseId, U03GateCClinicalInput input) {
        U03NonProductionExecutionContext context = context(
                caseId, 7, 7, U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(), "ci-nonprod-cd08");
        CapabilityBindingRecord capability = capabilityBinding();
        CapabilityInvocationGuard guard = fixedGuard(capability);

        U03GateCFrozenRuleEvaluator c02 = new U03GateCFrozenRuleEvaluator(
                new U03GateCClinicalInputPort() {
                    @Override
                    public U03GateCClinicalInput load(
                            U03ExecutionCommand command,
                            U03AcceptedEvidenceBinding acceptedEvidenceBinding) {
                        return input;
                    }
                });

        U03GovernedCandidateGateway.GovernedResult governed = new U03GovernedCandidateGateway(
                guard, new U03ReleaseRegistry(), c02, true).assess(context);
        U03DecisionOutcome decision = new U03DecisionService(new U03GateCFrozenDecisionPort())
                .decide(context, governed);

        Map<String, Object> observed = new LinkedHashMap<String, Object>();
        observed.put("boundary", "D09");
        observed.put("status", decision.getStatus());
        observed.put("disposition", decision.getOutcomeCode() == null ? "NONE" : decision.getOutcomeCode());
        observed.put("reason_code", decision.getReasonCode());
        observed.put("clinical_state_version", Integer.valueOf(context.getCommand().clinicalStateVersion));
        observed.put("release_refs", Arrays.asList(
                context.getReleaseRefs().getKnowledgeReleaseRef(),
                context.getReleaseRefs().getRuleReleaseRef(),
                context.getReleaseRefs().getCoverageContractRef(),
                context.getReleaseRefs().getPolicyReleaseRef(),
                context.getReleaseRefs().getPolicyPairRef()));
        observed.put("matched_rule_refs", matchedRules(governed));
        observed.put("insufficient_rule_refs", insufficientRules(governed));
        observed.put("rule_results", ruleEvidence(governed));

        return new ClinicalExecution(context, governed, decision, observed);
    }

    private static void requireDecision(
            String caseId,
            U03GateCClinicalInput input,
            String expectedStatus,
            String expectedDisposition,
            String expectedReason,
            Map<String, Object> observed,
            List<String> errors) {
        ClinicalExecution execution = executeClinical(caseId, input, false);
        observed.putAll(execution.observed);
        if (!expectedStatus.equals(execution.decision.getStatus())) {
            errors.add("status expected=" + expectedStatus + " observed=" + execution.decision.getStatus());
        }
        String actualDisposition = execution.decision.getOutcomeCode();
        if (expectedDisposition == null ? actualDisposition != null : !expectedDisposition.equals(actualDisposition)) {
            errors.add("disposition expected=" + expectedDisposition + " observed=" + actualDisposition);
        }
        if (!expectedReason.equals(execution.decision.getReasonCode())) {
            errors.add("reason expected=" + expectedReason + " observed=" + execution.decision.getReasonCode());
        }
    }

    private static U03GateCClinicalInput inputFromFixture(Map<String, Object> fixture) {
        U03GateCClinicalInput.Builder b = U03GateCClinicalInput.builder();

        Object age = fixture.get("age");
        if (age instanceof Number) {
            b.ageYears(((Number) age).intValue());
        } else if (age != null) {
            b.ageState(string(age));
        }
        if (fixture.get("pregnancy_or_puerperium") != null) {
            b.pregnancyOrPuerperium(string(fixture.get("pregnancy_or_puerperium")));
        }
        if (fixture.get("pediatrics") != null) b.pediatrics(bool(fixture.get("pediatrics")));
        if (fixture.get("region_scope_ok") != null) b.regionScopeOk(bool(fixture.get("region_scope_ok")));
        if (fixture.get("channel_scope_ok") != null) b.channelScopeOk(bool(fixture.get("channel_scope_ok")));
        if (fixture.get("setting") != null) b.setting(string(fixture.get("setting")));
        if (fixture.get("suspected_sepsis") != null) b.suspectedSepsis(string(fixture.get("suspected_sepsis")));
        if (fixture.get("dyspnoea_context") != null) b.dyspnoeaContext(string(fixture.get("dyspnoea_context")));
        if (Boolean.TRUE.equals(fixture.get("invalid_input"))) b.invalidInput(true);
        if (Boolean.TRUE.equals(fixture.get("dependency_failure"))) b.dependencyFailure(true);

        Map<String, Object> evidence = mapOrEmpty(fixture.get("evidence"));
        for (Map.Entry<String, Object> entry : evidence.entrySet()) {
            b.evidence(entry.getKey(), stateValue(entry.getValue()));
        }

        Map<String, Object> measurements = mapOrEmpty(fixture.get("measurements"));
        for (Map.Entry<String, Object> entry : measurements.entrySet()) {
            Object raw = entry.getValue();
            if (raw instanceof Number) {
                b.measurement(entry.getKey(),
                        U03GateCClinicalInput.Measurement.present(((Number) raw).doubleValue()));
            } else if (raw instanceof Map<?, ?>) {
                Map<String, Object> m = map(raw);
                String state = m.get("state") == null ? "PRESENT" : string(m.get("state"));
                if ("PRESENT".equals(state) && m.get("value") instanceof Number) {
                    b.measurement(entry.getKey(),
                            U03GateCClinicalInput.Measurement.present(
                                    ((Number) m.get("value")).doubleValue()));
                } else {
                    b.measurement(entry.getKey(), U03GateCClinicalInput.Measurement.state(state));
                }
            } else if (raw != null) {
                b.measurement(entry.getKey(), U03GateCClinicalInput.Measurement.state("INVALID"));
            }
        }

        Map<String, Object> scopes = mapOrEmpty(fixture.get("rule_scope"));
        for (Map.Entry<String, Object> entry : scopes.entrySet()) {
            b.ruleScope(entry.getKey(), string(entry.getValue()));
        }

        return b.build();
    }

    private static U03GateCClinicalInput baseFixtureInput() {
        return U03GateCClinicalInput.builder()
                .measurement("respiratory_rate_bpm", U03GateCClinicalInput.Measurement.present(18))
                .measurement("systolic_bp_mmHg", U03GateCClinicalInput.Measurement.present(120))
                .measurement("usual_systolic_bp_mmHg", U03GateCClinicalInput.Measurement.present(120))
                .measurement("heart_rate_bpm", U03GateCClinicalInput.Measurement.present(80))
                .build();
    }

    private static U03GateCClinicalInput.Builder sepsisBuilder() {
        return U03GateCClinicalInput.builder()
                .suspectedSepsis("TRUE")
                .evidence("EV-RF-APPEAR-001", "ABSENT")
                .evidence("EV-RF-SEPSIS-001", "ABSENT")
                .measurement("respiratory_rate_bpm", U03GateCClinicalInput.Measurement.present(18))
                .measurement("systolic_bp_mmHg", U03GateCClinicalInput.Measurement.present(120))
                .measurement("usual_systolic_bp_mmHg", U03GateCClinicalInput.Measurement.present(120))
                .measurement("heart_rate_bpm", U03GateCClinicalInput.Measurement.present(80));
    }

    private static U03NonProductionExecutionContext staleContext(String caseId) {
        return context(caseId, 7, 6,
                U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(), "ci-nonprod-cd08");
    }

    private static U03NonProductionExecutionContext context(
            String caseId,
            int commandVersion,
            int evidenceVersion,
            U03ExplicitNonProductionReleaseRefs refs,
            String environment) {
        String token = safe(caseId);
        U03ExecutionCommand command = new U03ExecutionCommand(
                "consult-" + token,
                "thread-" + token,
                "run-" + token,
                "event-" + token,
                "cdp-" + token,
                commandVersion,
                "corr-" + token,
                "trace-" + token);
        U03AcceptedEvidenceBinding evidence = new U03AcceptedEvidenceBinding(
                "acceptance-" + token,
                evidenceVersion,
                ACCEPTED_EVIDENCE_REFS,
                Collections.singletonList("frozen-source-" + token),
                Collections.singletonList("frozen-provenance-" + token));
        return new U03NonProductionExecutionContext(command, refs, evidence, environment);
    }

    private static U03ExplicitNonProductionReleaseRefs releaseRefsFromFixture(Map<String, Object> fixture) {
        Map<String, Object> refs = map(fixture.get("release_refs"));
        return new U03ExplicitNonProductionReleaseRefs(
                string(refs.get("knowledge_release_ref")),
                string(refs.get("rule_release_ref")),
                string(refs.get("coverage_contract_ref")),
                string(refs.get("policy_release_ref")),
                string(refs.get("policy_pair_ref")));
    }

    private static boolean hasFrozenReleaseTuple(Map<String, Object> fixture) {
        try {
            releaseRefsFromFixture(fixture).requireGateCFrozenSet();
            return true;
        } catch (RuntimeException mismatch) {
            return false;
        }
    }

    private static CapabilityInvocationGuard fixedGuard(final CapabilityBindingRecord binding) {
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

    private static CapabilityBindingRecord capabilityBinding() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 18, 0, 0);
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
                now.minusMinutes(1),
                null,
                now.minusMinutes(1));
    }

    private static void compareDecisionExpectation(
            Map<String, Object> expected,
            Map<String, Object> observed,
            List<String> errors) {
        compareField("result_status", string(expected.get("result_status")),
                stringOrNull(observed.get("status")), errors);
        compareField("disposition", string(expected.get("disposition")),
                stringOrNull(observed.get("disposition")), errors);
        compareField("reason_code", string(expected.get("reason_code")),
                stringOrNull(observed.get("reason_code")), errors);
    }

    private static void compareRuleExpectation(
            Map<String, Object> expected,
            U03GovernedCandidateGateway.GovernedResult governed,
            List<String> errors) {
        List<String> expectedMatched = sorted(listOfStrings(expected.get("matched_rule_refs")));
        List<String> expectedInsufficient = sorted(listOfStrings(expected.get("insufficient_rule_refs")));
        List<String> actualMatched = sorted(matchedRules(governed));
        List<String> actualInsufficient = sorted(insufficientRules(governed));
        if (!expectedMatched.equals(actualMatched)) {
            errors.add("matched_rule_refs expected=" + expectedMatched + " observed=" + actualMatched);
        }
        if (!expectedInsufficient.equals(actualInsufficient)) {
            errors.add("insufficient_rule_refs expected=" + expectedInsufficient
                    + " observed=" + actualInsufficient);
        }
    }

    private static List<String> matchedRules(U03GovernedCandidateGateway.GovernedResult governed) {
        List<String> out = new ArrayList<String>();
        U03GateCRuleEvaluation evaluation = governed.getCandidate().getGateCEvaluation();
        if (evaluation == null) return out;
        for (U03GateCRuleResult result : evaluation.getRuleResults()) {
            if ("MATCHED".equals(result.getExecutionState())) out.add(result.getRuleId());
        }
        return out;
    }

    private static List<String> insufficientRules(U03GovernedCandidateGateway.GovernedResult governed) {
        List<String> out = new ArrayList<String>();
        U03GateCRuleEvaluation evaluation = governed.getCandidate().getGateCEvaluation();
        if (evaluation == null) return out;
        for (U03GateCRuleResult result : evaluation.getRuleResults()) {
            if ("INPUT_INSUFFICIENT".equals(result.getExecutionState())) out.add(result.getRuleId());
        }
        return out;
    }

    private static List<Map<String, Object>> ruleEvidence(
            U03GovernedCandidateGateway.GovernedResult governed) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        U03GateCRuleEvaluation evaluation = governed.getCandidate().getGateCEvaluation();
        if (evaluation == null) return out;
        for (U03GateCRuleResult result : evaluation.getRuleResults()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("rule_id", result.getRuleId());
            row.put("execution_state", result.getExecutionState());
            row.put("signal", result.getSignal());
            row.put("evidence_refs", result.getEvidenceRefs());
            out.add(row);
        }
        return out;
    }

    private static void checkMustNot(
            List<String> forbidden,
            Map<String, Object> observed,
            List<String> errors) {
        // Match Gate-C outcome_tokens(): decision fields plus rule IDs/signals only.
        // Rule execution states such as NO_MATCH are internal C evidence and are
        // deliberately not treated as user/business output tokens.
        Set<String> tokens = new HashSet<String>();
        addToken(tokens, observed.get("status"));
        addToken(tokens, observed.get("disposition"));
        addToken(tokens, observed.get("reason_code"));
        Object rawRules = observed.get("rule_results");
        if (rawRules instanceof Iterable<?>) {
            for (Object raw : (Iterable<?>) rawRules) {
                if (raw instanceof Map<?, ?>) {
                    Map<String, Object> rr = map(raw);
                    addToken(tokens, rr.get("rule_id"));
                    addToken(tokens, rr.get("signal"));
                }
            }
            tokens.add("C_RULE_EVALUATION");
        }
        for (String token : forbidden) {
            if (tokens.contains(token)) errors.add("must_not_output violated: " + token);
        }
    }

    private static void addToken(Set<String> tokens, Object value) {
        if (value instanceof String) tokens.add((String) value);
    }

    private static Set<String> collectTokens(Object value) {
        Set<String> out = new HashSet<String>();
        collectTokens(value, out);
        return out;
    }

    private static void collectTokens(Object value, Set<String> out) {
        if (value == null) return;
        if (value instanceof String) {
            out.add((String) value);
        } else if (value instanceof Map<?, ?>) {
            for (Object v : ((Map<?, ?>) value).values()) collectTokens(v, out);
        } else if (value instanceof Iterable<?>) {
            for (Object v : (Iterable<?>) value) collectTokens(v, out);
        }
    }

    private static Map<String, Object> caseResult(
            String caseId,
            String kind,
            Map<String, Object> expected,
            Map<String, Object> observed,
            List<String> errors) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("case_id", caseId);
        result.put("kind", kind);
        result.put("expected", expected);
        result.put("observed", observed);
        result.put("errors", errors);
        result.put("pass", Boolean.valueOf(errors.isEmpty()));
        return result;
    }

    private static void compareField(String field, String expected, String observed, List<String> errors) {
        if (expected == null ? observed != null : !expected.equals(observed)) {
            errors.add(field + " expected=" + expected + " observed=" + observed);
        }
    }

    private static void assertNoTypeSurface(
            Class<?> type,
            String forbiddenTypeToken,
            String forbiddenMethodToken,
            List<String> errors) {
        for (Field field : type.getDeclaredFields()) {
            if (field.getType().getName().contains(forbiddenTypeToken)) {
                errors.add(type.getSimpleName() + " field exposes " + forbiddenTypeToken);
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().toLowerCase(Locale.ROOT)
                    .contains(forbiddenMethodToken.toLowerCase(Locale.ROOT))) {
                errors.add(type.getSimpleName() + " method exposes " + forbiddenMethodToken);
            }
            if (method.getReturnType().getName().contains(forbiddenTypeToken)) {
                errors.add(type.getSimpleName() + " return type exposes " + forbiddenTypeToken);
            }
            for (Class<?> p : method.getParameterTypes()) {
                if (p.getName().contains(forbiddenTypeToken)) {
                    errors.add(type.getSimpleName() + " parameter exposes " + forbiddenTypeToken);
                }
            }
        }
    }

    private static void assertNoU04Surface(Class<?> type, List<String> errors) {
        for (Field field : type.getDeclaredFields()) {
            if (field.getType().getName().contains(".u04.")) {
                errors.add(type.getSimpleName() + " field exposes U04");
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            if (name.contains("u04") || method.getReturnType().getName().contains(".u04.")) {
                errors.add(type.getSimpleName() + " exposes U04 method surface: " + method.getName());
            }
            for (Class<?> p : method.getParameterTypes()) {
                if (p.getName().contains(".u04.")) {
                    errors.add(type.getSimpleName() + " parameter exposes U04");
                }
            }
        }
    }

    private static boolean sameCommitIdentity(StateTypes.CommitResult a, StateTypes.CommitResult b) {
        if (a == null || b == null) return false;
        return eq(a.status, b.status)
                && eq(a.patchId, b.patchId)
                && eq(a.previousVersion, b.previousVersion)
                && eq(a.committedVersion, b.committedVersion);
    }

    private static boolean eq(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String stateValue(Object raw) {
        if (raw instanceof Map<?, ?>) {
            Map<String, Object> m = map(raw);
            return m.get("state") == null ? "ABSENT" : string(m.get("state"));
        }
        return raw == null ? "ABSENT" : string(raw);
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("required system property missing: " + name);
        }
        return value;
    }

    private static String safe(String value) {
        return value.replaceAll("[^A-Za-z0-9._:-]", "_");
    }

    private static String string(Object value) {
        if (value == null) throw new IllegalArgumentException("required string is null");
        return String.valueOf(value);
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("expected map but got " + value);
        }
        return (Map<String, Object>) value;
    }

    private static Map<String, Object> mapOrEmpty(Object value) {
        return value == null ? Collections.<String, Object>emptyMap() : map(value);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof List<?>)) {
            throw new IllegalArgumentException("expected list");
        }
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<String> listOfStrings(Object value) {
        if (value == null) return new ArrayList<String>();
        List<Object> raw = (List<Object>) value;
        List<String> out = new ArrayList<String>();
        for (Object item : raw) out.add(String.valueOf(item));
        return out;
    }

    private static List<String> sorted(List<String> values) {
        List<String> copy = new ArrayList<String>(values);
        Collections.sort(copy);
        return copy;
    }

    private static final class ClinicalExecution {
        final U03NonProductionExecutionContext context;
        final U03GovernedCandidateGateway.GovernedResult governed;
        final U03DecisionOutcome decision;
        final Map<String, Object> observed;
        U03StateProposal proposal;
        StateTypes.CommitResult commit;

        ClinicalExecution(
                U03NonProductionExecutionContext context,
                U03GovernedCandidateGateway.GovernedResult governed,
                U03DecisionOutcome decision,
                Map<String, Object> observed) {
            this.context = context;
            this.governed = governed;
            this.decision = decision;
            this.observed = observed;
        }
    }

    private static final class CommitFixture {
        final MechanicalVersionRepositoryFake repository;
        final U03CommitService service;

        CommitFixture(String cdpId, int currentVersion) {
            List<String> order = new ArrayList<String>();
            repository = new MechanicalVersionRepositoryFake(order);
            repository.seed(cdpId, currentVersion);
            StateCommitter committer = new StateCommitter(
                    repository,
                    new CapabilityPolicyPort() {
                        @Override
                        public CapabilityDecision evaluate(String capabilityId, String capabilityVersion) {
                            return CapabilityDecision.authorized();
                        }
                    },
                    new FieldPermissionPort() {
                        @Override
                        public FieldPermissionDecision evaluate(String path, String capabilityId) {
                            return FieldPermissionDecision.authorized();
                        }
                    },
                    new ConsentPolicyPort() {
                        @Override
                        public ConsentDecision evaluate(String cdpIdValue, String capabilityId) {
                            return ConsentDecision.authorized();
                        }
                    },
                    new SourceValidationPort() {
                        @Override
                        public SourceDecision evaluate(String source) {
                            return SourceDecision.authorized();
                        }
                    },
                    new InMemoryIdempotencyFake(order),
                    new SyntheticAuditPortFake(CLOCK, order),
                    new RecordingCommitEventEvidenceFake(order),
                    CLOCK);
            service = new U03CommitService(committer);
        }
    }

    private static final class RecordingTracePort implements U03RuntimeTracePort {
        final List<U03RuntimeTraceRecord> records = new ArrayList<U03RuntimeTraceRecord>();

        @Override
        public void record(U03RuntimeTraceRecord record) {
            records.add(record);
        }
    }
}
