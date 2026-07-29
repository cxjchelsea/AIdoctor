# AIdoctor 可执行实施路线

> 文档状态：Draft v2.6 Executable Roadmap  
> 更新时间：2026-07-29  
> 目标：覆盖代码改造、Capability、Model Runtime、RAG、数据迁移、前端、工程、评估、发布和旧系统下线。

## 1. 路线定位

```text
总体架构
定义最终系统是什么

Architecture Freeze Baseline
定义哪些决策稳定、如何变更

Implementation Roadmap
定义按什么顺序做、交付什么、如何验收

Migration Matrix
定义旧资产如何处理

Coverage Matrix
检查目标能力是否遗漏
```

本路线可以更新任务状态和实现细节，但不得绕过冻结的状态所有权、安全骨架和依赖方向。

## 2. 执行原则

1. 按纵向闭环推进，不按十一个模块分别大而全开发；
2. 每个 Phase 必须有可运行场景和 Exit Gate；
3. 先稳定 Contracts、Capability 和状态所有权，再引入 LangGraph；
4. 保留旧 Workflow，通过 Adapter、双跑和流量切换迁移；
5. 每阶段包含后端、前端、数据、测试、可观测性和发布；
6. LLM 只通过 Model Runtime 调用；
7. 每个临床场景以 Capability Package 发布；
8. 红旗和分诊不依赖 LLM 单点执行；
9. 每个数据库变化都有 Migration、Reconciliation 和 Rollback；
10. 旧服务只有满足 Decommission Gate 后才能归档或删除。

## 3. 目标纵向切片

```text
成人患者：“咳嗽三天，有点喘”
→ 创建 Encounter / Thread
→ 加载 adult_respiratory_v1
→ Observation Extraction
→ State Committer
→ Mandatory Safety
→ InformationGap / QuestionDecision
→ Question Wording
→ Checkpoint / Interrupt
→ 服务重启
→ Resume
→ 更新 Observation / Triage
→ Phase D 增加有限 Hypothesis 和白名单 Evidence
→ Phase E 增加医生审核和 Delivery
```

该切片贯穿 Phase B-E，每个阶段在前一阶段闭环基础上增加能力。

---

# Phase A：真实基线、Contracts、Capability 与架构冻结

## A0 目标

- 证明当前项目真实能否编译、启动和运行；
- 完成代码、数据、Prompt、规则和接口级 Inventory；
- 形成 Shared Contracts v1；
- 建立 Capability 和 Model Runtime 配置骨架；
- 完成关键 ADR；
- 将 Freeze Candidate 升级为 Frozen Baseline。

## A1 Java 基线

- `diagnosis-service` Maven compile/test；
- `examination-service` Maven compile/test；
- 盘点 JDK、Spring Boot、Oracle/MySQL、Flyway 和 Nacos 依赖；
- 记录编译错误、测试缺口和运行前置条件；
- 识别所有 CDP 写入点；
- 识别固定 Workflow 入口和 fallback 能力；
- 输出 Java API、Entity、Repository、Client、Prompt/LLM 调用清单。

交付：

- `java-build-baseline.md`；
- 可复现 Maven 命令；
- Java 资产更新到 Inventory/Migration Matrix。

## A2 Python 基线

- 为每个 Python 服务创建可复现依赖环境；
- 统一记录 Python、FastAPI、Pydantic、LLM SDK 版本；
- 启动 Parsing、Dialog、Safety、Diagnosis Engine、OCR 和 Trace 候选服务；
- 盘点所有模型直接调用点；
- 盘点 Prompt 文件、字符串和环境变量；
- 盘点 ToolContext/ToolResult 变体；
- 盘点 Redis、Neo4j、Milvus 等依赖。

交付：

- `python-runtime-baseline.md`；
- dependency lock strategy；
- Prompt/Model Call Inventory。

## A3 Frontend 与 Docker 基线

- Frontend build/lint；
- 增加最小 test runner 决策；
- 运行现有患者流程；
- 盘点 WebSocket/STOMP/API 状态；
- 校验 Docker Compose 服务缺失和环境变量不一致；
- 形成当前部署拓扑。

