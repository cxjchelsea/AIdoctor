# risk-assessment-service - 服务实现方案

> **文档定位**：本文档定义risk-assessment-service（风险评估服务）的技术实现方案，包括技术选型、接口设计、数据流、实现步骤等。  
> **参考文档**：
> - 《AI医生系统-业务逻辑详细设计.md》- 业务逻辑（核心参考）
> - 《AI医生系统-技术架构设计.md》及相关子文档- 技术架构
> - 《AI医生系统-项目结构设计.md》- 项目结构
> - 《AI医生系统-数据模型设计.md》- 数据模型
> - 《AI医生系统-API接口规范.md》- API规范
> - 《AI医生系统-错误处理规范.md》- 错误处理规范

---

## 一、服务概述

> **参考文档**：《AI医生系统-业务逻辑详细设计.md》第1.7节

### 1.1 服务定位

- **对应工具**：tool_6（风险评估工具）
- **在架构中的位置**：诊断流程中的风险评估环节，位于诊断引擎服务之后，贯穿整个诊断流程
- **服务职责**：
  1. 风险评估（识别高危情况，评估紧急程度，决定是否需要立即升级处理）
  2. 分诊评估（评估患者的紧急程度和分诊级别）
  3. 升级规则（根据诊断结果和患者状态，确定复评与升级规则）
  4. 终点结论包构建（构建终点结论包，包含结论、必须排除项状态、关键依据、行动与随访）

### 1.2 输入输出

#### 1.2.1 输入数据格式和来源

**输入来源**：diagnosis-service（流程编排服务）调用

**输入数据格式**（参考《AI医生系统-数据模型设计.md》）：
```json
{
  "cdpId": "cdp-123456",
  "cdp": {
    "ddx": [
      {
        "disease": "急性心肌梗死",
        "probability": 0.75,
        "layer": "primary_hypothesis"
      },
      {
        "disease": "主动脉夹层",
        "probability": 0.05,
        "layer": "must_exclude"
      }
    ],
    "patient_state": {
      "symptoms": ["胸痛", "气短"],
      "vital_signs": {
        "bp": {"systolic": 150, "diastolic": 95},
        "heart_rate": 100
      }
    },
    "three_layer_result": {
      "primary_hypothesis": {
        "disease": "急性心肌梗死",
        "probability": 0.75
      },
      "must_exclude": [
        {
          "disease": "主动脉夹层",
          "reason": "高危诊断，必须排除"
        }
      ]
    }
  }
}
```

**字段说明**：
- `cdpId`：CDP ID（必填）
- `cdp`：临床决策包（可选，如果提供则从CDP中读取数据）
- `ddx`：鉴别诊断候选集（从CDP读取）
- `patient_state`：患者状态（从CDP读取）
- `three_layer_result`：三层分层结果（从CDP读取）

#### 1.2.2 输出数据格式和目标

**输出目标**：diagnosis-service（流程编排服务）、前端

**输出数据格式**（参考《AI医生系统-最终输出格式规范.md》）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp-123456",
    "riskLevel": "L2",
    "severity": "moderate",
    "urgency": "urgent",
    "redFlags": [
      {
        "type": "symptom_combination",
        "description": "胸痛、气短、心率快",
        "risk": "high"
      }
    ],
    "reviewPlan": {
      "reviewTimeWindow": "24小时内",
      "earlyReviewConditions": [
        "症状加重",
        "出现新症状"
      ],
      "upgradeConditions": [
        "生命体征恶化",
        "出现危险信号"
      ]
    },
    "triageLevel": "urgent",
    "riskFactors": [
      "胸痛",
      "心率快",
      "血压高"
    ],
    "recommendations": [
      "建议立即就医",
      "监测生命体征",
      "24小时内复评"
    ]
  },
  "timestamp": 1705123456789
}
```

**字段说明**：
- `riskLevel`：风险等级（L1-L5，L1最紧急）
- `severity`：严重程度（mild/moderate/severe/critical）
- `urgency`：紧急程度（emergency/urgent/routine）
- `redFlags`：危险信号列表
- `reviewPlan`：复评计划
  - `reviewTimeWindow`：复评时间窗口
  - `earlyReviewConditions`：提前复评条件
  - `upgradeConditions`：升级条件
- `triageLevel`：分诊级别
- `riskFactors`：风险因素列表
- `recommendations`：风险处理建议

#### 1.2.3 数据流转关系

**数据流转**（参考《AI医生系统-技术架构设计-CDP数据与状态管理.md》）：
```
diagnosis-service → risk-assessment-service → 返回风险评估结果
                                      ↓
                              更新CDP的triage字段
