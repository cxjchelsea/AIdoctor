#!/usr/bin/env python3
"""Read-only release-baseline preflight. No database connectivity, migration, or repair.

Machine-readable observations are intentionally NOT release authorization.
"""
import argparse
import json
from pathlib import Path
import re

MIGRATION = "diagnosis-service/src/main/resources/db/migration"
ORACLE = "diagnosis-service/src/main/resources/db/migration-oracle"
CANDIDATE = "tools/u07_d5_o2_tx_topology/production_candidate"

def inspect(repo: Path) -> dict:
    paths = {}
    for name, location in (("mysql", MIGRATION), ("oracle", ORACLE)):
        root = repo / location
        files = sorted(root.glob("V*.sql"))
        versions = [int(p.name.split("__", 1)[0][1:]) for p in files]
        paths[name] = {
            "scanned_location": location,
            "files": [p.name for p in files],
            "versions": versions,
            "historical_v1_v6_complete": versions == [1, 2, 3, 4, 5, 6],
            "v7_v8_auto_scanned": any(v >= 7 for v in versions),
        }
    source_v1 = (repo / MIGRATION / "V1__create_agent_state_and_audit_trail.sql").read_text(encoding="utf-8")
    clob = len(re.findall(r"\bCLOB\b", source_v1))
    expected_candidates = [
        repo / CANDIDATE / vendor / ("V" + str(v) + "__" + suffix + ".sql")
        for vendor in ("mysql8", "oracle")
        for v, suffix in ((7, "u07_recovery_claim"), (8, "u07_outbox_no_dispatch"))
    ]
    config = (repo / "diagnosis-service/src/main/resources/application.yml").read_text(encoding="utf-8")
    active = re.search(r"(?m)^\s*active:\s*(\S+)", config)
    observations = {
        "main_app_default_profile": active.group(1) if active else "UNRESOLVED",
        "flyway": paths,
        "original_mysql_v1_clob_tokens": clob,
        "mysql8_legacy_v1_compatibility_blocker": clob > 0,
        "candidate_v7_v8_files_present": all(p.is_file() for p in expected_candidates),
        "candidate_v7_v8_outside_default_flyway": all(
            not path["v7_v8_auto_scanned"] for path in paths.values()
        ),
        "database_history_evidence_supplied": False,
        "actual_deployed_platform_verified": False,
        "physical_mysql_oracle_transaction_verified_by_this_script": False,
    }
    clean = (all(path["historical_v1_v6_complete"] for path in paths.values())
             and all(not path["v7_v8_auto_scanned"] for path in paths.values())
             and observations["candidate_v7_v8_files_present"]
             and clob == 9)
    return {
        "scope": "OFFLINE_READ_ONLY_NONPRODUCTION_BASELINE_INVENTORY",
        "structural_invariants": "PASS" if clean else "FAIL",
        "production_migration_decision": "BLOCK",
        "production_clinical_state_activation": "BLOCK",
        "observations": observations,
        "required_external_evidence": [
            "authenticated_platform_and_flyway_schema_history",
            "DBA-approved V1-V6 checksums and schema shape",
            "isolated physical CDP StateCommitter persistence/rollback rehearsal",
            "approved V7/V8 deployment plan and recovery authorization",
        ],
    }

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, default=Path("."))
    args = parser.parse_args()
    result = inspect(args.repo.resolve())
    print(json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True))
    if result["structural_invariants"] != "PASS":
        raise SystemExit(1)

if __name__ == "__main__":
    main()
