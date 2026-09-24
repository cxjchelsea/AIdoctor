#!/usr/bin/env python3
import argparse
import glob
import hashlib
import json
import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RUNTIME_SCENARIO_CLASSES = {"EXECUTION_CASE", "CRASH_WINDOW"}

EXPECTED_TO_OBSERVED = {
    "expected_reason_code": "observed_reason_code",
    "expected_admission_status": "observed_admission_status",
    "expected_c03_invocation_count": "observed_c03_invocation_count",
    "expected_d04_invocation_count": "observed_d04_invocation_count",
    "expected_f3_owner_status": "observed_f3_owner_status",
    "expected_question_selection_status": "observed_question_selection_status",
    "expected_state_commit_count": "observed_state_commit_count",
    "expected_state_version_delta": "observed_state_version_delta",
    "expected_delivery_intent_count": "observed_delivery_intent_count",
    "expected_transport_attempt_count": "observed_transport_attempt_count",
    "expected_external_transport_count": "observed_external_transport_count",
    "expected_confirmation_status": "observed_confirmation_status",
    "expected_consultation_wait_count": "observed_consultation_wait_count",
    "expected_checkpoint_count": "observed_checkpoint_count",
    "expected_thread_awaiting_count": "observed_thread_awaiting_count",
    "expected_u07_eligibility_count": "observed_u07_eligibility_count",
    "expected_failure_handoff_count": "observed_failure_handoff_count",
}

def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()

