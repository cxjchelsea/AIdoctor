# AI医生系统 - 服务与工具的对应关系

> **文档定位**：本文档说明AI医生系统中服务与工具的对应关系，以及主Agent（Clinical Agent Brain）的架构定位。  
> **架构原则**：单主Agent + 多工具Tools + CDP + 审计  
> **核心思想**：主Agent是唯一决策者，工具无状态、无独立目标，只按主Agent调用执行并返回结构化结果

---

## 一、架构定位说明

### 1.1 主Agent（Clinical Agent Brain）定位

**主Agent是系统的"大脑"**：
- **唯一决策者**：主Agent具备自主决策、自主调用工具、停止/升级/拒答能力
- **唯一最终结论提交者**：只有主Agent可以写入CDP并提交最终结论
- **工具调用者**：主Agent根据CDP状态和AgentState，决定调用哪些工具
- **证据融合者**：主Agent内部进行evidence fusion和conflict resolution

**主Agent的职责**：
- CDP（临床决策包）的创建、更新、版本控制、回放、回退
- 编排5步AI循证诊断流程（临床诊疗态）
- 编排健康筛查流程（健康管理态，A路径A1-A5）
- 根据CDP状态和AgentState，决定调用哪些工具
- 处理工具返回的结果，进行evidence fusion和conflict resolution
- 管理CDP状态流转和诊断流程状态
- 决定停止、升级、拒答

**技术实现**：
- `diagnosis-service` (Java) - 主Agent服务
- 包含主Agent运行循环：Observe→Plan→Act→Update→Evaluate→Stop/Escalate

### 1.2 工具（Tools）定位

**工具是系统的"能力模块"**：
- **无独立目标**：工具不维护长期策略状态，无独立决策能力
- **无状态**：工具每次调用都是独立的，不维护长期状态
- **按主Agent调用执行**：工具只按主Agent的调用执行，不自主决策
- **返回结构化结果**：工具返回payload、evidence、suggestedWrites给主Agent

**工具的设计原则**：
- 一个工具对应一个服务（按功能拆分）
- 工具无状态，每次调用都是独立的
- 工具从CDP读取输入，返回结构化结果
- 工具不直接写入CDP，只通过suggestedWrites建议主Agent写入

---

## 二、服务与工具的对应关系

### 2.1 工具服务（8个工具）

实际上是一个工具对应一个服务：

| 服务                              | 对应工具 | 工具ID | 工具类型 | 主要功能 |
| --------------------------------- | -------- | ------ | -------- | -------- |
| `health-state-assessment-service` | 工具0    | tool_0 | Deterministic | 健康状态判定、入口判定流程、工作态判定、危险信号检查、健康管理计划生成 |
| `clinical-parsing-service`        | 工具1    | tool_1 | Deterministic | 病例理解、医学概念识别与归一化、结构化提取、多模态理解、歧义表达判定 |
| `dialog-service`                  | 工具2    | tool_2 | Generative | 主动问诊、信息缺口识别、智能追问生成、自然语言理解与生成 |
| `diagnosis-engine-service`        | 工具3    | tool_3 | Deterministic | 五引擎融合诊断（规则/知识图谱/统计模型/LLM/鉴别诊断）、知识图谱推理、证据分析 |
| `workup-planner-service`          | 工具4    | tool_4 | Deterministic | 检查价值评估、信息增益计算、验证计划构建 |
| `treatment-engine-service`        | 工具5    | tool_5 | Generative | 治疗方案推理、药物推荐、非药物治疗建议 |
| `risk-assessment-service`         | 工具6    | tool_6 | Deterministic | 高危识别、紧急程度分级、复评与升级规则、终点结论包构建 |
| `explanation-service`             | 工具7    | tool_7 | Generative | 证据链构建、推理路径可视化、自然语言解释生成 |

### 2.2 主Agent服务

#### diagnosis-service（主Agent，非工具）

**核心定位**：主Agent（Clinical Agent Brain），系统的唯一决策者

**主要功能**：
- **CDP管理**：创建、更新、版本控制、回放、回退临床决策包（CDP）
- **主Agent运行循环**：Observe→Plan→Act→Update→Evaluate→Stop/Escalate
- **工具调用决策**：根据CDP状态和AgentState，决定调用哪些工具
- **证据融合与冲突解决**：处理工具返回的结果，进行evidence fusion和conflict resolution
- **流程编排**：
  - 编排5步AI循证诊断流程（临床诊疗态）
  - 编排健康筛查流程（健康管理态，A路径A1-A5）
