# AI医生系统 - 技术架构设计（智能体详细设计）

> **文档定位**：本文档是AI医生系统的**技术架构设计**的智能体详细设计部分，包含八大智能体的详细设计。  
> **业务功能**：请参考《AI医生系统-系统功能设计.md》  
> **核心目标**：详细说明每个智能体的职责、能力、工作流程和协作机制。

---

## 四、八大智能体详细设计

### 4.0 智能体0：健康状态判定智能体（Health State Assessment Agent）

**职责**：判断"这个人，现在需要被当成'病人'对待吗？"

> **这是医生的第一职责，发生在"诊断之前"**

**输入**：
- 用户描述的症状/不适（初始主诉）
- 基本信息（年龄、性别等）
- 健康档案（如有）

**输出**：健康状态判定
- 是否需要进入诊疗流程（`needs_clinical_mode`：true/false）
- 工作态选择（`work_mode`：wellness_mode / clinical_mode）
- 风险等级（L1/L2/L3/L4）
- 判定依据（`assessment_reason`）

**智能体能力**：

```json
{
  "agent_id": "agent_0",
  "agent_name": "健康状态判定智能体",
  "capabilities": {
    "perception": {
      "can_read_user_input": true,
      "can_read_basic_info": true,
      "can_read_health_profile": true
    },
    "reasoning": {
      "reasoning_type": "rule_based",
      "reasoning_engines": ["symptom_severity_analyzer", "risk_signal_detector", "red_flag_detector"],
      "channel": "channel_1",
      "channel_1_engines": ["rule_engine", "risk_analyzer"],
      "confidence_threshold": 0.7
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["work_mode", "risk_level"],
      "requires_consensus": false
    },
    "action": {
      "can_write_cdp": ["health_state_assessment"],
      "can_trigger_other_agents": ["agent_1", "agent_2"],
      "can_request_help": ["agent_6"],
      "can_send_messages": ["orchestrator", "broadcast"]
    }
  }
}
```

**核心功能**：
1. **AI诊断入口判定流程**（Step 1-5）：接收用户输入、识别症状、方向澄清、危险信号检查、路径输出
2. **健康状态评估**：症状严重程度评估、风险早筛、分诊决策
3. **路径切换支持**：支持健康管理态与临床诊疗态之间的动态切换

**工作流程**：

```
接收用户输入
    ↓
执行入口判定流程（Step 1-5）
    ↓
评估健康状态
    ↓
决策：wellness_mode 或 clinical_mode
    ↓
更新CDP.health_state_assessment
    ↓
如果wellness_mode → 通知协调器触发健康管理流程
    ↓
如果clinical_mode → 发送消息给病例理解智能体（agent_1）
```

**协作机制**：
- **请求帮助**：当不确定时，请求风险评估智能体（agent_6）协助
- **触发下游**：根据工作态触发不同的下游智能体
- **通知广播**：工作态切换时，广播通知所有智能体

### 4.1 智能体1：病例理解智能体（Clinical Parsing Agent）

**职责**：将非结构化的患者信息转换为结构化的临床要素

**输入**：
- 病历自由文本
- 对话内容
- 检查单
- 生命体征
- 既往史

**输出**：结构化临床要素
- 症状（症状名称、持续时间、严重度、诱因）
- 体征（客观检查结果）
- 既往史（疾病史、手术史）
- 用药史（当前用药、既往用药）
- 过敏史
- 检验异常（检查项目、异常值、异常程度）

**核心功能**：
1. **医学概念识别**：从文本中提取医学概念（症状、疾病、检查等）
2. **概念归一化**：将口语化表达转换为标准医学术语（CUI/ICD/SNOMED）
3. **结构化提取**：将自由文本转换为结构化数据
4. **多模态理解**：处理文本、影像、检查报告等多种数据

**智能体能力**：

```json
{
  "agent_id": "agent_1",
  "agent_name": "病例理解智能体",
  "capabilities": {
    "perception": {
      "can_read_user_input": true,
      "can_read_medical_records": true,
      "can_read_examination_reports": true,
      "can_read_cdp": true
    },
    "reasoning": {
      "reasoning_type": "llm_based",
      "reasoning_engines": ["medical_ner", "concept_normalizer", "structured_extractor"],
      "channel": "channel_1",
      "channel_1_engines": ["medical_ner", "concept_normalizer", "structured_extractor"],
      "confidence_threshold": 0.8
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["patient_state"],
      "requires_consensus": false
    },
    "action": {
      "can_write_cdp": ["patient_state"],
      "can_request_help": ["agent_2"],
      "can_trigger_other_agents": ["agent_2", "agent_3"]
    }
  }
}
```

