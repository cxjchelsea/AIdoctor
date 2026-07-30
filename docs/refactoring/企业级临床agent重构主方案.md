# AIdoctor 企业级临床 Agent 重构方案

> 文档状态：Draft v2.0  
> 首次编写：2026-07-29  
> 最近更新：2026-07-29  
> 适用仓库：`cxjchelsea/AIdoctor`  
> 目标读者：项目负责人、后端开发、Agent 开发、算法开发、临床顾问、测试与运维人员

---

## 0. v2.0 变更摘要

本版本在 v1.0 的企业工程重构基础上，引入对 K Health、Ada Health、Ubie、Infermedica / Symptomate、Buoy Health、Doctronic、LumineticsCore（原 IDx-DR）和 Google AMIE 的公开产品与研究资料分析，并据此调整 AIdoctor 的目标架构。

本次不是简单增加“竞品分析”章节，而是对原方案进行以下核心修正：

1. **LangGraph 只负责执行编排，不承担临床真值判断。**
2. **将临床推理、分诊安全、对话表达和高风险医疗动作的决策权明确拆分。**
3. **将 CDP 从大块 JSON 状态升级为带来源、时间、置信度和版本的临床证据账本。**
4. **将问诊过程改为 Safety Loop 与 Diagnostic Loop 双循环。**
5. **诊断概率与分诊等级彻底解耦，分诊安全链路不可被 Planner 绕过。**
6. **新增 Input Quality Gate、Capability Envelope 和 `OUT_OF_SCOPE` 等一等公民状态。**
7. **新增患者版、医生版、系统版三类交付结果，以及真实医生审核工作台。**
8. **新增 Care Navigation、预约/科室路由和纵向 Follow-up Graph。**
9. **将多 Agent 设计收敛为四个逻辑角色，不以 Agent 数量作为先进性指标。**
10. **新增患者模拟器、OSCE 风格评估、纵向病例和多模态病例评估。**
11. **新增影子模式、医生辅助模式、小流量患者模式等分阶段验证策略。**
12. **将最终目标从“智能诊断 Demo”调整为“受约束临床决策支持与医疗服务交付平台”。**

### 0.1 与 v1.0 相比需要重新理解的架构关系

```text
v1.0 容易被理解为：
LangGraph Agent → 规划并调用医疗工具 → 得出临床结果

v2.0 明确调整为：
LangGraph Agent Runtime → 管理执行、恢复和工具治理
Clinical Intelligence → 管理证据、问题策略、诊断候选和分诊
Safety Engine → 管理不可绕过的风险边界
LLM → 负责理解、总结和表达
Clinician → 承担高风险和受监管医疗决策
```

### 0.2 v2.0 的首要建设顺序

```text
可信基线
→ Clinical Domain / Evidence Ledger
→ Clinical Intelligence
→ LangGraph Runtime
→ 医生接管与交付
→ 评估、治理与分阶段放量
```

不得反向从“先搭复杂 Agent 图”开始。

---

## 1. 文档目的

本文档用于指导 AIdoctor 从当前“多服务 + 固定诊断 Workflow + 初步自研 Agent 循环”的实现，逐步重构为一个具备企业工程能力的**受约束临床决策支持 Agent 平台**。

本次重构不以“增加更多模型调用”“构建更多 Agent”或“拆出更多微服务”为目标，而以以下结果为核心：

1. 形成唯一、清晰、可维护的问诊主执行链路；
2. 实现多轮状态持久化、暂停、恢复、回放和人工审核；
3. 建立受约束的工具调用、状态更新和权限控制机制；
4. 建立有效的工具结果评估、重新规划、重试和降级闭环；
5. 建立不可绕过的医疗风险拦截、拒答、升级和审计机制；
6. 将临床推理和语言模型表达解耦，避免 LLM 成为唯一临床决策者；
7. 将问诊结果连接到医生审核、科室导航、预约和随访；
8. 建立可观测、可测试、可部署、可回滚的企业级工程体系；
9. 使项目文档、实现状态和运行结果保持一致；
10. 形成可以逐阶段验证的临床 AI 演进路线。

---

## 2. 项目重新定位

### 2.1 推荐定位

AIdoctor 应定位为：

> 面向社区与基层医疗场景的受约束临床决策支持 Agent。系统以临床决策包 CDP 维护患者状态、临床证据、诊断候选、不确定性、风险和处置计划，以状态图管理多轮问诊执行过程，通过结构化临床推理能力和受治理医疗工具完成病例理解、信息缺口分析、主动问诊、鉴别诊断、证据检索、风险评估、就医导航和随访，并通过规则门控、输入质控、结果评估、人工审核、固定流程降级和全链路审计保证过程可恢复、可追踪、可解释。

### 2.2 系统不是自主通用医生

以下表述在现阶段不应作为项目对外承诺：

- 自动替代医生完成通用诊断；
- 专科医生级自主诊断系统；
- 已经具备完整自主规划和自主医疗决策能力；
- 已经完成临床验证或医疗器械级合规；
- 所有服务均已完整实现；
- 所有异常场景均已覆盖；
- 多 Agent 共识天然等于正确；
- 使用 LangGraph 就等于实现了医疗 Agent；
- 能输出治疗建议就等于具备处方或治疗决策能力。

### 2.3 产品边界

默认能力边界：

- 支持健康信息采集；
- 支持可能疾病和鉴别方向提示；
- 支持风险与紧急程度评估；
- 支持检查和就医路径建议；
- 支持向医生生成结构化病例摘要；
- 支持随访与症状变化重新评估；
- 不默认输出确定性诊断；
- 不默认自主修改药物剂量；
- 不默认替代急诊和线下检查；
- 高风险、超范围和低质量输入必须进入拒答、升级或人工审核路径。

### 2.4 目标用户与价值

#### 患者

- 用自然语言描述问题；
- 更快完成结构化病史采集；
- 获得风险等级和下一步行动；
- 知道何时需要医生或急诊；
- 在后续症状变化时获得重新评估。

#### 医生

- 获得完整而非仅摘要化的临床证据；
- 获得 SOAP、DDx、支持/反对证据和缺失信息；
- 减少重复问诊和文书负担；
- 对高风险动作保留最终控制；
- 能查看 Agent 的数据来源、失败和不确定性。

#### 医疗机构或平台

- 将患者路由到合适的科室和渠道；
- 提高 Intake、分诊和随访效率；
- 对能力、模型、规则和知识版本进行治理；
- 获得可观测、可审计和可逐步放量的系统。

---

## 3. 当前实现基线与核心问题

### 3.1 当前值得保留的能力

当前仓库已经具备以下有价值的基础：

- Spring Boot 诊断服务和 CDP 领域对象；
- 固定五阶段诊断 Workflow；
- 病例理解、主动问诊、诊断引擎、检查建议、治疗推理、风险评估和解释生成等工具服务；
- MySQL/Oracle、Redis、Neo4j 等基础设施接入；
- CDP 版本记录、审计轨迹和执行追踪设计；
- 自研 `Observe → Plan → Act → Update → Evaluate` Agent 循环骨架；
- React 前端和部分 Docker Compose 编排；
- 医疗知识图谱、多引擎融合和多模态处理方向。

这些内容说明项目已经完成临床流程拆分和工程骨架建设，不需要从零重写。

### 3.2 当前核心问题

#### 3.2.1 存在多个潜在“主大脑”

当前同时存在：

1. Java 固定五步 `DiagnosisWorkflowOrchestrator`；
2. Java 自研 `AgentLoop`；
3. 计划引入的 LangGraph Agent 编排。

如果三套编排同时发展，会出现：

- 状态来源不唯一；
- 相同业务规则重复实现；
- 一个会话由多个状态机竞争控制；
- 故障恢复无法确定恢复到哪一层；
- 测试组合爆炸；
- 文档描述与真实执行路径不一致。

#### 3.2.2 自研 AgentLoop 仍接近带动态插入的固定 Workflow

当前 AgentLoop 虽然具备 Observe、Plan、Act、Update、Evaluate 结构，但默认工具选择仍然与 Step 1～5 强绑定。它可以作为思路验证，但不适合作为长期企业级执行运行时。

#### 3.2.3 临床状态、执行状态和持久化职责混杂

CDP、AgentState、审计、版本、执行轨迹、工具结果之间职责边界不清，容易出现：

- 同一字段多处写入；
- 无法区分患者原话、医生记录和模型推断；
- 无法追踪状态为什么变化；
- 回滚时无法判断哪些临床证据应该保留；
- 业务数据库版本与 Agent checkpoint 版本混淆。

#### 3.2.4 工具协议“有结构但无治理”

当前已有 ToolContext 和 ToolResult 思路，但仍缺少：

- 字段级读写权限；
- 输入适用性检查；
- 幂等性；
- 版本契约；
- 超时真正执行；
- 可重试错误分类；
- 结果质量验证；
- 替代工具和降级路径；
- 对工具建议写入的统一审核。

#### 3.2.5 安全链路可能被普通 Planner 绕过

如果风险评估只是一个可选工具，Planner 可能因为预算、失败或路由错误而未调用。医疗安全检查必须作为不可绕过的系统链路，而不是普通工具选择。

#### 3.2.6 缺少真实交付闭环

当前主链路主要关注“完成诊断流程”，但缺少：

- 医生版病例摘要；
- 人工审核队列；
- 科室和就医渠道导航；
- 预约、转诊和检查衔接；
- 纵向随访；
- 患者症状变化后的重新评估。

#### 3.2.7 实现状态与文档承诺不一致

重构前必须建立可信基线，所有“完整实现”都需要有端到端测试、异常测试和可复现运行结果支撑。

#### 3.2.8 缺少可度量的临床智能

当前“智能化”主要由动态工具调用和模型生成体现，但尚未明确：

- 下一问题为什么最有价值；
- 信息增益如何计算；
- 诊断候选如何被支持或反驳；
- 何时应停止问诊；
- 分诊为何升级；
- 多 Agent 或新模型是否真实优于基线；
- 患者体验改进是否以安全性为代价。

---

