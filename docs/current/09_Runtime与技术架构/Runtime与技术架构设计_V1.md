# AIdoctor Phase 9 — Runtime 与技术架构设计 V1

> 状态：FROZEN / V1（已同步 Phase 7 / Phase 8 最新治理语义）  
> 适用基线：`main` 当前真实代码 + Phase 1～8 当前权威设计  
> 目标：在不改变既有业务语义、状态 Owner、Unit、C/P/D 与 K01–K10 契约边界的前提下，定义 V1 临床 Runtime 的执行、调度、等待、恢复、提交、失败、并发、版本绑定、知识/规则装载、回滚、外部副作用一致性与 Brownfield 迁移架构。  
> 非目标：本文件不构成 Implementation Authorization；不冻结具体 Runtime 框架、消息队列、数据库、微服务拆分或部署厂商；不进入 Phase 10。

---

# 1. Phase 9 的边界

Phase 1～8 已冻结：

```text
业务语义
→ 状态与 Owner
→ 业务闭环
→ Unit
→ C / P / D
→ K01–K10 契约与数据语义
```

Phase 9 只回答：

```text
谁驱动一次执行？
如何从 committed Clinical State 选择下一 Unit？
何时 WAIT？如何 Resume？
如何处理 duplicate / conflict / crash / timeout？
如何保证 replay 不重复同一业务 effect？
如何在调用能力前校验 CapabilityBindingRef？
如何装载并校验 KnowledgeReleaseRef / RuleReleaseRef？
如何保证同一 Consultation 不静默切换临床语义版本？
如何进行 capability / knowledge / rule rollback？
如何管理外部发送与 Clinical State 的一致性？
如何把当前 fixed workflow 增量迁移进新 Runtime？
```

Phase 9 不重新定义 Clinical Risk、Safety Gate、Clinical Readiness、Delivery Readiness、Unit、C/P/D 或 K01–K10 语义。

---

# 2. 当前真实技术基线

当前 `main` 的 Brownfield 主链历史上来自：

```text
Frontend
→ DiagnosisController
→ DiagnosisOrchestrationService
→ CDPManager
→ DiagnosisWorkflowOrchestrator
→ legacy Python / Java clinical services
```

现有可复用基础包括：

```text
CDP aggregate/versioning/locking
StateCommitter
Python Runtime foundation
Model Runtime
Execution Trace
Foundation-0 Runtime 基础
```

已经完成的 Foundation-0 / U01 实施不改变本文件的设计角色：Phase 9 仍描述目标 Runtime 语义，不把实现进度混入架构真值。

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
RUNTIME-INV-10 same event_id transport replay maps to the same canonical event
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
RUNTIME-INV-22 external send success != Clinical State commit
RUNTIME-INV-23 Capability Exists != Capability Active
RUNTIME-INV-24 Runtime may invoke a clinical Capability only through a valid CapabilityBindingRef
RUNTIME-INV-25 Candidate/Withdrawn/Expired Knowledge cannot become new formal clinical basis
RUNTIME-INV-26 safety-critical Rule must resolve to an approved RuleReleaseRef
RUNTIME-INV-27 capability / knowledge / rule rollback cannot rewrite historical Run bindings
RUNTIME-INV-28 new releases apply to new bindings unless an explicit auditable migration is authorized
RUNTIME-INV-29 Checkpoint must preserve references needed to reconstruct the bound governance context
RUNTIME-INV-30 Runtime cache cannot override authoritative registry/release semantics
```

---

# 4. Runtime 总体逻辑架构

```text
API / Application Ingress
        ↓
Canonical Business Event / Effect Ledger
        ↓
Clinical Run Coordinator
        ↓
Binding & Release Resolver
        ↓
Unit Scheduler / Transition Engine
        ├────────→ Business Owner / D01-D10 Policy Host
        ├────────→ Capability Invocation Gateway
        │             ├→ P06 CapabilityBindingRef validation
        │             ├→ P04 KnowledgeReleaseRef / RuleReleaseRef resolution
        │             ├→ P03 Model Runtime / Prompt Registry
        │             └→ C01-C06
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
P06 能力范围 / 能力包 / 版本治理
P04 医学知识与证据治理
Runtime persistence / Registry / Outbox / delivery receipt
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

---

# 5. 逻辑组件职责

## 5.1 API / Application Ingress

接收 Start / Answer / Correction / Cancel，建立 identity / consultation / correlation context，转换 K02 Business Event。

禁止：

