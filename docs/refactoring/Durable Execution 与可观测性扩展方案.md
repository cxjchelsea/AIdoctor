# AIdoctor Durable Execution 与可观测性扩展方案

> 文档状态：Draft v2.3 Extension  
> 更新时间：2026-07-29  
> 关联主方案：[AIdoctor 企业级临床 Agent 重构方案](./enterprise-agent-refactoring-plan.md)  
> 关联扩展：[临床数据与循证智能扩展方案](./clinical-data-and-evidence-intelligence-extension.md)  
> 关联扩展：[Agent Runtime Foundations 扩展方案](./agent-runtime-foundations-extension.md)  
> 适用仓库：`cxjchelsea/AIdoctor`

---

## 1. 文档目的

本文档把 AIdoctor 中此前已经提出但尚未独立展开的两类能力正式纳入重构方案：

1. **Durable Execution & Resume**：问诊、工具执行、人工审核和外部动作在用户离开、服务重启、节点失败、部署升级或重复提交后仍可安全恢复；
2. **Observability & Decision Records**：使用开源标准观察技术调用链，同时将 Agent 决策、临床决策和合规审计分别记录，避免 Trace、Log、Checkpoint 和 Audit 相互混用。

本方案中的“断点续传”不是文件下载中的字节续传，而是：

> Agent 在明确的持久化执行点保存状态，在中断条件解除后，从可验证、可迁移且不会重复副作用的位置继续运行。

本扩展不替代：

- Encounter CDP；
- Evidence Ledger；
- LangGraph GraphState；
- Clinical Intelligence；
- Safety Engine；
- Agent Runtime Foundations；
- Clinical Decision Record；
- Compliance Audit。

它负责定义这些组件在中断、恢复、重放和观测过程中的协作关系。

---

## 2. 核心结论

### 2.1 恢复依赖 Checkpoint，不依赖 Trace 或 Log

```text
Checkpoint
回答：执行到哪里、当前状态是什么、下一步允许做什么

Trace
回答：一次请求如何跨节点和服务运行、哪里慢、哪里失败

Log
回答：某个时间点发生了什么事件或错误
```

Trace 和 Log 可以帮助定位恢复失败，但不能作为恢复状态源。

### 2.2 Checkpoint 不等于业务临床状态

```text
Encounter CDP / Evidence Ledger
保存本次问诊临床事实、候选、风险和计划

LangGraph Checkpoint
保存执行节点、GraphState、Interrupt 和恢复游标
```

恢复 Graph 时必须重新验证 CDP 版本，而不能假设 checkpoint 中缓存的临床状态仍是最新事实。

### 2.3 至少一次执行是现实，恰好一次依赖幂等

分布式系统很难保证所有动作天然“恰好执行一次”。AIdoctor 应采用：

```text
At-least-once delivery
+ Idempotency Key
+ ExternalActionRecord
+ Inbox/Outbox 去重
+ 状态版本校验
```

达到业务上的“等效恰好一次”。

### 2.4 Resume 不是从代码行继续

Interrupt 后恢复时，节点可能从节点入口重新执行。因此：

- interrupt 前不能放置未受保护的不可逆动作；
- 节点必须可重入；
- 副作用必须在独立 action node 中执行；
- 所有动作必须幂等或可查询执行状态。

### 2.5 技术可观测主干采用 OpenTelemetry

推荐：

```text
Spring Boot / FastAPI / LangGraph / Tool Services
                    │ OTLP
                    ▼
          OpenTelemetry Collector
          ├── Trace  → Tempo 或 Jaeger
          ├── Metric → Prometheus
          └── Log    → Loki 或 OpenSearch
                         │
                         ▼
                       Grafana
```

可选使用 Phoenix 作为 Agent/RAG 调试和评估界面，但不能用其替代临床决策记录和合规审计。

---

# Part A：Durable Execution

## 3. 恢复场景分类

### 3.1 用户跨轮继续

```text
Agent 提问
→ 保存 Checkpoint
→ Interrupt：AWAITING_USER
→ 用户关闭页面
→ 数小时后重新进入
→ 验证身份、Thread、Checkpoint 与 Consent
→ 提交回答
→ Resume
→ 继续问诊
```

### 3.2 医生审核后继续

```text
发现高风险或证据冲突
→ 写入 CDP 版本
→ 创建 ReviewTask
→ 保存 Checkpoint
→ Interrupt：AWAITING_CLINICIAN
→ 医生 approve / edit / reject / request_more_information
→ 创建 ReviewDecision
→ 验证 CDP 版本
→ Resume
```

### 3.3 服务故障后恢复

```text
节点 A 成功
→ Checkpoint N
→ 节点 B 调用工具时进程崩溃
→ Runtime 重启
→ 加载 Checkpoint N
→ 查询未完成 Run 与外部动作状态
→ 恢复节点 B 或进入降级路径
```

### 3.4 外部动作返回前故障

```text
预约系统已创建预约
→ Runtime 在保存响应前崩溃
→ 恢复时重新进入 action node
→ 使用 idempotency_key 查询 ExternalActionRecord / 外部系统
→ 返回已有 appointment_id
→ 不创建第二个预约
```

### 3.5 部署升级后恢复旧 Thread

```text
旧 Checkpoint：graph v1 / state schema v2
→ 新部署：graph v3 / state schema v5
→ Resume Compatibility Check
   ├── compatible：直接恢复
   ├── migratable：迁移后恢复
   ├── fallback：切换固定 Workflow
   └── non_resumable：人工接管
```

### 3.6 重复提交

```text
用户连续点击“提交”
→ 两个 resume 请求携带同一 resume_request_id
→ Inbox 去重
→ 只处理一次
→ 后续请求返回第一次处理结果
```

---

## 4. Thread 生命周期

建议使用以下状态：

```python
class ThreadStatus(str, Enum):
    CREATED = "created"
    RUNNING = "running"
    AWAITING_USER = "awaiting_user"
    AWAITING_ARTIFACT = "awaiting_artifact"
    AWAITING_CLINICIAN = "awaiting_clinician"
    RETRY_SCHEDULED = "retry_scheduled"
    SUSPENDED = "suspended"
    MIGRATION_REQUIRED = "migration_required"
    DEGRADED = "degraded"
    COMPLETED = "completed"
    CANCELLED = "cancelled"
    EXPIRED = "expired"
    FAILED_TERMINAL = "failed_terminal"
```

