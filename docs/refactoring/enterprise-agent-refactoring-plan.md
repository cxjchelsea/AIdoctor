# AIdoctor 企业级临床 Agent 重构方案

> 文档状态：Draft v1.0  
> 编写日期：2026-07-29  
> 适用仓库：`cxjchelsea/AIdoctor`  
> 目标读者：项目负责人、后端开发、Agent 开发、算法开发、测试与运维人员

---

## 1. 文档目的

本文档用于指导 AIdoctor 从当前“多服务 + 固定诊断 Workflow + 初步自研 Agent 循环”的实现，逐步重构为一个具备企业工程能力的**受约束临床决策支持 Agent 平台**。

本次重构不以“增加更多模型调用”或“拆出更多微服务”为目标，而以以下结果为核心：

1. 形成唯一、清晰、可维护的问诊主执行链路；
2. 实现多轮状态持久化、暂停、恢复、回放和人工审核；
3. 建立受约束的工具调用、状态更新和权限控制机制；
4. 建立真正有效的工具结果评估、重新规划、重试和降级闭环；
5. 建立医疗风险拦截、拒答、升级和审计机制；
6. 建立可观测、可测试、可部署、可回滚的企业级工程体系；
7. 使项目文档、实现状态和运行结果保持一致。

本文档既描述目标架构，也给出迁移阶段、目录规划、接口规范、验收标准和风险控制措施。

---

## 2. 项目重新定位

### 2.1 推荐定位

AIdoctor 应定位为：

> 面向社区与基层医疗场景的受约束临床决策支持 Agent。系统以临床决策包 CDP 维护患者状态、诊断候选、证据、不确定性和处置计划，以状态图管理多轮问诊执行过程，通过结构化医疗工具完成病例理解、信息缺口分析、主动问诊、鉴别诊断、证据检索、风险评估和建议生成，并通过规则门控、结果评估、人工审核、固定流程降级和全链路审计保证过程可恢复、可追踪、可解释。

### 2.2 不建议使用的定位

以下表述在现阶段不应作为项目对外承诺：

- 自动替代医生完成诊断；
- 专科医生级自主诊断系统；
- 已经具备完整自主规划和自主决策能力；
- 已经完成临床验证或医疗器械级合规；
- 所有服务均已完整实现；
- 所有异常场景均已覆盖。

### 2.3 产品边界

系统输出应属于“临床决策支持”和“就医建议”，而不是确定性医疗诊断。高风险场景、用药调整、急诊替代、治疗方案变更等动作必须进入规则拦截或人工审核路径。

---

## 3. 当前实现基线与主要问题

### 3.1 当前已有能力

当前仓库已经具备值得保留的基础：

- Spring Boot 诊断服务和 CDP 领域对象；
- 固定五阶段诊断 Workflow；
- 病例理解、主动问诊、诊断引擎、检查建议、治疗推理、风险评估和解释生成等工具服务；
- MySQL/Oracle、Redis、Neo4j 等基础设施接入；
- CDP 版本记录、审计轨迹和执行追踪设计；
- 自研 `Observe → Plan → Act → Update → Evaluate` Agent 循环骨架；
- React 前端和部分 Docker Compose 编排；
- 医疗知识图谱、多引擎融合和多模态处理方向。

这些内容说明项目已经完成了临床流程拆分和工程骨架建设，不需要从零重写。

### 3.2 当前核心问题

#### 3.2.1 存在多个潜在“主大脑”

当前同时存在：

1. Java 固定五步 `DiagnosisWorkflowOrchestrator`；
2. Java 自研 `AgentLoop`；
3. 计划引入的图式 Agent 编排。

如果三套编排同时发展，会导致：

- 状态来源不唯一；
- 路由规则互相冲突；
- 恢复和回放逻辑重复；
- 工具协议和异常处理不一致；
- 同一个业务修改需要同步维护多个执行引擎。

#### 3.2.2 新 Agent 骨架尚未形成公共主链路

新 Agent 已经实现循环、策略判断和工具选择骨架，但仍需解决：

- 与现有 Controller 和公开问诊接口的接入；
- CDPManager、CDP 实体方法和 Agent 调用之间的接口一致性；
- Agent 运行结果的持久化；
- 实际暂停、恢复、人工审核和回放；
- 真正的工具超时、重试和替代工具切换；
- 测试证明其能够端到端运行。

#### 3.2.3 固定 Workflow 尚未完全闭环

旧流程仍然是当前可见接口的核心路径，但部分响应解析、Step 3～5 数据回填和完整结果生成仍不够可靠。若基础 Workflow 本身无法稳定运行，直接接入动态 Agent 会放大错误。

#### 3.2.4 服务拆分超过当前工程成熟度

当前工具以多个独立服务存在，但部分服务只有基础实现。过早拆分会带来：

- 大量 HTTP 调用和序列化成本；
- DTO 和枚举漂移；
- 本地环境难以启动；
- 故障定位困难；
- 测试和部署复杂度远大于业务收益。

#### 3.2.5 工具协议仍缺少治理能力

当前虽然已经存在 `ToolContext` 和 `ToolResult`，但仍需统一：

- 输入输出 Schema；
- 可读和可写字段；
- 幂等性；
- 超时和重试策略；
- 工具风险等级；
- 人工审核要求；
- 备用工具；
- 状态更新审批；
- 版本兼容策略。

#### 3.2.6 结果评估仍偏静态

现有 Evaluate 更接近“字段是否存在”和“是否满足停止条件”，尚不能充分回答：

- 工具结果是否有效；
- 是否增加了有价值的临床信息；
- 是否存在证据冲突；
- 是否值得再次调用相同工具；
- 应追问用户、切换工具还是降级；
- 输出是否满足医疗安全要求。

#### 3.2.7 文档与实现状态存在偏差

README 中部分“完整实现”表述无法通过当前代码、接口和测试充分证明。企业级项目必须明确区分：

