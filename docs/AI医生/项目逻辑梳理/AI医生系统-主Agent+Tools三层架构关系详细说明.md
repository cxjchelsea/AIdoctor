# AI医生系统 - 主Agent+Tools三层架构关系详细说明

> **文档目的**：详细说明业务流程层、工具功能层、技术架构层三个层次之间的关系，以及数据流转机制  
> **架构原则**：单主Agent + 多工具Tools + CDP + 审计

---

## 一、三层架构概览

```
┌─────────────────────────────────────────────────────────────┐
│              业务流程层（两个工作态）                          │
│  定义"按什么顺序执行"、"什么时候调用哪个工具"                  │
│  - 健康管理态（A路径：A1→A2→A3→A4→A5）                      │
│  - 临床诊疗态（5步流程：Step1→Step2→...→Step5）              │
│  执行者：主Agent（Clinical Agent Brain）                     │
└─────────────────────────────────────────────────────────────┘
                            ↓ 调用工具
┌─────────────────────────────────────────────────────────────┐
│              工具功能层（八大工具）                            │
│  定义"能完成什么功能"、"每个工具的职责"                        │
│  tool_0 │ tool_1 │ tool_2 │ tool_3 │ tool_4 │ tool_5 │ tool_6 │ tool_7 │
└─────────────────────────────────────────────────────────────┘
                            ↓ 实现
┌─────────────────────────────────────────────────────────────┐
│              技术架构层（双通道）                              │
│  定义"如何技术实现"、"使用什么技术手段"                        │
│  - 通道1：结构化推理（知识图谱、规则引擎、统计模型等）        │
│  - 通道2：语言与策略（NLG、对话生成、解释生成等）              │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、第一层关系：业务流程层 → 工具功能层（调用关系）

### 2.1 调用机制说明

**核心思想**：
- 业务流程层定义了"什么时候做什么"
- 工具功能层定义了"能做什么"
- 主Agent通过**调用工具**来完成各个步骤

**调用方式**：
- 业务流程的每个步骤明确标注了"参与工具"
- 一个步骤可能调用多个工具
- 一个工具可能被多个步骤调用
- 主Agent决定调用哪些工具、调用顺序、调用参数

> 说明：本项目的**流程编排与CDP管理**由 `diagnosis-service`（主Agent，Spring Boot/Java）承担；各工具能力由独立的 Python 微服务（FastAPI）提供。  
> 下方"调用代码示例"为**伪代码**，用于表达主Agent对各工具的调用关系与 CDP 的读写流转，并不代表具体语言实现。

### 2.2 临床诊疗态流程示例

#### Step 1：识别问题

**业务流程定义**：
```
Step 1: 识别问题
  目标：把用户的自然语言描述，转化成可推理、可复用、可审计的结构化"问题清单"
```

**调用工具**：
- **tool_1：病例理解工具** - 执行概念归一化和结构化提取
- **tool_2：主动问诊工具** - 识别信息缺口并生成追问

**主Agent调用代码示例**：
```python
def step1_identify_problem(self, cdp: CDP) -> CDP:
    """
    Step 1: 识别问题
    主Agent调用工具功能层
    """
    # 1. 主Agent调用tool_1（clinical-parsing-service）：概念归一化
    tool_context_1 = ToolContext(
        trace_id="trace_001",
        cdp=cdp,
        agent_state_summary=self.agent_state.get_summary(),
        constraints={"max_time_seconds": 10}
    )
    tool_result_1 = self.tool_1.execute(tool_context_1)
    
    # 主Agent处理tool_1的返回结果
    if tool_result_1.status == "success":
        # 主Agent根据suggestedWrites写入CDP
        for write in tool_result_1.suggested_writes:
            if write.field_path == "cdp.patient_state.symptoms":
                cdp.patient_state.symptoms = write.value
    
    # 2. 主Agent调用tool_2（dialog-service）：标记信息缺口
    tool_context_2 = ToolContext(
        trace_id="trace_002",
        cdp=cdp,  # 包含tool_1的更新结果
        agent_state_summary=self.agent_state.get_summary(),
        constraints={"max_time_seconds": 10}
    )
    tool_result_2 = self.tool_2.execute(tool_context_2)
    
    # 主Agent处理tool_2的返回结果
    if tool_result_2.status == "success":
        # 主Agent根据suggestedWrites写入CDP
        for write in tool_result_2.suggested_writes:
            if write.field_path == "cdp.uncertainty.missing_critical_info":
                cdp.uncertainty.missing_critical_info = write.value
    
    # 3. 主Agent记录AuditTrail
    self.audit_trail.record_tool_call(tool_result_1)
    self.audit_trail.record_tool_call(tool_result_2)
    self.audit_trail.record_cdp_update(cdp, reason="Step 1完成")
    
    return cdp
```

**数据流转**：
- 输入：`cdp.patient_state.user_input`（用户自然语言输入）
- tool_1处理：概念归一化 → 问题清单
- tool_1返回suggestedWrites：`cdp.patient_state.symptoms`
- 主Agent写入CDP：`cdp.patient_state.symptoms`
- tool_2处理：识别信息缺口
- tool_2返回suggestedWrites：`cdp.uncertainty.missing_critical_info`
- 主Agent写入CDP：`cdp.uncertainty.missing_critical_info`

#### Step 2：构建鉴别诊断候选集并分层

**业务流程定义**：
```
Step 2: 构建鉴别诊断候选集并分层
  目标：建立该主诉的鉴别诊断全集，并按临床风险与证据强度分成三层
