"""
配置设置
"""
from pydantic_settings import BaseSettings
from typing import Optional

class Settings(BaseSettings):
    """应用配置"""
    app_name: str = "病例理解服务"
    app_version: str = "1.0.0"
    
    # 服务配置
    host: str = "0.0.0.0"
    port: int = 8082
    
    # OCR服务配置（可选）
    ocr_service_url: Optional[str] = None
    ocr_service_timeout: int = 3
    ocr_service_retry: int = 2
    
    # 归一化词表配置
    vocabulary_base_path: str = "data/vocabularies"
    
    # Redis配置（可选）
    redis_host: Optional[str] = None
    redis_port: int = 6379
    redis_db: int = 0
    redis_password: Optional[str] = None
    
    # 日志配置
    log_level: str = "INFO"
    
    class Config:
        env_file = ".env"
        case_sensitive = False

settings = Settings()

