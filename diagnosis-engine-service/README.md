# 诊断引擎服务 (diagnosis-engine-service)

## 服务概述

诊断引擎服务是AI医生系统的核心服务之一，对应**tool_3（鉴别诊断引擎）**，负责实现基于知识图谱的医学推理和多引擎融合诊断。

## 主要功能

1. **知识图谱推理引擎（DR.KNOWS方法）**
   - 多跳路径检索（2-4跳）
   - 三层评分体系（先验概率、似然、后验概率）
   - 路径注入LLM

2. **五引擎融合诊断**
   - 规则引擎
   - 知识图谱引擎
   - 统计模型引擎
   - 大模型引擎（LLM）
   - 鉴别诊断引擎

3. **三层分层分类器**
   - 首要假设（1个）
   - 主要备选（1-2个）
   - 必须排除（0-1个）

4. **推理组织和证据分析**
   - 推理子组组织
   - 证据强度评估
   - 证据链构建

## 技术栈

- **框架**: FastAPI 0.104.1
- **数据库**: Neo4j 5.14.0（知识图谱）
- **LLM**: LangChain + OpenAI/ChatGLM/Ollama（通过公共库`aidoctor_llm`）
- **机器学习**: scikit-learn 1.3.2
- **其他**: Pydantic 2.5.0, Jinja2 3.1.2

## 项目结构

```
diagnosis-engine-service/
├── app/
│   ├── main.py                 # FastAPI应用入口
│   ├── api/
│   │   └── routes.py           # API路由
│   ├── services/
│   │   └── diagnosis_service.py # 诊断服务
│   ├── engines/                # 五个诊断引擎
│   │   ├── rule_engine.py
│   │   ├── kg_engine.py
│   │   ├── statistical_engine.py
│   │   ├── llm_engine.py
│   │   ├── differential_engine.py
│   │   └── fusion_engine.py
│   ├── kg-reasoning-engine/    # 知识图谱推理引擎
│   │   ├── kg_client.py
│   │   ├── path_retriever.py
│   │   ├── path_scorer.py
│   │   ├── path_injector.py
│   │   └── kg_reasoning_engine.py
│   ├── classifiers/            # 分类器
│   │   └── three_layer_classifier.py
│   ├── analyzers/               # 分析器
│   │   ├── reasoning_organizer.py
│   │   └── evidence_analyzer.py
│   ├── models/                  # 数据模型
│   │   ├── request.py
│   │   └── response.py
│   ├── config/                  # 配置
│   │   ├── settings.py
│   │   └── prompt_templates/   # Prompt模板
│   └── utils/                   # 工具类
│       ├── exceptions.py
│       ├── llm_client.py        # LLM客户端（从公共库导入）
│       └── prompt_templates.py  # Prompt管理器（从公共库导入）
├── requirements.txt
├── Dockerfile
└── README.md
```

## 安装和运行

### 1. 安装依赖

```bash
# 安装服务依赖
pip install -r requirements.txt

# 安装公共LLM库（开发模式）
pip install -e ../common/aidoctor_llm
```

### 2. 配置环境变量

创建`.env`文件：

```env
# Neo4j配置
NEO4J_URI=bolt://localhost:7687
NEO4J_USER=neo4j
NEO4J_PASSWORD=your_password

# LLM配置
LLM_BACKEND=openai
LLM_MODEL=gpt-4
OPENAI_API_KEY=your_api_key

# 服务配置
DEBUG=False
LOG_LEVEL=INFO
```

### 3. 运行服务

```bash
# 开发模式
uvicorn app.main:app --reload --port 8086

# 生产模式
uvicorn app.main:app --host 0.0.0.0 --port 8086
```

## API接口

### 1. 五引擎融合诊断（完整流程）

```http
POST /api/v1/engine/diagnose
Content-Type: application/json

{
  "symptom_info": {
    "symptoms": [
      {"cui": "C0018681", "name": "胸痛"}
    ]
  },
  "vital_signs": {
    "bloodPressure": "120/80",
    "heartRate": 72
  },
  "examination_results": [],
  "health_profile": {
    "age": 45,
    "gender": "male"
  }
}
```

### 2. 单独引擎诊断

- `POST /api/v1/engine/rule-based` - 规则引擎
- `POST /api/v1/engine/knowledge-graph` - 知识图谱引擎
- `POST /api/v1/engine/statistical` - 统计模型引擎
- `POST /api/v1/engine/llm` - 大模型引擎
- `POST /api/v1/engine/differential` - 鉴别诊断引擎

### 3. 三层分层分类

```http
POST /api/v1/classify/three-layer
```

### 4. 知识图谱路径检索

```http
POST /api/v1/kg/paths/retrieve
```

## 开发说明

### 公共LLM库集成

本服务使用公共LLM库（`aidoctor_llm`）进行LLM调用和Prompt管理：

- `app/utils/llm_client.py` - 从公共库导入`LangChainLLMClient`
- `app/utils/prompt_templates.py` - 从公共库导入`PromptTemplateManager`，并扩展支持服务特定模板目录

### 知识图谱推理引擎

知识图谱推理引擎实现了DR.KNOWS核心方法：

1. **路径检索**: 使用Neo4j进行多跳图遍历
2. **路径评分**: 三层评分体系（先验、似然、后验）
3. **路径注入**: 将推理路径注入LLM Prompt

### 多引擎融合

融合引擎并行执行五个子引擎，然后使用加权融合算法合并结果。权重可在配置文件中调整。

## 测试

```bash
# 运行单元测试
pytest tests/

# 运行集成测试
pytest tests/integration/
```

## 部署

### Docker部署

```bash
docker build -t diagnosis-engine-service .
docker run -p 8086:8086 --env-file .env diagnosis-engine-service
```

## 参考文档

- [服务实现方案](./docs/diagnosis-engine-service%20-%20服务实现方案.md)
- [三个服务LangChain架构开发流程](../../docs/开发过程文件/三个服务LangChain架构开发流程.md)

## 许可证

[项目许可证]

