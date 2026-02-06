# AI医生系统 - 技术架构设计（项目实现与部署）

> **文档定位**：本文档是AI医生系统的**技术架构设计**的项目实现与部署部分，包含项目结构设计、技术栈选型、性能优化、部署架构和健康状态判定服务技术实现。  
> **业务功能**：请参考《AI医生系统-系统功能设计.md》  
> **核心目标**：详细说明项目结构、技术选型、性能优化策略和部署方案。

---

## 八、项目结构设计

> **设计原则**：基于服务架构设计，按照功能模块（工具）来组织项目结构，每个服务对应一个工具，职责单一明确。  
> **参考文档**：《AI医生系统-系统功能设计.md》第2章 工具功能设计

### 8.1 项目结构设计原则

#### 8.1.1 服务拆分原则

**原则1：按工具拆分服务**
- 每个服务对应一个工具（Tool）
- 服务职责单一，边界清晰
- 服务可以独立开发、测试、部署

**原则2：服务与工具一一对应**
- 每个服务实现一个工具的功能
- 服务内部可以包含多个工具实例（负载均衡）
- 工具通过服务API对外提供服务

**原则3：主Agent服务独立**
- 主Agent（Clinical Agent Brain）作为独立服务（diagnosis-service）
- 主Agent负责运行循环、工具调用、决策提交
- 工具服务只负责执行并返回结果

**原则3：技术栈选择**
- Java服务：使用Spring Boot框架，适合复杂业务逻辑
- Python服务：使用FastAPI框架，适合AI/ML模型调用
- 数据库：MySQL/Oracle存储业务数据，Neo4j存储知识图谱

#### 8.1.2 项目目录结构

**根目录结构**：

```
AIdoctor/
├── clinical-parsing-service/          # tool_1：病例理解服务（Python）
├── diagnosis-engine-service/           # tool_3：鉴别诊断服务（Python）
├── dialog-service/                    # tool_2：主动问诊服务（Python）
├── workup-planner-service/            # tool_4：检查建议服务（Python）
├── treatment-engine-service/          # tool_5：治疗建议服务（Python）
├── risk-assessment-service/           # tool_6：风险评估服务（Python）
├── explanation-service/               # tool_7：证据链服务（Python）
├── health-state-assessment-service/   # tool_0：健康状态判定服务（Python）
├── diagnosis-service/                 # 主Agent服务（Java）
├── examination-service/              # 检查服务（Java）
├── ocr-service/                      # OCR服务（Python）
├── frontend/                         # 前端服务（React/TypeScript）
├── docker-compose.yml                # Docker编排文件
└── docs/                             # 文档目录
```

#### 8.1.3 单个服务目录结构（Python服务示例）

**Python服务标准结构**：

```
service-name/
├── app/
│   ├── __init__.py
│   ├── main.py                      # FastAPI应用入口
│   ├── api/                         # API路由
│   │   ├── __init__.py
│   │   └── routes.py
│   ├── services/                    # 业务逻辑层
│   │   ├── __init__.py
│   │   └── service_name_service.py
│   ├── models/                      # 数据模型
│   │   ├── __init__.py
│   │   ├── request.py
│   │   └── response.py
│   ├── config/                      # 配置管理
│   │   ├── __init__.py
│   │   └── settings.py
│   └── utils/                       # 工具类
│       ├── __init__.py
│       └── exceptions.py
├── tests/                           # 测试代码
├── requirements.txt                 # Python依赖
├── Dockerfile                       # Docker镜像构建
├── README.md                        # 服务说明文档
└── run.py                           # 服务启动脚本
```

**Java服务标准结构**：

```
service-name/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/aidoctor/service/
│   │   │       ├── ServiceApplication.java
│   │   │       ├── controller/      # REST控制器
│   │   │       ├── service/         # 业务逻辑层
│   │   │       ├── repository/     # 数据访问层
│   │   │       ├── entity/         # 实体类
│   │   │       ├── dto/            # 数据传输对象
│   │   │       └── config/         # 配置类
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-dev.yml
│   └── test/
│       └── java/
├── pom.xml                          # Maven依赖管理
├── Dockerfile
└── README.md
```

#### 8.1.4 服务与代码模块对应关系

