# AI医生系统 - 技术架构设计

> **文档定位**：本文档是AI医生系统的**技术架构设计**，采用多智能体架构实现八大临床脑区的技术支撑，专注于技术实现、系统架构、服务设计、技术选型等技术层面的设计。  
> **业务功能**：请参考《AI医生系统-系统功能设计.md》  
> **核心目标**：基于DR.KNOWS论文的研究成果，采用多智能体协作架构，设计双通道推理架构，实现八大临床脑区的技术支撑，支持健康管理态和临床诊疗态两种工作态。

---

## 📖 术语表（Glossary）

> **说明**：本文档中使用的关键术语定义，便于快速理解核心概念。

| 术语 | 英文 | 定义 | 首次出现章节 |
|------|------|------|------------|
| **智能体** | Agent | 具备自主感知、推理、决策和执行能力的计算单元，对应一个临床脑区 | 1.2 |
| **协调器** | Orchestrator | 负责任务分配、流程编排、冲突解决的智能体协调器 | 1.2 |
| **双通道推理** | Dual-Channel Reasoning | 通道1（结构化推理）决定"该往哪想"，通道2（语言策略）决定"怎么说、怎么问" | 1.1 |
| **CDP** | Clinical Decision Package | 临床决策包，系统内部一切推理的核心数据结构 | 6.1 |
| **DDx** | Differential Diagnosis | 鉴别诊断列表 | 4.3 |
| **知识图谱** | Knowledge Graph | 基于Neo4j的医学知识图谱，用于DR.KNOWS路径推理 | 3.1 |
| **多引擎融合** | Multi-Engine Fusion | 融合规则引擎、知识图谱引擎、统计模型引擎、大模型引擎、鉴别诊断引擎的诊断方法 | 3.2 |
| **消息队列** | Message Queue | 基于Redis的智能体间消息传递机制 | 2.2.3 |
| **Saga模式** | Saga Pattern | 用于管理分布式长事务的模式 | 1.4.3 |
| **乐观锁** | Optimistic Locking | 通过版本号检查防止并发冲突的锁机制 | 1.4.3 |
| **写时复制** | Copy-on-Write | 每次更新CDP时创建新版本的机制 | 1.4.3 |
| **服务拆分** | Service Decomposition | 按照功能模块（脑区）来组织服务，每个服务对应一个脑区 | 1.3 |
| **同步调用** | Synchronous Call | 必须等待结果的调用方式 | 1.4.2 |
| **异步调用** | Asynchronous Call | 不阻塞主流程的调用方式 | 1.4.2 |
| **工作态** | Work Mode | 系统的工作模式：健康管理态（wellness_mode）和临床诊疗态（clinical_mode） | 4.0 |
| **三层分层** | Three-Layer Classification | 首要假设、主要备选诊断、必须排除的高危诊断 | 3.2.2 |
| **推理子组** | Reasoning Subgroup | 按系统分类、病理生理机制等维度组织的诊断候选组 | 3.2.2 |
| **分流路径** | Triage Path | 基于关键差异点形成的可执行的分流路径 | 3.2.3 |
| **验证计划** | Verification Plan | 针对诊断方向制定的验证计划 | 3.4.2 |
| **回退机制** | Rollback Mechanism | 当出现证据冲突、症状演变或处理无效时的回退机制 | 6.5 |
| **终点结论包** | Conclusion Package | 诊断流程的最终输出，包含四要素 | 6.7 |
| **CUI** | Concept Unique Identifier | UMLS中的医学概念编码 | 4.1 |
| **Neo4j** | - | 图数据库，用于存储和查询知识图谱 | 1.3 |
| **Redis** | - | 内存数据库，用于缓存和消息队列 | 1.3 |

---

## 📋 文档对应关系映射表

> **说明**：本表列出了技术架构文档与功能设计文档的对应关系，便于快速定位相关功能定义。

| 技术架构文档章节 | 功能设计文档章节 | 对应关系说明 |
|----------------|----------------|------------|
| **一、整体架构设计** | | |
| 1.1 双通道推理架构 | 1.2 核心设计原则 | 技术架构 → 设计理念 |
| 1.2 多智能体系统架构 | 二、八大临床脑区功能设计 | 技术实现 → 功能定义 |
| 1.3 系统架构图（服务部署） | 二、八大临床脑区功能设计 | 服务拆分 → 脑区划分 |
| 1.4 服务依赖关系 | 三、完整临床流程设计 | 服务调用 → 流程设计 |
| **二、多智能体系统设计** | | |
| 2.1-2.4 智能体基础架构 | 二、八大临床脑区功能设计 | 智能体设计 → 脑区功能 |
| **三、核心技术组件设计** | | |
| 3.1 知识图谱推理引擎 | 2.3 脑区C：鉴别诊断引擎 | 技术实现 → 功能定义 |
| 3.2 多引擎融合诊断系统 | 2.3 脑区C：鉴别诊断引擎 | 技术实现 → 功能定义 |
| 3.3 5步AI循证诊断流程编排 | 3.3 临床诊疗态详细流程 | 流程编排 → 流程定义 |
| 3.4 检查建议引擎 | 2.4 脑区D：检查/检验建议 | 技术实现 → 功能定义 |
| 3.5 对话管理服务 | 2.2 脑区B：主动问诊 | 技术实现 → 功能定义 |
| **四、八大智能体详细设计** | | |
| 4.0 智能体0：健康状态判定 | 2.0 脑区0：健康状态判定 | 智能体实现 → 功能定义 |
| 4.1 智能体1：病例理解 | 2.1 脑区A：病例理解 | 智能体实现 → 功能定义 |
| 4.2 智能体2：主动问诊 | 2.2 脑区B：主动问诊 | 智能体实现 → 功能定义 |
| 4.3 智能体3：鉴别诊断 | 2.3 脑区C：鉴别诊断 | 智能体实现 → 功能定义 |
| 4.4 智能体4：检查建议 | 2.4 脑区D：检查建议 | 智能体实现 → 功能定义 |
| 4.5 智能体5：治疗建议 | 2.5 脑区E：治疗建议 | 智能体实现 → 功能定义 |
| 4.6 智能体6：风险评估 | 2.6 脑区F：风险评估 | 智能体实现 → 功能定义 |
| 4.7 智能体7：证据链 | 2.7 脑区G：证据链 | 智能体实现 → 功能定义 |
| **五、多智能体协作流程** | | |
| 5.1 健康状态判定 → 分叉 | 3.1 健康状态判定 → 分叉 | 协作流程 → 流程定义 |
| 5.2 健康管理态协作流程 | 3.2 健康管理态详细流程 | 协作流程 → 流程定义 |
| 5.3 临床诊疗态协作流程 | 3.3 临床诊疗态详细流程 | 协作流程 → 流程定义 |
| **六、CDP数据结构设计** | | |
| 6.1 CDP数据模型 | 1.3 CDP数据结构 | 数据库设计 → 数据结构定义 |
| 6.2 CDP API设计 | 四、CDP流转与状态管理 | API设计 → 状态管理 |
| 6.3 证据清单结构化存储 | 2.7.1 证据清单结构化 | 存储设计 → 功能定义 |
| 6.4 回填与重排规则引擎 | 3.4.9 回填与重排规则 | 技术实现 → 功能定义 |
| 6.5 回退机制引擎 | 3.4.7 回退机制 | 技术实现 → 功能定义 |
| 6.6 复评与升级规则引擎 | 2.6.1 复评与升级规则库 | 技术实现 → 功能定义 |
| 6.7 终点结论包生成引擎 | 3.5 终点结论包四要素 | 技术实现 → 输出标准 |
| **七、CDP流转与状态管理** | | |
| 7.1 CDP生命周期 | 4.1 CDP生命周期 | 技术实现 → 概念定义 |
| 7.2 CDP版本管理 | 4.2 CDP版本管理 | 技术实现 → 概念定义 |
| **八、技术栈选型** | | |
| 9.1-9.3 技术栈 | - | 技术选型（功能设计文档中无对应） |

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
│  │  多引擎融合诊断                                         │  │
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

### 1.2 多智能体系统架构（核心架构）

> **对应功能设计文档**：二、八大临床脑区功能设计  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第2.0-2.7节  
> **核心设计理念**：将八大临床脑区重构为多智能体系统，每个智能体具备自主决策能力，通过协作完成完整的医学推理和决策支持。

**多智能体架构核心理念**：

1. **智能体自主性**：每个智能体具备自主感知、推理、决策和执行能力
2. **智能体协作**：智能体通过消息传递、协商、投票等方式协作
3. **智能体协调**：智能体协调器（Orchestrator）负责任务分配和流程编排
4. **冲突解决**：当智能体产生分歧时，通过协商机制达成共识
5. **动态适应**：智能体可以根据任务复杂度动态调整协作策略

**八大智能体**（对应原八大临床脑区）：

0. **健康状态判定智能体**（Health State Assessment Agent）- **关键分叉点**
1. **病例理解智能体**（Clinical Parsing Agent）
2. **主动问诊智能体**（Interview Agent）
3. **鉴别诊断智能体**（Differential Diagnosis Agent）
4. **检查建议智能体**（Workup Planner Agent）
5. **治疗建议智能体**（Management Planner Agent）
6. **风险评估智能体**（Triage Agent）
7. **证据链智能体**（Evidence Agent）

**智能体协调器**（Orchestrator Agent）：
- 负责任务分配和流程编排
- 监控智能体状态和任务进度
- 处理智能体间的冲突和协商
- 实现负载均衡和性能优化

**双通道推理架构在多智能体系统中的体现**：

#### 通道1：结构化推理通道（决定"该往哪想" - 临床逻辑）

**主要智能体**：
- **鉴别诊断智能体**：知识图谱推理（DR.KNOWS方法）、多引擎融合诊断
- **检查建议智能体**：检查价值评估、信息增益计算
- **治疗建议智能体**：治疗方案推理、药物推荐
- **风险评估智能体**：风险识别、紧急程度评估
- **健康状态判定智能体**：规则推理、风险评估
- **病例理解智能体**：概念归一化、结构化提取

**技术实现**：
- 知识图谱路径检索与排序（DR.KNOWS方法）
- 规则引擎、统计模型、贝叶斯推理
- 多引擎融合诊断
- 医学概念标准化（CUI/ICD/SNOMED）

**输出**：DDx候选 + 证据结构 + 推理路径（写入CDP）

#### 通道2：语言与策略通道（决定"怎么说、怎么问" - 医生表达）

**主要智能体**：
- **主动问诊智能体**：生成问诊问题、自然语言对话
- **证据链智能体**：生成解释和说明、推理路径可视化
- **治疗建议智能体**：生成治疗方案的自然语言表达

**技术实现**：
- LLM（大语言模型）
- NLG（自然语言生成）
- 对话生成、解释生成
- 自然语言理解（NLU）

**输出**：自然语言问诊、解释、建议

#### 智能体与双通道的映射关系

| 智能体 | 主要通道 | 通道1能力 | 通道2能力 |
|--------|---------|----------|----------|
| 健康状态判定智能体 | 通道1 | 规则推理、风险评估 | - |
| 病例理解智能体 | 通道1 | 概念归一化、结构化提取 | - |
| 主动问诊智能体 | 通道1 + 通道2 | 信息缺口识别、信息增益计算 | 生成问诊问题、自然语言对话 |
| 鉴别诊断智能体 | 通道1 | 知识图谱推理、多引擎融合 | - |
| 检查建议智能体 | 通道1 | 检查价值评估、信息增益计算 | - |
| 治疗建议智能体 | 通道1 + 通道2 | 治疗方案推理、药物推荐 | 生成治疗方案的自然语言表达 |
| 风险评估智能体 | 通道1 | 风险识别、紧急程度评估 | - |
| 证据链智能体 | 通道1 + 通道2 | 证据链构建、推理路径分析 | 生成解释和说明、可视化 |

**双通道协作机制**：

```
通道1智能体（结构化推理）
    ↓
生成结构化结果（DDx、检查建议等）
    ↓
写入CDP（结构化数据）
    ↓
通道2智能体（语言与策略）
    ↓
读取CDP
    ↓
生成自然语言表达
    ↓
输出给用户
```

**关键原则**：
- **通道1决定"该往哪想"**：结构化推理通道负责临床逻辑推理，生成结构化的诊断、检查、治疗建议
- **通道2决定"怎么说"**：语言与策略通道负责将结构化结果转换为自然语言，生成问诊问题、解释说明
- **CDP作为桥梁**：通道1的输出写入CDP，通道2基于CDP生成自然语言表达
- **可追溯性**：确保推理过程可追溯、可审计

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
│  │  - 诊断流程编排（5步AI循证诊断流程）                   │  │
│  │  - 诊断结果管理                                        │  │
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
│  │  │   ├─ 知识图谱推理引擎（kg-reasoning-engine）        │  │
│  │  │   │   - 多跳推理路径检索（DR.KNOWS）                │  │
│  │  │   │   - 三层评分体系（贝叶斯诊断理论）              │  │
│  │  │   │   - 路径注入LLM                                 │  │
│  │  │   ├─ 多引擎融合诊断（multi-engine-fusion）          │  │
│  │  │   │   ├─ 规则引擎（rule-engine）                    │  │
│  │  │   │   ├─ 知识图谱引擎（kg-engine）                  │  │
│  │  │   │   ├─ 统计模型引擎（statistical-engine）         │  │
│  │  │   │   ├─ 大模型引擎（llm-engine，路径约束）        │  │
│  │  │   │   └─ 鉴别诊断引擎（differential-engine）        │  │
│  │  │   ├─ 三层分层分类器（three_layer_classifier）       │  │
│  │  │   ├─ 推理组织器（reasoning_organizer）               │  │
│  │  │   └─ 证据分析器（evidence_analyzer）                │  │
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
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                数据层（Data Layer）                              │
│  - MySQL 8.0（本地开发）/ Oracle（生产环境）                    │
│    （CDP存储、诊断记录、检查记录、健康状态判定记录等）          │
│  - Redis 7（缓存、会话状态、CDP临时状态）                       │
│  - Neo4j 5（知识图谱，DR.KNOWS核心依赖）                        │
└─────────────────────────────────────────────────────────────────┘
```

**服务架构设计说明**：

> **设计原则**：按照功能模块（脑区）来组织服务，每个服务对应一个脑区，职责单一明确。  
> **详细说明**：请参考《项目结构设计原则.md》

**服务拆分说明**：
- `diagnosis-engine-service`：仅负责脑区C（鉴别诊断引擎）的核心功能
- `workup-planner-service`：独立服务，负责脑区D（检查/检验建议与价值评估）
- `treatment-engine-service`：独立服务，负责脑区E（治疗/处置建议引擎）
- `risk-assessment-service`：独立服务，负责脑区F（风险与急症识别）

**拆分原因**：
1. **职责清晰**：每个服务只负责一个脑区的功能，符合单一职责原则
2. **独立演进**：每个脑区可以单独训练、评测、替换，服务可以独立升级
3. **易于维护**：功能模块边界清晰，便于团队协作和代码维护
4. **符合微服务原则**：每个服务是独立的业务能力单元，可以独立部署和扩展

**服务与智能体的对应关系**：

| 服务 | 对应智能体 | 智能体ID | 主要职责 |
|------|-----------|---------|---------|
| health-state-assessment-service | 健康状态判定智能体 | agent_0 | 判断工作态、入口判定流程 |
| clinical-parsing-service | 病例理解智能体 | agent_1 | 概念归一化、结构化提取 |
| dialog-service | 主动问诊智能体 | agent_2 | 信息缺口识别、问诊生成 |
| diagnosis-engine-service | 鉴别诊断智能体 | agent_3 | 多引擎融合诊断、DDx生成 |
| workup-planner-service | 检查建议智能体 | agent_4 | 检查价值评估、验证计划 |
| treatment-engine-service | 治疗建议智能体 | agent_5 | 治疗方案推理、药物推荐 |
| risk-assessment-service | 风险评估智能体 | agent_6 | 高危识别、紧急程度评估 |
| explanation-service | 证据链智能体 | agent_7 | 证据链构建、解释生成 |
| diagnosis-service | 协调器（Orchestrator） | orchestrator | 任务分配、流程编排、冲突解决 |

### 1.4 服务依赖关系与调用策略

#### 1.4.1 服务依赖图

**核心服务依赖关系**：

```
diagnosis-service (协调器)
    ├─→ health-state-assessment-service (健康状态判定)
    │       └─→ [无依赖]
    │
    ├─→ clinical-parsing-service (病例理解)
    │       ├─→ ocr-service (OCR服务，可选)
    │       └─→ [Neo4j知识图谱，用于概念归一化]
    │
    ├─→ dialog-service (主动问诊)
    │       ├─→ clinical-parsing-service (获取结构化信息)
    │       └─→ diagnosis-engine-service (获取DDx信息)
    │
    ├─→ diagnosis-engine-service (鉴别诊断)
    │       ├─→ clinical-parsing-service (获取结构化病例)
    │       ├─→ risk-assessment-service (获取风险评估)
    │       └─→ [Neo4j知识图谱，用于路径推理]
    │
    ├─→ workup-planner-service (检查建议)
    │       ├─→ diagnosis-engine-service (获取DDx列表)
    │       └─→ risk-assessment-service (获取风险等级)
    │
    ├─→ treatment-engine-service (治疗建议)
    │       ├─→ diagnosis-engine-service (获取诊断结果)
    │       └─→ risk-assessment-service (获取风险等级)
    │
    ├─→ risk-assessment-service (风险评估)
    │       ├─→ diagnosis-engine-service (获取DDx列表)
    │       └─→ [无其他服务依赖]
    │
    └─→ explanation-service (证据链)
            ├─→ diagnosis-engine-service (获取推理路径)
            ├─→ workup-planner-service (获取检查建议)
            └─→ treatment-engine-service (获取治疗方案)
```

**数据流向**：

```
用户输入
    ↓
diagnosis-service (创建CDP)
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

**调用实现示例**：

```java
// 同步调用示例（Java - OpenFeign）
@Service
public class DiagnosisOrchestrator {
    
    @Autowired
    private HealthStateAssessmentClient healthStateClient;
    
    @Autowired
    private ClinicalParsingClient parsingClient;
    
    // 同步调用：健康状态判定
    public HealthStateAssessmentResult assessHealthState(CDP cdp) {
        try {
            // 同步调用，设置超时时间5秒
            return healthStateClient.assess(cdp, 5000);
        } catch (TimeoutException e) {
            // 超时处理：返回默认结果或重试
            log.error("Health state assessment timeout", e);
            return HealthStateAssessmentResult.defaultResult();
        }
    }
    
    // 异步调用：检查建议生成
    public CompletableFuture<WorkupPlan> generateWorkupPlanAsync(CDP cdp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return workupPlannerClient.generatePlan(cdp);
            } catch (Exception e) {
                log.error("Workup plan generation failed", e);
                return WorkupPlan.empty();
            }
        });
    }
}
```

```python
# 异步调用示例（Python - 消息队列）
class DiagnosisOrchestrator:
    def __init__(self):
        self.redis_client = redis.Redis()
        self.message_queue = "diagnosis:workup_plan"
    
    def generate_workup_plan_async(self, cdp: CDP):
        """
        异步生成检查建议
        """
        # 1. 发送任务到消息队列
        task_id = str(uuid.uuid4())
        task_data = {
            "task_id": task_id,
            "cdp_id": cdp.id,
            "action": "generate_workup_plan"
        }
        
        # 2. 发布到消息队列
        self.redis_client.lpush(
            self.message_queue,
            json.dumps(task_data)
        )
        
        # 3. 返回任务ID，客户端可以通过任务ID查询结果
        return task_id
    
    def get_workup_plan_result(self, task_id: str) -> Optional[WorkupPlan]:
        """
        查询异步任务结果
        """
        result_key = f"diagnosis:result:{task_id}"
        result_data = self.redis_client.get(result_key)
        
        if result_data:
            return WorkupPlan.from_dict(json.loads(result_data))
        
        return None
```

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

**实现示例**：

```java
// CDP版本控制（Java）
@Entity
public class ClinicalDecisionPackage {
    @Id
    private String id;
    
    @Version  // JPA乐观锁
    private Long version;
    
    private String cdpData;  // JSON格式的CDP数据
    
    // 更新CDP时检查版本号
    public void update(CDP newData) {
        // 1. 读取当前版本
        Long currentVersion = this.version;
        
        // 2. 更新数据
        this.cdpData = JSON.toJSONString(newData);
        
        // 3. 版本号自动递增（JPA处理）
        // 如果版本号不匹配，抛出OptimisticLockException
    }
}
```

```python
# CDP版本控制（Python）
class CDPManager:
    def __init__(self, db_client, redis_client):
        self.db = db_client
        self.redis = redis_client
        self.cache_ttl = 300  # 5分钟
    
    def update_cdp(self, cdp_id: str, new_data: dict) -> bool:
        """
        更新CDP，使用乐观锁
        """
        # 1. 从数据库读取当前版本
        current_cdp = self.db.get_cdp(cdp_id)
        current_version = current_cdp['version']
        
        # 2. 更新数据（带版本检查）
        new_version = current_version + 1
        update_result = self.db.update_cdp(
            cdp_id=cdp_id,
            data=new_data,
            expected_version=current_version,
            new_version=new_version
        )
        
        if not update_result:
            # 版本冲突，返回False
            return False
        
        # 3. 更新缓存
        cache_key = f"cdp:{cdp_id}"
        self.redis.setex(
            cache_key,
            self.cache_ttl,
            json.dumps(new_data)
        )
        
        return True
    
    def get_cdp(self, cdp_id: str, use_cache: bool = True) -> dict:
        """
        获取CDP，优先从缓存读取
        """
        cache_key = f"cdp:{cdp_id}"
        
        # 1. 尝试从缓存读取
        if use_cache:
            cached_data = self.redis.get(cache_key)
            if cached_data:
                return json.loads(cached_data)
        
        # 2. 从数据库读取
        cdp_data = self.db.get_cdp(cdp_id)
        
        # 3. 更新缓存
        if cdp_data:
            self.redis.setex(
                cache_key,
                self.cache_ttl,
                json.dumps(cdp_data)
            )
        
        return cdp_data
```

**分布式事务处理**：

1. **Saga模式**（长事务）：
   - 诊断流程涉及多个服务调用
   - 使用Saga模式管理分布式事务
   - 支持补偿操作（回滚）

2. **两阶段提交（2PC）**（短事务）：
   - 单个服务内的多个操作
   - 使用2PC保证原子性

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

## 二、多智能体系统设计

### 2.1 智能体基础架构

每个智能体具备以下核心能力：

#### 2.1.1 智能体能力模型

```json
{
  "agent_id": "agent_0",
  "agent_name": "健康状态判定智能体",
  "capabilities": {
    "perception": {
      "can_read_cdp": true,
      "can_read_patient_input": true,
      "can_read_other_agents_output": true
    },
    "reasoning": {
      "reasoning_type": "rule_based" | "knowledge_graph" | "statistical" | "llm",
      "reasoning_engines": ["rule_engine", "kg_engine"],
      "channel": "channel_1" | "channel_2" | "both",
      "channel_1_engines": ["rule_engine", "kg_engine", "statistical_engine"],
      "channel_2_engines": ["llm", "nlg"],
      "confidence_threshold": 0.7
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["health_state_assessment"],
      "requires_consensus": false
    },
    "action": {
      "can_write_cdp": true,
      "can_send_messages": true,
      "can_trigger_other_agents": true,
      "can_request_help": true
    },
    "communication": {
      "can_send_messages": true,
      "can_receive_messages": true,
      "message_types": ["request", "response", "notification", "consensus_request"]
    }
  },
  "state": {
    "status": "idle" | "working" | "waiting" | "error",
    "current_task": null,
    "task_queue": [],
    "collaboration_history": []
  }
}
```

#### 2.1.2 智能体生命周期