```

**CDP数据更新**：
- 更新CDP的`triage`字段，包含风险评估结果、分诊评估结果、升级规则等

### 1.3 业务价值

- **安全兜底**：识别高危情况，确保患者安全
- **分诊指导**：评估紧急程度，指导分诊决策
- **动态监控**：通过复评与升级规则，实现动态风险监控
- **决策支持**：为诊断和治疗决策提供风险依据

---

## 二、技术实现设计

> **参考文档**：
> - 《AI医生系统-技术架构设计.md》及相关子文档
> - 《AI医生系统-技术架构设计-核心技术组件.md》
> - 《AI医生系统-技术架构设计-性能与评估.md》

### 2.1 技术栈选择

#### 2.1.1 编程语言和框架

- **编程语言**：Python 3.10+
- **Web框架**：FastAPI 0.104+
- **数据验证**：Pydantic 2.0+

**技术选型理由**：
- Python生态丰富，便于快速开发
- FastAPI性能优秀，支持异步，自动生成API文档
- Pydantic提供强大的数据验证和序列化能力

#### 2.1.2 核心依赖库

```python
# Web框架
fastapi==0.104.1
uvicorn[standard]==0.24.0

# 数据验证
pydantic==2.5.0

# 工具库
python-dotenv==1.0.0  # 环境变量管理

