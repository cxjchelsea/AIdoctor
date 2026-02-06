# AI医生系统 - 技术架构设计（工具清单）

> **文档定位**：本文档是AI医生系统的**技术架构设计**的工具清单部分，包含八大工具的详细设计。  
> **业务功能**：请参考《AI医生系统-系统功能设计.md》  
> **核心目标**：详细说明每个工具的职责、输入依赖、输出结构、证据要求、建议写回字段、质量指标和可测试点。

---

## 四、工具清单（Tools Catalog）

### 4.0 工具0：健康状态判定工具（Health State Assessment Tool）

**工具ID**：`tool_0`  
**工具名称**：健康状态判定工具  
**工具类型**：Deterministic

**职责**：判断"这个人，现在需要被当成'病人'对待吗？"

> **这是医生的第一职责，发生在"诊断之前"**

**输入依赖（从CDP读取）**：
- `cdp.patient_state`（如有）
- 用户输入（初始主诉）
- 基本信息（年龄、性别等，通过ToolContext传递）

**输出payload结构**：
```json
{
  "work_mode": "wellness_mode" | "clinical_mode",
  "needs_clinical_mode": true | false,
  "risk_level": "L1" | "L2" | "L3" | "L4",
  "assessment_reason": "判定依据文本",
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

**evidence要求（必须包含）**：
- 规则依据：使用的健康状态判定规则ID和规则内容
- 风险信号来源：识别的风险信号及其来源（规则库/知识库）
- 危险信号来源：识别的危险信号及其来源（危险信号库）

**suggestedWrites（建议写回CDP的字段路径）**：
- `cdp.health_state_assessment`：健康状态判定结果（完整结构）

**质量/失败模式与降级策略**：

| 质量指标 | 目标值 | 评估方法 |
|---------|--------|---------|
| 判定准确率 | > 0.95 | 与专家判定结果对比 |
| 风险识别召回率 | > 0.98 | 宁可误报，不能漏报 |
| 响应时间 | < 5秒 | 工具执行时间 |

| 失败模式 | 降级策略 |
|---------|---------|
| 超时（> 5秒） | 默认进入`clinical_mode`，标记为"超时降级" |
| 规则匹配失败 | 使用默认规则，默认进入`clinical_mode` |
| 风险信号识别失败 | 默认L3风险等级，标记为"风险识别失败" |

**可测试点（单测/回归建议）**：
- 单测：测试各种症状严重程度的判定准确性
- 单测：测试危险信号识别的召回率
- 单测：测试超时和规则匹配失败的降级策略
- 回归：测试健康管理态和临床诊疗态路径选择的准确性
- 回归：测试入口判定流程（Step 1-5）的完整性

---

### 4.1 工具1：病例理解工具（Clinical Parsing Tool）

**工具ID**：`tool_1`  
**工具名称**：病例理解工具  
**工具类型**：Deterministic

**职责**：将非结构化的患者信息转换为结构化的临床要素

**输入依赖（从CDP读取）**：
- 用户输入（病历自由文本、对话内容）
- 检查单（通过ToolContext传递）
- 生命体征（通过ToolContext传递）
- 既往史（通过ToolContext传递）

**输出payload结构**：
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

**evidence要求（必须包含）**：
- 概念归一化来源：每个CUI编码的来源（UMLS/ICD/SNOMED）
- 提取依据：每个结构化字段的提取依据（文本片段、规则匹配）

**suggestedWrites（建议写回CDP的字段路径）**：
- `cdp.patient_state`：患者状态（完整结构，包含symptoms、signs、past_history、medications、allergies、lab_abnormalities）

**质量/失败模式与降级策略**：

| 质量指标 | 目标值 | 评估方法 |
|---------|--------|---------|
| 概念归一化准确率 | > 0.90 | 与专家标注的CUI编码对比 |
| 结构化提取完整率 | > 0.85 | 与专家标注的结构化字段对比 |
| 响应时间 | < 10秒 | 工具执行时间 |

| 失败模式 | 降级策略 |
|---------|---------|
| 概念识别失败 | 保留原始文本，标记为"未归一化"，在字段中记录原始文本 |
| 概念归一化失败 | 保留原始文本，标记为"归一化失败"，在字段中记录原始文本和归一化失败原因 |
| 结构化提取失败 | 保留原始文本，标记为"提取失败"，在字段中记录原始文本 |

**可测试点（单测/回归建议）**：
- 单测：测试各种医学概念（症状、疾病、药物）的归一化准确性
- 单测：测试结构化提取的完整率和准确率
- 单测：测试概念识别失败和归一化失败的降级策略
- 回归：测试多模态输入（文本/影像/检查报告）的理解准确性
- 回归：测试复杂病历的结构化提取准确性

---

### 4.2 工具2：主动问诊工具（Interview Tool）

**工具ID**：`tool_2`  
**工具名称**：主动问诊工具  
**工具类型**：Generative（但问诊策略基于临床决策分析，属于Deterministic）

**职责**：像医生一样问"关键问题"，补齐鉴别诊断所需证据

**输入依赖（从CDP读取）**：
- `cdp.ddx`：当前鉴别诊断列表
- `cdp.uncertainty.missing_critical_info`：缺失关键信息
- `cdp.patient_state`：当前患者状态
- 对话历史（通过ToolContext传递）

**输出payload结构**：
```json
{
  "next_question": "下一问是什么（自然语言）",
  "question_reason": "为什么问这个问题（基于临床决策分析）",
  "expected_info_gain": 0.0-1.0,
  "question_type": "追问" | "观察" | "测量",
  "answer_options": ["答案选项列表"],
  "clue_type": "差异点" | "阳性线索" | "关键阴性线索",
  "related_directions": ["相关诊断方向列表"]
}
```

**evidence要求（必须包含）**：
- 信息缺口识别依据：识别的缺失信息及其来源（CDP字段路径）
- 临床决策分析依据：信息增益计算的依据（信息论/风险/成本）
- 主诉关键线索库依据：使用的主诉关键线索库条目ID

**suggestedWrites（建议写回CDP的字段路径）**：
- `cdp.uncertainty.missing_critical_info`：缺失关键信息列表（更新）

**质量/失败模式与降级策略**：

| 质量指标 | 目标值 | 评估方法 |
|---------|--------|---------|
| 问诊质量 | > 0.85 | 与专家问诊问题对比 |
| 信息增益有效性 | > 0.80 | 问诊问题是否有助于区分DDx |
| 问诊轮次效率 | < 5轮 | 完成诊断所需轮次 |
| 响应时间 | < 3秒 | 工具执行时间 |

| 失败模式 | 降级策略 |
|---------|---------|
| 问题生成失败 | 使用模板问题，从主诉关键线索库中选择标准问题 |
| 信息增益计算失败 | 使用默认信息增益值（0.5），基于诊断候选数量选择问题 |
| LLM生成失败 | 使用模板问题，从差异点词库中选择标准问法 |

**可测试点（单测/回归建议）**：
- 单测：测试信息缺口识别的准确性
- 单测：测试信息增益计算的有效性
- 单测：测试问诊问题生成的质量
- 单测：测试问题生成失败和LLM生成失败的降级策略
- 回归：测试问诊轮次效率（完成诊断所需轮次）
- 回归：测试问诊质量（与专家问诊问题对比）

---

### 4.3 工具3：鉴别诊断工具（Differential Diagnosis Tool）

**工具ID**：`tool_3`  
**工具名称**：鉴别诊断工具  
**工具类型**：Retrieval（知识库优先）+ Generative（路径约束推理）

**职责**：生成Top-K鉴别诊断列表，每个诊断包含支持证据、反证、缺失证据

**输入依赖（从CDP读取）**：
- `cdp.patient_state`：患者状态（symptoms、signs、past_history等）
- `cdp.ddx`：当前鉴别诊断列表（如有，用于更新）

**输出payload结构**：
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
  "tier1_most_likely": [
    {
      "disease_name": "疾病名称",
      "disease_cui": "CUI编码",
      "probability": 0.0-1.0
    }
  ],
  "tier2_must_exclude": [
    {
      "disease_name": "疾病名称",
      "disease_cui": "CUI编码",
      "exclusion_reason": "排除原因"
    }
  ],
  "tier3_active_alternatives": [
    {
      "disease_name": "疾病名称",
      "disease_cui": "CUI编码",
      "probability": 0.0-1.0
    }
  ],
  "reasoning_subgroups": [
    {
      "subgroup_name": "推理子组名称",
      "covered_diseases": ["疾病列表"],
      "key_differences": ["关键差异点列表"]
    }
  ],
  "routing_paths": [
    {
      "path_id": "路径ID",
      "path_description": "路径描述",
      "key_questions": ["关键问题列表"]
    }
  ]
}
```

