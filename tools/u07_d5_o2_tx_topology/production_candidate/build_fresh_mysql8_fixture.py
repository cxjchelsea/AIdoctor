#!/usr/bin/env python3
"""Generate an OFFLINE, disposable-only MySQL8 fresh-install Flyway package.

Never edits V1-V6, never connects to a database, never executes Flyway,
and cannot be used against a previously baselined or deployed database.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re

PREFIX = "U07_D5_O2_FRESH_INSTALL_SYNTHETIC_ONLY"


def create_package(repo: Path, output: Path) -> dict:
    if os.environ.get("GITHUB_ACTIONS") != "true" or os.environ.get("U07_F_ISOLATED") != "true":
        raise RuntimeError("SYNTHETIC_GITHUB_ACTIONS_ONLY")
    if output.exists():
        raise RuntimeError("OUTPUT_MUST_NOT_EXIST")
    root = repo / "diagnosis-service/src/main/resources/db/migration"
    staged = repo / "tools/u07_d5_o2_tx_topology/production_candidate/mysql8"
    entries = []
    sources = []
    for v in range(1, 7):
        matches = list(root.glob(f"V{v}__*.sql"))
        if len(matches) != 1:
            raise RuntimeError(f"EXACT_HISTORICAL_SOURCE_REQUIRED_{v}")
        sources.append(matches[0])
    sources.extend([staged / "V7__u07_recovery_claim.sql",
                    staged / "V8__u07_outbox_no_dispatch.sql"])
    if any(not p.is_file() for p in sources):
        raise RuntimeError("CANDIDATE_FILES_MISSING")

    # Resolve the exact transformation before creating any output.
    raw_v1 = sources[0].read_text(encoding="utf-8")
    count = len(re.findall(r"\bCLOB\b", raw_v1))
    if count != 9:
        raise RuntimeError(f"UNEXPECTED_V1_CLOB_COUNT_{count}")
    transformed = re.sub(r"\bCLOB\b", "LONGTEXT", raw_v1)
    if transformed == raw_v1 or "CLOB" in transformed:
        raise RuntimeError("TRANSFORMATION_NOT_STRICT")

    output.mkdir(parents=False)
    for i, source in enumerate(sources):
        original = source.read_bytes()
        produced = transformed.encode("utf-8") if i == 0 else original
        (output / source.name).write_bytes(produced)
        entries.append({
            "name": source.name,
            "original_sha256": hashlib.sha256(original).hexdigest(),
            "generated_sha256": hashlib.sha256(produced).hexdigest(),
            "changed": i == 0,
        })
    manifest = {
        "mode": PREFIX,
        "deployable": False,
        "existing_database_allowed": False,
        "original_v1_modified_in_repo": False,
        "v1_clob_replacements": count,
        "migrations": entries,
    }
    (output / "U07_SYNTHETIC_PACKAGE_MANIFEST.json").write_text(
        json.dumps(manifest, sort_keys=True, indent=2) + "\n", encoding="utf-8"
    )
    return manifest


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--repo", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--ack", required=True)
    args = p.parse_args()
    if args.ack != PREFIX:
        raise SystemExit("EXPLICIT_SYNTHETIC_ACK_REQUIRED")
    result = create_package(args.repo.resolve(), args.output.resolve())
    print("U07_FRESH_INSTALL_SYNTHETIC_PACKAGE_CREATED=8_MIGRATIONS")
    print("U07_FRESH_INSTALL_CLOB_TRANSFORM_COUNT=" + str(result["v1_clob_replacements"]))


if __name__ == "__main__":
    main()
