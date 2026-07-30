# AIdoctor 原设计资产保留、改造与目标架构映射

> 文档状态：Draft v2.6 Authoritative Legacy Asset Mapping  
> 更新时间：2026-07-29  
> 适用范围：`docs/AI医生/项目文档`、对应代码/配置/测试，以及它们向目标架构的迁移  
> 关联文档：[当前系统资产盘点](./current-system-inventory.md) · [迁移矩阵](./migration-matrix.md) · [覆盖矩阵](./coverage-matrix.md) · [实施路线](./implementation-roadmap.md) · [冻结基线](./architecture-freeze-baseline.md)

---

## 1. 文档目的

当前重构不采用“旧设计默认无效、从零重写”，而采用：

```text
Legacy Design Evidence
→ Asset Inventory
→ Retention Decision
→ Target Architecture Mapping
→ Migration Task
→ Validation
→ Shadow / Rollback
→ Decommission Gate
```

本文件回答：

1. 原设计中哪些思想、协议、数据结构、规则、代码和评估资产有继承价值；
2. 哪些内容可以原样保留，哪些必须改造、拆分、评估或归档；
3. 每项资产在 11 个目标模块中的归属；
4. 开发阶段如何验证继承成功；
5. 旧实现满足什么条件后才允许下线。

本文件是以下文档的权威补充：

- `current-system-inventory.md`：增加 Legacy Design Asset Inventory；
- `migration-matrix.md`：增加设计原则、协议、状态、Trace、评估和知识治理资产；
- `coverage-matrix.md`：要求每个保留资产绑定 Owner、Contract、Phase、Test 和 Gate；
- `implementation-roadmap.md`：在 A6 与 A7 之间增加 A6.5 Legacy Design Asset Validation；
- `engineering-release-and-decommission-plan.md`：增加旧设计资产替代和下线门禁。

若旧文档与当前冻结基线冲突，以冻结基线、本文件和对应 v2.6 详细设计为准；旧文档只作为资产证据和历史设计来源。

---

## 2. 评估范围与证据等级

### 2.1 评估范围

重点证据目录：

```text
docs/AI医生/项目文档/2.架构设计/
docs/AI医生/项目文档/3.业务功能设计/
docs/AI医生/项目文档/4.工具设计/
docs/AI医生/项目文档/5.主agent设计/
docs/AI医生/项目文档/6.数据模型设计/
docs/AI医生/项目文档/7.接口规范/
docs/AI医生/项目文档/9.知识演化设计/
docs/AI医生/项目文档/11.错误处理与异常/
docs/AI医生/项目文档/12.性能与评估/
docs/AI医生/项目文档/13.技术细节/
```

覆盖资产：

- 系统定位、单主 Agent 和双通道推理；
- 临床诊疗流程、入口判定和终点结论包；
- 工具业务逻辑、接口和技术实现；
- CDP、AgentState、AuditTrail 和 DTO；
- REST、ToolContext 和 ToolResult；
- AOP、Trace、Feign 拦截器；
- 错误处理、降级、性能和评估；
- 知识图谱、知识演化和 Publish Gate；
- 旧前端、ReactFlow、时间线和管理端设计。

### 2.2 证据等级

| 等级 | 含义 |
|---|---|
| DESIGN_CONFIRMED | 旧文档明确给出结构、约束或流程 |
| CODE_CONFIRMED | 当前代码或配置中存在对应实现 |
| TEST_CONFIRMED | 有可运行测试、fixture 或评估集 |
| RUNTIME_VERIFIED | 已编译、启动或执行真实链路 |
| DATA_VERIFIED | 已核对真实数据库、索引或知识数据 |
| UNKNOWN | 尚无足够证据 |

`DESIGN_CONFIRMED` 只能证明设计存在，不能证明实现可直接进入生产。

---

## 3. 保留决定

