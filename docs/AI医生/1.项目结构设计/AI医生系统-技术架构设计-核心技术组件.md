# AI医生系统 - 技术架构设计（核心技术组件）

> **文档定位**：本文档是AI医生系统的**技术架构设计**的核心技术组件部分，包含知识图谱推理引擎、多引擎融合诊断系统、流程编排引擎、检查建议引擎和对话管理服务。  
> **业务功能**：请参考《AI医生系统-系统功能设计.md》  
> **核心目标**：详细说明各个核心技术组件的技术实现、算法设计和数据库设计。

---

## 三、核心技术组件设计

### 3.1 知识图谱推理引擎（基于DR.KNOWS）

#### 3.1.1 医学概念识别与归一化

**技术实现**：
- **工具**：QuickUMLS / cTAKES / 中文医学实体识别模型
- **功能**：从患者描述中提取医学概念，归一化到标准术语（CUI/ICD/SNOMED）
- **输出**：标准化的医学概念列表

**代码结构**：
```python
class MedicalConceptExtractor:
    def extract_concepts(self, text: str) -> List[Concept]:
        """
        从文本中提取医学概念
        """
        # 使用QuickUMLS或中文医学NER模型
        concepts = self.ner_model.extract(text)
        
        # 归一化到标准术语
        normalized_concepts = []
        for concept in concepts:
            cui = self.normalizer.normalize(concept)
            if cui:
                normalized_concepts.append(Concept(cui=cui, ...))
        
        return normalized_concepts
```

#### 3.1.2 多跳推理路径检索

**技术实现**：
- **知识图谱**：Neo4j（存储UMLS或自定义医学知识图谱）
- **路径搜索**：Cypher查询 + 图遍历算法
- **路径类型**：
  1. 症状→疾病路径（DR.KNOWS核心）
  2. 疾病→检查路径（扩展）
  3. 疾病→治疗路径（扩展）
  4. 综合推理路径（多路径融合）

**Cypher查询示例**：
```cypher
// 症状→疾病路径（2-4跳）
MATCH path = (s:Symptom)-[*2..4]->(d:Disease)
WHERE s.cui IN $symptom_cuis
RETURN path, 
       relationships(path) as rels,
       nodes(path) as nodes,
       length(path) as path_length
ORDER BY path_length
LIMIT 50
```

#### 3.1.3 三层评分体系（基于贝叶斯诊断理论）

**医疗理论依据**：贝叶斯诊断理论（Bayesian Diagnostic Theory）

**核心思想**：
诊断是一个概率推理过程，应该基于：
1. **先验概率**：疾病的患病率（常见病优先考虑）
2. **似然**：给定疾病下证据的敏感性/特异性
3. **后验概率**：给定证据下疾病的概率（贝叶斯更新）

**层1：先验概率评分**（Prior Probability Score）- 基于疾病患病率

**医疗理论依据**：
- **Occam's Razor in Medicine**：常见病优先考虑
- **疾病患病率**：不同疾病的先验概率不同

**技术实现**：
```python
class PriorProbabilityScorer:
    def score_prior_probability(self, disease: Disease, patient_context: Dict) -> float:
        """
        计算疾病的先验概率评分
        基于疾病患病率和患者特征
        """
        # 1. 基础患病率（基于流行病学数据）
        base_prevalence = self.prevalence_db.get_prevalence(
            disease.disease_cui,
            age_group=patient_context.get('age_group'),
            gender=patient_context.get('gender'),
            region=patient_context.get('region')
        )
        
        # 2. 归一化到[0, 1]区间
        max_prevalence = self.prevalence_db.get_max_prevalence()
        prior_score = log(base_prevalence + 1e-6) / log(max_prevalence + 1e-6)
        
        return prior_score
```

**层2：似然评分**（Likelihood Score）- 基于证据的敏感性/特异性

