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

Phase 9 不重新定义：Clinical Risk、Safety Gate、Clinical Readiness、Delivery Readiness、Unit、Capability、Deterministic Policy 或 Contract 语义。

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
RUNTIME-INV-09 one event may legitimately cause multiple distinct governed effects
RUNTIME-INV-10 commit conflict cannot be solved by blind stale replay
RUNTIME-INV-11 safety-sensitive failure cannot become ordinary clinical continuation
RUNTIME-INV-12 Runtime retry cannot bypass idempotency/version/safety policy
RUNTIME-INV-13 one Consultation cannot have two authoritative clinical writers
RUNTIME-INV-14 fixed 5-step workflow is migration source, not target truth
RUNTIME-INV-15 framework choice cannot redefine frozen Unit/Capability/Contract semantics
RUNTIME-INV-16 same Consultation cannot silently switch bound clinical semantics mid-flight
RUNTIME-INV-17 checkpoint loss after clinical commit must be recoverable from authoritative state
RUNTIME-INV-18 Runtime failure != business negative result
RUNTIME-INV-19 Thread/Run completion != Consultation completion
RUNTIME-INV-20 external send success != Clinical State commit; both require durable correlation/reconciliation
```

其中 RUNTIME-INV-08 与 RUNTIME-INV-09 必须同时成立。例如一个 `USER_ANSWER` 可以合法导致：

```text
patient fact commit
→ risk recomputation commit
→ safety gate commit
→ readiness commit
```

这些是不同 effect。禁止的是 replay 再次重复其中同一个 effect，而不是强迫整个 event 只能产生一次 commit。

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

职责：

- 接收 Start / Answer / Correction / Cancel 等输入；
- 建立 identity / consultation / correlation context；
- 转成 K02 Business Event；
- 查询业务结果。

禁止 Controller/Frontend：

```text
异常时生成“正常/低风险”临床默认值
直接推进 fixed step
绕过 K09/G2 写 Clinical Truth
```

## 5.2 Business Event Intake / Ledger

职责：

- durable 记录 `event_id / idempotency_key`；
- 识别 transport/request replay；
- 记录 event 与后续 business decision、effect、proposal、commit 的引用关系；
- 支持 crash/replay reconciliation。

它只保存：

```text
RECEIVED identity
processing correlation
business decision refs
applied effect refs
```

它不能自行宣布业务 `ACCEPTED / DUPLICATE / EXPIRED / REJECTED / APPLIED`。这些状态来自 Phase 8 的 Business Resume/Event Decision 与正式 commit 结果；Ledger 只能引用/记录。

## 5.3 Clinical Run Coordinator

职责：

- 为一个可处理 event 建立/恢复 Run；
- 关联 consultation/thread/run/clinical-state-version；
- 驱动 bounded execution；
- 协调 Scheduler、Policy、Capability、State Governance、Delivery Coordinator；
- 在 WAIT、terminal、controlled failure 或当前已无 eligible Unit 时结束 Run。

它不拥有 Consultation Lifecycle 的业务值。

## 5.4 Unit Scheduler / Transition Engine

输入：

```text
committed Clinical State
+ Consultation Lifecycle
+ Clinical Risk / Safety Gate / Clinical Readiness / Delivery Readiness
+ accepted event context
+ pending interaction refs
+ deterministic decision refs
+ version binding
```

输出：

```text
next eligible Unit
WAIT execution intent
no-progress/terminal execution intent
```

Scheduler 不重写业务规则；规则来自 Phase 3～7 冻结状态与 D01-D10。

## 5.5 Business Owner / Policy Host

承载：

```text
D01-D10
Business Owner interpretation
Deterministic Decision
K09 Proposal production after interpretation
```

Owner 保持：

```text
F4 → Clinical Risk
G4 → Safety Gate
F7 → Delivery Readiness
G2 → Clinical State Version
Runtime → Thread / Run / Checkpoint
```

## 5.6 Capability Invocation Gateway

职责：

- 根据 P06 binding 调用批准的 C01-C06；
- 适配当前 Python/Java clinical services、Model Runtime、KG/RAG；
- 管理执行级 timeout/cancellation/correlation；
- 分离旧 ToolResult execution status 与业务 Capability Result；
- 返回结构化 result。

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

负责：

```text
Thread
Run
Checkpoint
interrupt/wait execution context
pending refs
runtime expiry
retry/repair execution metadata
resume compatibility
```

不拥有 Business Resume validity。

## 5.9 Delivery Side-effect Coordinator

负责 Question/Result 等外部发送的：

```text
durable delivery intent
idempotency key
transport attempt
transport receipt
reconciliation
```

它不拥有 Question lifecycle、Consultation lifecycle 或 Delivery Readiness，只把可靠 transport outcome 提供给相应 Business Owner/Policy 形成正式业务状态变化。

---

# 6. Runtime 核心对象

## 6.1 Consultation

Phase 3 业务对象，不属于 Runtime ownership。

## 6.2 Thread

Thread 表示 Consultation 的 durable Clinical execution lineage。

```text
Thread state != Consultation state
```

V1 同一 Consultation 只能有一个能够驱动正式 Clinical State 写入的权威 orchestration lineage。技术上可以存在 child/background execution，但它们不能成为第二个 Clinical Truth writer。

## 6.3 Run

Run 是对一个 event 或一次合法 continuation 的有限执行区间。

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

一个 event 可在同一 Run 中触发多个不同 Unit/effect。

Run 结束于：

```text
WAIT boundary
terminal business state
controlled failure/safe stop
该 event 当前所有可执行 consequences 已收敛
no further eligible Unit
```

不能用“一个 event 已产生一个 effect”作为 Run 结束条件。

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

业务 Question 状态来自 governed Clinical State/K06。Runtime 只引用：

```text
pending_question_id
expected consultation/state context
expiry
correlation refs
```

---

# 7. Event 与 Effect 幂等模型

## 7.1 Event identity != Effect identity

必须显式区分：

```text
Event ID
= 外部/业务事件身份

