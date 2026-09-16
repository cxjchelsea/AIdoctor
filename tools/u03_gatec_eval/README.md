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

## Independent-review remediation

BF-IR-01 removed the `forced_rule_results` bypass. Valid C execution always comes from `evaluate_rules()` and returns the full 15-rule set on non-P0 paths.

`GC-026` and `SS-012` are retained as governed identities but classified `UNPRODUCIBLE_UNDER_SHARED_SCOPE` and excluded from executable GC/SS counts. The frozen C semantics require one shared scope outcome across the dyspnoea family and one shared scope outcome across the sepsis family, so a family-level `SCOPE_MISMATCH + MATCHED` result cannot be produced by a valid C execution. P5 remains covered only as an explicitly labeled D09 defensive contract-boundary check and is not reported as governed C output.

BF-IR-02 splits the three P4 paths into distinct coverage states:

- GC-012: both conditional families `NOT_APPLICABLE`
- GC-014: dyspnoea family applicable/all `NO_MATCH`; sepsis family `NOT_APPLICABLE`
- GC-015: sepsis family applicable/all `NO_MATCH`; dyspnoea family `NOT_APPLICABLE`

SS-014 / SS-016 also execute a structural side-effect boundary check rather than relying only on dataclass defaults.

Current executable counts after this review correction are 30 Golden Cases and 19 critical Safety Suite cases, with the two excluded identities recorded separately in the result bundle. This is implementation-verification structure only; it does not mean Governed Evaluation Execution has started.

## Run

```bash
cd tools/u03_gatec_eval
python3 -m unittest -v
python3 run_evaluation.py
```

The second command writes `build/u03-gatec-eval/result-bundle.json`.

The result bundle records:

- executable GC / Safety results;
- excluded unproducible governed identities;
- shared-scope invariant evidence;
- D09 P5 defensive contract-boundary evidence;
- `Governed Evaluation Execution = NOT_STARTED_PENDING_TARGETED_RE_REVIEW`;
- `Gate C = NOT_PASSED`.

The harness is evaluation-only implementation evidence. It does not authorize runtime/production wiring or Gate C PASS.