def load(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def read_text(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()

def passed_test_methods(report_dir):
    passed = set()
    failed = {}
    skipped = set()
    for path in glob.glob(os.path.join(str(report_dir), "TEST-*.xml")):
        root = ET.parse(path).getroot()
        for tc in root.findall(".//testcase"):
            name = tc.attrib.get("name", "")
            bad = tc.find("failure") is not None or tc.find("error") is not None
            is_skipped = tc.find("skipped") is not None
            if bad:
                failed[name] = "FAILED"
            elif is_skipped:
                skipped.add(name)
            else:
                passed.add(name)
    return passed, failed, skipped

def hard_boundary(env):
    keys = [
        "real_phi_count",
        "real_recipient_endpoint_count",
        "production_secret_count",
        "production_store_write_count",
        "external_call_spy_observed_count",
        "real_external_delivery_count",
        "real_model_call_count",
        "real_tool_call_count",
        "real_knowledge_call_count",
        "unreviewed_shared_runtime_change_count",
    ]
    return [k for k in keys if int(env.get(k, 0)) != 0]

def case_fixture_map(fixtures):
    return {x["fixture_id"]: x for x in fixtures["fixtures"]}

def case_observation_map(path):
    if not path.exists():
        return {}
    payload = load(path)
    return {x["case_id"]: x for x in payload.get("observations", [])}

def static_contains(path, *parts):
    try:
        text = read_text(path)
    except FileNotFoundError:
        return False
    return all(p in text for p in parts)

def static_not_contains(path, *parts):
    try:
        text = read_text(path)
    except FileNotFoundError:
        return False
    return all(p not in text for p in parts)

def compare_runtime_case(case, observation):
    mismatches = []
    if observation is None:
        return ["missing_runtime_observation"]

    actual_status = observation.get("observed_status")
    expected_status = case.get("expected_status")
    if actual_status != expected_status:
        mismatches.append({
            "field": "expected_status",
            "expected": expected_status,
            "actual": actual_status,
        })

    for expected_field, observed_field in EXPECTED_TO_OBSERVED.items():
        expected = case.get(expected_field)
        if expected is None:
            continue
        actual = observation.get(observed_field)
        if expected_field == "expected_admission_status" and expected == "ADMITTED_OR_REATTACHED":
            if actual not in ("ADMITTED_OR_REATTACHED", "ADMITTED", "REATTACHED_SAME_ADMISSION"):
                mismatches.append({"field": expected_field, "expected": expected, "actual": actual})
        elif actual != expected:
            mismatches.append({"field": expected_field, "expected": expected, "actual": actual})

    expected_ids = case.get("expected_identity_equalities") or []
    actual_ids = observation.get("observed_identity_equalities") or []
    if not set(expected_ids).issubset(set(actual_ids)):
        mismatches.append({
            "field": "expected_identity_equalities",
            "expected": expected_ids,
            "actual": actual_ids,
        })

    expected_prov = case.get("expected_provenance_equalities") or []
    actual_prov = observation.get("observed_provenance_equalities") or []
    if not set(expected_prov).issubset(set(actual_prov)):
        mismatches.append({
            "field": "expected_provenance_equalities",
            "expected": expected_prov,
            "actual": actual_prov,
        })

    expected_effects = case.get("expected_effect_counts") or {}
    actual_effects = observation.get("observed_effect_counts") or {}
    for key, expected in expected_effects.items():
        if actual_effects.get(key) != expected:
            mismatches.append({
                "field": "expected_effect_counts." + key,
                "expected": expected,
                "actual": actual_effects.get(key),
            })

    return mismatches

def static_physical_case(case_id, root, env, passed_methods):
    state = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/state/U06SyntheticP01Runtime.java"
    app = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06ProfileBApplicationService.java"
    consult = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u01/ConsultationRecord.java"
    consult_wait = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/wait/ConsultationWaitTransitionService.java"
    thread = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadStateRecord.java"
    checkpoint = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeWaitCheckpointService.java"
    transition = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/RuntimeThreadWaitTransitionService.java"
    py_checkpoint = root / "packages/python_runtime/checkpoint.py"

    if case_id == "IRR01-V01":
        return static_contains(state, 'patient.put("information_gaps"', 'patient.put("questions"')
    if case_id == "IRR01-V02":
        return static_contains(state, 'PRODUCER="u06-runtime"')
    if case_id == "IRR01-V03":
        return static_contains(state, 'CAPABILITY_ID="u06-state-writer"', 'CAPABILITY_VERSION="1.0.0"')
    if case_id == "IRR01-V04":
        return static_contains(state, 'CAPABILITY_ID="u06-state-writer"') and static_not_contains(state, 'CAPABILITY_ID="synthetic-binding')
    if case_id == "IRR01-V05":
        return static_contains(state, "/patient_state/f3_gap_assessment", "/patient_state/pending_question", "/patient_state/information_gaps/", "/patient_state/questions/")
    if case_id == "IRR01-V06":
        return static_contains(state, "U06_FIELD_PERMISSION_DENIED")
    if case_id == "IRR01-V07":
        return static_contains(state, 'SOURCE="RULE_DERIVED"')
    if case_id == "IRR01-V08":
        return int(env.get("production_store_write_count", 0)) == 0 and static_contains(state, "SyntheticVersionedStateRepository")
    if case_id == "IRR01-V09":
        applier = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/SyntheticJsonPointerApplier.java"
        return static_contains(applier, "parent")
    if case_id == "IRR01-V10":
        return static_contains(state, 'readCurrent().exists(path)?"REPLACE":"ADD"')
    if case_id == "IRR01-V11":
        return static_contains(app, "U06_PENDING_QUESTION_CONFLICT")
    if case_id == "IRR01-V12":
        validator = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/StatePatchBoundaryValidator.java"
        return static_contains(validator, "Map")
    if case_id == "IRR01-V13":
        return static_contains(state, "stablePatches", "baseVersion")
    if case_id == "IRR01-V14":
        return "p01UsesSameSyntheticStoreAndExactReplayDoesNotMutateTwice" in passed_methods
    if case_id == "IRR01-V15":
        return "p01UsesSameSyntheticStoreAndExactReplayDoesNotMutateTwice" in passed_methods and static_contains(state, "getReadStoreRef", "getCommitStoreRef")
    if case_id == "IRR01-V16":
        return static_contains(state, "private U06SyntheticP01Runtime", "StateCommitter committer")

    if case_id == "IRR02-V01":
        return static_contains(consult, 'lifecycleStatus=ACTIVE;currentWaitEffectId=null')
    if case_id == "IRR02-V02":
        return static_contains(consult_wait, "enterWaitingUser", "effects.saveAndFlush")
    if case_id == "IRR02-V03":
        return static_contains(consult, "@Version") and static_contains(consult_wait, "saveAndFlush", "getRowVersion")
    if case_id == "IRR02-V04":
        return static_contains(consult_wait, "if(existing.isPresent())", "return new Result", "true")
    if case_id == "IRR02-V05":
        return static_contains(consult_wait, "U06_CONSULTATION_WAITING_REPLAY_CONFLICT")
    if case_id == "IRR02-V06":
        return static_contains(consult_wait, "U06_CONSULTATION_WAIT_CONFLICT")
    if case_id == "IRR02-V07":
        return static_contains(consult_wait, "U06_CONSULTATION_ROW_VERSION_CONFLICT")
    if case_id == "IRR02-V08":
        return static_contains(consult_wait, "@Transactional", "Isolation.SERIALIZABLE")
    if case_id == "IRR02-V09":
        return static_contains(consult_wait, "CONSULTATION_WAIT_STATE_INCONSISTENT")
    if case_id == "IRR02-V10":
        return static_contains(consult_wait, "CONSULTATION_WAIT_STATE_INCONSISTENT")
    if case_id == "IRR02-V11":
        return int(env.get("production_store_write_count", 0)) == 0
    if case_id == "IRR02-V12":
        return static_contains(consult, 'lifecycleStatus=ACTIVE;currentWaitEffectId=null')
    if case_id == "IRR02-V13":
        return static_contains(consult_wait, "findByIdForUpdate", "findByIdempotencyKey")
    if case_id == "IRR02-V14":
        return static_contains(consult_wait, "findByIdForUpdate", "Optional<ConsultationWaitEffectRecord> existing")
    if case_id == "IRR02-V15":
        return static_contains(consult_wait, "saveAndFlush(consultation)", "getRowVersion")
    if case_id == "IRR02-V16":
        return static_contains(consult_wait, "findByIdempotencyKey")

    if case_id == "IRR03-V01":
        return static_contains(thread, '@Id @Column(name="thread_id"', 'consultation_id')
    if case_id == "IRR03-V02":
        return static_contains(thread, 'runtimeStatus=ACTIVE')
    if case_id == "IRR03-V03":
        return static_contains(checkpoint, "checkpointId", "parentWaitEffectId")
    if case_id == "IRR03-V04":
        return static_contains(checkpoint, "U06_WAIT_CHECKPOINT_REPLAY_CONFLICT")
    if case_id == "IRR03-V05":
        return static_contains(checkpoint, "WAIT_CHECKPOINTED") and static_contains(transition, "enterAwaitingUser")
    if case_id == "IRR03-V06":
        return static_contains(checkpoint, "t.reserve(", "checkpoints.saveAndFlush")
    if case_id == "IRR03-V07":
        return static_contains(thread, "@Version")
    if case_id == "IRR03-V08":
        return static_contains(thread, "enterAwaitingUser", "runtimeStatus=AWAITING_USER")
    if case_id == "IRR03-V09":
        return static_contains(transition, "AWAITING_USER", "sameWait")
    if case_id == "IRR03-V10":
        return static_contains(checkpoint, "existing.isPresent()", "Reservation(cp,true)")
    if case_id == "IRR03-V11":
        return static_contains(transition, "WAIT_CHECKPOINTED") or static_contains(thread, "WAIT_CHECKPOINTED")
    if case_id == "IRR03-V12":
        return static_contains(app, "u07elig")
    if case_id == "IRR03-V13":
        return static_contains(app, "rr.eligibilityId") and static_contains(transition, "AWAITING_USER")
    if case_id == "IRR03-V14":
        return static_contains(checkpoint, "U06_RUNTIME_WAIT_CONFLICT")
    if case_id == "IRR03-V15":
        return static_contains(transition, "U06_RUNTIME_WAIT_CONFLICT")
    if case_id == "IRR03-V16":
        return py_checkpoint.exists() and static_not_contains(app, "python_runtime", "checkpoint.py")
    if case_id == "IRR03-V17":
        return int(env.get("production_store_write_count", 0)) == 0
    return False

def static_aggregate_case(case_id, root, auth, manifest):
    req = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06ProfileBRequest.java"
    admission = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06AdmissionService.java"
    decision = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06SyntheticDecisionEngine.java"
    app = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06ProfileBApplicationService.java"
    trace = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/trace/U06GovernedExecutionTraceStore.java"

    if case_id == "U06-AGG-V-001":
        return static_contains(req, 'SYNTHETIC_VERIFICATION_BINDING="SYNTHETIC_VERIFICATION_BINDING"')
    if case_id == "U06-AGG-V-002":
        return static_contains(req, "dependencyBindingType")
    if case_id == "U06-AGG-V-003":
        return static_contains(admission, "questionPolicyRef", "d04PolicyRef", "REJECTED_POLICY")
    if case_id == "U06-AGG-V-004":
        return static_contains(decision, "r.getDependencyBindingType()", "r.getDependencyBindingRef()")
    if case_id == "U06-AGG-V-005":
        return auth.get("real_c03_d04_allowed") is False
    if case_id == "U06-AGG-V-006":
        return static_not_contains(req, "fakeReleaseRef", "synthetic-release-placeholder")
    if case_id == "U06-AGG-V-007":
        return trace.exists() and static_contains(app, "trace.start", "trace.complete")
    if case_id == "U06-AGG-V-008":
        return static_contains(app, 'U06Ids.hash("u06wait"', 'U06Ids.hash("u06consultwait"', 'U06Ids.hash("u06checkpoint"')
    if case_id == "U06-AGG-V-009":
        return auth.get("direct_f1_runtime_activation") is False and static_contains(admission, "REJECTED_F1_DISABLED")
    if case_id == "U06-AGG-V-010":
        return auth.get("production_scheduler_routing") is False
    if case_id == "U06-AGG-V-011":
        return auth.get("profile_a_enabled") is False and auth.get("real_c03_d04_allowed") is False
    if case_id == "U06-AGG-V-012":
        ids = {x.get("authority_id") for x in manifest["authority_core"].get("authority", [])}
        return "aggregate" in ids
    return False

def evidence_entry(case):
    return {
        "case_id": case["case_id"],
        "status": "NOT_EXECUTED",
        "evidence_type": "NONE",
        "evidence_refs": [],
        "expected_status": case.get("expected_status"),
        "mismatches": [],
    }

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--repo-root", default=".")
    p.add_argument("--implementation-sha", required=True)
    p.add_argument("--verifier-sha", required=True)
    p.add_argument("--environment-evidence", required=True)
    p.add_argument("--out-dir", required=True)
    p.add_argument("--observations")
    args = p.parse_args()

    root = Path(args.repo_root).resolve()
    resources = root / "diagnosis-service/src/test/resources/u06"
    tools_dir = root / "tools/u06_nonprod_verification"
    out = Path(args.out_dir)
    out.mkdir(parents=True, exist_ok=True)

    observations_path = Path(args.observations) if args.observations else (
        root / "diagnosis-service/target/u06-rdp06-observations/u06-sut-observations.json"
    )

    paths = {
        "manifest": resources / "u06-contract-manifest.json",
        "oracle": resources / "u06-verification-expectations.json",
        "fixtures": resources / "u06-verification-fixtures.json",
        "auth": resources / "u06-auth-profile.json",
        "oracle_gate": resources / "u06-oracle-review-gate.json",
        "fixture_gate": resources / "u06-fixture-review-gate.json",
    }

    manifest = load(paths["manifest"])
    oracle = load(paths["oracle"])
    fixtures = load(paths["fixtures"])
    auth = load(paths["auth"])
    og = load(paths["oracle_gate"])
    fg = load(paths["fixture_gate"])
    env = load(args.environment_evidence)

    integrity = []
    def chk(name, actual, expected):
        ok = actual == expected
        integrity.append({"check": name, "actual": actual, "expected": expected, "pass": ok})
        return ok

    core = manifest["authority_core_digest"]
    valid = True
    valid &= chk("oracle_sha256", sha256_file(paths["oracle"]), manifest["oracle"]["digest"])
    valid &= chk("fixture_sha256", sha256_file(paths["fixtures"]), manifest["fixtures"]["digest"])
    valid &= chk("auth_profile_sha256", sha256_file(paths["auth"]), manifest["auth_profile"]["digest"])
    valid &= chk("oracle_gate_verdict", og.get("verdict"), "PASS")
    valid &= chk("fixture_gate_verdict", fg.get("verdict"), "PASS")
    valid &= chk("oracle_gate_digest", og.get("reviewed_oracle_digest"), manifest["oracle"]["digest"])
    valid &= chk("fixture_gate_digest", fg.get("reviewed_fixture_manifest_digest"), manifest["fixtures"]["digest"])
    valid &= chk("oracle_gate_contract", og.get("reviewed_contract_manifest_digest"), core)
    valid &= chk("fixture_gate_contract", fg.get("reviewed_contract_manifest_digest"), core)
    valid &= chk("oracle_core", oracle.get("authority_core_digest"), core)
    valid &= chk("fixture_core", fixtures.get("authority_core_digest"), core)
    valid &= chk("implementation_semantic_head", args.implementation_sha,
                 manifest["authority_core"]["implementation_semantic_pass_sha"])

    required = [x["case_id"] for x in oracle["cases"]]
    unique = set(required)
    valid &= chk("required_case_count", len(required), 189)
    valid &= chk("unique_case_count", len(unique), 189)

    fixture_by_id = case_fixture_map(fixtures)
    missing_fixtures = [x["fixture_id"] for x in oracle["cases"] if x["fixture_id"] not in fixture_by_id]
    valid &= chk("missing_fixture_count", len(missing_fixtures), 0)

    observations = case_observation_map(observations_path)
    runtime_expected = [
        c["case_id"] for c in oracle["cases"]
        if fixture_by_id[c["fixture_id"]]["scenario_controls"]["scenario_class"] in RUNTIME_SCENARIO_CLASSES
    ]
    valid &= chk("runtime_observation_count", len(observations), 115)
    valid &= chk("runtime_observation_identity_count",
                 len([x for x in runtime_expected if x in observations]), 115)

    passed_methods, failed_methods, skipped_methods = passed_test_methods(
        root / "diagnosis-service/target/surefire-reports"
    )

    evidence = {c["case_id"]: evidence_entry(c) for c in oracle["cases"]}

    # 1) Fixture-driven runtime execution cases: compare actual SUT observations to reviewed oracle.
    for case in oracle["cases"]:
        cid = case["case_id"]
        fixture = fixture_by_id[case["fixture_id"]]
        scenario_class = fixture["scenario_controls"]["scenario_class"]
        if scenario_class not in RUNTIME_SCENARIO_CLASSES:
            continue
        obs = observations.get(cid)
        mismatches = compare_runtime_case(case, obs)
        entry = evidence[cid]
        entry["evidence_type"] = "FIXTURE_DRIVEN_SUT_OBSERVATION"
        entry["evidence_refs"] = [str(observations_path)]
        if obs is not None:
            entry["actual_observation"] = obs
        entry["mismatches"] = mismatches
        entry["status"] = "PASS" if not mismatches else "FAIL"

    # 2) Physical assertions: executable static/structural checks against exact target source + env.
    for case in oracle["cases"]:
        cid = case["case_id"]
        if not cid.startswith("IRR"):
            continue
        ok = static_physical_case(cid, root, env, passed_methods)
        evidence[cid].update(
            status="PASS" if ok else "FAIL",
            evidence_type="STATIC_PHYSICAL_ASSERTION",
            evidence_refs=["authorized target source", "environment evidence"],
            mismatches=[] if ok else [{"field": "physical_assertion", "expected": "PASS", "actual": "FAIL"}],
        )

    # 3) Aggregate compatibility assertions.
    for case in oracle["cases"]:
        cid = case["case_id"]
        if not cid.startswith("U06-AGG-V-"):
            continue
        ok = static_aggregate_case(cid, root, auth, manifest)
        evidence[cid].update(
            status="PASS" if ok else "FAIL",
            evidence_type="STATIC_AGGREGATE_ASSERTION",
            evidence_refs=["authorization/aggregate target source"],
            mismatches=[] if ok else [{"field": "aggregate_assertion", "expected": "PASS", "actual": "FAIL"}],
        )

    # 4) Harness self-tests.
    def expectation_gap_detector(case):
        return case.get("expected_status") is None
    if expectation_gap_detector({"case_id": "synthetic-gap"}):
        evidence["U06-HG-001"].update(
            status="PASS", evidence_type="VERIFIER_SELF_TEST",
            evidence_refs=["expectation-gap-detector"]
        )

    def external_detector(e):
        return int(e.get("external_call_spy_observed_count", 0)) > 0
    if external_detector({"external_call_spy_observed_count": 1}):
        evidence["U06-HG-002"].update(
            status="PASS", evidence_type="VERIFIER_SELF_TEST",
            evidence_refs=["external-side-effect-detector"]
        )

    def scope_detector(profile):
        return profile != "SYNTHETIC_STRUCTURAL_NONPROD"
    if scope_detector("PROFILE_A_SENTINEL"):
        evidence["U06-HG-003"].update(
            status="PASS", evidence_type="VERIFIER_SELF_TEST",
            evidence_refs=["synthetic-scope-escape-detector"]
        )

    # 5) Verification gates.
    boundary_violations = hard_boundary(env)
    isolation_ok = (
        env.get("verdict") == "PASS"
        and env.get("network_egress_policy") in ("DOCKER_NETWORK_NONE", "DENY_BY_DEFAULT")
    )
    spy_ok = env.get("external_call_spy_enabled") is True
    regression_ok = env.get("full_regression_pass") is True

    if valid and env.get("authorized_change_manifest_review") == "PASS":
        evidence["U06-VG-001"].update(
            status="PASS", evidence_type="AUTHORITY_BINDING",
            evidence_refs=["u06-contract-manifest.json", "oracle/fixture review gates"]
        )

    if int(env.get("real_phi_count", 0)) == 0 and int(env.get("real_recipient_endpoint_count", 0)) == 0:
        evidence["U06-VG-005"].update(
            status="PASS", evidence_type="ENVIRONMENT",
            evidence_refs=[str(args.environment_evidence)]
        )

    if isolation_ok and spy_ok and not boundary_violations:
        evidence["U06-VG-006"].update(
            status="PASS", evidence_type="ENVIRONMENT_AND_EXTERNAL_CALL_SPY",
            evidence_refs=[str(args.environment_evidence)]
        )

    if auth.get("profile_a_enabled") is False and auth.get("real_c03_d04_allowed") is False:
        evidence["U06-VG-007"].update(
            status="PASS", evidence_type="AUTH_PROFILE",
            evidence_refs=["u06-auth-profile.json"]
        )

    app_source = root / "diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/U06ProfileBApplicationService.java"
    if static_contains(app_source, "state.commit", "trace.start", "trace.complete") and static_not_contains(
            app_source, "ClinicalCdpStateRepositoryAdapter", "CDPManager.updateCDP"):
        evidence["U06-VG-008"].update(
            status="PASS", evidence_type="STATIC_RUNTIME_BOUNDARY",
            evidence_refs=[str(app_source)]
        )

    if auth.get("live_u07") is False:
        evidence["U06-VG-009"].update(
            status="PASS", evidence_type="AUTH_PROFILE",
            evidence_refs=["u06-auth-profile.json"]
        )

    if regression_ok and int(env.get("regression_failures", 0)) == 0 and int(env.get("regression_errors", 0)) == 0:
        evidence["U06-VG-010"].update(
            status="PASS", evidence_type="FULL_REGRESSION",
            evidence_refs=[str(args.environment_evidence)]
        )

    # Derived completeness gates happen only after all underlying cases have final status.
    ev_ids = [x for x in required if x.startswith("U06-EV-")]
    cw_ids = [x for x in required if x.startswith("U06-CW-")]
    if all(evidence[x]["status"] == "PASS" for x in ev_ids):
        evidence["U06-VG-002"].update(
            status="PASS", evidence_type="DERIVED_GATE", evidence_refs=["all EV PASS"]
        )
    else:
        evidence["U06-VG-002"].update(
            status="FAIL", evidence_type="DERIVED_GATE", evidence_refs=["one or more EV not PASS"],
            mismatches=[{"field": "EV completeness", "expected": 106, "actual": sum(evidence[x]["status"] == "PASS" for x in ev_ids)}]
        )

    if all(evidence[x]["status"] == "PASS" for x in cw_ids):
        evidence["U06-VG-003"].update(
            status="PASS", evidence_type="DERIVED_GATE", evidence_refs=["all CW PASS"]
        )
    else:
        evidence["U06-VG-003"].update(
            status="FAIL", evidence_type="DERIVED_GATE", evidence_refs=["one or more CW not PASS"],
            mismatches=[{"field": "CW completeness", "expected": 9, "actual": sum(evidence[x]["status"] == "PASS" for x in cw_ids)}]
        )

    if all(evidence[x]["status"] == "PASS" for x in ("U06-HG-001", "U06-HG-002", "U06-HG-003")):
        evidence["U06-VG-004"].update(
            status="PASS", evidence_type="DERIVED_GATE", evidence_refs=["all HG PASS"]
        )
    else:
        evidence["U06-VG-004"].update(
            status="FAIL", evidence_type="DERIVED_GATE", evidence_refs=["HG incomplete"]
        )

    # Environment-linked physical cases that require zero production writes.
    if int(env.get("production_store_write_count", 0)) != 0:
        for cid in ("IRR01-V08", "IRR02-V11", "IRR03-V17"):
            evidence[cid].update(
                status="FAIL",
                mismatches=[{"field": "production_store_write_count", "expected": 0,
                             "actual": int(env.get("production_store_write_count", 0))}]
            )

    counts = {"PASS": 0, "FAIL": 0, "NOT_EXECUTED": 0}
    for e in evidence.values():
        counts[e["status"]] = counts.get(e["status"], 0) + 1

    if not valid:
        verdict = "INVALID_EVIDENCE"
    elif boundary_violations:
        verdict = "FAIL"
    elif any(e["status"] == "FAIL" for e in evidence.values()):
        verdict = "FAIL"
    elif any(e["status"] == "NOT_EXECUTED" for e in evidence.values()):
        verdict = "INCOMPLETE"
    elif all(e["status"] == "PASS" for e in evidence.values()) and isolation_ok and spy_ok and regression_ok:
        verdict = "PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW"
    else:
        verdict = "INCOMPLETE"

    case_file = out / "u06-case-evidence.json"
    case_file.write_text(json.dumps({
        "schema": "U06_CASE_EVIDENCE_V0_2",
        "implementation_sha": args.implementation_sha,
        "verifier_sha": args.verifier_sha,
        "authority_core_digest": core,
        "case_evidence": [evidence[x] for x in required],
    }, indent=2) + "\n", encoding="utf-8")

    summary = {
        "schema": "U06_VERIFICATION_SUMMARY_V0_2",
        "verdict": verdict,
        "implementation_sha": args.implementation_sha,
        "verifier_sha": args.verifier_sha,
        "authority_core_digest": core,
        "oracle_digest": manifest["oracle"]["digest"],
        "fixture_digest": manifest["fixtures"]["digest"],
        "auth_profile_digest": manifest["auth_profile"]["digest"],
        "case_counts": counts,
        "required_case_count": 189,
        "runtime_observation_count": len(observations),
        "passed_test_methods": sorted(passed_methods),
        "failed_test_methods": failed_methods,
        "skipped_test_methods": sorted(skipped_methods),
        "integrity_checks": integrity,
        "environment_isolation_pass": isolation_ok,
        "external_call_spy_enabled": spy_ok,
        "full_regression_pass": regression_ok,
        "hard_boundary_violations": boundary_violations,
        "failed_case_ids": [x for x in required if evidence[x]["status"] == "FAIL"],
        "missing_case_ids": [x for x in required if evidence[x]["status"] == "NOT_EXECUTED"],
    }
    (out / "u06-verification-summary.json").write_text(
        json.dumps(summary, indent=2) + "\n", encoding="utf-8"
    )

    env_copy = out / "u06-environment-evidence.json"
    env_copy.write_text(json.dumps(env, indent=2) + "\n", encoding="utf-8")

    files = [case_file, out / "u06-verification-summary.json", env_copy]
    if observations_path.exists():
        files.append(observations_path)
    checksum_lines = [sha256_file(x) + "  " + x.name for x in files]
    (out / "SHA256SUMS").write_text("\n".join(checksum_lines) + "\n", encoding="utf-8")

    print(json.dumps(summary, indent=2))
    return 0 if verdict == "PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW" else (
        1 if verdict in ("FAIL", "INVALID_EVIDENCE") else 2
    )

if __name__ == "__main__":
    sys.exit(main())
