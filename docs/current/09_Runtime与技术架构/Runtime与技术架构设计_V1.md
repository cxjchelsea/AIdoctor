# AIdoctor Phase 9 — Runtime 与技术架构设计 V1

> 状态：DRAFT COMPLETE / REVIEW REMEDIATED / NOT FROZEN
> 适用基线：`main` 当前真实代码 + Phase 1～8 已冻结权威设计
> 目标：在不改变既有业务语义、状态 Owner、Unit、Capability 与 Contract 边界的前提下，定义 V1 临床 Runtime 的执行、等待、恢复、提交、失败、并发、版本绑定、外部副作用一致性与 Brownfield 迁移架构。
> 非目标：本文件不构成 Implementation Authorization；不冻结具体 Runtime 框架、消息队列、数据库、微服务拆分或部署厂商；不进入 Phase 10。

---

# 1. Phase 9 的边界

Phase 1～8 已冻结：

```text
业务语义
→ 状态与 Owner
→ 业务闭环
→ Unit
→ Capability / deterministic policy
→ Contract / Data semantics
```

Phase 9 只回答：

```text
谁驱动一次执行？
如何从 committed Clinical State 选择下一 Unit？
何时 WAIT？如何 Resume？
如何处理 duplicate / conflict / crash / timeout？
如何保证 replay 不重复同一业务 effect？
如何管理外部发送与 Clinical State 的一致性？
如何把当前 fixed workflow 增量迁移进新 Runtime？
```

Phase 9 不重新定义 Clinical Risk、Safety Gate、Clinical Readiness、Delivery Readiness、Unit、Capability、Deterministic Policy 或 Contract 语义。

---

# 2. 当前真实技术基线

当前 `main` 真实主链仍是：

```text
Frontend
→ DiagnosisController
→ DiagnosisOrchestrationService
→ CDPManager
→ DiagnosisWorkflowOrchestrator
→ legacy Python / Java clinical services
```

即：

```text
Java fixed workflow + legacy clinical services + CDP
```

不是 Enterprise Runtime 主控。

当前资产处置：

```text
CDP aggregate/versioning/locking       KEEP + REFACTOR
StateCommitter                        REUSE_FOUNDATION + ADAPT
Python Runtime                        REUSE_FOUNDATION + ADAPT
Model Runtime                         REUSE_FOUNDATION + ADAPT
Execution Trace                       REUSE_FOUNDATION + ADAPT
Clinical Parsing/Dialog/Risk/DDx      REFACTOR / ADAPT
Legacy LLM                            REPLACE / REMOVE
```

当前明确缺失：

```text
Clinical Runtime 主控
Unit-level deterministic scheduler
Durable Clinical Resume
Business Event durable intake/dedupe ledger
StateCommitter → authoritative Clinical CDP adapter
外部副作用的 crash-safe delivery boundary
fixed 5-step → state-driven Unit routing 的迁移层
```

现有 Python Runtime 是 deterministic、engine-neutral、single Tool invocation foundation；它不是 Clinical Workflow Runtime。

---

# 3. Phase 9 核心不变量

```text
RUNTIME-INV-01 Runtime != Clinical Truth Owner
RUNTIME-INV-02 Clinical State/CDP != Runtime Checkpoint
RUNTIME-INV-03 Trace != Clinical Truth
RUNTIME-INV-04 Capability Result cannot bypass Business Owner/Policy/G2
RUNTIME-INV-05 Scheduler routes from committed state, not uncommitted candidate
RUNTIME-INV-06 Business Resume validity != Runtime Resume compatibility
RUNTIME-INV-07 stale/missing checkpoint alone cannot invalidate a valid business event
RUNTIME-INV-08 replay cannot apply the same intended business effect twice
RUNTIME-INV-09 one accepted event may legitimately cause multiple distinct governed effects
RUNTIME-INV-10 same event_id transport replay maps to the same canonical event; it is not a second business event
RUNTIME-INV-11 Business Resume DUPLICATE itself produces zero new clinical effects
RUNTIME-INV-12 commit conflict cannot be solved by blind stale replay
RUNTIME-INV-13 safety-sensitive failure cannot become ordinary clinical continuation
RUNTIME-INV-14 Runtime retry cannot bypass idempotency/version/safety policy
RUNTIME-INV-15 one Consultation cannot have two authoritative clinical writers
RUNTIME-INV-16 fixed 5-step workflow is migration source, not target truth
RUNTIME-INV-17 framework choice cannot redefine frozen Unit/Capability/Contract semantics
RUNTIME-INV-18 same Consultation cannot silently switch bound clinical semantics mid-flight
RUNTIME-INV-19 checkpoint loss after clinical commit must be recoverable from authoritative state
RUNTIME-INV-20 Runtime failure != business negative result
RUNTIME-INV-21 Thread/Run completion != Consultation completion
RUNTIME-INV-22 external send success != Clinical State commit; both require durable correlation/reconciliation
```

