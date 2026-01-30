# NLU、意图识别和实体提取细化实现方案

> **文档定位**：本文档详细设计健康状态判定服务中NLU（自然语言理解）、意图识别和实体提取模块的细化实现方案。  
> **目标**：将当前基于关键词匹配的简化实现升级为基于LLM和医疗NER模型的完整NLU系统。

**文档状态**：
- ✅ **架构设计**：已完成
- 📋 **实施计划**：待实施
- 📋 **技术选型**：待确认

---

## 一、现状分析

### 1.1 当前实现（简化版）

**位置**：`app/services/entry_assessment/step1_receive_input.py`

**实现方式**：
- 意图识别：基于关键词匹配（`screening_keywords`、`symptom_keywords`）
- 实体提取：基于正则表达式（`symptom_patterns`）
- 无上下文理解能力
- 无同义词识别能力
- 无实体归一化能力

**局限性**：
1. ❌ **语义理解能力弱**：无法理解同义词和近义表达（如"心口疼"、"心脏部位不适"无法识别为"胸痛"）
2. ❌ **覆盖面有限**：只能识别预设的关键词，无法覆盖用户的多样化表达
3. ❌ **上下文理解缺失**：无法理解症状的时间、程度、频率等上下文信息
4. ❌ **否定语境处理错误**：无法理解否定语境（如"不疼"可能被误识别为"疼"）
5. ❌ **知识库依赖不足**：没有与医学知识图谱（如UMLS）集成

### 1.2 现有资源

**已具备的能力**：
1. ✅ **LLM客户端**：`common/aidoctor_llm` - 支持OpenAI、ChatGLM、Ollama等
2. ✅ **临床解析服务**：`clinical-parsing-service` - 基于词表的医学概念识别和归一化
3. ✅ **医学词表**：`clinical-parsing-service/data/vocabularies/` - 包含症状、疾病、药物等词表

**可复用的组件**：
- `ConceptRecognizer`：医学概念识别器（基于词表）
- `VocabularyLoader`：词表加载器
- `LangChainLLMClient`：LLM客户端封装

---

## 二、设计目标

### 2.1 功能目标

**核心功能**：
1. **自然语言理解（NLU）**
   - 理解用户自然语言输入
   - 支持复杂语法和语义关系
   - 支持上下文理解（时间、程度、频率、否定等）

2. **意图识别**
   - 准确识别用户意图（筛查/诊断/混合/不确定）
   - 支持混合诉求识别
   - 提供意图置信度评估

3. **实体提取**
   - 提取症状实体（支持同义词识别）
   - 提取基本信息（年龄、性别、BMI等）
   - 提取生命体征（血压、心率等）
   - 提取时间信息（症状持续时间、频率等）
   - 提取程度信息（轻度/中度/重度）

4. **实体归一化**
   - 将提取的实体映射到标准医学术语
   - 链接到医学知识库（UMLS、ICD-10等）
   - 同义词扩展和实体归一化

### 2.2 性能目标

- **准确率**：意图识别准确率 ≥ 90%，实体提取准确率 ≥ 85%
- **响应时间**：单次NLU处理时间 ≤ 2秒（含LLM调用）
- **覆盖率**：支持常见医疗表达方式的90%以上
- **可扩展性**：支持新增实体类型和意图类型

### 2.3 技术目标

- **可维护性**：模块化设计，易于扩展和维护
- **可配置性**：支持配置化调整，无需修改代码
- **可降级**：LLM不可用时，可降级到规则匹配
- **可监控**：支持性能监控和错误追踪

---

## 三、技术架构设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    NLU模块架构                                │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  意图识别层   │    │  实体提取层   │    │  实体归一化层  │
│ Intent       │    │ Entity       │    │ Normalization│
│ Recognition  │    │ Extraction   │    │              │
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
│                   知识库服务层                               │
│  - 临床解析服务（clinical-parsing-service）                  │
│  - 医学词表（VocabularyLoader）                             │
│  - 同义词库（SynonymDictionary）                             │
└─────────────────────────────────────────────────────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   规则匹配层（降级方案）                      │
│  - 关键词匹配（KeywordMatcher）                             │
│  - 正则匹配（RegexMatcher）                                 │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 模块设计

