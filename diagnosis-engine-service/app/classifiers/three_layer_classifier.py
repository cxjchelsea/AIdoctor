"""
三层分层分类器
"""
from typing import Dict, List, Optional
import logging

logger = logging.getLogger(__name__)


class ThreeLayerClassifier:
    """三层分层分类器"""
    
    def __init__(self):
        # 高危诊断关键词
        self.high_risk_keywords = [
            "心肌梗死", "脑梗死", "肺栓塞", "主动脉夹层",
            "急性心肌梗死", "急性脑梗死", "急性肺栓塞",
            "急性冠脉综合征", "脑出血", "蛛网膜下腔出血"
        ]
    
    def classify(self, possibilities: Dict[str, float]) -> Dict:
        """
        将候选疾病分为三层
        
        Args:
            possibilities: 疾病可能性字典 {疾病名: 可能性分数}
            
        Returns:
            三层分层结果
        """
        logger.info("进行三层分层分类")
        
        if not possibilities:
            return {
                "primary_hypothesis": None,
                "main_alternatives": [],
                "must_exclude": None,
                "all_candidates": {}
            }
        
        # 1. 识别高危诊断
        high_risk_diseases = self._identify_high_risk_diseases(possibilities)
        
        # 2. 选择首要假设
        primary_hypothesis = self._select_primary_hypothesis(possibilities, high_risk_diseases)
        
        # 3. 选择主要备选诊断
        main_alternatives = self._select_main_alternatives(possibilities, primary_hypothesis, high_risk_diseases)
        
        # 4. 选择必须排除的高危诊断
        must_exclude = self._select_must_exclude(possibilities, high_risk_diseases)
        
        return {
            "primary_hypothesis": primary_hypothesis,
            "main_alternatives": main_alternatives,
            "must_exclude": must_exclude,
            "all_candidates": possibilities
        }
    
    def _identify_high_risk_diseases(self, possibilities: Dict[str, float]) -> List[str]:
        """识别高危诊断"""
        return [
            disease for disease in possibilities.keys()
            if any(keyword in disease for keyword in self.high_risk_keywords)
        ]
    
    def _select_primary_hypothesis(
            self, 
            possibilities: Dict[str, float], 
            high_risk_diseases: List[str]) -> Optional[Dict]:
        """选择首要假设（1个）"""
        # 排除高危诊断
        filtered = {
            k: v for k, v in possibilities.items()
            if k not in high_risk_diseases
        }
        
        if not filtered:
            return None
        
        # 选择可能性最高的
        top_disease = max(filtered.items(), key=lambda x: x[1])
        
        return {
            "disease": top_disease[0],
            "score": top_disease[1],
            "layer": "primary_hypothesis",
            "evidence": "当前信息最能支持、最符合整体表现的方向"
        }
    
    def _select_main_alternatives(
            self,
            possibilities: Dict[str, float],
            primary_hypothesis: Optional[Dict],
            high_risk_diseases: List[str]) -> List[Dict]:
        """选择主要备选诊断（1-2个）"""
        if not primary_hypothesis:
            return []
        
        primary_disease = primary_hypothesis["disease"]
        
        # 排除首要假设和高危诊断
        filtered = {
            k: v for k, v in possibilities.items()
            if k != primary_disease and k not in high_risk_diseases
        }
        
        # 选择可能性较高的1-2个
        sorted_candidates = sorted(
            filtered.items(), 
            key=lambda x: x[1], 
            reverse=True
        )[:2]
        
        return [
            {
                "disease": disease,
                "score": score,
                "layer": "main_alternative",
                "evidence": "与首要假设并列需要对比、仍可能成立的方向"
            }
            for disease, score in sorted_candidates
        ]
    
    def _select_must_exclude(
            self,
            possibilities: Dict[str, float],
            high_risk_diseases: List[str]) -> Optional[Dict]:
        """选择必须排除的高危诊断（0-1个）"""
        if not high_risk_diseases:
            return None
        
        # 选择可能性最高的高危诊断
        high_risk_scores = {
            disease: possibilities.get(disease, 0.0)
            for disease in high_risk_diseases
        }
        
        if not high_risk_scores:
            return None
        
        top_high_risk = max(high_risk_scores.items(), key=lambda x: x[1])
        
        return {
            "disease": top_high_risk[0],
            "score": top_high_risk[1],
            "layer": "must_exclude",
            "reason": "高危诊断，必须排除，即使可能性不高"
        }