# 可选：知识库集成
# 高危识别规则库
# 严重程度评估规则库
# 紧急程度分级规则库
# 复评与升级规则库
```

**技术选型理由**：
- FastAPI：现代、高性能的Python Web框架
- Pydantic：类型安全的数据验证

### 2.2 核心算法/方法

#### 2.2.1 风险评估算法

**算法描述**：识别高危情况，评估紧急程度，决定是否需要立即升级处理。

**实现思路**：

1. **高危识别**（使用高危识别规则库：6.1）
   - 识别高危症状组合
   - 识别高危诊断
   - 识别生命体征异常

2. **严重程度评估**（使用严重程度评估规则库：6.2）
   - 评估症状严重程度
   - 评估诊断严重程度
   - 综合评估患者严重程度

3. **紧急程度分级**（使用紧急程度分级规则库：6.3）
   - 根据严重程度和风险等级分级
   - 确定紧急程度（emergency/urgent/routine）

4. **复评与升级规则**（使用复评与升级规则库：6.4）
   - 确定复评时间窗口
   - 确定提前复评条件
   - 确定升级条件

**算法复杂度**：O(n)，n为规则数

#### 2.2.2 分诊评估算法

**算法描述**：评估患者的紧急程度和分诊级别。

**实现思路**：

1. **风险等级计算**
   - 基于风险评估结果
   - 计算风险等级（L1-L5）

2. **分诊级别确定**
   - 根据风险等级和紧急程度
   - 确定分诊级别

3. **风险因素识别**
   - 识别主要风险因素
   - 生成风险因素列表

**算法复杂度**：O(1)

#### 2.2.3 升级规则算法

**算法描述**：根据诊断结果和患者状态，确定复评与升级规则。

**实现思路**：

1. **复评时间窗口确定**
   - 根据风险等级确定复评时间窗口
   - 高风险：短时间窗口（如24小时内）
   - 低风险：长时间窗口（如1周内）

2. **提前复评条件确定**
   - 识别需要提前复评的条件
   - 症状加重、出现新症状等

3. **升级条件确定**
   - 识别需要升级的条件
   - 生命体征恶化、出现危险信号等

**算法复杂度**：O(n)，n为规则数

#### 2.2.4 终点结论包构建算法

**算法描述**：构建终点结论包，包含结论、必须排除项状态、关键依据、行动与随访。

**实现思路**：

1. **结论构建**
   - 基于三层分层结果
   - 生成诊断结论

2. **必须排除项状态**
   - 检查必须排除的诊断状态
   - 是否已排除或仍需排除

3. **关键依据提取**
   - 从证据分析中提取关键依据
   - 生成关键依据列表

4. **行动与随访**
   - 生成行动建议
   - 生成随访计划

**算法复杂度**：O(n)，n为诊断候选数

### 2.3 性能要求

> **参考文档**：《AI医生系统-技术架构设计-性能与评估.md》

#### 2.3.1 响应时间要求

- **风险评估**：< 1秒
- **分诊评估**：< 1秒
- **目标**：< 500ms（优化后）

#### 2.3.2 并发处理能力

- **支持并发请求**：100+ 并发
- **目标**：500+ 并发（优化后）

#### 2.3.3 资源消耗限制

- **内存消耗**：< 512MB（单实例）
- **CPU消耗**：< 50%（正常负载）

### 2.4 关键技术点

#### 2.4.1 关键技术难点

1. **高危识别**
   - 难点：准确识别高危情况，避免漏检
   - 解决方案：
     - 维护完整的高危识别规则库
     - 使用规则引擎匹配
     - 定期更新规则库
     - 宁可误报不能漏报

2. **风险等级计算**
   - 难点：准确计算风险等级，考虑多种因素
   - 解决方案：
     - 使用多因素综合评估
     - 使用规则引擎计算
     - 结合专家经验

3. **复评与升级规则**
   - 难点：制定合理的复评和升级规则
   - 解决方案：
     - 维护复评与升级规则库
     - 根据风险等级动态调整
     - 考虑临床实践

#### 2.4.2 技术风险及应对

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| 高危识别漏检 | 高 | 完善高危识别规则库，定期更新，宁可误报不能漏报 |
| 风险等级计算不准确 | 中 | 使用多种方法验证，结合专家经验 |
| 性能瓶颈 | 中 | 使用缓存，优化算法，异步处理 |
| 复评规则不合理 | 中 | 完善复评规则库，增加测试用例 |

---

## 三、接口设计

> **参考文档**：
> - 《AI医生系统-API接口规范.md》
> - 《AI医生系统-错误处理规范.md》
> - 《AI医生系统-数据模型设计.md》

### 3.1 API端点定义

#### 3.1.1 统一工具调用接口（新增）

**接口路径**：`POST /api/v1/tools/tool_6/invoke`

**接口描述**：统一的工具调用接口，符合《工具调用协议.md》规范。主Agent通过此接口调用工具。

**请求方法**：POST

**请求头**：
```
Content-Type: application/json
```

**请求体**（ToolContext格式）：
```json
{
  "trace_id": "string",
  "cdp_reference": {
    "cdp_id": "string",
    "version": 0,
    "read_fields": ["cdp.patient_state", "cdp.ddx"]
  },
  "agent_state_summary": {
    "current_step": 0,
    "work_mode": "string"
  },
  "constraints": {
    "max_time_seconds": 0,
    "max_cost": 0.0,
    "risk_level_limit": "string"
  },
  "call_params": {}
}
```

**响应体**（ToolResult格式）：
```json
{
  "trace_id": "string",
  "tool_id": "tool_6",
  "status": "success",
  "payload": {},
  "evidence": [
    {
      "source": "rule",
      "reference": "risk_assessment_rule",
      "strength": "strong",
      "evidence_name": "风险评估规则"
    }
  ],
  "quality": {
    "confidence": 0.85,
    "completeness": 0.80,
    "accuracy": 0.82
  },
  "suggested_writes": [
    {
      "field_path": "cdp.triage",
      "value": {},
      "reason": "更新风险评估结果"
    }
  ],
  "errors": [],
  "duration_ms": 0,
  "metadata": {}
}
```

**实现方式**：
1. 从ToolContext中提取CDP引用信息
2. 通过HTTP调用diagnosis-service的CDP查询接口获取数据
3. 根据read_fields提取指定字段的数据
4. 构建现有服务的请求格式（RiskAssessmentRequest）
5. 调用现有业务逻辑服务（RiskAssessmentEngine）
6. 将业务结果转换为ToolResult格式
7. 构建evidence引用和suggested_writes建议

**代码位置**：
- 接口实现：`app/api/routes.py` 的 `invoke_tool_6()` 函数
- 数据模型：`app/models/tool_context.py`、`app/models/tool_result.py`
- CDP读取工具：`app/utils/cdp_reader.py`

**参考文档**：
- 《7.接口规范/工具调用协议.md》- 工具调用协议详细规范

#### 3.1.2 风险评估接口（原有接口，保持向后兼容）

**接口路径**：`POST /api/v1/risk/assess`

**接口描述**：进行风险评估，识别高危情况，评估紧急程度。

**请求方法**：POST

**URL路径设计**（遵循API接口规范）：
- 使用RESTful风格
- 版本号：`/api/v1`
- 资源路径：`/risk/assess`

**请求头**：
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**：
```json
{
  "cdpId": "cdp-123456",
  "cdp": {
    "ddx": [
      {
        "disease": "急性心肌梗死",
        "probability": 0.75,
        "layer": "primary_hypothesis"
      }
    ],
    "patient_state": {
      "symptoms": ["胸痛", "气短"],
      "vital_signs": {
        "bp": {"systolic": 150, "diastolic": 95},
        "heart_rate": 100
      }
    }
  }
}
```

**响应体**（成功）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp-123456",
    "riskLevel": "L2",
    "severity": "moderate",
    "urgency": "urgent",
    "redFlags": [
      {
        "type": "symptom_combination",
        "description": "胸痛、气短、心率快",
        "risk": "high"
      }
    ],
    "reviewPlan": {
      "reviewTimeWindow": "24小时内",
      "earlyReviewConditions": [
        "症状加重",
        "出现新症状"
      ],
      "upgradeConditions": [
        "生命体征恶化",
        "出现危险信号"
      ]
    }
  },
  "timestamp": 1705123456789
}
```

