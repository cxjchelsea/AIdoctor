# diagnosis-engine-service - 服务实现方案

> **文档定位**：本文档定义diagnosis-engine-service的技术实现方案，包括技术选型、接口设计、数据流、实现步骤等。  
> **参考文档**：
> - 《AI医生系统-业务逻辑详细设计.md》- 业务逻辑（核心参考）
> - 《AI医生系统-技术架构设计.md》及相关子文档- 技术架构
> - 《AI医生系统-项目结构设计.md》- 项目结构
> - 《AI医生系统-数据模型设计.md》- 数据模型
> - 《AI医生系统-API接口规范.md》- API规范
> - 《AI医生系统-错误处理规范.md》- 错误处理规范
> - 《AI医生系统-最终输出格式规范.md》- 输出格式规范

---

## 一、服务概述

> **参考文档**：《AI医生系统-业务逻辑详细设计.md》

### 1.1 服务定位

- **对应脑区**：脑区C（鉴别诊断引擎）
- **在双通道推理架构中的位置**：通道1（结构化推理通道）的核心组件
- **服务职责**：
  1. **知识图谱推理引擎（kg-reasoning-engine）**：实现DR.KNOWS路径检索与评分，生成推理路径
  2. **多引擎融合诊断（multi-engine-fusion）**：融合五个诊断引擎的结果（规则引擎、知识图谱引擎、统计模型引擎、大模型引擎、鉴别诊断引擎）
  3. **三层分层分类器（three_layer_classifier）**：将诊断候选集分为三层（首要假设/主要备选/必须排除）
  4. **推理组织器（reasoning_organizer）**：组织推理子组，设计分流路径
  5. **证据分析器（evidence_analyzer）**：分析证据强度，构建证据链

### 1.2 输入输出

#### 1.2.1 输入数据格式和来源

**输入来源**：
- **上游服务**：`clinical-parsing-service`（脑区A）提供结构化病例数据
- **数据格式**：CDP中的`patient_state`字段，包含：
  - `symptoms`：症状列表（CUI编码）
  - `signs`：体征信息
  - `examination_results`：检查结果
  - `health_profile`：健康档案
  - `problem_list`：结构化问题清单

**数据获取方式**：
- 通过`diagnosis-service`传递CDP ID
- 从CDP中读取`patient_state`字段

#### 1.2.2 输出数据格式和目标

**输出目标**：
- **更新CDP的`ddx`字段**：鉴别诊断候选集（三层分层结构）
- **更新CDP的`evidence_graph`字段**：证据图结构
- **更新CDP的`reasoning_paths`字段**：推理路径列表

**输出格式**（符合《AI医生系统-最终输出格式规范.md》）：
```json
{
  "ddx": {
    "primary_hypothesis": [
      {
        "disease_cui": "C0020538",
        "disease_name": "心绞痛",
        "confidence": 0.85,
        "evidence_strength": 0.9,
        "reasoning_paths": [...]
      }
    ],
    "major_alternatives": [...],
    "must_exclude": [...]
  },
  "evidence_graph": {...},
  "reasoning_paths": [...],
  "fusion_weights": {
    "rule_engine": 0.25,
    "kg_engine": 0.25,
    "statistical_engine": 0.20,
    "llm_engine": 0.25,
    "differential_engine": 0.05
  }
}
```

#### 1.2.3 数据流转关系

**数据流转**（参考《AI医生系统-技术架构设计-CDP数据与状态管理.md》）：
```
clinical-parsing-service（脑区A）
    ↓ 输出：结构化病例数据（写入CDP.patient_state）
diagnosis-engine-service（脑区C）
    ↓ 读取：CDP.patient_state
    ↓ 处理：知识图谱推理、多引擎融合、三层分层
    ↓ 输出：鉴别诊断候选集（写入CDP.ddx）
workup-planner-service（脑区D）、treatment-engine-service（脑区E）、risk-assessment-service（脑区F）
    ↓ 读取：CDP.ddx
```

### 1.3 业务价值

- **解决什么业务问题**：
  1. 实现基于知识图谱的医学推理（DR.KNOWS方法）
  2. 提供可解释的诊断结果（推理路径可视化）
  3. 支持多引擎融合诊断（提高诊断准确性）
  4. 实现三层分层诊断（临床风险分级）

- **业务价值**：
  1. **准确性**：多引擎融合提高诊断准确性
  2. **可解释性**：推理路径可视化，支持医生审查
  3. **临床适用性**：三层分层符合临床思维
  4. **可扩展性**：引擎可插拔，支持新引擎接入

---

## 二、技术实现设计