#### 3.2.1 意图识别模块（IntentRecognizer）

**职责**：
- 识别用户意图（screening/diagnosis/mixed/unknown）
- 提供意图置信度评估
- 支持混合诉求识别

**实现策略**：
- **主策略**：使用LLM进行意图识别（基于结构化Prompt）
- **降级策略**：关键词匹配（当前实现）

**接口设计**：
```python
class IntentRecognizer:
    async def recognize(self, user_input: str, context: Optional[Dict] = None) -> IntentResult:
        """
        识别用户意图
        
        Args:
            user_input: 用户输入文本
            context: 上下文信息（可选）
        
        Returns:
            IntentResult: 意图识别结果
        """
        pass
```

**输出模型**：
```python
class IntentResult(BaseModel):
    intent: str  # "screening" | "diagnosis" | "mixed" | "unknown"
    confidence: float  # 置信度 0-1
    reasoning: str  # 识别理由
    has_screening_intent: bool
    has_symptom_intent: bool
    details: Dict[str, Any]  # 详细信息
```

#### 3.2.2 实体提取模块（EntityExtractor）

**职责**：
- 提取症状实体
- 提取基本信息（年龄、性别、BMI等）
- 提取生命体征（血压、心率等）
- 提取时间信息（持续时间、频率等）
- 提取程度信息（轻度/中度/重度）

**实现策略**：
- **主策略1**：使用LLM进行结构化信息提取（基于JSON Schema）
- **主策略2**：调用临床解析服务进行医学概念识别
- **降级策略**：正则表达式匹配（当前实现）

**接口设计**：
```python
class EntityExtractor:
    async def extract(self, user_input: str, intent: str) -> EntityResult:
        """
        提取实体
        
        Args:
            user_input: 用户输入文本
            intent: 用户意图
        
        Returns:
            EntityResult: 实体提取结果
        """
        pass
```

**输出模型**：
```python
class EntityResult(BaseModel):
    symptoms: List[SymptomEntity]  # 症状实体列表
    basic_info: Dict[str, Any]  # 基本信息
    vital_signs: Dict[str, Any]  # 生命体征
    temporal_info: Dict[str, Any]  # 时间信息
    severity_info: Dict[str, Any]  # 程度信息
    entities: List[Dict]  # 所有实体（兼容现有格式）

class SymptomEntity(BaseModel):
    original_text: str  # 原始文本
    standard_term: str  # 标准术语
    cui: Optional[str]  # UMLS CUI
    confidence: float  # 置信度
    context: Dict[str, Any]  # 上下文信息（时间、程度等）
```

#### 3.2.3 实体归一化模块（EntityNormalizer）

**职责**：
- 将提取的实体映射到标准医学术语
- 链接到医学知识库（UMLS、ICD-10等）
- 同义词扩展和实体归一化

**实现策略**：
- **主策略**：调用临床解析服务进行概念归一化
- **辅助策略**：本地同义词库扩展

**接口设计**：
```python
class EntityNormalizer:
    async def normalize(self, entities: List[Dict]) -> List[Dict]:
        """
        归一化实体
        
        Args:
            entities: 原始实体列表
        
        Returns:
            归一化后的实体列表（包含CUI、ICD等编码）
        """
        pass
```

---

## 四、分阶段实施计划

### 阶段1：LLM增强的意图识别和实体提取（短期，2-3周）

**目标**：使用LLM提升意图识别和实体提取的准确率

**实施内容**：

1. **集成LLM客户端**
   - 在`health-state-assessment-service`中引入`common/aidoctor_llm`
   - 配置LLM后端（支持OpenAI、ChatGLM、Ollama等）

2. **实现LLM增强的意图识别**
   - 创建`app/services/nlu/intent_recognizer.py`
   - 设计意图识别Prompt模板
   - 实现LLM调用和结果解析
   - 保留关键词匹配作为降级方案

3. **实现LLM增强的实体提取**
   - 创建`app/services/nlu/entity_extractor.py`
   - 设计结构化信息提取Prompt（基于JSON Schema）
   - 实现LLM调用和结果解析
   - 集成临床解析服务作为辅助

4. **重构Step 1**
   - 修改`step1_receive_input.py`，使用新的NLU模块
   - 保持接口兼容性
   - 添加降级机制（LLM失败时使用规则匹配）

