# health-state-assessment-service - 服务实现方案

> **文档定位**：本文档定义health-state-assessment-service（健康状态判定服务）的技术实现方案，包括技术选型、接口设计、数据流、实现步骤等。  
> **参考文档**：
> - 《AI医生系统-业务逻辑详细设计.md》- 业务逻辑（核心参考）
> - 《AI医生系统-技术架构设计.md》及相关子文档- 技术架构
> - 《AI医生系统-项目结构设计.md》- 项目结构
> - 《AI医生系统-数据模型设计.md》- 数据模型
> - 《AI医生系统-API接口规范.md》- API规范
> - 《AI医生系统-错误处理规范.md》- 错误处理规范
> - 《AI医生系统-入口设计规范.md》- 入口设计规范

---

## 一、服务概述

> **参考文档**：《AI医生系统-业务逻辑详细设计.md》第1.1节

### 1.1 服务定位

- **对应脑区**：脑区0（健康状态判定）
- **在架构中的位置**：系统入口服务，所有用户请求的第一站，位于双通道推理架构的入口
- **服务职责**：
  1. 执行入口判定流程（P0模块，Step 1-5）
  2. 工作态判定（健康管理态/临床诊疗态）
  3. 危险信号检查（安全兜底，所有用户都要过一次）
  4. 健康管理计划生成（健康管理态时）
  5. CDP创建（可选，或由diagnosis-service创建）

### 1.2 输入输出

#### 1.2.1 输入数据格式和来源

**输入来源**：前端直接调用

**输入数据格式**（参考《AI医生系统-数据模型设计.md》）：
```json
{
  "userId": "user-1",
  "userInput": "我最近胸痛",
  "basicInfo": {
    "age": 30,
    "gender": "男",
    "bmi": 26.5
  },
  "symptoms": ["胸痛"],
  "vitalSigns": {
    "bp": {"systolic": 130, "diastolic": 85},
    "heartRate": 75
  }
}
```

**字段说明**：
- `userId`：用户ID（必填）
- `userInput`：用户自然语言输入（可选，但建议提供）
- `basicInfo`：基本信息（可选）：age, gender, bmi等
- `symptoms`：症状列表（可选）
- `vitalSigns`：生命体征（可选）：bp, heartRate等

#### 1.2.2 输出数据格式和目标

**输出目标**：前端、diagnosis-service（流程编排服务）

**输出数据格式**（参考《AI医生系统-最终输出格式规范.md》）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp-123456",
    "needsClinicalMode": true,
    "workMode": "clinical_mode",
    "riskLevel": "L3",
    "assessmentReason": "检测到症状，建议进入临床诊疗态",
    "entryAssessment": {
      "userInput": "我最近胸痛",
      "hasSymptom": true,
      "symptomStatus": "has_symptom",
      "symptoms": ["胸痛"],
      "clarificationNeeded": false,
      "redFlagsHit": false,
      "redFlagsList": [],
      "pathSelected": "B"
    },
    "redFlags": [],
    "wellnessPlan": null
  },
  "timestamp": 1705123456789
}
```

**字段说明**：
- `cdpId`：CDP ID（用于后续流程）
- `needsClinicalMode`：是否需要临床诊疗态
- `workMode`：工作态（`wellness_mode` / `clinical_mode`）
- `riskLevel`：风险等级（`L1`/`L2`/`L3`/`L4`）
- `entryAssessment`：入口判定详细结果（P0模块）
- `redFlags`：危险信号列表
- `wellnessPlan`：健康管理计划（仅在wellness_mode时返回）

#### 1.2.3 数据流转关系

**数据流转**（参考《AI医生系统-技术架构设计-CDP数据与状态管理.md》）：
```
前端 → health-state-assessment-service → diagnosis-service（CDP管理）
                                      ↓
                                  返回结果给前端
```

**CDP数据更新**：
- 如果由本服务创建CDP，则创建初始CDP记录
- 更新CDP的`health_state_assessment`字段
- 更新CDP的`work_mode`字段
- 更新CDP的`risk_level`字段

### 1.3 业务价值

- **系统入口判定**：决定用户进入哪个路径（健康筛查路径A / 症状诊断路径B）
- **安全兜底**：识别危险信号并提示就医，避免线上诊断处理紧急情况
- **工作态判定**：区分健康管理和临床诊疗场景，提供不同的服务流程
- **风险分级**：对用户进行风险等级评估，为后续流程提供依据

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
python-multipart==0.0.6  # 文件上传支持

# 可选：LLM集成（用于NLU和方向澄清）
# openai==1.3.0  # 或其他LLM API客户端
```