```
智能体初始化
    ↓
等待任务分配（Orchestrator）
    ↓
接收任务
    ↓
感知（读取CDP/患者输入）
    ↓
推理（执行核心能力）
    ↓
决策（生成输出）
    ↓
行动（更新CDP/发送消息）
    ↓
检查是否需要协作
    ↓
是 → 发起协作请求
    ↓
否 → 完成任务，通知Orchestrator
    ↓
返回等待状态
```

### 2.2 智能体通信机制

#### 2.2.1 消息传递协议

**消息结构**：

```json
{
  "message_id": "msg_123456",
  "sender": "agent_0",
  "receiver": "agent_1" | "broadcast" | "orchestrator",
  "message_type": "request" | "response" | "notification" | "consensus_request",
  "task_id": "task_789",
  "content": {
    "action": "request_parsing",
    "data": {...},
    "priority": "high" | "medium" | "low",
    "deadline": "2025-01-01T12:00:00Z",
    "requires_response": true
  },
  "timestamp": "2025-01-01T10:00:00Z",
  "correlation_id": "corr_123"
}
```

**消息类型**：

1. **Request（请求）**：智能体A请求智能体B执行某个任务
2. **Response（响应）**：智能体B响应智能体A的请求
3. **Notification（通知）**：智能体A通知其他智能体某个事件
4. **Consensus Request（协商请求）**：智能体A请求其他智能体参与协商

#### 2.2.2 消息路由机制

**路由规则**：

1. **直接路由**：发送给特定智能体（receiver = "agent_X"）
2. **广播路由**：发送给所有智能体（receiver = "broadcast"）
3. **组播路由**：发送给特定组（receiver = "group:diagnosis_agents"）
4. **条件路由**：根据消息内容路由到相关智能体

**消息队列**：

- 每个智能体维护一个消息队列
- 消息按优先级排序
- 支持消息超时和重试机制

#### 2.2.3 消息路由实现（技术细节）

**路由实现架构**：

```python
class MessageRouter:
    """
    消息路由器：负责消息的路由和分发
    """
    def __init__(self):
        self.agent_registry = AgentRegistry()  # 智能体注册表
        self.message_queue = MessageQueue()   # 消息队列（Redis）
        self.routing_table = RoutingTable()   # 路由表
        
    def route_message(self, message: Message) -> bool:
        """
        路由消息到目标智能体
        """
        # 1. 解析接收者
        receiver = message.receiver
        
        # 2. 根据接收者类型选择路由策略
        if receiver.startswith("agent_"):
            # 直接路由
            return self._direct_route(message, receiver)
        elif receiver == "broadcast":
            # 广播路由
            return self._broadcast_route(message)
        elif receiver.startswith("group:"):
            # 组播路由
            return self._multicast_route(message, receiver)
        else:
            # 条件路由
            return self._conditional_route(message)
    
    def _direct_route(self, message: Message, agent_id: str) -> bool:
        """
        直接路由：发送给特定智能体
        """
        # 1. 检查智能体是否存在
        if not self.agent_registry.exists(agent_id):
            # 智能体不存在，记录错误
            self._log_error(f"Agent {agent_id} not found")
            return False
        
        # 2. 获取智能体的消息队列地址
        queue_address = self.agent_registry.get_queue_address(agent_id)
        
        # 3. 发送消息到队列
        return self.message_queue.enqueue(queue_address, message)
    
    def _broadcast_route(self, message: Message) -> bool:
        """
        广播路由：发送给所有智能体
        """
        all_agents = self.agent_registry.get_all_agents()
        success_count = 0
        
        for agent_id in all_agents:
            if self._direct_route(message, agent_id):
                success_count += 1
        
        return success_count > 0
    
    def _multicast_route(self, message: Message, group: str) -> bool:
        """
        组播路由：发送给特定组
        """
        # 解析组名（如 "group:diagnosis_agents"）
        group_name = group.replace("group:", "")
        group_agents = self.agent_registry.get_group_agents(group_name)
        
        success_count = 0
        for agent_id in group_agents:
            if self._direct_route(message, agent_id):
                success_count += 1
        
        return success_count > 0
    
    def _conditional_route(self, message: Message) -> bool:
        """
        条件路由：根据消息内容路由到相关智能体
        """
        # 1. 分析消息内容
        content = message.content
        action = content.get("action")
        
        # 2. 根据动作类型查找相关智能体
        target_agents = self.routing_table.lookup(action)
        
        # 3. 发送给相关智能体
        success_count = 0
        for agent_id in target_agents:
            if self._direct_route(message, agent_id):
                success_count += 1
        
        return success_count > 0
```

**消息队列实现**（基于Redis）：

```python
class MessageQueue:
    """
    消息队列：基于Redis实现
    """
    def __init__(self, redis_client):
        self.redis = redis_client
        self.queue_prefix = "agent:queue:"
        
    def enqueue(self, queue_name: str, message: Message) -> bool:
        """
        将消息加入队列
        """
        queue_key = f"{self.queue_prefix}{queue_name}"
        
        # 1. 序列化消息
        message_json = json.dumps(message.to_dict())
        
        # 2. 根据优先级选择队列
        if message.content.get("priority") == "high":
            # 高优先级消息使用LPUSH（左插入，优先处理）
            self.redis.lpush(queue_key, message_json)
        else:
            # 普通优先级消息使用RPUSH（右插入）
            self.redis.rpush(queue_key, message_json)
        
        # 3. 设置消息过期时间（默认30秒）
        message_ttl = message.content.get("deadline")
        if message_ttl:
            self.redis.expire(queue_key, 30)
        
        return True
    
    def dequeue(self, queue_name: str, timeout: int = 5) -> Optional[Message]:
        """
        从队列中取出消息（阻塞式）
        """
        queue_key = f"{self.queue_prefix}{queue_name}"
        
        # 使用BRPOP（阻塞式右弹出）
        result = self.redis.brpop(queue_key, timeout=timeout)
        
        if result:
            _, message_json = result
            message_dict = json.loads(message_json)
            return Message.from_dict(message_dict)
        
        return None
```

**消息超时和重试机制**：

```python
class MessageRetryHandler:
    """
    消息重试处理器
    """
    def __init__(self, redis_client):
        self.redis = redis_client
        self.max_retries = 3
        self.retry_delay = 5  # 秒
        
    def handle_timeout(self, message: Message) -> bool:
        """
        处理消息超时
        """
        # 1. 检查重试次数
        retry_count = message.metadata.get("retry_count", 0)
        
        if retry_count >= self.max_retries:
            # 超过最大重试次数，记录失败
            self._log_failure(message)
            return False
        
        # 2. 增加重试次数
        message.metadata["retry_count"] = retry_count + 1
        
        # 3. 延迟后重新发送
        time.sleep(self.retry_delay)
        return self._resend_message(message)
    
    def _resend_message(self, message: Message) -> bool:
        """
        重新发送消息
        """
        router = MessageRouter()
        return router.route_message(message)
```

#### 2.2.4 智能体协作时序图

**顺序协作时序图**：

```
用户请求
    ↓
Orchestrator
    ↓
健康状态判定智能体 (agent_0)
    |-- 读取CDP
    |-- 执行健康状态判定
    |-- 更新CDP (work_mode)
    |-- 发送通知给Orchestrator
    ↓
Orchestrator
    ↓
病例理解智能体 (agent_1)
    |-- 读取CDP
    |-- 执行概念归一化
    |-- 更新CDP (patient_state)
    |-- 发送通知给Orchestrator
    ↓
Orchestrator
    ↓
主动问诊智能体 (agent_2)
    |-- 读取CDP
    |-- 识别信息缺口
    |-- 生成问诊问题
    |-- 更新CDP (uncertainty.missing_critical_info)
    |-- 发送通知给Orchestrator
    ↓
...
```

**并行协作时序图**：

```
Orchestrator
    ↓
    ├─→ 鉴别诊断智能体 (agent_3) ──┐
    ├─→ 检查建议智能体 (agent_4) ──┤
    └─→ 风险评估智能体 (agent_6) ──┼→ 等待所有结果
                                    ↓
                                Orchestrator
                                    ↓
                                融合结果
                                    ↓
                                更新CDP
```

**协商协作时序图**：

```
鉴别诊断智能体 (agent_3)
    |-- 生成诊断结果A
    |-- 发送协商请求
    ↓
Orchestrator
    ↓
    ├─→ 检查建议智能体 (agent_4)
    |   |-- 评估结果A
    |   |-- 提交观点和证据
    |   └─→
    ├─→ 风险评估智能体 (agent_6)
    |   |-- 评估结果A
    |   |-- 提交观点和证据
    |   └─→
    └─→ 证据链智能体 (agent_7)
        |-- 评估结果A
        |-- 提交观点和证据
        └─→
    ↓
Orchestrator
    |-- 分析观点和证据
    |-- 执行协商算法（加权投票/证据融合）
    |-- 生成协商结果
    ↓
如果达成共识 → 更新CDP
如果存在分歧 → 触发冲突解决机制
```

#### 2.2.5 性能与扩展性考虑

**性能优化策略**：

1. **消息队列优化**：
   - 使用Redis作为消息队列，支持高并发
   - 消息按优先级排序，高优先级消息优先处理
   - 支持消息批量处理，减少网络开销

2. **智能体负载均衡**：
   - 监控每个智能体的负载情况
   - 动态分配任务到负载较低的智能体
   - 支持智能体水平扩展（多实例）

3. **异步处理**：
   - 非关键路径使用异步消息传递
   - 关键路径（如高危诊断）使用同步等待
   - 支持消息超时和重试机制

**扩展性设计**：

1. **水平扩展**：
   - 每个智能体可以部署多个实例
   - 使用负载均衡器分发请求
   - 支持智能体动态注册和注销

2. **垂直扩展**：
   - 智能体可以根据负载动态调整资源
   - 支持智能体能力动态升级（模型版本更新）

3. **容错机制**：
   - 智能体故障时自动切换到备用实例
   - 消息持久化，支持故障恢复
   - 支持消息重试和死信队列

### 2.3 智能体协调器（Orchestrator Agent）

#### 2.3.1 协调器职责

1. **任务分配**：根据任务类型和智能体能力分配任务
2. **流程编排**：编排智能体的执行顺序
3. **状态监控**：监控所有智能体的状态和任务进度
4. **冲突处理**：处理智能体间的冲突
5. **负载均衡**：平衡智能体的工作负载

#### 2.3.2 协调器工作流程

```
接收新任务（用户咨询）
    ↓
分析任务类型和复杂度
    ↓
选择初始智能体（通常是健康状态判定智能体）
    ↓
分配任务给初始智能体
    ↓
监控任务执行
    ↓
接收智能体完成通知
    ↓
根据结果决定下一步
    ↓
分配任务给下一个智能体
    ↓
重复直到任务完成
```

#### 2.3.3 任务分配策略

**策略1：基于任务类型**
- 健康状态判定 → 健康状态判定智能体
- 病例理解 → 病例理解智能体
- 诊断推理 → 鉴别诊断智能体

**策略2：基于智能体负载**
- 选择负载最低的智能体
- 避免智能体过载

**策略3：基于智能体能力**
- 选择最适合的智能体
- 考虑智能体的专业领域

**策略4：基于历史表现**
- 选择历史表现最好的智能体
- 考虑智能体的成功率

### 2.4 智能体协作机制

#### 2.4.1 协作模式

**模式1：顺序协作**
```
智能体A → 智能体B → 智能体C
```
- 智能体A完成任务后，触发智能体B
- 智能体B完成任务后，触发智能体C

**模式2：并行协作**
```
智能体A
    ↓
智能体B ──┐
智能体C ──┼→ 智能体D（等待所有结果）
智能体E ──┘
```
- 多个智能体并行执行
- 智能体D等待所有结果后执行

**模式3：协商协作**
```
智能体A ──┐
智能体B ──┼→ 协商机制 → 达成共识
智能体C ──┘
```
- 多个智能体产生不同结果
- 通过协商机制达成共识

**模式4：竞争协作**
```
智能体A ──┐
智能体B ──┼→ 投票机制 → 选择最佳结果
智能体C ──┘
```
- 多个智能体竞争执行同一任务
- 通过投票选择最佳结果

#### 2.4.2 协商机制

**协商流程**：

```
智能体A发起协商请求
    ↓
通知相关智能体（智能体B、C、D）
    ↓
各智能体提交自己的观点和证据
    ↓
协商机制分析观点和证据
    ↓
生成协商结果（共识/分歧）
    ↓
如果达成共识 → 更新CDP
    ↓
如果存在分歧 → 触发冲突解决机制
```

**协商算法**：

1. **加权投票**：根据智能体的置信度和历史表现加权
2. **证据融合**：融合各智能体的证据
3. **贝叶斯更新**：使用贝叶斯方法更新概率
4. **专家系统**：使用专家规则解决分歧

#### 2.4.3 冲突解决机制

**冲突类型**：

1. **诊断冲突**：不同智能体给出不同诊断
2. **证据冲突**：不同智能体对同一证据有不同解释
3. **优先级冲突**：不同智能体对任务优先级有不同看法
4. **资源冲突**：多个智能体竞争同一资源

**冲突解决策略**：

**策略1：优先级规则**
- 风险评估智能体的优先级最高
- 健康状态判定智能体的优先级次之
- 其他智能体按任务重要性排序

**策略2：证据强度**
- 选择证据强度最高的结果
- 考虑证据的来源和可信度

**策略3：专家投票**
- 相关领域的智能体投票
- 根据智能体的专业性和历史表现加权

**策略4：人工介入**
- 当冲突无法自动解决时
- 请求人工医生介入

---

## 三、核心技术组件设计

### 3.1 知识图谱推理引擎（基于DR.KNOWS）

#### 3.1.1 医学概念识别与归一化

**技术实现**：
- **工具**：QuickUMLS / cTAKES / 中文医学实体识别模型
- **功能**：从患者描述中提取医学概念，归一化到标准术语（CUI/ICD/SNOMED）
- **输出**：标准化的医学概念列表

**代码结构**：
```python
class MedicalConceptExtractor:
    def extract_concepts(self, text: str) -> List[Concept]:
        """
        从文本中提取医学概念
        """
        # 使用QuickUMLS或中文医学NER模型
        concepts = self.ner_model.extract(text)
        
        # 归一化到标准术语
        normalized_concepts = []
        for concept in concepts:
            cui = self.normalizer.normalize(concept)
            if cui:
                normalized_concepts.append(Concept(cui=cui, ...))
        
        return normalized_concepts
```

#### 3.1.2 多跳推理路径检索

**技术实现**：
- **知识图谱**：Neo4j（存储UMLS或自定义医学知识图谱）
- **路径搜索**：Cypher查询 + 图遍历算法
- **路径类型**：
  1. 症状→疾病路径（DR.KNOWS核心）
  2. 疾病→检查路径（扩展）
  3. 疾病→治疗路径（扩展）
  4. 综合推理路径（多路径融合）

**Cypher查询示例**：
```cypher
// 症状→疾病路径（2-4跳）
MATCH path = (s:Symptom)-[*2..4]->(d:Disease)
WHERE s.cui IN $symptom_cuis
RETURN path, 
       relationships(path) as rels,
       nodes(path) as nodes,
       length(path) as path_length
ORDER BY path_length
LIMIT 50
```

#### 3.1.3 三层评分体系（基于贝叶斯诊断理论）

**医疗理论依据**：贝叶斯诊断理论（Bayesian Diagnostic Theory）

**核心思想**：
诊断是一个概率推理过程，应该基于：
1. **先验概率**：疾病的患病率（常见病优先考虑）
2. **似然**：给定疾病下证据的敏感性/特异性
3. **后验概率**：给定证据下疾病的概率（贝叶斯更新）

**层1：先验概率评分**（Prior Probability Score）- 基于疾病患病率

**医疗理论依据**：
- **Occam's Razor in Medicine**：常见病优先考虑
- **疾病患病率**：不同疾病的先验概率不同

**技术实现**：
```python
class PriorProbabilityScorer:
    def score_prior_probability(self, disease: Disease, patient_context: Dict) -> float:
        """
        计算疾病的先验概率评分
        基于疾病患病率和患者特征
        """
        # 1. 基础患病率（基于流行病学数据）
        base_prevalence = self.prevalence_db.get_prevalence(
            disease.disease_cui,
            age_group=patient_context.get('age_group'),
            gender=patient_context.get('gender'),
            region=patient_context.get('region')
        )
        
        # 2. 归一化到[0, 1]区间
        max_prevalence = self.prevalence_db.get_max_prevalence()
        prior_score = log(base_prevalence + 1e-6) / log(max_prevalence + 1e-6)
        
        return prior_score
```

**层2：似然评分**（Likelihood Score）- 基于证据的敏感性/特异性

**医疗理论依据**：
- **诊断试验的准确性**：敏感性（Sensitivity）和特异性（Specificity）
- **证据的预测价值**：阳性预测值（PPV）和阴性预测值（NPV）

**技术实现**：
```python
class LikelihoodScorer:
    def score_likelihood(self, disease: Disease, evidence: Evidence) -> float:
        """
        计算证据的似然评分
        基于诊断试验的敏感性和特异性
        """
        # 1. 计算敏感性（给定疾病，证据出现的概率）
        sensitivity = self._calculate_sensitivity(disease, evidence)
        
        # 2. 计算特异性（给定非疾病，证据不出现的概率）
        specificity = self._calculate_specificity(disease, evidence)
        
        # 3. 综合评分（Youden指数）
        likelihood_score = (sensitivity + specificity) / 2
        
        # 4. 考虑证据类型
        if evidence.evidence_type == 'objective_test':
            # 客观检查权重更高
            likelihood_score *= 1.2
        elif evidence.evidence_type == 'typical_symptom_combo':
            # 典型症状组合权重中等
            likelihood_score *= 1.0
        else:
            # 主观描述权重较低
            likelihood_score *= 0.8
        
        return min(likelihood_score, 1.0)
```

**层3：后验概率评分**（Posterior Probability Score）- 基于贝叶斯更新

**医疗理论依据**：
- **贝叶斯定理**：P(D|E) = (P(E|D) · P(D)) / P(E)
- **后验概率**：给定证据下疾病的最优诊断概率

**技术实现**：
```python
class PosteriorProbabilityScorer:
    def score_posterior_probability(self, 
                                   disease: Disease,
                                   evidence_list: List[Evidence],
                                   prior_probability: float) -> float:
        """
        计算疾病的后验概率
        基于贝叶斯定理
        """
        # 1. 计算似然（所有证据的联合似然）
        likelihood = 1.0
        for evidence in evidence_list:
            evidence_likelihood = self.likelihood_scorer.score(disease, evidence)
            likelihood *= evidence_likelihood
        
        # 2. 计算证据的边际概率（归一化常数）
        evidence_marginal = self._calculate_evidence_marginal(evidence_list)
        
        # 3. 贝叶斯更新
        posterior_probability = (likelihood * prior_probability) / evidence_marginal
        
        return posterior_probability
```

**综合评分**（基于医疗理论）：

```python
class BayesianPathRanker:
    def rank_paths(self, 
                   paths: List[Path], 
                   patient_context: str,
                   current_ddx: List[Diagnosis]) -> List[RankedPath]:
        """
        基于贝叶斯诊断理论对路径进行评分和排序
        """
        ranked_paths = []
        
        for path in paths:
            disease = path.target_disease
            
            # 层1：先验概率评分
            prior_score = self.prior_scorer.score_prior_probability(
                disease, patient_context
            )
            
            # 层2：似然评分
            likelihood_score = self.likelihood_scorer.score_likelihood(
                disease, path.evidence
            )
            
            # 层3：后验概率评分（贝叶斯更新）
            posterior_score = self.posterior_scorer.score_posterior_probability(
                disease, path.evidence, prior_score
            )
            
            # 综合评分（基于医疗理论）
            # 权重设置：
            # - w_prior = 0.2（先验概率权重较低，因为可能受人群影响）
            # - w_likelihood = 0.3（似然权重中等，因为反映诊断准确性）
            # - w_posterior = 0.5（后验概率权重最高，因为是最终诊断概率）
            composite_score = (
                0.2 * prior_score +
                0.3 * likelihood_score +
                0.5 * posterior_score
            )
            
            ranked_paths.append(RankedPath(
                path=path,
                prior_score=prior_score,
                likelihood_score=likelihood_score,
                posterior_score=posterior_score,
                composite_score=composite_score
            ))
        
        # 排序并返回Top-10
        ranked_paths.sort(key=lambda x: x.composite_score, reverse=True)
        return ranked_paths[:10]
```

**医疗理论保证**：
- **贝叶斯诊断理论**：后验概率 P(D|E) 是最优的诊断决策依据
- **证据融合**：多个证据的联合似然通过贝叶斯更新得到后验概率
- **个性化诊断**：考虑患者特征（年龄、性别等）调整先验概率

```python
class PathRelevanceScorer:
    def score_path_relevance(self, path: Path, patient_context: str) -> float:
        """
        计算路径与患者语境的相关性
        """
        # 1. 生成节点嵌入（SGIN或简化版本）
        node_embeddings = self.sgin.encode(path.nodes)
        
        # 2. 生成路径嵌入
        path_embedding = self._aggregate_embeddings(node_embeddings)
        
        # 3. 生成患者语境嵌入
        context_embedding = self.bert.encode(patient_context)
        
        # 4. 注意力机制计算相似度
        attention_score = self.attention(
            query=context_embedding,
            key=path_embedding,
            value=path_embedding
        )
        
        # 5. 路径长度惩罚
        length_penalty = 1.0 / (1.0 + path.length * 0.1)
        
        return attention_score * length_penalty
```

**层2：证据强度评分**（Clinical Evidence Strength）

**技术实现**：
- **证据类型分级**：
  - 强证据：客观检查异常（如心电图ST段抬高）
  - 中证据：典型症状组合（如胸痛+出汗）
  - 弱证据：模糊感受、非特异性症状

```python
class EvidenceStrengthScorer:
    EVIDENCE_STRENGTH = {
        'objective_test_abnormal': 1.0,  # 客观检查异常
        'typical_symptom_combo': 0.7,    # 典型症状组合
        'subjective_complaint': 0.4,     # 主诉描述
        'vague_feeling': 0.2             # 模糊感受
    }
    
    def score_evidence_strength(self, evidence: Evidence) -> float:
        """
        评估证据强度
        """
        evidence_type = self._classify_evidence_type(evidence)
        return self.EVIDENCE_STRENGTH.get(evidence_type, 0.1)
```

**层3：信息增益评分**（Information Gain）- 已整合到临床决策分析中

**注意**：信息增益计算已整合到脑区B的"临床决策分析驱动的问诊"中，
采用扩展的临床决策增益计算（IG_clinical），详见系统功能设计文档。

#### 3.1.4 路径排序与Top-N选择

**技术实现**：
- **综合评分**：三层评分的加权组合
- **排序**：按综合评分降序排序
- **Top-N选择**：返回Top-10最相关的路径

```python
class PathRanker:
    def rank_paths(self, 
                   paths: List[Path], 
                   patient_context: str,
                   current_ddx: List[Diagnosis]) -> List[RankedPath]:
        """
        对路径进行三层评分和排序
        """
        ranked_paths = []
        
        for path in paths:
            # 层1：路径相关性
            relevance_score = self.relevance_scorer.score(path, patient_context)
            
            # 层2：证据强度
            evidence_strength = self.evidence_scorer.score(path.evidence)
            
            # 综合评分（基于贝叶斯诊断理论）
            # 注意：这里使用贝叶斯诊断理论的三层评分体系
            # 详见上面的BayesianPathRanker实现
            
            ranked_paths.append(RankedPath(
                path=path,
                relevance_score=relevance_score,
                evidence_strength=evidence_strength,
                composite_score=composite_score
            ))
        
        # 排序并返回Top-10
        ranked_paths.sort(key=lambda x: x.composite_score, reverse=True)
        return ranked_paths[:10]
```

#### 3.1.5 路径注入LLM

**技术实现**：
- **路径格式化**：将路径格式化为自然语言描述
- **Prompt构建**：将路径作为上下文注入prompt
- **受控生成**：LLM在路径约束下生成诊断