**医疗理论依据**：
- **诊断试验的准确性**：敏感性（Sensitivity）和特异性（Specificity）
- **证据的预测价值**：阳性预测值（PPV）和阴性预测值（NPV）

**技术实现**：
```python
class LikelihoodScorer:
    def score_likelihood(self, disease: Disease, evidence: Evidence) -> float:
        """
        计算证据的似然评分
        基于诊断试验的敏感性和特异性
        """
        # 1. 计算敏感性（给定疾病，证据出现的概率）
        sensitivity = self._calculate_sensitivity(disease, evidence)
        
        # 2. 计算特异性（给定非疾病，证据不出现的概率）
        specificity = self._calculate_specificity(disease, evidence)
        
        # 3. 综合评分（Youden指数）
        likelihood_score = (sensitivity + specificity) / 2
        
        # 4. 考虑证据类型
        if evidence.evidence_type == 'objective_test':
            # 客观检查权重更高
            likelihood_score *= 1.2
        elif evidence.evidence_type == 'typical_symptom_combo':
            # 典型症状组合权重中等
            likelihood_score *= 1.0
        else:
            # 主观描述权重较低
            likelihood_score *= 0.8
        
        return min(likelihood_score, 1.0)
```

**层3：后验概率评分**（Posterior Probability Score）- 基于贝叶斯更新

**医疗理论依据**：
- **贝叶斯定理**：P(D|E) = (P(E|D) · P(D)) / P(E)
- **后验概率**：给定证据下疾病的最优诊断概率

**技术实现**：
```python
class PosteriorProbabilityScorer:
    def score_posterior_probability(self, 
                                   disease: Disease,
                                   evidence_list: List[Evidence],
                                   prior_probability: float) -> float:
        """
        计算疾病的后验概率
        基于贝叶斯定理
        """
        # 1. 计算似然（所有证据的联合似然）
        likelihood = 1.0
        for evidence in evidence_list:
            evidence_likelihood = self.likelihood_scorer.score(disease, evidence)
            likelihood *= evidence_likelihood
        
        # 2. 计算证据的边际概率（归一化常数）
        evidence_marginal = self._calculate_evidence_marginal(evidence_list)
        
        # 3. 贝叶斯更新
        posterior_probability = (likelihood * prior_probability) / evidence_marginal
        
        return posterior_probability
```

**综合评分**（基于医疗理论）：
```python
class BayesianPathRanker:
    def rank_paths(self, 
                   paths: List[Path], 
                   patient_context: str,
                   current_ddx: List[Diagnosis]) -> List[RankedPath]:
        """
        基于贝叶斯诊断理论对路径进行评分和排序
        """
        ranked_paths = []
        
        for path in paths:
            disease = path.target_disease
            
            # 层1：先验概率评分
            prior_score = self.prior_scorer.score_prior_probability(
                disease, patient_context
            )
            
            # 层2：似然评分
            likelihood_score = self.likelihood_scorer.score_likelihood(
                disease, path.evidence
            )
            
            # 层3：后验概率评分（贝叶斯更新）
            posterior_score = self.posterior_scorer.score_posterior_probability(
                disease, path.evidence, prior_score
            )
            
            # 综合评分（基于医疗理论）
            # 权重设置：
            # - w_prior = 0.2（先验概率权重较低，因为可能受人群影响）
            # - w_likelihood = 0.3（似然权重中等，因为反映诊断准确性）
            # - w_posterior = 0.5（后验概率权重最高，因为是最终诊断概率）
            composite_score = (
                0.2 * prior_score +
                0.3 * likelihood_score +
                0.5 * posterior_score
            )
            
            ranked_paths.append(RankedPath(
                path=path,
                prior_score=prior_score,
                likelihood_score=likelihood_score,
                posterior_score=posterior_score,
                composite_score=composite_score
            ))
        
        # 排序并返回Top-10
        ranked_paths.sort(key=lambda x: x.composite_score, reverse=True)
        return ranked_paths[:10]
```

