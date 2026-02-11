# explanation-service - 服务实现方案

> **文档定位**：本文档定义explanation-service（解释生成服务，tool_7）的技术实现方案，包括技术选型、接口设计、数据流、实现步骤等。  
> **参考文档**：
> - 《AI医生系统-业务逻辑详细设计.md》- 业务逻辑（核心参考）
> - 《AI医生系统-技术架构设计.md》及相关子文档- 技术架构
> - 《AI医生系统-项目结构设计.md》- 项目结构
> - 《AI医生系统-数据模型设计.md》- 数据模型
> - 《AI医生系统-API接口规范.md》- API规范
> - 《AI医生系统-错误处理规范.md》- 错误处理规范
> - 《AI医生系统-最终输出格式规范.md》- 输出格式规范
> - 《三个服务LangChain架构开发流程.md》- LangChain架构开发流程

---

## 一、服务概述

> **参考文档**：《AI医生系统-业务逻辑详细设计.md》

### 1.1 服务定位

- **对应工具**：tool_7（解释生成工具）
- **在双通道推理架构中的位置**：通道1 + 通道2（结构化推理通道 + 语言与策略通道）
- **服务职责**：
  1. **证据链构建**：构建完整的证据链，让系统的"结论"能被复核
  2. **推理路径可视化**：可视化推理路径（DR.KNOWS路径）
  3. **终点结论包生成**：生成终点结论包（四要素：结论、必须排除项状态、关键依据、行动与随访）
  4. **自然语言解释生成**：使用LLM生成自然、易懂的解释说明

### 1.2 输入输出

#### 1.2.1 输入数据格式和来源

**输入来源**：
- **上游服务**：`diagnosis-engine-service`（tool_3）提供诊断结果
- **数据格式**：CDP中的相关字段，包含：
  - `ddx`：鉴别诊断候选集（三层分层）
  - `evidence_graph`：证据图结构
  - `reasoning_paths`：推理路径列表
  - `workup_plan`：检查计划
  - `treatment_plan`：治疗方案
  - `risk_assessment`：风险评估结果

**数据获取方式**：
- 通过`diagnosis-service`传递CDP ID
- 从CDP中读取相关字段

#### 1.2.2 输出数据格式和目标

**输出目标**：
- **更新CDP的`evidence_graph`字段**：完整的证据链结构
- **更新CDP的`conclusion_package`字段**：终点结论包（四要素）
- **生成自然语言解释**：用户友好的解释说明

**输出格式**（符合《AI医生系统-最终输出格式规范.md》）：
```json
{
  "evidenceChain": {
    "evidence": [
      {
        "item": "胸痛症状",
        "type": "symptom",
        "strength": "strong",
        "supportingDiseases": ["心绞痛", "心肌梗死"],
        "contradictingDiseases": []
      }
    ],
    "reasoningPaths": [
      "症状：胸痛 → 疾病：心绞痛（置信度：0.85）"
    ]
  },
  "reasoningPaths": [
    {
      "pathId": "path_001",
      "description": "胸痛 → 心绞痛",
      "confidence": 0.85
    }
  ],
  "conclusionPackage": {
    "conclusion": {
      "type": "likely_diagnosis",
      "diagnosis": "心绞痛",
      "confidence": 0.85,
      "severity": "moderate",
      "uncertaintySource": "需要进一步检查确认"
    },
    "mustExcludeStatus": {
      "excluded": [],
      "notExcluded": ["急性心肌梗死"],
      "needOfflineExclude": ["急性心肌梗死"]
    },
    "keyEvidence": [
      {
        "type": "positive",
        "description": "胸痛症状",
        "strength": "strong"
      }
    ],
    "actionAndFollowUp": {
      "immediateAction": {
        "medicalAdvice": "建议尽快就医",
        "examinations": ["心电图", "心肌酶"],
        "treatmentDirection": "抗心绞痛治疗"
      },
      "reviewTimeWindow": {
        "defaultTime": "3-7天",
        "earlyReviewConditions": ["症状加重", "出现新症状"]
      },
      "upgradeConditions": ["持续胸痛", "呼吸困难加重"]
    }
  },
  "naturalLanguageExplanation": "根据您的症状和检查结果，我们考虑您可能患有心绞痛。主要依据包括：1. 胸痛症状... 2. 活动后加重... 建议您尽快就医，进行心电图和心肌酶检查以进一步确认。"
}
```

