"""
响应模型
"""
from typing import Dict, Any, List
from pydantic import BaseModel


class MedicationAdvice(BaseModel):
    """用药建议"""
    medication_type: str
    medication_name: str
    # 注意：研发阶段不涉及具体剂量


class TreatmentPlanResponse(BaseModel):
    """治疗建议响应"""
    treatmentPlan: Dict[str, Any] = {}
    medicationAdvice: List[MedicationAdvice] = []
    nonMedicationAdvice: List[str] = []

