# Step 2细化实现方案

> **文档定位**：本文档详细设计健康状态判定服务中Step 2（识别症状/困扰）的细化实现方案。  
> **目标**：将当前基于简单规则判断的简化实现升级为基于LLM和上下文理解的完整实现。

**文档状态**：
- ✅ **架构设计**：已完成
- 📋 **实施计划**：待实施
- 📋 **技术选型**：待确认

---

## 一、Step 1与Step 2的区别与联系

### 1.1 Step 1（NLU）的职责与输出

**Step 1：接收用户输入（NLU细化）**

**核心职责**：
- **自然语言理解**：理解用户自然语言输入
- **意图识别**：识别用户意图（screening/diagnosis/mixed/unknown）
- **实体提取**：提取症状实体、基本信息、生命体征、时间信息等
- **实体归一化**：将提取的实体映射到标准医学术语

**输出内容**：
```python
{
    "original_input": "我最近胸痛，想做个检查",
    "intent": "mixed",  # 意图：筛查/诊断/混合/不确定
    "intent_confidence": 0.85,
    "entities": [
        {"type": "symptom", "value": "胸痛", "standard_term": "胸痛", "cui": "C0002962"},
        {"type": "temporal", "value": "最近", "start_time": "3天前"}
    ],
    "symptoms": ["胸痛"],  # 症状实体列表
    "has_symptoms": True,  # 是否包含症状
    "has_screening_intent": True,  # 是否包含筛查意图
    "basic_info": {"age": 30, "gender": "男"},
    "temporal_info": {"start_time": "3天前", "duration": "持续"},
    "vital_signs": {}
}
```

**关注点**：**"理解用户说了什么"** - 提取和结构化信息

### 1.2 Step 2（症状/困扰识别）的职责与输出

**Step 2：识别症状/困扰**

**核心职责**：
- **症状/困扰识别**：基于NLU结果，深入识别症状和困扰
- **情况判断**：判断三种情况（A/B/C）
- **置信度评估**：综合评估判断的置信度
- **上下文分析**：分析症状的时间、程度、频率等上下文信息

**输出内容**：
```python
{
    "status": "uncertain",  # 情况：no_symptom/has_symptom/uncertain
    "symptoms": [
        {
            "original_text": "胸痛",
            "standard_term": "胸痛",
            "temporal_info": {"start_time": "3天前", "duration": "持续"},
            "severity": "中度",
            "location": "胸部"
        }
    ],
    "concerns": [
        {
            "type": "心理",
            "description": "担心心脏问题",
            "severity": "轻度"
        }
    ],
    "confidence": 0.75,
    "reasoning": "用户既有症状又有筛查意图，属于混合诉求"
}
```

**关注点**：**"判断用户的状态"** - 业务逻辑分析和决策

### 1.3 核心区别

| 维度 | Step 1（NLU） | Step 2（症状/困扰识别） |
|------|--------------|----------------------|
| **职责定位** | 自然语言理解 | 业务逻辑判断 |
| **关注点** | "理解用户说了什么" | "判断用户的状态" |
| **输入** | 原始用户输入文本 | Step 1的NLU结果 |
| **输出** | 结构化信息（意图、实体） | 业务判断结果（情况A/B/C） |
| **技术重点** | 语义理解、实体提取 | 业务规则、综合判断 |
| **处理粒度** | 词/短语/句子级别 | 整体状态级别 |
| **可复用性** | 通用NLU能力 | 特定业务逻辑 |

### 1.4 核心联系

1. **数据流依赖**：
   ```
   用户输入 → Step 1（NLU）→ Step 2（症状/困扰识别）→ Step 3（澄清）
   ```

2. **信息传递**：
   - Step 1提供"原材料"（意图、实体、上下文）
   - Step 2基于这些"原材料"进行"深度加工"（业务分析）

3. **互补关系**：
   - Step 1负责"提取信息"（What）
   - Step 2负责"分析状态"（How/Why）

### 1.5 Step 2的必要性

**为什么需要Step 2？为什么不能直接用Step 1的结果？**

#### 1.5.1 职责分离原则

