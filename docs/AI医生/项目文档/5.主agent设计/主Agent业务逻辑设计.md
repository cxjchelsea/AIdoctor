# AI医生系统 - 主Agent业务逻辑设计

> **文档定位**：本文档详细设计AI医生系统的主Agent业务逻辑，包括运行循环业务流程、5步诊断路径业务流程、工具调用流程和业务规则。这是理解主Agent业务逻辑的必读文档。  
> **相关文档**：
> - 主Agent运行循环设计：请参考《主Agent运行循环设计.md》
> - 主Agent技术实现：请参考《主Agent技术实现.md》
> - 主Agent核心算法设计：请参考《主Agent核心算法设计.md》
> - 工具调用协议：请参考《7.接口规范/工具调用协议.md》
> - 临床诊疗态流程设计：请参考《3.业务功能设计/临床诊疗态流程设计.md》

---

## 一、主Agent业务逻辑概述

### 1.1 业务逻辑定位

**主Agent业务逻辑定位**：
- **运行循环业务流程**：主Agent通过运行循环（Observe→Plan→Act→Update→Evaluate→Stop/Escalate）自主调用工具，完成诊断流程
- **5步诊断路径业务流程**：临床诊疗态采用5步AI循证诊断流程，确保诊断过程可推理、可复用、可审计
- **工具调用流程**：主Agent根据CDP状态和AgentState规划工具调用，执行工具，更新CDP
- **业务规则**：主Agent遵循一系列业务规则，确保诊断过程的安全性和有效性

### 1.2 业务逻辑特点

**业务逻辑特点**：
- **自主决策**：主Agent在运行循环中自主决定调用哪些工具、调用顺序、调用参数
- **动态适应**：主Agent根据CDP状态和工具结果动态调整策略
- **可追溯**：所有业务逻辑执行都记录到AuditTrail，支持问题定位和系统优化
- **可回退**：主Agent可以根据证据冲突或症状演变回退到之前的步骤

---

## 二、主Agent运行循环业务流程

### 2.1 运行循环业务流程概述

**运行循环业务流程**：

主Agent通过运行循环不断观察CDP状态、规划工具调用、执行工具、更新CDP、评估结果、决定停止/升级/继续。

**运行循环步骤**：

```
1. Observe（观察）
   - 读取当前CDP状态
   - 读取AgentState
   - 识别信息缺口
   - 识别证据冲突
   - 识别风险信号

2. Plan（规划）
   - 根据当前CDP状态和AgentState规划工具调用
   - 决定调用哪些工具、调用顺序、调用参数
   - 考虑约束（成本/时间/风险）
   - 考虑已尝试工具和失败回退策略

3. Act（执行）
   - 生成ToolContext
   - 调用工具（同步/异步）
   - 等待工具返回ToolResult
   - 记录到AuditTrail

4. Update（更新）
   - 评估ToolResult的quality和evidence
   - 进行evidence fusion和conflict resolution
   - 决定是否写回CDP（根据suggested_writes）
   - 更新AgentState（tried_tools、current_step等）
   - 记录到AuditTrail

5. Evaluate（评估）
   - 评估停止条件（StopCondition）
   - 评估升级条件（Escalation）
   - 评估拒答条件（Refusal）
   - 评估是否需要继续循环

6. Stop/Escalate/Continue（停止/升级/继续）
   - 如果满足停止条件 → Stop，输出终点结论包
   - 如果满足升级条件 → Escalate，执行升级策略
   - 如果满足拒答条件 → Refuse，输出拒答提示
   - 否则 → Continue，回到Observe步骤
```

### 2.2 Observe（观察）业务流程

**Observe步骤业务逻辑**：

```java
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
```

**业务规则**：
- **信息缺口识别**：通过`cdp.uncertainty.missing_critical_info`识别缺失的关键信息
- **证据冲突识别**：通过`agent_state.evidence_fusion_state.conflicts`识别证据冲突
- **风险信号识别**：通过`cdp.triage.red_flags`识别风险信号

