# dialog-service - 服务实现方案

> **文档定位**：本文档定义dialog-service（对话管理服务，脑区B）的技术实现方案，包括技术选型、接口设计、数据流、实现步骤等。  
> **参考文档**：
> - 《AI医生系统-业务逻辑详细设计.md》- 业务逻辑（核心参考）
> - 《AI医生系统-技术架构设计.md》及相关子文档- 技术架构
> - 《AI医生系统-项目结构设计.md》- 项目结构
> - 《AI医生系统-数据模型设计.md》- 数据模型
> - 《AI医生系统-API接口规范.md》- API规范
> - 《AI医生系统-错误处理规范.md》- 错误处理规范
> - 《三个服务LangChain架构开发流程.md》- LangChain架构开发流程

---

## 一、服务概述

> **参考文档**：《AI医生系统-业务逻辑详细设计.md》

### 1.1 服务定位

- **对应脑区**：脑区B（主动问诊与信息补全服务）
- **在双通道推理架构中的位置**：通道1 + 通道2（结构化推理通道 + 语言与策略通道）
- **服务职责**：
  1. **信息缺口识别**：识别诊断所需的关键信息缺口
  2. **信息缺口分级**：将信息缺口分为必填/重要/可选三级
  3. **智能追问生成**：基于信息缺口分级生成智能追问问题
  4. **自然语言理解（NLU）**：理解用户的自然语言输入
  5. **自然语言生成（NLG）**：生成自然、易懂的追问问题

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
- **更新CDP的`problem_list`字段**：结构化问题清单（包含信息缺口）
- **生成追问问题**：自然语言追问问题
- **更新对话上下文**：维护对话历史

**输出格式**（符合《AI医生系统-数据模型设计.md》）：
```json
{
  "question": "您胸痛是在什么情况下出现的？是活动后还是休息时？",
  "questionType": "symptom_detail",
  "missingInfo": [
    {
      "field": "symptom_trigger",
      "level": "required",
      "description": "症状诱因"
    }
  ],
  "completeness": 0.65,
  "informationGaps": {
    "required": [
      {
        "field": "symptom_trigger",
        "description": "症状诱因",
        "reason": "用于鉴别心绞痛和心肌梗死"
      }
    ],
    "important": [
      {
        "field": "symptom_duration",
        "description": "症状持续时间",
        "reason": "用于判断疾病严重程度"
      }
    ],
    "optional": [
      {
        "field": "family_history",
        "description": "家族史",
        "reason": "用于风险评估"
      }
    ]
  },
  "context": {
    "conversationHistory": [
      {
        "role": "user",
        "content": "我最近胸口闷"
      },
      {
        "role": "assistant",
        "content": "您胸痛是在什么情况下出现的？"
      }
    ]
  }
}
```

#### 1.2.3 数据流转关系

**数据流转**（参考《AI医生系统-技术架构设计-CDP数据与状态管理.md》）：
```
clinical-parsing-service（脑区A）
    ↓ 输出：结构化病例数据（写入CDP.patient_state）
dialog-service（脑区B）
    ↓ 读取：CDP.patient_state
    ↓ 处理：信息缺口识别、智能追问生成、NLU/NLG
    ↓ 输出：追问问题、信息缺口（更新CDP.problem_list）
用户
    ↓ 回答：自然语言回答
dialog-service（脑区B）
    ↓ 处理：NLU理解用户输入
    ↓ 输出：结构化信息（更新CDP.patient_state）
```

### 1.3 业务价值

- **解决什么业务问题**：
  1. 主动识别诊断所需的关键信息缺口
  2. 智能生成追问问题，提升信息采集效率
  3. 理解用户的自然语言输入，提升用户体验
  4. 基于信息缺口分级，优先采集关键信息

- **业务价值**：
  1. **信息完整性**：确保诊断所需的关键信息完整
  2. **问诊效率**：智能追问，减少无效问题
  3. **用户体验**：自然语言交互，提升用户体验
  4. **诊断准确性**：优先采集关键信息，提升诊断准确性

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
    - 支持WebSocket（用于实时对话）

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
| **redis** | 5.0.0 | 缓存和对话上下文存储 |
| **websockets** | 12.0 | WebSocket支持（可选） |