```text
前端生成“正常/低风险”临床默认值
Controller 直接推进 fixed step
绕过 K09/G2 写 Clinical Truth
```

## 5.2 Canonical Business Event / Effect Ledger

负责 durable canonical event identity、transport replay 映射、business decision refs、effect identities、proposal/commit/no-effect refs。

Ledger 不拥有 `ACCEPTED / DUPLICATE / EXPIRED / REJECTED / APPLIED` 的业务裁决。

```text
same event_id replay
→ attach to original canonical event
→ no second Business Decision
```

## 5.3 Clinical Run Coordinator

为 canonical event 或合法 internal continuation 建立/恢复 Run，驱动 bounded execution，并协调 Scheduler、Policy、Capability、State Governance、Delivery。

不拥有 Consultation Lifecycle 真值。

## 5.4 Binding & Release Resolver

这是 Phase 7/8 补齐后新增的显式 Runtime 逻辑职责，不要求一定独立成服务。

负责在 Run / Capability 调用前解析并校验：

```text
CapabilityBindingRef
KnowledgeReleaseRef
RuleReleaseRef
PromptReleaseRef
ModelRouteRef
Contract compatibility context
```

至少验证：

```text
能力状态允许使用
未过 effective_until
scope / population / region / language / channel 匹配
knowledge release = PUBLISHED 且当前有效
rule release = approved/current
prompt/model/tool/skill 在 capability allowlist
contract/schema compatible
```

任何一项不满足：

```text
不得静默 fallback 到未批准版本
不得使用“最新版本”替代已绑定版本
→ 进入明确 failure / D07 路由
```

## 5.5 Unit Scheduler / Transition Engine

输入：

```text
committed Clinical State
+ Consultation Lifecycle
+ Clinical Risk / Safety Gate / Clinical Readiness / Delivery Readiness
+ accepted canonical event context
+ pending interaction refs
+ deterministic decision refs
+ bound governance context
```

输出：`next eligible Unit / WAIT / no-progress / terminal execution intent`。

只从 committed state 路由。

## 5.6 Business Owner / Policy Host

承载 D01-D10、Business Owner interpretation、Deterministic Decision，以及完成业务解释后的 K09 Proposal production。

Owner 保持：

```text
F4 → Clinical Risk
G4 → Safety Gate
F7 → Delivery Readiness
G2 → Clinical State Version
Runtime → Thread / Run / Checkpoint
```

## 5.7 Capability Invocation Gateway

调用前必须获得有效 `CapabilityBindingRef`。

调用流程：

```text
resolve CapabilityBindingRef
↓
validate ACTIVE / effective period / scope / population
↓
resolve allowed RuleReleaseRef / KnowledgeReleaseRef
↓
validate prompt/model/tool/skill allowlists
↓
invoke approved C01-C06 / P03 / P04 assets
↓
return K04 Capability Result with binding/release refs
```

Gateway 管理执行级 timeout/cancellation/correlation，并分离旧 ToolResult execution status 与业务 Capability Result。

Capability 不得直接 commit Clinical State。

## 5.8 P01 State Governance / Clinical CDP Adapter

```text
StateCommitter foundation
+
CDP aggregate/version/locking
→ 唯一正式 Clinical State 写入边界
```

职责：authorization、source/field/consent validation、base version validation、typed proposal validation、atomic commit、audit/evidence refs、Clinical State Version advance。

## 5.9 P02 Durable Clinical Resume

负责 Thread、Run、Checkpoint、interrupt/wait execution context、pending refs、runtime expiry、retry/repair metadata、resume compatibility。

不拥有 Business Resume validity。

## 5.10 Delivery Side-effect Coordinator

负责 Question / Result 的 durable delivery intent、idempotency key、transport attempt、receipt、reconciliation。

不拥有 Question lifecycle、Consultation lifecycle 或 Delivery Readiness。

---

# 6. Runtime 核心对象

## 6.1 Consultation

业务对象，不属于 Runtime ownership。

## 6.2 Thread

表示 Consultation 的 durable execution lineage。

```text
Thread state != Consultation state
```

同一 Consultation 只能有一个正式 Clinical State orchestration lineage。

## 6.3 Run

Run 是一个 canonical event 或合法 internal continuation 的有限执行区间。

Run 必须绑定或引用稳定的治理上下文：

```text
clinical_state_version
capability_binding_refs[]
knowledge_release_refs[]
rule_release_refs[]
prompt_release_refs[]
model_route_refs[]
contract/runtime schema versions
```

