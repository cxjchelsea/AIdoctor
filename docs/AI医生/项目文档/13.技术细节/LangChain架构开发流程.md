# 三个服务LangChain架构开发流程

**文档版本**: v1.0  
**创建日期**: 2026-01-22  
**适用范围**: diagnosis-engine-service, explanation-service, dialog-service  
**技术栈**: Python 3.10+, LangChain, FastAPI

---

## 📋 目录

1. [背景与目标](#一背景与目标)
2. [架构设计](#二架构设计)
3. [开发流程概览](#三开发流程概览)
4. [Phase 1: 创建公共LLM库](#四phase-1-创建公共llm库)
5. [Phase 2: 提取和重构代码](#五phase-2-提取和重构代码)
6. [Phase 3: 集成到三个服务](#六phase-3-集成到三个服务)
7. [Phase 4: 测试与验证](#七phase-4-测试与验证)
8. [Phase 5: 文档与维护](#八phase-5-文档与维护)
9. [常见问题与解决方案](#九常见问题与解决方案)
10. [检查清单](#十检查清单)

---

## 一、背景与目标

### 1.1 背景

根据AI医生系统的双通道推理架构设计，以下三个服务需要使用LangChain框架进行LLM集成：

| 服务 | LLM使用场景 | 使用频率 | 复杂度 |
|------|------------|---------|--------|
| **diagnosis-engine-service** | • 大模型引擎（五引擎之一）<br>• 路径注入LLM（DR.KNOWS核心） | 高 | 高 |
| **explanation-service** | • 生成自然语言解释<br>• 证据链说明<br>• 终点结论包说明 | 高 | 中 |
| **dialog-service** | • NLU（自然语言理解）<br>• NLG（自然语言生成）<br>• 问诊问题生成 | 高 | 中 |

### 1.2 目标

1. **统一LLM集成**：三个服务使用统一的LangChain客户端和Prompt模板管理
2. **代码复用**：提取公共代码，避免重复实现
3. **便于维护**：统一管理LLM相关代码，降低维护成本
4. **易于扩展**：支持多种LLM后端（OpenAI、Ollama、ChatGLM等）

### 1.3 原则

- **DRY原则**：不重复代码，公共功能提取到公共库
- **统一接口**：三个服务使用相同的LLM客户端接口
- **向后兼容**：不影响现有功能，逐步迁移
- **可测试性**：公共库独立测试，服务集成测试

---

## 二、架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    公共LLM库 (common/aidoctor_llm)           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  LangChainLLMClient                                  │  │
│  │  - 支持多种后端（OpenAI/Ollama/ChatGLM/自定义）      │  │
│  │  - 统一的重试和错误处理                               │  │
│  │  - 异步/同步调用支持                                   │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  PromptTemplateManager                                │  │
│  │  - Jinja2模板管理                                      │  │
│  │  - 模板加载和格式化                                     │  │
│  │  - 内置模板支持                                        │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  LLMConfig                                           │  │
│  │  - 配置管理                                           │  │
│  │  - 环境变量支持                                        │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  LLMExceptions                                       │  │
│  │  - LLM相关异常类                                      │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓ 被引用
        ┌───────────────────┼───────────────────┐
        ↓                   ↓                   ↓
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ diagnosis-   │   │ explanation- │   │ dialog-      │
│ engine-      │   │ service      │   │ service      │
│ service      │   │              │   │              │
│              │   │              │   │              │
│ - LLM引擎    │   │ - 解释生成   │   │ - NLU/NLG    │
│ - 路径注入   │   │ - 证据链说明 │   │ - 问诊问题   │
└──────────────┘   └──────────────┘   └──────────────┘
```

### 2.2 目录结构设计

```
AIdoctor/
├── common/                              # 公共库目录（新创建）
│   └── aidoctor_llm/                    # LLM公共库
│       ├── __init__.py                  # 包初始化，导出公共接口
│       ├── llm_client.py                # LangChainLLMClient
│       ├── prompt_manager.py            # PromptTemplateManager
│       ├── config.py                    # LLMConfig, LLMBackend
│       ├── exceptions.py                # LLM相关异常
│       ├── setup.py                     # Python包配置
│       ├── requirements.txt             # 依赖管理
│       └── README.md                    # 使用说明
│
├── diagnosis-engine-service/
│   ├── app/
│   │   └── utils/
│   │       └── llm_client.py            # 改为从公共库导入
│   └── requirements.txt                 # 添加: -e ../common/aidoctor_llm
│
├── explanation-service/
│   ├── app/
│   │   └── utils/
│   │       └── llm_client.py            # 从公共库导入
│   └── requirements.txt                 # 添加: -e ../common/aidoctor_llm
│
└── dialog-service/
    ├── app/
    │   └── utils/
    │       └── llm_client.py            # 从公共库导入
    └── requirements.txt                 # 添加: -e ../common/aidoctor_llm
```

### 2.3 公共库接口设计

#### 2.3.1 LangChainLLMClient接口

```python
class LangChainLLMClient:
    """LangChain LLM客户端封装"""
    
    def __init__(self, config: Optional[LLMConfig] = None):
        """初始化LLM客户端"""
        pass
    
    async def generate(self, prompt: str, **kwargs) -> str:
        """异步生成文本"""
        pass
    
    def generate_sync(self, prompt: str, **kwargs) -> str:
        """同步生成文本"""
        pass
```

#### 2.3.2 PromptTemplateManager接口

```python
class PromptTemplateManager:
    """提示词模板管理器"""
    
    def __init__(self, templates_dir: Optional[str] = None):
        """初始化模板管理器"""
        pass
    
    def format(self, template_name: str, **kwargs) -> str:
        """格式化模板"""
        pass
    
    def format_diagnosis_reasoning(self, symptoms, signs, context, paths=None) -> str:
        """格式化诊断推理提示词"""
        pass
    
    def format_question_generation(self, missing_info, context) -> str:
        """格式化问诊问题生成提示词"""
        pass
    
    def format_explanation_generation(self, diagnosis, evidence, reasoning_path) -> str:
        """格式化解释生成提示词"""
        pass
```

---

## 三、开发流程概览

### 3.1 开发阶段

```
Phase 1: 创建公共LLM库
    ↓
Phase 2: 提取和重构代码
    ↓
Phase 3: 集成到三个服务
    ↓
Phase 4: 测试与验证
    ↓
Phase 5: 文档与维护
```

### 3.2 时间估算

| 阶段 | 任务 | 预估时间 |
|------|------|---------|
| **Phase 1** | 创建公共库目录结构和基础代码 | 1-2天 |
| **Phase 2** | 从diagnosis-engine-service提取代码并重构 | 2-3天 |
| **Phase 3** | 集成到三个服务 | 2-3天 |
| **Phase 4** | 测试与验证 | 2-3天 |
| **Phase 5** | 文档编写 | 1天 |
| **总计** | | **8-12天** |

---

## 四、Phase 1: 创建公共LLM库

### 4.1 创建目录结构

**步骤1：创建公共库目录**

```bash
# 在项目根目录下创建
mkdir -p common/aidoctor_llm
cd common/aidoctor_llm
```

**步骤2：创建基础文件**

```bash
# 创建Python包文件
touch __init__.py
touch llm_client.py
touch prompt_manager.py
touch config.py
touch exceptions.py
touch setup.py
touch requirements.txt
touch README.md
```

### 4.2 创建setup.py

**文件**: `common/aidoctor_llm/setup.py`

```python
from setuptools import setup, find_packages

setup(
    name="aidoctor-llm-common",
    version="0.1.0",
    description="AI医生系统LLM公共库",
    author="AI医生开发团队",
    packages=find_packages(),
    install_requires=[
        "langchain==0.1.0",
        "langchain-openai==0.0.2",
        "openai==1.10.0",
        "jinja2==3.1.2",
        "pyyaml==6.0.1",
        "httpx==0.25.1",
        "pydantic==2.5.0",
    ],
    python_requires=">=3.10",
)
```

### 4.3 创建requirements.txt

**文件**: `common/aidoctor_llm/requirements.txt`

```txt
langchain==0.1.0
langchain-openai==0.0.2
openai==1.10.0
jinja2==3.1.2
pyyaml==6.0.1
httpx==0.25.1
pydantic==2.5.0
```

### 4.4 创建__init__.py

**文件**: `common/aidoctor_llm/__init__.py`

```python
"""
AI医生系统LLM公共库
提供统一的LangChain客户端和Prompt模板管理
"""

from .llm_client import LangChainLLMClient
from .prompt_manager import PromptTemplateManager
from .config import LLMConfig, LLMBackend
from .exceptions import LLMException, LLMTimeoutException, LLMAPIException

__version__ = "0.1.0"
__all__ = [
    "LangChainLLMClient",
    "PromptTemplateManager",
    "LLMConfig",
    "LLMBackend",
    "LLMException",
    "LLMTimeoutException",
    "LLMAPIException",
]
```

### 4.5 创建README.md

**文件**: `common/aidoctor_llm/README.md`

```markdown
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

## 更多文档

详见各服务的使用文档。
```

---

## 五、Phase 2: 提取和重构代码

### 5.1 从diagnosis-engine-service提取代码

**步骤1：复制LLM客户端代码**

```bash
# 从diagnosis-engine-service复制到公共库
cp diagnosis-engine-service/app/utils/llm_client.py common/aidoctor_llm/llm_client.py
```

**步骤2：复制Prompt模板管理代码**

```bash
# 从diagnosis-engine-service复制到公共库
cp diagnosis-engine-service/app/utils/prompt_templates.py common/aidoctor_llm/prompt_manager.py
```

### 5.2 重构代码

#### 5.2.1 重构llm_client.py

**需要修改的地方**：

1. **更新导入路径**：
   ```python
   # 原代码
   from app.utils.exceptions import SomeException
   
   # 改为
   from .exceptions import LLMException
   ```

2. **移除服务特定的依赖**：
   - 移除对服务特定配置的依赖
   - 使用通用的配置管理

3. **统一异常处理**：
   - 使用公共库的异常类

#### 5.2.2 重构prompt_manager.py

**需要修改的地方**：

1. **更新模板目录路径**：
   ```python
   # 原代码
   self.templates_dir = Path(__file__).parent.parent / "config" / "prompt_templates"
   
   # 改为（支持自定义模板目录）
   if templates_dir:
       self.templates_dir = Path(templates_dir)
   else:
       # 默认使用当前包的templates目录
       self.templates_dir = Path(__file__).parent / "templates"
   ```

2. **提取服务特定的模板方法**：
   - `format_diagnosis_reasoning()` - 诊断引擎服务使用
   - `format_question_generation()` - 对话服务使用
   - `format_explanation_generation()` - 解释服务使用

### 5.3 创建config.py

**文件**: `common/aidoctor_llm/config.py`

```python
"""
LLM配置管理
"""
from enum import Enum
from typing import Optional, Dict
from pydantic import BaseModel


class LLMBackend(str, Enum):
    """LLM后端类型"""
    OPENAI = "openai"
    CHATGLM = "chatglm"
    OLLAMA = "ollama"
    CUSTOM = "custom"


class LLMConfig(BaseModel):
    """LLM配置"""
    backend: LLMBackend = LLMBackend.OPENAI
    model: str = "gpt-4"
    temperature: float = 0.3
    max_tokens: int = 2000
    timeout: int = 30
    max_retries: int = 3
    retry_delay: float = 1.0
    
    # OpenAI配置
    openai_api_key: Optional[str] = None
    openai_base_url: Optional[str] = None
    
    # ChatGLM配置
    chatglm_api_url: Optional[str] = None
    chatglm_api_key: Optional[str] = None
    
    # Ollama配置
    ollama_base_url: str = "http://localhost:11434"
    ollama_model: str = "llama2"
    
    # 自定义HTTP API配置
    custom_api_url: Optional[str] = None
    custom_api_key: Optional[str] = None
    custom_headers: Dict[str, str] = {}
```

### 5.4 创建exceptions.py

**文件**: `common/aidoctor_llm/exceptions.py`

```python
"""
LLM相关异常类
"""


class LLMException(Exception):
    """LLM基础异常"""
    def __init__(self, message: str, error_code: str = None):
        self.message = message
        self.error_code = error_code
        super().__init__(self.message)


class LLMTimeoutException(LLMException):
    """LLM超时异常"""
    def __init__(self, message: str = "LLM调用超时"):
        super().__init__(message, "LLM_TIMEOUT")


class LLMAPIException(LLMException):
    """LLM API调用异常"""
    def __init__(self, message: str, status_code: int = None):
        self.status_code = status_code
        super().__init__(message, "LLM_API_ERROR")


class LLMConfigException(LLMException):
    """LLM配置异常"""
    def __init__(self, message: str):
        super().__init__(message, "LLM_CONFIG_ERROR")
```

### 5.5 测试公共库

**创建测试文件**: `common/aidoctor_llm/tests/test_llm_client.py`

```python
"""
测试LLM客户端
"""
import pytest
from aidoctor_llm import LangChainLLMClient, LLMConfig, LLMBackend


def test_llm_client_init():
    """测试LLM客户端初始化"""
    config = LLMConfig(
        backend=LLMBackend.OPENAI,
        model="gpt-4",
        openai_api_key="test-key"
    )
    client = LangChainLLMClient(config)
    assert client.config.backend == LLMBackend.OPENAI


def test_llm_config_from_env():
    """测试从环境变量加载配置"""
    import os
    os.environ["LLM_BACKEND"] = "openai"
    os.environ["OPENAI_API_KEY"] = "test-key"
    
    client = LangChainLLMClient()
    assert client.config.backend == LLMBackend.OPENAI
```

**运行测试**：

```bash
cd common/aidoctor_llm
pytest tests/
```

---

## 六、Phase 3: 集成到三个服务

### 6.1 集成到diagnosis-engine-service

#### 6.1.1 更新requirements.txt

**文件**: `diagnosis-engine-service/requirements.txt`

```txt
# 原有依赖...
fastapi==0.104.1
uvicorn[standard]==0.24.0
# ... 其他依赖

# 添加公共LLM库（开发模式）
-e ../common/aidoctor_llm
```

#### 6.1.2 修改llm_client.py

**文件**: `diagnosis-engine-service/app/utils/llm_client.py`

```python
"""
LLM客户端（从公共库导入）
"""
# 从公共库导入
from aidoctor_llm import LangChainLLMClient, LLMConfig

# 为了向后兼容，可以保留别名
__all__ = ["LangChainLLMClient", "LLMConfig"]
```

#### 6.1.3 修改prompt_templates.py

**文件**: `diagnosis-engine-service/app/utils/prompt_templates.py`

```python
"""
Prompt模板管理（从公共库导入）
"""
# 从公共库导入
from aidoctor_llm import PromptTemplateManager

# 为了向后兼容，可以保留别名
PromptTemplateManager = PromptTemplateManager
```

#### 6.1.4 更新使用代码

**文件**: `diagnosis-engine-service/app/multi-engine-fusion/llm_engine.py`

```python
# 原代码
from app.utils.llm_client import LangChainLLMClient
from app.utils.prompt_templates import PromptTemplateManager

# 改为（可选，因为utils已经重新导出）
from app.utils.llm_client import LangChainLLMClient
from app.utils.prompt_templates import PromptTemplateManager

# 或者直接使用
from aidoctor_llm import LangChainLLMClient, PromptTemplateManager
```

#### 6.1.5 安装依赖

```bash
cd diagnosis-engine-service
pip install -r requirements.txt
```

### 6.2 集成到explanation-service

#### 6.2.1 更新requirements.txt

**文件**: `explanation-service/requirements.txt`

```txt
# 原有依赖...
fastapi==0.104.1
uvicorn[standard]==0.24.0
# ... 其他依赖

# 添加公共LLM库
-e ../common/aidoctor_llm
```

#### 6.2.2 创建llm_client.py

**文件**: `explanation-service/app/utils/llm_client.py`

```python
"""
LLM客户端（从公共库导入）
"""
from aidoctor_llm import LangChainLLMClient, LLMConfig

__all__ = ["LangChainLLMClient", "LLMConfig"]
```

#### 6.2.3 创建prompt_manager.py

**文件**: `explanation-service/app/utils/prompt_manager.py`

```python
"""
Prompt模板管理（从公共库导入）
"""
from aidoctor_llm import PromptTemplateManager

__all__ = ["PromptTemplateManager"]
```

#### 6.2.4 在服务中使用

**文件**: `explanation-service/app/services/explanation_service.py`

```python
from app.utils.llm_client import LangChainLLMClient
from app.utils.prompt_manager import PromptTemplateManager

class ExplanationService:
    def __init__(self):
        self.llm_client = LangChainLLMClient()
        self.template_manager = PromptTemplateManager()
    
    async def generate_explanation(self, cdp: Dict) -> str:
        """生成自然语言解释"""
        # 使用模板管理器格式化提示词
        prompt = self.template_manager.format_explanation_generation(
            diagnosis=cdp.get("ddx"),
            evidence=cdp.get("evidence_graph"),
            reasoning_path=cdp.get("reasoning_paths")
        )
        
        # 调用LLM生成解释
        explanation = await self.llm_client.generate(prompt)
        return explanation
```

#### 6.2.5 安装依赖

```bash
cd explanation-service
pip install -r requirements.txt
```

### 6.3 集成到dialog-service

#### 6.3.1 更新requirements.txt

**文件**: `dialog-service/requirements.txt`

```txt
# 原有依赖...
fastapi==0.104.1
uvicorn[standard]==0.24.0
# ... 其他依赖

# 添加公共LLM库
-e ../common/aidoctor_llm
```

#### 6.3.2 创建llm_client.py

**文件**: `dialog-service/app/utils/llm_client.py`

```python
"""
LLM客户端（从公共库导入）
"""
from aidoctor_llm import LangChainLLMClient, LLMConfig

__all__ = ["LangChainLLMClient", "LLMConfig"]
```

#### 6.3.3 创建prompt_manager.py

**文件**: `dialog-service/app/utils/prompt_manager.py`

```python
"""
Prompt模板管理（从公共库导入）
"""
from aidoctor_llm import PromptTemplateManager

__all__ = ["PromptTemplateManager"]
```

#### 6.3.4 更新NLG服务

**文件**: `dialog-service/app/core/nlg.py`

```python
from app.utils.llm_client import LangChainLLMClient
from app.utils.prompt_manager import PromptTemplateManager

class NaturalLanguageGenerator:
    """自然语言生成"""
    
    def __init__(self):
        self.llm_client = LangChainLLMClient()
        self.template_manager = PromptTemplateManager()
    
    async def generate_question_with_llm(self, question_info: Dict, context: Dict) -> str:
        """使用LLM生成问诊问题"""
        # 使用模板管理器格式化提示词
        prompt = self.template_manager.format_question_generation(
            missing_info=question_info.get("missing_info"),
            context=context
        )
        
        # 调用LLM生成问题
        question = await self.llm_client.generate(prompt)
        return question
```

#### 6.3.5 更新NLU服务

**文件**: `dialog-service/app/core/nlu.py`

```python
from app.utils.llm_client import LangChainLLMClient

class NaturalLanguageUnderstanding:
    """自然语言理解"""
    
    def __init__(self):
        self.llm_client = LangChainLLMClient()
    
    async def understand(self, text: str, context: Dict) -> Dict:
        """理解用户输入"""
        prompt = f"""理解以下用户输入，提取关键信息：
        
用户输入：{text}
上下文：{context}

请提取：
1. 症状信息
2. 持续时间
3. 严重程度
4. 伴随症状

返回JSON格式。"""
        
        result = await self.llm_client.generate(prompt)
        # 解析JSON结果
        import json
        return json.loads(result)
```

#### 6.3.6 安装依赖

```bash
cd dialog-service
pip install -r requirements.txt
```

---

## 七、Phase 4: 测试与验证

### 7.1 公共库测试

#### 7.1.1 单元测试

**测试文件**: `common/aidoctor_llm/tests/test_llm_client.py`

```python
"""
LLM客户端单元测试
"""
import pytest
from unittest.mock import Mock, patch
from aidoctor_llm import LangChainLLMClient, LLMConfig, LLMBackend


@pytest.fixture
def mock_llm_config():
    """Mock LLM配置"""
    return LLMConfig(
        backend=LLMBackend.OPENAI,
        model="gpt-4",
        openai_api_key="test-key"
    )


def test_llm_client_init(mock_llm_config):
    """测试LLM客户端初始化"""
    with patch('aidoctor_llm.llm_client.ChatOpenAI'):
        client = LangChainLLMClient(mock_llm_config)
        assert client.config.backend == LLMBackend.OPENAI


@pytest.mark.asyncio
async def test_llm_client_generate(mock_llm_config):
    """测试LLM生成文本"""
    with patch('aidoctor_llm.llm_client.ChatOpenAI') as mock_chat:
        mock_llm = Mock()
        mock_llm.apredict = Mock(return_value="测试响应")
        mock_chat.return_value = mock_llm
        
        client = LangChainLLMClient(mock_llm_config)
        result = await client.generate("测试提示词")
        
        assert result == "测试响应"
        mock_llm.apredict.assert_called_once()
```

#### 7.1.2 运行测试

```bash
cd common/aidoctor_llm
pytest tests/ -v
```

### 7.2 服务集成测试

#### 7.2.1 diagnosis-engine-service测试

**测试文件**: `diagnosis-engine-service/tests/test_llm_integration.py`

```python
"""
诊断引擎服务LLM集成测试
"""
import pytest
from app.multi_engine_fusion.llm_engine import LLMEngine


@pytest.mark.asyncio
async def test_llm_engine_with_common_lib():
    """测试LLM引擎使用公共库"""
    engine = LLMEngine()
    
    # 验证LLM客户端已初始化
    assert engine.llm_client is not None
    assert engine.template_manager is not None
    
    # 测试诊断（需要Mock LLM调用）
    # ...
```

#### 7.2.2 explanation-service测试

**测试文件**: `explanation-service/tests/test_explanation_service.py`

```python
"""
解释服务LLM集成测试
"""
import pytest
from app.services.explanation_service import ExplanationService


@pytest.mark.asyncio
async def test_explanation_service_with_common_lib():
    """测试解释服务使用公共库"""
    service = ExplanationService()
    
    # 验证LLM客户端已初始化
    assert service.llm_client is not None
    assert service.template_manager is not None
```

#### 7.2.3 dialog-service测试

**测试文件**: `dialog-service/tests/test_nlg.py`

```python
"""
对话服务NLG集成测试
"""
import pytest
from app.core.nlg import NaturalLanguageGenerator


@pytest.mark.asyncio
async def test_nlg_with_common_lib():
    """测试NLG使用公共库"""
    nlg = NaturalLanguageGenerator()
    
    # 验证LLM客户端已初始化
    assert nlg.llm_client is not None
    assert nlg.template_manager is not None
```

### 7.3 端到端测试

#### 7.3.1 测试场景

1. **诊断引擎服务**：
   - 测试大模型引擎调用
   - 测试路径注入LLM功能

2. **解释服务**：
   - 测试解释生成功能
   - 测试证据链说明生成

3. **对话服务**：
   - 测试问诊问题生成
   - 测试自然语言理解

#### 7.3.2 测试脚本

**文件**: `tests/e2e/test_llm_services.py`

```python
"""
端到端测试：三个服务的LLM功能
"""
import pytest
import asyncio


@pytest.mark.asyncio
async def test_all_services_use_common_llm_lib():
    """测试所有服务都使用公共LLM库"""
    # 1. 测试诊断引擎服务
    from diagnosis_engine_service.app.multi_engine_fusion.llm_engine import LLMEngine
    engine = LLMEngine()
    assert hasattr(engine, 'llm_client')
    
    # 2. 测试解释服务
    from explanation_service.app.services.explanation_service import ExplanationService
    service = ExplanationService()
    assert hasattr(service, 'llm_client')
    
    # 3. 测试对话服务
    from dialog_service.app.core.nlg import NaturalLanguageGenerator
    nlg = NaturalLanguageGenerator()
    assert hasattr(nlg, 'llm_client')
```

---

## 八、Phase 5: 文档与维护

### 8.1 更新服务文档

#### 8.1.1 更新diagnosis-engine-service文档

在服务实现方案中添加：

```markdown
## LLM集成

本服务使用公共LLM库（`aidoctor_llm`）进行LLM调用。

### 依赖

```txt
-e ../common/aidoctor_llm
```

### 使用方式

```python
from aidoctor_llm import LangChainLLMClient, PromptTemplateManager

llm_client = LangChainLLMClient()
template_manager = PromptTemplateManager()
```
```

#### 8.1.2 更新explanation-service文档

类似地更新解释服务的文档。

#### 8.1.3 更新dialog-service文档

类似地更新对话服务的文档。

### 8.2 创建维护文档

**文件**: `common/aidoctor_llm/MAINTENANCE.md`

```markdown
# 公共LLM库维护文档

## 版本管理

- 使用语义化版本（Semantic Versioning）
- 主版本号：不兼容的API修改
- 次版本号：向下兼容的功能性新增
- 修订号：向下兼容的问题修正

## 更新流程

1. 修改公共库代码
2. 更新版本号（setup.py和__init__.py）
3. 运行测试确保通过
4. 更新CHANGELOG.md
5. 通知各服务更新依赖

## 兼容性

- 保持向后兼容，避免破坏性更改
- 如需破坏性更改，先标记为deprecated，下一个主版本再移除

## 测试

每次更新前必须运行：

```bash
cd common/aidoctor_llm
pytest tests/ -v
```

## 依赖更新

更新依赖时：

1. 更新requirements.txt
2. 更新setup.py
3. 测试所有使用该库的服务
4. 更新文档
```

### 8.3 创建CHANGELOG

**文件**: `common/aidoctor_llm/CHANGELOG.md`

```markdown
# 更新日志

## [0.1.0] - 2026-01-22

### 新增
- 初始版本
- LangChainLLMClient：支持多种LLM后端
- PromptTemplateManager：提示词模板管理
- LLMConfig：配置管理
- LLM异常类

### 支持的后端
- OpenAI
- Ollama
- ChatGLM
- 自定义HTTP API
```

---

## 九、常见问题与解决方案

### 9.1 导入错误

**问题**：`ModuleNotFoundError: No module named 'aidoctor_llm'`

**解决方案**：

1. **检查安装**：
   ```bash
   cd common/aidoctor_llm
   pip install -e .
   ```

2. **检查PYTHONPATH**：
   ```bash
   export PYTHONPATH="${PYTHONPATH}:$(pwd)/common"
   ```

3. **检查requirements.txt**：
   确保服务的requirements.txt中包含：
   ```txt
   -e ../common/aidoctor_llm
   ```

### 9.2 版本冲突

**问题**：不同服务使用不同版本的公共库

**解决方案**：

1. **统一版本**：所有服务使用相同版本的公共库
2. **版本锁定**：在setup.py中明确版本号
3. **依赖管理**：使用requirements.txt锁定版本

### 9.3 配置问题

**问题**：环境变量配置不正确

**解决方案**：

1. **检查环境变量**：
   ```bash
   echo $OPENAI_API_KEY
   echo $LLM_BACKEND
   ```

2. **使用配置文件**：支持从配置文件读取（可选）

3. **默认值**：提供合理的默认值

### 9.4 模板路径问题

**问题**：找不到模板文件

**解决方案**：

1. **自定义模板目录**：
   ```python
   template_manager = PromptTemplateManager(
       templates_dir="/path/to/templates"
   )
   ```

2. **使用内置模板**：如果文件不存在，使用内置模板

3. **检查路径**：确保模板文件存在

---

## 十、检查清单

### 10.1 Phase 1检查清单

- [ ] 创建公共库目录结构
- [ ] 创建setup.py
- [ ] 创建requirements.txt
- [ ] 创建__init__.py
- [ ] 创建README.md

### 10.2 Phase 2检查清单

- [ ] 从diagnosis-engine-service提取llm_client.py
- [ ] 从diagnosis-engine-service提取prompt_templates.py
- [ ] 重构代码，移除服务特定依赖
- [ ] 创建config.py
- [ ] 创建exceptions.py
- [ ] 编写单元测试
- [ ] 测试通过

### 10.3 Phase 3检查清单

#### diagnosis-engine-service
- [ ] 更新requirements.txt
- [ ] 修改llm_client.py（改为从公共库导入）
- [ ] 修改prompt_templates.py（改为从公共库导入）
- [ ] 更新使用代码
- [ ] 安装依赖
- [ ] 测试服务启动

#### explanation-service
- [ ] 更新requirements.txt
- [ ] 创建llm_client.py
- [ ] 创建prompt_manager.py
- [ ] 在服务中使用
- [ ] 安装依赖
- [ ] 测试服务启动

#### dialog-service
- [ ] 更新requirements.txt
- [ ] 创建llm_client.py
- [ ] 创建prompt_manager.py
- [ ] 更新NLG服务
- [ ] 更新NLU服务
- [ ] 安装依赖
- [ ] 测试服务启动

### 10.4 Phase 4检查清单

- [ ] 公共库单元测试通过
- [ ] diagnosis-engine-service集成测试通过
- [ ] explanation-service集成测试通过
- [ ] dialog-service集成测试通过
- [ ] 端到端测试通过

### 10.5 Phase 5检查清单

- [ ] 更新diagnosis-engine-service文档
- [ ] 更新explanation-service文档
- [ ] 更新dialog-service文档
- [ ] 创建维护文档
- [ ] 创建CHANGELOG

---

## 十一、总结

### 11.1 开发流程总结

1. **Phase 1**：创建公共库基础结构（1-2天）
2. **Phase 2**：提取和重构代码（2-3天）
3. **Phase 3**：集成到三个服务（2-3天）
4. **Phase 4**：测试与验证（2-3天）
5. **Phase 5**：文档与维护（1天）

**总计**：8-12天

### 11.2 关键要点

1. **统一接口**：三个服务使用相同的LLM客户端接口
2. **代码复用**：公共功能提取到公共库
3. **向后兼容**：不影响现有功能
4. **测试驱动**：每个阶段都有测试验证

### 11.3 后续优化

1. **性能优化**：缓存、连接池等
2. **功能扩展**：支持更多LLM后端
3. **监控告警**：LLM调用监控
4. **成本控制**：Token使用统计和限制

---

**文档维护**: 当有新的需求或变更时，请及时更新本文档。