> **参考文档**：
> - 《AI医生系统-技术架构设计.md》及相关子文档
> - 《AI医生系统-技术架构设计-核心技术组件.md》
> - 《AI医生系统-技术架构设计-性能与评估.md》

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
| **neo4j** | 5.14.0 | Neo4j图数据库客户端，用于知识图谱推理 |
| **numpy** | 1.24.3 | 数值计算，用于评分算法 |
| **scikit-learn** | 1.3.2 | 机器学习，用于统计模型引擎 |
| **langchain** | 0.1.0 | LLM集成框架，用于大模型引擎（可选，也可直接使用openai SDK） |
| **langchain-openai** | 0.0.2 | OpenAI API集成（可选） |
| **openai** | 1.10.0 | OpenAI官方SDK（如果不用langchain，可直接使用此SDK） |
| **httpx** | 0.25.1 | HTTP客户端，用于服务间调用 |
| **pydantic** | 2.5.0 | 数据验证和序列化 |
| **jinja2** | 3.1.2 | 模板引擎，用于Prompt生成 |
| **pyyaml** | 6.0.1 | YAML配置文件解析 |

#### 2.1.3 技术选型理由

1. **Python + FastAPI**：
   - Python生态丰富，适合AI/ML场景
   - FastAPI性能优秀，支持异步并发
   - 与Neo4j、LLM API集成方便

2. **Neo4j**：
   - 图数据库，适合知识图谱存储和查询
   - 支持复杂图遍历查询（Cypher）
   - 与DR.KNOWS方法匹配度高

3. **LLM集成方案**：
   - **使用公共LLM库（aidoctor_llm）**（推荐）
     - 统一接口，支持多种LLM后端（OpenAI、Ollama、ChatGLM等）
     - 便于管理Prompt模板
     - 提供重试、超时等机制
     - 与其他服务（explanation-service、dialog-service）共享代码
     - 参考文档：《三个服务LangChain架构开发流程.md》

### 2.2 核心算法/方法

#### 2.2.1 知识图谱推理引擎（kg-reasoning-engine）

**算法描述**：实现DR.KNOWS方法，在Neo4j知识图谱中检索症状到疾病的推理路径

**核心组件**：
1. **路径检索器（path_retriever）**：
   - 多跳路径检索（2-4跳）
   - Cypher查询优化
   - 路径去重和过滤

2. **路径评分器（path_scorer）**：
   - **先验概率评分器（prior_scorer）**：基于疾病先验概率
   - **似然评分器（likelihood_scorer）**：基于症状-疾病关联强度
   - **后验概率评分器（posterior_scorer）**：贝叶斯推理计算

3. **路径注入器（path_injector）**：
   - 将推理路径作为上下文注入LLM
   - 构建增强Prompt
   - 路径格式化

**算法实现思路**：
```python
# 1. 路径检索
paths = kg_client.retrieve_paths(symptom_cuis, max_hops=4)

# 2. 路径评分（三层评分体系）
scored_paths = []
for path in paths:
    prior_score = prior_scorer.score(path)
    likelihood_score = likelihood_scorer.score(path)
    posterior_score = posterior_scorer.score(path, prior_score, likelihood_score)
    scored_paths.append({
        "path": path,
        "prior_score": prior_score,
        "likelihood_score": likelihood_score,
        "posterior_score": posterior_score
    })

# 3. 路径排序
ranked_paths = sorted(scored_paths, key=lambda x: x["posterior_score"], reverse=True)

# 4. 路径注入LLM
enhanced_prompt = path_injector.build_prompt(patient_info, ranked_paths[:10])
```

**算法复杂度分析**：
- **路径检索**：O(n²)，n为路径长度
- **路径评分**：O(m)，m为路径数量
- **路径排序**：O(m log m)
- **总体复杂度**：O(n² + m log m)

#### 2.2.2 多引擎融合诊断（multi-engine-fusion）

**算法描述**：融合五个诊断引擎的结果，生成最终诊断候选集

**五个引擎**：
1. **规则引擎（rule_engine）**：基于医学规则库
2. **知识图谱引擎（kg_engine）**：基于Neo4j知识图谱
3. **统计模型引擎（statistical_engine）**：基于机器学习模型
4. **大模型引擎（llm_engine）**：基于LLM（路径约束）
5. **鉴别诊断引擎（differential_engine）**：基于鉴别诊断规则

**融合策略**：
- **加权融合**：每个引擎有固定权重
  - 规则引擎：25%
  - 知识图谱引擎：25%
  - 统计模型引擎：20%
  - 大模型引擎：25%
  - 鉴别诊断引擎：5%
- **动态权重调整**（可选）：根据引擎置信度动态调整权重
- **并行执行**：五个引擎并行执行，提高性能

**算法实现思路**：
```python
# 1. 并行执行五个引擎
results = await asyncio.gather(
    rule_engine.diagnose(request),
    kg_engine.diagnose(request),
    statistical_engine.diagnose(request),
    llm_engine.diagnose(request),
    differential_engine.diagnose(request),
    return_exceptions=True
)

# 2. 融合结果
fused_result = fusion_engine.fuse_results(results, weights)
```

#### 2.2.3 三层分层分类器（three_layer_classifier）

**算法描述**：将诊断候选集分为三层（首要假设/主要备选/必须排除）

**分层规则**：
1. **首要假设（primary_hypothesis）**：
   - 置信度 ≥ 0.7
   - 证据强度 ≥ 0.8
   - 风险等级：低/中

