# AIdoctor 重构方案导航

> 当前整合版本：Draft v2.6 Freeze Candidate  
> 更新时间：2026-07-29  
> 适用分支：`agent/enterprise-agent-refactoring-plan`

## 1. 当前状态

```text
v2.4
总体架构、完整主流程、11个模块、共享契约、状态所有权和Phase A-F

v2.5
代码结构评估、迁移矩阵、覆盖矩阵、数据/前端/工程迁移与冻结基线

v2.6
Capability扩展规范、成人呼吸道RAG V1、模型调用矩阵、Prompt与Model Runtime
```

当前状态：

> **Freeze Candidate：总体架构和关键详细设计已完整，仍需通过 Phase A 的真实编译、运行、数据和契约验证进入 Frozen Baseline。**

## 2. 开发入口阅读顺序

### 第一步：理解最终系统

1. [总体架构与模块设计](./overall-architecture-and-module-design.md)
   - Workflow 与 Agent 的关系；
   - 完整正常、高风险、工具失败、医生审核和恢复流程；
   - 十一个一级模块；
   - 共享数据契约；
   - 状态所有权；
   - 依赖方向；
   - 第一条纵向切片。

2. [架构冻结基线](./architecture-freeze-baseline.md)
   - 长期稳定决策；
   - 变更控制和 ADR；
   - Frozen Baseline 门禁。

### 第二步：理解场景如何扩展

3. [Capability Package 规范](./capability-package-specification.md)
   - 稳定平台与场景包边界；
   - Manifest、Terminology、Observation、Safety、Question、Hypothesis、Knowledge、Runtime 和 Eval Pack；
   - `adult_respiratory_v1` 的范围和非目标；
   - 新增消化、心血管等场景的标准步骤；
   - Capability 版本、发布和回滚。

核心扩展方式：

```text
Stable Platform
+ Versioned Capability Package
= New Supported Clinical Scenario
```

扩场景不是只换 Prompt 或知识库，而是新增完整、可评估、可回滚的 Capability Package。

### 第三步：理解知识库和模型基础设施

4. [成人呼吸道 Medical RAG V1 详细设计](./adult-respiratory-medical-rag-v1-design.md)
   - Patient RAG 与 Medical Knowledge RAG 隔离；
   - 安全分诊、信息采集、有限鉴别、检查和患者教育知识域；
   - Source Registry、Ingestion、Chunk、PostgreSQL、pgvector、BM25、Reranker；
   - EvidencePack、Citation Validation、Knowledge Release；
   - 知识图谱 Schema、适用范围和启用门禁；
   - RAG 评估和上线标准。

5. [模型调用与路由矩阵](./model-call-and-routing-matrix.md)
   - 哪些任务调用 LLM；
   - 哪些任务必须确定性实现；
   - 首个 Capability 启用哪些模型路线；
   - RoutePolicy、Model Registry、Fallback 和专项评估；
   - 高风险任务禁止静默降级。

6. [Prompt 与模型运行时设计](./prompt-and-model-runtime-design.md)
   - Model Runtime Client；
   - Prompt Registry / Loader / Builder；
   - Model Registry / Router / Gateway；
   - Provider Adapter；
   - Structured Output Validator；
   - Prompt/Model 发布、兼容、追踪和回滚；
   - `common/aidoctor_llm` 的迁移方式。

统一模型调用链：

```text
Graph Node / Domain Module
→ Context Assembly
→ Prompt Registry / Loader
→ Prompt Builder
→ Model Router
→ Model Gateway
→ Provider Adapter
→ Structured Output Validator
→ Safety / Capability Validation
→ Candidate / Decision / Draft
```

业务模块不得直接调用模型供应商 SDK。

### 第四步：理解当前代码与迁移

7. [当前系统资产盘点](./current-system-inventory.md)
   - Java、Python、Frontend、Storage 当前结构；
   - CDP、固定 Workflow、Tool、Trace 和基础设施现状；
   - 静态代码证据和仍需运行验证的内容。

8. [代码与资产迁移矩阵](./migration-matrix.md)
   - KEEP / ADAPT / WRAP / REWRITE / ARCHIVE / REMOVE / SPLIT / MERGE；
   - 现有服务的目标归属和下线门禁。

9. [目标能力覆盖矩阵](./coverage-matrix.md)
   - Owner Module；
   - Phase；
   - Contract；
   - Implementation；
   - Test；
   - Completion Gate。

### 第五步：按路线实施