#### 3.1.2 分诊评估接口

**接口路径**：`POST /api/v1/risk/triage`

**接口描述**：进行分诊评估，评估患者的紧急程度和分诊级别。

**请求方法**：POST

**请求体**：
```json
{
  "patient_state": {
    "symptoms": ["胸痛", "气短"],
    "vital_signs": {
      "bp": {"systolic": 150, "diastolic": 95},
      "heart_rate": 100
    }
  },
  "ddx": [
    {
      "disease": "急性心肌梗死",
      "probability": 0.75
    }
  ]
}
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "risk_level": "L2",
    "urgency": "urgent",
    "triage_level": "urgent",
    "red_flags": [],
    "risk_factors": [
      "胸痛",
      "心率快",
      "血压高"
    ]
  },
  "timestamp": 1705123456789
}
```

#### 3.1.3 获取升级规则接口

**接口路径**：`POST /api/v1/risk/upgrade-rules`

**接口描述**：获取复评与升级规则。

**请求方法**：POST

**请求体**：
```json
{
  "risk_level": "L2",
  "patient_state": {
    "symptoms": ["胸痛"]
  }
}
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "upgrade_conditions": [
      "生命体征恶化",
      "出现危险信号"
    ],
    "review_time_window": "24小时内",
    "early_review_conditions": [
      "症状加重",
      "出现新症状"
    ]
  },
  "timestamp": 1705123456789
}
```

#### 3.1.4 构建终点结论包接口

**接口路径**：`POST /api/v1/risk/conclusion-package`

**接口描述**：构建终点结论包。

**请求方法**：POST

**请求体**：
```json
{
  "cdpId": "cdp-123456",
  "three_layer_result": {
    "primary_hypothesis": {
      "disease": "急性心肌梗死",
      "probability": 0.75
    },
    "must_exclude": [
      {
        "disease": "主动脉夹层",
        "reason": "高危诊断，必须排除"
      }
    ]
  },
  "evidence_analysis": {
    "key_evidence": [
      {
        "type": "symptom",
        "content": "胸痛",
        "strength": "strong"
      }
    ]
  }
}
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "conclusion": {
      "primary_diagnosis": "急性心肌梗死",
      "confidence": 0.75
    },
    "must_exclude_status": {
      "aortic_dissection": {
        "status": "pending",
        "reason": "高危诊断，必须排除"
      }
    },
    "key_evidence": [
      {
        "type": "symptom",
        "content": "胸痛",
        "strength": "strong"
      }
    ],
    "action_and_follow_up": {
      "immediate_actions": [
        "建议立即就医",
        "监测生命体征"
      ],
      "follow_up_plan": {
        "review_time": "24小时内",
        "review_conditions": [
          "症状加重",
          "出现新症状"
        ]
      }
    }
  },
  "timestamp": 1705123456789
}
```

### 3.2 请求/响应模型

#### 3.2.1 请求模型定义

**参考文档**：《AI医生系统-数据模型设计.md》

```python
class RiskAssessmentRequest(BaseModel):
    """风险评估请求"""
    cdpId: str  # CDP ID（必填）
    cdp: Optional[Dict[str, Any]] = None  # 临床决策包（可选）
    patient_state: Optional[Dict[str, Any]] = None  # 患者状态（可选）
    ddx: Optional[List[Dict[str, Any]]] = None  # 鉴别诊断列表（可选）

class TriageRequest(BaseModel):
    """分诊评估请求"""
    patient_state: Dict[str, Any]  # 患者状态
    ddx: List[Dict[str, Any]]  # 鉴别诊断列表

class UpgradeRulesRequest(BaseModel):
    """升级规则请求"""
    risk_level: str  # 风险等级
    patient_state: Dict[str, Any]  # 患者状态

class ConclusionPackageRequest(BaseModel):
    """终点结论包请求"""
    cdpId: str  # CDP ID
    three_layer_result: Dict[str, Any]  # 三层分层结果
    evidence_analysis: Dict[str, Any]  # 证据分析结果
```