一个被业务接受的 `USER_ANSWER` 可以合法导致：

```text
patient fact commit
→ risk recomputation commit
→ safety gate commit
→ readiness commit
```

这些是不同 intended effects。禁止的是 replay 再次应用同一 effect，而不是强迫整个 canonical event 只能有一次 commit。

---

# 4. Runtime 总体逻辑架构

以下是逻辑职责，不等于微服务边界：

```text
API / Application Ingress
        ↓
Business Event Intake / Ledger
        ↓
Clinical Run Coordinator
        ↓
Unit Scheduler / Transition Engine
        ├────────→ Business Owner / D01-D10 Policy Host
        ├────────→ Capability Invocation Gateway → C01-C06 / P03 / P04
        └────────→ Delivery Side-effect Coordinator
                              ↓
                Decision / accepted business effect
                              ↓
                    K09 State Change Proposal
                              ↓
          P01 State Governance / Clinical CDP Adapter
                              ↓
                   Clinical State Version n+1

Cross-cutting:
P02 Durable Clinical Resume
P05 Trace / Audit
P06 Scope / Capability / Version Binding
Runtime persistence / Outbox / delivery receipt
```

正式业务链保持：

```text
Capability Result
→ Business Owner / Deterministic Policy interprets
→ Deterministic Decision / accepted business effect
→ State Change Proposal
→ G2/P01
→ Commit Result
→ New Clinical State Version
```

Runtime 只负责执行、调度、挂起、恢复、超时、重试控制、checkpoint、并发协调、版本绑定和 trace 关联。

---

# 5. 逻辑组件职责

## 5.1 API / Application Ingress

当前可继续由既有 Java Controller/Application Service 承接。

职责：接收 Start/Answer/Correction/Cancel，建立 identity/consultation/correlation context，转换 K02 Business Event，查询业务结果。

禁止 Controller/Frontend：

```text
异常时生成“正常/低风险”临床默认值
直接推进 fixed step
绕过 K09/G2 写 Clinical Truth
```

## 5.2 Business Event Intake / Ledger

首先建立 **canonical event identity**。

```text
第一次收到 event_id E
→ durable canonical event E

相同 event_id E 的 HTTP/消息重发
→ transport replay
→ 映射回同一个 canonical event E
→ 不创建第二个 Business Event
→ 不重新产生第二份 Business Resume Decision
```

职责：

- durable 记录 `event_id / idempotency_key`；
- 识别 transport/request replay；
- 记录 canonical event 与 business decision、effect、proposal、commit 的引用关系；
- 记录 canonical event 当前处理状态，以支持 crash/replay reconciliation。

它只能记录：

```text
RECEIVED identity
processing correlation
business decision refs
effect refs
commit/no-effect refs
```

它不能自行宣布业务 `ACCEPTED / DUPLICATE / EXPIRED / REJECTED / APPLIED`。

这些业务状态只能来自 Phase 8 定义的 Business Resume/Event Decision 与正式业务链。

尤其：

```text
transport replay of same event_id
!= Business Resume DUPLICATE automatically
```

前者是同一个 canonical event 的请求重放；后者是业务裁决，且按 Phase 8 冻结语义不得产生新的临床 effect。

## 5.3 Clinical Run Coordinator

职责：为 canonical event 或合法 internal continuation 建立/恢复 Run，关联 consultation/thread/run/clinical-state-version，驱动 bounded execution，协调 Scheduler/Policy/Capability/State Governance/Delivery Coordinator。

在 WAIT、terminal、controlled failure 或当前已无 eligible Unit 时结束 Run。

它不拥有 Consultation Lifecycle 的业务值。

## 5.4 Unit Scheduler / Transition Engine

输入：

```text
committed Clinical State
+ Consultation Lifecycle
+ Clinical Risk / Safety Gate / Clinical Readiness / Delivery Readiness
+ canonical accepted event context
+ pending interaction refs
+ deterministic decision refs
+ version binding
```