### 4.1 状态转换约束

- `RUNNING` 只能由持有有效 thread lease 的执行者进入；
- `AWAITING_USER` 必须关联有效 interrupt；
- `AWAITING_CLINICIAN` 必须关联 ReviewTask；
- `COMPLETED` 后不能继续普通 Resume，只能开启 Follow-up Encounter；
- `CANCELLED` 与 `EXPIRED` 不允许执行外部动作；
- `MIGRATION_REQUIRED` 必须先完成兼容性处理；
- `FAILED_TERMINAL` 必须包含人工处理或安全退出说明。

### 4.2 Thread 与 Encounter 的关系

推荐：

```text
一个 Encounter
→ 一个主要 thread_id
→ 可以有多个 run_id
→ 可以有多个 checkpoint_id
→ 可以有多个 interrupt_id
```

Follow-up 默认创建新的 Encounter 与 thread，但可以引用原 Encounter 和相关 Memory。

---

## 5. 标识体系

```python
class ExecutionIdentity(BaseModel):
    tenant_id: str
    organization_id: str
    patient_id: str
    encounter_id: str
    thread_id: str
    run_id: str
    checkpoint_id: str | None
    interrupt_id: str | None
    resume_request_id: str | None
    external_action_id: str | None
```

### 5.1 各 ID 职责

| ID | 职责 |
|---|---|
| encounter_id | 一次临床服务事件 |
| thread_id | 可持久化 Agent 会话 |
| run_id | 一次从开始/恢复到下次暂停或结束的运行 |
| checkpoint_id | 某个持久化执行快照 |
| interrupt_id | 等待用户、文件或医生输入的中断点 |
| resume_request_id | 一次恢复请求的幂等标识 |
| external_action_id | 一次不可逆业务动作 |
| trace_id | 一次技术调用链 |

`trace_id` 的生命周期通常短于 `thread_id`。同一个 thread 多次 Resume 会产生多个 trace。

---

## 6. CheckpointMetadata

```python
class CheckpointMetadata(BaseModel):
    checkpoint_id: str
    thread_id: str
    run_id: str
    sequence_no: int

    graph_name: str
    graph_version: str
    state_schema_version: str

    capability_id: str
    capability_version: str
    context_policy_version: str
    skill_release_ids: list[str]
    model_route_version: str
    safety_policy_version: str
    knowledge_release_id: str

    cdp_id: str
    cdp_version: int

    current_node: str
    next_nodes: list[str]
    interrupt_id: str | None

    status: str
    created_at: datetime
    expires_at: datetime | None

    state_hash: str
    context_hashes: list[str]
    created_by_runtime_version: str
```

### 6.1 Checkpoint 必须包含的最小信息

- Graph 状态；
- 当前节点和下一节点；
- interrupt 信息；
- CDP ID 与版本；
- Capability 与 Safety 版本；
- Graph 和 State Schema 版本；
- 执行预算与失败计数；
- 已完成但可能产生副作用的 action 引用；
- 状态 hash。

### 6.2 Checkpoint 不应保存

- 患者完整长期病历副本；
- 未脱敏的全量外部模型 Prompt；
- 模型私有 Chain of Thought；
- 将 Trace 当作 GraphState；
- 已经由 SourceArtifact 管理的大文件内容；
- 临床事实的第二份独立真值。

---

## 7. Checkpoint 写入边界

推荐至少在以下位置保存：

1. Encounter 初始化后；
2. 每次 State Committer 成功提交 CDP 后；
3. 每次 interrupt 前；
4. 每次人工审核任务创建后；
5. 每个不可逆 action node 执行前；
6. 每个不可逆 action node 成功后；
7. 切换 fallback 前；
8. Graph 正常结束前。

不建议：

- 每个 token 流式输出都保存 checkpoint；
- 每个无状态格式化步骤都写数据库；
- 将高频技术事件写入 checkpoint 表。

---

## 8. 节点可重入性

每个 LangGraph 节点必须声明：

```python
class NodeExecutionPolicy(BaseModel):
    node_id: str
    reentrant: bool
    deterministic_given_state: bool
    side_effect_free: bool
    requires_checkpoint_before: bool
    requires_checkpoint_after: bool
    idempotency_scope: str | None
    retry_policy_id: str | None
    compensation_policy_id: str | None
```

### 8.1 节点分类

#### Pure Node

- 只读取 State；
- 生成结构化结果；
- 不写外部系统；
- 可以安全重复。

示例：

- question ranking；
- Context Assembly；
- Model Route Decision。

#### Commit Node

- 通过 State Committer 更新 CDP；
- 使用 CDP 乐观锁；
- 重复执行应检测相同 StatePatch。

#### Interrupt Node

- 创建中断记录；
- interrupt 前不执行不可逆动作；
- 恢复后重新验证输入和状态。

#### Action Node

- 执行预约、转诊、通知等外部动作；
- 必须 checkpoint-before；
- 必须幂等；
- 必须保存 ExternalActionRecord。

---

## 9. Interrupt 设计规则

### 9.1 InterruptRecord

```python
class InterruptRecord(BaseModel):
    interrupt_id: str
    thread_id: str
    checkpoint_id: str
    interrupt_type: Literal[
        "user_question",
        "artifact_request",
        "clinician_review",
        "consent_request",
        "migration_required"
    ]

    requested_input_schema: dict
    display_payload: dict
    allowed_actor_types: list[str]

    status: Literal["open", "resolved", "cancelled", "expired"]
    created_at: datetime
    expires_at: datetime | None
    resolved_at: datetime | None
```

### 9.2 Interrupt 前置规则

- 临床状态先提交；
- checkpoint 成功后再向前端返回等待状态；
- 返回 `interrupt_id` 和公开的输入 Schema；
- 不向患者暴露内部 GraphState；
- 不在 interrupt 前调用不可逆外部动作；
- 超时策略必须明确。

### 9.3 Resume 时重新验证

- 当前 actor 是否有权解决该 interrupt；
- interrupt 是否仍为 open；
- 是否过期；
- resume payload 是否符合 Schema；
- CDP 是否已被其他流程修改；
- Capability、Consent 与 Safety Policy 是否仍有效；
- Graph 版本是否可恢复。

---

## 10. ResumeRequest