**evidence要求（必须包含）**：
- 知识库来源：每个诊断候选的知识库来源（主诉知识图谱/疾病知识图谱）
- 路径来源：每个诊断候选的DR.KNOWS路径来源（路径ID、路径节点序列）
- 推理路径：路径约束推理的路径ID和推理结果
- 三层分层依据：基于知识库默认层级（Tier1/Tier2/Tier3）的划分依据

**suggestedWrites（建议写回CDP的字段路径）**：
- `cdp.ddx`：鉴别诊断列表（完整结构，包含ddx_rank_list、tier1_most_likely、tier2_must_exclude、tier3_active_alternatives）
- `cdp.evidence_graph`：证据图（补充推理路径和证据关联）

**质量/失败模式与降级策略**：

| 质量指标 | 目标值 | 评估方法 |
|---------|--------|---------|
| DDx Top-1命中率 | > 0.70 | 与专家诊断对比 |
| DDx Top-3命中率 | > 0.85 | 与专家诊断对比 |
| DDx Top-5命中率 | > 0.90 | 与专家诊断对比 |
| 三层分层合理性 | > 0.80 | 与专家分层对比 |
| 路径验证有效性 | > 0.75 | 知识库候选是否有路径支持 |
| 响应时间 | < 30秒 | 工具执行时间 |