输出：`next eligible Unit / WAIT / no-progress / terminal execution intent`。

Scheduler 不重写业务规则；规则来自 Phase 3～7 冻结状态与 D01-D10。

## 5.5 Business Owner / Policy Host

承载 D01-D10、Business Owner interpretation、Deterministic Decision，以及完成业务解释后的 K09 Proposal production。

Owner 保持：

```text
F4 → Clinical Risk
G4 → Safety Gate
F7 → Delivery Readiness
G2 → Clinical State Version
Runtime → Thread / Run / Checkpoint
```

## 5.6 Capability Invocation Gateway

根据 P06 binding 调用批准 C01-C06，适配当前 Python/Java clinical services、Model Runtime、KG/RAG，管理执行级 timeout/cancellation/correlation，并分离旧 ToolResult execution status 与业务 Capability Result。

Capability 不得直接 commit Clinical State。

## 5.7 P01 State Governance / Clinical CDP Adapter

目标：

```text
StateCommitter foundation
+
CDP aggregate/version/locking
→ 唯一正式 Clinical State 写入边界
```

职责：authorization、source/field/consent validation、base version validation、typed proposal validation、atomic commit、audit/evidence refs、Clinical State Version advance。

迁移目标：

```text
CDP aggregate = KEEP
uncontrolled direct CDP write = REMOVE incrementally
StateCommitter repository commit = ADAPT to authoritative Clinical CDP commit
```

## 5.8 P02 Durable Clinical Resume

负责 Thread、Run、Checkpoint、interrupt/wait execution context、pending refs、runtime expiry、retry/repair metadata、resume compatibility。

不拥有 Business Resume validity。

## 5.9 Delivery Side-effect Coordinator

负责 Question/Result 等外部发送的 durable delivery intent、idempotency key、transport attempt、transport receipt、reconciliation。

它不拥有 Question lifecycle、Consultation lifecycle 或 Delivery Readiness，只把可靠 transport outcome 提供给相应 Business Owner/Policy。

---

# 6. Runtime 核心对象

## 6.1 Consultation

Phase 3 业务对象，不属于 Runtime ownership。

## 6.2 Thread

Thread 表示 Consultation 的 durable Clinical execution lineage。

```text
Thread state != Consultation state
```

V1 同一 Consultation 只能有一个能够驱动正式 Clinical State 写入的权威 orchestration lineage。技术上可以存在 child/background execution，但不能成为第二个 Clinical Truth writer。

## 6.3 Run

Run 是对一个 canonical event 或一次合法 internal continuation 的有限执行区间。

来源可包括：

```text
START_CONSULTATION
USER_ANSWER
CORRECTION
CANCEL
expiry/system event
delivery confirmation
approved internal continuation
```

一个 accepted canonical event 可在同一或多个恢复后的 Run 中触发多个不同 Unit/effect。

Run 结束于：

```text
WAIT boundary
terminal business state
controlled failure/safe stop
canonical event 当前可执行 consequences 已收敛
no further eligible Unit
```

不能用“event 已产生一个 effect”作为 Run 结束条件。

## 6.4 Checkpoint

Checkpoint 是执行恢复快照，不是临床事实快照。

至少表达/引用：

```text
checkpoint_id
thread_id
run_id
consultation_id
based_on_clinical_state_version
execution_cursor / next_business_intent
pending_event_ref / pending_interaction_ref
bound_scope_version
bound_capability_set_version
bound_policy_versions
bound_model/prompt refs when relevant
runtime_schema_version
trace refs
created_at
integrity metadata
```

## 6.5 Pending Interaction

业务 Question 状态来自 governed Clinical State/K06。Runtime 只保存 pending question ref、expected consultation/state context、expiry 与 correlation refs。

---

# 7. Canonical Event 与 Effect 幂等模型

## 7.1 Event identity != Effect identity

```text
Canonical Event ID
= 一个唯一业务事件身份

Transport Replay
= 同一 event_id 的再次投递，不产生新的 canonical event

Effect ID / effect idempotency key
= canonical event 在某 Unit/Owner/Decision 下的一个预期正式效果身份
```

Effect identity 至少语义绑定：

```text
event_id
+ effect_type / unit_id / owner context
+ stable business target
+ version/binding context when required
```

具体编码留给 Contract Spec/实现。

## 7.2 exactly-once 的正确目标

Runtime 内部允许：