#### 2.1.3 技术选型理由

1. **Python + FastAPI**：
   - Python生态丰富，适合AI/ML场景
   - FastAPI性能优秀，支持异步并发
   - 支持WebSocket，适合实时对话场景

2. **公共LLM库（aidoctor_llm）**：
   - 统一LLM集成，与其他服务（diagnosis-engine-service、explanation-service）共享代码
   - 便于维护和扩展
   - 支持多种LLM后端
   - 参考《三个服务LangChain架构开发流程.md》

3. **Redis**：
   - 用于存储对话上下文
   - 支持过期时间，自动清理
   - 高性能，适合实时场景

### 2.2 核心算法/方法

#### 2.2.1 信息缺口识别算法

**算法描述**：识别诊断所需的关键信息缺口

**核心组件**：
1. **信息缺口识别器（information_gap_identifier）**：
   - 分析当前已收集的信息
   - 对比诊断所需的信息清单
   - 识别缺失信息

2. **信息缺口分级器（information_gap_classifier）**：
   - 将信息缺口分为必填/重要/可选三级
   - 基于信息增益和诊断重要性分级

**算法实现思路**：
```python
# 1. 分析已收集信息
collected_info = analyze_collected_info(cdp.patient_state)

# 2. 获取诊断所需信息清单
required_info_list = get_required_info_list(cdp.ddx)

# 3. 识别信息缺口
missing_info = identify_missing_info(collected_info, required_info_list)

# 4. 信息缺口分级
classified_gaps = classify_information_gaps(missing_info, cdp.ddx)
```

#### 2.2.2 智能追问生成算法

**算法描述**：基于信息缺口分级生成智能追问问题

**核心组件**：
1. **追问策略生成器（questioning_strategy_generator）**：
   - 基于信息缺口分级生成追问策略
   - 优先级排序（必填 > 重要 > 可选）

2. **追问问题生成器（question_generator）**：
   - 生成追问问题模板
   - 使用LLM生成自然语言问题

**算法实现思路**：
```python
# 1. 获取信息缺口（已分级）
information_gaps = cdp.problem_list.information_gaps

# 2. 生成追问策略
strategy = generate_questioning_strategy(information_gaps)

# 3. 选择最高优先级的信息缺口
priority_gap = select_priority_gap(information_gaps)

# 4. 生成追问问题
question = await generate_question(priority_gap, context)
```

#### 2.2.3 自然语言理解（NLU）算法

**算法描述**：理解用户的自然语言输入

**核心组件**：
1. **NLU引擎（nlu_engine）**：
   - 使用LLM理解用户输入
   - 提取关键信息（症状、体征、时间等）

2. **信息提取器（information_extractor）**：
   - 从用户输入中提取结构化信息
   - 验证信息完整性

**算法实现思路**：
```python
# 1. 使用LLM理解用户输入
understood_info = await llm_client.understand(
    user_input=user_text,
    context=conversation_context
)

# 2. 提取关键信息
extracted_info = extract_information(understood_info)

# 3. 验证信息完整性
validated_info = validate_information(extracted_info)

# 4. 更新CDP
update_cdp(cdp_id, validated_info)
```

#### 2.2.4 自然语言生成（NLG）算法

**算法描述**：生成自然、易懂的追问问题

**核心组件**：
1. **NLG引擎（nlg_engine）**：
   - 使用LLM生成自然语言问题
   - 确保问题清晰、易懂

2. **问题格式化器（question_formatter）**：
   - 格式化问题为自然语言
   - 添加必要的上下文信息

**算法实现思路**：
```python
# 1. 使用模板管理器格式化Prompt
prompt = template_manager.format_question_generation(
    information_gap=priority_gap,
    context=conversation_context
)

# 2. 调用LLM生成问题
question = await llm_client.generate(prompt)

# 3. 后处理（格式化、验证）
formatted_question = post_process_question(question)
```

### 2.3 性能要求

> **参考文档**：《AI医生系统-技术架构设计-性能与评估.md》

#### 2.3.1 响应时间要求

