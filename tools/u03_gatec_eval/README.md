# U03 Gate C evaluation-only harness

This directory implements the narrow authorization `AUTH-U03-GATEC-EVAL-IMPL-001`.

It is intentionally isolated from `diagnosis-service` runtime code. It has no network, database, production release-registry, Clinical State commit, U04, or U14 integration.

## Bound governed objects

- `ER-U03-RISK-001@0.1.0-candidate`
- `KR-U03-SOURCE-001@0.1.0-candidate`
- `RR-U03-RISK-001@0.2.1-candidate`
- `U03_D09_COVERAGE_V0_2_1_CANDIDATE`
- `PR-U03-D09-001@0.2.1-candidate`
- `PF-U03-C-POLICY-001`

## Run

```bash
cd tools/u03_gatec_eval
python3 -m unittest -v
python3 run_evaluation.py
```

The second command writes `build/u03-gatec-eval/result-bundle.json`. Any Golden Case failure or any critical Safety Suite failure exits non-zero.

The harness is evaluation evidence only. It does not authorize or implement runtime/production wiring.