```text
at-least-once execution / replay
```

临床侧要求：

```text
same intended effect
→ at most once formal application
```

不能错误写成：

```text
same event
→ only one Clinical State commit
```

## 7.3 Canonical Event Processing Ledger

Ledger 必须回答：

```text
canonical event 是否存在？
其 Business Decision 是什么？
若 ACCEPTED，处理是否仍 IN_PROGRESS / 已 APPLIED / 已受控终止？
已形成哪些 intended effects？
哪些 effects 已 COMMITTED / governed NO_EFFECT？
哪些尚未完成或处于 conflict/failure？
```

同一 event_id 的 transport replay：

```text
如果 canonical event 仍在 ACCEPTED/processing
→ 恢复/附着到原 canonical event 的处理
→ 不创建第二个业务 decision

如果 canonical event 已 APPLIED/terminal
→ 返回已有 outcome
→ 0 新 clinical effects
```

若收到 **新的 event_id**，即使 payload 相似，也必须经过正式 Business Resume/Event Validation；只有该业务决策可以返回 `DUPLICATE`。一旦 `DUPLICATE`，该新 business event 自身产生 0 新 clinical effects。

---

# 8. 标准 Run 执行协议

```text
1. Receive K02 input / transport request
2. resolve-or-create canonical Event identity
3. 若只是同 event_id replay → attach/resume original processing，不生成第二个 business decision
4. Load authoritative Clinical State
5. 对新 canonical event 执行业务 applicability/resume validation
6. persist Business Decision ref
7. 非 ACCEPTED event 按其业务决策收敛；不得产生新的临床 effect
8. ACCEPTED event → bind scope/capability/policy/runtime compatibility context
9. Open/resume Thread and create Run
10. Reconcile already-applied effects for this canonical event
11. Scheduler 从 committed state 选择 next eligible Unit
12. 执行 deterministic logic / approved Capability
13. Business Owner/Resolver interprets
14. 形成 Decision / accepted intended effect
15. 为该 intended effect 形成独立 idempotency identity
16. 如需 state change → K09 Proposal → G2/P01 commit
17. 处理 Commit Result
18. reload authoritative Clinical State
19. 继续下一 eligible consequence，直到 WAIT/terminal/failure/no-progress
20. 在 durable boundary 写 Checkpoint
21. canonical event 达到定义的 business completion 后才记录 APPLIED/terminal outcome
22. 全程关联 Trace/Audit refs
```

只有 `COMMITTED` 或语义明确的 governed `NO_EFFECT` 才能认为一个 intended effect 已收敛。

`REJECTED / CONFLICT / FAILED` 不能按“effect 已完成”继续。

---

# 9. Unit 路由模型

V1 不使用 `current_step = 1..5` 或固定 completeness threshold 作为系统真源。

主要路由：

```text
terminal/cancel/expire
→ correction/invalidation consequences
→ risk/safety preemption
→ readiness
   ├─ NEEDS_CLARIFICATION / CAN_ASK_MORE → U06
   ├─ READY_FOR_CLINICAL_ANALYSIS → U08 → U09 → U05
   ├─ NEEDS_OFFLINE_EVIDENCE → U10
   ├─ OUT_OF_SCOPE → U11
   └─ NO_RELIABLE_DIRECTION → U11
→ normal delivery → U12
```

普通事实主干：

```text
committed Facts
→ U03 Risk
→ U04 Safety
→ U05 Readiness
```

U14 failure routing 可抢占普通路径。BL-04 ↔ BL-05 只有存在真实 decision value 且 D03/D04 允许时才循环。

---

# 10. WAIT / Resume 协议

## 10.1 Business Resume 必须先于 Runtime Resume

对一个 **新的 canonical USER_ANSWER event**：

```text
USER_ANSWER
→ Business Resume Event Decision
→ ACCEPTED / DUPLICATE / EXPIRED / REJECTED
→ 只有 ACCEPTED 才能产生该 event 的新业务 effects
→ Runtime resume / rehydrate
```

Runtime 不能用 checkpoint existence 判断回答是否合法。

## 10.2 stale/missing checkpoint

若：

```text
canonical event Business Resume = ACCEPTED
checkpoint = MISSING / INCOMPATIBLE
```

优先从：

```text
authoritative Clinical State
+ accepted canonical event
+ pending interaction refs
+ bound version context
+ event/effect ledger
```

重建执行上下文。

