# AI医生系统-三层架构关系详细说明

> **文档目的**：详细说明业务流程层、功能模块层、技术架构层三个层次之间的关系，以及数据流转机制

---

## 一、三层架构概览

```
┌─────────────────────────────────────────────────────────────┐
│              业务流程层（两个工作态）                          │
│  定义"按什么顺序执行"、"什么时候调用哪个脑区"                  │
│  - 健康管理态（A路径：A1→A2→A3→A4→A5）                      │
│  - 临床诊疗态（5步流程：Step1→Step2→...→Step5）              │
└─────────────────────────────────────────────────────────────┘
                            ↓ 调用
┌─────────────────────────────────────────────────────────────┐
│              功能模块层（八大脑区）                            │
│  定义"能完成什么功能"、"每个脑区的职责"                        │
│  脑区0 │ 脑区A │ 脑区B │ 脑区C │ 脑区D │ 脑区E │ 脑区F │ 脑区G │
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

## 二、第一层关系：业务流程层 → 功能模块层（调用关系）

### 2.1 调用机制说明

**核心思想**：
- 业务流程层定义了"什么时候做什么"
- 功能模块层定义了"能做什么"
- 业务流程通过**调用**功能模块来完成各个步骤

**调用方式**：
- 业务流程的每个步骤明确标注了"参与脑区"
- 一个步骤可能调用多个脑区
- 一个脑区可能被多个步骤调用

> 说明：本项目的**流程编排与CDP管理**由 `diagnosis-service`（Spring Boot/Java）承担；各脑区能力由独立的 Python 微服务（FastAPI）提供。
> 下方“调用代码示例”为**伪代码**，用于表达编排层对各服务的调用关系与 CDP 的读写流转，并不代表具体语言实现。

### 2.2 临床诊疗态流程示例

#### Step 1：识别问题

**业务流程定义**：
```
Step 1: 识别问题
  目标：把用户的自然语言描述，转化成可推理、可复用、可审计的结构化"问题清单"
```

**调用脑区**：
- **脑区A：病例理解与结构化** - 执行概念归一化和结构化提取
- **脑区B：主动问诊与信息补全** - 识别信息缺口并生成追问

**调用代码示例**：
```python
def step1_identify_problem(self, cdp: CDP) -> CDP:
    """
    Step 1: 识别问题
    业务流程层调用功能模块层
    """
    # 1. 调用脑区A（clinical-parsing-service）：概念归一化
    normalized_concepts = self.clinical_parsing_service.normalize_concepts(
        cdp.patient_state.user_input
    )
    
    # 2. 调用脑区A（clinical-parsing-service）：形成完整问题清单
    problem_list = self.clinical_parsing_service.build_problem_list(
        normalized_concepts,
        cdp.patient_state
    )
    
    # 3. 调用脑区B（dialog-service）：标记信息缺口
    information_gaps = self.dialog_service.identify_gaps(
        problem_list, cdp
    )
    
    # 更新CDP（数据流转）
    cdp.patient_state.problem_list = problem_list
    cdp.uncertainty.missing_critical_info = information_gaps
    
    return cdp
```

**数据流转**：
- 输入：`cdp.patient_state.user_input`（用户自然语言输入）
- 脑区A处理：概念归一化 → 问题清单
- 脑区B处理：识别信息缺口
- 输出：更新`cdp.patient_state.problem_list`和`cdp.uncertainty.missing_critical_info`

#### Step 2：构建鉴别诊断候选集并分层

**业务流程定义**：
```
Step 2: 构建鉴别诊断候选集并分层
  目标：建立该主诉的鉴别诊断全集，并按临床风险与证据强度分成三层