| 服务 | 对应工具 | 工具ID | 主要代码模块 | 技术栈 |
|------|---------|--------|------------|--------|
| health-state-assessment-service | 健康状态判定工具 | tool_0 | `app/services/health_state_service.py` | Python/FastAPI |
| clinical-parsing-service | 病例理解工具 | tool_1 | `app/services/parsing_service.py` | Python/FastAPI |
| dialog-service | 主动问诊工具 | tool_2 | `app/services/dialog_service.py` | Python/FastAPI |
| diagnosis-engine-service | 鉴别诊断工具 | tool_3 | `app/services/diagnosis_service.py`<br>`app/engines/diagnosis_engine.py`<br>`app/engines/knowledge_base_retriever.py`<br>`app/engines/drknows_path_retriever.py`<br>`app/engines/path_constrained_reasoning.py` | Python/FastAPI |
| workup-planner-service | 检查建议工具 | tool_4 | `app/services/workup_service.py` | Python/FastAPI |
| treatment-engine-service | 治疗建议工具 | tool_5 | `app/services/treatment_service.py` | Python/FastAPI |
| risk-assessment-service | 风险评估工具 | tool_6 | `app/services/risk_service.py` | Python/FastAPI |
| explanation-service | 证据链工具 | tool_7 | `app/services/explanation_service.py` | Python/FastAPI |
| diagnosis-service | 主Agent（Clinical Agent Brain） | agent_main | `com.aidoctor.diagnosis.agent`<br>`com.aidoctor.diagnosis.agent_loop`<br>`com.aidoctor.diagnosis.tool_caller`<br>`com.aidoctor.diagnosis.evidence_fusion` | Java/Spring Boot |

#### 8.1.5 数据库设计对应关系

**数据库表与CDP字段对应**：

| CDP字段（功能设计文档） | 数据库表（技术架构文档） | 说明 |
|----------------------|----------------------|------|
| `health_state_assessment` | `cdp.health_state_assessment` (JSON) | 健康状态判定结果 |
| `wellness_plan` | `cdp.wellness_plan` (JSON) | 健康管理计划 |
| `patient_state` | `cdp.patient_state` (JSON) | 患者状态 |
| `ddx` | `cdp.ddx` (JSON) | 鉴别诊断列表 |
| `evidence_graph` | `cdp.evidence_graph` (JSON) | 证据图 |
| `workup_plan` | `cdp.workup_plan` (JSON) | 检查计划 |
| `management_plan` | `cdp.management_plan` (JSON) | 治疗计划 |
| `triage` | `cdp.triage` (JSON) | 风险评估 |
| `uncertainty` | `cdp.uncertainty` (JSON) | 不确定性信息 |
| `audit` | `cdp.audit` (JSON) | 审计信息 |

**知识库表设计**：

| 知识库 | 数据库表 | 对应功能设计文档章节 |
|--------|---------|-------------------|
| 主诉关键线索库 | `chief_complaint_clue_library` | 2.2.1 主诉关键线索库 |
| 差异点词库 | `difference_point_library` | 2.2.2 差异点词库与问法规范 |
| 分流路径模板库 | `routing_path_template` | 2.3.3 分流路径模板库 |
| 复评与升级规则库 | `review_and_upgrade_rule` | 2.6.1 复评与升级规则库 |
| 验证计划库 | `verification_plan_library` | 2.4.1 验证计划制定 |

#### 8.1.6 从文档到代码的实现路径

**实现路径示例：从功能设计到代码实现**

```
功能设计文档（2.2 主动问诊工具）
    ↓
技术架构文档（工具清单：tool_2 + 核心技术组件：主动问诊工具）
    ↓
服务设计（dialog-service）
    ↓
代码实现：
  - app/services/dialog_service.py（业务逻辑）
  - app/api/routes.py（API接口）
  - app/models/request.py（请求模型）
  - app/models/response.py（响应模型）
```

**具体实现步骤**：

1. **阅读功能设计文档**：理解主动问诊工具的功能定义和职责
2. **阅读技术架构文档**：理解tool_2的实现方式和对话管理服务的技术细节
3. **设计服务接口**：定义API接口（RESTful API）
4. **实现业务逻辑**：实现信息缺口识别、问诊生成等功能
5. **实现数据访问**：访问主诉关键线索库、差异点词库等
6. **编写测试**：编写单元测试和集成测试

#### 8.1.7 项目搭建检查清单

**基于两个文档搭建项目的检查清单**：

