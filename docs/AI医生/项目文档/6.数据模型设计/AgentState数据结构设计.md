# AI医生系统 - AgentState数据结构设计

> **文档定位**：本文档详细设计AI医生系统的AgentState数据结构，包括AgentState数据模型、字段详细说明、更新规则和持久化策略。  
> **相关文档**：
> - 主Agent架构设计：请参考《2.架构设计/主Agent架构设计.md》
> - CDP数据结构设计：请参考《CDP数据结构设计.md》
> - AuditTrail数据结构设计：请参考《AuditTrail数据结构设计.md》

---

## 一、AgentState概述

### 1.1 AgentState定位

**AgentState定位**：
- **主Agent策略状态**：AgentState存储主Agent的策略状态，包括阈值、预算、失败回退、已尝试工具等
- **工具不可见**：工具不能直接访问AgentState，只能通过ToolContext获取AgentState摘要
- **主Agent独占写入**：只有主Agent可以写入AgentState

### 1.2 AgentState核心原则

**AgentState核心原则**：
1. **主Agent独占写入**：只有主Agent可以写入AgentState
2. **工具只读摘要**：工具只能通过ToolContext获取AgentState摘要
3. **状态同步**：AgentState存储在Redis（快速访问），定期持久化到MySQL/Oracle（长期存储）

---

## 二、AgentState数据模型

### 2.1 数据库设计

**数据库设计**（Oracle/MySQL）：

```sql
-- AgentState主表
CREATE TABLE agent_state (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64),
    cdp_id VARCHAR(64),
    current_step INT,  -- 1-5
    work_mode VARCHAR(32),  -- wellness_mode/clinical_mode
    thresholds JSON,  -- 阈值配置
    budget JSON,  -- 预算配置
    failure_backoff JSON,  -- 失败回退策略
    tried_tools JSON,  -- 已尝试工具列表
    evidence_fusion_state JSON,  -- 证据融合状态
    stop_conditions JSON,  -- 停止条件状态
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_cdp_id (cdp_id)
);
```

### 2.2 AgentState字段结构

**AgentState字段结构**：

| 字段路径 | 数据类型 | 说明 |
|---------|---------|------|
| `agent_state.session_id` | String | 会话ID（与CDP关联） |
| `agent_state.cdp_id` | String | CDP ID |
| `agent_state.current_step` | Integer | 当前诊断步骤（1-5） |
| `agent_state.work_mode` | String | 工作态（wellness_mode/clinical_mode） |
| `agent_state.thresholds` | JSON | 阈值配置 |
| `agent_state.thresholds.confidence_threshold` | Float | 置信度阈值（默认0.7） |
| `agent_state.thresholds.evidence_count_threshold` | Integer | 证据数量阈值（默认3） |
| `agent_state.thresholds.information_gain_threshold` | Float | 信息增益阈值（默认0.5） |
| `agent_state.budget` | JSON | 预算配置 |
| `agent_state.budget.max_tool_calls` | Integer | 最大工具调用次数（默认50） |
| `agent_state.budget.max_time_seconds` | Integer | 最大执行时间（秒，默认300） |
| `agent_state.budget.max_cost` | Float | 最大成本（默认100.0） |
| `agent_state.budget.current_tool_calls` | Integer | 当前工具调用次数 |
| `agent_state.budget.current_time_seconds` | Integer | 当前执行时间（秒） |
| `agent_state.budget.current_cost` | Float | 当前成本 |
| `agent_state.failure_backoff` | JSON | 失败回退策略 |
| `agent_state.failure_backoff.max_retries` | Integer | 最大重试次数（默认3） |
| `agent_state.failure_backoff.backoff_strategy` | String | 回退策略（exponential/linear） |
| `agent_state.tried_tools` | Array | 已尝试工具列表 |
| `agent_state.tried_tools[].tool_id` | String | 工具ID |
| `agent_state.tried_tools[].call_count` | Integer | 调用次数 |
| `agent_state.tried_tools[].last_result` | String | 上次结果（success/failure/timeout） |
| `agent_state.tried_tools[].last_call_time` | Timestamp | 上次调用时间 |
| `agent_state.evidence_fusion_state` | JSON | 证据融合状态 |
| `agent_state.evidence_fusion_state.conflicts` | Array | 证据冲突列表 |
| `agent_state.evidence_fusion_state.resolution_strategy` | String | 冲突解决策略 |
| `agent_state.stop_conditions` | JSON | 停止条件状态 |
| `agent_state.stop_conditions.cdp_required_fields_complete` | Boolean | CDP必填项是否完成 |
| `agent_state.stop_conditions.evidence_references_complete` | Boolean | 证据引用是否齐全 |
| `agent_state.stop_conditions.risk_assessment_complete` | Boolean | 风险评估是否完成 |

---

## 三、AgentState字段详细说明

### 3.1 thresholds 字段

**用途**：存储主Agent的阈值配置

**数据结构**：
```json
{
  "confidence_threshold": 0.7,
  "evidence_count_threshold": 3,
  "information_gain_threshold": 0.5
}
```

