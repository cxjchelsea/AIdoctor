"""
请求模型
"""
from typing import Dict, Any
from pydantic import BaseModel


class TreatmentPlanRequest(BaseModel):
    """治疗建议请求"""
    cdp: Dict[str, Any]
    ddx: list = []
    patient_state: Dict[str, Any] = {}