**技术选型**：
- LLM：使用现有的`LangChainLLMClient`
- Prompt管理：创建`PromptManager`管理Prompt模板
- 结果解析：使用JSON Schema验证和解析LLM输出

**预期效果**：
- 意图识别准确率提升至85%+
- 实体提取准确率提升至80%+
- 支持复杂表达和混合诉求

### 阶段2：集成临床解析服务（中期，1-2周）

**目标**：充分利用现有的临床解析服务进行实体归一化

**实施内容**：

1. **集成临床解析服务**
   - 创建`app/services/nlu/clinical_parser_client.py`
   - 调用`clinical-parsing-service`的API进行医学概念识别
   - 实现实体归一化（映射到CUI、ICD等编码）

2. **实现实体归一化模块**
   - 创建`app/services/nlu/entity_normalizer.py`
   - 整合LLM提取的实体和临床解析服务的结果
   - 实现同义词扩展和实体去重

3. **优化实体提取流程**
   - LLM提取 → 临床解析服务归一化 → 实体合并
   - 提升实体提取的准确率和覆盖率

**技术选型**：
- HTTP客户端：使用`httpx`调用临床解析服务
- 实体合并：基于CUI进行实体去重和合并

**预期效果**：
- 实体归一化准确率提升至90%+
- 支持同义词识别和实体链接
- 实体提取结果包含标准编码（CUI、ICD等）

### 阶段3：医疗NER模型集成（长期，3-4周）

**目标**：集成医疗领域NER模型，进一步提升实体提取准确率

**实施内容**：

1. **评估和选择NER模型**
   - 评估中文医疗NER模型（医疗BERT、BioBERT等）
   - 选择适合的模型（考虑准确率、性能、资源消耗）

2. **实现NER模型服务**
   - 创建`app/services/nlu/ner_model.py`
   - 集成选定的NER模型
   - 实现模型推理接口

3. **融合多种实体提取方法**
   - LLM提取 + NER模型提取 + 临床解析服务
   - 实现多源结果融合和置信度评估

4. **性能优化**
   - 模型量化、缓存优化
   - 批量处理优化

**技术选型**：
- NER模型：中文医疗BERT（如`bert-base-chinese` + 医疗语料微调）
- 模型框架：Transformers（Hugging Face）
- 推理加速：ONNX Runtime（可选）

**预期效果**：
- 实体提取准确率提升至90%+
- 支持更复杂的医疗实体识别
- 降低对LLM的依赖（降低成本）

---

## 五、详细设计

### 5.1 意图识别Prompt设计

**Prompt模板**：
```
你是一个医疗AI助手，需要识别用户的医疗咨询意图。

用户输入：{user_input}

请分析用户的意图，判断用户是想要：
1. **健康筛查**（screening）：用户想要进行体检、健康评估、预防性检查等，没有明确的症状描述
2. **症状诊断**（diagnosis）：用户有明确的症状、不适或困扰，想要了解可能的原因或诊断
3. **混合诉求**（mixed）：用户既有筛查需求，又有轻微症状描述
4. **不确定**（unknown）：无法明确判断用户意图

请以JSON格式返回结果：
{{
    "intent": "screening|diagnosis|mixed|unknown",
    "confidence": 0.0-1.0,
    "reasoning": "识别理由",
    "has_screening_intent": true/false,
    "has_symptom_intent": true/false,
    "screening_keywords": ["关键词列表"],
    "symptom_keywords": ["关键词列表"]
}}
```

### 5.2 实体提取Prompt设计

