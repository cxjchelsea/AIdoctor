# AI医生系统 - 业务逻辑详细设计

> **文档定位**：本文档详细设计AI医生系统的核心业务逻辑，包括主Agent运行循环、工具调用流程、DR.KNOWS路径检索、智能追问算法、多引擎融合、CDP管理等。  
> **参考文档**：《AI医生系统-系统功能设计.md》、《AI医生系统-技术架构设计-核心架构.md》、《AI医生系统-技术架构设计-主Agent核心算法设计.md》  
> **设计基础**：基于DR.KNOWS论文，采用单主Agent + 多工具Tools架构，实现双通道推理架构

---

## 一、AI医生系统单主Agent架构的业务逻辑设计

根据系统设计方案，AI医生系统采用**单主Agent + 多工具Tools架构设计**，基于DR.KNOWS论文的方法。主Agent（Clinical Agent Brain）是唯一"最终结论提交者"，通过运行循环自主调用工具，完成诊断流程。本节详细设计主Agent运行循环和各个工具的业务逻辑。

### 0.0 单主Agent架构与双通道推理（核心设计理念）

> **参考文档**：《AI医生系统-技术架构设计-核心架构.md》  
> **核心原则**：让结构化通道决定"该往哪想"，让LLM决定"怎么说、怎么问、怎么组织方案"

**单主Agent架构核心理念**：
- **主Agent自主性**：主Agent（Clinical Agent Brain）具备自主感知、推理、决策和执行能力，是唯一"最终结论提交者"
- **工具无状态性**：工具无独立目标、无长期策略状态，只按主Agent调用执行并返回结构化结果与证据引用
- **CDP唯一事实源**：CDP是病例事实的唯一事实源，所有工具从CDP读取，建议写回CDP
- **证据融合与冲突解决**：主Agent内部进行evidence fusion和conflict resolution，而非自治agent协商
- **审计可追溯**：所有工具调用、证据、写回字段、版本、时间必须记录到AuditTrail

**双通道推理架构**：

AI医生系统采用**双通道推理架构**，将临床推理分为两个通道：

**通道1：结构化推理通道**（决定"该往哪想" - 临床逻辑）
- **主要工具**：tool_1（病例理解）、tool_3（鉴别诊断）、tool_4（检查建议）、tool_5（治疗建议）、tool_6（风险评估）、tool_0（健康状态判定）
- **技术实现**：
  - 医学概念标准化（CUI/ICD/SNOMED）
  - 知识图谱路径检索与排序（DR.KNOWS方法）
  - 规则/概率/贝叶斯/评分量表（可插拔）
  - 多引擎融合诊断
- **输出**：DDx候选 + 证据结构 + 推理路径（写入CDP）

**通道2：语言与策略通道**（决定"怎么说、怎么问" - 医生表达）
- **主要工具**：tool_2（主动问诊）、tool_7（证据链）
- **技术实现**：
  - 问诊对话生成（问什么、怎么问）
  - 解释与沟通（把结构化结果讲成人话）
  - 生成处置方案草案（在受控证据基础上）
  - 自然语言生成（NLG）
- **输出**：自然语言问诊、解释、建议

**两个通道通过CDP（Clinical Decision Package）连接**：
- 结构化通道的输出写入CDP
- 语言通道基于CDP生成自然语言表达
- 主Agent在运行循环中协调两个通道的工作
- 确保推理过程可追溯、可审计

### 1.0 核心工作流程概述

**主Agent运行循环**（Observe→Plan→Act→Update→Evaluate→Stop/Escalate）：

```
用户发起咨询
    ↓
主Agent创建CDP，初始化AgentState
    ↓
【主Agent运行循环开始】
    ↓
Observe（观察）：读取CDP状态、识别信息缺口
    ↓
Plan（规划）：决定调用 tool_0（健康状态判定工具）
    ↓
Act（执行）：调用 tool_0
    ├─ 症状严重程度评估
    ├─ 风险早筛
    ├─ 红旗信号识别
    └─ 工作态判定（健康管理态/临床诊疗态）
    ↓
Update（更新）：写回 cdp.health_state_assessment
    ↓
Evaluate（评估）：判断工作态
    ↓
    ├─ 【健康管理态】                    ├─ 【临床诊疗态】
    │   ↓                                  │   ↓
    │   主Agent调用工具生成健康管理计划      │   【主Agent运行循环继续】
    │   └─ 健康管理态结束                   │       ↓
    │                                       │   Observe：读取CDP状态
    │                                       │       ↓
    │                                       │   Plan：决定调用 tool_1（病例理解工具）
    │                                       │       ↓
    │                                       │   Act：调用 tool_1
    │                                       │       ├─ 医学概念识别
    │                                       │       ├─ 概念归一化
    │                                       │       └─ 结构化提取
    │                                       │       ↓
    │                                       │   Update：写回 cdp.patient_state
    │                                       │       ↓
    │                                       │   Plan：决定调用 tool_2（主动问诊工具）
    │                                       │       ↓
    │                                       │   Act：调用 tool_2
    │                                       │       ├─ 信息缺口识别
    │                                       │       ├─ 智能追问生成
    │                                       │       └─ 信息完整度计算
    │                                       │       ↓
    │                                       │   Update：写回 cdp.uncertainty.missing_critical_info
    │                                       │       ↓
    │                                       │   Plan：决定调用 tool_3（鉴别诊断工具）
    │                                       │       ↓
    │                                       │   Act：调用 tool_3（DR.KNOWS核心）
    │                                       │       ├─ 知识图谱路径检索（Neo4j）
    │                                       │       ├─ 路径评分排序（SGIN + 注意力）
    │                                       │       ├─ 路径注入LLM
    │                                       │       ├─ 多引擎融合（规则/KG/统计/LLM/鉴别）
    │                                       │       └─ 三层排序（首要假设/主要备选/必须排除）
    │                                       │       ↓
    │                                       │   Update：写回 cdp.ddx
    │                                       │       ↓
    │                                       │   Plan：决定调用 tool_4（检查建议工具）
    │                                       │       ↓
    │                                       │   Act：调用 tool_4
    │                                       │       ├─ 检查价值评估
    │                                       │       └─ 验证计划生成
    │                                       │       ↓
    │                                       │   Update：写回 cdp.workup_plan
    │                                       │       ↓
    │                                       │   Plan：决定调用 tool_5（治疗建议工具）
    │                                       │       ↓
    │                                       │   Act：调用 tool_5
    │                                       │       ├─ 治疗方案推理
    │                                       │       └─ 药物推荐
    │                                       │       ↓
    │                                       │   Update：写回 cdp.management_plan
    │                                       │       ↓
    │                                       │   Plan：决定调用 tool_6（风险评估工具）
    │                                       │       ↓
    │                                       │   Act：调用 tool_6
    │                                       │       ├─ 高危识别
    │                                       │       └─ 紧急程度分级
    │                                       │       ↓
    │                                       │   Update：写回 cdp.triage
    │                                       │       ↓
    │                                       │   Plan：决定调用 tool_7（证据链工具）
    │                                       │       ↓
    │                                       │   Act：调用 tool_7
    │                                       │       ├─ 证据链构建
    │                                       │       └─ 终点结论包生成
    │                                       │       ↓
    │                                       │   Update：写回 cdp.evidence_graph、cdp.final_conclusion
    │                                       │       ↓
    │                                       │   Evaluate：评估停止条件
    │                                       │       ↓
    │                                       │   Stop：输出终点结论包
    │                                       │       ↓
    │                                       │   CDP生成（版本控制）
```

**关键说明**：
- 主Agent通过运行循环（Observe→Plan→Act→Update→Evaluate）自主决定调用哪些工具
- 每个工具调用都遵循ToolContext输入和ToolResult输出的协议
- 主Agent根据工具返回的suggestedWrites决定是否写回CDP
- 主Agent进行证据融合和冲突解决，确保诊断结论的一致性

---

### 1.1 工具0：健康状态判定工具（tool_0）

> **对应服务**：health-state-assessment-service  
> **工具类型**：Deterministic  
> **参考文档**：《AI医生系统-技术架构设计-智能体详细设计.md》4.0节

#### 1.1.1 业务目标

**核心目标**：判断"这个人，现在需要被当成'病人'对待吗？"这是医生的第一职责，发生在"诊断之前"。

#### 1.1.2 AI诊断入口判定流程（P0模块）

**流程概述**：入口判定模块通过五个步骤完成用户意图识别、症状识别、方向澄清、危险信号检查和路径输出。

**Step 1｜接收用户输入**

**功能**：接收用户自然语言描述本次来访目的

**业务逻辑**：
```java
/**
 * Step 1：接收用户输入
 */
public UserIntent step1ReceiveInput(String userInput) {
    // 1. 使用NLU引擎理解用户意图
    NLUResult nluResult = nluEngine.parse(userInput);
    
    // 2. 提取实体（症状、疾病、检查等）
    List<Entity> entities = nluResult.getEntities();
    
    // 3. 判断是否为混合诉求
    boolean isMixed = detectMixedRequest(nluResult);
    
    return UserIntent.builder()
        .text(userInput)
        .intent(nluResult.getIntent())
        .entities(entities)
        .isMixed(isMixed)
        .build();
}
```

**Step 2｜识别用户是否"有症状/困扰"**

**功能**：系统对用户输入做一次判断，识别用户是否有症状/困扰

**业务逻辑**：
```java
/**
 * Step 2：识别用户是否"有症状/困扰"
 */
public SymptomStatus step2IdentifySymptom(UserIntent userIntent) {
    // 1. 识别症状/困扰表达
    List<String> symptoms = symptomRecognizer.recognize(userIntent.getText());
    
    // 2. 识别健康管理/体检规划表达
    boolean hasWellnessIntent = hasWellnessKeywords(userIntent.getText());
    
    // 3. 判断情况
    String status;
    if (symptoms.isEmpty() && hasWellnessIntent) {
        // 情况A：明确无症状
        status = "no_symptom";
    } else if (!symptoms.isEmpty()) {
        // 情况B：存在症状/困扰
        status = "has_symptom";
    } else {
        // 情况C：不确定/模糊/混合诉求
        status = "uncertain";
    }
    
    return SymptomStatus.builder()
        .status(status)
        .symptoms(symptoms)
        .hasWellnessIntent(hasWellnessIntent)
        .isMixed(userIntent.isMixed())
        .build();
}
```

**Step 3｜方向澄清（仅对"情况C"触发一次）**

**功能**：通过最小澄清确认用户当前的主要目标

**业务逻辑**：
```java
/**
 * Step 3：方向澄清（仅对"情况C"触发一次）
 */
public ClarificationResult step3Clarification(UserIntent userIntent) {
    // 1. 生成最小澄清问题
    String clarificationQuestion = clarificationEngine.generateQuestion(userIntent);
    
    // 2. 等待用户回答（在实际实现中，这里是异步的）
    String userResponse = waitForUserResponse(clarificationQuestion);
    
    // 3. 根据用户回答确定方向
    String direction = clarificationEngine.determineDirection(userResponse);
    // direction = "A"（健康筛查）或 "B"（症状诊断）
    
    return ClarificationResult.builder()
        .question(clarificationQuestion)
        .direction(direction)
        .clarified(userResponse != null)
        .build();
}
```

**Step 4｜危险信号检查（安全兜底，所有用户都要过一次）**

**功能**：无论用户走 A 还是 B，都必须判断是否存在危险信号

**业务逻辑**：
```java
/**
 * Step 4：危险信号检查（安全兜底）
 */
public RedFlagsCheck step4RedFlagCheck(UserIntent userIntent, BasicInfo basicInfo) {
    // 1. 检测危险信号
    List<RedFlag> redFlags = redFlagDetector.detect(
        userIntent.getText(), 
        basicInfo
    );
    
    // 2. 判断是否命中危险信号
    boolean redFlagsHit = !redFlags.isEmpty();
    
    // 3. 生成安全提示消息
    String safetyMessage = null;
    if (redFlagsHit) {
        safetyMessage = generateSafetyMessage(redFlags);
    }
    
    return RedFlagsCheck.builder()
        .redFlagsHit(redFlagsHit)
        .redFlags(redFlags)
        .safetyMessage(safetyMessage)
        .shouldExit(redFlagsHit)
        .build();
}
```

**Step 5｜输出路径结果并跳转**

**功能**：根据前面的判断结果，输出明确的路径选择并跳转

**业务逻辑**：
```java
/**
 * Step 5：输出路径结果并跳转
 */
public PathResult step5PathSelection(
        SymptomStatus symptomStatus, 
        ClarificationResult clarificationResult, 
        RedFlagsCheck redFlagsCheck) {
    
    // 1. 如果命中危险信号，退出线上流程
    if (redFlagsCheck.isRedFlagsHit()) {
        return PathResult.builder()
            .path("exit")
            .pathName("退出线上流程")
            .reason("危险信号命中")
            .message(redFlagsCheck.getSafetyMessage())
            .build();
    }
    
    // 2. 确定路径
    String path;
    String nextStep;
    
    if ("no_symptom".equals(symptomStatus.getStatus())) {
        path = "A";  // 健康筛查路径
        nextStep = "A1｜需求分类";
    } else if ("has_symptom".equals(symptomStatus.getStatus())) {
        path = "B";  // 症状诊断路径
        nextStep = "阶段1｜问诊";
    } else if ("uncertain".equals(symptomStatus.getStatus())) {
        // 根据澄清结果确定路径
        if (clarificationResult != null) {
            path = clarificationResult.getDirection();
            nextStep = "A".equals(path) ? "A1｜需求分类" : "阶段1｜问诊";
        } else {
            // 默认进入症状诊断路径（宁可误报，不能漏报）
            path = "B";
            nextStep = "阶段1｜问诊";
        }
    } else {
        path = "B";
        nextStep = "阶段1｜问诊";
    }
    
    return PathResult.builder()
        .path(path)
        .pathName("A".equals(path) ? "健康筛查路径" : "症状诊断路径")
        .nextStep(nextStep)
        .build();
}
```

#### 1.1.3 健康状态判定业务逻辑

```java
/**
 * 健康状态判定服务（tool_0）
 */
@Service
public class HealthStateAssessmentService {
    
    /**
     * 健康状态判定
     */
    public HealthStateAssessmentResult assess(UserInput input, BasicInfo basicInfo) {
        // 1. 症状严重程度评估
        SeverityLevel severity = assessSymptomSeverity(input.getSymptoms());
        
        // 2. 风险早筛
        RiskSignals riskSignals = earlyRiskScreening(input, basicInfo);
        
        // 3. 红旗信号识别
        List<RedFlag> redFlags = detectRedFlags(input.getSymptoms());
        
        // 4. 工作态判定
        WorkMode workMode = determineWorkMode(severity, riskSignals, redFlags);
        
        // 5. 创建CDP
        CDP cdp = cdpManager.createCDP(input.getUserId(), input.getSessionId());
        
        // 6. 如果是健康管理态，生成健康管理计划
        WellnessPlan wellnessPlan = null;
        if (workMode == WorkMode.WELLNESS_MODE) {
            wellnessPlan = generateWellnessPlan(riskSignals, cdp);
        }
        
        return HealthStateAssessmentResult.builder()
            .needsClinicalMode(workMode == WorkMode.CLINICAL_MODE)
            .workMode(workMode.name())
            .riskLevel(calculateRiskLevel(severity, riskSignals, redFlags))
            .assessmentReason(generateReason(severity, riskSignals, redFlags))
            .redFlags(redFlags)
            .wellnessPlan(wellnessPlan)
            .cdpId(cdp.getId())
            .build();
    }
    
    /**
     * 症状严重程度评估
     */
    private SeverityLevel assessSymptomSeverity(List<String> symptoms) {
        // 使用症状严重程度判定规则库（0.1）
        // 判断症状是否在正常范围
        // ...
        return SeverityLevel.MODERATE;
    }
    
    /**
     * 早期风险信号识别
     */
    private RiskSignals earlyRiskScreening(UserInput input, BasicInfo basicInfo) {
        // 使用早期风险信号识别库（0.2）
        // 识别早期风险信号（家族史、行为、慢性暴露）
        // ...
        return new RiskSignals();
    }
    
    /**
     * 红旗信号识别
     */
    private List<RedFlag> detectRedFlags(List<String> symptoms) {
        // 使用红旗信号库（0.3）
        // 识别高危症状组合
        // ...
        return new ArrayList<>();
    }
    
    /**
     * 工作态判定
     */
    private WorkMode determineWorkMode(SeverityLevel severity, RiskSignals riskSignals, List<RedFlag> redFlags) {
        // 使用工作态判定规则库（0.4）
        // 出现红旗信号 → 立即进入临床诊疗态
        if (!redFlags.isEmpty()) {
            return WorkMode.CLINICAL_MODE;
        }
        // 症状严重程度高或风险等级≥L2 → 进入临床诊疗态
        if (severity == SeverityLevel.HIGH || riskSignals.getRiskLevel().compareTo(RiskLevel.L2) >= 0) {
            return WorkMode.CLINICAL_MODE;
        }
        // 症状在正常范围且风险等级低（L3/L4） → 进入健康管理态
        if (severity == SeverityLevel.NORMAL && riskSignals.getRiskLevel().compareTo(RiskLevel.L3) <= 0) {
            return WorkMode.WELLNESS_MODE;
        }
        // 不确定时，优先进入临床诊疗态（宁可误报，不能漏报）
        return WorkMode.CLINICAL_MODE;
    }
}
```

