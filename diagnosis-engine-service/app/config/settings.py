"""
配置管理
从环境变量或配置文件加载配置
"""

import os
from typing import Optional
from pydantic import BaseModel
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()


class Settings(BaseModel):
    """应用配置"""
    
    # 服务配置
    SERVICE_NAME: str = "diagnosis-engine-service"
    SERVICE_VERSION: str = "1.0.0"
    DEBUG: bool = os.getenv("DEBUG", "False").lower() == "true"
    
    # Neo4j配置（性能优化：连接池配置）
    NEO4J_URI: str = os.getenv("NEO4J_URI", "bolt://localhost:7687")
    NEO4J_USER: str = os.getenv("NEO4J_USER", "neo4j")
    NEO4J_PASSWORD: str = os.getenv("NEO4J_PASSWORD", "password")
    NEO4J_MAX_CONNECTION_POOL_SIZE: int = int(os.getenv("NEO4J_MAX_CONNECTION_POOL_SIZE", "50"))
    NEO4J_CONNECTION_TIMEOUT: int = int(os.getenv("NEO4J_CONNECTION_TIMEOUT", "2"))
    NEO4J_MAX_CONNECTION_LIFETIME: int = int(os.getenv("NEO4J_MAX_CONNECTION_LIFETIME", "1800"))  # 30分钟
    NEO4J_QUERY_TIMEOUT: int = int(os.getenv("NEO4J_QUERY_TIMEOUT", "10"))
    
    # LLM配置
    LLM_BACKEND: str = os.getenv("LLM_BACKEND", "openai")
    LLM_MODEL: str = os.getenv("LLM_MODEL", "gpt-4")
    LLM_TEMPERATURE: float = float(os.getenv("LLM_TEMPERATURE", "0.3"))
    LLM_MAX_TOKENS: int = int(os.getenv("LLM_MAX_TOKENS", "2000"))
    LLM_TIMEOUT: int = int(os.getenv("LLM_TIMEOUT", "30"))
    LLM_MAX_RETRIES: int = int(os.getenv("LLM_MAX_RETRIES", "3"))
    
    # OpenAI配置
    OPENAI_API_KEY: Optional[str] = os.getenv("OPENAI_API_KEY")
    OPENAI_BASE_URL: Optional[str] = os.getenv("OPENAI_BASE_URL")
    
    # ChatGLM配置
    CHATGLM_API_URL: Optional[str] = os.getenv("CHATGLM_API_URL")
    CHATGLM_API_KEY: Optional[str] = os.getenv("CHATGLM_API_KEY")
    
    # Ollama配置
    OLLAMA_BASE_URL: str = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
    OLLAMA_MODEL: str = os.getenv("OLLAMA_MODEL", "llama2")
    
    # 自定义HTTP API配置
    CUSTOM_API_URL: Optional[str] = os.getenv("CUSTOM_API_URL")
    CUSTOM_API_KEY: Optional[str] = os.getenv("CUSTOM_API_KEY")
    
    # Redis配置（可选）
    REDIS_HOST: Optional[str] = os.getenv("REDIS_HOST")
    REDIS_PORT: int = int(os.getenv("REDIS_PORT", "6379"))
    REDIS_PASSWORD: Optional[str] = os.getenv("REDIS_PASSWORD")
    REDIS_DB: int = int(os.getenv("REDIS_DB", "0"))
    REDIS_TTL: int = int(os.getenv("REDIS_TTL", "3600"))  # 1小时
    
    # 诊断引擎配置
    MAX_DIAGNOSIS_CANDIDATES: int = int(os.getenv("MAX_DIAGNOSIS_CANDIDATES", "5"))
    PATH_MAX_HOPS: int = int(os.getenv("PATH_MAX_HOPS", "4"))
    PATH_MIN_HOPS: int = int(os.getenv("PATH_MIN_HOPS", "2"))
    
    # 引擎权重配置
    ENGINE_WEIGHT_RULE: float = float(os.getenv("ENGINE_WEIGHT_RULE", "0.25"))
    ENGINE_WEIGHT_KG: float = float(os.getenv("ENGINE_WEIGHT_KG", "0.25"))
    ENGINE_WEIGHT_STATISTICAL: float = float(os.getenv("ENGINE_WEIGHT_STATISTICAL", "0.20"))
    ENGINE_WEIGHT_LLM: float = float(os.getenv("ENGINE_WEIGHT_LLM", "0.25"))
    ENGINE_WEIGHT_DIFFERENTIAL: float = float(os.getenv("ENGINE_WEIGHT_DIFFERENTIAL", "0.05"))
    
    # 日志配置
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")
    LOG_FORMAT: str = os.getenv("LOG_FORMAT", "%(asctime)s - %(name)s - %(levelname)s - %(message)s")
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        # Pydantic v2兼容性
        extra = "allow"


# 全局配置实例
settings = Settings()

