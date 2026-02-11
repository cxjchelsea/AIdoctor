# AI医生系统 - API接口规范

> **文档定位**：本文档详细定义AI医生系统的所有REST API接口规范，包括请求参数、响应格式、错误码等。  
> **参考文档**：《AI医生系统-系统功能设计.md》、《AI医生系统-技术架构设计.md》、《AI医生系统-数据模型设计.md》  
> **设计基础**：基于DR.KNOWS论文，采用单主Agent + 多工具Tools架构设计

---

## 一、接口规范说明

### 1.1 基础信息

- **API版本**：v1
- **基础路径**：`/api/v1`
- **数据格式**：JSON
- **字符编码**：UTF-8
- **认证方式**：Bearer Token（JWT）

### 1.2 统一响应格式

所有接口统一使用以下响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1705123456789
}
```

**响应字段说明**：
- `code`: 状态码（200表示成功，其他表示错误，详见错误码定义）
- `message`: 响应消息
- `data`: 响应数据（成功时返回，失败时可能为null）
- `timestamp`: 时间戳（毫秒）

### 1.3 分页响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  },
  "timestamp": 1705123456789
}
```

### 1.4 错误响应格式

```json
{
  "code": 400,
  "message": "参数验证失败",
  "data": null,
  "errors": [
    {
      "field": "userId",
      "message": "用户ID不能为空"
    }
  ],
  "traceId": "3f1a9b1d2c5a4b0e",
  "requestId": "req_20260122_0001",
  "service": "diagnosis-service",
  "agentId": "agent_main",
  "cdpId": "cdp_123456",
  "retryable": false,
  "timestamp": 1705123456789,
  "path": "/api/v1/diagnosis/start"
}
```

> **说明（重要）**：错误响应字段的完整契约、错误码段位分配与HTTP状态码映射，以《AI医生系统-错误处理规范.md》为准；本文档只在接口示例中引用，避免多处口径漂移。

### 1.5 字段命名与CDP字段映射约定（重要）

为保证“对外接口易用”与“CDP内部结构可审计”同时成立，统一约定：
- **对外REST API**：字段使用 `camelCase`（例如：`cdpId`、`needsClinicalMode`、`workMode`）
- **CDP内部JSON（`cdp.*` 的各JSON列）**：字段使用 `snake_case`（例如：`health_state_assessment.needs_clinical_mode`、`work_mode`）

> 说明：API层可在网关/服务内部完成字段映射；CDP持久化结构必须与《AI医生系统-技术架构设计-CDP数据与状态管理.md》6.1保持一致。

---

## 二、诊断服务接口（diagnosis-service）

> **说明**：diagnosis-service是主Agent（Clinical Agent Brain）的实现，负责运行循环、工具调用调度、CDP管理、证据融合、冲突解决等核心功能。

### 2.0 工具调用协议（Tool Protocol）

> **说明**：主Agent通过工具调用协议调用各个工具服务。所有工具服务必须实现统一的ToolContext输入和ToolResult输出格式。

#### 2.0.1 工具调用接口（通用）

**接口路径**：`POST /api/v1/tools/{tool_id}/invoke`

**接口描述**：主Agent调用工具服务的统一接口

**路径参数**：
- `tool_id`：工具ID（tool_0, tool_1, tool_2, tool_3, tool_4, tool_5, tool_6, tool_7）

**请求头**：
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数（ToolContext）**：
```json
{
  "traceId": "trace_123456",
  "cdpReference": {
    "cdpId": "cdp_001",
    "version": 1,
    "readFields": ["cdp.patient_state", "cdp.ddx"]
  },
  "agentStateSummary": {
    "currentStep": 2,
    "workMode": "clinical_mode"
  },
  "constraints": {
    "maxTimeSeconds": 30,
    "maxCost": 10.0,
    "riskLevelLimit": "L3"
  },
  "callParams": {
    "tool_specific_params": "value"
  }
}
```

**响应示例（ToolResult）**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "traceId": "trace_123456",
    "toolId": "tool_3",
    "status": "success",
    "payload": {
      "ddx_rank_list": [
        {
          "disease_name": "疾病名称",
          "disease_cui": "CUI编码",
          "rank": 1,
          "tier": "Tier1",
          "probability": 0.85,
          "pros": ["支持证据列表"],
          "cons": ["反对证据列表"],
          "missing": ["缺失证据列表"]
        }
      ],
      "tier1_most_likely": [
        {
          "disease_name": "疾病名称",
          "disease_cui": "CUI编码",
          "probability": 0.85
        }
      ],
      "tier2_must_exclude": [],
      "tier3_active_alternatives": []
    },
    "evidence": [
      {
        "source": "knowledge_base",
        "reference": "chief_complaint_kg_entry_001",
        "strength": "strong"
      }
    ],
    "quality": {
      "confidence": 0.85,
      "completeness": 0.90,
      "accuracy": 0.88
    },
    "suggestedWrites": [
      {
        "fieldPath": "cdp.ddx",
        "value": {
          "ddx_rank_list": [...],
          "tier1_most_likely": [...],
          "tier2_must_exclude": [],
          "tier3_active_alternatives": []
        },
        "reason": "更新鉴别诊断列表"
      }
    ],
    "errors": [],
    "durationMs": 2500,
    "metadata": {}
  },
  "timestamp": 1705123456789
}
```

**响应状态说明**：

| status | 说明 | 主Agent处理策略 |
|--------|------|----------------|
| `success` | 工具执行成功，输出完整 | 评估quality，决定是否写回CDP |
| `partial_success` | 工具执行部分成功，输出不完整 | 评估quality和errors，决定是否写回CDP或重试 |
| `failure` | 工具执行失败 | 根据错误类型决定是否重试或使用降级策略 |
| `timeout` | 工具执行超时 | 根据工具重要性决定是否重试或使用降级策略 |

**错误响应示例**：
```json
{
  "code": 500,
  "message": "工具执行失败",
  "data": {
    "traceId": "trace_123456",
    "toolId": "tool_3",
    "status": "failure",
    "payload": null,
    "evidence": [],
    "quality": {
      "confidence": 0.0,
      "completeness": 0.0
    },
    "suggestedWrites": [],
    "errors": [
      {
        "errorType": "runtime_error",
        "errorMessage": "知识库连接失败",
        "errorDetails": {
          "service": "knowledge_base_service",
          "error_code": "KB_CONNECTION_FAILED"
        }
      }
    ],
    "durationMs": 5000,
    "metadata": {}
  },
  "timestamp": 1705123456789
}
```

#### 2.0.2 工具调用协议字段说明

**ToolContext字段说明**：

| 字段路径 | 数据类型 | 是否必填 | 说明 |
|---------|---------|---------|------|
| `traceId` | String | 是 | 追踪ID（用于审计和调试） |
| `cdpReference` | JSON | 是 | CDP引用 |
| `cdpReference.cdpId` | String | 是 | CDP ID |
| `cdpReference.version` | Integer | 是 | CDP版本号 |
| `cdpReference.readFields` | Array | 是 | 工具需要读取的CDP字段路径 |
| `agentStateSummary` | JSON | 是 | AgentState摘要 |
| `agentStateSummary.currentStep` | Integer | 是 | 当前诊断步骤（1-5） |
| `agentStateSummary.workMode` | String | 是 | 工作态（wellness_mode/clinical_mode） |
| `constraints` | JSON | 否 | 约束（成本/时间/风险） |
| `constraints.maxTimeSeconds` | Integer | 否 | 最大执行时间（秒） |
| `constraints.maxCost` | Float | 否 | 最大成本 |
| `constraints.riskLevelLimit` | String | 否 | 风险等级限制（L1/L2/L3/L4） |
| `callParams` | JSON | 否 | 工具调用参数（工具特定） |

**ToolResult字段说明**：

| 字段路径 | 数据类型 | 是否必填 | 说明 |
|---------|---------|---------|------|
| `traceId` | String | 是 | 追踪ID（与ToolContext中的traceId一致） |
| `toolId` | String | 是 | 工具ID |
| `status` | String | 是 | 执行状态（success/partial_success/failure/timeout） |
| `payload` | JSON | 是 | 输出payload（工具特定结构） |
| `evidence` | Array | 是 | 证据引用 |
| `evidence[].source` | String | 是 | 证据来源（knowledge_base/kg_path/rule/llm） |
| `evidence[].reference` | String | 是 | 证据引用（CUI/路径ID/规则ID/LLM prompt） |
| `evidence[].strength` | String | 是 | 证据强度（strong/medium/weak） |
| `quality` | JSON | 是 | 质量指标 |
| `quality.confidence` | Float | 是 | 置信度（0.0-1.0） |
| `quality.completeness` | Float | 是 | 完整度（0.0-1.0） |
| `quality.accuracy` | Float | 否 | 准确度（0.0-1.0，如可评估） |
| `suggestedWrites` | Array | 是 | 建议写回CDP的字段路径 |
| `suggestedWrites[].fieldPath` | String | 是 | 字段路径（如`cdp.ddx`） |
| `suggestedWrites[].value` | JSON | 是 | 字段值 |
| `suggestedWrites[].reason` | String | 是 | 写回原因 |
| `errors` | Array | 是 | 错误信息（无错误时为空数组） |
| `errors[].errorType` | String | 是 | 错误类型（timeout/validation_error/runtime_error） |
| `errors[].errorMessage` | String | 是 | 错误消息 |
| `errors[].errorDetails` | JSON | 否 | 错误详情 |
| `durationMs` | Integer | 是 | 执行时间（毫秒） |
| `metadata` | JSON | 否 | 元数据（工具特定） |

> **参考文档**：详细的工具调用协议定义请参考《AI医生系统-技术架构设计-核心架构.md》第三章 Tool协议与I/O契约。

### 2.1 AI诊断入口判定流程（tool_0）

**说明**：这是用户发起咨询后的第一步，通过五个步骤完成用户意图识别、症状识别、方向澄清、危险信号检查和路径输出。

#### 2.1.1 Step 1｜接收用户输入

**接口路径**：`POST /api/v1/entry-assessment/step1-receive-input`

**接口描述**：Step 1 - 接收用户自然语言描述本次来访目的

**请求头**：
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**：
```json
{
  "userId": "user123",
  "userInput": "我最近有点头痛，但不严重",
  "inputType": "text"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userIntent": {
      "text": "我最近有点头痛，但不严重",
      "intent": "symptom_consultation",
      "entities": ["头痛"],
      "isMixed": false
    }
  },
  "timestamp": 1705123456789
}
```

#### 2.1.2 Step 2｜识别用户是否"有症状/困扰"

**接口路径**：`POST /api/v1/entry-assessment/step2-identify-symptom`

**接口描述**：Step 2 - 识别用户是否有症状/困扰

**请求参数**：
```json
{
  "userIntent": {
    "text": "我最近有点头痛，但不严重",
    "intent": "symptom_consultation",
    "entities": ["头痛"],
    "isMixed": false
  }
}
```

**响应示例**（情况B：存在症状/困扰）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "symptomStatus": {
      "status": "has_symptom",
      "symptoms": ["头痛"],
      "hasWellnessIntent": false,
      "isMixed": false
    }
  },
  "timestamp": 1705123456789
}
```

