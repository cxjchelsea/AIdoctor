"""
需求分类器
"""
from typing import Dict, Any
import logging

logger = logging.getLogger(__name__)


class DemandClassifier:
    """需求分类器"""
    
    def classify(self, user_input: str) -> Dict[str, Any]:
        """
        分类用户需求
        
        Args:
            user_input: 用户输入
            
        Returns:
            分类结果
        """
        logger.info(f"分类用户需求: user_input={user_input}")
        
        # 简化实现：基于关键词匹配的需求分类
        user_input_lower = user_input.lower() if user_input else ""
        
        # 需求类型关键词
        screening_keywords = ["体检", "筛查", "检查", "健康管理", "预防", "健康评估"]
        goal_keywords = ["目标", "计划", "改善", "提升", "健康目标"]
        report_keywords = ["报告", "解读", "结果", "化验单", "检查单"]
        
        demand_type = "screening"  # 默认：筛查建议
        confidence = 0.7
        
        if any(keyword in user_input_lower for keyword in goal_keywords):
            demand_type = "goal_management"
            confidence = 0.8
        elif any(keyword in user_input_lower for keyword in report_keywords):
            demand_type = "report_interpretation"
            confidence = 0.8
        elif any(keyword in user_input_lower for keyword in screening_keywords):
            demand_type = "screening"
            confidence = 0.9
        
        logger.info(f"需求分类结果: demand_type={demand_type}, confidence={confidence}")
        
        return {
            "demand_type": demand_type,
            "confidence": confidence,
            "intent": user_input
        }