Run 结束于 WAIT、terminal、controlled failure、no-progress 或当前 consequences 已收敛。

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

capability_binding_refs[]
knowledge_release_refs[]
rule_release_refs[]
prompt_release_refs[]
model_route_refs[]
contract_version
runtime_schema_version

trace refs
created_at
integrity metadata
```

Checkpoint 不应复制完整 Knowledge/Rule/Capability 对象；保存稳定引用并由 Registry/Release Store 解析。

## 6.5 Pending Interaction

业务 Question 状态来自 governed Clinical State/K06。Runtime 只保存引用、expected state context、expiry 和 correlation refs。

---

# 7. Canonical Event 与 Effect 幂等模型

```text
Canonical Event ID
!= Effect ID
```

同一个 accepted event 可以合法产生多个不同 intended effects。

Runtime 内部允许 at-least-once execution，但临床侧要求：

```text
same intended effect
→ at most once formal application
```

Effect identity 至少语义绑定：

```text
event_id
+ unit/owner/effect type
+ stable target
+ relevant binding/version context
```

---

# 8. 标准 Run 执行协议

```text
1. Receive K02 input
2. resolve-or-create canonical Event identity
3. same event_id replay → attach original processing
4. Load authoritative Clinical State
5. Business applicability/resume validation
6. persist Business Decision ref
7. non-ACCEPTED event 收敛，0 新 clinical effect
8. Resolve Consultation binding context
9. Validate CapabilityBindingRef / KnowledgeReleaseRef / RuleReleaseRef / contract compatibility
10. Open/resume Thread and create Run
11. Reconcile already-applied effects
12. Scheduler 从 committed state 选择 next eligible Unit
13. Resolve Unit-required C/P/D dependencies
14. Capability Gateway 调用 approved Capability，或执行 deterministic policy
15. Business Owner/Resolver interprets
16. 形成 Decision / intended effect
17. 形成独立 effect idempotency identity
18. 如需 state change → K09 Proposal → G2/P01 commit
19. 处理 Commit Result
20. reload authoritative Clinical State
21. 继续下一 consequence，直到 WAIT/terminal/failure/no-progress
22. 在 durable boundary 写 Checkpoint，包含治理绑定 refs
23. canonical event 达到业务 completion 后记录 APPLIED/terminal outcome
24. 全程关联 Trace/Audit refs
```

若 binding/release validation 失败，不允许把问题降级成普通“无结果”；必须进入明确 failure semantics / U14。

---

# 9. Unit 路由模型

V1 不使用固定 `current_step=1..5` 或 completeness threshold 作为系统真源。

普通事实主干：

```text
committed Facts
→ U03 Risk
→ U04 Safety
→ U05 Readiness
```

Readiness 再决定 U06 / U08 / U10 / U11 / U12 等合法路径。

U14 failure routing 可抢占普通路径。

---

# 10. WAIT / Resume 协议

Business Resume 必须先于 Runtime Resume。

对新的 canonical USER_ANSWER event：

```text
USER_ANSWER
→ Business Resume Decision
→ ACCEPTED / DUPLICATE / EXPIRED / REJECTED
→ 只有 ACCEPTED 才允许产生新 effects
→ Runtime resume / rehydrate
```

Resume compatibility 至少检查：

```text
checkpoint → Clinical State Version compatibility
checkpoint → runtime schema compatibility
checkpoint → CapabilityBindingRef compatibility
checkpoint → contract compatibility
pending interaction validity
```

对于 Knowledge/Rule：

- 历史 Run 重放使用历史绑定 refs；
- 新的未开始 Unit 调用不得使用已经 WITHDRAWN/EXPIRED 的 release 生成新的临床判断；
- 若无法在“不改变已完成历史 effect”的前提下继续安全执行，则进入 D07/U14，而不是静默换 release。

stale/missing checkpoint 时，优先从 authoritative Clinical State + accepted event + ledger + bound refs 重建。

---

# 11. Question / Delivery 外部副作用一致性

```text
Question SELECTED commit
→ durable delivery intent
→ stable delivery_id/idempotency
→ transport send
→ durable receipt
→ delivery confirmation
→ G2 atomic commit:
   Question DELIVERED_TO_USER
   + Consultation WAITING_USER
