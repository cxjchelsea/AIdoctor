# AI医生系统 - CDP数据结构设计

> **文档定位**：本文档详细设计AI医生系统的CDP（Clinical Decision Package）数据结构，包括CDP数据模型、字段详细说明、版本管理、状态转换和持久化策略。  
> **相关文档**：
> - 系统定位与核心理念：请参考《1.系统概述/系统定位与核心理念.md》
> - AgentState数据结构设计：请参考《AgentState数据结构设计.md》
> - AuditTrail数据结构设计：请参考《AuditTrail数据结构设计.md》

---

## 一、CDP概述

### 1.1 CDP定位

**CDP（Clinical Decision Package）定位**：
- **病例事实唯一事实源**：CDP是系统中所有病例事实的唯一事实源，所有工具从CDP读取数据，建议写回CDP
- **结构化数据存储**：CDP以结构化JSON格式存储，支持版本管理和状态转换
- **不可变历史**：CDP采用写时复制机制，每次更新创建新版本，历史版本不可修改

### 1.2 CDP核心原则

**CDP核心原则**：
1. **唯一事实源**：CDP是病例事实的唯一事实源，所有工具从CDP读取，建议写回CDP
2. **版本管理**：每次更新CDP都创建新版本，历史版本不可修改
3. **写时复制**：采用写时复制（Copy-on-Write）机制，每次更新创建新版本
4. **审计可追溯**：所有CDP更新都记录到AuditTrail

---

## 二、CDP数据模型

### 2.1 数据库设计

**数据库设计**（Oracle/MySQL）：

```sql
-- CDP主表
CREATE TABLE cdp (
    id VARCHAR(64) PRIMARY KEY,
    patient_id VARCHAR(64),
    session_id VARCHAR(64),
    version INT,
    -- 健康状态判定结果（tool_0：健康状态判定工具输出）
    health_state_assessment JSON,
    -- 健康管理计划（健康管理态使用）
    wellness_plan JSON,
    -- 患者状态（tool_1：病例理解工具输出）
    patient_state JSON,
    -- 鉴别诊断列表（tool_3：鉴别诊断工具输出，三层排序：首要假设/主要备选/必须排除）
    ddx JSON,
    -- 证据图（tool_7：证据链工具输出）
    evidence_graph JSON,
    -- 检查计划（tool_4：检查建议工具输出）
    workup_plan JSON,
    -- 治疗计划（tool_5：治疗建议工具输出）
    management_plan JSON,
    -- 风险评估（tool_6：风险评估工具输出）
    triage JSON,
    -- 不确定性信息
    uncertainty JSON,
    -- 审计信息（模型版本、提示词版本、知识版本、推理轨迹等）
    audit JSON,
    -- 知识引用（记录使用的知识对象和版本）
    knowledge_refs JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_patient_id (patient_id),
    INDEX idx_session_id (session_id),
    INDEX idx_version (version)
);

-- CDP版本表（支持回放）
CREATE TABLE cdp_version (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64),
    version INT,
    change_type VARCHAR(32),  -- CREATE/UPDATE
    changed_fields JSON,
    reason TEXT,
    created_at TIMESTAMP,
    INDEX idx_cdp_id (cdp_id),
    INDEX idx_version (version)
);
```

### 2.2 CDP字段路径规范

**CDP字段路径规范**：

| 字段路径 | 数据类型 | 说明 | 更新工具 |
|---------|---------|------|---------|
| `cdp.id` | String | CDP唯一标识 | 系统生成 |
| `cdp.patient_id` | String | 患者ID | 系统生成 |
| `cdp.session_id` | String | 会话ID | 系统生成 |
| `cdp.version` | Integer | 版本号 | 系统管理 |
| `cdp.health_state_assessment` | JSON | 健康状态判定结果 | tool_0 |
| `cdp.health_state_assessment.work_mode` | String | 工作态（wellness_mode/clinical_mode） | tool_0 |
| `cdp.health_state_assessment.needs_clinical_mode` | Boolean | 是否需要临床诊疗态 | tool_0 |
| `cdp.health_state_assessment.risk_level` | String | 风险等级（L1/L2/L3/L4） | tool_0 |
| `cdp.wellness_plan` | JSON | 健康管理计划 | tool_0 |
| `cdp.patient_state` | JSON | 患者状态 | tool_1 |
| `cdp.patient_state.symptoms` | Array | 症状列表 | tool_1 |
| `cdp.patient_state.signs` | Array | 体征列表 | tool_1 |
| `cdp.patient_state.past_history` | Array | 既往史 | tool_1 |
| `cdp.patient_state.medications` | Array | 用药史 | tool_1 |
| `cdp.ddx` | JSON | 鉴别诊断列表 | tool_3 |
| `cdp.ddx.rank_list` | Array | 诊断排序列表 | tool_3 |
| `cdp.ddx.tier1_most_likely` | Array | 首要假设（Tier1） | tool_3 |
| `cdp.ddx.tier2_must_exclude` | Array | 必须排除（Tier2） | tool_3 |
| `cdp.ddx.tier3_active_alternatives` | Array | 积极备选（Tier3） | tool_3 |
| `cdp.evidence_graph` | JSON | 证据图 | tool_7 |
| `cdp.workup_plan` | JSON | 检查计划 | tool_4 |
| `cdp.management_plan` | JSON | 治疗计划 | tool_5 |
| `cdp.triage` | JSON | 风险评估 | tool_6 |
| `cdp.uncertainty` | JSON | 不确定性信息 | 多个工具 |
| `cdp.uncertainty.missing_critical_info` | Array | 缺失关键信息 | tool_2 |
| `cdp.audit` | JSON | 审计信息 | 系统管理 |

