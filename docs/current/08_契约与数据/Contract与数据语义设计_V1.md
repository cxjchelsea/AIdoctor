# AIdoctor V1 Contract 与数据语义设计

> 文档状态：FROZEN / V1  
> 所属阶段：复杂业务软件开发 SOP — Phase 8 契约与数据设计  
> 上游权威：`01_需求/需求与系统边界_V1.md`、`02_功能/功能模块划分_V1.md`、`03_状态/系统级状态主干_V1.md`、`03_状态/模块级状态与状态所有权_V1.md`、`05_业务闭环/业务闭环设计_V1.md`、`06_开发单元/可验证开发单元拆分_V1.md`、`07_能力设计/按开发单元的Capability设计_V1.md`  
> 当前事实依据：`00_现状与治理/Current_State_Baseline_V1.md`、现有 `contracts/v1/`。  
> 本文冻结跨 Unit / Capability / Governance / Runtime 的业务数据语义与 Contract Family；不冻结 HTTP URL、RPC 方式、数据库表、语言类名、部署拓扑或具体序列化实现。

---

# 1. Phase 8 目标

Phase 7 已回答“每个 Unit 需要什么能力”。Phase 8 回答：

> 这些 Unit、Capability、Policy、State Governance 与 Runtime 之间，究竟交换什么数据？这些数据分别属于候选结果、正式临床状态、业务事件、运行时状态还是审计证据？

本阶段冻结：

```text
Contract Family
数据语义
Owner
Version Binding
Failure Semantics
State Change Proposal 边界
Clinical State / Runtime State / Trace 分层
跨 Unit 最小交换语义
```

本阶段不冻结：

```text
/api/... URL
Java DTO / Python class 名
数据库表
Kafka topic
具体 JSON 字段最终拼写
ORM
网络协议
部署方式
```

---

# 2. Contract 总原则

## CT-01 Contract != State Owner

Contract 只承载信息，不拥有业务真值。

```text
CapabilityResult
!= Clinical State

DeterministicDecision
!= Clinical State

StateChangeProposal
!= committed state

DeliveryValidationResult
!= Delivery Readiness

RiskEvidenceResult
!= Clinical Risk Disposition
```

最终业务真值仍按 Phase 4 Owner 规则形成。

## CT-02 Candidate / Decision / Proposal / Commit 必须分层

统一模式：

```text
Input Context
↓
Capability Result / Policy Input
↓
Business Owner / Deterministic Resolver
↓
Deterministic Decision
↓
State Change Proposal（仅在需要改变 governed Clinical State 时）
↓
G2 State Governance
↓
Commit Result
↓
New Clinical State Version
```

四层语义固定为：

```text
Candidate
= Capability 提供的候选、证据、建议、推断或不确定性

Decision
= Business Owner / Deterministic Policy 对业务语义的正式裁决结果

Proposal
= 基于 Decision 或已验证业务事件形成的、请求修改 governed Clinical State 的提案

Commit
= G2 对 Proposal 做授权、版本、字段权限、幂等、并发、证据等校验后的提交结果
```

必须保持：

```text
Capability Result != Deterministic Decision
Deterministic Decision != StateChangeProposal
StateChangeProposal != CommitResult
```

Capability 不得直接产生 K09 `StateChangeProposal`。

## CT-03 Failure 必须是一等数据

禁止：

```text
null
[]
{}
```

承担“没有结果 / 服务失败 / 信息不足 / 不适用”的多重含义。

业务 Capability Result 使用独立三维语义：

```text
business_status
reason_code
retryable
```

`business_status` 至少支持：

```text
SUCCESS
NO_RESULT
INSUFFICIENT_INFORMATION
NOT_APPLICABLE
UNSUPPORTED
DEPENDENCY_FAILURE
TIMEOUT
INVALID_OUTPUT
SAFETY_BLOCKED
```

其中：

```text
business_status
= 业务层结果语义

reason_code
= 对当前结果原因的可枚举解释

retryable
= 在当前上下文/依赖条件下是否允许工程重试
```

三者不得压缩为一个枚举。

## CT-04 所有临床结果必须绑定版本上下文

凡可能影响临床判断的结果至少关联：

```text
consultation_id
clinical_state_version
capability_id / capability_version（适用时）
scope_version
rule/policy_version（适用时）
knowledge_release_version（适用时）
prompt/model version（使用模型时）
```

不能把基于 v3 Clinical State 产生的结果无条件提交到 v5。

## CT-05 Runtime Contract 与 Clinical Contract 分离