- 已实现并经过测试；
- 已实现基础框架；
- 部分实现；
- 设计完成但未接入；
- 计划实现。

---

## 4. 重构原则

### 4.1 单一执行引擎

主问诊流程只能有一个 Agent Runtime。固定 Workflow 作为降级路径，而不是并行发展的第二套大脑。

### 4.2 模块化单体优先

先在清晰模块边界下完成一条可靠主链路，再根据团队规模、性能、隔离和部署需求拆分微服务。

### 4.3 状态显式化

所有影响路由的状态必须进入结构化 GraphState 或 CDP，不能依赖日志、提示词上下文或隐式内存。

### 4.4 工具只能提出状态变更

工具不能直接任意修改 CDP。工具返回 `proposed_writes`，由 Agent Runtime 统一校验、授权、合并和持久化。

### 4.5 规则负责边界，Agent 负责有限决策

红旗、拒答、权限、预算、风险升级和高风险输出必须由确定性规则控制。模型只在安全边界内决定下一步。

### 4.6 先可验证，再智能化

优先级应为：

1. 可运行；
2. 可测试；
3. 可恢复；
4. 可观测；
5. 可降级；
6. 再提高自主规划能力。

### 4.7 文档必须与测试结果一致

任何“完整实现”“支持恢复”“支持回放”等声明，都必须有对应接口、测试用例和运行证据。

---

## 5. 目标总体架构

```text
┌────────────────────────────────────────────────────────────────────┐
│                        Patient / Clinician UI                      │
│              React Patient Web + Admin/Review Console             │
└───────────────────────────────┬────────────────────────────────────┘
                                │ HTTPS / SSE / WebSocket
                                ▼
┌────────────────────────────────────────────────────────────────────┐
│                    Business API / BFF (Spring Boot)                │
│                                                                    │
│  Auth  Patient  Session  Consent  Review Task  Audit  API Gateway  │
│  Rate Limit  Idempotency  Data Permission  Business Persistence    │
└───────────────────────────────┬────────────────────────────────────┘
                                │ Internal API / Event
                                ▼
┌────────────────────────────────────────────────────────────────────┐
│                Agent Runtime (Python + FastAPI + LangGraph)        │
│                                                                    │
│  Graph Routing    Checkpoint     Interrupt / Resume                │
│  Tool Registry    Policy Engine  Result Evaluator                  │
│  Replanner        Fallback       Prompt / Model Gateway            │
└──────────┬───────────────────┬───────────────────┬─────────────────┘
           │                   │                   │
           ▼                   ▼                   ▼
┌──────────────────┐ ┌──────────────────┐ ┌────────────────────────┐
│Clinical           │ │Clinical          │ │Care Planning           │
│Understanding      │ │Reasoning          │ │                        │
│                  │ │                  │ │Workup / Management     │
│Parse / Normalize │ │DDx / Retrieval   │ │Risk / Explanation     │
│Gap / Question    │ │Evidence / Conflict│ │Conclusion Package     │
└──────────┬───────┘ └─────────┬────────┘ └──────────┬─────────────┘
           │                   │                     │
           └───────────────────┼─────────────────────┘
                               ▼
┌────────────────────────────────────────────────────────────────────┐
│                         Data & Knowledge                            │
│ PostgreSQL/Oracle  Redis  Neo4j  Object Storage  Vector Index      │
└────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌────────────────────────────────────────────────────────────────────┐
│                         Observability                              │
│ OpenTelemetry  Metrics  Traces  Logs  Audit  Evaluation Dashboard │
└────────────────────────────────────────────────────────────────────┘
```

---

## 6. 组件职责划分

### 6.1 Spring Boot Business API

Spring Boot 不再承担 Agent 内部节点编排，主要负责企业业务能力：

- 用户、患者、角色和组织管理；
- 问诊会话创建和查询；
- 同意书、隐私授权和数据访问控制；
- 对外 REST API、SSE 和 WebSocket；
- 请求幂等、限流和审计；
- 人工审核任务创建、分配、处理和超时；
- 业务数据库写入；
- 调用 Agent Runtime；
- 对 Agent Runtime 进行熔断、超时和降级控制。

### 6.2 Agent Runtime

Agent Runtime 是唯一的问诊执行引擎，负责：

- 加载 GraphState 和 CDP；
- 运行状态图；
- 决定下一节点和可调用工具；
- 执行工具调用；
- 校验工具结果；
- 合并状态更新；
- 判断重试、切换工具、追问、停止、人工审核或降级；
- 保存 checkpoint；
- 支持 interrupt 和 resume；
- 输出流式进度事件；
- 记录运行指标和轨迹。

### 6.3 Clinical Understanding

将以下能力合并为一个清晰模块：

- 症状、时间、程度、诱因和伴随症状抽取；
- 医学概念归一化；
- 否定、既往史、用药史、过敏史和检查结果识别；
- 歧义识别；
- 信息缺口分析；
- 下一问生成；
- 问题去重和问题价值排序。

### 6.4 Clinical Reasoning

负责：

- 鉴别诊断候选集生成；
- 规则、知识图谱、统计模型和 LLM 推理结果融合；
- 医疗知识检索；
- 证据引用；
- 支持证据和反对证据计算；
- 诊断候选排序；
- 证据冲突识别；
- 不确定性更新。

### 6.5 Care Planning

负责：

- 检查建议；
- 处置和就医建议；
- 风险分层；
- 解释生成；
- 最终结论包构建；
- 输出安全检查；
- 随访建议。

### 6.6 Knowledge Service

负责统一访问：

- Neo4j 医疗知识图谱；
- 指南、共识和医学资料；
- 向量检索；
- 文档版本和知识版本；
- 证据来源和引用；
- 检索权限和数据隔离。

初期可作为 Agent Runtime 内部模块，达到独立扩缩容需求后再拆分。

---

## 7. 主状态图设计

### 7.1 主流程