| 功能模块 | 响应时间要求 | 说明 |
|---------|------------|------|
| **信息缺口识别** | < 1秒 | 结构化数据处理 |
| **智能追问生成** | < 3秒 | LLM调用 |
| **自然语言理解（NLU）** | < 3秒 | LLM调用 |
| **自然语言生成（NLG）** | < 3秒 | LLM调用 |
| **整体对话流程** | < 5秒 | 端到端响应时间 |

#### 2.3.2 并发处理能力

- **目标并发**：支持20+并发对话
- **峰值并发**：支持50+并发对话
- **并发策略**：异步处理 + Redis缓存

#### 2.3.3 资源消耗限制

- **内存消耗**：< 2GB（单实例）
- **CPU使用率**：< 70%（正常负载）
- **Redis内存**：< 1GB（对话上下文）

### 2.4 关键技术点

#### 2.4.1 关键技术难点

1. **信息缺口识别的准确性**：
   - **问题**：如何准确识别诊断所需的关键信息缺口
   - **解决方案**：
     - 使用规则引擎识别信息缺口
     - 基于诊断候选集动态调整信息需求
     - 信息缺口分级算法

2. **智能追问的质量**：
   - **问题**：如何生成高质量、有针对性的追问问题
   - **解决方案**：
     - 使用结构化的Prompt模板
     - 提供足够的上下文信息
     - 基于信息缺口分级优先生成关键问题

3. **自然语言理解的准确性**：
   - **问题**：如何准确理解用户的自然语言输入
   - **解决方案**：
     - 使用LLM进行语义理解
     - 结合上下文信息
     - 信息提取和验证

#### 2.4.2 技术风险及应对

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| **LLM API调用失败** | 中 | 降级策略：使用模板生成问题 |
| **信息缺口识别失败** | 低 | 使用默认信息清单 |
| **对话上下文丢失** | 中 | Redis持久化、定期备份 |

---

## 三、接口设计

> **参考文档**：
> - 《AI医生系统-API接口规范.md》
> - 《AI医生系统-错误处理规范.md》
> - 《AI医生系统-数据模型设计.md》

### 3.1 API端点定义

#### 3.1.1 生成追问问题接口

**接口路径**：`POST /api/v1/dialog/generate-question`

**请求方法**：POST

**功能描述**：基于当前信息状态生成智能追问问题

**请求体**：
```json
{
  "cdpId": "cdp_123456",
  "context": {
    "conversationHistory": [
      {
        "role": "user",
        "content": "我最近胸口闷"
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
    "question": "您胸痛是在什么情况下出现的？是活动后还是休息时？",
    "questionType": "symptom_detail",
    "missingInfo": [
      {
        "field": "symptom_trigger",
        "level": "required",
        "description": "症状诱因"
      }
    ],
    "completeness": 0.65,
    "informationGaps": {
      "required": [
        {
          "field": "symptom_trigger",
          "description": "症状诱因",
          "reason": "用于鉴别心绞痛和心肌梗死"
        }
      ],
      "important": [],
      "optional": []
    }
  },
  "timestamp": 1705123456789
}
```

#### 3.1.2 理解用户输入接口

**接口路径**：`POST /api/v1/dialog/understand`

**请求方法**：POST

**功能描述**：理解用户的自然语言输入，提取结构化信息

**请求体**：
```json
{
  "cdpId": "cdp_123456",
  "userInput": "我活动后就会胸痛，休息一下就好了",
  "context": {
    "conversationHistory": [...]
  }
}
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "extractedInfo": {
      "symptom_trigger": "activity",
      "symptom_relief": "rest",
      "symptom_duration": "recent"
    },
    "confidence": 0.9,
    "updatedFields": ["symptom_trigger", "symptom_relief"]
  },
  "timestamp": 1705123456789
}
```

#### 3.1.3 识别信息缺口接口

**接口路径**：`POST /api/v1/dialog/identify-gaps`

**请求方法**：POST

**功能描述**：识别当前信息缺口并分级

**请求体**：
```json
{
  "cdpId": "cdp_123456"
}
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "informationGaps": {
      "required": [...],
      "important": [...],
      "optional": [...]
    },
    "completeness": 0.65
  },
  "timestamp": 1705123456789
}
```

#### 3.1.4 WebSocket对话接口（可选）