## A4 数据资产盘点

- 导出现有数据库表和索引；
- 抽样 CDP CLOB/JSON 字段；
- 统计隐藏字段、空字段、非法 JSON 和版本分布；
- 盘点 Redis Key；
- 盘点 Neo4j Schema 和数据来源；
- 盘点 Milvus 是否真实使用；
- 识别 PHI、Consent 和 Tenant 缺口。

## A5 Shared Contracts v1

首批必须落地为 JSON Schema：

```text
Encounter
EncounterCDP
ClinicalObservation
ObservationCandidate
SourceArtifact
StatePatch
CommitResult
TriageAssessment
InformationGap
QuestionDecision
CapabilityManifest
ContextEnvelope
ToolResult
ModelInvocationRequest
ModelInvocationResult
CheckpointMetadata
InterruptRecord
ResumeRequest
AgentEvent
```

任务：

- JSON Schema 作为源；
- 生成 Pydantic；
- 生成 Java DTO；
- 生成 TypeScript 类型；
- Schema Version；
- backward compatibility policy；
- contract fixtures；
- Java/Python/TS contract tests。

## A6 Capability Package 骨架

建立：

```text
capabilities/adult_respiratory_v1/
```

至少创建：

- manifest；
- population/scope；
- terminology；
- observation profile；
- safety；
- question；
- hypothesis；
- knowledge policy；
- runtime allowlist；
- eval fixtures。

A 阶段只要求骨架、Schema 和临床待评审内容，不要求全部临床规则完成。

## A7 Model Runtime 骨架

- ModelSpec；
- ModelRoutePolicy；
- PromptSpec；
- Output Schema Registry；
- ProviderAdapter 接口；
- `common/aidoctor_llm` Legacy Adapter 设计；
- Prompt YAML 规范；
- 禁止新代码直接调用 Provider SDK 的 lint/architecture rule。

## A8 ADR

必须完成：

1. 临床主库 PostgreSQL 还是阶段性 MySQL/Oracle；
2. Checkpoint 与临床库部署边界；
3. Java/Python 通信协议；
4. Python 首版单 Runtime package 模式；
5. LangGraph 使用方式；
6. pgvector 与 Milvus；
7. BM25 实现；
8. Neo4j 保留和生产启用门禁；
9. Secret Manager；
10. OTel Backend。

## A9 CI 与开发环境

Pipeline 至少包含：

- Java compile/test；
- Python install/lint/type/test；
- Frontend build/lint/test；
- Contract generation/diff；
- Schema compatibility；
- secret/dependency scan；
- docs link check；
- migration dry-run。

## A10 固定 Workflow 基线

- 跑通或明确无法跑通原因；
- 记录正常、服务失败和默认降级行为；
- 建立输入输出 fixture；
- 确定新旧双跑对比字段；
- 保留为 Phase C/F fallback。

## A11 Freeze Review

Exit Gate：

- [ ] Java/Python/Frontend 基线完成；
- [ ] 所有生产候选目录已进入 Inventory；
- [ ] 每个现有服务有 Migration Decision；
- [ ] Contracts v1 可生成三种语言类型；
- [ ] Capability Package 骨架完成；
- [ ] Model Runtime 接口和 Prompt 格式完成；
- [ ] 关键 ADR 批准；
- [ ] 固定 Workflow 有运行证据或明确失败原因；
- [ ] 第一条 E2E 测试设计完成；
- [ ] Coverage Matrix 无未归属能力；
- [ ] 总体状态升级为 Frozen Baseline。

---

# Phase B：Clinical State、State Committer 与呼吸道 Safety

## B0 运行场景

患者输入后，系统创建有来源的 Observation，经过唯一 State Committer 写入 CDP，并执行不可绕过的呼吸道 Safety。

## B1 新 Clinical State

- Encounter/EncounterCDP Repository；
- Observation/Event-style Evidence Ledger；
- SourceArtifact；
- CDP Version；
- Expected Version；
- Snapshot；
- Patient/Tenant Scope。

## B2 State Committer

公开接口：

```python
def commit_state_patch(patch: StatePatch) -> CommitResult: ...
```

校验：