#### 1.2.3 数据流转关系

**数据流转**（参考《AI医生系统-技术架构设计-CDP数据与状态管理.md》）：
```
diagnosis-engine-service（tool_3）
    ↓ 输出：诊断结果（写入CDP.ddx、CDP.reasoning_paths）
explanation-service（tool_7）
    ↓ 读取：CDP.ddx、CDP.reasoning_paths、CDP.evidence_graph
    ↓ 处理：证据链构建、推理路径可视化、终点结论包生成、自然语言解释生成
    ↓ 输出：证据链、终点结论包、自然语言解释（写入CDP.evidence_graph、CDP.conclusion_package）
前端/用户
    ↓ 展示：可解释的诊断结果
```

### 1.3 业务价值

- **解决什么业务问题**：
  1. 提供可解释的诊断结果，让用户理解诊断依据
  2. 生成完整的证据链，支持医生复核
  3. 生成终点结论包，提供可行动的建议
  4. 提升系统的可信度和透明度

- **业务价值**：
  1. **可解释性**：让用户理解诊断依据，提升信任度
  2. **可审计性**：完整的证据链支持医生复核
  3. **可行动性**：终点结论包提供明确的行动建议
  4. **用户体验**：自然语言解释提升用户体验

---

## 二、技术实现设计

> **参考文档**：
> - 《AI医生系统-技术架构设计.md》及相关子文档
> - 《AI医生系统-技术架构设计-核心技术组件.md》
> - 《AI医生系统-技术架构设计-性能与评估.md》
> - 《三个服务LangChain架构开发流程.md》

### 2.1 技术栈选择

#### 2.1.1 编程语言和框架

- **编程语言**：Python 3.10+
- **Web框架**：FastAPI 0.104.1
  - **选择理由**：
    - 高性能异步框架，支持并发处理
    - 自动生成OpenAPI文档
    - 类型提示支持，代码可维护性强
    - 与Python生态集成良好

#### 2.1.2 核心依赖库

| 依赖库 | 版本 | 用途 |
|--------|------|------|
| **aidoctor_llm** | 0.1.0 | 公共LLM库（LangChain客户端和Prompt模板管理） |
| **langchain** | 0.1.0 | LLM集成框架（通过公共库使用） |
| **langchain-openai** | 0.0.2 | OpenAI API集成（通过公共库使用） |
| **openai** | 1.10.0 | OpenAI官方SDK（通过公共库使用） |
| **jinja2** | 3.1.2 | 模板引擎（通过公共库使用） |
| **pydantic** | 2.5.0 | 数据验证和序列化 |
| **httpx** | 0.25.1 | HTTP客户端，用于服务间调用 |

#### 2.1.3 技术选型理由

1. **Python + FastAPI**：
   - Python生态丰富，适合AI/ML场景
   - FastAPI性能优秀，支持异步并发
   - 与LLM API集成方便

2. **公共LLM库（aidoctor_llm）**：
   - 统一LLM集成，与其他服务（diagnosis-engine-service、dialog-service）共享代码
   - 便于维护和扩展
   - 支持多种LLM后端
   - 参考《三个服务LangChain架构开发流程.md》

### 2.2 核心算法/方法

#### 2.2.1 证据链构建算法

**算法描述**：从CDP中提取证据，构建完整的证据链结构

