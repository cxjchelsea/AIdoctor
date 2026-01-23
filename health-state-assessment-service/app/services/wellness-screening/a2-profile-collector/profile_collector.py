"""
健康画像收集器
"""
from typing import Dict, Any
import logging

logger = logging.getLogger(__name__)


class ProfileCollector:
    """健康画像收集器"""
    
    def collect(self, user_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        收集健康画像
        
        Args:
            user_data: 用户数据
            
        Returns:
            健康画像
        """
        logger.info(f"收集健康画像: user_data={user_data}")
        
        # 简化实现：从user_data中提取健康画像信息
        profile = {}
        completeness_score = 0.0
        
        # 基本信息
        if "basicInfo" in user_data:
            basic_info = user_data["basicInfo"]
            profile["basicInfo"] = basic_info
            if basic_info:
                completeness_score += 0.3
        
        # 症状信息
        if "symptoms" in user_data:
            symptoms = user_data["symptoms"]
            profile["symptoms"] = symptoms
            if symptoms:
                completeness_score += 0.2
        
        # 用户输入
        if "userInput" in user_data:
            user_input = user_data["userInput"]
            profile["userInput"] = user_input
            if user_input:
                completeness_score += 0.2
        
        # 生命体征
        if "vitalSigns" in user_data:
            vital_signs = user_data["vitalSigns"]
            profile["vitalSigns"] = vital_signs
            if vital_signs:
                completeness_score += 0.3
        
        # 计算完整度（0-1）
        completeness = min(completeness_score, 1.0)
        
        logger.info(f"健康画像收集完成: completeness={completeness}")
        
        return {
            "profile": profile,
            "completeness": completeness,
            "gaps": []  # 信息缺口（简化实现，返回空列表）
        }