#### 1.1.4 健康筛查流程（A路径）业务逻辑

健康筛查流程是健康管理态下的核心业务流程，适用于无症状或症状轻微的个体，通过结构化流程提供健康管理服务。

**流程图概览**：
```
入口判定（P0）→ A1需求分类 → A2收集健康画像 → A3执行分支 → A4生成统一结果 → A5设置随访
```

##### 1.1.4.1 A1｜需求分类（Demand Classification）

**功能**：识别用户进入健康管理态的诉求类型，确定后续分支执行路径。

**业务逻辑**：
```java
/**
 * A1｜需求分类服务
 */
@Service
public class DemandClassificationService {
    
    /**
     * 需求分类
     */
    public DemandClassificationResult classify(String userId, String userInput, CDP cdp) {
        // 1. 意图识别（使用NLU引擎）
        NLUResult nluResult = nluEngine.parse(userInput);
        String intent = nluResult.getIntent();
        
        // 2. 需求分类（根据意图匹配到3个分支之一）
        DemandType demandType = determineDemandType(intent, nluResult);
        // demandType: SCREENING_RECOMMENDATION | HEALTH_GOAL_MANAGEMENT | PLANNED_HEALTH_NEEDS
        
        // 3. 提取关键信息
        Map<String, Object> extractedInfo = extractKeyInformation(nluResult, demandType);
        
        // 4. 更新CDP
        cdpManager.updateCDP(cdp.getId(), Map.of(
            "demandType", demandType.name(),
            "extractedInfo", extractedInfo,
            "stage", "A1_DEMAND_CLASSIFICATION"
        ));
        
        return DemandClassificationResult.builder()
            .demandType(demandType)
            .extractedInfo(extractedInfo)
            .nextStep("A2｜收集健康画像")
            .build();
    }
    
    /**
     * 确定需求类型
     */
    private DemandType determineDemandType(String intent, NLUResult nluResult) {
        // 规则1：明确提及"体检"、"筛查"、"检查" → 筛查建议
        if (hasKeywords(nluResult, Arrays.asList("体检", "筛查", "检查", "体检建议"))) {
            return DemandType.SCREENING_RECOMMENDATION;
        }
        
        // 规则2：明确提及"目标"、"计划"、"管理" → 健康目标管理
        if (hasKeywords(nluResult, Arrays.asList("目标", "计划", "管理", "健康管理"))) {
            return DemandType.HEALTH_GOAL_MANAGEMENT;
        }
        
        // 规则3：明确提及"预约"、"安排"、"准备" → 计划性健康需求
        if (hasKeywords(nluResult, Arrays.asList("预约", "安排", "准备", "计划性"))) {
            return DemandType.PLANNED_HEALTH_NEEDS;
        }
        
        // 默认：筛查建议（最常见）
        return DemandType.SCREENING_RECOMMENDATION;
    }
}
```

##### 1.1.4.2 A2｜收集健康画像（Collect Health Profile）

**功能**：收集用户的完整健康画像，包括基本信息、健康史、家族史、生活方式等。

**业务逻辑**：
```java
/**
 * A2｜收集健康画像服务
 */
@Service
public class HealthProfileCollectionService {
    
    /**
     * 收集健康画像
     */
    public HealthProfile collectHealthProfile(String userId, CDP cdp, Map<String, Object> initialData) {
        // 1. 从CDP获取已有信息
        Map<String, Object> cdpData = cdpManager.getCDPData(cdp.getId());
        
        // 2. 构建健康画像结构
        HealthProfile profile = HealthProfile.builder()
            .userId(userId)
            .basicInfo(extractBasicInfo(cdpData, initialData))
            .healthHistory(extractHealthHistory(cdpData, initialData))
            .familyHistory(extractFamilyHistory(cdpData, initialData))
            .lifestyle(extractLifestyle(cdpData, initialData))
            .medicationHistory(extractMedicationHistory(cdpData, initialData))
            .allergyHistory(extractAllergyHistory(cdpData, initialData))
            .build();
        
        // 3. 识别缺失信息
        InformationGaps gaps = identifyMissingInformation(profile);
        
        // 4. 生成补充提问（仅对必填项）
        List<String> followUpQuestions = generateFollowUpQuestions(gaps.getRequiredGaps());
        
        // 5. 如果还有必填项缺失，返回需要补充的信息
        if (!gaps.getRequiredGaps().isEmpty()) {
            return HealthProfile.builder()
                .profile(profile)
                .completeness(calculateCompleteness(profile))
                .missingRequiredFields(gaps.getRequiredGaps())
                .followUpQuestions(followUpQuestions)
                .isComplete(false)
                .build();
        }
        
        // 6. 画像完整，更新CDP并进入下一步
        cdpManager.updateCDP(cdp.getId(), Map.of(
            "healthProfile", profile,
            "stage", "A2_HEALTH_PROFILE_COLLECTED"
        ));
        
        return HealthProfile.builder()
            .profile(profile)
            .completeness(1.0)
            .isComplete(true)
            .nextStep("A3｜执行分支")
            .build();
    }
    
    /**
     * 提取基本信息
     */
    private BasicInfo extractBasicInfo(Map<String, Object> cdpData, Map<String, Object> initialData) {
        BasicInfo basicInfo = new BasicInfo();
        basicInfo.setAge((Integer) cdpData.getOrDefault("age", initialData.get("age")));
        basicInfo.setGender((String) cdpData.getOrDefault("gender", initialData.get("gender")));
        basicInfo.setHeight((Double) cdpData.getOrDefault("height", initialData.get("height")));
        basicInfo.setWeight((Double) cdpData.getOrDefault("weight", initialData.get("weight")));
        basicInfo.setBmi(calculateBMI(basicInfo.getHeight(), basicInfo.getWeight()));
        return basicInfo;
    }
    
    /**
     * 识别缺失信息
     */
    private InformationGaps identifyMissingInformation(HealthProfile profile) {
        InformationGaps gaps = new InformationGaps();
        
        // 必填项：年龄、性别（用于风险计算）
        if (profile.getBasicInfo().getAge() == null) {
            gaps.addRequiredGap("basicInfo.age");
        }
        if (profile.getBasicInfo().getGender() == null) {
            gaps.addRequiredGap("basicInfo.gender");
        }
        
        // 重要项：健康史、家族史（用于个性化筛查建议）
        // ...
        
        return gaps;
    }
}
```

##### 1.1.4.3 A3｜执行分支（Execute Branch）

**功能**：根据A1的需求分类结果，执行对应的分支逻辑。

**业务逻辑**：
```java
/**
 * A3｜执行分支服务
 */
@Service
public class BranchExecutionService {
    
    /**
     * 执行分支
     */
    public BranchExecutionResult executeBranch(String userId, CDP cdp, DemandType demandType, HealthProfile profile) {
        BranchExecutionResult result;
        
        switch (demandType) {
            case SCREENING_RECOMMENDATION:
                result = executeScreeningRecommendation(userId, profile, cdp);
                break;
            case HEALTH_GOAL_MANAGEMENT:
                result = executeHealthGoalManagement(userId, profile, cdp);
                break;
            case PLANNED_HEALTH_NEEDS:
                result = executePlannedHealthNeeds(userId, profile, cdp);
                break;
            default:
                throw new IllegalArgumentException("Unknown demand type: " + demandType);
        }
        
        // 更新CDP
        cdpManager.updateCDP(cdp.getId(), Map.of(
            "branchResult", result,
            "stage", "A3_BRANCH_EXECUTED"
        ));
        
        return result;
    }
    
    /**
     * 分支1：筛查建议
     */
    private BranchExecutionResult executeScreeningRecommendation(String userId, HealthProfile profile, CDP cdp) {
        // 1. 基于健康画像计算风险等级
        RiskLevel riskLevel = riskCalculator.calculateRiskLevel(profile);
        
        // 2. 基于风险等级生成筛查建议（使用筛查建议规则库）
        List<ScreeningRecommendation> recommendations = screeningEngine.generateRecommendations(profile, riskLevel);
        
        // 3. 优先级排序
        recommendations.sort((a, b) -> Double.compare(b.getPriority(), a.getPriority()));
        
        return BranchExecutionResult.builder()
            .demandType(DemandType.SCREENING_RECOMMENDATION)
            .riskLevel(riskLevel)
            .recommendations(recommendations)
            .build();
    }
    
    /**
     * 分支2：健康目标管理
     */
    private BranchExecutionResult executeHealthGoalManagement(String userId, HealthProfile profile, CDP cdp) {
        // 1. 获取用户当前健康目标
        List<HealthGoal> currentGoals = goalManager.getCurrentGoals(userId);
        
        // 2. 目标达成度评估
        List<GoalProgress> progressList = goalTracker.evaluateProgress(currentGoals, profile);
        
        // 3. 生成目标调整建议
        List<GoalAdjustment> adjustments = goalAdvisor.suggestAdjustments(currentGoals, progressList);
        
        return BranchExecutionResult.builder()
            .demandType(DemandType.HEALTH_GOAL_MANAGEMENT)
            .currentGoals(currentGoals)
            .progressList(progressList)
            .adjustments(adjustments)
            .build();
    }
    
    /**
     * 分支4：计划性健康需求
     */
    private BranchExecutionResult executePlannedHealthNeeds(String userId, HealthProfile profile, CDP cdp) {
        // 1. 识别计划性健康需求（如手术前准备、疫苗接种等）
        List<PlannedHealthNeed> needs = needIdentifier.identifyNeeds(profile);
        
        // 2. 生成准备建议
        List<PreparationAdvice> adviceList = preparationAdvisor.generateAdvice(needs, profile);
        
        return BranchExecutionResult.builder()
            .demandType(DemandType.PLANNED_HEALTH_NEEDS)
            .needs(needs)
            .adviceList(adviceList)
            .build();
    }
}
```

##### 1.1.4.4 A4｜生成统一结果（Generate Unified Result）

**功能**：将各分支的执行结果统一封装为标准化输出。

**业务逻辑**：
```java
/**
 * A4｜生成统一结果服务
 */
@Service
public class UnifiedResultGeneratorService {
    
    /**
     * 生成统一结果
     */
    public UnifiedResult generateUnifiedResult(CDP cdp, BranchExecutionResult branchResult) {
        // 1. 提取分支结果
        DemandType demandType = branchResult.getDemandType();
        
        // 2. 构建统一结果结构
        UnifiedResult result = UnifiedResult.builder()
            .cdpId(cdp.getId())
            .demandType(demandType)
            .summary(generateSummary(branchResult))
            .mainContent(buildMainContent(branchResult))
            .recommendations(buildRecommendations(branchResult))
            .nextSteps(buildNextSteps(branchResult))
            .build();
        
        // 3. 更新CDP
        cdpManager.updateCDP(cdp.getId(), Map.of(
            "unifiedResult", result,
            "stage", "A4_UNIFIED_RESULT_GENERATED"
        ));
        
        return result;
    }
    
    /**
     * 生成摘要
     */
    private String generateSummary(BranchExecutionResult branchResult) {
        switch (branchResult.getDemandType()) {
            case SCREENING_RECOMMENDATION:
                return String.format("基于您的健康画像，我们为您推荐了%d项筛查建议。",
                    branchResult.getRecommendations().size());
            case HEALTH_GOAL_MANAGEMENT:
                return String.format("您当前有%d个健康目标，其中%d个进展良好。",
                    branchResult.getCurrentGoals().size(),
                    branchResult.getProgressList().stream()
                        .filter(p -> p.getStatus() == ProgressStatus.GOOD)
                        .count());
            case PLANNED_HEALTH_NEEDS:
                return String.format("已为您识别%d项计划性健康需求，并提供了相应的准备建议。",
                    branchResult.getNeeds().size());
            default:
                return "健康管理服务已完成。";
        }
    }
}
```

##### 1.1.4.5 A5｜设置随访（Setup Follow-up）

**功能**：根据健康画像和分支结果，设置个性化随访计划。

**业务逻辑**：
```java
/**
 * A5｜设置随访服务
 */
@Service
public class FollowUpSetupService {
    
    /**
     * 设置随访
     */
    public FollowUpPlan setupFollowUp(String userId, CDP cdp, UnifiedResult unifiedResult) {
        // 1. 计算随访周期（基于风险等级、需求类型等）
        int followUpDays = calculateFollowUpDays(unifiedResult);
        
        // 2. 生成随访提醒内容
        String reminderContent = generateReminderContent(unifiedResult);
        
        // 3. 创建随访计划
        FollowUpPlan plan = FollowUpPlan.builder()
            .userId(userId)
            .cdpId(cdp.getId())
            .followUpDate(LocalDate.now().plusDays(followUpDays))
            .reminderContent(reminderContent)
            .isActive(true)
            .build();
        
        // 4. 保存随访计划
        followUpManager.saveFollowUpPlan(plan);
        
        // 5. 更新CDP
        cdpManager.updateCDP(cdp.getId(), Map.of(
            "followUpPlan", plan,
            "stage", "A5_FOLLOW_UP_SETUP"
        ));
        
        return plan;
    }
    
    /**
     * 计算随访周期
     */
    private int calculateFollowUpDays(UnifiedResult unifiedResult) {
        // 高风险 → 30天
        // 中风险 → 90天
        // 低风险 → 180天
        // ...
        return 90;
    }
}
```

---

### 1.2 工具1：病例理解工具（tool_1）

> **对应服务**：clinical-parsing-service  
> **工具类型**：Deterministic  
> **参考文档**：《AI医生系统-技术架构设计-智能体详细设计.md》4.1节

#### 1.2.1 业务目标

**核心目标**：将非结构化的患者信息转换为结构化的临床要素，包括医学概念识别、概念归一化、结构化提取。

#### 1.2.2 业务逻辑设计

```java
/**
 * 病例理解工具（tool_1）
 */
@Service
public class ClinicalParsingService {
    
    /**
     * 病例理解与结构化
     */
    public ClinicalParsingResult parse(UserInput input, CDP cdp) {
        // 1. 医学概念识别
        List<MedicalConcept> concepts = extractConcepts(input.getText());
        
        // 2. 概念归一化（使用归一化词表：1.1-1.6）
        List<NormalizedConcept> normalizedConcepts = normalizeConcepts(concepts);
        
        // 3. 多模态理解（使用多模态理解规则库：1.7）
        StructuredData structuredData = extractStructuredData(input);
        
        // 4. 歧义表达判定与追问（使用歧义表达判定规则库：1.8）
        List<AmbiguousExpression> ambiguousExpressions = detectAmbiguousExpressions(input);
        
        // 5. 更新CDP
        cdp.getPatientState().put("concepts", normalizedConcepts);
        cdp.getPatientState().put("structuredData", structuredData);
        cdpManager.updateCDP(cdp.getId(), cdp);
        
        return ClinicalParsingResult.builder()
            .concepts(normalizedConcepts)
            .structuredData(structuredData)
            .ambiguousExpressions(ambiguousExpressions)
            .build();
    }
    
    /**
     * 医学概念识别
     */
    private List<MedicalConcept> extractConcepts(String text) {
        // 使用QuickUMLS或中文医学NER模型
        // 从文本中提取症状、疾病、药物等医学概念
        // ...
        return new ArrayList<>();
    }
    
    /**
     * 概念归一化
     */
    private List<NormalizedConcept> normalizeConcepts(List<MedicalConcept> concepts) {
        // 使用归一化词表（1.1-1.6）：
        // 1.1 症状归一化词表
        // 1.2 疾病名称归一化词表
        // 1.3 药物名称归一化词表
        // 1.4 过敏源归一化词表
        // 1.5 检查项目归一化词表
        // 1.6 指标名称归一化词表
        // 映射到CUI/ICD/SNOMED编码
        // ...
        return new ArrayList<>();
    }
}
```

