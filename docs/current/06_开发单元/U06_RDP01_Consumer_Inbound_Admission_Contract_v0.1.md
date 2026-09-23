# U06-RDP-01 Consumer Inbound / Admission Contract v0.1

> Unit: U06  
> Readiness blocker: `BF-U06-RG-01`  
> Design basis: approved U06 Unit Spec + U06 Initial Implementation Readiness / Gap Review  
> Parent readiness head: `aac25a0f7fa597b4a2c6ebf2201e606a8c58857e`  
> Runtime repository basis: `main@7b37c03026cb17e89e3d7769df2b1bb1f03a9ca8`  
> Scope: **CONSUMER CONTRACT / ADMISSION DESIGN ONLY**  
> Status: **DRAFT_FOR_INDEPENDENT_DESIGN_REVIEW**  
> This contract grants no implementation, upstream wiring, Scheduler activation, C03/D04 execution, delivery, merge, production, or real-patient authorization.

---

# 1. Purpose

U06 有三个受治理 Mode，且每个 Mode 的合法来源、Safety/currentness、owner provenance 与副作用边界不同。

RDP-01 的职责是回答：

```
谁可以请求 U06？
以哪个 Mode 请求？
请求必须携带哪些 authoritative refs？
U06 如何独立证明这些 refs 仍 current / lawful？
哪些 source + mode 组合必须拒绝？
retry/replay 如何避免变成第二次业务 effect？
在 admission 之前哪些动作绝对不能发生？
```

RDP-01 不回答：
- C03/D04 的具体业务政策；
- F3/Question StatePatch；
- delivery 物理事务；
- P06 release applicability 的最终细节；
- Verification evidence matrix。

这些分别属于 RDP-02/03/04/05/06。

---

# 2. Core admission invariant

任何 U06 业务动作之前必须先形成：

```
U06AdmissionResult = ADMITTED
```

在此之前：

```
C03 invocation = 0
D04 invocation = 0
StateChangeProposal = 0
P01 commit = 0
Question selection = 0
delivery intent = 0
external send = 0
WAITING_USER mutation = 0
Runtime AWAITING_USER transition = 0
```

禁止：

```
caller selected mode
→ therefore mode is trusted

caller says state is current
→ therefore currentness is trusted

caller supplies target_unit = U06
→ therefore source authority is trusted

route/eligibility exists
→ therefore U06 business execution is authorized
```

U06 Admission Host 必须独立重验证 authoritative state 与 source authority。

---

# 3. Canonical U06 inbound request

定义语义对象：

```
U06ConsumerInboundRequest
```

最小字段：

```
request_id

consultation_id
cdp_id

u06_mode

claimed_clinical_state_version
authoritative_state_ref

source_authority_type
source_authority_ref
source_authority_version?
source_consequence?
source_context

u04_gate_ref?
u04_gate_commit_ref?
gate_value?

route_authorization_type
route_authorization_ref
routing_policy_version

restricted_context_ref?
restricted_permission_ref?

f1_clarification_requirement_ref?
source_readiness_record_ref?
source_readiness_effect_id?
u05_routing_decision_ref?
u05_downstream_eligibility_ref?

f3_canonical_effect_id?
f3_source_state_ref?
f3_revalidation_decision_ref?
source_clinical_continuation_routing_ref?

bootstrap_architecture_binding_ref?

capability_binding_ref?
question_policy_ref?
rule_release_ref?
knowledge_release_ref?
prompt_release_ref?
model_route_ref?

canonical_event_ref
business_event_identity
thread_id?
run_id?
correlation_id
trace_id

admission_contract_version
created_at
```

注意：

```
request_id
!= admission_id
!= business effect identity
!= F3_CANONICAL_EFFECT_ID
!= Question effect identity
!= delivery_id
```

request 只是调用 envelope。

---

# 4. Frozen U06 mode vocabulary

只允许：

```
PRE_READINESS_GAP_ASSESSMENT
QUESTION_SELECTION_DELIVERY
F3_CURRENT_VERSION_REVALIDATION
```

简称：

```
MODE-1
MODE-2
MODE-3
```

禁止：
- null/unknown mode；
- 从 nullable field 推断 mode；
- admission 后 Scheduler/adapter 改 mode；
- 一个 request 同时声明两个 mode；
- MODE-1 request 在执行中切换为 MODE-2；
- MODE-3 因 binding 不兼容自动变成 MODE-1，而不形成新的 admission。

---

# 5. Source authority vocabulary

RDP-01 冻结 source-neutral admission authority categories：

```
A1_PRE_READINESS_ROUTING
F1_CLARIFICATION_ROUTING
U05_QUESTION_ROUTING
POST_F3_SAFETY_BARRIER_ROUTING
CLINICAL_CONTINUATION_ROUTING
F3_REASSESSMENT_ROUTING
```

这些是：

```
U06 admission authority categories
```

不是：
- Clinical Truth；
- C03 result；
- F3 state；
- Question；
- Scheduler preference。

每个 source category 必须映射到唯一合法 Mode 集合。

---

# 6. Mode/source business-legality matrix

本矩阵回答：

```
这个 source 在业务语义上是否允许请求这个 U06 mode？
```

它不等于当前 producer/activation 已可执行。