```python
class ResumeRequest(BaseModel):
    resume_request_id: str
    thread_id: str
    interrupt_id: str
    expected_checkpoint_id: str
    expected_cdp_version: int

    actor_id: str
    actor_type: Literal["patient", "caregiver", "clinician", "system"]
    payload: dict

    submitted_at: datetime
    client_request_id: str | None
```

### 10.1 Resume 处理顺序

```text
接收请求
→ 身份认证
→ Tenant / Patient / Role / Consent 校验
→ Inbox 幂等检查
→ 获取 Thread Lease
→ 加载 Thread 与 Interrupt
→ Checkpoint 兼容性检查
→ CDP 版本检查
→ Payload Schema 校验
→ 写入用户/医生输入的 SourceArtifact 与 Observation
→ 标记 Interrupt resolved
→ 创建新 run_id
→ Resume Graph
→ 释放 Lease
```

### 10.2 冲突处理

若 `expected_cdp_version` 落后：

- 不直接覆盖；
- 返回 `CDP_VERSION_CONFLICT`；
- 重新加载最新状态；
- 必要时重新生成问题或要求医生重新审核。

---

## 11. Thread Lease 与并发控制

### 11.1 为什么需要 Lease

同一 thread 可能同时收到：

- 患者重复提交；
- 医生审核提交；
- 后台 retry worker；
- 超时恢复任务；
- 多实例 Runtime 抢占。

### 11.2 Lease 结构

```python
class ThreadLease(BaseModel):
    thread_id: str
    lease_owner: str
    lease_token: str
    acquired_at: datetime
    expires_at: datetime
    heartbeat_at: datetime
```

### 11.3 推荐实现

- Redis：短期 lease 和 heartbeat；
- PostgreSQL：thread 状态与最终一致性；
- lease 有短过期时间；
- 长节点定期 heartbeat；
- 失联后允许其他 worker 接管；
- 接管前检查当前 run 和 ExternalActionRecord。

### 11.4 乐观锁

CDP、ReviewTask、Interrupt 和 ExternalActionRecord 都应包含版本字段：

```sql
UPDATE cdp
SET version = version + 1, ...
WHERE cdp_id = :id AND version = :expected_version;
```

更新行数为 0 时进入冲突处理，不重试覆盖。

---

## 12. Graph 与 State Schema 版本兼容

### 12.1 ResumeCompatibilityDecision

```python
class ResumeCompatibilityDecision(BaseModel):
    decision: Literal[
        "compatible",
        "migrate_state",
        "legacy_runtime",
        "fallback_workflow",
        "human_handoff",
        "non_resumable"
    ]

    source_graph_version: str
    target_graph_version: str
    source_state_schema_version: str
    target_state_schema_version: str

    migration_ids: list[str]
    reason_codes: list[str]
```

### 12.2 兼容策略

#### Compatible

节点重命名、字段新增等不影响旧 State 的修改可以直接恢复。

#### Migrate State

通过版本化迁移函数将旧 GraphState 转成新 Schema：

```python
def migrate_v2_to_v3(old_state: dict) -> dict:
    ...
```

迁移必须：

- 纯函数优先；
- 有测试；
- 记录迁移前后 hash；
- 不静默修改临床事实；
- 失败时可回退。

#### Legacy Runtime

短期保留旧 Runtime 镜像，仅用于完成存量 thread。

#### Fallback Workflow

若 Agent Graph 无法安全恢复，转换成固定 Workflow 或人工流程。

#### Non-resumable

对于已撤销 Consent、Capability 被紧急下线或状态不可验证的会话，安全终止并创建人工任务。

---

## 13. Checkpoint Migration Registry

```python
class CheckpointMigration(BaseModel):
    migration_id: str
    from_graph_version: str
    to_graph_version: str
    from_state_schema_version: str
    to_state_schema_version: str

    migration_handler: str
    reversible: bool
    test_suite_id: str
    owner: str
    status: Literal["draft", "approved", "deprecated"]
```

发布新 Graph 前必须回答：

- 旧 thread 能否恢复；
- 支持多少历史版本；
- 何时停止旧版本支持；
- 不可迁移 thread 如何处理；
- 是否需要医生重新审核。

---

## 14. CDP、Checkpoint 与业务事务一致性

### 14.1 不使用跨服务分布式事务作为默认方案

推荐：

```text
Spring Boot / Agent Runtime 业务事务
→ 写 CDP / ReviewTask / Interrupt
→ 同一事务写 Outbox Event
→ 提交
→ Outbox Publisher 发送事件
→ Consumer 使用 Inbox 去重
→ 驱动 Graph / 通知 / 外部动作
```

### 14.2 Transactional Outbox

```python
class OutboxEvent(BaseModel):
    event_id: str
    aggregate_type: str
    aggregate_id: str
    aggregate_version: int
    event_type: str
    payload: dict
    created_at: datetime
    published_at: datetime | None
    status: str
```

适合事件：

- `cdp_committed`；
- `interrupt_opened`；
- `review_task_created`；
- `review_decision_recorded`；
- `external_action_requested`；
- `thread_resume_requested`；
- `follow_up_due`。

### 14.3 Inbox

Consumer 保存已处理 `event_id` 或 `resume_request_id`，重复投递直接返回已处理结果。

### 14.4 一致性优先级

临床状态提交成功、Checkpoint 失败时：

- 不回滚已确认的临床事实；
- 创建 reconciliation 任务；
- 根据 CDP 最新版本重建可恢复状态；
- 未确认恢复前不执行不可逆动作。

Checkpoint 成功、CDP 提交失败时：

- 当前 checkpoint 不得作为可执行状态；
- 标记 `INVALID_CDP_REFERENCE`；
- 回退上一有效 checkpoint。

---

## 15. ExternalActionRecord

```python
class ExternalActionRecord(BaseModel):
    action_id: str
    action_type: str
    thread_id: str
    run_id: str
    checkpoint_id: str

    idempotency_key: str
    target_system: str
    request_hash: str

    status: Literal[
        "prepared",
        "submitted",
        "succeeded",
        "failed_retryable",
        "failed_permanent",
        "unknown",
        "cancelled",
        "compensated"
    ]

    external_reference_id: str | None
    response_summary: dict | None
    attempt_count: int
    last_attempt_at: datetime | None

    requires_human_review: bool
    approved_review_id: str | None
```

### 15.1 动作执行协议

