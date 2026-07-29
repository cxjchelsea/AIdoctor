# AIdoctor 代码与资产迁移矩阵

> 文档状态：Draft v2.6 Migration Matrix  
> 更新时间：2026-07-29  
> 输入：[当前系统资产盘点](./current-system-inventory.md)  
> 目标：[总体架构与模块设计](./overall-architecture-and-module-design.md)  
> 扩展规范：[Capability Package](./capability-package-specification.md) · [呼吸道 RAG V1](./adult-respiratory-medical-rag-v1-design.md) · [Model Runtime](./prompt-and-model-runtime-design.md)

---

## 1. 处理类型

| 类型 | 定义 |
|---|---|
| KEEP | 职责和契约基本符合目标，可原位保留 |
| ADAPT | 核心逻辑有价值，需修改接口、类型、权限、版本或依赖 |
| WRAP | 暂时通过 Adapter 接入，待新实现稳定后迁移 |
| REWRITE | 职责、状态所有权或安全边界根本不符合，需要按新设计实现 |
| ARCHIVE | 不进入目标生产主链路，但保留历史、研究或参考价值 |
| REMOVE | 已确认无引用、无数据和无回滚价值后删除 |
| SPLIT | 当前资产跨越多个目标模块，需要拆分迁移 |
| MERGE | 多个当前资产职责重叠，合并为一个目标模块 |
| EXTRACT | 从现有代码、配置或数据中抽出为独立治理资产 |
| EVALUATE | 保留候选能力，但必须通过基准和收益评估后才能进入生产 |

任何 `REMOVE` 必须满足：替代路径上线、数据和流量迁移、全仓引用为空、回滚窗口结束、归档完成、Owner 批准。

---

## 2. 总体迁移策略

采用 Strangler Migration：

```text
建立 Shared Contracts
→ 建立 Capability / Prompt / Model / Knowledge 版本资产
→ Adapter 包装旧能力
→ 第一条新纵向切片
→ 新旧双跑与差异比较
→ 分模块切换流量
→ 旧路径只读或 fallback
→ 达到下线门禁
→ Archive / Remove
```

禁止：

- 一次性重写全部服务；
- 用目录改名代替职责重构；
- 未验证便删除固定 Workflow；
- 业务模块直接调用模型供应商 SDK；
- 未注册的 Prompt、模型、知识来源进入生产；
- 新旧代码无版本控制地直接写同一临床状态；
- 把 Neo4j 中已有关系直接当作循证结论。

---

## 3. 顶层资产迁移矩阵

| 当前资产 | 目标模块 | 决策 | 主要保留 | 主要改造 | 阶段 | 完成条件 |
|---|---|---|---|---|---|---|
| `diagnosis-service` | Business、Clinical State | SPLIT + ADAPT | API 语义、CDP 历史、固定 Workflow | Encounter、StatePatch、State Committer、v1 兼容 Adapter | A-E | 新 Business API 和 State Committer 稳定，旧 Workflow 仅 fallback |
| `examination-service` | Business、Evidence Intake | REWRITE/ARCHIVE | 上传需求和接口草案 | SourceArtifact、Consent、质量门、对象存储 | D/E | 新资料接入 E2E 通过 |
| `health-state-assessment-service` | Safety、后续 Wellness Capability | SPLIT + MERGE | 红旗、风险、输入校验 | 红旗合并 Safety；健康管理抽为独立 Capability | B/E | 单一 Safety Engine 回归通过 |
| `clinical-parsing-service` | Clinical Intelligence | ADAPT | 词表、归一化、抽取算法 | ObservationCandidate、统一 Model Route、去除 CDP 直写 | B/C | 新抽取合同与 Eval 达标 |
| `dialog-service` | Intelligence、Context、Runtime、Business | SPLIT + REWRITE | 信息缺口、问题模板、通信经验 | QuestionDecision、ContextEnvelope、Prompt Registry、统一事件流 | C/D | 新 Runtime 和通信层覆盖旧接口 |
| `diagnosis-engine-service` | Clinical Intelligence、Evidence | ADAPT + WRAP | KG、规则、统计和模型 Adapter | DiagnosticHypothesis、证据方向、版本、Model Route | D/F | 双跑结果通过临床评审 |
| `workup-planner-service` | Tool Governance、Clinical Intelligence | ARCHIVE + WRAP | 检查建议原型 | 受控 Tool、证据、权限、审核 | E+ | 专项评估和审核链完成 |
| `treatment-engine-service` | 高风险 Tool | ARCHIVE | 原型、术语和历史数据 | 不进入首个 Capability；未来单独立项 | 后续 | 高风险能力独立批准 |
| `risk-assessment-service` | Safety、Delivery | SPLIT + MERGE | 分诊、升级规则、reason code | 与 health 规则合并；表达移出 Safety | B/E | 单一 Triage 输出上线 |
| `explanation-service` | Delivery、Decision Record | SPLIT + ADAPT | NLG 和展示逻辑 | Patient/Clinician Delivery、统一 Prompt Route、证据边界 | D/E | 三类 Delivery 回归通过 |
| `ocr-service` | Tool、Evidence Intake | KEEP + ADAPT | OCR Adapter | SourceArtifact、质量状态、幂等、人工确认 | D | 报告链路 E2E 通过 |
| `execution-trace-service` | Observability | SPLIT + ADAPT | 时间线、调用图和 AgentEvent UI | 技术 Trace 迁 OTel；增加版本链字段；PHI 过滤 | C/F | OTel 与 AgentEvent Dashboard 完成 |
| `frontend` | Patient/Clinician/Admin UI | ADAPT | React、组件、ReactFlow | Thread、Interrupt、Review、Evidence、Release 管理 | C-F | 新页面覆盖目标场景 |
| `common/aidoctor_llm` | Model Governance | ADAPT | 供应商客户端与基础调用封装 | ProviderAdapter、Model Gateway、统一错误和指标 | A/C | 所有调用只经 Gateway |
| `science/` | Research | ARCHIVE | 研究代码和 Demo | 与生产依赖隔离、补数据声明 | A/F | 生产构建无依赖 |
| `scripts/` | Tooling | SPLIT | 有效批处理与迁移脚本 | 分类、幂等、参数校验和测试 | A-F | 每个脚本有 Owner |
| `docs/` 历史内容 | Documentation | SPLIT + ARCHIVE | 历史设计 | 当前真值与归档分离 | A | README 只指向当前基线 |
| `docker-compose.yml` | Platform | REWRITE | 端口和依赖经验 | Postgres、Redis、OTel、健康检查、无硬编码密钥 | A-C | 开发环境一键启动 |

