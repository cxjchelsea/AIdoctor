"""
分诊引擎
"""
from typing import Dict, Any, List
import logging
from app.services.risk_assessment_engine import RiskAssessmentEngine

logger = logging.getLogger(__name__)


class TriageEngine:
    """分诊引擎"""
    
    def __init__(self):
        """初始化分诊引擎"""
        self.risk_assessment_engine = RiskAssessmentEngine()
    
    def _identify_risk_factors(
        self,
        patient_state: Dict[str, Any],
        ddx: List[Dict[str, Any]]
    ) -> List[str]:
        """
        识别风险因素
        
        Args:
            patient_state: 患者状态
            ddx: 鉴别诊断列表
            
        Returns:
            风险因素列表
        """
        risk_factors = []
        
        # 从症状中提取风险因素
        symptoms = patient_state.get("symptoms", [])
        risk_factors.extend(symptoms)
        
        # 从生命体征中提取异常指标
        vital_signs = patient_state.get("vital_signs", {})
        if vital_signs.get("heart_rate"):
            heart_rate = vital_signs.get("heart_rate")
            if heart_rate > 100:
                risk_factors.append("心率快")
            elif heart_rate < 60:
                risk_factors.append("心率慢")
        
        if vital_signs.get("bp"):
            bp = vital_signs.get("bp", {})
            systolic = bp.get("systolic", 0)
            if systolic > 140:
                risk_factors.append("血压高")
        
        # 从诊断中提取风险因素
        for ddx_item in ddx:
            disease = ddx_item.get("disease", "")
            probability = ddx_item.get("probability", 0.0)
            if probability > 0.5:
                risk_factors.append(disease)
        
        return risk_factors
    
    def _determine_triage_level(self, risk_level: str, urgency: str) -> str:
        """
        确定分诊级别
        
        Args:
            risk_level: 风险等级
            urgency: 紧急程度
            
        Returns:
            分诊级别（emergency/urgent/routine）
        """
        # 风险等级L1或紧急程度为emergency -> emergency
        if risk_level == "L1" or urgency == "emergency":
            return "emergency"
        
        # 风险等级L2或紧急程度为urgent -> urgent
        if risk_level == "L2" or urgency == "urgent":
            return "urgent"
        
        # 其他情况 -> routine
        return "routine"
    
    def triage(
            self,
            patient_state: Dict[str, Any],
            ddx: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
        分诊评估
        评估患者的紧急程度和分诊级别
        
        Args:
            patient_state: 患者状态
            ddx: 鉴别诊断列表
            
        Returns:
            分诊结果
        """
        logger.info("进行分诊评估")
        
        # 构建临时CDP进行风险评估
        temp_cdp = {
            "patient_state": patient_state,
            "ddx": ddx
        }
        
        # 进行风险评估
        risk_result = self.risk_assessment_engine.assess_risk(temp_cdp)
        
        risk_level = risk_result.get("riskLevel", "L3")
        urgency = risk_result.get("urgency", "routine")
        red_flags = risk_result.get("redFlags", [])
        
        # 识别风险因素
        risk_factors = self._identify_risk_factors(patient_state, ddx)
        
        # 确定分诊级别
        triage_level = self._determine_triage_level(risk_level, urgency)
        
        return {
            "risk_level": risk_level,
            "urgency": urgency,
            "triage_level": triage_level,
            "red_flags": red_flags,
            "risk_factors": risk_factors
        }

