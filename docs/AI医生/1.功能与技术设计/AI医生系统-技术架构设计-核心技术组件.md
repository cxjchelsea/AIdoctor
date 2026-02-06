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

### 3.6 知识查询服务（Knowledge Query Service）

> **对应架构文档**：核心架构 - 七、知识演化与维护子系统 - 7.2.1 在线层：Knowledge Query  
> **功能定位**：服务于通道1结构化推理，提供只读的知识库查询服务

#### 3.6.1 服务定位与职责

**定位**：
- 在线层服务，服务于通道1结构化推理
- 只读访问生产知识库
- 不参与知识演化流程

**核心职责**：
- 知识库优先查询（主诉知识图谱、疾病知识图谱）
- Neo4j路径检索验证
- 路径约束推理
- 版本化知识访问

#### 3.6.2 知识库查询接口

**技术实现**：

```python
class KnowledgeQueryService:
    def __init__(self):
        self.neo4j_client = Neo4jClient()
        self.knowledge_base_client = KnowledgeBaseClient()
        self.version_manager = KnowledgeVersionManager()
    
    def query_knowledge_base(self, 
                           query_type: str,
                           query_params: Dict,
                           kg_version: str = None) -> Dict:
        """
        知识库优先查询
        
        Args:
            query_type: 查询类型（chief_complaint/disease/relation）
            query_params: 查询参数
            kg_version: 知识版本（可选，默认使用当前生产版本）
        
        Returns:
            查询结果（包含知识对象、证据来源、版本信息）
        """
        # 1. 确定使用的知识版本
        if not kg_version:
            kg_version = self.version_manager.get_current_production_version()
        
        # 2. 查询知识库
        if query_type == "chief_complaint":
            return self._query_chief_complaint_kb(query_params, kg_version)
        elif query_type == "disease":
            return self._query_disease_kb(query_params, kg_version)
        elif query_type == "relation":
            return self._query_relation_kb(query_params, kg_version)
        else:
            raise ValueError(f"Unknown query type: {query_type}")
    
    def _query_chief_complaint_kb(self, params: Dict, kg_version: str) -> Dict:
        """
        查询主诉知识图谱
        """
        chief_complaint = params.get("chief_complaint")
        
        # 查询主诉知识图谱（从MySQL/Oracle）
        kb_result = self.knowledge_base_client.query_chief_complaint(
            chief_complaint=chief_complaint,
            kg_version=kg_version
        )
        
        return {
            "knowledge_objects": kb_result,
            "kg_version": kg_version,
            "source": "knowledge_base"
        }
    
    def _query_disease_kb(self, params: Dict, kg_version: str) -> Dict:
        """
        查询疾病知识图谱
        """
        disease_cui = params.get("disease_cui")
        
        # 查询疾病知识图谱
        kb_result = self.knowledge_base_client.query_disease(
            disease_cui=disease_cui,
            kg_version=kg_version
        )
        
        return {
            "knowledge_objects": kb_result,
            "kg_version": kg_version,
            "source": "knowledge_base"
        }
```

#### 3.6.3 Neo4j路径检索验证

**技术实现**：

```python
class PathRetriever:
    def __init__(self):
        self.neo4j_client = Neo4jClient()
    
    def retrieve_paths(self,
                      symptom_cuis: List[str],
                      max_hops: int = 4,
                      kg_version: str = None) -> List[Path]:
        """
        检索多跳推理路径
        
        Args:
            symptom_cuis: 症状CUI列表
            max_hops: 最大跳数（默认4）
            kg_version: 知识版本
        
        Returns:
            推理路径列表
        """
        # 构建Cypher查询（根据kg_version过滤）
        cypher_query = f"""
        MATCH path = (s:Symptom)-[*2..{max_hops}]->(d:Disease)
        WHERE s.cui IN $symptom_cuis
        AND d.release_id = $kg_version
        RETURN path, 
               relationships(path) as rels,
               nodes(path) as nodes,
               length(path) as path_length
        ORDER BY path_length
        LIMIT 50
        """
        
        results = self.neo4j_client.execute_query(
            cypher_query,
            symptom_cuis=symptom_cuis,
            kg_version=kg_version or "current"
        )
        
        paths = []
        for result in results:
            paths.append(Path(
                nodes=result["nodes"],
                relationships=result["rels"],
                length=result["path_length"],
                kg_version=kg_version
            ))
        
        return paths
    
    def validate_knowledge_base_candidates(self,
                                          kb_candidates: List[str],
                                          paths: List[Path]) -> Dict:
        """
        验证知识库候选是否有路径支持
        """
        validated = []
        unvalidated = []
        
        for candidate in kb_candidates:
            # 检查是否有路径指向该候选
            has_path = any(
                path.target_disease_cui == candidate 
                for path in paths
            )
            
            if has_path:
                validated.append(candidate)
            else:
                unvalidated.append(candidate)
        
        return {
            "validated": validated,
            "unvalidated": unvalidated,
            "validation_rate": len(validated) / len(kb_candidates) if kb_candidates else 0
        }
```

#### 3.6.4 版本化知识访问

**技术实现**：