```

**调用工具**：
- **tool_3：鉴别诊断工具** - 生成鉴别诊断全集并进行三层分层
- **tool_6：风险评估工具** - 识别必须排除的高危诊断（可选）

**主Agent调用代码示例**：
```python
def step2_build_ddx_candidates(self, cdp: CDP) -> CDP:
    """
    Step 2: 构建鉴别诊断候选集并分层
    主Agent调用工具功能层
    """
    # 1. 主Agent调用tool_3（diagnosis-engine-service）：生成鉴别诊断全集
    tool_context_3 = ToolContext(
        trace_id="trace_003",
        cdp=cdp,
        agent_state_summary=self.agent_state.get_summary(),
        constraints={"max_time_seconds": 30}
    )
    tool_result_3 = self.tool_3.execute(tool_context_3)
    
    # 主Agent处理tool_3的返回结果
    if tool_result_3.status == "success":
        # 主Agent根据suggestedWrites写入CDP
        for write in tool_result_3.suggested_writes:
            if write.field_path == "cdp.ddx":
                cdp.ddx = write.value
    
    # 2. 主Agent调用tool_6（risk-assessment-service）：风险评估（可选）
    tool_context_6 = ToolContext(
        trace_id="trace_004",
        cdp=cdp,  # 包含tool_3的更新结果
        agent_state_summary=self.agent_state.get_summary(),
        constraints={"max_time_seconds": 10}
    )
    tool_result_6 = self.tool_6.execute(tool_context_6)
    
    # 主Agent进行evidence fusion和conflict resolution
    if tool_result_6.status == "success":
        # 主Agent融合tool_3和tool_6的结果
        fused_result = self.evidence_fusion.merge(
            tool_result_3.evidence,
            tool_result_6.evidence
        )
        
        # 主Agent根据融合结果更新CDP
        cdp.ddx = self._update_ddx_with_risk_assessment(
            cdp.ddx,
            tool_result_6.payload
        )
    
    # 3. 主Agent记录AuditTrail
    self.audit_trail.record_tool_call(tool_result_3)
    self.audit_trail.record_tool_call(tool_result_6)
    self.audit_trail.record_cdp_update(cdp, reason="Step 2完成")
    
    return cdp
```

**数据流转**：
- 输入：`cdp.patient_state.problem_list`（Step 1的输出）
- tool_3处理：生成鉴别诊断候选集
- tool_3返回suggestedWrites：`cdp.ddx`
- 主Agent写入CDP：`cdp.ddx`
- tool_6处理：识别高危诊断
- tool_6返回suggestedWrites：`cdp.triage`
- 主Agent进行evidence fusion和conflict resolution
- 主Agent更新CDP：`cdp.ddx`（融合后的三层排序）

#### Step 4：采集关键证据并形成排序与验证计划

**业务流程定义**：
```
Step 4: 采集关键证据并形成排序与验证计划
  目标：系统采集能够"推动排序变化"的关键证据，形成稳定的三层清单，并制定验证计划
```

**调用工具**：
- **tool_2：主动问诊工具** - 采集关键证据
- **tool_3：鉴别诊断工具** - 固化三层排序
- **tool_4：检查建议工具** - 制定验证计划

**主Agent调用代码示例**：
```python
def step4_collect_evidence_and_plan(self, cdp: CDP) -> CDP:
    """
    Step 4: 采集关键证据并形成排序与验证计划
    主Agent调用工具功能层（一个步骤调用多个工具）
    """
    # 1. 主Agent调用tool_2（dialog-service）：采集关键证据
    tool_context_2 = ToolContext(
        trace_id="trace_005",
        cdp=cdp,
        agent_state_summary=self.agent_state.get_summary(),
        constraints={"max_time_seconds": 10}
    )
    tool_result_2 = self.tool_2.execute(tool_context_2)
    
    # 主Agent处理tool_2的返回结果
    if tool_result_2.status == "success":
        # 主Agent根据suggestedWrites写入CDP
        for write in tool_result_2.suggested_writes:
            if write.field_path == "cdp.patient_state":
                cdp.patient_state.update(write.value)
    
    # 2. 主Agent调用tool_3（diagnosis-engine-service）：固化三层排序
    tool_context_3 = ToolContext(
        trace_id="trace_006",
        cdp=cdp,  # 包含tool_2的更新结果
        agent_state_summary=self.agent_state.get_summary(),
        constraints={"max_time_seconds": 30}
    )
    tool_result_3 = self.tool_3.execute(tool_context_3)
    
    # 主Agent处理tool_3的返回结果
    if tool_result_3.status == "success":
        # 主Agent根据suggestedWrites写入CDP
        for write in tool_result_3.suggested_writes:
            if write.field_path == "cdp.ddx":
                cdp.ddx = write.value
    
    # 3. 主Agent调用tool_4（workup-planner-service）：制定验证计划
    tool_context_4 = ToolContext(
        trace_id="trace_007",
        cdp=cdp,  # 包含tool_3的更新结果
        agent_state_summary=self.agent_state.get_summary(),
        constraints={"max_time_seconds": 10}
    )
    tool_result_4 = self.tool_4.execute(tool_context_4)
    
    # 主Agent处理tool_4的返回结果
    if tool_result_4.status == "success":
        # 主Agent根据suggestedWrites写入CDP
        for write in tool_result_4.suggested_writes:
            if write.field_path == "cdp.workup_plan":
                cdp.workup_plan = write.value
    
    # 4. 主Agent记录AuditTrail
    self.audit_trail.record_tool_call(tool_result_2)
    self.audit_trail.record_tool_call(tool_result_3)
    self.audit_trail.record_tool_call(tool_result_4)
    self.audit_trail.record_cdp_update(cdp, reason="Step 4完成")
    
    return cdp