### 2.3 Plan（规划）业务流程

**Plan步骤业务逻辑**：

```java
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
```

**业务规则**：
- **优先级规则**：健康状态判定 > 病例理解 > 信息缺口识别 > 鉴别诊断
- **约束考虑**：考虑成本/时间/风险约束
- **失败回退**：考虑已尝试工具和失败回退策略

### 2.4 Act（执行）业务流程

**Act步骤业务逻辑**：

```java
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
```

**业务规则**：
- **ToolContext生成**：必须包含CDP引用、AgentState摘要、约束条件、调用参数
- **工具调用**：通过REST API或内部调用工具
- **审计记录**：所有工具调用都记录到AuditTrail

### 2.5 Update（更新）业务流程

**Update步骤业务逻辑**：

```java
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
```

**业务规则**：
- **质量评估**：ToolResult的confidence必须达到阈值才能写回CDP
- **证据融合**：进行evidence fusion和conflict resolution
- **写回决策**：根据suggested_writes决定是否写回CDP
- **状态更新**：更新AgentState的tried_tools、current_step等字段

### 2.6 Evaluate（评估）业务流程

**Evaluate步骤业务逻辑**：

```java
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
```

**业务规则**：
- **停止条件评估**：所有停止条件必须同时满足才能停止
- **升级条件评估**：升级条件优先级最高，可以中断任何步骤
- **拒答条件评估**：拒答条件定义了系统的安全边界

### 2.7 Stop/Escalate/Continue业务流程

**Stop/Escalate/Continue步骤业务逻辑**：

```java
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
```

**业务规则**：
- **Stop**：如果满足停止条件，停止运行循环，输出终点结论包
- **Escalate**：如果满足升级条件，执行升级策略，输出升级提示
- **Refuse**：如果满足拒答条件，输出拒答提示
- **Continue**：否则继续运行循环，回到Observe步骤

---

## 三、5步AI循证诊断流程详细业务逻辑

### 3.1 流程总览

**5步AI循证诊断流程**：

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

### 3.2 Step 1：识别问题

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

**工具调用**：
- **tool_1（病例理解工具）**：概念归一化、形成问题清单
- **tool_2（主动问诊工具）**：标记信息缺口

### 3.3 Step 2：构建鉴别诊断候选集并分层

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

**工具调用**：
- **tool_3（鉴别诊断工具）**：生成鉴别诊断全集
- **tool_6（风险评估工具）**：三层分层（结合风险等级）

### 3.4 Step 3：组织候选集并建立分流路径

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

**工具调用**：
- **tool_3（鉴别诊断工具）**：组织推理子组、提炼关键差异点
- **tool_2（主动问诊工具）**：形成分流路径清单

### 3.5 Step 4：采集关键证据并形成排序与验证计划

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

**工具调用**：
- **tool_2（主动问诊工具）**：采集关键证据
- **tool_3（鉴别诊断工具）**：固化三层排序
- **tool_4（检查建议工具）**：制定验证计划

### 3.6 Step 5：回填证据并输出终点结论包

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

**工具调用**：
- **tool_1（病例理解工具）**：证据回填
- **tool_3（鉴别诊断工具）**：更新三层排序
- **tool_4（检查建议工具）**：回填检查结果
- **tool_5（治疗建议工具）**：生成治疗方案
- **tool_6（风险评估工具）**：风险评估
- **tool_7（证据链工具）**：生成终点结论包

---

## 四、工具调用流程

### 4.1 工具调用流程概述

**工具调用流程**：

主Agent在运行循环中根据CDP状态和AgentState规划工具调用，执行工具，更新CDP。

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

### 4.2 工具调用业务规则

**工具调用业务规则**：