**技术选型理由**：
- FastAPI：现代、高性能的Python Web框架
- Pydantic：类型安全的数据验证
- 可选LLM：用于自然语言理解和方向澄清（Step 3）

### 2.2 核心算法/方法

#### 2.2.1 入口判定流程（P0模块）

**算法描述**：通过5个步骤完成用户意图识别、症状识别、方向澄清、危险信号检查和路径输出。

**实现思路**：

1. **Step 1：接收用户输入**
   - 接收用户自然语言输入
   - 提取基本信息（可选）
   - 判断是否为混合诉求

2. **Step 2：识别症状/困扰**
   - 使用关键词匹配识别症状
   - 识别健康管理/体检规划表达
   - 判断三种情况：
     - 情况A：明确无症状 → 进入健康筛查路径
     - 情况B：存在症状/困扰 → 进入症状诊断路径
     - 情况C：不确定/模糊/混合诉求 → 进入Step 3

3. **Step 3：方向澄清**（仅对情况C触发）
   - 生成最小澄清问题
   - 等待用户回答（在实际实现中，这里是异步的）
   - 根据用户回答确定方向（A或B）

4. **Step 4：危险信号检查**（所有用户都要过一次）
   - 使用红旗信号库匹配
   - 检查高危症状组合
   - 如果命中危险信号，退出线上流程

5. **Step 5：输出路径结果**
   - 根据前面的判断结果确定路径
   - 输出路径选择结果

**算法复杂度**：O(n)，n为用户输入文本长度

#### 2.2.2 工作态判定算法

**算法描述**：基于入口判定结果、症状严重程度、风险信号、危险信号等，判定工作态。

**实现思路**：

1. **规则1**：出现红旗信号 → 立即进入临床诊疗态
2. **规则2**：检查生命体征异常 → 进入临床诊疗态
3. **规则3**：症状严重程度高 → 进入临床诊疗态
4. **规则4**：风险等级≥L2 → 进入临床诊疗态
5. **规则5**：症状在正常范围且风险等级低（L3/L4） → 进入健康管理态
6. **规则6**：不确定时，优先进入临床诊疗态（宁可误报，不能漏报）

**算法复杂度**：O(1)

#### 2.2.3 风险等级计算算法

**算法描述**：根据症状严重程度、风险信号、危险信号、生命体征等，计算风险等级。

**风险等级定义**：
- **L1**：极高风险（需立即处理）- 有红旗信号 + 生命体征异常
- **L2**：高风险（需尽快处理）- 有红旗信号或症状严重程度高
- **L3**：中风险（建议关注）- 症状严重程度中等或存在风险因素
- **L4**：低风险（正常管理）- 症状在正常范围

**算法复杂度**：O(1)

### 2.3 性能要求

> **参考文档**：《AI医生系统-技术架构设计-性能与评估.md》

#### 2.3.1 响应时间要求

- **入口判定（P0模块）**：< 1秒
- **健康状态判定（完整流程）**：< 2秒
- **目标**：< 500ms（优化后）

#### 2.3.2 并发处理能力

- **支持并发请求**：100+ 并发
- **目标**：500+ 并发（优化后）

#### 2.3.3 资源消耗限制

- **内存消耗**：< 512MB（单实例）
- **CPU消耗**：< 50%（正常负载）

### 2.4 关键技术点

#### 2.4.1 关键技术难点

1. **自然语言理解（NLU）**
   - 难点：准确识别用户意图和症状
   - 解决方案：
     - 使用关键词匹配（初期）
     - 可选：集成LLM API（如OpenAI）进行NLU（后期优化）

2. **危险信号识别**
   - 难点：准确识别高危症状组合，避免漏检
   - 解决方案：
     - 维护完整的红旗信号库
     - 使用规则引擎匹配
     - 定期更新红旗信号库

