"""
请求模型
按照《dialog-service - 服务实现方案.md》定义
"""
from pydantic import BaseModel
from typing import Dict, Optional, List, Any


class QuestionRequest(BaseModel):
    """生成问题请求"""
    cdpId: str  # 使用cdpId而不是diagnosis_id，符合文档规范
    context: Optional[Dict[str, Any]] = None
    patientState: Optional[Dict[str, Any]] = None  # 可选：如果提供则直接使用，否则从CDP读取


class UserInputRequest(BaseModel):
    """用户输入请求"""
    cdpId: str  # 使用cdpId而不是diagnosis_id，符合文档规范
    userInput: str  # 使用userInput而不是user_input，符合文档规范
    context: Optional[Dict[str, Any]] = None


class IdentifyGapsRequest(BaseModel):
    """识别信息缺口请求"""
    cdpId: str
    patientState: Optional[Dict[str, Any]] = None  # 可选：如果提供则直接使用，否则从CDP读取