10. [可执行实施路线](./implementation-roadmap.md)
    - Phase A：现状基线、契约和冻结；
    - Phase B：Clinical State 与 Safety；
    - Phase C：Agent Runtime、Model Runtime 基础和 Resume；
    - Phase D：有限推理、Patient History、呼吸道 RAG 和 Evidence；
    - Phase E：医生审核和业务闭环；
    - Phase F：治理、评估、生产放量和下线。

### 第六步：专项迁移

11. [数据与基础设施迁移](./data-and-infrastructure-migration.md)
12. [前端与业务迁移](./frontend-and-business-migration.md)
13. [工程、发布、回滚与下线](./engineering-release-and-decommission-plan.md)

## 3. 专题架构方案

以下四份长文定义完整边界，日常实施以总体蓝图、详细设计和 Implementation Roadmap 为入口。

1. [企业级临床 Agent 重构主方案](./enterprise-agent-refactoring-plan.md)
2. [临床数据与循证智能扩展](./clinical-data-and-evidence-intelligence-extension.md)
3. [Agent Runtime Foundations 扩展](./agent-runtime-foundations-extension.md)
4. [Durable Execution 与可观测性扩展](./durable-execution-and-observability-extension.md)

## 4. 系统形态

当前：

```text
固定诊断 Workflow
+ 实验性 Agent / Tool 骨架
```

目标：

```text
确定性的安全和生命周期 Workflow
+ 受约束的 Agent 下一动作决策
```

即 **Constrained Agentic Workflow**。

不可绕过：

- Capability、Consent 和权限；
- 输入质量；
- State Committer；
- Mandatory Safety；
- 高风险医生审核；
- Checkpoint、Resume 和幂等；
- 输出安全；
- 固定 Workflow Fallback。

## 5. 十一个一级模块

| 编号 | 模块 | 核心职责 |
|---|---|---|
| 1 | Business & Care Delivery | 身份、Encounter、审核、交付、导航、随访和业务动作 |
| 2 | Clinical Domain Contracts | Java、Python、数据库、事件和前端共享语言 |
| 3 | Clinical State & Data Foundation | CDP、Observation、Ledger、长期记录和 State Committer |
| 4 | Safety & Policy Engine | 红旗、分诊、Capability、权限和不可绕过策略 |
| 5 | Clinical Intelligence | 临床理解、候选、信息缺口、问题和停止策略 |
| 6 | Evidence Intelligence & RAG | 患者历史、医学证据、引用、适用性和冲突 |
| 7 | Agent Runtime | LangGraph、节点、路由、暂停、恢复、重试和降级 |
| 8 | Context & Memory | Context、摘要、Token、Memory Write/Recall |
| 9 | Tool, Skill & Model Governance | Tool、Skill、Prompt、Model Runtime 和发布治理 |
| 10 | Durable Execution | Thread、Checkpoint、Resume、Lease、Outbox 和幂等 |
| 11 | Observability, Audit & Evaluation | OTel、Log、AgentEvent、Decision、Audit、Replay 和 Eval |

这些是职责边界，不要求立即部署为十一个微服务。

## 6. 完整主流程

```text
创建 Encounter / Thread
→ 加载 Capability、Consent 和权限
→ 接收患者输入或资料
→ 输入质量和范围检查
→ Context Assembly
→ Clinical Understanding
→ Model Runtime（需要模型时）
→ State Committer 写入 Observation
→ Mandatory Safety Check
→ 更新 Hypothesis、InformationGap 和 Triage
→ 选择批准的 NextAction
→ 执行并验证动作
→ State Committer 提交 StatePatch
→ 保存 Checkpoint
→ Interrupt 或继续
→ 身份、版本和幂等校验后 Resume
→ 达到停止条件
→ Patient / Clinician / System Delivery
→ Care Navigation / Follow-up
```

## 7. 首个 Capability

首个生产级能力：

> 成人常见呼吸道症状的风险分层、结构化信息采集、有限鉴别、白名单循证展示、医生交接和就医导航。

它是场景坍缩，不是平台永久边界。

首版不包括：

- 通用所有疾病自动诊断；
- 儿童和孕产完整自动路径；
- 自动处方和治疗调整；
- 影像自动确诊；
- 自由自治多 Agent。

## 8. 知识库技术基线

```text
Structured Safety Rules
+ Source Registry / PostgreSQL Metadata
+ BM25
+ pgvector
+ Reranker
+ EvidencePack / Citation Validation
+ optional Neo4j enhancement
```

Neo4j 用于术语关系、候选扩展和多跳查询；只有通过净收益评估后进入生产，不能替代指南、推荐强度、适用性和引用。

