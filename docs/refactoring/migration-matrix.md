# AIdoctor 代码与资产迁移矩阵

> 文档状态：Draft v2.5 Migration Matrix  
> 更新时间：2026-07-29  
> 输入：[当前系统资产盘点](./current-system-inventory.md)  
> 目标：[总体架构与模块设计](./overall-architecture-and-module-design.md)

---

## 1. 处理类型

| 类型 | 定义 |
|---|---|
| KEEP | 职责和契约基本符合目标，可原位保留 |
| ADAPT | 核心逻辑有价值，需改接口、类型、权限或依赖 |
| WRAP | 暂时通过 Adapter 接入，待新实现稳定后迁移 |
| REWRITE | 职责、状态所有权或安全边界根本不符合，需要按新设计实现 |
| ARCHIVE | 不进入目标生产主链路，但保留历史、研究或参考价值 |
| REMOVE | 已确认无引用、无数据和无回滚价值后删除 |
| SPLIT | 当前资产跨越多个目标模块，需要拆分迁移 |
| MERGE | 多个当前资产职责重叠，合并为一个目标模块 |

任何 `REMOVE` 必须满足：

- 新路径已上线；
- 数据和流量已迁移；
- 无运行时引用；
- 回滚窗口结束；
- 归档或备份完成；
- Owner 批准。

---

## 2. 总体迁移策略

采用 Strangler Migration：

```text
建立新 Contracts
→ Adapter 包装旧能力
→ 第一条新纵向切片
→ 新旧双跑和差异比较
→ 分模块切换流量
→ 旧路径只读或 fallback
→ 达到下线门禁
→ Archive / Remove
```

不采用：

- 一次性重写所有服务；
- 先移动所有目录再修代码；
- 用服务改名代替职责重构；
- 未验证便删除旧 Workflow；
- 新旧代码同时直接写同一临床状态而无版本控制。

---

## 3. 顶层资产迁移矩阵

| 当前资产 | 目标模块 | 决策 | 主要保留 | 主要改造 | 阶段 | 下线/完成条件 |
|---|---|---|---|---|---|---|
| `diagnosis-service` | Business、Clinical State | SPLIT + ADAPT | API、Encounter/CDP经验、版本历史、固定 Workflow | 移除 Agent 编排职责；引入 Encounter、StatePatch、State Committer；旧 API Adapter | A-E | 新 Business API 和 State Committer 稳定，固定 Workflow 成为 fallback |
| `examination-service` | Business、Evidence Intake | REWRITE/ARCHIVE | 上传业务需求、API 草案 | 按 SourceArtifact、Consent、质量门和对象存储重写 | D/E | 新报告接入 E2E 通过 |
| `health-state-assessment-service` | Safety、后续 Wellness Capability | SPLIT + MERGE | 红旗、风险、输入校验、健康流程样例 | 红旗并入 Safety；健康管理拆为独立 Capability | B/E | Safety 规则回归通过，旧入口无流量 |
| `clinical-parsing-service` | Clinical Intelligence | ADAPT | 词表、归一化、抽取算法 | 输出 ObservationCandidate；去掉 CDP 直读写；真实质量指标 | B/C | 新抽取合同回归达到阈值 |
| `dialog-service` | Intelligence、Context、Runtime、Business | SPLIT + REWRITE | 信息缺口、问题模板、NLU/NLG、WS经验 | 拆分职责；QuestionDecision；ContextEnvelope；删除 CDP 直写 | C/D | 新 LangGraph 和通信层覆盖旧接口 |
| `diagnosis-engine-service` | Clinical Intelligence、Evidence | ADAPT + WRAP | KG、规则、模型 Adapter、候选分层 | 统一 DiagnosticHypothesis；证据方向；版本；禁止直接确诊 | D/F | 双跑差异通过临床评审 |
| `workup-planner-service` | Tool Governance、Clinical Intelligence | ARCHIVE + WRAP | 检查建议算法和验证计划 | 作为受控 Tool，增加证据、权限、医生审核 | E+ | 专项评估和审核链完成 |
| `treatment-engine-service` | 高风险 Tool | ARCHIVE | 数据、原型和术语 | 不进入首个主链路；未来按医生审核和证据重新设计 | 后续 | 高风险能力单独立项 |
| `risk-assessment-service` | Safety、Delivery | SPLIT + MERGE | 分诊、升级规则、reason code | 与 health 风险规则合并；Conclusion 移出 Safety | B/E | 单一 Safety Engine 上线 |
| `explanation-service` | Delivery、Decision Record | SPLIT + ADAPT | NLG、证据路径展示、可视化 | Evidence/Decision/Presentation 分责；受控 Context | D/E | 三类 Delivery 回归通过 |
| `ocr-service` | Tool、Evidence Intake | KEEP + ADAPT | OCR Adapter | SourceArtifact、质量状态、幂等、人工确认 | D | 报告链路 E2E 通过 |
| `execution-trace-service` | Observability | SPLIT + ADAPT | 时间线、调用树、AgentEvent UI | 技术 Trace 改 OTel；CDP Trace 分离；PHI 过滤 | C/F | OTel + AgentEvent Dashboard 完成 |
| `frontend` | Patient/Clinician/Admin UI | ADAPT | React、组件、流程页面、ReactFlow | 新 API、Interrupt/Resume、ReviewTask、角色拆分、测试 | C-E | 新页面覆盖目标场景 |
| `common/aidoctor_llm` | Model Governance | ADAPT | Provider Adapter | Model Registry、结构化输出、策略、PHI、版本、成本 | C/F | 所有模型调用经 Router |
| `science/` | Research | ARCHIVE | 研究代码和 Demo | 与生产依赖隔离、补数据声明 | A/F | 生产构建无依赖 |
| `scripts/` | Tooling | SPLIT | 有效批处理和迁移脚本 | 分类、参数校验、幂等、测试、危险操作保护 | A-F | 每个脚本有 Owner 和类别 |
| `docs/` 历史内容 | Documentation | SPLIT + ARCHIVE | 历史设计和知识 | 当前真值与历史归档分离 | A | README 只指向当前基线 |
| `docker-compose.yml` | Platform | REWRITE | 本地依赖和端口信息 | 完整 profile、无硬编码密码、Postgres/OTel、健康检查 | A-C | 新开发环境一键启动 |