- [ ] **服务拆分**：是否按照8个服务拆分（对应8个工具）
- [ ] **服务依赖**：是否按照技术架构文档中的依赖关系设计服务调用
- [ ] **CDP数据结构**：是否按照功能设计文档中的CDP字段设计数据库表
- [ ] **API设计**：是否按照技术架构文档中的API设计实现接口
- [ ] **知识库表**：是否创建了所有必需的知识库表（线索库、词库、规则库等）
- [ ] **流程编排**：是否按照5步诊断流程实现流程编排
- [ ] **工具实现**：是否按照工具设计实现各个服务
- [ ] **双通道架构**：是否实现了双通道推理架构（结构化推理+语言策略）

---

## 九、技术栈选型

### 9.1 后端技术栈

**Java服务层**（业务服务）：
- **Web框架**：Spring Boot 2.7.8
- **服务治理**：Spring Cloud Gateway + Nacos（可选）
- **ORM**：JPA / Hibernate
- **服务调用**：OpenFeign
- **数据库**：MySQL 8.0（本地开发）/ Oracle（生产环境）

**Python服务层**（AI服务）：
- **Web框架**：FastAPI
- **异步框架**：asyncio + aiohttp
- **NLP**：transformers（BERT/ClinicalBERT）、spaCy
- **图神经网络**：PyTorch Geometric（SGIN实现）
- **机器学习**：scikit-learn、XGBoost/LightGBM

### 9.2 数据存储

- **关系型数据库**：
  - MySQL 8.0（本地开发推荐）
  - Oracle（生产环境，与同事共同使用）
  - 用途：CDP存储、诊断记录、检查记录、健康状态判定记录、健康筛查记录等
- **图数据库**：Neo4j 5（知识图谱，DR.KNOWS核心依赖）
- **缓存**：Redis 7（CDP临时状态、会话管理）

### 9.3 模型与工具

- **医学概念识别**：QuickUMLS / 中文医学NER模型
- **大语言模型**：T5 / ChatGPT / 医学专用LLM（用于路径注入和NLG）
- **图遍历算法**：Neo4j Cypher查询 + 图遍历算法（DR.KNOWS方法）
- **OCR**：PaddleOCR / Tesseract

---

## 十、性能优化

### 10.1 路径检索优化

- **路径缓存**：Redis缓存常用路径
- **并行计算**：多症状路径并行搜索
- **智能剪枝**：基于关系权重的路径过滤

### 10.2 CDP性能优化

- **CDP缓存**：Redis缓存CDP临时状态
- **版本管理优化**：只保存关键版本，不保存每次更新
- **批量更新**：合并多个字段更新为一次操作

### 10.3 LLM推理优化

- **Prompt缓存**：缓存常用prompt模板
- **批量推理**：批量处理多个请求
- **模型量化**：使用量化模型减少推理时间
- **路径约束**：通过知识图谱路径约束LLM生成，减少无效推理

### 10.4 工具性能优化

#### 10.4.1 并行执行优化

**并行策略**：

1. **任务并行**：多个独立任务并行执行
   - 不同患者的任务分配给不同工具实例并行处理
   - 同一患者的不同阶段任务可以并行执行

2. **数据并行**：同一任务的不同数据并行处理
   - 多个诊断候选并行评估
   - 多个检查建议并行生成

3. **流水线并行**：工具间形成流水线
   - 病例理解工具处理完立即传递给问诊工具
   - 诊断工具处理完立即传递给检查建议工具

**示例**：

```
【病例理解工具】处理患者A
【主动问诊工具】处理患者B
【鉴别诊断工具】处理患者C
（三个工具并行工作）
```

#### 10.4.2 工具缓存机制

**缓存策略**：

1. **CDP缓存**：缓存常用CDP状态，减少重复计算
2. **推理结果缓存**：缓存常用推理结果（如常见症状的诊断路径）
3. **消息缓存**：缓存常用消息模板，减少消息构建时间
4. **工具结果缓存**：缓存工具执行结果，减少重复计算

#### 10.4.3 工具负载均衡

**负载均衡策略**：

1. **轮询**：轮流分配任务给工具实例
2. **最少连接**：分配给连接数最少的工具实例
3. **最快响应**：分配给响应最快的工具实例
4. **能力匹配**：分配给最适合的工具实例（基于任务类型和工具能力）

**工具实例管理**：

- 支持工具实例的动态扩缩容
- 根据负载情况自动增加或减少工具实例
- 支持工具实例的健康检查和故障转移

#### 10.4.4 工具调用优化

**调用优化策略**：

