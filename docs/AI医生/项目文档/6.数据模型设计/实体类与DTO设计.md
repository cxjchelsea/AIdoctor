# AI医生系统 - 实体类与DTO设计

> **文档定位**：本文档详细定义AI医生系统的实体类（Entity）和DTO类（Data Transfer Object）设计，包括所有实体类和DTO类的字段定义、数据类型、约束条件等。这是开发数据模型时的必读文档。  
> **相关文档**：
> - CDP数据结构设计：请参考《CDP数据结构设计.md》
> - AgentState数据结构设计：请参考《AgentState数据结构设计.md》
> - AuditTrail数据结构设计：请参考《AuditTrail数据结构设计.md》

---

## 一、实体类（Entity）设计概述

### 1.1 实体类设计原则

**实体类设计原则**：
- **JPA注解**：使用JPA注解定义实体类和数据库表的映射关系
- **JSON字段**：复杂数据结构使用JSONB类型存储
- **版本控制**：支持版本控制和历史记录
- **审计字段**：包含创建时间、更新时间等审计字段

### 1.2 实体类列表

**核心实体类**：
1. **CDP实体**：临床决策包，系统的核心数据结构
2. **AgentState实体**：主Agent策略状态
3. **AuditTrail实体**：审计轨迹
4. **CDPVersion实体**：CDP版本记录
5. **DiagnosisRecord实体**：诊断记录
6. **HealthStateAssessmentRecord实体**：健康状态判定记录
7. **WellnessScreeningRecord实体**：健康筛查记录
8. **FollowUpPlan实体**：随访计划
9. **ExaminationRecord实体**：检查记录
10. **ExaminationPlan实体**：检查方案

---

## 二、核心实体类设计

### 2.1 CDP实体（Clinical Decision Package）

**对应表**：`cdp`

> **说明**：CDP是AI医生系统的核心数据结构，贯穿整个诊断流程。基于DR.KNOWS设计，支持版本控制和可追溯性。

**实体类定义**：

