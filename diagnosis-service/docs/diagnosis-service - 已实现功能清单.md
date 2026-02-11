# diagnosis-service - 已实现功能清单

本文档记录了 `diagnosis-service`（诊断服务，流程编排服务）的已实现功能。

**文档版本**: v1.3  
**最后更新**: 2026-01-24  
**服务状态**: ✅ 核心框架完整实现，服务调用和CDP管理已通过测试验证，健康筛查流程已完整实现，诊断流程Step 1已实现并正常工作

---

## 📋 目录

- [核心功能实现](#核心功能实现)
- [API接口](#api接口)
- [简化实现说明](#简化实现说明)
- [未实现功能](#未实现功能)
- [测试覆盖](#测试覆盖)

---

## ✅ 核心功能实现

### 1. 基础框架搭建

**实现状态**: ✅ 完整实现

**功能描述**:
- Spring Boot服务基础框架
- 配置管理、日志系统、异常处理

**实现方式**:
- 使用Spring Boot 2.7+框架
- 代码位置: `src/main/java/com/aidoctor/diagnosis/DiagnosisServiceApplication.java`
- 统一异常处理: `src/main/java/com/aidoctor/diagnosis/exception/GlobalExceptionHandler.java`

**功能特性**:
- ✅ Spring Boot配置管理
- ✅ 结构化日志记录（Slf4j）
- ✅ 统一异常处理框架
- ✅ 健康检查接口
- ✅ API文档（Swagger）

---

### 2. CDP管理功能

**实现状态**: ✅ 完整实现

**功能描述**:
- CDP（Clinical Data Package）的创建、更新、查询、版本控制

**实现方式**:
- CDP实体类: `src/main/java/com/aidoctor/diagnosis/entity/CDP.java`
- CDP管理器: `src/main/java/com/aidoctor/diagnosis/service/cdp/CDPManager.java`
- CDP版本服务: `src/main/java/com/aidoctor/diagnosis/service/cdp/CDPVersionService.java`
- CDP仓库: `src/main/java/com/aidoctor/diagnosis/repository/CDPRepository.java`

**功能特性**:
- ✅ CDP创建（自动生成CDP ID）
- ✅ CDP更新（支持乐观锁版本控制）
- ✅ CDP查询（按ID、patientId、sessionId查询）
- ✅ CDP版本历史查询
- ✅ CDP版本回退
- ✅ CDP回放功能
- ✅ CDP重新排序功能

**验证状态**:
- ✅ **已通过数据库验证**: CDP的创建和更新已成功写入数据库
- ✅ **实际测试**: 启动诊断流程时自动创建CDP，健康状态判定后更新CDP，健康筛查流程各步骤更新CDP

**版本控制机制**:
- 使用乐观锁（version字段）避免并发更新冲突
- 每次更新创建新版本记录（写时复制）
- 支持查询任意历史版本
- 支持回退到指定版本

---

### 3. 服务间调用框架

**实现状态**: ✅ 完整实现

**功能描述**:
- 使用Feign客户端调用下游AI服务
- 配置超时和熔断机制

**实现方式**:
- Feign配置: `src/main/java/com/aidoctor/diagnosis/config/FeignConfig.java`
- Feign客户端: `src/main/java/com/aidoctor/diagnosis/client/` 目录

**已实现的Feign客户端**:
- ✅ **ClinicalParsingClient**（tool_1 - 病例理解服务）
- ✅ **DialogServiceClient**（tool_2 - 对话管理服务）
- ✅ **DiagnosisEngineClient**（tool_3 - 诊断引擎服务）
- ✅ **WorkupPlannerClient**（tool_4 - 检查建议服务）
- ✅ **TreatmentEngineClient**（tool_5 - 治疗推理服务）
- ✅ **RiskAssessmentClient**（tool_6 - 风险评估服务）
- ✅ **ExplanationServiceClient**（tool_7 - 解释生成服务）
- ✅ **HealthStateAssessmentClient**（tool_0 - 健康状态判定服务）
- ✅ **OcrServiceClient**（OCR识别服务）
- ✅ **ProfileServiceClient**（用户画像服务）
- ✅ **WellnessServiceClient**（健康筛查服务）

**验证状态**:
- ✅ **HealthStateAssessmentClient**: 已通过测试，成功调用健康状态判定服务，获取workMode和riskLevel
- ✅ **ClinicalParsingClient**: 已通过测试，成功调用病例理解服务，识别医学概念并返回结构化数据
- ⚠️ **DialogServiceClient**: 代码已实现，但服务可能未启动，调用失败时使用默认问题（降级策略）

**配置特性**:
- ✅ 超时配置（连接超时、读取超时）
- ✅ 熔断机制（Hystrix/Sentinel）
- ✅ 负载均衡支持

---

### 4. 诊断流程编排框架

**实现状态**: ✅ 框架完整实现，业务逻辑部分实现

**功能描述**:
- 编排5步AI循证诊断流程
- 协调各个AI服务完成诊断任务

**实现方式**:
- 诊断流程编排器: `src/main/java/com/aidoctor/diagnosis/service/orchestration/DiagnosisWorkflowOrchestrator.java`
- 诊断编排服务: `src/main/java/com/aidoctor/diagnosis/service/DiagnosisOrchestrationService.java`

**5步AI循证诊断流程**:

#### Step 1: 识别问题
- **实现状态**: ✅ 完整实现并已验证
- **功能**: 调用clinical-parsing-service和dialog-service
- **代码位置**: `DiagnosisWorkflowOrchestrator.step1IdentifyProblem()`
- **验证状态**: 
  - ✅ 成功调用clinical-parsing-service，识别医学概念（如"腹痛"）
  - ✅ 响应解析已实现（parseParsingResponse），能够正确提取结构化数据
  - ⚠️ dialog-service调用失败时使用默认问题（降级策略）

#### Step 2: 构建鉴别诊断候选集并分层
- **实现状态**: ✅ 框架完整，⚠️ 响应解析待实现
- **功能**: 调用diagnosis-engine-service和risk-assessment-service
- **代码位置**: `DiagnosisWorkflowOrchestrator.step2BuildDDxCandidates()`

#### Step 3: 组织候选集并建立分流路径
- **实现状态**: ✅ 框架完整，⚠️ 响应解析待实现
- **功能**: 调用diagnosis-engine-service和dialog-service
- **代码位置**: `DiagnosisWorkflowOrchestrator.step3OrganizeRoutingPath()`

#### Step 4: 采集关键证据并形成排序与验证计划
- **实现状态**: ✅ 框架完整，⚠️ 响应解析待实现
- **功能**: 调用dialog-service、diagnosis-engine-service、workup-planner-service
- **代码位置**: `DiagnosisWorkflowOrchestrator.step4CollectEvidenceAndPlan()`

#### Step 5: 回填证据并输出终点结论包
- **实现状态**: ✅ 框架完整，⚠️ 响应解析待实现
- **功能**: 调用所有相关服务生成终点结论包
- **代码位置**: `DiagnosisWorkflowOrchestrator.step5BackfillAndConclude()`

**流程控制逻辑**:
- ✅ 流程步骤顺序控制
- ✅ 服务调用逻辑
- ✅ 错误处理和状态管理
- ✅ CDP更新逻辑
- ⚠️ **响应解析方法待实现**（标记为TODO）

---

### 5. 健康筛查流程编排框架

**实现状态**: ✅ 完整实现

**功能描述**:
- 编排健康筛查流程（A路径，A1-A5）
- 协调health-state-assessment-service完成健康管理态流程

**实现方式**:
- 健康筛查编排器: `src/main/java/com/aidoctor/diagnosis/service/wellness/WellnessScreeningOrchestrator.java`

**健康筛查流程步骤**:

#### A1: 需求分类
- **实现状态**: ✅ 完整实现
- **功能**: 调用health-state-assessment-service的A1接口
- **响应解析**: ✅ 已实现 `parseA1Response()`
- **代码位置**: `WellnessScreeningOrchestrator.a1DemandClassification()`

#### A2: 收集健康画像
- **实现状态**: ✅ 完整实现
- **功能**: 调用health-state-assessment-service的A2接口
- **响应解析**: ✅ 已实现 `parseA2Response()`
- **代码位置**: `WellnessScreeningOrchestrator.a2HealthProfileCollection()`

#### A3: 执行分支
- **实现状态**: ✅ 完整实现
- **功能**: 调用health-state-assessment-service的A3接口
- **响应解析**: ✅ 已实现 `parseA3Response()`
- **代码位置**: `WellnessScreeningOrchestrator.a3BranchExecution()`

#### A4: 生成统一结果
- **实现状态**: ✅ 完整实现
- **功能**: 调用health-state-assessment-service的A4接口
- **响应解析**: ✅ 已实现 `parseA4Response()`
- **代码位置**: `WellnessScreeningOrchestrator.a4UnifiedResultGeneration()`

#### A5: 设置随访
- **实现状态**: ✅ 完整实现
- **功能**: 调用health-state-assessment-service的A5接口
- **响应解析**: ✅ 已实现 `parseA5Response()`
- **代码位置**: `WellnessScreeningOrchestrator.a5FollowUpSetup()`

**流程控制逻辑**:
- ✅ 流程步骤顺序控制
- ✅ 服务调用逻辑
- ✅ 错误处理和状态管理
- ✅ **响应解析方法已实现**

---

### 6. 诊断编排服务

**实现状态**: ✅ 完整实现

**功能描述**:
- 诊断流程的启动、继续、状态查询、结果获取

**实现方式**:
- 诊断编排服务: `src/main/java/com/aidoctor/diagnosis/service/DiagnosisOrchestrationService.java`

**功能特性**:
- ✅ **启动诊断流程**：
  - 生成会话ID
  - 创建CDP
  - 调用健康状态判定服务
  - 根据workMode选择流程（临床诊疗态/健康管理态）
- ✅ **继续诊断流程**：
  - 接收用户回答
  - 更新CDP
  - 继续执行诊断步骤
- ✅ **获取诊断状态**：
  - 查询CDP状态
  - 返回当前步骤和状态
- ✅ **获取诊断结果**：
  - 查询CDP
  - 返回诊断结果和结论包
- ⚠️ **响应解析部分待完善**

---

## 🔌 API接口

### 接口信息

#### 1. 启动诊断流程

**接口路径**: `POST /api/v1/diagnosis/start`

**请求格式**:
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

**响应格式**:
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
      "question": "请详细描述一下您的胸痛症状",
      "questionId": "q-001"
    }
  },
  "timestamp": 1705123456789
}
```

#### 2. 继续诊断流程

**接口路径**: `POST /api/v1/diagnosis/continue`

**请求格式**:
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

**响应格式**:
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

#### 3. 获取诊断状态

**接口路径**: `GET /api/v1/diagnosis/{cdpId}/status`

**响应格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp-123456",
    "currentStep": "step2_build_ddx_candidates",
    "status": "in_progress",
    "progress": 40
  },
  "timestamp": 1705123456789
}
```

#### 4. 获取诊断结果

**接口路径**: `GET /api/v1/diagnosis/{cdpId}/result`

**响应格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp-123456",
    "status": "completed",
    "conclusionPackage": {
      "conclusion": "考虑为稳定性心绞痛，建议进一步检查",
      "primaryHypothesis": [...],
      "mainAlternatives": [...],
      "mustExclude": [...],
      "keyEvidence": [...],
      "actionPlan": [...],
      "followUp": {...}
    }
  },
  "timestamp": 1705123456789
}
```

### 功能特性

- ✅ 统一响应格式（code, message, data, timestamp）
- ✅ 参数验证（DTO模型）
- ✅ 错误处理（统一异常处理、错误码）
- ✅ 日志记录（结构化日志）
- ✅ API文档（Swagger UI）

### 错误码

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 2000 | 诊断流程启动失败 | 500 |
| 2001 | CDP创建失败 | 500 |
| 2002 | CDP更新失败（版本冲突） | 409 |
| 2003 | CDP不存在 | 404 |
| 2004 | CDP状态不允许此操作 | 400 |
| 2005 | 诊断流程已完成 | 400 |
| 2006 | 诊断流程已中断 | 400 |
| 2007 | 下游服务调用超时 | 504 |
| 2008 | 下游服务不可用 | 503 |
| 2009 | 流程编排失败 | 500 |
| 2010 | CDP版本回退失败 | 500 |

---

## 🟡 简化实现说明

### 简化实现的功能

以下功能采用简化实现方式，框架完整但业务逻辑待完善：

#### 1. 响应解析方法（部分实现）

**当前实现**: 7个已实现（诊断流程1个 + 健康筛查流程5个 + 健康状态判定1个），9个待实现

**已实现的解析方法**:

**DiagnosisOrchestrationService中**:
- ✅ `parseAssessmentResponse()` - 已完整实现，能够解析health-state-assessment-service的响应格式
  - 支持提取workMode、needsClinicalMode、riskLevel等关键字段
  - 支持提取entryAssessment、redFlags、wellnessPlan等复杂字段
  - 代码位置: `DiagnosisOrchestrationService.java` 第370-420行

**DiagnosisWorkflowOrchestrator中**:
- ✅ `parseParsingResponse()` - 已完整实现，能够解析clinical-parsing-service的响应格式
  - 支持解析concepts、structuredData、ambiguousExpressions
  - 能够提取symptoms、signs、examinations等结构化数据
  - 代码位置: `DiagnosisWorkflowOrchestrator.java` 第528-601行

**WellnessScreeningOrchestrator中**:
- ✅ `parseA1Response()` - 已实现，解析A1需求分类响应
- ✅ `parseA2Response()` - 已实现，解析A2收集健康画像响应
- ✅ `parseA3Response()` - 已实现，解析A3执行分支响应
- ✅ `parseA4Response()` - 已实现，解析A4生成统一结果响应
- ✅ `parseA5Response()` - 已实现，解析A5设置随访响应
- 代码位置: `WellnessScreeningOrchestrator.java` 第190-270行

**待实现的解析方法**:

**DiagnosisWorkflowOrchestrator中**:
- ⚠️ `parseDDxResponse()` - 解析diagnosis-engine-service的DDx响应（TODO）
- ⚠️ `parseRiskResponse()` - 解析risk-assessment-service响应（TODO）
- ⚠️ `parseOrganizeResponse()` - 解析推理子组组织响应（TODO）
- ⚠️ `parseEvidenceResponse()` - 解析证据采集响应（TODO）
- ⚠️ `parseAnalysisResponse()` - 解析证据分析响应（TODO）
- ⚠️ `parseWorkupResponse()` - 解析检查计划响应（TODO）
- ⚠️ `parseBackfillResponse()` - 解析证据回填响应（TODO）
- ⚠️ `parseRankingResponse()` - 解析排序更新响应（TODO）
- ⚠️ `parseTreatmentResponse()` - 解析治疗方案响应（TODO）


**影响**:
- ✅ Step 1可以正确解析clinical-parsing-service的响应并更新CDP（已验证）
- ✅ 健康筛查流程（A1-A5）可以正确解析响应并更新CDP（已验证）
- ✅ 健康状态判定响应可以正确解析（已验证）
- ⚠️ Step 2-5可以调用下游服务，但无法正确解析响应数据（返回空列表/空Map）
- ⚠️ 无法将Step 2-5的响应数据更新到CDP
- ⚠️ 诊断流程Step 2-5无法完整执行

**实际验证**:
- ✅ 通过前端测试，Step 1已成功执行，能够识别用户输入并生成问题
- ✅ 通过数据库验证，CDP的创建和更新已成功写入数据库

**优化方向**:
- 根据实际下游服务API响应格式实现所有parse方法
- 确保能正确解析workMode、needsClinicalMode等关键字段
- 将解析后的数据正确更新到CDP

**代码位置**: 
- `src/main/java/com/aidoctor/diagnosis/service/orchestration/DiagnosisWorkflowOrchestrator.java`
- `src/main/java/com/aidoctor/diagnosis/service/wellness/WellnessScreeningOrchestrator.java`
- `src/main/java/com/aidoctor/diagnosis/service/DiagnosisOrchestrationService.java`

---

#### 2. 具体业务逻辑服务类（可选）

**当前实现**: 服务类存在但标记为TODO

**说明**:
- 这些服务类在step1-step5目录下，但当前流程编排器直接调用下游服务
- 这些类可能是为未来扩展准备的，当前可能不需要

**服务类列表**:
- ⚠️ `ConceptNormalizationService` - 概念归一化
- ⚠️ `StructuredQuestionListBuilder` - 结构化问题清单构建
- ⚠️ `DDxCandidateGenerator` - DDx候选集生成
- ⚠️ `ThreeLayerClassifier` - 三层分类器
- ⚠️ `ReasoningGroupOrganizer` - 推理子组组织
- ⚠️ `RoutingPathDesigner` - 分流路径设计
- ⚠️ `KeyEvidenceCollector` - 关键证据采集
- ⚠️ `EvidenceAnalyzer` - 证据分析
- ⚠️ `VerificationPlanBuilder` - 验证计划构建
- ⚠️ `EvidenceBackfillService` - 证据回填
- ⚠️ `DDxRankingUpdater` - DDx排序更新
- ⚠️ `ConclusionPackageBuilder` - 终点结论包构建

**影响**:
- 当前不影响核心功能（流程编排器直接调用下游服务）
- 如果未来需要本地处理逻辑，可以完善这些服务类

---

### 为什么采用简化实现

1. **架构优先**: 先搭建完整的服务架构和流程编排框架
2. **快速迭代**: 框架搭建完成后，可以逐步完善业务逻辑
3. **依赖下游服务**: 响应解析需要等待下游服务API稳定后才能实现
4. **分阶段实现**: 先实现核心框架，再完善业务逻辑

---

## ❌ 未实现功能

### 1. CDP管理接口（部分）

**状态**: 🟡 部分实现

**已实现接口**:
- ✅ CDP创建（通过诊断流程启动时创建）
- ✅ CDP查询（通过诊断状态/结果接口查询）

**未实现接口**:
- ❌ `POST /api/v1/cdp` - 独立创建CDP接口
- ❌ `GET /api/v1/cdp/{cdpId}` - 独立查询CDP接口
- ❌ `PUT /api/v1/cdp/{cdpId}` - 独立更新CDP接口
- ❌ `GET /api/v1/cdp/{cdpId}/versions` - 获取CDP版本历史接口
- ❌ `GET /api/v1/cdp/{cdpId}/replay` - 回放CDP演变过程接口
- ❌ `POST /api/v1/cdp/{cdpId}/rollback` - 回退CDP到指定版本接口

**说明**:
- CDP管理功能已实现（CDPManager、CDPVersionService等）
- 但独立的CDP管理API接口未实现
- 当前CDP管理通过诊断流程接口间接使用

**影响**:
- 无法直接通过API管理CDP
- 需要通过诊断流程接口间接使用CDP功能

---

### 2. 回填检查结果接口

**状态**: 🔴 未实现

**功能描述**:
- 回填检查报告等证据到CDP
- 接口路径: `POST /api/v1/diagnosis/backfill-examination`

**未实现原因**:
- 当前主要关注诊断流程编排
- 证据回填功能可以后续实现

**影响**:
- 无法通过此接口回填检查结果
- 需要通过其他方式回填证据

---

### 3. 单元测试和集成测试

**状态**: 🔴 未实现

**功能描述**:
- 单元测试（CDP管理、流程编排逻辑）
- 集成测试（完整流程，Mock下游服务）

**未实现原因**:
- 当前处于开发阶段，测试待编写

**影响**:
- 无法验证功能正确性
- 无法保证代码质量

---

## 🧪 测试覆盖

### 已覆盖的测试场景

根据实现状态说明，以下场景已可测试：

#### 1. CDP创建
- ✅ 启动诊断流程 → 自动创建CDP
- ✅ 返回cdpId

#### 2. 健康状态判定调用
- ✅ 调用health-state-assessment-service
- ✅ 获取workMode和needsClinicalMode

#### 3. 流程分支判断
- ✅ 根据workMode选择流程（临床诊疗态/健康管理态）

#### 4. CDP状态管理
- ✅ 更新CDP状态
- ✅ 查询CDP状态

#### 5. API接口
- ✅ 所有API端点可以调用
- ⚠️ 需要Mock下游服务响应

---

### 需要Mock的功能

1. **下游服务响应**: 需要Mock所有下游服务的响应格式
2. **响应解析**: 需要实现响应解析方法或使用Mock数据

---

## 📊 功能实现统计

### 实现完成度

| 功能模块 | 实现状态 | 完成度 |
|---------|---------|--------|
| 基础框架搭建 | ✅ 完整实现 | 100% |
| CDP管理功能 | ✅ 完整实现 | 100% |
| 服务间调用框架 | ✅ 完整实现 | 100% |
| 诊断流程编排框架 | ✅ 框架完整 | 80% |
| 健康筛查流程编排框架 | ✅ 完整实现 | 100% |
| 诊断编排服务 | ✅ 完整实现 | 90% |
| API接口 | ✅ 核心接口实现 | 80% |
| 响应解析方法 | 🟡 部分实现 | 40% |
| 单元测试 | ❌ 未实现 | 0% |
| 集成测试 | ❌ 未实现 | 0% |

### 简化实现统计

| 功能模块 | 实现方式 | 状态 |
|---------|---------|------|
| parseAssessmentResponse | 已完整实现 | ✅ 已实现 |
| parseParsingResponse | 已完整实现 | ✅ 已实现 |
| parseA1-A5Response | 已完整实现 | ✅ 已实现 |
| Step 2-5的9个响应解析方法 | 标记为TODO | 🟡 待实现 |
| 业务逻辑服务类 | 标记为TODO | 🟡 可选实现 |

---

## 📝 相关文档

- **服务实现方案**: `docs/diagnosis-service - 服务实现方案.md`
- **实现状态说明**: `docs/实现状态说明.md`
- **README**: `README.md`

---

## 🔄 更新日志

### 2026-01-22
- 创建已实现功能清单文档
- 记录所有已实现的核心功能
- 标注简化实现和未实现功能
- 添加测试覆盖说明

### 2026-01-22 (v1.1)
- 完善 `parseAssessmentResponse()` 方法
- 实现健康筛查流程（A1-A5）API接口和业务逻辑
- 实现健康筛查流程响应解析方法（parseA1-A5Response）
- 更新文档以反映最新实现状态

### 2026-01-23 (v1.2)
- 验证服务调用功能已正常工作（health-state-assessment-service、clinical-parsing-service）
- 验证CDP管理功能已正常工作（创建、更新、数据库持久化）
- 验证Step 1已成功执行并能够识别医学概念
- 更新文档以反映实际测试验证状态

### 2026-01-24 (v1.3)
- 根据最新代码更新文档
- 修正响应解析方法统计（7个已实现，9个待实现）
- 确认所有功能状态准确无误

---

## 💡 使用说明

1. **核心框架**: CDP管理、流程编排框架已完整实现，可用于开发
2. **业务逻辑**: 
   - ✅ Step 1的响应解析已实现（parseParsingResponse）
   - ⚠️ Step 2-5的响应解析方法待实现，需要根据下游服务API实现
3. **测试**: 单元测试和集成测试待编写
4. **下一步**: 优先实现Step 2-5的响应解析方法，确保诊断流程可以完整执行

---

**文档维护**: 当有新功能实现或优化时，请及时更新本文档。