**接口路径**：`WS /api/v1/dialog/ws/{cdpId}`

**功能描述**：实时对话接口，支持双向通信

### 3.2 请求/响应模型

#### 3.2.1 请求模型定义

**QuestionRequest**：
```python
class QuestionRequest(BaseModel):
    cdpId: str
    context: Optional[Dict[str, Any]] = None
```

**UserInputRequest**：
```python
class UserInputRequest(BaseModel):
    cdpId: str
    userInput: str
    context: Optional[Dict[str, Any]] = None
```

#### 3.2.2 响应模型定义

**QuestionResponse**：
```python
class QuestionResponse(BaseModel):
    question: str
    questionType: str
    missingInfo: List[Dict[str, Any]]
    completeness: float
    informationGaps: Dict[str, List[Dict[str, Any]]]
```

**UnderstandingResponse**：
```python
class UnderstandingResponse(BaseModel):
    extractedInfo: Dict[str, Any]
    confidence: float
    updatedFields: List[str]
```

### 3.3 错误码定义

> **参考文档**：《AI医生系统-错误处理规范.md》

**错误码范围**：1200-1299（dialog-service）

| 错误码 | HTTP状态码 | 错误描述 | 说明 |
|--------|-----------|---------|------|
| **1200** | 500 | 对话服务内部错误 | 系统内部错误 |
| **1201** | 500 | 信息缺口识别失败 | 信息缺口识别算法失败 |
| **1202** | 500 | 智能追问生成失败 | 追问生成失败 |
| **1203** | 500 | 自然语言理解失败 | NLU失败 |
| **1204** | 500 | 自然语言生成失败 | NLG失败 |
| **1205** | 400 | CDP不存在 | CDP ID不存在 |
| **1206** | 400 | 用户输入为空 | 用户输入为空或无效 |

### 3.4 错误处理策略

1. **LLM调用失败**：降级策略，使用模板生成问题
2. **信息缺口识别失败**：使用默认信息清单
3. **对话上下文丢失**：从CDP重新构建上下文

---

## 四、数据流设计

> **参考文档**：
> - 《AI医生系统-数据模型设计.md》
> - 《AI医生系统-技术架构设计-CDP数据与状态管理.md》

### 4.1 数据输入来源

#### 4.1.1 上游服务

- **clinical-parsing-service**（脑区A）：
  - 提供结构化病例数据

#### 4.1.2 数据格式

**CDP.patient_state结构**：
```json
{
  "symptoms": [...],
  "signs": [...],
  "examination_results": [...],
  "health_profile": {...},
  "problem_list": {
    "chief_complaint": {...},
    "accompanying_symptoms": [...],
    "information_gaps": {
      "required": [...],
      "important": [...],
      "optional": [...]
    }
  }
}
```

### 4.2 数据处理流程

#### 4.2.1 数据处理步骤

1. **读取CDP数据**：
   - 从CDP中读取`patient_state`字段

2. **识别信息缺口**：
   - 分析已收集信息
   - 识别缺失信息
   - 信息缺口分级

3. **生成追问问题**：
   - 选择最高优先级的信息缺口
   - 使用LLM生成自然语言问题

4. **理解用户输入**：
   - 使用LLM理解用户输入
   - 提取结构化信息
   - 更新CDP

5. **更新对话上下文**：
   - 更新Redis中的对话上下文
   - 更新CDP中的`problem_list`字段

### 4.3 数据输出格式

#### 4.3.1 输出数据结构

**CDP.problem_list结构**：
```json
{
  "chief_complaint": {...},
  "accompanying_symptoms": [...],
  "information_gaps": {
    "required": [...],
    "important": [...],
    "optional": [...]
  },
  "completeness": 0.65
}
```

---

## 五、依赖关系

### 5.1 依赖的其他服务

#### 5.1.1 上游服务

| 服务 | 依赖关系 | 调用方式 | 说明 |
|------|---------|---------|------|
| **clinical-parsing-service** | 弱依赖 | 读取CDP | 读取结构化病例数据 |
| **diagnosis-service** | 强依赖 | 同步调用 | 获取CDP数据，更新CDP |

### 5.2 依赖的外部资源

#### 5.2.1 公共LLM库

