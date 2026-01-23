# diagnosis-service - 服务实现方案

> **文档定位**：本文档定义diagnosis-service（诊断服务）的技术实现方案，包括技术选型、接口设计、数据流、实现步骤等。  
> **参考文档**：
> - 《AI医生系统-业务逻辑详细设计.md》- 业务逻辑（核心参考）
> - 《AI医生系统-技术架构设计.md》及相关子文档- 技术架构
> - 《AI医生系统-项目结构设计.md》- 项目结构
> - 《AI医生系统-数据模型设计.md》- 数据模型
> - 《AI医生系统-API接口规范.md》- API规范
> - 《AI医生系统-错误处理规范.md》- 错误处理规范
> - 《AI医生系统-技术架构设计-CDP数据与状态管理.md》- CDP数据管理

---

## 一、服务概述

> **参考文档**：《AI医生系统-业务逻辑详细设计.md》第1.11节、第1.10节

### 1.1 服务定位

- **对应脑区**：协调器/编排器（非独立脑区，负责协调所有脑区）
- **在架构中的位置**：双通道推理架构的协调中心，位于所有AI服务的上层，负责流程编排和CDP管理
- **服务职责**：
  1. **CDP管理**：创建、更新、版本控制、回放、回退
  2. **诊断流程编排**：编排5步AI循证诊断流程（临床诊疗态）
  3. **健康筛查流程编排**：编排健康筛查流程（A路径，A1-A5，健康管理态）
  4. **服务协调**：协调各个AI服务的调用顺序和依赖关系
  5. **状态管理**：管理CDP状态流转和诊断流程状态

### 1.2 输入输出

#### 1.2.1 输入数据格式和来源

**输入来源**：前端直接调用、health-state-assessment-service（健康状态判定服务）

**输入数据格式**（参考《AI医生系统-数据模型设计.md》）：

**场景1：启动诊断流程**
```json
{
  "userId": "user-1",
  "sessionId": "session-123",
  "userInput": "我最近胸痛，持续了3天",
  "basicInfo": {
    "age": 45,
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

**场景2：继续诊断流程（提供用户回答）**
```json
{
  "cdpId": "cdp-123456",
  "userAnswer": {
    "questionId": "q-001",
    "answer": "是的，有胸闷感",
    "answerType": "text"
  }
}
```

**场景3：回填检查结果**
```json
{
  "cdpId": "cdp-123456",
  "examinationResults": [
    {
      "examinationType": "ECG",
      "result": "正常",
      "reportUrl": "https://example.com/report.pdf"
    }
  ]
}
```

#### 1.2.2 输出数据格式和目标

**输出目标**：前端、其他AI服务（通过CDP）

**输出数据格式**（参考《AI医生系统-最终输出格式规范.md》）：

**场景1：诊断流程启动响应**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp-123456",
    "workMode": "clinical_mode",
    "currentStep": "step1_identify_problem",
    "status": "in_progress",
    "nextAction": {
      "type": "question",
      "question": "请详细描述一下您的胸痛症状"
    }
  },
  "timestamp": 1705123456789
}
```

