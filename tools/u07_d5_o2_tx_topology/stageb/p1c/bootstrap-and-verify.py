#!/usr/bin/env python3
"""Test-only original-DDL bootstrap and physical race evidence verifier."""
import argparse
import hashlib
import json
import os
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
MANIFEST = ROOT / "diagnosis-service/src/test/resources/u07/d5o2/stageb/p1c/manifest.json"
TABLES = ("u06_delivery_authority", "u06_delivery_intent", "u06_delivery_attempt",
          "u06_delivery_receipt", "u06_delivery_confirmation", "u06_delivery_ledger")
BLOBS = ("73029495c5f46e8576e34f8c9a77644fb8c1c4c4",
         "ccc1aa30b8e9405bd07249bae1d3531853f7510b",
         "7ed5a82bfba3b3fed2666bee969a7fe276e67566")
NAMES = ("V2__create_clinical_runtime_foundation.sql",
         "V3__create_clinical_consultation.sql",
         "V6__add_u06_wait_runtime.sql")
FULL = [1] * 6
EMPTY = [0] * 6
SHA = re.compile(r"^[0-9a-f]{64}$")

def fail(reason):
    raise SystemExit("P1C_FAIL_CLOSED_" + reason)

def contract():
    d = json.loads(MANIFEST.read_text())
    if (d.get("schema") != "U07_D5O2_STAGE_B_P1C_MANIFEST_V1"
        or d.get("sourceMainSha") != "20a54b4a7478641d8b8d8c12c6213b8e2ea73acc"
        or d.get("profile") != "U07_D5O2_STAGE_B_P1C_SYNTHETIC"
        or d.get("database") != "u07_d5o2_stage_b_p1c"
        or d.get("port") != 33330 or len(d.get("cases", {})) != 8
        or d.get("downstreamAuthority") is not False
        or d.get("originalFourTableReplayGapStillOpen") is not True):
        fail("MANIFEST")
    data = []
    if len(d.get("ddl", [])) != 3:
        fail("DDL_COUNT")
    for item, name, sha in zip(d["ddl"], NAMES, BLOBS):
        path = "diagnosis-service/src/main/resources/db/migration/" + name
        if item != {"path": path, "gitBlob": sha}:
            fail("DDL_NAME")
        raw = (ROOT / path).read_bytes()
        actual = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()
        if actual != sha:
            fail("DDL_BLOB_MISMATCH")
        data.append(raw.decode("utf8"))
    return d, data

def bootstrap(ddl):
    if os.getenv("GITHUB_ACTIONS") != "true" or os.getenv("U07_D5O2_P1C_ISOLATED") != "true":
        fail("NOT_ISOLATED")
    pwds = {}
    for role in ("WRITER", "READER"):
        val = os.environ.get("P1C_" + role + "_PWD", "")
        if re.fullmatch(r"[a-f0-9]{48}", val) is None:
            fail("PASSWORD_" + role)
        pwds[role] = val
    chunks = ["SET default_storage_engine=InnoDB;"] + ddl
    for role, pwd in (("stagebp1c_writer", pwds["WRITER"]),
                      ("stagebp1c_reader", pwds["READER"])):
        chunks.append("CREATE USER '" + role + "'@'%' IDENTIFIED BY '" + pwd + "';")
    for table in TABLES:
        target = "u07_d5o2_stage_b_p1c." + table
        chunks.append("GRANT SELECT,INSERT ON " + target + " TO 'stagebp1c_writer'@'%';")
        chunks.append("GRANT SELECT ON " + target + " TO 'stagebp1c_reader'@'%';")
    print("\n".join(chunks))

def digest(s):
    return isinstance(s, str) and SHA.fullmatch(s) is not None

