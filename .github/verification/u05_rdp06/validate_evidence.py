#!/usr/bin/env python3
import argparse
import hashlib
import json
import pathlib
import re
import sys

TARGET = "261ee5525c8260e93db19173ffbde89a8af6810d"
AUTHORITY_DIGEST = "d5d272963066dccb14a6c0f124cfd5128bbacd363e84e96279800dd3b6e08ef7"
EFFECT_KEYS = [
    "state_commit_count",
    "readiness_effect_count",
    "invalidation_effect_count",
    "route_decision_count",
    "route_eligibility_count",
    "scheduler_target_intent_count",
    "downstream_unit_invocation_count",
    "external_delivery_count",
    "external_tool_model_call_count",
]

def fail(message):
    raise SystemExit("U05_RDP06_VALIDATION_FAIL: " + message)

def load(path):
    return json.loads(pathlib.Path(path).read_text(encoding="utf-8"))

def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

def canonical(value):
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")

def verify_checksums(root):
    sums = root / "SHA256SUMS"
    if not sums.exists():
        fail("SHA256SUMS missing")
    seen = set()
    for raw in sums.read_text(encoding="utf-8").splitlines():
        if not raw.strip():
            continue
        match = re.match(r"^([0-9a-f]{64})  (.+)$", raw)
        if not match:
            fail("malformed checksum line: " + raw)
        digest, rel = match.groups()
        if rel == "SHA256SUMS":
            fail("SHA256SUMS must not hash itself")
        path = root / rel
        if not path.is_file():
            fail("checksum target missing: " + rel)
        if sha256_file(path) != digest:
            fail("checksum mismatch: " + rel)
        if rel in seen:
            fail("duplicate checksum entry: " + rel)
        seen.add(rel)
    actual = {
        p.relative_to(root).as_posix()
        for p in root.rglob("*")
        if p.is_file() and p.name != "SHA256SUMS"
    }
    if seen != actual:
        fail("checksum coverage mismatch missing=%s extra=%s" %
             (sorted(actual - seen), sorted(seen - actual)))

