"""
响应模型
"""
from typing import Dict, Any, List
from pydantic import BaseModel


class RedFlag(BaseModel):
    """红旗信号"""
    flag_type: str
    description: str
    severity: str


class RiskAssessmentResponse(BaseModel):
    """风险评估响应"""
    riskLevel: str  # L1/L2/L3/L4
    severity: str  # mild/moderate/severe
    urgency: str  # urgent/normal/non-urgent
    redFlags: List[RedFlag] = []
    reviewPlan: Dict[str, Any] = {}

