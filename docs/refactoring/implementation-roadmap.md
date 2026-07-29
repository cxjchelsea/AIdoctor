# AIdoctor 可执行实施路线

> 文档状态：Draft v2.5 Executable Roadmap  
> 更新时间：2026-07-29  
> 目标：作为总体架构冻结后的主要执行文件，覆盖代码改造、数据迁移、前端、工程、评估、发布和旧系统下线。

---

## 1. 路线定位

本文件是执行路线，不替代总体架构。

```text
总体架构
定义系统最终是什么

实施路线
定义按什么顺序做、交付什么、如何验收

Migration Matrix
定义旧资产如何处理

Coverage Matrix
检查有没有遗漏
```

路线可更新任务状态、依赖和排期，但不得绕过 Architecture Freeze Baseline。

---

## 2. 执行原则

1. 按纵向闭环推进，不按模块分别大而全开发；
2. 每个 Phase 必须有运行场景；
3. 先稳定 Contracts 和状态所有权，再引入 LangGraph；
4. 先保留旧能力，通过 Adapter 迁移；
5. 每个阶段包含后端、前端、数据、测试和运维；
6. 每个临床能力必须有 Safety 和 Eval；
7. 每个数据库变化必须有 Migration 和 Rollback；
8. 每个旧服务下线必须有替代能力和流量证据；
9. 每个发布绑定 Capability/Prompt/Model/Knowledge 版本；
10. 未满足 Exit Gate 不进入下一阶段生产范围。

---

## 3. 工作流

并行工作流：

| Track | 内容 |
|---|---|
| T1 Architecture & Contracts | 架构、ADR、Schema、兼容性 |
| T2 Clinical State & Data | CDP、Observation、Migration |
| T3 Safety & Clinical Intelligence | 红旗、分诊、抽取、问题、推理 |
| T4 Agent Runtime & Durable | LangGraph、Checkpoint、Resume、Outbox |
| T5 Evidence, Context & Governance | RAG、Memory、Tool、Skill、Model |
| T6 Business & Frontend | API、患者端、医生端、Delivery |
| T7 Observability & Evaluation | OTel、AgentEvent、Audit、Eval |
| T8 Platform & Release | CI/CD、环境、备份、灰度、下线 |

每个 Phase 从多个 Track 选取形成完整闭环。

---

# Phase A：现状基线与架构冻结

## A0 目标

把“设计正确”转换为“知道旧系统真实状态，并能安全开工”。

## A1 代码运行基线

### Tasks

- A1.1 Java diagnosis-service `mvn clean verify`；
- A1.2 Java examination-service `mvn clean verify`；
- A1.3 所有 Python 服务建立可复现依赖安装；
- A1.4 所有 Python 服务 health check；
- A1.5 frontend `npm ci/lint/build`；
- A1.6 补 frontend test framework；
- A1.7 Docker Compose 现状启动；
- A1.8 记录所有编译、启动和接口错误；
- A1.9 扫描 TODO/stub/hard-coded response；
- A1.10 扫描 secret 和 PHI 日志风险。

### Outputs

- `baseline-build-report.md`；
- `runtime-smoke-report.md`；
- `known-blockers.md`；
- 修复基线 PR。

## A2 代码和数据资产盘点

### Tasks

- A2.1 所有服务、包、类、API；
- A2.2 所有 CDP 读写位置；
- A2.3 所有数据库表和 Flyway；
- A2.4 Redis keys；
- A2.5 Neo4j schema/规模；
- A2.6 Prompt、模型、词表和规则；
- A2.7 ToolContext/ToolResult 复制点；
- A2.8 Trace/Log/Audit 写入点；
- A2.9 前端页面、路由、Store、API；
- A2.10 science/scripts/docs 分类。

### Outputs

- 更新 `current-system-inventory.md`；
- 更新 `migration-matrix.md`；
- 代码到 11 模块映射；
- Owner 列表。

## A3 Shared Contracts v1

### Tasks

定义并评审：