| 失败模式 | 降级策略 |
|---------|---------|
| 知识库检索失败 | 使用DR.KNOWS路径检索结果，标记为"知识库检索失败" |
| 路径检索失败 | 使用知识库候选，标记为"路径检索失败"，路径验证标记为"未验证" |
| 路径约束推理失败 | 使用路径本身作为推理结果，标记为"推理失败"，在evidence中记录失败原因 |
| 路径验证失败 | 保留知识库候选，但标记为"路径未验证"，在evidence中记录验证失败原因 |

**可测试点（单测/回归建议）**：
- 单测：测试知识库候选检索的准确性
- 单测：测试DR.KNOWS路径检索的准确性
- 单测：测试路径约束推理的准确性
- 单测：测试路径验证的有效性
- 单测：测试三层分层的合理性
- 单测：测试各种失败模式的降级策略
- 回归：测试DDx准确性（Top-1/Top-3/Top-5命中率）
- 回归：测试路径验证有效性（知识库候选是否有路径支持）

---

### 4.4 工具4：检查建议工具（Workup Planner Tool）

**工具ID**：`tool_4`  
**工具名称**：检查建议工具  
**工具类型**：Deterministic

**职责**：基于当前DDx和已有证据，建议下一步检查，并评估检查的价值

**输入依赖（从CDP读取）**：
- `cdp.ddx`：当前鉴别诊断列表
- `cdp.triage`：风险评估结果
- `cdp.patient_state`：患者状态（已有检查结果）

**输出payload结构**：
```json
{
  "workup_items": [
    {
      "test_name": "检查名称",
      "test_cui": "CUI编码",
      "purpose": "检查目的（能确认/排除哪些DDx）",
      "priority": "紧急" | "重要" | "可选",
      "expected_gain": 0.0-1.0,
      "can_confirm": ["能确认的诊断列表"],
      "can_exclude": ["能排除的诊断列表"]
    }
  ],
  "verification_plans": [
    {
      "plan_id": "计划ID",
      "target_direction": "目标诊断方向",
      "purpose": "确认" | "排除" | "升级判定",
      "verification_actions": ["验证动作列表"],
      "judgment_standard": "判断标准",
      "result_backfill_rule": "结果回填规则"
    }
  ]
}
```

**evidence要求（必须包含）**：
- 信息增益计算依据：每个检查的信息增益计算依据（能区分哪些DDx）
- 验证计划依据：验证计划的依据（知识库/规则库）

**suggestedWrites（建议写回CDP的字段路径）**：
- `cdp.workup_plan`：检查计划（完整结构，包含workup_items、verification_plans）

**质量/失败模式与降级策略**：

| 质量指标 | 目标值 | 评估方法 |
|---------|--------|---------|
| 检查建议合理性 | > 0.85 | 与专家检查建议对比 |
| 信息增益有效性 | > 0.80 | 检查是否能有效区分DDx |
| 验证计划有效性 | > 0.80 | 验证计划是否能改变排序或触发升级 |
| 响应时间 | < 15秒 | 工具执行时间 |

| 失败模式 | 降级策略 |
|---------|---------|
| 信息增益计算失败 | 使用默认检查清单，基于诊断候选选择常见检查 |
| 验证计划生成失败 | 使用默认验证计划，基于诊断方向选择标准验证动作 |

