"""
LLM 配置结构（mechanical compatibility only）

A7-NC-P5：保留 LLMConfig / LLMBackend 的结构形状供 import 兼容，
但不得作为 provider 激活权威；不得在此模块读取环境密钥。
"""

from enum import Enum
from typing import Dict, Optional

from pydantic import BaseModel, Field


class LLMBackend(str, Enum):
    """LLM 后端类型枚举（结构兼容；不激活任何 provider）"""

    OPENAI = "openai"
    CHATGLM = "chatglm"
    OLLAMA = "ollama"
    CUSTOM = "custom"


class LLMConfig(BaseModel):
    """LLM 配置结构体（仅数据形状；不触发网络 / SDK / 密钥读取）"""

    backend: LLMBackend = LLMBackend.OPENAI
    model: str = "gpt-4"
    temperature: float = 0.3
    max_tokens: int = 2000
    timeout: int = 30
    max_retries: int = 3
    retry_delay: float = 1.0

    # 以下字段仅为历史结构兼容；赋值不会激活 provider
    openai_api_key: Optional[str] = None
    openai_base_url: Optional[str] = None
    chatglm_api_url: Optional[str] = None
    chatglm_api_key: Optional[str] = None
    ollama_base_url: str = "http://localhost:11434"
    ollama_model: str = "llama2"
    custom_api_url: Optional[str] = None
    custom_api_key: Optional[str] = None
    custom_headers: Dict[str, str] = Field(default_factory=dict)