---

## 4. diagnosis-service 详细迁移

### 4.1 保留

- 当前 `/start`、`/continue`、`/status`、`/result` 业务语义；
- 旧 API 兼容能力；
- JPA、Flyway 和基础异常处理；
- CDP 历史版本和回放数据作为迁移输入；
- 固定 Workflow 中经过验证的流程场景；
- 对外系统和前端已依赖的 DTO 字段映射。

### 4.2 拆分

```text
DiagnosisController
→ Business & Care Delivery API

DiagnosisOrchestrationService
→ 业务入口部分保留
→ Agent 路由迁往 Python Agent Runtime
→ 固定路径转为 FallbackWorkflowAdapter

CDPManager
→ 迁为 State Committer + Repository

CDP Entity
→ EncounterCDP + Observation/Evidence/Decision 等合同

TraceContext / executionTrace
→ OTel Context + AgentEvent
```

### 4.3 重写点

- `Map<String,Object>` 更新改为 `StatePatch`；
- `expected_cdp_version` 代替仅依赖悲观锁；
- 远程模型/工具调用不得在临床状态事务中执行；
- 旧 `cdpStatus` 映射到 EncounterStatus、ThreadStatus 和 DeliveryStatus；
- `conclusionPackage` 不再写入 patient_state；
- audit、trace 从 CDP 拆出；
- 默认继续流程的降级必须由 Safety Policy 审批。

### 4.4 迁移方式

```text
旧 Diagnosis API
→ Compatibility Controller
→ 新 Business Command
→ Python Runtime 或 Fixed Workflow Adapter
→ StatePatch
→ State Committer
→ 旧响应 DTO Mapper
```

---

## 5. Python 工具服务迁移

### 5.1 目标物理形态

第一阶段不继续维持每个步骤一个生产服务，建议：

```text
apps/agent-runtime/
└── FastAPI + LangGraph

packages/
├── clinical-intelligence/
├── safety-policy/
├── evidence-intelligence/
├── context-memory/
├── tool-skill-model-governance/
├── durable-execution/
└── observability/
```

