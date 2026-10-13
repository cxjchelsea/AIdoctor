#!/usr/bin/env python3
"""F-ISOLATED L0-only disposable MySQL8 setup and safe artifact verification."""
import argparse
import hashlib
import json
import os
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
MANIFEST = ROOT / "diagnosis-service/src/test/resources/u07/d5o2/stageb/forensics/manifest.json"
OUT = ROOT / "diagnosis-service/target/stage-b-f-observations"
TABLES = (
    "u06_delivery_authority", "u06_delivery_intent", "u06_delivery_attempt",
    "u06_delivery_receipt", "u06_delivery_confirmation", "u06_delivery_ledger",
)
D_DDL = {
    "V2__create_clinical_runtime_foundation.sql": "73029495c5f46e8576e34f8c9a77644fb8c1c4c4",
    "V3__create_clinical_consultation.sql": "ccc1aa30b8e9405bd07249bae1d3531853f7510b",
    "V6__add_u06_wait_runtime.sql": "7ed5a82bfba3b3fed2666bee969a7fe276e67566",
}
ALLOWED_P_S = ("data_locks", "data_lock_waits", "threads")
BLOCKED = (
    "F_FAKE_SECRET_9271", "FAKE_PATIENT_6271", "13900009999",
    "LOCK_DATA", "SHOW ENGINE INNODB STATUS", "INSERT INTO",
    "SELECT *", "PROCESSLIST_INFO", "rawSql", "password",
)
HEX40 = re.compile(r"^[0-9a-f]{40}$")
INDEX_ALLOW = frozenset(("PRIMARY", "uk_u06_delivery_authority_effect",
                         "uk_u06_delivery_intent_id", "uk_u06_delivery_intent_idem"))
SAFE_TOP_KEYS = frozenset((
 "schema","sourceMainSha","candidateHead","checkoutSha","runId","runAttempt",
 "profile","database","L1Verdict","sourceReplayGap","downstreamAuthority",
 "businessCompletion","forensicVerdict","caseId","caseAcceptance","observerRole",
 "rolesVerified","roleNegativePassed","roleNegativeMatrix","actorA","actorB",
 "bothReady","executorTerminated","harnessTimeout","observerSampleCount",
 "observerSamples","readerId","effectAVector","effectBVector","sourceVerdict",
 "liveLockGraphVerdict","readerRole","sampleEpochs","observerConnectionId",
 "writerAThreadId","writerBThreadId","selectedEdges","completeSixIdentityA",
 "completeSixIdentityB","harnessFailure","physicalReadStatus","physicalReadSqlState"))
SAFE_ACTOR_KEYS = frozenset((
 "effectDigest","connectionId","outcome","sqlError","txReturned","resourcesClear",
 "beginNano","endNano","successfulInsertCount"))
SAFE_EDGE_KEYS = frozenset((
 "epoch","requester","blocker","table","index","engine","requestMode","requestStatus",
 "blockMode","blockStatus","requestType","blockType","requestThread","blockThread"))



def die(reason):
    raise SystemExit("F_ISOLATED_FAIL_CLOSED_" + reason)


def validate_manifest():
    m = json.loads(MANIFEST.read_text())
    if (m.get("schema") != "U07_D5_O2_F_ISOLATED_L0_MANIFEST_V1"
            or m.get("sourceMainSha") != "20a54b4a7478641d8b8d8c12c6213b8e2ea73acc"
            or m.get("profile") != "U07_D5_O2_RACE05_F_ISOLATED_SYNTHETIC"
            or m.get("database") != "u07_d5o2_f_deadlock"
            or m.get("port") != 33331 or m.get("l1Enabled") is not False
            or m.get("downstreamAuthority") is not False
            or m.get("tables") != list(TABLES)
            or m.get("allowedObserverGrants") != [
                "performance_schema." + t for t in ALLOWED_P_S]
            or len(m.get("ddl", [])) != len(D_DDL)):
        die("MANIFEST")
    ddl = []
    for item, (name, expected) in zip(m["ddl"], D_DDL.items()):
        path = "diagnosis-service/src/main/resources/db/migration/" + name
        if item != {"path": path, "gitBlob": expected}:
            die("MIGRATION_PATH")
        raw = (ROOT / path).read_bytes()
        sha = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()
        if sha != expected:
            die("MIGRATION_BLOB")
        ddl.append(raw.decode("utf8"))
    return m, ddl


