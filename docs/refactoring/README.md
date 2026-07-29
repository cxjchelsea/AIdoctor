# AIdoctor 重构方案导航

> 当前整合版本：Draft v2.5 Freeze Candidate  
> 更新时间：2026-07-29  
> 适用分支：`agent/enterprise-agent-refactoring-plan`

---

## 1. 当前状态

AIdoctor 的目标架构已经完成总体设计，并进入冻结前收口阶段。

```text
v2.4
完成总体架构、主流程、11个模块、共享契约和Phase A-F

v2.5
补充代码结构评估、覆盖检查、迁移方案、工程发布和可执行路线
```

当前状态：

> **Freeze Candidate：总体目标完整，正在通过真实代码、数据和运行基线验证迁移结论。**

---

## 2. 开发入口阅读顺序

### 第一步：理解最终系统

1. [总体架构与模块设计](./overall-architecture-and-module-design.md)
   - 当前是 Workflow 还是 Agent；
   - 完整主流程；
   - 十一个一级模块；
   - 共享数据契约；
   - 状态所有权；
   - 依赖方向；
   - 第一条纵向切片。

### 第二步：理解冻结规则

2. [架构冻结基线](./architecture-freeze-baseline.md)
   - 哪些决策冻结；
   - 哪些细节允许更新；
   - 哪些变化需要 ADR；
   - 总体架构进入 Frozen Baseline 的门禁。

### 第三步：理解当前代码

3. [当前系统资产盘点](./current-system-inventory.md)
   - 当前 Java/Python/Frontend/Storage 真实结构；
   - 代码抽样发现；
   - CDP、Workflow、Tool、Trace 和基础设施现状；
   - 需要进一步编译、运行和数据验证的内容。

4. [代码与资产迁移矩阵](./migration-matrix.md)
   - KEEP / ADAPT / WRAP / REWRITE / ARCHIVE / REMOVE；
   - 每个现有服务的目标归属；
   - 下线门禁；
   - 迁移顺序。

### 第四步：确认没有遗漏

5. [目标能力覆盖矩阵](./coverage-matrix.md)
   - 每项能力的 Owner Module；
   - 所属 Phase；
   - 核心 Contract；
   - 实现产物；
   - 测试和完成标准。

### 第五步：按执行路线开发

6. [可执行实施路线](./implementation-roadmap.md)
   - Phase A：现状基线与架构冻结；
   - Phase B：临床状态和 Safety；
   - Phase C：Agent Runtime 和 Resume；
   - Phase D：推理、历史和循证；
   - Phase E：医生审核和业务闭环；
   - Phase F：治理、生产放量和旧系统下线。

### 第六步：查看专项迁移

7. [数据与基础设施迁移](./data-and-infrastructure-migration.md)
   - MySQL/Oracle/PostgreSQL；
   - Redis、Neo4j、Milvus、Nacos；
   - Object Storage；
   - OTel；
   - CDP 数据迁移和对账。

8. [前端与业务迁移](./frontend-and-business-migration.md)
   - v1/v2 API；
   - 患者端；
   - 医生端；
   - 管理端；
   - Interrupt/Resume；
   - Trace 页面迁移。

9. [工程、发布、回滚与下线](./engineering-release-and-decommission-plan.md)
   - CI/CD；
   - 测试；
   - 数据库 Migration；
   - 发布和灰度；
   - 备份恢复；
   - 回滚；
   - 老服务下线。

---

## 3. 专题架构方案

以下四份文档定义最终系统不同关注面的完整边界。日常开发不从这里选择实施顺序，而是在总体蓝图和 Implementation Roadmap 中执行，需要深入设计时再查阅。

1. [企业级临床 Agent 重构主方案](./enterprise-agent-refactoring-plan.md)
   - 临床主流程；
   - Safety Loop；
   - Diagnostic Loop；
   - 医生接管；
   - Delivery、Care Navigation 和 Follow-up。

2. [临床数据与循证智能扩展](./clinical-data-and-evidence-intelligence-extension.md)
   - Encounter 与长期患者记录；
   - Observation、SourceArtifact 和 Evidence Ledger；
   - Evidence Intelligence；
   - PICO、引用、适用性和冲突；
   - 研究数据治理。

3. [Agent Runtime Foundations 扩展](./agent-runtime-foundations-extension.md)
   - Context Assembly；
   - Memory；
   - Patient/Medical RAG；
   - Skill Registry；
   - Model Router；
   - Runtime Security；
   - Agent Evaluation。

