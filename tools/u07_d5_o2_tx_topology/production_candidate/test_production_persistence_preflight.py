"""Offline and fail-closed baseline checks, no DB access or migration."""
import importlib.util
import tempfile
from pathlib import Path
import unittest

SCRIPT = Path(__file__).with_name("production_persistence_preflight.py")
spec = importlib.util.spec_from_file_location("u07_persistence_preflight", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
REPO = Path(__file__).resolve().parents[3]

class ProductionPersistencePreflightTest(unittest.TestCase):
    def test_actual_repository_baseline_is_not_release_authorized(self):
        x = module.inspect(REPO)
        self.assertEqual("PASS", x["structural_invariants"])
        self.assertEqual("BLOCK", x["production_migration_decision"])
        self.assertEqual("BLOCK", x["production_clinical_state_activation"])
        self.assertEqual(9, x["observations"]["original_mysql_v1_clob_tokens"])
        self.assertEqual("dev", x["observations"]["main_app_default_profile"])
        self.assertTrue(x["observations"]["candidate_v7_v8_outside_default_flyway"])
        self.assertFalse(x["observations"]["actual_deployed_platform_verified"])

    def test_candidate_promoted_to_default_flyway_is_detected(self):
        with tempfile.TemporaryDirectory() as td:
            d = Path(td)
            self._copy_relevant(REPO, d)
            target = d / module.MIGRATION / "V7__u07_recovery_claim.sql"
            target.write_text("CREATE TABLE unsafe_promoted_candidate(x INT);\n")
            x = module.inspect(d)
            self.assertEqual("FAIL", x["structural_invariants"])
            self.assertEqual("BLOCK", x["production_migration_decision"])

    def test_legacy_v1_change_is_detected_without_repairing_history(self):
        with tempfile.TemporaryDirectory() as td:
            d = Path(td)
            self._copy_relevant(REPO, d)
            v1 = d / module.MIGRATION / "V1__create_agent_state_and_audit_trail.sql"
            v1.write_text(v1.read_text().replace("CLOB", "LONGTEXT"))
            x = module.inspect(d)
            self.assertEqual("FAIL", x["structural_invariants"])
            self.assertEqual(0, x["observations"]["original_mysql_v1_clob_tokens"])
            self.assertEqual("BLOCK", x["production_migration_decision"])

    def _copy_relevant(self, source, destination):
        for directory in (module.MIGRATION, module.ORACLE, module.CANDIDATE + "/mysql8",
                          module.CANDIDATE + "/oracle"):
            p = destination / directory
            p.mkdir(parents=True)
            for f in (source / directory).glob("V*.sql"):
                (p / f.name).write_bytes(f.read_bytes())
        original = source / "diagnosis-service/src/main/resources/application.yml"
        target = destination / "diagnosis-service/src/main/resources/application.yml"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(original.read_bytes())

if __name__ == "__main__":
    unittest.main()