```text
prepare action
→ 生成 idempotency_key
→ checkpoint-before
→ 保存 ExternalActionRecord(prepared)
→ 权限和审核
→ 调用外部系统
→ 保存 external_reference_id
→ ExternalActionRecord(succeeded)
→ checkpoint-after
```

### 15.2 状态 unknown

外部系统超时不等于动作失败。

若请求已发送但未收到结果：

- 标记 `unknown`；
- 先调用外部查询接口；
- 不能直接重试创建；
- 无查询能力时进入人工核对。

### 15.3 补偿而非回滚

预约、通知和转诊通常无法数据库回滚，应定义补偿动作：

- 取消预约；
- 撤销草稿；
- 发送更正通知；
- 关闭转诊任务。

高风险动作的补偿也可能需要人工审核。

---

## 16. Retry 与 Resume 的区别

### Retry

同一 run 内对临时技术失败再次尝试：

- 网络超时；
- 429；
- 临时 5xx；
- 格式化失败。

### Resume

run 已结束或暂停后，从持久化 checkpoint 开启新 run：

- 用户回答；
- 医生审核；
- 服务重启；
- 定时任务到期；
- 人工修复后继续。

### 16.1 禁止无限重试

每个 Tool、Model、Node 和 Thread 都应有：

- 单次最大尝试；
- 总失败预算；
- 指数退避；
- 熔断；
- 无进展检测；
- fallback；
- 人工升级。

---

## 17. 恢复安全与鉴权

Resume 不只验证 token，还要验证：

- tenant_id；
- organization_id；
- patient_id；
- actor 与患者关系；
- 医生执业/科室权限；
- interrupt 类型；
- ConsentScope；
- ReviewTask 状态；
- Capability 是否仍启用；
- resume_token 是否过期；
- CDP 版本；
- 数据驻留和机构策略。

### 17.1 Resume Token

Resume token 应：

- 只引用 interrupt，不直接携带敏感 GraphState；
- 短期有效；
- 可撤销；
- 与 actor 或访问场景绑定；
- 防重放；
- 在服务端验证。

### 17.2 紧急下线

当某 Capability、Skill、模型或 Safety Rule 出现严重问题时：

- 禁止存量 thread 继续自动执行相关路径；
- 将 thread 标记为 `SUSPENDED` 或 `MIGRATION_REQUIRED`；
- 根据风险转人工或固定 Workflow；
- 记录安全事件。

---

## 18. 过期、取消与保留

### 18.1 ExpirationPolicy

```python
class ThreadRetentionPolicy(BaseModel):
    thread_type: str
    interrupt_ttl: timedelta
    checkpoint_retention: timedelta
    completed_thread_retention: timedelta
    clinical_record_retention_policy_id: str
    audit_retention_policy_id: str
```

### 18.2 过期处理

- 普通信息补充超时：结束或重新创建 Encounter；
- 高风险审核超时：升级队列，绝不自动批准；
- Consent 请求超时：停止数据读取；
- 文件上传请求超时：保留已确认事实并安全结束；
- Checkpoint 过期不代表临床记录删除。

### 18.3 取消

用户取消后：

- 停止 Graph 新执行；
- 取消未提交的动作；
- 已执行外部动作按业务规则处理；
- 保留必要临床和审计记录；
- 删除或到期清理临时 Context、缓存和 Resume Token。

---

## 19. Reconciliation Jobs

至少建设：

### 19.1 Stuck Thread Scanner

寻找长期 `RUNNING`、lease 已过期的 thread。

### 19.2 Dangling Interrupt Scanner

寻找 open interrupt 但 thread 状态不匹配的记录。

### 19.3 Orphan ReviewTask Scanner

寻找 ReviewTask 没有关联有效 checkpoint 的情况。

### 19.4 Unknown External Action Scanner

查询状态为 `unknown` 的预约、通知和转诊动作。

### 19.5 Checkpoint-CDP Consistency Scanner

验证 checkpoint 引用的 CDP 版本是否存在且 hash 一致。

### 19.6 Outbox Lag Scanner

监控未发布或反复失败的 Outbox 事件。

所有自动修复必须：

- 幂等；
- 有 AgentEvent；
- 有技术 Trace；
- 高风险场景转人工；
- 不静默修改临床事实。

---

## 20. Durable Execution API

```text
POST /api/v1/encounters/{encounterId}/threads
GET  /api/v1/threads/{threadId}
GET  /api/v1/threads/{threadId}/interrupts/current
POST /api/v1/threads/{threadId}/resume
POST /api/v1/threads/{threadId}/cancel
POST /api/v1/threads/{threadId}/suspend
POST /api/v1/threads/{threadId}/migrate
GET  /api/v1/threads/{threadId}/checkpoints
GET  /api/v1/threads/{threadId}/runs
GET  /api/v1/external-actions/{actionId}
POST /api/v1/admin/threads/{threadId}/reconcile
```

### 20.1 API 要求

- 认证与 RBAC；
- Consent；
- 幂等 Header；
- expected version；
- trace context；
- 审计；
- 明确错误码；
- 不返回内部敏感 State。

---

## 21. 推荐数据表

```text
agent_thread
agent_run
agent_checkpoint
agent_interrupt
thread_lease_history
resume_request_inbox
outbox_event
external_action_record
checkpoint_migration
reconciliation_job
```

业务表继续包括：

```text
encounter
cdp
clinical_observation
review_task
review_decision
source_artifact
```

Checkpoint 表、技术 Trace 后端和 Audit Store 不应共用同一职责模型。

---

# Part B：Trace、Log 与决策记录

## 22. 四类观测与记录

### 22.1 Technical Telemetry

回答：

- 哪个服务慢；
- 哪个请求失败；
- 哪个数据库调用超时；
- CPU、内存和队列是否异常。

使用：OpenTelemetry Trace、Metric、Log。

### 22.2 Agent Execution Trace

回答：

- Graph 进入哪个节点；
- 为什么走该分支；
- 选择哪个 Skill、Tool 和模型；
- 是否 retry、fallback 或 resume；
- 预算如何变化。

使用：结构化 AgentEvent，并与 OTel trace_id 关联。

### 22.3 Clinical Decision Record

回答：

- 哪些 ClinicalObservation 被采用；
- 哪些规则触发分诊；
- 哪些证据支持或反对候选；
- 医生修改了什么；
- 最终结果基于哪个 CDP 版本。

它是受权限控制的临床解释记录，不是普通技术日志。