---

## 三、CDP字段详细说明

### 3.1 health_state_assessment 字段

**用途**：存储健康状态判定结果（tool_0：健康状态判定工具的输出）

**数据结构**：
```json
{
  "needs_clinical_mode": true,
  "work_mode": "wellness_mode" | "clinical_mode",
  "risk_level": "L1" | "L2" | "L3" | "L4",
  "assessment_reason": "判定依据",
  "symptom_severity": "正常" | "轻度" | "中度" | "重度",
  "early_risk_signals": ["早期风险信号列表"],
  "entry_assessment": {
    "user_input": "用户自然语言输入",
    "has_symptom": true | false,
    "symptom_status": "明确无症状" | "存在症状" | "不确定",
    "clarification_needed": true | false,
    "clarification_result": "A（健康筛查）" | "B（症状诊断）" | null,
    "red_flags_hit": true | false,
    "red_flags_list": ["危险信号列表"],
    "path_selected": "A（健康筛查）" | "B（症状诊断）" | "退出线上流程"
  }
}
```

**更新时机**：
- CDP创建时：由健康状态判定服务（health-state-assessment-service）生成
- 工作态切换时：当从健康管理态升级到临床诊疗态时更新

### 3.2 wellness_plan 字段

**用途**：存储健康管理计划（健康管理态使用）

**数据结构**：
```json
{
  "risk_management": [
    {
      "risk_type": "风险类型",
      "risk_level": "风险等级",
      "management_advice": "管理建议"
    }
  ],
  "lifestyle_advice": ["生活方式建议"],
  "follow_up_plan": [
    {
      "follow_up_type": "随访类型",
      "timing": "随访时间",
      "purpose": "随访目的"
    }
  ],
  "reassurance": "安抚与解释文本",
  "upgrade_conditions": ["升级到临床诊疗态的条件"],
  "wellness_screening_path": {
    "demand_type": 1 | 2 | 3 | 4,
    "demand_type_name": "筛查建议/健康目标管理/计划性健康需求",
    "health_profile": {...},
    "branch_result": {...},
    "unified_result": {...},
    "follow_up_schedule": {...}
  }
}
```

**更新时机**：
- 健康管理态流程执行时：由健康筛查服务（WellnessScreeningService）生成
- A路径（A1-A5）执行完成后：存储健康筛查路径的执行结果

**注意**：
- 仅在`work_mode = "wellness_mode"`时使用此字段
- 当升级到临床诊疗态时，此字段保留历史记录，但不再更新

### 3.3 patient_state 字段

**用途**：存储患者状态（tool_1：病例理解工具的输出）

**数据结构**：
```json
{
  "symptoms": [
    {
      "symptom_name": "症状名称",
      "symptom_cui": "CUI编码",
      "duration": "持续时间",
      "severity": "严重度",
      "trigger": "诱因"
    }
  ],
  "signs": [
    {
      "sign_name": "体征名称",
      "sign_cui": "CUI编码",
      "value": "体征值",
      "unit": "单位"
    }
  ],
  "past_history": [
    {
      "disease_name": "疾病名称",
      "disease_cui": "CUI编码",
      "diagnosis_date": "诊断日期",
      "treatment_status": "治疗状态"
    }
  ],
  "medications": [
    {
      "medication_name": "药物名称",
      "medication_cui": "CUI编码",
      "dosage": "剂量",
      "frequency": "频率",
      "start_date": "开始日期"
    }
  ],
  "allergies": [
    {
      "allergen_name": "过敏原名称",
      "allergen_cui": "CUI编码",
      "reaction": "过敏反应"
    }
  ],
  "lab_abnormalities": [
    {
      "test_name": "检查项目名称",
      "test_cui": "CUI编码",
      "abnormal_value": "异常值",
      "abnormal_degree": "异常程度"
    }
  ]
}
```