**输出到CDP**：
- 更新 `patient_state` 字段，包含：症状、体征、既往史、用药史、过敏史、检验异常

### 4.2 智能体2：主动问诊智能体（Interview Agent）

**职责**：像医生一样问"关键问题"，补齐鉴别诊断所需证据

**输入**：
- 当前CDP（特别是 `ddx` 和 `uncertainty.missing_critical_info`）
- 对话历史

**输出**：问诊计划
- 下一问是什么
- 为什么问这个问题（基于临床决策分析）
- 期望得到什么信息
- 如何问（自然语言生成）

**核心功能**：
1. **信息缺口识别**：识别缺失的关键信息
2. **临床决策分析**：基于临床决策理论评估下一问的价值
3. **问诊策略生成**：决定问诊顺序和优先级
4. **自然语言生成**：生成像医生一样的追问问题

**智能体能力**：

```json
{
  "agent_id": "agent_2",
  "agent_name": "主动问诊智能体",
  "capabilities": {
    "perception": {
      "can_read_cdp": true,
      "can_read_ddx": true,
      "can_read_missing_info": true,
      "can_read_conversation_history": true
    },
    "reasoning": {
      "reasoning_type": "clinical_decision_analysis",
      "reasoning_engines": ["information_gap_identifier", "question_generator", "clinical_decision_analyzer"],
      "channel": "both",
      "channel_1_engines": ["information_gap_identifier", "clinical_decision_analyzer"],
      "channel_2_engines": ["question_generator", "nlg"],
      "confidence_threshold": 0.7
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["next_question", "question_priority"],
      "requires_consensus": false
    },
    "action": {
      "can_write_cdp": ["uncertainty.missing_critical_info"],
      "can_send_messages": ["user", "agent_3"],
      "can_request_help": ["agent_3"]
    }
  }
}
```

**临床决策分析驱动的问诊**（基于临床决策理论）：

```python
# 扩展的信息增益计算（基于临床决策理论）
IG_clinical(q|DDx, E) = IG_information(q|DDx, E) 
                      + α · IG_risk(q|DDx, E)
                      + β · IG_cost(q|DDx, E)

其中：
1. IG_information: 信息论的信息增益（原有）
2. IG_risk: 风险相关的信息增益（优先问能排除高危诊断的问题）
3. IG_cost: 成本效益相关的信息增益（考虑患者负担）

权重设置（基于医疗理论）：
- α = 0.5（风险权重较高，因为漏诊代价极大）
- β = 0.2（成本权重较低，但需要考虑患者体验）
- 信息增益权重 = 0.3（基础权重）
```

**输出到CDP**：
- 更新 `uncertainty.missing_critical_info`
- 生成问诊计划（临时字段，不持久化）

### 4.3 智能体3：鉴别诊断智能体（Differential Diagnosis Agent）

**职责**：生成Top-K鉴别诊断列表，每个诊断包含支持证据、反证、缺失证据

**输入**：
- 当前CDP的 `patient_state`
- 知识图谱推理路径（来自技术架构）

**输出**：鉴别诊断列表
- Top-K诊断（带排序/概率或置信区间）
- 每个诊断的支持证据（`pros`）
- 每个诊断的反证（`cons`）
- 每个诊断的缺失证据（`missing`）

**核心功能**：
1. **多引擎融合诊断**：
   - 规则引擎：症状组合规则匹配
   - 知识图谱引擎：基于推理路径的诊断（DR.KNOWS方法）
   - 统计模型引擎：基于历史数据的概率预测
   - 大模型引擎：在路径约束下的深度推理
   - 鉴别诊断引擎：相似疾病的区分

2. **证据分析**：
   - 支持证据识别和评分
   - 反对证据识别和评分
   - 缺失证据识别

3. **诊断排序**：
   - 基于多引擎融合结果
   - 考虑证据强度
   - 考虑不确定性

**智能体能力**：

```json
{
  "agent_id": "agent_3",
  "agent_name": "鉴别诊断智能体",
  "capabilities": {
    "perception": {
      "can_read_cdp": true,
      "can_read_patient_state": true,
      "can_read_evidence_graph": true,
      "can_read_kg_paths": true
    },
    "reasoning": {
      "reasoning_type": "multi_engine_fusion",
      "reasoning_engines": ["rule_engine", "kg_engine", "statistical_engine", "llm_engine", "differential_engine"],
      "channel": "channel_1",
      "channel_1_engines": ["rule_engine", "kg_engine", "statistical_engine", "differential_engine"],
      "confidence_threshold": 0.6
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["ddx_list", "diagnosis_ranking"],
      "requires_consensus": true
    },
    "action": {
      "can_write_cdp": ["ddx", "evidence_graph"],
      "can_trigger_other_agents": ["agent_2", "agent_4", "agent_6"],
      "can_request_consensus": ["agent_4", "agent_6"],
      "can_send_messages": ["agent_2"]
    }
  }
}
```

