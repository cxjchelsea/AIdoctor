"""
健康状态判定请求模型
"""
from pydantic import BaseModel
from typing import List, Optional, Dict, Any

class HealthStateAssessmentRequest(BaseModel):
    """健康状态判定请求"""
    userId: str
    cdpId: Optional[str] = None  # CDP ID（如果诊断服务已创建CDP，传入此ID；否则服务会生成新的）
    userInput: Optional[str] = None  # 用户原始输入文本
    basicInfo: Optional[Dict[str, Any]] = None  # 基本信息：age, gender, bmi等
    symptoms: Optional[List[str]] = None  # 症状列表
    vitalSigns: Optional[Dict[str, Any]] = None  # 生命体征：bp, heartRate等
    
    class Config:
        json_schema_extra = {
            "example": {
                "userId": "user123",
                "cdpId": "cdp_xxxxxxxxxxxxx",  # 可选：诊断服务传入的CDP ID
                "userInput": "我最近胸痛",
                "basicInfo": {
                    "age": 45,
                    "gender": "male",
                    "bmi": 26.5
                },
                "symptoms": ["胸痛"],
                "vitalSigns": {
                    "bp": {"systolic": 130, "diastolic": 85},
                    "heartRate": 75
                }
            }
        }