| Source authority | MODE-1 | MODE-2 | MODE-3 |
|---|---:|---:|---:|
| A1_PRE_READINESS_ROUTING | BUSINESS_LEGAL | DENY | DENY |
| F1_CLARIFICATION_ROUTING | DENY | BUSINESS_LEGAL | DENY |
| U05_QUESTION_ROUTING | DENY | BUSINESS_LEGAL | DENY |
| POST_F3_SAFETY_BARRIER_ROUTING | DENY | DENY | BUSINESS_LEGAL |
| CLINICAL_CONTINUATION_ROUTING | DENY | DENY | BUSINESS_LEGAL |
| F3_REASSESSMENT_ROUTING | BUSINESS_LEGAL | DENY | DENY |

任何不在矩阵中的组合：

```
U06 admission = REJECTED_MODE_SOURCE_MISMATCH
```

Scheduler 不得把 source consequence 重标成另一个 Mode。

## 6.1 Current activation state

必须另行区分：

```
BUSINESS_LEGAL
!= ADMISSION_ACTIVATED
!= SOURCE_PRODUCER_IMPLEMENTED
```

当前设计基线：

| Source authority | Consumer contract | Current activation / producer state |
|---|---|---|
| U05_QUESTION_ROUTING | DEFINED | producer AVAILABLE in current U05 |
| A1_PRE_READINESS_ROUTING | DEFINED | producer NOT_IMPLEMENTED |
| POST_F3_SAFETY_BARRIER_ROUTING | DEFINED | producer NOT_IMPLEMENTED |
| CLINICAL_CONTINUATION_ROUTING | DEFINED | producer NOT_IMPLEMENTED |
| F3_REASSESSMENT_ROUTING | DEFINED | projection producer NOT_IMPLEMENTED |
| F1_CLARIFICATION_ROUTING | DEFINED | **ACTIVATION_BLOCKED_PENDING_CONTROLLED_AMENDMENT** |

其中 direct F1 特别要求：

```
frozen business path exists
but exact runtime safety/routing authority is not yet refrozen
```

因此当前任何 implementation authorization 若未先关闭该 controlled amendment：

```
F1_CLARIFICATION_ROUTING
= MUST_REMAIN_DISABLED
```

Synthetic authoritative fixture 可以验证 consumer schema/currentness/fail-closed 行为，但：

```
synthetic fixture
!= activation
!= upstream implementation
!= authority to deliver a patient-facing clarification
```

---

# 7. MODE-1 admission — A1 pre-readiness source

## 7.1 Legal source

初始 A1 bootstrap：

```
source_authority_type
= A1_PRE_READINESS_ROUTING

source_consequence
= PRE_READINESS_A1_F3_C03_ELIGIBLE

u06_mode
= PRE_READINESS_GAP_ASSESSMENT
```

必须证明：

```
BootstrapArchitectureBindingRef = A1
current committed U04 Gate = ALLOW
  or RESTRICTED with explicit PRE_READINESS_F3 permission

A1 canonical F3 bootstrap not already current
current authoritative Clinical State Version
current F1 framing/fact basis
current routing authorization targets U06 MODE-1
```

## 7.2 Required refs

至少：

```
source_authority_ref
u04_gate_ref
u04_gate_commit_ref
route_authorization_ref
bootstrap_architecture_binding_ref = A1
canonical_event_ref / business_event_identity
correlation_id
trace_id

capability_binding_ref
```

Rule/Knowledge/Prompt/Model refs 是否必需由 RDP-05 按实际 C03 binding 冻结。

RDP-01 只允许：
- ref absent because RDP-05 says NOT_APPLICABLE；
- ref present and later validated。

不允许 placeholder/fake ref。

## 7.3 Currentness

Admission Host 必须证明：
- source authority 仍 CURRENT；
- Gate 仍 committed/current；
- route authorization 尚未 superseded/revoked；
- Clinical State dependency basis 未发生使 pre-readiness eligibility 失效的变化；
- 同一 current basis 下不存在已 current 的 canonical A1 F3 completion。

若 canonical F3 已 current：

```
MODE-1 admission = REJECTED_ALREADY_SATISFIED
```

不得再次 C03 assessment。

---

# 8. MODE-1 admission — re-assessment source

MODE-3 可以产生：

```
REASSESSMENT_REQUIRED
```

冻结语义要求：

```
fresh U06 MODE-1
+ current bindings
```

因此 RDP-01 允许第二种 MODE-1 source-neutral projection：

```
source_authority_type
= F3_REASSESSMENT_ROUTING

source_consequence
= REASSESSMENT_REQUIRED

u06_mode
= PRE_READINESS_GAP_ASSESSMENT
```

必须保持：

```
F3_REASSESSMENT_ROUTING
!= new F3 business decision
!= new revalidation outcome
!= new Clinical Truth

business authority
= prior authoritative F3CurrentVersionRevalidationDecision
  with outcome REASSESSMENT_REQUIRED
+ current Safety/routing authority
```

该 projection 只回答：

```
the already-authoritative REASSESSMENT_REQUIRED consequence
may now be considered for this exact fresh MODE-1 execution
```