**说明**：
- `confidence_threshold`：置信度阈值，用于判断诊断结论的置信度是否足够
- `evidence_count_threshold`：证据数量阈值，用于判断是否有足够的证据支持诊断
- `information_gain_threshold`：信息增益阈值，用于判断问诊问题的价值

### 3.2 budget 字段

**用途**：存储主Agent的预算配置

**数据结构**：
```json
{
  "max_tool_calls": 50,
  "max_time_seconds": 300,
  "max_cost": 100.0,
  "current_tool_calls": 10,
  "current_time_seconds": 60,
  "current_cost": 20.0
}
```

**说明**：
- `max_tool_calls`：最大工具调用次数，防止无限循环
- `max_time_seconds`：最大执行时间（秒），防止超时
- `max_cost`：最大成本，控制LLM调用成本
- `current_*`：当前已使用的资源

### 3.3 tried_tools 字段

**用途**：存储已尝试工具列表

**数据结构**：
```json
[
  {
    "tool_id": "tool_3",
    "call_count": 3,
    "last_result": "success",
    "last_call_time": "2025-01-01T10:00:00Z"
  }
]
```

**说明**：
- `tool_id`：工具ID
- `call_count`：调用次数
- `last_result`：上次结果（success/failure/timeout）
- `last_call_time`：上次调用时间

### 3.4 evidence_fusion_state 字段

**用途**：存储证据融合状态

**数据结构**：
```json
{
  "conflicts": [
    {
      "conflict_id": "conflict_1",
      "conflict_type": "diagnosis_probability",
      "conflicting_evidence": [
        {"tool_id": "tool_3", "value": 0.8},
        {"tool_id": "tool_4", "value": 0.3}
      ],
      "resolution_strategy": "evidence_strength",
      "resolved_value": 0.5
    }
  ],
  "resolution_strategy": "evidence_strength"
}
```

**说明**：
- `conflicts`：证据冲突列表
- `conflict_type`：冲突类型（diagnosis_probability/evidence_direction等）
- `conflicting_evidence`：冲突的证据列表
- `resolution_strategy`：冲突解决策略
- `resolved_value`：解决后的值

### 3.5 stop_conditions 字段

**用途**：存储停止条件状态

**数据结构**：
```json
{
  "cdp_required_fields_complete": true,
  "evidence_references_complete": true,
  "risk_assessment_complete": true,
  "diagnosis_conclusion_clear": true,
  "workup_plan_complete": true,
  "management_plan_complete": true,
  "evidence_chain_complete": true,
  "final_conclusion_generated": true
}
```

**说明**：
- 每个字段表示一个停止条件的满足状态
- 所有条件都满足时，主Agent可以停止运行循环

---

## 四、AgentState更新规则

### 4.1 更新时机

**AgentState更新时机**：
- 主Agent在每次工具调用后更新AgentState
- 更新`tried_tools`、`current_step`、`evidence_fusion_state`等字段
- 更新操作记录到AuditTrail

### 4.2 更新流程

```
工具调用完成
    ↓
更新tried_tools（增加调用次数）
    ↓
更新current_step（根据工具结果）
    ↓
更新evidence_fusion_state（如果有证据冲突）
    ↓
更新stop_conditions（检查停止条件）
    ↓
更新budget（更新当前资源使用）
    ↓
持久化到Redis和MySQL/Oracle
    ↓
记录到AuditTrail
```

### 4.3 AgentState摘要（提供给工具）

**AgentState摘要**：
工具通过ToolContext获取AgentState摘要，包括：
- `current_step`：当前诊断步骤
- `work_mode`：工作态
- `constraints`：约束（成本/时间/风险）

**AgentState摘要结构**：
```json
{
  "current_step": 2,
  "work_mode": "clinical_mode",
  "constraints": {
    "max_time_seconds": 30,
    "max_cost": 10.0,
    "risk_level_limit": "L3"
  }
}
```

---

## 五、AgentState持久化策略

### 5.1 存储策略

**存储策略**：
- **Redis（快速访问）**：AgentState存储在Redis中，用于快速访问
- **MySQL/Oracle（长期存储）**：定期持久化到MySQL/Oracle，用于长期存储和查询

### 5.2 同步策略

**同步策略**：
- **实时写入Redis**：每次更新都实时写入Redis
- **定期持久化**：每5分钟或每次重要更新时持久化到MySQL/Oracle
- **故障恢复**：从MySQL/Oracle恢复AgentState到Redis

---

## 六、参考文档

### 6.1 数据模型设计文档
- 《CDP数据结构设计.md》：CDP数据结构详细设计
- 《AuditTrail数据结构设计.md》：AuditTrail数据结构详细设计

### 6.2 架构设计文档
- 《2.架构设计/主Agent架构设计.md》：主Agent架构详细设计

---

**文档来源**：
- 原文档：《功能与技术设计/AI医生系统-技术架构设计-CDP数据与状态管理.md》6.2节
- 原文档：《功能与技术设计/AI医生系统-技术架构设计-核心架构.md》2.2节
- 创建时间：2025-01-22
- 文档版本：v1.0

