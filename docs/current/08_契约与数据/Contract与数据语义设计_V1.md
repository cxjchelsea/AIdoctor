# AIdoctor V1 Contract 与数据语义设计

> 文档状态：DRAFT FOR FREEZE  
> 所属阶段：复杂业务软件开发 SOP — Phase 8 契约与数据设计  
> 上游权威：`01_需求/需求与系统边界_V1.md`、`02_功能/功能模块划分_V1.md`、`03_状态/系统级状态主干_V1.md`、`03_状态/模块级状态与状态所有权_V1.md`、`05_业务闭环/业务闭环设计_V1.md`、`06_开发单元/可验证开发单元拆分_V1.md`、`07_能力设计/按开发单元的Capability设计_V1.md`  
> 当前事实依据：`00_现状与治理/Current_State_Baseline_V1.md`、现有 `contracts/v1/`。  
> 本文冻结跨 Unit / Capability / Governance / Runtime 的业务数据语义与 Contract Family；不冻结 HTTP URL、RPC 方式、数据库表、语言类名、部署拓扑或具体序列化实现。

---

# 1. Phase 8 目标

Phase 7 已回答“每个 Unit 需要什么能力”。Phase 8 回答：

> 这些 Unit、Capability、Policy、State Governance 与 Runtime 之间，究竟交换什么数据？这些数据分别属于候选结果、正式临床状态、业务事件、运行时状态还是审计证据？

本阶段重点冻结：

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

StateChangeProposal
!= committed state

DeliveryValidationResult
!= Delivery Readiness

RiskEvidenceResult
!= Clinical Risk Disposition
```

最终业务真值仍按 Phase 4 Owner 规则形成。

## CT-02 候选结果、业务裁决、正式提交必须分层

统一模式：

```text
Input Context
↓
Capability Result / Policy Input
↓
Business Owner / Deterministic Resolver
↓
State Change Proposal
↓
G2 State Governance
↓
Commit Result
↓
New Clinical State Version
```

任何 Capability 不得直接把自己的输出等同于已提交状态。

## CT-03 Failure 必须是一等数据

禁止：

```text
null
[]
{}
```

承担“没有结果 / 服务失败 / 信息不足 / 不适用”的多重含义。

统一业务级 Failure/Result Status 至少支持：

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

具体技术错误码可在后续实现细化，但不得破坏这些业务语义。

## CT-04 所有临床结果必须绑定版本上下文

凡可能影响临床判断的结果至少关联：

```text
consultation_id
clinical_state_version
capability_id / capability_version（适用时）
scope_version
rule_version（适用时）
knowledge_release_version（适用时）
prompt/model version（使用模型时）
```

不能把基于 v3 Clinical State 产生的结果无条件提交到 v5。

## CT-05 Runtime Contract 与 Clinical Contract 分离

```text
Clinical State
= 患者事实、Risk、Gap、DDx、Workup、Delivery 等临床业务真值

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
= ADAPT 为更一般的 CapabilityResult 语义来源

StatePatch
= REUSE_FOUNDATION + REFACTOR PATH/SEMANTICS

CommitResult
= REUSE_FOUNDATION

PatientDeliveryView
= REFERENCE / ADAPT，不能反向定义 F7 业务真值
```

现有 `ToolResult` 已明确 `suggested_patches` 只是 proposal；现有 `StatePatch` 也明确自己不是 committed state。该核心语义保留。

但现有 StatePatch path 仍主要围绕旧 CDP 字段，如 `patient_state/ddx/evidence_graph/workup_plan/management_plan/...`，其中 `management_plan`、`wellness_plan` 与当前 V1 边界并不完全一致，因此不能直接把现有 path allowlist 当成 Phase 8 当前业务真源。

---

# 4. V1 Contract Family 总览

Phase 8 将跨组件数据分为 10 个 Contract Family：

```text
K01 Identity & Version Context
K02 Business Event
K03 Clinical Observation / Fact
K04 Capability Result
K05 Risk / Safety Decision Input
K06 Gap / Question
K07 DDx / Evidence / Must-Exclude
K08 Offline Evidence / Workup
K09 State Change Proposal / Commit
K10 Delivery / Runtime / Trace References
```

这些是语义族，不要求最终实现一定只有 10 个 JSON Schema。

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
→ Validation
→ Accepted / Duplicate / Expired / Rejected
→ 才可能产生状态效果
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

## 7.2 Observation lifecycle 与 Fact 分离

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

## 7.3 Source

至少表达：

```text
PATIENT_REPORTED
EXTERNAL_MEASUREMENT
OCR_EXTRACTED
MODEL_INFERRED
RULE_DERIVED
CLINICIAN_CONFIRMED
```

Source Attribution 必须保真，模型推断不能冒充患者陈述。

---

# 8. K04 Capability Result Contract

所有概率性或外部 Capability 统一遵循类似结果封装：

```text
result_id
capability_id
capability_version
input_clinical_state_version
status
reason_code
retryable
structured_output
state_change_proposals[]
evidence_refs[]
version_refs
errors[]
started_at
completed_at
```

其中：

```text
structured_output
= Capability 候选/证据/建议

