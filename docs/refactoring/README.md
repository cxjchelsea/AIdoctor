# AIdoctor 重构方案导航

> 当前整合版本：Draft v2.4  
> 更新时间：2026-07-29  
> 适用分支：`agent/enterprise-agent-refactoring-plan`

## 1. 文档体系与阅读顺序

AIdoctor 的重构方案现在由一份总体开发蓝图、四份专题方案和本导航组成。

### 1.1 首先阅读：总体开发蓝图

1. [AIdoctor 总体架构与模块设计](./overall-architecture-and-module-design.md)
   - 系统定位：受约束的 Agentic Workflow；
   - 一条完整正常主流程；
   - 高风险、工具失败、医生审核和中断恢复流程；
   - 十一个一级模块及其职责边界；
   - 第一批共享数据契约；
   - 状态所有权和依赖方向；
   - 物理部署建议；
   - 第一条纵向切片；
   - Phase A～F 实施路线。

这份文档是开发总入口，回答：

> 整个系统如何联结、代码如何组织、接口如何协作，以及当前应从哪里开始。

### 1.2 按需阅读：四份专题方案

2. [企业级临床 Agent 重构主方案](./enterprise-agent-refactoring-plan.md)
   - 项目定位与临床能力边界；
   - Clinical Intelligence 与 LangGraph Runtime 分工；
   - Safety Loop 与 Diagnostic Loop；
   - Evidence Ledger、工具治理和医生接管；
   - Care Navigation、Follow-up、评估与分阶段放量；
   - 原 Phase 0～6 迁移主路线。

3. [临床数据与循证智能扩展方案](./clinical-data-and-evidence-intelligence-extension.md)
   - 纵向患者记录与 Encounter CDP 分离；
   - ClinicalObservation、SourceArtifact 和 Promotion Policy；
   - 临床表型、数据集、队列和研究工作区治理；
   - Evidence Intelligence Service；
   - PICO、来源分级、引用校验和证据冲突处理；
   - 医生 Evidence Copilot。

4. [Agent Runtime Foundations 扩展方案](./agent-runtime-foundations-extension.md)
   - Context Assembly 与 Token Budget；
   - Working、Episodic、Semantic、Procedural Memory；
   - Patient RAG 与 Medical Knowledge RAG；
   - Agent Trace、Clinical Decision Record、Audit 与 Replay；
   - Skill Registry，以及对 Hermes 类 Context Files、Bounded Memory 和 Skills 思想的受控吸收；
   - Model Router、运行时安全与 Agent 专项评估。

5. [Durable Execution 与可观测性扩展方案](./durable-execution-and-observability-extension.md)
   - Thread、Run、Checkpoint、Interrupt 与 Resume 生命周期；
   - 服务重启、用户跨轮、医生审核和部署升级后的恢复；
   - Thread Lease、CDP 乐观锁、Outbox/Inbox 和幂等外部动作；
   - Checkpoint 版本迁移、Reconciliation 与 Replay Sandbox；
   - OpenTelemetry、Tempo/Jaeger、Prometheus、Loki/OpenSearch 和 Grafana；
   - Trace、Log、AgentEvent、Clinical Decision Record 和 Compliance Audit 的职责边界。

## 2. 当前系统形态

### 2.1 当前实现

当前仓库仍以固定诊断 Workflow 为主，并包含部分实验性 Agent 能力。

```text
当前实现
= 固定诊断 Workflow
+ 实验性 Agent 骨架
```

### 2.2 重构目标

```text
目标系统
= 确定性安全与生命周期 Workflow
+ 受约束的 Agent 动态决策
```

即：

> **Constrained Agentic Workflow，受约束的智能体工作流。**

固定且不可绕过：

- Capability 检查；
- 输入质量门；
- 每轮 Safety Check；
- State Committer 唯一临床写入；
- 高风险医生审核；
- 不可逆动作幂等和审计；
- 输出安全验证；
- 固定 Workflow 降级路径。

动态决策：

- 是否继续提问；
- 问什么；
- 是否检索患者历史或医学证据；
- 是否调用 Tool 或 Skill；
- 是否切换工具或降级；
- 是否需要医生介入；
- 是否停止并交付。

## 3. 十一个一级模块

| 编号 | 模块 | 核心职责 |
|---|---|---|
| 1 | Business & Care Delivery | 身份、Encounter、审核、交付、导航、随访和业务动作 |
| 2 | Clinical Domain Contracts | Java、Python、数据库和事件共享的数据语言 |
| 3 | Clinical State & Data Foundation | CDP、Evidence Ledger、长期记录和 State Committer |
| 4 | Safety & Policy Engine | 红旗、分诊、Capability、权限和不可绕过策略 |
| 5 | Clinical Intelligence | 临床理解、诊断候选、信息缺口和问题策略 |
| 6 | Evidence Intelligence & RAG | 患者历史、医学知识、引用、适用性和冲突 |
| 7 | Agent Runtime | LangGraph、节点、路由、暂停、恢复、重试和降级 |
| 8 | Context & Memory | 节点上下文、摘要、Token、记忆写入和召回 |
| 9 | Tool, Skill & Model Governance | 工具、技能、Prompt、模型和发布治理 |
| 10 | Durable Execution | Thread、Checkpoint、Resume、并发、Outbox 和幂等 |
| 11 | Observability, Audit & Evaluation | Trace、Log、AgentEvent、临床记录、审计和评估 |