| 决定 | 含义 |
|---|---|
| KEEP | 职责和契约基本符合目标，可保留 |
| ADAPT | 核心思想或逻辑有价值，但接口、权限、状态、版本或依赖必须调整 |
| EXTRACT | 从旧代码、配置或文档中抽取为独立治理资产 |
| SPLIT | 一个旧资产跨越多个目标模块，需要拆分 |
| MERGE | 多个旧资产职责重叠，需要合并 |
| WRAP | 通过 Legacy Adapter 暂时接入 |
| EVALUATE | 可能有价值，但必须通过对照实验、来源治理或安全评估 |
| ARCHIVE | 不进入当前生产主链路，但保留历史或研究价值 |
| REWRITE | 目标职责保留，但当前实现不满足状态、安全或工程边界 |
| REMOVE | 替代、迁移、归档和回滚窗口全部完成后才允许删除 |

禁止仅因为旧目录或服务名不符合目标架构就直接 `REMOVE`。

---

## 4. 原设计资产总览

| Legacy Asset | 初始决定 | 有价值部分 | 目标位置 | 主要修正 | Phase |
|---|---|---|---|---|---|
| 双通道推理 | KEEP + ADAPT | 结构化推理和语言表达分离 | Clinical Intelligence、Safety、Evidence、Model Runtime | LLM 不拥有最终临床决策权 | A-D |
| 单主 Agent / 单一最终提交者 | ADAPT | 统一编排和责任链 | Agent Runtime + State Committer + Safety | 拆分规划、提交和安全权限 | B-D |
| 无状态工具 | KEEP + ADAPT | 工具无长期目标、结构化返回 | Tool/Skill Governance | 限制 Context、版本、权限和副作用 | A-D |
| ToolContext | ADAPT | 统一上下文、版本、预算和 Trace | ToolExecutionRequest | 增加 Capability、Policy、Idempotency 和字段权限 | A-C |
| ToolResult | ADAPT | payload、evidence、quality、errors | Versioned ToolResult | `suggestedWrites` 改为 ProposedStatePatch | A-D |
| CDP 聚合视图 | KEEP + SPLIT | 统一病例视图和版本 | Clinical State & Data Foundation | 大 JSON 拆为实体、Ledger 和 Delivery | A-D |
| CDP Copy-on-Write | KEEP + ADAPT | 版本、历史和回放 | State Committer + Version Ledger | 乐观并发、幂等和不可变记录 | B |
| AgentState | ADAPT | 预算、尝试、回退和停止条件 | GraphState + Run/Checkpoint | Durable Store 为事实源 | A-C |
| AuditTrail | KEEP + SPLIT | 追加写和可追溯 | Trace、AgentEvent、Decision、Compliance Audit | PHI、版本链和职责拆分 | B-F |
| `@TraceExecution` | KEEP + ADAPT | 语义注解、低侵入埋点 | Observability Instrumentation | 对接 OTel，不隐藏临床逻辑 | A-C |
| `ExecutionTraceAspect` | ADAPT | 横切追踪、异常和耗时 | OTel Aspect / Observation Adapter | Trace 失败不得阻断业务 | A-C |
| `TraceContext` | REWRITE | 请求上下文传播需求 | OTel Context + MDC | 不以裸 ThreadLocal 作为跨异步方案 | A-C |
| Feign Trace Interceptor | ADAPT | 跨服务上下文传播 | OTel Feign Instrumentation | 使用 W3C Trace Context | A-C |
| execution-trace-service | SPLIT + ADAPT | 时间线、调用树和 UI | AgentEvent Store/UI + OTel Backend | 技术 Trace 和临床审计分离 | C-F |
| 五步循证流程 | ADAPT | 信息缺口、分层候选、证据回填和回退 | Capability Clinical Policy + Fallback Workflow | 不作为不可变全局顺序 | A-E |
| 三层候选 Tier1/2/3 | ADAPT | 最可能、必须排除、积极备选 | Hypothesis Pack | 场景化、证据化、动态更新 | A-D |
| 终点结论包 | ADAPT | 当前判断、必须排除、依据、行动随访 | DeliveryPackage | 区分患者、医生和系统输出 | D-E |
| 分层错误处理 | KEEP + ADAPT | 工具/服务/系统/业务错误分类 | Runtime Error Policy + Safety Failure Policy | 技术降级和临床降级分离 | A-F |
| Static Case Set | KEEP + EXTEND | 静态病例回归 | Capability Eval Suite | 增加版本快照和安全指标 | A-F |
| Interactive Interview Set | KEEP + EXTEND | 多轮问诊质量和效率 | Conversation Eval | 增加重复、偏航和攻击测试 | A-F |
| Trajectory Replay Set | KEEP + EXTEND | 信息逐步揭示后的修正能力 | Durable Replay Eval | 增加 Crash/Resume 和版本回放 | C-F |
| Knowledge Sandbox/Staging/Production | KEEP + ADAPT | 候选区、验证区和生产只读区 | Knowledge Release Lifecycle | 绑定 Source、License、Embedding 和 Eval | A-D |
| Knowledge Publish Gate | KEEP + ADAPT | Schema、来源、回归和回滚门禁 | Knowledge Release Gate | 增加 Citation、适用性和撤回 | A-F |
| Knowledge Evolution Agent 集群 | ARCHIVE + DEFER | 自动抽取、验证、冲突和监控研究 | 后续 Knowledge Ops | V1 先用确定性 Pipeline 和人工审核 | 后续 |
| Neo4j / DR.KNOWS 路径 | EVALUATE + ADAPT | 概念关系、多跳和 Query Expansion | Knowledge Graph Enhancement | 不替代 EvidenceClaim 和 Citation | D+ |
| 路径注入 LLM | EVALUATE + ADAPT | 受控上下文和路径外输出验证 | Evidence/Model Runtime | 验证失败不得把路径直接当诊断 | D+ |
| ReactFlow Trace 可视化 | KEEP + ADAPT | 流程、调用树和路径展示 | Clinician/Admin Observability UI | 分离 Trace、AgentEvent 和 Decision | C-F |

