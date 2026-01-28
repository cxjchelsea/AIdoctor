"""
配置管理
"""
import os
from typing import Optional
try:
    from pydantic_settings import BaseSettings
except ImportError:
    # 兼容旧版本pydantic
    from pydantic import BaseSettings


class Settings(BaseSettings):
    """应用配置"""
    
    # 服务配置
    SERVICE_NAME: str = "dialog-service"
    SERVICE_VERSION: str = "1.0.0"
    DEBUG: bool = os.getenv("DEBUG", "False").lower() == "true"
    
    # Redis配置
    REDIS_HOST: str = os.getenv("REDIS_HOST", "localhost")
    REDIS_PORT: int = int(os.getenv("REDIS_PORT", "6379"))
    REDIS_DB: int = int(os.getenv("REDIS_DB", "0"))
    REDIS_PASSWORD: Optional[str] = os.getenv("REDIS_PASSWORD")
    REDIS_CONTEXT_TTL: int = int(os.getenv("REDIS_CONTEXT_TTL", "1800"))  # 30分钟
    
    # LLM配置（通过环境变量，由公共库读取）
    LLM_BACKEND: str = os.getenv("LLM_BACKEND", "openai")
    OPENAI_API_KEY: Optional[str] = os.getenv("OPENAI_API_KEY")
    OPENAI_MODEL: str = os.getenv("OPENAI_MODEL", "gpt-4")
    LLM_TEMPERATURE: float = float(os.getenv("LLM_TEMPERATURE", "0.3"))
    LLM_MAX_TOKENS: int = int(os.getenv("LLM_MAX_TOKENS", "2000"))
    LLM_TIMEOUT: int = int(os.getenv("LLM_TIMEOUT", "30"))
    LLM_MAX_RETRIES: int = int(os.getenv("LLM_MAX_RETRIES", "3"))
    
    # 诊断服务配置（用于获取CDP数据）
    DIAGNOSIS_SERVICE_URL: str = os.getenv("DIAGNOSIS_SERVICE_URL", "http://localhost:8084")
    
    class Config:
        env_file = ".env"
        case_sensitive = True


# 全局配置实例
settings = Settings()