### 22.4 Compliance Audit

回答：

- 谁访问了患者数据；
- 谁修改了长期状态；
- 谁执行或批准高风险动作；
- 谁导出了数据；
- 使用了什么权限和 Consent。

Audit 需要不可抵赖、独立权限和更长保留策略。

---

## 23. Trace 与 Log 的区别

| 维度 | Trace | Log |
|---|---|---|
| 基本单位 | Span 与父子链路 | 单条事件记录 |
| 主要问题 | 请求如何跨组件执行 | 某时刻发生了什么 |
| 关联方式 | trace_id / span_id | 时间、字段，可携带 trace_id |
| 适合 | 延迟、调用链、依赖、错误传播 | 错误详情、状态变化、调试信息 |
| 恢复来源 | 否 | 否 |
| PHI 策略 | 默认不放原文 | 默认不放原文 |

### 23.1 示例 Trace

```text
Trace: encounter turn
├── business-api POST /message
├── agent-runtime resume
│   ├── load_checkpoint
│   ├── validate_resume
│   ├── assemble_context
│   ├── mandatory_safety_check
│   ├── retrieve_evidence
│   ├── model_call
│   └── commit_state
└── business-api response
```

### 23.2 示例 Log

```json
{
  "level": "ERROR",
  "event": "checkpoint_write_failed",
  "trace_id": "...",
  "span_id": "...",
  "thread_id": "...",
  "run_id": "...",
  "error_code": "POSTGRES_TIMEOUT",
  "retryable": true
}
```

---

## 24. Checkpoint、Trace、Log、AgentEvent、Clinical Record 与 Audit 对比

| 类型 | 是否持久化业务状态 | 是否支持恢复 | 是否允许包含临床原文 | 主要用户 |
|---|---:|---:|---:|---|
| Checkpoint | GraphState | 是 | 最小化 | Runtime |
| Trace | 否 | 否 | 默认否 | 开发/运维 |
| Log | 否 | 否 | 默认否 | 开发/运维 |
| AgentEvent | 决策事件 | 间接 | 不保存不必要原文 | Agent 开发/测试 |
| ClinicalDecisionRecord | 临床依据 | 否 | 受控可引用来源 | 医生/临床审核 |
| Audit | 访问与变更记录 | 否 | 最小必要信息 | 合规/安全 |

### 24.1 关键禁止项

- 不从 Log 重建患者事实；
- 不从 Trace 恢复 Graph；
- 不把 Checkpoint 当审计；
- 不将 OTel Span 当正式临床记录；
- 不把 Clinical Decision Record 无限制开放给调试人员；
- 不在 Audit 中保存完整模型 Prompt。

---

## 25. 开源技术栈

### 25.1 应用侧

- Java：OpenTelemetry Java Agent 或 SDK；
- Python/FastAPI：OpenTelemetry Python SDK 与 instrumentation；
- HTTP/gRPC/数据库/Redis：标准 instrumentation；
- LangGraph 节点：自定义 Span 与 AgentEvent；
- LLM/Embedding/Retrieval：使用 OpenTelemetry GenAI 语义约定可表达的字段，并保留项目自定义字段。

### 25.2 Collector

OpenTelemetry Collector 负责：

- OTLP 接收；
- 批处理；
- 重试；
- 属性清洗；
- PHI 字段删除；
- Tail Sampling；
- 导出到多个后端。

应用不直接绑定 Tempo、Jaeger 或商业 APM SDK。

### 25.3 Trace 后端

#### 本地开发

- Jaeger：部署简单、适合查看分布式 Trace。

#### 目标环境

- Grafana Tempo：适合与 Prometheus、Loki、Grafana 联动。

只选择一个主 Trace 后端，避免重复维护。

### 25.4 Metric

- Prometheus；
- Grafana；
- Alertmanager。

### 25.5 Log

可选：

- Loki：与 Grafana/Tempo 联动方便；
- OpenSearch：需要较强全文检索、日志分析和独立权限时使用。

项目早期不必同时维护 Loki 和 OpenSearch。

### 25.6 Agent/RAG 可视化

可选 Phoenix：

- LLM Trace；
- RAG 检索；
- Tool/Agent 调试；
- Evaluation。

限制：

- Phoenix 不是 Clinical Decision Record；
- Phoenix 不是 Compliance Audit；
- 不默认上传完整 PHI；
- 生产部署必须评估数据保留和权限。

### 25.7 不建议的做法

- 继续自研完整分布式 Trace 后端；
- 让 execution-trace-service 同时承担 APM、临床记录和审计；
- 每个服务使用不同 Trace SDK；
- 把所有模型输入输出无差别存入第三方平台；
- 同时部署多个功能重叠的 LLM Observability 平台。

---

## 26. OTel Span 设计

### 26.1 顶层 Span

```text
HTTP request / scheduled resume / review resume
```

### 26.2 Agent Span

```text
agent.run
agent.load_checkpoint
agent.validate_resume
agent.node
agent.interrupt
agent.resume
agent.fallback
agent.reconcile
```

### 26.3 Tool 与模型 Span

```text
tool.call
model.inference
embedding.generate
rag.retrieve
rag.rerank
db.query
external.action
```

### 26.4 推荐属性

```text
service.name
service.version
deployment.environment
trace_id
thread_id
run_id
checkpoint_id
encounter_id
cdp_id
cdp_version
capability_id
capability_version
graph_version
graph_node
skill_id
tool_id
model_id
prompt_version
knowledge_release_id
retry_count
resume_reason
```

敏感 ID 使用内部标识或 hash，不记录姓名、身份证和完整病历。

---

## 27. AgentEvent

```python
class AgentEvent(BaseModel):
    event_id: str
    event_type: str

    trace_id: str
    span_id: str | None
    thread_id: str
    run_id: str
    checkpoint_id: str | None

    node_id: str | None
    skill_id: str | None
    tool_id: str | None
    model_id: str | None

    reason_codes: list[str]
    input_refs: list[str]
    output_refs: list[str]

    occurred_at: datetime
    payload: dict
```

事件示例：

- `thread_resumed`；
- `resume_rejected`；
- `checkpoint_created`；
- `checkpoint_migrated`；
- `interrupt_opened`；
- `interrupt_resolved`；
- `node_retried`；
- `fallback_activated`；
- `external_action_deduplicated`；
- `reconciliation_started`；
- `reconciliation_completed`。