## 4. 外部产品与研究带来的设计启发

### 4.1 使用说明

商业产品通常不会公开完整源码、部署拓扑、模型参数和内部算法。本节严格区分：

- **公开确认**：来自产品官网、开发者文档、监管文件或同行评审论文；
- **架构启发**：AIdoctor 可吸收的设计思想；
- **不应直接复制**：公开信息不足、宣传性较强或不适合当前项目规模的部分。

### 4.2 横向对比

| 项目 | 公开可确认的核心模式 | 对 AIdoctor 的主要启发 |
|---|---|---|
| K Health | AI Intake、分诊、文书与 Epic/医疗系统集成 | 从诊断 Demo 升级为医疗服务交付系统 |
| Ada Health | LLM + 专家知识库 + 概率推理引擎的混合架构 | LLM 不拥有最终临床决策权 |
| Ubie | 约 3 分钟的动态问诊、医生治理和真实反馈 | 优化问题价值、用户成本和完成率 |
| Infermedica | 无状态推理 Engine + 有状态 Platform；Diagnosis、Triage、Rationale、Explain 分离 | 证据协议、状态分层、诊断与分诊解耦 |
| Buoy Health | 症状评估后连接服务导航和后续行动 | 新增 Care Navigation 和 Follow-up |
| Doctronic | 多角色协作、SOAP、医生接管；处方续签采用受限范围和分阶段审核 | 真实 ReviewTask、能力白名单和逐阶段放权 |
| LumineticsCore | 极窄适应证、输入质量检查、可输出无法判断、明确下一步 | Capability Envelope 和 Input Quality Gate |
| Google AMIE | 自博弈患者模拟、推理时规划、多维 OSCE 评估、纵向与多模态研究 | 建设患者模拟器和临床多维评估体系 |

### 4.3 K Health：交付层优先于单次诊断

K Health 的公开产品结构强调：

- AI 调查症状和分诊；
- 自动生成临床文书；
- 患者入口可嵌入健康系统应用；
- 医生端与 EHR/Epic 工作流衔接；
- 前、中、后就诊流程连续。

对 AIdoctor 的启发：

1. 不能只设计“Agent 如何得出结论”；
2. 必须设计“结果交给谁、进入什么医疗流程、下一步发生什么”；
3. 需要患者版、医生版、系统版三种结果；
4. 需要 EHR/EMR、预约、检查、转诊和随访适配层；
5. 业务 API 与 Agent Runtime 必须解耦。

### 4.4 Ada Health：混合临床 AI

Ada 公开的混合模式表明：

- LLM 负责理解自由表达和上下文；
- 追问受专家维护知识库约束；
- 概率推理引擎独立评估症状；
- 临床推理引擎保留最终决策权；
- 输出强调白盒、可解释和可审计。

对 AIdoctor 的启发：

- LangGraph 只负责执行；
- LLM 负责自然语言理解、总结和表达；
- 临床推理引擎负责疾病候选、问题价值和证据关系；
- 安全规则负责红旗、禁忌和升级；
- 任何 LLM 输出都不能直接覆盖 CDP 临床事实。

### 4.5 Ubie：把用户交互成本纳入临床策略

Ubie 公开强调短问诊、个性化问题、医生持续监督和真实反馈。

对 AIdoctor 的启发：

- 不能仅依赖“信息完整度达到 60%”决定停止；
- 问题选择必须考虑诊断区分度、风险价值、信息增益和用户成本；
- 需要监控重复提问率、平均轮数、完成率和患者理解度；
- 应支持短分诊模式与完整评估模式。

### 4.6 Infermedica：最值得直接借鉴的协议骨架

Infermedica 将以下能力分离：

- 自由文本到医学概念；
- 证据收集；
- 下一个问题；
- 疾病候选排序；
- 是否停止；
- 分诊等级；
- 为什么问这个问题；
- 为什么支持或反对某个疾病；
- 科室和渠道推荐。

其 Engine API 是无状态推理能力，Platform API 提供有状态问诊、Intake 和 Follow-up。

对 AIdoctor 的启发：

1. Clinical Inference Engine 应尽量无状态；
2. Agent Runtime 负责保存问诊状态；
3. CDP 必须保存全部 evidence，而不是只保存最终摘要；
4. Diagnosis 和 Triage 分开；
5. `question_rationale` 和 `condition_explain` 应是标准接口；
6. `diagnosis_unknown`、`should_stop` 等状态必须明确建模。

### 4.7 Buoy Health：从结果页延伸到下一项行动

对 AIdoctor 的启发：

- 输出应该包含去哪里、何时去、选择什么渠道；
- 结果页不是终点；
- 需要设计 Follow-up Graph；
- 用户症状加重时应重新分诊；
- 个性化应基于健康旅程，而不仅是当前对话。

### 4.8 Doctronic：能力白名单、医生接管和渐进放权

Doctronic 的对外资料强调多角色协作和 SOAP 交付；其犹他州处方续签试点则公开展示了更重要的企业级模式：

- 只允许续签已有处方；
- 不允许新开药或修改剂量；
- 排除受控药物；
- 采用药物白名单；
- 复杂、冲突或高风险情况升级医生；
- 第一阶段全部由持证医生审核；
- 达到指标后才可能进入下一阶段；
- 持续向监管机构报告安全指标。

对 AIdoctor 的启发：

- Human Review 必须是真实工作流；
- 每类能力必须有白名单和禁止项；
- 上线必须分阶段；
- 不能以“多个 Agent 达成共识”代替临床验证；
- 审核结果必须反向进入评估和模型治理。

### 4.9 LumineticsCore：限制能力比增加能力更重要

LumineticsCore 的核心价值来自：

- 单病种；
- 明确人群；
- 明确设备和输入；
- 明确操作流程；
- 明确输出；
- 输入质量不足时不强行判断；
- 明确转诊路径。

对 AIdoctor 的启发：

- 每个能力都需要 Capability Envelope；
- 每个工具调用前必须先做输入质量检查；
- `INSUFFICIENT_INPUT` 和 `OUT_OF_SCOPE` 是正常结果，不是异常；
- 不应在不满足适用条件时继续生成结论。

### 4.10 Google AMIE：训练和评估比编排框架更关键

AMIE 公开研究的重要内容包括：

- 使用患者 Agent、医生 Agent、Moderator/Critic 构造模拟对话；
- 通过自博弈扩展疾病和对话覆盖；
- 推理时逐轮更新诊断不确定性和下一步问题；
- 使用病史采集、诊断、管理、沟通和共情等多维指标；
- 扩展到纵向多次就诊和多模态输入；
- guardrailed 版本将病史采集与医生医疗决策拆开。

对 AIdoctor 的启发：

- 必须建设 Patient Simulator；
- 必须测试“关键内容是否被主动问出”；
- 需要 OSCE 风格病例；
- 医疗 Agent 的评估不能只有 Top-k；
- 纵向随访和多模态不能只是一次性工具调用；
- 早期可以采用“AI 采集 + 医生决策”的保守模式。

### 4.11 外部经验的组合方式

AIdoctor 不直接复制某一产品，而采用组合策略：

```text
Infermedica → 证据与推理 API 骨架
Ada         → 混合临床推理和决策权分配
K Health    → 医疗系统集成与医生工作流
Ubie        → 问诊效率和用户成本
Buoy        → 就医导航与持续行动
Doctronic   → 医生接管、SOAP 和渐进放权
Luminetics  → 能力边界与输入质量门
AMIE        → 患者模拟、纵向推理和多维评估
```

不采用：

- 未经证实的内部技术细节；
- 以 Agent 数量为目标；
- 以模型自评代替临床评价；
- 将商业宣传指标直接作为项目验收指标；
- 在没有适应证和验证的情况下扩大能力。

---

## 5. 重构原则

### 5.1 单一主执行引擎

- Python + LangGraph 作为唯一 Agent Runtime；
- Java 固定 Workflow 保留为 fallback；
- Java 自研 AgentLoop 冻结，完成迁移后删除或归档；
- 不允许三个编排器同时处理生产会话。

### 5.2 编排与临床推理解耦

LangGraph 决定：

- 当前执行哪个节点；
- 调用哪个受治理工具；
- 是否暂停、恢复、重试或降级；
- 是否创建人工审核任务。

Clinical Inference Engine 决定：

- 疾病候选；
- 支持和反对证据；
- 临床不确定性；
- 下一问题的临床价值；
- 是否仍需要收集证据。

Safety Engine 决定：

- 红旗；
- 特殊人群；
- 欠分诊风险；
- 禁止输出；
- 是否必须升级。

LLM 决定：

- 如何理解自由文本；
- 如何向患者提问；
- 如何总结；
- 如何解释已经被临床引擎和安全规则允许的内容。

### 5.3 模型建议不能直接写入临床事实

所有 Tool 和 LLM 只能返回 `proposed_writes`。State Committer 统一执行：

1. Schema 校验；
2. 字段权限校验；
3. 来源和置信度补全；
4. 冲突检查；
5. 版本检查；
6. 审计记录；
7. 状态提交。

### 5.4 安全链路不可绕过

Safety Loop 每轮必跑，不是 Planner 可选工具。

### 5.5 允许不知道和停止

系统必须原生支持：

- `INSUFFICIENT_INPUT`；
- `OUT_OF_SCOPE`；
- `DIAGNOSIS_UNKNOWN`；
- `REQUIRES_HUMAN_REVIEW`；
- `EMERGENCY_ESCALATION`；
- `USER_STOPPED`；
- `TOOLCHAIN_DEGRADED`。

### 5.6 模块化单体优先

先收敛 Python 工具服务为可测试的领域模块，确认团队规模、性能和部署隔离需求后再拆微服务。

### 5.7 通过评估证明“更智能”

任何动态路由、多 Agent 或新模型设计，都必须通过固定评估集和消融实验证明收益。

### 5.8 患者沟通和临床决策使用不同质量标准

- 自然、共情和易理解属于沟通质量；
- 正确、完整、可解释和安全属于临床质量；
- 沟通高分不能抵消临床错误；
- 临床正确但表达难以理解也不能视为完整交付。

