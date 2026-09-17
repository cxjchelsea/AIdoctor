# U03 CD-07 Implementation Authorization Review v0.1

> 对象：PR #90 HEAD `2bca5eb162fc8e1118ed8a5de7b8d0fa7b0d12a9` 的独立 Implementation Authorization Review。  
> 基线：`83d00ddacac5666da9228709b8349d562741d759`。  
> 输入：R1–R6 readiness contracts + `U03_CD07_Implementation_Readiness_ReReview_v0.1.md`。  
> 审核角色：`U03_TECHNICAL_GOVERNANCE_REVIEW`  
> 审核日期：`2026-09-17`  
> 状态：`AUTHORIZATION_REVIEW_COMPLETE / AUTHORIZED / NON_PRODUCTION_ONLY / NOT_PRODUCTION / NOT_U04 / NOT_MERGE`。

```text
CD-07 Implementation Readiness = READY_FOR_AUTHORIZATION_REVIEW
AUTH-U03-CD07-RUNTIME-IMPL-001 = AUTHORIZED
Runtime binding mode = EXPLICIT_NON_PRODUCTION_BINDING_ONLY
Production Authorization = NOT_GRANTED
U04 Implementation Authorization = NOT_GRANTED
```

本审查判断的不是“设计还缺什么”，而是：**这 6 份 contract 是否足以约束实现，以及是否允许开工。**  
它不把 Gate C PASS、PR #89 merge 或 readiness PASS 自动当成生产授权。

---

## 1. Scope

只判：

```text
AR-01  R1–R6 是否足以作为实现约束
AR-02  是否允许在 EXPLICIT_NON_PRODUCTION_BINDING_ONLY 下开始 CD-07
AR-03  授权的 exact PR / commit / scope
AR-04  明确排除 production / U04 owner / real patient traffic
```

不重开：

```text
Gate A / B / C
PR #89 Independent Review / Evidence Freeze
RDP-01..06 readiness re-review PASS
```

不授权：

```text
production Clinical State mutation
release publication / activation
real patient traffic
U04 owner / U14 routing
PR #90 merge
PR #90 内写 runtime 代码
pediatrics / pregnancy-puerperium / China production localization
新的临床规则、阈值、expected outcome
```

---

## 2. Change isolation

GitHub compare：

```text
base = 83d00ddacac5666da9228709b8349d562741d759
head = 2bca5eb162fc8e1118ed8a5de7b8d0fa7b0d12a9
ahead = 7
files changed = 7
runtime code = 0
workflow = 0
clinical rule / threshold / fixture = 0
```

7 个新增文件全部是治理文档。PR #90 仍为 Draft。  
基线 `83d00dd` 已包含 PR #89 standard merge `a185efcdc7c84107563a562b516f0d015bd10fd8`，因此 R1 写的 `PR #89 已 standard merge` 成立。

---

## 3. Mandatory questions

| ID | Question | Verdict |
|---|---|---|
| AR-01 | R1–R6 是否足以作为实现约束 | APPROVE |
| AR-02 | 是否允许开始 non-production CD-07 runtime implementation | **AUTHORIZE** |
| AR-03 | exact PR / commit / scope 是否可绑定 | APPROVE / 见 §5 |
| AR-04 | production / U04 / real patient 是否被明确排除 | APPROVE |

blocking authorization finding = 0

---

## 4. Why the six contracts are sufficient

对照 `U03_CD07_Implementation_Readiness_Assessment_v0.1.md` 的开工前置：

```text
RDP-01  R1 冻结 S1–S14 / O1–O14，并要求未来授权绑定 R1–R6
RDP-02  R2 冻结 I1–I9、C02/D09/K09/P01 链、失败≠阴性临床结果
RDP-03  R3 钉死 exact refs + 禁止 latest/current + fail-closed + 不改变 candidate 生命周期
RDP-04  R4 钉死 StateCommitter-only、stale/partial/replay、生产 mutation 禁止
RDP-05  R5 预先冻结 V1–V8 / N1–N13 / E1–E11，避免“写完再决定怎么验”
RDP-06  R6 只冻结 U03 outbound producer，不授权 U04
```