**可测试点（单测/回归建议）**：
- 单测：测试信息增益计算的有效性
- 单测：测试检查建议的合理性
- 单测：测试验证计划的生成准确性
- 单测：测试各种失败模式的降级策略
- 回归：测试检查建议合理性（与专家检查建议对比）
- 回归：测试验证计划有效性（是否能改变排序或触发升级）

---

### 4.5 工具5：治疗建议工具（Management Planner Tool）

**工具ID**：`tool_5`  
**工具名称**：治疗建议工具  
**工具类型**：Generative（但治疗方案推理基于指南，属于Deterministic）

**职责**：基于诊断结果，生成治疗方案和处置建议

**输入依赖（从CDP读取）**：
- `cdp.ddx`：鉴别诊断列表（特别是Top-1诊断）
- `cdp.triage`：风险评估结果
- `cdp.patient_state`：患者状态（用药史、过敏史等）

**输出payload结构**：
```json
{
  "management_plan": {
    "symptomatic_treatment": ["对症处理建议"],
    "medication_suggestions": [
      {
        "medication_category": "药物类别",
        "medication_name": "药物名称（不涉及具体剂量）",
        "indication": "适应症",
        "contraindication": "禁忌症"
      }
    ],
    "non_pharmacological_treatment": ["非药物治疗建议"],
    "observation_advice": ["观察建议"],
    "referral_advice": ["转诊建议"]
  },
  "treatment_rationale": "治疗方案依据（指南/知识库）"
}
```

**evidence要求（必须包含）**：
- 治疗方案依据：每个治疗建议的依据（指南ID/知识库条目ID）
- 药物推荐依据：每个药物推荐的依据（指南/知识库）

**suggestedWrites（建议写回CDP的字段路径）**：
- `cdp.management_plan`：治疗计划（完整结构）

**质量/失败模式与降级策略**：

| 质量指标 | 目标值 | 评估方法 |
|---------|--------|---------|
| 治疗方案合理性 | > 0.85 | 与专家治疗方案对比 |
| 药物推荐合理性 | > 0.90 | 与专家药物推荐对比 |
| 响应时间 | < 15秒 | 工具执行时间 |

| 失败模式 | 降级策略 |
|---------|---------|
| 治疗方案推理失败 | 使用指南推荐方案，从知识库中选择标准治疗方案 |
| 药物推荐失败 | 使用指南推荐药物，从知识库中选择标准药物推荐 |
| LLM生成失败 | 使用模板治疗方案，从知识库中选择标准治疗方案文本 |

**可测试点（单测/回归建议）**：
- 单测：测试治疗方案推理的准确性
- 单测：测试药物推荐的合理性
- 单测：测试各种失败模式的降级策略
- 回归：测试治疗方案合理性（与专家治疗方案对比）
- 回归：测试药物推荐合理性（与专家药物推荐对比）

---

### 4.6 工具6：风险评估工具（Risk Assessment Tool）

**工具ID**：`tool_6`  
**工具名称**：风险评估工具  
**工具类型**：Deterministic

**职责**：识别高危情况，评估紧急程度，决定是否需要立即升级处理

**输入依赖（从CDP读取）**：
- `cdp.patient_state`：患者状态
- `cdp.ddx`：鉴别诊断列表

**输出payload结构**：
```json
{
  "risk_level": "L1" | "L2" | "L3" | "L4",
  "red_flags": [
    {
      "red_flag_name": "红旗信号名称",
      "red_flag_severity": "L1" | "L2" | "L3" | "L4",
      "safety_message": "安全提示"
    }
  ],
  "urgency_level": "极紧急" | "紧急" | "一般" | "非紧急",
  "upgrade_conditions": ["升级触发条件列表"],
  "review_plan": {
    "default_review_time": "默认复评时间",
    "early_review_conditions": ["提前复评条件"],
    "delay_review_conditions": ["延迟复评条件"]
  }
}
```

**evidence要求（必须包含）**：
- 风险识别依据：每个风险信号的识别依据（规则库/知识库）
- 紧急程度评估依据：紧急程度评估的依据（规则/阈值）

**suggestedWrites（建议写回CDP的字段路径）**：
- `cdp.triage`：风险评估结果（完整结构，包含risk_level、red_flags、urgency_level、upgrade_conditions、review_plan）

**质量/失败模式与降级策略**：

