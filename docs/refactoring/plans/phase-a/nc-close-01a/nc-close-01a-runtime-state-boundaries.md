# NC-CLOSE-01A Runtime & State Boundaries

> Dated: 2026-08-17
>
> Classification: `ARCHITECTURE_DECISION_ONLY` / `NO IMPLEMENTATION`
>
> Lane: `PHASE_A_NC_CLOSURE`
>
> Parent batch: `NC-CLOSE-01` = ADR Foundation (umbrella implementation
> remains `NOT_AUTHORIZED`)
>
> Refined batch: `NC-CLOSE-01A`
>
> Exact Enterprise Base: `8a1a7691cdc3e95c5c9fd85e482c73073b5530f6`
>
> PR #42 merge: `8a1a7691cdc3e95c5c9fd85e482c73073b5530f6`
>
> Authorization token:
> `NC_CLOSE_01A_ADR_DECISION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> ADR status: `PROPOSED_DECIDED_PENDING_REVIEW`
>
> implementation_authorized: `false`

## 1. Purpose

This is the integration / index artifact for the four NC-CLOSE-01A
architecture decisions. It exists to enforce a single dependency chain
and to reject circular authority.

It does **not** authorize implementation, merge, A8, 01B, 01C, clinical
runtime, or production.

## 2. Decision package

| ADR | File | Central selected decision |
|---|---|---|
| ADR-02 | [adr-02-checkpoint-clinical-db-boundary.md](./adr-02-checkpoint-clinical-db-boundary.md) | Checkpoint ≠ clinical SoR. Future State Committer is the only authoritative writer. |
| ADR-03 | [adr-03-java-python-protocol.md](./adr-03-java-python-protocol.md) | Public Java↔Python Runtime protocol = versioned HTTP/REST JSON; `contracts/v1` is semantic catalog. |
| ADR-04 | [adr-04-python-runtime-package.md](./adr-04-python-runtime-package.md) | Canonical target package = `packages/python_runtime`; consume Model Runtime; do not absorb it. |
| ADR-05 | [adr-05-langgraph-usage-boundary.md](./adr-05-langgraph-usage-boundary.md) | LangGraph, if adopted, is an internal Python Runtime engine only. Java remains current production orchestrator. |

Each ADR status is `PROPOSED_DECIDED_PENDING_REVIEW`.
None is `APPROVED`, `MERGED`, `IMPLEMENTED`, or `RUNTIME_VERIFIED`.

## 3. Control state that must remain true

```text
NC-CLOSE-01:                 NOT_AUTHORIZED as umbrella implementation
NC-CLOSE-01A ADR Decision:   PROPOSED_DECIDED_PENDING_REVIEW
NC-CLOSE-01A implementation: NOT_AUTHORIZED
NC-CLOSE-01B:                NOT_AUTHORIZED
NC-CLOSE-01C:                NOT_AUTHORIZED
A7-NC:                       COMPLETE
A7-CL:                       BLOCKED
A7:                          NOT_COMPLETE
A8–A11:                      NOT_AUTHORIZED
Clinical Runtime:            NOT_ENABLED
Production:                  BLOCKED
Phase B:                     NOT_AUTHORIZED
```

## 4. Required dependency chain

```text
ADR-02  State ownership
   →
ADR-03  Cross-language protocol
   →
ADR-04  Python Runtime ownership
   →