该表为设计级初始结论。A6.5 必须把每一项升级为代码、测试、运行或数据证据支持的结论。

---

## 5. 核心设计资产映射

### 5.1 双通道推理

原原则：

```text
结构化通道决定“该往哪想”
语言通道决定“怎么说、怎么问”
```

目标映射：

```text
Structured Clinical Channel
├── Terminology / Observation
├── Mandatory Safety / Triage
├── Information Gap
├── Limited Hypothesis
├── Evidence Retrieval / Applicability
└── Deterministic Validation

Language and Presentation Channel
├── Question Wording
├── Patient Explanation
├── Clinician Summary
└── Delivery Formatting
```

保留门禁：

- 结构化输出必须有 Schema；
- 红旗和最终分诊不能依赖语言通道单点执行；
- 语言通道只能基于批准的结构化状态和 EvidencePack；
- 模型输出只能产生 Candidate、Decision Draft 或 Delivery Draft。

### 5.2 单主 Agent

保留统一编排和责任链，但拆分全能权限：

```text
Agent Runtime
负责选择批准范围内的 NextAction

State Committer
负责临床状态的唯一提交

Safety & Policy
负责不可绕过的安全和权限

Business / Clinician
负责高风险审核和外部动作
```

禁止主 Agent、LLM 或 Tool 直接写最终临床事实。

### 5.3 ToolContext / ToolResult

目标协议：

```text
ToolExecutionRequest
├── request_id
├── trace_id / span_id
├── encounter_id / thread_id / run_id
├── capability_id / capability_version
├── tool_id / tool_release_id
├── contract_version
├── allowed_context
├── read_field_allowlist
├── timeout_ms
├── idempotency_key
├── policy_snapshot
└── parameters

ToolResult
├── status
├── payload
├── evidence_claims
├── source_ids
├── quality
├── proposed_state_patch
├── errors
├── retryability
├── duration_ms
└── version_snapshot
```

迁移要求：

- 原 `suggested_writes` 抽取为 `ProposedStatePatch`；
- Tool 不能读取完整 CDP，只获取 `allowed_context`；
- Tool 不能自行选择 Prompt、模型或知识索引；
- Tool 副作用必须经过 Capability Policy 和幂等控制。