交叉不变式在 R1–R6 中一致：

```text
candidate != PUBLISHED != ACTIVE_FOR_PRODUCTION
Candidate != Decision != Proposal != Commit
FAILED != negative clinical result
U03 result != U04 Safety decision
P01 → StateCommitter = controlled canonical mutation path
C02 / D09 / K09 != mutation authority
```

这些已经足够约束实现者“可以写什么、必须怎么失败、最后怎么证明没越界”。  
缺的 C02/D09 wiring 和 runtime E2E 属于 IMP-01..09，不是再挡授权的 readiness gap。

---

## 5. Authorization

```text
Authorization ID = AUTH-U03-CD07-RUNTIME-IMPL-001
Status = AUTHORIZED
Mode = EXPLICIT_NON_PRODUCTION_BINDING_ONLY
```

### 5.1 Exact identity

```text
readiness PR = #90
readiness HEAD = 2bca5eb162fc8e1118ed8a5de7b8d0fa7b0d12a9
readiness branch = prep/u03-cd07-implementation-readiness
parent baseline = 83d00ddacac5666da9228709b8349d562741d759
PR #89 merge = a185efcdc7c84107563a562b516f0d015bd10fd8
```

实现必须开 **新的 isolated implementation branch / PR**，不得把 runtime 代码写入 PR #90。

允许的实现起点：

```text
prep/u03-cd07-implementation-readiness @ 2bca5eb
或
#90 standard merge 之后的 prep/u03-clinical-dependency-completion
```

### 5.2 Bound contracts

实现必须同时遵守：

```text
R1 U03_CD07_Scope_Authorization_Boundary_v0.1.md
R2 U03_CD07_Runtime_IO_Contract_v0.1.md
R3 U03_CD07_Runtime_Release_Binding_Policy_v0.1.md
R4 U03_CD07_Commit_Safety_Failure_Contract_v0.1.md
R5 U03_CD07_Runtime_Verification_Evidence_Plan_v0.1.md
R6 U03_CD07_U03_to_U04_Outbound_Boundary_Contract_v0.1.md
```

### 5.3 Authorized implementation scope

仅 R1 的 S1–S14，且只在 non-production / 受控验证环境：

```text
S1  non-production U03 runtime integration
S2  exact Clinical State Version binding
S3  real Thread / Run / Event identity binding
S4  Foundation-1 governed C02 invocation adapter
S5  accepted-evidence boundary consumption
S6  C02 candidate/result + explicit FAILED
S7  D09 owner execution using frozen governed contract
S8  typed K09 proposal generation
S9  P01 / StateCommitter controlled non-production integration
S10 exact governed release/version binding checks
S11 P05 runtime trace/provenance correlation
S12 fail-closed for stale/missing/mismatch/dependency failure
S13 focused runtime verification and non-production E2E
S14 U03 outbound producer boundary for future U04 handoff
```

只允许绑定：

```text
KR-U03-SOURCE-001@0.1.0-candidate
RR-U03-RISK-001@0.2.1-candidate
U03_D09_COVERAGE_V0_2_1_CANDIDATE
PR-U03-D09-001@0.2.1-candidate
PF-U03-C-POLICY-001
```

Gate C regression 可额外引用 `ER-U03-RISK-001@0.1.0-candidate`，但不得改其 expected outcome。

### 5.4 Implementation constraints

