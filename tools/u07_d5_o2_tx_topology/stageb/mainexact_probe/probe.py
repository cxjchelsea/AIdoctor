#!/usr/bin/env python3
"""Isolated no-DB feasibility probe. Does not modify tracked files or invoke live delivery."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET

EXPECTED = "33c0914e843d7976c7ff2272338377aa4925f443"
EXPECTED_STORE_BLOB = "6b67b75bb121410a0a78b6cbf188a5369ad5d1fb"
TEST = "ProbeStoreOriginTest"
RESOURCE = "com/aidoctor/diagnosis/runtime/u06/delivery/JdbcU06DeliveryStore.class"
ORDINARY = "U02ClinicalFactFormationTest"
DEDICATED = [
 "U07D5O2StageAMultiOwnerTest", "U07D5O2StageANegativeTest",
 "U07D5O2StageATopologyTest", "U07D5O2StageATxPhysicalTest",
 "StageBJdbcFaultHookTest", "StageBTxOwnerBindingTest",
 "StageBFDeadlockForensicsTest", "StageBP1AtomicityTest",
 "StageBP1ReplayConflictTest", "StageBP1BPartialLedgerTest",
 "StageBP1BReplayConflictTest",
]
OWNERS = {
 "stage-a": DEDICATED[0:4], "stage-b-p0": DEDICATED[4:6],
 "stage-b-forensics": DEDICATED[6:7],
 "stage-b-p1": DEDICATED[7:9], "stage-b-p1b": DEDICATED[9:11],
}
SELECTORS = {
 "stage-a": "u07-d5-o2-stage-a.yml",
 "stage-b-p0": "u07-d5-o2-stage-b-p0.yml",
 "stage-b-forensics": "u07-d5-o2-stage-b-forensics.yml",
 "stage-b-p1": "u07-d5-o2-stage-b-p1.yml",
 "stage-b-p1b": "u07-d5-o2-stage-b-p1b.yml",
}
def check(cond, message):
    if not cond: raise RuntimeError(message)
def sha256(p): return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def cmd(argv, cwd=None, env=None, expect=0):
    p = subprocess.run(argv, cwd=cwd, env=env, text=True,
                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=240)
    if expect is not None and (p.returncode == 0) != (expect == 0):
        # Never echo full Maven/JDBC logs.
        # Strict, typed boolean signals only: never forward arbitrary Maven/SQL output.
        signals = {
            "buildFailure": "BUILD FAILURE" in p.stdout,
            "testFailures": "There are test failures" in p.stdout,
            "noTests": ("No tests were executed" in p.stdout or
                        "No tests matching pattern" in p.stdout),
            "nonzero": p.returncode != 0,
        }
        raise RuntimeError("COMMAND_FAILED_" + Path(argv[0]).name +
                           "_SIGNALS_" + json.dumps(signals, sort_keys=True))
    return p
def git(repo, *args): return cmd(["git", "-C", str(repo), *args]).stdout.strip()
def suite_xml(source):
    reports = source / "diagnosis-service/target/surefire-reports"
    out = []
    for path in sorted(reports.glob("TEST-*.xml")):
        root = ET.parse(path).getroot()
        for tc in root.findall(".//testcase"):
            skips = tc.findall("skipped")
            status = "ERROR" if tc.findall("error") else ("FAIL" if tc.findall("failure") else ("SKIP" if skips else "PASS"))
            out.append((tc.attrib.get("classname", ""), tc.attrib.get("name", ""),
                        int(bool(tc.findall("failure"))), int(bool(tc.findall("error"))),
                        int(bool(skips)), status))
    return out
def parse_executable_test_selector(text):
    # Names in comments, display titles or shell echo are not executable ownership.
    commands = re.findall(r"^\s*mvn\s+[^\n]*?-Dtest=([A-Za-z0-9_,]+)\s+test\s*$",
                          text, flags=re.MULTILINE)
    check(len(commands) == 1, "SEL_EXECUTABLE_MAVEN_TEST_COMMAND_NOT_UNIQUE")
    names = commands[0].split(",")
    check(len(names) == len(set(names)), "SEL_EXECUTABLE_SELECTOR_DUPLICATE")
    return names

def selector_parser_negatives():
    legitimate = "  mvn -B -f diagnosis-service/pom.xml -Dtest=AlphaTest,BetaTest test\n"
    check(parse_executable_test_selector(legitimate) == ["AlphaTest", "BetaTest"],
          "SEL_PARSER_POSITIVE_REJECTED")
    mutations = [
      "# mvn -B -f diagnosis-service/pom.xml -Dtest=AlphaTest,BetaTest test\n",
      "name: -Dtest=AlphaTest,BetaTest\n",
      "echo mvn -Dtest=AlphaTest,BetaTest test\n",
      "mvn -Dtest=AlphaTest,AlphaTest test\n",
      "mvn -Dtest=AlphaTest test\nmvn -Dtest=BetaTest test\n",
    ]
    for i, raw in enumerate(mutations):
        try:
            parse_executable_test_selector(raw)
        except RuntimeError:
            continue
        raise RuntimeError("SEL_PARSER_MUTANT_ACCEPTED_" + str(i))

def validate_selector(source):
    wf = source / ".github/workflows"
    all_owners = []
    for owner, classes in OWNERS.items():
        text = (wf / SELECTORS[owner]).read_text(encoding="utf-8")
        # Extract executable Maven -Dtest= selector; a name in a comment is not proof.
        actual = parse_executable_test_selector(text)
        check(len(actual) == len(set(actual)), "SEL_OWNER_DUPLICATE_" + owner)
        check(set(actual) == set(classes), "SEL_OWNER_EXECUTABLE_SCOPE_MISMATCH_" + owner)
        all_owners.extend(classes)
    check(len(all_owners) == len(set(all_owners)) == 11, "SEL_NOT_UNIQUE_11")
    tree = source / "diagnosis-service/src/test/java"
    for name in DEDICATED:
        hits = list(tree.rglob(name + ".java"))
        check(len(hits) == 1, "SEL_SOURCE_CLASS_ABSENT_OR_AMBIGUOUS_" + name)
    return True
def selector_mutants():
    good = set(DEDICATED)
    check(len(good) == 11, "SEL_LIST_INVALID")
    tests = {
        "missing": set(DEDICATED[:-1]),
        "extra-ordinary": good | {ORDINARY},
        "case-typo": (good - {DEDICATED[0]}) | {DEDICATED[0].lower()},
        "duplicate": DEDICATED + [DEDICATED[0]],
    }
    for key, value in tests.items():
        if isinstance(value, list):
            check(not(len(value) == len(set(value)) == 11), "SEL_MUTANT_NOT_REJECTED_" + key)
        else:
            check(value != good, "SEL_MUTANT_NOT_REJECTED_" + key)
    for forbidden in ("-DskipTests", "-Dmaven.test.skip=true", "**/u07/**", "*Stage*"):
        check(forbidden not in ("*,!" + ",!".join(DEDICATED)), "SEL_BROAD_SELECTOR")
def main():
    base = Path(os.environ["GITHUB_WORKSPACE"])
    control = (base / "control").resolve()
    source = (base / "source").resolve()
    check(control != source and not control.is_relative_to(source)
          and not source.is_relative_to(control), "FEA_WORKTREE_OVERLAP")
    head = git(source, "rev-parse", "HEAD")
    ctrl = git(control, "rev-parse", "HEAD")
    check(head == EXPECTED, "FEA_SOURCE_SHA_NOT_EXACT")
    check(ctrl == os.environ["PROBE_CONTROL_HEAD"], "FEA_CONTROL_SHA_NOT_EXACT")
    check(git(source, "status", "--porcelain") == "", "FEA_SOURCE_DIRTY")
    check(git(source, "rev-parse", "HEAD:diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/delivery/JdbcU06DeliveryStore.java") == EXPECTED_STORE_BLOB, "FEA_SOURCE_STORE_BLOB_MISMATCH")
    check(git(control, "status", "--porcelain") == "", "FEA_CONTROL_DIRTY")
    selector_parser_negatives()
    validate_selector(source)
    selector_mutants()
    m2 = Path(tempfile.mkdtemp(prefix="u07-probe-m2-"))
    env = dict(os.environ)
    env["MAVEN_OPTS"] = "-Dmaven.repo.local=" + str(m2)
    mvn = ["mvn", "-B", "-ntp"]
    cmd(mvn + ["-f", str(source / "contracts/v1/bindings/java/pom.xml"),
               "-DskipTests", "install"], env=env)
    cmd(mvn + ["-f", str(source / "diagnosis-service/pom.xml"),
               "-DskipTests", "test-compile"], env=env)
    store = source / "diagnosis-service/target/classes" / RESOURCE
    check(store.is_file(), "FEA_STORE_CLASS_MISSING")
    store_hash = sha256(store)
    check(store.read_bytes()[6:8] == b"\x00\x34", "FEA_STORE_NOT_JAVA8")
    check(git(source, "status", "--porcelain") == "", "FEA_SOURCE_CHANGED_AFTER_COMPILE")
    cpfile = Path(tempfile.mkstemp(prefix="u07-probe-classpath-")[1])
    cmd(mvn + ["-f", str(source / "diagnosis-service/pom.xml"),
               "org.apache.maven.plugins:maven-dependency-plugin:3.3.0:build-classpath",
               "-Dmdep.includeScope=test", "-Dmdep.outputFile=" + str(cpfile)], env=env)
    deps = cpfile.read_text().strip()
    check(bool(deps) and "junit-jupiter" in deps, "FEA_JUNIT5_CLASSPATH_MISSING")
    sourceclasses = source / "diagnosis-service/target/classes"
    testclasses = source / "diagnosis-service/target/test-classes"
    ctrl_test = control / "tools/u07_d5_o2_tx_topology/stageb/mainexact_probe/ProbeStoreOriginTest.java"
    augment = Path(tempfile.mkdtemp(prefix="u07-probe-javac-"))
    javac = ["javac", "-source", "8", "-target", "8",
             "-cp", os.pathsep.join([str(sourceclasses), str(testclasses), deps]),
             "-d", str(augment), str(ctrl_test)]
    cmd(javac, env=env)
    compiled = list(augment.rglob("*.class"))
    check(len(compiled) == 1 and compiled[0].name == TEST + ".class",
          "FEA_CONTROLLER_CLASS_INVENTORY")
    target = testclasses / compiled[0].relative_to(augment)
    check(not target.exists(), "FEA_STALE_OVERLAY")
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(compiled[0], target)
    check(sha256(compiled[0]) == sha256(target), "FEA_OVERLAY_CHANGED")
    check(sha256(store) == store_hash, "FEA_STORE_OVERWRITTEN")
    runenv = dict(env, PROBE_SOURCE_CLASSES=str(sourceclasses.resolve()),
                  PROBE_STORE_CLASS_SHA256=store_hash,
                  PROBE_STORE_SOURCE_BLOB=EXPECTED_STORE_BLOB,
                  PROBE_TARGET_MAIN_SHA=EXPECTED)
    # Verify Surefire 2.22.x sees a staged JUnit5 class, executes exactly 1.
    cmd(mvn + ["-f", str(source / "diagnosis-service/pom.xml"),
               "-Dtest=" + TEST, "surefire:test"], env=runenv)
    report = suite_xml(source)
    pos = [t for t in report if t[0].endswith("." + TEST)]
    check(len(pos) == 1 and not any(t[2] or t[3] or t[4] for t in pos),
          "FEA_JUNIT5_TEST_NOT_ACTUALLY_EXECUTED")
    check(sha256(store) == store_hash, "FEA_POST_RUN_STORE_CHANGED")
    # Runtime byte mismatch must be rejected by genuine JUnit execution.
    wrong = dict(runenv, PROBE_STORE_CLASS_SHA256="0" * 64)
    bad = cmd(mvn + ["-f", str(source / "diagnosis-service/pom.xml"),
                     "-Dtest=" + TEST, "surefire:test"], env=wrong, expect=1)
    bad_errors = suite_xml(source)
    check(bad.returncode != 0 and any(c.endswith("." + TEST) and (failure or error) and
          "loadedStoreMustBeFromPinnedHistoricalSource" == n
          for c,n,failure,error,_,_ in bad_errors),
          "FEA_BAD_HASH_MUTANT_CAUSE_UNVERIFIED")
    check("PROBE_STORE_RUNTIME_BYTES_MISMATCH" in bad.stdout or
          "PROBE_STORE_LOADED_BYTES_MISMATCH" in bad.stdout,
          "FEA_BAD_HASH_EXPECTED_ASSERTION_MISSING")
    absent = cmd(mvn + ["-f", str(source / "diagnosis-service/pom.xml"),
                        "-Dtest=DefinitelyNoSuchTest_U07", "surefire:test"], env=runenv, expect=1)
    check(absent.returncode != 0 and ("No tests were executed" in absent.stdout or
          "No tests matching pattern" in absent.stdout),
          "FEA_ZERO_TEST_REASON_UNVERIFIED")
    # Actual exact exclusion syntax smoke on selected safe ordinary test.
    cmd(mvn + ["-f", str(source / "diagnosis-service/pom.xml"),
               "-Dtest=" + ORDINARY + ",!" + DEDICATED[-1], "surefire:test"], env=runenv)
    report = suite_xml(source)
    check(any(ORDINARY in t[0] for t in report), "SEL_SAFE_ORDINARY_NOT_RUN")
    # Execute unmodified generic test selection as an EXPECTED FAIL baseline,
    # retain Surefire case identities, then run exact 11 exclusions.
    reports = source / "diagnosis-service/target/surefire-reports"
    if reports.exists(): shutil.rmtree(reports)
    print("PROBE_STAGE=FULL_BASELINE_EXPECTED_FAIL", flush=True)
    base_run = cmd(mvn + ["-f", str(source / "diagnosis-service/pom.xml"),
                          "surefire:test"], env=runenv, expect=1)
    check(base_run.returncode != 0, "SEL_BASELINE_UNEXPECTEDLY_GREEN")
    baseline_cases = suite_xml(source)
    check(len(baseline_cases) >= 11, "SEL_BASELINE_XML_NOT_COMPLETE")
    baseline_by_class = {c.split(".")[-1] for c, _, _, _, _, _ in baseline_cases}
    check(set(DEDICATED).issubset(baseline_by_class),
          "SEL_BASELINE_MISSING_DEDICATED_TEST_CLASS")
    ordinary_before = sorted((c, n, status) for c,n,_,_,_,status in baseline_cases
                             if c.split(".")[-1] not in DEDICATED)
    check(len(ordinary_before) == len(set((c,n) for c,n,_ in ordinary_before)), "SEL_ORDINARY_DUPLICATE_CASE")
    baseline_errors = sum(e + failure for _,_,failure,e,_,_ in baseline_cases)
    check(baseline_errors > 0, "SEL_BASELINE_EXPECTED_ISOLATED_FAILURE_MISSING")
    if reports.exists(): shutil.rmtree(reports)
    candidate = "Test*,*Test,*Tests,*TestCase,!" + ",!".join(DEDICATED)
    check(candidate.count(",!") == 11, "SEL_EXCLUSIONS_NOT_EXACT_11")
    print("PROBE_STAGE=EXACT_ELEVEN_NEGATIVE_SELECTOR", flush=True)
    cmd(mvn + ["-f", str(source / "diagnosis-service/pom.xml"),
               "-Dtest=" + candidate, "surefire:test"], env=runenv)
    selected_cases = suite_xml(source)
    check(len(selected_cases) > 0, "SEL_ALL_TESTS_WERE_EXCLUDED")
    check(not any(c.split(".")[-1] in DEDICATED
                  for c,_,_,_,_,_ in selected_cases),
          "SEL_DEDICATED_TEST_EXECUTED_IN_GENERIC")
    ordinary_after = sorted((c,n,status) for c,n,_,_,_,status in selected_cases)
    check(len(ordinary_after) == len(set((c,n) for c,n,_ in ordinary_after)), "SEL_SELECTED_DUPLICATE_CASE")
    check(ordinary_before == ordinary_after,
          "SEL_ORDINARY_PER_CASE_STATUS_DRIFT"
          + " before=" + str(len(ordinary_before))
          + " after=" + str(len(ordinary_after)))
    check(not any(failure or error
                  for _,_,failure,error,_,_ in selected_cases),
          "SEL_ORDINARY_TEST_FAILURE")
    # Negative selector: intentionally delete one of 11 exclusions, then
    # the isolated class must execute and fail its normal disposable guard.
    if reports.exists(): shutil.rmtree(reports)
    mutation = "Test*,*Test,*Tests,*TestCase,!" + ",!".join(DEDICATED[:-1])
    print("PROBE_STAGE=MISSING_ONE_EXCLUSION_EXPECTED_FAIL", flush=True)
    bad_selection = cmd(mvn + ["-f", str(source / "diagnosis-service/pom.xml"),
                              "-Dtest=" + mutation, "surefire:test"],
                        env=runenv, expect=1)
    missing_class_cases = suite_xml(source)
    check(bad_selection.returncode != 0 and
          any(c.split(".")[-1] == DEDICATED[-1] and (failure or err)
              for c,_,failure,err,_,_ in missing_class_cases),
          "SEL_MISSING_ONE_EXCLUSION_MUTANT_NOT_REJECTED")
    # Real classpath-shadow mutation: stage a duplicate original Store class in
    # test-classes (which Surefire loads first); assertion must reject code origin.
    shadow_file = testclasses / RESOURCE
    check(not shadow_file.exists(), "FEA_SHADOW_PREEXISTS")
    shadow_file.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(store, shadow_file)
    try:
        shadow_run = cmd(mvn + ["-f", str(source / "diagnosis-service/pom.xml"),
                               "-Dtest=" + TEST, "surefire:test"], env=runenv, expect=1)
        shadow_cases = suite_xml(source)
        check(shadow_run.returncode != 0 and
              any(c.endswith("." + TEST) and (failure or error)
                  for c,n,failure,error,_,_ in shadow_cases),
              "FEA_SHADOW_MUTANT_NOT_REJECTED")
        check("PROBE_STORE_CODESOURCE_MISMATCH" in shadow_run.stdout or
              "PROBE_STORE_DUPLICATE_RESOURCE" in shadow_run.stdout,
              "FEA_SHADOW_CAUSAL_MARKER_MISSING")
    finally:
        shadow_file.unlink(missing_ok=True)
    check(sha256(store) == store_hash, "FEA_SOURCE_STORE_CHANGED_AFTER_SHADOW")
    check(git(source, "status", "--porcelain") == "", "FEA_TRACKED_SOURCE_MUTATED")
    evidence = {
      "schemaVersion":"U07_D5_O2_NONDB_PROBE_V1",
      "runId":os.environ.get("GITHUB_RUN_ID", ""),
      "runAttempt":os.environ.get("GITHUB_RUN_ATTEMPT", ""),
      "verdict":"NON_DB_PROBE_FEASIBILITY_POSITIVE",
      "sourceSha":head,"controlSha":ctrl,
      "storeSourceBlob":EXPECTED_STORE_BLOB,
      "storeCompiledSha256":store_hash,
      "originTestExecuted":len(pos),
      "badHashRejected":True,"zeroTestsRejected":True,
      "safeOrdinarySelectorExecuted":True,
      "exact11SelectorExecution":"PASS_11_EXCLUDED",
      "ordinaryTestsPreserved":len(ordinary_after),
      "ordinaryPerCaseStatusPreserved":True,
      "missingExclusionNegativeRejected":True,
      "shadowClassNegativeRejected":True,
      "unfilteredBaselineErrors":baseline_errors,
      "fullSEL_N01_16":"NOT_RUN",
      "mainMySQLPhysical":"NOT_RUN"
    }
    check(bool(evidence["runId"]) and bool(evidence["runAttempt"]), "PROBE_RUN_IDENTITY_MISSING")
    evidence_path = Path(os.environ["RUNNER_TEMP"]) / "u07-nondb-probe-evidence.json"
    evidence_path.write_text(json.dumps(evidence, sort_keys=True, separators=(",", ":")) + "\n", encoding="utf-8")
    check(json.loads(evidence_path.read_text(encoding="utf-8")) == evidence, "PROBE_EVIDENCE_ROUNDTRIP")
    print(json.dumps(evidence, sort_keys=True))
def validate_evidence_object(obj):
    allowed = {
        "schemaVersion", "runId", "runAttempt", "verdict",
        "sourceSha", "controlSha", "storeSourceBlob",
        "storeCompiledSha256", "originTestExecuted",
        "badHashRejected", "zeroTestsRejected",
        "safeOrdinarySelectorExecuted", "exact11SelectorExecution",
        "ordinaryTestsPreserved", "ordinaryPerCaseStatusPreserved",
        "missingExclusionNegativeRejected", "shadowClassNegativeRejected",
        "unfilteredBaselineErrors", "fullSEL_N01_16", "mainMySQLPhysical"
    }
    check(type(obj) is dict and set(obj) == allowed, "PROBE_EVIDENCE_SCHEMA_INVALID")
    check(obj["schemaVersion"] == "U07_D5_O2_NONDB_PROBE_V1" and
          obj["verdict"] == "NON_DB_PROBE_FEASIBILITY_POSITIVE" and
          obj["sourceSha"] == EXPECTED and
          obj["storeSourceBlob"] == EXPECTED_STORE_BLOB and
          obj["controlSha"] == os.environ["PROBE_CONTROL_HEAD"] and
          obj["runId"] == os.environ["GITHUB_RUN_ID"] and
          obj["runAttempt"] == os.environ["GITHUB_RUN_ATTEMPT"],
          "PROBE_EVIDENCE_PROVENANCE_MISMATCH")
    check(obj["originTestExecuted"] == 1 and
          obj["ordinaryTestsPreserved"] == 435 and
          obj["unfilteredBaselineErrors"] == 38 and
          obj["exact11SelectorExecution"] == "PASS_11_EXCLUDED" and
          obj["fullSEL_N01_16"] == "NOT_RUN" and
          obj["mainMySQLPhysical"] == "NOT_RUN", "PROBE_EVIDENCE_VERDICT_MISMATCH")
    for k in ("badHashRejected", "zeroTestsRejected", "safeOrdinarySelectorExecuted",
              "ordinaryPerCaseStatusPreserved", "missingExclusionNegativeRejected",
              "shadowClassNegativeRejected"):
        check(obj[k] is True, "PROBE_EVIDENCE_REQUIRED_NEGATIVE_MISSING")
    check(type(obj["storeCompiledSha256"]) is str and
          re.fullmatch(r"[0-9a-f]{64}", obj["storeCompiledSha256"]) is not None,
          "PROBE_EVIDENCE_SHA_FORMAT")

def verify_evidence():
    # lstat, not stat: refuse symlink or multiple-hardlink artifact aliases.
    path = Path(os.environ["RUNNER_TEMP"]) / "u07-nondb-probe-evidence.json"
    check(path.parent.is_dir() and not path.is_symlink() and path.is_file() and
          path.stat().st_nlink == 1 and path.stat().st_size < 4096,
          "PROBE_EVIDENCE_FILE_NOT_SAFE")
    raw = path.read_text(encoding="utf-8")
    obj = json.loads(raw)
    validate_evidence_object(obj)
    check(json.dumps(obj, sort_keys=True, separators=(",", ":")) + "\n" == raw,
          "PROBE_EVIDENCE_NONCANONICAL")
    # Real validator invocation on adversarial copies: no disk mutation.
    mutants = []
    for key, value in (("controlSha", "0" * 40),
                       ("runAttempt", "99999"),
                       ("sourceSha", "0" * 40),
                       ("ordinaryTestsPreserved", 434),
                       ("badHashRejected", False),
                       ("storeCompiledSha256", "invalid")):
        wrong = dict(obj)
        wrong[key] = value
        mutants.append(wrong)
    missing = dict(obj)
    del missing["shadowClassNegativeRejected"]
    mutants.append(missing)
    extra = dict(obj)
    extra["secret"] = "never-admit"
    mutants.append(extra)
    for index, mutant in enumerate(mutants):
        try:
            validate_evidence_object(mutant)
        except (RuntimeError, KeyError, TypeError):
            continue
        raise RuntimeError("PROBE_ADMISSION_MUTANT_ACCEPTED_" + str(index))
    print("PROBE_EVIDENCE_NEGATIVE_MUTANTS_REJECTED=" + str(len(mutants)))
    print("PROBE_EVIDENCE_SAFE_UPLOAD_ADMISSION=PASS")


if __name__ == "__main__":
    try:
        if sys.argv[1:] == ["--verify"]:
            verify_evidence()
        elif not sys.argv[1:]:
            main()
        else:
            raise RuntimeError("PROBE_UNKNOWN_COMMAND")
    except Exception as exc:
        print("NON_DB_PROBE_FAILURE=" + str(exc)[:1900], file=sys.stderr)
        sys.exit(1)