```python
class KnowledgeVersionManager:
    def __init__(self):
        self.neo4j_client = Neo4jClient()
        self.metadata_db = MetadataDatabase()
    
    def get_current_production_version(self) -> str:
        """
        获取当前生产版本
        """
        metadata = self.metadata_db.query(
            "SELECT release_id FROM ReleaseMetadata WHERE is_current = true"
        )
        if metadata:
            return metadata[0]["release_id"]
        return "v1.0"  # 默认版本
    
    def query_with_version(self, 
                          query: str,
                          kg_version: str = None) -> Dict:
        """
        使用指定版本查询知识
        """
        if not kg_version:
            kg_version = self.get_current_production_version()
        
        # 在查询中添加版本过滤
        versioned_query = self._add_version_filter(query, kg_version)
        
        return self.neo4j_client.execute_query(versioned_query)
    
    def _add_version_filter(self, query: str, kg_version: str) -> str:
        """
        在Cypher查询中添加版本过滤
        """
        # 在WHERE子句中添加版本过滤
        if "WHERE" in query.upper():
            query = query.replace(
                "WHERE",
                f"WHERE ko.release_id = '{kg_version}' AND"
            )
        else:
            query = query + f" WHERE ko.release_id = '{kg_version}'"
        
        return query
```

### 3.7 知识运维服务（Knowledge Operations Service）

> **对应架构文档**：核心架构 - 七、知识演化与维护子系统 - 7.2.2 离线层：Knowledge Evolution  
> **功能定位**：知识演化的核心子系统，负责知识的抽取、验证、冲突处理、发布门禁

#### 3.7.1 服务定位与职责

**定位**：
- 离线层服务，知识演化的核心子系统
- 异步运行，不阻塞在线诊断流程
- 工作在候选区，不直接修改生产知识

**核心职责**：
- 知识抽取（Extractor Agent）
- 知识验证（Verifier Agent）
- 冲突处理（Conflict Resolver Agent）
- 候选构建（Release Builder Agent）
- 回归评测（Shadow Evaluator Agent）
- 发布门禁（Publish Gate）
- 监控与回滚（Rollback & Drift Monitor Agent）

#### 3.7.2 知识对象（KO）数据结构

**技术实现**：

```python
from dataclasses import dataclass
from typing import List, Dict, Optional
from enum import Enum

class KOType(Enum):
    RULE = "rule"
    RELATION = "relation"
    PATHWAY_TEMPLATE = "pathway_template"
    CONTRAINDICATION = "contraindication"
    THRESHOLD = "threshold"
    DDX_FEATURE = "ddx_feature"

class KOStatus(Enum):
    PROPOSED = "proposed"
    VERIFIED = "verified"
    PUBLISHED = "published"
    DEPRECATED = "deprecated"

@dataclass
class Provenance:
    """证据来源"""
    doc_id: str
    doc_version: str
    section: str
    paragraph: str
    page_range: Optional[tuple] = None
    timestamp: str = None

@dataclass
class DownstreamBinding:
    """下游绑定"""
    binding_type: str  # tool / rule_engine / pathway_template
    binding_id: str
    binding_name: str
    usage_context: str
    binding_strength: str  # required / optional / conditional

@dataclass
class KnowledgeObject:
    """知识对象（KO）"""
    ko_id: str
    ko_type: KOType
    content: Dict
    concept_ids: List[str]  # CUI/ICD/SNOMED
    provenance: List[Provenance]
    status: KOStatus
    kg_version: Optional[str] = None  # 仅对published状态
    impact_scope: List[str] = None  # DDx / workup / treatment / risk
    downstream_bindings: List[DownstreamBinding] = None
    release_id: str = None  # Sandbox / Staging / v1.0 / v2.0等
```

#### 3.7.3 知识更新提案（Knowledge Proposal）数据结构

**技术实现**：

```python
from enum import Enum
from typing import Dict, List, Optional

class ProposalTrigger(Enum):
    """提案触发原因"""
    KNOWLEDGE_GAP = "knowledge_gap"  # 知识覆盖缺口
    CONFLICT_RESOLUTION_FAILED = "conflict_resolution_failed"  # 冲突解决失败
    EVIDENCE_INSUFFICIENT = "evidence_insufficient"  # 证据不足
    NEW_GUIDELINE = "new_guideline"  # 新指南发布
    RULE_CHANGE = "rule_change"  # 内部规则变更
    REGRESSION_DETECTED = "regression_detected"  # 线上监控发现回归

class RiskLevel(Enum):
    """风险等级"""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"

@dataclass
class EvidenceSnapshot:
    """证据快照"""
    snapshot_hash: str  # sha256 hash
    cdp_snapshot: Dict  # CDP状态快照
    tool_results: List[Dict]  # 工具调用结果
    knowledge_queries: List[Dict]  # 知识查询结果
    trigger_context: Dict  # 触发上下文（session_id、timestamp、agent_state等）

@dataclass
class KnowledgeProposal:
    """知识更新提案"""
    proposal_id: str
    trigger: ProposalTrigger
    diff: Dict  # 对哪些ko做新增/修改/删除
    required_tests: List[str]  # 需要跑哪些回归/影子评测
    risk_level: RiskLevel
    dedupe_key: str  # 去重键：按病种/概念/缺口类型聚合
    cooldown_window: int  # 冷却时间窗口（秒）
    evidence_snapshot: EvidenceSnapshot  # 触发当时的关键证据快照
    trigger_count: int = 1  # 触发次数（合并时更新）
    first_trigger_time: Optional[str] = None  # 首次触发时间
    latest_trigger_time: Optional[str] = None  # 最新触发时间
    created_at: Optional[str] = None
    status: str = "pending"  # pending / processing / completed / rejected
```