AgentEvent 应保存结构化 reason code，而不是模型长篇自由反思。

---

## 28. Log 规范

### 28.1 结构化 JSON

所有服务使用统一字段：

```json
{
  "timestamp": "...",
  "level": "INFO",
  "service": "agent-runtime",
  "event": "thread_resumed",
  "trace_id": "...",
  "span_id": "...",
  "thread_id": "...",
  "run_id": "...",
  "error_code": null,
  "duration_ms": 123
}
```

### 28.2 日志级别

- DEBUG：仅开发环境，禁止完整 PHI；
- INFO：生命周期和关键运行状态；
- WARN：可恢复异常、版本兼容、降级；
- ERROR：当前操作失败；
- FATAL/CRITICAL：影响安全或大面积服务。

### 28.3 Log 不记录

- 患者姓名、证件号；
- 完整对话；
- 完整报告；
- 完整 Prompt；
- 模型私有推理；
- API 密钥；
- Resume Token；
- 未脱敏外部响应。

### 28.4 需要原始内容时

通过受权限控制的：

- SourceArtifact；
- ClinicalDecisionRecord；
- ReviewTask；
- EvidencePack。

不通过普通日志查看。

---

## 29. Trace 与 Log 关联

日志必须自动注入：

```text
trace_id
span_id
```

同时允许通过：

```text
thread_id
run_id
checkpoint_id
encounter_id
```

跨多个 Resume 查询同一问诊。

典型排障流程：

```text
告警
→ Metric
→ 定位 Trace
→ 查看失败 Span
→ 跳转关联 Log
→ 查看 AgentEvent
→ 必要时查看 ClinicalDecisionRecord
→ 若涉及权限或修改，再查看 Audit
```

---

## 30. Sampling

### 30.1 技术 Trace Sampling

低风险正常请求可采样，高风险和错误请求优先保留。

Tail Sampling 条件建议：

- ERROR；
- emergency / urgent；
- human_review；
- fallback；
- resume failure；
- checkpoint migration；
- external action unknown；
- latency 超阈值；
- 安全策略拒绝。

### 30.2 AgentEvent 与 Audit 不按普通 Trace 比例采样

- 关键 Agent 决策事件应完整保存；
- 合规 Audit 按法规和机构策略保存；
- 不能因为 Trace Sampling 丢失高风险决策记录。

---

## 31. PHI 与遥测治理

### 31.1 数据分类

```text
Public Metadata
Operational Metadata
Pseudonymous Identifier
Clinical Sensitive Data
Direct Identifier
Secret
```

### 31.2 Collector 处理

Collector Processor 负责：

- 删除禁止字段；
- hash patient_id；
- 截断异常大属性；
- 过滤 Prompt/Response；
- 按环境路由；
- 记录违规遥测事件。

### 31.3 PHI Scanner

CI 和运行环境增加：

- 日志样本扫描；
- Trace 属性扫描；
- Prompt 记录策略测试；
- 第三方导出检查；
- 数据保留验证。

---

## 32. Metric 与告警

### 32.1 Durable Execution 指标

- active_threads；
- awaiting_user_threads；
- awaiting_clinician_threads；
- stuck_threads；
- resume_success_rate；
- resume_rejection_rate；
- resume_latency；
- duplicate_resume_rate；
- checkpoint_write_failure_rate；
- checkpoint_migration_rate；
- checkpoint_migration_failure_rate；
- cdp_version_conflict_rate；
- lease_contention_rate；
- outbox_lag；
- reconciliation_jobs；
- unknown_external_actions。

### 32.2 Trace 和 Log 指标

- trace_export_failure；
- collector_queue_size；
- dropped_spans；
- log_ingestion_lag；
- PHI_violation_count；
- span_attribute_oversize；
- cross_service_trace_completeness。

### 32.3 告警

高优先级：

- Checkpoint 持续写入失败；
- 高风险 ReviewTask 无法恢复；
- ExternalAction `unknown` 超时；
- 大量 thread 卡在 RUNNING；
- Outbox 堆积；
- 跨患者 Thread/Context 异常；
- PHI 写入遥测；
- 高风险请求 Trace 传播中断。

---

# Part C：Replay

## 33. 三类 Replay

### 33.1 State Resume

生产流程从有效 checkpoint 继续，可能产生新的外部动作。

要求：

- 严格鉴权；
- 并发锁；
- 版本兼容；
- 幂等。

### 33.2 Simulation Replay

在隔离环境中使用同一输入和版本重新运行：

- 对比模型；
- 对比 Prompt；
- 对比 Question Policy；
- 对比 Skill；
- 对比 Knowledge Release。

禁止连接真实外部动作系统。

### 33.3 Forensic Replay

还原当时决策条件：

- CDP 版本；
- ContextEnvelope hash；
- Prompt 版本；
- 模型版本；
- Tool 版本；
- Skill Release；
- Capability；
- Safety Policy；
- Knowledge Release；
- 医生审核结果。

目标是可解释和可追责，不要求大模型逐字确定性复现。

---

## 34. Replay Sandbox

Simulation/Forensic Replay 必须：

- 默认只读生产快照；
- 禁止调用真实预约、通知和转诊；
- 使用 mock 或 sandbox connector；
- 产生新的 replay_run_id；
- 不修改原 CDP；
- 输出对比报告；
- 记录使用的数据和版本。

---

## 35. Replay 对比

```python
class ReplayComparison(BaseModel):
    source_run_id: str
    replay_run_id: str

    route_changes: list[str]
    question_changes: list[str]
    hypothesis_changes: list[str]
    triage_changes: list[str]
    evidence_changes: list[str]
    safety_changes: list[str]
    cost_delta: float
    latency_delta_ms: int

    regression_flags: list[str]
```

高风险变化必须人工评审。

---

# Part D：测试与验收

## 36. Durable Execution 单元测试

- Thread 状态转换；
- Interrupt 状态；
- Resume Schema；
- Lease 过期；
- CDP 乐观锁；
- 幂等键；
- Checkpoint 迁移；
- ExternalAction 状态机；
- Outbox/Inbox 去重；
- ExpirationPolicy。

---

## 37. 故障注入测试

### 37.1 节点故障

- Context Assembly 后崩溃；
- 模型返回后、State Commit 前崩溃；
- CDP 提交后、Checkpoint 前崩溃；
- Checkpoint 后、响应前崩溃。