无法安全重建则进入 U14/D07；不能改写成用户回答非法。

## 10.3 同 event_id 重放与业务 DUPLICATE 必须分开

```text
Case A: same event_id transport replay
→ 回到原 canonical event
→ 如果原 event 已 ACCEPTED 但处理中断，继续原 event 未完成的合法 consequences
→ 不是新的 Business Resume DUPLICATE

Case B: new event_id，经业务规则判断 DUPLICATE
→ 该新 event 0 新 clinical effects
→ 返回/引用已有业务结果
```

这保证同时满足：

```text
Business Resume DUPLICATE → no new clinical effect
AND
crash/retry can continue an already-ACCEPTED canonical event
```

---

# 11. Question/Delivery 外部副作用一致性

Phase 8 已冻结：只有 Question 真正 `DELIVERED_TO_USER` 后，Consultation 才能进入 `WAITING_USER`。

因此不能：

```text
send question → assume WAITING_USER
```

也不能：

```text
commit WAITING_USER → 问题尚未真正发送
```

V1 durable side-effect protocol：

```text
A. U06 选择 Question
B. G2 commit Question=SELECTED
C. 基于已 committed SELECTED question 建立 durable delivery intent
D. intent 获得稳定 delivery_id + idempotency key
E. transport 执行发送
F. durable transport receipt / confirmed outcome
G. delivery-confirmation canonical/internal event or continuation
H. Business Owner 根据 confirmed outcome 形成正式业务效果
I. G2 原子提交：Question=DELIVERED_TO_USER + Consultation=WAITING_USER
J. checkpoint at WAIT boundary
```

若 B 后 crash、C 尚未建立：Runtime 可从 committed `SELECTED` 且未 delivered 的 Question 重建同一稳定 delivery intent，不能重新选另一问题。

若 E/F 后 crash：通过相同 `delivery_id/idempotency key` 查询 transport outcome 或安全重试，不能重复发送。

若用户实际已收到问题但 H/I 暂时失败：进入 reconciliation/repair，不允许生成另一问题或假装未发送；最终无法修复则 U14 + 完整审计。

Normal Delivery / Safe Exit 同样采用 durable delivery principle，但 F7 的 Delivery Readiness Owner 不转移给 Delivery Coordinator。

---

# 12. 并发、Conflict 与单写者

V1 同一 Consultation 任一时刻只有一个正式 Clinical State 主写路径。

lease/mutex/queue partition/serialized commands 只能作为 Runtime 协调；最终还必须由：

```text
base_clinical_state_version
+ StateCommitter CAS/conflict validation
```

保护。

遇到 `CONFLICT`：

```text
reload authoritative Clinical State
→ reconcile intended effect 是否已经存在
→ 判断当前 event/effect 是否仍适用
→ 必要时重新运行 Owner/Policy interpretation
→ 形成基于新版本的新 Proposal
→ 再 commit
```

禁止旧 Proposal 盲目重试。若新版本改变安全/readiness 前提，必须重走对应路由。

---

# 13. Checkpoint 与 Crash Recovery

Checkpoint 写在明确 durable boundary：

```text
canonical event identity durable
formal state commit completed
entered WAIT
external side-effect outcome durable
run terminal/no-progress
```

## 13.1 crash after commit / before checkpoint

```text
Clinical State Vn → commit Vn+1 → crash → checkpoint still Vn
```

恢复：

```text
load Vn+1
→ use event/effect ledger + commit refs
→ recognize effect already applied
→ rebuild cursor
→ continue remaining consequences
```

## 13.2 crash after Capability / before Proposal

Capability output 不是 Clinical Truth。恢复后按 policy 重新调用/repair/retry，或 U14。

## 13.3 crash during delivery

依赖 stable `delivery_id/idempotency key` + durable intent/receipt reconciliation，禁止无条件重发。

---

# 14. Failure 与 Retry

必须分开：

```text
A. execution/tool failure
B. capability business_status + reason_code + retryable
C. D07 business failure routing decision
```

旧 ToolResult 只作为工程基础，不能成为新的业务状态真源。

Retry 受：

```text
retryable
attempt budget
time budget
idempotency
clinical-state version
safety impact
fallback policy
D07
```

约束，Capability/Runtime 均不得无限自主重试。

必须保持：

```text
risk service timeout != NO_HIGH_RISK_SIGNAL
DDx failure != empty normal DDx
Safety failure != SAFE
Capability SUCCESS + Commit FAILED != Unit success
```