它不得：
- 从 `REVALIDATED_CURRENT` 改成 reassessment；
- 从 `FAILED` 改成 reassessment；
- 在不存在 authoritative revalidation decision 时创造 reassessment；
- 改写 prior F3 effect/history。

该 source 必须绑定：

```
prior F3CurrentVersionRevalidationDecision identity
prior F3CurrentVersionRevalidationDecision outcome
= REASSESSMENT_REQUIRED

prior f3_canonical_effect_id

current authoritative Clinical State Version

current U04 Gate
current route authorization

revalidation/reassessment context identity
```

它不得仅凭：
- old F3 stale；
- C03 available；
- caller wants reassessment；

直接启动 MODE-1。

## 8.1 Reassessment projection identity

定义稳定：

```
F3_REASSESSMENT_ROUTING_ID
```

至少绑定：

```
consultation_id
cdp_id
source F3 revalidation decision id
source F3_CANONICAL_EFFECT_ID
source revalidation context
target authoritative Clinical State dependency identity
current U04 Gate / safety authority
current route authorization
reassessment projection policy version
```

因此：

```
same authoritative REASSESSMENT_REQUIRED decision
+ same context/current basis
→ same reassessment routing identity

different context
or different revalidation decision
or different Gate/routing basis
→ different projection identity
```

该 identity 只保护 execution/admission projection replay，不拥有 F3 truth。

## 8.2 Context preservation

F3_REASSESSMENT_ROUTING 必须保留原 evaluation/continuation context，例如：

```
A1_POST_BARRIER_CURRENT
POST_USER_FACT_UPDATE
POST_DDX_REEVALUATION
POST_OFFLINE_ASSESSMENT
```

防止：

```
POST_DDX replay
→ attach POST_OFFLINE reassessment
```

RDP-01 不定义 reassessment 后最终去哪；后续 routing owner 仍按其 context 处理。

---

# 9. MODE-2 admission — U05 question route

普通 Clinical Readiness question path：

```
source_authority_type
= U05_QUESTION_ROUTING

u06_mode
= QUESTION_SELECTION_DELIVERY
```

合法 source consequence 仅：

```
TO_U06_QUESTION_PATH
```

且 authoritative readiness 必须是：

```
NEEDS_CLARIFICATION
or
CAN_ASK_MORE
```

需要绑定：

```
U05DownstreamEligibility
u05_routing_decision_ref
source_readiness_record_ref
source_readiness_effect_id
current_u04_gate_ref
downstream permission ref when RESTRICTED
current Clinical State Version
routing_policy_version
```

必须验证：

```
target_unit_id = U06
downstream_consequence = TO_U06_QUESTION_PATH
eligibility.validity = CURRENT
routing_status = ELIGIBLE
```

禁止：

```
U05 READY_FOR_CLINICAL_ANALYSIS route
→ relabel as U06

U05 route stale
→ replace only state version and reuse

U05 eligibility
→ treated as U06 business execution result
```

## 9.1 NEEDS_CLARIFICATION vs CAN_ASK_MORE

RDP-01 只冻结 admission provenance，不把二者合并成同一 business need。

Admitted input 必须保存：

```
source_readiness_value
```

供 RDP-02 区分：
- F1 clarification；
- F3 question path。

U06 不可因为都到 MODE-2 就丢失 source semantics。

---

# 10. MODE-2 admission — direct F1 clarification

Phase 5 BL-01 已冻结：

```
F1 Problem Framing = CLARIFICATION_REQUIRED
→ F3 creates minimal clarification question
→ WAITING_USER
```

因此 direct F1 clarification 是合法业务 source，不能被 RDP-01 删除。

但当前仓库没有可执行的 authoritative F1→U06 routing projection。

RDP-01 冻结一个**需要上游 controlled amendment 实现的投影合同**：

```
F1ClarificationRoutingEligibility
```

其业务 owner 仍是 F1/U01 对：

```
CLARIFICATION_REQUIRED
```

的权威判断；投影本身不是新 Clinical Truth。

最小语义：

```
eligibility_id

consultation_id
cdp_id

source_f1_state_ref
source_f1_decision_ref
clarification_requirement_ref

source_clinical_state_version

clarification_scope = MINIMAL_ENTRY_CLARIFICATION

safety_clearance_type
safety_clearance_ref

route_authorization_ref

target_unit_id = U06
target_mode = QUESTION_SELECTION_DELIVERY

canonical_event_ref
business_event_identity
correlation_id
trace_id

policy_version
validity
created_at
```

## 10.1 Safety requirement

BL-01 同时冻结：

```
若入口已发现可能危险信号
→ 不得因为 framing 尚未完成而延迟风险处置
```

因此 direct F1 eligibility 必须包含一个**受治理的 safety clearance/ref**。

RDP-01 不擅自规定该 clearance 必须由：
- full U03/U04；
- entry-safety precheck；
- existing frozen Safety projection；

哪一种物理实现产生。

这属于：

```
UPSTREAM CONTROLLED AMENDMENT REQUIRED
```

在该 amendment 被设计/审查/refreeze 前：

```
direct F1 source
= CONTRACTUALLY_DEFINED
= BUSINESS_LEGAL
= ACTIVATION_BLOCKED_PENDING_CONTROLLED_AMENDMENT
```

