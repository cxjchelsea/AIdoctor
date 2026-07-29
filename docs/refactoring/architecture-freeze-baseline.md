# AIdoctor 架构冻结基线

> 文档状态：Draft v2.6 Freeze Candidate  
> 更新时间：2026-07-29  
> 适用分支：`agent/enterprise-agent-refactoring-plan`

## 1. 目的

本文对总体架构进行冻结前收口，明确：

- 哪些决策作为长期稳定基线；
- 哪些实现内容由 Roadmap、Matrix 和详细设计维护；
- 哪些事项必须通过真实代码、数据和运行验证；
- 哪些变化必须提交 ADR 或修改总体蓝图。

冻结不代表永不修改，而是：

> 只有系统定位、一级模块、状态所有权、安全骨架、核心依赖方向或首个 Capability 边界发生实质变化时修改总体蓝图。

## 2. 冻结对象

### 2.1 系统形态

目标形态冻结为：

> **Constrained Agentic Workflow：确定性的安全与生命周期 Workflow + 在批准范围内动态选择下一动作的 Agent。**

不调整为：

- 完全固定单一路径 Workflow；
- 自由自治 Agent；
- 无治理多 Agent；
- 以模型输出替代临床状态和规则。

### 2.2 固定安全骨架

```text
Capability / Consent / Permission
→ Input Quality
→ Context Policy
→ Candidate Generation
→ State Committer
→ Mandatory Safety
→ Approved NextAction
→ Action Result Validation
→ State Committer
→ Checkpoint
→ Output Safety / Human Review
```

Planner、Prompt、模型、Skill 和 Tool 均不得绕过。

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

它们是职责边界，不等于十一个微服务。

### 2.4 状态所有权

- Encounter：Business；
- EncounterCDP/Evidence Ledger：Clinical State；
- 所有临床写入：State Committer；
- Safety/Intelligence/Tool/Model：Candidate、Decision 或 StatePatch；
- AgentState：Agent Runtime；
- Thread/Checkpoint/Interrupt/Resume：Durable Execution；
- ContextEnvelope：临时视图，不是事实源；
- EvidencePack：Evidence Intelligence；
- Prompt/Model/Tool/Skill Registry：Governance；
- Trace、AgentEvent、ClinicalDecisionRecord、Audit：分别存储；
- Checkpoint、Trace、Memory 和模型输出均不得替代 CDP。

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

Observability、Audit 和 Evaluation 横向接收事件，不反向定义临床真值。

### 2.6 场景扩展方式

扩展单位冻结为 **Capability Package**：

```text
Stable Platform
+ Versioned Capability Package
= New Supported Clinical Scenario
```

每个场景必须包含：

- Manifest；
- Population/Scope；
- Terminology；
- Observation；
- Safety；
- Question；
- Hypothesis；
- Knowledge；
- Runtime Allowlist；
- Delivery；
- Eval。

不得通过“只换 Prompt”或“只换知识库”宣称支持新临床场景。

### 2.7 首个 Capability

首个生产级 Capability 冻结为：

> 成人常见呼吸道症状的风险分层、结构化信息采集、有限鉴别、白名单循证展示、医生交接和就医导航。

首版不扩展为通用医学自主诊断，也不包含自动治疗和处方。

### 2.8 RAG 基线

冻结原则：

- Patient RAG 与 Medical Knowledge RAG 必须隔离；
- 红旗和分诊规则不依赖向量检索才能运行；
- Medical RAG 只使用批准的 Knowledge Release；
- 来源、许可、版本、地区、人群和有效期必须登记；
- 关键 Claim 必须绑定 SourceSpan；
- 无结果、冲突、人群不匹配和过期是合法状态；
- V1 基线为规则库 + PostgreSQL 元数据 + BM25 + pgvector + Reranker；
- 知识图谱是可选增强，只有通过净收益评估才进入生产；
- 知识图谱不能替代指南、推荐强度、适用性和 Citation。

### 2.9 Model Runtime 基线

所有模型调用必须经过统一 Model Runtime：

```text
Prompt Registry / Loader / Builder
Model Registry / Router / Gateway
Provider Adapter
Structured Output Validator
```

冻结规则：

- 业务模块不直接调用 Provider SDK；
- 业务模块引用 route_id，不写死模型名；
- Prompt、Model、Schema、Context 和 Capability 可追踪；
- 生产不使用未固定版本的 latest 模型或 Prompt；
- 高风险任务禁止静默降级；
- 模型不能直接写状态或执行外部动作；
- 红旗和分诊最终判断为确定性 Safety；
- Prompt/RAG/Tool 不可信内容不能改变 Policy 和 Schema。

### 2.10 Java 与 Python 边界

- Java：业务入口、身份、Consent、Encounter、Review、Delivery 和外部业务动作；
- Python：唯一 Agent Runtime；
- Python 首版 package 模块化，不为每个步骤继续增加微服务；
- Java/Python 使用版本化 Contracts；
- 固定 Workflow 暂时保留为 fallback。

## 3. 冻结前收口文档