高风险依赖不可用时，普通 clinical continuation 禁止。

---

# 15. Version Binding

Consultation 至少绑定：

```text
scope_version
capability_set_version
contract compatibility context
```

Run 至少可追溯：

```text
runtime_version
policy/rule versions
capability versions
model/prompt versions when invoked
knowledge/evidence version when relevant
contract/schema version
```

Resume 校验 checkpoint referenced Clinical State Version、runtime schema compatibility、bound scope/capability/policy compatibility、pending interaction validity。

Checkpoint 不兼容时优先从 authoritative state 重建。

进行中的 Consultation 默认保持已绑定临床语义版本；除非显式、可审计 migration policy，否则不得静默升级。

---

# 16. 逻辑持久化职责

Phase 9 冻结职责，不冻结数据库产品。

```text
Clinical State Store
  governed CDP + version history

Canonical Event / Effect Ledger
  canonical event identity + transport replay mapping
  + business decision refs + effect identities + commit/no-effect refs

Runtime Store
  Thread + Run + Checkpoint + retry/expiry/execution cursor

Delivery/Outbox Store
  delivery intent + idempotency + transport receipt/reconciliation

Trace/Audit Store
  execution/audit refs

Version/Registry Store
  scope/capability/policy/model/prompt/knowledge/contract metadata
```

Runtime Store 不得成为第二份 Clinical Truth store。

---

# 17. Trace / Observability 关联

至少贯穿：

```text
consultation_id
clinical_state_version
event_id
effect_id
thread_id
run_id
unit_id
capability_call_id
decision_id
proposal_id
commit_result_ref
checkpoint_id
delivery_id
trace_id
```

```text
Trace = what happened
Audit = governed action evidence
Clinical State = business truth
```

默认不复制完整 PHI；优先记录版本、状态、引用、reason code 与必要摘要。

---

# 18. Brownfield 资产映射

| 当前资产 | Phase 9 定位 | 处置 | 关键变化 |
|---|---|---|---|
| DiagnosisController | Ingress | KEEP + ADAPT | 从驱动旧流程转为 Business Event/Query façade |
| DiagnosisOrchestrationService | legacy orchestration seam | REFACTOR | 逐步让出 fixed workflow 主控 |
| CDPManager | Clinical State aggregate adapter | KEEP + REFACTOR | 保留版本/锁/快照，移除无治理直接写 |
| DiagnosisWorkflowOrchestrator | legacy fixed scheduler | REPLACE incrementally | 5-step 不再是系统真源 |
| AgentLoop | historical asset | DO NOT PROMOTE | 只吸收局部资产，不恢复开放式临床主控 |
| StateCommitter | P01 foundation | REUSE_FOUNDATION + ADAPT | 接管 authoritative Clinical CDP write |
| Python Runtime | execution foundation | REUSE_FOUNDATION + ADAPT | 保留 deterministic invocation；上层新增 Clinical Runtime/P02 |
| Model Runtime | P03 foundation | REUSE_FOUNDATION + ADAPT | 受 Capability/Prompt/Version binding 管理 |
| Execution Trace | P05 | REUSE_FOUNDATION + ADAPT | 增加 event/effect/unit/decision/proposal/commit/runtime refs |
| Dialog Redis/memory | cache/execution aid | REMOVE AS TRUTH | 不再作为正式状态源 |
| Frontend Zustand | UI local state | KEEP AS UI LOCAL | 不生成 Clinical Truth |
| Legacy LLM | legacy dependency | REPLACE / REMOVE | 不重新启用为正式临床路径 |

---

# 19. 部署与技术选型原则

Phase 9 不因“先进”自动拆微服务。

V1 首先保证：

```text
one authoritative Clinical Orchestration control path
+
one authoritative State Governance write path
+
approved Capability runtimes
+
durable Runtime/Event/Delivery persistence
+
Trace/Registry
```

Brownfield 初期允许：

```text
Java Ingress/Application
→ Clinical Runtime façade / scheduler
→ existing Python/Java capabilities
→ P01 State Governance
```

语言边界不等于业务 Owner 边界。

本阶段不冻结：

```text
LangGraph / Temporal / Cadence / 自研 scheduler
Kafka / RabbitMQ / DB queue
Redis / PostgreSQL / Mongo 等 store 产品
Kubernetes 服务拆分粒度
sync RPC / async message 最终组合
```