3. **方向澄清（Step 3）**
   - 难点：生成合适的澄清问题，避免反复追问
   - 解决方案：
     - 仅对"情况C"触发一次
     - 使用LLM生成澄清问题（可选）
     - 提供选项按钮，方便用户快速选择

#### 2.4.2 技术风险及应对

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| LLM API调用失败 | 中 | 降级到规则引擎，使用关键词匹配 |
| 危险信号漏检 | 高 | 完善红旗信号库，定期更新，宁可误报不能漏报 |
| 性能瓶颈 | 中 | 使用缓存，优化算法，异步处理 |
| 并发冲突 | 低 | 使用无状态设计，避免共享状态 |

---

## 三、接口设计

> **参考文档**：
> - 《AI医生系统-API接口规范.md》
> - 《AI医生系统-错误处理规范.md》
> - 《AI医生系统-数据模型设计.md》

### 3.1 API端点定义

#### 3.1.1 健康状态判定接口

**接口路径**：`POST /api/v1/health-state-assessment/assess`

**接口描述**：健康状态判定（脑区0），整合入口判定流程（P0模块，Step 1-5）和工作态判定。

**请求方法**：POST

**URL路径设计**（遵循API接口规范）：
- 使用RESTful风格
- 版本号：`/api/v1`
- 资源路径：`/health-state-assessment/assess`

**请求头**：
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**：
```json
{
  "userId": "user-1",
  "userInput": "我最近胸痛",
  "basicInfo": {
    "age": 30,
    "gender": "男",
    "bmi": 26.5
  },
  "symptoms": ["胸痛"],
  "vitalSigns": {
    "bp": {"systolic": 130, "diastolic": 85},
    "heartRate": 75
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
    "needsClinicalMode": true,
    "workMode": "clinical_mode",
    "riskLevel": "L3",
    "assessmentReason": "检测到症状，建议进入临床诊疗态",
    "entryAssessment": {
      "userInput": "我最近胸痛",
      "hasSymptom": true,
      "symptomStatus": "has_symptom",
      "symptoms": ["胸痛"],
      "clarificationNeeded": false,
      "redFlagsHit": false,
      "redFlagsList": [],
      "pathSelected": "B"
    },
    "redFlags": [],
    "wellnessPlan": null
  },
  "timestamp": 1705123456789
}
```

**响应体**（危险信号命中）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp-123456",
    "needsClinicalMode": true,
    "workMode": "clinical_mode",
    "riskLevel": "L1",
    "assessmentReason": "检测到危险信号，建议立即就医",
    "entryAssessment": {
      "userInput": "我胸痛，还出汗，感觉喘不上气",
      "hasSymptom": true,
      "symptomStatus": "has_symptom",
      "symptoms": ["胸痛", "出汗", "呼吸困难"],
      "clarificationNeeded": false,
      "redFlagsHit": true,
      "redFlagsList": ["高危症状组合：胸痛、气短、出汗"],
      "pathSelected": "exit"
    },
    "redFlags": ["高危症状组合：胸痛、气短、出汗"],
    "wellnessPlan": null
  },
  "timestamp": 1705123456789
}
```

### 3.2 请求/响应模型

#### 3.2.1 请求模型定义

**参考文档**：《AI医生系统-数据模型设计.md》

```python
class HealthStateAssessmentRequest(BaseModel):
    """健康状态判定请求"""
    userId: str  # 用户ID（必填）
    userInput: Optional[str] = None  # 用户原始输入文本
    basicInfo: Optional[Dict[str, Any]] = None  # 基本信息
    symptoms: Optional[List[str]] = None  # 症状列表
    vitalSigns: Optional[Dict[str, Any]] = None  # 生命体征
```

#### 3.2.2 响应模型定义

```python
class EntryAssessmentResult(BaseModel):
    """入口判定结果"""
    userInput: str
    hasSymptom: bool
    symptomStatus: str  # "no_symptom" | "has_symptom" | "uncertain"
    symptoms: List[str]
    clarificationNeeded: bool
    clarificationResult: Optional[Dict[str, Any]] = None
    redFlagsHit: bool
    redFlagsList: List[str]
    pathSelected: str  # "A" | "B" | "exit"

class WellnessPlan(BaseModel):
    """健康管理计划"""
    riskManagement: str
    lifestyleAdvice: str
    followUpPlan: str

