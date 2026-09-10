# ADR-11 Legacy AOP to OpenTelemetry Migration

| Field | Value |
|---|---|
| ADR ID | ADR-11 |
| Title | Legacy AOP to OpenTelemetry Migration |
| Status | `PROPOSED_DECIDED_PENDING_REVIEW` |
| Exact Base | `e9f4baa791009ae16b6cc956a31bcd4bbd428213` |
| Parent batch | `NC-CLOSE-01` |
| Refined batch | `NC-CLOSE-01C` |
| Decision depth | `FULL_DECISION_REQUIRED` |
| Disposition | `FULL_DECISION` / `PHASED_LEGACY_BRIDGE_TO_OTEL` |
| implementation_authorized | `false` |

## 1. Decision scope

Decide the durable **migration / bridge strategy** from current Java AOP
and `execution-trace-service` technical tracing to a future
OpenTelemetry-compatible technical-span path.

This ADR does not implement any migration phase, install an SDK, change
headers, or remove legacy fallback.

## 2. Context

Current technical tracing is self-built. Target documents recommend
OTel. ADR-10 freezes the OTel/OTLP **boundary** but does not install it.
Without a full migration decision, later NC-CLOSE-04 work would have to
choose between immediate replacement, permanent legacy, or an unstated
bridge.

ADR-03 remains the public Java↔Python protocol authority.

## 3. Current authority

**CURRENT technical-tracing authority** is the legacy Java path when
enabled, persisted by `execution-trace-service`. It is **not**
OpenTelemetry.

| Mechanism | Role | Class |
|---|---|---|
| `ExecutionTraceAspect` | in-process AOP around `@TraceExecution` | `CODE_CONFIRMED` |
| `@TraceExecution` | annotation contract (`service`, `module`, input/output flags) | `CODE_CONFIRMED` |
| `TraceServiceClient` | HTTP export to trace service | `CODE_CONFIRMED` |
| `FeignTraceInterceptor` / response decoder | emit Feign start/end; inject headers | `CODE_CONFIRMED` |
| `TraceContext` | ThreadLocal `cdpId` only | `CODE_CONFIRMED` |
| `execution-trace-service` / `execution_trace` | persistence + WebSocket | `CODE_CONFIRMED` |
| `frontend-admin` trace/timeline/call-graph | legacy consumers | `CODE_CONFIRMED` |
| `common/aidoctor_trace` decorator/middleware | helper exists; **not** service-wide production tracing authority | `CODE_CONFIRMED` |
| Gate `execution.trace.enabled=true` (`matchIfMissing=false`) | opt-in; default off in tracked diagnosis-service YAML | `CODE_CONFIRMED` |
| Headers `X-CDP-Id`, `X-Trace-Id` | current correlation | `CODE_CONFIRMED` |
| W3C `traceparent` / `tracestate` runtime | **absent** | `CODE_CONFIRMED` |
| MDC trace correlation | **absent** | `CODE_CONFIRMED` |

Do not claim a current authoritative W3C runtime.

## 4. Problem

Immediate replacement would drop admin consumers and an existing
(optional) trace store before equivalence exists. Permanent legacy-only
architecture would contradict the already-frozen OTel export boundary
(ADR-10). A big-bang cutover has no rollback story.

## 5. Architectural invariants

1. Selected strategy: **phased legacy bridge → OTel migration**.
2. Legacy AOP / `execution-trace-service` remain CURRENT until
   equivalence evidence and rollback readiness exist.
3. ADR-03 headers remain the current minimum set.
4. Future OTel may **add** W3C context; it must not redefine ADR-03.
5. `cdp_id` and `trace_id` remain different concepts.
6. This ADR authorizes **no** implementation phase.

## 6. Alternatives considered

| ID | Alternative |
|---|---|
| A11-A | Immediate replacement of AOP / execution-trace |
| A11-B | Permanent legacy-only architecture |
| A11-C | Big-bang dual-write then hard cutover without evidence gates |
| A11-D | **Selected.** Phased legacy bridge → OTel migration |

## 7. Selected decision

**A11-D — PHASED LEGACY BRIDGE → OTEL MIGRATION.**

**CURRENT authority:** legacy AOP + `execution-trace-service`.
**TARGET authority:** OpenTelemetry-compatible Technical Span
instrumentation/export (ADR-10), with AgentEvent on a separate channel
(ADR-12).

### 7.1 Conceptual migration sequence

These phases are architecture only. None is authorized now.

| Phase | Meaning |
|---|---|
| 1 | Legacy tracing remains authoritative/current; define compatibility and equivalence evidence. |
| 2 | Introduce OTel-compatible instrumentation/context in a **separately authorized** implementation. |
| 3 | Bridge / dual-emission or adapter period where necessary. |
| 4 | Verify operational equivalence and consumer compatibility. |
| 5 | Only after evidence and rollback readiness may legacy technical tracing be reduced or removed. |

### 7.2 Bridge

During the authorized implementation window, a bridge or dual-emission
adapter **MAY** map:

- legacy `ExecutionTrace` events ↔ Technical Spans
- `cdpId` ↔ resource/span attributes
- service/method/timing/error status ↔ OTel semantic fields

The OTel model is **not** required to copy every legacy field
one-to-one.

### 7.3 Compatibility requirements

Future migration must preserve or explicitly map:

