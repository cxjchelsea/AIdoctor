"""
配置管理
"""
from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """应用配置"""
    
    # 应用信息
    app_name: str = "知识库管理服务"
    app_version: str = "1.0.0"
    
    # 服务配置
    host: str = "0.0.0.0"
    port: int = 8094
    
    # Neo4j配置
    neo4j_uri: str = "bolt://localhost:7687"
    neo4j_user: str = "neo4j"
    neo4j_password: str = "password"
    neo4j_max_connection_lifetime: int = 3600
    neo4j_max_connection_pool_size: int = 50
    neo4j_connection_timeout: int = 60
    
    # 数据目录配置
    data_dir: str = "data"
    source_dir: str = "data/source"
    processed_dir: str = "data/processed"
    
    # 日志配置
    log_level: str = "INFO"
    log_file: str = "logs/app.log"
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()