```text
START
  │
  ▼
load_context
  │
  ▼
entry_safety_check
  ├── emergency ──► emergency_escalation ──► END
  ├── refuse ─────► refusal_response ──────► END
  └── continue
        │
        ▼
normalize_case
        │
        ▼
validate_case_state
        ├── invalid ──► repair_case_state ──┐
        └── valid                          │
               │                          │
               ▼                          │
identify_information_gaps ◄───────────────┘
        │
        ├── critical_gap ──► generate_question
        │                         │
        │                         ▼
        │                     interrupt
        │                         │ user answer / resume
        │                         └────────► normalize_case
        │
        └── sufficient
               │
               ▼
generate_differential_diagnosis
               │
               ▼
retrieve_and_fuse_evidence
               │
               ▼
evaluate_reasoning_result
        ┌──────┼──────────┬─────────────┐
        │      │          │             │
      accept retry      switch       ask_user
        │      │          │             │
        │      └──────────┴─────────────┘
        ▼
risk_gate
  ┌─────┼─────────────┐
  │     │             │
normal review       emergency
  │     │             │
  │     ▼             ▼
  │ human_review   emergency_escalation
  │     │ interrupt
  │     │ approve/edit/reject
  └─────┴─────────────┐
                      ▼
build_workup_and_management_plan
                      │
                      ▼
final_output_guard
        ┌─────────────┼──────────────┐
        │             │              │
      accept        repair         review
        │             │              │
        ▼             └───────┐      │
persist_conclusion            │      │
        │                      │      │
        ▼                      │      │
       END ◄───────────────────┴──────┘
```

### 7.2 节点设计规范

每个节点必须满足：

- 单一职责；
- 明确输入字段；
- 明确输出字段；
- 明确可能抛出的错误；
- 明确可重试性；
- 明确路由结果；
- 明确审计事件；
- 明确指标；
- 可单元测试；
- 不直接修改不属于自己的状态字段。

### 7.3 建议节点接口

```python
class NodeResult(BaseModel):
    status: Literal[
        "success",
        "partial_success",
        "retryable_failure",
        "permanent_failure",
        "needs_user_input",
        "needs_human_review"
    ]
    state_patch: dict
    route_hint: str | None = None
    errors: list[StructuredError] = []
    audit_events: list[AuditEvent] = []
```

---

## 8. CDP 与 AgentState 重构

### 8.1 CDP 只保存临床领域状态

```python
class ClinicalDecisionPackage(BaseModel):
    cdp_id: str
    patient_id: str
    session_id: str
    version: int
    status: str

    patient_state: PatientState
    diagnostic_hypotheses: list[DiagnosticHypothesis]
    evidence_items: list[EvidenceItem]
    uncertainty: UncertaintyState
    risk_assessment: RiskAssessment | None
    workup_plan: WorkupPlan | None
    management_plan: ManagementPlan | None
    final_conclusion: FinalConclusion | None

    created_at: datetime
    updated_at: datetime
```

### 8.2 AgentState 只保存执行状态

```python
class AgentState(TypedDict):
    thread_id: str
    cdp_id: str
    session_id: str
    turn_number: int

    current_phase: str
    current_node: str
    next_action: str | None

    missing_information: list[InformationGap]
    current_plan: list[PlannedAction]
    tool_history: list[ToolExecution]
    node_history: list[NodeExecution]

    consecutive_failures: int
    total_tool_calls: int
    total_model_calls: int
    elapsed_time_ms: int
    estimated_cost: float

    requires_human_review: bool
    review_task_id: str | None
    review_reason: str | None

    fallback_mode: bool
    fallback_reason: str | None

    messages: list
```

### 8.3 Checkpoint 与 CDP 版本的区别

| 对象 | 目的 | 保存时机 | 恢复用途 |
|---|---|---|---|
| CDP 当前版本 | 当前临床事实和结论 | 临床状态有效变更后 | 展示当前患者状态 |
| CDP 历史版本 | 临床数据版本审计 | 每次重要写入后 | 比较、审计和业务回退 |
| Graph Checkpoint | Agent 执行快照 | 每个关键节点后 | 中断恢复、故障恢复和回放 |
| Audit Event | 不可抵赖事件记录 | 每个敏感动作后 | 合规审计和责任追踪 |

禁止用 CDP 版本表替代 Graph Checkpoint，也禁止只依靠 checkpoint 代替业务版本记录。

---

## 9. 统一工具协议

### 9.1 ToolSpec

```python
class ToolSpec(BaseModel):
    tool_id: str
    name: str
    version: str
    description: str

    input_schema: dict
    output_schema: dict

    readable_fields: list[str]
    writable_fields: list[str]

    timeout_seconds: int
    max_retries: int
    retryable_errors: list[str]

    idempotent: bool
    risk_level: Literal["low", "medium", "high"]
    requires_human_review: bool

    fallback_tool_ids: list[str]
    enabled: bool
```

### 9.2 ToolContext

```python
class ToolContext(BaseModel):
    trace_id: str
    thread_id: str
    execution_id: str
    cdp_id: str
    cdp_version: int

    actor: ActorContext
    state_projection: dict
    request: dict

    deadline_at: datetime
    max_cost: float
    policy_snapshot: dict
```

`state_projection` 只能包含 ToolSpec 声明可读取的字段，禁止将完整 CDP 默认传给所有工具。

### 9.3 ToolResult

```python
class ToolResult(BaseModel):
    execution_id: str
    tool_id: str
    tool_version: str

    status: Literal[
        "success",
        "partial_success",
        "retryable_failure",
        "permanent_failure",
        "timeout"
    ]

    data: dict
    evidence: list[EvidenceReference]
    proposed_writes: list[StatePatch]

    confidence: float | None
    completeness: float | None
    information_gain: float | None

    validation_errors: list[str]
    warnings: list[str]
    retryable: bool
    duration_ms: int
```

### 9.4 状态写入流程