2. **主要备选（major_alternatives）**：
   - 置信度 0.3-0.7
   - 证据强度 0.5-0.8
   - 需要进一步验证

3. **必须排除（must_exclude）**：
   - 高危疾病（即使置信度低）
   - 紧急程度高
   - 需要立即排除

**算法实现思路**：
```python
def classify_ddx(candidates, risk_assessment):
    primary = []
    alternatives = []
    must_exclude = []
    
    for candidate in candidates:
        if is_high_risk(candidate, risk_assessment):
            must_exclude.append(candidate)
        elif candidate.confidence >= 0.7 and candidate.evidence_strength >= 0.8:
            primary.append(candidate)
        else:
            alternatives.append(candidate)
    
    return {
        "primary_hypothesis": primary,
        "major_alternatives": alternatives,
        "must_exclude": must_exclude
    }
```

#### 2.2.4 推理组织器（reasoning_organizer）

**算法描述**：组织推理子组，设计分流路径

**功能**：
1. **推理子组组织**：将相似疾病分组
2. **分流路径设计**：设计关键差异点问题
3. **信息增益计算**：评估问题的信息价值

#### 2.2.5 证据分析器（evidence_analyzer）

**算法描述**：分析证据强度，构建证据链

**功能**：
1. **证据强度评估**：评估每个证据的支持强度
2. **证据链构建**：构建证据-疾病关联图
3. **证据追踪**：追踪证据来源和推理路径

### 2.3 性能要求

> **参考文档**：《AI医生系统-技术架构设计-性能与评估.md》

#### 2.3.1 响应时间要求

| 功能模块 | 响应时间要求 | 说明 |
|---------|------------|------|
| **知识图谱路径检索** | < 3秒 | 单次路径检索 |
| **多引擎融合诊断** | < 5秒 | 五个引擎并行执行 |
| **三层分层分类** | < 1秒 | 分类算法 |
| **整体诊断流程** | < 8秒 | 端到端响应时间 |

#### 2.3.2 并发处理能力

- **目标并发**：支持20+并发请求
- **峰值并发**：支持50+并发请求
- **并发策略**：异步处理 + 连接池

#### 2.3.3 资源消耗限制

- **内存消耗**：< 4GB（单实例）
- **CPU使用率**：< 80%（正常负载）
- **数据库连接数**：Neo4j连接池 < 20

### 2.4 关键技术点

#### 2.4.1 关键技术难点

1. **Neo4j路径检索性能优化**：
   - **问题**：多跳路径检索可能性能较差
   - **解决方案**：
     - 使用索引优化查询
     - 限制路径长度（2-4跳）
     - 使用路径缓存

2. **多引擎融合权重调优**：
   - **问题**：如何确定最优权重
   - **解决方案**：
     - 基于历史数据调优
     - 支持动态权重调整
     - A/B测试验证

3. **LLM路径注入Prompt设计**：
   - **问题**：如何有效将路径注入LLM
   - **解决方案**：
     - 使用结构化Prompt模板（Jinja2）
     - 路径格式化（JSON/自然语言）
     - 上下文长度控制
   - **注意**：无论使用LangChain还是OpenAI SDK，都需要实现路径注入功能

#### 2.4.2 技术风险及应对

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| **Neo4j连接失败** | 高 | 连接池 + 重试机制 + 降级策略 |
| **LLM API超时** | 中 | 超时控制 + 异步调用 + 降级策略 |
| **多引擎融合性能瓶颈** | 中 | 并行执行 + 缓存 + 异步处理 |
| **知识图谱数据不完整** | 低 | 数据质量检查 + 默认值处理 |

---

## 三、接口设计

> **参考文档**：
> - 《AI医生系统-API接口规范.md》
> - 《AI医生系统-错误处理规范.md》
> - 《AI医生系统-数据模型设计.md》

### 3.1 API端点定义

#### 3.1.1 五引擎融合诊断接口

**接口路径**：`POST /api/v1/engine/diagnose`

**请求方法**：POST

**功能描述**：执行五引擎融合诊断，生成鉴别诊断候选集