---

## 6. 目标总体架构

```text
┌───────────────────────────────────────────────────────────────┐
│                    Patient / Clinician UI                      │
│ 患者问诊端 | 医生审核台 | 运营管理台 | 随访端                 │
└──────────────────────────────┬────────────────────────────────┘
                               │
┌──────────────────────────────▼────────────────────────────────┐
│              Business & Care Delivery Layer                    │
│ Spring Boot API / BFF                                          │
│ 用户 | 患者 | 权限 | 会话 | EMR/EHR | 预约 | 转诊 | 审核 | 随访 │
└──────────────────────────────┬────────────────────────────────┘
                               │
┌──────────────────────────────▼────────────────────────────────┐
│                 Agent Runtime - FastAPI + LangGraph             │
│ GraphState | Checkpoint | Interrupt | Retry | Fallback          │
│ Tool Registry | State Committer | Execution Policy             │
└──────────────────────────────┬────────────────────────────────┘
                               │
┌──────────────────────────────▼────────────────────────────────┐
│                    Clinical Intelligence Layer                  │
│ Terminology | Evidence Ledger | Question Policy                │
│ Diagnostic Inference | Triage & Safety | Care Navigation       │
│ Follow-up Reasoning | Guideline Retrieval                      │
└──────────────────────────────┬────────────────────────────────┘
                               │
┌──────────────────────────────▼────────────────────────────────┐
│                       Governed Tool Layer                       │
│ Clinical Parsing | RAG | Knowledge Graph | OCR                 │
│ Workup Planning | Explanation | Report Processing              │
└──────────────────────────────┬────────────────────────────────┘
                               │
┌──────────────────────────────▼────────────────────────────────┐
│ Data / Governance / Observability / Evaluation                 │
│ PostgreSQL | Redis | Neo4j | Object Storage | OpenTelemetry    │
│ Knowledge Version | Prompt Version | Model Registry | Evals    │
└───────────────────────────────────────────────────────────────┘
```

### 6.1 架构请求流

```text
Patient Web
→ Spring Boot 创建 encounter/thread
→ Agent Runtime 加载 Capability 与 CDP
→ Safety + Clinical Intelligence 生成下一动作
→ Governed Tool 执行并返回 proposed_writes
→ State Committer 更新 Evidence Ledger
→ LangGraph checkpoint
→ 患者继续回答或医生审核
→ Delivery Layer 创建结果、导航和随访任务
```

### 6.2 架构依赖方向

允许：

```text
UI → Business API → Agent Runtime → Clinical Intelligence → Tool/Data
```

不允许：

- Tool 直接控制 Graph；
- LLM 直接写数据库；
- 临床推理引擎直接创建预约；
- 前端直接调用内部临床工具；
- Safety Engine 依赖 Response Composer 才能工作；
- ReviewTask 只存在内存中。

---

## 7. 服务职责划分

### 7.1 Spring Boot Business API

负责：

- 用户、患者和组织权限；
- 会话入口；
- 患者授权和隐私策略；
- 业务记录；
- EHR/EMR 适配；
- 人工审核任务；
- 预约、转诊和通知；
- 管理后台；
- 对外 API；
- 审计查询。

不负责：

- Agent 状态图；
- LLM 规划；
- 临床问题选择；
- 诊断候选排序。

### 7.2 Python Agent Runtime

负责：

- LangGraph 图定义；
- Checkpoint；
- Interrupt 和恢复；
- 节点路由；
- 工具调用策略；
- 预算和超时；
- 失败分类；
- retry/switch/fallback；
- State Committer；
- 创建人工审核请求；
- 调用固定 Workflow 降级。

### 7.3 Clinical Intelligence Layer

建议初期作为同一 Python 应用内的独立 package：

- `clinical-terminology`；
- `evidence-ledger`；
- `question-policy`；
- `diagnostic-inference`；
- `triage-safety`；
- `care-navigation`；
- `follow-up-reasoning`；
- `guideline-retrieval`。

### 7.4 Governed Tool Layer

将现有 tool_0～tool_7 收敛为领域模块：

| 新领域模块 | 吸收现有能力 |
|---|---|
| clinical-understanding | tool_1、tool_2 部分能力 |
| clinical-reasoning | tool_3、tool_6、知识图谱与证据融合 |
| care-planning | tool_4、tool_5、tool_7 |
| multimodal-processing | OCR、报告解析、图像质量检查 |
| knowledge-service | RAG、指南、文献和版本管理 |

### 7.5 Evaluation Platform

负责：

- 数据集管理；
- Patient Simulator；
- 自动病例运行；
- 指标计算；
- 模型/Prompt 对比；
- 子群分析；
- 医生盲评任务；
- 评估报告归档；
- CI 质量门禁。

---

## 8. 决策权矩阵

| 决策 | 主决策者 | LLM 权限 | 是否可自动执行 |
|---|---|---|---|
| 用户自由文本理解 | LLM + Terminology | 生成候选概念 | 校验后可 |
| 临床事实写入 | State Committer | 只能建议 | 是，但需规则校验 |
| 下一问题 | Question Policy | 负责自然语言表达 | 低风险可 |
| 疾病候选排序 | Clinical Inference | 可作为第二意见 | 可生成辅助结果 |
| 分诊等级 | Triage/Safety Engine | 不拥有最终权 | 规则通过后可 |
| 红旗升级 | Safety Engine | 只能补充解释 | 必须执行 |
| 检查建议 | Clinical Reasoning + 规则 | 可生成说明 | 按风险分级 |
| 治疗和用药变更 | 医生 | 只能草拟 | 默认不可 |
| 最终患者表达 | Response Composer | 主负责 | 经输出安全门后 |
| 人工审核结果 | 持证人员 | 不可覆盖 | 是 |

### 8.1 冲突裁决顺序

当不同模块意见冲突时，按以下顺序裁决：

```text
监管/Capability 禁止项
> 确定性安全规则
> 医生审核结论
> 临床推理引擎
> 经验证的工具结果
> LLM 建议
> 通用生成结果
```

高优先级结论可以阻止低优先级动作，但必须保留冲突记录。

---

## 9. 核心状态模型

### 9.1 CDP：临床领域状态

```python
class ClinicalDecisionPackage(BaseModel):
    cdp_id: str
    patient_id: str
    session_id: str
    encounter_id: str
    version: int

    capability_id: str
    patient_profile: PatientProfile
    observations: list[ClinicalObservation]
    hypotheses: list[DiagnosticHypothesis]
    uncertainty: UncertaintyState
    triage: TriageAssessment | None
    workup_plan: WorkupPlan | None
    care_path: CarePath | None
    follow_up_plan: FollowUpPlan | None
    final_conclusion: FinalConclusion | None
```

CDP 不保存 LangGraph 节点游标，也不保存基础设施重试状态。

### 9.2 ClinicalObservation：证据账本最小单元

```python
class ClinicalObservation(BaseModel):
    observation_id: str
    concept_id: str
    concept_type: Literal[
        "symptom", "sign", "risk_factor", "history",
        "medication", "allergy", "lab", "imaging", "document"
    ]

    status: Literal["present", "absent", "unknown"]
    value: Any | None
    unit: str | None

    source: Literal[
        "user_initial", "user_answer", "ehr", "report",
        "suggested", "red_flag_check", "tool_inference", "clinician"
    ]

    onset: datetime | None
    temporal_status: str | None
    confidence: float
    provenance_id: str
    recorded_at: datetime
    supersedes: str | None
```

必须满足：

- 患者原话不可被模型推断覆盖；
- 否定证据与未知证据明确区分；
- 每条证据有来源；
- 每条证据有时间；
- 每次修改保留历史；
- 医生更正生成新版本，不物理删除旧证据。

### 9.3 DiagnosticHypothesis

```python
class DiagnosticHypothesis(BaseModel):
    condition_id: str
    probability: float | None
    rank: int

    supporting_evidence: list[str]
    contradicting_evidence: list[str]
    missing_discriminators: list[str]

    severity: str
    urgency: str
    confidence: float
    inference_version: str
```

### 9.4 AgentState：执行状态

```python
class AgentState(TypedDict):
    thread_id: str
    cdp_id: str
    encounter_id: str
    turn_number: int

    current_phase: str
    next_action: str | None
    current_plan: list[PlannedAction]
    missing_information: list[InformationGap]
    tool_history: list[ToolExecution]

    consecutive_failures: int
    total_tool_calls: int
    elapsed_time_ms: int
    estimated_cost: float

    requires_human_review: bool
    review_reason: str | None
    fallback_reason: str | None
    messages: list
```

### 9.5 CapabilityEnvelope：能力边界

```yaml
capability_id: adult_respiratory_triage_v1
status: shadow
supported_population:
  min_age: 18
  max_age: 80
excluded_population:
  - pregnancy
  - severe_immunosuppression
supported_complaints:
  - cough
  - fever
  - dyspnea
allowed_outputs:
  - possible_conditions
  - triage
  - care_navigation
disallowed_outputs:
  - definitive_diagnosis
  - prescription_change
required_evidence:
  - age
  - biological_sex
  - symptom_duration
  - red_flag_answers
human_review_rules:
  - low_confidence
  - conflicting_evidence
  - high_risk
```

### 9.6 ReviewTask

```python
class ReviewTask(BaseModel):
    review_id: str
    cdp_id: str
    cdp_version: int
    checkpoint_id: str

    reason: str
    risk_level: str
    proposed_action: dict
    evidence_snapshot: list[str]

    status: Literal["pending", "approved", "edited", "rejected", "expired"]
    reviewer_id: str | None
    review_comment: str | None
    resume_token: str
```

### 9.7 Checkpoint

由 LangGraph 管理执行快照：

- 当前节点；
- GraphState；
- interrupt 信息；
- 执行历史；
- 恢复点。

Checkpoint 不代替 CDP 版本，也不代替合规审计。

### 9.8 原始叙述与结构化事实分离

除 Evidence Ledger 外保留原始叙述：

