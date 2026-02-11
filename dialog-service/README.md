# 对话服务 (dialog-service)

对话管理服务，对应tool_2（主动问诊与信息补全服务）。

## 功能概述

1. **信息缺口识别**：识别诊断所需的关键信息缺口
2. **信息缺口分级**：将信息缺口分为必填/重要/可选三级
3. **智能追问生成**：基于信息缺口分级生成智能追问问题
4. **自然语言理解（NLU）**：理解用户的自然语言输入
5. **自然语言生成（NLG）**：生成自然、易懂的追问问题

## 技术栈

- Python 3.10+
- FastAPI 0.104.1
- Redis 5.0.1（对话上下文存储）
- 公共LLM库（aidoctor_llm）- LangChain集成

## 安装

### 1. 安装依赖

```bash
cd dialog-service
pip install -r requirements.txt
```

### 2. 配置环境变量

创建 `.env` 文件：

```bash
# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DB=0
REDIS_PASSWORD=

# LLM配置
LLM_BACKEND=openai
OPENAI_API_KEY=your-api-key
OPENAI_MODEL=gpt-4
LLM_TEMPERATURE=0.3
LLM_MAX_TOKENS=2000
LLM_TIMEOUT=30

# 诊断服务配置
DIAGNOSIS_SERVICE_URL=http://localhost:8080
```

### 3. 启动服务

```bash
python run.py
```

或使用uvicorn：

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8088
```

## API接口

### 1. 生成追问问题

**POST** `/api/v1/dialog/generate-question`

请求体：
```json
{
  "cdpId": "cdp_123456",
  "context": {
    "conversationHistory": [
      {
        "role": "user",
        "content": "我最近胸口闷"
      }
    ]
  }
}
```

响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "question": "您胸痛是在什么情况下出现的？是活动后还是休息时？",
    "questionType": "symptom_detail",
    "missingInfo": [
      {
        "field": "symptom_trigger",
        "level": "required",
        "description": "症状诱因"
      }
    ],
    "completeness": 0.65,
    "informationGaps": {
      "required": [...],
      "important": [...],
      "optional": [...]
    }
  }
}
```

### 2. 理解用户输入

**POST** `/api/v1/dialog/understand`

请求体：
```json
{
  "cdpId": "cdp_123456",
  "userInput": "我活动后就会胸痛，休息一下就好了",
  "context": {}
}
```

响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "extractedInfo": {
      "symptom_trigger": "activity",
      "symptom_relief": "rest",
      "symptom_duration": "recent"
    },
    "confidence": 0.9,
    "updatedFields": ["symptom_trigger", "symptom_relief"]
  }
}
```

### 3. 识别信息缺口

**POST** `/api/v1/dialog/identify-gaps`

请求体：
```json
{
  "cdpId": "cdp_123456"
}
```

响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "informationGaps": {
      "required": [...],
      "important": [...],
      "optional": [...]
    },
    "completeness": 0.65
  }
}
```

### 4. WebSocket实时对话

**WS** `/api/v1/dialog/ws/{cdp_id}`

支持双向通信的实时对话接口。

## 项目结构

```
dialog-service/
├── app/
│   ├── main.py                 # FastAPI应用入口
│   ├── api/
│   │   └── routes.py           # API路由
│   ├── services/
│   │   └── dialog_service.py   # 对话服务
│   ├── core/
│   │   ├── adaptive_questioning.py # 智能追问
│   │   ├── nlu.py              # 自然语言理解
│   │   └── nlg.py              # 自然语言生成
│   ├── identifiers/
│   │   └── information_gap_identifier.py # 信息缺口识别
│   ├── calculators/
│   │   └── completeness_calculator.py # 完整度计算
│   ├── models/
│   │   ├── request.py          # 请求模型
│   │   └── response.py         # 响应模型
│   ├── utils/
│   │   ├── exceptions.py       # 异常处理
│   │   ├── llm_client.py      # LLM客户端（从公共库导入）
│   │   └── prompt_manager.py  # Prompt管理器（从公共库导入）
│   └── config/
│       └── settings.py        # 配置管理
├── requirements.txt
├── Dockerfile
└── README.md
```

## 开发说明

### 使用公共LLM库

本服务使用公共LLM库（`aidoctor_llm`）进行LLM调用：

```python
from app.utils.llm_client import LangChainLLMClient
from app.utils.prompt_manager import PromptTemplateManager

# 初始化
llm_client = LangChainLLMClient()
template_manager = PromptTemplateManager()

# 使用
prompt = template_manager.format_question_generation(
    missing_info="症状诱因",
    context={}
)
result = await llm_client.generate(prompt)
```

### 数据流转

1. 从`diagnosis-service`获取CDP数据
2. 识别信息缺口
3. 生成追问问题
4. 理解用户输入
5. 更新CDP数据

## 测试

```bash
# 运行测试
pytest tests/
```

## 参考文档

- [服务实现方案](./docs/dialog-service%20-%20服务实现方案.md)
- [三个服务LangChain架构开发流程](../../docs/开发过程文件/三个服务LangChain架构开发流程.md)