Effect ID / effect idempotency key
= 某 event 在某 Unit/Owner/Decision 下的一个预期正式效果身份
```

推荐 effect idempotency 语义至少绑定：

```text
event_id
+ effect_type / unit_id / owner context
+ stable business target
+ version/binding context when required
```

具体编码格式留给 Contract Spec/实现，不在此冻结。

## 7.2 正确的 exactly-once 目标

Runtime 内部允许：

```text
at-least-once processing / replay
```

临床侧要求：

```text
same intended effect
→ at most once formal application
```

而不是：

```text
same event
→ only one Clinical State commit
```

## 7.3 Event replay ledger

Ledger 必须能回答：

```text
这个 event 是否见过？
它已形成哪些 business decisions？
哪些 intended effects 已经 COMMITTED / NO_EFFECT？
哪些 effect 尚未完成或处于 conflict/failure？
```

这样 crash 后才能继续未完成 consequences，而不是因为“event seen”就错误地停止，也不是从头重复所有 effect。

---

# 8. 标准 Run 执行协议

```text
1. Receive K02 Business Event
2. Persist/lookup Event identity
3. Load authoritative Clinical State
4. 执行业务 applicability/resume validation（若该 event 需要）
5. 记录 business decision ref，不由 Ledger 自行裁决
6. Bind scope/capability/policy/runtime compatibility context
7. Open/resume Thread and create Run
8. Reconcile already-applied effects for this event
9. Scheduler 从 committed state 选择 next eligible Unit
10. 执行 deterministic logic / approved Capability
11. Business Owner/Resolver interprets
12. 形成 Decision / accepted business effect
13. 为该 intended effect 形成独立 idempotency identity
14. 如需 state change → K09 Proposal → G2/P01 commit
15. 处理 Commit Result
16. reload authoritative Clinical State
17. 继续下一 eligible consequence，直到 WAIT/terminal/failure/no-progress
18. 在 durable boundary 写 Checkpoint
19. 全程关联 Trace/Audit refs
```

只有：

```text
COMMITTED
或语义明确的 governed NO_EFFECT
```

才能认为该 intended effect 已收敛。

`REJECTED / CONFLICT / FAILED` 不能按“effect 已完成”继续。

---

# 9. Unit 路由模型

V1 不使用 `current_step = 1..5` 或固定 completeness threshold 作为系统真源。

主要路由由 committed state 驱动：

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

U14 failure routing 可抢占普通路径。

BL-04 ↔ BL-05 只有存在真实 decision value 且 D03/D04 允许时才循环。

---

# 10. WAIT / Resume 协议

## 10.1 Business Resume 必须先于 Runtime Resume

```text
USER_ANSWER
→ Business Resume Event Decision
→ ACCEPTED / DUPLICATE / EXPIRED / REJECTED
→ 只有业务 ACCEPTED 才允许产生新的业务 effects
→ Runtime resume / rehydrate
```

Runtime 不能用 checkpoint existence 判断用户回答是否合法。

## 10.2 stale/missing checkpoint

若：

```text
Business Resume = ACCEPTED
checkpoint = MISSING / INCOMPATIBLE
```

则优先从：

```text
authoritative Clinical State
+ accepted event
+ pending interaction refs
+ bound version context
+ event/effect ledger
```

重建执行上下文。

无法安全重建时进入 U14/D07；不能把 Runtime failure 改写成用户回答非法。

## 10.3 Duplicate Resume

重复 answer 不得重复产生已应用 effects。

如果第一次 Run 只完成了部分 distinct effects 后 crash，replay 必须：

```text
跳过已经完成的 effect
继续仍合法且未完成的 consequences
```

而不是简单地“看到 event 已存在 → 全部返回 DUPLICATE”。

Business Resume 的 DUPLICATE 语义与 runtime effect reconciliation 必须可追溯地对齐。

---

# 11. Question/Delivery 外部副作用一致性

Phase 8 已冻结：只有 Question 真正 `DELIVERED_TO_USER` 后，Consultation 才能进入 `WAITING_USER`。

因此不能简单写成：

```text
send question
→ assume WAITING_USER
```

也不能：

```text
commit WAITING_USER
→ 尚未真正发送问题
```

V1 采用 durable side-effect protocol：

```text
A. U06 选择 Question
B. commit Question=SELECTED / pending delivery intent（不得提前 WAITING_USER）
C. durable outbox/delivery intent 获得稳定 delivery_id + idempotency key
D. transport 执行发送
E. durable transport receipt / confirmed outcome
F. delivery-confirmation event/run
G. Business Owner 根据 confirmed outcome 形成正式状态效果
H. G2 原子提交：Question=DELIVERED_TO_USER + Consultation=WAITING_USER
I. checkpoint at WAIT boundary
```

如果 D/E 后 crash，恢复必须通过相同 `delivery_id/idempotency key` 查 transport outcome 或安全重试，不能重复发送。

如果外部已成功发送但 G/H 暂时失败：

```text
用户实际已看到问题
但 Clinical State 尚未完成 WAITING transition
```

此时必须进入 reconciliation/repair，不允许重新生成另一问题或假装未发送。最终无法修复时进入 U14，并保留完整审计。

Normal Delivery / Safe Exit 的外部发送也使用同一类 durable delivery principle，但 F7 的 Delivery Readiness Owner 不因此转移给 Delivery Coordinator。

---

# 12. 并发、Conflict 与单写者

## 12.1 单 Consultation 逻辑单写者

V1 要求同一 Consultation 任一时刻只有一个正式 Clinical State 主写路径。

可用：lease、mutex、queue partition、serialized command handling 等协调，但最终一致性不能只依赖锁。

必须同时由：

```text
base_clinical_state_version
+ StateCommitter CAS/conflict validation
```

保护。

## 12.2 Commit Conflict

遇到 `CONFLICT`：

```text
reload authoritative Clinical State
→ reconcile 原 intended effect 是否已经存在
→ 判断当前 event/effect 是否仍适用
→ 必要时重新运行 Owner/Policy interpretation
→ 形成基于新版本的新 Proposal
→ 再 commit
```

禁止把旧 Proposal 盲目重试到成功。

若新版本改变安全/readiness 前提，必须重新经过对应路由。

---

# 13. Checkpoint 与 Crash Recovery

Checkpoint 写在明确 durable boundary：

```text
event identity durable
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