**Prompt模板**：
```
你是一个医疗AI助手，需要从用户输入中提取结构化的医疗信息。

用户输入：{user_input}
用户意图：{intent}

请提取以下信息：

1. **症状列表**：识别用户描述的所有症状，包括：
   - 症状名称（使用标准医学术语）
   - 症状位置（如：胸部、腹部、头部等）
   - 症状程度（轻度/中度/重度）
   - 症状持续时间（如：3天、1周等）
   - 症状频率（如：持续、间歇、偶尔等）

2. **基本信息**（如果提及）：
   - 年龄
   - 性别
   - BMI或体重信息

3. **生命体征**（如果提及）：
   - 血压（收缩压/舒张压）
   - 心率
   - 体温
   - 其他生命体征

4. **时间信息**：
   - 症状开始时间
   - 症状持续时间
   - 症状频率

请以JSON格式返回结果：
{{
    "symptoms": [
        {{
            "original_text": "原始文本",
            "standard_term": "标准术语",
            "location": "位置",
            "severity": "轻度|中度|重度",
            "duration": "持续时间",
            "frequency": "频率"
        }}
    ],
    "basic_info": {{
        "age": 年龄或null,
        "gender": "性别"或null,
        "bmi": BMI或null
    }},
    "vital_signs": {{
        "bp": {{"systolic": 收缩压, "diastolic": 舒张压}}或null,
        "heart_rate": 心率或null,
        "temperature": 体温或null
    }},
    "temporal_info": {{
        "start_time": "开始时间",
        "duration": "持续时间",
        "frequency": "频率"
    }}
}}
```

### 5.3 代码结构设计

```
health-state-assessment-service/
├── app/
│   ├── services/
│   │   ├── nlu/                          # 新增：NLU模块
│   │   │   ├── __init__.py
│   │   │   ├── intent_recognizer.py      # 意图识别
│   │   │   ├── entity_extractor.py      # 实体提取
│   │   │   ├── entity_normalizer.py     # 实体归一化
│   │   │   ├── clinical_parser_client.py # 临床解析服务客户端
│   │   │   ├── prompt_manager.py         # Prompt管理
│   │   │   ├── response_parser.py        # 结果解析
│   │   │   └── fallback_matcher.py       # 降级匹配器
│   │   │
│   │   └── entry_assessment/
│   │       └── step1_receive_input.py    # 重构：使用NLU模块
│   │
│   ├── config/
│   │   └── settings.py                   # 新增：NLU配置
│   │
│   └── models/
│       └── nlu.py                        # 新增：NLU数据模型
```

### 5.4 配置设计

**配置文件**：`app/config/settings.py`

```python
class NLUConfig(BaseSettings):
    """NLU配置"""
    # LLM配置
    use_llm: bool = True  # 是否使用LLM
    llm_backend: str = "openai"  # LLM后端
    llm_model: str = "gpt-4"  # LLM模型
    llm_temperature: float = 0.3  # LLM温度
    llm_timeout: int = 30  # LLM超时时间（秒）
    
    # 临床解析服务配置
    use_clinical_parser: bool = True  # 是否使用临床解析服务
    clinical_parser_url: str = "http://clinical-parsing-service:8000"  # 临床解析服务URL
    clinical_parser_timeout: int = 10  # 超时时间（秒）
    
    # 降级配置
    enable_fallback: bool = True  # 是否启用降级方案
    fallback_threshold: float = 0.5  # 降级阈值（置信度低于此值时使用降级方案）
    
    class Config:
        env_file = ".env"
        env_prefix = "NLU_"
```

---

## 六、实施步骤

### 步骤1：环境准备（1天）

1. **添加依赖**
   - 在`requirements.txt`中添加`aidoctor-llm`依赖（如果未添加）
   - 安装依赖：`pip install -r requirements.txt`

2. **配置LLM**
   - 在`.env`文件中配置LLM相关环境变量
   - 测试LLM连接

### 步骤2：创建NLU模块基础结构（2天）

1. **创建目录结构**
   ```bash
   mkdir -p app/services/nlu
   ```

2. **创建基础文件**
   - `app/services/nlu/__init__.py`
   - `app/services/nlu/prompt_manager.py`
   - `app/services/nlu/response_parser.py`

3. **创建数据模型**
   - `app/models/nlu.py`

### 步骤3：实现意图识别模块（3天）

1. **实现PromptManager**
   - 管理意图识别Prompt模板
   - 支持模板变量替换

2. **实现IntentRecognizer**
   - 集成LLM客户端
   - 实现意图识别逻辑
   - 实现降级方案（关键词匹配）

3. **单元测试**
   - 编写测试用例
   - 验证意图识别准确率

### 步骤4：实现实体提取模块（4天）

1. **实现EntityExtractor**
   - 集成LLM客户端
   - 实现结构化信息提取
   - 实现结果解析和验证

2. **集成临床解析服务**
   - 创建`clinical_parser_client.py`
   - 实现HTTP客户端调用
   - 实现结果合并逻辑

