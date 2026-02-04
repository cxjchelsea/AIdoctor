"""
Step 1：接收用户输入
AI诊断入口判定的第一步
"""
from typing import Dict, Any, Optional
import asyncio
from app.services.nlu import IntentRecognizer, EntityExtractor
from app.utils.logger import logger


# 全局NLU模块实例（单例模式）
_intent_recognizer: Optional[IntentRecognizer] = None
_entity_extractor: Optional[EntityExtractor] = None


def _get_intent_recognizer() -> IntentRecognizer:
    """获取意图识别器（单例）"""
    global _intent_recognizer
    if _intent_recognizer is None:
        _intent_recognizer = IntentRecognizer()
    return _intent_recognizer


def _get_entity_extractor() -> EntityExtractor:
    """获取实体提取器（单例）"""
    global _entity_extractor
    if _entity_extractor is None:
        _entity_extractor = EntityExtractor()
    return _entity_extractor


async def receive_user_input_async(user_input: dict) -> dict:
    """
    接收用户输入并进行自然语言理解（NLU）- 异步版本
    
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
    user_id = user_input.get("userId", "default_user")
    session_id = user_input.get("sessionId", f"session_{user_id}")
    
    try:
        # 使用新的NLU模块进行意图识别和实体提取
        intent_recognizer = _get_intent_recognizer()
        entity_extractor = _get_entity_extractor()
        
        # 异步调用
        logger.info(f"开始NLU处理: user_input={original_input[:100]}...")
        intent_result = await intent_recognizer.recognize(original_input)
        logger.info(
            f"意图识别完成: intent={intent_result.intent}, "
            f"confidence={intent_result.confidence:.2f}"
        )
        
        entity_result = await entity_extractor.extract(
            original_input,
            intent_result.intent,
            provided_symptoms,
            basic_info,
            user_id=user_id,
            session_id=session_id
        )
        logger.info(
            f"实体提取完成: 症状数={len(entity_result.symptoms)}, "
            f"实体数={len(entity_result.entities)}"
        )
        
        # 构造返回结果（保持接口兼容性）
        return {
            "original_input": original_input,
            "intent": intent_result.intent,
            "entities": entity_result.entities,
            "has_symptoms": intent_result.has_symptom_intent or len(entity_result.symptoms) > 0,
            "has_screening_intent": intent_result.has_screening_intent,
            "basic_info": entity_result.basic_info or basic_info,
            "provided_symptoms": provided_symptoms,
            # 新增字段（向后兼容）
            "intent_confidence": intent_result.confidence,
            "intent_reasoning": intent_result.reasoning,
            "symptoms": [s.standard_term for s in entity_result.symptoms],
            "vital_signs": entity_result.vital_signs,
            "temporal_info": entity_result.temporal_info
        }
    except Exception as e:
        logger.error(f"NLU处理失败，使用降级方案: {e}", exc_info=True)
        # 降级到原来的关键词匹配方案
        return _process_with_fallback(original_input, basic_info, provided_symptoms)


def receive_user_input(user_input: dict) -> dict:
    """
    接收用户输入（同步版本，保持向后兼容）
    
    注意：此函数内部会尝试使用异步NLU模块，如果失败则降级到关键词匹配
    建议在异步环境中直接使用receive_user_input_async
    
    Args:
        user_input: 用户输入数据
        
    Returns:
        结构化的用户意图表达
    """
    original_input = user_input.get("userInput", "") or ""
    basic_info = user_input.get("basicInfo", {}) or {}
    provided_symptoms = user_input.get("symptoms", []) or []
    user_id = user_input.get("userId", "default_user")
    session_id = user_input.get("sessionId", f"session_{user_id}")
    
    try:
        # 尝试在同步环境中运行异步代码
        try:
            # 检查是否有运行中的事件循环
            loop = asyncio.get_running_loop()
            # 如果有运行中的事件循环，不能使用asyncio.run，降级到关键词匹配
            logger.warning("检测到运行中的事件循环，使用降级方案")
            return _process_with_fallback(original_input, basic_info, provided_symptoms)
        except RuntimeError:
            # 没有运行中的事件循环，可以使用asyncio.run
            pass
        
        # 使用新的NLU模块进行意图识别和实体提取
        intent_recognizer = _get_intent_recognizer()
        entity_extractor = _get_entity_extractor()
        
        # 同步调用异步函数
        intent_result = asyncio.run(intent_recognizer.recognize(original_input))
        entity_result = asyncio.run(
            entity_extractor.extract(
                original_input,
                intent_result.intent,
                provided_symptoms,
                basic_info,
                user_id=user_id,
                session_id=session_id
            )
        )
        
        # 构造返回结果（保持接口兼容性）
        return {
            "original_input": original_input,
            "intent": intent_result.intent,
            "entities": entity_result.entities,
            "has_symptoms": intent_result.has_symptom_intent or len(entity_result.symptoms) > 0,
            "has_screening_intent": intent_result.has_screening_intent,
            "basic_info": entity_result.basic_info or basic_info,
            "provided_symptoms": provided_symptoms,
            # 新增字段（向后兼容）
            "intent_confidence": intent_result.confidence,
            "intent_reasoning": intent_result.reasoning,
            "symptoms": [s.standard_term for s in entity_result.symptoms],
            "vital_signs": entity_result.vital_signs,
            "temporal_info": entity_result.temporal_info
        }
    except Exception as e:
        logger.error(f"NLU处理失败，使用降级方案: {e}", exc_info=True)
        # 降级到原来的关键词匹配方案
        return _process_with_fallback(original_input, basic_info, provided_symptoms)


def _process_with_fallback(
    original_input: str,
    basic_info: Dict[str, Any],
    provided_symptoms: list
) -> dict:
    """
    降级处理方案（使用关键词匹配）
    
    Args:
        original_input: 用户原始输入
        basic_info: 基本信息
        provided_symptoms: 已提供的症状列表
        
    Returns:
        结构化的用户意图表达
    """
    from app.services.nlu.fallback_matcher import FallbackMatcher
    
    # 使用降级匹配器
    intent_result = FallbackMatcher.match_intent(original_input)
    entities = FallbackMatcher.extract_entities(original_input, intent_result["intent"])
    
    # 添加已提供的症状
    if provided_symptoms:
        for symptom in provided_symptoms:
            if symptom not in [e.get("value") for e in entities if e.get("type") == "symptom"]:
                entities.append({"type": "symptom", "value": symptom})
    
    return {
        "original_input": original_input,
        "intent": intent_result["intent"],
        "entities": entities,
        "has_symptoms": intent_result["has_symptom_intent"] or len(provided_symptoms) > 0,
        "has_screening_intent": intent_result["has_screening_intent"],
        "basic_info": basic_info,
        "provided_symptoms": provided_symptoms
    }

