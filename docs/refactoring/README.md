# AIdoctor 重构方案导航

> 当前整合版本：Draft v2.3  
> 更新时间：2026-07-29  
> 适用分支：`agent/enterprise-agent-refactoring-plan`

## 1. 阅读顺序

AIdoctor 的企业级重构方案由以下四份主题文档与一份导航共同组成：

1. [企业级临床 Agent 重构主方案](./enterprise-agent-refactoring-plan.md)
   - 项目定位与能力边界；
   - Clinical Intelligence 与 LangGraph Runtime 分工；
   - Safety Loop 与 Diagnostic Loop；
   - Evidence Ledger、工具治理和医生接管；
   - Care Navigation、Follow-up、评估与分阶段放量；
   - Phase 0～6 迁移主路线。

2. [临床数据与循证智能扩展方案](./clinical-data-and-evidence-intelligence-extension.md)
   - 纵向患者记录与 Encounter CDP 分离；
   - ClinicalObservation、SourceArtifact 和 Promotion Policy；
   - 临床表型、数据集、队列和研究工作区治理；
   - Evidence Intelligence Service；
   - PICO、来源分级、引用校验和证据冲突处理；
   - 医生 Evidence Copilot。

3. [Agent Runtime Foundations 扩展方案](./agent-runtime-foundations-extension.md)
   - Context Assembly 与 Token Budget；
   - Working、Episodic、Semantic、Procedural Memory；
   - Patient RAG 与 Medical Knowledge RAG；
   - Agent Trace、Clinical Decision Record、Audit 与 Replay；
   - Skill Registry，以及对 Hermes 类 Context Files、Bounded Memory 和 Skills 思想的受控吸收；
   - Model Router、运行时安全与 Agent 专项评估；
   - 对 Phase 0～6 的增量任务、验收标准、ADR 和实施 PR。

4. [Durable Execution 与可观测性扩展方案](./durable-execution-and-observability-extension.md)
   - Thread、Run、Checkpoint、Interrupt 与 Resume 生命周期；
   - 服务重启、用户跨轮、医生审核和部署升级后的恢复；
   - Thread Lease、CDP 乐观锁、Outbox/Inbox 和幂等外部动作；
   - Checkpoint 版本迁移、Reconciliation 与 Replay Sandbox；
   - OpenTelemetry、Tempo/Jaeger、Prometheus、Loki/OpenSearch 和 Grafana；
   - Trace、Log、AgentEvent、Clinical Decision Record 和 Compliance Audit 的职责边界；
   - 对 Phase 0～6 的 Durable Execution 和 Observability 增量任务。

## 2. v2.3 的架构定义

v2.3 保持此前核心原则：

- LangGraph 只管理执行过程，不承担临床真值判断；
- 临床推理、分诊安全、语言表达和高风险动作决策权分离；
- Safety Loop 不可被 Planner 绕过；
- 高风险和受监管动作由医生最终决定；
- 固定 Workflow 保留为可测试的降级路径；
- 首个 Capability 必须收窄并经过分阶段验证；
- 患者事实、模型推断、执行状态、上下文和记忆不能混用；
- Checkpoint 是执行恢复状态源，Trace 和 Log 不是；
- 不可逆动作必须通过幂等、版本和审核实现业务上的等效恰好一次。

v2.3 的正式平台能力包括以下四组。

### 2.1 Clinical Intelligence & Care Delivery

负责：

- 症状和医学概念理解；
- Evidence Ledger；
- 下一问题策略；
- Diagnostic Inference；
- Triage 与 Safety；
- Care Navigation；
- 医生审核；
- 患者、医生和系统三类交付；
- Follow-up。

### 2.2 Clinical Data & Evidence Foundation

负责：

- 长期患者画像与单次 Encounter 分离；
- 多来源临床事实、推断和确认状态管理；
- 临床表型定义及版本管理；
- 数据集、队列、标签和研究用途治理；
- 生产数据与研究工作区隔离；
- 临床问题和 PICO；
- 权威指南、综述和研究检索；
- Claim-Level Citation；
- 人群适配、时效性和证据冲突。

### 2.3 Agent Runtime Foundations

负责：