```text
Clinical State
= 患者事实、正式派生临床判断、Risk、Safety Gate、Readiness、Gap、DDx、Workup、Delivery 等 governed 业务真值

Runtime State
= Thread / Run / Checkpoint / Pending Event / Resume execution

Trace Data
= 发生过什么、调用了什么、耗时、结果引用、错误
```

三者不可互相替代。

---

# 3. 现有 contracts/v1 的处置

当前 `contracts/v1/` 已存在通用 Contract 基础，包括：

```text
ContractEnvelope
IdentifierSet
ToolContext
ToolResult
StatePatch
CommitResult
EvidencePack
KnowledgeReleaseRef
AuditRef
PatientDeliveryView
ContractConflict
```

Phase 8 处置：

```text
通用 Envelope / Identifier / Audit / Knowledge Ref
= REUSE_FOUNDATION

ToolResult
= REUSE_FOUNDATION + ADAPT
  仅作为工程 Tool 执行结果基础，不作为新业务 Capability Result 的业务状态真源

StatePatch
= REUSE_FOUNDATION + ADAPT
  保留 proposal / atomic commit 基础思想，但 path 与 value model 必须适配新的 governed Clinical State

CommitResult
= REUSE_FOUNDATION + ADAPT
  保留现有 COMMITTED / REJECTED / CONFLICT / NO_OP / FAILED 基础语义，并建立业务层 NO_EFFECT 映射

PatientDeliveryView
= REFERENCE / ADAPT，不能反向定义 F7 业务真值
```

现有 `ToolResult.suggested_patches` 只代表旧工程基础中的建议 patch；它不能被提升为新的 K09 `StateChangeProposal` 生产权。新的正式 Proposal 仍只能在 Business Owner / Resolver 完成业务解释之后形成。

现有 StatePatch path 仍主要围绕旧 CDP 字段，如 `patient_state/ddx/evidence_graph/workup_plan/management_plan/...`，其中 `management_plan`、`wellness_plan` 与当前 V1 边界并不完全一致，因此不能直接把现有 path allowlist 当成 Phase 8 当前业务真源。

现有 StatePatch `controlledValue` 主要只支持：

```text
string
number
boolean
null
简单数组
```

新的 Clinical State 则需要承载结构化的：

```text
ClinicalObservation
DerivedClinicalAssertion
RiskDecision
SafetyGateDecision
Gap
Question
DDx Candidate
MustExclude
OfflineEvidenceNeed
ExaminationSuggestion
DeliveryPackage
...
```

因此新的 K09 Proposal 必须支持：

```text
typed structured value
或
governed object/reference
```

明确禁止为了迁就旧 schema 而把复杂临床对象 stringify 成 JSON string。

---

# 4. V1 Contract Family 总览

Phase 8 将跨组件数据维持为 10 个 Contract Family：

```text
K01 Identity & Version Context
K02 Business Event
K03 Clinical Observation / Fact
K04 Capability Result
K05 Risk / Safety / Deterministic Decision
K06 Gap / Question
K07 DDx / Evidence / Must-Exclude
K08 Offline Evidence / Workup
K09 State Change Proposal / Commit
K10 Delivery / Resume / Runtime / Trace References
```

这些是语义族，不要求最终实现一定只有 10 个 JSON Schema。

本次冻结保持 10 个 Family 总体结构，不新增平行 Contract Family。

---

# 5. K01 Identity & Version Context

所有跨 Unit 的正式业务请求至少能携带以下身份语义：

```text
consultation_id
subject_id / subject_ref
clinical_state_version
business_event_id
idempotency_key
```

按需增加：

```text
thread_id
run_id
checkpoint_id
capability_id
capability_version
scope_version
rule_version
policy_version
knowledge_release_id/version
prompt_release_id
model_route_version
trace_id
```

原则：

```text
ID 用于身份
Version 用于兼容性与可重放
Trace ID 用于观察
三者不得混用
```

---

# 6. K02 Business Event

业务事件用于 U01/U07/U13/U15 等状态转换触发。

最小语义：

```text
business_event_id
event_type
consultation_id
occurred_at
received_at
actor/source
idempotency_key
payload_ref / payload
expected_state_version（适用时）
```

V1 至少有以下业务事件类别：

```text
START_CONSULTATION
NEW_CLINICAL_INPUT
USER_ANSWER
RESUME_REQUEST
CORRECTION_REQUEST
USER_CANCEL
WAIT_EXPIRED
```

事件本身不等于状态变化。

