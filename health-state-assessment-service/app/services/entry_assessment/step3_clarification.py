"""
Step 3：方向澄清（仅对"情况C"触发一次）
AI诊断入口判定的第三步

功能：
- 通过最小澄清确认用户当前的主要目标
- 澄清后不得反复追问，避免入口卡顿
- 澄清结果只能落到两类：进入 A 或进入 B
"""
from typing import Dict, Any, Optional


def clarify_direction(symptom_data: dict) -> Optional[dict]:
    """
    方向澄清
    
    触发条件：仅当 Step 2 判断为"情况C"（uncertain）时触发
    
    澄清目标：确认用户主要目标是：
    - A：健康筛查/体检规划
    - B：症状咨询/问题排查
    
    Args:
        symptom_data: Step 2的输出，包含：
            - status: str - "no_symptom" | "has_symptom" | "uncertain"
            - symptoms: list
            - concerns: list
            - intent: str
        
    Returns:
        澄清结果，包含：
            - direction: str - "A" | "B" | None（如果不需要澄清则返回None）
            - question: str - 澄清问题（如果需要澄清）
            - clarified: bool - 是否已澄清
            - confidence: float - 澄清置信度
        如果status不是"uncertain"，返回None
    """
    status = symptom_data.get("status", "")
    intent = symptom_data.get("intent", "")
    original_input = symptom_data.get("original_input", "")
    
    # 仅对"情况C"（uncertain）触发澄清
    if status != "uncertain":
        return None
    
    # 生成澄清问题
    clarification_question = "您当前的主要目标是健康筛查/体检规划，还是症状咨询/问题排查？"
    
    # 根据已有信息尝试推断方向（避免必须等待用户回答）
    # 如果症状明显，倾向于B路径
    symptoms = symptom_data.get("symptoms", [])
    has_screening_intent = "体检" in original_input or "筛查" in original_input or "检查" in original_input
    
    if symptoms and len(symptoms) > 0 and not has_screening_intent:
        # 有症状且没有明确筛查意图，倾向于B路径
        direction = "B"
        clarified = True
        confidence = 0.7
    elif has_screening_intent and (not symptoms or len(symptoms) == 0):
        # 有筛查意图且无明显症状，倾向于A路径
        direction = "A"
        clarified = True
        confidence = 0.7
    else:
        # 无法推断，需要用户澄清
        # 注意：在实际实现中，这里应该返回问题等待用户回答
        # 但为了流程顺畅，我们根据症状存在性做默认判断
        direction = "B" if symptoms else "A"
        clarified = False  # 标记为未澄清，实际应该等待用户回答
        confidence = 0.5
    
    return {
        "direction": direction,
        "question": clarification_question,
        "clarified": clarified,
        "confidence": confidence
    }

