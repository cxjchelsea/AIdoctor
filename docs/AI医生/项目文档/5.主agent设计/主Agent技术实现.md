# AI医生系统 - 主Agent技术实现

> **文档定位**：本文档详细设计AI医生系统的主Agent技术实现，包括运行循环技术实现、流程编排技术实现、状态管理技术实现等。这是开发主Agent时的技术实现参考文档。  
> **相关文档**：
> - 主Agent运行循环设计：请参考《主Agent运行循环设计.md》
> - 主Agent业务逻辑设计：请参考《主Agent业务逻辑设计.md》
> - 主Agent核心算法设计：请参考《主Agent核心算法设计.md》
> - 工具调用协议：请参考《7.接口规范/工具调用协议.md》

---

## 一、主Agent技术实现概述

### 1.1 技术实现定位

**主Agent技术实现定位**：
- **运行循环技术实现**：主Agent运行循环的技术实现，包括数据库设计、代码结构
- **流程编排技术实现**：5步默认诊断路径的流程编排技术实现
- **状态管理技术实现**：CDP和AgentState的状态管理技术实现

### 1.2 技术实现特点

**技术实现特点**：
- **可推理**：每个步骤都有明确的推理逻辑和依据
- **可复用**：问题清单、候选集、分流路径等可沉淀为模板库
- **可审计**：每个步骤的输出都有结构化记录，支持追溯和验证
- **可回退**：支持回退到任意步骤，确保系统不会在错误结论上"锁死"
- **动态插入**：支持红旗优先、冲突复核、证据不足触发检索等动态插入策略

---

## 二、主Agent运行循环技术实现

### 2.1 运行循环技术实现概述

**主Agent运行循环**：
- **Observe**：读取CDP状态、AgentState，识别信息缺口、证据冲突、风险信号
- **Plan**：根据当前状态规划工具调用（调用哪些工具、调用顺序、调用参数）
- **Act**：生成ToolContext，调用工具，等待ToolResult
- **Update**：评估ToolResult，进行evidence fusion和conflict resolution，决定是否写回CDP，更新AgentState
- **Evaluate**：评估停止条件、升级条件、拒答条件
- **Stop/Escalate/Continue**：如果满足停止条件→Stop，如果满足升级条件→Escalate，如果满足拒答条件→Refuse，否则→Continue

**5步默认诊断路径**：
- **Step 1**：识别问题（调用tool_1、tool_2）
- **Step 2**：构建鉴别诊断候选集并分层（调用tool_3、tool_6）
- **Step 3**：组织候选集并建立分流路径（调用tool_3）
- **Step 4**：采集关键证据并形成排序与验证计划（调用tool_2、tool_3、tool_4）
- **Step 5**：回填证据并输出终点结论包（调用tool_4、tool_3、tool_5、tool_7）

**流程编排特点**：
- **可推理**：每个步骤都有明确的推理逻辑和依据
- **可复用**：问题清单、候选集、分流路径等可沉淀为模板库
- **可审计**：每个步骤的输出都有结构化记录，支持追溯和验证
- **可回退**：支持回退到任意步骤，确保系统不会在错误结论上"锁死"
- **动态插入**：支持红旗优先、冲突复核、证据不足触发检索等动态插入策略

### 2.2 数据库设计

**诊断流程状态表**：

```sql
-- 诊断流程状态表
CREATE TABLE diagnosis_workflow_state (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64),
    current_step INT,  -- 1-5
    step_status VARCHAR(32),  -- pending/in_progress/completed
    step_output JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_cdp_id (cdp_id),
    INDEX idx_current_step (current_step)
);
```

**诊断流程回退记录表**：

```sql
-- 诊断流程回退记录表
CREATE TABLE diagnosis_workflow_rollback (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64),
    from_step INT,
    to_step INT,
    rollback_reason TEXT,
    rollback_condition VARCHAR(255),
    timestamp TIMESTAMP,
    INDEX idx_cdp_id (cdp_id)
);
```

### 2.3 代码结构设计

**主Agent运行循环服务类结构**：

