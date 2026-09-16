import unittest

from evaluator import ALL_RULES, CURRENT_REFS, ClinicalEvaluator
from cases import golden_cases
from run_evaluation import run_golden, run_safety


class GateCEvaluatorTest(unittest.TestCase):
    def test_exact_governed_refs_are_pinned(self):
        self.assertNotIn("latest", " ".join(CURRENT_REFS.values()).lower())
        self.assertEqual(5, len(CURRENT_REFS))

    def test_all_15_frozen_rule_ids_are_implemented(self):
        self.assertEqual(15, len(ALL_RULES))
        self.assertEqual(15, len(set(ALL_RULES)))

    def test_31_approved_golden_cases_execute(self):
        results = run_golden()
        self.assertEqual(31, len(results))
        failures = {r["case_id"]: r["errors"] for r in results if r["status"] != "PASS"}
        self.assertEqual({}, failures)

    def test_20_critical_safety_cases_execute(self):
        results = run_safety()
        self.assertEqual(20, len(results))
        failures = {r["case_id"]: r["errors"] for r in results if r["status"] != "PASS"}
        self.assertEqual({}, failures)
        self.assertTrue(all(r["critical_blocking"] for r in results))

    def test_evaluator_has_no_commit_or_u04_effect(self):
        fixture = golden_cases()[0]["fixture"]
        outcome = ClinicalEvaluator().evaluate(fixture)
        self.assertFalse(outcome.commit_attempted)
        self.assertIsNone(outcome.u04_decision)


if __name__ == "__main__":
    unittest.main()