---

### 1.3 工具2：主动问诊工具（tool_2）

> **对应服务**：dialog-service  
> **工具类型**：Generative（但问诊策略基于临床决策分析，属于Deterministic）  
> **参考文档**：《AI医生系统-技术架构设计-智能体详细设计.md》4.2节

#### 1.3.1 业务目标

**核心目标**：像医生一样问"关键问题"，补齐鉴别诊断所需证据，基于信息增益决定问什么问题。

#### 1.1.1 业务目标

**核心目标**：把用户的自然语言描述，转化成可推理、可复用、可审计的结构化"问题清单"，作为后续诊断分析的唯一输入。

#### 1.1.2 概念归一化业务逻辑

```java
/**
 * 概念归一化服务
 */
@Service
public class ConceptNormalizationService {
    
    /**
     * 将用户原话转换为标准医学概念
     */
    public NormalizedConcept normalizeConcept(String userText, String symptomType) {
        NormalizedConcept normalized = new NormalizedConcept();
        normalized.setOriginalText(userText);
        
        // 症状概念映射表
        Map<String, String> symptomMapping = new HashMap<>();
        symptomMapping.put("胸口闷", "胸闷样不适");
        symptomMapping.put("走几步就喘", "活动后气促");
        symptomMapping.put("心慌", "心悸");
        symptomMapping.put("肚子疼", "腹痛");
        // ... 更多映射
        
        // 查找映射
        String normalizedSymptom = symptomMapping.entrySet().stream()
            .filter(entry -> userText.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(userText); // 如果没有映射，使用原文本
        
        normalized.setNormalizedSymptom(normalizedSymptom);
        
        // 识别同义表达
        if (isSynonym(normalizedSymptom, userText)) {
            normalized.setConfidence(0.9);
        } else {
            normalized.setConfidence(0.7);
        }
        
        return normalized;
    }
    
    /**
     * 判断是否为同义表达
     */
    private boolean isSynonym(String standard, String userText) {
        // 使用同义词典或NLP模型判断
        // 这里简化处理
        return true;
    }
}
```

#### 1.1.3 结构化问题清单构建

```java
/**
 * 结构化问题清单构建器
 */
@Service
public class StructuredQuestionListBuilder {
    
    /**
     * 构建结构化问题清单
     */
    public StructuredQuestionList buildQuestionList(
            DiagnosisRecord record, 
            HealthProfile profile,
            List<ExaminationResult> examinationResults) {
        
        StructuredQuestionList questionList = new StructuredQuestionList();
        
        // 1. 主要问题（用户最关注的主诉）
        questionList.setChiefComplaint(buildChiefComplaint(record));
        
        // 2. 伴随问题（同时间窗出现的关键症状/体征）
        questionList.setAccompanyingSymptoms(buildAccompanyingSymptoms(record));
        
        // 3. 关键背景（诊断错误常源于信息采集偏差，必须纳入）
        questionList.setKeyBackground(buildKeyBackground(profile));
        
        // 4. 生命体征（主动询问或设备采集）
        questionList.setVitalSigns(buildVitalSigns(record));
        
        // 5. 检查结果（用户上传）
        questionList.setExaminationResults(examinationResults);
        
        // 6. 历史诊断（从诊断记录获取）
        questionList.setHistoricalDiagnoses(buildHistoricalDiagnoses(record.getUserId()));
        
        // 7. 标记信息缺口
        InformationGaps gaps = informationGapIdentifier.identifyGaps(questionList);
        questionList.setInformationGaps(gaps);
        
        // 8. 计算信息完整度
        double completeness = completenessCalculator.calculateCompleteness(questionList);
        questionList.setCompleteness(completeness);
        
        return questionList;
    }
    
    /**
     * 构建主要问题
     */
    private ChiefComplaint buildChiefComplaint(DiagnosisRecord record) {
        ChiefComplaint chiefComplaint = new ChiefComplaint();
        chiefComplaint.setName(record.getChiefComplaint());
        chiefComplaint.setOriginalText(record.getOriginalSymptomDescription());
        chiefComplaint.setDuration(record.getSymptomDuration());
        chiefComplaint.setOnsetMode(determineOnsetMode(record.getSymptomDuration()));
        chiefComplaint.setSeverity(record.getSymptomSeverity());
        chiefComplaint.setFrequency(determineFrequency(record.getSymptomFeatures()));
        chiefComplaint.setLocation(extractLocation(record.getSymptomFeatures()));
        chiefComplaint.setFeatures(buildSymptomFeatures(record));
        chiefComplaint.setTriageLevel(record.getTriageLevel());
        return chiefComplaint;
    }
    
    /**
     * 构建症状特征
     */
    private Map<String, Object> buildSymptomFeatures(DiagnosisRecord record) {
        Map<String, Object> features = new HashMap<>();
        if (record.getSymptomFeatures() != null) {
            features.putAll(record.getSymptomFeatures());
        }
        // 提取诱因、缓解因素等
        features.put("trigger", extractTrigger(record.getSymptomDescription()));
        features.put("relief_factor", extractReliefFactor(record.getSymptomDescription()));
        return features;
    }
}
```

### 1.2 阶段2：信息补全（智能追问，按信息缺口分级处理）

#### 1.2.1 信息缺口识别与分级业务逻辑

```java
/**
 * 信息缺口识别与分级服务
 */
@Service
public class InformationGapIdentifier {
    
    /**
     * 识别信息缺口并按等级分类
     */
    public InformationGaps identifyGaps(StructuredQuestionList questionList) {
        InformationGaps gaps = new InformationGaps();
        
        // 1. 识别必填缺口（缺失则不能进入阶段3）
        List<String> requiredGaps = identifyRequiredGaps(questionList);
        gaps.setRequiredGaps(requiredGaps);
        
        // 2. 识别重要缺口（可进入但必须提示不确定性与风险）
        List<String> importantGaps = identifyImportantGaps(questionList);
        gaps.setImportantGaps(importantGaps);
        
        // 3. 识别可选缺口（后续补充即可）
        List<String> optionalGaps = identifyOptionalGaps(questionList);
        gaps.setOptionalGaps(optionalGaps);
        
        return gaps;
    }
    
    /**
     * 识别必填缺口
     * 规则：缺失则不能进入阶段3（会导致必须排除项无法判断）
     */
    private List<String> identifyRequiredGaps(StructuredQuestionList questionList) {
        List<String> requiredGaps = new ArrayList<>();
        
        ChiefComplaint chiefComplaint = questionList.getChiefComplaint();
        if (chiefComplaint == null || chiefComplaint.getName() == null) {
            requiredGaps.add("chief_complaint");
        }
        if (chiefComplaint == null || chiefComplaint.getDuration() == null) {
            requiredGaps.add("duration");
        }
        if (chiefComplaint == null || chiefComplaint.getSeverity() == null) {
            requiredGaps.add("severity");
        }
        
        // 高危情况下，生命体征是必填的
        if (isHighRiskSituation(questionList)) {
            VitalSigns vitalSigns = questionList.getVitalSigns();
            if (vitalSigns == null || vitalSigns.getBloodPressure() == null) {
                requiredGaps.add("vital_signs");
            }
        }
        
        return requiredGaps;
    }
    
    /**
     * 判断是否为高危情况
     */
    private boolean isHighRiskSituation(StructuredQuestionList questionList) {
        // 如果有胸痛、呼吸困难等严重症状
        ChiefComplaint chiefComplaint = questionList.getChiefComplaint();
        if (chiefComplaint != null) {
            String name = chiefComplaint.getName();
            List<String> highRiskSymptoms = Arrays.asList("胸痛", "呼吸困难", "意识不清", "剧烈头痛");
            return highRiskSymptoms.stream().anyMatch(name::contains);
        }
        return false;
    }
    
    /**
     * 识别重要缺口
     * 规则：可进入但必须提示不确定性与风险
     */
    private List<String> identifyImportantGaps(StructuredQuestionList questionList) {
        List<String> importantGaps = new ArrayList<>();
        
        if (questionList.getAccompanyingSymptoms() == null || 
            questionList.getAccompanyingSymptoms().isEmpty()) {
            importantGaps.add("accompanying_symptoms");
        }
        
        KeyBackground background = questionList.getKeyBackground();
        if (background == null || background.getMedicalHistory() == null) {
            importantGaps.add("medical_history");
        }
        if (background == null || background.getFamilyHistory() == null) {
            importantGaps.add("family_history");
        }
        if (background == null || background.getMedicationHistory() == null) {
            importantGaps.add("medication_history");
        }
        
        return importantGaps;
    }
    
    /**
     * 识别可选缺口
     * 规则：后续补充即可
     */
    private List<String> identifyOptionalGaps(StructuredQuestionList questionList) {
        List<String> optionalGaps = new ArrayList<>();
        
        KeyBackground background = questionList.getKeyBackground();
        if (background == null || background.getLifestyle() == null) {
            optionalGaps.add("lifestyle");
        }
        if (background == null || background.getRecentEvents() == null) {
            optionalGaps.add("recent_events");
        }
        
        return optionalGaps;
    }
}
```

### 1.3 信息完整度计算（更新版）

#### 1.3.1 信息完整度计算业务逻辑（基于结构化问题清单）

```java
/**
 * 信息完整度计算服务（基于结构化问题清单）
 */
@Service
public class CompletenessCalculator {
    
    /**
     * 计算信息完整度
     * 完整度 = (已有信息项数 / 总信息项数) × 100%
     * 
     * 总信息项包括：
     * - 主要问题（主诉、起病时间、严重度、症状特征）：4项
     * - 伴随问题（关键伴随症状）：1项
     * - 关键背景（年龄、性别、既往史、用药史、家族史）：5项
     * - 生命体征（血压、心率、体温）：3项
     * - 检查结果（如果有）：1项
     * 总计：14项
     */
    public double calculateCompleteness(StructuredQuestionList questionList) {
        int totalItems = getTotalItemCount(); // 14项
        int existingItems = countExistingItems(questionList);
        
        double completeness = (double) existingItems / totalItems;
        
        // 检查必填缺口：如果有必填缺口，完整度不能超过60%
        InformationGaps gaps = questionList.getInformationGaps();
        if (gaps != null && !gaps.getRequiredGaps().isEmpty()) {
            completeness = Math.min(completeness, 0.6);
        }
        
        return completeness;
    }
    
    /**
     * 获取总信息项数
     */
    private int getTotalItemCount() {
        // 总信息项包括：
        // - 主要问题（主诉、起病时间、严重度、症状特征）：4项
        // - 伴随问题（关键伴随症状）：1项
        // - 关键背景（年龄、性别、既往史、用药史、家族史）：5项
        // - 生命体征（血压、心率、体温）：3项
        // - 检查结果（如果有）：1项
        return 14;
    }
    
    /**
     * 计算已有信息项数
     */
    private int countExistingItems(StructuredQuestionList questionList) {
        int count = 0;
        
        // 主要问题计数（4项）
        ChiefComplaint chiefComplaint = questionList.getChiefComplaint();
        if (chiefComplaint != null) {
            if (chiefComplaint.getName() != null) count++;
            if (chiefComplaint.getDuration() != null) count++;
            if (chiefComplaint.getSeverity() != null) count++;
            if (chiefComplaint.getFeatures() != null && !chiefComplaint.getFeatures().isEmpty()) count++;
        }
        
        // 伴随问题计数（1项）
        if (questionList.getAccompanyingSymptoms() != null && 
            !questionList.getAccompanyingSymptoms().isEmpty()) {
            count++;
        }
        
        // 关键背景计数（5项）
        KeyBackground background = questionList.getKeyBackground();
        if (background != null) {
            if (background.getAge() != null) count++;
            if (background.getGender() != null) count++;
            if (background.getMedicalHistory() != null && !background.getMedicalHistory().isEmpty()) count++;
            if (background.getMedicationHistory() != null && !background.getMedicationHistory().isEmpty()) count++;
            if (background.getFamilyHistory() != null && !background.getFamilyHistory().isEmpty()) count++;
        }
        
        // 生命体征计数（3项）
        VitalSigns vitalSigns = questionList.getVitalSigns();
        if (vitalSigns != null) {
            if (vitalSigns.getBloodPressure() != null) count++;
            if (vitalSigns.getHeartRate() != null) count++;
            if (vitalSigns.getTemperature() != null) count++;
        }
        
        // 检查结果计数（1项）
        if (questionList.getExaminationResults() != null && 
            !questionList.getExaminationResults().isEmpty()) {
            count++;
        }
        
        return count;
    }
}
```

#### 1.2.2 权重调整

不同信息项的重要性不同，可以使用加权计算：

```java
/**
 * 加权计算信息完整度
 */
public double calculateWeightedCompleteness(DiagnosisRecord record, HealthProfile profile) {
    Map<String, Double> weights = new HashMap<>();
    weights.put("chief_complaint", 0.15);      // 主诉最重要
    weights.put("symptom_duration", 0.12);
    weights.put("symptom_severity", 0.10);
    weights.put("accompanying_symptoms", 0.10);
    weights.put("age", 0.10);
    weights.put("gender", 0.08);
    weights.put("medical_history", 0.12);
    weights.put("medication_history", 0.08);
    weights.put("bp", 0.05);
    weights.put("heart_rate", 0.05);
    weights.put("temperature", 0.03);
    weights.put("examination_results", 0.02);
    
    double completeness = 0.0;
    
    // 计算各项权重
    if (record.getChiefComplaint() != null) {
        completeness += weights.get("chief_complaint");
    }
    // ... 其他项类似
    
    return completeness;
}
```

---

### 1.4 工具3：鉴别诊断工具（tool_3）

> **对应服务**：diagnosis-engine-service  
> **工具类型**：Retrieval（知识库优先）+ Generative（路径约束推理）  
> **参考文档**：《AI医生系统-技术架构设计-智能体详细设计.md》4.3节、《DR.KNOWS与5步诊断流程整合方案.md》

#### 1.4.1 业务目标

**核心目标**：生成Top-K鉴别诊断列表，使用DR.KNOWS方法进行知识图谱路径检索和路径注入LLM，实现多引擎融合诊断。

#### 1.4.2 DR.KNOWS核心方法实现

```java
/**
 * 知识图谱推理引擎（DR.KNOWS核心方法）
 */
@Service
public class KGReasoningEngine {
    
    /**
     * 多跳推理路径检索（DR.KNOWS核心方法）
     */
    public List<ReasoningPath> retrievePaths(List<String> symptomCuis, int maxHops) {
        // 使用Neo4j进行图遍历
        String cypherQuery = """
            MATCH path = (s:Symptom)-[*2..4]->(d:Disease)
            WHERE s.cui IN $symptom_cuis
            RETURN path, 
                   relationships(path) as rels,
                   nodes(path) as nodes,
                   length(path) as path_length
            ORDER BY path_length
            LIMIT 50
            """;
        
        // 执行Cypher查询
        List<ReasoningPath> paths = neo4jClient.query(cypherQuery, 
            Map.of("symptom_cuis", symptomCuis));
        
        return paths;
    }
    
    /**
     * 路径评分排序（DR.KNOWS三层评分体系）
     */
    public List<RankedPath> scorePaths(List<ReasoningPath> paths, String patientContext) {
        List<RankedPath> rankedPaths = new ArrayList<>();
        
        for (ReasoningPath path : paths) {
            // 层1：路径相关性评分（SGIN + 注意力机制）
            double relevanceScore = pathRelevanceScorer.score(path, patientContext);
            
            // 层2：证据强度评分
            double evidenceStrength = evidenceStrengthScorer.score(path.getEvidence());
            
            // 层3：信息增益评分（如果用于问诊）
            double informationGain = informationGainScorer.calculate(path, currentDDx);
            
            // 综合评分（加权组合）
            double compositeScore = 0.5 * relevanceScore 
                                  + 0.3 * evidenceStrength 
                                  + 0.2 * informationGain;
            
            rankedPaths.add(RankedPath.builder()
                .path(path)
                .relevanceScore(relevanceScore)
                .evidenceStrength(evidenceStrength)
                .compositeScore(compositeScore)
                .build());
        }
        
        // 排序并返回Top-10
        return rankedPaths.stream()
            .sorted(Comparator.comparing(RankedPath::getCompositeScore).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }
    
    /**
     * 路径注入LLM（DR.KNOWS核心方法）
     */
    public String buildEnhancedPrompt(String patientInfo, List<RankedPath> rankedPaths) {
        // 格式化路径
        String pathsText = formatPathsForLLM(rankedPaths);
        
        // 构建包含推理路径的Prompt
        String prompt = String.format("""
            你是一位经验丰富的医生，需要根据患者的症状、体征、健康档案等信息，
            结合以下医学推理路径，分析可能的疾病方向。
            
            患者信息：
            %s
            
            医学推理路径（来自知识图谱，按相关性排序）：
            %s
            
            请根据以上医学推理路径，分析：
            1. 可能的疾病方向（Top 3-5），按可能性排序
            2. 每个方向的支持证据（症状、体征、检查结果）
            3. 每个方向的反对证据
            4. 还需要哪些信息来进一步判断
            5. 建议做哪些检查来辅助诊断
            
            注意：
            - 优先考虑推理路径中提到的疾病方向
            - 不要给出确诊结论，使用"可能"、"考虑"等表述
            - 如果信息不足，明确说明
            - 如果推理路径与患者情况不符，请说明原因
            """, patientInfo, pathsText);
        
        return prompt;
    }
}
```

