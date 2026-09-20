# U04 Implementation Authorization Review v0.1

> Review target:
> AUTH-U04-RUNTIME-IMPL-001
>
> Scope:
> U04 V1 / NON_PRODUCTION_ONLY
>
> This document determines authorization eligibility only. It does not itself grant implementation authorization, merge authorization, routing activation, production authorization, or real-patient traffic.

## 1. Review inputs

Exact readiness head:

`f3c2ed6671f8e13d62b841689030ff772be7ee55`

Readiness verdict:

```text
U04 Implementation Readiness Re-Review v0.3
= PASS

Open readiness blockers
= 0

U04 Implementation Readiness
= READY

U04 Implementation Authorization Readiness
= READY_FOR_IMPLEMENTATION_AUTHORIZATION_REVIEW
```

Frozen package:

```text
RDP-01 = FROZEN / PASS_FOR_READINESS
RDP-02 = APPROVED_AS_PROPOSED / FROZEN / PASS_FOR_READINESS
RDP-03 = FROZEN / PASS_FOR_READINESS
RDP-04 = FROZEN / PASS_FOR_READINESS
RDP-05 = APPROVED_AS_PROPOSED / FROZEN / PASS_FOR_READINESS
RDP-06 = FROZEN / PASS_FOR_READINESS
```

## 2. Review question

Can U04 now receive a narrowly scoped implementation authorization without granting production, live downstream routing, or new clinical/safety truth?

Verdict:

```text
YES
```

under the exact scope below.

## 3. Proposed authorization identity

```text
AUTH-U04-RUNTIME-IMPL-001
= NON_PRODUCTION_ONLY
= FROZEN_RDP01_TO_RDP06_ONLY
= NO_LIVE_DOWNSTREAM_ROUTING
```

This authorization may only be granted later by an explicit repository-owner instruction.

## 4. Authorized implementation scope if separately approved

A later explicit authorization MAY permit implementation of:

### A. U04 consumer admission

Implement the RDP-01 consumer boundary for the governed U03 handoff, including:

- current Clinical State Version validation;
- exact governed release/provenance validation;
- stale/malformed/untrusted fail-closed behavior;
- duplicate/replay identity handling;
- preservation of U03 typed status/disposition/failure semantics.

### B. U04 deterministic Safety Gate owner

Implement exactly the frozen RDP-02 policy:

```text
P0 invalid / stale / untrusted inbound
→ typed admission failure
→ no Gate commit

P1 VALID + HIGH_RISK
→ BLOCKED

P2 U03 Risk Assessment FAILED
→ UNAVAILABLE

P3 required Safety Capability unavailable / failed
→ UNAVAILABLE

P4 scope unavailable / not established
→ UNAVAILABLE

P5 VALID + CAUTION
→ RESTRICTED

P6 VALID + NO_HIGH_RISK_SIGNAL
→ ALLOW
```

With frozen distinctions:

```text
NO_HIGH_RISK_SIGNAL != SAFE / NORMAL
BLOCKED != runtime failure
UNAVAILABLE != known HIGH_RISK
known HIGH_RISK is not erased by technical/dependency failure
```

### C. Controlled Safety Gate state proposal/commit boundary

Implement the RDP-03 boundary:

```text
admitted U04 input
→ U04 decision
→ typed state proposal
→ controlled G2 / StateCommitter boundary
→ committed current-version Safety Gate
```

Implementation may add the minimum non-production adapters/contracts needed to exercise this boundary.

Direct mutation outside controlled state governance remains prohibited.

### D. Downstream route eligibility

Implement the RDP-04 eligibility result only:

```text
ALLOW
→ U05 eligibility

RESTRICTED
→ policy-restricted U05 eligibility with restricted context preserved

BLOCKED
→ ordinary U05 prohibited
→ U11 eligibility may be exposed

UNAVAILABLE
→ ordinary U05 prohibited
→ U14 eligibility may be exposed
```

The implementation MAY produce typed routing eligibility/intent data.

It MUST NOT activate real U05/U11/U14 owner execution or live routing under this authorization.

### E. Current dependency policy

Implement only the approved RDP-05 strategy:

```text
NO_ADDITIONAL_REQUIRED_SAFETY_CAPABILITY_FOR_CURRENT_U04_V1_SLICE

OPTIONAL_SAFETY_CAPABILITY_SET
= EMPTY

U04_V1_SAFETY_CAPABILITY_FALLBACK
= NONE
```

The implementation may consume existing governed infrastructure required by RDP-03/RDP-06.

It may not introduce a new model/tool/API Safety Capability.

### F. Verification support

Implement tests, harnesses, fixtures, workflow/evidence generation and non-production wiring necessary to satisfy RDP-06.

## 5. Explicitly prohibited scope

