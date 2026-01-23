"""
红旗信号库（0.3）
定义需要紧急处理的危险信号
"""

# 红旗信号库
RED_FLAG_RULES = {
    "emergency": {
        "symptoms": [
            "胸痛",
            "呼吸困难",
            "意识丧失",
            "严重外伤"
        ],
        "action": "立即就医"
    },
    "urgent": {
        "symptoms": [
            "高烧不退",
            "剧烈头痛",
            "严重腹痛"
        ],
        "action": "尽快就医"
    }
}


def check_red_flags(symptom_list: list) -> list:
    """
    检查是否存在红旗信号
    
    Args:
        symptom_list: 症状列表
        
    Returns:
        检测到的红旗信号列表
    """
    detected_flags = []
    for rule_type, rule_data in RED_FLAG_RULES.items():
        for symptom in symptom_list:
            if symptom in rule_data["symptoms"]:
                detected_flags.append({
                    "type": rule_type,
                    "symptom": symptom,
                    "action": rule_data["action"]
                })
    return detected_flags

