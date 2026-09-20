# U03 Evaluation-Only Implementation Authorization Record v0.1

> Authorization ID：`AUTH-U03-GATEC-EVAL-IMPL-001`  
> 状态：`AUTHORIZED / EVALUATION_ONLY / NOT_RUNTIME / NOT_PRODUCTION`  
> 授权来源：用户显式给出 `Implementation Authorization: AUTH-U03-GATEC-EVAL-IMPL-001`。  
> 前置：`U03_Evaluation_Only_Clinical_Implementation_Authorization_Readiness_v0.1.md = READINESS_COMPLETE`。

## 1. Authorized Scope

仅授权：

```text
Gate C offline evaluation harness
+ evaluation-only executable C semantics
+ evaluation-only executable Coverage semantics
+ evaluation-only executable D09 semantics
+ execution of ER-U03-RISK-001@0.1.0-candidate
+ Gate C execution evidence generation
```

唯一允许实现的 clinical authority refs：

```text
KR-U03-SOURCE-001@0.1.0-candidate
RR-U03-RISK-001@0.2.1-candidate
U03_D09_COVERAGE_V0_2_1_CANDIDATE
PR-U03-D09-001@0.2.1-candidate
PF-U03-C-POLICY-001
ER-U03-RISK-001@0.1.0-candidate
```

## 2. Explicitly Not Authorized

```text
CD-07 runtime implementation
production/runtime C02 adapter wiring
production/runtime D09 wiring
real patient traffic
production Clinical State commit
U04 / U14 routing
release publication / activation
production rollout
legacy physical deletion
China-localized policy
pediatrics
pregnancy/puerperium clinical pathway
```

## 3. Isolation Requirement

Implementation must remain evaluation-only and isolated:

```text
fixture-only synthetic state
exact immutable release refs
no production DB credentials
no production message bus
no real patient identifiers
no external patient request entry
no mutable latest alias
no production release activation
no production state mutation
```

Prefer test/evaluation source-set placement so the evaluator cannot be wired into production runtime accidentally.

## 4. Acceptance Criteria

```text
A1 exact candidate refs pinned
A2 15 C rules traceable to frozen IDs
A3 missingness/scope semantics traceable
A4 D09 P0..P5 traceable
A5 precedence deterministic
A6 no SAFE/NORMAL invented vocabulary
A7 no production mutation path
A8 no mutable latest alias
A9 result bundle captures actual/expected/assertions
A10 critical safety failure blocks Gate C
```

## 5. Authorization Effect

```text
AUTH-U03-GATEC-EVAL-IMPL-001 = AUTHORIZED
BF-CD06-EXEC-01 = may now be addressed by implementation
Evaluation Execution = still NOT_STARTED until runnable path verified
Gate C = NOT_PASSED
CD-07 = BLOCKED
Runtime Implementation Authorization = NOT_GRANTED
Production Authorization = BLOCKED
```

Authorization does not constitute Merge Authorization.