**更新时机**：
- 病例理解工具执行时：由病例理解服务（clinical-parsing-service）生成
- 用户提供新信息时：更新相应的字段

### 3.4 ddx 字段

**用途**：存储鉴别诊断列表（tool_3：鉴别诊断工具的输出）

**数据结构**：
```json
{
  "ddx_rank_list": [
    {
      "disease_name": "疾病名称",
      "disease_cui": "CUI编码",
      "rank": 1,
      "tier": "Tier1" | "Tier2" | "Tier3",
      "probability": 0.0-1.0,
      "pros": ["支持证据列表"],
      "cons": ["反对证据列表"],
      "missing": ["缺失证据列表"]
    }
  ],
  "tier1_most_likely": [...],
  "tier2_must_exclude": [...],
  "tier3_active_alternatives": [...]
}
```

**更新时机**：
- 鉴别诊断工具执行时：由诊断引擎服务（diagnosis-engine-service）生成
- 新证据出现时：根据新证据更新DDx列表

### 3.5 其他字段

**workup_plan**：检查计划（tool_4输出）
**management_plan**：治疗计划（tool_5输出）
**triage**：风险评估（tool_6输出）
**evidence_graph**：证据图（tool_7输出）
**uncertainty**：不确定性信息（多个工具输出）
**audit**：审计信息（系统管理）
**knowledge_refs**：知识引用（记录使用的知识对象和版本）

---

## 四、CDP版本管理

### 4.1 版本管理机制

**版本管理机制**：
- **写时复制（Copy-on-Write）**：每次更新CDP时创建新版本
- **版本号递增**：版本号从1开始，每次更新递增
- **历史版本保留**：所有历史版本都保留在`cdp_version`表中
- **版本回放**：支持回放CDP的演变过程

### 4.2 CDP读取规则

**CDP读取规则**：
- 工具通过ToolContext中的`cdp_reference`字段获取CDP引用
- 工具只能读取CDP字段，不能直接修改CDP
- 工具通过ToolResult中的`suggested_writes`字段建议主Agent写回CDP

### 4.3 CDP写入规则

**CDP写入规则**：
- 只有主Agent可以写入CDP
- 主Agent根据工具的`suggested_writes`决定是否写回CDP
- 每次写回CDP都创建新版本，记录到AuditTrail
- 更新操作记录到`cdp_version`表

---

## 五、CDP状态转换

### 5.1 状态转换流程

```
创建CDP（初始版本v1）
    ↓
健康状态判定（tool_0）
    ↓
    ├─→ wellness_mode: 健康管理态
    │       ↓
    │   生成wellness_plan
    │       ↓
    │   健康管理态结束
    │
    └─→ clinical_mode: 临床诊疗态
            ↓
        病例理解（tool_1）
            ↓
        更新patient_state（v2）
            ↓
        主动问诊（tool_2）
            ↓
        鉴别诊断（tool_3）
            ↓
        更新ddx（v3）
            ↓
        检查建议（tool_4）
            ↓
        更新workup_plan（v4）
            ↓
        治疗建议（tool_5）
            ↓
        更新management_plan（v5）
            ↓
        风险评估（tool_6）
            ↓
        更新triage（v6）
            ↓
        证据链（tool_7）
            ↓
        更新evidence_graph（v7）
            ↓
        最终CDP（包含完整诊断结果）
```

---

## 六、参考文档

### 6.1 数据模型设计文档
- 《AgentState数据结构设计.md》：AgentState数据结构详细设计
- 《AuditTrail数据结构设计.md》：AuditTrail数据结构详细设计
- 《实体类与DTO设计.md》：实体类与DTO详细设计

### 6.2 工具设计文档
- 《4.工具设计/工具接口规范.md》：工具接口规范详细设计

### 6.3 架构设计文档
- 《2.架构设计/主Agent架构设计.md》：主Agent架构详细设计

---

**文档来源**：
- 原文档：《功能与技术设计/AI医生系统-技术架构设计-CDP数据与状态管理.md》6.1节
- 原文档：《功能与技术设计/AI医生系统-技术架构设计-核心架构.md》2.1节
- 创建时间：2025-01-22
- 文档版本：v1.0

