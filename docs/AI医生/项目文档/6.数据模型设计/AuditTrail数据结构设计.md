# AI医生系统 - AuditTrail数据结构设计

> **文档定位**：本文档详细设计AI医生系统的AuditTrail数据结构，包括AuditTrail数据模型、事件类型、字段详细说明和查询接口。  
> **相关文档**：
> - 主Agent架构设计：请参考《2.架构设计/主Agent架构设计.md》
> - CDP数据结构设计：请参考《CDP数据结构设计.md》
> - AgentState数据结构设计：请参考《AgentState数据结构设计.md》

---

## 一、AuditTrail概述

### 1.1 AuditTrail定位

**AuditTrail定位**：
- **必须记录**：所有工具调用、证据、写回字段、版本、时间必须记录到AuditTrail
- **不可修改**：AuditTrail采用追加写入模式，历史记录不可修改
- **可追溯**：支持审计记录的查询和追溯，用于问题定位和系统优化

### 1.2 AuditTrail核心原则

**AuditTrail核心原则**：
1. **追加写入**：AuditTrail采用追加写入模式，不修改历史记录
2. **实时持久化**：AuditTrail实时持久化到MySQL/Oracle
3. **完整记录**：记录所有工具调用、CDP更新、主Agent决策的完整信息

---

## 二、AuditTrail数据模型

### 2.1 数据库设计

**数据库设计**（Oracle/MySQL）：

```sql
-- AuditTrail主表
CREATE TABLE audit_trail (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64),
    session_id VARCHAR(64),
    timestamp TIMESTAMP,
    event_type VARCHAR(32),  -- tool_call/cdp_update/agent_decision
    tool_call JSON,  -- 工具调用记录（如event_type=tool_call）
    cdp_update JSON,  -- CDP更新记录（如event_type=cdp_update）
    agent_decision JSON,  -- 主Agent决策记录（如event_type=agent_decision）
    created_at TIMESTAMP,
    INDEX idx_cdp_id (cdp_id),
    INDEX idx_session_id (session_id),
    INDEX idx_event_type (event_type),
    INDEX idx_timestamp (timestamp)
);
```

### 2.2 事件类型

**事件类型**：
- **tool_call**：工具调用事件
- **cdp_update**：CDP更新事件
- **agent_decision**：主Agent决策事件

---

## 三、AuditTrail字段详细说明

### 3.1 tool_call 字段（event_type=tool_call时）

**用途**：存储工具调用记录

**数据结构**：
```json
{
  "tool_id": "tool_3",
  "tool_name": "鉴别诊断工具",
  "trace_id": "trace_123456",
  "input": {
    "cdp_reference": {
      "cdp_id": "cdp_001",
      "version": 1,
      "read_fields": ["cdp.patient_state", "cdp.ddx"]
    },
    "agent_state_summary": {
      "current_step": 2,
      "work_mode": "clinical_mode"
    },
    "constraints": {
      "max_time_seconds": 30,
      "max_cost": 10.0
    }
  },
  "output": {
    "status": "success",
    "payload_summary": "DDx候选集（5个诊断）",
    "evidence_count": 3
  },
  "evidence": [
    {
      "source": "knowledge_base",
      "reference": "chief_complaint_kg_entry_001",
      "strength": "strong"
    }
  ],
  "suggested_writes": [
    {
      "field_path": "cdp.ddx",
      "reason": "更新鉴别诊断列表"
    }
  ],
  "quality": {
    "confidence": 0.85,
    "completeness": 0.90
  },
  "errors": [],
  "duration_ms": 2500
}
```

**字段说明**：
- `tool_id`：工具ID
- `tool_name`：工具名称
- `trace_id`：追踪ID（用于关联ToolContext和ToolResult）
- `input`：工具输入（CDP引用、AgentState摘要、约束）
- `output`：工具输出（状态、payload摘要、证据数量）
- `evidence`：证据引用列表
- `suggested_writes`：建议写回CDP的字段路径
- `quality`：质量指标
- `errors`：错误信息列表
- `duration_ms`：执行时间（毫秒）

### 3.2 cdp_update 字段（event_type=cdp_update时）

**用途**：存储CDP更新记录

