# AI医生系统 - 技术架构设计（CDP数据与状态管理）

> **文档定位**：本文档是AI医生系统的**技术架构设计**的CDP数据与状态管理部分，包含CDP数据结构设计、API设计、证据清单存储、回填与重排规则、回退机制、复评与升级规则、终点结论包生成，以及CDP流转与状态管理。  
> **业务功能**：请参考《AI医生系统-系统功能设计.md》  
> **核心目标**：详细说明CDP数据模型、版本管理、状态转换和持久化策略。

---

## 六、CDP数据结构设计

### 6.1 CDP数据模型

> **对应功能设计文档**：1.3 核心数据结构：Clinical Decision Package (CDP)  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第1.3节（第68-254行）

**数据库设计**（Oracle/MySQL）：

> **说明**：CDP数据结构与《AI医生系统-系统功能设计.md》中定义的CDP字段结构完全对应，确保业务功能与技术实现的一致性。

```sql
-- CDP主表
CREATE TABLE cdp (
    id VARCHAR(64) PRIMARY KEY,
    patient_id VARCHAR(64),
    session_id VARCHAR(64),
    version INT,
    -- 健康状态判定结果（脑区0输出）
    -- 包含：工作态判定、风险等级、入口判定流程结果等
    -- 对应功能设计文档：2.0节 健康状态判定
    health_state_assessment JSON,
    -- 健康管理计划（健康管理态使用）
    -- 包含：风险管理、生活方式建议、随访计划、健康筛查路径（A路径）执行结果等
    -- 对应功能设计文档：3.2节 健康管理态详细流程
    wellness_plan JSON,
    -- 患者状态（脑区A输出）
    patient_state JSON,
    -- 鉴别诊断列表（脑区C输出，三层排序：首要假设/主要备选/必须排除）
    ddx JSON,
    -- 证据图（脑区G输出）
    evidence_graph JSON,
    -- 检查计划（脑区D输出）
    workup_plan JSON,
    -- 治疗计划（脑区E输出）
    management_plan JSON,
    -- 风险评估（脑区F输出）
    triage JSON,
    -- 不确定性信息
    uncertainty JSON,
    -- 审计信息（模型版本、提示词版本、知识版本、推理轨迹等）
    audit JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- CDP版本表（支持回放）
CREATE TABLE cdp_version (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64),
    version INT,
    change_type VARCHAR(32),  -- CREATE/UPDATE
    changed_fields JSON,
    reason TEXT,
    created_at TIMESTAMP
);
```

**字段详细说明**：

#### health_state_assessment 字段

**用途**：存储健康状态判定结果（脑区0的输出）

**数据结构**（对应功能设计文档第76-93行）：
```json
{
  "needs_clinical_mode": true,
  "work_mode": "wellness_mode" | "clinical_mode",
  "risk_level": "L1/L2/L3/L4",
  "assessment_reason": "判定依据",
  "symptom_severity": "正常/轻度/中度/重度",
  "early_risk_signals": ["早期风险信号列表"],
  "entry_assessment": {
    "user_input": "用户自然语言输入",
    "has_symptom": true | false,
    "symptom_status": "明确无症状/存在症状/不确定",
    "clarification_needed": true | false,
    "clarification_result": "A（健康筛查）/B（症状诊断）",
    "red_flags_hit": true | false,
    "red_flags_list": ["危险信号列表"],
    "path_selected": "A（健康筛查）/B（症状诊断）/退出线上流程"
  }
}
```

**更新时机**：
- CDP创建时：由健康状态判定服务（health-state-assessment-service）生成
- 工作态切换时：当从健康管理态升级到临床诊疗态时更新

**相关服务**：
- `HealthStateAssessmentService.assess_health_state()`：生成健康状态判定结果
- `EntryAssessmentModule.entry_assessment()`：生成入口判定流程结果

#### wellness_plan 字段

**用途**：存储健康管理计划（健康管理态使用）

