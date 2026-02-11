# AI医生系统 - 技术架构设计（核心架构）

> **文档定位**：本文档是AI医生系统的**技术架构设计**的核心架构部分，包含整体架构设计、单主Agent架构设计、工具系统设计和协作流程。  
> **业务功能**：请参考《AI医生系统-系统功能设计.md》  
> **核心目标**：基于DR.KNOWS论文的研究成果，采用单主Agent + 多工具Tools架构，设计临床可用的诊断系统，支持健康管理态和临床诊疗态两种工作态。

---

## 📖 术语表（Glossary）

> **说明**：本文档中使用的关键术语定义，便于快速理解核心概念。

| 术语 | 英文 | 定义 | 首次出现章节 |
|------|------|------|------------|
| **主Agent** | Clinical Agent Brain | 具备自主决策、自主调用工具、停止/升级/拒答能力的单一决策大脑，是唯一"最终结论提交者" | 1.2 |
| **工具** | Tool | 无独立目标、无长期策略状态的能力模块，只按主Agent调用执行并返回结构化结果与证据引用 | 1.2 |
| **CDP** | Clinical Decision Package | 临床决策包，系统内部一切推理的核心数据结构，病例事实唯一事实源 | 1.3 |
| **AgentState** | Agent State | 主Agent的策略状态（阈值、预算、失败回退、已尝试工具等） | 1.3 |
| **AuditTrail** | Audit Trail | 审计轨迹，必须记录工具调用、证据、写回字段、版本、时间 | 1.3 |
| **双通道推理** | Dual-Channel Reasoning | 通道1（结构化推理）决定"该往哪想"，通道2（语言策略）决定"怎么说、怎么问" | 1.1 |
| **DDx** | Differential Diagnosis | 鉴别诊断列表 | 3.2 |
| **知识图谱** | Knowledge Graph | 基于Neo4j的医学知识图谱，用于DR.KNOWS路径推理 | 2.1 |
| **工具协议** | Tool Protocol | 定义ToolContext和ToolResult的I/O契约 | 2.2 |
| **运行循环** | Agent Loop | Observe→Plan→Act→Update→Evaluate→Stop/Escalate循环 | 3.1 |
| **停止条件** | Stop Condition | CDP必填项、证据引用齐全、风险评估完成 | 4.1 |
| **升级策略** | Escalation Strategy | 急危重/超能力/证据不足且风险高的升级处理 | 4.2 |
| **拒答策略** | Refusal Strategy | 无法安全推断的拒答边界与提示 | 4.3 |
| **消息队列** | Message Queue | 基于Redis的工具调用调度层（并行、异步、超时、重试、限流） | 2.3 |
| **写时复制** | Copy-on-Write | 每次更新CDP时创建新版本的机制 | 1.4.3 |
| **服务拆分** | Service Decomposition | 按照功能模块（工具）来组织服务，每个服务对应一个工具 | 1.3 |
| **工作态** | Work Mode | 系统的工作模式：健康管理态（wellness_mode）和临床诊疗态（clinical_mode） | 3.0 |
| **三层分层** | Three-Layer Classification | 首要假设、主要备选诊断、必须排除的高危诊断 | 2.2 |
| **推理子组** | Reasoning Subgroup | 按系统分类、病理生理机制等维度组织的诊断候选组 | 2.2 |
| **分流路径** | Triage Path | 基于关键差异点形成的可执行的分流路径 | 2.2 |
| **验证计划** | Verification Plan | 针对诊断方向制定的验证计划 | 2.3 |
| **回退机制** | Rollback Mechanism | 当出现证据冲突、症状演变或处理无效时的回退机制 | 6.5 |
| **终点结论包** | Conclusion Package | 诊断流程的最终输出，包含四要素 | 6.7 |
| **CUI** | Concept Unique Identifier | UMLS中的医学概念编码 | 2.1 |
| **Neo4j** | - | 图数据库，用于存储和查询知识图谱 | 1.3 |
| **Redis** | - | 内存数据库，用于缓存和消息队列 | 1.3 |

---

## 📋 文档对应关系映射表

> **说明**：本表列出了技术架构文档与功能设计文档的对应关系，便于快速定位相关功能定义。

| 技术架构文档章节 | 功能设计文档章节 | 对应关系说明 |
|----------------|----------------|------------|
| **一、整体架构设计** | | |
| 1.1 双通道推理架构 | 1.2 核心设计原则 | 技术架构 → 设计理念 |
| 1.2 单主Agent架构 | 二、工具功能设计 | 技术实现 → 功能定义 |
| 1.3 系统架构图（服务部署） | 二、工具功能设计 | 服务拆分 → 工具划分 |
| 1.4 服务依赖关系 | 三、完整临床流程设计 | 服务调用 → 流程设计 |
| **二、核心对象与责任边界** | | |
| 2.1 CDP（病例事实唯一事实源） | 1.3 核心数据结构：CDP | CDP定义 → 数据结构 |
| 2.2 AgentState（主Agent策略状态） | - | 新增：主Agent状态管理 |
| 2.3 Tools（能力模块） | 二、工具功能设计 | 工具设计 → 功能定义 |
| 2.4 AuditTrail（审计轨迹） | - | 新增：审计与追溯 |
| **三、Tool协议与I/O契约** | | |
| 3.1 ToolContext（工具调用上下文） | - | 新增：工具调用协议 |
| 3.2 ToolResult（工具返回结果） | - | 新增：工具返回协议 |
| 3.3 工具分类 | - | 新增：工具类型定义 |
| **四、主Agent运行循环与默认诊断路径** | | |
| 4.1 运行循环（Observe→Plan→Act→Update→Evaluate） | 3.3 临床诊疗态详细流程 | 运行循环 → 流程定义 |
| 4.2 默认诊断路径（Step1-5） | 3.3 临床诊疗态详细流程 | 默认路径 → 流程定义 |
| 4.3 动态插入策略 | - | 新增：红旗优先、冲突复核 |
| **五、停止条件、升级与拒答策略** | | |
| 5.1 停止条件（StopCondition） | - | 新增：停止条件定义 |
| 5.2 升级策略（Escalation） | 2.6 风险评估与升级 | 升级策略 → 功能定义 |
| 5.3 拒答策略（Refusal） | - | 新增：拒答边界定义 |
| **六、工具调用调度层** | | |
| 6.1 消息队列定位 | 二、工具系统设计 | 消息系统 → 工具调度 |
| 6.2 并行/异步/超时/重试/限流 | - | 新增：调度机制 |

---

## 一、整体架构设计

### 1.1 双通道推理架构

基于GPT建议和DR.KNOWS论文，采用**双通道推理架构**：

```
┌─────────────────────────────────────────────────────────────┐
│                    通道1：结构化推理通道                      │
│              （决定"该往哪想" - 临床逻辑）                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  医学概念标准化（CUI/ICD/SNOMED）                      │  │
│  │  知识图谱路径检索与排序（DR.KNOWS方法）                │  │
│  │  规则/概率/贝叶斯/评分量表（可插拔）                   │  │
│  │  知识库优先诊断                                         │  │
│  └──────────────────────────────────────────────────────┘  │
│  输出：DDx候选 + 证据结构 + 推理路径                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
                    CDP（Clinical Decision Package）
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    通道2：语言与策略通道                      │
│            （决定"怎么说、怎么问" - 医生表达）               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  问诊对话生成（问什么、怎么问）                        │  │
│  │  解释与沟通（把结构化结果讲成人话）                    │  │
│  │  生成处置方案草案（在受控证据基础上）                  │  │
│  │  自然语言生成（NLG）                                   │  │
│  └──────────────────────────────────────────────────────┘  │
│  输出：自然语言问诊、解释、建议                              │
└─────────────────────────────────────────────────────────────┘
```

**核心原则**：
> **让结构化通道决定"该往哪想"，让LLM决定"怎么说、怎么问、怎么组织方案"**

### 1.2 单主Agent架构（核心架构）

> **核心设计理念**：将原八大智能体重构为单主Agent + 多工具Tools架构，主Agent具备自主决策能力，工具只负责执行并返回结构化结果。

**单主Agent架构核心理念**：

1. **主Agent自主性**：主Agent（Clinical Agent Brain）具备自主感知、推理、决策和执行能力，是唯一"最终结论提交者"
2. **工具无状态性**：工具无独立目标、无长期策略状态，只按主Agent调用执行并返回结构化结果与证据引用
3. **CDP唯一事实源**：CDP是病例事实的唯一事实源，所有工具从CDP读取，建议写回CDP
4. **证据融合与冲突解决**：主Agent内部进行evidence fusion和conflict resolution，而非自治agent协商
5. **审计可追溯**：所有工具调用、证据、写回字段、版本、时间必须记录到AuditTrail

**主Agent（Clinical Agent Brain）**：

- **唯一决策者**：主Agent是唯一"最终结论提交者"，所有诊断结论、检查建议、治疗方案都由主Agent最终决定
- **自主调用工具**：主Agent根据当前CDP状态和AgentState，自主决定调用哪些工具、调用顺序、调用参数
- **停止/升级/拒答能力**：主Agent具备停止条件判断、升级策略执行、拒答边界判断的能力
- **证据融合**：主Agent内部融合多个工具返回的证据，进行冲突解决和一致性检查
- **策略状态管理**：主Agent维护AgentState，包括阈值、预算、失败回退、已尝试工具等

**工具（Tools）**：

- **无独立目标**：工具不拥有独立的诊断目标或治疗目标，只按主Agent调用执行
- **无长期策略状态**：工具不维护长期状态，每次调用都是独立的
- **结构化输出**：工具返回结构化的payload、evidence、quality、suggestedWrites
- **证据引用**：工具必须提供evidence引用，说明输出结果的依据来源
- **建议写回字段**：工具通过suggestedWrites建议主Agent写回CDP的字段路径

**原八大智能体 → 工具映射**：

| 原智能体 | 工具名称 | 工具ID | 主要职责 |
|---------|---------|--------|---------|
| 健康状态判定智能体 | 健康状态判定工具 | tool_0 | 判断工作态、入口判定流程 |
| 病例理解智能体 | 病例理解工具 | tool_1 | 概念归一化、结构化提取 |
| 主动问诊智能体 | 主动问诊工具 | tool_2 | 信息缺口识别、问诊生成 |
| 鉴别诊断智能体 | 鉴别诊断工具 | tool_3 | 多引擎融合诊断、DDx生成 |
| 检查建议智能体 | 检查建议工具 | tool_4 | 检查价值评估、验证计划 |
| 治疗建议智能体 | 治疗建议工具 | tool_5 | 治疗方案推理、药物推荐 |
| 风险评估智能体 | 风险评估工具 | tool_6 | 高危识别、紧急程度评估 |
| 证据链智能体 | 证据链工具 | tool_7 | 证据链构建、解释生成 |

