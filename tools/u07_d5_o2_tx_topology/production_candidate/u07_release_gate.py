#!/usr/bin/env python3
"""U07 release evidence gate: read-only, fail-closed, no network, no clinical activation.

A manifest and local evidence digests prove only integrity of submitted files,
not issuer identity or independent approval. Even complete intake cannot
authorize production activation through this tool.
"""
import argparse
import hashlib
import json
from pathlib import Path

REQUIRED = {
    "deployed_platform": "Authenticated database platform, version and environment inventory",
    "flyway_history": "Read-only deployed flyway_schema_history and V1-V6 checksum comparison",
    "historical_schema": "DBA-approved physical historical schema compatibility review",
    "migration_authorization": "Signed V7/V8/V9 migration sequencing and scoped DBA authorization",
    "rollback_rehearsal": "Representative environment rollback and recovery test evidence",
    "durable_wait": "Real persisted consultation/thread/checkpoint crash and replay evidence",
    "delivery_authenticity": "Independent actual recipient/delivery authenticity proof",
    "clinical_authority": "Signed owner, barrier, revalidation, route binding and revocation proof",
    "deployment_security": "Deployment credentials, least-privilege grants and audit verification",
    "clinical_safety_signoff": "Independent clinical safety review and formal approval",
}
VALID_PLATFORMS = ("mysql8", "oracle", "dual")
EXPECTED_MAIN = "b927b1c8a7019884129013be85b37afd404f18cd"


def gate(manifest, root):
    """Never returns RELEASE_AUTHORIZED; exact source and issuer checks are external."""
    failures = []
    if not isinstance(manifest, dict):
        return {"decision": "BLOCK", "failures": ["INVALID_MANIFEST"], "evidence": {}}
    baseline = manifest.get("source_main_sha")
    if not isinstance(baseline, str) or len(baseline) != 40 or any(
            c not in "0123456789abcdef" for c in baseline):
        failures.append("SOURCE_MAIN_SHA_INVALID")
    if baseline != EXPECTED_MAIN:
        failures.append("SOURCE_BASELINE_NOT_REVIEWED")
    platform = manifest.get("platform")
    if platform not in VALID_PLATFORMS:
        failures.append("PLATFORM_NOT_RESOLVED")
    supplied = manifest.get("evidence")
    if not isinstance(supplied, dict):
        supplied = {}
        failures.append("EVIDENCE_MAP_MISSING")
    report = {}
    for key, description in REQUIRED.items():
        record = supplied.get(key)
        item = {"purpose": description, "intake": "MISSING"}
        if not isinstance(record, dict):
            failures.append("MISSING_" + key.upper())
        else:
            path = record.get("path")
            digest = record.get("sha256")
            if not isinstance(path, str) or not isinstance(digest, str):
                failures.append("INVALID_" + key.upper())
            else:
                relative = Path(path)
                # No symlink escapes, traversal, absolute paths, or secret fetching.
                if relative.is_absolute() or ".." in relative.parts or path.startswith("."):
                    failures.append("UNSAFE_PATH_" + key.upper())
                else:
                    target = (root / relative).resolve()
                    try:
                        target.relative_to(root.resolve())
                        if not target.is_file() or target.is_symlink():
                            raise ValueError("not regular")
                        if len(digest) != 64 or any(c not in "0123456789abcdef" for c in digest):
                            raise ValueError("digest invalid")
                        actual = hashlib.sha256(target.read_bytes()).hexdigest()
                        if actual != digest:
                            raise ValueError("digest mismatch")
                    except (ValueError, OSError):
                        failures.append("UNVERIFIED_" + key.upper())
                    else:
                        item = {"purpose": description, "intake": "HASH_MATCH_ONLY"}
        report[key] = item
    if platform in ("oracle", "dual"):
        # Oracle SQL structural parity is not Oracle physical execution evidence.
        record = supplied.get("oracle_physical")
        if not isinstance(record, dict):
            failures.append("MISSING_ORACLE_PHYSICAL")
    return {
        "decision": "BLOCK",
        "scope": "OFFLINE_EVIDENCE_INTAKE_NOT_RELEASE_AUTHORIZATION",
        "source_main_sha": baseline,
        "evidence": report,
        "failures": sorted(set(failures)),
        "release_authority": "INDEPENDENT_APPROVAL_REQUIRED",
        "production_migration": "BLOCK",
        "production_clinical_activation": "BLOCK",
        "production_patient_delivery": "BLOCK",
        "note": "A SHA-256 match authenticates neither evidence issuer nor clinical permission.",
    }


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--manifest", type=Path)
    p.add_argument("--evidence-root", type=Path, default=Path("."))
    p.add_argument("--out", type=Path)
    args = p.parse_args()
    if args.manifest:
        try:
            document = json.loads(args.manifest.read_text(encoding="utf-8"))
        except (ValueError, OSError):
            document = {}
    else:
        document = {}
    report = gate(document, args.evidence_root.resolve())
    rendered = json.dumps(report, indent=2, sort_keys=True, ensure_ascii=False)
    if args.out:
        args.out.write_text(rendered + "\n", encoding="utf-8")
    print(rendered)
    # Missing intake is a failing gate; valid intake is still NOT authorization.
    return 2 if report["failures"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