```python
class PathInjector:
    def format_paths_for_llm(self, ranked_paths: List[RankedPath]) -> str:
        """
        将路径格式化为自然语言描述
        """
        path_descriptions = []
        
        for ranked_path in ranked_paths:
            path = ranked_path.path
            description = "推理路径："
            
            for i in range(len(path.nodes) - 1):
                from_node = path.nodes[i]
                to_node = path.nodes[i + 1]
                rel = path.relationships[i]
                
                description += f"{from_node.name} --[{rel.type}]--> {to_node.name}; "
            
            description += f"（相关性评分：{ranked_path.relevance_score:.2f}）"
            path_descriptions.append(description)
        
        return "\n".join(path_descriptions)
    
    def build_enhanced_prompt(self, 
                             patient_info: str, 
                             reasoning_paths: List[RankedPath]) -> str:
        """
        构建增强的prompt，包含推理路径
        """
        paths_text = self.format_paths_for_llm(reasoning_paths)
        
        prompt = f"""
你是一位经验丰富的医生，需要根据患者的症状、体征、健康档案等信息，
结合以下医学推理路径，分析可能的疾病方向。

患者信息：
{patient_info}

医学推理路径（来自知识图谱，按相关性排序）：
{paths_text}

请根据以上医学推理路径，分析：
1. 可能的疾病方向（Top 3-5），按可能性排序
2. 每个方向的支持证据（症状、体征、检查结果）
3. 每个方向的反对证据
4. 还需要哪些信息来进一步判断
5. 建议做哪些检查来辅助诊断

注意：
- 优先考虑推理路径中提到的疾病方向
- 不要给出确诊结论，使用"可能"、"考虑"等表述
- 如果信息不足，明确说明
- 如果推理路径与患者情况不符，请说明原因

请以JSON格式返回：
{{
    "possibilities": {{
        "疾病1": 0.8,
        "疾病2": 0.6,
        "疾病3": 0.4
    }},
    "reasoning_paths_used": ["路径1", "路径2"],
    "supporting_evidence": {{
        "疾病1": ["症状1", "体征1"]
    }},
    "opposing_evidence": {{
        "疾病1": ["症状3"]
    }},
    "missing_info": ["检查X", "症状Y"],
    "recommended_tests": ["检查1", "检查2"]
}}
"""
        return prompt
```

### 3.2 多引擎融合诊断系统

#### 3.2.1 引擎架构

**五个核心引擎**：

1. **规则引擎**（Rule Engine）
   - 基于症状组合规则的快速匹配
   - 技术：规则库（MySQL/Oracle）+ 规则引擎（Drools/Python规则引擎）

2. **知识图谱引擎**（KG Engine）
   - 基于DR.KNOWS方法的推理路径诊断
   - 技术：Neo4j + 路径检索 + 三层评分

3. **统计模型引擎**（Statistical Engine）
   - 基于历史数据的概率预测
   - 技术：XGBoost/LightGBM + 特征工程

4. **大模型引擎**（LLM Engine）
   - 在路径约束下的深度推理
   - 技术：T5/ChatGPT/医学专用LLM + 路径注入

5. **鉴别诊断引擎**（Differential Engine）
   - 相似疾病的区分
   - 技术：鉴别诊断规则库 + 相似度计算

#### 3.2.2 推理子组组织

**技术实现**：
- **子组组织**：按系统来源、病程、诱因等维度组织候选方向
- **子组匹配**：基于患者信息匹配相关子组
- **子组推理**：一条关键问题能推动一个子组整体前移或后移

```python
class ReasoningSubgroupOrganizer:
    def __init__(self):
        self.subgroup_db = SubgroupDatabase()  # MySQL/Oracle
        
    def organize_subgroups(self, 
                          candidate_directions: List[Diagnosis],
                          patient_state: Dict) -> List[Subgroup]:
        """
        组织推理子组
        """
        subgroups = []
        
        # 1. 按维度组织
        by_system = self._group_by_system(candidate_directions)
        by_course = self._group_by_course(candidate_directions, patient_state)
        by_trigger = self._group_by_trigger(candidate_directions, patient_state)
        
        # 2. 选择最有效的组织方式
        best_dimension = self._select_best_dimension(
            by_system, by_course, by_trigger, patient_state
        )
        
        # 3. 为每个子组提取关键差异点
        for subgroup in best_dimension:
            subgroup.key_differences = self._extract_key_differences(
                subgroup, patient_state
            )
            subgroups.append(subgroup)
        
        return subgroups
    
    def _group_by_system(self, directions: List[Diagnosis]) -> List[Subgroup]:
        """
        按系统来源组织
        """
        system_groups = {}
        for direction in directions:
            system = direction.system_source  # 心血管/呼吸/代谢等
            if system not in system_groups:
                system_groups[system] = Subgroup(
                    name=f"{system}相关",
                    candidate_directions=[],
                    dimension="系统来源"
                )
            system_groups[system].candidate_directions.append(direction)
        
        return list(system_groups.values())
    
    def _extract_key_differences(self, 
                                 subgroup: Subgroup,
                                 patient_state: Dict) -> List[str]:
        """
        提取关键差异点
        """
        # 从差异点词库查询
        difference_points = []
        for direction in subgroup.candidate_directions:
            points = self.difference_library.get_difference_points(
                direction.diagnosis_name
            )
            difference_points.extend(points)
        
        # 选择能最快区分的差异点
        key_differences = self._select_key_differences(
            difference_points, subgroup
        )
        
        return key_differences
```

#### 3.2.3 分流路径设计

**技术实现**：
- **分流路径生成**：基于推理子组生成分流路径
- **第一层分叉问题**：生成用于快速分流子组的问题
- **高危路径优先**：对必须排除的高危方向建立优先路径

```python
class RoutingPathDesigner:
    def __init__(self):
        self.routing_template_db = RoutingTemplateDatabase()  # MySQL/Oracle
        
    def design_routing_path(self, 
                           subgroups: List[Subgroup],
                           cdp: CDP) -> RoutingPath:
        """
        设计分流路径
        """
        # 1. 生成第一层分叉问题
        first_layer_questions = self._generate_first_layer_questions(
            subgroups, cdp
        )
        
        # 2. 识别高危路径
        high_risk_path = self._identify_high_risk_path(subgroups, cdp)
        
        # 3. 生成正常路径
        normal_path = self._generate_normal_path(subgroups, cdp)
        
        return RoutingPath(
            first_layer_questions=first_layer_questions,
            high_risk_path=high_risk_path,
            normal_path=normal_path
        )
    
    def _generate_first_layer_questions(self, 
                                       subgroups: List[Subgroup],
                                       cdp: CDP) -> List[Question]:
        """
        生成第一层分叉问题
        """
        questions = []
        
        # 从分流路径模板库查询
        template = self.routing_template_db.query_template(
            chief_complaint=cdp.patient_state.chief_complaint
        )
        
        if template:
            # 使用模板生成问题
            for question_template in template.first_layer_questions:
                question = self._generate_question_from_template(
                    question_template, subgroups, cdp
                )
                questions.append(question)
        else:
            # 基于关键差异点生成问题
            for subgroup in subgroups:
                if subgroup.key_differences:
                    question = self._generate_question_from_difference(
                        subgroup.key_differences[0], subgroup, cdp
                    )
                    questions.append(question)
        
        return questions
```

**数据库设计**：

```sql
-- 推理子组表
CREATE TABLE reasoning_subgroup (
    id VARCHAR(64) PRIMARY KEY,
    subgroup_name VARCHAR(255),
    dimension VARCHAR(32),  -- 系统来源/病程/诱因等
    candidate_directions JSON,
    inclusion_reason TEXT,
    key_differences JSON,
    high_risk_triggers JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 分流路径模板库表
CREATE TABLE routing_path_template (
    id VARCHAR(64) PRIMARY KEY,
    chief_complaint VARCHAR(255),
    first_layer_questions JSON,
    high_risk_path JSON,
    normal_path JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_chief_complaint (chief_complaint)
);
```

#### 3.2.4 融合策略

**加权融合算法**：

```python
class MultiEngineFusion:
    def fuse_engine_results(self, 
                           engine_results: Dict[str, Dict],
                           patient_context: Dict) -> Dict:
        """
        融合多个引擎的诊断结果
        """
        # 引擎权重（可根据置信度动态调整）
        base_weights = {
            'rule_engine': 0.2,
            'kg_engine': 0.3,  # 知识图谱引擎权重较高
            'statistical_engine': 0.2,
            'llm_engine': 0.25,  # LLM引擎（在路径约束下）
            'differential_engine': 0.05
        }
        
        # 动态调整权重（基于引擎置信度）
        weights = self._adjust_weights(base_weights, engine_results)
        
        # 融合结果
        fused_possibilities = {}
        
        for engine_name, result in engine_results.items():
            weight = weights.get(engine_name, 0.1)
            possibilities = result.get('possibilities', {})
            
            for disease, confidence in possibilities.items():
                if disease not in fused_possibilities:
                    fused_possibilities[disease] = 0.0
                
                fused_possibilities[disease] += confidence * weight
        
        # 归一化
        total = sum(fused_possibilities.values())
        if total > 0:
            fused_possibilities = {
                k: v / total 
                for k, v in fused_possibilities.items()
            }
        
        # 排序，返回Top-5
        top_diseases = sorted(
            fused_possibilities.items(), 
            key=lambda x: x[1], 
            reverse=True
        )[:5]
        
        return {
            'possibilities': dict(top_diseases),
            'reasoning_paths': engine_results['kg_engine'].get('reasoning_paths', []),
            'engine_contributions': {
                engine_name: result.get('possibilities', {})
                for engine_name, result in engine_results.items()
            }
        }
    
    def _adjust_weights(self, base_weights: Dict, engine_results: Dict) -> Dict:
        """
        根据引擎置信度动态调整权重
        """
        weights = base_weights.copy()
        
        # 如果某个引擎置信度很低，降低其权重
        for engine_name, result in engine_results.items():
            confidence = result.get('confidence', 1.0)
            if confidence < 0.5:
                weights[engine_name] *= 0.5
        
        # 归一化
        total = sum(weights.values())
        weights = {k: v / total for k, v in weights.items()}
        
        return weights
```

**一致性检查**：

```python
class ConsistencyChecker:
    def check_consistency(self, engine_results: Dict[str, Dict]) -> Dict:
        """
        检查多个引擎结果的一致性
        """
        # 提取所有引擎的Top-3诊断
        top_diagnoses = {}
        for engine_name, result in engine_results.items():
            possibilities = result.get('possibilities', {})
            top_3 = sorted(possibilities.items(), 
                          key=lambda x: x[1], 
                          reverse=True)[:3]
            top_diagnoses[engine_name] = [d[0] for d in top_3]
        
        # 计算一致性
        consistency_score = self._calculate_consistency(top_diagnoses)
        
        # 识别冲突
        conflicts = self._identify_conflicts(engine_results)
        
        return {
            'consistency_score': consistency_score,
            'conflicts': conflicts,
            'is_consistent': consistency_score > 0.7
        }
```

### 3.3 5步AI循证诊断流程编排引擎（Diagnosis Workflow Orchestrator）

> **对应功能设计文档**：3.3 临床诊疗态详细流程（AI循证诊断流程）  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第3.3.1-3.3.6节  
> **参考文档**：《智能诊断A路径：AI循证诊断流程.md》  
> **核心思想**：将临床诊疗态流程组织为5步AI循证诊断流程，确保诊断过程可推理、可复用、可审计。

**职责**：编排5步AI循证诊断流程，协调各个脑区完成诊断任务

**技术实现**：

```python
class DiagnosisWorkflowOrchestrator:
    def __init__(self):
        self.parsing_service = ClinicalParsingService()  # 脑区A
        self.interview_service = DialogService()  # 脑区B
        self.ddx_engine = MultiEngineFusion()  # 脑区C
        self.workup_planner = WorkupPlanner()  # 脑区D
        self.treatment_engine = TreatmentEngine()  # 脑区E
        self.risk_assessment = RiskAssessmentEngine()  # 脑区F
        self.explanation_service = ExplanationService()  # 脑区G
        
    def execute_diagnosis_workflow(self, cdp: CDP) -> CDP:
        """
        执行5步AI循证诊断流程
        """
        # Step 1: 识别问题
        cdp = self.step1_identify_problem(cdp)
        
        # Step 2: 构建鉴别诊断候选集并分层
        cdp = self.step2_build_ddx_candidates(cdp)
        
        # Step 3: 组织候选集并建立分流路径
        cdp = self.step3_organize_routing_path(cdp)
        
        # Step 4: 采集关键证据并形成排序与验证计划
        cdp = self.step4_collect_evidence_and_plan(cdp)
        
        # Step 5: 回填证据并输出终点结论包
        cdp = self.step5_backfill_and_conclude(cdp)
        
        return cdp
    
    def step1_identify_problem(self, cdp: CDP) -> CDP:
        """
        Step 1: 识别问题
        目标：把用户的自然语言描述，转化成可推理、可复用、可审计的结构化"问题清单"
        """
        # 1. 概念归一化（脑区A）
        normalized_concepts = self.parsing_service.normalize_concepts(
            cdp.patient_state.user_input
        )
        
        # 2. 形成完整问题清单（脑区A）
        problem_list = self.parsing_service.build_problem_list(
            normalized_concepts,
            cdp.patient_state
        )
        
        # 3. 标记信息缺口（脑区B）
        information_gaps = self.interview_service.identify_gaps(
            problem_list, cdp
        )
        
        # 更新CDP
        cdp.patient_state.problem_list = problem_list
        cdp.uncertainty.missing_critical_info = information_gaps
        
        return cdp
    
    def step2_build_ddx_candidates(self, cdp: CDP) -> CDP:
        """
        Step 2: 构建鉴别诊断候选集并分层
        目标：建立该主诉的鉴别诊断全集，并按临床风险与证据强度分成三层
        """
        # 1. 生成鉴别诊断全集（脑区C）
        ddx_candidates = self.ddx_engine.generate_ddx_candidates(
            cdp.patient_state.problem_list
        )
        
        # 2. 三层分层（脑区C + 脑区F）
        three_layer_ddx = self.ddx_engine.three_layer_classification(
            ddx_candidates,
            cdp.patient_state,
            self.risk_assessment
        )
        
        # 更新CDP
        cdp.ddx = three_layer_ddx
        
        return cdp
    
    def step3_organize_routing_path(self, cdp: CDP) -> CDP:
        """
        Step 3: 组织候选集并建立分流路径
        目标：把分层候选清单组织成可推进的推理结构，并提炼出分流路径
        """
        # 1. 组织成推理子组（脑区C）
        reasoning_subgroups = self.ddx_engine.organize_subgroups(
            cdp.ddx,
            cdp.patient_state
        )
        
        # 2. 提炼关键差异点（脑区C）
        key_differences = self.ddx_engine.extract_key_differences(
            reasoning_subgroups
        )
        
        # 3. 形成分流路径清单（脑区B）
        routing_path = self.interview_service.design_routing_path(
            reasoning_subgroups,
            key_differences,
            cdp
        )
        
        # 更新CDP（临时字段，用于指导问诊）
        cdp.routing_path = routing_path
        
        return cdp
    
    def step4_collect_evidence_and_plan(self, cdp: CDP) -> CDP:
        """
        Step 4: 采集关键证据并形成排序与验证计划
        目标：系统采集能够"推动排序变化"的关键证据，形成稳定的三层清单，并制定验证计划
        """
        # 1. 采集关键证据（脑区B）
        evidence_list = self.interview_service.collect_key_evidence(
            cdp.routing_path,
            cdp
        )
        
        # 2. 固化三层排序（脑区C）
        three_layer_ranking = self.ddx_engine.solidify_three_layer_ranking(
            evidence_list,
            cdp.ddx
        )
        
        # 3. 制定验证计划（脑区D）
        verification_plan = self.workup_planner.generate_verification_plan(
            three_layer_ranking,
            cdp
        )
        
        # 更新CDP
        cdp.evidence_graph = evidence_list
        cdp.ddx = three_layer_ranking
        cdp.workup_plan = verification_plan
        
        return cdp
    
    def step5_backfill_and_conclude(self, cdp: CDP) -> CDP:
        """
        Step 5: 回填证据并输出终点结论包
        目标：将验证结果回填，更新三层排序，并输出可行动、可审计的终点结论包
        """
        # 1. 证据回填（脑区A）
        backfilled_evidence = self.parsing_service.backfill_evidence(
            cdp.workup_plan,
            cdp
        )
        
        # 2. 更新三层排序（脑区C）
        updated_ranking = self.ddx_engine.update_three_layer_ranking(
            backfilled_evidence,
            cdp.ddx
        )
        
        # 3. 生成终点结论包（脑区G + 脑区E + 脑区F）
        conclusion_package = self.explanation_service.generate_conclusion_package(
            updated_ranking,
            backfilled_evidence,
            self.treatment_engine,
            self.risk_assessment,
            cdp
        )
        
        # 更新CDP
        cdp.evidence_graph.extend(backfilled_evidence)
        cdp.ddx = updated_ranking
        cdp.conclusion_package = conclusion_package
        cdp.management_plan = conclusion_package.action_and_followup
        
        return cdp
```

**流程编排特点**：
- **可推理**：每个步骤都有明确的推理逻辑和依据
- **可复用**：问题清单、候选集、分流路径等可沉淀为模板库
- **可审计**：每个步骤的输出都有结构化记录，支持追溯和验证
- **可回退**：支持回退到任意步骤，确保系统不会在错误结论上"锁死"

**数据库设计**：

```sql
-- 诊断流程状态表
CREATE TABLE diagnosis_workflow_state (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64),
    current_step INT,  -- 1-5
    step_status VARCHAR(32),  -- pending/in_progress/completed
    step_output JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_cdp_id (cdp_id),
    INDEX idx_current_step (current_step)
);

-- 诊断流程回退记录表
CREATE TABLE diagnosis_workflow_rollback (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64),
    from_step INT,
    to_step INT,
    rollback_reason TEXT,
    rollback_condition VARCHAR(255),
    timestamp TIMESTAMP,
    INDEX idx_cdp_id (cdp_id)
);
```

### 3.4 检查建议引擎（Workup Planner）

#### 3.4.1 检查价值评估

**技术实现**：
- **信息增益计算**：评估检查能区分哪些DDx
- **检查必要性评估**：判断是否需要进一步检查
- **检查优先级排序**：根据紧急程度和信息增益排序

```python
class WorkupPlanner:
    def plan_workup(self, 
                   current_ddx: List[Diagnosis],
                   patient_state: Dict) -> List[WorkupItem]:
        """
        生成检查建议
        """
        workup_items = []
        
        for test in self.available_tests:
            # 1. 计算信息增益
            information_gain = self._calculate_information_gain(
                test, current_ddx, patient_state
            )
            
            # 2. 评估检查必要性
            necessity = self._assess_necessity(test, current_ddx, patient_state)
            
            # 3. 评估检查优先级
            priority = self._assess_priority(test, information_gain, necessity)
            
            # 4. 确定检查目的
            can_confirm = self._get_confirmable_ddx(test, current_ddx)
            can_exclude = self._get_excludable_ddx(test, current_ddx)
            
            if information_gain > 0.1 or necessity > 0.5:  # 阈值
                workup_items.append(WorkupItem(
                    test_name=test.name,
                    purpose=f"区分{can_confirm}和{can_exclude}",
                    priority=priority,
                    expected_gain=information_gain,
                    can_confirm=can_confirm,
                    can_exclude=can_exclude
                ))
        
        # 排序并返回
        workup_items.sort(key=lambda x: (x.priority, x.expected_gain), reverse=True)
        return workup_items[:5]  # 返回Top-5
```

#### 3.4.2 验证计划制定

**技术实现**：
- **验证计划生成**：针对"最可能方向"和"必须排除方向"生成验证计划
- **验证计划评估**：评估验证计划是否能改变排序或触发升级
- **验证计划执行**：执行验证计划并回填结果

```python
class VerificationPlanner:
    def __init__(self):
        self.verification_db = VerificationDatabase()  # MySQL/Oracle
        
    def generate_verification_plan(self, 
                                   cdp: CDP) -> List[VerificationPlan]:
        """
        生成验证计划
        """
        verification_plans = []
        
        # 1. 针对最可能方向生成验证计划
        most_likely = cdp.ddx[0]  # Top-1
        plan = self._generate_plan_for_direction(
            direction=most_likely,
            purpose="确认",
            cdp=cdp
        )
        if plan and self._can_change_ranking(plan, cdp):
            verification_plans.append(plan)
        
        # 2. 针对必须排除方向生成验证计划
        must_exclude = [d for d in cdp.ddx if d.must_exclude]
        for direction in must_exclude:
            plan = self._generate_plan_for_direction(
                direction=direction,
                purpose="排除",
                cdp=cdp
            )
            if plan and self._can_change_ranking(plan, cdp):
                verification_plans.append(plan)
        
        return verification_plans
    
    def _generate_plan_for_direction(self, 
                                     direction: Diagnosis,
                                     purpose: str,
                                     cdp: CDP) -> VerificationPlan:
        """
        为特定方向生成验证计划
        """
        # 从验证计划库查询
        plans = self.verification_db.query_plans(
            diagnosis=direction.diagnosis_name,
            purpose=purpose
        )
        
        # 选择最合适的验证计划
        best_plan = self._select_best_plan(plans, cdp)
        
        return best_plan
    
    def execute_verification(self, 
                            plan: VerificationPlan,
                            result: Dict) -> Dict:
        """
        执行验证计划并回填结果
        """
        # 1. 判定验证结果
        judgment = self._judge_result(plan, result)
        
        # 2. 执行回填规则
        if judgment == "support":
            # 执行支持动作
            action = plan.result_backfill_rule.if_support
            updated_cdp = self._apply_backfill_action(action, plan, result)
        elif judgment == "not_support":
            # 执行不支持动作
            action = plan.result_backfill_rule.if_not_support
            updated_cdp = self._apply_backfill_action(action, plan, result)
        else:  # uncertain
            # 执行不确定动作
            action = plan.result_backfill_rule.if_uncertain
            updated_cdp = self._apply_backfill_action(action, plan, result)
        
        return {
            "judgment": judgment,
            "updated_cdp": updated_cdp
        }
```

**数据库设计**：

```sql
-- 验证计划库表
CREATE TABLE verification_plan_library (
    id VARCHAR(64) PRIMARY KEY,
    diagnosis_name VARCHAR(255),
    verification_purpose VARCHAR(32),  -- 确认/排除/升级判定
    verification_action JSON,
    judgment_standard JSON,
    result_backfill_rule JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_diagnosis (diagnosis_name),
    INDEX idx_purpose (verification_purpose)
);
```

### 3.5 对话管理服务（Dialog Service）

#### 3.5.1 信息缺口识别

**技术实现**：
- **基于CDP的缺失信息识别**：从CDP的 `uncertainty.missing_critical_info` 获取
- **基于信息增益的问题生成**：优先问信息增益高的问题

```python
class GapFillingEngine:
    def identify_gaps(self, cdp: CDP) -> List[InformationGap]:
        """
        识别信息缺口
        """
        gaps = []
        
        # 1. 从CDP获取缺失信息
        missing_info = cdp.uncertainty.missing_critical_info
        
        # 2. 评估每个缺失信息的重要性
        for info in missing_info:
            importance = self._assess_importance(info, cdp.ddx)
            gaps.append(InformationGap(
                info_type=info,
                importance=importance
            ))
        
        # 排序
        gaps.sort(key=lambda x: x.importance, reverse=True)
        return gaps
    
    def generate_question(self, gap: InformationGap, cdp: CDP) -> str:
        """
        生成追问问题（基于信息增益）
        """
        # 1. 计算信息增益
        information_gain = self._calculate_information_gain(gap, cdp.ddx)
        
        # 2. 生成问题（使用LLM或模板）
        question = self.llm.generate_question(
            gap=gap,
            information_gain=information_gain,
            context=cdp.patient_state
        )
        
        return question
```