```text
ToolResult.proposed_writes
        │
        ▼
Schema Validator
        │
        ▼
Field Permission Validator
        │
        ▼
Clinical Invariant Validator
        │
        ▼
Conflict Detector
        │
        ├── conflict ──► review / replan
        └── valid
              │
              ▼
Optimistic Lock + Transaction
              │
              ▼
Persist CDP Version + Audit Event
```

### 9.5 工具注册中心

初期使用代码注册和配置文件组合：

```yaml
tools:
  clinical_parser:
    version: v1
    endpoint: http://agent-runtime/internal/tools/clinical-parser
    timeout_seconds: 15
    max_retries: 1
    risk_level: low
    readable_fields:
      - patient_state.raw_input
      - patient_state.history
    writable_fields:
      - patient_state.symptoms
      - patient_state.medications
      - patient_state.allergies
```

后期可接入配置中心，但不建议在第一阶段建设复杂动态插件市场。

---

## 10. 结果评估与重新规划

### 10.1 四层评估

#### 第一层：结构评估

- JSON 是否可解析；
- 是否符合 Pydantic/JSON Schema；
- 必填字段是否存在；
- 枚举值是否合法；
- 引用和 ID 格式是否合法；
- proposed_writes 是否越权。

#### 第二层：业务一致性评估

- 同一症状的时间、程度和否定状态是否冲突；
- 诊断候选是否包含支持证据；
- 风险等级是否与红旗信息一致；
- 检查建议是否与诊断候选相关；
- 处置建议是否违反禁忌或安全规则；
- 最终结论是否超出系统权限。

#### 第三层：信息增量评估

建议建立可解释的信息增量分数：

```text
information_gain =
    新增有效临床字段权重
  + 新增可区分诊断的证据权重
  + 不确定性降低量
  - 重复信息惩罚
  - 冲突信息惩罚
  - 无证据结论惩罚
```

分数不一定需要一开始就训练模型，可以先使用规则和权重实现，并记录数据，为后续学习型 evaluator 提供样本。

#### 第四层：安全评估

- 是否存在红旗遗漏；
- 是否输出确定诊断；
- 是否输出未经审核的高风险用药建议；
- 是否不当替代线下急诊；
- 是否使用了无来源医学结论；
- 是否暴露敏感信息。

### 10.2 统一决策枚举

结果评估后只能进入以下动作：

```text
ACCEPT
RETRY_SAME_TOOL
SWITCH_TOOL
ASK_USER
HUMAN_REVIEW
FALLBACK_WORKFLOW
REFUSE
STOP
```

禁止使用任意字符串作为动态路由结果。

### 10.3 重新规划策略

| 条件 | 推荐动作 |
|---|---|
| 网络超时且工具幂等 | 退避后重试一次 |
| LLM 输出 Schema 错误 | 带校验错误修复一次 |
| 工具返回空结果且输入不足 | 追问用户 |
| 工具返回空结果但输入充分 | 切换备用工具 |
| 连续两次信息增量过低 | 停止同类工具调用并重新规划 |
| 证据冲突影响高风险结论 | 人工审核 |
| 核心工具不可用 | 固定 Workflow 降级 |
| 达到预算或总时长上限 | 输出受限结论或升级 |
| 红旗信号出现 | 立即进入紧急升级 |

---

## 11. 重试、熔断和降级

### 11.1 错误分类

```python
class ErrorCategory(str, Enum):
    NETWORK_TIMEOUT = "network_timeout"
    TEMPORARY_UNAVAILABLE = "temporary_unavailable"
    RATE_LIMITED = "rate_limited"
    INVALID_SCHEMA = "invalid_schema"
    INVALID_INPUT = "invalid_input"
    EMPTY_RESULT = "empty_result"
    POLICY_DENIED = "policy_denied"
    CLINICAL_CONFLICT = "clinical_conflict"
    INTERNAL_ERROR = "internal_error"
```

只有明确可恢复的错误允许重试。

### 11.2 重试要求

- 每次重试必须记录 attempt；
- 使用指数退避并设置抖动；
- 重试次数由 ToolSpec 控制；
- 非幂等工具必须带幂等键；
- 达到 deadline 后禁止继续重试；
- 业务证据不足不能通过重复调用同一个工具解决。

### 11.3 固定 Workflow 降级

现有五阶段 Workflow 应保留为 fallback，但必须先补齐和验证。

建议降级触发条件：

- Agent Runtime 无法加载 checkpoint；
- 核心图路由发生未处理异常；
- 连续多个工具失败；
- 模型网关不可用；
- 重新规划超过上限；
- 系统进入保守运行模式。

降级后必须：

- 标记 `fallback_mode=true`；
- 记录降级原因；
- 限制可输出内容；
- 禁止高风险自动建议；
- 在最终结果中明确说明能力受限；
- 产生运维告警。

---

## 12. 医疗安全和人工审核

### 12.1 输入安全门

输入进入 LLM 前执行确定性规则，包括但不限于：

- 胸痛、呼吸困难、意识障碍等红旗；
- 大出血、严重过敏和急性神经系统症状；
- 自伤或他伤风险；
- 儿童、孕妇、高龄和严重基础病高风险组合；
- 用户要求自行调整处方药剂量；
- 用户要求系统替代急诊或线下医生；
- 恶意输入、提示词注入和越权请求。

### 12.2 工具调用安全门

- 工具白名单；
- 字段级读权限；
- 字段级写权限；
- 高风险工具人工审批；
- 参数 Schema 校验；
- 禁止模型生成任意 SQL；
- 禁止模型直接拼接内部 URL；
- 所有写操作使用幂等键；
- 对外部内容进行提示词注入隔离。

### 12.3 输出安全门

- 禁止确定性诊断措辞；
- 明确不确定性和证据范围；
- 明确就医时机；
- 高风险场景优先给出线下处置建议；
- 药物相关输出必须符合权限和审核策略；
- 结论必须能回溯到证据；
- 禁止输出无来源或与知识版本不匹配的结论。

