# AI医生系统 - 数据模型设计

> **文档定位**：本文档详细定义AI医生系统的实体类（Entity）和DTO类（Data Transfer Object）设计。  
> **参考文档**：《AI医生系统-系统功能设计.md》、《AI医生系统-技术架构设计.md》  
> **设计基础**：基于DR.KNOWS论文，采用单主Agent + 多工具Tools架构设计

---

## 一、实体类（Entity）设计

### 1.1 CDP实体（Clinical Decision Package - 临床决策包）

**对应表**：`cdp`

> **说明**：CDP是AI医生系统的核心数据结构，贯穿整个诊断流程。基于DR.KNOWS设计，支持版本控制和可追溯性。

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
     * CDP状态（与《AI医生系统-技术架构设计-CDP数据与状态管理.md》7.3对齐）
     *
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
     * 包含：工作态判定（wellness_mode/clinical_mode）、风险等级、入口判定流程结果等
     * 对应功能设计文档：2.0节 健康状态判定
     */
    @Type(type = "jsonb")
    @Column(name = "health_state_assessment", columnDefinition = "jsonb")
    private Map<String, Object> healthStateAssessment;
    
    /**
     * 健康管理计划（JSON格式）
     * 健康管理态使用
     * 包含：风险管理、生活方式建议、随访计划、健康筛查路径（A路径）执行结果等
     * 对应功能设计文档：3.2节 健康管理态详细流程
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
     * 对应知识演化与维护设计：CDP中的知识引用
     * 包含：knowledge_refs数组、default_kg_version、knowledge_usage_summary
     * 
     * 字段结构：
     * {
     *   "default_kg_version": "v2.1",  // 默认使用的知识版本
     *   "knowledge_refs": [            // 知识引用数组
     *     {
     *       "ko_id": "KO_001",         // 知识对象ID
     *       "kg_version": "v2.1",      // 知识版本
     *       "provenance_pointer": "指南v2026-第3章-第5段",  // 来源指针
     *       "usage_context": "ddx_candidate_generation",    // 使用上下文
     *       "binding_strength": "required"                 // 绑定强度
     *     }
     *   ],
     *   "knowledge_usage_summary": {   // 知识使用摘要
     *     "total_ko_count": 10,         // 使用的KO总数
     *     "ko_types": {                 // 按类型统计
     *       "rule": 5,
     *       "relation": 3,
     *       "pathway_template": 2
     *     },
     *     "impact_scope": ["DDx", "workup"]  // 影响模块
     *   }
     * }
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

#### 1.1.1 CDP字段结构对齐说明（与《技术架构设计-CDP数据与状态管理.md》一致）

为避免“外部API字段（camelCase）”与“CDP内部字段（snake_case）”混淆，建议统一约定：
- **对外REST API（给前端/业务方）**：使用 `camelCase`（如 `cdpId`、`needsClinicalMode`、`workMode`）
- **CDP内部JSON（持久化字段）**：使用 `snake_case`（如 `health_state_assessment.needs_clinical_mode`、`work_mode`）

其中 `health_state_assessment` 必须包含入口判定结果 `entry_assessment`（Step 1-5 的结构化产物），并补齐 `symptom_severity`、`early_risk_signals` 等字段；`wellness_plan` 需要包含 `wellness_screening_path`（A1-A5 全流程产物），详见：
- 《AI医生系统-系统功能设计.md》1.3、2.0、3.2
- 《AI医生系统-技术架构设计-CDP数据与状态管理.md》6.1、7.3

---

### 1.2 AgentState实体（主Agent策略状态）

**对应表**：`agent_state`

> **说明**：AgentState存储主Agent的策略状态，包括阈值、预算、失败回退、已尝试工具等。工具不能直接访问AgentState，只能通过ToolContext获取AgentState摘要。

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

#### 1.2.1 AgentState字段结构对齐说明

**字段命名规范**：
- **数据库字段**：使用`snake_case`（如`current_step`、`work_mode`）
- **Java实体字段**：使用`camelCase`（如`currentStep`、`workMode`）
- **JSON字段**：使用`snake_case`（与数据库字段一致）

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

> **参考文档**：详细的AgentState数据结构定义请参考《AI医生系统-技术架构设计-CDP数据与状态管理.md》6.2节。

---

### 1.3 AuditTrail实体（审计轨迹）

**对应表**：`audit_trail`

> **说明**：AuditTrail记录所有工具调用、CDP更新、主Agent决策等审计信息，采用追加写入模式，历史记录不可修改。

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

#### 1.3.1 AuditTrail字段结构对齐说明

**事件类型说明**：

| 事件类型 | 说明 | 使用字段 |
|---------|------|---------|
| `tool_call` | 工具调用记录 | `tool_call` |
| `cdp_update` | CDP更新记录 | `cdp_update` |
| `agent_decision` | 主Agent决策记录 | `agent_decision` |

**字段详细说明**：

**tool_call字段结构**（event_type=tool_call时）：
- `tool_id`：工具ID
- `tool_name`：工具名称
- `trace_id`：追踪ID
- `input`：输入（CDP字段路径引用）
- `output`：输出（payload摘要）
- `evidence`：证据引用
- `suggested_writes`：建议写回字段路径
- `quality`：质量指标
- `errors`：错误信息
- `duration_ms`：执行时间（毫秒）

**cdp_update字段结构**（event_type=cdp_update时）：
- `from_version`：源版本号
- `to_version`：目标版本号
- `changed_fields`：变更字段路径
- `reason`：更新原因
- `updated_by`：更新者（agent_main）
- `tool_trace_id`：工具追踪ID

**agent_decision字段结构**（event_type=agent_decision时）：
- `decision_type`：决策类型（stop/escalate/refuse/continue）
- `reason`：决策原因
- `evidence_fusion`：证据融合结果
- `conflict_resolution`：冲突解决结果

> **参考文档**：详细的AuditTrail数据结构定义请参考《AI医生系统-技术架构设计-CDP数据与状态管理.md》6.3节。

---

### 1.4 CDP版本实体（CDPVersion）

**对应表**：`cdp_version`

```java
package com.aidoctor.diagnosis.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

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

### 1.3 诊断记录实体（DiagnosisRecord）

**对应表**：`diagnosis_record`

**更新说明**：添加`cdpId`字段，关联CDP。

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
 * 诊断记录实体
 */