---

## 4. v2.6 新增资产迁移矩阵

### 4.1 Prompt 与模型资产

| 当前资产形态 | 决策 | 目标资产 | 必须盘点 |
|---|---|---|---|
| Python/Java 字符串中的 Prompt | EXTRACT + ADAPT | `PromptSpec` / `PromptRelease` | 文件、调用者、输入、输出、风险、语言 |
| YAML/JSON 中的零散 Prompt | ADAPT | Git 管理 Prompt Registry | 版本、Owner、Reviewer、Eval |
| 服务内直接调用供应商 SDK | REWRITE | `ModelGateway.invoke()` | Provider、模型、超时、重试、PHI |
| 各服务独立模型配置 | MERGE | `ModelSpec` / `ModelRoutePolicy` | 模型能力、地区、成本、Fallback |
| 自行解析模型 JSON | REWRITE | Structured Output Validator | Schema、修复次数、失败策略 |
| 自行记录 Token/Cost | MERGE | ModelInvocationRecord / OTel | token、成本、延迟、route、版本 |
| 固定写死模型名称 | REWRITE | 稳定 `route_id` | 任务、风险、Capability、允许模型 |

### 4.2 Capability 资产

| 当前资产 | 决策 | 目标位置 |
|---|---|---|
| 呼吸道规则和问诊步骤 | EXTRACT + ADAPT | `adult_respiratory_v1/safety`、`question_policy` |
| 术语词典和编码映射 | EXTRACT + ADAPT | Terminology Pack |
| 字段、症状和体征定义 | EXTRACT + ADAPT | Observation Profile |
| 候选疾病和 must-not-miss 列表 | REVIEW + ADAPT | Hypothesis Pack |
| 允许的 Tool/Prompt/Model | NEW | Runtime Allowlist |
| 红旗病例和对话样例 | EXTRACT | Capability Eval Suite |
| 健康管理 A1-A5 | ARCHIVE / NEW CAPABILITY | 后续 Wellness Capability |

### 4.3 知识与 RAG 资产

| 当前资产 | 决策 | 目标位置 |
|---|---|---|
| 现有指南、论文和参考文档 | REVIEW | Source Registry |
| 无来源或无法确认许可的知识 | ARCHIVE | 禁止进入生产 Knowledge Release |
| 现有向量数据 | EVALUATE / REBUILD | pgvector Knowledge Index |
| 现有检索 Prompt | EXTRACT + ADAPT | Evidence Query / Claim Route |
| 现有 Neo4j 节点和关系 | EVALUATE + ADAPT | Knowledge Graph Enhancement |
| 现有症状—疾病关系 | REVIEW | Terminology/Graph Pack，必须保留来源 |
| 患者历史向量数据 | REWRITE | Patient RAG 独立索引和权限域 |
| 公共医学知识向量数据 | REWRITE | Medical RAG 独立 Knowledge Release |

知识图谱生产启用条件：来源可追踪、版本可回滚、患者数据隔离、对照实验显示净收益、无 Citation 替代行为。

---

## 5. diagnosis-service 迁移

```text
DiagnosisController
→ Business & Care Delivery API

DiagnosisOrchestrationService
→ 业务入口保留
→ Agent 路由迁 Python Runtime
→ 固定路径变为 FallbackWorkflowAdapter

CDPManager
→ State Committer + Repository

CDP Entity
→ EncounterCDP + Observation/Evidence/Decision Contracts

TraceContext / executionTrace
→ OTel Context + AgentEvent
```

重写点：

