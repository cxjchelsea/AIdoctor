"""
澄清结果处理器
处理用户对澄清问题的回答，确定方向（A或B）
"""
from typing import Dict, Any, Optional
from app.models.entry_assessment import ClarificationResult
from app.utils.llm_client import LangChainLLMClient, LLMConfig, LLMBackend
from app.config.settings import settings
from app.services.nlu.response_parser import ResponseParser
from app.utils.logger import logger


class ClarificationResultProcessor:
    """澄清结果处理器"""
    
    # 澄清结果处理Prompt模板
    CLARIFICATION_RESULT_PROMPT = """你是一个医疗AI助手，需要分析用户对澄清问题的回答，确定用户的主要目标方向。

澄清问题：{question}
问题选项：
{options}

用户回答：{user_answer}

原始用户输入：{original_input}

上下文信息：
- 用户意图：{intent}
- 症状识别状态：{symptom_status}
- 识别到的症状：{symptoms}

请分析用户回答，确定用户的主要目标是：
- **A（健康筛查/体检规划）**：用户想要进行体检、健康评估、预防性检查等，没有明确的症状需要咨询
- **B（症状咨询/问题排查）**：用户有明确的症状、不适或困扰，想要了解可能的原因或诊断

分析要求：
1. **直接选择**：如果用户直接选择了选项A或B，直接返回对应方向
2. **间接表达**：如果用户通过描述表达意图，需要理解语义
3. **关键词匹配**：
   - 方向A关键词：体检、筛查、检查、预防、常规、定期等
   - 方向B关键词：症状、不舒服、疼痛、咨询、问题、排查等
4. **上下文一致性**：结合原始输入和症状识别结果，确保判断一致
5. **置信度评估**：
   - 高置信度（0.8-1.0）：回答明确，上下文一致
   - 中置信度（0.6-0.8）：回答偏向某个方向，但不够明确
   - 低置信度（0.4-0.6）：回答模糊，上下文冲突

请以JSON格式返回结果：
{{
    "direction": "A|B",
    "confidence": 0.0-1.0,
    "reasoning": "判断理由（详细说明判断依据和置信度来源）"
}}"""
    
    # 方向A关键词
    DIRECTION_A_KEYWORDS = ["体检", "筛查", "检查", "预防", "常规", "定期", "健康管理", "健康评估"]
    
    # 方向B关键词
    DIRECTION_B_KEYWORDS = ["症状", "不舒服", "疼痛", "咨询", "问题", "排查", "诊断", "不适", "困扰"]
    
    def __init__(self, llm_client: Optional[LangChainLLMClient] = None):
        """
        初始化澄清结果处理器
        
        Args:
            llm_client: LLM客户端，如果为None则自动创建
        """
        self.nlu_config = settings.nlu
        self.use_llm = self.nlu_config.use_llm
        self.enable_fallback = self.nlu_config.enable_fallback
        
        # 初始化LLM客户端（如果需要）
        self.llm_client = llm_client
        if self.use_llm and self.llm_client is None:
            try:
                llm_config = LLMConfig(
                    backend=LLMBackend(self.nlu_config.llm_backend),
                    model=self.nlu_config.llm_model,
                    temperature=0.3,  # 结果处理需要更准确
                    max_tokens=200,
                    timeout=2.0,
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
                logger.info("澄清结果处理器LLM客户端初始化成功")
            except Exception as e:
                logger.warning(f"澄清结果处理器LLM客户端初始化失败，将使用降级方案: {e}")
                self.use_llm = False
                self.llm_client = None
        
        self.response_parser = ResponseParser()
    
    async def process(
        self,
        user_answer: str,
        question: str,
        options: list,
        symptom_data: Optional[Dict[str, Any]] = None,
        nlu_result: Optional[Dict[str, Any]] = None
    ) -> ClarificationResult:
        """
        处理用户回答
        
        Args:
            user_answer: 用户回答文本
            question: 澄清问题文本
            options: 问题选项列表
            symptom_data: Step 2的输出（可选）
            nlu_result: Step 1的NLU结果（可选）
        
        Returns:
            ClarificationResult: 澄清结果
        """
        # 预处理用户回答
        processed_answer = self._preprocess_answer(user_answer)
        
        # 尝试使用LLM处理
        if self.use_llm and self.llm_client:
            try:
                logger.info(f"使用LLM处理澄清结果: user_answer={processed_answer[:50]}...")
                result = await self._process_with_llm(
                    processed_answer, question, options, symptom_data, nlu_result
                )
                if result:
                    logger.info(f"LLM处理澄清结果成功: direction={result.direction}, confidence={result.confidence:.2f}")
                    return result
            except Exception as e:
                logger.warning(f"LLM处理澄清结果失败，使用降级方案: {e}")
        
        # 降级到关键词匹配
        logger.info("使用关键词匹配处理澄清结果")
        return self._process_with_keywords(
            processed_answer, question, options, symptom_data, nlu_result
        )
    
    async def _process_with_llm(
        self,
        user_answer: str,
        question: str,
        options: list,
        symptom_data: Optional[Dict[str, Any]],
        nlu_result: Optional[Dict[str, Any]]
    ) -> Optional[ClarificationResult]:
        """
        使用LLM处理澄清结果
        
        Args:
            user_answer: 用户回答
            question: 澄清问题
            options: 问题选项
            symptom_data: Step 2的输出
            nlu_result: Step 1的NLU结果
        
        Returns:
            ClarificationResult: 澄清结果，如果失败返回None
        """
        # 构建Prompt
        prompt = self._build_prompt(user_answer, question, options, symptom_data, nlu_result)
        
        # 调用LLM
        try:
            response = await self.llm_client.ainvoke(prompt)
            if not response:
                return None
            
            # 解析响应
            data = self.response_parser.parse_json_response(response)
            if not data:
                return None
            
            # 验证数据
            if not self._validate_result_data(data):
                logger.warning(f"LLM返回的澄清结果数据验证失败: {data}")
                return None
            
            # 构建澄清结果对象
            return ClarificationResult(
                direction=data.get("direction", "B"),
                confidence=float(data.get("confidence", 0.5)),
                reasoning=data.get("reasoning", ""),
                clarified=True,
                user_answer=user_answer
            )
        except Exception as e:
            logger.error(f"LLM处理澄清结果异常: {e}", exc_info=True)
            return None
    
    def _process_with_keywords(
        self,
        user_answer: str,
        question: str,
        options: list,
        symptom_data: Optional[Dict[str, Any]],
        nlu_result: Optional[Dict[str, Any]]
    ) -> ClarificationResult:
        """
        使用关键词匹配处理澄清结果（降级方案）
        
        Args:
            user_answer: 用户回答
            question: 澄清问题
            options: 问题选项
            symptom_data: Step 2的输出
            nlu_result: Step 1的NLU结果
        
        Returns:
            ClarificationResult: 澄清结果
        """
        # 检查直接选择
        answer_lower = user_answer.lower().strip()
        
        # 检查是否直接选择了选项
        for opt in options:
            opt_id = opt.get("id", "") if isinstance(opt, dict) else ""
            opt_text = opt.get("text", "") if isinstance(opt, dict) else str(opt)
            
            if opt_id.upper() in answer_lower or opt_id.lower() in answer_lower:
                direction = opt_id.upper()
                confidence = 0.9
                reasoning = f"用户直接选择了选项{opt_id}"
                return ClarificationResult(
                    direction=direction,
                    confidence=confidence,
                    reasoning=reasoning,
                    clarified=True,
                    user_answer=user_answer
                )
            
            # 检查选项文本是否在回答中
            if opt_text in user_answer:
                direction = opt_id.upper()
                confidence = 0.85
                reasoning = f"用户回答中包含选项文本：{opt_text}"
                return ClarificationResult(
                    direction=direction,
                    confidence=confidence,
                    reasoning=reasoning,
                    clarified=True,
                    user_answer=user_answer
                )
        
        # 关键词匹配
        direction_a_score = sum(1 for keyword in self.DIRECTION_A_KEYWORDS if keyword in user_answer)
        direction_b_score = sum(1 for keyword in self.DIRECTION_B_KEYWORDS if keyword in user_answer)
        
        if direction_a_score > direction_b_score:
            direction = "A"
            confidence = min(0.7 + direction_a_score * 0.1, 0.9)
            reasoning = f"用户回答中包含方向A的关键词（匹配{direction_a_score}个）"
        elif direction_b_score > direction_a_score:
            direction = "B"
            confidence = min(0.7 + direction_b_score * 0.1, 0.9)
            reasoning = f"用户回答中包含方向B的关键词（匹配{direction_b_score}个）"
        else:
            # 无法判断，根据上下文推断
            if symptom_data and symptom_data.get("symptoms"):
                direction = "B"
                confidence = 0.5
                reasoning = "用户回答模糊，根据症状识别结果推断为方向B"
            else:
                direction = "A"
                confidence = 0.5
                reasoning = "用户回答模糊，根据上下文推断为方向A"
        
        return ClarificationResult(
            direction=direction,
            confidence=confidence,
            reasoning=reasoning,
            clarified=True,
            user_answer=user_answer
        )
    
    def _preprocess_answer(self, user_answer: str) -> str:
        """
        预处理用户回答
        
        Args:
            user_answer: 用户原始回答
        
        Returns:
            str: 预处理后的回答
        """
        if not user_answer:
            return ""
        
        # 去除多余空格
        answer = " ".join(user_answer.split())
        
        # 去除标点符号（保留中文标点）
        import re
        answer = re.sub(r'[，。！？、；：]', '', answer)
        
        return answer.strip()
    
    def _build_prompt(
        self,
        user_answer: str,
        question: str,
        options: list,
        symptom_data: Optional[Dict[str, Any]],
        nlu_result: Optional[Dict[str, Any]]
    ) -> str:
        """
        构建Prompt
        
        Args:
            user_answer: 用户回答
            question: 澄清问题
            options: 问题选项
            symptom_data: Step 2的输出
            nlu_result: Step 1的NLU结果
        
        Returns:
            str: 填充后的Prompt
        """
        # 格式化选项
        options_text = "\n".join(
            f"- {opt.get('id', '')}: {opt.get('text', '')} - {opt.get('description', '')}"
            if isinstance(opt, dict) else f"- {opt}"
            for opt in options
        )
        
        # 获取上下文信息
        original_input = ""
        intent = "unknown"
        symptom_status = "uncertain"
        symptoms = "无"
        
        if nlu_result:
            original_input = nlu_result.get("original_input", "")
            intent = nlu_result.get("intent", "unknown")
        
        if symptom_data:
            symptom_status = symptom_data.get("status", "uncertain")
            symptoms_list = symptom_data.get("symptoms", [])
            symptoms = ", ".join(
                s if isinstance(s, str) else s.get("original_text", "")
                for s in symptoms_list[:3]
            ) if symptoms_list else "无"
        
        return self.CLARIFICATION_RESULT_PROMPT.format(
            question=question,
            options=options_text,
            user_answer=user_answer,
            original_input=original_input,
            intent=intent,
            symptom_status=symptom_status,
            symptoms=symptoms
        )
    
    def _validate_result_data(self, data: Dict[str, Any]) -> bool:
        """
        验证澄清结果数据
        
        Args:
            data: 解析后的数据
        
        Returns:
            bool: 是否有效
        """
        # 验证必填字段
        required_fields = ["direction", "confidence", "reasoning"]
        if not all(field in data for field in required_fields):
            return False
        
        # 验证方向
        direction = data.get("direction", "").upper()
        if direction not in ["A", "B"]:
            return False
        
        # 验证置信度
        confidence = data.get("confidence", 0)
        if not isinstance(confidence, (int, float)) or not (0 <= confidence <= 1):
            return False
        
        return True

