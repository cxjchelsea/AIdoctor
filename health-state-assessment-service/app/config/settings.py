"""
配置设置
"""
from pathlib import Path
from pydantic_settings import BaseSettings
from typing import Optional

# 计算项目根目录路径（从当前文件向上3级：app/config -> app -> health-state-assessment-service -> 项目根目录）
_current_file = Path(__file__).resolve()
_project_root = _current_file.parent.parent.parent.parent
_env_file_path = _project_root / ".env"


class NLUConfig(BaseSettings):
    """NLU配置"""
    # LLM配置
    use_llm: bool = True  # 是否使用LLM
    llm_backend: str = "ollama"  # LLM后端：openai/chatglm/ollama/custom
    llm_model: str = "qwen2.5:7b"  # LLM模型
    llm_temperature: float = 0.3  # LLM温度
    llm_timeout: int = 30  # LLM超时时间（秒）
    llm_max_tokens: int = 2000  # LLM最大token数
    llm_max_retries: int = 3  # LLM最大重试次数
    
    # OpenAI配置
    openai_api_key: Optional[str] = None
    openai_base_url: Optional[str] = None
    
    # ChatGLM配置
    chatglm_api_url: Optional[str] = None
    chatglm_api_key: Optional[str] = None
    
    # Ollama配置
    ollama_base_url: str = "http://localhost:11434"
    ollama_model: str = "qwen2.5:7b"
    
    # 自定义API配置
    custom_api_url: Optional[str] = None
    custom_api_key: Optional[str] = None
    
    # 临床解析服务配置
    use_clinical_parser: bool = True  # 是否使用临床解析服务
    clinical_parser_url: str = "http://localhost:8001"  # 临床解析服务URL
    clinical_parser_timeout: int = 10  # 超时时间（秒）
    
    # 降级配置
    enable_fallback: bool = True  # 是否启用降级方案
    fallback_threshold: float = 0.5  # 降级阈值（置信度低于此值时使用降级方案）
    
    # NER模型配置（阶段3）
    use_ner: bool = False  # 是否使用NER模型
    ner_model_name: Optional[str] = None  # NER模型名称或路径（如"bert-base-chinese"或本地路径）
    ner_device: Optional[str] = None  # NER模型设备（cpu/cuda），如果为None则自动选择
    use_onnx: bool = False  # 是否使用ONNX Runtime加速
    
    # 融合策略配置
    fusion_strategy: str = "weighted_vote"  # 融合策略：weighted_vote/majority_vote/max_confidence
    
    class Config:
        env_file = str(_env_file_path)  # 使用项目根目录的 .env 文件
        env_prefix = "NLU_"
        extra = "ignore"  # 忽略未定义的字段（因为.env文件可能包含其他服务的配置）


class Settings(BaseSettings):
    """应用配置"""
    app_name: str = "健康状态判定服务"
    app_version: str = "1.0.0"
    
    # NLU配置
    nlu: NLUConfig = NLUConfig()
    
    class Config:
        env_file = str(_env_file_path)  # 使用项目根目录的 .env 文件
        extra = "ignore"  # 忽略未定义的字段（因为.env文件可能包含其他服务的配置）

settings = Settings()

