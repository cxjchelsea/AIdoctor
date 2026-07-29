# AIdoctor 架构冻结基线

> 文档状态：Draft v2.6 Freeze Candidate  
> 更新时间：2026-07-29  
> 适用分支：`agent/enterprise-agent-refactoring-plan`

---

## 1. 目的

本文明确：

1. 哪些架构决策作为长期稳定基线；
2. 哪些实施内容由路线、矩阵和详细设计持续维护；
3. 哪些事项仍需真实代码、数据和运行验证；
4. 文档冲突如何处理；
5. 最终进入 Frozen Baseline 的门禁。

冻结不是永远禁止修改，而是：普通实现变化进入 Implementation Roadmap、Migration Matrix 或 ADR；只有系统定位、状态所有权、安全原则和核心依赖方向实质变化时，才修改总体蓝图。

---

## 2. 冻结对象

### 2.1 系统形态

```text
Constrained Agentic Workflow
= 确定性的安全与生命周期 Workflow
+ 在批准范围内动态选择下一动作的 Agent
```

不调整为完全自由自治 Agent、无治理多 Agent、纯模型诊断或单一固定路径。

### 2.2 不可绕过的安全骨架

```text
Capability / Consent / Permission
→ Input Quality
→ Context Policy
→ Clinical Candidate Generation
→ State Committer
→ Mandatory Safety
→ Approved NextAction
→ Result Validation
→ State Committer
→ Checkpoint
→ Output Safety / Human Review
```

Planner、LLM、Tool、Skill、Prompt 和知识文档都不能绕过该骨架。

### 2.3 十一个一级模块

1. Business & Care Delivery
2. Clinical Domain Contracts
3. Clinical State & Data Foundation
4. Safety & Policy Engine
5. Clinical Intelligence
6. Evidence Intelligence & RAG
7. Agent Runtime
8. Context & Memory
9. Tool, Skill & Model Governance
10. Durable Execution
11. Observability, Audit & Evaluation

Capability Package、Prompt/Model Runtime 和 Knowledge Release 是现有模块内部能力，不新增新的一级模块。

### 2.4 状态所有权

- Encounter：Business；
- EncounterCDP/Evidence Ledger：Clinical State；
- 所有临床写入：State Committer；
- AgentState：Agent Runtime；
- Thread/Run/Checkpoint/Interrupt/Resume：Durable Execution；
- ContextEnvelope：Context 临时视图；
- EvidencePack：Evidence Intelligence；
- Prompt/Model/Capability/Knowledge Release：Governance；
- Trace、AgentEvent、ClinicalDecisionRecord、Audit：各自独立。

LLM、Tool、Memory、RAG、Checkpoint 和 Trace 均不是临床事实源。

### 2.5 核心依赖方向

```text
Business & Care Delivery
          ↓
Agent Runtime
          ↓
Context / Durable / Governance
          ↓
Safety / Clinical Intelligence / Evidence Intelligence
          ↓
Clinical State & Data Foundation
          ↓
Clinical Domain Contracts
```

Observability/Audit/Evaluation 横向接收事件，不反向定义临床真值。

---

## 3. 场景扩展冻结规则

```text
Stable Platform
+ Versioned Capability Package
= New Supported Clinical Scenario
```

首个生产级能力为 `adult_respiratory_v1`：成人常见呼吸道症状的风险分层、结构化信息采集、有限鉴别、白名单循证展示、医生交接和就医导航。

呼吸道是首个场景坍缩，不是永久边界。未来新场景必须新增完整 Capability Package，至少包含 Scope、Terminology、Observation、Safety、Question、Hypothesis、Knowledge、Prompt、Model Route、Tool/Skill、Delivery 和 Eval。

禁止仅更换 Prompt 或知识文档便宣称支持新疾病域。

---

## 4. RAG 与知识图谱冻结规则

### 4.1 两类 RAG 隔离

- Patient RAG：患者/租户/Consent/时间范围，结构化查询优先；
- Medical Knowledge RAG：白名单来源、版本、人群、地区、Citation 和冲突。

两者不得使用无权限隔离的同一索引或 collection。

### 4.2 Medical RAG V1 基线

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

### 4.3 Knowledge Graph

知识图谱只用于术语、多跳关系、Query Expansion 和 must-not-miss 增强；不能替代指南、证据等级、适用性和 Citation。

生产启用必须通过来源治理、版本回滚和有图/无图净收益评估；未通过时保持实验状态。

---

## 5. Prompt 与 Model Runtime 冻结规则

所有模型调用必须经过：

```text
Prompt Registry / Loader / Builder
→ Model Registry / Router / Gateway
→ Provider Adapter
→ Structured Output Validator
```

业务模块只能引用稳定 `route_id`，禁止：

- 直接调用供应商 SDK；
- 写死模型名称；
- 自动加载“最新 Prompt”或“最新模型”；
- 自行实现不一致重试和 JSON 修复；
- 模型直接写临床数据库；
- 高风险任务静默降级到未验证模型。

保持确定性的能力：State Committer、最终 Safety/Triage、权限、Consent、Checkpoint/Resume、幂等、Outbox/Inbox 和 Audit。

---

## 6. 统一生产发布单元

生产发布必须形成统一 Release Manifest：