state_change_proposals
= 建议改变哪些正式状态
```

两者都不是 committed state。

现有 `ToolResult` 可作为该模式的工程基础，但当前 Phase 8 不要求所有能力继续使用“Tool”命名。

---

# 9. K05 Risk / Safety Contract

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

## 9.2 D09 输出：Risk Disposition Decision

由 F4 / D09 形成：

```text
clinical_state_version
risk_disposition
basis_refs[]
rule_version
reason_codes[]
```

其中：

```text
NOT_EVALUATED
NO_HIGH_RISK_SIGNAL
CAUTION
HIGH_RISK
```

## 9.3 D02 输出：Safety Gate Decision

```text
clinical_state_version
safety_gate
risk_decision_ref
capability_availability_refs[]
scope/authorization/consent refs
reason_codes[]
policy_version
```

结果：

```text
ALLOW
RESTRICTED
BLOCKED
UNAVAILABLE
```

Risk 与 Safety Gate 必须是两个不同 Contract。

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
source_requirement_ref  # F1 clarification 或 F3 gap
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

至少表达：

```text
proposal_id
consultation_id
base_clinical_state_version
producer
business_owner
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

## 13.2 Proposal 限制

```text
Capability
→ 可以提出 Proposal

Business Owner / Resolver
→ 可以解释 Proposal 是否符合业务语义

G2
→ 最终验证授权、版本、来源、字段权限、幂等、并发、证据要求
```

任何 Proposal 都不能宣称自己已经 committed。

## 13.3 Commit Result

至少区分：

```text
COMMITTED
REJECTED
CONFLICT
DUPLICATE_NO_EFFECT
```

并返回：

```text
previous_version
new_version（若成功）
reason_codes[]
audit_ref
conflict_detail（适用时）
```

Capability success + Commit reject = Unit 未成功。

## 13.4 Invalidation

D05 只负责计算 dependency invalidation requirement，例如：

```text
Risk → STALE
DDx → INVALIDATED
Workup → SUPERSEDED
Delivery → SUPERSEDED
```

最终仍转换为受 G2 管理的 State Change Proposal / Commit。

---

# 14. K10 Delivery / Runtime / Trace References

## 14.1 Delivery Package Contract

结构化 Delivery Package 在渲染前形成，至少关联：

```text
delivery_id
delivery_type  # NORMAL / SAFE_EXIT
clinical_state_version
risk_ref
ddx_refs[]
must_exclude_refs[]
evidence_refs[]
uncertainty_refs[]
offline_evidence_refs[]
action_guidance
boundary_disclosures[]
rendered_content_ref
```

Delivery Validator 输出独立：

```text
validation_status
violations[]
validated_clinical_state_version
policy_versions[]
```

其结果不等于 `Delivery Readiness`；F7 根据验证结果形成正式 Delivery Readiness。

## 14.2 Runtime Resume Contract

Runtime 只处理执行身份与恢复：

```text
thread_id
run_id
checkpoint_id
pending_event_id
pending_question_id
referenced_clinical_state_version
resume_event_id
resume_status
expires_at
```

Resume status 至少表达：

```text
ACCEPTED
DUPLICATE
EXPIRED
REJECTED
APPLIED
```

Checkpoint 不保存/替代 Clinical Truth，只引用相应 Clinical State Version。

## 14.3 Trace / Audit Reference

Trace 可以记录：

```text
unit_id
capability_id
run_id
input_ref
output_ref
state_proposal_ref
commit_result_ref
failure_ref
duration/status
```

默认不得为了“可观察性”复制完整 PHI payload。

---

# 15. Clinical State 数据域

V1 正式 Versioned Clinical State / CDP 至少需要承载以下业务域语义：

