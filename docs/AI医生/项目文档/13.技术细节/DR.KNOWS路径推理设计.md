# AI医生系统 - DR.KNOWS路径推理设计

> **文档定位**：本文档详细说明DR.KNOWS路径推理方法的技术实现，包括医学概念识别、多跳推理路径检索、路径结构特征分析、路径验证与知识库候选匹配、路径约束推理与验证等核心技术。  
> **相关文档**：
> - [工具技术实现](../4.工具设计/工具技术实现.md)：请参考《工具技术实现》中的知识图谱推理引擎部分
> - [工具业务逻辑设计](../4.工具设计/工具业务逻辑设计.md)：请参考《工具业务逻辑设计》中的鉴别诊断工具部分
> - [路径注入LLM详细说明](./路径注入LLM详细说明.md)：请参考《路径注入LLM详细说明》了解路径如何注入LLM

---

## 一、DR.KNOWS方法概述

### 1.1 核心思想

**核心问题**：如何让大语言模型在医疗诊断中更可靠、更可解释？

**DR.KNOWS的解决方案**：
> **"路径是核心产物，诊断文本是最后的表达层"**

DR.KNOWS不是让LLM自由生成诊断，而是：
1. **先检索医学推理路径**：从知识图谱中找出与患者症状相关的多跳推理路径
2. **对路径进行结构分析**：基于路径结构特征（路径数量、路径长度、关系类型、风险评估等级）选择Top-N路径
3. **将路径注入LLM**：让LLM在受控的推理路径约束下生成诊断
4. **路径验证**：验证LLM推理结果是否符合路径约束

### 1.2 技术架构

```
病历文本
   ↓
医学概念识别（症状/体征）→ QuickUMLS / cTAKES / 中文医学NER
   ↓
医学知识图谱（UMLS）→ Neo4j（450万概念，1500万关系）
   ↓
多跳推理路径生成 → Cypher查询 + 图遍历算法
   ↓
路径结构特征分析 → 路径数量统计、路径长度、关系类型、风险评估等级
   ↓
Top-N 推理路径（不是结论）→ 基于结构特征选择
   ↓
大语言模型（在路径约束下）→ LangChain + T5/ChatGPT
   ↓
诊断预测 + 推理说明（路径验证）
```

### 1.3 核心原则

1. **知识库优先**：知识库中的权威医学内容（主诉知识图谱、疾病知识图谱）是主要来源
2. **路径验证**：DR.KNOWS路径用于验证知识库候选是否有效
3. **路径约束推理**：LLM在路径约束下进行推理，避免自由联想
4. **非数值评分**：不使用数值评分，完全基于路径结构特征和知识库中的风险评估等级

---

## 二、医学概念识别与归一化

### 2.1 技术实现

**工具**：
- QuickUMLS（英文医学实体识别）
- cTAKES（英文医学实体识别）
- 中文医学NER模型（中文医学实体识别）

**功能**：
- 从患者描述中提取医学概念（症状、体征、疾病等）
- 归一化到标准术语（CUI/ICD/SNOMED）
- 输出标准化的医学概念列表

### 2.2 代码结构

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

### 2.3 输出格式

```python
@dataclass
class Concept:
    cui: str  # 统一医学语言系统编码
    text: str  # 原始文本
    category: str  # 概念类别（Symptom/Disease/Examination等）
    confidence: float  # 识别置信度
```

---

## 三、多跳推理路径检索

### 3.1 技术实现

**知识图谱**：
- Neo4j（存储UMLS或自定义医学知识图谱）
- 规模：450万概念，1500万关系

**路径搜索**：
- Cypher查询 + 图遍历算法
- 支持2-4跳路径（症状→中间概念→疾病）

**路径类型**：
1. 症状→疾病路径（DR.KNOWS核心）
2. 疾病→检查路径（扩展）
3. 疾病→治疗路径（扩展）
4. 综合推理路径（多路径融合）

### 3.2 Cypher查询示例

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

### 3.3 路径检索实现