- Encounter；
- EncounterCDP；
- ClinicalObservation；
- ObservationCandidate；
- SourceArtifact；
- StatePatch；
- CommitResult；
- TriageAssessment；
- InformationGap；
- QuestionDecision；
- CapabilityEnvelope；
- ContextEnvelope；
- AgentState；
- ToolExecutionRequest；
- ToolResult；
- Thread/Run；
- CheckpointMetadata；
- InterruptRecord；
- ResumeRequest；
- AgentEvent；
- ErrorResponse。

建立：

- JSON Schema source；
- Java/Python/TypeScript generated models；
- contract version；
- compatibility policy；
- contract test。

## A4 架构 ADR

必须完成：

- ADR-051 Clinical Database Target；
- ADR-052 Java/Python Runtime Boundary；
- ADR-053 Contract Source and Code Generation；
- ADR-054 Legacy Workflow Fallback；
- ADR-055 Nacos/Service Discovery Transition；
- ADR-056 Vector Store: pgvector vs Milvus；
- ADR-057 OTel and Legacy Trace Transition。

## A5 开发环境与 CI

### Tasks

- 统一 Python/Pydantic；
- 建立 lock file；
- CI 基线；
- Contract generation；
- secret scan；
- dependency scan；
- container build；
- compose profiles；
- test fixtures；
- synthetic patient data。

## A6 固定 Workflow 基线

### Scenario

使用一个合成呼吸道病例跑通当前路径。

记录：

- 输入；
- 调用服务；
- CDP 前后状态；
- 问题；
- 风险；
- 输出；
- 耗时；
-失败和降级。

若无法运行，必须记录根因和修复/替代决策。

## A7 前端和业务基线

- 旧 API 调用图；
- 页面截图；
- WebSocket/STOMP 行为；
- Trace 页面数据源；
- 前端状态模型；
- v1 兼容范围。

## A8 Freeze Review

更新：

- Architecture Freeze Baseline；
- Coverage Matrix；
- Migration Matrix；
- Implementation Roadmap。

### Phase A Exit Gate

- [ ] 核心服务状态有真实报告；
- [ ] 每个资产有目标归属和处理类型；
- [ ] Shared Contracts v1 生成成功；
- [ ] 数据库和 Runtime ADR 批准；
- [ ] CI 基线可运行；
- [ ] 固定 Workflow 有基准或明确替代；
- [ ] 第一条纵向切片输入/输出冻结；
- [ ] 总体架构进入 Frozen Baseline。

---

# Phase B：最小临床状态与不可绕过 Safety

## B0 运行场景

```text
“咳嗽三天，有点喘”
→ 创建 Encounter
→ 抽取 ObservationCandidate
→ State Committer
→ Mandatory Safety
→ 返回结构化安全结果
```

暂不使用动态多轮 Agent。

## B1 新 Clinical State Schema

- Encounter；
- EncounterCDP；
- ClinicalObservation；
- SourceArtifact metadata；
- TriageAssessment；
- StateCommitLog；
- optimistic version；
- migration metadata。

## B2 State Committer

实现：

```text
validate schema
validate capability
validate field permission
validate source
validate consent
validate expected version
resolve conflict
commit
emit outbox/event
```

状态：

- committed；
- partially_committed；
- rejected；
- version_conflict；
- consent_denied；
- invalid_source。

## B3 Legacy CDP Adapter

- read old CDP；
- map legacy patient_state；
- raw payload archive；
- contract response mapper；
- no direct new Agent write to old CLOB。

## B4 Parsing Adapter

改造 clinical-parsing：

- 输入 SourceArtifact/TextSource；
- 输出 ObservationCandidate；
- negation/uncertainty；
- source span；
- confidence calibration；
- remove direct CDP read/write；
- regression tests。

## B5 Safety Engine v1

合并 health/risk：

- 成人呼吸道红旗；
- 基础分诊；
- 输入不足；
- special risk hooks；
- reason code；
- emergency guidance；
- deterministic rules；
- no Planner bypass。

## B6 Business API v2 基础

- create Encounter；
- submit initial message；
- get Encounter state；
- error model；
- v1 Compatibility Adapter。

## B7 Frontend v2 基础

- API client generated；
- Encounter Store；
- initial input；
- Safety result；
- risk banner；
- feature flag。

## B8 Observability 基础

- trace/correlation IDs；
- State Commit span；
- Safety span；
- structured logs；
- AgentEvent-like business event；
- PHI filter v1。