```python
class SourceArtifact(BaseModel):
    artifact_id: str
    artifact_type: Literal["message", "document", "image", "audio", "ehr_record"]
    raw_content_location: str
    normalized_text: str | None
    owner_id: str
    captured_at: datetime
    quality_status: str
    hash: str
```

ClinicalObservation 通过 `provenance_id` 指向 SourceArtifact，保证可以追溯到原始输入。

---

## 10. 双循环问诊架构

### 10.1 Safety Loop：每轮不可绕过

```text
用户输入 / 工具结果 / 检查结果
            │
            ▼
      结构和输入质量检查
            │
            ▼
      红旗与紧急情况检查
            │
            ▼
      特殊人群与禁忌检查
            │
            ▼
      自伤、他伤与安全风险
            │
            ▼
      风险等级是否发生变化
       ┌────┼──────────────┐
       │    │              │
     普通  人工审核       紧急升级
       │    │              │
       └────┴──────────────┘
```

Safety Loop：

- 不受 Planner 是否选择工具影响；
- 不受 LLM token 预算影响；
- 关键规则优先使用确定性实现；
- 失败时采用保守升级策略；
- 每轮生成可审计结果。

### 10.2 Diagnostic Loop：信息采集与推理

```text
更新 Evidence Ledger
        │
        ▼
更新 Diagnostic Hypotheses
        │
        ▼
计算不确定性和信息缺口
        │
        ▼
生成候选问题 / 检查动作
        │
        ▼
评估问题价值和用户成本
        │
  ┌─────┼───────────────┐
  │     │               │
提问  请求检查       停止/医生介入
  │     │               │
  └─────┴───────回到证据更新
```

### 10.3 两个循环的关系

- Safety Loop 优先级最高；
- Diagnostic Loop 的任何结论都必须经过 Safety Gate；
- 分诊可以提前停止诊断问诊；
- 诊断候选稳定不代表分诊安全；
- 高风险场景无需为了提高 Top-1 准确率继续提问。

### 10.4 模式切换

系统至少支持：

| 模式 | 目标 | 特点 |
|---|---|---|
| emergency_screen | 快速识别紧急风险 | 问题少，Safety 优先 |
| short_triage | 给出安全分诊和渠道 | 不追求完整 DDx |
| full_assessment | 完成较完整问诊与 DDx | 信息增益和体验平衡 |
| clinician_intake | 为医生采集完整病史 | 医生最终决策 |
| follow_up | 评估症状和治疗响应变化 | 使用纵向证据 |

模式由入口场景和 Capability 决定，不能仅由 LLM 自由切换。

---

## 11. LangGraph 主状态图

```text
START
  │
  ▼
load_context
  │
  ▼
capability_check
  ├── out_of_scope → safe_exit / human_review → END
  └── supported
        │
        ▼
input_quality_gate
  ├── insufficient → request_better_input / ask_user
  └── sufficient
        │
        ▼
mandatory_safety_check
  ├── emergency → emergency_escalation → END
  ├── review → create_review_task → interrupt
  └── normal
        │
        ▼
normalize_case
        │
        ▼
commit_evidence
        │
        ▼
update_hypotheses
        │
        ▼
identify_information_gaps
        │
        ▼
question_policy
  ├── ask_user → generate_question → interrupt
  ├── request_artifact → request_document_or_image → interrupt
  ├── call_tool → governed_tool_call
  ├── human_review → create_review_task → interrupt
  └── stop
        │
        ▼
evaluate_result
  ├── accept → commit_state
  ├── retry_same_tool → retry_policy
  ├── switch_tool → alternate_tool
  ├── fallback → fixed_workflow
  ├── ask_user → generate_question
  └── human_review → create_review_task
        │
        ▼
post_update_safety_check
        │
        ▼
build_delivery_package
  ├── patient_result
  ├── clinician_summary
  └── system_actions
        │
        ▼
create_follow_up_plan
        │
        ▼
END
```

### 11.1 节点实现规则

每个节点必须定义：

- 输入 State 字段；
- 输出 State 字段；
- 可调用工具；
- 允许写入的 CDP 字段；
- 超时；
- 重试策略；
- 错误类型；
- 可观测事件；
- 测试用例；
- 是否允许 interrupt；
- 是否可能执行不可逆动作。

### 11.2 不可逆动作规则

创建处方、预约、转诊、发送紧急通知或修改正式病历等动作必须：

1. 在独立 action node 中执行；
2. 使用幂等键；
3. 在执行前保存 checkpoint；
4. 满足权限和人工审核要求；
5. 记录外部系统返回值；
6. 不因 Graph 重放而重复执行。

---

## 12. 问题策略模块

### 12.1 目录建议

```text
packages/question-policy/
├── candidate_generator.py
├── information_gain.py
├── red_flag_priority.py
├── differential_discrimination.py
├── question_cost.py
├── repetition_detector.py
├── mode_policy.py
└── stop_policy.py
```

### 12.2 问题决策对象

```python
class QuestionDecision(BaseModel):
    question_concept_id: str
    rationale_code: str
    rationale_text: str

    target_hypotheses: list[str]
    expected_information_gain: float
    safety_priority: float
    diagnostic_discrimination: float
    user_cost: float
    repetition_penalty: float

    alternatives: list[str]
```

### 12.3 问题评分

```text
QuestionScore =
    w1 × DiagnosticDiscrimination
  + w2 × SafetyValue
  + w3 × ExpectedInformationGain
  + w4 × ManagementImpact
  - w5 × RepetitionPenalty
  - w6 × UserCost
  - w7 × TurnFatigue
```

具体权重必须由评估集和临床审核确定，不允许直接由 LLM 自由生成。

### 12.4 停止策略

满足以下任一条件可以停止或改变路径：

1. 已识别紧急红旗，立即分诊；
2. 下一问题预期信息增益低于阈值；
3. 主要候选稳定且关键排除项已完成；
4. 当前问题必须依靠检查或医生体检；
5. 达到用户体验轮数上限；
6. 用户拒绝继续回答；
7. 输入长期矛盾；
8. 当前能力超出 Capability Envelope；
9. 系统不确定性过高，需要医生；
10. 已满足短分诊模式目标。

不得继续使用单一“完整度 60%”作为主停止条件。

### 12.5 问题去重

问题去重不能只比较文本相似度，还要比较：

- 医学 concept_id；
- 时间范围；
- 属性；
- 否定与未知状态；
- 上一次答案是否充分；
- 当前是否因为状态变化需要重新确认。

---

## 13. 统一工具协议与治理

### 13.1 ToolSpec

```python
class ToolSpec(BaseModel):
    tool_id: str
    name: str
    version: str

    input_schema: dict
    output_schema: dict

    readable_fields: list[str]
    writable_fields: list[str]

    timeout_seconds: int
    max_retries: int
    idempotent: bool

    risk_level: Literal["low", "medium", "high"]
    requires_human_review: bool
    allowed_capabilities: list[str]
    fallback_tool_ids: list[str]
```

### 13.2 ToolContext

```python
class ToolContext(BaseModel):
    trace_id: str
    thread_id: str
    cdp_id: str
    cdp_version: int
    capability_id: str

    requested_read_fields: list[str]
    requested_write_fields: list[str]
    state_summary: dict

    timeout_seconds: int
    cost_budget: float
    idempotency_key: str
```

### 13.3 ToolResult

```python
class ToolResult(BaseModel):
    execution_id: str
    tool_id: str
    tool_version: str

    status: Literal[
        "success",
        "partial_success",
        "insufficient_input",
        "out_of_scope",
        "retryable_failure",
        "permanent_failure",
        "timeout"
    ]

    data: dict
    evidence: list[EvidenceReference]
    proposed_writes: list[StatePatch]

    confidence: float | None
    completeness: float | None
    information_gain: float | None
    input_quality: float | None

    validation_errors: list[str]
    retryable: bool
    duration_ms: int
```

### 13.4 Input Quality Gate

每个工具调用前先返回：

```python
class InputQualityResult(BaseModel):
    sufficient: bool
    quality_score: float
    missing_fields: list[str]
    invalid_fields: list[str]
    unsupported_reasons: list[str]
    retry_instruction: str | None
```

例如：

- OCR 图像模糊时要求重新上传；
- 诊断引擎缺少年龄或红旗回答时不输出排名；
- 报告缺少日期和患者身份时不自动合并；
- 工具不支持儿童时返回 `out_of_scope`。

### 13.5 StatePatch

```python
class StatePatch(BaseModel):
    operation: Literal["add", "supersede", "annotate"]
    path: str
    value: Any
    source: str
    confidence: float
    evidence_ids: list[str]
    requires_review: bool
```

工具不能直接访问数据库任意写入 CDP。

### 13.6 Tool Registry

Tool Registry 至少维护：

- ToolSpec；
- 启用状态；
- Endpoint 或本地实现；
- 适用 Capability；
- 风险等级；
- 当前版本；
- 健康状态；
- 降级工具；
- 发布和下线时间；
- 最近评估结果。

---

## 14. 结果评估与重新规划

### 14.1 四层评估

#### 第一层：结构评估

- JSON Schema；
- 必填字段；
- 类型；
- 枚举；
- 引用有效性；
- 版本兼容。

#### 第二层：输入与适用性评估

- 是否满足 Capability Envelope；
- 输入质量是否充分；
- 是否属于特殊人群；
- 是否超出疾病和工具覆盖范围。

#### 第三层：临床业务评估

- 是否增加有效临床证据；
- 是否与原始证据冲突；
- DDx 是否有支持和反对证据；
- 检查建议是否影响决策；
- 分诊是否与红旗冲突；
- 是否生成无来源治疗建议。

#### 第四层：信息增量评估

```text
InformationGain =
    新增有效证据价值
  + 疾病候选区分价值
  + 风险识别价值
  + 管理决策影响
  - 重复信息
  - 冲突惩罚
  - 用户成本
```

### 14.2 决策枚举

评估器只能输出：