```java
package com.aidoctor.diagnosis.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CDP（Clinical Decision Package）实体
 * 临床决策包 - 系统的核心数据结构
 */
@Entity
@Table(name = "cdp", indexes = {
    @Index(name = "idx_patient_id", columnList = "patient_id"),
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_version", columnList = "version"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CDP {
    
    /**
     * CDP ID（主键）
     */
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;
    
    /**
     * 患者ID
     */
    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;
    
    /**
     * 会话ID
     */
    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;
    
    /**
     * 版本号（支持版本控制）
     */
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * CDP状态
     * - initial：CDP刚创建，等待健康状态判定
     * - wellness_mode：健康管理态（A路径）
     * - clinical_mode_collecting：临床诊疗态-信息收集
     * - clinical_mode_diagnosing：临床诊疗态-诊断中
     * - clinical_mode_managing：临床诊疗态-处置中
     * - completed：诊断流程完成（已输出终点结论包）
     * - follow_up：随访状态
     */
    @Column(name = "cdp_status", length = 64)
    private String cdpStatus;
    
    /**
     * 健康状态判定结果（JSON格式）
     * tool_0（健康状态判定工具）的输出
     */
    @Type(type = "jsonb")
    @Column(name = "health_state_assessment", columnDefinition = "jsonb")
    private Map<String, Object> healthStateAssessment;
    
    /**
     * 健康管理计划（JSON格式）
     * 健康管理态使用
     */
    @Type(type = "jsonb")
    @Column(name = "wellness_plan", columnDefinition = "jsonb")
    private Map<String, Object> wellnessPlan;
    
    /**
     * 患者状态（JSON格式）
     * 包含：症状、体征、背景信息等
     */
    @Type(type = "jsonb")
    @Column(name = "patient_state", columnDefinition = "jsonb")
    private Map<String, Object> patientState;
    
    /**
     * 鉴别诊断列表（三层排序：首要假设/主要备选/必须排除）
     */
    @Type(type = "jsonb")
    @Column(name = "ddx", columnDefinition = "jsonb")
    private List<Map<String, Object>> ddx;
    
    /**
     * 证据图（JSON格式）
     * 包含：支持证据、反对证据、证据链等
     */
    @Type(type = "jsonb")
    @Column(name = "evidence_graph", columnDefinition = "jsonb")
    private List<Map<String, Object>> evidenceGraph;
    
    /**
     * 检查计划（JSON格式）
     * tool_4（检查建议工具）的输出
     */
    @Type(type = "jsonb")
    @Column(name = "workup_plan", columnDefinition = "jsonb")
    private List<Map<String, Object>> workupPlan;
    
    /**
     * 治疗计划（JSON格式）
     * tool_5（治疗建议工具）的输出
     */
    @Type(type = "jsonb")
    @Column(name = "management_plan", columnDefinition = "jsonb")
    private List<Map<String, Object>> managementPlan;
    
    /**
     * 风险评估（JSON格式）
     * tool_6（风险评估工具）的输出
     */
    @Type(type = "jsonb")
    @Column(name = "triage", columnDefinition = "jsonb")
    private Map<String, Object> triage;
    
    /**
     * 不确定性信息（JSON格式）
     * 包含：缺失信息、冲突证据、不确定性来源等
     */
    @Type(type = "jsonb")
    @Column(name = "uncertainty", columnDefinition = "jsonb")
    private Map<String, Object> uncertainty;
    
    /**
     * 审计信息（JSON格式）
     * 包含：创建者、修改者、操作记录等
     */
    @Type(type = "jsonb")
    @Column(name = "audit", columnDefinition = "jsonb")
    private Map<String, Object> audit;
    
    /**
     * 知识引用（JSON格式）
     * 记录CDP推理过程中使用的知识对象（KO）和知识版本
     */
    @Type(type = "jsonb")
    @Column(name = "knowledge_refs", columnDefinition = "jsonb")
    private Map<String, Object> knowledgeRefs;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

**字段结构对齐说明**：
- **对外REST API**：使用 `camelCase`（如 `cdpId`、`needsClinicalMode`、`workMode`）
- **CDP内部JSON**：使用 `snake_case`（如 `health_state_assessment.needs_clinical_mode`、`work_mode`）

> **详细字段结构**：请参考《CDP数据结构设计.md》

---

### 2.2 AgentState实体（主Agent策略状态）

**对应表**：`agent_state`

> **说明**：AgentState存储主Agent的策略状态，包括阈值、预算、失败回退、已尝试工具等。

**实体类定义**：

```java
package com.aidoctor.diagnosis.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AgentState实体
 * 主Agent策略状态 - 存储主Agent的策略状态
 */
