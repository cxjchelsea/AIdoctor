# U05 RDP-01 Consumer Inbound Contract v0.1

> Scope: U05 / D03 的 pre-D03 consumer admission、统一入站 envelope、route/gate/currentness/replay/restricted-context 边界。  
> Design basis: U05 Implementation Readiness Re-Evaluation v0.2 / PR #170 exact head `39e13a91fc2fc2dc78fbda99c063af2411b8b24f`.  
> Exact frozen semantic baseline: `3bd85f908a1cb09355f6ea1c5ce737638d1c0fdc`.  
> Status: **PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW**.  
> Target blocker: `BF-U05-RG-01`.  
> 本文件不授予 U05 implementation、D03 execution、downstream execution、merge、production、release activation 或 real-patient authorization。

---

# 1. Purpose

U05 已经有明确业务职责：

```text
U05
= Clinical Readiness 唯一求值 Unit

D03
= Clinical Readiness deterministic policy

RDP-05
= readiness input applicability/currentness contract

RDP-02
= D03 deterministic decision contract
```

但当前 Frozen baseline 中，U05 的入口合同仍分散在：

```text
U04-RDP-04
A1 amendments
RDP-05
RDP-02
post-DDx routing
post-analysis routing
ClinicalContinuationRoutingDecision
CL-04
Phase 6 / 8 / 9
```

因此本文件只解决一个问题：

> **什么样的上游请求，才有资格真正进入 U05 / D03？**

RDP-01 必须把以下内容统一：

```text
identity
current Clinical State binding
current committed Safety Gate
routing authorization
routing source/context
restricted permission
readiness-input-set identity
replay/idempotency
pre-D03 fail-closed rejection
trace/correlation
```

同时保持：

```text
admission
!= D03 decision
!= Clinical Readiness
!= state mutation
!= downstream routing
```

---

# 2. Non-negotiable ownership boundaries

必须保持：

```text
Clinical Truth
!= Model Output
!= Capability Result
!= Runtime State
!= Trace
```

RDP-01 的 Owner 是：

```text
U05 consumer admission boundary
```

它只回答：

```text
当前这份 U05 inbound request
是否有资格进入 D03？
```

它不回答：

```text
Clinical Readiness 是什么
哪个 business signal 优先
READY 是否成立
F3/F5/F6 是否医学上充分
下游应执行 U06/U08/U10/U11 哪一个
是否 commit Clinical Readiness
```

职责分层：

```text
U04 / continuation routing
-> produces current route eligibility / authorization

RDP-01
-> validates U05 consumer admission

RDP-05
-> defines readiness-input applicability/currentness semantics

RDP-02 / D03
-> resolves Clinical Readiness

RDP-03
-> later defines readiness proposal/commit/trace

RDP-04
-> later defines post-D03 downstream routing/side-effect boundary

RDP-06
-> later verifies all of the above
```

---

# 3. Pre-D03 sequence

正式顺序：

```text
authoritative committed Clinical State
+
current committed U04 Safety Gate
+
current routing source / routing authorization
+
RDP-05 readiness-input set
        ↓
U05-RDP-01 consumer admission
        ↓
ADMITTED envelope
        ↓
D03
```

如果 RDP-01 拒绝：

```text
no D03 invocation
no D03 decision_id
no D03 decision_status
no Clinical Readiness
no K09 readiness proposal
no ordinary U05 downstream route
```

RDP-01 rejection 不是：

```text
D03 INPUT_FAILURE
D03 INPUT_CONFLICT
Clinical Readiness result
Capability failure
Safe Exit result
```

---

# 4. Supported evaluation contexts

RDP-01 不创建新的 clinical evaluation context，只消费当前已冻结的 context。

V1 admission context 至少包括：

```text
POST_SAFETY_INITIAL
A1_POST_BARRIER_CURRENT
POST_USER_FACT_UPDATE
POST_DDX_REEVALUATION
POST_OFFLINE_ASSESSMENT
```

另外以下 A1 中间态明确属于：

```text
PRE-D03 NON-ENTRY
```

而不是 U05 admission context：

```text
A1_POST_SAFETY_BOOTSTRAP_REQUIRED
A1_PRE_READINESS_IN_PROGRESS
A1_F3_COMMITTED_BARRIER_PENDING
```

这些状态下：

```text
U05 admission = PROHIBITED
D03 = not invoked
```

## 4.1 Current selected bootstrap architecture

当前已选择：

```text
BootstrapArchitectureBindingRef = A1
```

