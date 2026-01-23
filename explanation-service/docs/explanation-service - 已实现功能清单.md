# explanation-service - 已实现功能清单

本文档记录了 `explanation-service`（解释生成服务，脑区G）的已实现功能。

**文档版本**: v1.0  
**最后更新**: 2025-01  
**服务状态**: ✅ 核心功能完整实现，可用于生产环境

---

## 📋 目录

- [核心功能实现](#核心功能实现)
- [API接口](#api接口)
- [数据模型](#数据模型)
- [异常处理](#异常处理)
- [技术实现](#技术实现)
- [测试覆盖](#测试覆盖)
- [使用说明](#使用说明)

---

## ✅ 核心功能实现

### 1. 证据链构建（P0模块）

**实现状态**: ✅ 完整实现

**功能描述**:
- 从CDP中提取证据，构建完整的证据链结构
- 让系统的"结论"能被复核

**实现方式**:
- 使用证据链构建规则库
- 代码位置: `app/builders/evidence_chain_builder.py`

#### 1.1 证据提取

**功能描述**:
- 从CDP的patient_state中提取症状、体征、检查结果
- 从CDP的evidence_graph中提取已有证据

**提取来源**:
- `patient_state.symptoms`: 症状列表
- `patient_state.signs`: 体征字典
- `patient_state.examination_results`: 检查结果列表
- `evidence_graph.evidence`: 已有证据列表

**实现逻辑**:
- 遍历所有证据源
- 提取证据项、类型、分类（阳性/阴性）、值

#### 1.2 证据分类

**功能描述**:
- 将证据分类为阳性证据和阴性证据
- 根据证据的value和category判断

**分类规则**:
- `category == "positive"` → 阳性证据
- `category == "negative"` → 阴性证据

#### 1.3 证据强度评估

**功能描述**:
- 计算证据支持强度（strong/medium/weak）

**评估规则**:
- 症状 + 阳性 → `strong`
- 检查结果 + 阳性 → `strong`
- 体征 + 阳性 → `medium`
- 其他 → `weak`

#### 1.4 证据-疾病关联构建

**功能描述**:
- 确定证据支持的疾病列表
- 确定证据反对的疾病列表

**关联逻辑**:
- 从DDx的primary_hypothesis和major_alternatives中提取疾病
- 阳性证据 → 支持相关疾病
- 阴性证据 → 反对相关疾病

#### 1.5 推理路径文本生成

**功能描述**:
- 生成可读的推理路径文本描述

**生成逻辑**:
- 优先从CDP.reasoning_paths中读取
- 如果没有，从症状和DDx生成简单路径
- 格式：`"症状：{symptom} → 疾病：{disease}（置信度：{confidence}）"`

---

### 2. 推理路径可视化（P0模块）

**实现状态**: ✅ 完整实现

**功能描述**:
- 可视化DR.KNOWS推理路径
- 生成前端可渲染的可视化数据结构

**实现方式**:
- 代码位置: `app/builders/path_visualizer.py`

#### 2.1 路径格式化

**功能描述**:
- 格式化推理路径为统一的数据结构

**数据结构**:
```json
{
  "pathId": "path_001",
  "description": "胸痛 → 心绞痛",
  "confidence": 0.85,
  "nodes": [
    {"type": "symptom", "name": "胸痛"},
    {"type": "disease", "name": "心绞痛"}
  ],
  "edges": [
    {"from": "symptom", "to": "disease", "weight": 0.85}
  ]
}
```

#### 2.2 路径可视化数据生成

**功能描述**:
- 生成包含节点和边的可视化数据
- 支持前端图形化渲染

**生成逻辑**:
- 从CDP.reasoning_paths中提取路径信息
- 如果没有，从症状和DDx生成默认路径
- 最多返回5条路径

---

### 3. 终点结论包生成（P0模块）

**实现状态**: ✅ 完整实现

**功能描述**:
- 生成终点结论包（四要素）
- 提供可行动的建议

**实现方式**:
- 代码位置: `app/builders/conclusion_package_builder.py`

#### 3.1 结论构建

**功能描述**:
- 从DDx构建诊断结论
- 三层分层处理

**结论类型**:
- **likely_diagnosis**: 很可能诊断（置信度 ≥ 0.8）
- **possible_diagnosis**: 可能诊断（置信度 ≥ 0.5）
- **uncertain**: 不确定（置信度 < 0.5）

**结论字段**:
- `type`: 结论类型
- `diagnosis`: 诊断名称
- `confidence`: 置信度（0.0-1.0）
- `severity`: 严重程度（moderate/high/low）
- `uncertaintySource`: 不确定性来源

#### 3.2 必须排除项状态构建

**功能描述**:
- 识别必须排除的高危诊断状态

**状态分类**:
- **excluded**: 已排除的高危诊断
- **notExcluded**: 未排除的高危诊断（需要关注）
- **needOfflineExclude**: 需要线下排除的高危诊断

**数据来源**:
- `ddx.must_exclude`: 必须排除的疾病列表
- `risk_assessment.high_risk_diseases`: 高危疾病列表

#### 3.3 关键依据构建

**功能描述**:
- 提取关键证据（至少3条）
- 证据优先级排序

**提取逻辑**:
- 优先从patient_state.symptoms中提取（最多3条）
- 从evidence_graph.evidence中补充
- 如果不足，添加默认证据

**证据字段**:
- `type`: 证据类型（positive/negative）
- `description`: 证据描述
- `strength`: 证据强度（strong/medium/weak）

#### 3.4 行动与随访构建

**功能描述**:
- 构建立即行动建议
- 构建复评时间窗
- 构建升级触发条件

**立即行动（immediateAction）**:
- `medicalAdvice`: 医疗建议（如"建议尽快就医"）
- `examinations`: 建议检查项列表（从workup_plan中提取）
- `treatmentDirection`: 治疗方向（从treatment_plan中提取）

**复评时间窗（reviewTimeWindow）**:
- `defaultTime`: 默认复评时间（如"3-7天"）
- `earlyReviewConditions`: 提前复评条件（如"症状加重"）

**升级触发条件（upgradeConditions）**:
- 根据风险等级调整
- 高风险（L3/L4）→ 缩短复评时间，增加升级条件

---

### 4. 自然语言解释生成（P1模块）

**实现状态**: ✅ 完整实现（带降级策略）

**功能描述**:
- 使用LLM生成自然、易懂的解释说明
- 提升用户体验和系统透明度

**实现方式**:
- 使用公共LLM库（aidoctor_llm）
- 代码位置: `app/builders/explanation_generator.py`

#### 4.1 LLM客户端集成

**功能描述**:
- 集成公共LLM库（LangChainLLMClient）
- 支持多种LLM后端（OpenAI、ChatGLM、Ollama、自定义API）

**初始化逻辑**:
- 从环境变量读取LLM配置
- 如果初始化失败，使用降级策略（不生成自然语言解释）

#### 4.2 Prompt模板管理

**功能描述**:
- 使用公共Prompt模板管理器
- 格式化解释生成Prompt

**Prompt内容**:
- 诊断结果（名称、置信度、严重程度）
- 关键证据列表
- 推理路径描述
- 行动建议（医疗建议、检查项）

#### 4.3 解释生成

**功能描述**:
- 调用LLM生成自然语言解释
- 后处理和验证

**生成流程**:
1. 格式化Prompt（包含诊断、证据、路径、建议）
2. 调用LLM API生成解释
3. 后处理（去除多余空白、限制长度）

**降级策略**:
- LLM调用失败 → 返回None，不阻塞主流程
- 服务仍可返回结构化结论包

#### 4.4 解释文本后处理

**功能描述**:
- 处理生成的解释文本
- 确保文本质量和长度

**处理逻辑**:
- 去除多余空白
- 限制长度（最多500字）
- 格式化输出

---

### 5. 解释服务核心逻辑

**实现状态**: ✅ 完整实现

**功能描述**:
- 协调各个构建器，完成完整的解释生成流程
- 管理CDP数据获取和更新

**实现方式**:
- 代码位置: `app/services/explanation_service.py`

#### 5.1 CDP数据获取

**功能描述**:
- 从diagnosis-service获取CDP数据

**获取方式**:
- HTTP GET请求: `{DIAGNOSIS_SERVICE_URL}/api/v1/diagnosis/cdp/{cdpId}`
- 解析响应，提取data字段

**容错处理**:
- 如果获取失败，抛出CDPNotFoundException

#### 5.2 完整解释生成流程

**功能描述**:
- 协调所有构建器，生成完整的可解释性结果

**流程步骤**:
1. 获取CDP数据
2. 构建证据链（EvidenceChainBuilder）
3. 可视化推理路径（PathVisualizer）
4. 生成终点结论包（ConclusionPackageBuilder）
5. 生成自然语言解释（ExplanationGenerator，可选）
6. 更新CDP（写入evidence_graph和conclusion_package）
7. 构建响应

#### 5.3 CDP数据更新

**功能描述**:
- 将生成的证据链和结论包写入CDP

**更新字段**:
- `evidence_graph.evidence_chain`: 证据链结构
- `conclusion_package`: 终点结论包（四要素）

**更新方式**:
- HTTP POST请求: `{DIAGNOSIS_SERVICE_URL}/api/v1/diagnosis/cdp/{cdpId}/update`
- 容错处理：更新失败不阻塞主流程（记录警告日志）

---

## 🔌 API接口

### 核心接口

#### 1. 生成完整解释接口

**接口路径**: `POST /api/v1/explain`

**功能描述**: 生成完整的可解释性结果，包括证据链、推理路径、终点结论包和自然语言解释

**请求格式**:
```json
{
  "cdpId": "cdp_123456",
  "diagnosisResult": {
    "ddx": {
      "primary_hypothesis": [
        {
          "disease": "心绞痛",
          "confidence": 0.85
        }
      ],
      "major_alternatives": [],
      "must_exclude": []
    }
  }
}
```

**响应格式**:
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
      "reasoningPaths": [
        "症状：胸痛 → 疾病：心绞痛（置信度：0.85）"
      ]
    },
    "reasoningPaths": [
      {
        "pathId": "path_001",
        "description": "胸痛 → 心绞痛",
        "confidence": 0.85,
        "nodes": [
          {"type": "symptom", "name": "胸痛"},
          {"type": "disease", "name": "心绞痛"}
        ],
        "edges": [
          {"from": "symptom", "to": "disease", "weight": 0.85}
        ]
      }
    ],
    "conclusionPackage": {
      "conclusion": {
        "type": "likely_diagnosis",
        "diagnosis": "心绞痛",
        "confidence": 0.85,
        "severity": "moderate",
        "uncertaintySource": null
      },
      "mustExcludeStatus": {
        "excluded": [],
        "notExcluded": ["急性心肌梗死"],
        "needOfflineExclude": []
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
  },
  "timestamp": 1705123456789
}
```

#### 2. 生成证据链接口

**接口路径**: `POST /api/v1/explain/evidence-chain`

**功能描述**: 仅生成证据链

**请求格式**: 同完整解释接口

**响应格式**: 返回EvidenceChain对象

#### 3. 生成终点结论包接口

**接口路径**: `POST /api/v1/explain/conclusion-package`

**功能描述**: 仅生成终点结论包

**请求格式**: 同完整解释接口

**响应格式**: 返回ConclusionPackage对象

#### 4. 生成自然语言解释接口

**接口路径**: `POST /api/v1/explain/natural-language`

**功能描述**: 仅生成自然语言解释

**请求格式**: 同完整解释接口

**响应格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "naturalLanguageExplanation": "根据您的症状和检查结果..."
  },
  "timestamp": 1705123456789
}
```

### 功能特性

- ✅ 统一响应格式（code, message, data, timestamp）
- ✅ 参数验证（Pydantic模型）
- ✅ 错误处理（统一异常处理、错误码）
- ✅ 日志记录（结构化日志）
- ✅ CORS支持
- ✅ API文档（Swagger UI: `/docs`）

### 错误码

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 1600 | 解释生成服务内部错误 | 500 |
| 1601 | 证据链构建失败 | 500 |
| 1602 | 推理路径可视化失败 | 500 |
| 1603 | 终点结论包生成失败 | 500 |
| 1604 | 自然语言解释生成失败 | 500 |
| 1605 | CDP不存在 | 400 |
| 1606 | 诊断结果数据不完整 | 400 |

---

## 📊 数据模型

### 请求模型

#### ExplanationRequest

**位置**: `app/models/request.py`

**字段**:
- `cdpId` (str, 必需): CDP ID
- `diagnosisResult` (Dict[str, Any], 可选): 诊断结果数据

### 响应模型

#### Evidence

**位置**: `app/models/response.py`

**字段**:
- `item` (str): 证据项
- `type` (str): 证据类型（symptom/sign/examination）
- `strength` (str): 证据强度（strong/medium/weak）
- `supportingDiseases` (List[str]): 支持的疾病列表
- `contradictingDiseases` (List[str]): 反对的疾病列表

#### EvidenceChain

**字段**:
- `evidence` (List[Evidence]): 证据列表
- `reasoningPaths` (List[str]): 推理路径文本列表

#### ReasoningPath

**字段**:
- `pathId` (str): 路径ID
- `description` (str): 路径描述
- `confidence` (float): 置信度
- `nodes` (List[Dict[str, Any]]): 路径节点
- `edges` (List[Dict[str, Any]]): 路径边

#### ConclusionPackage

**字段**:
- `conclusion` (Dict[str, Any]): 结论
- `mustExcludeStatus` (Dict[str, Any]): 必须排除项状态
- `keyEvidence` (List[Dict[str, Any]]): 关键依据
- `actionAndFollowUp` (Dict[str, Any]): 行动与随访

#### ExplanationResponse

**字段**:
- `evidenceChain` (EvidenceChain): 证据链
- `reasoningPaths` (List[ReasoningPath]): 推理路径列表
- `conclusionPackage` (ConclusionPackage): 终点结论包
- `naturalLanguageExplanation` (Optional[str]): 自然语言解释

---

## ⚠️ 异常处理

### 异常类定义

**位置**: `app/utils/specific_exceptions.py`

#### EvidenceChainConstructionFailedException (1601)
- **触发条件**: 证据链构建失败
- **处理方式**: 抛出异常，返回错误响应

#### ReasoningPathVisualizationFailedException (1602)
- **触发条件**: 推理路径可视化失败
- **处理方式**: 抛出异常，返回错误响应

#### ConclusionPackageGenerationFailedException (1603)
- **触发条件**: 终点结论包生成失败
- **处理方式**: 抛出异常，返回错误响应

#### NaturalLanguageExplanationFailedException (1604)
- **触发条件**: 自然语言解释生成失败
- **处理方式**: 降级策略，返回None，不阻塞主流程

#### CDPNotFoundException (1605)
- **触发条件**: CDP不存在
- **处理方式**: 抛出异常，返回400错误

#### DiagnosisDataIncompleteException (1606)
- **触发条件**: 诊断结果数据不完整
- **处理方式**: 抛出异常，返回400错误

### 异常处理机制

**位置**: `app/utils/exceptions.py`

- ✅ 统一异常处理器（setup_exception_handlers）
- ✅ HTTP状态码映射
- ✅ 错误响应格式统一
- ✅ 日志记录

---

## 🔧 技术实现

### 技术栈

- **编程语言**: Python 3.10+
- **Web框架**: FastAPI 0.104.1
- **LLM库**: aidoctor_llm (公共库)
- **HTTP客户端**: httpx 0.25.1
- **数据验证**: Pydantic 2.5.0

### 依赖管理

**位置**: `requirements.txt`

- ✅ 公共LLM库依赖（`-e ../common/aidoctor_llm`）
- ✅ 所有依赖版本固定

### 配置管理

**位置**: `app/config/settings.py`

**配置项**:
- 服务配置（名称、版本、调试模式）
- LLM配置（后端、模型、温度、超时等）
- 诊断服务URL配置
- 环境变量支持

---

## 🧪 测试覆盖

### 已覆盖的测试场景

#### 1. 完整解释生成
- ✅ 输入：有效的cdpId和诊断结果
- ✅ 预期：返回完整的证据链、推理路径、结论包、自然语言解释

#### 2. 证据链生成
- ✅ 输入：有效的cdpId
- ✅ 预期：返回证据链（包含证据列表和推理路径文本）

#### 3. 推理路径可视化
- ✅ 输入：CDP中包含reasoning_paths
- ✅ 预期：返回格式化的推理路径列表

#### 4. 终点结论包生成
- ✅ 输入：有效的cdpId和DDx数据
- ✅ 预期：返回四要素完整的结论包

#### 5. 自然语言解释生成
- ✅ 输入：完整的结论包和证据链
- ✅ 预期：返回自然语言解释（如果LLM可用）

#### 6. 降级策略
- ✅ 输入：LLM不可用或调用失败
- ✅ 预期：返回结构化结论包，naturalLanguageExplanation为None

#### 7. 错误处理
- ✅ CDP不存在 → 错误码1605
- ✅ 参数验证失败 → 错误码5001
- ✅ 证据链构建失败 → 错误码1601

---

## 📊 功能实现统计

### 实现完成度

| 功能模块 | 实现状态 | 完成度 |
|---------|---------|--------|
| 证据链构建 | ✅ 完整实现 | 100% |
| 推理路径可视化 | ✅ 完整实现 | 100% |
| 终点结论包生成 | ✅ 完整实现 | 100% |
| 自然语言解释生成 | ✅ 完整实现 | 100% |
| 解释服务核心逻辑 | ✅ 完整实现 | 100% |
| API接口 | ✅ 完整实现 | 100% |
| 数据模型 | ✅ 完整实现 | 100% |
| 异常处理 | ✅ 完整实现 | 100% |
| 配置管理 | ✅ 完整实现 | 100% |

### 技术特点

| 特性 | 实现状态 | 说明 |
|------|---------|------|
| 公共LLM库集成 | ✅ | 使用aidoctor_llm，与其他服务保持一致 |
| 降级策略 | ✅ | LLM失败时不影响主流程 |
| CDP集成 | ✅ | 支持从diagnosis-service获取和更新CDP |
| 错误处理 | ✅ | 完整的错误码和异常处理 |
| API文档 | ✅ | Swagger UI自动生成 |

---

## 📝 相关文档

- **服务实现方案**: `docs/explanation-service - 服务实现方案.md`
- **README**: `README.md`
- **错误处理规范**: `docs/AI医生系统-错误处理规范.md`
- **输出格式规范**: `docs/AI医生系统-最终输出格式规范.md`
- **LangChain架构开发流程**: `docs/开发过程文件/三个服务LangChain架构开发流程.md`

---

## 🔄 更新日志

### 2025-01 (v1.0)
- ✅ 实现证据链构建器（EvidenceChainBuilder）
- ✅ 实现推理路径可视化器（PathVisualizer）
- ✅ 实现终点结论包构建器（ConclusionPackageBuilder）
- ✅ 实现自然语言解释生成器（ExplanationGenerator）
- ✅ 实现解释服务核心逻辑（ExplanationService）
- ✅ 实现所有API接口（4个接口）
- ✅ 完善数据模型和异常处理
- ✅ 集成公共LLM库（aidoctor_llm）
- ✅ 创建已实现功能清单文档

---

## 💡 使用说明

### 启动服务

```bash
cd explanation-service
python run.py
# 或
uvicorn app.main:app --host 0.0.0.0 --port 8089
```

### 环境变量配置

```bash
# LLM配置
LLM_BACKEND=openai
OPENAI_API_KEY=your_api_key
OPENAI_MODEL=gpt-4
OPENAI_TEMPERATURE=0.3
LLM_TIMEOUT=30
LLM_MAX_RETRIES=3

# 诊断服务配置
DIAGNOSIS_SERVICE_URL=http://localhost:8080
```

### API调用示例

#### 生成完整解释

```bash
curl -X POST http://localhost:8089/api/v1/explain \
  -H "Content-Type: application/json" \
  -d '{
    "cdpId": "cdp_123456",
    "diagnosisResult": {
      "ddx": {
        "primary_hypothesis": [
          {
            "disease": "心绞痛",
            "confidence": 0.85
          }
        ]
      }
    }
  }'
```

#### 仅生成证据链

```bash
curl -X POST http://localhost:8089/api/v1/explain/evidence-chain \
  -H "Content-Type: application/json" \
  -d '{
    "cdpId": "cdp_123456"
  }'
```

### 功能说明

1. **核心功能**: 所有核心功能已完整实现，可用于生产环境
2. **降级策略**: LLM调用失败时，服务仍可返回结构化结论包
3. **CDP集成**: 自动从diagnosis-service获取CDP数据，并更新evidence_graph和conclusion_package字段
4. **错误处理**: 完整的错误码和异常处理机制

---

## 🎯 实现亮点

1. **完整的四要素实现**: 终点结论包包含结论、必须排除项状态、关键依据、行动与随访四个要素
2. **智能降级策略**: LLM不可用时，服务仍可正常工作，返回结构化数据
3. **灵活的API设计**: 支持完整生成和部分生成，满足不同场景需求
4. **统一的代码风格**: 与其他服务（dialog-service、diagnosis-engine-service）保持一致
5. **完善的错误处理**: 所有错误码和异常处理符合系统规范

---

**文档维护**: 当有新功能实现或优化时，请及时更新本文档。
