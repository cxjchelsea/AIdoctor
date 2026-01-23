# AI医生系统LLM公共库

## 简介

本库提供统一的LangChain客户端和Prompt模板管理，供以下服务使用：
- diagnosis-engine-service
- explanation-service
- dialog-service

## 安装

### 开发模式安装

```bash
cd common/aidoctor_llm
pip install -e .
```

### 在服务中使用

在服务的`requirements.txt`中添加：

```txt
-e ../common/aidoctor_llm
```

然后安装：

```bash
pip install -r requirements.txt
```

## 使用示例

### 基本使用

```python
from aidoctor_llm import LangChainLLMClient, PromptTemplateManager

# 初始化LLM客户端（自动从环境变量读取配置）
llm_client = LangChainLLMClient()

# 初始化模板管理器
template_manager = PromptTemplateManager()

# 生成文本
result = await llm_client.generate("你好")

# 使用模板
prompt = template_manager.format_diagnosis_reasoning(
    symptoms=["发热", "咳嗽"],
    signs={"体温": "38.5℃"},
    context={"年龄": 35}
)
result = await llm_client.generate(prompt)
```

### 自定义配置

```python
from aidoctor_llm import LangChainLLMClient, LLMConfig, LLMBackend

# 创建自定义配置
config = LLMConfig(
    backend=LLMBackend.OPENAI,
    model="gpt-4",
    temperature=0.3,
    max_tokens=2000,
    openai_api_key="your-api-key"
)

# 使用自定义配置初始化
llm_client = LangChainLLMClient(config=config)
```

## 配置

通过环境变量配置：

- `LLM_BACKEND`: LLM后端类型（openai/ollama/chatglm/custom）
- `OPENAI_API_KEY`: OpenAI API密钥
- `OPENAI_MODEL`: OpenAI模型名称（默认：gpt-4）
- `LLM_TEMPERATURE`: 温度参数（默认：0.3）
- `LLM_MAX_TOKENS`: 最大token数（默认：2000）
- `LLM_TIMEOUT`: 超时时间（秒，默认：30）
- `LLM_MAX_RETRIES`: 最大重试次数（默认：3）

## 模板管理

### 使用内置模板

```python
# 诊断推理模板
prompt = template_manager.format_diagnosis_reasoning(
    symptoms=["胸痛", "气促"],
    signs={"血压": "140/90"},
    context={"年龄": 50}
)

# 问诊问题生成模板
prompt = template_manager.format_question_generation(
    missing_info="症状诱因",
    context={"已收集信息": "胸痛"}
)

# 解释生成模板
prompt = template_manager.format_explanation_generation(
    diagnosis={"disease": "心绞痛", "confidence": 0.85},
    evidence=["胸痛", "活动后加重"],
    reasoning_path="症状：胸痛 → 疾病：心绞痛"
)
```

### 自定义模板

在`templates`目录下创建`.jinja2`文件，例如`custom_template.jinja2`：

```jinja2
这是一个自定义模板：{{ variable }}
```

使用：

```python
prompt = template_manager.format("custom_template", variable="值")
```

## 异常处理

```python
from aidoctor_llm import LLMException, LLMTimeoutException, LLMAPIException

try:
    result = await llm_client.generate(prompt)
except LLMTimeoutException as e:
    print(f"超时: {e.message}")
except LLMAPIException as e:
    print(f"API错误: {e.message}")
except LLMException as e:
    print(f"LLM错误: {e.message}")
```

## 更多文档

详见各服务的使用文档和《三个服务LangChain架构开发流程.md》。