**场景2：诊断完成响应**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp-123456",
    "status": "completed",
    "conclusionPackage": {
      "conclusion": "考虑为稳定性心绞痛，建议进一步检查",
      "primaryHypothesis": [
        {
          "disease": "稳定性心绞痛",
          "cui": "C0002962",
          "confidence": 0.85,
          "evidence": ["胸痛", "胸闷", "持续3天"]
        }
      ],
      "mustExclude": [],
      "keyEvidence": [...],
      "actionPlan": [...],
      "followUp": {...}
    }
  },
  "timestamp": 1705123456789
}
```

### 1.3 业务价值

- **统一流程编排**：将复杂的多服务协作流程统一管理，降低前端调用复杂度
- **CDP生命周期管理**：确保诊断过程可追溯、可审计、可回退
- **状态一致性**：保证CDP状态与诊断流程状态的一致性
- **服务解耦**：前端只需调用diagnosis-service，无需了解底层AI服务的细节

---

## 二、技术实现设计

> **参考文档**：
> - 《AI医生系统-技术架构设计.md》及相关子文档
> - 《AI医生系统-技术架构设计-核心技术组件.md》
> - 《AI医生系统-技术架构设计-性能与评估.md》

### 2.1 技术栈选择

- **编程语言**：Java 8+
- **框架**：Spring Boot 2.7+
- **核心依赖库**：
  - `spring-boot-starter-web`：Web框架
  - `spring-boot-starter-data-jpa`：JPA数据访问
  - `spring-cloud-starter-openfeign`：服务间调用（Feign）
  - `spring-boot-starter-data-redis`：Redis缓存
  - `mysql-connector-java` / `ojdbc8`：数据库驱动（MySQL/Oracle）
  - `lombok`：代码简化
  - `swagger-springfox`：API文档
  - `flyway-core`：数据库迁移

**技术选型理由**：
- **Java + Spring Boot**：项目其他Java服务（如examination-service）使用相同技术栈，便于统一管理和维护
- **Feign**：简化服务间HTTP调用，支持负载均衡和熔断
- **JPA**：简化数据库操作，支持CDP复杂JSON字段的持久化
- **Redis**：缓存CDP数据，减少数据库查询，提升性能
- **Flyway**：数据库版本管理，确保环境一致性

### 2.2 核心算法/方法

#### 2.2.1 5步AI循证诊断流程编排算法

**算法描述**：按照5步AI循证诊断流程，协调各个脑区完成诊断任务

**实现思路**：
1. **Step 1：识别问题**
   - 调用 `clinical-parsing-service`（脑区A）进行概念归一化
   - 调用 `dialog-service`（脑区B）识别信息缺口
   - 构建结构化问题清单
   - 更新CDP的`patient_state`字段

2. **Step 2：构建鉴别诊断候选集并分层**
   - 调用 `diagnosis-engine-service`（脑区C）生成鉴别诊断候选集
   - 调用 `risk-assessment-service`（脑区F）进行风险评估
   - 执行三层分层（首要假设/主要备选/必须排除）
   - 更新CDP的`ddx`字段

3. **Step 3：组织候选集并建立分流路径**
   - 调用 `diagnosis-engine-service`（脑区C）组织推理子组
   - 调用 `dialog-service`（脑区B）设计分流路径
   - 生成结构化分流问题清单
   - 更新CDP的`ddx`字段

4. **Step 4：采集关键证据并形成排序与验证计划**
   - 调用 `dialog-service`（脑区B）采集关键证据
   - 调用 `diagnosis-engine-service`（脑区C）分析证据
   - 调用 `workup-planner-service`（脑区D）构建验证计划
   - 更新CDP的`evidence_graph`和`workup_plan`字段

5. **Step 5：回填证据并输出终点结论包**
   - 调用 `clinical-parsing-service`（脑区A）回填证据
   - 调用 `diagnosis-engine-service`（脑区C）更新三层排序
   - 调用 `treatment-engine-service`（脑区E）生成治疗方案
   - 调用 `risk-assessment-service`（脑区F）进行最终风险评估
   - 调用 `explanation-service`（脑区G）生成终点结论包
   - 更新CDP的`management_plan`、`triage`、`evidence_graph`字段

**算法复杂度**：O(n)，n为诊断步骤数（固定为5步）

#### 2.2.2 CDP版本控制算法

**算法描述**：使用乐观锁机制实现CDP版本控制，支持并发更新和版本回退

**实现思路**：
1. **版本号管理**：每次更新CDP时，版本号自增
2. **乐观锁**：更新时检查版本号，如果版本号不匹配则更新失败
3. **写时复制**：每次更新创建新版本记录，保留历史版本
4. **版本回退**：支持回退到任意历史版本

**算法复杂度**：O(1)（单次更新），O(n)（版本回退，n为版本数）

#### 2.2.3 健康筛查流程编排算法（A路径）

**算法描述**：编排健康筛查流程（A1-A5），协调health-state-assessment-service完成健康管理态流程

**实现思路**：
1. **A1：需求分类**：调用health-state-assessment-service的A1模块
2. **A2：收集健康画像**：调用health-state-assessment-service的A2模块
3. **A3：执行分支**：根据需求类型调用对应的分支执行器
4. **A4：生成统一结果**：调用health-state-assessment-service的A4模块
5. **A5：设置随访**：调用health-state-assessment-service的A5模块

**算法复杂度**：O(1)（固定5步）

### 2.3 性能要求

- **响应时间要求**（参考《AI医生系统-技术架构设计-性能与评估.md》）：
  - CDP创建：< 100ms
  - CDP更新：< 200ms
  - CDP查询：< 50ms（缓存命中）/< 200ms（数据库查询）
  - 流程编排单步调用：< 5s（包含下游服务调用）
  - 完整诊断流程：< 30s（5步流程，包含用户交互等待）

- **并发处理能力**：
  - 支持100+并发诊断会话
  - CDP读写支持1000+ QPS

- **资源消耗限制**：
  - 内存：< 2GB（单实例）
  - CPU：< 50%（正常负载）

### 2.4 关键技术点

#### 2.4.1 CDP并发更新控制

**技术难点**：多个服务可能同时更新CDP，需要保证数据一致性

**解决方案**：
- 使用乐观锁机制（version字段）
- 更新失败时自动重试（最多3次）
- 使用Redis分布式锁（可选，用于关键更新）

**技术风险**：高并发场景下可能出现更新冲突

**应对措施**：
- 实现重试机制
- 监控更新冲突率
- 必要时使用分布式锁

#### 2.4.2 服务间调用超时和熔断

**技术难点**：下游AI服务可能响应慢或不可用，需要保证系统稳定性

**解决方案**：
- 使用Feign配置超时时间（连接超时5s，读取超时30s）
- 使用Hystrix/Sentinel实现熔断机制
- 实现降级策略（服务不可用时返回友好提示）

**技术风险**：下游服务故障可能导致诊断流程中断

**应对措施**：
- 实现服务降级
- 记录详细日志，便于问题排查
- 监控服务健康状态

#### 2.4.3 CDP状态流转管理

**技术难点**：CDP状态流转复杂，需要保证状态转换的正确性

**解决方案**：
- 使用状态机模式管理CDP状态
- 定义明确的状态转换规则
- 状态转换时进行校验

**技术风险**：状态转换错误可能导致诊断流程异常

**应对措施**：
- 完善状态转换校验逻辑
- 记录状态转换日志
- 支持状态回退

---

## 三、接口设计

> **参考文档**：
> - 《AI医生系统-API接口规范.md》
> - 《AI医生系统-错误处理规范.md》
> - 《AI医生系统-数据模型设计.md》

### 3.1 API端点定义

#### 3.1.1 诊断流程接口

**启动诊断流程**
```
POST /api/v1/diagnosis/start
```

**继续诊断流程（提供用户回答）**
```
POST /api/v1/diagnosis/continue
```

**回填检查结果**
```
POST /api/v1/diagnosis/backfill-examination
```

**获取诊断状态**
```
GET /api/v1/diagnosis/{cdpId}/status
```

**获取诊断结果**
```
GET /api/v1/diagnosis/{cdpId}/result
```

#### 3.1.2 CDP管理接口

**创建CDP**
```
POST /api/v1/cdp
```

**获取CDP**
```
GET /api/v1/cdp/{cdpId}
```

**更新CDP**
```
PUT /api/v1/cdp/{cdpId}
```

**获取CDP版本历史**
```
GET /api/v1/cdp/{cdpId}/versions
```

**回放CDP演变过程**
```
GET /api/v1/cdp/{cdpId}/replay
```

**回退CDP到指定版本**
```
POST /api/v1/cdp/{cdpId}/rollback
```

### 3.2 请求/响应模型

#### 3.2.1 启动诊断流程

**请求体**：
```json
{
  "userId": "user-1",
  "sessionId": "session-123",
  "userInput": "我最近胸痛，持续了3天",
  "basicInfo": {
    "age": 45,
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

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp-123456",
    "workMode": "clinical_mode",
    "currentStep": "step1_identify_problem",
    "status": "in_progress",
    "nextAction": {
      "type": "question",
      "question": "请详细描述一下您的胸痛症状，是刺痛还是闷痛？",
      "questionId": "q-001"
    }
  },
  "timestamp": 1705123456789
}
```

#### 3.2.2 继续诊断流程

**请求体**：
```json
{
  "cdpId": "cdp-123456",
  "userAnswer": {
    "questionId": "q-001",
    "answer": "是闷痛，感觉胸口有压迫感",
    "answerType": "text"
  }
}
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp-123456",
    "currentStep": "step2_build_ddx_candidates",
    "status": "in_progress",
    "nextAction": {
      "type": "wait",
      "message": "正在分析您的症状，请稍候..."
    }
  },
  "timestamp": 1705123456789
}
```

#### 3.2.3 获取CDP

**请求参数**：
- `cdpId`：CDP ID（路径参数）
- `version`：版本号（查询参数，可选，不传则返回最新版本）

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp-123456",
    "patientId": "user-1",
    "sessionId": "session-123",
    "version": 5,
    "cdpStatus": "clinical_mode_diagnosing",
    "healthStateAssessment": {...},
    "patientState": {...},
    "ddx": [...],
    "evidenceGraph": [...],
    "workupPlan": [...],
    "managementPlan": [...],
    "triage": {...},
    "createdAt": "2025-01-22T10:00:00",
    "updatedAt": "2025-01-22T10:30:00"
  },
  "timestamp": 1705123456789
}
```

