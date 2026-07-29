# AIdoctor 架构冻结基线

> 文档状态：Draft v2.5 Freeze Candidate  
> 更新时间：2026-07-29  
> 适用分支：`agent/enterprise-agent-refactoring-plan`  
> 关联总蓝图：[总体架构与模块设计](./overall-architecture-and-module-design.md)

---

## 1. 文档目的

本文档不重新设计 AIdoctor，而是对 v2.4 总体蓝图进行冻结前收口，明确：

1. 哪些架构决策作为长期稳定基线；
2. 哪些实施内容由独立路线和迁移文档持续维护；
3. 哪些事项尚需通过代码、数据和运行验证后才能关闭；
4. 实施过程中发现变化时如何记录，而不是反复改写总体蓝图。

冻结后的含义不是“永远不允许修改”，而是：

> 总体架构只在系统边界、状态所有权、安全原则或核心依赖方向发生实质变化时修改；普通实现调整进入 Implementation Roadmap、Migration Matrix 或 ADR。

---

## 2. 冻结对象

以下内容在 v2.5 基线中冻结。

### 2.1 系统形态

AIdoctor 的目标形态为：

> **Constrained Agentic Workflow：确定性的安全与生命周期 Workflow + 在批准范围内动态选择下一动作的 Agent。**

不调整为：

- 完全固定的单一路径 Workflow；
- 自由自治、可任意规划和调用工具的 Agent；
- 多个 Agent 无治理地自由讨论；
- 以模型输出替代临床状态和规则。

### 2.2 固定安全骨架

以下步骤不得被 Planner、模型、Skill 或 Tool 绕过：

```text
Capability / Consent / Permission Check
→ Input Quality Check
→ Context Policy
→ Clinical Candidate Generation
→ State Committer
→ Mandatory Safety Check
→ Approved NextAction
→ Action Result Validation
→ State Committer
→ Checkpoint
→ Output Safety / Human Review
```

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

这些是职责和领域边界，不等同于十一个微服务。

### 2.4 状态所有权

以下原则冻结：

- Encounter 由 Business & Care Delivery 管理；
- EncounterCDP 与 Evidence Ledger 由 Clinical State & Data Foundation 管理；
- 所有临床写入必须经过 State Committer；
- Safety、Clinical Intelligence、Tool 和模型只产生 Candidate、Decision 或 StatePatch；
- Agent Runtime 拥有 AgentState，不拥有临床真值；
- Durable Execution 拥有 Thread、Run、Checkpoint、Interrupt 和 Resume 状态；
- ContextEnvelope 是临时视图，不是事实源；
- Trace、Log、AgentEvent、ClinicalDecisionRecord、Audit 各自独立；
- Checkpoint、Trace 和 Log 均不得替代 EncounterCDP。

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

Observability、Audit 与 Evaluation 横向接收事件，不反向定义临床真值。

### 2.6 首个 Capability

首个生产级能力冻结为：

> 成人常见呼吸道症状的风险分层、结构化信息采集、有限鉴别分析、白名单循证依据展示、医生交接和就医导航。

首个版本不扩展为通用医学自主诊断。

---

## 3. 冻结前收口文档

v2.5 通过以下文档完成冻结前五项补充和代码结构评估：

| 文档 | 解决的问题 |
|---|---|
| [Current System Inventory](./current-system-inventory.md) | 当前仓库实际上有什么 |
| [Migration Matrix](./migration-matrix.md) | 每项资产保留、改造、包装、重写、归档还是删除 |
| [Coverage Matrix](./coverage-matrix.md) | 每项目标能力是否进入模块、阶段、契约和测试 |
| [Data & Infrastructure Migration](./data-and-infrastructure-migration.md) | MySQL/Oracle/PostgreSQL、Redis、Neo4j、Milvus、Nacos 和遥测如何演进 |
| [Frontend & Business Migration](./frontend-and-business-migration.md) | 前端、API、实时通信、医生审核和业务闭环如何切换 |
| [Engineering, Release & Decommission](./engineering-release-and-decommission-plan.md) | 测试、CI/CD、备份、部署、回滚、灰度和旧服务下线 |
| [Implementation Roadmap](./implementation-roadmap.md) | 实际执行顺序、任务、依赖和阶段门禁 |

---

## 4. 冻结前必须关闭的决策

### 4.1 数据库目标

必须通过 ADR 确认：

- 临床生产主库最终使用 PostgreSQL，还是阶段性保留 MySQL/Oracle；
- Checkpoint 是否与临床业务表同实例不同 Schema；
- 旧 CDP CLOB JSON 如何迁移为新契约；
- 双写和回滚窗口如何安排。