#### 3.2.2 响应模型定义

```python
class RedFlag(BaseModel):
    """危险信号"""
    type: str  # 类型
    description: str  # 描述
    risk: str  # 风险等级

class ReviewPlan(BaseModel):
    """复评计划"""
    reviewTimeWindow: str  # 复评时间窗口
    earlyReviewConditions: List[str]  # 提前复评条件
    upgradeConditions: List[str]  # 升级条件

class RiskAssessmentResponse(BaseModel):
    """风险评估响应"""
    cdpId: str
    riskLevel: str  # 风险等级（L1-L5）
    severity: str  # 严重程度
    urgency: str  # 紧急程度
    redFlags: List[RedFlag]  # 危险信号列表
    reviewPlan: ReviewPlan  # 复评计划

class TriageResponse(BaseModel):
    """分诊评估响应"""
    risk_level: str  # 风险等级
    urgency: str  # 紧急程度
    triage_level: str  # 分诊级别
    red_flags: List[RedFlag]  # 危险信号列表
    risk_factors: List[str]  # 风险因素列表

class ConclusionPackageResponse(BaseModel):
    """终点结论包响应"""
    conclusion: Dict[str, Any]  # 结论
    must_exclude_status: Dict[str, Any]  # 必须排除项状态
    key_evidence: List[Dict[str, Any]]  # 关键依据
    action_and_follow_up: Dict[str, Any]  # 行动与随访
```

#### 3.2.3 数据验证规则

**遵循API接口规范**：
- 使用Pydantic进行数据验证
- `cdpId`：必填，字符串，长度1-100
- `riskLevel`：字符串，取值范围：L1/L2/L3/L4/L5
- `urgency`：字符串，取值范围：emergency/urgent/routine

### 3.3 错误码定义

> **参考文档**：《AI医生系统-错误处理规范.md》

**错误码范围**：6000-6099（risk-assessment-service）

| 错误码 | 说明 | HTTP状态码 | 是否可重试 |
|--------|------|------------|-----------|
| 6001 | 风险评估失败 | 500 | false |
| 6002 | 分诊评估失败 | 500 | false |
| 6003 | 升级规则获取失败 | 500 | false |
| 6004 | 终点结论包构建失败 | 500 | false |
| 6005 | CDP ID不能为空 | 400 | false |
| 6006 | 参数验证失败 | 400 | false |

**错误处理策略**（遵循错误处理规范）：
- 统一使用全局异常处理器
- 所有错误都要记录日志
- 错误信息对用户友好，不暴露系统内部信息
- 错误必须包含traceId、cdpId等追踪信息

### 3.4 API文档

**Swagger/OpenAPI文档**：
- 自动生成：FastAPI自动生成Swagger文档
- 访问路径：`http://localhost:8092/docs`
- 文档格式：OpenAPI 3.0

**接口示例**：
- 在Pydantic模型中定义`Config.json_schema_extra`提供示例
- 在Swagger文档中展示

---

## 四、数据流设计

> **参考文档**：
> - 《AI医生系统-数据模型设计.md》
> - 《AI医生系统-技术架构设计-CDP数据与状态管理.md》
> - 《AI医生系统-最终输出格式规范.md》

### 4.1 数据输入来源

**上游服务**：diagnosis-service（流程编排服务）

**数据格式**（参考数据模型设计）：
- JSON格式
- 字段使用camelCase（对外API）

**数据获取方式**：
- HTTP POST请求
- 请求体包含CDP ID或完整的CDP数据

### 4.2 数据处理流程

**数据处理步骤**（参考业务逻辑详细设计）：

1. **接收请求**
   - 验证请求参数
   - 提取CDP ID和CDP数据

2. **获取诊断信息**
   - 从CDP中读取DDx（鉴别诊断候选集）
   - 从CDP中读取患者状态
   - 从CDP中读取三层分层结果（如需要）

3. **风险评估**
   - 高危识别（使用高危识别规则库）
   - 严重程度评估（使用严重程度评估规则库）
   - 紧急程度分级（使用紧急程度分级规则库）
   - 复评与升级规则（使用复评与升级规则库）

