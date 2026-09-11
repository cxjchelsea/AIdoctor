# AIdoctor Phase 9 — Runtime 与技术架构设计 V1

> 状态：DRAFT COMPLETE / NOT FROZEN
> 适用基线：`main` 当前真实代码 + Phase 1～8 已冻结权威设计
> 目标：在不改变既有业务语义、状态 Owner、Unit、Capability 与 Contract 边界的前提下，定义 V1 临床 Runtime 的执行、等待、恢复、提交、失败、并发、版本绑定与 Brownfield 迁移架构。
> 非目标：本文件不构成 Implementation Authorization；不冻结具体框架、消息队列、数据库、微服务拆分或部署厂商选型；不进入 Phase 10 前端设计。

---

# 1. Phase 9 的问题边界

Phase 9 回答的是：

```text
已经有了：
业务语义
→ 系统状态
→ 业务闭环
→ Unit
→ Capability / deterministic policy
→ Contract

接下来：
谁驱动一次执行？
如何从已提交 Clinical State 决定下一 Unit？
何时等待？
如何恢复？
如何处理 duplicate / conflict / crash / timeout？
如何保证同一事件最多产生一次临床效果？
如何把现有 Java fixed workflow 迁入新 Runtime？
```

Phase 9 不重新回答：

```text
什么是 Clinical Risk
谁拥有 Safety Gate
什么时候应该问问题
什么 DDx 是医学上正确的
Capability 应该返回什么业务语义
Clinical State 应有哪些业务域
```

这些已经由 Phase 1～8 冻结。

---

# 2. 当前真实技术基线

根据 Current State Baseline 与当前 `main`：

```text
Frontend
→ DiagnosisController
→ DiagnosisOrchestrationService
→ CDPManager
→ DiagnosisWorkflowOrchestrator
→ legacy Python / Java clinical services
```

当前真实主链仍是：

```text
Java fixed workflow
+
legacy clinical services
+
CDP
```

而不是 Enterprise Runtime 主控。

现有可复用基础：

```text
CDP aggregate / versioning / locking       KEEP + REFACTOR
StateCommitter                            REUSE_FOUNDATION + ADAPT
Python Runtime                            REUSE_FOUNDATION + ADAPT
Model Runtime                             REUSE_FOUNDATION + ADAPT
Execution Trace                           REUSE_FOUNDATION + ADAPT
Clinical Parsing / Dialog / Risk / DDx    REFACTOR / ADAPT
```

当前明确缺失：

```text
Clinical Runtime 主控
Durable Clinical Resume
Business Event Inbox / durable dedupe boundary
Unit-level deterministic scheduler
正式 Clinical State 与 StateCommitter 的主链接管
旧固定 5-step workflow 的增量迁移机制
```

重要事实：当前 Python Runtime 是 deterministic、engine-neutral、single Tool invocation runtime；它不是 Clinical Workflow Runtime，也没有完整业务 Resume 闭环。

---

# 3. Phase 9 架构总原则

## 3.1 Runtime 不拥有 Clinical Truth

正式链路保持：

```text
Capability Result
→ Business Owner / Deterministic Policy interprets
→ Deterministic Decision / accepted business effect
→ State Change Proposal
→ G2 State Governance
→ Commit Result
→ New Clinical State Version
```

Runtime 只负责：

```text
执行
调度
挂起
恢复
超时
重试控制
checkpoint
并发协调
版本绑定
trace 关联
```

禁止：

```text
Runtime 根据模型结果直接改 Clinical State
Runtime 根据 checkpoint 恢复一份独立 Clinical Truth
Runtime 根据 null / [] / error 猜业务语义
Runtime 自己决定 Clinical Risk / Safety Gate / Readiness / Delivery Readiness
```

## 3.2 Clinical State 是业务真源

```text
Clinical Business Truth = governed Clinical State / CDP
Execution Truth         = Runtime Thread / Run / Checkpoint
Observation Truth       = Trace / Audit
UI Local State          = Frontend local state
```

必须保持：

```text
Clinical State != Runtime Checkpoint
Consultation ACTIVE != Thread RUNNING
Thread COMPLETED != Consultation COMPLETED
Trace != Clinical Truth
```

## 3.3 Runtime 只能从已提交状态继续推进

