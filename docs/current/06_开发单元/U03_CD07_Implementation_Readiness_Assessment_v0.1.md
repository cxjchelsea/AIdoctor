# U03 CD-07 Implementation Readiness Assessment v0.1

> 对象：Gate C PASS 且 PR #89 已完成 standard merge 后，重新判断 CD-07 是否具备进入 runtime implementation 的开工前置。  
> 基线：`prep/u03-clinical-dependency-completion`，PR #89 merge commit `a185efcdc7c84107563a562b516f0d015bd10fd8`，post-merge governance status sync 已验证。  
> 审核角色：`U03_TECHNICAL_GOVERNANCE_REVIEW`。  
> 状态：`READINESS_ASSESSMENT_COMPLETE / NOT_READY / PREIMPLEMENTATION_GAPS_DEFINED / NOT_IMPLEMENTATION_AUTHORIZATION`。

## 1. Decision

```text
Gate A = PASS
Gate B = PASS / GOVERNED_CONTENT_READY
Gate C = PASS
PR #89 = MERGED
Post-Merge Governance Status Sync = PASS

CD-07 Implementation Readiness = NOT_READY
CD-07 Implementation Authorization = NOT_GRANTED
Runtime Implementation Authorization = NOT_GRANTED
```

Gate C 已解除“不能讨论 CD-07”的阻挡，但当前仍缺少足够明确的 **runtime implementation preconditions**。

本判定不以“runtime 代码尚未实现”作为 readiness blocker；否则会形成循环条件。

## 2. Readiness 与 Implementation Deliverable 的边界

### 2.1 开工前必须存在的 Readiness Preconditions

CD-07 开工前应冻结：

```text
RDP-01 CD-07 Scope / Authorization Boundary
RDP-02 U03 Runtime Input / Output Contract
RDP-03 Governed Release Binding + Activation Boundary
RDP-04 Clinical State Commit / Safety / Failure Boundary
RDP-05 Runtime Verification & Evidence Plan
RDP-06 U03→U04 Boundary Contract
```

这些内容决定开发者“允许实现什么、不得实现什么、输入输出是什么、失败时怎么停、最后怎么证明实现没有越界”。

### 2.2 开工后才应交付的 Implementation / Verification Results

以下不是 readiness 前置，而是 CD-07 获得授权后需要完成的交付：

```text
IMP-01 C02 runtime wiring
IMP-02 D09 runtime wiring
IMP-03 governed release resolution in runtime
IMP-04 typed proposal → P01 / StateCommitter runtime path
IMP-05 P05 runtime trace binding
IMP-06 U03→U04 handoff wiring（若单独授权）
IMP-07 runtime-focused tests
IMP-08 U03 runtime E2E
IMP-09 failure / rollback / stale-version / release-mismatch verification
```

因此：

```text
“未完成 IMP-01..09” != “CD-07 不具备 Readiness”
```

真正阻挡当前 readiness 的是 RDP-01..06 尚未形成完整冻结包。

## 3. RDP-01 — CD-07 Scope / Authorization Boundary

当前只有 evaluation-only：

```text
AUTH-U03-GATEC-EVAL-IMPL-001
```

它明确不覆盖 runtime。

CD-07 需要独立冻结至少以下 scope：

```text
IN SCOPE
- non-production U03 runtime integration
- C02 candidate/result consumption under Foundation-1 authorization
- D09 deterministic owner execution using frozen governed refs
- typed K09 proposal generation
- P01 / StateCommitter integration in controlled non-production runtime
- release/version binding checks
- runtime trace correlation
- fail-closed behavior
- focused runtime verification

OUT OF SCOPE unless separately authorized
- real patient traffic
- production Clinical State mutation
- production release activation
- external production clinical APIs
- U04/U14 production routing
- pediatrics
- pregnancy/puerperium pathway expansion
- China production localization
```

Verdict：