### 3.3 错误码定义

**错误码范围**：2000-2099（参考《AI医生系统-错误处理规范.md》）

| 错误码 | 说明 | HTTP状态码 | 是否可重试 |
|--------|------|------------|-----------|
| 2000 | 诊断流程启动失败 | 500 | false |
| 2001 | CDP创建失败 | 500 | true |
| 2002 | CDP更新失败（版本冲突） | 409 | true |
| 2003 | CDP不存在 | 404 | false |
| 2004 | CDP状态不允许此操作 | 400 | false |
| 2005 | 诊断流程已完成 | 400 | false |
| 2006 | 诊断流程已中断 | 400 | false |
| 2007 | 下游服务调用超时 | 504 | true |
| 2008 | 下游服务不可用 | 503 | true |
| 2009 | 流程编排失败 | 500 | false |
| 2010 | CDP版本回退失败 | 500 | false |

### 3.4 API文档

- **Swagger UI**：`http://localhost:8084/swagger-ui.html`
- **OpenAPI规范**：`http://localhost:8084/v3/api-docs`

---

## 四、数据流设计

> **参考文档**：
> - 《AI医生系统-数据模型设计.md》
> - 《AI医生系统-技术架构设计-CDP数据与状态管理.md》
> - 《AI医生系统-最终输出格式规范.md》