```text
Business Event
→ Business Validation / Deterministic Decision
→ ACCEPTED / DUPLICATE / EXPIRED / REJECTED
→ 才可能产生业务效果或 State Change Proposal
```

---

# 7. K03 Clinical Observation / Fact Contract

这是 U02 / C01 的核心 Contract。

## 7.1 Observation Candidate

Capability 可产生：

```text
observation_id
concept_id
concept_display
raw_text_ref
normalized_value
value_semantics
unit（适用时）
negation
temporality
severity/degree（适用时）
source_type
confidence / uncertainty
provenance
ambiguity_flags
contradiction_refs
```

`value_semantics` 必须保留 Phase 1/4 已冻结值：

```text
YES
NO
UNKNOWN
UNMEASURED
NOT_ASKED
NOT_APPLICABLE
```

## 7.2 Observation lifecycle 与 Fact value 分离

```text
EXTRACTED
NORMALIZED
CONFIRMED
UNCERTAIN
CONTRADICTED
INVALIDATED
```

是 observation lifecycle；

```text
YES / NO / UNKNOWN / ...
```

是 fact value semantics。

两类 enum 不得混成一个字段。

## 7.3 Patient Fact 与 Derived Clinical Assertion 分离

允许的 source 至少包括：

```text
PATIENT_REPORTED
EXTERNAL_MEASUREMENT
OCR_EXTRACTED
MODEL_INFERRED
RULE_DERIVED
CLINICIAN_CONFIRMED
```

但 source 能进入同一 governed Clinical State，不代表它们属于同一事实等级。

明确分层：

```text
Observed / Reported Fact
= PATIENT_REPORTED
  EXTERNAL_MEASUREMENT
  OCR_EXTRACTED（保留原始来源与提取不确定性）
  CLINICIAN_CONFIRMED（若未来存在正式确认来源）

Derived Clinical Assertion
= MODEL_INFERRED
  RULE_DERIVED
```

必须保持：

```text
Patient Fact != Derived Clinical Assertion
MODEL_INFERRED != PATIENT_REPORTED
RULE_DERIVED != EXTERNAL_MEASUREMENT
```

Derived Clinical Assertion 至少额外绑定：

```text
derived_from_clinical_state_version
input_fact_refs[]
producer_ref
policy/rule/model version（按来源）
confidence / uncertainty（适用时）
validity / staleness
provenance
```

上游事实变更时，派生断言必须可识别为 stale / invalidated，不能继续冒充当前患者事实。

---

# 8. K04 Capability Result Contract

所有概率性或外部 Capability 统一遵循类似结果封装：

```text
result_id
capability_id
capability_version
input_clinical_state_version
business_status
reason_code
retryable
candidates[] / structured_output
evidence_refs[]
recommendations[]
uncertainties[]
provenance
version_refs
errors[]
started_at
completed_at
```

Capability Result 只允许提供：

```text
candidate
evidence
recommendation
uncertainty
failure
provenance
```

正式禁止在 K04 中包含：

```text
state_change_proposals[]
K09 StateChangeProposal
```

如某能力确有必要表达“建议影响”，只能使用非治理对象，例如：

```text
suggested_effect
```

且必须满足：

```text
suggested_effect
!= K09 StateChangeProposal
!= state mutation authorization
!= committed state
```

`suggested_effect` 只能作为 Business Owner / Resolver 的输入之一；正式 K09 Proposal 只能由业务 Owner / Resolver 在完成业务解释后形成。

## 8.1 与现有 ToolResult 的关系

现有 ToolResult 工程状态为：

```text
SUCCEEDED
NO_RESULT
RETRYABLE_FAILURE
NON_RETRYABLE_FAILURE
TIMED_OUT
POLICY_BLOCKED
```

这些状态描述“工具执行发生了什么”，不能直接成为新的业务 Capability Result 真源。

新旧语义至少按三个维度适配：

```text
Tool execution status
→ 工程执行结果

business_status
→ 业务结果语义

reason_code + retryable
→ 原因与是否允许重试
```

典型映射只作为适配原则，不作为唯一机械映射：

```text
SUCCEEDED
→ 仍需根据业务输出判断 SUCCESS / NO_RESULT / INSUFFICIENT_INFORMATION / NOT_APPLICABLE ...

NO_RESULT
→ 通常映射业务 NO_RESULT，但仍需 reason_code 说明原因

RETRYABLE_FAILURE / NON_RETRYABLE_FAILURE
→ 由失败类型形成 DEPENDENCY_FAILURE / INVALID_OUTPUT / ...，retryable 独立表达

TIMED_OUT
→ business_status = TIMEOUT，retryable 由上下文决定

POLICY_BLOCKED
→ 可映射 SAFETY_BLOCKED / UNSUPPORTED / NOT_APPLICABLE 等，必须由业务语义解释，不能直接复制枚举
```