**核心组件**：
1. **证据提取器（evidence_extractor）**：
   - 从CDP中提取症状、体征、检查结果等证据
   - 证据分类（阳性证据/阴性证据）
   - 证据强度评估

2. **证据链构建器（evidence_chain_builder）**：
   - 构建证据-疾病关联图
   - 计算证据支持强度
   - 生成证据链结构

**算法实现思路**：
```python
# 1. 提取证据
evidence_list = extract_evidence_from_cdp(cdp)

# 2. 证据分类
positive_evidence = [e for e in evidence_list if e.type == "positive"]
negative_evidence = [e for e in evidence_list if e.type == "negative"]

# 3. 构建证据链
evidence_chain = build_evidence_chain(
    positive_evidence=positive_evidence,
    negative_evidence=negative_evidence,
    ddx=cdp.ddx
)

# 4. 计算证据强度
for evidence in evidence_chain:
    evidence.strength = calculate_evidence_strength(evidence, cdp.ddx)
```

#### 2.2.2 推理路径可视化算法

**算法描述**：可视化DR.KNOWS推理路径

**核心组件**：
1. **路径格式化器（path_formatter）**：
   - 格式化推理路径为可读文本
   - 生成路径描述
   - 路径可视化数据生成

2. **路径可视化生成器（path_visualizer）**：
   - 生成可视化数据结构
   - 支持前端渲染

**算法实现思路**：
```python
# 1. 获取推理路径
reasoning_paths = cdp.reasoning_paths

# 2. 格式化路径
formatted_paths = []
for path in reasoning_paths:
    formatted_path = format_path(path)
    formatted_paths.append(formatted_path)

# 3. 生成可视化数据
visualization_data = generate_visualization_data(formatted_paths)
```

#### 2.2.3 终点结论包生成算法

**算法描述**：生成终点结论包（四要素）

**四要素**：
1. **结论（conclusion）**：诊断结论（三层分层）
2. **必须排除项状态（mustExcludeStatus）**：必须排除的高危诊断状态
3. **关键依据（keyEvidence）**：至少三条关键证据
4. **行动与随访（actionAndFollowUp）**：立即行动、复评时间窗、升级触发条件

**算法实现思路**：
```python
# 1. 构建结论
conclusion = build_conclusion(cdp.ddx)

# 2. 构建必须排除项状态
must_exclude_status = build_must_exclude_status(cdp.ddx, cdp.risk_assessment)

# 3. 构建关键依据
key_evidence = build_key_evidence(cdp.evidence_graph, min_count=3)

# 4. 构建行动与随访
action_and_followup = build_action_and_followup(
    conclusion=conclusion,
    workup_plan=cdp.workup_plan,
    treatment_plan=cdp.treatment_plan,
    risk_assessment=cdp.risk_assessment
)

# 5. 组装终点结论包
conclusion_package = ConclusionPackage(
    conclusion=conclusion,
    mustExcludeStatus=must_exclude_status,
    keyEvidence=key_evidence,
    actionAndFollowUp=action_and_followup
)
```

#### 2.2.4 自然语言解释生成算法

**算法描述**：使用LLM生成自然、易懂的解释说明

**核心组件**：
1. **LLM客户端（LangChainLLMClient）**：
   - 使用公共LLM库
   - 调用LLM API生成解释

2. **Prompt模板管理器（PromptTemplateManager）**：
   - 使用公共LLM库
   - 管理解释生成Prompt模板

**算法实现思路**：
```python
# 1. 使用模板管理器格式化Prompt
prompt = template_manager.format_explanation_generation(
    diagnosis=conclusion_package.conclusion,
    evidence=key_evidence,
    reasoning_path=formatted_paths
)

# 2. 调用LLM生成解释
explanation = await llm_client.generate(prompt)

# 3. 后处理（格式化、验证）
formatted_explanation = post_process_explanation(explanation)
```

### 2.3 性能要求

> **参考文档**：《AI医生系统-技术架构设计-性能与评估.md》