#### 3.5.2 主诉关键线索库

> **对应功能设计文档**：2.2.1 主诉关键线索库  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第2.2.1节（第532-569行）

**技术实现**：
- **线索库存储**：MySQL/Oracle存储线索库
- **线索匹配**：基于主诉和当前DDx匹配相关线索
- **线索应用**：根据线索类型执行推理动作

```python
class ChiefComplaintClueLibrary:
    def __init__(self):
        self.clue_db = ClueDatabase()  # MySQL/Oracle
        
    def get_clues(self, chief_complaint: str, current_ddx: List[Diagnosis]) -> List[Clue]:
        """
        获取主诉相关的关键线索
        """
        # 1. 从线索库查询
        clues = self.clue_db.query_clues(
            chief_complaint=chief_complaint,
            candidate_directions=[d.diagnosis_name for d in current_ddx]
        )
        
        # 2. 按线索类型分类
        difference_clues = [c for c in clues if c.clue_type == "差异点"]
        positive_clues = [c for c in clues if c.clue_type == "阳性线索"]
        negative_clues = [c for c in clues if c.clue_type == "关键阴性线索"]
        
        return {
            "difference_clues": difference_clues,
            "positive_clues": positive_clues,
            "negative_clues": negative_clues
        }
    
    def apply_clue(self, clue: Clue, answer: str, cdp: CDP) -> Dict:
        """
        应用线索，执行推理动作
        """
        # 1. 判定规则匹配
        judgment = self._match_judgment_rule(clue, answer)
        
        # 2. 执行推理动作
        if judgment == "support":
            action = clue.reasoning_action.if_support
            # 方向上移
            self._move_direction_up(action.direction, cdp)
        elif judgment == "not_support":
            action = clue.reasoning_action.if_not_support
            # 方向后移
            self._move_direction_down(action.direction, cdp)
        else:  # uncertain
            action = clue.reasoning_action.if_uncertain
            # 标记为缺口并进入验证计划
            self._mark_as_gap(action, cdp)
        
        return {
            "judgment": judgment,
            "action": action,
            "updated_cdp": cdp
        }
```

**数据库设计**：

```sql
-- 主诉关键线索库表
CREATE TABLE chief_complaint_clue_library (
    id VARCHAR(64) PRIMARY KEY,
    chief_complaint VARCHAR(255),
    clue_name VARCHAR(255),
    clue_type VARCHAR(32),  -- 差异点/阳性线索/关键阴性线索
    acquisition_method VARCHAR(32),  -- 追问/观察/测量
    standard_question TEXT,
    answer_options JSON,
    judgment_rule JSON,
    reasoning_action JSON,
    candidate_directions JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_chief_complaint (chief_complaint),
    INDEX idx_clue_type (clue_type)
);
```

#### 3.5.3 差异点词库与问法规范

> **对应功能设计文档**：2.2.2 差异点词库与问法规范  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第2.2.2节（第570-588行）

**技术实现**：
- **差异点词库**：存储可问、可观察或可测量的差异点
- **问法规范**：统一问法和答案选项
- **问法生成**：基于差异点生成标准问法

```python
class DifferencePointLibrary:
    def __init__(self):
        self.difference_db = DifferenceDatabase()  # MySQL/Oracle
        
    def get_difference_points(self, chief_complaint: str) -> List[DifferencePoint]:
        """
        获取主诉相关的差异点
        """
        return self.difference_db.query_difference_points(chief_complaint)
    
    def generate_standard_question(self, difference_point: DifferencePoint) -> str:
        """
        生成标准问法
        """
        # 使用模板或LLM生成标准问法
        if difference_point.question_template:
            question = difference_point.question_template.format(
                symptom=difference_point.related_symptom
            )
        else:
            question = self.llm.generate_question(difference_point)
        
        return question
    
    def normalize_answer(self, difference_point: DifferencePoint, user_answer: str) -> str:
        """
        归一化用户答案
        """
        # 匹配标准答案选项
        for option in difference_point.answer_options:
            if self._match_answer(user_answer, option):
                return option
        
        # 如果无法匹配，返回"不确定"
        return "不确定"
```

**数据库设计**：

```sql
-- 差异点词库表
CREATE TABLE difference_point_library (
    id VARCHAR(64) PRIMARY KEY,
    chief_complaint VARCHAR(255),
    difference_point_name VARCHAR(255),
    question_template TEXT,
    answer_options JSON,
    threshold_expression TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_chief_complaint (chief_complaint)
);
```

---

## 四、八大智能体详细设计

### 4.0 智能体0：健康状态判定智能体（Health State Assessment Agent）

**职责**：判断"这个人，现在需要被当成'病人'对待吗？"

> **这是医生的第一职责，发生在"诊断之前"**

**输入**：
- 用户描述的症状/不适（初始主诉）
- 基本信息（年龄、性别等）
- 健康档案（如有）

**输出**：健康状态判定
- 是否需要进入诊疗流程（`needs_clinical_mode`：true/false）
- 工作态选择（`work_mode`：wellness_mode / clinical_mode）
- 风险等级（L1/L2/L3/L4）
- 判定依据（`assessment_reason`）

**智能体能力**：

```json
{
  "agent_id": "agent_0",
  "agent_name": "健康状态判定智能体",
  "capabilities": {
    "perception": {
      "can_read_user_input": true,
      "can_read_basic_info": true,
      "can_read_health_profile": true
    },
    "reasoning": {
      "reasoning_type": "rule_based",
      "reasoning_engines": ["symptom_severity_analyzer", "risk_signal_detector", "red_flag_detector"],
      "channel": "channel_1",
      "channel_1_engines": ["rule_engine", "risk_analyzer"],
      "confidence_threshold": 0.7
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["work_mode", "risk_level"],
      "requires_consensus": false
    },
    "action": {
      "can_write_cdp": ["health_state_assessment"],
      "can_trigger_other_agents": ["agent_1", "agent_2"],
      "can_request_help": ["agent_6"],
      "can_send_messages": ["orchestrator", "broadcast"]
    }
  }
}
```

**核心功能**：
1. **AI诊断入口判定流程**（Step 1-5）：接收用户输入、识别症状、方向澄清、危险信号检查、路径输出
2. **健康状态评估**：症状严重程度评估、风险早筛、分诊决策
3. **路径切换支持**：支持健康管理态与临床诊疗态之间的动态切换

**工作流程**：

```
接收用户输入
    ↓
执行入口判定流程（Step 1-5）
    ↓
评估健康状态
    ↓
决策：wellness_mode 或 clinical_mode
    ↓
更新CDP.health_state_assessment
    ↓
如果wellness_mode → 通知协调器触发健康管理流程
    ↓
如果clinical_mode → 发送消息给病例理解智能体（agent_1）
```

**协作机制**：
- **请求帮助**：当不确定时，请求风险评估智能体（agent_6）协助
- **触发下游**：根据工作态触发不同的下游智能体
- **通知广播**：工作态切换时，广播通知所有智能体

### 4.1 智能体1：病例理解智能体（Clinical Parsing Agent）

**职责**：将非结构化的患者信息转换为结构化的临床要素

**输入**：
- 病历自由文本
- 对话内容
- 检查单
- 生命体征
- 既往史

**输出**：结构化临床要素
- 症状（症状名称、持续时间、严重度、诱因）
- 体征（客观检查结果）
- 既往史（疾病史、手术史）
- 用药史（当前用药、既往用药）
- 过敏史
- 检验异常（检查项目、异常值、异常程度）

**核心功能**：
1. **医学概念识别**：从文本中提取医学概念（症状、疾病、检查等）
2. **概念归一化**：将口语化表达转换为标准医学术语（CUI/ICD/SNOMED）
3. **结构化提取**：将自由文本转换为结构化数据
4. **多模态理解**：处理文本、影像、检查报告等多种数据

**智能体能力**：

```json
{
  "agent_id": "agent_1",
  "agent_name": "病例理解智能体",
  "capabilities": {
    "perception": {
      "can_read_user_input": true,
      "can_read_medical_records": true,
      "can_read_examination_reports": true,
      "can_read_cdp": true
    },
    "reasoning": {
      "reasoning_type": "llm_based",
      "reasoning_engines": ["medical_ner", "concept_normalizer", "structured_extractor"],
      "channel": "channel_1",
      "channel_1_engines": ["medical_ner", "concept_normalizer", "structured_extractor"],
      "confidence_threshold": 0.8
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["patient_state"],
      "requires_consensus": false
    },
    "action": {
      "can_write_cdp": ["patient_state"],
      "can_request_help": ["agent_2"],
      "can_trigger_other_agents": ["agent_2", "agent_3"]
    }
  }
}
```

**输出到CDP**：
- 更新 `patient_state` 字段，包含：症状、体征、既往史、用药史、过敏史、检验异常

### 4.2 智能体2：主动问诊智能体（Interview Agent）

**职责**：像医生一样问"关键问题"，补齐鉴别诊断所需证据

**输入**：
- 当前CDP（特别是 `ddx` 和 `uncertainty.missing_critical_info`）
- 对话历史

**输出**：问诊计划
- 下一问是什么
- 为什么问这个问题（基于临床决策分析）
- 期望得到什么信息
- 如何问（自然语言生成）

**核心功能**：
1. **信息缺口识别**：识别缺失的关键信息
2. **临床决策分析**：基于临床决策理论评估下一问的价值
3. **问诊策略生成**：决定问诊顺序和优先级
4. **自然语言生成**：生成像医生一样的追问问题

**智能体能力**：

```json
{
  "agent_id": "agent_2",
  "agent_name": "主动问诊智能体",
  "capabilities": {
    "perception": {
      "can_read_cdp": true,
      "can_read_ddx": true,
      "can_read_missing_info": true,
      "can_read_conversation_history": true
    },
    "reasoning": {
      "reasoning_type": "clinical_decision_analysis",
      "reasoning_engines": ["information_gap_identifier", "question_generator", "clinical_decision_analyzer"],
      "channel": "both",
      "channel_1_engines": ["information_gap_identifier", "clinical_decision_analyzer"],
      "channel_2_engines": ["question_generator", "nlg"],
      "confidence_threshold": 0.7
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["next_question", "question_priority"],
      "requires_consensus": false
    },
    "action": {
      "can_write_cdp": ["uncertainty.missing_critical_info"],
      "can_send_messages": ["user", "agent_3"],
      "can_request_help": ["agent_3"]
    }
  }
}
```

**临床决策分析驱动的问诊**（基于临床决策理论）：

```python
# 扩展的信息增益计算（基于临床决策理论）
IG_clinical(q|DDx, E) = IG_information(q|DDx, E) 
                      + α · IG_risk(q|DDx, E)
                      + β · IG_cost(q|DDx, E)

其中：
1. IG_information: 信息论的信息增益（原有）
2. IG_risk: 风险相关的信息增益（优先问能排除高危诊断的问题）
3. IG_cost: 成本效益相关的信息增益（考虑患者负担）

权重设置（基于医疗理论）：
- α = 0.5（风险权重较高，因为漏诊代价极大）
- β = 0.2（成本权重较低，但需要考虑患者体验）
- 信息增益权重 = 0.3（基础权重）
```

**输出到CDP**：
- 更新 `uncertainty.missing_critical_info`
- 生成问诊计划（临时字段，不持久化）

### 4.3 智能体3：鉴别诊断智能体（Differential Diagnosis Agent）

**职责**：生成Top-K鉴别诊断列表，每个诊断包含支持证据、反证、缺失证据

**输入**：
- 当前CDP的 `patient_state`
- 知识图谱推理路径（来自技术架构）

**输出**：鉴别诊断列表
- Top-K诊断（带排序/概率或置信区间）
- 每个诊断的支持证据（`pros`）
- 每个诊断的反证（`cons`）
- 每个诊断的缺失证据（`missing`）

**核心功能**：
1. **多引擎融合诊断**：
   - 规则引擎：症状组合规则匹配
   - 知识图谱引擎：基于推理路径的诊断（DR.KNOWS方法）
   - 统计模型引擎：基于历史数据的概率预测
   - 大模型引擎：在路径约束下的深度推理
   - 鉴别诊断引擎：相似疾病的区分

2. **证据分析**：
   - 支持证据识别和评分
   - 反对证据识别和评分
   - 缺失证据识别

3. **诊断排序**：
   - 基于多引擎融合结果
   - 考虑证据强度
   - 考虑不确定性

**智能体能力**：

```json
{
  "agent_id": "agent_3",
  "agent_name": "鉴别诊断智能体",
  "capabilities": {
    "perception": {
      "can_read_cdp": true,
      "can_read_patient_state": true,
      "can_read_evidence_graph": true,
      "can_read_kg_paths": true
    },
    "reasoning": {
      "reasoning_type": "multi_engine_fusion",
      "reasoning_engines": ["rule_engine", "kg_engine", "statistical_engine", "llm_engine", "differential_engine"],
      "channel": "channel_1",
      "channel_1_engines": ["rule_engine", "kg_engine", "statistical_engine", "differential_engine"],
      "confidence_threshold": 0.6
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["ddx_list", "diagnosis_ranking"],
      "requires_consensus": true
    },
    "action": {
      "can_write_cdp": ["ddx", "evidence_graph"],
      "can_trigger_other_agents": ["agent_2", "agent_4", "agent_6"],
      "can_request_consensus": ["agent_4", "agent_6"],
      "can_send_messages": ["agent_2"]
    }
  }
}
```

**推理子组组织**（基于诊断树理论）：

**医疗理论依据**：
1. **诊断树理论**（Diagnostic Tree Theory）- 诊断学中的系统化诊断方法
2. **解剖学分类**（Anatomical Classification）- 基于解剖学/生理学系统的分类
3. **病理生理机制**（Pathophysiological Mechanism）- 基于疾病机制的诊断
4. **时间特征诊断**（Temporal Pattern Diagnosis）- 基于起病方式和病程的诊断

**组织维度**（按诊断学优先级）：
1. **系统分类**（第一优先级）- 基于解剖学/生理学系统
2. **病理生理机制**（第二优先级）- 基于疾病机制
3. **起病方式与病程**（第三优先级）- 基于时间特征
4. **严重程度与风险**（贯穿所有层级）- 基于临床严重性

**输出到CDP**：
- 更新 `ddx` 字段（包含三层分层结果）
- 更新 `evidence_graph` 字段
- 生成分流路径（临时字段，用于指导问诊）

### 4.4 智能体4：检查建议智能体（Workup Planner Agent）

**职责**：基于当前DDx和已有证据，建议下一步检查，并评估检查的价值

**输入**：
- 当前CDP的 `ddx`
- 已有证据（`patient_state`）
- 风险等级（`triage`）

**输出**：检查建议
- 检查名称
- 检查目的（能确认/排除哪些DDx）
- 优先级（紧急/重要/可选）
- 预期信息增益（能区分哪些DDx）

**核心功能**：
1. **检查必要性评估**：判断是否需要进一步检查
2. **检查价值评估**：评估检查能提供多少信息增益
3. **检查优先级排序**：根据紧急程度和信息增益排序
4. **验证计划制定**：针对"最可能方向"和"必须排除方向"制定验证计划

**智能体能力**：

```json
{
  "agent_id": "agent_4",
  "agent_name": "检查建议智能体",
  "capabilities": {
    "perception": {
      "can_read_cdp": true,
      "can_read_ddx": true,
      "can_read_evidence_graph": true,
      "can_read_triage": true
    },
    "reasoning": {
      "reasoning_type": "value_based",
      "reasoning_engines": ["information_gain_calculator", "test_value_evaluator", "verification_planner"],
      "channel": "channel_1",
      "channel_1_engines": ["information_gain_calculator", "test_value_evaluator", "verification_planner"],
      "confidence_threshold": 0.7
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["workup_plan", "test_priority"],
      "requires_consensus": false
    },
    "action": {
      "can_write_cdp": ["workup_plan"],
      "can_trigger_other_agents": ["agent_3"],
      "can_request_help": ["agent_6"]
    }
  }
}
```

**输出到CDP**：
- 更新 `workup_plan` 字段（包含验证计划）

### 4.5 智能体5：治疗建议智能体（Management Planner Agent）

**职责**：基于诊断结果，生成治疗方案和处置建议

**输入**：
- 当前CDP的 `ddx`（特别是Top-1诊断）
- 患者状态（`patient_state`）
- 风险等级（`triage`）

**输出**：处置方案
- 对症处理建议
- 用药建议（不涉及具体剂量，研发阶段）
- 非药物治疗建议（生活方式、饮食、运动等）
- 观察/复诊建议
- 转诊建议（如需要）

**核心功能**：
1. **治疗方案推理**：基于疾病类型、严重程度，推理治疗方向
2. **药物推荐**：推荐药物类别（研发阶段不涉及具体剂量）
3. **非药物治疗**：生活方式、饮食、运动等建议
4. **治疗效果评估**：评估治疗方案的可能效果

**智能体能力**：

```json
{
  "agent_id": "agent_5",
  "agent_name": "治疗建议智能体",
  "capabilities": {
    "perception": {
      "can_read_cdp": true,
      "can_read_ddx": true,
      "can_read_patient_state": true,
      "can_read_triage": true
    },
    "reasoning": {
      "reasoning_type": "guideline_based",
      "reasoning_engines": ["treatment_reasoner", "drug_recommender", "lifestyle_advisor"],
      "channel": "both",
      "channel_1_engines": ["treatment_reasoner", "drug_recommender"],
      "channel_2_engines": ["lifestyle_advisor", "nlg"],
      "confidence_threshold": 0.7
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["management_plan"],
      "requires_consensus": false
    },
    "action": {
      "can_write_cdp": ["management_plan"],
      "can_request_help": ["agent_6"],
      "can_trigger_other_agents": ["agent_7"]
    }
  }
}
```

**输出到CDP**：
- 更新 `management_plan` 字段

### 4.6 智能体6：风险评估智能体（Triage Agent）

**职责**：识别高危情况，评估紧急程度，决定是否需要立即升级处理

**输入**：
- 当前CDP的 `patient_state`
- 当前DDx（`ddx`）

**输出**：风险评估
- 风险等级（L1极紧急 / L2紧急 / L3一般 / L4非紧急）
- 红旗信号列表
- 需要立即升级处理的条件

**核心功能**：
1. **高危识别**：识别需要立即就医的高危情况
2. **严重程度评估**：评估疾病的严重程度
3. **预后判断**：评估疾病预后情况
4. **紧急程度分级**：L1到L4分级

**安全原则**：
- **宁可误报，不能漏报**：高危识别召回率优先
- **及时升级**：一旦识别高危，立即建议就医
- **明确表达**：明确告知患者风险等级和处理建议

**智能体能力**：

```json
{
  "agent_id": "agent_6",
  "agent_name": "风险评估智能体",
  "capabilities": {
    "perception": {
      "can_read_cdp": true,
      "can_read_patient_state": true,
      "can_read_ddx": true,
      "can_monitor_continuously": true
    },
    "reasoning": {
      "reasoning_type": "risk_based",
      "reasoning_engines": ["high_risk_detector", "severity_assessor", "prognosis_predictor"],
      "channel": "channel_1",
      "channel_1_engines": ["high_risk_detector", "severity_assessor", "prognosis_predictor"],
      "confidence_threshold": 0.8
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["risk_level", "urgency", "upgrade_decision"],
      "requires_consensus": false,
      "priority": "high"
    },
    "action": {
      "can_write_cdp": ["triage"],
      "can_trigger_other_agents": ["agent_0"],
      "can_send_urgent_notifications": true,
      "can_interrupt_tasks": true
    }
  }
}
```

**输出到CDP**：
- 更新 `triage` 字段（包含复评与升级规则）

### 4.7 智能体7：证据链智能体（Evidence Agent）

**职责**：生成完整的证据链，让系统的"结论"能被复核，而不是黑箱

**输入**：
- 当前CDP的所有字段

**输出**：证据链
- 病历片段（支持诊断的原始信息）
- 检查异常（客观证据）
- KG路径（知识图谱推理路径）
- 指南/知识片段（医学知识依据）

**核心功能**：
1. **证据链构建**：将分散的证据组织成完整的证据链
2. **推理路径可视化**：可视化展示从症状到疾病的推理路径
3. **证据来源标注**：标注每个证据的来源和可信度
4. **不确定性表达**：明确表达诊断的不确定性来源

**可解释性要求**：
- 每个诊断结论都能追溯到证据
- 推理路径清晰可追溯
- 支持证据、反对证据、缺失证据明确
- 不确定性来源明确

**智能体能力**：

```json
{
  "agent_id": "agent_7",
  "agent_name": "证据链智能体",
  "capabilities": {
    "perception": {
      "can_read_cdp": true,
      "can_read_all_fields": true
    },
    "reasoning": {
      "reasoning_type": "evidence_synthesis",
      "reasoning_engines": ["evidence_chain_builder", "reasoning_path_visualizer", "explanation_generator"],
      "channel": "both",
      "channel_1_engines": ["evidence_chain_builder", "reasoning_path_visualizer"],
      "channel_2_engines": ["explanation_generator", "nlg"],
      "confidence_threshold": 0.7
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["evidence_graph", "explanation"],
      "requires_consensus": false
    },
    "action": {
      "can_write_cdp": ["evidence_graph"],
      "can_generate_explanation": true
    }
  }
}
```

**输出到CDP**：
- 更新 `evidence_graph` 字段（包含结构化证据清单）
- 更新 `uncertainty` 字段
- 生成终点结论包（包含四要素）

---

## 五、多智能体协作流程

### 5.1 健康状态判定 → 分叉 → 两种路径

```
┌─────────────────────────────────────────────────────────────┐
│  用户发起咨询                                                │
│  "我最近有点头痛"                                            │
└────────┬───────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│  阶段0：健康状态判定（Health State Assessment）             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  【协调器】分配任务给健康状态判定智能体（agent_0）    │  │
│  │  - 执行入口判定流程（Step 1-5）                       │  │
│  │  - 评估：头痛频率、严重程度、伴随症状                │  │
│  │  - 判断：症状在正常范围 vs 需要进一步检查            │  │
│  │  - 输出：work_mode = wellness_mode / clinical_mode  │  │
│  │  - 更新CDP.health_state_assessment                   │  │
│  └──────────────────────────────────────────────────────┘  │
└────────┬───────────────────────────────────────────────────┘
         │
         ▼
    ┌──────────────┬──────────────┐
    │ 非患者路径    │ 患者路径      │
    │（健康管理态） │（临床诊疗态） │
    └──────────────┴──────────────┘
         │                    │
         ▼                    ▼
┌─────────────────┐  ┌─────────────────────────────────────┐
│ 健康管理态流程   │  │ 临床诊疗态流程                       │
│                  │  │                                     │
│ 见4.2节详细流程  │  │ 见4.3节详细流程（5步AI循证诊断）    │
└─────────────────┘  └─────────────────────────────────────┘
```

### 5.2 健康管理态协作流程

**目标**：面向无症状的健康人群，提供健康筛查、体检规划、健康管理等服务

**流程结构**：
```
A1｜需求分类 → A2｜通用最小健康档案 → A3｜进入对应分支执行 → A4｜统一结果页输出 → A5｜随访闭环
```

**智能体协作流程**：

```
【协调器】根据wellness_mode分配任务
    ↓
【病例理解智能体（agent_1）】
    - 理解用户需求
    - 识别需求类型（1/2/3/4）
    - 更新CDP.patient_state
    ↓
【主动问诊智能体（agent_2）】（可选）
    - 如果信息缺失，补充追问
    ↓
【治疗建议智能体（agent_5）】（复用）
    - 生成健康管理计划
    - 更新CDP.wellness_plan
    ↓
【风险评估智能体（agent_6）】
    - 生成随访节奏规则
    - 更新CDP.triage
    ↓
【证据链智能体（agent_7）】
    - 生成统一结果页结构
    - 输出结果给用户
```

### 5.3 临床诊疗态协作流程（5步AI循证诊断流程）

