# U04 RDP-04 Downstream Routing and Side-effect Boundary v0.1

> Scope: routing eligibility after a committed/current U04 Safety Gate result.

## 1. Preconditions

Routing may consume only the current committed U04 Safety Gate bound to the current Clinical State Version.
Stale/uncommitted U04 output is not routable.

## 2. Gate-to-route boundary

`ALLOW` -> may make U05 ordinary-path eligibility available.

`RESTRICTED` -> may permit only the restricted downstream path explicitly allowed by the governed U04 policy; ordinary continuation is not assumed.

`BLOCKED` -> ordinary U05 continuation prohibited; high-risk escalation/safe-exit eligibility may be exposed to U11 according to frozen business-loop rules.

`UNAVAILABLE` -> ordinary U05 continuation prohibited; failure-handling eligibility may be exposed to U14.

## 3. Ownership split

U04 answers only: whether ordinary clinical continuation is allowed/restricted/blocked/unavailable.
U14 answers only: how an execution/capability failure is recovered, degraded, safely exited, or terminally failed.

U04 and U14 must not both own the same final business route.

## 4. Forbidden routes

- BLOCKED or UNAVAILABLE -> ordinary U05;
- HIGH_RISK -> ordinary continuation merely because a planner requests it;
- direct U11/U14 execution from an uncommitted U04 proposal where canonical gate state is required;
- routing from stale U03/U04 versions;
- frontend/model/agent override of committed gate.

## 5. Side-effect idempotency

One committed U04 result may produce at most one routing side effect for the same business event identity.
Replay must not duplicate downstream execution.

## 6. Verdict

U04-RDP-04 = FROZEN / PASS_FOR_READINESS
U04 implementation authorization = NOT_GRANTED
