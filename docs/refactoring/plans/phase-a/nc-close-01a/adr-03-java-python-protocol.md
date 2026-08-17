# ADR-03 Java / Python Protocol

| Field | Value |
|---|---|
| ADR ID | ADR-03 |
| Title | Java / Python Protocol |
| Status | `PROPOSED_DECIDED_PENDING_REVIEW` |
| Exact Base | `8a1a7691cdc3e95c5c9fd85e482c73073b5530f6` |
| Parent batch | `NC-CLOSE-01` |
| Refined batch | `NC-CLOSE-01A` |
| Decision depth | `FULL_DECISION_REQUIRED` |
| implementation_authorized | `false` |

## 1. Decision scope

Define the public communication boundary between Java
orchestration / application services and the future Python Runtime.

This ADR does not implement a new protocol, fix Feign routes, generate
bindings, or change Shared Contracts v1 semantics.

## 2. Context

Production Java already calls many Python FastAPI services through
OpenFeign HTTP POST. Trace headers `X-CDP-Id` and `X-Trace-Id` exist.
Shared Contracts v1 is the language-neutral semantic catalog, but Java
and legacy Python currently use parallel DTOs / Pydantic models.
NC-CLOSE-02 (bindings) is not authorized.

## 3. Current repository evidence

| Evidence | Classification | Path / note |
|---|---|---|
| OpenFeign clients from diagnosis-service | `CODE_CONFIRMED` | `diagnosis-service/.../client/*Client.java` |
| Feign headers `X-CDP-Id`, `X-Trace-Id` | `CODE_CONFIRMED` | `FeignTraceInterceptor.java` |
| Python FastAPI routes under `/api/v1` | `CODE_CONFIRMED` | each `*-service/app/main.py` |
| Legacy `ToolContext` / `ToolResult` Pydantic | `CODE_CONFIRMED` | e.g. `health-state-assessment-service/app/models/` |
| Java `ToolContext` / `ToolResult` DTOs | `CODE_CONFIRMED` | `diagnosis-service/.../dto/tool/` |
| contracts v1 ToolContext / ToolResult / StatePatch | `CODE_CONFIRMED` | `contracts/v1/schemas/` |
| Java does not import `contracts/v1` | `CODE_CONFIRMED` | no Java dependency |
| No Java→Python SSE/WebSocket | `CODE_CONFIRMED` | WebSocket exists only on execution-trace → frontend |
| Some Feign paths ≠ Python routes | `CODE_CONFIRMED` | see §11 |
| Live integration behavior | `UNKNOWN` | not `RUNTIME_VERIFIED` |

## 4. Problem

Without a frozen protocol, Java and Python can keep growing incompatible
paths, envelopes, and error shapes. Bindings (NC-CLOSE-02) cannot start
from an undefined transport/authority model. Route mismatches already
exist.

## 5. Architectural invariants

1. Shared Contracts v1 semantics are `IMMUTABLE_WITHIN_LANE`.
2. Public Java↔Python Runtime traffic is **HTTP/REST + JSON**.
3. Semantic payload authority is `contracts/v1`, not ad-hoc DTO drift.
4. Protocol adapters live in Python Runtime (ADR-04); Java is a
   versioned consumer.
5. SoR writes are not completed by “the HTTP call succeeded”; they
   require State Committer (ADR-02).
6. Trace / AgentEvent transport is not clinical authority.

## 6. Alternatives considered

| ID | Alternative |
|---|---|
| A3-A | gRPC as the new public protocol |
| A3-B | Shared in-process / embedded Python |
| A3-C | **Selected.** Versioned HTTP/REST JSON; contracts/v1 as payload semantics; Java consumes Python Runtime public API |
| A3-D | Keep undocumented per-service Pydantic/DTO as the permanent public contract |

## 7. Selected decision

**A3-C — The public Java ↔ Python Runtime protocol is versioned
HTTP/REST with JSON bodies. Shared Contracts v1 is the semantic
payload catalog. Python Runtime owns the public API surface. Java
orchestrators consume it. Bindings are a later NC-CLOSE-02 concern.**

### 7.1 Transport / protocol category

**HTTP/REST + JSON** is the only public cross-language Runtime protocol.

SSE and WebSocket are **not** the Java↔Python Runtime protocol.
Existing execution-trace WebSocket is frontend observability, not SoR
or Runtime control.

### 7.2 Ownership of the public API boundary

- **Semantic catalog:** `contracts/v1` (unchanged).
- **Public HTTP surface of the future Runtime:** owned by
  `packages/python_runtime` (ADR-04).
- **Current production callers:** Java Feign clients.
- **Legacy FastAPI services:** contained adapters, not a second public
  contract family.

### 7.3 Synchronous vs asynchronous

| Traffic | Mode |
|---|---|
| Orchestration / tool invoke / parse / plan | **Synchronous** request/response |
| Technical trace / AgentEvent publish | **Asynchronous** side channel; must not commit SoR |
| Authoritative state commit | Synchronous to State Committer; not “fire-and-forget HTTP” |

### 7.4 Request / response envelope

Required on public Runtime calls:

- `contract_version` = `1.0.0` where a Shared Contract payload is used
- case / CDP identity
- correlation / trace identity
- producer service name

Payload types should map to existing v1 contracts (`ToolContext`,
`ToolResult`, `StatePatch`, `CommitResult`, `TraceRef`, `AuditRef`)
via later bindings. This ADR does not edit those schemas.

### 7.5 Contract versioning

Follow `contracts/v1/manifest.json`:

```text
version_negotiation: EXACT
supported_versions: ["1.0.0"]
```