- Schema；
- Capability；
- 字段权限；
- 来源；
- Consent；
- expected version；
- conflict；
- operation type；
- Audit/AgentEvent。

## B3 Legacy CDP Adapter

- 读取旧 CLOB/JSON；
- 转换为新 Contract；
- 新状态映射回旧响应；
- 不允许新模块直接更新旧 Map；
- 双写只在 ADR 批准后启用；
- 迁移和对账记录。

## B4 adult_respiratory_v1 Safety Pack

- 主诉和范围识别；
- 红旗规则；
- 生命体征规则；
- 组合风险；
- 输入不足保守升级；
- 特殊人群排除；
- Emergency Guidance；
- 固定安全输出。

Safety 最终判断使用确定性规则，模型只可提供 Candidate。

## B5 Clinical Parsing Adapter

- 旧 Parsing 输出适配到 ObservationCandidate；
- 保存原始 Span；
- 否定、时间、程度；
- 概念归一化；
- 不直接写 CDP；
- 规则抽取 fallback。

## B6 Business API v2 基础

- Create Encounter；
- Submit Input；
- Get Encounter State；
- v1 Compatibility Adapter；
- correlation id；
- Tenant/Consent 占位门禁。

## B7 Frontend

- 新 Encounter ID；
- 输入提交状态；
- 红旗/升级 UI；
- 错误和 correlation id；
- v1/v2 feature flag。

## B8 测试

- StatePatch 正常、部分、拒绝、冲突；
- 无来源写入；
- 旧 CDP 转换；
- 红旗病例集；
- 输入不足；
- out-of-scope；
- Planner 绕过 Safety 尝试；
- 并发版本冲突。

Exit Gate：

- [ ] 所有新临床写入经过 State Committer；
- [ ] Candidate 不自动成为 Fact；
- [ ] 呼吸道 Safety 可独立于 LLM 运行；
- [ ] v1 旧 API 仍可用；
- [ ] 状态版本和来源可追溯；
- [ ] 红旗测试达到批准阈值。

---

# Phase C：最小 Agent Runtime、Model Runtime 与跨轮恢复

## C0 运行场景

系统能够动态选择下一问题，调用统一 Model Runtime 生成结构化抽取和问题表达，保存 Checkpoint，服务重启后恢复且不重复写入。

## C1 Python Agent Runtime

初始 package：

```text
agent_runtime/
├── graph/
├── clinical_state/
├── safety/
├── clinical_intelligence/
├── context/
├── model_runtime/
├── durable/
├── tools/
└── observability/
```

不继续为每个临床步骤创建独立微服务。

## C2 最小 LangGraph

节点：

```text
validate_input
→ assemble_context
→ understand_input
→ commit_observations
→ mandatory_safety
→ identify_information_gap
→ choose_next_question
→ word_question
→ checkpoint_interrupt
→ resume
→ prepare_basic_delivery
```

只有 Question/NextAction 在批准范围内动态。

## C3 ContextEnvelope v1

- System Policy；
- Capability Snapshot；
- Critical Clinical Context；
- selected observations；
- recent messages；
- asked questions；
- token budget；
- redaction manifest；
- context hash。

## C4 Model Runtime v1

必须实现：

- Prompt Registry（Git YAML）；
- Prompt Loader；
- Prompt Builder；
- Model Registry；
- Model Router；
- Model Gateway；
- ProviderAdapter；
- Structured Output Validator；
- Timeout/Retry/Fallback；
- Token/Cost/Trace。

首批 Route：

```text
clinical.extract_observations.medium.v1
clinical.question_wording.low.v1
```

可选 Route：

```text
safety.extract_red_flags.high.v1
```

但 Safety 最终结论仍为确定性。

## C5 Prompt v1

至少发布：

- respiratory clinical understanding；
- respiratory question wording；
- basic patient delivery wording。

每个 Prompt 有：

- ID/version；
- input/output Schema；
- Capability；
- Route；
- Owner/Reviewer；
- Eval；
- rollback version。

## C6 InformationGap 与 QuestionDecision