- `Map<String,Object>` 改为 `StatePatch`；
- 使用 `expected_cdp_version`；
- 远程模型和 Tool 调用不得处于临床状态事务中；
- Audit、Trace、Delivery 从 CDP 拆出；
- 旧响应通过 DTO Mapper 兼容；
- 固定 Workflow 使用同一 Capability、Prompt、Knowledge 和 Safety 版本快照。

---

## 6. Python 运行形态与旧服务 Adapter

目标首版：

```text
apps/agent-runtime/
└── FastAPI + LangGraph

packages/
├── clinical_intelligence/
├── safety_policy/
├── evidence_intelligence/
├── context_memory/
├── model_runtime/
├── tool_skill_governance/
├── durable_execution/
└── observability/
```

旧服务可暂时独立运行，但必须通过：

```text
Versioned Request
→ Limited Context
→ Legacy Adapter
→ Versioned Result
→ Validator
→ Candidate / StatePatch
```

旧服务不得自行读取完整 CDP、写数据库、选择未批准模型、加载未发布 Prompt 或访问未授权知识索引。

---

## 7. CDP 数据迁移矩阵

| 旧字段 | 新目标 | 处理 |
|---|---|---|
| `patient_id` | Encounter.patient_id | 映射 |
| `session_id` | Encounter + Thread | 拆分 |
| `version_no` | EncounterCDP.version | 映射并校验历史 |
| `cdp_status` | EncounterStatus / ThreadStatus | 状态映射 |
| `health_state_assessment` | TriageAssessment + DecisionRecord | 解析迁移 |
| `wellness_plan` | 独立 Wellness Capability | 延后迁移 |
| `patient_state` | ClinicalObservation + SourceArtifact | 逐字段解析 |
| `ddx` | DiagnosticHypothesis | 解析迁移 |
| `evidence_graph` | Evidence Ledger / Legacy Graph Archive | 保留来源，不自动转 EvidenceClaim |
| `workup_plan` | ClinicalPlanCandidate / ToolResult | 解析迁移 |
| `management_plan` | 高风险历史记录 | 只读迁移 |
| `triage` | TriageAssessment | 解析迁移 |
| `uncertainty` | InformationGap + Conflict | 解析迁移 |
| `audit_info` | Compliance Audit | 拆出 |
| `execution_trace` | AgentEvent / Legacy Trace Archive | 拆出 |
| `conclusion_package` | DeliveryPackage | 拆出 |

旧 JSON 必须保留原始 blob、checksum、旧 schema、migration version、parse error、unmapped fields 和 source record ID。

---

## 8. 前端迁移矩阵

| 当前能力 | 决策 | 目标 |
|---|---|---|
| 患者问诊页 | ADAPT | Encounter/Thread/Interrupt 状态驱动 |
| WebSocket 对话 | WRAP | 统一 Event Stream + Resume Token |
| 诊断状态页 | ADAPT | DeliveryPackage + Timeline |
| Trace 管理页 | SPLIT | Technical Trace、AgentEvent、Decision 分视图 |
| ReactFlow 调用图 | KEEP/ADAPT | Graph/Trace 可视化 |
| 医生页面 | NEW | ReviewTask、Evidence、Edit/Approve/Reject |
| 管理页面 | NEW/ADAPT | Capability、Prompt、Model、Knowledge Release、Eval |
| Zustand Store | ADAPT | Encounter/Thread/Review/Release 分域 |
| Axios Client | REWRITE | 版本化 API 和统一错误模型 |
| 前端测试 | NEW | Unit、Component、E2E、Accessibility |

---

## 9. 验证门禁

### KEEP / ADAPT / WRAP

- 编译和测试通过；
- 职责符合目标模块；
- 无绕过 State、Safety、Model Gateway 和 Knowledge Policy；
- 新旧输出可比较；
- 版本链和回滚明确。

### REWRITE

- 旧功能、Prompt、数据和测试清单完整；
- 新实现有 E2E；
- 双跑窗口和数据迁移完成；
- Owner 与 Reviewer 批准。

### ARCHIVE / REMOVE

- 无流量、无依赖；
- 数据和密钥已处理；
- 当前文档已替代；
- 全仓引用为空；
- 回滚期结束。

---

## 10. 初始迁移顺序

```text
M1 运行基线与全量 Inventory
→ M2 Shared Contracts + Capability/Prompt/Model/Knowledge Schemas
→ M3 State Committer 与 CDP Adapter
→ M4 合并 Safety 和抽取 adult_respiratory_v1
→ M5 ProviderAdapter + Prompt Loader + Model Gateway
→ M6 包装 Parsing/Dialog 核心能力
→ M7 最小 LangGraph + Checkpoint/Resume
→ M8 呼吸道 Knowledge Release + Evidence 接入
→ M9 新前端与医生审核
→ M10 OTel、发布治理与双跑
→ M11 旧服务下线
```

---

## 11. 当前结论

当前没有核心服务可立即 `REMOVE`。迁移原则是：保留可验证的领域逻辑，重写状态、安全、模型、知识和发布边界；将场景资产抽为 Capability Package；将散落的大模型调用收敛为统一 Model Runtime；将现有知识和知识图谱先治理、评估，再决定生产启用。