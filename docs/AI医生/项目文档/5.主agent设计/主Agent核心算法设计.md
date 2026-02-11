# AI医生系统 - 技术架构设计（主Agent核心算法设计）

> **文档定位**：本文档是AI医生系统的**技术架构设计**的主Agent核心算法设计部分，包含证据融合算法、冲突解决算法、工具调用决策算法、停止条件评估算法等核心算法的详细实现。**核心目标**：详细说明主Agent的核心算法实现，确保主Agent能够自主决策、自主调用工具、进行证据融合和冲突解决。

---

## 一、主Agent核心算法概述

### 1.1 算法定位

**主Agent核心算法**是主Agent（Clinical Agent Brain）的核心能力，包括：

1. **证据融合算法（Evidence Fusion）**：融合多个工具返回的证据，形成统一的证据视图
2. **冲突解决算法（Conflict Resolution）**：解决多个工具返回结果之间的冲突
3. **工具调用决策算法（Tool Call Decision）**：决定调用哪些工具、调用顺序、调用参数
4. **停止条件评估算法（Stop Condition Evaluation）**：评估是否满足停止条件

**算法特点**：
- **可追溯**：所有算法决策都有明确的依据和记录
- **可审计**：所有算法执行都记录到AuditTrail
- **可配置**：算法参数可以通过AgentState配置
- **可降级**：算法失败时有明确的降级策略

### 1.2 算法与运行循环的关系

主Agent运行循环中的算法调用：

```
Observe（观察）
  ↓
  - 识别信息缺口（信息缺口识别算法）
  - 识别证据冲突（冲突检测算法）
  - 识别风险信号（风险信号识别算法）
  ↓
Plan（规划）
  ↓
  - 工具调用决策（工具调用决策算法）
  ↓
Act（执行）
  ↓
  - 生成ToolContext
  - 调用工具
  ↓
Update（更新）
  ↓
  - 证据融合（证据融合算法）
  - 冲突解决（冲突解决算法）
  - 决定是否写回CDP
  ↓
Evaluate（评估）
  ↓
  - 停止条件评估（停止条件评估算法）
  - 升级条件评估（升级条件评估算法）
  - 拒答条件评估（拒答条件评估算法）
```

---

## 二、证据融合算法（Evidence Fusion）

### 2.1 算法定位

**证据融合算法**：将多个工具返回的证据融合成统一的证据视图，支持主Agent的决策。

**输入**：
- 多个工具的ToolResult（包含evidence字段）
- 当前CDP状态
- AgentState（evidence_fusion_state）

**输出**：
- 融合后的证据视图（fused_evidence_view）
- 证据冲突列表（conflicts）
- 证据融合状态（evidence_fusion_state）

### 2.2 证据融合策略

#### 2.2.1 证据来源分类

**证据来源类型**：
- `knowledge_base`：知识库来源（主诉知识图谱、疾病知识图谱）
- `kg_path`：知识图谱路径来源（DR.KNOWS路径）
- `rule`：规则来源（规则库）
- `llm`：LLM生成来源（路径约束推理）

**证据强度分类**：
- `strong`：强证据（知识库、规则库）
- `medium`：中证据（知识图谱路径、已验证的LLM推理）
- `weak`：弱证据（未验证的LLM推理）

#### 2.2.2 证据融合规则

**规则1：同源证据合并**
- 如果多个工具返回相同来源的证据，合并为一条证据
- 合并后的证据强度取最高强度
- 合并后的证据引用包含所有来源引用

**规则2：异源证据叠加**
- 如果多个工具返回不同来源的证据，叠加为多条证据
- 每条证据保持独立的来源和强度

**规则3：证据强度排序**
- 按证据强度排序：strong > medium > weak
- 相同强度按来源优先级排序：knowledge_base > kg_path > rule > llm

**规则4：证据去重**
- 如果多条证据指向同一诊断方向且内容相同，去重
- 保留证据强度最高的证据

### 2.3 证据融合算法实现

**算法步骤**：

```
1. 收集所有工具的evidence
   - 从所有ToolResult中提取evidence字段
   - 构建证据列表 evidence_list

2. 按证据来源分类
   - 按source字段分类：knowledge_base、kg_path、rule、llm
   - 构建分类后的证据列表 classified_evidence

3. 同源证据合并
   - 对于每个来源，合并相同内容的证据
   - 合并后的证据强度取最高强度
   - 合并后的证据引用包含所有来源引用

4. 异源证据叠加
   - 将不同来源的证据叠加
   - 保持每条证据的独立性

5. 证据强度排序
   - 按证据强度排序：strong > medium > weak
   - 相同强度按来源优先级排序

6. 证据去重
   - 如果多条证据指向同一诊断方向且内容相同，去重
   - 保留证据强度最高的证据

7. 构建融合后的证据视图
   - 构建 fused_evidence_view
   - 包含：证据列表、证据统计、证据分布

8. 检测证据冲突
   - 检测指向不同诊断方向的证据冲突
   - 构建冲突列表 conflicts

9. 更新evidence_fusion_state
   - 更新 agent_state.evidence_fusion_state
   - 记录融合结果和冲突列表
```

