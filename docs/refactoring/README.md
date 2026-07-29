# AIdoctor 重构方案导航

> 当前整合版本：Draft v2.6 Freeze Candidate  
> 更新时间：2026-07-29  
> 适用分支：`agent/enterprise-agent-refactoring-plan`

---

## 1. 当前状态

```text
v2.4：总体架构、主流程、11个模块、共享契约、Phase A-F
v2.5：现状盘点、迁移矩阵、生产工程、冻结流程
v2.6：Capability 扩场景、呼吸道 RAG V1、统一 Prompt/Model Runtime、跨文档一致性
```

当前仍为 **Freeze Candidate**。目标架构和执行体系已完整，最终 Frozen Baseline 还需要 Phase A 的真实编译、启动、数据库、资产和 E2E 验证。

---

## 2. 开发入口阅读顺序

### 第一步：理解稳定架构

1. [总体架构与模块设计](./overall-architecture-and-module-design.md)  
   系统形态、主流程、11 个模块、共享契约、状态所有权和部署边界。

2. [架构冻结基线](./architecture-freeze-baseline.md)  
   冻结内容、ADR 规则、冻结门禁和文档优先级。

3. [v2.6 跨文档一致性补充](./v2.6-cross-document-consistency-addendum.md)  
   对总体蓝图和四份专题长文统一补充 Capability、RAG、知识图谱、Prompt/Model Runtime 和版本链。

### 第二步：理解如何扩展临床场景

4. [Capability Package 规范](./capability-package-specification.md)  
   定义“稳定平台 + 版本化 Capability Package = 新临床场景”。

5. [成人呼吸道 Medical RAG V1](./adult-respiratory-medical-rag-v1-design.md)  
   首个场景的知识内容、来源、摄取、Chunk、索引、检索、Citation、知识图谱和评估。

6. [模型调用与路由矩阵](./model-call-and-routing-matrix.md)  
   哪些任务使用模型、哪些保持确定性、Route、Fallback 和评估。

7. [Prompt 与 Model Runtime](./prompt-and-model-runtime-design.md)  
   Prompt Registry/Loader/Builder、Model Registry/Router/Gateway、Provider Adapter 和 Validator。

### 第三步：理解当前代码和迁移方式

8. [当前系统资产盘点](./current-system-inventory.md)  
   Java/Python/Frontend/Storage，以及 Capability、Prompt、Model Call、Knowledge 和 Neo4j 专项 Inventory。

9. [代码与资产迁移矩阵](./migration-matrix.md)  
   KEEP、ADAPT、WRAP、REWRITE、ARCHIVE、REMOVE、SPLIT、MERGE、EXTRACT、EVALUATE。

10. [目标能力覆盖矩阵](./coverage-matrix.md)  
    每项能力的 Owner、Phase、Contract、实现、测试和完成门禁。

### 第四步：照路线实施

11. [可执行实施路线](./implementation-roadmap.md)  
    Phase A-F 的代码、数据、前端、RAG、Model Runtime、评估、发布和下线任务。

12. [数据与基础设施迁移](./data-and-infrastructure-migration.md)  
    PostgreSQL、旧 CDP、Checkpoint、Capability/Prompt/Model/Knowledge Schema、pgvector、BM25、Neo4j、OTel 和回滚。

13. [前端与业务迁移](./frontend-and-business-migration.md)  
    v1/v2 API、患者端、医生端、管理端、Evidence、Release 管理和实时通信。

14. [工程、发布、回滚与下线](./engineering-release-and-decommission-plan.md)  
    CI、测试、Prompt/Model/Knowledge/Capability Release Gate、灰度、回滚、备份和旧服务下线。

---

## 3. 四份专题长文

以下文档用于深入设计，不作为日常任务顺序来源：

1. [企业级临床 Agent 重构主方案](./enterprise-agent-refactoring-plan.md)  
   Clinical Intelligence、Safety Loop、Diagnostic Loop、医生接管、Delivery、Care Navigation 和 Follow-up。

2. [临床数据与循证智能扩展](./clinical-data-and-evidence-intelligence-extension.md)  
   Longitudinal Record、Encounter CDP、Evidence Ledger、Evidence Intelligence、PICO、引用和研究治理。

3. [Agent Runtime Foundations 扩展](./agent-runtime-foundations-extension.md)  
   Context、Memory、Patient/Medical RAG、Skill、Model Router、Security 和 Eval。

4. [Durable Execution 与可观测性扩展](./durable-execution-and-observability-extension.md)  
   Thread、Checkpoint、Interrupt/Resume、Lease、Outbox/Inbox、Replay、OTel、Audit。

四份长文未逐字复制 v2.6 新设计；涉及 Capability、RAG、Knowledge Graph、Prompt/Model Runtime 和版本链时，以 [v2.6 跨文档一致性补充](./v2.6-cross-document-consistency-addendum.md) 及对应详细设计为准。

---

## 4. 系统定位

```text
目标系统
= 确定性的安全与生命周期 Workflow
+ 受约束的 Agent 下一动作决策
```

不可绕过：Capability/Consent/Permission、输入质量、Context Policy、State Committer、Mandatory Safety、结果验证、Checkpoint、医生审核、输出安全和幂等。

动态范围：继续提问、检索患者历史或医学证据、调用批准 Tool/Skill、请求医生、停止并交付。

---