**双通道推理架构在单主Agent系统中的体现**：

#### 通道1：结构化推理通道（决定"该往哪想" - 临床逻辑）

**主要工具**：
- **鉴别诊断工具**：知识库优先 + DR.KNOWS路径验证、路径约束推理
- **检查建议工具**：检查价值评估、信息增益计算
- **治疗建议工具**：治疗方案推理、药物推荐
- **风险评估工具**：风险识别、紧急程度评估
- **健康状态判定工具**：规则推理、风险评估
- **病例理解工具**：概念归一化、结构化提取

**技术实现**：
- 知识库优先（主诉知识图谱、疾病知识图谱）
- DR.KNOWS路径检索与验证（Neo4j知识图谱）
- 路径约束推理（LLM在路径约束下推理）
- 医学概念标准化（CUI/ICD/SNOMED）

**输出**：DDx候选 + 证据结构 + 推理路径（写入CDP）

#### 通道2：语言与策略通道（决定"怎么说、怎么问" - 医生表达）

**主要工具**：
- **主动问诊工具**：生成问诊问题、自然语言对话
- **证据链工具**：生成解释和说明、推理路径可视化
- **治疗建议工具**：生成治疗方案的自然语言表达

**技术实现**：
- LLM（大语言模型）
- NLG（自然语言生成）
- 对话生成、解释生成
- 自然语言理解（NLU）

**输出**：自然语言问诊、解释、建议

**双通道协作机制**：

```
通道1工具（结构化推理）
    ↓
生成结构化结果（DDx、检查建议等）
    ↓
写入CDP（结构化数据）
    ↓
主Agent读取CDP
    ↓
通道2工具（语言与策略）
    ↓
读取CDP
    ↓
生成自然语言表达
    ↓
输出给用户
```

**关键原则**：
- **通道1决定"该往哪想"**：结构化推理工具负责临床逻辑推理，生成结构化的诊断、检查、治疗建议
- **通道2决定"怎么说"**：语言与策略工具负责将结构化结果转换为自然语言，生成问诊问题、解释说明
- **CDP作为桥梁**：通道1的输出写入CDP，通道2基于CDP生成自然语言表达
- **主Agent统一决策**：主Agent统一管理两个通道的工具调用，确保推理过程可追溯、可审计

### 1.3 系统架构图（服务部署架构）

```
┌─────────────────────────────────────────────────────────────────┐
│                        患者交互层                                  │
│  - 自然语言对话界面（文本/语音）                                   │
│  - 多模态输入（文本/图片/语音/视频）                              │
│  - 诊断结果展示（结构化+自然语言+可视化）                          │
│  - CDP可视化界面                                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    API网关层（Gateway）                           │
│  - 路由转发、认证授权、限流熔断                                  │
│  - 服务发现、负载均衡                                            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│              业务服务层（Business Services - Java）               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  诊断服务（diagnosis-service:8084）                   │  │
│  │  - CDP管理（创建、更新、版本控制）                     │  │
│  │  - 主Agent运行循环（Observe→Plan→Act→Update→Evaluate）│  │
│  │  - 诊断流程编排（5步AI循证诊断流程）                   │  │
│  │  - 诊断结果管理                                        │  │
│  │  - AgentState管理（阈值、预算、失败回退、已尝试工具）  │  │
│  │  - AuditTrail管理（工具调用、证据、写回字段、版本）   │  │
│  │  - 健康状态判定服务代理（调用Python服务）              │  │
│  │  - 健康筛查流程服务（A路径，A1-A5）                    │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  检查服务（examination-service:8085）                  │  │
│  │  - 检查方案设计                                        │  │
│  │  - 报告上传与识别                                      │  │
│  │  - 结果解读与分析                                      │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                AI服务层（AI Services - Python）                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  健康状态判定服务（health-state-assessment-service:8081）│  │
│  │   - AI诊断入口判定流程（Step 1-5）                    │  │
│  │   - 症状严重程度评估                                  │  │
│  │   - 风险早筛                                          │  │
│  │   - 分诊决策（决定工作态）                            │  │
│  │   - 健康管理计划生成                                  │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  通道1：结构化推理通道                                │  │
│  │  ├─ 病例理解服务（clinical-parsing-service:8082）      │  │
│  │  │   - 医学概念识别与归一化                            │  │
│  │  │   - 多模态理解（文本/影像/检查报告）                │  │
│  │  │   - 结构化提取                                      │  │
│  │  │   - 结构化问题清单构建                              │  │
│  │  ├─ 诊断引擎服务（diagnosis-engine-service:8086）      │  │
│  │  │   ├─ 诊断引擎（diagnosis-engine）                    │  │
│  │  │   │   ├─ 知识库候选检索（knowledge-base-retriever）  │  │
│  │  │   │   │   - 主诉知识图谱检索（Tier1/Tier2/Tier3）    │  │
│  │  │   │   │   - 疾病知识图谱检索（疾病详细信息）          │  │
│  │  │   │   ├─ DR.KNOWS路径检索（drknows-path-retriever） │  │
│  │  │   │   │   - 多跳推理路径检索（2-4跳）                │  │
│  │  │   │   │   - 路径结构特征分析（非数值评分）            │  │
│  │  │   │   │   - 路径验证（验证知识库候选）                │  │
│  │  │   │   ├─ 路径约束推理（path-constrained-reasoning） │  │
│  │  │   │   │   - 路径约束LLM推理                          │  │
│  │  │   │   │   - 推理结果验证（路径一致性/证据来源）      │  │
│  │  │   │   │   - 降级策略（验证失败时使用路径本身）        │  │
│  │  │   │   ├─ 三层分层分类器（three_layer_classifier）   │  │
│  │  │   │   │   - 基于知识库默认层级（Tier1/Tier2/Tier3）  │  │
│  │  │   │   │   ├─ 推理组织器（reasoning_organizer）       │  │
│  │  │   │   │   └─ 证据分析器（evidence_analyzer）        │  │
│  │  ├─ 检查建议服务（workup-planner-service:8090）        │  │
│  │  │   - 检查价值评估                                    │  │
│  │  │   - 信息增益计算                                    │  │
│  │  │   - 检查优先级排序                                  │  │
│  │  │   - 验证计划构建                                    │  │
│  │  ├─ 治疗推理服务（treatment-engine-service:8091）      │  │
│  │  │   - 治疗方案推理                                    │  │
│  │  │   - 药物推荐                                        │  │
│  │  ├─ 风险评估服务（risk-assessment-service:8092）       │  │
│  │  │   - 高危识别                                        │  │
│  │  │   - 紧急程度分级                                    │  │
│  │  │   - 复评与升级规则                                  │  │
│  │  │   - 终点结论包构建器（conclusion_package_builder） │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  通道2：语言与策略通道                                │  │
│  │  ├─ 对话管理服务（dialog-service:8088）                │  │
│  │  │   - 主动问诊引擎（信息缺口识别、智能追问）          │  │
│  │  │   - 自然语言理解（NLU）                             │  │
│  │  │   - 自然语言生成（NLG）                             │  │
│  │  │   - 对话上下文管理                                  │  │
│  │  │   - 概念归一化器（concept_normalizer）              │  │
│  │  │   - 信息缺口识别器（information_gap_identifier）     │  │
│  │  │   - 完整度计算器（completeness_calculator）          │  │
│  │  └─ 解释生成服务（explanation-service:8089）            │  │
│  │      - 证据链格式化                                    │  │
│  │      - 推理路径可视化                                  │  │
│  │      - 自然语言解释生成                                │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  OCR服务（ocr-service:8087）                           │  │
│  │  - 报告图片识别                                        │  │
│  │  - 结构化数据提取                                      │  │
│  │  - 多模态理解支持                                      │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  知识查询服务（knowledge-query-service:8093）          │  │
│  │  - 知识库优先查询（只读）                              │  │
│  │  - Neo4j路径检索验证                                   │  │
│  │  - 路径约束推理                                        │  │
│  │  - 版本化知识访问                                      │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│           知识演化与维护层（Knowledge Evolution Layer）          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  知识运维服务（knowledge-ops-service:8094）            │  │
│  │  ├─ Extractor Agent（抽取Agent）                      │  │
│  │  │   - 从知识源中抽取结构化知识                        │  │
│  │  │   - 生成KO草稿 + provenance                        │  │
│  │  ├─ Verifier Agent（验证Agent）                       │  │
│  │  │   - 证据一致性验证                                  │  │
│  │  │   - 冲突检测                                        │  │
│  │  ├─ Conflict Resolver Agent（冲突解决Agent）           │  │
│  │  │   - 解决知识冲突                                    │  │
│  │  │   - 冲突解决策略                                    │  │
│  │  ├─ Release Builder Agent（打包发布候选Agent）         │  │
│  │  │   - 打包candidate_release                          │  │
│  │  │   - 生成diff与测试计划                              │  │
│  │  ├─ Shadow Evaluator Agent（影子评测Agent）            │  │
│  │  │   - 离线环境评测                                    │  │
│  │  │   - 回归测试                                        │  │
│  │  ├─ Rollback & Drift Monitor Agent（回滚与漂移监控）  │  │
│  │  │   - 监控生产环境知识使用情况                        │  │
│  │  │   - 自动触发回滚                                    │  │
│  │  └─ Publish Gate（发布门禁）                          │  │
│  │      - Gate-1: 结构合法性检查                          │  │
│  │      - Gate-2: 证据可追溯性检查                        │  │
│  │      - Gate-3: 回归评测与影子评测                     │  │
│  │      - Gate-4: 风险分级审批                           │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                数据层（Data Layer）                              │
│  - MySQL 8.0（本地开发）/ Oracle（生产环境）                    │
│    （CDP存储、诊断记录、检查记录、健康状态判定记录等）          │
│  - Redis 7（缓存、会话状态、CDP临时状态、工具调用队列）         │
│  - Neo4j 5（知识图谱，DR.KNOWS核心依赖）                        │
│    ├─ Production（生产知识库，只读）                            │
│    ├─ Staging（候选验证区）                                    │
│    └─ Sandbox（实验/构建区）                                   │
└─────────────────────────────────────────────────────────────────┘
```

**服务架构设计说明**：

> **设计原则**：按照功能模块（工具）来组织服务，每个服务对应一个工具，职责单一明确。  
> **详细说明**：请参考《项目结构设计原则.md》