## B9 Tests

- normal；
- red flag；
- no source；
- candidate not confirmed；
- version conflict；
- invalid consent；
- legacy mapping；
- frontend E2E。

### Phase B Exit Gate

- [ ] 所有新临床写入经过 State Committer；
- [ ] 模型/Tool 不直接写状态；
- [ ] 红旗回归通过；
- [ ] 旧 CDP 可只读映射；
- [ ] v1/v2 API 共存；
- [ ] 临床和技术事件可关联；
- [ ] 第一阶段数据库 Migration 可回滚。

---

# Phase C：最小 Agentic Workflow 与 Durable Resume

## C0 运行场景

```text
患者初始输入
→ Safety
→ 识别 Information Gap
→ 选择一个 QuestionDecision
→ 输出问题
→ Checkpoint + Interrupt
→ Runtime 重启
→ Resume
→ 提交新 Observation
→ 再次 Safety
→ 简单 Delivery
```

## C1 Python Agent Runtime

- 一个 FastAPI App；
- LangGraph；
- explicit graph nodes；
- approved NextAction enum；
- fixed safety skeleton；
- fixed Workflow fallback adapter；
- no generic autonomous planner。

## C2 Graph Nodes v1

```text
load_runtime_context
validate_input
understand_input
commit_observations
mandatory_safety_check
evaluate_information_gap
select_question
compose_question
checkpoint
interrupt
resume
prepare_basic_delivery
complete
```

## C3 Question Policy v1

从 dialog-service 迁移：

- 必须/重要/可选缺口；
- 单问题输出；
- 避免重复；
- 不知道/跳过；
- 问题理由；
- stop decision。

## C4 ContextEnvelope v1

- 最近 6-10 轮；
- 当前 CDP 摘要；
- Critical Pin；
- token budget；
- actor/capability scope；
- context_hash；
- no full CDP by default。

## C5 PostgreSQL Checkpointer

- Thread/Run；
- Checkpoint；
- Interrupt；
- ResumeRequest；
- runtime version；
- cdp version；
- graph/state schema version。

## C6 Resume 安全

- authentication；
- authorization；
- expected checkpoint；
- expected cdp version；
- resume inbox dedup；
- expired/cancelled；
- process restart；
- node re-entry test。

## C7 Model Adapter

从 common/aidoctor_llm 适配：

- extraction route；
- question wording route；
- structured output；
- timeout；
- no provider direct calls outside Router；
- token/cost metadata。

## C8 Frontend Agent Flow

- Thread Store；
- Interrupt UI；
- Resume idempotency；
- reload recovery；
- basic delivery；
- SSE events；
- connection degraded UI。

## C9 OTel v1

- Java→Python propagation；
- node spans；
- model/tool spans；
- checkpoint spans；
- log correlation；
- AgentEvent timeline；
- one restart trace。

## C10 E2E Crash Matrix

- before commit；
- after commit before checkpoint；
- after checkpoint before response；
- after question response；
- duplicate Resume；
- stale Resume；
- concurrent Resume；
- Redis unavailable；
- model timeout。

### Phase C Exit Gate

- [ ] 第一条真正 Agentic 主链路可运行；
- [ ] 只有 NextAction 动态；
- [ ] 每轮 Safety；
- [ ] 重启可恢复；
- [ ] 重复提交不重复写；
- [ ] 旧 Workflow 可 fallback；
- [ ] 前端刷新可恢复；
- [ ] E2E crash matrix 通过。

---

# Phase D：有限临床推理、Patient History 与白名单 Evidence

## D0 运行场景

Agent 结合当前 Observation、有限患者历史和白名单指南，更新候选和信息缺口，并给出带引用的有限说明。

## D1 DiagnosticHypothesis

- candidate only；
- support/against evidence；
- must-not-miss；
- uncertainty；
- status；
- version；
- no final diagnosis without policy/review。

## D2 Diagnosis Engine Adapter

- rule/KG/LLM/statistical adapters；
- common contract；
- no fixed confidence constants；
- ensemble explanation；
- engine failure isolation；
- double-run compare。

## D3 Patient History Retrieval

