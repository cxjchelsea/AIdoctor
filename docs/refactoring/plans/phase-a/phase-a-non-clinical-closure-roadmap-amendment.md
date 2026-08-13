# Phase A Non-Clinical Closure Roadmap Amendment

> Title: Phase A Non-Clinical Closure Roadmap Amendment
>
> Classification: `IMPLEMENTATION_ORDER_AMENDMENT`
>
> Lane ID: `PHASE_A_NC_CLOSURE`
>
> Status: `PLANNED_NOT_IMPLEMENTATION_AUTHORIZED`
>
> Planning state: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorized Base: `49b0e4467fb4cf96a4893f9158bf23584ab69ee5`
>
> Architecture Change: `NO`
>
> Architecture Refreeze: `NOT_REQUIRED`
>
> Clinical Runtime: `NOT_ENABLED`
>
> Production: `BLOCKED`
>
> Real Provider: `FORBIDDEN`
>
> Phase B: `NOT_AUTHORIZED`
>
> Future Extensions: `NOT_AUTHORIZED`
>
> Relationship to PR #29 / A7-NC: **independent artifact**; does not rewrite PR #29 authority

## 1. Authorization basis

This amendment is created under Repository Owner Explicit Authorization for Exact Base
`49b0e4467fb4cf96a4893f9158bf23584ab69ee5`, following:

```text
Frozen Baseline Readiness Assessment: COMPLETE
Extension Delta: EXTENSION_DELTA_NON_INTERFERING
A7-NC: COMPLETE
A7-NC Exit: PASSED
A6.5: INCOMPLETE_BLOCKED_DEPENDENCY
A7-CL: BLOCKED_BY_A6_5_CLINICAL_LANE
A7: NOT_COMPLETE
Phase A: Freeze Candidate
A8: TECHNICALLY_READY_BUT_ORDER_AMENDMENT_REQUIRED
Roadmap Amendment Authorization Assessment:
  PHASE_A_NC_CLOSURE_ROADMAP_AMENDMENT_AUTHORIZATION_RECOMMENDED
AC-PHASE-A-NC-AMEND-AUTH: 40/40 PASS
Blockers: 0
```

This document defines execution-order authority only. It does **not** authorize NC Closure
implementation batches, A8 ADR decisions, A9 CI, A10 Workflow/Trace, A5 binding generation,
A11 Freeze Review, A7-CL, or Phase B.

## 2. Historical order preservation

The historical formal Phase A sequence remains authoritative and is not rewritten:

```text
A1 → A2 → A3 → A4 → A5 → A6 → A6.5 → A7 → A8 → A9 → A10 → A11
```

This amendment is only an `IMPLEMENTATION_ORDER_AMENDMENT`. It does **not** claim:

- the historical order was wrong;
- A6.5 is complete;
- A7 is complete;
- A8 is authorized;
- A11 / Frozen Baseline is ready.

## 3. Distinction from A7-NC