> **对应功能设计文档**：3.3 临床诊疗态详细流程（AI循证诊断流程）  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第3.3.1-3.3.6节  
> **核心思想**：将临床诊疗态流程组织为5步AI循证诊断流程，确保诊断过程可推理、可复用、可审计。

**流程总览**：

```
Step 1: 识别问题
  ↓
Step 2: 构建鉴别诊断候选集并分层
  ↓
Step 3: 组织候选集并建立分流路径
  ↓
Step 4: 采集关键证据并形成排序与验证计划
  ↓
Step 5: 回填证据并输出终点结论包
```

**智能体协作流程**：

```
【协调器】根据clinical_mode分配任务
    ↓
Step 1: 识别问题
    【病例理解智能体（agent_1）】+ 【主动问诊智能体（agent_2）】
    - 执行概念归一化
    - 形成完整问题清单
    - 标记信息缺口
    ↓
Step 2: 构建鉴别诊断候选集并分层
    【鉴别诊断智能体（agent_3）】+ 【风险评估智能体（agent_6）】
    - 生成鉴别诊断全集
    - 三层分层：首要假设/主要备选/必须排除
    ↓
Step 3: 组织候选集并建立分流路径
    【鉴别诊断智能体（agent_3）】
    - 组织推理子组（基于诊断树理论）
    - 提炼关键差异点
    - 形成分流路径清单
    ↓
Step 4: 采集关键证据并形成排序与验证计划
    【主动问诊智能体（agent_2）】
    - 沿着分流路径采集关键证据
    【鉴别诊断智能体（agent_3）】
    - 固化三层排序
    【检查建议智能体（agent_4）】
    - 制定验证计划
    ↓
Step 5: 回填证据并输出终点结论包
    【检查建议智能体（agent_4）】
    - 回填检查结果
    【鉴别诊断智能体（agent_3）】
    - 更新三层排序
    【治疗建议智能体（agent_5）】
    - 生成治疗方案
    【证据链智能体（agent_7）】
    - 生成终点结论包（四要素）
```

### 5.4 协商流程示例

**场景**：鉴别诊断智能体和检查建议智能体对诊断有不同看法

```
【鉴别诊断智能体】
    - 生成诊断：心绞痛（概率0.7）
    - 发起协商请求
    ↓
【协调器】通知相关智能体
    ↓
【检查建议智能体】
    - 分析：建议做心电图
    - 提交观点：支持心绞痛，但需要排除心梗
    ↓
【风险评估智能体】
    - 分析：风险等级L2
    - 提交观点：支持优先排除心梗
    ↓
【协调器】执行协商算法
    - 加权投票
    - 证据融合
    ↓
【协商结果】
    - 共识：优先排除心梗
    - 更新CDP.ddx（添加必须排除项）
```

### 5.5 冲突解决流程示例

**场景**：多个智能体对同一诊断有不同概率评估

```
【鉴别诊断智能体】
    - 诊断A概率：0.8
    ↓
【检查建议智能体】
    - 诊断A概率：0.5（基于检查结果）
    ↓
【风险评估智能体】
    - 诊断A概率：0.9（基于风险分析）
    ↓
【协调器】检测到冲突
    ↓
【协调器】执行冲突解决策略
    - 策略：优先级规则 + 证据强度
    - 风险评估智能体优先级最高
    - 但检查建议智能体的证据是客观检查
    ↓
【协调器】执行专家投票
    - 相关智能体投票
    - 加权：风险评估(0.4) + 检查建议(0.4) + 鉴别诊断(0.2)
    ↓
【冲突解决结果】
    - 最终概率：0.65
    - 标记为"需要进一步验证"
```

---

## 六、CDP数据结构设计

### 6.1 CDP数据模型

> **对应功能设计文档**：1.3 核心数据结构：Clinical Decision Package (CDP)  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第1.3节（第68-254行）

**数据库设计**（Oracle/MySQL）：

> **说明**：CDP数据结构与《AI医生系统-系统功能设计.md》中定义的CDP字段结构完全对应，确保业务功能与技术实现的一致性。

```sql
-- CDP主表
CREATE TABLE cdp (
    id VARCHAR(64) PRIMARY KEY,
    patient_id VARCHAR(64),
    session_id VARCHAR(64),
    version INT,
    -- 健康状态判定结果（脑区0输出）
    -- 包含：工作态判定、风险等级、入口判定流程结果等
    -- 对应功能设计文档：2.0节 健康状态判定
    health_state_assessment JSON,
    -- 健康管理计划（健康管理态使用）
    -- 包含：风险管理、生活方式建议、随访计划、健康筛查路径（A路径）执行结果等
    -- 对应功能设计文档：3.2节 健康管理态详细流程
    wellness_plan JSON,
    -- 患者状态（脑区A输出）
    patient_state JSON,
    -- 鉴别诊断列表（脑区C输出，三层排序：首要假设/主要备选/必须排除）
    ddx JSON,
    -- 证据图（脑区G输出）
    evidence_graph JSON,
    -- 检查计划（脑区D输出）
    workup_plan JSON,
    -- 治疗计划（脑区E输出）
    management_plan JSON,
    -- 风险评估（脑区F输出）
    triage JSON,
    -- 不确定性信息
    uncertainty JSON,
    -- 审计信息（模型版本、提示词版本、知识版本、推理轨迹等）
    audit JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- CDP版本表（支持回放）
CREATE TABLE cdp_version (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64),
    version INT,
    change_type VARCHAR(32),  -- CREATE/UPDATE
    changed_fields JSON,
    reason TEXT,
    created_at TIMESTAMP
);
```

**字段详细说明**：

#### health_state_assessment 字段

**用途**：存储健康状态判定结果（脑区0的输出）

**数据结构**（对应功能设计文档第76-93行）：
```json
{
  "needs_clinical_mode": true,
  "work_mode": "wellness_mode" | "clinical_mode",
  "risk_level": "L1/L2/L3/L4",
  "assessment_reason": "判定依据",
  "symptom_severity": "正常/轻度/中度/重度",
  "early_risk_signals": ["早期风险信号列表"],
  "entry_assessment": {
    "user_input": "用户自然语言输入",
    "has_symptom": true | false,
    "symptom_status": "明确无症状/存在症状/不确定",
    "clarification_needed": true | false,
    "clarification_result": "A（健康筛查）/B（症状诊断）",
    "red_flags_hit": true | false,
    "red_flags_list": ["危险信号列表"],
    "path_selected": "A（健康筛查）/B（症状诊断）/退出线上流程"
  }
}
```

**更新时机**：
- CDP创建时：由健康状态判定服务（health-state-assessment-service）生成
- 工作态切换时：当从健康管理态升级到临床诊疗态时更新

**相关服务**：
- `HealthStateAssessmentService.assess_health_state()`：生成健康状态判定结果
- `EntryAssessmentModule.entry_assessment()`：生成入口判定流程结果

#### wellness_plan 字段

**用途**：存储健康管理计划（健康管理态使用）

**数据结构**（对应功能设计文档第94-181行）：
```json
{
  "risk_management": [
    {
      "risk_type": "风险类型",
      "risk_level": "风险等级",
      "management_advice": "管理建议"
    }
  ],
  "lifestyle_advice": ["生活方式建议"],
  "follow_up_plan": [
    {
      "follow_up_type": "随访类型",
      "timing": "随访时间",
      "purpose": "随访目的"
    }
  ],
  "reassurance": "安抚与解释文本",
  "upgrade_conditions": ["升级到临床诊疗态的条件"],
  "wellness_screening_path": {
    "demand_type": 1 | 2 | 3,
    "demand_type_name": "筛查建议/健康目标管理/计划性健康需求",
    "health_profile": {
      "basic_info": {...},
      "past_history": [...],
      "family_history": [...],
      "lifestyle": {...},
      "medications": [...],
      "missing_fields": [...]
    },
    "branch_result": {
      "screening_recommendations": [...],
      "abnormality_grading": {...},
      "action_plan": {...},
      "scenario_plan": {...}
    },
    "unified_result": {
      "summary": "一句话总结",
      "recommended_actions": [...],
      "not_recommended_actions": [...],
      "next_review_time": "下一次复查/更新时间点",
      "exit_conditions": [...]
    },
    "follow_up_schedule": {...}
  }
}
```

**更新时机**：
- 健康管理态流程执行时：由健康筛查服务（WellnessScreeningService）生成
- A路径（A1-A5）执行完成后：存储健康筛查路径的执行结果

**相关服务**：
- `WellnessManagementEngine.generate_wellness_plan()`：生成健康管理计划
- `WellnessScreeningService.wellness_screening_workflow()`：执行健康筛查流程（A路径）

**注意**：
- 仅在`work_mode = "wellness_mode"`时使用此字段
- 当升级到临床诊疗态时，此字段保留历史记录，但不再更新

### 6.2 CDP API设计

**RESTful API**：

```python
# CDP管理API
POST   /api/v1/cdp                    # 创建CDP
GET    /api/v1/cdp/{cdp_id}            # 获取CDP
PUT    /api/v1/cdp/{cdp_id}            # 更新CDP
GET    /api/v1/cdp/{cdp_id}/versions   # 获取CDP版本历史
GET    /api/v1/cdp/{cdp_id}/replay     # 回放CDP演变过程
```

### 6.3 证据清单结构化存储

**技术实现**：
- **证据清单表**：存储结构化的证据清单
- **证据关联**：证据与诊断方向的关联关系

```python
class EvidenceListManager:
    def __init__(self):
        self.evidence_db = EvidenceDatabase()  # MySQL/Oracle
        
    def add_evidence(self, 
                    evidence: Evidence,
                    cdp_id: str) -> str:
        """
        添加证据到证据清单
        """
        evidence_id = self.evidence_db.insert_evidence({
            "cdp_id": cdp_id,
            "evidence_name": evidence.evidence_name,
            "evidence_source": evidence.evidence_source,
            "evidence_result": evidence.evidence_result,
            "evidence_direction": evidence.evidence_direction,
            "affected_direction": evidence.affected_direction,
            "evidence_strength": evidence.evidence_strength,
            "timestamp": datetime.now()
        })
        
        return evidence_id
    
    def get_evidence_list(self, cdp_id: str) -> List[Evidence]:
        """
        获取证据清单
        """
        return self.evidence_db.query_evidence_list(cdp_id)
```

**数据库设计**：

```sql
-- 证据清单表
CREATE TABLE evidence_list (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64),
    evidence_name VARCHAR(255),
    evidence_source VARCHAR(32),  -- 追问/观察/设备测量/线下检查
    evidence_result TEXT,
    evidence_direction VARCHAR(32),  -- 支持/不支持/不确定
    affected_direction VARCHAR(255),
    evidence_strength VARCHAR(32),  -- 强证据/中证据/弱证据
    timestamp TIMESTAMP,
    created_at TIMESTAMP,
    INDEX idx_cdp_id (cdp_id),
    INDEX idx_evidence_direction (evidence_direction)
);
```

### 6.4 回填与重排规则引擎

**技术实现**：
- **重排规则库**：存储重排规则
- **重排引擎**：根据新信息执行重排规则
- **证据冲突处理**：处理证据冲突并触发回退

```python
class RerankRuleEngine:
    def __init__(self):
        self.rerank_rule_db = RerankRuleDatabase()  # MySQL/Oracle
        
    def rerank_ddx(self, 
                   new_evidence: Evidence,
                   cdp: CDP) -> CDP:
        """
        根据新证据重新排序DDx
        """
        # 1. 获取重排规则
        rules = self.rerank_rule_db.query_rules(
            evidence_type=new_evidence.evidence_name,
            evidence_direction=new_evidence.evidence_direction
        )
        
        # 2. 应用重排规则
        updated_ddx = cdp.ddx.copy()
        for rule in rules:
            if rule.condition.match(new_evidence):
                # 执行重排动作
                if rule.action.type == "move_up":
                    updated_ddx = self._move_direction_up(
                        rule.action.direction,
                        rule.action.weight_adjustment,
                        updated_ddx
                    )
                elif rule.action.type == "move_down":
                    updated_ddx = self._move_direction_down(
                        rule.action.direction,
                        rule.action.weight_adjustment,
                        updated_ddx
                    )
        
        # 3. 检查证据冲突
        conflicts = self._check_evidence_conflicts(new_evidence, cdp)
        if conflicts:
            # 触发回退
            cdp = self._trigger_rollback(conflicts, cdp)
        
        # 4. 更新三层排序
        cdp.ddx = self._update_three_layer_ranking(updated_ddx)
        
        return cdp
```

**数据库设计**：

```sql
-- 回填与重排规则表
CREATE TABLE rerank_rule (
    id VARCHAR(64) PRIMARY KEY,
    evidence_type VARCHAR(255),
    evidence_direction VARCHAR(32),
    condition JSON,
    action JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_evidence_type (evidence_type)
);
```

### 6.5 回退机制引擎

**技术实现**：
- **回退条件检测**：检测是否满足回退条件
- **回退执行**：执行回退到指定阶段
- **回退记录**：记录回退原因和动作

```python
class RollbackEngine:
    def __init__(self):
        self.rollback_condition_db = RollbackConditionDatabase()  # MySQL/Oracle
        
    def check_rollback_conditions(self, cdp: CDP) -> List[RollbackCondition]:
        """
        检查回退条件
        """
        conditions = []
        
        # 1. 检查证据冲突
        if self._has_evidence_conflict(cdp):
            conditions.append(RollbackCondition(
                condition="证据冲突",
                rollback_target_step="Step 4（采集关键证据并形成排序与验证计划）",
                rollback_reason="需要重新采集证据，重新分析支持/反对证据",
                rollback_action="回到 Step 4，重新采集证据与制定验证计划"
            ))
        
        # 2. 检查症状演变
        if self._has_symptom_evolution(cdp):
            conditions.append(RollbackCondition(
                condition="症状演变",
                rollback_target_step="Step 3（组织候选集并建立分流路径）",
                rollback_reason="症状变化需要重新组织分流路径，重新生成候选集",
                rollback_action="回到 Step 3，重新组织分流路径"
            ))
        
        # 3. 检查处理无效
        if self._has_treatment_ineffective(cdp):
            conditions.append(RollbackCondition(
                condition="处理无效",
                rollback_target_step="Step 4（采集关键证据并形成排序与验证计划）",
                rollback_reason="处理无效说明诊断方向可能错误，需要重新采集证据",
                rollback_action="回到 Step 4，重新采集证据与制定验证计划"
            ))
        
        # 4. 检查必须排除项未完成
        if self._has_must_exclude_not_completed(cdp):
            conditions.append(RollbackCondition(
                condition="必须排除项未完成排除",
                rollback_target_step="Step 4（采集关键证据并形成排序与验证计划）",
                rollback_reason="必须排除的高危诊断未完成排除，需要继续验证",
                rollback_action="回到 Step 4，重新制定验证计划或强调必须执行验证"
            ))
        
        return conditions
    
    def execute_rollback(self, 
                        condition: RollbackCondition,
                        cdp: CDP) -> CDP:
        """
        执行回退
        """
        # 1. 记录回退信息
        self._record_rollback(condition, cdp)
        
        # 2. 根据回退目标步骤执行回退
        if "Step 3" in condition.rollback_target_step:
            # 重新组织分流路径
            cdp = self._reorganize_routing_path(cdp)
        elif "Step 4" in condition.rollback_target_step:
            # 重新采集证据与制定验证计划
            cdp = self._recollect_evidence(cdp)
        elif "Step 1" in condition.rollback_target_step:
            # 重新识别问题
            cdp = self._reidentify_problem(cdp)
        
        return cdp
```

**数据库设计**：

```sql
-- 回退条件清单表
CREATE TABLE rollback_condition_list (
    id VARCHAR(64) PRIMARY KEY,
    condition_name VARCHAR(255),
    trigger_rules JSON,
    rollback_target_stage VARCHAR(32),
    rollback_reason TEXT,
    rollback_action TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 回退记录表
CREATE TABLE rollback_record (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64),
    rollback_condition VARCHAR(255),
    rollback_target_stage VARCHAR(32),
    rollback_reason TEXT,
    rollback_action TEXT,
    timestamp TIMESTAMP,
    INDEX idx_cdp_id (cdp_id)
);
```

### 6.6 复评与升级规则引擎

> **对应功能设计文档**：2.6.1 复评与升级规则库  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第2.6.1节（第991-1050行）

**技术实现**：
- **复评规则库**：存储复评规则
- **复评时间计算**：计算复评时间窗
- **升级条件检测**：检测升级触发条件

```python
class ReviewAndUpgradeEngine:
    def __init__(self):
        self.review_rule_db = ReviewRuleDatabase()  # MySQL/Oracle
        
    def generate_review_plan(self, 
                            diagnosis: Diagnosis,
                            cdp: CDP) -> ReviewPlan:
        """
        生成复评计划
        """
        # 1. 查询复评规则
        rule = self.review_rule_db.query_rule(
            diagnosis_name=diagnosis.diagnosis_name
        )
        
        if not rule:
            # 使用默认规则
            rule = self._get_default_rule()
        
        # 2. 计算复评时间窗
        review_time_window = self._calculate_review_time_window(
            rule, cdp
        )
        
        # 3. 检查提前复评条件
        early_review = self._check_early_review_conditions(
            rule, cdp
        )
        
        # 4. 检查延迟复评条件
        delay_review = self._check_delay_review_conditions(
            rule, cdp
        )
        
        return ReviewPlan(
            default_time=review_time_window,
            early_review_conditions=early_review,
            delay_review_conditions=delay_review,
            upgrade_conditions=rule.upgrade_conditions
        )
    
    def check_upgrade_conditions(self, 
                                cdp: CDP) -> List[UpgradeCondition]:
        """
        检查升级触发条件
        """
        upgrade_conditions = []
        
        # 从复评规则库查询升级条件
        rules = self.review_rule_db.query_all_rules()
        
        for rule in rules:
            for condition in rule.upgrade_conditions:
                if self._match_condition(condition, cdp):
                    upgrade_conditions.append(condition)
        
        return upgrade_conditions
```

**数据库设计**：

```sql
-- 复评与升级规则库表
CREATE TABLE review_and_upgrade_rule (
    id VARCHAR(64) PRIMARY KEY,
    diagnosis_name VARCHAR(255),
    default_review_time VARCHAR(64),
    review_trigger TEXT,
    early_review_conditions JSON,
    delay_review_conditions JSON,
    upgrade_conditions JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_diagnosis (diagnosis_name)
);
```

### 6.7 终点结论包生成引擎

**技术实现**：
- **四要素生成**：生成结论、必须排除项状态、关键依据、行动与随访
- **结论包格式化**：格式化输出结论包

```python
class ConclusionPackageGenerator:
    def generate_conclusion_package(self, cdp: CDP) -> ConclusionPackage:
        """
        生成终点结论包（四要素）
        """
        # 要素1：结论
        conclusion = self._generate_conclusion(cdp)
        
        # 要素2：必须排除项状态
        must_exclude_status = self._generate_must_exclude_status(cdp)
        
        # 要素3：关键依据
        key_evidence = self._generate_key_evidence(cdp)
        
        # 要素4：行动与随访
        action_and_followup = self._generate_action_and_followup(cdp)
        
        return ConclusionPackage(
            conclusion=conclusion,
            must_exclude_status=must_exclude_status,
            key_evidence=key_evidence,
            action_and_followup=action_and_followup
        )
    
    def _generate_conclusion(self, cdp: CDP) -> Conclusion:
        """
        生成结论
        """
        top_diagnosis = cdp.ddx[0]
        
        # 判断是否可确证
        if top_diagnosis.probability >= 0.7 and len(cdp.evidence_list) >= 3:
            return Conclusion(
                type="可确证",
                diagnosis=top_diagnosis.diagnosis_name,
                confidence=top_diagnosis.probability,
                severity=self._assess_severity(top_diagnosis, cdp),
                evidence_chain=self._build_evidence_chain(cdp)
            )
        else:
            return Conclusion(
                type="不可确证",
                most_likely_direction=top_diagnosis.diagnosis_name,
                uncertainty_source=self._identify_uncertainty_source(cdp),
                missing_evidence=cdp.uncertainty.missing_critical_info
            )
    
    def _generate_key_evidence(self, cdp: CDP) -> KeyEvidence:
        """
        生成关键依据（至少三条证据）
        """
        evidence_list = cdp.evidence_list
        
        # 筛选强证据和中证据
        strong_evidence = [
            e for e in evidence_list 
            if e.evidence_strength in ["强证据", "中证据"]
        ]
        
        # 至少三条证据
        if len(strong_evidence) >= 3:
            positive_evidence = [
                e for e in strong_evidence 
                if e.evidence_direction == "支持"
            ]
            negative_evidence = [
                e for e in strong_evidence 
                if e.evidence_direction == "不支持"
            ]
        else:
            # 如果强证据不足，使用所有证据
            positive_evidence = [
                e for e in evidence_list 
                if e.evidence_direction == "支持"
            ]
            negative_evidence = [
                e for e in evidence_list 
                if e.evidence_direction == "不支持"
            ]
        
        return KeyEvidence(
            positive_evidence=positive_evidence[:3],  # 至少三条
            negative_evidence=negative_evidence,
            check_results=self._extract_check_results(cdp)
        )
```

**数据库设计**：

```sql
-- 终点结论包表
CREATE TABLE conclusion_package (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64),
    conclusion JSON,
    must_exclude_status JSON,
    key_evidence JSON,
    action_and_followup JSON,
    created_at TIMESTAMP,
    INDEX idx_cdp_id (cdp_id)
);
```

---

## 七、CDP流转与状态管理

### 7.1 CDP生命周期

**CDP生命周期流程**：

```
CDP创建（用户发起咨询）
    ↓
阶段0：健康状态判定
    - 【协调器】分配任务给健康状态判定智能体（agent_0）
    - 判定是否需要进入诊疗流程
    - 更新 health_state_assessment
    - 设置 work_mode
    ↓
    ┌──────────────┬──────────────┐
    │ 健康管理态    │ 临床诊疗态    │
    └──────────────┴──────────────┘
         │                    │
         ▼                    ▼
CDP更新（健康管理）    CDP更新（信息收集）
    - 病例理解智能体（agent_1）更新 patient_state
    - 生成 wellness_plan
    - 持续监控          - 病例理解智能体（agent_1）更新 patient_state
         │              - 主动问诊智能体（agent_2）生成问诊计划
         │                   ↓
         │              CDP更新（诊断阶段）
         │              - 鉴别诊断智能体（agent_3）更新 ddx
         │              - 检查建议智能体（agent_4）更新 workup_plan
         │              - 风险评估智能体（agent_6）更新 triage
         │                   ↓
         │              CDP更新（处置阶段）
         │              - 治疗建议智能体（agent_5）更新 management_plan
         │              - 证据链智能体（agent_7）更新 evidence_graph
         │                   ↓
         │              CDP持久化（诊断完成）
         │                   ↓
         │              CDP更新（随访阶段）
         │              - 重新运行相关智能体
         │              - 更新CDP状态
         │
         └──────────→ 升级触发（症状加重/风险信号）
                      - 更新 work_mode = clinical_mode
                      - 进入临床诊疗态流程
```

### 7.2 CDP版本管理

**版本控制机制**：

1. **版本创建**：
   - 每次CDP更新都创建新版本
   - 版本号格式：`CDP_v{timestamp}_{version}`
   - 记录版本创建原因和来源智能体

2. **版本历史**：
   - 保留所有历史版本，支持回放
   - 记录每次更新的智能体、时间戳、更新原因
   - 支持版本对比和差异分析

3. **版本回滚**：
   - 支持回滚到任意历史版本
   - 记录回滚原因和操作者
   - 回滚后重新触发相关智能体

**审计信息**：

```json
{
  "audit": {
    "cdp_version": "CDP_v20250101_001",
    "model_version": "模型版本",
    "prompt_version": "提示词版本",
    "knowledge_version": "知识版本",
    "timestamp": "时间戳",
    "reasoning_trace": "推理轨迹",
    "agent_activities": [
      {
        "agent_id": "agent_0",
        "activity": "执行的任务",
        "timestamp": "时间戳",
        "input": "输入",
        "output": "输出",
        "cdp_fields_updated": ["health_state_assessment"]
      }
    ],
    "version_history": [
      {
        "version": "CDP_v20250101_001",
        "created_by": "agent_0",
        "created_at": "时间戳",
        "reason": "健康状态判定完成",
        "changes": ["health_state_assessment"]
      }
    ]
  }
}
```

