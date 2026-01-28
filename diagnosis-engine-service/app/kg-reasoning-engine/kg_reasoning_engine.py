"""
知识图谱推理引擎（DR.KNOWS核心方法）
"""
from typing import List, Dict, Any, Optional
import logging
# 使用绝对导入，避免相对导入问题
import sys
import os
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)
from kg_client import Neo4jClient
from path_retriever import PathRetriever
from path_scorer import PathScorer
from path_injector import PathInjector

logger = logging.getLogger(__name__)


class KGReasoningEngine:
    """知识图谱推理引擎"""
    
    def __init__(
        self,
        kg_client: Optional[Neo4jClient] = None,
        path_retriever: Optional[PathRetriever] = None,
        path_scorer: Optional[PathScorer] = None,
        path_injector: Optional[PathInjector] = None
    ):
        """
        初始化知识图谱推理引擎
        
        Args:
            kg_client: Neo4j客户端
            path_retriever: 路径检索器
            path_scorer: 路径评分器
            path_injector: 路径注入器
        """
        self.kg_client = kg_client
        self.path_retriever = path_retriever or PathRetriever(kg_client) if kg_client else None
        self.path_scorer = path_scorer or PathScorer()
        self.path_injector = path_injector or PathInjector()
        
        logger.info("知识图谱推理引擎初始化完成")
    
    def retrieve_paths(
        self, 
        symptom_cuis: List[str], 
        max_hops: int = 4,
        min_hops: int = 2
    ) -> List[Dict[str, Any]]:
        """
        多跳推理路径检索（DR.KNOWS核心方法）
        使用Neo4j进行图遍历
        
        Args:
            symptom_cuis: 症状CUI编码列表
            max_hops: 最大跳数
            min_hops: 最小跳数
            
        Returns:
            路径列表
        """
        if not self.path_retriever:
            logger.warning("路径检索器未初始化")
            return []
        
        try:
            paths = self.path_retriever.retrieve_disease_paths(
                symptoms=symptom_cuis,
                max_hops=max_hops
            )
            logger.info(f"检索到 {len(paths)} 条路径")
            return paths
        except Exception as e:
            logger.error(f"路径检索失败: {str(e)}", exc_info=True)
            return []
    
    def score_paths(
        self, 
        paths: List[Dict[str, Any]], 
        evidence: Dict[str, Any]
    ) -> List[Dict[str, Any]]:
        """
        路径评分排序（DR.KNOWS三层评分体系）
        - 先验概率评分
        - 似然评分
        - 后验概率评分
        
        Args:
            paths: 路径列表
            evidence: 证据信息（症状、体征等）
            
        Returns:
            带评分的路径列表（按后验概率排序）
        """
        if not paths:
            return []
        
        try:
            # 使用路径评分器进行三层评分
            scored_paths = self.path_scorer.score_paths(paths, evidence)
            
            # 按后验概率排序
            scored_paths.sort(
                key=lambda p: p.get('scores', {}).get('posterior', 0.0),
                reverse=True
            )
            
            logger.info(f"完成 {len(scored_paths)} 条路径的评分")
            return scored_paths
        except Exception as e:
            logger.error(f"路径评分失败: {str(e)}", exc_info=True)
            return paths  # 返回原始路径
    
    def build_enhanced_prompt(
        self, 
        patient_info: str, 
        ranked_paths: List[Dict[str, Any]],
        max_paths: int = 5
    ) -> str:
        """
        路径注入LLM（DR.KNOWS核心方法）
        构建包含推理路径的Prompt
        
        Args:
            patient_info: 患者信息
            ranked_paths: 已排序的路径列表
            max_paths: 最大注入路径数
            
        Returns:
            增强后的Prompt
        """
        if not ranked_paths:
            return patient_info
        
        # 选择Top N路径
        top_paths = ranked_paths[:max_paths]
        
        # 使用路径注入器构建增强Prompt
        enhanced_prompt = self.path_injector.inject_paths(
            prompt=patient_info,
            paths=top_paths,
            max_paths=max_paths
        )
        
        return enhanced_prompt
    
    def reasoning(
        self,
        symptom_cuis: List[str],
        evidence: Dict[str, Any],
        max_hops: int = 4,
        max_paths: int = 5
    ) -> Dict[str, Any]:
        """
        完整的推理流程：检索 -> 评分 -> 排序
        
        Args:
            symptom_cuis: 症状CUI编码列表
            evidence: 证据信息
            max_hops: 最大跳数
            max_paths: 最大返回路径数
            
        Returns:
            推理结果，包含路径列表和统计信息
        """
        # 1. 路径检索
        paths = self.retrieve_paths(symptom_cuis, max_hops=max_hops)
        
        if not paths:
            return {
                "paths": [],
                "total_paths": 0,
                "top_paths": []
            }
        
        # 2. 路径评分
        scored_paths = self.score_paths(paths, evidence)
        
        # 3. 选择Top N路径
        top_paths = scored_paths[:max_paths]
        
        return {
            "paths": scored_paths,
            "total_paths": len(scored_paths),
            "top_paths": top_paths,
            "statistics": {
                "avg_posterior": sum(
                    p.get('scores', {}).get('posterior', 0.0) 
                    for p in scored_paths
                ) / len(scored_paths) if scored_paths else 0.0,
                "max_posterior": max(
                    (p.get('scores', {}).get('posterior', 0.0) 
                     for p in scored_paths),
                    default=0.0
                )
            }
        }

