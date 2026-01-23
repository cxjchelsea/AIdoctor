"""
响应模型
"""
from typing import Dict, Any, List
from pydantic import BaseModel


class WorkupItem(BaseModel):
    """检查项"""
    test_name: str
    purpose: str
    priority: str
    expected_gain: float


class WorkupPlanResponse(BaseModel):
    """检查建议响应"""
    workupItems: List[WorkupItem] = []
    verificationPlan: Dict[str, Any] = {}
    priority: str = "normal"