### 5.4 CDP

保留：

- Encounter 内统一临床聚合视图；
- Copy-on-Write；
- 版本历史；
- 变更原因和来源；
- 回放能力。

拆分：

```text
Legacy CDP JSON
├── patient_state → ClinicalObservation + SourceArtifact
├── ddx → DiagnosticHypothesis
├── evidence_graph → EvidenceLedger / GraphReference
├── triage → TriageAssessment
├── uncertainty → InformationGap / Conflict
├── workup_plan → ClinicalPlanCandidate
├── conclusion_package → DeliveryPackage
├── audit → ComplianceAudit
└── execution_trace → AgentEvent / Technical Trace
```

旧原始 JSON 必须保留 blob、checksum、旧 schema、解析错误和未映射字段。

### 5.5 AgentState

保留字段候选：

```text
max_tool_calls
max_time_seconds
max_cost
current_usage
tried_tools
tool_attempts
failure_backoff
conflicts
stop_conditions
```

目标持久化：

```text
PostgreSQL Checkpoint / RunRecord = Durable Source of Truth
Redis = Lease / Coordination / Short-lived Cache
```

不得采用“Redis 实时写、数据库定时落盘”作为唯一恢复保障。

---

## 6. AOP 与执行追踪保留方案

### 6.1 总体决定

```text
AOP：KEEP + ADAPT
现有同步自定义 Trace 链路：REWRITE / SPLIT
```

保留 AOP 的原因：

- 横切关注点集中治理；
- 业务代码和遥测分离；
- 注解式启用；
- 方法耗时、异常和统一标签采集；
- 可配置、低侵入。

AOP 允许处理：

```text
Technical Trace
Metrics
Structured Logging
PHI-safe telemetry metadata
Request context observation
Technical audit entry
```

AOP 禁止隐藏执行：

```text
Red Flag / Triage
Clinical State Commit
Clinical Fallback
Model Route Selection
Human Review Requirement
Clinical Permission Decision
Knowledge Release Selection
```

### 6.2 组件级迁移

| 原组件 | 决定 | 目标组件 | 迁移动作 |
|---|---|---|---|
| `@TraceExecution` | KEEP/ADAPT | `@WithSpan`、`@Observed` 或领域语义注解 | 保留 service/module 语义，增加 operation、risk、data-classification |
| `ExecutionTraceAspect` | ADAPT | OTel Span Adapter | 生成或附着 Span，不同步调用远程 Trace 服务 |
| `TraceContext` | REWRITE | OpenTelemetry Context + MDC | 标准上下文传播；异步使用 TaskDecorator/Reactor Context |
| `FeignTraceInterceptor` | ADAPT | OTel Feign Instrumentation | 注入 W3C `traceparent`/`tracestate` |
| `FeignTraceResponseInterceptor` | ADAPT | Client Span lifecycle | 统一状态码、异常和 latency 属性 |
| `TraceServiceClient` | SPLIT | OTel Exporter + AgentEventPublisher | 技术遥测异步 Export；领域事件走可靠事件通道 |
| `execution-trace-service` | SPLIT | OTel Backend + AgentEvent Store/UI | 保留时间线和调用树，不承担 Checkpoint 或临床事实 |

### 6.3 必须修复的问题

1. Trace 记录失败不得阻止 `joinPoint.proceed()`；
2. Span Export 不得在临床主事务中同步阻塞；
3. 不生成孤立 UUID 作为分布式 Trace 标准；
4. 不记录完整患者输入、Prompt、模型原始响应或临床对象；
5. ThreadLocal 必须有同步、异步、线程池和响应式传播策略；
6. AOP 自调用、private method 和代理边界必须有测试；
7. AgentEvent 和技术 Span 可通过 run_id/trace_id 关联，但不能混成同一记录。

### 6.4 AOP 验证集

