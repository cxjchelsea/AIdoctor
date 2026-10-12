#!/usr/bin/env python3
"""Stage B P1B: produce disposable bootstrap SQL or validate actual JUnit/DB observations."""
import argparse
import hashlib
import json
import os
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
MANIFEST = ROOT / "diagnosis-service/src/test/resources/u07/d5o2/stageb/p1b/manifest.json"
TABLES = ("u06_delivery_authority", "u06_delivery_intent", "u06_delivery_attempt",
          "u06_delivery_receipt", "u06_delivery_confirmation", "u06_delivery_ledger")
BLOBS = ("73029495c5f46e8576e34f8c9a77644fb8c1c4c4",
         "ccc1aa30b8e9405bd07249bae1d3531853f7510b",
         "7ed5a82bfba3b3fed2666bee969a7fe276e67566")
DDL_NAMES = ("V2__create_clinical_runtime_foundation.sql",
             "V3__create_clinical_consultation.sql",
             "V6__add_u06_wait_runtime.sql")
HEX64 = re.compile(r"^[a-f0-9]{64}$")
ZERO = [0] * 6
ONE = [1] * 6

def fail(msg):
    raise SystemExit("P1B_FAIL_CLOSED_" + msg)

def blob_sha(raw):
    return hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()

def check_contract(d):
    if (d.get("schema") != "U07_D5O2_STAGE_B_P1B_MANIFEST_V1"
        or d.get("sourceMainSha") != "e7562bd9b5390c85e14c3c01424c1004b1479ebc"
        or d.get("profile") != "U07_D5O2_STAGE_B_P1B_SYNTHETIC"
        or d.get("database") != "u07_d5o2_stage_b_p1b" or d.get("port") != 33329
        or d.get("authority") != "TEST_ONLY" or d.get("downstreamAuthority") is not False
        or d.get("fullSixTableReplaySafe") is not False
        or d.get("originalStoreModified") is not False or len(d.get("cases", {})) != 15):
        fail("MANIFEST")
    paths = []
    for item, name, expected in zip(d.get("ddl", []), DDL_NAMES, BLOBS):
        path = "diagnosis-service/src/main/resources/db/migration/" + name
        if item != {"path": path, "gitBlob": expected}: fail("DDL_MANIFEST")
        raw = (ROOT / path).read_bytes()
        if blob_sha(raw) != expected: fail("DDL_TAMPER")
        paths.append(raw.decode("utf-8"))
    if len(paths) != 3: fail("DDL_COUNT")
    return paths

def secret(k):
    v = os.getenv(k, "")
    if not re.fullmatch("[0-9a-f]{48}", v): fail("MISSING_EPHEMERAL_" + k)
    return v

def bootstrap(d, ddl):
    if os.getenv("GITHUB_ACTIONS") != "true" or os.getenv("U07_D5O2_P1B_ISOLATED") != "true":
        fail("NOT_GITHUB_DISPOSABLE")
    passwords = {name: secret(env) for name, env in (
        ("stagebp1b_writer", "P1B_WRITER_PWD"), ("stagebp1b_reader", "P1B_READER_PWD"),
        ("stagebp1b_seeder", "P1B_SEEDER_PWD"))}
    chunks = ["SET default_storage_engine=InnoDB;"] + ddl
    for role, pw in passwords.items():
        chunks.append("CREATE USER '" + role + "'@'%' IDENTIFIED BY '" + pw + "';")
    for table in TABLES:
        target = "u07_d5o2_stage_b_p1b." + table
        chunks.extend((
            "GRANT SELECT, INSERT ON " + target + " TO 'stagebp1b_writer'@'%';",
            "GRANT SELECT ON " + target + " TO 'stagebp1b_reader'@'%';",
            "GRANT SELECT, INSERT, DELETE ON " + target + " TO 'stagebp1b_seeder'@'%';",
        ))
    print("\n".join(chunks))

def is_digest(value):
    return isinstance(value, str) and HEX64.fullmatch(value) is not None