#### 1.4.3 多引擎融合诊断

```java
/**
 * 鉴别诊断工具（tool_3）- 多引擎融合诊断系统
 */
@Service
public class MultiEngineFusionService {
    
    /**
     * 多引擎融合诊断
     */
    public DifferentialDiagnosisResult diagnose(CDP cdp) {
        // 1. 规则引擎
        Map<String, Double> ruleResult = ruleEngine.match(cdp.getPatientState());
        
        // 2. 知识图谱引擎（DR.KNOWS核心）
        Map<String, Double> kgResult = kgReasoningEngine.diagnose(cdp);
        
        // 3. 统计模型引擎
        Map<String, Double> statisticalResult = statisticalEngine.predict(cdp.getPatientState());
        
        // 4. 大模型引擎（路径注入）
        Map<String, Double> llmResult = llmEngine.diagnoseWithPaths(cdp);
        
        // 5. 鉴别诊断引擎
        Map<String, Double> differentialResult = differentialEngine.compare(cdp);
        
        // 6. 加权融合
        Map<String, Double> fusedResult = fuseEngineResults(
            Map.of(
                "rule", ruleResult,
                "kg", kgResult,
                "statistical", statisticalResult,
                "llm", llmResult,
                "differential", differentialResult
            ),
            cdp
        );
        
        // 7. 三层排序（首要假设/主要备选/必须排除）
        ThreeLayerResult threeLayerResult = organizeThreeLayer(fusedResult, cdp);
        
        // 8. 更新CDP
        cdp.setDdx(threeLayerResult);
        cdpManager.updateCDP(cdp.getId(), cdp);
        
        return DifferentialDiagnosisResult.builder()
            .ddx(threeLayerResult)
            .reasoningPaths(kgResult.getReasoningPaths()) // DR.KNOWS路径
            .engineResults(Map.of(
                "ruleEngine", ruleResult,
                "kgEngine", kgResult,
                "statisticalEngine", statisticalResult,
                "llmEngine", llmResult,
                "differentialEngine", differentialResult
            ))
            .build();
    }
}
```

---

### 1.5 工具4：检查建议工具（tool_4）

> **对应服务**：workup-planner-service  
> **工具类型**：Deterministic  
> **参考文档**：《AI医生系统-技术架构设计-智能体详细设计.md》4.4节

#### 1.5.1 业务目标

**核心目标**：基于当前DDx和已有证据，建议下一步检查，并评估检查的价值，制定验证计划。

#### 1.5.2 业务逻辑设计

```java
/**
 * 检查建议工具（tool_4）
 */
@Service
public class WorkupPlannerService {
    
    /**
     * 生成检查建议
     */
    public WorkupPlanResult planWorkup(CDP cdp) {
        // 1. 检查价值评估（使用检查价值评估规则库：4.1）
        List<WorkupItem> workupItems = evaluateWorkupValue(cdp);
        
        // 2. 生成验证计划（使用验证计划库：4.3）
        VerificationPlan verificationPlan = generateVerificationPlan(cdp);
        
        // 3. 更新CDP
        cdp.setWorkupPlan(Map.of(
            "workupItems", workupItems,
            "verificationPlan", verificationPlan
        ));
        cdpManager.updateCDP(cdp.getId(), cdp);
        
        return WorkupPlanResult.builder()
            .workupPlan(workupItems)
            .verificationPlan(verificationPlan)
            .build();
    }
}
```

---

### 1.6 工具5：治疗建议工具（tool_5）

> **对应服务**：treatment-engine-service  
> **工具类型**：Generative（但治疗方案推理基于临床指南，属于Deterministic）  
> **参考文档**：《AI医生系统-技术架构设计-智能体详细设计.md》4.5节

#### 1.6.1 业务目标

**核心目标**：基于诊断结果，生成治疗方案和处置建议，包括对症处理、用药建议、非药物治疗建议等。

#### 1.6.2 业务逻辑设计

```java
/**
 * 治疗建议工具（tool_5）
 */
@Service
public class TreatmentEngineService {
    
    /**
     * 生成治疗建议
     */
    public TreatmentPlanResult planTreatment(CDP cdp) {
        // 1. 治疗方案推理（使用治疗方案推理规则库：5.1）
        TreatmentPlan treatmentPlan = inferTreatmentPlan(cdp.getDdx(), cdp);
        
        // 2. 更新CDP
        cdp.setManagementPlan(treatmentPlan);
        cdpManager.updateCDP(cdp.getId(), cdp);
        
        return TreatmentPlanResult.builder()
            .treatmentPlan(treatmentPlan)
            .build();
    }
}
```

---

### 1.7 工具6：风险评估工具（tool_6）

> **对应服务**：risk-assessment-service  
> **工具类型**：Deterministic  
> **参考文档**：《AI医生系统-技术架构设计-智能体详细设计.md》4.6节

#### 1.7.1 业务目标

**核心目标**：识别高危情况，评估紧急程度，决定是否需要立即升级处理。

#### 1.7.2 业务逻辑设计

```java
/**
 * 风险评估工具（tool_6）
 */
@Service
public class RiskAssessmentService {
    
    /**
     * 风险评估
     */
    public RiskAssessmentResult assessRisk(CDP cdp) {
        // 1. 高危识别（使用高危识别规则库：6.1）
        List<RedFlag> redFlags = identifyHighRisk(cdp);
        
        // 2. 严重程度评估（使用严重程度评估规则库：6.2）
        String severity = assessSeverity(cdp.getDdx());
        
        // 3. 紧急程度分级（使用紧急程度分级规则库：6.3）
        String urgency = determineUrgency(cdp);
        
        // 4. 复评与升级规则（使用复评与升级规则库：6.4）
        ReviewPlan reviewPlan = generateReviewPlan(cdp.getDdx());
        
        // 5. 更新CDP
        cdp.setTriage(Map.of(
            "riskLevel", calculateRiskLevel(redFlags, severity),
            "severity", severity,
            "urgency", urgency,
            "redFlags", redFlags,
            "reviewPlan", reviewPlan
        ));
        cdpManager.updateCDP(cdp.getId(), cdp);
        
        return RiskAssessmentResult.builder()
            .riskLevel(calculateRiskLevel(redFlags, severity))
            .severity(severity)
            .urgency(urgency)
            .redFlags(redFlags)
            .reviewPlan(reviewPlan)
            .build();
    }
}
```

---

### 1.8 工具7：证据链工具（tool_7）

> **对应服务**：explanation-service  
> **工具类型**：Generative  
> **参考文档**：《AI医生系统-技术架构设计-智能体详细设计.md》4.7节

#### 1.8.1 业务目标

**核心目标**：生成完整的证据链，让系统的"结论"能被复核，包括证据清单结构化、终点结论包生成等。

#### 1.8.2 业务逻辑设计

```java
/**
 * 证据链工具（tool_7）
 */
@Service
public class ExplanationService {
    
    /**
     * 生成可解释性结果
     */
    public EvidenceChainResult explain(CDP cdp) {
        // 1. 证据链构建（使用证据链构建规则库：7.1）
        List<Evidence> evidenceChain = buildEvidenceChain(cdp);
        
        // 2. 推理路径可视化（使用推理路径可视化规则库：7.2）
        List<String> reasoningPaths = visualizeReasoningPaths(cdp);
        
        // 3. 终点结论包生成（使用终点结论包模板库：7.5）
        ConclusionPackage conclusionPackage = generateConclusionPackage(cdp);
        
        // 4. 更新CDP
        cdp.setEvidenceGraph(Map.of(
            "evidenceChain", evidenceChain,
            "reasoningPaths", reasoningPaths
        ));
        cdpManager.updateCDP(cdp.getId(), cdp);
        
        return EvidenceChainResult.builder()
            .evidenceChain(evidenceChain)
            .reasoningPaths(reasoningPaths)
            .conclusionPackage(conclusionPackage)
            .build();
    }
}
```

---

### 1.9 CDP管理业务逻辑

#### 1.9.1 CDP创建

```java
/**
 * CDP管理服务
 */
@Service
public class CDPManager {
    
    /**
     * 创建CDP
     */
    public CDP createCDP(String patientId, String sessionId) {
        CDP cdp = CDP.builder()
            .id(generateCDPId())
            .patientId(patientId)
            .sessionId(sessionId)
            .version(1)
            .patientState(new HashMap<>())
            .ddx(new ArrayList<>())
            .evidenceGraph(new HashMap<>())
            .workupPlan(new HashMap<>())
            .managementPlan(new HashMap<>())
            .triage(new HashMap<>())
            .uncertainty(new HashMap<>())
            .audit(new HashMap<>())
            .build();
        
        // 保存CDP
        cdpRepository.save(cdp);
        
        // 创建版本记录
        createVersionRecord(cdp, "CREATE");
        
        return cdp;
    }
    
    /**
     * 更新CDP（自动创建新版本）
     */
    public CDP updateCDP(String cdpId, Map<String, Object> updates) {
        CDP cdp = getCDP(cdpId);
        
        // 创建新版本
        CDP newVersion = cdp.createNewVersion();
        
        // 应用更新
        applyUpdates(newVersion, updates);
        
        // 保存新版本
        cdpRepository.save(newVersion);
        
        // 创建版本记录
        createVersionRecord(newVersion, "UPDATE");
        
        return newVersion;
    }
}
```

---

### 1.10 主Agent运行循环与工具调用流程

> **参考文档**：《AI医生系统-技术架构设计-核心架构.md》第四章 主Agent运行循环与默认诊断路径

**主Agent运行循环**是系统的核心，主Agent通过运行循环自主调用工具，完成诊断流程。

```java
/**
 * 主Agent运行循环服务
 */
@Service
public class ClinicalAgentBrain {
    
    /**
     * 主Agent运行循环
     */
    public DiagnosisResult runAgentLoop(UserInput input) {
        String sessionId = generateSessionId();
        
        // 1. 创建CDP，初始化AgentState
        CDP cdp = cdpManager.createCDP(input.getPatientId(), sessionId);
        AgentState agentState = agentStateManager.createAgentState(sessionId, cdp.getId());
        
        // 2. 开始运行循环
        while (true) {
            // Observe（观察）
            CDPState cdpState = observe(cdp, agentState);
            
            // Plan（规划）
            ToolCallPlan plan = plan(cdpState, agentState);
            
            // Act（执行）
            ToolResult result = act(plan, cdp, agentState);
            
            // Update（更新）
            update(cdp, agentState, result);
            
            // Evaluate（评估）
            Decision decision = evaluate(cdp, agentState);
            
            // Stop/Escalate/Continue
            if (decision.getType() == DecisionType.STOP) {
                return generateFinalConclusion(cdp, agentState);
            } else if (decision.getType() == DecisionType.ESCALATE) {
                return escalate(cdp, agentState, decision);
            } else if (decision.getType() == DecisionType.REFUSE) {
                return refuse(cdp, agentState, decision);
            }
            // Continue：继续循环
        }
    }
    
    /**
     * Observe（观察）：读取CDP状态、识别信息缺口
     */
    private CDPState observe(CDP cdp, AgentState agentState) {
        // 读取当前CDP状态
        CDPState state = CDPState.builder()
            .cdpId(cdp.getId())
            .version(cdp.getVersion())
            .healthStateAssessment(cdp.getHealthStateAssessment())
            .patientState(cdp.getPatientState())
            .ddx(cdp.getDdx())
            .workupPlan(cdp.getWorkupPlan())
            .managementPlan(cdp.getManagementPlan())
            .triage(cdp.getTriage())
            .uncertainty(cdp.getUncertainty())
            .build();
        
        // 识别信息缺口
        state.setInformationGaps(identifyInformationGaps(state));
        
        // 识别证据冲突
        state.setEvidenceConflicts(identifyEvidenceConflicts(state));
        
        // 识别风险信号
        state.setRiskSignals(identifyRiskSignals(state));
        
        return state;
    }
    
    /**
     * Plan（规划）：决定调用哪些工具
     */
    private ToolCallPlan plan(CDPState cdpState, AgentState agentState) {
        // 根据当前CDP状态和AgentState规划工具调用
        // 1. 如果健康状态未判定 → 调用 tool_0
        if (cdpState.getHealthStateAssessment() == null) {
            return ToolCallPlan.builder()
                .toolId("tool_0")
                .toolName("健康状态判定工具")
                .priority(1)
                .build();
        }
        
        // 2. 如果工作态为临床诊疗态，且患者状态未结构化 → 调用 tool_1
        if (cdpState.getWorkMode() == WorkMode.CLINICAL_MODE 
            && cdpState.getPatientState() == null) {
            return ToolCallPlan.builder()
                .toolId("tool_1")
                .toolName("病例理解工具")
                .priority(1)
                .build();
        }
        
        // 3. 如果信息缺口存在 → 调用 tool_2
        if (!cdpState.getInformationGaps().isEmpty()) {
            return ToolCallPlan.builder()
                .toolId("tool_2")
                .toolName("主动问诊工具")
                .priority(1)
                .build();
        }
        
        // 4. 如果DDx未生成 → 调用 tool_3
        if (cdpState.getDdx() == null || cdpState.getDdx().isEmpty()) {
            return ToolCallPlan.builder()
                .toolId("tool_3")
                .toolName("鉴别诊断工具")
                .priority(1)
                .build();
        }
        
        // ... 其他规划逻辑
        
        return null;
    }
    
    /**
     * Act（执行）：调用工具
     */
    private ToolResult act(ToolCallPlan plan, CDP cdp, AgentState agentState) {
        // 生成ToolContext
        ToolContext context = ToolContext.builder()
            .traceId(generateTraceId())
            .cdpReference(CDPReference.builder()
                .cdpId(cdp.getId())
                .version(cdp.getVersion())
                .readFields(plan.getReadFields())
                .build())
            .agentStateSummary(AgentStateSummary.builder()
                .currentStep(agentState.getCurrentStep())
                .workMode(agentState.getWorkMode())
                .build())
            .constraints(agentState.getConstraints())
            .callParams(plan.getCallParams())
            .build();
        
        // 调用工具服务
        ToolResult result = toolCaller.invoke(plan.getToolId(), context);
        
        // 记录到AuditTrail
        auditTrailManager.recordToolCall(cdp.getId(), context, result);
        
        return result;
    }
    
    /**
     * Update（更新）：更新CDP和AgentState
     */
    private void update(CDP cdp, AgentState agentState, ToolResult result) {
        // 评估ToolResult的quality和evidence
        if (result.getStatus() == ToolStatus.SUCCESS 
            && result.getQuality().getConfidence() >= agentState.getThresholds().getConfidenceThreshold()) {
            
            // 进行evidence fusion和conflict resolution
            EvidenceFusionResult fusionResult = evidenceFusion.fuse(cdp, result);
            
            // 决定是否写回CDP（根据suggested_writes）
            for (SuggestedWrite write : result.getSuggestedWrites()) {
                cdpManager.updateCDP(cdp.getId(), Map.of(write.getFieldPath(), write.getValue()));
            }
            
            // 更新AgentState
            agentStateManager.updateAgentState(agentState.getId(), Map.of(
                "tried_tools", updateTriedTools(agentState.getTriedTools(), result.getToolId()),
                "current_step", determineCurrentStep(cdp, result)
            ));
        }
        
        // 记录到AuditTrail
        auditTrailManager.recordCDPUpdate(cdp.getId(), result);
    }
    
    /**
     * Evaluate（评估）：评估停止条件
     */
    private Decision evaluate(CDP cdp, AgentState agentState) {
        // 评估停止条件
        StopConditionEvaluation evaluation = stopConditionEvaluator.evaluate(cdp, agentState);
        
        if (evaluation.isStopConditionMet()) {
            return Decision.builder()
                .type(DecisionType.STOP)
                .reason("所有停止条件满足")
                .build();
        }
        
        // 评估升级条件
        EscalationEvaluation escalation = escalationHandler.evaluate(cdp, agentState);
        if (escalation.isEscalationNeeded()) {
            return Decision.builder()
                .type(DecisionType.ESCALATE)
                .reason(escalation.getReason())
                .build();
        }
        
        // 评估拒答条件
        RefusalEvaluation refusal = refusalHandler.evaluate(cdp, agentState);
        if (refusal.isRefusalNeeded()) {
            return Decision.builder()
                .type(DecisionType.REFUSE)
                .reason(refusal.getReason())
                .build();
        }
        
        // 继续循环
        return Decision.builder()
            .type(DecisionType.CONTINUE)
            .build();
    }
}
```