- **状态管理**：管理CDP状态流转和诊断流程状态
- **停止/升级/拒答决策**：决定何时停止、何时升级、何时拒答

**在系统中的位置**：位于所有工具服务的上层，作为唯一决策中心

**架构关系**：
```
主Agent（diagnosis-service）
  ↓ 调用工具（ToolContext）
工具服务（8个工具服务）
  ↓ 返回结构化结果（ToolResult）
主Agent
  ↓ 写入CDP + 记录AuditTrail
CDP（Clinical Decision Package）+ AuditTrail（审计轨迹）
```

### 2.3 辅助服务

#### ocr-service（OCR服务）

**主要功能**：
- **图像识别**：识别医疗报告图片
- **报告OCR**：从图片中提取文字信息
- **结构化数据提取**：将OCR结果转换为结构化数据

**使用场景**：被 `clinical-parsing-service`（tool_1）内部调用，支持多模态输入

#### examination-service（检查服务）

**主要功能**：
- **检查管理**：管理检查相关的业务逻辑
- **检查结果处理**：处理检查结果的存储和查询
- **业务服务层功能**：提供检查相关的业务服务

**技术栈**：Java服务

---

## 三、架构关系图

```
┌─────────────────────────────────────┐
│  主Agent层（Clinical Agent Brain）   │
│  diagnosis-service (Java)            │  ← 唯一决策者、工具调用者
│  - CDP管理                           │
│  - 主Agent运行循环                   │
│  - 工具调用决策                       │
│  - 证据融合与冲突解决                 │
│  - 停止/升级/拒答决策                 │
└──────────────┬──────────────────────┘
               │ 调用工具（ToolContext）
┌──────────────▼──────────────────────┐
│  工具服务层（8个工具服务）            │
│  - health-state-assessment-service  │  ← tool_0
│  - clinical-parsing-service         │  ← tool_1
│  - dialog-service                   │  ← tool_2
│  - diagnosis-engine-service         │  ← tool_3
│  - workup-planner-service           │  ← tool_4
│  - treatment-engine-service         │  ← tool_5
│  - risk-assessment-service          │  ← tool_6
│  - explanation-service               │  ← tool_7
└──────────────┬──────────────────────┘
               │ 返回结构化结果（ToolResult）
┌──────────────▼──────────────────────┐
│  数据层（CDP + AuditTrail）          │
│  - CDP（Clinical Decision Package） │  ← 唯一事实源
│  - AuditTrail（审计轨迹）            │  ← 所有操作记录
└─────────────────────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  辅助服务层（通用能力）               │
│  - ocr-service                       │  ← OCR能力
│  - examination-service               │  ← 业务服务
└─────────────────────────────────────┘
```

**架构特点**：
- **8个工具对应8个服务**（按功能拆分）
- **主Agent服务（`diagnosis-service`）**负责决策和工具调用
- **辅助服务（`ocr-service`、`examination-service`）**提供通用能力
- **CDP作为唯一事实源**，所有工具从CDP读取，主Agent写入CDP
- **AuditTrail记录所有操作**，确保可追溯性

**设计依据**：
1. 按功能拆分，职责单一
2. 独立演进，便于升级
3. 微服务原则，独立部署
4. 主Agent模式，统一决策

因此，服务设计遵循"一个工具一个服务"的原则，主Agent作为唯一决策者，工具无状态、无独立目标，只按主Agent调用执行。

---

## 四、各服务与工具功能详细说明

### 4.1 工具服务（8个工具）

#### 1. health-state-assessment-service（工具0，tool_0）- 健康状态判定工具

**工具ID**：`tool_0`  
**工具类型**：Deterministic  
**核心目标**：判断"这个人，现在需要被当成'病人'对待吗？"

**主要功能**：
- **入口判定流程（P0模块）**：执行5步入口判定（接收输入、识别症状、方向澄清、危险信号检查、路径选择）
- **工作态判定**：区分健康管理态（wellness_mode）和临床诊疗态（clinical_mode）
- **风险评估**：症状严重程度评估、早期风险信号识别、红旗信号识别、风险等级计算（L1/L2/L3/L4）
- **健康管理计划生成**：在健康管理态时生成包含风险识别、生活方式建议、随访计划的健康管理方案