| Lane | Meaning |
|---|---|
| `A7-NC` | Model Runtime-specific non-clinical parallel lane (PR #29) |
| `PHASE_A_NC_CLOSURE` | Phase A-wide Freeze-preparation non-clinical closure lane |

```text
A7-NC COMPLETE != A7 COMPLETE
PHASE_A_NC_CLOSURE_COMPLETE != PHASE_A_FROZEN_BASELINE
```

PR #29 remains historical Model Runtime order authority. This amendment does not modify
PR #29 body, historical A7-NC amendment semantics, or P7 Exit evidence.

## 4. Rationale

`A6.5` / `A7-CL` remain blocked by external clinical-governance dependency
(`B04 → C02 → E01 → E02`). Meanwhile, Phase A Frozen Baseline still has non-clinical P1 gaps
that do not require clinical approval (ADR, bindings, CI MVP, Workflow/Trace verification,
runtime-evidence accounting, E2E design, coverage/migration reconciliation).

This amendment establishes a controlled parallel execution lane so those gaps can be closed
under separate batch authorizations, without waiving clinical gates or A11 requirements.

## 5. Clinical lane (fail-closed; preserved)

```text
B04
  → C02
  → E01
  → E02
  → A6.5 Exit
  → A7-CL
  → Full A7 Exit
```

Current clinical control state:

```text
B04: BLOCKED
C02: BLOCKED_BY_TASK_B04
E01: NOT_ELIGIBLE
E02: NOT_ELIGIBLE
A6.5: INCOMPLETE_BLOCKED_DEPENDENCY
A7-CL: BLOCKED_BY_A6_5_CLINICAL_LANE
A7: NOT_COMPLETE
```

FB-20 (A6.5 Exit) and FB-21 (A7 overall) remain Freeze blockers and are **not** deleted or lowered.

## 6. NC Closure lane

```text
Roadmap Amendment
  → separately authorized NC Closure batches
  → Independent NC Closure Exit (PHASE_A_NC_CLOSURE_EXIT)
```

This must **not** be read as:

```text
Roadmap Amendment → automatic A8 implementation
```

Amendment merge (when later verified) only means execution-order authority is defined.
Every implementation batch remains `NOT_AUTHORIZED` until its own Authorization Assessment
and Explicit Authorization.

## 7. A11 rejoin model

```text
PHASE_A_NC_CLOSURE Exit
  + A6.5 Exit
  + Full A7 Exit
  + remaining A11 prerequisites
  → A11 eligibility
```

```text
A11 eligibility != A11 PASS
```

A11 still requires separate Authorization Assessment + Explicit Authorization + Independent
Freeze Review. This amendment does not authorize A11 and does not mark Frozen Baseline.

## 8. Authoritative scope register

Exact machine-readable scope is owned by:

[phase-a-non-clinical-closure-scope-register.csv](./phase-a-non-clinical-closure-scope-register.csv)

Scope IDs and batch IDs are distinct. Batch numbers do **not** themselves create hard
dependencies.

## 9. In-lane scope summary

### 9.1 A8 ADR (`IN_LANE_AUTHORITATIVE`, `implementation_authorized=false`)

Future separately authorized ADR work may cover technical decisions only:

1. Primary DB
2. Checkpoint / clinical DB boundary
3. Java/Python protocol
4. Python single Runtime package
5. LangGraph usage
6. pgvector / Milvus
7. BM25
8. Neo4j
9. Secret Manager (architecture/config mechanics only)
10. OTel Backend
11. AOP → OTel migration
12. Legacy Trace / AgentEvent Store ownership

This amendment does **not** select ADR outcomes. Especially ADR-01 must not decide the
production primary database here; future ADR batches must provide decision scope, evidence,
review requirements, and architecture stop handling
(`PHASE_A_NC_CLOSURE_ARCHITECTURE_BOUNDARY_REACHED` / Architecture Review when required).

Clinical thresholds, red-flag rules, question policy, hypotheses, medical-source approval,
clinical Prompt bodies, and route eligibility are **forbidden** in ADR outcomes under this lane.

### 9.2 A5 bindings (`IN_LANE_AUTHORITATIVE`, `implementation_authorized=false`)

Python binding, Java DTO binding, TypeScript types, generation, version synchronization,
and cross-language tests may proceed only after separate batch authorization.

Shared Contracts v1 semantics are `IMMUTABLE_WITHIN_LANE`. If a semantic change is required:

```text
A5_CONTRACT_SEMANTIC_CHANGE_REQUIRES_SEPARATE_REVIEW
```

### 9.3 A9 CI (`IN_LANE_AUTHORITATIVE` for MVP; `implementation_authorized=false`)

A9-MVP Freeze-blocking candidates (planning only; no workflow files in this PR):

- Java compile/test
- Python tests
- Frontend build/typecheck
- Contracts validation
- Capability validation
- Model Runtime tests
- Legacy tests
- Provider SDK architecture rule
- State-Committer ownership architecture rule
- secret scan
- dependency scan
- Prompt/schema validation
- clinical-block assertion

A9-FULL items such as performance, artifact publishing, deployment automation, and full lint
expansion are `DEFER_AFTER_FREEZE`. Docs-link check and migration dry-run may be
`IN_LANE_SUPPORT` when separately authorized.

Until a real workflow is merged and verified, CI truth remains `NO_CI_CONFIGURED`.
Repository settings / branch protection are separate governance actions, not implied by
this amendment.

### 9.4 A10 Fixed Workflow (`IN_LANE_AUTHORITATIVE`, `implementation_authorized=false`)

Future work may verify existing fixed Workflow runtime evidence or record explicit failure
reason, fixtures, failure/fallback behavior, double-run comparison fields, and fallback
retention. Clinical policy must not be modified.

### 9.5 A10 AOP / Trace (`IN_LANE_AUTHORITATIVE`, `implementation_authorized=false`)

Future work may verify `@TraceExecution`, `ExecutionTraceAspect`, `TraceContext`, Feign Trace,
runtime coverage, failure isolation, PHI-safe metadata structure, async/thread propagation,
OTel migration evidence, and boundaries among Technical Span, AgentEvent,
ClinicalDecisionRecord, and ComplianceAudit.

A6.5 observability / legacy mapping evidence is a **hard input dependency**. This lane must
reuse that evidence and must not redo the same inventory.

### 9.6 A1–A3 runtime evidence (`PARALLEL_BUT_PREEXISTING_AUTHORITY`)

Missing/failed/partial runtime evidence remediation already has historical A1–A3 authority.
This amendment does **not** newly authorize A1–A3. NC Closure may only coordinate closure
accounting.

### 9.7 A4

Default: `NOT_REQUIRED_IN_AMENDMENT`. If Freeze needs bounded inventory delta refresh:
`IN_LANE_SUPPORT` only. No clinical/data-source approval expansion.

### 9.8 E2E (`IN_LANE_SUPPORT`, DESIGN_ONLY)

First vertical-slice E2E is design-only: blocked clinical placeholders, mocked boundaries,
gate ordering, failure matrix, version/provenance expectations. Forbidden: run clinical E2E,
approve clinical outputs, create clinical gold.

### 9.9 Coverage / Migration / Decommission (`IN_LANE_SUPPORT`)

Allow A7-NC delta reconciliation, owner/phase/contract/completion-gate mapping, and
decommission mapping. Future Extension Design is `REFERENCE_ONLY` and must not become
current implementation requirements.

### 9.10 Current-state reconciliation (`IN_LANE_SUPPORT`)

Bounded dated reconciliation is allowed. Historical snapshots, planning-time statuses,
PR #29 history, and P7 evidence must be preserved (no rewrite of historical meaning).

## 10. Explicit exclusions

### 10.1 Clinical

Exact exclude: B04, C02, E01, E02, A6.5 clinical Exit work, A7-CL, CL-01, CL-02, clinical
Prompt, eligible clinical route, Capability runtime binding, clinical evaluation, clinical
rules, thresholds, hypotheses, medical-source approval, clinical gold, patient data, PHI.

### 10.2 Provider

```text
Real Provider: FORBIDDEN
External Model/API: 0
Provider Credentials: NOT_AUTHORIZED
Network Model Inference: 0
```

Secret Manager work is architecture/config mechanics only.

### 10.3 Phase B / Future Extensions

Phase B: `NOT_AUTHORIZED`.

`docs/refactoring/extensions/**` and related future-only designs remain
`FUTURE_EXTENSION_DESIGN / NOT_AUTHORIZED / REFERENCE_ONLY`, including at least:

Device Connector, Digital Pulse, Tongue Imaging, Wearable, EHR, Structured Medical Provider,
Drug Intelligence, Regulatory Intelligence, Device Intelligence, Temporal, Special Population,
Chronic, Multimodal, `tcm_four_diagnosis_v1`.

They must not enter NC Closure implementation batches.

## 11. Stability assertions

### 11.1 Capability

```text
adult_respiratory_v1:
  DRAFT
  PARTIALLY_VALIDATED
  REQUIRES_CLINICAL_REVIEW
  NOT_IMPLEMENTED
  Production Eligibility: BLOCKED
```

NC Closure must not upgrade Capability lifecycle.

### 11.2 Model Runtime

```text
A7-NC: COMPLETE
```

This amendment must not reopen ModelSpec, ModelRoutePolicy, Prompt Runtime, Output Schema
Registry, Gateway, ProviderAdapter, Fake, Legacy containment, or blocked-shell semantics
unless a future separate architecture review authorizes it.

## 12. Candidate batch graph

Suggested batches (planning recommendation only; all `NOT_AUTHORIZED`):

| Batch ID | Title | Notes |
|---|---|---|
| NC-CLOSE-01 | ADR Foundation | RECOMMENDED_ONLY first batch |
| NC-CLOSE-02 | Shared Contract Bindings | parallel-safe vs 01 after separate auth |
| NC-CLOSE-03 | CI MVP | soft depends on ADR/bindings |
| NC-CLOSE-04 | Workflow / Trace | A6.5 mappings hard input; soft ADR |
| NC-CLOSE-05 | Runtime Evidence Coordination | preexisting A1–A3 authority |
| NC-CLOSE-06 | E2E / Coverage / Reconciliation | late integration |
| NC-CLOSE-07 | Independent Exit Review | hard depends on required lane-owned batches |

### 12.1 Dependency notes

- ADR Foundation soft → CI
- ADR Foundation soft → Workflow/Trace
- Bindings soft → CI contract jobs
- A6.5 mappings hard input → Workflow/Trace
- Runtime Evidence parallel-safe
- E2E/Coverage late integration
- Exit Review hard depends on all required lane-owned batches

Batch numbers do **not** imply hard serialization. Technically independent batches may run
in parallel only after each receives its own Explicit Authorization.

### 12.2 Recommended first batch

```text
Recommended first Explicit Authorization target: NC-CLOSE-01 ADR Foundation
Status: RECOMMENDED_ONLY
Authorized: NO
```

Reasons: high fan-out, Freeze-gate importance, low clinical leakage, reduces downstream rework.
This recommendation does not authorize NC-CLOSE-01.

### 12.3 Batch governance lifecycle

Each implementation batch must follow:

```text
Authorization Assessment
→ Explicit Authorization
→ Implementation
→ Independent Review
→ Remediation if required
→ Re-Review
→ Merge Review
→ Explicit Merge Authorization
→ Merge
→ Mandatory Post-Merge Verification
```

## 13. PHASE_A_NC_CLOSURE_EXIT

Independent exit gate: `PHASE_A_NC_CLOSURE_EXIT`.

Proposed exit conditions (all required lane-owned gaps must be `MERGED_AND_VERIFIED`):

- required ADR decisions approved per governance;
- three-language bindings complete;
- CI MVP merged/verified;
- Workflow/Trace evidence closed;
- runtime evidence accounting complete;
- E2E design complete;
- Coverage/Migration reconciliation complete;
- clinical exclusions intact;
- Contract semantic diff controlled;
- Capability lifecycle unchanged;
- Model Runtime unchanged unless separately reviewed;
- Future Extensions untouched;
- real provider = 0;
- PHI = 0;
- clinical activation = 0;
- Independent Exit Review PASS.

Legal dual state after Exit PASS:

```text
PHASE_A_NC_CLOSURE: COMPLETE
Phase A: Freeze Candidate
Frozen Baseline: NOT_READY_BLOCKED_CLINICAL
A7: NOT_COMPLETE
A7-CL: BLOCKED
```

```text
PHASE_A_NC_CLOSURE_COMPLETE != PHASE_A_FROZEN_BASELINE
```

Suggested lane vocabulary:

```text
PHASE_A_NC_CLOSURE:
  NOT_AUTHORIZED | AUTHORIZED | IN_PROGRESS | EXIT_REVIEW | COMPLETE
```

Do not abuse “A8 COMPLETE” to mean lane complete.

## 14. Stop / revoke conditions

Exact stop tokens:

```text
PHASE_A_NC_CLOSURE_ARCHITECTURE_BOUNDARY_REACHED
A5_CONTRACT_SEMANTIC_CHANGE_REQUIRES_SEPARATE_REVIEW
PHASE_A_NC_CLOSURE_CLINICAL_BOUNDARY_REACHED
PHASE_A_NC_CLOSURE_PROVIDER_BOUNDARY_REACHED
PHASE_A_NC_CLOSURE_PHI_BOUNDARY_REACHED
PHASE_A_NC_CLOSURE_FUTURE_EXTENSION_BOUNDARY_REACHED
```

### 14.1 Architecture stop

If a future batch requires changing system shape, 11 modules, state ownership, core dependency
direction, Java/Python primary boundary, or fixed Workflow fallback principle: STOP and
complete Architecture Review before continuation.

### 14.2 Contract stop

If bindings require Shared Contracts v1 semantic change: STOP and separate Contract Review.

### 14.3 Clinical stop

Any real clinical content, rule, threshold, hypothesis, medical-source approval, Prompt body,
route eligibility, Capability activation, or clinical evaluation: STOP.

### 14.4 Provider / PHI / Future Extension stop

Real provider/credentials/paid or network model inference → provider stop.
Patient/PHI → PHI stop.
Future-extension implementation → future-extension stop.

Revocation returns work to the historical order without rewriting history, without claiming
A6.5/A7 complete, and without authorizing clinical activation.

## 15. External runtime boundary

Future batches may use Docker, DB, Redis, Neo4j, Nacos, local services, Frontend/backend
integration, and OTel test backends for infrastructure/runtime evidence only, with:

```text
PHI = 0
clinical decision = 0
real provider = 0
```

## 16. What this amendment does not authorize

This planning artifact does not authorize:

A8 ADR decision/approval, A9 CI implementation, A10 Workflow/Trace implementation,
A5 binding generation, A1–A3 runtime remediation as newly granted authority,
E2E clinical execution, A11 Freeze Review, A7-CL/CL-01/CL-02, B04/C02/E01/E02,
Phase B, clinical Prompt/route/Capability/evaluation/rules/thresholds/hypotheses,
medical-source approval, clinical gold, patient/PHI, real provider, external model/API,
or production enablement.

## 17. Current control state after this planning change

```text
A7-NC: COMPLETE
A7-NC Exit: PASSED
PHASE_A_NC_CLOSURE Roadmap Amendment: IMPLEMENTED_PENDING_INDEPENDENT_REVIEW
PHASE_A_NC_CLOSURE implementation: NOT_AUTHORIZED
A6.5: INCOMPLETE_BLOCKED_DEPENDENCY
A7-CL: BLOCKED_BY_A6_5_CLINICAL_LANE
A7: NOT_COMPLETE
Phase A: Freeze Candidate
A8/A9/A10/A11: NOT_AUTHORIZED
Clinical Runtime: NOT_ENABLED
Production: BLOCKED
Phase B: NOT_AUTHORIZED
Future Extensions: NOT_AUTHORIZED
CI: NO_CI_CONFIGURED
```

Next Gate after this PR is Independent Review only. Do not treat this document as
`MERGED_AND_VERIFIED` until Independent Review, Merge Review, Explicit Merge Authorization,
Merge, and Post-Merge Verification complete.
