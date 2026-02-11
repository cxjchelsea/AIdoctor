"""
澄清问题生成器
基于上下文生成个性化的澄清问题
"""
from typing import Dict, Any, Optional, List
from app.models.entry_assessment import ClarificationQuestion, ClarificationOption
from app.utils.llm_client import LangChainLLMClient, LLMConfig, LLMBackend
from app.config.settings import settings
from app.services.nlu.response_parser import ResponseParser
from app.utils.logger import logger
import json


class ClarificationQuestionGenerator:
    """澄清问题生成器"""
    
    # 澄清问题生成Prompt模板
    CLARIFICATION_QUESTION_PROMPT = """你是一个医疗AI助手，需要生成一个澄清问题来帮助用户明确他们的主要目标。

用户输入：{user_input}

当前情况：
- 用户意图：{intent}
- 症状识别状态：{symptom_status}
- 识别到的症状：{symptoms}
- 识别到的困扰：{concerns}
- 识别置信度：{confidence}

需要澄清的原因：
{reasoning}

请生成一个个性化的澄清问题，帮助用户明确他们的主要目标是：
- **A（健康筛查/体检规划）**：用户想要进行体检、健康评估、预防性检查等，没有明确的症状需要咨询
- **B（症状咨询/问题排查）**：用户有明确的症状、不适或困扰，想要了解可能的原因或诊断

问题要求：
1. **清晰**：问题表述清楚，不含歧义
2. **简洁**：问题长度控制在20-50字
3. **易于理解**：使用通俗语言，避免专业术语
4. **个性化**：基于用户的具体情况生成（如果用户提到具体症状，在问题中提及）
5. **语言风格**：根据用户的表达方式调整（正式/口语化）

请以JSON格式返回结果：
{{
    "question": "澄清问题文本",
    "question_type": "single_choice",
    "options": [
        {{
            "id": "A",
            "text": "健康筛查/体检规划",
            "description": "进行体检、健康评估、预防性检查等"
        }},
        {{
            "id": "B",
            "text": "症状咨询/问题排查",
            "description": "咨询症状、不适或困扰，了解可能的原因"
        }}
    ],
    "reasoning": "生成理由"
}}"""

    # 降级模板
    FALLBACK_TEMPLATES = {
        "with_symptom": "您提到{ symptom_text }，您是想针对这个{ symptom_text }进行咨询，还是想进行常规体检？",
        "without_symptom": "您当前的主要目标是健康筛查/体检规划，还是症状咨询/问题排查？",
        "mixed": "您既有体检需求，又提到了一些不适，您的主要目标是健康筛查/体检规划，还是症状咨询/问题排查？"
    }
    
    def __init__(self, llm_client: Optional[LangChainLLMClient] = None):
        """
        初始化澄清问题生成器
        
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
                    temperature=0.8,  # 澄清问题生成需要一定的创造性
                    max_tokens=300,
                    timeout=2.0,  # 2秒超时
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
                logger.info("澄清问题生成器LLM客户端初始化成功")
            except Exception as e:
                logger.warning(f"澄清问题生成器LLM客户端初始化失败，将使用降级方案: {e}")
                self.use_llm = False
                self.llm_client = None
        
        self.response_parser = ResponseParser()
    
    async def generate(
        self,
        symptom_data: Dict[str, Any],
        nlu_result: Optional[Dict[str, Any]] = None
    ) -> ClarificationQuestion:
        """
        生成澄清问题
        
        Args:
            symptom_data: Step 2的输出，包含：
                - status: str - "uncertain"
                - symptoms: list - 症状列表
                - concerns: list - 困扰列表
                - confidence: float - 识别置信度
                - reasoning: str - 识别理由
            nlu_result: Step 1的NLU结果（可选）
        
        Returns:
            ClarificationQuestion: 澄清问题
        """
        # 尝试使用LLM生成澄清问题
        if self.use_llm and self.llm_client:
            try:
                logger.info("使用LLM生成澄清问题")
                result = await self._generate_with_llm(symptom_data, nlu_result)
                if result:
                    logger.info(f"LLM生成澄清问题成功: {result.question[:50]}...")
                    return result
            except Exception as e:
                logger.warning(f"LLM生成澄清问题失败，使用降级方案: {e}")
        
        # 降级到模板生成
        logger.info("使用模板生成澄清问题")
        return self._generate_with_template(symptom_data, nlu_result)
    
    async def _generate_with_llm(
        self,
        symptom_data: Dict[str, Any],
        nlu_result: Optional[Dict[str, Any]]
    ) -> Optional[ClarificationQuestion]:
        """
        使用LLM生成澄清问题
        
        Args:
            symptom_data: Step 2的输出
            nlu_result: Step 1的NLU结果
        
        Returns:
            ClarificationQuestion: 澄清问题，如果失败返回None
        """
        # 构建Prompt
        prompt = self._build_prompt(symptom_data, nlu_result)
        
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
            if not self._validate_question_data(data):
                logger.warning(f"LLM返回的澄清问题数据验证失败: {data}")
                return None
            
            # 构建澄清问题对象
            options = [
                ClarificationOption(
                    id=opt.get("id", ""),
                    text=opt.get("text", ""),
                    description=opt.get("description")
                )
                for opt in data.get("options", [])
            ]
            
            return ClarificationQuestion(
                question=data.get("question", ""),
                question_type=data.get("question_type", "single_choice"),
                options=options,
                reasoning=data.get("reasoning", "")
            )
        except Exception as e:
            logger.error(f"LLM生成澄清问题异常: {e}", exc_info=True)
            return None
    
    def _generate_with_template(
        self,
        symptom_data: Dict[str, Any],
        nlu_result: Optional[Dict[str, Any]]
    ) -> ClarificationQuestion:
        """
        使用模板生成澄清问题（降级方案）
        
        Args:
            symptom_data: Step 2的输出
            nlu_result: Step 1的NLU结果
        
        Returns:
            ClarificationQuestion: 澄清问题
        """
        symptoms = symptom_data.get("symptoms", [])
        original_input = symptom_data.get("original_input", "")
        
        # 选择模板
        if symptoms and len(symptoms) > 0:
            # 有症状，使用症状模板
            symptom_text = symptoms[0] if isinstance(symptoms[0], str) else symptoms[0].get("original_text", "不适")
            template = self.FALLBACK_TEMPLATES["with_symptom"]
            question = template.format(symptom_text=symptom_text)
        elif nlu_result and nlu_result.get("intent") == "mixed":
            # 混合诉求
            template = self.FALLBACK_TEMPLATES["mixed"]
            question = template
        else:
            # 无症状
            template = self.FALLBACK_TEMPLATES["without_symptom"]
            question = template
        
        # 构建选项
        options = [
            ClarificationOption(
                id="A",
                text="健康筛查/体检规划",
                description="进行体检、健康评估、预防性检查等"
            ),
            ClarificationOption(
                id="B",
                text="症状咨询/问题排查",
                description="咨询症状、不适或困扰，了解可能的原因"
            )
        ]
        
        return ClarificationQuestion(
            question=question,
            question_type="single_choice",
            options=options,
            reasoning="使用模板生成澄清问题（降级方案）"
        )
    
    def _build_prompt(
        self,
        symptom_data: Dict[str, Any],
        nlu_result: Optional[Dict[str, Any]]
    ) -> str:
        """
        构建Prompt
        
        Args:
            symptom_data: Step 2的输出
            nlu_result: Step 1的NLU结果
        
        Returns:
            str: 填充后的Prompt
        """
        # 优先从nlu_result获取original_input，如果没有则从symptom_data获取
        original_input = ""
        if nlu_result:
            original_input = nlu_result.get("original_input", "")
        if not original_input:
            original_input = symptom_data.get("original_input", "")
        
        intent = nlu_result.get("intent", "unknown") if nlu_result else "unknown"
        symptom_status = symptom_data.get("status", "uncertain")
        symptoms = symptom_data.get("symptoms", [])
        concerns = symptom_data.get("concerns", [])
        confidence = symptom_data.get("confidence", 0.5)
        reasoning = symptom_data.get("reasoning", "用户意图不明确，需要澄清")
        
        # 格式化症状和困扰
        symptoms_text = ", ".join(
            s if isinstance(s, str) else s.get("original_text", "")
            for s in symptoms[:3]  # 最多显示3个症状
        ) if symptoms else "无"
        
        concerns_text = ", ".join(
            c if isinstance(c, str) else c.get("description", "")
            for c in concerns[:3]  # 最多显示3个困扰
        ) if concerns else "无"
        
        return self.CLARIFICATION_QUESTION_PROMPT.format(
            user_input=original_input,
            intent=intent,
            symptom_status=symptom_status,
            symptoms=symptoms_text,
            concerns=concerns_text,
            confidence=confidence,
            reasoning=reasoning
        )
    
    def _validate_question_data(self, data: Dict[str, Any]) -> bool:
        """
        验证澄清问题数据
        
        Args:
            data: 解析后的数据
        
        Returns:
            bool: 是否有效
        """
        # 验证必填字段
        required_fields = ["question", "question_type", "options", "reasoning"]
        if not all(field in data for field in required_fields):
            return False
        
        # 验证问题文本
        question = data.get("question", "")
        if not question or len(question) < 10 or len(question) > 200:
            return False
        
        # 验证问题类型
        valid_types = ["single_choice", "multiple_choice", "open_ended"]
        if data.get("question_type") not in valid_types:
            return False
        
        # 验证选项
        options = data.get("options", [])
        if not isinstance(options, list) or len(options) < 2:
            return False
        
        for opt in options:
            if not isinstance(opt, dict):
                return False
            if "id" not in opt or "text" not in opt:
                return False
        
        return True