这十一个模块是职责边界，不是十一个微服务。

## 4. 完整主流程

```text
创建 Encounter / Thread
→ 加载 Capability、Consent 和权限
→ 接收患者输入
→ 输入质量和范围检查
→ Context Assembly
→ Clinical Understanding
→ State Committer 写入 Observation
→ Mandatory Safety Check
→ 更新 Hypothesis、InformationGap 和 Triage
→ 选择下一动作
   ├── ask_user
   ├── request_artifact
   ├── retrieve_patient_history
   ├── retrieve_medical_evidence
   ├── call_tool / execute_skill
   ├── require_clinician_review
   └── prepare_delivery
→ 验证动作结果
→ State Committer 提交 StatePatch
→ 保存 Checkpoint
→ Interrupt 或继续
→ Resume 时校验身份、Checkpoint、CDP 版本和幂等
→ 达到停止条件
→ 生成患者版、医生版和系统版 DeliveryPackage
→ Care Navigation / Follow-up
```

每轮固定安全骨架：

```text
validate_input
→ understand_input
→ commit_observations
→ mandatory_safety_check
→ update_clinical_state
→ choose_next_action
→ execute_action
→ validate_result
→ commit_state_patch
→ checkpoint
```

## 5. 关键状态所有权

| 状态 | 所有者 |
|---|---|
| Encounter | Business & Care Delivery |
| EncounterCDP / Evidence Ledger | Clinical State & Data Foundation |
| ClinicalObservation | State Committer |
| DiagnosticHypothesis | Clinical Intelligence 提议，State Committer 提交 |
| TriageAssessment | Safety Engine 生成，State Committer 提交 |
| ContextEnvelope / MemoryItem | Context & Memory |
| EvidencePack | Evidence Intelligence |
| AgentState | Agent Runtime |
| Checkpoint / Interrupt / Resume | Durable Execution |
| ToolSpec / Skill / ModelRoutePolicy | Tool, Skill & Model Governance |
| ReviewTask | Business & Care Delivery |
| AgentEvent | Agent Event Store |
| ClinicalDecisionRecord | Clinical Decision Store |
| Compliance Audit | Audit Store |

核心规则：

```text
其他模块产生 Candidate 或 ProposedWrite
→ StatePatch
→ State Committer
→ 校验来源、权限、Consent 和 CDP 版本
→ CommitResult
```

LLM、Tool、Memory、Context 和 LangGraph 都不能直接修改临床真值。

## 6. 完整目标架构

```text
Patient UI / Clinician Console
            │
Business & Care Delivery
            │
Agent Runtime
LangGraph / Route / Interrupt / Resume
      ┌─────┼──────────┬──────────────┐
      │     │          │              │
 Safety  Clinical   Evidence       Context &
 Policy  Intelligence Intelligence Memory
      │     │          │              │
      └─────┴──────┬───┴──────────────┘
                   │
             State Committer
                   │
Clinical State & Data Foundation
CDP / Ledger / Longitudinal Record

横向：
Tool / Skill / Model Governance
Durable Execution
Observability / Audit / Evaluation

底层共享：
Clinical Domain Contracts
```

## 7. 关键边界

```text
Encounter CDP / Evidence Ledger
保存本次问诊的临床事实、候选、风险和计划

Patient Longitudinal Record
保存经过治理的跨 Encounter 长期患者状态

AgentState
保存当前执行计划、预算、失败和临时状态

Checkpoint
保存 Graph 状态、Interrupt 和恢复游标

ContextEnvelope
保存某个节点本次允许看到的临时上下文

Memory
保存明确分类、经过写入门和授权治理的信息

EvidencePack
保存可引用、可验证、可评估适用性的医学证据

Trace
保存一次技术请求的跨服务路径

Log
保存离散技术事件和错误详情

AgentEvent
保存节点、路由、重试、恢复和降级

ClinicalDecisionRecord
保存影响诊断、分诊和医生修改的临床依据

Compliance Audit
保存谁在何时访问、修改、批准或导出数据
```

Context、Summary、Trace、Log、Checkpoint 和模型输出都不是新的临床事实源。

## 8. 推荐初始物理部署