一个 Unit 即使完成 Capability 调用，如果正式业务效果尚未经 G2 commit，则 Runtime 不得把“候选结果”当成下一 Unit 的正式输入。

标准推进点：

```text
Decision / accepted effect
→ Proposal
→ Commit Result = COMMITTED / governed NO_EFFECT
→ reload / resolve authoritative Clinical State
→ choose next Unit
```

若：

```text
Commit Result = REJECTED / CONFLICT / FAILED
```

则不能按“已完成状态变化”继续执行。

## 3.4 Safety 可抢占普通执行路径

任何普通执行计划都不得绕过：

```text
U03 Risk
→ U04 Safety Gate
```

当安全敏感能力不可用、Safety Gate 为 BLOCKED / UNAVAILABLE，或 D07 判定普通临床路径不得继续时，Runtime 必须停止普通路径并进入受控的 U14 / Safe Exit / terminal 路由。

## 3.5 Runtime 不采用无界自主 Agent Loop

V1 不设计：

```text
Observe → Think → Tool → Think → Tool → ...
```

的开放式自主循环作为临床主控。

V1 使用：

```text
accepted business event
→ bounded Run
→ deterministic Unit routing
→ approved Capability invocation
→ governed commit
→ next deterministic route / WAIT / terminal
```

BL-04 ↔ BL-05 可以循环，但只有 D03/D04/U09 判定仍存在真实决策价值时才允许继续。

---

# 4. V1 Runtime 逻辑架构

V1 定义以下逻辑组件。它们是职责边界，不等价于必须拆成独立微服务。

```text
┌─────────────────────────────────────────────┐
│ API / Application Ingress                  │
│ 当前可由既有 Java Controller/Service 承接  │
└──────────────────┬──────────────────────────┘
                   ↓
┌─────────────────────────────────────────────┐
│ Business Event Intake / Inbox              │
│ event identity / dedupe / ordering context │
└──────────────────┬──────────────────────────┘
                   ↓
┌─────────────────────────────────────────────┐
│ Clinical Run Coordinator                   │
│ Thread / Run / lifecycle coordination      │
└──────────────────┬──────────────────────────┘
                   ↓
┌─────────────────────────────────────────────┐
│ Unit Scheduler / Transition Engine         │
│ 根据 committed state + policies 选择 Unit │
└───────┬─────────────┬─────────────┬─────────┘
        │             │             │
        ↓             ↓             ↓
 Business Owner   D01-D10 Policy   Capability Gateway
 / Interpreter       Host          C01-C06 / P03/P04
        │             │             │
        └─────────────┴──────┬──────┘
                             ↓
                 K09 State Change Proposal
                             ↓
┌─────────────────────────────────────────────┐
│ P01 State Governance / Clinical CDP Adapter│
│ StateCommitter + authoritative CDP write   │
└──────────────────┬──────────────────────────┘
                   ↓
             Clinical State V(n+1)
                   ↓
┌─────────────────────────────────────────────┐
│ P02 Durable Clinical Resume                │
│ Thread / Run / Checkpoint / Pending refs   │
└─────────────────────────────────────────────┘

Cross-cutting:
P05 Trace / Audit
P06 Scope / Capability / Version Binding
Delivery / Outbox boundary
```

---

# 5. 组件职责与禁止权限

## 5.1 API / Application Ingress

职责：

- 接收 Start / Answer / Correction / Cancel 等外部事件；
- 建立身份、consultation、request/correlation context；
- 将输入转换为 K02 Business Event；
- 不直接写 Clinical Truth。

Brownfield 初期可继续由既有 Java Controller / application service 承接。

禁止：

```text
前端或 Controller 根据异常生成“正常/低风险”默认值
Controller 自行推进 fixed step
Controller 绕过 Event/State Governance 直接改正式 Clinical State
```

## 5.2 Business Event Intake / Inbox

职责：

- 记录可识别的 business event；
- 按 `event_id / idempotency_key` 做 durable dedupe；
- 保存 event received / accepted / applied/no-effect 的可追溯关系；
- 为 replay/crash recovery 提供事件入口事实。

它不决定临床语义。

## 5.3 Clinical Run Coordinator

职责：

- 为一次被接受的事件建立或恢复 Run；
- 关联 consultation / thread / run / clinical_state_version；
- 控制一次 bounded execution；
- 协调 Unit Scheduler、Capability、Policy、State Governance；
- 在 WAIT、terminal 或当前 Run 无可执行 Unit 时结束本次 Run。

