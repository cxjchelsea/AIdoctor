#!/usr/bin/env python3
"""Evidence admission for V1 *live* clinical E2E; simulated unit tests never qualify."""
import argparse
import json
from pathlib import Path

EXPECTED = {"E2E-%02d" % i for i in range(1, 11)}
REQUIRED_EVIDENCE = {"http_request_response", "governed_runtime_routing_trace",
                     "mysql_cdp_state_readback", "clinical_safety_evaluation",
                     "trace_and_version_bindings"}

def evaluate(payload):
    cases = payload.get("tests", []) if isinstance(payload, dict) else []
    ids = [case.get("case_id") for case in cases if isinstance(case, dict)]
    if set(ids) != EXPECTED or len(ids) != len(EXPECTED):
        return {"decision": "NOT_ACCEPTED", "reason": "INVALID_CASE_MANIFEST",
                "actual_full_e2e_cases_passed": 0}
    verified = 0
    for case in cases:
        # Never trust an unbound PASS field or fabricated fixture as runtime proof.
        # Actual traces from an independent real-HTTP/MySQL execution must be
        # assessed through a separate verifier before adding a passing case.
        if case.get("evidence_status") == "NOT_EXECUTED":
            continue
        # Deliberately closed to unsigned/unverified runtime assertions.
        continue
    return {"decision": "NOT_ACCEPTED",
            "reason": "NO_LIVE_HTTP_DB_CLINICAL_EVIDENCE",
            "actual_full_e2e_cases_passed": verified,
            "required_scenarios": len(EXPECTED),
            "missing": sorted(EXPECTED)}

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--scenarios", type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(evaluate(json.loads(args.scenarios.read_text(encoding="utf-8"))),
                     indent=2, ensure_ascii=False))
