"""
工作态判定规则（0.4）
定义工作态（健康管理态 vs 临床诊疗态）的判定规则
"""
from typing import Dict, Any, List
from app.detectors.symptom_severity_detector import SeverityLevel
from app.detectors.risk_signal_detector import RiskLevel


class WorkMode:
    """工作态"""
    CLINICAL_MODE = "clinical_mode"  # 临床诊疗态
    WELLNESS_MODE = "wellness_mode"  # 健康管理态


def determine_work_mode(
    severity: str,
    risk_signals: Dict[str, Any],
    red_flags: List[str],
    vital_signs: Dict[str, Any] = None
) -> str:
    """
    工作态判定
    
    使用工作态判定规则库（0.4）
    
    判定规则：
    1. 出现红旗信号 → 立即进入临床诊疗态
    2. 检查生命体征异常 → 进入临床诊疗态
    3. 症状严重程度高 → 进入临床诊疗态
    4. 风险等级≥L2 → 进入临床诊疗态
    5. 症状在正常范围且风险等级低（L3/L4） → 进入健康管理态
    6. 不确定时，优先进入临床诊疗态（宁可误报，不能漏报）
    
    Args:
        severity: 症状严重程度（NORMAL/LOW/MODERATE/HIGH）
        risk_signals: 风险信号识别结果
        red_flags: 红旗信号列表
        vital_signs: 生命体征（可选）
    
    Returns:
        工作态（clinical_mode / wellness_mode）
    """
    # 规则1：出现红旗信号 → 立即进入临床诊疗态
    if red_flags:
        return WorkMode.CLINICAL_MODE
    
    # 规则2：检查生命体征异常
    if vital_signs:
        bp = vital_signs.get("bp", {})
        if isinstance(bp, dict):
            systolic = bp.get("systolic")
            diastolic = bp.get("diastolic")
            if systolic and diastolic:
                # 高血压危象
                if systolic >= 180 or diastolic >= 120:
                    return WorkMode.CLINICAL_MODE
                # 低血压休克
                if systolic < 90:
                    return WorkMode.CLINICAL_MODE
        
        heart_rate = vital_signs.get("heartRate")
        if heart_rate:
            # 心动过速或过缓
            if heart_rate >= 120 or heart_rate <= 50:
                return WorkMode.CLINICAL_MODE
    
    # 规则3：症状严重程度高 → 进入临床诊疗态
    if severity == SeverityLevel.HIGH:
        return WorkMode.CLINICAL_MODE
    
    # 规则4：风险等级≥L2 → 进入临床诊疗态
    risk_level = risk_signals.get("risk_level", RiskLevel.L4)
    if risk_level in [RiskLevel.L1, RiskLevel.L2]:
        return WorkMode.CLINICAL_MODE
    
    # 规则5：症状严重程度中 + 风险等级L3 → 进入临床诊疗态
    if severity == SeverityLevel.MODERATE and risk_level == RiskLevel.L3:
        return WorkMode.CLINICAL_MODE
    
    # 规则6：症状在正常范围且风险等级低（L3/L4） → 进入健康管理态
    if severity in [SeverityLevel.NORMAL, SeverityLevel.LOW] and risk_level in [RiskLevel.L3, RiskLevel.L4]:
        return WorkMode.WELLNESS_MODE
    
    # 规则7：不确定时，优先进入临床诊疗态（宁可误报，不能漏报）
    return WorkMode.CLINICAL_MODE
