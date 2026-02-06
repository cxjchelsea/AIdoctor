# 路径注入LLM详细说明

## 一、路径注入的具体内容

### 1.1 注入的是什么？

**路径注入不是注入整个知识图谱**，而是只注入**Top-N条推理路径**（通常是Top-10）。

每条推理路径包含：
- **路径节点**：症状节点、中间节点（如系统来源、病理机制）、疾病节点
- **路径关系**：节点之间的医学关系（如"引起"、"导致"、"属于"等）
- **路径长度**：路径的跳数（2跳、3跳、4跳等）
- **目标疾病**：路径指向的疾病

### 1.2 路径格式化示例

```python
# 路径数据结构（从Neo4j检索返回）
path = {
    "node_names": ["胸痛", "心血管系统", "急性心肌梗死"],
    "relation_types": ["属于", "引起"],
    "path_length": 2,
    "disease_name": "急性心肌梗死",
    "disease_cui": "C0027051",
    "description": "胸痛 -> 心血管系统 -> 急性心肌梗死"
}

# 注意：节点可能包含更多属性（但当前实现不注入）
# 节点完整属性示例（来自Neo4j）：
# {
#     "name": "急性心肌梗死",
#     "cui": "C0027051",
#     "type": "disease",
#     "definition": "急性心肌梗死是指...",
#     "typical_manifestations": ["胸痛", "ST段抬高", "心肌酶升高"],
#     "icd10_code": "I21.9",
#     ...
# }

# 格式化后的文本（注入到LLM）- 当前实现
"""
推理路径1：
胸痛 --[属于]--> 心血管系统 --[引起]--> 急性心肌梗死
（路径长度：2跳，目标疾病：急性心肌梗死）

推理路径2：
胸痛 --[属于]--> 呼吸系统 --[引起]--> 肺炎
（路径长度：2跳，目标疾病：肺炎）

...
（共Top-10条路径）
"""
```

### 1.2.1 节点属性注入情况

**当前实现**：**只注入节点名称和关系类型**，不注入节点的其他属性。

**注入的内容**：
- ✅ 节点名称（name）
- ✅ 关系类型（relation type）
- ✅ 路径长度（path length）
- ✅ 目标疾病名称（target disease name）

**不注入的内容**：
- ❌ 节点CUI（概念唯一标识符）
- ❌ 节点类型（symptom/disease/system等）
- ❌ 节点定义（definition）
- ❌ 典型表现（typical_manifestations）
- ❌ ICD10编码（icd10_code）
- ❌ 其他节点属性

**原因**：
1. **简化Prompt**：避免Prompt过长，影响LLM理解
2. **减少噪音**：节点属性可能包含冗余信息
3. **当前实现**：路径检索只返回节点名称，不返回完整节点属性

**潜在改进**：
如果需要更丰富的上下文，可以考虑注入关键节点属性：
- 疾病定义（definition）：帮助LLM理解疾病
- 典型表现（typical_manifestations）：帮助LLM理解疾病特征
- 节点类型（type）：帮助LLM理解节点语义

### 1.3 路径选择依据

**路径选择的排序依据是知识库中的内容**（Tier1/Tier2/Tier3），而不是路径长度、数量、风险评估等级：

1. **第一优先级**：指向Tier1候选的路径（最可能）
2. **第二优先级**：指向Tier2候选的路径（必须优先除外）
3. **第三优先级**：指向Tier3候选的路径（积极备选）
4. **第四优先级**：补充候选路径（用于知识库完善）

### 1.4 注入的Prompt结构

```python
prompt = f"""
你是一位经验丰富的医生，需要根据患者的症状、体征、健康档案等信息，
结合以下医学推理路径，分析可能的疾病方向。

患者信息：
{patient_info}

医学推理路径（来自知识图谱，基于知识库候选选择）：
{paths_text}  # Top-10条路径的格式化文本

请根据以上医学推理路径，分析：
1. 可能的疾病方向（Top 3-5），按可能性排序
2. 每个方向的支持证据（症状、体征、检查结果）
3. 每个方向的反对证据
4. 还需要哪些信息来进一步判断
5. 建议做哪些检查来辅助诊断

注意：
- **必须基于提供的路径推理，不得引入路径外的自由联想**
- 优先考虑推理路径中提到的疾病方向
- 不要给出确诊结论，使用"可能"、"考虑"等表述
- 如果信息不足，明确说明
- 如果推理路径与患者情况不符，请说明原因
"""
```