**推理子组组织**（基于诊断树理论）：

**医疗理论依据**：
1. **诊断树理论**（Diagnostic Tree Theory）- 诊断学中的系统化诊断方法
2. **解剖学分类**（Anatomical Classification）- 基于解剖学/生理学系统的分类
3. **病理生理机制**（Pathophysiological Mechanism）- 基于疾病机制的诊断
4. **时间特征诊断**（Temporal Pattern Diagnosis）- 基于起病方式和病程的诊断

**组织维度**（按诊断学优先级）：
1. **系统分类**（第一优先级）- 基于解剖学/生理学系统
2. **病理生理机制**（第二优先级）- 基于疾病机制
3. **起病方式与病程**（第三优先级）- 基于时间特征
4. **严重程度与风险**（贯穿所有层级）- 基于临床严重性

**输出到CDP**：
- 更新 `ddx` 字段（包含三层分层结果）
- 更新 `evidence_graph` 字段
- 生成分流路径（临时字段，用于指导问诊）

### 4.4 智能体4：检查建议智能体（Workup Planner Agent）

**职责**：基于当前DDx和已有证据，建议下一步检查，并评估检查的价值

**输入**：
- 当前CDP的 `ddx`
- 已有证据（`patient_state`）
- 风险等级（`triage`）

**输出**：检查建议
- 检查名称
- 检查目的（能确认/排除哪些DDx）
- 优先级（紧急/重要/可选）
- 预期信息增益（能区分哪些DDx）

**核心功能**：
1. **检查必要性评估**：判断是否需要进一步检查
2. **检查价值评估**：评估检查能提供多少信息增益
3. **检查优先级排序**：根据紧急程度和信息增益排序
4. **验证计划制定**：针对"最可能方向"和"必须排除方向"制定验证计划

**智能体能力**：

```json
{
  "agent_id": "agent_4",
  "agent_name": "检查建议智能体",
  "capabilities": {
    "perception": {
      "can_read_cdp": true,
      "can_read_ddx": true,
      "can_read_evidence_graph": true,
      "can_read_triage": true
    },
    "reasoning": {
      "reasoning_type": "value_based",
      "reasoning_engines": ["information_gain_calculator", "test_value_evaluator", "verification_planner"],
      "channel": "channel_1",
      "channel_1_engines": ["information_gain_calculator", "test_value_evaluator", "verification_planner"],
      "confidence_threshold": 0.7
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["workup_plan", "test_priority"],
      "requires_consensus": false
    },
    "action": {
      "can_write_cdp": ["workup_plan"],
      "can_trigger_other_agents": ["agent_3"],
      "can_request_help": ["agent_6"]
    }
  }
}
```

**输出到CDP**：
- 更新 `workup_plan` 字段（包含验证计划）

### 4.5 智能体5：治疗建议智能体（Management Planner Agent）

**职责**：基于诊断结果，生成治疗方案和处置建议

**输入**：
- 当前CDP的 `ddx`（特别是Top-1诊断）
- 患者状态（`patient_state`）
- 风险等级（`triage`）

**输出**：处置方案
- 对症处理建议
- 用药建议（不涉及具体剂量，研发阶段）
- 非药物治疗建议（生活方式、饮食、运动等）
- 观察/复诊建议
- 转诊建议（如需要）

**核心功能**：
1. **治疗方案推理**：基于疾病类型、严重程度，推理治疗方向
2. **药物推荐**：推荐药物类别（研发阶段不涉及具体剂量）
3. **非药物治疗**：生活方式、饮食、运动等建议
4. **治疗效果评估**：评估治疗方案的可能效果

**智能体能力**：

```json
{
  "agent_id": "agent_5",
  "agent_name": "治疗建议智能体",
  "capabilities": {
    "perception": {
      "can_read_cdp": true,
      "can_read_ddx": true,
      "can_read_patient_state": true,
      "can_read_triage": true
    },
    "reasoning": {
      "reasoning_type": "guideline_based",
      "reasoning_engines": ["treatment_reasoner", "drug_recommender", "lifestyle_advisor"],
      "channel": "both",
      "channel_1_engines": ["treatment_reasoner", "drug_recommender"],
      "channel_2_engines": ["lifestyle_advisor", "nlg"],
      "confidence_threshold": 0.7
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["management_plan"],
      "requires_consensus": false
    },
    "action": {
      "can_write_cdp": ["management_plan"],
      "can_request_help": ["agent_6"],
      "can_trigger_other_agents": ["agent_7"]
    }
  }
}
```

