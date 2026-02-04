"""
请求模型
"""
from pydantic import BaseModel
from typing import Optional, List, Dict, Any


class ImportRequest(BaseModel):
    """导入请求"""
    file_path: str
    options: Optional[Dict[str, Any]] = None


class ValidationRequest(BaseModel):
    """验证请求"""
    item_type: str  # kg, table, config
    item_id: Optional[str] = None
    validation_rules: Optional[List[str]] = None


class CleanRequest(BaseModel):
    """清洗请求"""
    data_type: str  # kg, table, config
    clean_options: Optional[Dict[str, Any]] = None

