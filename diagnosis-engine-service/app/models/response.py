"""
响应模型
"""
from typing import Dict, Optional
from pydantic import BaseModel


class DiagnosisEngineResult(BaseModel):
    """诊断引擎响应"""
    possibilities: Dict[str, float]
    engine_results: Optional[Dict] = None

