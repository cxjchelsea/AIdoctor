#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GATEC = ROOT / "tools" / "u03_gatec_eval"
sys.path.insert(0, str(GATEC))

from cases import golden_cases, safety_cases, excluded_unproducible_cases  # type: ignore  # noqa: E402
from run_evaluation import safety_check  # type: ignore  # noqa: E402


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    golden = golden_cases()
    safety_specs = safety_cases()
    safety = []
    for spec in safety_specs:
        expected = safety_check(spec["id"], spec["scenario"])
        safety.append({
            "case_id": spec["id"],
            "scenario": spec["scenario"],
            "frozen_gatec_result": expected,
        })

    payload = {
        "schema": "U03_CD08_FROZEN_CASE_EXPORT_V1",
        "source": {
            "cases_py": {
                "path": "tools/u03_gatec_eval/cases.py",
                "sha256": sha256(GATEC / "cases.py"),
            },
            "evaluator_py": {
                "path": "tools/u03_gatec_eval/evaluator.py",
                "sha256": sha256(GATEC / "evaluator.py"),
            },
            "run_evaluation_py": {
                "path": "tools/u03_gatec_eval/run_evaluation.py",
                "sha256": sha256(GATEC / "run_evaluation.py"),
            },
        },
        "golden": golden,
        "safety": safety,
        "excluded": excluded_unproducible_cases(),
        "counts": {
            "golden": len(golden),
            "safety": len(safety),
            "excluded": len(excluded_unproducible_cases()),
        },
    }

    if payload["counts"] != {"golden": 30, "safety": 19, "excluded": 2}:
        raise SystemExit("frozen CD-08 population count mismatch")

    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(payload, indent=2, sort_keys=True), encoding="utf-8")
    print(out)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