**完整诊断流程示例**（基于主Agent运行循环）：

```java
/**
 * 诊断流程编排服务（基于主Agent运行循环）
 */
@Service
public class DiagnosisOrchestrationService {
    
    /**
     * 完整诊断流程
     */
    public DiagnosisResult diagnose(UserInput input) {
        String sessionId = generateSessionId();
        
        // 主Agent运行循环
        ClinicalAgentBrain agent = new ClinicalAgentBrain();
        DiagnosisResult result = agent.runAgentLoop(input);
        
        // 【工具0：健康状态判定】
        // 主Agent在运行循环中调用 tool_0
        // - 输入：用户输入、基本信息
        // - 输出：工作态判定、风险等级
        // - 写回：cdp.health_state_assessment
        
        if (result.getWorkMode() == WorkMode.WELLNESS_MODE) {
            // 健康管理态：生成健康管理计划并返回
            return result;
        }
        
        // 【临床诊疗态流程】
        // 主Agent继续运行循环，依次调用各个工具
        CDP cdp = cdpManager.getCDP(result.getCdpId());
        
        // 【工具1：病例理解工具】
        ClinicalParsingResult parsingResult = clinicalParsingService.parse(input, cdp);
        
        // 【工具2：主动问诊工具】
        while (needsMoreInformation(cdp)) {
            InterviewResult interviewResult = dialogService.interview(cdp);
            // 等待用户回答
            // ...
        }
        
        // 【工具3：鉴别诊断工具（DR.KNOWS核心）】
        DifferentialDiagnosisResult diagnosisResult = diagnosisEngineService.diagnose(cdp);
        
        // 【tool_4：检查建议】
        WorkupPlanResult workupResult = workupPlannerService.planWorkup(cdp);
        
        // 【tool_5：治疗建议】
        TreatmentPlanResult treatmentResult = treatmentEngineService.planTreatment(cdp);
        
        // 【tool_6：风险评估】
        RiskAssessmentResult riskResult = riskAssessmentService.assessRisk(cdp);
        
        // 【tool_7：可解释性】
        EvidenceChainResult explanationResult = explanationService.explain(cdp);
        
        // 生成诊断结果
        return DiagnosisResult.builder()
            .cdpId(cdp.getId())
            .diagnosisResult(diagnosisResult)
            .workupPlan(workupResult)
            .treatmentPlan(treatmentResult)
            .riskAssessment(riskResult)
            .explanation(explanationResult)
            .build();
    }
}
```

---

### 1.11 5步AI循证诊断流程详细业务逻辑（临床诊疗态核心流程）

> **参考文档**：《AI医生系统-系统功能设计.md》  
> **核心思想**：将临床诊疗态流程组织为5步AI循证诊断流程，确保诊断过程可推理、可复用、可审计。

#### 1.11.1 流程总览

临床诊疗态采用**5步AI循证诊断流程**：

```
Step 1: 识别问题
  ↓
Step 2: 构建鉴别诊断候选集并分层
  ↓
Step 3: 组织候选集并建立分流路径
  ↓
Step 4: 采集关键证据并形成排序与验证计划
  ↓
Step 5: 回填证据并输出终点结论包
```

**流程特点**：
- **可推理**：每个步骤都有明确的推理逻辑和依据
- **可复用**：问题清单、候选集、分流路径等可沉淀为模板库
- **可审计**：每个步骤的输出都有结构化记录，支持追溯和验证
- **可回退**：支持回退到任意步骤，确保系统不会在错误结论上"锁死"

#### 1.11.2 Step 1：识别问题

**目标**：把用户的自然语言描述，转化成可推理、可复用、可审计的结构化"问题清单"，作为后续候选方向生成与分流问诊的唯一输入。

**核心任务**：
1. **概念归一化（明确用户在说什么）** - 把"原话"翻译为医学可用的标准概念
2. **形成完整问题清单** - 不仅记录主诉，还要同步记录伴随问题与关键背景
3. **标记信息缺口（决定能不能进入下一步）** - 明确"当前已知什么、还缺什么、缺了会造成什么推理风险"

**业务逻辑**：
```java
/**
 * Step 1：识别问题服务
 */
@Service
public class Step1IdentifyProblemService {
    
    /**
     * Step 1：识别问题
     */
    public StructuredQuestionList identifyProblem(CDP cdp, String userInput) {
        // 1. 概念归一化（tool_1）
        List<NormalizedConcept> normalizedConcepts = clinicalParsingService.normalizeConcepts(userInput);
        
        // 2. 形成完整问题清单（tool_1）
        StructuredQuestionList questionList = clinicalParsingService.buildProblemList(
            normalizedConcepts,
            cdp.getPatientState()
        );
        
        // 3. 标记信息缺口（tool_2）
        InformationGaps informationGaps = dialogService.identifyGaps(questionList, cdp);
        
        // 4. 更新CDP
        cdp.setPatientState(questionList.toPatientState());
        cdp.getUncertainty().setMissingCriticalInfo(informationGaps.getRequiredGaps());
        
        return questionList;
    }
}
```

**输出标准**：结构化问题清单（包含主要问题、伴随问题、关键背景、信息缺口）

#### 1.11.3 Step 2：构建鉴别诊断候选集并分层

**目标**：在 Step 1 的"问题清单"基础上，建立该主诉的鉴别诊断全集，并按临床风险与证据强度分成三层。

**核心任务**：
1. **生成鉴别诊断全集（先"列全"）** - 围绕主要问题、伴随问题与关键背景，列出所有可能方向
2. **三层分层（把"可能"变成"可推进"）** - 首要假设/主要备选/必须排除
3. **为每个候选方向标注"入选依据"** - 每个方向必须写清楚是由问题清单中的哪条线索触发纳入

**业务逻辑**：
```java
/**
 * Step 2：构建鉴别诊断候选集并分层服务
 */
@Service
public class Step2BuildDDxCandidatesService {
    
    /**
     * Step 2：构建鉴别诊断候选集并分层
     */
    public ThreeLayerResult buildDDxCandidates(CDP cdp, StructuredQuestionList questionList) {
        // 1. 生成鉴别诊断全集（tool_3 - DR.KNOWS核心）
        List<Diagnosis> ddxCandidates = diagnosisEngineService.generateDDxCandidates(questionList);
        
        // 2. 三层分层（tool_3 + tool_6）
        ThreeLayerResult threeLayerResult = threeLayerClassifier.classify(
            ddxCandidates,
            cdp.getPatientState(),
            riskAssessmentService
        );
        
        // 3. 更新CDP
        cdp.setDdx(threeLayerResult.toDDXList());
        
        return threeLayerResult;
    }
}
```

**输出标准**：鉴别诊断分层清单（首要假设、主要备选诊断、必须排除的高危诊断）

#### 1.11.4 Step 3：组织候选集并建立分流路径

**目标**：把 Step 2 的"分层候选清单"组织成可推进的推理结构，并提炼出能在问诊与基础测量中快速收缩范围的分流路径。

**核心任务**：
1. **组织成推理子组（把清单变成结构）** - 将候选方向按最有效的维度组织为子组
2. **提炼关键差异点（分流用）** - 为每对子组或每个关键方向提炼"能最快区分"的差异点
3. **明确"必须优先处理"的路径（高危先行）** - 对必须排除的高危方向建立优先路径
4. **形成分流问题清单（供下一步证据采集使用）** - 把差异点转换为结构化问题

**业务逻辑**：
```java
/**
 * Step 3：组织候选集并建立分流路径服务
 */
@Service
public class Step3OrganizeRoutingPathService {
    
    /**
     * Step 3：组织候选集并建立分流路径
     */
    public RoutingPath organizeRoutingPath(CDP cdp, ThreeLayerResult threeLayerResult) {
        // 1. 组织成推理子组（tool_3）
        List<ReasoningSubgroup> subgroups = reasoningOrganizer.organizeSubgroups(
            threeLayerResult,
            cdp.getPatientState()
        );
        
        // 2. 提炼关键差异点（tool_3）
        List<KeyDifference> keyDifferences = reasoningOrganizer.extractKeyDifferences(subgroups);
        
        // 3. 形成分流路径清单（tool_2）
        RoutingPath routingPath = dialogService.designRoutingPath(
            subgroups,
            keyDifferences,
            cdp
        );
        
        // 4. 更新CDP（临时字段，用于指导问诊）
        cdp.setRoutingPath(routingPath);
        
        return routingPath;
    }
}
```

**输出标准**：推理结构表或结构图（推理子组、关键差异点、分流路径清单）

#### 1.11.5 Step 4：采集关键证据并形成排序与验证计划

**目标**：沿着 Step 3 的分流路径，系统采集能够"推动排序变化"的关键证据，形成稳定的三层清单，并制定验证计划。

**核心任务**：
1. **采集关键证据（用证据推动，而不是继续罗列）** - 阳性线索、关键阴性线索
2. **固化三层排序（把结果输出成稳定结构）** - 最可能方向、必须排除方向、积极备选方向
3. **制定验证计划（让排序进入下一阶段）** - 针对"最可能"和"必须排除"制定验证计划

**业务逻辑**：
```java
/**
 * Step 4：采集关键证据并形成排序与验证计划服务
 */
@Service
public class Step4CollectEvidenceAndPlanService {
    
    /**
     * Step 4：采集关键证据并形成排序与验证计划
     */
    public VerificationPlan collectEvidenceAndPlan(CDP cdp, RoutingPath routingPath) {
        // 1. 采集关键证据（tool_2）
        List<Evidence> evidenceList = dialogService.collectKeyEvidence(
            routingPath,
            cdp
        );
        
        // 2. 固化三层排序（tool_3）
        ThreeLayerResult solidifiedRanking = threeLayerClassifier.solidifyRanking(
            evidenceList,
            cdp.getDdx()
        );
        
        // 3. 制定验证计划（tool_4）
        VerificationPlan verificationPlan = verificationPlanBuilder.generateVerificationPlan(
            solidifiedRanking,
            cdp
        );
        
        // 4. 更新CDP
        cdp.setEvidenceGraph(evidenceList);
        cdp.setDdx(solidifiedRanking.toDDXList());
        cdp.setWorkupPlan(verificationPlan);
        
        return verificationPlan;
    }
}
```

**输出标准**：证据清单（结构化）、三层排序清单、验证计划

#### 1.11.6 Step 5：回填证据并输出终点结论包

**目标**：将 Step 4 的验证结果回填到证据清单与问题清单中，更新三层排序，并输出可行动、可审计的终点结论包。

**核心任务**：
1. **证据回填（把结果变成可审计记录）** - 把新增信息逐条回填，标记其对各方向的作用
2. **更新三层排序（根据证据重排）** - 根据回填证据重新确定最可能方向、必须排除方向、积极备选方向
3. **输出终点结论包（两类终点）** - 可确证终点/不可确证终点（四要素：结论、必须排除项状态、关键依据、行动与随访）
4. **复评与回退（让流程闭环且可迭代）** - 当出现证据冲突、症状演变或处理无效时，触发回退

**业务逻辑**：
```java
/**
 * Step 5：回填证据并输出终点结论包服务
 */
@Service
public class Step5BackfillAndConcludeService {
    
    /**
     * Step 5：回填证据并输出终点结论包
     */
    public ConclusionPackage backfillAndConclude(CDP cdp, VerificationPlan verificationPlan) {
        // 1. 证据回填（tool_1）
        List<Evidence> backfilledEvidence = clinicalParsingService.backfillEvidence(
            verificationPlan,
            cdp
        );
        
        // 2. 更新三层排序（tool_3）
        ThreeLayerResult updatedRanking = threeLayerClassifier.updateRanking(
            backfilledEvidence,
            cdp.getDdx()
        );
        
        // 3. 生成终点结论包（tool_7 + tool_5 + tool_6）
        ConclusionPackage conclusionPackage = conclusionPackageBuilder.generateConclusionPackage(
            updatedRanking,
            backfilledEvidence,
            treatmentEngineService,
            riskAssessmentService,
            cdp
        );
        
        // 4. 更新CDP
        cdp.getEvidenceGraph().addAll(backfilledEvidence);
        cdp.setDdx(updatedRanking.toDDXList());
        cdp.setConclusionPackage(conclusionPackage);
        
        // 5. 检查回退条件
        List<RollbackCondition> rollbackConditions = rollbackEngine.checkRollbackConditions(cdp);
        if (!rollbackConditions.isEmpty()) {
            // 触发回退
            cdp = rollbackEngine.executeRollback(rollbackConditions.get(0), cdp);
        }
        
        return conclusionPackage;
    }
}
```

**输出标准**：证据回填记录、更新后的三层清单、终点结论包（四要素）

---

### 1.12 阶段5：结果输出（证据回填 + 更新排序 + 输出终点结论包）

**注意**：此部分已整合到tool_7（证据链工具）中，不再作为独立阶段。

#### 1.4.1 三层分层业务逻辑

```java
/**
 * 三层分层分类器
 */
@Service
public class ThreeLayerClassifier {
    
    /**
     * 将候选疾病分为三层
     * 根据系统设计方案：
     * - 首要假设（1个）：当前信息最能支持、最符合整体表现的方向
     * - 主要备选诊断（1-2个）：与首要假设并列需要对比、仍可能成立的方向
     * - 必须排除的高危诊断（0-1个）：一旦漏诊后果严重，即使概率不高也必须纳入并优先排除
     */
    public ThreeLayerResult classify(DiagnosisEngineResult engineResult) {
        Map<String, Double> candidates = engineResult.getPossibilities();
        
        // 1. 识别高危诊断
        List<String> highRiskDiseases = identifyHighRiskDiseases(candidates);
        
        // 2. 选择首要假设（排除高危诊断，选择可能性最高的）
        PrimaryHypothesis primaryHypothesis = selectPrimaryHypothesis(candidates, highRiskDiseases);
        
        // 3. 选择主要备选诊断（1-2个）
        List<MainAlternative> mainAlternatives = selectMainAlternatives(candidates, primaryHypothesis, highRiskDiseases);
        
        // 4. 选择必须排除的高危诊断（0-1个）
        MustExcludeDiagnosis mustExclude = selectMustExclude(candidates, highRiskDiseases);
        
        return ThreeLayerResult.builder()
            .primaryHypothesis(primaryHypothesis)
            .mainAlternatives(mainAlternatives)
            .mustExclude(mustExclude)
            .allCandidates(candidates)
            .build();
    }
    
    /**
     * 识别高危诊断
     * 规则：一旦漏诊后果严重，即使概率不高也必须纳入并优先排除
     */
    private List<String> identifyHighRiskDiseases(Map<String, Double> candidates) {
        List<String> highRiskKeywords = Arrays.asList(
            "心肌梗死", "脑梗死", "肺栓塞", "主动脉夹层",
            "急性心肌梗死", "急性脑梗死", "急性肺栓塞",
            "急性冠脉综合征", "脑出血", "蛛网膜下腔出血"
        );
        
        return candidates.keySet().stream()
            .filter(disease -> highRiskKeywords.stream()
                .anyMatch(keyword -> disease.contains(keyword)))
            .collect(Collectors.toList());
    }
    
    /**
     * 选择首要假设（1个）
     */
    private PrimaryHypothesis selectPrimaryHypothesis(
            Map<String, Double> candidates, 
            List<String> highRiskDiseases) {
        
        // 排除高危诊断（高危诊断单独处理）
        Map<String, Double> filteredCandidates = candidates.entrySet().stream()
            .filter(entry -> !highRiskDiseases.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        if (filteredCandidates.isEmpty()) {
            return null;
        }
        
        // 选择可能性最高的
        Map.Entry<String, Double> topEntry = filteredCandidates.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);
        
        if (topEntry == null) {
            return null;
        }
        
        return PrimaryHypothesis.builder()
            .disease(topEntry.getKey())
            .score(topEntry.getValue())
            .layer("primary_hypothesis")
            .build();
    }
    
    /**
     * 选择主要备选诊断（1-2个）
     */
    private List<MainAlternative> selectMainAlternatives(
            Map<String, Double> candidates,
            PrimaryHypothesis primary,
            List<String> highRiskDiseases) {
        
        if (primary == null) {
            return Collections.emptyList();
        }
        
        // 排除首要假设和高危诊断
        Map<String, Double> filteredCandidates = candidates.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(primary.getDisease()) 
                          && !highRiskDiseases.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        // 选择可能性较高的1-2个
        List<Map.Entry<String, Double>> sortedCandidates = filteredCandidates.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(2)
            .collect(Collectors.toList());
        
        return sortedCandidates.stream()
            .map(entry -> MainAlternative.builder()
                .disease(entry.getKey())
                .score(entry.getValue())
                .layer("main_alternative")
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * 选择必须排除的高危诊断（0-1个）
     * 规则：即使可能性不高，也必须纳入并优先排除
     */
    private MustExcludeDiagnosis selectMustExclude(
            Map<String, Double> candidates,
            List<String> highRiskDiseases) {
        
        if (highRiskDiseases.isEmpty()) {
            return null;
        }
        
        // 选择可能性最高的高危诊断（即使不高也要排除）
        Optional<Map.Entry<String, Double>> topHighRisk = highRiskDiseases.stream()
            .map(disease -> new AbstractMap.SimpleEntry<>(disease, candidates.get(disease)))
            .filter(entry -> entry.getValue() != null)
            .max(Map.Entry.comparingByValue());
        
        if (!topHighRisk.isPresent()) {
            return null;
        }
        
        Map.Entry<String, Double> entry = topHighRisk.get();
        return MustExcludeDiagnosis.builder()
            .disease(entry.getKey())
            .score(entry.getValue())
            .layer("must_exclude")
            .reason("高危诊断，必须排除，即使可能性不高")
            .build();
    }
}
```