不得重复同一 effect。

## 13.2 crash after Capability / before Proposal

Capability output 不是 Clinical Truth。恢复后可以按 policy 重新调用/repair/retry，或进入 U14。

## 13.3 crash during delivery

依赖 stable `delivery_id/idempotency key` + durable intent/receipt 做 transport reconciliation，禁止无条件重新发送。

---

# 14. Failure 与 Retry

必须分开：

```text
A. execution/tool failure
B. capability business_status + reason_code + retryable
C. D07 business failure routing decision
```

旧 ToolResult 只作为工程基础，不能成为新的业务状态真源。

Retry 必须受：

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

高风险依赖不可用时，普通临床 continuation 禁止。

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

Resume 校验：

```text
checkpoint referenced Clinical State Version
runtime schema compatibility
bound scope/capability/policy compatibility
pending interaction validity
```

checkpoint 不兼容时优先从 authoritative state 重建。

进行中的 Consultation 默认保持已绑定临床语义版本；除非存在显式、可审计 migration policy，否则不得静默升级。

---

# 16. 逻辑持久化职责

Phase 9 冻结职责，不冻结数据库产品。

```text
Clinical State Store
  governed CDP + version history

Business Event / Effect Ledger
  event identity + decision refs + effect identities + commit/no-effect refs

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

本阶段明确不冻结：

```text
LangGraph / Temporal / Cadence / 自研 scheduler
Kafka / RabbitMQ / DB queue
Redis / PostgreSQL / Mongo 等 store 产品
Kubernetes 服务拆分粒度
sync RPC / async message 最终组合
```

技术选型必须服务于上述执行语义，而不是反过来重定义业务架构。

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

允许：shadow calculation / route comparison / capability output / trace。

Shadow：

```text
不得 commit Clinical State
不得发送给用户
不得形成第二套 business effect
```

## 20.3 推荐迁移顺序

```text
Foundation:
P01 State Governance/CDP Adapter
P02 Durable Clinical Resume
P05 Trace correlation
P06 Version Binding
Event/Effect Ledger
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
→ delivery-confirmation effect
→ atomically commit Question DELIVERED_TO_USER + Consultation WAITING_USER
→ checkpoint / Run end