它不拥有 Consultation Lifecycle 的业务值；D01 / 对应业务 Owner 决定业务迁移，Runtime 执行。

## 5.4 Unit Scheduler / Transition Engine

输入必须以当前 committed Clinical State 为核心：

```text
current Clinical State Version
+ Consultation Lifecycle
+ Risk / Safety / Readiness / Delivery states
+ accepted event context
+ pending interaction refs
+ deterministic policy results
+ bound version context
```

输出是：

```text
next eligible Unit / WAIT / no-op / terminal execution intent
```

Scheduler 不重新发明业务流程规则，规则来自 Phase 3～7 冻结的状态和 D01-D10。

## 5.5 Business Owner / Deterministic Policy Host

职责：

- 承载 D01-D10；
- 执行业务 Owner 对 Capability Result 的解释；
- 生成 Deterministic Decision；
- 在需要正式状态变化时形成 K09 State Change Proposal。

必须保持 Phase 4 Owner：

```text
F4 → Clinical Risk
G4 → Safety Gate
F7 → Delivery Readiness
G2 → Clinical State Version
Runtime → Thread / Run / Checkpoint
```

## 5.6 Capability Invocation Gateway

职责：

- 根据 P06 已批准 binding 调用 C01-C06；
- 适配现有 Python/Java clinical service、Model Runtime、KG/RAG；
- 统一执行级 timeout/cancellation/correlation；
- 将旧 ToolResult 与新的 business capability semantics 分离；
- 返回结构化 Capability Result。

禁止 Capability 自行 commit Clinical State。

## 5.7 P01 State Governance / Clinical CDP Adapter

目标是把现有：

```text
StateCommitter foundation
+
CDP aggregate / versioning / locking
```

收敛为唯一正式临床写入边界。

职责：

- 校验 authorization / source / field permission / consent；
- 校验 `base_clinical_state_version`；
- 校验 proposal schema / precondition；
- 原子提交 typed governed changes；
- 返回 Commit Result；
- 推进正式 Clinical State Version；
- 写 audit/evidence refs。

关键迁移目标：

```text
CDP aggregate = KEEP
uncontrolled direct CDP writes = REMOVE incrementally
StateCommitter mechanical repository commit = ADAPT to authoritative Clinical CDP commit
```

## 5.8 P02 Durable Clinical Resume

P02 是 Phase 9 的核心新增层，负责：

```text
Thread
Run
Checkpoint
Pending interaction refs
interrupt / waiting context
runtime expiry
retry / repair execution metadata
resume compatibility
```

它不拥有 Business Resume validity。

---

# 6. Runtime 核心对象

## 6.1 Consultation

业务对象，由 Phase 3 生命周期管理。不是 Runtime object。

## 6.2 Thread

Thread 表示一个 Consultation 的 durable execution lineage。

它用于把多次 Run 串起来，但：

```text
Thread running/completed
!= Consultation ACTIVE/COMPLETED
```

一个 Consultation V1 原则上拥有一个权威 Clinical Thread lineage；不得同时存在两个都能向同一 Clinical State 写正式效果的主控 Thread。

## 6.3 Run

Run 是对一个已接受事件进行的一次有限执行尝试。

典型来源：

```text
START_CONSULTATION
USER_ANSWER
CORRECTION
CANCEL
expiry/system event
approved internal continuation
```

一个 Run 必须是 bounded 的；它结束于：

```text
WAITING boundary
terminal business state
safe stop
current event effect applied/no-effect
runtime/capability failure routed
no further eligible Unit
```

## 6.4 Checkpoint

Checkpoint 是执行恢复快照，不是临床事实快照。

至少应表达/引用：

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

可以缓存执行所需的最小派生数据，但不得形成与 Clinical State 长期漂移的第二份临床真值。

## 6.5 Pending Interaction

V1 至少需要正式引用：

```text
pending_question_id
question state
expected consultation/state context
expiry
business correlation refs
```

它用于 U06 → WAITING_USER → U07 Resume。

Pending Interaction 的业务状态来自 governed Clinical State / K06；Runtime 仅保存恢复引用和执行上下文。

---

# 7. 一次标准 Run 的执行协议

标准协议：