- 根据节点、任务、风险、权限和 token 预算装配 ContextEnvelope；
- 将 Working、Episodic、Semantic 和 Procedural Memory 分开治理；
- 隔离 Patient RAG 和 Medical Knowledge RAG；
- 注册版本化 ClinicalSkill；
- 依据风险、PHI、数据驻留、质量、延迟和成本进行 Model Routing；
- 执行 Prompt、RAG、Tool、Memory 和多租户运行时安全策略；
- 支持结构化 AgentEvent 和决策版本链。

### 2.4 Durable Execution & Observability

负责：

- 维护 Thread、Run、Checkpoint 和 Interrupt 生命周期；
- 通过 PostgreSQL Checkpointer 持久化执行状态；
- 通过 Resume 鉴权、Thread Lease 和 CDP 乐观锁安全恢复；
- 通过 Outbox/Inbox、ExternalActionRecord 和幂等键防止重复副作用；
- 对旧 Checkpoint 执行 Graph/State Schema 版本兼容和迁移；
- 区分 State Resume、Simulation Replay 和 Forensic Replay；
- 使用 OpenTelemetry 建立跨 Java、Python、LangGraph 和 Tool 的技术 Trace；
- 将 Trace、Log、AgentEvent、Clinical Decision Record 和 Audit 分开治理。

## 3. 完整目标架构

```text
Patient UI / Clinician Console
            │
Business & Care Delivery
用户、权限、预约、转诊、审核、随访
            │
Resume Auth / Thread Lease / Inbox
            │
┌───────────▼──────────────────────────────────────┐
│ Agent Runtime Foundations                       │
│ Context / Memory / RAG Runtime / Skills         │
│ Model Router / Security / Runtime Policy        │
└───────────┬──────────────────────────────────────┘
            │
LangGraph Durable Runtime
GraphState / Checkpoint / Interrupt / Resume /
Retry / Fallback / Migration
       ┌────┴─────────────────────┐
       │                          │
Clinical Intelligence            Evidence Intelligence
问诊策略、诊断、分诊、           PICO、指南/论文检索、
Safety、Care Path                来源、引用、适配和冲突
       └────────────┬─────────────┘
                    │
State Committer
                    │
Clinical Data & Research Foundation
Longitudinal Record / Encounter CDP / Evidence Ledger /
Phenotype Registry / Dataset Registry / Research Workspace
                    │
Outbox → External Action Worker → ExternalActionRecord

并行观测与记录：
OpenTelemetry Trace / Metric / Log
AgentEvent
ClinicalDecisionRecord
Compliance Audit
```

## 4. 关键数据、执行和观测边界

```text
Patient Longitudinal Record
保存经过治理的跨 Encounter 长期患者状态

Encounter CDP / Evidence Ledger
保存本次问诊的临床事实、候选、风险和计划

AgentState
保存当前执行计划、失败、预算和临时状态

Checkpoint
保存 Graph 节点、GraphState、Interrupt 和恢复游标

ContextEnvelope
保存某个节点本次允许看到的临时上下文

Memory
保存明确分类、经过写入门和授权治理的信息

EvidencePack
保存可引用、可验证、可评估适用性的医学证据

Trace
保存一次技术请求的跨服务调用链

Log
保存离散技术事件与错误详情

AgentEvent
保存节点、路由、重试、恢复和降级等结构化 Agent 决策事件

ClinicalDecisionRecord
保存影响诊断、分诊和医生修改的临床依据

Compliance Audit
保存谁在何时访问、修改、批准或导出数据
```

ContextEnvelope、摘要、Trace、Log 和模型输出都不是新的临床事实源。所有临床状态写入必须经过 State Committer，并指向可追溯来源。

Trace 和 Log 都不是恢复状态源。恢复依赖 Checkpoint、CDP 版本、幂等协议和有效的 ResumeRequest。

## 5. 推荐开源可观测栈

```text
Spring Boot / FastAPI / LangGraph / Tool Services
                    │ OTLP
                    ▼
          OpenTelemetry Collector
          ├── Trace  → Tempo 或 Jaeger
          ├── Metric → Prometheus
          └── Log    → Loki 或 OpenSearch
                         │
                         ▼
                       Grafana
```

建议：

- 开发环境可优先使用 Jaeger；
- 目标环境可使用 Tempo + Prometheus + Loki + Grafana；
- 只选择一个主 Trace 后端；
- Agent/RAG 调试可选 Phoenix；
- Phoenix、Tempo 或 Jaeger 都不能替代 Clinical Decision Record 和 Compliance Audit；
- OpenTelemetry Collector 负责批处理、重试、采样和 PHI 字段清洗；
- Trace 后端故障不能阻塞临床请求；
- Checkpoint 写入失败必须阻止不可逆动作继续执行。

