#!/usr/bin/env python3
"""Offline-only verified-source projection from the original Foundation scan ZIP.

Only local evidence archive + exact Java snapshots supplied by caller are consumed.
No git commands, Spring process, imports, sockets, database drivers or network APIs.
"""
from __future__ import annotations

import argparse
import json
import re
import zipfile
from pathlib import Path

from static_schema import EvidenceError, canonical_json, digest, make_result, verify_inventory, verify_source_snapshot

ROOT = "diagnosis-service/src/main/java/com/aidoctor/diagnosis/"
FILES = {
    "U01ConsultationService": "runtime/u01/U01ConsultationService.java",
    "CanonicalBusinessEventLedger": "runtime/foundation/CanonicalBusinessEventLedger.java",
    "CDPManager": "service/cdp/CDPManager.java",
    "CDPVersionService": "service/cdp/CDPVersionService.java",
    "RuntimeBindingService": "runtime/foundation/RuntimeBindingService.java",
    "ClinicalRunCoordinator": "runtime/foundation/ClinicalRunCoordinator.java",
}
EDGES = (
    ("U01ConsultationService", "CanonicalBusinessEventLedger", r"\beventLedger\s*\.\s*resolveOrCreate\s*\("),
    ("U01ConsultationService", "CDPManager", r"\bcdpManager\s*\.\s*createCDP\s*\("),
    ("CDPManager", "CDPVersionService", r"\bcdpVersionService\s*\.\s*createInitialVersion\s*\("),
    ("U01ConsultationService", "RuntimeBindingService", r"\bruntimeBindingService\s*\.\s*bind\s*\("),
    ("U01ConsultationService", "ClinicalRunCoordinator", r"\brunCoordinator\s*\.\s*openRun\s*\("),
)
METHODS = {"U01ConsultationService": "start", "CanonicalBusinessEventLedger": "resolveOrCreate",
           "CDPManager": "createCDP", "CDPVersionService": "createInitialVersion", "RuntimeBindingService": "bind", "ClinicalRunCoordinator": "openRun"}


def _member_tx(text: str, method: str) -> bool:
    # Declared annotation only; never actual Spring interception evidence.
    rx = r"@Transactional(?:\([^)]*\))?\s+public\s+[\w<>?,\[\]. ]+\s+" + re.escape(method) + r"\s*\("
    return re.search(rx, text, re.MULTILINE) is not None


def project(archive: bytes, snapshots: dict[str, bytes]) -> dict:
    if not isinstance(archive, bytes):
        raise EvidenceError("archive bytes required")
    with zipfile.ZipFile(__import__("io").BytesIO(archive), "r") as z:
        names = z.namelist()
        if sorted(names) != sorted(["foundation-exact-head-inventory.json", "foundation-matches.tsv", "foundation-summary.md"]):
            raise EvidenceError("unexpected evidence archive files")
        if len(set(names)) != len(names) or any(z.getinfo(x).file_size > 15000000 for x in names):
            raise EvidenceError("unsafe evidence archive")
        inventory_raw = z.read("foundation-exact-head-inventory.json")
    inventory = json.loads(inventory_raw)
    by_path = verify_inventory(inventory)
    if set(snapshots) != {ROOT + name for name in FILES.values()}:
        raise EvidenceError("missing/extra source snapshots")
    source_map = {}
    sources = []
    for node, relative in sorted(FILES.items()):
        path = ROOT + relative
        if path not in by_path:
            raise EvidenceError("missing path in exact inventory")
        raw = snapshots[path]
        proof = verify_source_snapshot(path, raw, by_path[path])
        actual_blob = next((m["file_git_blob"] for m in inventory["matches"] if m["path"] == path), None)
        if actual_blob is not None and actual_blob != proof["git_blob"]:
            raise EvidenceError("source Git blob mismatch")
        sources.append(proof)
        source_map[node] = raw.decode("utf-8", errors="strict")
    nodes = []
    for node in sorted(FILES):
        nodes.append({"name": node, "status": "SOURCE_ONLY", "declared_transactional": _member_tx(source_map[node], METHODS[node]),
                      "method": METHODS[node], "source_path": ROOT + FILES[node]})
    nodes += [{"name": "U07AdmissionApplicationService", "status": "NOT_IMPLEMENTED"},
              {"name": "U07CanonicalEventBindingRepository", "status": "NOT_IMPLEMENTED"}]
    edges = []
    for caller, callee, pattern in EDGES:
        found = [(i, m.group(0)) for i, line in enumerate(source_map[caller].splitlines(), start=1)
                 for m in re.finditer(pattern, line)]
        if len(found) != 1:
            raise EvidenceError("missing or duplicate direct edge: " + caller + " -> " + callee)
        edges.append({"from": caller, "to": callee, "status": "SOURCE_ONLY", "authority": "DECLARED_SOURCE_EDGE",
                      "source_path": ROOT + FILES[caller], "line": found[0][0]})
    u01 = source_map["U01ConsultationService"]
    occurrences = [i for i, line in enumerate(u01.splitlines(), start=1) if re.search(r"\bconsultationRepository\s*\.\s*save\s*\(", line)]
    if len(occurrences) != 1:
        raise EvidenceError("missing/duplicate ConsultationRepository persistence call")
    nodes.append({"name": "ConsultationRepository", "status": "SOURCE_ONLY", "method": "save"})
    edges.append({"from": "U01ConsultationService", "to": "ConsultationRepository", "status": "SOURCE_ONLY",
                  "authority": "DECLARED_SOURCE_EDGE", "source_path": ROOT + FILES["U01ConsultationService"], "line": occurrences[0]})
    return make_result(archive_sha256=digest(archive), inventory_sha256=digest(inventory_raw), nodes=nodes, edges=edges, sources=sources)


def main() -> None:
    parser = argparse.ArgumentParser(description="Offline, source-only Foundation topology projection")
    parser.add_argument("--archive", required=True, help="verified original Foundation scan ZIP")
    parser.add_argument("--snapshots-dir", required=True, help="root with six exact-head Java source snapshots")
    parser.add_argument("--output", required=True, help="new output JSON outside source tree")
    args = parser.parse_args()
    src_dir = Path(args.snapshots_dir).resolve(strict=True)
    output = Path(args.output).resolve()
    if output.exists() or src_dir == output or src_dir in output.parents or Path(__file__).resolve().parent in output.parents:
        raise SystemExit("FAIL_CLOSED: unsafe output location")
    try:
        snapshots = {ROOT + p: (src_dir / ROOT / p).read_bytes() for p in FILES.values()}
        result = project(Path(args.archive).read_bytes(), snapshots)
        output.parent.mkdir(parents=True, exist_ok=True)
        with output.open("xb") as f:
            f.write(canonical_json(result))
        print("SOURCE_ONLY projection_sha256=" + digest(output.read_bytes()))
    except (EvidenceError, OSError, KeyError, ValueError, zipfile.BadZipFile) as exc:
        raise SystemExit("FAIL_CLOSED: " + str(exc)) from None


if __name__ == "__main__":
    main()
