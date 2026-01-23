"""
AI医生系统LLM公共库
提供统一的LangChain客户端和Prompt模板管理
"""

from .llm_client import LangChainLLMClient
from .prompt_manager import PromptTemplateManager
from .config import LLMConfig, LLMBackend
from .exceptions import LLMException, LLMTimeoutException, LLMAPIException, LLMConfigException

__version__ = "0.1.0"
__all__ = [
    "LangChainLLMClient",
    "PromptTemplateManager",
    "LLMConfig",
    "LLMBackend",
    "LLMException",
    "LLMTimeoutException",
    "LLMAPIException",
    "LLMConfigException",
]