```text
正常方法调用
异常重新抛出
Exporter不可用
Trace服务超时
Feign成功/失败/重试
线程池异步传播
服务重启
PHI字段输入输出
AOP self-invocation
重复Span和孤儿Span
```

验收：追踪失败不改变业务结果；Trace 连通；无完整 PHI；开销达标；AgentEvent 和技术 Trace 可关联。

---

## 7. AuditTrail 拆分方案

原 AuditTrail 的追加写和不可修改原则保留，拆分为：

```text
Technical Trace
├── span、latency、status、exception

AgentEvent
├── node、tool、retry、interrupt、resume、fallback

ClinicalDecisionRecord
├── safety、triage、hypothesis、evidence、stop、review

ComplianceAudit
├── actor、access、change、approval、release、export
```

共同版本快照至少包含：

```text
capability_id/version
prompt_id/version
model_route_id/version
selected_model_id
knowledge_release_id
embedding_version
reranker_version
graph_release_id
contract_version
policy_version
```

---

## 8. 错误处理与降级继承

保留旧的层级分类：

```text
Tool Error
Service Error
System Error
Business/Clinical Error
```

目标错误模型增加：

```text
retryable
non_retryable
safe_fallback
human_review_required
fail_closed
delivery_only
state_unchanged
```

关键原则：

- 技术上存在 fallback，不等于临床上允许 fallback；
- Safety、Triage、Consent、Permission、State Commit 失败时默认 fail closed；
- RAG 不可用时不得生成伪装成循证结论的输出；
- 模型失败时不得静默切换到未评估模型；
- 重试必须有 idempotency、attempt record 和预算限制。

---

## 9. 评估资产继承

### 9.1 直接保留并扩展

| 原评估集 | 目标评估集 | 新增内容 |
|---|---|---|
| Static Case Set | Capability Static Case Eval | 红旗、边界、不确定性和版本快照 |
| Interactive Interview Set | Conversation Policy Eval | 信息增益、重复问题、偏航、攻击和停止 |
| Trajectory Replay Set | Durable Replay Eval | Crash/Resume、版本固定和状态一致性 |

### 9.2 新增评估集

```text
Safety and Triage Set
Capability Boundary Set
State Committer Set
Prompt / Model Route Set
RAG Retrieval and Citation Set
Knowledge Release Regression Set
Crash Matrix
Context Leakage Set
Human Review Set
AOP / OTel Instrumentation Set
```

原评估材料必须盘点真实文件、样例数量、标注来源、许可和可运行程度，不能只根据文档名称判定为可用。

---

## 10. 知识演化资产继承

### 10.1 保留治理骨架

```text
Sandbox
→ Staging
→ Evaluation
→ Publish Gate
→ Production Read-only Release
→ Monitoring / Rollback
```

目标映射：

```text
Knowledge Source
→ Ingestion Run
→ Candidate Knowledge Release
→ Schema / License / Provenance Validation
→ Retrieval / Citation / Clinical Eval
→ Approval
→ Production Knowledge Release
```

### 10.2 延后自动 Agent 集群

Extractor、Verifier、Conflict Resolver、Release Builder 和 Shadow Evaluator 等设计作为后续 Knowledge Ops 研究资产保留。Medical RAG V1 先使用：

- 确定性摄取 Pipeline；
- 受控模型抽取；
- Schema Validator；
- 来源和许可审核；
- 临床审核；
- Release Gate；
- 可回滚索引。

自动演化 Agent 不得直接写生产知识。

---

## 11. 五步流程与结论包继承

### 11.1 五步流程

保留：

- 自然语言到结构化问题；
- 必填、重要和可选信息缺口；
- 最可能、必须排除和积极备选；
- 支持、反对和缺失证据；
- 验证计划；
- 证据到达后的排序更新；
- 回退、升级和拒答。

目标形态：

```text
Capability Clinical Policy
+ Approved NextAction
+ Mandatory Safety Every Turn
+ Fixed Workflow Fallback
```

Step1-5 不作为所有场景不可变的全局顺序。

### 11.2 终点结论包

原四要素保留：