```

**调用脑区**：
- **脑区C：鉴别诊断引擎** - 生成鉴别诊断全集并进行三层分层
- **脑区F：风险与急症识别** - 识别必须排除的高危诊断

**调用代码示例**：
```python
def step2_build_ddx_candidates(self, cdp: CDP) -> CDP:
    """
    Step 2: 构建鉴别诊断候选集并分层
    业务流程层调用功能模块层
    """
    # 1. 调用脑区C（diagnosis-engine-service）：生成鉴别诊断全集
    ddx_candidates = self.diagnosis_engine_service.generate_ddx_candidates(
        cdp.patient_state.problem_list
    )
    
    # 2. 调用脑区C + 脑区F：三层分层（脑区F由 risk-assessment-service 提供）
    three_layer_ddx = self.diagnosis_engine_service.three_layer_classification(
        ddx_candidates,
        cdp.patient_state,
        self.risk_assessment_service  # 脑区F参与
    )
    
    # 更新CDP（数据流转）
    cdp.ddx = three_layer_ddx
    
    return cdp
```

**数据流转**：
- 输入：`cdp.patient_state.problem_list`（Step 1的输出）
- 脑区C处理：生成鉴别诊断候选集
- 脑区F处理：识别高危诊断，参与三层分层
- 输出：更新`cdp.ddx`（三层排序的鉴别诊断列表）

#### Step 4：采集关键证据并形成排序与验证计划

**业务流程定义**：
```
Step 4: 采集关键证据并形成排序与验证计划
  目标：系统采集能够"推动排序变化"的关键证据，形成稳定的三层清单，并制定验证计划
```

**调用脑区**：
- **脑区B：主动问诊与信息补全** - 采集关键证据
- **脑区C：鉴别诊断引擎** - 固化三层排序
- **脑区D：检查/检验建议与价值评估** - 制定验证计划

**调用代码示例**：
```python
def step4_collect_evidence_and_plan(self, cdp: CDP) -> CDP:
    """
    Step 4: 采集关键证据并形成排序与验证计划
    业务流程层调用功能模块层（一个步骤调用多个脑区）
    """
    # 1. 调用脑区B（dialog-service）：采集关键证据
    evidence_list = self.dialog_service.collect_key_evidence(
        cdp.routing_path,
        cdp
    )
    
    # 2. 调用脑区C（diagnosis-engine-service）：固化三层排序
    three_layer_ranking = self.diagnosis_engine_service.solidify_three_layer_ranking(
        evidence_list,
        cdp.ddx
    )
    
    # 3. 调用脑区D（workup-planner-service）：制定验证计划
    verification_plan = self.workup_planner_service.generate_verification_plan(
        three_layer_ranking,
        cdp
    )
    
    # 更新CDP（数据流转）
    cdp.evidence_graph = evidence_list
    cdp.ddx = three_layer_ranking
    cdp.workup_plan = verification_plan
    
    return cdp
```

**数据流转**：
- 输入：`cdp.routing_path`（Step 3的输出）和`cdp.ddx`（Step 2的输出）
- 脑区B处理：采集关键证据
- 脑区C处理：根据证据更新三层排序
- 脑区D处理：制定验证计划
- 输出：更新`cdp.evidence_graph`、`cdp.ddx`、`cdp.workup_plan`

### 2.3 健康管理态流程示例

#### A1：需求分类

**业务流程定义**：
```
A1: 需求分类
  目标：把用户需求归入三类之一（筛查建议/健康目标管理/计划性健康需求）
```

**调用脑区**：
- **脑区A：病例理解与结构化** - 理解用户需求，结构化需求信息

**调用代码示例**：
```python
def a1_demand_classification(self, cdp: CDP) -> Dict:
    """
    A1: 需求分类
    业务流程层调用功能模块层
    """
    user_intent = cdp.patient_state.user_input
    
    # 调用脑区A：理解用户需求
    demand_type = self.parsing_service.classify_demand(user_intent)
    
    return {
        "type": demand_type["type"],
        "type_name": demand_type["name"],
        "confidence": demand_type["confidence"]
    }
