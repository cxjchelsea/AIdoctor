# AI医生系统 - 项目结构设计

> **文档定位**：本文档定义AI医生系统的项目目录结构、模块划分、代码组织方式等。  
> **参考文档**：《AI医生系统-技术架构设计-核心架构.md》、《AI医生系统-技术架构设计-项目实现与部署.md》、《AI医生系统-系统功能设计.md》  
> **设计基础**：基于DR.KNOWS论文，采用单主Agent + 多工具Tools架构，实现双通道推理架构

---

## 一、整体项目结构

### 1.1 项目根目录

```
AIdoctor/
├── diagnosis-service/              # 主Agent服务（Java）- CDP管理、诊断流程编排、工具调用调度
├── examination-service/             # 检查服务（Java）- 检查方案、报告识别
├── health-state-assessment-service/ # 健康状态判定工具服务（Python）- tool_0
├── clinical-parsing-service/        # 病例理解工具服务（Python）- tool_1
├── dialog-service/                  # 主动问诊工具服务（Python）- tool_2
├── diagnosis-engine-service/        # 鉴别诊断工具服务（Python）- tool_3
├── workup-planner-service/         # 检查建议工具服务（Python）- tool_4
├── treatment-engine-service/        # 治疗建议工具服务（Python）- tool_5
├── risk-assessment-service/         # 风险评估工具服务（Python）- tool_6
├── explanation-service/             # 证据链工具服务（Python）- tool_7
├── ocr-service/                     # OCR服务（Python）- 多模态理解
├── frontend/                        # 前端应用（React + TypeScript）
├── docker-compose.yml              # Docker编排配置
└── docs/                           # 文档目录
```

### 1.2 单主Agent架构说明

> **核心设计理念**：将原八大智能体重构为单主Agent + 多工具Tools架构，主Agent具备自主决策能力，工具只负责执行并返回结构化结果。

**主Agent（Clinical Agent Brain）**：
- **唯一决策者**：主Agent是唯一"最终结论提交者"，所有诊断结论、检查建议、治疗方案都由主Agent最终决定
- **自主调用工具**：主Agent根据当前CDP状态和AgentState，自主决定调用哪些工具、调用顺序、调用参数
- **停止/升级/拒答能力**：主Agent具备停止条件判断、升级策略执行、拒答边界判断的能力
- **证据融合**：主Agent内部融合多个工具返回的证据，进行冲突解决和一致性检查
- **策略状态管理**：主Agent维护AgentState，包括阈值、预算、失败回退、已尝试工具等

**工具（Tools）**：
- **无独立目标**：工具不拥有独立的诊断目标或治疗目标，只按主Agent调用执行
- **无长期策略状态**：工具不维护长期状态，每次调用都是独立的
- **结构化输出**：工具返回结构化的payload、evidence、quality、suggestedWrites
- **证据引用**：工具必须提供evidence引用，说明输出结果的依据来源
- **建议写回字段**：工具通过suggestedWrites建议主Agent写回CDP的字段路径

**原八大智能体 → 工具映射**：

| 原智能体 | 工具名称 | 工具ID | 对应服务 | 主要职责 |
|---------|---------|--------|---------|---------|
| 健康状态判定智能体 | 健康状态判定工具 | tool_0 | health-state-assessment-service | 判断工作态、入口判定流程 |
| 病例理解智能体 | 病例理解工具 | tool_1 | clinical-parsing-service | 概念归一化、结构化提取 |
| 主动问诊智能体 | 主动问诊工具 | tool_2 | dialog-service | 信息缺口识别、问诊生成 |
| 鉴别诊断智能体 | 鉴别诊断工具 | tool_3 | diagnosis-engine-service | 多引擎融合诊断、DDx生成 |
| 检查建议智能体 | 检查建议工具 | tool_4 | workup-planner-service | 检查价值评估、验证计划 |
| 治疗建议智能体 | 治疗建议工具 | tool_5 | treatment-engine-service | 治疗方案推理、药物推荐 |
| 风险评估智能体 | 风险评估工具 | tool_6 | risk-assessment-service | 高危识别、紧急程度评估 |
| 证据链智能体 | 证据链工具 | tool_7 | explanation-service | 证据链构建、解释生成 |

