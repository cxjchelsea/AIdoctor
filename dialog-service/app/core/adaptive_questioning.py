"""
智能追问策略
按照《dialog-service - 服务实现方案.md》实现
基于信息缺口分级生成智能追问问题
"""
from typing import Dict, List, Optional, Any
import logging

logger = logging.getLogger(__name__)


class AdaptiveQuestioningStrategy:
    """智能追问策略"""
    
    def __init__(self):
        # 问题类型优先级映射
        self.question_priority = {
            "symptom_trigger": 1,      # 最高优先级（必填）
            "chief_complaint": 1,       # 最高优先级（必填）
            "symptom_duration": 2,     # 高优先级（必填）
            "symptom_severity": 3,     # 中优先级（重要）
            "symptom_location": 4,     # 中优先级（重要）
            "symptom_frequency": 5,    # 中优先级（重要）
            "accompanying_symptoms": 6, # 低优先级（重要）
            "family_history": 7,       # 低优先级（可选）
            "past_history": 8,         # 低优先级（可选）
        }
    
    async def generate_question(
        self,
        information_gaps: Dict[str, List[Dict[str, Any]]],
        context: Dict[str, Any],
        completeness: float
    ) -> Optional[Dict[str, Any]]:
        """
        生成追问问题
        
        Args:
            information_gaps: 信息缺口分类（required/important/optional）
            context: 对话上下文
            completeness: 信息完整度
            
        Returns:
            问题信息字典，包含类型、缺失信息等
        """
        logger.info(f"生成追问问题: completeness={completeness:.2f}")
        
        # 如果完整度已经很高，不再追问
        if completeness >= 0.9:
            logger.info("信息完整度已足够，无需追问")
            return None
        
        # 选择最高优先级的信息缺口
        priority_gap = self._select_priority_gap(information_gaps)
        
        if not priority_gap:
            logger.info("没有需要追问的信息缺口")
            return None
        
        # 检查是否已经问过这个问题（避免重复）
        if self._is_already_asked(priority_gap, context):
            logger.info(f"问题已问过，跳过: {priority_gap.get('field')}")
            # 尝试下一个优先级的问题
            priority_gap = self._select_priority_gap(information_gaps, exclude=[priority_gap.get("field")])
            if not priority_gap:
                return None
        
        # 构建问题信息
        question_info = {
            "type": self._get_question_type(priority_gap.get("field", "")),
            "missing_info": [priority_gap],
            "priority": self.question_priority.get(priority_gap.get("field", ""), 99),
            "field": priority_gap.get("field", "")
        }
        
        return question_info
    
    def _select_priority_gap(
        self,
        information_gaps: Dict[str, List[Dict[str, Any]]],
        exclude: List[str] = None
    ) -> Optional[Dict[str, Any]]:
        """选择最高优先级的信息缺口"""
        exclude = exclude or []
        
        # 优先级：required > important > optional
        # 1. 先检查必填项
        for gap in information_gaps.get("required", []):
            if gap.get("field") not in exclude:
                return gap
        
        # 2. 再检查重要项
        for gap in information_gaps.get("important", []):
            if gap.get("field") not in exclude:
                return gap
        
        # 3. 最后检查可选项
        for gap in information_gaps.get("optional", []):
            if gap.get("field") not in exclude:
                return gap
        
        return None
    
    def _get_question_type(self, field: str) -> str:
        """根据字段获取问题类型"""
        type_mapping = {
            "chief_complaint": "chief_complaint",
            "symptom_trigger": "trigger",
            "symptom_duration": "duration",
            "symptom_severity": "severity",
            "symptom_location": "location",
            "symptom_frequency": "frequency",
            "accompanying_symptoms": "accompanying",
            "family_history": "family_history",
            "past_history": "past_history",
        }
        return type_mapping.get(field, "general")
    
    def _is_already_asked(self, gap: Dict[str, Any], context: Dict[str, Any]) -> bool:
        """检查是否已经问过这个问题"""
        conversation_history = context.get("conversationHistory", [])
        field = gap.get("field", "")
        
        # 检查最近3条对话中是否包含相关问题
        for msg in conversation_history[-3:]:
            if msg.get("role") == "assistant":
                content = msg.get("content", "").lower()
                # 简单的关键词匹配
                if field in content or gap.get("description", "").lower() in content:
                    return True
        
        return False