- **Step 1**：通用NLU能力，专注于理解语言
- **Step 2**：业务逻辑判断，专注于状态分析
- **分离的好处**：各司其职，易于维护和扩展

#### 1.5.2 业务逻辑复杂性

Step 2需要处理的业务逻辑，超出了Step 1的NLU范围：

1. **困扰识别**：
   - Step 1只能提取显式的症状实体
   - Step 2需要识别隐式的困扰（心理、功能变化、异常感觉）
   - 例如："走几步就喘" → 功能变化困扰

2. **情况判断**：
   - Step 1只提供意图和实体
   - Step 2需要综合判断三种情况（A/B/C）
   - 需要结合意图、症状、困扰、置信度等多因素

3. **上下文理解**：
   - Step 1提取时间、程度等信息
   - Step 2需要分析这些信息的业务含义
   - 例如：症状持续时间影响判断置信度

#### 1.5.3 置信度评估

- **Step 1**：提供意图置信度、实体置信度
- **Step 2**：需要综合评估整体判断的置信度
- 需要结合多个因素：症状强度、上下文信息、意图明确性等

#### 1.5.4 降级和容错

- **Step 1失败**：可以降级到关键词匹配
- **Step 2失败**：可以降级到规则判断
- 两层降级机制，提高系统鲁棒性

#### 1.5.5 业务规则集中管理

- Step 2集中管理业务规则（情况A/B/C的判断规则）
- 便于业务人员理解和调整规则
- 便于测试和验证业务逻辑

### 1.6 实际案例对比

**案例1：用户输入"我想体检，但最近有点不舒服"**

**Step 1输出**：
```python
{
    "intent": "mixed",
    "has_symptoms": True,
    "has_screening_intent": True,
    "symptoms": ["不舒服"]
}
```

**Step 2输出**：
```python
{
    "status": "uncertain",  # 混合诉求，需要澄清
    "symptoms": [{"original_text": "不舒服", "severity": "轻度"}],
    "concerns": [],
    "confidence": 0.6,  # 置信度较低，因为"不舒服"太模糊
    "reasoning": "用户既有筛查意图又有症状，但症状不明确，属于情况C"
}
```

**分析**：
- Step 1识别出混合意图和症状实体
- Step 2判断为"情况C"（不确定），因为症状不明确，需要澄清
- 如果只有Step 1，无法做出这个业务判断

**案例2：用户输入"我最近胸痛，担心是心脏病"**

**Step 1输出**：
```python
{
    "intent": "diagnosis",
    "has_symptoms": True,
    "symptoms": ["胸痛"]
}
```

**Step 2输出**：
```python
{
    "status": "has_symptom",
    "symptoms": [{"original_text": "胸痛", "severity": "中度"}],
    "concerns": [
        {"type": "心理", "description": "担心是心脏病", "severity": "中度"}
    ],
    "confidence": 0.9,
    "reasoning": "用户有明确症状和心理困扰，属于情况B"
}
```

**分析**：
- Step 1识别出症状实体"胸痛"
- Step 2识别出心理困扰"担心是心脏病"（这是Step 1无法识别的）
- Step 2综合判断为"情况B"（有症状/困扰）

---

## 二、现状分析

### 2.1 Step 2当前实现（简化版）

**位置**：`app/services/entry_assessment/step2_identify_symptom.py`

**实现方式**：
- 基于Step 1输出的简单判断
- 根据意图（intent）和症状存在性（has_symptoms）进行规则判断
- 无法理解症状的上下文信息（时间、程度、频率等）
- 无法识别困扰（心理、功能变化等）
- 无法处理复杂情况（混合诉求、模糊表达等）

**局限性**：
1. ❌ **上下文理解缺失**：无法利用NLU提取的时间、程度、频率等上下文信息
2. ❌ **困扰识别不足**：只能识别简单的关键词（"担心"、"害怕"等），无法理解复杂的困扰表达
3. ❌ **混合诉求处理粗糙**：对混合诉求的判断过于简单，可能误判
4. ❌ **置信度评估不准确**：置信度计算过于简单，无法反映实际情况

