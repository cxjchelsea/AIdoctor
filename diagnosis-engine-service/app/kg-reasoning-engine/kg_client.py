"""
Neo4j客户端
用于连接和操作Neo4j知识图谱数据库
"""

from typing import List, Dict, Any, Optional
from neo4j import GraphDatabase


class Neo4jClient:
    """Neo4j客户端"""
    
    def __init__(
        self,
        uri: str,
        user: str,
        password: str
    ):
        """
        初始化Neo4j客户端
        
        Args:
            uri: Neo4j连接URI
            user: 用户名
            password: 密码
        """
        self.driver = GraphDatabase.driver(uri, auth=(user, password))
    
    def close(self):
        """关闭数据库连接"""
        self.driver.close()
    
    def execute_query(
        self,
        query: str,
        parameters: Optional[Dict[str, Any]] = None
    ) -> List[Dict[str, Any]]:
        """
        执行Cypher查询
        
        Args:
            query: Cypher查询语句
            parameters: 查询参数
            
        Returns:
            查询结果列表
        """
        with self.driver.session() as session:
            result = session.run(query, parameters or {})
            return [record.data() for record in result]
    
    def find_paths(
        self,
        start_node: str,
        end_node: str,
        max_hops: int = 3,
        relation_types: Optional[List[str]] = None
    ) -> List[Dict[str, Any]]:
        """
        查找两个节点之间的路径
        
        Args:
            start_node: 起始节点（CUI或节点ID）
            end_node: 结束节点（CUI或节点ID）
            max_hops: 最大跳数
            relation_types: 关系类型过滤
            
        Returns:
            路径列表
        """
        # 构建Cypher查询
        if relation_types:
            relation_filter = ":" + "|".join(relation_types)
        else:
            relation_filter = ""
        
        query = f"""
        MATCH path = (start)-[*1..{max_hops}{relation_filter}]->(end)
        WHERE (start.cui = $start_node OR id(start) = $start_node_id)
           AND (end.cui = $end_node OR id(end) = $end_node_id)
        RETURN path, 
               [node in nodes(path) | node.name] as node_names,
               [rel in relationships(path) | type(rel)] as relation_types,
               length(path) as path_length
        LIMIT 50
        """
        
        try:
            results = self.execute_query(
                query,
                {
                    "start_node": start_node,
                    "start_node_id": int(start_node) if start_node.isdigit() else None,
                    "end_node": end_node,
                    "end_node_id": int(end_node) if end_node.isdigit() else None
                }
            )
            
            paths = []
            for record in results:
                paths.append({
                    "path": record.get("path"),
                    "node_names": record.get("node_names", []),
                    "relation_types": record.get("relation_types", []),
                    "path_length": record.get("path_length", 0)
                })
            
            return paths
        except Exception as e:
            # 如果查询失败，返回空列表
            return []
    
    def get_node_properties(
        self,
        node_id: str
    ) -> Dict[str, Any]:
        """
        获取节点属性
        
        Args:
            node_id: 节点ID
            
        Returns:
            节点属性字典
        """
        query = "MATCH (n) WHERE id(n) = $node_id RETURN n"
        result = self.execute_query(query, {"node_id": node_id})
        if result:
            return result[0].get('n', {})
        return {}