**请求体**：
```json
{
  "cdpId": "cdp_123456",
  "symptomInfo": {
    "symptoms": ["C0018681", "C0027051"],
    "symptomNames": ["胸痛", "呼吸困难"]
  },
  "vitalSigns": {
    "bloodPressure": "120/80",
    "heartRate": 72,
    "temperature": 36.5
  },
  "examinationResults": [
    {
      "type": "ECG",
      "result": "正常",
      "timestamp": "2025-01-22T10:30:00Z"
    }
  ],
  "healthProfile": {
    "age": 45,
    "gender": "male",
    "medicalHistory": ["高血压"]
  }
}
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "ddx": {
      "primaryHypothesis": [
        {
          "diseaseCui": "C0020538",
          "diseaseName": "心绞痛",
          "confidence": 0.85,
          "evidenceStrength": 0.9,
          "reasoningPaths": [
            {
              "pathId": "path_001",
              "path": ["胸痛", "心绞痛"],
              "score": 0.85
            }
          ]
        }
      ],
      "majorAlternatives": [
        {
          "diseaseCui": "C0030193",
          "diseaseName": "心肌梗死",
          "confidence": 0.65,
          "evidenceStrength": 0.7
        }
      ],
      "mustExclude": [
        {
          "diseaseCui": "C0027051",
          "diseaseName": "急性心肌梗死",
          "confidence": 0.3,
          "riskLevel": "high",
          "reason": "高危疾病，需要立即排除"
        }
      ]
    },
    "evidenceGraph": {
      "nodes": [...],
      "edges": [...]
    },
    "reasoningPaths": [...],
    "fusionWeights": {
      "ruleEngine": 0.25,
      "kgEngine": 0.25,
      "statisticalEngine": 0.20,
      "llmEngine": 0.25,
      "differentialEngine": 0.05
    },
    "engineResults": {
      "ruleEngine": {...},
      "kgEngine": {...},
      "statisticalEngine": {...},
      "llmEngine": {...},
      "differentialEngine": {...}
    }
  },
  "timestamp": 1705123456789
}
```

#### 3.1.2 知识图谱路径检索接口

**接口路径**：`POST /api/v1/engine/kg/paths/retrieve`

**请求方法**：POST

**功能描述**：检索知识图谱推理路径（DR.KNOWS方法）

**请求体**：
```json
{
  "symptoms": ["C0018681", "C0027051"],
  "maxHops": 4,
  "limit": 50
}
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "paths": [
      {
        "pathId": "path_001",
        "disease": "C0020538",
        "diseaseName": "心绞痛",
        "path": ["symptom1", "disease1", "disease2"],
        "priorScore": 0.8,
        "likelihoodScore": 0.7,
        "posteriorScore": 0.85
      }
    ],
    "total": 50
  },
  "timestamp": 1705123456789
}
```

#### 3.1.3 路径评分接口

**接口路径**：`POST /api/v1/engine/kg/paths/score`

**请求方法**：POST

**功能描述**：对推理路径进行评分排序

#### 3.1.4 路径注入LLM接口

**接口路径**：`POST /api/v1/engine/kg/paths/inject`

**请求方法**：POST

**功能描述**：将推理路径注入LLM，生成增强Prompt

#### 3.1.5 三层分层分类接口

**接口路径**：`POST /api/v1/engine/classify/three-layer`

**请求方法**：POST

**功能描述**：对诊断候选集进行三层分层分类

#### 3.1.6 推理组织接口

**接口路径**：`POST /api/v1/engine/organize/reasoning`

**请求方法**：POST

**功能描述**：组织推理子组，设计分流路径

#### 3.1.7 证据分析接口

**接口路径**：`POST /api/v1/engine/analyze/evidence`

**请求方法**：POST

**功能描述**：分析证据强度，构建证据链

### 3.2 请求/响应模型

#### 3.2.1 请求模型定义

**DiagnosisEngineRequest**：
```python
class DiagnosisEngineRequest(BaseModel):
    cdpId: str
    symptomInfo: Optional[Dict] = None
    vitalSigns: Optional[Dict] = None
    examinationResults: Optional[List[Dict]] = None
    healthProfile: Optional[Dict] = None
```

#### 3.2.2 响应模型定义

**DiagnosisEngineResult**：
```python
class DiagnosisEngineResult(BaseModel):
    ddx: Dict  # 鉴别诊断候选集（三层分层）
    evidenceGraph: Optional[Dict] = None
    reasoningPaths: Optional[List[Dict]] = None
    fusionWeights: Optional[Dict] = None
    engineResults: Optional[Dict] = None
```

#### 3.2.3 数据验证规则

- **cdpId**：必填，格式：`cdp_[0-9a-zA-Z]+`
- **symptoms**：必填，至少包含一个症状CUI
- **vitalSigns**：可选，但包含时必须符合格式规范

### 3.3 错误码定义

> **参考文档**：《AI医生系统-错误处理规范.md》

**错误码范围**：3000-3999（diagnosis-engine-service）

| 错误码 | HTTP状态码 | 错误描述 | 说明 |
|--------|-----------|---------|------|
| **3000** | 500 | 诊断引擎服务内部错误 | 系统内部错误 |
| **3001** | 400 | 请求参数验证失败 | 参数格式错误 |
| **3002** | 404 | CDP不存在 | CDP ID不存在 |
| **3003** | 503 | Neo4j连接失败 | Neo4j服务不可用 |
| **3004** | 503 | LLM API调用失败 | LLM服务不可用 |
| **3005** | 500 | 知识图谱路径检索失败 | DR.KNOWS核心功能失败 |
| **3006** | 500 | 路径评分排序失败 | 路径评分算法失败 |
| **3007** | 500 | 路径注入LLM失败 | 路径注入功能失败 |
| **3008** | 500 | 多引擎融合失败 | 融合算法失败 |
| **3009** | 500 | 三层分层分类失败 | 分类算法失败 |
| **3010** | 500 | 推理组织失败 | 推理组织算法失败 |
| **3011** | 500 | 证据分析失败 | 证据分析算法失败 |