```text
Current Assessment
Must-not-miss Status
Key Evidence and Uncertainty
Action / Follow-up / Escalation
```

拆为：

```text
PatientDelivery
ClinicianDelivery
SystemDelivery
```

“至少三条证据”等固定数量规则改为证据质量、适用性、来源和不确定性门禁。

---

## 12. 知识图谱和路径注入 LLM

### 12.1 可保留能力

- 医学术语和同义词关系；
- 症状—疾病—检查关系导航；
- must-not-miss 候选扩展；
- Query Expansion；
- 多跳解释辅助；
- 路径外模型输出检测。

### 12.2 不允许保留的旧假设

```text
图中存在路径
≠ 当前指南支持
≠ 对该患者适用
≠ 可以直接形成诊断
```

路径验证失败时：

```text
返回结构化候选
→ 标记低置信度和失败原因
→ 继续提问或检索
→ 必要时医生审核
```

禁止把路径本身直接降级为患者诊断结果。

Knowledge Graph 只有在来源、版本、许可、回滚、患者隔离和有图/无图对照实验均通过后才可进入生产。

---

## 13. 不原样继承的内容

以下内容不得原样进入目标生产主链路：

- 八个工具长期各自作为独立微服务；
- Tool 直接读取完整 CDP；
- Deterministic 或 Retrieval Tool 直接写最终临床结论；
- 主 Agent 同时拥有规划、安全和状态提交全部权限；
- Audit、Trace、Delivery、临床事实全部放入一个 CDP JSON；
- Redis 作为 AgentState 唯一恢复事实源；
- 自定义 UUID/Header 替代标准分布式 Trace；
- 路径或知识图谱替代 Citation；
- 治疗和处方自动进入首个 Capability；
- 固定 confidence 数值单独决定医疗结论；
- Trace、日志或 Prompt 中保存完整 PHI；
- 模型或知识失败后静默返回看似确定的结果。

---

## 14. 目标模块映射

| 目标模块 | 主要继承资产 |
|---|---|
| Business & Care Delivery | 入口流程、终点结论包、随访、页面和 API 语义 |
| Clinical Domain Contracts | ToolContext、ToolResult、CDP/AgentState DTO |
| Clinical State & Data Foundation | CDP 聚合视图、Copy-on-Write、版本历史 |
| Safety & Policy Engine | 红旗、风险、升级、拒答和错误安全原则 |
| Clinical Intelligence | 双通道结构化推理、问题清单、信息缺口和三层候选 |
| Evidence Intelligence & RAG | Evidence、知识库优先、路径验证和 Knowledge Gate |
| Agent Runtime | Observe/Plan/Act/Update/Evaluate、预算、停止和回退 |
| Context & Memory | AgentState 摘要、允许字段和上下文限制 |
| Tool, Skill & Model Governance | 无状态工具、质量、降级和 Prompt/Model 适配资产 |
| Durable Execution | AgentState、版本、恢复和工具尝试记录 |
| Observability, Audit & Evaluation | AOP、Trace、AuditTrail、三类评估集和 ReactFlow UI |

---

## 15. Phase A6.5：Legacy Design Asset Validation

该工作包插入在：

```text
A6 Capability Package 骨架
→ A6.5 Legacy Design Asset Validation
→ A7 Model Runtime 骨架
```

### 15.1 任务

1. 扫描 `docs/AI医生/项目文档` 全部设计资产；
2. 将设计项映射到实际代码、配置、数据和测试；
3. 为每项资产登记 Evidence Level；
4. 确认 KEEP/ADAPT/EXTRACT/SPLIT/EVALUATE/ARCHIVE；
5. 绑定 Target Module、Contract、Phase、Owner 和 Reviewer；
6. 提取 Shared Contracts、Capability Pack、Prompt、规则和评估样例；
7. 建立 AOP/Trace、CDP、AgentState、AuditTrail 专项验证；
8. 将结果回写 Migration Matrix 和 Coverage Matrix；
9. 未验证资产不得删除或进入生产 Release。

### 15.2 交付物