4. **分诊评估**
   - 风险等级计算
   - 分诊级别确定
   - 风险因素识别

5. **终点结论包构建**（如需要）
   - 结论构建
   - 必须排除项状态
   - 关键依据提取
   - 行动与随访

6. **构建响应**
   - 组装响应数据
   - 返回统一格式响应

**数据转换逻辑**：
- 诊断结果 + 患者状态 → 风险评估 → 风险等级、紧急程度
- 风险评估结果 → 分诊评估 → 分诊级别
- 三层分层结果 + 证据分析 → 终点结论包

**数据验证规则**（参考数据模型设计）：
- 使用Pydantic进行数据验证
- 验证CDP数据格式
- 验证诊断信息格式
- 验证患者状态格式

### 4.3 数据输出格式

**输出数据结构**（参考数据模型设计）：
- JSON格式
- 字段使用camelCase（对外API）

**数据格式规范**（遵循最终输出格式规范）：
- 统一响应格式：`{code, message, data, timestamp}`
- 数据字段：`{cdpId, riskLevel, severity, urgency, redFlags, reviewPlan}`

**数据存储方式**（参考CDP数据与状态管理）：
- 风险评估数据写入CDP的`triage`字段
- CDP字段使用snake_case（内部存储）

### 4.4 CDP数据流转

**CDP字段更新**（参考CDP数据与状态管理）：

1. **更新CDP的`triage`字段**：
   ```json
   {
     "risk_level": "L2",
     "severity": "moderate",
     "urgency": "urgent",
     "red_flags": [
       {
         "type": "symptom_combination",
         "description": "胸痛、气短、心率快",
         "risk": "high"
       }
     ],
     "review_plan": {
       "review_time_window": "24小时内",
       "early_review_conditions": [
         "症状加重",
         "出现新症状"
       ],
       "upgrade_conditions": [
         "生命体征恶化",
         "出现危险信号"
       ]
     },
     "triage_level": "urgent",
     "risk_factors": [
       "胸痛",
       "心率快",
       "血压高"
     ]
   }
   ```

**CDP版本控制**（参考CDP数据与状态管理）：
- 使用乐观锁机制（version字段）
- 每次更新创建新版本
- 版本号自增

**CDP状态流转**（参考CDP数据与状态管理）：
- 风险评估完成：`RISK_ASSESSED`

---

## 五、依赖关系

### 5.1 依赖的其他服务

#### 5.1.1 上游服务

**diagnosis-service**（流程编排服务）：
- **依赖关系**：被diagnosis-service调用
- **调用方式**：HTTP同步调用
- **说明**：diagnosis-service在诊断流程中调用本服务进行风险评估

#### 5.1.2 下游服务

**无**：本服务不调用其他服务

### 5.2 依赖的外部资源

#### 5.2.1 数据库

**无**：本服务不直接访问数据库，数据通过CDP流转

#### 5.2.2 缓存

**Redis**（可选）：
- **用途**：缓存风险评估结果、规则库等
- **说明**：可选，用于性能优化

#### 5.2.3 外部API

**知识库API**（可选）：
- **用途**：获取高危识别规则、严重程度评估规则等
- **说明**：可选，初期使用本地规则库，后期可集成外部知识库

### 5.3 依赖管理

#### 5.3.1 依赖版本管理

**使用requirements.txt管理依赖**：
```
fastapi==0.104.1
uvicorn[standard]==0.24.0
pydantic==2.5.0
python-dotenv==1.0.0
```

#### 5.3.2 依赖更新策略

- 定期更新依赖版本
- 测试通过后再更新
- 记录依赖更新日志

#### 5.3.3 依赖冲突处理

- 使用虚拟环境隔离依赖
- 使用pip-tools管理依赖版本
- 测试依赖兼容性

---

## 六、实现步骤

> **参考文档**：
> - 《AI医生系统-项目结构设计.md》
> - 《AI医生系统-技术架构设计-项目实现与部署.md》

### 6.1 分阶段实现计划

#### Phase 1: 基础框架搭建（2天）

**目标**：搭建服务基础框架，配置开发环境

**任务清单**：
- [ ] 创建服务目录结构（参考项目结构设计）
  - `app/main.py`：FastAPI应用入口
  - `app/api/routes.py`：API路由
  - `app/services/`：业务服务
  - `app/models/`：数据模型
  - `app/utils/`：工具类
  - `app/config/`：配置
  - `app/builders/`：构建器
