# NC-CLOSE-01C Platform & Observability Boundaries

> Dated: 2026-08-17
>
> Classification: `ARCHITECTURE_DECISION_ONLY` / `NO IMPLEMENTATION` / `DOCS_ONLY`
>
> Lane: `PHASE_A_NC_CLOSURE`
>
> Parent batch: `NC-CLOSE-01` = ADR Foundation (umbrella implementation
> remains `NOT_AUTHORIZED`)
>
> Refined batch: `NC-CLOSE-01C`
>
> Exact Enterprise Base: `e9f4baa791009ae16b6cc956a31bcd4bbd428213`
>
> Base provenance: PR #45 / NC-CLOSE-03 `MERGED_AND_VERIFIED`
>
> Authorization token:
> `NC_CLOSE_01C_ADR_DECISION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> ADR status: `PROPOSED_DECIDED_PENDING_REVIEW`
>
> implementation_authorized: `false`

## 1. Purpose

This is the integration / index artifact for the four NC-CLOSE-01C
architecture decisions. It exists to freeze platform secret-access and
observability ownership without authorizing implementation.

It does **not** authorize OpenTelemetry installation, Secret Manager
implementation, real credentials, trace migration, event-schema
implementation, NC-CLOSE-04, 01B, clinical runtime, or production.

## 2. Decision package

| ADR | File | Decision depth | Central selected decision |
|---|---|---|---|
| ADR-09 | [adr-09-secret-manager-mechanics.md](./adr-09-secret-manager-mechanics.md) | `BOUNDARY_DECISION_REQUIRED` | Secret-access boundary frozen; concrete Secret Manager backend deferred. |
| ADR-10 | [adr-10-otel-backend-boundary.md](./adr-10-otel-backend-boundary.md) | `BOUNDARY_DECISION_REQUIRED` | OTel telemetry/backend layers frozen; concrete collector/backend deferred. |
| ADR-11 | [adr-11-aop-to-otel-migration.md](./adr-11-aop-to-otel-migration.md) | `FULL_DECISION_REQUIRED` | Legacy AOP / execution-trace → phased OTel bridge/migration; no immediate replacement. |
| ADR-12 | [adr-12-trace-event-ownership.md](./adr-12-trace-event-ownership.md) | `FULL_DECISION_REQUIRED` | Technical Span / AgentEvent / ClinicalDecisionRecord / ComplianceAudit have separate ownership and authority. |

Each ADR status is `PROPOSED_DECIDED_PENDING_REVIEW`.
None is `APPROVED`, `MERGED`, `IMPLEMENTED`, or `RUNTIME_VERIFIED`.

```text
ADR-09 disposition: BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED
ADR-10 disposition: BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED
ADR-11 disposition: FULL_DECISION / PHASED_LEGACY_BRIDGE_TO_OTEL
ADR-12 disposition: FULL_DECISION / SEPARATE_OWNERSHIP_MODEL
```

## 3. Control state that must remain true

```text
NC-CLOSE-01:                 NOT_AUTHORIZED as umbrella implementation
NC-CLOSE-01A ADR Decision:   MERGED_AND_VERIFIED
NC-CLOSE-01A implementation: NOT_AUTHORIZED
NC-CLOSE-01B:                NOT_AUTHORIZED
NC-CLOSE-01C ADR Decision:   PROPOSED_DECIDED_PENDING_REVIEW
NC-CLOSE-01C implementation: NOT_AUTHORIZED
NC-CLOSE-03:                 MERGED_AND_VERIFIED
NC-CLOSE-04:                 NOT_AUTHORIZED
A7-NC:                       COMPLETE
A7-CL:                       BLOCKED
A7:                          NOT_COMPLETE
A8–A11:                      NOT_AUTHORIZED
Clinical Runtime:            NOT_ENABLED
Production:                  BLOCKED
Phase B:                     NOT_AUTHORIZED
```

## 4. Required dependency / consistency matrix

```text
ADR-09  Secret-access ownership
   soft → later production secret/provider gates
ADR-10  Telemetry layer boundary
   hard → NC-CLOSE-04 Trace / Observability
ADR-11  Legacy AOP → OTel migration strategy
   hard → NC-CLOSE-04 Trace / Observability
ADR-12  Event-class ownership
   hard → NC-CLOSE-04 Trace / Observability
