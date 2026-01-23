"""
配置设置
"""
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    """应用配置"""
    app_name: str = "健康状态判定服务"
    app_version: str = "1.0.0"
    
    class Config:
        env_file = ".env"

settings = Settings()

