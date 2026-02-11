"""
书籍接入与版本治理脚本（阶段一）

功能：
1. 书籍基础信息管理（书名、版本、ISBN、学科领域、使用状态）
2. 建立书籍注册表，为每本书分配唯一标识（书名_版本）
3. 支持多版本并存，便于版本对比和历史追溯
4. 记录书籍的完整元数据信息

输出格式：
- 书籍注册表（JSON格式）
- 每本书的唯一标识（book_id = 书名_版本）

使用方法：
    python book_registry.py register <pdf_path> [--name <name>] [--version <version>] [--isbn <isbn>] [--field <field>]
    python book_registry.py list
    python book_registry.py get <book_id>
    python book_registry.py update <book_id> [--status <status>]

作者：AI医生系统
日期：2024
"""

import json
import argparse
from typing import Dict, List, Optional
from pathlib import Path
from datetime import datetime
import re


class BookRegistry:
    """书籍注册表管理器"""
    
    # 使用状态枚举
    STATUS_ENABLED = "启用"
    STATUS_DISABLED = "停用"
    STATUS_RUNNING = "重跑中"
    
    # 学科领域枚举
    FIELDS = ["内科", "外科", "儿科", "妇科", "全科", "其他"]
    
    def __init__(self, registry_file: str = "book_registry.json"):
        """
        初始化书籍注册表
        
        Args:
            registry_file: 注册表文件路径
        """
        self.registry_file = Path(registry_file)
        self.books = self._load_registry()
    
    def _load_registry(self) -> Dict:
        """加载注册表"""
        if self.registry_file.exists():
            try:
                with open(self.registry_file, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception as e:
                print(f"加载注册表失败: {e}")
                return {}
        return {}
    
    def _save_registry(self):
        """保存注册表"""
        try:
            with open(self.registry_file, 'w', encoding='utf-8') as f:
                json.dump(self.books, f, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"保存注册表失败: {e}")
            raise
    
    def _generate_book_id(self, name: str, version: str) -> str:
        """
        生成书籍唯一标识
        
        Args:
            name: 书名
            version: 版本
            
        Returns:
            唯一标识（书名_版本）
        """
        # 清理名称和版本，移除特殊字符
        clean_name = re.sub(r'[^\w\u4e00-\u9fa5]', '_', name)
        clean_version = re.sub(r'[^\w\u4e00-\u9fa5]', '_', version)
        book_id = f"{clean_name}_{clean_version}"
        return book_id
    
    def register_book(
        self,
        pdf_path: str,
        name: Optional[str] = None,
        version: Optional[str] = None,
        isbn: Optional[str] = None,
        field: Optional[str] = None,
        status: str = STATUS_ENABLED
    ) -> Dict:
        """
        注册新书籍
        
        Args:
            pdf_path: PDF文件路径
            name: 书名（如果未提供，从文件名提取）
            version: 版本（如果未提供，使用当前日期）
            isbn: ISBN（可选）
            field: 学科领域（可选）
            status: 使用状态（默认：启用）
            
        Returns:
            注册的书籍信息
        """
        pdf_path_obj = Path(pdf_path)
        if not pdf_path_obj.exists():
            raise FileNotFoundError(f"PDF文件不存在: {pdf_path}")
        
        # 如果没有提供书名，从文件名提取
        if not name:
            name = pdf_path_obj.stem
        
        # 如果没有提供版本，使用当前日期
        if not version:
            version = datetime.now().strftime("%Y%m%d")
        
        # 生成唯一标识
        book_id = self._generate_book_id(name, version)
        
        # 检查是否已存在
        if book_id in self.books:
            print(f"警告：书籍 {book_id} 已存在，将更新信息")
        
        # 创建书籍记录
        book_info = {
            "book_id": book_id,
            "name": name,
            "version": version,
            "isbn": isbn,
            "field": field,
            "status": status,
            "pdf_path": str(pdf_path_obj.absolute()),
            "file_name": pdf_path_obj.name,
            "file_size": pdf_path_obj.stat().st_size,
            "registered_at": datetime.now().isoformat(),
            "updated_at": datetime.now().isoformat()
        }
        
        # 保存到注册表
        self.books[book_id] = book_info
        self._save_registry()
        
        print(f"✓ 书籍注册成功: {book_id}")
        print(f"  书名: {name}")
        print(f"  版本: {version}")
        print(f"  文件: {pdf_path_obj.name}")
        
        return book_info
    
    def get_book(self, book_id: str) -> Optional[Dict]:
        """
        获取书籍信息
        
        Args:
            book_id: 书籍唯一标识
            
        Returns:
            书籍信息，如果不存在返回None
        """
        return self.books.get(book_id)
    
    def list_books(self, status: Optional[str] = None) -> List[Dict]:
        """
        列出所有书籍
        
        Args:
            status: 按状态筛选（可选）
            
        Returns:
            书籍列表
        """
        books = list(self.books.values())
        if status:
            books = [b for b in books if b.get('status') == status]
        return sorted(books, key=lambda x: x.get('registered_at', ''), reverse=True)
    
    def update_book(
        self,
        book_id: str,
        status: Optional[str] = None,
        field: Optional[str] = None,
        **kwargs
    ) -> Optional[Dict]:
        """
        更新书籍信息
        
        Args:
            book_id: 书籍唯一标识
            status: 使用状态
            field: 学科领域
            **kwargs: 其他字段
            
        Returns:
            更新后的书籍信息，如果不存在返回None
        """
        if book_id not in self.books:
            print(f"错误：书籍 {book_id} 不存在")
            return None
        
        book = self.books[book_id]
        
        # 更新字段
        if status:
            if status not in [self.STATUS_ENABLED, self.STATUS_DISABLED, self.STATUS_RUNNING]:
                print(f"错误：无效的状态 {status}")
                return None
            book['status'] = status
        
        if field:
            if field not in self.FIELDS:
                print(f"警告：学科领域 {field} 不在预定义列表中，但仍会保存")
            book['field'] = field
        
        # 更新其他字段
        for key, value in kwargs.items():
            if key not in ['book_id', 'registered_at']:  # 不允许修改这些字段
                book[key] = value
        
        book['updated_at'] = datetime.now().isoformat()
        
        self._save_registry()
        print(f"✓ 书籍信息已更新: {book_id}")
        
        return book
    
    def get_versions(self, name: str) -> List[Dict]:
        """
        获取指定书名的所有版本
        
        Args:
            name: 书名
            
        Returns:
            该书籍的所有版本列表
        """
        versions = []
        for book_id, book in self.books.items():
            if book.get('name') == name:
                versions.append(book)
        return sorted(versions, key=lambda x: x.get('version', ''), reverse=True)


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='书籍接入与版本治理工具')
    subparsers = parser.add_subparsers(dest='command', help='命令')
    
    # 注册书籍
    register_parser = subparsers.add_parser('register', help='注册新书籍')
    register_parser.add_argument('pdf_path', help='PDF文件路径')
    register_parser.add_argument('--name', '-n', help='书名（如果未提供，从文件名提取）')
    register_parser.add_argument('--version', '-v', help='版本（如果未提供，使用当前日期）')
    register_parser.add_argument('--isbn', '-i', help='ISBN')
    register_parser.add_argument('--field', '-f', choices=BookRegistry.FIELDS, help='学科领域')
    register_parser.add_argument('--status', '-s', 
                                choices=[BookRegistry.STATUS_ENABLED, BookRegistry.STATUS_DISABLED, BookRegistry.STATUS_RUNNING],
                                default=BookRegistry.STATUS_ENABLED, help='使用状态')
    register_parser.add_argument('--registry', '-r', default='book_registry.json', help='注册表文件路径')
    
    # 列出书籍
    list_parser = subparsers.add_parser('list', help='列出所有书籍')
    list_parser.add_argument('--status', '-s', 
                            choices=[BookRegistry.STATUS_ENABLED, BookRegistry.STATUS_DISABLED, BookRegistry.STATUS_RUNNING],
                            help='按状态筛选')
    list_parser.add_argument('--registry', '-r', default='book_registry.json', help='注册表文件路径')
    
    # 获取书籍信息
    get_parser = subparsers.add_parser('get', help='获取书籍信息')
    get_parser.add_argument('book_id', help='书籍唯一标识')
    get_parser.add_argument('--registry', '-r', default='book_registry.json', help='注册表文件路径')
    
    # 更新书籍信息
    update_parser = subparsers.add_parser('update', help='更新书籍信息')
    update_parser.add_argument('book_id', help='书籍唯一标识')
    update_parser.add_argument('--status', '-s',
                              choices=[BookRegistry.STATUS_ENABLED, BookRegistry.STATUS_DISABLED, BookRegistry.STATUS_RUNNING],
                              help='使用状态')
    update_parser.add_argument('--field', '-f', choices=BookRegistry.FIELDS, help='学科领域')
    update_parser.add_argument('--registry', '-r', default='book_registry.json', help='注册表文件路径')
    
    # 获取版本列表
    versions_parser = subparsers.add_parser('versions', help='获取书籍的所有版本')
    versions_parser.add_argument('name', help='书名')
    versions_parser.add_argument('--registry', '-r', default='book_registry.json', help='注册表文件路径')
    
    args = parser.parse_args()
    
    if not args.command:
        parser.print_help()
        return
    
    registry = BookRegistry(args.registry)
    
    if args.command == 'register':
        book_info = registry.register_book(
            pdf_path=args.pdf_path,
            name=args.name,
            version=args.version,
            isbn=args.isbn,
            field=args.field,
            status=args.status
        )
        print(f"\n书籍信息:")
        print(json.dumps(book_info, ensure_ascii=False, indent=2))
    
    elif args.command == 'list':
        books = registry.list_books(status=args.status)
        print(f"\n共找到 {len(books)} 本书籍:\n")
        for book in books:
            print(f"  [{book['book_id']}] {book['name']} (版本: {book['version']})")
            print(f"      状态: {book.get('status', '未知')}")
            print(f"      学科: {book.get('field', '未设置')}")
            print(f"      注册时间: {book.get('registered_at', '未知')}")
            print()
    
    elif args.command == 'get':
        book = registry.get_book(args.book_id)
        if book:
            print(f"\n书籍信息:")
            print(json.dumps(book, ensure_ascii=False, indent=2))
        else:
            print(f"错误：书籍 {args.book_id} 不存在")
    
    elif args.command == 'update':
        book = registry.update_book(
            book_id=args.book_id,
            status=args.status,
            field=args.field
        )
        if book:
            print(f"\n更新后的书籍信息:")
            print(json.dumps(book, ensure_ascii=False, indent=2))
    
    elif args.command == 'versions':
        versions = registry.get_versions(args.name)
        print(f"\n《{args.name}》的所有版本 ({len(versions)} 个):\n")
        for version in versions:
            print(f"  [{version['book_id']}] 版本: {version['version']}")
            print(f"      状态: {version.get('status', '未知')}")
            print(f"      注册时间: {version.get('registered_at', '未知')}")
            print()


if __name__ == '__main__':
    main()