### 3.4 错误处理策略

1. **参数验证错误**：返回400，包含详细错误信息
2. **服务依赖错误**：返回503，标记`retryable=true`
3. **业务逻辑错误**：返回500，记录详细日志
4. **超时错误**：返回504，标记`retryable=true`

### 3.5 API文档

- **Swagger UI**：`http://localhost:8086/docs`
- **ReDoc**：`http://localhost:8086/redoc`
- **OpenAPI JSON**：`http://localhost:8086/openapi.json`

---

## 四、数据流设计

> **参考文档**：
> - 《AI医生系统-数据模型设计.md》
> - 《AI医生系统-技术架构设计-CDP数据与状态管理.md》
> - 《AI医生系统-最终输出格式规范.md》

### 4.1 数据输入来源

#### 4.1.1 上游服务

- **clinical-parsing-service**（脑区A）：
  - 提供结构化病例数据
  - 数据格式：CDP中的`patient_state`字段

#### 4.1.2 数据格式

**CDP.patient_state结构**：
```json
{
  "symptoms": [
    {
      "cui": "C0018681",
      "name": "胸痛",
      "normalized": true
    }
  ],
  "signs": {
    "bloodPressure": "120/80",
    "heartRate": 72
  },
  "examinationResults": [...],
  "healthProfile": {
    "age": 45,
    "gender": "male"
  },
  "problemList": [...]
}
```

#### 4.1.3 数据获取方式

- 通过`diagnosis-service`传递CDP ID
- 从CDP中读取`patient_state`字段
- 使用CDP版本控制机制

### 4.2 数据处理流程

#### 4.2.1 数据处理步骤

1. **读取CDP数据**：
   - 从CDP中读取`patient_state`字段
   - 提取症状列表（CUI编码）
   - 提取体征和检查结果

2. **知识图谱路径检索**：
   - 使用症状CUI查询Neo4j
   - 检索2-4跳推理路径
   - 路径去重和过滤

3. **路径评分排序**：
   - 计算先验概率评分
   - 计算似然评分
   - 计算后验概率评分
   - 路径排序

4. **路径注入LLM**：
   - 构建增强Prompt
   - 调用LLM API
   - 获取LLM诊断结果

5. **多引擎融合**：
   - 并行执行五个引擎
   - 融合结果
   - 生成最终诊断候选集

6. **三层分层分类**：
   - 根据置信度和证据强度分类
   - 考虑风险评估结果
   - 生成三层结构

7. **证据分析**：
   - 分析证据强度
   - 构建证据链
   - 更新证据图

8. **更新CDP**：
   - 更新`ddx`字段
   - 更新`evidence_graph`字段
   - 更新`reasoning_paths`字段

#### 4.2.2 数据转换逻辑

**症状CUI提取**：
```python
symptom_cuis = [s["cui"] for s in patient_state["symptoms"] if s.get("normalized")]
```

**路径格式化**：
```python
formatted_path = {
    "pathId": f"path_{path_id}",
    "disease": path["disease_cui"],
    "diseaseName": path["disease_name"],
    "path": path["nodes"],
    "scores": {
        "prior": path["prior_score"],
        "likelihood": path["likelihood_score"],
        "posterior": path["posterior_score"]
    }
}
```

#### 4.2.3 数据验证规则

- **症状CUI验证**：必须是有效的CUI编码
- **路径验证**：路径长度必须在2-4跳之间
- **评分验证**：评分必须在0-1之间

### 4.3 数据输出格式

#### 4.3.1 输出数据结构

**CDP.ddx结构**：
```json
{
  "primary_hypothesis": [
    {
      "disease_cui": "C0020538",
      "disease_name": "心绞痛",
      "confidence": 0.85,
      "evidence_strength": 0.9,
      "reasoning_paths": [...],
      "evidence": [...]
    }
  ],
  "major_alternatives": [...],
  "must_exclude": [...]
}
```

#### 4.3.2 数据格式规范

- **字段命名**：使用`snake_case`（CDP内部）
- **数据类型**：JSON格式
- **版本控制**：使用CDP版本号

#### 4.3.3 数据存储方式

- **CDP存储**：MySQL数据库（JSON字段）
- **缓存**：Redis缓存推理结果（可选）
- **版本控制**：CDP版本管理机制

### 4.4 CDP数据流转

#### 4.4.1 CDP字段更新

**更新字段**：
- `ddx`：鉴别诊断候选集（三层分层）
- `evidence_graph`：证据图结构
- `reasoning_paths`：推理路径列表

**更新方式**：
- 使用CDP版本控制机制
- 乐观锁防止并发冲突
- 写时复制（Copy-on-Write）

#### 4.4.2 CDP版本控制

- **版本号**：每次更新递增版本号
- **版本历史**：保留历史版本（可选）
- **版本查询**：支持版本查询接口

#### 4.4.3 CDP状态流转

**状态流转**：
```
CDP状态：initialized
    ↓ diagnosis-engine-service处理
CDP状态：ddx_generated
    ↓ 其他服务处理
CDP状态：completed
```

