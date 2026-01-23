# LLM统一配置说明

## 配置位置

LLM配置通过**环境变量**统一管理，所有使用LLM的服务（`diagnosis-engine-service`、`explanation-service`、`dialog-service`）都从环境变量读取配置。

## 配置方式

### 方式1：项目根目录的 `.env` 文件（推荐）

在项目根目录创建 `.env` 文件，所有服务共享同一份配置：

```bash
# 项目根目录/.env

# ============================================
# LLM统一配置（所有服务共享）
# ============================================

# LLM后端类型：openai, chatglm, ollama, custom
LLM_BACKEND=openai

# ============================================
# OpenAI配置（如果使用OpenAI）
# ============================================
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4
OPENAI_TEMPERATURE=0.3
OPENAI_MAX_TOKENS=2000
# OPENAI_BASE_URL=https://api.openai.com/v1  # 可选，自定义API端点

# ============================================
# ChatGLM配置（如果使用ChatGLM）
# ============================================
# CHATGLM_API_URL=http://chatglm-service:8000
# CHATGLM_API_KEY=your_chatglm_key
# LLM_MODEL=chatglm3-6b

# ============================================
# Ollama配置（如果使用本地模型）
# ============================================
# OLLAMA_BASE_URL=http://localhost:11434
# OLLAMA_MODEL=llama2
# LLM_MODEL=llama2

# ============================================
# 自定义HTTP API配置（如果使用自定义LLM服务）
# ============================================
# CUSTOM_LLM_API_URL=http://llm-service:8080/api/generate
# CUSTOM_LLM_API_KEY=your_custom_key
# LLM_MODEL=medical-llm

# ============================================
# 通用LLM配置
# ============================================
LLM_TIMEOUT=30
LLM_MAX_RETRIES=3
LLM_RETRY_DELAY=1.0
```

### 方式2：系统环境变量

在服务器上设置系统环境变量：

```bash
export LLM_BACKEND=openai
export OPENAI_API_KEY=your_openai_api_key_here
export OPENAI_MODEL=gpt-4
export OPENAI_TEMPERATURE=0.3
export OPENAI_MAX_TOKENS=2000
export LLM_TIMEOUT=30
export LLM_MAX_RETRIES=3
export LLM_RETRY_DELAY=1.0
```

### 方式3：Docker环境变量

在Docker Compose或Docker运行命令中设置：

```yaml
# docker-compose.yml
services:
  diagnosis-engine-service:
    environment:
      - LLM_BACKEND=openai
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - OPENAI_MODEL=gpt-4
      - OPENAI_TEMPERATURE=0.3
      - OPENAI_MAX_TOKENS=2000
      - LLM_TIMEOUT=30
      - LLM_MAX_RETRIES=3
```

或使用 `.env` 文件：

```bash
docker run --env-file .env diagnosis-engine-service
```

## 环境变量说明

### 必需配置

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `LLM_BACKEND` | LLM后端类型 | `openai`, `chatglm`, `ollama`, `custom` |
| `OPENAI_API_KEY` | OpenAI API密钥（使用OpenAI时必需） | `sk-...` |

### OpenAI配置

| 变量名 | 说明 | 默认值 | 必需 |
|--------|------|--------|------|
| `OPENAI_API_KEY` | OpenAI API密钥 | - | 是（使用OpenAI时） |
| `OPENAI_MODEL` 或 `LLM_MODEL` | 模型名称 | `gpt-4` | 否 |
| `OPENAI_TEMPERATURE` 或 `LLM_TEMPERATURE` | 温度参数 | `0.3` | 否 |
| `OPENAI_MAX_TOKENS` 或 `LLM_MAX_TOKENS` | 最大token数 | `2000` | 否 |
| `OPENAI_BASE_URL` | 自定义API端点 | `https://api.openai.com/v1` | 否 |

### ChatGLM配置

| 变量名 | 说明 | 默认值 | 必需 |
|--------|------|--------|------|
| `CHATGLM_API_URL` | ChatGLM服务地址 | - | 是（使用ChatGLM时） |
| `CHATGLM_API_KEY` | ChatGLM API密钥 | - | 否 |
| `LLM_MODEL` | 模型名称 | - | 否 |

### Ollama配置

| 变量名 | 说明 | 默认值 | 必需 |
|--------|------|--------|------|
| `OLLAMA_BASE_URL` | Ollama服务地址 | `http://localhost:11434` | 否 |
| `OLLAMA_MODEL` | 模型名称 | `llama2` | 否 |
| `LLM_MODEL` | 模型名称（与OLLAMA_MODEL一致） | - | 否 |

### 自定义HTTP API配置

