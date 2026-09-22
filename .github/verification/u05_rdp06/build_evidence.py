#!/usr/bin/env python3
import argparse
import hashlib
import json
import pathlib
import shutil
import xml.etree.ElementTree as ET

TARGET = "261ee5525c8260e93db19173ffbde89a8af6810d"
CASE_SCHEMA = "U05_CASE_EVIDENCE_V0_1"
BUNDLE_SCHEMA = "U05_NONPROD_VERIFICATION_EVIDENCE_V0_1"

def canonical(value):
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")

def load(path):
    return json.loads(pathlib.Path(path).read_text(encoding="utf-8"))

def write(path, value):
    p = pathlib.Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_bytes(canonical(value))

def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

def junit_summary(paths):
    tests = failures = errors = skipped = 0
    for path in paths:
        root = ET.parse(path).getroot()
        tests += int(root.attrib.get("tests", 0))
        failures += int(root.attrib.get("failures", 0))
        errors += int(root.attrib.get("errors", 0))
        skipped += int(root.attrib.get("skipped", 0))
    return {"tests": tests, "failures": failures, "errors": errors, "skipped": skipped}

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence-root", required=True)
    parser.add_argument("--expectations", required=True)
    parser.add_argument("--precedence-expectations", required=True)
    parser.add_argument("--fixtures", required=True)
    parser.add_argument("--auth-profile", required=True)
    parser.add_argument("--contract-manifest", required=True)
    parser.add_argument("--structural-summary", required=True)
    parser.add_argument("--regression-summary", required=True)
    parser.add_argument("--workflow-provenance", required=True)
    parser.add_argument("--focused-reports-dir", required=True)
    parser.add_argument("--harness-report", required=True)
    parser.add_argument("--output-dir", required=True)
    args = parser.parse_args()

    evidence_root = pathlib.Path(args.evidence_root)
    out = pathlib.Path(args.output_dir)
    out.mkdir(parents=True, exist_ok=True)

    expectations = load(args.expectations)
    precedence_oracle = load(args.precedence_expectations)
    fixtures = load(args.fixtures)
    auth = load(args.auth_profile)
    contract = load(args.contract_manifest)
    structural = load(args.structural_summary)
    regression = load(args.regression_summary)

    case_files = sorted((evidence_root / "cases").glob("U05-EV-*.json"))
    if len(case_files) != 60:
        raise SystemExit("expected exactly 60 case evidence files, got %d" % len(case_files))
    cases = [load(p) for p in case_files]
    if any(c.get("schema") != CASE_SCHEMA for c in cases):
        raise SystemExit("case evidence schema mismatch")

    precedence_files = sorted((evidence_root / "precedence").glob("U05-PM-*.json"))
    if len(precedence_files) != 28:
        raise SystemExit("expected exactly 28 precedence evidence files, got %d" % len(precedence_files))
    precedence = [load(p) for p in precedence_files]

    special_path = evidence_root / "u05-d03-special-proof-evidence.json"
    if not special_path.exists():
        raise SystemExit("missing D03 special-proof evidence")
    special_proofs = load(special_path).get("proofs", [])
    if len(special_proofs) != 4:
        raise SystemExit("expected exactly 4 D03 special proofs")

    harness_path = evidence_root / "u05-harness-self-test-evidence.json"
    if not harness_path.exists():
        raise SystemExit("missing harness self-test evidence")
    harness = load(harness_path)

    focused_reports = sorted(pathlib.Path(args.focused_reports_dir).glob(
        "TEST-com.aidoctor.diagnosis.runtime.u05.verification.*VerificationTest.xml"))
    if not focused_reports:
        raise SystemExit("missing focused verification surefire reports")
    focused_summary = junit_summary([
        p for p in focused_reports
        if p.name != pathlib.Path(args.harness_report).name
    ])
    harness_summary = junit_summary([args.harness_report])

    hard_boundaries = {
        "live_u06_execution": bool(auth.get("live_u06_execution")),
        "live_u08_execution": bool(auth.get("live_u08_execution")),
        "live_u10_execution": bool(auth.get("live_u10_execution")),
        "live_u11_execution": bool(auth.get("live_u11_execution")),
        "live_u14_execution": bool(auth.get("live_u14_execution")),
        "production_routing": bool(auth.get("production_routing")),
        "production_mutation": bool(auth.get("production_mutation")),
        "real_patient_traffic": bool(auth.get("real_patient_traffic")),
        "external_delivery_side_effects": bool(auth.get("external_delivery_side_effects")),
        "unauthorized_model_tool_calls": bool(auth.get("unauthorized_model_tool_calls")),
        "shared_runtime_source_change": bool(auth.get("shared_runtime_source_change")),
    }

    vg = [
        {"gate_id": "U05-VG-001", "name": "no direct live downstream Unit invocation",
         "pass": bool(structural.get("no_live_downstream_execution"))},
        {"gate_id": "U05-VG-002", "name": "no automatic production/Spring activation",
         "pass": bool(structural.get("no_automatic_production_activation"))},
        {"gate_id": "U05-VG-003", "name": "no model/tool/external-service dependency",
         "pass": bool(structural.get("no_external_model_tool_dependency"))},
        {"gate_id": "U05-VG-004", "name": "no direct Clinical State write bypassing K09/P01",
         "pass": bool(structural.get("no_state_write_bypass"))},
        {"gate_id": "U05-VG-005", "name": "synthetic/non-PHI fixture/evidence guard",
         "pass": fixtures.get("synthetic") is True and fixtures.get("contains_real_phi") is False},
        {"gate_id": "U05-VG-006", "name": "Foundation + U01-U04 / diagnosis-service regression",
         "pass": regression.get("pass") is True and int(regression.get("unexpected_skips", -1)) == 0},
    ]

    case_bundle = {"schema": "U05_CASE_EVIDENCE_BUNDLE_V0_1", "cases": cases}
    precedence_bundle = {
        "schema": "U05_D03_PRECEDENCE_EVIDENCE_BUNDLE_V0_1",
        "pairs": precedence,
        "special_proofs": special_proofs,
    }
    gate_bundle = {"schema": "U05_VERIFICATION_GATE_EVIDENCE_V0_1", "gates": vg}

    all_cases_pass = all(c.get("pass") is True for c in cases)
    all_precedence_pass = (
        all(p.get("pass") is True for p in precedence)
        and all(p.get("pass") is True for p in special_proofs)
    )
    all_vg_pass = all(g["pass"] is True for g in vg)
    harness_pass = harness.get("pass") is True
    no_hard_boundary = all(v is False for v in hard_boundaries.values())
    focused_pass = (
        focused_summary["failures"] == 0
        and focused_summary["errors"] == 0
        and focused_summary["skipped"] == 0
    )
    harness_tests_pass = (
        harness_summary["failures"] == 0
        and harness_summary["errors"] == 0
        and harness_summary["skipped"] == 0
    )

    verdict = (
        "PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW"
        if all_cases_pass and all_precedence_pass and all_vg_pass
        and harness_pass and no_hard_boundary and focused_pass and harness_tests_pass
        else "FAIL"
    )

    top = {
        "schema": BUNDLE_SCHEMA,
        "implementation_sha": TARGET,
        "authorization_id": auth.get("authorization_id"),
        "parent_authorization_id": auth.get("parent_authorization_id"),
        "contract_manifest_digest": contract.get("authority_core_digest"),
        "focused_tests": focused_summary,
        "evidence_harness_tests": harness_summary,
        "required_counts": {
            "ev": 60,
            "precedence_pairs": 28,
            "harness": 1,
            "verification_gates": 6,
        },
        "observed_counts": {
            "ev": len(cases),
            "precedence_pairs": len(precedence),
            "harness": 1,
            "verification_gates": len(vg),
        },
        "hard_boundaries": hard_boundaries,
        "normal_policy_expectation_gap_count": sum(
            1 for c in cases if c.get("policy_expectation_gap") is True
        ),
        "verification_verdict": verdict,
        "final_u05_verification": "NOT_PASSED_PENDING_INDEPENDENT_EVIDENCE_REVIEW",
    }

    write(out / "evidence.json", top)
    write(out / "u05-case-evidence.json", case_bundle)
    write(out / "u05-harness-self-test-evidence.json", harness)
    write(out / "u05-verification-gate-evidence.json", gate_bundle)
    write(out / "u05-d03-precedence-evidence.json", precedence_bundle)

    copies = {
        pathlib.Path(args.expectations): out / "u05-verification-expectations.json",
        pathlib.Path(args.precedence_expectations): out / "u05-d03-precedence-expectations.json",
        pathlib.Path(args.fixtures): out / "u05-verification-fixtures.json",
        pathlib.Path(args.auth_profile): out / "auth-profile.json",
        pathlib.Path(args.contract_manifest): out / "u05-contract-manifest.json",
        pathlib.Path(args.structural_summary): out / "structural-guard-summary.json",
        pathlib.Path(args.regression_summary): out / "regression-summary.json",
        pathlib.Path(args.workflow_provenance): out / "workflow-provenance.txt",
    }
    for src, dst in copies.items():
        shutil.copyfile(str(src), str(dst))

    reports_out = out / "surefire-reports"
    reports_out.mkdir(exist_ok=True)
    for report in focused_reports:
        shutil.copyfile(str(report), str(reports_out / report.name))

    checksum_paths = sorted(
        [p for p in out.rglob("*") if p.is_file() and p.name != "SHA256SUMS"],
        key=lambda p: str(p.relative_to(out)),
    )
    with open(out / "SHA256SUMS", "w", encoding="utf-8", newline="\n") as handle:
        for p in checksum_paths:
            handle.write("%s  %s\n" % (sha256_file(p), p.relative_to(out).as_posix()))

    print(verdict)

if __name__ == "__main__":
    main()