```

**数据流转**：
- 输入：`cdp.routing_path`（Step 3的输出）和`cdp.ddx`（Step 2的输出）
- tool_2处理：采集关键证据
- tool_2返回suggestedWrites：`cdp.patient_state`（更新）
- 主Agent写入CDP：`cdp.patient_state`（更新）
- tool_3处理：根据证据更新三层排序
- tool_3返回suggestedWrites：`cdp.ddx`（更新）
- 主Agent写入CDP：`cdp.ddx`（更新）
- tool_4处理：制定验证计划
- tool_4返回suggestedWrites：`cdp.workup_plan`
- 主Agent写入CDP：`cdp.workup_plan`

### 2.3 健康管理态流程示例

#### A1：需求分类

**业务流程定义**：
```
A1: 需求分类
  目标：把用户需求归入三类之一（筛查建议/健康目标管理/计划性健康需求）
```

**调用工具**：
- **tool_0：健康状态判定工具** - 理解用户需求，结构化需求信息

**主Agent调用代码示例**：
```python
def a1_demand_classification(self, cdp: CDP) -> Dict:
    """
    A1: 需求分类
    主Agent调用工具功能层
    """
    # 主Agent调用tool_0（health-state-assessment-service）：理解用户需求
    tool_context_0 = ToolContext(
        trace_id="trace_008",
        cdp=cdp,
        agent_state_summary=self.agent_state.get_summary(),
        constraints={"max_time_seconds": 5}
    )
    tool_result_0 = self.tool_0.execute(tool_context_0)
    
    # 主Agent处理tool_0的返回结果
    if tool_result_0.status == "success":
        demand_type = tool_result_0.payload.get("demand_type")
        
        # 主Agent根据suggestedWrites写入CDP
        for write in tool_result_0.suggested_writes:
            if write.field_path == "cdp.health_state_assessment":
                cdp.health_state_assessment = write.value
    
    # 主Agent记录AuditTrail
    self.audit_trail.record_tool_call(tool_result_0)
    
    return {
        "type": demand_type["type"],
        "type_name": demand_type["name"],
        "confidence": demand_type["confidence"]
    }
```

### 2.4 调用关系总结

**特点**：
1. **多对多关系**：
   - 一个流程步骤可能调用多个工具
   - 一个工具可能被多个流程步骤调用

2. **明确标注**：
   - 每个流程步骤都明确标注了"参与工具"
   - 调用顺序和调用时机由主Agent决定

3. **数据流转**：
   - 所有调用都围绕CDP进行
   - 工具从CDP读取输入，返回suggestedWrites
   - 主Agent根据suggestedWrites写入CDP

4. **主Agent决策权**：
   - 主Agent决定调用哪些工具
   - 主Agent决定是否接受工具的suggestedWrites
   - 主Agent进行evidence fusion和conflict resolution

---

## 三、第二层关系：工具功能层 → 技术架构层（实现关系）

### 3.1 实现机制说明

**核心思想**：
- 工具功能层定义了"要完成什么功能"
- 技术架构层定义了"如何技术实现"
- 每个工具的技术实现都基于双通道架构

**实现方式**：
- 通道1（结构化推理）：负责"决定该往哪想"的逻辑推理
- 通道2（语言与策略）：负责"怎么说、怎么问"的自然语言生成

### 3.2 tool_1：病例理解工具

**工具功能层定义**：
- **职责**：将非结构化的患者信息转换为结构化的临床要素
- **输入**：病历自由文本、对话内容、检查单等
- **输出**：结构化临床要素（症状、体征、既往史等）

**技术架构层实现**：

#### 通道1：结构化推理（决定"该往哪想"）

```python
class ClinicalParsingTool:
    """
    tool_1的技术实现
    基于双通道架构
    """
    
    def execute(self, tool_context: ToolContext) -> ToolResult:
        """
        工具执行：概念归一化和结构化提取
        """
        # 1. 从CDP读取输入
        cdp = tool_context.cdp
        user_input = cdp.patient_state.user_input
        
        # 2. 使用医学概念识别（通道1技术）
        concepts = self.ner_model.extract(user_input)  # QuickUMLS / 中文医学NER模型
        
        # 3. 归一化到标准术语（通道1技术）
        normalized_concepts = []
        for concept in concepts:
            cui = self.normalizer.normalize(concept)  # CUI/ICD/SNOMED编码
            if cui:
                normalized_concepts.append(Concept(cui=cui, ...))
        
        # 4. 形成完整问题清单（通道1技术）
        problem_list = {
            "主要问题": self._extract_main_problem(normalized_concepts),
            "伴随问题": self._extract_accompanying_problems(normalized_concepts),
            "关键背景": self._extract_key_background(cdp.patient_state)
        }
        
        # 5. 返回结构化结果
        return ToolResult(
            tool_id="tool_1",
            tool_name="病例理解工具",
            status="success",
            payload={
                "normalized_concepts": normalized_concepts,
                "problem_list": problem_list
            },
            evidence=[
                {
                    "source": "knowledge_base",
                    "reference": "normalization_vocabulary",
                    "strength": "strong"
                }
            ],
            suggested_writes=[
                {
                    "field_path": "cdp.patient_state.symptoms",
                    "value": problem_list["主要问题"],
                    "reason": "概念归一化结果"
                }
            ],
            quality={
                "confidence": 0.9,
                "completeness": 0.85
            }
        )