**算法伪代码**：

```
function evidence_fusion(tool_results: List[ToolResult], cdp: CDP, agent_state: AgentState) -> Dict:
    # 1. 收集所有工具的evidence
    evidence_list = []
    for tool_result in tool_results:
        if tool_result.status == "success":
            evidence_list.extend(tool_result.evidence)
    
    # 2. 按证据来源分类
    classified_evidence = {}
    for evidence in evidence_list:
        source = evidence.source
        if source not in classified_evidence:
            classified_evidence[source] = []
        classified_evidence[source].append(evidence)
    
    # 3. 同源证据合并
    merged_evidence = {}
    for source, evidence_list in classified_evidence.items():
        # 按内容分组
        content_groups = group_by_content(evidence_list)
        for content, evidences in content_groups.items():
            # 合并相同内容的证据
            merged = merge_evidences(evidences)
            merged_evidence[source] = merged_evidence.get(source, []) + [merged]
    
    # 4. 异源证据叠加
    fused_evidence_list = []
    for source in ["knowledge_base", "kg_path", "rule", "llm"]:
        if source in merged_evidence:
            fused_evidence_list.extend(merged_evidence[source])
    
    # 5. 证据强度排序
    fused_evidence_list.sort(key=lambda e: (
        strength_priority(e.strength),  # strong=3, medium=2, weak=1
        source_priority(e.source)       # knowledge_base=4, kg_path=3, rule=2, llm=1
    ), reverse=True)
    
    # 6. 证据去重
    deduplicated_evidence = []
    seen_directions = {}
    for evidence in fused_evidence_list:
        direction = evidence.affected_direction
        if direction not in seen_directions:
            seen_directions[direction] = evidence
            deduplicated_evidence.append(evidence)
        else:
            # 如果新证据强度更高，替换旧证据
            if strength_priority(evidence.strength) > strength_priority(seen_directions[direction].strength):
                deduplicated_evidence.remove(seen_directions[direction])
                seen_directions[direction] = evidence
                deduplicated_evidence.append(evidence)
    
    # 7. 构建融合后的证据视图
    fused_evidence_view = {
        "evidence_list": deduplicated_evidence,
        "evidence_count": len(deduplicated_evidence),
        "evidence_distribution": {
            "by_source": count_by_source(deduplicated_evidence),
            "by_strength": count_by_strength(deduplicated_evidence),
            "by_direction": count_by_direction(deduplicated_evidence)
        }
    }
    
    # 8. 检测证据冲突
    conflicts = detect_conflicts(deduplicated_evidence, cdp)
    
    # 9. 更新evidence_fusion_state
    agent_state.evidence_fusion_state = {
        "fused_evidence_view": fused_evidence_view,
        "conflicts": conflicts,
        "fusion_timestamp": current_timestamp()
    }
    
    return {
        "fused_evidence_view": fused_evidence_view,
        "conflicts": conflicts
    }
```

### 2.4 证据冲突检测

**冲突类型**：

1. **诊断概率冲突**：
   - 多个工具对同一诊断给出不同的概率
   - 例如：tool_3返回诊断A概率0.8，tool_4返回诊断A概率0.3

2. **诊断方向冲突**：
   - 多个工具给出不同的诊断方向
   - 例如：tool_3返回诊断A，tool_4返回诊断B

3. **证据方向冲突**：
   - 同一证据被不同工具解释为不同的方向
   - 例如：tool_3认为证据支持诊断A，tool_4认为证据不支持诊断A

**冲突检测算法**：

```
function detect_conflicts(evidence_list: List[Evidence], cdp: CDP) -> List[Conflict]:
    conflicts = []
    
    # 1. 检测诊断概率冲突
    diagnosis_probabilities = {}
    for evidence in evidence_list:
        direction = evidence.affected_direction
        if direction not in diagnosis_probabilities:
            diagnosis_probabilities[direction] = []
        
        # 从CDP中获取该诊断的概率（如果有）
        if direction in cdp.ddx.rank_list:
            prob = cdp.ddx.rank_list[direction].probability
            diagnosis_probabilities[direction].append({
                "source": evidence.source,
                "probability": prob,
                "evidence": evidence
            })
    
    # 检测概率差异
    for direction, probs in diagnosis_probabilities.items():
        if len(probs) > 1:
            prob_values = [p["probability"] for p in probs]
            if max(prob_values) - min(prob_values) > 0.3:  # 阈值
                conflicts.append({
                    "conflict_type": "diagnosis_probability",
                    "direction": direction,
                    "conflicting_values": probs,
                    "severity": "high" if max(prob_values) - min(prob_values) > 0.5 else "medium"
                })
    
    # 2. 检测诊断方向冲突
    # 如果多个工具返回不同的Top-1诊断
    top1_diagnoses = []
    for tool_result in tool_results:
        if tool_result.tool_id == "tool_3" and tool_result.status == "success":
            if tool_result.payload.ddx_rank_list:
                top1_diagnoses.append(tool_result.payload.ddx_rank_list[0].disease_name)
    
    if len(set(top1_diagnoses)) > 1:
        conflicts.append({
            "conflict_type": "diagnosis_direction",
            "conflicting_directions": list(set(top1_diagnoses)),
            "severity": "high"
        })
    
    # 3. 检测证据方向冲突
    evidence_directions = {}
    for evidence in evidence_list:
        key = f"{evidence.evidence_name}_{evidence.affected_direction}"
        if key not in evidence_directions:
            evidence_directions[key] = []
        evidence_directions[key].append(evidence)
    
    for key, evidences in evidence_directions.items():
        directions = [e.evidence_direction for e in evidences]
        if "支持" in directions and "不支持" in directions:
            conflicts.append({
                "conflict_type": "evidence_direction",
                "evidence_name": evidences[0].evidence_name,
                "conflicting_directions": directions,
                "severity": "medium"
            })
    
    return conflicts
```