---

# 9. K05 Risk / Safety / Deterministic Decision Contract

## 9.1 C02 输出：Risk Evidence Result

只能包含：

```text
red_flag_hits[]
risk_factor_hits[]
vital_sign_signals[]
must_not_miss_signals[]
proposed_assessment
uncertainties[]
evidence_refs[]
```

不得直接拥有最终：

```text
Clinical Risk Disposition
Safety Gate
```

## 9.2 统一 Deterministic Decision Contract

D01-D10 中凡形成正式业务裁决的 Policy / Resolver，统一使用 Deterministic Decision 语义。

最小字段：

```text
decision_id
decision_type
consultation_id
input_clinical_state_version
decision
reason_codes[]
basis_refs[]
policy_id
policy_version
input_refs[]
created_at
```

按需增加：

```text
allowed_actions[]
blocked_actions[]
next_business_intent
violations[]
validity / staleness
```

至少覆盖：

```text
CONSULTATION_LIFECYCLE
SCOPE_ADJUDICATION
RISK_DISPOSITION
SAFETY_GATE
CLINICAL_READINESS
QUESTION_STOPPING
DELIVERY_VALIDATION
FAILURE_ROUTING
RESUME_BUSINESS_VALIDATION
CANCEL_EXPIRE
CORRECTION_INVALIDATION
```

其中一些 `decision_type` 对应 D01-D10 的直接输出，一些是同一 Policy 在特定 Unit 中形成的业务裁决实例；这不新增业务 Owner。

必须保持：

```text
Deterministic Decision Result
!= StateChangeProposal
```

Decision 只有在需要改变 governed Clinical State 时，才由相应 Business Owner / Resolver 转换为 K09 Proposal。

## 9.3 D09 输出：Risk Disposition Decision

使用统一 Deterministic Decision Contract，`decision_type = RISK_DISPOSITION`，业务 decision 为：

```text
NOT_EVALUATED
NO_HIGH_RISK_SIGNAL
CAUTION
HIGH_RISK
```

并至少具有：

```text
basis_refs[]
policy_id = D09
policy_version
reason_codes[]
```

F4 / D09 拥有 Clinical Risk 的业务裁决语义；C02 不拥有。

## 9.4 D02 输出：Safety Gate Decision

使用统一 Deterministic Decision Contract，`decision_type = SAFETY_GATE`，并至少关联：

```text
risk_decision_ref
capability_availability_refs[]
scope/authorization/consent refs
reason_codes[]
policy_id = D02
policy_version
```

业务 decision：

```text
ALLOW
RESTRICTED
BLOCKED
UNAVAILABLE
```

Risk 与 Safety Gate 必须是两个不同 Decision。

---

# 10. K06 Gap / Question Contract

## 10.1 Information Gap

至少表达：

```text
gap_id
target_concept / target_decision
status
decision_impact
source_basis_refs[]
askable_online
invalidated_by[]
```

状态沿用 Phase 4：

```text
IDENTIFIED
QUESTIONABLE_ONLINE
ASKED
ANSWERED
USER_UNKNOWN
UNMEASURED
OFFLINE_REQUIRED
WAIVED
RESOLVED
INVALIDATED
```

Decision Impact：

```text
BLOCKING
UNCERTAINTY_INCREASING
DEFERABLE
OFFLINE_ONLY
```

## 10.2 Question Candidate / Pending Question

```text
question_id
question_purpose
source_requirement_ref
candidate_text / rendered_text
expected_decision_value
target_concepts[]
clinical_state_version
status
```

Question lifecycle：

```text
PROPOSED
SELECTED
DELIVERED_TO_USER
ANSWER_RECEIVED
EXPIRED
SUPERSEDED
```

`WAITING_USER` 不是 Question Contract 字段的替代物；只有 `DELIVERED_TO_USER` 成功后业务生命周期才能进入 WAITING_USER。

Question Stopping 由 D04 的 Deterministic Decision 产生，不由 Question Capability 自己决定。

---

# 11. K07 DDx / Evidence / Must-Exclude Contract

## 11.1 Disease Direction Candidate

至少表达：