```

#### 通道2：语言与策略（决定"怎么说"）

```python
    def generate_clarification_question(self, missing_info: str) -> str:
        """
        生成澄清问题（通道2：语言与策略）
        """
        # 使用LLM生成自然语言问题（通道2技术）
        prompt = f"""
        根据缺失信息：{missing_info}
        生成一个像医生一样的澄清问题，要求：
        1. 自然、友好
        2. 专业但易懂
        3. 避免过于技术化
        """
        
        question = self.llm.generate(prompt)  # 使用LLM（通道2技术）
        return question
```

### 3.3 tool_3：鉴别诊断工具

**工具功能层定义**：
- **职责**：生成Top-K鉴别诊断列表，每个诊断包含支持证据、反证、缺失证据
- **输入**：当前CDP的`patient_state`、知识图谱推理路径
- **输出**：鉴别诊断列表（三层排序）

**技术架构层实现**：

#### 通道1：结构化推理（决定"该往哪想"）

```python
class DifferentialDiagnosisTool:
    """
    tool_3的技术实现
    基于双通道架构
    """
    
    def execute(self, tool_context: ToolContext) -> ToolResult:
        """
        工具执行：多引擎融合诊断
        """
        # 1. 从CDP读取输入
        cdp = tool_context.cdp
        problem_list = cdp.patient_state.problem_list
        
        # 2. 知识图谱引擎（通道1技术：DR.KNOWS方法）
        kg_paths = self.kg_engine.retrieve_paths(
            symptoms=problem_list["主要问题"]["主诉名称"],
            max_hops=4
        )
        
        # 3. 规则引擎（通道1技术：症状组合规则）
        rule_matches = self.rule_engine.match(problem_list)
        
        # 4. 统计模型引擎（通道1技术：概率预测）
        statistical_predictions = self.statistical_engine.predict(problem_list)
        
        # 5. LLM引擎（通道1技术：路径约束推理）
        llm_result = self.llm_engine.diagnose(
            problem_list,
            kg_paths  # 路径注入
        )
        
        # 6. 多引擎融合（通道1技术：加权融合）
        fused_result = self.fuse_engine_results(
            kg_paths, rule_matches, statistical_predictions, llm_result
        )
        
        # 7. 三层分层（通道1技术：基于证据强度排序）
        three_layer_ddx = self.three_layer_classification(fused_result)
        
        # 8. 返回结构化结果
        return ToolResult(
            tool_id="tool_3",
            tool_name="鉴别诊断工具",
            status="success",
            payload={
                "ddx_candidates": three_layer_ddx,
                "reasoning_paths": kg_paths
            },
            evidence=[
                {
                    "source": "knowledge_graph",
                    "reference": "kg_paths",
                    "strength": "strong"
                },
                {
                    "source": "rule_engine",
                    "reference": "symptom_combination_rules",
                    "strength": "medium"
                }
            ],
            suggested_writes=[
                {
                    "field_path": "cdp.ddx",
                    "value": three_layer_ddx,
                    "reason": "多引擎融合诊断结果"
                },
                {
                    "field_path": "cdp.ddx.reasoning_paths",
                    "value": kg_paths,
                    "reason": "知识图谱推理路径"
                }
            ],
            quality={
                "confidence": 0.85,
                "completeness": 0.90
            }
        )
```

#### 通道2：语言与策略（决定"怎么说"）

```python
    def generate_ddx_explanation(self, ddx: Dict) -> str:
        """
        生成诊断解释（通道2：语言与策略）
        """
        # 使用LLM生成自然语言解释（通道2技术）
        prompt = f"""
        根据以下鉴别诊断结果，生成一个像医生一样的解释：
        {ddx}
        
        要求：
        1. 用通俗易懂的语言
        2. 解释为什么考虑这些诊断
        3. 说明还需要什么信息来进一步判断
        """
        
        explanation = self.llm.generate(prompt)  # 使用LLM（通道2技术）
        return explanation
