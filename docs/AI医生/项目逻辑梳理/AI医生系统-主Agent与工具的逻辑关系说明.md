# AI医生系统 - 主Agent与工具的逻辑关系说明

> **文档定位**：本文档详细说明AI医生系统中主Agent（Clinical Agent Brain）与工具（Tools）之间的逻辑关系，以及工具之间的协作机制，帮助理解系统的整体架构和协作流程。  
> **架构原则**：单主Agent + 多工具Tools + CDP + 审计  
> **参考文档**：
> - 《AI医生系统-服务与工具的对应关系.md》- 服务与工具对应关系
> - 《AI医生系统-服务执行顺序与系统完整性.md》- 工具调用顺序
> - 《AI医生系统-技术架构设计-核心架构.md》- 技术架构设计

---

## 📋 目录

1. [主Agent与工具逻辑关系](#一主agent与工具逻辑关系)
2. [工具分类与功能定位](#二工具分类与功能定位)
3. [主Agent与工具的映射关系](#三主agent与工具的映射关系)
4. [数据流转关系](#四数据流转关系)
5. [协作模式总结](#五协作模式总结)

---

## 一、主Agent与工具逻辑关系

### 1.1 架构分类

AI医生系统采用**单主Agent + 多工具Tools**架构，分为三类：

#### 1.1.1 主Agent服务（1个）

- **diagnosis-service** (Java)
  - **定位**：主Agent（Clinical Agent Brain），系统的唯一决策者
  - **职责**：CDP管理、工具调用决策、证据融合与冲突解决、流程编排、停止/升级/拒答决策
  - **特点**：具备自主决策能力，是唯一可以写入CDP的服务

#### 1.1.2 工具服务（8个，对应8个工具）

| 服务名称 | 工具ID | 工具类型 | 主要职责 |
|---------|--------|---------|---------|
| `health-state-assessment-service` | tool_0 | Deterministic | 健康状态判定、入口判定流程 |
| `clinical-parsing-service` | tool_1 | Deterministic | 病例理解、概念归一化 |
| `dialog-service` | tool_2 | Generative | 主动问诊、智能追问 |
| `diagnosis-engine-service` | tool_3 | Deterministic | 鉴别诊断、多引擎融合 |
| `workup-planner-service` | tool_4 | Deterministic | 检查建议、验证计划 |
| `treatment-engine-service` | tool_5 | Generative | 治疗推理、药物推荐 |
| `risk-assessment-service` | tool_6 | Deterministic | 风险评估、紧急程度分级 |
| `explanation-service` | tool_7 | Generative | 证据链构建、解释生成 |

#### 1.1.3 辅助服务（2个）

- **ocr-service** (Python)
  - **定位**：通用能力服务
  - **职责**：图像识别、报告OCR、结构化数据提取
  - **被调用**：被 `clinical-parsing-service`（tool_1）内部调用

- **examination-service** (Java)
  - **定位**：业务服务层
  - **职责**：检查管理、检查结果处理
  - **特点**：提供检查相关的业务逻辑

---

### 1.2 主Agent与工具依赖关系图

```
┌─────────────────────────────────────────────────────────┐
│              主Agent层（Clinical Agent Brain）            │
│              diagnosis-service                          │
│              （唯一决策者、工具调用者）                   │
│  - CDP管理                                               │
│  - 主Agent运行循环                                        │
│  - 工具调用决策                                           │
│  - 证据融合与冲突解决                                     │
│  - 停止/升级/拒答决策                                     │
└───────────────────┬───────────────────────────────────┘
                     │
                     │ 调用工具（ToolContext）
                     │
┌────────────────────▼───────────────────────────────────┐
│              工具服务层（8个工具服务）                    │
│                                                          │
│  ┌────────────────────────────────────────────────┐   │
│  │  health-state-assessment-service (tool_0)        │   │
│  │  └─→ [从CDP读取输入，返回结构化结果]            │   │
│  └────────────────────────────────────────────────┘   │
│                                                          │
│  ┌────────────────────────────────────────────────┐   │
│  │  clinical-parsing-service (tool_1)             │   │
│  │  ├─→ ocr-service (可选，内部调用)              │   │
│  │  └─→ [Neo4j知识图谱，内部调用]                  │   │
│  └────────────────────────────────────────────────┘   │
│                                                          │
│  ┌────────────────────────────────────────────────┐   │
│  │  dialog-service (tool_2)                      │   │
│  │  └─→ [从CDP读取输入，返回追问问题]             │   │
│  └────────────────────────────────────────────────┘   │
│                                                          │
│  ┌────────────────────────────────────────────────┐   │
│  │  diagnosis-engine-service (tool_3)             │   │
│  │  └─→ [Neo4j知识图谱，内部调用]                  │   │
│  └────────────────────────────────────────────────┘   │
│                                                          │
│  ┌────────────────────────────────────────────────┐   │
│  │  workup-planner-service (tool_4)               │   │
│  │  └─→ [从CDP读取输入，返回检查计划]              │   │
│  └────────────────────────────────────────────────┘   │
│                                                          │
│  ┌────────────────────────────────────────────────┐   │
│  │  treatment-engine-service (tool_5)             │   │
│  │  └─→ [从CDP读取输入，返回治疗计划]              │   │
│  └────────────────────────────────────────────────┘   │
│                                                          │
│  ┌────────────────────────────────────────────────┐   │
│  │  risk-assessment-service (tool_6)              │   │
│  │  └─→ [从CDP读取输入，返回风险评估]              │   │
│  └────────────────────────────────────────────────┘   │
│                                                          │
│  ┌────────────────────────────────────────────────┐   │
│  │  explanation-service (tool_7)                  │   │
│  │  └─→ [从CDP读取输入，返回证据链]                │   │
│  └────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────┘
                     │
                     │ 返回结构化结果（ToolResult）
                     │
┌────────────────────▼───────────────────────────────────┐
│              数据层（CDP + AuditTrail）                 │
│  - CDP（Clinical Decision Package）                    │  ← 唯一事实源
│  - AuditTrail（审计轨迹）                              │  ← 所有操作记录
└──────────────────────────────────────────────────────────┘
                     │
                     │ 被调用
                     │
┌────────────────────▼───────────────────────────────────┐
│              辅助服务层（通用能力）                       │
│  ┌────────────────────────────────────────────────┐   │
│  │  ocr-service                                    │   │
│  │  └─→ [无依赖]                                   │   │
│  └────────────────────────────────────────────────┘   │
│  ┌────────────────────────────────────────────────┐   │
│  │  examination-service                            │   │
│  │  └─→ [业务服务层]                               │   │
│  └────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────┘
```

---

### 1.3 主Agent与工具调用关系详解

#### 1.3.1 主Agent服务（diagnosis-service）的调用关系

**调用所有工具服务**：
- 作为唯一决策者，`diagnosis-service`（主Agent）负责调用所有8个工具服务
- 根据CDP状态和AgentState，按顺序或并行调用相应的工具
- 管理工具间的数据流转和状态同步（通过CDP）

**调用特点**：
- **同步调用**：关键路径工具（如健康状态判定、病例理解、诊断引擎）
- **异步调用**：非关键路径工具（如检查建议、治疗建议、解释生成）
- **并行调用**：可以并行调用多个工具，提高效率

**调用流程**：
```
主Agent（diagnosis-service）
  ↓ 创建ToolContext
  ↓ 包含：traceId、cdp引用、agentState摘要、约束（成本/时间/风险）
工具服务（如tool_3）
  ↓ 从CDP读取输入
  ↓ 执行工具逻辑
  ↓ 返回ToolResult
  ↓ 包含：status、payload、evidence、quality、suggestedWrites、errors
主Agent
  ↓ 进行evidence fusion和conflict resolution
  ↓ 根据suggestedWrites写入CDP
  ↓ 记录AuditTrail
CDP + AuditTrail
```

#### 1.3.2 工具服务间的调用关系

**重要原则**：工具服务之间不直接调用，所有数据流转通过CDP

**工具内部调用**（工具内部实现，不涉及主Agent）：
1. **clinical-parsing-service（tool_1）内部调用 ocr-service**
   - 当需要处理图片输入时，tool_1内部调用ocr-service
   - 调用方式：同步HTTP调用
   - 调用场景：多模态理解（文本+图片）
   - **注意**：这是工具内部实现，不涉及主Agent

**工具通过CDP共享数据**（推荐方式）：
- 所有工具从CDP读取输入
- 所有工具返回的结果通过主Agent写入CDP
- 工具之间不直接传递数据，而是通过CDP共享
- 这保证了数据的一致性和可追溯性

**示例**：
```
工具A（tool_1）：
  - 从CDP读取：cdp.patient_state.user_input
  - 处理：概念归一化
  - 返回suggestedWrites：cdp.patient_state.symptoms

主Agent：
  - 接收tool_1的suggestedWrites
  - 写入CDP：cdp.patient_state.symptoms

工具B（tool_3）：
  - 从CDP读取：cdp.patient_state.symptoms（由tool_1写入）
  - 处理：诊断推理
  - 返回suggestedWrites：cdp.ddx
```

---

### 1.4 主Agent运行循环与工具调用顺序

#### 1.4.1 系统入口流程

```
用户输入
    ↓
主Agent（diagnosis-service，创建CDP）
    ↓
调用tool_0（健康状态判定工具）
    ├─→ 入口判定流程（Step 1-5）
    ├─→ 工作态判定（wellness_mode / clinical_mode）
    └─→ 危险信号检查
    ↓
主Agent根据tool_0的结果决定后续流程
```

#### 1.4.2 健康管理态流程（路径A）

```
主Agent调用tool_0
    ↓
tool_0返回：work_mode = wellness_mode
    ↓
主Agent执行健康管理流程（主要在tool_0内部完成）
    ├─→ A1: 需求分类
    ├─→ A2: 通用最小健康档案
    ├─→ A3: 进入对应分支
    ├─→ A4: 统一结果页输出
    └─→ A5: 随访闭环
    ↓
主Agent写入CDP：cdp.wellness_plan
```

**涉及工具**：
- `tool_0`（主要执行）
- 主Agent负责流程编排和CDP管理

#### 1.4.3 临床诊疗态流程（路径B）- 5步AI循证诊断流程

```
主Agent调用tool_0
    ↓
tool_0返回：work_mode = clinical_mode
    ↓
主Agent执行5步AI循证诊断流程

Step 1: 识别问题
    ├─→ 主Agent调用tool_1（病例理解工具）
    └─→ 主Agent调用tool_2（主动问诊工具）

Step 2: 构建鉴别诊断候选集并分层
    └─→ 主Agent调用tool_3（鉴别诊断工具）
        └─→ 主Agent调用tool_6（风险评估工具，可选）

Step 3: 组织候选集并建立分流路径
    ├─→ 主Agent调用tool_3（推理组织器）
    └─→ 主Agent调用tool_2（分流路径设计）

Step 4: 采集关键证据并形成排序与验证计划
    ├─→ 主Agent调用tool_2（主动问诊）
    ├─→ 主Agent调用tool_3（证据分析器）
    ├─→ 主Agent调用tool_4（检查建议）
    └─→ 主Agent调用tool_6（风险评估）

Step 5: 回填证据并输出终点结论包
    ├─→ 主Agent调用tool_3（证据回填）
    ├─→ 主Agent调用tool_7（解释生成）
    ├─→ 主Agent调用tool_5（治疗推理）
    └─→ 主Agent生成终点结论包
```

---

### 1.5 主Agent与工具协作模式

#### 1.5.1 顺序协作

**特点**：工具按顺序执行，前一个工具的输出（通过CDP）作为后一个工具的输入

**示例**：
```
主Agent调用tool_1（病例理解）
  ↓ tool_1返回suggestedWrites：cdp.patient_state.symptoms
主Agent写入CDP
  ↓
主Agent调用tool_3（鉴别诊断）
  ↓ tool_3从CDP读取：cdp.patient_state.symptoms
  ↓ tool_3返回suggestedWrites：cdp.ddx
主Agent写入CDP
```

#### 1.5.2 并行协作

**特点**：主Agent并行调用多个工具，最后汇总结果

**示例**：
```
主Agent并行调用：
  ├─→ tool_3（鉴别诊断）
  ├─→ tool_4（检查建议）
  └─→ tool_5（治疗推理）
    ↓
主Agent等待所有工具返回
    ↓
主Agent进行evidence fusion和conflict resolution
    ↓
主Agent写入CDP
```

#### 1.5.3 循环协作

**特点**：主Agent循环调用工具，直到满足条件

**示例**：
```
主Agent循环调用tool_2（主动问诊）
  ↓ 用户回答
  ↓ 主Agent更新CDP
  ↓ 主Agent检查停止条件
  ↓ 如果未满足，继续调用tool_2
  ↓ 如果满足，停止循环
```

#### 1.5.4 证据融合与冲突解决

**特点**：主Agent内部进行evidence fusion和conflict resolution，而非工具协商

**示例**：
```
主Agent调用tool_3（鉴别诊断）
  ↓ tool_3返回：疾病A概率0.8
主Agent调用tool_6（风险评估）
  ↓ tool_6返回：疾病A风险等级L2
主Agent进行evidence fusion：
  - 融合tool_3和tool_6的结果
  - 检查是否存在冲突
  - 如果冲突，进行conflict resolution
  - 最终决定：疾病A概率0.75，风险等级L2
主Agent写入CDP
```

---

## 二、工具分类与功能定位

### 2.1 工具分类

AI医生系统的8个工具分为两类：

#### 2.1.1 通道1：结构化推理通道（决定"该往哪想"）

**主要工具**：
- **tool_0**：健康状态判定（规则推理、风险评估）
- **tool_1**：病例理解（概念归一化、结构化提取）
- **tool_3**：鉴别诊断（知识图谱推理、多引擎融合）
- **tool_4**：检查建议（检查价值评估、信息增益计算）
- **tool_6**：风险评估（风险识别、紧急程度评估）

**技术实现**：
- 知识图谱路径检索与排序（DR.KNOWS方法）
- 规则引擎、统计模型、贝叶斯推理
- 多引擎融合诊断
- 医学概念标准化（CUI/ICD/SNOMED）

**输出**：DDx候选 + 证据结构 + 推理路径（写入CDP）

#### 2.1.2 通道2：语言与策略通道（决定"怎么说、怎么问"）

**主要工具**：
- **tool_2**：主动问诊（生成问诊问题、自然语言对话）
- **tool_5**：治疗建议（生成治疗方案、自然语言表达）
- **tool_7**：证据链工具（生成解释和说明、推理路径可视化）

**技术实现**：
- LLM（大语言模型）
- NLG（自然语言生成）
- 对话生成、解释生成
- 自然语言理解（NLU）

**输出**：自然语言问诊、解释、建议

#### 2.1.3 双通道协作

**协作机制**：
```
通道1工具（结构化推理）
    ↓
生成结构化结果（DDx、检查建议等）
    ↓
写入CDP（结构化数据）
    ↓
主Agent读取CDP
    ↓
调用通道2工具（语言与策略）
    ↓
生成自然语言表达
    ↓
输出给用户
```

**关键原则**：
- **通道1决定"该往哪想"**：结构化推理通道负责临床逻辑推理
- **通道2决定"怎么说"**：语言与策略通道负责将结构化结果转换为自然语言
- **CDP作为桥梁**：通道1的输出写入CDP，通道2基于CDP生成自然语言表达

---

### 2.2 工具依赖关系图

```
┌─────────────────────────────────────────────────────────┐
│                   主Agent（Clinical Agent Brain）         │
│              diagnosis-service                          │
│              （唯一决策者、工具调用者）                   │
└───────────────────┬─────────────────────────────────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
        ▼                       ▼
┌───────────────┐      ┌───────────────────────────────┐
│ 健康管理态     │      │ 临床诊疗态                      │
│ （路径A）      │      │ （路径B）                        │
│ tool_0        │      │ tool_0 → tool_1 → tool_2 → ...│
└───────────────┘      └───────────┬───────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │ tool_1：病例理解                │
                    │ （概念归一化、结构化提取）       │
                    └───────────┬───────────────────┘
                                │
                                ▼
                    ┌───────────────────────────────┐
                    │ tool_2：主动问诊                │
                    │ （信息缺口识别、智能追问）       │
                    └───────────┬───────────────────┘
                                │
                                ▼
                    ┌───────────────────────────────┐
                    │ tool_3：鉴别诊断                │
                    │ （知识图谱推理、多引擎融合）     │
                    └───────────┬───────────────────┘
                                │
                    ┌───────────┼───────────┐
                    │           │           │
                    ▼           ▼           ▼
        ┌───────────────┐ ┌───────────┐ ┌───────────┐
        │ tool_4：检查建议 │ │ tool_5：   │ │ tool_6：   │
        │ （检查价值评估） │ │ 治疗推理  │ │ 风险评估  │
        └───────────────┘ └───────────┘ └───────────┘
                    │           │           │
                    └───────────┼───────────┘
                                │
                                ▼
                    ┌───────────────────────────────┐
                    │ tool_7：证据链工具             │
                    │ （证据链构建、解释生成）         │
                    └───────────────────────────────┘
```

---

### 2.3 工具执行顺序

#### 2.3.1 健康管理态流程（路径A）

```
主Agent调用tool_0（健康状态判定）
    ↓
tool_0返回：work_mode = wellness_mode
    ↓
主Agent执行健康管理流程（主要在tool_0内部完成）
    ├─→ A1: 需求分类
    ├─→ A2: 通用最小健康档案
    ├─→ A3: 进入对应分支
    ├─→ A4: 统一结果页输出
    └─→ A5: 随访闭环
```

**涉及工具**：
- **tool_0**：主要执行
- 主Agent负责流程编排和CDP管理

#### 2.3.2 临床诊疗态流程（路径B）- 5步AI循证诊断流程

```
主Agent调用tool_0（健康状态判定）
    ↓
tool_0返回：work_mode = clinical_mode
    ↓
主Agent执行5步AI循证诊断流程

Step 1: 识别问题
    ├─→ 主Agent调用tool_1（病例理解）
    └─→ 主Agent调用tool_2（主动问诊）

Step 2: 构建鉴别诊断候选集并分层
    └─→ 主Agent调用tool_3（鉴别诊断）
        └─→ 主Agent调用tool_6（风险评估，可选）

Step 3: 组织候选集并建立分流路径
    ├─→ 主Agent调用tool_3（推理组织器）
    └─→ 主Agent调用tool_2（分流路径设计）

Step 4: 采集关键证据并形成排序与验证计划
    ├─→ 主Agent调用tool_2（主动问诊）
    ├─→ 主Agent调用tool_3（证据分析器）
    ├─→ 主Agent调用tool_4（检查建议）
    └─→ 主Agent调用tool_6（风险评估）

Step 5: 回填证据并输出终点结论包
    ├─→ 主Agent调用tool_3（证据回填）
    ├─→ 主Agent调用tool_7（解释生成）
    ├─→ 主Agent调用tool_5（治疗推理）
    └─→ 主Agent生成终点结论包
```

---

### 2.4 工具协作模式

#### 2.4.1 顺序协作

**特点**：工具按顺序执行，前一个工具的输出（通过CDP）作为后一个工具的输入

**示例**：
```
主Agent调用tool_1（病例理解）
  ↓ tool_1返回suggestedWrites：cdp.patient_state.symptoms
主Agent写入CDP
  ↓
主Agent调用tool_3（鉴别诊断）
  ↓ tool_3从CDP读取：cdp.patient_state.symptoms
  ↓ tool_3返回suggestedWrites：cdp.ddx
主Agent写入CDP
```

#### 2.4.2 并行协作

**特点**：主Agent并行调用多个工具，最后汇总结果

**示例**：
```
主Agent并行调用：
  ├─→ tool_3（鉴别诊断）
  ├─→ tool_4（检查建议）
  └─→ tool_5（治疗推理）
    ↓
主Agent等待所有工具返回
    ↓
主Agent进行evidence fusion和conflict resolution
    ↓
主Agent写入CDP
```

#### 2.4.3 循环协作

**特点**：主Agent循环调用工具，直到满足条件

**示例**：
```
主Agent循环调用tool_2（主动问诊）
  ↓ 用户回答
  ↓ 主Agent更新CDP
  ↓ 主Agent检查停止条件
  ↓ 如果未满足，继续调用tool_2
  ↓ 如果满足，停止循环
```

#### 2.4.4 证据融合与冲突解决

**特点**：主Agent内部进行evidence fusion和conflict resolution，而非工具协商

**示例**：
```
主Agent调用tool_3（鉴别诊断）
  ↓ tool_3返回：疾病A概率0.8
主Agent调用tool_6（风险评估）
  ↓ tool_6返回：疾病A风险等级L2
主Agent进行evidence fusion：
  - 融合tool_3和tool_6的结果
  - 检查是否存在冲突
  - 如果冲突，进行conflict resolution
  - 最终决定：疾病A概率0.75，风险等级L2
主Agent写入CDP
```

---

### 2.5 工具功能互补关系

#### 2.5.1 信息收集阶段

**tool_1 + tool_2**：
- **tool_1**：将非结构化输入转换为结构化数据
- **tool_2**：识别信息缺口，生成追问问题
- **协作**：tool_1提供结构化基础，tool_2基于此生成有针对性的追问

#### 2.5.2 诊断推理阶段

**tool_3 + tool_6**：
- **tool_3**：生成诊断候选集
- **tool_6**：评估风险，调整诊断优先级
- **协作**：tool_6的风险评估结果影响tool_3的诊断排序（主Agent进行融合）

#### 2.5.3 检查建议阶段

**tool_3 + tool_4 + tool_6**：
- **tool_3**：提供诊断候选集
- **tool_4**：评估检查价值，生成检查建议
- **tool_6**：提供风险等级，影响检查优先级
- **协作**：三个工具的结果由主Agent融合，共同决定检查建议

#### 2.5.4 治疗建议阶段

**tool_3 + tool_5 + tool_6**：
- **tool_3**：提供诊断结果
- **tool_5**：生成治疗方案
- **tool_6**：提供风险等级，影响治疗方案
- **协作**：三个工具的结果由主Agent融合，共同决定治疗方案

#### 2.5.5 解释生成阶段

**tool_7 + 其他工具**：
- **tool_7**：生成解释和说明
- **其他工具**：提供推理路径、检查建议、治疗方案
- **协作**：tool_7整合所有工具的输出（通过CDP），生成完整的解释

---

## 三、主Agent与工具的映射关系

### 3.1 一对一映射

**原则**：一个工具对应一个服务，一个服务对应一个工具

| 服务 | 工具ID | 映射关系 |
|------|--------|---------|
| `health-state-assessment-service` | tool_0 | 一对一 |
| `clinical-parsing-service` | tool_1 | 一对一 |
| `dialog-service` | tool_2 | 一对一 |
| `diagnosis-engine-service` | tool_3 | 一对一 |
| `workup-planner-service` | tool_4 | 一对一 |
| `treatment-engine-service` | tool_5 | 一对一 |
| `risk-assessment-service` | tool_6 | 一对一 |
| `explanation-service` | tool_7 | 一对一 |

### 3.2 主Agent与工具的关系

**diagnosis-service（主Agent）**：
- **不是工具**：主Agent不属于8个工具之一
- **调用所有工具**：负责调用所有8个工具
- **流程编排**：决定工具的调用顺序和协作方式
- **唯一决策者**：是唯一可以写入CDP的服务

### 3.3 辅助服务与工具的关系

**ocr-service**：
- **不是工具**：辅助服务不属于8个工具之一
- **被tool_1调用**：被 `clinical-parsing-service`（tool_1）内部调用
- **提供能力**：为tool_1提供多模态理解能力

**examination-service**：
- **不是工具**：业务服务不属于8个工具之一
- **独立服务**：提供检查相关的业务逻辑
- **与工具关系**：可能被多个工具间接使用（通过业务层）

---

## 四、数据流转关系

### 4.1 CDP（临床决策包）作为数据载体

**核心原则**：所有工具通过CDP共享数据，而不是直接传递数据

**CDP结构**：
```json
{
  "id": "cdp_123",
  "version": 1,
  "health_state_assessment": {},  // tool_0写入
  "patient_state": {},            // tool_1写入
  "ddx": [],                      // tool_3写入
  "workup_plan": {},              // tool_4写入
  "management_plan": {},          // tool_5写入
  "triage": {},                   // tool_6写入
  "evidence_graph": {}            // tool_7写入
}
```

### 4.2 数据流转路径

#### 4.2.1 健康管理态数据流转

```
用户输入
    ↓
主Agent（diagnosis-service，创建CDP）
    ↓
主Agent调用tool_0（健康状态判定）
    ├─→ 从CDP读取：用户输入
    └─→ 返回suggestedWrites：CDP.health_state_assessment
    ↓
主Agent写入CDP：cdp.health_state_assessment
    ↓
主Agent执行健康管理流程（主要在tool_0内部完成）
    ↓
主Agent写入CDP：cdp.wellness_plan
    ↓
返回结果给用户
```

#### 4.2.2 临床诊疗态数据流转

```
用户输入
    ↓
主Agent（diagnosis-service，创建CDP）
    ↓
主Agent调用tool_0（健康状态判定）
    └─→ 返回suggestedWrites：CDP.health_state_assessment
    ↓
主Agent写入CDP：cdp.health_state_assessment
    ↓
主Agent调用tool_1（病例理解）
    ├─→ 从CDP读取：cdp.health_state_assessment
    └─→ 返回suggestedWrites：CDP.patient_state
    ↓
主Agent写入CDP：cdp.patient_state
    ↓
主Agent调用tool_2（主动问诊）
    ├─→ 从CDP读取：cdp.patient_state
    └─→ 返回suggestedWrites：CDP.patient_state（更新）
    ↓
主Agent写入CDP：cdp.patient_state（更新）
    ↓
主Agent调用tool_3（鉴别诊断）
    ├─→ 从CDP读取：cdp.patient_state
    └─→ 返回suggestedWrites：CDP.ddx
    ↓
主Agent写入CDP：cdp.ddx
    ↓
主Agent调用tool_6（风险评估）
    ├─→ 从CDP读取：cdp.ddx
    └─→ 返回suggestedWrites：CDP.triage
    ↓
主Agent写入CDP：cdp.triage
    ↓
主Agent调用tool_4（检查建议）
    ├─→ 从CDP读取：cdp.ddx, cdp.triage
    └─→ 返回suggestedWrites：CDP.workup_plan
    ↓
主Agent写入CDP：cdp.workup_plan
    ↓
主Agent调用tool_5（治疗建议）
    ├─→ 从CDP读取：cdp.ddx, cdp.triage
    └─→ 返回suggestedWrites：CDP.management_plan
    ↓
主Agent写入CDP：cdp.management_plan
    ↓
主Agent调用tool_7（证据链工具）
    ├─→ 从CDP读取：cdp.ddx, cdp.workup_plan, cdp.management_plan
    └─→ 返回suggestedWrites：CDP.evidence_graph
    ↓
主Agent写入CDP：cdp.evidence_graph
    ↓
返回结果给用户
```

---

## 五、协作模式总结

### 5.1 主Agent与工具协作模式

| 协作模式 | 特点 | 示例 |
|---------|------|------|
| **顺序协作** | 工具按顺序执行 | tool_1 → tool_3 → tool_5 |
| **并行协作** | 主Agent并行调用多个工具 | tool_3 + tool_4 + tool_5 |
| **循环协作** | 主Agent循环调用工具，直到满足条件 | tool_2 → tool_2 → tool_2 |
| **证据融合** | 主Agent内部进行evidence fusion和conflict resolution | tool_3 + tool_6 → 主Agent融合 |

### 5.2 关键设计原则

1. **单一职责原则**：每个工具只负责一个功能领域
2. **依赖最小化**：工具间不直接调用，通过CDP共享数据
3. **数据共享**：通过CDP共享数据，保证一致性
4. **可追溯性**：所有推理过程可追溯、可审计（AuditTrail）
5. **可扩展性**：支持工具的独立演进和升级
6. **主Agent决策权**：主Agent是唯一决策者，工具无独立目标

---

## 六、总结

### 6.1 主Agent与工具逻辑关系总结

- **1个主Agent服务**：diagnosis-service（唯一决策者）
- **8个工具服务**：对应8个工具（tool_0到tool_7）
- **工具依赖**：主Agent调用所有工具，工具间不直接调用
- **数据流转**：通过CDP共享数据，保证一致性和可追溯性
- **协作模式**：顺序、并行、循环、证据融合四种模式

### 6.2 架构特点

- **单主Agent + 多工具**：主Agent是唯一决策者，工具无状态、无独立目标
- **CDP唯一事实源**：所有工具从CDP读取，主Agent写入CDP
- **AuditTrail可追溯**：所有操作记录到AuditTrail
- **双通道协作**：通道1生成结构化结果，通道2生成自然语言表达

---

**文档版本**：v2.0  
**最后更新**：2026-02-04  
**维护者**：AI医生系统开发团队