任何 U06 implementation authorization package 若 direct-F1 amendment 尚未关闭，必须显式配置：

```
F1_CLARIFICATION_ROUTING = DISABLED
```

不得把“consumer contract 已定义”解释成“direct F1 已可运行”。

非生产 U06 admission verification 可使用 synthetic authoritative fixture 验证 consumer behavior，但：

```
synthetic fixture
!= U01/F1 upstream implementation complete
```

## 10.2 Direct F1 prohibitions

禁止：

```
raw F1 state = CLARIFICATION_REQUIRED
→ directly invoke U06

frontend says clarification needed
→ invoke U06

model output says ask follow-up
→ invoke U06

missing field
→ invoke U06
```

必须经过：

```
F1ClarificationRoutingEligibility
or future explicitly equivalent refrozen authority
```

---

# 11. MODE-3 admission — post-F3 Safety barrier

A1 canonical F3 commit 后：

```
F3 commit
→ RISK_REEVALUATION_REQUIRED
→ U03
→ U04 current Gate
→ U06 MODE-3
```

合法 source：

```
source_authority_type
= POST_F3_SAFETY_BARRIER_ROUTING

u06_mode
= F3_CURRENT_VERSION_REVALIDATION
```

必须绑定：

```
f3_canonical_effect_id
f3_source_state_ref
source F3 decision/effect provenance

source F3 Clinical State Version
target authoritative Clinical State Version

current post-barrier U04 Gate
current routing_authorization_ref

Risk reevaluation / Safety evaluation basis refs
dependency fingerprint before/current

historical capability_binding_ref
historical release refs as applicable

bootstrap_architecture_binding_ref = A1
```

只有：

```
Gate = ALLOW
or RESTRICTED with explicit F3_REVALIDATION permission
```

可 admission。

BLOCKED/UNAVAILABLE：

```
no MODE-3 admission
```

---

# 12. MODE-3 admission — Clinical Continuation source

冻结 continuation contexts：

```
POST_USER_FACT_UPDATE
POST_DDX_REEVALUATION
POST_OFFLINE_ASSESSMENT
```

当 current:

```
ClinicalContinuationRoutingDecision
→ TO_F3_CURRENT_VERSION_REVALIDATION
```

时：

```
source_authority_type
= CLINICAL_CONTINUATION_ROUTING

u06_mode
= F3_CURRENT_VERSION_REVALIDATION
```

必须绑定：

```
clinical_continuation_routing_id/ref
evaluation_context

accepted current mutation/invalidation provenance
prior F3 activation/effect ref

f3_canonical_effect_id
source/target Clinical State Version

current U04 Gate
current route authorization

dependency fingerprint
historical binding/release refs
```

禁止：

```
TO_U05_CLINICAL_READINESS
TO_U08_REASSESSMENT
TO_F6_CURRENT_VERSION_REASSESSMENT
TO_U12_DELIVERY_PREPARATION
FAILURE_ROUTE
```

被调用方重标为 U06 MODE-3。

---

# 13. Authoritative current-state binding

调用方声明：

```
claimed_clinical_state_version
```

不是 authority。

Admission Host 必须独立加载：

```
authoritative current Consultation/CDP/Clinical State
```

并至少验证：

```
consultation_id exact match
cdp_id exact match
claimed version compatible with source-specific currentness rule
source authority still valid/current
u04_gate_ref still valid/current when required
route authorization still routable
restricted permission still valid when required
```

禁止：

```
old source ref
+ new state version
→ silent rebind

old Gate
+ new routing ref
→ assume compatible

version changed
→ automatically stale
```

因为冻结规则是：

```
dependency validity
!= raw version equality
```

但任何跨版本继续使用必须有 source-specific dependency-validity/revalidation proof。

---

# 14. U04 Gate / Safety admission

## 14.1 Gate-required paths

以下 source 必须有 current committed U04 Gate：

```
A1_PRE_READINESS_ROUTING
U05_QUESTION_ROUTING
POST_F3_SAFETY_BARRIER_ROUTING
CLINICAL_CONTINUATION_ROUTING
F3_REASSESSMENT_ROUTING
```

只接受：

```
ALLOW
or action-permitted RESTRICTED
```

## 14.2 Direct F1 exception is not Safety bypass

```
F1_CLARIFICATION_ROUTING
```

可以发生在 BL-01 主流程尚未进入普通 U03/U04 链之前，因此 RDP-01 不强制其携带完整 U04 Gate。

但它必须携带：

```
safety_clearance_type
safety_clearance_ref
```

并且该 safety clearance 必须由单独 controlled amendment 冻结为足以允许：

```
MINIMAL_ENTRY_CLARIFICATION
```

的 authority。

因此：

```
no U04 Gate
!= no Safety proof
```

## 14.3 RESTRICTED

当 current Gate = RESTRICTED 时：

```
restricted_context_ref required
restricted_permission_ref required
```

permission 必须是 **action-specific**：

MODE-1:
```
PRE_READINESS_F3_ASSESSMENT
```

MODE-2:
```
QUESTION_SELECTION_DELIVERY
```

MODE-3:
```
F3_CURRENT_VERSION_REVALIDATION
```

禁止：

```
permission to evaluate U05
→ reused as permission to ask user

permission to perform MODE-1
→ reused for MODE-2 delivery

permission to MODE-3
→ reused for fresh C03 assessment
```