```

### 3.4 tool_2：主动问诊工具

**工具功能层定义**：
- **职责**：像医生一样问"关键问题"，补齐鉴别诊断所需证据
- **输入**：当前CDP（特别是`ddx`和`uncertainty.missing_critical_info`）
- **输出**：问诊计划（下一问是什么、为什么问、如何问）

**技术架构层实现**：

#### 通道1：结构化推理（决定"该往哪想"）

```python
class DialogTool:
    """
    tool_2的技术实现
    基于双通道架构
    """
    
    def execute(self, tool_context: ToolContext) -> ToolResult:
        """
        工具执行：信息缺口识别和追问生成
        """
        # 1. 从CDP读取输入
        cdp = tool_context.cdp
        missing_info = cdp.uncertainty.missing_critical_info
        
        # 2. 识别信息缺口（通道1技术：临床决策分析）
        gaps = []
        for info in missing_info:
            # 计算信息增益（通道1技术：临床决策理论）
            information_gain = self._calculate_information_gain(
                info, cdp.ddx
            )
            
            # 计算风险增益（通道1技术：风险优先原则）
            risk_gain = self._calculate_risk_gain(
                info, cdp.ddx
            )
            
            # 综合评分（通道1技术：临床决策增益）
            clinical_gain = (
                0.3 * information_gain +
                0.5 * risk_gain +
                0.2 * (information_gain / cost)
            )
            
            gaps.append({
                "info": info,
                "importance": clinical_gain
            })
        
        # 排序，返回最重要的缺口
        gaps.sort(key=lambda x: x["importance"], reverse=True)
        
        # 3. 返回结构化结果
        return ToolResult(
            tool_id="tool_2",
            tool_name="主动问诊工具",
            status="success",
            payload={
                "information_gaps": gaps,
                "question_plan": self._generate_question_plan(gaps)
            },
            evidence=[
                {
                    "source": "clinical_decision_analysis",
                    "reference": "information_gain_calculation",
                    "strength": "strong"
                }
            ],
            suggested_writes=[
                {
                    "field_path": "cdp.uncertainty.missing_critical_info",
                    "value": gaps,
                    "reason": "信息缺口识别结果"
                }
            ],
            quality={
                "confidence": 0.8,
                "completeness": 0.75
            }
        )
```

#### 通道2：语言与策略（决定"怎么说"）

```python
    def generate_question(self, gap: Dict, cdp: CDP) -> str:
        """
        生成追问问题（通道2：语言与策略）
        """
        # 使用LLM生成自然语言问题（通道2技术）
        prompt = f"""
        根据以下信息缺口，生成一个像医生一样的追问问题：
        缺失信息：{gap["info"]}
        患者当前状态：{cdp.patient_state}
        当前诊断方向：{cdp.ddx}
        
        要求：
        1. 自然、友好，像医生在问诊
        2. 专业但易懂
        3. 避免重复提问
        4. 说明为什么问这个问题（可选）
        """
        
        question = self.llm.generate(prompt)  # 使用LLM（通道2技术）
        return question
```

### 3.5 实现关系总结

**特点**：
1. **双通道分工明确**：
   - 通道1负责"逻辑推理"（该往哪想）
   - 通道2负责"自然语言"（怎么说、怎么问）

2. **技术手段对应**：
   - 通道1：知识图谱、规则引擎、统计模型、贝叶斯推理等
   - 通道2：LLM、NLG、对话生成、解释生成等

3. **所有工具都基于双通道**：
   - 每个工具都需要"逻辑推理"和"自然语言生成"
   - 只是不同工具在双通道上的侧重点不同

---

## 四、数据流转：CDP贯穿三个层次

### 4.1 CDP的作用

**核心思想**：
- CDP（Clinical Decision Package）是系统内部一切推理的核心数据结构
- 所有层次都围绕CDP进行数据流转
- CDP贯穿业务流程层、工具功能层、技术架构层

### 4.2 数据流转路径

```
用户输入
    ↓
业务流程层（Step 1）
    ↓ 主Agent调用工具
工具功能层（tool_1）
    ↓ 实现
技术架构层（通道1：概念归一化）
    ↓ 返回suggestedWrites
主Agent写入CDP
    ↓
CDP.patient_state.symptoms
    ↓
业务流程层（Step 2）
    ↓ 主Agent调用工具
工具功能层（tool_3）
    ↓ 实现
技术架构层（通道1：知识图谱推理）
    ↓ 返回suggestedWrites
主Agent写入CDP
    ↓
CDP.ddx
    ↓
业务流程层（Step 3）
    ↓ 主Agent调用工具
工具功能层（tool_2）
    ↓ 实现
技术架构层（通道2：生成问诊问题）
    ↓ 返回suggestedWrites
主Agent写入CDP
    ↓
输出给用户：自然语言问诊问题
```

### 4.3 具体数据流转示例

#### 示例：从用户输入到诊断结果

**Step 1：识别问题**

```python
# 1. 业务流程层接收用户输入
user_input = "我这两天胸口闷，走几步就喘"

# 2. 主Agent调用tool_1
tool_context = ToolContext(cdp=cdp, ...)
tool_result = tool_1.execute(tool_context)

# 3. tool_1通过通道1实现概念归一化
normalized_concepts = tool_1.normalize_concepts(user_input)
# 输出：["胸闷", "活动后气促"] → CUI编码