def require_effect_map(value, label):
    if not isinstance(value, dict):
        fail(label + " is not a map")
    if set(value.keys()) != set(EFFECT_KEYS):
        fail(label + " effect keys mismatch")
    for key in EFFECT_KEYS:
        if not isinstance(value[key], int) or value[key] < 0:
            fail(label + " invalid count for " + key)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--bundle-dir", required=True)
    args = parser.parse_args()
    root = pathlib.Path(args.bundle_dir)

    verify_checksums(root)

    top = load(root / "evidence.json")
    if top.get("schema") != "U05_NONPROD_VERIFICATION_EVIDENCE_V0_1":
        fail("top-level schema mismatch")
    if top.get("implementation_sha") != TARGET:
        fail("implementation SHA mismatch")
    if top.get("authorization_id") != "AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001":
        fail("authorization id mismatch")
    if top.get("parent_authorization_id") != "AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001":
        fail("parent authorization id mismatch")
    if top.get("contract_manifest_digest") != AUTHORITY_DIGEST:
        fail("top-level contract manifest digest mismatch")
    if top.get("verification_verdict") != "PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW":
        fail("workflow must stop at PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW")
    if top.get("final_u05_verification") != "NOT_PASSED_PENDING_INDEPENDENT_EVIDENCE_REVIEW":
        fail("workflow must not self-declare final U05 PASS")
    if int(top.get("normal_policy_expectation_gap_count", -1)) != 0:
        fail("normal policy expectation gap count is nonzero")
    if any(top.get("hard_boundaries", {}).values()):
        fail("one or more hard authorization boundaries are enabled")

    auth = load(root / "auth-profile.json")
    if auth.get("implementation_sha") != TARGET:
        fail("auth-profile target mismatch")
    if auth.get("authorization_record_sha") != "c31f654779a4a3dc6316d955c7e4a5c6b5b8c094":
        fail("auth-profile authorization record SHA mismatch")
    if auth.get("authorization_record_blob_sha") != "9893a016130d807ecb2f1da06af23312e898fb22":
        fail("auth-profile authorization record blob mismatch")
    if auth.get("authorization_record_path") != "docs/current/06_开发单元/U05_RDP06_Authoritative_Verifier_Target_Rebind_Authorization_Decision_v0.1.md":
        fail("auth-profile authorization record path mismatch")
    if auth.get("environment") != "ci-nonprod-u05":
        fail("auth-profile environment mismatch")
    hard_auth_keys = [
        "u05_live_external_entry", "production_routing", "production_mutation",
        "real_patient_traffic", "live_u06_execution", "live_u08_execution",
        "live_u10_execution", "live_u11_execution", "live_u14_execution",
        "external_delivery_side_effects", "unauthorized_model_tool_calls",
        "shared_runtime_source_change",
    ]
    for key in hard_auth_keys:
        if auth.get(key) is not False:
            fail("auth-profile hard boundary not false: " + key)
    if auth.get("synthetic_non_phi_only") is not True:
        fail("synthetic_non_phi_only is not true")

    contract = load(root / "u05-contract-manifest.json")
    core = contract.get("authority_core")
    if hashlib.sha256(canonical(core)).hexdigest() != AUTHORITY_DIGEST:
        fail("authority_core digest mismatch")
    if contract.get("authority_core_digest") != AUTHORITY_DIGEST:
        fail("recorded authority_core_digest mismatch")
    if core.get("implementation_sha") != TARGET:
        fail("authority core implementation target mismatch")
    if len(core.get("contracts", [])) != 6 or len(core.get("referenced_phases", [])) != 4:
        fail("contract/phase authority inventory incomplete")

    reviewed = contract.get("reviewed_inputs", {})
    expected_files = {
        "expectation_oracle": root / "u05-verification-expectations.json",
        "precedence_oracle": root / "u05-d03-precedence-expectations.json",
        "fixture_manifest": root / "u05-verification-fixtures.json",
    }
    for key, path in expected_files.items():
        entry = reviewed.get(key, {})
        if entry.get("sha256") != sha256_file(path):
            fail("reviewed input digest mismatch: " + key)
        if not str(entry.get("review_id", "")).isdigit():
            fail("review id missing for " + key)
    if not reviewed.get("reviewed_verifier_head"):
        fail("reviewed verifier head missing")

    expectations = load(root / "u05-verification-expectations.json")
    expected_cases = expectations.get("cases", [])
    if len(expected_cases) != 60:
        fail("expectation oracle must contain exactly 60 EV cases")
    expected_by_id = {c.get("case_id"): c for c in expected_cases}
    required_ids = {"U05-EV-%03d" % n for n in range(1, 61)}
    if set(expected_by_id.keys()) != required_ids:
        fail("expectation case ID set mismatch")
    for case in expected_cases:
        if case.get("contract_manifest_digest") != AUTHORITY_DIGEST:
            fail(case["case_id"] + " contract digest mismatch")
        if not case.get("expected_authority_refs"):
            fail(case["case_id"] + " missing expected authority refs")
        require_effect_map(case.get("expected_effect_counts"), case["case_id"] + " expected")

    cases_bundle = load(root / "u05-case-evidence.json")
    cases = cases_bundle.get("cases", [])
    if len(cases) != 60:
        fail("case evidence must contain exactly 60 records")
    ids = [c.get("case_id") for c in cases]
    if set(ids) != required_ids or len(set(ids)) != 60:
        fail("case evidence identity/uniqueness mismatch")
    for case in cases:
        cid = case["case_id"]
        exp = expected_by_id[cid]
        if case.get("implementation_sha") != TARGET:
            fail(cid + " implementation SHA mismatch")
        if case.get("fixture_id") != exp.get("fixture_id") or case.get("fixture_digest") != exp.get("fixture_digest"):
            fail(cid + " fixture binding mismatch")
        if case.get("expected_authority_refs") != exp.get("expected_authority_refs"):
            fail(cid + " expected authority refs changed")
        if case.get("expected_boundary") != exp.get("expected_boundary"):
            fail(cid + " embedded expected boundary changed")
        if case.get("expected_result") != exp.get("expected_result"):
            fail(cid + " embedded expected result changed")
        if case.get("observed_boundary") != exp.get("expected_boundary"):
            fail(cid + " observed boundary mismatch")
        if case.get("observed_result") != exp.get("expected_result"):
            fail(cid + " observed result mismatch")
        if case.get("policy_expectation_gap") is not False:
            fail(cid + " policy expectation gap fired")
        require_effect_map(case.get("expected_effect_counts"), cid + " evidence expected")
        require_effect_map(case.get("observed_effect_counts"), cid + " observed")
        if case.get("expected_effect_counts") != exp.get("expected_effect_counts"):
            fail(cid + " evidence expected effect map differs from oracle")
        if case.get("observed_effect_counts") != exp.get("expected_effect_counts"):
            fail(cid + " observed effect map mismatch")
        observed = case["observed_effect_counts"]
        if observed["downstream_unit_invocation_count"] != 0:
            fail(cid + " invoked downstream Unit")
        if observed["external_delivery_count"] != 0:
            fail(cid + " produced external delivery")
        if observed["external_tool_model_call_count"] != 0:
            fail(cid + " called external model/tool")
        refs = case.get("side_effect_evidence_refs", {})
        nonzero_requires = {
            "state_commit_count": "state_commit",
            "readiness_effect_count": "readiness_effect",
            "invalidation_effect_count": "invalidation_effect",
            "route_eligibility_count": "route_eligibility",
            "scheduler_target_intent_count": "scheduler_target_intent",
        }
        for count_key, ref_key in nonzero_requires.items():
            if observed[count_key] > 0 and not refs.get(ref_key):
                fail(cid + " nonzero " + count_key + " lacks durable/inspectable ref")
        if observed["route_decision_count"] > 0:
            route_refs = [k for k in refs if k.startswith("route_decision")]
            if not route_refs:
                fail(cid + " nonzero route_decision_count lacks evidence ref")
        expected_eq_ids = set(case.get("expected_provenance_equalities", []))
        observed_eqs = case.get("observed_provenance_equalities", [])
        observed_eq_ids = {eq.get("equality") for eq in observed_eqs}
        if observed_eq_ids != expected_eq_ids:
            fail(cid + " provenance equality identity coverage mismatch expected=%s observed=%s" %
                 (sorted(expected_eq_ids), sorted(observed_eq_ids)))
        for eq in observed_eqs:
            if eq.get("equal") is not True:
                fail(cid + " provenance equality failed: " + str(eq.get("equality")))
        if case.get("pass") is not True:
            fail(cid + " pass=false")

    fixtures = load(root / "u05-verification-fixtures.json")
    if fixtures.get("synthetic") is not True or fixtures.get("contains_real_phi") is not False:
        fail("fixture manifest top-level synthetic/non-PHI guard failed")
    fixture_by_id = {}
    for fixture in fixtures.get("fixtures", []):
        fid = fixture.get("fixture_id")
        if fid in fixture_by_id:
            fail("duplicate fixture id " + str(fid))
        payload = fixture.get("fixture_payload")
        if payload.get("synthetic") is not True or payload.get("contains_real_phi") is not False:
            fail("fixture is not synthetic/non-PHI: " + str(fid))
        digest = hashlib.sha256(canonical(payload)).hexdigest()
        if digest != fixture.get("fixture_digest"):
            fail("fixture digest mismatch: " + str(fid))
        fixture_by_id[fid] = fixture
    for exp in expected_cases:
        if exp["fixture_id"] not in fixture_by_id:
            fail(exp["case_id"] + " fixture missing from manifest")
        if fixture_by_id[exp["fixture_id"]]["fixture_digest"] != exp["fixture_digest"]:
            fail(exp["case_id"] + " fixture digest differs from manifest")

    precedence_oracle = load(root / "u05-d03-precedence-expectations.json")
    oracle_pairs = precedence_oracle.get("pairs", [])
    if len(oracle_pairs) != 28:
        fail("precedence oracle must contain 28 theoretical pairs")
    expected_pair_ids = {"U05-PM-%03d" % n for n in range(1, 29)}
    if {p.get("subcase_id") for p in oracle_pairs} != expected_pair_ids:
        fail("precedence oracle ID set mismatch")
    precedence_evidence_bundle = load(root / "u05-d03-precedence-evidence.json")
    pair_evidence = precedence_evidence_bundle.get("pairs", [])
    if len(pair_evidence) != 28:
        fail("precedence evidence must contain 28 pairs")
    evidence_by_id = {p.get("subcase_id"): p for p in pair_evidence}
    for oracle in oracle_pairs:
        sid = oracle["subcase_id"]
        ev = evidence_by_id.get(sid)
        if ev is None:
            fail("missing precedence evidence " + sid)
        if ev.get("constructibility") != oracle.get("constructibility"):
            fail(sid + " constructibility mismatch")
        if not oracle.get("constructibility_authority_refs") or not oracle.get("rationale"):
            fail(sid + " missing authority/rationale")
        if oracle.get("constructibility") == "CONSTRUCTIBLE":
            if not oracle.get("fixture_id") or not oracle.get("fixture_digest"):
                fail(sid + " constructible pair lacks fixture")
            if ev.get("observed_precedence") != oracle.get("expected_higher_outcome"):
                fail(sid + " higher precedence was not observed")
        else:
            if ev.get("observed_precedence") is not None:
                fail(sid + " NOT_CONSTRUCTIBLE pair must not execute runtime profile")
        if ev.get("pass") is not True:
            fail(sid + " precedence evidence pass=false")

    oracle_special = precedence_oracle.get("special_proofs", [])
    observed_special = precedence_evidence_bundle.get("special_proofs", [])
    if len(oracle_special) != 4 or len(observed_special) != 4:
        fail("D03 special-proof set must contain exactly four records")
    oracle_special_by_id = {p.get("id"): p for p in oracle_special}
    observed_special_by_id = {p.get("id"): p for p in observed_special}
    if set(oracle_special_by_id) != set(observed_special_by_id):
        fail("D03 special-proof identity mismatch")
    for proof_id, oracle in oracle_special_by_id.items():
        observed = observed_special_by_id[proof_id]
        if observed.get("expected") != oracle.get("expected"):
            fail(proof_id + " special-proof expected value mismatch")
        if observed.get("observed") != oracle.get("expected"):
            fail(proof_id + " special-proof observed value mismatch")
        if observed.get("authority_refs") != oracle.get("authority_refs"):
            fail(proof_id + " special-proof authority refs mismatch")
        if observed.get("proof_source") != "RUNTIME_OR_CONTRACT_PREDICATE":
            fail(proof_id + " special-proof source is not runtime/contract-derived")
        if proof_id == "POL-005-POL-011-MUTUAL-EXCLUSION":
            if observed.get("pol005_observed_rule") != "D03-POL-005":
                fail(proof_id + " POL-005 positive branch not observed")
            if observed.get("pol011_observed_rule") != "D03-POL-011":
                fail(proof_id + " POL-011 positive branch not observed")
            if observed.get("contract_predicate") is not True:
                fail(proof_id + " mutual-exclusion contract predicate not proven")
        if proof_id == "POST-DDX-POL-011-EXCLUSION":
            if observed.get("observed_runtime_rule") == "D03-POL-011":
                fail(proof_id + " runtime incorrectly applied POL-011")
            if observed.get("observed_runtime_rule") != "D03-POL-006":
                fail(proof_id + " expected governed POST-DDX rule not observed")
        if not observed.get("rationale") or observed.get("pass") is not True:
            fail(proof_id + " special-proof failed")

    harness = load(root / "u05-harness-self-test-evidence.json")
    if harness.get("harness_id") != "U05-HG-001":
        fail("HG-001 identity mismatch")
    if harness.get("detector") != "FIRED" or harness.get("verification_path") != "FAIL_CLOSED":
        fail("HG-001 did not prove fail-closed expectation gap")
    if harness.get("business_expected_result_invented") is not False or harness.get("pass") is not True:
        fail("HG-001 semantics invalid")

    vg = load(root / "u05-verification-gate-evidence.json").get("gates", [])
    if len(vg) != 6:
        fail("verification gate evidence must contain six records")
    if {g.get("gate_id") for g in vg} != {"U05-VG-%03d" % n for n in range(1, 7)}:
        fail("verification gate ID set mismatch")
    if not all(g.get("pass") is True for g in vg):
        fail("one or more verification gates failed")

    regression = load(root / "regression-summary.json")
    if regression.get("pass") is not True:
        fail("regression summary pass=false")
    if int(regression.get("failures", -1)) != 0 or int(regression.get("errors", -1)) != 0:
        fail("regression has failure/error")
    if int(regression.get("unexpected_skips", -1)) != 0:
        fail("regression has unexpected skips")

    focused = top.get("focused_tests", {})
    if int(focused.get("tests", 0)) < 60:
        fail("focused verifier did not execute at least 60 test methods")
    if any(int(focused.get(k, -1)) != 0 for k in ("failures", "errors", "skipped")):
        fail("focused verifier test summary is not all green")
    harness_tests = top.get("evidence_harness_tests", {})
    if int(harness_tests.get("tests", 0)) < 2:
        fail("evidence harness suite incomplete")
    if any(int(harness_tests.get(k, -1)) != 0 for k in ("failures", "errors", "skipped")):
        fail("evidence harness tests are not all green")

    print("U05_RDP06_EVIDENCE_VALIDATION=PASS")
    print("U05_RDP06_VERDICT=PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW")

if __name__ == "__main__":
    main()