---

# 15. Capability/binding fields at admission

RDP-01 冻结 presence/currentness responsibility boundary，不替代 RDP-05。

## 15.1 MODE-1

必须有：

```
capability_binding_ref
```

用于 C03。

release/prompt/model refs：
- 由 RDP-05 判定 applicability；
- required when applicable；
- absent when typed NOT_APPLICABLE；
- never placeholder。

## 15.2 MODE-2

必须有：

```
capability_binding_ref
question_policy_ref
```

D04 policy ref/version 的 exact contract 由 RDP-02/RDP-05 冻结。

## 15.3 MODE-3

默认不 invoke C03，因此：

```
current C03 invocation binding
= NOT_REQUIRED_BY_DEFAULT
```

但必须保存/加载：

```
historical governing capability_binding_ref
historical release refs as applicable
semantic binding compatibility evidence
```

供 deterministic revalidation。

RDP-01 不得因为 MODE-3 request 没有“当前 C03 invocation binding”而错误拒绝。

---

# 16. Source-specific mandatory field matrix

Legend：

```
R = required
C = conditional
- = prohibited/not applicable
```

| Field family | MODE-1 A1 | MODE-1 reassess | MODE-2 U05 | MODE-2 F1 | MODE-3 barrier | MODE-3 continuation |
|---|---:|---:|---:|---:|---:|---:|
| current state identity | R | R | R | R | R | R |
| source authority ref | R | R | R | R | R | R |
| current U04 Gate | R | R | R | - | R | R |
| route authorization | R | R | R | R | R | R |
| restricted context/permission | C | C | C | C | C | C |
| U05 eligibility/readiness provenance | - | - | R | - | - | - |
| F1 clarification requirement | - | - | C | R | - | - |
| canonical F3 effect | - | R | C | - | R | R |
| prior F3 revalidation decision | - | R | - | - | - | C |
| continuation routing ref | - | C | - | - | - | R |
| C03 current binding | R | R | R | R | - | - |
| question policy ref | - | - | R | R | - | - |
| historical F3 binding refs | - | C | - | - | R | R |
| A1 bootstrap binding | R | C | - | - | R | C |
| safety clearance ref | via Gate | via Gate | via Gate | R | via Gate | via Gate |

Notes：
- MODE-2 U05 + NEEDS_CLARIFICATION may bind an F1 requirement ref if U05 readiness provenance exposes it；RDP-02/RDP-05 define exact requirement.
- MODE-1 reassess context may originate A1 or continuation/post-analysis.
- MODE-3 continuation may carry prior revalidation ref only if replay/re-entry requires it.

---

# 17. Canonical normalized admitted input

成功 admission 形成：

```
U06AdmittedInput
```

它是 immutable normalized snapshot，最小包含：

```
admission_id
admission_fingerprint
replay_disposition

consultation_id
cdp_id
authoritative_clinical_state_version
authoritative_state_ref

u06_mode

accepted_source_authority_type
accepted_source_authority_ref
accepted_source_consequence
accepted_source_context

accepted_u04_gate_ref?
accepted_gate_value?

accepted_route_authorization_type
accepted_route_authorization_ref

accepted_restricted_context_ref?
accepted_restricted_permission_ref?

accepted_f1_clarification_requirement_ref?
accepted_u05_readiness_ref?
accepted_u05_eligibility_ref?

accepted_f3_canonical_effect_id?
accepted_f3_state_ref?
accepted_f3_revalidation_ref?
accepted_continuation_routing_ref?

accepted_capability_binding_ref?
accepted_question_policy_ref?
accepted_rule_release_ref?
accepted_knowledge_release_ref?
accepted_prompt_release_ref?
accepted_model_route_ref?

canonical_event_ref
business_event_identity
thread_id?
run_id?
correlation_id
trace_id

admission_contract_version
admitted_at
```

后续 RDP-02..05 只能消费 accepted refs，不重新信任 caller raw request。

---

# 18. Admission identity / canonical fingerprint

定义：

```
U06_ADMISSION_ID
```

至少绑定：

```
consultation_id
cdp_id
u06_mode

accepted source authority type/ref
accepted source consequence/context

authoritative Clinical State dependency identity
current Gate / safety-clearance identity
route authorization identity

mode-specific business basis:
  F1 clarification requirement
  or U05 readiness/eligibility
  or F3 canonical effect/revalidation/continuation refs

admission contract version
```

它不包含：
- request_id；
- retry count；
- attempt timestamp；
- run_id/thread_id；
- tracing span；
- transport message ID。

另定义：

```
U06_ADMISSION_CANONICAL_FINGERPRINT
```

覆盖所有影响 admission 语义的 normalized accepted fields。

规则：

```
same admission_id
+ same fingerprint
→ exact replay candidate

same admission_id
+ different fingerprint
→ U06_ADMISSION_REPLAY_CONFLICT
```

---

# 19. Replay disposition

只允许：

```
ORIGINAL
REATTACHED
```

exact replay：

```
reload authoritative state
→ revalidate source currentness
→ reconcile admission ledger
→ attach prior U06AdmittedInput
→ replay_disposition = REATTACHED
```

