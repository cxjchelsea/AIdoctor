"""
路径评分器（三层评分体系）
包含先验概率、似然、后验概率三个评分器
"""

from typing import List, Dict, Any
# 使用绝对导入，避免相对导入问题
import sys
import os
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)
from prior_scorer import PriorScorer
from likelihood_scorer import LikelihoodScorer
from posterior_scorer import PosteriorScorer


class PathScorer:
    """路径评分器（三层评分体系）"""
    
    def __init__(self):
        """初始化路径评分器"""
        self.prior_scorer = PriorScorer()
        self.likelihood_scorer = LikelihoodScorer()
        self.posterior_scorer = PosteriorScorer()
    
    def score_paths(
        self,
        paths: List[Dict[str, Any]],
        evidence: Dict[str, Any]
    ) -> List[Dict[str, Any]]:
        """
        对路径进行三层评分
        
        Args:
            paths: 路径列表
            evidence: 证据信息
            
        Returns:
            带评分的路径列表
        """
        scored_paths = []
        
        for path in paths:
            # 先验概率评分
            prior_score = self.prior_scorer.score(path)
            
            # 似然评分
            likelihood_score = self.likelihood_scorer.score(path, evidence)
            
            # 后验概率评分
            posterior_score = self.posterior_scorer.score(
                path, evidence, prior_score, likelihood_score
            )
            
            scored_paths.append({
                **path,
                'scores': {
                    'prior': prior_score,
                    'likelihood': likelihood_score,
                    'posterior': posterior_score
                }
            })
        
        return scored_paths