4. [Durable Execution 与可观测性扩展](./durable-execution-and-observability-extension.md)
   - Thread、Run、Checkpoint、Interrupt、Resume；
   - 并发、Outbox/Inbox 和幂等；
   - Checkpoint Migration 和 Reconciliation；
   - OpenTelemetry；
   - Trace、Log、AgentEvent、ClinicalDecisionRecord 和 Audit。

---

## 4. 系统形态

### 当前实现

```text
固定诊断 Workflow
+
实验性 Agent / Tool 骨架
```

### 重构目标

```text
确定性的安全和生命周期 Workflow
+
受约束的 Agent 下一动作决策
```

也就是：

> **Constrained Agentic Workflow**

只有下一动作在批准范围内动态选择。Capability、输入质量、State Committer、Mandatory Safety、医生审核、Checkpoint、幂等和输出安全不可绕过。

---

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
| 9 | Tool, Skill & Model Governance | Tool、Skill、Prompt、Model 和发布治理 |
| 10 | Durable Execution | Thread、Checkpoint、Resume、Lease、Outbox 和幂等 |
| 11 | Observability, Audit & Evaluation | OTel、Log、AgentEvent、Decision、Audit、Replay 和 Eval |

这些模块不是十一个必须独立部署的微服务。

---

## 6. 完整主流程

```text
创建 Encounter / Thread
→ 加载 Capability、Consent 和权限
→ 接收患者输入或资料
→ 输入质量和范围检查
→ Context Assembly
→ Clinical Understanding
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

---

## 7. 当前代码迁移总判断

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
| common/aidoctor_llm | 迁为 Model Provider Adapter |

当前没有核心服务被直接标记为立即删除。

---

## 8. Phase A-F 一览

### Phase A：现状基线与冻结

- 编译、启动和测试基线；
- 全量资产盘点；
- Contracts v1；
- 数据库/Runtime ADR；
- CI 和开发环境；
- 固定 Workflow 基线；
- 冻结评审。

### Phase B：临床状态与 Safety

- EncounterCDP；
- ClinicalObservation；
- SourceArtifact；
- StatePatch/CommitResult；
- State Committer；
- Legacy CDP Adapter；
- 呼吸道红旗和分诊；
- Business API v2 基础。

### Phase C：Agent Runtime 与 Resume

- Python FastAPI + LangGraph；
- InformationGap/QuestionDecision；
- ContextEnvelope；
- PostgreSQL Checkpointer；
- Interrupt/Resume；
- 重复提交去重；
- 前端跨轮和刷新恢复；
- OTel v1。

### Phase D：推理、历史和循证

- DiagnosticHypothesis；
- Diagnosis Engine Adapter；
- Patient History Retrieval；
- Memory Write Gate；
- 白名单 RAG；
- EvidencePack/Citation；
- Summary/Critical Pin；
- OCR/Artifact。

### Phase E：医生审核与业务闭环

- ReviewTask；
- Clinician Resume；
- 医生端；
- 三类 Delivery；
- Care Navigation；
- Follow-up；
- Outbox/Inbox；
- 模拟外部动作幂等。

### Phase F：治理、生产放量和下线

- Tool/Skill/Model Governance；
- Thread Lease、Migration、Reconciliation；
- OTel Stack；
- Eval/Replay；
- Secret、Backup、DR、Performance；
- Shadow/Clinician Assist/Restricted Rollout；
- 旧服务归档和下线。

---

## 9. 开工规则

当前不要直接从 Phase B 写业务代码。

先执行 Phase A：

1. 真实编译和启动；
2. 补全类、API、表、Prompt 和规则级 Inventory；
3. 将 Migration Matrix 初步结论升级为验证后结论；
4. 完成 Contracts v1；
5. 批准数据库和 Runtime ADR；
6. 冻结第一条纵向切片；
7. 总体架构进入 Frozen Baseline。

---

## 10. 防止遗漏的方法

任何新增需求必须进入 [Coverage Matrix](./coverage-matrix.md)，并填写：

```text
Owner Module
Phase
Contract
Implementation
Test
Completion Gate
```

任何现有代码变化必须更新 [Migration Matrix](./migration-matrix.md)。

任何偏离总体架构的决定必须提交 ADR。

因此后续发现实现细节变化时，主要更新矩阵、路线和 ADR，不反复推翻总体蓝图。

---

## 11. 当前推荐下一步

立即开始 Implementation Roadmap 的 Phase A：

```text
A1 代码运行基线
→ A2 全量资产盘点
→ A3 Shared Contracts v1
→ A4 架构 ADR
→ A5 CI/开发环境
→ A6 固定 Workflow 基线
→ A7 前端业务基线
→ A8 Freeze Review
```

完成 A8 后再进入正式模块实现。