### 7.3 CDP状态转换

**状态定义**：

1. **初始状态**（`initial`）：
   - CDP刚创建，只有用户输入
   - 等待健康状态判定

2. **健康管理态**（`wellness_mode`）：
   - 健康状态判定完成，进入健康管理流程
   - CDP包含 `wellness_plan`，不包含 `ddx`

3. **临床诊疗态-信息收集**（`clinical_mode_collecting`）：
   - 进入临床诊疗态，正在收集信息
   - CDP包含 `patient_state`，可能包含部分 `ddx`

4. **临床诊疗态-诊断中**（`clinical_mode_diagnosing`）：
   - 正在执行诊断推理
   - CDP包含完整的 `ddx` 和 `evidence_graph`

5. **临床诊疗态-处置中**（`clinical_mode_managing`）：
   - 诊断完成，正在生成处置方案
   - CDP包含 `management_plan` 和 `final_conclusion`

6. **完成状态**（`completed`）：
   - 诊断流程完成，输出终点结论包
   - CDP包含完整的 `final_conclusion`

7. **随访状态**（`follow_up`）：
   - 进入随访阶段，等待复评
   - CDP可能根据新信息更新

**状态转换规则**：

```
initial
    ↓ (健康状态判定完成)
wellness_mode 或 clinical_mode_collecting
    ↓ (如果是临床诊疗态，信息收集完成)
clinical_mode_diagnosing
    ↓ (诊断完成)
clinical_mode_managing
    ↓ (处置方案生成完成)
completed
    ↓ (进入随访)
follow_up
    ↓ (可能触发升级或回退)
clinical_mode_collecting (升级) 或 任意之前状态 (回退)
```

### 7.4 CDP并发控制

**并发访问控制**：

1. **读写锁机制**：
   - 多个智能体可以同时读取CDP
   - 写入CDP需要获取写锁
   - 写锁按智能体优先级分配

2. **冲突检测**：
   - 检测多个智能体同时写入同一字段
   - 使用版本号检测并发冲突
   - 冲突时触发协商机制

3. **事务保证**：
   - CDP更新使用事务保证原子性
   - 更新失败时回滚
   - 记录所有更新操作

### 7.5 CDP持久化策略

**持久化时机**：

1. **关键节点持久化**：
   - 健康状态判定完成
   - 诊断完成
   - 终点结论包生成
   - 状态转换时

2. **定期持久化**：
   - 每隔一定时间自动持久化
   - 防止数据丢失

3. **手动持久化**：
   - 支持手动触发持久化
   - 用于重要节点保存

**持久化存储**：

- 支持多种存储后端（数据库、文件系统、对象存储）
- 支持CDP压缩和归档
- 支持CDP查询和检索

### 7.6 智能体状态管理

**智能体状态**：

每个智能体维护以下状态：

```json
{
  "agent_id": "agent_3",
  "state": {
    "status": "working" | "idle" | "waiting" | "error",
    "current_task": {
      "task_id": "task_123",
      "task_type": "diagnosis",
      "started_at": "2025-01-01T10:00:00Z",
      "deadline": "2025-01-01T10:05:00Z",
      "progress": 0.6
    },
    "task_queue": [],
    "collaboration_history": [],
    "performance_metrics": {
      "tasks_completed": 100,
      "success_rate": 0.95,
      "average_response_time": 2.5,
      "collaboration_count": 50
    }
  }
}
```

**状态同步机制**：

1. **主动推送**：智能体状态变化时，主动推送给协调器
2. **定期轮询**：协调器定期轮询智能体状态
3. **事件驱动**：通过事件总线同步状态

**状态一致性保证**：

- 使用分布式锁保证状态一致性
- 使用版本号防止状态冲突
- 使用事务保证状态更新的原子性

---

## 八、项目结构设计

> **设计原则**：基于服务架构设计，按照功能模块（脑区）来组织项目结构，每个服务对应一个脑区，职责单一明确。  
> **参考文档**：《AI医生系统-系统功能设计.md》第2章 八大临床脑区功能设计

### 8.1 项目结构设计原则

#### 8.1.1 服务拆分原则

**原则1：按脑区拆分服务**
- 每个服务对应一个临床脑区
- 服务职责单一，边界清晰
- 服务可以独立开发、测试、部署

**原则2：服务与智能体一一对应**
- 每个服务实现一个智能体的功能
- 服务内部可以包含多个智能体实例（负载均衡）
- 智能体通过服务API对外提供服务

**原则3：技术栈选择**
- Java服务：使用Spring Boot框架，适合复杂业务逻辑
- Python服务：使用FastAPI框架，适合AI/ML模型调用
- 数据库：MySQL/Oracle存储业务数据，Neo4j存储知识图谱

#### 8.1.2 项目目录结构

**根目录结构**：

```
AIdoctor/
├── clinical-parsing-service/          # 脑区A：病例理解服务（Python）
├── diagnosis-engine-service/           # 脑区C：鉴别诊断服务（Python）
├── dialog-service/                    # 脑区B：主动问诊服务（Python）
├── workup-planner-service/            # 脑区D：检查建议服务（Python）
├── treatment-engine-service/          # 脑区E：治疗建议服务（Python）
├── risk-assessment-service/           # 脑区F：风险评估服务（Python）
├── explanation-service/               # 脑区G：证据链服务（Python）
├── health-state-assessment-service/   # 脑区0：健康状态判定服务（Python）
├── diagnosis-service/                 # 协调器服务（Java）
├── examination-service/              # 检查服务（Java）
├── ocr-service/                      # OCR服务（Python）
├── frontend/                         # 前端服务（React/TypeScript）
├── docker-compose.yml                # Docker编排文件
└── docs/                             # 文档目录
```

#### 8.1.3 单个服务目录结构（Python服务示例）

**Python服务标准结构**：

```
service-name/
├── app/
│   ├── __init__.py
│   ├── main.py                      # FastAPI应用入口
│   ├── api/                         # API路由
│   │   ├── __init__.py
│   │   └── routes.py
│   ├── services/                    # 业务逻辑层
│   │   ├── __init__.py
│   │   └── service_name_service.py
│   ├── models/                      # 数据模型
│   │   ├── __init__.py
│   │   ├── request.py
│   │   └── response.py
│   ├── config/                      # 配置管理
│   │   ├── __init__.py
│   │   └── settings.py
│   └── utils/                       # 工具类
│       ├── __init__.py
│       └── exceptions.py
├── tests/                           # 测试代码
├── requirements.txt                 # Python依赖
├── Dockerfile                       # Docker镜像构建
├── README.md                        # 服务说明文档
└── run.py                           # 服务启动脚本
```

**Java服务标准结构**：

```
service-name/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/aidoctor/service/
│   │   │       ├── ServiceApplication.java
│   │   │       ├── controller/      # REST控制器
│   │   │       ├── service/         # 业务逻辑层
│   │   │       ├── repository/     # 数据访问层
│   │   │       ├── entity/         # 实体类
│   │   │       ├── dto/            # 数据传输对象
│   │   │       └── config/         # 配置类
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-dev.yml
│   └── test/
│       └── java/
├── pom.xml                          # Maven依赖管理
├── Dockerfile
└── README.md
```

#### 8.1.4 服务与代码模块对应关系

| 服务 | 对应脑区 | 主要代码模块 | 技术栈 |
|------|---------|------------|--------|
| health-state-assessment-service | 脑区0 | `app/services/health_state_service.py` | Python/FastAPI |
| clinical-parsing-service | 脑区A | `app/services/parsing_service.py` | Python/FastAPI |
| dialog-service | 脑区B | `app/services/dialog_service.py` | Python/FastAPI |
| diagnosis-engine-service | 脑区C | `app/services/diagnosis_service.py`<br>`app/engines/` | Python/FastAPI |
| workup-planner-service | 脑区D | `app/services/workup_service.py` | Python/FastAPI |
| treatment-engine-service | 脑区E | `app/services/treatment_service.py` | Python/FastAPI |
| risk-assessment-service | 脑区F | `app/services/risk_service.py` | Python/FastAPI |
| explanation-service | 脑区G | `app/services/explanation_service.py` | Python/FastAPI |
| diagnosis-service | 协调器 | `com.aidoctor.diagnosis.orchestrator` | Java/Spring Boot |

#### 8.1.5 数据库设计对应关系

**数据库表与CDP字段对应**：

| CDP字段（功能设计文档） | 数据库表（技术架构文档） | 说明 |
|----------------------|----------------------|------|
| `health_state_assessment` | `cdp.health_state_assessment` (JSON) | 健康状态判定结果 |
| `wellness_plan` | `cdp.wellness_plan` (JSON) | 健康管理计划 |
| `patient_state` | `cdp.patient_state` (JSON) | 患者状态 |
| `ddx` | `cdp.ddx` (JSON) | 鉴别诊断列表 |
| `evidence_graph` | `cdp.evidence_graph` (JSON) | 证据图 |
| `workup_plan` | `cdp.workup_plan` (JSON) | 检查计划 |
| `management_plan` | `cdp.management_plan` (JSON) | 治疗计划 |
| `triage` | `cdp.triage` (JSON) | 风险评估 |
| `uncertainty` | `cdp.uncertainty` (JSON) | 不确定性信息 |
| `audit` | `cdp.audit` (JSON) | 审计信息 |

**知识库表设计**：

| 知识库 | 数据库表 | 对应功能设计文档章节 |
|--------|---------|-------------------|
| 主诉关键线索库 | `chief_complaint_clue_library` | 2.2.1 主诉关键线索库 |
| 差异点词库 | `difference_point_library` | 2.2.2 差异点词库与问法规范 |
| 分流路径模板库 | `routing_path_template` | 2.3.3 分流路径模板库 |
| 复评与升级规则库 | `review_and_upgrade_rule` | 2.6.1 复评与升级规则库 |
| 验证计划库 | `verification_plan_library` | 2.4.1 验证计划制定 |

#### 8.1.6 从文档到代码的实现路径

**实现路径示例：从功能设计到代码实现**

```
功能设计文档（2.2 脑区B：主动问诊）
    ↓
技术架构文档（4.2 智能体2 + 3.5 对话管理服务）
    ↓
服务设计（dialog-service）
    ↓
代码实现：
  - app/services/dialog_service.py（业务逻辑）
  - app/api/routes.py（API接口）
  - app/models/request.py（请求模型）
  - app/models/response.py（响应模型）
```

**具体实现步骤**：

1. **阅读功能设计文档**：理解脑区B的功能定义和职责
2. **阅读技术架构文档**：理解智能体2的实现方式和对话管理服务的技术细节
3. **设计服务接口**：定义API接口（RESTful API）
4. **实现业务逻辑**：实现信息缺口识别、问诊生成等功能
5. **实现数据访问**：访问主诉关键线索库、差异点词库等
6. **编写测试**：编写单元测试和集成测试

#### 8.1.7 项目搭建检查清单

**基于两个文档搭建项目的检查清单**：

- [ ] **服务拆分**：是否按照8个服务拆分（对应8个脑区）
- [ ] **服务依赖**：是否按照技术架构文档中的依赖关系设计服务调用
- [ ] **CDP数据结构**：是否按照功能设计文档中的CDP字段设计数据库表
- [ ] **API设计**：是否按照技术架构文档中的API设计实现接口
- [ ] **知识库表**：是否创建了所有必需的知识库表（线索库、词库、规则库等）
- [ ] **流程编排**：是否按照5步诊断流程实现流程编排
- [ ] **智能体实现**：是否按照智能体设计实现各个服务
- [ ] **双通道架构**：是否实现了双通道推理架构（结构化推理+语言策略）

---

## 九、技术栈选型

### 9.1 后端技术栈

**Java服务层**（业务服务）：
- **Web框架**：Spring Boot 2.7.8
- **服务治理**：Spring Cloud Gateway + Nacos（可选）
- **ORM**：JPA / Hibernate
- **服务调用**：OpenFeign
- **数据库**：MySQL 8.0（本地开发）/ Oracle（生产环境）

**Python服务层**（AI服务）：
- **Web框架**：FastAPI
- **异步框架**：asyncio + aiohttp
- **NLP**：transformers（BERT/ClinicalBERT）、spaCy
- **图神经网络**：PyTorch Geometric（SGIN实现）
- **机器学习**：scikit-learn、XGBoost/LightGBM
- **LLM框架**：LangChain（统一管理LLM调用）
  - 支持多种后端：OpenAI、ChatGLM、Ollama、自定义HTTP API
  - 提示词模板管理：Jinja2模板系统
  - 自动重试和错误处理机制
  - 异步和同步调用支持

### 9.2 数据存储

- **关系型数据库**：
  - MySQL 8.0（本地开发推荐）
  - Oracle（生产环境，与同事共同使用）
  - 用途：CDP存储、诊断记录、检查记录、健康状态判定记录、健康筛查记录等
- **图数据库**：Neo4j 5（知识图谱，DR.KNOWS核心依赖）
- **缓存**：Redis 7（CDP临时状态、会话管理）

### 9.3 模型与工具

- **医学概念识别**：QuickUMLS / 中文医学NER模型
- **LLM框架**：LangChain（统一管理大语言模型调用）
  - **支持的后端**：OpenAI（GPT-4/GPT-3.5）、ChatGLM、Ollama（本地模型）、自定义HTTP API
  - **提示词管理**：使用LangChain的PromptTemplateManager + Jinja2模板系统
  - **配置管理**：通过环境变量配置LLM参数（模型、temperature、max_tokens等）
- **大语言模型**：T5 / ChatGPT / 医学专用LLM（通过LangChain调用，用于路径注入和NLG）
- **图遍历算法**：Neo4j Cypher查询 + 图遍历算法（DR.KNOWS方法）
- **OCR**：PaddleOCR / Tesseract

#### 9.3.1 各服务LangChain使用情况

> **详细评估**：请参考《LangChain框架使用评估.md》文档

| 服务 | 是否需要LangChain | 优先级 | 使用场景 | 状态 |
|------|------------------|--------|---------|------|
| **Java服务** | | | | |
| diagnosis-service | ❌ 不需要 | - | 流程编排，不直接调用LLM | Java服务 |
| examination-service | ❌ 不需要 | - | 业务服务，不涉及LLM | Java服务 |
| **Python AI服务** | | | | |
| diagnosis-engine-service | ✅ **必须使用** | P0 | LLM引擎（诊断推理） | ✅ 已完成 |
| explanation-service | 🔄 **建议使用** | P1 | NLG（解释生成） | 📅 待实施 |
| dialog-service | 🔄 **建议使用** | P1 | NLG/NLU（问诊问题生成） | 📅 待实施 |
| health-state-assessment-service | ⚠️ 可选 | P2 | NLU增强（可选） | 规则引擎已够用 |
| clinical-parsing-service | ⚠️ 可选 | P2 | NLU增强（可选） | 词典+规则已够用 |
| treatment-engine-service | ⚠️ 可选 | P2 | NLG增强（可选） | 规则引擎已够用 |
| workup-planner-service | ❌ 不需要 | - | 规则引擎，不涉及LLM | 规则引擎已够用 |
| risk-assessment-service | ❌ 不需要 | - | 规则引擎，不涉及LLM | 规则引擎已够用 |
| ocr-service | ⚠️ 可选 | P3 | 多模态LLM（可选） | OCR模型已够用 |

**说明**：
- ✅ **必须使用**：核心功能依赖LLM，必须使用LangChain
- 🔄 **建议使用**：功能可以增强，建议使用LangChain
- ⚠️ **可选使用**：功能可用其他方案，LangChain是可选项
- ❌ **不需要**：功能不涉及LLM，不需要LangChain

**为什么不是统一框架？**
- 微服务架构：每个服务独立部署，需要各自管理依赖
- 技术栈不同：Java服务无法使用Python框架
- 职责分离：每个服务的LLM使用场景不同
- 配置隔离：不同服务需要不同的LLM参数

---

## 十、性能优化

### 10.1 路径检索优化

- **路径缓存**：Redis缓存常用路径
- **并行计算**：多症状路径并行搜索
- **智能剪枝**：基于关系权重的路径过滤

### 10.2 CDP性能优化

- **CDP缓存**：Redis缓存CDP临时状态
- **版本管理优化**：只保存关键版本，不保存每次更新
- **批量更新**：合并多个字段更新为一次操作

### 10.3 LLM推理优化

- **Prompt缓存**：缓存常用prompt模板
- **批量推理**：批量处理多个请求
- **模型量化**：使用量化模型减少推理时间
- **路径约束**：通过知识图谱路径约束LLM生成，减少无效推理

### 10.4 智能体性能优化

#### 10.4.1 并行执行优化

**并行策略**：

1. **任务并行**：多个独立任务并行执行
   - 不同患者的任务分配给不同智能体并行处理
   - 同一患者的不同阶段任务可以并行执行

2. **数据并行**：同一任务的不同数据并行处理
   - 多个诊断候选并行评估
   - 多个检查建议并行生成

3. **流水线并行**：智能体间形成流水线
   - 病例理解智能体处理完立即传递给问诊智能体
   - 诊断智能体处理完立即传递给检查建议智能体

**示例**：

```
【病例理解智能体】处理患者A
【主动问诊智能体】处理患者B
【鉴别诊断智能体】处理患者C
（三个智能体并行工作）
```

#### 10.4.2 智能体缓存机制

**缓存策略**：

1. **CDP缓存**：缓存常用CDP状态，减少重复计算
2. **推理结果缓存**：缓存常用推理结果（如常见症状的诊断路径）
3. **消息缓存**：缓存常用消息模板，减少消息构建时间
4. **智能体状态缓存**：缓存智能体状态，减少状态查询时间

#### 10.4.3 智能体负载均衡

**负载均衡策略**：

1. **轮询**：轮流分配任务给智能体实例
2. **最少连接**：分配给连接数最少的智能体实例
3. **最快响应**：分配给响应最快的智能体实例
4. **能力匹配**：分配给最适合的智能体实例（基于任务类型和智能体能力）

**智能体实例管理**：

- 支持智能体实例的动态扩缩容
- 根据负载情况自动增加或减少智能体实例
- 支持智能体实例的健康检查和故障转移

#### 10.4.4 智能体协作优化

**协作优化策略**：

1. **异步协作**：智能体间异步通信，不阻塞等待
2. **批量协作**：批量处理协作请求，减少通信开销
3. **智能路由**：根据任务类型智能路由到最合适的智能体
4. **冲突预判**：提前预判可能的冲突，减少协商次数

---

## 十一、部署架构

### 11.1 微服务部署

```
┌─────────────────┐
│   API Gateway   │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
┌───▼───┐ ┌──▼────┐
│ Java  │ │Python │
│Services│ │Services│
└───┬───┘ └──┬────┘
    │         │
    └────┬────┘
         │
┌────────▼────────┐
│   Data Layer    │
│ MySQL/Oracle/   │
│ Neo4j/Redis     │
└─────────────────┘
```

### 11.2 容器化部署

- **Docker**：容器化所有服务
- **Kubernetes**：容器编排和自动扩缩容
- **服务网格**：Istio（可选，用于服务治理）

---

## 十二、健康状态判定服务技术实现

### 12.1 健康状态判定服务（Health State Assessment Service）

**职责**：判断"这个人，现在需要被当成'病人'对待吗？"

**技术实现**：

```python
class HealthStateAssessmentService:
    def assess_health_state(self, user_input: str, basic_info: Dict) -> Dict:
        """
        评估健康状态，决定工作态
        """
        # 1. 提取症状和基本信息
        symptoms = self.extract_symptoms(user_input)
        
        # 2. 症状严重程度评估
        severity = self.assess_symptom_severity(symptoms)
        
        # 3. 风险早筛
        risk_signals = self.early_risk_screening(symptoms, basic_info)
        
        # 4. 红旗信号识别
        red_flags = self.detect_red_flags(symptoms)
        
        # 5. 分诊决策
        if red_flags or severity >= "high" or risk_signals:
            work_mode = "clinical_mode"
            needs_clinical_mode = True
        elif severity == "normal" and not risk_signals:
            work_mode = "wellness_mode"
            needs_clinical_mode = False
        else:
            # 不确定时，优先进入临床诊疗态（宁可误报，不能漏报）
            work_mode = "clinical_mode"
            needs_clinical_mode = True
        
        # 构建健康状态判定结果
        health_state_assessment = {
            "needs_clinical_mode": needs_clinical_mode,
            "work_mode": work_mode,
            "risk_level": self.calculate_risk_level(severity, risk_signals, red_flags),
            "assessment_reason": self.generate_reason(severity, risk_signals, red_flags),
            "symptom_severity": severity,
            "early_risk_signals": risk_signals
        }
        
        # 将结果存储到CDP的health_state_assessment字段
        # 注意：实际实现中需要通过CDP管理服务更新CDP
        # cdp_manager.update_cdp_field(cdp_id, "health_state_assessment", health_state_assessment)
        
        return health_state_assessment
```

**CDP存储说明**：
- 健康状态判定结果需要存储到CDP的`health_state_assessment`字段
- 由诊断服务（diagnosis-service）调用健康状态判定服务后，将结果更新到CDP
- 更新时机：CDP创建时或工作态切换时

#### 12.1.1 入口判定模块（Entry Assessment Module）

**职责**：实现AI诊断入口判定的完整流程（Step 1-5）

**技术实现**：

