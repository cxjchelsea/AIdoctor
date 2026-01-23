"""
概念归一化器
"""
from typing import Dict, Optional
import logging

logger = logging.getLogger(__name__)


class ConceptNormalizer:
    """概念归一化器"""
    
    def __init__(self):
        # 症状概念映射表
        self.symptom_mapping = {
            "胸口闷": "胸闷样不适",
            "走几步就喘": "活动后气促",
            "心慌": "心悸",
            "肚子疼": "腹痛",
        }
    
    def normalize_concept(self, user_text: str, symptom_type: str = None) -> Dict:
        """
        将用户原话转换为标准医学概念
        
        Args:
            user_text: 用户原始文本
            symptom_type: 症状类型（可选）
            
        Returns:
            归一化结果
        """
        logger.info(f"归一化概念: {user_text}")
        
        normalized_symptom = self.symptom_mapping.get(user_text, user_text)
        
        return {
            "original_text": user_text,
            "normalized_symptom": normalized_symptom,
            "confidence": 0.9 if normalized_symptom in self.symptom_mapping.values() else 0.7
        }