- 必问清单；
- 红旗优先；
- 已问问题；
- 信息价值；
- 用户负担；
- 停止和升级；
- 模型只能辅助 Candidate 或语言表达。

## C7 Durable Execution v1

- Thread/Run；
- PostgreSQL Checkpointer；
- Interrupt；
- ResumeRequest；
- Resume Inbox 去重；
- expected checkpoint/CDP version；
- process restart；
- stale/concurrent Resume。

## C8 Tool Runtime v1

- Tool Registry 基础；
- ToolContext/ToolResult 统一；
- Parsing/Safety Legacy Adapter；
- Timeout；
- Cancel；
- Policy；
- Tool Result Validation。

## C9 OTel v1

- Java→Python propagation；
- Graph node spans；
- Model spans；
- Tool spans；
- Checkpoint spans；
- log correlation；
- AgentEvent timeline；
- PHI Filter。

## C10 Frontend Agent Flow

- Thread Store；
- Interrupt UI；
- Resume idempotency；
- reload recovery；
- SSE 事件；
- connection degraded UI；
- basic PatientDelivery。

## C11 Crash Matrix

- before State Commit；
- after commit before checkpoint；
- after checkpoint before response；
- Provider timeout；
- Prompt invalid；
- invalid structured output；
- duplicate/stale/concurrent Resume；
- Redis unavailable；
- PostgreSQL unavailable；
- fixed Workflow fallback。

Exit Gate：

- [ ] 第一条 Agentic 主链路可运行；
- [ ] 所有模型调用经过 Model Runtime；
- [ ] 业务代码无 Provider 直接调用；
- [ ] Prompt、Model、Schema 和 Context 可追踪；
- [ ] 服务重启可恢复；
- [ ] 重复提交不重复写入；
- [ ] 每轮 Mandatory Safety；
- [ ] 高风险无模型时进入固定路径；
- [ ] 前端刷新可恢复；
- [ ] Crash Matrix 通过。

---

# Phase D：有限临床推理、Patient History 与呼吸道 RAG V1

## D0 运行场景

Agent 结合当前 Observation、批准候选、有限患者历史和白名单知识，形成支持/反对证据、信息缺口和带引用的有限说明。

## D1 Hypothesis Pack 与 Clinical Engine

- 只加载 Capability 批准候选；
- common/alternative/must-not-miss；
- support/against；
- discriminator；
- uncertainty；
- rule/KG/statistical/LLM adapters；
- failure isolation；
- 不输出正式确诊。

## D2 Patient History Retrieval

- patient/tenant scope；
- consent；
- structured filter；
- 时间相关性；
- reason for recall；
- 不发送全历史 Prompt；
- cross-patient=0。

## D3 Memory v1

- MemoryCandidate；
- Write Gate；
- 仅确认事实；
- source/expiry；
- correction hook；
- recall trace；
- 模型推断不自动晋升。

## D4 Source Registry

- 具体白名单来源版本；
- Publisher、Tier、Region、Population；
- License 和 Access；
- valid_from/valid_until；
- clinical review；
- checksum；
- source manifest。

## D5 Knowledge Ingestion

- Artifact 安全检查；
- 结构解析；
- 推荐、表格、算法和限制提取；
- 术语归一化；
- RespiratoryKnowledgeChunk；
- 自动质量检查；
- 临床抽样审核；
- Knowledge Release。

## D6 RAG Storage 与 Retrieval

- PostgreSQL knowledge schema；
- pgvector；
- BM25 Adapter；
- Embedding Registry；
- Reranker Registry；
- metadata/ACL filter；
- hybrid retrieval；
- no-result；
- cache namespace；
- Retrieval Audit。

Embedding/Reranker 通过离线评估选型，不在代码中硬编码供应商名称。

## D7 Knowledge Graph 对照实验

- 图 Schema；
- 来源链；
- 关系有效期；
- terminology expansion；
- must-not-miss expansion；
- retrieval comparison；
- false expansion；
- latency。

只有净收益报告通过后，Neo4j 才进入生产路径；否则保持离线或归档。

## D8 Evidence Intelligence