**服务拆分说明**：
- `diagnosis-engine-service`：仅负责鉴别诊断工具的核心功能
- `workup-planner-service`：独立服务，负责检查建议工具
- `treatment-engine-service`：独立服务，负责治疗建议工具
- `risk-assessment-service`：独立服务，负责风险评估工具

**拆分原因**：
1. **职责清晰**：每个服务只负责一个工具的功能，符合单一职责原则
2. **独立演进**：每个工具可以单独训练、评测、替换，服务可以独立升级
3. **易于维护**：功能模块边界清晰，便于团队协作和代码维护
4. **符合微服务原则**：每个服务是独立的业务能力单元，可以独立部署和扩展

**服务与工具的对应关系**：

| 服务 | 对应工具 | 工具ID | 主要职责 |
|------|---------|--------|---------|
| health-state-assessment-service | 健康状态判定工具 | tool_0 | 判断工作态、入口判定流程 |
| clinical-parsing-service | 病例理解工具 | tool_1 | 概念归一化、结构化提取 |
| dialog-service | 主动问诊工具 | tool_2 | 信息缺口识别、问诊生成 |
| diagnosis-engine-service | 鉴别诊断工具 | tool_3 | 多引擎融合诊断、DDx生成 |
| workup-planner-service | 检查建议工具 | tool_4 | 检查价值评估、验证计划 |
| treatment-engine-service | 治疗建议工具 | tool_5 | 治疗方案推理、药物推荐 |
| risk-assessment-service | 风险评估工具 | tool_6 | 高危识别、紧急程度评估 |
| explanation-service | 证据链工具 | tool_7 | 证据链构建、解释生成 |
| diagnosis-service | 主Agent（Clinical Agent Brain） | agent_main | 任务分配、流程编排、冲突解决、最终决策 |
| knowledge-query-service | 知识查询服务 | - | 知识库查询、路径检索、版本化知识访问 |
| knowledge-ops-service | 知识运维服务 | - | 知识抽取、验证、冲突处理、发布门禁 |

### 1.4 服务依赖关系与调用策略

#### 1.4.1 服务依赖图

**核心服务依赖关系**：

```
diagnosis-service (主Agent)
    ├─→ health-state-assessment-service (健康状态判定工具)
    │       └─→ [无依赖]
    │
    ├─→ clinical-parsing-service (病例理解工具)
    │       ├─→ ocr-service (OCR服务，可选)
    │       └─→ [Neo4j知识图谱，用于概念归一化]
    │
    ├─→ dialog-service (主动问诊工具)
    │       ├─→ clinical-parsing-service (获取结构化信息)
    │       └─→ diagnosis-engine-service (获取DDx信息)
    │
    ├─→ diagnosis-engine-service (鉴别诊断工具)
    │       ├─→ clinical-parsing-service (获取结构化病例)
    │       ├─→ risk-assessment-service (获取风险评估)
    │       └─→ [Neo4j知识图谱，用于路径推理]
    │
    ├─→ workup-planner-service (检查建议工具)
    │       ├─→ diagnosis-engine-service (获取DDx列表)
    │       └─→ risk-assessment-service (获取风险等级)
    │
    ├─→ treatment-engine-service (治疗建议工具)
    │       ├─→ diagnosis-engine-service (获取诊断结果)
    │       └─→ risk-assessment-service (获取风险等级)
    │
    ├─→ risk-assessment-service (风险评估工具)
    │       ├─→ diagnosis-engine-service (获取DDx列表)
    │       └─→ [无其他服务依赖]
    │
    └─→ explanation-service (证据链工具)
            ├─→ diagnosis-engine-service (获取推理路径)
            ├─→ workup-planner-service (获取检查建议)
            └─→ treatment-engine-service (获取治疗方案)
```

**数据流向**：

```
用户输入
    ↓
diagnosis-service (创建CDP，初始化AgentState)
    ↓
health-state-assessment-service (判定工作态)
    ↓
    ├─→ wellness_mode: 健康管理态流程
    │       └─→ [生成wellness_plan，结束]
    │
    └─→ clinical_mode: 临床诊疗态流程
            ↓
        clinical-parsing-service (结构化提取)
            ↓
        dialog-service (主动问诊)
            ↓
        diagnosis-engine-service (生成DDx)
            ├─→ risk-assessment-service (风险评估)
            └─→ workup-planner-service (检查建议)
                    ↓
                treatment-engine-service (治疗建议)
                    ↓
                explanation-service (生成解释)
                    ↓
                主Agent最终决策
                    ↓
                返回诊断结果
```

#### 1.4.2 同步/异步调用策略

**同步调用场景**（必须等待结果）：

1. **健康状态判定**：
   - `diagnosis-service` → `health-state-assessment-service`
   - 必须等待判定结果才能决定后续流程
   - 超时时间：5秒

2. **病例理解**：
   - `diagnosis-service` → `clinical-parsing-service`
   - 必须等待结构化结果才能进行诊断
   - 超时时间：10秒

3. **鉴别诊断**：
   - `diagnosis-service` → `diagnosis-engine-service`
   - 必须等待DDx结果才能进行后续处理
   - 超时时间：30秒

**异步调用场景**（不阻塞主流程）：

1. **检查建议生成**：
   - `diagnosis-service` → `workup-planner-service` (异步)
   - 可以在后台生成，通过回调或消息队列返回结果

2. **治疗建议生成**：
   - `diagnosis-service` → `treatment-engine-service` (异步)
   - 可以在后台生成，通过回调或消息队列返回结果

3. **证据链生成**：
   - `diagnosis-service` → `explanation-service` (异步)
   - 可以在后台生成，通过回调或消息队列返回结果

#### 1.4.3 数据一致性方案

**CDP数据一致性策略**：

1. **写时复制（Copy-on-Write）**：
   - 每次更新CDP时创建新版本
   - 旧版本保留用于审计和回滚
   - 版本号递增：v1, v2, v3, ...

2. **乐观锁机制**：
   - CDP包含版本号字段
   - 更新时检查版本号，防止并发冲突
   - 冲突时回退并重试

3. **最终一致性**：
   - 主数据存储在MySQL/Oracle（强一致性）
   - 缓存数据存储在Redis（最终一致性）
   - 缓存失效时间：5分钟

**AgentState一致性策略**：

1. **主Agent独占写入**：
   - 只有主Agent可以写入AgentState
   - 工具只能读取AgentState摘要（通过ToolContext）

2. **状态同步**：
   - AgentState存储在Redis（快速访问）
   - 定期持久化到MySQL/Oracle（长期存储）

**AuditTrail一致性策略**：

1. **追加写入**：
   - AuditTrail采用追加写入模式，不修改历史记录
   - 每个工具调用都追加一条审计记录

2. **实时持久化**：
   - AuditTrail实时持久化到MySQL/Oracle
   - 支持审计记录的查询和追溯

**服务调用超时和重试策略**：

| 服务 | 超时时间 | 重试次数 | 重试间隔 |
|------|---------|---------|---------|
| health-state-assessment-service | 5秒 | 2次 | 1秒 |
| clinical-parsing-service | 10秒 | 2次 | 2秒 |
| diagnosis-engine-service | 30秒 | 1次 | 5秒 |
| workup-planner-service | 15秒 | 1次 | 3秒 |
| treatment-engine-service | 15秒 | 1次 | 3秒 |
| risk-assessment-service | 10秒 | 2次 | 2秒 |
| explanation-service | 20秒 | 1次 | 3秒 |

---

## 二、核心对象与责任边界

### 2.1 CDP（病例事实唯一事实源）

> **对应功能设计文档**：1.3 核心数据结构：Clinical Decision Package (CDP)  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第1.3节

**CDP定位**：
- **病例事实唯一事实源**：CDP是系统中所有病例事实的唯一事实源，所有工具从CDP读取数据，建议写回CDP
- **结构化数据存储**：CDP以结构化JSON格式存储，支持版本管理和状态转换
- **不可变历史**：CDP采用写时复制机制，每次更新创建新版本，历史版本不可修改

**CDP字段路径规范**：

| 字段路径 | 数据类型 | 说明 | 更新工具 |
|---------|---------|------|---------|
| `cdp.id` | String | CDP唯一标识 | 系统生成 |
| `cdp.patient_id` | String | 患者ID | 系统生成 |
| `cdp.session_id` | String | 会话ID | 系统生成 |
| `cdp.version` | Integer | 版本号 | 系统管理 |
| `cdp.health_state_assessment` | JSON | 健康状态判定结果 | tool_0 |
| `cdp.health_state_assessment.work_mode` | String | 工作态（wellness_mode/clinical_mode） | tool_0 |
| `cdp.health_state_assessment.needs_clinical_mode` | Boolean | 是否需要临床诊疗态 | tool_0 |
| `cdp.health_state_assessment.risk_level` | String | 风险等级（L1/L2/L3/L4） | tool_0 |
| `cdp.wellness_plan` | JSON | 健康管理计划 | tool_0 |
| `cdp.patient_state` | JSON | 患者状态 | tool_1 |
| `cdp.patient_state.symptoms` | Array | 症状列表 | tool_1 |
| `cdp.patient_state.signs` | Array | 体征列表 | tool_1 |
| `cdp.patient_state.past_history` | Array | 既往史 | tool_1 |
| `cdp.patient_state.medications` | Array | 用药史 | tool_1 |
| `cdp.ddx` | JSON | 鉴别诊断列表 | tool_3 |
| `cdp.ddx.rank_list` | Array | 诊断排序列表 | tool_3 |
| `cdp.ddx.tier1_most_likely` | Array | 首要假设（Tier1） | tool_3 |
| `cdp.ddx.tier2_must_exclude` | Array | 必须排除（Tier2） | tool_3 |
| `cdp.ddx.tier3_active_alternatives` | Array | 积极备选（Tier3） | tool_3 |
| `cdp.evidence_graph` | JSON | 证据图 | tool_7 |
| `cdp.workup_plan` | JSON | 检查计划 | tool_4 |
| `cdp.management_plan` | JSON | 治疗计划 | tool_5 |
| `cdp.triage` | JSON | 风险评估 | tool_6 |
| `cdp.uncertainty` | JSON | 不确定性信息 | 多个工具 |
| `cdp.uncertainty.missing_critical_info` | Array | 缺失关键信息 | tool_2 |
| `cdp.audit` | JSON | 审计信息 | 系统管理 |

**CDP读取规则**：
- 工具通过ToolContext中的`cdp_reference`字段获取CDP引用
- 工具只能读取CDP字段，不能直接修改CDP
- 工具通过ToolResult中的`suggested_writes`字段建议主Agent写回CDP

**CDP写入规则**：
- 只有主Agent可以写入CDP
- 主Agent根据工具的`suggested_writes`决定是否写回CDP
- 每次写回CDP都创建新版本，记录到AuditTrail

