"""
红旗信号检测器
使用红旗信号库（0.3）
识别高危症状组合
"""
from typing import Dict, Any, List
from app.rules.red_flag_rules import RED_FLAG_RULES


def detect_red_flags(
    symptoms: List[str], 
    user_input: str = ""
) -> List[str]:
    """
    红旗信号识别
    
    使用红旗信号库（0.3）
    识别高危症状组合
    
    Args:
        symptoms: 症状列表
        user_input: 用户原始输入文本
    
    Returns:
        检测到的红旗信号列表（字符串描述）
    """
    red_flags = []
    
    # 合并症状文本
    symptom_text = " ".join(symptoms) + " " + user_input
    
    # 检查高危症状
    detected_high_risk = []
    for rule_type, rule_data in RED_FLAG_RULES.items():
        rule_symptoms = rule_data.get("symptoms", [])
        action = rule_data.get("action", "立即就医")
        
        for red_flag_symptom in rule_symptoms:
            # 检查症状列表中是否包含
            if any(red_flag_symptom in symptom for symptom in symptoms):
                detected_high_risk.append({
                    "symptom": red_flag_symptom,
                    "action": action
                })
                break
            # 检查原始输入中是否包含
            elif red_flag_symptom in user_input:
                detected_high_risk.append({
                    "symptom": red_flag_symptom,
                    "action": action
                })
                break
    
    # 检查高危症状组合
    high_risk_combinations = [
        (["胸痛", "气短", "出汗"], "心梗三联征"),
        (["剧烈头痛", "恶心", "呕吐"], "脑出血可能"),
        (["呼吸困难", "胸痛", "晕厥"], "肺栓塞可能"),
        (["胸痛", "大汗", "恶心"], "急性心梗可能"),
    ]
    
    for combination, description in high_risk_combinations:
        if all(keyword in symptom_text for keyword in combination):
            red_flags.append(f"高危症状组合：{', '.join(combination)}（{description}）")
    
    # 单独的高危症状也标记为红旗信号
    for risk in detected_high_risk:
        symptom = risk["symptom"]
        action = risk["action"]
        flag_desc = f"高危症状：{symptom}（{action}）"
        if flag_desc not in red_flags:
            red_flags.append(flag_desc)
    
    return red_flags