**数据结构**（对应功能设计文档第94-181行）：
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
    "health_profile": {
      "basic_info": {...},
      "past_history": [...],
      "family_history": [...],
      "lifestyle": {...},
      "medications": [...],
      "missing_fields": [...]
    },
    "branch_result": {
      "screening_recommendations": [...],
      "abnormality_grading": {...},
      "action_plan": {...},
      "scenario_plan": {...}
    },
    "unified_result": {
      "summary": "一句话总结",
      "recommended_actions": [...],
      "not_recommended_actions": [...],
      "next_review_time": "下一次复查/更新时间点",
      "exit_conditions": [...]
    },
    "follow_up_schedule": {...}
  }
}
```

**更新时机**：
- 健康管理态流程执行时：由健康筛查服务（WellnessScreeningService）生成
- A路径（A1-A5）执行完成后：存储健康筛查路径的执行结果

**相关服务**：
- `WellnessManagementEngine.generate_wellness_plan()`：生成健康管理计划
- `WellnessScreeningService.wellness_screening_workflow()`：执行健康筛查流程（A路径）

**注意**：
- 仅在`work_mode = "wellness_mode"`时使用此字段
- 当升级到临床诊疗态时，此字段保留历史记录，但不再更新

### 6.2 CDP API设计

**RESTful API**：

```python
# CDP管理API
POST   /api/v1/cdp                    # 创建CDP
GET    /api/v1/cdp/{cdp_id}            # 获取CDP
PUT    /api/v1/cdp/{cdp_id}            # 更新CDP
GET    /api/v1/cdp/{cdp_id}/versions   # 获取CDP版本历史
GET    /api/v1/cdp/{cdp_id}/replay     # 回放CDP演变过程
```

### 6.3 证据清单结构化存储

**技术实现**：
- **证据清单表**：存储结构化的证据清单
- **证据关联**：证据与诊断方向的关联关系

```python
class EvidenceListManager:
    def __init__(self):
        self.evidence_db = EvidenceDatabase()  # MySQL/Oracle
        
    def add_evidence(self, 
                    evidence: Evidence,
                    cdp_id: str) -> str:
        """
        添加证据到证据清单
        """
        evidence_id = self.evidence_db.insert_evidence({
            "cdp_id": cdp_id,
            "evidence_name": evidence.evidence_name,
            "evidence_source": evidence.evidence_source,
            "evidence_result": evidence.evidence_result,
            "evidence_direction": evidence.evidence_direction,
            "affected_direction": evidence.affected_direction,
            "evidence_strength": evidence.evidence_strength,
            "timestamp": datetime.now()
        })
        
        return evidence_id
    
    def get_evidence_list(self, cdp_id: str) -> List[Evidence]:
        """
        获取证据清单
        """
        return self.evidence_db.query_evidence_list(cdp_id)
```

**数据库设计**：

```sql
-- 证据清单表
CREATE TABLE evidence_list (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64),
    evidence_name VARCHAR(255),
    evidence_source VARCHAR(32),  -- 追问/观察/设备测量/线下检查
    evidence_result TEXT,
    evidence_direction VARCHAR(32),  -- 支持/不支持/不确定
    affected_direction VARCHAR(255),
    evidence_strength VARCHAR(32),  -- 强证据/中证据/弱证据
    timestamp TIMESTAMP,
    created_at TIMESTAMP,
    INDEX idx_cdp_id (cdp_id),
    INDEX idx_evidence_direction (evidence_direction)
);
```

### 6.4 回填与重排规则引擎

**技术实现**：
- **重排规则库**：存储重排规则
- **重排引擎**：根据新信息执行重排规则
- **证据冲突处理**：处理证据冲突并触发回退

```python
class RerankRuleEngine:
    def __init__(self):
        self.rerank_rule_db = RerankRuleDatabase()  # MySQL/Oracle
        
    def rerank_ddx(self, 
                   new_evidence: Evidence,
                   cdp: CDP) -> CDP:
        """
        根据新证据重新排序DDx
        """
        # 1. 获取重排规则
        rules = self.rerank_rule_db.query_rules(
            evidence_type=new_evidence.evidence_name,
            evidence_direction=new_evidence.evidence_direction
        )
        
        # 2. 应用重排规则
        updated_ddx = cdp.ddx.copy()
        for rule in rules:
            if rule.condition.match(new_evidence):
                # 执行重排动作
                if rule.action.type == "move_up":
                    updated_ddx = self._move_direction_up(
                        rule.action.direction,
                        rule.action.weight_adjustment,
                        updated_ddx
                    )
                elif rule.action.type == "move_down":
                    updated_ddx = self._move_direction_down(
                        rule.action.direction,
                        rule.action.weight_adjustment,
                        updated_ddx
                    )
        
        # 3. 检查证据冲突
        conflicts = self._check_evidence_conflicts(new_evidence, cdp)
        if conflicts:
            # 触发回退
            cdp = self._trigger_rollback(conflicts, cdp)
        
        # 4. 更新三层排序
        cdp.ddx = self._update_three_layer_ranking(updated_ddx)
        
        return cdp