→ checkpoint
```

crash/replay 必须通过稳定 delivery identity reconciliation，禁止重复外发。

---

# 12. 并发、Conflict 与单写者

同一 Consultation 任一时刻只有一个正式 Clinical State 主写路径。

遇到 `CONFLICT`：

```text
reload authoritative Clinical State
→ reconcile intended effect
→ 判断 event/effect 与 binding context 是否仍适用
→ 必要时重新执行 Owner/Policy interpretation
→ 形成基于新版本的新 Proposal
```

禁止旧 Proposal 盲目重试。

---

# 13. Checkpoint 与 Crash Recovery

Durable boundary：

```text
canonical event identity durable
formal state commit completed
entered WAIT
external side-effect outcome durable
run terminal/no-progress
binding context resolved/changed through approved migration
```

## 13.1 crash after commit / before checkpoint

从 authoritative Clinical State + event/effect ledger + commit refs 重建 cursor，不能重复 effect。

## 13.2 crash after Capability / before Proposal

能力结果不是 Clinical Truth；恢复后重新校验原 `CapabilityBindingRef` 和 release refs 后才可决定 retry/repair。

## 13.3 binding/release changed while crashed

历史已完成 effect 保留原 refs。

未完成 effect：

```text
原 binding/release 仍合法 → 按原 refs 恢复
原 release 已撤回/失效且不可继续 → D07/U14
```

禁止自动改用“最新版本”。

---

# 14. Failure 与 Retry

分开：

```text
A. execution/tool failure
B. capability business_status + reason_code + retryable
C. binding/release validation failure
D. D07 business failure routing decision
```

Retry 受：

```text
retryable
attempt/time budget
idempotency
clinical-state version
capability binding
knowledge/rule release validity
safety impact
fallback policy
D07
```

必须保持：

```text
risk service timeout != NO_HIGH_RISK_SIGNAL
DDx failure != empty normal DDx
Safety failure != SAFE
invalid binding != UNSUPPORTED normal outcome
withdrawn knowledge != NO_EVIDENCE
Capability SUCCESS + Commit FAILED != Unit success
```

---

# 15. Version / Binding / Release 运行规则

Consultation 至少绑定：

```text
scope_version
capability binding context
contract compatibility context
```

Run 至少可追溯：

```text
runtime_version
capability_binding_refs[]
policy versions
rule_release_refs[]
knowledge_release_refs[]
prompt_release_refs[]
model_route_refs[]
contract/schema version
```

## 15.1 Capability activation

Runtime 不允许根据“代码存在”判断 capability 可调用。

必须满足：

```text
CapabilityBindingRef.status 可用
within effective period
scope/population/region/language/channel compatible
all referenced releases / routes are authorized
contract compatible
```

## 15.2 Knowledge / Rule loading

正式临床调用只能装载绑定的 release：

```text
KnowledgeReleaseRef.status = PUBLISHED / allowed active state
RuleReleaseRef.status = approved/current active state
```

候选、撤回、过期、退役 release 不得用于新的正式 Clinical Decision。

## 15.3 静默升级禁止

进行中的 Consultation 默认保持已绑定临床语义版本。

```text
new capability/knowledge/rule release
!= automatic mid-flight switch
```

只有显式、可审计 migration policy 才允许迁移。

---

# 16. Rollback 运行语义

## 16.1 Capability rollback

```text
发现新能力版本问题
→ stop new binding
→ activate approved rollback capability version
→ new Run / new Consultation use rollback version
→ historical Run keeps original binding refs
```

## 16.2 Knowledge / Rule rollback

```text
release 出现质量/安全问题
→ WITHDRAW / DEPRECATE / EXPIRE
→ stop new formal use
→ activate approved rollback target
→ historical decisions keep original refs
```

Rollback 不能删除或改写历史 Trace / Decision / Commit。

---

# 17. 逻辑持久化职责

```text
Clinical State Store
  governed CDP + version history

Canonical Event / Effect Ledger
  canonical event identity + replay mapping
  + decision/effect/commit refs

Runtime Store
  Thread + Run + Checkpoint + retry/expiry/cursor

Delivery/Outbox Store
  intent + idempotency + transport receipt/reconciliation

Trace/Audit Store
  execution/audit refs

Version/Registry Store
  CapabilityBindingRef
  Scope / Capability metadata
  KnowledgeReleaseRef
  RuleReleaseRef
  PromptReleaseRef
  ModelRouteRef
  Contract / Runtime schema compatibility metadata
  rollback targets
