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
        safe = [ln for ln in p.stdout.splitlines() if
                "BUILD FAILURE" in ln or "BUILD SUCCESS" in ln or
                "Tests run:" in ln or "PROBE_" in ln or "No tests" in ln]
        raise RuntimeError("COMMAND_FAILED " + argv[0] + " rc=" +
                           str(p.returncode) + " signals=" + repr(safe[-12:])[:1200])
    return p
def git(repo, *args): return cmd(["git", "-C", str(repo), *args]).stdout.strip()
def suite_xml(source):
    reports = source / "diagnosis-service/target/surefire-reports"
    out = []
    for path in reports.glob("TEST-*.xml"):
        root = ET.parse(path).getroot()
        for tc in root.findall(".//testcase"):
            out.append((tc.attrib.get("classname", ""), tc.attrib.get("name", ""),
                        len(tc.findall("failure")), len(tc.findall("error")),
                        len(tc.findall("skipped"))))
    return out
def validate_selector(source):
    wf = source / ".github/workflows"
    all_owners = []
    for owner, classes in OWNERS.items():
        text = (wf / SELECTORS[owner]).read_text(encoding="utf-8")
        # Verify exact dedicated selectors with literal test names.
        for name in classes:
            check(re.search(r"\b" + re.escape(name) + r"\b", text) is not None,
                  "SEL_OWNER_SELECTOR_ABSENT_" + name)
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
    check(git(control, "status", "--porcelain") == "", "FEA_CONTROL_DIRTY")
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
    check(bad.returncode != 0, "FEA_BAD_HASH_MUTANT_ACCEPTED")
    absent = cmd(mvn + ["-f", str(source / "diagnosis-service/pom.xml"),
                        "-Dtest=DefinitelyNoSuchTest_U07", "surefire:test"], env=runenv, expect=1)
    check(absent.returncode != 0, "FEA_ZERO_TEST_MUTANT_ACCEPTED")
    # Actual exact exclusion syntax smoke on selected safe ordinary test.
    cmd(mvn + ["-f", str(source / "diagnosis-service/pom.xml"),
               "-Dtest=" + ORDINARY + ",!" + DEDICATED[-1], "surefire:test"], env=runenv)
    report = suite_xml(source)
    check(any(ORDINARY in t[0] for t in report), "SEL_SAFE_ORDINARY_NOT_RUN")
    # Full 11-class selector expression static resolution; running whole suite
    # requires a separate dedicated-runner coverage and baseline authorization.
    candidate = "*,!" + ",!".join(DEDICATED)
    check(candidate.count(",!") == 11, "SEL_EXCLUSIONS_NOT_EXACT_11")
    check(git(source, "status", "--porcelain") == "", "FEA_TRACKED_SOURCE_MUTATED")
    print(json.dumps({
      "verdict":"NON_DB_PROBE_FEASIBILITY_POSITIVE",
      "sourceSha":head,"controlSha":ctrl,
      "storeSourceBlob":git(source,"rev-parse","HEAD:diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/delivery/JdbcU06DeliveryStore.java"),
      "storeCompiledSha256":store_hash,
      "originTestExecuted":len(pos),
      "badHashRejected":True,"zeroTestsRejected":True,
      "safeOrdinarySelectorExecuted":True,
      "exact11SelectorExecution":"NOT_RUN",
      "fullSEL_N01_16":"NOT_RUN",
      "mainMySQLPhysical":"NOT_RUN"
    }, sort_keys=True))
if __name__ == "__main__":
    try: main()
    except Exception as exc:
        print("NON_DB_PROBE_FAILURE=" + str(exc)[:650], file=sys.stderr)
        sys.exit(1)
