# U04 RDP-02 Safety Gate Policy / Owner Decision Task v0.1

> This document isolates unresolved U04 Safety Gate business semantics that cannot be invented by implementation.
> Status: OWNER_DECISION_REQUIRED / BLOCKING.

## 1. Already frozen

Safety Gate vocabulary:
`ALLOW`, `RESTRICTED`, `BLOCKED`, `UNAVAILABLE`.

Already frozen invariants:
- HIGH_RISK must not remain ordinary ALLOW;
- Risk Assessment FAILED must not produce ALLOW;
- required Safety Capability unavailable must not produce ALLOW;
- ordinary planner/model/agent may not override the gate;
- U04 and U14 may not compete for the same final route.

## 2. Decisions still missing

The following mappings are not uniquely determined by current frozen documents:

| Input condition | Current frozen constraint | Missing owner decision |
|---|---|---|
| NO_HIGH_RISK_SIGNAL | does not mean SAFE/NORMAL | whether/when gate may be ALLOW vs RESTRICTED |
| CAUTION | no direct U04 mapping frozen | exact gate and conditions |
| HIGH_RISK | must not ordinary ALLOW | RESTRICTED vs BLOCKED selection rule |
| U03 FAILED | must not ALLOW | UNAVAILABLE vs stricter governed state |
| scope unavailable/not established | fail closed required | exact gate/result semantics |
| conflicting safety inputs | no permissive flattening | precedence/resolution policy |
| dependency failure | must not unsafe-allow | exact gate/result semantics |

## 3. Owner boundary

These decisions may alter whether ordinary clinical continuation is permitted and therefore are Safety/Product/Medical governance, not developer implementation detail.

Required owner record must:
- name the authorized policy owner(s);
- freeze exact mapping/preference rules;
- distinguish clinical/safety truth from runtime failure;
- define any permitted RESTRICTED behavior;
- define conflict precedence;
- bind version/release identity;
- provide executable expected cases for RDP-06.

## 4. Prohibited shortcuts

Developers/tests must not assume:
`NO_HIGH_RISK_SIGNAL -> ALLOW`
`CAUTION -> RESTRICTED`
`HIGH_RISK -> BLOCKED`
`FAILED -> UNAVAILABLE`

unless the corresponding owner decision is explicitly frozen.

## 5. Verdict

U04-RDP-02 = OWNER_DECISION_REQUIRED / OPEN / BLOCKING
U04 implementation readiness cannot PASS until this package is frozen.
