#!/usr/bin/env python3
"""Read-only exact-head Git tracked-source inventory for U07 Foundation audit.

No network, application execution, database access, or clinical data.
Do not interpret lexical reference matches as runtime transaction proof.
"""
import argparse
import csv
import hashlib
import json
import re
import subprocess
from collections import Counter, defaultdict
from pathlib import Path

PATTERNS = {
    "ledger_class": r"\bCanonicalBusinessEventLedger\b",
    "ledger_repo": r"\bCanonicalBusinessEventRepository\b",
    "ledger_record": r"\bCanonicalBusinessEventRecord\b",
    "ledger_call": r"\bresolveOrCreate\s*\(",
    "ledger_table": r"\bcanonical_business_event\b",
    "foundation_unique": r"\buk_canonical_event_idempotency\b",
    "payload_digest": r"\bpayload_digest\b",
    "idempotency_key": r"\bidempotency_key\b",
    "runtime_binding": r"\bRuntimeBindingService\b",
    "transaction_annotation": r"@Transactional\b",
    "transaction_template": r"\bTransactionTemplate\b",
    "transaction_manager": r"\bPlatformTransactionManager\b",
    "data_source": r"\bDataSource\b",
    "fresh_tx": r"\bREQUIRES_NEW\b",
    "direct_jpa": r"\bEntityManager\b|\bJpaRepository\b",
    "direct_sql": r"\bJdbcTemplate\b|\bnativeQuery\b|\bcreateNativeQuery\b",
    "after_commit": r"\bafterCommit\b|\bAFTER_COMMIT\b",
}
COMPILED = {k: re.compile(v) for k, v in PATTERNS.items()}
FIRST_ORDER = {"ledger_class", "ledger_repo", "ledger_record", "ledger_call", "ledger_table", "foundation_unique"}
TRANSACTION = {"transaction_annotation", "transaction_template", "transaction_manager", "data_source", "fresh_tx", "direct_jpa", "direct_sql", "after_commit"}

def run(root, *args):
    p = subprocess.run(["git", "-C", str(root), *args], capture_output=True, check=True)
    return p.stdout

def sha256_bytes(b):
    return hashlib.sha256(b).hexdigest()