| 文档 | 解决的问题 |
|---|---|
| [总体架构与模块设计](./overall-architecture-and-module-design.md) | 最终系统如何联结 |
| [Current System Inventory](./current-system-inventory.md) | 当前仓库实际有什么 |
| [Migration Matrix](./migration-matrix.md) | 资产保留、改造、拆分、归档或删除 |
| [Coverage Matrix](./coverage-matrix.md) | 目标能力是否有唯一归属 |
| [Capability Package 规范](./capability-package-specification.md) | 场景如何扩展 |
| [呼吸道 RAG V1](./adult-respiratory-medical-rag-v1-design.md) | 首个知识库如何搭建 |
| [模型调用矩阵](./model-call-and-routing-matrix.md) | 哪些地方用模型及失败策略 |
| [Prompt 与模型运行时](./prompt-and-model-runtime-design.md) | 统一模型和 Prompt 基础设施 |
| [Data & Infrastructure Migration](./data-and-infrastructure-migration.md) | 数据和基础设施演进 |
| [Frontend & Business Migration](./frontend-and-business-migration.md) | 前端与业务切换 |
| [Engineering/Release/Decommission](./engineering-release-and-decommission-plan.md) | 测试、发布、回滚和下线 |
| [Implementation Roadmap](./implementation-roadmap.md) | 实际执行顺序和阶段门禁 |

## 4. 冻结前必须关闭的决策

### 4.1 数据库和检索

ADR 必须确认：

- 临床主库最终目标；
- Checkpoint 部署边界；
- 旧 CDP CLOB/JSON 迁移；
- BM25 实现；
- pgvector 与 Milvus；
- Neo4j 保留和启用门禁；
- 双写和回滚窗口。

### 4.2 Runtime 与框架

ADR 必须确认：

- FastAPI + LangGraph 的版本和使用边界；
- Java/Python 通信方式；
- 单 Python Runtime 的部署方式；
- OTel 后端；
- Secret Manager；
- Provider Adapter 边界。

### 4.3 首批知识和模型发布

冻结前至少确认：

- 呼吸道白名单 Source Manifest 格式；
- 首批来源审核流程；
- Embedding/Reranker 评估方法；
- 首批 PromptSpec；
- 首批 ModelSpec/RoutePolicy；
- extraction/question wording 的评估集；
- 高风险无模型 fallback。

### 4.4 高风险能力

治疗、用药、处方修改和正式医疗动作不进入首个自动路径。恢复开发前必须具备：

- Evidence/Applicability；
- 医生审核；
- 权限和 Consent；
- 幂等动作；
- ClinicalDecisionRecord；
- 专项临床评估。

## 5. 变更控制

### 5.1 不需要修改总体蓝图

- 类名、包名和文件路径；
- 内部函数拆分；
- 数据库索引；
- Prompt/模型具体版本；
- Embedding/Reranker 选型；
- 某阶段任务顺序；
- 旧服务下线时间；
- 内部库替换；
- 评估阈值调整。

这些更新 Roadmap、Matrix、详细设计或 Registry。

### 5.2 必须提交 ADR

- 修改状态唯一所有者；
- 允许 LLM/Tool 直接写临床状态；
- 取消 Mandatory Safety；
- Checkpoint/Trace/Memory 作为临床事实源；
- 新增生产自治 Agent；
- 修改 Java/Python 主职责；
- 更换临床主数据库；
- 取消高风险医生控制；
- 合并 Patient/Medical RAG；
- 允许业务代码绕过 Model Runtime；
- 知识图谱成为唯一临床证据源；
- 取消固定 Workflow fallback。

### 5.3 必须修改总体蓝图

只有批准 ADR 影响以下内容时：

- 系统定位；
- 十一个模块；
- 主流程；
- 状态所有权；
- 依赖方向；
- Capability 扩展模型；
- 首个 Capability 边界；
- 安全和治理原则。

## 6. Frozen Baseline 门禁

- [ ] Inventory 覆盖所有生产候选目录；
- [ ] 每个现有服务有验证后的 Migration Decision；
- [ ] Coverage Matrix 每项能力有唯一归属；
- [ ] 数据库、Runtime、RAG 和框架 ADR 完成；
- [ ] Java/Python/TS Contracts v1 完成；
- [ ] Capability Package 骨架通过评审；
- [ ] 首批 PromptSpec/ModelSpec/RoutePolicy 完成；
- [ ] 呼吸道 Source Manifest 和知识审核流程完成；
- [ ] 现有固定 Workflow 能运行或明确失败原因；
- [ ] 第一条纵向切片有 E2E 和 Crash Matrix；
- [ ] CI/CD、回滚、备份和下线方案完成；
- [ ] 所有 REWRITE/REMOVE 具有验证依据；
- [ ] 未验证资产没有被删除。

## 7. 当前状态

> **Freeze Candidate：目标架构、Capability、RAG 和 Model Runtime 设计已完整；真实运行基线、Contracts、ADR、具体知识来源和模型评估尚需在 Phase A 关闭。**