- `ACCEPT`；
- `RETRY_SAME_TOOL`；
- `SWITCH_TOOL`；
- `ASK_USER`；
- `REQUEST_ARTIFACT`；
- `HUMAN_REVIEW`；
- `FALLBACK_WORKFLOW`；
- `STOP`。

### 14.3 重试规则

允许重试：

- 网络超时；
- 临时服务不可用；
- LLM 格式错误；
- 明确标记 retryable 的错误。

不应通过重复调用解决：

- 证据不足；
- 业务冲突；
- 超范围；
- 高风险；
- 用户拒绝；
- 输入质量差但用户未重新提供。

### 14.4 重新规划输入

Replanner 只能接收经过压缩的结构化信息：

- 当前 phase；
- 缺失信息；
- 工具失败类型；
- 已尝试工具；
- 预算；
- Capability；
- Safety 结果；
- 可选动作白名单。

不得将全部数据库内容和敏感原文无差别发送给规划模型。

---

## 15. 诊断与分诊解耦

### 15.1 Diagnostic Inference 输出

- 疾病候选；
- 排名；
- 支持证据；
- 反对证据；
- 缺失区分项；
- 不确定性；
- 是否继续问诊。

### 15.2 Triage Engine 输出

- 紧急程度；
- 根因；
- 触发红旗；
- 症状组合规则；
- 特殊人群加权；
- 建议渠道；
- 最大允许等待时间；
- 是否必须人工审核。

### 15.3 关键约束

- 最高概率疾病不等于最高风险疾病；
- 低概率严重疾病也可能触发高分诊；
- 多个症状组合可以提升分诊；
- 无法诊断时仍可给出安全分诊；
- 高危分诊后不为追求诊断准确率继续长问诊。

### 15.4 TriageAssessment 建议结构

```python
class TriageAssessment(BaseModel):
    level: Literal[
        "emergency", "urgent", "soon", "routine", "self_care", "unknown"
    ]
    reason_codes: list[str]
    red_flag_observation_ids: list[str]
    tuple_rule_ids: list[str]
    recommended_channel: str
    max_wait_time_minutes: int | None
    requires_human_review: bool
    rule_version: str
```

---

## 16. 安全体系

### 16.1 输入安全门

- 急危重红旗；
- 自伤或他伤；
- 意识障碍；
- 严重过敏；
- 特殊人群；
- 药物剂量和停药请求；
- 急诊替代请求；
- 提示词注入和恶意指令；
- 身份和数据归属异常。

### 16.2 工具安全门

- Tool 白名单；
- Capability 白名单；
- 字段级权限；
- 高风险工具审批；
- 输入 Schema；
- 幂等键；
- 数据最小化；
- 禁止 LLM 自行构造任意 SQL；
- 外部知识来源白名单。

### 16.3 输出安全门

- 禁止无依据确定诊断；
- 禁止超范围治疗决策；
- 禁止擅自修改药物；
- 明确证据不足；
- 明确就医时机；
- 高风险结果不走普通患者结论；
- 患者版表达不得暴露内部推理草稿；
- 所有医学事实需关联来源或规则。

### 16.4 失败时的安全原则

医疗安全链路失败时采取保守策略：

- 安全规则服务不可用：进入人工审核或高一级分诊；
- CDP 版本冲突：禁止提交，重新加载；
- 知识来源不可用：禁止生成新增治疗建议；
- 结果无法校验：不提交临床状态；
- Checkpoint 失败：不继续执行不可逆动作。

### 16.5 Prompt Injection 防护

患者输入、报告文本和检索文档一律视为不可信数据：

- 与系统指令分隔；
- 不允许文本修改 ToolSpec；
- 不允许文本扩大 Capability；
- 不允许检索结果决定权限；
- 工具参数必须重新构造并校验；
- 记录注入检测事件，但不向患者暴露内部防御细节。

---

## 17. Human-in-the-loop

### 17.1 触发条件

- 高风险；
- 证据冲突；
- 低置信度；
- 特殊人群；
- 超范围；
- 工具连续失败；
- 拟执行高风险动作；
- 用户主动要求医生；
- 安全规则要求；
- 随访中症状恶化。

### 17.2 工作流

```text
Agent interrupt
      │
      ▼
创建 ReviewTask + CDP 快照
      │
      ▼
医生审核台
  ├── approve
  ├── edit
  ├── reject
  └── request_more_information
      │
      ▼
保存审核人、时间、理由和修改
      │
      ▼
LangGraph resume
```

### 17.3 医生审核台最小信息

- 主诉和病史；
- 已问问题和回答；
- Clinical Observations；
- DDx；
- 支持和反对证据；
- 红旗和分诊原因；
- 不确定性；
- 拟执行动作；
- 模型、Prompt、知识版本；
- 患者原话和模型推断区分；
- 批准、修改、拒绝和补充信息按钮。

### 17.4 审核 SLA

ReviewTask 应按风险定义：

- 优先级；
- 响应时限；
- 超时动作；
- 可转交范围；
- 需要的执业或专业资格；
- 用户等待提示。

审核超时不能自动批准高风险动作。

---

## 18. 临床交付层

### 18.1 患者版结果

包含：

- 当前信息摘要；
- 可能方向，不使用确定性诊断措辞；
- 风险等级；
- 为什么建议就医或观察；
- 需要立即关注的变化；
- 下一步行动；
- 信息不足说明；
- 安全免责声明。

### 18.2 医生版结果

建议采用 SOAP + 临床决策附录：

- Subjective；
- Objective；
- Assessment；
- Plan；
- DDx；
- Supporting Evidence；
- Contradicting Evidence；
- Missing Information；
- Triage Root Cause；
- Agent Uncertainty；
- Tool Failures；
- Review Required。

### 18.3 系统版结果

```python
class DeliveryPackage(BaseModel):
    patient_result: PatientResult
    clinician_summary: ClinicianSummary
    system_actions: list[SystemAction]
    care_path: CarePath
    follow_up_plan: FollowUpPlan
```

SystemAction 示例：

- 创建预约；
- 推荐科室；
- 创建人工审核；
- 请求检查报告；
- 设置随访；
- 发送紧急提示；
- 生成医生草稿。

### 18.4 结果一致性

三类结果可以表达不同，但必须基于同一 CDP 版本：

- 患者版不能出现医生版没有依据的新增医学事实；
- 医生版可以包含更完整的不确定性和证据；
- 系统版动作必须通过权限与幂等校验；
- 任何结果更新需要重新生成一致性校验记录。

---

## 19. Care Navigation 与 Follow-up

### 19.1 CarePath

```python
class CarePath(BaseModel):
    recommended_service: str
    recommended_specialty: str | None
    urgency: str
    care_channel: Literal["self_care", "online", "clinic", "urgent", "emergency"]
    appointment_window: str | None
    preparation_instructions: list[str]
    fallback_service: str | None
    escalation_signs: list[str]
```

### 19.2 Follow-up Graph

```text
诊断/分诊完成
      │
      ▼
生成 24h / 3d / 7d 随访计划
      │
      ▼
采集症状变化和执行情况
  ┌───┼──────────────┐
  │   │              │
改善  无改善         加重
  │   │              │
结束/ 重新评估       立即升级
观察  或医生介入
```

### 19.3 纵向状态

随访必须记录：

- 症状变化；
- 新增检查；
- 治疗响应；
- 未执行建议；
- 风险变化；
- 复诊记录；
- 旧假设的保留、降级或排除。

### 19.4 随访不是定时重复问卷

Follow-up Policy 应根据：

- 当前风险；
- 预期疾病进程；
- 已执行的治疗或检查；
- 患者偏好；
- 上次状态变化；
- 医生要求；

选择随访时间与内容。

---

## 20. 多 Agent 设计原则

不建设以数量为目标的“100 个 Agent”。

初期只保留四个逻辑角色：

1. **Interview Agent**：负责患者沟通和信息采集；
2. **Clinical Reasoning Agent**：负责调用结构化临床推理能力；
3. **Safety Supervisor**：检查风险、边界和输出；
4. **Response Composer**：生成患者版和医生版表达。

另有一个非 LLM 的：

5. **State Committer**：唯一临床状态写入者。

这些角色可以运行在同一个服务中，不必拆成多个部署单元。

只有满足以下条件才继续拆分：

- 消融实验表明质量提升；
- 成本和延迟可接受；
- 责任边界更清晰；
- 能独立测试；
- 能解释共识失败时的裁决方式。

### 20.1 多 Agent 评估要求

新增 Agent 前至少比较：

```text
单模型基线
vs.
单模型 + 结构化工具
vs.
单模型 + 独立 Safety
vs.
多 Agent
```

报告：

- 临床质量变化；
- 安全变化；
- 延迟；
- 成本；
- Token；
- 失败模式；
- 可解释性。

---

## 21. 数据与存储

| 数据类型 | 推荐存储 |
|---|---|
| 用户、患者、授权、业务记录 | PostgreSQL 或现有 Oracle |
| CDP 当前版本 | PostgreSQL/Oracle |
| CDP 历史版本 | PostgreSQL/Oracle |
| Evidence Ledger | PostgreSQL/Oracle |
| LangGraph Checkpoint | PostgreSQL |
| ReviewTask | PostgreSQL/Oracle |
| 缓存、限流、分布式锁 | Redis |
| 医疗知识图谱 | Neo4j |
| 文档和原始报告 | MinIO/S3 |
| 向量检索 | pgvector；规模明确后再独立 |
| 技术遥测 | OpenTelemetry 后端 |
| 合规审计 | 独立不可变审计表/存储 |

数据库策略：

- 开源和本地基线优先 PostgreSQL；
- 公司生产环境若明确要求 Oracle，使用适配器；
- 不同时承诺 MySQL、PostgreSQL、Oracle 三套同等生产支持；
- 数据库迁移统一使用 Flyway/Alembic；
- 所有临床状态表必须有版本和审计字段。

### 21.1 数据最小化

每个节点和工具只能读取完成任务所需字段：

- 使用 read projection；
- 默认不传患者完整身份；
- 日志使用 patient_id_hash；
- 临床原文与遥测分离；
- 外部模型调用前执行脱敏策略；
- 敏感字段访问记录审计。

