# AI医生系统 - 技术架构设计（核心技术组件）

> **文档定位**：本文档是AI医生系统的**技术架构设计**的核心技术组件部分，包含知识图谱推理引擎、诊断工具（知识库优先+DR.KNOWS路径验证）、检查建议工具、对话管理工具等核心技术组件的技术实现。  
> **业务功能**：请参考《AI医生系统-系统功能设计.md》  
> **核心目标**：详细说明各个核心技术组件的技术实现、算法设计和数据库设计。

---

## 三、核心技术组件设计

> **架构说明**：本文档描述的是工具（Tools）内部的技术实现，工具由主Agent调用，返回结构化结果和证据引用。工具不拥有决策权，只负责执行并返回结果。

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

#### 3.1.3 路径结构特征分析（非数值评分）

> **核心原则**：不使用数值评分，完全基于路径结构特征和知识库中的风险评估等级

**路径结构特征**：
- **路径数量统计**：指向同一疾病的路径数量（客观统计）
- **路径长度**：路径跳数（2-4跳）
- **关系类型分布**：直接关系/间接关系
- **患者特征匹配**：路径是否匹配患者特征（年龄、性别等）
- **风险评估等级**：来自知识库的风险评估等级（L1/L2/L3/L4/L5）

**技术实现**：
```python
class PathStructureAnalyzer:
    def analyze_path_structure(self, paths: List[Path]) -> Dict:
        """
        分析路径结构特征（非数值评分）
        """
        # 1. 按目标疾病分组
        disease_paths = {}
        for path in paths:
            disease_cui = path.target_disease_cui
            if disease_cui not in disease_paths:
                disease_paths[disease_cui] = []
            disease_paths[disease_cui].append(path)
        
        # 2. 计算路径结构特征
        structure_features = {}
        for disease_cui, paths_list in disease_paths.items():
            structure_features[disease_cui] = {
                'path_count': len(paths_list),  # 路径数量（客观统计）
                'shortest_path_length': min(p.path_length for p in paths_list),
                'longest_path_length': max(p.path_length for p in paths_list),
                'relation_types': self._analyze_relation_types(paths_list),
                'patient_match': all(p.patient_match for p in paths_list),
                'risk_level': self._get_risk_level_from_kb(disease_cui)  # 来自知识库
            }
        
        return structure_features
    
    def select_top_paths(self, paths: List[Path], n: int = 10) -> List[Path]:
        """
        选择Top-N路径（基于路径结构特征，非数值评分）
        """
        structure_features = self.analyze_path_structure(paths)
        
        # 1. 优先选择风险评估等级为L1/L2的疾病路径（高危路径）
        high_risk_paths = [
            p for p in paths 
            if structure_features[p.target_disease_cui]['risk_level'] in ['L1', 'L2']
        ]
        
        # 2. 选择路径数量最多的疾病对应的路径
        sorted_diseases = sorted(
            structure_features.items(),
            key=lambda x: x[1]['path_count'],
            reverse=True
        )
        
        top_paths = []
        # 先添加高危路径
        top_paths.extend(high_risk_paths[:n//2])
        
        # 再添加路径数量最多的疾病路径
        for disease_cui, features in sorted_diseases:
            if disease_cui not in [p.target_disease_cui for p in top_paths]:
                disease_paths = [p for p in paths if p.target_disease_cui == disease_cui]
                disease_paths.sort(key=lambda p: p.path_length)  # 优先选择路径长度最短的
                top_paths.extend(disease_paths[:3])  # 每个疾病最多3条路径
                if len(top_paths) >= n:
                    break
        
        return top_paths[:n]
```

#### 3.1.4 路径验证与知识库候选匹配

**技术实现**：
- **路径验证**：验证知识库候选是否有对应的推理路径支持
- **路径补充**：如果路径指向知识库候选外的疾病，作为补充候选