---

## 二、设计目标

### 2.1 Step 2细化目标

**核心功能**：
1. **智能症状/困扰识别**
   - 利用NLU提取的实体和上下文信息
   - 识别症状的时间、程度、频率等特征
   - 识别困扰（心理、功能变化、异常感觉等）

2. **准确情况判断**
   - 准确判断"情况A"（明确无症状）
   - 准确判断"情况B"（存在症状/困扰）
   - 准确判断"情况C"（不确定/模糊/混合诉求）

3. **置信度评估**
   - 基于多因素综合评估置信度
   - 考虑症状强度、上下文信息、意图明确性等

### 2.2 性能目标

- **准确率**：情况判断准确率 ≥ 90%
- **响应时间**：处理时间 ≤ 500ms
- **用户体验**：准确识别症状和困扰，提供清晰的判断结果

---

## 三、技术架构设计

### 3.1 Step 2细化架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Step 2 细化架构                           │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ 症状/困扰识别  │    │  情况判断器   │    │  上下文分析器  │
│ Symptom      │    │ Situation    │    │ Context      │
│ Concern      │    │ Judger       │    │ Analyzer     │
│ Identifier   │    │              │    │              │
└──────────────┘    └──────────────┘    └──────────────┘
        │                     │                     │
        │                     │                     │
        ▼                     ▼                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      LLM服务层                               │
│  - LLM客户端（LangChainLLMClient）                           │
│  - Prompt管理（PromptManager）                              │
│  - 结果解析（ResponseParser）                               │
└─────────────────────────────────────────────────────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    NLU结果集成层                              │
│  - Step 1的NLU结果（意图、实体、上下文）                     │
│  - 实体归一化结果                                            │
│  - 上下文信息提取                                            │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Step 2细化模块设计

#### 3.2.1 症状/困扰识别器（SymptomConcernIdentifier）

**职责**：
- 基于NLU结果识别症状和困扰
- 提取症状的上下文信息（时间、程度、频率等）
- 识别困扰（心理、功能变化、异常感觉等）

**实现策略**：
- **主策略**：基于Step 1的NLU结果进行智能分析
- **辅助策略**：使用LLM进行复杂情况判断（如混合诉求）

**接口设计**：
```python
class SymptomConcernIdentifier:
    async def identify(
        self, 
        nlu_result: Dict[str, Any],
        context: Optional[Dict] = None
    ) -> SymptomIdentificationResult:
        """
        识别症状/困扰
        
        Args:
            nlu_result: Step 1的NLU结果，包含：
                - intent: str - 用户意图
                - entities: List[Dict] - 提取的实体
                - symptoms: List[SymptomEntity] - 症状实体
                - context: Dict - 上下文信息
            context: 额外的上下文信息（可选）
        
        Returns:
            SymptomIdentificationResult: 识别结果
        """
        pass
```

**输出模型**：
```python
class SymptomIdentificationResult(BaseModel):
    status: str  # "no_symptom" | "has_symptom" | "uncertain"
    symptoms: List[SymptomInfo]  # 症状详细信息
    concerns: List[ConcernInfo]  # 困扰信息
    confidence: float  # 识别置信度
    reasoning: str  # 识别理由
    context_analysis: Dict[str, Any]  # 上下文分析结果

class SymptomInfo(BaseModel):
    original_text: str  # 原始文本
    standard_term: str  # 标准术语
    cui: Optional[str]  # UMLS CUI
    temporal_info: Dict[str, Any]  # 时间信息（开始时间、持续时间、频率）
    severity: str  # 严重程度（轻度/中度/重度）
    location: Optional[str]  # 位置
    context: Dict[str, Any]  # 其他上下文信息

class ConcernInfo(BaseModel):
    type: str  # 困扰类型（心理/功能变化/异常感觉等）
    description: str  # 困扰描述
    severity: str  # 严重程度
    confidence: float  # 识别置信度
```

#### 3.2.2 情况判断器（SituationJudger）

**职责**：
- 基于症状/困扰识别结果判断三种情况（A/B/C）
- 综合评估置信度