### 2.2 AgentState（主Agent策略状态）

**AgentState定位**：
- **主Agent策略状态**：AgentState存储主Agent的策略状态，包括阈值、预算、失败回退、已尝试工具等
- **工具不可见**：工具不能直接访问AgentState，只能通过ToolContext获取AgentState摘要
- **主Agent独占写入**：只有主Agent可以写入AgentState

**AgentState字段结构**：

| 字段路径 | 数据类型 | 说明 |
|---------|---------|------|
| `agent_state.session_id` | String | 会话ID（与CDP关联） |
| `agent_state.current_step` | Integer | 当前诊断步骤（1-5） |
| `agent_state.work_mode` | String | 工作态（wellness_mode/clinical_mode） |
| `agent_state.thresholds` | JSON | 阈值配置 |
| `agent_state.thresholds.confidence_threshold` | Float | 置信度阈值（默认0.7） |
| `agent_state.thresholds.evidence_count_threshold` | Integer | 证据数量阈值（默认3） |
| `agent_state.budget` | JSON | 预算配置 |
| `agent_state.budget.max_tool_calls` | Integer | 最大工具调用次数（默认50） |
| `agent_state.budget.max_time_seconds` | Integer | 最大执行时间（秒，默认300） |
| `agent_state.budget.max_cost` | Float | 最大成本（默认100.0） |
| `agent_state.failure_backoff` | JSON | 失败回退策略 |
| `agent_state.failure_backoff.max_retries` | Integer | 最大重试次数（默认3） |
| `agent_state.failure_backoff.backoff_strategy` | String | 回退策略（exponential/linear） |
| `agent_state.tried_tools` | Array | 已尝试工具列表 |
| `agent_state.tried_tools[].tool_id` | String | 工具ID |
| `agent_state.tried_tools[].call_count` | Integer | 调用次数 |
| `agent_state.tried_tools[].last_result` | String | 上次结果（success/failure/timeout） |
| `agent_state.evidence_fusion_state` | JSON | 证据融合状态 |
| `agent_state.evidence_fusion_state.conflicts` | Array | 证据冲突列表 |
| `agent_state.evidence_fusion_state.resolution_strategy` | String | 冲突解决策略 |
| `agent_state.stop_conditions` | JSON | 停止条件状态 |
| `agent_state.stop_conditions.cdp_required_fields_complete` | Boolean | CDP必填项是否完成 |
| `agent_state.stop_conditions.evidence_references_complete` | Boolean | 证据引用是否齐全 |
| `agent_state.stop_conditions.risk_assessment_complete` | Boolean | 风险评估是否完成 |

**AgentState更新规则**：
- 主Agent在每次工具调用后更新AgentState
- 更新`tried_tools`、`current_step`、`evidence_fusion_state`等字段
- 更新操作记录到AuditTrail

**AgentState摘要（提供给工具）**：
- 工具通过ToolContext获取AgentState摘要，包括：
  - `current_step`：当前诊断步骤
  - `work_mode`：工作态
  - `constraints`：约束（成本/时间/风险）

### 2.3 Tools（能力模块）

**工具定位**：
- **无独立目标**：工具不拥有独立的诊断目标或治疗目标，只按主Agent调用执行
- **无长期策略状态**：工具不维护长期状态，每次调用都是独立的
- **结构化输出**：工具返回结构化的payload、evidence、quality、suggestedWrites
- **证据引用**：工具必须提供evidence引用，说明输出结果的依据来源

**工具输入依赖（从CDP读取）**：

每个工具必须明确声明从CDP读取哪些字段路径，例如：

| 工具ID | 工具名称 | 输入依赖（CDP字段路径） |
|-------|---------|---------------------|
| tool_0 | 健康状态判定工具 | `cdp.patient_state`（如有）、用户输入 |
| tool_1 | 病例理解工具 | 用户输入、病历文本、检查报告 |
| tool_2 | 主动问诊工具 | `cdp.ddx`、`cdp.uncertainty.missing_critical_info` |
| tool_3 | 鉴别诊断工具 | `cdp.patient_state`、`cdp.ddx`（如有） |
| tool_4 | 检查建议工具 | `cdp.ddx`、`cdp.triage` |
| tool_5 | 治疗建议工具 | `cdp.ddx`、`cdp.triage` |
| tool_6 | 风险评估工具 | `cdp.patient_state`、`cdp.ddx` |
| tool_7 | 证据链工具 | `cdp.ddx`、`cdp.evidence_graph`、`cdp.workup_plan`、`cdp.management_plan` |

**工具输出payload结构**：

每个工具必须明确声明输出payload的字段结构，例如：

| 工具ID | 工具名称 | 输出payload结构 |
|-------|---------|----------------|
| tool_0 | 健康状态判定工具 | `{work_mode, needs_clinical_mode, risk_level, assessment_reason}` |
| tool_1 | 病例理解工具 | `{symptoms[], signs[], past_history[], medications[]}` |
| tool_2 | 主动问诊工具 | `{next_question, question_reason, expected_info_gain}` |
| tool_3 | 鉴别诊断工具 | `{ddx_rank_list[], tier1_most_likely[], tier2_must_exclude[], tier3_active_alternatives[]}` |
| tool_4 | 检查建议工具 | `{workup_items[], verification_plans[]}` |
| tool_5 | 治疗建议工具 | `{management_plan, medication_suggestions[]}` |
| tool_6 | 风险评估工具 | `{risk_level, red_flags[], urgency_level}` |
| tool_7 | 证据链工具 | `{evidence_graph, explanation_text}` |

**工具evidence要求**：

每个工具必须提供evidence引用，说明输出结果的依据来源：

| 工具ID | 工具名称 | evidence要求 |
|-------|---------|-------------|
| tool_0 | 健康状态判定工具 | 规则依据、风险信号来源 |
| tool_1 | 病例理解工具 | 概念归一化来源（CUI/ICD）、提取依据 |
| tool_2 | 主动问诊工具 | 信息缺口识别依据、临床决策分析依据 |
| tool_3 | 鉴别诊断工具 | 知识库来源、路径来源、推理路径 |
| tool_4 | 检查建议工具 | 信息增益计算依据、验证计划依据 |
| tool_5 | 治疗建议工具 | 治疗方案依据、药物推荐依据 |
| tool_6 | 风险评估工具 | 风险识别依据、紧急程度评估依据 |
| tool_7 | 证据链工具 | 证据来源、推理路径来源 |

**工具suggestedWrites（建议写回CDP的字段路径）**：

每个工具通过`suggested_writes`字段建议主Agent写回CDP的字段路径：

| 工具ID | 工具名称 | suggestedWrites |
|-------|---------|----------------|
| tool_0 | 健康状态判定工具 | `cdp.health_state_assessment` |
| tool_1 | 病例理解工具 | `cdp.patient_state` |
| tool_2 | 主动问诊工具 | `cdp.uncertainty.missing_critical_info` |
| tool_3 | 鉴别诊断工具 | `cdp.ddx`、`cdp.evidence_graph` |
| tool_4 | 检查建议工具 | `cdp.workup_plan` |
| tool_5 | 治疗建议工具 | `cdp.management_plan` |
| tool_6 | 风险评估工具 | `cdp.triage` |
| tool_7 | 证据链工具 | `cdp.evidence_graph` |

**工具质量/失败模式与降级策略**：

每个工具必须声明质量指标、失败模式和降级策略：

| 工具ID | 工具名称 | 质量指标 | 失败模式 | 降级策略 |
|-------|---------|---------|---------|---------|
| tool_0 | 健康状态判定工具 | 判定准确率 | 超时、规则匹配失败 | 默认进入clinical_mode |
| tool_1 | 病例理解工具 | 概念归一化准确率 | 概念识别失败、归一化失败 | 保留原始文本，标记未归一化 |
| tool_2 | 主动问诊工具 | 问诊质量、信息增益 | 问题生成失败 | 使用模板问题 |
| tool_3 | 鉴别诊断工具 | DDx准确性 | 知识库检索失败、路径检索失败 | 使用路径本身作为推理结果 |
| tool_4 | 检查建议工具 | 检查建议合理性 | 信息增益计算失败 | 使用默认检查清单 |
| tool_5 | 治疗建议工具 | 治疗方案合理性 | 治疗方案推理失败 | 使用指南推荐方案 |
| tool_6 | 风险评估工具 | 风险识别召回率 | 风险识别失败 | 默认L3风险等级 |
| tool_7 | 证据链工具 | 证据链完整性 | 证据链构建失败 | 使用简化证据链 |

### 2.4 AuditTrail（审计轨迹）

**AuditTrail定位**：
- **必须记录**：所有工具调用、证据、写回字段、版本、时间必须记录到AuditTrail
- **不可修改**：AuditTrail采用追加写入模式，历史记录不可修改
- **可追溯**：支持审计记录的查询和追溯，用于问题定位和系统优化

**AuditTrail字段结构**：

| 字段路径 | 数据类型 | 说明 |
|---------|---------|------|
| `audit_trail.id` | String | 审计记录ID |
| `audit_trail.cdp_id` | String | CDP ID |
| `audit_trail.session_id` | String | 会话ID |
| `audit_trail.timestamp` | Timestamp | 时间戳 |
| `audit_trail.event_type` | String | 事件类型（tool_call/cdp_update/agent_decision） |
| `audit_trail.tool_call` | JSON | 工具调用记录（如event_type=tool_call） |
| `audit_trail.tool_call.tool_id` | String | 工具ID |
| `audit_trail.tool_call.tool_name` | String | 工具名称 |
| `audit_trail.tool_call.trace_id` | String | 追踪ID |
| `audit_trail.tool_call.input` | JSON | 输入（CDP字段路径引用） |
| `audit_trail.tool_call.output` | JSON | 输出（payload摘要） |
| `audit_trail.tool_call.evidence` | Array | 证据引用 |
| `audit_trail.tool_call.suggested_writes` | Array | 建议写回字段路径 |
| `audit_trail.tool_call.quality` | JSON | 质量指标 |
| `audit_trail.tool_call.errors` | Array | 错误信息 |
| `audit_trail.tool_call.duration_ms` | Integer | 执行时间（毫秒） |
| `audit_trail.cdp_update` | JSON | CDP更新记录（如event_type=cdp_update） |
| `audit_trail.cdp_update.from_version` | Integer | 源版本号 |
| `audit_trail.cdp_update.to_version` | Integer | 目标版本号 |
| `audit_trail.cdp_update.changed_fields` | Array | 变更字段路径 |
| `audit_trail.cdp_update.reason` | String | 更新原因 |
| `audit_trail.agent_decision` | JSON | 主Agent决策记录（如event_type=agent_decision） |
| `audit_trail.agent_decision.decision_type` | String | 决策类型（stop/escalate/refuse/continue） |
| `audit_trail.agent_decision.reason` | String | 决策原因 |
| `audit_trail.agent_decision.evidence_fusion` | JSON | 证据融合结果 |
| `audit_trail.agent_decision.conflict_resolution` | JSON | 冲突解决结果 |