Replay 不允许跳过：
- current Safety；
- source validity；
- permission validity；
- binding applicability checks required at admission。

如果 prior admission 的 source 已 stale：

```
do not reattach as current
→ REJECTED_STALE
```

---

# 20. Admission result vocabulary

定义：

```
U06AdmissionResult
```

status 只允许：

```
ADMITTED
REJECTED_INVALID
REJECTED_MODE_SOURCE_MISMATCH
REJECTED_STALE
REJECTED_UNAUTHORIZED
REJECTED_ALREADY_SATISFIED
REPLAY_CONFLICT
DEPENDENCY_UNAVAILABLE
```

语义：

### ADMITTED
```
all source/mode/currentness/Safety requirements passed
→ may form U06AdmittedInput
```

### REJECTED_INVALID
malformed/missing required refs/identity mismatch。

### REJECTED_MODE_SOURCE_MISMATCH
source authority 与 mode 不在合法矩阵。

### REJECTED_STALE
source/Gate/route/currentness 已失效。

### REJECTED_UNAUTHORIZED
Safety/restriction/source authority 不允许该 exact U06 action。

### REJECTED_ALREADY_SATISFIED
例如 MODE-1 requested but canonical current F3 already satisfies the exact basis。

### REPLAY_CONFLICT
same protected admission identity but different semantic fingerprint。

### DEPENDENCY_UNAVAILABLE
Admission 所需 authoritative currentness/binding/permission infrastructure 无法可靠读取。

注意：

```
DEPENDENCY_UNAVAILABLE
!= no business need
!= D04 STOP
!= C03 NO_RESULT
```

RDP-01 不输出 Clinical Readiness/F3/Question truth。

---

# 21. Admission reason-code families

Exact reason strings may be finalized during implementation design, but families are frozen：

```
U06_ADMISSION_REQUEST_MALFORMED
U06_ADMISSION_MODE_UNSUPPORTED
U06_ADMISSION_SOURCE_UNSUPPORTED
U06_ADMISSION_MODE_SOURCE_MISMATCH

U06_ADMISSION_CONSULTATION_MISMATCH
U06_ADMISSION_CDP_MISMATCH
U06_ADMISSION_STATE_STALE

U06_ADMISSION_SOURCE_STALE
U06_ADMISSION_ROUTE_STALE
U06_ADMISSION_GATE_STALE
U06_ADMISSION_SAFETY_BLOCKED
U06_ADMISSION_SAFETY_UNAVAILABLE
U06_ADMISSION_RESTRICTED_PERMISSION_MISSING
U06_ADMISSION_RESTRICTED_PERMISSION_DENIED

U06_ADMISSION_F1_CLARIFICATION_AUTHORITY_MISSING
U06_ADMISSION_U05_ELIGIBILITY_MISMATCH
U06_ADMISSION_F3_EFFECT_MISSING
U06_ADMISSION_REVALIDATION_SOURCE_MISMATCH
U06_ADMISSION_CONTINUATION_CONSEQUENCE_MISMATCH

U06_ADMISSION_CAPABILITY_BINDING_MISSING
U06_ADMISSION_QUESTION_POLICY_MISSING

U06_ADMISSION_ALREADY_SATISFIED
U06_ADMISSION_REPLAY_CONFLICT
U06_ADMISSION_DEPENDENCY_UNAVAILABLE
```

RDP-06 later freezes exact executable expectations.

---

# 22. Admission ordering

必须 fail closed，并采用以下逻辑顺序：

```
P0 structural envelope validation
↓
P1 consultation / CDP authoritative identity
↓
P2 mode vocabulary
↓
P3 source authority vocabulary
↓
P4 mode/source compatibility
↓
P5 source consequence / target qualification
↓
P6 authoritative current-state/source currentness
↓
P7 Safety / Gate / restricted permission
↓
P8 mode-specific business authority refs
↓
P9 binding/policy presence + admission-level applicability
↓
P10 replay/admission identity reconciliation
↓
ADMITTED
```

任何 P0-P10 failure：

```
no U06 business side effect
```

RDP-01 不把 failure 继续送给 C03/D04“看看能否处理”。

---

# 23. Scheduler / execution-intent boundary

Scheduler 或 application host 可以：

```
consume a lawful upstream source authority
create U06 target execution intent
copy source refs into U06ConsumerInboundRequest
invoke U06 Admission Host
```

Scheduler 不可以：

```
create missing F1 clarification truth
create missing F3 effect
change U05 readiness
change continuation consequence
synthesize Safety permission
pick different U06 mode because dependency unavailable
skip admission
```

一个 execution intent：

```
!= U06 admission
```

因此即使未来存在：

```
U06TargetExecutionIntent
```

也必须进入 RDP-01 admission。

---

# 24. Producer availability vs consumer contract

RDP-01 明确区分：

```
SOURCE CONTRACT DEFINED
!= SOURCE PRODUCER IMPLEMENTED
```

当前仓库 producer availability：

