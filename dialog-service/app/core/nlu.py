"""
自然语言理解（NLU）
按照《dialog-service - 服务实现方案.md》实现
使用LLM进行语义理解
"""
from typing import Dict, Any, Optional
import logging
import json
import re
from app.utils.llm_client import LangChainLLMClient
from app.utils.prompt_manager import PromptTemplateManager
from aidoctor_llm.exceptions import LLMConfigException

logger = logging.getLogger(__name__)


class NaturalLanguageUnderstanding:
    """自然语言理解"""
    
    def __init__(self):
        self.llm_client = LangChainLLMClient()
        self.template_manager = PromptTemplateManager()
    
    async def understand(self, text: str, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        理解用户输入，提取结构化信息
        
        Args:
            text: 用户输入文本
            context: 对话上下文
            
        Returns:
            理解结果，包含提取的信息和置信度
        """
        logger.info(f"理解用户输入: text={text[:50]}...")
        
        try:
            # 使用LLM理解用户输入
            prompt = self._build_understanding_prompt(text, context)
            result = await self.llm_client.generate(prompt)
            
            # 解析LLM返回的JSON
            extracted_info = self._parse_llm_response(result)
            
            # 验证信息完整性
            validated_info = self._validate_information(extracted_info)
            
            return {
                **validated_info,
                "confidence": extracted_info.get("confidence", 0.8)
            }
        except Exception as e:
            logger.error(f"NLU处理失败: {str(e)}", exc_info=True)
            # 降级策略：使用规则匹配
            return self._fallback_rule_based_extraction(text)
    
    def _build_understanding_prompt(self, text: str, context: Dict[str, Any]) -> str:
        """构建理解提示词"""
        conversation_history = context.get("conversationHistory", [])
        history_text = "\n".join([
            f"{msg.get('role', 'user')}: {msg.get('content', '')}"
            for msg in conversation_history[-5:]  # 只取最近5条
        ])
        
        prompt = f"""你是一位经验丰富的临床医生。请理解以下患者输入，提取关键信息。

患者输入：{text}

对话历史：
{history_text if history_text else "无"}

请提取以下信息（如果存在）：
1. symptom_duration: 症状持续时间（如：3天、2周、1个月）
2. symptom_severity: 症状严重程度（0-10分，数字）
3. symptom_location: 症状部位（如：胸口、腹部、头部）
4. symptom_trigger: 症状诱因（如：活动后、休息时、进食后）
5. symptom_frequency: 症状频率（如：持续、阵发性、间歇性）
6. accompanying_symptoms: 伴随症状（列表）
7. symptom_relief: 缓解方式（如：休息后缓解、用药后缓解）

请以JSON格式返回，格式如下：
{{
    "symptom_duration": "3天",
    "symptom_severity": 7,
    "symptom_location": "胸口",
    "symptom_trigger": "活动后",
    "symptom_frequency": "阵发性",
    "accompanying_symptoms": ["出汗", "气短"],
    "symptom_relief": "休息后缓解",
    "confidence": 0.9
}}

如果某项信息不存在，请设置为null。只返回JSON，不要其他内容。"""
        
        return prompt
    
    def _parse_llm_response(self, response: str) -> Dict[str, Any]:
        """解析LLM响应"""
        try:
            # 尝试提取JSON（可能包含markdown代码块）
            json_match = re.search(r'\{[^{}]*\}', response, re.DOTALL)
            if json_match:
                json_str = json_match.group(0)
                return json.loads(json_str)
            else:
                # 如果没有找到JSON，尝试直接解析
                return json.loads(response)
        except json.JSONDecodeError as e:
            logger.warning(f"解析LLM响应失败: {str(e)}, response={response[:200]}")
            return {}
    
    def _validate_information(self, extracted_info: Dict[str, Any]) -> Dict[str, Any]:
        """验证信息完整性"""
        validated = {}
        
        # 验证持续时间格式
        if extracted_info.get("symptom_duration"):
            duration = extracted_info["symptom_duration"]
            if isinstance(duration, str) and any(keyword in duration for keyword in ["天", "小时", "周", "月", "年"]):
                validated["symptom_duration"] = duration
        
        # 验证严重程度（0-10）
        if extracted_info.get("symptom_severity") is not None:
            severity = extracted_info["symptom_severity"]
            if isinstance(severity, (int, float)) and 0 <= severity <= 10:
                validated["symptom_severity"] = int(severity)
        
        # 验证其他字段
        for field in ["symptom_location", "symptom_trigger", "symptom_frequency", "symptom_relief"]:
            if extracted_info.get(field):
                validated[field] = extracted_info[field]
        
        # 验证伴随症状
        if extracted_info.get("accompanying_symptoms"):
            accompanying = extracted_info["accompanying_symptoms"]
            if isinstance(accompanying, list):
                validated["accompanying_symptoms"] = accompanying
            elif isinstance(accompanying, str):
                validated["accompanying_symptoms"] = [accompanying]
        
        return validated
    
    def _fallback_rule_based_extraction(self, text: str) -> Dict[str, Any]:
        """降级策略：使用规则匹配"""
        extracted = {}
        
        # 提取持续时间
        duration_patterns = [
            r'(\d+)\s*天',
            r'(\d+)\s*小时',
            r'(\d+)\s*周',
            r'(\d+)\s*个月',
        ]
        for pattern in duration_patterns:
            match = re.search(pattern, text)
            if match:
                extracted["symptom_duration"] = match.group(0)
                break
        
        # 提取严重程度（0-10分）
        severity_patterns = [
            r'(\d+)\s*分',
            r'(\d+)\s*级',
        ]
        for pattern in severity_patterns:
            match = re.search(pattern, text)
            if match:
                try:
                    severity = int(match.group(1))
                    if 0 <= severity <= 10:
                        extracted["symptom_severity"] = severity
                except ValueError:
                    pass
                break
        
        # 提取伴随症状
        accompanying_keywords = ['还', '另外', '同时', '伴有', '伴随']
        for keyword in accompanying_keywords:
            if keyword in text:
                extracted["accompanying_symptoms"] = [text]
                break
        
        return {
            **extracted,
            "confidence": 0.6  # 规则匹配的置信度较低
        }