```text
RDP-01 = GAP / NOT_FROZEN
```

## 4. RDP-02 — U03 Runtime Input / Output Contract

当前工程代码已有 U03 command / C02 generic candidate / D09 owner / typed proposal 等基础，但 CD-07 仍需把 runtime contract 冻结成开发可执行规范。

至少需要：

```text
Input
- exact Clinical State Version
- Thread / Run / Event identity
- accepted evidence binding
- governed release refs
- scope context
- dependency execution status

C02 Output
- candidate-only risk evidence/result
- explicit FAILED semantics
- exact provenance + release refs

D09 Input
- accepted C02 result set
- Coverage 5+2 state
- exact governed policy refs

D09 Output
- frozen disposition vocabulary
- reason codes
- typed K09 proposal payload

Commit Boundary
- proposal != commit
- StateCommitter remains unique controlled mutation authority
```

还必须明确：

```text
UNKNOWN != NO
UNMEASURED != NORMAL
MODEL_INFERRED != PATIENT_REPORTED
Capability Result != Clinical Truth
Candidate != Decision != Proposal != Commit
Trace != Clinical State
```

Verdict：

```text
RDP-02 = PARTIAL / NEEDS_RUNTIME_CONTRACT_FREEZE
```

## 5. RDP-03 — Governed Release Binding + Activation Boundary

当前 governed set：

```text
KR-U03-SOURCE-001@0.1.0-candidate
RR-U03-RISK-001@0.2.1-candidate
U03_D09_COVERAGE_V0_2_1_CANDIDATE
PR-U03-D09-001@0.2.1-candidate
```

状态仍是：

```text
CANDIDATE / FROZEN / EVALUATED
NOT_PUBLISHED
NOT_ACTIVE_FOR_RUNTIME
NOT_ACTIVE_FOR_PRODUCTION
```

CD-07 readiness 不要求现在就 production activate，但必须先冻结：

```text
- runtime 如何解析 exact release refs
- candidate 是否允许在 isolated/non-production runtime 中被显式绑定
- 谁拥有 publication / activation 权限
- activation 与 implementation authorization 必须分离
- release mismatch / missing / stale 时 fail closed
- runtime 不得自行选择“最新”临床 release
```

建议 CD-07 第一阶段采用：

```text
EXPLICIT_NON_PRODUCTION_BINDING_ONLY
```

即只允许在测试/受控 runtime 中显式指定已冻结且 Gate C PASS 的 candidate refs；不因此改变其 `NOT_PUBLISHED / NOT_ACTIVE_FOR_PRODUCTION` 状态。

Verdict：

```text
RDP-03 = GAP / RELEASE_RUNTIME_BINDING_POLICY_NOT_FROZEN
```

## 6. RDP-04 — Clinical State Commit / Safety / Failure Boundary

现有架构原则：

```text
StateCommitter = controlled state mutation authority
proposal != commit
Clinical Truth != Capability Result
```

CD-07 开工前仍需冻结 runtime commit boundary：

```text
- 哪些 U03 outputs 只是 candidate/result
- 哪些 D09 outputs 可以形成 typed proposal
- 哪些 proposal 类型允许进入 StateCommitter
- exact expected_version / stale-version behavior
- release mismatch behavior
- dependency failure behavior
- partial failure / retry / idempotency boundary
- P05 trace failure 不得反向成为 Clinical Truth
- non-production mutation 与 production mutation 的隔离方式
```

并保持：

```text
Production Clinical State commit = NOT_AUTHORIZED
```

Verdict：

```text
RDP-04 = PARTIAL / NEEDS_CD07_COMMIT_FAILURE_CONTRACT
```

## 7. RDP-05 — Runtime Verification & Evidence Plan

Gate C 已证明 frozen clinical semantics 在 isolated evaluator 中成立，但不能替代 runtime implementation verification。

CD-07 开工前需要先定义“实现完成后按什么判 PASS”，至少包括：