**dedupe_key生成规则**：

```python
def generate_dedupe_key(trigger: ProposalTrigger, 
                        disease_concept_id: Optional[str] = None,
                        symptom_concept_id: Optional[str] = None) -> str:
    """
    生成去重键
    
    格式：{gap_type}_{disease_concept_id}_{symptom_concept_id}
    示例：
    - knowledge_gap_ICD_I20_0_SYMP_001：心绞痛的知识缺口
    - conflict_resolution_failed_ICD_I21_0：急性心肌梗死冲突解决失败
    - evidence_insufficient_ICD_J18_0：肺炎证据不足
    """
    gap_type = trigger.value
    disease_part = disease_concept_id or "UNKNOWN"
    symptom_part = symptom_concept_id or "UNKNOWN"
    return f"{gap_type}_{disease_part}_{symptom_part}"
```

**cooldown_window配置规则**：

```python
def get_cooldown_window(trigger: ProposalTrigger) -> int:
    """
    根据触发类型获取冷却时间窗口（秒）
    """
    cooldown_map = {
        ProposalTrigger.KNOWLEDGE_GAP: 3600,  # 1小时
        ProposalTrigger.CONFLICT_RESOLUTION_FAILED: 7200,  # 2小时
        ProposalTrigger.EVIDENCE_INSUFFICIENT: 1800,  # 30分钟
        ProposalTrigger.NEW_GUIDELINE: 0,  # 新指南立即处理
        ProposalTrigger.RULE_CHANGE: 0,  # 规则变更立即处理
        ProposalTrigger.REGRESSION_DETECTED: 3600,  # 1小时
    }
    return cooldown_map.get(trigger, 3600)  # 默认1小时
```

#### 3.7.4 Extractor Agent（抽取Agent）

**技术实现**：

```python
class ExtractorAgent:
    def __init__(self):
        self.llm_client = LLMClient()
        self.sandbox_db = SandboxDatabase()
    
    def extract_knowledge(self,
                         source_text: str,
                         source_metadata: Dict) -> KnowledgeObject:
        """
        从知识源中抽取结构化知识
        
        Args:
            source_text: 知识源文本
            source_metadata: 知识源元数据（doc_id、版本等）
        
        Returns:
            知识对象（KO草稿）
        """
        # 1. 构建提取Prompt
        prompt = self._build_extraction_prompt(source_text)
        
        # 2. 调用LLM提取
        extraction_result = self.llm_client.extract(
            prompt=prompt,
            schema=self._get_ko_schema()
        )
        
        # 3. 构建知识对象
        ko = KnowledgeObject(
            ko_id=self._generate_ko_id(),
            ko_type=extraction_result["ko_type"],
            content=extraction_result["content"],
            concept_ids=extraction_result["concept_ids"],
            provenance=[Provenance(
                doc_id=source_metadata["doc_id"],
                doc_version=source_metadata["version"],
                section=source_metadata.get("section"),
                paragraph=source_metadata.get("paragraph")
            )],
            status=KOStatus.PROPOSED,
            release_id="Sandbox"
        )
        
        # 4. 保存到Sandbox
        self.sandbox_db.save_ko(ko)
        
        return ko
```

#### 3.7.4 Verifier Agent（验证Agent）

**技术实现**：

```python
class VerifierAgent:
    def __init__(self):
        self.sandbox_db = SandboxDatabase()
        self.staging_db = StagingDatabase()
    
    def verify_knowledge(self, ko: KnowledgeObject) -> Dict:
        """
        验证知识的正确性和一致性
        
        Returns:
            验证结果（包含是否通过、验证报告）
        """
        verification_result = {
            "ko_id": ko.ko_id,
            "passed": True,
            "issues": []
        }
        
        # 1. 证据一致性验证
        evidence_consistency = self._verify_evidence_consistency(ko)
        if not evidence_consistency["passed"]:
            verification_result["passed"] = False
            verification_result["issues"].extend(evidence_consistency["issues"])
        
        # 2. 冲突检测
        conflicts = self._detect_conflicts(ko)
        if conflicts:
            verification_result["passed"] = False
            verification_result["issues"].extend(conflicts)
        
        # 3. 如果通过验证，移动到Staging
        if verification_result["passed"]:
            ko.status = KOStatus.VERIFIED
            ko.release_id = "Staging"
            self.staging_db.save_ko(ko)
            self.sandbox_db.remove_ko(ko.ko_id)
        
        return verification_result
    
    def _verify_evidence_consistency(self, ko: KnowledgeObject) -> Dict:
        """
        验证证据一致性
        """
        # 检查provenance是否完整
        if not ko.provenance:
            return {
                "passed": False,
                "issues": ["缺少证据来源（provenance）"]
            }
        
        # 检查证据是否支持KO内容
        # ... 实现细节
        
        return {"passed": True, "issues": []}
    
    def _detect_conflicts(self, ko: KnowledgeObject) -> List[Dict]:
        """
        检测与现有KO的冲突
        """
        conflicts = []
        
        # 查询现有KO（从Staging和Production）
        existing_kos = self._query_existing_kos(ko)
        
        for existing_ko in existing_kos:
            conflict = self._check_conflict(ko, existing_ko)
            if conflict:
                conflicts.append(conflict)
        
        return conflicts
```