- CDP correlation
- trace correlation
- service / method attribution
- timing
- error status
- existing admin trace consumers **where still required**
- legacy trace lookup capability during transition

Compatibility is defined at semantic / consumer-requirement level, not
as a field-for-field clone.

### 7.4 W3C context boundary

ADR-03 remains authoritative for the current Java/Python protocol.

Therefore:

- `X-CDP-Id`
- `X-Trace-Id`

remain the current minimum correlation headers where ADR-03 established
them.

A future OTel implementation **MAY ADD**:

- `traceparent`
- `tracestate`

as technical trace context.

01C does **not** redefine ADR-03.

W3C context must not turn `cdp_id` into `trace_id`.

### 7.5 Legacy removal gate

It is **forbidden** to remove current AOP / execution-trace fallback
before equivalent evidence exists.

Required future evidence categories before reduction/removal:

- `CODE_CONFIRMED`
- `BUILD_VERIFIED`
- `TEST_VERIFIED`
- and, where actual runtime behavior is being replaced:
  `RUNTIME_VERIFIED` or equivalent authorized integration evidence

Consumer compatibility must be verified.
A rollback path must exist (disable OTel export; keep legacy switch).

### 7.6 Failure / fallback

Target technical-span emission should be
`FAIL_OPEN_WITH_OBSERVABILITY_DEGRADATION` (ADR-10).

CURRENT AOP/client behavior is **not** uniformly proven fail-open.
That is implementation/verification debt (see §10), not a claim that
the current path is already correct.

## 8. Rationale

The repository has a real legacy authority and a frozen OTel target.
A phased bridge is the only strategy that preserves consumers, honors
ADR-03, and still allows later NC-CLOSE-04 work to proceed without
ambiguity.

## 9. Rejected alternatives

- **A11-A** removes fallback before evidence.
- **A11-B** contradicts ADR-10’s target export boundary.
- **A11-C** skips verification and rollback.

## 10. Known implementation / verification debt

Recorded, **not fixed** in this package:

| Debt | Observation | Class |
|---|---|---|
| `TraceContext` does not own persistent `traceId` propagation | ThreadLocal `cdpId` only | `CODE_CONFIRMED` |
| Per-hop `X-Trace-Id` may generate new identifiers | Feign interceptor creates a new UUID per call | `CODE_CONFIRMED` |
| Async / cross-thread propagation may be incomplete | no TaskDecorator / MDC | `CODE_CONFIRMED` |
| AOP/client failure behavior may not be uniformly fail-open | start `recordEvent` is synchronous before `proceed`; C03 noted fail-closed risk | `DOCUMENTED` / `PARTIALLY_VALIDATED` |
| Python helper is not service-wide authority | no service import of `aidoctor_trace` observed | `CODE_CONFIRMED` |
| Production enablement of `execution.trace.enabled` | `UNKNOWN` | — |

These are `IMPLEMENTATION / VERIFICATION DEBT`, not reasons to invent
unverified runtime claims.

## 11. Compatibility with 01A

- ADR-02: technical traces remain non-SoR.
- ADR-03: protocol and minimum headers unchanged.
- ADR-04: future adapters live in Python Runtime when that
  implementation is authorized; this ADR does not create the package.
- ADR-05: LangGraph, if adopted, may emit Technical Spans / AgentEvents
  only inside Runtime limits; it must not write SoR.

## 12. Implementation consequences

Later authorized NC-CLOSE-04 or observability implementation may add
OTel-compatible instrumentation and a bridge. This ADR does not change
AOP, Feign, TraceContext, Python middleware, execution-trace-service,
frontend-admin, CI, or Docker.

## 13. Deferred items

Phase scheduling, dual-write mechanics, consumer cutover date, sampling
percentages, SDK choice.

## 14. Explicit non-goals / forbidden claims

Do **not** read this ADR as:

- OTel installed
- AOP removed
- execution-trace-service rewritten or archived
- W3C headers already in production
- migration implemented
- NC-CLOSE-04 authorized

## 15. Architecture stop conditions

```text
NC_CLOSE_01C_CONFLICT_WITH_01A_DECISION
```

if a later change would redefine ADR-03 or treat checkpoint/trace as
SoR.

```text
A5_CONTRACT_SEMANTIC_CHANGE_REQUIRES_SEPARATE_REVIEW
```

if Shared Contracts v1 semantics would have to change.

## 16. Evidence truth

| Claim | Label |
|---|---|
| Current AOP / Feign / trace-service existence | `CODE_CONFIRMED` |
| Current authority identification | `CODE_CONFIRMED` |
| OTel runtime | `NOT_IMPLEMENTED` / `ABSENT` |
| Migration equivalence | `NOT_VERIFIED` |
| Production enablement | `UNKNOWN` |

## 17. Relationships

- `HARD_INPUT_FOR_NC_CLOSE_04_TRACE_OBSERVABILITY`
- Consumes ADR-10 layer boundary and ADR-12 ownership split

## 18. Decision owner / governance state

Owner: Phase A NC Closure / NC-CLOSE-01C.
State: `PROPOSED_DECIDED_PENDING_REVIEW`.
Not `APPROVED` / `MERGED` / `IMPLEMENTED` / `RUNTIME_VERIFIED`.

## 19. Next gate

Combined Independent Review + Merge Review of the NC-CLOSE-01C Draft PR.
Implementation remains `NOT_AUTHORIZED`.