**响应示例**（情况C：不确定/模糊/混合诉求）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "symptomStatus": {
      "status": "uncertain",
      "symptoms": [],
      "hasWellnessIntent": true,
      "isMixed": true,
      "needsClarification": true
    }
  },
  "timestamp": 1705123456789
}
```

#### 2.1.3 Step 3｜方向澄清（仅对"情况C"触发一次）

**接口路径**：`POST /api/v1/entry-assessment/step3-clarification`

**接口描述**：Step 3 - 方向澄清（仅当Step 2判断为"情况C"时触发）

**请求参数**：
```json
{
  "userIntent": {
    "text": "我想体检，但也有点不舒服",
    "intent": "mixed",
    "entities": [],
    "isMixed": true
  },
  "symptomStatus": {
    "status": "uncertain",
    "symptoms": [],
    "hasWellnessIntent": true,
    "isMixed": true
  }
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "clarificationResult": {
      "question": "您当前的主要目标是健康筛查/体检规划，还是症状咨询/问题排查？",
      "direction": "B",
      "clarified": true
    }
  },
  "timestamp": 1705123456789
}
```

#### 2.1.4 Step 4｜危险信号检查（安全兜底，所有用户都要过一次）

**接口路径**：`POST /api/v1/entry-assessment/step4-red-flag-check`

**接口描述**：Step 4 - 危险信号检查（安全兜底，所有用户都要过一次）

**请求参数**：
```json
{
  "userInput": "我最近胸痛，伴有大汗、恶心",
  "basicInfo": {
    "age": 55,
    "gender": "male"
  }
}
```

**响应示例**（命中危险信号）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "redFlagsCheck": {
      "redFlagsHit": true,
      "redFlags": ["胸痛+大汗+恶心"],
      "safetyMessage": "检测到危险信号：胸痛+大汗+恶心。建议您优先线下就医或急诊。",
      "shouldExit": true
    }
  },
  "timestamp": 1705123456789
}
```

**响应示例**（未命中危险信号）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "redFlagsCheck": {
      "redFlagsHit": false,
      "redFlags": [],
      "shouldExit": false
    }
  },
  "timestamp": 1705123456789
}
```

#### 2.1.5 Step 5｜输出路径结果并跳转

**接口路径**：`POST /api/v1/entry-assessment/step5-path-selection`

**接口描述**：Step 5 - 输出路径结果并跳转

**请求参数**：
```json
{
  "symptomStatus": {
    "status": "has_symptom",
    "symptoms": ["头痛"]
  },
  "clarificationResult": null,
  "redFlagsCheck": {
    "redFlagsHit": false,
    "redFlags": []
  }
}
```

**响应示例**（进入症状诊断路径）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pathResult": {
      "path": "B",
      "pathName": "症状诊断路径",
      "nextStep": "阶段1｜问诊",
      "cdpId": "cdp_123456"
    }
  },
  "timestamp": 1705123456789
}
```

**响应示例**（进入健康筛查路径）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pathResult": {
      "path": "A",
      "pathName": "健康筛查路径",
      "nextStep": "A1｜需求分类",
      "cdpId": "cdp_123456"
    }
  },
  "timestamp": 1705123456789
}
```

#### 2.1.6 健康状态判定（整合接口）

**接口路径**：`POST /api/v1/health-state-assessment/assess`

**接口描述**：完整的健康状态判定流程（整合Step 1-5）。这是诊断流程的第一步（tool_0：健康状态判定工具）。

**请求头**：
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**：
```json
{
  "userId": "user123",
  "userInput": "我最近胸痛",
  "basicInfo": {
    "age": 45,
    "gender": "male",
    "bmi": 26.5
  },
  "symptoms": ["胸痛"],
  "vitalSigns": {
    "bp": {"systolic": 130, "diastolic": 85},
    "heartRate": 75
  }
}
```

**响应示例**（进入临床诊疗态）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp_123456",
    "healthStateAssessment": {
      "needsClinicalMode": true,
      "workMode": "clinical_mode",
      "riskLevel": "L2",
      "assessmentReason": "综合入口判定与风险评估，建议进入临床诊疗态",
      "symptomSeverity": "moderate",
      "earlyRiskSignals": [],
      "entryAssessment": {
        "userInput": "我最近胸痛",
        "hasSymptom": true,
        "symptomStatus": "存在症状",
        "clarificationNeeded": false,
        "clarificationResult": null,
        "redFlagsHit": false,
        "redFlagsList": [],
        "pathSelected": "B"
      }
    }
  },
  "timestamp": 1705123456789
}
```