```python
class PathValidator:
    def validate_knowledge_base_candidates(self, 
                                          kb_candidates: List[Candidate],
                                          paths: List[Path]) -> Dict:
        """
        验证知识库候选是否有对应的路径支持
        """
        verified_candidates = []
        unverified_candidates = []
        
        for candidate in kb_candidates:
            # 检查是否有路径指向该候选疾病
            matching_paths = [
                p for p in paths 
                if p.target_disease_cui == candidate.disease_cui
            ]
            
            if matching_paths:
                candidate.verified_paths = matching_paths
                candidate.path_verified = True
                verified_candidates.append(candidate)
            else:
                candidate.path_verified = False
                unverified_candidates.append(candidate)
        
        # 提取补充候选（路径指向知识库候选外的疾病）
        kb_disease_cuis = {c.disease_cui for c in kb_candidates}
        supplementary_candidates = [
            p.target_disease_cui 
            for p in paths 
            if p.target_disease_cui not in kb_disease_cuis
        ]
        
        return {
            'verified_candidates': verified_candidates,
            'unverified_candidates': unverified_candidates,
            'supplementary_candidates': list(set(supplementary_candidates))
        }
```

#### 3.1.5 路径约束推理与验证

> **核心原则**：LLM在路径约束下推理，避免自由联想，并对推理结果进行验证

**技术实现**：
- **路径约束**：使用Top-N路径约束LLM推理，明确要求不得引入路径外的自由联想
- **推理验证**：验证LLM推理结果是否符合路径约束
- **降级策略**：如果验证失败，使用路径本身作为推理结果

```python
class PathConstrainedReasoning:
    def __init__(self):
        self.llm_client = LLMClient()  # LangChain
        self.path_validator = PathValidator()
        
    def reason_with_path_constraints(self, 
                                    top_paths: List[Path],
                                    patient_state: Dict) -> Dict:
        """
        在路径约束下进行推理
        """
        # 1. 选择Top-N路径（基于路径结构特征）
        top_n_paths = self._select_top_paths(top_paths, n=10)
        
        # 2. 构建路径约束Prompt
        prompt = self._build_constrained_prompt(top_n_paths, patient_state)
        
        # 3. LLM推理
        llm_response = self.llm_client.generate(prompt)
        
        # 4. 解析LLM响应
        reasoning_result = self._parse_llm_response(llm_response)
        
        # 5. 推理结果验证（关键步骤）
        validation_result = self.path_validator.validate_reasoning_result(
            reasoning_result, top_n_paths
        )
        
        # 6. 如果验证失败，使用降级策略
        if not validation_result['is_valid']:
            reasoning_result = self._fallback_to_paths(top_n_paths)
            reasoning_result['needs_manual_review'] = True
            reasoning_result['validation_failures'] = validation_result['failures']
        
        return reasoning_result
    
    def _build_constrained_prompt(self, paths: List[Path], patient_state: Dict) -> str:
        """
        构建路径约束Prompt
        """
        paths_text = self._format_paths_for_llm(paths)
        
        prompt = f"""
你是一位经验丰富的医生，需要根据患者的症状、体征、健康档案等信息，
结合以下医学推理路径，分析可能的疾病方向。

患者信息：
{patient_state}

医学推理路径（来自知识图谱，必须基于这些路径推理，不得引入路径外的自由联想）：
{paths_text}

请根据以上医学推理路径，分析：
1. 可能的疾病方向（Top 3-5），必须基于提供的路径
2. 每个方向的支持证据（必须来自路径中的节点）
3. 每个方向的反对证据（必须来自路径中的节点）
4. 还需要哪些信息来进一步判断（路径中缺失的节点）

注意：
- 必须基于提供的路径推理，不得引入路径外的自由联想
- 所有证据必须来自路径中的节点
- 如果路径中没有相关信息，明确说明"路径中无此信息"

请以JSON格式返回。
"""
        return prompt
    
    def _format_paths_for_llm(self, paths: List[Path]) -> str:
        """
        将路径格式化为自然语言描述
        """
        path_descriptions = []
        
        for path in paths:
            description = f"推理路径 {path.path_id}："
            
            for i in range(len(path.node_sequence) - 1):
                from_node = path.node_sequence[i]
                to_node = path.node_sequence[i + 1]
                rel = path.relation_sequence[i]
                
                description += f"{from_node} --[{rel}]--> {to_node}; "
            
            description += f"（目标疾病：{path.target_disease}，路径长度：{path.path_length}跳）"
            path_descriptions.append(description)
        
        return "\n".join(path_descriptions)
```

