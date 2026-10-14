"""Fail-closed U07 release gate tests: even hash-complete evidence is not authority."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("u07_release_gate.py")
SPEC = importlib.util.spec_from_file_location("u07_release_gate", SCRIPT)
gate_module = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(gate_module)


class U07ReleaseEvidenceGateTest(unittest.TestCase):
    def test_no_manifest_is_blocked(self):
        result = gate_module.gate({}, Path("."))
        self.assertEqual("BLOCK", result["decision"])
        self.assertIn("EVIDENCE_MAP_MISSING", result["failures"])
        self.assertEqual("BLOCK", result["production_patient_delivery"])

    def test_complete_hashes_are_not_production_approval(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = {
                "platform": "mysql8",
                "source_main_sha": gate_module.EXPECTED_MAIN,
                "evidence": {},
            }
            for name in gate_module.REQUIRED:
                path = root / (name + ".txt")
                content = ("synthetic-untrusted-" + name).encode("utf-8")
                path.write_bytes(content)
                manifest["evidence"][name] = {
                    "path": path.name,
                    "sha256": hashlib.sha256(content).hexdigest(),
                }
            result = gate_module.gate(manifest, root)
            self.assertEqual([], result["failures"])
            self.assertTrue(all(x["intake"] == "HASH_MATCH_ONLY"
                                for x in result["evidence"].values()))
            self.assertEqual("BLOCK", result["decision"])
            self.assertEqual("INDEPENDENT_APPROVAL_REQUIRED", result["release_authority"])

    def test_digest_and_path_tampering_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = {
                "platform": "mysql8",
                "source_main_sha": gate_module.EXPECTED_MAIN,
                "evidence": {"flyway_history": {"path": "../secret.txt", "sha256": "0" * 64},
                             "durable_wait": {"path": "missing.txt", "sha256": "0" * 64}},
            }
            result = gate_module.gate(manifest, root)
            self.assertIn("UNSAFE_PATH_FLYWAY_HISTORY", result["failures"])
            self.assertIn("UNVERIFIED_DURABLE_WAIT", result["failures"])
            self.assertEqual("BLOCK", result["decision"])

    def test_oracle_requires_physical_evidence(self):
        result = gate_module.gate({
            "platform": "dual",
            "source_main_sha": gate_module.EXPECTED_MAIN,
            "evidence": {},
        }, Path("."))
        self.assertIn("MISSING_ORACLE_PHYSICAL", result["failures"])

    def test_unreviewed_source_sha_is_blocked(self):
        result = gate_module.gate({
            "platform": "mysql8",
            "source_main_sha": "a" * 40,
            "evidence": {},
        }, Path("."))
        self.assertIn("SOURCE_BASELINE_NOT_REVIEWED", result["failures"])


if __name__ == "__main__":
    unittest.main()