**响应示例**（进入健康管理态）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp_123456",
    "healthStateAssessment": {
      "needsClinicalMode": false,
      "workMode": "wellness_mode",
      "riskLevel": "L3",
      "assessmentReason": "入口判定为无症状且未命中危险信号，建议进入健康管理态",
      "symptomSeverity": "normal",
      "earlyRiskSignals": [],
      "entryAssessment": {
        "userInput": "我想做个体检",
        "hasSymptom": false,
        "symptomStatus": "明确无症状",
        "clarificationNeeded": false,
        "clarificationResult": null,
        "redFlagsHit": false,
        "redFlagsList": [],
        "pathSelected": "A"
      }
    },
    "wellnessPlan": {
      "riskManagement": [
        {
          "riskType": "高血压风险",
          "riskLevel": "L3",
          "managementAdvice": "建议监测血压，控制盐摄入"
        }
      ],
      "lifestyleAdvice": ["低盐低脂饮食", "每周规律运动"],
      "followUpPlan": [
        {
          "followUpType": "血压复查",
          "timing": "3个月后",
          "purpose": "评估血压趋势"
        }
      ],
      "reassurance": "目前未见高危信号，可先按健康管理建议执行；如出现不适请及时升级就医。",
      "upgradeConditions": ["出现胸痛、气短、大汗、恶心等高危症状", "症状突然加重或持续不缓解"]
    }
  },
  "timestamp": 1705123456789
}
```

---

### 2.2 开始诊断（5步AI循证诊断流程入口）

**接口路径**：`POST /api/v1/diagnosis/start`

**接口描述**：开始一个新的诊断流程。先调用健康状态判定（tool_0），然后根据工作态进入不同的流程：
- **健康管理态**：进入健康筛查流程（A路径，A1-A5）
- **临床诊疗态**：进入5步AI循证诊断流程（Step 1-5）

**5步AI循证诊断流程说明**（临床诊疗态）：
- **Step 1：识别问题** - 把用户的自然语言描述转化成可推理、可复用、可审计的结构化"问题清单"
- **Step 2：构建鉴别诊断候选集并分层** - 建立该主诉的鉴别诊断全集，并按临床风险与证据强度分成三层（首要假设/主要备选/必须排除）
- **Step 3：组织候选集并建立分流路径** - 把分层候选清单组织成可推进的推理结构，并提炼出分流路径
- **Step 4：采集关键证据并形成排序与验证计划** - 系统采集能够"推动排序变化"的关键证据，形成稳定的三层清单，并制定验证计划
- **Step 5：回填证据并输出终点结论包** - 将验证结果回填，更新三层排序，并输出可行动、可审计的终点结论包（四要素：结论、必须排除项状态、关键依据、行动与随访）

**请求头**：
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**：
```json
{
  "userId": "user123",
  "diagnosisType": "symptom",
  "symptomInfo": {
    "chiefComplaint": "我最近胸痛",
    "duration": "3天",
    "severity": 6,
    "frequency": "intermittent",
    "location": "胸部正中",
    "accompanyingSymptoms": ["气短"],
    "features": {
      "trigger": "运动后",
      "relief": "休息后缓解"
    }
  },
  "examinationRecordId": null
}
```

**请求参数说明**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | String | 是 | 用户ID |
| diagnosisType | String | 是 | 诊断类型：symptom/examination/comprehensive |
| symptomInfo | Object | 否 | 初始症状信息 |
| examinationRecordId | Long | 否 | 检查记录ID（如果是基于检查结果的诊断） |

**响应示例**（信息不足，需要追问）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosisId": "diag_123456",
    "status": "questioning",
    "completeness": 0.55,
    "question": {
      "question": "您这个症状出现多久了？",
      "questionType": "symptom_duration",
      "missingInfoType": "duration",
      "priority": "required",
      "required": true
    },
    "informationGaps": {
      "requiredGaps": ["duration"],
      "importantGaps": ["accompanying_symptoms"],
      "optionalGaps": []
    }
  },
  "timestamp": 1705123456789
}
```

**响应示例**（信息足够，直接分析）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosisId": "diag_123456",
    "status": "analyzing",
    "completeness": 0.85,
    "informationGaps": {
      "requiredGaps": [],
      "importantGaps": [],
      "optionalGaps": ["lifestyle"]
    }
  },
  "timestamp": 1705123456789
}
```

---

### 2.2 回答追问问题

**接口路径**：`POST /api/v1/diagnosis/{diagnosisId}/answer`

**接口描述**：回答系统的追问问题，继续收集信息

**路径参数**：
- `diagnosisId`: 诊断ID

**请求参数**：
```json
{
  "answer": "3天了",
  "answerType": "symptom"
}
```

**请求参数说明**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| answer | String | 是 | 用户回答内容 |
| answerType | String | 否 | 回答类型：symptom/sign/history/other |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosisId": "diag_123456",
    "status": "questioning",
    "completeness": 0.65,
    "question": {
      "question": "疼痛是持续的还是阵发性的？",
      "questionType": "symptom_frequency",
      "missingInfoType": "frequency",
      "required": true
    }
  },
  "timestamp": 1705123456789
}
```

---

### 2.3 收集症状信息

**接口路径**：`POST /api/v1/diagnosis/{diagnosisId}/symptom`

**接口描述**：收集或更新症状信息

**路径参数**：
- `diagnosisId`: 诊断ID

**请求参数**：
```json
{
  "chiefComplaint": "我最近胸痛",
  "duration": "3天",
  "severity": 6,
  "frequency": "intermittent",
  "location": "胸部正中",
  "accompanyingSymptoms": ["气短", "出汗"],
  "features": {
    "trigger": "运动后",
    "relief": "休息后缓解"
  }
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosisId": "diag_123456",
    "status": "questioning",
    "completeness": 0.70
  },
  "timestamp": 1705123456789
}
```

---

### 2.4 收集体征数据

**接口路径**：`POST /api/v1/diagnosis/{diagnosisId}/sign`

**接口描述**：收集或更新生命体征数据

**路径参数**：
- `diagnosisId`: 诊断ID

**请求参数**：
```json
{
  "bp": {
    "systolic": 130,
    "diastolic": 85
  },
  "heartRate": 75,
  "temperature": 36.5,
  "oxygenSaturation": 98,
  "respiratoryRate": 18
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosisId": "diag_123456",
    "status": "questioning",
    "completeness": 0.80
  },
  "timestamp": 1705123456789
}
```

---

### 2.5 上传检查结果

**接口路径**：`POST /api/v1/diagnosis/{diagnosisId}/examination`

**接口描述**：上传检查报告，系统自动识别并更新诊断记录

**路径参数**：
- `diagnosisId`: 诊断ID

**请求类型**：`multipart/form-data`

**请求参数**：
- `file`: 文件（图片或PDF）

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosisId": "diag_123456",
    "examinationRecordId": "exam_789",
    "status": "questioning",
    "completeness": 0.90,
    "ocrResult": {
      "rawText": "...",
      "structuredData": {
        "indicators": [
          {
            "name": "WBC",
            "value": 6.5,
            "unit": "10^9/L",
            "normalRange": "4-10",
            "status": "normal"
          }
        ]
      }
    }
  },
  "timestamp": 1705123456789
}
```

---

### 2.6 执行诊断分析

**接口路径**：`POST /api/v1/diagnosis/{diagnosisId}/analyze`

**接口描述**：手动触发诊断分析（当信息完整度达到要求时）

**路径参数**：
- `diagnosisId`: 诊断ID

**请求参数**：无

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosisId": "diag_123456",
    "status": "analyzing"
  },
  "timestamp": 1705123456789
}
```

**注意**：诊断分析是异步的，需要轮询获取结果或使用WebSocket推送。

---

### 2.7 获取诊断结果

**接口路径**：`GET /api/v1/diagnosis/{id}`

**接口描述**：获取诊断结果详情

