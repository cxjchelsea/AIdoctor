"""
导入任务模型
"""
from pydantic import BaseModel
from typing import Optional, Dict, Any
from datetime import datetime
from app.models.response import TaskStatus


class ImportTask(BaseModel):
    """导入任务"""
    task_id: str
    task_type: str  # kg, table, config
    item_type: str  # coding-standards, standard-directory, etc.
    status: TaskStatus
    file_path: str
    options: Optional[Dict[str, Any]] = None
    progress: float = 0.0
    message: str = ""
    result: Optional[Dict[str, Any]] = None
    created_at: datetime
    updated_at: datetime
    completed_at: Optional[datetime] = None