**输入依赖（从CDP读取）**：
- `cdp.patient_state`（如有）
- 用户输入（初始主诉）
- 基本信息（年龄、性别等，通过ToolContext传递）

**输出payload结构**：
- `work_mode`：工作态（wellness_mode / clinical_mode）
- `needs_clinical_mode`：是否需要进入临床诊疗态
- `risk_level`：风险等级（L1/L2/L3/L4）
- `entry_assessment`：入口判定流程结果

**suggestedWrites（建议写回CDP）**：
- `cdp.health_state_assessment`：健康状态判定结果

**在系统中的位置**：系统入口工具，所有用户请求的第一站

---

#### 2. clinical-parsing-service（工具1，tool_1）- 病例理解工具

**工具ID**：`tool_1`  
**工具类型**：Deterministic  
**核心目标**：将非结构化的患者信息转换为结构化的临床要素

**主要功能**：
- **医学概念识别**：从文本中提取症状、疾病、药物等医学概念
- **概念归一化**：将用户自然语言描述转换为医学标准概念（CUI/ICD/SNOMED）
- **结构化提取**：提取症状、体征、检查等结构化数据
- **多模态理解**：支持文本、图片等多种输入形式（调用OCR服务）
- **歧义表达判定与追问**：识别歧义表达，生成追问问题

**输入依赖（从CDP读取）**：
- 用户输入（病历自由文本、对话内容）
- 检查单（通过ToolContext传递）
- 生命体征（通过ToolContext传递）

**输出payload结构**：
- `symptoms`：症状列表（结构化）
- `signs`：体征列表（结构化）
- `normalized_concepts`：归一化后的医学概念

**suggestedWrites（建议写回CDP）**：
- `cdp.patient_state.symptoms`：症状列表
- `cdp.patient_state.signs`：体征列表

**在系统中的位置**：结构化推理通道的起点，为后续诊断提供标准化输入

---

#### 3. dialog-service（工具2，tool_2）- 主动问诊工具

**工具ID**：`tool_2`  
**工具类型**：Generative  
**核心目标**：提供像医生一样的主动对话、智能追问能力

**主要功能**：
- **信息缺口识别**：识别诊断所需但尚未收集的关键信息
- **智能追问生成**：基于信息缺口和诊断需求，生成有针对性的追问问题
- **自然语言理解（NLU）**：理解用户的自然语言输入，提取关键信息
- **自然语言生成（NLG）**：将结构化问题转换为自然语言表达
- **对话上下文管理**：维护对话历史，支持多轮对话

**输入依赖（从CDP读取）**：
- `cdp.patient_state`：患者状态
- `cdp.ddx`：鉴别诊断列表（用于生成有针对性的追问）
- `cdp.uncertainty.missing_critical_info`：信息缺口

**输出payload结构**：
- `questions`：生成的追问问题列表
- `information_gaps`：信息缺口列表
- `question_plan`：问诊计划

**suggestedWrites（建议写回CDP）**：
- `cdp.patient_state`：更新患者状态（根据用户回答）
- `cdp.uncertainty.missing_critical_info`：更新信息缺口

**在系统中的位置**：主动问诊通道，与病例理解工具配合完成信息收集

---

#### 4. diagnosis-engine-service（工具3，tool_3）- 鉴别诊断工具

**工具ID**：`tool_3`  
**工具类型**：Deterministic  
**核心目标**：执行多引擎融合的鉴别诊断推理

**主要功能**：
- **五引擎融合诊断**：
  - 规则引擎：基于医学规则进行推理
  - 知识图谱引擎：基于知识图谱路径进行推理
  - 统计模型引擎：基于机器学习模型进行推理
  - LLM引擎：基于大语言模型进行推理（使用LangChain）
  - 鉴别诊断引擎：执行鉴别诊断分析
- **知识图谱推理**：基于DR.KNOWS知识图谱进行路径推理
- **三层分层分类器**：对疾病进行分层分类
- **证据分析**：分析支持诊断的证据，评估证据强度
- **结果融合**：融合多个引擎的诊断结果，生成最终诊断候选集（DDx）