**路径参数**：
- `id`: 诊断ID

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosisId": "diag_123456",
    "status": "completed",
    "summary": "根据您的症状和健康档案，我考虑以下几个可能的方向：心绞痛、焦虑症、胃食管反流",
    "threeLayerResult": {
      "primaryHypothesis": {
        "disease": "心绞痛",
        "score": 0.75,
        "layer": "primary_hypothesis",
        "evidence": {
          "supporting": [
            {"item": "胸痛", "type": "symptom", "strength": "strong"},
            {"item": "气短", "type": "symptom", "strength": "medium"},
            {"item": "有高血压史", "type": "medical_history", "strength": "medium"}
          ],
          "opposing": []
        }
      },
      "mainAlternatives": [
        {
          "disease": "焦虑症",
          "score": 0.55,
          "layer": "main_alternative",
          "evidence": {
            "supporting": [
              {"item": "阵发性胸痛", "type": "symptom", "strength": "medium"}
            ],
            "opposing": []
          }
        }
      ],
      "mustExclude": {
        "disease": "急性心肌梗死",
        "score": 0.30,
        "layer": "must_exclude",
        "reason": "高危诊断，必须排除，即使可能性不高",
        "status": "not_excluded",
        "excludeReason": "需要心电图等检查才能排除"
      }
    },
    "conclusionPackage": {
      "要素1：结论": {
        "type": "可确证",
        "diagnosis": "心绞痛",
        "confidence": 0.75,
        "severity": "中等",
        "evidenceChain": [
          "胸痛伴活动后加重",
          "有高血压史",
          "心电图显示ST段压低"
        ]
      },
      "要素2：必须排除项状态": {
        "excluded": [
          {
            "diagnosis": "急性心肌梗死",
            "status": "已排除",
            "reason": "心电图无ST段抬高，心肌酶正常"
          }
        ],
        "needOfflineExclude": [],
        "notExcluded": []
      },
      "要素3：关键依据": {
        "positiveEvidence": [
          {
            "evidence": "胸痛伴活动后加重",
            "source": "主诉",
            "strength": "强证据"
          },
          {
            "evidence": "有高血压史",
            "source": "既往史",
            "strength": "中证据"
          },
          {
            "evidence": "心电图显示ST段压低",
            "source": "检查结果",
            "strength": "强证据"
          }
        ],
        "negativeEvidence": [
          {
            "evidence": "无发热",
            "excludedDirection": "感染性疾病"
          }
        ]
      },
      "要素4：行动与随访": {
        "immediateActions": {
          "medicalAdvice": "建议尽快到心内科就诊",
          "examinations": ["心电图", "心脏彩超"],
          "treatmentDirection": "抗心绞痛治疗",
          "urgentConditions": "如出现持续胸痛、大汗、恶心等症状，请立即就医"
        },
        "reviewTimeWindow": {
          "defaultTime": "3天后",
          "earlyReviewConditions": ["症状加重", "出现新症状"],
          "delayReviewConditions": ["症状完全缓解"]
        },
        "upgradeConditions": [
          "疼痛突然加重",
          "伴有大汗、恶心等症状",
          "处理无效"
        ]
      }
    },
    "possibilities": [
      {
        "disease": "心绞痛",
        "level": "high",
        "confidence": 0.75,
        "supportingEvidence": [
          "胸痛",
          "气短",
          "有高血压史"
        ],
        "opposingEvidence": [],
        "missingInfo": ["心电图检查"]
      },
      {
        "disease": "焦虑症",
        "level": "medium",
        "confidence": 0.55,
        "supportingEvidence": [
          "阵发性胸痛"
        ],
        "opposingEvidence": [],
        "missingInfo": []
      }
    ],
    "examinationSuggestion": {
      "priorityExaminations": [
        {
          "name": "心电图",
          "purpose": "确诊",
          "priority": "high",
          "reason": "排除心脏疾病"
        },
        {
          "name": "心脏彩超",
          "purpose": "评估",
          "priority": "high",
          "reason": "评估心脏功能"
        }
      ],
      "optionalExaminations": [
        {
          "name": "胸部CT",
          "purpose": "排除",
          "priority": "medium",
          "reason": "排除肺部疾病"
        }
      ],
      "explanation": "建议您尽快到心内科就诊，做心电图和心脏彩超检查。"
    },
    "medicalAdvice": {
      "department": "心内科",
      "timing": "尽快",
      "preparation": {
        "documents": [
          "健康档案",
          "检查报告",
          "用药清单"
        ],
        "questions": [
          "这个症状需要立即治疗吗？",
          "需要做哪些检查？",
          "平时需要注意什么？"
        ]
      },
      "sbarSummary": "患者，男性，45岁，主诉胸痛3天，伴有气短，有高血压史。考虑心绞痛可能性较高，建议尽快到心内科就诊。"
    },
    "precautions": {
      "observationPoints": [
        "观察疼痛是否加重",
        "注意是否有其他症状出现"
      ],
      "dangerSigns": [
        "疼痛突然加重",
        "伴有大汗、恶心",
        "呼吸困难"
      ],
      "disclaimer": "本分析仅供参考，不替代医生诊断，最终诊断需由专业医生确定。"
    },
    "reasoning": "根据您的症状（胸痛、气短）和健康档案（有高血压史），结合五引擎融合分析，心绞痛的可能性最高。"
  },
  "timestamp": 1705123456789
}
```

---

### 2.8 获取诊断历史

**接口路径**：`GET /api/v1/diagnosis/history`

**接口描述**：获取用户的诊断历史记录

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | String | 是 | 用户ID |
| page | Integer | 否 | 页码（默认1） |
| pageSize | Integer | 否 | 每页数量（默认20） |
| status | String | 否 | 状态筛选（collecting/questioning/analyzing/completed/cancelled） |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "diag_123456",
        "diagnosisType": "symptom",
        "status": "completed",
        "chiefComplaint": "我最近胸痛",
        "createdAt": "2025-01-15T10:30:00",
        "completedAt": "2025-01-15T10:35:00"
      }
    ],
    "total": 10,
    "page": 1,
    "pageSize": 20,
    "totalPages": 1
  },
  "timestamp": 1705123456789
}
```

---

## 三、健康筛查流程接口（Wellness Screening - A路径）

### 3.1 A1｜需求分类

**接口路径**：`POST /api/v1/wellness-screening/a1-demand-classification`

**接口描述**：A1 - 需求分类，把用户需求归入四类之一

**请求参数**：
```json
{
  "userId": "user123",
  "cdpId": "cdp_123456",
  "userInput": "我想做个体检"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "demandType": {
      "type": 1,
      "typeName": "筛查建议",
      "confidence": 0.9
    }
  },
  "timestamp": 1705123456789
}
```

### 3.2 A2｜通用最小健康档案

**接口路径**：`POST /api/v1/wellness-screening/a2-collect-health-profile`

**接口描述**：A2 - 采集最小健康档案

**请求参数**：
```json
{
  "userId": "user123",
  "cdpId": "cdp_123456",
  "healthProfile": {
    "basicInfo": {
      "age": 30,
      "gender": "male",
      "height": 175,
      "weight": 70
    },
    "pastHistory": [],
    "familyHistory": [],
    "lifestyle": {
      "smoking": "否",
      "drinking": "偶尔",
      "exercise": "是",
      "diet": "正常"
    },
    "medications": []
  }
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "healthProfile": {
      "basicInfo": {...},
      "pastHistory": [],
      "familyHistory": [],
      "lifestyle": {...},
      "medications": [],
      "missingFields": ["familyHistory"],
      "completeness": 0.85
    }
  },
  "timestamp": 1705123456789
}
```

### 3.3 A3｜进入对应分支执行

#### 3.3.1 A3-1｜筛查建议分支

**接口路径**：`POST /api/v1/wellness-screening/a3-1-screening-recommendation`

**接口描述**：A3-1 - 筛查建议分支

**请求参数**：
```json
{
  "userId": "user123",
  "cdpId": "cdp_123456",
  "demandType": 1,
  "healthProfile": {...}
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "branchResult": {
      "branchType": "screening_recommendation",
      "recommendedScreenings": [
        {
          "screeningName": "血常规",
          "recommended": true,
          "frequency": "每年一次",
          "timing": "体检时",
          "reason": "常规健康检查项目"
        }
      ],
      "notRecommendedScreenings": [
        {
          "screeningName": "PET-CT",
          "recommended": false,
          "reason": "无高危因素，不建议常规筛查",
          "alternativeSuggestion": "如有需要可考虑常规CT"
        }
      ],
      "nextUpdateTime": "6个月后"
    }
  },
  "timestamp": 1705123456789
}
```

#### 3.3.2 A3-2｜健康目标管理分支

**接口路径**：`POST /api/v1/wellness-screening/a3-3-health-goal`

**接口描述**：A3-3 - 健康目标管理分支

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "branchResult": {
      "branchType": "health_goal",
      "actionPlan": {
        "goals": ["减脂"],
        "currentStatus": "体重70kg，BMI 22.9",
        "resources": "每周可运动3次",
        "timeline": "3个月",
        "actionSteps": [
          "每周运动3次，每次30分钟",
          "控制饮食，减少高热量食物",
          "记录体重变化"
        ],
        "trackingIndicators": ["体重", "BMI", "体脂率"],
        "reviewPoints": ["1周后", "1个月后", "3个月后"]
      }
    }
  },
  "timestamp": 1705123456789
}
```

#### 3.3.4 A3-4｜计划性健康需求分支

**接口路径**：`POST /api/v1/wellness-screening/a3-4-scenario`

**接口描述**：A3-4 - 计划性健康需求分支

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "branchResult": {
      "branchType": "scenario",
      "scenarioPlan": {
        "scenario": "备孕",
        "checklist": [
          "血常规检查",
          "肝肾功能检查",
          "传染病筛查",
          "甲状腺功能检查"
        ],
        "schedule": "备孕前3个月开始",
        "notes": [
          "建议补充叶酸",
          "戒烟限酒",
          "规律作息"
        ]
      }
    }
  },
  "timestamp": 1705123456789
}
```

### 3.4 A4｜统一结果页输出

**接口路径**：`POST /api/v1/wellness-screening/a4-unified-result`

**接口描述**：A4 - 统一结果页输出

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "unifiedResult": {
      "summary": "根据您的健康档案，建议进行常规体检筛查",
      "recommendedActions": [
        "血常规检查（每年一次）",
        "血压监测（每3个月一次）"
      ],
      "notRecommendedActions": [
        "PET-CT（无高危因素，不建议常规筛查）"
      ],
      "nextReviewTime": "6个月后",
      "exitConditions": [
        "出现不适症状",
        "发现高危信号",
        "需要线下评估"
      ]
    }
  },
  "timestamp": 1705123456789
}
```

### 3.5 A5｜随访闭环

**接口路径**：`POST /api/v1/wellness-screening/a5-setup-follow-up`

**接口描述**：A5 - 随访闭环

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "followUpSchedule": {
      "followUpType": "筛查建议",
      "followUpRules": {
        "defaultTime": "6个月后",
        "reviewTrigger": "健康档案更新、筛查项目到期",
        "reminderTemplates": [
          "您的体检时间即将到期，建议及时复查",
          "您的健康档案有更新，建议更新筛查计划"
        ]
      }
    }
  },
  "timestamp": 1705123456789
}
```

---

## 四、检查服务接口（examination-service）

### 3.1 设计检查方案

**接口路径**：`POST /api/v1/examination/plan`

**接口描述**：根据症状、年龄、性别、既往史设计检查方案

