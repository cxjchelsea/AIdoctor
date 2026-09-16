package com.aidoctor.diagnosis.runtime.u03.eval;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gate-C governed offline evaluation execution for ER-U03-RISK-001@0.1.0-candidate. */
class U03GateCEvaluationExecutionTest {
    private final U03EvaluationOnlyClinicalEvaluator evaluator = new U03EvaluationOnlyClinicalEvaluator();

    static final class GoldenCase {
        final String id;
        final U03EvaluationOnlyClinicalEvaluator.Fixture fixture;
        final String status;
        final String disposition;
        final String reason;
        GoldenCase(String id, U03EvaluationOnlyClinicalEvaluator.Fixture fixture,
                   String status, String disposition, String reason) {
            this.id = id; this.fixture = fixture; this.status = status;
            this.disposition = disposition; this.reason = reason;
        }
    }

    static final class Check {
        final String id;
        final boolean passed;
        final String detail;
        Check(String id, boolean passed, String detail) { this.id = id; this.passed = passed; this.detail = detail; }
    }

    @Test
    void executesAllApprovedGoldenCasesAndCriticalSafetySuite() throws IOException {
        List<String> bundle = new ArrayList<String>();
        bundle.add("evalset_release=" + U03EvaluationOnlyClinicalEvaluator.EVALSET_RELEASE);
        bundle.add("rule_release=" + U03EvaluationOnlyClinicalEvaluator.RULE_RELEASE);
        bundle.add("knowledge_release=" + U03EvaluationOnlyClinicalEvaluator.KNOWLEDGE_RELEASE);
        bundle.add("coverage_release=" + U03EvaluationOnlyClinicalEvaluator.COVERAGE_RELEASE);
        bundle.add("policy_release=" + U03EvaluationOnlyClinicalEvaluator.POLICY_RELEASE);
        bundle.add("policy_pair=" + U03EvaluationOnlyClinicalEvaluator.POLICY_PAIR);

        List<GoldenCase> cases = goldenCases();
        assertEquals(31, cases.size());
        int goldenPass = 0;
        for (GoldenCase c : cases) {
            U03EvaluationOnlyClinicalEvaluator.Outcome actual = evaluator.evaluate(c.fixture);
            boolean pass = c.status.equals(actual.status)
                    && eq(c.disposition, actual.disposition)
                    && c.reason.equals(actual.reasonCode);
            bundle.add(c.id + "=" + (pass ? "PASS" : "FAIL")
                    + " expected=" + triple(c.status, c.disposition, c.reason)
                    + " actual=" + triple(actual.status, actual.disposition, actual.reasonCode)
                    + " policy=" + actual.policyId
                    + " matched=" + actual.matchedRuleRefs
                    + " insufficient=" + actual.insufficientRuleRefs);
            assertTrue(pass, c.id + " expected " + triple(c.status, c.disposition, c.reason)
                    + " but got " + triple(actual.status, actual.disposition, actual.reasonCode));
            goldenPass++;
        }

        List<Check> safety = safetyChecks();
        assertEquals(20, safety.size());
        int safetyPass = 0;
        for (Check check : safety) {
            bundle.add(check.id + "=" + (check.passed ? "PASS" : "FAIL") + " " + check.detail);
            assertTrue(check.passed, check.id + " critical safety assertion failed: " + check.detail);
            safetyPass++;
        }

        bundle.add("golden_cases_passed=" + goldenPass + "/31");
        bundle.add("critical_safety_cases_passed=" + safetyPass + "/20");
        bundle.add("critical_safety_failure_count=0");
        bundle.add("gate_c_execution_test=PASS");
        writeBundle(bundle);
    }

