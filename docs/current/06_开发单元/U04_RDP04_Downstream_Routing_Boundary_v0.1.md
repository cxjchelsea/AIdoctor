# U04 RDP-04 Downstream Routing and Side-effect Boundary v0.1

> Scope: routing eligibility after a committed/current U04 Safety Gate result.

## 1. Preconditions

Routing may consume only the current committed U04 Safety Gate bound to the current Clinical State Version.
Stale/uncommitted U04 output is not routable.

## 2. Gate-to-route boundary

For the governed A1 bootstrap path:

```text
ALLOW
→ if BootstrapArchitectureBindingRef = A1
   and A1 bootstrap F3 effect is required / not current:
   PRE_READINESS_A1_F3_C03_ELIGIBLE

→ if A1 bootstrap F3 effect is already current / current-version revalidated:
   U05_ELIGIBLE
```

For non-A1 paths, the previously frozen ordinary-path projection remains unchanged unless separately amended.

`RESTRICTED` -> may expose A1 pre-readiness eligibility or eventual U05 eligibility **only** when the governed restricted policy explicitly permits that consequence; `restricted_context_ref` must be preserved. Ordinary continuation is not assumed.

`BLOCKED` -> ordinary U05 continuation prohibited; high-risk escalation/safe-exit eligibility may be exposed to U11 according to frozen business-loop rules.

`UNAVAILABLE` -> ordinary U05 continuation prohibited; failure-handling eligibility may be exposed to U14.

## 3. Ownership split

U04 answers only: whether ordinary clinical continuation is allowed/restricted/blocked/unavailable.
U14 answers only: how an execution/capability failure is recovered, degraded, safely exited, or terminally failed.

U04 and U14 must not both own the same final business route.

## 4. Forbidden routes

- BLOCKED or UNAVAILABLE -> A1 pre-readiness or ordinary U05;
- HIGH_RISK -> ordinary continuation merely because a planner requests it;
- direct U11/U14 execution from an uncommitted U04 proposal where canonical gate state is required;
- routing from stale U03/U04 versions;
- frontend/model/agent override of committed gate.

## 5. Side-effect idempotency

For A1, one current committed U04 result may create at most one governed `routing_authorization_id` for the same business-event / current-version / selected-path identity.

A routing authorization may sequence Scheduler consequences, but U04 does not directly execute downstream Units.

A1 VS-B semantics:

```text
current U04 Gate @ Vn
→ routing authorization RA-n
→ PRE_READINESS_A1_F3_C03_ELIGIBLE
→ canonical F3 state commit
→ Clinical State Version advances
→ RA-n becomes STALE / NON_ROUTABLE
→ Safety revalidation barrier
→ new current U04 Gate
→ new routing authorization RA-k
→ if same F3 canonical effect is current-version revalidated:
   U05_ELIGIBLE
```

RA-k must not retrigger the same A1 pre-readiness effect merely because a new Gate/version exists.

Replay must not duplicate routing authorization or downstream execution.

## 6. Verdict

U04-RDP-04 A1 affected scope = AMENDED / INDEPENDENT_REVIEW_PENDING
Unaffected routing semantics = prior FROZEN baseline retained
U04 implementation authorization = NOT_GRANTED


---

## 6. A1 controlled amendment details

> Authorization: `AUTH-U05-A1-FROZEN-AMEND-001`  
> Reviewed design source: PR #138 exact head `7a62cc6f3b0cd9d803590594394bbed433351fab`

### 6.1 Ownership remains unchanged

U04 still answers only：

```text
ALLOW
RESTRICTED
BLOCKED
UNAVAILABLE
```

U04 does **not**：

```text
execute U06
invoke C03
interpret F3 sufficiency
select bootstrap architecture at runtime
decide Clinical Readiness
```

`BootstrapArchitectureBindingRef = A1` is a governed configuration/binding input to routing projection, not a U04 Safety decision.

### 6.2 A1 eligibility projection

A1 typed eligibility：

```text
PRE_READINESS_A1_F3_C03_ELIGIBLE
```

must bind at least：

```text
consultation_id
cdp_id
clinical_state_version
u04_gate_ref
gate_value
BootstrapArchitectureBindingRef = A1
restricted_context_ref when applicable
routing_authorization_id
validity
routing policy/version
trace refs
```

Eligibility projection：

```text
!= Unit invocation
!= Clinical State truth
!= Clinical Readiness
```

### 6.3 Staleness

A1 eligibility / routing authorization becomes non-routable when：

```text
u04_gate_ref is no longer current
or Clinical State Version advances
or BootstrapArchitectureBindingRef changes
or restricted context becomes incompatible
or A1 bootstrap effect/revalidation becomes stale/failed
```

Stale eligibility must not be silently rebound.

### 6.4 Gate semantics retained

```text
BLOCKED
→ no A1 pre-readiness
→ no U05

UNAVAILABLE
→ no A1 pre-readiness
→ no U05
```

Existing U11 / U14 ownership remains unchanged.

### 6.5 Current amendment status

```text
U04-RDP-04 A1 amendment
= APPLIED / INDEPENDENT_REVIEW_PENDING

Re-freeze
= NOT_YET_GRANTED

Runtime implementation
= NOT_AUTHORIZED
```