3. **单元测试**
   - 编写测试用例
   - 验证实体提取准确率

### 步骤5：重构Step 1（2天）

1. **修改step1_receive_input.py**
   - 集成新的NLU模块
   - 保持接口兼容性
   - 添加错误处理和降级机制

2. **集成测试**
   - 测试完整的入口判定流程
   - 验证与现有系统的兼容性

### 步骤6：优化和文档（2天）

1. **性能优化**
   - 添加缓存机制
   - 优化LLM调用频率

2. **文档更新**
   - 更新API文档
   - 更新架构文档
   - 编写使用指南

---

## 七、风险评估和应对

### 7.1 技术风险

**风险1：LLM调用失败或超时**
- **影响**：服务不可用
- **应对**：实现降级机制，LLM失败时使用规则匹配

**风险2：LLM输出格式不稳定**
- **影响**：结果解析失败
- **应对**：使用JSON Schema验证，实现容错解析

**风险3：LLM成本过高**
- **影响**：运营成本增加
- **应对**：实现缓存机制，减少重复调用；考虑使用更便宜的模型

### 7.2 性能风险

**风险1：LLM调用延迟高**
- **影响**：响应时间增加
- **应对**：设置合理的超时时间；实现异步调用；考虑使用更快的模型

**风险2：并发处理能力不足**
- **影响**：高并发时性能下降
- **应对**：实现请求队列；考虑使用LLM服务池

### 7.3 业务风险

**风险1：准确率不达标**
- **影响**：用户体验差
- **应对**：持续优化Prompt；收集错误案例进行迭代；考虑集成NER模型

**风险2：与现有系统不兼容**
- **影响**：系统集成问题
- **应对**：保持接口兼容性；充分测试；渐进式迁移

---

## 八、成功指标

### 8.1 功能指标

- ✅ 意图识别准确率 ≥ 90%
- ✅ 实体提取准确率 ≥ 85%
- ✅ 实体归一化准确率 ≥ 90%
- ✅ 支持混合诉求识别

### 8.2 性能指标

- ✅ 单次NLU处理时间 ≤ 2秒（含LLM调用）
- ✅ 降级方案响应时间 ≤ 500ms
- ✅ 系统可用性 ≥ 99%

### 8.3 业务指标

- ✅ 用户意图识别错误率 ≤ 10%
- ✅ 实体提取遗漏率 ≤ 15%
- ✅ 用户满意度提升

---

## 九、后续优化方向

### 9.1 短期优化（1-2个月）

1. **Prompt优化**
   - 基于实际使用数据优化Prompt
   - A/B测试不同Prompt版本

2. **缓存优化**
   - 实现智能缓存（基于用户输入相似度）
   - 减少重复LLM调用

3. **错误处理优化**
   - 完善错误处理和重试机制
   - 实现错误日志分析

### 9.2 中期优化（3-6个月）

1. **NER模型集成**
   - 评估和集成医疗NER模型
   - 实现多源结果融合

2. **上下文理解**
   - 支持多轮对话上下文
   - 实现对话状态管理

3. **个性化优化**
   - 基于用户历史数据优化识别
   - 实现用户画像分析

### 9.3 长期优化（6-12个月）

1. **知识图谱集成**
   - 集成医学知识图谱（UMLS等）
   - 实现实体链接和推理

2. **模型微调**
   - 基于实际数据微调模型
   - 实现持续学习机制

3. **多模态支持**
   - 支持图像输入（如检查报告图片）
   - 支持语音输入

---

## 十、附录

### 10.1 参考文档

- 《健康判定服务 - 架构与逻辑整理.md》
- 《health-state-assessment-service - 服务实现方案.md》
- 《clinical-parsing-service - 服务实现方案.md》
- 《AI医生系统-技术架构设计.md》

### 10.2 技术栈

- **LLM框架**：LangChain
- **HTTP客户端**：httpx
- **数据验证**：Pydantic
- **配置管理**：pydantic-settings

### 10.3 相关服务

- **临床解析服务**：`clinical-parsing-service`
- **LLM客户端**：`common/aidoctor_llm`

---

**文档版本**：v1.0  
**创建时间**：2025-01-28  
**最后更新**：2025-01-28

