# AIdoctor V1 契约与数据语义设计（Phase 7 补齐同步版）

> 文档状态：V1 同步修订稿  
> 所属阶段：复杂业务软件开发 SOP — Phase 8 契约与数据设计  
> 上游权威：Phase 1–7 已冻结设计，其中 Phase 7 已补齐 P04 医学知识与证据治理、P06 能力范围/能力包/版本治理。  
> 当前事实依据：`00_现状与治理/Current_State_Baseline_V1.md`、现有 `contracts/v1/`。  
> 本次修订原则：**不新增 K11，不改变 K01–K10 总体结构，不改变 Candidate → Decision → Proposal → Commit 主链，只同步补齐 Phase 7 新增治理语义。**

---

# 1. Phase 8 目标

Phase 7 回答：

> 每个业务开发单元需要哪些临床能力、平台能力与确定性规则？

Phase 8 回答：

> 这些业务开发单元、临床能力、确定性规则、状态治理与运行时之间究竟交换什么数据？这些数据分别属于候选结果、正式业务裁决、状态变更提案、正式临床状态、运行时状态还是审计证据？

本阶段冻结：

```text
契约族
数据语义
业务 Owner
版本绑定
失败语义
能力绑定
知识发布绑定
规则发布绑定
状态变更提案边界
Clinical State / Runtime State / Trace 分层
跨 Unit 最小交换语义
```

本阶段不冻结：

```text
HTTP URL
RPC 方式
数据库表
Java DTO / Python class 最终类名
Kafka topic
具体 JSON 字段最终拼写
ORM
网络协议
部署方式
```

---

# 2. 契约总原则

## CT-01 契约不拥有业务真值

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

最终业务真值仍按 Phase 4 的 Owner 规则形成。

---

## CT-02 Candidate / Decision / Proposal / Commit 必须分层

```text
输入上下文
↓
能力结果 / 规则输入
↓
业务 Owner / 确定性裁决器
↓
确定性业务裁决
↓
状态变更提案
↓
G2 临床状态治理
↓
提交结果
↓
新的 Clinical State Version
```

四层语义：

```text
Candidate
= 能力提供的候选、证据、建议、推断、不确定性

Decision
= 业务 Owner / 确定性规则形成的正式业务裁决

Proposal
= 请求修改 governed Clinical State 的正式提案

Commit
= G2 完成授权、版本、字段权限、幂等、并发、证据校验后的提交结果
```

必须保持：

```text
Capability Result != Deterministic Decision
Deterministic Decision != StateChangeProposal
StateChangeProposal != CommitResult
```

临床能力不得直接产生正式 K09 `StateChangeProposal`。

---

## CT-03 Failure 必须是一等数据

禁止使用：

```text
null
[]
{}
```

同时表达“没有结果 / 服务失败 / 信息不足 / 不适用”。

业务能力结果至少拆成：

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

---

## CT-04 所有临床结果必须绑定版本上下文

凡可能影响正式临床判断的结果，至少关联：

```text
consultation_id
clinical_state_version

capability_binding_ref（适用时）
scope_version

rule_release_ref（适用时）
knowledge_release_ref（适用时）

prompt_release_ref（使用模型时）
model_route_ref（使用模型时）

contract_version
```

禁止：

```text
基于 Clinical State v3 产生结果
→ 无校验直接写入 v5
```

---

## CT-05 Runtime Contract 与 Clinical Contract 分离

```text
Clinical State
= 患者事实、正式派生判断、Risk、Safety Gate、Readiness、Gap、DDx、Workup、Delivery 等 governed 业务真值

Runtime State
= Thread / Run / Checkpoint / Pending Event / Resume execution

Trace Data
= 发生了什么、调用了什么、耗时、结果引用、错误
```

三者不可互相替代。

---

## CT-06 能力存在不等于能力可用

新增同步约束：

```text
Capability Exists
!= Capability Evaluated
!= Capability Active
!= Capability Authorized
```

因此 Runtime 不能只凭：

```text
capability_id + capability_version
```

判断能力可用。

必须依据正式的：

```text
CapabilityBindingRef
```

验证该能力当前是否允许用于本次 Consultation。

---

## CT-07 知识存在不等于知识可用于正式临床判断

```text
Knowledge Candidate
!= Knowledge Published
!= Knowledge Active
```

候选知识、实验知识、已撤回知识、已过期知识不得成为新临床判断的正式依据。

正式使用必须绑定：

