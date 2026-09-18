#!/usr/bin/env python3
import argparse
import json
import xml.etree.ElementTree as ET
from pathlib import Path

def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument("--implementation-sha", required=True)
    p.add_argument("--run-id", required=True)
    p.add_argument("--run-attempt", required=True)
    p.add_argument("--repository", required=True)
    p.add_argument("--ref", required=True)
    p.add_argument("--report", required=True)
    p.add_argument("--output", required=True)
    return p.parse_args()

def main():
    args = parse_args()
    report = Path(args.report)
    root = ET.parse(report).getroot()
    tests = int(root.attrib.get("tests", "0"))
    failures = int(root.attrib.get("failures", "0"))
    errors = int(root.attrib.get("errors", "0"))
    skipped = int(root.attrib.get("skipped", "0"))

    case_results = []
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
        case_results.append({
            "name": case.attrib.get("name"),
            "status": status,
        })

    data = {
        "schema": "U04_NONPROD_VERIFICATION_EVIDENCE_V0_1",
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
        "focused_tests": {
            "tests": tests,
            "failures": failures,
            "errors": errors,
            "skipped": skipped,
            "cases": case_results,
        },
        "verification_verdict": (
            "PASS_PENDING_INDEPENDENT_REVIEW"
            if failures == 0 and errors == 0 and skipped == 0
            else "FAIL"
        ),
    }
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(data, indent=2, sort_keys=True) + "\n", encoding="utf-8")

if __name__ == "__main__":
    main()
