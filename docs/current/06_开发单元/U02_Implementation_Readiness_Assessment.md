# U02 Implementation Readiness Assessment

> Unit：U02 临床事实形成与版本提交  
> 阶段：Pre-Implementation Readiness  
> 原前置分支：`prep/u02-clinical-fact-formation` / PR #82  
> 当前承载分支：`impl/u02-clinical-fact-formation`  
> 基线：`main@e0b9d776fec9b9510b4d6b68425f87f8f5fbc87c`  
> 本文件用于记录 U02 进入实施前已经通过的 Readiness；Implementation Authorization 已由用户在后续门禁中显式授予，但本文件本身不构成 Merge Authorization。

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

| 依赖 | Readiness 时状态 | U02 处置 | 是否阻塞授权 |
|---|---|---|---|
| Foundation-1 P06 Binding Governance | MERGED / COMPONENT_VERIFIED | REUSE | 否 |
| Foundation-1 Resolver / Invocation Guard | MERGED / COMPONENT_VERIFIED | REUSE | 否 |
| Foundation-1 P05 Trace Baseline | MERGED / COMPONENT_VERIFIED | U02 wiring increment | 否 |
| C01-U01 minimal slice | MERGED / COMPONENT_VERIFIED / INTERNALLY_WIRED / FOUNDATION-1-BINDING-GOVERNED | C01-U02 增量扩展基线 | 否 |
| C01-U02 Clinical Fact Parsing | NOT_IMPLEMENTED | INCREMENTAL_EXTENSION | 否，属于 U02 实现范围 |
| P01 typed Clinical Fact Commit | Foundation mechanical core exists；U02 typed path NOT_IMPLEMENTED | INCREMENTAL_EXTENSION | 否，属于 U02 实现范围 |
| minimum D05 invalidation hook | NOT_IMPLEMENTED | U02 最小版本 | 否，属于 U02 实现范围 |
| U02 Business Unit/Application path | NOT_IMPLEMENTED | NEW | 否，属于 U02 实现范围 |
| External `/continue` / Phase10 cutover | NOT_COMPLETE | DEFERRED | 否，不属于当前 U02 component slice |

---

## 3. C01-U02 Capability Gap Assessment

U02 必须继承 C01-U01 的 typed candidate-only contract、Java typed gateway/client、fail-closed invocation、Foundation-1 `CapabilityInvocationGuard`、binding validation、provenance/uncertainty pattern，以及 `Capability Result != Clinical Truth` 边界。

C01-U02 最小 `ObservationCandidate`：

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

必须区分：

```text
Observation lifecycle
= EXTRACTED / NORMALIZED / CONFIRMED / UNCERTAIN / CONTRADICTED / INVALIDATED

Fact value
= YES / NO / UNKNOWN / UNMEASURED / NOT_ASKED / NOT_APPLICABLE
```

C01-U02 不得直接写 Clinical State、不得直接产生正式 K09 Proposal、不得做 Risk/Safety/Question/DDx，不得把 `MODEL_INFERRED` 伪装成 `PATIENT_REPORTED`。

Eval 最低门槛覆盖：否定、时间、程度、单位、症状/药物/检查、UNKNOWN/UNMEASURED、source attribution、ambiguity、contradiction、mixed input、capability failure/invalid output。

```text
C01-U02 Gap = CONFIRMED
Disposition = INCREMENTAL_EXTENSION
Foundation-2 = NOT_REQUIRED
```

---

## 4. P01-U02 Gap Assessment

复用：`StateCommitter`、`StatePatchBoundaryValidator`、`CommitResult`、`ClinicalCdpStateRepositoryAdapter` 与 CDP version/history persistence。

U02 最小 typed commit：

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

Proposal 至少绑定 proposal_id、consultation_id、base version、business_owner、accepted event/decision、typed operations、evidence/source refs、capability binding refs、idempotency_key。

禁止 Capability Result 直达 StateCommitter、复杂临床对象字符串化 patch、绕过 P01 的 `CDPManager.updateCDP`、静默覆盖版本。

```text
P01-U02 Gap = CONFIRMED
Disposition = INCREMENTAL_EXTENSION DRIVEN BY U02
```

---

## 5. Minimum D05 Scope Assessment

本轮只要求：

```text
Fact changed / corrected
↓
identify registered dependent derived artifacts
↓
mark dependent artifact stale/invalidated/superseded
↓
record invalidation reason + source fact/event + target version
```

目标效果：Risk→STALE、DDx→INVALIDATED、Workup/Delivery→SUPERSEDED，但不提前实现未来模块本身；只建设 dependency/invalidation hook、无依赖时安全 no-op、可追踪 invalidation record。

```text
D05 minimum hook = REQUIRED IN U02
full dependency engine = DEFERRED
```

---

## 6. 冻结施工包

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

明确排除 U03/C02、U04、完整 U05、U06/C03、U08/C04、full D05、full P01/P05/P06、Phase10 external cutover、legacy physical deletion、production activation。

---

## 7. Readiness Verdict 与后续授权事实

```text
U02 Business Design = READY
U02 Legacy Impact Assessment = COMPLETE
C01-U02 Gap = DEFINED
P01-U02 Gap = DEFINED
minimum D05 Scope = DEFINED
Foundation-1 Dependency = SATISFIED
C01-U01 Mainline Dependency = SATISFIED
Foundation-2 = NOT_REQUIRED

U02 Implementation Readiness = PASS / IMPLEMENTATION_READY
```

后续治理事实：

```text
Explicit U02 Implementation Authorization = GRANTED BY USER
Merge Authorization = NOT_GRANTED
Production Authorization = NOT_GRANTED
```
