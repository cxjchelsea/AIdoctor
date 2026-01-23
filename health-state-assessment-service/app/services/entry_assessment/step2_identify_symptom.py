"""
Step 2：识别用户是否"有症状/困扰"
AI诊断入口判定的第二步

判断结果：
- 情况 A：明确无症状（纯筛查/体检规划）
- 情况 B：存在症状/困扰（不论轻重）
- 情况 C：不确定/模糊/混合诉求
"""
from typing import Dict, Any, List


def identify_symptom(input_data: dict) -> dict:
    """
    识别用户是否"有症状/困扰"
    
    功能：
    - 能识别"症状/不适/困扰/异常感觉/功能变化"等表达
    - 轻微问题也算"有困扰"，不要求严重程度
    - 能识别"明确无症状"的表达（纯筛查/纯体检规划）
    
    Args:
        input_data: Step 1的输出，包含：
            - original_input: str
            - intent: str
            - entities: list
            - has_symptoms: bool
            - has_screening_intent: bool
        
    Returns:
        识别结果，包含：
            - status: str - "no_symptom" | "has_symptom" | "uncertain"
            - symptoms: list - 识别的症状列表
            - concerns: list - 识别的困扰列表
            - confidence: float - 识别置信度
    """
    intent = input_data.get("intent", "unknown")
    has_symptoms = input_data.get("has_symptoms", False)
    has_screening_intent = input_data.get("has_screening_intent", False)
    entities = input_data.get("entities", [])
    original_input = input_data.get("original_input", "")
    provided_symptoms = input_data.get("provided_symptoms", [])
    
    # 提取症状实体
    symptoms = []
    concerns = []
    
    # 从实体中提取症状
    for entity in entities:
        if entity.get("type") == "symptom":
            symptom_value = entity.get("value", "")
            if symptom_value:
                symptoms.append(symptom_value)
    
    # 从提供的症状列表中提取
    if provided_symptoms:
        symptoms.extend(provided_symptoms)
    
    # 去重
    symptoms = list(set(symptoms))
    
    # 判断情况
    if intent == "screening" and not has_symptoms:
        # 情况 A：明确无症状（纯筛查/体检规划）
        status = "no_symptom"
        confidence = 0.9
    elif intent == "diagnosis" and has_symptoms:
        # 情况 B：存在症状/困扰
        status = "has_symptom"
        confidence = 0.9
    elif intent == "mixed":
        # 情况 C：不确定/模糊/混合诉求
        status = "uncertain"
        confidence = 0.7
    elif intent == "unknown":
        # 情况 C：不确定
        status = "uncertain"
        confidence = 0.5
    else:
        # 根据症状存在性判断
        if has_symptoms or symptoms:
            status = "has_symptom"
            confidence = 0.8
        elif has_screening_intent:
            status = "no_symptom"
            confidence = 0.8
        else:
            status = "uncertain"
            confidence = 0.5
    
    # 提取困扰（心理、功能变化等）
    concern_keywords = ["担心", "害怕", "焦虑", "困扰", "疑问", "不确定"]
    for keyword in concern_keywords:
        if keyword in original_input:
            concerns.append(f"存在{keyword}情绪")
    
    return {
        "status": status,
        "symptoms": symptoms,
        "concerns": concerns,
        "confidence": confidence,
        "intent": intent,
        "original_input": original_input
    }