```

### 2.4 调用关系总结

**特点**：
1. **多对多关系**：
   - 一个流程步骤可能调用多个脑区
   - 一个脑区可能被多个流程步骤调用

2. **明确标注**：
   - 每个流程步骤都明确标注了"参与脑区"
   - 调用顺序和调用时机由业务流程决定

3. **数据流转**：
   - 所有调用都围绕CDP进行
   - 脑区通过"读/写"CDP来传递数据

---

## 三、第二层关系：功能模块层 → 技术架构层（实现关系）

### 3.1 实现机制说明

**核心思想**：
- 功能模块层定义了"要完成什么功能"
- 技术架构层定义了"如何技术实现"
- 每个脑区的技术实现都基于双通道架构

**实现方式**：
- 通道1（结构化推理）：负责"决定该往哪想"的逻辑推理
- 通道2（语言与策略）：负责"怎么说、怎么问"的自然语言生成

### 3.2 脑区A：病例理解与结构化

**功能模块层定义**：
- **职责**：将非结构化的患者信息转换为结构化的临床要素
- **输入**：病历自由文本、对话内容、检查单等
- **输出**：结构化临床要素（症状、体征、既往史等）

**技术架构层实现**：

#### 通道1：结构化推理（决定"该往哪想"）

```python
class ClinicalParsingService:
    """
    脑区A的技术实现
    基于双通道架构
    """
    
    def normalize_concepts(self, text: str) -> List[Concept]:
        """
        概念归一化（通道1：结构化推理）
        """
        # 1. 使用医学概念识别工具（通道1技术）
        concepts = self.ner_model.extract(text)  # QuickUMLS / 中文医学NER模型
        
        # 2. 归一化到标准术语（通道1技术）
        normalized_concepts = []
        for concept in concepts:
            cui = self.normalizer.normalize(concept)  # CUI/ICD/SNOMED编码
            if cui:
                normalized_concepts.append(Concept(cui=cui, ...))
        
        return normalized_concepts
    
    def build_problem_list(self, concepts: List[Concept], patient_state: Dict) -> Dict:
        """
        形成完整问题清单（通道1：结构化推理）
        """
        # 使用结构化提取逻辑（通道1技术）
        problem_list = {
            "主要问题": self._extract_main_problem(concepts),
            "伴随问题": self._extract_accompanying_problems(concepts),
            "关键背景": self._extract_key_background(patient_state)
        }
        
        return problem_list
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

### 3.3 脑区C：鉴别诊断引擎

**功能模块层定义**：
- **职责**：生成Top-K鉴别诊断列表，每个诊断包含支持证据、反证、缺失证据
- **输入**：当前CDP的`patient_state`、知识图谱推理路径
- **输出**：鉴别诊断列表（三层排序）

**技术架构层实现**：

#### 通道1：结构化推理（决定"该往哪想"）