| 变量名 | 说明 | 默认值 | 必需 |
|--------|------|--------|------|
| `CUSTOM_LLM_API_URL` | 自定义API地址 | - | 是（使用custom时） |
| `CUSTOM_LLM_API_KEY` | 自定义API密钥 | - | 否 |

### 通用配置

| 变量名 | 说明 | 默认值 | 必需 |
|--------|------|--------|------|
| `LLM_TIMEOUT` | 请求超时时间（秒） | `30` | 否 |
| `LLM_MAX_RETRIES` | 最大重试次数 | `3` | 否 |
| `LLM_RETRY_DELAY` | 重试延迟（秒） | `1.0` | 否 |

## 服务器部署配置示例

### 1. 创建统一配置文件

在项目根目录创建 `.env` 文件：

```bash
# 在服务器上
cd /path/to/AIdoctor
nano .env
```

添加LLM配置：

```env
LLM_BACKEND=openai
OPENAI_API_KEY=sk-your-actual-api-key-here
OPENAI_MODEL=gpt-4
OPENAI_TEMPERATURE=0.3
OPENAI_MAX_TOKENS=2000
LLM_TIMEOUT=30
LLM_MAX_RETRIES=3
LLM_RETRY_DELAY=1.0
```

### 2. 使用Docker Compose部署

```yaml
# docker-compose.yml
version: '3.8'

services:
  diagnosis-engine-service:
    build: ./diagnosis-engine-service
    env_file:
      - .env  # 使用项目根目录的.env文件
    ports:
      - "8086:8086"
  
  explanation-service:
    build: ./explanation-service
    env_file:
      - .env  # 共享同一份配置
    ports:
      - "8087:8087"
  
  dialog-service:
    build: ./dialog-service
    env_file:
      - .env  # 共享同一份配置
    ports:
      - "8088:8088"
```

### 3. 使用systemd服务（非Docker）

创建systemd服务文件 `/etc/systemd/system/aidoctor-llm.service`：

```ini
[Unit]
Description=AI Doctor LLM Services
After=network.target

[Service]
Type=oneshot
RemainAfterExit=yes
EnvironmentFile=/path/to/AIdoctor/.env
ExecStart=/bin/true

[Install]
WantedBy=multi-user.target
```

然后在各个服务的systemd文件中引用：

```ini
[Service]
EnvironmentFile=/etc/systemd/system/aidoctor-llm.service
```

### 4. 使用Kubernetes ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: llm-config
data:
  LLM_BACKEND: "openai"
  OPENAI_MODEL: "gpt-4"
  OPENAI_TEMPERATURE: "0.3"
  OPENAI_MAX_TOKENS: "2000"
  LLM_TIMEOUT: "30"
  LLM_MAX_RETRIES: "3"
  LLM_RETRY_DELAY: "1.0"
---
apiVersion: v1
kind: Secret
metadata:
  name: llm-secret
type: Opaque
stringData:
  OPENAI_API_KEY: "sk-your-actual-api-key-here"
```

在Deployment中引用：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: diagnosis-engine-service
spec:
  template:
    spec:
      containers:
      - name: diagnosis-engine
        envFrom:
        - configMapRef:
            name: llm-config
        - secretRef:
            name: llm-secret
```

## 配置优先级

配置读取优先级（从高到低）：

1. **显式传入的LLMConfig对象**（代码中直接创建）
2. **服务特定的环境变量**（如 `diagnosis-engine-service/.env`）
3. **项目根目录的 `.env` 文件**
4. **系统环境变量**
5. **默认值**

## 验证配置

### 1. 检查环境变量

```bash
# 检查LLM配置
echo $LLM_BACKEND
echo $OPENAI_API_KEY
```

### 2. 测试LLM连接

```python
# test_llm.py
from aidoctor_llm import LangChainLLMClient

client = LangChainLLMClient()
result = await client.generate("Hello, world!")
print(result)
```

### 3. 查看服务日志

启动服务后，查看日志确认LLM配置：

```
INFO: LLM客户端初始化完成: backend=openai, model=gpt-4
```

## 常见问题

### Q1: 多个服务需要不同的LLM配置怎么办？

A: 可以在各服务的 `.env` 文件中覆盖配置，或者使用不同的环境变量前缀。

### Q2: 如何切换LLM后端？

A: 修改 `LLM_BACKEND` 环境变量，并配置对应的API密钥和地址。

### Q3: 配置不生效怎么办？

A: 
1. 检查环境变量是否正确设置
2. 确认服务是否重启（环境变量需要重启服务才能生效）
3. 检查 `.env` 文件路径是否正确
4. 查看服务日志确认配置加载情况

## 相关文件

- 公共LLM库配置：`common/aidoctor_llm/config.py`
- 公共LLM库客户端：`common/aidoctor_llm/llm_client.py`
- 诊断引擎服务配置：`diagnosis-engine-service/app/config/settings.py`