### 1.5 阶段4：鉴别诊断（组织推理结构 + 采集关键证据 + 形成三层排序与验证计划）

#### 1.5.1 支持/反对证据分析业务逻辑

```java
/**
 * 证据分析服务
 */
@Service
public class EvidenceAnalyzer {
    
    /**
     * 分析支持/反对证据
     */
    public EvidenceAnalysis analyzeEvidence(
            ThreeLayerResult threeLayerResult,
            StructuredQuestionList questionList,
            DiagnosisEngineResult engineResult) {
        
        EvidenceAnalysis analysis = new EvidenceAnalysis();
        
        // 分析首要假设的证据
        if (threeLayerResult.getPrimaryHypothesis() != null) {
            PrimaryHypothesis primary = threeLayerResult.getPrimaryHypothesis();
            DiseaseEvidence primaryEvidence = analyzeDiseaseEvidence(
                primary.getDisease(), 
                questionList
            );
            analysis.setPrimaryHypothesisEvidence(primaryEvidence);
        }
        
        // 分析主要备选诊断的证据
        List<DiseaseEvidence> alternativesEvidence = threeLayerResult.getMainAlternatives().stream()
            .map(alt -> analyzeDiseaseEvidence(alt.getDisease(), questionList))
            .collect(Collectors.toList());
        analysis.setAlternativesEvidence(alternativesEvidence);
        
        // 分析必须排除的高危诊断的证据
        if (threeLayerResult.getMustExclude() != null) {
            MustExcludeDiagnosis mustExclude = threeLayerResult.getMustExclude();
            DiseaseEvidence mustExcludeEvidence = analyzeDiseaseEvidence(
                mustExclude.getDisease(),
                questionList
            );
            analysis.setMustExcludeEvidence(mustExcludeEvidence);
        }
        
        return analysis;
    }
    
    /**
     * 分析单个疾病的证据
     */
    private DiseaseEvidence analyzeDiseaseEvidence(
            String disease,
            StructuredQuestionList questionList) {
        
        DiseaseEvidence evidence = new DiseaseEvidence();
        evidence.setDisease(disease);
        
        // 1. 症状支持证据
        List<SupportingEvidence> symptomSupport = analyzeSymptomSupport(disease, questionList);
        evidence.setSupportingSymptoms(symptomSupport);
        
        // 2. 体征支持证据
        List<SupportingEvidence> signSupport = analyzeSignSupport(disease, questionList);
        evidence.setSupportingSigns(signSupport);
        
        // 3. 检查结果支持证据
        List<SupportingEvidence> examSupport = analyzeExamSupport(disease, questionList);
        evidence.setSupportingExaminations(examSupport);
        
        // 4. 用户画像匹配
        ProfileMatch profileMatch = analyzeProfileMatch(disease, questionList);
        evidence.setProfileMatch(profileMatch);
        
        // 5. 反对证据
        List<OpposingEvidence> opposing = analyzeOpposingEvidence(disease, questionList);
        evidence.setOpposingEvidence(opposing);
        
        // 6. 证据强度分级
        EvidenceStrength strength = calculateEvidenceStrength(evidence);
        evidence.setStrength(strength);
        
        return evidence;
    }
    
    /**
     * 分析症状支持证据
     */
    private List<SupportingEvidence> analyzeSymptomSupport(
            String disease,
            StructuredQuestionList questionList) {
        
        List<SupportingEvidence> supporting = new ArrayList<>();
        
        ChiefComplaint chiefComplaint = questionList.getChiefComplaint();
        if (chiefComplaint != null) {
            String symptom = chiefComplaint.getName();
            if (isSymptomSupportingDisease(symptom, disease)) {
                SupportingEvidence evidence = SupportingEvidence.builder()
                    .item(symptom)
                    .type("symptom")
                    .strength(determineEvidenceStrength(symptom, disease))
                    .build();
                supporting.add(evidence);
            }
        }
        
        // 分析伴随症状
        List<AccompanyingSymptom> accompanying = questionList.getAccompanyingSymptoms();
        if (accompanying != null) {
            for (AccompanyingSymptom acc : accompanying) {
                if (isSymptomSupportingDisease(acc.getName(), disease)) {
                    SupportingEvidence evidence = SupportingEvidence.builder()
                        .item(acc.getName())
                        .type("accompanying_symptom")
                        .strength(determineEvidenceStrength(acc.getName(), disease))
                        .build();
                    supporting.add(evidence);
                }
            }
        }
        
        return supporting;
    }
    
    /**
     * 判断症状是否支持疾病
     */
    private boolean isSymptomSupportingDisease(String symptom, String disease) {
        // 使用知识图谱或规则库判断
        // 这里简化处理，实际应该查询知识图谱
        return true; // 示例
    }
    
    /**
     * 确定证据强度
     * - 强证据：特异性症状/体征（权重1.0）
     * - 中证据：常见症状/体征（权重0.7）
     * - 弱证据：非特异性症状/体征（权重0.4）
     */
    private String determineEvidenceStrength(String symptom, String disease) {
        // 使用知识图谱查询症状-疾病关系的特异性
        // 这里简化处理
        if (isSpecificSymptom(symptom, disease)) {
            return "strong";
        } else if (isCommonSymptom(symptom, disease)) {
            return "medium";
        } else {
            return "weak";
        }
    }
}
```

### 1.6 阶段5：结果输出（证据回填 + 更新排序 + 输出终点结论包）

#### 1.6.1 终点结论包构建业务逻辑

```java
/**
 * 终点结论包构建器
 */
@Service
public class ConclusionPackageBuilder {
    
    /**
     * 构建终点结论包
     * 根据系统设计方案，终点结论包包含四要素：
     * 1. 结论（可确证/不可确证）
     * 2. 必须排除项状态
     * 3. 关键依据（至少三条证据）
     * 4. 行动与随访
     */
    public ConclusionPackage buildConclusionPackage(
            DiagnosisEngineResult engineResult,
            ThreeLayerResult threeLayerResult,
            EvidenceAnalysis evidenceAnalysis) {
        
        ConclusionPackage conclusion = new ConclusionPackage();
        
        // 1. 结论
        Conclusion conclusionItem = buildConclusion(threeLayerResult, evidenceAnalysis);
        conclusion.setConclusion(conclusionItem);
        
        // 2. 必须排除项状态
        MustExcludeStatus mustExcludeStatus = buildMustExcludeStatus(
            threeLayerResult.getMustExclude()
        );
        conclusion.setMustExcludeStatus(mustExcludeStatus);
        
        // 3. 关键依据（至少三条证据）
        List<KeyEvidence> keyEvidence = buildKeyEvidence(evidenceAnalysis, 3);
        conclusion.setKeyEvidence(keyEvidence);
        
        // 4. 行动与随访
        ActionAndFollowUp actionAndFollowUp = buildActionAndFollowUp(
            engineResult, 
            threeLayerResult
        );
        conclusion.setActionAndFollowUp(actionAndFollowUp);
        
        return conclusion;
    }
    
    /**
     * 构建结论
     */
    private Conclusion buildConclusion(
            ThreeLayerResult threeLayerResult,
            EvidenceAnalysis evidenceAnalysis) {
        
        Conclusion conclusion = new Conclusion();
        PrimaryHypothesis primary = threeLayerResult.getPrimaryHypothesis();
        
        if (primary == null) {
            conclusion.setType(ConclusionType.UNCERTAIN);
            conclusion.setDiagnosis("无法确定");
            return conclusion;
        }
        
        // 判断是否可确证
        boolean canConfirm = canConfirmDiagnosis(primary, evidenceAnalysis);
        
        if (canConfirm) {
            // 可确证终点
            conclusion.setType(ConclusionType.CONFIRMED);
            conclusion.setDiagnosis(primary.getDisease());
            conclusion.setConfidence(primary.getScore());
        } else {
            // 不可确证终点
            conclusion.setType(ConclusionType.PROBABLE);
            conclusion.setDiagnosis(primary.getDisease());
            conclusion.setConfidence(primary.getScore());
            conclusion.setUncertaintyReason("当前证据不足以确证，需要进一步检查");
            conclusion.setReviewWindow("3天后复评");
            conclusion.setUpgradeTriggers(Arrays.asList(
                "症状加重",
                "出现新症状",
                "检查结果异常"
            ));
        }
        
        return conclusion;
    }
    
    /**
     * 判断是否可确证
     */
    private boolean canConfirmDiagnosis(
            PrimaryHypothesis primary,
            EvidenceAnalysis evidenceAnalysis) {
        
        // 规则：
        // 1. 可能性分数 > 0.8
        // 2. 有强证据支持
        // 3. 无重要反对证据
        
        if (primary.getScore() < 0.8) {
            return false;
        }
        
        DiseaseEvidence primaryEvidence = evidenceAnalysis.getPrimaryHypothesisEvidence();
        if (primaryEvidence == null) {
            return false;
        }
        
        // 检查是否有强证据
        boolean hasStrongEvidence = primaryEvidence.getSupportingSymptoms().stream()
            .anyMatch(e -> "strong".equals(e.getStrength()));
        
        // 检查是否有重要反对证据
        boolean hasImportantOpposing = primaryEvidence.getOpposingEvidence().stream()
            .anyMatch(e -> "strong".equals(e.getStrength()) || "medium".equals(e.getStrength()));
        
        return hasStrongEvidence && !hasImportantOpposing;
    }
    
    /**
     * 构建必须排除项状态
     */
    private MustExcludeStatus buildMustExcludeStatus(MustExcludeDiagnosis mustExclude) {
        MustExcludeStatus status = new MustExcludeStatus();
        
        if (mustExclude == null) {
            status.setStatus(ExcludeStatus.NONE);
            return status;
        }
        
        // 判断是否已排除
        // 这里简化处理，实际需要根据验证计划的结果判断
        boolean excluded = checkExcluded(mustExclude);
        
        if (excluded) {
            status.setStatus(ExcludeStatus.EXCLUDED);
            status.setExcludeReason("已通过验证明确排除");
        } else {
            // 判断是否需要线下检查才能排除
            boolean needOfflineCheck = needOfflineCheckToExclude(mustExclude);
            if (needOfflineCheck) {
                status.setStatus(ExcludeStatus.NEED_OFFLINE_EXCLUDE);
                status.setExcludeReason("需要线下检查才能排除");
            } else {
                status.setStatus(ExcludeStatus.NOT_EXCLUDED);
                status.setExcludeReason("仍需保留，需要进一步验证");
            }
        }
        
        return status;
    }
}
```

### 1.7 智能追问算法（更新版）

#### 1.7.1 基于信息缺口分级的追问策略

```java
/**
 * 智能追问服务（基于信息缺口分级）
 */
@Service
public class AdaptiveQuestioningService {
    
    /**
     * 生成追问问题（按信息缺口等级优先级排序）
     */
    public QuestionResponse generateQuestion(
            StructuredQuestionList questionList,
            InformationGaps gaps,
            double completeness) {
        
        // 优先级1：必填缺口（必须先补齐）
        if (!gaps.getRequiredGaps().isEmpty()) {
            String gapType = gaps.getRequiredGaps().get(0);
            String question = generateRequiredQuestion(gapType, questionList);
            return QuestionResponse.builder()
                .question(question)
                .questionType(gapType)
                .priority("required")
                .completeness(completeness)
                .build();
        }
        
        // 优先级2：重要缺口（尽量补齐，影响诊断准确性）
        if (!gaps.getImportantGaps().isEmpty() && completeness < 0.8) {
            String gapType = gaps.getImportantGaps().get(0);
            String question = generateImportantQuestion(gapType, questionList);
            return QuestionResponse.builder()
                .question(question)
                .questionType(gapType)
                .priority("important")
                .completeness(completeness)
                .build();
        }
        
        // 优先级3：可选缺口（可后续补充）
        if (!gaps.getOptionalGaps().isEmpty() && completeness < 0.9) {
            String gapType = gaps.getOptionalGaps().get(0);
            String question = generateOptionalQuestion(gapType, questionList);
            return QuestionResponse.builder()
                .question(question)
                .questionType(gapType)
                .priority("optional")
                .completeness(completeness)
                .build();
        }
        
        // 无需继续追问
        return null;
    }
    
    /**
     * 生成必填缺口问题
     */
    private String generateRequiredQuestion(String gapType, StructuredQuestionList questionList) {
        Map<String, String> questions = new HashMap<>();
        questions.put("duration", "您这个症状出现多久了？");
        questions.put("severity", "症状严重程度如何？0-10分打几分？");
        questions.put("chief_complaint", "请详细描述一下您的主要症状。");
        questions.put("vital_signs", "您最近测过血压/心率/体温吗？");
        
        return questions.getOrDefault(gapType, "能详细描述一下吗？");
    }
    
    /**
     * 生成重要缺口问题
     */
    private String generateImportantQuestion(String gapType, StructuredQuestionList questionList) {
        Map<String, String> questions = new HashMap<>();
        questions.put("accompanying_symptoms", "除了这个症状，还有没有其他不舒服？");
        questions.put("medical_history", "您之前有没有类似的症状？或者您有没有XX疾病史？");
        questions.put("family_history", "您家里有没有人得过类似的病？");
        questions.put("medication_history", "您最近有没有吃什么药？");
        
        return questions.getOrDefault(gapType, "能补充一下相关信息吗？");
    }
    
    /**
     * 生成可选缺口问题
     */
    private String generateOptionalQuestion(String gapType, StructuredQuestionList questionList) {
        Map<String, String> questions = new HashMap<>();
        questions.put("lifestyle", "最近饮食、运动、睡眠有什么变化吗？");
        questions.put("recent_events", "最近有没有外伤、用药改变、旅行等？");
        
        return questions.getOrDefault(gapType, "还有其他需要补充的信息吗？");
    }
    
    /**
     * 判断是否应该停止追问
     * 规则（与系统设计方案保持一致）：
     * - 必填缺口已补齐 且 信息完整度 ≥ 60%（最低要求）
     * - 信息完整度达到阈值（≥ 70%，停止追问阈值）
     * - 追问轮次超过限制（最多3轮）
     */
    public boolean shouldStopQuestioning(
            StructuredQuestionList questionList,
            InformationGaps gaps,
            double completeness,
            int questioningCount) {
        
        // 条件1：必填缺口已补齐 且 信息完整度 ≥ 60%（最低要求）
        if (gaps.getRequiredGaps().isEmpty() && completeness >= 0.6) {
            return true;
        }
        
        // 条件2：信息完整度达到阈值（≥ 70%，停止追问阈值）
        if (completeness >= 0.7) {
            return true;
        }
        
        // 条件3：追问轮次超过限制（最多3轮）
        if (questioningCount >= 3) {
            return true;
        }
        
        return false;
    }
}
```