**输入依赖（从CDP读取）**：
- `cdp.patient_state`：患者状态（症状、体征等）
- `cdp.ddx`：当前鉴别诊断列表（如有）

**输出payload结构**：
- `ddx_candidates`：鉴别诊断候选集（三层排序：首要假设/主要备选/必须排除）
- `reasoning_paths`：推理路径
- `evidence_analysis`：证据分析结果

**suggestedWrites（建议写回CDP）**：
- `cdp.ddx`：更新鉴别诊断列表
- `cdp.ddx.reasoning_paths`：推理路径

**在系统中的位置**：诊断推理的核心，结构化推理通道的核心引擎

---

#### 5. workup-planner-service（工具4，tool_4）- 检查建议工具

**工具ID**：`tool_4`  
**工具类型**：Deterministic  
**核心目标**：基于当前DDx和已有证据，建议下一步检查，并评估检查的价值

**主要功能**：
- **检查价值评估**：评估各项检查对诊断的价值和必要性
- **信息增益计算**：计算检查结果对诊断的信息增益
- **检查优先级排序**：根据价值和信息增益对检查进行排序
- **验证计划构建**：制定验证诊断假设的检查计划

**输入依赖（从CDP读取）**：
- `cdp.ddx`：鉴别诊断列表
- `cdp.patient_state`：患者状态
- `cdp.triage`：风险评估结果（如有）

**输出payload结构**：
- `workup_plan`：检查计划
- `recommended_examinations`：推荐的检查列表
- `verification_plan`：验证计划

**suggestedWrites（建议写回CDP）**：
- `cdp.workup_plan`：检查计划

**在系统中的位置**：诊断流程中的检查建议环节，帮助确定下一步需要进行的检查

---

#### 6. treatment-engine-service（工具5，tool_5）- 治疗建议工具

**工具ID**：`tool_5`  
**工具类型**：Generative  
**核心目标**：基于诊断结果，生成治疗方案和处置建议

**主要功能**：
- **治疗方案推理**：基于诊断结果和患者情况，推理合适的治疗方案
- **药物推荐**：推荐合适的药物（研发阶段不涉及具体剂量）
- **非药物治疗建议**：提供生活方式、康复训练等非药物治疗建议
- **治疗计划构建**：构建完整的治疗计划

**输入依赖（从CDP读取）**：
- `cdp.ddx`：鉴别诊断列表
- `cdp.patient_state`：患者状态
- `cdp.triage`：风险评估结果

**输出payload结构**：
- `treatment_plan`：治疗计划
- `medication_recommendations`：药物推荐列表
- `non_pharmacological_advice`：非药物治疗建议

**suggestedWrites（建议写回CDP）**：
- `cdp.management_plan`：治疗计划

**在系统中的位置**：诊断流程的后续环节，提供治疗建议

---

#### 7. risk-assessment-service（工具6，tool_6）- 风险评估工具

**工具ID**：`tool_6`  
**工具类型**：Deterministic  
**核心目标**：识别高危情况，评估紧急程度，决定是否需要立即升级处理

**主要功能**：
- **高危识别**：识别高危疾病和危险信号
- **严重程度评估**：评估疾病的严重程度
- **紧急程度分级**：评估紧急程度，决定处理优先级
- **复评与升级规则**：制定复评计划，确定升级处理规则
- **终点结论包构建**：构建包含诊断、风险、建议的完整结论包

**输入依赖（从CDP读取）**：
- `cdp.ddx`：鉴别诊断列表
- `cdp.patient_state`：患者状态
- `cdp.evidence_graph`：证据图（如有）

**输出payload结构**：
- `risk_level`：风险等级（L1/L2/L3/L4）
- `high_risk_diseases`：高危疾病列表
- `urgent_conditions`：紧急情况列表
- `review_plan`：复评计划

**suggestedWrites（建议写回CDP）**：
- `cdp.triage`：风险评估结果

**在系统中的位置**：贯穿诊断流程，在多个环节进行风险评估

---

#### 8. explanation-service（工具7，tool_7）- 证据链工具

**工具ID**：`tool_7`  
**工具类型**：Generative  
**核心目标**：生成完整的证据链，让系统的"结论"能被复核

**主要功能**：
- **证据链构建**：构建支持诊断的完整证据链
- **推理路径可视化**：可视化展示诊断推理路径
- **自然语言解释生成**：将证据链和推理路径转换为自然语言解释
- **可解释性增强**：提高系统诊断结果的可解释性和可信度

