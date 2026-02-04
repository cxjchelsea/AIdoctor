"""
Neo4j客户端工具
"""
from typing import Optional, List, Dict, Any
import logging
from neo4j import GraphDatabase

logger = logging.getLogger(__name__)


class Neo4jClient:
    """Neo4j客户端"""
    
    def __init__(
        self,
        uri: str,
        user: str,
        password: str,
        max_connection_lifetime: int = 3600,
        max_connection_pool_size: int = 50,
        connection_acquisition_timeout: int = 60
    ):
        """
        初始化Neo4j客户端
        
        Args:
            uri: Neo4j连接URI
            user: 用户名
            password: 密码
            max_connection_lifetime: 连接最大生存时间（秒）
            max_connection_pool_size: 连接池最大大小
            connection_acquisition_timeout: 获取连接超时时间（秒）
        """
        self.uri = uri
        self.user = user
        self.password = password
        self.driver = GraphDatabase.driver(
            uri,
            auth=(user, password),
            max_connection_lifetime=max_connection_lifetime,
            max_connection_pool_size=max_connection_pool_size,
            connection_acquisition_timeout=connection_acquisition_timeout
        )
        logger.info(f"Neo4j客户端初始化完成: {uri}")
    
    def close(self):
        """关闭连接"""
        if self.driver:
            self.driver.close()
            logger.info("Neo4j连接已关闭")
    
    def execute_query(self, query: str, parameters: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
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
    
    def execute_write(self, query: str, parameters: Optional[Dict[str, Any]] = None) -> int:
        """
        执行写操作（CREATE, UPDATE, DELETE等）
        
        Args:
            query: Cypher查询语句
            parameters: 查询参数
            
        Returns:
            影响的行数
        """
        with self.driver.session() as session:
            result = session.run(query, parameters or {})
            return result.consume().counters.nodes_created + result.consume().counters.nodes_deleted