```python
class MultiEngineFusion:
    """
    脑区C的技术实现
    基于双通道架构
    """
    
    def generate_ddx_candidates(self, problem_list: Dict) -> List[Diagnosis]:
        """
        生成鉴别诊断候选集（通道1：结构化推理）
        """
        # 1. 知识图谱引擎（通道1技术：DR.KNOWS方法）
        kg_paths = self.kg_engine.retrieve_paths(
            symptoms=problem_list["主要问题"]["主诉名称"],
            max_hops=4
        )
        
        # 2. 规则引擎（通道1技术：症状组合规则）
        rule_matches = self.rule_engine.match(problem_list)
        
        # 3. 统计模型引擎（通道1技术：概率预测）
        statistical_predictions = self.statistical_engine.predict(problem_list)
        
        # 4. 多引擎融合（通道1技术：加权融合）
        fused_result = self.fuse_engine_results(
            kg_paths, rule_matches, statistical_predictions
        )
        
        return fused_result
    
    def three_layer_classification(self, candidates: List[Diagnosis], 
                                   patient_state: Dict,
                                   risk_assessment: RiskAssessmentEngine) -> Dict:
        """
        三层分层（通道1：结构化推理）
        """
        # 使用风险评估（脑区F）识别高危诊断
        high_risk = risk_assessment.identify_high_risk(candidates, patient_state)
        
        # 三层排序逻辑（通道1技术：基于证据强度排序）
        three_layer = {
            "首要假设": self._select_most_likely(candidates),
            "主要备选诊断": self._select_alternatives(candidates),
            "必须排除的高危诊断": high_risk
        }
        
        return three_layer
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

### 3.4 脑区B：主动问诊与信息补全

**功能模块层定义**：
- **职责**：像医生一样问"关键问题"，补齐鉴别诊断所需证据
- **输入**：当前CDP（特别是`ddx`和`uncertainty.missing_critical_info`）
- **输出**：问诊计划（下一问是什么、为什么问、如何问）

**技术架构层实现**：

#### 通道1：结构化推理（决定"该往哪想"）

```python
class DialogService:
    """
    脑区B的技术实现
    基于双通道架构
    """
    
    def identify_gaps(self, problem_list: Dict, cdp: CDP) -> List[str]:
        """
        识别信息缺口（通道1：结构化推理）
        """
        # 1. 基于临床决策分析计算信息增益（通道1技术）
        missing_info = cdp.uncertainty.missing_critical_info
        
        # 2. 评估每个缺失信息的重要性（通道1技术：信息增益计算）
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
        return gaps
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

3. **所有脑区都基于双通道**：
   - 每个脑区都需要"逻辑推理"和"自然语言生成"
   - 只是不同脑区在双通道上的侧重点不同

---

## 四、数据流转：CDP贯穿三个层次

### 4.1 CDP的作用

**核心思想**：
- CDP（Clinical Decision Package）是系统内部一切推理的核心数据结构
- 所有层次都围绕CDP进行数据流转
- CDP贯穿业务流程层、功能模块层、技术架构层

### 4.2 数据流转路径

```
用户输入
    ↓
业务流程层（Step 1）
    ↓ 调用
功能模块层（脑区A）
    ↓ 实现
技术架构层（通道1：概念归一化）
    ↓ 更新
CDP.patient_state.problem_list
    ↓
业务流程层（Step 2）
    ↓ 调用
功能模块层（脑区C）
    ↓ 实现
技术架构层（通道1：知识图谱推理）
    ↓ 更新
CDP.ddx
    ↓
业务流程层（Step 3）
    ↓ 调用
功能模块层（脑区B）
    ↓ 实现
技术架构层（通道2：生成问诊问题）
    ↓ 输出
自然语言问诊问题（返回给用户）
```

### 4.3 具体数据流转示例

#### 示例：从用户输入到诊断结果

**Step 1：识别问题**

```python
# 1. 业务流程层接收用户输入
user_input = "我这两天胸口闷，走几步就喘"

# 2. 业务流程层调用脑区A
cdp = step1_identify_problem(cdp)

# 3. 脑区A通过通道1实现概念归一化
normalized_concepts = parsing_service.normalize_concepts(user_input)
# 输出：["胸闷", "活动后气促"] → CUI编码

# 4. 脑区A通过通道1实现结构化提取
problem_list = parsing_service.build_problem_list(normalized_concepts)
# 输出：{
#   "主要问题": {"主诉名称": "胸闷样不适", ...},
#   "伴随问题": [{"症状名称": "活动后气促", ...}]
# }

# 5. 更新CDP
cdp.patient_state.problem_list = problem_list
```

**Step 2：构建鉴别诊断候选集**

