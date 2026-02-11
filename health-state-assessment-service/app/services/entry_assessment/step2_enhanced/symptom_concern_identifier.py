"""
症状/困扰识别器
基于NLU结果识别症状和困扰
"""
from typing import Dict, Any, List, Optional
from app.models.entry_assessment import (
    SymptomIdentificationResult,
    SymptomInfo,
    ConcernInfo
)
from app.models.nlu import SymptomEntity
from app.services.nlu.prompt_manager import PromptManager
from app.services.nlu.response_parser import ResponseParser
from app.utils.llm_client import LangChainLLMClient
from app.utils.logger import logger
from .context_analyzer import ContextAnalyzer


class SymptomConcernIdentifier:
    """症状/困扰识别器"""
    
    def __init__(
        self,
        llm_client: Optional[LangChainLLMClient] = None,
        context_analyzer: Optional[ContextAnalyzer] = None
    ):
        """
        初始化识别器
        
        Args:
            llm_client: LLM客户端，如果为None则自动创建
            context_analyzer: 上下文分析器，如果为None则自动创建
        """
        self.llm_client = llm_client or LangChainLLMClient()
        self.context_analyzer = context_analyzer or ContextAnalyzer()
        self.response_parser = ResponseParser()
    
    async def identify(
        self,
        nlu_result: Dict[str, Any],
        context: Optional[Dict] = None
    ) -> SymptomIdentificationResult:
        """
        识别症状/困扰
        
        Args:
            nlu_result: Step 1的NLU结果
            context: 额外的上下文信息（可选）
        
        Returns:
            SymptomIdentificationResult: 识别结果
        """
        original_input = nlu_result.get("original_input", "")
        intent = nlu_result.get("intent", "unknown")
        entities = nlu_result.get("entities", [])
        symptoms_from_nlu = nlu_result.get("symptoms", [])
        temporal_info = nlu_result.get("temporal_info", {})
        
        # 首先基于NLU结果进行基础识别
        base_result = self._identify_from_nlu(nlu_result)
        
        # 如果情况复杂（混合诉求、模糊表达），使用LLM进行深度分析
        if intent == "mixed" or intent == "unknown" or base_result.confidence < 0.7:
            try:
                llm_result = await self._identify_with_llm(nlu_result)
                if llm_result and llm_result.confidence > base_result.confidence:
                    return llm_result
            except Exception as e:
                logger.warning(f"LLM识别失败，使用基础识别结果: {e}")
        
        return base_result
    
    def _identify_from_nlu(self, nlu_result: Dict[str, Any]) -> SymptomIdentificationResult:
        """
        基于NLU结果进行基础识别
        
        Args:
            nlu_result: Step 1的NLU结果
        
        Returns:
            SymptomIdentificationResult: 识别结果
        """
        intent = nlu_result.get("intent", "unknown")
        has_symptoms = nlu_result.get("has_symptoms", False)
        entities = nlu_result.get("entities", [])
        symptoms_from_nlu = nlu_result.get("symptoms", [])
        temporal_info = nlu_result.get("temporal_info", {})
        original_input = nlu_result.get("original_input", "")
        
        # 提取症状信息
        symptoms = []
        for entity in entities:
            if entity.get("type") == "symptom":
                symptom_info = SymptomInfo(
                    original_text=entity.get("value", ""),
                    standard_term=entity.get("standard_term", entity.get("value", "")),
                    cui=entity.get("cui"),
                    temporal_info=temporal_info,
                    severity=entity.get("severity"),
                    location=entity.get("location"),
                    context=entity.get("context", {})
                )
                symptoms.append(symptom_info)
        
        # 如果没有从实体中提取到症状，但从symptoms列表中获取
        if not symptoms and symptoms_from_nlu:
            for symptom_text in symptoms_from_nlu:
                symptom_info = SymptomInfo(
                    original_text=symptom_text,
                    standard_term=symptom_text,
                    temporal_info=temporal_info,
                    context={}
                )
                symptoms.append(symptom_info)
        
        # 识别困扰
        concerns = self._identify_concerns(original_input, nlu_result)
        
        # 分析上下文
        from app.models.nlu import SymptomEntity
        symptom_entities = []
        for symptom in symptoms:
            symptom_entities.append(SymptomEntity(
                original_text=symptom.original_text,
                standard_term=symptom.standard_term,
                cui=symptom.cui,
                confidence=0.8,
                context=symptom.context,
                location=symptom.location,
                severity=symptom.severity,
                duration=symptom.temporal_info.get("duration"),
                frequency=symptom.temporal_info.get("frequency")
            ))
        
        context_analysis = self.context_analyzer.analyze(nlu_result, symptom_entities)
        
        # 判断状态
        if intent == "screening" and not has_symptoms and not symptoms:
            status = "no_symptom"
            confidence = 0.9
            reasoning = "用户明确表达筛查意图且无症状描述"
        elif has_symptoms or symptoms or concerns:
            status = "has_symptom"
            confidence = 0.85 if symptoms or concerns else 0.7
            reasoning = f"识别到{'症状' if symptoms else ''}{'和困扰' if concerns else ''}"
        else:
            status = "uncertain"
            confidence = 0.5
            reasoning = "无法明确判断用户状态"
        
        return SymptomIdentificationResult(
            status=status,
            symptoms=symptoms,
            concerns=concerns,
            confidence=confidence,
            reasoning=reasoning,
            context_analysis=context_analysis
        )
    
    async def _identify_with_llm(
        self,
        nlu_result: Dict[str, Any]
    ) -> Optional[SymptomIdentificationResult]:
        """
        使用LLM进行深度识别（处理复杂情况）
        
        Args:
            nlu_result: Step 1的NLU结果
        
        Returns:
            SymptomIdentificationResult或None（如果失败）
        """
        try:
            # 获取Prompt
            prompt = PromptManager.get_symptom_concern_identification_prompt(nlu_result)
            
            # 调用LLM
            response = await self.llm_client.generate(prompt)
            
            # 解析响应
            data = self.response_parser.parse_json_response(response)
            if not data:
                return None
            
            # 验证结果
            if not self._validate_identification_result(data):
                logger.warning(f"LLM返回的识别结果格式无效: {data}")
                return None
            
            # 构造症状信息
            symptoms = []
            for symptom_data in data.get("symptoms", []):
                symptoms.append(SymptomInfo(
                    original_text=symptom_data.get("original_text", ""),
                    standard_term=symptom_data.get("standard_term", symptom_data.get("original_text", "")),
                    cui=symptom_data.get("cui"),
                    temporal_info=symptom_data.get("temporal_info", {}),
                    severity=symptom_data.get("severity"),
                    location=symptom_data.get("location"),
                    context=symptom_data.get("context", {})
                ))
            
            # 构造困扰信息
            concerns = []
            for concern_data in data.get("concerns", []):
                concerns.append(ConcernInfo(
                    type=concern_data.get("type", "其他"),
                    description=concern_data.get("description", ""),
                    severity=concern_data.get("severity", "轻度"),
                    confidence=concern_data.get("confidence", 0.8)
                ))
            
            # 分析上下文
            from app.models.nlu import SymptomEntity
            symptom_entities = []
            for symptom in symptoms:
                symptom_entities.append(SymptomEntity(
                    original_text=symptom.original_text,
                    standard_term=symptom.standard_term,
                    cui=symptom.cui,
                    confidence=0.8,
                    context=symptom.context,
                    location=symptom.location,
                    severity=symptom.severity,
                    duration=symptom.temporal_info.get("duration"),
                    frequency=symptom.temporal_info.get("frequency")
                ))
            
            context_analysis = self.context_analyzer.analyze(nlu_result, symptom_entities)
            
            return SymptomIdentificationResult(
                status=data.get("status", "uncertain"),
                symptoms=symptoms,
                concerns=concerns,
                confidence=data.get("confidence", 0.7),
                reasoning=data.get("reasoning", ""),
                context_analysis=context_analysis
            )
        except Exception as e:
            logger.error(f"LLM识别过程出错: {e}", exc_info=True)
            return None
    
    def _identify_concerns(
        self,
        original_input: str,
        nlu_result: Dict[str, Any]
    ) -> List[ConcernInfo]:
        """
        识别困扰（心理、功能变化、异常感觉等）
        
        Args:
            original_input: 原始输入
            nlu_result: NLU结果
        
        Returns:
            List[ConcernInfo]: 困扰列表
        """
        concerns = []
        
        # 心理困扰关键词
        psychological_keywords = {
            "担心": "心理",
            "害怕": "心理",
            "焦虑": "心理",
            "困扰": "心理",
            "疑问": "心理",
            "不确定": "心理",
            "恐惧": "心理"
        }
        
        # 功能变化关键词
        functional_keywords = {
            "走几步就喘": "功能变化",
            "爬楼梯": "功能变化",
            "活动受限": "功能变化",
            "不能": "功能变化",
            "无法": "功能变化"
        }
        
        # 检查心理困扰
        for keyword, concern_type in psychological_keywords.items():
            if keyword in original_input:
                concerns.append(ConcernInfo(
                    type=concern_type,
                    description=f"存在{keyword}情绪",
                    severity="轻度",
                    confidence=0.7
                ))
        
        # 检查功能变化
        for keyword, concern_type in functional_keywords.items():
            if keyword in original_input:
                concerns.append(ConcernInfo(
                    type=concern_type,
                    description=f"存在{keyword}",
                    severity="中度",
                    confidence=0.8
                ))
        
        return concerns
    
    def _validate_identification_result(self, data: Dict[str, Any]) -> bool:
        """
        验证识别结果格式
        
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