#### 2.3.1 响应时间要求

| 功能模块 | 响应时间要求 | 说明 |
|---------|------------|------|
| **证据链构建** | < 1秒 | 结构化数据处理 |
| **推理路径可视化** | < 1秒 | 路径格式化 |
| **终点结论包生成** | < 2秒 | 四要素构建 |
| **自然语言解释生成** | < 5秒 | LLM调用 |
| **整体解释生成流程** | < 8秒 | 端到端响应时间 |

#### 2.3.2 并发处理能力

- **目标并发**：支持10+并发请求
- **峰值并发**：支持20+并发请求
- **并发策略**：异步处理 + 连接池

#### 2.3.3 资源消耗限制

- **内存消耗**：< 2GB（单实例）
- **CPU使用率**：< 70%（正常负载）

### 2.4 关键技术点

#### 2.4.1 关键技术难点

1. **证据链构建的准确性**：
   - **问题**：如何准确提取和分类证据
   - **解决方案**：
     - 使用规则引擎识别证据类型
     - 基于CDP结构化数据提取
     - 证据强度评估算法

2. **终点结论包四要素的完整性**：
   - **问题**：如何确保四要素完整且准确
   - **解决方案**：
     - 使用模板确保结构完整
     - 验证每个要素的必要字段
     - 提供默认值处理

3. **自然语言解释的质量**：
   - **问题**：如何生成高质量的自然语言解释
   - **解决方案**：
     - 使用结构化的Prompt模板
     - 提供足够的上下文信息
     - 后处理和验证

#### 2.4.2 技术风险及应对

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| **LLM API调用失败** | 中 | 降级策略：返回结构化结论包，不生成自然语言解释 |
| **证据链构建失败** | 低 | 使用默认证据链结构 |
| **终点结论包生成失败** | 中 | 使用简化版结论包 |

---

## 三、接口设计

> **参考文档**：
> - 《AI医生系统-API接口规范.md》
> - 《AI医生系统-错误处理规范.md》
> - 《AI医生系统-数据模型设计.md》

### 3.1 API端点定义

#### 3.1.1 统一工具调用接口（新增）