技术选型必须服务于已冻结执行语义，而不是反过来重定义业务架构。

---

# 20. Brownfield 迁移策略

## 20.1 Strangler，不双主控

legacy fixed workflow 与 new Clinical Runtime 可阶段共存，但同一 Consultation 不允许两套主控都产生正式 Clinical State 写入。

```text
旧 Consultation → legacy binding
新 eligible Consultation → new runtime binding
```

不得进行中的 Consultation 静默换主控。

## 20.2 Shadow 边界

允许 shadow calculation / route comparison / capability output / trace。

Shadow 不得 commit Clinical State、不得发送给用户、不得形成第二套 business effect。

## 20.3 推荐迁移顺序

```text
Foundation:
P01 State Governance/CDP Adapter
P02 Durable Clinical Resume
P05 Trace correlation
P06 Version Binding
Canonical Event/Effect Ledger
Delivery side-effect boundary

Slice A:
U01 U02 U03 U04 U05 U06 U07
+ U11 U14 U15 closure

Then:
U08/U09
U10
U12
U13 full correction/invalidation
```

只是迁移设计顺序，不构成 Implementation Authorization。

---

# 21. Slice A Runtime 映射

```text
U01 START
→ canonical event
→ subject/problem extraction
→ D10 scope
→ D01 lifecycle
→ governed commit

U02 input
→ C01 parse/normalize
→ Owner interpretation
→ typed Proposal
→ P01 commit

U03/U04
→ C02 risk evidence
→ D09/F4 Clinical Risk commit
→ D02/G4 Safety Gate commit

U05
→ D03 Clinical Readiness
→ commit

U06
→ C03 question/gap
→ D04 stopping
→ Question SELECTED commit
→ durable delivery intent/send/receipt
→ delivery-confirmation continuation
→ atomically commit Question DELIVERED_TO_USER + Consultation WAITING_USER
→ checkpoint / Run end

U07 new answer
→ resolve canonical event
→ Business Resume validation once
→ ACCEPTED event runtime resume/rehydrate
→ reconcile already-applied effects
→ apply remaining/new consequences idempotently
→ continue U02/U03/U04/U05...

same event_id replay
→ attach to the same canonical event processing
→ never create second Business Resume Decision

U11/U14/U15
→ Safe Exit / Failure / Cancel-Expire closure
```

---

# 22. Runtime correctness 必测场景

```text
RTE-01 same event_id transport replay → same canonical event, no second business decision
RTE-02 one accepted USER_ANSWER legitimately causes multiple distinct governed effects
RTE-03 crash after one of several effects → resume original accepted canonical event, skip completed effect, continue remaining legal consequences
RTE-04 new event_id judged Business DUPLICATE → zero new clinical effects
RTE-05 crash after commit before checkpoint → no repeated intended effect
RTE-06 crash after capability before proposal → no phantom Clinical Truth
RTE-07 stale checkpoint + valid accepted event → rehydrate or controlled U14, not false rejection
RTE-08 stale checkpoint cannot overwrite newer Clinical State
RTE-09 concurrent events → conflict detected, no blind stale replay
RTE-10 risk capability unavailable → ordinary continuation blocked
RTE-11 Safety Gate BLOCKED/UNAVAILABLE → no ordinary DDx/delivery route
RTE-12 capability SUCCESS + commit FAILED → Unit not treated as completed
RTE-13 Question SELECTED commit + crash before outbox intent → rebuild same stable delivery intent
RTE-14 question send success + crash before WAITING commit → reconcile by delivery_id without duplicate question
RTE-15 delivery retry → no duplicate external side effect
RTE-16 CANCELLED/EXPIRED + late answer → zero new clinical effects
RTE-17 correction invalidation → affected downstream result cannot remain silently current
RTE-18 restart while WAITING_USER → pending question resumable
RTE-19 unsupported/failure → not coerced to empty/normal/negative
RTE-20 legacy/new runtime coexist → same Consultation has one authoritative writer
RTE-21 Event Ledger cannot declare clinical ACCEPTED/DUPLICATE by itself
RTE-22 new bound version applies to new Consultation without silent mid-flight semantic switch
```

---

# 23. 与后续阶段边界

Phase 9 冻结：

```text
Runtime ownership
logical component boundaries
Thread/Run/Checkpoint semantics
Canonical Event/Effect idempotency model
Unit scheduling model
WAIT/Resume protocol
external side-effect consistency protocol
State commit/conflict integration
failure/retry control
version binding
logical persistence responsibilities
Brownfield runtime migration strategy
```

