# U02 Implementation Readiness Assessment

> Unit：U02 临床事实形成与版本提交  
> 阶段：Pre-Implementation Readiness  
> 分支：`prep/u02-clinical-fact-formation`  
> 基线：`main@8257b399bd4547d56c5613366feafb7b72318b15`  
> 本文件用于判断 U02 是否满足 Implementation Authorization 条件；不构成实现授权或 Merge Authorization。

---

## 1. 冻结上游输入

U02 的业务语义来自 Phase 6：

```text
S_in
= Consultation ACTIVE
  或合法 Resume / Correction Input

Event
= NEW_CLINICAL_INPUT

Action
= F2 提取/归一化 Clinical Observation
  + G2 验证并提交新的 Clinical State Version
  + 对依赖派生状态触发正式失效/重评语义

S_out
A. new Clinical State Version + current effective Clinical Facts → U03
B. still needs clarification → U05/U06
C. cannot reliably form facts / unsafe continuation → U11/U14
```

强制语义：

```text
UNKNOWN != NO
UNMEASURED != NORMAL
MODEL_INFERRED != PATIENT_REPORTED
Capability Result != Clinical Truth
Candidate != Decision != Proposal != Commit
```

Phase 8 已冻结 K03 Observation Candidate 与 K09 StateChangeProposal/Commit 的分层契约。

---

## 2. U02 所需依赖拆分

| 依赖 | 当前状态 | U02 需要做什么 | 是否阻塞授权 |
|---|---|---|---|
| Foundation-1 P06 Binding Governance | MERGED / COMPONENT_VERIFIED | REUSE | 否 |
| Foundation-1 Resolver / Invocation Guard | MERGED / COMPONENT_VERIFIED | REUSE | 否 |
| Foundation-1 P05 Trace Baseline | MERGED / COMPONENT_VERIFIED | U02 wiring increment | 否 |
| C01-U01 minimal slice | PR #80 IMPLEMENTED / COMPONENT_VERIFIED / INTERNALLY_WIRED，但 **NOT_MERGED** | 作为 C01-U02 增量扩展基线 | **是** |
| C01-U02 Clinical Fact Parsing | NOT_IMPLEMENTED | INCREMENTAL_EXTENSION | 否，属于 U02 实现范围 |
| P01 typed Clinical Fact Commit | Foundation mechanical core exists，U02 typed path NOT_IMPLEMENTED | INCREMENTAL_EXTENSION | 否，属于 U02 实现范围 |
| minimum D05 invalidation hook | NOT_IMPLEMENTED | U02 最小版本 | 否，属于 U02 实现范围 |
| U02 Business Unit/Application path | NOT_IMPLEMENTED | NEW | 否，属于 U02 实现范围 |
| External `/continue` / Phase10 cutover | NOT_COMPLETE | 明确 deferred | 否，不属于当前 U02 component slice |

---

## 3. C01-U02 Capability Gap Assessment

### 3.1 当前必须继承的 C01-U01 基础

U02 不应重新实现以下能力基础，而应在 PR #80 的 C01-U01 slice 上增量扩展：

```text
typed candidate-only capability contract
Java typed gateway/client
fail-closed capability invocation
CapabilityBinding validation
provenance / uncertainty pattern
raw-input fingerprint / replay safety pattern
Capability Result != Clinical Truth boundary
```

### 3.2 U02 新增最小输出

按 Phase 7 / Phase 8，C01-U02 至少扩展 `ObservationCandidate`：

```text
observation_id
concept_id
concept_display
raw_text_ref
normalized_value
value_semantics
unit
negation
temporality
severity_or_degree
source_type
confidence_or_uncertainty
provenance
ambiguity_flags[]
contradiction_refs[]
```

必须明确区分：

```text
Observation lifecycle
= EXTRACTED / NORMALIZED / CONFIRMED / UNCERTAIN / CONTRADICTED / INVALIDATED

Fact value
= YES / NO / UNKNOWN / UNMEASURED / NOT_ASKED / NOT_APPLICABLE
```

### 3.3 U02 C01 不得做

```text
不得直接写 Clinical State
不得直接生成正式 StateChangeProposal
不得做 Risk Disposition
不得做 Safety Gate
不得做 Question selection
不得做 DDx
不得把模型推断伪装成患者报告事实
```

### 3.4 质量/Eval 最低门槛

U02 C01 Eval 至少覆盖：

- 否定；
- 时间；
- 程度；
- 单位；
- 症状/药物/检查相关表达；
- UNKNOWN / UNMEASURED；
- source attribution；
- ambiguity；
- contradiction；
- mixed input；
- capability failure / invalid output。

结论：

```text
C01-U02 Gap = CONFIRMED
Disposition = INCREMENTAL_EXTENSION
Foundation-2 = NOT_REQUIRED
```

