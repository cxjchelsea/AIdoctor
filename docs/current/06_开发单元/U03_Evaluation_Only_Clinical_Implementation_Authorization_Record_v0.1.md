# U03 Evaluation-Only Clinical Implementation Authorization Record v0.1

> Authorization ID: `AUTH-U03-GATEC-EVAL-IMPL-001`  
> Status: `AUTHORIZED / EVALUATION_ONLY / NOT_RUNTIME / NOT_PRODUCTION`  
> Authorized by explicit user statement: `Implementation Authorization: AUTH-U03-GATEC-EVAL-IMPL-001`  
> Date: `2026-09-16`.

## 1. Authorized scope

Only the narrow Gate C offline evaluation slice is authorized:

```text
- isolated EvalSet / fixture loader and exact-release pinning
- in-memory / fixture-only evaluation execution context
- evaluation-only executable semantics for:
  - RR-U03-RISK-001@0.2.1-candidate
  - U03_D09_COVERAGE_V0_2_1_CANDIDATE
  - PR-U03-D09-001@0.2.1-candidate
- deterministic C rule / missingness / family-scope evaluation
- deterministic D09 P0..P5 decision / precedence
- expected-vs-actual and forbidden assertion checking
- critical-blocking aggregation
- case-level result / provenance bundle generation
- independent implementation review and governed evaluation execution
```

Clinical semantics must be mechanically traceable to the frozen governed objects. No developer-created clinical expansion is authorized.

## 2. Explicit exclusions

This authorization does **not** authorize:

```text
production/runtime C02 wiring
production/runtime D09 wiring
CD-07 runtime implementation
real patient traffic
production Clinical State commit
production DB/message bus integration
U04 / U14 routing
external clinical API exposure
release publication / activation
production rollout
legacy physical deletion
China localized clinical policy
pediatric or pregnancy/puerperium clinical pathways
```

## 3. Isolation requirements

```text
no production credentials
no real patient identifiers
no network clinical retrieval dependency
no mutable latest release lookup
no production release registry activation
no production state mutation path
fixture-only synthetic evaluation state
exact immutable candidate refs only
```

## 4. Acceptance requirements

The implementation must independently demonstrate:

```text
A1 exact candidate refs pinned
A2 all 15 C rules traceable to frozen IDs
A3 missingness/scope semantics traceable
A4 D09 P0..P5 traceable
A5 precedence deterministic and order-independent
A6 no SAFE/NORMAL invented vocabulary
A7 no production state mutation path
A8 no mutable latest alias
A9 result bundle captures actual/expected/assertions
A10 any critical safety failure blocks Gate C
```

## 5. Effect

```text
AUTH-U03-GATEC-EVAL-IMPL-001 = AUTHORIZED
BF-CD06-EXEC-01 = MAY_NOW_BE_ADDRESSED_BY_IMPLEMENTATION
Evaluation Execution = STILL_NOT_STARTED
Gate C = NOT_PASSED
CD-07 = BLOCKED
Production Authorization = NOT_GRANTED
```

Authorization alone does not close `BF-CD06-EXEC-01`; closure requires a runnable, reviewed, isolated evaluation path.