---

## 三、冲突解决算法（Conflict Resolution）

### 3.1 算法定位

**冲突解决算法**：解决证据融合过程中检测到的冲突，形成统一的决策。

**输入**：
- 证据冲突列表（conflicts）
- 当前CDP状态
- AgentState（evidence_fusion_state.resolution_strategy）

**输出**：
- 冲突解决结果（resolution_result）
- 更新后的CDP（如有需要）
- 更新后的AgentState

### 3.2 冲突解决策略

#### 3.2.1 策略类型

**策略1：优先级规则（Priority Rule）**
- 根据工具优先级解决冲突
- 工具优先级：tool_6（风险评估）> tool_4（检查建议）> tool_3（鉴别诊断）
- 适用于：诊断概率冲突、诊断方向冲突

**策略2：证据强度优先（Evidence Strength Priority）**
- 根据证据强度解决冲突
- 证据强度：strong > medium > weak
- 适用于：证据方向冲突

**策略3：专家投票（Expert Voting）**
- 多个工具投票，根据投票结果解决冲突
- 投票权重：根据工具的历史表现和置信度加权
- 适用于：复杂冲突场景

**策略4：风险优先（Risk Priority）**
- 优先考虑风险等级高的诊断
- 风险等级：L1 > L2 > L3 > L4
- 适用于：涉及高危诊断的冲突

#### 3.2.2 策略选择规则

**策略选择算法**：

```
function select_resolution_strategy(conflict: Conflict, cdp: CDP, agent_state: AgentState) -> String:
    # 1. 如果冲突涉及高危诊断（L1/L2），使用风险优先策略
    if conflict.affected_directions:
        for direction in conflict.affected_directions:
            if direction in cdp.ddx.rank_list:
                risk_level = cdp.ddx.rank_list[direction].risk_level
                if risk_level in ["L1", "L2"]:
                    return "risk_priority"
    
    # 2. 如果冲突类型是诊断概率冲突，使用优先级规则或专家投票
    if conflict.conflict_type == "diagnosis_probability":
        # 如果涉及检查结果，使用优先级规则（检查建议工具优先级高）
        if any("tool_4" in str(c) for c in conflict.conflicting_values):
            return "priority_rule"
        else:
            return "expert_voting"
    
    # 3. 如果冲突类型是证据方向冲突，使用证据强度优先
    if conflict.conflict_type == "evidence_direction":
        return "evidence_strength_priority"
    
    # 4. 如果冲突类型是诊断方向冲突，使用专家投票
    if conflict.conflict_type == "diagnosis_direction":
        return "expert_voting"
    
    # 5. 默认使用专家投票
    return "expert_voting"
```

### 3.3 冲突解决算法实现

**算法步骤**：

```
1. 选择冲突解决策略
   - 根据冲突类型和CDP状态选择策略
   - 策略：优先级规则、证据强度优先、专家投票、风险优先

2. 执行冲突解决策略
   - 根据选择的策略执行解决算法
   - 生成解决结果

3. 验证解决结果
   - 验证解决结果是否合理
   - 如果解决结果不合理，使用降级策略

4. 更新CDP和AgentState
   - 根据解决结果更新CDP
   - 更新AgentState的conflict_resolution字段

5. 记录到AuditTrail
   - 记录冲突解决过程
   - 记录解决结果和依据
```

**算法伪代码**：

