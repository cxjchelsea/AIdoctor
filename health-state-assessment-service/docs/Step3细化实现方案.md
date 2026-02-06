# Step 3细化实现方案

> **文档定位**：本文档详细设计健康状态判定服务中Step 3（方向澄清）的细化实现方案。  
> **目标**：将当前基于自动推断的简化实现升级为基于LLM的交互式澄清流程。

**前置依赖**：
- ✅ **Step 1**：NLU细化实现（已完成）
- ✅ **Step 2**：症状/困扰识别细化实现（需先完成）

**文档状态**：
- ✅ **架构设计**：已完成
- 📋 **实施计划**：待实施（需等待Step 2完成）
- 📋 **技术选型**：待确认

---

## 一、现状分析

### 1.1 Step 3当前实现（简化版）

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

### 1.2 触发条件

Step 3仅在以下情况触发：
- Step 2判断结果为"情况C"（不确定/模糊/混合诉求）
- 需要澄清用户主要目标是：
  - **A**：健康筛查/体检规划
  - **B**：症状咨询/问题排查

---

## 二、设计目标

### 2.1 Step 3细化目标

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

### 2.2 性能目标

- **准确率**：澄清准确率 ≥ 85%
- **响应时间**：澄清问题生成时间 ≤ 2秒
- **用户体验**：澄清问题清晰、个性化，用户易于理解

---

## 三、技术架构设计

### 3.1 Step 3细化架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Step 3 细化架构                           │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ 澄清问题生成器 │    │ 澄清结果处理器 │    │  澄清管理器   │
│ Question     │    │ Result       │    │ Clarification│
│ Generator    │    │ Processor    │    │ Manager      │
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
│                    Step 2结果集成层                           │
│  - Step 2的症状识别结果                                      │
│  - Step 1的NLU结果（意图、实体、上下文）                     │
│  - 上下文信息                                                │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Step 3细化模块设计

#### 3.2.1 澄清问题生成器（ClarificationQuestionGenerator）

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

#### 3.2.2 澄清结果处理器（ClarificationResultProcessor）

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

#### 3.2.3 澄清管理器（ClarificationManager）

**职责**：
- 管理澄清流程的完整生命周期
- 协调问题生成和结果处理
- 管理澄清状态

**接口设计**：
```python
class ClarificationManager:
    async def start_clarification(
        self,
        symptom_result: SymptomIdentificationResult,
        nlu_result: Dict[str, Any]
    ) -> ClarificationQuestion:
        """
        启动澄清流程
        
        Args:
            symptom_result: Step 2的症状识别结果
            nlu_result: Step 1的NLU结果
        
        Returns:
            ClarificationQuestion: 澄清问题
        """
        pass
    
    async def process_answer(
        self,
        question: ClarificationQuestion,
        user_answer: str
    ) -> ClarificationResult:
        """
        处理用户回答
        
        Args:
            question: 澄清问题
            user_answer: 用户回答
        
        Returns:
            ClarificationResult: 澄清结果
        """
        pass
```

---

## 四、详细设计

### 4.1 Step 3细化Prompt设计

#### 4.1.1 澄清问题生成Prompt模板

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

#### 4.1.2 澄清结果处理Prompt模板

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

## 五、实施计划

### 阶段1：Step 3细化实现（中期，1-2周）

**前置条件**：
- ✅ Step 2细化实现已完成
- ✅ Step 2接口稳定可用

**目标**：实现真正的交互式澄清流程

**实施内容**：

1. **创建Step 3细化模块**
   - 创建`app/services/entry_assessment/step3_enhanced/`目录
   - 实现`ClarificationQuestionGenerator`类
   - 实现`ClarificationResultProcessor`类
   - 实现`ClarificationManager`类

2. **实现澄清问题生成**
   - 使用LLM生成个性化的澄清问题
   - 基于上下文信息生成问题
   - 支持多种问题类型（单选、多选、开放）
   - 实现降级机制（LLM失败时使用模板）

3. **实现异步澄清流程**
   - 修改API接口，支持返回澄清问题
   - 实现澄清结果处理接口
   - 前端集成澄清流程

4. **重构Step 3**
   - 修改`step3_clarification.py`，使用新的细化模块
   - 支持异步澄清流程
   - 添加降级机制（LLM失败时使用模板）

5. **测试和验证**
   - 单元测试：测试各个模块功能
   - 集成测试：测试与Step 2的集成
   - 性能测试：验证响应时间
   - 准确率测试：验证澄清准确率

**预期效果**：
- 澄清问题个性化、清晰
- 支持异步澄清流程
- 澄清准确率提升至85%+
- 澄清问题生成时间 ≤ 2秒

---

## 六、代码结构设计

```
health-state-assessment-service/
├── app/
│   ├── services/
│   │   ├── entry_assessment/
│   │   │   ├── step1_receive_input.py      # Step 1（已完成细化）
│   │   │   ├── step2_identify_symptom.py   # Step 2（已完成细化）
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
- **应对**：实现降级机制，LLM失败时使用模板生成问题

**风险2：澄清流程复杂**
- **影响**：用户体验差
- **应对**：简化澄清流程，最多一次澄清

**风险3：前端集成复杂**
- **影响**：开发周期延长
- **应对**：提前与前端团队沟通，明确接口规范

### 8.2 业务风险

**风险1：澄清问题不够清晰**
- **影响**：用户难以理解
- **应对**：优化Prompt，使用通俗易懂的语言

**风险2：澄清准确率不达标**
- **影响**：用户体验差
- **应对**：持续优化Prompt，收集错误案例进行迭代

---

## 九、成功指标

### 9.1 功能指标

- ✅ 澄清准确率 ≥ 85%
- ✅ 澄清问题清晰、个性化
- ✅ 支持异步澄清流程

### 9.2 性能指标

- ✅ 澄清问题生成时间 ≤ 2秒
- ✅ 系统可用性 ≥ 99%

### 9.3 业务指标

- ✅ 澄清问题用户理解率 ≥ 90%
- ✅ 用户满意度提升

---

## 十、后续优化方向

### 10.1 短期优化（1-2个月）

1. **Prompt优化**
   - 基于实际使用数据优化Prompt
   - A/B测试不同Prompt版本

2. **澄清问题类型优化**
   - 支持更多问题类型（多选、开放等）
   - 根据情况选择最适合的问题类型

### 10.2 中期优化（3-6个月）

1. **多轮澄清支持**
   - 支持多轮澄清（如果需要）
   - 实现澄清历史管理

2. **个性化优化**
   - 基于用户历史数据优化澄清问题
   - 实现用户画像分析

---

## 十一、附录

### 11.1 参考文档

- 《NLU细化实现方案.md》- 第一步细化方案
- 《Step2细化实现方案.md》- Step 2细化方案（前置依赖）
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