### 4.1 数据输入来源

#### 4.1.1 上游服务

- **health-state-assessment-service**：
  - 健康状态判定结果
  - 入口判定流程结果（Step 1-5）
  - 健康管理计划（健康管理态）

#### 4.1.2 前端输入

- 用户自然语言输入
- 用户回答（追问回答）
- 检查结果（回填）

#### 4.1.3 数据格式

- **CDP数据结构**：参考《AI医生系统-数据模型设计.md》第1.1节
- **请求/响应DTO**：参考《AI医生系统-数据模型设计.md》第2节

### 4.2 数据处理流程

#### 4.2.1 诊断流程启动

```
1. 接收前端请求（用户输入）
   ↓
2. 调用health-state-assessment-service进行健康状态判定
   ↓
3. 创建CDP（初始版本）
   ↓
4. 根据workMode决定流程：
   - wellness_mode → 执行健康筛查流程（A路径）
   - clinical_mode → 执行5步AI循证诊断流程
   ↓
5. 返回CDP ID和当前状态
```

#### 4.2.2 5步AI循证诊断流程数据流转

```
Step 1: 识别问题
  输入：CDP（包含用户输入）
  处理：调用clinical-parsing-service、dialog-service
  输出：更新CDP.patient_state（结构化问题清单）
   ↓
Step 2: 构建鉴别诊断候选集并分层
  输入：CDP.patient_state
  处理：调用diagnosis-engine-service、risk-assessment-service
  输出：更新CDP.ddx（三层分层结果）
   ↓
Step 3: 组织候选集并建立分流路径
  输入：CDP.ddx
  处理：调用diagnosis-engine-service、dialog-service
  输出：更新CDP.ddx（分流路径）
   ↓
Step 4: 采集关键证据并形成排序与验证计划
  输入：CDP.ddx、CDP.patient_state
  处理：调用dialog-service、diagnosis-engine-service、workup-planner-service
  输出：更新CDP.evidence_graph、CDP.workup_plan
   ↓
Step 5: 回填证据并输出终点结论包
  输入：CDP（所有字段）
  处理：调用所有相关服务
  输出：更新CDP.management_plan、CDP.triage、CDP.evidence_graph
        生成终点结论包
```

#### 4.2.3 CDP更新流程

```
1. 接收更新请求（来自下游服务或前端）
   ↓
2. 从数据库/缓存获取CDP（带版本号）
   ↓
3. 校验CDP状态是否允许更新
   ↓
4. 执行更新操作（乐观锁检查版本号）
   ↓
5. 如果版本冲突，重试（最多3次）
   ↓
6. 创建新版本记录（写时复制）
   ↓
7. 更新缓存
   ↓
8. 返回更新后的CDP
```

### 4.3 数据输出格式

#### 4.3.1 CDP输出格式

