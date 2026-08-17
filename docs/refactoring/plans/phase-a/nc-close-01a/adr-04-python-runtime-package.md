# ADR-04 Python Single Runtime Package

| Field | Value |
|---|---|
| ADR ID | ADR-04 |
| Title | Python Single Runtime Package |
| Status | `PROPOSED_DECIDED_PENDING_REVIEW` |
| Exact Base | `8a1a7691cdc3e95c5c9fd85e482c73073b5530f6` |
| Parent batch | `NC-CLOSE-01` |
| Refined batch | `NC-CLOSE-01A` |
| Decision depth | `FULL_DECISION_REQUIRED` |
| implementation_authorized | `false` |

## 1. Decision scope

Define future **Python Runtime** ownership and package boundary.

This ADR does not create the package, move services, add dependencies,
or reopen A7-NC Model Runtime semantics.

## 2. Context

The repository has ten legacy FastAPI microservices plus one isolated
A7-NC package `packages/model_runtime`. There is no `packages/python_runtime`
(or equivalent) today. Capability assets live under `capabilities/` and
are `NOT_IMPLEMENTED`. Java currently owns production orchestration.

## 3. Current repository evidence

| Evidence | Classification | Path / note |
|---|---|---|
| Ten FastAPI `*-service` trees | `CODE_CONFIRMED` | e.g. `health-state-assessment-service/app/main.py` |
| `packages/model_runtime` structural package | `CODE_CONFIRMED` | `packages/model_runtime/gateway/gateway.py` |
| ModelGateway never invokes providers/network | `CODE_CONFIRMED` | gateway docstring / implementation |
| Legacy services do not import `model_runtime` | `CODE_CONFIRMED` | no production references |
| `packages/` contains only `model_runtime` | `CODE_CONFIRMED` | package layout |
| Capability `runtime_adoption: NOT_IMPLEMENTED` | `CODE_CONFIRMED` | `capabilities/adult_respiratory_v1/manifest.yaml` |
| A7-NC COMPLETE / Exit PASSED | `DOCUMENTED` | Enterprise control pointer |
| No LangGraph package | `CODE_CONFIRMED` | search = 0 |

## 4. Problem

Without a canonical Python Runtime boundary, later LangGraph work,
protocol adapters, and checkpoint adapters will land inside random
legacy services or inside `model_runtime`, reopening A7-NC or mixing
clinical content with orchestration.

## 5. Architectural invariants

```text
Python Runtime  !=  Model Runtime
Python Runtime  !=  Capability Package
Python Runtime  !=  clinical intelligence / Safety content
Python Runtime  !=  contracts/v1 (it consumes them)
Python Runtime  !=  future extensions
```

A7-NC remains closed. Python Runtime may **consume** Model Runtime
only through that package’s existing public boundary.

## 6. Alternatives considered

| ID | Alternative |
|---|---|
| A4-A | Absorb all ten FastAPI services into `model_runtime` |
| A4-B | Declare the ten services to be the permanent Runtime |
| A4-C | **Selected.** New canonical package `packages/python_runtime`; legacy services contained; Model Runtime stays separate |
| A4-D | New top-level `agent-runtime/` service repo-style tree created immediately |

## 7. Selected decision

**A4-C — Canonical future Python Runtime boundary is
`packages/python_runtime`. It owns orchestration, protocol adapters,
checkpoint adapters, tool adapters, and (if adopted) LangGraph
integration. It does not absorb Model Runtime, Capability assets,
clinical rule bodies, or the ten legacy services in this decision.**

### 7.1 What owns Python-side orchestration?

**Target:** `packages/python_runtime`.
**Current production:** Java orchestrators (ADR-05). Python services
are invoked tools/steps, not the system orchestrator.

### 7.2 Canonical Runtime package boundary

```text
packages/python_runtime/          # target; NOT created in this PR
  api/                            # public HTTP + ports
  orchestration/                  # graph/workflow integration
  protocol/                       # Java↔Python adapters (ADR-03)
  checkpoint/                     # execution-state adapters (ADR-02)
  tools/                          # tool-port adapters
  model_runtime_port/             # consume packages/model_runtime only
```

Directory names above are **target architecture labels**, not an
instruction to create files now.

### 7.3 Public interfaces

Public to Java (ADR-03): versioned HTTP/REST JSON.
Public to other Python packages: explicit ports only.
Forbidden as public cross-language contract: undocumented service-local
Pydantic models.

