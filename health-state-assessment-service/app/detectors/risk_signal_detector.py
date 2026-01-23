"""
风险信号检测器
使用早期风险信号识别库（0.2）
识别早期风险信号（家族史、行为、慢性暴露）
"""
from typing import Dict, Any, List


class RiskLevel:
    """风险等级"""
    L1 = "L1"  # 极高风险（需立即处理）
    L2 = "L2"  # 高风险（需尽快处理）
    L3 = "L3"  # 中风险（建议关注）
    L4 = "L4"  # 低风险（正常管理）


# 早期风险信号关键词（用于健康管理）
EARLY_RISK_SIGNALS = {
    "高血压风险": ["家族史", "高盐饮食", "肥胖", "缺乏运动"],
    "糖尿病风险": ["家族史", "肥胖", "多饮", "多尿"],
    "心血管风险": ["家族史", "高脂饮食", "吸烟", "饮酒"],
}


def early_risk_screening(
    basic_info: Dict[str, Any], 
    user_input: str = ""
) -> Dict[str, Any]:
    """
    早期风险信号识别
    
    使用早期风险信号识别库（0.2）
    识别早期风险信号（家族史、行为、慢性暴露）
    
    Args:
        basic_info: 基本信息，包含age, gender, bmi等
        user_input: 用户原始输入文本
    
    Returns:
        风险信号识别结果，包含：
            - risk_factors: list - 风险因素列表
            - risk_level: str - 风险等级（L1/L2/L3/L4）
            - family_history: bool - 是否有家族史
            - lifestyle_risks: list - 生活方式风险列表
    """
    risk_signals = {
        "risk_factors": [],
        "risk_level": RiskLevel.L4,  # 默认低风险
        "family_history": False,
        "lifestyle_risks": []
    }
    
    # 检查年龄
    age = basic_info.get("age")
    if age:
        if age >= 65:
            risk_signals["risk_factors"].append("高龄（65岁以上）")
            risk_signals["risk_level"] = RiskLevel.L3
        elif age >= 50:
            risk_signals["risk_factors"].append("中老年（50-65岁）")
    
    # 检查BMI
    bmi = basic_info.get("bmi")
    if bmi:
        if bmi >= 30:
            risk_signals["risk_factors"].append("肥胖（BMI≥30）")
            risk_signals["lifestyle_risks"].append("肥胖")
            if risk_signals["risk_level"] == RiskLevel.L4:
                risk_signals["risk_level"] = RiskLevel.L3
        elif bmi >= 25:
            risk_signals["risk_factors"].append("超重（BMI 25-30）")
            risk_signals["lifestyle_risks"].append("超重")
    
    # 检查家族史关键词
    if user_input:
        for risk_type, keywords in EARLY_RISK_SIGNALS.items():
            for keyword in keywords:
                if "家族" in keyword and keyword in user_input:
                    risk_signals["family_history"] = True
                    risk_signals["risk_factors"].append(f"{risk_type}家族史")
                    if risk_signals["risk_level"] == RiskLevel.L4:
                        risk_signals["risk_level"] = RiskLevel.L3
                elif keyword in user_input and keyword not in ["家族史"]:
                    # 生活方式风险
                    if keyword not in risk_signals["lifestyle_risks"]:
                        risk_signals["lifestyle_risks"].append(keyword)
    
    return risk_signals