**接口路径**：`POST /api/v1/tools/tool_7/invoke`

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
    "read_fields": ["cdp.ddx", "cdp.evidence_graph", "cdp.workup_plan", "cdp.management_plan"]
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
  "tool_id": "tool_7",
  "status": "success",
  "payload": {},
  "evidence": [
    {
      "source": "kg_path",
      "reference": "evidence_chain",
      "strength": "strong",
      "evidence_name": "证据链"
    }
  ],
  "quality": {
    "confidence": 0.85,
    "completeness": 0.90,
    "accuracy": 0.80
  },
  "suggested_writes": [
    {
      "field_path": "cdp.evidence_graph",
      "value": {},
      "reason": "更新证据图"
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
4. 构建现有服务的请求格式（ExplanationRequest）
5. 调用现有业务逻辑服务（ExplanationService）
6. 将业务结果转换为ToolResult格式
7. 构建evidence引用和suggested_writes建议

**代码位置**：
- 接口实现：`app/api/routes.py` 的 `invoke_tool_7()` 函数
- 数据模型：`app/models/tool_context.py`、`app/models/tool_result.py`
- CDP读取工具：`app/utils/cdp_reader.py`

**参考文档**：
- 《7.接口规范/工具调用协议.md》- 工具调用协议详细规范

#### 3.1.2 生成解释接口（原有接口，保持向后兼容）

**接口路径**：`POST /api/v1/explain`

**请求方法**：POST

**功能描述**：生成完整的可解释性结果，包括证据链、推理路径、终点结论包和自然语言解释

**请求体**：
```json
{
  "cdpId": "cdp_123456",
  "diagnosisResult": {
    "ddx": {
      "primaryHypothesis": [...],
      "majorAlternatives": [...],
      "mustExclude": [...]
    }
  }
}
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "evidenceChain": {
      "evidence": [
        {
          "item": "胸痛症状",
          "type": "symptom",
          "strength": "strong",
          "supportingDiseases": ["心绞痛"],
          "contradictingDiseases": []
        }
      ],
      "reasoningPaths": ["症状：胸痛 → 疾病：心绞痛（置信度：0.85）"]
    },
    "reasoningPaths": [
      {
        "pathId": "path_001",
        "description": "胸痛 → 心绞痛",
        "confidence": 0.85
      }
    ],
    "conclusionPackage": {
      "conclusion": {
        "type": "likely_diagnosis",
        "diagnosis": "心绞痛",
        "confidence": 0.85
      },
      "mustExcludeStatus": {
        "excluded": [],
        "notExcluded": ["急性心肌梗死"]
      },
      "keyEvidence": [
        {
          "type": "positive",
          "description": "胸痛症状",
          "strength": "strong"
        }
      ],
      "actionAndFollowUp": {
        "immediateAction": {
          "medicalAdvice": "建议尽快就医",
          "examinations": ["心电图", "心肌酶"]
        }
      }
    },
    "naturalLanguageExplanation": "根据您的症状和检查结果，我们考虑您可能患有心绞痛..."
  },
  "timestamp": 1705123456789
}
```

#### 3.1.2 生成证据链接口

**接口路径**：`POST /api/v1/explain/evidence-chain`

**请求方法**：POST

**功能描述**：仅生成证据链

#### 3.1.3 生成终点结论包接口

**接口路径**：`POST /api/v1/explain/conclusion-package`

**请求方法**：POST

**功能描述**：仅生成终点结论包

#### 3.1.4 生成自然语言解释接口

**接口路径**：`POST /api/v1/explain/natural-language`

**请求方法**：POST

**功能描述**：仅生成自然语言解释

### 3.2 请求/响应模型

#### 3.2.1 请求模型定义

**ExplanationRequest**：
```python
class ExplanationRequest(BaseModel):
    cdpId: str
    diagnosisResult: Optional[Dict[str, Any]] = None
```

#### 3.2.2 响应模型定义

**ExplanationResponse**：
```python
class ExplanationResponse(BaseModel):
    evidenceChain: EvidenceChain
    reasoningPaths: List[Dict[str, Any]]
    conclusionPackage: ConclusionPackage
    naturalLanguageExplanation: Optional[str] = None
```

### 3.3 错误码定义

> **参考文档**：《AI医生系统-错误处理规范.md》

**错误码范围**：1600-1699（explanation-service）

| 错误码 | HTTP状态码 | 错误描述 | 说明 |
|--------|-----------|---------|------|
| **1600** | 500 | 解释生成服务内部错误 | 系统内部错误 |
| **1601** | 500 | 证据链构建失败 | 证据链构建算法失败 |
| **1602** | 500 | 推理路径可视化失败 | 路径可视化失败 |
| **1603** | 500 | 终点结论包生成失败 | 结论包生成失败 |
| **1604** | 500 | 自然语言解释生成失败 | LLM调用失败 |
| **1605** | 400 | CDP不存在 | CDP ID不存在 |
| **1606** | 400 | 诊断结果数据不完整 | 缺少必要的诊断数据 |

### 3.4 错误处理策略

1. **LLM调用失败**：降级策略，返回结构化结论包，不生成自然语言解释
2. **证据链构建失败**：使用默认证据链结构
3. **终点结论包生成失败**：返回简化版结论包

---

## 四、数据流设计

> **参考文档**：
> - 《AI医生系统-数据模型设计.md》
> - 《AI医生系统-技术架构设计-CDP数据与状态管理.md》
> - 《AI医生系统-最终输出格式规范.md》

### 4.1 数据输入来源

#### 4.1.1 上游服务

- **diagnosis-engine-service**（tool_3）：
  - 提供诊断结果（DDx、推理路径）

#### 4.1.2 数据格式

**CDP相关字段结构**：
```json
{
  "ddx": {
    "primary_hypothesis": [...],
    "major_alternatives": [...],
    "must_exclude": [...]
  },
  "reasoning_paths": [...],
  "evidence_graph": {...},
  "workup_plan": {...},
  "treatment_plan": {...},
  "risk_assessment": {...}
}
```

### 4.2 数据处理流程

#### 4.2.1 数据处理步骤

1. **读取CDP数据**：
   - 从CDP中读取诊断结果、推理路径、证据图等

2. **构建证据链**：
   - 提取证据
   - 证据分类
   - 构建证据链结构

3. **可视化推理路径**：
   - 格式化推理路径
   - 生成可视化数据

4. **生成终点结论包**：
   - 构建结论
   - 构建必须排除项状态
   - 构建关键依据
   - 构建行动与随访

5. **生成自然语言解释**：
   - 使用LLM生成解释
   - 后处理和验证

6. **更新CDP**：
   - 更新`evidence_graph`字段
   - 更新`conclusion_package`字段

### 4.3 数据输出格式

#### 4.3.1 输出数据结构

**CDP.evidence_graph结构**：
```json
{
  "evidence_chain": {
    "evidence": [...],
    "reasoning_paths": [...]
  }
}
```

**CDP.conclusion_package结构**：
```json
{
  "conclusion": {...},
  "must_exclude_status": {...},
  "key_evidence": [...],
  "action_and_followup": {...}
}
```

---

## 五、依赖关系

### 5.1 依赖的其他服务

#### 5.1.1 上游服务

| 服务 | 依赖关系 | 调用方式 | 说明 |
|------|---------|---------|------|
| **diagnosis-engine-service** | 弱依赖 | 读取CDP | 读取诊断结果 |
| **diagnosis-service** | 强依赖 | 同步调用 | 获取CDP数据，更新CDP |

### 5.2 依赖的外部资源

#### 5.2.1 公共LLM库

- **aidoctor_llm**：
  - **用途**：LLM客户端和Prompt模板管理
  - **安装方式**：`-e ../common/aidoctor_llm`
  - **参考文档**：《三个服务LangChain架构开发流程.md》

#### 5.2.2 外部API

- **LLM API**（通过公共库）：
  - **用途**：生成自然语言解释
  - **调用方式**：通过LangChainLLMClient
  - **超时设置**：30秒
  - **重试机制**：最多重试3次

### 5.3 依赖管理

#### 5.3.1 依赖版本管理

- **requirements.txt**：固定版本号
- **公共LLM库**：使用 `-e ../common/aidoctor_llm`（开发模式安装）

---

## 六、实现步骤

> **参考文档**：
> - 《AI医生系统-项目结构设计.md》
> - 《AI医生系统-技术架构设计-项目实现与部署.md》
> - 《三个服务LangChain架构开发流程.md》

### 6.1 分阶段实现计划

#### Phase 1: 基础框架搭建（1周）

**目标**：搭建服务基础框架，集成公共LLM库

**任务清单**：
- [ ] 创建服务目录结构（参考项目结构设计）
- [ ] 配置FastAPI框架
- [ ] 集成公共LLM库（aidoctor_llm）
- [ ] 实现基础工具类（异常处理、日志、配置）
- [ ] 实现健康检查接口
- [ ] 编写基础单元测试

**目录结构**：
```
explanation-service/
├── app/
│   ├── main.py                 # FastAPI应用入口
│   ├── api/
│   │   └── routes.py           # API路由
│   ├── services/
│   │   └── explanation_service.py # 解释生成服务
│   ├── builders/
│   │   ├── evidence_chain_builder.py # 证据链构建器
│   │   ├── conclusion_package_builder.py # 终点结论包构建器
│   │   └── path_visualizer.py  # 推理路径可视化器
│   ├── models/
│   │   ├── request.py          # 请求模型
│   │   └── response.py         # 响应模型
│   ├── utils/
│   │   ├── exceptions.py       # 异常处理
│   │   ├── llm_client.py      # LLM客户端（从公共库导入）
│   │   └── prompt_manager.py  # Prompt管理器（从公共库导入）
│   └── config/
│       └── settings.py        # 配置管理
├── requirements.txt            # 包含: -e ../common/aidoctor_llm
├── Dockerfile
└── README.md
```

**注意**：LLM客户端和Prompt管理器使用公共库（`aidoctor_llm`），参考《三个服务LangChain架构开发流程.md》

#### Phase 2: 证据链构建实现（1周）

**目标**：实现证据链构建功能

**任务清单**：
- [ ] 实现证据提取器（evidence_extractor.py）
  - [ ] 从CDP提取证据
  - [ ] 证据分类（阳性/阴性）
  - [ ] 证据强度评估
- [ ] 实现证据链构建器（evidence_chain_builder.py）
  - [ ] 构建证据-疾病关联图
  - [ ] 计算证据支持强度
  - [ ] 生成证据链结构
- [ ] 编写单元测试（覆盖率≥80%）

#### Phase 3: 推理路径可视化实现（1周）

**目标**：实现推理路径可视化功能

**任务清单**：
- [ ] 实现路径格式化器（path_formatter.py）
  - [ ] 格式化推理路径为可读文本
  - [ ] 生成路径描述
- [ ] 实现路径可视化生成器（path_visualizer.py）
  - [ ] 生成可视化数据结构
  - [ ] 支持前端渲染
- [ ] 编写单元测试

#### Phase 4: 终点结论包生成实现（1周）

**目标**：实现终点结论包生成功能（四要素）

**任务清单**：
- [ ] 实现结论构建器（conclusion_builder.py）
  - [ ] 从DDx构建结论
  - [ ] 三层分层处理
- [ ] 实现必须排除项状态构建器（exclusion_status_builder.py）
  - [ ] 识别必须排除的高危诊断
  - [ ] 构建排除状态
- [ ] 实现关键依据构建器（key_evidence_builder.py）
  - [ ] 提取关键证据（至少三条）
  - [ ] 证据优先级排序
- [ ] 实现行动与随访构建器（action_builder.py）
  - [ ] 构建立即行动建议
  - [ ] 构建复评时间窗
  - [ ] 构建升级触发条件
- [ ] 实现终点结论包构建器（conclusion_package_builder.py）
  - [ ] 组装四要素
  - [ ] 验证完整性
- [ ] 编写单元测试（覆盖率≥80%）

#### Phase 5: 自然语言解释生成实现（1周）

**目标**：使用LLM生成自然语言解释

**任务清单**：
- [ ] 集成公共LLM库
  - [ ] 使用LangChainLLMClient
  - [ ] 使用PromptTemplateManager
- [ ] 设计解释生成Prompt模板
  - [ ] 结论解释模板
  - [ ] 证据说明模板
  - [ ] 行动建议模板
- [ ] 实现解释生成器（explanation_generator.py）
  - [ ] 调用LLM生成解释
  - [ ] 后处理和验证
  - [ ] 降级策略（LLM失败时返回结构化结论包）
- [ ] 编写单元测试

#### Phase 6: 接口实现和集成（1周）

**目标**：实现所有API接口，完成服务集成

**任务清单**：
- [ ] 实现所有API接口（routes.py）
  - [ ] 生成解释接口
  - [ ] 生成证据链接口
  - [ ] 生成终点结论包接口
  - [ ] 生成自然语言解释接口
- [ ] 实现请求/响应模型验证
- [ ] 实现错误处理（遵循错误处理规范）
- [ ] 实现API文档（Swagger/OpenAPI）
- [ ] 编写集成测试

#### Phase 7: 测试与优化（1周）

**目标**：完善测试，性能优化

**任务清单**：
- [ ] 完善单元测试（覆盖率≥80%）
- [ ] 完善集成测试
- [ ] 性能测试
- [ ] 优化LLM调用性能
- [ ] 编写测试报告

### 6.2 优先级排序

- **P0（必须）**：
  - 证据链构建
  - 终点结论包生成（四要素）
  - 基础API接口

- **P1（重要）**：
  - 推理路径可视化
  - 自然语言解释生成
  - 完整API接口

- **P2（可选）**：
  - 性能优化
  - 高级可视化功能

### 6.3 里程碑定义

- **Milestone 1**：基础框架完成（Phase 1）
  - 服务可启动
  - 公共LLM库集成完成
  - 健康检查接口正常

- **Milestone 2**：证据链构建完成（Phase 2）
  - 证据链构建功能正常
  - 证据提取和分类正确

- **Milestone 3**：推理路径可视化完成（Phase 3）
  - 路径可视化功能正常

- **Milestone 4**：终点结论包生成完成（Phase 4）
  - 四要素构建完成
  - 结论包结构完整

- **Milestone 5**：自然语言解释生成完成（Phase 5）
  - LLM解释生成功能正常
  - 降级策略实现

- **Milestone 6**：接口实现和集成完成（Phase 6）
  - 所有API接口实现完成
  - 集成测试通过

- **Milestone 7**：测试与优化完成（Phase 7）
  - 测试覆盖率达标
  - 性能测试通过

---

## 七、测试策略

### 7.1 单元测试计划

#### 7.1.1 测试覆盖范围

- **核心算法**：证据链构建、终点结论包生成、自然语言解释生成
- **工具类**：异常处理、日志、配置
- **数据模型**：请求/响应模型验证

#### 7.1.2 测试用例设计

**证据链构建测试**：
- 测试证据提取
- 测试证据分类
- 测试证据链构建

**终点结论包生成测试**：
- 测试四要素构建
- 测试结论包完整性验证
- 测试边界情况

**自然语言解释生成测试**：
- 测试LLM调用
- 测试Prompt格式化
- 测试降级策略

#### 7.1.3 Mock策略

- **LLM Mock**：使用Mock LLM客户端
- **CDP Mock**：使用Mock CDP数据

### 7.2 集成测试计划

#### 7.2.1 服务间集成测试

- **与diagnosis-service集成**：
  - 测试CDP读写
  - 测试数据流转

#### 7.2.2 端到端测试

- **完整解释生成流程测试**：
  - 从CDP读取数据
  - 生成解释
  - 更新CDP
  - 验证结果

### 7.3 性能测试计划

#### 7.3.1 性能测试指标

- **响应时间**：
  - 证据链构建：< 1秒
  - 终点结论包生成：< 2秒
  - 自然语言解释生成：< 5秒
  - 整体流程：< 8秒

- **并发处理能力**：
  - 目标并发：10+
  - 峰值并发：20+

---

## 八、风险评估

### 8.1 技术风险

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| **LLM API调用失败** | 中 | 降级策略：返回结构化结论包 |
| **证据链构建失败** | 低 | 使用默认证据链结构 |
| **终点结论包生成失败** | 中 | 返回简化版结论包 |

### 8.2 业务风险

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| **解释质量不足** | 中 | Prompt优化、后处理验证 |
| **结论包不完整** | 低 | 模板验证、完整性检查 |

---

## 九、后续优化方向

### 9.1 性能优化

- **LLM调用优化**：缓存、批量处理
- **证据链构建优化**：索引、缓存

### 9.2 功能扩展

- **多语言支持**：支持多语言解释生成
- **个性化解释**：根据用户背景生成个性化解释

---

**文档版本**：v1.0  
**创建日期**：2025年1月  
**维护人员**：开发团队