**路径验证机制**：

```python
class PathValidator:
    def validate_reasoning_result(self, 
                                  reasoning_result: Dict,
                                  paths: List[Path]) -> Dict:
        """
        验证LLM推理结果是否符合路径约束
        """
        failures = []
        
        # 1. 路径一致性验证：检查LLM输出的诊断是否在路径中
        for diagnosis in reasoning_result.get('possibilities', {}).keys():
            if not any(p.target_disease == diagnosis for p in paths):
                failures.append(f"诊断'{diagnosis}'不在提供的路径中")
        
        # 2. 证据来源验证：检查LLM输出的证据是否来自路径
        for diagnosis, evidence_list in reasoning_result.get('supporting_evidence', {}).items():
            matching_paths = [p for p in paths if p.target_disease == diagnosis]
            path_nodes = set()
            for path in matching_paths:
                path_nodes.update(path.node_sequence)
            
            for evidence in evidence_list:
                if evidence not in path_nodes:
                    failures.append(f"支持证据'{evidence}'不在路径节点中")
        
        # 3. 路径完整性验证：检查LLM是否使用了所有Top-N路径
        used_paths = set(reasoning_result.get('reasoning_paths_used', []))
        all_path_ids = {p.path_id for p in paths}
        if not used_paths.issubset(all_path_ids):
            failures.append("LLM使用了路径外的推理路径")
        
        return {
            'is_valid': len(failures) == 0,
            'failures': failures
        }
```

### 3.2 鉴别诊断工具（基于知识库优先 + DR.KNOWS路径验证）

> **工具定位**：tool_3（鉴别诊断工具）的核心技术实现  
> **工具类型**：Retrieval（知识库优先）+ Generative（路径约束推理）  
> **工具职责**：生成Top-K鉴别诊断列表，每个诊断包含支持证据、反证、缺失证据

#### 3.2.1 诊断工具架构

> **核心设计理念**：知识库优先，DR.KNOWS路径用于验证和补充，不使用数值评分，完全基于路径结构特征和知识库中的风险评估等级。

**鉴别诊断工具**（tool_3）：
   - **知识库优先**：优先使用知识库中的权威医学内容（主诉知识图谱、疾病知识图谱）
   - **DR.KNOWS路径验证**：使用DR.KNOWS路径验证知识库候选是否有效
   - **路径约束推理**：LLM在路径约束下进行推理，避免自由联想
   - **技术栈**：
     - 知识库：MySQL/Oracle（主诉知识图谱、疾病知识图谱）
     - 知识图谱：Neo4j（DR.KNOWS路径检索）
     - LLM：LangChain框架 + T5/ChatGPT/医学专用LLM（路径约束推理）
     - 路径验证：路径一致性验证、证据来源验证、路径完整性验证

#### 3.2.2 知识库候选检索（优先）

**技术实现**：
- **知识库优先原则**：知识库中的权威医学内容（主诉知识图谱、疾病知识图谱）是主要来源，直接使用
- **三层候选检索**：从主诉知识图谱检索Tier1/Tier2/Tier3候选疾病
- **疾病详细信息检索**：从疾病知识图谱检索疾病详细信息