因此当前 first-entry 正常路径应是：

```text
A1_POST_BARRIER_CURRENT
```

而不是绕过 A1 直接把 `POST_SAFETY_INITIAL` 解释成当前 first-entry route。

`POST_SAFETY_INITIAL` 作为 frozen non-A1 baseline 仍保留，但：

```text
it is admissible only when
a separately valid current bootstrap architecture binding
lawfully permits the non-A1 ordinary entry.
```

RDP-01 不自行选择 bootstrap architecture。

---

# 5. Canonical U05 inbound request envelope

定义统一语义对象：

```text
U05ConsumerInboundRequest
```

最小字段：

```text
request_id

consultation_id
cdp_id

claimed_clinical_state_version
authoritative_state_ref

evaluation_context

route_source_type
route_source_ref
route_consequence

u04_gate_ref
u04_gate_commit_ref
gate_value

routing_authorization_id
routing_policy_version

bootstrap_architecture_binding_ref? 

restricted_context_ref?
restricted_permission_ref?

readiness_input_manifest_ref
readiness_input_set_identity
readiness_input_refs[]

canonical_event_ref
business_event_identity
thread_id?
run_id?
correlation_id
trace_id

admission_contract_version

created_at
```

这里：

```text
request_id
!= business truth identity
!= D03 decision_id
```

它只是一次 inbound request 的 transport/application identity。

## 5.1 Mandatory vs conditional fields

始终必须：

```text
consultation_id
cdp_id
claimed_clinical_state_version
evaluation_context
route_source_type
route_source_ref
u04_gate_ref
u04_gate_commit_ref
gate_value
routing_authorization_id
readiness_input_manifest_ref
readiness_input_set_identity
canonical_event_ref / business_event_identity
correlation_id
trace_id
admission_contract_version
```

条件必需：

```text
RESTRICTED
-> restricted_context_ref required
-> restricted_permission_ref required

A1_POST_BARRIER_CURRENT
-> bootstrap_architecture_binding_ref = A1 required

ClinicalContinuationRoutingDecision source
-> route_consequence = TO_U05_CLINICAL_READINESS required
```

---

# 6. Route source vocabulary

RDP-01 冻结 route source 的最小类别：

```text
U04_ORDINARY_ROUTING
U04_A1_POST_BARRIER_ROUTING
CLINICAL_CONTINUATION_ROUTING
```

它们不是 Clinical Truth。

## 6.1 U04_ORDINARY_ROUTING

适用于：

```text
POST_SAFETY_INITIAL
```

但只有在当前 bootstrap binding 合法允许 non-A1 ordinary path 时才可使用。

要求：

```text
current committed U04 Gate
+ current U05 ordinary eligibility
+ current routing_authorization_id
```

不得在当前 A1 bootstrap required 时绕过 A1。

## 6.2 U04_A1_POST_BARRIER_ROUTING

适用于：

```text
A1_POST_BARRIER_CURRENT
```

要求：

```text
BootstrapArchitectureBindingRef = A1
+ A1 canonical F3 completion exists
+ F3 current-version revalidation = REVALIDATED_CURRENT
+ current F3 readiness input exists
+ current post-barrier U04 Gate
+ current U05_ELIGIBLE
+ current routing_authorization_id
```

必须禁止：

```text
old pre-F3 Gate
old pre-F3 routing authorization
old PRE_READINESS_A1 eligibility
```

进入 U05。

## 6.3 CLINICAL_CONTINUATION_ROUTING

适用于：

```text
POST_USER_FACT_UPDATE
POST_DDX_REEVALUATION
POST_OFFLINE_ASSESSMENT
```

route source 必须是 current：

```text
ClinicalContinuationRoutingDecision
```

并且：

```text
route_consequence
= TO_U05_CLINICAL_READINESS
```

如果 current route consequence 是：

```text
TO_F3_CURRENT_VERSION_REVALIDATION
TO_F6_CURRENT_VERSION_REASSESSMENT
TO_U08_REASSESSMENT
TO_U12_DELIVERY_PREPARATION
FAILURE_ROUTE
```

则：

```text
U05 admission = REJECTED
```

RDP-01 不允许调用方把其他 consequence 重标成 U05。

---

# 7. Authoritative current-state binding

调用方传入的：

```text
claimed_clinical_state_version
```

不是系统真源。

Admission Host 必须独立装载：

```text
authoritative current Clinical State / CDP
```

并校验：