```text
1. Receive K02 Business Event
2. Durable event identity / dedupe check
3. Load authoritative Clinical State
4. Validate event business applicability when required
5. Bind runtime/scope/capability/policy versions
6. Open or resume Thread, create Run
7. Resolve next eligible Unit from committed state
8. Execute deterministic logic and/or approved Capability
9. Business Owner / Resolver interprets result
10. Produce Decision / accepted business effect
11. If state change required → create K09 Proposal
12. G2/P01 commit
13. Handle Commit Result
14. Reload/advance from committed Clinical State
15. Continue bounded routing, WAIT, safe exit or terminate Run
16. Persist checkpoint at durable execution boundary
17. Trace references are linked throughout
```

禁止：

```text
Capability success
→ scheduler 假定 state 已改变
→ 直接进入下一 Unit
```

必须以 Commit Result 和正式 Clinical State 为准。

---

# 8. Unit 路由模型

V1 Runtime 不使用固定 `current_step = 1..5` 作为系统真源。

路由由正式状态驱动。

基础优先级：

```text
A. terminal / cancel / expire handling
B. accepted correction / invalidation consequences
C. risk & safety preemption
D. required clarification / question wait
E. clinical analysis / DDx
F. post-DDx gap re-evaluation
G. offline evidence route
H. safe exit
I. normal delivery
```

这只是 Runtime 的路由优先级，不改变 U01-U15 的业务语义。

核心主循环：

```text
Clinical Facts committed
→ U03 Risk
→ U04 Safety
→ U05 Readiness

Readiness:
├─ NEEDS_CLARIFICATION / CAN_ASK_MORE → U06 → WAITING_USER
├─ READY_FOR_CLINICAL_ANALYSIS        → U08 → U09 → U05
├─ NEEDS_OFFLINE_EVIDENCE             → U10
├─ OUT_OF_SCOPE                       → U11
└─ NO_RELIABLE_DIRECTION              → U11
```

正常满足交付条件时进入 U12。

任何 U14 failure routing 可抢占当前普通路径。

---

# 9. WAIT / Resume 协议

## 9.1 WAITING_USER 建立

U06 只有在：

```text
Question = DELIVERED_TO_USER
+
对应 lifecycle transition 已经正式 commit
```

后，Consultation 才能进入 `WAITING_USER`。

仅生成 Question Candidate 或渲染文本不等于已经等待用户。

## 9.2 Resume 必须先做 Business validation

严格顺序：

```text
USER_ANSWER event
→ Business Resume Event Decision
→ ACCEPTED / DUPLICATE / EXPIRED / REJECTED
→ 只有 ACCEPTED 才能进入后续业务 effect
→ Runtime Execution Resume
```

Runtime 不得使用：

```text
checkpoint exists
```

来证明用户回答业务上有效。

## 9.3 checkpoint 不兼容不自动否定合法业务事件

当：

```text
Business Resume = ACCEPTED
但 checkpoint = MISSING / INCOMPATIBLE
```

Runtime 应优先：

```text
authoritative Clinical State
+ accepted event
+ pending interaction refs
+ bound version context
```

重新水合执行上下文。

因此：

```text
stale checkpoint
!= stale Clinical Truth
!= user answer automatically rejected
```

若无法安全重建，则进入 U14 / D07；不得把 Runtime 恢复失败改写成“用户回答非法”。

## 9.4 Resume 后事件只允许一次临床效果

合法 answer 的 effect 必须与：

```text
event_id
accepted_event_ref
proposal idempotency_key
commit result
```

建立可追溯链。

重复请求必须收敛到：

```text
DUPLICATE
或 governed NO_EFFECT
```

而不是产生第二个 Clinical State 变化。

---

# 10. 幂等、并发与一致性

## 10.1 语义目标

不要求 Runtime 的每一个内部执行动作“物理 exactly once”。

允许：

```text
at-least-once processing / replay
```

但必须实现：

```text
same business event
→ at most one formal clinical effect
```

通过：

```text
Event identity
+ Business Resume / event applicability
+ Proposal idempotency key
+ StateCommitter idempotency
+ expected/base Clinical State Version
+ atomic commit
```

共同保证。

## 10.2 单 Consultation 逻辑单写者

V1 原则：

```text
同一 Consultation 的正式 Clinical State
在任一时刻只有一个逻辑主写执行路径
```