```
function conflict_resolution(conflicts: List[Conflict], cdp: CDP, agent_state: AgentState) -> Dict:
    resolution_results = []
    
    for conflict in conflicts:
        # 1. 选择冲突解决策略
        strategy = select_resolution_strategy(conflict, cdp, agent_state)
        
        # 2. 执行冲突解决策略
        if strategy == "priority_rule":
            resolution = resolve_by_priority_rule(conflict, cdp, agent_state)
        elif strategy == "evidence_strength_priority":
            resolution = resolve_by_evidence_strength(conflict, cdp, agent_state)
        elif strategy == "expert_voting":
            resolution = resolve_by_expert_voting(conflict, cdp, agent_state)
        elif strategy == "risk_priority":
            resolution = resolve_by_risk_priority(conflict, cdp, agent_state)
        
        # 3. 验证解决结果
        if not validate_resolution(resolution):
            # 使用降级策略
            resolution = fallback_resolution(conflict, cdp, agent_state)
        
        resolution_results.append({
            "conflict_id": conflict.conflict_id,
            "strategy": strategy,
            "resolution": resolution,
            "confidence": calculate_confidence(resolution)
        })
    
    # 4. 更新CDP和AgentState
    updated_cdp = apply_resolutions(cdp, resolution_results)
    agent_state.evidence_fusion_state.conflict_resolution = resolution_results
    
    # 5. 记录到AuditTrail
    record_conflict_resolution(conflicts, resolution_results, agent_state)
    
    return {
        "resolution_results": resolution_results,
        "updated_cdp": updated_cdp
    }
```

### 3.4 各策略的详细实现

#### 3.4.1 优先级规则（Priority Rule）

**算法实现**：

```
function resolve_by_priority_rule(conflict: Conflict, cdp: CDP, agent_state: AgentState) -> Resolution:
    # 工具优先级：tool_6 > tool_4 > tool_3 > tool_2 > tool_1 > tool_0 > tool_5 > tool_7
    tool_priority = {
        "tool_6": 8,  # 风险评估工具优先级最高
        "tool_4": 7,  # 检查建议工具
        "tool_3": 6,  # 鉴别诊断工具
        "tool_2": 5,  # 主动问诊工具
        "tool_1": 4,  # 病例理解工具
        "tool_0": 3,  # 健康状态判定工具
        "tool_5": 2,  # 治疗建议工具
        "tool_7": 1   # 证据链工具
    }
    
    # 获取冲突中各工具的值
    conflicting_values = conflict.conflicting_values
    
    # 选择优先级最高的工具的值
    highest_priority_tool = None
    highest_priority = 0
    for value in conflicting_values:
        tool_id = value["source"].tool_id
        priority = tool_priority.get(tool_id, 0)
        if priority > highest_priority:
            highest_priority = priority
            highest_priority_tool = value
    
    return {
        "resolved_value": highest_priority_tool["value"],
        "resolution_reason": f"使用优先级规则，选择优先级最高的工具{highest_priority_tool['source'].tool_id}的值",
        "confidence": 0.8  # 优先级规则的置信度
    }
```

#### 3.4.2 证据强度优先（Evidence Strength Priority）

**算法实现**：

```
function resolve_by_evidence_strength(conflict: Conflict, cdp: CDP, agent_state: AgentState) -> Resolution:
    # 证据强度优先级：strong > medium > weak
    strength_priority = {
        "strong": 3,
        "medium": 2,
        "weak": 1
    }
    
    # 获取冲突中的证据
    conflicting_evidences = conflict.conflicting_evidences
    
    # 选择强度最高的证据
    highest_strength_evidence = None
    highest_strength = 0
    for evidence in conflicting_evidences:
        strength = strength_priority.get(evidence.strength, 0)
        if strength > highest_strength:
            highest_strength = strength
            highest_strength_evidence = evidence
    
    return {
        "resolved_value": highest_strength_evidence.evidence_direction,
        "resolution_reason": f"使用证据强度优先规则，选择强度最高的证据（{highest_strength_evidence.strength}）",
        "confidence": 0.85  # 证据强度优先规则的置信度
    }
```

#### 3.4.3 专家投票（Expert Voting）

**算法实现**：

```
function resolve_by_expert_voting(conflict: Conflict, cdp: CDP, agent_state: AgentState) -> Resolution:
    # 工具权重（基于历史表现和置信度）
    tool_weights = {
        "tool_6": 0.4,  # 风险评估工具权重最高
        "tool_4": 0.3,  # 检查建议工具
        "tool_3": 0.2,  # 鉴别诊断工具
        "tool_2": 0.05,  # 主动问诊工具
        "tool_1": 0.03,  # 病例理解工具
        "tool_0": 0.01,  # 健康状态判定工具
        "tool_5": 0.005,  # 治疗建议工具
        "tool_7": 0.005  # 证据链工具
    }
    
    # 获取冲突中各工具的值和权重
    conflicting_values = conflict.conflicting_values
    
    # 计算加权投票
    vote_counts = {}
    for value in conflicting_values:
        tool_id = value["source"].tool_id
        weight = tool_weights.get(tool_id, 0.01)
        vote_value = value["value"]
        
        if vote_value not in vote_counts:
            vote_counts[vote_value] = 0
        vote_counts[vote_value] += weight
    
    # 选择得票最多的值
    resolved_value = max(vote_counts.items(), key=lambda x: x[1])[0]
    total_votes = sum(vote_counts.values())
    confidence = vote_counts[resolved_value] / total_votes
    
    return {
        "resolved_value": resolved_value,
        "resolution_reason": f"使用专家投票规则，{resolved_value}得票最多（{vote_counts[resolved_value]:.2f}/{total_votes:.2f}）",
        "confidence": confidence
    }
```