```text
legacy-design-asset-inventory.csv
legacy-design-code-evidence.csv
legacy-contract-extraction.md
legacy-clinical-policy-extraction.md
legacy-eval-asset-inventory.csv
legacy-observability-migration.md
legacy-asset-decommission-register.csv
```

### 15.3 Inventory 字段

```text
asset_id
asset_name
asset_type
document_path
code_path
config_path
data_location
test_location
evidence_level
valuable_principle
current_problem
retention_decision
target_module
target_contract
migration_action
validation_suite
owner
reviewer
phase
dependency
decommission_condition
status
```

### 15.4 Exit Gate

- 所有 P0/P1 原设计资产有唯一记录；
- 每项资产有明确迁移决定；
- `KEEP/ADAPT` 有代码和测试验证计划；
- `EVALUATE` 有对照实验；
- `ARCHIVE/REMOVE` 有依赖和数据检查；
- AOP/Trace、CDP、AgentState、AuditTrail 和 Tool Contract 已形成目标 Contract；
- Coverage Matrix 无“有价值但无归属”的资产；
- 未验证旧能力未被删除。

---

## 16. 对现有文档的执行约束

### 16.1 Current System Inventory

必须增加 Legacy Design 维度，不只盘点服务和代码。每项设计关联代码、配置、数据、测试和证据等级。

### 16.2 Migration Matrix

至少覆盖：

```text
双通道推理
单主Agent责任链
ToolContext / ToolResult
CDP版本机制
AgentState预算与停止条件
AuditTrail
@TraceExecution / ExecutionTraceAspect
TraceContext / Feign Interceptor
五步流程
三层候选
Conclusion Package
错误分类与降级
三类评估集
Knowledge Publish Gate
Neo4j / DR.KNOWS
路径注入LLM
```

### 16.3 Coverage Matrix

每个保留资产绑定：

```text
Legacy Asset
→ Owner Module
→ Target Capability
→ Contract
→ Implementation
→ Validation Suite
→ Phase
→ Completion Gate
```

### 16.4 Implementation Roadmap

A6.5 完成前不得：

- 删除旧服务；
- 覆盖旧 Prompt；
- 丢弃旧规则和问诊模板；
- 清理 Neo4j 或向量数据；
- 替换 AOP/Trace 后直接下线旧链路；
- 将固定 Workflow 下线。

### 16.5 Engineering / Decommission

旧资产下线必须满足：

```text
Target replacement implemented
+ Contract compatibility verified
+ Regression passed
+ Shadow comparison passed
+ Data migrated or archived
+ Fallback available
+ Rollback tested
+ Full-repo references empty
+ Owner approval
+ Rollback window completed
```

---

## 17. 冻结原则

1. 重构采用资产继承式迁移，不采用默认推倒重建；
2. 旧设计文档是资产证据，不是当前架构真值；
3. 有价值原则可以 KEEP，但实现仍可能需要 REWRITE；
4. AOP 作为技术横切机制保留，不得隐藏临床决策；
5. Tool、LLM、KG 和旧 Workflow 都不能绕过 State Committer 与 Mandatory Safety；
6. Knowledge Evolution 的候选、发布和回滚思想保留，自动 Agent 集群延后；
7. 原评估集必须尽量继承并扩展，不能只验证新代码能启动；
8. 删除旧资产必须有替代、验证、归档、回滚和下线证据。

---

## 18. 当前结论

原设计不是被目标架构否定，而是被重新分配到更清晰的边界中：

```text
Original Clinical and Engineering Assets
+ Corrected State / Safety / Governance Boundaries
+ Durable Runtime
+ Standard Observability
+ Versioned Capability / Prompt / Model / Knowledge Releases
= Target Enterprise Clinical Agent
```

进入 Phase A 后，第一项不是机械移动目录，而是证明哪些旧资产真实存在、可运行、可测试，并将有价值内容抽取进 Shared Contracts、`adult_respiratory_v1`、Model Runtime、Knowledge Release、OTel 和 Eval Suite。