1. **异步调用**：工具间异步调用，不阻塞等待
2. **批量调用**：批量处理工具调用请求，减少通信开销
3. **智能路由**：根据任务类型智能路由到最合适的工具
4. **冲突预判**：主Agent提前预判可能的冲突，减少evidence fusion次数

---

## 十一、部署架构

### 11.1 微服务部署

```
┌─────────────────┐
│   API Gateway   │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
┌───▼───┐ ┌──▼────┐
│ Java  │ │Python │
│Services│ │Services│
└───┬───┘ └──┬────┘
    │         │
    └────┬────┘
         │
┌────────▼────────┐
│   Data Layer    │
│ MySQL/Oracle/   │
│ Neo4j/Redis     │
└─────────────────┘
```

### 11.2 容器化部署

- **Docker**：容器化所有服务
- **Kubernetes**：容器编排和自动扩缩容
- **服务网格**：Istio（可选，用于服务治理）

---

## 十二、健康状态判定服务技术实现

### 12.1 健康状态判定服务（Health State Assessment Service）

**职责**：判断"这个人，现在需要被当成'病人'对待吗？"

**技术实现**：

```python
class HealthStateAssessmentService:
    def assess_health_state(self, user_input: str, basic_info: Dict) -> Dict:
        """
        评估健康状态，决定工作态
        """
        # 1. 提取症状和基本信息
        symptoms = self.extract_symptoms(user_input)
        
        # 2. 症状严重程度评估
        severity = self.assess_symptom_severity(symptoms)
        
        # 3. 风险早筛
        risk_signals = self.early_risk_screening(symptoms, basic_info)
        
        # 4. 红旗信号识别
        red_flags = self.detect_red_flags(symptoms)
        
        # 5. 分诊决策
        if red_flags or severity >= "high" or risk_signals:
            work_mode = "clinical_mode"
            needs_clinical_mode = True
        elif severity == "normal" and not risk_signals:
            work_mode = "wellness_mode"
            needs_clinical_mode = False
        else:
            # 不确定时，优先进入临床诊疗态（宁可误报，不能漏报）
            work_mode = "clinical_mode"
            needs_clinical_mode = True
        
        # 构建健康状态判定结果
        health_state_assessment = {
            "needs_clinical_mode": needs_clinical_mode,
            "work_mode": work_mode,
            "risk_level": self.calculate_risk_level(severity, risk_signals, red_flags),
            "assessment_reason": self.generate_reason(severity, risk_signals, red_flags),
            "symptom_severity": severity,
            "early_risk_signals": risk_signals
        }
        
        return health_state_assessment
```

**CDP存储说明**：
- 健康状态判定结果需要存储到CDP的`health_state_assessment`字段
- 由诊断服务（diagnosis-service）调用健康状态判定服务后，将结果更新到CDP
- 更新时机：CDP创建时或工作态切换时

#### 12.1.1 入口判定模块（Entry Assessment Module）

**职责**：实现AI诊断入口判定的完整流程（Step 1-5）

**技术实现**：

```python
class EntryAssessmentModule:
    def __init__(self):
        self.nlu_engine = NLUEngine()  # 自然语言理解引擎
        self.symptom_recognizer = SymptomRecognizer()  # 症状识别器
        self.clarification_engine = ClarificationEngine()  # 方向澄清引擎
        self.red_flag_detector = RedFlagDetector()  # 危险信号检测器
        
    def entry_assessment(self, user_input: str, basic_info: Dict) -> Dict:
        """
        入口判定流程：Step 1-5
        """
        # Step 1: 接收用户输入
        user_intent = self.step1_receive_input(user_input)
        
        # Step 2: 识别用户是否"有症状/困扰"
        symptom_status = self.step2_identify_symptom(user_intent)
        
        # Step 3: 方向澄清（仅对"情况C"触发一次）
        clarification_result = None
        if symptom_status["status"] == "uncertain":
            clarification_result = self.step3_clarification(user_intent)
        
        # Step 4: 危险信号检查（所有用户都要过一次）
        red_flags_check = self.step4_red_flag_check(user_intent, basic_info)
        
        # Step 5: 输出路径结果并跳转
        path_result = self.step5_path_selection(
            symptom_status, 
            clarification_result, 
            red_flags_check
        )
        
        # 构建入口判定结果（entry_assessment）
        entry_assessment = {
            "user_input": user_input,
            "has_symptom": symptom_status["status"] == "has_symptom",
            "symptom_status": symptom_status["status"],
            "clarification_needed": symptom_status["status"] == "uncertain",
            "clarification_result": clarification_result["direction"] if clarification_result else None,
            "red_flags_hit": red_flags_check["red_flags_hit"],
            "red_flags_list": red_flags_check["red_flags"],
            "path_selected": path_result["path"]
        }
        
        return {
            "entry_assessment": entry_assessment,
            "path_result": path_result
        }
```