| Source | Contract semantics | Current producer implementation |
|---|---|---|
| U05_QUESTION_ROUTING | frozen + U05 in main | AVAILABLE |
| A1_PRE_READINESS_ROUTING | frozen Phase 6/8/9 | NOT_IMPLEMENTED |
| POST_F3_SAFETY_BARRIER_ROUTING | frozen Phase 6/8/9 | NOT_IMPLEMENTED |
| CLINICAL_CONTINUATION_ROUTING | frozen Phase 8/9 | NOT_IMPLEMENTED |
| F3_REASSESSMENT_ROUTING | frozen consequence semantics | NOT_IMPLEMENTED |
| F1_CLARIFICATION_ROUTING | business semantics frozen; projection defined by this RDP | ACTIVATION_BLOCKED_PENDING_CONTROLLED_AMENDMENT |

因此 future U06 non-production component verification 可以：

```
use synthetic authoritative source fixtures
```

但只能证明：

```
U06 consumer/admission behavior
```

不得声明：
- U04 A1 producer implemented；
- U01 F1 clarification routing implemented；
- Scheduler implemented；
- ClinicalContinuationRoutingDecision implemented；
- live loop closed。

---

# 25. Exact upstream amendment impact inventory

RDP-01 识别以下 implementation impacts，均 **NOT_AUTHORIZED**：

## U06-RDP01-IMP-01 — U04 A1 routing projection

Current `U04RoutingEligibility` 仅有：

```
u05Eligible
u11Eligible
u14Eligible
restrictedContextRequired
```

缺：

```
PRE_READINESS_A1_F3_C03_ELIGIBLE
routing_authorization_id
mode/target intent provenance
```

需要后续 controlled amendment / implementation authorization。

## U06-RDP01-IMP-02 — F1 clarification routing projection / activation gate

需要实现并独立 refreeze：

```
F1ClarificationRoutingEligibility
```

及 exact safety authority/clearance semantics。

在该 amendment closure 之前：

```
F1_CLARIFICATION_ROUTING
= disabled for executable admission
```

不得直接将 raw F1 state 暴露为 U06 execution authorization。

## U06-RDP01-IMP-03 — post-F3 barrier → MODE-3 routing

需要 executable source object / projection，绑定：
- canonical F3 effect；
- post-F3 U03/U04 basis；
- current Gate；
- route authorization；
- target mode。

## U06-RDP01-IMP-04 — ClinicalContinuationRoutingDecision producer

冻结合同存在，但 current `src/main` 未实现 producer。

U06 consumer 可先定义/验证 contract；producer implementation 需独立治理。

## U06-RDP01-IMP-05 — F3 REASSESSMENT_REQUIRED → MODE-1 admission projection

需要 typed reassessment execution/admission projection。

该 projection：
- only projects an already-authoritative `REASSESSMENT_REQUIRED`;
- binds the exact source revalidation decision/context/current Safety authority;
- does not own or recompute the reassessment decision.

不能让 Scheduler 仅看到枚举后直接调用 MODE-1。

## U06-RDP01-IMP-06 — Scheduler target-intent surface

current production `src/main` 无 generic Scheduler implementation。

U06 implementation scope必须后续明确：
- production/shared Scheduler amendment；
- bounded non-production execution host；
- verification fake/spy。

RDP-01 不选择或授权其中任何一种。

---

# 26. No direct live wiring

即使 RDP-01 冻结通过：

```
U05DownstreamEligibility
→ U06ConsumerInboundRequest
```

也不自动授权：

```
U05 live invokes U06
```

同理：

```
synthetic A1 eligibility
→ U06 admission PASS
```

不代表真实 U04→Scheduler→U06 已接线。

Live wiring/production routing 仍需独立 authorization。

---

# 27. Contract acceptance scenarios

这些是 RDP-01 design acceptance cases，不是最终 RDP-06 evidence IDs。

## MODE-1 A1

```
RDP01-AC-01
current A1 pre-readiness source + ALLOW
→ ADMITTED MODE-1

RDP01-AC-02
same source but canonical F3 already current
→ REJECTED_ALREADY_SATISFIED

RDP01-AC-03
old pre-readiness eligibility after state/basis change
→ REJECTED_STALE

RDP01-AC-04
RESTRICTED without exact PRE_READINESS_F3 permission
→ REJECTED_UNAUTHORIZED
```

## MODE-1 reassessment

```
RDP01-AC-05
REASSESSMENT_REQUIRED + current Gate/current basis
→ ADMITTED MODE-1

RDP01-AC-06
caller sees old F3 stale but no revalidation decision
→ REJECTED_INVALID / UNAUTHORIZED
```

## MODE-2 U05

```
RDP01-AC-07
current U05 TO_U06_QUESTION_PATH
+ NEEDS_CLARIFICATION
→ ADMITTED MODE-2

RDP01-AC-08
current U05 TO_U06_QUESTION_PATH
+ CAN_ASK_MORE
→ ADMITTED MODE-2

RDP01-AC-09
U05 eligibility targets U08
but request claims MODE-2/U06
→ REJECTED_MODE_SOURCE_MISMATCH

RDP01-AC-10
stale U05 eligibility
→ REJECTED_STALE
```

## MODE-2 F1

```
RDP01-AC-11
current governed F1ClarificationRoutingEligibility
+ valid safety clearance
→ ADMITTED MODE-2

RDP01-AC-12
raw CLARIFICATION_REQUIRED without routing eligibility
→ REJECTED_INVALID

RDP01-AC-13
direct F1 eligibility with missing/invalid safety clearance
→ REJECTED_UNAUTHORIZED / DEPENDENCY_UNAVAILABLE
```

