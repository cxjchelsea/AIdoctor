"""
请求模型
"""
from typing import Dict, List, Optional
from pydantic import BaseModel


class DiagnosisEngineRequest(BaseModel):
    """诊断引擎请求"""
    symptom_info: Optional[Dict] = None
    vital_signs: Optional[Dict] = None
    examination_results: Optional[List[Dict]] = None
    health_profile: Optional[Dict] = None