## 二、路径注入的可靠性保障

### 2.1 为什么需要路径注入？

**问题**：LLM在自由推理时可能产生"幻觉"（hallucination），即生成不符合医学逻辑的推理。

**解决方案**：通过路径注入，将LLM的推理**约束在知识图谱的路径范围内**，避免自由联想。

### 2.2 可靠性保障机制

#### 2.2.1 Prompt设计约束

- **明确要求**：在Prompt中明确要求"必须基于提供的路径推理，不得引入路径外的自由联想"
- **路径优先**：要求"优先考虑推理路径中提到的疾病方向"
- **不确定性表达**：要求"不要给出确诊结论，使用'可能'、'考虑'等表述"

#### 2.2.2 推理结果验证（关键机制）

在LLM推理完成后，对推理结果进行**四层验证**：

1. **路径一致性验证**：
   - 检查LLM输出的诊断是否在路径中
   - 如果诊断不在任何路径中，标记为"路径外诊断"

2. **证据来源验证**：
   - 检查LLM输出的证据是否来自路径
   - 如果证据不在路径中，标记为"路径外证据"

3. **路径完整性验证**：
   - 检查LLM是否使用了所有Top-N路径
   - 如果某些路径未被使用，记录为"未使用路径"

4. **医学逻辑验证**：
   - 检查LLM的推理是否符合医学逻辑
   - 如果推理逻辑不合理，标记为"逻辑错误"

#### 2.2.3 降级策略

如果验证失败，采用**降级策略**：

1. **使用路径本身作为推理结果**：
   - 如果LLM推理不符合路径约束，直接使用路径中的信息
   - 例如：如果路径指向"急性心肌梗死"，直接使用该诊断

2. **标记为"需要人工审核"**：
   - 验证失败的推理结果标记为"需要人工审核"
   - 不直接用于诊断，需要医生复核

3. **记录验证失败的原因**：
   - 记录验证失败的具体原因（路径外诊断、路径外证据、逻辑错误等）
   - 用于后续优化Prompt设计和路径质量

### 2.3 可靠性评估

#### 2.3.1 优势

1. **约束推理方向**：路径注入将LLM的推理约束在知识图谱范围内，避免自由联想
2. **可追溯性**：每个诊断结论都能追溯到具体的推理路径
3. **验证机制**：四层验证机制确保推理结果符合路径约束
4. **降级策略**：即使验证失败，也有降级策略保证系统可用性

#### 2.3.2 潜在风险

1. **路径质量依赖**：如果路径质量不高（路径不完整、路径错误），可能影响推理质量
2. **LLM不遵循约束**：即使有明确的约束要求，LLM仍可能不遵循路径约束
3. **路径覆盖不足**：如果知识图谱中缺少某些疾病的路径，可能无法推理出该疾病

#### 2.3.3 风险缓解措施

1. **路径质量保障**：
   - 路径来自权威医学知识图谱（如UMLS、SNOMED CT）
   - 路径经过医学专家审核
   - 定期更新路径库

2. **Prompt优化**：
   - 持续优化Prompt设计，强调路径约束
   - 使用few-shot learning，提供遵循路径约束的示例

3. **验证机制**：
   - 四层验证机制确保推理结果符合路径约束
   - 降级策略保证系统可用性

4. **人工审核**：
   - 验证失败的推理结果标记为"需要人工审核"
   - 医生可以复核并纠正推理结果

## 三、路径注入的技术实现

### 3.1 路径选择