---

## 22. 可观测性

### 22.1 技术可观测性

统一使用 OpenTelemetry：

- Traces；
- Metrics；
- Logs；
- Java/Python trace context 传播；
- Agent node、tool call 和业务事件关联。

关键上下文字段：

```text
trace_id
thread_id
checkpoint_id
cdp_id
cdp_version
patient_id_hash
encounter_id
turn_number
graph_node
tool_id
tool_execution_id
model_name
model_version
prompt_version
knowledge_version
capability_id
```

### 22.2 Agent 指标

- 问诊完成率；
- 平均问诊轮数；
- 重复提问率；
- 信息缺口减少率；
- 问题信息增益；
- 工具调用成功率；
- Schema 失败率；
- 重试率；
- 工具切换率；
- Workflow 降级率；
- 人工审核率；
- 审核修改率；
- 高危召回率；
- 欠分诊率；
- 过度分诊率；
- 无证据结论率；
- 超范围回答率；
- 单次问诊成本；
- P50/P95/P99 延迟；
- 随访完成率；
- 症状恶化升级成功率。

### 22.3 业务与安全事件

除技术 trace 外，定义结构化事件：

- `capability_rejected`；
- `red_flag_detected`；
- `triage_upgraded`；
- `review_created`；
- `review_modified`；
- `fallback_activated`；
- `unsafe_output_blocked`；
- `knowledge_source_missing`；
- `follow_up_escalated`。

---

## 23. 评估体系

### 23.1 评估分层

#### A. 单元测试

- 规则；
- Schema；
- 路由；
- 权限；
- 状态转换；
- 冲突合并；
- 停止策略。

#### B. 契约测试

- Java/Python DTO；
- API；
- JSON Schema；
- 枚举；
- 错误码；
- 版本兼容。

#### C. Graph 路由测试

给定状态，断言下一节点。

#### D. 故障注入

- 工具超时；
- 非法 JSON；
- 空结果；
- Neo4j 不可用；
- Redis 不可用；
- Checkpoint 写入失败；
- LLM 限流；
- 版本冲突；
- 审核超时；
- 外部知识源不可用。

#### E. 临床静态病例

- 普通低危；
- 信息不足；
- 高危红旗；
- 多疾病冲突；
- 特殊人群；
- 用药风险；
- 检查结果冲突；
- 超范围；
- 多模态；
- 纵向随访。

### 23.2 Patient Simulator

```text
Patient Persona
├── Ground Truth
├── 初始主诉
├── 隐藏信息
├── 只有被问到才回答的信息
├── 模糊表达
├── 错误理解
├── 否认或不确定
├── 情绪
├── 拒绝回答
├── 多模态资料
└── 症状随时间变化
```

用于测试：

- 是否主动问到关键红旗；
- 是否重复提问；
- 是否过早停止；
- 是否错误引导；
- 面对矛盾能否恢复；
- 是否在正确时机请求图片或报告；
- 是否正确升级医生。

### 23.3 OSCE 风格评估

评估维度：

- 病史采集；
- 关键问题覆盖；
- 诊断候选质量；
- 管理建议；
- 分诊；
- 安全；
- 沟通清晰度；
- 共情；
- 患者理解；
- 医生工作量影响。

### 23.4 核心指标

#### Diagnostic

- Top-1/Top-3/Top-5；
- DDx 覆盖率；
- 排序质量；
- 支持证据准确率；
- 反对证据准确率。

#### Interview

- 红旗提问召回率；
- 关键病史覆盖率；
- 重复提问率；
- 平均轮数；
- 信息增益；
- 用户完成率。

#### Triage

- 欠分诊率；
- 过度分诊率；
- 高风险召回率；
- 分诊根因可解释率。

#### Safety

- 无证据结论率；
- 禁忌建议率；
- 应升级未升级率；
- 超范围回答率；
- 风险规则遗漏率。

#### Workflow

- 工具失败恢复率；
- Checkpoint 恢复率；
- 人工审核成功率；
- 固定 Workflow 降级成功率；
- 版本冲突处理率。

#### Longitudinal

- 随访计划完成率；
- 症状加重识别率；
- 治疗响应记录完整率；
- 旧结论修正准确率。

### 23.5 子群与偏差分析

按以下维度报告：

- 年龄；
- 性别；
- 孕产状态；
- 常见病/罕见病；
- 语言；
- 健康素养；
- 基础疾病；
- 数据完整度；
- 多模态质量。

### 23.6 自动评估限制

- LLM Judge 只能作为辅助；
- 高风险病例必须有规则或临床专家评价；
- Judge 使用的模型和 Prompt 必须版本化；
- 不允许用被评估模型无约束地评价自身；
- 临床准确性、沟通和格式指标分开计算。

---

## 24. 模型、Prompt 和知识治理

### 24.1 模型治理

每次调用记录：

- 模型供应商；
- 模型名称；
- 版本；
- 参数；
- 使用目的；
- 风险等级；
- 成本；
- 评估结果。

### 24.2 Prompt 治理

Prompt 必须：

- 有版本；
- 有所有者；
- 有适用节点；
- 有输入输出 Schema；
- 有测试集；
- 有变更说明；
- 有回滚版本。

### 24.3 医疗知识治理

每条知识记录：

- 来源；
- 发布机构；
- 版本；
- 生效日期；
- 适用地区；
- 适用人群；
- 审核人；
- 下次复审时间；
- 被哪些结果引用。

### 24.4 变更门禁

模型、Prompt、知识库、问题策略和分诊规则变化，必须执行：

1. 离线回归；
2. 高危病例回归；
3. 子群分析；
4. 人工临床抽检；
5. 影子模式；
6. 灰度发布；
7. 监控和回滚。

### 24.5 Prompt 与知识职责分离

- Prompt 定义任务和输出方式；
- Knowledge 定义可引用的医学内容；
- Safety Rule 定义禁止和升级边界；
- Capability 定义系统允许做什么；
- 不将长篇医学知识硬编码进系统 Prompt。

---

## 25. 推荐仓库结构

```text
AIdoctor/
├── apps/
│   ├── business-api/                  # Spring Boot
│   ├── agent-runtime/                 # FastAPI + LangGraph
│   ├── clinician-console/             # 医生审核台
│   ├── admin-web/
│   └── patient-web/
│
├── packages/
│   ├── clinical-domain/
│   ├── evidence-ledger/
│   ├── agent-state/
│   ├── tool-sdk/
│   ├── state-committer/
│   ├── clinical-terminology/
│   ├── question-policy/
│   ├── clinical-understanding/
│   ├── diagnostic-inference/
│   ├── triage-safety/
│   ├── clinical-reasoning/
│   ├── care-planning/
│   ├── care-navigation/
│   ├── follow-up-reasoning/
│   ├── knowledge-client/
│   ├── review-task/
│   ├── clinical-summary/
│   └── observability/
│
├── evals/
│   ├── datasets/
│   ├── patient-simulator/
│   ├── clinical-vignettes/
│   ├── osce-cases/
│   ├── route-tests/
│   ├── safety-tests/
│   ├── longitudinal-cases/
│   ├── multimodal-cases/
│   ├── adversarial-cases/
│   ├── subgroup-analysis/
│   └── regression/
│
├── contracts/
│   ├── openapi/
│   ├── json-schema/
│   └── events/
│
├── capabilities/
│   ├── adult-respiratory-triage-v1.yaml
│   └── README.md
│
├── knowledge/
│   ├── sources/
│   ├── versions/
│   └── governance/
│
├── infra/
│   ├── docker/
│   ├── kubernetes/
│   ├── otel/
│   ├── prometheus/
│   └── grafana/
│
├── migrations/
├── scripts/
└── docs/
    ├── architecture/
    ├── refactoring/
    ├── adr/
    ├── runbooks/
    └── validation/
```

迁移期间允许旧目录存在，但新代码不得继续复制 ToolContext、ToolResult、异常类和 CDP Reader。

### 25.1 迁移映射

| 现有目录 | 目标位置/处理 |
|---|---|
| diagnosis-service | `apps/business-api`，移除 Agent 编排职责 |
| clinical-parsing-service | `packages/clinical-understanding` |
| dialog-service | Interview Agent + `question-policy` |
| diagnosis-engine-service | `diagnostic-inference` 与知识模块 |
| workup-planner-service | `care-planning` |
| risk-assessment-service | `triage-safety` |
| explanation-service | `clinical-summary` / Response Composer |
| execution-trace-service | 业务事件保留，技术 trace 迁移 OTel |
| ocr-service | `multimodal-processing`，增加质量门 |

---

## 26. 分阶段实施路线

## Phase 0：可信基线与主链路闭环

### 目标

先证明现有系统能真实运行，再引入新架构。

### 任务

1. 修复当前 Java Agent 代码编译和接口不一致；
2. 明确 Java AgentLoop 为实验代码并冻结；
3. 补齐固定五步 Workflow 的响应解析；
4. 建立一条真实端到端问诊；
5. 建立当前能力清单；
6. 将 README 状态改为“已验证/基础实现/框架/TODO”；
7. 为 tool_0～tool_7 增加健康检查；
8. 建立最小临床回归集；
9. 所有工具支持 `insufficient_input` 和 `out_of_scope`；
10. 确认固定 Workflow 可作为未来 fallback。

### 交付物

- 可复现启动说明；
- 单条 E2E；
- 当前能力矩阵；
- 已知问题列表；
- 基础病例集；
- README 纠偏。

### 验收标准

- 新环境可按文档启动；
- 一条完整问诊能返回非空结构化结果；
- Step 1～5 无空解析器；
- 文档承诺均有测试或被标为未完成；
- 固定 Workflow 异常路径可预测。

### 退出条件

在 Phase 0 未通过前，不开始大规模 LangGraph 迁移。

---

## Phase 1：Clinical Domain、Evidence Ledger 与 Contracts

### 目标

建立所有服务共享的临床语言和数据契约。

### 任务