#### 3.4.4 风险优先（Risk Priority）

**算法实现**：

```
function resolve_by_risk_priority(conflict: Conflict, cdp: CDP, agent_state: AgentState) -> Resolution:
    # 风险等级优先级：L1 > L2 > L3 > L4
    risk_priority = {
        "L1": 4,
        "L2": 3,
        "L3": 2,
        "L4": 1
    }
    
    # 获取冲突中涉及的诊断方向
    affected_directions = conflict.affected_directions
    
    # 选择风险等级最高的诊断
    highest_risk_direction = None
    highest_risk = 0
    for direction in affected_directions:
        if direction in cdp.ddx.rank_list:
            risk_level = cdp.ddx.rank_list[direction].risk_level
            risk = risk_priority.get(risk_level, 0)
            if risk > highest_risk:
                highest_risk = risk
                highest_risk_direction = direction
    
    return {
        "resolved_value": highest_risk_direction,
        "resolution_reason": f"使用风险优先规则，选择风险等级最高的诊断（{highest_risk_direction}，风险等级{cdp.ddx.rank_list[highest_risk_direction].risk_level}）",
        "confidence": 0.9  # 风险优先规则的置信度很高
    }
```

---

## 四、工具调用决策算法（Tool Call Decision）

### 4.1 算法定位

**工具调用决策算法**：决定调用哪些工具、调用顺序、调用参数。

**输入**：
- 当前CDP状态
- AgentState（current_step、tried_tools、budget等）
- 信息缺口列表
- 证据冲突列表

**输出**：
- 工具调用计划（tool_call_plan）
  - 工具列表（tool_list）
  - 调用顺序（call_order）
  - 调用参数（call_params）
  - 并行/串行策略（parallel_strategy）

### 4.2 工具调用决策策略

#### 4.2.1 基于默认诊断路径的决策

**策略**：根据当前诊断步骤（current_step）和默认诊断路径（Step1-5）决定调用哪些工具。

**决策规则**：

```
Step 1: 识别问题
  - 必须调用：tool_1（病例理解工具）
  - 可选调用：tool_2（主动问诊工具，如果信息不足）

Step 2: 构建鉴别诊断候选集并分层
  - 必须调用：tool_3（鉴别诊断工具）
  - 必须调用：tool_6（风险评估工具）

Step 3: 组织候选集并建立分流路径
  - 必须调用：tool_3（鉴别诊断工具）- 推理子组组织模式

Step 4: 采集关键证据并形成排序与验证计划
  - 必须调用：tool_2（主动问诊工具）- 沿着分流路径采集证据
  - 必须调用：tool_3（鉴别诊断工具）- 固化三层排序
  - 必须调用：tool_4（检查建议工具）- 制定验证计划

Step 5: 回填证据并输出终点结论包
  - 必须调用：tool_4（检查建议工具）- 回填检查结果
  - 必须调用：tool_3（鉴别诊断工具）- 更新三层排序
  - 必须调用：tool_5（治疗建议工具）- 生成治疗方案
  - 必须调用：tool_7（证据链工具）- 生成终点结论包
```

#### 4.2.2 基于动态插入策略的决策

**策略**：根据动态插入策略（红旗优先、冲突复核、证据不足触发检索）决定调用哪些工具。

**决策规则**：

```
红旗优先（Red Flag Priority）
  - 立即调用：tool_6（风险评估工具）- 详细风险评估模式
  - 如果风险等级L1/L2：立即执行升级策略，不调用其他工具

冲突复核（Conflict Review）
  - 调用：相关工具重新评估（根据冲突类型决定）
  - 调用：tool_6（风险评估工具）- 评估风险

证据不足触发检索（Insufficient Evidence Trigger Retrieval）
  - 调用：tool_3（鉴别诊断工具）- 扩展检索模式
  - 如果检索结果仍不足：触发升级或拒答
```

#### 4.2.3 基于预算和约束的决策

**策略**：根据AgentState中的budget和constraints决定是否调用工具。

**决策规则**：

```
预算检查
  - 如果 current_tool_calls >= max_tool_calls：不调用新工具，触发升级或拒答
  - 如果 current_time_seconds >= max_time_seconds：不调用新工具，触发升级或拒答
  - 如果 current_cost >= max_cost：不调用新工具，触发升级或拒答

约束检查
  - 如果工具调用会超过max_time_seconds：不调用该工具
  - 如果工具调用会超过max_cost：不调用该工具
  - 如果工具调用会超过risk_level_limit：不调用该工具
```

### 4.3 工具调用决策算法实现

**算法步骤**：