U07
→ Business Resume validation
→ runtime resume/rehydrate
→ reconcile prior effects
→ apply new answer consequences idempotently
→ continue U02/U03/U04/U05...

U11/U14/U15
→ Safe Exit / Failure / Cancel-Expire closure
```

---

# 22. Runtime correctness 必测场景

```text
RTE-01 duplicate USER_ANSWER → no repeated intended effect
RTE-02 one USER_ANSWER legitimately causes multiple distinct governed effects
RTE-03 crash after one of several effects → replay skips completed effect and continues remaining legal consequences
RTE-04 crash after commit before checkpoint → no repeated commit effect
RTE-05 crash after capability before proposal → no phantom Clinical Truth
RTE-06 stale checkpoint + valid answer → rehydrate or controlled U14, not false rejection
RTE-07 stale checkpoint cannot overwrite newer Clinical State
RTE-08 concurrent events → conflict detected, no blind stale replay
RTE-09 risk capability unavailable → ordinary continuation blocked
RTE-10 Safety Gate BLOCKED/UNAVAILABLE → no ordinary DDx/delivery route
RTE-11 capability SUCCESS + commit FAILED → Unit not treated as completed
RTE-12 question send success + crash before WAITING commit → reconcile by delivery_id without duplicate question
RTE-13 delivery retry → no duplicate external side effect
RTE-14 CANCELLED/EXPIRED + late answer → zero new clinical effects
RTE-15 correction invalidation → affected downstream result cannot remain silently current
RTE-16 restart while WAITING_USER → pending question resumable
RTE-17 unsupported/failure → not coerced to empty/normal/negative
RTE-18 legacy/new runtime coexist → same Consultation has one authoritative writer
RTE-19 event ledger cannot declare clinical ACCEPTED/DUPLICATE by itself
RTE-20 new bound version applies to new Consultation without silent mid-flight semantic switch
```

---

# 23. 与后续阶段边界

Phase 9 冻结：

```text
Runtime ownership
logical component boundaries
Thread/Run/Checkpoint semantics
Event/Effect idempotency model
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