---

## 4. P01-U02 Gap Assessment

### 4.1 可复用基础

当前已有：

```text
StateCommitter
StatePatchBoundaryValidator
CommitResult semantics
ClinicalCdpStateRepositoryAdapter
CDP version/history persistence
```

这些属于 P01 mechanical/governance foundation，可复用。

### 4.2 U02 必须新增的 typed commit 语义

U02 需要最小：

```text
accepted Observation Candidate / Business Decision
↓
typed Clinical Fact StateChangeProposal
↓
base_clinical_state_version precondition
↓
field/path permission validation
↓
source/provenance validation
↓
idempotency against accepted NEW_CLINICAL_INPUT
↓
atomic commit
↓
CommitResult
↓
Clinical State Version n+1
```

Proposal 至少绑定：

```text
proposal_id
consultation_id
base_clinical_state_version
business_owner
accepted_event_ref / source_decision_ref
operations[]
evidence/source refs
capability_binding_refs[]
idempotency_key
```

不得：

```text
Capability Result -> StateCommitter direct
complex clinical object -> JSON string patch
CDPManager.updateCDP bypass
silent overwrite current version
```

结论：

```text
P01-U02 Gap = CONFIRMED
Disposition = INCREMENTAL_EXTENSION DRIVEN BY U02
```

---

## 5. Minimum D05 Scope Assessment

U02 当前只需要能够保证“新事实版本不会让旧派生状态继续冒充当前真值”的最小失效机制。

本轮最小 D05 只定义：

```text
Fact changed / corrected
↓
identify registered dependent derived artifacts
↓
mark dependent artifact stale/invalidated/superseded
↓
record invalidation reason + source fact/event + target version
```

支持的状态效果边界：

```text
Risk → STALE       （当 U03 后存在）
DDx → INVALIDATED  （当 U08 后存在）
Workup → SUPERSEDED（当 U10 后存在）
Delivery → SUPERSEDED（当 U11/U12 后存在）
```

但是 U02 阶段不提前实现这些未来模块本身。当前实现应只提供：

```text
dependency/invalidation contract or hook
+ no-op safe behavior when no downstream artifact exists
+ traceable invalidation record
```

D05 不成为 Risk/DDx/Workup/Delivery Owner。

结论：

```text
D05 minimum hook = REQUIRED IN U02
full dependency engine = DEFERRED
```

---

## 6. U02 最小施工包

若 Implementation Authorization 后，允许的施工范围固定为：

```text
U02-01 C01-U02 Observation Candidate contract extension
U02-02 C01-U02 parsing / normalization / provenance implementation + Eval
U02-03 Java/Runtime governed C01-U02 invocation adapter
U02-04 U02 Business Owner / interpretation boundary
U02-05 K09 typed Clinical Fact Proposal
U02-06 P01 typed Clinical Fact commit extension
U02-07 minimum D05 dependency invalidation hook
U02-08 P05 capability-result → decision → proposal → commit trace wiring
U02-09 U02 unit tests / capability eval / regression / boundary guards
U02-10 U02 implementation + verification record
```

明确排除：

```text
U03/C02 Risk
U04 Safety Gate
U05 full Readiness implementation
U06/C03 Question
U08/C04 DDx
full D05 engine
full P01/P05/P06 platform
Phase10 external API cutover
legacy physical deletion
production activation
```

---

## 7. 当前阻塞项

### BLOCKER-U02-01 — C01-U01 仍未进入 main

当前 PR #80：

```text
OPEN
DRAFT
MERGEABLE
NOT_MERGED
```

U02 的 C01 设计是 `INCREMENTAL_EXTENSION`，因此必须以 C01-U01 基础为前置。当前 `main` 没有该基础。

可接受的解除方式：

```text
A. PR #80 完成最终 Merge Readiness → Merge Authorization → standard merge → PMV

或

B. 明确重构 PR #80，使其在最新 Foundation-1 main 上更新并重新验证后再合并
```

不接受：

```text
U02 在 main 上另造一套平行 C01 implementation
```

---

## 8. Readiness Verdict

当前结论：

```text
U02 Business Design = READY
U02 Legacy Impact Assessment = COMPLETE
C01-U02 Gap = DEFINED
P01-U02 Gap = DEFINED
minimum D05 Scope = DEFINED
Foundation-1 Dependency = SATISFIED
C01-U01 Mainline Dependency = NOT_SATISFIED

U02 Implementation Readiness = BLOCKED_BY_PREDECESSOR
U02 Implementation Authorization = NOT_YET_RECOMMENDED
```

解除唯一当前硬阻塞后，应再次检查 PR #80 与最新 Foundation-1 的兼容性，然后即可进入 U02 Implementation Authorization Gate。