```
1. 检查预算和约束
   - 检查是否超过max_tool_calls、max_time_seconds、max_cost
   - 如果超过，返回空计划，触发升级或拒答

2. 识别信息缺口和证据冲突
   - 从CDP中识别信息缺口（uncertainty.missing_critical_info）
   - 从evidence_fusion_state中识别证据冲突

3. 检查动态插入策略
   - 检查是否需要红旗优先处理
   - 检查是否需要冲突复核
   - 检查是否需要证据不足触发检索

4. 根据当前步骤和默认路径决定工具调用
   - 根据current_step决定必须调用的工具
   - 根据信息缺口决定可选调用的工具

5. 决定调用顺序和并行策略
   - 决定工具调用的顺序（串行/并行）
   - 决定哪些工具可以并行调用

6. 生成调用参数
   - 为每个工具生成ToolContext
   - 设置调用参数（call_params）

7. 构建工具调用计划
   - 构建tool_call_plan
   - 包含工具列表、调用顺序、调用参数、并行策略
```

**算法伪代码**：

```
function tool_call_decision(cdp: CDP, agent_state: AgentState) -> ToolCallPlan:
    # 1. 检查预算和约束
    if agent_state.budget.current_tool_calls >= agent_state.budget.max_tool_calls:
        return {
            "tool_list": [],
            "reason": "超过最大工具调用次数",
            "action": "escalate"
        }
    
    if agent_state.budget.current_time_seconds >= agent_state.budget.max_time_seconds:
        return {
            "tool_list": [],
            "reason": "超过最大执行时间",
            "action": "escalate"
        }
    
    # 2. 识别信息缺口和证据冲突
    missing_info = cdp.uncertainty.missing_critical_info if cdp.uncertainty else []
    conflicts = agent_state.evidence_fusion_state.conflicts if agent_state.evidence_fusion_state else []
    
    # 3. 检查动态插入策略
    # 3.1 红旗优先
    if has_red_flags(cdp):
        return {
            "tool_list": [{
                "tool_id": "tool_6",
                "call_mode": "sync",
                "priority": "high",
                "call_params": {"mode": "detailed_risk_assessment"}
            }],
            "call_order": "sequential",
            "reason": "红旗优先处理"
        }
    
    # 3.2 冲突复核
    if conflicts:
        return {
            "tool_list": build_conflict_review_tools(conflicts),
            "call_order": "sequential",
            "reason": "冲突复核"
        }
    
    # 3.3 证据不足触发检索
    if len(missing_info) > 0 and evidence_count(cdp) < agent_state.thresholds.evidence_count_threshold:
        return {
            "tool_list": [{
                "tool_id": "tool_3",
                "call_mode": "sync",
                "priority": "high",
                "call_params": {"mode": "extended_retrieval"}
            }],
            "call_order": "sequential",
            "reason": "证据不足触发检索"
        }
    
    # 4. 根据当前步骤和默认路径决定工具调用
    current_step = agent_state.current_step
    tool_list = []
    
    if current_step == 1:
        # Step 1: 识别问题
        tool_list.append({
            "tool_id": "tool_1",
            "call_mode": "sync",
            "priority": "high",
            "call_params": {}
        })
        if missing_info:
            tool_list.append({
                "tool_id": "tool_2",
                "call_mode": "sync",
                "priority": "medium",
                "call_params": {"focus": missing_info}
            })
    
    elif current_step == 2:
        # Step 2: 构建鉴别诊断候选集并分层
        tool_list.append({
            "tool_id": "tool_3",
            "call_mode": "sync",
            "priority": "high",
            "call_params": {}
        })
        tool_list.append({
            "tool_id": "tool_6",
            "call_mode": "sync",
            "priority": "high",
            "call_params": {}
        })
    
    elif current_step == 3:
        # Step 3: 组织候选集并建立分流路径
        tool_list.append({
            "tool_id": "tool_3",
            "call_mode": "sync",
            "priority": "high",
            "call_params": {"mode": "reasoning_subgroup_organization"}
        })
    
    elif current_step == 4:
        # Step 4: 采集关键证据并形成排序与验证计划
        tool_list.append({
            "tool_id": "tool_2",
            "call_mode": "sync",
            "priority": "high",
            "call_params": {"mode": "routing_path_collection"}
        })
        tool_list.append({
            "tool_id": "tool_3",
            "call_mode": "sync",
            "priority": "high",
            "call_params": {"mode": "three_layer_ranking"}
        })
        tool_list.append({
            "tool_id": "tool_4",
            "call_mode": "sync",
            "priority": "high",
            "call_params": {"mode": "verification_plan"}
        })
    
    elif current_step == 5:
        # Step 5: 回填证据并输出终点结论包
        tool_list.append({
            "tool_id": "tool_4",
            "call_mode": "sync",
            "priority": "high",
            "call_params": {"mode": "backfill_results"}
        })
        tool_list.append({
            "tool_id": "tool_3",
            "call_mode": "sync",
            "priority": "high",
            "call_params": {"mode": "update_ranking"}
        })
        tool_list.append({
            "tool_id": "tool_5",
            "call_mode": "sync",
            "priority": "high",
            "call_params": {}
        })
        tool_list.append({
            "tool_id": "tool_7",
            "call_mode": "async",  # 可以异步调用
            "priority": "medium",
            "call_params": {}
        })
    
    # 5. 决定调用顺序和并行策略
    call_order, parallel_strategy = decide_call_order(tool_list)
    
    # 6. 生成调用参数
    for tool in tool_list:
        tool["tool_context"] = generate_tool_context(tool, cdp, agent_state)
    
    # 7. 构建工具调用计划
    return {
        "tool_list": tool_list,
        "call_order": call_order,
        "parallel_strategy": parallel_strategy,
        "reason": f"基于Step {current_step}的默认诊断路径"
    }
```