```java
/**
 * 主Agent运行循环服务
 */
@Service
public class ClinicalAgentBrainService {
    
    @Autowired
    private CDPManager cdpManager;
    
    @Autowired
    private AgentStateManager agentStateManager;
    
    @Autowired
    private ToolCaller toolCaller;
    
    @Autowired
    private EvidenceFusionService evidenceFusionService;
    
    @Autowired
    private StopConditionEvaluator stopConditionEvaluator;
    
    @Autowired
    private EscalationHandler escalationHandler;
    
    @Autowired
    private RefusalHandler refusalHandler;
    
    @Autowired
    private AuditTrailManager auditTrailManager;
    
    /**
     * 主Agent运行循环
     */
    public DiagnosisResult runAgentLoop(UserInput input) {
        // 实现逻辑
    }
    
    /**
     * Observe（观察）
     */
    private CDPState observe(CDP cdp, AgentState agentState) {
        // 实现逻辑
    }
    
    /**
     * Plan（规划）
     */
    private ToolCallPlan plan(CDPState cdpState, AgentState agentState) {
        // 实现逻辑
    }
    
    /**
     * Act（执行）
     */
    private ToolResult act(ToolCallPlan plan, CDP cdp, AgentState agentState) {
        // 实现逻辑
    }
    
    /**
     * Update（更新）
     */
    private void update(CDP cdp, AgentState agentState, ToolResult result) {
        // 实现逻辑
    }
    
    /**
     * Evaluate（评估）
     */
    private Decision evaluate(CDP cdp, AgentState agentState) {
        // 实现逻辑
    }
}
```

---

## 三、流程编排技术实现

### 3.1 流程编排技术实现概述

**流程编排技术实现**：

主Agent通过流程编排技术实现5步默认诊断路径的执行，支持动态插入策略和回退机制。

### 3.2 流程编排服务设计

**流程编排服务类结构**：

```java
/**
 * 诊断流程编排服务
 */
@Service
public class DiagnosisOrchestrationService {
    
    @Autowired
    private ClinicalAgentBrainService agentBrainService;
    
    @Autowired
    private Step1IdentifyProblemService step1Service;
    
    @Autowired
    private Step2BuildDDxCandidatesService step2Service;
    
    @Autowired
    private Step3OrganizeRoutingPathService step3Service;
    
    @Autowired
    private Step4CollectEvidenceAndPlanService step4Service;
    
    @Autowired
    private Step5BackfillAndConcludeService step5Service;
    
    @Autowired
    private RollbackEngine rollbackEngine;
    
    /**
     * 完整诊断流程
     */
    public DiagnosisResult diagnose(UserInput input) {
        String sessionId = generateSessionId();
        
        // 创建CDP和AgentState
        CDP cdp = cdpManager.createCDP(input.getPatientId(), sessionId);
        AgentState agentState = agentStateManager.createAgentState(sessionId, cdp.getId());
        
        // 主Agent运行循环
        ClinicalAgentBrain agent = new ClinicalAgentBrain();
        DiagnosisResult result = agent.runAgentLoop(input);
        
        // 如果工作态为健康管理态，直接返回
        if (result.getWorkMode() == WorkMode.WELLNESS_MODE) {
            return result;
        }
        
        // 临床诊疗态：执行5步诊断路径
        try {
            // Step 1: 识别问题
            StructuredQuestionList questionList = step1Service.identifyProblem(cdp, input.getUserInput());
            
            // Step 2: 构建鉴别诊断候选集并分层
            ThreeLayerResult threeLayerResult = step2Service.buildDDxCandidates(cdp, questionList);
            
            // Step 3: 组织候选集并建立分流路径
            RoutingPath routingPath = step3Service.organizeRoutingPath(cdp, threeLayerResult);
            
            // Step 4: 采集关键证据并形成排序与验证计划
            VerificationPlan verificationPlan = step4Service.collectEvidenceAndPlan(cdp, routingPath);
            
            // Step 5: 回填证据并输出终点结论包
            ConclusionPackage conclusionPackage = step5Service.backfillAndConclude(cdp, verificationPlan);
            
            return DiagnosisResult.builder()
                .cdpId(cdp.getId())
                .conclusionPackage(conclusionPackage)
                .build();
                
        } catch (RollbackException e) {
            // 触发回退
            cdp = rollbackEngine.executeRollback(e.getRollbackCondition(), cdp);
            // 重新执行流程
            return diagnose(input);
        }
    }
}
```

### 3.3 动态插入策略技术实现

**动态插入策略技术实现**：