### 1.3 双通道推理架构说明

> **核心设计理念**：让结构化通道决定"该往哪想"，让LLM决定"怎么说、怎么问、怎么组织方案"

**通道1：结构化推理通道**（决定"该往哪想" - 临床逻辑）
- tool_1（病例理解工具）：概念归一化、结构化提取
- tool_3（鉴别诊断工具）：知识库优先 + DR.KNOWS路径验证、路径约束推理
- tool_4（检查建议工具）：检查价值评估、信息增益计算
- tool_5（治疗建议工具）：治疗方案推理、药物推荐
- tool_6（风险评估工具）：风险识别、紧急程度评估
- tool_0（健康状态判定工具）：规则推理、风险评估

**通道2：语言与策略通道**（决定"怎么说、怎么问" - 医生表达）
- tool_2（主动问诊工具）：生成问诊问题、自然语言对话
- tool_7（证据链工具）：生成解释和说明、推理路径可视化

**连接点**：CDP（Clinical Decision Package）- 两个通道通过CDP交换数据

**双通道协作机制**：
```
通道1工具（结构化推理）
    ↓
生成结构化结果（DDx、检查建议等）
    ↓
写入CDP（结构化数据）
    ↓
主Agent读取CDP
    ↓
通道2工具（语言与策略）
    ↓
读取CDP
    ↓
生成自然语言表达
    ↓
输出给用户
```

---

## 二、诊断服务（diagnosis-service）- 主Agent服务

> **说明**：diagnosis-service是主Agent（Clinical Agent Brain）的实现，负责运行循环、工具调用调度、CDP管理、证据融合、冲突解决等核心功能。

### 2.1 项目结构

