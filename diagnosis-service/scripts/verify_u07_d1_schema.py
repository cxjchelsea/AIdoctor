#!/usr/bin/env python3
"""Static, database-free parity check for U07 D1 Flyway V7 migrations.

Run from any working directory:
    python3 diagnosis-service/scripts/verify_u07_d1_schema.py

This validates SQL text structure ONLY: it does not parse full SQL grammar, open a
database connection, run Flyway, or prove actual MySQL/Oracle DDL acceptance.
"""
from __future__ import annotations

import re
import unittest
from pathlib import Path

RESOURCES = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "db"
NAME = "V7__create_u07_event_application_and_outbox.sql"
TABLES = {"u07_event_application", "u07_effect_outbox"}
FIELDS = {
    "u07_event_application": {
        "event_id", "consultation_id", "question_id", "parent_wait_effect_id",
        "payload_digest", "source_event_ref", "source_version_ref",
        "source_state_version", "business_policy_ref", "trace_ref", "phase",
        "decision", "row_version", "owner_token", "lease_expires_at",
        "receipt_ref", "effect_id", "effect_fingerprint",
        "created_at", "updated_at", "terminal_at",
    },
    "u07_effect_outbox": {
        "effect_id", "event_id", "target_type", "payload_ref_or_digest",
        "effect_status", "attempt", "owner_token", "lease_expires_at",
        "last_error_class", "created_at", "updated_at",
    },
}
REQUIRED_NOT_NULL = {
    "u07_event_application": {
        "event_id", "consultation_id", "question_id", "parent_wait_effect_id",
        "payload_digest", "source_event_ref", "source_version_ref",
        "source_state_version", "phase", "row_version", "created_at", "updated_at",
    },
    "u07_effect_outbox": {
        "effect_id", "event_id", "target_type", "payload_ref_or_digest",
        "effect_status", "attempt", "created_at", "updated_at",
    },
}
INDEXES = {
    "idx_u07_app_consultation",
    "idx_u07_app_wait",
    "idx_u07_app_phase",
    "idx_u07_outbox_event",
    "idx_u07_outbox_poll",
}
# Tables only contain fields followed by optional constraints. Exclude table
# constraint/index declarations when comparing canonical column names.
IGNORE_PREFIXES = ("primary key", "unique key", "key ", "constraint ")


def read(dialect: str) -> str:
    return (RESOURCES / dialect / NAME).read_text(encoding="utf-8")


def table_body(sql: str, table: str) -> str:
    match = re.search(
        r"\bCREATE\s+TABLE\s+" + table + r"\s*\((.*?)\)\s*;",
        sql,
        flags=re.IGNORECASE | re.DOTALL,
    )
    if match is None:
        raise AssertionError("missing table " + table)
    return match.group(1)


def columns(sql: str, table: str) -> dict[str, str]:
    # Definitions are one column per line in this intentionally fixed migration.
    result = {}
    for line in table_body(sql, table).splitlines():
        stripped = line.strip().rstrip(",")
        if not stripped or stripped.lower().startswith(IGNORE_PREFIXES):
            continue
        name, _, ddl = stripped.partition(" ")
        if not ddl:
            raise AssertionError("unparseable column: " + stripped)
        result[name.lower()] = ddl.upper()
    return result


class U07D1SchemaTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.mysql = read("migration")
        cls.oracle = read("migration-oracle")

    def test_v7_exists_as_dialect_pair(self) -> None:
        self.assertTrue((RESOURCES / "migration" / NAME).is_file())
        self.assertTrue((RESOURCES / "migration-oracle" / NAME).is_file())

    def test_exactly_two_u07_tables(self) -> None:
        for sql in (self.mysql, self.oracle):
            self.assertEqual(
                {m.lower() for m in re.findall(r"\bCREATE\s+TABLE\s+(\w+)", sql, re.I)},
                TABLES,
            )

    def test_column_sets_equal_and_complete(self) -> None:
        for table, expected in FIELDS.items():
            self.assertEqual(set(columns(self.mysql, table)), expected)
            self.assertEqual(set(columns(self.oracle, table)), expected)

    def test_required_columns_are_not_nullable(self) -> None:
        for sql in (self.mysql, self.oracle):
            for table, expected in REQUIRED_NOT_NULL.items():
                for field in expected:
                    self.assertIn("NOT NULL", columns(sql, table)[field], (table, field))

    def test_primary_unique_and_indexes(self) -> None:
        for sql in (self.mysql, self.oracle):
            application = table_body(sql, "u07_event_application")
            outbox = table_body(sql, "u07_effect_outbox")
            self.assertRegex(application.upper(), r"PRIMARY\s+KEY\s*\(EVENT_ID\)")
            self.assertRegex(outbox.upper(), r"PRIMARY\s+KEY\s*\(EFFECT_ID\)")
            self.assertRegex(application.upper(), r"(?:UNIQUE\s+KEY\s+\w+|UNIQUE)\s*\(EFFECT_ID\)")
            self.assertEqual(
                {i.lower() for i in re.findall(
                    r"\b(?:KEY\s+|CREATE\s+INDEX\s+)(idx_u07_\w+)", sql, re.I
                )},
                INDEXES,
            )

    def test_no_dml_existing_table_mutation_or_dispatch(self) -> None:
        for sql in (self.mysql, self.oracle):
            without_comments = re.sub(r"--[^\n]*", "", sql)
            self.assertNotRegex(
                without_comments.upper(),
                r"\b(?:INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|MERGE|EXECUTE|CALL)\b",
            )
            self.assertNotRegex(
                without_comments.upper(), r"\bCREATE\s+TABLE\s+CANONICAL_BUSINESS_EVENT\b"
            )

    def test_dialect_specific_data_types(self) -> None:
        self.assertIn("VARCHAR(128)", self.mysql)
        self.assertIn("BIGINT", self.mysql)
        self.assertIn("DATETIME", self.mysql)
        self.assertNotIn("VARCHAR2(", self.mysql)
        self.assertIn("VARCHAR2(128 CHAR)", self.oracle)
        self.assertIn("NUMBER(19)", self.oracle)
        self.assertIn("TIMESTAMP", self.oracle)
        self.assertNotIn("DATETIME", self.oracle)


if __name__ == "__main__":
    unittest.main(verbosity=2)
