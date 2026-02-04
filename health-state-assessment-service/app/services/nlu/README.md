# NLU模块使用说明

## 概述

NLU模块提供了自然语言理解、意图识别和实体提取功能，用于替代原有的基于关键词匹配的简化实现。

## 功能特性

1. **意图识别**：识别用户意图（screening/diagnosis/mixed/unknown）
2. **实体提取**：提取症状、基本信息、生命体征等实体
3. **降级机制**：LLM不可用时自动降级到关键词匹配
4. **配置化**：支持通过配置文件控制LLM使用和降级策略

## 配置说明

在`.env`文件中配置以下参数：

```env
# LLM配置
NLU_USE_LLM=true
NLU_LLM_BACKEND=ollama  # openai/chatglm/ollama/custom
NLU_LLM_MODEL=qwen2.5:7b
NLU_LLM_TEMPERATURE=0.3
NLU_LLM_TIMEOUT=30

# Ollama配置（如果使用Ollama）
NLU_OLLAMA_BASE_URL=http://localhost:11434
NLU_OLLAMA_MODEL=qwen2.5:7b

# OpenAI配置（如果使用OpenAI）
NLU_OPENAI_API_KEY=your_api_key
NLU_OPENAI_BASE_URL=https://api.openai.com/v1

# 降级配置
NLU_ENABLE_FALLBACK=true
NLU_FALLBACK_THRESHOLD=0.5
```

## 使用示例

### 同步使用（推荐）

```python
from app.services.entry_assessment.step1_receive_input import receive_user_input

user_input = {
    "userInput": "我最近胸痛，想做个检查",
    "basicInfo": {"age": 45, "gender": "male"},
    "symptoms": []
}

result = receive_user_input(user_input)
print(result["intent"])  # "mixed"
print(result["entities"])  # [{"type": "symptom", "value": "胸痛", ...}]
```

### 异步使用

```python
from app.services.entry_assessment.step1_receive_input import receive_user_input_async

user_input = {
    "userInput": "我最近胸痛，想做个检查",
    "basicInfo": {"age": 45, "gender": "male"},
    "symptoms": []
}

result = await receive_user_input_async(user_input)
```

### 直接使用NLU模块

```python
from app.services.nlu import IntentRecognizer, EntityExtractor

# 意图识别
intent_recognizer = IntentRecognizer()
intent_result = await intent_recognizer.recognize("我最近胸痛")

# 实体提取
entity_extractor = EntityExtractor()
entity_result = await entity_extractor.extract(
    "我最近胸痛", 
    intent_result.intent
)
```

## 降级机制

当LLM不可用或置信度低于阈值时，系统会自动降级到基于关键词匹配的方案：

1. **意图识别降级**：使用关键词匹配识别意图
2. **实体提取降级**：使用正则表达式提取实体

降级方案保证系统在LLM不可用时仍能正常工作，但准确率会有所下降。

## 模块结构

```
app/services/nlu/
├── __init__.py              # 模块导出
├── intent_recognizer.py     # 意图识别器
├── entity_extractor.py      # 实体提取器
├── prompt_manager.py         # Prompt管理器
├── response_parser.py       # 响应解析器
└── fallback_matcher.py       # 降级匹配器
```

## 注意事项

1. **LLM配置**：确保LLM服务可用，否则会自动降级
2. **性能考虑**：LLM调用有延迟，建议在异步环境中使用
3. **错误处理**：所有错误都会被捕获并降级到关键词匹配
4. **向后兼容**：保持与原有接口的兼容性

## 阶段2：集成临床解析服务（已完成）

### 功能说明

已集成临床解析服务（`clinical-parsing-service`）进行实体归一化：

1. **实体归一化**：将LLM提取的实体映射到标准医学术语
2. **编码链接**：为实体添加CUI、ICD等标准编码
3. **同义词识别**：识别同义词并归一化到标准术语
4. **实体去重**：基于CUI或标准术语进行实体去重

### 配置要求

确保临床解析服务可用，并在`.env`中配置：

```env
# 临床解析服务配置
NLU_USE_CLINICAL_PARSER=true
NLU_CLINICAL_PARSER_URL=http://localhost:8001
NLU_CLINICAL_PARSER_TIMEOUT=10
```

### 使用说明

实体归一化会自动在实体提取过程中执行，无需额外调用。提取的实体会自动包含：
- `standard_term`：标准医学术语
- `cui`：UMLS CUI编码（如果可用）
- `confidence`：置信度（LLM和临床解析服务的较高值）

## 阶段3：医疗NER模型集成（已完成）

### 功能说明

已集成医疗NER模型进行实体提取，支持多源结果融合：

1. **多源实体提取**：
   - LLM提取（语义理解能力强）
   - NER模型提取（基础实体识别）
   - 临床解析服务提取（标准编码）

2. **多源结果融合**：
   - 加权投票融合（默认）：临床解析服务0.5，LLM 0.3，NER 0.2
   - 多数投票融合：只保留被多个源识别的实体
   - 最大置信度融合：选择置信度最高的版本

3. **自动降级**：
   - NER模型不可用时自动使用简单NER模型
   - 所有方法失败时降级到关键词匹配

### 配置要求

在`.env`文件中配置：

```env
# NER模型配置（可选）
NLU_USE_NER=false  # 是否使用NER模型（默认false，需要安装transformers和torch）
NLU_NER_MODEL_NAME=bert-base-chinese  # NER模型名称或路径
NLU_NER_DEVICE=cpu  # 设备（cpu/cuda），如果为None则自动选择
NLU_USE_ONNX=false  # 是否使用ONNX Runtime加速

# 融合策略配置
NLU_FUSION_STRATEGY=weighted_vote  # weighted_vote/majority_vote/max_confidence
```

### 安装依赖（可选）

如果需要使用NER模型功能，需要安装：

```bash
pip install transformers>=4.35.0 torch>=2.0.0
```

### 使用说明

NER模型集成是可选功能，默认关闭。如果启用：
1. 系统会自动尝试加载配置的NER模型
2. 如果模型不可用，会自动降级到简单NER模型
3. 多源结果会自动融合，提升准确率

### 融合策略说明

- **weighted_vote**（默认）：加权投票，考虑不同源的可靠性
- **majority_vote**：多数投票，只保留被多个源识别的实体
- **max_confidence**：最大置信度，选择置信度最高的版本

## 后续优化

- [x] 集成临床解析服务进行实体归一化（已完成）
- [x] 集成医疗NER模型提升准确率（已完成）
- [ ] 添加缓存机制减少LLM调用
- [ ] 支持多轮对话上下文
- [ ] 模型量化和性能优化

