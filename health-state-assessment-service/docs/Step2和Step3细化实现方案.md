# Step 2和Step 3细化实现方案

> **文档定位**：本文档详细设计健康状态判定服务中Step 2（识别症状/困扰）和Step 3（方向澄清）的细化实现方案。  
> **目标**：将当前基于简单规则判断的简化实现升级为基于LLM和上下文理解的完整实现。

**文档状态**：
- ✅ **架构设计**：已完成
- 📋 **实施计划**：待实施
- 📋 **技术选型**：待确认

---

## 一、现状分析

### 1.1 Step 2当前实现（简化版）

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

### 1.2 Step 3当前实现（简化版）

**位置**：`app/services/entry_assessment/step3_clarification.py`

**实现方式**：
- 自动推断方向，不等待用户回答
- 根据症状存在性和筛查意图简单判断
- 无法真正澄清用户意图

**局限性**：
1. ❌ **无法真正澄清**：自动推断可能误判，不符合设计规范
2. ❌ **澄清问题固定**：澄清问题过于通用，不够个性化
3. ❌ **无法等待用户回答**：没有实现异步澄清流程
4. ❌ **澄清逻辑简单**：无法基于上下文生成智能澄清问题

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

### 2.2 Step 3细化目标

**核心功能**：
1. **智能澄清问题生成**
   - 基于上下文生成个性化的澄清问题
   - 使用LLM生成澄清问题，而非固定模板

2. **异步澄清流程**
   - 返回澄清问题给前端
   - 等待用户回答后再继续流程
   - 支持澄清结果处理

3. **澄清结果处理**
   - 基于用户回答确定方向（A或B）
   - 提供澄清置信度评估

### 2.3 性能目标

- **准确率**：Step 2情况判断准确率 ≥ 90%，Step 3澄清准确率 ≥ 85%
- **响应时间**：Step 2处理时间 ≤ 500ms，Step 3澄清问题生成时间 ≤ 2秒
- **用户体验**：澄清问题清晰、个性化，用户易于理解

---

## 三、技术架构设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Step 2 & Step 3 细化架构                   │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Step 2细化   │    │  Step 3细化   │    │  上下文管理器  │
│ Symptom      │    │ Clarification│    │ Context      │
│ Identifier   │    │ Manager      │    │ Manager      │
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

### 3.3 Step 3细化模块设计

#### 3.3.1 澄清问题生成器（ClarificationQuestionGenerator）

**职责**：
- 基于上下文生成个性化的澄清问题
- 使用LLM生成澄清问题

**实现策略**：
- **主策略**：使用LLM生成澄清问题（基于上下文）
- **降级策略**：使用模板生成澄清问题

**接口设计**：
```python
class ClarificationQuestionGenerator:
    async def generate(
        self,
        symptom_result: SymptomIdentificationResult,
        nlu_result: Dict[str, Any],
        context: Optional[Dict] = None
    ) -> ClarificationQuestion:
        """
        生成澄清问题
        
        Args:
            symptom_result: Step 2的症状识别结果
            nlu_result: Step 1的NLU结果
            context: 额外的上下文信息（可选）
        
        Returns:
            ClarificationQuestion: 澄清问题
        """
        pass
```

**输出模型**：
```python
class ClarificationQuestion(BaseModel):
    question: str  # 澄清问题文本
    question_type: str  # 问题类型（single_choice/multiple_choice/open_ended）
    options: Optional[List[str]]  # 选项（如果是选择题）
    context: Dict[str, Any]  # 问题上下文
    reasoning: str  # 生成理由
```

#### 3.3.2 澄清结果处理器（ClarificationResultProcessor）

**职责**：
- 处理用户对澄清问题的回答
- 基于回答确定方向（A或B）
- 提供澄清置信度评估

**接口设计**：
```python
class ClarificationResultProcessor:
    async def process(
        self,
        question: ClarificationQuestion,
        user_answer: str,
        context: Optional[Dict] = None
    ) -> ClarificationResult:
        """
        处理澄清结果
        
        Args:
            question: 澄清问题
            user_answer: 用户回答
            context: 额外的上下文信息（可选）
        
        Returns:
            ClarificationResult: 澄清结果
        """
        pass
```

**输出模型**：
```python
class ClarificationResult(BaseModel):
    direction: str  # "A" | "B"
    confidence: float  # 澄清置信度
    reasoning: str  # 澄清理由
    clarified: bool  # 是否已澄清
    user_answer: str  # 用户回答
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

### 4.2 Step 3细化Prompt设计

**澄清问题生成Prompt模板**：
```
你是一个医疗AI助手，需要生成澄清问题来确认用户的主要目标。

用户输入：{user_input}
当前情况：{situation}（情况C：不确定/模糊/混合诉求）
症状识别结果：{symptom_result}

请生成一个澄清问题，帮助确认用户主要目标是：
- A：健康筛查/体检规划
- B：症状咨询/问题排查

要求：
1. 问题要清晰、简洁、易于理解
2. 基于用户的具体情况个性化生成
3. 避免使用医学术语，使用通俗易懂的语言

请以JSON格式返回结果：
{{
    "question": "澄清问题文本",
    "question_type": "single_choice",
    "options": ["健康筛查/体检规划", "症状咨询/问题排查"],
    "reasoning": "生成理由"
}}
```

**澄清结果处理Prompt模板**：
```
你是一个医疗AI助手，需要根据用户对澄清问题的回答确定方向。

澄清问题：{question}
用户回答：{user_answer}
原始输入：{user_input}

请分析用户回答，确定用户主要目标是：
- A：健康筛查/体检规划
- B：症状咨询/问题排查

