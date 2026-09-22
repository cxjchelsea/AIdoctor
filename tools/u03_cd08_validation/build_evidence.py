#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any, Dict


def digest(path: Path) -> str | None:
    if not path.exists():
        return None
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def read_json(path: Path) -> Dict[str, Any] | None:
    if not path.exists():
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--implementation-sha", required=True)
    p.add_argument("--workflow-run-id", required=True)
    p.add_argument("--run-attempt", required=True)
    p.add_argument("--repository", required=True)
    p.add_argument("--ref", required=True)
    p.add_argument("--event-name", required=True)
    p.add_argument("--runtime-outcome", required=True)
    p.add_argument("--structural-outcome", required=True)
    p.add_argument("--regression-outcome", required=True)
    p.add_argument("--compile-outcome", required=True)
    p.add_argument("--frozen-cases", required=True)
    p.add_argument("--runtime-report", required=True)
    p.add_argument("--gatec-bundle", required=True)
    p.add_argument("--output", required=True)
    args = p.parse_args()

    frozen = Path(args.frozen_cases)
    runtime = Path(args.runtime_report)
    gatec = Path(args.gatec_bundle)

    runtime_data = read_json(runtime)
    gatec_data = read_json(gatec)

    result: Dict[str, Any] = {
        "schema": "U03_CD08_EXECUTION_EVIDENCE_V1",
        "authorization": {
            "id": "AUTH-U03-CD08-CLINICAL-VALIDATION-EXEC-001",
            "state": "AUTHORIZED",
            "scope": "NON_PRODUCTION_POST_IMPLEMENTATION_CLINICAL_VALIDATION_ONLY",
        },
        "execution_identity": {
            "implementation_sha": args.implementation_sha,
            "workflow_run_id": args.workflow_run_id,
            "run_attempt": args.run_attempt,
            "repository": args.repository,
            "ref": args.ref,
            "event_name": args.event_name,
        },
        "hard_boundaries": {
            "production_mutation": False,
            "real_patient_traffic": False,
            "u04_execution": False,
            "u14_execution": False,
            "release_activation": False,
        },
        "step_outcomes": {
            "compile": args.compile_outcome,
            "runtime_validation": args.runtime_outcome,
            "structural_regression": args.structural_outcome,
            "full_regression": args.regression_outcome,
        },
        "frozen_case_export": {
            "path": str(frozen),
            "sha256": digest(frozen),
            "payload": read_json(frozen),
        },
        "runtime_validation": {
            "path": str(runtime),
            "sha256": digest(runtime),
            "payload": runtime_data,
        },
        "gate_c_regression": {
            "path": str(gatec),
            "sha256": digest(gatec),
            "payload": gatec_data,
        },
        "interpretation": {
            "runtime_pass_does_not_equal_cd08_pass_without_independent_review": True,
            "critical_safety_failure_is_blocking": True,
            "expected_semantics_may_not_be_modified_to_close_mismatch": True,
            "u04_remains_unauthorized": True,
            "production_remains_blocked": True,
        },
    }

    if runtime_data is None:
        result["execution_verdict"] = "FAILED_NO_RUNTIME_REPORT"
    elif runtime_data.get("failed_count", 0) != 0:
        result["execution_verdict"] = "CLINICAL_VALIDATION_MISMATCH"
    elif args.runtime_outcome != "success":
        result["execution_verdict"] = "FAILED_RUNTIME_TEST_STEP"
    elif args.structural_outcome != "success" or args.regression_outcome != "success":
        result["execution_verdict"] = "FAILED_ENGINEERING_REGRESSION"
    else:
        result["execution_verdict"] = "EXECUTION_PASS_PENDING_INDEPENDENT_CLINICAL_GOVERNANCE_REVIEW"

    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(result, indent=2, sort_keys=True), encoding="utf-8")
    print(result["execution_verdict"])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
