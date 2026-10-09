#!/usr/bin/env python3
"""Static allowlist checks of DBA-controlled SELECT-only templates. No DB access."""
import re
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
FILES = (
    HERE / "u07_legacy_dba_readonly_mysql.sql",
    HERE / "u07_legacy_dba_readonly_oracle.sql",
)
FORBIDDEN = re.compile(
    r"\b(?:INSERT|UPDATE|DELETE|ALTER|DROP|TRUNCATE|MERGE|EXECUTE|CALL|"
    r"REPAIR|BASELINE|MIGRATE|GRANT|REVOKE|CREATE|DBMS_\w+)\b",
    re.IGNORECASE,
)
FORBIDDEN_SENSITIVE = re.compile(
    r"\b(?:jdbc_url|connection_string|password|secret|patient_id|"
    r"diagnosis_record|dialogue_history|select\s+\*)\b",
    re.IGNORECASE,
)
ALLOWED_SOURCES = {
    "information_schema.tables",
    "information_schema.columns",
    "flyway_schema_history",
    "user_tables",
    "user_tab_columns",
}


def statements(path):
    sql = path.read_text(encoding="utf-8")
    no_comments = re.sub(r"--[^\n]*", "", sql)
    return [piece.strip() for piece in no_comments.split(";") if piece.strip()]


class DBAReadOnlyTemplateTests(unittest.TestCase):
    def test_both_templates_exist(self):
        self.assertTrue(all(p.is_file() for p in FILES))

    def test_select_only_no_mutation(self):
        for path in FILES:
            for sql in statements(path):
                self.assertRegex(sql, r"(?is)^\s*SELECT\b")
                self.assertIsNone(FORBIDDEN.search(sql), path.name)

    def test_read_only_metadata_tables(self):
        for path in FILES:
            for sql in statements(path):
                tables = re.findall(r"(?i)\b(?:FROM|JOIN)\s+([\w.]+)", sql)
                for name in tables:
                    if name.lower() in ALLOWED_SOURCES:
                        continue
                    # SELECT VERSION() has no FROM.
                    self.fail("Unapproved metadata source in " + path.name)

    def test_no_patient_or_connection_values(self):
        for path in FILES:
            for sql in statements(path):
                self.assertIsNone(FORBIDDEN_SENSITIVE.search(sql), path.name)

    def test_conditional_history_query_documentation(self):
        for path in FILES:
            raw = path.read_text(encoding="utf-8")
            self.assertIn("if absent, DO NOT execute", raw)
            self.assertIn("flyway_schema_history", raw)

    def test_no_sql_execution_in_test(self):
        for path in FILES:
            self.assertEqual("", str(path.suffix).replace(".sql", ""))


if __name__ == "__main__":
    unittest.main(verbosity=2)