```python
class EntryAssessmentModule:
    def __init__(self):
        self.nlu_engine = NLUEngine()  # 自然语言理解引擎
        self.symptom_recognizer = SymptomRecognizer()  # 症状识别器
        self.clarification_engine = ClarificationEngine()  # 方向澄清引擎
        self.red_flag_detector = RedFlagDetector()  # 危险信号检测器
        
    def entry_assessment(self, user_input: str, basic_info: Dict) -> Dict:
        """
        入口判定流程：Step 1-5
        """
        # Step 1: 接收用户输入
        user_intent = self.step1_receive_input(user_input)
        
        # Step 2: 识别用户是否"有症状/困扰"
        symptom_status = self.step2_identify_symptom(user_intent)
        
        # Step 3: 方向澄清（仅对"情况C"触发一次）
        clarification_result = None
        if symptom_status["status"] == "uncertain":
            clarification_result = self.step3_clarification(user_intent)
        
        # Step 4: 危险信号检查（所有用户都要过一次）
        red_flags_check = self.step4_red_flag_check(user_intent, basic_info)
        
        # Step 5: 输出路径结果并跳转
        path_result = self.step5_path_selection(
            symptom_status, 
            clarification_result, 
            red_flags_check
        )
        
        # 构建入口判定结果（entry_assessment）
        entry_assessment = {
            "user_input": user_input,
            "has_symptom": symptom_status["status"] == "has_symptom",
            "symptom_status": symptom_status["status"],
            "clarification_needed": symptom_status["status"] == "uncertain",
            "clarification_result": clarification_result["direction"] if clarification_result else None,
            "red_flags_hit": red_flags_check["red_flags_hit"],
            "red_flags_list": red_flags_check["red_flags"],
            "path_selected": path_result["path"]
        }
        
        # 将入口判定结果存储到CDP的health_state_assessment.entry_assessment字段
        # 注意：实际实现中需要通过CDP管理服务更新CDP
        # cdp_manager.update_cdp_nested_field(
        #     cdp_id, 
        #     "health_state_assessment.entry_assessment", 
        #     entry_assessment
        # )
        
        return {
            "entry_assessment": entry_assessment,
            "path_result": path_result
        }
    
    def step1_receive_input(self, user_input: str) -> Dict:
        """
        Step 1: 接收用户输入
        """
        # 使用NLU引擎理解用户意图
        user_intent = self.nlu_engine.parse(user_input)
        
        return {
            "text": user_input,
            "intent": user_intent.intent,
            "entities": user_intent.entities,
            "is_mixed": user_intent.is_mixed_request  # 是否混合诉求
        }
    
    def step2_identify_symptom(self, user_intent: Dict) -> Dict:
        """
        Step 2: 识别用户是否"有症状/困扰"
        """
        # 识别症状/困扰表达
        symptoms = self.symptom_recognizer.recognize(user_intent["text"])
        
        # 识别健康管理/体检规划表达
        wellness_keywords = ["体检", "筛查", "预防", "健康管理", "健康规划"]
        has_wellness_intent = any(
            keyword in user_intent["text"] for keyword in wellness_keywords
        )
        
        # 判断情况
        if len(symptoms) == 0 and has_wellness_intent:
            # 情况A：明确无症状
            status = "no_symptom"
        elif len(symptoms) > 0:
            # 情况B：存在症状/困扰
            status = "has_symptom"
        else:
            # 情况C：不确定/模糊/混合诉求
            status = "uncertain"
        
        return {
            "status": status,
            "symptoms": symptoms,
            "has_wellness_intent": has_wellness_intent,
            "is_mixed": user_intent.get("is_mixed", False)
        }
    
    def step3_clarification(self, user_intent: Dict) -> Dict:
        """
        Step 3: 方向澄清（仅对"情况C"触发一次）
        """
        # 生成最小澄清问题
        clarification_question = self.clarification_engine.generate_question(
            user_intent
        )
        
        # 等待用户回答（在实际实现中，这里是异步的）
        # user_response = await self.wait_for_user_response(clarification_question)
        user_response = None  # 示例
        
        # 根据用户回答确定方向
        if user_response:
            direction = self.clarification_engine.determine_direction(user_response)
        else:
            # 默认处理：根据关键词倾向判断
            direction = self.clarification_engine.default_direction(user_intent)
        
        return {
            "question": clarification_question,
            "direction": direction,  # "A"（健康筛查）或 "B"（症状诊断）
            "clarified": user_response is not None
        }
    
    def step4_red_flag_check(self, user_intent: Dict, basic_info: Dict) -> Dict:
        """
        Step 4: 危险信号检查（安全兜底，所有用户都要过一次）
        """
        # 检测危险信号
        red_flags = self.red_flag_detector.detect(
            user_intent["text"], 
            basic_info
        )
        
        # 判断是否命中危险信号
        red_flags_hit = len(red_flags) > 0
        
        return {
            "red_flags_hit": red_flags_hit,
            "red_flags": red_flags,
            "safety_message": self._generate_safety_message(red_flags) if red_flags_hit else None
        }
    
    def step5_path_selection(self, 
                            symptom_status: Dict, 
                            clarification_result: Dict, 
                            red_flags_check: Dict) -> Dict:
        """
        Step 5: 输出路径结果并跳转
        """
        # 如果命中危险信号，退出线上流程
        if red_flags_check["red_flags_hit"]:
            return {
                "path": "exit",
                "reason": "危险信号命中",
                "message": red_flags_check["safety_message"]
            }
        
        # 确定路径
        if symptom_status["status"] == "no_symptom":
            path = "A"  # 健康筛查路径
        elif symptom_status["status"] == "has_symptom":
            path = "B"  # 症状诊断路径
        elif symptom_status["status"] == "uncertain":
            # 根据澄清结果确定路径
            if clarification_result:
                path = clarification_result["direction"]
            else:
                # 默认进入症状诊断路径（宁可误报，不能漏报）
                path = "B"
        else:
            # 默认进入症状诊断路径
            path = "B"
        
        return {
            "path": path,
            "path_name": "健康筛查路径" if path == "A" else "症状诊断路径",
            "next_step": "A1｜需求分类" if path == "A" else "Step 1：识别问题"
        }
    
    def _generate_safety_message(self, red_flags: List[str]) -> str:
        """
        生成安全提示消息
        """
        return f"检测到危险信号：{', '.join(red_flags)}。建议您优先线下就医或急诊。"
```

**数据库设计**：

```sql
-- 危险信号库表
CREATE TABLE red_flag_library (
    id VARCHAR(64) PRIMARY KEY,
    red_flag_name VARCHAR(255),
    red_flag_keywords JSON,
    red_flag_pattern TEXT,
    severity VARCHAR(32),  -- L1/L2/L3/L4
    safety_message TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_severity (severity)
);

-- 方向澄清模板表
CREATE TABLE clarification_template (
    id VARCHAR(64) PRIMARY KEY,
    scenario VARCHAR(255),  -- 不确定/混合诉求场景
    question_template TEXT,
    answer_options JSON,
    direction_mapping JSON,  -- 答案到方向的映射
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 症状识别规则表
CREATE TABLE symptom_recognition_rule (
    id VARCHAR(64) PRIMARY KEY,
    symptom_keyword VARCHAR(255),
    symptom_category VARCHAR(64),  -- 症状/不适/困扰/异常感觉/功能变化
    severity_threshold VARCHAR(32),  -- 轻微也算"有困扰"
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_keyword (symptom_keyword)
);
```

### 12.2 健康管理引擎（Wellness Management Engine）

**职责**：生成健康管理计划（面向健康管理态）

**技术实现**：

```python
class WellnessManagementEngine:
    def generate_wellness_plan(self, cdp: CDP) -> Dict:
        """
        生成健康管理计划
        """
        # 1. 风险识别与管理
        risk_management = self.identify_risks(cdp.patient_state)
        
        # 2. 生活方式建议生成
        lifestyle_advice = self.generate_lifestyle_advice(cdp.patient_state)
        
        # 3. 随访计划生成
        follow_up_plan = self.generate_follow_up_plan(cdp)
        
        # 4. 安抚与解释生成（使用LLM）
        reassurance = self.llm.generate_reassurance(cdp.patient_state)
        
        wellness_plan = {
            "risk_management": risk_management,
            "lifestyle_advice": lifestyle_advice,
            "follow_up_plan": follow_up_plan,
            "reassurance": reassurance,
            "upgrade_conditions": self.define_upgrade_conditions()
        }
        
        # 将健康管理计划存储到CDP的wellness_plan字段
        # 注意：实际实现中需要通过CDP管理服务更新CDP
        # cdp_manager.update_cdp_field(cdp_id, "wellness_plan", wellness_plan)
        
        return wellness_plan
```

**CDP存储说明**：
- 健康管理计划需要存储到CDP的`wellness_plan`字段
- 由诊断服务（diagnosis-service）调用健康管理引擎后，将结果更新到CDP
- 更新时机：健康管理态流程执行时（A路径A1-A5完成后）

#### 12.2.1 健康筛查服务（Wellness Screening Service）

**职责**：实现健康人筛查流程（A路径）的完整功能

**技术实现**：

```python
class WellnessScreeningService:
    def __init__(self):
        self.demand_classifier = DemandClassifier()  # 需求分类器
        self.health_profile_collector = HealthProfileCollector()  # 健康档案采集器
        self.screening_branch_executor = ScreeningBranchExecutor()  # 分支执行器
        self.result_generator = WellnessResultGenerator()  # 结果生成器
        self.follow_up_manager = FollowUpManager()  # 随访管理器
        
    def wellness_screening_workflow(self, cdp: CDP) -> Dict:
        """
        健康人筛查流程（A路径）：A1-A5
        """
        # A1: 需求分类
        demand_type = self.a1_demand_classification(cdp)
        
        # A2: 通用最小健康档案
        health_profile = self.a2_collect_health_profile(cdp)
        
        # A3: 进入对应分支执行
        branch_result = self.a3_execute_branch(demand_type, health_profile, cdp)
        
        # A4: 统一结果页输出
        unified_result = self.a4_generate_unified_result(
            demand_type, branch_result, cdp
        )
        
        # A5: 随访闭环
        follow_up_schedule = self.a5_setup_follow_up(
            demand_type, unified_result, cdp
        )
        
        # 构建健康筛查路径结果（wellness_screening_path）
        wellness_screening_path = {
            "demand_type": demand_type["type"],
            "demand_type_name": demand_type["type_name"],
            "health_profile": health_profile,
            "branch_result": branch_result,
            "unified_result": unified_result,
            "follow_up_schedule": follow_up_schedule
        }
        
        # 构建完整的健康管理计划（wellness_plan）
        wellness_plan = {
            "risk_management": self._generate_risk_management(cdp),
            "lifestyle_advice": self._generate_lifestyle_advice(cdp),
            "follow_up_plan": self._generate_follow_up_plan(cdp),
            "reassurance": self._generate_reassurance(cdp),
            "upgrade_conditions": self._define_upgrade_conditions(),
            "wellness_screening_path": wellness_screening_path
        }
        
        # 将健康管理计划存储到CDP的wellness_plan字段
        # 注意：实际实现中需要通过CDP管理服务更新CDP
        # cdp_manager.update_cdp_field(cdp_id, "wellness_plan", wellness_plan)
        
        return wellness_plan
    
    def a1_demand_classification(self, cdp: CDP) -> Dict:
        """
        A1: 需求分类
        """
        user_intent = cdp.patient_state.user_input
        
        # 使用需求分类器识别需求类型
        demand_type = self.demand_classifier.classify(user_intent)
        
        return {
            "type": demand_type["type"],  # 1/2/3/4
            "type_name": demand_type["name"],  # 筛查建议/健康目标管理/计划性健康需求
            "confidence": demand_type["confidence"]
        }
    
    def a2_collect_health_profile(self, cdp: CDP) -> Dict:
        """
        A2: 通用最小健康档案
        """
        # 采集最小健康档案字段
        health_profile = self.health_profile_collector.collect(cdp)
        
        # 识别缺失字段
        missing_fields = self.health_profile_collector.identify_missing(health_profile)
        
        return {
            "basic_info": health_profile.get("basic_info", {}),
            "past_history": health_profile.get("past_history", []),
            "family_history": health_profile.get("family_history", []),
            "lifestyle": health_profile.get("lifestyle", {}),
            "medications": health_profile.get("medications", []),
            "missing_fields": missing_fields,
            "completeness": self._calculate_completeness(health_profile)
        }
    
    def a3_execute_branch(self, demand_type: Dict, health_profile: Dict, cdp: CDP) -> Dict:
        """
        A3: 进入对应分支执行
        """
        branch_type = demand_type["type"]
        
        if branch_type == 1:
            # A3-1: 筛查建议分支
            return self._execute_screening_recommendation_branch(health_profile, cdp)
        elif branch_type == 2:
            # A3-2: 健康目标管理分支
            return self._execute_health_goal_branch(health_profile, cdp)
        elif branch_type == 3:
            # A3-3: 计划性健康需求分支
            return self._execute_scenario_branch(health_profile, cdp)
        else:
            raise ValueError(f"Unknown demand type: {branch_type}")
    
    def _execute_screening_recommendation_branch(self, health_profile: Dict, cdp: CDP) -> Dict:
        """
        A3-1: 筛查建议分支
        """
        # 1. 补充追问（仅问会影响筛查决策的关键信息）
        additional_info = self.screening_branch_executor.collect_key_info(health_profile)
        
        # 2. 规则化生成两张清单
        recommended_screenings = self.screening_branch_executor.generate_recommended_list(
            health_profile, additional_info
        )
        not_recommended_screenings = self.screening_branch_executor.generate_not_recommended_list(
            health_profile, additional_info
        )
        
        # 3. 输出分支结果
        return {
            "branch_type": "screening_recommendation",
            "additional_info": additional_info,
            "recommended_screenings": recommended_screenings,
            "not_recommended_screenings": not_recommended_screenings,
            "next_update_time": self._calculate_next_update_time(health_profile)
        }
    
    def _execute_health_goal_branch(self, health_profile: Dict, cdp: CDP) -> Dict:
        """
        A3-3: 健康目标管理分支
        """
        # 1. 固定追问（目标/现状/资源/周期）
        goal_info = self.screening_branch_executor.collect_goal_info()
        
        # 2. 生成可执行行动计划
        action_plan = self.screening_branch_executor.generate_action_plan(goal_info)
        
        # 3. 输出分支结果
        return {
            "branch_type": "health_goal",
            "goal_info": goal_info,
            "action_plan": action_plan
        }
    
    def _execute_scenario_branch(self, health_profile: Dict, cdp: CDP) -> Dict:
        """
        A3-4: 计划性健康需求分支
        """
        # 1. 追问场景必要信息（场景/时间点/禁忌信息）
        scenario_info = self.screening_branch_executor.collect_scenario_info()
        
        # 2. 输出清单 + 时间表 + 注意事项
        scenario_plan = self.screening_branch_executor.generate_scenario_plan(scenario_info)
        
        # 3. 输出分支结果
        return {
            "branch_type": "scenario",
            "scenario_info": scenario_info,
            "scenario_plan": scenario_plan
        }
    
    def a4_generate_unified_result(self, demand_type: Dict, branch_result: Dict, cdp: CDP) -> Dict:
        """
        A4: 统一结果页输出
        """
        # 生成统一结果展示结构
        unified_result = self.result_generator.generate(
            demand_type, branch_result, cdp
        )
        
        return {
            "summary": unified_result["summary"],  # 一句话总结
            "recommended_actions": unified_result["recommended_actions"],  # 现在建议做的事
            "not_recommended_actions": unified_result["not_recommended_actions"],  # 暂时不建议做的事
            "next_review_time": unified_result["next_review_time"],  # 下一次复查/更新时间点
            "exit_conditions": unified_result["exit_conditions"]  # 退出A路径条件提示
        }
    
    def a5_setup_follow_up(self, demand_type: Dict, unified_result: Dict, cdp: CDP) -> Dict:
        """
        A5: 随访闭环
        """
        # 根据需求类型设置随访节奏
        follow_up_schedule = self.follow_up_manager.setup_schedule(
            demand_type, unified_result, cdp
        )
        
        return {
            "follow_up_type": demand_type["type_name"],
            "follow_up_rules": follow_up_schedule["rules"],
            "reminder_templates": follow_up_schedule["reminders"]
        }
    
    def _calculate_completeness(self, health_profile: Dict) -> float:
        """
        计算健康档案完整度
        """
        required_fields = ["basic_info", "past_history", "family_history", "lifestyle"]
        filled_fields = sum(1 for field in required_fields if health_profile.get(field))
        return filled_fields / len(required_fields)
    
    def _calculate_next_update_time(self, health_profile: Dict) -> str:
        """
        计算下次更新时间点
        """
        # 根据健康档案和筛查建议计算
        return "6个月后"  # 示例
```

**数据库设计**：

```sql
-- 需求分类规则表
CREATE TABLE demand_classification_rule (
    id VARCHAR(64) PRIMARY KEY,
    demand_type INT,  -- 1/2/3/4
    demand_type_name VARCHAR(64),
    keywords JSON,
    classification_rule TEXT,
    guide_template TEXT,  -- 不确定时的引导话术
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_demand_type (demand_type)
);

-- 筛查建议规则库表
CREATE TABLE screening_recommendation_rule (
    id VARCHAR(64) PRIMARY KEY,
    condition_json JSON,  -- 触发条件（年龄、性别、既往史等）
    screening_name VARCHAR(255),
    recommended BOOLEAN,
    frequency VARCHAR(64),  -- 筛查频率
    timing VARCHAR(255),  -- 筛查时机
    reason TEXT,  -- 推荐/不推荐理由
    alternative_suggestion TEXT,  -- 替代建议
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 体检异常分级规则表
CREATE TABLE abnormality_grading_rule (
    id VARCHAR(64) PRIMARY KEY,
    indicator_name VARCHAR(255),
    indicator_category VARCHAR(64),
    grade VARCHAR(32),  -- 轻度/中度/重度
    grading_criteria JSON,
    next_action VARCHAR(64),  -- 复查/进一步评估/退出A路径就医
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_indicator (indicator_name)
);

-- 健康目标计划模板表
CREATE TABLE health_goal_template (
    id VARCHAR(64) PRIMARY KEY,
    goal_type VARCHAR(64),  -- 减脂/睡眠/运动/精力
    goal_template JSON,
    action_steps_template JSON,
    tracking_indicators JSON,
    review_points JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_goal_type (goal_type)
);

-- 计划性健康需求场景库表
CREATE TABLE scenario_library (
    id VARCHAR(64) PRIMARY KEY,
    scenario_name VARCHAR(64),  -- 备孕/疫苗/差旅/材料等
    checklist JSON,
    schedule_template JSON,
    notes_template TEXT,
    reminder_rules JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_scenario (scenario_name)
);

-- 随访规则表
CREATE TABLE follow_up_rule (
    id VARCHAR(64) PRIMARY KEY,
    demand_type INT,
    follow_up_type VARCHAR(64),
    default_time VARCHAR(64),
    review_trigger TEXT,
    reminder_template TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_demand_type (demand_type)
);
```

### 12.3 工作态切换机制

**健康管理态 → 临床诊疗态升级**：

```python
class WorkModeUpgrader:
    def check_upgrade_conditions(self, cdp: CDP) -> bool:
        """
        检查是否需要从健康管理态升级到临床诊疗态
        """
        # 检查升级条件
        if self.symptom_worsened(cdp):
            return True
        if self.new_symptoms_appeared(cdp):
            return True
        if self.risk_signals_detected(cdp):
            return True
        
        return False
    
    def upgrade_to_clinical_mode(self, cdp: CDP):
        """
        升级到临床诊疗态
        """
        cdp.health_state_assessment.work_mode = "clinical_mode"
        cdp.health_state_assessment.needs_clinical_mode = True
        # 启动诊断流程
        self.start_diagnosis_workflow(cdp)
```

---

## 十三、评估与验证体系

### 13.1 三类离线评测集

#### 13.1.1 静态病例集（Static Case Set）

**用途**：评估"读病历 → 给DDx/计划"的能力

**输入**：固定的完整病历文本  
**输出**：DDx列表、检查建议、处置方案  
**评估**：可重复、可对比

**评估指标**：
- DDx Top-K命中率
- 检查建议合理性
- 处置方案合理性

**智能体评估重点**：
- **病例理解智能体（agent_1）**：概念归一化准确率、结构化提取完整率
- **鉴别诊断智能体（agent_3）**：DDx准确性、三层分层合理性
- **检查建议智能体（agent_4）**：检查建议合理性、验证计划有效性
- **治疗建议智能体（agent_5）**：处置方案合理性

#### 13.1.2 交互问诊集（Interactive Interview Set）

**用途**：评估"问对问题"的能力和问诊效率

**输入**：初始主诉，需要多轮对话  
**输出**：问诊问题序列、最终诊断  
**评估**：问诊质量、问诊效率

**评估指标**：
- "缺失信息"是否合理（问诊质量）
- 问诊轮次效率（完成诊断所需轮次）
- 信息增益有效性（问的问题是否有助于区分DDx）

**智能体评估重点**：
- **主动问诊智能体（agent_2）**：问诊质量、问诊效率、信息增益有效性
- **临床决策分析**：问题选择是否基于临床决策理论
- **主诉关键线索库**：线索使用是否合理

#### 13.1.3 轨迹回放集（Trajectory Replay Set）

**用途**：评估"信息逐步揭示时修正DDx"的能力

**输入**：逐步提供信息
- 先给主诉
- 再给体征
- 再给检验
- 再给影像

**输出**：每个阶段的DDx更新  
**评估**：是否能随着信息更新修正DDx

**评估指标**：
- DDx修正准确性
- 信息更新后的诊断更新及时性
- 证据链更新完整性

**智能体评估重点**：
- **鉴别诊断智能体（agent_3）**：DDx修正准确性、三层排序更新及时性
- **检查建议智能体（agent_4）**：证据回填准确性、排序更新规则有效性
- **证据链智能体（agent_7）**：证据链更新完整性

### 13.2 核心评估指标

#### 13.2.1 DDx准确性指标

- **DDx Top-1命中率**：最高可能性诊断是否正确
- **DDx Top-3命中率**：前3个可能性中是否包含正确诊断
- **DDx Top-5命中率**：前5个可能性中是否包含正确诊断

**智能体评估**：
- **鉴别诊断智能体（agent_3）**：多引擎融合诊断的准确性
- **三层分层合理性**：首要假设、主要备选、必须排除的划分是否合理

#### 13.2.2 安全性指标

- **红旗识别召回率**：宁可误报，不能漏报
- **高危升级及时性**：识别高危后是否及时建议就医
- **不确定性表达准确性**：低置信度时是否明确表达不确定性

**智能体评估**：
- **健康状态判定智能体（agent_0）**：危险信号检查召回率
- **风险评估智能体（agent_6）**：高危识别召回率、升级及时性
- **证据链智能体（agent_7）**：不确定性表达准确性

#### 13.2.3 问诊质量指标

- **信息增益有效性**：问的问题是否有助于区分DDx
- **问诊轮次效率**：完成诊断所需轮次
- **缺失信息合理性**：识别的缺失信息是否合理

**智能体评估**：
- **主动问诊智能体（agent_2）**：临床决策分析驱动的问诊有效性
- **主诉关键线索库**：线索使用是否合理
- **差异点词库与问法规范**：问法是否统一、答案是否可比

#### 13.2.4 可解释性指标

- **证据链完整性**：支持证据、反对证据、缺失证据是否完整
- **推理路径可追溯性**：诊断结论是否能追溯到证据
- **方案合理性**：基于路径/证据可解释

**智能体评估**：
- **证据链智能体（agent_7）**：证据链构建完整性、推理路径可视化
- **鉴别诊断智能体（agent_3）**：诊断树结构清晰性、推理子组组织合理性
- **所有智能体**：输出是否可追溯、可审计

#### 13.2.5 健康状态判定指标

- **健康状态判定准确率**：是否正确判断是否需要进入诊疗流程
- **健康管理态升级及时性**：发现风险信号后是否及时升级到临床诊疗态
- **漏诊率**：应该进入临床诊疗态但误判为健康管理态的比例（必须极低）

**智能体评估**：
- **健康状态判定智能体（agent_0）**：入口判定流程准确性、路径选择合理性
- **风险评估智能体（agent_6）**：升级触发条件准确性、升级及时性

### 13.3 智能体性能评估

**评估指标**：

1. **任务完成率**：完成任务数 / 分配任务数
2. **响应时间**：从接收任务到完成的时间
3. **准确率**：任务结果的准确率
4. **协作效率**：协作任务的完成效率

**各智能体评估重点**：

- **健康状态判定智能体（agent_0）**：入口判定准确率、路径选择合理性
- **病例理解智能体（agent_1）**：概念归一化准确率、结构化提取完整率
- **主动问诊智能体（agent_2）**：问诊质量、问诊效率、信息增益有效性
- **鉴别诊断智能体（agent_3）**：DDx准确性、三层分层合理性、诊断树组织合理性
- **检查建议智能体（agent_4）**：检查建议合理性、验证计划有效性
- **治疗建议智能体（agent_5）**：处置方案合理性
- **风险评估智能体（agent_6）**：高危识别召回率、升级及时性
- **证据链智能体（agent_7）**：证据链完整性、可解释性

### 13.4 协作效果评估

**评估指标**：

1. **协商成功率**：达成共识的协商数 / 总协商数
2. **冲突解决时间**：解决冲突的平均时间
3. **协作效率**：协作任务的完成效率
4. **系统整体性能**：整个系统的性能指标

**协作模式评估**：

- **顺序协作**：任务传递效率、信息传递准确性
- **并行协作**：并行执行效率、结果融合准确性
- **协商协作**：协商成功率、协商时间
- **竞争协作**：投票机制有效性、最佳结果选择准确性

### 13.5 对比评估

**对比维度**：

1. **单智能体 vs 多智能体**：对比单智能体和多智能体的性能
2. **不同协作策略**：对比不同协作策略的效果
3. **不同冲突解决策略**：对比不同冲突解决策略的效果
4. **不同推理引擎**：对比不同推理引擎的诊断准确性
5. **不同问诊策略**：对比不同问诊策略的效率和质量

### 13.6 评估体系技术实现

> **对应功能设计文档**：五、评估与验证体系  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第5.1-5.2节