class HealthStateAssessmentResponse(BaseModel):
    """健康状态判定响应"""
    entryAssessment: Optional[EntryAssessmentResult] = None
    needsClinicalMode: bool
    workMode: str  # "clinical_mode" | "wellness_mode"
    riskLevel: str  # "L1" | "L2" | "L3" | "L4"
    assessmentReason: str
    redFlags: Optional[List[str]] = []
    wellnessPlan: Optional[WellnessPlan] = None
    cdpId: str
```

#### 3.2.3 数据验证规则

**遵循API接口规范**：
- 使用Pydantic进行数据验证
- `userId`：必填，字符串，长度1-100
- `userInput`：可选，字符串，最大长度5000
- `basicInfo`：可选，字典
- `symptoms`：可选，字符串列表
- `vitalSigns`：可选，字典

### 3.3 错误码定义

> **参考文档**：《AI医生系统-错误处理规范.md》

**错误码范围**：1000-1099（health-state-assessment-service）

| 错误码 | 说明 | HTTP状态码 | 是否可重试 |
|--------|------|------------|-----------|
| 1001 | 入口判定失败 | 500 | false |
| 1002 | 危险信号检查失败 | 500 | false |
| 1003 | 工作态判定失败 | 500 | false |
| 1004 | 健康管理计划生成失败 | 500 | false |
| 1005 | 用户输入为空 | 400 | false |
| 1006 | 用户ID不能为空 | 400 | false |
| 1007 | 参数验证失败 | 400 | false |

**错误处理策略**（遵循错误处理规范）：
- 统一使用全局异常处理器
- 所有错误都要记录日志
- 错误信息对用户友好，不暴露系统内部信息
- 错误必须包含traceId、cdpId等追踪信息

### 3.4 API文档

**Swagger/OpenAPI文档**：
- 自动生成：FastAPI自动生成Swagger文档
- 访问路径：`http://localhost:8000/docs`
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

**上游服务**：前端直接调用

**数据格式**（参考数据模型设计）：
- JSON格式
- 字段使用camelCase（对外API）

**数据获取方式**：
- HTTP POST请求
- 请求体包含用户输入、基本信息、症状列表、生命体征等

### 4.2 数据处理流程

**数据处理步骤**（参考业务逻辑详细设计）：

1. **接收用户输入**
   - 验证请求参数
   - 提取用户输入、基本信息、症状列表、生命体征

2. **执行入口判定（P0模块，Step 1-5）**
   - Step 1：接收用户输入
   - Step 2：识别症状/困扰
   - Step 3：方向澄清（仅对情况C触发）
   - Step 4：危险信号检查
   - Step 5：路径选择

3. **工作态判定**
   - 症状严重程度评估
   - 风险早筛
   - 红旗信号识别
   - 工作态判定
   - 风险等级计算

4. **生成健康管理计划**（仅健康管理态）
   - 风险识别
   - 生活方式建议
   - 随访计划

5. **生成CDP ID**
   - 创建或获取CDP ID

6. **构建响应**
   - 组装响应数据
   - 返回统一格式响应

**数据转换逻辑**：
- 用户输入文本 → 结构化数据（症状列表、意图等）
- 入口判定结果 → 工作态判定依据
- 工作态判定结果 → 响应数据

**数据验证规则**（参考数据模型设计）：
- 使用Pydantic进行数据验证
- 验证用户输入格式
- 验证基本信息格式
- 验证症状列表格式
- 验证生命体征格式

### 4.3 数据输出格式

**输出数据结构**（参考数据模型设计）：
- JSON格式
- 字段使用camelCase（对外API）

**数据格式规范**（遵循最终输出格式规范）：
- 统一响应格式：`{code, message, data, timestamp}`
- 数据字段：`{cdpId, needsClinicalMode, workMode, riskLevel, ...}`

**数据存储方式**（参考CDP数据与状态管理）：
- CDP数据存储在diagnosis-service（MySQL/Oracle）
- 本服务不直接存储数据，仅返回结果
- CDP字段使用snake_case（内部存储）

### 4.4 CDP数据流转

**CDP字段更新**（参考CDP数据与状态管理）：