旧服务暂时保留独立进程，通过 Tool Adapter 接入。

### 5.2 Adapter 协议

旧服务包装必须统一：

```text
ToolExecutionRequest
→ 限定 Context
→ Legacy Service Adapter
→ ToolResultV2
→ Result Validator
→ StatePatch Candidate
```

Adapter 必须补充：

- `tool_release_id`；
- `contract_version`；
- `capability_id`；
- `idempotency_key`；
- `source_ids`；
- `reason_codes`；
- `retryability`；
- `data_classification`；
- `trace_id/span_id`。

### 5.3 禁止

- 旧 Tool 自行读取完整 CDP；
- 旧 Tool 直接写数据库；
- ToolResult 的 suggested write 自动提交；
- 固定 confidence 常量作为验收质量；
- Tool 内部自行选择未批准模型；
- Tool 返回自然语言失败后 Runtime 继续推理。

---

## 6. Safety 合并矩阵

| 当前能力 | 当前位置 | 目标位置 | 决策 |
|---|---|---|---|
| 入口危险信号 | health-state | Safety Engine | ADAPT |
| 红旗识别 | health-state | Safety Engine | ADAPT |
| 风险等级 | health-state/risk | Safety Engine | MERGE |
| 分诊 | risk-assessment | Safety Engine | ADAPT |
| 升级规则 | risk-assessment | Safety Policy | ADAPT |
| 工作态选择 | health-state/Java | Capability Router | REWRITE |
| 健康管理 A1-A5 | health-state/Java | Wellness Capability | ARCHIVE/后续 |
| Conclusion Package | risk/explanation/Java | Delivery Builder | MERGE + REWRITE |
| 高风险医生审核 | 缺失/不完整 | Business + Runtime | NEW |

Safety 合并完成前，不允许同时让两个旧服务独立决定最终分诊。

---

## 7. Clinical Intelligence 迁移矩阵

| 能力 | 旧资产 | 目标合同 | 决策 |
|---|---|---|---|
| 临床概念抽取 | clinical-parsing | ObservationCandidate | ADAPT |
| 标准编码归一化 | clinical-parsing | ConceptReference | KEEP/ADAPT |
| 输入理解 | dialog | ObservationCandidate | SPLIT |
| 信息缺口 | dialog | InformationGap | ADAPT |
| 下一问题 | dialog | QuestionDecision | REWRITE 接口 |
| 诊断候选 | diagnosis-engine | DiagnosticHypothesis | ADAPT |
| KG 路径 | diagnosis-engine | EvidenceClaim/ReasoningPath | WRAP |
| 多引擎融合 | diagnosis-engine | InferenceEnsembleResult | ADAPT |
| 检查建议 | workup | ToolResult/ClinicalPlanCandidate | WRAP |
| 风险分诊 | health/risk | TriageAssessment | MERGE |
| 自然语言解释 | explanation/dialog | DeliverySection | ADAPT |

---

## 8. CDP 数据迁移矩阵

| 旧字段 | 新目标 | 处理 |
|---|---|---|
| `patient_id` | Encounter.patient_id | 映射 |
| `session_id` | Encounter + Thread | 拆分 |
| `version_no` | EncounterCDP.version | 映射并校验历史 |
| `cdp_status` | EncounterStatus/ThreadStatus | 状态映射 |
| `health_state_assessment` | TriageAssessment + Decision Record | 解析迁移 |
| `wellness_plan` | 独立 Wellness Capability 数据 | 延后迁移 |
| `patient_state` | ClinicalObservation + SourceArtifact | 逐字段解析 |
| `ddx` | DiagnosticHypothesis | 解析迁移 |
| `evidence_graph` | Evidence Ledger/EvidencePack | 解析并保留来源 |
| `workup_plan` | ClinicalPlan/ToolResult | 解析迁移 |
| `management_plan` | 高风险历史记录 | 只读迁移，不自动激活 |
| `triage` | TriageAssessment | 解析迁移 |
| `uncertainty` | InformationGap + Conflict | 解析迁移 |
| `audit_info` | Compliance Audit | 从临床主表拆出 |
| `execution_trace` | AgentEvent/Legacy Trace Archive | 从临床主表拆出 |
| `conclusion_package` in patient_state | DeliveryPackage | 拆出 |

