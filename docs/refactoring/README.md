# AIdoctor 重构方案导航

> 当前整合版本：Draft v2.2  
> 更新时间：2026-07-29  
> 适用分支：`agent/enterprise-agent-refactoring-plan`

## 1. 阅读顺序

AIdoctor 的企业级重构方案由以下三份文档共同组成：

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
   - Technical Trace、Agent Trace、Clinical Decision Record、Audit 与 Replay；
   - Skill Registry，以及对 Hermes 类 Context Files、Bounded Memory 和 Skills 思想的受控吸收；
   - Model Router、运行时安全与 Agent 专项评估；
   - 对 Phase 0～6 的增量任务、验收标准、ADR 和实施 PR。

## 2. v2.2 的架构定义

v2.2 保持此前核心原则：

- LangGraph 只管理执行过程，不承担临床真值判断；
- 临床推理、分诊安全、语言表达和高风险动作决策权分离；
- Safety Loop 不可被 Planner 绕过；
- 高风险和受监管动作由医生最终决定；
- 固定 Workflow 保留为可测试的降级路径；
- 首个 Capability 必须收窄并经过分阶段验证；
- 患者事实、模型推断、执行状态、上下文和记忆不能混用。

v2.2 的正式平台能力包括以下三组。

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
- 将 Technical Trace、Agent Trace、Clinical Decision Record 和 Compliance Audit 分开；
- 支持 State Resume、Simulation Replay 和 Forensic Replay。

## 3. 完整目标架构

```text
Patient UI / Clinician Console
            │
Business & Care Delivery
用户、权限、预约、转诊、审核、随访
            │
┌───────────▼──────────────────────────────────────┐
│ Agent Runtime Foundations                       │
│ Context / Memory / RAG Runtime / Skills         │
│ Model Router / Security / Trace / Replay        │
└───────────┬──────────────────────────────────────┘
            │
LangGraph Agent Runtime
GraphState / Checkpoint / Interrupt / Retry / Fallback
       ┌────┴─────────────────────┐
       │                          │
Clinical Intelligence            Evidence Intelligence
问诊策略、诊断、分诊、           PICO、指南/论文检索、
Safety、Care Path                来源、引用、适配和冲突
       └────────────┬─────────────┘
                    │
Clinical Data & Research Foundation
Longitudinal Record / Encounter CDP / Evidence Ledger /
Phenotype Registry / Dataset Registry / Research Workspace
```

## 4. 关键数据和运行时边界

```text
Patient Longitudinal Record
保存经过治理的跨 Encounter 长期患者状态

Encounter CDP / Evidence Ledger
保存本次问诊的临床事实、候选、风险和计划

AgentState / Checkpoint
保存执行进度、计划、失败、预算和恢复点

ContextEnvelope
保存某个节点本次允许看到的临时上下文

Memory
保存明确分类、经过写入门和授权治理的信息

EvidencePack
保存可引用、可验证、可评估适用性的医学证据

Agent Trace / Clinical Decision Record / Audit
分别服务运行分析、临床解释和合规追责
```

ContextEnvelope、摘要、Trace 和模型输出都不是新的临床事实源。所有临床状态写入必须经过 State Committer，并指向可追溯来源。

## 5. 实施优先级

v2.2 不意味着立即建设完整 Agent 平台。建议顺序为：

1. 修复当前编译、接口和固定 Workflow 主链路；
2. 盘点 Prompt、Context、Memory、RAG、Trace 和敏感数据流；
3. 建立 ClinicalObservation、Encounter CDP、Evidence Ledger 和 Tool Contracts；
4. 建立首个 Capability 的 ContextEnvelope、ContextPolicy 和 Critical Context Pin；
5. 建立 Memory Write Gate 和结构化患者历史召回；
6. 建立只覆盖白名单指南的 RAG / Evidence Intelligence MVP；
7. 建立首个窄 Capability 的 Clinical Intelligence MVP；
8. 接入 LangGraph Runtime、AgentEvent、Model Router 和受控 Skill；
9. 完成医生审核、三类输出、Care Navigation、Follow-up 与 Replay；
10. 建立脱敏研究数据集、Patient Simulator、Agent 专项评估和分阶段验证。

## 6. 当前明确不做

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
- 以增加 Agent、Tool、Skill 或微服务数量替代临床质量评估。

## 7. 当前推荐起点

仍建议以以下 Capability 为首个落地范围：

> 成人常见呼吸道症状的风险分层、结构化信息采集、循证依据展示和就医导航。

该 Capability 第一版只需要：

- 红旗和过敏等 Critical Context Pin；
- 最近 6～10 轮原始对话与结构化摘要；
- 经确认的相关长期病史召回；
- 少量白名单指南和 Claim-Level Citation；
- `collect_respiratory_history`、`evaluate_respiratory_red_flags`、`prepare_clinician_handoff`、`follow_up_respiratory_case` 四个受控 Skill；
- 抽取、对话表达、总结和引用校验的受控模型路由；
- OTel、AgentEvent、context_hash、版本链和不可逆动作幂等；
- Context、Memory、RAG、Injection、Replay 和临床病例回归测试。

第一阶段的目标不是让 Agent 拥有最多上下文、最长记忆或最多技能，而是保证：

> 每个节点只看到完成任务所需且经过授权的信息；关键临床事实不会在压缩中丢失；长期记忆不会被模型推断污染；检索结果有来源和适用边界；模型和 Skill 经过评估；整个决策条件可追溯并能安全恢复。