CDP输出格式遵循《AI医生系统-数据模型设计.md》中定义的CDP实体结构，包括：
- `health_state_assessment`：健康状态判定结果
- `patient_state`：患者状态（结构化问题清单）
- `ddx`：鉴别诊断列表（三层分层）
- `evidence_graph`：证据图
- `workup_plan`：检查计划
- `management_plan`：治疗计划
- `triage`：风险评估
- `wellness_plan`：健康管理计划（健康管理态）

#### 4.3.2 终点结论包输出格式

终点结论包输出格式遵循《AI医生系统-最终输出格式规范.md》，包括：
- `conclusion`：诊断结论
- `primaryHypothesis`：首要假设列表
- `mainAlternatives`：主要备选列表
- `mustExclude`：必须排除列表
- `keyEvidence`：关键证据链
- `actionPlan`：行动方案
- `followUp`：随访计划

### 4.4 CDP数据流转

#### 4.4.1 CDP字段更新规则

- **health_state_assessment**：由health-state-assessment-service更新（CDP创建时）
- **patient_state**：由clinical-parsing-service更新（Step 1）
- **ddx**：由diagnosis-engine-service更新（Step 2、Step 3、Step 5）
- **evidence_graph**：由diagnosis-engine-service、explanation-service更新（Step 4、Step 5）
- **workup_plan**：由workup-planner-service更新（Step 4）
- **management_plan**：由treatment-engine-service更新（Step 5）
- **triage**：由risk-assessment-service更新（Step 2、Step 5）
- **wellness_plan**：由health-state-assessment-service更新（健康管理态）

#### 4.4.2 CDP版本控制

- **版本号**：每次更新自增
- **版本记录**：每次更新创建新版本记录（cdp_version表）
- **版本查询**：支持查询任意历史版本
- **版本回退**：支持回退到指定版本

#### 4.4.3 CDP状态流转

CDP状态流转规则（参考《AI医生系统-技术架构设计-CDP数据与状态管理.md》第7.3节）：

```
initial
  ↓
wellness_mode / clinical_mode_collecting
  ↓
clinical_mode_diagnosing
  ↓
clinical_mode_managing
  ↓
completed
  ↓
follow_up
```

---

## 五、依赖关系

### 5.1 依赖的其他服务

#### 5.1.1 上游服务

- **health-state-assessment-service**（健康状态判定服务）
  - **依赖关系**：必须依赖
  - **调用方式**：同步HTTP调用（Feign）
  - **调用场景**：
    - 诊断流程启动时：健康状态判定
    - 健康管理态：健康筛查流程编排（A1-A5）

#### 5.1.2 下游服务（5步AI循证诊断流程）

- **clinical-parsing-service**（病例理解服务 - 脑区A）
  - **依赖关系**：必须依赖
  - **调用方式**：同步HTTP调用（Feign）
  - **调用场景**：Step 1（识别问题）、Step 5（回填证据）

- **dialog-service**（对话管理服务 - 脑区B）
  - **依赖关系**：必须依赖
  - **调用方式**：同步HTTP调用（Feign）
  - **调用场景**：Step 1（信息缺口识别）、Step 3（分流路径设计）、Step 4（关键证据采集）

- **diagnosis-engine-service**（诊断引擎服务 - 脑区C）
  - **依赖关系**：必须依赖
  - **调用方式**：同步HTTP调用（Feign）
  - **调用场景**：Step 2（构建鉴别诊断候选集）、Step 3（组织候选集）、Step 4（证据分析）、Step 5（更新三层排序）

- **workup-planner-service**（检查建议服务 - 脑区D）
  - **依赖关系**：必须依赖
  - **调用方式**：同步HTTP调用（Feign）
  - **调用场景**：Step 4（构建验证计划）

- **treatment-engine-service**（治疗推理服务 - 脑区E）
  - **依赖关系**：必须依赖
  - **调用方式**：同步HTTP调用（Feign）
  - **调用场景**：Step 5（生成治疗方案）

- **risk-assessment-service**（风险评估服务 - 脑区F）
  - **依赖关系**：必须依赖
  - **调用方式**：同步HTTP调用（Feign）
  - **调用场景**：Step 2（风险评估）、Step 5（最终风险评估）

- **explanation-service**（解释生成服务 - 脑区G）
  - **依赖关系**：必须依赖
  - **调用方式**：同步HTTP调用（Feign）
  - **调用场景**：Step 5（生成终点结论包）

### 5.2 依赖的外部资源

#### 5.2.1 数据库

- **MySQL/Oracle**：
  - **用途**：CDP持久化、CDP版本历史、诊断记录
  - **表结构**：参考《AI医生系统-数据模型设计.md》