def verify(d):
    if os.getenv("GITHUB_ACTIONS") != "true" or os.getenv("U07_D5O2_P1B_ISOLATED") != "true":
        fail("RUNNER")
    head = os.getenv("U07_P1B_PR_HEAD_SHA", "")
    runid = os.getenv("GITHUB_RUN_ID", "")
    if not re.fullmatch("[0-9a-f]{40}", head) or not runid.isdigit(): fail("RUN_ID")
    checkout = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    parent = subprocess.check_output(["git", "rev-parse", "HEAD^2"], cwd=ROOT, text=True).strip()
    if parent != head or checkout == head or os.getenv("GITHUB_SHA") != checkout: fail("HEAD_BIND")
    subprocess.run(["git", "merge-base", "--is-ancestor", d["sourceMainSha"], checkout],
                   cwd=ROOT, check=True)
    reports = ROOT / "diagnosis-service/target/surefire-reports"
    all_reports = list(reports.glob("TEST-*StageBP1B*.xml"))
    if len(all_reports) != 2: fail("XML_REPORT_COUNT")
    observed = {}
    for xml_file in all_reports:
        root = ET.parse(xml_file).getroot()
        cls = root.get("name", "").split(".")[-1]
        if cls not in {"StageBP1BPartialLedgerTest", "StageBP1BReplayConflictTest"}:
            fail("CLASS")
        if any(int(root.get(x, "0")) for x in ("failures", "errors", "skipped")):
            fail("SUITE_FAILURE")
        for case in root.findall(".//testcase"):
            name = case.get("name")
            if name in observed or name not in d["cases"]: fail("DUPLICATE_OR_EXTRA_METHOD")
            if case.get("classname", "").split(".")[-1] != cls or d["cases"][name]["class"] != cls:
                fail("METHOD_CLASS")
            if any(case.find(tag) is not None for tag in ("failure", "error", "skipped")):
                fail("CASE_FAIL")
            observed[name] = cls
    if set(observed) != set(d["cases"]): fail("METHOD_SET")
    folder = ROOT / "diagnosis-service/target/stage-b-p1b-observations"
    if not folder.exists() or len(list(folder.glob("*.json"))) != 15:
        fail("OBSERVATION_COUNT")
    entries = {}
    for method, config in d["cases"].items():
        path = folder / (method + ".json")
        if not path.is_file(): fail("OBSERVATION_MISSING")
        f = json.loads(path.read_text())
        if (f.get("testMethod") != method or f.get("caseId") != config["caseId"]
            or f.get("testClass") != config["class"] or f.get("runId") != runid
            or f.get("prHeadSha") != head or f.get("mergeCheckoutSha") != checkout
            or f.get("sourceMainSha") != d["sourceMainSha"]
            or f.get("profile") != d["profile"] or f.get("schema") != d["database"]
            or f.get("downstreamAuthority") is not False
            or f.get("sqlSignatureStrict") is not True
            or f.get("writerDeleteDenied") is not True
            or f.get("readerDeleteDenied") is not True
            or f.get("roleIdentityPhysicallyChecked") is not True
            or not isinstance(f.get("writerRole"), str)
            or not f["writerRole"].startswith("stagebp1b_writer@")
            or f.get("readerRole") != "stagebp1b_reader"
            or f.get("writerIsolation") != 8
            or not isinstance(f.get("writerConnectionId"), int)
            or f["writerConnectionId"] <= 0
            or f["writerConnectionId"] == f.get("readerConnectionId")
            or not is_digest(f.get("caseEffectDigest"))
            or not is_digest(f.get("graphBefore")) or not is_digest(f.get("graphAfter"))
            or f["graphBefore"] != f["graphAfter"]
            or not is_digest(f.get("protectedGraphBefore"))
            or f.get("protectedGraphBefore") != f.get("protectedGraphAfter")
            or not isinstance(f.get("readerConnectionId"), int)
            or f["readerConnectionId"] <= 0):
            fail("FACT_" + method)
        before, after = f.get("sixBefore"), f.get("sixAfter")
        writes = f.get("originalInsertCount")
        if not isinstance(writes, int) or writes < 0 or writes > 6:
            fail("WRITES_"+method)
        if f.get("sqlTrace") != list(TABLES[:writes]):
            fail("SQL_TRACE_"+method)
        if not isinstance(before, list) or len(before) != 6 or before != after:
            fail("SIX_VECTOR_" + method)
        if method.startswith("partial"):
            n = int(method[-2:])
            expected = [[0,0,0,0,0,1],[1,0,0,0,0,0],[0,1,0,0,0,0],
                        [0,0,0,0,1,0],[1,1,0,1,1,1],[1,1,1,0,1,1]][n-1]
            if (before != expected or f.get("cleanupVerified") is not True
                or f.get("fixtureCleanupAfter") != ZERO
                or not is_digest(f.get("fixtureCleanupGraphSha256"))
                or not is_digest(f.get("emptyGroupBeforeSha256"))
                or f["fixtureCleanupGraphSha256"] != f["emptyGroupBeforeSha256"]
                or f.get("cleanupEffectDigest") != f.get("caseEffectDigest")
                or f.get("seederRole") != "stagebp1b_seeder"
                or f.get("seederDdlDenied") is not True
                or not isinstance(f.get("cleanupSeederConnectionId"), int)
                or not isinstance(f.get("cleanupReaderConnectionId"), int)
                or f["cleanupSeederConnectionId"] <= 0
                or f["cleanupReaderConnectionId"] <= 0
                or f["cleanupSeederConnectionId"] == f["cleanupReaderConnectionId"]
                or f["writerConnectionId"] in (
                     f["cleanupSeederConnectionId"], f["cleanupReaderConnectionId"])
                or f["readerConnectionId"] == f["seederConnectionId"]
                or not isinstance(f.get("seederConnectionId"), int)
                or f["seederConnectionId"] <= 0
                or f.get("fullSixTableReplaySafety") != "FAIL_NOT_ESTABLISHED"
                or f.get("businessReplaySafetyVerdict") != "FAIL_NOT_ESTABLISHED"
                or f.get("fullSixCoherentObserved") is not False
                or f.get("sourceGapDetected") is not (n >= 5)
                or f.get("testVerdict") != ("SOURCE_GAP_CONFIRMED" if n >= 5 else "SOURCE_CHARACTERIZATION_PASS")
                or f.get("sourceOutcome") != ("ORIGINAL_REPLAY_TRUE" if n >= 5 else "ORIGINAL_THROW_SQL_CONSTRAINT")
                or f.get("originalInsertCount") != [5,0,1,4,0,0][n-1]):
                fail("PARTIAL_" + method)
        else:
            if (before != ONE or f.get("sourceGapDetected") is not False
                or f.get("fullSixCoherentObserved") is not True
                or f.get("businessReplaySafetyVerdict") != "NOT_ASSESSED_CONFLICT_ONLY"
                or f.get("fixtureCleanupRequired") is not False
                or f.get("seederRole") != "NOT_USED"
                or f.get("seederDdlDenied") is not False
                or not is_digest(f.get("candidateGraphAfter"))
                or f.get("fullSixTableReplaySafety") != "NOT_ASSESSED_CONFLICT_ONLY"
                or f.get("sourceOutcome") != (
                  "ORIGINAL_THROW_TYPED_CONFLICT" if method in
                  {"conflict01","conflict02","conflict03","conflict04","conflict05"}
                  else "ORIGINAL_THROW_SQL_CONSTRAINT"
                )):
                fail("CONFLICT_" + method)
            expected_writes = (0 if method in {"conflict01","conflict02","conflict03",
               "conflict04","conflict05","conflict06A"} else
               4 if method == "conflict06D" else 1)
            if f.get("originalInsertCount") != expected_writes:fail("CONFLICT_WRITES")
            if method.startswith("conflict06") and f.get("candidateSixAfter") != ZERO:
                fail("CANDIDATE_NOT_ROLLED_BACK")
        if f.get("sourceOutcome") == "ORIGINAL_THROW_SQL_CONSTRAINT":
            if not re.fullmatch(r"[0-9A-Z]{5}:[0-9]+",f.get("sqlError","")):
                fail("MISSING_DB_SQLSTATE")
        entries[method] = f
    output = {"schema": "P1B_REAL_MYSQL8_SOURCE_CHARACTERIZATION_V1",
              "sourceMainSha": d["sourceMainSha"], "prHeadSha": head,
              "syntheticMergeSha": checkout, "runId": runid, "cases": entries,
              "observedCases": len(entries), "sourceGapDetected": True,
              "fullSixTableReplaySafe": False, "downstreamAuthority": False}
    out = ROOT / "diagnosis-service/target/u07-d5o2-stage-b-p1b-evidence.json"
    out.write_text(json.dumps(output, indent=2, sort_keys=True) + "\n")
    print("STAGE_B_P1B_VERIFIED_CASES=" + str(len(entries)))
    print("P1B_FOUR_TABLE_SOURCE_GAP=CONFIRMED_AND_NOT_SAFE")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["bootstrap","verify"])
    args = parser.parse_args()
    d = json.loads(MANIFEST.read_text())
    ddl = check_contract(d)
    if args.mode == "bootstrap":bootstrap(d, ddl)
    else:verify(d)

if __name__ == "__main__": main()