1. 定义 ClinicalObservation；
2. 定义 DiagnosticHypothesis；
3. 定义 TriageAssessment；
4. 定义 CarePath 和 FollowUpPlan；
5. 定义 ToolSpec、ToolContext、ToolResult；
6. 定义 StatePatch；
7. 定义 CapabilityEnvelope；
8. 定义 ReviewTask；
9. 建立 JSON Schema；
10. Java/Python 生成或同步 DTO；
11. 建立字段级读写权限；
12. 建立契约测试；
13. 迁移 CDP 为 Evidence Ledger 兼容模式。

### 交付物

- `clinical-domain`；
- `tool-sdk`；
- `contracts/json-schema`；
- `capabilities/`；
- Evidence Ledger 数据库迁移；
- 契约测试。

### 验收标准

- Java/Python 对同一测试样例序列化结果一致；
- 任意 Tool 不能写入未授权字段；
- 所有 observation 均带 source 和 provenance；
- 患者原始信息与模型推断可区分；
- Schema 变更有兼容策略。

---

## Phase 2：Clinical Intelligence MVP

### 目标

在引入动态 Agent 前，先建立可独立测试的临床核心。

### 任务

1. 医学概念标准化；
2. Evidence Ledger 更新规则；
3. Diagnostic Inference 接口；
4. Triage/Safety Engine；
5. Question Policy；
6. Question Rationale；
7. Condition Explain；
8. Input Quality Gate；
9. Capability 检查；
10. 短分诊模式；
11. 完整评估模式；
12. 基础 Care Navigation；
13. 固定病例离线评估。

### 交付物

- 无状态 Clinical Inference API；
- Safety API；
- Question Policy；
- Explain/Rationale；
- 第一版 Capability；
- 离线评估报告。

### 验收标准

- 相同证据输入产生稳定结构化输出；
- 诊断和分诊可独立调用；
- 高危病例可在不依赖 LLM 的条件下升级；
- 输入不足时不强行生成结论；
- 问题选择可返回可解释 rationale；
- 基线指标可重复计算。

---

## Phase 3：LangGraph Agent Runtime

### 目标

建立唯一、可恢复、可审计的执行运行时。

### 任务

1. 创建 FastAPI Agent Runtime；
2. 实现 GraphState；
3. 接入 PostgreSQL Checkpointer；
4. 实现 capability_check；
5. 实现 input_quality_gate；
6. 实现 mandatory_safety_check；
7. 实现 normalize/commit/update_hypotheses；
8. 实现 question_policy；
9. 实现 interrupt 和 resume；
10. 实现 Tool Registry；
11. 实现 State Committer；
12. 实现 retry/switch/fallback；
13. 固定 Workflow 作为 fallback；
14. Spring Boot 对外 API 切换到 Runtime；
15. 冻结旧 AgentLoop 对外入口。

### 交付物

- Agent Runtime；
- 主状态图；
- Checkpoint；
- Tool Registry；
- State Committer；
- Fallback Adapter；
- E2E 路由测试。

### 验收标准

- 同一 thread 可跨轮继续；
- 服务重启后可恢复；
- 人工输入后从 interrupt 继续；
- 工具超时可分类处理；
- StatePatch 未校验不能写入 CDP；
- Safety Loop 每轮执行；
- 旧 Workflow 可降级接管。

---

## Phase 4：医生接管与临床交付

### 目标

从“问诊引擎”升级为真实临床工作流组件。

### 任务

1. ReviewTask 数据模型；
2. 医生审核台；
3. approve/edit/reject/request_more_information；
4. 审核后 Graph resume；
5. 患者版结果；
6. SOAP 医生版；
7. 系统版 SystemAction；
8. Care Navigation；
9. 预约/转诊适配器；
10. Follow-up Graph；
11. 症状恶化重新分诊；
12. 审核反馈进入评估集。

### 交付物

- clinician-console；
- Review API；
- 三类 DeliveryPackage；
- CarePath；
- Follow-up Graph。

### 验收标准

- 高风险案例无法绕过审核；
- 审核操作有完整审计；
- 医生修改可生成新 CDP 版本；
- 患者版不暴露内部推理；
- 医生版标明证据来源和不确定性；
- 随访加重能触发升级。

---

## Phase 5：企业工程、评估与治理

### 目标

建立可持续迭代和安全发布能力。

### 任务

1. OpenTelemetry；
2. Prometheus/Grafana；
3. Patient Simulator；
4. OSCE 病例；
5. 纵向病例；
6. 多模态病例；
7. 故障注入；
8. 子群分析；
9. Prompt Registry；
10. Model Registry；
11. Knowledge Version；
12. CI 质量门禁；
13. 镜像和依赖扫描；
14. 数据迁移与回滚；
15. Runbook；
16. 安全事件处理流程。

### 验收标准

- 每次变更自动生成评估报告；
- 高危病例指标未达标无法合并；
- 模型/Prompt/知识版本可追踪；
- Java/Python trace 可关联；
- 关键指标有告警；
- 依赖故障有演练记录；
- 发布可回滚。

---

## Phase 6：分阶段临床验证与放量

### 目标

避免一次性把未验证 Agent 暴露给真实医疗决策。

### Stage 0：离线模式

- 只运行测试集；
- 不处理真实患者；
- 临床专家审核输出。

### Stage 1：影子模式

- 读取真实流程副本；
- 不向患者或医生展示；
- 与真实结果比较；
- 记录欠分诊和安全问题。

### Stage 2：医生辅助模式

- AI 生成 Intake 和医生摘要；
- 医生承担全部决策；
- 记录接受、修改和拒绝率。

### Stage 3：受限患者模式

- 只开放明确 Capability；
- 高风险全部人工审核；
- 不开放治疗和处方变更；
- 小流量灰度。

### Stage 4：逐项扩大能力

只有满足预设指标后，才扩大：

- 人群；
- 主诉；
- 输出类型；
- 自动化程度；
- 随访能力。

任何能力扩大都需要新的 Capability 版本和评估报告。

### Phase 6 验收证据

每个 Stage 至少保存：

- 使用的 Capability 版本；
- 病例数量和分布；
- 安全指标；
- 欠分诊和过度分诊；
- 医生修改率；
- 严重事件；
- 进入下一阶段的审批结论。

---

## 27. 初始 Capability 建议

不建议一开始覆盖通用医学全部场景。

建议选择一个风险相对可控、现有数据较充分的能力，例如：

> 成人常见呼吸道症状的风险分层、信息采集和就医导航。

允许：

- 咳嗽、发热、咽痛、鼻塞、轻中度呼吸不适；
- 红旗筛查；
- 可能方向；
- 是否需要线下就医；
- 推荐科室和时间；
- 随访。

不允许：

- 确定性诊断；
- 自主开药；
- 修改处方；
- 儿童、孕妇和严重免疫抑制人群自动处理；
- 严重呼吸困难继续普通问诊。

选择该 Capability 的条件：

- 必须由临床顾问确认；
- 必须有红旗病例集；
- 必须有清晰的线下升级路径；
- 必须可以定义真实验收指标。

### 27.1 初始 Capability 验收重点

不以覆盖疾病数量为主要指标，优先验证：

- 红旗不遗漏；
- 能识别输入不足；
- 能正确停止；
- 能解释为什么继续问；
- 能输出安全就医路径；
- 能被医生快速审核；
- 能在故障时降级。

---

## 28. 现有代码处理建议

### 保留并重构

- CDP 领域思想；
- 病例解析；
- 主动问诊；
- Neo4j 医疗知识图谱；
- 多引擎融合；
- 风险评估；
- AuditTrail；
- React 前端；
- Spring Boot 业务接口；
- 执行追踪中的业务语义。

### 转为 fallback

- `DiagnosisWorkflowOrchestrator`；
- 固定五步路径；
- 默认问题；
- 规则诊断基线。

### 冻结并逐步移除

- Java 自研 `AgentLoop`；
- 多套重复 ToolContext/ToolResult；
- 每个简单工具一个微服务；
- 自研完整技术 tracing 体系；
- 多数据库同等生产承诺；
- 未经验证的“完整实现”描述。

### 28.1 Strangler 迁移策略

1. 保持旧 API 可用；
2. 新建 Agent Runtime；
3. Spring Boot 按 feature flag 路由到新旧路径；
4. 首先仅影子调用新 Runtime；
5. 比较输出和安全指标；
6. 按 Capability 切换流量；
7. 新路径稳定后下线旧 AgentLoop；
8. 固定 Workflow 长期保留为降级，不作为默认主链路。

---

## 29. CI/CD 建议

### Pull Request 门禁

- 格式和静态检查；
- 单元测试；
- 契约测试；
- Graph 路由测试；
- 高危病例回归；
- Schema 兼容检查；
- Prompt 变更评估；
- 依赖安全扫描；
- Docker 构建；
- 评估指标差异报告。

### 发布策略

- 镜像不可变；
- 配置和密钥分离；
- 数据库迁移前向兼容；
- Capability 灰度；
- 模型和 Prompt 可独立回滚；
- Agent Runtime 与工具版本兼容矩阵；
- 发布后自动观察关键安全指标。

### 29.1 Feature Flags

至少支持：

- 新 Runtime 是否启用；
- 指定 Capability 是否开放；
- 是否只运行影子模式；
- 是否要求全部人工审核；
- 新模型/Prompt 百分比；
- 新 Question Policy；
- 新 Triage 规则版本；
- 自动 Follow-up 是否启用。

---

## 30. ADR 清单

建议建立：

1. ADR-001：LangGraph 作为唯一 Agent Runtime；
2. ADR-002：Spring Boot 与 Agent Runtime 职责边界；
3. ADR-003：Clinical Inference 与 LLM 决策权；
4. ADR-004：CDP 与 GraphState 分离；
5. ADR-005：Evidence Ledger；
6. ADR-006：State Committer 单写入者；
7. ADR-007：Safety Loop 不可绕过；
8. ADR-008：Diagnosis 与 Triage 分离；
9. ADR-009：固定 Workflow 作为 fallback；
10. ADR-010：模块化单体优先；
11. ADR-011：Capability Envelope；
12. ADR-012：Human Review 工作流；
13. ADR-013：Care Navigation 与 Follow-up；
14. ADR-014：Patient Simulator 与评估门禁；
15. ADR-015：PostgreSQL 作为开源基线数据库。