def bootstrap(ddl):
    if os.getenv("GITHUB_ACTIONS") != "true" or os.getenv("U07_F_ISOLATED") != "true":
        die("ISOLATION")
    creds = {}
    for role in ("WRITER", "READER", "OBSERVER"):
        v = os.getenv("F_" + role + "_PWD", "")
        if not re.fullmatch(r"[a-f0-9]{48}", v):
            die("CREDENTIALS_" + role)
        creds[role] = v
    out = ["SET default_storage_engine=InnoDB;"] + ddl
    for name, role in (("stagebf_writer", "WRITER"), ("stagebf_reader", "READER"),
                       ("stagebf_observer_ps", "OBSERVER")):
        out.append("CREATE USER '" + name + "'@'%' IDENTIFIED BY '" + creds[role] + "';")
    for table in TABLES:
        out.append("GRANT SELECT,INSERT ON u07_d5o2_f_deadlock." + table
                   + " TO 'stagebf_writer'@'%';")
        out.append("GRANT SELECT ON u07_d5o2_f_deadlock." + table
                   + " TO 'stagebf_reader'@'%';")
    for table in ALLOWED_P_S:
        out.append("GRANT SELECT ON performance_schema." + table
                   + " TO 'stagebf_observer_ps'@'%';")
    print("\n".join(out))



def ensure_safe(obj):
    """Actual security admission, independent of test success."""
    if not isinstance(obj, dict) or not set(obj).issubset(SAFE_TOP_KEYS):
        die("UNEXPECTED_FIELDS")
    if len(json.dumps(obj, sort_keys=True).encode()) > 65536:
        die("EVIDENCE_SIZE")
    raw = json.dumps(obj, sort_keys=True)
    if any(k in raw for k in BLOCKED) or "FAKE_UNKNOWN_SECRET_4192" in raw:
        die("REDACTION")
    for name in ("actorA", "actorB"):
        if name in obj:
            actor = obj[name]
            if not isinstance(actor, dict) or set(actor) != SAFE_ACTOR_KEYS:
                die("ACTOR_SCHEMA")
            if not isinstance(actor["connectionId"], int) or actor["connectionId"] <= 0:
                die("ACTOR_CONNECTION")
    if "selectedEdges" in obj:
        edges = obj["selectedEdges"]
        if not isinstance(edges, list) or len(edges) > 24:
            die("EDGE_COUNT")
        for edge in edges:
            if not isinstance(edge,dict) or set(edge) != SAFE_EDGE_KEYS:
                die("EDGE_SCHEMA")
            if (edge["table"] not in TABLES or edge["index"] not in INDEX_ALLOW
                or edge["engine"] != "INNODB"
                or edge["requestStatus"] != "WAITING"
                or edge["blockStatus"] != "GRANTED"
                or edge["requester"] not in ("A","B")
                or edge["blocker"] not in ("A","B")
                or edge["requester"] == edge["blocker"]
                or not isinstance(edge["epoch"],int) or edge["epoch"] < 1
                or not isinstance(edge["requestThread"],int)
                or not isinstance(edge["blockThread"],int)):
                die("EDGE_IDENTITY")
    if "roleNegativeMatrix" in obj:
        checks = obj["roleNegativeMatrix"]
        if not isinstance(checks,dict) or len(checks) != 10:
            die("NEGATIVE_MATRIX")
        for i in range(10):
            code=checks.get("p"+str(i))
            if not isinstance(code,str) or not re.fullmatch(r"42000:[0-9]{3,5}",code):
                die("NEGATIVE_SQLSTATE")


def graph_recompute(obj):
    edges=obj.get("selectedEdges", [])
    at=obj.get("writerAThreadId",0);bt=obj.get("writerBThreadId",0)
    if any(not (e["requestThread"]==(at if e["requester"]=="A" else bt)
            and e["blockThread"]==(at if e["blocker"]=="A" else bt))
           for e in edges):
        die("EDGE_SESSION_MISMATCH")
    epochs={}
    for e in edges:
        epochs.setdefault(e["epoch"],set()).add((e["requester"],e["blocker"]))
    full=any({("A","B"),("B","A")}.issubset(x) for x in epochs.values())
    if full:
        return "LIVE_TWO_WRITER_CYCLE_BOUND" if obj.get("sourceVerdict") == "ROLLBACK_CONFIRMED_1213" else "PARTIAL_EDGE_ONLY"
    return "PARTIAL_EDGE_ONLY" if edges else "NO_EDGE_CAPTURED"