```text
KnowledgeReleaseRef
```

---

# 3. 现有 contracts/v1 的处置

现有基础包括：

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

处置：

```text
Envelope / Identifier / Audit / Knowledge Ref
= REUSE_FOUNDATION

ToolResult
= REUSE_FOUNDATION + ADAPT
仅作为工程 Tool 执行结果基础

StatePatch
= REUSE_FOUNDATION + ADAPT
保留 proposal / atomic commit 基础思想

CommitResult
= REUSE_FOUNDATION + ADAPT

PatientDeliveryView
= REFERENCE / ADAPT
不能反向定义 F7 业务真值

KnowledgeReleaseRef
= REUSE_FOUNDATION + ADAPT
需要补齐发布状态、生效期、范围、回滚与来源语义
```

现有 `ToolResult.suggested_patches` 不能提升为新的 K09 `StateChangeProposal` 生产权。

新的正式 Proposal 仍只能在 Business Owner / Resolver 完成业务解释后形成。

---

# 4. V1 契约族总览

保持 10 个契约族，不新增 K11：

```text
K01 身份、版本与治理绑定上下文
K02 业务事件
K03 临床观察 / 事实
K04 能力结果
K05 风险 / 安全 / 确定性裁决
K06 信息缺口 / 问题
K07 鉴别方向 / 证据 / 必须排除项
K08 线下证据 / 检查建议
K09 状态变更提案 / 提交
K10 交付 / Resume / Runtime / Trace 引用
```

---

# 5. K01 身份、版本与治理绑定上下文

所有正式业务请求至少携带：

```text
consultation_id
subject_id / subject_ref
clinical_state_version
business_event_id
idempotency_key
contract_version
```

按需增加：

```text
thread_id
run_id
checkpoint_id
trace_id
```

本次同步新增三个核心引用对象：

```text
CapabilityBindingRef
KnowledgeReleaseRef
RuleReleaseRef
```

---

## 5.1 CapabilityBindingRef — 能力绑定引用

目的：

> 表达“当前 Consultation 在当前范围、版本、人群与治理条件下，究竟允许使用哪个能力版本”。

最小语义：

```text
capability_id
capability_version
binding_status

scope_version
population_ref
region
language
channel

effective_from
effective_until

allowed_tool_refs[]
allowed_skill_refs[]
allowed_prompt_release_refs[]
allowed_model_route_refs[]
allowed_knowledge_release_refs[]
allowed_rule_release_refs[]

evaluation_refs[]
rollback_capability_version

binding_created_at
binding_source
```

推荐状态语义：

```text
DRAFT
CLINICAL_REVIEW
TECHNICAL_REVIEW
EVALUATION
SHADOW
RESTRICTED_ACTIVE
ACTIVE
DEPRECATED
RETIRED
```

必须保持：

```text
DRAFT != ACTIVE
EVALUATION != ACTIVE
DEPRECATED != RETIRED
RETIRED != DELETED
```

正式 Runtime 使用能力前至少验证：

```text
status 可用
当前时间在有效期内
scope 命中
population 合法
region/language/channel 合法
依赖的知识/规则/模型/Prompt/Tool 均处于允许列表
契约版本兼容
```

---

## 5.2 KnowledgeReleaseRef — 知识发布引用

最小语义：

```text
knowledge_release_id
knowledge_version
status

effective_from
effective_until

scope_ref
population_ref
region
language

source_refs[]
provenance_refs[]
evaluation_refs[]

supersedes_refs[]
conflict_refs[]
rollback_target_ref

published_at
withdrawn_at
```

推荐状态：

```text
CANDIDATE
VALIDATING
READY_TO_PUBLISH
PUBLISHED
DEPRECATED
WITHDRAWN
EXPIRED
RETIRED
```

必须保持：

```text
CANDIDATE != PUBLISHED
WITHDRAWN != ACTIVE
EXPIRED != ACTIVE
```

正式能力只能使用当前已批准且有效的知识发布版本。

---

## 5.3 RuleReleaseRef — 规则发布引用

适用于：

```text
红旗规则
风险组合规则
特殊人群规则
Safety Gate 规则
停止规则
检查建议规则
Delivery 规则
```

最小语义：

```text
rule_release_id
rule_set_id
rule_version
status

effective_from
effective_until

scope_ref
population_ref
region

source_refs[]
knowledge_release_refs[]
evaluation_refs[]

supersedes_refs[]
rollback_target_ref
```