## 5. 场景扩展方式

```text
Stable Platform
+ Versioned Capability Package
= New Supported Clinical Scenario
```

首个 Capability 为 `adult_respiratory_v1`。它用于验证通用平台，不是永久限制到呼吸道。

新增场景必须增加：Scope、Terminology、Observation、Safety、Question、Hypothesis、Knowledge、Prompt、Model Route、Tool/Skill、Delivery 和 Eval，不允许只更换 Prompt。

---

## 6. 统一 Model Runtime

```text
Graph Node / Clinical Module
→ Context Assembly
→ Prompt Loader / Builder
→ Model Router / Gateway
→ Provider Adapter
→ Structured Output Validator
→ Candidate / Decision / Draft
```

业务模块不得直接调用模型供应商 SDK。State Committer、最终 Safety/Triage、权限、Checkpoint/Resume、幂等和 Audit 保持确定性。

---

## 7. RAG V1 基线

```text
Source Registry
+ PostgreSQL Metadata
+ Structure-aware Chunk
+ BM25
+ pgvector
+ Reranker
+ Claim-Level Citation
+ EvidencePack
+ Knowledge Release
```

Patient RAG 与 Medical RAG 必须隔离。Knowledge Graph 是术语、多跳和 Query Expansion 增强，只有在来源治理和净收益评估通过后才能进入生产。

---

## 8. 11 个一级模块

| 编号 | 模块 | 核心职责 |
|---|---|---|
| 1 | Business & Care Delivery | 身份、Encounter、Review、Delivery、Navigation、Follow-up |
| 2 | Clinical Domain Contracts | Java/Python/DB/Event/Frontend 共享语言 |
| 3 | Clinical State & Data Foundation | CDP、Observation、Ledger、State Committer、长期记录 |
| 4 | Safety & Policy Engine | 红旗、分诊、Capability、权限和不可绕过策略 |
| 5 | Clinical Intelligence | 临床理解、候选、信息缺口、问题和停止策略 |
| 6 | Evidence Intelligence & RAG | Patient History、医学证据、Citation、适用性和冲突 |
| 7 | Agent Runtime | LangGraph、Route、Interrupt、Resume、Retry、Fallback |
| 8 | Context & Memory | Context、Summary、Token、Memory Gate |
| 9 | Tool, Skill & Model Governance | Tool、Skill、Prompt、Model、Capability 发布治理 |
| 10 | Durable Execution | Thread、Checkpoint、Lease、Outbox 和幂等 |
| 11 | Observability, Audit & Evaluation | OTel、AgentEvent、Decision、Audit、Replay、Eval |

Capability、Prompt/Model Runtime 和 Knowledge Release 是现有模块内部能力，不新增第十二个一级模块。

---

## 9. 实施路线一览

### Phase A：现状与冻结

- 三端编译、启动和测试；
- 全量 Service/API/Table/Prompt/Model/Knowledge/Rule Inventory；
- Shared Contracts；
- Capability/Prompt/Model/Knowledge Schema；
- 数据库、Runtime、RAG、Graph 和框架 ADR；
- CI 和固定 Workflow 基线；
- Freeze Review。

### Phase B：临床状态和 Safety

- EncounterCDP、Observation、SourceArtifact；
- StatePatch/CommitResult、State Committer；
- Legacy CDP Adapter；
- `adult_respiratory_v1` Safety Pack；
- v2 Business API 基础。

### Phase C：Agent Runtime 和 Model Runtime

- FastAPI + LangGraph；
- ContextEnvelope、QuestionDecision；
- Prompt Loader/Builder、Model Gateway、ProviderAdapter；
- extraction/question wording routes；
- Checkpoint、Interrupt/Resume；
- OTel v1 和 Crash Matrix。

### Phase D：有限推理和 RAG

- DiagnosticHypothesis；
- Patient History/Memory；
- Source Registry、Knowledge Release；
- PostgreSQL/pgvector/BM25/Reranker；
- EvidencePack/Citation；
- Knowledge Graph 对照实验；
- OCR/Artifact。

### Phase E：医生审核和业务闭环

- ReviewTask、Clinician Resume；
- 三类 Delivery；
- Care Navigation、Follow-up；
- Outbox/Inbox、模拟外部动作；
- 医生端和患者闭环。

### Phase F：生产治理和下线

- 完整 Tool/Skill/Prompt/Model/Capability Governance；
- Durable Hardening；
- OTel Stack、Eval、Replay；
- Shadow/Canary；
- Backup/DR；
- 旧服务、旧库和旧索引下线。

---

## 10. 防遗漏规则

所有新增能力必须先进入 Coverage Matrix：

```text
Owner Module
→ Phase
→ Contract
→ Implementation
→ Test
→ Completion Gate
```

所有旧资产变化更新 Migration Matrix；所有架构偏离提交 ADR；所有生产变化绑定 Release Manifest。

---

## 11. 当前下一步

立即执行 Phase A：

```text
A1 真实运行基线
→ A2 全量资产盘点
→ A3 Shared Contracts 和 Registry Schemas
→ A4 ADR
→ A5 CI/开发环境
→ A6 固定 Workflow 基线
→ A7 adult_respiratory_v1 与首批 Prompt/Model 配置骨架
→ A8 Freeze Review
```

在 A8 前，不应直接建设完整 LangGraph、完整医学知识库或大规模知识图谱。