### 4.4 并行/串行策略决策

**并行策略决策算法**：

```
function decide_call_order(tool_list: List[Tool]) -> Tuple[str, Dict]:
    # 工具依赖关系
    dependencies = {
        "tool_2": ["tool_1"],  # 主动问诊工具依赖病例理解工具
        "tool_3": ["tool_1"],  # 鉴别诊断工具依赖病例理解工具
        "tool_4": ["tool_3", "tool_6"],  # 检查建议工具依赖鉴别诊断工具和风险评估工具
        "tool_5": ["tool_3", "tool_6"],  # 治疗建议工具依赖鉴别诊断工具和风险评估工具
        "tool_7": ["tool_3", "tool_4", "tool_5"]  # 证据链工具依赖多个工具
    }
    
    # 构建依赖图
    dependency_graph = build_dependency_graph(tool_list, dependencies)
    
    # 拓扑排序决定调用顺序
    call_order = topological_sort(dependency_graph)
    
    # 决定并行策略
    parallel_groups = []
    current_group = []
    for tool in call_order:
        # 如果工具没有依赖或依赖已满足，可以并行
        if can_parallelize(tool, current_group, dependencies):
            current_group.append(tool)
        else:
            if current_group:
                parallel_groups.append(current_group)
            current_group = [tool]
    
    if current_group:
        parallel_groups.append(current_group)
    
    return {
        "call_order": call_order,
        "parallel_groups": parallel_groups,
        "parallel_strategy": "mixed" if len(parallel_groups) > 1 else "sequential"
    }
```

---

## 五、停止条件评估算法（Stop Condition Evaluation）

### 5.1 算法定位

**停止条件评估算法**：评估是否满足停止条件，决定主Agent是否停止运行循环。

**输入**：
- 当前CDP状态
- AgentState（stop_conditions）

**输出**：
- 停止条件评估结果（stop_condition_result）
  - 是否满足停止条件（satisfied）
  - 满足的条件列表（satisfied_conditions）
  - 未满足的条件列表（unsatisfied_conditions）
  - 评估依据（evaluation_reason）

### 5.2 停止条件列表

**停止条件**（必须全部满足）：

1. **CDP必填项完成**：`cdp.health_state_assessment`、`cdp.patient_state`、`cdp.ddx`等必填字段已完成
2. **证据引用齐全**：所有诊断结论都有完整的evidence引用
3. **风险评估完成**：`cdp.triage`不为空，风险评估已完成
4. **诊断结论明确**：`cdp.ddx.tier1_most_likely`不为空，至少有一个首要假设
5. **检查建议完成**：`cdp.workup_plan`不为空或明确不需要检查
6. **治疗建议完成**：`cdp.management_plan`不为空
7. **证据链完整**：`cdp.evidence_graph`不为空，证据链已构建
8. **终点结论包生成**：`cdp.final_conclusion`不为空，终点结论包已生成

### 5.3 停止条件评估算法实现

**算法步骤**：

```
1. 检查CDP必填项
   - 检查 cdp.health_state_assessment 是否完成
   - 检查 cdp.patient_state 是否完成
   - 检查 cdp.ddx 是否完成

2. 检查证据引用齐全
   - 检查所有诊断结论是否有evidence引用
   - 检查evidence引用的完整性

3. 检查风险评估完成
   - 检查 cdp.triage 是否不为空
   - 检查风险评估是否完成

4. 检查诊断结论明确
   - 检查 cdp.ddx.tier1_most_likely 是否不为空

5. 检查检查建议完成
   - 检查 cdp.workup_plan 是否不为空或明确不需要检查

6. 检查治疗建议完成
   - 检查 cdp.management_plan 是否不为空

7. 检查证据链完整
   - 检查 cdp.evidence_graph 是否不为空

8. 检查终点结论包生成
   - 检查 cdp.final_conclusion 是否不为空

9. 汇总评估结果
   - 汇总满足的条件和未满足的条件
   - 生成评估依据
```

**算法伪代码**：

