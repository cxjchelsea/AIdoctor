# U04 RDP-01 Consumer-side Inbound Contract v0.1

> Scope: U04 Safety Gate consumer boundary only.
> This document does not authorize U04 implementation, routing, production, or real-patient traffic.

## 1. Source

U04 consumes only the governed U03 outbound boundary produced from the current-version U03 execution/committed-state boundary.

Authoritative producer-side source:
`U03_CD07_U03_to_U04_Outbound_Boundary_Contract_v0.1.md`

Producer contract exists != consumer acceptance.

## 2. Required inbound identity

Accepted U04 input must preserve, as applicable:
- consultation / CDP identity;
- source/current Clinical State Version;
- Thread / Run / Event / correlation / trace identity;
- U03 execution/result identity;
- U03 status and structured disposition/failure semantics;
- exact knowledge/rule/coverage/policy/policy-pair refs;
- accepted evidence/provenance refs;
- proposal/commit identity where canonical state was mutated.

## 3. Admission rules

U04 consumer admission must fail closed when:
- the handoff is null, malformed, or missing required identity;
- Clinical State Version is stale or does not match the current authoritative version;
- exact governed release refs are missing, mixed, mutable, or unapproved;
- required provenance/evidence identity is missing;
- a committed-state claim lacks the corresponding governed commit identity;
- replay/idempotency identity is malformed or conflicting.

Admission failure must not be reinterpreted as ALLOW.

## 4. U03 result preservation

U04 must preserve the distinction among:
`VALID`, `FAILED`, `NO_HIGH_RISK_SIGNAL`, `CAUTION`, `HIGH_RISK`, insufficiency/scope/failure conditions, and any already-governed typed failure reason.

Forbidden:
`FAILED -> NO_HIGH_RISK_SIGNAL`
`NO_HIGH_RISK_SIGNAL -> SAFE`
`missing/unknown -> negative`

## 5. Idempotency

Same accepted U03 business handoff identity may produce at most one U04 clinical effect.
Duplicate replay may return an idempotent acknowledgement/current authoritative U04 result but must not duplicate state mutation or routing side effects.

## 6. Consumer output

RDP-01 only produces an admitted U04 input envelope or a typed admission failure.
It does not decide ALLOW/RESTRICTED/BLOCKED/UNAVAILABLE.

## 7. Verdict

U04-RDP-01 = FROZEN / PASS_FOR_READINESS
U04 implementation authorization = NOT_GRANTED