```python
class PathSelector:
    def select_top_paths(self, 
                        paths: List[Path], 
                        knowledge_base_candidates: Dict,  # 知识库候选（Tier1/Tier2/Tier3）
                        max_paths: int = 10) -> List[Path]:
        """
        基于知识库候选选择Top-N路径，排序依据是知识库中的内容
        """
        # 1. 将路径按知识库层级分组
        tier1_paths = []  # 指向Tier1候选的路径
        tier2_paths = []  # 指向Tier2候选的路径
        tier3_paths = []  # 指向Tier3候选的路径
        supplementary_paths = []  # 补充候选路径
        
        for path in paths:
            target_disease = path.target_disease
            
            # 检查路径是否指向知识库候选
            if target_disease in knowledge_base_candidates.get('Tier1', []):
                tier1_paths.append(path)
            elif target_disease in knowledge_base_candidates.get('Tier2', []):
                tier2_paths.append(path)
            elif target_disease in knowledge_base_candidates.get('Tier3', []):
                tier3_paths.append(path)
            else:
                supplementary_paths.append(path)
        
        # 2. 按知识库层级优先级选择路径
        selected_paths = []
        selected_paths.extend(tier1_paths[:max_paths])
        
        if len(selected_paths) < max_paths:
            remaining = max_paths - len(selected_paths)
            selected_paths.extend(tier2_paths[:remaining])
        
        if len(selected_paths) < max_paths:
            remaining = max_paths - len(selected_paths)
            selected_paths.extend(tier3_paths[:remaining])
        
        if len(selected_paths) < max_paths:
            remaining = max_paths - len(selected_paths)
            selected_paths.extend(supplementary_paths[:remaining])
        
        return selected_paths[:max_paths]
```

### 3.2 路径格式化

```python
class PathInjector:
    def format_paths_for_llm(self, paths: List[Path]) -> str:
        """
        将路径格式化为自然语言描述（不包含评分）
        
        注意：当前实现只注入节点名称和关系类型，不注入节点属性
        """
        path_descriptions = []
        
        for i, path in enumerate(paths, 1):
            description = f"推理路径{i}："
            
            # 当前实现：只使用节点名称
            node_names = path.get("node_names", [])
            relation_types = path.get("relation_types", [])
            
            for j in range(len(node_names) - 1):
                from_node_name = node_names[j]
                to_node_name = node_names[j + 1]
                rel_type = relation_types[j] if j < len(relation_types) else "相关"
                
                description += f"{from_node_name} --[{rel_type}]--> {to_node_name}; "
            
            description += f"（路径长度：{path.get('path_length', 0)}跳，目标疾病：{path.get('disease_name', 'N/A')}）"
            path_descriptions.append(description)
        
        return "\n".join(path_descriptions)
    
    def format_paths_with_attributes(self, paths: List[Path], include_attributes: bool = False) -> str:
        """
        格式化路径（可选：包含节点属性）
        
        Args:
            paths: 路径列表
            include_attributes: 是否包含节点属性（如定义、典型表现等）
        
        Returns:
            格式化后的路径文本
        """
        if not include_attributes:
            return self.format_paths_for_llm(paths)
        
        # 如果包含属性，需要从Neo4j查询完整节点信息
        path_descriptions = []
        
        for i, path in enumerate(paths, 1):
            description = f"推理路径{i}：\n"
            
            node_names = path.get("node_names", [])
            relation_types = path.get("relation_types", [])
            
            # 查询节点完整属性（需要额外查询Neo4j）
            for j, node_name in enumerate(node_names):
                # 这里需要查询Neo4j获取节点属性
                # node_properties = self.kg_client.get_node_properties(node_name)
                
                description += f"  {j+1}. {node_name}"
                
                # 如果包含属性，添加节点属性信息
                # if include_attributes and node_properties:
                #     if node_properties.get("type") == "disease":
                #         definition = node_properties.get("definition", "")
                #         if definition:
                #             description += f"（定义：{definition}）"
                #         typical_manifestations = node_properties.get("typical_manifestations", [])
                #         if typical_manifestations:
                #             description += f"（典型表现：{', '.join(typical_manifestations)}）"
                
                if j < len(relation_types):
                    description += f" --[{relation_types[j]}]--> "
            
            description += f"\n（路径长度：{path.get('path_length', 0)}跳，目标疾病：{path.get('disease_name', 'N/A')}）"
            path_descriptions.append(description)
        
        return "\n".join(path_descriptions)
```

### 3.3 推理结果验证

