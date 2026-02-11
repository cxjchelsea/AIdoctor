#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
知识图谱结构搭建脚本
功能：创建所有约束和索引
使用方法：
    python scripts/setup_schema.py --uri bolt://localhost:7687 --user neo4j --password your_password
"""
import asyncio
import sys
import os
from pathlib import Path

# 获取当前脚本所在目录（scripts目录）
SCRIPT_DIR = Path(__file__).resolve().parent
# 获取服务目录（knowledge-management-service）
SERVICE_DIR = SCRIPT_DIR.parent
# 获取项目根目录（AIdoctor）
PROJECT_ROOT = SERVICE_DIR.parent

# 加载根目录的 .env 文件
try:
    from dotenv import load_dotenv
    env_path = PROJECT_ROOT / ".env"
    if env_path.exists():
        load_dotenv(env_path, override=True)
        print(f"已加载环境变量文件: {env_path}")
    else:
        print(f"警告: 未找到根目录 .env 文件: {env_path}")
        # 尝试加载服务目录的 .env 文件作为备选
        local_env = SERVICE_DIR / ".env"
        if local_env.exists():
            load_dotenv(local_env, override=True)
            print(f"已加载本地环境变量文件: {local_env}")
except ImportError:
    print("警告: python-dotenv 未安装，将使用默认配置或命令行参数")

# 添加服务目录到路径
sys.path.insert(0, str(SERVICE_DIR))

from app.services.schema_service import SchemaService
from app.utils.neo4j_client import Neo4jClient
from app.config.settings import settings


async def main():
    """主函数"""
    import argparse
    parser = argparse.ArgumentParser(description='搭建知识图谱结构')
    parser.add_argument('--uri', default=None, help='Neo4j连接URI（默认从配置读取）')
    parser.add_argument('--user', default=None, help='Neo4j用户名（默认从配置读取）')
    parser.add_argument('--password', default=None, help='Neo4j密码（默认从配置读取）')
    
    args = parser.parse_args()
    
    # 使用命令行参数或配置
    uri = args.uri or settings.neo4j_uri
    user = args.user or settings.neo4j_user
    password = args.password or settings.neo4j_password
    
    if not password:
        print("❌ 错误：必须提供Neo4j密码（通过--password参数或环境变量）")
        sys.exit(1)
    
    print("=" * 60)
    print("知识图谱结构搭建")
    print("=" * 60)
    print(f"Neo4j URI: {uri}")
    print(f"用户: {user}")
    print()
    
    # 创建Neo4j客户端
    neo4j_client = Neo4jClient(
        uri=uri,
        user=user,
        password=password
    )
    
    try:
        # 创建结构搭建服务
        schema_service = SchemaService(neo4j_client=neo4j_client)
        
        # 搭建结构
        print("开始搭建知识图谱结构...")
        print()
        results = await schema_service.setup_schema()
        
        # 输出结果
        print()
        print("=" * 60)
        print("搭建结果")
        print("=" * 60)
        print(f"✅ 成功创建约束: {len(results['constraints_created'])} 个")
        if results['constraints_created']:
            for constraint in results['constraints_created']:
                print(f"   - {constraint}")
        
        print(f"✅ 成功创建索引: {len(results['indexes_created'])} 个")
        if results['indexes_created']:
            for index in results['indexes_created']:
                print(f"   - {index}")
        
        if results['constraints_failed']:
            print(f"⚠️  约束创建失败: {len(results['constraints_failed'])} 个")
            for constraint in results['constraints_failed']:
                print(f"   - {constraint}")
        
        if results['indexes_failed']:
            print(f"⚠️  索引创建失败: {len(results['indexes_failed'])} 个")
            for index in results['indexes_failed']:
                print(f"   - {index}")
        
        if results['errors']:
            print(f"❌ 错误: {len(results['errors'])} 个")
            for error in results['errors']:
                print(f"   - {error}")
        
        # 验证结果
        if 'validation' in results:
            validation = results['validation']
            print()
            print("=" * 60)
            print("验证结果")
            print("=" * 60)
            print(f"约束数量: {validation['constraints_count']}")
            print(f"索引数量: {validation['indexes_count']}")
            
            if validation['is_valid']:
                print("✅ 结构验证通过！")
            else:
                print("⚠️  结构验证未完全通过")
                if validation.get('missing_constraints'):
                    print(f"缺少约束: {validation['missing_constraints']}")
        
        print()
        print("=" * 60)
        print("✅ 结构搭建完成！")
        print("=" * 60)
        
    except Exception as e:
        print(f"❌ 结构搭建失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
    finally:
        neo4j_client.close()


if __name__ == "__main__":
    asyncio.run(main())