**请求参数**：
```json
{
  "userId": "user123",
  "planName": "胸痛相关检查",
  "planType": "diagnostic",
  "targetConditions": {
    "suspectedDiseases": ["心绞痛", "心肌梗死"]
  },
  "symptomInfo": {
    "chiefComplaint": "胸痛",
    "duration": "3天"
  },
  "basicInfo": {
    "age": 45,
    "gender": "male"
  }
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "planId": 1001,
    "planName": "胸痛相关检查",
    "planType": "diagnostic",
    "planItems": [
      {
        "item": "心电图",
        "purpose": "确诊",
        "priority": "high",
        "reason": "排除心脏疾病"
      },
      {
        "item": "心脏彩超",
        "purpose": "评估",
        "priority": "high",
        "reason": "评估心脏功能"
      },
      {
        "item": "血常规",
        "purpose": "辅助诊断",
        "priority": "medium",
        "reason": "排除感染"
      }
    ],
    "targetConditions": {
      "suspectedDiseases": ["心绞痛", "心肌梗死"]
    }
  },
  "timestamp": 1705123456789
}
```

---

### 3.2 上传检查报告

**接口路径**：`POST /api/v1/examination/upload`

**接口描述**：上传检查报告，系统自动识别

**请求类型**：`multipart/form-data`

**请求参数**：
- `file`: 文件（图片或PDF）
- `userId`: 用户ID
- `examinationType`: 检查类型（blood_test/imaging/physical/comprehensive）
- `examinationDate`: 检查日期（可选，格式：yyyy-MM-dd）

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "examinationRecordId": "exam_789",
    "reportType": "image",
    "reportFilePath": "/uploads/exam_789.jpg",
    "ocrResult": {
      "rawText": "血常规检查报告\n白细胞：6.5 10^9/L\n...",
      "confidence": 0.95
    },
    "structuredData": {
      "indicators": [
        {
          "name": "WBC",
          "value": 6.5,
          "unit": "10^9/L",
          "normalRange": "4-10",
          "status": "normal"
        }
      ]
    }
  },
  "timestamp": 1705123456789
}
```

---

### 3.3 OCR识别报告

**接口路径**：`POST /api/v1/examination/ocr`

**接口描述**：OCR识别检查报告（不保存记录）

**请求类型**：`multipart/form-data`

**请求参数**：
- `file`: 文件（图片或PDF）

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "rawText": "血常规检查报告\n白细胞：6.5 10^9/L\n...",
    "structuredData": {
      "indicators": [
        {
          "name": "WBC",
          "value": 6.5,
          "unit": "10^9/L",
          "normalRange": "4-10",
          "status": "normal"
        }
      ]
    }
  },
  "timestamp": 1705123456789
}
```

---

### 3.4 解读检查结果

**接口路径**：`POST /api/v1/examination/{examinationId}/interpret`

**接口描述**：解读检查结果，识别异常指标，分析临床意义

**路径参数**：
- `examinationId`: 检查记录ID

**请求参数**：无

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "examinationId": "exam_789",
    "interpretationResult": {
      "abnormalIndicators": [
        "WBC: 12.5 (偏高)",
        "CRP: 15.2 (偏高)"
      ],
      "clinicalSignificance": {
        "WBC": "白细胞升高可能提示感染或炎症",
        "CRP": "C反应蛋白升高提示炎症反应"
      },
      "possibleDiseases": [
        "细菌感染",
        "炎症性疾病"
      ],
      "suggestions": [
        "建议结合临床症状进一步判断",
        "如伴有发热，建议抗感染治疗",
        "建议复查血常规"
      ]
    }
  },
  "timestamp": 1705123456789
}
```

---

### 3.5 获取检查历史

**接口路径**：`GET /api/v1/examination/history`

**接口描述**：获取用户的检查历史记录

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | String | 是 | 用户ID |
| page | Integer | 否 | 页码（默认1） |
| pageSize | Integer | 否 | 每页数量（默认20） |
| examinationType | String | 否 | 检查类型筛选 |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "exam_789",
        "examinationType": "blood_test",
        "examinationDate": "2025-01-15",
        "planName": "血常规检查",
        "createdAt": "2025-01-15T10:00:00"
      }
    ],
    "total": 5,
    "page": 1,
    "pageSize": 20,
    "totalPages": 1
  },
  "timestamp": 1705123456789
}
```

---

### 8.2 五引擎融合诊断请求参数（更新版）

**接口路径**：`POST /api/v1/engine/diagnose`

**接口描述**：执行五引擎融合诊断

**请求参数**（支持结构化问题清单）：
```json
{
  "structuredQuestionList": {
    "chiefComplaint": {
      "name": "胸痛",
      "duration": "3天",
      "severity": 6,
      "accompanyingSymptoms": ["气短"]
    },
    "vitalSigns": {
      "bloodPressure": {"systolic": 130, "diastolic": 85},
      "heartRate": 75
    },
    "examinationResults": [],
    "keyBackground": {
      "age": 45,
      "gender": "male",
      "medicalHistory": ["高血压"]
    }
  },
  "symptomInfo": {
    "chiefComplaint": "胸痛",
    "duration": "3天",
    "severity": 6,
    "accompanyingSymptoms": ["气短"]
  },
  "vitalSigns": {
    "bp": {"systolic": 130, "diastolic": 85},
    "heartRate": 75
  },
  "examinationResults": [],
  "healthProfile": {
    "age": 45,
    "gender": "male",
    "medicalHistory": ["高血压"]
  }
}
```

**注意**：`structuredQuestionList` 是新格式（推荐），`symptomInfo`、`vitalSigns` 等是旧格式（兼容）。

**响应示例**（包含三层分层结果）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "possibilities": {
      "心绞痛": 0.75,
      "焦虑症": 0.55,
      "胃食管反流": 0.35,
      "急性心肌梗死": 0.30
    },
    "threeLayerResult": {
      "primaryHypothesis": {
        "disease": "心绞痛",
        "score": 0.75,
        "layer": "primary_hypothesis"
      },
      "mainAlternatives": [
        {
          "disease": "焦虑症",
          "score": 0.55,
          "layer": "main_alternative"
        }
      ],
      "mustExclude": {
        "disease": "急性心肌梗死",
        "score": 0.30,
        "layer": "must_exclude",
        "reason": "高危诊断，必须排除，即使可能性不高"
      }
    },
    "engineResults": {
      "ruleEngine": {
        "possibilities": {
          "心绞痛": 0.80
        }
      },
      "knowledgeGraph": {
        "possibilities": {
          "心绞痛": 0.70
        }
      },
      "statistical": {
        "possibilities": {
          "心绞痛": 0.75
        }
      },
      "llm": {
        "possibilities": {
          "心绞痛": 0.75,
          "焦虑症": 0.60
        }
      },
      "differential": {
        "possibilities": {
          "心绞痛": 0.70
        }
      }
    }
  },
  "timestamp": 1705123456789
}
```

---

### 8.3 工具接口

#### 8.3.1 tool_1：病例理解工具

**接口路径**：`POST /api/v1/brain-a/parse`

**接口描述**：病例理解与结构化（tool_1）- 医学概念识别与归一化

**请求参数**：
```json
{
  "text": "我最近胸痛，胸口闷",
  "images": [],
  "examinationReports": []
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "concepts": [
      {"text": "胸痛", "cui": "C0002962", "type": "symptom"},
      {"text": "胸闷", "cui": "C0027769", "type": "symptom"}
    ],
    "structuredData": {
      "chiefComplaint": "胸痛",
      "accompanyingSymptoms": ["胸闷"]
    }
  }
}
```

---

#### 8.3.2 tool_2：主动问诊工具

**接口路径**：`POST /api/v1/brain-b/interview`

**接口描述**：主动问诊与信息补全（tool_2）- 智能追问生成

**请求参数**：
```json
{
  "cdpId": "cdp_123456",
  "currentInfo": {},
  "missingInfo": ["duration", "severity"]
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "question": "您这个症状出现多久了？",
    "questionType": "duration",
    "priority": "required",
    "informationGaps": {
      "required": ["duration"],
      "important": ["severity"]
    }
  }
}
```

---

#### 8.3.3 tool_3：鉴别诊断工具（DR.KNOWS核心）

**接口路径**：`POST /api/v1/brain-c/diagnose`

**接口描述**：鉴别诊断工具（tool_3）- 基于DR.KNOWS的路径检索+路径注入LLM

**请求参数**：
```json
{
  "cdpId": "cdp_123456",
  "patientState": {
    "symptoms": ["胸痛"],
    "vitalSigns": {}
  }
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "ddx": [
      {"disease": "心绞痛", "probability": 0.75, "layer": "primary_hypothesis"},
      {"disease": "急性心肌梗死", "probability": 0.30, "layer": "must_exclude"}
    ],
    "reasoningPaths": [
      {
        "path": "胸痛 → 心血管疾病 → 心绞痛",
        "relevanceScore": 0.85,
        "evidenceStrength": 0.8
      }
    ],
    "engineResults": {
      "kgEngine": {"心绞痛": 0.70},
      "llmEngine": {"心绞痛": 0.75}
    }
  }
}
```

---

#### 8.3.4 tool_4：检查建议工具

**接口路径**：`POST /api/v1/brain-d/plan-workup`

**接口描述**：检查/检验建议与价值评估（tool_4）

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "workupPlan": [
      {
        "testName": "心电图",
        "priority": "high",
        "informationGain": 0.8,
        "purpose": "区分心绞痛和急性心梗"
      }
    ]
  }
}
```

