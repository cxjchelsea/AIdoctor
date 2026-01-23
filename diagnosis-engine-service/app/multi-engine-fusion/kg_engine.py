"""
知识图谱引擎
基于知识图谱推理进行诊断（调用kg_reasoning_engine）
"""

from typing import Dict, Any, List
from .base_engine import BaseEngine
from app.kg_reasoning_engine.path_retriever import PathRetriever
from app.kg_reasoning_engine.path_scorer import PathScorer
from app.kg_reasoning_engine.kg_client import Neo4jClient


class KGEngine(BaseEngine):
    """知识图谱引擎"""
    
    def __init__(
        self,
        kg_client: Neo4jClient,
        path_retriever: PathRetriever = None,
        path_scorer: PathScorer = None
    ):
        """
        初始化知识图谱引擎
        
        Args:
            kg_client: Neo4j客户端
            path_retriever: 路径检索器
            path_scorer: 路径评分器
        """
        self.kg_client = kg_client
        self.path_retriever = path_retriever or PathRetriever(kg_client)
        self.path_scorer = path_scorer or PathScorer()
    
    def diagnose(
        self,
        symptoms: List[str],
        signs: Dict[str, Any],
        context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        基于知识图谱进行诊断
        
        Args:
            symptoms: 症状列表
            signs: 体征信息
            context: 上下文信息
            
        Returns:
            诊断结果
        """
        # 检索推理路径
        paths = self.path_retriever.retrieve_disease_paths(symptoms)
        
        # 评分路径
        evidence = {'symptoms': symptoms, 'signs': signs}
        scored_paths = self.path_scorer.score_paths(paths, evidence)
        
        # 提取疾病候选
        diseases = self._extract_diseases(scored_paths)
        
        return {
            'diseases': diseases,
            'confidence': self._calculate_confidence(scored_paths),
            'engine': self.get_engine_name(),
            'paths': scored_paths
        }
    
    def _extract_diseases(self, paths: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """从路径中提取疾病候选"""
        # TODO: 实现疾病提取逻辑
        return []
    
    def _calculate_confidence(self, paths: List[Dict[str, Any]]) -> float:
        """计算总体置信度"""
        if not paths:
            return 0.0
        # 使用最高后验概率作为置信度
        max_posterior = max(
            p.get('scores', {}).get('posterior', 0.0) for p in paths
        )
        return max_posterior
    
    def get_engine_name(self) -> str:
        """获取引擎名称"""
        return "kg_engine"

