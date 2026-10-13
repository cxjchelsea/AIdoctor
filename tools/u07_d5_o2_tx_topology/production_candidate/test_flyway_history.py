#!/usr/bin/env python3
"""Adversarial synthetic-only checks for history admission; no database access."""
import copy
import importlib.util
import pathlib
import unittest

HERE = pathlib.Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("history_gate", HERE / "verify_flyway_history.py")
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)


def fixture(engine="mysql8"):
    checks = {str(x): x * 100 - 7 for x in range(1, 7)}
    approved = {
        "engine": engine,
        "approval_status": "APPROVED_BY_DATABASE_OWNER",
        "checksums": checks,
    }
    history = {
        "engine": engine,
        "source": "read_only_flyway_schema_history_export",
        "rows": [
            {"version": str(x), "checksum": checks[str(x)],
             "type": "SQL", "success": True} for x in range(1, 7)
        ],
    }
    # These are invented test-only checksums, NEVER real deployment evidence.
    return history, approved


class HistoricalGateTests(unittest.TestCase):
    def test_exact_synthetic_history_allows_review_not_migration(self):
        h, a = fixture()
        result = gate.evaluate(h, a, engine="mysql8")
        self.assertEqual(result["decision"], "ALLOW_REVIEW_ONLY")
        self.assertFalse(result["migration_authorized"])

    def test_new_install_is_separate_release_path(self):
        h, a = fixture()
        h["rows"] = []
        self.assertEqual(gate.evaluate(h, a, engine="mysql8")["reason"],
                         "NO_APPLIED_HISTORY_NEW_INSTALL_REQUIRES_SEPARATE_BASELINE")

    def test_missing_owner_checksum_attestation_blocks(self):
        h, a = fixture()
        a["approval_status"] = "PENDING"
        self.assertEqual(gate.evaluate(h, a, engine="mysql8")["decision"], "BLOCK")

    def test_mutated_checksum_blocks(self):
        h, a = fixture()
        h["rows"][0]["checksum"] += 1
        self.assertEqual(gate.evaluate(h, a, engine="mysql8")["reason"],
                         "DEPLOYED_CHECKSUM_MISMATCH")

    def test_baseline_or_repair_like_row_blocks(self):
        h, a = fixture()
        h["rows"][0]["type"] = "BASELINE"
        self.assertEqual(gate.evaluate(h, a, engine="mysql8")["reason"],
                         "NON_SQL_OR_BASELINED_HISTORY")

    def test_failed_history_blocks(self):
        h, a = fixture()
        h["rows"][3]["success"] = False
        self.assertEqual(gate.evaluate(h, a, engine="mysql8")["reason"],
                         "HISTORY_FAILED_OR_INCOMPLETE")

    def test_extra_version_blocks(self):
        h, a = fixture()
        h["rows"].append({"version": "7", "checksum": 700,
                          "type": "SQL", "success": True})
        self.assertEqual(gate.evaluate(h, a, engine="mysql8")["reason"],
                         "HISTORY_NOT_EXACT_V1_TO_V6")

    def test_missing_version_blocks(self):
        h, a = fixture()
        h["rows"].pop(2)
        self.assertEqual(gate.evaluate(h, a, engine="mysql8")["decision"], "BLOCK")

    def test_engine_binding_blocks(self):
        h, a = fixture()
        self.assertEqual(gate.evaluate(h, a, engine="oracle")["reason"],
                         "ENGINE_BINDING_MISMATCH")

    def test_oracle_never_allows_runtime(self):
        h, a = fixture("oracle")
        self.assertEqual(gate.evaluate(h, a, engine="oracle")["reason"],
                         "ORACLE_RUNTIME_SQL_DIALECT_UNVERIFIED")

    def test_untrusted_origin_blocks(self):
        h, a = fixture()
        h["source"] = "copied_by_hand"
        self.assertEqual(gate.evaluate(h, a, engine="mysql8")["reason"],
                         "PROVENANCE_NOT_ATTESTED")

    def test_bad_checksum_manifest_blocks(self):
        h, a = fixture()
        del a["checksums"]["3"]
        self.assertEqual(gate.evaluate(h, a, engine="mysql8")["reason"],
                         "APPROVED_CHECKSUM_MANIFEST_INCOMPLETE")


if __name__ == "__main__":
    unittest.main()