```text
request.consultation_id
= authoritative consultation_id

request.cdp_id
= authoritative cdp_id

request.claimed_clinical_state_version
= authoritative current Clinical State Version
```

禁止：

```text
caller says current -> trust caller
old version -> silently rebind to new version
old route refs -> copy onto current version
```

任何 Clinical State Version advance 在 admission 前发生，都必须使旧 request 的 route/currentness 重新校验。

---

# 8. U04 Gate admission rules

RDP-01 只接受：

```text
current committed Gate
= ALLOW
or action-permitted RESTRICTED
```

## 8.1 ALLOW

必须证明：

```text
u04_gate_ref exists
u04_gate_commit_ref exists
Gate is committed
Gate is current/routable for authoritative current state
routing_authorization_id derives from that current Gate
```

## 8.2 RESTRICTED

必须额外证明：

```text
restricted_context_ref exists
restricted_permission_ref exists
current restriction explicitly permits
U05 Clinical Readiness evaluation
for this exact context/consequence
```

并要求：

```text
restricted_context_ref
from Gate
= route source
= routing authorization
= inbound request
```

不可 silent widen：

```text
RESTRICTED
-> ALLOW
```

## 8.3 BLOCKED

```text
BLOCKED
-> U05 admission prohibited
```

RDP-01 不把 BLOCKED 转成：

```text
OUT_OF_SCOPE
NO_RELIABLE_DIRECTION
INPUT_FAILURE
```

## 8.4 UNAVAILABLE

```text
UNAVAILABLE
-> U05 admission prohibited
```

失败/恢复由现有 U14/governed failure path 处理。

---

# 9. Routing authorization contract

U05 admission 必须绑定：

```text
routing_authorization_id
```

最小语义必须可证明：

```text
consultation_id
cdp_id
authorized Clinical State Version
u04_gate_ref
business_event_identity
selected route path
evaluation_context
restricted_context_ref when applicable
routing policy/version
validity
```

RDP-01 不要求所有实现使用同一个 Java class，但上述语义不可缺失。

## 9.1 Routing authorization currentness

以下任一发生时，旧 authorization 不可路由：

```text
Clinical State Version advances
u04_gate_ref no longer current
route decision superseded
route consequence changes
BootstrapArchitectureBindingRef changes
restricted context changes/incompatible
relevant owner recomputation is required
authorization explicitly revoked/expired
```

除非 Frozen contract 明确说明某个 derived decision 可通过 owner current-version revalidation 重新形成新可用 ref。

禁止：

```text
old routing_authorization_id
+ new Clinical State Version
-> automatic reuse
```

---

# 10. RDP-05 readiness input manifest at admission

RDP-01 不重新定义 RDP-05 的 business signal 或 applicability vocabulary。

它只要求 inbound request 绑定一个：

```text
readiness_input_manifest_ref
```

该 manifest 必须按当前 RDP-05 frozen semantics 覆盖当前 evaluation context 所需的 domain slots。

至少覆盖：

```text
F1
F3
F5
F6
```

`F2_CLARIFICATION` 在适用时加入。

每个 slot 至少可证明：

```text
source_domain
applicability_status
readiness_input_ref? 
source_decision_ref?
source_state_ref?
applicability_evidence_ref
validity/currentness metadata
invalidation_ref when applicable
```

## 10.1 PRESENT

```text
PRESENT
-> readiness_input_ref required
-> authoritative RDP-05 envelope must exist
```

RDP-01 校验 identity/binding/structure。

D03 再校验：

```text
business signal
conflict
precedence
requiredness
```

## 10.2 NOT_YET_APPLICABLE / ABSENT_BY_DESIGN

必须有 authoritative applicability evidence。

禁止：

```text
missing row
null lookup
no artifact found
-> infer NOT_YET_APPLICABLE
```

同样禁止：

```text
no artifact found
-> infer ABSENT_BY_DESIGN
```

## 10.3 STALE / FAILED / UNAVAILABLE

必须区分两类：

### A. lawful pre-D03 recomputation state

例如：

```text
STALE_BY_UPSTREAM_MUTATION
+ valid mutation/invalidation provenance
+ frozen continuation contract says owner recomputation required
```

则：

```text
TO_F3_CURRENT_VERSION_REVALIDATION
TO_F6_CURRENT_VERSION_REASSESSMENT
TO_U08_REASSESSMENT
or other governed owner path
```

应该先发生。

若 request 仍声称：

```text
TO_U05_CLINICAL_READINESS
```

则：

```text
RDP-01 rejects
= pending owner recomputation / route mismatch
```

