# AI医生系统 - 主Agent架构设计

> **文档定位**：本文档详细设计AI医生系统的主Agent架构，包括主Agent的职责、运行循环、工具调用机制、证据融合与冲突解决等。  
> **相关文档**：
> - 整体架构概述：请参考《1.系统概述/整体架构概述.md》
> - 双通道推理架构：请参考《双通道推理架构.md》
> - 工具系统架构：请参考《工具系统架构.md》
> - 主Agent运行循环设计：请参考《5.主Agent设计/主Agent运行循环设计.md》

---

## 一、主Agent架构概述

### 1.1 核心设计理念

**单主Agent架构核心理念**：

1. **主Agent自主性**：主Agent（Clinical Agent Brain）具备自主感知、推理、决策和执行能力，是唯一"最终结论提交者"
2. **工具无状态性**：工具无独立目标、无长期策略状态，只按主Agent调用执行并返回结构化结果与证据引用
3. **CDP唯一事实源**：CDP是病例事实的唯一事实源，所有工具从CDP读取，建议写回CDP
4. **证据融合与冲突解决**：主Agent内部进行evidence fusion和conflict resolution，而非自治agent协商
5. **审计可追溯**：所有工具调用、证据、写回字段、版本、时间必须记录到AuditTrail

### 1.2 主Agent职责

**主Agent（Clinical Agent Brain）**：

- **唯一决策者**：主Agent是唯一"最终结论提交者"，所有诊断结论、检查建议、治疗方案都由主Agent最终决定
- **自主调用工具**：主Agent根据当前CDP状态和AgentState，自主决定调用哪些工具、调用顺序、调用参数
- **停止/升级/拒答能力**：主Agent具备停止条件判断、升级策略执行、拒答边界判断的能力
- **证据融合**：主Agent内部融合多个工具返回的证据，进行冲突解决和一致性检查
- **策略状态管理**：主Agent维护AgentState，包括阈值、预算、失败回退、已尝试工具等

### 1.3 工具系统

**工具（Tools）**：

- **无独立目标**：工具不拥有独立的诊断目标或治疗目标，只按主Agent调用执行
- **无长期策略状态**：工具不维护长期状态，每次调用都是独立的
- **结构化输出**：工具返回结构化的payload、evidence、quality、suggestedWrites
- **证据引用**：工具必须提供evidence引用，说明输出结果的依据来源
- **建议写回字段**：工具通过suggestedWrites建议主Agent写回CDP的字段路径

**八大工具**：

| 工具ID | 工具名称 | 主要职责 |
|--------|---------|---------|
| tool_0 | 健康状态判定工具 | 判断工作态、入口判定流程 |
| tool_1 | 病例理解工具 | 概念归一化、结构化提取 |
| tool_2 | 主动问诊工具 | 信息缺口识别、问诊生成 |
| tool_3 | 鉴别诊断工具 | 多引擎融合诊断、DDx生成 |
| tool_4 | 检查建议工具 | 检查价值评估、验证计划 |
| tool_5 | 治疗建议工具 | 治疗方案推理、药物推荐 |
| tool_6 | 风险评估工具 | 高危识别、紧急程度评估 |
| tool_7 | 证据链工具 | 证据链构建、解释生成 |

---

## 二、主Agent运行循环

### 2.1 运行循环概述

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

2. Plan（规划）
   - 决定调用哪个工具
   - 决定调用顺序
   - 决定调用参数
   - 评估预算和约束

3. Act（执行）
   - 生成ToolContext
   - 调用工具
   - 等待工具返回结果

4. Update（更新）
   - 评估ToolResult
   - 决定是否写回CDP
   - 更新CDP（创建新版本）
   - 更新AgentState
   - 记录到AuditTrail

5. Evaluate（评估）
   - 评估停止条件
   - 评估升级条件
   - 评估拒答条件
   - 决定下一步行动

6. Stop/Escalate/Continue（停止/升级/继续）
   - Stop：满足停止条件，返回最终结果
   - Escalate：满足升级条件，升级处理
   - Refuse：满足拒答条件，拒绝回答
   - Continue：继续运行循环
```

> **详细设计**：请参考《5.主Agent设计/主Agent运行循环设计.md》

---

## 三、工具调用机制

### 3.1 工具调用流程

```
主Agent决定调用工具
    ↓
生成ToolContext（包含CDP引用、AgentState摘要、约束等）
    ↓
调用工具（通过REST API或内部调用）
    ↓
工具执行（从CDP读取数据，执行逻辑，生成结果）
    ↓
工具返回ToolResult（包含payload、evidence、quality、suggestedWrites）
    ↓
主Agent评估ToolResult
    ↓