在 ADR 完成前，禁止直接删除 MySQL/Oracle 适配代码。

### 4.2 Java 与 Python 边界

冻结方向：

- Java 保留业务入口、身份、Consent、Encounter、ReviewTask、Delivery 和外部业务动作；
- Python 成为唯一 Agent Runtime；
- Python 内部首版以 package 模块化，不继续为每个临床步骤增加独立微服务；
- Java 与 Python 通过版本化契约协作。

需要在 Phase A 验证现有 Java CDP 和 API 哪些可直接适配。

### 4.3 固定 Workflow 降级路径

现有固定 Workflow 不立即删除。

必须先定义：

- 能够覆盖哪些安全场景；
- 与新合同的适配方式；
- 双跑对比指标；
- 何时只作为 fallback；
- 何时可以归档。

### 4.4 高风险能力

治疗、用药、处方修改和正式医疗动作不进入首个自动路径。

这些能力只有在以下条件满足后才能恢复开发：

- 证据与适用性治理；
- 医生审核；
- 权限和 Consent；
- 幂等外部动作；
- ClinicalDecisionRecord；
- 专项临床评估。

### 4.5 自定义 Trace 的处理

冻结方向：

- 技术 Trace 采用 OpenTelemetry；
- 现有 execution trace 中可复用的 AgentEvent、时间线和可视化概念保留；
- 不使用自定义 Trace 恢复 Graph；
- 不将完整患者数据写入普通遥测后端。

---

## 5. 变更控制规则

### 5.1 不需要修改总体蓝图的变化

以下变化更新对应实施文档即可：

- 类名、包名和文件路径；
- 内部函数拆分；
- 数据库索引；
- 某个 Prompt 或模型版本；
- 某阶段任务顺序微调；
- 某个旧服务下线时间；
- 某个库的替换；
- 评估阈值的调整。

### 5.2 必须提交 ADR 的变化

以下变化必须先提交 ADR：

- 修改状态唯一所有者；
- 允许 LLM 或 Tool 直接写临床状态；
- 取消 Mandatory Safety Check；
- 将 Checkpoint、Trace 或 Memory 作为临床事实源；
- 新增生产级自治 Agent；
- 修改 Java/Python 主职责边界；
- 更换生产临床主数据库；
- 取消医生对高风险动作的最终控制；
- 将 Patient RAG 与 Medical Knowledge RAG 合并；
- 取消固定 Workflow 降级路径。

### 5.3 必须修改总体蓝图的变化

只有当 ADR 被批准且影响以下内容时，才修改总体蓝图：

- 系统定位；
- 十一个一级模块；
- 完整主流程；
- 状态所有权；
- 依赖方向；
- 首个 Capability 边界；
- 安全与治理原则。

---

## 6. 冻结门禁

总体架构进入 `Frozen Baseline` 前，必须满足：

- [ ] Current System Inventory 覆盖所有生产候选目录；
- [ ] 每个现有服务有 Migration Decision；
- [ ] 每项目标能力在 Coverage Matrix 中有唯一归属；
- [ ] 数据库目标 ADR 完成；
- [ ] Java/Python 契约同步策略完成；
- [ ] 第一批核心契约字段和枚举完成评审；
- [ ] 现有固定 Workflow 能运行或明确无法运行原因；
- [ ] 首条纵向切片有 E2E 测试设计；
- [ ] CI/CD、回滚、备份和下线方案完成；
- [ ] 前端/API 迁移方案完成；
- [ ] 所有 `REWRITE` 与 `REMOVE` 决策具有验证依据；
- [ ] 未验证资产没有被直接删除。

---

## 7. 冻结后的执行规则

冻结后按照以下文档关系执行：

```text
总体架构与模块设计
稳定的目标系统定义

Architecture Freeze Baseline
稳定决策和变更控制

Implementation Roadmap
可持续更新的阶段和任务

Current System Inventory / Migration Matrix
持续更新的现状与迁移状态

Coverage Matrix
防止遗漏的闭环检查

ADR
记录必须偏离基线的原因和取舍
```

---

## 8. 当前冻结状态

当前状态为：

> **Freeze Candidate：目标架构已完整，代码迁移和基础设施决策正在收口，尚未进入最终 Frozen Baseline。**

进入最终冻结前的主要剩余工作是：

1. 执行真实编译、启动和测试基线；
2. 完成类、接口、数据表、Prompt 和规则级资产盘点；
3. 确认数据库迁移 ADR；
4. 确认第一批 JSON Schema；
5. 将 Migration Matrix 中的初步结论升级为验证后结论。