- ClinicalQuestion/Query Type；
- PICO/Query Plan；
- Claim Extraction Route；
- SourceSpan；
- Citation Validation；
- Applicability；
- Freshness；
- Conflict；
- Limitations；
- EvidencePack。

## D9 Model Runtime 扩展

新增 Route：

```text
clinical.hypothesis_support.high.v1
evidence.classify_question.medium.v1
evidence.build_query.medium.v1
evidence.extract_claims.medium.v1
evidence.validate_citation.high.v1
evidence.summarize_conflict.high.v1
```

高风险 Route 无已批准模型时返回 unavailable/review，不静默降级。

## D10 Context Summary

- structured summary；
- source links；
- Critical Pin；
- contradiction retention；
- summary route；
- fact faithfulness evaluation。

## D11 OCR/Artifact

- Object Storage；
- SourceArtifact；
- checksum；
- file/MIME/malware gate；
- OCR Adapter；
- quality score；
- patient/report identity；
- human confirmation；
- Artifact ObservationCandidate。

## D12 Frontend Evidence

- report upload；
- parse/quality state；
- citation display；
- source span；
- permission；
- uncertainty/conflict/no-result；
- knowledge version。

## D13 Evaluation

- extraction；
- hypothesis quality；
- must-not-miss recall；
- patient isolation；
- retrieval Recall@k/Precision/nDCG；
- citation precision；
- population match；
- stale source rejection；
- injection；
- graph false expansion；
- latency/cost。

Exit Gate：

- [ ] 候选有支持和反对证据；
- [ ] 候选不超出 Capability 批准范围；
- [ ] Patient RAG 跨患者为零；
- [ ] Medical RAG 仅使用批准 Knowledge Release；
- [ ] 每个关键 Claim 可追踪 SourceSpan；
- [ ] 无结果、冲突、人群不匹配可显式返回；
- [ ] Citation 支持对应 Claim；
- [ ] Knowledge Release 可回滚；
- [ ] Critical Context 不因摘要丢失；
- [ ] OCR 不自动成为确认事实；
- [ ] 知识图谱若启用具有净收益报告。

---

# Phase E：医生审核、Delivery 与业务闭环

## E0 运行场景

高风险或不确定 Encounter 进入医生审核。医生可以修改、批准、拒绝或要求更多信息；系统生成三类 Delivery 并创建随访和模拟外部动作。

## E1 ReviewTask

- create/assign/priority；
- reason codes；
- expected versions；
- approve/edit/reject/request more info/escalate；
- SLA；
- Audit。

## E2 Clinician Interrupt/Resume

- checkpoint link；
- reviewer auth；
- concurrent/stale decision；
- doctor StatePatch；
- commit；
- resume。

## E3 Clinician Console

- queue；
- source facts；
- hypotheses；
- evidence/citations；
- triage；
- AgentEvent timeline；
- edit/decision；
- audit trail。

## E4 DeliveryPackage

```text
PatientDelivery
ClinicianDelivery
SystemDelivery
```

- 同一事实基础；
- 不同披露范围；
- 版本一致；
- 限制说明；
- 不暴露 CoT/Prompt。

## E5 Model Runtime 扩展

新增：

```text
delivery.patient_rewrite.low.v1
review.clinician_summary.high.v1
delivery.followup_wording.low.v1
```

医生摘要关键事实必须由结构化字段校验，不能只依赖生成文本。

## E6 Care Navigation 与 Follow-up

- care level；
- urgency；
- location capability；
- unavailable fallback；
- plan/task/reminder；
- response；
- deterioration escalation；
- cancel/expire。

## E7 Outbox/Inbox 与模拟外部动作

- transactional outbox；
- consumer inbox；
- ExternalActionRecord；
- idempotency；
- timeout=unknown；
- status lookup；
- compensation hook；
- replay disabled。

## E8 Frontend Closure

- PatientDelivery；
- waiting review；
- review changes；
- follow-up；
- status/retry；
- support correlation id。

Exit Gate：

- [ ] 医生修改进入真实 CDP；
- [ ] 高风险不能绕过医生；
- [ ] 三类 Delivery 一致；
- [ ] 关键医生摘要事实经过校验；
- [ ] Outbox/Inbox 不丢事件；
- [ ] 重复请求不重复动作；
- [ ] 患者和医生前端闭环；
- [ ] Audit 可追踪受保护操作。