### 37.2 外部动作故障

- 请求发送前失败；
- 请求发送后超时；
- 外部成功、本地记录失败；
- 重复恢复；
- 外部查询接口不可用。

### 37.3 数据库和基础设施

- PostgreSQL 主从切换；
- Redis lease 丢失；
- Outbox Publisher 停止；
- Collector 不可用；
- Trace 后端不可用；
- Log 后端延迟。

Collector/Trace 故障不能阻塞临床安全路径；Checkpoint 故障则必须阻止不可逆动作。

---

## 38. 并发测试

- 患者双击提交；
- 患者和医生同时提交；
- 两个 Runtime 实例抢占；
- retry worker 与人工恢复同时发生；
- ReviewTask 被两位医生同时处理；
- 过期任务与 Resume 同时发生。

验收：

- 不重复写临床事实；
- 不重复创建外部动作；
- 冲突可解释；
- 所有拒绝有 reason code。

---

## 39. 版本迁移测试

- 当前版本直接恢复；
- 增加可选字段；
- 字段重命名；
- 节点拆分；
- Skill 替换；
- Capability 下线；
- Safety Rule 紧急升级；
- 不可迁移 State 转人工。

---

## 40. 可观测性测试

- Java → Python trace context 传播；
- Runtime → Tool trace context；
- Trace 与 Log 关联；
- AgentEvent 与 trace_id 关联；
- Resume 产生新 trace、保留同一 thread；
- PHI 不进入遥测；
- Collector 过滤有效；
- 高风险/error Tail Sampling 保留；
- Trace 后端故障不阻塞请求。

---

## 41. 恢复临床场景

### 场景 A：患者回答后服务崩溃

- 回答已保存为 SourceArtifact；
- Observation 已提交；
- Graph 可从下一安全点继续；
- 不重复问相同问题。

### 场景 B：医生修改 DDx 后恢复

- 医生修改产生新 CDP 版本；
- 旧 checkpoint 检测版本变化；
- 使用 ReviewDecision 恢复；
- 不覆盖医生修改。

### 场景 C：紧急分诊通知

- 通知动作幂等；
- 服务崩溃后不重复发送误导性通知；
- 若状态 unknown，进入人工核对；
- 安全事件完整记录。

---

# Part E：与 Phase 0～6 整合

## 42. Phase 0 增量任务

1. 盘点当前 execution-trace-service；
2. 区分现有 Trace、Log、业务事件和 Audit；
3. 盘点所有不可逆动作；
4. 盘点当前 Thread/Session/Workflow 状态；
5. 识别现有重复提交风险；
6. 定义首个 E2E 的故障恢复点；
7. 定义遥测 PHI 禁止字段。

### 验收

- 现有记录职责分类完成；
- 所有外部动作列出幂等方案；
- 一条固定 Workflow 能在模拟故障后恢复或安全终止；
- Trace/Log 不再被描述为状态恢复来源。

---

## 43. Phase 1 增量任务：Durable Contracts

新增：

- ThreadStatus；
- AgentRun；
- CheckpointMetadata；
- InterruptRecord；
- ResumeRequest；
- ThreadLease；
- ExternalActionRecord；
- OutboxEvent；
- ResumeCompatibilityDecision；
- CheckpointMigration。

### 验收

- Java/Python Schema 一致；
- 所有 Schema 有版本；
- Resume 有幂等字段；
- ExternalAction 有状态机；
- Checkpoint 能关联 CDP 与版本链。

---

## 44. Phase 2 增量任务：Checkpoint 与幂等 MVP

1. PostgreSQL Checkpointer；
2. thread/run/interrupt 表；
3. Resume API；
4. Redis Thread Lease；
5. CDP 乐观锁；
6. 一个 ExternalActionRecord 示例；
7. 基础 Outbox/Inbox；
8. 故障注入测试。

### 首个 MVP 场景

成人呼吸道问诊：

```text
提问
→ 用户离开
→ 重启服务
→ 用户回答
→ 恢复问诊
→ 最终生成医生交接包
```

验收：

- 不丢已确认 Observation；
- 不重复提问；
- 重复 Resume 只处理一次；
- 服务重启后可继续。

---

## 45. Phase 3 增量任务：LangGraph Durable Runtime

新增节点/服务：

```text
load_checkpoint
validate_resume
acquire_thread_lease
check_resume_compatibility
migrate_checkpoint
record_interrupt
resolve_interrupt
prepare_external_action
execute_external_action
reconcile_execution
```

同时实现：

- action node 规范；
- checkpoint-before/after；
- ReviewTask Resume；
- fallback 转换；
- AgentEvent。

---

## 46. Phase 4 增量任务：医生与运营恢复界面

医生端：

- 查看 thread 当前状态；
- 查看等待原因；
- 查看 checkpoint 与 CDP 版本；
- 提交 ReviewDecision；
- 处理冲突；
- 查看恢复结果。

运营端：

- Stuck Thread；
- Unknown External Action；
- Outbox Lag；
- Migration Required；
- Reconciliation 操作；
- Forensic Review 入口。

不得提供“强制跳过安全节点”的按钮。

---

## 47. Phase 5 增量任务：开源可观测栈

1. Java/Python OTel SDK 或自动 instrumentation；
2. OTel Collector；
3. Tempo 或 Jaeger；
4. Prometheus/Grafana；
5. Loki 或 OpenSearch；
6. Trace/Log correlation；
7. AgentEvent Store；
8. ClinicalDecisionRecord；
9. Audit Store；
10. PHI Collector Processor；
11. Sampling；
12. Dashboard、Alert 和 Runbook。

### 验收

- Java/Python/Tool Trace 可关联；
- Log 可通过 trace_id 跳转；
- 同一 thread 多次 Resume 可查询；
- Trace 后端故障不阻塞临床请求；
- Checkpoint 故障会阻止不可逆动作；
- 遥测中无禁止 PHI。

---

## 48. Phase 6 增量任务：分阶段验证

### Stage 0：离线

- Crash Matrix；
- Double Submit；
- Migration；
- Action Unknown；
- Replay Sandbox；
- PHI Telemetry。

### Stage 1：影子

- 记录 checkpoint 与 AgentEvent；
- 不改变真实流程；
- 对比恢复决策。

### Stage 2：医生辅助

- 启用 ReviewTask Resume；
- 所有外部动作由医生批准；
- 监控冲突和重复动作。