**实现策略**：
- **主策略**：基于规则引擎判断（快速、准确）
- **辅助策略**：使用LLM进行复杂情况判断（混合诉求、模糊表达）

**接口设计**：
```python
class SituationJudger:
    async def judge(
        self,
        symptom_result: SymptomIdentificationResult,
        nlu_result: Dict[str, Any]
    ) -> SituationJudgmentResult:
        """
        判断情况（A/B/C）
        
        Args:
            symptom_result: 症状/困扰识别结果
            nlu_result: Step 1的NLU结果
        
        Returns:
            SituationJudgmentResult: 判断结果
        """
        pass
```

**判断规则**：
1. **情况A（no_symptom）**：
   - 意图为"screening"且无症状
   - 明确表达无症状（如"我想做个体检"）
   - 置信度 ≥ 0.8

2. **情况B（has_symptom）**：
   - 意图为"diagnosis"且有症状
   - 存在明确的症状或困扰
   - 置信度 ≥ 0.8

3. **情况C（uncertain）**：
   - 意图为"mixed"或"unknown"
   - 症状存在但不明确
   - 混合诉求（既有筛查又有症状）
   - 置信度 < 0.8

#### 3.2.3 上下文分析器（ContextAnalyzer）

**职责**：
- 分析症状的上下文信息（时间、程度、频率等）
- 提取关键上下文特征
- 为情况判断提供依据

**接口设计**：
```python
class ContextAnalyzer:
    def analyze(
        self,
        nlu_result: Dict[str, Any],
        symptoms: List[SymptomInfo]
    ) -> Dict[str, Any]:
        """
        分析上下文信息
        
        Args:
            nlu_result: Step 1的NLU结果
            symptoms: 症状列表
        
        Returns:
            Dict: 上下文分析结果
        """
        pass
```

---

## 四、详细设计

### 4.1 Step 2细化Prompt设计

**症状/困扰识别Prompt模板**：
```
你是一个医疗AI助手，需要从用户输入中识别症状和困扰。

用户输入：{user_input}
NLU结果：
- 意图：{intent}
- 症状实体：{symptoms}
- 上下文信息：{context}

请分析：
1. **症状识别**：
   - 识别所有症状（包括轻微症状）
   - 提取症状的时间、程度、频率等信息
   - 识别症状的位置和性质

2. **困扰识别**：
   - 识别心理困扰（担心、焦虑、害怕等）
   - 识别功能变化（如"走几步就喘"）
   - 识别异常感觉（如"感觉不对劲"）

3. **情况判断**：
   - 情况A：明确无症状（纯筛查/体检规划）
   - 情况B：存在症状/困扰（不论轻重）
   - 情况C：不确定/模糊/混合诉求

请以JSON格式返回结果：
{{
    "status": "no_symptom|has_symptom|uncertain",
    "symptoms": [
        {{
            "original_text": "原始文本",
            "standard_term": "标准术语",
            "temporal_info": {{"start_time": "...", "duration": "...", "frequency": "..."}},
            "severity": "轻度|中度|重度",
            "location": "位置",
            "context": {{}}
        }}
    ],
    "concerns": [
        {{
            "type": "心理|功能变化|异常感觉",
            "description": "困扰描述",
            "severity": "轻度|中度|重度"
        }}
    ],
    "confidence": 0.0-1.0,
    "reasoning": "识别理由"
}}
```

---

## 五、实施计划

### 阶段1：Step 2细化实现（短期，1-2周）

**目标**：基于NLU结果实现智能症状/困扰识别

**实施内容**：

1. **创建Step 2细化模块**
   - 创建`app/services/entry_assessment/step2_enhanced/`目录
   - 实现`SymptomConcernIdentifier`类
   - 实现`SituationJudger`类
   - 实现`ContextAnalyzer`类

2. **集成NLU结果**
   - 利用Step 1的NLU结果（意图、实体、上下文）
   - 提取症状的上下文信息（时间、程度、频率等）
   - 识别困扰（心理、功能变化等）