决定是否写回CDP
```

### 3.2 ToolContext（工具调用上下文）

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

> **详细设计**：请参考《7.接口规范/工具调用协议.md》

### 3.3 ToolResult（工具返回结果）

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
| `tool_result.quality` | JSON | 质量指标 |
| `tool_result.suggested_writes` | Array | 建议写回CDP的字段路径 |
| `tool_result.errors` | Array | 错误信息 |
| `tool_result.duration_ms` | Integer | 执行时间（毫秒） |

> **详细设计**：请参考《7.接口规范/工具调用协议.md》

---

## 四、证据融合与冲突解决

### 4.1 证据融合机制

**证据融合定位**：
- **主Agent内部融合**：主Agent内部融合多个工具返回的证据，形成统一的证据视图
- **非自治agent协商**：不是多个自治agent协商，而是主Agent统一决策
- **可追溯**：所有证据融合过程记录到AuditTrail

**证据融合流程**：

```
工具1返回证据
    ↓
工具2返回证据
    ↓
主Agent证据融合
    ├─ 合并支持证据
    ├─ 合并反对证据
    ├─ 合并缺失证据
    └─ 识别证据冲突
    ↓
冲突解决
    ├─ 评估证据强度
    ├─ 评估证据来源可信度
    └─ 选择最优证据
    ↓
更新CDP（写入融合后的证据）
```

### 4.2 冲突解决策略

**冲突类型**：
- **诊断冲突**：不同工具返回不同的诊断结果
- **证据冲突**：不同工具返回相互矛盾的证据
- **优先级冲突**：不同工具对同一诊断的优先级不同

**冲突解决策略**：
- **证据强度优先**：优先选择证据强度高的证据
- **来源可信度优先**：优先选择来源可信度高的证据
- **工具类型优先**：Deterministic > Retrieval > Generative
- **时间优先**：优先选择最新的证据

---

## 五、停止/升级/拒答机制

### 5.1 停止条件

**停止条件判断**：
- **CDP必填项完成**：CDP中所有必填字段都已填写
- **证据引用齐全**：所有诊断结论都有完整的证据引用
- **风险评估完成**：风险评估已完成
- **达到预算上限**：工具调用次数或时间达到预算上限

### 5.2 升级条件

**升级条件判断**：
- **高风险识别**：识别到高风险情况（L1/L2）
- **红旗信号**：识别到危险信号
- **证据不足**：证据不足以做出诊断结论
- **工具失败**：关键工具执行失败

### 5.3 拒答条件

**拒答条件判断**：
- **超出能力范围**：超出AI医生的能力范围
- **信息不足**：信息不足以做出任何判断
- **安全风险**：存在安全风险，不应给出建议

---

## 六、AgentState管理

### 6.1 AgentState定位

**AgentState定位**：
- **主Agent策略状态**：AgentState存储主Agent的策略状态，包括阈值、预算、失败回退、已尝试工具等
- **工具不可见**：工具不能直接访问AgentState，只能通过ToolContext获取AgentState摘要
- **主Agent独占写入**：只有主Agent可以写入AgentState

### 6.2 AgentState字段结构

| 字段路径 | 数据类型 | 说明 |
|---------|---------|------|
| `agent_state.session_id` | String | 会话ID（与CDP关联） |
| `agent_state.current_step` | Integer | 当前诊断步骤（1-5） |
| `agent_state.work_mode` | String | 工作态（wellness_mode/clinical_mode） |
| `agent_state.thresholds` | JSON | 阈值配置 |
| `agent_state.budget` | JSON | 预算配置 |
| `agent_state.failure_backoff` | JSON | 失败回退策略 |
| `agent_state.tried_tools` | Array | 已尝试工具列表 |
| `agent_state.evidence_fusion_state` | JSON | 证据融合状态 |
| `agent_state.stop_conditions` | JSON | 停止条件状态 |

> **详细设计**：请参考《6.数据模型设计/AgentState数据结构设计.md》

---

## 七、参考文档

### 7.1 架构设计文档
- 《1.系统概述/整体架构概述.md》：整体架构概述
- 《双通道推理架构.md》：双通道推理架构详细设计
- 《工具系统架构.md》：工具系统架构详细设计

### 7.2 主Agent设计文档
- 《5.主Agent设计/主Agent运行循环设计.md》：主Agent运行循环详细设计
- 《5.主Agent设计/主Agent业务逻辑设计.md》：主Agent业务逻辑详细设计
- 《5.主Agent设计/主Agent技术实现.md》：主Agent技术实现详细设计

### 7.3 数据模型设计文档
- 《6.数据模型设计/CDP数据结构设计.md》：CDP数据结构详细设计
- 《6.数据模型设计/AgentState数据结构设计.md》：AgentState数据结构详细设计
- 《6.数据模型设计/AuditTrail数据结构设计.md》：AuditTrail数据结构详细设计

### 7.4 接口规范文档
- 《7.接口规范/工具调用协议.md》：工具调用协议详细设计

---

**文档来源**：
- 原文档：《功能与技术设计/AI医生系统-技术架构设计-核心架构.md》1.2节、二、核心对象与责任边界
- 创建时间：2025-01-22
- 文档版本：v1.0

