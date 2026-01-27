"""
FastAPI依赖注入
用于管理共享资源（LLM客户端、数据库连接等）
性能优化：使用单例模式避免重复创建
"""
from functools import lru_cache
from typing import Optional

# 直接从公共库导入，避免重复定义问题
try:
    from aidoctor_llm import LangChainLLMClient, LLMConfig, LLMBackend
except ImportError:
    # 如果公共库不可用，使用本地导入
    from app.utils.llm_client import LangChainLLMClient, LLMConfig, LLMBackend

from app.config.settings import settings

# 全局LLM客户端单例
_llm_client_singleton: Optional[LangChainLLMClient] = None


@lru_cache()
def get_llm_client() -> LangChainLLMClient:
    """
    获取LLM客户端（单例模式）
    使用lru_cache确保只创建一次
    
    Returns:
        LangChainLLMClient实例
    """
    global _llm_client_singleton
    
    if _llm_client_singleton is None:
        llm_config = LLMConfig(
            backend=LLMBackend(settings.LLM_BACKEND),
            model=settings.LLM_MODEL,
            temperature=settings.LLM_TEMPERATURE,
            max_tokens=settings.LLM_MAX_TOKENS,
            timeout=settings.LLM_TIMEOUT,
            max_retries=settings.LLM_MAX_RETRIES,
            openai_api_key=settings.OPENAI_API_KEY,
            openai_base_url=settings.OPENAI_BASE_URL,
            chatglm_api_url=settings.CHATGLM_API_URL,
            chatglm_api_key=settings.CHATGLM_API_KEY,
            ollama_base_url=settings.OLLAMA_BASE_URL,
            ollama_model=settings.OLLAMA_MODEL,
            custom_api_url=settings.CUSTOM_API_URL,
            custom_api_key=settings.CUSTOM_API_KEY
        )
        _llm_client_singleton = LangChainLLMClient(config=llm_config)
    
    return _llm_client_singleton