留给 Phase 10：Frontend presentation、UI recovery UX、client-side waiting/error/delivery presentation。

留给 Phase 11：完整异常矩阵、安全验证矩阵、Eval/E2E/chaos/resilience acceptance、observability acceptance。

留给 Phase 12：具体 deployment topology、CI/CD、SLO/capacity/autoscaling、migration execution plan、rollback/runbook、legacy retirement、production authorization。

---

# 24. Phase 9 独立审查与修复记录

第一次独立审查：

```text
P9-R01 BLOCKING
错误：把“duplicate event 不得产生重复 effect”收窄为“same event 只能一个 clinical effect”。
修复：拆分 Event/Effect identity；一个 accepted event 可产生多个 distinct governed effects，每个 effect 最多正式应用一次。

P9-R02 BLOCKING
问题：question transport success → WAITING_USER 存在 crash consistency window。
修复：增加 Question SELECTED commit → durable delivery intent → idempotent transport → receipt → confirmation →
Question DELIVERED_TO_USER + Consultation WAITING_USER 原子业务 commit。

P9-R03 REQUIRED
问题：Event Inbox accepted/applied 描述可能被误读为 Inbox 拥有业务裁决。
修复：Ledger 只记录 Business Decision/Commit refs，不拥有 ACCEPTED/DUPLICATE 等业务语义。

P9-R04 REQUIRED
问题：Run 结束条件暗示 event 只有一个 effect。
修复：Run 以当前可执行 consequences 收敛为边界。

P9-R05 REQUIRED
问题：“delivery success”可能与 F7 Delivery Readiness 混淆。
修复：独立 Delivery Side-effect Coordinator，只负责 transport outcome。
```

第二次独立复核新增：

```text
P9-R06 BLOCKING
问题：把 same-event retry 与 Business Resume DUPLICATE 混合，会违反 Phase 8：DUPLICATE 不得产生新 clinical effect。
修复：引入 canonical event identity：同 event_id transport replay 回到原 canonical event，不生成第二个 Business Decision；
只有新 canonical event 经业务验证才可能得到 DUPLICATE，且 DUPLICATE 自身 0 新 clinical effects。

P9-R07 REQUIRED
问题：Question SELECTED 与 outbox intent 的 durable 边界不够明确。
修复：先正式 commit Question=SELECTED，再从 committed SELECTED 恢复/建立 stable delivery intent；
即使两者之间 crash，也不得重新选择另一 Question。
```

当前：

```text
P9-R01 CLOSED
P9-R02 CLOSED
P9-R03 CLOSED
P9-R04 CLOSED
P9-R05 CLOSED
P9-R06 CLOSED
P9-R07 CLOSED
Independent final re-review = PENDING
Phase 9 = NOT FROZEN
```

---

# 25. 冻结前检查清单

- [x] Runtime 与 Clinical State Owner 边界无歧义；
- [x] Thread / Run / Checkpoint / Consultation 语义分离；
- [x] Unit 调度未重新引入 fixed completeness / 5-step truth；
- [x] Capability → Decision → Proposal → Commit 没有被 Runtime 绕过；
- [x] Business Resume 与 Runtime Resume 拆分；
- [x] canonical event / transport replay / Business DUPLICATE 三者区分；
- [x] Event identity 与 Effect idempotency 正确拆分；
- [x] partial replay / crash / stale checkpoint / conflict 有明确语义；
- [x] Question/Delivery 外部副作用具有 durable reconciliation 边界；
- [x] safety failure 不存在 fail-open；
- [x] one-consultation-one-authoritative-writer 原则明确；
- [x] P01/P02/P05/P06 与现有资产处置明确；
- [x] Brownfield 迁移不存在 dual-write Clinical Truth；
- [x] Slice A 形成 Start → Facts → Risk → Safety → Readiness → Ask → Wait → Resume，并含 Safe Exit/Failure/Cancel closure；
- [x] 未提前冻结 Phase 10～12；
- [x] 未把框架选型当成架构本身；
- [ ] final independent re-review 无新的 BLOCKING/REQUIRED 问题。

当前状态：

```text
Phase 9 design = DRAFT COMPLETE
Independent review findings = REMEDIATED
Final independent re-review = PENDING
Phase 9 = NOT FROZEN
Implementation Authorization = NOT IMPLIED
```