**输出到CDP**：
- 更新 `management_plan` 字段

### 4.6 智能体6：风险评估智能体（Triage Agent）

**职责**：识别高危情况，评估紧急程度，决定是否需要立即升级处理

**输入**：
- 当前CDP的 `patient_state`
- 当前DDx（`ddx`）

**输出**：风险评估
- 风险等级（L1极紧急 / L2紧急 / L3一般 / L4非紧急）
- 红旗信号列表
- 需要立即升级处理的条件

**核心功能**：
1. **高危识别**：识别需要立即就医的高危情况
2. **严重程度评估**：评估疾病的严重程度
3. **预后判断**：评估疾病预后情况
4. **紧急程度分级**：L1到L4分级

**安全原则**：
- **宁可误报，不能漏报**：高危识别召回率优先
- **及时升级**：一旦识别高危，立即建议就医
- **明确表达**：明确告知患者风险等级和处理建议

**智能体能力**：

```json
{
  "agent_id": "agent_6",
  "agent_name": "风险评估智能体",
  "capabilities": {
    "perception": {
      "can_read_cdp": true,
      "can_read_patient_state": true,
      "can_read_ddx": true,
      "can_monitor_continuously": true
    },
    "reasoning": {
      "reasoning_type": "risk_based",
      "reasoning_engines": ["high_risk_detector", "severity_assessor", "prognosis_predictor"],
      "channel": "channel_1",
      "channel_1_engines": ["high_risk_detector", "severity_assessor", "prognosis_predictor"],
      "confidence_threshold": 0.8
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["risk_level", "urgency", "upgrade_decision"],
      "requires_consensus": false,
      "priority": "high"
    },
    "action": {
      "can_write_cdp": ["triage"],
      "can_trigger_other_agents": ["agent_0"],
      "can_send_urgent_notifications": true,
      "can_interrupt_tasks": true
    }
  }
}
```

**输出到CDP**：
- 更新 `triage` 字段（包含复评与升级规则）

### 4.7 智能体7：证据链智能体（Evidence Agent）

**职责**：生成完整的证据链，让系统的"结论"能被复核，而不是黑箱

**输入**：
- 当前CDP的所有字段

**输出**：证据链
- 病历片段（支持诊断的原始信息）
- 检查异常（客观证据）
- KG路径（知识图谱推理路径）
- 指南/知识片段（医学知识依据）

**核心功能**：
1. **证据链构建**：将分散的证据组织成完整的证据链
2. **推理路径可视化**：可视化展示从症状到疾病的推理路径
3. **证据来源标注**：标注每个证据的来源和可信度
4. **不确定性表达**：明确表达诊断的不确定性来源

**可解释性要求**：
- 每个诊断结论都能追溯到证据
- 推理路径清晰可追溯
- 支持证据、反对证据、缺失证据明确
- 不确定性来源明确

**智能体能力**：

```json
{
  "agent_id": "agent_7",
  "agent_name": "证据链智能体",
  "capabilities": {
    "perception": {
      "can_read_cdp": true,
      "can_read_all_fields": true
    },
    "reasoning": {
      "reasoning_type": "evidence_synthesis",
      "reasoning_engines": ["evidence_chain_builder", "reasoning_path_visualizer", "explanation_generator"],
      "channel": "both",
      "channel_1_engines": ["evidence_chain_builder", "reasoning_path_visualizer"],
      "channel_2_engines": ["explanation_generator", "nlg"],
      "confidence_threshold": 0.7
    },
    "decision": {
      "can_make_autonomous_decisions": true,
      "decision_scope": ["evidence_graph", "explanation"],
      "requires_consensus": false
    },
    "action": {
      "can_write_cdp": ["evidence_graph"],
      "can_generate_explanation": true
    }
  }
}
```

**输出到CDP**：
- 更新 `evidence_graph` 字段（包含结构化证据清单）
- 更新 `uncertainty` 字段
- 生成终点结论包（包含四要素）

---

## 相关文档

- [AI医生系统-技术架构设计-核心架构](./AI医生系统-技术架构设计-核心架构.md)
- [AI医生系统-技术架构设计-核心技术组件](./AI医生系统-技术架构设计-核心技术组件.md)
- [AI医生系统-技术架构设计-CDP数据与状态管理](./AI医生系统-技术架构设计-CDP数据与状态管理.md)
- [AI医生系统-技术架构设计-索引](./AI医生系统-技术架构设计-索引.md)