### 12.4 人工审核任务

```text
Agent interrupt
  │
  ▼
Create ReviewTask
  ├── task_id
  ├── status: pending
  ├── reason
  ├── risk_level
  ├── cdp_snapshot
  ├── graph_checkpoint
  ├── proposed_action
  ├── assigned_reviewer
  └── due_at
        │
        ▼
Reviewer
  ├── approve
  ├── edit_and_approve
  ├── reject
  └── request_more_information
        │
        ▼
Resume Graph
```

审核必须是真正的业务实体，而不是返回一段“建议人工介入”的文本。

---

## 13. API 与事件契约

### 13.1 对外核心 API

```text
POST   /api/v1/consultations
POST   /api/v1/consultations/{id}/messages
GET    /api/v1/consultations/{id}
GET    /api/v1/consultations/{id}/state
GET    /api/v1/consultations/{id}/result
GET    /api/v1/consultations/{id}/events
POST   /api/v1/review-tasks/{id}/approve
POST   /api/v1/review-tasks/{id}/reject
POST   /api/v1/review-tasks/{id}/request-information
```

### 13.2 Agent Runtime 内部 API

```text
POST   /internal/v1/runs
POST   /internal/v1/runs/{threadId}/resume
GET    /internal/v1/runs/{threadId}
GET    /internal/v1/runs/{threadId}/history
POST   /internal/v1/runs/{threadId}/cancel
```

### 13.3 请求幂等

创建问诊、提交消息、审核处理和状态写入都必须支持：

```text
Idempotency-Key: <uuid>
```

服务端记录请求摘要和处理结果，重复请求返回原结果，不重复创建会话、消息或审核动作。

### 13.4 事件模型

```json
{
  "event_id": "evt_xxx",
  "event_type": "agent.node.completed",
  "occurred_at": "2026-07-29T10:00:00Z",
  "trace_id": "trace_xxx",
  "thread_id": "thread_xxx",
  "cdp_id": "cdp_xxx",
  "session_id": "session_xxx",
  "node": "evaluate_reasoning_result",
  "payload": {}
}
```

事件必须版本化，例如：`event_version: 1`。

---

## 14. 数据存储方案

### 14.1 推荐基线

| 数据类型 | 推荐存储 |
|---|---|
| 用户、患者、会话、审核任务 | PostgreSQL 或现有 Oracle |
| CDP 当前状态和历史版本 | PostgreSQL 或现有 Oracle |
| LangGraph Checkpoint | PostgreSQL |
| 缓存、分布式锁、限流 | Redis |
| 医疗知识图谱 | Neo4j |
| 原始报告和文件 | MinIO/S3 兼容对象存储 |
| 向量检索 | pgvector，规模增长后再独立拆分 |
| 技术指标和 Trace | OpenTelemetry 后端 |
| 合规审计 | 独立审计表或审计存储 |

### 14.2 数据库选择原则

不建议长期同时维护 MySQL、PostgreSQL 和 Oracle 三套同等生产基线。

建议选择：

- 开源演示和个人项目：PostgreSQL；
- 明确的企业 Oracle 环境：保留 Oracle Adapter；
- MySQL 只作为迁移期兼容，不继续扩大专有实现。

### 14.3 数据一致性

- CDP 更新使用乐观锁；
- 状态写入和版本记录在同一事务内；
- 工具执行结果保存 execution_id；
- 重复回调通过 execution_id 去重；
- 异步事件使用 Outbox Pattern；
- 对象存储文件保存哈希和版本；
- 知识检索结果记录 knowledge_version。

---

## 15. 模型和提示词治理

### 15.1 Model Gateway

所有 LLM 调用通过统一网关，禁止各服务自行读取模型 Key 和直接调用外部模型。

网关负责：

- 模型选择；
- 超时和重试；
- 限流；
- Token 和成本统计；
- 敏感信息脱敏；
- Prompt 版本；
- 响应 Schema 校验；
- 供应商切换；
- 审计。

### 15.2 Prompt Registry

每个提示词必须包含：

- prompt_id；
- version；
- owner；
- input schema；
- output schema；
- target model；
- risk level；
- evaluation dataset；
- change log。

### 15.3 模型输出原则

- 默认结构化输出；
- 禁止模型直接决定最终高风险动作；
- 重要结论必须有证据引用；
- 模型置信度不作为唯一决策依据；
- 医疗规则和权限优先于模型输出。

---

## 16. 可观测性和审计

### 16.1 技术可观测性

统一接入 OpenTelemetry：

```text
Spring Boot / FastAPI / LangGraph
              │
              ▼
OpenTelemetry Collector
  ├── Traces: Tempo / Jaeger
  ├── Metrics: Prometheus
  └── Logs: Loki / Elasticsearch
```

### 16.2 统一上下文字段

每次请求和工具调用至少包含：

```text
trace_id
span_id
thread_id
session_id
cdp_id
patient_id_hash
turn_number
graph_node
tool_id
tool_execution_id
model_name
prompt_version
knowledge_version
```

禁止在普通日志中直接记录姓名、身份证号、手机号、完整病历原文等敏感字段。

### 16.3 Agent 专属指标

- 问诊完成率；
- 平均问诊轮数；
- 信息缺口减少率；
- 重复追问率；
- 无效问题率；
- 工具调用成功率；
- 工具超时率；
- Schema 校验失败率；
- 重试率；
- 工具切换率；
- Workflow 降级率；
- 人工审核率；
- 高危召回率；
- 红旗漏检率；
- 无证据结论率；
- 单次问诊模型调用次数；
- 单次问诊 Token 和成本；
- P50/P95/P99 延迟；
- checkpoint 恢复成功率。

### 16.4 业务审计事件

必须记录：