旧 JSON 迁移必须保留：

- 原始 blob；
- checksum；
- 旧 schema version；
- migration version；
- parse error；
- unmapped fields；
- source record ID。

---

## 9. 前端迁移矩阵

| 当前页面/能力 | 决策 | 目标 |
|---|---|---|
| 患者问诊页 | ADAPT | Encounter/Thread/Interrupt 状态驱动 |
| WebSocket 对话 | WRAP | 统一 Event Stream，支持 Resume Token |
| 诊断状态页 | ADAPT | DeliveryPackage + Timeline |
| Trace 管理页 | SPLIT | Technical Trace 与 AgentEvent 分视图 |
| ReactFlow 调用图 | KEEP/ADAPT | Agent Graph/Trace 可视化 |
| 医生页面 | NEW | ReviewTask、Evidence、Edit/Approve/Reject |
| 管理页面 | NEW/ADAPT | Capability、Release、Eval、Audit |
| Zustand Store | ADAPT | 按 Encounter/Thread/Review 分域 |
| Axios API Client | REWRITE | 版本化 API Client 和错误模型 |
| 前端测试 | NEW | Unit/Component/E2E/Accessibility |

---

## 10. 文档迁移矩阵

### 当前真值

以下目录作为当前设计真值：

- `docs/refactoring/`；
- 后续 `docs/modules/`；
- 后续 `docs/contracts/`；
- 后续 `docs/adr/`。

### 历史归档

以下内容在完成分类后移动到 `docs/archive/`：

- 五脑/脑区旧架构；
- 与当前状态所有权冲突的设计；
- 已被 v2.5 替代的阶段路线；
- 给特定展示或汇报使用的非实施方案；
- 已完成且不再适用的开发过程文档。

归档文件顶部必须增加：

```text
ARCHIVED
Replaced by: <current document>
Archived at: <date>
Do not use as implementation source of truth.
```

---

## 11. 决策验证门禁

### KEEP

- [ ] 编译通过；
- [ ] 测试通过；
- [ ] 职责符合目标模块；
- [ ] 无绕过状态和安全门禁；
- [ ] 契约兼容。

### ADAPT/WRAP

- [ ] Adapter 契约；
- [ ] 输入字段最小化；
- [ ] 超时、重试、错误和幂等；
- [ ] 结构化结果校验；
- [ ] 新旧输出对比；
- [ ] 移除直接状态写入。

### REWRITE

- [ ] 旧功能和测试清单完整；
- [ ] 新实现有 E2E；
- [ ] 双跑窗口；
- [ ] 数据迁移和回滚；
- [ ] Owner 签字。

### ARCHIVE

- [ ] 无生产流量；
- [ ] 无生产依赖；
- [ ] 相关数据已保留；
- [ ] 当前文档已替代；
- [ ] 安全密钥已移除。

### REMOVE

- [ ] Archive 条件全部满足；
- [ ] 回滚期结束；
- [ ] 全仓引用扫描为空；
- [ ] CI 通过；
- [ ] 删除 PR 单独提交。

---

## 12. 初始迁移顺序

```text
M1 统一 Contracts
→ M2 State Committer 和 CDP Adapter
→ M3 合并 Safety
→ M4 包装 Parsing / Dialog 核心能力
→ M5 最小 Python LangGraph Runtime
→ M6 Checkpoint / Resume
→ M7 新前端主链路
→ M8 Diagnosis Engine / Evidence 接入
→ M9 Doctor Review / Delivery
→ M10 OTel 和 Trace 迁移
→ M11 旧服务下线
```

---

## 13. 当前结论

当前不建议把任何核心旧服务立即标记为 `REMOVE`。

当前最合理的策略是：

- `diagnosis-service`：拆分和适配；
- parsing、dialog、diagnosis engine：保留领域逻辑，重写边界；
- health/risk：合并安全规则；
- workup/treatment：首阶段归档或包装；
- explanation/trace/frontend：保留展示资产，重构数据源；
- CDP：保留历史和版本，重构为新状态合同；
- Python 多服务：逐步收拢为一个 Runtime 内的模块包。