实现可以使用：

```text
lease / mutex / queue partition / serialized command handling
```

但这些只是 Runtime 协调手段。

最终临床一致性不能只依赖锁，仍必须由：

```text
base_clinical_state_version
+ StateCommitter CAS/conflict validation
```

保护。

## 10.3 Commit Conflict

遇到：

```text
Commit Result = CONFLICT
```

禁止：

```text
对旧 proposal 盲目重试直到成功
```

正确路径：

```text
reload current Clinical State
→ 判断原 event 是否已产生 effect
→ 如未产生，重新执行必要的 Owner/Policy interpretation
→ 生成基于新版本的新 Proposal
→ 再 commit
```

如果 conflict 改变了安全/业务前提，必须重新经过相应安全和 readiness 路由。

---

# 11. Checkpoint 与 crash recovery

## 11.1 checkpoint 的 durable boundary

Checkpoint 应保存于明确的 durable execution boundary，例如：

```text
accepted event durable
state commit completed
entered WAIT
approved external side-effect durable
run terminal
```

不得依靠每个内存步骤都写 checkpoint 来替代正式状态治理。

## 11.2 crash after commit / before checkpoint

场景：

```text
Clinical State Vn → commit → Vn+1
进程 crash
checkpoint 仍指向 Vn
```

恢复时必须：

```text
load authoritative Vn+1
→ 识别 event/proposal effect 已存在
→ checkpoint incompatible/stale
→ rebuild runtime cursor
→ 不再次提交同一 clinical effect
```

## 11.3 crash after capability / before proposal

Capability Result 未形成正式 Proposal/Commit 时，不存在正式 Clinical State effect。

恢复后可以按当前版本重新调用或经 D07 决定 repair/retry，但不能把此前非 durable 的模型输出视为 Clinical Truth。

## 11.4 crash after external delivery side effect

对非幂等外部副作用，需要 durable outbox / delivery idempotency boundary。

必须避免：

```text
内容已发送给用户
→ crash
→ replay
→ 再次发送
```

具体消息基础设施选型不在 Phase 9 冻结。

---

# 12. Failure 与 retry 架构

## 12.1 三层 failure 必须分开

```text
A. execution/tool failure
B. capability business_status / reason_code / retryable
C. business failure routing decision
```

旧 ToolResult：

```text
SUCCEEDED
NO_RESULT
RETRYABLE_FAILURE
NON_RETRYABLE_FAILURE
TIMED_OUT
POLICY_BLOCKED
```

只能作为工程执行基础。

业务 Capability Result 仍按 Phase 7/8 表达：

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

## 12.2 retry 不由 Capability 自治

V1 禁止：

```text
Capability 自己无限重试
Runtime 因“可能成功”无限重试
```

Retry 必须受：

```text
retryable
attempt budget
time budget
idempotency
safety impact
fallback policy
D07 routing
```

约束。

## 12.3 高风险依赖失败

必须保持：

```text
high-risk capability unavailable
→ ordinary clinical continuation prohibited
```

不能把：

```text
risk service timeout
→ no risk
```

或：

```text
DDx failure
→ empty DDx
→ low risk
```

作为 fallback。

## 12.4 Commit failure

```text
Capability SUCCESS
+
Commit FAILED / REJECTED / unresolved CONFLICT
```

表示 Unit 预期的正式状态效果没有完成。

Runtime 不得继续假设 state 已更新。

---

# 13. Version Binding

## 13.1 Consultation-level binding

一个 Consultation 至少绑定：

```text
scope_version
capability_set_version
contract compatibility context
```

禁止在进行中的 Consultation 中静默切换临床语义集合。

## 13.2 Run-level binding

每个 Run 至少可追溯：

```text
runtime_version
policy/rule versions
capability versions
model/prompt versions when invoked
knowledge/evidence version when relevant
contract/schema version
```

## 13.3 checkpoint compatibility

Resume 时校验：

```text
checkpoint clinical state reference
runtime schema compatibility
bound scope/capability/policy compatibility
pending interaction validity
```

兼容：可以直接恢复执行 cursor。

不兼容：优先从 canonical Clinical State 重建；不能静默沿用旧 cached clinical semantics。

## 13.4 版本升级原则

进行中的 Consultation 默认保持其绑定版本，除非存在显式、可审计的 migration policy。

