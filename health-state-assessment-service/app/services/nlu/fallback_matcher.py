"""
降级匹配器：当LLM不可用时的规则匹配方案
"""
import re
from typing import Dict, Any, List
from app.utils.logger import logger


class FallbackMatcher:
    """降级匹配器（基于关键词和正则表达式）"""
    
    # 筛查关键词
    SCREENING_KEYWORDS = [
        "体检", "筛查", "检查", "健康管理", "预防", "健康评估",
        "想查", "需要检查", "做体检", "健康体检", "年度体检"
    ]
    
    # 症状关键词
    SYMPTOM_KEYWORDS = [
        "痛", "疼", "不适", "难受", "不舒服", "异常", "问题",
        "症状", "困扰", "担心", "害怕", "有问题"
    ]
    
    # 症状匹配模式
    SYMPTOM_PATTERNS = [
        r"([^，。！？\s]+(?:痛|疼|不适|难受|不舒服|异常))",
        r"([^，。！？\s]+(?:症状|困扰|问题))"
    ]
    
    @classmethod
    def match_intent(cls, user_input: str) -> Dict[str, Any]:
        """
        使用关键词匹配识别意图（降级方案）
        
        Args:
            user_input: 用户输入文本
            
        Returns:
            意图识别结果
        """
        has_screening_intent = any(keyword in user_input for keyword in cls.SCREENING_KEYWORDS)
        
        # 检查是否包含症状描述
        has_symptoms_text = False
        if has_screening_intent:
            # 如果有筛查意图，需要更严格地判断症状
            wellness_context_keywords = ["健康状况", "健康状态", "健康评估", "健康管理", "健康检查", "健康体检"]
            is_wellness_context = any(keyword in user_input for keyword in wellness_context_keywords)
            
            if not is_wellness_context:
                has_symptoms_text = any(keyword in user_input for keyword in cls.SYMPTOM_KEYWORDS)
        else:
            has_symptoms_text = any(keyword in user_input for keyword in cls.SYMPTOM_KEYWORDS)
        
        # 判断意图类型
        if has_screening_intent and has_symptoms_text:
            strong_screening_keywords = ["体检", "筛查", "健康体检", "年度体检", "做体检"]
            has_strong_screening = any(keyword in user_input for keyword in strong_screening_keywords)
            if has_strong_screening:
                intent = "screening"
                has_symptoms_text = False
            else:
                intent = "mixed"
        elif has_screening_intent:
            intent = "screening"
        elif has_symptoms_text:
            intent = "diagnosis"
        else:
            intent = "unknown"
        
        return {
            "intent": intent,
            "confidence": 0.6,  # 降级方案的置信度较低
            "reasoning": "使用关键词匹配识别意图（降级方案）",
            "has_screening_intent": has_screening_intent,
            "has_symptom_intent": has_symptoms_text,
            "details": {}
        }
    
    @classmethod
    def extract_entities(cls, user_input: str, intent: str) -> List[Dict[str, Any]]:
        """
        使用正则表达式提取实体（降级方案）
        
        Args:
            user_input: 用户输入文本
            intent: 用户意图
            
        Returns:
            实体列表
        """
        entities = []
        
        # 只有在没有筛查意图，或者明确有症状时，才提取症状实体
        has_screening_intent = any(keyword in user_input for keyword in cls.SCREENING_KEYWORDS)
        if not has_screening_intent or intent == "diagnosis":
            for pattern in cls.SYMPTOM_PATTERNS:
                matches = re.findall(pattern, user_input)
                for match in matches:
                    # 排除健康管理相关的词汇
                    if "健康" not in match and "体检" not in match and "筛查" not in match:
                        if match not in [e.get("value") for e in entities if e.get("type") == "symptom"]:
                            entities.append({
                                "type": "symptom",
                                "value": match,
                                "original_text": match
                            })
        
        return entities