@Entity
@Table(name = "agent_state", indexes = {
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_cdp_id", columnList = "cdp_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentState {
    
    /**
     * AgentState ID（主键）
     */
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;
    
    /**
     * 会话ID（与CDP关联）
     */
    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;
    
    /**
     * CDP ID（与CDP关联）
     */
    @Column(name = "cdp_id", length = 64, nullable = false)
    private String cdpId;
    
    /**
     * 当前诊断步骤（1-5）
     */
    @Column(name = "current_step")
    private Integer currentStep;
    
    /**
     * 工作态（wellness_mode/clinical_mode）
     */
    @Column(name = "work_mode", length = 32)
    private String workMode;
    
    /**
     * 阈值配置（JSON格式）
     * 包含：confidence_threshold、evidence_count_threshold、information_gain_threshold
     */
    @Type(type = "jsonb")
    @Column(name = "thresholds", columnDefinition = "jsonb")
    private Map<String, Object> thresholds;
    
    /**
     * 预算配置（JSON格式）
     * 包含：max_tool_calls、max_time_seconds、max_cost、current_tool_calls、current_time_seconds、current_cost
     */
    @Type(type = "jsonb")
    @Column(name = "budget", columnDefinition = "jsonb")
    private Map<String, Object> budget;
    
    /**
     * 失败回退策略（JSON格式）
     * 包含：max_retries、backoff_strategy
     */
    @Type(type = "jsonb")
    @Column(name = "failure_backoff", columnDefinition = "jsonb")
    private Map<String, Object> failureBackoff;
    
    /**
     * 已尝试工具列表（JSON格式）
     * 包含：tool_id、call_count、last_result、last_call_time
     */
    @Type(type = "jsonb")
    @Column(name = "tried_tools", columnDefinition = "jsonb")
    private List<Map<String, Object>> triedTools;
    
    /**
     * 证据融合状态（JSON格式）
     * 包含：conflicts、resolution_strategy
     */
    @Type(type = "jsonb")
    @Column(name = "evidence_fusion_state", columnDefinition = "jsonb")
    private Map<String, Object> evidenceFusionState;
    
    /**
     * 停止条件状态（JSON格式）
     * 包含：cdp_required_fields_complete、evidence_references_complete、risk_assessment_complete等
     */
    @Type(type = "jsonb")
    @Column(name = "stop_conditions", columnDefinition = "jsonb")
    private Map<String, Object> stopConditions;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

**字段详细说明**：

| 字段路径 | 数据类型 | 说明 | 默认值 |
|---------|---------|------|--------|
| `thresholds.confidence_threshold` | Float | 置信度阈值 | 0.7 |
| `thresholds.evidence_count_threshold` | Integer | 证据数量阈值 | 3 |
| `thresholds.information_gain_threshold` | Float | 信息增益阈值 | 0.5 |
| `budget.max_tool_calls` | Integer | 最大工具调用次数 | 50 |
| `budget.max_time_seconds` | Integer | 最大执行时间（秒） | 300 |
| `budget.max_cost` | Float | 最大成本 | 100.0 |
| `failure_backoff.max_retries` | Integer | 最大重试次数 | 3 |
| `failure_backoff.backoff_strategy` | String | 回退策略（exponential/linear） | exponential |

> **详细字段结构**：请参考《AgentState数据结构设计.md》

---

### 2.3 AuditTrail实体（审计轨迹）

**对应表**：`audit_trail`

> **说明**：AuditTrail记录所有工具调用、CDP更新、主Agent决策等审计信息，采用追加写入模式，历史记录不可修改。

**实体类定义**：

```java
package com.aidoctor.diagnosis.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * AuditTrail实体
 * 审计轨迹 - 记录所有工具调用、CDP更新、主Agent决策等审计信息
 */
@Entity
@Table(name = "audit_trail", indexes = {
    @Index(name = "idx_cdp_id", columnList = "cdp_id"),
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditTrail {
    
    /**
     * AuditTrail ID（主键）
     */
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;
    
    /**
     * CDP ID
     */
    @Column(name = "cdp_id", length = 64, nullable = false)
    private String cdpId;
    
    /**
     * 会话ID
     */
    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;
    
    /**
     * 时间戳
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    /**
     * 事件类型（tool_call/cdp_update/agent_decision）
     */
    @Column(name = "event_type", length = 32, nullable = false)
    private String eventType;
    
    /**
     * 工具调用记录（如event_type=tool_call）
     * 包含：tool_id、tool_name、trace_id、input、output、evidence、suggested_writes、quality、errors、duration_ms
     */
    @Type(type = "jsonb")
    @Column(name = "tool_call", columnDefinition = "jsonb")
    private Map<String, Object> toolCall;
    
    /**
     * CDP更新记录（如event_type=cdp_update）
     * 包含：from_version、to_version、changed_fields、reason、updated_by、tool_trace_id
     */
    @Type(type = "jsonb")
    @Column(name = "cdp_update", columnDefinition = "jsonb")
    private Map<String, Object> cdpUpdate;
    
    /**
     * 主Agent决策记录（如event_type=agent_decision）
     * 包含：decision_type、reason、evidence_fusion、conflict_resolution
     */
    @Type(type = "jsonb")
    @Column(name = "agent_decision", columnDefinition = "jsonb")
    private Map<String, Object> agentDecision;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

**事件类型说明**：

| 事件类型 | 说明 | 使用字段 |
|---------|------|---------|
| `tool_call` | 工具调用记录 | `tool_call` |
| `cdp_update` | CDP更新记录 | `cdp_update` |
| `agent_decision` | 主Agent决策记录 | `agent_decision` |

> **详细字段结构**：请参考《AuditTrail数据结构设计.md》

---

### 2.4 CDP版本实体（CDPVersion）

**对应表**：`cdp_version`

**实体类定义**：

```java
package com.aidoctor.diagnosis.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * CDP版本实体（支持版本回放）
 */
@Entity
@Table(name = "cdp_version", indexes = {
    @Index(name = "idx_cdp_id", columnList = "cdp_id"),
    @Index(name = "idx_version", columnList = "version")
})
@Data
public class CDPVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * CDP ID
     */
    @Column(name = "cdp_id", length = 64, nullable = false)
    private String cdpId;
    
    /**
     * 版本号
     */
    @Column(name = "version", nullable = false)
    private Integer version;
    
    /**
     * 变更类型（CREATE/UPDATE）
     */
    @Column(name = "change_type", length = 32, nullable = false)
    private String changeType;
    
    /**
     * 变更字段（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "changed_fields", columnDefinition = "jsonb")
    private Map<String, Object> changedFields;
    
    /**
     * 变更原因
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
```

---

### 2.5 其他实体类

**其他实体类**（详细定义请参考原文档）：
- **DiagnosisRecord实体**：诊断记录
- **HealthStateAssessmentRecord实体**：健康状态判定记录
- **WellnessScreeningRecord实体**：健康筛查记录
- **FollowUpPlan实体**：随访计划
- **ExaminationRecord实体**：检查记录
- **ExaminationPlan实体**：检查方案

> **详细定义**：请参考《3.项目前置设计/AI医生系统-数据模型设计.md》第1.3-1.5节

---

## 三、DTO类（Data Transfer Object）设计概述

### 3.1 DTO设计原则

**DTO设计原则**：
- **请求DTO**：用于接收前端或外部系统的请求参数
- **响应DTO**：用于返回给前端或外部系统的响应数据
- **验证注解**：使用JSR-303验证注解进行参数校验
- **Builder模式**：使用Builder模式构建复杂DTO对象

### 3.2 DTO类列表

**核心DTO类**：
1. **请求DTO**：
   - DiagnosisRequest：诊断请求
   - SymptomInfo：症状信息
   - SignData：体征数据
   - UserAnswer：用户回答
   - ExaminationPlanRequest：检查方案请求

2. **结构化问题清单DTO**：
   - StructuredQuestionList：结构化问题清单

3. **三层分层结果DTO**：
   - ThreeLayerResult：三层分层结果

4. **终点结论包DTO**：
   - ConclusionPackage：终点结论包

5. **证据分析DTO**：
   - EvidenceAnalysis：证据分析

6. **健康筛查流程DTO**：
   - DemandClassificationRequest/Result：需求分类
   - HealthProfileCollectionRequest/Response：健康画像收集
   - BranchExecutionRequest/Result：分支执行
   - UnifiedResult：统一结果
   - FollowUpPlanResponse：随访计划

7. **工具相关DTO**：
   - HealthStateAssessmentResult：健康状态判定结果
   - ClinicalParsingResult：病例理解结果
   - InterviewResult：主动问诊结果
   - DifferentialDiagnosisResult：鉴别诊断结果
   - WorkupPlanResult：检查建议结果
   - TreatmentPlanResult：治疗建议结果
   - RiskAssessmentResult：风险评估结果
   - EvidenceChainResult：证据链结果

8. **响应DTO**：
   - DiagnosisResponse：诊断响应
   - QuestionResponse：问题响应
   - DiagnosisResult：诊断结果
   - ExaminationPlanResponse：检查方案响应

---

## 四、核心DTO类设计

### 4.1 诊断请求DTO（DiagnosisRequest）

**请求DTO定义**：

```java
package com.aidoctor.diagnosis.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 诊断请求
 */
@Data
public class DiagnosisRequest {
    
    /**
     * 用户ID（必填）
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    
    /**
     * 会话ID（可选，如果已创建）
     */
    private String sessionId;
    
    /**
     * 用户输入（必填）
     */
    @NotBlank(message = "用户输入不能为空")
    private String userInput;
    
    /**
     * 症状信息（可选）
     */
    private SymptomInfo symptomInfo;
    
    /**
     * 体征数据（可选）
     */
    private SignData signData;
    
    /**
     * 检查结果（可选）
     */
    private List<Map<String, Object>> examinationResults;
    
    /**
     * 基本信息（可选）
     */
    private Map<String, Object> basicInfo;
}
```

### 4.2 结构化问题清单DTO（StructuredQuestionList）

**DTO定义**：

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * 结构化问题清单
 */
@Data
@Builder
public class StructuredQuestionList {
    
    /**
     * 主要问题（主诉）
     */
    private ChiefComplaint chiefComplaint;
    
    /**
     * 伴随问题
     */
    private List<AccompanyingSymptom> accompanyingSymptoms;
    
    /**
     * 关键背景
     */
    private KeyBackground keyBackground;
    
    /**
     * 生命体征
     */
    private VitalSigns vitalSigns;
    
    /**
     * 检查结果
     */
    private List<ExaminationResult> examinationResults;
    
    /**
     * 历史诊断
     */
    private List<HistoricalDiagnosis> historicalDiagnoses;
    
    /**
     * 信息缺口
     */
    private InformationGaps informationGaps;
    
    /**
     * 信息完整度
     */
    private Double completeness;
    
    @Data
    @Builder
    public static class ChiefComplaint {
        private String name;
        private String originalText;
        private String duration;
        private String onsetMode;
        private Integer severity;
        private String frequency;
        private String location;
        private Map<String, Object> features;
        private String triageLevel;
    }
    
    @Data
    @Builder
    public static class AccompanyingSymptom {
        private String name;
        private String duration;
        private String relationship;
    }
    
    @Data
    @Builder
    public static class KeyBackground {
        private Integer age;
        private String gender;
        private List<String> medicalHistory;
        private List<String> medicationHistory;
        private List<String> familyHistory;
        private Map<String, Object> lifestyle;
        private List<String> recentEvents;
    }
    
    @Data
    @Builder
    public static class VitalSigns {
        private BloodPressure bloodPressure;
        private Integer heartRate;
        private Double temperature;
        private Integer oxygenSaturation;
        private Integer respiratoryRate;
        
        @Data
        @Builder
        public static class BloodPressure {
            private Integer systolic;
            private Integer diastolic;
        }
    }
    
    @Data
    @Builder
    public static class InformationGaps {
        private List<String> requiredGaps;
        private List<String> importantGaps;
        private List<String> optionalGaps;
    }
}
```

### 4.3 三层分层结果DTO（ThreeLayerResult）

**DTO定义**：

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 三层分层结果
 */
@Data
@Builder
public class ThreeLayerResult {
    
    /**
     * 首要假设（Tier 1）
     */
    private List<DiagnosisCandidate> tier1MostLikely;
    
    /**
     * 主要备选诊断（Tier 2）
     */
    private List<DiagnosisCandidate> tier2MustExclude;
    
    /**
     * 积极备选方向（Tier 3）
     */
    private List<DiagnosisCandidate> tier3ActiveAlternatives;
    
    @Data
    @Builder
    public static class DiagnosisCandidate {
        private String diseaseName;
        private String diseaseCui;
        private String inclusionReason;
        private List<String> supportingClues;
        private List<String> opposingClues;
        private List<String> missingInfo;
        private String defaultTier;
    }
}
```

### 4.4 终点结论包DTO（ConclusionPackage）

**DTO定义**：

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 终点结论包（四要素）
 */
@Data
@Builder
public class ConclusionPackage {
    
    /**
     * 结论
     */
    private Conclusion conclusion;
    
    /**
     * 必须排除项状态
     */
    private MustExcludeStatus mustExcludeStatus;
    
    /**
     * 关键依据
     */
    private List<KeyEvidence> keyEvidence;
    
    /**
     * 行动与随访
     */
    private ActionAndFollowUp actionAndFollowUp;
    
    @Data
    @Builder
    public static class Conclusion {
        private ConclusionType type;
        private String mostLikelyDiagnosis;
        private String confidence;
        private String reasoning;
    }
    
    public enum ConclusionType {
        CONFIRMED,    // 可确证终点
        PROBABLE      // 不可确证终点
    }
    
    @Data
    @Builder
    public static class MustExcludeStatus {
        private ExcludeStatus status;
        private String excludeReason;
    }
    
    public enum ExcludeStatus {
        NONE,                    // 无必须排除项
        EXCLUDED,                // 已排除
        NOT_EXCLUDED,            // 未排除
        NEED_OFFLINE_EXCLUDE     // 需线下排除
    }
    
    @Data
    @Builder
    public static class KeyEvidence {
        private String item;
        private String type;  // symptom / sign / examination / medical_history
        private String strength;  // strong / medium / weak
        private String role;  // supporting / opposing
    }
    
    @Data
    @Builder
    public static class ActionAndFollowUp {
        private List<Action> immediateActions;
        private String reviewWindow;
        private List<String> upgradeTriggers;
        
        @Data
        @Builder
        public static class Action {
            private String type;  // examination / medical_advice / observation
            private String name;
            private String priority;  // high / medium / low
        }
    }
}
```

### 4.5 工具相关DTO

#### 4.5.1 健康状态判定结果（HealthStateAssessmentResult）

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * tool_0：健康状态判定结果
 */
@Data
@Builder
public class HealthStateAssessmentResult {
    
    /**
     * 是否需要进入临床诊疗态
     */
    private Boolean needsClinicalMode;
    
    /**
     * 工作态
     */
    private String workMode;
    
    /**
     * 风险等级
     */
    private String riskLevel;
    
    /**
     * 判定依据
     */
    private String assessmentReason;
    
    /**
     * 红旗信号
     */
    private List<RedFlag> redFlags;
    
    /**
     * 健康管理计划（健康管理态使用）
     */
    private WellnessPlan wellnessPlan;
    
    /**
     * CDP ID
     */
    private String cdpId;
}
```

#### 4.5.2 病例理解结果（ClinicalParsingResult）

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * tool_1：病例理解结果
 */
@Data
@Builder
public class ClinicalParsingResult {
    
    /**
     * 归一化后的概念列表
     */
    private List<NormalizedConcept> concepts;
    
    /**
     * 结构化数据
     */
    private StructuredData structuredData;
    
    /**
     * 歧义表达列表
     */
    private List<AmbiguousExpression> ambiguousExpressions;
}
```

#### 4.5.3 主动问诊结果（InterviewResult）

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * tool_2：主动问诊结果
 */
@Data
@Builder
public class InterviewResult {
    
    /**
     * 问诊问题列表
     */
    private List<Question> questions;
    
    /**
     * 信息缺口列表
     */
    private List<InformationGap> informationGaps;
    
    /**
     * 问诊策略
     */
    private InterviewStrategy strategy;
}
```

#### 4.5.4 鉴别诊断结果（DifferentialDiagnosisResult）

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * tool_3：鉴别诊断结果
 */
@Data
@Builder
public class DifferentialDiagnosisResult {
    
    /**
     * 三层分层结果
     */
    private ThreeLayerResult ddx;
    
    /**
     * 推理路径（DR.KNOWS路径）
     */
    private List<ReasoningPath> reasoningPaths;
    
    /**
     * 引擎结果
     */
    private Map<String, Object> engineResults;
}
```

#### 4.5.5 检查建议结果（WorkupPlanResult）

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * tool_4：检查建议结果
 */
@Data
@Builder
public class WorkupPlanResult {
    
    /**
     * 检查计划
     */
    private List<WorkupItem> workupPlan;
    
    /**
     * 验证计划
     */
    private VerificationPlan verificationPlan;
}
```

#### 4.5.6 治疗建议结果（TreatmentPlanResult）

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;

/**
 * tool_5：治疗建议结果
 */
@Data
@Builder
public class TreatmentPlanResult {
    
    /**
     * 治疗方案
     */
    private TreatmentPlan treatmentPlan;
}
```

#### 4.5.7 风险评估结果（RiskAssessmentResult）

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * tool_6：风险评估结果
 */
@Data
@Builder
public class RiskAssessmentResult {
    
    /**
     * 风险等级
     */
    private String riskLevel;
    
    /**
     * 严重程度
     */
    private String severity;
    
    /**
     * 紧急程度
     */
    private String urgency;
    
    /**
     * 红旗信号
     */
    private List<RedFlag> redFlags;
    
    /**
     * 复评计划
     */
    private ReviewPlan reviewPlan;
}
```

#### 4.5.8 证据链结果（EvidenceChainResult）

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * tool_7：证据链结果
 */
@Data
@Builder
public class EvidenceChainResult {
    
    /**
     * 证据链
     */
    private List<Evidence> evidenceChain;
    
    /**
     * 推理路径
     */
    private List<String> reasoningPaths;
    
    /**
     * 终点结论包
     */
    private ConclusionPackage conclusionPackage;
}
```

---

## 五、健康筛查流程（A路径）相关DTO

### 5.1 需求分类DTO

**请求DTO**：

```java
package com.aidoctor.wellness.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * A1｜需求分类请求
 */
@Data
public class DemandClassificationRequest {
    
    /**
     * 用户ID（必填）
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    
    /**
     * 用户输入（必填）
     */
    @NotBlank(message = "用户输入不能为空")
    private String userInput;
    
    /**
     * CDP ID（可选，如果已创建）
     */
    private String cdpId;
}
```

**响应DTO**：

```java
package com.aidoctor.wellness.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

/**
 * A1｜需求分类响应
 */
@Data
@Builder
public class DemandClassificationResult {
    
    /**
     * 需求类型
     */
    private DemandType demandType;
    
    /**
     * 提取的关键信息
     */
    private Map<String, Object> extractedInfo;
    
    /**
     * 下一步操作
     */
    private String nextStep;
    
    /**
     * CDP ID
     */
    private String cdpId;
    
    public enum DemandType {
        SCREENING_RECOMMENDATION,    // 筛查建议
        HEALTH_GOAL_MANAGEMENT,      // 健康目标管理
        PLANNED_HEALTH_NEEDS         // 计划性健康需求
    }
}
```

### 5.2 其他健康筛查流程DTO

**其他DTO**（详细定义请参考原文档）：
- HealthProfileCollectionRequest/Response：健康画像收集
- BranchExecutionRequest/Result：分支执行
- UnifiedResult：统一结果
- FollowUpPlanResponse：随访计划

> **详细定义**：请参考《3.项目前置设计/AI医生系统-数据模型设计.md》第2.6节

---

## 六、数据模型关系与约束

### 6.1 实体关系

**核心实体关系**：
- **CDP ↔ AgentState**：一对一关系（通过sessionId和cdpId关联）
- **CDP ↔ AuditTrail**：一对多关系（一个CDP有多个审计记录）
- **CDP ↔ CDPVersion**：一对多关系（一个CDP有多个版本记录）
- **CDP ↔ DiagnosisRecord**：一对一关系（通过cdpId关联）

### 6.2 数据约束

**数据约束**：
- **CDP**：patientId、sessionId、version不能为空
- **AgentState**：sessionId、cdpId不能为空
- **AuditTrail**：cdpId、sessionId、eventType、timestamp不能为空
- **CDPVersion**：cdpId、version、changeType不能为空

### 6.3 索引设计

**索引设计**：
- **CDP表**：patientId、sessionId、version、createdAt
- **AgentState表**：sessionId、cdpId、createdAt
- **AuditTrail表**：cdpId、sessionId、eventType、timestamp
- **CDPVersion表**：cdpId、version

---

## 七、参考文档

### 7.1 数据模型设计文档
- 《CDP数据结构设计.md》：CDP数据结构详细设计
- 《AgentState数据结构设计.md》：AgentState数据结构详细设计
- 《AuditTrail数据结构设计.md》：AuditTrail数据结构详细设计

### 7.2 原文档
- 《3.项目前置设计/AI医生系统-数据模型设计.md》：完整的数据模型设计文档

---

**文档来源**：
- 原文档：《3.项目前置设计/AI医生系统-数据模型设计.md》一、实体类（Entity）设计（第9-2000行）
- 原文档：《3.项目前置设计/AI医生系统-数据模型设计.md》二、DTO类（Data Transfer Object）设计（第2001-4133行）
- 创建时间：2025-01-22
- 文档版本：v1.0