不进入 D03。

### B. admitted input-level failure

如果当前 route legitimately reaches U05，但 required RDP-05 input 本身是：

```text
FAILED
UNAVAILABLE
unexpected STALE
```

且没有更具体的 pre-D03 owner recomputation route，则 RDP-01 可在 structural/current route admission 通过后将该 exact manifest 交给 D03。

D03 根据 RDP-02 形成：

```text
INPUT_FAILURE
```

因此必须保持：

```text
routing/admission stale
!= readiness-input failure
```

---

# 11. Context-specific admission matrix

| evaluation_context | route source | mandatory current route consequence | special prerequisites | U05 admission |
|---|---|---|---|---|
| POST_SAFETY_INITIAL | U04_ORDINARY_ROUTING | U05 ordinary eligibility | only when current bootstrap binding legally permits non-A1 ordinary path | conditional |
| A1_POST_BARRIER_CURRENT | U04_A1_POST_BARRIER_ROUTING | U05_ELIGIBLE | A1 completion + F3 REVALIDATED_CURRENT + current F3 input | allowed |
| POST_USER_FACT_UPDATE | CLINICAL_CONTINUATION_ROUTING | TO_U05_CLINICAL_READINESS | accepted mutation provenance current; no unresolved required owner recomputation | allowed |
| POST_DDX_REEVALUATION | CLINICAL_CONTINUATION_ROUTING | TO_U05_CLINICAL_READINESS | current/compatible F3/F5/F6 route basis | allowed |
| POST_OFFLINE_ASSESSMENT | CLINICAL_CONTINUATION_ROUTING | TO_U05_CLINICAL_READINESS | current F6 return basis; no higher non-D03 consequence | allowed |

Explicitly rejected as U05 entry:

```text
A1_POST_SAFETY_BOOTSTRAP_REQUIRED
A1_PRE_READINESS_IN_PROGRESS
A1_F3_COMMITTED_BARRIER_PENDING
```

---

# 12. Canonical readiness-input-set identity

定义：

```text
READINESS_INPUT_SET_IDENTITY
```

它不是 Clinical Truth，而是本次 D03 输入 snapshot 的稳定 identity。

最小语义组成：

```text
consultation_id
cdp_id
authoritative Clinical State Version
evaluation_context

for each domain slot:
  source_domain
  applicability_status
  readiness_input_ref when PRESENT
  applicability_evidence_ref
  source_decision/state ref identity when applicable
  validity/invalidation identity

RDP-05 contract version
```

不冻结具体 hash 算法。

要求：

```text
same semantic input set
-> same identity

different accepted input ref
different applicability status
different invalidation basis
different evaluation context
different current version
-> different identity
```

D03 必须消费 admission 已固定的 exact input-set identity，不得在执行中偷偷换成后来出现的另一组 readiness inputs。

如果执行前 authoritative current state 已变化：

```text
old admission becomes stale
-> re-admit
```

---

# 13. U05 admission identity / idempotency

定义：

```text
U05_ADMISSION_ID
```

语义至少绑定：

```text
consultation_id
cdp_id
authoritative Clinical State Version
evaluation_context
route_source_ref
u04_gate_ref
routing_authorization_id
READINESS_INPUT_SET_IDENTITY
restricted_context_ref when applicable
business_event_identity
admission_contract_version
```

不冻结 hash/UUID 计算方式。

## 13.1 Exact replay

如果：

```text
same U05_ADMISSION_ID
+ same normalized inbound fingerprint
```

则：

```text
reattach to existing admission record
no second admission effect
```

Runtime 后续必须再 reconcile：

```text
whether D03 decision already exists
whether readiness commit already exists
```

RDP-01 不自行伪造 D03 replay result。

## 13.2 Replay conflict

如果：

```text
same U05_ADMISSION_ID
+ different normalized inbound fingerprint
```

则：

```text
REJECTED
reason = U05_ADMISSION_REPLAY_CONFLICT
no D03
```

## 13.3 New state / new route / new input set

任一发生：

```text
new Clinical State Version
new Gate
new routing authorization
new route source
new readiness-input-set identity
new restricted context
```

不得 attach 到旧 admission identity。

---

# 14. Admission outcome contract

RDP-01 只输出：

```text
U05AdmissionResult
```

最小字段：

```text
admission_id
request_id

admission_status
replay_disposition

reason_code?
reason_refs[]

admitted_input_ref?
rejected_input_fingerprint?

consultation_id
cdp_id
clinical_state_version
evaluation_context

u04_gate_ref
routing_authorization_id
route_source_ref
readiness_input_set_identity
restricted_context_ref?

correlation_id
trace_id

admission_contract_version
created_at
```