**数据结构**：
```json
{
  "from_version": 1,
  "to_version": 2,
  "changed_fields": [
    "cdp.ddx",
    "cdp.evidence_graph"
  ],
  "reason": "tool_3返回的DDx结果写回CDP",
  "updated_by": "agent_main",
  "tool_trace_id": "trace_123456"
}
```

**字段说明**：
- `from_version`：源版本号
- `to_version`：目标版本号
- `changed_fields`：变更字段路径列表
- `reason`：更新原因
- `updated_by`：更新者（agent_main）
- `tool_trace_id`：关联的工具调用追踪ID

### 3.3 agent_decision 字段（event_type=agent_decision时）

**用途**：存储主Agent决策记录

**数据结构**：
```json
{
  "decision_type": "stop",
  "reason": "所有停止条件满足",
  "evidence_fusion": {
    "fused_evidence_count": 5,
    "conflicts_resolved": 1
  },
  "conflict_resolution": {
    "conflict_id": "conflict_1",
    "resolution_strategy": "evidence_strength",
    "resolved_value": 0.5
  }
}
```

**字段说明**：
- `decision_type`：决策类型（stop/escalate/refuse/continue）
- `reason`：决策原因
- `evidence_fusion`：证据融合结果
- `conflict_resolution`：冲突解决结果

---

## 四、AuditTrail记录规则

### 4.1 记录规则

**AuditTrail记录规则**：
- 每次工具调用都记录一条`tool_call`事件
- 每次CDP更新都记录一条`cdp_update`事件
- 每次主Agent决策都记录一条`agent_decision`事件
- 所有记录都包含完整的时间戳和追踪ID

### 4.2 记录流程

```
工具调用开始
    ↓
记录tool_call事件（开始）
    ↓
工具调用完成
    ↓
记录tool_call事件（完成，包含输出）
    ↓
主Agent决定写回CDP
    ↓
记录cdp_update事件
    ↓
主Agent决策
    ↓
记录agent_decision事件
    ↓
实时持久化到MySQL/Oracle
```

---

## 五、AuditTrail查询接口

### 5.1 查询接口

**AuditTrail查询接口**：

```python
# AuditTrail查询API
GET    /api/v1/audit_trail/{cdp_id}              # 获取CDP的审计记录
GET    /api/v1/audit_trail/tool/{tool_id}        # 获取工具的调用记录
GET    /api/v1/audit_trail/event/{event_type}    # 按事件类型查询
GET    /api/v1/audit_trail/session/{session_id}  # 获取会话的审计记录
GET    /api/v1/audit_trail/time_range            # 按时间范围查询
```

### 5.2 查询示例

**查询CDP的审计记录**：
```python
GET /api/v1/audit_trail/cdp_001

Response:
{
  "cdp_id": "cdp_001",
  "audit_records": [
    {
      "id": "audit_001",
      "timestamp": "2025-01-01T10:00:00Z",
      "event_type": "tool_call",
      "tool_call": {...}
    },
    {
      "id": "audit_002",
      "timestamp": "2025-01-01T10:00:05Z",
      "event_type": "cdp_update",
      "cdp_update": {...}
    }
  ]
}
```

**查询工具的调用记录**：
```python
GET /api/v1/audit_trail/tool/tool_3

Response:
{
  "tool_id": "tool_3",
  "call_records": [
    {
      "id": "audit_001",
      "timestamp": "2025-01-01T10:00:00Z",
      "cdp_id": "cdp_001",
      "tool_call": {...}
    }
  ]
}
```

---

## 六、参考文档

### 6.1 数据模型设计文档
- 《CDP数据结构设计.md》：CDP数据结构详细设计
- 《AgentState数据结构设计.md》：AgentState数据结构详细设计

### 6.2 架构设计文档
- 《2.架构设计/主Agent架构设计.md》：主Agent架构详细设计

---

**文档来源**：
- 原文档：《功能与技术设计/AI医生系统-技术架构设计-CDP数据与状态管理.md》6.3节
- 原文档：《功能与技术设计/AI医生系统-技术架构设计-核心架构.md》2.4节
- 创建时间：2025-01-22
- 文档版本：v1.0

