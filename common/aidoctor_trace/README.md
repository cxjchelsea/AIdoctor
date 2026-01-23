# AI医生执行追踪公共库

用于所有Python服务的执行追踪功能。

## 安装

```bash
pip install -e .
```

## 使用方法

### 1. 在FastAPI应用中添加中间件

```python
from fastapi import FastAPI
from aidoctor_trace import TraceMiddleware

app = FastAPI()

# 添加追踪中间件
app.add_middleware(TraceMiddleware)
```

### 2. 在服务方法上添加装饰器

```python
from aidoctor_trace import trace_execution

class ParsingService:
    
    @trace_execution(service="clinical-parsing-service", module="concept_normalizer")
    async def normalize_concepts(self, text: str) -> Dict:
        # 业务逻辑
        pass
```

### 3. 配置追踪服务URL（可选）

默认连接到 `http://localhost:8084`，可以通过环境变量配置：

```bash
export TRACE_SERVICE_URL=http://your-trace-service:8084
```

## 功能特性

- 自动提取CDP ID（从请求头或请求体）
- 自动记录方法调用（开始、结束、错误）
- 异步支持
- 不影响主业务流程（追踪失败不影响服务）