**AuditTrail记录规则**：
- 每次工具调用都记录一条`tool_call`事件
- 每次CDP更新都记录一条`cdp_update`事件
- 每次主Agent决策都记录一条`agent_decision`事件
- 所有记录都包含完整的时间戳和追踪ID

**AuditTrail查询接口**：
- 支持按CDP ID查询所有审计记录
- 支持按工具ID查询工具调用记录
- 支持按时间范围查询审计记录
- 支持按事件类型查询审计记录

---

## 三、Tool协议与I/O契约

### 3.1 ToolContext（工具调用上下文）

**ToolContext定位**：
- **工具调用上下文**：ToolContext是主Agent调用工具时传递的上下文信息
- **工具输入来源**：工具通过ToolContext获取CDP引用、AgentState摘要、约束等信息
- **不可修改**：工具不能修改ToolContext，只能读取

**ToolContext字段结构**：

| 字段路径 | 数据类型 | 说明 |
|---------|---------|------|
| `tool_context.trace_id` | String | 追踪ID（用于审计和调试） |
| `tool_context.cdp_reference` | JSON | CDP引用 |
| `tool_context.cdp_reference.cdp_id` | String | CDP ID |
| `tool_context.cdp_reference.version` | Integer | CDP版本号 |
| `tool_context.cdp_reference.read_fields` | Array | 工具需要读取的CDP字段路径 |
| `tool_context.agent_state_summary` | JSON | AgentState摘要 |
| `tool_context.agent_state_summary.current_step` | Integer | 当前诊断步骤（1-5） |
| `tool_context.agent_state_summary.work_mode` | String | 工作态（wellness_mode/clinical_mode） |
| `tool_context.constraints` | JSON | 约束（成本/时间/风险） |
| `tool_context.constraints.max_time_seconds` | Integer | 最大执行时间（秒） |
| `tool_context.constraints.max_cost` | Float | 最大成本 |
| `tool_context.constraints.risk_level_limit` | String | 风险等级限制（L1/L2/L3/L4） |
| `tool_context.call_params` | JSON | 工具调用参数（工具特定） |

**ToolContext生成规则**：
- 主Agent在调用工具前生成ToolContext
- 根据工具的输入依赖（从CDP读取哪些字段路径）设置`cdp_reference.read_fields`
- 根据AgentState生成`agent_state_summary`
- 根据当前状态和工具特性设置`constraints`

### 3.2 ToolResult（工具返回结果）

**ToolResult定位**：
- **工具返回结果**：ToolResult是工具执行后返回给主Agent的结果
- **结构化输出**：ToolResult包含status、payload、evidence、quality、suggestedWrites、errors等字段
- **主Agent决策依据**：主Agent根据ToolResult决定是否写回CDP、是否继续调用其他工具

**ToolResult字段结构**：

| 字段路径 | 数据类型 | 说明 |
|---------|---------|------|
| `tool_result.trace_id` | String | 追踪ID（与ToolContext中的trace_id一致） |
| `tool_result.tool_id` | String | 工具ID |
| `tool_result.status` | String | 执行状态（success/partial_success/failure/timeout） |
| `tool_result.payload` | JSON | 输出payload（工具特定结构） |
| `tool_result.evidence` | Array | 证据引用 |
| `tool_result.evidence[].source` | String | 证据来源（knowledge_base/kg_path/rule/llm） |
| `tool_result.evidence[].reference` | String | 证据引用（CUI/路径ID/规则ID/LLM prompt） |
| `tool_result.evidence[].strength` | String | 证据强度（strong/medium/weak） |
| `tool_result.quality` | JSON | 质量指标 |
| `tool_result.quality.confidence` | Float | 置信度（0.0-1.0） |
| `tool_result.quality.completeness` | Float | 完整度（0.0-1.0） |
| `tool_result.quality.accuracy` | Float | 准确度（0.0-1.0，如可评估） |
| `tool_result.suggested_writes` | Array | 建议写回CDP的字段路径 |
| `tool_result.suggested_writes[].field_path` | String | 字段路径（如`cdp.ddx`） |
| `tool_result.suggested_writes[].value` | JSON | 字段值 |
| `tool_result.suggested_writes[].reason` | String | 写回原因 |
| `tool_result.errors` | Array | 错误信息 |
| `tool_result.errors[].error_type` | String | 错误类型（timeout/validation_error/runtime_error） |
| `tool_result.errors[].error_message` | String | 错误消息 |
| `tool_result.errors[].error_details` | JSON | 错误详情 |
| `tool_result.duration_ms` | Integer | 执行时间（毫秒） |
| `tool_result.metadata` | JSON | 元数据（工具特定） |

**ToolResult状态说明**：

| 状态 | 说明 | 主Agent处理策略 |
|------|------|----------------|
| `success` | 工具执行成功，输出完整 | 评估quality，决定是否写回CDP |
| `partial_success` | 工具执行部分成功，输出不完整 | 评估quality和errors，决定是否写回CDP或重试 |
| `failure` | 工具执行失败 | 根据错误类型决定是否重试或使用降级策略 |
| `timeout` | 工具执行超时 | 根据工具重要性决定是否重试或使用降级策略 |

### 3.3 工具分类

**工具分类依据**：
- **确定性**：工具输出的确定性程度
- **可追溯性**：工具输出是否可追溯到知识来源
- **可验证性**：工具输出是否可验证

**工具分类**：

#### 3.3.1 Deterministic（确定性工具）

**定义**：输出完全由输入决定，无随机性，可重复执行得到相同结果

**特点**：
- 输出确定性高
- 可追溯性强（可追溯到规则、知识库）
- 可验证性强（可通过规则验证）

**工具列表**：
- **tool_0（健康状态判定工具）**：基于规则和阈值，输出确定
- **tool_1（病例理解工具）**：概念归一化基于知识库，输出确定
- **tool_4（检查建议工具）**：基于信息增益计算，输出确定
- **tool_6（风险评估工具）**：基于规则和阈值，输出确定

**结论字段限制**：
- Deterministic工具可以直接产生诊断结论字段（如`cdp.ddx`）
- 但需要提供完整的evidence引用

#### 3.3.2 Retrieval（检索工具）

**定义**：从知识库或知识图谱中检索信息，输出基于检索结果

**特点**：
- 输出基于检索结果，确定性中等
- 可追溯性强（可追溯到知识库/知识图谱）
- 可验证性强（可通过检索结果验证）

**工具列表**：
- **tool_3（鉴别诊断工具）**：从知识库和知识图谱检索诊断候选

**结论字段限制**：
- Retrieval工具可以直接产生诊断结论字段（如`cdp.ddx`）
- 但需要提供完整的evidence引用（知识库来源、路径来源）

#### 3.3.3 Generative（生成工具）

**定义**：使用LLM生成输出，输出具有随机性，不可完全重复

**特点**：
- 输出具有随机性，确定性低
- 可追溯性中等（可追溯到LLM prompt和路径约束）
- 可验证性低（难以完全验证生成内容）

**工具列表**：
- **tool_2（主动问诊工具）**：使用LLM生成问诊问题
- **tool_5（治疗建议工具）**：使用LLM生成治疗方案的自然语言表达
- **tool_7（证据链工具）**：使用LLM生成解释和说明

**结论字段限制**：
- **Generative工具不能直接产生诊断结论字段**（如`cdp.ddx`、`cdp.workup_plan`的核心诊断字段）
- Generative工具只能产生：
  - 自然语言表达（问诊问题、解释说明）
  - 在受控证据基础上的建议（治疗方案的自然语言表达）
- 所有诊断结论必须由Deterministic或Retrieval工具产生

**Generative工具使用原则**：
- Generative工具的输出必须基于Deterministic或Retrieval工具的结构化结果
- Generative工具不能"自由联想"诊断结论，必须在路径约束下生成
- Generative工具的输出需要经过主Agent的验证和审核

---

## 四、主Agent运行循环与默认诊断路径

### 4.1 运行循环（Observe→Plan→Act→Update→Evaluate→Stop/Escalate）

**运行循环定位**：
- **主Agent核心循环**：主Agent通过运行循环不断观察CDP状态、规划工具调用、执行工具、更新CDP、评估结果、决定停止/升级/继续
- **自主决策**：主Agent在循环中自主决定调用哪些工具、调用顺序、调用参数
- **动态适应**：主Agent根据CDP状态和工具结果动态调整策略

**运行循环步骤**：

```
1. Observe（观察）
   - 读取当前CDP状态
   - 读取AgentState
   - 识别信息缺口
   - 识别证据冲突
   - 识别风险信号

2. Plan（规划）
   - 根据当前CDP状态和AgentState规划工具调用
   - 决定调用哪些工具、调用顺序、调用参数
   - 考虑约束（成本/时间/风险）
   - 考虑已尝试工具和失败回退策略

3. Act（执行）
   - 生成ToolContext
   - 调用工具（同步/异步）
   - 等待工具返回ToolResult
   - 记录到AuditTrail

4. Update（更新）
   - 评估ToolResult的quality和evidence
   - 进行evidence fusion和conflict resolution
   - 决定是否写回CDP（根据suggested_writes）
   - 更新AgentState（tried_tools、current_step等）
   - 记录到AuditTrail

5. Evaluate（评估）
   - 评估停止条件（StopCondition）
   - 评估升级条件（Escalation）
   - 评估拒答条件（Refusal）
   - 评估是否需要继续循环

6. Stop/Escalate/Continue（停止/升级/继续）
   - 如果满足停止条件 → Stop，输出终点结论包
   - 如果满足升级条件 → Escalate，执行升级策略
   - 如果满足拒答条件 → Refuse，输出拒答提示
   - 否则 → Continue，回到Observe步骤
```

**运行循环特点**：
- **可中断**：主Agent可以在任意步骤中断循环，执行升级或拒答
- **可回退**：主Agent可以根据证据冲突或症状演变回退到之前的步骤
- **可追溯**：所有循环步骤都记录到AuditTrail，支持问题定位和系统优化

### 4.2 默认诊断路径（Step1-5）

> **对应功能设计文档**：3.3 临床诊疗态详细流程（AI循证诊断流程）  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第3.3.1-3.3.6节

