# U03 CD-07 Scope & Authorization Boundary v0.1

> 对象：CD-07 runtime implementation 的开工范围与授权边界。  
> 前置：Gate A/B/C = PASS；PR #89 已 standard merge；`U03_CD07_Implementation_Readiness_Assessment_v0.1.md`。  
> 状态：`RDP-01_FROZEN / READINESS_INPUT_ONLY / NOT_IMPLEMENTATION_AUTHORIZATION / NOT_PRODUCTION_AUTHORIZATION`。

## 1. Purpose

本文件只回答：**未来若 CD-07 获得单独 Implementation Authorization，允许实现什么、禁止实现什么。**

它本身不授予任何 runtime implementation 权限。

```text
READINESS CONTRACT
!= IMPLEMENTATION AUTHORIZATION
!= RUNTIME ACTIVATION
!= PRODUCTION AUTHORIZATION
```

## 2. Authorized-to-be-considered implementation scope

后续独立授权可覆盖的最小 CD-07 scope：

```text
S1 non-production U03 runtime integration
S2 exact Clinical State Version binding
S3 real Thread / Run / Event identity binding
S4 Foundation-1 governed C02 invocation adapter
S5 consumption of accepted-evidence boundary already established upstream
S6 C02 candidate/result handling with explicit FAILED semantics
S7 D09 business-owner execution using the frozen governed contract
S8 typed K09 proposal generation
S9 P01 / StateCommitter controlled non-production integration
S10 exact governed release/version binding checks
S11 P05 runtime trace/provenance correlation
S12 fail-closed behavior for stale/missing/mismatch/dependency failure
S13 focused runtime verification and non-production E2E
S14 U03 outbound contract producer boundary required for future U04 handoff
```

## 3. Explicitly out of scope

除非未来另有独立授权，不得包含：

```text
O1 real patient traffic
O2 production Clinical State mutation
O3 production runtime enablement
O4 publication / activation of clinical releases
O5 external production clinical API wiring
O6 U04 Safety Gate implementation or owner execution
O7 U14 implementation/routing
O8 pediatrics source/rule expansion
O9 pregnancy / puerperium pathway expansion
O10 China production localization
O11 new clinical rules, thresholds, evidence semantics, disposition vocabulary
O12 changing Gate-C-frozen expected outcomes to make runtime tests pass
O13 bypassing StateCommitter / P01 controlled mutation path
O14 direct `CDPManager.updateCDP` mutation for governed U03 semantics
```

## 4. Clinical-authority boundary

CD-07 developers may only consume already-governed clinical semantics. They must not infer or author new clinical truth.

Frozen governed refs remain:

```text
KR-U03-SOURCE-001@0.1.0-candidate
RR-U03-RISK-001@0.2.1-candidate
U03_D09_COVERAGE_V0_2_1_CANDIDATE
PR-U03-D09-001@0.2.1-candidate
PF-U03-C-POLICY-001
```

These refs are evaluated candidates and are not thereby published or production-active.

## 5. Invariants

```text
Clinical Truth != Model Output
Clinical Truth != Capability Result
Capability Result != Runtime State
Candidate != Decision != Proposal != Commit
Trace != Clinical State
FAILED != negative clinical result
UNKNOWN != NO
UNMEASURED != NORMAL
MODEL_INFERRED != PATIENT_REPORTED
```

## 6. Authorization contract

Any future CD-07 Implementation Authorization must bind at least:

```text
- exact repository branch / commit
- this R1 contract
- R2 Runtime IO Contract
- R3 Runtime Release Binding Policy
- R4 Commit / Safety / Failure Contract
- R5 Verification & Evidence Plan
- R6 U03→U04 Outbound Boundary Contract
- explicit implementation scope
- explicit exclusions
```

Authorization must not be inferred from Gate C PASS, PR #89 merge, readiness PASS, code presence, or test success.

## 7. Verdict

```text
RDP-01 = FROZEN / PASS_FOR_READINESS_REVIEW
CD-07 Implementation Authorization = NOT_GRANTED
Runtime Implementation Authorization = NOT_GRANTED
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
```
