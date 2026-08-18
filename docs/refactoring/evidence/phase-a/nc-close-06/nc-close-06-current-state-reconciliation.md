# NC-CLOSE-06 Current-State Reconciliation

> Dated: 2026-08-18
>
> Classification: `DATED_CONTROL_TRUTH` / `NO HISTORICAL REWRITE`
>
> Lane: `PHASE_A_NC_CLOSURE`
>
> Scope ID: `NC-CLOSE-REC-01`
>
> Exact Enterprise Base: `f08f26a1e1187c19cad73351ebc048388d7fdbc8`
>
> Base tree: `f0493a3f1e24378d917afe7d338b4b5895b2b791`
>
> Authorization token:
> `NC_CLOSE_06_INTEGRATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Status: `RECONCILIATION_AUTHORED_PENDING_REVIEW`

This is a **new** current-truth layer. It does not replace PR #29, P7,
historical addenda, A1–A6 evidence, or old ADR authoring files.

Companion artifacts:

- [E2E design](../../plans/phase-a/nc-close-06/nc-close-06-e2e-design.md)
- [Coverage / migration map](./nc-close-06-coverage-migration-map.csv)
- [Gap ledger](./nc-close-06-gap-ledger.csv)

```text
PHASE_A_NC_CLOSURE_EXIT != A11 PASS
PHASE_A_NC_CLOSURE_EXIT != PHASE_A_FROZEN_BASELINE
```

## 1. Exact Base / tree

```text
Repository:     cxjchelsea/AIdoctor
Enterprise:     agent/enterprise-agent-refactoring-plan
HEAD:           f08f26a1e1187c19cad73351ebc048388d7fdbc8
Tree:           f0493a3f1e24378d917afe7d338b4b5895b2b791
Provenance:     PR #50 merge / NC-CLOSE-01B MERGED_AND_VERIFIED
Push CI:        32016199869 / Phase A CI MVP / success
```

## 2. Authorization token

```text
NC_CLOSE_06_INTEGRATION_EXPLICIT_AUTHORIZATION_GRANTED
Mode: DOCS / DESIGN / MAP / RECONCILIATION ONLY
implementation_authorized: false
```

Assessment that preceded this token:
`NC_CLOSE_06_INTEGRATION_AUTHORIZATION_RECOMMENDED`
(AC-01…AC-36 PASS; Blocking 0; Critical UNKNOWN 0).

`RECOMMENDED != AUTHORIZED` remains true for NC-CLOSE-07.

## 3. Scope IDs

| ID | Class | Authoring state |
|---|---|---|
| NC-CLOSE-E2E-01 | DESIGN_ONLY | `DESIGN_AUTHORED_PENDING_REVIEW` |
| NC-CLOSE-COVERAGE-01 | map_only | `MAP_AUTHORED_PENDING_REVIEW` |
| NC-CLOSE-REC-01 | dated reconciliation | `RECONCILIATION_AUTHORED_PENDING_REVIEW` |

Parent batch NC-CLOSE-06:
`INTEGRATION_ARTIFACTS_AUTHORED_PENDING_REVIEW`.

## 4. Prior-batch provenance matrix

| Batch | Merge / exit provenance | Durable state | Control state | Evidence level | Consumable by 06 | Remaining debt | NC Exit impact |
|---|---|---|---|---|---|---|---|
| 01A | PR #43 MERGED_AND_VERIFIED | ADR-02/03/04/05 decided | implementation NOT_AUTHORIZED | DOCUMENTED / CODE_CONFIRMED inputs | YES | State Committer / Runtime package / LangGraph unimplemented | no; decisions complete |
| 01B | PR #50 MERGED_AND_VERIFIED | ADR-01/06/07/08 decided | implementation NOT_AUTHORIZED | DOCUMENTED / CODE_CONFIRMED / CONFIG_VERIFIED | YES | vendor/enablement deferred; Neo4j structural risk | DEFERRED_BY_VALID_ADR |
| 01C | PR #47 MERGED_AND_VERIFIED | ADR-09/10/11/12 decided | implementation NOT_AUTHORIZED | DOCUMENTED / CODE_CONFIRMED | YES | Secret/OTel backends deferred | DEFERRED_BY_VALID_ADR |
| 02 | PR #44 MERGED_AND_VERIFIED | three-language bindings | bindings ≠ runtime migrated | TEST_VERIFIED / CI_VERIFIED schemas | YES | Feign/FastAPI/frontend parallel models | CONTRACT_SEMANTIC_DIFF_CONTROLLED_UNCHANGED |
| 03 | PR #45 MERGED_AND_VERIFIED | CI MVP | engineering baseline only | CI_VERIFIED | YES | no full runtime/E2E/deploy CI | CI MVP satisfies that Exit bullet |
| 04 | PR #48 MERGED_AND_VERIFIED | WF VERIFIED; TRACE PARTIAL | **PARTIALLY_VALIDATED** | TEST_VERIFIED characterization | YES | see §7 | UNKNOWN_REQUIRES_EXIT_REVIEW |
| 05 | PR #49 MERGED_AND_VERIFIED | ACCOUNTING_COMPLETE | ≠ ALL_RUNTIME_PASS | mixed RUNTIME_VERIFIED_LOCAL / FAIL | YES | four A2 current failures | NON_BLOCKING_DEBT |
| A6.5 | PR #38/#39/#40; Exit 2026-08-14 | LEGACY_GOVERNANCE_CLOSED | Exit PASS_LEGACY_GOVERNANCE_ONLY | DOCUMENTED Exit review | YES | B04/C02 not reopened; deletion 0 | closed for NC |
| A7-NC | P7 Exit PASSED | COMPLETE / CLOSED / STABLE | ≠ A7 COMPLETE | DOCUMENTED Exit | YES | do not reopen Fake/Gateway/containment | closed for NC |

## 5. Durable control-state table

```text
NC-CLOSE-01A ADR:                    MERGED_AND_VERIFIED
NC-CLOSE-01B ADR:                    MERGED_AND_VERIFIED
NC-CLOSE-01C ADR:                    MERGED_AND_VERIFIED
NC-CLOSE-01 ADR decision coverage:   COMPLETE
NC-CLOSE-01 implementation:          NOT_AUTHORIZED
NC-CLOSE-02:                         MERGED_AND_VERIFIED
NC-CLOSE-03:                         MERGED_AND_VERIFIED
NC-CLOSE-04 evidence:                MERGED_AND_VERIFIED
NC-CLOSE-04:                         PARTIALLY_VALIDATED
NC-CLOSE-05:                         RUNTIME_EVIDENCE_ACCOUNTING_COMPLETE
NC-CLOSE-06:                         INTEGRATION_ARTIFACTS_AUTHORED_PENDING_REVIEW
NC-CLOSE-07:                         NOT_AUTHORIZED
A6.5:                                LEGACY_GOVERNANCE_CLOSED
A7-NC:                               COMPLETE
A7-CL:                               BLOCKED
Clinical Runtime:                    NOT_ENABLED
Production:                          BLOCKED
A11:                                 NOT_PASSED
Phase B:                             NOT_AUTHORIZED
```

```text
NC-CLOSE-WF-01:     VERIFIED
NC-CLOSE-TRACE-01:  PARTIALLY_VALIDATED
```

Do not write NC-CLOSE-04 `VERIFIED` / `COMPLETE` / `SATISFIES_EXIT`.

## 6. Historical → current supersession

| Item | Provenance class | Current reading |
|---|---|---|
| phase-a-current-state-addendum.md A7-NC NOT_STARTED | `HISTORICAL_AT_THAT_BASE` | `SUPERSEDED_BY_LATER_VERIFIED_WORK` (A7-NC COMPLETE) |
| addendum A6.5 INCOMPLETE_BLOCKED_DEPENDENCY | `HISTORICAL_AT_THAT_BASE` | `SUPERSEDED_BY_LATER_VERIFIED_WORK` (LEGACY_GOVERNANCE_CLOSED) |
| addendum CI NO_CI_CONFIGURED (later post-PR40 still said this) | `HISTORICAL_AT_THAT_BASE` | `SUPERSEDED_BY_LATER_VERIFIED_WORK` (NC-CLOSE-03 CI MVP) |
| A1 diagnosis compile FAIL (44 errors) | `HISTORICAL_AT_THAT_BASE` | `SUPERSEDED_BY_LATER_VERIFIED_WORK` (repair + CI) |
| A2 historical pip/import fails superseded in NC-CLOSE-05 §3 | `HISTORICAL_AT_THAT_BASE` | current A2 fails are the four listed in §8 |
| A3 tsc/build BLOCKED | `HISTORICAL_AT_THAT_BASE` | `SUPERSEDED_BY_LATER_VERIFIED_WORK` (PR #46 + CI) |
| 01B ADR files `PROPOSED_DECIDED_PENDING_REVIEW` | `POST_MERGE_POINTER_DEBT` | durable ADR state is MERGED_AND_VERIFIED; **do not rewrite those files here** |
| Roadmap A8 “必须完成” topic list | `PREEXISTING_SUPERSEDED_ROADMAP_WORDING` | topics need disposition; concrete backend implementation is **not** required |
| NC-CLOSE-04 report `BLOCKING_FOR_NC_CLOSE_04_EXIT` | `REVIEW_SUPERSEDED_AUTHORING_SEVERITY` | see §7; **do not rewrite the report** |
| NC-CLOSE-04 index `VERIFICATION_EXECUTED_WITH_BLOCKING_FINDINGS_PENDING_REVIEW` | `HISTORICAL_AT_THAT_BASE` / authoring | durable control is PARTIALLY_VALIDATED after merge |

PR #29 body and P7 evidence meaning are **not** rewritten.

## 7. NC-CLOSE-04 special disposition

Authoring report (preserved):

- three injected tracing failure-isolation findings labeled
  `BLOCKING_FOR_NC_CLOSE_04_EXIT`;
- production remediation required: YES (separate token);
- package did not implement the fix.

Fresh independent review of PR #48 (consumed, not re-run):

- ordinary `TraceServiceClient` HTTP failures are isolated;
- injected collaborator contract-breach failures prove
  **defense-in-depth** gaps;
- those gaps are **not** proven mandatory NC-CLOSE-04 Exit
  remediation prerequisites.

06 classification:

```text
REVIEW_SUPERSEDED_AUTHORING_SEVERITY
EXIT_SEMANTICS_REQUIRE_NC_CLOSE_07_DECISION
EVIDENCE_CLOSED_RESULT_PARTIALLY_VALIDATED
```

Meaning: verification work and the evidence package are closed and
merged; the **control result** remains partial. Only NC-CLOSE-07 may
decide whether that satisfies “Workflow/Trace evidence closed”.

Durable trace debt (not repaired):

- caller defense-in-depth;
- `executeRemainingSteps` coverage;
- `errorMessage` privacy structure;
- async propagation absence;
- production occupancy UNKNOWN;
- OTel equivalence deferred.

`repair_required_now=false`. `authorization_needed_now=false`.

## 8. NC-CLOSE-05 accounting truth

```text
RUNTIME_EVIDENCE_ACCOUNTING_COMPLETE
ACCOUNTING_COMPLETE != ALL_RUNTIME_PASS
ACCOUNTING_COMPLETE != REPOSITORY_RUNTIME_VERIFIED
A11_RUNTIME_GATE_AMBIGUOUS
```

Current A2 failures (not repaired):

| Service | Current fail |
|---|---|
| workup-planner-service | missing `ServiceException` |
| diagnosis-engine-service | undeclared `jinja2` |
| dialog-service | `LegacyLLMDisabledError` / A7-NC legacy containment + eager construction |
| ocr-service | OpenCV / NumPy ABI |

NC Exit classification for all four: `NON_BLOCKING_DURABLE_DEBT`.  
A11 impact: `A11_IMPACT_UNRESOLVED_UNDER_A11_RUNTIME_GATE_AMBIGUOUS`
(`UNKNOWN_REQUIRES_A11_REVIEW`). Do not invent `BLOCKING_FOR_A11`.

## 9. NC-CLOSE-01 decision coverage

```text
NC-CLOSE-01 ADR decision coverage: COMPLETE
NC-CLOSE-01 implementation:        NOT_AUTHORIZED
```

Coverage COMPLETE means every ADR-01…12 has a valid disposition
(full decision or explicit deferral). It does **not** mean backends
are installed.

## 10. Contract semantic integrity

`contracts/v1/**` last schema-establishing work is A5. Language
bindings (PR #44 / `c681906258cd`) did not mutate semantic authority.
No later NC batch changed Shared Contracts v1 semantics.

```text
CONTRACT_SEMANTIC_DIFF_CONTROLLED_UNCHANGED
IMMUTABLE_WITHIN_LANE
```

Stop `A5_CONTRACT_SEMANTIC_CHANGE_REQUIRES_SEPARATE_REVIEW` was **not**
triggered.

## 11. Capability lifecycle

`adult_respiratory_v1` remains:

```text
DRAFT
PARTIALLY_VALIDATED
REQUIRES_CLINICAL_REVIEW
NOT_IMPLEMENTED
Production Eligibility: BLOCKED
```

NC-CLOSE-06 does not upgrade it.

## 12. Model Runtime stability

```text
A7-NC: COMPLETE / CLOSED / STABLE
A7-NC Exit: PASSED
A7-NC COMPLETE != A7 COMPLETE
```

No later NC batch modified ModelSpec, ModelRoutePolicy, Prompt Runtime,
Output Schema Registry, Gateway, ProviderAdapter, Fake, legacy
containment, or blocked-shell semantics.

Real provider remains 0.

## 13. Future Extension exclusions

`docs/refactoring/extensions/**` is `REFERENCE_ONLY`.

Not current requirements: Device Connector, Digital Pulse, Tongue
Imaging, Wearable, EHR, Structured Medical Provider, Drug Intelligence,
Regulatory Intelligence, Device Intelligence, Temporal, Special
Population, Chronic, Multimodal, `tcm_four_diagnosis_v1`, or analogous
designs.

A6.5: `LEGACY_GOVERNANCE_CLOSED`. Legacy clinical assets
`WILL_NOT_BE_MIGRATED`. Do not reopen B04 extraction or C02
rejected-asset mapping. Future clinical work is new-clinical governance.

## 14. Current remaining gaps

See [gap ledger](./nc-close-06-gap-ledger.csv). Material items are
classified; none is left as an unclassified placeholder.

Integration summary (not a fifth document):

- E2E slice is DESIGN_ONLY and current/target tagged.
- Coverage map is mapping-complete for required surfaces.
- Decommission candidates are mapped; **deletion authorized = NO**.
- 04 remains PARTIALLY_VALIDATED for 07 judgment.
- 05 four A2 fails remain durable debt.
- Preview after 06 review+merge: `LIKELY_READY_FOR_NC_CLOSE_07_AFTER_06_REVIEW_AND_MERGE`.
- This is **not** Exit PASS, NC Closure COMPLETE, or A11 PASS.

Legal future dual state after a later Exit PASS may be:

```text
PHASE_A_NC_CLOSURE = COMPLETE
A7-CL = BLOCKED
A7 = NOT_COMPLETE
FB-11 = UNSATISFIED
A11 = NOT_PASSED
Production = BLOCKED
```

## 15. NC-CLOSE-07 Handoff

NC-CLOSE-07 should start from these four artifacts plus this matrix.
It should **not** reconstruct batch history from scratch.

Point 07 to:

1. [nc-close-06-e2e-design.md](../../plans/phase-a/nc-close-06/nc-close-06-e2e-design.md)
2. [nc-close-06-coverage-migration-map.csv](./nc-close-06-coverage-migration-map.csv)
3. This reconciliation (provenance matrix in §4)
4. [nc-close-06-gap-ledger.csv](./nc-close-06-gap-ledger.csv)

### BLOCKING_FOR_NC_EXIT

```text
(none proven in this package)
```

### NON_BLOCKING_DEBT

- NC-CLOSE-04 trace implementation debt (defense-in-depth, coverage,
  privacy structure, async, occupancy UNKNOWN, OTel deferred)
- A2 workup / diagnosis-engine / dialog / OCR current failures
- Neo4j structural candidate-fusion risk
- Python Runtime package unimplemented
- contract runtime not migrated
- pointer / A8 wording metadata debt

### UNKNOWN_REQUIRES_EXIT_REVIEW

- whether PARTIALLY_VALIDATED 04 satisfies “Workflow/Trace evidence closed”

### BLOCKING_FOR_A11_NOT_NC_EXIT

- FB-11 new clinical baseline
- Full A7 incomplete
- Frozen Baseline not ready
- Capability lifecycle still DRAFT / clinical-blocked
- A7-CL blocked

### UNKNOWN_REQUIRES_A11_REVIEW

- A11 runtime-gate ambiguity for the four A2 failures
- frontend / admin / Docker full-stack beyond CI MVP
- later observability completeness

### CLINICAL_BLOCKED_OUTSIDE_NC_LANE

- A7-CL
- clinical prompts / rules / thresholds / hypotheses / gold
- `adult_respiratory_v1` clinical activation

### PHASE_B_DEFERRED

- State Committer implementation
- ClinicalDecisionRecord live store
- Safety Pack / other Phase B surfaces (mapped only)

### DEFERRED_BY_VALID_ADR

- production DB vendor
- vector backend
- BM25
- Neo4j production enablement
- Secret Manager backend
- OTel backend / AOP→OTel implementation

### FUTURE_EXTENSION_REFERENCE_ONLY

- `docs/refactoring/extensions/**` and listed future designs

Would NC-CLOSE-07 need to reconstruct history: **NO**.

## 16. Unauthorized-work check

```text
production Java / Python / tests / CI workflow / config / deps / Docker = 0
contracts/v1 semantic change = 0
database / vector / BM25 / Neo4j enablement / OTel / Secret Manager = 0
clinical / provider / PHI = 0
Future Extension implementation = 0
Phase B implementation = 0
NC-CLOSE-07 = NOT_AUTHORIZED
```

Old A8 “必须完成” wording remains historical topic inventory. Refinement
authority still governs decision depth. 06 explains this as
`PREEXISTING_SUPERSEDED_ROADMAP_WORDING` and does not rewrite the A8
list body.

README UTF-8 BOM is inherited. Pointer edits must preserve it.

## 17. Exit readiness preview

```text
LIKELY_READY_FOR_NC_CLOSE_07_AFTER_06_REVIEW_AND_MERGE
```

Not:

```text
READY_FOR_EXIT_PASS
NC_CLOSURE_COMPLETE
NC_EXIT_PASS
A11_PASS
PHASE_A_FROZEN_BASELINE
```
