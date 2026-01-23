"""
健康管理计划生成
使用健康管理计划生成规则（0.5）
"""
from typing import Dict, Any
from app.models.response import WellnessPlan
from app.detectors.risk_signal_detector import RiskLevel


def generate_wellness_plan(
    risk_signals: Dict[str, Any],
    basic_info: Dict[str, Any] = None
) -> WellnessPlan:
    """
    生成健康管理计划
    
    使用健康管理计划生成规则（0.5）
    仅在健康管理态时调用
    
    Args:
        risk_signals: 风险信号识别结果
        basic_info: 基本信息（可选）
    
    Returns:
        健康管理计划
    """
    risk_factors = risk_signals.get("risk_factors", [])
    lifestyle_risks = risk_signals.get("lifestyle_risks", [])
    risk_level = risk_signals.get("risk_level", RiskLevel.L4)
    
    # 风险识别
    if risk_factors:
        risk_management = f"识别到以下风险因素：{', '.join(risk_factors)}"
    else:
        risk_management = "当前健康状况良好，建议保持良好生活习惯"
    
    # 生活方式建议
    lifestyle_advice_parts = []
    
    if "肥胖" in lifestyle_risks or "超重" in lifestyle_risks:
        lifestyle_advice_parts.append("控制体重，建议低盐低脂饮食、适量运动")
    
    if risk_signals.get("family_history"):
        lifestyle_advice_parts.append("鉴于家族史，建议定期体检，关注相关指标")
    
    if not lifestyle_advice_parts:
        lifestyle_advice_parts.append("保持健康饮食、规律运动、充足睡眠")
    
    lifestyle_advice = "；".join(lifestyle_advice_parts)
    
    # 随访计划
    if risk_level == RiskLevel.L3:
        follow_up_plan = "建议3个月后复查相关指标"
    else:
        follow_up_plan = "建议6个月后复查，或出现新症状时及时咨询"
    
    return WellnessPlan(
        riskManagement=risk_management,
        lifestyleAdvice=lifestyle_advice,
        followUpPlan=follow_up_plan
    )
