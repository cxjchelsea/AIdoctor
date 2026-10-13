#!/usr/bin/env python3
"""Fail-closed preflight for *exported* Flyway schema history evidence.

No database connection, no credentials, no DDL, no Flyway repair/baseline action.
Only canonical V1..V6 history with independently approved checksums can
qualify for a candidate V7/V8 deployment review. This tool never deploys.
"""
import argparse
import json
import pathlib
import sys

ALLOWED_TYPES = {"SQL"}
EXPECTED = [str(x) for x in range(1, 7)]


def deny(reason):
    return {"decision": "BLOCK", "reason": reason, "migration_authorized": False}


def evaluate(history, approved, *, engine):
    if engine not in ("mysql8", "oracle"):
        return deny("UNSUPPORTED_DATABASE_ENGINE")
    if not isinstance(history, dict) or not isinstance(approved, dict):
        return deny("EVIDENCE_SHAPE_INVALID")
    if history.get("engine") != engine or approved.get("engine") != engine:
        return deny("ENGINE_BINDING_MISMATCH")
    if history.get("source") != "read_only_flyway_schema_history_export":
        return deny("PROVENANCE_NOT_ATTESTED")
    if approved.get("approval_status") != "APPROVED_BY_DATABASE_OWNER":
        return deny("APPROVED_CHECKSUM_MANIFEST_ABSENT")
    if not all(isinstance(approved.get("checksums", {}).get(v), int) for v in EXPECTED):
        return deny("APPROVED_CHECKSUM_MANIFEST_INCOMPLETE")
    if set(approved["checksums"]) != set(EXPECTED):
        return deny("APPROVED_CHECKSUM_MANIFEST_NOT_EXACT")
    entries = history.get("rows")
    if not isinstance(entries, list):
        return deny("HISTORY_ROWS_INVALID")
    if not entries:
        return deny("NO_APPLIED_HISTORY_NEW_INSTALL_REQUIRES_SEPARATE_BASELINE")
    if any(not isinstance(x, dict) for x in entries):
        return deny("HISTORY_ROW_INVALID")
    if any(x.get("success") is not True for x in entries):
        return deny("HISTORY_FAILED_OR_INCOMPLETE")
    # A migration history can contain baseline, repeatable or extra versions.
    # Never silently drop such evidence to manufacture a clean V6 lineage.
    if len(entries) != 6 or [str(x.get("version")) for x in entries] != EXPECTED:
        return deny("HISTORY_NOT_EXACT_V1_TO_V6")
    if any(x.get("type") not in ALLOWED_TYPES for x in entries):
        return deny("NON_SQL_OR_BASELINED_HISTORY")
    if any(not isinstance(x.get("checksum"), int) for x in entries):
        return deny("CHECKSUM_MISSING")
    if any(x["checksum"] != approved["checksums"][str(x["version"])] for x in entries):
        return deny("DEPLOYED_CHECKSUM_MISMATCH")
    if engine == "oracle":
        return deny("ORACLE_RUNTIME_SQL_DIALECT_UNVERIFIED")
    # ALLOW_REVIEW does not imply direct apply: environment backup, rollback,
    # migration DryRun, privilege, version compatibility are separate gates.
    return {
        "decision": "ALLOW_REVIEW_ONLY",
        "reason": "EXACT_APPROVED_V1_TO_V6_HISTORY",
        "migration_authorized": False,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--history", required=True, type=pathlib.Path)
    parser.add_argument("--approved", required=True, type=pathlib.Path)
    parser.add_argument("--engine", choices=("mysql8", "oracle"), required=True)
    opts = parser.parse_args()
    result = evaluate(json.loads(opts.history.read_text(encoding="utf8")),
                      json.loads(opts.approved.read_text(encoding="utf8")),
                      engine=opts.engine)
    print(json.dumps(result, sort_keys=True))
    return 0 if result["decision"] == "ALLOW_REVIEW_ONLY" else 2


if __name__ == "__main__":
    sys.exit(main())
