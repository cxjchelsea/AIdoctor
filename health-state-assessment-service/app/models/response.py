"""
健康状态判定响应模型
"""
from pydantic import BaseModel
from typing import List, Optional, Dict, Any

class WellnessPlan(BaseModel):
    """健康管理计划"""
    riskManagement: str  # 风险识别
    lifestyleAdvice: str  # 生活方式建议
    followUpPlan: str  # 随访计划

class EntryAssessmentResult(BaseModel):
    """入口判定结果"""
    userInput: str
    hasSymptom: bool
    symptomStatus: str  # "no_symptom" | "has_symptom" | "uncertain"
    symptoms: List[str] = []
    concerns: List[str] = []
    clarificationNeeded: bool
    clarificationResult: Optional[Dict[str, Any]] = None
    redFlagsHit: bool
    redFlagsList: List[str] = []
    pathSelected: str  # "A" | "B" | "exit"

class HealthStateAssessmentResponse(BaseModel):
    """健康状态判定响应"""
    entryAssessment: Optional[EntryAssessmentResult] = None  # 入口判定结果（P0模块）
    needsClinicalMode: bool
    workMode: str  # "clinical_mode" or "wellness_mode"
    riskLevel: str  # "L1", "L2", "L3", "L4"
    assessmentReason: str
    redFlags: Optional[List[str]] = []  # 红旗信号列表
    wellnessPlan: Optional[WellnessPlan] = None  # 健康管理计划（仅在wellness_mode时返回）
    cdpId: str
    
    class Config:
        json_schema_extra = {
            "example": {
                "entryAssessment": {
                    "userInput": "我最近胸痛",
                    "hasSymptom": True,
                    "symptomStatus": "has_symptom",
                    "symptoms": ["胸痛"],
                    "clarificationNeeded": False,
                    "redFlagsHit": False,
                    "redFlagsList": [],
                    "pathSelected": "B"
                },
                "needsClinicalMode": True,
                "workMode": "clinical_mode",
                "riskLevel": "L2",
                "assessmentReason": "症状严重程度高，建议进入临床诊疗态",
                "redFlags": [],
                "cdpId": "cdp_123456"
            }
        }