#### 13.6.1 评估系统架构

**评估系统设计**：

```
┌─────────────────────────────────────────────────────────────┐
│                    评估系统架构                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  评估数据集管理模块                                      │  │
│  │  - 静态病例集管理                                        │  │
│  │  - 交互问诊集管理                                        │  │
│  │  - 轨迹回放集管理                                        │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  评估执行引擎                                            │  │
│  │  - 自动化评估流程                                        │  │
│  │  - 指标计算引擎                                          │  │
│  │  - 结果分析引擎                                          │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  评估结果存储                                            │  │
│  │  - 评估结果数据库                                        │  │
│  │  - 评估报告生成                                          │  │
│  │  - 历史对比分析                                          │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

#### 13.6.2 评估数据集管理

**静态病例集数据结构**：

```python
class StaticCaseSet:
    """
    静态病例集数据结构
    """
    def __init__(self):
        self.case_id: str  # 病例ID
        self.case_text: str  # 完整病历文本
        self.ground_truth: {
            "diagnosis": [  # 标准诊断列表
                {
                    "disease_code": "CUI/ICD编码",
                    "disease_name": "疾病名称",
                    "rank": 1,  # 诊断优先级
                    "is_correct": True  # 是否为正确诊断
                }
            ],
            "workup_plan": [  # 标准检查建议
                {
                    "test_name": "检查名称",
                    "purpose": "检查目的",
                    "priority": "优先级"
                }
            ],
            "management_plan": [  # 标准处置方案
                {
                    "type": "处置类型",
                    "content": "具体方案"
                }
            ]
        }
        self.metadata: {
            "source": "数据来源",
            "expert_annotation": "专家标注信息",
            "difficulty_level": "难度等级"
        }
```

**交互问诊集数据结构**：

```python
class InteractiveInterviewSet:
    """
    交互问诊集数据结构
    """
    def __init__(self):
        self.case_id: str  # 病例ID
        self.initial_complaint: str  # 初始主诉
        self.interview_trajectory: [  # 问诊轨迹
            {
                "round": 1,  # 轮次
                "question": "问诊问题",
                "answer": "患者回答",
                "expected_info_gain": 0.8,  # 预期信息增益
                "actual_info_gain": 0.75  # 实际信息增益
            }
        ]
        self.final_diagnosis: {
            "diagnosis": "最终诊断",
            "confidence": 0.9,
            "evidence_chain": ["证据1", "证据2"]
        }
        self.evaluation: {
            "total_rounds": 5,  # 总轮次
            "information_gain_effectiveness": 0.85,  # 信息增益有效性
            "missing_info_reasonableness": 0.9  # 缺失信息合理性
        }
```

**轨迹回放集数据结构**：

```python
class TrajectoryReplaySet:
    """
    轨迹回放集数据结构
    """
    def __init__(self):
        self.case_id: str  # 病例ID
        self.information_stages: [  # 信息阶段
            {
                "stage": 1,
                "stage_name": "主诉阶段",
                "information": {
                    "chief_complaint": "主诉内容",
                    "symptoms": ["症状列表"]
                },
                "expected_ddx": [  # 预期DDx
                    {
                        "disease": "疾病名称",
                        "probability": 0.6,
                        "rank": 1
                    }
                ]
            },
            {
                "stage": 2,
                "stage_name": "体征阶段",
                "information": {
                    "signs": ["体征列表"],
                    "vital_signs": {"血压": "120/80"}
                },
                "expected_ddx": [...]  # 更新后的预期DDx
            }
        ]
        self.evaluation: {
            "ddx_correction_accuracy": 0.9,  # DDx修正准确性
            "update_timeliness": 0.85,  # 更新及时性
            "evidence_chain_completeness": 0.9  # 证据链完整性
        }
```

#### 13.6.3 评估执行引擎

**评估执行引擎实现**：

```python
class EvaluationEngine:
    """
    评估执行引擎
    """
    def __init__(self):
        self.metric_calculator = MetricCalculator()
        self.result_analyzer = ResultAnalyzer()
        self.report_generator = ReportGenerator()
    
    def evaluate_static_case_set(self, case_set: StaticCaseSet, system_output: dict) -> dict:
        """
        评估静态病例集
        """
        # 1. DDx准确性评估
        ddx_metrics = self.metric_calculator.calculate_ddx_accuracy(
            ground_truth=case_set.ground_truth["diagnosis"],
            system_output=system_output["ddx"]
        )
        
        # 2. 检查建议合理性评估
        workup_metrics = self.metric_calculator.calculate_workup_reasonableness(
            ground_truth=case_set.ground_truth["workup_plan"],
            system_output=system_output["workup_plan"]
        )
        
        # 3. 处置方案合理性评估
        management_metrics = self.metric_calculator.calculate_management_reasonableness(
            ground_truth=case_set.ground_truth["management_plan"],
            system_output=system_output["management_plan"]
        )
        
        return {
            "ddx_metrics": ddx_metrics,
            "workup_metrics": workup_metrics,
            "management_metrics": management_metrics,
            "overall_score": self._calculate_overall_score(
                ddx_metrics, workup_metrics, management_metrics
            )
        }
    
    def evaluate_interactive_interview(self, interview_set: InteractiveInterviewSet, 
                                       system_trajectory: list) -> dict:
        """
        评估交互问诊集
        """
        # 1. 问诊质量评估
        interview_quality = self.metric_calculator.calculate_interview_quality(
            expected_trajectory=interview_set.interview_trajectory,
            system_trajectory=system_trajectory
        )
        
        # 2. 问诊效率评估
        interview_efficiency = self.metric_calculator.calculate_interview_efficiency(
            expected_rounds=interview_set.evaluation["total_rounds"],
            actual_rounds=len(system_trajectory)
        )
        
        # 3. 信息增益有效性评估
        information_gain = self.metric_calculator.calculate_information_gain_effectiveness(
            expected_gain=interview_set.interview_trajectory,
            actual_gain=system_trajectory
        )
        
        return {
            "interview_quality": interview_quality,
            "interview_efficiency": interview_efficiency,
            "information_gain": information_gain,
            "overall_score": self._calculate_overall_score(
                interview_quality, interview_efficiency, information_gain
            )
        }
    
    def evaluate_trajectory_replay(self, replay_set: TrajectoryReplaySet, 
                                   system_stages: list) -> dict:
        """
        评估轨迹回放集
        """
        # 1. DDx修正准确性评估
        ddx_correction = self.metric_calculator.calculate_ddx_correction_accuracy(
            expected_stages=replay_set.information_stages,
            system_stages=system_stages
        )
        
        # 2. 更新及时性评估
        update_timeliness = self.metric_calculator.calculate_update_timeliness(
            expected_stages=replay_set.information_stages,
            system_stages=system_stages
        )
        
        # 3. 证据链完整性评估
        evidence_completeness = self.metric_calculator.calculate_evidence_completeness(
            expected_stages=replay_set.information_stages,
            system_stages=system_stages
        )
        
        return {
            "ddx_correction": ddx_correction,
            "update_timeliness": update_timeliness,
            "evidence_completeness": evidence_completeness,
            "overall_score": self._calculate_overall_score(
                ddx_correction, update_timeliness, evidence_completeness
            )
        }
```

#### 13.6.4 指标计算引擎

**指标计算引擎实现**：

```python
class MetricCalculator:
    """
    指标计算引擎
    """
    def calculate_ddx_accuracy(self, ground_truth: list, system_output: list) -> dict:
        """
        计算DDx准确性指标
        """
        # 1. Top-1命中率
        top1_hit = 1 if system_output[0]["disease_code"] == ground_truth[0]["disease_code"] else 0
        
        # 2. Top-3命中率
        top3_codes = [d["disease_code"] for d in system_output[:3]]
        ground_truth_codes = [d["disease_code"] for d in ground_truth]
        top3_hit = 1 if any(code in top3_codes for code in ground_truth_codes) else 0
        
        # 3. Top-5命中率
        top5_codes = [d["disease_code"] for d in system_output[:5]]
        top5_hit = 1 if any(code in top5_codes for code in ground_truth_codes) else 0
        
        return {
            "top1_hit_rate": top1_hit,
            "top3_hit_rate": top3_hit,
            "top5_hit_rate": top5_hit,
            "average_rank": self._calculate_average_rank(ground_truth, system_output)
        }
    
    def calculate_safety_metrics(self, red_flags: list, system_output: dict) -> dict:
        """
        计算安全性指标
        """
        # 1. 红旗识别召回率
        detected_red_flags = system_output.get("red_flags", [])
        true_positives = len(set(red_flags) & set(detected_red_flags))
        recall = true_positives / len(red_flags) if red_flags else 0
        
        # 2. 高危升级及时性
        upgrade_time = system_output.get("upgrade_time", 0)
        expected_time = system_output.get("expected_upgrade_time", 0)
        timeliness = 1 - min(upgrade_time / expected_time, 1) if expected_time > 0 else 0
        
        # 3. 不确定性表达准确性
        uncertainty_expression = system_output.get("uncertainty_expression", {})
        confidence = system_output.get("confidence", 1.0)
        uncertainty_accuracy = 1.0 if (confidence < 0.7 and uncertainty_expression) else 0.0
        
        return {
            "red_flag_recall": recall,
            "upgrade_timeliness": timeliness,
            "uncertainty_accuracy": uncertainty_accuracy
        }
    
    def calculate_interview_quality(self, expected_trajectory: list, 
                                     system_trajectory: list) -> dict:
        """
        计算问诊质量指标
        """
        # 1. 缺失信息合理性
        expected_missing = [q["expected_info_gain"] for q in expected_trajectory]
        system_missing = [q.get("info_gain", 0) for q in system_trajectory]
        missing_reasonableness = self._calculate_correlation(expected_missing, system_missing)
        
        # 2. 问诊轮次效率
        expected_rounds = len(expected_trajectory)
        actual_rounds = len(system_trajectory)
        efficiency = expected_rounds / actual_rounds if actual_rounds > 0 else 0
        
        # 3. 信息增益有效性
        expected_gains = [q["expected_info_gain"] for q in expected_trajectory]
        actual_gains = [q.get("actual_info_gain", 0) for q in system_trajectory]
        gain_effectiveness = self._calculate_correlation(expected_gains, actual_gains)
        
        return {
            "missing_reasonableness": missing_reasonableness,
            "efficiency": efficiency,
            "gain_effectiveness": gain_effectiveness
        }
```

#### 13.6.5 评估结果存储与报告生成

**评估结果数据库设计**：

```sql
-- 评估结果表
CREATE TABLE evaluation_result (
    id VARCHAR(64) PRIMARY KEY,
    evaluation_type VARCHAR(32),  -- static/interactive/trajectory
    case_id VARCHAR(64),
    system_version VARCHAR(32),
    evaluation_timestamp TIMESTAMP,
    metrics JSON,  -- 评估指标结果
    overall_score DECIMAL(5,2),
    created_at TIMESTAMP,
    INDEX idx_evaluation_type (evaluation_type),
    INDEX idx_system_version (system_version)
);

-- 评估报告表
CREATE TABLE evaluation_report (
    id VARCHAR(64) PRIMARY KEY,
    report_name VARCHAR(255),
    report_type VARCHAR(32),  -- daily/weekly/monthly
    evaluation_period_start TIMESTAMP,
    evaluation_period_end TIMESTAMP,
    summary_metrics JSON,
    detailed_results JSON,
    comparison_with_previous JSON,
    created_at TIMESTAMP
);
```

**评估报告生成**：

```python
class ReportGenerator:
    """
    评估报告生成器
    """
    def generate_evaluation_report(self, evaluation_results: list, 
                                   report_type: str = "weekly") -> dict:
        """
        生成评估报告
        """
        # 1. 汇总指标
        summary_metrics = self._aggregate_metrics(evaluation_results)
        
        # 2. 详细结果分析
        detailed_results = self._analyze_detailed_results(evaluation_results)
        
        # 3. 与历史对比
        previous_results = self._get_previous_results(report_type)
        comparison = self._compare_with_previous(summary_metrics, previous_results)
        
        # 4. 生成报告
        report = {
            "report_name": f"{report_type}_evaluation_report_{datetime.now()}",
            "report_type": report_type,
            "evaluation_period": {
                "start": evaluation_results[0]["timestamp"],
                "end": evaluation_results[-1]["timestamp"]
            },
            "summary_metrics": summary_metrics,
            "detailed_results": detailed_results,
            "comparison_with_previous": comparison,
            "recommendations": self._generate_recommendations(summary_metrics, comparison)
        }
        
        return report
```

---

## 十四、复杂度分析

### 14.1 系统复杂度概述

多智能体架构在带来灵活性和可扩展性的同时，也引入了系统复杂度的增加。本章节从多个维度分析系统的复杂度，并提出相应的优化策略。

### 14.2 架构复杂度分析

#### 14.2.1 智能体数量复杂度

**复杂度来源**：
- 系统包含8个核心智能体 + 1个协调器智能体
- 每个智能体具备独立的感知、推理、决策和执行能力
- 智能体间存在复杂的协作关系

**复杂度评估**：
- **智能体数量**：O(n)，其中 n = 9（8个核心智能体 + 1个协调器）
- **智能体间通信路径**：O(n²) = O(81)，理论上最多81条通信路径
- **实际通信路径**：O(n)，通过协调器统一管理，实际通信路径约为线性复杂度

**优化策略**：
1. **通过协调器统一管理**：所有智能体间通信通过协调器，减少直接通信路径
2. **消息路由优化**：使用消息队列和路由机制，避免全连接
3. **智能体分组**：按功能域分组，减少跨组通信

#### 14.2.2 协作模式复杂度

**复杂度来源**：
- 支持4种协作模式：顺序、并行、协商、竞争
- 不同协作模式有不同的执行流程和状态管理
- 协作模式可能动态切换

**复杂度评估**：
- **协作模式数量**：O(4) = 常数复杂度
- **状态转换复杂度**：O(m)，其中 m 为状态数量
- **协作流程复杂度**：O(k)，其中 k 为协作步骤数

**优化策略**：
1. **状态机管理**：使用状态机统一管理协作状态转换
2. **协作模式模板化**：为每种协作模式建立模板，减少重复实现
3. **异步协作**：使用异步消息传递，避免阻塞等待

#### 14.2.3 CDP流转复杂度

**复杂度来源**：
- CDP在多个智能体间流转
- CDP版本管理和状态同步
- CDP并发访问控制

**复杂度评估**：
- **CDP流转路径**：O(n)，其中 n 为智能体数量
- **版本管理复杂度**：O(v)，其中 v 为版本数量
- **并发控制复杂度**：O(1)，通过锁机制保证

**优化策略**：
1. **版本管理优化**：只保存关键版本，不保存每次更新
2. **批量更新**：合并多个字段更新为一次操作
3. **读写分离**：支持多读单写，提高并发性能

### 14.3 算法复杂度分析

#### 14.3.1 知识图谱推理复杂度

**复杂度来源**：
- 知识图谱路径检索
- 路径排序和评分
- 多路径融合

**复杂度评估**：
- **路径检索复杂度**：O(V + E)，其中 V 为节点数，E 为边数
- **路径排序复杂度**：O(k log k)，其中 k 为路径数量
- **路径融合复杂度**：O(k)，其中 k 为路径数量

**优化策略**：
1. **路径缓存**：缓存常用路径，减少重复计算
2. **并行计算**：多症状路径并行搜索
3. **智能剪枝**：基于关系权重的路径过滤

#### 14.3.2 多引擎融合诊断复杂度

**复杂度来源**：
- 多个诊断引擎并行执行
- 引擎结果融合和排序
- 证据分析和评分

**复杂度评估**：
- **引擎执行复杂度**：O(e)，其中 e 为引擎数量（可并行）
- **结果融合复杂度**：O(d log d)，其中 d 为诊断候选数量
- **证据分析复杂度**：O(e × d)，其中 e 为证据数量，d 为诊断数量

**优化策略**：
1. **并行执行**：多个引擎并行执行，减少总执行时间
2. **结果缓存**：缓存引擎结果，避免重复计算
3. **增量更新**：只更新变化的诊断候选，减少计算量

#### 14.3.3 问诊策略生成复杂度

**复杂度来源**：
- 信息缺口识别
- 临床决策分析（信息增益计算）
- 问诊问题生成

**复杂度评估**：
- **信息缺口识别复杂度**：O(d)，其中 d 为诊断候选数量
- **信息增益计算复杂度**：O(q × d)，其中 q 为问题候选数量，d 为诊断数量
- **问题生成复杂度**：O(1)，基于模板生成

**优化策略**：
1. **问题候选预筛选**：基于诊断候选预筛选问题，减少计算量
2. **信息增益缓存**：缓存常用问题的信息增益
3. **增量计算**：只计算新增问题的信息增益

### 14.4 数据复杂度分析

#### 14.4.1 CDP数据结构复杂度

**复杂度来源**：
- CDP包含多个嵌套字段
- 字段间存在依赖关系
- 需要支持版本管理和状态同步

**复杂度评估**：
- **字段数量**：O(f)，其中 f 为字段数量（约20-30个）
- **嵌套深度**：O(d)，其中 d 为嵌套深度（约3-4层）
- **版本管理复杂度**：O(v)，其中 v 为版本数量

**优化策略**：
1. **字段分组**：按功能域分组，减少字段间依赖
2. **版本压缩**：只保存关键版本，定期归档旧版本
3. **增量更新**：只更新变化的字段，减少数据传输

#### 14.4.2 知识图谱数据复杂度

**复杂度来源**：
- 知识图谱包含大量医学概念和关系
- 需要支持高效的路径检索
- 需要支持实时更新

**复杂度评估**：
- **节点数量**：O(N)，其中 N 为医学概念数量（百万级）
- **边数量**：O(E)，其中 E 为关系数量（千万级）
- **路径检索复杂度**：O(V + E)，图遍历复杂度

**优化策略**：
1. **图数据库优化**：使用Neo4j等图数据库，优化路径检索
2. **索引优化**：为常用查询建立索引
3. **分区存储**：按领域分区存储，减少检索范围

### 14.5 通信复杂度分析

#### 14.5.1 智能体间通信复杂度

**复杂度来源**：
- 智能体间消息传递
- 消息路由和转发
- 消息队列管理

**复杂度评估**：
- **消息数量**：O(m)，其中 m 为消息数量
- **路由复杂度**：O(1)，通过协调器统一路由
- **队列管理复杂度**：O(log q)，其中 q 为队列长度

**优化策略**：
1. **消息批处理**：批量处理消息，减少通信开销
2. **异步通信**：使用异步消息传递，避免阻塞
3. **消息压缩**：压缩消息内容，减少网络传输

#### 14.5.2 服务间通信复杂度

**复杂度来源**：
- 微服务间API调用
- 服务发现和负载均衡
- 网络延迟和故障处理

**复杂度评估**：
- **服务数量**：O(s)，其中 s 为服务数量（约10-15个）
- **API调用复杂度**：O(c)，其中 c 为调用次数
- **网络延迟**：O(1)，但受网络环境影响

**优化策略**：
1. **服务聚合**：合并相关服务，减少服务间调用
2. **缓存机制**：缓存服务调用结果，减少重复调用
3. **异步调用**：使用异步调用，提高并发性能

### 14.6 性能复杂度分析

#### 14.6.1 响应时间复杂度

**复杂度来源**：
- 多智能体协作需要等待所有智能体完成
- 知识图谱推理需要遍历大量节点
- LLM推理需要较长处理时间

**复杂度评估**：
- **智能体协作时间**：O(max(t_i))，其中 t_i 为各智能体执行时间
- **知识图谱推理时间**：O(V + E)，图遍历时间
- **LLM推理时间**：O(1)，但实际时间较长（秒级）

**优化策略**：
1. **并行执行**：智能体并行执行，减少总执行时间
2. **路径缓存**：缓存常用路径，减少推理时间
3. **LLM优化**：使用量化模型、批量推理等优化LLM性能

#### 14.6.2 并发处理复杂度

**复杂度来源**：
- 多个用户同时使用系统
- CDP并发访问控制
- 资源竞争和锁竞争

**复杂度评估**：
- **并发用户数**：O(u)，其中 u 为并发用户数
- **CDP并发访问**：O(1)，通过锁机制保证
- **资源竞争**：O(r)，其中 r 为资源数量

**优化策略**：
1. **负载均衡**：分散用户请求，减少单点压力
2. **读写分离**：支持多读单写，提高并发性能
3. **资源池化**：使用连接池、线程池等，减少资源竞争

### 14.7 维护复杂度分析

#### 14.7.1 代码复杂度

**复杂度来源**：
- 多智能体系统代码量大
- 智能体间协作逻辑复杂
- 需要处理各种异常情况

**复杂度评估**：
- **代码行数**：O(L)，其中 L 为代码行数（预计10万+行）
- **模块数量**：O(m)，其中 m 为模块数量（约50-100个）
- **依赖关系**：O(d)，其中 d 为依赖数量

**优化策略**：
1. **模块化设计**：按功能模块化，减少模块间耦合
2. **接口标准化**：统一接口标准，减少集成复杂度
3. **代码复用**：提取公共逻辑，减少重复代码

#### 14.7.2 测试复杂度

**复杂度来源**：
- 多智能体系统测试需要模拟智能体协作
- 需要测试各种协作场景
- 需要测试异常处理和容错机制

**复杂度评估**：
- **测试用例数量**：O(t)，其中 t 为测试用例数量（预计1000+）
- **测试场景数量**：O(s)，其中 s 为测试场景数量（约100+）
- **测试执行时间**：O(t × e)，其中 e 为单个用例执行时间

**优化策略**：
1. **单元测试**：为每个智能体编写单元测试
2. **集成测试**：测试智能体间协作
3. **自动化测试**：使用自动化测试框架，减少测试时间

### 14.8 复杂度优化总结

#### 14.8.1 总体复杂度评估

**系统总体复杂度**：
- **架构复杂度**：中等（通过协调器统一管理，复杂度可控）
- **算法复杂度**：中等（通过缓存和并行优化，性能可接受）
- **数据复杂度**：中等（通过优化存储和检索，性能可接受）
- **通信复杂度**：低（通过消息队列和异步通信，复杂度可控）
- **性能复杂度**：中等（通过并行和缓存优化，响应时间可接受）
- **维护复杂度**：中等（通过模块化和标准化，维护成本可控）

#### 14.8.2 关键优化策略

1. **架构层面**：
   - 通过协调器统一管理智能体通信
   - 使用消息队列和路由机制
   - 支持智能体分组和模块化

2. **算法层面**：
   - 使用缓存机制减少重复计算
   - 并行执行提高性能
   - 智能剪枝减少计算量

3. **数据层面**：
   - 优化存储结构
   - 使用索引和分区
   - 支持增量更新

4. **通信层面**：
   - 使用异步消息传递
   - 批量处理消息
   - 压缩消息内容

5. **性能层面**：
   - 并行执行智能体任务
   - 缓存常用结果
   - 优化LLM推理性能

6. **维护层面**：
   - 模块化设计
   - 接口标准化
   - 自动化测试

#### 14.8.3 复杂度风险与应对

**主要风险**：
1. **智能体数量增加**：随着功能扩展，智能体数量可能增加
2. **协作复杂度增加**：新的协作模式可能增加系统复杂度
3. **性能瓶颈**：高并发场景下可能出现性能瓶颈

**应对策略**：
1. **智能体数量控制**：通过智能体分组和功能聚合，控制智能体数量
2. **协作模式标准化**：建立协作模式模板，减少新增复杂度
3. **性能监控和优化**：建立性能监控体系，及时发现和优化性能瓶颈

#### 14.8.4 复杂度演进路径

**短期（1-3个月）**：
- 完成基础架构搭建
- 实现核心智能体和协作机制
- 建立基础性能优化机制

**中期（3-6个月）**：
- 优化智能体协作效率
- 完善缓存和并行机制
- 建立性能监控体系

**长期（6-12个月）**：
- 持续优化系统性能
- 扩展智能体功能
- 建立完善的测试和维护体系
