# ADR-05 LangGraph Usage Boundary

| Field | Value |
|---|---|
| ADR ID | ADR-05 |
| Title | LangGraph Usage Boundary |
| Status | `PROPOSED_DECIDED_PENDING_REVIEW` |
| Exact Base | `8a1a7691cdc3e95c5c9fd85e482c73073b5530f6` |
| Parent batch | `NC-CLOSE-01` |
| Refined batch | `NC-CLOSE-01A` |
| Decision depth | `FULL_DECISION_REQUIRED` |
| implementation_authorized | `false` |

## 1. Decision scope

Define **whether and where** LangGraph belongs in the target
architecture, and which authorities it may never acquire.

This ADR does not add a LangGraph dependency, create a `StateGraph`,
or rewrite Java orchestrators.

## 2. Context

Planning documents discuss Thread / Checkpoint / Resume and mention
LangGraph as a candidate durable-execution engine. Current production
orchestration is Java and deterministic. `ClinicalAgentBrain` /
`AgentLoop` exist but are not Controller-wired. Python has no LangGraph
implementation.

## 3. Current repository evidence

| Evidence | Classification | Path / note |
|---|---|---|
| Production diagnosis path uses Java orchestrators | `CODE_CONFIRMED` | `DiagnosisOrchestrationService` → `DiagnosisWorkflowOrchestrator` / `WellnessScreeningOrchestrator` |
| `ClinicalAgentBrain` / `AgentLoop` implemented | `CODE_CONFIRMED` | `diagnosis-service/.../agent/` |
| AgentLoop not Controller-wired | `CODE_CONFIRMED` | no controller injection / route |
| Clinical fail-closed points in AgentLoop | `CODE_CONFIRMED` | agent package |
| Zero `langgraph` / `StateGraph` in `*.java` / `*.py` | `CODE_CONFIRMED` | repository-wide search |
| Durable Execution design mentions checkpoint/resume | `DOCUMENTED` | `docs/refactoring/Durable Execution 与可观测性扩展方案.md` |
| Agent-runtime foundations | `DOCUMENTED` | `docs/refactoring/agent-runtime-foundations-扩展.md` |
| Target graph already running | not claimed | not `RUNTIME_VERIFIED` |

## 4. Problem

A slogan decision (“use LangGraph” / “do not use LangGraph”) would
either freeze Java forever without a target, or let a graph library
become clinical / Safety / SoR authority by controlling edges.

## 5. Architectural invariants

1. Orchestration ≠ clinical authority.
2. Orchestration ≠ Safety Engine authority.
3. Orchestration ≠ State Committer authority (ADR-02).
4. Orchestration ≠ Model Runtime authority (A7-NC / ADR-04).
5. A graph edge never grants write, Safety bypass, or provider
   activation rights.
6. Current production authority remains Java until a separately
   authorized migration.

## 6. Alternatives considered

| ID | Alternative |
|---|---|
| A5-A | No LangGraph in the target Runtime |
| A5-B | **Selected target.** LangGraph as an **internal** orchestration engine inside Python Runtime |
| A5-C | LangGraph as top-level cross-system orchestration authority |
| A5-D | Hybrid / unbounded (Java + LangGraph both SoR writers) |

Selected disposition is **A5-B as the target architecture**, with an
explicit **hybrid coexistence** rule for the current Java production
path. That is bounded hybrid **migration**, not Option C and not
unbounded dual authority.

## 7. Selected decision

**A5-B (target) + bounded coexistence — LangGraph, if adopted, is only
an internal orchestration engine inside `packages/python_runtime`.
It is never the cross-system authority, never the SoR writer, never
the Safety Engine, and never the Model Runtime. Current production
orchestration remains the Java deterministic orchestrators until a
later authorized cutover.**

This is **not** “use LangGraph now”. It is **not** “never use
LangGraph”. It is a usage boundary.

### 7.1 What LangGraph may control (inside Python Runtime only)

| Concern | Allowed? | Condition |
|---|---|---|
| Deterministic transitions | Yes | graph-local; no SoR write |
| Model invocation | Yes | **only** via `packages/model_runtime` public port |
| Tool invocation | Yes | **only** via Runtime tool adapters |
| Retry | Yes | idempotent / bounded; no SoR mutation by retry itself |
| Fallback | Yes | technical fallback only; no clinical-policy rewrite |
| Checkpoint / resume | Yes | execution metadata only (ADR-02) |
| Human-review handoff | Yes | interrupt / resume token; review decision is outside the graph |
| Conditional routing | Yes | routing ≠ clinical truth |

### 7.2 What LangGraph may NOT control

| Concern | Allowed? |
|---|---|
| Irreversible clinical truth writes | **No** — State Committer only |
| Safety Engine bypass | **No** |
| Direct clinical authority | **No** |
| Provider activation / paid network inference | **No** |
| Self-modifying policy | **No** |
| Unrestricted autonomous loops | **No** — hard iteration / budget / stop conditions required |
| Patient-facing delivery as committed claim | **No** |
| Audit/compliance substitution | **No** — audit is produced, not replaced |

### 7.3 Authority map