- 谁查看了患者信息；
- 谁修改了 CDP；
- 哪个工具提出了修改；
- 哪个规则或审核人批准修改；
- 哪个模型和提示词生成了结论；
- 哪个知识版本支持了证据；
- 为什么触发拒答、升级或降级；
- 审核人做了什么决策。

---

## 17. 测试和评估体系

### 17.1 单元测试

覆盖：

- 每个路由函数；
- 每个安全规则；
- 每个 Schema；
- 每个状态转换；
- 每个降级策略；
- 每个字段权限校验；
- 每个错误分类；
- 信息增量计算。

### 17.2 契约测试

验证 Java 与 Python：

- 字段名称；
- 枚举；
- 时间格式；
- 错误结构；
- JSON Schema；
- 版本兼容；
- 幂等性；
- 超时行为。

建议由 `contracts/` 目录中的 OpenAPI 和 JSON Schema 生成 DTO，减少手工维护。

### 17.3 Graph 路由测试

```python
def test_red_flag_routes_to_emergency():
    state = build_state(red_flags=["severe_dyspnea"])
    assert route_after_entry_safety(state) == "emergency_escalation"


def test_low_information_routes_to_question():
    state = build_state(missing_information=[critical_gap()])
    assert route_after_gap_analysis(state) == "generate_question"
```

### 17.4 故障注入测试

至少覆盖：

- tool 超时；
- tool 返回非法 JSON；
- tool 返回空结果；
- tool 返回越权 state patch；
- Neo4j 不可用；
- Redis 不可用；
- 数据库锁冲突；
- checkpoint 写入失败；
- LLM 限流；
- 模型返回危险建议；
- 人工审核长期未处理；
- 服务重启后恢复；
- 同一消息重复提交。

### 17.5 医疗场景回归集

- 普通低危问诊；
- 信息不足；
- 高危红旗；
- 多疾病冲突；
- 用户拒绝回答；
- 工具连续失败；
- 错误输入；
- 提示词注入；
- 儿童、孕妇和老人；
- 检查结果前后矛盾；
- 用户中途退出后恢复；
- 处方药剂量询问；
- 急诊替代请求。

### 17.6 评估数据格式

```json
{
  "case_id": "case_001",
  "input": {},
  "expected_routes": [],
  "expected_required_questions": [],
  "expected_red_flags": [],
  "forbidden_outputs": [],
  "expected_evidence": [],
  "review_required": false
}
```

---

## 18. CI/CD 与工程治理

### 18.1 Pull Request 门禁

每个 PR 至少执行：

- Java 编译和单元测试；
- Python lint、类型检查和单元测试；
- OpenAPI/JSON Schema 契约测试；
- Graph 路由测试；
- 安全规则回归；
- Docker 镜像构建；
- 依赖漏洞扫描；
- 密钥泄露扫描；
- 数据库迁移校验。

### 18.2 分支策略

建议：

- `main`：可部署基线；
- `agent/*`：重构和功能分支；
- 所有变更通过 PR；
- 禁止直接向 main 提交；
- 使用 squash merge；
- 重大架构决策通过 ADR。

### 18.3 发布策略

- 开发环境自动部署；
- 测试环境执行 E2E 和评估集；
- 生产或演示环境使用灰度发布；
- 新 Agent Runtime 初期使用 feature flag；
- 支持一键切回固定 Workflow；
- 数据库迁移必须向前兼容；
- 每次发布记录模型、Prompt 和知识版本。

---

## 19. 推荐仓库结构

```text
AIdoctor/
├── apps/
│   ├── business-api/                 # Spring Boot 业务 API
│   ├── agent-runtime/                # FastAPI + LangGraph
│   ├── patient-web/                  # 患者端
│   └── admin-web/                    # 审核与管理后台
│
├── packages/
│   ├── clinical-domain/              # CDP 和领域模型
│   ├── agent-state/                  # GraphState
│   ├── tool-sdk/                     # ToolSpec / ToolContext / ToolResult
│   ├── clinical-understanding/       # 解析、归一化、缺口、追问
│   ├── clinical-reasoning/           # DDx、检索、证据、冲突
│   ├── care-planning/                # 检查、处置、风险、解释
│   ├── knowledge-client/             # 知识服务客户端
│   ├── safety-engine/                # 安全规则和输出门控
│   ├── model-gateway/                # 模型调用治理
│   └── observability/                # Trace、Metrics、Audit SDK
│
├── contracts/
│   ├── openapi/
│   ├── json-schema/
│   └── events/
│
├── evals/
│   ├── datasets/
│   ├── scenarios/
│   ├── routing/
│   ├── safety/
│   └── regression/
│
├── infra/
│   ├── docker/
│   ├── kubernetes/
│   ├── otel/
│   ├── prometheus/
│   └── grafana/
│
├── migrations/
├── scripts/
├── docs/
│   ├── architecture/
│   ├── adr/
│   ├── api/
│   ├── operations/
│   └── refactoring/
│
└── README.md
```

### 19.1 迁移期间的目录策略

不建议一次性移动所有目录。先新增目标目录并迁移一条完整链路，旧服务通过 Adapter 接入。每完成一个模块并通过回归后，再删除对应旧实现。

---

## 20. 分阶段实施计划

## Phase 0：建立可信基线

### 目标

让当前系统的文档、代码、接口和测试形成一致基线。

### 任务

1. 修复 Java Agent 相关接口和实体方法不一致；
2. 确认 `diagnosis-service` 可以编译；
3. 补齐固定五步 Workflow 的响应解析；
4. 建立一条可运行的端到端问诊 happy path；
5. 为 `/start`、`/continue`、`/status`、`/result` 建立集成测试；
6. 明确每个工具服务的真实实现状态；
7. 修正 README 的完成度描述；
8. 为所有服务统一错误结构和健康检查；
9. 冻结 Java AgentLoop 的新增功能，只修复阻塞性问题。

### 交付物

- 可编译的主分支；
- 一键启动最小环境；
- E2E happy path；
- 服务实现状态矩阵；
- 已知问题清单；
- 基线性能和成功率数据。