ADR-05  Workflow / LangGraph authority
```

Interpretation:

1. **ADR-02 first.** Nothing downstream may invent a second SoR.
2. **ADR-03 next.** Identities (`cdp_id`, version, correlation) cross
   languages only as protocol fields, not as checkpoint-as-truth.
3. **ADR-04 next.** Protocol adapters, checkpoint adapters, and later
   LangGraph live in one Runtime package that is not Model Runtime.
4. **ADR-05 last.** A graph engine may orchestrate only inside that
   package and only within ADR-02 / ADR-03 / ADR-04 limits.

## 5. Cross-ADR consistency check

| Check | Result |
|---|---|
| ADR-02: only State Committer writes SoR; ADR-05: LangGraph must not write clinical truth | **Consistent** |
| ADR-03: public protocol + contracts/v1 authority; ADR-04: no undocumented Python DTO as cross-language contract | **Consistent** |
| ADR-04: Model Runtime remains separate; ADR-05: no provider/routing semantics inside LangGraph nodes | **Consistent** |
| ADR-05: Java remains current production orchestrator; ADR-04: Python Runtime is the **target** owner, not already production authority | **Consistent** |
| ADR-02: checkpoint never clinical; ADR-03: HTTP success ≠ SoR commit | **Consistent** |
| ADR-01 treated as soft input only; no production DB selected | **Consistent** |
| Shared Contracts v1 semantics unchanged | **Consistent** |
| A7-NC not reopened | **Consistent** |

Contradictions rejected (none selected):

- LangGraph writes SoR because it owns an edge
- Python-internal DTO as the public Java contract
- Model routing / provider policy embedded in graph nodes
- Python Runtime claimed as already-running production orchestrator
- Checkpoint declared clinical authority

```text
Contradictions: 0
Package coherent: YES
```

## 6. Repository evidence used (revalidation)

This package was written from current Enterprise documentation **and**
source inspection on Base `8a1a7691cdc3e95c5c9fd85e482c73073b5530f6`.
Authorization Assessment evidence remains valid.

| Area | Finding | Class |
|---|---|---|
| Java production orchestration | `DiagnosisOrchestrationService` → workflow / wellness orchestrators; Feign then `CDPManager.updateCDP` | `CODE_CONFIRMED` |
| Java agent loop | `ClinicalAgentBrain` / `AgentLoop` implemented; not Controller-wired | `CODE_CONFIRMED` |
| Java SoR | CDP / CDPVersion / AgentState / AuditTrail; no `StateCommitter` class | `CODE_CONFIRMED` |
| Java protocol | OpenFeign + `X-CDP-Id` / `X-Trace-Id`; Java does not consume `contracts/v1` | `CODE_CONFIRMED` |
| Python services | Ten FastAPI `*-service` trees; no `packages/python_runtime` | `CODE_CONFIRMED` |
| Model Runtime | `packages/model_runtime`; gateway never invokes providers/network | `CODE_CONFIRMED` |
| Contracts | `contracts/v1` `1.0.0` EXACT; `StatePatch` is a proposal | `CODE_CONFIRMED` |
| LangGraph | zero `langgraph` / `StateGraph` in Java/Python | `CODE_CONFIRMED` |
| Route mismatches | Feign paths ≠ several Python routes | `CODE_CONFIRMED` → `IMPLEMENTATION_DEBT_AFTER_ADR` |
| Build / test / runtime of these ADRs | not performed | not `BUILD_VERIFIED` / `TEST_VERIFIED` / `RUNTIME_VERIFIED` |

Historical note: `phase-a-current-state-reconciliation-2026-08-14-post-pr40.md`
is stale relative to PR #42. Current authority is this package plus
NC-CLOSE-01 refinement / README / executable roadmap. This file is
**not** a replacement current-state reconciliation.

## 7. What this package does not do

- implement checkpoint backend, State Committer, LangGraph, or Runtime
- create `packages/python_runtime`
- change Feign or Python routes
- change DTOs or `contracts/v1`
- select or migrate a production primary DB
- reopen A7-NC
- authorize 01B / 01C / A8–A11 / Phase B
- enable Clinical Runtime or production

## 8. Next gate

```text
Combined Independent Review + Merge Review
of the exact Draft PR Head that contains this package.
```

Do not mark Ready from the authoring task.
Do not merge from the authoring task.
Do not implement NC-CLOSE-01A from this documentation.

## 9. Success meaning

Successful publication of this package means only:

```text
NC-CLOSE-01A ADR decisions: PROPOSED_DECIDED_PENDING_REVIEW
```

It does not mean approved, merged, implementation-authorized, NC Closure
complete, or Frozen Baseline.