    private List<GoldenCase> goldenCases() {
        List<GoldenCase> c = new ArrayList<GoldenCase>();
        c.add(gc("GC-001", base().evidence("EV-RF-RESP-001", present()), "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-002", base().evidence("EV-MNM-NEURO-001", present()), "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-003", base().evidence("EV-MNM-NEURO-002", present()), "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-004", base().evidence("EV-MNM-CARD-001", present()), "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-005", base().evidence("EV-RF-ALLERGY-001", present()), "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-006", base().dyspnoea(t()).evidence("EV-RF-APPEAR-001", present()).evidence("EV-RF-NEURO-001", absent()), "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-007", base().dyspnoea(t()).evidence("EV-RF-APPEAR-001", absent()).evidence("EV-RF-NEURO-001", present()), "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-008", sepsisNoSignal().rr(25), "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-009", sepsisNoSignal().rr(22), "VALID", "CAUTION", "MODERATE_HIGH_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-010", sepsisNoSignal().sbp(90).usualSbp(null, false), "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-011", sepsisNoSignal().sbp(95).usualSbp(120, true), "VALID", "CAUTION", "MODERATE_HIGH_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-012", base(), "VALID", "NO_HIGH_RISK_SIGNAL", "COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL"));
        c.add(gc("GC-013", sepsisNoSignal().hr(100), "VALID", "CAUTION", "MODERATE_HIGH_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-014", base().sepsis(f()), "VALID", "NO_HIGH_RISK_SIGNAL", "COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL"));
        c.add(gc("GC-015", base().dyspnoea(f()), "VALID", "NO_HIGH_RISK_SIGNAL", "COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL"));
        c.add(gc("GC-016", base().evidence("EV-RF-RESP-001", present()).evidence("EV-MNM-NEURO-001", unknown()), "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-017", base().evidence("EV-MNM-NEURO-001", unknown()), "FAILED", null, "INSUFFICIENT_INFORMATION"));
        c.add(gc("GC-018", sepsisNoSignal().rrState(U03EvaluationOnlyClinicalEvaluator.FactState.UNMEASURED), "FAILED", null, "INSUFFICIENT_INFORMATION"));
        c.add(gc("GC-019", base().evidence("EV-RF-RESP-001", U03EvaluationOnlyClinicalEvaluator.FactState.REMOTE_NOT_OBSERVED), "FAILED", null, "INSUFFICIENT_INFORMATION"));
        c.add(gc("GC-020", base().pregnancy(t()), "FAILED", null, "OVERALL_POLICY_SCOPE_MISMATCH"));
        c.add(gc("GC-021", base().pregnancy(U03EvaluationOnlyClinicalEvaluator.TriState.UNKNOWN), "FAILED", null, "OVERALL_POLICY_SCOPE_NOT_ESTABLISHED"));
        c.add(gc("GC-022", base().pregnancy(U03EvaluationOnlyClinicalEvaluator.TriState.NOT_ASKED), "FAILED", null, "OVERALL_POLICY_SCOPE_NOT_ESTABLISHED"));
        c.add(gc("GC-023", base().pregnancy(U03EvaluationOnlyClinicalEvaluator.TriState.NOT_ESTABLISHED), "FAILED", null, "OVERALL_POLICY_SCOPE_NOT_ESTABLISHED"));
        U03EvaluationOnlyClinicalEvaluator.Fixture stale = base().evidence("EV-RF-RESP-001", present()); stale.staleInput = true;
        c.add(gc("GC-024", stale, "FAILED", null, "STALE_INPUT"));
        U03EvaluationOnlyClinicalEvaluator.Fixture mismatch = base(); mismatch.releaseMismatch = true;
        c.add(gc("GC-025", mismatch, "FAILED", null, "RELEASE_MISMATCH"));
        U03EvaluationOnlyClinicalEvaluator.Fixture conflict = base(); conflict.unresolvableConflict = true;
        c.add(gc("GC-026", conflict, "FAILED", null, "UNRESOLVABLE_CONFLICT"));
        c.add(gc("GC-027", sepsisNoSignal().rr(22).evidence("EV-MNM-NEURO-001", unknown()), "FAILED", null, "INSUFFICIENT_INFORMATION"));
        c.add(gc("GC-028", base(), "VALID", "NO_HIGH_RISK_SIGNAL", "COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL"));
        c.add(gc("GC-029", sepsisNoSignal().evidence("EV-RF-APPEAR-001", present()), "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-030", sepsisNoSignal().evidence("EV-RF-SEPSIS-001", present()), "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT"));
        c.add(gc("GC-031", sepsisNoSignal().hr(131), "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT"));
        return c;
    }

