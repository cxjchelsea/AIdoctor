#!/usr/bin/env python3
"""Isolated-test-only Flyway migration overlay; never mutate release migrations.

Copy original MySQL V1..V7 to a disposable directory, fixing exactly nine
historical CLOB type declarations in V1 to MySQL LONGTEXT.
The overlay intentionally changes Flyway V1 checksum: NEVER deploy/use it
against any existing Flyway history/database.
"""
from __future__ import annotations

import argparse
import hashlib
import re
from pathlib import Path

EXPECTED_V1_GIT_BLOB = "dfd87844be26015131fffa57430d3d982b5a4ef6"
V1_NAME = "V1__create_agent_state_and_audit_trail.sql"
REQUIRED_VERSIONS = tuple(range(1, 8))
PATTERN = re.compile(r"(?m)^(\s*\w+\s+)CLOB(?=\s*(?:,|COMMENT\b))")
EXPECTED_NAMES = {
    "thresholds", "budget", "failure_backoff", "tried_tools",
    "evidence_fusion_state", "stop_conditions", "tool_call",
    "cdp_update", "agent_decision",
}


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()


def stage(source: Path, destination: Path) -> list[str]:
    if destination.exists() and any(destination.iterdir()):
        raise ValueError("staging destination must be empty")
    paths = sorted(source.glob("V[0-9]*__*.sql"))
    versions = tuple(sorted(int(p.name.split("__", 1)[0][1:]) for p in paths))
    if versions != REQUIRED_VERSIONS or len(paths) != 7:
        raise ValueError("expected exactly the MySQL V1..V7 migration set")
    if any(p.is_symlink() or not p.is_file() for p in paths):
        raise ValueError("migration inputs must be regular non-symlink files")
    v1 = next(p for p in paths if p.name == V1_NAME)
    before = v1.read_bytes()
    if blob_sha(before) != EXPECTED_V1_GIT_BLOB:
        raise ValueError("historical V1 Git Blob changed; refuse transformation")
    original = before.decode("utf-8")
    found = {m.group(1).strip().lower() for m in PATTERN.finditer(original)}
    if found != EXPECTED_NAMES or len(list(PATTERN.finditer(original))) != 9:
        raise ValueError("unexpected historical V1 CLOB definitions")
    transformed, count = PATTERN.subn(lambda m: m.group(1) + "LONGTEXT", original)
    if count != 9 or "CLOB" in transformed:
        raise ValueError("unexpected transformation scope")
    destination.mkdir(parents=True, exist_ok=True)
    for p in paths:
        dest = destination / p.name
        if p.name == V1_NAME:
            dest.write_bytes(transformed.encode("utf-8"))
        else:
            dest.write_bytes(p.read_bytes())
            if dest.read_bytes() != p.read_bytes():
                raise ValueError("copy mismatch " + p.name)
    return [p.name for p in paths]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--destination", type=Path, required=True)
    args = parser.parse_args()
    names = stage(args.source.resolve(strict=True), args.destination)
    print("U07_D1_MYSQL_TEST_ONLY_COMPAT=PASS")
    print("V1: nine verified CLOB declarations -> LONGTEXT; Flyway checksum changes")
    print("V2..V7: byte-identical source copies")
    print("STAGED=" + ",".join(names))


if __name__ == "__main__":
    main()