```

Runtime Store 不得成为第二份 Clinical Truth store。

Registry cache 可以存在，但缓存失效策略不能让已撤回/过期 release 继续被新的正式调用使用。

---

# 18. Trace / Observability 关联

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
capability_binding_ref
knowledge_release_refs[]
rule_release_refs[]
prompt_release_refs[]
model_route_refs[]
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

默认不复制完整 PHI。

---

# 19. Brownfield 资产映射

| 当前资产 | Phase 9 定位 | 处置 | 关键变化 |
|---|---|---|---|
| DiagnosisController | Ingress | KEEP + ADAPT | 转为 Business Event/Query façade |
| DiagnosisOrchestrationService | legacy orchestration seam | REFACTOR | 逐步让出 fixed workflow 主控 |
| CDPManager | Clinical State aggregate adapter | KEEP + REFACTOR | 移除无治理直接写 |
| DiagnosisWorkflowOrchestrator | legacy fixed scheduler | REPLACE incrementally | 5-step 不再是系统真源 |
| AgentLoop | historical asset | DO NOT PROMOTE | 不恢复开放式临床主控 |
| StateCommitter | P01 foundation | REUSE_FOUNDATION + ADAPT | 接管 authoritative Clinical CDP write |
| Python Runtime | execution foundation | REUSE_FOUNDATION + ADAPT | 上层新增 Clinical Runtime/P02 |
| Model Runtime | P03 | REUSE_FOUNDATION + ADAPT | 受 CapabilityBindingRef / Prompt / Model route 管理 |
| KG/RAG/Knowledge assets | P04 | REFACTOR + ADAPT | 受 KnowledgeReleaseRef 治理 |
| Risk/Safety Rule assets | P04/Dxx input | REFACTOR + ADAPT | 受 RuleReleaseRef 治理 |
| Execution Trace | P05 | REUSE_FOUNDATION + ADAPT | 增加 binding/release refs |
| Capability Package 思路 | P06 | ABSORB + ADAPT | 形成正式 binding / lifecycle / allowlist / rollback |
| Dialog Redis/memory | cache/execution aid | REMOVE AS TRUTH | 不作为正式状态源 |
| Frontend Zustand | UI local state | KEEP AS UI LOCAL | 不生成 Clinical Truth |
| Legacy LLM | legacy dependency | REPLACE / REMOVE | 不重新启用为正式临床路径 |

---

# 20. 部署与技术选型原则

V1 首先保证：

```text
one authoritative Clinical Orchestration control path
+
one authoritative State Governance write path
+
approved and bound Capability execution
+
versioned Knowledge/Rule release resolution
+
durable Runtime/Event/Delivery persistence
+
Trace/Registry
```

本阶段不冻结 LangGraph/Temporal/Cadence、自研 scheduler、MQ、数据库产品或微服务粒度。

---

# 21. Brownfield 迁移策略

## 21.1 Strangler，不双主控

```text
legacy-bound Consultation → old workflow
new-runtime-bound Consultation → Clinical Runtime
```

同一 Consultation 不允许 legacy/new 双主控或双写 Clinical State。

## 21.2 Shadow

允许 shadow calculation / route comparison / capability output / trace；不得 commit Clinical State、不得发送给用户、不得形成第二套 business effect。

## 21.3 按 Unit 增量建设

Foundation 只保留首个 Unit 必需的最小跨 Unit 基础；P03/P04/P06 的后续治理能力按第一个真实消费者 Unit 增量扩展。

这与 Phase 7 的：

```text
FOUNDATION_PREREQUISITE
FIRST_CONSUMER_UNIT
INCREMENTAL_EXTENSION
DEFERRED
```

保持一致。

---

# 22. Slice A Runtime 映射

```text
U01
→ canonical event
→ C01 subject/problem semantics
→ resolve minimal CapabilityBindingRef / scope binding
→ D10 scope
→ D01 lifecycle
→ governed commit

U02
→ C01 parse/normalize
→ Owner interpretation
→ typed Proposal
→ P01 commit

U03/U04
→ validate C02 binding + RuleReleaseRef / KnowledgeReleaseRef
→ C02 risk evidence
→ D09/F4 Risk commit
→ D02/G4 Safety Gate commit

U05
→ D03 Readiness
→ commit

U06
→ validate C03 binding / question policy
→ C03 question/gap
→ D04 stopping
→ Question SELECTED commit
→ durable delivery
→ DELIVERED_TO_USER + WAITING_USER commit
→ checkpoint with binding refs

U07
→ Business Resume validation
→ validate resume compatibility incl. capability binding / contract
→ rehydrate
→ reconcile completed effects
→ continue remaining consequences idempotently

