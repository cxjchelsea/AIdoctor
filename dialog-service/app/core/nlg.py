"""
自然语言生成（NLG）
按照《dialog-service - 服务实现方案.md》实现
使用LLM生成自然、易懂的追问问题
"""
from typing import Dict, Any, Optional
import logging
from app.utils.llm_client import LangChainLLMClient
from app.utils.prompt_manager import PromptTemplateManager

logger = logging.getLogger(__name__)


class NaturalLanguageGenerator:
    """自然语言生成"""
    
    def __init__(self):
        self.llm_client = LangChainLLMClient()
        self.template_manager = PromptTemplateManager()
        
        # 降级策略：模板问题
        self.fallback_templates = {
            "duration_question": "您这个症状出现多久了？",
            "severity_question": "疼痛程度0-10分，您打几分？",
            "accompanying_question": "除了这个症状，还有没有其他不舒服？",
            "location_question": "症状出现在哪个部位？",
            "frequency_question": "症状是持续的还是阵发性的？",
            "trigger_question": "症状是在什么情况下出现的？是活动后还是休息时？",
        }
    
    async def generate_question(
        self,
        question_info: Dict[str, Any],
        context: Dict[str, Any]
    ) -> str:
        """
        生成自然语言问题
        
        Args:
            question_info: 问题信息（包含类型、缺失信息等）
            context: 对话上下文
            
        Returns:
            自然语言问题
        """
        logger.info(f"生成问题: type={question_info.get('type')}")
        
        try:
            # 使用LLM生成问题
            question = await self._generate_question_with_llm(question_info, context)
            return question
        except Exception as e:
            logger.warning(f"LLM生成问题失败，使用降级策略: {str(e)}")
            # 降级策略：使用模板
            return self._generate_question_with_template(question_info)
    
    async def _generate_question_with_llm(
        self,
        question_info: Dict[str, Any],
        context: Dict[str, Any]
    ) -> str:
        """使用LLM生成问题"""
        # 获取缺失信息
        missing_info = question_info.get("missing_info", [])
        if isinstance(missing_info, list) and missing_info:
            missing_info_text = ", ".join([
                item.get("description", item.get("field", ""))
                if isinstance(item, dict) else str(item)
                for item in missing_info
            ])
        else:
            missing_info_text = str(missing_info)
        
        # 构建对话历史
        conversation_history = context.get("conversationHistory", [])
        history_text = "\n".join([
            f"{msg.get('role', 'user')}: {msg.get('content', '')}"
            for msg in conversation_history[-3:]  # 只取最近3条
        ])
        
        # 使用模板管理器格式化提示词
        prompt = self.template_manager.format_question_generation(
            missing_info=missing_info_text,
            context={
                "conversation_history": history_text,
                "question_type": question_info.get("type", ""),
                "patient_info": context.get("patient_info", {})
            }
        )
        
        # 调用LLM生成问题
        question = await self.llm_client.generate(prompt)
        
        # 后处理：清理和验证
        question = self._post_process_question(question)
        
        return question
    
    def _post_process_question(self, question: str) -> str:
        """后处理问题：清理和验证"""
        # 移除多余的空白字符
        question = question.strip()
        
        # 移除引号（如果LLM返回带引号的）
        if question.startswith('"') and question.endswith('"'):
            question = question[1:-1]
        if question.startswith("'") and question.endswith("'"):
            question = question[1:-1]
        
        # 确保以问号结尾
        if question and not question.endswith(("？", "?")):
            question += "？"
        
        # 限制长度（不超过50字）
        if len(question) > 50:
            question = question[:47] + "..."
        
        return question
    
    def _generate_question_with_template(self, question_info: Dict[str, Any]) -> str:
        """使用模板生成问题（降级策略）"""
        question_type = question_info.get("type", "")
        template_key = f"{question_type}_question"
        
        if template_key in self.fallback_templates:
            return self.fallback_templates[template_key]
        
        # 默认问题
        return "请详细描述一下您的症状。"