def classify(path, labels):
    p = path.lower()
    if p.startswith("docs/"):
        return "DESIGN_DOC"
    if ".github/" in p or p.startswith(".github/"):
        return "CI"
    if p.endswith(".sql"):
        return "SCHEMA"
    if "/src/test/" in p or "/tests/" in p or p.startswith("tests/"):
        return "TEST"
    if p.endswith((".yml", ".yaml", ".properties", ".xml", ".toml", ".json")):
        return "CONFIG_OR_FIXTURE"
    if ("ledger_class" in labels or "ledger_repo" in labels or "ledger_call" in labels):
        return "FOUNDATION_CONSUMER_CANDIDATE"
    if "ledger_table" in labels or "direct_sql" in labels:
        return "SQL_OR_DIRECT_STORAGE_CANDIDATE"
    if "transaction_annotation" in labels or "transaction_manager" in labels:
        return "TRANSACTION_BOUNDARY_CANDIDATE"
    return "INDIRECT_OR_CONTEXT"

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--source-dir", required=True)
    ap.add_argument("--expected-head", required=True)
    ap.add_argument("--output-dir", required=True)
    args = ap.parse_args()
    root = Path(args.source_dir).resolve()
    dest = Path(args.output_dir).resolve()
    dest.mkdir(parents=True, exist_ok=True)
    head = run(root, "rev-parse", "HEAD").decode().strip()
    if head != args.expected_head:
        raise SystemExit("FAIL_CLOSED: HEAD mismatch: " + head)
    if run(root, "status", "--porcelain", "--untracked-files=no").strip():
        raise SystemExit("FAIL_CLOSED: tracked source tree not clean")
    names_raw = run(root, "ls-files", "-z")
    names = sorted(x.decode("utf-8", errors="surrogateescape") for x in names_raw.split(b"\0") if x)
    if not names or len(names) != len(set(names)):
        raise SystemExit("FAIL_CLOSED: empty or duplicated Git tracked file inventory")

    matches, per_file, failures = [], [], []
    totals = Counter()
    for name in names:
        p = root / name
        if not p.is_file():
            failures.append({"path": name, "reason": "tracked path is not readable regular file"})
            continue
        try:
            raw = p.read_bytes()
            digest = sha256_bytes(raw)
            is_binary = b"\0" in raw[:8192]
            if is_binary:
                per_file.append({"path": name, "sha256": digest, "bytes": len(raw), "mode": "BINARY_READ_NOT_TEXT", "hit_count": 0})
                totals["binary"] += 1
                continue
            text = raw.decode("utf-8", errors="replace")
            hits_this = []
            for num, line in enumerate(text.splitlines(), start=1):
                labels = [label for label, rx in COMPILED.items() if rx.search(line)]
                if not labels:
                    continue
                role = classify(name, labels)
                for label in labels:
                    entry = {"path": name, "line": num, "term": label, "consumer_role_candidate": role, "file_sha256": digest, "file_git_blob": run(root, "hash-object", "--", name).decode().strip()}
                    matches.append(entry)
                    totals[label] += 1
                hits_this.extend(labels)
            per_file.append({"path": name, "sha256": digest, "bytes": len(raw), "mode": "UTF8_TEXT_SCAN", "hit_count": len(hits_this), "consumer_role_candidate": classify(name, hits_this)})
            totals["text_scanned"] += 1
        except (OSError, UnicodeError, subprocess.CalledProcessError) as exc:
            failures.append({"path": name, "reason": type(exc).__name__})
    if failures:
        raise SystemExit("FAIL_CLOSED: unreadable tracked paths: " + json.dumps(failures[:20], ensure_ascii=False))

    matches.sort(key=lambda x: (x["path"], x["line"], x["term"]))
    payload = {
        "audit_name": "U07 Foundation Exhaustive Exact-Head Tracked-Source Lexical Inventory",
        "source_head": head,
        "tree_sha": run(root, "rev-parse", "HEAD^{tree}").decode().strip(),
        "tracked_count": len(names),
        "text_scanned": totals["text_scanned"],
        "binary_read_and_classified": totals["binary"],
        "skipped_tracked_paths": 0,
        "filename_list_sha256": sha256_bytes(names_raw),
        "pattern_set": PATTERNS,
        "pattern_counts": {x: totals[x] for x in PATTERNS},
        "first_order_hit_files": sorted({m["path"] for m in matches if m["term"] in FIRST_ORDER}),
        "transaction_related_hit_files": sorted({m["path"] for m in matches if m["term"] in TRANSACTION}),
        "file_inventory": per_file,
        "matches": matches,
        "limitations": [
            "Lexical reference inventory, not all possible runtime reflection/call graph paths.",
            "No application/DB/MySQL/Oracle physical transactions executed.",
            "Classification is heuristic and each candidate requires independent owner review.",
            "No executable producer, migration or implementation authorization is granted.",
        ],
        "gate_status": "EVIDENCE_CANDIDATE_NOT_INDEPENDENTLY_REVIEWED",
    }
    json_path = dest / "foundation-exact-head-inventory.json"
    json_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    tsv_path = dest / "foundation-matches.tsv"
    with tsv_path.open("w", encoding="utf-8", newline="") as o:
        writer = csv.DictWriter(o, fieldnames=["path", "line", "term", "consumer_role_candidate", "file_sha256", "file_git_blob"], delimiter="\t")
        writer.writeheader()
        writer.writerows(matches)
    summary = [
        "# U07 Foundation Exact-HEAD Lexical Source Inventory",
        "",
        "Source: " + head,
        "Tree: " + payload["tree_sha"],
        "Tracked: " + str(len(names)),
        "Text scanned: " + str(totals["text_scanned"]),
        "Binary classified: " + str(totals["binary"]),
        "Skipped tracked paths: 0",
        "Matches: " + str(len(matches)),
        "First-order hit paths: " + str(len(payload["first_order_hit_files"])),
        "Transaction candidate paths: " + str(len(payload["transaction_related_hit_files"])),
        "",
        "## Critical limitation",
        "This is complete **tracked-file lexical scanning**, not complete runtime consumer semantic proof.",
        "Independent review of each hit, dynamic consumers, physical transaction managers and MySQL/Oracle remain REQUIRED.",
    ]
    (dest / "foundation-summary.md").write_text("\n".join(summary) + "\n", encoding="utf-8")
    for file in (json_path, tsv_path, dest / "foundation-summary.md"):
        print(file.name + " sha256=" + sha256_bytes(file.read_bytes()))
    print("AUDIT_SCAN_COMPLETE source=" + head + " tracked=" + str(len(names)) + " matches=" + str(len(matches)) + " gate=NOT_PASSED")

if __name__ == "__main__":
    main()
