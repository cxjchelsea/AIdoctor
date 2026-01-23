"""
推理组织器
组织推理子组，设计分流路径
"""
from typing import Dict, List, Optional
import logging

logger = logging.getLogger(__name__)


class ReasoningOrganizer:
    """推理组织器"""
    
    def __init__(self):
        """初始化推理组织器"""
        logger.info("推理组织器初始化完成")
    
    def organize(
        self,
        possibilities: Dict[str, float],
        engine_results: Optional[Dict] = None
    ) -> Dict:
        """
        组织推理结构
        
        Args:
            possibilities: 疾病可能性字典
            engine_results: 各引擎结果（可选）
            
        Returns:
            推理组织结果
        """
        logger.info("组织推理结构")
        
        if not possibilities:
            return {
                "reasoning_groups": [],
                "triage_paths": []
            }
        
        # 1. 按置信度分组
        high_confidence = {
            disease: score 
            for disease, score in possibilities.items() 
            if score >= 0.7
        }
        medium_confidence = {
            disease: score 
            for disease, score in possibilities.items() 
            if 0.4 <= score < 0.7
        }
        low_confidence = {
            disease: score 
            for disease, score in possibilities.items() 
            if score < 0.4
        }
        
        # 2. 构建推理子组
        reasoning_groups = []
        if high_confidence:
            reasoning_groups.append({
                "group_name": "高置信度诊断",
                "diseases": high_confidence,
                "priority": "high"
            })
        if medium_confidence:
            reasoning_groups.append({
                "group_name": "中等置信度诊断",
                "diseases": medium_confidence,
                "priority": "medium"
            })
        if low_confidence:
            reasoning_groups.append({
                "group_name": "低置信度诊断",
                "diseases": low_confidence,
                "priority": "low"
            })
        
        # 3. 设计分流路径（基于引擎结果）
        triage_paths = []
        if engine_results:
            # 根据各引擎的一致性设计分流路径
            triage_paths = self._design_triage_paths(possibilities, engine_results)
        
        return {
            "reasoning_groups": reasoning_groups,
            "triage_paths": triage_paths,
            "information_gain": self._calculate_information_gain(possibilities)
        }
    
    def _design_triage_paths(
        self,
        possibilities: Dict[str, float],
        engine_results: Dict
    ) -> List[Dict]:
        """
        设计分流路径
        
        Args:
            possibilities: 疾病可能性字典
            engine_results: 各引擎结果
            
        Returns:
            分流路径列表
        """
        triage_paths = []
        
        # 如果知识图谱引擎有路径信息，优先使用
        kg_result = engine_results.get('kg', {})
        if kg_result and kg_result.get('reasoning_paths'):
            triage_paths.append({
                "path_type": "knowledge_graph",
                "description": "基于知识图谱推理路径",
                "priority": "high"
            })
        
        # 如果LLM引擎有推荐检查，添加检查路径
        llm_result = engine_results.get('llm', {})
        if llm_result and llm_result.get('recommended_tests'):
            triage_paths.append({
                "path_type": "recommended_tests",
                "description": "基于LLM推荐的检查路径",
                "tests": llm_result.get('recommended_tests', []),
                "priority": "medium"
            })
        
        return triage_paths
    
    def _calculate_information_gain(self, possibilities: Dict[str, float]) -> float:
        """
        计算信息增益
        
        Args:
            possibilities: 疾病可能性字典
            
        Returns:
            信息增益值
        """
        if not possibilities:
            return 0.0
        
        # 简化的信息增益计算（基于熵）
        import math
        total = sum(possibilities.values())
        if total == 0:
            return 0.0
        
        entropy = 0.0
        for score in possibilities.values():
            if score > 0:
                p = score / total
                entropy -= p * math.log2(p) if p > 0 else 0
        
        # 信息增益 = 1 - 归一化熵
        max_entropy = math.log2(len(possibilities)) if len(possibilities) > 1 else 1.0
        normalized_entropy = entropy / max_entropy if max_entropy > 0 else 0.0
        information_gain = 1.0 - normalized_entropy
        
        return max(0.0, min(1.0, information_gain))