## 14.1 admission_status vocabulary

仅允许：

```text
ADMITTED
REJECTED
```

不得增加：

```text
READY
NOT_READY
INPUT_FAILURE
INPUT_CONFLICT
BLOCKED
UNAVAILABLE
```

这些属于其他业务/治理层。

## 14.2 replay_disposition

允许：

```text
ORIGINAL
REATTACHED
```

`REATTACHED` 仍然引用同一个 authoritative admission record。

---

# 15. Admitted U05 input envelope

只有：

```text
admission_status = ADMITTED
```

才形成：

```text
U05AdmittedInput
```

它至少冻结：

```text
admission_id

consultation_id
cdp_id
clinical_state_version
evaluation_context

accepted_u04_gate_ref
accepted_routing_authorization_id
accepted_route_source_ref

accepted_readiness_input_manifest_ref
accepted_readiness_input_set_identity
accepted_readiness_input_refs[]

accepted_restricted_context_ref?

canonical_event_ref
business_event_identity
correlation_id
trace_id

admission_contract_version
```

该 envelope 是 D03 的 precondition snapshot。

不得：

```text
D03 re-fetch arbitrary latest inputs
and replace admitted input set silently
```

如果 current state 已变化：

```text
admitted envelope = stale
-> new RDP-01 admission required
```

---

# 16. Pre-D03 admission rejection reason family

RDP-01 冻结 reason family；最终 Java enum/string naming 可在实现时保持等价。

## 16.1 Structural / identity

```text
U05_ADMISSION_MALFORMED_REQUEST
U05_ADMISSION_CONSULTATION_ID_MISMATCH
U05_ADMISSION_CDP_ID_MISMATCH
U05_ADMISSION_STATE_VERSION_MISMATCH
U05_ADMISSION_CONTEXT_MISMATCH
```

## 16.2 Gate / routing

```text
U05_ADMISSION_GATE_NOT_COMMITTED
U05_ADMISSION_GATE_NOT_CURRENT
U05_ADMISSION_GATE_NOT_ELIGIBLE
U05_ADMISSION_ROUTING_AUTH_MISSING
U05_ADMISSION_ROUTING_AUTH_STALE
U05_ADMISSION_ROUTE_SOURCE_MISMATCH
U05_ADMISSION_ROUTE_CONSEQUENCE_NOT_U05
```

## 16.3 Restricted

```text
U05_ADMISSION_RESTRICTED_CONTEXT_MISSING
U05_ADMISSION_RESTRICTED_CONTEXT_MISMATCH
U05_ADMISSION_RESTRICTED_PERMISSION_MISSING
U05_ADMISSION_RESTRICTED_ACTION_NOT_PERMITTED
```

## 16.4 Readiness manifest

```text
U05_ADMISSION_INPUT_MANIFEST_MISSING
U05_ADMISSION_INPUT_SET_IDENTITY_MISMATCH
U05_ADMISSION_INPUT_REF_IDENTITY_MISMATCH
U05_ADMISSION_APPLICABILITY_EVIDENCE_MISSING
U05_ADMISSION_PENDING_OWNER_RECOMPUTATION
```

## 16.5 Replay / governance

```text
U05_ADMISSION_REPLAY_CONFLICT
U05_ADMISSION_CONTRACT_VERSION_MISMATCH
U05_ADMISSION_ENVIRONMENT_NOT_AUTHORIZED
```

不得用这些 reason 形成 patient-facing conclusion。

---

# 17. Admission validation order

为避免先解释 business semantics 再发现 route 不合法，固定如下顺序：

```text
A0 request structural validation
A1 environment / authorization scope validation
A2 consultation + CDP identity validation
A3 authoritative current Clinical State load
A4 claimed/current version validation
A5 evaluation_context validation
A6 current committed U04 Gate validation
A7 ALLOW / RESTRICTED action-permission validation
A8 route source + route consequence validation
A9 routing_authorization currentness/binding validation
A10 bootstrap binding / context-specific prerequisite validation
A11 readiness input manifest structural/identity validation
A12 unresolved owner-recomputation guard
A13 admission identity / replay reconciliation
A14 create or reattach authoritative U05 admission
A15 hand admitted snapshot to D03
```

RDP-01 不在 A11/A12 执行：