```python
class KnowledgeBaseCandidateRetriever:
    def __init__(self):
        self.chief_complaint_kg = ChiefComplaintKnowledgeGraph()  # MySQL/Oracle
        self.disease_kg = DiseaseKnowledgeGraph()  # MySQL/Oracle
        
    def retrieve_candidates(self, 
                            chief_complaint: str,
                            structured_problem_list: Dict) -> List[Candidate]:
        """
        从知识库检索候选疾病（优先）
        """
        candidates = []
        
        # 1. 从主诉知识图谱检索三层候选
        tier1_candidates = self.chief_complaint_kg.get_tier1_candidates(chief_complaint)
        tier2_candidates = self.chief_complaint_kg.get_tier2_candidates(chief_complaint)
        tier3_candidates = self.chief_complaint_kg.get_tier3_candidates(chief_complaint)
        
        # 2. 从疾病知识图谱检索疾病详细信息
        for candidate in tier1_candidates + tier2_candidates + tier3_candidates:
            disease_info = self.disease_kg.get_disease_info(candidate.disease_id)
            candidate.disease_info = disease_info
            candidate.default_tier = disease_info.default_tier
            candidate.inclusion_reason = disease_info.inclusion_reason
            candidate.supporting_clues = disease_info.supporting_clues
            candidate.opposing_clues = disease_info.opposing_clues
            candidate.missing_info = disease_info.missing_info
            candidates.append(candidate)
        
        return candidates
```

#### 3.2.3 DR.KNOWS路径检索与验证

**技术实现**：
- **路径检索**：从Neo4j知识图谱检索症状→疾病的推理路径
- **路径验证**：验证知识库候选是否有对应的推理路径支持
- **路径补充**：如果路径指向知识库候选外的疾病，作为补充候选

```python
class DRKnowsPathRetriever:
    def __init__(self):
        self.neo4j_client = Neo4jClient()
        
    def retrieve_paths(self, 
                     symptom_cuis: List[str],
                     patient_context: Dict) -> List[Path]:
        """
        检索DR.KNOWS路径
        """
        # 1. 构建检索条件
        query = """
        MATCH path = (s:Symptom)-[*2..4]->(d:Disease)
        WHERE s.cui IN $symptom_cuis
        RETURN path, 
               relationships(path) as rels,
               nodes(path) as nodes,
               length(path) as path_length
        ORDER BY path_length
        LIMIT 100
        """
        
        # 2. 执行路径检索
        paths = self.neo4j_client.execute_query(
            query, 
            symptom_cuis=symptom_cuis
        )
        
        # 3. 路径去重
        unique_paths = self._deduplicate_paths(paths)
        
        return unique_paths
    
    def verify_candidates(self, 
                         knowledge_base_candidates: List[Candidate],
                         paths: List[Path]) -> Dict:
        """
        验证知识库候选是否有对应的路径支持
        """
        verified_candidates = []
        unverified_candidates = []
        
        for candidate in knowledge_base_candidates:
            # 检查是否有路径指向该候选疾病
            matching_paths = [
                p for p in paths 
                if p.target_disease_cui == candidate.disease_cui
            ]
            
            if matching_paths:
                candidate.verified_paths = matching_paths
                candidate.path_verified = True
                verified_candidates.append(candidate)
            else:
                candidate.path_verified = False
                unverified_candidates.append(candidate)
        
        return {
            "verified_candidates": verified_candidates,
            "unverified_candidates": unverified_candidates,
            "supplementary_candidates": self._extract_supplementary_candidates(paths, knowledge_base_candidates)
        }
```

#### 3.2.4 推理子组组织（基于知识库）

**技术实现**：
- **知识库优先**：优先使用知识库中的推理子组（主诉表7：推理子组清单表）
- **路径补充**：如果知识库中的子组不完整，可以从DR.KNOWS路径中提取系统来源节点作为补充

