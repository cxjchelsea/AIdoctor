"""
书籍管理服务
负责读取和管理书籍注册表和结构解析结果
"""
import json
import logging
from pathlib import Path
from typing import Dict, Optional, List
from fastapi import HTTPException

logger = logging.getLogger(__name__)

# 书籍提取脚本目录
# 从 knowledge-management-service/app/services/book_service.py
# 到项目根目录: ../../../
# 然后到: docs/AI医生/知识内容提取/提取脚本/book_extraction
BOOK_EXTRACTION_DIR = Path(__file__).resolve().parent.parent.parent.parent / "docs" / "AI医生" / "知识内容提取" / "提取脚本" / "book_extraction"

# 提取结果目录（新路径）
EXTRACTION_RESULTS_DIR = Path(__file__).resolve().parent.parent.parent.parent / "docs" / "AI医生" / "知识内容提取" / "提取结果"


class BookService:
    """书籍管理服务"""
    
    def __init__(self):
        self.registry_file = BOOK_EXTRACTION_DIR / "book_registry.json"
        # 结构文件可能在两个位置：
        # 1. 旧的：book_extraction 目录
        # 2. 新的：提取结果/书籍名称 目录
        self.structure_dirs = [
            BOOK_EXTRACTION_DIR,  # 旧路径（兼容）
            EXTRACTION_RESULTS_DIR  # 新路径（主路径）
        ]
    
    def _load_json_file(self, file_path: Path) -> Dict:
        """加载JSON文件"""
        try:
            if not file_path.exists():
                logger.warning(f"文件不存在: {file_path}")
                return {}
            
            with open(file_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except json.JSONDecodeError as e:
            logger.error(f"JSON解析失败: {file_path}, 错误: {e}")
            raise HTTPException(status_code=500, detail=f"JSON文件格式错误: {str(e)}")
        except Exception as e:
            logger.error(f"读取文件失败: {file_path}, 错误: {e}")
            raise HTTPException(status_code=500, detail=f"读取文件失败: {str(e)}")
    
    def get_book_registry(self) -> Dict:
        """获取书籍注册表"""
        return self._load_json_file(self.registry_file)
    
    def get_book_by_id(self, book_id: str) -> Optional[Dict]:
        """根据book_id获取书籍信息"""
        registry = self.get_book_registry()
        return registry.get(book_id)
    
    def get_book_structure(self, book_id: str) -> Optional[Dict]:
        """获取书籍的结构解析结果"""
        # 从注册表获取书籍信息，用于匹配
        book_info = self.get_book_by_id(book_id)
        book_name = book_info.get("name") if book_info else None
        
        # 查找结构解析结果文件（在多个目录中查找）
        # 文件名格式: {book_name}_structure.json
        structure_files = []
        for structure_dir in self.structure_dirs:
            # 在目录本身查找
            structure_files.extend(list(structure_dir.glob("*_structure.json")))
            # 在子目录中查找（例如：提取结果/贝茨/）
            if structure_dir.exists():
                for subdir in structure_dir.iterdir():
                    if subdir.is_dir():
                        structure_files.extend(list(subdir.glob("*_structure.json")))
        
        # 策略1: 精确匹配book_id
        for structure_file in structure_files:
            try:
                structure_data = self._load_json_file(structure_file)
                if structure_data.get("book_id") == book_id:
                    logger.info(f"通过精确book_id匹配找到结构文件: {structure_file}")
                    return structure_data
            except Exception as e:
                logger.warning(f"读取结构文件失败: {structure_file}, 错误: {e}")
                continue
        
        # 策略2: 通过注册表中的书名匹配（去掉版本号）
        if book_name:
            for structure_file in structure_files:
                try:
                    structure_data = self._load_json_file(structure_file)
                    structure_book_id = structure_data.get("book_id", "")
                    structure_book_name = structure_data.get("book_name", "")
                    
                    # 匹配逻辑：
                    # 1. structure中的book_id等于注册表中的name
                    # 2. structure中的book_name包含注册表中的name
                    # 3. 文件名包含注册表中的name
                    if (structure_book_id == book_name or 
                        book_name in structure_book_name or
                        book_name in structure_file.stem):
                        logger.info(f"通过书名匹配找到结构文件: {structure_file}")
                        return structure_data
                except Exception as e:
                    logger.warning(f"读取结构文件失败: {structure_file}, 错误: {e}")
                    continue
        
        # 策略3: 通过book_id的部分匹配（去掉版本号部分）
        # book_id格式通常是: 书名_版本，尝试匹配书名部分
        book_id_base = book_id.rsplit("_", 1)[0]  # 去掉最后一个下划线后的部分（版本号）
        for structure_file in structure_files:
            try:
                structure_data = self._load_json_file(structure_file)
                structure_book_id = structure_data.get("book_id", "")
                
                # 如果结构文件中的book_id等于book_id的基础部分（书名）
                if structure_book_id == book_id_base:
                    logger.info(f"通过book_id基础部分匹配找到结构文件: {structure_file}")
                    return structure_data
            except Exception as e:
                logger.warning(f"读取结构文件失败: {structure_file}, 错误: {e}")
                continue
        
        # 策略4: 通过文件名模糊匹配
        for structure_file in structure_files:
            try:
                # 文件名包含book_id或book_name的主要部分
                file_stem = structure_file.stem.replace("_structure", "")
                if book_id_base in file_stem or (book_name and book_name in file_stem):
                    structure_data = self._load_json_file(structure_file)
                    logger.info(f"通过文件名模糊匹配找到结构文件: {structure_file}")
                    return structure_data
            except Exception as e:
                logger.warning(f"读取结构文件失败: {structure_file}, 错误: {e}")
                continue
        
        logger.warning(f"未找到book_id={book_id}的结构解析结果文件")
        return None
    
    def update_book_status(self, book_id: str, status: str) -> bool:
        """更新书籍状态"""
        if status not in ["启用", "停用", "重跑中"]:
            raise HTTPException(status_code=400, detail="无效的状态值")
        
        registry = self.get_book_registry()
        if book_id not in registry:
            raise HTTPException(status_code=404, detail=f"书籍不存在: {book_id}")
        
        registry[book_id]["status"] = status
        
        try:
            with open(self.registry_file, 'w', encoding='utf-8') as f:
                json.dump(registry, f, ensure_ascii=False, indent=2)
            logger.info(f"更新书籍状态成功: {book_id} -> {status}")
            return True
        except Exception as e:
            logger.error(f"保存书籍状态失败: {book_id}, 错误: {e}")
            raise HTTPException(status_code=500, detail=f"保存失败: {str(e)}")