---

## 五、依赖关系

### 5.1 依赖的其他服务

#### 5.1.1 上游服务

| 服务 | 依赖关系 | 调用方式 | 说明 |
|------|---------|---------|------|
| **clinical-parsing-service** | 强依赖 | 同步调用 | 获取结构化病例数据 |
| **diagnosis-service** | 强依赖 | 同步调用 | 获取CDP数据，更新CDP |

#### 5.1.2 下游服务

| 服务 | 依赖关系 | 调用方式 | 说明 |
|------|---------|---------|------|
| **workup-planner-service** | 弱依赖 | 读取CDP | 读取诊断结果 |
| **treatment-engine-service** | 弱依赖 | 读取CDP | 读取诊断结果 |
| **risk-assessment-service** | 弱依赖 | 读取CDP | 读取诊断结果 |

### 5.2 依赖的外部资源

#### 5.2.1 数据库

- **Neo4j**：
  - **用途**：知识图谱存储和查询
  - **连接方式**：Neo4j Python Driver
  - **连接池**：最大20个连接
  - **超时设置**：连接超时5秒，查询超时10秒

#### 5.2.2 缓存

- **Redis**（可选）：
  - **用途**：缓存推理结果
  - **TTL**：1小时
  - **键格式**：`diagnosis:cdp:{cdpId}`

#### 5.2.3 外部API

- **LLM API**（OpenAI/其他）：
  - **用途**：大模型引擎
  - **调用方式**：HTTP API
  - **超时设置**：30秒
  - **重试机制**：最多重试3次

### 5.3 依赖管理

#### 5.3.1 依赖版本管理

- **requirements.txt**：固定版本号
- **公共LLM库**：使用 `-e ../common/aidoctor_llm`（开发模式安装）
- **版本更新策略**：定期更新，测试后发布

#### 5.3.2 依赖更新策略

- **安全更新**：立即更新
- **功能更新**：测试后更新
- **重大版本更新**：评估影响后更新
- **公共LLM库更新**：统一更新，确保三个服务版本一致

#### 5.3.3 依赖冲突处理

- **版本冲突**：使用最新兼容版本
- **依赖冲突**：使用虚拟环境隔离
- **公共库依赖**：统一管理，避免版本不一致

---

## 六、实现步骤

> **参考文档**：
> - 《AI医生系统-项目结构设计.md》
> - 《AI医生系统-技术架构设计-项目实现与部署.md》

### 6.1 分阶段实现计划

#### Phase 1: 基础框架搭建（1周）

**目标**：搭建服务基础框架，配置开发环境

**任务清单**：
- [ ] 创建服务目录结构（参考项目结构设计）
- [ ] 配置FastAPI框架
- [ ] 实现基础工具类（异常处理、日志、配置）
- [ ] 配置Neo4j连接
- [ ] 配置LLM客户端
- [ ] 实现健康检查接口
- [ ] 编写基础单元测试

**目录结构**：
```
diagnosis-engine-service/
├── app/
│   ├── main.py                 # FastAPI应用入口
│   ├── api/
│   │   └── routes.py           # API路由
│   ├── services/
│   │   └── diagnosis_service.py # 诊断服务
│   ├── models/
│   │   ├── request.py          # 请求模型
│   │   └── response.py         # 响应模型
│   ├── utils/
│   │   ├── exceptions.py       # 异常处理
│   │   └── llm_client.py      # LLM客户端（从公共库导入）
│   └── config/
│       └── settings.py        # 配置管理
├── requirements.txt            # 包含: -e ../common/aidoctor_llm
├── Dockerfile
└── README.md
```

**注意**：LLM客户端使用公共库（`aidoctor_llm`），参考《三个服务LangChain架构开发流程.md》

#### Phase 2: 知识图谱推理引擎实现（2周）

**目标**：实现DR.KNOWS核心方法（路径检索、评分、注入）

**任务清单**：
- [ ] 实现Neo4j客户端（kg_client.py）
- [ ] 实现路径检索器（path_retriever.py）
  - [ ] 多跳路径检索（2-4跳）
  - [ ] 路径去重和过滤
  - [ ] 性能优化（索引、缓存）
- [ ] 实现路径评分器（path_scorer.py）
  - [ ] 先验概率评分器（prior_scorer.py）
  - [ ] 似然评分器（likelihood_scorer.py）
  - [ ] 后验概率评分器（posterior_scorer.py）
- [ ] 实现路径注入器（path_injector.py）
  - [ ] Prompt模板设计
  - [ ] 路径格式化
  - [ ] LLM API调用
- [ ] 实现知识图谱推理引擎主类（kg_reasoning_engine.py）
- [ ] 编写单元测试（覆盖率≥80%）

#### Phase 3: 多引擎融合诊断实现（2周）

**目标**：实现五个诊断引擎和融合算法

**任务清单**：
- [ ] 实现基础引擎接口（base_engine.py）
- [ ] 实现规则引擎（rule_engine.py）
  - [ ] 规则库设计
  - [ ] 规则匹配算法