### 1.8 缺失信息识别（更新版）

```java
/**
 * 识别缺失信息
 */
public List<MissingInfo> identifyMissingInfo(DiagnosisRecord record, HealthProfile profile) {
    List<MissingInfo> missingInfoList = new ArrayList<>();
    
    // 1. 症状信息缺失
    if (record.getChiefComplaint() == null || record.getChiefComplaint().isEmpty()) {
        missingInfoList.add(new MissingInfo("chief_complaint", "主诉", 1));
    }
    if (record.getSymptomDuration() == null || record.getSymptomDuration().isEmpty()) {
        missingInfoList.add(new MissingInfo("symptom_duration", "症状持续时间", 1));
    }
    if (record.getSymptomSeverity() == null) {
        missingInfoList.add(new MissingInfo("symptom_severity", "症状严重程度", 1));
    }
    if (record.getAccompanyingSymptoms() == null || record.getAccompanyingSymptoms().isEmpty()) {
        missingInfoList.add(new MissingInfo("accompanying_symptoms", "伴随症状", 2));
    }
    
    // 2. 健康档案缺失
    if (profile == null) {
        missingInfoList.add(new MissingInfo("health_profile", "健康档案", 3));
    } else {
        if (profile.getAge() == null) {
            missingInfoList.add(new MissingInfo("age", "年龄", 3));
        }
        if (profile.getMedicalHistory() == null || profile.getMedicalHistory().isEmpty()) {
            missingInfoList.add(new MissingInfo("medical_history", "既往史", 3));
        }
    }
    
    // 3. 生命体征缺失
    if (record.getVitalSigns() == null || !record.getVitalSigns().containsKey("bp")) {
        missingInfoList.add(new MissingInfo("vital_signs_bp", "血压", 4));
    }
    
    // 按优先级排序
    missingInfoList.sort(Comparator.comparing(MissingInfo::getPriority));
    
    return missingInfoList;
}

@Data
@Builder
public static class MissingInfo {
    private String field;
    private String name;
    private Integer priority; // 1-4，数字越小优先级越高
}
```

## 二、智能追问算法设计（完整版）

### 2.1 算法概述

智能追问算法的目标是根据已收集的信息和信息缺口分级，智能判断还需要什么信息，并生成合适的追问问题。

### 2.2 追问问题生成（基于信息缺口分级）

```java
/**
 * 生成追问问题
 */
public QuestionResponse generateQuestion(DiagnosisRecord record, HealthProfile profile) {
    // 1. 识别缺失信息
    List<MissingInfo> missingInfoList = identifyMissingInfo(record, profile);
    
    if (missingInfoList.isEmpty()) {
        return null; // 信息足够，不需要追问
    }
    
    // 2. 选择优先级最高的缺失信息
    MissingInfo missingInfo = missingInfoList.get(0);
    
    // 3. 根据缺失信息类型生成问题
    String question = generateQuestionByType(missingInfo, record);
    
    // 4. 构建响应
    return QuestionResponse.builder()
        .question(question)
        .questionType(getQuestionType(missingInfo.getField()))
        .missingInfoType(missingInfo.getField())
        .required(true)
        .build();
}

/**
 * 根据缺失信息类型生成问题
 */
private String generateQuestionByType(MissingInfo missingInfo, DiagnosisRecord record) {
    String field = missingInfo.getField();
    
    switch (field) {
        case "symptom_duration":
            return "您这个症状出现多久了？";
            
        case "symptom_severity":
            return "症状严重程度如何？0-10分打几分？";
            
        case "accompanying_symptoms":
            String chiefComplaint = record.getChiefComplaint();
            return String.format("除了%s，还有没有其他不舒服？", chiefComplaint);
            
        case "medical_history":
            // 根据当前症状，询问相关既往史
            String relatedDisease = getRelatedDisease(record.getChiefComplaint());
            return String.format("您有没有%s等病史？", relatedDisease);
            
        case "vital_signs_bp":
            return "您最近测过血压吗？";
            
        default:
            return "能详细描述一下吗？";
    }
}

/**
 * 根据症状获取相关疾病
 */
private String getRelatedDisease(String chiefComplaint) {
    // 症状 -> 相关疾病的映射
    Map<String, String> symptomToDisease = new HashMap<>();
    symptomToDisease.put("胸痛", "高血压、心脏病");
    symptomToDisease.put("头痛", "高血压、偏头痛");
    symptomToDisease.put("腹痛", "胃炎、消化性溃疡");
    // ...
    
    return symptomToDisease.getOrDefault(chiefComplaint, "相关疾病");
}
```

---

## 三、规则引擎设计

### 2.1 规则库结构

#### 2.1.1 规则定义

```java
/**
 * 诊断规则
 */
@Data
@Builder
public class DiagnosisRule {
    /**
     * 规则ID
     */
    private String ruleId;
    
    /**
     * 规则名称
     */
    private String ruleName;
    
    /**
     * 规则优先级（high, medium, low）
     */
    private String priority;
    
    /**
     * 症状组合（必须全部匹配）
     */
    private List<String> requiredSymptoms;
    
    /**
     * 症状组合（至少匹配一个）
     */
    private List<String> optionalSymptoms;
    
    /**
     * 体征组合
     */
    private Map<String, Object> vitalSigns;
    
    /**
     * 目标疾病
     */
    private String disease;
    
    /**
     * 置信度（0-1）
     */
    private Double confidence;
    
    /**
     * 支持证据
     */
    private List<String> supportingEvidence;
    
    /**
     * 排除条件
     */
    private List<String> exclusionConditions;
}
```

#### 2.1.2 规则库示例

```json
[
  {
    "ruleId": "rule_001",
    "ruleName": "心绞痛规则",
    "priority": "high",
    "requiredSymptoms": ["胸痛", "气短"],
    "optionalSymptoms": ["出汗", "恶心"],
    "vitalSigns": {
      "bp": {"systolic": ">120"},
      "heart_rate": ">70"
    },
    "disease": "心绞痛",
    "confidence": 0.80,
    "supportingEvidence": ["胸痛", "气短", "有心血管风险"],
    "exclusionConditions": ["年龄<30", "无心血管病史"]
  },
  {
    "ruleId": "rule_002",
    "ruleName": "肺炎规则",
    "priority": "high",
    "requiredSymptoms": ["发热", "咳嗽", "胸痛"],
    "optionalSymptoms": ["咳痰", "呼吸困难"],
    "vitalSigns": {
      "temperature": ">37.5"
    },
    "disease": "肺炎",
    "confidence": 0.75,
    "supportingEvidence": ["发热", "咳嗽", "胸痛"],
    "exclusionConditions": []
  }
]
```

### 2.2 规则匹配算法

#### 2.2.1 匹配逻辑

```java
/**
 * 规则引擎
 */
@Service
public class RuleEngine {
    
    @Autowired
    private RuleRepository ruleRepository;
    
    /**
     * 执行规则匹配
     */
    public Map<String, Double> diagnose(DiagnosisEngineRequest request) {
        // 1. 获取所有规则
        List<DiagnosisRule> rules = ruleRepository.findAll();
        
        // 2. 提取用户症状
        List<String> userSymptoms = extractSymptoms(request);
        Map<String, Object> userVitalSigns = request.getVitalSigns();
        Map<String, Object> healthProfile = request.getHealthProfile();
        
        // 3. 匹配规则
        Map<String, Double> diseaseScores = new HashMap<>();
        
        for (DiagnosisRule rule : rules) {
            double matchScore = calculateMatchScore(rule, userSymptoms, userVitalSigns, healthProfile);
            
            if (matchScore > 0.7) { // 匹配阈值
                String disease = rule.getDisease();
                double confidence = rule.getConfidence() * matchScore;
                
                // 累加分数
                diseaseScores.put(disease, 
                    diseaseScores.getOrDefault(disease, 0.0) + confidence);
            }
        }
        
        // 4. 归一化分数（0-1）
        return normalizeScores(diseaseScores);
    }
    
    /**
     * 计算匹配分数
     */
    private double calculateMatchScore(DiagnosisRule rule, List<String> userSymptoms, 
                                       Map<String, Object> userVitalSigns,
                                       Map<String, Object> healthProfile) {
        double score = 0.0;
        double totalWeight = 0.0;
        
        // 1. 必须症状匹配（权重0.5）
        if (matchRequiredSymptoms(rule.getRequiredSymptoms(), userSymptoms)) {
            score += 0.5;
            totalWeight += 0.5;
        } else {
            return 0.0; // 必须症状不匹配，直接返回0
        }
        
        // 2. 可选症状匹配（权重0.3）
        double optionalMatchRate = matchOptionalSymptoms(rule.getOptionalSymptoms(), userSymptoms);
        score += 0.3 * optionalMatchRate;
        totalWeight += 0.3;
        
        // 3. 体征匹配（权重0.2）
        if (matchVitalSigns(rule.getVitalSigns(), userVitalSigns)) {
            score += 0.2;
            totalWeight += 0.2;
        }
        
        // 4. 排除条件检查
        if (checkExclusionConditions(rule.getExclusionConditions(), healthProfile)) {
            return 0.0; // 满足排除条件，返回0
        }
        
        // 5. 归一化
        return totalWeight > 0 ? score / totalWeight : 0.0;
    }
    
    /**
     * 匹配必须症状
     */
    private boolean matchRequiredSymptoms(List<String> requiredSymptoms, List<String> userSymptoms) {
        if (requiredSymptoms == null || requiredSymptoms.isEmpty()) {
            return true;
        }
        
        Set<String> userSymptomSet = new HashSet<>(userSymptoms);
        for (String requiredSymptom : requiredSymptoms) {
            if (!userSymptomSet.contains(requiredSymptom)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 匹配可选症状
     */
    private double matchOptionalSymptoms(List<String> optionalSymptoms, List<String> userSymptoms) {
        if (optionalSymptoms == null || optionalSymptoms.isEmpty()) {
            return 1.0;
        }
        
        Set<String> userSymptomSet = new HashSet<>(userSymptoms);
        int matchCount = 0;
        for (String optionalSymptom : optionalSymptoms) {
            if (userSymptomSet.contains(optionalSymptom)) {
                matchCount++;
            }
        }
        
        return (double) matchCount / optionalSymptoms.size();
    }
    
    /**
     * 匹配体征
     */
    private boolean matchVitalSigns(Map<String, Object> ruleVitalSigns, 
                                    Map<String, Object> userVitalSigns) {
        if (ruleVitalSigns == null || ruleVitalSigns.isEmpty()) {
            return true;
        }
        
        for (Map.Entry<String, Object> entry : ruleVitalSigns.entrySet()) {
            String key = entry.getKey();
            Object ruleValue = entry.getValue();
            Object userValue = userVitalSigns.get(key);
            
            if (!matchVitalSignValue(ruleValue, userValue)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 匹配体征值（支持范围、比较运算符）
     */
    private boolean matchVitalSignValue(Object ruleValue, Object userValue) {
        if (userValue == null) {
            return false;
        }
        
        String ruleStr = ruleValue.toString();
        
        // 支持格式：">120", "<100", "120-140"
        if (ruleStr.startsWith(">")) {
            double threshold = Double.parseDouble(ruleStr.substring(1));
            return Double.parseDouble(userValue.toString()) > threshold;
        } else if (ruleStr.startsWith("<")) {
            double threshold = Double.parseDouble(ruleStr.substring(1));
            return Double.parseDouble(userValue.toString()) < threshold;
        } else if (ruleStr.contains("-")) {
            String[] range = ruleStr.split("-");
            double min = Double.parseDouble(range[0]);
            double max = Double.parseDouble(range[1]);
            double userVal = Double.parseDouble(userValue.toString());
            return userVal >= min && userVal <= max;
        } else {
            return ruleStr.equals(userValue.toString());
        }
    }
    
    /**
     * 检查排除条件
     */
    private boolean checkExclusionConditions(List<String> exclusionConditions, 
                                           Map<String, Object> healthProfile) {
        if (exclusionConditions == null || exclusionConditions.isEmpty()) {
            return false;
        }
        
        for (String condition : exclusionConditions) {
            if (evaluateCondition(condition, healthProfile)) {
                return true; // 满足排除条件
            }
        }
        
        return false;
    }
    
    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String condition, Map<String, Object> healthProfile) {
        // 简单条件评估，如："年龄<30", "无心血管病史"
        // 实际实现可以使用表达式引擎（如SpEL、MVEL）
        
        if (condition.contains("年龄")) {
            Integer age = (Integer) healthProfile.get("age");
            if (age == null) return false;
            
            if (condition.contains("<")) {
                int threshold = Integer.parseInt(condition.replaceAll("[^0-9]", ""));
                return age < threshold;
            }
        }
        
        if (condition.contains("无") && condition.contains("病史")) {
            String diseaseType = extractDiseaseType(condition);
            List<String> medicalHistory = (List<String>) healthProfile.get("medicalHistory");
            if (medicalHistory == null || medicalHistory.isEmpty()) {
                return true; // 无病史，满足排除条件
            }
            return !medicalHistory.contains(diseaseType);
        }
        
        return false;
    }
}
```

---

## 四、知识图谱查询设计

### 3.1 图谱结构

#### 3.1.1 节点类型

- **Symptom（症状）**：胸痛、发热、咳嗽等
- **Disease（疾病）**：心绞痛、肺炎、胃炎等
- **Examination（检查）**：心电图、血常规、CT等
- **Sign（体征）**：血压、心率、体温等
- **Medicine（药物）**：阿司匹林、抗生素等

#### 3.1.2 关系类型

- **CAUSES（引起）**：症状 -> 疾病
- **DIAGNOSES（诊断）**：检查 -> 疾病
- **TREATS（治疗）**：药物 -> 疾病
- **RELATED_TO（相关）**：疾病 -> 疾病

### 3.2 查询逻辑

#### 3.2.1 Cypher查询

```java
/**
 * 知识图谱引擎
 */
@Service
public class KnowledgeGraphEngine {
    
    @Autowired
    private Neo4jDriver neo4jDriver;
    
    /**
     * 执行知识图谱查询
     */
    public Map<String, Double> diagnose(DiagnosisEngineRequest request) {
        List<String> symptoms = extractSymptoms(request);
        
        // 构建Cypher查询
        String query = """
            MATCH (s:Symptom)-[r:CAUSES]->(d:Disease)
            WHERE s.name IN $symptoms
            WITH d, count(r) as symptom_count, 
                 avg(r.probability) as avg_prob,
                 collect(s.name) as matched_symptoms
            RETURN d.name as disease, 
                   d.category as category,
                   symptom_count,
                   avg_prob,
                   matched_symptoms
            ORDER BY symptom_count DESC, avg_prob DESC
            LIMIT 5
            """;
        
        Map<String, Object> parameters = Map.of("symptoms", symptoms);
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(query, parameters);
            
            Map<String, Double> diseaseScores = new HashMap<>();
            
            while (result.hasNext()) {
                Record record = result.next();
                String disease = record.get("disease").asString();
                int symptomCount = record.get("symptom_count").asInt();
                double avgProb = record.get("avg_prob").asDouble();
                
                // 计算分数：症状匹配数 * 平均概率 / 总症状数
                double score = (symptomCount / (double) symptoms.size()) * avgProb;
                diseaseScores.put(disease, score);
            }
            
            return normalizeScores(diseaseScores);
        }
    }
    
    /**
     * 考虑用户画像的查询
     */
    public Map<String, Double> diagnoseWithProfile(DiagnosisEngineRequest request) {
        List<String> symptoms = extractSymptoms(request);
        Map<String, Object> healthProfile = request.getHealthProfile();
        
        Integer age = (Integer) healthProfile.get("age");
        String gender = (String) healthProfile.get("gender");
        
        String query = """
            MATCH (s:Symptom)-[r:CAUSES]->(d:Disease)
            WHERE s.name IN $symptoms
            AND (d.minAge IS NULL OR $age >= d.minAge)
            AND (d.maxAge IS NULL OR $age <= d.maxAge)
            AND (d.gender IS NULL OR d.gender = $gender OR d.gender = 'both')
            WITH d, count(r) as symptom_count, 
                 avg(r.probability) as avg_prob
            RETURN d.name as disease, 
                   symptom_count,
                   avg_prob
            ORDER BY symptom_count DESC, avg_prob DESC
            LIMIT 5
            """;
        
        Map<String, Object> parameters = Map.of(
            "symptoms", symptoms,
            "age", age != null ? age : 0,
            "gender", gender != null ? gender : "unknown"
        );
        
        // 执行查询...
    }
}
```