```text
Spring Boot
├── Business & Care Delivery
├── Identity / Consent
├── Review / Delivery
└── External Actions

Python FastAPI + LangGraph
├── Agent Runtime
├── Safety & Policy
├── Clinical Intelligence
├── Evidence Intelligence
├── Context & Memory
├── Tool / Skill / Model Governance
├── Durable Execution Adapter
└── Observability Instrumentation

Shared
├── Contracts
├── Capability Packages
└── Evals

Storage
├── PostgreSQL
├── Redis
├── pgvector
├── Object Storage
├── Neo4j（按需）
└── OTel Backends
```

模块不要求立即拆成独立服务。

## 9. 分阶段纵向路线

### Phase A：现状盘点与总体契约

- 修复当前 Workflow；
- 建立现有代码到 11 个模块的映射；
- 定义第一批共享 Schema；
- 建立 Java/Python 契约测试；
- 明确首个 Capability。

### Phase B：最小临床状态与安全核心

- EncounterCDP；
- ClinicalObservation；
- StatePatch / CommitResult；
- State Committer；
- Evidence Ledger；
- 呼吸道 Safety 和 Triage。

### Phase C：最小 Agentic Workflow 与跨轮恢复

- 最小 LangGraph；
- Clinical Understanding；
- Information Gap；
- QuestionDecision；
- ContextEnvelope；
- PostgreSQL Checkpoint；
- Interrupt / Resume；
- AgentEvent 和 OTel 基线。

### Phase D：临床推理、患者历史和白名单证据

- DiagnosticHypothesis；
- Patient History Retrieval；
- Memory Write Gate；
- 白名单 RAG；
- EvidencePack；
- Citation Validation；
- Context Summary 和 Critical Pin。

### Phase E：医生审核和业务交付闭环

- ReviewTask；
- 医生 Interrupt / Resume；
- 三类 DeliveryPackage；
- CarePath / FollowUpPlan；
- Outbox 和幂等外部动作。

### Phase F：治理、评估和分阶段放量

- Thread Lease；
- Checkpoint Migration；
- Reconciliation；
- Skill Registry；
- Model Router；
- 完整 OTel 和 Dashboard；
- ClinicalDecisionRecord；
- Audit；
- Replay；
- Patient Simulator；
- Shadow、Clinician Assist 和 Restricted Patient Rollout。

## 10. 第一条纵向切片

首个落地范围仍为：

> 成人常见呼吸道症状的风险分层、结构化信息采集、有限临床分析和就医导航。

第一条主链路：

```text
患者输入“咳嗽三天，有点喘”
→ 创建 Observation
→ State Committer 写入 CDP
→ 红旗检查
→ 选择一个下一问题
→ 保存 Checkpoint
→ 用户离开
→ 服务重启
→ 用户回答后 Resume
→ 更新 Observation 和 Triage
→ 生成简单就医建议
```

第一阶段完成标准：

- 临床事实有来源和版本；
- 模型推断不自动成为事实；
- 每轮执行 Safety；
- 下一问题来自结构化 QuestionDecision；
- 服务重启后可以继续；
- 重复 Resume 不重复写入；
- 所有临床写入经过 State Committer；
- Trace、Log 和 AgentEvent 可关联；
- 正常、红旗、输入不足、重复提交和恢复 E2E 测试通过。

## 11. 当前明确不做

首个版本不建设：

- 通用医学自主诊断；
- 通用无限长期记忆；
- 自由自治多 Agent；
- Agent 自主修改 Prompt、Skill、Capability 或临床知识；
- 全量医学期刊搜索引擎或任意互联网检索；
- 患者数据和公共医学知识混合向量库；
- 未授权医学全文抓取；
- 通过保存全部对话解决上下文问题；
- 保存模型私有 Chain of Thought；
- 未经审核的自我反思自动学习；
- 任意 Shell、浏览器或代码执行；
- 没有医生审核的治疗和处方修改；
- 基于 Log 或 Trace 恢复 GraphState；
- 无幂等保护地重试预约、通知、转诊和正式病历写入；
- 无版本迁移策略地让旧 Checkpoint 进入新 Graph；
- 将完整患者 Prompt 和报告写入普通遥测平台；
- 一开始拆分十一个微服务；
- 以增加 Agent、Tool、Skill 或微服务数量替代临床质量评估。

## 12. 当前开发入口

接下来的开发不再从四份专题文档任意挑选功能，而按以下顺序执行：

```text
总体方案确认
→ Clinical Domain Contracts 详细设计
→ Clinical State / State Committer 详细设计
→ Safety & Policy Engine 详细设计
→ 第一条纵向切片
→ Agent Runtime / Resume MVP
→ 按 Phase D～F 扩展
```

当前最重要的原则是：

> 先统一总体流程、模块职责、共享契约和状态所有权，再用纵向切片逐步实现，而不是按四份方案或十一个模块分别进行大而全的开发。