#### 3.7.5 Conflict Resolver Agent（冲突解决Agent）

**技术实现**：

```python
@dataclass
class ConflictResolutionRecord:
    """冲突解决记录"""
    conflict_case_id: str  # 冲突实例ID
    conflicting_ko_ids: List[str]  # 冲突的KO ID列表
    conflict_type: str  # 冲突类型（value_conflict / logic_conflict / path_conflict等）
    conflict_description: str  # 冲突描述
    resolution_strategy: str  # 解决策略（覆盖/并存/分支/降级）
    resolution_rationale: str  # 解决理由
    resolution_timestamp: str  # 解决时间
    resolved_by: str  # 解决者（Agent ID或人工审核者）
    resolution_evidence: Dict  # 解决证据（如新指南版本、证据等级对比等）

class ConflictResolverAgent:
    def __init__(self):
        self.staging_db = StagingDatabase()
        self.resolution_history_db = ResolutionHistoryDatabase()
    
    def resolve_conflict(self, conflict: Dict) -> ConflictResolutionRecord:
        """
        解决知识冲突
        
        Args:
            conflict: 冲突信息（包含conflicting_ko_ids、conflict_type等）
        
        Returns:
            冲突解决记录
        """
        # 1. 分析冲突类型和严重程度
        conflict_analysis = self._analyze_conflict(conflict)
        
        # 2. 选择解决策略
        resolution_strategy = self._select_resolution_strategy(conflict_analysis)
        
        # 3. 执行解决策略
        resolution_result = self._execute_resolution(conflict, resolution_strategy)
        
        # 4. 生成冲突解决记录
        resolution_record = ConflictResolutionRecord(
            conflict_case_id=self._generate_conflict_case_id(),
            conflicting_ko_ids=conflict["conflicting_ko_ids"],
            conflict_type=conflict["conflict_type"],
            conflict_description=conflict["description"],
            resolution_strategy=resolution_strategy,
            resolution_rationale=resolution_result["rationale"],
            resolution_timestamp=self._get_current_timestamp(),
            resolved_by="ConflictResolverAgent_v1.0",
            resolution_evidence=resolution_result["evidence"]
        )
        
        # 5. 保存解决记录
        self.resolution_history_db.save_resolution_record(resolution_record)
        
        # 6. 更新KO状态
        self._update_ko_status(conflict, resolution_strategy, resolution_result)
        
        return resolution_record
    
    def _select_resolution_strategy(self, conflict_analysis: Dict) -> str:
        """
        选择冲突解决策略
        
        策略：
        - 覆盖：新知识覆盖旧知识（新指南证据等级更高）
        - 并存：多个版本并存（机构政策差异）
        - 分支：按不同场景分支（不同人群适用不同标准）
        - 降级：降级为"需人工审阅"（复杂冲突无法自动解决）
        """
        if conflict_analysis["can_auto_resolve"]:
            if conflict_analysis["newer_evidence_level"] > conflict_analysis["older_evidence_level"]:
                return "覆盖"
            elif conflict_analysis["institutional_difference"]:
                return "并存"
            elif conflict_analysis["scenario_dependent"]:
                return "分支"
        return "降级"
    
    def _execute_resolution(self, conflict: Dict, strategy: str) -> Dict:
        """
        执行解决策略
        """
        if strategy == "覆盖":
            return self._resolve_by_override(conflict)
        elif strategy == "并存":
            return self._resolve_by_coexist(conflict)
        elif strategy == "分支":
            return self._resolve_by_branch(conflict)
        else:  # 降级
            return self._resolve_by_downgrade(conflict)
```

#### 3.7.6 Release Builder Agent（打包发布候选Agent）

**技术实现**：