**数据库设计**：

```sql
-- 危险信号库表
CREATE TABLE red_flag_library (
    id VARCHAR(64) PRIMARY KEY,
    red_flag_name VARCHAR(255),
    red_flag_keywords JSON,
    red_flag_pattern TEXT,
    severity VARCHAR(32),  -- L1/L2/L3/L4
    safety_message TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_severity (severity)
);

-- 方向澄清模板表
CREATE TABLE clarification_template (
    id VARCHAR(64) PRIMARY KEY,
    scenario VARCHAR(255),  -- 不确定/混合诉求场景
    question_template TEXT,
    answer_options JSON,
    direction_mapping JSON,  -- 答案到方向的映射
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 症状识别规则表
CREATE TABLE symptom_recognition_rule (
    id VARCHAR(64) PRIMARY KEY,
    symptom_keyword VARCHAR(255),
    symptom_category VARCHAR(64),  -- 症状/不适/困扰/异常感觉/功能变化
    severity_threshold VARCHAR(32),  -- 轻微也算"有困扰"
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_keyword (symptom_keyword)
);
```

### 12.2 健康管理引擎（Wellness Management Engine）

**职责**：生成健康管理计划（面向健康管理态）

**技术实现**：

```python
class WellnessManagementEngine:
    def generate_wellness_plan(self, cdp: CDP) -> Dict:
        """
        生成健康管理计划
        """
        # 1. 风险识别与管理
        risk_management = self.identify_risks(cdp.patient_state)
        
        # 2. 生活方式建议生成
        lifestyle_advice = self.generate_lifestyle_advice(cdp.patient_state)
        
        # 3. 随访计划生成
        follow_up_plan = self.generate_follow_up_plan(cdp)
        
        # 4. 安抚与解释生成（使用LLM）
        reassurance = self.llm.generate_reassurance(cdp.patient_state)
        
        wellness_plan = {
            "risk_management": risk_management,
            "lifestyle_advice": lifestyle_advice,
            "follow_up_plan": follow_up_plan,
            "reassurance": reassurance,
            "upgrade_conditions": self.define_upgrade_conditions()
        }
        
        return wellness_plan
```

**CDP存储说明**：
- 健康管理计划需要存储到CDP的`wellness_plan`字段
- 由诊断服务（diagnosis-service）调用健康管理引擎后，将结果更新到CDP
- 更新时机：健康管理态流程执行时（A路径A1-A5完成后）

#### 12.2.1 健康筛查服务（Wellness Screening Service）

**职责**：实现健康人筛查流程（A路径）的完整功能

**技术实现**：

```python
class WellnessScreeningService:
    def __init__(self):
        self.demand_classifier = DemandClassifier()  # 需求分类器
        self.health_profile_collector = HealthProfileCollector()  # 健康档案采集器
        self.screening_branch_executor = ScreeningBranchExecutor()  # 分支执行器
        self.result_generator = WellnessResultGenerator()  # 结果生成器
        self.follow_up_manager = FollowUpManager()  # 随访管理器
        
    def wellness_screening_workflow(self, cdp: CDP) -> Dict:
        """
        健康人筛查流程（A路径）：A1-A5
        """
        # A1: 需求分类
        demand_type = self.a1_demand_classification(cdp)
        
        # A2: 通用最小健康档案
        health_profile = self.a2_collect_health_profile(cdp)
        
        # A3: 进入对应分支执行
        branch_result = self.a3_execute_branch(demand_type, health_profile, cdp)
        
        # A4: 统一结果页输出
        unified_result = self.a4_generate_unified_result(
            demand_type, branch_result, cdp
        )
        
        # A5: 随访闭环
        follow_up_schedule = self.a5_setup_follow_up(
            demand_type, unified_result, cdp
        )
        
        # 构建健康筛查路径结果（wellness_screening_path）
        wellness_screening_path = {
            "demand_type": demand_type["type"],
            "demand_type_name": demand_type["type_name"],
            "health_profile": health_profile,
            "branch_result": branch_result,
            "unified_result": unified_result,
            "follow_up_schedule": follow_up_schedule
        }
        
        # 构建完整的健康管理计划（wellness_plan）
        wellness_plan = {
            "risk_management": self._generate_risk_management(cdp),
            "lifestyle_advice": self._generate_lifestyle_advice(cdp),
            "follow_up_plan": self._generate_follow_up_plan(cdp),
            "reassurance": self._generate_reassurance(cdp),
            "upgrade_conditions": self._define_upgrade_conditions(),
            "wellness_screening_path": wellness_screening_path
        }
        
        return wellness_plan
```

