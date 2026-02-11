"""
情况判断器
基于症状/困扰识别结果判断三种情况（A/B/C）
"""
from typing import Dict, Any, Optional
from app.models.entry_assessment import (
    SituationJudgmentResult,
    SymptomIdentificationResult
)
from app.services.nlu.prompt_manager import PromptManager
from app.services.nlu.response_parser import ResponseParser
from app.utils.llm_client import LangChainLLMClient
from app.utils.logger import logger


class SituationJudger:
    """情况判断器"""
    
    def __init__(self, llm_client: Optional[LangChainLLMClient] = None):
        """
        初始化判断器
        
        Args:
            llm_client: LLM客户端，如果为None则自动创建
        """
        self.llm_client = llm_client or LangChainLLMClient()
        self.response_parser = ResponseParser()
    
    async def judge(
        self,
        symptom_result: SymptomIdentificationResult,
        nlu_result: Dict[str, Any]
    ) -> SituationJudgmentResult:
        """
        判断情况（A/B/C）
        
        Args:
            symptom_result: 症状/困扰识别结果
            nlu_result: Step 1的NLU结果
        
        Returns:
            SituationJudgmentResult: 判断结果
        """
        # 首先基于规则进行判断
        rule_result = self._judge_with_rules(symptom_result, nlu_result)
        
        # 如果情况复杂（混合诉求、置信度低），使用LLM进行深度判断
        intent = nlu_result.get("intent", "unknown")
        if intent == "mixed" or rule_result.confidence < 0.7:
            try:
                llm_result = await self._judge_with_llm(symptom_result, nlu_result)
                if llm_result and llm_result.confidence > rule_result.confidence:
                    return llm_result
            except Exception as e:
                logger.warning(f"LLM判断失败，使用规则判断结果: {e}")
        
        return rule_result
    
    def _judge_with_rules(
        self,
        symptom_result: SymptomIdentificationResult,
        nlu_result: Dict[str, Any]
    ) -> SituationJudgmentResult:
        """
        基于规则进行判断
        
        Args:
            symptom_result: 症状/困扰识别结果
            nlu_result: Step 1的NLU结果
        
        Returns:
            SituationJudgmentResult: 判断结果
        """
        intent = nlu_result.get("intent", "unknown")
        intent_confidence = nlu_result.get("intent_confidence", 0.5)
        status = symptom_result.status
        symptoms = symptom_result.symptoms
        concerns = symptom_result.concerns
        context_analysis = symptom_result.context_analysis
        
        # 情况A：明确无症状（纯筛查/体检规划）
        if (intent == "screening" and 
            status == "no_symptom" and 
            not symptoms and 
            not concerns and
            intent_confidence >= 0.8):
            return SituationJudgmentResult(
                status="no_symptom",
                confidence=0.9,
                reasoning="用户明确表达筛查意图且无症状/困扰，属于情况A",
                symptoms=[],
                concerns=[],
                context_analysis=context_analysis
            )
        
        # 情况B：存在症状/困扰
        if (status == "has_symptom" and 
            (symptoms or concerns) and
            symptom_result.confidence >= 0.8):
            return SituationJudgmentResult(
                status="has_symptom",
                confidence=0.9,
                reasoning=f"用户存在{'症状' if symptoms else ''}{'和困扰' if concerns else ''}，属于情况B",
                symptoms=symptoms,
                concerns=concerns,
                context_analysis=context_analysis
            )
        
        # 情况C：不确定/模糊/混合诉求
        if (intent == "mixed" or 
            intent == "unknown" or
            status == "uncertain" or
            (symptoms and symptom_result.confidence < 0.7)):
            return SituationJudgmentResult(
                status="uncertain",
                confidence=0.6,
                reasoning="用户诉求不明确或混合，需要进一步澄清，属于情况C",
                symptoms=symptoms,
                concerns=concerns,
                context_analysis=context_analysis
            )
        
        # 默认情况：根据症状存在性判断
        if symptoms or concerns:
            return SituationJudgmentResult(
                status="has_symptom",
                confidence=0.75,
                reasoning="基于症状/困扰存在性判断为情况B",
                symptoms=symptoms,
                concerns=concerns,
                context_analysis=context_analysis
            )
        else:
            return SituationJudgmentResult(
                status="uncertain",
                confidence=0.5,
                reasoning="信息不足，无法明确判断",
                symptoms=[],
                concerns=[],
                context_analysis=context_analysis
            )
    
    async def _judge_with_llm(
        self,
        symptom_result: SymptomIdentificationResult,
        nlu_result: Dict[str, Any]
    ) -> Optional[SituationJudgmentResult]:
        """
        使用LLM进行深度判断（处理复杂情况）
        
        Args:
            symptom_result: 症状/困扰识别结果
            nlu_result: Step 1的NLU结果
        
        Returns:
            SituationJudgmentResult或None（如果失败）
        """
        try:
            # 获取Prompt
            prompt = PromptManager.get_situation_judgment_prompt(symptom_result, nlu_result)
            
            # 调用LLM
            response = await self.llm_client.generate(prompt)
            
            # 解析响应
            data = self.response_parser.parse_json_response(response)
            if not data:
                return None
            
            # 验证结果
            if not self._validate_judgment_result(data):
                logger.warning(f"LLM返回的判断结果格式无效: {data}")
                return None
            
            # 使用LLM返回的状态，但保留原有的症状和困扰信息
            return SituationJudgmentResult(
                status=data.get("status", symptom_result.status),
                confidence=data.get("confidence", symptom_result.confidence),
                reasoning=data.get("reasoning", symptom_result.reasoning),
                symptoms=symptom_result.symptoms,
                concerns=symptom_result.concerns,
                context_analysis=symptom_result.context_analysis
            )
        except Exception as e:
            logger.error(f"LLM判断过程出错: {e}", exc_info=True)
            return None
    
    def _validate_judgment_result(self, data: Dict[str, Any]) -> bool:
        """
        验证判断结果格式
        
        Args:
            data: 解析后的数据
        
        Returns:
            是否有效
        """
        required_fields = ["status", "confidence", "reasoning"]
        if not all(field in data for field in required_fields):
            return False
        
        # 验证status值
        valid_statuses = ["no_symptom", "has_symptom", "uncertain"]
        if data.get("status") not in valid_statuses:
            return False
        
        # 验证confidence范围
        confidence = data.get("confidence", 0)
        if not isinstance(confidence, (int, float)) or not (0 <= confidence <= 1):
            return False
        
        return True