**默认诊断路径定位**：
- **默认策略路径**：Step1-5是主Agent的默认诊断路径，适用于大多数临床诊疗场景
- **可动态插入**：主Agent可以在默认路径中动态插入红旗优先、冲突复核、证据不足触发检索等策略
- **可回退**：主Agent可以根据证据冲突或症状演变回退到之前的步骤

**默认诊断路径步骤**：

```
Step 1: 识别问题
  - 主Agent调用 tool_1（病例理解工具）
    - 输入：用户输入、病历文本、检查报告
    - 输出：结构化患者状态（symptoms、signs、past_history等）
    - 写回：cdp.patient_state
  - 主Agent调用 tool_2（主动问诊工具）
    - 输入：cdp.patient_state、cdp.uncertainty.missing_critical_info
    - 输出：问诊问题、信息缺口识别
    - 写回：cdp.uncertainty.missing_critical_info
  - 主Agent评估：是否完成问题识别
    - 如果信息不足 → 继续问诊
    - 如果信息充足 → 进入Step 2

Step 2: 构建鉴别诊断候选集并分层
  - 主Agent调用 tool_3（鉴别诊断工具）
    - 输入：cdp.patient_state
    - 输出：DDx候选集、三层分层（Tier1/Tier2/Tier3）
    - 写回：cdp.ddx
  - 主Agent调用 tool_6（风险评估工具）
    - 输入：cdp.patient_state、cdp.ddx
    - 输出：风险等级、红旗信号
    - 写回：cdp.triage
  - 主Agent评估：是否完成候选集构建
    - 如果候选集为空 → 触发检索或升级
    - 如果候选集充足 → 进入Step 3

Step 3: 组织候选集并建立分流路径
  - 主Agent调用 tool_3（鉴别诊断工具）- 推理子组组织
    - 输入：cdp.ddx
    - 输出：推理子组、关键差异点、分流路径
    - 写回：cdp.ddx（补充推理子组信息）
  - 主Agent评估：是否完成分流路径建立
    - 如果分流路径不清晰 → 触发检索或升级
    - 如果分流路径清晰 → 进入Step 4

Step 4: 采集关键证据并形成排序与验证计划
  - 主Agent调用 tool_2（主动问诊工具）- 沿着分流路径采集证据
    - 输入：cdp.ddx、cdp.uncertainty.missing_critical_info
    - 输出：问诊问题、关键证据采集
    - 写回：cdp.patient_state（补充证据）
  - 主Agent调用 tool_3（鉴别诊断工具）- 固化三层排序
    - 输入：cdp.patient_state、cdp.ddx
    - 输出：更新后的DDx排序
    - 写回：cdp.ddx
  - 主Agent调用 tool_4（检查建议工具）- 制定验证计划
    - 输入：cdp.ddx、cdp.triage
    - 输出：检查建议、验证计划
    - 写回：cdp.workup_plan
  - 主Agent评估：是否完成证据采集和验证计划
    - 如果证据不足 → 继续采集或触发检索
    - 如果证据充足 → 进入Step 5

Step 5: 回填证据并输出终点结论包
  - 主Agent调用 tool_4（检查建议工具）- 回填检查结果
    - 输入：cdp.workup_plan、检查结果
    - 输出：更新后的检查建议
    - 写回：cdp.workup_plan
  - 主Agent调用 tool_3（鉴别诊断工具）- 更新三层排序
    - 输入：cdp.patient_state、cdp.ddx、cdp.workup_plan
    - 输出：最终DDx排序
    - 写回：cdp.ddx
  - 主Agent调用 tool_5（治疗建议工具）- 生成治疗方案
    - 输入：cdp.ddx、cdp.triage
    - 输出：治疗方案、药物推荐
    - 写回：cdp.management_plan
  - 主Agent调用 tool_7（证据链工具）- 生成终点结论包
    - 输入：cdp.ddx、cdp.evidence_graph、cdp.workup_plan、cdp.management_plan
    - 输出：证据链、解释说明、终点结论包（四要素）
    - 写回：cdp.evidence_graph、cdp.final_conclusion
  - 主Agent评估：是否满足停止条件
    - 如果满足 → Stop，输出终点结论包
    - 如果不满足 → 触发升级或拒答
```

### 4.3 动态插入策略

**动态插入策略定位**：
- **主Agent自主决策**：主Agent在运行循环中自主决定是否插入动态策略
- **优先级高于默认路径**：动态插入策略的优先级高于默认路径
- **可中断默认路径**：动态插入策略可以中断默认路径的执行

**动态插入策略类型**：

#### 4.3.1 红旗优先（Red Flag Priority）

**触发条件**：
- tool_6（风险评估工具）识别到红旗信号（L1/L2风险等级）
- tool_0（健康状态判定工具）识别到危险信号

**插入策略**：
- 立即中断当前步骤，优先处理红旗信号
- 调用 tool_6（风险评估工具）进行详细风险评估
- 如果风险等级为L1/L2，立即执行升级策略
- 如果风险等级为L3/L4，继续当前步骤，但提高优先级

**示例**：
```
Step 2执行中
  ↓
tool_6识别到L1风险（急性心梗可能）
  ↓
主Agent中断Step 2
  ↓
立即执行升级策略（建议立即就医）
  ↓
输出紧急提示
```

#### 4.3.2 冲突复核（Conflict Review）

**触发条件**：
- 主Agent在evidence fusion过程中识别到证据冲突
- 多个工具返回的结果不一致

**插入策略**：
- 立即中断当前步骤，进行冲突复核
- 调用相关工具重新评估
- 根据冲突解决策略（优先级规则、证据强度、专家投票）解决冲突
- 如果冲突无法解决，触发升级或拒答

**示例**：
```
Step 4执行中
  ↓
tool_3返回：诊断A概率0.8
tool_4返回：诊断A概率0.3（基于检查结果）
  ↓
主Agent识别到冲突
  ↓
中断Step 4，进行冲突复核
  ↓
调用 tool_6（风险评估工具）评估风险
  ↓
根据冲突解决策略（证据强度优先）选择诊断A概率0.5
  ↓
标记为"需要进一步验证"
  ↓
继续Step 4
```

#### 4.3.3 证据不足触发检索（Insufficient Evidence Trigger Retrieval）

**触发条件**：
- 主Agent评估发现证据不足（evidence_count < threshold）
- tool_2（主动问诊工具）无法识别到关键信息缺口
- tool_3（鉴别诊断工具）返回的候选集为空或不充分

**插入策略**：
- 立即触发检索策略
- 调用 tool_3（鉴别诊断工具）进行扩展检索
- 如果检索结果仍不足，触发升级或拒答

**示例**：
```
Step 2执行中
  ↓
tool_3返回：候选集为空
  ↓
主Agent识别到证据不足
  ↓
触发扩展检索
  ↓
调用 tool_3（鉴别诊断工具）- 扩展检索模式
  ↓
如果检索结果仍不足
  ↓
触发升级或拒答
```

---

## 五、停止条件、升级与拒答策略

### 5.1 停止条件（StopCondition）

**停止条件定位**：
- **主Agent停止标准**：主Agent根据停止条件判断是否停止运行循环，输出终点结论包
- **必须满足**：所有停止条件必须同时满足，才能停止
- **可配置**：停止条件可以通过AgentState配置

**停止条件列表**：

| 停止条件 | 字段路径 | 说明 |
|---------|---------|------|
| CDP必填项完成 | `agent_state.stop_conditions.cdp_required_fields_complete` | CDP的必填字段是否完成 |
| 证据引用齐全 | `agent_state.stop_conditions.evidence_references_complete` | 所有诊断结论是否有完整的evidence引用 |
| 风险评估完成 | `agent_state.stop_conditions.risk_assessment_complete` | 风险评估是否完成（tool_6已调用） |
| 诊断结论明确 | `cdp.ddx.tier1_most_likely`不为空 | 至少有一个首要假设 |
| 检查建议完成 | `cdp.workup_plan`不为空或明确不需要检查 | 检查建议已生成或明确不需要 |
| 治疗建议完成 | `cdp.management_plan`不为空 | 治疗建议已生成 |
| 证据链完整 | `cdp.evidence_graph`不为空 | 证据链已构建 |
| 终点结论包生成 | `cdp.final_conclusion`不为空 | 终点结论包已生成 |

**停止条件评估规则**：
- 主Agent在每次运行循环的Evaluate步骤中评估停止条件
- 如果所有停止条件都满足，主Agent停止运行循环，输出终点结论包
- 如果部分停止条件不满足，主Agent继续运行循环，优先满足未满足的条件

### 5.2 升级策略（Escalation）

> **对应功能设计文档**：2.6 风险评估与升级  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第2.6节

**升级策略定位**：
- **主Agent升级标准**：主Agent根据升级条件判断是否需要升级处理
- **优先级最高**：升级策略的优先级最高，可以中断任何步骤
- **安全第一**：升级策略遵循"宁可误报，不能漏报"的原则

**升级条件列表**：

| 升级条件 | 触发工具 | 说明 |
|---------|---------|------|
| 急危重（L1/L2风险等级） | tool_6（风险评估工具） | 风险等级为L1或L2，需要立即就医 |
| 超能力（超出系统诊断能力） | 主Agent评估 | 诊断候选集为空、证据不足且无法检索、工具连续失败 |
| 证据不足且风险高 | 主Agent评估 | 证据不足（evidence_count < threshold）且风险等级为L2以上 |

**升级策略执行**：

| 升级条件 | 升级动作 | 输出 |
|---------|---------|------|
| 急危重（L1/L2） | 立即中断，输出紧急提示 | "建议立即就医，可能存在急危重情况" |
| 超能力 | 中断，输出能力边界提示 | "当前情况超出系统诊断能力，建议咨询专业医生" |
| 证据不足且风险高 | 中断，输出风险提示 | "证据不足但存在高风险，建议进一步检查或咨询专业医生" |

**升级策略记录**：
- 所有升级策略执行都记录到AuditTrail
- 记录升级原因、触发条件、执行动作、输出内容

### 5.3 拒答策略（Refusal）

**拒答策略定位**：
- **主Agent拒答标准**：主Agent根据拒答条件判断是否无法安全推断，需要拒答
- **安全边界**：拒答策略定义了系统的安全边界，超出边界必须拒答
- **明确提示**：拒答时必须明确告知用户拒答原因和替代建议

**拒答条件列表**：

| 拒答条件 | 说明 |
|---------|------|
| 无法安全推断 | 诊断结论置信度低于阈值（confidence < 0.5）且无法通过工具提升 |
| 证据严重不足 | 证据数量少于最低要求（evidence_count < 2）且无法通过工具补充 |
| 工具连续失败 | 关键工具连续失败3次以上，无法继续诊断 |
| 超出诊断范围 | 诊断候选集为空，且检索和扩展检索都无法找到候选 |
| 风险等级未知 | 风险等级无法确定，且存在潜在高风险信号 |