```text
D03 business precedence
READY policy
NO_RELIABLE_DIRECTION policy
input conflict business resolution
```

---

# 18. Context-specific rules in detail

## 18.1 A1_POST_BARRIER_CURRENT

必须同时满足：

```text
BootstrapArchitectureBindingRef = A1

A1 canonical F3 completion ref exists

post-F3 Safety barrier completed

current committed U04 Gate
= ALLOW or permitted RESTRICTED

current routing authorization
= U05 eligible

F3 current-version revalidation
= REVALIDATED_CURRENT

current F3 readiness input exists

RDP-05 manifest current/compatible
```

旧：

```text
pre-F3 Gate
pre-F3 route authorization
PRE_READINESS_A1_F3_C03_ELIGIBLE
```

不得用于 admission。

## 18.2 POST_USER_FACT_UPDATE

必须证明：

```text
accepted current fact/correction mutation provenance
+ current U03/U04 Safety basis
+ current ClinicalContinuationRoutingDecision
+ consequence = TO_U05_CLINICAL_READINESS
```

如果：

```text
F3 mutation-stale requiring revalidation
F5 mutation-stale requiring U08
F6 mutation-stale requiring reassessment
```

仍未解决，则 current consequence 不应是 U05。

RDP-01 必须 fail closed：

```text
U05_ADMISSION_PENDING_OWNER_RECOMPUTATION
```

## 18.3 POST_DDX_REEVALUATION

必须证明：

```text
current ClinicalContinuationRoutingDecision
+ context = POST_DDX_REEVALUATION
+ consequence = TO_U05_CLINICAL_READINESS
+ current/compatible owner inputs
```

不得将：

```text
TO_U08_REASSESSMENT
TO_U12_DELIVERY_PREPARATION
```

重标为 U05。

## 18.4 POST_OFFLINE_ASSESSMENT

必须证明：

```text
current F6 return basis
+ current ClinicalContinuationRoutingDecision
+ context = POST_OFFLINE_ASSESSMENT
+ consequence = TO_U05_CLINICAL_READINESS
```

如果 F6/F3/F5 current profile 已明确产生：

```text
TO_U12_DELIVERY_PREPARATION
TO_U08_REASSESSMENT
TO_F3_CURRENT_VERSION_REVALIDATION
TO_F6_CURRENT_VERSION_REASSESSMENT
FAILURE_ROUTE
```

则 U05 admission prohibited。

---

# 19. Uncommitted / stale / malformed boundaries

## 19.1 Uncommitted Gate

任何：

```text
candidate Gate
proposal-only Gate
uncommitted U04 decision
```

均不可进入 U05。

## 19.2 Stale route

以下任一视为 stale/non-routable：

```text
route version != authoritative current version
route points to superseded Gate
route decision superseded
route consequence no longer current
restricted context changed
bootstrap completion/revalidation stale
required owner recomputation pending
```

## 19.3 Malformed request

Malformed 只表示 consumer admission 失败。

不得映射成：

```text
NO_RELIABLE_DIRECTION
OUT_OF_SCOPE
READY_FOR_CLINICAL_ANALYSIS
```

---

# 20. Restricted-context propagation

RDP-01 是 restricted path 的强制 preservation boundary。

当：

```text
Gate = RESTRICTED
```

必须绑定同一：

```text
restricted_context_ref
```

贯穿：

```text
U04 Gate
-> routing source
-> routing authorization
-> U05 inbound request
-> U05 admitted envelope
-> D03 decision envelope
```

RDP-01 只负责到 admitted envelope。

后续：

```text
RDP-02 / D03
RDP-03 commit
RDP-04 downstream
```

必须继续保持该 context。

如果 downstream action permission 比 U05 evaluation 更窄：

```text
U05 admission allowed
!= all downstream consequences allowed
```

不能在 RDP-01 提前放宽。

---

# 21. Non-production authorization scope

当前 U05 readiness package 仍为：

```text
NON_PRODUCTION_ONLY
```

因此 inbound request 必须绑定当前受授权 environment / execution scope。

RDP-01 不硬编码：

```text
prod
staging
test
```

字符串，但必须能够验证：

```text
current environment authorization profile
permits U05 non-production evaluation
```

未经授权的 production/live request：

```text
REJECTED
reason = U05_ADMISSION_ENVIRONMENT_NOT_AUTHORIZED
```

---

# 22. Trace / audit requirements

RDP-01 不把 Trace 当 Clinical Truth，但 admission 必须可审计。

至少关联：

