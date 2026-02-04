"""
响应解析器：解析LLM返回的JSON结果
"""
import json
import re
from typing import Dict, Any, Optional
from app.utils.logger import logger


class ResponseParser:
    """响应解析器"""
    
    @staticmethod
    def parse_json_response(response: str) -> Optional[Dict[str, Any]]:
        """
        解析LLM返回的JSON响应
        
        Args:
            response: LLM返回的文本
            
        Returns:
            解析后的字典，如果解析失败返回None
        """
        if not response:
            return None
        
        # 尝试直接解析JSON
        try:
            return json.loads(response)
        except json.JSONDecodeError:
            pass
        
        # 尝试提取JSON代码块
        json_match = re.search(r'```json\s*(\{.*?\})\s*```', response, re.DOTALL)
        if json_match:
            try:
                return json.loads(json_match.group(1))
            except json.JSONDecodeError:
                pass
        
        # 尝试提取大括号内的内容
        brace_match = re.search(r'\{.*\}', response, re.DOTALL)
        if brace_match:
            try:
                return json.loads(brace_match.group(0))
            except json.JSONDecodeError:
                pass
        
        # 如果都失败了，记录警告
        logger.warning(f"无法解析LLM响应为JSON: {response[:200]}")
        return None
    
    @staticmethod
    def validate_intent_result(data: Dict[str, Any]) -> bool:
        """
        验证意图识别结果
        
        Args:
            data: 解析后的数据
            
        Returns:
            是否有效
        """
        required_fields = ["intent", "confidence", "reasoning"]
        if not all(field in data for field in required_fields):
            return False
        
        # 验证intent值
        valid_intents = ["screening", "diagnosis", "mixed", "unknown"]
        if data.get("intent") not in valid_intents:
            return False
        
        # 验证confidence范围
        confidence = data.get("confidence", 0)
        if not isinstance(confidence, (int, float)) or not (0 <= confidence <= 1):
            return False
        
        return True
    
    @staticmethod
    def validate_entity_result(data: Dict[str, Any]) -> bool:
        """
        验证实体提取结果
        
        Args:
            data: 解析后的数据
            
        Returns:
            是否有效
        """
        # 至少应该有一个symptoms字段（即使是空列表）
        if "symptoms" not in data:
            return False
        
        return True