```text
candidate_id
concept_id
display_name
tier
status
supporting_evidence_refs[]
opposing_evidence_refs[]
unknown_evidence_refs[]
knowledge/rule refs
clinical_state_version
```

Candidate Tier：

```text
LIKELY
ACTIVE_ALTERNATIVE
MUST_EXCLUDE
```

Candidate lifecycle：

```text
PROPOSED
ACTIVE
SUPPORTED
WEAKENED
EXCLUDED
INVALIDATED
```

## 11.2 Must-Exclude Assessment

```text
candidate_ref
status
required_evidence_refs[]
missing_evidence_refs[]
reason_codes[]
```

状态：

```text
UNASSESSED
NOT_EXCLUDED
EXCLUDED
NEEDS_OFFLINE_EXCLUSION
INVALIDATED
```

必须保持：

```text
UNKNOWN / insufficient evidence != EXCLUDED
NO_DDX != LOW_RISK
```

## 11.3 Evidence Relation

必须区分：

```text
patient evidence
clinical rule evidence
KG reasoning path
medical knowledge citation
model inference
```

并表达关系：

```text
SUPPORTS
OPPOSES
UNKNOWN_FOR
```

KG path 不自动升级为 medical citation。

---

# 12. K08 Offline Evidence / Workup Contract

Offline Evidence Assessment 至少表达：

```text
assessment_id
clinical_state_version
status
needs[]
reason_codes[]
evidence_refs[]
```

Assessment status：

```text
NOT_ASSESSED
ASSESSING
VALID
STALE
FAILED
```

每个 Offline Evidence Need 至少有：

```text
need_id
target_gap / ddx / must_exclude / risk ref
need_status
rationale
```

Examination Suggestion 至少有：

```text
suggestion_id
examination_concept
rationale_ref
policy/knowledge refs
status
```

状态：

```text
PROPOSED
VALIDATED
REJECTED_BY_POLICY
SUPERSEDED
DELIVERABLE
```

禁止把 Suggestion Contract 设计成 Order/Prescription Contract。

---

# 13. K09 State Change Proposal / Commit Contract

这是 G2 的核心边界。

## 13.1 State Change Proposal

正式 Proposal 只能在 Business Owner / Resolver 已经完成业务解释后产生。

至少表达：

```text
proposal_id
consultation_id
base_clinical_state_version
producer
business_owner
source_decision_ref / accepted_event_ref
reason_code
operations[]
evidence_refs[]
source_refs[]
idempotency_key
created_at
```

operations 的语义至少支持：

```text
ADD
REPLACE
REMOVE
TEST / PRECONDITION
```

但 Phase 8 冻结的是业务语义，不冻结 JSON Pointer、字段路径最终形式。

## 13.2 Proposal producer 边界

正式链路：

```text
Capability Result
→ Business Owner / Deterministic Resolver interprets
→ Deterministic Decision / accepted business effect
→ State Change Proposal
→ G2
→ Commit Result
```

禁止：

```text
Capability Result
→ 直接生产正式 K09 StateChangeProposal
```

`producer` 可以记录生成 Proposal 的业务组件/Resolver，但不能借此改变 Phase 4 Owner。

## 13.3 Typed structured value

新的 Proposal operation value 必须能够表达：

```text
typed structured value
或
governed object/reference
```

用于合法承载 ClinicalObservation、DerivedClinicalAssertion、Risk/Safety/Readiness Decision、Gap、DDx、MustExclude、DeliveryPackage 等对象。

明确禁止：

```text
complex clinical object
→ JSON.stringify(...)
→ 塞进 string value
```

具体 schema、JSON Pointer、对象内联还是引用，由后续 Contract Spec / 实现阶段决定；Phase 8 只冻结“必须支持结构化 governed value”这一语义要求。

## 13.4 Commit Result

现有 `contracts/v1` 的基础状态保持：

```text
COMMITTED
REJECTED
CONFLICT
NO_OP
FAILED
```

Phase 8 处置为：

```text
CommitResult = REUSE_FOUNDATION + ADAPT
```

返回至少包括：

```text
previous_version
new/committed_version（若成功）
reason_code / reason_codes[]
audit_ref
conflict_detail（适用时）
retryable
```

业务层必须明确：

```text
NO_EFFECT
!= COMMIT_FAILED
!= REJECTED
```

其中现有工程 `NO_OP` 可适配为业务 `NO_EFFECT`，reason_code 用于进一步区分：

```text
DUPLICATE_EVENT
ALREADY_APPLIED
VALUE_UNCHANGED
NO_APPLICABLE_CHANGE
...
```