```python
class ReasoningSubgroupOrganizer:
    def __init__(self):
        self.subgroup_db = SubgroupDatabase()  # MySQL/Oracle
        self.disease_kg = DiseaseKnowledgeGraph()  # MySQL/Oracle
        
    def organize_subgroups(self, 
                          candidates: List[Candidate],
                          paths: List[Path]) -> List[Subgroup]:
        """
        组织推理子组（基于知识库）
        """
        subgroups = []
        
        # 1. 优先使用知识库中的推理子组
        knowledge_base_subgroups = self.subgroup_db.get_subgroups_by_chief_complaint(
            chief_complaint=candidates[0].chief_complaint
        )
        
        # 2. 从疾病知识图谱获取推理子组归属
        for candidate in candidates:
            disease_info = self.disease_kg.get_disease_info(candidate.disease_id)
            candidate.subgroup_belonging = disease_info.subgroup_belonging
        
        # 3. 组织子组
        for kb_subgroup in knowledge_base_subgroups:
            subgroup = Subgroup(
                name=kb_subgroup.name,
                covered_diseases=[c for c in candidates if c.subgroup_belonging == kb_subgroup.name],
                entry_triggers=kb_subgroup.entry_triggers,
                promotion_targets=kb_subgroup.promotion_targets
            )
            
            # 4. 从路径中提取关键差异点（补充）
            subgroup.key_differences = self._extract_key_differences_from_paths(
                subgroup, paths
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

#### 3.2.5 候选集整合与三层分层（基于知识库）

**核心原则**：**知识库优先，路径验证和补充，不使用数值评分**

**技术实现**：

```python
class CandidateIntegrator:
    def __init__(self):
        self.kb_retriever = KnowledgeBaseCandidateRetriever()
        self.path_retriever = DRKnowsPathRetriever()
        
    def integrate_candidates(self, 
                           chief_complaint: str,
                           structured_problem_list: Dict) -> Dict:
        """
        整合知识库候选和DR.KNOWS路径
        """
        # 1. 知识库候选检索（优先）
        kb_candidates = self.kb_retriever.retrieve_candidates(
            chief_complaint, structured_problem_list
        )
        
        # 2. DR.KNOWS路径检索
        symptom_cuis = [item['symptom_cui'] for item in structured_problem_list['symptoms']]
        paths = self.path_retriever.retrieve_paths(
            symptom_cuis, 
            structured_problem_list['patient_context']
        )
        
        # 3. 路径验证
        verification_result = self.path_retriever.verify_candidates(
            kb_candidates, paths
        )
        
        # 4. 三层分层（完全基于知识库中的权威内容）
        three_layer_ranking = self._build_three_layer_ranking(
            verification_result['verified_candidates']
        )
        
        return {
            'three_layer_ranking': three_layer_ranking,
            'verified_paths': paths,
            'supplementary_candidates': verification_result['supplementary_candidates']
        }
    
    def _build_three_layer_ranking(self, candidates: List[Candidate]) -> Dict:
        """
        构建三层分层（基于知识库中的默认层级）
        """
        tier1 = [c for c in candidates if c.default_tier == 'Tier1']
        tier2 = [c for c in candidates if c.default_tier == 'Tier2']
        tier3 = [c for c in candidates if c.default_tier == 'Tier3']
        
        return {
            'tier1_most_likely': tier1[:1],  # 最可能（1个）
            'tier2_must_exclude': tier2[:1],  # 必须排除（0-1个）
            'tier3_active_alternatives': tier3[:2]  # 积极备选（1-2个）
        }
