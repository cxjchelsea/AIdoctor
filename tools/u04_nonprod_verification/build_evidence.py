#!/usr/bin/env python3
import argparse
import json
import xml.etree.ElementTree as ET
from pathlib import Path


REQUIRED_CASE_FIELDS = {
    "case_id",
    "scenario",
    "source_clinical_state_version",
    "current_clinical_state_version",
    "input_identity",
    "u03_execution_status",
    "u03_decision_status",
    "policy_ref",
    "correlation_id",
    "trace_id",
    "expected_boundary",
    "observed_boundary",
    "expected_result",
    "observed_result",
    "observed_gate",
    "typed_failure_reason",
    "proposal_id",
    "commit_status",
    "committed_version",
    "audit_id",
    "routing_eligibility",
    "pass",
}


def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument("--implementation-sha", required=True)
    p.add_argument("--run-id", required=True)
    p.add_argument("--run-attempt", required=True)
    p.add_argument("--repository", required=True)
    p.add_argument("--ref", required=True)
    p.add_argument("--report", required=True)
    p.add_argument("--harness-report", required=True)
    p.add_argument("--case-evidence", required=True)
    p.add_argument("--output", required=True)
    return p.parse_args()


def read_junit(path):
    root = ET.parse(path).getroot()
    tests = int(root.attrib.get("tests", "0"))
    failures = int(root.attrib.get("failures", "0"))
    errors = int(root.attrib.get("errors", "0"))
    skipped = int(root.attrib.get("skipped", "0"))
    cases = []
    for case in root.findall("testcase"):
        failure = case.find("failure")
        error = case.find("error")
        skipped_node = case.find("skipped")
        status = "PASS"
        if failure is not None:
            status = "FAIL"
        elif error is not None:
            status = "ERROR"
        elif skipped_node is not None:
            status = "SKIPPED"
        cases.append({"name": case.attrib.get("name"), "status": status})
    return {
        "tests": tests,
        "failures": failures,
        "errors": errors,
        "skipped": skipped,
        "cases": cases,
    }


def validate_case_evidence(case_doc):
    assert case_doc.get("schema") == "U04_CASE_EVIDENCE_V0_1"
    cases = case_doc.get("cases")
    assert isinstance(cases, list)
    assert len(cases) >= 12

    ids = set()
    for item in cases:
        assert REQUIRED_CASE_FIELDS.issubset(item.keys()), sorted(
            REQUIRED_CASE_FIELDS.difference(item.keys())
        )
        assert item["case_id"] not in ids
        ids.add(item["case_id"])
        assert item["pass"] is True
        assert item["expected_boundary"] == item["observed_boundary"]
        assert item["expected_result"] == item["observed_result"]
        assert item["input_identity"]
        assert item["policy_ref"] == "U04-SAFETY-GATE-POLICY-V0.1-FROZEN"
        assert item["correlation_id"]
        assert item["trace_id"]

        routing = item["routing_eligibility"]
        assert set(routing.keys()) == {"u05", "restricted_context", "u11", "u14"}

        if item["observed_boundary"] == "GATE_COMMITTED":
            assert item["observed_gate"] in {
                "ALLOW", "RESTRICTED", "BLOCKED", "UNAVAILABLE"
            }
            assert item["typed_failure_reason"] is None
            assert item["proposal_id"]
            assert item["commit_status"] == "COMMITTED"
            assert isinstance(item["committed_version"], int)
            assert item["audit_id"]
            assert all(isinstance(routing[k], bool) for k in routing)
        else:
            assert item["observed_boundary"] == "U04_CONSUMER_ADMISSION"
            assert item["observed_gate"] is None
            assert item["typed_failure_reason"]
            assert item["proposal_id"] is None
            assert item["commit_status"] is None
            assert item["committed_version"] is None
            assert item["audit_id"] is None
            assert all(routing[k] is None for k in routing)

    return cases


def main():
    args = parse_args()
    focused = read_junit(Path(args.report))
    harness = read_junit(Path(args.harness_report))
    case_doc = json.loads(Path(args.case_evidence).read_text(encoding="utf-8"))
    structured_cases = validate_case_evidence(case_doc)

    data = {
        "schema": "U04_NONPROD_VERIFICATION_EVIDENCE_V0_2",
        "implementation_sha": args.implementation_sha,
        "workflow": {
            "repository": args.repository,
            "ref": args.ref,
            "run_id": args.run_id,
            "run_attempt": args.run_attempt,
        },
        "frozen_policy": {
            "policy_ref": "U04-SAFETY-GATE-POLICY-V0.1-FROZEN",
            "no_high_risk_signal": "ALLOW_WITH_FROZEN_PRECONDITIONS",
            "caution": "RESTRICTED",
            "high_risk": "BLOCKED",
            "u03_failed": "UNAVAILABLE",
            "scope_unavailable": "UNAVAILABLE",
        },
        "dependency_policy": {
            "additional_required_safety_capability": False,
            "optional_safety_capability": False,
            "safety_capability_fallback": False,
        },
        "hard_boundaries": {
            "live_u05_execution": False,
            "live_u11_execution": False,
            "live_u14_execution": False,
            "production_routing": False,
            "production_mutation": False,
            "real_patient_traffic": False,
        },
        "focused_tests": focused,
        "evidence_harness_tests": harness,
        "structured_case_evidence": {
            "schema": case_doc["schema"],
            "case_count": len(structured_cases),
            "cases": structured_cases,
        },
        "verification_verdict": (
            "PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW"
            if focused["failures"] == 0
            and focused["errors"] == 0
            and focused["skipped"] == 0
            and harness["failures"] == 0
            and harness["errors"] == 0
            and harness["skipped"] == 0
            and all(x["pass"] is True for x in structured_cases)
            else "FAIL"
        ),
    }
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(data, indent=2, sort_keys=True) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