**医疗理论保证**：
- **贝叶斯诊断理论**：后验概率 P(D|E) 是最优的诊断决策依据
- **证据融合**：多个证据的联合似然通过贝叶斯更新得到后验概率
- **个性化诊断**：考虑患者特征（年龄、性别等）调整先验概率

#### 3.1.4 路径排序与Top-N选择

**技术实现**：
- **综合评分**：三层评分的加权组合
- **排序**：按综合评分降序排序
- **Top-N选择**：返回Top-10最相关的路径

#### 3.1.5 路径注入LLM

**技术实现**：
- **路径格式化**：将路径格式化为自然语言描述
- **Prompt构建**：将路径作为上下文注入prompt
- **受控生成**：LLM在路径约束下生成诊断

```python
class PathInjector:
    def format_paths_for_llm(self, ranked_paths: List[RankedPath]) -> str:
        """
        将路径格式化为自然语言描述
        """
        path_descriptions = []
        
        for ranked_path in ranked_paths:
            path = ranked_path.path
            description = "推理路径："
            
            for i in range(len(path.nodes) - 1):
                from_node = path.nodes[i]
                to_node = path.nodes[i + 1]
                rel = path.relationships[i]
                
                description += f"{from_node.name} --[{rel.type}]--> {to_node.name}; "
            
            description += f"（相关性评分：{ranked_path.relevance_score:.2f}）"
            path_descriptions.append(description)
        
        return "\n".join(path_descriptions)
    
    def build_enhanced_prompt(self, 
                             patient_info: str, 
                             reasoning_paths: List[RankedPath]) -> str:
        """
        构建增强的prompt，包含推理路径
        """
        paths_text = self.format_paths_for_llm(reasoning_paths)
        
        prompt = f"""
你是一位经验丰富的医生，需要根据患者的症状、体征、健康档案等信息，
结合以下医学推理路径，分析可能的疾病方向。

患者信息：
{patient_info}

医学推理路径（来自知识图谱，按相关性排序）：
{paths_text}

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

请以JSON格式返回：
{{
    "possibilities": {{
        "疾病1": 0.8,
        "疾病2": 0.6,
        "疾病3": 0.4
    }},
    "reasoning_paths_used": ["路径1", "路径2"],
    "supporting_evidence": {{
        "疾病1": ["症状1", "体征1"]
    }},
    "opposing_evidence": {{
        "疾病1": ["症状3"]
    }},
    "missing_info": ["检查X", "症状Y"],
    "recommended_tests": ["检查1", "检查2"]
}}
"""
        return prompt
```

### 3.2 多引擎融合诊断系统

#### 3.2.1 引擎架构

**五个核心引擎**：

1. **规则引擎**（Rule Engine）
   - 基于症状组合规则的快速匹配
   - 技术：规则库（MySQL/Oracle）+ 规则引擎（Drools/Python规则引擎）

2. **知识图谱引擎**（KG Engine）
   - 基于DR.KNOWS方法的推理路径诊断
   - 技术：Neo4j + 路径检索 + 三层评分

3. **统计模型引擎**（Statistical Engine）
   - 基于历史数据的概率预测
   - 技术：XGBoost/LightGBM + 特征工程

4. **大模型引擎**（LLM Engine）
   - 在路径约束下的深度推理
   - 技术：LangChain框架 + T5/ChatGPT/医学专用LLM + 路径注入
   - 实现：使用LangChain统一管理LLM调用，支持多种后端（OpenAI、ChatGLM、Ollama等）
   - 提示词管理：使用LangChain的PromptTemplateManager管理医疗诊断提示词模板

5. **鉴别诊断引擎**（Differential Engine）
   - 相似疾病的区分
   - 技术：鉴别诊断规则库 + 相似度计算

#### 3.2.2 推理子组组织

**技术实现**：
- **子组组织**：按系统来源、病程、诱因等维度组织候选方向
- **子组匹配**：基于患者信息匹配相关子组
- **子组推理**：一条关键问题能推动一个子组整体前移或后移

