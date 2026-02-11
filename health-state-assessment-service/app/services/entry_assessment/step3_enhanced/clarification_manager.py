"""
澄清管理器
管理澄清流程的完整生命周期，协调问题生成和结果处理
"""
from typing import Dict, Any, Optional
from app.models.entry_assessment import ClarificationQuestion, ClarificationResult
from .question_generator import ClarificationQuestionGenerator
from .result_processor import ClarificationResultProcessor
from app.utils.logger import logger


class ClarificationManager:
    """澄清管理器"""
    
    def __init__(
        self,
        question_generator: Optional[ClarificationQuestionGenerator] = None,
        result_processor: Optional[ClarificationResultProcessor] = None
    ):
        """
        初始化澄清管理器
        
        Args:
            question_generator: 澄清问题生成器，如果为None则自动创建
            result_processor: 澄清结果处理器，如果为None则自动创建
        """
        self.question_generator = question_generator or ClarificationQuestionGenerator()
        self.result_processor = result_processor or ClarificationResultProcessor()
    
    async def generate_question(
        self,
        symptom_data: Dict[str, Any],
        nlu_result: Optional[Dict[str, Any]] = None
    ) -> Optional[ClarificationQuestion]:
        """
        生成澄清问题
        
        Args:
            symptom_data: Step 2的输出
            nlu_result: Step 1的NLU结果（可选）
        
        Returns:
            ClarificationQuestion: 澄清问题，如果不需要澄清则返回None
        """
        # 检查是否需要澄清
        status = symptom_data.get("status", "")
        if status != "uncertain":
            logger.info(f"状态为{status}，不需要澄清")
            return None
        
        try:
            # 生成澄清问题
            question = await self.question_generator.generate(symptom_data, nlu_result)
            logger.info(f"澄清问题生成成功: {question.question[:50]}...")
            return question
        except Exception as e:
            logger.error(f"生成澄清问题失败: {e}", exc_info=True)
            return None
    
    async def process_answer(
        self,
        user_answer: str,
        question: ClarificationQuestion,
        symptom_data: Optional[Dict[str, Any]] = None,
        nlu_result: Optional[Dict[str, Any]] = None
    ) -> ClarificationResult:
        """
        处理用户回答
        
        Args:
            user_answer: 用户回答文本
            question: 澄清问题
            symptom_data: Step 2的输出（可选）
            nlu_result: Step 1的NLU结果（可选）
        
        Returns:
            ClarificationResult: 澄清结果
        """
        try:
            # 处理用户回答
            result = await self.result_processor.process(
                user_answer=user_answer,
                question=question.question,
                options=[
                    {
                        "id": opt.id,
                        "text": opt.text,
                        "description": opt.description
                    }
                    for opt in question.options
                ],
                symptom_data=symptom_data,
                nlu_result=nlu_result
            )
            logger.info(
                f"澄清结果处理成功: direction={result.direction}, "
                f"confidence={result.confidence:.2f}"
            )
            return result
        except Exception as e:
            logger.error(f"处理澄清结果失败: {e}", exc_info=True)
            # 返回默认结果
            return ClarificationResult(
                direction="B",  # 默认方向B（安全优先）
                confidence=0.3,
                reasoning=f"处理澄清结果失败: {str(e)}",
                clarified=False,
                user_answer=user_answer
            )
    
    async def clarify(
        self,
        symptom_data: Dict[str, Any],
        nlu_result: Optional[Dict[str, Any]] = None,
        user_answer: Optional[str] = None
    ) -> Optional[Dict[str, Any]]:
        """
        完整的澄清流程（兼容旧接口）
        
        Args:
            symptom_data: Step 2的输出
            nlu_result: Step 1的NLU结果（可选）
            user_answer: 用户回答（可选，如果有则直接处理，否则返回问题）
        
        Returns:
            Dict: 澄清结果，包含：
                - direction: str - "A" | "B" | None
                - question: str - 澄清问题（如果需要澄清）
                - clarified: bool - 是否已澄清
                - confidence: float - 澄清置信度
                如果不需要澄清或已澄清，返回None
        """
        # 检查是否需要澄清
        status = symptom_data.get("status", "")
        if status != "uncertain":
            return None
        
        # 如果提供了用户回答，直接处理
        if user_answer:
            # 生成问题（用于验证）
            question = await self.generate_question(symptom_data, nlu_result)
            if not question:
                return None
            
            # 处理回答
            result = await self.process_answer(user_answer, question, symptom_data, nlu_result)
            
            return {
                "direction": result.direction,
                "question": question.question,
                "clarified": result.clarified,
                "confidence": result.confidence,
                "reasoning": result.reasoning,
                "user_answer": result.user_answer
            }
        else:
            # 只生成问题，等待用户回答
            question = await self.generate_question(symptom_data, nlu_result)
            if not question:
                return None
            
            return {
                "direction": None,
                "question": question.question,
                "question_type": question.question_type,
                "options": [
                    {
                        "id": opt.id,
                        "text": opt.text,
                        "description": opt.description
                    }
                    for opt in question.options
                ],
                "clarified": False,
                "confidence": 0.0,
                "reasoning": question.reasoning
            }