def verify(d):
    if os.getenv("GITHUB_ACTIONS") != "true" or os.getenv("U07_D5O2_P1C_ISOLATED") != "true":
        fail("RUNNER")
    head = os.environ.get("U07_P1C_PR_HEAD_SHA", "")
    runid = os.environ.get("GITHUB_RUN_ID", "")
    if not re.fullmatch(r"[0-9a-f]{40}", head) or not runid.isdigit():
        fail("HEAD_RUN")
    checkout = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    parent = subprocess.check_output(["git", "rev-parse", "HEAD^2"], cwd=ROOT, text=True).strip()
    if checkout == head or parent != head or checkout != os.getenv("GITHUB_SHA"):
        fail("SYNTHETIC_CHECKOUT")
    subprocess.run(["git", "merge-base", "--is-ancestor", d["sourceMainSha"], checkout],
                   cwd=ROOT, check=True)
    reports = list((ROOT / "diagnosis-service/target/surefire-reports").glob(
        "TEST-*StageBP1CConcurrencyTest.xml"))
    if len(reports) != 1:
        fail("SUREFIRE_COUNT")
    suite = ET.parse(reports[0]).getroot()
    if any(int(suite.get(k, "0")) for k in ("errors", "failures", "skipped")):
        fail("SUREFIRE_FAIL")
    actual = []
    for case in suite.findall(".//testcase"):
        method = case.get("name")
        if (method not in d["cases"] or
            case.get("classname", "").split(".")[-1] != "StageBP1CConcurrencyTest" or
            any(case.find(k) is not None for k in ("error","failure","skipped"))):
            fail("METHOD_IDENTITY")
        actual.append(method)
    if len(actual) != 8 or len(set(actual)) != 8 or set(actual) != set(d["cases"]):
        fail("EIGHT_METHODS")
    folder = ROOT / "diagnosis-service/target/stage-b-p1c-observations"
    if not folder.exists() or len(list(folder.glob("*.json"))) != 8:
        fail("FACT_COUNT")
    facts = {}
    for method in actual:
        f = json.loads((folder / (method + ".json")).read_text())
        if (f.get("testMethod") != method or
            f.get("testClass") != "StageBP1CConcurrencyTest" or
            f.get("caseId") != d["cases"][method]["caseId"] or
            f.get("sourceMainSha") != d["sourceMainSha"] or
            f.get("prHeadSha") != head or f.get("mergeCheckoutSha") != checkout or
            f.get("runId") != runid or f.get("schema") != d["database"] or
            f.get("profile") != d["profile"] or
            f.get("downstreamAuthority") is not False or
            f.get("originalFourTableReplayGapStillOpen") is not True or
            f.get("businessReplaySafetyVerdict") != "NOT_PROVEN_PRODUCTION" or
            f.get("bothReady") is not True or
            f.get("transactionIntervalsOverlapped") is not True or
            f.get("executorTerminated") is not True or
            f.get("workerTimeout") is not False or
            f.get("harnessVerdict") != "OBSERVED_OVERLAP" or
            f.get("readerDeleteDenied") is not True or
            f.get("physicalSixTableSafety") != "VERIFIED_BY_FRESH_READER" or
            not isinstance(f.get("finalReaderConnectionId"), int) or
            f.get("finalReaderConnectionId") <= 0 or
            not digest(f.get("effectAAfterDigest")) or
            not digest(f.get("effectBAfterDigest"))):
            fail("FACT_" + method)
        actors = [f.get("actorA"), f.get("actorB")]
        if not all(isinstance(a, dict) for a in actors):
            fail("ACTORS")
        ids = []
        for a in actors:
            if (not isinstance(a.get("writerConnectionId"), int) or
                a["writerConnectionId"] <= 0 or
                not a.get("role","").startswith("stagebp1c_writer@") or
                a.get("isolation") != 8 or a.get("lockWait") != 5 or
                a.get("deleteDenied") is not True or
                a.get("resourceClear") is not True or
                not isinstance(a.get("beginNano"), int) or
                not isinstance(a.get("endNano"), int) or
                a["endNano"] <= a["beginNano"] or
                not isinstance(a.get("writes"), int) or not 0 <= a["writes"] <= 6 or
                a.get("sqlTrace") != list(TABLES[:a["writes"]]) or
                not digest(a.get("effectDigest")) or not digest(a.get("payloadDigest")) or
                not digest(a.get("idempotencyDigest"))):
                fail("ACTOR_" + method)
            ids.append(a["writerConnectionId"])
        if len(set(ids + [f["finalReaderConnectionId"]])) != 3:
            fail("CONNECTION_ISOLATION")
        if method == "race05":
            if f.get("newSuccessCount") != 2 or f.get("effectAAfterVector") != FULL or f.get("effectBAfterVector") != FULL:
                fail("UNRELATED_TWO_WINNERS")
        else:
            if f.get("newSuccessCount") != 1:
                fail("SINGLE_WINNER")
            if method in ("race01","race02","race06A","race06B"):
                if f.get("effectAAfterVector") != FULL or f.get("effectBAfterVector") != FULL:
                    fail("SAME_EFFECT_COMPLETE")
            else:
                if sorted([f.get("effectAAfterVector"),f.get("effectBAfterVector")]) != sorted([FULL,EMPTY]):
                    fail("DISTINCT_WINNER_LOSER")
        if method in ("race01","race06A","race06B"):
            if not isinstance(f.get("sequential"),dict) or not digest(f.get("afterSequentialDigest")):
                fail("SEQUENTIAL_EVIDENCE")
            if f.get("afterSequentialDigest") != f.get("effectAAfterDigest"):
                fail("SEQUENTIAL_INVARIANT")
            expected = "ORIGINAL_THROW_TYPED_CONFLICT" if method=="race06B" else "ORIGINAL_REPLAY_TRUE"
            if f["sequential"].get("sourceOutcome") != expected or f["sequential"].get("writes") != 0:
                fail("SEQUENTIAL_SOURCE")
        facts[method] = f
    out = ROOT / "diagnosis-service/target/u07-d5o2-stage-b-p1c-evidence.json"
    out.write_text(json.dumps({"schema":"P1C_REAL_MYSQL8_CONCURRENCY_V1",
        "sourceMainSha":d["sourceMainSha"],"prHeadSha":head,"mergeCheckoutSha":checkout,
        "runId":runid,"caseCount":len(facts),"cases":facts,
        "productionReplaySafe":False,"downstreamAuthority":False},sort_keys=True,indent=2)+"\n")
    print("STAGE_B_P1C_VERIFIED_CASES=8")
    print("P1C_PRODUCTION_REPLAY_SAFE=NOT_PROVEN")

if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("mode",choices=("bootstrap","verify"))
    args = p.parse_args()
    contract_data, ddls = contract()
    if args.mode == "bootstrap":
        bootstrap(ddls)
    else:
        verify(contract_data)