```python
class ReasoningSubgroupOrganizer:
    def __init__(self):
        self.subgroup_db = SubgroupDatabase()  # MySQL/Oracle
        
    def organize_subgroups(self, 
                          candidate_directions: List[Diagnosis],
                          patient_state: Dict) -> List[Subgroup]:
        """
        组织推理子组
        """
        subgroups = []
        
        # 1. 按维度组织
        by_system = self._group_by_system(candidate_directions)
        by_course = self._group_by_course(candidate_directions, patient_state)
        by_trigger = self._group_by_trigger(candidate_directions, patient_state)
        
        # 2. 选择最有效的组织方式
        best_dimension = self._select_best_dimension(
            by_system, by_course, by_trigger, patient_state
        )
        
        # 3. 为每个子组提取关键差异点
        for subgroup in best_dimension:
            subgroup.key_differences = self._extract_key_differences(
                subgroup, patient_state
            )
            subgroups.append(subgroup)
        
        return subgroups
```

#### 3.2.3 分流路径设计

**技术实现**：
- **分流路径生成**：基于推理子组生成分流路径
- **第一层分叉问题**：生成用于快速分流子组的问题
- **高危路径优先**：对必须排除的高危方向建立优先路径

```python
class RoutingPathDesigner:
    def __init__(self):
        self.routing_template_db = RoutingTemplateDatabase()  # MySQL/Oracle
        
    def design_routing_path(self, 
                           subgroups: List[Subgroup],
                           cdp: CDP) -> RoutingPath:
        """
        设计分流路径
        """
        # 1. 生成第一层分叉问题
        first_layer_questions = self._generate_first_layer_questions(
            subgroups, cdp
        )
        
        # 2. 识别高危路径
        high_risk_path = self._identify_high_risk_path(subgroups, cdp)
        
        # 3. 生成正常路径
        normal_path = self._generate_normal_path(subgroups, cdp)
        
        return RoutingPath(
            first_layer_questions=first_layer_questions,
            high_risk_path=high_risk_path,
            normal_path=normal_path
        )
```

**数据库设计**：

```sql
-- 推理子组表
CREATE TABLE reasoning_subgroup (
    id VARCHAR(64) PRIMARY KEY,
    subgroup_name VARCHAR(255),
    dimension VARCHAR(32),  -- 系统来源/病程/诱因等
    candidate_directions JSON,
    inclusion_reason TEXT,
    key_differences JSON,
    high_risk_triggers JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 分流路径模板库表
CREATE TABLE routing_path_template (
    id VARCHAR(64) PRIMARY KEY,
    chief_complaint VARCHAR(255),
    first_layer_questions JSON,
    high_risk_path JSON,
    normal_path JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_chief_complaint (chief_complaint)
);
```

#### 3.2.4 融合策略

**加权融合算法**：

```python
class MultiEngineFusion:
    def fuse_engine_results(self, 
                           engine_results: Dict[str, Dict],
                           patient_context: Dict) -> Dict:
        """
        融合多个引擎的诊断结果
        """
        # 引擎权重（可根据置信度动态调整）
        base_weights = {
            'rule_engine': 0.2,
            'kg_engine': 0.3,  # 知识图谱引擎权重较高
            'statistical_engine': 0.2,
            'llm_engine': 0.25,  # LLM引擎（在路径约束下）
            'differential_engine': 0.05
        }
        
        # 动态调整权重（基于引擎置信度）
        weights = self._adjust_weights(base_weights, engine_results)
        
        # 融合结果
        fused_possibilities = {}
        
        for engine_name, result in engine_results.items():
            weight = weights.get(engine_name, 0.1)
            possibilities = result.get('possibilities', {})
            
            for disease, confidence in possibilities.items():
                if disease not in fused_possibilities:
                    fused_possibilities[disease] = 0.0
                
                fused_possibilities[disease] += confidence * weight
        
        # 归一化
        total = sum(fused_possibilities.values())
        if total > 0:
            fused_possibilities = {
                k: v / total 
                for k, v in fused_possibilities.items()
            }
        
        # 排序，返回Top-5
        top_diseases = sorted(
            fused_possibilities.items(), 
            key=lambda x: x[1], 
            reverse=True
        )[:5]
        
        return {
            'possibilities': dict(top_diseases),
            'reasoning_paths': engine_results['kg_engine'].get('reasoning_paths', []),
            'engine_contributions': {
                engine_name: result.get('possibilities', {})
                for engine_name, result in engine_results.items()
            }
        }
```