1. **创建CDP**（可选，或由diagnosis-service创建）
   - 创建初始CDP记录
   - 设置CDP的`user_id`、`session_id`等

2. **更新CDP的`health_state_assessment`字段**：
   ```json
   {
     "needs_clinical_mode": true,
     "work_mode": "clinical_mode",
     "risk_level": "L3",
     "assessment_reason": "检测到症状，建议进入临床诊疗态",
     "red_flags": [],
     "wellness_plan": null
   }
   ```

3. **更新CDP的`entry_assessment`字段**（入口判定结果）：
   ```json
   {
     "user_input": "我最近胸痛",
     "has_symptom": true,
     "symptom_status": "has_symptom",
     "symptoms": ["胸痛"],
     "clarification_needed": false,
     "red_flags_hit": false,
     "red_flags_list": [],
     "path_selected": "B"
   }
   ```

**CDP版本控制**（参考CDP数据与状态管理）：
- 使用乐观锁机制（version字段）
- 每次更新创建新版本
- 版本号自增

**CDP状态流转**（参考CDP数据与状态管理）：
- 初始状态：`INITIAL`
- 入口判定完成：`ENTRY_ASSESSED`
- 工作态判定完成：`WORK_MODE_DETERMINED`

---

## 五、依赖关系

### 5.1 依赖的其他服务

#### 5.1.1 上游服务

**无**：本服务是入口服务，直接接收前端请求

#### 5.1.2 下游服务

**diagnosis-service**（可选）：
- **依赖关系**：CDP管理
- **调用方式**：HTTP同步调用（可选）
- **说明**：如果CDP由diagnosis-service管理，则不需要调用；如果由本服务创建CDP，则需要调用diagnosis-service的CDP创建接口

### 5.2 依赖的外部资源

#### 5.2.1 数据库

**无**：本服务不直接访问数据库，数据通过CDP流转

#### 5.2.2 缓存

**Redis**（可选）：
- **用途**：缓存入口判定结果、红旗信号库等
- **说明**：可选，用于性能优化

#### 5.2.3 外部API

**LLM API**（可选）：
- **用途**：自然语言理解（NLU）、方向澄清（Step 3）
- **说明**：可选，初期使用规则引擎，后期可集成LLM API

### 5.3 依赖管理

#### 5.3.1 依赖版本管理