**拒答策略执行**：

| 拒答条件 | 拒答动作 | 输出 |
|---------|---------|------|
| 无法安全推断 | 输出拒答提示 | "当前信息不足以做出安全诊断，建议咨询专业医生" |
| 证据严重不足 | 输出拒答提示 | "证据不足，无法完成诊断，建议提供更多信息或咨询专业医生" |
| 工具连续失败 | 输出拒答提示 | "系统暂时无法处理，建议稍后重试或咨询专业医生" |
| 超出诊断范围 | 输出拒答提示 | "当前情况超出系统诊断范围，建议咨询专业医生" |
| 风险等级未知 | 输出拒答提示 | "无法确定风险等级，建议咨询专业医生以排除潜在风险" |

**拒答策略记录**：
- 所有拒答策略执行都记录到AuditTrail
- 记录拒答原因、触发条件、执行动作、输出内容

---

## 六、工具调用调度层

### 6.1 消息队列定位

**消息队列定位**：
- **工具调用调度层**：消息队列是工具调用的调度层，负责并行、异步、超时、重试、限流
- **不拥有决策提交权**：消息队列不拥有决策提交权，主Agent仍是唯一提交者
- **技术实现层**：消息队列是技术实现层，不涉及业务逻辑

**消息队列职责**：

| 职责 | 说明 |
|------|------|
| 并行调用 | 支持多个工具并行调用，提高执行效率 |
| 异步调用 | 支持工具异步调用，不阻塞主Agent运行循环 |
| 超时控制 | 控制工具调用超时时间，防止工具长时间阻塞 |
| 重试机制 | 支持工具调用失败后的重试，提高系统可靠性 |
| 限流控制 | 控制工具调用频率，防止系统过载 |

**消息队列不负责**：
- 不负责决策逻辑（决策由主Agent负责）
- 不负责证据融合（证据融合由主Agent负责）
- 不负责冲突解决（冲突解决由主Agent负责）
- 不负责CDP写入（CDP写入由主Agent负责）

### 6.2 并行/异步/超时/重试/限流

**并行调用**：
- 主Agent可以同时调用多个独立工具（如tool_4和tool_5可以并行调用）
- 消息队列负责管理并行调用的执行和结果收集
- 并行调用的结果由主Agent统一处理和融合

**异步调用**：
- 主Agent可以异步调用非关键工具（如tool_7证据链工具）
- 消息队列负责管理异步调用的执行和回调
- 异步调用的结果通过回调返回给主Agent

**超时控制**：
- 每个工具都有配置的超时时间（见1.4.2节）
- 消息队列负责监控工具调用时间，超时后返回timeout状态
- 主Agent根据timeout状态决定是否重试或使用降级策略

**重试机制**：
- 工具调用失败后，消息队列根据重试策略自动重试
- 重试次数和重试间隔由AgentState配置
- 主Agent根据重试结果决定是否继续或使用降级策略

**限流控制**：
- 消息队列负责控制工具调用频率，防止系统过载
- 限流策略包括：每个工具的调用频率限制、总体调用频率限制
- 限流触发后，消息队列返回限流错误，主Agent根据错误决定是否等待或使用降级策略

---

## 七、知识演化与维护子系统

### 7.1 设计概述

**核心思想**：
- **双层决策架构**：诊断域（主Agent）与知识域（知识演化Agent集群）分离
- **权限分离**：Read（生产知识）、Write（候选区）、Publish（发布门禁）、Rollback（回滚控制）
- **版本化管理**：类似CDP的写时复制思想，支持知识版本追溯和回滚

**设计原则**：
1. **诊断决策单点负责**：主Agent仍是诊断与临床动作的唯一责任主体
2. **知识演化多点自治**：知识演化Agent集群可以自动运行，最大化研究空间
3. **发布单点门禁**：知识演化不能直接改生产知识，必须通过Publish Gate
4. **可追溯性**：所有知识变更可追溯，支持审计和回滚

### 7.2 架构分层

#### 7.2.1 在线层：Knowledge Query（只读）

**定位**：服务于通道1结构化推理

**功能**：
- 知识库优先查询
- Neo4j路径检索验证
- 路径约束推理

**实现方式**：
- 新增工具服务：`knowledge-query-service`
- 或作为 `diagnosis-engine-service` 的可插拔retriever模块

**特点**：
- 只读访问生产知识库
- 不参与知识演化流程
- 响应主Agent的工具调用请求

#### 7.2.2 离线层：Knowledge Evolution（写候选/跑评测/提发布）

**定位**：知识演化的核心子系统

**功能**：
- 知识抽取
- 知识验证
- 冲突处理
- 候选构建
- 回归评测
- 发布提议

**实现方式**：
- 新增独立子系统：`knowledge-ops-service`
- 内部由多个"知识维护Agent"组成
- 通过消息队列异步运行（复用现有调度层能力）

**特点**：
- 异步运行，不阻塞在线诊断流程
- 工作在候选区，不直接修改生产知识
- 通过Publish Gate控制发布

### 7.3 三个知识库区

#### 7.3.1 Sandbox（实验/构建区）

**定位**：知识演化的起点，实验和构建区域

**功能**：
- 抽取Agent、解析Agent在这里生成结构化知识对象（KO）
- 与证据绑定
- 不影响任何线上推理

**特点**：
- 完全隔离，不影响生产环境
- 支持快速迭代和实验
- 可以随时清理和重建

#### 7.3.2 Staging（候选验证区）

**定位**：知识验证和候选构建区域

**功能**：
- 验证Agent工作：证据一致性、冲突检测
- 冲突处理Agent工作：解决知识冲突
- 回归评测Agent工作：影子评测
- 产出"候选release（candidate_release）"

**特点**：
- 通过验证的知识才能进入Staging
- 支持完整的验证和测试流程
- 生成候选发布包

#### 7.3.3 Production（线上只读发布区）

**定位**：生产环境的知识库，只读访问

**功能**：
- 只有通过Publish Gate的release才进入Production
- 生成 `kg_version`（v1, v2, v3...）
- 支持版本化管理和回滚

**特点**：
- 只读访问，不直接修改
- 版本化管理，支持回滚
- 对应CDP的写时复制思想

**数据流向**：
```
Sandbox（KO草稿）
  ↓ 验证通过
Staging（candidate_release）
  ↓ 通过Publish Gate
Production（kg_version）
  ↓
在线推理使用
```

### 7.4 知识演化Agent集群

**Agent职责划分**：

| Agent | 职责 | 工作区域 |
|-------|------|---------|
| Extractor Agent | 从知识源中抽取结构化知识 | Sandbox |
| Verifier Agent | 验证知识的正确性和一致性 | Sandbox → Staging |
| Conflict Resolver Agent | 解决知识冲突 | Staging |
| Release Builder Agent | 打包发布候选 | Staging |
| Shadow Evaluator Agent | 在离线环境进行评测 | Staging |
| Rollback & Drift Monitor Agent | 监控生产环境的知识使用情况 | Production |

**Agent协作流程**：
```
新知识源
  ↓
Extractor Agent（抽取）→ Sandbox
  ↓
Verifier Agent（验证）→ Staging
  ↓
Conflict Resolver Agent（冲突解决）→ Staging
  ↓
Release Builder Agent（打包）→ candidate_release
  ↓
Shadow Evaluator Agent（影子评测）→ 评测报告
  ↓
Publish Gate（门禁）→ Production
  ↓
Rollback & Drift Monitor Agent（监控）→ 持续监控
```

### 7.5 Publish Gate：发布门禁机制

**定位**：非LLM、规则/测试驱动的发布控制器

**四道门禁**：

1. **Gate-1：结构合法性**
   - 检查KO schema完整
   - concept标准化字段齐全
   - 避免脏数据

2. **Gate-2：证据可追溯性**
   - 每条KO必须绑定provenance
   - 没有证据不允许进入发布候选

3. **Gate-3：回归评测与影子评测通过**

**检查内容**：
- 必须跑最小回归集
- 核心DDx场景测试
- 红旗病种测试
- 关键路径断裂测试

**测试契约（Test Contract）**：

**必跑测试集**：
1. **固定小集（Core Regression Set）**：
   - 核心DDx场景：50个标准病例（覆盖前10大常见疾病）
   - 红旗病种：20个急危重病例（急性心肌梗死、脑卒中、肺栓塞等）
   - 关键路径：30个关键诊疗路径（覆盖主要诊疗流程）
   - **总计：100个固定测试用例**

2. **抽样大集（Sampling Set）**：
   - 从历史病例库中随机抽样500个病例
   - 覆盖不同病种、不同严重程度、不同人群
   - 每次发布都重新抽样，确保覆盖全面

**硬阈值（Hard Thresholds）**：
- **红旗召回率**：不得下降（与基线版本对比，红旗病种召回率 ≥ 基线）
- **关键路径断裂**：必须为0（关键路径断裂数 = 0）
- **总体性能下降**：不超过5%（总体诊断准确率下降 ≤ 5%）
- **新增错误**：不超过2%（新增错误病例数 / 总测试病例数 ≤ 2%）

**软阈值（Soft Thresholds）**：
- **平均响应时间**：增加不超过10%
- **证据完整性**：证据链完整性得分 ≥ 基线 - 3%
- **路径可追溯性**：路径可追溯性得分 ≥ 基线 - 5%

**评测输出要求**：
- **评测报告**：必须生成完整的评测报告
- **报告摘要hash**：评测报告的hash值必须记录到candidate_release中
- **审计记录**：评测过程必须记录到AuditTrail
- **对比基线**：必须与当前生产版本（基线）进行对比

**评测报告结构**：
```json
{
  "evaluation_id": "EVAL_20260128_001",
  "candidate_release_id": "RELEASE_20260128_001",
  "baseline_version": "v2.0",
  "test_sets": {
    "core_regression": {
      "total_cases": 100,
      "passed": 98,
      "failed": 2,
      "pass_rate": 0.98
    },
    "sampling_set": {
      "total_cases": 500,
      "passed": 485,
      "failed": 15,
      "pass_rate": 0.97
    }
  },
  "metrics": {
    "red_flag_recall": {
      "baseline": 0.95,
      "candidate": 0.96,
      "delta": 0.01,
      "threshold_met": true
    },
    "critical_path_breaks": {
      "count": 0,
      "threshold_met": true
    },
    "overall_accuracy": {
      "baseline": 0.92,
      "candidate": 0.91,
      "delta": -0.01,
      "threshold_met": true
    },
    "new_errors": {
      "count": 10,
      "rate": 0.0167,
      "threshold_met": true
    }
  },
  "report_hash": "sha256:abc123...",
  "evaluation_timestamp": "2026-01-28T10:00:00Z",
  "evaluator": "ShadowEvaluatorAgent_v1.0"
}
```