```

**数据库设计**：

```sql
-- 回填与重排规则表
CREATE TABLE rerank_rule (
    id VARCHAR(64) PRIMARY KEY,
    evidence_type VARCHAR(255),
    evidence_direction VARCHAR(32),
    condition JSON,
    action JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_evidence_type (evidence_type)
);
```

### 6.5 回退机制引擎

**技术实现**：
- **回退条件检测**：检测是否满足回退条件
- **回退执行**：执行回退到指定阶段
- **回退记录**：记录回退原因和动作

```python
class RollbackEngine:
    def __init__(self):
        self.rollback_condition_db = RollbackConditionDatabase()  # MySQL/Oracle
        
    def check_rollback_conditions(self, cdp: CDP) -> List[RollbackCondition]:
        """
        检查回退条件
        """
        conditions = []
        
        # 1. 检查证据冲突
        if self._has_evidence_conflict(cdp):
            conditions.append(RollbackCondition(
                condition="证据冲突",
                rollback_target_step="Step 4（采集关键证据并形成排序与验证计划）",
                rollback_reason="需要重新采集证据，重新分析支持/反对证据",
                rollback_action="回到 Step 4，重新采集证据与制定验证计划"
            ))
        
        # 2. 检查症状演变
        if self._has_symptom_evolution(cdp):
            conditions.append(RollbackCondition(
                condition="症状演变",
                rollback_target_step="Step 3（组织候选集并建立分流路径）",
                rollback_reason="症状变化需要重新组织分流路径，重新生成候选集",
                rollback_action="回到 Step 3，重新组织分流路径"
            ))
        
        # 3. 检查处理无效
        if self._has_treatment_ineffective(cdp):
            conditions.append(RollbackCondition(
                condition="处理无效",
                rollback_target_step="Step 4（采集关键证据并形成排序与验证计划）",
                rollback_reason="处理无效说明诊断方向可能错误，需要重新采集证据",
                rollback_action="回到 Step 4，重新采集证据与制定验证计划"
            ))
        
        # 4. 检查必须排除项未完成
        if self._has_must_exclude_not_completed(cdp):
            conditions.append(RollbackCondition(
                condition="必须排除项未完成排除",
                rollback_target_step="Step 4（采集关键证据并形成排序与验证计划）",
                rollback_reason="必须排除的高危诊断未完成排除，需要继续验证",
                rollback_action="回到 Step 4，重新制定验证计划或强调必须执行验证"
            ))
        
        return conditions
    
    def execute_rollback(self, 
                        condition: RollbackCondition,
                        cdp: CDP) -> CDP:
        """
        执行回退
        """
        # 1. 记录回退信息
        self._record_rollback(condition, cdp)
        
        # 2. 根据回退目标步骤执行回退
        if "Step 3" in condition.rollback_target_step:
            # 重新组织分流路径
            cdp = self._reorganize_routing_path(cdp)
        elif "Step 4" in condition.rollback_target_step:
            # 重新采集证据与制定验证计划
            cdp = self._recollect_evidence(cdp)
        elif "Step 1" in condition.rollback_target_step:
            # 重新识别问题
            cdp = self._reidentify_problem(cdp)
        
        return cdp
