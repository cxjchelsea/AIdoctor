"""
Step 4：危险信号检查（安全兜底，所有用户都要过一次）
AI诊断入口判定的第四步

功能：
- 无论用户走 A 还是 B，都必须判断是否存在危险信号
- 如果命中危险信号：不进入线上流程，输出安全提示并结束
- 如果未命中危险信号：进入 Step 5 输出选路结果
"""
from typing import Dict, Any, List
from app.rules.red_flag_rules import RED_FLAG_RULES


def check_red_flags(symptom_data: dict) -> dict:
    """
    检查危险信号（红旗信号）
    
    检查原则：
    - 入口判定阶段只做"是否命中"，不做解释诊断
    - 命中后输出固定的安全提示并结束线上流程
    
    Args:
        symptom_data: Step 2的输出，包含：
            - status: str
            - symptoms: list - 症状列表
            - original_input: str - 原始输入
        
    Returns:
        危险信号检查结果，包含：
            - red_flags_hit: bool - 是否命中危险信号
            - red_flags: list - 命中的危险信号列表
            - safety_message: str - 安全提示信息（如果命中）
            - should_exit: bool - 是否应该退出线上流程
    """
    symptoms = symptom_data.get("symptoms", [])
    original_input = symptom_data.get("original_input", "")
    
    # 合并症状列表和原始输入文本进行检测
    symptom_text = " ".join(symptoms) + " " + original_input
    
    detected_flags = []
    
    # 检查红旗信号库中的紧急症状
    for rule_type, rule_data in RED_FLAG_RULES.items():
        rule_symptoms = rule_data.get("symptoms", [])
        action = rule_data.get("action", "立即就医")
        
        for red_flag_symptom in rule_symptoms:
            # 检查症状列表中是否包含
            if any(red_flag_symptom in symptom for symptom in symptoms):
                detected_flags.append({
                    "type": rule_type,
                    "symptom": red_flag_symptom,
                    "action": action,
                    "description": f"{red_flag_symptom}（{action}）"
                })
            # 检查原始输入中是否包含
            elif red_flag_symptom in original_input:
                detected_flags.append({
                    "type": rule_type,
                    "symptom": red_flag_symptom,
                    "action": action,
                    "description": f"{red_flag_symptom}（{action}）"
                })
    
    # 检查高危症状组合
    high_risk_combinations = [
        (["胸痛", "气短", "出汗"], "心梗三联征"),
        (["剧烈头痛", "恶心", "呕吐"], "脑出血可能"),
        (["呼吸困难", "胸痛", "晕厥"], "肺栓塞可能"),
        (["胸痛", "大汗", "恶心"], "急性心梗可能"),
    ]
    
    for combination, description in high_risk_combinations:
        if all(keyword in symptom_text for keyword in combination):
            detected_flags.append({
                "type": "emergency",
                "symptom": description,
                "action": "立即就医",
                "description": f"高危症状组合：{', '.join(combination)}（{description}）"
            })
    
    # 去重
    seen = set()
    unique_flags = []
    for flag in detected_flags:
        flag_key = (flag["type"], flag["symptom"])
        if flag_key not in seen:
            seen.add(flag_key)
            unique_flags.append(flag)
    
    red_flags_hit = len(unique_flags) > 0
    
    # 生成安全提示信息
    safety_message = None
    if red_flags_hit:
        flag_descriptions = [flag["description"] for flag in unique_flags]
        safety_message = (
            f"检测到危险信号：{', '.join(flag_descriptions)}。"
            f"建议您优先线下就医或急诊，线上AI诊断不适合处理此类紧急情况。"
        )
    
    return {
        "red_flags_hit": red_flags_hit,
        "red_flags": unique_flags,
        "safety_message": safety_message,
        "should_exit": red_flags_hit  # 命中危险信号则退出线上流程
    }