```text
admission_id
request_id
consultation_id
cdp_id
clinical_state_version
evaluation_context

u04_gate_ref
u04_gate_commit_ref
routing_authorization_id
route_source_ref

readiness_input_manifest_ref
readiness_input_set_identity
readiness_input_refs[]

restricted_context_ref when applicable

canonical_event_ref
business_event_identity
correlation_id
trace_id

admission_status
reason_code
replay_disposition
admission_contract_version
```

默认禁止在 admission trace 复制完整 PHI payload。

---

# 23. Concurrency / race rules

## 23.1 State changes during admission

如果：

```text
authoritative state loaded at Vn
but before admission record is finalized
Clinical State advances to Vn+1
```

则：

```text
admission for Vn must not become current
```

实现可以：

```text
optimistic compare-and-check
lock
transactional validation
```

但语义必须是 fail closed。

## 23.2 Gate/route changes during admission

如果 Gate 或 routing authorization 在 admission finalization 前被 superseded：

```text
reject/retry from authoritative state
```

不得用“刚才还是 current”继续执行 D03。

## 23.3 Readiness input set changes at same Clinical State Version

某些 owner revalidation 是非 state-mutating 的。

因此：

```text
same Clinical State Version
!= same readiness input set
```

如果 current authoritative `READINESS_INPUT_SET_IDENTITY` 与 request 不一致：

```text
reject old request
-> require re-admission
```

---

# 24. Replay/crash semantics

## 24.1 crash after admission / before D03

恢复：

```text
lookup U05_ADMISSION_ID
-> reattach same admitted envelope
-> verify it is still current
-> if no D03 effect exists, D03 may continue
```

## 24.2 crash after D03 / before readiness commit

RDP-01 不创建第二个 admission。

Runtime/RDP-03 必须基于：

```text
admission_id
+ D03 decision identity
```

reconcile 后续 effect。

## 24.3 crash after readiness commit

不得因为 request replay：

```text
create second readiness commit
```

该规则由 RDP-03/RDP-06 进一步验证，但 RDP-01 必须提供稳定 admission identity 支撑。

---

# 25. Prohibited implementations

一律禁止：

```text
frontend says safe -> admit U05
model says continue -> admit U05
missing Gate -> assume ALLOW
RESTRICTED without context -> treat as ALLOW
old Gate + new Clinical State -> silently rebind
old routing auth -> reuse after state mutation
route consequence != U05 -> still invoke U05
missing F5 artifact -> infer NOT_YET_APPLICABLE
mutation-stale F6 -> admit D03 as no offline need
duplicate request -> create new admission identity
same admission id + changed payload -> accept
D03 re-fetch arbitrary latest readiness inputs
U05 admission rejection -> D03 INPUT_FAILURE
```

---

# 26. Contract examples

## Example A — A1 first-entry admitted

```text
context = A1_POST_BARRIER_CURRENT
Gate = ALLOW / committed/current
A1 completion = current
F3 revalidation = REVALIDATED_CURRENT
route = U05_ELIGIBLE
readiness input set = current
```

Result：

```text
ADMITTED
-> D03
```

## Example B — A1 barrier pending

```text
context = A1_F3_COMMITTED_BARRIER_PENDING
old Gate exists
```

Result：

```text
REJECTED / context not U05-admissible
no D03
```

## Example C — BLOCKED

```text
Gate = BLOCKED
```

Result：

```text
REJECTED / U05_ADMISSION_GATE_NOT_ELIGIBLE
no D03
```

## Example D — RESTRICTED context lost

```text
Gate = RESTRICTED
route authorization has restricted_context_ref = R1
request restricted_context_ref = absent
```

Result：

```text
REJECTED / U05_ADMISSION_RESTRICTED_CONTEXT_MISSING
```

## Example E — post-user-fact-update F6 mutation-stale

```text
context = POST_USER_FACT_UPDATE
F6 = STALE_BY_UPSTREAM_MUTATION
current frozen router requires F6 reassessment
request claims TO_U05_CLINICAL_READINESS
```

Result：

```text
REJECTED / U05_ADMISSION_PENDING_OWNER_RECOMPUTATION
```

## Example F — current route to U05 but required source FAILED

```text
route = valid/current TO_U05_CLINICAL_READINESS
readiness manifest structurally valid
required input = FAILED
no separate owner recomputation path applies
```

Result：

```text
RDP-01 = ADMITTED
D03 = INPUT_FAILURE
```

This preserves:

```text
consumer admission failure
!= readiness-input failure
```

## Example G — exact replay

