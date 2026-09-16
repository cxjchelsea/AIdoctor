import unittest

from cases import (
    excluded_unproducible_cases,
    golden_cases,
    p4_both_families_not_applicable_fixture,
    p4_dyspnoea_applicable_fixture,
    p4_sepsis_applicable_fixture,
    safety_cases,
)
from evaluator import ALL_RULES, CURRENT_REFS, ClinicalEvaluator, evaluate_rules, has_shared_scope_conflict
from run_evaluation import (
    executable_side_effect_boundary_evidence,
    p5_defensive_contract_boundary_check,
    run_golden,
    run_safety,
    shared_scope_invariant_evidence,
)


class GateCEvaluatorTest(unittest.TestCase):
    def test_exact_governed_refs_are_pinned(self):
        self.assertNotIn("latest", " ".join(CURRENT_REFS.values()).lower())
        self.assertEqual(5, len(CURRENT_REFS))

    def test_all_15_frozen_rule_ids_are_implemented_and_execute(self):
        self.assertEqual(15, len(ALL_RULES))
        self.assertEqual(15, len(set(ALL_RULES)))
        for fixture in (
            p4_both_families_not_applicable_fixture(),
            p4_dyspnoea_applicable_fixture(),
            p4_sepsis_applicable_fixture(),
        ):
            results = evaluate_rules(fixture)
            self.assertEqual(15, len(results))
            self.assertFalse(has_shared_scope_conflict(results))

    def test_executable_golden_cases_pass_without_injected_c_results(self):
        results = run_golden()
        self.assertEqual(30, len(results))
        self.assertNotIn("GC-026", {r["case_id"] for r in results})
        failures = {r["case_id"]: r["errors"] for r in results if r["status"] != "PASS"}
        self.assertEqual({}, failures)
        for spec in golden_cases():
            self.assertNotIn("forced_rule_results", spec["fixture"])
            self.assertNotIn("forced_family_conflict", spec["fixture"])

    def test_executable_safety_cases_pass_without_stub_conflict(self):
        results = run_safety()
        self.assertEqual(19, len(results))
        self.assertNotIn("SS-012", {r["case_id"] for r in results})
        failures = {r["case_id"]: r["errors"] for r in results if r["status"] != "PASS"}
        self.assertEqual({}, failures)
        self.assertTrue(all(r["critical_blocking"] for r in results))
        self.assertEqual(19, len(safety_cases()))

    def test_bf_ir_01_unproducible_cases_are_explicitly_excluded(self):
        excluded = {x["case_id"]: x for x in excluded_unproducible_cases()}
        self.assertEqual({"GC-026", "SS-012"}, set(excluded))
        self.assertTrue(all(x["classification"] == "UNPRODUCIBLE_UNDER_SHARED_SCOPE" for x in excluded.values()))
        self.assertTrue(all(x["executable_counted"] is False for x in excluded.values()))

    def test_bf_ir_02_p4_fixtures_are_independent(self):
        fixtures = [
            p4_both_families_not_applicable_fixture(),
            p4_dyspnoea_applicable_fixture(),
            p4_sepsis_applicable_fixture(),
        ]
        self.assertNotEqual(fixtures[0], fixtures[1])
        self.assertNotEqual(fixtures[0], fixtures[2])
        self.assertNotEqual(fixtures[1], fixtures[2])
        outcomes = [ClinicalEvaluator().evaluate(f) for f in fixtures]
        self.assertTrue(all(o.decision.policy_ref == "D09-P-040" for o in outcomes))
        self.assertTrue(all(len(o.rule_results) == 15 for o in outcomes))

    def test_shared_scope_invariant_and_p5_defensive_boundary(self):
        shared = shared_scope_invariant_evidence()
        self.assertEqual("PASS", shared["status"], shared["errors"])
        p5 = p5_defensive_contract_boundary_check()
        self.assertEqual("PASS", p5["status"], p5["errors"])
        self.assertFalse(p5["counts_as_governed_c_execution"])
        self.assertEqual(15, p5["honest_c_rule_count"])
        self.assertEqual("D09-P-090", p5["decision"]["policy_ref"])

    def test_ss_014_ss_016_have_executable_boundary_evidence(self):
        evidence = executable_side_effect_boundary_evidence()
        self.assertTrue(evidence["pass"], evidence)
        self.assertEqual([], evidence["forbidden_commit_calls"])
        self.assertEqual([], evidence["forbidden_u04_calls"])
        self.assertEqual([], evidence["forbidden_runtime_imports"])


if __name__ == "__main__":
    unittest.main()
