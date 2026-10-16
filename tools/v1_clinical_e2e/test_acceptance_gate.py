import importlib.util
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).parent
spec = importlib.util.spec_from_file_location("v1_acceptance_gate", ROOT / "acceptance_gate.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class ClinicalAcceptanceGateTests(unittest.TestCase):
    def test_all_unexecuted_stays_not_accepted(self):
        payload = json.loads((ROOT / "scenarios.json").read_text(encoding="utf-8"))
        result = module.evaluate(payload)
        self.assertEqual("NOT_ACCEPTED", result["decision"])
        self.assertEqual(0, result["actual_full_e2e_cases_passed"])
        self.assertEqual(10, result["required_scenarios"])

    def test_forged_pass_status_is_not_live_evidence(self):
        payload = json.loads((ROOT / "scenarios.json").read_text(encoding="utf-8"))
        for case in payload["tests"]:
            case["evidence_status"] = "PASS"
        result = module.evaluate(payload)
        self.assertEqual(0, result["actual_full_e2e_cases_passed"])
        self.assertEqual("NOT_ACCEPTED", result["decision"])

    def test_missing_case_is_not_a_complete_suite(self):
        payload = json.loads((ROOT / "scenarios.json").read_text(encoding="utf-8"))
        payload["tests"].pop()
        self.assertEqual("INVALID_CASE_MANIFEST", module.evaluate(payload)["reason"])

    def test_duplicate_case_is_not_a_complete_suite(self):
        payload = json.loads((ROOT / "scenarios.json").read_text(encoding="utf-8"))
        payload["tests"][9]["case_id"] = "E2E-01"
        self.assertEqual("INVALID_CASE_MANIFEST", module.evaluate(payload)["reason"])

if __name__ == "__main__":
    unittest.main()