请以JSON格式返回结果：
{{
    "direction": "A|B",
    "confidence": 0.0-1.0,
    "reasoning": "确定理由"
}}
```

---

## 五、分阶段实施计划

### 阶段1：Step 2细化实现（短期，1-2周）

**目标**：基于NLU结果实现智能症状/困扰识别

**实施内容**：

1. **创建Step 2细化模块**
   - 创建`app/services/entry_assessment/step2_enhanced/`目录
   - 实现`SymptomConcernIdentifier`类
   - 实现`SituationJudger`类

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

**预期效果**：
- 情况判断准确率提升至90%+
- 支持复杂症状和困扰识别
- 置信度评估更准确

### 阶段2：Step 3细化实现（中期，1-2周）

**目标**：实现真正的交互式澄清流程

**实施内容**：

1. **创建Step 3细化模块**
   - 创建`app/services/entry_assessment/step3_enhanced/`目录
   - 实现`ClarificationQuestionGenerator`类
   - 实现`ClarificationResultProcessor`类

2. **实现澄清问题生成**
   - 使用LLM生成个性化的澄清问题
   - 基于上下文信息生成问题
   - 支持多种问题类型（单选、多选、开放）

3. **实现异步澄清流程**
   - 修改API接口，支持返回澄清问题
   - 实现澄清结果处理接口
   - 前端集成澄清流程

4. **重构Step 3**
   - 修改`step3_clarification.py`，使用新的细化模块
   - 支持异步澄清流程
   - 添加降级机制（LLM失败时使用模板）

**预期效果**：
- 澄清问题个性化、清晰
- 支持异步澄清流程
- 澄清准确率提升至85%+

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
│   │   │   ├── step3_clarification.py     # Step 3（重构：使用细化模块）
│   │   │   ├── step3_enhanced/            # 新增：Step 3细化模块
│   │   │   │   ├── __init__.py
│   │   │   │   ├── clarification_question_generator.py  # 澄清问题生成器
│   │   │   │   ├── clarification_result_processor.py    # 澄清结果处理器
│   │   │   │   └── clarification_manager.py              # 澄清管理器
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
│   │
│   └── api/
│       └── routes.py                      # 修改：添加澄清接口
```

---

## 七、API接口设计

### 7.1 澄清问题接口

**接口路径**：`POST /api/v1/entry-assessment/clarification/question`

**请求格式**：
```json
{
  "cdpId": "cdp-123456",
  "userInput": "我想体检，但最近有点不舒服",
  "step2Result": {
    "status": "uncertain",
    "symptoms": ["不舒服"],
    "confidence": 0.6
  }
}
```

**响应格式**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp-123456",
    "question": "您当前的主要目标是健康筛查/体检规划，还是症状咨询/问题排查？",
    "questionType": "single_choice",
    "options": ["健康筛查/体检规划", "症状咨询/问题排查"],
    "requiresAnswer": true
  },
  "timestamp": 1705123456789
}
```

### 7.2 澄清结果处理接口

**接口路径**：`POST /api/v1/entry-assessment/clarification/result`

**请求格式**：
```json
{
  "cdpId": "cdp-123456",
  "question": "您当前的主要目标是健康筛查/体检规划，还是症状咨询/问题排查？",
  "userAnswer": "症状咨询/问题排查"
}
```

**响应格式**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp-123456",
    "direction": "B",
    "confidence": 0.9,
    "reasoning": "用户明确表示是症状咨询",
    "clarified": true
  },
  "timestamp": 1705123456789
}
```

---

## 八、风险评估和应对

### 8.1 技术风险

**风险1：LLM调用失败或超时**
- **影响**：服务不可用
- **应对**：实现降级机制，LLM失败时使用规则判断

**风险2：澄清流程复杂**
- **影响**：用户体验差
- **应对**：简化澄清流程，最多一次澄清

### 8.2 业务风险

**风险1：情况判断准确率不达标**
- **影响**：用户体验差
- **应对**：持续优化判断逻辑，收集错误案例进行迭代

**风险2：澄清问题不够清晰**
- **影响**：用户难以理解
- **应对**：优化Prompt，使用通俗易懂的语言

---

## 九、成功指标

### 9.1 功能指标

- ✅ Step 2情况判断准确率 ≥ 90%
- ✅ Step 3澄清准确率 ≥ 85%
- ✅ 支持复杂症状和困扰识别
- ✅ 澄清问题清晰、个性化

### 9.2 性能指标

- ✅ Step 2处理时间 ≤ 500ms
- ✅ Step 3澄清问题生成时间 ≤ 2秒
- ✅ 系统可用性 ≥ 99%

### 9.3 业务指标

- ✅ 情况判断错误率 ≤ 10%
- ✅ 澄清问题用户理解率 ≥ 90%
- ✅ 用户满意度提升

---

## 十、后续优化方向

### 10.1 短期优化（1-2个月）

1. **Prompt优化**
   - 基于实际使用数据优化Prompt
   - A/B测试不同Prompt版本

2. **上下文理解优化**
   - 支持多轮对话上下文
   - 实现对话状态管理

### 10.2 中期优化（3-6个月）

1. **知识图谱集成**
   - 集成医学知识图谱（UMLS等）
   - 实现症状和困扰的语义理解

2. **个性化优化**
   - 基于用户历史数据优化识别
   - 实现用户画像分析

---

## 十一、附录

### 11.1 参考文档

- 《NLU细化实现方案.md》- 第一步细化方案
- 《health-state-assessment-service - 服务实现方案.md》
- 《health-state-assessment-service - 已实现功能清单.md》

### 11.2 技术栈

- **LLM框架**：LangChain
- **数据验证**：Pydantic
- **配置管理**：pydantic-settings

---

**文档版本**：v1.0  
**创建时间**：2026-01-28  
**最后更新**：2026-01-28

