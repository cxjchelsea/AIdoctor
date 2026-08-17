# ADR-10 OpenTelemetry Export / Backend Boundary

| Field | Value |
|---|---|
| ADR ID | ADR-10 |
| Title | OpenTelemetry Export / Backend Boundary |
| Status | `PROPOSED_DECIDED_PENDING_REVIEW` |
| Exact Base | `e9f4baa791009ae16b6cc956a31bcd4bbd428213` |
| Parent batch | `NC-CLOSE-01` |
| Refined batch | `NC-CLOSE-01C` |
| Decision depth | `BOUNDARY_DECISION_REQUIRED` |
| Disposition | `BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED` |
| implementation_authorized | `false` |

## 1. Decision scope

Freeze the **technical telemetry** architecture as six separable layers
and defer the concrete collector/backend.

This ADR does not install OpenTelemetry, provision a hosted backend, or
authorize production telemetry export.

## 2. Context

Target documents describe OpenTelemetry as the future technical
telemetry stack. Current runtime tracing is a self-built HTTP event
path into `execution-trace-service`. Prometheus/Micrometer exist as
**metrics**, not as distributed-trace authority.

ADR-03 already deferred W3C `traceparent` adoption to this batch. That
is a context-propagation question; it is not a hosted-backend selection.

## 3. Layer separation

“OTel” is **not** one component. This ADR treats:

| Layer | Meaning |
|---|---|
| 1. Instrumentation | in-process span/event creation |
| 2. Context propagation | correlation identifiers on the wire |
| 3. Exporter / protocol | how process-local signals leave the process |
| 4. Collector | optional pipeline: batch, retry, redact, sample, route |
| 5. Backend / storage | durable technical-telemetry store |
| 6. Visualization | query/UI over stored signals |

## 4. Current repository evidence

| Layer | Current fact | Class |
|---|---|---|
| Instrumentation | Java AOP / Feign; Python helper unused as service-wide authority | `CODE_CONFIRMED` |
| Propagation | `X-CDP-Id`, `X-Trace-Id`; no `traceparent` / `tracestate` | `CODE_CONFIRMED` |
| Exporter / protocol | HTTP POST to `/api/v1/trace/events`; not OTLP | `CODE_CONFIRMED` |
| Collector | absent | `CODE_CONFIRMED` |
| Backend / storage | `execution-trace-service` / `execution_trace` | `CODE_CONFIRMED` |
| Visualization | `frontend-admin` trace views; Prometheus/Grafana for **metrics** | `CODE_CONFIRMED` / `DOCUMENTED` |
| OTel SDK | `ABSENT` | `CODE_CONFIRMED` |
| OTLP exporter | `ABSENT` | `CODE_CONFIRMED` |
| Hosted OTel backend | `ABSENT` | `CODE_CONFIRMED` |
| Production trace enablement | `execution.trace.enabled` defaults off; production occupancy | `UNKNOWN` |
| OTel equivalence | `NOT_VERIFIED` | — |

## 5. Problem

If “choose Jaeger/Tempo now” is treated as required to close Phase A,
the lane would provision infrastructure without deployment evidence and
could accidentally make a telemetry store a second clinical or audit
authority.

## 6. Architectural invariants

1. Technical observability **MAY** adopt OpenTelemetry-compatible
   instrumentation and export.
2. Preferred future **export protocol** boundary: **OTLP**.
3. OTLP is an architectural target, not an installed dependency.
4. Concrete collector/backend is **DEFERRED**.
5. Telemetry backend must not become clinical SoR, ClinicalDecisionRecord
   authority, ComplianceAudit authority, 01B retrieval DB, or production
   primary clinical DB.
6. Technical exporter failure is normally
   `FAIL_OPEN_WITH_OBSERVABILITY_DEGRADATION` for ordinary business
   execution only.

## 7. Alternatives considered

| ID | Alternative |
|---|---|
| A10-A | Select Jaeger / Tempo / Zipkin / vendor SaaS now |
| A10-B | Keep custom HTTP + `execution-trace-service` as permanent target |
| A10-C | **Selected.** Freeze OTel-compatible / OTLP boundary; defer concrete backend |
| A10-D | Install SDK/collector in this package |

## 8. Selected decision

**A10-C — OTel telemetry/backend boundary frozen; concrete
collector/backend deferred.**

### 8.1 Target boundary

**TARGET**

- Instrumentation: OpenTelemetry-compatible technical spans.
- Propagation: current ADR-03 headers remain; future OTel work may
  **add** W3C context (see ADR-11). `cdp_id` ≠ `trace_id`.
- Export protocol: **OTLP**.
- Collector: optional later pipeline; not required to freeze this
  boundary.
