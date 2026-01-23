"""
配置设置
"""
import os
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """应用配置"""
    
    # 服务配置
    service_name: str = "treatment-engine-service"
    service_port: int = 8091
    
    # 数据库配置（如果需要）
    database_url: str = os.getenv("DATABASE_URL", "")
    
    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()