```text
same admission identity
same normalized fingerprint
```

Result：

```text
ADMITTED / REATTACHED
same admission_id
no duplicate admission effect
```

## Example H — replay conflict

```text
same admission identity
different readiness_input_set_identity
```

Result：

```text
REJECTED / U05_ADMISSION_REPLAY_CONFLICT
```

---

# 27. Relationship to RDP-02

RDP-02 已冻结：

```text
pre-D03 admission belongs to RDP-01
```

本文件落实该边界。

RDP-01 rejection：

```text
no D03 object
```

只有 admitted envelope 才能进入：

```text
D03 DECIDED
D03 INPUT_FAILURE
D03 INPUT_CONFLICT
```

RDP-01 不定义：

```text
D03 precedence
D03-POL-005
D03-POL-011
POLICY_EXPECTATION_GAP
```

---

# 28. Relationship to RDP-05

RDP-05 owns：

```text
source_domain
applicability_status
business_signal
readiness input currentness
context-specific requiredness
```

RDP-01 owns：

```text
whether exact RDP-05 input set
is structurally/governance-valid to hand to D03
```

因此：

```text
RDP-01 validates the envelope
RDP-05 defines the semantic slots
D03 interprets admitted business signals
```

---

# 29. Relationship to future RDP-03

RDP-03 必须消费：

```text
admission_id
accepted_readiness_input_set_identity
accepted_readiness_input_refs
accepted Gate/route/restricted refs
D03 decision_id
```

来形成 Clinical Readiness K09 proposal/commit。

RDP-03 不得在 commit 时替换成另一组未 admission 的 inputs。

---

# 30. Relationship to future RDP-04

RDP-04 必须从：

```text
committed Clinical Readiness
+ current committed Safety/restriction context
```

形成 post-D03 downstream consequence。

RDP-01 不直接执行：

```text
U06
U08
U10
U11
```

也不形成 post-D03 route。

---

# 31. Relationship to future RDP-06

RDP-06 至少要验证：

```text
ALLOW admission
permitted RESTRICTED admission
RESTRICTED context missing rejection
BLOCKED rejection
UNAVAILABLE rejection

A1 old-Gate rejection
A1 post-barrier admission

POST_USER_FACT_UPDATE correct route admission
mutation-stale owner-recompute rejection
POST_DDX wrong consequence rejection
POST_OFFLINE wrong consequence rejection

state version mismatch
stale Gate
uncommitted Gate
stale routing authorization
route source mismatch
manifest identity mismatch

exact replay reattachment
replay conflict
same-version input-set change
race during admission

RDP-01 rejection -> no D03
admitted required-source failure -> D03 INPUT_FAILURE
```

---

# 32. BF-U05-RG-01 disposition

Original blocker：

```text
BF-U05-RG-01
= U04 -> U05 consumer admission contract missing
```

本设计已提供：

```text
unified inbound request
supported context matrix
route source vocabulary
authoritative state binding
Gate/currentness contract
RESTRICTED preservation
routing authorization contract
RDP-05 manifest binding
pre-D03 owner-recompute guard
admission identity
replay/idempotency
typed rejection reasons
admitted immutable snapshot
trace/audit refs
race/crash semantics
RDP-02/03/04/05/06 boundaries
```

因此当前只能推进到：

```text
BF-U05-RG-01
= DESIGN_RESOLVED / INDEPENDENT_REVIEW_PENDING

U05-RDP-01
= PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW
```

只有独立审查 PASS 后，才可考虑：

```text
BF-U05-RG-01
= CLOSED
U05-RDP-01
= FROZEN / PASS_FOR_READINESS
```

---

# 33. Authorization boundary

本文件不授权：

```text
U05 runtime/code implementation
D03 runtime execution
U04 -> U05 live routing
Clinical Readiness commit
U05 -> U06/U08/U10/U11 execution
new clinical policy
new medical truth
production Clinical State mutation
merge to main
production Clinical Runtime
release activation
real-patient traffic
```

当前 aggregate status 仍为：

```text
BF-U05-RG-01 = DESIGN_RESOLVED / REVIEW_PENDING
BF-U05-RG-02 = CLOSED
BF-U05-RG-03 = OPEN / BLOCKING
BF-U05-RG-04 = OPEN / BLOCKING
BF-U05-RG-05 = CLOSED
BF-U05-RG-06 = OPEN / BLOCKING

U05 Implementation Readiness
= NOT_READY

U05 Implementation Authorization
= NOT_GRANTED
```