```python
# 1. 业务流程层调用脑区C
cdp = step2_build_ddx_candidates(cdp)

# 2. 脑区C通过通道1实现知识图谱推理
kg_paths = kg_engine.retrieve_paths(
    symptoms=["胸闷", "活动后气促"],
    max_hops=4
)
# 输出：推理路径 → ["胸闷" --[相关]--> "心绞痛", "胸闷" --[相关]--> "急性心梗", ...]

# 3. 脑区C通过通道1实现多引擎融合
ddx_candidates = fusion_engine.fuse(kg_paths, rule_matches, ...)
# 输出：鉴别诊断候选集

# 4. 脑区C通过通道1实现三层分层
three_layer_ddx = ddx_engine.three_layer_classification(ddx_candidates)
# 输出：{
#   "首要假设": [{"疾病名称": "心绞痛", "概率": 0.4}],
#   "主要备选诊断": [{"疾病名称": "急性心梗", "概率": 0.3}],
#   "必须排除的高危诊断": [{"疾病名称": "急性心梗", "必须排除": true}]
# }

# 5. 更新CDP
cdp.ddx = three_layer_ddx
```

**Step 3：组织候选集并建立分流路径**

```python
# 1. 业务流程层调用脑区B
cdp = step3_organize_routing_path(cdp)

# 2. 脑区B通过通道1实现信息缺口识别
gaps = interview_service.identify_gaps(cdp)
# 输出：["是否静息也发作", "是否持续不缓解", ...]

# 3. 脑区B通过通道2实现生成问诊问题
question = interview_service.generate_question(gaps[0], cdp)
# 输出："症状是否在静息时也出现，且持续不缓解？"

# 4. 更新CDP（分流路径）
cdp.routing_path = {
    "first_layer_questions": [question],
    "high_risk_path": {...},
    "normal_path": {...}
}
```

### 4.4 CDP字段与三个层次的对应关系

| CDP字段 | 业务流程层 | 功能模块层 | 技术架构层 |
|---------|-----------|-----------|-----------|
| `health_state_assessment` | 阶段0：健康状态判定 | 脑区0 | 通道1：风险评估逻辑 |
| `patient_state.problem_list` | Step 1：识别问题 | 脑区A | 通道1：概念归一化 |
| `ddx` | Step 2：构建候选集 | 脑区C | 通道1：知识图谱推理 |
| `routing_path` | Step 3：建立分流路径 | 脑区B + 脑区C | 通道1：信息增益计算 + 通道2：问题生成 |
| `evidence_graph` | Step 4：采集证据 | 脑区B | 通道1：证据分析 |
| `workup_plan` | Step 4：验证计划 | 脑区D | 通道1：检查价值评估 |
| `management_plan` | Step 5：处置建议 | 脑区E | 通道1：治疗方案推理 + 通道2：方案生成 |
| `evidence_graph`（解释） | Step 5：证据链 | 脑区G | 通道2：解释生成 |

---

## 五、完整调用链示例

### 5.1 临床诊疗态完整调用链