`FAILED` 表示提交过程自身失败，不能被解释成 NO_EFFECT。

Capability success + Commit reject/fail/conflict = Unit 未完成预期状态提交。

## 13.5 Invalidation

D05 负责形成 dependency invalidation decision / requirement，例如：

```text
Risk → STALE
DDx → INVALIDATED
Workup → SUPERSEDED
Delivery → SUPERSEDED
```

D05 不成为这些状态的跨模块 Owner。最终失效效果仍由各业务 Owner 语义与 G2 规则转换为受治理的 State Change Proposal / Commit。

---

# 14. K10 Delivery / Resume / Runtime / Trace References

## 14.1 Delivery Package Contract

结构化 Delivery Package 在渲染前形成，至少关联：

```text
delivery_id
delivery_type  # NORMAL / SAFE_EXIT
clinical_state_version
risk_ref
safety_gate_ref
readiness_ref
ddx_refs[]
must_exclude_refs[]
evidence_refs[]
uncertainty_refs[]
offline_evidence_refs[]
action_guidance
boundary_disclosures[]
rendered_content_ref
```

Delivery Validator 使用统一 Deterministic Decision Contract，`decision_type = DELIVERY_VALIDATION`，按需包含：

```text
violations[]
allowed_actions[]
blocked_actions[]
```

其结果不等于 `Delivery Readiness`；F7 根据验证结果形成正式 Delivery Readiness。

## 14.2 Business Resume Event Decision

用户回答/Resume 的业务有效性属于 F8/业务层，不属于 Runtime。

Business Resume Event Decision 使用统一 Deterministic Decision Contract，`decision_type = RESUME_BUSINESS_VALIDATION`，业务处理生命周期可表达：

```text
RECEIVED
VALIDATING
ACCEPTED
DUPLICATE
EXPIRED
REJECTED
APPLIED
```

其中：

```text
ACCEPTED
= 该业务事件被允许产生后续业务效果

DUPLICATE / EXPIRED / REJECTED
= 不允许产生新的临床效果

APPLIED
= 经业务链与必要 State Commit 后，该事件效果已经落地
```

必须保持：

```text
same event cannot produce duplicate clinical effects
```

## 14.3 Runtime Execution Resume Result

Runtime 只判断执行上下文能否恢复以及恢复结果，不裁决用户回答的业务合法性。

最小语义：

```text
thread_id
checkpoint_id
referenced_clinical_state_version
checkpoint_compatible
runtime_resume_status
run_id
failure_ref
```

可按需关联：

```text
pending_event_id
pending_question_id
resume_event_id
expires_at
```

`runtime_resume_status` 表达执行恢复状态，例如：

```text
RESUMED
CHECKPOINT_INCOMPATIBLE
CHECKPOINT_MISSING
RUNTIME_FAILED
```

具体执行枚举可在 Phase 9 落地，但不得复用 `ACCEPTED / DUPLICATE / EXPIRED / REJECTED / APPLIED` 来代替业务 Resume 决策。

Checkpoint 不保存/替代 Clinical Truth，只引用相应 Clinical State Version。

## 14.4 Trace / Audit Reference

Trace 可以记录：

```text
unit_id
capability_id
run_id
input_ref
output_ref
decision_ref
state_proposal_ref
commit_result_ref
failure_ref
duration/status
```

默认不得为了“可观察性”复制完整 PHI payload。

---

# 15. Clinical State 数据域

V1 正式 Versioned Clinical State / CDP 至少需要承载或权威引用以下业务域语义：

```text
subject_context
problem_framing
clinical_observations / patient_facts
derived_clinical_assertions
information_gaps
questions / pending question refs
clinical_risk
safety_gate
clinical_readiness
delivery_readiness
red_flags
ddx_candidates
must_exclude
evidence_relations
offline_evidence_needs
examination_suggestions
delivery_package refs
uncertainty
version metadata
```

四个系统级正式状态必须都存在于 governed Clinical State 或由其权威引用：

```text
clinical_risk
safety_gate
clinical_readiness
delivery_readiness
```

每个派生状态至少绑定：

```text
derived_from_clinical_state_version
decision_ref
policy/rule version
validity / staleness
```

因此 Safety Gate 不能只存在于：

```text
临时函数返回值
Runtime state
Trace
前端状态
```

注意：这是“业务域语义”，不是数据库字段清单。

以下不得重新进入 V1 正式核心：

```text
autonomous treatment plan
autonomous prescription
wellness plan as V1 clinical truth
fixed current_step=1..5 as system truth
frontend-generated clinical defaults
```