| 质量指标 | 目标值 | 评估方法 |
|---------|--------|---------|
| 风险识别召回率 | > 0.98 | 宁可误报，不能漏报 |
| 紧急程度分级准确性 | > 0.90 | 与专家分级对比 |
| 响应时间 | < 10秒 | 工具执行时间 |

| 失败模式 | 降级策略 |
|---------|---------|
| 风险识别失败 | 默认L3风险等级，标记为"风险识别失败" |
| 紧急程度评估失败 | 默认"一般"紧急程度，标记为"评估失败" |

**可测试点（单测/回归建议）**：
- 单测：测试风险识别召回率（宁可误报，不能漏报）
- 单测：测试紧急程度分级的准确性
- 单测：测试各种失败模式的降级策略
- 回归：测试风险识别召回率（与专家风险识别对比）
- 回归：测试紧急程度分级准确性（与专家分级对比）

---

### 4.7 工具7：证据链工具（Evidence Chain Tool）

**工具ID**：`tool_7`  
**工具名称**：证据链工具  
**工具类型**：Generative（但证据链构建基于结构化数据，属于Deterministic）

**职责**：生成完整的证据链，让系统的"结论"能被复核，而不是黑箱

**输入依赖（从CDP读取）**：
- `cdp.ddx`：鉴别诊断列表
- `cdp.evidence_graph`：证据图（如有）
- `cdp.workup_plan`：检查计划
- `cdp.management_plan`：治疗计划
- `cdp.patient_state`：患者状态

**输出payload结构**：
```json
{
  "evidence_graph": {
    "evidence_nodes": [
      {
        "evidence_id": "证据ID",
        "evidence_name": "证据名称",
        "evidence_source": "证据来源（追问/观察/设备测量/线下检查）",
        "evidence_result": "证据结果",
        "evidence_direction": "支持" | "不支持" | "不确定",
        "affected_direction": "影响的诊断方向",
        "evidence_strength": "强证据" | "中证据" | "弱证据"
      }
    ],
    "reasoning_paths": [
      {
        "path_id": "路径ID",
        "path_nodes": ["路径节点序列"],
        "path_relations": ["路径关系序列"],
        "target_disease": "目标疾病"
      }
    ]
  },
  "explanation_text": "自然语言解释文本",
  "conclusion_package": {
    "conclusion": "结论",
    "must_exclude_status": "必须排除项状态",
    "key_evidence": ["关键依据（至少三条）"],
    "action_and_followup": "行动与随访"
  }
}
```

**evidence要求（必须包含）**：
- 证据来源：每个证据的来源（CDP字段路径、工具调用记录）
- 推理路径来源：每个推理路径的来源（DR.KNOWS路径ID、知识库条目ID）

**suggestedWrites（建议写回CDP的字段路径）**：
- `cdp.evidence_graph`：证据图（完整结构）
- `cdp.final_conclusion`：终点结论包（完整结构，包含conclusion、must_exclude_status、key_evidence、action_and_followup）

**质量/失败模式与降级策略**：

| 质量指标 | 目标值 | 评估方法 |
|---------|--------|---------|
| 证据链完整性 | > 0.90 | 与专家证据链对比 |
| 推理路径可追溯性 | > 0.95 | 诊断结论是否能追溯到证据 |
| 响应时间 | < 20秒 | 工具执行时间 |

| 失败模式 | 降级策略 |
|---------|---------|
| 证据链构建失败 | 使用简化证据链，只包含关键证据节点 |
| 推理路径可视化失败 | 使用文本描述推理路径，不使用可视化 |
| LLM生成失败 | 使用模板解释文本，从知识库中选择标准解释 |

**可测试点（单测/回归建议）**：
- 单测：测试证据链构建的完整性
- 单测：测试推理路径可视化的准确性
- 单测：测试终点结论包生成的完整性
- 单测：测试各种失败模式的降级策略
- 回归：测试证据链完整性（与专家证据链对比）
- 回归：测试推理路径可追溯性（诊断结论是否能追溯到证据）

---

## 相关文档

- [AI医生系统-技术架构设计-核心架构](./AI医生系统-技术架构设计-核心架构.md)
- [AI医生系统-技术架构设计-核心技术组件](./AI医生系统-技术架构设计-核心技术组件.md)
- [AI医生系统-技术架构设计-CDP数据与状态管理](./AI医生系统-技术架构设计-CDP数据与状态管理.md)
- [AI医生系统-技术架构设计-索引](./AI医生系统-技术架构设计-索引.md)

