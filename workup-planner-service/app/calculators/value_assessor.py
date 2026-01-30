"""
检查价值评估器
"""
from typing import Dict, Any, List
import logging
import math

logger = logging.getLogger(__name__)


class ValueAssessor:
    """检查价值评估器"""
    
    def __init__(self):
        """初始化检查价值评估器"""
        # 检查-诊断关联规则库（初期使用简单规则，后期可扩展为知识库）
        self.test_disease_rules = self._load_test_disease_rules()
        
    def _load_test_disease_rules(self) -> Dict[str, Dict[str, float]]:
        """
        加载检查-诊断关联规则库
        初期使用简单规则，后期可扩展为从数据库或知识库加载
        
        Returns:
            检查-诊断关联规则字典 {test_name: {disease: relevance_score}}
        """
        # 示例规则：检查名称 -> {疾病名称: 关联度评分(0-1)}
        rules = {
            "心电图": {
                "急性心肌梗死": 0.9,
                "不稳定心绞痛": 0.8,
                "心律失常": 0.7,
                "心肌炎": 0.6
            },
            "心肌酶谱": {
                "急性心肌梗死": 0.95,
                "心肌炎": 0.7
            },
            "胸部CT": {
                "肺炎": 0.9,
                "肺栓塞": 0.85,
                "肺癌": 0.8,
                "主动脉夹层": 0.9
            },
            "胸部X光": {
                "肺炎": 0.8,
                "肺结核": 0.7,
                "气胸": 0.9
            },
            "血常规": {
                "感染": 0.7,
                "贫血": 0.8,
                "白血病": 0.6
            },
            "尿常规": {
                "尿路感染": 0.9,
                "肾炎": 0.7,
                "糖尿病": 0.6
            }
        }
        return rules
    
    def assess_value(self, test_name: str, ddx_list: List[Dict[str, Any]]) -> float:
        """
        评估检查的价值
        基于诊断候选集和检查-诊断关联度，评估检查的价值
        
        Args:
            test_name: 检查名称
            ddx_list: 鉴别诊断列表，每个元素包含disease和probability字段
            
        Returns:
            价值评分（0-1）
        """
        logger.info(f"评估检查价值: {test_name}, DDx数量: {len(ddx_list)}")
        
        if not ddx_list:
            return 0.0
        
        # 获取检查的疾病关联规则
        test_rules = self.test_disease_rules.get(test_name, {})
        if not test_rules:
            # 如果没有规则，返回默认值
            logger.warning(f"检查 {test_name} 没有关联规则，返回默认值")
            return 0.3
        
        # 计算检查对诊断候选集的综合价值
        total_value = 0.0
        total_weight = 0.0
        
        for ddx_item in ddx_list:
            disease = ddx_item.get("disease", "")
            probability = ddx_item.get("probability", 0.0)
            
            # 获取检查对该疾病的关联度
            relevance = test_rules.get(disease, 0.0)
            
            # 价值 = 关联度 * 诊断概率（加权求和）
            value = relevance * probability
            total_value += value
            total_weight += probability
        
        # 归一化：如果总权重为0，返回0；否则返回加权平均
        if total_weight == 0:
            return 0.0
        
        normalized_value = total_value / total_weight if total_weight > 0 else 0.0
        
        # 确保返回值在0-1范围内
        return min(max(normalized_value, 0.0), 1.0)
    
    def assess_differentiation_value(self, test_name: str, ddx_list: List[Dict[str, Any]]) -> float:
        """
        评估检查的区分能力
        检查能区分哪些诊断，区分能力越强，价值越高
        
        Args:
            test_name: 检查名称
            ddx_list: 鉴别诊断列表
            
        Returns:
            区分能力评分（0-1）
        """
        if len(ddx_list) < 2:
            # 如果只有一个诊断候选，区分能力为0
            return 0.0
        
        test_rules = self.test_disease_rules.get(test_name, {})
        if not test_rules:
            return 0.0
        
        # 计算检查能覆盖的诊断数量
        covered_diseases = 0
        for ddx_item in ddx_list:
            disease = ddx_item.get("disease", "")
            if disease in test_rules and test_rules[disease] > 0.5:
                covered_diseases += 1
        
        # 区分能力 = 覆盖的诊断数量 / 总诊断数量
        differentiation_value = covered_diseases / len(ddx_list) if ddx_list else 0.0
        
        return min(max(differentiation_value, 0.0), 1.0)