**数据库设计**：

```sql
-- 需求分类规则表
CREATE TABLE demand_classification_rule (
    id VARCHAR(64) PRIMARY KEY,
    demand_type INT,  -- 1/2/3/4
    demand_type_name VARCHAR(64),
    keywords JSON,
    classification_rule TEXT,
    guide_template TEXT,  -- 不确定时的引导话术
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_demand_type (demand_type)
);

-- 筛查建议规则库表
CREATE TABLE screening_recommendation_rule (
    id VARCHAR(64) PRIMARY KEY,
    condition_json JSON,  -- 触发条件（年龄、性别、既往史等）
    screening_name VARCHAR(255),
    recommended BOOLEAN,
    frequency VARCHAR(64),  -- 筛查频率
    timing VARCHAR(255),  -- 筛查时机
    reason TEXT,  -- 推荐/不推荐理由
    alternative_suggestion TEXT,  -- 替代建议
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 体检异常分级规则表
CREATE TABLE abnormality_grading_rule (
    id VARCHAR(64) PRIMARY KEY,
    indicator_name VARCHAR(255),
    indicator_category VARCHAR(64),
    grade VARCHAR(32),  -- 轻度/中度/重度
    grading_criteria JSON,
    next_action VARCHAR(64),  -- 复查/进一步评估/退出A路径就医
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_indicator (indicator_name)
);

-- 健康目标计划模板表
CREATE TABLE health_goal_template (
    id VARCHAR(64) PRIMARY KEY,
    goal_type VARCHAR(64),  -- 减脂/睡眠/运动/精力
    goal_template JSON,
    action_steps_template JSON,
    tracking_indicators JSON,
    review_points JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_goal_type (goal_type)
);

-- 计划性健康需求场景库表
CREATE TABLE scenario_library (
    id VARCHAR(64) PRIMARY KEY,
    scenario_name VARCHAR(64),  -- 备孕/疫苗/差旅/材料等
    checklist JSON,
    schedule_template JSON,
    notes_template TEXT,
    reminder_rules JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_scenario (scenario_name)
);

-- 随访规则表
CREATE TABLE follow_up_rule (
    id VARCHAR(64) PRIMARY KEY,
    demand_type INT,
    follow_up_type VARCHAR(64),
    default_time VARCHAR(64),
    review_trigger TEXT,
    reminder_template TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_demand_type (demand_type)
);
```

### 12.3 工作态切换机制

**健康管理态 → 临床诊疗态升级**：

```python
class WorkModeUpgrader:
    def check_upgrade_conditions(self, cdp: CDP) -> bool:
        """
        检查是否需要从健康管理态升级到临床诊疗态
        """
        # 检查升级条件
        if self.symptom_worsened(cdp):
            return True
        if self.new_symptoms_appeared(cdp):
            return True
        if self.risk_signals_detected(cdp):
            return True
        
        return False
    
    def upgrade_to_clinical_mode(self, cdp: CDP):
        """
        升级到临床诊疗态
        """
        cdp.health_state_assessment.work_mode = "clinical_mode"
        cdp.health_state_assessment.needs_clinical_mode = True
        # 启动诊断流程
        self.start_diagnosis_workflow(cdp)
```

---

## 相关文档

- [AI医生系统-技术架构设计-核心架构](./AI医生系统-技术架构设计-核心架构.md)
- [AI医生系统-技术架构设计-工具清单](./AI医生系统-技术架构设计-工具清单.md)
- [AI医生系统-技术架构设计-核心技术组件](./AI医生系统-技术架构设计-核心技术组件.md)
- [AI医生系统-技术架构设计-CDP数据与状态管理](./AI医生系统-技术架构设计-CDP数据与状态管理.md)
- [AI医生系统-技术架构设计-索引](./AI医生系统-技术架构设计-索引.md)