安全关键规则必须显式版本化，禁止使用未发布规则直接形成正式 Clinical Decision。

---

## 5.4 版本与治理引用原则

```text
ID
= 身份

Version
= 可重放与兼容性

Binding / Release
= 当前是否允许使用

Trace
= 观察发生过什么
```

四者不得混用。

---

# 6. K02 业务事件

用于 U01/U07/U13/U15 等状态转换触发。

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
expected_state_version
```

V1 至少包括：

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

# 7. K03 临床观察 / 事实契约

## 7.1 Observation Candidate

至少：

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
severity/degree
source_type
confidence / uncertainty
provenance
ambiguity_flags
contradiction_refs
```

`value_semantics`：

```text
YES
NO
UNKNOWN
UNMEASURED
NOT_ASKED
NOT_APPLICABLE
```

---

## 7.2 Observation lifecycle 与 Fact value 分离

Observation lifecycle：

```text
EXTRACTED
NORMALIZED
CONFIRMED
UNCERTAIN
CONTRADICTED
INVALIDATED
```

Fact value：

```text
YES
NO
UNKNOWN
UNMEASURED
NOT_ASKED
NOT_APPLICABLE
```

两者不能混成一个字段。

---

## 7.3 Patient Fact 与 Derived Clinical Assertion 分离

允许来源至少：

```text
PATIENT_REPORTED
EXTERNAL_MEASUREMENT
OCR_EXTRACTED
MODEL_INFERRED
RULE_DERIVED
CLINICIAN_CONFIRMED
```

必须保持：

```text
Patient Fact != Derived Clinical Assertion
MODEL_INFERRED != PATIENT_REPORTED
RULE_DERIVED != EXTERNAL_MEASUREMENT
```

Derived Clinical Assertion 至少绑定：

```text
derived_from_clinical_state_version
input_fact_refs[]
producer_ref
rule_release_ref（适用时）
capability_binding_ref（适用时）
prompt/model refs（适用时）
confidence / uncertainty
validity / staleness
provenance
```

---

# 8. K04 能力结果契约

所有概率性或外部能力统一使用类似结果封装：

```text
result_id

capability_binding_ref
input_clinical_state_version

business_status
reason_code
retryable

candidates[] / structured_output
evidence_refs[]
recommendations[]
uncertainties[]

knowledge_release_refs[]
rule_release_refs[]
prompt_release_refs[]
model_route_refs[]

provenance
errors[]
started_at
completed_at
```

能力结果只允许提供：

```text
candidate
evidence
recommendation
uncertainty
failure
provenance
```

禁止直接包含：

```text
K09 StateChangeProposal
```

如需表达建议影响，只能用非治理对象：

```text
suggested_effect
```

且：

```text
suggested_effect
!= StateChangeProposal
!= state mutation authorization
```

---

## 8.1 与现有 ToolResult 的关系

工程 Tool status 与业务能力状态继续分离：

```text
Tool execution status
→ 工程执行结果

business_status
→ 业务结果语义

reason_code + retryable
→ 原因与是否允许重试
```

---

## 8.2 能力绑定校验

K04 结果必须能证明：

```text
执行时使用的 capability binding 有效
```

若：

```text
binding 已过期
binding 已退役
scope 不匹配
knowledge/rule/model/prompt 不在允许列表
```

则该结果不能进入正式业务解释链。

---

# 9. K05 风险 / 安全 / 确定性裁决契约

## 9.1 C02 风险证据结果

只能包含：

```text
red_flag_hits[]
risk_factor_hits[]
vital_sign_signals[]
must_not_miss_signals[]
proposed_assessment
uncertainties[]
evidence_refs[]
knowledge_release_refs[]
rule_release_refs[]
```

不得拥有：

```text
Clinical Risk Disposition
Safety Gate
```

---

## 9.2 统一 Deterministic Decision Contract

D01–D10 使用统一确定性裁决语义。

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
rule_release_refs[]
knowledge_release_refs[]

input_refs[]
created_at
validity / staleness
```

按需增加：

```text
allowed_actions[]
blocked_actions[]
next_business_intent
violations[]
```

覆盖：

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

必须保持：

```text
Deterministic Decision != StateChangeProposal
```

---

## 9.3 D09 Risk Disposition

至少绑定：

```text
basis_refs[]
policy_id = D09
policy_version
rule_release_refs[]
knowledge_release_refs[]
reason_codes[]
```

业务 decision：

```text
NOT_EVALUATED
NO_HIGH_RISK_SIGNAL
CAUTION
HIGH_RISK
```

---

## 9.4 D02 Safety Gate

至少关联：

```text
risk_decision_ref
capability_availability_refs[]
scope/authorization/consent refs
rule_release_refs[]
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