```python
class PathRetriever:
    def retrieve_disease_paths(
        self, 
        symptoms: List[str], 
        max_hops: int = 4,
        min_hops: int = 2
    ) -> List[Dict[str, Any]]:
        """
        多跳推理路径检索（DR.KNOWS核心方法）
        使用Neo4j进行图遍历
        """
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
        
        results = self.neo4j_client.execute_query(
            query, 
            symptom_cuis=symptoms
        )
        
        # 路径去重
        unique_paths = self._deduplicate_paths(results)
        
        return unique_paths
```

---

## 四、路径结构特征分析（非数值评分）

### 4.1 核心原则

> **核心原则**：不使用数值评分，完全基于路径结构特征和知识库中的风险评估等级

### 4.2 路径结构特征

**特征类型**：
- **路径数量统计**：指向同一疾病的路径数量（客观统计）
- **路径长度**：路径跳数（2-4跳）
- **关系类型分布**：直接关系/间接关系
- **患者特征匹配**：路径是否匹配患者特征（年龄、性别等）
- **风险评估等级**：来自知识库的风险评估等级（L1/L2/L3/L4/L5）

### 4.3 路径结构分析实现

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

---

## 五、路径验证与知识库候选匹配

### 5.1 技术实现

**路径验证**：
- 验证知识库候选是否有对应的推理路径支持
- 如果路径指向知识库候选外的疾病，作为补充候选

### 5.2 路径验证实现

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

---

## 六、路径约束推理与验证

### 6.1 核心原则

> **核心原则**：LLM在路径约束下推理，避免自由联想，并对推理结果进行验证

### 6.2 技术实现

**路径约束**：
- 使用Top-N路径约束LLM推理
- 明确要求不得引入路径外的自由联想

**推理验证**：
- 验证LLM推理结果是否符合路径约束
- 路径一致性验证、证据来源验证、路径完整性验证

**降级策略**：
- 如果验证失败，使用路径本身作为推理结果

### 6.3 路径约束推理实现

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

### 6.4 路径验证机制

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

---

## 七、知识库优先 + DR.KNOWS路径验证

### 7.1 核心设计理念

> **核心设计理念**：知识库优先，DR.KNOWS路径用于验证和补充，不使用数值评分，完全基于路径结构特征和知识库中的风险评估等级。

### 7.2 工作流程

1. **知识库候选检索（优先）**
   - 从主诉知识图谱检索Tier1/Tier2/Tier3候选疾病
   - 从疾病知识图谱检索疾病详细信息

2. **DR.KNOWS路径检索**
   - 从Neo4j知识图谱检索症状→疾病的推理路径

3. **路径验证**
   - 验证知识库候选是否有对应的推理路径支持
   - 如果路径指向知识库候选外的疾病，作为补充候选

4. **路径约束推理**
   - LLM在路径约束下进行推理，避免自由联想
   - 推理结果验证

### 7.3 候选整合实现

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
```

---

## 八、总结

### 8.1 核心特点

1. **知识库优先**：知识库中的权威医学内容是主要来源
2. **路径验证**：DR.KNOWS路径用于验证知识库候选是否有效
3. **路径约束推理**：LLM在路径约束下进行推理，避免自由联想
4. **非数值评分**：不使用数值评分，完全基于路径结构特征和知识库中的风险评估等级
5. **可解释性**：所有推理结果都有明确的路径支持，可追溯、可验证

### 8.2 技术优势

1. **可靠性**：路径约束确保推理结果符合医学知识图谱
2. **可解释性**：每个诊断都有明确的推理路径支持
3. **安全性**：路径验证机制防止LLM自由联想产生错误诊断
4. **可扩展性**：支持多种路径类型（症状→疾病、疾病→检查、疾病→治疗）

---

**文档来源**：
- 原文档：《AI医生系统-技术架构设计-核心技术组件.md》3.1 知识图谱推理引擎（第13-326行）
- 原文档：《AI医生系统-技术架构设计-核心技术组件.md》3.2 鉴别诊断工具（第327-734行）- DR.KNOWS相关部分
- 创建时间：2025-01-22
- 文档版本：v1.0