**一致性检查**：

```python
class ConsistencyChecker:
    def check_consistency(self, engine_results: Dict[str, Dict]) -> Dict:
        """
        检查多个引擎结果的一致性
        """
        # 提取所有引擎的Top-3诊断
        top_diagnoses = {}
        for engine_name, result in engine_results.items():
            possibilities = result.get('possibilities', {})
            top_3 = sorted(possibilities.items(), 
                          key=lambda x: x[1], 
                          reverse=True)[:3]
            top_diagnoses[engine_name] = [d[0] for d in top_3]
        
        # 计算一致性
        consistency_score = self._calculate_consistency(top_diagnoses)
        
        # 识别冲突
        conflicts = self._identify_conflicts(engine_results)
        
        return {
            'consistency_score': consistency_score,
            'conflicts': conflicts,
            'is_consistent': consistency_score > 0.7
        }
```

### 3.3 5步AI循证诊断流程编排引擎（Diagnosis Workflow Orchestrator）

> **对应功能设计文档**：3.3 临床诊疗态详细流程（AI循证诊断流程）  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第3.3.1-3.3.6节  
> **参考文档**：《智能诊断A路径：AI循证诊断流程.md》  
> **核心思想**：将临床诊疗态流程组织为5步AI循证诊断流程，确保诊断过程可推理、可复用、可审计。

**职责**：编排5步AI循证诊断流程，协调各个脑区完成诊断任务

**技术实现**：

```python
class DiagnosisWorkflowOrchestrator:
    def __init__(self):
        self.parsing_service = ClinicalParsingService()  # 脑区A
        self.interview_service = DialogService()  # 脑区B
        self.ddx_engine = MultiEngineFusion()  # 脑区C
        self.workup_planner = WorkupPlanner()  # 脑区D
        self.treatment_engine = TreatmentEngine()  # 脑区E
        self.risk_assessment = RiskAssessmentEngine()  # 脑区F
        self.explanation_service = ExplanationService()  # 脑区G
        
    def execute_diagnosis_workflow(self, cdp: CDP) -> CDP:
        """
        执行5步AI循证诊断流程
        """
        # Step 1: 识别问题
        cdp = self.step1_identify_problem(cdp)
        
        # Step 2: 构建鉴别诊断候选集并分层
        cdp = self.step2_build_ddx_candidates(cdp)
        
        # Step 3: 组织候选集并建立分流路径
        cdp = self.step3_organize_routing_path(cdp)
        
        # Step 4: 采集关键证据并形成排序与验证计划
        cdp = self.step4_collect_evidence_and_plan(cdp)
        
        # Step 5: 回填证据并输出终点结论包
        cdp = self.step5_backfill_and_conclude(cdp)
        
        return cdp
```

**流程编排特点**：
- **可推理**：每个步骤都有明确的推理逻辑和依据
- **可复用**：问题清单、候选集、分流路径等可沉淀为模板库
- **可审计**：每个步骤的输出都有结构化记录，支持追溯和验证
- **可回退**：支持回退到任意步骤，确保系统不会在错误结论上"锁死"

**数据库设计**：

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

### 3.4 检查建议引擎（Workup Planner）

#### 3.4.1 检查价值评估

**技术实现**：
- **信息增益计算**：评估检查能区分哪些DDx
- **检查必要性评估**：判断是否需要进一步检查
- **检查优先级排序**：根据紧急程度和信息增益排序