```python
class ReleaseBuilderAgent:
    def __init__(self):
        self.staging_db = StagingDatabase()
        self.diff_analyzer = DiffAnalyzer()
    
    def build_candidate_release(self, 
                                verified_ko_ids: List[str]) -> Dict:
        """
        打包发布候选
        
        Args:
            verified_ko_ids: 通过验证的KO ID列表
        
        Returns:
            candidate_release对象
        """
        # 1. 收集通过验证的KO
        verified_kos = self.staging_db.get_kos_by_ids(verified_ko_ids)
        
        # 2. 生成diff（与当前生产版本对比）
        diff = self._generate_diff(verified_kos)
        
        # 3. 生成测试计划
        test_plan = self._generate_test_plan(diff)
        
        # 4. 准备发布材料
        release_materials = self._prepare_release_materials(
            verified_kos, diff, test_plan
        )
        
        # 5. 创建candidate_release
        candidate_release = {
            "candidate_release_id": self._generate_release_id(),
            "knowledge_objects": verified_kos,
            "diff": diff,
            "test_plan": test_plan,
            "release_materials": release_materials,
            "created_at": self._get_current_timestamp(),
            "status": "pending_evaluation"
        }
        
        # 6. 保存到Staging
        self.staging_db.save_candidate_release(candidate_release)
        
        return candidate_release
    
    def _generate_diff(self, verified_kos: List[KnowledgeObject]) -> Dict:
        """
        生成与当前生产版本的diff
        """
        current_production_kos = self._get_current_production_kos()
        
        diff = {
            "added": [],  # 新增的KO
            "modified": [],  # 修改的KO
            "deleted": []  # 删除的KO
        }
        
        for ko in verified_kos:
            existing_ko = self._find_existing_ko(ko.ko_id, current_production_kos)
            if not existing_ko:
                diff["added"].append(ko.ko_id)
            elif self._has_changes(ko, existing_ko):
                diff["modified"].append({
                    "ko_id": ko.ko_id,
                    "changes": self._compare_ko(ko, existing_ko)
                })
        
        return diff
    
    def _generate_test_plan(self, diff: Dict) -> Dict:
        """
        生成测试计划
        """
        test_plan = {
            "required_tests": [],
            "priority_tests": [],
            "optional_tests": []
        }
        
        # 根据diff确定需要测试的范围
        if diff["added"] or diff["modified"]:
            test_plan["required_tests"].extend([
                "core_regression_set",  # 核心回归集
                "affected_disease_tests"  # 受影响疾病的测试
            ])
        
        if diff["deleted"]:
            test_plan["required_tests"].append("deletion_impact_tests")
        
        return test_plan
```

#### 3.7.7 Shadow Evaluator Agent（影子评测Agent）

**技术实现**：

```python
class ShadowEvaluatorAgent:
    def __init__(self):
        self.test_case_db = TestCaseDatabase()
        self.evaluation_result_db = EvaluationResultDatabase()
        self.baseline_version = None
    
    def evaluate(self, candidate_release: Dict) -> Dict:
        """
        在离线环境进行评测
        
        评测内容：
        - 下游诊断性能
        - 路径可追溯性
        - 失败模式分布
        """
        # 1. 加载测试用例
        test_sets = self._load_test_sets()
        
        # 2. 获取基线版本
        baseline_version = self._get_baseline_version()
        
        # 3. 运行评测
        evaluation_results = {}
        for test_set_name, test_cases in test_sets.items():
            results = self._run_test_set(test_cases, candidate_release)
            evaluation_results[test_set_name] = results
        
        # 4. 计算指标
        metrics = self._calculate_metrics(evaluation_results, baseline_version)
        
        # 5. 生成评测报告
        evaluation_report = self._generate_evaluation_report(
            candidate_release, evaluation_results, metrics, baseline_version
        )
        
        # 6. 保存评测结果
        self.evaluation_result_db.save_evaluation_report(evaluation_report)
        
        return evaluation_report
    
    def _load_test_sets(self) -> Dict:
        """
        加载测试用例集
        
        测试契约：
        1. 固定小集（Core Regression Set）：100个固定测试用例
           - 核心DDx场景：50个标准病例
           - 红旗病种：20个急危重病例
           - 关键路径：30个关键诊疗路径
        2. 抽样大集（Sampling Set）：500个随机抽样病例
        """
        return {
            "core_regression": self._load_core_regression_set(),  # 100个固定用例
            "sampling_set": self._load_sampling_set()  # 500个随机抽样
        }
    
    def _calculate_metrics(self, 
                           evaluation_results: Dict,
                           baseline_version: str) -> Dict:
        """
        计算评测指标
        
        硬阈值：
        - 红旗召回率：≥ 基线
        - 关键路径断裂：= 0
        - 总体性能下降：≤ 5%
        - 新增错误率：≤ 2%
        
        软阈值：
        - 平均响应时间：增加 ≤ 10%
        - 证据完整性：≥ 基线 - 3%
        - 路径可追溯性：≥ 基线 - 5%
        """
        baseline_metrics = self._get_baseline_metrics(baseline_version)
        
        metrics = {
            "red_flag_recall": self._calculate_red_flag_recall(evaluation_results),
            "critical_path_breaks": self._count_critical_path_breaks(evaluation_results),
            "overall_accuracy": self._calculate_overall_accuracy(evaluation_results),
            "new_errors": self._count_new_errors(evaluation_results),
            "avg_response_time": self._calculate_avg_response_time(evaluation_results),
            "evidence_completeness": self._calculate_evidence_completeness(evaluation_results),
            "path_traceability": self._calculate_path_traceability(evaluation_results)
        }
        
        # 检查硬阈值
        metrics["red_flag_recall"]["threshold_met"] = (
            metrics["red_flag_recall"]["candidate"] >= 
            baseline_metrics["red_flag_recall"]
        )
        metrics["critical_path_breaks"]["threshold_met"] = (
            metrics["critical_path_breaks"]["count"] == 0
        )
        metrics["overall_accuracy"]["threshold_met"] = (
            metrics["overall_accuracy"]["delta"] >= -0.05
        )
        metrics["new_errors"]["threshold_met"] = (
            metrics["new_errors"]["rate"] <= 0.02
        )
        
        # 检查软阈值
        metrics["avg_response_time"]["threshold_met"] = (
            metrics["avg_response_time"]["delta"] <= 0.10
        )
        metrics["evidence_completeness"]["threshold_met"] = (
            metrics["evidence_completeness"]["candidate"] >= 
            (baseline_metrics["evidence_completeness"] - 0.03)
        )
        metrics["path_traceability"]["threshold_met"] = (
            metrics["path_traceability"]["candidate"] >= 
            (baseline_metrics["path_traceability"] - 0.05)
        )
        
        return metrics
    
    def _generate_evaluation_report(self,
                                    candidate_release: Dict,
                                    evaluation_results: Dict,
                                    metrics: Dict,
                                    baseline_version: str) -> Dict:
        """
        生成评测报告
        
        报告结构：
        - evaluation_id
        - candidate_release_id
        - baseline_version
        - test_sets（测试集结果）
        - metrics（指标对比）
        - report_hash（报告hash）
        - evaluation_timestamp
        - evaluator
        """
        import hashlib
        import json
        
        report = {
            "evaluation_id": self._generate_evaluation_id(),
            "candidate_release_id": candidate_release["candidate_release_id"],
            "baseline_version": baseline_version,
            "test_sets": evaluation_results,
            "metrics": metrics,
            "evaluation_timestamp": self._get_current_timestamp(),
            "evaluator": "ShadowEvaluatorAgent_v1.0"
        }
        
        # 计算报告hash
        report_json = json.dumps(report, sort_keys=True)
        report_hash = hashlib.sha256(report_json.encode()).hexdigest()
        report["report_hash"] = f"sha256:{report_hash}"
        
        return report
```