新版本优先作用于新 Consultation。

---

# 14. 数据存储职责

Phase 9 冻结逻辑存储职责，不冻结具体数据库产品。

## 14.1 Clinical State Store

保存/承载 governed Clinical State / CDP 与版本历史。

## 14.2 Business Event / Idempotency Store

保存：

```text
event identity
received/accepted/applied/no-effect relations
idempotency metadata
```

## 14.3 Runtime Store

保存：

```text
Thread
Run
Checkpoint
runtime expiry/retry metadata
execution cursor
pending refs
```

不得成为 Clinical Truth store。

## 14.4 Trace / Audit Store

保存引用化的执行与审计链。

## 14.5 Version / Registry Store

保存：

```text
scope
capability set
policy/rule
model route
prompt
knowledge/evidence
contract/schema
```

版本元数据。

## 14.6 Delivery / Outbox Store

在存在非幂等外部发送时保存 durable delivery intent / outcome，以支持 crash-safe replay。

---

# 15. Trace / Observability 关联模型

至少贯穿以下 correlation identity：

```text
consultation_id
clinical_state_version
event_id
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

原则：

```text
Trace records what happened
Audit records governed actions
Clinical State records business truth
```

Trace 默认不复制完整 PHI payload；优先记录类型、版本、状态、引用、摘要和必要 reason code。

现有 Execution Trace implementation 可保留并接入这些 identity，但 Trace 开启状态、采样、持久化和敏感字段策略需要在 Phase 11/12 继续工程化。

---

# 16. 安全与权限 Runtime 边界

Phase 9 不重新定义 G4，但 Runtime 必须提供执行支撑：

- 每次 Capability invocation 携带最小授权 Context；
- P06 确保只调用当前 Scope / Capability Set 允许的能力；
- StateCommitter/P01 再次验证正式写权限；
- Runtime cancellation/timeout 不能跳过安全后果；
- safety-sensitive failure 必须进入 D07/D02 语义链；
- checkpoint / trace / error 默认最小化 PHI；
- resume token/ref 不得等价为 Clinical authorization。

---

# 17. Brownfield 资产映射

| 当前资产 | Phase 9 定位 | 处置 | 关键变化 |
|---|---|---|---|
| DiagnosisController | Ingress | KEEP + ADAPT | 从直接驱动旧流程转为提交 Business Event / 查询结果 |
| DiagnosisOrchestrationService | Application/legacy orchestration seam | REFACTOR | 逐步让出 fixed workflow 主控，成为 Runtime façade/adapter |
| CDPManager | Clinical State aggregate adapter | KEEP + REFACTOR | 保留版本/锁/快照资产，移除无治理直接写路径 |
| DiagnosisWorkflowOrchestrator | legacy fixed scheduler | REPLACE incrementally | 5-step 不再是系统真源；由 Unit Scheduler 按状态路由 |
| AgentLoop | historical asset | DO NOT PROMOTE | 可吸收局部能力，不恢复为开放式临床主控 |
| StateCommitter | P01 foundation | REUSE_FOUNDATION + ADAPT | 接管真实 Clinical CDP authoritative write |
| Python Runtime | capability execution foundation | REUSE_FOUNDATION + ADAPT | 保留 deterministic single-invocation；上层新增 Clinical Runtime/P02 |
| Model Runtime | P03 foundation | REUSE_FOUNDATION + ADAPT | 受 Capability/Prompt/Version binding 管理 |
| Execution Trace | P05 | REUSE_FOUNDATION + ADAPT | 接入 event/unit/decision/proposal/commit/runtime refs |
| Dialog private Redis/memory truth | temporary execution/cache only | REMOVE AS TRUTH | 不再作为正式问诊状态源 |
| Frontend Zustand | UI local state | KEEP AS UI LOCAL | 不生成 Clinical Truth |
| Legacy LLM | legacy dependency | REPLACE / REMOVE | 不重新启用作为临床正式路径 |

---

# 18. 部署与进程拓扑原则

Phase 9 不以“先进”为理由拆微服务。

V1 优先形成：

```text
一个权威 Clinical Orchestration control path
+
一个权威 State Governance write path
+
若干可独立部署/现有的 Capability runtime
+
独立 Runtime persistence / Trace / Registry logical stores
```

Brownfield 初期允许：

```text
Java Ingress/Application
→ Clinical Runtime façade / scheduler
→ existing Python/Java capabilities
→ P01 State Governance
```

语言边界不等于业务 Owner 边界。

明确暂不冻结：

```text
LangGraph / Temporal / Cadence / 自研 scheduler
Kafka / RabbitMQ / DB queue
Redis / PostgreSQL / Mongo 等 Runtime store 产品
Kubernetes 服务拆分粒度
sync RPC vs async message 的最终组合
```

选择这些技术必须以后续的可靠性、恢复语义、吞吐、运维成本和当前团队能力为依据，而不是先选框架再倒推架构。

---

# 19. Brownfield 迁移策略

## 19.1 Strangler，而不是双主控

迁移原则：

```text
legacy fixed workflow
与
new Clinical Runtime
```

可以阶段性共存，但同一个 Consultation 不能同时由两套主控产生正式 Clinical State 写入。

禁止：

```text
Dual Orchestration + Dual Clinical Write
```

## 19.2 Consultation-level routing

推荐通过显式 runtime/orchestration binding 或 feature gate，使：

```text
旧 Consultation → 继续 legacy binding
新 eligible Consultation → new runtime binding
```

不得在进行中的 Consultation 中静默从旧 fixed workflow 切到新语义。

## 19.3 Shadow 的允许边界

可以：

```text
shadow calculation
shadow route comparison
shadow capability output
shadow trace
```

但 shadow 结果：

```text
不得 commit Clinical State
不得发送给用户
不得形成第二个正式业务 effect
```

## 19.4 推荐迁移顺序

Runtime 迁移必须与 Phase 6 推荐 Slice A 对齐：

```text
Foundation:
P01 State Governance / Clinical CDP Adapter
P02 Durable Clinical Resume
P05 Trace correlation
P06 Version Binding