### Stage 3：受限患者

- 开放跨轮用户恢复；
- 只开放已验证的低风险流程；
- 高风险继续人工审核。

### Stage 4：扩大

每次扩大要求：

- Graph/State Migration Plan；
- Resume Regression；
- External Action Idempotency Test；
- OTel Dashboard；
- Incident Runbook；
- Clinical Approval。

---

# Part F：首批实施 PR

## 49. PR-K：Durable Execution Contracts

范围：

- Thread、Run、CheckpointMetadata；
- InterruptRecord；
- ResumeRequest；
- ExternalActionRecord；
- JSON Schema 与契约测试。

不包含完整 LangGraph 改造。

---

## 50. PR-L：PostgreSQL Checkpointer 与 Resume MVP

范围：

- Checkpointer；
- Thread/Run/Interrupt 存储；
- Resume API；
- 成人呼吸道跨轮恢复 E2E。

---

## 51. PR-M：Lease、Outbox 与幂等动作

范围：

- Redis Thread Lease；
- Resume Inbox；
- Transactional Outbox；
- ExternalActionRecord；
- 一个模拟预约动作。

---

## 52. PR-N：OpenTelemetry 基线

范围：

- Java/Python trace context；
- OTel Collector；
- Jaeger 或 Tempo；
- Prometheus；
- 结构化 Log；
- trace_id/span_id 注入；
- PHI 过滤测试。

---

## 53. PR-O：AgentEvent 与恢复 Dashboard

范围：

- AgentEvent；
- Resume/Checkpoint 指标；
- Stuck Thread Dashboard；
- Unknown Action Dashboard；
- Alert Rules；
- Runbook。

---

# Part G：ADR 增量

## 54. 新增 ADR

1. ADR-038：LangGraph Checkpoint 是执行恢复状态源；
2. ADR-039：CDP 与 Checkpoint 独立版本；
3. ADR-040：Interrupt 后节点按可重入语义设计；
4. ADR-041：外部动作使用幂等键与 ExternalActionRecord；
5. ADR-042：Thread 使用 Lease 与乐观锁控制并发；
6. ADR-043：业务一致性使用 Transactional Outbox/Inbox；
7. ADR-044：旧 Checkpoint 必须执行版本兼容检查；
8. ADR-045：技术可观测性采用 OpenTelemetry；
9. ADR-046：Trace、Log、AgentEvent、Clinical Record 与 Audit 分离；
10. ADR-047：Trace 和 Log 不作为恢复状态源；
11. ADR-048：不保存模型私有 Chain of Thought；
12. ADR-049：OpenTelemetry Collector 负责 PHI 属性清理；
13. ADR-050：Replay Sandbox 禁止真实外部副作用。

---

# Part H：Definition of Done

## 55. Durable Execution DoD

一个可恢复流程只有满足以下条件才能标记完成：

### 状态

- Thread 生命周期明确；
- Checkpoint 有 Graph/Schema/CDP 版本；
- Interrupt 可查询；
- Resume 幂等；
- CDP 冲突可处理。

### 副作用

- Action 独立；
- checkpoint-before/after；
- 幂等键；
- ExternalActionRecord；
- unknown 状态处理；
- 补偿策略。

### 安全

- Resume 鉴权；
- Consent；
- Capability 验证；
- 高风险人工审核；
- 过期和取消处理。

### 测试

- 服务重启；
- 重复提交；
- 并发恢复；
- 数据库故障；
- 外部动作超时；
- Graph 升级迁移；
- Fallback；
- 人工接管。

---

## 56. Observability DoD

- OTel Trace 跨 Java/Python/Tool；
- Log 结构化并携带 trace_id/span_id；
- AgentEvent 可关联 Thread/Run；
- ClinicalDecisionRecord 可关联 CDP；
- Audit 独立；
- PHI 过滤；
- Dashboard；
- Alert；
- Runbook；
- Trace 后端不可用不影响安全链路；
- Checkpoint 不可用时阻止不可逆动作。

---

## 57. 明确不做

当前阶段不建设：

- 文件传输级分片续传系统；
- 基于 Log 还原 GraphState；
- 基于 Trace 自动修改临床事实；
- 跨数据库两阶段提交作为默认方案；
- 无幂等保护的外部动作重试；
- 永久保存所有 checkpoint；
- 无版本兼容策略的 Graph 热更新；
- 将完整患者 Prompt 写入 Trace 平台；
- 保存模型私有 Chain of Thought；
- 同时部署多个重叠 APM/LLM Trace 平台；
- 让运营人员绕过 Safety 强制恢复。

---

## 58. 推荐实施顺序

```text
现有 Workflow E2E 与故障点盘点
→ Durable Contracts
→ PostgreSQL Checkpointer
→ 用户跨轮 Resume MVP
→ ReviewTask Resume
→ Thread Lease 与 CDP 乐观锁
→ Outbox/Inbox
→ External Action 幂等
→ OTel Collector + Trace/Metric/Log
→ AgentEvent / ClinicalDecisionRecord / Audit 分离
→ Checkpoint Migration
→ Replay 与 Reconciliation
```

第一阶段不要先做完整 Replay Console，也不要先接真实预约系统。

最合理的第一个可验证目标是：

> 成人呼吸道问诊在用户离开和服务重启后，可以从已保存的问题与临床证据继续；重复提交不会重复写入；所有恢复动作可通过 thread、run、checkpoint 和 trace 被定位；Trace 或日志系统故障不会破坏问诊状态。

---

## 59. 最终架构关系

```text
Patient / Clinician
       │
Business API
       │
Resume Auth / Thread Lease / Inbox
       │
LangGraph Durable Runtime
Checkpoint / Interrupt / Resume / Migration
       │
Context / Memory / Skills / Model Router
       │
Clinical Intelligence / Evidence Intelligence / Safety
       │
State Committer → Encounter CDP / Evidence Ledger
       │
Outbox → External Action Worker → ExternalActionRecord

并行观测：
OpenTelemetry Trace / Metric / Log
AgentEvent
ClinicalDecisionRecord
Compliance Audit
```

系统的恢复能力来自 Checkpoint、版本控制、幂等和一致性协议；系统的可观察能力来自 Trace、Metric、Log 和 AgentEvent；系统的临床可信度来自 Evidence Ledger、Clinical Decision Record、Safety 和医生审核。三者互相引用，但不能相互替代。