```java
/**
 * 动态插入策略处理器
 */
@Service
public class DynamicInsertionStrategyHandler {
    
    /**
     * 红旗优先策略
     */
    public boolean handleRedFlagPriority(CDP cdp, AgentState agentState) {
        // 检查是否有红旗信号
        if (cdp.getTriage() != null && cdp.getTriage().getRedFlags() != null) {
            List<RedFlag> redFlags = cdp.getTriage().getRedFlags();
            for (RedFlag flag : redFlags) {
                if (flag.getRiskLevel() == RiskLevel.L1 || flag.getRiskLevel() == RiskLevel.L2) {
                    // 立即中断当前步骤，执行升级策略
                    escalationHandler.escalate(cdp, agentState, EscalationReason.RED_FLAG);
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 冲突复核策略
     */
    public boolean handleConflictReview(CDP cdp, AgentState agentState) {
        // 检查是否有证据冲突
        if (agentState.getEvidenceFusionState() != null 
            && !agentState.getEvidenceFusionState().getConflicts().isEmpty()) {
            // 进行冲突复核
            ConflictResolutionResult resolution = conflictResolver.resolve(cdp, agentState);
            if (!resolution.isResolved()) {
                // 如果冲突无法解决，触发升级或拒答
                escalationHandler.escalate(cdp, agentState, EscalationReason.CONFLICT_UNRESOLVED);
                return true;
            }
        }
        return false;
    }
    
    /**
     * 证据不足触发检索策略
     */
    public boolean handleInsufficientEvidenceRetrieval(CDP cdp, AgentState agentState) {
        // 检查证据是否充足
        if (cdp.getEvidenceGraph() == null || cdp.getEvidenceGraph().getEvidenceCount() < 2) {
            // 触发扩展检索
            diagnosisEngineService.expandRetrieval(cdp);
            return true;
        }
        return false;
    }
}
```

### 3.4 回退机制技术实现

**回退机制技术实现**：

```java
/**
 * 回退引擎
 */
@Service
public class RollbackEngine {
    
    @Autowired
    private CDPManager cdpManager;
    
    @Autowired
    private AgentStateManager agentStateManager;
    
    /**
     * 检查回退条件
     */
    public List<RollbackCondition> checkRollbackConditions(CDP cdp) {
        List<RollbackCondition> conditions = new ArrayList<>();
        
        // 检查证据冲突
        if (hasEvidenceConflict(cdp)) {
            conditions.add(RollbackCondition.builder()
                .type(RollbackType.EVIDENCE_CONFLICT)
                .targetStep(3)  // 回退到Step 3
                .reason("证据冲突，需要重新组织候选集")
                .build());
        }
        
        // 检查症状演变
        if (hasSymptomEvolution(cdp)) {
            conditions.add(RollbackCondition.builder()
                .type(RollbackType.SYMPTOM_EVOLUTION)
                .targetStep(1)  // 回退到Step 1
                .reason("症状演变，需要重新识别问题")
                .build());
        }
        
        // 检查处理无效
        if (hasTreatmentIneffective(cdp)) {
            conditions.add(RollbackCondition.builder()
                .type(RollbackType.TREATMENT_INEFFECTIVE)
                .targetStep(4)  // 回退到Step 4
                .reason("处理无效，需要重新采集证据")
                .build());
        }
        
        return conditions;
    }
    
    /**
     * 执行回退
     */
    public CDP executeRollback(RollbackCondition condition, CDP cdp) {
        // 记录回退
        diagnosisWorkflowRollbackRepository.save(DiagnosisWorkflowRollback.builder()
            .cdpId(cdp.getId())
            .fromStep(cdp.getCurrentStep())
            .toStep(condition.getTargetStep())
            .rollbackReason(condition.getReason())
            .rollbackCondition(condition.getType().name())
            .timestamp(LocalDateTime.now())
            .build());
        
        // 恢复CDP到目标步骤的状态
        CDP restoredCDP = cdpManager.restoreCDPToStep(cdp.getId(), condition.getTargetStep());
        
        // 更新AgentState
        agentStateManager.updateAgentState(cdp.getId(), Map.of(
            "current_step", condition.getTargetStep(),
            "rollback_count", getRollbackCount(cdp) + 1
        ));
        
        return restoredCDP;
    }
}
```

---

## 四、状态管理技术实现

### 4.1 CDP状态管理技术实现

**CDP状态管理技术实现**：

```java
/**
 * CDP管理器
 */
@Service
public class CDPManager {
    
    @Autowired
    private CDPRepository cdpRepository;
    
    @Autowired
    private CDPVersionManager cdpVersionManager;
    
    /**
     * 创建CDP
     */
    public CDP createCDP(String patientId, String sessionId) {
        CDP cdp = CDP.builder()
            .id(generateCDPId())
            .patientId(patientId)
            .sessionId(sessionId)
            .version(1)
            .status(CDPStatus.INITIALIZED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        cdpRepository.save(cdp);
        cdpVersionManager.createVersion(cdp);
        
        return cdp;
    }
    
    /**
     * 更新CDP
     */
    public CDP updateCDP(String cdpId, Map<String, Object> updates) {
        CDP cdp = cdpRepository.findById(cdpId)
            .orElseThrow(() -> new CDPNotFoundException(cdpId));
        
        // 更新字段
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            updateField(cdp, entry.getKey(), entry.getValue());
        }
        
        // 版本管理
        cdp.setVersion(cdp.getVersion() + 1);
        cdp.setUpdatedAt(LocalDateTime.now());
        
        cdpRepository.save(cdp);
        cdpVersionManager.createVersion(cdp);
        
        return cdp;
    }
    
    /**
     * 恢复CDP到指定步骤
     */
    public CDP restoreCDPToStep(String cdpId, int targetStep) {
        CDP cdp = cdpRepository.findById(cdpId)
            .orElseThrow(() -> new CDPNotFoundException(cdpId));
        
        // 从版本历史中恢复
        CDPVersion targetVersion = cdpVersionManager.getVersionByStep(cdpId, targetStep);
        CDP restoredCDP = targetVersion.toCDP();
        
        restoredCDP.setVersion(cdp.getVersion() + 1);
        restoredCDP.setUpdatedAt(LocalDateTime.now());
        
        cdpRepository.save(restoredCDP);
        cdpVersionManager.createVersion(restoredCDP);
        
        return restoredCDP;
    }
}
```