```text
Application Version
+ Contract / Schema Version
+ Graph Runtime Version
+ Capability Release
+ Safety Policy Version
+ Prompt Releases
+ Model Route Releases
+ Knowledge Release
+ Embedding/Reranker/Graph Versions
+ Tool/Skill Releases
+ Eval Report
```

Prompt、Model Route、Knowledge 和 Capability 必须可独立禁用和回滚。历史运行永远保留当时版本快照。

---

## 7. Java 与 Python 边界

- Java：身份、Consent、Encounter、ReviewTask、Delivery、外部业务动作和兼容 API；
- Python：唯一 Agent Runtime，以及 Safety、Clinical Intelligence、Evidence、Context、Model Runtime 等首版 packages；
- Python 首版 package 模块化，不继续按每个临床步骤拆独立微服务；
- Java/Python 通过版本化 Contracts 协作；
- 现有固定 Workflow 保留为受同一 Capability/Safety/Release 约束的 fallback。

---

## 8. 高风险边界

治疗、用药、处方修改和正式医疗动作不进入首个自动路径。恢复开发前必须具备：

- Evidence 和适用性治理；
- 医生审核；
- 权限和 Consent；
- 幂等外部动作；
- ClinicalDecisionRecord；
- 专项临床评估；
- Capability Release 审批。

---

## 9. 文档体系与优先级

### 9.1 稳定架构

- [总体架构与模块设计](./overall-architecture-and-module-design.md)
- 本冻结基线
- [v2.6 跨文档一致性补充](./v2.6-cross-document-consistency-addendum.md)

### 9.2 详细设计

- [Capability Package](./capability-package-specification.md)
- [成人呼吸道 RAG V1](./adult-respiratory-medical-rag-v1-design.md)
- [模型调用矩阵](./model-call-and-routing-matrix.md)
- [Prompt 与 Model Runtime](./prompt-and-model-runtime-design.md)

### 9.3 执行文件

- Inventory
- Migration Matrix
- Coverage Matrix
- Data/Frontend/Engineering Migration
- Implementation Roadmap
- ADR

### 9.4 冲突优先级

```text
Architecture Freeze Baseline
→ v2.6 Cross-document Addendum
→ Overall Architecture
→ Capability/RAG/Model 详细设计
→ Coverage Matrix / Roadmap
→ 四份专题长文
→ 历史文档
```

涉及状态所有权、安全骨架或核心依赖的冲突必须提交 ADR，不能静默覆盖。

---

## 10. 变更控制

### 不需要修改总体蓝图

- 类名、包名、私有方法；
- 数据库索引；
- Prompt/Model/Knowledge 具体版本；
- 阶段内部任务顺序；
- 某旧服务下线时间；
- 评估阈值调整。

这些进入路线、矩阵、Release 或 ADR。

### 必须提交 ADR

- 修改状态唯一所有者；
- 允许模型或 Tool 直接写临床状态；
- 取消 Mandatory Safety；
- 合并 Patient RAG 与 Medical RAG；
- 取消统一 Model Gateway；
- 让知识图谱替代 Citation；
- 修改 Java/Python 主边界；
- 更换生产主数据库；
- 取消固定 Workflow fallback；
- 取消医生对高风险动作的控制。

### 必须修改总体蓝图

仅当批准的 ADR 改变：系统定位、11 个模块、完整主流程、状态所有权、依赖方向、首个 Capability 或核心安全原则。

---

## 11. Frozen Baseline 门禁

进入最终 Frozen Baseline 前必须满足：

### 代码与现状

- [ ] Java/Python/Frontend 真实编译和启动结果；
- [ ] 固定 Workflow 可运行或明确失败原因；
- [ ] 所有生产候选目录已盘点；
- [ ] 每个现有服务有 Migration Decision；
- [ ] Prompt、Model Call、Knowledge、Neo4j、Rule 和 Test Inventory 完成；
- [ ] 未验证资产未被直接删除。

### 契约与基础设施

- [ ] Shared Contracts v1 评审；
- [ ] Capability/Prompt/Model/Knowledge Schema 评审；
- [ ] 数据库、Runtime、RAG、Graph、框架 ADR；
- [ ] Java/Python/TypeScript 同步策略；
- [ ] CDP 迁移和回滚设计。

### 首个 Capability

- [ ] `adult_respiratory_v1` Package Skeleton；
- [ ] Scope、Safety、Question、Hypothesis 和 Eval 初版；
- [ ] 呼吸道 Source Manifest；
- [ ] 首批 PromptSpec、ModelSpec、RoutePolicy；
- [ ] 第一条纵向切片 E2E 设计。

### 工程和发布

- [ ] CI/CD；
- [ ] Prompt/Model/Knowledge/Capability Release Gate；
- [ ] OTel 和版本链字段；
- [ ] 前端/API 迁移；
- [ ] Backup、Rollback、Decommission 方案；
- [ ] Coverage Matrix 无未归属能力。

---

## 12. 当前冻结状态

当前状态：

> **Freeze Candidate：架构、扩场景、RAG、Model Runtime、迁移和发布体系已经设计完成；尚需 Phase A 真实运行、资产和数据验证后转为 Frozen Baseline。**

下一步不是继续增加总体概念，而是执行 Phase A，并把 Inventory 和 Migration Matrix 中的静态结论升级为验证后结论。