- patient scope；
- consent；
- structured filter；
- reason for recall；
- no cross-patient；
- no raw all-history prompt。

## D4 Memory v1

- MemoryCandidate；
- Write Gate；
- confirmed facts only；
- expiry/correction hooks；
- recall trace；
- no automatic model inference promotion。

## D5 Medical Knowledge RAG v1

- whitelist sources；
- ingestion release；
- chunking metadata；
- pgvector；
- hybrid retrieval；
- no-result state；
- versioned source。

## D6 EvidencePack

- clinical question/PICO；
- claims；
- citations；
- applicability；
- freshness；
- conflicts；
- limitations；
- citation validation。

## D7 Context Summary

- structured summary；
- source links；
- Critical Pin；
- contradiction retention；
- summary evaluation。

## D8 Artifact/OCR

- Object Storage；
- SourceArtifact；
- checksum；
- quality gate；
- OCR Adapter；
- human confirmation；
- malicious file scan。

## D9 Frontend Evidence

- report upload；
- quality state；
- citation display；
- source view permissions；
- uncertainty display。

## D10 Evaluation

- extraction；
- hypothesis quality；
- patient isolation；
- retrieval recall；
- citation correctness；
- context faithfulness；
- injection attacks。

### Phase D Exit Gate

- [ ] 候选有支持和反对证据；
- [ ] Patient RAG 跨患者为零；
- [ ] 白名单 Evidence 可追溯；
- [ ] Citation 支持对应 Claim；
- [ ] 无证据和冲突可明确表达；
- [ ] Critical Context 不因摘要丢失；
- [ ] OCR 不自动成为确认事实。

---

# Phase E：医生审核、Delivery 与业务闭环

## E0 运行场景

高风险或不确定 Encounter 进入医生审核；医生可修改并批准，系统生成患者、医生和系统三类交付，创建随访和模拟外部动作。

## E1 ReviewTask

- create；
- assign；
- priority；
- reason codes；
- expected versions；
- approve/edit/reject/request more info/escalate；
- SLA；
- audit。

## E2 Clinician Interrupt/Resume

- checkpoint link；
- reviewer auth；
- version conflict；
- state commit；
- resume；
- stale decision；
- concurrent clinicians。

## E3 Clinician Console

- queue；
- source facts；
- hypotheses；
- evidence；
- triage；
- Agent timeline；
- edit and decision；
- audit trail。

## E4 DeliveryPackage

```text
PatientDelivery
ClinicianDelivery
SystemDelivery
```

保证：

- 同一事实基础；
- 不同披露范围；
- 版本一致；
- 限制说明；
- 不暴露 CoT/Prompt。

## E5 Care Navigation

- recommended care level；
- urgency；
- location capability；
- unavailable location fallback；
- emergency guidance；
- no false availability claim。

## E6 Follow-up

- plan；
- task；
- reminder；
- patient response；
- deterioration escalation；
- cancel/expire。

## E7 Outbox/Inbox

- State/Review/Follow-up events；
- transactional outbox；
- consumer inbox；
- retry；
- poison message；
- reconciliation。

## E8 External Action Simulation

- notification or simulated appointment；
- ExternalActionRecord；
- idempotency；
- timeout=unknown；
- status lookup；
- compensation hook；
- replay disabled。

## E9 Frontend Business Closure

- PatientDelivery；
- wait review；
- review changes；
- follow-up；
- status and retry；
- support correlation ID。

### Phase E Exit Gate

- [ ] 医生修改进入真实 CDP；
- [ ] 高风险不能绕过医生；
- [ ] 三类 Delivery 一致；
- [ ] Outbox/Inbox 不丢事件；
- [ ] 重复请求不重复动作；
- [ ] 前端患者/医生闭环；
- [ ] Audit 可追踪受保护操作。

---

# Phase F：治理、生产硬化、评估、放量与下线

## F1 Tool/Skill/Model Governance

- Tool Registry；
- Skill Registry；
- Prompt Registry；
- Model Registry/Router；
- release workflow；
- owner/reviewer；
- compatibility matrix；
- emergency disable；
- cost/latency policy。

## F2 Durable Hardening