    private List<Check> safetyChecks() {
        List<Check> checks = new ArrayList<Check>();
        U03EvaluationOnlyClinicalEvaluator.Outcome ss1 = evaluator.evaluate(base().evidence("EV-RF-RESP-001", unknown()));
        checks.add(check("SS-001", "FAILED".equals(ss1.status) && "INSUFFICIENT_INFORMATION".equals(ss1.reasonCode), "UNKNOWN != NEGATIVE"));
        U03EvaluationOnlyClinicalEvaluator.Outcome ss2 = evaluator.evaluate(sepsisNoSignal().rrState(U03EvaluationOnlyClinicalEvaluator.FactState.UNMEASURED));
        checks.add(check("SS-002", "FAILED".equals(ss2.status), "UNMEASURED != NORMAL"));
        U03EvaluationOnlyClinicalEvaluator.Outcome ss3 = evaluator.evaluate(base().evidence("EV-RF-RESP-001", U03EvaluationOnlyClinicalEvaluator.FactState.REMOTE_NOT_OBSERVED));
        checks.add(check("SS-003", "FAILED".equals(ss3.status), "REMOTE_NOT_OBSERVED != EXCLUDED"));
        U03EvaluationOnlyClinicalEvaluator.Outcome ss4 = evaluator.evaluate(base().evidence("EV-MNM-NEURO-001", U03EvaluationOnlyClinicalEvaluator.FactState.NOT_ASKED));
        checks.add(check("SS-004", "FAILED".equals(ss4.status), "NOT_ASKED != NO"));
        U03EvaluationOnlyClinicalEvaluator.Outcome ss5 = evaluator.evaluate(base().pregnancy(t()));
        checks.add(check("SS-005", "OVERALL_POLICY_SCOPE_MISMATCH".equals(ss5.reasonCode), "whole-slice scope TRUE fails at P0"));
        U03EvaluationOnlyClinicalEvaluator.Outcome ss6 = evaluator.evaluate(base().pregnancy(U03EvaluationOnlyClinicalEvaluator.TriState.UNKNOWN));
        checks.add(check("SS-006", "OVERALL_POLICY_SCOPE_NOT_ESTABLISHED".equals(ss6.reasonCode), "scope UNKNOWN != FALSE"));
        U03EvaluationOnlyClinicalEvaluator.Outcome ss7 = evaluator.evaluate(base().pregnancy(U03EvaluationOnlyClinicalEvaluator.TriState.NOT_ASKED));
        checks.add(check("SS-007", "OVERALL_POLICY_SCOPE_NOT_ESTABLISHED".equals(ss7.reasonCode), "scope NOT_ASKED != FALSE"));
        U03EvaluationOnlyClinicalEvaluator.Outcome ss8 = evaluator.evaluate(base().pregnancy(U03EvaluationOnlyClinicalEvaluator.TriState.NOT_ESTABLISHED));
        checks.add(check("SS-008", "OVERALL_POLICY_SCOPE_NOT_ESTABLISHED".equals(ss8.reasonCode), "scope NOT_ESTABLISHED != FALSE"));
        U03EvaluationOnlyClinicalEvaluator.Fixture stale = base().evidence("EV-RF-RESP-001", present()); stale.staleInput = true;
        U03EvaluationOnlyClinicalEvaluator.Outcome ss9 = evaluator.evaluate(stale);
        checks.add(check("SS-009", "FAILED".equals(ss9.status) && ss9.disposition == null, "stale != current HIGH"));
        U03EvaluationOnlyClinicalEvaluator.Fixture release = base(); release.releaseMismatch = true;
        checks.add(check("SS-010", "RELEASE_MISMATCH".equals(evaluator.evaluate(release).reasonCode), "release mismatch != accepted"));
        U03EvaluationOnlyClinicalEvaluator.Outcome ss11 = evaluator.evaluate(base());
        checks.add(check("SS-011", "NO_HIGH_RISK_SIGNAL".equals(ss11.disposition) && !"SAFE".equals(ss11.disposition) && !"NORMAL".equals(ss11.disposition), "NO_HIGH_RISK_SIGNAL != SAFE/NORMAL"));
        U03EvaluationOnlyClinicalEvaluator.Fixture conflict = base(); conflict.unresolvableConflict = true;
        checks.add(check("SS-012", "UNRESOLVABLE_CONFLICT".equals(evaluator.evaluate(conflict).reasonCode), "conflict != arbitrary branch"));
        U03EvaluationOnlyClinicalEvaluator.Outcome replay1 = evaluator.evaluate(base());
        U03EvaluationOnlyClinicalEvaluator.Outcome replay2 = evaluator.evaluate(base());
        checks.add(check("SS-013", triple(replay1.status, replay1.disposition, replay1.reasonCode).equals(triple(replay2.status, replay2.disposition, replay2.reasonCode)), "replay deterministic / no duplicate mutation path"));
        checks.add(check("SS-014", noProductionMutationApi(), "evaluation evaluator exposes no production commit API"));
        U03EvaluationOnlyClinicalEvaluator.Outcome signalOutcome = evaluator.evaluate(base().evidence("EV-RF-RESP-001", present()));
        String ruleSignal = signalOutcome.rules.get("C-RULE-RESP-001").signal;
        checks.add(check("SS-015", U03EvaluationOnlyClinicalEvaluator.CRITICAL.equals(ruleSignal) && "HIGH_RISK".equals(signalOutcome.disposition) && !ruleSignal.equals(signalOutcome.disposition), "Rule Signal != D09 disposition"));
        checks.add(check("SS-016", signalOutcome.policyId.startsWith("D09-") && !signalOutcome.policyId.contains("U04"), "D09 decision != U04 decision"));
        checks.add(check("SS-017", exactRefsContainNoLatest(), "mutable latest alias prohibited"));
        U03EvaluationOnlyClinicalEvaluator.Fixture unapproved = base(); unapproved.releaseMismatch = true;
        checks.add(check("SS-018", "FAILED".equals(evaluator.evaluate(unapproved).status), "unapproved/wrong release prohibited"));
        checks.add(check("SS-019", U03EvaluationOnlyClinicalEvaluator.KNOWLEDGE_RELEASE.startsWith("KR-") && !U03EvaluationOnlyClinicalEvaluator.RULE_RELEASE.equals(U03EvaluationOnlyClinicalEvaluator.KNOWLEDGE_RELEASE), "knowledge authority != rule owner"));
        U03EvaluationOnlyClinicalEvaluator.Fixture pediatric = base(); pediatric.adultPopulation = false; pediatric.age = 15;
        checks.add(check("SS-020", "OVERALL_POLICY_SCOPE_MISMATCH".equals(evaluator.evaluate(pediatric).reasonCode), "scope expansion prohibited"));
        return checks;
    }