```
diagnosis-service/
├── pom.xml
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── java/com/aidoctor/diagnosis/
│   │   │   ├── DiagnosisServiceApplication.java    # 启动类
│   │   │   │
│   │   │   ├── controller/                         # 控制器层
│   │   │   │   ├── DiagnosisController.java
│   │   │   │   └── HealthController.java
│   │   │   │
│   │   │   ├── agent/                              # 主Agent核心模块
│   │   │   │   ├── ClinicalAgentBrain.java         # 主Agent主类
│   │   │   │   ├── AgentLoop.java                  # 运行循环（Observe→Plan→Act→Update→Evaluate）
│   │   │   │   ├── ToolCaller.java                 # 工具调用器
│   │   │   │   ├── EvidenceFusion.java             # 证据融合算法
│   │   │   │   ├── ConflictResolution.java         # 冲突解决算法
│   │   │   │   ├── StopConditionEvaluator.java    # 停止条件评估
│   │   │   │   ├── EscalationHandler.java          # 升级策略处理
│   │   │   │   └── RefusalHandler.java            # 拒答策略处理
│   │   │   │
│   │   │   ├── service/                            # 业务逻辑层
│   │   │   │   ├── DiagnosisService.java           # 诊断服务主入口
│   │   │   │   ├── DiagnosisOrchestrationService.java  # 诊断流程编排
│   │   │   │   ├── AdaptiveQuestioningService.java # 智能追问服务
│   │   │   │   ├── CompletenessCalculator.java     # 完整度计算
│   │   │   │   │
│   │   │   │   ├── cdp/                           # CDP管理服务
│   │   │   │   │   ├── CDPManager.java            # CDP管理器
│   │   │   │   │   ├── CDPVersionService.java     # CDP版本服务
│   │   │   │   │   ├── CDPReplayService.java      # CDP回放服务
│   │   │   │   │   ├── CDPRerankService.java      # CDP重排服务
│   │   │   │   │   └── CDPRollbackService.java    # CDP回退服务
│   │   │   │   │
│   │   │   │   ├── agent_state/                    # AgentState管理服务
│   │   │   │   │   ├── AgentStateManager.java     # AgentState管理器
│   │   │   │   │   └── AgentStateService.java      # AgentState服务
│   │   │   │   │
│   │   │   │   ├── orchestration/                 # 流程编排
│   │   │   │   │   └── DiagnosisWorkflowCoordinator.java
│   │   │   │   │
│   │   │   │   ├── step1/                        # Step 1：识别问题
│   │   │   │   │   ├── ConceptNormalizationService.java
│   │   │   │   │   └── StructuredQuestionListBuilder.java
│   │   │   │   │
│   │   │   │   ├── step2/                        # Step 2：构建鉴别诊断候选集
│   │   │   │   │   ├── DDxCandidateGenerator.java
│   │   │   │   │   └── ThreeLayerClassifier.java
│   │   │   │   │
│   │   │   │   ├── step3/                        # Step 3：组织候选集并建立分流路径
│   │   │   │   │   ├── ReasoningGroupOrganizer.java
│   │   │   │   │   └── RoutingPathDesigner.java
│   │   │   │   │
│   │   │   │   ├── step4/                        # Step 4：采集关键证据
│   │   │   │   │   ├── KeyEvidenceCollector.java
│   │   │   │   │   ├── EvidenceAnalyzer.java
│   │   │   │   │   └── VerificationPlanBuilder.java
│   │   │   │   │
│   │   │   │   ├── step5/                        # Step 5：回填证据并输出结论
│   │   │   │   │   ├── EvidenceBackfillService.java
│   │   │   │   │   ├── DDxRankingUpdater.java
│   │   │   │   │   └── ConclusionPackageBuilder.java
│   │   │   │   │
│   │   │   │   ├── health-state-assessment/       # 健康状态判定服务代理
│   │   │   │   │   ├── HealthStateAssessmentService.java
│   │   │   │   │   └── EntryAssessmentService.java
│   │   │   │   │
│   │   │   │   └── wellness-screening/            # 健康筛查流程编排
│   │   │   │       ├── DemandClassificationService.java
│   │   │   │       ├── HealthProfileCollectionService.java
│   │   │   │       ├── BranchExecutionService.java
│   │   │   │       ├── UnifiedResultGeneratorService.java
│   │   │   │       └── FollowUpSetupService.java
│   │   │   │
│   │   │   ├── repository/                       # 数据访问层
│   │   │   │   ├── DiagnosisRecordRepository.java
│   │   │   │   ├── CDPRepository.java
│   │   │   │   ├── CDPVersionRepository.java
│   │   │   │   ├── HealthStateAssessmentRecordRepository.java
│   │   │   │   └── WellnessScreeningRecordRepository.java
│   │   │   │
│   │   │   ├── entity/                            # 实体类
│   │   │   │   ├── DiagnosisRecord.java
│   │   │   │   ├── CDP.java
│   │   │   │   ├── CDPVersion.java
│   │   │   │   ├── AgentState.java                # AgentState实体
│   │   │   │   ├── AuditTrail.java                # AuditTrail实体
│   │   │   │   └── HealthStateAssessmentRecord.java
│   │   │   │
│   │   │   ├── dto/                               # DTO类
│   │   │   │   ├── tool/                         # 工具调用DTO
│   │   │   │   │   ├── ToolContext.java          # 工具调用上下文
│   │   │   │   │   └── ToolResult.java           # 工具返回结果
│   │   │   │
│   │   │   ├── dto/                               # DTO类
│   │   │   │   ├── request/                       # 请求DTO
│   │   │   │   ├── response/                      # 响应DTO
│   │   │   │   ├── questionlist/                  # 结构化问题清单DTO
│   │   │   │   ├── threeLayer/                    # 三层分层结果DTO
│   │   │   │   ├── conclusion/                    # 终点结论包DTO
│   │   │   │   └── evidence/                      # 证据分析DTO
│   │   │   │
│   │   │   ├── client/                            # Feign客户端（工具服务调用）
│   │   │   │   ├── Tool0Client.java               # tool_0: 健康状态判定工具
│   │   │   │   ├── Tool1Client.java               # tool_1: 病例理解工具
│   │   │   │   ├── Tool2Client.java               # tool_2: 主动问诊工具
│   │   │   │   ├── Tool3Client.java               # tool_3: 鉴别诊断工具
│   │   │   │   ├── Tool4Client.java               # tool_4: 检查建议工具
│   │   │   │   ├── Tool5Client.java               # tool_5: 治疗建议工具
│   │   │   │   ├── Tool6Client.java               # tool_6: 风险评估工具
│   │   │   │   ├── Tool7Client.java               # tool_7: 证据链工具
│   │   │   │   └── OcrServiceClient.java          # OCR服务
│   │   │   │
│   │   │   ├── scheduler/                         # 工具调用调度层
│   │   │   │   ├── ToolScheduler.java             # 工具调度器
│   │   │   │   ├── ParallelExecutor.java         # 并行执行器
│   │   │   │   ├── AsyncExecutor.java            # 异步执行器
│   │   │   │   ├── TimeoutHandler.java           # 超时处理
│   │   │   │   ├── RetryHandler.java             # 重试处理
│   │   │   │   └── RateLimiter.java              # 限流器
│   │   │   │
│   │   │   ├── config/                            # 配置类
│   │   │   │   ├── SwaggerConfig.java
│   │   │   │   └── FeignConfig.java
│   │   │   │
│   │   │   ├── exception/                         # 异常类
│   │   │   │   ├── BusinessException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │
│   │   │   └── util/                              # 工具类
│   │   │       ├── JsonUtil.java
│   │   │       └── EvidenceTracer.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-dev.yml
│   │
│   └── test/
│       └── java/com/aidoctor/diagnosis/
│
└── .gitignore
```

