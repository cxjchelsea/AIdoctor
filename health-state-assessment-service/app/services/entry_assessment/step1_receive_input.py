"""
Step 1：接收用户输入
AI诊断入口判定的第一步
"""
from typing import Dict, Any
import re


def receive_user_input(user_input: dict) -> dict:
    """
    接收用户输入并进行自然语言理解（NLU）
    
    功能：
    - 支持自然语言理解（NLU）
    - 允许混合诉求（既想体检又有轻微不适）
    - 不依赖选择题入口
    
    Args:
        user_input: 用户输入数据，包含：
            - userInput: str - 用户原始输入文本
            - basicInfo: dict - 基本信息（可选）
            - symptoms: list - 症状列表（可选）
        
    Returns:
        结构化的用户意图表达，包含：
            - original_input: str - 原始输入
            - intent: str - 用户意图（screening/diagnosis/mixed/unknown）
            - entities: list - 提取的实体（症状、需求等）
            - has_symptoms: bool - 是否包含症状描述
            - has_screening_intent: bool - 是否包含筛查意图
    """
    original_input = user_input.get("userInput", "") or ""
    basic_info = user_input.get("basicInfo", {}) or {}
    provided_symptoms = user_input.get("symptoms", []) or []
    
    # 提取用户意图关键词
    screening_keywords = [
        "体检", "筛查", "检查", "健康管理", "预防", "健康评估",
        "想查", "需要检查", "做体检", "健康体检", "年度体检"
    ]
    
    symptom_keywords = [
        "痛", "疼", "不适", "难受", "不舒服", "异常", "问题",
        "症状", "困扰", "担心", "害怕", "有问题"
    ]
    
    # 检查是否包含筛查意图
    has_screening_intent = any(keyword in original_input for keyword in screening_keywords)
    
    # 检查是否包含症状描述
    # 注意：如果输入包含筛查关键词，需要更严格地判断症状
    # 避免将"健康状况"、"健康评估"等词误判为症状
    has_symptoms_text = False
    if has_screening_intent:
        # 如果有筛查意图，需要更严格地判断症状
        # 排除健康管理相关的词汇
        wellness_context_keywords = ["健康状况", "健康状态", "健康评估", "健康管理", "健康检查", "健康体检"]
        is_wellness_context = any(keyword in original_input for keyword in wellness_context_keywords)
        
        # 只有在明确包含症状关键词且不在健康管理上下文中时，才认为有症状
        if not is_wellness_context:
            has_symptoms_text = any(keyword in original_input for keyword in symptom_keywords)
    else:
        # 没有筛查意图时，正常判断症状
        has_symptoms_text = any(keyword in original_input for keyword in symptom_keywords)
    
    has_symptoms_list = len(provided_symptoms) > 0
    has_symptoms = has_symptoms_text or has_symptoms_list
    
    # 判断意图类型
    # 优先判断筛查意图，避免将健康管理需求误判为症状咨询
    if has_screening_intent and has_symptoms:
        # 如果同时有筛查意图和症状，需要进一步判断
        # 如果筛查意图很强（如"体检"、"筛查"），且症状不明显，优先认为是筛查
        strong_screening_keywords = ["体检", "筛查", "健康体检", "年度体检", "做体检"]
        has_strong_screening = any(keyword in original_input for keyword in strong_screening_keywords)
        if has_strong_screening and not has_symptoms_list:
            # 有强筛查意图且没有明确症状列表，优先认为是筛查
            intent = "screening"
            has_symptoms = False  # 重置症状标志
        else:
            intent = "mixed"  # 混合诉求
    elif has_screening_intent:
        intent = "screening"  # 纯筛查
    elif has_symptoms:
        intent = "diagnosis"  # 症状咨询
    else:
        intent = "unknown"  # 不确定
    
    # 提取实体（简单实现，实际应该使用NER模型）
    entities = []
    if provided_symptoms:
        entities.extend([{"type": "symptom", "value": s} for s in provided_symptoms])
    
    # 从文本中提取症状关键词
    # 注意：如果输入包含筛查关键词，需要更严格地提取症状实体
    # 避免将"健康状况"、"健康评估"等词误判为症状实体
    if not has_screening_intent or has_symptoms:
        # 只有在没有筛查意图，或者明确有症状时，才提取症状实体
        symptom_patterns = [
            r"([^，。！？\s]+(?:痛|疼|不适|难受|不舒服|异常))",
            r"([^，。！？\s]+(?:症状|困扰|问题))"
        ]
        for pattern in symptom_patterns:
            matches = re.findall(pattern, original_input)
            for match in matches:
                # 排除健康管理相关的词汇
                if "健康" not in match and "体检" not in match and "筛查" not in match:
                    if match not in [e["value"] for e in entities if e["type"] == "symptom"]:
                        entities.append({"type": "symptom", "value": match})
    
    return {
        "original_input": original_input,
        "intent": intent,
        "entities": entities,
        "has_symptoms": has_symptoms,
        "has_screening_intent": has_screening_intent,
        "basic_info": basic_info,
        "provided_symptoms": provided_symptoms
    }