---

#### 8.3.5 tool_5：治疗建议工具

**接口路径**：`POST /api/v1/brain-e/plan-treatment`

**接口描述**：治疗/处置建议工具（tool_5）

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "treatmentPlan": {
      "medications": ["硝酸酯类", "β受体阻滞剂"],
      "nonPharmacological": ["避免剧烈活动", "低盐低脂饮食"],
      "referral": null
    }
  }
}
```

---

#### 8.3.6 tool_6：风险评估工具

**接口路径**：`POST /api/v1/brain-f/assess-risk`

**接口描述**：风险与急症识别工具（tool_6）

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "riskLevel": "L2",
    "severity": "moderate",
    "urgency": "urgent",
    "redFlags": [],
    "reviewPlan": {
      "defaultTime": "3天后",
      "earlyReviewConditions": ["症状加重"],
      "upgradeConditions": ["出现新高危症状"]
    }
  }
}
```

---

#### 8.3.7 tool_7：证据链工具

**接口路径**：`POST /api/v1/brain-g/explain`

**接口描述**：可解释性与证据链工具（tool_7）

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "evidenceChain": [
      {
        "evidence": "胸痛伴活动后加重",
        "type": "support",
        "strength": "strong",
        "affectedDisease": "心绞痛"
      }
    ],
    "reasoningPaths": [
      "胸痛 → 心血管疾病 → 心绞痛"
    ],
    "conclusionPackage": {
      "conclusion": "可确证：心绞痛",
      "mustExcludeStatus": "已排除：急性心肌梗死",
      "keyEvidence": ["症状组合", "体征", "KG路径"],
      "actionAndFollowUp": {
        "immediateActions": ["药物治疗"],
        "reviewTime": "3天后",
        "upgradeConditions": ["症状加重"]
      }
    }
  }
}
```

---

### 8.4 回填与重排规则引擎接口

#### 8.4.1 回填与重排诊断方向

**接口路径**：`POST /api/v1/rerank/rerank-ddx`

**接口描述**：根据新证据重新排序诊断方向，处理证据冲突

**请求参数**：
```json
{
  "cdpId": "cdp_123456",
  "newEvidence": {
    "evidenceName": "心电图ST段抬高",
    "evidenceSource": "线下检查",
    "evidenceResult": "ST段抬高≥0.1mV",
    "evidenceDirection": "支持",
    "affectedDirection": "急性心肌梗死",
    "evidenceStrength": "强证据"
  }
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp_123456",
    "updatedDdx": [
      {
        "diagnosis": "急性心肌梗死",
        "rank": 1,
        "probability": 0.85,
        "previousProbability": 0.30,
        "change": "方向上移",
        "weightAdjustment": "+0.3"
      },
      {
        "diagnosis": "心绞痛",
        "rank": 2,
        "probability": 0.65,
        "previousProbability": 0.75,
        "change": "方向下移",
        "weightAdjustment": "-0.2"
      }
    ],
    "hasConflict": false,
    "conflicts": []
  },
  "timestamp": 1705123456789
}
```

#### 8.4.2 检查证据冲突

**接口路径**：`POST /api/v1/rerank/check-conflicts`

**接口描述**：检查新证据与原有证据是否存在冲突

**请求参数**：
```json
{
  "cdpId": "cdp_123456",
  "newEvidence": {
    "evidenceName": "症状说发热",
    "evidenceResult": "体温正常（36.5℃）",
    "evidenceDirection": "冲突"
  }
}
```

**响应示例**（检测到冲突）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "hasConflict": true,
    "conflicts": [
      {
        "conflictType": "症状与体征冲突",
        "description": "症状说发热，但体温正常",
        "suggestion": "需要重新验证，建议询问患者是否测过体温"
      }
    ],
    "shouldRollback": true,
    "rollbackTargetStage": "阶段4"
  },
  "timestamp": 1705123456789
}
```

---

### 8.5 回退机制引擎接口

#### 8.5.1 检查回退条件

**接口路径**：`POST /api/v1/rollback/check-conditions`

**接口描述**：检查是否满足回退条件

**请求参数**：
```json
{
  "cdpId": "cdp_123456",
  "currentState": {
    "symptomWorsened": true,
    "newSymptomsAppeared": false,
    "evidenceConflict": false,
    "treatmentIneffective": false
  }
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "shouldRollback": true,
    "rollbackConditions": [
      {
        "condition": "症状恶化/出现新高危",
        "rollbackTargetStage": "触发诊断",
        "rollbackReason": "症状突然加重，需要重新评估健康状态",
        "rollbackAction": "回到触发诊断阶段，重新进行健康状态判定"
      }
    ]
  },
  "timestamp": 1705123456789
}
```

#### 8.5.2 执行回退

**接口路径**：`POST /api/v1/rollback/execute`

**接口描述**：执行回退到指定阶段

**请求参数**：
```json
{
  "cdpId": "cdp_123456",
  "rollbackCondition": {
    "condition": "症状恶化/出现新高危",
    "rollbackTargetStage": "触发诊断",
    "rollbackReason": "症状突然加重",
    "rollbackAction": "回到触发诊断阶段"
  }
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp_123456",
    "previousStage": "阶段4",
    "currentStage": "触发诊断",
    "rollbackRecord": {
      "condition": "症状恶化/出现新高危",
      "rollbackReason": "症状突然加重",
      "timestamp": "2025-01-15T10:40:00"
    }
  },
  "timestamp": 1705123456789
}
```

---

### 8.6 CDP管理接口

#### 8.6.1 创建CDP

**接口路径**：`POST /api/v1/cdp`

