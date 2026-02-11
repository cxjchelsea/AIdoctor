"""
Step 2：识别用户是否"有症状/困扰"
AI诊断入口判定的第二步

判断结果：
- 情况 A：明确无症状（纯筛查/体检规划）
- 情况 B：存在症状/困扰（不论轻重）
- 情况 C：不确定/模糊/混合诉求
"""
from typing import Dict, Any
import asyncio
from app.services.entry_assessment.step2_enhanced import (
    SymptomConcernIdentifier,
    SituationJudger
)
from app.utils.logger import logger


def identify_symptom(input_data: dict) -> dict:
    """
    识别用户是否"有症状/困扰"（同步接口，保持向后兼容）
    
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
    try:
        # 使用异步实现
        loop = asyncio.get_event_loop()
        if loop.is_running():
            # 如果事件循环已经在运行，使用线程池执行
            import concurrent.futures
            with concurrent.futures.ThreadPoolExecutor() as executor:
                future = executor.submit(asyncio.run, identify_symptom_async(input_data))
                return future.result()
        else:
            return loop.run_until_complete(identify_symptom_async(input_data))
    except RuntimeError:
        # 如果没有事件循环，创建新的
        return asyncio.run(identify_symptom_async(input_data))


async def identify_symptom_async(input_data: dict) -> dict:
    """
    识别用户是否"有症状/困扰"（异步接口）
    
    Args:
        input_data: Step 1的输出
    
    Returns:
        识别结果
    """
    try:
        # 使用新的细化模块
        identifier = SymptomConcernIdentifier()
        judger = SituationJudger()
        
        # 步骤1：识别症状/困扰
        symptom_result = await identifier.identify(input_data)
        
        # 步骤2：判断情况
        judgment_result = await judger.judge(symptom_result, input_data)
        
        # 转换为兼容格式
        return {
            "status": judgment_result.status,
            "symptoms": [s.original_text for s in judgment_result.symptoms],
            "concerns": [c.description for c in judgment_result.concerns],
            "confidence": judgment_result.confidence,
            "reasoning": judgment_result.reasoning,
            "intent": input_data.get("intent", "unknown"),
            "original_input": input_data.get("original_input", ""),
            # 新增字段（向后兼容）
            "symptom_details": [
                {
                    "original_text": s.original_text,
                    "standard_term": s.standard_term,
                    "severity": s.severity,
                    "location": s.location,
                    "temporal_info": s.temporal_info
                }
                for s in judgment_result.symptoms
            ],
            "concern_details": [
                {
                    "type": c.type,
                    "description": c.description,
                    "severity": c.severity
                }
                for c in judgment_result.concerns
            ],
            "context_analysis": judgment_result.context_analysis
        }
    except Exception as e:
        logger.error(f"Step 2识别失败，使用降级方案: {e}", exc_info=True)
        # 降级到原来的简单规则判断
        return _fallback_identify_symptom(input_data)


def _fallback_identify_symptom(input_data: dict) -> dict:
    """
    降级方案：基于简单规则的识别（原实现）
    
    Args:
        input_data: Step 1的输出
    
    Returns:
        识别结果
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