U11/U14/U15
→ Safe Exit / Failure / Cancel-Expire closure
```

---

# 23. Runtime correctness 必测场景

保留原 RTE-01～RTE-22，并新增：

```text
RTE-23 capability code exists but binding not ACTIVE → invocation blocked
RTE-24 CapabilityBindingRef expired → no silent latest-version fallback
RTE-25 capability allowlist excludes prompt/model/tool → invocation blocked
RTE-26 KnowledgeReleaseRef = CANDIDATE → cannot support formal Clinical Decision
RTE-27 KnowledgeReleaseRef = WITHDRAWN / EXPIRED → no new formal use
RTE-28 safety-critical RuleReleaseRef missing/invalid → ordinary continuation blocked
RTE-29 checkpoint resumes with historical valid binding refs → historical effects remain reproducible
RTE-30 release changed after checkpoint → no silent mid-flight semantic switch
RTE-31 capability rollback → new bindings use rollback version, historical Run refs unchanged
RTE-32 knowledge/rule rollback → new decisions use rollback target, historical Decision refs unchanged
RTE-33 stale registry cache cannot authorize withdrawn release
RTE-34 Trace must identify capability/knowledge/rule/prompt/model versions used for each governed result
```

---

# 24. 与后续阶段边界

Phase 9 冻结：

```text
Runtime ownership
logical component boundaries
Thread/Run/Checkpoint semantics
Canonical Event/Effect idempotency
Unit scheduling
WAIT/Resume
Capability binding validation
Knowledge/Rule release loading
rollback runtime semantics
external side-effect consistency
State commit/conflict integration
failure/retry control
logical persistence
Brownfield runtime migration
```

Phase 10：前端与交付展示。  
Phase 11：完整安全、异常、Eval/E2E/chaos/resilience/observability acceptance。  
Phase 12：具体部署、CI/CD、SLO、迁移执行、回滚 runbook、legacy retirement、production authorization。

---

# 25. Phase 9 同步审查与冻结结论

原 P9-R01～P9-R07 的关闭结论继续成立，不重新打开。

本次 Phase 7 / Phase 8 同步新增：

```text
P9-SYNC-01 CLOSED
Phase 8 新增 CapabilityBindingRef
→ Runtime 增加 Binding & Release Resolver 与调用前激活/范围/白名单校验。

P9-SYNC-02 CLOSED
P04 新增 Knowledge Release 生命周期
→ Runtime 明确只装载已批准、当前有效的 KnowledgeReleaseRef。

P9-SYNC-03 CLOSED
Phase 8 新增 RuleReleaseRef
→ 安全关键 Policy/Capability 输入必须解析正式 RuleReleaseRef。

P9-SYNC-04 CLOSED
Checkpoint 旧设计只记录粗粒度 version binding
→ 补 capability/knowledge/rule/prompt/model/contract refs。

P9-SYNC-05 CLOSED
Resume 旧兼容检查不足
→ 增加 capability binding / contract / release compatibility。

P9-SYNC-06 CLOSED
Rollback 过去仅作为版本治理概念
→ 明确新绑定切换、历史 Run 不改写的 Runtime 语义。

P9-SYNC-07 CLOSED
Trace 过去不足以重放新的治理上下文
→ 增加 capability/knowledge/rule/prompt/model refs。
```

冻结检查：

- [x] Runtime 与 Clinical State Owner 边界无歧义；
- [x] Thread / Run / Checkpoint / Consultation 分离；
- [x] Scheduler 只从 committed state 路由；
- [x] Capability → Decision → Proposal → Commit 不被绕过；
- [x] Business Resume 与 Runtime Resume 分离；
- [x] Event identity 与 Effect identity 分离；
- [x] Question/Delivery 有 crash-safe reconciliation；
- [x] 同一 Consultation 只有一个 authoritative writer；
- [x] Capability Exists != Capability Active；
- [x] Capability invocation 必须经过 CapabilityBindingRef；
- [x] Knowledge/Rule 正式使用必须经过 Release Ref；
- [x] Checkpoint 保留治理绑定引用；
- [x] Resume 不静默切换 bound semantics；
- [x] Rollback 不改写历史运行；
- [x] Trace 可定位能力/知识/规则/Prompt/Model 版本；
- [x] 未新增 K11；
- [x] 未改变 Phase 1–8 已冻结业务语义。

最终状态：

```text
Phase 9 = FROZEN / V1
Synchronized with current Phase 7 / Phase 8 authority
Implementation Authorization = NOT IMPLIED
Merge Authorization = NOT IMPLIED
```
