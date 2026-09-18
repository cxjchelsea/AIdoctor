# U04 Implementation Readiness Re-Review v0.3

> Review target: U04 Safety Gate decision unit
> Review basis: U03 clinical dependency closed + U04 RDP-01..06 package complete
> Current scope: U04 V1 / NON_PRODUCTION_ONLY
>
> This review determines implementation readiness only.
> It does not grant U04 implementation authorization, owner execution, routing activation, production authorization, or real-patient traffic.

## 1. Upstream prerequisite

```text
U03 Clinical Dependency Closure Review
= PASS

U03 Clinical Dependency
= CLOSED / STACKED_AGGREGATE_SCOPE

BLOCKED_BY_U03_RUNTIME_CLINICAL_DEPENDENCY
= CLOSED / REMOVED
```

The verified U03 producer-side outbound boundary remains available.

## 2. U04 readiness package status

```text
U04-RDP-01 Consumer-side inbound contract
= FROZEN / PASS_FOR_READINESS

U04-RDP-02 Safety Gate policy / owner semantics
= APPROVED_AS_PROPOSED
= FROZEN / PASS_FOR_READINESS

U04-RDP-03 Safety Gate state ownership / mutation contract
= FROZEN / PASS_FOR_READINESS

U04-RDP-04 downstream routing / side-effect boundary
= FROZEN / PASS_FOR_READINESS

U04-RDP-05 Safety Capability / dependency policy
= APPROVED_AS_PROPOSED
= FROZEN / PASS_FOR_READINESS

U04-RDP-06 verification / durable evidence plan
= FROZEN / PASS_FOR_READINESS
```

Open readiness blockers:

```text
0
```

## 3. Frozen U04 Safety Gate policy

Current approved non-production V1 policy:

```text
P0 invalid / stale / untrusted inbound
→ typed admission failure
→ no Gate commit

P1 VALID + HIGH_RISK
→ BLOCKED
→ ordinary U05 prohibited
→ U11 high-risk/safe-exit eligibility

P2 U03 Risk Assessment FAILED
→ UNAVAILABLE
→ ordinary U05 prohibited
→ U14 failure-handling eligibility

P3 required Safety Capability unavailable / failed
→ UNAVAILABLE
→ ordinary U05 prohibited
→ U14 failure-handling eligibility

P4 scope unavailable / not established
→ UNAVAILABLE
→ ordinary U05 prohibited
→ U14 failure-handling eligibility

P5 VALID + CAUTION
→ RESTRICTED
→ U05 eligibility allowed only with restricted context preserved

P6 VALID + NO_HIGH_RISK_SIGNAL
→ ALLOW
→ ordinary U05 eligibility
```

Important invariant:

```text
NO_HIGH_RISK_SIGNAL != SAFE / NORMAL
```

Known HIGH_RISK is not erased by technical/dependency failure.

## 4. Frozen dependency policy

Current U04 V1 strategy:

```text
NO_ADDITIONAL_REQUIRED_SAFETY_CAPABILITY_FOR_CURRENT_U04_V1_SLICE

OPTIONAL_SAFETY_CAPABILITY_SET
= EMPTY

U04_V1_SAFETY_CAPABILITY_FALLBACK
= NONE
```

The current U04 V1 may consume only:

- governed U03 typed handoff;
- approved U04 Safety Gate Policy;
- current scope / authorization context;
- existing governed G2 / StateCommitter / trace/audit infrastructure.

Any new external/model/tool Safety Capability remains prohibited until separately governed.

## 5. Implementation boundary is sufficiently defined

The current package now defines:

1. what U04 may accept from U03;
2. how stale/malformed/untrusted input fails closed;
3. the unique Safety Gate vocabulary;
4. the owner-approved decision mapping and precedence;
5. how canonical Gate state is proposed/committed/invalidated;
6. which downstream route is eligible for each Gate;
7. which dependencies are allowed or prohibited;
8. that no additional Safety Capability or fallback exists in the current slice;
9. what exact verification families and durable evidence are required;
10. when implementation must stop as a governance expectation gap instead of inventing truth.

Concrete Java class names, method signatures, repository/table layout, and framework wiring remain implementation details and do not block readiness.

## 6. Remaining authorization boundary

Readiness does not equal authorization.

The following remain prohibited until a separate explicit authorization:

```text
- U04 runtime implementation
- U04 owner execution
- U03→U04 routing activation
- U04→U05/U11/U14 live routing
- production state mutation
- production release activation
- real-patient traffic
- any new Safety Capability
- any fallback
- pediatric/pregnancy/China production expansion
```

## 7. Required future implementation verification

Any later authorized implementation must at minimum prove:

- exact U03 handoff admission;
- stale/malformed/mixed-release fail closed;
- NO_HIGH_RISK_SIGNAL → conditional ALLOW only under frozen preconditions;
- CAUTION → RESTRICTED;
- HIGH_RISK → BLOCKED;
- U03 FAILED → UNAVAILABLE;
- scope unavailable → UNAVAILABLE;
- unauthorized dependency/fallback attempts rejected;
- exactly one Gate result per accepted current-version input;
- controlled G2/StateCommitter mutation;
- stale invalidation;
- no ordinary U05 after BLOCKED/UNAVAILABLE;
- U04/U14 no double-route;
- replay/idempotency no duplicate effects;
- frontend/model/agent cannot override Gate;
- exact-SHA durable artifact and checksums;
- independent implementation/evidence review before merge.

## 8. Re-review verdict

```text
U04 Implementation Readiness Re-Review v0.3
= PASS

Open readiness blockers
= 0

U04 Implementation Readiness
= READY

U04 Implementation Authorization Readiness
= READY_FOR_IMPLEMENTATION_AUTHORIZATION_REVIEW

U04 Implementation Authorization
= NOT_GRANTED

U04 Owner Execution
= NOT_AUTHORIZED

U04 Routing Activation
= NOT_AUTHORIZED

Clinical Runtime Production
= NOT_ENABLED

Production Authorization
= BLOCKED

Real-patient traffic
= NOT_AUTHORIZED
```

## 9. Next permitted step

```text
U04 Implementation Authorization Review
```

That review may determine whether a narrowly scoped NON_PRODUCTION_ONLY U04 implementation authorization is eligible for explicit repository-owner approval.

This readiness review does not itself grant that authorization.
