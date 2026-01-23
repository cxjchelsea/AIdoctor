"""
请求模型
"""
from typing import Dict, Any
from pydantic import BaseModel


class RiskAssessmentRequest(BaseModel):
    """风险评估请求"""
    cdp: Dict[str, Any]
    patient_state: Dict[str, Any] = {}
    ddx: list = []

