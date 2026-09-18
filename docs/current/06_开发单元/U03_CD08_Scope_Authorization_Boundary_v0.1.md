# U03 CD-08 Scope & Authorization Boundary v0.1

> 对象：CD-08 post-implementation clinical validation 的范围与授权边界。  
> 前置：Gate C PASS；CD-07 non-production runtime 已完成 implementation / verification / independent review / merge / PMV。  
> 状态：`CD08_SCOPE_FROZEN / READINESS_INPUT_ONLY / NOT_EXECUTION_AUTHORIZATION / NOT_PRODUCTION_AUTHORIZATION`。

## 1. Purpose

CD-08 只验证：**Gate-C-frozen clinical semantics 在真实已实现的 CD-07 non-production runtime path 上，是否仍保持被治理的临床结果、安全行为和失败语义。**

```text
CD-08
!= new clinical design
!= runtime implementation
!= production activation
!= U04 Safety Gate
```

## 2. Validation target

CD-08 必须针对 PR #91 已实现并验证的真实链路，而不是只运行 Gate C evaluation-only harness：

```text
exact execution identity
+ exact Clinical State Version
+ accepted evidence / provenance
+ exact governed release set
        ↓
Foundation-1 CapabilityInvocationGuard
        ↓
C02 governed execution
        ↓
accepted evidence boundary
        ↓
D09 governed owner execution
        ↓
K09 typed proposal
        ↓
P01 admission
        ↓
StateCommitter
        ↓
P05 trace
        ↓
post-commit finalization / S14 producer boundary
```

## 3. Frozen clinical/evaluation bindings

Unless a separate governance change is approved, CD-08 must bind exactly:

```text
EvalSet = ER-U03-RISK-001@0.1.0-candidate
Knowledge = KR-U03-SOURCE-001@0.1.0-candidate
Rule = RR-U03-RISK-001@0.2.1-candidate
Coverage = U03_D09_COVERAGE_V0_2_1_CANDIDATE
Policy = PR-U03-D09-001@0.2.1-candidate
Policy Pair = PF-U03-C-POLICY-001
```

Candidate lifecycle remains:

```text
FROZEN / EVALUATED / NOT_PUBLISHED / NOT_ACTIVE_FOR_PRODUCTION
```

## 4. In-scope validation

A future explicit CD-08 execution authorization may cover only:

```text
C8-01 executable Gate-C Golden cases through actual CD-07 runtime
C8-02 executable critical Safety cases through actual CD-07 runtime
C8-03 expected-vs-actual clinical outcome comparison
C8-04 evidence/rule/policy provenance preservation
C8-05 committed Clinical State semantic-result verification where commit is part of the governed path
C8-06 failure/insufficiency/scope distinctions
C8-07 replay/idempotency behavior relevant to clinical outcome stability
C8-08 exact release/state/run identity traceability
C8-09 forbidden-output / false-negative-safety assertions
C8-10 evidence freeze for independent clinical/governance review
```

## 5. Case accounting

The Gate-C governed case inventory remains the source validation set:

```text
approved Golden identities = 31
executable Golden = 30
excluded Golden = GC-026 / UNPRODUCIBLE_UNDER_SHARED_SCOPE

approved critical Safety identities = 20
executable critical Safety = 19
excluded Safety = SS-012 / UNPRODUCIBLE_UNDER_SHARED_SCOPE
```

CD-08 must not silently convert `GC-026` or `SS-012` into executable cases. Any change requires separate clinical-governance review.

## 6. Explicit exclusions

Not authorized by this package:

```text
O8-01 real patient traffic
O8-02 production Clinical State mutation
O8-03 production runtime enablement
O8-04 release publication / activation
O8-05 external production clinical API wiring
O8-06 U04 Safety Gate implementation / decision / owner execution / routing
O8-07 U14 implementation / routing
O8-08 new rules, thresholds, evidence semantics, disposition vocabulary
O8-09 changing Gate-C expected outcomes to make runtime pass
O8-10 pediatrics expansion
O8-11 pregnancy / puerperium expansion
O8-12 China production localization
O8-13 replacing formal clinical review with test success
```

## 7. Clinical authority boundary

CD-08 implementation may build validation adapters, fixtures, evidence capture and comparison machinery, but may not author new medical truth.

Any discrepancy must be classified as a finding, not repaired by silently editing expected clinical truth.

```text
runtime mismatch
→ finding / investigation
→ clinical-governance decision if semantics may need change
→ separately authorized revision
```

## 8. Core invariants

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
U03 result != U04 Safety decision
```

## 9. Verdict

```text
CD-08 Scope = DEFINED
CD-08 Execution Authorization = NOT_GRANTED
U03 Clinical Dependency Closure = NOT_COMPLETE
U04 Readiness = DEFERRED_UNTIL_CD08_CLOSURE
Production Authorization = BLOCKED
```