- [ ] 配置FastAPI框架
  - 创建FastAPI应用实例
  - 配置CORS、中间件等
  - 配置日志
- [ ] 实现基础工具类
  - 异常处理工具
  - 日志工具
  - 响应格式化工具
- [ ] 配置日志和错误处理（参考错误处理规范）

**验收标准**：
- 服务可以启动
- 健康检查接口正常
- 日志输出正常

#### Phase 2: 风险评估实现（4天）

**目标**：实现风险评估逻辑

**任务清单**：
- [ ] 实现风险评估引擎（RiskAssessmentEngine）
  - 实现高危识别（使用高危识别规则库：6.1）
  - 实现严重程度评估（使用严重程度评估规则库：6.2）
  - 实现紧急程度分级（使用紧急程度分级规则库：6.3）
  - 实现复评与升级规则（使用复评与升级规则库：6.4）
- [ ] 实现规则库（初期使用简单规则，后期完善）
  - 高危识别规则库
  - 严重程度评估规则库
  - 紧急程度分级规则库
  - 复评与升级规则库
- [ ] 单元测试

**验收标准**：
- 风险评估逻辑正确
- 单元测试通过

#### Phase 3: 分诊评估实现（2天）

**目标**：实现分诊评估逻辑

**任务清单**：
- [ ] 实现分诊引擎（TriageEngine）
  - 实现风险等级计算
  - 实现分诊级别确定
  - 实现风险因素识别
- [ ] 单元测试

**验收标准**：
- 分诊评估逻辑正确
- 单元测试通过

#### Phase 4: 升级规则实现（2天）

**目标**：实现升级规则逻辑

**任务清单**：
- [ ] 实现升级规则引擎（UpgradeRuleEngine）
  - 实现复评时间窗口确定
  - 实现提前复评条件确定
  - 实现升级条件确定
- [ ] 单元测试

**验收标准**：
- 升级规则逻辑正确
- 单元测试通过

#### Phase 5: 终点结论包构建实现（3天）

**目标**：实现终点结论包构建逻辑

**任务清单**：
- [ ] 实现终点结论包构建器（ConclusionPackageBuilder）
  - 实现结论构建
  - 实现必须排除项状态
  - 实现关键依据提取
  - 实现行动与随访
- [ ] 单元测试

**验收标准**：
- 终点结论包构建逻辑正确
- 单元测试通过

#### Phase 6: 接口实现（2天）

**目标**：实现API接口

**任务清单**：
- [ ] 实现API接口（遵循API接口规范）
  - 实现`POST /api/v1/risk/assess`
  - 实现`POST /api/v1/risk/triage`
  - 实现`POST /api/v1/risk/upgrade-rules`
  - 实现`POST /api/v1/risk/conclusion-package`
  - 实现请求/响应模型
  - 实现统一响应格式
- [ ] 实现错误处理（遵循错误处理规范）
  - 实现全局异常处理器
  - 实现错误码定义（6000-6099）
  - 实现错误响应格式
- [ ] 实现数据验证
  - 使用Pydantic进行数据验证
  - 验证请求参数
  - 验证响应数据

**验收标准**：
- API接口符合规范
- 错误处理完善
- 数据验证正确
- API文档完整

#### Phase 7: 集成与优化（2天）

**目标**：服务间集成测试和性能优化

**任务清单**：
- [ ] 服务间集成测试
  - 与diagnosis-service集成测试
- [ ] 性能优化（参考性能与评估）
  - 优化风险评估性能
  - 优化分诊评估性能
  - 使用缓存减少重复计算
- [ ] 代码优化
  - 代码重构
  - 代码审查
  - 文档完善

**验收标准**：
- 集成测试通过
- 性能满足要求（响应时间<1秒）
- 代码质量达标

### 6.2 优先级排序

- **P0（必须）**：
  - 风险评估
  - 分诊评估
  - API接口实现
  - 错误处理

- **P1（重要）**：
  - 升级规则
  - 终点结论包构建
  - 性能优化
  - 单元测试

- **P2（可选）**：
  - 缓存优化
  - 监控和告警

### 6.3 里程碑定义

- **Milestone 1**：基础框架完成（Phase 1）
  - 服务可以启动
  - 健康检查接口正常

- **Milestone 2**：风险评估完成（Phase 2）
  - 风险评估逻辑正确

- **Milestone 3**：分诊评估完成（Phase 3）
  - 分诊评估逻辑正确

- **Milestone 4**：接口实现完成（Phase 6）
  - API接口符合规范
  - 错误处理完善