@Entity
@Table(name = "diagnosis_record", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_family_id", columnList = "family_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisRecord {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    /**
     * 家庭ID（可选）
     */
    @Column(name = "family_id", length = 64)
    private String familyId;
    
    /**
     * CDP ID（关联CDP）
     * 新增字段：关联临床决策包
     */
    @Column(name = "cdp_id", length = 64)
    private String cdpId;
    
    /**
     * 工作态
     * wellness_mode: 健康管理态
     * clinical_mode: 临床诊疗态
     */
    @Column(name = "work_mode", length = 32)
    @Enumerated(EnumType.STRING)
    private WorkMode workMode;
    
    /**
     * 诊断类型
     * symptom: 症状诊断
     * examination: 检查诊断
     * comprehensive: 综合诊断
     */
    @Column(name = "diagnosis_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private DiagnosisType diagnosisType;
    
    /**
     * 诊断状态
     * collecting: 信息收集中
     * questioning: 追问中
     * analyzing: 分析中
     * completed: 已完成
     * cancelled: 已取消
     */
    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private DiagnosisStatus status;
    
    // ========== 症状信息 ==========
    
    /**
     * 主诉
     */
    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;
    
    /**
     * 症状持续时间
     */
    @Column(name = "symptom_duration", length = 64)
    private String symptomDuration;
    
    /**
     * 症状严重程度（0-10分）
     */
    @Column(name = "symptom_severity")
    private Integer symptomSeverity;
    
    /**
     * 症状频率
     * continuous: 持续
     * intermittent: 间歇
     * occasional: 偶尔
     */
    @Column(name = "symptom_frequency", length = 32)
    private String symptomFrequency;
    
    /**
     * 症状位置
     */
    @Column(name = "symptom_location", columnDefinition = "TEXT")
    private String symptomLocation;
    
    /**
     * 伴随症状（逗号分隔）
     */
    @Column(name = "accompanying_symptoms", columnDefinition = "TEXT")
    private String accompanyingSymptoms;
    
    /**
     * 症状特征（JSON格式）
     * {
     *   "trigger": "运动后",
     *   "relief": "休息后缓解",
     *   "aggravating": "活动时加重",
     *   "quality": "压迫感"
     * }
     */
    @Type(type = "jsonb")
    @Column(name = "symptom_features", columnDefinition = "jsonb")
    private Map<String, Object> symptomFeatures;
    
    // ========== 体征数据 ==========
    
    /**
     * 生命体征（JSON格式）
     * {
     *   "bp": {"systolic": 130, "diastolic": 85},
     *   "heart_rate": 75,
     *   "temperature": 36.5,
     *   "oxygen_saturation": 98,
     *   "respiratory_rate": 18
     * }
     */
    @Type(type = "jsonb")
    @Column(name = "vital_signs", columnDefinition = "jsonb")
    private Map<String, Object> vitalSigns;
    
    /**
     * 体格检查结果（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "physical_exam", columnDefinition = "jsonb")
    private Map<String, Object> physicalExam;
    
    // ========== 检查结果 ==========
    
    /**
     * 检查结果列表（JSON格式）
     * [
     *   {
     *     "type": "blood_test",
     *     "name": "血常规",
     *     "date": "2025-01-15",
     *     "data": {...}
     *   }
     * ]
     */
    @Type(type = "jsonb")
    @Column(name = "examination_results", columnDefinition = "jsonb")
    private List<Map<String, Object>> examinationResults;
    
    // ========== 诊断结果 ==========
    
    /**
     * 诊断结果（JSON格式）
     * {
     *   "possibilities": [
     *     {
     *       "disease": "心绞痛",
     *       "confidence": 0.75,
     *       "level": "high",
     *       "supporting_evidence": [...],
     *       "opposing_evidence": [...]
     *     }
     *   ],
     *   "suggestions": {
     *     "examinations": [...],
     *     "medical_advice": "..."
     *   }
     * }
     */
    @Type(type = "jsonb")
    @Column(name = "diagnosis_result", columnDefinition = "jsonb")
    private Map<String, Object> diagnosisResult;
    
    // ========== 对话记录 ==========
    
    /**
     * 对话历史（JSON格式）
     * [
     *   {
     *     "role": "user",
     *     "content": "我最近胸痛",
     *     "timestamp": "2025-01-15T10:30:00"
     *   },
     *   {
     *     "role": "system",
     *     "content": "胸痛需要重视。请问疼痛持续多久了？",
     *     "timestamp": "2025-01-15T10:30:05"
     *   }
     * ]
     */
    @Type(type = "jsonb")
    @Column(name = "dialogue_history", columnDefinition = "jsonb")
    private List<Map<String, Object>> dialogueHistory;
    
    /**
     * 追问次数
     */
    @Column(name = "questioning_count", columnDefinition = "INTEGER DEFAULT 0")
    private Integer questioningCount;
    
    // ========== 元数据 ==========
    
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
    
    /**
     * 完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    /**
     * 诊断类型枚举
     */
    public enum DiagnosisType {
        SYMPTOM,        // 症状诊断
        EXAMINATION,    // 检查诊断
        COMPREHENSIVE   // 综合诊断
    }
    
    /**
     * 诊断状态枚举
     */
    public enum DiagnosisStatus {
        COLLECTING,     // 信息收集中
        QUESTIONING,    // 追问中
        ANALYZING,      // 分析中
        COMPLETED,      // 已完成
        CANCELLED       // 已取消
    }
}
```

### 1.3 健康状态判定记录实体（HealthStateAssessmentRecord）

**对应表**：`health_state_assessment_record`

```java
package com.aidoctor.assessment.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 健康状态判定记录实体
 * 对应tool_0（健康状态判定工具）
 */
@Entity
@Table(name = "health_state_assessment_record", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_work_mode", columnList = "work_mode")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStateAssessmentRecord {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    /**
     * 会话ID
     */
    @Column(name = "session_id", length = 64)
    private String sessionId;
    
    /**
     * CDP ID（关联CDP）
     */
    @Column(name = "cdp_id", length = 64)
    private String cdpId;
    
    /**
     * 工作态
     * wellness_mode: 健康管理态
     * clinical_mode: 临床诊疗态
     */
    @Column(name = "work_mode", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private WorkMode workMode;
    
    /**
     * 风险等级（L1-L4）
     */
    @Column(name = "risk_level", length = 10)
    private String riskLevel;
    
    /**
     * 症状严重程度
     */
    @Column(name = "severity_level", length = 32)
    @Enumerated(EnumType.STRING)
    private SeverityLevel severityLevel;
    
    /**
     * 红旗信号列表（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "red_flags", columnDefinition = "jsonb")
    private List<String> redFlags;
    
    /**
     * 风险信号（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "risk_signals", columnDefinition = "jsonb")
    private Map<String, Object> riskSignals;
    
    /**
     * 判定原因
     */
    @Column(name = "assessment_reason", columnDefinition = "TEXT")
    private String assessmentReason;
    
    /**
     * 用户输入（原始输入）
     */
    @Column(name = "user_input", columnDefinition = "TEXT")
    private String userInput;
    
    /**
     * 基本信息（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "basic_info", columnDefinition = "jsonb")
    private Map<String, Object> basicInfo;
    
    /**
     * 健康管理计划（如果是健康管理态，JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "wellness_plan", columnDefinition = "jsonb")
    private Map<String, Object> wellnessPlan;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 工作态枚举
     */
    public enum WorkMode {
        WELLNESS_MODE,      // 健康管理态
        CLINICAL_MODE       // 临床诊疗态
    }
    
    /**
     * 严重程度枚举
     */
    public enum SeverityLevel {
        NORMAL,             // 正常
        LOW,                // 轻微
        MODERATE,           // 中等
        HIGH                // 严重
    }
}
```

### 1.4 健康筛查记录实体（WellnessScreeningRecord）

**对应表**：`wellness_screening_record`

```java
package com.aidoctor.wellness.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康筛查记录实体
 * 对应健康筛查流程（A路径）
 */
@Entity
@Table(name = "wellness_screening_record", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_cdp_id", columnList = "cdp_id"),
    @Index(name = "idx_demand_type", columnList = "demand_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessScreeningRecord {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    /**
     * CDP ID（关联CDP）
     */
    @Column(name = "cdp_id", length = 64)
    private String cdpId;
    
    /**
     * 需求类型
     * SCREENING_RECOMMENDATION: 筛查建议
     * HEALTH_GOAL_MANAGEMENT: 健康目标管理
     * PLANNED_HEALTH_NEEDS: 计划性健康需求
     */
    @Column(name = "demand_type", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    private DemandType demandType;
    
    /**
     * 健康画像（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "health_profile", columnDefinition = "jsonb")
    private Map<String, Object> healthProfile;
    
    /**
     * 分支执行结果（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "branch_result", columnDefinition = "jsonb")
    private Map<String, Object> branchResult;
    
    /**
     * 统一结果（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "unified_result", columnDefinition = "jsonb")
    private Map<String, Object> unifiedResult;
    
    /**
     * 随访计划（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "follow_up_plan", columnDefinition = "jsonb")
    private Map<String, Object> followUpPlan;
    
    /**
     * 当前阶段
     */
    @Column(name = "current_stage", length = 64)
    private String currentStage;
    // A1_DEMAND_CLASSIFICATION | A2_HEALTH_PROFILE_COLLECTED | A3_BRANCH_EXECUTED | A4_UNIFIED_RESULT_GENERATED | A5_FOLLOW_UP_SETUP
    
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
    
    /**
     * 完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    /**
     * 需求类型枚举
     */
    public enum DemandType {
        SCREENING_RECOMMENDATION,    // 筛查建议
        HEALTH_GOAL_MANAGEMENT,      // 健康目标管理
        PLANNED_HEALTH_NEEDS         // 计划性健康需求
    }
}
```

### 1.5 随访计划实体（FollowUpPlan）

**对应表**：`follow_up_plan`

```java
package com.aidoctor.wellness.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 随访计划实体
 */
@Entity
@Table(name = "follow_up_plan", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_follow_up_date", columnList = "follow_up_date"),
    @Index(name = "idx_is_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpPlan {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    /**
     * CDP ID（关联CDP）
     */
    @Column(name = "cdp_id", length = 64)
    private String cdpId;
    
    /**
     * 筛查记录ID
     */
    @Column(name = "screening_record_id")
    private Long screeningRecordId;
    
    /**
     * 随访日期
     */
    @Column(name = "follow_up_date", nullable = false)
    private LocalDate followUpDate;
    
    /**
     * 提醒内容
     */
    @Column(name = "reminder_content", columnDefinition = "TEXT")
    private String reminderContent;
    
    /**
     * 是否激活
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    /**
     * 是否已提醒
     */
    @Column(name = "is_reminded", nullable = false)
    private Boolean isReminded;
    
    /**
     * 提醒时间
     */
    @Column(name = "reminded_at")
    private LocalDateTime remindedAt;
    
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

### 1.2 检查记录实体（ExaminationRecord）

**对应表**：`examination_record`

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 检查记录实体
 */
@Entity
@Table(name = "examination_record", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_family_id", columnList = "family_id"),
    @Index(name = "idx_examination_type", columnList = "examination_type"),
    @Index(name = "idx_examination_date", columnList = "examination_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExaminationRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    @Column(name = "family_id", length = 64)
    private String familyId;
    
    /**
     * 检查类型
     * blood_test: 血液检查
     * imaging: 影像检查
     * physical: 体格检查
     * comprehensive: 综合检查
     */
    @Column(name = "examination_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private ExaminationType examinationType;
    
    // ========== 检查方案 ==========
    
    @Column(name = "plan_id")
    private Long planId;
    
    @Column(name = "plan_name", length = 255)
    private String planName;
    
    /**
     * 检查项目列表（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "plan_items", columnDefinition = "jsonb")
    private List<Map<String, Object>> planItems;
    
    // ========== 报告信息 ==========
    
    /**
     * 报告类型
     * image: 图片
     * pdf: PDF文件
     * text: 文本
     */
    @Column(name = "report_type", length = 32)
    private String reportType;
    
    @Column(name = "report_file_path", length = 512)
    private String reportFilePath;
    
    /**
     * OCR识别结果（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "report_ocr_result", columnDefinition = "jsonb")
    private Map<String, Object> reportOcrResult;
    
    /**
     * 结构化数据（JSON格式）
     * {
     *   "indicators": [
     *     {
     *       "name": "WBC",
     *       "value": 6.5,
     *       "unit": "10^9/L",
     *       "normal_range": "4-10",
     *       "status": "normal"
     *     }
     *   ]
     * }
     */
    @Type(type = "jsonb")
    @Column(name = "report_structured_data", columnDefinition = "jsonb")
    private Map<String, Object> reportStructuredData;
    
    // ========== 解读结果 ==========
    
    /**
     * 解读结果（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "interpretation_result", columnDefinition = "jsonb")
    private Map<String, Object> interpretationResult;
    
    // ========== 元数据 ==========
    
    @Column(name = "examination_date")
    private LocalDate examinationDate;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum ExaminationType {
        BLOOD_TEST,
        IMAGING,
        PHYSICAL,
        COMPREHENSIVE
    }
}
```

### 1.3 检查方案实体（ExaminationPlan）

**对应表**：`examination_plan`

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
 * 检查方案实体
 */
@Entity
@Table(name = "examination_plan", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_plan_type", columnList = "plan_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExaminationPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    @Column(name = "plan_name", nullable = false, length = 255)
    private String planName;
    
    /**
     * 方案类型
     * routine: 常规体检
     * diagnostic: 诊断性检查
     * follow_up: 随访检查
     */
    @Column(name = "plan_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private PlanType planType;
    
    /**
     * 检查项目列表（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "plan_items", columnDefinition = "jsonb")
    private List<Map<String, Object>> planItems;
    
    /**
     * 目标疾病或健康状态（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "target_conditions", columnDefinition = "jsonb")
    private Map<String, Object> targetConditions;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum PlanType {
        ROUTINE,
        DIAGNOSTIC,
        FOLLOW_UP
    }
}
```

---

## 二、DTO类（Data Transfer Object）设计

### 2.1 请求DTO

#### 2.1.1 诊断请求（DiagnosisRequest）

```java
package com.aidoctor.diagnosis.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 开始诊断请求
 */
@Data
public class DiagnosisRequest {
    
    /**
     * 用户ID（必填）
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    
    /**
     * 诊断类型
     */
    @NotNull(message = "诊断类型不能为空")
    private String diagnosisType; // symptom, examination, comprehensive
    
    /**
     * 初始症状信息（可选）
     */
    private SymptomInfo symptomInfo;
    
    /**
     * 检查结果ID（如果是基于检查结果的诊断）
     */
    private Long examinationRecordId;
}
```

#### 2.1.2 症状信息（SymptomInfo）

```java
package com.aidoctor.diagnosis.dto.request;

import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Map;

/**
 * 症状信息
 */
@Data
public class SymptomInfo {
    
    /**
     * 主诉
     */
    private String chiefComplaint;
    
    /**
     * 症状持续时间
     */
    private String duration; // 如："3天"、"1周"、"2个月"
    
    /**
     * 症状严重程度（0-10分）
     */
    @Min(value = 0, message = "严重程度不能小于0")
    @Max(value = 10, message = "严重程度不能大于10")
    private Integer severity;
    
    /**
     * 症状频率
     */
    private String frequency; // continuous, intermittent, occasional
    
    /**
     * 症状位置
     */
    private String location;
    
    /**
     * 伴随症状（列表）
     */
    private java.util.List<String> accompanyingSymptoms;
    
    /**
     * 症状特征
     */
    private Map<String, Object> features; // trigger, relief, aggravating, quality
}
```

#### 2.1.3 体征数据（SignData）

```java
package com.aidoctor.diagnosis.dto.request;

import lombok.Data;
import java.util.Map;

/**
 * 体征数据
 */
@Data
public class SignData {
    
    /**
     * 血压
     */
    private BloodPressure bp;
    
    /**
     * 心率（次/分）
     */
    private Integer heartRate;
    
    /**
     * 体温（摄氏度）
     */
    private Double temperature;
    
    /**
     * 血氧饱和度（%）
     */
    private Integer oxygenSaturation;
    
    /**
     * 呼吸频率（次/分）
     */
    private Integer respiratoryRate;
    
    /**
     * 其他体征数据
     */
    private Map<String, Object> otherSigns;
    
    @Data
    public static class BloodPressure {
        private Integer systolic;   // 收缩压
        private Integer diastolic;  // 舒张压
    }
}
```

#### 2.1.4 用户回答（UserAnswer）

```java
package com.aidoctor.diagnosis.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 用户回答
 */
@Data
public class UserAnswer {
    
    /**
     * 诊断ID
     */
    @NotBlank(message = "诊断ID不能为空")
    private String diagnosisId;
    
    /**
     * 用户回答内容
     */
    @NotBlank(message = "回答内容不能为空")
    private String answer;
    
    /**
     * 回答类型（可选）
     * symptom: 症状相关
     * sign: 体征相关
     * history: 病史相关
     * other: 其他
     */
    private String answerType;
}
```

#### 2.1.5 检查方案请求（ExaminationPlanRequest）

```java
package com.aidoctor.diagnosis.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 检查方案设计请求
 */
@Data
public class ExaminationPlanRequest {
    
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    
    @NotBlank(message = "方案名称不能为空")
    private String planName;
    
    @NotNull(message = "方案类型不能为空")
    private String planType; // routine, diagnostic, follow_up
    
    /**
     * 目标疾病或健康状态
     */
    private Map<String, Object> targetConditions;
    
    /**
     * 症状信息（用于设计诊断性检查）
     */
    private SymptomInfo symptomInfo;
    
    /**
     * 年龄、性别等基本信息
     */
    private Map<String, Object> basicInfo;
}
```

### 2.2 结构化问题清单DTO（新增）

**对应5步AI循证诊断流程**：Step 1（识别问题）的输出数据结构

根据系统设计方案，结构化问题清单是诊断流程Step 1的核心数据结构，用于把用户的自然语言描述转化成可推理、可复用、可审计的结构化"问题清单"。

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * 结构化问题清单
 * 核心目标：把用户的自然语言描述，转化成可推理、可复用、可审计的结构化"问题清单"
 */
@Data
@Builder
public class StructuredQuestionList {
    
    /**
     * 主要问题（用户最关注的主诉）
     */
    private ChiefComplaint chiefComplaint;
    
    /**
     * 伴随问题（同时间窗出现的关键症状/体征）
     */
    private List<AccompanyingSymptom> accompanyingSymptoms;
    
    /**
     * 关键背景（诊断错误常源于信息采集偏差，必须纳入）
     */
    private KeyBackground keyBackground;
    
    /**
     * 生命体征（主动询问或设备采集）
     */
    private VitalSigns vitalSigns;
    
    /**
     * 检查结果（用户上传）
     */
    private List<ExaminationResult> examinationResults;
    
    /**
     * 历史诊断（从诊断记录获取）
     */
    private List<HistoricalDiagnosis> historicalDiagnoses;
    
    /**
     * 信息缺口标记
     */
    private InformationGaps informationGaps;
    
    /**
     * 信息完整度（0-1）
     */
    private Double completeness;
    
    /**
     * 主要问题
     */
    @Data
    @Builder
    public static class ChiefComplaint {
        private String name;  // 归一化后的主诉名称
        private String originalText;  // 用户原始描述
        private String duration;  // 持续时间
        private String onsetMode;  // 起病方式（sudden/gradual）
        private Integer severity;  // 严重程度（0-10）
        private String frequency;  // 频率（continuous/intermittent/occasional）
        private String location;  // 位置
        private Map<String, Object> features;  // 症状特征（诱因、缓解因素等）
        private String triageLevel;  // 分诊等级（L1-L4）
    }
    
    /**
     * 伴随症状
     */
    @Data
    @Builder
    public static class AccompanyingSymptom {
        private String name;
        private String startTime;
        private Integer severity;
    }
    
    /**
     * 关键背景
     */
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
    
    /**
     * 生命体征
     */
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
    
    /**
     * 信息缺口
     */
    @Data
    @Builder
    public static class InformationGaps {
        /**
         * 必填缺口（缺失则不能进入阶段3）
         */
        private List<String> requiredGaps;
        
        /**
         * 重要缺口（可进入但必须提示不确定性与风险）
         */
        private List<String> importantGaps;
        
        /**
         * 可选缺口（后续补充即可）
         */
        private List<String> optionalGaps;
    }
}
```

### 2.3 三层分层结果DTO（新增）

根据系统设计方案，三层分层结果是诊断流程阶段3的核心输出。

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * 三层分层结果
 * 根据系统设计方案：
 * - 首要假设（1个）：当前信息最能支持、最符合整体表现的方向
 * - 主要备选诊断（1-2个）：与首要假设并列需要对比、仍可能成立的方向
 * - 必须排除的高危诊断（0-1个）：一旦漏诊后果严重，即使概率不高也必须纳入并优先排除
 */
@Data
@Builder
public class ThreeLayerResult {
    
    /**
     * 首要假设（1个）
     */
    private PrimaryHypothesis primaryHypothesis;
    
    /**
     * 主要备选诊断（1-2个）
     */
    private List<MainAlternative> mainAlternatives;
    
    /**
     * 必须排除的高危诊断（0-1个）
     */
    private MustExcludeDiagnosis mustExclude;
    
    /**
     * 所有候选（Top 5）
     */
    private Map<String, Double> allCandidates;
    
    @Data
    @Builder
    public static class PrimaryHypothesis {
        private String disease;
        private Double score;
        private String layer;
        private String evidence;  // 入选依据
    }
    
    @Data
    @Builder
    public static class MainAlternative {
        private String disease;
        private Double score;
        private String layer;
        private String evidence;  // 入选依据
    }
    
    @Data
    @Builder
    public static class MustExcludeDiagnosis {
        private String disease;
        private Double score;
        private String layer;
        private String reason;  // 为什么必须排除
    }
}
```

### 2.4 终点结论包DTO（新增）

根据系统设计方案，终点结论包是诊断流程阶段5的核心输出，包含四要素。

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 终点结论包
 * 根据系统设计方案，包含四要素：
 * 1. 结论（可确证/不可确证）
 * 2. 必须排除项状态
 * 3. 关键依据（至少三条证据）
 * 4. 行动与随访
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
     * 关键依据（至少三条证据）
     */
    private List<KeyEvidence> keyEvidence;
    
    /**
     * 行动与随访
     */
    private ActionAndFollowUp actionAndFollowUp;
    
    @Data
    @Builder
    public static class Conclusion {
        private ConclusionType type;  // CONFIRMED / PROBABLE
        private String diagnosis;
        private Double confidence;
        private String uncertaintyReason;  // 不可确证时的不确定性来源
        private String reviewWindow;  // 复评时间窗
        private List<String> upgradeTriggers;  // 升级触发条件
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

### 2.5 证据分析DTO（新增）

```java
package com.aidoctor.diagnosis.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 证据分析
 */
@Data
@Builder
public class EvidenceAnalysis {
    
    /**
     * 首要假设的证据
     */
    private DiseaseEvidence primaryHypothesisEvidence;
    
    /**
     * 主要备选诊断的证据
     */
    private List<DiseaseEvidence> alternativesEvidence;
    
    /**
     * 必须排除的高危诊断的证据
     */
    private DiseaseEvidence mustExcludeEvidence;
    
    @Data
    @Builder
    public static class DiseaseEvidence {
        private String disease;
        private List<SupportingEvidence> supportingSymptoms;
        private List<SupportingEvidence> supportingSigns;
        private List<SupportingEvidence> supportingExaminations;
        private ProfileMatch profileMatch;
        private List<OpposingEvidence> opposingEvidence;
        private EvidenceStrength strength;
    }
    
    @Data
    @Builder
    public static class SupportingEvidence {
        private String item;
        private String type;
        private String strength;  // strong / medium / weak
    }
    
    @Data
    @Builder
    public static class OpposingEvidence {
        private String item;
        private String type;
        private String strength;
    }
    
    @Data
    @Builder
    public static class ProfileMatch {
        private Boolean matched;
        private String reason;
    }
    
    @Data
    @Builder
    public static class EvidenceStrength {
        private List<String> strong;
        private List<String> medium;
        private List<String> weak;
    }
}
```

### 2.6 健康筛查流程（A路径）相关DTO

#### 2.6.1 A1需求分类请求（DemandClassificationRequest）

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

#### 2.6.2 A1需求分类响应（DemandClassificationResult）

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

#### 2.6.3 A2健康画像收集请求（HealthProfileCollectionRequest）

```java
package com.aidoctor.wellness.dto.request;

import lombok.Data;
import java.util.Map;

/**
 * A2｜收集健康画像请求
 */
@Data
public class HealthProfileCollectionRequest {
    
    /**
     * 用户ID（必填）
     */
    private String userId;
    
    /**
     * CDP ID（必填）
     */
    private String cdpId;
    
    /**
     * 初始数据（可选）
     */
    private Map<String, Object> initialData;
}
```

#### 2.6.4 A2健康画像响应（HealthProfileResponse）

```java
package com.aidoctor.wellness.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * A2｜健康画像响应
 */
@Data
@Builder
public class HealthProfileResponse {
    
    /**
     * 健康画像
     */
    private HealthProfile profile;
    
    /**
     * 完整度（0-1）
     */
    private Double completeness;
    
    /**
     * 是否完整
     */
    private Boolean isComplete;
    
    /**
     * 缺失的必填字段
     */
    private List<String> missingRequiredFields;
    
    /**
     * 补充提问
     */
    private List<String> followUpQuestions;
    
    /**
     * 下一步操作
     */
    private String nextStep;
    
    @Data
    @Builder
    public static class HealthProfile {
        private BasicInfo basicInfo;
        private HealthHistory healthHistory;
        private FamilyHistory familyHistory;
        private Lifestyle lifestyle;
        private MedicationHistory medicationHistory;
        private AllergyHistory allergyHistory;
        
        @Data
        @Builder
        public static class BasicInfo {
            private Integer age;
            private String gender;
            private Double height;
            private Double weight;
            private Double bmi;
        }
        
        @Data
        @Builder
        public static class HealthHistory {
            private List<String> chronicDiseases;
            private List<String> pastIllnesses;
            private List<String> surgeries;
        }
        
        @Data
        @Builder
        public static class FamilyHistory {
            private List<String> diseases;
        }
        
        @Data
        @Builder
        public static class Lifestyle {
            private String smokingStatus;
            private String drinkingStatus;
            private String exerciseFrequency;
            private String diet;
        }
        
        @Data
        @Builder
        public static class MedicationHistory {
            private List<String> currentMedications;
            private List<String> pastMedications;
        }
        
        @Data
        @Builder
        public static class AllergyHistory {
            private List<String> allergies;
        }
    }
}
```

#### 2.6.5 A3分支执行请求（BranchExecutionRequest）

```java
package com.aidoctor.wellness.dto.request;

import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * A3｜执行分支请求
 */
@Data
public class BranchExecutionRequest {
    
    /**
     * 用户ID（必填）
     */
    private String userId;
    
    /**
     * CDP ID（必填）
     */
    private String cdpId;
    
    /**
     * 需求类型（必填）
     */
    @NotNull(message = "需求类型不能为空")
    private DemandType demandType;
    
    /**
     * 健康画像（可选，如果已收集）
     */
    private Map<String, Object> healthProfile;
    
    public enum DemandType {
        SCREENING_RECOMMENDATION,
        HEALTH_GOAL_MANAGEMENT,
        PLANNED_HEALTH_NEEDS
    }
}
```

#### 2.6.6 A3分支执行响应（BranchExecutionResult）

```java
package com.aidoctor.wellness.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * A3｜执行分支响应
 */
@Data
@Builder
public class BranchExecutionResult {
    
    /**
     * 需求类型
     */
    private DemandType demandType;
    
    /**
     * 风险等级（仅筛查建议分支）
     */
    private String riskLevel;
    
    /**
     * 筛查建议（仅筛查建议分支）
     */
    private List<ScreeningRecommendation> recommendations;
    
    /**
     * 健康目标（仅健康目标管理分支）
     */
    private List<HealthGoal> currentGoals;
    
    /**
     * 目标进展（仅健康目标管理分支）
     */
    private List<GoalProgress> progressList;
    
    /**
     * 目标调整建议（仅健康目标管理分支）
     */
    private List<GoalAdjustment> adjustments;
    
    /**
     * 计划性需求（仅计划性健康需求分支）
     */
    private List<PlannedHealthNeed> needs;
    
    /**
     * 准备建议（仅计划性健康需求分支）
     */
    private List<PreparationAdvice> adviceList;
    
    @Data
    @Builder
    public static class ScreeningRecommendation {
        private String name;
        private String description;
        private Double priority;
        private String reason;
    }
    
    @Data
    @Builder
    public static class AbnormalFinding {
        private String indicator;
        private String value;
        private String normalRange;
        private String significance;
    }
    
    @Data
    @Builder
    public static class HealthGoal {
        private String goal;
        private String targetValue;
        private String currentValue;
    }
    
    @Data
    @Builder
    public static class GoalProgress {
        private String goal;
        private ProgressStatus status;
        private Double progress;
    }
    
    public enum ProgressStatus {
        GOOD, FAIR, POOR
    }
    
    @Data
    @Builder
    public static class GoalAdjustment {
        private String goal;
        private String suggestion;
    }
    
    @Data
    @Builder
    public static class PlannedHealthNeed {
        private String need;
        private String description;
    }
    
    @Data
    @Builder
    public static class PreparationAdvice {
        private String advice;
        private String priority;
    }
    
    public enum DemandType {
        SCREENING_RECOMMENDATION,
        HEALTH_GOAL_MANAGEMENT,
        PLANNED_HEALTH_NEEDS
    }
}
```

#### 2.6.7 A4统一结果响应（UnifiedResult）

```java
package com.aidoctor.wellness.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * A4｜统一结果响应
 */
@Data
@Builder
public class UnifiedResult {
    
    /**
     * CDP ID
     */
    private String cdpId;
    
    /**
     * 需求类型
     */
    private DemandType demandType;
    
    /**
     * 摘要
     */
    private String summary;
    
    /**
     * 主要内容
     */
    private Map<String, Object> mainContent;
    
    /**
     * 建议列表
     */
    private List<String> recommendations;
    
    /**
     * 下一步操作
     */
    private List<String> nextSteps;
    
    public enum DemandType {
        SCREENING_RECOMMENDATION,
        HEALTH_GOAL_MANAGEMENT,
        PLANNED_HEALTH_NEEDS
    }
}
```

#### 2.6.8 A5随访计划响应（FollowUpPlanResponse）

```java
package com.aidoctor.wellness.dto.response;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;

/**
 * A5｜随访计划响应
 */
@Data
@Builder
public class FollowUpPlanResponse {
    
    /**
     * 随访计划ID
     */
    private Long planId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * CDP ID
     */
    private String cdpId;
    
    /**
     * 随访日期
     */
    private LocalDate followUpDate;
    
    /**
     * 提醒内容
     */
    private String reminderContent;
    
    /**
     * 是否激活
     */
    private Boolean isActive;
}
```

### 2.7 工具相关DTO

#### 2.7.1 tool_0：健康状态判定结果（HealthStateAssessmentResult）

```java
package com.aidoctor.diagnosis.dto.response;

import lombok.Data;
import lombok.Builder;

/**
 * tool_0：健康状态判定结果
 */
@Data
@Builder
public class HealthStateAssessmentResult {
    /**
     * 是否需要临床诊疗态
     */
    private Boolean needsClinicalMode;
    
    /**
     * 工作态（wellness_mode/clinical_mode）
     */
    private String workMode;
    
    /**
     * 风险等级（L1-L4）
     */
    private String riskLevel;
    
    /**
     * 判定原因
     */
    private String assessmentReason;
    
    /**
     * 红旗信号列表
     */
    private List<String> redFlags;
    
    /**
     * 健康管理计划（如果是健康管理态）
     */
    private WellnessPlan wellnessPlan;
    
    /**
     * CDP ID
     */
    private String cdpId;
    
    @Data
    @Builder
    public static class WellnessPlan {
        private String riskManagement;
        private String lifestyleAdvice;
        private String followUpPlan;
    }
}
```

---

#### 2.6.2 tool_1：病例理解结果（ClinicalParsingResult）

```java
package com.aidoctor.diagnosis.dto.response;

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
     * 识别的医学概念列表
     */
    private List<MedicalConcept> concepts;
    
    /**
     * 结构化数据
     */
    private StructuredData structuredData;
    
    @Data
    @Builder
    public static class MedicalConcept {
        private String text;        // 原始文本
        private String cui;         // CUI编码
        private String type;        // symptom/disease/medication等
        private Double confidence;  // 置信度
    }
    
    @Data
    @Builder
    public static class StructuredData {
        private String chiefComplaint;
        private List<String> symptoms;
        private Map<String, Object> vitalSigns;
    }
}
```

---

#### 2.6.3 tool_2：主动问诊结果（InterviewResult）

```java
package com.aidoctor.diagnosis.dto.response;

import lombok.Data;
import lombok.Builder;

/**
 * tool_2：主动问诊结果
 */
@Data
@Builder
public class InterviewResult {
    /**
     * 追问问题
     */
    private Question question;
    
    /**
     * 信息缺口
     */
    private InformationGaps informationGaps;
    
    /**
     * 信息完整度（0-1）
     */
    private Double completeness;
    
    @Data
    @Builder
    public static class Question {
        private String question;        // 问题文本
        private String questionType;    // 问题类型
        private String priority;        // required/important/optional
        private Boolean required;       // 是否必填
    }
}
```

---

#### 2.6.4 tool_3：鉴别诊断结果（DifferentialDiagnosisResult）

```java
package com.aidoctor.diagnosis.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * tool_3：鉴别诊断结果（DR.KNOWS核心）
 */
@Data
@Builder
public class DifferentialDiagnosisResult {
    /**
     * 鉴别诊断列表（三层排序）
     */
    private ThreeLayerResult ddx;
    
    /**
     * 推理路径（DR.KNOWS核心）
     */
    private List<ReasoningPath> reasoningPaths;
    
    /**
     * 引擎结果（多引擎融合）
     */
    private Map<String, Map<String, Double>> engineResults;
    
    @Data
    @Builder
    public static class ReasoningPath {
        /**
         * 路径描述（例如：胸痛 → 心血管疾病 → 心绞痛）
         */
        private String path;
        
        /**
         * 路径相关性评分（DR.KNOWS层1）
         */
        private Double relevanceScore;
        
        /**
         * 证据强度评分（DR.KNOWS层2）
         */
        private Double evidenceStrength;
        
        /**
         * 综合评分
         */
        private Double compositeScore;
    }
}
```

---

#### 2.6.5 tool_4：检查建议结果（WorkupPlanResult）

```java
package com.aidoctor.diagnosis.dto.response;

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
    
    @Data
    @Builder
    public static class WorkupItem {
        private String testName;        // 检查名称
        private String priority;        // high/medium/low
        private Double informationGain; // 信息增益
        private String purpose;         // 检查目的
    }
}
```

---

#### 2.6.6 tool_5：治疗建议结果（TreatmentPlanResult）

```java
package com.aidoctor.diagnosis.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;

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
    
    @Data
    @Builder
    public static class TreatmentPlan {
        /**
         * 药物推荐类别（不涉及具体剂量）
         */
        private List<String> medications;
        
        /**
         * 非药物治疗建议
         */
        private List<String> nonPharmacological;
        
        /**
         * 转诊建议
         */
        private Referral referral;
    }
    
    @Data
    @Builder
    public static class Referral {
        private Boolean needed;
        private String department;
        private String reason;
    }
}
```

---

#### 2.6.7 tool_6：风险评估结果（RiskAssessmentResult）

```java
package com.aidoctor.diagnosis.dto.response;

import lombok.Data;
import lombok.Builder;

/**
 * tool_6：风险评估结果
 */
@Data
@Builder
public class RiskAssessmentResult {
    /**
     * 风险等级（L1-L4）
     */
    private String riskLevel;
    
    /**
     * 严重程度（mild/moderate/severe）
     */
    private String severity;
    
    /**
     * 紧急程度（urgent/normal）
     */
    private String urgency;
    
    /**
     * 红旗信号列表
     */
    private List<String> redFlags;
    
    /**
     * 复评计划
     */
    private ReviewPlan reviewPlan;
    
    @Data
    @Builder
    public static class ReviewPlan {
        private String defaultTime;              // 默认复评时间
        private List<String> earlyReviewConditions; // 提前复评条件
        private List<String> upgradeConditions;     // 升级触发条件
    }
}
```

---

#### 2.6.8 tool_7：可解释性结果（EvidenceChainResult）

```java
package com.aidoctor.diagnosis.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * tool_7：可解释性结果
 */
@Data
@Builder
public class EvidenceChainResult {
    /**
     * 证据链
     */
    private List<Evidence> evidenceChain;
    
    /**
     * 推理路径（DR.KNOWS核心）
     */
    private List<String> reasoningPaths;
    
    /**
     * 终点结论包
     */
    private ConclusionPackage conclusionPackage;
    
    @Data
    @Builder
    public static class Evidence {
        private String evidence;           // 证据内容
        private String type;               // support/oppose/uncertain
        private String strength;           // strong/medium/weak
        private String affectedDisease;    // 影响的疾病
    }
    
    @Data
    @Builder
    public static class ConclusionPackage {
        private Conclusion conclusion;         // 结论
        private MustExcludeStatus mustExcludeStatus; // 必须排除项状态
        private KeyEvidence keyEvidence;       // 关键依据
        private ActionAndFollowUp actionAndFollowUp; // 行动与随访
    }
}
```

---

#### 2.6.9 CDP相关DTO

##### 2.6.9.1 CDP请求（CDPRequest）

```java
package com.aidoctor.diagnosis.dto.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * CDP创建/更新请求
 */
@Data
public class CDPRequest {
    /**
     * CDP状态（可选）
     * initial/wellness_mode/clinical_mode_collecting/clinical_mode_diagnosing/clinical_mode_managing/completed/follow_up
     */
    private String cdpStatus;
    private String patientId;
    private String sessionId;
    private Map<String, Object> healthStateAssessment;
    private Map<String, Object> wellnessPlan;
    private Map<String, Object> patientState;
    private List<Map<String, Object>> ddx;
    private List<Map<String, Object>> evidenceGraph;
    private List<Map<String, Object>> workupPlan;
    private List<Map<String, Object>> managementPlan;
    private Map<String, Object> triage;
    private Map<String, Object> uncertainty;
    // ... 其他字段
}
```

##### 2.6.9.2 CDP响应（CDPResponse）

```java
package com.aidoctor.diagnosis.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * CDP响应
 */
@Data
@Builder
public class CDPResponse {
    private String cdpId;
    private Integer version;
    private String cdpStatus;
    private Map<String, Object> healthStateAssessment;
    private Map<String, Object> wellnessPlan;
    private Map<String, Object> patientState;
    private List<Map<String, Object>> ddx;
    private List<Map<String, Object>> evidenceGraph;
    private List<Map<String, Object>> workupPlan;
    private List<Map<String, Object>> managementPlan;
    private Map<String, Object> triage;
    private Map<String, Object> uncertainty;
}
```

---

### 2.7 响应DTO

#### 2.7.1 诊断响应（DiagnosisResponse）

```java
package com.aidoctor.diagnosis.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

/**
 * 诊断响应
 */
@Data
@Builder
public class DiagnosisResponse {
    
    /**
     * 响应状态码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 诊断ID
     */
    private String diagnosisId;
    
    /**
     * 诊断状态
     */
    private String status; // collecting, questioning, analyzing, completed
    
    /**
     * 信息完整度（0-1）
     */
    private Double completeness;
    
    /**
     * 追问问题（如果状态为questioning）
     */
    private QuestionResponse question;
    
    /**
     * 诊断结果（如果状态为completed）
     */
    private DiagnosisResult result;
    
    /**
     * 时间戳
     */
    private Long timestamp;
}
```

#### 2.2.2 问题响应（QuestionResponse）

```java
package com.aidoctor.diagnosis.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 追问问题响应
 */
@Data
@Builder
public class QuestionResponse {
    
    /**
     * 问题内容
     */
    private String question;
    
    /**
     * 问题类型
     * symptom_duration: 症状持续时间
     * symptom_severity: 症状严重程度
     * accompanying_symptom: 伴随症状
     * medical_history: 既往史
     * vital_sign: 生命体征
     */
    private String questionType;
    
    /**
     * 缺失信息类型
     */
    private String missingInfoType;
    
    /**
     * 选项（如果是选择题）
     */
    private List<String> options;
    
    /**
     * 是否必答
     */
    private Boolean required;
    
    /**
     * 优先级（更新版，基于信息缺口分级）
     * required: 必填缺口
     * important: 重要缺口
     * optional: 可选缺口
     */
    private String priority;
}
```

#### 2.2.3 诊断结果（DiagnosisResult）

```java
package com.aidoctor.diagnosis.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * 诊断结果
 */
@Data
@Builder
public class DiagnosisResult {
    
    /**
     * 一句话总结
     */
    private String summary;
    
    /**
     * 可能性列表（Top 3-5）（兼容旧版本）
     */
    private List<DiseasePossibility> possibilities;
    
    /**
     * 三层分层结果（新增，根据系统设计方案v2.0）
     */
    private ThreeLayerResult threeLayerResult;
    
    /**
     * 终点结论包（新增，根据系统设计方案v2.0）
     * 包含四要素：结论、必须排除项状态、关键依据、行动与随访
     */
    private ConclusionPackage conclusionPackage;
    
    /**
     * 建议检查
     */
    private ExaminationSuggestion examinationSuggestion;
    
    /**
     * 就医建议
     */
    private MedicalAdvice medicalAdvice;
    
    /**
     * 注意事项
     */
    private Precautions precautions;
    
    /**
     * 诊断推理过程
     */
    private String reasoning;
    
    @Data
    @Builder
    public static class DiseasePossibility {
        /**
         * 疾病名称
         */
        private String disease;
        
        /**
         * 可能性等级（high, medium, low）
         */
        private String level;
        
        /**
         * 可能性分数（0-1）
         */
        private Double confidence;
        
        /**
         * 支持证据
         */
        private List<String> supportingEvidence;
        
        /**
         * 反对证据
         */
        private List<String> opposingEvidence;
        
        /**
         * 缺失信息
         */
        private List<String> missingInfo;
    }
    
    @Data
    @Builder
    public static class ExaminationSuggestion {
        /**
         * 优先检查
         */
        private List<ExaminationItem> priorityExaminations;
        
        /**
         * 可选检查
         */
        private List<ExaminationItem> optionalExaminations;
        
        /**
         * 检查说明
         */
        private String explanation;
    }
    
    @Data
    @Builder
    public static class ExaminationItem {
        private String name;
        private String purpose; // 确诊/排除/评估
        private String priority; // high, medium, low
        private String reason;
    }
    
    @Data
    @Builder
    public static class MedicalAdvice {
        /**
         * 建议科室
         */
        private String department;
        
        /**
         * 就医时机
         */
        private String timing; // 尽快/2-3天内/1周内
        
        /**
         * 就医准备
         */
        private MedicalPreparation preparation;
        
        /**
         * 就医摘要（SBAR格式）
         */
        private String sbarSummary;
    }
    
    @Data
    @Builder
    public static class MedicalPreparation {
        private List<String> documents;
        private List<String> questions;
    }
    
    @Data
    @Builder
    public static class Precautions {
        /**
         * 观察要点
         */
        private List<String> observationPoints;
        
        /**
         * 危险信号
         */
        private List<String> dangerSigns;
        
        /**
         * 免责声明
         */
        private String disclaimer;
    }
}
```

#### 2.2.4 检查方案响应（ExaminationPlanResponse）

```java
package com.aidoctor.diagnosis.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * 检查方案响应
 */
@Data
@Builder
public class ExaminationPlanResponse {
    
    private Integer code;
    private String message;
    
    /**
     * 方案ID
     */
    private Long planId;
    
    /**
     * 方案名称
     */
    private String planName;
    
    /**
     * 方案类型
     */
    private String planType;
    
    /**
     * 检查项目列表
     */
    private List<PlanItem> planItems;
    
    /**
     * 目标疾病或健康状态
     */
    private Map<String, Object> targetConditions;
    
    @Data
    @Builder
    public static class PlanItem {
        private String item;
        private String purpose;
        private String priority; // high, medium, low
        private String reason;
    }
}
```

#### 2.2.5 检查记录响应（ExaminationRecordResponse）

```java
package com.aidoctor.diagnosis.dto.response;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import java.util.Map;

/**
 * 检查记录响应
 */
@Data
@Builder
public class ExaminationRecordResponse {
    
    private Integer code;
    private String message;
    
    private Long id;
    private String userId;
    private String examinationType;
    private LocalDate examinationDate;
    
    /**
     * 报告文件路径
     */
    private String reportFilePath;
    
    /**
     * 结构化数据
     */
    private Map<String, Object> structuredData;
    
    /**
     * 解读结果
     */
    private InterpretationResult interpretationResult;
    
    @Data
    @Builder
    public static class InterpretationResult {
        /**
         * 异常指标列表
         */
        private List<String> abnormalIndicators;
        
        /**
         * 临床意义
         */
        private Map<String, String> clinicalSignificance;
        
        /**
         * 可能的疾病方向
         */
        private List<String> possibleDiseases;
        
        /**
         * 建议
         */
        private List<String> suggestions;
    }
}
```

### 2.3 诊断引擎DTO

#### 2.3.1 诊断引擎请求（DiagnosisEngineRequest）

```java
package com.aidoctor.diagnosis.dto.engine;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * 诊断引擎请求
 */
@Data
@Builder
public class DiagnosisEngineRequest {
    
    /**
     * 症状信息
     */
    private Map<String, Object> symptomInfo;
    
    /**
     * 生命体征
     */
    private Map<String, Object> vitalSigns;
    
    /**
     * 检查结果
     */
    private List<Map<String, Object>> examinationResults;
    
    /**
     * 健康档案
     */
    private Map<String, Object> healthProfile;
}
```

#### 2.3.2 诊断引擎响应（DiagnosisEngineResult）

```java
package com.aidoctor.diagnosis.dto.engine;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

/**
 * 诊断引擎响应
 */
@Data
@Builder
public class DiagnosisEngineResult {
    
    /**
     * 可能性列表（疾病 -> 分数）
     */
    private Map<String, Double> possibilities;
    
    /**
     * 各引擎结果
     */
    private EngineResults engineResults;
    
    @Data
    @Builder
    public static class EngineResults {
        private Map<String, Object> ruleEngine;
        private Map<String, Object> knowledgeGraph;
        private Map<String, Object> statistical;
        private Map<String, Object> llm;
        private Map<String, Object> differential;
    }
}
```

---

### 1.10 知识对象实体（Knowledge Object - KO）

**对应表**：`knowledge_object`（存储在Neo4j中，MySQL/Oracle存储元数据）

> **说明**：知识对象（KO）是知识演化与维护系统的核心数据结构，存储在Neo4j知识图谱中，支持版本化管理和可追溯性。

**Neo4j节点结构**：
```cypher
(:KnowledgeObject {
  ko_id: "KO_001",
  ko_type: "rule",
  content: {...},
  concept_ids: ["CUI_001", "ICD_I20.0"],
  provenance: [...],
  status: "published",
  kg_version: "v2.1",
  impact_scope: ["DDx", "workup"],
  downstream_bindings: [...],
  release_id: "v2.1"
})
```

**downstream_bindings字段详解**：

**字段结构**：
```json
{
  "downstream_bindings": [
    {
      "binding_type": "tool",           // tool / rule_engine / pathway_template
      "binding_id": "tool_3",          // 工具ID或规则引擎ID
      "binding_name": "鉴别诊断工具",    // 名称
      "usage_context": "ddx_candidate_generation",  // 使用上下文
      "binding_strength": "required"   // required / optional / conditional
    },
    {
      "binding_type": "pathway_template",
      "binding_id": "pathway_acute_mi",
      "binding_name": "急性心肌梗死路径",
      "usage_context": "workup_planning",
      "binding_strength": "required"
    }
  ]
}
```

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `binding_type` | String | 绑定类型：tool（工具）/ rule_engine（规则引擎）/ pathway_template（路径模板） |
| `binding_id` | String | 绑定ID：工具ID（如tool_3）、规则引擎ID、路径模板ID |
| `binding_name` | String | 绑定名称：便于识别的名称 |
| `usage_context` | String | 使用上下文：描述KO在组件中的使用场景 |
| `binding_strength` | String | 绑定强度：required（必需）/ optional（可选）/ conditional（条件） |

**作用**：
- **依赖分析**：明确知道哪些组件依赖这条KO
- **影响范围评估**：评估KO变更的影响范围
- **局部回滚**：回滚时可以精确知道需要更新哪些组件
- **变更通知**：KO变更时可以通知相关组件

**维护方式**：
- 在KO发布时自动分析依赖关系
- 在工具/规则引擎注册时建立绑定
- 在KO变更时更新绑定关系

#### 1.10.1 Neo4j存储实现方案（三个知识库区的label分区机制）

**核心问题**：三个知识库区（Sandbox/Staging/Production）在存储层的实现方式

**推荐方案：方案Y（一套物理库 + release/label分区 + current_release指针）**

**存储架构**：
- **一套Neo4j物理库**：所有知识数据存储在同一个Neo4j实例中
- **release/label分区**：通过Neo4j的label机制实现分区
  - Sandbox：`(:KnowledgeObject:Release_Sandbox)`
  - Staging：`(:KnowledgeObject:Release_Staging)`
  - Production：`(:KnowledgeObject:Release_v1)`、`(:KnowledgeObject:Release_v2.1)`等
- **current_release指针**：在ReleaseMetadata元数据表中维护当前生产版本指针
  ```cypher
  (:ReleaseMetadata {
    release_id: "v2.1",
    status: "active",
    created_at: timestamp,
    is_current: true
  })
  ```

**查询方式**：
- **生产查询**：`MATCH (ko:KnowledgeObject:Release_v2.1) WHERE ko.status = 'published'`
- **通过current_release指针查询**：
  ```cypher
  MATCH (rm:ReleaseMetadata {is_current: true})
  MATCH (ko:KnowledgeObject)
  WHERE ko.release_id = rm.release_id AND ko.status = 'published'
  RETURN ko
  ```
- **Sandbox查询**：`MATCH (ko:KnowledgeObject:Release_Sandbox) RETURN ko`
- **Staging查询**：`MATCH (ko:KnowledgeObject:Release_Staging) RETURN ko`

**发布流程**：
1. Staging中的KO标记为`Release_Staging`
2. 通过Publish Gate后，创建新release（如`Release_v2.1`）
3. 将Staging中的KO复制并标记为新release（写时复制）
   ```cypher
   // 复制Staging中的KO到新release
   MATCH (ko:KnowledgeObject:Release_Staging {status: 'verified'})
   CREATE (new_ko:KnowledgeObject:Release_v2.1)
   SET new_ko = ko,
       new_ko.release_id = 'v2.1',
       new_ko.status = 'published',
       new_ko.kg_version = 'v2.1'
   ```
4. 更新`current_release`指针指向新release
   ```sql
   -- 更新ReleaseMetadata表
   UPDATE release_metadata SET is_current = false WHERE is_current = true;
   UPDATE release_metadata SET is_current = true WHERE release_id = 'v2.1';
   ```
5. 旧release保留，支持回滚

**回滚机制**：
- **全局回滚**：只需修改`current_release`指针，指向旧release
  ```sql
  -- 全局回滚：修改current_release指针
  UPDATE release_metadata SET is_current = false WHERE is_current = true;
  UPDATE release_metadata SET is_current = true WHERE release_id = 'v2.0';
  ```
- **局部回滚**：按ko_id在新release中标记为deprecated，查询时fallback到旧release
  ```cypher
  // 局部回滚：标记KO为deprecated
  MATCH (ko:KnowledgeObject:Release_v2.1 {ko_id: 'KO_001'})
  SET ko.status = 'deprecated',
      ko.deprecated_at = timestamp(),
      ko.deprecated_reason = '性能回归'
  // 查询时fallback到旧release
  MATCH (ko:KnowledgeObject {ko_id: 'KO_001'})
  WHERE ko.status = 'published' OR (ko.status = 'deprecated' AND ko.release_id = 'v2.1')
  WITH ko
  MATCH (old_ko:KnowledgeObject {ko_id: 'KO_001', release_id: 'v2.0', status: 'published'})
  RETURN COALESCE(ko, old_ko) as result
  ```
- **优势**：回滚速度快（O(1)），不需要重建数据

**运维优势**：
- **查询一致性**：所有查询在同一库中，保证ACID特性
- **回滚成本低**：只需改指针，不需要数据迁移
- **运维简单**：只需管理一套Neo4j实例
- **存储效率**：通过label分区，不需要数据复制（发布时才复制）

**注意事项**：
- **严格的分区管理**：避免跨分区污染，确保查询时使用正确的label
- **维护release元数据表**：确保current_release指针的准确性
- **定期清理过期的Sandbox数据**：避免数据积累过多
- **label命名规范**：统一使用`Release_{release_id}`格式

**MySQL/Oracle元数据表**：
```sql
-- 知识对象元数据表
CREATE TABLE knowledge_object_metadata (
    id VARCHAR(64) PRIMARY KEY,
    ko_id VARCHAR(64) UNIQUE NOT NULL,
    ko_type VARCHAR(32) NOT NULL,  -- rule/relation/pathway_template/contraindication/threshold/ddx_feature
    status VARCHAR(32) NOT NULL,  -- proposed/verified/published/deprecated
    kg_version VARCHAR(32),  -- 仅对published状态
    release_id VARCHAR(32) NOT NULL,  -- Sandbox/Staging/v1.0/v2.0等
    impact_scope JSON,  -- 影响模块数组
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_ko_id (ko_id),
    INDEX idx_status (status),
    INDEX idx_kg_version (kg_version),
    INDEX idx_release_id (release_id)
);
```

**Java实体类**（元数据部分）：
```java
package com.aidoctor.knowledge.entity;

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

/**
 * 知识对象元数据实体
 * 知识对象（KO）的元数据存储在MySQL/Oracle中
 * 实际内容存储在Neo4j知识图谱中
 */
@Entity
@Table(name = "knowledge_object_metadata", indexes = {
    @Index(name = "idx_ko_id", columnList = "ko_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_kg_version", columnList = "kg_version"),
    @Index(name = "idx_release_id", columnList = "release_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeObjectMetadata {
    
    /**
     * 元数据ID（主键）
     */
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;
    
    /**
     * 知识对象ID（唯一标识，对应Neo4j中的ko_id）
     */
    @Column(name = "ko_id", length = 64, unique = true, nullable = false)
    private String koId;
    
    /**
     * 知识对象类型
     * rule/relation/pathway_template/contraindication/threshold/ddx_feature
     */
    @Column(name = "ko_type", length = 32, nullable = false)
    private String koType;
    
    /**
     * 状态
     * proposed/verified/published/deprecated
     */
    @Column(name = "status", length = 32, nullable = false)
    private String status;
    
    /**
     * 知识版本（仅对published状态）
     */
    @Column(name = "kg_version", length = 32)
    private String kgVersion;
    
    /**
     * 发布区域ID
     * Sandbox/Staging/v1.0/v2.0等
     */
    @Column(name = "release_id", length = 32, nullable = false)
    private String releaseId;
    
    /**
     * 影响模块（JSON格式）
     * 包含：DDx/workup/treatment/risk
     */
    @Type(type = "jsonb")
    @Column(name = "impact_scope", columnDefinition = "jsonb")
    private List<String> impactScope;
    
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

### 1.11 知识提案实体（Knowledge Proposal）

**对应表**：`knowledge_proposal`

> **说明**：知识提案用于记录知识演化的触发和变更需求，由主Agent或离线任务触发。

```java
package com.aidoctor.knowledge.entity;

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
 * 知识提案实体
 * 记录知识演化的触发和变更需求
 */
@Entity
@Table(name = "knowledge_proposal", indexes = {
    @Index(name = "idx_proposal_id", columnList = "proposal_id"),
    @Index(name = "idx_dedupe_key", columnList = "dedupe_key"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeProposal {
    
    /**
     * 提案ID（主键）
     */
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;
    
    /**
     * 提案ID（业务标识）
     */
    @Column(name = "proposal_id", length = 64, unique = true, nullable = false)
    private String proposalId;
    
    /**
     * 触发原因
     * 知识覆盖缺口/冲突复核失败/证据不足/新指南发布/内部规则变更/线上监控回归
     */
    @Column(name = "trigger", length = 128, nullable = false)
    private String trigger;
    
    /**
     * 变更内容（JSON格式）
     * 对哪些ko做新增/修改/删除
     */
    @Type(type = "jsonb")
    @Column(name = "diff", columnDefinition = "jsonb")
    private Map<String, Object> diff;
    
    /**
     * 需要跑的回归/影子评测（JSON格式）
     */
    @Type(type = "jsonb")
    @Column(name = "required_tests", columnDefinition = "jsonb")
    private List<String> requiredTests;
    
    /**
     * 风险等级
     * high/medium/low
     */
    @Column(name = "risk_level", length = 32, nullable = false)
    private String riskLevel;
    
    /**
     * 去重键
     * 按病种/概念/缺口类型聚合
     */
    @Column(name = "dedupe_key", length = 128)
    private String dedupeKey;
    
    /**
     * 冷却时间窗口（秒）
     */
    @Column(name = "cooldown_window")
    private Integer cooldownWindow;
    
    /**
     * 证据快照hash
     */
    @Column(name = "evidence_snapshot", length = 128)
    private String evidenceSnapshot;
    
    /**
     * 触发次数
     */
    @Column(name = "trigger_count", nullable = false)
    private Integer triggerCount;
    
    /**
     * 状态
     * pending/processing/completed/rejected
     */
    @Column(name = "status", length = 32, nullable = false)
    private String status;
    
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

**数据库表设计**：
```sql
-- 知识提案表
CREATE TABLE knowledge_proposal (
    id VARCHAR(64) PRIMARY KEY,
    proposal_id VARCHAR(64) UNIQUE NOT NULL,
    trigger VARCHAR(128) NOT NULL,
    diff JSONB,
    required_tests JSONB,
    risk_level VARCHAR(32) NOT NULL,
    dedupe_key VARCHAR(128),
    cooldown_window INT,
    evidence_snapshot VARCHAR(128),
    trigger_count INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_proposal_id (proposal_id),
    INDEX idx_dedupe_key (dedupe_key),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

### 1.12 版本元数据实体（Release Metadata）

**对应表**：`release_metadata`

> **说明**：版本元数据用于管理知识库的版本信息，支持版本切换和回滚。

```java
package com.aidoctor.knowledge.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 版本元数据实体
 * 管理知识库的版本信息
 */
@Entity
@Table(name = "release_metadata", indexes = {
    @Index(name = "idx_release_id", columnList = "release_id"),
    @Index(name = "idx_is_current", columnList = "is_current"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseMetadata {
    
    /**
     * 元数据ID（主键）
     */
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;
    
    /**
     * 发布版本ID
     * Sandbox/Staging/v1.0/v2.0等
     */
    @Column(name = "release_id", length = 32, unique = true, nullable = false)
    private String releaseId;
    
    /**
     * 状态
     * active/deprecated/archived
     */
    @Column(name = "status", length = 32, nullable = false)
    private String status;
    
    /**
     * 是否为当前生产版本
     */
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent;
    
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

**数据库表设计**：
```sql
-- 版本元数据表
CREATE TABLE release_metadata (
    id VARCHAR(64) PRIMARY KEY,
    release_id VARCHAR(32) UNIQUE NOT NULL,
    status VARCHAR(32) NOT NULL,  -- active/deprecated/archived
    is_current BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_release_id (release_id),
    INDEX idx_is_current (is_current),
    INDEX idx_status (status)
);
```

---

## 三、数据验证规则

### 3.1 实体类验证

- 使用JPA注解进行数据库约束
- 使用Bean Validation进行业务验证

### 3.2 DTO验证

- 使用`@NotNull`、`@NotBlank`、`@Min`、`@Max`等注解
- 在Controller层使用`@Valid`进行验证

### 3.3 JSON字段格式规范

所有JSONB字段需要遵循预定义的JSON Schema，确保数据一致性。

---

## 四、数据转换

### 4.1 Entity <-> DTO转换

使用MapStruct或手动转换：
- Entity转DTO：用于返回给前端
- DTO转Entity：用于保存到数据库

### 4.2 JSON序列化/反序列化

- 使用Jackson进行JSON处理
- 配置日期时间格式
- 处理空值情况

---

**文档版本**：v3.0（基于DR.KNOWS的单主Agent + 多工具Tools架构）  
**创建日期**：2025年1月  
**更新日期**：2025年1月  
**文档定位**：AI医生系统的数据模型设计（Entity和DTO详细定义）  
**参考文档**：《AI医生系统-系统功能设计.md》、《AI医生系统-技术架构设计.md》  
**设计基础**：基于DR.KNOWS论文，采用单主Agent + 多工具Tools架构设计  
**更新说明**：根据DR.KNOWS设计，新增CDP实体和CDP版本实体，添加健康状态判定记录实体、健康筛查记录实体、随访计划实体，添加健康筛查流程（A路径）相关DTO（需求分类、健康画像收集、分支执行、统一结果、随访计划），添加工具相关的DTO（HealthStateAssessmentResult、ClinicalParsingResult、DifferentialDiagnosisResult等），更新诊断记录实体添加cdpId和工作态（WorkMode）字段。