### 验收标准

- CI 中 Java 和 Python 核心模块全部通过；
- 从创建问诊到获得基础结果可自动化运行；
- Step 1～5 不再使用空实现解析器；
- README 不再将未验证功能标记为完整实现；
- 所有接口错误均返回统一结构；
- 不存在阻断主链路的编译错误。

---

## Phase 1：统一领域模型和工具协议

### 目标

消除 Java/Python DTO 漂移和工具写入失控问题。

### 任务

1. 建立 `contracts/json-schema`；
2. 定义 CDP v1 Schema；
3. 定义 ToolSpec、ToolContext、ToolResult v1；
4. 定义 StructuredError 和 AuditEvent；
5. 从 Schema 生成 Java DTO 和 Python Model；
6. 改造 tool_1～tool_7 返回统一 ToolResult；
7. 工具只返回 proposed_writes；
8. 实现字段级权限校验；
9. 实现状态 patch 合并器；
10. 建立契约测试。

### 交付物

- 版本化 Schema；
- Java/Python 生成代码；
- Tool SDK；
- 契约测试报告；
- 工具权限矩阵。

### 验收标准

- 所有工具通过相同 ToolResult Schema；
- 工具无法直接写数据库；
- 越权字段写入会被拒绝并记录审计；
- Java/Python DTO 不再手工重复维护；
- 兼容性变更有明确版本策略。

---

## Phase 2：建立 Agent Runtime 和 LangGraph 主链路

### 目标

建立唯一、可持久化、可恢复的 Agent 执行引擎。

### 任务

1. 创建 `apps/agent-runtime`；
2. 实现 GraphState；
3. 实现 PostgreSQL Checkpointer；
4. 实现 load_context、entry_safety_check、normalize_case、gap_analysis 和 ask_user 节点；
5. 实现 interrupt/resume；
6. 接入病例理解和对话工具；
7. Spring Boot 创建 consultation 后调用 Agent Runtime；
8. 建立 SSE 进度事件；
9. 通过 feature flag 控制新旧执行路径；
10. 固定 Workflow 作为 fallback。

### 交付物

- Agent Runtime 服务；
- 基础状态图；
- 多轮问诊恢复；
- checkpoint 查询接口；
- 新旧流程切换开关。

### 验收标准

- 用户回答后可从 checkpoint 恢复；
- 服务重启后会话可继续；
- 相同 thread_id 状态连续；
- 重复消息不会重复写入；
- 新 Agent 失败时可切换固定 Workflow；
- Controller 不再直接承载 Agent 内部路由。

---

## Phase 3：诊断推理、结果评估和重新规划

### 目标

实现可解释的 `执行 → 评估 → 重新规划` 闭环。

### 任务

1. 接入 DDx、知识检索、证据融合和检查建议；
2. 实现结构评估器；
3. 实现业务一致性评估器；
4. 实现信息增量评估器；
5. 实现证据冲突评估器；
6. 实现统一决策枚举；
7. 实现 retry、switch、ask_user、fallback 路由；
8. 实现连续失败计数；
9. 实现总轮次、总工具调用、总时长和成本预算；
10. 建立故障注入测试。

### 交付物

- Result Evaluator；
- Replanner；
- 工具替代策略；
- 故障注入报告；
- Agent 路由可视化。

### 验收标准

- Schema 错误可以自动修复或降级；
- 工具超时后不会无限重试；
- 信息不足时会追问而不是继续猜测；
- 证据冲突会进入复核；
- 连续失败会触发明确策略；
- 每次路由决策可解释和可审计。

---

## Phase 4：医疗安全和人工审核

### 目标

让系统具备受约束运行和真正的人在环能力。

### 任务

1. 建立红旗规则库；
2. 建立拒答和升级规则；
3. 实现工具风险等级；
4. 实现输出安全门；
5. 建立 ReviewTask 表和状态机；
6. 建立审核后台；
7. 实现 LangGraph interrupt/resume 审核链路；
8. 建立审核超时和升级策略；
9. 建立提示词注入测试；
10. 建立医疗安全评估集。

### 交付物

- Safety Engine；
- Review Task API；
- 审核后台；
- 安全测试集；
- 红旗和拒答指标面板。

### 验收标准

- 高危场景不会进入普通自动结论；
- 审核人可以批准、编辑或拒绝；
- 审核完成后图可以恢复；
- 每个审核动作有完整审计；
- 提示词注入无法绕过工具权限；
- 红旗回归集达到预设召回标准。

---

## Phase 5：企业工程体系

### 目标

形成可持续交付、部署和运维能力。

### 任务

1. 接入 OpenTelemetry；
2. 建立 Prometheus/Grafana；
3. 建立日志脱敏；
4. 建立 CI 门禁；
5. 建立依赖和镜像扫描；
6. 建立数据库迁移管理；
7. 建立开发、测试、演示环境；
8. 建立灰度发布和回滚；
9. 建立模型、Prompt 和知识版本治理；
10. 建立定期评估报告。

### 交付物

- 可观测性面板；
- CI/CD Pipeline；
- 发布和回滚手册；
- 安全扫描报告；
- Agent 评估报告。

### 验收标准

- 可通过 trace_id 定位一次完整问诊；
- 可观察每个节点和工具耗时；
- 发布失败可以回滚；
- 数据库迁移可重复执行；
- 新版本必须通过评估集后才能启用；
- 敏感数据不出现在普通日志中。

---

## 21. 推荐里程碑

| 里程碑 | 核心结果 | 对应阶段 |
|---|---|---|
| M1 可信基线 | 旧流程完整可运行，文档与实现一致 | Phase 0 |
| M2 统一契约 | CDP 和工具协议统一，工具写入受控 | Phase 1 |
| M3 可恢复 Agent | LangGraph 多轮问诊、checkpoint、fallback | Phase 2 |
| M4 自适应闭环 | 结果评估、重试、工具切换、重新规划 | Phase 3 |
| M5 安全人机协同 | 红旗、拒答、人工审核和恢复 | Phase 4 |
| M6 企业级运行 | 可观测、CI/CD、灰度和评估体系 | Phase 5 |