#### 5.2.2 缓存

- **Redis**：
  - **用途**：CDP缓存、会话管理
  - **缓存策略**：
    - CDP缓存：TTL 1小时
    - 热点CDP：永久缓存（手动刷新）

### 5.3 依赖管理

#### 5.3.1 依赖版本管理

- **Spring Boot**：2.7.18
- **Spring Cloud**：2021.0.8
- **Java**：8+
- **MySQL Connector**：8.0.33
- **Oracle JDBC**：21.7.0.0

#### 5.3.2 依赖更新策略

- **安全更新**：及时更新安全补丁
- **功能更新**：评估兼容性后更新
- **重大版本更新**：充分测试后更新

#### 5.3.3 依赖冲突处理

- 使用Maven依赖管理统一版本
- 使用`mvn dependency:tree`检查依赖冲突
- 优先使用Spring Boot管理的版本

---

## 六、实现步骤

> **参考文档**：
> - 《AI医生系统-项目结构设计.md》
> - 《AI医生系统-技术架构设计-项目实现与部署.md》

### 6.1 分阶段实现计划

#### Phase 1: 基础框架搭建（1周）

- [ ] 创建服务目录结构（参考项目结构设计）
- [ ] 配置Spring Boot基础框架
- [ ] 配置数据库连接（MySQL/Oracle）
- [ ] 配置Redis连接
- [ ] 实现基础工具类（JsonUtil、DateUtil等）
- [ ] 配置日志和错误处理（参考错误处理规范）
- [ ] 配置Swagger API文档
- [ ] 实现健康检查接口

#### Phase 2: CDP管理功能实现（1.5周）

- [ ] 实现CDP实体类（参考数据模型设计）
- [ ] 实现CDPRepository（JPA）
- [ ] 实现CDPManager（创建、更新、查询）
- [ ] 实现CDP版本控制（乐观锁、版本记录）
- [ ] 实现CDP缓存（Redis）
- [ ] 实现CDP版本历史查询
- [ ] 实现CDP回放功能
- [ ] 实现CDP回退功能
- [ ] 单元测试

#### Phase 3: 服务间调用框架（1周）

- [ ] 配置Feign客户端
- [ ] 实现HealthStateAssessmentClient
- [ ] 实现ClinicalParsingClient
- [ ] 实现DialogServiceClient
- [ ] 实现DiagnosisEngineClient
- [ ] 实现WorkupPlannerClient
- [ ] 实现TreatmentEngineClient
- [ ] 实现RiskAssessmentClient
- [ ] 实现ExplanationServiceClient
- [ ] 配置超时和熔断
- [ ] 单元测试

#### Phase 4: 健康筛查流程编排（1周）

- [ ] 实现健康筛查流程编排服务（WellnessScreeningOrchestrator）
- [ ] 实现A1需求分类编排
- [ ] 实现A2收集健康画像编排
- [ ] 实现A3执行分支编排
- [ ] 实现A4生成统一结果编排
- [ ] 实现A5设置随访编排
- [ ] 集成测试

#### Phase 5: 5步AI循证诊断流程编排（2周）

- [ ] 实现诊断流程编排服务（DiagnosisWorkflowOrchestrator）
- [ ] 实现Step 1：识别问题
- [ ] 实现Step 2：构建鉴别诊断候选集并分层
- [ ] 实现Step 3：组织候选集并建立分流路径
- [ ] 实现Step 4：采集关键证据并形成排序与验证计划
- [ ] 实现Step 5：回填证据并输出终点结论包
- [ ] 实现流程状态管理
- [ ] 集成测试

#### Phase 6: API接口实现（1周）

- [ ] 实现诊断流程启动接口
- [ ] 实现诊断流程继续接口
- [ ] 实现回填检查结果接口
- [ ] 实现获取诊断状态接口
- [ ] 实现获取诊断结果接口
- [ ] 实现CDP管理接口（创建、查询、更新、版本历史、回放、回退）
- [ ] 实现请求参数验证
- [ ] 实现统一响应格式
- [ ] 集成测试

#### Phase 7: 集成与优化（1周）

- [ ] 服务间集成测试
- [ ] 端到端测试
- [ ] 性能优化（CDP缓存、数据库查询优化）
- [ ] 并发测试
- [ ] 错误处理完善
- [ ] 日志完善
- [ ] 监控和告警配置

### 6.2 优先级排序

