#!/usr/bin/env python3
"""Test-only current-main physical witness; never asserts production/retry acceptance."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import runpy
import subprocess
import sys
import xml.etree.ElementTree as ET

SOURCE = "8614b1ee94ab4640d412c9b29d9cd237a8930814"
STORE_BLOB = "6b67b75bb121410a0a78b6cbf188a5369ad5d1fb"
POM_BLOB = "dfa3c6f1cfc274ca796bf1bde8824ed1d9b80bb5"
STORE_CLASS = "com/aidoctor/diagnosis/runtime/u06/delivery/JdbcU06DeliveryStore.class"
FILES = ("forensic-incident.json", "role-negative.json")
SCHEMA = "U07_D5_O2_MAIN_L0_SOURCE_WITNESS_V1"
OLD_MARKER = "20a54b4a7478641d8b8d8c12c6213b8e2ea73acc"

def assert_(cond, code):
    if not cond:
        raise RuntimeError("MAIN_L0_" + code)

def cmd(*args, cwd):
    p = subprocess.run(args, cwd=str(cwd), capture_output=True, text=True, timeout=20)
    assert_(p.returncode == 0, "GIT_COMMAND_FAILED")
    return p.stdout.strip()

def paths():
    root = Path(os.environ["GITHUB_WORKSPACE"]).resolve()
    source = (root / "source").resolve()
    control = (root / "control").resolve()
    assert_(source != control and source.is_dir() and control.is_dir(), "SOURCE_CONTROL_COLLISION")
    assert_(not source.is_relative_to(control) and not control.is_relative_to(source), "TREE_OVERLAP")
    return source, control

def identities():
    source, control = paths()
    source_sha = cmd("git", "rev-parse", "HEAD", cwd=source)
    control_sha = cmd("git", "rev-parse", "HEAD", cwd=control)
    assert_(source_sha == SOURCE, "SOURCE_SHA_MISMATCH")
    assert_(re.fullmatch(r"[0-9a-f]{40}", control_sha) is not None, "CONTROL_SHA_FORMAT")
    assert_(control_sha == os.environ["MAIN_L0_CONTROL_SHA"], "CONTROL_SHA_MISMATCH")
    for root in (source, control):
        assert_(cmd("git", "status", "--porcelain", cwd=root) == "", "DIRTY_TRACKED_WORKTREE")
    for path, expected in (
        ("diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/delivery/JdbcU06DeliveryStore.java", STORE_BLOB),
        ("diagnosis-service/pom.xml", POM_BLOB),
    ):
        assert_(cmd("git", "rev-parse", "HEAD:" + path, cwd=source) == expected, "SOURCE_BLOB_DRIFT")
    assert_(os.environ.get("GITHUB_ACTIONS") == "true", "NOT_ACTIONS_RUNNER")
    for field in ("GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT"):
        assert_(os.environ.get(field, "").isdigit(), "MISSING_RUN_IDENTITY")
    return source, control, control_sha

def old_helper(source):
    return runpy.run_path(str(source / "tools/u07_d5_o2_tx_topology/stageb/forensics/bootstrap-and-verify.py"))

def preflight():
    source, _, control_sha = identities()
    legacy = old_helper(source)
    manifest, _ = legacy["validate_manifest"]()
    assert_(manifest["sourceMainSha"] == OLD_MARKER, "LEGACY_MANIFEST_DRIFT")
    assert_(manifest["l1Enabled"] is False and manifest["downstreamAuthority"] is False,
            "SIDE_EFFECT_BOUNDARY")
    print("MAIN_L0_CURRENT_SOURCE_REBIND=PASS; LEGACY_EMITTER_MARKER_DISTINCT=PASS")

def read_facts(source, legacy, control_sha):
    out = source / "diagnosis-service/target/stage-b-f-observations"
    assert_(out.is_dir(), "MISSING_FORENSIC_DIRECTORY")
    assert_(set(p.name for p in out.glob("*.json")) == set(FILES), "EXACT_EVIDENCE_FILE_SET")
    result = []
    for name in FILES:
        p = out / name
        assert_(p.is_file() and not p.is_symlink() and p.stat().st_nlink == 1,
                "UNSAFE_EVIDENCE_FILE")
        assert_(p.stat().st_size < 65536, "EVIDENCE_SIZE")
        obj = json.loads(p.read_text(encoding="utf-8"))
        legacy["ensure_safe"](obj)
        assert_(obj.get("sourceMainSha") == OLD_MARKER, "LEGACY_EMITTER_PROVENANCE_DRIFT")
        assert_(obj.get("checkoutSha") == SOURCE and obj.get("candidateHead") == control_sha,
                "EMITTER_CONTROL_SOURCE_BINDING")
        result.append(obj)
    return result

def admit():
    source, control, control_sha = identities()
    legacy = old_helper(source)
    manifest, _ = legacy["validate_manifest"]()
    incident, role = read_facts(source, legacy, control_sha)
    # Original validated physical L0 role constraints, four independent connections,
    # six-table readback, first-attempt 1213 rollback, edge-epoch consistency.
    legacy["validate_fact_bundle"](incident, role, manifest, control_sha, SOURCE,
        os.environ["GITHUB_RUN_ID"], os.environ["GITHUB_RUN_ATTEMPT"])
    legacy["adversarial_mutations"](incident, role, manifest, control_sha, SOURCE,
        os.environ["GITHUB_RUN_ID"], os.environ["GITHUB_RUN_ATTEMPT"])
    report_dir = source / "diagnosis-service/target/surefire-reports"
    reports = list(report_dir.glob("TEST-*StageBFDeadlockForensicsTest.xml"))
    assert_(len(reports) == 1, "SUREFIRE_REPORT_COUNT")
    suite = ET.parse(reports[0]).getroot()
    assert_(all(int(suite.get(k, "-1")) == 0 for k in ("failures","errors","skipped")),
            "SUREFIRE_FAILED_OR_SKIPPED")
    cases = suite.findall(".//testcase")
    assert_(len(cases) == 1 and cases[0].get("name") == "originalIndependentEffectDeadlockObservation",
            "SUREFIRE_WRONG_CASE")
    compiled = source / "diagnosis-service/target/classes" / STORE_CLASS
    assert_(compiled.is_file() and not compiled.is_symlink(), "STORE_CLASS_ABSENT")
    blob = compiled.read_bytes()
    assert_(blob[:4] == b"\xca\xfe\xba\xbe" and blob[6:8] == b"\x00\x34",
            "STORE_CLASS_NOT_JAVA8")
    class_sha = hashlib.sha256(blob).hexdigest()
    assert_(class_sha == os.environ["MAIN_L0_COMPILED_STORE_SHA256"], "COMPILED_STORE_SHA_CHANGED")
    assert_(incident["downstreamAuthority"] is False and
            incident["businessCompletion"] == "NOT_AUTHORIZED_TO_ATTEST" and
            incident["L1Verdict"] == "L1_NOT_ENABLED", "DOWNSTREAM_FALSE_PASS")
    assert_(incident["sourceVerdict"] in ("ROLLBACK_CONFIRMED_1213", "NO_1213_OBSERVED"),
            "UNSUPPORTED_SOURCE_VERDICT")
    assert_(incident["forensicVerdict"] in
            ("NO_EDGE_CAPTURED", "PARTIAL_EDGE_ONLY", "LIVE_TWO_WRITER_CYCLE_BOUND"),
            "UNSUPPORTED_LOCK_VERDICT")
    e = {
        "schema":SCHEMA,"sourceUnderTestSha":SOURCE,
        "runnerControlSha":control_sha,
        "legacyEmitterSourceMarker":OLD_MARKER,
        "storeSourceBlob":STORE_BLOB,
        "sourcePomBlob":POM_BLOB,
        "compiledStoreSha256":class_sha,
        "workflowRunId":os.environ["GITHUB_RUN_ID"],
        "workflowRunAttempt":os.environ["GITHUB_RUN_ATTEMPT"],
        "actualCaseCount":1,
        "roleNegativePassed":True,
        "sourceFirstAttemptVerdict":incident["sourceVerdict"],
        "lockGraphVerdict":incident["forensicVerdict"],
        "eventualRetryVerdict":"NOT_EXECUTED",
        "L1Verdict":"NOT_ENABLED",
        "productionAuthority":False,
    }
    output = Path(os.environ["RUNNER_TEMP"]) / "u07-main-l0-admitted.json"
    assert_(not output.exists(), "STALE_SUMMARY")
    output.write_text(json.dumps(e,sort_keys=True,separators=(",",":"))+"\n",encoding="utf8")
    print("MAIN_L0_REAL_MYSQL8_EVIDENCE_VALIDATED=PASS")
    print("MAIN_L0_FIRST_ATTEMPT_VERDICT=" + e["sourceFirstAttemptVerdict"])
    print("MAIN_L0_CAUSAL_GRAPH=" + e["lockGraphVerdict"])

def safe_upload():
    source, _, control_sha = identities()
    output = Path(os.environ["RUNNER_TEMP"]) / "u07-main-l0-admitted.json"
    assert_(output.is_file() and not output.is_symlink() and output.stat().st_nlink == 1
            and output.stat().st_size < 4096, "UNSAFE_SUMMARY")
    raw = output.read_text(encoding="utf8")
    obj = json.loads(raw)
    expected = {"schema","sourceUnderTestSha","runnerControlSha",
       "legacyEmitterSourceMarker","storeSourceBlob","sourcePomBlob",
       "compiledStoreSha256","workflowRunId","workflowRunAttempt","actualCaseCount",
       "roleNegativePassed","sourceFirstAttemptVerdict","lockGraphVerdict",
       "eventualRetryVerdict","L1Verdict","productionAuthority"}
    assert_(set(obj) == expected, "UNEXPECTED_SUMMARY_FIELDS")
    assert_(obj["schema"] == SCHEMA and obj["sourceUnderTestSha"] == SOURCE
       and obj["runnerControlSha"] == control_sha
       and obj["legacyEmitterSourceMarker"] == OLD_MARKER
       and obj["storeSourceBlob"] == STORE_BLOB and obj["sourcePomBlob"] == POM_BLOB
       and obj["workflowRunId"] == os.environ["GITHUB_RUN_ID"]
       and obj["workflowRunAttempt"] == os.environ["GITHUB_RUN_ATTEMPT"]
       and obj["actualCaseCount"] == 1 and obj["roleNegativePassed"] is True
       and obj["eventualRetryVerdict"] == "NOT_EXECUTED"
       and obj["L1Verdict"] == "NOT_ENABLED" and obj["productionAuthority"] is False,
       "SUMMARY_AUTHORITY_MISMATCH")
    assert_(obj["sourceFirstAttemptVerdict"] in
       ("ROLLBACK_CONFIRMED_1213","NO_1213_OBSERVED"), "SOURCE_VERDICT")
    assert_(obj["lockGraphVerdict"] in
       ("NO_EDGE_CAPTURED","PARTIAL_EDGE_ONLY","LIVE_TWO_WRITER_CYCLE_BOUND"), "EDGE_VERDICT")
    assert_(type(obj["compiledStoreSha256"]) is str and
       re.fullmatch("[0-9a-f]{64}",obj["compiledStoreSha256"]) is not None,
       "COMPILED_CLASS_SHA")
    assert_(raw == json.dumps(obj,sort_keys=True,separators=(",",":"))+"\n","SUMMARY_NONCANONICAL")
    print("MAIN_L0_FAIL_CLOSED_SAFE_UPLOAD=PASS")

def adversarial():
    # Negative-path gate: artifact cannot exist unless the physical witness
    # was executed and all independent admission checks completed.
    try:
        safe_upload()
    except (RuntimeError, FileNotFoundError, ValueError):
        print("MAIN_L0_MISSING_EVIDENCE_REJECTED=PASS")
        return
    raise RuntimeError("MAIN_L0_NEGATIVE_UPLOAD_FALSE_ACCEPT")

if __name__ == "__main__":
    try:
        mode = argparse.ArgumentParser()
        mode.add_argument("phase", choices=("preflight","admit","safe-upload","negative"))
        args = mode.parse_args()
        {"preflight":preflight,"admit":admit,"safe-upload":safe_upload,
         "negative":adversarial}[args.phase]()
    except Exception as exc:
        # Never forward raw MySQL output, SQL, passwords, or exception payload.
        print("MAIN_L0_FAILURE=" + type(exc).__name__,file=sys.stderr)
        sys.exit(1)