#### 3.7.8 Rollback & Drift Monitor Agent（回滚与漂移监控Agent）

**技术实现**：

```python
class RollbackDriftMonitorAgent:
    def __init__(self):
        self.production_db = ProductionDatabase()
        self.monitoring_db = MonitoringDatabase()
        self.rollback_engine = RollbackEngine()
    
    def monitor_production(self):
        """
        监控生产环境的知识使用情况
        
        监控内容：
        - DDx波动（新版本上线后是否出现DDx波动）
        - 风险评估异常（风险评估是否异常）
        - 特定人群退化（特定人群的诊断性能是否下降）
        """
        # 1. 收集生产环境指标
        production_metrics = self._collect_production_metrics()
        
        # 2. 检测异常
        anomalies = self._detect_anomalies(production_metrics)
        
        # 3. 评估是否需要回滚
        if anomalies:
            rollback_decision = self._evaluate_rollback_need(anomalies)
            if rollback_decision["should_rollback"]:
                self._trigger_rollback(rollback_decision)
        
        # 4. 生成监控报告
        monitoring_report = self._generate_monitoring_report(
            production_metrics, anomalies
        )
        
        # 5. 保存监控记录
        self.monitoring_db.save_monitoring_report(monitoring_report)
    
    def _detect_anomalies(self, metrics: Dict) -> List[Dict]:
        """
        检测异常
        
        异常类型：
        - DDx波动：新版本上线后，DDx分布发生显著变化
        - 风险评估异常：高风险病例识别率下降
        - 特定人群退化：特定人群（如老年人、儿童）的诊断准确率下降
        """
        anomalies = []
        
        # 检测DDx波动
        ddx_anomaly = self._detect_ddx_drift(metrics)
        if ddx_anomaly:
            anomalies.append(ddx_anomaly)
        
        # 检测风险评估异常
        risk_anomaly = self._detect_risk_assessment_anomaly(metrics)
        if risk_anomaly:
            anomalies.append(risk_anomaly)
        
        # 检测特定人群退化
        population_anomaly = self._detect_population_degradation(metrics)
        if population_anomaly:
            anomalies.append(population_anomaly)
        
        return anomalies
    
    def _evaluate_rollback_need(self, anomalies: List[Dict]) -> Dict:
        """
        评估是否需要回滚
        
        回滚触发条件：
        - 严重异常（如高风险病例识别率下降超过10%）
        - 多个异常同时出现
        - 异常持续时间超过阈值
        """
        severity_score = sum(anomaly["severity"] for anomaly in anomalies)
        duration = max(anomaly["duration"] for anomaly in anomalies)
        
        should_rollback = (
            severity_score > 50 or  # 严重异常
            (len(anomalies) >= 2 and duration > 3600)  # 多个异常且持续时间超过1小时
        )
        
        return {
            "should_rollback": should_rollback,
            "anomalies": anomalies,
            "rollback_reason": self._generate_rollback_reason(anomalies),
            "rollback_type": "global" if severity_score > 50 else "partial"
        }
    
    def _trigger_rollback(self, rollback_decision: Dict):
        """
        触发回滚
        """
        self.rollback_engine.execute_rollback(
            rollback_type=rollback_decision["rollback_type"],
            reason=rollback_decision["rollback_reason"],
            anomalies=rollback_decision["anomalies"]
        )
```

