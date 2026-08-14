"""
AI医生系统 LLM 公共库（LEGACY / DISABLED）

A7-NC-P5：仅提供机械 import 兼容 facade。
legacy provider runtime disabled；无 in-scope enable path。
"""

from .config import LLMBackend, LLMConfig
from .exceptions import (
    LLMAPIException,
    LLMConfigException,
    LLMException,
    LLMTimeoutException,
    LegacyLLMDisabledError,
)
from .llm_client import LangChainLLMClient
from .prompt_manager import PromptTemplateManager

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
    "LegacyLLMDisabledError",
]