# 4. tool_1通过通道1实现结构化提取
problem_list = tool_1.build_problem_list(normalized_concepts)
# 输出：{
#   "主要问题": {"主诉名称": "胸闷样不适", ...},
#   "伴随问题": [{"症状名称": "活动后气促", ...}]
# }

# 5. tool_1返回suggestedWrites
tool_result.suggested_writes = [
    {
        "field_path": "cdp.patient_state.symptoms",
        "value": problem_list["主要问题"]
    }
]

# 6. 主Agent写入CDP
cdp.patient_state.symptoms = tool_result.suggested_writes[0].value
```

**Step 2：构建鉴别诊断候选集**

```python
# 1. 主Agent调用tool_3
tool_context = ToolContext(cdp=cdp, ...)
tool_result = tool_3.execute(tool_context)

# 2. tool_3通过通道1实现知识图谱推理
kg_paths = tool_3.kg_engine.retrieve_paths(
    symptoms=["胸闷", "活动后气促"],
    max_hops=4
)
# 输出：推理路径 → ["胸闷" --[相关]--> "心绞痛", "胸闷" --[相关]--> "急性心梗", ...]

# 3. tool_3通过通道1实现多引擎融合
ddx_candidates = tool_3.fusion_engine.fuse(kg_paths, rule_matches, ...)
# 输出：鉴别诊断候选集

# 4. tool_3通过通道1实现三层分层
three_layer_ddx = tool_3.three_layer_classification(ddx_candidates)
# 输出：{
#   "首要假设": [{"疾病名称": "心绞痛", "概率": 0.4}],
#   "主要备选诊断": [{"疾病名称": "急性心梗", "概率": 0.3}],
#   "必须排除的高危诊断": [{"疾病名称": "急性心梗", "必须排除": true}]
# }

# 5. tool_3返回suggestedWrites
tool_result.suggested_writes = [
    {
        "field_path": "cdp.ddx",
        "value": three_layer_ddx
    }
]

# 6. 主Agent写入CDP
cdp.ddx = tool_result.suggested_writes[0].value
```

**Step 3：组织候选集并建立分流路径**

```python
# 1. 主Agent调用tool_2
tool_context = ToolContext(cdp=cdp, ...)
tool_result = tool_2.execute(tool_context)

# 2. tool_2通过通道1实现信息缺口识别
gaps = tool_2.identify_gaps(cdp)
# 输出：["是否静息也发作", "是否持续不缓解", ...]

# 3. tool_2通过通道2实现生成问诊问题
question = tool_2.generate_question(gaps[0], cdp)
# 输出："症状是否在静息时也出现，且持续不缓解？"

# 4. tool_2返回suggestedWrites
tool_result.suggested_writes = [
    {
        "field_path": "cdp.routing_path.first_layer_questions",
        "value": [question]
    }
]

# 5. 主Agent写入CDP
cdp.routing_path = {
    "first_layer_questions": [question],
    "high_risk_path": {...},
    "normal_path": {...}
}
```

### 4.4 CDP字段与三个层次的对应关系

| CDP字段 | 业务流程层 | 工具功能层 | 技术架构层 |
|---------|-----------|-----------|-----------|
| `health_state_assessment` | 阶段0：健康状态判定 | tool_0 | 通道1：风险评估逻辑 |
| `patient_state.problem_list` | Step 1：识别问题 | tool_1 | 通道1：概念归一化 |
| `ddx` | Step 2：构建候选集 | tool_3 | 通道1：知识图谱推理 |
| `routing_path` | Step 3：建立分流路径 | tool_2 + tool_3 | 通道1：信息增益计算 + 通道2：问题生成 |
| `evidence_graph` | Step 4：采集证据 | tool_2 | 通道1：证据分析 |
| `workup_plan` | Step 4：验证计划 | tool_4 | 通道1：检查价值评估 |
| `management_plan` | Step 5：处置建议 | tool_5 | 通道1：治疗方案推理 + 通道2：方案生成 |
| `evidence_graph`（解释） | Step 5：证据链 | tool_7 | 通道2：解释生成 |

---

## 五、完整调用链示例

### 5.1 临床诊疗态完整调用链

```
用户输入："我这两天胸口闷，走几步就喘，昨天还出汗，有点恶心。"
    ↓
┌─────────────────────────────────────────────────────────────┐
│ 业务流程层：Step 1 - 识别问题                                │
│ 执行者：主Agent（diagnosis-service）                         │
└─────────────────────────────────────────────────────────────┘
    ↓ 主Agent调用工具
┌─────────────────────────────────────────────────────────────┐
│ 工具功能层：tool_1 - 病例理解工具                            │
│ 工具功能层：tool_2 - 主动问诊工具                            │
└─────────────────────────────────────────────────────────────┘
    ↓ 实现
┌─────────────────────────────────────────────────────────────┐
│ 技术架构层：通道1 - 概念归一化（QuickUMLS/NER模型）          │
│ 技术架构层：通道1 - 信息缺口识别（临床决策分析）              │
│ 技术架构层：通道2 - 生成追问问题（LLM）                      │
└─────────────────────────────────────────────────────────────┘
    ↓ 返回suggestedWrites