```

| Check | Result |
|---|---|
| ADR-09 does not select a vendor or enable provider credentials | **Consistent** |
| ADR-10 separates instrumentation / export / collector / backend / visualization | **Consistent** |
| ADR-10 backend is not clinical SoR, audit authority, or 01B retrieval DB | **Consistent** |
| ADR-11 keeps ADR-03 `X-CDP-Id` / `X-Trace-Id` as current minimum headers | **Consistent** |
| ADR-11 forbids removing legacy AOP/trace before equivalence evidence | **Consistent** |
| ADR-12 four classes remain distinct; OTel span is not clinical/audit SoR | **Consistent** |
| ADR-02: checkpoint ≠ clinical SoR; State Committer remains commit boundary | **Consistent** |
| ADR-03: public protocol remains versioned HTTP/REST JSON | **Consistent** |
| ADR-04: Python Runtime remains separate from Model Runtime | **Consistent** |
| ADR-05: LangGraph, if adopted, remains internal controlled Runtime engine | **Consistent** |
| Shared Contracts v1 semantics unchanged | **Consistent** |
| A7-NC not reopened | **Consistent** |
| 01B not decided or implemented | **Consistent** |
| NC-CLOSE-03 CI not modified | **Consistent** |
| NC-CLOSE-04 not implemented | **Consistent** |

```text
Contradictions: 0
Package coherent: YES
```

## 5. Relationship to NC-CLOSE-01A

01A decisions remain authoritative and are not reopened:

- ADR-02: checkpoint ≠ clinical SoR; future State Committer is the only
  authoritative writer.
- ADR-03: public Java↔Python protocol is versioned HTTP/REST JSON;
  `X-CDP-Id` and `X-Trace-Id` remain the current minimum correlation
  headers. W3C `traceparent` / `tracestate` may be **added** later as
  technical context; they must not replace `cdp_id` with `trace_id`.
- ADR-04: canonical future Python Runtime package remains
  `packages/python_runtime`; it consumes Model Runtime and does not
  absorb it.
- ADR-05: LangGraph, if adopted, is an internal Python Runtime engine
  only. Java remains the current production orchestrator.

If a later 01C implementation would contradict these decisions:

```text
NC_CLOSE_01C_CONFLICT_WITH_01A_DECISION
```

## 6. Relationship to NC-CLOSE-01B

01B remains `NOT_AUTHORIZED`.

This package does not select primary DB, pgvector, Milvus, BM25, or
Neo4j. An OTel backend, if later selected, is **not** a clinical
primary database, retrieval database, or knowledge authority.

## 7. Relationship to NC-CLOSE-03

NC-CLOSE-03 remains `MERGED_AND_VERIFIED`.

This package does not modify `.github/workflows/ci.yml` and does not
add gitleaks, state-writer checks, OTel validation, or trace tests.
Those require separately authorized implementation or CI work.

## 8. Relationship to NC-CLOSE-04

NC-CLOSE-04 remains `NOT_AUTHORIZED`.

| 01C decision | Input class for NC-CLOSE-04 |
|---|---|
| ADR-10 | `HARD_INPUT_FOR_NC_CLOSE_04_TRACE_OBSERVABILITY` |
| ADR-11 | `HARD_INPUT_FOR_NC_CLOSE_04_TRACE_OBSERVABILITY` |
| ADR-12 | `HARD_INPUT_FOR_NC_CLOSE_04_TRACE_OBSERVABILITY` |
| ADR-09 | `SOFT_PLATFORM_INPUT` unless a later authorized implementation specifically requires managed secrets |

This package documents those dependencies. It does not implement
Workflow / Trace closure.

## 9. Explicit no-implementation statement

```text
implementation_authorized: false
```

Forbidden in this package and by this token:

- OpenTelemetry SDK / OTLP exporter / collector / Jaeger / Tempo / Zipkin
- Secret Manager SDK / Vault / managed secret integration
- real credentials / provider keys
- AOP, Feign, TraceContext, execution-trace-service, or frontend-admin edits
- AgentEvent / ClinicalDecisionRecord / ComplianceAudit implementation
- AuditTrail rewrite, migrations, database tables
- Shared Contracts v1 semantic change
- NC-CLOSE-04 or 01B implementation

## 10. Repository evidence used

This package was written from the NC-CLOSE-01C Authorization Assessment
on Base `e9f4baa791009ae16b6cc956a31bcd4bbd428213`, plus CODE_CONFIRMED
source inspection of current legacy mechanisms.

| Area | Finding | Class |
|---|---|---|
| Java AOP / annotation / client | `ExecutionTraceAspect`, `@TraceExecution`, `TraceServiceClient` | `CODE_CONFIRMED` |
| Feign headers | `X-CDP-Id`, `X-Trace-Id`; no W3C runtime | `CODE_CONFIRMED` |
| Trace storage / UI | `execution-trace-service` + `frontend-admin` consumers | `CODE_CONFIRMED` |
| Python trace helper | library exists; not service-wide authority | `CODE_CONFIRMED` |
| OTel SDK / OTLP / collector | absent | `CODE_CONFIRMED` |
| Secret Manager | absent; env / Spring YAML / Compose-style access | `CODE_CONFIRMED` |
| AgentEvent / ClinicalDecisionRecord / ComplianceAudit types | designed, not implemented | `DOCUMENTED` + `CODE_CONFIRMED` absence |
| AuditTrail mixed predecessor | `tool_call` / `cdp_update` / `agent_decision` | `CODE_CONFIRMED` |
| Model Runtime secret/provider access | forbidden by existing tests | `CODE_CONFIRMED` / `TEST_VERIFIED` for that package |
| Enterprise CI | Phase A CI MVP run `32000285502` success | `CI_VERIFIED` for engineering baseline only |
| Observability correctness / OTel equivalence | not claimed | not `RUNTIME_VERIFIED` |

## 11. What this package does not do

- implement observability, secret retrieval, or event stores
- install or configure telemetry backends
- change Feign, contracts, CI, Docker, or runtime packages
- reopen A7-NC
- enable Clinical Runtime or production
- claim regulatory compliance

## 12. Next gate

```text
Combined Independent Review + Merge Review
of the exact Draft PR Head that contains this package.
```

Do not mark Ready from the authoring task.
Do not merge from the authoring task.
Do not implement NC-CLOSE-01C from this documentation.
Do not start NC-CLOSE-04 or 01B.

## 13. Success meaning

Successful publication of this package means only:

```text
NC-CLOSE-01C ADR decisions: PROPOSED_DECIDED_PENDING_REVIEW
```

It does not mean approved, merged, implementation-authorized, NC Closure
complete, Frozen Baseline, or production-ready.