| Authority | Owner |
|---|---|
| Production orchestration today | Java `DiagnosisWorkflowOrchestrator` / `WellnessScreeningOrchestrator` |
| Target orchestration engine | LangGraph **inside** Python Runtime |
| Clinical content / hypotheses | Capability + clinical services (not this ADR) |
| Safety | Safety Engine (not LangGraph) |
| Authoritative state | State Committer (ADR-02) |
| Model routing / providers | Model Runtime (A7-NC, closed) |
| Tool execution | Runtime tool ports + contained legacy services |
| Human review | explicit interrupt; human / policy, not a graph edge |
| Patient delivery | application / UI after SoR commit |
| Audit / compliance | AuditTrail / AgentEvent / trace; not the graph |

### 7.4 Why not A5-A, A5-C, or unbounded hybrid

- **A5-A** would freeze a Java-only target and contradict already
  published durable-execution / Python Runtime direction without
  offering a replacement engine.
- **A5-C** would make LangGraph the cross-system authority, colliding
  with Java application ownership, State Committer, and Safety.
- **Unbounded hybrid** would allow two SoR writers and two
  orchestrators with no cutover rule.

### 7.5 Migration / coexistence with current Java authority

Do not rewrite history:

1. **Today:** Java deterministic orchestrators are the production path.
2. **Today:** `ClinicalAgentBrain` / `AgentLoop` are implemented Java
   agent loops, **not** LangGraph, and **not** Controller-wired.
3. **Today:** Python has no LangGraph.
4. **Target:** Python Runtime may host a bounded LangGraph engine.
5. **Cutover:** only after explicit implementation authorization,
   protocol adapters (ADR-03), Runtime package (ADR-04), and State
   Committer governance (ADR-02).
6. **During coexistence:** Java remains the production orchestrator.
   A hidden second production graph is forbidden.
7. AgentLoop is **not** declared the target engine. It is historical
   unimplemented-on-the-wire code.

The target architecture is **not** claimed to be running.

### 7.6 Autonomous loop policy

Any future graph MUST have:

- max steps / budget
- fail-closed stop on Safety or contract failure
- no self-enlarging tool/model loops
- no provider activation from a node

`AgentLoop` already contains fail-closed clinical points; those
semantics are not copied or relaxed here.

## 8. Rationale

Repository evidence shows Java production orchestration and zero
LangGraph. Planning evidence wants durable graph execution. The only
non-contradictory target is: LangGraph may exist later, but only as
an internal Runtime engine with no extra authority.

## 9. Rejected alternatives

See §7.4. A slogan “use LangGraph” is rejected because it omits
authority limits. A slogan “do not use LangGraph” is rejected because
it leaves Python Runtime without a named target engine while still
requiring checkpoint/resume (ADR-02).

## 10. Tradeoffs

Teams cannot drop LangGraph into `diagnosis-service` or a random
FastAPI app. Adoption is slower and requires the Runtime package.
That is the intended control.

## 11. Dependencies

- ADR-02: checkpoint ≠ SoR; graph resume cannot beat CDP.
- ADR-03: graph I/O crosses Java only through the public HTTP protocol.
- ADR-04: LangGraph lives only in `packages/python_runtime`.

## 12. Compatibility requirements

No new Java or Python dependency in this PR. A7-NC unchanged.
Shared Contracts v1 unchanged.

## 13. Migration implications

Implementation (not authorized) may add LangGraph **inside**
`packages/python_runtime` only. Adding it to Java, to
`packages/model_runtime`, or as a top-level service is out of bounds.

## 14. Implementation consequences

No `langgraph` dependency, no `StateGraph`, no rewrite of
`DiagnosisWorkflowOrchestrator`.

## 15. Deferred questions

| Question | Reason | Owner | Trigger | Evidence | Stop |
|---|---|---|---|---|---|
| Exact graph topology / node list | implementation + clinical-content exclusion | later design | implementation auth | Runtime design review | do not encode clinical nodes here |
| Whether AgentLoop is deleted or frozen | unused Java code | later Java cleanup | separate auth | wiring audit | do not delete now |

## 16. Explicit non-goals

Adding LangGraph; wiring AgentLoop; enabling clinical runtime;
authorizing A8–A11.

## 17. Architecture stop conditions

- LangGraph required to write SoR directly
- LangGraph required to own ModelRoutePolicy / providers
- LangGraph required as cross-system authority (A5-C)

None of these are selected.

## 18. Verification required before implementation / adoption

IR/MR of this ADR; later architecture tests that LangGraph nodes
cannot import SoR writers, provider SDKs, or Safety bypass hooks.
No `RUNTIME_VERIFIED` claim now.

## 19. Clinical / Provider / PHI exclusions

LangGraph must not contain clinical rule bodies, thresholds, or
prompts. No provider activation.

## 20. Relationships

- A5 Shared Contracts: graph payloads bind v1 types; no semantic edit.
- A7-NC: model calls only through closed Model Runtime.
- ADR-02 / 03 / 04: see §11.

## 21. Decision owner / governance state

Owner: Phase A NC Closure / NC-CLOSE-01A.
State: `PROPOSED_DECIDED_PENDING_REVIEW`.

## 22. Next gate

Combined Independent Review + Merge Review of the NC-CLOSE-01A Draft PR.
Do not implement LangGraph in this task.