```text
V1 exact release binding
V2 exact Clinical State Version binding
V3 stale input fail closed
V4 release mismatch fail closed
V5 dependency failure != negative clinical result
V6 candidate/result/proposal/commit separation
V7 StateCommitter-only mutation path
V8 idempotency / replay behavior
V9 trace correlation correctness
V10 no unauthorized network / production traffic
V11 no unauthorized U04/U14 routing
V12 Gate C frozen semantics regression remains PASS
V13 focused runtime integration tests
V14 U03 non-production runtime E2E
```

必须明确：

```text
component tests PASS
!= runtime E2E PASS
!= clinical evaluation PASS
!= production authorization
```

Verdict：

```text
RDP-05 = GAP / VERIFICATION_PLAN_NOT_FROZEN
```

## 8. RDP-06 — U03→U04 Boundary Contract

U04 当前状态：

```text
U04 Implementation Readiness = NOT_READY
U04 blocker = BLOCKED_BY_U03_RUNTIME_CLINICAL_DEPENDENCY
U04 Implementation Authorization = NOT_GRANTED
```

CD-07 不应因此实现 U04，但必须避免把 U03 runtime 输出设计成未来无法安全交给 U04。

开工前至少冻结 U03 的 outbound boundary：

```text
- handoff payload schema
- committed Clinical State Version ref
- U03 disposition/risk refs
- provenance / release refs
- trace correlation refs
- FAILED / insufficient / scope mismatch handling
- “handoff contract exists” != “U04 routing authorized”
```

CD-07 可只实现 contract producer / adapter boundary，不得启动 U04 owner。

Verdict：

```text
RDP-06 = GAP / OUTBOUND_CONTRACT_NOT_FROZEN
```

## 9. Readiness Matrix

| ID | Precondition | Current Verdict |
|---|---|---|
| RDP-01 | CD-07 scope / authorization boundary | GAP |
| RDP-02 | runtime input/output contract | PARTIAL |
| RDP-03 | runtime release binding policy | GAP |
| RDP-04 | commit / safety / failure contract | PARTIAL |
| RDP-05 | runtime verification & evidence plan | GAP |
| RDP-06 | U03→U04 outbound boundary contract | GAP |

因此：

```text
CD-07 Implementation Readiness = NOT_READY
```

但其原因已经从模糊的“runtime 还没做”收敛为六个可关闭的 pre-implementation gaps。

## 10. Recommended Readiness Closure Package

建议下一阶段只产出并冻结以下 6 项：

```text
CD07-R1 Scope & Authorization Boundary
CD07-R2 Runtime Contract
CD07-R3 Release Binding Policy
CD07-R4 Commit / Failure Contract
CD07-R5 Verification Plan
CD07-R6 U03→U04 Outbound Contract
```

完成后再做一次：

```text
CD-07 Implementation Readiness Re-Review
```

若 RDP-01..06 全部 PASS，才允许把状态提升为：

```text
CD-07 Implementation Readiness = READY
```

然后仍必须由 Repository Owner / 治理流程单独给出：

```text
CD-07 Implementation Authorization = GRANTED
```

`READY != AUTHORIZED`。

## 11. Current Governance State

```text
Gate A = PASS
Gate B = PASS / GOVERNED_CONTENT_READY
Gate C = PASS
PR #89 = MERGED
Post-Merge Governance Status Sync = PASS

CD-07 Implementation Readiness = NOT_READY
CD-07 Implementation Authorization = NOT_GRANTED
Runtime Implementation Authorization = NOT_GRANTED

U04 Implementation Readiness = NOT_READY
U04 blocker = BLOCKED_BY_U03_RUNTIME_CLINICAL_DEPENDENCY
U04 Implementation Authorization = NOT_GRANTED

Current governed releases = CANDIDATE / NOT_PUBLISHED / NOT_ACTIVE_FOR_RUNTIME
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
```