```text
subject_context
problem_framing
clinical_observations / patient_facts
information_gaps
questions / pending question refs
clinical_risk
red_flags
clinical_readiness
ddx_candidates
must_exclude
evidence_relations
offline_evidence_needs
examination_suggestions
delivery_package / delivery_readiness refs
uncertainty
version metadata
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

---

# 17. U01–U15 Contract 映射

| Unit | 主要输入 Contract | 主要输出 Contract |
|---|---|---|
| U01 | K02 START_CONSULTATION + K01 Context | Subject/Problem semantic result + Scope Decision + State Proposal |
| U02 | K02 NEW_CLINICAL_INPUT + K03 candidates | K03 governed facts + K09 Commit Result |
| U03 | K03 current facts | K05 Risk Evidence + Risk Disposition proposal |
| U04 | K05 Risk Decision + capability/policy refs | K05 Safety Gate Decision |
| U05 | F1/F3/F5/F6 readiness inputs | Clinical Readiness Decision + K09 proposal |
| U06 | Clarification/Gap Contract | K06 Question + wait transition proposal |
| U07 | K02 Resume Event + Runtime refs | Resume Decision + event effect/no-effect |
| U08 | K03 facts + K07 evidence context | K07 DDx/Must-Exclude result + proposals |
| U09 | K07 DDx + K06 gaps | updated K06 Gap/Stopping inputs |
| U10 | readiness + K07 refs | K08 Offline Evidence/Exam Suggestion |
| U11 | Safe Exit reason + current state refs | K10 Safe Exit Delivery + Validation Result |
| U12 | normal delivery prerequisites | K10 Normal Delivery + Validation Result |
| U13 | K02 Correction Event | accepted/rejected correction + invalidation proposals |
| U14 | Failure Contract + current safety/runtime context | retry/fallback/safe-exit/terminal routing decision |
| U15 | Cancel/Expire Event + Runtime refs | lifecycle terminal effect + late-event rejection state |

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

至少需要遵循：

- Contract 不兼容变化需要显式版本升级；
- 同一 Consultation 运行中绑定的 Scope/Capability 版本不得静默切换；
- 派生结果必须记录其依赖版本；
- 新事实提交导致 Clinical State Version 变化时，旧派生结果必须可以识别为 stale/invalid；
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
CONTRACT-INV-02 StateChangeProposal != CommitResult
CONTRACT-INV-03 Clinical State != Runtime Checkpoint
CONTRACT-INV-04 Trace != Clinical Truth
CONTRACT-INV-05 Failure 不能通过 null/[]/{} 隐式表达
CONTRACT-INV-06 UNKNOWN != NO；UNMEASURED != NORMAL
CONTRACT-INV-07 Risk Evidence != Clinical Risk Disposition
CONTRACT-INV-08 Clinical Risk Disposition != Safety Gate
CONTRACT-INV-09 DeliveryValidationResult != Delivery Readiness
CONTRACT-INV-10 Scope semantic extraction != OUT_OF_SCOPE final decision
CONTRACT-INV-11 所有正式 Clinical State 写入必须经 G2/K09
CONTRACT-INV-12 所有临床派生结果必须可追溯到输入 Clinical State Version
CONTRACT-INV-13 Duplicate Event 不得产生第二次临床效果
CONTRACT-INV-14 Checkpoint 只能引用而不能替代 Clinical State
CONTRACT-INV-15 Patient/Rule/KG/Citation/Model evidence provenance 不得互相冒充
CONTRACT-INV-16 Suggestion != Medical Order / Prescription
CONTRACT-INV-17 Frontend View Contract 不得制造新的临床语义
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
```

因此 Phase 8 不提前把 Contract Family 映射成微服务或 HTTP API。

---

# 22. Phase 8 完成标准

Phase 8 应能够回答：

> 当 U01–U15 开始真正实现时，各 Unit、Capability、Resolver、State Governance 与 Runtime 之间传递的数据分别是什么语义，哪些只是候选，哪些能够成为正式状态，版本和 Failure 如何表达？

当前第一版已经形成：

- 10 个 Contract Family；
- Clinical State / Runtime State / Trace 三层数据边界；
- Observation / Risk / Gap / DDx / Workup / Delivery / Resume 核心业务语义；
- State Change Proposal → G2 → Commit 的统一链；
- U01–U15 Contract 映射；
- Failure、Version、Evidence Provenance 与 PHI 基础规则；
- 对现有 `contracts/v1/` 的 REUSE/ADAPT/REFACTOR 判断。

当前状态：

```text
SOP Phase 8 — Contract & Data Design
= DRAFT COMPLETE / NOT FROZEN
```

冻结前应独立审查：

1. 10 个 Contract Family 是否覆盖 U01–U15；
2. Candidate / Decision / Proposal / Commit 是否有任何语义混淆；
3. Clinical State / Runtime / Trace 是否仍存在双真源；
4. Failure status 是否足够表达 Phase 7 语义；
5. Risk / Scope / Delivery / Invalidation Owner 是否与 Phase 4/7 一致；
6. 现有 ToolResult / StatePatch / CommitResult 复用判断是否合理；
7. 当前 Clinical State 域是否漏掉 V1 必要核心数据；
8. 是否错误把 Treatment/Wellness/检查执行闭环带回 V1；
9. Version binding 是否足以支撑 stale/invalidation/resume；
10. 是否提前越界到 Phase 9 的 API/Runtime/服务拓扑。

通过独立审查后，Phase 8 才可标记 `FROZEN / V1`。