主Agent写入CDP
    ↓
CDP.patient_state.problem_list = {
  "主要问题": {"主诉名称": "胸闷样不适", ...},
  "伴随问题": [...],
  "信息缺口": ["是否静息也发作", ...]
}
    ↓
┌─────────────────────────────────────────────────────────────┐
│ 业务流程层：Step 2 - 构建鉴别诊断候选集并分层                  │
│ 执行者：主Agent（diagnosis-service）                         │
└─────────────────────────────────────────────────────────────┘
    ↓ 主Agent调用工具
┌─────────────────────────────────────────────────────────────┐
│ 工具功能层：tool_3 - 鉴别诊断工具                             │
│ 工具功能层：tool_6 - 风险评估工具（可选）                    │
└─────────────────────────────────────────────────────────────┘
    ↓ 实现
┌─────────────────────────────────────────────────────────────┐
│ 技术架构层：通道1 - 知识图谱路径检索（DR.KNOWS方法）          │
│ 技术架构层：通道1 - 多引擎融合（规则+KG+统计+LLM）           │
│ 技术架构层：通道1 - 三层分层（基于证据强度）                  │
└─────────────────────────────────────────────────────────────┘
    ↓ 返回suggestedWrites
主Agent进行evidence fusion和conflict resolution
主Agent写入CDP
    ↓
CDP.ddx = {
  "首要假设": [{"疾病名称": "心血管相关方向", "概率": 0.4}],
  "主要备选诊断": [...],
  "必须排除的高危诊断": [{"疾病名称": "高风险心血管急症", ...}]
}
    ↓
┌─────────────────────────────────────────────────────────────┐
│ 业务流程层：Step 3 - 组织候选集并建立分流路径                  │
│ 执行者：主Agent（diagnosis-service）                         │
└─────────────────────────────────────────────────────────────┘
    ↓ 主Agent调用工具
┌─────────────────────────────────────────────────────────────┐
│ 工具功能层：tool_3 - 组织推理子组                             │
│ 工具功能层：tool_2 - 生成分流问题                             │
└─────────────────────────────────────────────────────────────┘
    ↓ 实现
┌─────────────────────────────────────────────────────────────┐
│ 技术架构层：通道1 - 推理子组组织（诊断树理论）                │
│ 技术架构层：通道1 - 关键差异点提炼                            │
│ 技术架构层：通道2 - 生成分流问题（LLM）                      │
└─────────────────────────────────────────────────────────────┘
    ↓ 返回suggestedWrites
主Agent写入CDP
    ↓
CDP.routing_path = {
  "first_layer_questions": [
    "症状是否在静息时也出现，且持续不缓解？"
  ],
  "high_risk_path": {...},
  "normal_path": {...}
}
    ↓
┌─────────────────────────────────────────────────────────────┐
│ 业务流程层：Step 4 - 采集关键证据并形成排序与验证计划          │
│ 执行者：主Agent（diagnosis-service）                         │
└─────────────────────────────────────────────────────────────┘
    ↓ 主Agent调用工具
┌─────────────────────────────────────────────────────────────┐
│ 工具功能层：tool_2 - 采集关键证据                              │
│ 工具功能层：tool_3 - 固化三层排序                              │
│ 工具功能层：tool_4 - 制定验证计划                              │
└─────────────────────────────────────────────────────────────┘
    ↓ 实现
┌─────────────────────────────────────────────────────────────┐
│ 技术架构层：通道1 - 证据分析（支持/反对/不确定）              │
│ 技术架构层：通道1 - 三层排序更新（基于证据重排）              │
│ 技术架构层：通道1 - 验证计划生成（检查价值评估）              │
└─────────────────────────────────────────────────────────────┘
    ↓ 返回suggestedWrites
主Agent进行evidence fusion和conflict resolution
主Agent写入CDP
    ↓
CDP.evidence_graph = [...]
CDP.ddx = {...}  # 更新后的三层排序
CDP.workup_plan = [...]
    ↓
┌─────────────────────────────────────────────────────────────┐
│ 业务流程层：Step 5 - 回填证据并输出终点结论包                  │
│ 执行者：主Agent（diagnosis-service）                         │
└─────────────────────────────────────────────────────────────┘
    ↓ 主Agent调用工具
┌─────────────────────────────────────────────────────────────┐
│ 工具功能层：tool_3 - 回填证据                                  │
│ 工具功能层：tool_3 - 更新三层排序                              │
│ 工具功能层：tool_5 - 生成处置建议                              │
│ 工具功能层：tool_7 - 生成终点结论包                            │
└─────────────────────────────────────────────────────────────┘
    ↓ 实现
┌─────────────────────────────────────────────────────────────┐
│ 技术架构层：通道1 - 证据回填逻辑                              │
│ 技术架构层：通道1 - 诊断排序更新                              │
│ 技术架构层：通道1 - 治疗方案推理                              │
│ 技术架构层：通道2 - 生成终点结论包（LLM）                    │
│ 技术架构层：通道2 - 生成自然语言解释                          │
└─────────────────────────────────────────────────────────────┘
    ↓ 返回suggestedWrites
主Agent进行evidence fusion和conflict resolution
主Agent写入CDP
主Agent生成终点结论包
    ↓