### 4.2 AgentState状态管理技术实现

**AgentState状态管理技术实现**：

```java
/**
 * AgentState管理器
 */
@Service
public class AgentStateManager {
    
    @Autowired
    private AgentStateRepository agentStateRepository;
    
    /**
     * 创建AgentState
     */
    public AgentState createAgentState(String sessionId, String cdpId) {
        AgentState agentState = AgentState.builder()
            .id(generateAgentStateId())
            .sessionId(sessionId)
            .cdpId(cdpId)
            .currentStep(0)
            .workMode(WorkMode.UNKNOWN)
            .triedTools(new ArrayList<>())
            .stopConditions(StopConditions.builder().build())
            .constraints(Constraints.builder().build())
            .thresholds(Thresholds.builder().build())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        agentStateRepository.save(agentState);
        
        return agentState;
    }
    
    /**
     * 更新AgentState
     */
    public AgentState updateAgentState(String agentStateId, Map<String, Object> updates) {
        AgentState agentState = agentStateRepository.findById(agentStateId)
            .orElseThrow(() -> new AgentStateNotFoundException(agentStateId));
        
        // 更新字段
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            updateField(agentState, entry.getKey(), entry.getValue());
        }
        
        agentState.setUpdatedAt(LocalDateTime.now());
        
        agentStateRepository.save(agentState);
        
        return agentState;
    }
}
```

---

## 五、技术实现总结

### 5.1 运行循环技术实现总结

1. **数据库设计**：
   - 诊断流程状态表：记录当前步骤、步骤状态、步骤输出
   - 诊断流程回退记录表：记录回退历史

2. **代码结构**：
   - ClinicalAgentBrainService：主Agent运行循环服务
   - 各个步骤服务：Step1-Step5服务
   - 动态插入策略处理器：处理红旗优先、冲突复核、证据不足触发检索

### 5.2 流程编排技术实现总结

1. **流程编排服务**：
   - DiagnosisOrchestrationService：诊断流程编排服务
   - 支持5步默认诊断路径的执行
   - 支持动态插入策略和回退机制

2. **回退机制**：
   - RollbackEngine：回退引擎
   - 支持回退到任意步骤
   - 记录回退历史

### 5.3 状态管理技术实现总结

1. **CDP状态管理**：
   - CDPManager：CDP管理器
   - 支持CDP的创建、更新、版本管理
   - 支持CDP恢复到指定步骤

2. **AgentState状态管理**：
   - AgentStateManager：AgentState管理器
   - 支持AgentState的创建、更新
   - 记录主Agent的运行状态

---

## 六、参考文档

### 6.1 架构设计文档
- 《2.架构设计/主Agent架构设计.md》：主Agent架构详细设计
- 《2.架构设计/工具系统架构.md》：工具系统架构详细设计

### 6.2 主Agent设计文档
- 《主Agent运行循环设计.md》：主Agent运行循环详细设计
- 《主Agent业务逻辑设计.md》：主Agent业务逻辑详细设计
- 《主Agent核心算法设计.md》：主Agent核心算法详细设计

### 6.3 数据模型设计文档
- 《6.数据模型设计/CDP数据结构设计.md》：CDP数据结构详细设计
- 《6.数据模型设计/AgentState数据结构设计.md》：AgentState数据结构详细设计

### 6.4 接口规范文档
- 《7.接口规范/工具调用协议.md》：工具调用协议详细设计

---

**文档来源**：
- 原文档：《2.功能与技术设计/AI医生系统-技术架构设计-核心技术组件.md》3.3 主Agent运行循环与5步默认诊断路径（第735-791行）
- 创建时间：2025-01-22
- 文档版本：v1.0