### 7.4 Permitted dependencies

- `contracts/v1` (read / validate; no semantic edit)
- `packages/model_runtime` public API only
- stdlib / explicitly approved non-provider libraries in a later
  implementation ADR

### 7.5 Forbidden reverse dependencies

- `packages/model_runtime` MUST NOT import `packages/python_runtime`
- `capabilities/**` MUST NOT import Runtime internals
- clinical rule modules MUST NOT become Runtime internals
- Runtime MUST NOT import real provider SDKs (A7-NC rule stands)

### 7.6 Where later pieces belong

| Concern | Owner |
|---|---|
| LangGraph integration (if any) | `packages/python_runtime` orchestration |
| Model Runtime invocation | port into `packages/model_runtime` |
| Tool adapters | `packages/python_runtime` tools + legacy wrappers |
| Checkpoint adapters | `packages/python_runtime` checkpoint |
| Java/Python protocol adapters | `packages/python_runtime` protocol |

### 7.7 Legacy service strategy

**Containment, not immediate absorption.**

The ten FastAPI services remain separately owned until a later
authorized migration. They may be wrapped behind Runtime tool ports.
This ADR does **not** move, merge, or delete them.

### 7.8 What remains separately owned

- `packages/model_runtime` (A7-NC)
- `contracts/v1` (A5)
- `capabilities/**` (clinical package assets; still not activated)
- Java `diagnosis-service` orchestration (current production)
- `docs/refactoring/extensions/**` (not authorized)

### 7.9 What is explicitly NOT part of this package

ModelSpec / route / prompt / gateway internals; clinical YAML/rules;
Safety Engine policy bodies; provider credentials; Phase B modules.

## 8. Rationale

A new package name makes the A7-NC closure mechanically enforceable
(`model_runtime` stays closed). Immediate absorption of ten services
would be implementation, not a decision, and would mix clinical
legacy content into the Runtime.

## 9. Rejected alternatives

- **A4-A** reopens A7-NC by dumping orchestration into Model Runtime.
- **A4-B** freezes fragmentation as the target.
- **A4-D** creates a tree now; this task forbids implementation.

## 10. Tradeoffs

Two Python packages (`python_runtime` + `model_runtime`) add a port
layer. That is the cost of not reopening A7-NC.

## 11. Dependencies

- ADR-02: checkpoint adapters live here.
- ADR-03: protocol adapters live here.
- ADR-05: LangGraph, if used, lives here and nowhere else.

## 12. Compatibility requirements

A7-NC public types remain as-is. Shared Contracts v1 unchanged.

## 13. Migration implications

Later authorized work creates `packages/python_runtime` and adapters.
Legacy services are wrapped first, replaced later. No big-bang move.

## 14. Implementation consequences

No package is created in this PR. No `requirements.txt` change.

## 15. Deferred questions

| Question | Reason | Owner | Trigger | Evidence | Stop |
|---|---|---|---|---|---|
| Internal submodule names beyond the labels in §7.2 | implementation | 01A implementation | implementation auth | package design review | do not create code now |
| Order of wrapping the ten services | implementation sequencing | later batch | implementation auth | per-service inventory | no silent absorption |

## 16. Explicit non-goals

Creating the package; moving services; changing Model Runtime;
activating Capability; enabling providers.

## 17. Architecture stop conditions

```text
A7_NC_MODEL_RUNTIME_REOPEN_REQUIRED
```

if ModelSpec / ModelRoutePolicy / Prompt Runtime / Output Schema
Registry / Gateway / ProviderAdapter / Fake / blocked-shell semantics
must change. **Not triggered.**

## 18. Verification required before implementation / adoption

IR/MR of this ADR; later import-linter / architecture tests forbidding
`model_runtime` → `python_runtime` and provider SDK imports.

## 19. Clinical / Provider / PHI exclusions

No clinical content ownership. No provider activation.

## 20. Relationships

- A5: Runtime validates contracts; does not own them.
- A7-NC: consume-only.
- ADR-05: LangGraph cannot live in `model_runtime` or in Java as a
  hidden second engine without this package.

## 21. Decision owner / governance state

Owner: Phase A NC Closure / NC-CLOSE-01A.
State: `PROPOSED_DECIDED_PENDING_REVIEW`.

## 22. Next gate

Combined Independent Review + Merge Review.
Do not create `packages/python_runtime` in this task.