---

# 16. Runtime State 数据域

Runtime 数据独立保存：

```text
thread
run
checkpoint
interrupt reason
pending event
resume token/ref
retry/repair state
execution status
expiry
idempotency execution metadata
```

原则：

```text
Runtime Checkpoint
→ 引用 Clinical State Version

不得复制一份独立 Clinical Truth 后长期漂移
```

Runtime 的 execution resume 也不得承担 Business Resume validity。

---

# 17. U01–U15 Contract 映射

| Unit | 主要输入 Contract | 主要输出 Contract |
|---|---|---|
| U01 | K02 START_CONSULTATION + K01 Context | Subject/Problem semantic result + Scope Deterministic Decision + 必要的 K09 Proposal |
| U02 | K02 NEW_CLINICAL_INPUT + K03 candidates | K03 governed facts/assertions + K09 Commit Result |
| U03 | K03 current facts | K05 Risk Evidence + Risk Disposition Decision + 必要的 K09 Proposal |
| U04 | Risk Decision + capability/policy refs | K05 Safety Gate Decision + 必要的 K09 Proposal |
| U05 | F1/F3/F5/F6 readiness inputs | Clinical Readiness Decision + K09 Proposal |
| U06 | Clarification/Gap Contract | K06 Question + Stopping Decision + wait transition proposal |
| U07 | K02 Resume Event + Runtime refs | Business Resume Event Decision + Runtime Execution Resume Result + event effect/no-effect |
| U08 | K03 facts + K07 evidence context | K07 DDx/Must-Exclude candidates/assessment + Owner interpretation + 必要 proposals |
| U09 | K07 DDx + K06 gaps | updated K06 Gap + Question Stopping / Readiness Decisions |
| U10 | readiness + K07 refs | K08 Offline Evidence/Exam Suggestion + 必要 Proposal |
| U11 | Safe Exit reason + current state refs | K10 Safe Exit Delivery + Delivery Validation Decision + Delivery Readiness proposal |
| U12 | normal delivery prerequisites | K10 Normal Delivery + Delivery Validation Decision + Delivery Readiness proposal |
| U13 | K02 Correction Event | correction decision + invalidation decisions + owner-governed proposals |
| U14 | Failure Contract + current safety/runtime context | Failure Routing Decision + retry/fallback/safe-exit/terminal intent |
| U15 | Cancel/Expire Event + Runtime refs | Cancel/Expire Decision + lifecycle terminal effect + late-event rejection semantics |

所有产生正式 Clinical State 变更的输出最终仍必须经过 K09 / G2。

---

# 18. 版本与兼容性原则

Phase 8 冻结：

```text
Contract Version
Clinical State Version
Capability Version
Policy/Rule Version
Knowledge Version
Prompt/Model Version
```

是不同维度。

禁止：

```text
contract_version == clinical_state_version
```

之类的语义混用。

至少遵循：

- Contract 不兼容变化需要显式版本升级；
- 同一 Consultation 运行中绑定的 Scope/Capability 版本不得静默切换；
- 派生结果必须记录其依赖版本；
- 新事实提交导致 Clinical State Version 变化时，旧派生结果必须可以识别为 stale/invalid；
- Risk / Safety Gate / Clinical Readiness / Delivery Readiness 必须能追溯到其 Decision 与派生版本；
- Runtime Resume 必须校验 checkpoint 所引用的 Clinical State Version 是否仍兼容。

---

# 19. PHI 与最小上下文原则

Contract 设计遵循：

```text
需要什么传什么
而不是把完整 CDP 传给所有 Capability
```

每个 Capability 的输入应由后续 Contract Spec 明确：

```text
required fields
optional fields
forbidden fields
sensitivity
purpose
retention/logging rule
```

Trace / Error / Audit 默认使用引用与摘要，不默认复制完整患者原文。

---

# 20. Phase 8 全局不变量

