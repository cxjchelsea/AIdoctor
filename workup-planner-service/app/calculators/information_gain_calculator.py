"""
信息增益计算器
"""
from typing import Dict, Any, List
import logging
import math

logger = logging.getLogger(__name__)


class InformationGainCalculator:
    """信息增益计算器"""
    
    def __init__(self):
        """初始化信息增益计算器"""
        # 检查结果-诊断概率分布（初期使用简单规则，后期可扩展为ML模型）
        self.test_result_probabilities = self._load_test_result_probabilities()
    
    def _load_test_result_probabilities(self) -> Dict[str, Dict[str, Dict[str, float]]]:
        """
        加载检查结果-诊断概率分布
        格式: {test_name: {result: {disease: probability}}}
        
        Returns:
            检查结果-诊断概率分布字典
        """
        # 示例规则：检查名称 -> {结果: {疾病: 概率}}
        rules = {
            "心电图": {
                "异常": {
                    "急性心肌梗死": 0.7,
                    "不稳定心绞痛": 0.6,
                    "心律失常": 0.5
                },
                "正常": {
                    "急性心肌梗死": 0.1,
                    "不稳定心绞痛": 0.2,
                    "心律失常": 0.1
                }
            },
            "心肌酶谱": {
                "升高": {
                    "急性心肌梗死": 0.9,
                    "心肌炎": 0.6
                },
                "正常": {
                    "急性心肌梗死": 0.05,
                    "心肌炎": 0.2
                }
            }
        }
        return rules
    
    def calculate_entropy(self, probabilities: List[float]) -> float:
        """
        计算信息熵
        H(X) = -Σ P(x) * log2(P(x))
        
        Args:
            probabilities: 概率列表
            
        Returns:
            信息熵值
        """
        entropy = 0.0
        for prob in probabilities:
            if prob > 0:
                entropy -= prob * math.log2(prob)
        return entropy
    
    def calculate_conditional_entropy(
        self, 
        test_name: str, 
        ddx_list: List[Dict[str, Any]]
    ) -> float:
        """
        计算条件熵
        H(X|Y) = -Σ P(y) * Σ P(x|y) * log2(P(x|y))
        
        Args:
            test_name: 检查名称
            ddx_list: 鉴别诊断列表
            
        Returns:
            条件熵值
        """
        test_rules = self.test_result_probabilities.get(test_name, {})
        if not test_rules:
            # 如果没有规则，返回0（无法计算）
            return 0.0
        
        # 获取所有可能的结果
        possible_results = list(test_rules.keys())
        if not possible_results:
            return 0.0
        
        # 计算每个结果的概率（简化：假设各结果等概率）
        result_probability = 1.0 / len(possible_results)
        
        conditional_entropy = 0.0
        
        for result in possible_results:
            result_rules = test_rules.get(result, {})
            
            # 计算该结果下的诊断概率分布
            conditional_probs = []
            for ddx_item in ddx_list:
                disease = ddx_item.get("disease", "")
                prior_prob = ddx_item.get("probability", 0.0)
                
                # 获取条件概率 P(disease|result)
                conditional_prob = result_rules.get(disease, 0.0)
                
                # 使用贝叶斯更新（简化版）
                # P(disease|result) = P(result|disease) * P(disease) / P(result)
                # 这里简化处理，直接使用条件概率
                if conditional_prob > 0:
                    conditional_probs.append(conditional_prob * prior_prob)
            
            # 归一化条件概率
            total = sum(conditional_probs)
            if total > 0:
                normalized_probs = [p / total for p in conditional_probs]
                result_entropy = self.calculate_entropy(normalized_probs)
                conditional_entropy += result_probability * result_entropy
        
        return conditional_entropy
    
    def calculate_gain(self, test_name: str, ddx_list: List[Dict[str, Any]]) -> float:
        """
        计算检查的信息增益
        IG(X,Y) = H(X) - H(X|Y)
        
        Args:
            test_name: 检查名称
            ddx_list: 鉴别诊断列表，每个元素包含disease和probability字段
            
        Returns:
            信息增益值（0-1）
        """
        logger.info(f"计算信息增益: {test_name}, DDx数量: {len(ddx_list)}")
        
        if not ddx_list:
            return 0.0
        
        # 提取诊断概率
        probabilities = [ddx_item.get("probability", 0.0) for ddx_item in ddx_list]
        
        # 归一化概率
        total = sum(probabilities)
        if total == 0:
            return 0.0
        
        normalized_probs = [p / total for p in probabilities]
        
        # 计算信息熵 H(X)
        entropy = self.calculate_entropy(normalized_probs)
        
        # 计算条件熵 H(X|Y)
        conditional_entropy = self.calculate_conditional_entropy(test_name, ddx_list)
        
        # 计算信息增益
        information_gain = entropy - conditional_entropy
        
        # 归一化到0-1范围（信息增益最大值为熵值）
        if entropy > 0:
            normalized_gain = information_gain / entropy
        else:
            normalized_gain = 0.0
        
        # 确保返回值在0-1范围内
        return min(max(normalized_gain, 0.0), 1.0)

