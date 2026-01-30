"""
响应模型
"""
from typing import Dict, Any, List, Optional
from pydantic import BaseModel


class Treatment(BaseModel):
    """治疗方案"""
    name: str  # 治疗方案名称
    description: str  # 治疗方案描述
    priority: str  # 优先级（high/medium/low）


class TreatmentPlan(BaseModel):
    """治疗计划"""
    primaryTreatment: Treatment  # 主要治疗方案
    alternativeTreatments: List[Treatment] = []  # 备选治疗方案


class MedicationAdvice(BaseModel):
    """药物建议"""
    name: str  # 药物名称
    indication: str  # 适应症
    contraindications: List[str] = []  # 禁忌症
    drugInteractions: List[str] = []  # 药物相互作用
    note: Optional[str] = None  # 注意事项（研发阶段不涉及具体剂量）


class NonMedicationAdvice(BaseModel):
    """非药物治疗建议"""
    type: str  # 建议类型（lifestyle/rehabilitation/other）
    content: str  # 建议内容
    priority: str  # 优先级


class TreatmentPlanResponse(BaseModel):
    """治疗建议响应"""
    cdpId: Optional[str] = None  # CDP ID
    treatmentPlan: TreatmentPlan  # 治疗计划
    medicationAdvice: List[MedicationAdvice]  # 药物推荐列表
    nonMedicationAdvice: List[NonMedicationAdvice]  # 非药物治疗建议列表