第一次独立审查发现：

```text
P9-R01 BLOCKING
初稿把“duplicate event 不得产生重复临床效果”错误收窄为
“same event → at most one formal clinical effect”。
修复：拆分 Event identity 与 Effect identity；允许一个 event 产生多个不同 governed effects，
但每个 intended effect 最多正式应用一次；增加 partial-replay reconciliation。

P9-R02 BLOCKING
初稿对 question transport success → WAITING_USER 的 crash window 描述不足。
修复：增加 durable delivery intent / idempotent transport / receipt / confirmation event /
Question DELIVERED_TO_USER + Consultation WAITING_USER 原子业务 commit 的协议。

P9-R03 REQUIRED
Business Event Inbox 的 accepted/applied 描述可能被误读为 Inbox 拥有业务裁决。
修复：改为 Event/Effect Ledger，只记录 Business Decision / Commit refs，不拥有 ACCEPTED/DUPLICATE 等业务语义。

P9-R04 REQUIRED
Run 结束条件使用“current event effect applied/no-effect”可能错误暗示 event 只有一个 effect。
修复：改为 event 当前可执行 consequences 收敛；支持已完成 effect 跳过、未完成 consequences 继续。

P9-R05 REQUIRED
U06 中“delivery success”容易与 F7 Delivery Readiness 混淆。
修复：单独定义 Delivery Side-effect Coordinator 与 question transport receipt，明确其不拥有 Delivery Readiness。
```

修复后状态：

```text
P9-R01 CLOSED
P9-R02 CLOSED
P9-R03 CLOSED
P9-R04 CLOSED
P9-R05 CLOSED
Independent re-review = PENDING
Phase 9 = NOT FROZEN
```

---

# 25. 冻结前检查清单

- [x] Runtime 与 Clinical State Owner 边界无歧义；
- [x] Thread / Run / Checkpoint / Consultation 语义分离；
- [x] Unit 调度未重新引入 fixed completeness / 5-step truth；
- [x] Capability → Decision → Proposal → Commit 没有被 Runtime 绕过；
- [x] Business Resume 与 Runtime Resume 拆分；
- [x] Event identity 与 Effect idempotency 正确拆分；
- [x] partial replay / duplicate / crash / stale checkpoint / conflict 有明确语义；
- [x] Question/Delivery 外部副作用具有 durable reconciliation 边界；
- [x] safety failure 不存在 fail-open；
- [x] one-consultation-one-authoritative-writer 原则明确；
- [x] P01/P02/P05/P06 与现有资产处置明确；
- [x] Brownfield 迁移不存在 dual-write Clinical Truth；
- [x] Slice A 形成 Start → Facts → Risk → Safety → Readiness → Ask → Wait → Resume，并含 Safe Exit/Failure/Cancel closure；
- [x] 未提前冻结 Phase 10～12；
- [x] 未把框架选型当成架构本身；
- [ ] 独立复核确认上述修复未引入新的 BLOCKING/REQUIRED 问题。

当前状态：

```text
Phase 9 design = DRAFT COMPLETE
Independent review findings = REMEDIATED
Independent re-review = PENDING
Phase 9 = NOT FROZEN
Implementation Authorization = NOT IMPLIED
```