1. **调用顺序规则**：
   - 健康状态判定（tool_0）必须在最前面
   - 病例理解（tool_1）必须在临床诊疗态开始时调用
   - 鉴别诊断（tool_3）必须在病例理解完成后调用
   - 检查建议（tool_4）和治疗建议（tool_5）必须在鉴别诊断完成后调用

2. **调用条件规则**：
   - 如果信息缺口存在，必须调用主动问诊工具（tool_2）
   - 如果DDx未生成，必须调用鉴别诊断工具（tool_3）
   - 如果风险等级为L1/L2，必须立即调用风险评估工具（tool_6）

3. **失败回退规则**：
   - 如果工具调用失败，主Agent记录失败信息，尝试调用备用工具或触发升级
   - 如果关键工具连续失败3次以上，触发拒答

---

## 五、业务规则总结

### 5.1 运行循环业务规则

1. **Observe规则**：
   - 必须读取CDP的所有关键字段
   - 必须识别信息缺口、证据冲突、风险信号

2. **Plan规则**：
   - 必须根据CDP状态和AgentState规划工具调用
   - 必须考虑约束（成本/时间/风险）
   - 必须考虑已尝试工具和失败回退策略

3. **Act规则**：
   - 必须生成完整的ToolContext
   - 必须记录所有工具调用到AuditTrail

4. **Update规则**：
   - 必须评估ToolResult的quality和evidence
   - 必须进行evidence fusion和conflict resolution
   - 必须根据suggested_writes决定是否写回CDP

5. **Evaluate规则**：
   - 必须评估停止条件、升级条件、拒答条件
   - 必须根据评估结果决定下一步行动

### 5.2 5步诊断路径业务规则

1. **Step 1规则**：
   - 必须完成概念归一化
   - 必须形成完整问题清单
   - 必须标记信息缺口

2. **Step 2规则**：
   - 必须生成鉴别诊断全集
   - 必须进行三层分层
   - 必须标注入选依据

3. **Step 3规则**：
   - 必须组织成推理子组
   - 必须提炼关键差异点
   - 必须形成分流路径清单

4. **Step 4规则**：
   - 必须采集关键证据
   - 必须固化三层排序
   - 必须制定验证计划

5. **Step 5规则**：
   - 必须回填证据
   - 必须更新三层排序
   - 必须输出终点结论包
   - 必须检查回退条件

### 5.3 工具调用业务规则

1. **调用顺序规则**：必须遵循工具调用的优先级和依赖关系
2. **调用条件规则**：必须满足工具调用的前置条件
3. **失败回退规则**：必须处理工具调用失败的情况

---

## 六、参考文档

### 6.1 架构设计文档
- 《2.架构设计/主Agent架构设计.md》：主Agent架构详细设计
- 《2.架构设计/工具系统架构.md》：工具系统架构详细设计

### 6.2 主Agent设计文档
- 《主Agent运行循环设计.md》：主Agent运行循环详细设计
- 《主Agent技术实现.md》：主Agent技术实现详细设计
- 《主Agent核心算法设计.md》：主Agent核心算法详细设计

### 6.3 业务功能设计文档
- 《3.业务功能设计/临床诊疗态流程设计.md》：临床诊疗态流程详细设计
- 《3.业务功能设计/健康管理态流程设计.md》：健康管理态流程详细设计

### 6.4 工具设计文档
- 《4.工具设计/工具接口规范.md》：工具接口规范详细设计
- 《4.工具设计/工具业务逻辑设计.md》：工具业务逻辑详细设计

### 6.5 接口规范文档
- 《7.接口规范/工具调用协议.md》：工具调用协议详细设计

---

**文档来源**：
- 原文档：《3.项目前置设计/AI医生系统-业务逻辑详细设计.md》1.10 主Agent运行循环与工具调用流程（第1770-2067行）
- 原文档：《3.项目前置设计/AI医生系统-业务逻辑详细设计.md》1.11 5步AI循证诊断流程详细业务逻辑（第2068-2342行）
- 创建时间：2025-01-22
- 文档版本：v1.0

