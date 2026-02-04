"""
响应模型
"""
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from datetime import datetime
from enum import Enum


class TaskStatus(str, Enum):
    """任务状态"""
    PENDING = "pending"
    RUNNING = "running"
    SUCCESS = "success"
    FAILED = "failed"


class ImportResult(BaseModel):
    """导入结果"""
    task_id: str
    status: TaskStatus
    message: str
    imported_count: Optional[int] = None
    failed_count: Optional[int] = None
    errors: Optional[List[str]] = None
    created_at: datetime
    completed_at: Optional[datetime] = None


class ValidationResult(BaseModel):
    """验证结果"""
    is_valid: bool
    errors: List[str]
    warnings: List[str]
    details: Optional[Dict[str, Any]] = None


class TaskStatusResponse(BaseModel):
    """任务状态响应"""
    task_id: str
    status: TaskStatus
    progress: float  # 0.0 - 1.0
    message: str
    result: Optional[Dict[str, Any]] = None
    created_at: datetime
    updated_at: datetime

