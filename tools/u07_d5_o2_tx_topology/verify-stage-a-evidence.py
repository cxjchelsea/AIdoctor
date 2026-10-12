#!/usr/bin/env python3
"""Machine-checkable, nonclinical, SHA-bound evidence from real Surefire XML."""
import glob
import hashlib
import json
import os
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "diagnosis-service/src/test/resources/u07/d5o2/stage-a-manifest.json"
EXPECTED = {
    "U07D5O2StageATopologyTest",
    "U07D5O2StageATxPhysicalTest",
    "U07D5O2StageAMultiOwnerTest",
    "U07D5O2StageANegativeTest",
}


def main():
    data = json.loads(MANIFEST.read_text())
    if (data["authority"] != "UNKNOWN"
            or data["production_topology_verified"] is not False
            or data["schema_mode"] != "OWNER_SCHEMA_SLICE"):
        raise SystemExit("STAGE_A_AUTHORITY_PROVENANCE_DENIED")
    actual_head = subprocess.check_output(
        ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    subprocess.run(["git", "merge-base", "--is-ancestor",
        data["source_main_sha"], actual_head], cwd=ROOT, check=True)
    if len(actual_head) != 40:
        raise SystemExit("STAGE_A_HEAD_INVALID")
    observed = {}
    for p in glob.glob(str(ROOT / "diagnosis-service/target/surefire-reports/TEST-*.xml")):
        root = ET.parse(p).getroot()
        name = root.attrib.get("name", "").rsplit(".", 1)[-1]
        if name not in EXPECTED:
            continue
        if name in observed:
            raise SystemExit("STAGE_A_DUPLICATE_REPORT")
        counts = {k: int(root.attrib.get(k, "0"))
                  for k in ("tests", "errors", "failures", "skipped")}
        if counts["tests"] < 1 or any(counts[k] for k in ("errors", "failures", "skipped")):
            raise SystemExit("STAGE_A_FAILED_OR_SKIPPED_" + name)
        observed[name] = counts["tests"]
    if set(observed) != EXPECTED:
        raise SystemExit("STAGE_A_MISSING_EXPECTED_TEST_REPORTS")
    original = []
    for item in data["ddl"]:
        raw = (ROOT / item["path"]).read_bytes()
        git_blob = hashlib.sha1(b"blob " + str(len(raw)).encode()
                                + b"\0" + raw).hexdigest()
        if git_blob != item["git_blob"]:
            raise SystemExit("STAGE_A_DDL_HASH_MISMATCH")
        original.append({"gitBlob": git_blob, "sha256": hashlib.sha256(raw).hexdigest()})
    result = {
        "schema": "U07_D5O2_STAGE_A_EVIDENCE_V1",
        "sourceMainSha": data["source_main_sha"],
        "testedCheckoutSha": actual_head,
        "workflowRunId": os.environ.get("GITHUB_RUN_ID", ""),
        "evidencePlane": "PHYSICAL_MYSQL",
        "wiringProvenance": "TEST_ONLY_CONFIG",
        "schemaMode": "OWNER_SCHEMA_SLICE",
        "backend": "MYSQL8_DISPOSABLE",
        "ddl": original,
        "testClasses": observed,
        "tests": sum(observed.values()),
        "authority": "UNKNOWN",
        "productionTopologyVerified": False,
        "fullFlywayVerified": False,
        "oracleVerified": False,
    }
    if not result["workflowRunId"].isdigit() or not os.environ.get("GITHUB_ACTIONS") == "true":
        raise SystemExit("STAGE_A_UNTRUSTED_EXECUTION_ENVIRONMENT")
    output = ROOT / "diagnosis-service/target/u07-d5o2-stage-a-evidence.json"
    output.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n")
    print("STAGE_A_SYNTHETIC_MYSQL_TESTS_EXECUTED=" + str(result["tests"]))


if __name__ == "__main__":
    main()