Slice A core:
U01
U02
U03
U04
U05
U06
U07

Horizontal closure:
U11
U14
U15
```

然后再进入：

```text
U08/U09 DDx loop
U10 offline evidence
U12 normal completion
U13 correction/invalidation full path
```

这只是迁移设计顺序，不构成 Implementation Authorization。

---

# 20. Slice A Runtime 映射

## U01

```text
START event
→ Run open
→ subject/problem extraction
→ D10 scope adjudication
→ D01 lifecycle decision
→ governed commit
```

## U02

```text
new clinical input
→ C01 parse/normalize
→ Owner interpretation
→ typed K09 proposal
→ P01 commit Clinical State version
```

## U03/U04

```text
committed facts
→ C02 risk evidence
→ D09 / F4 Clinical Risk
→ commit
→ D02 / G4 Safety Gate
→ commit
```

普通路由只允许从已提交 Safety 状态继续。

## U05

```text
current committed readiness inputs
→ D03 Clinical Readiness decision
→ proposal
→ commit
```

## U06

```text
readiness requires question
→ C03 gap/question
→ D04 stopping
→ selected question
→ delivery success
→ lifecycle WAITING_USER commit
→ checkpoint
→ Run ends
```

## U07

```text
answer event
→ business resume validation
→ dedupe/expiry/applicability
→ runtime resume/rehydrate
→ event effect once
→ U02 / subsequent state route
```

## U11/U14/U15

分别保证：

```text
safe exit closure
failure closure
cancel/expire closure
```

使 Slice A 不是只有 happy path 的半条链。

---

# 21. Runtime correctness 场景

Phase 9 后续实现至少必须能证明以下场景：

```text
RTE-01 duplicate USER_ANSWER → one clinical effect
RTE-02 process crash after commit before checkpoint → no duplicate effect
RTE-03 process crash after capability before proposal → no phantom Clinical Truth
RTE-04 stale checkpoint + valid business answer → rehydrate or controlled U14, not false rejection
RTE-05 checkpoint points to stale Clinical State → cannot overwrite newer state
RTE-06 concurrent events → conflict detected, no blind stale replay
RTE-07 risk capability unavailable → ordinary clinical continuation blocked
RTE-08 Safety Gate BLOCKED/UNAVAILABLE → no DDx/delivery ordinary route
RTE-09 capability SUCCESS + commit FAILED → Unit not treated as completed
RTE-10 duplicate delivery replay → no duplicate external side effect
RTE-11 CANCELLED/EXPIRED consultation + late answer → zero new clinical effect
RTE-12 correction invalidates upstream fact → affected downstream results cannot remain silently current
RTE-13 Runtime restart while WAITING_USER → pending question remains resumable
RTE-14 unsupported/failure result → not coerced to empty/normal/negative business value
RTE-15 old fixed workflow and new runtime coexist → same Consultation has only one authoritative writer
```

---

# 22. Phase 9 与后续阶段边界

## Phase 9 冻结

```text
Runtime ownership
logical component boundaries
Thread / Run / Checkpoint semantics
Unit scheduling model
WAIT / Resume protocol
State commit integration protocol
idempotency/concurrency/conflict model
failure/retry control model
version binding
logical persistence responsibilities
Brownfield runtime migration strategy
```

## 留给 Phase 10

```text
Frontend presentation
UI recovery UX
loading/waiting/error presentation
client-side delivery behavior
```

## 留给 Phase 11

```text
完整异常矩阵
安全验证矩阵
Eval / E2E / chaos / resilience acceptance
clinical evaluation gates
observability acceptance
```

## 留给 Phase 12

```text
具体 deployment topology
CI/CD
release gate
SLO / capacity / autoscaling
migration execution plan
rollback/runbook
legacy retirement
production authorization
```

具体框架/数据库/消息系统可以在实施设计中选定，但不得改变本 Phase 9 冻结的业务执行语义。

---

# 23. Phase 9 全局不变量

```text
RUNTIME-INV-01 Runtime != Clinical Truth Owner
RUNTIME-INV-02 Checkpoint != Clinical State
RUNTIME-INV-03 Trace != Clinical State
RUNTIME-INV-04 Capability Result cannot bypass Owner/Policy/G2
RUNTIME-INV-05 Scheduler routes from committed state, not uncommitted candidate
RUNTIME-INV-06 Business Resume validity != Runtime Resume compatibility
RUNTIME-INV-07 valid business event cannot be invalidated only because checkpoint is stale/missing
RUNTIME-INV-08 duplicate event cannot produce duplicate clinical effect
RUNTIME-INV-09 commit conflict cannot be solved by blind stale replay
RUNTIME-INV-10 safety-sensitive failure cannot become ordinary clinical continuation
RUNTIME-INV-11 Runtime retry cannot bypass idempotency/version/safety policy
RUNTIME-INV-12 one Consultation cannot have two authoritative clinical writers
RUNTIME-INV-13 fixed 5-step workflow is migration source, not target system truth
RUNTIME-INV-14 runtime/framework choice cannot redefine frozen Unit/Capability/Contract semantics
RUNTIME-INV-15 same Consultation cannot silently switch bound clinical semantics mid-flight
RUNTIME-INV-16 checkpoint loss after clinical commit must be recoverable from authoritative state
RUNTIME-INV-17 Runtime failure != business negative result
RUNTIME-INV-18 Thread/Run completion != Consultation completion
```

---

# 24. Phase 9 冻结前检查清单

Phase 9 只有同时满足以下条件才能冻结：

- [ ] Runtime 与 Clinical State Owner 边界无歧义；
- [ ] Thread / Run / Checkpoint / Consultation 语义完全分离；
- [ ] Unit 调度不重新引入 fixed completeness / 5-step truth；
- [ ] Capability → Decision → Proposal → Commit 链没有被 Runtime 绕过；
- [ ] Business Resume 与 Runtime Resume 完整拆分；
- [ ] duplicate / crash / checkpoint stale / commit conflict 均有明确恢复语义；
- [ ] safety failure 不存在 fail-open；
- [ ] one-consultation-one-authoritative-writer 原则明确；
- [ ] P01/P02/P05/P06 与现有资产处置清楚；
- [ ] Brownfield 迁移不存在 dual-write Clinical Truth；
- [ ] Slice A 能形成 Start → Facts → Risk → Safety → Readiness → Ask → Wait → Resume 以及 Safe Exit / Failure / Cancel 闭环；
- [ ] 未提前冻结 Phase 10～12 的具体实现/交付事项；
- [ ] 未把框架选型当成架构本身。

当前状态：

```text
Phase 9 design draft = COMPLETE
Independent review = PENDING
Phase 9 = NOT FROZEN
Implementation Authorization = NOT IMPLIED
```