- [ ] 实现知识图谱引擎（kg_engine.py）
  - [ ] 调用kg-reasoning-engine
- [ ] 实现统计模型引擎（statistical_engine.py）
  - [ ] 模型加载和推理
  - [ ] 特征工程
- [ ] 实现大模型引擎（llm_engine.py）
  - [ ] 集成公共LLM库（aidoctor_llm）
  - [ ] 使用LangChainLLMClient
  - [ ] 使用PromptTemplateManager
  - [ ] 路径注入LLM功能
  - [ ] 参考《三个服务LangChain架构开发流程.md》
- [ ] 实现鉴别诊断引擎（differential_engine.py）
  - [ ] 鉴别诊断规则
- [ ] 实现融合引擎（fusion_engine.py）
  - [ ] 并行执行逻辑
  - [ ] 加权融合算法
  - [ ] 动态权重调整（可选）
- [ ] 编写单元测试（覆盖率≥80%）

#### Phase 4: 三层分层分类器实现（1周）

**目标**：实现三层分层分类算法

**任务清单**：
- [ ] 实现三层分层分类器（three_layer_classifier.py）
  - [ ] 首要假设分类规则
  - [ ] 主要备选分类规则
  - [ ] 必须排除分类规则
- [ ] 集成风险评估结果
- [ ] 编写单元测试

#### Phase 5: 推理组织器和证据分析器实现（1周）

**目标**：实现推理组织和证据分析功能

**任务清单**：
- [ ] 实现推理组织器（reasoning_organizer.py）
  - [ ] 推理子组组织
  - [ ] 分流路径设计
  - [ ] 信息增益计算
- [ ] 实现证据分析器（evidence_analyzer.py）
  - [ ] 证据强度评估
  - [ ] 证据链构建
  - [ ] 证据追踪
- [ ] 编写单元测试

#### Phase 6: 接口实现和集成（1周）

**目标**：实现所有API接口，完成服务集成

**任务清单**：
- [ ] 实现所有API接口（routes.py）
  - [ ] 五引擎融合诊断接口
  - [ ] 知识图谱路径检索接口
  - [ ] 路径评分接口
  - [ ] 路径注入LLM接口
  - [ ] 三层分层分类接口
  - [ ] 推理组织接口
  - [ ] 证据分析接口
- [ ] 实现请求/响应模型验证
- [ ] 实现错误处理（遵循错误处理规范）
- [ ] 实现API文档（Swagger/OpenAPI）
- [ ] 编写集成测试

#### Phase 7: 性能优化和测试（1周）

**目标**：性能优化，完善测试

**任务清单**：
- [ ] 性能优化
  - [ ] Neo4j查询优化
  - [ ] 缓存策略实现
  - [ ] 并发处理优化
- [ ] 性能测试
  - [ ] 响应时间测试
  - [ ] 并发测试
  - [ ] 压力测试
- [ ] 完善单元测试（覆盖率≥80%）
- [ ] 完善集成测试
- [ ] 编写测试报告

### 6.2 优先级排序

- **P0（必须）**：
  - 知识图谱推理引擎（DR.KNOWS核心）
  - 多引擎融合诊断
  - 三层分层分类器
  - 基础API接口

- **P1（重要）**：
  - 推理组织器
  - 证据分析器
  - 性能优化

- **P2（可选）**：
  - 动态权重调整
  - 路径缓存
  - 高级性能优化

### 6.3 里程碑定义

- **Milestone 1**：基础框架完成（Phase 1）
  - 服务可启动
  - 健康检查接口正常
  - 基础工具类完成

- **Milestone 2**：知识图谱推理引擎完成（Phase 2）
  - 路径检索功能正常
  - 路径评分功能正常
  - 路径注入LLM功能正常

- **Milestone 3**：多引擎融合诊断完成（Phase 3）
  - 五个引擎实现完成
  - 融合算法实现完成
  - 并行执行正常

- **Milestone 4**：三层分层分类器完成（Phase 4）
  - 分类算法实现完成
  - 分类结果正确

- **Milestone 5**：推理组织和证据分析完成（Phase 5）
  - 推理组织功能正常
  - 证据分析功能正常

- **Milestone 6**：接口实现和集成完成（Phase 6）
  - 所有API接口实现完成
  - 集成测试通过

- **Milestone 7**：性能优化和测试完成（Phase 7）
  - 性能测试通过
  - 测试覆盖率达标

---

## 七、测试策略

### 7.1 单元测试计划

#### 7.1.1 测试覆盖范围

- **核心算法**：知识图谱推理引擎、多引擎融合、三层分层分类
- **工具类**：异常处理、日志、配置
- **数据模型**：请求/响应模型验证

#### 7.1.2 测试用例设计

**知识图谱推理引擎测试**：
- 测试路径检索（不同跳数）
- 测试路径评分（不同评分算法）
- 测试路径注入LLM（不同Prompt格式）

**多引擎融合测试**：
- 测试单个引擎执行
- 测试并行执行
- 测试融合算法
- 测试异常处理