- Backend/storage: later Platform Observability selection.
- Visualization: later; current `frontend-admin` is a legacy consumer,
  not the target backend.

**CURRENT**

`execution-trace-service` remains the legacy implementation.

**DEFERRED**

Concrete collector and hosted/self-hosted backend product.

**NOT_AUTHORIZED**

Production telemetry export enablement.

### 8.2 Enable / disable semantics

Technical telemetry is **optional** relative to business execution.

If the destination is disabled or unconfigured, the process must not
invent a fake clinical or SoR success path. The exporter stays disabled.

### 8.3 Failure semantics

Technical telemetry exporter/backend failure must normally be:

```text
FAIL_OPEN_WITH_OBSERVABILITY_DEGRADATION
```

for ordinary business execution.

Meaning: technical telemetry failure alone does not become application
truth or clinical commit failure.

This rule applies **only** to Technical Span telemetry.

It does **not** apply to:

- State Committer
- ClinicalDecisionRecord durable write
- ComplianceAudit mandatory write
- clinical safety gate

Those authorities are defined elsewhere (ADR-02 / ADR-12). This ADR
does not invent their clinical failure behavior.

### 8.4 Compatibility expectations

- Existing `X-CDP-Id` / `X-Trace-Id` minimum semantics remain (ADR-03).
- Metrics (Prometheus/Micrometer) remain metrics, not trace authority.
- Logs remain logs, not audit authority.
- Shared Contracts v1 `TraceRef` / `AuditRef` semantics unchanged.

## 9. Concrete backend deferral

```text
Concrete backend: DEFERRED
Disposition: BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED
```

Explicitly **not** selected now:

- Jaeger
- Grafana Tempo
- Zipkin
- vendor SaaS APM
- hosted Grafana stack as the trace backend
- cloud telemetry backend

| Deferral field | Value |
|---|---|
| Rationale | No deployment evidence is available to justify a hosted or product backend merely to close Phase A. |
| Future owner | Platform Observability |
| Trigger | NC-CLOSE-04 implementation/equivalence work, or separately authorized production observability enablement |
| Evidence requirement | required signal support; OTLP compatibility; deployment constraints; retention; availability/failure behavior; cost/operational evidence where relevant; privacy/security boundary evidence |
| Stop condition | A telemetry backend becoming clinical SoR, ClinicalDecisionRecord authority, ComplianceAudit authority, 01B retrieval DB, or production primary clinical DB |

## 10. Rationale

Layer separation lets Phase A freeze “how signals leave the process”
without pretending a backend has been chosen or installed.

## 11. Rejected alternatives

- **A10-A** provisions a product without evidence.
- **A10-B** freezes the legacy HTTP store as the target architecture.
- **A10-D** is implementation and is not authorized.

## 12. Tradeoffs

Until a later backend decision, operators keep the legacy store. That
is acceptable because ADR-11 forbids removing it before equivalence
evidence.

## 13. Implementation consequences

Later authorized work may add OTel-compatible instrumentation and an
OTLP exporter. This ADR does not add them and does not modify CI.

## 14. Deferred items

Collector product, backend product, retention SLOs, sampling
percentages, production export enablement.

## 15. Explicit non-goals / forbidden claims

Do **not** read this ADR as:

- OpenTelemetry installed
- OTLP exporter present
- collector or hosted backend provisioned
- production telemetry enabled
- observability correctness proven by NC-CLOSE-03 CI

## 16. Architecture stop conditions

```text
PHASE_A_NC_CLOSURE_ARCHITECTURE_BOUNDARY_REACHED
```

if stating this boundary requires changing the production primary DB or
making telemetry storage a clinical/retrieval authority.

## 17. Evidence truth

| Claim | Label |
|---|---|
| Layer inventory | `CODE_CONFIRMED` |
| OTel runtime | `NOT_IMPLEMENTED` / `ABSENT` |
| OTel equivalence | `NOT_VERIFIED` |
| Production trace enablement | `UNKNOWN` |
| NC-CLOSE-03 CI | `CI_VERIFIED` for engineering baseline only |

## 18. Relationships

- `HARD_INPUT_FOR_NC_CLOSE_04_TRACE_OBSERVABILITY`
- Compatible with ADR-03 / ADR-11 / ADR-12
- Independent of 01B vendor choices

## 19. Decision owner / governance state

Owner: Phase A NC Closure / NC-CLOSE-01C.
State: `PROPOSED_DECIDED_PENDING_REVIEW`.
Not `APPROVED` / `MERGED` / `IMPLEMENTED` / `RUNTIME_VERIFIED`.

## 20. Next gate

Combined Independent Review + Merge Review of the NC-CLOSE-01C Draft PR.
Implementation remains `NOT_AUTHORIZED`.
