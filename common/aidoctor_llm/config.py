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

