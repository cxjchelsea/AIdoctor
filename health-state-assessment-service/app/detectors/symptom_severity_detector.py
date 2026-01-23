"""
症状严重程度检测器
使用症状严重程度判定规则库（0.1）
"""
from typing import Dict, Any, List
from app.rules.severity_rules import SEVERITY_RULES, get_severity_level


class SeverityLevel:
    """症状严重程度等级"""
    NORMAL = "NORMAL"  # 正常范围
    LOW = "LOW"  # 轻度
    MODERATE = "MODERATE"  # 中度
    HIGH = "HIGH"  # 重度


# 高危症状关键词
HIGH_RISK_SYMPTOMS = {
    "胸痛": ["胸痛", "胸闷", "心绞痛", "心脏不适"],
    "呼吸困难": ["呼吸困难", "气促", "喘不上气", "窒息感"],
    "意识不清": ["意识不清", "昏迷", "晕厥", "意识模糊"],
    "剧烈头痛": ["剧烈头痛", "突发头痛", "爆炸样头痛"],
    "大出血": ["大出血", "咯血", "呕血", "便血"],
    "高热": ["高热", "40度以上"],
}

# 中等严重症状
MODERATE_SEVERITY_SYMPTOMS = [
    "胸痛", "腹痛", "头痛", "发热", "咳嗽", 
    "气短", "乏力", "恶心", "呕吐", "腹泻"
]


def assess_symptom_severity(
    symptoms: List[str], 
    user_input: str = ""
) -> Dict[str, Any]:
    """
    症状严重程度评估
    
    使用症状严重程度判定规则库（0.1）
    判断症状是否在正常范围
    
    Args:
        symptoms: 症状列表
        user_input: 用户原始输入文本
    
    Returns:
        严重程度评估结果，包含：
            - severity_level: str - 严重程度等级（NORMAL/LOW/MODERATE/HIGH）
            - score: float - 严重程度分数（0-1）
            - description: str - 严重程度描述
    """
    if not symptoms:
        return {
            "severity_level": SeverityLevel.NORMAL,
            "score": 0.0,
            "description": "无症状"
        }
    
    # 合并症状文本
    symptom_text = " ".join(symptoms) + " " + user_input
    
    # 检查是否包含高危症状
    for high_risk_key, keywords in HIGH_RISK_SYMPTOMS.items():
        for keyword in keywords:
            if any(keyword in symptom for symptom in symptoms):
                return {
                    "severity_level": SeverityLevel.HIGH,
                    "score": 1.0,
                    "description": f"检测到高危症状：{high_risk_key}"
                }
            if keyword in user_input:
                return {
                    "severity_level": SeverityLevel.HIGH,
                    "score": 1.0,
                    "description": f"检测到高危症状：{high_risk_key}"
                }
    
    # 检查是否包含中等严重症状
    moderate_count = 0
    for moderate_symptom in MODERATE_SEVERITY_SYMPTOMS:
        if any(moderate_symptom in symptom for symptom in symptoms):
            moderate_count += 1
        if moderate_symptom in user_input:
            moderate_count += 1
    
    if moderate_count > 0:
        # 根据症状数量计算分数
        score = min(0.3 + (moderate_count * 0.1), 0.9)
        severity_level = get_severity_level(score)
        
        # 映射到我们的严重程度等级
        if severity_level == "severe":
            level = SeverityLevel.HIGH
        elif severity_level == "moderate":
            level = SeverityLevel.MODERATE
        else:
            level = SeverityLevel.LOW
        
        return {
            "severity_level": level,
            "score": score,
            "description": f"检测到{moderate_count}个中等严重症状"
        }
    
    # 如果只有轻微症状，返回轻度
    if symptoms:
        return {
            "severity_level": SeverityLevel.LOW,
            "score": 0.2,
            "description": "检测到轻微症状"
        }
    
    return {
        "severity_level": SeverityLevel.NORMAL,
        "score": 0.0,
        "description": "无症状"
    }