- Thread Lease；
- worker heartbeat；
- takeover；
- Checkpoint Migration；
- legacy runtime；
- fixed Workflow fallback；
- cancellation/expiry；
- reconciliation；
- compensation。

## F3 Observability Stack

- OTel Collector；
- Tempo/Jaeger；
- Prometheus；
- Loki/OpenSearch；
- Grafana；
- PHI filter；
- SLO；
- on-call alert；
- AgentEvent/Decision/Audit separated views。

## F4 Evaluation Platform

- versioned case sets；
- patient simulator；
- OSCE；
- clinical metrics；
- safety metrics；
- RAG/context/memory/resume/security metrics；
- release report；
- regression blocking。

## F5 Replay

- State Resume；
- Simulation Replay；
- Forensic Replay；
- no real action；
- version compare；
- access control；
- evidence snapshot。

## F6 Platform Hardening

- secret manager；
- SBOM/signing；
- resource limits；
- autoscaling；
- rate limiting；
- backup/PITR；
- restore drill；
- DR；
- performance/load；
- capacity planning。

## F7 Rollout

```text
Internal
→ Shadow
→ Clinician Assist
→ Restricted Patient
→ Gradual Cohort
```

每一步有：

- clinical approval；
- safety report；
- rollback；
- monitoring；
- support plan。

## F8 Legacy Decommission

按 Migration Matrix：

- 禁止新依赖；
- adapter-only；
- traffic zero；
- read-only；
- archive；
- delete PR。

重点：

- direct CDP writes；
- duplicate health/risk endpoints；
- Python step services；
- old Java Agent orchestration；
- CDP execution trace writes；
- legacy Trace technical backend；
- Nacos if unused；
- old database writes。

### Phase F Exit Gate

- [ ] Capability Release Report 完整；
- [ ] PHI telemetry violations 为零；
- [ ] high-risk silent fallback 为零；
- [ ] replay 无真实副作用；
- [ ] backup/restore drill；
- [ ] SLO 和 on-call；
- [ ] 生产灰度通过；
- [ ] 旧服务按门禁下线；
- [ ] 第一 Capability 进入受控生产。

---

## 4. 跨阶段不可遗漏项

每个 Phase 都检查：

### Contracts

- schema version；
- compatibility；
- Java/Python/TS parity。

### Security

- authentication；
- authorization；
- Consent；
- tenant/patient isolation；
- PHI；
- injection；
- secrets。

### Data

- source；
- version；
- migration；
- retention；
- backup；
- audit。

### Durable

- idempotency；
- retry；
- resume；
- concurrency；
- cancellation；
- reconciliation。

### Observability

- trace；
- metric；
- log；
- AgentEvent；
- ClinicalDecisionRecord；
- Audit。

### Evaluation

- normal；
- red flag；
- missing data；
- conflict；
- attack；
- failure；
- restart；
- regression。

### Release

- feature flag；
- migration dry-run；
- rollback；
- dashboard；
- runbook；
- communication。

---

## 5. Task 状态和证据

每个任务记录：

```text
Task ID
Owner
Status
Dependencies
Target release
PR
Test evidence
Eval report
Migration evidence
Rollback evidence
Open risks
```

任务只有在证据链接完整时才能标记 Done。

---

## 6. Roadmap 修改规则

可以调整：

- 同一 Phase 内任务顺序；
- 具体库；
- 类和包；
- 排期和 Owner；
- 指标阈值。

必须 ADR：

- 移除 Phase 的安全/状态/Durable 能力；
- 让 Agent 绕过固定骨架；
- 改状态所有权；
- 改数据库目标；
- 提前开放高风险能力；
- 删除医生审核；
- 删除固定 Workflow fallback。

---

## 7. 实施完成定义

“照路线做完”的含义是：

- 第一呼吸道 Capability 在受控生产运行；
- 核心旧资产完成迁移或明确归档；
- 新旧数据完成对账；
- 患者与医生闭环；
- 服务重启可恢复；
- 外部动作幂等；
- 证据可引用；
- Safety 不可绕过；
- Trace/Decision/Audit 分责；
- 评估阻止回归；
- CI/CD、备份和回滚有效；
- 无必要的旧生产写路径已下线。

后续增加其他疾病和人群属于新增 Capability，不是本次重构遗漏。