## 6. 实施优先级

v2.3 不意味着立即建设完整 Agent 平台。建议顺序为：

1. 修复当前编译、接口和固定 Workflow 主链路；
2. 盘点 Prompt、Context、Memory、RAG、Trace、Log、外部动作和敏感数据流；
3. 建立 ClinicalObservation、Encounter CDP、Evidence Ledger 和 Tool Contracts；
4. 建立 Thread、Run、CheckpointMetadata、Interrupt、ResumeRequest 和 ExternalActionRecord 契约；
5. 建立首个 Capability 的 ContextEnvelope、ContextPolicy 和 Critical Context Pin；
6. 建立 PostgreSQL Checkpointer 和用户跨轮 Resume MVP；
7. 建立 Memory Write Gate 和结构化患者历史召回；
8. 建立只覆盖白名单指南的 RAG / Evidence Intelligence MVP；
9. 建立首个窄 Capability 的 Clinical Intelligence MVP；
10. 接入 LangGraph Durable Runtime、Thread Lease、Outbox/Inbox 和受控 Skill；
11. 完成医生审核恢复、幂等外部动作、三类输出、Care Navigation 与 Follow-up；
12. 接入 OpenTelemetry Collector、Trace/Metric/Log 和 AgentEvent；
13. 建立 Checkpoint Migration、Reconciliation、Replay 和运营 Dashboard；
14. 建立脱敏研究数据集、Patient Simulator、Agent 专项评估和分阶段验证。

## 7. 当前明确不做

首个版本不建设：

- 通用医学自主诊断；
- 通用无限长期记忆；
- Agent 自主修改 Prompt、Skill、Capability 或临床知识；
- 全量医学期刊搜索引擎或任意互联网检索；
- 患者数据和公共医学知识混合向量库；
- 未授权医学全文抓取；
- 通过保存全部对话解决上下文问题；
- 保存模型私有 Chain of Thought；
- 未经审核的自我反思自动学习；
- 任意 Shell、浏览器或代码执行；
- 同时接入大量模型并在生产流量中动态试错；
- 没有医生审核的治疗和处方修改；
- 为了模仿 Hermes 而引入新的通用 Agent Runtime；
- 基于 Log 或 Trace 恢复 GraphState；
- 无幂等保护地重试预约、通知、转诊和正式病历写入；
- 无版本迁移策略地让旧 Checkpoint 进入新 Graph；
- 将完整患者 Prompt 和报告写入普通遥测平台；
- 同时部署多个功能重叠的 APM 或 LLM Trace 平台；
- 以增加 Agent、Tool、Skill 或微服务数量替代临床质量评估。

## 8. 当前推荐起点

仍建议以以下 Capability 为首个落地范围：

> 成人常见呼吸道症状的风险分层、结构化信息采集、循证依据展示和就医导航。

该 Capability 第一版只需要：

- 红旗和过敏等 Critical Context Pin；
- 最近 6～10 轮原始对话与结构化摘要；
- 经确认的相关长期病史召回；
- 少量白名单指南和 Claim-Level Citation；
- `collect_respiratory_history`、`evaluate_respiratory_red_flags`、`prepare_clinician_handoff`、`follow_up_respiratory_case` 四个受控 Skill；
- 抽取、对话表达、总结和引用校验的受控模型路由；
- PostgreSQL Checkpoint；
- 用户离开和服务重启后的 Resume；
- 重复 Resume 去重；
- Thread Lease 与 CDP 乐观锁；
- 一个模拟外部动作的幂等测试；
- OTel、AgentEvent、context_hash、版本链和结构化 Log；
- Context、Memory、RAG、Injection、Resume、Replay 和临床病例回归测试。

第一阶段的目标不是让 Agent 拥有最多上下文、最长记忆、最多技能或最复杂的 Trace 平台，而是保证：

> 每个节点只看到完成任务所需且经过授权的信息；关键临床事实不会在压缩中丢失；长期记忆不会被模型推断污染；检索结果有来源和适用边界；用户和医生可以在中断后安全恢复；不可逆动作不会因重试或重放而重复；所有技术链路、Agent 决策、临床依据和合规访问都能在各自正确的记录系统中被追踪。