#### 3.7.9 Publish Gate（发布门禁）

**技术实现**：

```python
class PublishGate:
    def __init__(self):
        self.staging_db = StagingDatabase()
        self.production_db = ProductionDatabase()
        self.evaluator = ShadowEvaluatorAgent()
    
    def evaluate_candidate_release(self,
                                   candidate_release_id: str) -> Dict:
        """
        评估候选发布包，通过四道门禁
        
        Returns:
            评估结果（包含是否通过、门禁结果）
        """
        candidate_release = self.staging_db.get_candidate_release(
            candidate_release_id
        )
        
        gate_results = {
            "candidate_release_id": candidate_release_id,
            "passed": True,
            "gate_results": {}
        }
        
        # Gate-1: 结构合法性检查
        gate1_result = self._gate1_structure_check(candidate_release)
        gate_results["gate_results"]["gate1"] = gate1_result
        if not gate1_result["passed"]:
            gate_results["passed"] = False
            return gate_results
        
        # Gate-2: 证据可追溯性检查
        gate2_result = self._gate2_provenance_check(candidate_release)
        gate_results["gate_results"]["gate2"] = gate2_result
        if not gate2_result["passed"]:
            gate_results["passed"] = False
            return gate_results
        
        # Gate-3: 回归评测与影子评测
        gate3_result = self._gate3_evaluation(candidate_release)
        gate_results["gate_results"]["gate3"] = gate3_result
        if not gate3_result["passed"]:
            gate_results["passed"] = False
            return gate_results
        
        # Gate-4: 风险分级审批
        gate4_result = self._gate4_risk_approval(candidate_release)
        gate_results["gate_results"]["gate4"] = gate4_result
        if not gate4_result["passed"]:
            gate_results["passed"] = False
            return gate_results
        
        # 如果全部通过，发布到Production
        if gate_results["passed"]:
            self._publish_to_production(candidate_release)
        
        return gate_results
    
    def _gate1_structure_check(self, candidate_release) -> Dict:
        """
        Gate-1: 结构合法性检查
        """
        issues = []
        
        for ko in candidate_release.knowledge_objects:
            # 检查必需字段
            if not ko.ko_id or not ko.ko_type or not ko.content:
                issues.append(f"KO {ko.ko_id} 缺少必需字段")
            
            # 检查concept_ids
            if not ko.concept_ids:
                issues.append(f"KO {ko.ko_id} 缺少concept_ids")
        
        return {
            "passed": len(issues) == 0,
            "issues": issues
        }
    
    def _gate2_provenance_check(self, candidate_release) -> Dict:
        """
        Gate-2: 证据可追溯性检查
        """
        issues = []
        
        for ko in candidate_release.knowledge_objects:
            if not ko.provenance:
                issues.append(f"KO {ko.ko_id} 缺少provenance")
            else:
                # 检查provenance完整性
                for prov in ko.provenance:
                    if not prov.doc_id:
                        issues.append(f"KO {ko.ko_id} provenance缺少doc_id")
        
        return {
            "passed": len(issues) == 0,
            "issues": issues
        }
    
    def _gate3_evaluation(self, candidate_release) -> Dict:
        """
        Gate-3: 回归评测与影子评测
        
        测试契约（Test Contract）：
        
        必跑测试集：
        1. 固定小集（Core Regression Set）：100个固定测试用例
           - 核心DDx场景：50个标准病例（覆盖前10大常见疾病）
           - 红旗病种：20个急危重病例（急性心肌梗死、脑卒中、肺栓塞等）
           - 关键路径：30个关键诊疗路径（覆盖主要诊疗流程）
        2. 抽样大集（Sampling Set）：500个随机抽样病例
        
        硬阈值（Hard Thresholds）：
        - 红旗召回率：不得下降（≥ 基线）
        - 关键路径断裂：必须为0
        - 总体性能下降：不超过5%（≤ 5%）
        - 新增错误：不超过2%（≤ 2%）
        
        软阈值（Soft Thresholds）：
        - 平均响应时间：增加不超过10%
        - 证据完整性：≥ 基线 - 3%
        - 路径可追溯性：≥ 基线 - 5%
        
        通过条件：
        1. 所有硬阈值必须满足
        2. 至少80%的软阈值满足
        3. 评测报告必须完整（包含report_hash）
        4. 评测结果必须入审计
        """
        # 调用Shadow Evaluator Agent进行评测
        evaluation_result = self.evaluator.evaluate(candidate_release)
        
        # 检查硬阈值（所有必须满足）
        hard_thresholds_met = (
            evaluation_result["metrics"]["red_flag_recall"]["threshold_met"] and
            evaluation_result["metrics"]["critical_path_breaks"]["threshold_met"] and
            evaluation_result["metrics"]["overall_accuracy"]["threshold_met"] and
            evaluation_result["metrics"]["new_errors"]["threshold_met"]
        )
        
        if not hard_thresholds_met:
            return {
                "passed": False,
                "reason": "硬阈值不满足",
                "evaluation_result": evaluation_result
            }
        
        # 检查软阈值（至少80%满足）
        soft_thresholds = [
            evaluation_result["metrics"]["avg_response_time"]["threshold_met"],
            evaluation_result["metrics"]["evidence_completeness"]["threshold_met"],
            evaluation_result["metrics"]["path_traceability"]["threshold_met"]
        ]
        soft_thresholds_met_count = sum(soft_thresholds)
        soft_thresholds_met_rate = soft_thresholds_met_count / len(soft_thresholds)
        
        # 检查评测报告完整性
        report_complete = (
            "report_hash" in evaluation_result and
            evaluation_result["report_hash"] and
            "test_sets" in evaluation_result and
            "baseline_version" in evaluation_result
        )
        
        # 综合判断
        passed = (
            hard_thresholds_met and
            soft_thresholds_met_rate >= 0.8 and
            report_complete
        )
        
        # 如果软阈值不满足，标记为需要人工审核
        requires_manual_review = not passed and hard_thresholds_met
        
        return {
            "passed": passed,
            "requires_manual_review": requires_manual_review,
            "hard_thresholds_met": hard_thresholds_met,
            "soft_thresholds_met_rate": soft_thresholds_met_rate,
            "report_complete": report_complete,
            "evaluation_result": evaluation_result
        }
    
    def _gate4_risk_approval(self, candidate_release) -> Dict:
        """
        Gate-4: 风险分级审批
        """
        risk_level = self._assess_risk_level(candidate_release)
        
        if risk_level == "low":
            # 低风险：自动发布
            return {"passed": True, "approval_type": "auto"}
        elif risk_level == "medium":
            # 中风险：需要人工审批
            return {"passed": False, "approval_type": "manual", "requires_approval": True}
        elif risk_level == "high":
            # 高风险：必须人工审批+灰度发布
            return {"passed": False, "approval_type": "manual", "requires_approval": True, "requires_gray_release": True}
    
    def _publish_to_production(self, candidate_release):
        """
        发布到Production
        """
        # 生成新版本号
        new_version = self._generate_new_version()
        
        # 复制KO到Production，标记新版本
        for ko in candidate_release.knowledge_objects:
            ko.status = KOStatus.PUBLISHED
            ko.kg_version = new_version
            ko.release_id = new_version
            self.production_db.save_ko(ko)
        
        # 更新current_release指针
        self._update_current_release(new_version)
```