Even if AUTH-U04-RUNTIME-IMPL-001 is later granted, it MUST NOT authorize:

- modification of frozen RDP-02 Safety Gate semantics;
- modification of frozen RDP-05 dependency policy;
- new medical/safety thresholds;
- a second risk model;
- LLM Safety Gate ownership;
- autonomous Agent-selected safety tools;
- external Safety API dependency;
- optional Safety Capability introduction;
- hidden fallback;
- mutable latest/current dependency refs;
- frontend Safety Gate override;
- direct uncontrolled Clinical State mutation;
- live U05 owner execution;
- live U11 owner execution;
- live U14 owner execution;
- production U03→U04 routing activation;
- production mutation;
- release publication/activation;
- real-patient traffic;
- pediatric/pregnancy/China production expansion;
- unrelated runtime refactor;
- automatic merge.

## 6. Expected implementation boundaries

A future implementation must keep the following conceptual boundaries separate:

```text
U03 handoff
!= U04 admission result

U04 admission result
!= U04 Safety Gate decision

U04 Safety Gate decision
!= state proposal

state proposal
!= committed Safety Gate

committed Safety Gate
!= live downstream execution

routing eligibility
!= downstream owner execution
```

This separation is mandatory for reviewability and fail-closed behavior.

## 7. Required verification gate

A future authorized implementation must prove, at minimum:

1. valid current-version U03 handoff is admitted;
2. stale/malformed/untrusted handoff fails closed before Gate decision;
3. mixed/wrong release or provenance fails closed;
4. VALID + NO_HIGH_RISK_SIGNAL produces ALLOW only when all frozen admission/scope conditions hold;
5. VALID + CAUTION produces RESTRICTED;
6. VALID + HIGH_RISK produces BLOCKED;
7. U03 FAILED produces UNAVAILABLE;
8. scope unavailable/not established produces UNAVAILABLE;
9. known HIGH_RISK is not erased by technical/dependency failure;
10. conflicting mutually exclusive U03 dispositions do not silently choose a permissive Gate;
11. no additional Safety Capability is called;
12. hidden fallback introduction is rejected;
13. mutable latest/current dependency refs are rejected;
14. exactly one Gate result exists for one accepted current-version input;
15. controlled state proposal/commit path is used;
16. stale Gate state is invalidated/re-evaluated on source-version change;
17. BLOCKED/UNAVAILABLE cannot expose ordinary U05 eligibility;
18. U04 and U14 do not both own the final business route;
19. replay/idempotency does not duplicate commit or routing eligibility effects;
20. frontend/model/agent cannot override Gate;
21. no live U05/U11/U14 execution occurs in the authorized implementation slice;
22. no production mutation or real-patient traffic occurs;
23. exact implementation SHA is bound to durable evidence;
24. full relevant regression passes;
25. independent implementation/evidence review passes before merge authorization review.

If an expected Gate outcome is not covered by the frozen RDP-02/RDP-05 package, execution must stop as:

```text
SAFETY_POLICY_EXPECTATION_GAP
```

and return to governance rather than inventing an answer.

## 8. Merge / evidence governance

Implementation, verification and merge remain separate stages:

```text
AUTHORIZATION
!= IMPLEMENTED
!= VERIFIED
!= INDEPENDENTLY_REVIEWED
!= MERGE_AUTHORIZED
!= MERGED
!= PMV_PASS
```

Any future implementation PR must remain Draft until its verification and independent review gates are complete.

Merge, if later authorized, must use standard merge commit only.

Squash, rebase and auto-merge remain unauthorized unless separately changed by explicit governance.

## 9. Authorization review verdict

```text
U04 Implementation Authorization Review
= PASS

AUTH-U04-RUNTIME-IMPL-001
= ELIGIBLE_FOR_EXPLICIT_REPOSITORY_OWNER_AUTHORIZATION

Permitted authorization shape:
NON_PRODUCTION_ONLY
FROZEN_RDP01_TO_RDP06_ONLY
NO_LIVE_DOWNSTREAM_ROUTING

Implementation Authorization
= NOT_GRANTED_BY_THIS_REVIEW

U04 Owner Execution
= NOT_AUTHORIZED_BY_THIS_REVIEW

U04 Routing Activation
= NOT_AUTHORIZED

Clinical Runtime Production
= NOT_ENABLED

Production Authorization
= BLOCKED

Real-patient traffic
= NOT_AUTHORIZED
```

## 10. Next permitted step

A separate explicit repository-owner instruction may grant:

```text
AUTH-U04-RUNTIME-IMPL-001
= AUTHORIZED
/ NON_PRODUCTION_ONLY
/ FROZEN_RDP01_TO_RDP06_ONLY
/ NO_LIVE_DOWNSTREAM_ROUTING
```

Only after that explicit authorization may U04 runtime implementation begin.