```

**数据库设计**：

```sql
-- 回退条件清单表
CREATE TABLE rollback_condition_list (
    id VARCHAR(64) PRIMARY KEY,
    condition_name VARCHAR(255),
    trigger_rules JSON,
    rollback_target_stage VARCHAR(32),
    rollback_reason TEXT,
    rollback_action TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 回退记录表
CREATE TABLE rollback_record (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64),
    rollback_condition VARCHAR(255),
    rollback_target_stage VARCHAR(32),
    rollback_reason TEXT,
    rollback_action TEXT,
    timestamp TIMESTAMP,
    INDEX idx_cdp_id (cdp_id)
);
```

### 6.6 复评与升级规则引擎

> **对应功能设计文档**：2.6.1 复评与升级规则库  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第2.6.1节（第991-1050行）

**技术实现**：
- **复评规则库**：存储复评规则
- **复评时间计算**：计算复评时间窗
- **升级条件检测**：检测升级触发条件

```python
class ReviewAndUpgradeEngine:
    def __init__(self):
        self.review_rule_db = ReviewRuleDatabase()  # MySQL/Oracle
        
    def generate_review_plan(self, 
                            diagnosis: Diagnosis,
                            cdp: CDP) -> ReviewPlan:
        """
        生成复评计划
        """
        # 1. 查询复评规则
        rule = self.review_rule_db.query_rule(
            diagnosis_name=diagnosis.diagnosis_name
        )
        
        if not rule:
            # 使用默认规则
            rule = self._get_default_rule()
        
        # 2. 计算复评时间窗
        review_time_window = self._calculate_review_time_window(
            rule, cdp
        )
        
        # 3. 检查提前复评条件
        early_review = self._check_early_review_conditions(
            rule, cdp
        )
        
        # 4. 检查延迟复评条件
        delay_review = self._check_delay_review_conditions(
            rule, cdp
        )
        
        return ReviewPlan(
            default_time=review_time_window,
            early_review_conditions=early_review,
            delay_review_conditions=delay_review,
            upgrade_conditions=rule.upgrade_conditions
        )
    
    def check_upgrade_conditions(self, 
                                cdp: CDP) -> List[UpgradeCondition]:
        """
        检查升级触发条件
        """
        upgrade_conditions = []
        
        # 从复评规则库查询升级条件
        rules = self.review_rule_db.query_all_rules()
        
        for rule in rules:
            for condition in rule.upgrade_conditions:
                if self._match_condition(condition, cdp):
                    upgrade_conditions.append(condition)
        
        return upgrade_conditions
```

**数据库设计**：

```sql
-- 复评与升级规则库表
CREATE TABLE review_and_upgrade_rule (
    id VARCHAR(64) PRIMARY KEY,
    diagnosis_name VARCHAR(255),
    default_review_time VARCHAR(64),
    review_trigger TEXT,
    early_review_conditions JSON,
    delay_review_conditions JSON,
    upgrade_conditions JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_diagnosis (diagnosis_name)
);
```

### 6.7 终点结论包生成引擎

**技术实现**：
- **四要素生成**：生成结论、必须排除项状态、关键依据、行动与随访
- **结论包格式化**：格式化输出结论包

```python
class ConclusionPackageGenerator:
    def generate_conclusion_package(self, cdp: CDP) -> ConclusionPackage:
        """
        生成终点结论包（四要素）
        """
        # 要素1：结论
        conclusion = self._generate_conclusion(cdp)
        
        # 要素2：必须排除项状态
        must_exclude_status = self._generate_must_exclude_status(cdp)
        
        # 要素3：关键依据
        key_evidence = self._generate_key_evidence(cdp)
        
        # 要素4：行动与随访
        action_and_followup = self._generate_action_and_followup(cdp)
        
        return ConclusionPackage(
            conclusion=conclusion,
            must_exclude_status=must_exclude_status,
            key_evidence=key_evidence,
            action_and_followup=action_and_followup
        )
    
    def _generate_conclusion(self, cdp: CDP) -> Conclusion:
        """
        生成结论
        """
        top_diagnosis = cdp.ddx[0]
        
        # 判断是否可确证
        if top_diagnosis.probability >= 0.7 and len(cdp.evidence_list) >= 3:
            return Conclusion(
                type="可确证",
                diagnosis=top_diagnosis.diagnosis_name,
                confidence=top_diagnosis.probability,
                severity=self._assess_severity(top_diagnosis, cdp),
                evidence_chain=self._build_evidence_chain(cdp)
            )
        else:
            return Conclusion(
                type="不可确证",
                most_likely_direction=top_diagnosis.diagnosis_name,
                uncertainty_source=self._identify_uncertainty_source(cdp),
                missing_evidence=cdp.uncertainty.missing_critical_info
            )
    
    def _generate_key_evidence(self, cdp: CDP) -> KeyEvidence:
        """
        生成关键依据（至少三条证据）
        """
        evidence_list = cdp.evidence_list
        
        # 筛选强证据和中证据
        strong_evidence = [
            e for e in evidence_list 
            if e.evidence_strength in ["强证据", "中证据"]
        ]
        
        # 至少三条证据
        if len(strong_evidence) >= 3:
            positive_evidence = [
                e for e in strong_evidence 
                if e.evidence_direction == "支持"
            ]
            negative_evidence = [
                e for e in strong_evidence 
                if e.evidence_direction == "不支持"
            ]
        else:
            # 如果强证据不足，使用所有证据
            positive_evidence = [
                e for e in evidence_list 
                if e.evidence_direction == "支持"
            ]
            negative_evidence = [
                e for e in evidence_list 
                if e.evidence_direction == "不支持"
            ]
        
        return KeyEvidence(
            positive_evidence=positive_evidence[:3],  # 至少三条
            negative_evidence=negative_evidence,
            check_results=self._extract_check_results(cdp)
        )