```
用户输入："我这两天胸口闷，走几步就喘，昨天还出汗，有点恶心。"
    ↓
┌─────────────────────────────────────────────────────────────┐
│ 业务流程层：Step 1 - 识别问题                                │
└─────────────────────────────────────────────────────────────┘
    ↓ 调用
┌─────────────────────────────────────────────────────────────┐
│ 功能模块层：脑区A - 病例理解与结构化                          │
│ 功能模块层：脑区B - 主动问诊与信息补全                        │
└─────────────────────────────────────────────────────────────┘
    ↓ 实现
┌─────────────────────────────────────────────────────────────┐
│ 技术架构层：通道1 - 概念归一化（QuickUMLS/NER模型）          │
│ 技术架构层：通道1 - 信息缺口识别（临床决策分析）              │
│ 技术架构层：通道2 - 生成追问问题（LLM）                      │
└─────────────────────────────────────────────────────────────┘
    ↓ 更新
CDP.patient_state.problem_list = {
  "主要问题": {"主诉名称": "胸闷样不适", ...},
  "伴随问题": [...],
  "信息缺口": ["是否静息也发作", ...]
}
    ↓
┌─────────────────────────────────────────────────────────────┐
│ 业务流程层：Step 2 - 构建鉴别诊断候选集并分层                  │
└─────────────────────────────────────────────────────────────┘
    ↓ 调用
┌─────────────────────────────────────────────────────────────┐
│ 功能模块层：脑区C - 鉴别诊断引擎                              │
│ 功能模块层：脑区F - 风险与急症识别                            │
└─────────────────────────────────────────────────────────────┘
    ↓ 实现
┌─────────────────────────────────────────────────────────────┐
│ 技术架构层：通道1 - 知识图谱路径检索（DR.KNOWS方法）          │
│ 技术架构层：通道1 - 多引擎融合（规则+KG+统计+LLM）           │
│ 技术架构层：通道1 - 三层分层（基于证据强度）                  │
└─────────────────────────────────────────────────────────────┘
    ↓ 更新
CDP.ddx = {
  "首要假设": [{"疾病名称": "心血管相关方向", "概率": 0.4}],
  "主要备选诊断": [...],
  "必须排除的高危诊断": [{"疾病名称": "高风险心血管急症", ...}]
}
    ↓
┌─────────────────────────────────────────────────────────────┐
│ 业务流程层：Step 3 - 组织候选集并建立分流路径                  │
└─────────────────────────────────────────────────────────────┘
    ↓ 调用
┌─────────────────────────────────────────────────────────────┐
│ 功能模块层：脑区C - 组织推理子组                              │
│ 功能模块层：脑区B - 生成分流问题                              │
└─────────────────────────────────────────────────────────────┘
    ↓ 实现
┌─────────────────────────────────────────────────────────────┐
│ 技术架构层：通道1 - 推理子组组织（诊断树理论）                │
│ 技术架构层：通道1 - 关键差异点提炼                            │
│ 技术架构层：通道2 - 生成分流问题（LLM）                      │
└─────────────────────────────────────────────────────────────┘
    ↓ 更新
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
└─────────────────────────────────────────────────────────────┘
    ↓ 调用
┌─────────────────────────────────────────────────────────────┐
│ 功能模块层：脑区B - 采集关键证据                              │
│ 功能模块层：脑区C - 固化三层排序                              │
│ 功能模块层：脑区D - 制定验证计划                              │
└─────────────────────────────────────────────────────────────┘
    ↓ 实现
┌─────────────────────────────────────────────────────────────┐
│ 技术架构层：通道1 - 证据分析（支持/反对/不确定）              │
│ 技术架构层：通道1 - 三层排序更新（基于证据重排）              │
│ 技术架构层：通道1 - 验证计划生成（检查价值评估）              │
└─────────────────────────────────────────────────────────────┘
    ↓ 更新
CDP.evidence_graph = [...]
CDP.ddx = {...}  # 更新后的三层排序
CDP.workup_plan = [...]
    ↓
┌─────────────────────────────────────────────────────────────┐
│ 业务流程层：Step 5 - 回填证据并输出终点结论包                  │
└─────────────────────────────────────────────────────────────┘
    ↓ 调用
┌─────────────────────────────────────────────────────────────┐
│ 功能模块层：脑区A - 回填证据                                  │
│ 功能模块层：脑区C - 更新三层排序                              │
│ 功能模块层：脑区E - 生成处置建议                              │
│ 功能模块层：脑区F - 生成升级触发条件                          │
│ 功能模块层：脑区G - 生成终点结论包                            │
└─────────────────────────────────────────────────────────────┘
    ↓ 实现
┌─────────────────────────────────────────────────────────────┐
│ 技术架构层：通道1 - 证据回填逻辑                              │
│ 技术架构层：通道1 - 诊断排序更新                              │
│ 技术架构层：通道1 - 治疗方案推理                              │
│ 技术架构层：通道2 - 生成终点结论包（LLM）                    │
│ 技术架构层：通道2 - 生成自然语言解释                          │
└─────────────────────────────────────────────────────────────┘
    ↓ 更新
CDP.conclusion_package = {
  "结论": {...},
  "必须排除项状态": {...},
  "关键依据": {...},
  "行动与随访": {...}
}
    ↓
输出给用户：自然语言诊断结果 + 证据链 + 行动建议
```
