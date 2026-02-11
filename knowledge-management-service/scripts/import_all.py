#!/usr/bin/env python
"""
全量导入脚本
"""
import asyncio
import sys
from pathlib import Path

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from app.utils.neo4j_client import Neo4jClient
from app.config.settings import settings


async def main():
    """主函数"""
    print("开始全量导入...")
    
    # 初始化Neo4j客户端
    neo4j_client = Neo4jClient(
        uri=settings.neo4j_uri,
        user=settings.neo4j_user,
        password=settings.neo4j_password
    )
    
    # TODO: 实现全量导入逻辑
    print("全量导入完成")


if __name__ == "__main__":
    asyncio.run(main())