```python
class WorkupPlanner:
    def plan_workup(self, 
                   current_ddx: List[Diagnosis],
                   patient_state: Dict) -> List[WorkupItem]:
        """
        生成检查建议
        """
        workup_items = []
        
        for test in self.available_tests:
            # 1. 计算信息增益
            information_gain = self._calculate_information_gain(
                test, current_ddx, patient_state
            )
            
            # 2. 评估检查必要性
            necessity = self._assess_necessity(test, current_ddx, patient_state)
            
            # 3. 评估检查优先级
            priority = self._assess_priority(test, information_gain, necessity)
            
            # 4. 确定检查目的
            can_confirm = self._get_confirmable_ddx(test, current_ddx)
            can_exclude = self._get_excludable_ddx(test, current_ddx)
            
            if information_gain > 0.1 or necessity > 0.5:  # 阈值
                workup_items.append(WorkupItem(
                    test_name=test.name,
                    purpose=f"区分{can_confirm}和{can_exclude}",
                    priority=priority,
                    expected_gain=information_gain,
                    can_confirm=can_confirm,
                    can_exclude=can_exclude
                ))
        
        # 排序并返回
        workup_items.sort(key=lambda x: (x.priority, x.expected_gain), reverse=True)
        return workup_items[:5]  # 返回Top-5
```

#### 3.4.2 验证计划制定

**技术实现**：
- **验证计划生成**：针对"最可能方向"和"必须排除方向"生成验证计划
- **验证计划评估**：评估验证计划是否能改变排序或触发升级
- **验证计划执行**：执行验证计划并回填结果

```python
class VerificationPlanner:
    def __init__(self):
        self.verification_db = VerificationDatabase()  # MySQL/Oracle
        
    def generate_verification_plan(self, 
                                   cdp: CDP) -> List[VerificationPlan]:
        """
        生成验证计划
        """
        verification_plans = []
        
        # 1. 针对最可能方向生成验证计划
        most_likely = cdp.ddx[0]  # Top-1
        plan = self._generate_plan_for_direction(
            direction=most_likely,
            purpose="确认",
            cdp=cdp
        )
        if plan and self._can_change_ranking(plan, cdp):
            verification_plans.append(plan)
        
        # 2. 针对必须排除方向生成验证计划
        must_exclude = [d for d in cdp.ddx if d.must_exclude]
        for direction in must_exclude:
            plan = self._generate_plan_for_direction(
                direction=direction,
                purpose="排除",
                cdp=cdp
            )
            if plan and self._can_change_ranking(plan, cdp):
                verification_plans.append(plan)
        
        return verification_plans
```

**数据库设计**：

```sql
-- 验证计划库表
CREATE TABLE verification_plan_library (
    id VARCHAR(64) PRIMARY KEY,
    diagnosis_name VARCHAR(255),
    verification_purpose VARCHAR(32),  -- 确认/排除/升级判定
    verification_action JSON,
    judgment_standard JSON,
    result_backfill_rule JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_diagnosis (diagnosis_name),
    INDEX idx_purpose (verification_purpose)
);
```

### 3.5 对话管理服务（Dialog Service）

#### 3.5.1 信息缺口识别

**技术实现**：
- **基于CDP的缺失信息识别**：从CDP的 `uncertainty.missing_critical_info` 获取
- **基于信息增益的问题生成**：优先问信息增益高的问题

```python
class GapFillingEngine:
    def identify_gaps(self, cdp: CDP) -> List[InformationGap]:
        """
        识别信息缺口
        """
        gaps = []
        
        # 1. 从CDP获取缺失信息
        missing_info = cdp.uncertainty.missing_critical_info
        
        # 2. 评估每个缺失信息的重要性
        for info in missing_info:
            importance = self._assess_importance(info, cdp.ddx)
            gaps.append(InformationGap(
                info_type=info,
                importance=importance
            ))
        
        # 排序
        gaps.sort(key=lambda x: x.importance, reverse=True)
        return gaps
```