---

# Phase F：治理、生产硬化、评估、放量与下线

## F1 Capability Governance

- Registry；
- clinical/technical review；
- Eval Report；
- Shadow；
- Clinician Assist；
- Restricted Patient；
- Active/Deprecated/Retired；
- emergency disable；
- rollback。

## F2 Prompt/Model Governance

- Prompt Registry 后台或受控配置；
- Model Health；
- Compatibility Matrix；
- cost/latency policy；
- provider/region policy；
- prompt/model shadow compare；
- emergency disable；
- release and rollback audit。

## F3 Tool/Skill Governance

- Tool Registry 完整化；
- Skill Registry；
- Owner/Reviewer；
- Compatibility；
- Sandbox；
- permissions；
- version and rollback。

## F4 Durable Hardening

- Thread Lease；
- worker heartbeat/takeover；
- Checkpoint Migration；
- legacy runtime；
- cancellation/expiry；
- reconciliation；
- compensation；
- fixed Workflow fallback drills。

## F5 Observability Stack

- OTel Collector；
- Tempo/Jaeger；
- Prometheus；
- Loki/OpenSearch；
- Grafana；
- SLO/Alert；
- PHI telemetry gate；
- Agent/Clinical/Audit 分层视图。

## F6 Evaluation 与 Replay

- Clinical/Safety/Context/Memory/RAG/Model/Prompt/Resume/Security；
- Patient Simulator；
- Simulation Replay；
- version comparison；
- no real side effects；
- release report automation。

## F7 生产基础设施

- Secret Manager；
- backup/restore；
- disaster recovery；
- performance/capacity；
- data retention/deletion；
- operational runbooks；
- incident levels。

## F8 放量

```text
Internal
→ Shadow
→ Clinician Assist
→ Restricted Patient
→ Controlled Expansion
```

每一步有：

- Capability version；
- Knowledge Release；
- Prompt Release；
- Model Route Policy；
- Eval Report；
- Rollback；
- monitoring threshold。

## F9 旧服务下线

每个旧服务满足：

- 新能力覆盖；
- 无生产流量；
- 无代码引用；
- 数据迁移和对账；
- rollback window；
- Archive Snapshot；
- Owner 批准；
- Decommission Record。

Exit Gate：

- [ ] Capability 发布和停用可控；
- [ ] Prompt/Model/Knowledge 可独立回滚；
- [ ] 高风险静默降级为零；
- [ ] Replay 不产生真实副作用；
- [ ] 版本链可还原；
- [ ] PHI 遥测门禁通过；
- [ ] 备份恢复演练通过；
- [ ] 生产 SLO 和 Incident Runbook 完成；
- [ ] 旧服务按门禁下线。

---

# 4. 跨阶段任务

## 4.1 文档状态

每完成一个阶段必须更新：

- Implementation Roadmap；
- Coverage Matrix；
- Migration Matrix；
- Current System Inventory；
- ADR；
- Release Notes；
- Eval Report。

## 4.2 Definition of Done

任何任务完成至少满足：

- 代码合并；
- Contract 和 Migration；
- Unit/Integration 测试；
- E2E 或阶段场景；
- Trace/Metric/Log；
- Security/PHI；
- Rollback；
- 文档更新。

## 4.3 禁止提前建设

在对应前置未满足前，不建设：

- 全医学知识库；
- 自由自治 Agent；
- 自动治疗和处方；
- 多套 Agent 框架；
- 未治理的 Prompt 平台；
- 无来源知识图谱；
- 无评估模型自动路由；
- 真实不可逆外部动作。

## 4.4 当前第一批实际任务

```text
1. 执行 A1-A4 真实基线
2. 生成 Contracts v1
3. 创建 adult_respiratory_v1 配置骨架
4. 创建 PromptSpec/ModelSpec/RoutePolicy Schema
5. 适配 common/aidoctor_llm 的第一个 ProviderAdapter
6. 完成数据库/Runtime/RAG ADR
7. 建立 CI
8. Freeze Review
```