- **Milestone 5**：集成测试通过（Phase 7）
  - 集成测试通过
  - 性能满足要求

---

## 七、测试策略

### 7.1 单元测试计划

**测试覆盖范围**：
- 风险评估逻辑
- 分诊评估逻辑
- 升级规则逻辑
- 终点结论包构建逻辑

**测试用例设计**：

1. **风险评估测试用例**：
   - 高风险患者
   - 中风险患者
   - 低风险患者
   - 有危险信号的患者
   - 无危险信号的患者

2. **分诊评估测试用例**：
   - 紧急患者
   - 非紧急患者
   - 不同风险等级的患者

3. **升级规则测试用例**：
   - 高风险患者的升级规则
   - 低风险患者的升级规则
   - 不同风险等级的复评时间窗口

4. **终点结论包构建测试用例**：
   - 正常诊断结论
   - 有必须排除项的情况
   - 无必须排除项的情况

**Mock策略**：
- Mock规则库
- Mock外部服务

**测试框架**：
- pytest
- pytest-asyncio（异步测试）
- pytest-mock（Mock支持）

**测试覆盖率要求**：≥80%

### 7.2 集成测试计划

**服务间集成测试**：
- 与diagnosis-service集成测试
  - 测试API接口调用
  - 测试响应格式
  - 测试错误处理

**数据流测试**：
- 测试完整的数据流转
- 测试CDP数据更新
- 测试响应数据格式

**端到端测试**：
- 测试完整的风险评估流程
- 测试分诊评估流程
- 测试终点结论包构建流程

### 7.3 性能测试计划

**性能测试指标**（参考性能与评估）：
- 响应时间：< 1秒（风险评估）
- 并发处理：100+ 并发
- 资源消耗：内存 < 512MB

**性能测试场景**：
- 单用户请求
- 并发请求（25/50/100/200）
- 不同诊断数量（1/3/5）
- 不同风险等级（L1/L2/L3/L4/L5）

**性能优化目标**：
- 响应时间：< 500ms（优化后）
- 并发处理：500+ 并发（优化后）

### 7.4 测试数据准备

**测试数据设计**：
- 正常诊断候选集数据
- 边界情况数据（空诊断候选集、单个诊断候选等）
- 不同风险等级数据（高风险、中风险、低风险）

**测试数据管理**：
- 使用测试数据文件（JSON/YAML）
- 使用Fixture管理测试数据
- 测试数据隔离（不影响生产数据）

**测试环境配置**：
- 独立的测试环境
- Mock外部服务
- Mock规则库

---

## 八、风险评估

### 8.1 技术风险

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| 高危识别漏检 | 高 | 完善高危识别规则库，定期更新，宁可误报不能漏报 |
| 风险等级计算不准确 | 中 | 使用多种方法验证，结合专家经验 |
| 性能瓶颈 | 中 | 使用缓存，优化算法，异步处理 |
| 复评规则不合理 | 中 | 完善复评规则库，增加测试用例 |

### 8.2 业务风险

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| 高危识别漏检 | 高 | 完善高危识别规则库，定期更新，宁可误报不能漏报 |
| 风险等级评估错误 | 中 | 完善评估规则，增加测试用例 |
| 用户体验差 | 中 | 优化响应时间，优化错误提示 |

### 8.3 时间风险

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| 开发时间超期 | 中 | 分阶段实现，优先级排序，及时调整计划 |
| 测试时间不足 | 低 | 提前准备测试数据，自动化测试 |

---

## 九、后续优化方向

### 9.1 性能优化

**优化方向**：
- 使用缓存减少重复计算
- 异步处理优化
- 算法优化（如使用更高效的风险评估算法）

**优化计划**：
- Phase 1：实现基础缓存（Redis）
- Phase 2：优化风险评估算法
- Phase 3：异步处理优化

### 9.2 功能扩展

**扩展方向**：
- 支持动态风险监控（实时更新风险等级）
- 支持风险预测（基于历史数据）
- 支持个性化风险评估（基于患者特征）

**扩展计划**：
- Phase 1：支持动态风险监控
- Phase 2：支持风险预测
- Phase 3：支持个性化评估

### 9.3 可观测性

**监控和告警**：
- 接口性能监控
- 错误率监控
- 业务指标监控（风险评估准确率、高危识别率等）

**日志和追踪**：
- 结构化日志
- 分布式追踪（traceId）
- 业务日志分析

---

**文档版本**：v1.0  
**创建日期**：2025年1月  
**维护人员**：开发团队