---

## 五、统计模型推理设计

### 4.1 模型输入特征

```java
/**
 * 特征提取
 */
public class FeatureExtractor {
    
    /**
     * 提取特征向量
     */
    public double[] extractFeatures(DiagnosisEngineRequest request) {
        List<Double> features = new ArrayList<>();
        
        // 1. 症状特征（one-hot编码）
        List<String> allSymptoms = getAllSymptoms(); // 所有可能的症状
        List<String> userSymptoms = extractSymptoms(request);
        for (String symptom : allSymptoms) {
            features.add(userSymptoms.contains(symptom) ? 1.0 : 0.0);
        }
        
        // 2. 用户画像特征
        Map<String, Object> profile = request.getHealthProfile();
        features.add(normalizeAge((Integer) profile.get("age")));
        features.add(encodeGender((String) profile.get("gender")));
        
        // 3. 体征特征
        Map<String, Object> vitalSigns = request.getVitalSigns();
        features.add(normalizeBP(vitalSigns));
        features.add(normalizeHeartRate(vitalSigns));
        features.add(normalizeTemperature(vitalSigns));
        
        return features.stream().mapToDouble(Double::doubleValue).toArray();
    }
    
    private double normalizeAge(Integer age) {
        // 归一化到0-1：假设年龄范围0-120
        return age != null ? age / 120.0 : 0.5;
    }
    
    private double encodeGender(String gender) {
        // 编码：male=1.0, female=0.0
        return "male".equals(gender) ? 1.0 : 0.0;
    }
}
```

### 4.2 模型推理

```python
# Python服务中的统计模型推理
import numpy as np
from sklearn.ensemble import RandomForestClassifier
import joblib

class StatisticalModelEngine:
    """统计模型引擎"""
    
    def __init__(self):
        # 加载训练好的模型
        self.model = joblib.load('models/diagnosis_model.pkl')
        self.label_encoder = joblib.load('models/label_encoder.pkl')
    
    async def diagnose(self, request: DiagnosisEngineRequest) -> Dict:
        """统计模型推理"""
        # 1. 提取特征
        features = self.extract_features(request)
        
        # 2. 模型预测
        probabilities = self.model.predict_proba([features])[0]
        
        # 3. 获取疾病标签
        disease_labels = self.label_encoder.classes_
        
        # 4. 构建结果
        possibilities = {}
        for i, disease in enumerate(disease_labels):
            possibilities[disease] = float(probabilities[i])
        
        # 5. 排序，返回Top 5（作为候选，最终展示Top 3-5）
        sorted_possibilities = sorted(
            possibilities.items(), 
            key=lambda x: x[1], 
            reverse=True
        )[:5]
        
        return {
            'possibilities': dict(sorted_possibilities),
            'engine_type': 'statistical'
        }
```

---

## 六、结果融合算法

### 5.1 加权融合

```java
/**
 * 结果融合
 */
public DiagnosisEngineResult fuseResults(List<EngineResult> engineResults) {
    // 各引擎权重
    Map<String, Double> weights = Map.of(
        "rule", 0.25,
        "knowledgeGraph", 0.25,
        "statistical", 0.20,
        "llm", 0.25,
        "differential", 0.05
    );
    
    // 融合分数
    Map<String, Double> fusedScores = new HashMap<>();
    
    for (EngineResult result : engineResults) {
        String engineType = result.getEngineType();
        Double weight = weights.get(engineType);
        
        if (weight == null || result.getPossibilities() == null) {
            continue;
        }
        
        // 加权累加
        for (Map.Entry<String, Double> entry : result.getPossibilities().entrySet()) {
            String disease = entry.getKey();
            Double score = entry.getValue();
            
            fusedScores.put(disease, 
                fusedScores.getOrDefault(disease, 0.0) + score * weight);
        }
    }
    
    // 归一化到0-1
    double maxScore = fusedScores.values().stream()
        .mapToDouble(Double::doubleValue)
        .max()
        .orElse(1.0);
    
    if (maxScore > 0) {
        fusedScores.replaceAll((k, v) -> v / maxScore);
    }
    
    // 排序，返回Top 5（作为候选，最终展示Top 3-5，与系统设计方案保持一致）
    List<Map.Entry<String, Double>> sorted = fusedScores.entrySet().stream()
        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
        .limit(5)
        .collect(Collectors.toList());
    
    Map<String, Double> topPossibilities = new LinkedHashMap<>();
    for (Map.Entry<String, Double> entry : sorted) {
        topPossibilities.put(entry.getKey(), entry.getValue());
    }
    
    return DiagnosisEngineResult.builder()
        .possibilities(topPossibilities)
        .engineResults(buildEngineResults(engineResults))
        .build();
}
```

---

## 七、数据冲突处理

### 6.1 冲突检测

```java
/**
 * 检测数据冲突
 */
public List<DataConflict> detectConflicts(DiagnosisRecord record) {
    List<DataConflict> conflicts = new ArrayList<>();
    
    // 1. 症状与体征冲突
    if (record.getChiefComplaint() != null && record.getChiefComplaint().contains("发热")) {
        Map<String, Object> vitalSigns = record.getVitalSigns();
        if (vitalSigns != null && vitalSigns.containsKey("temperature")) {
            Double temperature = (Double) vitalSigns.get("temperature");
            if (temperature != null && temperature < 37.0) {
                conflicts.add(new DataConflict(
                    "symptom_vital_sign",
                    "症状描述为发热，但体温正常",
                    "您自己感觉发热，还是测过体温？"
                ));
            }
        }
    }
    
    // 2. 症状与分诊等级冲突
    if (record.getChiefComplaint() != null && record.getChiefComplaint().contains("胸痛")) {
        // 如果分诊等级是L4（非紧急），但症状严重
        Integer severity = record.getSymptomSeverity();
        if (severity != null && severity >= 8) {
            conflicts.add(new DataConflict(
                "symptom_triage",
                "症状严重程度高，但分诊等级较低",
                "疼痛程度如何？有没有其他症状？"
            ));
        }
    }
    
    return conflicts;
}
```

---

## 八、与智能家庭医生的协作逻辑

### 7.1 协作流程设计

**核心公式**：
> **智能诊断 = 医学判断引擎**（回答"是不是病？是否安全？"）  
> **智能家庭医生 = 照护执行引擎**（回答"怎么管理？如何改善？"）

### 7.2 输出给智能家庭医生的数据格式

```java
/**
 * 诊断结果输出（供智能家庭医生使用）
 */
@Data
@Builder
public class DiagnosisResultForFamilyDoctor {
    /**
     * 医学判断结论
     */
    private MedicalJudgment medicalJudgment;
    
    /**
     * 给家庭医生的建议
     */
    private FamilyDoctorSuggestions suggestions;
    
    @Data
    @Builder
    public static class MedicalJudgment {
        /**
         * 判断结论
         */
        private String conclusion; // "目前不构成疾病，医学上安全" / "需要进一步检查" / "可能患有XX疾病"
        
        /**
         * 判断依据（为什么得出这个结论）
         */
        private String reason;
        
        /**
         * 风险评估（虽然没病，但有什么风险需要关注）
         */
        private String riskAssessment;
        
        /**
         * 后续建议
         */
        private FollowUpSuggestions followUpSuggestions;
        
        /**
         * 检查建议（包括"不需要做哪些检查"的建议）
         */
        private ExaminationSuggestions examinationSuggestions;
    }
    
    @Data
    @Builder
    public static class FollowUpSuggestions {
        /**
         * 是否需要复测
         */
        private Boolean needRetest;
        
        /**
         * 复测周期
         */
        private String retestCycle; // "3个月" / "6个月" / "1年"
        
        /**
         * 复测项目
         */
        private List<String> retestItems;
        
        /**
         * 什么情况下需要就医
         */
        private String whenToSeeDoctor;
    }
    
    @Data
    @Builder
    public static class ExaminationSuggestions {
        /**
         * 建议做的检查
         */
        private List<ExaminationItem> recommendedExams;
        
        /**
         * 不需要做的检查（避免过度医疗）
         */
        private List<ExaminationItem> unnecessaryExams;
        
        /**
         * 不需要做的原因
         */
        private String unnecessaryReason;
    }
    
    @Data
    @Builder
    public static class ExaminationItem {
        private String name;
        private String purpose; // "确诊" / "排除" / "评估严重程度"
        private String priority; // "high" / "medium" / "low"
        private String reason;
    }
    
    @Data
    @Builder
    public static class FamilyDoctorSuggestions {
        /**
         * 照护计划建议
         */
        private String carePlanSuggestions;
        
        /**
         * 需要关注的风险点
         */
        private List<String> riskReminders;
        
        /**
         * 用户需要了解的知识点
         */
        private List<String> userEducationNeeds;
    }
}
```

### 7.3 构建输出给智能家庭医生的结果

```java
/**
 * 构建输出给智能家庭医生的结果
 */
@Service
public class DiagnosisResultBuilder {
    
    /**
     * 构建诊断结果（供智能家庭医生使用）
     */
    public DiagnosisResultForFamilyDoctor buildResultForFamilyDoctor(
            DiagnosisEngineResult engineResult,
            DiagnosisRecord record) {
        
        // 1. 判断是否有疾病
        boolean hasDisease = checkHasDisease(engineResult);
        
        // 2. 构建医学判断
        MedicalJudgment medicalJudgment = buildMedicalJudgment(engineResult, record, hasDisease);
        
        // 3. 构建给家庭医生的建议
        FamilyDoctorSuggestions suggestions = buildFamilyDoctorSuggestions(engineResult, record, hasDisease);
        
        return DiagnosisResultForFamilyDoctor.builder()
            .medicalJudgment(medicalJudgment)
            .suggestions(suggestions)
            .build();
    }
    
    /**
     * 判断是否有疾病
     */
    private boolean checkHasDisease(DiagnosisEngineResult engineResult) {
        // 如果Top 1可能性分数 > 0.7，认为可能有疾病
        Map<String, Double> possibilities = engineResult.getPossibilities();
        if (possibilities == null || possibilities.isEmpty()) {
            return false;
        }
        
        Double topScore = possibilities.values().stream()
            .max(Double::compareTo)
            .orElse(0.0);
        
        return topScore > 0.7;
    }
    
    /**
     * 构建"没病"的医学判断（关键）
     */
    private MedicalJudgment buildNoDiseaseJudgment(
            DiagnosisEngineResult engineResult,
            DiagnosisRecord record) {
        
        // 关键：对"没病的人"给出专业的医学判断依据
        return MedicalJudgment.builder()
            .conclusion("目前不构成疾病，医学上安全")
            .reason(buildNoDiseaseReason(engineResult, record))
            .riskAssessment(buildNoDiseaseRiskAssessment(engineResult, record))
            .followUpSuggestions(buildNoDiseaseFollowUpSuggestions(engineResult, record))
            .examinationSuggestions(buildNoDiseaseExaminationSuggestions(engineResult, record))
            .build();
    }
    
    /**
     * 构建"没病"的判断依据
     */
    private String buildNoDiseaseReason(
            DiagnosisEngineResult engineResult,
            DiagnosisRecord record) {
        
        StringBuilder reason = new StringBuilder();
        
        // 1. 症状分析
        if (record.getChiefComplaint() != null) {
            reason.append("症状分析：").append(record.getChiefComplaint())
                  .append("，无典型疾病特征。");
        }
        
        // 2. 检查结果分析
        if (record.getExaminationResults() != null && !record.getExaminationResults().isEmpty()) {
            reason.append("检查结果：各项指标均在正常范围或轻度异常，未达到疾病诊断标准。");
        }
        
        // 3. 体征分析
        if (record.getVitalSigns() != null) {
            reason.append("生命体征：正常。");
        }
        
        return reason.toString();
    }
}
```

---

## 九、对"没病的人"的处理逻辑

### 9.1 处理流程（更新版）

**关键理解**：
> 智能诊断对"没病的人"做的不是"告诉你没病"，而是**"用医学方法证明你没病，并告诉你为什么安全"**。

这是一个**主动的医学判断过程**，而不是被动的"没发现问题"。

### 9.2 判断"没病"的业务逻辑（更新版）

#### 9.2.1 判断"没病"的逻辑

```java
/**
 * 判断是否构成疾病
 */
@Service
public class DiseaseJudgmentService {
    
    /**
     * 判断是否构成疾病
     */
    public DiseaseJudgmentResult judgeDisease(DiagnosisEngineResult engineResult, DiagnosisRecord record) {
        
        // 1. 检查可能性分数
        Map<String, Double> possibilities = engineResult.getPossibilities();
        if (possibilities == null || possibilities.isEmpty()) {
            return DiseaseJudgmentResult.builder()
                .hasDisease(false)
                .reason("无疾病可能性")
                .build();
        }
        
        // 2. 检查Top 1可能性分数
        Double topScore = possibilities.values().stream()
            .max(Double::compareTo)
            .orElse(0.0);
        
        // 3. 如果Top 1可能性分数 < 0.5，认为不构成疾病
        if (topScore < 0.5) {
            return DiseaseJudgmentResult.builder()
                .hasDisease(false)
                .reason("所有疾病可能性均较低（<0.5），不构成疾病")
                .build();
        }
        
        // 4. 检查是否达到疾病诊断标准
        String topDisease = possibilities.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        
        if (topDisease != null) {
            boolean meetsDiagnosticCriteria = checkMeetsDiagnosticCriteria(topDisease, record);
            if (!meetsDiagnosticCriteria) {
                return DiseaseJudgmentResult.builder()
                    .hasDisease(false)
                    .reason("未达到" + topDisease + "的诊断标准")
                    .build();
            }
        }
        
        // 5. 如果都通过，认为可能有疾病
        return DiseaseJudgmentResult.builder()
            .hasDisease(true)
            .reason("存在疾病可能性，建议进一步检查")
            .build();
    }
}
```

---

---

## 十、总结

本文档详细设计了AI医生系统的核心业务逻辑，包括：

1. **诊断流程五个阶段的业务逻辑**：
   - 阶段1：主动询问（形成结构化问题清单、概念归一化）
   - 阶段2：信息补全（信息缺口分级、智能追问策略）
   - 阶段3：可能性分析（五引擎融合、三层分层）
   - 阶段4：鉴别诊断（证据分析、验证计划）
   - 阶段5：结果输出（终点结论包、证据回填）

2. **智能追问算法**：基于信息缺口分级的追问策略（必填/重要/可选）

3. **信息完整度计算**：基于结构化问题清单的完整度计算算法

4. **三层分层诊断**：首要假设/主要备选/必须排除的业务逻辑

5. **证据分析**：支持/反对证据分析、证据强度分级

6. **终点结论包**：四要素构建逻辑（结论、必须排除项状态、关键依据、行动与随访）

7. **规则引擎**：规则匹配算法、规则库结构

8. **知识图谱查询**：Cypher查询逻辑、用户画像过滤

9. **统计模型推理**：特征提取、模型推理

10. **结果融合算法**：五引擎加权融合

11. **数据冲突处理**：冲突检测与解决

12. **与智能家庭医生的协作**：输出数据格式、构建逻辑

13. **对"没病的人"的处理**：专业医学判断逻辑

**核心特点**：
- ✅ **结构化流程**：五个阶段的完整业务逻辑设计
- ✅ **信息缺口分级**：必填/重要/可选分级，智能追问策略
- ✅ **三层分层诊断**：确保高危诊断不遗漏
- ✅ **证据驱动**：支持/反对证据分析，证据强度分级
- ✅ **终点结论包**：可行动、可审计的诊断结论

---

**文档版本**：v3.0（基于DR.KNOWS的单主Agent + 多工具Tools架构）  
**创建日期**：2025年1月  
**更新日期**：2025年1月  
**文档定位**：AI医生系统的业务逻辑详细设计（工具的工作流程、DR.KNOWS核心方法、CDP管理等）  
**参考文档**：《AI医生系统-系统功能设计.md》、《AI医生系统-技术架构设计.md》  
**设计基础**：基于DR.KNOWS论文，采用单主Agent + 多工具Tools架构设计  
**更新说明**：根据DR.KNOWS设计，更新为工具的工作流程，添加DR.KNOWS核心方法（知识图谱路径检索、路径评分排序、路径注入LLM），添加CDP管理业务逻辑，整合健康管理态和临床诊疗态两种工作态。