3. **实现情况判断逻辑**
   - 基于规则引擎判断三种情况（A/B/C）
   - 综合评估置信度
   - 使用LLM处理复杂情况（混合诉求、模糊表达）

4. **重构Step 2**
   - 修改`step2_identify_symptom.py`，使用新的细化模块
   - 保持接口兼容性
   - 添加降级机制（LLM失败时使用规则判断）

5. **测试和验证**
   - 单元测试：测试各个模块功能
   - 集成测试：测试与Step 1的集成
   - 性能测试：验证响应时间
   - 准确率测试：验证情况判断准确率

**预期效果**：
- 情况判断准确率提升至90%+
- 支持复杂症状和困扰识别
- 置信度评估更准确
- 处理时间 ≤ 500ms

---

## 六、代码结构设计

```
health-state-assessment-service/
├── app/
│   ├── services/
│   │   ├── entry_assessment/
│   │   │   ├── step1_receive_input.py      # Step 1（已完成细化）
│   │   │   ├── step2_identify_symptom.py   # Step 2（重构：使用细化模块）
│   │   │   ├── step2_enhanced/             # 新增：Step 2细化模块
│   │   │   │   ├── __init__.py
│   │   │   │   ├── symptom_concern_identifier.py  # 症状/困扰识别器
│   │   │   │   ├── situation_judger.py            # 情况判断器
│   │   │   │   └── context_analyzer.py             # 上下文分析器
│   │   │   ├── step3_clarification.py     # Step 3（待细化）
│   │   │   ├── step4_red_flag_check.py
│   │   │   └── step5_path_selection.py
│   │   │
│   │   └── nlu/                           # NLU模块（第一步已完成）
│   │       ├── intent_recognizer.py
│   │       ├── entity_extractor.py
│   │       └── ...
│   │
│   ├── models/
│   │   └── entry_assessment.py            # 新增：入口判定数据模型
```

---

## 七、风险评估和应对

### 7.1 技术风险

**风险1：LLM调用失败或超时**
- **影响**：服务不可用
- **应对**：实现降级机制，LLM失败时使用规则判断

**风险2：情况判断准确率不达标**
- **影响**：用户体验差
- **应对**：持续优化判断逻辑，收集错误案例进行迭代

### 7.2 业务风险

**风险1：症状识别不准确**
- **影响**：误判情况，影响后续流程
- **应对**：建立测试用例库，持续优化识别逻辑

**风险2：性能不达标**
- **影响**：响应时间过长
- **应对**：优化LLM调用策略，使用缓存机制

---

## 八、成功指标

### 8.1 功能指标

- ✅ 情况判断准确率 ≥ 90%
- ✅ 支持复杂症状和困扰识别
- ✅ 置信度评估准确

### 8.2 性能指标

- ✅ 处理时间 ≤ 500ms
- ✅ 系统可用性 ≥ 99%

### 8.3 业务指标

- ✅ 情况判断错误率 ≤ 10%
- ✅ 用户满意度提升

---

## 九、后续优化方向

### 9.1 短期优化（1-2个月）

1. **Prompt优化**
   - 基于实际使用数据优化Prompt
   - A/B测试不同Prompt版本

2. **上下文理解优化**
   - 支持多轮对话上下文
   - 实现对话状态管理

### 9.2 中期优化（3-6个月）

1. **知识图谱集成**
   - 集成医学知识图谱（UMLS等）
   - 实现症状和困扰的语义理解

2. **个性化优化**
   - 基于用户历史数据优化识别
   - 实现用户画像分析

---

## 十、附录

### 10.1 参考文档

- 《NLU细化实现方案.md》- 第一步细化方案
- 《health-state-assessment-service - 服务实现方案.md》
- 《health-state-assessment-service - 已实现功能清单.md》
- 《Step3细化实现方案.md》- Step 3细化方案（依赖Step 2输出）

### 10.2 技术栈

- **LLM框架**：LangChain
- **数据验证**：Pydantic
- **配置管理**：pydantic-settings

---

**文档版本**：v1.0  
**创建时间**：2026-01-28  
**最后更新**：2026-01-28