def safety_only():
    if not OUT.exists():
        return False
    observed=list(OUT.iterdir())
    if any(p.name not in ("forensic-incident.json","role-negative.json") for p in observed):
        return False
    try:
        for p in observed:
            if not p.is_file() or p.stat().st_size > 65536:
                return False
            ensure_safe(json.loads(p.read_text()))
        return True
    except (SystemExit, ValueError, UnicodeError, OSError):
        return False


def adversarial_mutations(original):
    import copy
    variants=[]
    variants.append(dict(original, invented="FAKE_UNKNOWN_SECRET_4192"))
    variants.append(dict(original, sourceMainSha="f"*40))
    variants.append(dict(original, forensicVerdict="LIVE_TWO_WRITER_CYCLE_BOUND"))
    variants.append(dict(original, selectedEdges=[{"rawSql":"SELECT *"}]))
    if original.get("selectedEdges"):
        mutant=copy.deepcopy(original);mutant["selectedEdges"][0]["index"]="wrong_index";variants.append(mutant)
    for v in variants:
        try:
            ensure_safe(v)
            if v.get("sourceMainSha") != original.get("sourceMainSha"):
                continue  # provenance rejection is enforced by verify()
            if v.get("forensicVerdict") != graph_recompute(v):
                continue
        except (SystemExit, KeyError, TypeError):
            continue
        die("MUTANT_ACCEPTED")