### 2.2 关键说明

- **diagnosis-service** 是主Agent服务，负责：
  - 运行循环（Observe→Plan→Act→Update→Evaluate）
  - 工具调用调度（并行、异步、超时、重试、限流）
  - CDP管理（创建、更新、版本控制、回放、回退）
  - AgentState管理（阈值、预算、失败回退、已尝试工具）
  - AuditTrail管理（工具调用记录、CDP更新记录、主Agent决策记录）
  - 证据融合与冲突解决
  - 停止条件评估、升级策略执行、拒答边界判断
- **agent/** 目录包含主Agent核心算法实现
- **scheduler/** 目录包含工具调用调度层实现（基于Redis消息队列）
  - 职责：工具调用调度（并行、异步、超时、重试、限流）
  - 重要：消息层不拥有决策提交权，主Agent仍是唯一提交者
- **step1-step5** 对应临床诊疗态的5步AI循证诊断流程（已整合到主Agent运行循环中）
- **wellness-screening** 是健康筛查流程的编排层，实际业务逻辑在Python服务中

---

## 三、健康状态判定服务（health-state-assessment-service）

### 3.1 项目结构

```
health-state-assessment-service/
├── requirements.txt
├── Dockerfile
├── run.py
├── app/
│   ├── __init__.py
│   ├── main.py                      # FastAPI应用入口
│   │
│   ├── api/                         # API路由
│   │   ├── __init__.py
│   │   └── routes.py
│   │
│   ├── services/                    # 业务服务
│   │   ├── __init__.py
│   │   ├── health_state_assessment.py  # 健康状态判定服务（tool_0）
│   │   ├── wellness_plan_generator.py   # 健康管理计划生成
│   │   │
│   │   ├── entry_assessment/           # AI诊断入口判定（P0模块）
│   │   │   ├── __init__.py
│   │   │   ├── step1_receive_input.py
│   │   │   ├── step2_identify_symptom.py
│   │   │   ├── step3_clarification.py
│   │   │   ├── step4_red_flag_check.py
│   │   │   └── step5_path_selection.py
│   │   │
│   │   └── wellness-screening/         # 健康筛查流程（A路径）
│   │       ├── __init__.py
│   │       ├── a1-demand-classifier/    # A1需求分类模块
│   │       │   ├── __init__.py
│   │       │   ├── demand_classifier.py
│   │       │   └── intent_recognizer.py
│   │       ├── a2-profile-collector/     # A2收集健康画像模块
│   │       │   ├── __init__.py
│   │       │   ├── profile_collector.py
│   │       │   ├── gap_identifier.py
│   │       │   └── completeness_calculator.py
│   │       ├── a3-branch-executor/      # A3执行分支模块
│   │       │   ├── __init__.py
│   │       │   ├── branch_executor.py
│   │       │   ├── screening_engine.py
│   │       │   ├── goal_manager.py
│   │       │   └── planned_needs_handler.py
│   │       ├── a4-result-generator/     # A4生成统一结果模块
│   │       │   ├── __init__.py
│   │       │   ├── result_generator.py
│   │       │   └── summary_builder.py
│   │       └── a5-followup-manager/     # A5设置随访模块
│   │           ├── __init__.py
│   │           ├── followup_manager.py
│   │           └── schedule_builder.py
│   │
│   ├── models/                       # 数据模型
│   │   ├── __init__.py
│   │   ├── request.py
│   │   └── response.py
│   │
│   ├── detectors/                    # 检测器
│   │   ├── __init__.py
│   │   ├── symptom_severity_detector.py
│   │   ├── risk_signal_detector.py
│   │   └── red_flag_detector.py
│   │
│   ├── rules/                       # 规则库
│   │   ├── __init__.py
│   │   ├── severity_rules.py
│   │   ├── risk_screening_rules.py
│   │   ├── red_flag_rules.py
│   │   ├── work_mode_rules.py
│   │   └── wellness_plan_rules.py
│   │
│   ├── config/                      # 配置
│   │   ├── __init__.py
│   │   └── settings.py
│   │
│   └── utils/                       # 工具类
│       ├── __init__.py
│       └── exceptions.py
│
└── README.md
```

### 3.2 关键说明

- **entry_assessment** 实现AI诊断入口判定（P0模块），包含5个步骤
- **wellness-screening** 实现健康筛查流程（A路径），包含A1-A5五个模块

---

## 四、病例理解服务（clinical-parsing-service）

### 4.1 项目结构

```
clinical-parsing-service/
├── requirements.txt
├── Dockerfile
├── run.py
├── app/
│   ├── __init__.py
│   ├── main.py                      # FastAPI应用入口
│   │
│   ├── api/                         # API路由
│   │   ├── __init__.py
│   │   └── routes.py
│   │
│   ├── services/                    # 业务服务
│   │   ├── __init__.py
│   │   └── parsing_service.py       # 病例理解服务（tool_1）
│   │
│   ├── models/                      # 数据模型
│   │   ├── __init__.py
│   │   ├── request.py
│   │   └── response.py
│   │
│   ├── config/                      # 配置
│   │   ├── __init__.py
│   │   └── settings.py
│   │
│   └── utils/                       # 工具类
│       ├── __init__.py
│       └── exceptions.py
│
└── README.md
```

---

## 五、对话管理服务（dialog-service）

### 5.1 项目结构

```
dialog-service/
├── requirements.txt
├── Dockerfile
├── run.py
├── app/
│   ├── __init__.py
│   ├── main.py                      # FastAPI应用入口
│   │
│   ├── api/                         # API路由
│   │   ├── __init__.py
│   │   └── routes.py
│   │
│   ├── services/                    # 业务服务
│   │   ├── __init__.py
│   │   └── dialog_service.py       # 对话管理服务（tool_2）
│   │
│   ├── core/                        # 核心功能
│   │   ├── __init__.py
│   │   ├── adaptive_questioning.py # 智能追问
│   │   ├── nlg.py                   # 自然语言生成
│   │   └── nlu.py                   # 自然语言理解
│   │
│   ├── normalizers/                 # 归一化器
│   │   ├── __init__.py
│   │   └── concept_normalizer.py
│   │
│   ├── builders/                    # 构建器
│   │   ├── __init__.py
│   │   └── question_list_builder.py
│   │
│   ├── identifiers/                 # 识别器
│   │   ├── __init__.py
│   │   └── information_gap_identifier.py
│   │
│   ├── calculators/                 # 计算器
│   │   ├── __init__.py
│   │   └── completeness_calculator.py
│   │
│   ├── models/                      # 数据模型
│   │   ├── __init__.py
│   │   ├── request.py
│   │   └── response.py
│   │
│   ├── config/                      # 配置
│   │   ├── __init__.py
│   │   └── settings.py
│   │
│   └── utils/                       # 工具类
│       ├── __init__.py
│       └── exceptions.py
│
└── README.md
```

---

## 六、诊断引擎服务（diagnosis-engine-service）

### 6.1 项目结构

```
diagnosis-engine-service/
├── requirements.txt
├── Dockerfile
├── run.py
├── app/
│   ├── __init__.py
│   ├── main.py                      # FastAPI应用入口
│   │
│   ├── api/                         # API路由
│   │   ├── __init__.py
│   │   └── routes.py
│   │
│   ├── services/                    # 业务服务
│   │   ├── __init__.py
│   │   └── diagnosis_service.py
│   │
│   ├── kg-reasoning-engine/         # 知识图谱推理引擎（DR.KNOWS核心）
│   │   ├── __init__.py
│   │   ├── kg_client.py             # Neo4j客户端
│   │   ├── kg_reasoning_engine.py   # 知识图谱推理引擎主类
│   │   ├── path_retriever.py        # 多跳推理路径检索
│   │   ├── path_scorer.py           # 路径评分器
│   │   ├── prior_scorer.py          # 先验概率评分器
│   │   ├── likelihood_scorer.py    # 似然评分器
│   │   ├── posterior_scorer.py      # 后验概率评分器
│   │   └── path_injector.py         # 路径注入LLM
│   │
│   ├── multi-engine-fusion/         # 多引擎融合诊断
│   │   ├── __init__.py
│   │   ├── base_engine.py           # 引擎基类
│   │   ├── rule_engine.py           # 规则引擎
│   │   ├── kg_engine.py             # 知识图谱引擎
│   │   ├── statistical_engine.py    # 统计模型引擎
│   │   ├── llm_engine.py            # 大模型引擎
│   │   ├── differential_engine.py   # 鉴别诊断引擎
│   │   ├── fusion_engine.py         # 结果融合引擎
│   │   └── fusion_service.py        # 融合服务
│   │
│   ├── engines/                     # 引擎模块（备用目录）
│   │   ├── __init__.py
│   │   └── ...
│   │
│   ├── classifiers/                 # 分类器
│   │   ├── __init__.py
│   │   ├── three_layer_classifier.py  # 三层分层分类器
│   │   └── evidence_tracer.py         # 证据追踪器
│   │
│   ├── analyzers/                   # 分析器
│   │   ├── __init__.py
│   │   ├── evidence_analyzer.py     # 证据分析器
│   │   └── reasoning_organizer.py   # 推理组织器
│   │
│   ├── builders/                    # 构建器
│   │   └── __init__.py
│   │
│   ├── models/                      # 数据模型
│   │   ├── __init__.py
│   │   ├── request.py
│   │   └── response.py
│   │
│   ├── config/                      # 配置
│   │   ├── __init__.py
│   │   └── settings.py
│   │
│   └── utils/                       # 工具类
│       ├── __init__.py
│       └── exceptions.py
│
└── README.md
```

### 6.2 关键说明

- **kg-reasoning-engine** 是DR.KNOWS方法的核心实现
- **multi-engine-fusion** 实现多引擎融合诊断
- **classifiers** 和 **analyzers** 支持三层分层和证据分析

---

## 七、检查建议服务（workup-planner-service）

### 7.1 项目结构

```
workup-planner-service/
├── requirements.txt
├── Dockerfile
├── run.py
├── app/
│   ├── __init__.py
│   ├── main.py                      # FastAPI应用入口
│   │
│   ├── api/                         # API路由
│   │   ├── __init__.py
│   │   └── routes.py
│   │
│   ├── services/                    # 业务服务
│   │   ├── __init__.py
│   │   ├── workup_planner.py        # 检查建议引擎（tool_4）
│   │   └── verification_planner.py  # 验证计划构建器
│   │
│   ├── calculators/                 # 计算器
│   │   ├── __init__.py
│   │   ├── value_assessor.py        # 检查价值评估
│   │   └── information_gain_calculator.py  # 信息增益计算
│   │
│   ├── models/                      # 数据模型
│   │   ├── __init__.py
│   │   ├── request.py
│   │   └── response.py
│   │
│   ├── config/                      # 配置
│   │   ├── __init__.py
│   │   └── settings.py
│   │
│   └── utils/                       # 工具类
│       ├── __init__.py
│       └── exceptions.py
│
└── README.md
```

---

## 八、治疗推理服务（treatment-engine-service）

### 8.1 项目结构

```
treatment-engine-service/
├── requirements.txt
├── Dockerfile
├── run.py
├── app/
│   ├── __init__.py
│   ├── main.py                      # FastAPI应用入口
│   │
│   ├── api/                         # API路由
│   │   ├── __init__.py
│   │   └── routes.py
│   │
│   ├── services/                    # 业务服务
│   │   ├── __init__.py
│   │   ├── treatment_engine.py     # 治疗方案推理引擎（tool_5）
│   │   └── medication_recommender.py  # 药物推荐器
│   │
│   ├── models/                      # 数据模型
│   │   ├── __init__.py
│   │   ├── request.py
│   │   └── response.py
│   │
│   ├── config/                      # 配置
│   │   ├── __init__.py
│   │   └── settings.py
│   │
│   └── utils/                       # 工具类
│       ├── __init__.py
│       └── exceptions.py
│
└── README.md
```

---

## 九、风险评估服务（risk-assessment-service）

### 9.1 项目结构

```
risk-assessment-service/
├── requirements.txt
├── Dockerfile
├── run.py
├── app/
│   ├── __init__.py
│   ├── main.py                      # FastAPI应用入口
│   │
│   ├── api/                         # API路由
│   │   ├── __init__.py
│   │   └── routes.py
│   │
│   ├── services/                    # 业务服务
│   │   ├── __init__.py
│   │   ├── risk_assessment_engine.py  # 风险评估引擎（tool_6）
│   │   ├── triage_engine.py         # 分诊引擎
│   │   └── upgrade_rule_engine.py   # 升级规则引擎
│   │
│   ├── builders/                    # 构建器
│   │   ├── __init__.py
│   │   └── conclusion_package_builder.py  # 终点结论包构建器
│   │
│   ├── models/                      # 数据模型
│   │   ├── __init__.py
│   │   ├── request.py
│   │   └── response.py
│   │
│   ├── config/                      # 配置
│   │   ├── __init__.py
│   │   └── settings.py
│   │
│   └── utils/                       # 工具类
│       ├── __init__.py
│       └── exceptions.py
│
└── README.md
```

---

## 十、解释生成服务（explanation-service）

### 10.1 项目结构

```
explanation-service/
├── requirements.txt
├── Dockerfile
├── run.py
├── app/
│   ├── __init__.py
│   ├── main.py                      # FastAPI应用入口
│   │
│   ├── api/                         # API路由
│   │   ├── __init__.py
│   │   └── routes.py
│   │
│   ├── services/                    # 业务服务
│   │   ├── __init__.py
│   │   └── explanation_service.py   # 解释生成服务（tool_7）
│   │
│   ├── models/                      # 数据模型
│   │   ├── __init__.py
│   │   ├── request.py
│   │   └── response.py
│   │
│   ├── config/                      # 配置
│   │   ├── __init__.py
│   │   └── settings.py
│   │
│   └── utils/                       # 工具类
│       ├── __init__.py
│       └── exceptions.py
│
└── README.md
```

---

## 十一、OCR服务（ocr-service）

### 11.1 项目结构

```
ocr-service/
├── requirements.txt
├── Dockerfile
├── run.py
├── app/
│   ├── __init__.py
│   ├── main.py                      # FastAPI应用入口
│   │
│   ├── api/                         # API路由
│   │   ├── __init__.py
│   │   └── routes.py
│   │
│   ├── services/                    # 业务服务
│   │   ├── __init__.py
│   │   ├── ocr_service.py           # OCR服务
│   │   └── parser_service.py        # 解析服务
│   │
│   ├── models/                      # 数据模型
│   │   ├── __init__.py
│   │   ├── request.py
│   │   └── response.py
│   │
│   └── utils/                       # 工具类
│       ├── __init__.py
│       └── image_processor.py
│
└── README.md
```

---

## 十二、前端应用（frontend）

### 12.1 项目结构

```
frontend/
├── package.json
├── tsconfig.json
├── vite.config.ts
├── index.html
├── src/
│   ├── main.tsx                     # 应用入口
│   ├── App.tsx                      # 根组件
│   │
│   ├── components/                  # 组件
│   │   ├── diagnosis/               # 诊断相关组件
│   │   ├── dialog/                  # 对话相关组件
│   │   └── ...
│   │
│   ├── pages/                       # 页面
│   │   └── ...
│   │
│   ├── services/                    # 服务层
│   │   └── api.ts                   # API调用
│   │
│   ├── stores/                      # 状态管理
│   │   └── ...
│   │
│   ├── types/                       # 类型定义
│   │   └── ...
│   │
│   └── utils/                        # 工具函数
│       └── ...
│
└── README.md
```

---

## 十三、代码组织规范

### 13.1 Python服务规范

**目录结构**：
- `app/main.py`：FastAPI应用入口
- `app/api/`：API路由定义
- `app/services/`：业务逻辑服务
- `app/models/`：数据模型（Pydantic）
- `app/config/`：配置文件
- `app/utils/`：工具类

**命名规范**：
- **文件名**：小写下划线，如 `parsing_service.py`
- **类名**：大驼峰，如 `ParsingService`
- **函数名**：小写下划线，如 `extract_concepts`
- **常量**：全大写下划线，如 `MAX_RETRY_COUNT`

### 13.2 Java服务规范

**目录结构**：
- `controller/`：控制器层，处理HTTP请求
- `service/`：业务逻辑层
- `repository/`：数据访问层
- `entity/`：实体类，对应数据库表
- `dto/`：数据传输对象
- `client/`：外部服务客户端（Feign）
- `config/`：配置类
- `exception/`：异常类
- `util/`：工具类

**命名规范**：
- **类名**：大驼峰，如 `DiagnosisService`
- **方法名**：小驼峰，如 `startDiagnosis`
- **常量**：全大写下划线，如 `MAX_QUESTIONING_COUNT`
- **包名**：全小写，如 `com.aidoctor.diagnosis`

### 13.3 前端规范

**目录结构**：
- `components/`：可复用组件
- `pages/`：页面组件
- `services/`：API服务层
- `stores/`：状态管理
- `types/`：TypeScript类型定义
- `utils/`：工具函数

**命名规范**：
- **组件名**：大驼峰，如 `DiagnosisInfoPanel`
- **文件名**：与组件名一致
- **函数名**：小驼峰，如 `fetchDiagnosisData`

---

## 十四、开发环境配置

### 14.1 必需工具

- **Java**：JDK 8+
- **Python**：Python 3.10+
- **Node.js**：Node.js 18+
- **Maven**：Maven 3.8+
- **Docker**：Docker 20.10+
- **Docker Compose**：Docker Compose 2.0+

### 14.2 数据库和中间件

- **MySQL**：8.0+（用于Java服务）
- **Redis**：7.0+（用于缓存和消息队列）
- **Neo4j**：5.0+（用于知识图谱）

### 14.3 启动服务

```bash
# 启动基础设施
docker-compose up -d mysql redis neo4j

# 启动Java服务
cd diagnosis-service
mvn spring-boot:run

# 启动Python服务
cd diagnosis-engine-service
pip install -r requirements.txt
python run.py
```

---

## 十五、服务端口分配

| 服务 | 端口 | 说明 |
|------|------|------|
| diagnosis-service | 8084 | 诊断服务（Java） |
| examination-service | 8085 | 检查服务（Java） |
| diagnosis-engine-service | 8086 | 诊断引擎服务（Python） |
| ocr-service | 8087 | OCR服务（Python） |
| dialog-service | 8088 | 对话管理服务（Python） |
| explanation-service | 8089 | 解释生成服务（Python） |
| workup-planner-service | 8090 | 检查建议服务（Python） |
| treatment-engine-service | 8091 | 治疗推理服务（Python） |
| risk-assessment-service | 8092 | 风险评估服务（Python） |
| health-state-assessment-service | 8081 | 健康状态判定服务（Python） |
| clinical-parsing-service | 8082 | 病例理解服务（Python） |
| frontend | 5173 | 前端应用（开发端口） |

---

**文档版本**：v4.0  
**创建日期**：2025年1月  
**更新日期**：2025年1月  
**文档定位**：AI医生系统的项目结构设计（目录结构、代码组织、开发环境）  
**参考文档**：《AI医生系统-系统功能设计.md》、《AI医生系统-技术架构设计.md》  
**设计基础**：基于DR.KNOWS论文，采用单主Agent + 多工具Tools架构设计