Unknown contract versions fail closed. No silent schema widening.

### 7.6 Correlation, trace, CDP identity

Current `CODE_CONFIRMED` headers remain the minimum propagation set:

- `X-CDP-Id`
- `X-Trace-Id`

They are correlation, not W3C `traceparent` and not clinical authority.
Future OTel exporter choice is ADR-10 (01C), not decided here.

### 7.7 Timeout, retry, idempotency, cancellation

- Timeouts are mandatory on Java→Python Runtime calls.
- Retry is allowed for **idempotent reads**.
- Writes / tool invokes that can produce SoR proposals require an
  idempotency key (aligned with `StatePatch.idempotency_key` when a
  patch is produced).
- Cancellation: HTTP cancellation must not be treated as a SoR commit
  or commit rollback unless State Committer confirms it.
- Streaming: not part of the public Java↔Python Runtime protocol.

### 7.8 Error taxonomy and failure propagation

Public errors MUST be machine-readable:

- HTTP status for transport class
- stable error code
- retryable vs fail-closed
- correlation ids echoed

Silent success-on-partial-failure is forbidden. Clinical fallback
policy is out of scope.

### 7.9 Compatibility and API evolution

- Additive HTTP routes may appear only under a versioned Runtime API.
- Removing or renaming a public route requires a compatibility window
  and is an implementation concern after authorization.
- Semantic field changes require
  `A5_CONTRACT_SEMANTIC_CHANGE_REQUIRES_SEPARATE_REVIEW`.

### 7.10 Relationship to NC-CLOSE-02

NC-CLOSE-02 generates / validates language bindings. It does not
redefine this protocol. This ADR is the sequencing input; it does not
authorize bindings.

## 8. Rationale

HTTP/REST is what the repository already uses. Inventing gRPC or
in-process embedding would be an architecture jump without evidence.
Freezing HTTP + contracts/v1 lets 01A proceed without touching A5
semantics.

## 9. Rejected alternatives

- **A3-A** introduces a new stack with no current code evidence.
- **A3-B** collapses process isolation and Java ownership.
- **A3-D** institutionalizes the current DTO drift.

## 10. Tradeoffs

HTTP is chatty and already shows path drift. A frozen protocol plus
later bindings is slower than “just fix the paths now”, but this task
is not allowed to repair routes.

## 11. Current route-debt disposition

`CODE_CONFIRMED` mismatches include at least:

| Java Feign declaration | Python actual (where checked) | Note |
|---|---|---|
| `POST .../workup/build-verification-plan` | `POST .../workup/verification-plan` | path mismatch |
| `POST .../treatment/generate-plan` | `POST .../treatment/plan` | path mismatch |
| `POST .../entry-assessment/perform` | no matching route found | gap |
| `POST .../parsing/backfill-evidence` | no matching route found | gap |
| `POST .../wellness/screening` on `wellness-service` | health-state-assessment `.../wellness-screening/a1-...`–`a5-...` | service + path split |

**Disposition:** `IMPLEMENTATION_DEBT_AFTER_ADR`

These mismatches **violate the selected future protocol** (one versioned
public Runtime API, stable paths, contracts-backed payloads). They do
**not** require a Shared Contracts semantic change. They must not be
repaired in this PR. They are not `RUNTIME_VERIFIED`.

Also: `BINDING_VALIDATION_REQUIRED` at NC-CLOSE-02 time to prove Java
and Python bind the same v1 identifiers.

## 12. Compatibility requirements

Shared Contracts v1 unchanged. No new clinical fields. No provider
network protocol.

## 13. Migration implications

Later implementation may introduce a façade Runtime HTTP API and
deprecate per-service Feign paths. No route edits now.

## 14. Implementation consequences

Feign clients, Python routers, and DTO alignment are future work.
This ADR only freezes the rules.

## 15. Deferred questions

| Question | Reason | Owner | Trigger | Evidence | Stop |
|---|---|---|---|---|---|
| Exact public path prefix of Python Runtime | implementation | 01A implementation | implementation auth | OpenAPI after design | do not invent paths here |
| Whether to adopt W3C traceparent | 01C / ADR-10 | NC-CLOSE-01C | 01C auth | OTel boundary ADR | do not force hosted OTel |

## 16. Explicit non-goals

Fixing Feign/Python routes; generating bindings; changing contracts;
enabling SSE as control plane.

## 17. Architecture stop conditions

```text
A5_CONTRACT_SEMANTIC_CHANGE_REQUIRES_SEPARATE_REVIEW
```

if a semantic contract change is required. Not triggered by this
decision.

## 18. Verification required before implementation / adoption

IR/MR of this ADR; later contract-binding tests; no current
`RUNTIME_VERIFIED` claim.

## 19. Clinical / Provider / PHI exclusions

Protocol may carry opaque identifiers only. No PHI policy, no provider
activation, no clinical content.

## 20. Relationships

- ADR-02: protocol carries `cdp_id` + version; not checkpoint-as-truth.
- ADR-04: Python Runtime owns the public HTTP adapter.
- ADR-05: LangGraph nodes call Runtime ports, not raw undocumented
  Python DTOs as the cross-language contract.
- A5: bind / transport / validate only.
- A7-NC: Model Runtime is not the HTTP protocol owner.

## 21. Decision owner / governance state

Owner: Phase A NC Closure / NC-CLOSE-01A.
State: `PROPOSED_DECIDED_PENDING_REVIEW`.

## 22. Next gate

Combined Independent Review + Merge Review of the NC-CLOSE-01A Draft PR.
Do not implement the protocol in this task.