---

## 31. Definition of Done

一个功能只有同时满足以下条件，才能标记“已完成”：

### 业务

- 有明确用户场景；
- 有输入和输出；
- 有边界；
- 有错误和超范围结果；
- 有患者和医生交付方式。

### 临床

- 有证据来源；
- 有适用人群；
- 有排除人群；
- 有红旗规则；
- 有不确定性；
- 有人工升级条件；
- 有临床审核人。

### 工程

- 有 Schema；
- 有版本；
- 有权限；
- 有超时；
- 有幂等；
- 有降级；
- 有审计；
- 有监控。

### 测试

- 有单元测试；
- 有契约测试；
- 有正常 E2E；
- 有异常 E2E；
- 有高危病例；
- 有超范围病例；
- 有失败恢复测试；
- 有评估结果。

### 发布

- 有迁移方案；
- 有回滚方案；
- 有 Runbook；
- 有告警；
- 有负责人；
- 有分阶段放量计划。

---

## 32. 首批 Issue 建议

### Epic A：可信基线

- 修复 diagnosis-service 编译和接口不一致；
- 补齐 Step 2～5 解析；
- 建立 E2E；
- 更新实现状态；
- 建立基础病例集。

### Epic B：Clinical Domain

- ClinicalObservation Schema；
- Evidence Ledger；
- DiagnosticHypothesis；
- TriageAssessment；
- CarePath；
- CapabilityEnvelope。

### Epic C：Tool Governance

- ToolSpec；
- ToolResult；
- Input Quality；
- StatePatch；
- Tool Registry；
- State Committer；
- 权限和契约测试。

### Epic D：Clinical Intelligence

- Terminology；
- Question Policy；
- Diagnostic Inference；
- Triage Safety；
- Rationale；
- Explain；
- Care Navigation。

### Epic E：Agent Runtime

- LangGraph skeleton；
- Checkpointer；
- Safety Loop；
- Diagnostic Loop；
- Retry/Switch/Fallback；
- Interrupt/Resume。

### Epic F：Clinical Delivery

- ReviewTask；
- Clinician Console；
- SOAP；
- Patient Result；
- System Action；
- Follow-up Graph。

### Epic G：Evaluation

- Patient Simulator；
- OSCE cases；
- Red flag suite；
- Longitudinal suite；
- Multimodal suite；
- Subgroup analysis；
- CI metric gate。

### 32.1 推荐的前三个实现 PR

#### PR A：Phase 0 基线修复

- 只修复编译、接口和空解析；
- 不引入 LangGraph；
- 增加一条 E2E；
- 更新 README 状态。

#### PR B：Clinical Contracts

- 新增 JSON Schema；
- 新增 ClinicalObservation/ToolResult/StatePatch；
- Java/Python 契约测试；
- 不改变默认业务路径。

#### PR C：Evidence Ledger 兼容写入

- 新表和迁移；
- 将旧 CDP 数据同步为 Observation；
- 保持旧读取接口；
- 增加 provenance 与版本测试。

---

## 33. 主要风险与缓解措施

| 风险 | 表现 | 缓解措施 |
|---|---|---|
| 架构过度设计 | 长期只有文档没有闭环 | Phase 0 优先；每阶段有退出条件 |
| LangGraph 被当作临床引擎 | Planner 自由决定医学结论 | 临床推理与编排分离 |
| LLM 污染临床事实 | 推断覆盖患者原话 | Evidence Ledger + State Committer |
| 多 Agent 成本爆炸 | 延迟和错误传播 | 四角色起步，消融后扩展 |
| 欠分诊 | 严重病例被低估 | Safety Loop、红旗规则、保守升级 |
| 过度分诊 | 大量用户被送急诊 | 分诊规则评估和医生复核 |
| 工具失败被掩盖 | 空结果仍生成答案 | 强制结果状态和质量门 |
| 知识过期 | 建议不符合新指南 | 知识版本和复审机制 |
| 医生审核成为瓶颈 | ReviewTask 堆积 | 风险分层、队列 SLA、工作台优化 |
| 数据隐私风险 | 日志暴露患者信息 | 脱敏、最小化、访问审计 |
| 评估不代表真实环境 | 静态病例高分、真实失败 | 模拟患者、影子模式、分阶段验证 |
| 项目范围过大 | 通用医学无法收敛 | 首个 Capability 收窄 |

---

## 34. 推荐实施优先级

### 第一优先级：可信和安全

1. 编译与接口修复；
2. 固定主链路跑通；
3. Evidence Ledger；
4. Safety Loop；
5. Capability Envelope；
6. 结果状态与输入质控；
7. 基础高危测试。

### 第二优先级：状态与治理

1. Tool SDK；
2. State Committer；
3. Clinical Inference API；
4. Diagnosis/Triage 分离；
5. LangGraph Checkpoint；
6. Retry/Fallback。

### 第三优先级：交付闭环

1. 医生审核；
2. 患者/医生/系统三类结果；
3. Care Navigation；
4. Follow-up。

### 第四优先级：智能提升

1. Question Policy；
2. 信息增益；
3. 患者模拟器；
4. 多模态状态感知；
5. 纵向管理；
6. 经评估证明有效的多 Agent。

### 34.1 不应该优先做的事项

- 继续新增独立微服务；
- 继续扩充 Java AgentLoop；
- 同时接入大量模型；
- 建设大量专家 Agent；
- 在无评估集时优化 Prompt；
- 在无 Capability 时宣传通用诊断；
- 在无审核工作台时实现高风险自动动作。

---

## 35. 最终目标形态

重构完成后的 AIdoctor 不应是：

> 一个 LLM 根据当前状态自由选择八个工具完成诊断。

而应是：

> 一个由 LangGraph 管理执行状态、由结构化临床推理引擎管理疾病与证据、由规则安全引擎控制医疗边界、由 LLM 负责理解和沟通、由医生接管高风险决策，并能连接就医导航和随访的混合临床 Agent 平台。

最终应能够演示以下完整链路：

```text
患者自由描述症状
→ LLM 转为候选医学概念
→ Terminology 和 Schema 校验
→ Evidence Ledger 写入原始证据
→ Safety Loop 运行
→ Clinical Inference 更新 DDx
→ Question Policy 选择高价值问题
→ 用户回答并保存 checkpoint
→ 工具输入质量不足，要求重新上传报告
→ 工具超时，重试后切换替代工具
→ 证据冲突，创建 ReviewTask
→ 医生修改结论并恢复 Graph
→ 输出患者版、医生版和系统版结果
→ 推荐科室和就医时间
→ 创建 3 天随访
→ 症状加重后重新分诊并升级
→ 全过程可恢复、可回放、可审计
```

只有当这条链路能够被测试和复现时，项目才真正具备企业级临床 Agent 的说服力。

---

## 36. 外部参考资料

以下资料用于形成本文的产品和架构启发。商业公司未公开的内部实现不应被视为已确认事实。

### K Health

- https://khealth.com/

### Ada Health

- https://about.ada.com/press/patent-llm-clinical-safety-layer/
- https://about.ada.com/medical-quality/

### Ubie

- https://ubiehealth.com/symptom-checker
- https://ubiehealth.com/how-ubies-ai-works

### Infermedica

- https://developer.infermedica.com/
- https://developer.infermedica.com/documentation/engine-api/build-your-solution/diagnosis/
- https://developer.infermedica.com/documentation/engine-api/build-your-solution/triage/
- https://developer.infermedica.com/documentation/engine-api/build-your-solution/rationale/
- https://developer.infermedica.com/documentation/engine-api/build-your-solution/explain/

### Buoy Health

- https://www.buoyhealth.com/

### Doctronic / Utah OAIP

- https://www.doctronic.ai/resource-center/
- https://commerce.utah.gov/ai/regulatory-relief/authorized-ai-pilots/doctronic/

### LumineticsCore / IDx-DR

- https://www.digitaldiagnostics.com/products/eye-disease/lumineticscore/
- https://www.accessdata.fda.gov/scripts/cdrh/cfdocs/cfpmn/denovo.cfm?ID=DEN180001

### Google AMIE

- https://research.google/blog/amie-a-research-ai-system-for-diagnostic-medical-reasoning-and-conversations/
- https://www.nature.com/articles/s41586-025-08866-7
- https://research.google/blog/enabling-physician-centered-oversight-for-amie/
- https://www.nature.com/articles/s41586-026-10764-5
- https://www.nature.com/articles/s41591-026-04371-0

---

## 37. 文档维护规则

- 本文档描述目标和迁移计划，不代表所有能力已经实现；
- 每完成一个 Phase，更新对应状态和验收证据；
- 重大架构决定写入 ADR；
- 实现状态只允许使用：`已验证`、`基础实现`、`实验性`、`框架`、`未实现`；
- “已验证”必须附测试或评估链接；
- 外部产品信息应定期核验；
- 临床能力边界变化必须更新 Capability Envelope；
- 任何真实患者模式上线前必须经过临床、安全和隐私审核。

---

## 38. 下一步执行建议

本方案通过后，不应立刻开始 Phase 3。建议按以下顺序创建工作：

1. 创建 Phase 0 Epic；
2. 修复当前编译与接口问题；
3. 建立一条真实 E2E；
4. 创建第一版临床病例回归集；
5. 创建 ClinicalObservation、ToolResult 和 StatePatch Schema；
6. 选择第一个 Capability，并由临床顾问审核边界；
7. 完成 Evidence Ledger 兼容迁移；
8. 再开始 Clinical Intelligence MVP；
9. Clinical Intelligence 有稳定接口和测试后，再接入 LangGraph。

第一阶段的成功标准不是“界面看起来更智能”，而是：

> 系统能够诚实描述自己的能力，稳定完成一条问诊链路，在证据不足和风险场景下正确停止，并且所有临床信息都可追溯。