## 9. 模型基础设施基线

统一建设 Model Runtime：

```text
Prompt Registry
Prompt Loader
Prompt Builder
Model Registry
Model Router
Model Gateway
Provider Adapter
Structured Output Validator
```

首个纵向切片优先启用：

- Observation Extraction；
- Question Wording；
- 可选 Red Flag Candidate Extraction；
- 较长对话后的 Structured Summary；
- Patient Delivery Wording。

红旗和分诊最终判断、State Commit、Checkpoint、权限、幂等和 Audit 不由 LLM 执行。

## 10. 当前代码迁移总判断

| 当前资产 | 初步方向 |
|---|---|
| diagnosis-service | 拆分 Business、Clinical State 和固定 Workflow Adapter |
| examination-service | 保留需求，按 SourceArtifact 重写 |
| health-state + risk-assessment | 合并 Safety 规则 |
| clinical-parsing | 保留抽取和归一化，改 ObservationCandidate |
| dialog-service | 拆分 Intelligence、Context、Runtime 和通信 |
| diagnosis-engine | 保留 KG/规则/模型 Adapter，统一 Hypothesis 合同 |
| workup-planner | 后续受控 Tool |
| treatment-engine | 首阶段归档，不进入自动主链路 |
| explanation-service | 拆分 Evidence、Decision 与 Presentation |
| ocr-service | 保留 Tool，增加 SourceArtifact 和质量门 |
| execution trace | UI/AgentEvent 可复用，技术 Trace 迁 OTel |
| frontend | 保留 React 资产，重写 API、状态和审核流程 |
| common/aidoctor_llm | ADAPT 为 Model Runtime Provider Adapter |

当前没有核心服务被直接标记为立即删除。

## 11. Phase A-F 一览

### Phase A：现状基线与冻结

- 编译、启动和测试基线；
- 全量资产盘点；
- Contracts v1；
- 数据库、Runtime 和框架 ADR；
- CI 和开发环境；
- 固定 Workflow 基线；
- 确认 Capability、RAG、Model Runtime 详细设计；
- Freeze Review。

### Phase B：Clinical State 与 Safety

- EncounterCDP、Observation、SourceArtifact；
- StatePatch、CommitResult、State Committer；
- Legacy CDP Adapter；
- 呼吸道红旗和分诊；
- Capability Registry 基础。

### Phase C：Agent Runtime、Model Runtime 与 Resume

- FastAPI + LangGraph；
- ContextEnvelope；
- InformationGap、QuestionDecision；
- Prompt Loader/Builder、Model Gateway；
- Extraction 与 Question Wording Route；
- PostgreSQL Checkpoint；
- Interrupt/Resume；
- OTel v1。

### Phase D：推理、历史和呼吸道 RAG

- DiagnosticHypothesis；
- Diagnosis Engine Adapter；
- Patient History 与 Memory Gate；
- Source Registry 和 Knowledge Release；
- BM25 + pgvector + Reranker；
- EvidencePack 与 Citation；
- 可选知识图谱对照实验；
- OCR/Artifact。

### Phase E：医生审核和业务闭环

- ReviewTask、Clinician Resume 和医生端；
- 三类 Delivery；
- Care Navigation、Follow-up；
- Outbox/Inbox 和幂等模拟动作。

### Phase F：治理、评估、放量和下线

- Tool/Skill/Prompt/Model 完整治理；
- Durable Hardening；
- OTel Stack；
- Eval、Replay、Shadow、Clinician Assist；
- Restricted Patient Rollout；
- 旧服务归档和下线。

## 12. 防止遗漏与变更控制

任何目标能力必须进入 [Coverage Matrix](./coverage-matrix.md)：

```text
Owner Module
→ Phase
→ Contract
→ Implementation
→ Test
→ Completion Gate
```

任何现有代码处理变化必须更新 [Migration Matrix](./migration-matrix.md)。

普通实现变化更新 Roadmap、Matrix 或详细设计；修改状态所有权、安全骨架、模块边界或 Java/Python 主职责时必须提交 ADR。

## 13. 当前推荐下一步

执行 Implementation Roadmap 的 Phase A：

```text
A1 真实代码运行基线
→ A2 全量资产盘点
→ A3 Shared Contracts v1
→ A4 数据库/Runtime/框架 ADR
→ A5 Capability与Model Runtime配置骨架
→ A6 CI和开发环境
→ A7 固定Workflow与前端基线
→ A8 Freeze Review
```

完成 Phase A 后进入正式模块实现。