```text
CONTRACT-INV-01 CapabilityResult != committed Clinical State
CONTRACT-INV-02 CapabilityResult cannot directly produce formal K09 StateChangeProposal
CONTRACT-INV-03 DeterministicDecision != StateChangeProposal
CONTRACT-INV-04 StateChangeProposal != CommitResult
CONTRACT-INV-05 Clinical State != Runtime Checkpoint
CONTRACT-INV-06 Trace != Clinical Truth
CONTRACT-INV-07 Failure 不能通过 null/[]/{} 隐式表达
CONTRACT-INV-08 UNKNOWN != NO；UNMEASURED != NORMAL
CONTRACT-INV-09 Patient Fact != Derived Clinical Assertion
CONTRACT-INV-10 Risk Evidence != Clinical Risk Disposition
CONTRACT-INV-11 Clinical Risk Disposition != Safety Gate
CONTRACT-INV-12 DeliveryValidationResult != Delivery Readiness
CONTRACT-INV-13 Scope semantic extraction != OUT_OF_SCOPE final decision
CONTRACT-INV-14 所有正式 Clinical State 写入必须经 G2/K09
CONTRACT-INV-15 所有临床派生结果必须可追溯到输入 Clinical State Version
CONTRACT-INV-16 Duplicate Event 不得产生第二次临床效果
CONTRACT-INV-17 Business Resume validity != Runtime Execution Resume
CONTRACT-INV-18 Checkpoint 只能引用而不能替代 Clinical State
CONTRACT-INV-19 Patient/Rule/KG/Citation/Model evidence provenance 不得互相冒充
CONTRACT-INV-20 Suggestion != Medical Order / Prescription
CONTRACT-INV-21 Frontend View Contract 不得制造新的临床语义
CONTRACT-INV-22 NO_EFFECT != COMMIT_FAILED != REJECTED
CONTRACT-INV-23 Complex governed Clinical State value 不得通过 JSON string 伪装为标量
CONTRACT-INV-24 clinical_risk / safety_gate / clinical_readiness / delivery_readiness 均属于 governed system-level state
```

---

# 21. 与后续 Phase 9 的边界

Phase 8 冻结“说什么数据”，Phase 9 才回答“这些 Contract 如何运行”。

Phase 9 将进一步决定：

```text
哪些 Unit 在 Java / Python / Runtime 中编排
Capability 调用边界
同步/异步执行方式
Checkpoint 时机
StateCommitter 接管路径
Model Runtime 调用路径
失败、重试与恢复执行机制
服务与模块拓扑
具体 Runtime resume status schema
```

因此 Phase 8 不提前把 Contract Family 映射成微服务或 HTTP API。

---

# 22. Phase 8 冻结结论

本阶段已经能够回答：

> 当 U01–U15 开始真正实现时，各 Unit、Capability、Resolver、State Governance 与 Runtime 之间传递的数据分别是什么语义，哪些只是候选，哪些是业务裁决，哪些可以形成正式状态变更提案，版本和 Failure 如何表达，以及 Resume 的业务有效性和 Runtime 恢复如何分离。

已冻结内容：

- 10 个 Contract Family 总体结构；
- Clinical State / Runtime State / Trace 三层数据边界；
- Candidate / Decision / Proposal / Commit 四层边界；
- Observation / Fact / Derived Assertion / Risk / Safety / Gap / DDx / Workup / Delivery / Resume 核心语义；
- State Change Proposal → G2 → Commit 的统一链；
- Deterministic Decision Contract；
- U01–U15 Contract 映射；
- Failure、Version、Evidence Provenance 与 PHI 基础规则；
- Business Resume 与 Runtime Execution Resume 分离；
- 对现有 ToolResult / StatePatch / CommitResult 的 REUSE_FOUNDATION + ADAPT 判断；
- governed Clinical State 对 Clinical Risk / Safety Gate / Clinical Readiness / Delivery Readiness 的完整承载要求；
- typed structured state value 要求。

冻结前独立审查提出的 P8-R01 ～ P8-R08 已全部封口：

```text
P8-R01 CLOSED — Capability Result 不再承载正式 StateChangeProposal
P8-R02 CLOSED — 增加统一 Deterministic Decision Contract
P8-R03 CLOSED — Safety Gate 纳入 governed Clinical State
P8-R04 CLOSED — Business Resume Decision 与 Runtime Execution Resume Result 分离
P8-R05 CLOSED — CommitResult 改为 REUSE_FOUNDATION + ADAPT，保留 NO_OP/FAILED 并定义 NO_EFFECT
P8-R06 CLOSED — Tool execution status 与 business_status/reason_code/retryable 分离
P8-R07 CLOSED — Proposal 支持 typed structured value / governed object reference
P8-R08 CLOSED — Patient Fact 与 Derived Clinical Assertion 明确分层
```

最终状态：

```text
SOP Phase 8 — Contract & Data Design
= FROZEN / V1
```

只有从本冻结基线继续，才可进入 Phase 9 — Runtime 与技术架构；本文件本身不构成任何实现授权或 merge 授权。