**输入依赖（从CDP读取）**：
- `cdp.ddx`：鉴别诊断列表
- `cdp.evidence_graph`：证据图
- `cdp.workup_plan`：检查计划（如有）
- `cdp.management_plan`：治疗计划（如有）

**输出payload结构**：
- `evidence_chain`：证据链
- `reasoning_path_visualization`：推理路径可视化
- `natural_language_explanation`：自然语言解释

**suggestedWrites（建议写回CDP）**：
- `cdp.evidence_graph`：证据图
- `cdp.explanation`：解释内容

**在系统中的位置**：语言与策略通道，为诊断结果提供可解释性支持

---

### 4.2 主Agent服务

#### diagnosis-service（主Agent，非工具）

**核心定位**：主Agent（Clinical Agent Brain），系统的唯一决策者

**主要功能**：
- **CDP管理**：创建、更新、版本控制、回放、回退临床决策包（CDP）
- **主Agent运行循环**：Observe→Plan→Act→Update→Evaluate→Stop/Escalate
- **工具调用决策**：根据CDP状态和AgentState，决定调用哪些工具
- **证据融合与冲突解决**：处理工具返回的结果，进行evidence fusion和conflict resolution
- **流程编排**：
  - 编排5步AI循证诊断流程（临床诊疗态）
  - 编排健康筛查流程（健康管理态，A路径A1-A5）
- **状态管理**：管理CDP状态流转和诊断流程状态
- **停止/升级/拒答决策**：决定何时停止、何时升级、何时拒答

**在系统中的位置**：位于所有工具服务的上层，作为唯一决策中心

**架构关系**：
- 主Agent调用工具，工具返回结构化结果
- 主Agent写入CDP，记录AuditTrail
- 主Agent是唯一可以写入CDP的服务

---

### 4.3 辅助服务

#### ocr-service（OCR服务）

**主要功能**：
- **图像识别**：识别医疗报告图片
- **报告OCR**：从图片中提取文字信息
- **结构化数据提取**：将OCR结果转换为结构化数据

**使用场景**：被 `clinical-parsing-service`（tool_1）内部调用，支持多模态输入

#### examination-service（检查服务）

**主要功能**：
- **检查管理**：管理检查相关的业务逻辑
- **检查结果处理**：处理检查结果的存储和查询
- **业务服务层功能**：提供检查相关的业务服务

**技术栈**：Java服务

---

## 五、工具调用流程

### 5.1 工具调用架构

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

### 5.2 工具调用原则

**工具无状态**：
- 工具每次调用都是独立的，不维护长期状态
- 工具从CDP读取输入，返回结构化结果
- 工具不直接写入CDP，只通过suggestedWrites建议主Agent写入

**主Agent决策权**：
- 主Agent决定调用哪些工具
- 主Agent决定是否接受工具的suggestedWrites
- 主Agent进行evidence fusion和conflict resolution
- 主Agent是唯一可以写入CDP的服务

**CDP唯一事实源**：
- 所有工具从CDP读取输入
- 所有工具返回的结果通过主Agent写入CDP
- CDP是系统内部一切推理的核心数据结构

**AuditTrail可追溯**：
- 所有工具调用记录到AuditTrail
- 所有CDP更新记录到AuditTrail
- 所有证据引用记录到AuditTrail

---

## 六、总结

### 6.1 架构特点

1. **单主Agent + 多工具**：
   - 主Agent是唯一决策者
   - 工具无状态、无独立目标，只按主Agent调用执行

2. **CDP唯一事实源**：
   - 所有工具从CDP读取输入
   - 主Agent写入CDP

3. **AuditTrail可追溯**：
   - 所有操作记录到AuditTrail
   - 确保可追溯性

4. **工具分类**：
   - Deterministic工具：规则、知识图谱、统计模型等
   - Generative工具：LLM生成自然语言等

### 6.2 服务与工具对应关系

- **8个工具对应8个服务**（按功能拆分）
- **主Agent服务（`diagnosis-service`）**负责决策和工具调用
- **辅助服务（`ocr-service`、`examination-service`）**提供通用能力

---

**文档版本**：v2.0  
**最后更新**：2026-02-04  
**维护者**：AI医生系统开发团队

