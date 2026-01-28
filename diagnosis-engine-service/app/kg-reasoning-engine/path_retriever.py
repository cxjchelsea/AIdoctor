"""
多跳推理路径检索器
用于从知识图谱中检索多跳推理路径
"""

from typing import List, Dict, Any, Optional
# 使用绝对导入，避免相对导入问题
import sys
import os
# 添加当前目录到路径
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)
from kg_client import Neo4jClient


class PathRetriever:
    """多跳推理路径检索器"""
    
    def __init__(self, kg_client: Neo4jClient):
        """
        初始化路径检索器
        
        Args:
            kg_client: Neo4j客户端
        """
        self.kg_client = kg_client
    
    def retrieve_paths(
        self,
        source_concepts: List[str],
        target_concepts: List[str],
        max_hops: int = 3,
        relation_types: Optional[List[str]] = None
    ) -> List[Dict[str, Any]]:
        """
        检索多跳推理路径
        
        Args:
            source_concepts: 源概念列表（症状、体征等，CUI编码）
            target_concepts: 目标概念列表（疾病等，CUI编码）
            max_hops: 最大跳数
            relation_types: 关系类型过滤
            
        Returns:
            路径列表，每个路径包含节点和关系信息
        """
        all_paths = []
        
        # 对每个源概念和目标概念组合进行路径检索
        for source in source_concepts:
            for target in target_concepts:
                paths = self.kg_client.find_paths(
                    start_node=source,
                    end_node=target,
                    max_hops=max_hops,
                    relation_types=relation_types
                )
                all_paths.extend(paths)
        
        # 去重（基于路径节点序列）
        unique_paths = self._deduplicate_paths(all_paths)
        
        return unique_paths
    
    def retrieve_disease_paths(
        self,
        symptoms: List[str],
        max_hops: int = 3
    ) -> List[Dict[str, Any]]:
        """
        检索从症状到疾病的推理路径
        
        Args:
            symptoms: 症状列表（CUI编码或症状名称）
            max_hops: 最大跳数
            
        Returns:
            路径列表
        """
        # 构建Cypher查询：从症状到疾病的多跳路径
        query = """
        MATCH path = (s:Symptom)-[*2..{max_hops}]->(d:Disease)
        WHERE s.cui IN $symptom_cuis OR s.name IN $symptom_names
        WITH path, d, 
             [node in nodes(path) | node.name] as node_names,
             [rel in relationships(path) | type(rel)] as relation_types,
             length(path) as path_length
        RETURN DISTINCT path, node_names, relation_types, path_length,
               d.name as disease_name, d.cui as disease_cui
        ORDER BY path_length ASC
        LIMIT 100
        """
        
        try:
            # 分离CUI和名称
            symptom_cuis = [s for s in symptoms if s.startswith("C")]
            symptom_names = [s for s in symptoms if not s.startswith("C")]
            
            results = self.kg_client.execute_query(
                query.format(max_hops=max_hops),
                {
                    "symptom_cuis": symptom_cuis,
                    "symptom_names": symptom_names
                }
            )
            
            paths = []
            for record in results:
                paths.append({
                    "path": record.get("path"),
                    "node_names": record.get("node_names", []),
                    "relation_types": record.get("relation_types", []),
                    "path_length": record.get("path_length", 0),
                    "disease_name": record.get("disease_name"),
                    "disease_cui": record.get("disease_cui"),
                    "description": f"{' -> '.join(record.get('node_names', []))}"
                })
            
            # 去重
            unique_paths = self._deduplicate_paths(paths)
            
            return unique_paths
        except Exception as e:
            # 如果查询失败，返回空列表
            return []
    
    def _deduplicate_paths(self, paths: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        路径去重（基于节点序列）
        
        Args:
            paths: 路径列表
            
        Returns:
            去重后的路径列表
        """
        seen = set()
        unique_paths = []
        
        for path in paths:
            # 使用节点名称序列作为唯一标识
            node_sequence = tuple(path.get("node_names", []))
            if node_sequence not in seen:
                seen.add(node_sequence)
                unique_paths.append(path)
        
        return unique_paths

