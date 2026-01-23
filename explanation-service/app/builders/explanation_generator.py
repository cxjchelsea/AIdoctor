"""
自然语言解释生成器
使用LLM生成自然、易懂的解释说明
"""
import logging
from typing import Dict, Any, Optional
try:
    from aidoctor_llm.llm_client import LangChainLLMClient
    from aidoctor_llm.prompt_manager import PromptTemplateManager
    LLM_AVAILABLE = True
except ImportError:
    LLM_AVAILABLE = False
    LangChainLLMClient = None
    PromptTemplateManager = None

from app.config.settings import settings

logger = logging.getLogger(__name__)


class ExplanationGenerator:
    """自然语言解释生成器"""
    
    def __init__(self):
        """初始化解释生成器"""
        if not LLM_AVAILABLE:
            logger.warning("LLM库不可用，将使用降级策略")
            self.llm_client = None
            self.prompt_manager = None
            return
        
        try:
            self.llm_client = LangChainLLMClient()
            self.prompt_manager = PromptTemplateManager()
            logger.info("自然语言解释生成器初始化成功")
        except Exception as e:
            logger.warning(f"LLM客户端初始化失败，将使用降级策略: {str(e)}")
            self.llm_client = None
            self.prompt_manager = None
    
    async def generate(
        self,
        conclusion_package: Dict[str, Any],
        evidence_chain: Any,
        reasoning_paths: list
    ) -> Optional[str]:
        """
        生成自然语言解释
        
        Args:
            conclusion_package: 终点结论包
            evidence_chain: 证据链
            reasoning_paths: 推理路径
            
        Returns:
            自然语言解释，如果生成失败则返回None
        """
        if not self.llm_client or not self.prompt_manager:
            logger.warning("LLM客户端不可用，跳过自然语言解释生成")
            return None
        
        try:
            # 1. 格式化Prompt
            prompt = self._format_prompt(conclusion_package, evidence_chain, reasoning_paths)
            
            # 2. 调用LLM生成解释
            explanation = await self.llm_client.generate(prompt)
            
            # 3. 后处理
            formatted_explanation = self._post_process(explanation)
            
            return formatted_explanation
        except Exception as e:
            logger.error(f"自然语言解释生成失败: {str(e)}", exc_info=True)
            # 降级策略：返回None，不抛出异常
            return None
    
    def _format_prompt(
        self,
        conclusion_package: Dict[str, Any],
        evidence_chain: Any,
        reasoning_paths: list
    ) -> str:
        """
        格式化Prompt
        
        Args:
            conclusion_package: 终点结论包
            evidence_chain: 证据链
            reasoning_paths: 推理路径
            
        Returns:
            格式化后的Prompt
        """
        # 提取诊断信息
        conclusion = conclusion_package.get("conclusion", {})
        diagnosis = conclusion.get("diagnosis", "未知")
        confidence = conclusion.get("confidence", 0.0)
        severity = conclusion.get("severity", "moderate")
        
        # 提取关键证据
        key_evidence = conclusion_package.get("keyEvidence", [])
        evidence_descriptions = [
            ev.get("description", "") for ev in key_evidence[:3]
            if isinstance(ev, dict) and ev.get("description")
        ]
        
        # 提取行动建议
        action_and_followup = conclusion_package.get("actionAndFollowUp", {})
        immediate_action = action_and_followup.get("immediateAction", {})
        medical_advice = immediate_action.get("medicalAdvice", "建议就医")
        examinations = immediate_action.get("examinations", [])
        
        # 格式化推理路径
        reasoning_path_text = ""
        if reasoning_paths:
            path_descriptions = [
                path.get("description", str(path)) if isinstance(path, dict) else str(path)
                for path in reasoning_paths[:3]
            ]
            reasoning_path_text = "\n".join(f"- {desc}" for desc in path_descriptions)
        
        # 使用模板管理器格式化Prompt
        try:
            prompt = self.prompt_manager.format_explanation_generation(
                diagnosis={
                    "name": diagnosis,
                    "confidence": confidence,
                    "severity": severity
                },
                evidence=evidence_descriptions,
                reasoning_path=reasoning_path_text
            )
        except Exception:
            # 如果模板格式化失败，使用默认Prompt
            prompt = f"""请为以下诊断结果生成自然、易懂的解释：

诊断结果：{diagnosis}
置信度：{confidence:.2f}
严重程度：{severity}

关键证据：
{chr(10).join(f"- {ev}" for ev in evidence_descriptions)}

推理路径：
{reasoning_path_text if reasoning_path_text else "基于症状和检查结果进行推理"}

行动建议：
- {medical_advice}
- 建议检查：{', '.join(examinations) if examinations else '根据医生建议'}

要求：
1. 使用通俗易懂的语言
2. 解释诊断依据
3. 说明下一步建议
4. 不超过300字

请生成解释："""
        
        return prompt
    
    def _post_process(self, explanation: str) -> str:
        """
        后处理解释文本
        
        Args:
            explanation: 原始解释文本
            
        Returns:
            处理后的解释文本
        """
        if not explanation:
            return ""
        
        # 去除多余空白
        explanation = explanation.strip()
        
        # 限制长度（最多500字）
        if len(explanation) > 500:
            explanation = explanation[:500] + "..."
        
        return explanation