```text
- exact ref 必须由受控配置/调用上下文指定，禁止 latest/current/newest
- 不得改变 candidate 的 NOT_PUBLISHED / NOT_ACTIVE_FOR_PRODUCTION
- 不得新增临床规则、阈值、disposition 词汇或 SAFE/NORMAL 语义
- 不得把 FAILED/UNKNOWN/UNMEASURED 转成阴性临床结论
- C02 / D09 / K09 / P05 都不是 canonical mutation authority
- 只允许已存在的 governed P01 proposal types，不得发明新的临床 proposal 类型
- 非生产 store / 环境身份必须在实现中显式声明，并被 R5 N12 证明
- 不得打开生产请求路径或生产 feature flag
- 不得通过 CDPManager.updateCDP 或同类路径绕过 StateCommitter
- tools/u03_gatec_eval 只能用于 V7 回归，不得改冻结临床语义来让 runtime 过测
```

### 5.5 Explicit exclusions

```text
O1  real patient traffic
O2  production Clinical State mutation
O3  production runtime enablement
O4  publication / activation of clinical releases
O5  external production clinical API wiring
O6  U04 Safety Gate implementation or owner execution
O7  U14 implementation/routing
O8  pediatrics source/rule expansion
O9  pregnancy / puerperium pathway expansion
O10 China production localization
O11 new clinical rules / thresholds / evidence semantics / disposition vocabulary
O12 changing Gate-C-frozen expected outcomes
O13 bypassing StateCommitter / P01
O14 direct CDPManager.updateCDP for governed U03 semantics
```

---

## 6. Non-blocking residuals

这些项不阻止本次授权，但独立实现审查必须核对：

```text
N-AUTH-01
R1 未点名具体 package path。
授权要求：新代码必须可识别为 CD-07 non-production slice，
不得 silently 接入生产 clinical request path。

N-AUTH-02
R4 未枚举允许的 K09/P01 proposal types。
授权要求：只复用已治理 P01 边界中的类型。

N-AUTH-03
非生产 store / 环境身份未在 readiness 中命名。
授权要求：实现时声明，并由 R5 E3 / N12 证明。

N-AUTH-04
R2 未把 Coverage 5+2 写成单独 D09 输入字段名。
授权要求：D09 必须通过 R3 绑定的 coverage_contract_ref 消费冻结 Coverage，
不得另算一套。
```

---

## 7. What this authorization does not do

```text
AUTH-U03-CD07-RUNTIME-IMPL-001
  != production authorization
  != release publication / activation
  != U04 Implementation Authorization
  != U04 Implementation Readiness
  != PR #90 Merge Authorization
  != 实现已完成
  != runtime E2E PASS
```

U04 仍为：

```text
U04 Implementation Readiness = NOT_READY
U04 blocker = BLOCKED_BY_U03_RUNTIME_CLINICAL_DEPENDENCY
```

R6 只允许准备 outbound producer，不能当成 U04 已解除。

---

## 8. Required sequence after this grant

```text
new isolated implementation PR
↓
implement only S1–S14 under R1–R6
↓
R5 verification + durable evidence
↓
independent implementation / evidence review
↓
explicit Merge Authorization review
↓
Repository Owner explicit Merge Authorization
↓
standard merge
↓
PMV
```

测试通过不能自我授权 merge 或生产。

---

## 9. Decision

```text
AR-01 = APPROVE
AR-02 = AUTHORIZE
AR-03 = BIND 2bca5eb / R1–R6 / S1–S14
AR-04 = EXCLUSIONS FROZEN

AUTH-U03-CD07-RUNTIME-IMPL-001 = AUTHORIZED
CD-07 Implementation Readiness = READY_FOR_AUTHORIZATION_REVIEW
CD-07 Implementation Authorization = GRANTED / NON_PRODUCTION_ONLY

Runtime Implementation Authorization
= GRANTED ONLY AS AUTH-U03-CD07-RUNTIME-IMPL-001
= NOT a general runtime/production grant

U04 Implementation Authorization = NOT_GRANTED
PR #90 Merge Authorization = NOT_GRANTED
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
```

下一步可以开始 **新的** CD-07 non-production runtime implementation PR。  
不要在 PR #90 里写代码，也不要开始 U04 或生产接线。