```

#### 3.2.6 路径约束推理（LLM在路径约束下推理）

**技术实现**：
- **路径约束**：使用Top-N路径约束LLM推理，避免自由联想
- **推理验证**：验证LLM推理结果是否符合路径约束
- **降级策略**：如果验证失败，使用路径本身作为推理结果

```python
class PathConstrainedReasoning:
    def __init__(self):
        self.llm_client = LLMClient()  # LangChain
        self.path_validator = PathValidator()
        
    def reason_with_path_constraints(self, 
                                    top_paths: List[Path],
                                    patient_state: Dict) -> Dict:
        """
        在路径约束下进行推理
        """
        # 1. 选择Top-N路径（基于路径结构特征）
        top_n_paths = self._select_top_paths(top_paths, n=10)
        
        # 2. 构建路径约束Prompt
        prompt = self._build_constrained_prompt(top_n_paths, patient_state)
        
        # 3. LLM推理
        llm_response = self.llm_client.generate(prompt)
        
        # 4. 解析LLM响应
        reasoning_result = self._parse_llm_response(llm_response)
        
        # 5. 推理结果验证（关键步骤）
        validation_result = self.path_validator.validate(
            reasoning_result, top_n_paths
        )
        
        # 6. 如果验证失败，使用降级策略
        if not validation_result['is_valid']:
            reasoning_result = self._fallback_to_paths(top_n_paths)
            reasoning_result['needs_manual_review'] = True
        
        return reasoning_result
    
    def _select_top_paths(self, paths: List[Path], n: int = 10) -> List[Path]:
        """
        选择Top-N路径（基于路径结构特征，非数值评分）
        """
        # 1. 按路径数量统计（指向同一疾病的路径数量）
        disease_path_count = {}
        for path in paths:
            disease = path.target_disease_cui
            disease_path_count[disease] = disease_path_count.get(disease, 0) + 1
        
        # 2. 选择路径数量最多的疾病对应的路径
        top_diseases = sorted(
            disease_path_count.items(), 
            key=lambda x: x[1], 
            reverse=True
        )[:n]
        
        # 3. 选择这些疾病的路径，优先选择路径长度最短的
        top_paths = []
        for disease_cui, _ in top_diseases:
            disease_paths = [p for p in paths if p.target_disease_cui == disease_cui]
            disease_paths.sort(key=lambda p: p.path_length)
            top_paths.extend(disease_paths[:3])  # 每个疾病最多3条路径
        
        return top_paths[:n]
    
    def _build_constrained_prompt(self, paths: List[Path], patient_state: Dict) -> str:
        """
        构建路径约束Prompt
        """
        paths_text = self._format_paths_for_llm(paths)
        
        prompt = f"""
你是一位经验丰富的医生，需要根据患者的症状、体征、健康档案等信息，
结合以下医学推理路径，分析可能的疾病方向。

患者信息：
{patient_state}

医学推理路径（来自知识图谱，必须基于这些路径推理，不得引入路径外的自由联想）：
{paths_text}

请根据以上医学推理路径，分析：
1. 可能的疾病方向（Top 3-5），必须基于提供的路径
2. 每个方向的支持证据（必须来自路径中的节点）
3. 每个方向的反对证据（必须来自路径中的节点）
4. 还需要哪些信息来进一步判断（路径中缺失的节点）

注意：
- 必须基于提供的路径推理，不得引入路径外的自由联想
- 所有证据必须来自路径中的节点
- 如果路径中没有相关信息，明确说明"路径中无此信息"

请以JSON格式返回。
"""
        return prompt
```

### 3.3 主Agent运行循环与5步默认诊断路径

> **对应功能设计文档**：3.3 临床诊疗态详细流程（AI循证诊断流程）  
> **功能定义详情**：请参考《AI医生系统-系统功能设计.md》第3.3.1-3.3.6节  
> **参考文档**：《智能诊断A路径：AI循证诊断流程.md》  
> **核心思想**：主Agent通过运行循环（Observe→Plan→Act→Update→Evaluate→Stop/Escalate）执行5步默认诊断路径，确保诊断过程可推理、可复用、可审计。

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

### 3.4 检查建议工具（Workup Planner Tool）

> **工具定位**：tool_4（检查建议工具）的核心技术实现  
> **工具类型**：Deterministic  
> **工具职责**：基于当前DDx和已有证据，建议下一步检查，并评估检查的价值

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

### 3.5 主动问诊工具（Interview Tool）

> **工具定位**：tool_2（主动问诊工具）的核心技术实现  
> **工具类型**：Generative（但问诊策略基于临床决策分析，属于Deterministic）  
> **工具职责**：像医生一样问"关键问题"，补齐鉴别诊断所需证据

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
- [AI医生系统-技术架构设计-工具清单](./AI医生系统-技术架构设计-工具清单.md)
- [AI医生系统-技术架构设计-CDP数据与状态管理](./AI医生系统-技术架构设计-CDP数据与状态管理.md)
- [AI医生系统-技术架构设计-索引](./AI医生系统-技术架构设计-索引.md)