```
function stop_condition_evaluation(cdp: CDP, agent_state: AgentState) -> StopConditionResult:
    satisfied_conditions = []
    unsatisfied_conditions = []
    
    # 1. 检查CDP必填项
    if cdp.health_state_assessment and cdp.patient_state and cdp.ddx:
        satisfied_conditions.append("cdp_required_fields_complete")
        agent_state.stop_conditions.cdp_required_fields_complete = True
    else:
        unsatisfied_conditions.append("cdp_required_fields_complete")
        agent_state.stop_conditions.cdp_required_fields_complete = False
    
    # 2. 检查证据引用齐全
    if check_evidence_references_complete(cdp):
        satisfied_conditions.append("evidence_references_complete")
        agent_state.stop_conditions.evidence_references_complete = True
    else:
        unsatisfied_conditions.append("evidence_references_complete")
        agent_state.stop_conditions.evidence_references_complete = False
    
    # 3. 检查风险评估完成
    if cdp.triage and cdp.triage.risk_level:
        satisfied_conditions.append("risk_assessment_complete")
        agent_state.stop_conditions.risk_assessment_complete = True
    else:
        unsatisfied_conditions.append("risk_assessment_complete")
        agent_state.stop_conditions.risk_assessment_complete = False
    
    # 4. 检查诊断结论明确
    if cdp.ddx and cdp.ddx.tier1_most_likely and len(cdp.ddx.tier1_most_likely) > 0:
        satisfied_conditions.append("diagnosis_conclusion_clear")
        agent_state.stop_conditions.diagnosis_conclusion_clear = True
    else:
        unsatisfied_conditions.append("diagnosis_conclusion_clear")
        agent_state.stop_conditions.diagnosis_conclusion_clear = False
    
    # 5. 检查检查建议完成
    if cdp.workup_plan or (cdp.uncertainty and cdp.uncertainty.no_workup_needed):
        satisfied_conditions.append("workup_plan_complete")
        agent_state.stop_conditions.workup_plan_complete = True
    else:
        unsatisfied_conditions.append("workup_plan_complete")
        agent_state.stop_conditions.workup_plan_complete = False
    
    # 6. 检查治疗建议完成
    if cdp.management_plan:
        satisfied_conditions.append("management_plan_complete")
        agent_state.stop_conditions.management_plan_complete = True
    else:
        unsatisfied_conditions.append("management_plan_complete")
        agent_state.stop_conditions.management_plan_complete = False
    
    # 7. 检查证据链完整
    if cdp.evidence_graph and len(cdp.evidence_graph.evidence_nodes) > 0:
        satisfied_conditions.append("evidence_chain_complete")
        agent_state.stop_conditions.evidence_chain_complete = True
    else:
        unsatisfied_conditions.append("evidence_chain_complete")
        agent_state.stop_conditions.evidence_chain_complete = False
    
    # 8. 检查终点结论包生成
    if cdp.final_conclusion:
        satisfied_conditions.append("final_conclusion_generated")
        agent_state.stop_conditions.final_conclusion_generated = True
    else:
        unsatisfied_conditions.append("final_conclusion_generated")
        agent_state.stop_conditions.final_conclusion_generated = False
    
    # 9. 汇总评估结果
    satisfied = len(unsatisfied_conditions) == 0
    
    evaluation_reason = f"满足{len(satisfied_conditions)}/8个停止条件"
    if unsatisfied_conditions:
        evaluation_reason += f"，未满足：{', '.join(unsatisfied_conditions)}"
    
    return {
        "satisfied": satisfied,
        "satisfied_conditions": satisfied_conditions,
        "unsatisfied_conditions": unsatisfied_conditions,
        "evaluation_reason": evaluation_reason
    }
```

### 5.4 证据引用完整性检查

**证据引用完整性检查算法**：

```
function check_evidence_references_complete(cdp: CDP) -> Boolean:
    # 检查所有诊断结论是否有evidence引用
    if not cdp.ddx or not cdp.ddx.rank_list:
        return False
    
    for diagnosis in cdp.ddx.rank_list:
        # 检查诊断是否有evidence引用
        if not diagnosis.evidence_references or len(diagnosis.evidence_references) == 0:
            return False
        
        # 检查evidence引用的完整性
        for evidence_ref in diagnosis.evidence_references:
            if not evidence_ref.source or not evidence_ref.reference:
                return False
    
    return True
```

---

## 六、算法降级策略

### 6.1 证据融合算法降级策略

**降级场景**：
- 证据融合算法执行失败
- 证据冲突检测失败

**降级策略**：
- 使用简化融合策略：只保留strong证据，忽略medium和weak证据
- 如果简化融合仍失败，使用原始证据列表，不进行融合

### 6.2 冲突解决算法降级策略

**降级场景**：
- 冲突解决算法执行失败
- 冲突解决结果不合理

**降级策略**：
- 使用默认策略：优先级规则（最简单、最可靠）
- 如果优先级规则仍失败，标记冲突为"未解决"，触发升级或拒答

### 6.3 工具调用决策算法降级策略

**降级场景**：
- 工具调用决策算法执行失败
- 无法决定调用哪些工具

**降级策略**：
- 使用默认路径：根据current_step调用默认工具
- 如果默认路径仍失败，触发升级或拒答

### 6.4 停止条件评估算法降级策略

**降级场景**：
- 停止条件评估算法执行失败
- 无法判断是否满足停止条件

**降级策略**：
- 使用保守策略：如果无法判断，默认不满足停止条件，继续运行循环
- 如果运行循环次数过多，触发升级或拒答