**三层分层分类测试**：
- 测试首要假设分类
- 测试主要备选分类
- 测试必须排除分类
- 测试边界情况

#### 7.1.3 Mock策略

- **Neo4j Mock**：使用Mock Neo4j客户端
- **LLM API Mock**：使用Mock LLM客户端
- **CDP Mock**：使用Mock CDP数据

### 7.2 集成测试计划

#### 7.2.1 服务间集成测试

- **与clinical-parsing-service集成**：
  - 测试数据流转
  - 测试错误处理

- **与diagnosis-service集成**：
  - 测试CDP读写
  - 测试版本控制

#### 7.2.2 数据流测试

- **CDP数据流转测试**：
  - 测试CDP读取
  - 测试CDP更新
  - 测试版本控制

#### 7.2.3 端到端测试

- **完整诊断流程测试**：
  - 从CDP读取数据
  - 执行诊断
  - 更新CDP
  - 验证结果

### 7.3 性能测试计划

#### 7.3.1 性能测试指标

- **响应时间**：
  - 知识图谱路径检索：< 3秒
  - 多引擎融合诊断：< 5秒
  - 整体诊断流程：< 8秒

- **并发处理能力**：
  - 目标并发：20+
  - 峰值并发：50+

- **资源消耗**：
  - 内存：< 4GB
  - CPU：< 80%

#### 7.3.2 性能测试场景

- **场景1**：单次诊断请求
- **场景2**：并发诊断请求（20并发）
- **场景3**：峰值并发请求（50并发）
- **场景4**：长时间运行测试（稳定性）

#### 7.3.3 性能优化目标

- **响应时间优化**：减少20%
- **并发能力提升**：提升50%
- **资源消耗降低**：降低30%

### 7.4 测试数据准备

#### 7.4.1 测试数据设计

- **症状数据**：常见症状CUI编码
- **疾病数据**：常见疾病CUI编码
- **路径数据**：知识图谱路径数据
- **CDP数据**：模拟CDP数据

#### 7.4.2 测试数据管理

- **测试数据隔离**：使用独立的测试数据库
- **数据清理**：每个测试用例执行后清理数据
- **测试数据准备**：使用Fixture或TestDataBuilder

#### 7.4.3 测试环境配置

- **开发环境**：本地开发环境
- **测试环境**：独立的测试环境
- **性能测试环境**：独立的性能测试环境

---

## 八、风险评估

### 8.1 技术风险

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| **Neo4j连接失败** | 高 | 连接池 + 重试机制 + 降级策略 |
| **LLM API超时** | 中 | 超时控制 + 异步调用 + 降级策略 |
| **多引擎融合性能瓶颈** | 中 | 并行执行 + 缓存 + 异步处理 |
| **知识图谱数据不完整** | 低 | 数据质量检查 + 默认值处理 |
| **路径检索性能问题** | 中 | 索引优化 + 路径缓存 + 查询优化 |

### 8.2 业务风险

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| **诊断准确性不足** | 高 | 多引擎融合 + 权重调优 + A/B测试 |
| **三层分层分类错误** | 中 | 规则优化 + 人工审核 + 反馈机制 |
| **推理路径不准确** | 中 | 路径评分优化 + 人工验证 |

### 8.3 时间风险

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| **开发时间超期** | 中 | 分阶段实现 + 优先级排序 + 里程碑管理 |
| **测试时间不足** | 低 | 并行测试 + 自动化测试 |

---

## 九、后续优化方向

### 9.1 性能优化

#### 9.1.1 优化方向

1. **Neo4j查询优化**：
   - 使用更高效的Cypher查询
   - 优化索引策略
   - 实现查询缓存

2. **多引擎融合优化**：
   - 实现动态权重调整
   - 优化并行执行策略
   - 实现结果缓存

3. **LLM调用优化**：
   - 实现Prompt缓存
   - 优化Prompt长度
   - 使用更高效的LLM模型

#### 9.1.2 优化计划

- **短期**（1-2个月）：
  - Neo4j查询优化
  - 实现基础缓存策略

- **中期**（3-6个月）：
  - 动态权重调整
  - 高级缓存策略

- **长期**（6个月以上）：
  - 机器学习优化权重
  - 自适应性能调优

### 9.2 功能扩展

#### 9.2.1 扩展方向

1. **新引擎接入**：
   - 支持新诊断引擎接入
   - 引擎插件化设计

2. **高级推理功能**：
   - 时序推理
   - 多模态推理
   - 因果推理

3. **可解释性增强**：
   - 推理路径可视化
   - 证据链可视化
   - 自然语言解释生成

#### 9.2.2 扩展计划

- **短期**（1-2个月）：
  - 引擎插件化设计
  - 基础可视化功能

- **中期**（3-6个月）：
  - 高级推理功能
  - 可解释性增强

- **长期**（6个月以上）：
  - 多模态推理
  - 因果推理

---

**文档版本**：v1.0  
**创建日期**：2025年1月  
**维护人员**：开发团队