**接口描述**：创建临床决策包（CDP）

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp_123456",
    "version": 1,
    "cdpStatus": "initial",
    "createdAt": "2025-01-01T10:00:00Z"
  }
}
```

---

#### 8.6.2 获取CDP

**接口路径**：`GET /api/v1/cdp/{cdpId}`

**接口描述**：获取临床决策包（CDP）

---

#### 8.6.3 更新CDP

**接口路径**：`PUT /api/v1/cdp/{cdpId}`

**接口描述**：更新临床决策包（CDP），会自动创建新版本

---

#### 8.6.4 获取CDP版本历史

**接口路径**：`GET /api/v1/cdp/{cdpId}/versions`

**接口描述**：获取CDP的所有版本历史

---

#### 8.6.5 回放CDP演变过程

**接口路径**：`GET /api/v1/cdp/{cdpId}/replay`

**接口描述**：回放CDP从创建到当前状态的演变过程

---

### 8.5 单独引擎诊断（兼容旧接口）

**接口路径**：
- `POST /api/v1/engine/rule-based` - 规则引擎
- `POST /api/v1/engine/knowledge-graph` - 知识图谱引擎（DR.KNOWS核心）
- `POST /api/v1/engine/statistical` - 统计模型引擎
- `POST /api/v1/engine/llm` - 大模型引擎（路径注入）
- `POST /api/v1/engine/differential` - 鉴别诊断引擎

**请求参数**：同融合诊断接口

**响应格式**：各引擎返回各自的结果格式

> **注意**：推荐使用tool_3接口（`/api/v1/tools/tool_3/invoke`），包含完整的DR.KNOWS路径检索+路径注入功能。

---

## 九、检查建议服务接口（workup-planner-service）

### 9.1 生成检查建议

**接口路径**：`POST /api/v1/workup/plan`

**接口描述**：基于当前DDx和已有证据，建议下一步检查，并评估检查的价值

**请求参数**：
```json
{
  "cdpId": "cdp_123456",
  "currentDdx": [
    {
      "diagnosisCode": "CUI编码",
      "diagnosisName": "疾病名称",
      "rank": 1,
      "probability": 0.8
    }
  ],
  "patientState": {
    "symptoms": ["症状列表"],
    "signs": ["体征列表"]
  }
}
```

**响应数据**：
```json
{
  "workupPlan": [
    {
      "testName": "检查名称",
      "purpose": "检查目的",
      "priority": "紧急/重要/可选",
      "expectedGain": 0.8,
      "canConfirm": ["可确认的DDx"],
      "canExclude": ["可排除的DDx"]
    }
  ]
}
```

### 9.2 生成验证计划

**接口路径**：`POST /api/v1/workup/verification-plan`

**接口描述**：针对"最可能方向"和"必须排除方向"制定验证计划

**请求参数**：
```json
{
  "cdpId": "cdp_123456",
  "mostLikelyDirection": {
    "diagnosisName": "疾病名称",
    "probability": 0.8
  },
  "mustExcludeDirection": {
    "diagnosisName": "疾病名称",
    "mustExclude": true
  }
}
```

**响应数据**：
```json
{
  "verificationPlans": [
    {
      "verificationPurpose": "确认/排除/升级判定",
      "verificationAction": {
        "type": "补问/观察/测量/检查或就医",
        "content": "具体动作",
        "priority": "优先级"
      },
      "judgmentStandard": {
        "if_support": "什么结果支持该方向",
        "if_not_support": "什么结果不支持该方向",
        "if_uncertain": "什么结果需要进一步验证"
      },
      "resultBackfillRule": {
        "if_support": "如何更新三层排序（上移/巩固）",
        "if_not_support": "如何更新三层排序（下移/排除）",
        "if_uncertain": "如何更新三层排序（保留/标记需线下排除）"
      }
    }
  ]
}
```

---

## 十、治疗推理服务接口（treatment-engine-service）

### 10.1 生成治疗方案

**接口路径**：`POST /api/v1/treatment/plan`

**接口描述**：基于诊断结果，生成治疗方案和处置建议

**请求参数**：
```json
{
  "cdpId": "cdp_123456",
  "diagnosis": {
    "diagnosisCode": "CUI编码",
    "diagnosisName": "疾病名称",
    "probability": 0.8
  },
  "patientState": {
    "symptoms": ["症状列表"],
    "severity": "严重程度"
  },
  "riskLevel": "L1/L2/L3/L4"
}
```

**响应数据**：
```json
{
  "managementPlan": [
    {
      "type": "对症处理/用药建议/观察/转诊",
      "content": "具体方案",
      "rationale": "方案依据"
    }
  ]
}
```

### 10.2 药物推荐

**接口路径**：`POST /api/v1/treatment/medication`

**接口描述**：推荐药物类别（研发阶段不涉及具体剂量）

**请求参数**：
```json
{
  "diagnosis": "疾病名称",
  "patientState": {
    "allergies": ["过敏史"],
    "medications": ["当前用药"]
  }
}
```

**响应数据**：
```json
{
  "medications": [
    {
      "medicationCategory": "药物类别",
      "rationale": "推荐理由",
      "contraindications": ["禁忌症"]
    }
  ]
}
```

---

## 十一、风险评估服务接口（risk-assessment-service）

### 11.1 风险评估

**接口路径**：`POST /api/v1/risk/assess`

**接口描述**：识别高危情况，评估紧急程度，决定是否需要立即升级处理

**请求参数**：
```json
{
  "cdpId": "cdp_123456",
  "patientState": {
    "symptoms": ["症状列表"],
    "signs": ["体征列表"]
  },
  "ddx": [
    {
      "diagnosisName": "疾病名称",
      "probability": 0.8
    }
  ]
}
```

**响应数据**：
```json
{
  "triage": {
    "riskLevel": "L1/L2/L3/L4",
    "urgency": "极紧急/紧急/一般/非紧急",
    "redFlags": ["红旗信号列表"],
    "upgradeConditions": ["升级处理条件"]
  }
}
```

### 11.2 生成终点结论包

**接口路径**：`POST /api/v1/risk/conclusion-package`

**接口描述**：生成终点结论包（四要素：结论、必须排除项状态、关键依据、行动与随访）

**请求参数**：
```json
{
  "cdpId": "cdp_123456",
  "ddx": [...],
  "evidenceGraph": [...],
  "workupPlan": [...]
}
```

**响应数据**：
```json
{
  "conclusionPackage": {
    "conclusion": {
      "type": "可确证/不可确证",
      "diagnosis": "具体疾病诊断",
      "confidence": 0.8
    },
    "mustExcludeStatus": {
      "excluded": [...],
      "notExcluded": [...],
      "needOfflineExclude": [...]
    },
    "keyEvidence": {
      "positiveEvidence": [...],
      "negativeEvidence": [...],
      "checkResults": [...]
    },
    "actionAndFollowUp": {
      "immediateAction": {...},
      "reviewTimeWindow": {...},
      "upgradeConditions": [...]
    }
  }
}
```

---

## 十二、OCR服务接口（ocr-service）

### 5.1 识别报告

**接口路径**：`POST /api/v1/ocr/recognize`

**接口描述**：OCR识别检查报告图片

**请求类型**：`multipart/form-data`

**请求参数**：
- `file`: 文件（图片或PDF）

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "rawText": "血常规检查报告\n白细胞：6.5 10^9/L\n红细胞：4.5 10^12/L\n...",
    "structuredData": {
      "indicators": [
        {
          "name": "WBC",
          "value": 6.5,
          "unit": "10^9/L",
          "normalRange": "4-10",
          "status": "normal"
        },
        {
          "name": "RBC",
          "value": 4.5,
          "unit": "10^12/L",
          "normalRange": "4.0-5.5",
          "status": "normal"
        }
      ]
    }
  },
  "timestamp": 1705123456789
}
```

---

## 十三、错误码定义

### 6.1 通用错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 6.2 业务错误码

> **权威来源**：请以《AI医生系统-错误处理规范.md》为准（按微服务/工具分段，确保全系统唯一）。

**业务错误码分段（摘要）**：

| 范围 | 归属 | 说明 |
|------|------|------|
| 1000-1099 | health-state-assessment-service（tool_0） | 入口判定/健康状态判定/红旗兜底 |
| 1100-1199 | clinical-parsing-service（tool_1） | 概念识别/归一化/结构化提取/多模态 |
| 1200-1299 | dialog-service（tool_2） | 信息缺口/追问生成/对话上下文 |
| 1300-1399 | workup-planner-service（tool_4） | 信息增益/检查价值/验证计划 |
| 1400-1499 | treatment-engine-service（tool_5） | 处置建议/药物类别/生活方式 |
| 1500-1599 | risk-assessment-service（tool_6） | 风险分级/高危识别/升级规则 |
| 1600-1699 | explanation-service（tool_7） | 证据链/可解释/终点结论包 |
| 1700-1799 | CDP管理（跨服务通用） | CDP创建/更新/版本/回放/回填/回退 |
| 1800-1899 | A路径健康筛查（跨服务通用） | 需求分类/健康档案/分支执行/随访闭环 |
| 2000-2099 | diagnosis-service（主Agent服务，Java） | 流程编排/聚合/下游调用失败封装 |
| 2100-2199 | examination-service（Java） | 检查业务/报告上传/解析/OCR编排 |
| 3000-3999 | diagnosis-engine-service（tool_3） | 规则/知识图谱/融合/路径注入LLM |
| 4000-4999 | ocr-service（Python） | OCR识别/解析/置信度/格式 |
| 5000-5999 | 通用错误（跨服务） | 参数校验/格式/DB/缓存/外部依赖 |
| 6000-6099 | 并发与幂等（跨服务） | 乐观锁/版本冲突/幂等冲突 |
| 6100-6199 | 调用治理（跨服务） | 超时/熔断/限流/降级/重试耗尽 |

---

## 十四、接口调用示例

### 7.1 完整诊断流程示例

```bash
# 1. 开始诊断
curl -X POST http://localhost:8084/api/v1/diagnosis/start \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "diagnosisType": "symptom",
    "symptomInfo": {
      "chiefComplaint": "我最近胸痛"
    }
  }'

# 2. 回答追问
curl -X POST http://localhost:8084/api/v1/diagnosis/diag_123456/answer \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "answer": "3天了"
  }'

# 3. 获取诊断结果
curl -X GET http://localhost:8084/api/v1/diagnosis/diag_123456 \
  -H "Authorization: Bearer {token}"
```

---

## 十五、Swagger文档

建议使用Swagger/OpenAPI生成API文档：

```java
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.aidoctor.diagnosis.controller"))
            .paths(PathSelectors.any())
            .build()
            .apiInfo(apiInfo());
    }
}
```

访问地址：`http://localhost:8084/swagger-ui.html`

---

**文档版本**：v4.0（基于DR.KNOWS的单主Agent + 多工具Tools架构 + 健康状态判定 + 健康筛查流程）  
**创建日期**：2025年1月  
**更新日期**：2025年1月  
**文档定位**：AI医生系统的API接口规范（完整的接口定义和示例）  
**参考文档**：《AI医生系统-系统功能设计.md》、《AI医生系统-技术架构设计.md》  
**设计基础**：基于DR.KNOWS论文，采用单主Agent + 多工具Tools架构设计  

**更新说明**：
1. **新增AI诊断入口判定流程接口**（Step 1-5）：接收用户输入、识别症状、方向澄清、危险信号检查、路径选择
2. **新增健康状态判定接口**（tool_0）：整合Step 1-5，输出工作态判定结果
3. **新增健康筛查流程接口**（A路径，A1-A5）：需求分类、健康档案采集、分支执行、统一结果输出、随访闭环
4. **更新诊断结果接口**：添加终点结论包四要素（结论、必须排除项状态、关键依据、行动与随访）
5. **新增回填与重排接口**：回填与重排诊断方向、检查证据冲突
6. **新增回退机制接口**：检查回退条件、执行回退
7. **完善CDP管理接口**：支持版本控制、版本历史、回放功能

