#!/usr/bin/env python3
"""Pure stdlib offline tests. No DB/network/application credentials."""
import unittest

from validate_u07_migration_history_evidence import classify, validate

BASE = {
    "schema_version": 1,
    "environment_alias": "SYNTHETIC_TEST",
    "source_kind": "synthetic_fixture",
    "owner_approved": False,
    "approval_ref_present": False,
    "engine": "mysql",
    "engine_major": 8,
    "flyway_major": 7,
    "migration_chain": "mysql",
    "history_table": "present",
    "history_complete": True,
    "highest_success_version": 6,
    "v1_checksum_relation": "match",
    "failed_migration_present": False,
    "manual_baseline_or_repair": False,
    "legacy_text_columns_verified": True,
    "v1_source_relation": "match",
}


def metadata(**replacements):
    return dict(BASE, **replacements)


class ReadOnlyMigrationHistoryEvidenceTests(unittest.TestCase):
    def test_synthetic_fixture_never_attests_real_environment(self):
        result = classify(validate(metadata()))
        self.assertEqual("UNKNOWN", result["classification"])
        self.assertFalse(result["production_migration_authorized"])
        self.assertFalse(result["flyway_repair_authorized"])
        self.assertFalse(result["merge_authorized"])

    def test_unapproved_environment_is_unknown(self):
        result = classify(validate(metadata(source_kind="owner_supplied_metadata")))
        self.assertEqual("UNKNOWN", result["classification"])

    def test_owner_reported_success_is_not_independent_proof(self):
        result = classify(validate(metadata(
            source_kind="owner_supplied_metadata", owner_approved=True,
            approval_ref_present=True)))
        self.assertEqual("E1_SUCCESS_HISTORY_REPORTED", result["classification"])
        self.assertFalse(result["checksums_and_actual_schema_independently_verified"])

    def test_no_history(self):
        result = classify(validate(metadata(
            source_kind="owner_supplied_metadata", owner_approved=True,
            approval_ref_present=True, history_table="absent", highest_success_version=None)))
        self.assertEqual("E0_NO_HISTORY", result["classification"])

    def test_checksum_mismatch(self):
        result = classify(validate(metadata(
            source_kind="owner_supplied_metadata", owner_approved=True,
            approval_ref_present=True, v1_checksum_relation="mismatch")))
        self.assertEqual("E2_CHECKSUM_MISMATCH", result["classification"])

    def test_failed_or_partial_history(self):
        result = classify(validate(metadata(
            source_kind="owner_supplied_metadata", owner_approved=True,
            approval_ref_present=True, failed_migration_present=True)))
        self.assertEqual("E3_FAILED_OR_PARTIAL", result["classification"])

    def test_manual_baseline_priority(self):
        result = classify(validate(metadata(
            source_kind="owner_supplied_metadata", owner_approved=True,
            approval_ref_present=True, manual_baseline_or_repair=True,
            v1_checksum_relation="mismatch")))
        self.assertEqual("E4_MANUAL_OR_UNKNOWN_BASELINE", result["classification"])

    def test_unknown_checksum_remains_unknown(self):
        result = classify(validate(metadata(
            source_kind="owner_supplied_metadata", owner_approved=True,
            approval_ref_present=True, v1_checksum_relation="unknown")))
        self.assertEqual("UNKNOWN", result["classification"])

    def test_rejects_secrets_and_unexpected_fields(self):
        with self.assertRaises(ValueError):
            validate(metadata(password="secret"))
        with self.assertRaises(ValueError):
            validate(metadata(jdbc_url="jdbc:mysql://host/db"))

    def test_rejects_nonboolean_flags_and_alias_with_host(self):
        with self.assertRaises(ValueError):
            validate(metadata(owner_approved="true"))
        with self.assertRaises(ValueError):
            validate(metadata(environment_alias="jdbc:mysql://host/db"))

    def test_no_programmatic_authorization_in_any_classification(self):
        for overrides in (
            {}, {"history_table": "absent"}, {"v1_checksum_relation": "mismatch"},
            {"failed_migration_present": True}, {"manual_baseline_or_repair": True},
        ):
            result = classify(validate(metadata(
                source_kind="owner_supplied_metadata", owner_approved=True,
                approval_ref_present=True, **overrides)))
            self.assertFalse(result["production_migration_authorized"])
            self.assertFalse(result["flyway_repair_authorized"])
            self.assertFalse(result["merge_authorized"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