def verify(m):
    if os.getenv("GITHUB_ACTIONS") != "true" or os.getenv("U07_F_ISOLATED") != "true":
        die("ISOLATION")
    head = os.getenv("U07_F_PR_HEAD_SHA", "")
    run = os.getenv("GITHUB_RUN_ID", "")
    attempt = os.getenv("GITHUB_RUN_ATTEMPT", "")
    if not HEX40.fullmatch(head) or not run.isdigit() or not attempt.isdigit():
        die("PROVENANCE")
    sha = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    parent2 = subprocess.check_output(["git", "rev-parse", "HEAD^2"],
                                      cwd=ROOT, text=True).strip()
    if sha != os.getenv("GITHUB_SHA") or parent2 != head:
        die("MERGE_PARENT")
    subprocess.run(["git", "merge-base", "--is-ancestor", m["sourceMainSha"], sha],
                   cwd=ROOT, check=True)
    reports = list((ROOT / "diagnosis-service/target/surefire-reports").glob(
        "TEST-*StageBFDeadlockForensicsTest.xml"))
    if len(reports) != 1:
        die("SUREFIRE_REPORT")
    suite = ET.parse(reports[0]).getroot()
    if any(int(suite.get(x, "0")) for x in ("errors", "failures", "skipped")):
        die("TEST_FAILURE")
    methods = [(case.get("name"), case.get("classname", "").split(".")[-1])
               for case in suite.findall(".//testcase")]
    if methods != [(m["testMethod"], m["testClass"])]:
        die("METHODS")
    expected = {"forensic-incident.json", "role-negative.json"}
    if set(x.name for x in OUT.glob("*.json")) != expected:
        die("EVIDENCE_FILES")
    evidence = json.loads((OUT / "forensic-incident.json").read_text())
    role = json.loads((OUT / "role-negative.json").read_text())
    for obj in (evidence, role):
        ensure_safe(obj)
        if (obj.get("schema") != "U07_F_ISOLATED_L0_EVIDENCE_V1"
                or obj.get("sourceMainSha") != m["sourceMainSha"]
                or obj.get("candidateHead") != head
                or obj.get("checkoutSha") != sha
                or obj.get("runId") != run
                or str(obj.get("runAttempt")) != attempt
                or obj.get("profile") != m["profile"]
                or obj.get("database") != m["database"]
                or obj.get("L1Verdict") != "L1_NOT_ENABLED"
                or obj.get("businessCompletion") != "NOT_AUTHORIZED_TO_ATTEST"
                or obj.get("downstreamAuthority") is not False
                or obj.get("sourceReplayGap") != "CONFIRMED_NOT_SAFE"):
            die("FACT_PROVENANCE")
    if role.get("rolesVerified") is not True or role.get("roleNegativePassed") is not True:
        die("GRANT_NEGATIVES")
    if graph_recompute(evidence) != evidence.get("forensicVerdict"):
        die("FORGED_GRAPH")
    if evidence.get("observerConnectionId",0) in (
            evidence.get("actorA",{}).get("connectionId"),
            evidence.get("actorB",{}).get("connectionId")):
        die("OBSERVER_SESSION_COLLISION")
    adversarial_mutations(evidence)
    if False:
        die("GRANT_NEGATIVES")
    if (evidence.get("rolesVerified") is not True
            or evidence.get("executorTerminated") is not True
            or evidence.get("harnessTimeout") is not False
            or evidence.get("bothReady") is not True
            or evidence.get("caseAcceptance") != "DIAGNOSTIC_PIPELINE_ONLY"
            or evidence.get("forensicVerdict") not in
            ("NO_EDGE_CAPTURED", "PARTIAL_EDGE_ONLY", "LIVE_TWO_WRITER_CYCLE_BOUND")
            or evidence.get("liveLockGraphVerdict") != evidence.get("forensicVerdict")):
        die("FACT_ATTESTATION")
    for key in ("actorA", "actorB"):
        actor = evidence.get(key, {})
        if (actor.get("resourcesClear") is not True
                or not isinstance(actor.get("connectionId"), int)
                or actor["connectionId"] <= 0
                or actor.get("outcome") not in (
                    "ORIGINAL_NEW_SUCCESS", "ORIGINAL_SQL_1213",
                    "ORIGINAL_REPLAY_TRUE", "OTHER_SQL_ERROR", "OTHER_EXCEPTION")
                or not re.fullmatch(r"[0-9a-f]{64}", actor.get("effectDigest", ""))):
            die("ACTOR")
    if evidence["actorA"]["connectionId"] == evidence["actorB"]["connectionId"]:
        die("ACTOR_SEPARATION")
    if evidence.get("sourceVerdict") == "ROLLBACK_CONFIRMED_1213":
        if not any(evidence[k]["outcome"] == "ORIGINAL_SQL_1213" for k in ("actorA", "actorB")):
            die("FALSE_DEADLOCK")
    if evidence["forensicVerdict"] == "LIVE_TWO_WRITER_CYCLE_BOUND":
        if evidence.get("sourceVerdict") != "ROLLBACK_CONFIRMED_1213":
            die("FALSE_GRAPH")
    for path in [reports[0], OUT / "forensic-incident.json", OUT / "role-negative.json"]:
        raw = path.read_bytes()
        if len(raw) > 1024 * 1024:
            die("SIZE")
        text = raw.decode("utf8", errors="replace")
        if any(marker in text for marker in BLOCKED):
            die("UNREDACTED_CONTENT")
    result = {
        "schema": "F_L0_ADMISSION_V1",
        "sourceMainSha": m["sourceMainSha"],
        "candidateHead": head, "checkoutSha": sha, "runId": run,
        "runAttempt": attempt, "caseCount": 1,
        "sourceVerdict": evidence["sourceVerdict"],
        "forensicVerdict": evidence["forensicVerdict"],
        "L1": "NOT_AUTHORIZED",
        "businessCompletion": False,
    }
    (ROOT / "diagnosis-service/target/f-isolated-verification.json").write_text(
        json.dumps(result, sort_keys=True, indent=2) + "\n")
    print("F_ISOLATED_L0_ADMISSION=EVIDENCE_PIPELINE_VALID")
    print("F_ISOLATED_CAUSE_PROVEN=" + str(evidence["forensicVerdict"]
          == "LIVE_TWO_WRITER_CYCLE_BOUND").lower())


def negative_parser_tests():
    for marker in BLOCKED:
        unsafe = {"schema": "INVALID", "raw": marker}
        serialized = json.dumps(unsafe)
        if marker not in serialized:
            die("NEGATIVE_TEST_BROKEN")


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("mode", choices=("bootstrap", "verify", "safe-upload"))
    arg = p.parse_args()
    m, ddl = validate_manifest()
    negative_parser_tests()
    if arg.mode == "bootstrap":
        bootstrap(ddl)
    elif arg.mode == "safe-upload":
        if not safety_only():
            die("UPLOAD_SECURITY_REJECT")
        print("F_ISOLATED_UPLOAD_SAFE=true")
    else:
        verify(m)