CDP.conclusion_package = {
  "结论": {...},
  "必须排除项状态": {...},
  "关键依据": {...},
  "行动与随访": {...}
}
    ↓
输出给用户：自然语言诊断结果 + 证据链 + 行动建议
```

---

## 六、主Agent运行循环

### 6.1 主Agent运行循环（Observe→Plan→Act→Update→Evaluate→Stop/Escalate）

```
┌─────────────────────────────────────────────────────────────┐
│  Observe（观察）：读取CDP状态和AgentState                    │
│  - 读取CDP当前状态                                            │
│  - 读取AgentState（阈值、预算、已尝试工具等）                 │
│  - 评估当前信息完整度                                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  Plan（计划）：决定调用哪些工具                               │
│  - 根据CDP状态和AgentState，决定调用哪些工具                  │
│  - 决定调用顺序（顺序/并行）                                  │
│  - 决定调用参数（约束：成本/时间/风险）                       │
│  - 应用默认诊断路径（Step1-5）或动态插入策略                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  Act（执行）：调用工具                                        │
│  - 创建ToolContext（traceId、cdp引用、agentState摘要、约束）  │
│  - 调用工具（同步/异步/并行）                                 │
│  - 等待工具返回ToolResult                                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  Update（更新）：写入CDP和记录AuditTrail                      │
│  - 进行evidence fusion和conflict resolution                  │
│  - 根据suggestedWrites写入CDP                                 │
│  - 记录AuditTrail（工具调用、CDP更新）                       │
│  - 更新AgentState（已尝试工具、预算消耗等）                   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  Evaluate（评估）：检查停止条件                               │
│  - 检查停止条件（CDP必填项、证据引用齐全、风险评估完成）      │
│  - 检查升级条件（急危重/超能力/证据不足且风险高）             │
│  - 检查拒答条件（无法安全推断）                               │
└─────────────────────────────────────────────────────────────┘
                            ↓
        ┌───────────────────┴───────────────────┐
        │                                       │
        ▼                                       ▼
┌───────────────┐                      ┌───────────────┐
│  Stop（停止）  │                      │ Escalate（升级）│
│  - 满足停止条件│                      │ - 满足升级条件 │
│  - 生成终点结论包│                     │ - 升级处理     │
│  - 输出结果     │                      │ - 或拒答       │
└───────────────┘                      └───────────────┘
```

### 6.2 默认诊断路径（Step1-5）

**主Agent的默认策略路径**：

```
Step 1: 识别问题
  - 主Agent调用tool_1（病例理解工具）
  - 主Agent调用tool_2（主动问诊工具）

Step 2: 构建鉴别诊断候选集并分层
  - 主Agent调用tool_3（鉴别诊断工具）
  - 主Agent调用tool_6（风险评估工具，可选）

Step 3: 组织候选集并建立分流路径
  - 主Agent调用tool_3（推理组织器）
  - 主Agent调用tool_2（分流路径设计）

Step 4: 采集关键证据并形成排序与验证计划
  - 主Agent调用tool_2（主动问诊）
  - 主Agent调用tool_3（证据分析器）
  - 主Agent调用tool_4（检查建议）
  - 主Agent调用tool_6（风险评估）

Step 5: 回填证据并输出终点结论包
  - 主Agent调用tool_3（证据回填）
  - 主Agent调用tool_7（解释生成）
  - 主Agent调用tool_5（治疗推理）
  - 主Agent生成终点结论包
```

### 6.3 动态插入策略

**主Agent可以动态插入以下策略**：

1. **红旗优先**：
   - 如果检测到危险信号，主Agent优先调用tool_6（风险评估工具）
   - 主Agent可能跳过某些步骤，直接进入紧急处理

2. **冲突复核**：
   - 如果检测到证据冲突，主Agent重新调用相关工具
   - 主Agent进行conflict resolution

3. **证据不足触发检索**：
   - 如果信息不足，主Agent继续调用tool_2（主动问诊工具）
   - 主Agent可能循环调用tool_2，直到信息充足

---

## 七、总结

### 7.1 三层架构关系总结

1. **业务流程层 → 工具功能层**：
   - 主Agent根据业务流程调用工具
   - 一个流程步骤可能调用多个工具
   - 一个工具可能被多个流程步骤调用

2. **工具功能层 → 技术架构层**：
   - 每个工具基于双通道架构实现
   - 通道1负责"逻辑推理"（该往哪想）
   - 通道2负责"自然语言"（怎么说、怎么问）

3. **CDP贯穿三个层次**：
   - 所有层次都围绕CDP进行数据流转
   - 工具从CDP读取输入，返回suggestedWrites
   - 主Agent根据suggestedWrites写入CDP

### 7.2 架构特点

- **单主Agent + 多工具**：主Agent是唯一决策者，工具无状态、无独立目标
- **CDP唯一事实源**：所有工具从CDP读取，主Agent写入CDP
- **AuditTrail可追溯**：所有操作记录到AuditTrail
- **双通道协作**：通道1生成结构化结果，通道2生成自然语言表达

---

**文档版本**：v2.0  
**最后更新**：2026-02-04  
**维护者**：AI医生系统开发团队