    private boolean noProductionMutationApi() {
        return Arrays.stream(U03EvaluationOnlyClinicalEvaluator.class.getDeclaredMethods())
                .noneMatch(m -> m.getName().toLowerCase().contains("commit") || m.getName().toLowerCase().contains("publish") || m.getName().toLowerCase().contains("activate"));
    }

    private boolean exactRefsContainNoLatest() {
        List<String> refs = Arrays.asList(
                U03EvaluationOnlyClinicalEvaluator.RULE_RELEASE,
                U03EvaluationOnlyClinicalEvaluator.KNOWLEDGE_RELEASE,
                U03EvaluationOnlyClinicalEvaluator.COVERAGE_RELEASE,
                U03EvaluationOnlyClinicalEvaluator.POLICY_RELEASE,
                U03EvaluationOnlyClinicalEvaluator.POLICY_PAIR,
                U03EvaluationOnlyClinicalEvaluator.EVALSET_RELEASE);
        for (String ref : refs) if (ref.toLowerCase().contains("latest")) return false;
        return true;
    }

    private U03EvaluationOnlyClinicalEvaluator.Fixture base() {
        return new U03EvaluationOnlyClinicalEvaluator.Fixture()
                .evidence("EV-RF-RESP-001", absent())
                .evidence("EV-MNM-NEURO-001", absent())
                .evidence("EV-MNM-NEURO-002", absent())
                .evidence("EV-MNM-CARD-001", absent())
                .evidence("EV-RF-ALLERGY-001", absent())
                .evidence("EV-RF-APPEAR-001", absent())
                .evidence("EV-RF-NEURO-001", absent())
                .evidence("EV-RF-SEPSIS-001", absent());
    }

    private U03EvaluationOnlyClinicalEvaluator.Fixture sepsisNoSignal() {
        return base().sepsis(t()).rr(20).sbp(110).usualSbp(120, true).hr(80)
                .evidence("EV-RF-APPEAR-001", absent()).evidence("EV-RF-SEPSIS-001", absent());
    }

    private GoldenCase gc(String id, U03EvaluationOnlyClinicalEvaluator.Fixture f, String status, String disposition, String reason) {
        return new GoldenCase(id, f, status, disposition, reason);
    }
    private Check check(String id, boolean passed, String detail) { return new Check(id, passed, detail); }
    private U03EvaluationOnlyClinicalEvaluator.FactState present() { return U03EvaluationOnlyClinicalEvaluator.FactState.PRESENT; }
    private U03EvaluationOnlyClinicalEvaluator.FactState absent() { return U03EvaluationOnlyClinicalEvaluator.FactState.ABSENT; }
    private U03EvaluationOnlyClinicalEvaluator.FactState unknown() { return U03EvaluationOnlyClinicalEvaluator.FactState.UNKNOWN; }
    private U03EvaluationOnlyClinicalEvaluator.TriState t() { return U03EvaluationOnlyClinicalEvaluator.TriState.TRUE; }
    private U03EvaluationOnlyClinicalEvaluator.TriState f() { return U03EvaluationOnlyClinicalEvaluator.TriState.FALSE; }
    private static boolean eq(Object a, Object b) { return a == null ? b == null : a.equals(b); }
    private static String triple(String status, String disposition, String reason) { return status + "/" + (disposition == null ? "NONE" : disposition) + "/" + reason; }

    private void writeBundle(List<String> lines) throws IOException {
        Path out = Paths.get("target", "u03-gatec-evaluation-result.txt");
        Files.createDirectories(out.getParent());
        Files.write(out, lines, StandardCharsets.UTF_8);
        assertTrue(Files.exists(out));
        assertFalse(lines.isEmpty());
    }
}
