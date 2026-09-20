# U04 Implementation Readiness Re-Review v0.1

> Review target: U04 Safety Gate decision unit
> Review type: post-U03-closure readiness re-review
> Upstream reviewed U03 closure head: 35b976d9744c2a17598067cb77ba51e52dfa0183
> Upstream reviewed stacked aggregate: 99566a5bfe9bee437316299d65101b1bc8a3e398
>
> This document determines U04 implementation readiness only.
> It does not grant U04 implementation authorization, U04 owner execution, routing activation, production authorization, or real-patient traffic.

## 1. Re-review question

The historical blocker was:

```text
BLOCKED_BY_U03_RUNTIME_CLINICAL_DEPENDENCY
```

U03 Clinical Dependency Closure Review has now passed on the reviewed stacked aggregate:

```text
U03 Clinical Dependency Closure Review = PASS
U03 Clinical Dependency = CLOSED / STACKED_AGGREGATE_SCOPE
```

Can U04 now move directly to Implementation Authorization Review?

Verdict: NO.

The U03-dependent blocker is removed, but U04 still lacks its own implementation-readiness package.

## 2. Frozen U04 business authority

The frozen Phase-6 unit definition establishes U04 as the Safety Gate decision owner.

Inputs include, as applicable:
- current Clinical Risk / U03 result;
- explicit Risk Assessment FAILED;
- required Safety Capability availability;
- currently applicable Safety Policy / Scope conditions.

U04 must form exactly one Safety Gate state:

```text
ALLOW
RESTRICTED
BLOCKED
UNAVAILABLE
```

Required business consequences include:

```text
ALLOW / legally permitted RESTRICTED -> U05
HIGH_RISK + RESTRICTED/BLOCKED -> Consultation SAFETY_ESCALATED -> U11
Risk Assessment FAILED / required Safety Capability unavailable
-> Gate = UNAVAILABLE or a separately approved stricter gate
-> ordinary clinical continuation prohibited
-> U14 determines recovery / safe exit / failed-terminal handling
```

Frozen invariants include:

```text
HIGH_RISK -> Safety Gate must not remain ordinary ALLOW
Safety capability unavailable -> Gate != ALLOW
Risk Assessment FAILED -> ordinary continuation prohibited until resolved
U04 and U14 must not compete for the same final business decision
Frontend / Model / free Agent may not override Safety Gate
```

These frozen business rules define the unit boundary but do not yet define an implementation-authorizable owner package.

## 3. Upstream U03 dependency re-review

Current upstream state:

```text
Gate A = PASS
Gate B = PASS / GOVERNED_CONTENT_READY
Gate C = PASS
CD-07 = COMPLETE / VERIFIED / MERGED / PMV_PASS
CD-07R = COMPLETE / VERIFIED / MERGED / PMV_PASS
CD-08 = PASS / COMPLETE_ON_STACKED_AGGREGATE
Open blocking U03 clinical-dependency findings = 0
```

Existing producer-side boundary:

`U03_CD07_U03_to_U04_Outbound_Boundary_Contract_v0.1.md`

Existing runtime producer:

`U03OutboundProducer / U03OutboundHandoff`

The producer-side boundary preserves, as applicable:
- Clinical State Version;
- Thread / Run / Event / trace correlation;
- U03 execution/result identity;
- structured U03 disposition/status;
- exact knowledge/rule/coverage/policy/policy-pair refs;
- evidence/provenance refs;
- insufficiency/scope/failure information;
- proposal/commit identity where applicable.

CD-07/CD-08 verification also proves unauthorized U04 owner execution remains fail-closed.

Therefore:

```text
BLOCKED_BY_U03_RUNTIME_CLINICAL_DEPENDENCY
= CLOSED / REMOVED
```

Producer readiness does not supply U04's own consumer/policy/owner implementation package.

## 4. U04 intrinsic readiness blockers

### U04-RDP-01 — Consumer-side inbound contract

Status: OPEN / BLOCKING.

Missing: a U04-owned consumer contract that defines exactly how U03 outbound fields are accepted, rejected, version-checked, release-checked, and normalized into U04 Safety Gate inputs.

Must define at minimum:
- required vs optional U03 handoff fields;
- exact Clinical State Version acceptance rule;
- stale/mismatched handoff behavior;
- exact release/provenance acceptance;
- treatment of U03 VALID vs FAILED;
- treatment of scope/insufficiency status;
- malformed/missing handoff fail-closed behavior;
- idempotency / replay identity.

Producer contract exists != consumer contract exists.

### U04-RDP-02 — Safety Gate decision policy / owner semantics

Status: OPEN / BLOCKING.

Missing: a governed U04 policy that uniquely maps accepted U04 inputs to ALLOW / RESTRICTED / BLOCKED / UNAVAILABLE.

A future policy must cover at minimum:
- NO_HIGH_RISK_SIGNAL;
- CAUTION;
- HIGH_RISK;
- U03 Risk Assessment FAILED;
- required Safety Capability unavailable;
- applicable scope unavailable / not established;
- conflicting safety inputs;
- stale U03 input;
- dependency failure.

Clinical or safety semantics not already frozen must be supplied by the appropriate Medical / Safety / Product Owner. Developers must not invent them.

### U04-RDP-03 — Safety Gate state ownership and mutation contract

Status: OPEN / BLOCKING.

Missing: a frozen contract for how the unique Safety Gate state is represented, version-bound, proposed, validated, committed, invalidated, and re-evaluated.

Must preserve:

```text
Capability Result != Safety Gate state
Safety Gate decision != uncontrolled Clinical State mutation
```

Any canonical mutation must remain under G2 / controlled StateCommitter authority.

The contract must define:
- owner of Safety Gate business interpretation;
- proposal vs committed gate state;
- Clinical State Version binding;
- stale invalidation;
- duplicate/idempotent application;
- forbidden direct mutation paths.

### U04-RDP-04 — Downstream routing / side-effect boundary

Status: OPEN / BLOCKING.

Missing: a deterministic contract from committed U04 result to downstream business effect.

Required branches:

```text
ALLOW / permitted RESTRICTED -> U05 eligibility
RESTRICTED/BLOCKED high-risk -> SAFETY_ESCALATED / U11 path
UNAVAILABLE -> ordinary continuation prohibited -> U14 failure handling eligibility
```

Must explicitly prevent:
- U04 and U14 both owning the same final route;
- U05 continuing after BLOCKED/UNAVAILABLE;
- HIGH_RISK ordinary-path continuation;
- direct U11/U14 execution before committed U04 state when canonical state is required;
- routing from stale U03/U04 state.

### U04-RDP-05 — Required Safety Capability / dependency policy

Status: OPEN / BLOCKING.

The frozen unit says required Safety Capability availability is a U04 input, but the repository does not yet freeze:
- which capabilities are mandatory for this V1 U04 slice;
- capability authorization scope;
- availability / timeout / dependency-failure semantics;
- optional vs required vs prohibited dependencies;
- exact fail-closed behavior per dependency;
- fallback authorization, if any.

This is not implementation-detail-only because capability unavailability can change the Safety Gate business result.

### U04-RDP-06 — Verification / evidence package

Status: OPEN / BLOCKING.

Missing: a U04-specific verification plan and governed test/evidence set.

At minimum it must verify:
1. NO_HIGH_RISK_SIGNAL cannot be silently reinterpreted as SAFE/NORMAL.
2. HIGH_RISK cannot produce ordinary ALLOW.
3. Risk Assessment FAILED cannot produce ALLOW.
4. required Safety Capability unavailable cannot produce ALLOW.
5. stale/malformed U03 handoff fails closed.
6. wrong/mixed release/provenance inputs fail closed.
7. exactly one Safety Gate result is produced for one accepted business input.
8. no direct state mutation bypasses G2 / StateCommitter.
9. BLOCKED/UNAVAILABLE cannot route to normal U05 flow.
10. U04 and U14 do not double-route.
11. Frontend/Model/Agent cannot override Gate.
12. replay/idempotency does not duplicate state or routing effects.
13. trace/audit evidence binds exact state/run/policy/capability versions.
14. no production mutation or real-patient traffic is required for verification.

A later implementation must have durable exact-SHA evidence and independent review before merge.

## 5. Non-blocking items at readiness stage

Once RDP-01..06 are frozen, these are future implementation deliverables rather than readiness blockers:
- concrete Java class names;
- concrete repository/table layout;
- exact API endpoint names;
- framework wiring;
- production deployment;
- U05/U11/U14 implementation completion, provided consumer boundaries are sufficiently frozen for U04 output;
- real-patient integration.

## 6. Scope exclusions

This re-review does not authorize or require:
- production U03->U04 routing;
- U04 production activation;
- U14 implementation;
- real-patient traffic;
- pediatric/pregnancy/China production expansion;
- new medical thresholds;
- new emergency-care instructions;
- release publication.

Any new clinical/safety truth discovered while preparing U04 RDP-01..06 must return to governed Medical/Safety/Product Owner review.

## 7. Re-review matrix

| Item | Result |
|---|---|
| U03 clinical dependency | PASS / CLOSED |
| U03->U04 producer contract | AVAILABLE |
| U03 outbound runtime producer | AVAILABLE / VERIFIED |
| Unauthorized U04 execution guard | VERIFIED |
| U04 consumer-side inbound contract | MISSING / BLOCKING |
| U04 Safety Gate decision policy | MISSING / BLOCKING |
| U04 state ownership / mutation contract | MISSING / BLOCKING |
| U04 downstream routing boundary | MISSING / BLOCKING |
| U04 required capability/dependency policy | MISSING / BLOCKING |
| U04 verification/evidence plan | MISSING / BLOCKING |

## 8. Readiness verdict

```text
Historical blocker:
BLOCKED_BY_U03_RUNTIME_CLINICAL_DEPENDENCY
= CLOSED / REMOVED

New intrinsic readiness blockers:
U04-RDP-01 = OPEN / BLOCKING
U04-RDP-02 = OPEN / BLOCKING
U04-RDP-03 = OPEN / BLOCKING
U04-RDP-04 = OPEN / BLOCKING
U04-RDP-05 = OPEN / BLOCKING
U04-RDP-06 = OPEN / BLOCKING

U04 Implementation Readiness
= NOT_READY

U04 Implementation Authorization Readiness
= NOT_READY

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

## 9. Next permitted work

Readiness-package preparation only:

```text
R1 U04 consumer-side inbound contract
R2 U04 Safety Gate policy / owner semantics
R3 U04 state ownership + mutation contract
R4 U04 downstream routing / side-effect contract
R5 U04 Safety Capability / dependency policy
R6 U04 verification & durable evidence plan
```

After R1-R6 are frozen, perform a new U04 Implementation Readiness Re-Review.

Only if that later review passes may U04 enter a separate Implementation Authorization Review.

No U04 runtime implementation is authorized by this document.