```

**数据库设计**：

```sql
-- 终点结论包表
CREATE TABLE conclusion_package (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64),
    conclusion JSON,
    must_exclude_status JSON,
    key_evidence JSON,
    action_and_followup JSON,
    created_at TIMESTAMP,
    INDEX idx_cdp_id (cdp_id)
);
```

---

## 七、CDP流转与状态管理

### 7.1 CDP生命周期

**CDP生命周期流程**：

```
CDP创建（用户发起咨询）
    ↓
阶段0：健康状态判定
    - 【协调器】分配任务给健康状态判定智能体（agent_0）
    - 判定是否需要进入诊疗流程
    - 更新 health_state_assessment
    - 设置 work_mode
    ↓
    ┌──────────────┬──────────────┐
    │ 健康管理态    │ 临床诊疗态    │
    └──────────────┴──────────────┘
         │                    │
         ▼                    ▼
CDP更新（健康管理）    CDP更新（信息收集）
    - 病例理解智能体（agent_1）更新 patient_state
    - 生成 wellness_plan
    - 持续监控          - 病例理解智能体（agent_1）更新 patient_state
         │              - 主动问诊智能体（agent_2）生成问诊计划
         │                   ↓
         │              CDP更新（诊断阶段）
         │              - 鉴别诊断智能体（agent_3）更新 ddx
         │              - 检查建议智能体（agent_4）更新 workup_plan
         │              - 风险评估智能体（agent_6）更新 triage
         │                   ↓
         │              CDP更新（处置阶段）
         │              - 治疗建议智能体（agent_5）更新 management_plan
         │              - 证据链智能体（agent_7）更新 evidence_graph
         │                   ↓
         │              CDP持久化（诊断完成）
         │                   ↓
         │              CDP更新（随访阶段）
         │              - 重新运行相关智能体
         │              - 更新CDP状态
         │
         └──────────→ 升级触发（症状加重/风险信号）
                      - 更新 work_mode = clinical_mode
                      - 进入临床诊疗态流程
```

### 7.2 CDP版本管理

**版本控制机制**：

1. **版本创建**：
   - 每次CDP更新都创建新版本
   - 版本号格式：`CDP_v{timestamp}_{version}`
   - 记录版本创建原因和来源智能体

2. **版本历史**：
   - 保留所有历史版本，支持回放
   - 记录每次更新的智能体、时间戳、更新原因
   - 支持版本对比和差异分析

3. **版本回滚**：
   - 支持回滚到任意历史版本
   - 记录回滚原因和操作者
   - 回滚后重新触发相关智能体

**审计信息**：

```json
{
  "audit": {
    "cdp_version": "CDP_v20250101_001",
    "model_version": "模型版本",
    "prompt_version": "提示词版本",
    "knowledge_version": "知识版本",
    "timestamp": "时间戳",
    "reasoning_trace": "推理轨迹",
    "agent_activities": [
      {
        "agent_id": "agent_0",
        "activity": "执行的任务",
        "timestamp": "时间戳",
        "input": "输入",
        "output": "输出",
        "cdp_fields_updated": ["health_state_assessment"]
      }
    ],
    "version_history": [
      {
        "version": "CDP_v20250101_001",
        "created_by": "agent_0",
        "created_at": "时间戳",
        "reason": "健康状态判定完成",
        "changes": ["health_state_assessment"]
      }
    ]
  }
}
```

### 7.3 CDP状态转换

**状态定义**：

1. **初始状态**（`initial`）：
   - CDP刚创建，只有用户输入
   - 等待健康状态判定

2. **健康管理态**（`wellness_mode`）：
   - 健康状态判定完成，进入健康管理流程
   - CDP包含 `wellness_plan`，不包含 `ddx`

3. **临床诊疗态-信息收集**（`clinical_mode_collecting`）：
   - 进入临床诊疗态，正在收集信息
   - CDP包含 `patient_state`，可能包含部分 `ddx`

4. **临床诊疗态-诊断中**（`clinical_mode_diagnosing`）：
   - 正在执行诊断推理
   - CDP包含完整的 `ddx` 和 `evidence_graph`

5. **临床诊疗态-处置中**（`clinical_mode_managing`）：
   - 诊断完成，正在生成处置方案
   - CDP包含 `management_plan` 和 `final_conclusion`

6. **完成状态**（`completed`）：
   - 诊断流程完成，输出终点结论包
   - CDP包含完整的 `final_conclusion`

7. **随访状态**（`follow_up`）：
   - 进入随访阶段，等待复评
   - CDP可能根据新信息更新

**状态转换规则**：

```
initial
    ↓ (健康状态判定完成)