---

## 七、结构化问题清单相关接口

### 7.1 获取结构化问题清单

**接口路径**：`GET /api/v1/diagnosis/{id}/question-list`

**接口描述**：获取诊断的结构化问题清单

**路径参数**：
- `id`: 诊断ID

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diagnosisId": "diag_123456",
    "chiefComplaint": {
      "name": "胸痛",
      "originalText": "我最近胸痛",
      "duration": "3天",
      "onsetMode": "gradual",
      "severity": 6,
      "frequency": "intermittent",
      "location": "胸部正中",
      "features": {
        "trigger": "运动后",
        "relief": "休息后缓解"
      },
      "triageLevel": "L3"
    },
    "accompanyingSymptoms": [
      {
        "name": "气短",
        "startTime": "3天前",
        "severity": 5
      }
    ],
    "keyBackground": {
      "age": 45,
      "gender": "male",
      "medicalHistory": ["高血压"],
      "medicationHistory": ["降压药"],
      "familyHistory": []
    },
    "vitalSigns": {
      "bloodPressure": {"systolic": 130, "diastolic": 85},
      "heartRate": 75,
      "temperature": 36.5
    },
    "informationGaps": {
      "requiredGaps": [],
      "importantGaps": [],
      "optionalGaps": ["lifestyle"]
    },
    "completeness": 0.85
  },
  "timestamp": 1705123456789
}
```

---

## 八、知识查询服务接口（knowledge-query-service）

> **说明**：knowledge-query-service是知识演化与维护系统的在线层服务，提供只读的知识库查询功能，服务于通道1结构化推理。

### 8.1 知识库查询接口

#### 8.1.1 查询主诉知识图谱

**接口**：`GET /api/v1/knowledge/chief-complaint`

**功能**：查询主诉知识图谱，获取主诉相关的诊断候选、差异点等信息

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chiefComplaint | String | 是 | 主诉名称 |
| kgVersion | String | 否 | 知识版本（默认使用当前生产版本） |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "chiefComplaint": "胸痛",
    "knowledgeObjects": [
      {
        "koId": "KO_001",
        "koType": "rule",
        "content": {...},
        "kgVersion": "v2.1",
        "provenance": [...]
      }
    ],
    "kgVersion": "v2.1",
    "source": "knowledge_base"
  },
  "timestamp": 1705123456789
}
```

#### 8.1.2 查询疾病知识图谱

**接口**：`GET /api/v1/knowledge/disease`

**功能**：查询疾病知识图谱，获取疾病详细信息

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| diseaseCui | String | 是 | 疾病CUI编码 |
| kgVersion | String | 否 | 知识版本 |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "diseaseCui": "CUI_001",
    "knowledgeObjects": [...],
    "kgVersion": "v2.1",
    "source": "knowledge_base"
  },
  "timestamp": 1705123456789
}
```

#### 8.1.3 路径检索验证

**接口**：`POST /api/v1/knowledge/path/retrieve`

**功能**：检索多跳推理路径，用于DR.KNOWS路径验证

**请求体**：
```json
{
  "symptomCuis": ["CUI_001", "CUI_002"],
  "maxHops": 4,
  "kgVersion": "v2.1"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "paths": [
      {
        "nodes": [...],
        "relationships": [...],
        "length": 3,
        "kgVersion": "v2.1"
      }
    ],
    "kgVersion": "v2.1"
  },
  "timestamp": 1705123456789
}
```

#### 8.1.4 验证知识库候选

**接口**：`POST /api/v1/knowledge/path/validate`

**功能**：验证知识库候选是否有路径支持

**请求体**：
```json
{
  "kbCandidates": ["CUI_001", "CUI_002"],
  "paths": [...]
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "validated": ["CUI_001"],
    "unvalidated": ["CUI_002"],
    "validationRate": 0.5
  },
  "timestamp": 1705123456789
}
```

#### 8.1.5 获取当前生产版本

**接口**：`GET /api/v1/knowledge/version/current`

**功能**：获取当前生产环境的知识版本

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "kgVersion": "v2.1",
    "releaseId": "v2.1",
    "status": "active"
  },
  "timestamp": 1705123456789
}
```

### 8.2 错误码定义

| 错误码 | 说明 | HTTP状态码 |
|--------|------|-----------|
| 8001 | 知识版本不存在 | 404 |
| 8002 | 知识对象不存在 | 404 |
| 8003 | 路径检索失败 | 500 |
| 8004 | 知识库查询失败 | 500 |

---

## 九、知识运维服务接口（knowledge-ops-service）

> **说明**：knowledge-ops-service是知识演化与维护系统的离线层服务，负责知识的抽取、验证、冲突处理、发布门禁等。

### 9.1 知识提案接口

#### 9.1.1 提交知识演化提案

**接口**：`POST /api/v1/knowledge/proposal`

**功能**：主Agent或离线任务提交知识演化提案

**请求体**：
```json
{
  "trigger": "知识覆盖缺口",
  "diff": {
    "add": [...],
    "modify": [...],
    "delete": [...]
  },
  "requiredTests": ["core_regression", "sampling_set"],
  "riskLevel": "medium",
  "dedupeKey": "knowledge_gap_ICD_I20_0_SYMP_001",
  "cooldownWindow": 3600,
  "evidenceSnapshot": "sha256:abc123..."
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "proposalId": "PROP_20260128_001",
    "status": "pending"
  },
  "timestamp": 1705123456789
}
```

#### 9.1.2 查询知识提案

**接口**：`GET /api/v1/knowledge/proposal/{proposalId}`

**功能**：查询知识提案详情

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "proposalId": "PROP_20260128_001",
    "trigger": "知识覆盖缺口",
    "status": "processing",
    "riskLevel": "medium",
    "createdAt": "2026-01-28T10:00:00Z"
  },
  "timestamp": 1705123456789
}
```

### 9.2 知识对象管理接口

#### 9.2.1 查询知识对象

**接口**：`GET /api/v1/knowledge/object/{koId}`

**功能**：查询知识对象详情（包括Neo4j中的内容和元数据）

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| kgVersion | String | 否 | 知识版本（默认使用当前生产版本） |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "koId": "KO_001",
    "koType": "rule",
    "content": {...},
    "conceptIds": ["CUI_001"],
    "provenance": [...],
    "status": "published",
    "kgVersion": "v2.1",
    "impactScope": ["DDx", "workup"],
    "downstreamBindings": [...]
  },
  "timestamp": 1705123456789
}
```

### 9.3 版本管理接口

#### 9.3.1 查询版本列表

**接口**：`GET /api/v1/knowledge/version/list`

**功能**：查询所有知识版本列表

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 否 | 版本状态（active/deprecated/archived） |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "versions": [
      {
        "releaseId": "v2.1",
        "status": "active",
        "isCurrent": true,
        "createdAt": "2026-01-28T10:00:00Z"
      },
      {
        "releaseId": "v2.0",
        "status": "deprecated",
        "isCurrent": false,
        "createdAt": "2026-01-20T10:00:00Z"
      }
    ]
  },
  "timestamp": 1705123456789
}
```

#### 9.3.2 切换知识版本

**接口**：`POST /api/v1/knowledge/version/switch`

**功能**：切换当前生产版本（需要管理员权限）

**请求体**：
```json
{
  "releaseId": "v2.1",
  "reason": "新版本发布"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "releaseId": "v2.1",
    "status": "active",
    "isCurrent": true
  },
  "timestamp": 1705123456789
}
```

### 9.4 发布门禁接口

#### 9.4.1 评估候选发布包

**接口**：`POST /api/v1/knowledge/publish/evaluate`

**功能**：评估候选发布包，通过四道门禁

**请求体**：
```json
{
  "candidateReleaseId": "RELEASE_20260128_001"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "candidateReleaseId": "RELEASE_20260128_001",
    "passed": true,
    "gateResults": {
      "gate1": {"passed": true},
      "gate2": {"passed": true},
      "gate3": {"passed": true, "evaluationResult": {...}},
      "gate4": {"passed": true, "approvalType": "auto"}
    }
  },
  "timestamp": 1705123456789
}
```

### 9.5 错误码定义

| 错误码 | 说明 | HTTP状态码 |
|--------|------|-----------|
| 9001 | 知识提案不存在 | 404 |
| 9002 | 知识提案已存在 | 409 |
| 9003 | 知识提案处理失败 | 500 |
| 9004 | 发布门禁未通过 | 400 |
| 9005 | 版本切换失败 | 500 |

---

## 十、诊断引擎服务接口（diagnosis-engine-service）

### 10.1 五引擎融合诊断（更新版）