**通过条件（明确契约）**：
1. **所有硬阈值必须满足**：
   - 红旗召回率 ≥ 基线
   - 关键路径断裂 = 0
   - 总体性能下降 ≤ 5%
   - 新增错误率 ≤ 2%

2. **至少80%的软阈值满足**：
   - 响应时间、证据完整性、路径可追溯性等

3. **评测报告必须完整**：
   - 报告hash已记录
   - 所有测试用例都有结果
   - 对比基线数据完整

4. **评测结果必须入审计**：
   - candidate_release的评测报告hash已记录
   - 评测过程已记录到AuditTrail

**不通过处理**：
- 如果硬阈值不满足：直接拒绝，不允许进入Gate-4
- 如果软阈值不满足：标记为"需要人工审核"，进入Gate-4但需要人工审批
- 如果评测报告不完整：拒绝，要求重新评测

**与主Agent的"停止条件必须证据链完整"思想一致**：
- Gate-3的评测也要求证据链完整
- 评测报告必须包含证据链完整性评估
- 证据链不完整的测试用例视为失败

4. **Gate-4：风险分级审批（可选人工）**
   - 低风险：自动发布
   - 中风险：需要人工审批或双人签署
   - 高风险：必须人工审批+灰度发布

**门禁决策流程**：
```
candidate_release
  ↓
Gate-1: 结构合法性检查
  ↓ (通过)
Gate-2: 证据可追溯性检查
  ↓ (通过)
Gate-3: 回归评测与影子评测
  ↓ (通过)
Gate-4: 风险分级审批
  ↓ (通过)
生成 kg_version
  ↓
发布到 Production
```

### 7.6 与现有系统的集成

#### 7.6.1 与主Agent的集成

**主Agent职责不变**：
- 诊断与临床动作的唯一责任主体仍是主Agent
- 主Agent运行循环不变：Observe→Plan→Act→Update→Evaluate→Stop/Escalate
- 主Agent负责证据融合和冲突解决

**新增能力**：
- 主Agent可以触发知识演化提案（异步）
- 主Agent使用版本化知识进行推理
- 主Agent在CDP中记录知识引用

#### 7.6.2 与工具服务的集成

**工具服务职责不变**：
- 工具仍然无独立目标、无长期策略状态
- 工具只按主Agent调用执行并返回结构化结果
- 工具必须提供evidence引用和suggestedWrites

**新增能力**：
- 工具可以访问版本化知识
- 工具的evidence引用包含kg_version和ko_id
- 工具通过knowledge-query-service访问知识

#### 7.6.3 与CDP的集成

**CDP职责不变**：
- CDP仍然是病例事实的唯一事实源
- 所有工具从CDP读取，建议写回CDP
- CDP支持版本控制、回放、回退

**新增能力**：
- CDP记录知识引用（knowledge_refs）
- CDP的audit包含kg_version
- CDP支持知识版本追溯

### 7.7 知识演化触发机制

#### 7.7.1 在线触发：只产生Proposal

**触发场景**：
- tool_3鉴别诊断候选为空/很弱（知识覆盖缺口）
- 冲突复核反复失败（图谱路径无法验证、证据互斥）
- 反复"证据不足"仍无法满足停止条件中的"证据链完整"

**处理方式**：
- 主Agent在Evaluate阶段可决定：向knowledge-ops提交 `proposal`（异步）
- **不阻塞当前会话闭环**
- 只产生提案，不直接更新生产知识

#### 7.7.3 去噪/节流机制

**问题**：真实运行中会出现"提案风暴"（同类缺口重复触发）

**解决方案**：

**1. dedupe_key（去重键）**

**生成规则**：
- 按病种/概念/缺口类型聚合
- 格式：`{gap_type}_{disease_concept_id}_{symptom_concept_id}`
- 示例：
  - `knowledge_gap_ICD_I20_0_SYMP_001`：心绞痛的知识缺口
  - `conflict_resolution_failed_ICD_I21_0`：急性心肌梗死冲突解决失败
  - `evidence_insufficient_ICD_J18_0`：肺炎证据不足

**去重逻辑**：
- 相同`dedupe_key`的proposal在`cooldown_window`内只保留第一个
- 后续相同key的proposal会被合并或丢弃
- 合并时更新`trigger_count`（触发次数）和`latest_evidence_snapshot`

**2. cooldown_window（冷却时间窗口）**

**配置规则**：
- 默认冷却时间：3600秒（1小时）
- 可根据gap_type调整：
  - 知识缺口：3600秒（1小时）
  - 冲突解决失败：7200秒（2小时）
  - 证据不足：1800秒（30分钟）

**冷却逻辑**：
- 在冷却窗口内，相同`dedupe_key`的proposal会被拒绝
- 冷却窗口外，允许新的proposal
- 冷却窗口可以动态调整（根据触发频率）

**3. evidence_snapshot（证据快照）**

**快照内容**：
- 触发时的CDP状态（关键字段）
- 触发时的工具调用结果
- 触发时的知识查询结果
- 触发时的错误信息

**快照格式**：
```json
{
  "evidence_snapshot": {
    "snapshot_hash": "sha256:abc123...",
    "cdp_snapshot": {
      "patient_state": {...},
      "ddx": [...],
      "evidence_graph": {...}
    },
    "tool_results": [
      {
        "tool_id": "tool_3",
        "result": {...},
        "error": "knowledge_gap: no candidate found"
      }
    ],
    "knowledge_queries": [
      {
        "query": "...",
        "result_count": 0
      }
    ],
    "trigger_context": {
      "session_id": "...",
      "timestamp": "2026-01-28T10:00:00Z",
      "agent_state": {...}
    }
  }
}
```

**作用**：
- **便于复现**：可以复现触发场景，用于知识演化Agent分析
- **问题诊断**：可以分析为什么触发，是否误触发
- **审计追溯**：记录触发时的完整上下文

**4. 节流策略**

**触发频率限制**：
- 单个`dedupe_key`：每小时最多触发1次
- 单个会话：每小时最多触发5次proposal
- 全局：每小时最多触发100个proposal

**超出限制处理**：
- 超出限制的proposal会被标记为"节流"
- 记录到日志，但不提交到knowledge-ops
- 可以人工查看被节流的proposal

**5. Proposal合并策略**

**合并条件**：
- 相同`dedupe_key`
- 在`cooldown_window`内
- 触发原因相同或相似

**合并方式**：
- 保留第一个proposal
- 更新`trigger_count`（触发次数）
- 更新`latest_evidence_snapshot`（最新证据快照）
- 合并`diff`（如果有多个KO变更）

**合并后的Proposal结构**：
```json
{
  "proposal_id": "PROP_20260128_001",
  "dedupe_key": "knowledge_gap_ICD_I20_0_SYMP_001",
  "trigger": "知识覆盖缺口",
  "trigger_count": 5,
  "first_trigger_time": "2026-01-28T09:00:00Z",
  "latest_trigger_time": "2026-01-28T10:00:00Z",
  "cooldown_window": 3600,
  "evidence_snapshot": {
    "snapshot_hash": "sha256:abc123...",
    "first_snapshot": {...},
    "latest_snapshot": {...}
  },
  "diff": {...},
  "risk_level": "medium"
}
```

#### 7.7.2 离线触发：周期性扫描

**触发场景**：
- 新指南/新共识发布
- 内部规则变更
- 线上监控发现某类case回归，触发"修复提案"

**处理方式**：
- 定时任务扫描知识源
- 检测到更新后触发知识演化流程
- 自动生成提案

### 7.8 版本化管理与回滚

**版本生成**：
- 每次发布生成 `kg_version`（v1, v2, v3...）
- 对应CDP的写时复制思想
- 新版本、旧版本保留用于审计回滚

**回滚机制**：

**1. 全局回滚**

**实现方式**：
- 把默认kg_version指针回退（通过修改current_release指针）
- 只需修改`current_release`指针，指向旧release
- 回滚速度快（O(1)），不需要重建数据

**2. 局部回滚（基于downstream_bindings的精确回滚）**

**依赖分析流程**：

**步骤1：识别受影响组件**
- 根据KO的`downstream_bindings`字段，识别所有依赖该KO的组件
- 包括：工具、规则引擎、路径模板等

**步骤2：影响范围评估**
- 评估回滚对每个组件的影响
- 评估是否需要更新组件配置
- 评估是否需要通知组件重新加载

**步骤3：回滚执行**
- 在新release中标记KO为deprecated
- 查询时fallback到旧release的KO
- 通知相关组件（通过downstream_bindings）
- 更新组件配置（如果需要）

**步骤4：回滚验证**
- 验证回滚后的KO是否可用
- 验证相关组件是否正常工作
- 验证诊断流程是否恢复正常

**局部回滚示例**：
```cypher
// 1. 识别需要回滚的KO
MATCH (ko:KnowledgeObject {ko_id: "KO_001"})
WHERE ko.release_id = "v2.1"

// 2. 获取下游绑定
WITH ko, ko.downstream_bindings as bindings

// 3. 标记为deprecated
SET ko.status = "deprecated"
SET ko.deprecated_at = timestamp()
SET ko.deprecated_reason = "性能回归"

// 4. 通知相关组件（通过bindings）
// - tool_3: 需要重新加载知识
// - pathway_acute_mi: 需要更新路径配置
```

**回滚特点**：
- **可追溯**：回滚操作记录到AuditTrail
- **支持审计**：回滚原因、影响范围都有记录
- **不影响其他版本**：只影响当前版本
- **精确控制**：基于downstream_bindings实现精确回滚

**灰度发布**：
- 按会话分流
- 按机构分流
- 按病种分流

---

## 相关文档

- [AI医生系统-技术架构设计-工具清单](./AI医生系统-技术架构设计-工具清单.md)
- [AI医生系统-技术架构设计-核心技术组件](./AI医生系统-技术架构设计-核心技术组件.md)
- [AI医生系统-技术架构设计-CDP数据与状态管理](./AI医生系统-技术架构设计-CDP数据与状态管理.md)
- [AI医生系统-技术架构设计-索引](./AI医生系统-技术架构设计-索引.md)
- [知识演化与知识维护-完整设计方案](../../3.知识内容提取/知识库结构设计/知识演化与维护/知识演化与知识维护-完整设计方案.md)