wellness_mode 或 clinical_mode_collecting
    ↓ (如果是临床诊疗态，信息收集完成)
clinical_mode_diagnosing
    ↓ (诊断完成)
clinical_mode_managing
    ↓ (处置方案生成完成)
completed
    ↓ (进入随访)
follow_up
    ↓ (可能触发升级或回退)
clinical_mode_collecting (升级) 或 任意之前状态 (回退)
```

### 7.4 CDP并发控制

**并发访问控制**：

1. **读写锁机制**：
   - 多个智能体可以同时读取CDP
   - 写入CDP需要获取写锁
   - 写锁按智能体优先级分配

2. **冲突检测**：
   - 检测多个智能体同时写入同一字段
   - 使用版本号检测并发冲突
   - 冲突时触发协商机制

3. **事务保证**：
   - CDP更新使用事务保证原子性
   - 更新失败时回滚
   - 记录所有更新操作

### 7.5 CDP持久化策略

**持久化时机**：

1. **关键节点持久化**：
   - 健康状态判定完成
   - 诊断完成
   - 终点结论包生成
   - 状态转换时

2. **定期持久化**：
   - 每隔一定时间自动持久化
   - 防止数据丢失

3. **手动持久化**：
   - 支持手动触发持久化
   - 用于重要节点保存

**持久化存储**：

- 支持多种存储后端（数据库、文件系统、对象存储）
- 支持CDP压缩和归档
- 支持CDP查询和检索

### 7.6 智能体状态管理

**智能体状态**：

每个智能体维护以下状态：

```json
{
  "agent_id": "agent_3",
  "state": {
    "status": "working" | "idle" | "waiting" | "error",
    "current_task": {
      "task_id": "task_123",
      "task_type": "diagnosis",
      "started_at": "2025-01-01T10:00:00Z",
      "deadline": "2025-01-01T10:05:00Z",
      "progress": 0.6
    },
    "task_queue": [],
    "collaboration_history": [],
    "performance_metrics": {
      "tasks_completed": 100,
      "success_rate": 0.95,
      "average_response_time": 2.5,
      "collaboration_count": 50
    }
  }
}
```

**状态同步机制**：

1. **主动推送**：智能体状态变化时，主动推送给协调器
2. **定期轮询**：协调器定期轮询智能体状态
3. **事件驱动**：通过事件总线同步状态

**状态一致性保证**：

- 使用分布式锁保证状态一致性
- 使用版本号防止状态冲突
- 使用事务保证状态更新的原子性

---

## 相关文档

- [AI医生系统-技术架构设计-核心架构](./AI医生系统-技术架构设计-核心架构.md)
- [AI医生系统-技术架构设计-智能体详细设计](./AI医生系统-技术架构设计-智能体详细设计.md)
- [AI医生系统-技术架构设计-核心技术组件](./AI医生系统-技术架构设计-核心技术组件.md)
- [AI医生系统-技术架构设计-索引](./AI医生系统-技术架构设计-索引.md)

