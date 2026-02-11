"""
Step 3：方向澄清（仅对"情况C"触发一次）
AI诊断入口判定的第三步

功能：
- 通过最小澄清确认用户当前的主要目标
- 澄清后不得反复追问，避免入口卡顿
- 澄清结果只能落到两类：进入 A 或进入 B
"""
from typing import Dict, Any, Optional
import asyncio
from app.services.entry_assessment.step3_enhanced import ClarificationManager
from app.utils.logger import logger


# 全局澄清管理器实例（单例模式）
_clarification_manager: Optional[ClarificationManager] = None


def _get_clarification_manager() -> ClarificationManager:
    """获取澄清管理器（单例）"""
    global _clarification_manager
    if _clarification_manager is None:
        _clarification_manager = ClarificationManager()
    return _clarification_manager


async def clarify_direction_async(
    symptom_data: dict,
    nlu_result: Optional[dict] = None,
    user_answer: Optional[str] = None
) -> Optional[dict]:
    """
    方向澄清（异步版本）
    
    触发条件：仅当 Step 2 判断为"情况C"（uncertain）时触发
    
    澄清目标：确认用户主要目标是：
    - A：健康筛查/体检规划
    - B：症状咨询/问题排查
    
    Args:
        symptom_data: Step 2的输出，包含：
            - status: str - "no_symptom" | "has_symptom" | "uncertain"
            - symptoms: list
            - concerns: list
            - original_input: str
            - confidence: float
            - reasoning: str
        nlu_result: Step 1的NLU结果（可选）
        user_answer: 用户回答（可选，如果有则直接处理，否则返回问题）
        
    Returns:
        澄清结果，包含：
            - direction: str - "A" | "B" | None（如果不需要澄清或等待回答则返回None）
            - question: str - 澄清问题（如果需要澄清）
            - question_type: str - 问题类型（如果需要澄清）
            - options: list - 问题选项（如果需要澄清）
            - clarified: bool - 是否已澄清
            - confidence: float - 澄清置信度
            - reasoning: str - 澄清理由
        如果status不是"uncertain"，返回None
    """
    try:
        manager = _get_clarification_manager()
        result = await manager.clarify(symptom_data, nlu_result, user_answer)
        return result
    except Exception as e:
        logger.error(f"澄清流程失败: {e}", exc_info=True)
        # 降级到简单推断
        return _fallback_clarify(symptom_data)


def clarify_direction(
    symptom_data: dict,
    nlu_result: Optional[dict] = None,
    user_answer: Optional[str] = None
) -> Optional[dict]:
    """
    方向澄清（同步版本，保持向后兼容）
    
    注意：此函数内部会尝试使用异步澄清模块，如果失败则降级到简单推断
    建议在异步环境中直接使用clarify_direction_async
    
    Args:
        symptom_data: Step 2的输出
        nlu_result: Step 1的NLU结果（可选）
        user_answer: 用户回答（可选）
        
    Returns:
        澄清结果
    """
    try:
        # 尝试在同步环境中运行异步代码
        try:
            # 检查是否有运行中的事件循环
            loop = asyncio.get_running_loop()
            # 如果有运行中的事件循环，不能使用asyncio.run，降级到简单推断
            logger.warning("检测到运行中的事件循环，使用降级方案")
            return _fallback_clarify(symptom_data)
        except RuntimeError:
            # 没有运行中的事件循环，可以使用asyncio.run
            pass
        
        # 使用新的澄清模块
        manager = _get_clarification_manager()
        result = asyncio.run(manager.clarify(symptom_data, nlu_result, user_answer))
        return result
    except Exception as e:
        logger.error(f"澄清流程失败，使用降级方案: {e}", exc_info=True)
        # 降级到简单推断
        return _fallback_clarify(symptom_data)


def _fallback_clarify(symptom_data: dict) -> Optional[dict]:
    """
    降级处理方案（使用简单推断）
    
    Args:
        symptom_data: Step 2的输出
        
    Returns:
        澄清结果
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
        "confidence": confidence,
        "reasoning": "使用降级方案进行简单推断"
    }