#### 3.7.6 存储实现方案

**方案：一套Neo4j + release/label分区 + current_release指针**

**技术实现**：

```python
class KnowledgeStorage:
    def __init__(self):
        self.neo4j_client = Neo4jClient()
        self.metadata_db = MetadataDatabase()
    
    def save_ko_to_sandbox(self, ko: KnowledgeObject):
        """
        保存KO到Sandbox
        """
        cypher = """
        CREATE (ko:KnowledgeObject:Release_Sandbox {
            ko_id: $ko_id,
            ko_type: $ko_type,
            content: $content,
            concept_ids: $concept_ids,
            provenance: $provenance,
            status: $status,
            release_id: 'Sandbox'
        })
        """
        self.neo4j_client.execute_query(
            cypher,
            ko_id=ko.ko_id,
            ko_type=ko.ko_type.value,
            content=ko.content,
            concept_ids=ko.concept_ids,
            provenance=[p.__dict__ for p in ko.provenance],
            status=ko.status.value
        )
    
    def query_production_ko(self, ko_id: str, kg_version: str = None):
        """
        查询生产环境的KO
        """
        if not kg_version:
            kg_version = self._get_current_production_version()
        
        cypher = f"""
        MATCH (ko:KnowledgeObject)
        WHERE ko.ko_id = $ko_id
        AND ko.release_id = $kg_version
        AND ko.status = 'published'
        RETURN ko
        """
        
        return self.neo4j_client.execute_query(
            cypher,
            ko_id=ko_id,
            kg_version=kg_version
        )
    
    def _get_current_production_version(self) -> str:
        """
        获取当前生产版本
        """
        result = self.metadata_db.query(
            "SELECT release_id FROM ReleaseMetadata WHERE is_current = true"
        )
        return result[0]["release_id"] if result else "v1.0"
```

---

## 相关文档

- [AI医生系统-技术架构设计-核心架构](./AI医生系统-技术架构设计-核心架构.md)
- [AI医生系统-技术架构设计-工具清单](./AI医生系统-技术架构设计-工具清单.md)
- [AI医生系统-技术架构设计-CDP数据与状态管理](./AI医生系统-技术架构设计-CDP数据与状态管理.md)
- [AI医生系统-技术架构设计-索引](./AI医生系统-技术架构设计-索引.md)
- [知识演化与知识维护-完整设计方案](../../3.知识内容提取/知识库结构设计/知识演化与维护/知识演化与知识维护-完整设计方案.md)

