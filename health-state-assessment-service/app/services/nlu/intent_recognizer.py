"""
意图识别模块：识别用户意图（screening/diagnosis/mixed/unknown）
"""
from typing import Optional, Dict, Any
from app.utils.llm_client import LangChainLLMClient, LLMConfig, LLMBackend
from app.config.settings import settings
from app.models.nlu import IntentResult
from app.services.nlu.prompt_manager import PromptManager
from app.services.nlu.response_parser import ResponseParser
from app.services.nlu.fallback_matcher import FallbackMatcher
from app.utils.logger import logger


class IntentRecognizer:
    """意图识别器"""
    
    def __init__(self, llm_client: Optional[LangChainLLMClient] = None):
        """
        初始化意图识别器
        
        Args:
            llm_client: LLM客户端，如果为None则自动创建
        """
        self.nlu_config = settings.nlu
        self.use_llm = self.nlu_config.use_llm
        self.enable_fallback = self.nlu_config.enable_fallback
        self.fallback_threshold = self.nlu_config.fallback_threshold
        
        # 初始化LLM客户端（如果需要）
        self.llm_client = llm_client
        if self.use_llm and self.llm_client is None:
            try:
                llm_config = LLMConfig(
                    backend=LLMBackend(self.nlu_config.llm_backend),
                    model=self.nlu_config.llm_model,
                    temperature=self.nlu_config.llm_temperature,
                    max_tokens=self.nlu_config.llm_max_tokens,
                    timeout=self.nlu_config.llm_timeout,
                    max_retries=self.nlu_config.llm_max_retries,
                    openai_api_key=self.nlu_config.openai_api_key,
                    openai_base_url=self.nlu_config.openai_base_url,
                    chatglm_api_url=self.nlu_config.chatglm_api_url,
                    chatglm_api_key=self.nlu_config.chatglm_api_key,
                    ollama_base_url=self.nlu_config.ollama_base_url,
                    ollama_model=self.nlu_config.ollama_model,
                    custom_api_url=self.nlu_config.custom_api_url,
                    custom_api_key=self.nlu_config.custom_api_key
                )
                self.llm_client = LangChainLLMClient(config=llm_config)
                logger.info("LLM客户端初始化成功")
            except Exception as e:
                logger.warning(f"LLM客户端初始化失败，将使用降级方案: {e}")
                self.use_llm = False
                self.llm_client = None
    
    async def recognize(
        self,
        user_input: str,
        context: Optional[Dict[str, Any]] = None
    ) -> IntentResult:
        """
        识别用户意图
        
        Args:
            user_input: 用户输入文本
            context: 上下文信息（可选）
        
        Returns:
            IntentResult: 意图识别结果
        """
        if not user_input or not user_input.strip():
            return IntentResult(
                intent="unknown",
                confidence=0.0,
                reasoning="用户输入为空",
                has_screening_intent=False,
                has_symptom_intent=False,
                details={}
            )
        
        # 尝试使用LLM识别意图
        if self.use_llm and self.llm_client:
            try:
                logger.info(f"使用LLM进行意图识别: user_input={user_input[:50]}...")
                result = await self._recognize_with_llm(user_input)
                if result and result.confidence >= self.fallback_threshold:
                    logger.info(
                        f"LLM意图识别成功: intent={result.intent}, "
                        f"confidence={result.confidence:.2f}, "
                        f"has_screening={result.has_screening_intent}, "
                        f"has_symptom={result.has_symptom_intent}, "
                        f"reasoning={result.reasoning[:100] if result.reasoning else 'N/A'}"
                    )
                    return result
                elif result:
                    logger.warning(
                        f"LLM识别置信度较低({result.confidence:.2f})，"
                        f"低于阈值({self.fallback_threshold})，使用降级方案"
                    )
            except Exception as e:
                logger.error(f"LLM意图识别失败: {e}", exc_info=True)
        
        # 使用降级方案（关键词匹配）
        if self.enable_fallback:
            logger.info("使用降级方案进行意图识别")
            result = self._recognize_with_fallback(user_input)
            logger.info(
                f"降级方案意图识别结果: intent={result.intent}, "
                f"confidence={result.confidence:.2f}, "
                f"has_screening={result.has_screening_intent}, "
                f"has_symptom={result.has_symptom_intent}"
            )
            return result
        
        # 如果降级方案也禁用，返回unknown
        return IntentResult(
            intent="unknown",
            confidence=0.0,
            reasoning="LLM不可用且降级方案已禁用",
            has_screening_intent=False,
            has_symptom_intent=False,
            details={}
        )
    
    async def _recognize_with_llm(self, user_input: str) -> Optional[IntentResult]:
        """
        使用LLM识别意图
        
        Args:
            user_input: 用户输入文本
            
        Returns:
            IntentResult或None（如果失败）
        """
        try:
            # 获取Prompt
            prompt = PromptManager.get_intent_prompt(user_input)
            
            # 调用LLM
            response = await self.llm_client.generate(prompt)
            
            # 解析响应
            parser = ResponseParser()
            data = parser.parse_json_response(response)
            
            if not data:
                return None
            
            # 验证结果
            if not parser.validate_intent_result(data):
                logger.warning(f"LLM返回的意图识别结果格式无效: {data}")
                return None
            
            # 构造结果
            return IntentResult(
                intent=data.get("intent", "unknown"),
                confidence=float(data.get("confidence", 0.0)),
                reasoning=data.get("reasoning", ""),
                has_screening_intent=data.get("has_screening_intent", False),
                has_symptom_intent=data.get("has_symptom_intent", False),
                details={
                    "screening_keywords": data.get("screening_keywords", []),
                    "symptom_keywords": data.get("symptom_keywords", []),
                    "llm_response": response
                }
            )
        except Exception as e:
            logger.error(f"LLM意图识别过程出错: {e}", exc_info=True)
            return None
    
    def _recognize_with_fallback(self, user_input: str) -> IntentResult:
        """
        使用降级方案识别意图（关键词匹配）
        
        Args:
            user_input: 用户输入文本
            
        Returns:
            IntentResult
        """
        result = FallbackMatcher.match_intent(user_input)
        return IntentResult(
            intent=result["intent"],
            confidence=result["confidence"],
            reasoning=result["reasoning"],
            has_screening_intent=result["has_screening_intent"],
            has_symptom_intent=result["has_symptom_intent"],
            details=result.get("details", {})
        )