每个里程碑都必须形成可演示场景，而不是只合并代码。

---

## 22. 首批建议 Issue 列表

### Epic 1：可信基线

- 修复 diagnosis-service 编译和 Agent 接口不一致；
- 补齐固定 Workflow Step 3～5 响应解析；
- 新增最小 E2E 问诊测试；
- 建立服务真实完成度矩阵；
- 修正 README 实现状态；
- 补齐健康检查和统一错误响应。

### Epic 2：统一契约

- 创建 CDP JSON Schema v1；
- 创建 ToolSpec/ToolResult Schema v1；
- 创建 Java/Python DTO 生成脚本；
- 创建 Tool SDK；
- 创建字段权限校验器；
- 改造 clinical parser 为标准工具。

### Epic 3：Agent Runtime

- 初始化 FastAPI + LangGraph 项目；
- 实现 GraphState；
- 接入 PostgreSQL Checkpointer；
- 实现多轮 interrupt/resume；
- Spring Boot 接入 Agent Runtime；
- 建立 feature flag 和 fallback。

### Epic 4：评估与安全

- 实现结构评估器；
- 实现信息增量评估器；
- 实现红旗规则；
- 实现 ReviewTask；
- 实现输出安全门；
- 建立安全回归集。

---

## 23. 关键架构决策 ADR

建议新增以下 ADR：

1. `ADR-001-single-agent-runtime.md`：为什么只保留一个 Agent Runtime；
2. `ADR-002-langgraph-orchestration.md`：为什么使用 LangGraph 管理执行状态；
3. `ADR-003-modular-monolith-first.md`：为什么先收敛服务；
4. `ADR-004-cdp-vs-agent-state.md`：CDP 与 GraphState 的边界；
5. `ADR-005-tool-write-governance.md`：为什么工具只能提出写入；
6. `ADR-006-database-baseline.md`：主数据库选择；
7. `ADR-007-fixed-workflow-fallback.md`：固定 Workflow 的降级职责；
8. `ADR-008-human-review.md`：人工审核触发和恢复机制。

---

## 24. 风险与应对

| 风险 | 影响 | 应对措施 |
|---|---|---|
| 一次性大规模重写 | 长期无法形成可运行版本 | 采用旁路 Agent Runtime 和 feature flag |
| 过早拆微服务 | 开发和测试成本失控 | 模块化单体优先 |
| 新旧状态不一致 | 问诊恢复错误 | 明确 CDP、GraphState 和 checkpoint 边界 |
| 工具协议频繁变化 | Java/Python 大量联动修改 | Schema First + 代码生成 |
| Agent 自主性过高 | 医疗风险增加 | 规则门控、工具权限和人工审核 |
| 模型输出不稳定 | 结果不可预测 | 结构化输出、评估器和固定降级 |
| 数据库多基线 | 迁移脚本和测试翻倍 | 确定单一生产基线 |
| 文档继续超前 | 项目可信度下降 | 文档声明必须绑定测试和指标 |
| 评估数据不足 | 无法判断重构是否有效 | 从 Phase 0 开始积累回归集 |

---

## 25. Definition of Done

一个功能只有同时满足以下条件才能标记为“完整实现”：

- 业务代码已经接入主链路；
- 输入输出 Schema 已定义；
- 单元测试通过；
- 契约测试通过；
- 至少一个 E2E 场景通过；
- 异常和降级路径已覆盖；
- 日志、指标和审计已接入；
- 安全影响已评估；
- 文档已更新；
- 不存在仅返回空对象或硬编码成功指标的占位实现。

---

## 26. 重构完成后的核心演示场景

项目最终应能够稳定演示以下链路：

```text
用户发起问诊
→ 系统创建 consultation、thread 和 CDP
→ 输入安全检查通过
→ 病例解析发现关键信息缺失
→ Agent 生成高价值问题并暂停
→ 用户回答后从 checkpoint 恢复
→ 生成鉴别诊断候选
→ 检索知识图谱和医学证据
→ 某工具返回非法结构
→ 系统自动修复一次
→ 修复失败后切换备用工具
→ 新结果与既有证据冲突
→ 风险门控触发人工审核
→ 审核人编辑并批准建议
→ Agent 从 interrupt 恢复
→ 生成受约束的检查和就医建议
→ 保存 CDP 新版本
→ 前端展示证据、风险、不确定性和执行轨迹
→ 全过程可通过 trace_id 和 thread_id 回放
```

这条链路比简单增加模型数量更能证明项目具备企业级 Agent 能力。

---

## 27. 下一步执行建议

重构应从 Phase 0 开始，第一批实际代码改动建议严格限定在以下范围：

1. 修复当前编译和接口不一致；
2. 补齐旧 Workflow 的空解析逻辑；
3. 建立最小 E2E 测试；
4. 创建 `contracts/` 基础目录；
5. 创建 CDP 和 ToolResult v1 Schema；
6. 修正文档中的实现状态。

在以上内容完成前，不建议继续扩展 Java AgentLoop，也不建议立即把所有工具拆成新的独立微服务。

---

## 28. 总结

AIdoctor 的重构重点不是把固定流程替换成一个可以任意行动的 LLM，而是建立一个**有状态、有边界、有证据、有评估、有恢复、有降级、有人审和可审计**的临床 Agent Runtime。

推荐的演进路线是：

```text
可信固定 Workflow
→ 统一 CDP 与工具契约
→ 可恢复的 LangGraph Agent
→ 结果评估和重新规划
→ 医疗安全与人工审核
→ 可观测和可持续交付
```

只有每一层都通过测试和验收后，项目才能从“功能演示型 AI 系统”成长为“企业级受约束临床决策支持 Agent”。