- **aidoctor_llm**：
  - **用途**：LLM客户端和Prompt模板管理
  - **安装方式**：`-e ../common/aidoctor_llm`
  - **参考文档**：《三个服务LangChain架构开发流程.md》

#### 5.2.2 外部API

- **LLM API**（通过公共库）：
  - **用途**：NLU和NLG
  - **调用方式**：通过LangChainLLMClient
  - **超时设置**：30秒
  - **重试机制**：最多重试3次

#### 5.2.3 数据库/缓存

- **Redis**：
  - **用途**：存储对话上下文
  - **过期时间**：30分钟
  - **数据结构**：Hash（对话历史）

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

**目标**：搭建服务基础框架，集成公共LLM库和Redis

**任务清单**：
- [ ] 创建服务目录结构（参考项目结构设计）
- [ ] 配置FastAPI框架
- [ ] 集成公共LLM库（aidoctor_llm）
- [ ] 配置Redis连接
- [ ] 实现基础工具类（异常处理、日志、配置）
- [ ] 实现健康检查接口
- [ ] 编写基础单元测试

**目录结构**：
```
dialog-service/
├── app/
│   ├── main.py                 # FastAPI应用入口
│   ├── api/
│   │   └── routes.py           # API路由
│   ├── services/
│   │   └── dialog_service.py   # 对话服务
│   ├── core/
│   │   ├── adaptive_questioning.py # 智能追问
│   │   ├── nlu.py              # 自然语言理解
│   │   └── nlg.py              # 自然语言生成
│   ├── identifiers/
│   │   └── information_gap_identifier.py # 信息缺口识别
│   ├── calculators/
│   │   └── completeness_calculator.py # 完整度计算
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

#### Phase 2: 信息缺口识别实现（1周）

**目标**：实现信息缺口识别和分级功能

**任务清单**：
- [ ] 实现信息缺口识别器（information_gap_identifier.py）
  - [ ] 分析已收集信息
  - [ ] 识别缺失信息
  - [ ] 信息缺口分级（必填/重要/可选）
- [ ] 实现完整度计算器（completeness_calculator.py）
  - [ ] 计算信息完整度
  - [ ] 判断是否满足进入下一步的条件
- [ ] 编写单元测试（覆盖率≥80%）

#### Phase 3: 智能追问生成实现（1周）

**目标**：实现智能追问生成功能

**任务清单**：
- [ ] 实现追问策略生成器（questioning_strategy_generator.py）
  - [ ] 基于信息缺口分级生成追问策略
  - [ ] 优先级排序
- [ ] 实现追问问题生成器（question_generator.py）
  - [ ] 使用LLM生成自然语言问题
  - [ ] 问题格式化
- [ ] 实现自适应追问策略（adaptive_questioning.py）
  - [ ] 根据对话上下文调整追问策略
  - [ ] 避免重复问题
- [ ] 编写单元测试（覆盖率≥80%）

#### Phase 4: 自然语言理解（NLU）实现（1周）

**目标**：实现NLU功能

**任务清单**：
- [ ] 集成公共LLM库
  - [ ] 使用LangChainLLMClient
  - [ ] 使用PromptTemplateManager
- [ ] 设计NLU Prompt模板
  - [ ] 用户输入理解模板
  - [ ] 信息提取模板
- [ ] 实现NLU引擎（nlu.py）
  - [ ] 调用LLM理解用户输入
  - [ ] 提取结构化信息
  - [ ] 信息验证
- [ ] 编写单元测试

#### Phase 5: 自然语言生成（NLG）实现（1周）

**目标**：实现NLG功能

**任务清单**：
- [ ] 设计NLG Prompt模板
  - [ ] 追问问题生成模板
  - [ ] 问题格式化模板
- [ ] 实现NLG引擎（nlg.py）
  - [ ] 调用LLM生成自然语言问题
  - [ ] 问题后处理和验证
  - [ ] 降级策略（LLM失败时使用模板）
- [ ] 编写单元测试

#### Phase 6: 接口实现和集成（1周）

**目标**：实现所有API接口，完成服务集成

**任务清单**：
- [ ] 实现所有API接口（routes.py）
  - [ ] 生成追问问题接口
  - [ ] 理解用户输入接口
  - [ ] 识别信息缺口接口
  - [ ] WebSocket对话接口（可选）
- [ ] 实现请求/响应模型验证
- [ ] 实现错误处理（遵循错误处理规范）
- [ ] 实现API文档（Swagger/OpenAPI）
- [ ] 实现对话上下文管理（Redis）
- [ ] 编写集成测试

#### Phase 7: 测试与优化（1周）

**目标**：完善测试，性能优化

**任务清单**：
- [ ] 完善单元测试（覆盖率≥80%）
- [ ] 完善集成测试
- [ ] 性能测试
- [ ] 优化LLM调用性能
- [ ] 优化Redis缓存策略
- [ ] 编写测试报告

### 6.2 优先级排序

- **P0（必须）**：
  - 信息缺口识别
  - 智能追问生成
  - 基础API接口

- **P1（重要）**：
  - 自然语言理解（NLU）
  - 自然语言生成（NLG）
  - 完整API接口

- **P2（可选）**：
  - WebSocket实时对话
  - 性能优化
  - 高级追问策略

### 6.3 里程碑定义

- **Milestone 1**：基础框架完成（Phase 1）
  - 服务可启动
  - 公共LLM库集成完成
  - Redis连接正常
  - 健康检查接口正常

- **Milestone 2**：信息缺口识别完成（Phase 2）
  - 信息缺口识别功能正常
  - 信息缺口分级正确
  - 完整度计算准确

- **Milestone 3**：智能追问生成完成（Phase 3）
  - 追问生成功能正常
  - 追问策略合理

- **Milestone 4**：NLU完成（Phase 4）
  - NLU功能正常
  - 信息提取准确

- **Milestone 5**：NLG完成（Phase 5）
  - NLG功能正常
  - 问题生成质量高

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

- **核心算法**：信息缺口识别、智能追问生成、NLU、NLG
- **工具类**：异常处理、日志、配置
- **数据模型**：请求/响应模型验证

#### 7.1.2 测试用例设计

**信息缺口识别测试**：
- 测试信息缺口识别
- 测试信息缺口分级
- 测试完整度计算

**智能追问生成测试**：
- 测试追问策略生成
- 测试问题生成
- 测试优先级排序

**NLU测试**：
- 测试用户输入理解
- 测试信息提取
- 测试信息验证

**NLG测试**：
- 测试问题生成
- 测试问题格式化
- 测试降级策略

#### 7.1.3 Mock策略

- **LLM Mock**：使用Mock LLM客户端
- **CDP Mock**：使用Mock CDP数据
- **Redis Mock**：使用Mock Redis客户端

### 7.2 集成测试计划

#### 7.2.1 服务间集成测试

- **与diagnosis-service集成**：
  - 测试CDP读写
  - 测试数据流转

#### 7.2.2 端到端测试

- **完整对话流程测试**：
  - 从CDP读取数据
  - 生成追问问题
  - 理解用户输入
  - 更新CDP
  - 验证结果

### 7.3 性能测试计划

#### 7.3.1 性能测试指标

- **响应时间**：
  - 信息缺口识别：< 1秒
  - 智能追问生成：< 3秒
  - NLU：< 3秒
  - NLG：< 3秒
  - 整体流程：< 5秒

- **并发处理能力**：
  - 目标并发：20+
  - 峰值并发：50+

---

## 八、风险评估

### 8.1 技术风险

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| **LLM API调用失败** | 中 | 降级策略：使用模板生成问题 |
| **信息缺口识别失败** | 低 | 使用默认信息清单 |
| **对话上下文丢失** | 中 | Redis持久化、定期备份 |

### 8.2 业务风险

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| **追问质量不足** | 中 | Prompt优化、追问策略优化 |
| **NLU理解不准确** | 中 | Prompt优化、上下文增强 |

---

## 九、后续优化方向

### 9.1 性能优化

- **LLM调用优化**：缓存、批量处理
- **Redis优化**：连接池、数据压缩

### 9.2 功能扩展

- **多轮对话优化**：上下文理解增强
- **个性化追问**：根据用户背景调整追问策略

---

**文档版本**：v1.0  
**创建日期**：2025年1月  
**维护人员**：开发团队