## MODE-3

```
RDP01-AC-14
post-F3 barrier + current ALLOW Gate
→ ADMITTED MODE-3

RDP01-AC-15
ClinicalContinuationRoutingDecision
= TO_F3_CURRENT_VERSION_REVALIDATION
→ ADMITTED MODE-3

RDP01-AC-16
continuation consequence = TO_U05_CLINICAL_READINESS
but request claims MODE-3
→ REJECTED_MODE_SOURCE_MISMATCH

RDP01-AC-17
MODE-3 missing canonical F3 effect
→ REJECTED_INVALID

RDP01-AC-18
MODE-3 request supplies current C03 binding but historical provenance missing
→ REJECTED_INVALID
```

## Replay/global

```
RDP01-AC-19
same admission identity + same fingerprint + still-current source
→ REATTACHED

RDP01-AC-20
same admission identity + different fingerprint
→ REPLAY_CONFLICT

RDP01-AC-21
source becomes stale between request creation and admission
→ REJECTED_STALE

RDP01-AC-22
any admission failure
→ C03/D04/proposal/delivery effect counts remain zero
```

---

# 28. RDP-01 → later RDP handoffs

RDP-01 输出的 `U06AdmittedInput` 是后续设计唯一 caller-normalized S_in。

## To RDP-05
需要冻结：
- C03 binding applicability；
- D04/question policy binding；
- release refs applicability；
- conditional P03；
- source-specific binding currentness。

## To RDP-02
需要冻结：
- admitted F1 vs U05 NEEDS_CLARIFICATION vs CAN_ASK_MORE semantics；
- F3 owner decision；
- D04 stopping；
- no-progress result。

## To RDP-03
需要冻结：
- admission_id 与 F3/Question effect identity relationship；
- authoritative accepted refs；
- replay ledger；
- trace provenance；
- StatePatch ownership。

## To RDP-04
需要冻结：
- MODE-2 delivered/wait side effects；
- U07 handoff；
- failure handoff；
- external delivery boundaries。

## To RDP-06
必须验证完整 source/mode matrix、P0-P10 precedence 与 all-zero pre-admission effects。

---

# 29. Readiness blocker disposition

本设计若通过独立审查：

```
BF-U06-RG-01
= DESIGNED
```

但不能立即写：

```
BF-U06-RG-01 = CLOSED
```

因为 readiness blocker 最终 closure 还依赖：
- RDP-05 对 binding/applicability 的兼容确认；
- upstream impact inventory 的 aggregate compatibility review；
- RDP-06 可验证性；
- Implementation Readiness Re-Evaluation。

因此当前目标状态是：

```
BF-U06-RG-01
= CONTRACT_DESIGNED / PENDING_AGGREGATE_CLOSURE
```

---

# 30. Authorization boundary

RDP-01 不授权：

```
U06 implementation
U04 A1 amendment implementation
F1 routing amendment implementation
ClinicalContinuation router implementation
Scheduler implementation
C03 invocation
D04 execution
State mutation
Question selection
delivery
WAITING_USER
U07 execution
U14 live failure routing
production
real-patient traffic
```

---

# 31. Design verdict

```
U06-RDP-01 Consumer Inbound / Admission Contract
= DRAFT_FOR_INDEPENDENT_DESIGN_REVIEW

BF-U06-RG-01
= OPEN / DESIGN_PROPOSED

U06 Implementation Readiness
= NOT_READY

U06 Implementation Authorization
= NOT_GRANTED
```

Next after independent design PASS:

```
U06-RDP-05 Capability / Dependency / Applicability Contract
```

in the recommended construction order.


---

# 32. Independent Design Review Remediation

Initial Independent Design Review:

```
PR #230
review_id = 5287226930
verdict = REVISE_REQUIRED
```

Findings:

```
BF-U06-RDP01-IR-01
= DIRECT_F1_SOURCE_ACTIVATION_OVERSTATED

BF-U06-RDP01-IR-02
= REASSESSMENT_PROJECTION_OWNER_AMBIGUITY
```

Remediation:

1. split source/mode **business legality** from current **admission activation / producer implementation**;
2. retained direct F1 clarification as frozen business-legal U06 MODE-2 path;
3. froze direct F1 current activation as:
   `ACTIVATION_BLOCKED_PENDING_CONTROLLED_AMENDMENT`;
4. prohibited any implementation package from enabling direct F1 until exact routing + safety authority is independently reviewed/refrozen;
5. froze `F3_REASSESSMENT_ROUTING` as an execution/admission projection only;
6. bound its authority to the prior authoritative `F3CurrentVersionRevalidationDecision(REASSESSMENT_REQUIRED)` plus current Safety/routing authority;
7. added stable reassessment projection identity and cross-context replay isolation.

Current:

```
BF-U06-RDP01-IR-01
= REMEDIATED / TARGETED_RE_REVIEW_PENDING

BF-U06-RDP01-IR-02
= REMEDIATED / TARGETED_RE_REVIEW_PENDING

U06-RDP-01
= REVISED / TARGETED_RE_REVIEW_PENDING
```