```python
class ReasoningValidator:
    def validate_reasoning_results(self, 
                                   llm_output: Dict,
                                   paths: List[Path]) -> Dict:
        """
        验证LLM推理结果是否符合路径约束
        """
        validation_result = {
            "is_valid": True,
            "errors": [],
            "warnings": []
        }
        
        # 1. 路径一致性验证
        diagnoses = llm_output.get("possibilities", {})
        path_diseases = {path.target_disease for path in paths}
        
        for disease in diagnoses.keys():
            if disease not in path_diseases:
                validation_result["is_valid"] = False
                validation_result["errors"].append(
                    f"诊断'{disease}'不在任何路径中"
                )
        
        # 2. 证据来源验证
        supporting_evidence = llm_output.get("supporting_evidence", {})
        path_nodes = {node.name for path in paths for node in path.nodes}
        
        for disease, evidence_list in supporting_evidence.items():
            for evidence in evidence_list:
                if evidence not in path_nodes:
                    validation_result["warnings"].append(
                        f"证据'{evidence}'不在路径中（疾病：{disease}）"
                    )
        
        # 3. 路径完整性验证
        used_paths = llm_output.get("reasoning_paths_used", [])
        if len(used_paths) < len(paths) * 0.5:  # 至少使用50%的路径
            validation_result["warnings"].append(
                f"只使用了{len(used_paths)}/{len(paths)}条路径"
            )
        
        # 4. 医学逻辑验证（简化版）
        # 这里可以添加更复杂的医学逻辑验证
        
        return validation_result
```

## 四、总结

### 4.1 路径注入的核心要点

1. **不是注入整个知识图谱**，而是只注入Top-N条推理路径（通常是Top-10）
2. **路径选择的排序依据是知识库中的内容**（Tier1/Tier2/Tier3）
3. **路径格式化**为自然语言描述，便于LLM理解
4. **路径约束推理**，避免LLM自由联想

### 4.2 可靠性保障

1. **Prompt设计约束**：明确要求遵循路径约束
2. **四层验证机制**：路径一致性、证据来源、路径完整性、医学逻辑验证
3. **降级策略**：验证失败时使用路径本身作为推理结果
4. **人工审核**：验证失败的推理结果标记为"需要人工审核"

### 4.3 潜在风险与缓解措施

1. **路径质量依赖**：通过权威知识图谱和专家审核保障路径质量
2. **LLM不遵循约束**：通过Prompt优化和验证机制缓解
3. **路径覆盖不足**：通过持续更新路径库和补充候选路径缓解

### 4.4 建议

1. **持续优化Prompt设计**，强调路径约束
2. **完善验证机制**，提高验证准确性
3. **建立路径质量评估体系**，定期评估路径质量
4. **收集验证失败案例**，用于系统优化
5. **考虑注入关键节点属性**（可选）：
   - 如果发现LLM推理质量不足，可以考虑注入关键节点属性
   - 建议注入的属性：疾病定义（definition）、典型表现（typical_manifestations）
   - 需要权衡：增加Prompt长度 vs 提高推理质量

## 五、节点属性注入的权衡分析

### 5.1 当前实现（不注入节点属性）

**优势**：
- ✅ Prompt简洁，LLM容易理解
- ✅ 减少Token消耗，降低API成本
- ✅ 减少噪音，避免冗余信息干扰

**劣势**：
- ❌ LLM可能无法准确理解节点语义
- ❌ 缺少疾病定义，可能影响推理准确性
- ❌ 缺少典型表现，可能影响证据匹配

### 5.2 注入节点属性的潜在方案

**方案1：注入关键属性（推荐）**
- 只注入关键属性：疾病定义、典型表现
- 保持Prompt相对简洁
- 平衡信息量和Token消耗

**方案2：注入完整属性**
- 注入所有节点属性
- 信息最全面，但Prompt会很长
- 可能超出LLM上下文窗口限制

**方案3：按需注入**
- 根据路径类型和节点类型，选择性注入属性
- 例如：疾病节点注入定义，症状节点注入描述
- 需要复杂的逻辑判断

### 5.3 建议

**当前阶段**：保持当前实现（不注入节点属性），因为：
1. 路径本身已经提供了足够的推理线索
2. 节点名称通常足够清晰，LLM可以理解
3. 减少Token消耗，提高响应速度

**未来优化**：如果发现推理质量不足，可以考虑：
1. 先尝试注入疾病定义（definition）
2. 评估效果后，再决定是否注入其他属性
3. 使用A/B测试，对比注入前后的推理质量