---

# 10. K06 信息缺口 / 问题契约

## 10.1 Information Gap

```text
gap_id
target_concept / target_decision
status
decision_impact
source_basis_refs[]
askable_online
invalidated_by[]
```

状态：

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

---

## 10.2 Question Candidate / Pending Question

```text
question_id
question_purpose
source_requirement_ref
candidate_text / rendered_text
expected_decision_value
target_concepts[]
clinical_state_version
capability_binding_ref
question_policy_ref
status
```

生命周期：

```text
PROPOSED
SELECTED
DELIVERED_TO_USER
ANSWER_RECEIVED
EXPIRED
SUPERSEDED
```

只有 `DELIVERED_TO_USER` 成功后，业务生命周期才能进入 `WAITING_USER`。

---

# 11. K07 鉴别方向 / 证据 / Must-Exclude 契约

## 11.1 Disease Direction Candidate

```text
candidate_id
concept_id
display_name
tier
status

supporting_evidence_refs[]
opposing_evidence_refs[]
unknown_evidence_refs[]

knowledge_release_refs[]
rule_release_refs[]
capability_binding_ref

clinical_state_version
```

Tier：

```text
LIKELY
ACTIVE_ALTERNATIVE
MUST_EXCLUDE
```

生命周期：

```text
PROPOSED
ACTIVE
SUPPORTED
WEAKENED
EXCLUDED
INVALIDATED
```

---

## 11.2 Must-Exclude Assessment

```text
candidate_ref
status
required_evidence_refs[]
missing_evidence_refs[]
reason_codes[]
rule_release_refs[]
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

---

## 11.3 Evidence Relation

必须区分：

```text
patient evidence
clinical rule evidence
KG reasoning path
medical knowledge citation
model inference
```

关系：

```text
SUPPORTS
OPPOSES
UNKNOWN_FOR
```

医学知识引用必须能够反向追踪到：

```text
KnowledgeReleaseRef
+
source/provenance ref
```

KG path 不自动升级为 medical citation。

---

# 12. K08 线下证据 / 检查建议契约

Offline Evidence Assessment：

```text
assessment_id
clinical_state_version
status
needs[]
reason_codes[]
evidence_refs[]
knowledge_release_refs[]
rule_release_refs[]
```

状态：

```text
NOT_ASSESSED
ASSESSING
VALID
STALE
FAILED
```

Examination Suggestion：

```text
suggestion_id
examination_concept
rationale_ref

knowledge_release_refs[]
rule_release_refs[]

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

必须保持：

```text
Suggestion != Order
Suggestion != Prescription
```

---

# 13. K09 状态变更提案 / 提交契约

## 13.1 State Change Proposal

正式 Proposal 只能在 Business Owner / Resolver 完成业务解释后产生。

至少：

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

capability_binding_refs[]
knowledge_release_refs[]
rule_release_refs[]

idempotency_key
created_at
```

operations 至少支持：

```text
ADD
REPLACE
REMOVE
TEST / PRECONDITION
```

---

## 13.2 Proposal producer 边界

```text
Capability Result
→ Business Owner / Deterministic Resolver
→ Decision
→ State Change Proposal
→ G2
→ Commit Result
```

禁止：

```text
Capability Result
→ 直接产生正式 StateChangeProposal
```

---

## 13.3 Typed structured value

Proposal 必须支持：

```text
typed structured value
或
governed object/reference
```

禁止：

```text
复杂临床对象
→ JSON.stringify
→ 塞入 string value
```

---

## 13.4 Commit Result

保持：

```text
COMMITTED
REJECTED
CONFLICT
NO_OP
FAILED
```

至少返回：

```text
previous_version
new/committed_version
reason_code / reason_codes[]
audit_ref
conflict_detail
retryable
```

必须保持：

```text
NO_EFFECT
!= COMMIT_FAILED
!= REJECTED
```

---

## 13.5 Invalidation

D05 可形成：

```text
Risk → STALE
DDx → INVALIDATED
Workup → SUPERSEDED
Delivery → SUPERSEDED
```

但 D05 不成为这些状态的跨模块 Owner。

---

# 14. K10 交付 / Resume / Runtime / Trace 引用

## 14.1 Delivery Package

至少：

```text
delivery_id
delivery_type
clinical_state_version