- **P0（必须）**：
  - CDP管理功能（Phase 2）
  - 服务间调用框架（Phase 3）
  - 5步AI循证诊断流程编排（Phase 5）
  - API接口实现（Phase 6）

- **P1（重要）**：
  - 健康筛查流程编排（Phase 4）
  - 集成与优化（Phase 7）

- **P2（可选）**：
  - 性能优化（缓存策略优化）
  - 监控和告警

### 6.3 里程碑定义

- **Milestone 1**：基础框架完成（Phase 1）
  - 服务可启动
  - 健康检查接口正常
  - 数据库连接正常

- **Milestone 2**：CDP管理功能完成（Phase 2）
  - CDP创建、更新、查询正常
  - CDP版本控制正常
  - CDP缓存正常

- **Milestone 3**：服务间调用框架完成（Phase 3）
  - 所有Feign客户端配置完成
  - 超时和熔断配置完成

- **Milestone 4**：流程编排完成（Phase 4 + Phase 5）
  - 健康筛查流程编排完成
  - 5步AI循证诊断流程编排完成

- **Milestone 5**：API接口完成（Phase 6）
  - 所有API接口实现完成
  - 集成测试通过

- **Milestone 6**：集成测试通过（Phase 7）
  - 端到端测试通过
  - 性能满足要求

---

## 七、测试策略

### 7.1 单元测试计划

#### 7.1.1 测试覆盖范围

- **CDP管理服务**：100%覆盖
  - CDP创建
  - CDP更新（含版本控制）
  - CDP查询
  - CDP版本历史
  - CDP回退

- **流程编排服务**：核心逻辑100%覆盖
  - 5步AI循证诊断流程编排
  - 健康筛查流程编排
  - 状态流转

- **服务客户端**：Mock测试
  - 所有Feign客户端Mock
  - 超时和熔断测试

#### 7.1.2 测试用例设计

**CDP管理测试用例**：
1. 创建CDP成功
2. 创建CDP失败（参数验证）
3. 更新CDP成功
4. 更新CDP失败（版本冲突）
5. 查询CDP成功
6. 查询CDP失败（不存在）
7. 查询CDP版本历史
8. CDP回退成功
9. CDP回退失败（版本不存在）

**流程编排测试用例**：
1. 启动诊断流程成功（临床诊疗态）
2. 启动诊断流程成功（健康管理态）
3. 继续诊断流程成功
4. 继续诊断流程失败（CDP不存在）
5. 继续诊断流程失败（流程已完成）
6. 5步流程完整执行
7. 流程中断处理
8. 服务调用超时处理
9. 服务调用失败处理

#### 7.1.3 Mock策略

- **数据库**：使用H2内存数据库（测试环境）
- **Redis**：使用EmbeddedRedis（测试环境）
- **下游服务**：使用Mockito Mock Feign客户端
- **外部依赖**：全部Mock

### 7.2 集成测试计划

#### 7.2.1 服务间集成测试

- **与health-state-assessment-service集成**：
  - 健康状态判定调用
  - 健康筛查流程调用（A1-A5）

- **与clinical-parsing-service集成**：
  - Step 1调用
  - Step 5调用

- **与其他AI服务集成**：
  - 5步流程完整集成测试

#### 7.2.2 数据流测试

- **CDP数据流转测试**：
  - CDP创建 → 更新 → 版本历史
  - CDP状态流转
  - CDP字段更新

- **诊断流程数据流转测试**：
  - Step 1 → Step 2 → Step 3 → Step 4 → Step 5
  - 每个步骤的CDP更新验证

#### 7.2.3 端到端测试

- **完整诊断流程测试**：
  1. 用户输入 → 健康状态判定 → 创建CDP
  2. 执行5步AI循证诊断流程
  3. 生成终点结论包
  4. 返回诊断结果

- **健康筛查流程测试**：
  1. 用户输入 → 健康状态判定 → 创建CDP
  2. 执行健康筛查流程（A1-A5）
  3. 生成健康管理计划
  4. 返回筛查结果

### 7.3 性能测试计划

#### 7.3.1 性能测试指标

- **CDP创建**：< 100ms（P95）
- **CDP更新**：< 200ms（P95）
- **CDP查询**：< 50ms（缓存命中，P95）/< 200ms（数据库查询，P95）
- **流程编排单步**：< 5s（P95）
- **完整诊断流程**：< 30s（P95）

#### 7.3.2 性能测试场景

- **并发CDP创建**：100并发
- **并发CDP更新**：100并发
- **并发诊断流程**：50并发
- **CDP缓存命中率**：> 80%

