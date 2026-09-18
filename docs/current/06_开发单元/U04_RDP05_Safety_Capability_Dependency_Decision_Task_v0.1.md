# U04 RDP-05 Safety Capability / Dependency Decision Task v0.1

> This document isolates unresolved dependency semantics that affect U04 Safety Gate outcome.
> Status: OWNER_DECISION_REQUIRED / BLOCKING.

## 1. Existing frozen rule

The frozen U04 unit states that required Safety Capability availability may be an input to Safety Gate and that required capability unavailability must not produce ALLOW.

## 2. Missing decisions

Current governance does not yet define:
- whether the current V1 U04 slice has any required Safety Capability beyond the verified U03 input itself;
- exact capability IDs / governed versions if required;
- required vs optional vs prohibited classification;
- authorization/scope requirements;
- timeout/unavailable/dependency-failure semantics;
- whether any fallback is permitted;
- fallback authorization and evidence requirements;
- whether dependency failure maps to UNAVAILABLE or another separately approved stricter gate.

## 3. Why this is blocking

Declaring a dependency required changes the business meaning of Gate availability. Declaring it optional can permit ordinary progression that a stricter safety policy might prohibit.

Therefore this cannot be chosen by implementation convenience.

## 4. Required owner record

The owner decision must freeze, for the current U04 slice:
1. complete dependency list;
2. each dependency's REQUIRED / OPTIONAL / PROHIBITED status;
3. exact version/release binding where applicable;
4. authorization scope;
5. availability/failure semantics;
6. fallback policy, if any;
7. expected Safety Gate consequence for each blocking failure;
8. executable verification cases.

## 5. Conservative implementation boundary before owner decision

Until RDP-05 is frozen:
- no new Safety Capability may be called by U04;
- absence of an undefined dependency may not be interpreted as safe;
- no fallback may be invented;
- no production wiring may be activated.

## 6. Verdict

U04-RDP-05 = OWNER_DECISION_REQUIRED / OPEN / BLOCKING
U04 implementation readiness cannot PASS until this package is frozen.