risk_ref
safety_gate_ref
readiness_ref

ddx_refs[]
must_exclude_refs[]
evidence_refs[]
uncertainty_refs[]
offline_evidence_refs[]

capability_binding_refs[]
knowledge_release_refs[]
rule_release_refs[]

action_guidance
boundary_disclosures[]
rendered_content_ref
```

---

## 14.2 Business Resume Event Decision

业务处理生命周期：

```text
RECEIVED
VALIDATING
ACCEPTED
DUPLICATE
EXPIRED
REJECTED
APPLIED
```

必须保持：

```text
same event cannot produce duplicate clinical effects
```

---

## 14.3 Runtime Execution Resume Result

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

---

## 14.4 Trace / Audit Reference

Trace 至少可记录：

```text
unit_id
capability_binding_ref

knowledge_release_refs[]
rule_release_refs[]

prompt_release_refs[]
model_route_refs[]

run_id
input_ref
output_ref
decision_ref
state_proposal_ref
commit_result_ref
failure_ref

duration/status
```

默认不得复制完整 PHI payload。

---

# 15. 临床状态数据域

V1 Versioned Clinical State / CDP 至少承载或权威引用：

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

四个系统级状态必须进入 governed Clinical State 或其权威引用：

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
rule_release_ref / policy version
knowledge_release_ref（适用时）
validity / staleness
```

---

# 16. Runtime State 数据域

Runtime 独立保存：

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

不得复制第二份 Clinical Truth
```

---

# 17. U01–U15 契约映射

| Unit | 主要输入 | 主要输出 / 绑定 |
|---|---|---|
| U01 | K02 START_CONSULTATION + K01 | Subject/Problem semantics + Scope Decision + CapabilityBindingRef + 必要 K09 |
| U02 | K02 NEW_CLINICAL_INPUT + K03 | Governed facts/assertions + K09 Commit |
| U03 | K03 facts + RuleReleaseRef + KnowledgeReleaseRef | Risk Evidence + Risk Decision + K09 |
| U04 | Risk Decision + capability/policy refs | Safety Gate Decision + RuleReleaseRef + K09 |
| U05 | readiness inputs | Readiness Decision + K09 |
| U06 | Gap + CapabilityBindingRef + Question Policy | Question + Stopping Decision + wait transition |
| U07 | Resume Event + Runtime refs | Business Resume Decision + Runtime Resume Result |
| U08 | Facts + K07 context + Knowledge/Rule/Capability binding | DDx/Must-Exclude/Evidence + Owner interpretation |
| U09 | DDx + Gap | updated Gap + stopping/readiness decisions |
| U10 | readiness + K07 + Knowledge/Rule refs | K08 Offline Evidence / Exam Suggestion |
| U11 | Safe Exit reason + current refs | Safe Exit Delivery + Delivery Validation |
| U12 | normal delivery prerequisites | Normal Delivery + Delivery Validation |
| U13 | Correction Event | correction decision + invalidation decisions + K09 |
| U14 | Failure Contract + safety/runtime context | Failure Routing Decision |
| U15 | Cancel/Expire Event + Runtime refs | Cancel/Expire Decision + terminal effect |

所有正式 Clinical State 变化最终仍必须经过 K09 / G2。

---

# 18. 版本、激活与兼容性原则

以下均为独立维度：

```text
Contract Version
Clinical State Version
Capability Version
Capability Binding Status
Scope Version
Policy Version
Rule Release Version
Knowledge Release Version
Prompt Release Version
Model Route Version
```

禁止把它们混为一个 version。

至少遵循：

```text
1. Contract 不兼容变化必须显式升级版本

2. 同一 Consultation 已绑定的 Capability / Scope
   不得静默切换

3. Capability Version 变化
   不代表自动激活

4. Knowledge / Rule 新版本发布
   不得自动影响已经运行中的历史 Clinical Decision

5. 新运行使用新的正式绑定版本
   历史运行保留原绑定版本以便重放

6. 已撤回/过期知识
   不允许产生新的正式 Clinical Decision

7. 已废弃 Capability
   不允许新的 Consultation 继续绑定

8. 已退役 Capability
   只保留历史追踪

9. Runtime Resume
   必须校验 checkpoint 与当前 Clinical State / binding 的兼容性
