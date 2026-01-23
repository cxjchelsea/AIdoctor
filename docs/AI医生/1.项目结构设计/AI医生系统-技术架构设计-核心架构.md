# AI医生系统 - 技术架构设计（核心架构）

> **文档定位**：本文档是AI医生系统的**技术架构设计**的核心架构部分，包含整体架构设计、多智能体系统设计和协作流程。  
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
| **五、多智能体协作流程** | | |
| 5.1 健康状态判定 → 分叉 | 3.1 健康状态判定 → 分叉 | 协作流程 → 流程定义 |
| 5.2 健康管理态协作流程 | 3.2 健康管理态详细流程 | 协作流程 → 流程定义 |
| 5.3 临床诊疗态协作流程 | 3.3 临床诊疗态详细流程 | 协作流程 → 流程定义 |

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

## 相关文档

- [AI医生系统-技术架构设计-智能体详细设计](./AI医生系统-技术架构设计-智能体详细设计.md)
- [AI医生系统-技术架构设计-核心技术组件](./AI医生系统-技术架构设计-核心技术组件.md)
- [AI医生系统-技术架构设计-CDP数据与状态管理](./AI医生系统-技术架构设计-CDP数据与状态管理.md)
- [AI医生系统-技术架构设计-索引](./AI医生系统-技术架构设计-索引.md)