#### 3.5.2 主诉关键线索库

> **对应功能设计文档**：2.2.1 主诉关键线索库  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第2.2.1节

**技术实现**：
- **线索库存储**：MySQL/Oracle存储线索库
- **线索匹配**：基于主诉和当前DDx匹配相关线索
- **线索应用**：根据线索类型执行推理动作

```python
class ChiefComplaintClueLibrary:
    def __init__(self):
        self.clue_db = ClueDatabase()  # MySQL/Oracle
        
    def get_clues(self, chief_complaint: str, current_ddx: List[Diagnosis]) -> List[Clue]:
        """
        获取主诉相关的关键线索
        """
        # 1. 从线索库查询
        clues = self.clue_db.query_clues(
            chief_complaint=chief_complaint,
            candidate_directions=[d.diagnosis_name for d in current_ddx]
        )
        
        # 2. 按线索类型分类
        difference_clues = [c for c in clues if c.clue_type == "差异点"]
        positive_clues = [c for c in clues if c.clue_type == "阳性线索"]
        negative_clues = [c for c in clues if c.clue_type == "关键阴性线索"]
        
        return {
            "difference_clues": difference_clues,
            "positive_clues": positive_clues,
            "negative_clues": negative_clues
        }
```

**数据库设计**：

```sql
-- 主诉关键线索库表
CREATE TABLE chief_complaint_clue_library (
    id VARCHAR(64) PRIMARY KEY,
    chief_complaint VARCHAR(255),
    clue_name VARCHAR(255),
    clue_type VARCHAR(32),  -- 差异点/阳性线索/关键阴性线索
    acquisition_method VARCHAR(32),  -- 追问/观察/测量
    standard_question TEXT,
    answer_options JSON,
    judgment_rule JSON,
    reasoning_action JSON,
    candidate_directions JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_chief_complaint (chief_complaint),
    INDEX idx_clue_type (clue_type)
);
```

#### 3.5.3 差异点词库与问法规范

> **对应功能设计文档**：2.2.2 差异点词库与问法规范  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第2.2.2节

**技术实现**：
- **差异点词库**：存储可问、可观察或可测量的差异点
- **问法规范**：统一问法和答案选项
- **问法生成**：基于差异点生成标准问法

```python
class DifferencePointLibrary:
    def __init__(self):
        self.difference_db = DifferenceDatabase()  # MySQL/Oracle
        
    def get_difference_points(self, chief_complaint: str) -> List[DifferencePoint]:
        """
        获取主诉相关的差异点
        """
        return self.difference_db.query_difference_points(chief_complaint)
    
    def generate_standard_question(self, difference_point: DifferencePoint) -> str:
        """
        生成标准问法
        """
        # 使用模板或LLM生成标准问法
        if difference_point.question_template:
            question = difference_point.question_template.format(
                symptom=difference_point.related_symptom
            )
        else:
            question = self.llm.generate_question(difference_point)
        
        return question
```

**数据库设计**：

```sql
-- 差异点词库表
CREATE TABLE difference_point_library (
    id VARCHAR(64) PRIMARY KEY,
    chief_complaint VARCHAR(255),
    difference_point_name VARCHAR(255),
    question_template TEXT,
    answer_options JSON,
    threshold_expression TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_chief_complaint (chief_complaint)
);
```

---

## 相关文档

- [AI医生系统-技术架构设计-核心架构](./AI医生系统-技术架构设计-核心架构.md)
- [AI医生系统-技术架构设计-智能体详细设计](./AI医生系统-技术架构设计-智能体详细设计.md)
- [AI医生系统-技术架构设计-CDP数据与状态管理](./AI医生系统-技术架构设计-CDP数据与状态管理.md)
- [AI医生系统-技术架构设计-索引](./AI医生系统-技术架构设计-索引.md)