**使用requirements.txt管理依赖**：
```
fastapi==0.104.1
uvicorn[standard]==0.24.0
pydantic==2.5.0
python-dotenv==1.0.0
python-multipart==0.0.6
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

#### Phase 1: 基础框架搭建（3天）

**目标**：搭建服务基础框架，配置开发环境

**任务清单**：
- [ ] 创建服务目录结构（参考项目结构设计）
  - `app/main.py`：FastAPI应用入口
  - `app/api/routes.py`：API路由
  - `app/services/`：业务服务
  - `app/models/`：数据模型
  - `app/utils/`：工具类
  - `app/config/`：配置
- [ ] 配置FastAPI框架
  - 创建FastAPI应用实例
  - 配置CORS、中间件等
  - 配置日志
- [ ] 实现基础工具类
  - 异常处理工具
  - 日志工具
  - 响应格式化工具
- [ ] 配置日志和错误处理（参考错误处理规范）
  - 配置日志格式
  - 实现全局异常处理器
  - 实现统一响应格式

**验收标准**：
- 服务可以启动
- 健康检查接口正常
- 日志输出正常

#### Phase 2: 入口判定流程实现（5天）

**目标**：实现入口判定流程（P0模块，Step 1-5）

**任务清单**：
- [ ] Step 1：接收用户输入
  - 实现`step1_receive_input.py`
  - 接收用户自然语言输入
  - 提取基本信息
  - 判断是否为混合诉求
- [ ] Step 2：识别症状/困扰
  - 实现`step2_identify_symptom.py`
  - 使用关键词匹配识别症状
  - 识别健康管理/体检规划表达
  - 判断三种情况（A/B/C）
- [ ] Step 3：方向澄清
  - 实现`step3_clarification.py`
  - 生成最小澄清问题
  - 根据用户回答确定方向
  - 仅对"情况C"触发一次
- [ ] Step 4：危险信号检查
  - 实现`step4_red_flag_check.py`
  - 使用红旗信号库匹配
  - 检查高危症状组合
  - 所有用户都要过一次
- [ ] Step 5：路径选择
  - 实现`step5_path_selection.py`
  - 根据前面的判断结果确定路径
  - 输出路径选择结果
- [ ] 整合入口判定流程
  - 实现`entry_assessment/__init__.py`
  - 整合5个步骤
  - 构建入口判定结果

**验收标准**：
- 5个步骤都可以独立运行
- 入口判定流程完整
- 单元测试通过

#### Phase 3: 工作态判定实现（2天）

**目标**：实现工作态判定逻辑

**任务清单**：
- [ ] 症状严重程度评估
  - 实现`detectors/symptom_severity_detector.py`
  - 使用症状严重程度判定规则库（0.1）
  - 判断症状是否在正常范围
- [ ] 风险早筛
  - 实现`detectors/risk_signal_detector.py`
  - 使用早期风险信号识别库（0.2）
  - 识别早期风险信号（家族史、行为、慢性暴露）
- [ ] 红旗信号识别
  - 实现`detectors/red_flag_detector.py`
  - 使用红旗信号库（0.3）
  - 识别高危症状组合
- [ ] 工作态判定
  - 实现`rules/work_mode_rules.py`
  - 使用工作态判定规则库（0.4）
  - 判定工作态（健康管理态/临床诊疗态）
- [ ] 风险等级计算
  - 实现风险等级计算逻辑
  - 根据症状严重程度、风险信号、危险信号等计算风险等级

**验收标准**：
- 工作态判定逻辑正确
- 风险等级计算准确
- 单元测试通过

#### Phase 4: 健康管理计划生成（1天）

**目标**：实现健康管理计划生成逻辑

**任务清单**：
- [ ] 健康管理计划生成
  - 实现`services/wellness_plan_generator.py`
  - 使用健康管理计划生成规则（0.5）
  - 生成风险识别、生活方式建议、随访计划
- [ ] 仅在健康管理态时生成

**验收标准**：
- 健康管理计划生成逻辑正确
- 单元测试通过

#### Phase 5: 接口实现（2天）

**目标**：实现API接口

**任务清单**：
- [ ] 实现API接口（遵循API接口规范）
  - 实现`POST /api/v1/health-state-assessment/assess`
  - 实现请求/响应模型（参考数据模型设计）
  - 实现统一响应格式
- [ ] 实现错误处理（遵循错误处理规范）
  - 实现全局异常处理器
  - 实现错误码定义（1000-1099）
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

#### Phase 6: 集成与优化（2天）

**目标**：服务间集成测试和性能优化

**任务清单**：
- [ ] 服务间集成测试
  - 与前端集成测试
  - 与diagnosis-service集成测试（CDP管理）
- [ ] 性能优化（参考性能与评估）
  - 优化入口判定流程性能
  - 使用缓存减少重复计算
  - 异步处理优化
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
  - 入口判定流程（Step 1-5）
  - 工作态判定
  - API接口实现
  - 错误处理

- **P1（重要）**：
  - 健康管理计划生成
  - 性能优化
  - 单元测试

- **P2（可选）**：
  - LLM集成（NLU和方向澄清）
  - 缓存优化
  - 监控和告警

### 6.3 里程碑定义

- **Milestone 1**：基础框架完成（Phase 1）
  - 服务可以启动
  - 健康检查接口正常

- **Milestone 2**：入口判定流程完成（Phase 2）
  - 5个步骤都可以独立运行
  - 入口判定流程完整

- **Milestone 3**：工作态判定完成（Phase 3）
  - 工作态判定逻辑正确
  - 风险等级计算准确

- **Milestone 4**：接口实现完成（Phase 5）
  - API接口符合规范
  - 错误处理完善

- **Milestone 5**：集成测试通过（Phase 6）
  - 集成测试通过
  - 性能满足要求

---

## 七、测试策略

### 7.1 单元测试计划

**测试覆盖范围**：
- 入口判定流程（Step 1-5）
- 工作态判定逻辑
- 风险等级计算
- 健康管理计划生成

**测试用例设计**：

1. **Step 1测试用例**：
   - 正常用户输入
   - 空输入
   - 混合诉求

2. **Step 2测试用例**：
   - 情况A：明确无症状
   - 情况B：存在症状/困扰
   - 情况C：不确定/模糊/混合诉求

3. **Step 3测试用例**：
   - 情况C触发澄清
   - 用户选择A路径
   - 用户选择B路径

4. **Step 4测试用例**：
   - 命中危险信号
   - 未命中危险信号
   - 高危症状组合

5. **Step 5测试用例**：
   - 路径A选择
   - 路径B选择
   - 危险信号退出

6. **工作态判定测试用例**：
   - 有红旗信号 → 临床诊疗态
   - 症状严重程度高 → 临床诊疗态
   - 无症状 + 低风险 → 健康管理态

**Mock策略**：
- Mock LLM API（如使用）
- Mock外部服务调用（如diagnosis-service）

**测试框架**：
- pytest
- pytest-asyncio（异步测试）
- pytest-mock（Mock支持）

**测试覆盖率要求**：≥80%

### 7.2 集成测试计划

**服务间集成测试**：
- 与前端集成测试
  - 测试API接口调用
  - 测试响应格式
  - 测试错误处理
- 与diagnosis-service集成测试（CDP管理）
  - 测试CDP创建（如需要）
  - 测试CDP更新（如需要）

**数据流测试**：
- 测试完整的数据流转
- 测试CDP数据更新
- 测试响应数据格式

**端到端测试**：
- 测试完整的入口判定流程
- 测试工作态判定流程
- 测试健康管理计划生成流程

### 7.3 性能测试计划

**性能测试指标**（参考性能与评估）：
- 响应时间：< 1秒（入口判定）
- 并发处理：100+ 并发
- 资源消耗：内存 < 512MB

**性能测试场景**：
- 单用户请求
- 并发请求（10/50/100/200）
- 不同输入长度（短/中/长）
- 不同症状数量（0/1/5/10）

**性能优化目标**：
- 响应时间：< 500ms（优化后）
- 并发处理：500+ 并发（优化后）

### 7.4 测试数据准备

**测试数据设计**：
- 正常用户输入数据
- 边界情况数据（空输入、超长输入等）
- 危险信号数据（各种高危症状组合）
- 不同工作态数据（健康管理态/临床诊疗态）

**测试数据管理**：
- 使用测试数据文件（JSON/YAML）
- 使用Fixture管理测试数据
- 测试数据隔离（不影响生产数据）

**测试环境配置**：
- 独立的测试环境
- 测试数据库（如需要）
- Mock外部服务

---

## 八、风险评估

### 8.1 技术风险

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| LLM API调用失败 | 中 | 降级到规则引擎，使用关键词匹配 |
| 危险信号漏检 | 高 | 完善红旗信号库，定期更新，宁可误报不能漏报 |
| 性能瓶颈 | 中 | 使用缓存，优化算法，异步处理 |
| 并发冲突 | 低 | 使用无状态设计，避免共享状态 |
| 依赖服务不可用 | 中 | 实现熔断机制，降级处理 |

### 8.2 业务风险

| 风险 | 风险等级 | 应对措施 |
|------|---------|---------|
| 危险信号漏检 | 高 | 完善红旗信号库，定期更新，宁可误报不能漏报 |
| 工作态判定错误 | 中 | 完善判定规则，增加测试用例 |
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
- 算法优化（如使用更高效的匹配算法）

**优化计划**：
- Phase 1：实现基础缓存（Redis）
- Phase 2：优化入口判定流程算法
- Phase 3：异步处理优化

### 9.2 功能扩展

**扩展方向**：
- 集成LLM API（NLU和方向澄清）
- 支持多模态输入（图片、语音）
- 支持多语言

**扩展计划**：
- Phase 1：集成LLM API（OpenAI或其他）
- Phase 2：支持图片输入（OCR识别）
- Phase 3：支持语音输入（语音识别）

### 9.3 可观测性

**监控和告警**：
- 接口性能监控
- 错误率监控
- 业务指标监控（入口判定成功率、危险信号识别率等）

**日志和追踪**：
- 结构化日志
- 分布式追踪（traceId）
- 业务日志分析

---

**文档版本**：v1.0  
**创建日期**：2025年1月  
**维护人员**：开发团队