#### 7.3.3 性能优化目标

- **CDP查询性能**：缓存命中率 > 80%
- **数据库查询优化**：索引优化，查询时间 < 200ms
- **服务调用优化**：连接池优化，超时配置优化

### 7.4 测试数据准备

#### 7.4.1 测试数据设计

- **CDP测试数据**：准备10个不同状态的CDP
- **诊断流程测试数据**：准备5个完整诊断流程的测试用例
- **健康筛查测试数据**：准备5个健康筛查流程的测试用例

#### 7.4.2 测试数据管理

- 使用测试数据库（独立于开发/生产环境）
- 每个测试用例执行前清理数据
- 使用Fixture或TestDataBuilder准备测试数据

#### 7.4.3 测试环境配置

- **数据库**：MySQL 8.0（测试环境）
- **Redis**：Redis 7.0（测试环境）
- **下游服务**：Mock服务或测试环境服务

---

## 八、风险评估

### 8.1 技术风险

#### 8.1.1 CDP并发更新冲突

**风险识别**：高并发场景下可能出现CDP更新冲突

**风险等级**：中

**应对措施**：
- 使用乐观锁机制
- 实现自动重试（最多3次）
- 监控更新冲突率
- 必要时使用分布式锁

#### 8.1.2 下游服务调用超时

**风险识别**：下游AI服务可能响应慢或不可用

**风险等级**：高

**应对措施**：
- 配置合理的超时时间
- 实现熔断机制
- 实现降级策略
- 监控服务健康状态

#### 8.1.3 CDP状态流转错误

**风险识别**：状态转换错误可能导致诊断流程异常

**风险等级**：中

**应对措施**：
- 使用状态机模式管理状态
- 完善状态转换校验逻辑
- 记录状态转换日志
- 支持状态回退

### 8.2 业务风险

#### 8.2.1 诊断流程中断

**风险识别**：服务故障可能导致诊断流程中断，用户体验差

**风险等级**：高

**应对措施**：
- 实现流程状态持久化
- 支持流程恢复
- 实现友好的错误提示
- 记录详细日志，便于问题排查

#### 8.2.2 CDP数据丢失

**风险识别**：数据库故障可能导致CDP数据丢失

**风险等级**：高

**应对措施**：
- 定期备份数据库
- 实现CDP版本历史（写时复制）
- 使用Redis缓存作为备份
- 监控数据库健康状态

### 8.3 时间风险

#### 8.3.1 下游服务开发延迟

**风险识别**：下游AI服务开发延迟可能影响集成测试

**风险等级**：中

**应对措施**：
- 提前与下游服务团队沟通
- 使用Mock服务进行开发
- 制定详细的集成测试计划

#### 8.3.2 性能优化时间不足

**风险识别**：性能优化可能需要额外时间

**风险等级**：低

**应对措施**：
- 提前进行性能测试
- 识别性能瓶颈
- 制定性能优化计划

---

## 九、后续优化方向

### 9.1 性能优化

#### 9.1.1 CDP缓存优化

- **优化方向**：提升CDP缓存命中率
- **优化计划**：
  - 实现CDP预加载（热点CDP）
  - 优化缓存策略（LRU + TTL）
  - 实现缓存预热

#### 9.1.2 数据库查询优化

- **优化方向**：优化CDP查询性能
- **优化计划**：
  - 添加数据库索引
  - 优化查询SQL
  - 实现读写分离（可选）

#### 9.1.3 服务调用优化

- **优化方向**：优化服务间调用性能
- **优化计划**：
  - 优化连接池配置
  - 实现请求合并（批量调用）
  - 实现异步调用（可选）

### 9.2 功能扩展

#### 9.2.1 异步流程编排

- **扩展方向**：支持异步诊断流程（长时间诊断）
- **扩展计划**：
  - 实现消息队列（RabbitMQ/Kafka）
  - 实现流程状态轮询接口
  - 实现流程结果通知

#### 9.2.2 流程模板化

- **扩展方向**：支持诊断流程模板化
- **扩展计划**：
  - 定义流程模板
  - 支持流程模板配置
  - 支持流程模板复用

#### 9.2.3 多租户支持

- **扩展方向**：支持多租户（不同医院/机构）
- **扩展计划**：
  - 实现租户隔离
  - 实现租户配置
  - 实现租户数据隔离

---

**文档版本**：v1.0  
**创建日期**：2025年1月  
**维护人员**：开发团队