```

---

# 19. 能力与知识回滚语义

## 19.1 Capability 回滚

```text
发现新能力版本存在安全/质量/兼容问题
↓
停止新绑定
↓
激活已批准回滚版本
↓
新的 Run 使用回滚版本
↓
历史 Run 保留原 capability binding
```

禁止直接覆写历史 binding。

---

## 19.2 Knowledge / Rule 回滚

```text
Knowledge/Rule Release 出现问题
↓
标记 WITHDRAWN / DEPRECATED / EXPIRED
↓
停止新的正式使用
↓
切回批准的 rollback target
↓
保留所有历史引用与审计
```

回滚不等于删除历史知识。

---

# 20. PHI 与最小上下文原则

```text
需要什么传什么
而不是完整 CDP 传给所有能力
```

每个能力输入后续 Spec 明确：

```text
required fields
optional fields
forbidden fields
sensitivity
purpose
retention/logging rule
```

Trace / Error / Audit 默认使用引用和摘要，不默认复制完整患者原文。

---

# 21. Phase 8 全局不变量

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

CONTRACT-INV-25 Capability Exists != Capability Active
CONTRACT-INV-26 Capability Eval PASS != Capability Authorized
CONTRACT-INV-27 Runtime 使用 Capability 前必须验证有效 CapabilityBindingRef
CONTRACT-INV-28 Knowledge Candidate != Published Knowledge
CONTRACT-INV-29 Withdrawn / Expired Knowledge 不得用于新的正式 Clinical Decision
CONTRACT-INV-30 Safety-critical Rule 必须绑定正式 RuleReleaseRef
CONTRACT-INV-31 Knowledge / Rule / Capability 回滚不得改写历史运行绑定
CONTRACT-INV-32 新版本发布不得静默改变已运行 Consultation 的历史语义
```

---

# 22. 与 Phase 9 Runtime 的边界

Phase 8 冻结“交换什么数据、哪些版本允许被使用”。

Phase 9 决定：

```text
CapabilityBindingRef 在 Runtime 中如何解析
KnowledgeReleaseRef 如何装载
RuleReleaseRef 如何装载
何时验证 effective time / scope / allowlist
如何做 runtime compatibility
如何缓存但不破坏版本语义
如何完成 rollback binding
Checkpoint 如何保存绑定引用
```

Phase 8 不提前冻结：

```text
具体服务
HTTP API
数据库表
缓存实现
微服务拓扑
```

---

# 23. 本次 Phase 7 → Phase 8 同步变更

本次不改变原 P8-R01 ～ P8-R08 已关闭结论。

新增同步项：

```text
P8-SYNC-01
补 CapabilityBindingRef
→ 承接 P06 能力范围、能力包、版本、激活、白名单与回滚

P8-SYNC-02
扩展 KnowledgeReleaseRef
→ 承接 P04 知识发布、生效期、来源、冲突、撤回、回滚

P8-SYNC-03
新增 RuleReleaseRef 语义
→ 安全关键规则必须正式版本化和发布

P8-SYNC-04
K04/K05/K07/K08/K10
→ 显式绑定 Capability / Knowledge / Rule refs

P8-SYNC-05
版本原则增加 activation / effective period / rollback

P8-SYNC-06
新增 CONTRACT-INV-25 ～ CONTRACT-INV-32
→ 封闭能力激活、知识发布、规则发布和回滚语义
```

---

# 24. Phase 8 完成定义

当前 Phase 8 应能够回答：

1. 各 Unit、能力、规则、G2、Runtime 交换什么数据？
2. 哪些只是候选，哪些是正式业务裁决？
3. 哪些对象可以形成 State Change Proposal？
4. Clinical State / Runtime State / Trace 如何分离？
5. 能力失败如何表达？
6. 如何绑定 Clinical State Version？
7. 如何绑定 Capability Version？
8. 如何判断一个能力是否真正处于可用状态？
9. 如何绑定 Knowledge Release？
10. 如何绑定安全关键 Rule Release？
11. Capability / Knowledge / Rule 如何过期、废弃、撤回与回滚？
12. 如何保证历史运行可重放，而新运行使用新的批准版本？

冻结范围继续保持：

```text
K01–K10
```

不新增 K11。

本文件不构成：

```text
实现授权
Merge Authorization
Production Authorization
```

最终语义状态：

```text
SOP Phase 8 — Contract & Data Design
= V1 synchronized with completed Phase 7 governance design
```
