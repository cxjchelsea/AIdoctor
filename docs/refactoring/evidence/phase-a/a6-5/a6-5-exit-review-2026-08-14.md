# A6.5 Legacy Governance Exit Review — 2026-08-14

> Decision: `A6_5_EXIT_ASSESSMENT_PASS_LEGACY_GOVERNANCE_ONLY`
>
> A6.5: `LEGACY_GOVERNANCE_CLOSED`
>
> A6.5 Exit: `PASS_LEGACY_GOVERNANCE_ONLY`
>
> Exact Enterprise Base at record time: `16fe716ebdee82a3b8cf00bfcde1a7f99ae68d2d`
>
> This record persists an already-assessed Exit. It does not re-implement A6.5.
> [a6-5-exit-gate-check.md](../../../plans/phase-a/a6-5/a6-5-exit-gate-check.md)
> remains the E02 **pre-Exit check** artifact and is not rewritten as if it
> had already been the Exit decision.

## 1. Exact Base / provenance

```text
Enterprise branch: agent/enterprise-agent-refactoring-plan
Enterprise HEAD:   16fe716ebdee82a3b8cf00bfcde1a7f99ae68d2d
```

| Item | Provenance |
|---|---|
| Non-Adoption `A6.5-NONADOPT-001` | PR #38 MERGED `6ee86cb001aceb8a6cf264c9a6fbd629dddd8dda` |
| TASK-E01 | PR #39 MERGED `3bb48dc538ac83325d7cdd6f460c9da9b0e93bb9` |
| E01 reviewed Head | `fd3fa8e2e11c63315d939b99b1a4b00e476cdccb` |
| PR #39 parents | P1 `6ee86cb…` / P2 `fd3fa8e…` |
| TASK-E02 | PR #40 MERGED `16fe716ebdee82a3b8cf00bfcde1a7f99ae68d2d` |
| E02 reviewed Head | `989673373dcf31b66d4b931641dc0d60d0eec552` |
| PR #40 parents | P1 `3bb48dc…` / P2 `9896733…` |
| E01 board | 153 rows |
| E02 write-back | 168 rows (153 board ∪ 15 durable extras) |

## 2. Exit evidence checklist (15 non-adoption conditions)

| # | Condition | Result | Evidence pointer |
|---|---|---|---|
| 1 | exact legacy inventory | **PASS** | [a6-5-legacy-asset-inventory.csv](../../../plans/phase-a/a6-5/a6-5-legacy-asset-inventory.csv) (203) |
| 2 | per-asset disposition | **PASS** | [a6-5-e01-disposition-board.csv](../../../plans/phase-a/a6-5/a6-5-e01-disposition-board.csv) + E02 extras |
| 3 | runtime consumer check | **PASS** | E01 board `runtime_consumer_status` (metadata only) |
| 4 | Owner decision provenance | **PASS** | [non-adoption strategy](../../../plans/phase-a/a6-5/a6-5-legacy-clinical-non-adoption-strategy.md) |
| 5 | no Capability authority binding | **PASS** | E01/E02 labels; A6 approved rules = 0 |
| 6 | no Prompt Release | **PASS** | no Prompt body copied; no release |
| 7 | clinical content copied = 0 | **PASS** | E01/E02 docs-only diffs |
| 8 | threshold copied = 0 | **PASS** | E01/E02 docs-only diffs |
| 9 | clinical gold copied = 0 | **PASS** | no gold claimed; D01 gap only |
| 10 | PHI copied = 0 | **PASS** | path-level quarantine retained |
| 11 | approved medical sources inherited = 0 | **PASS** | DATA-K* remain `NOT_APPROVED_MEDICAL_SOURCE` |
| 12 | Migration Matrix write-back | **PASS** | [migration matrix §20](../../../代码、数据与设计资产迁移矩阵.md) + [write-back CSV](../../../plans/phase-a/a6-5/a6-5-e02-matrix-writeback.csv) |
| 13 | Coverage Matrix write-back | **PASS** | [coverage matrix §20](../../../目标能力与旧设计资产覆盖矩阵.md) + write-back CSV |
| 14 | Decommission status recorded | **PASS** | [legacy-asset-decommission-register.csv](../../../plans/phase-a/a6-5/legacy-asset-decommission-register.csv); eligibility now = 0 |
| 15 | physical deletion = 0 | **PASS** | all register rows `PRESERVED` |

```text
PASS: 15
FAIL: 0
UNKNOWN: 0
```

## 3. Explicit zero assertions

```text
Physical deletion: 0
Clinical authority migration / ADOPT / MAP-as-authority: 0
PHI copied: 0
Clinical Prompt copied: 0
Clinical Threshold copied: 0
Clinical Gold copied: 0
Provider / runtime activation: 0
```

## 4. Limitations

- Consumer status is inventory/metadata, not live traffic proof.
- D01 remains synthetic-only planning / evaluation-gap inventory. No real regression suite, fixtures executed, or clinical gold.
- B02 remains fail-closed provenance/license/privacy evidence. No legal, clinical, or product-use approval.
- Physical decommission remains `NOT_AUTHORIZED`.
- Same-session E02 IR/MR was accepted by Owner sequencing; merge parents and trees were independently re-verified on GitHub.

## 5. Non-claims

This Exit **MUST NOT** be read as:

```text
A6 COMPLETE / A6 APPROVED / adult_respiratory_v1 ACTIVE
A7 COMPLETE / A7-CL READY / A7-CL AUTHORIZED
Clinical Runtime ENABLED / Production READY
Frozen Baseline / A11 eligible / A11 PASS
PHASE_A_NC_CLOSURE COMPLETE
NC-CLOSE-01 AUTHORIZED
A8–A11 AUTHORIZED
Phase B AUTHORIZED
Future New Clinical Track AUTHORIZED
```

## 6. Resulting control state

```text
E01: MERGED_AND_VERIFIED
E02: MERGED_AND_VERIFIED
A6.5: LEGACY_GOVERNANCE_CLOSED
A6.5 Exit: PASS_LEGACY_GOVERNANCE_ONLY

A6: DRAFT / PARTIALLY_VALIDATED / REQUIRES_CLINICAL_REVIEW
    / NOT_IMPLEMENTED / Production BLOCKED
A7-NC: COMPLETE
A7-CL: BLOCKED
A7: NOT_COMPLETE
PHASE_A_NC_CLOSURE roadmap: MERGED_AND_VERIFIED
PHASE_A_NC_CLOSURE implementation: NOT_AUTHORIZED
NC-CLOSE-01: NOT_AUTHORIZED
A8–A11: NOT_AUTHORIZED
Clinical Runtime: NOT_ENABLED
Production: BLOCKED
Phase B: NOT_AUTHORIZED
Future New Clinical: NOT_AUTHORIZED
```

`FUTURE_REBUILD_REQUIRED` does not authorize starting new clinical content now.

## 7. Next permitted gate

```text
NC-CLOSE-01 Authorization / ADR Foundation planning
```

Not: `NC-CLOSE-01 IMPLEMENTATION AUTHORIZED`.
Not: A7-CL. Not: Phase B.
