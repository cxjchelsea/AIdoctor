#!/usr/bin/env python3
"""Offline, allowlisted, no-network U07 Flyway history evidence classifier.

Requires manually approved, sanitized JSON metadata assembled by an environment
owner. NEVER connects to a database. Output contains no source metadata values
other than coarse version/status classes, and is NEVER an upgrade authorization.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

ALLOWED = frozenset({
    "schema_version", "environment_alias", "source_kind", "owner_approved",
    "approval_ref_present", "engine", "engine_major", "flyway_major",
    "migration_chain", "history_table", "history_complete",
    "highest_success_version", "v1_checksum_relation",
    "failed_migration_present", "manual_baseline_or_repair",
    "legacy_text_columns_verified", "v1_source_relation",
})
ENGINES = {"mysql", "oracle", "unknown"}
HISTORY = {"present", "absent", "unknown"}
RELATION = {"match", "mismatch", "unknown"}
CHAIN = {"mysql", "oracle", "unknown"}
SOURCE = {"synthetic_fixture", "owner_supplied_metadata"}
MAX_BYTES = 4096


def validate(data: object) -> dict:
    if not isinstance(data, dict):
        raise ValueError("JSON root must be an object")
    extra = set(data) - ALLOWED
    if extra:
        # Never reflect rejected arbitrary field names, as they might be secrets.
        raise ValueError("Unexpected fields; use only the documented allowlist")
    if data.get("schema_version") != 1:
        raise ValueError("Unsupported schema_version")
    for field, allowed in (
        ("engine", ENGINES), ("history_table", HISTORY),
        ("v1_checksum_relation", RELATION), ("v1_source_relation", RELATION),
        ("migration_chain", CHAIN), ("source_kind", SOURCE),
    ):
        if data.get(field) not in allowed:
            raise ValueError("Missing or invalid metadata enum")
    for field in ("owner_approved", "approval_ref_present", "history_complete",
                  "failed_migration_present", "manual_baseline_or_repair",
                  "legacy_text_columns_verified"):
        if type(data.get(field)) is not bool:
            raise ValueError("Missing or invalid metadata boolean")
    for field in ("engine_major", "flyway_major", "highest_success_version"):
        v = data.get(field)
        if v is not None and (type(v) is not int or v < 0 or v > 10000):
            raise ValueError("Invalid metadata version number")
    alias = data.get("environment_alias")
    if not isinstance(alias, str) or not re.fullmatch(r"[A-Z0-9_-]{2,32}", alias):
        raise ValueError("Invalid sanitized environment alias")
    return data


def classify(data: dict) -> dict:
    # A synthetic example can exercise logic, not attest to a deployed DB.
    attestable = (data["source_kind"] == "owner_supplied_metadata"
                  and data["owner_approved"] and data["approval_ref_present"])
    evidence = "OWNER_METADATA_UNVERIFIED" if attestable else "NOT_ATTESTED"
    if not attestable or data["history_table"] == "unknown":
        state = "UNKNOWN"
    elif data["manual_baseline_or_repair"]:
        state = "E4_MANUAL_OR_UNKNOWN_BASELINE"
    elif data["failed_migration_present"] or not data["history_complete"]:
        state = "E3_FAILED_OR_PARTIAL"
    elif data["history_table"] == "absent":
        state = "E0_NO_HISTORY"
    elif (data["v1_checksum_relation"] == "mismatch"
          or data["v1_source_relation"] == "mismatch"):
        state = "E2_CHECKSUM_MISMATCH"
    elif (data["history_table"] == "present"
          and data["v1_checksum_relation"] == "match"
          and data["v1_source_relation"] == "match"
          and data["highest_success_version"] is not None):
        state = "E1_SUCCESS_HISTORY_REPORTED"
    else:
        state = "UNKNOWN"
    return {
        "classification": state,
        "evidence_assurance": evidence,
        "engine_family": data["engine"],
        "migration_chain": data["migration_chain"],
        "checksums_and_actual_schema_independently_verified": False,
        "production_migration_authorized": False,
        "flyway_repair_authorized": False,
        "merge_authorized": False,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, type=Path,
                        help="Owner-reviewed non-PHI JSON metadata (max 4 KiB)")
    args = parser.parse_args()
    size = args.input.stat().st_size
    if size > MAX_BYTES:
        raise SystemExit("Rejected oversized evidence input")
    try:
        data = validate(json.loads(args.input.read_text(encoding="utf-8")))
        result = classify(data)
    except (ValueError, UnicodeError, json.JSONDecodeError) as exc:
        # Avoid source fields or values in error messages.
        raise SystemExit("Rejected invalid or non-allowlisted evidence input") from None
    print(json.dumps(result, sort_keys=True))


if __name__ == "__main__":
    main()
